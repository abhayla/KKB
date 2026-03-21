"""Recipe rules and nutrition goals API endpoints."""

import logging
from datetime import datetime, timezone
from uuid import uuid4

from fastapi import APIRouter, HTTPException, Query, status
from fastapi.responses import JSONResponse
from sqlalchemy import delete, func, select
from sqlalchemy.orm import selectinload

from app.api.deps import CurrentUser, DbSession
from app.core.exceptions import NotFoundError
from app.models.household import Household, HouseholdMember
from app.models.recipe_rule import NutritionGoal, RecipeRule
from app.models.user import FamilyMember
from app.services.family_constraints import get_family_forbidden_keywords
from app.schemas.recipe_rule import (
    ConflictDetail,
    ConflictResponse,
    NutritionGoalCreate,
    NutritionGoalResponse,
    NutritionGoalsListResponse,
    NutritionGoalUpdate,
    RecipeRuleCreate,
    RecipeRuleResponse,
    RecipeRulesListResponse,
    RecipeRuleUpdate,
    SyncRequest,
    SyncResponse,
)

logger = logging.getLogger(__name__)


def _ensure_tz_aware(dt: datetime | None) -> datetime | None:
    """Ensure datetime is timezone-aware (UTC)."""
    if dt is None:
        return None
    if dt.tzinfo is None:
        # Assume naive datetimes are UTC
        return dt.replace(tzinfo=timezone.utc)
    return dt


def _find_constraint_source(
    member_dicts: list[dict], member_name: str, keyword: str
) -> str:
    """Find which constraint (health condition or dietary restriction) contains the keyword."""
    from app.services.family_constraints import FAMILY_CONSTRAINT_MAP

    for member in member_dicts:
        if member.get("name") != member_name:
            continue
        for condition in member.get("health_conditions", []):
            if condition.lower() in FAMILY_CONSTRAINT_MAP:
                if keyword in FAMILY_CONSTRAINT_MAP[condition.lower()]:
                    return condition
        for restriction in member.get("dietary_restrictions", []):
            if restriction.lower() in FAMILY_CONSTRAINT_MAP:
                if keyword in FAMILY_CONSTRAINT_MAP[restriction.lower()]:
                    return restriction
    return "dietary"


async def _get_household_member_ids(db, user_id: str) -> list[str] | None:
    """Get all user IDs in the same household as the given user.

    Returns None if the user has no household.
    """
    # Check if user owns a household
    result = await db.execute(
        select(Household)
        .options(selectinload(Household.members))
        .where(Household.owner_id == user_id, Household.is_active == True)
    )
    household = result.scalar_one_or_none()

    if not household:
        # Check if user is a member of a household
        result = await db.execute(
            select(HouseholdMember).where(
                HouseholdMember.user_id == user_id,
                HouseholdMember.status == "ACTIVE",
            )
        )
        membership = result.scalar_one_or_none()
        if not membership:
            return None

        result = await db.execute(
            select(Household)
            .options(selectinload(Household.members))
            .where(Household.id == membership.household_id, Household.is_active == True)
        )
        household = result.scalar_one_or_none()
        if not household:
            return None

    member_user_ids = [
        m.user_id for m in household.members
        if m.user_id and m.status == "ACTIVE"
    ]
    if household.owner_id not in member_user_ids:
        member_user_ids.append(household.owner_id)

    return member_user_ids


router = APIRouter(prefix="/recipe-rules", tags=["recipe-rules"])


# ==================== Recipe Rules Endpoints ====================


@router.get("", response_model=RecipeRulesListResponse)
async def get_recipe_rules(
    db: DbSession,
    current_user: CurrentUser,
    scope: str = Query("personal", description="Data scope: personal or family"),
) -> RecipeRulesListResponse:
    """Get all recipe rules for the current user.

    Returns rules sorted by creation date (newest first).
    When scope=family, returns rules for all household members.
    Falls back to personal data if user has no household.
    """
    user_id = current_user.id

    user_ids = [user_id]
    if scope == "family":
        household_members = await _get_household_member_ids(db, user_id)
        if household_members:
            user_ids = household_members

    result = await db.execute(
        select(RecipeRule)
        .where(RecipeRule.user_id.in_(user_ids))
        .order_by(RecipeRule.created_at.desc())
    )
    rules = result.scalars().all()

    return RecipeRulesListResponse(
        rules=[RecipeRuleResponse.model_validate(r) for r in rules],
        total_count=len(rules),
    )


@router.post("", response_model=RecipeRuleResponse, status_code=status.HTTP_201_CREATED)
async def create_recipe_rule(
    db: DbSession,
    current_user: CurrentUser,
    rule: RecipeRuleCreate,
) -> RecipeRuleResponse | JSONResponse:
    """Create a new recipe rule."""
    user_id = current_user.id

    # Check for duplicate rule (same user + target_name + action + target_type + meal_slot)
    dup_query = select(RecipeRule).where(
        RecipeRule.user_id == user_id,
        func.upper(RecipeRule.target_name) == rule.target_name.strip().upper(),
        RecipeRule.action == rule.action,
        RecipeRule.target_type == rule.target_type,
    )
    # NULL-safe meal_slot comparison
    if rule.meal_slot is not None:
        dup_query = dup_query.where(RecipeRule.meal_slot == rule.meal_slot)
    else:
        dup_query = dup_query.where(RecipeRule.meal_slot.is_(None))

    dup_result = await db.execute(dup_query)
    if dup_result.scalar_one_or_none():
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=f"A {rule.action} rule for '{rule.target_name}' already exists"
            + (f" in {rule.meal_slot}" if rule.meal_slot else ""),
        )

    # Family safety conflict check for INCLUDE rules
    if rule.action == "INCLUDE" and not rule.force_override:
        members_result = await db.execute(
            select(FamilyMember).where(FamilyMember.user_id == user_id)
        )
        members = members_result.scalars().all()
        if members:
            member_dicts = [
                {
                    "name": m.name,
                    "health_conditions": m.health_conditions or [],
                    "dietary_restrictions": m.dietary_restrictions or [],
                }
                for m in members
            ]
            forbidden_map = get_family_forbidden_keywords(member_dicts)
            target_lower = rule.target_name.lower()

            # Collect ALL conflicts (not just the first)
            conflicts: list[ConflictDetail] = []
            for member_name, keywords in forbidden_map.items():
                for keyword in keywords:
                    if keyword in target_lower:
                        conflicts.append(
                            ConflictDetail(
                                member_name=member_name,
                                condition=_find_constraint_source(
                                    member_dicts, member_name, keyword
                                ),
                                keyword=keyword,
                                rule_target=rule.target_name,
                            )
                        )

            if conflicts:
                conflict_response = ConflictResponse(
                    detail=(
                        f"INCLUDE rule '{rule.target_name}' conflicts with family "
                        f"member health/dietary restrictions. "
                        f"Set force_override=true to override."
                    ),
                    conflict_type="family_safety",
                    conflict_details=conflicts,
                )
                return JSONResponse(
                    status_code=status.HTTP_409_CONFLICT,
                    content=conflict_response.model_dump(),
                )

    now = datetime.now(timezone.utc)
    new_rule = RecipeRule(
        id=str(uuid4()),
        user_id=user_id,
        target_type=rule.target_type,
        action=rule.action,
        target_id=rule.target_id,
        target_name=rule.target_name,
        frequency_type=rule.frequency_type,
        frequency_count=rule.frequency_count,
        frequency_days=rule.frequency_days,
        enforcement=rule.enforcement,
        meal_slot=rule.meal_slot,
        is_active=rule.is_active,
        force_override=rule.force_override,
        sync_status="SYNCED",
        created_at=now,
        updated_at=now,
    )

    db.add(new_rule)
    await db.commit()
    await db.refresh(new_rule)

    logger.info(f"Created recipe rule: {new_rule.target_name} for user {user_id}")
    return RecipeRuleResponse.model_validate(new_rule)


@router.get("/{rule_id}", response_model=RecipeRuleResponse)
async def get_recipe_rule(
    db: DbSession,
    current_user: CurrentUser,
    rule_id: str,
) -> RecipeRuleResponse:
    """Get a specific recipe rule by ID."""
    user_id = current_user.id

    result = await db.execute(
        select(RecipeRule).where(
            RecipeRule.id == rule_id,
            RecipeRule.user_id == user_id,
        )
    )
    rule = result.scalar_one_or_none()

    if not rule:
        raise NotFoundError(f"Recipe rule {rule_id} not found")

    return RecipeRuleResponse.model_validate(rule)


@router.put("/{rule_id}", response_model=RecipeRuleResponse)
async def update_recipe_rule(
    db: DbSession,
    current_user: CurrentUser,
    rule_id: str,
    rule_update: RecipeRuleUpdate,
) -> RecipeRuleResponse:
    """Update a recipe rule."""
    user_id = current_user.id

    result = await db.execute(
        select(RecipeRule).where(
            RecipeRule.id == rule_id,
            RecipeRule.user_id == user_id,
        )
    )
    rule = result.scalar_one_or_none()

    if not rule:
        raise NotFoundError(f"Recipe rule {rule_id} not found")

    # Update only provided fields
    update_data = rule_update.model_dump(exclude_unset=True)
    update_data["updated_at"] = datetime.now(timezone.utc)

    for field, value in update_data.items():
        setattr(rule, field, value)

    await db.commit()
    await db.refresh(rule)

    logger.info(f"Updated recipe rule: {rule_id}")
    return RecipeRuleResponse.model_validate(rule)


@router.delete("/{rule_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_recipe_rule(
    db: DbSession,
    current_user: CurrentUser,
    rule_id: str,
) -> None:
    """Delete a recipe rule."""
    user_id = current_user.id

    result = await db.execute(
        delete(RecipeRule).where(
            RecipeRule.id == rule_id,
            RecipeRule.user_id == user_id,
        )
    )

    if result.rowcount == 0:
        raise NotFoundError(f"Recipe rule {rule_id} not found")

    await db.commit()
    logger.info(f"Deleted recipe rule: {rule_id}")


# ==================== Nutrition Goals Endpoints ====================


nutrition_goals_router = APIRouter(prefix="/nutrition-goals", tags=["nutrition-goals"])


@nutrition_goals_router.get("", response_model=NutritionGoalsListResponse)
async def get_nutrition_goals(
    db: DbSession,
    current_user: CurrentUser,
) -> NutritionGoalsListResponse:
    """Get all nutrition goals for the current user."""
    user_id = current_user.id

    result = await db.execute(
        select(NutritionGoal)
        .where(NutritionGoal.user_id == user_id)
        .order_by(NutritionGoal.created_at.desc())
    )
    goals = result.scalars().all()

    return NutritionGoalsListResponse(
        goals=[NutritionGoalResponse.model_validate(g) for g in goals],
        total_count=len(goals),
    )


@nutrition_goals_router.post(
    "", response_model=NutritionGoalResponse, status_code=status.HTTP_201_CREATED
)
async def create_nutrition_goal(
    db: DbSession,
    current_user: CurrentUser,
    goal: NutritionGoalCreate,
) -> NutritionGoalResponse:
    """Create a new nutrition goal."""
    user_id = current_user.id

    # Check for existing goal with same category
    existing = await db.execute(
        select(NutritionGoal).where(
            NutritionGoal.user_id == user_id,
            NutritionGoal.food_category == goal.food_category,
        )
    )
    if existing.scalar_one_or_none():
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=f"Nutrition goal for {goal.food_category} already exists",
        )

    now = datetime.now(timezone.utc)
    new_goal = NutritionGoal(
        id=str(uuid4()),
        user_id=user_id,
        food_category=goal.food_category,
        weekly_target=goal.weekly_target,
        current_progress=0,
        enforcement=goal.enforcement,
        is_active=goal.is_active,
        sync_status="SYNCED",
        created_at=now,
        updated_at=now,
    )

    db.add(new_goal)
    await db.commit()
    await db.refresh(new_goal)

    logger.info(f"Created nutrition goal: {new_goal.food_category} for user {user_id}")
    return NutritionGoalResponse.model_validate(new_goal)


@nutrition_goals_router.get("/{goal_id}", response_model=NutritionGoalResponse)
async def get_nutrition_goal(
    db: DbSession,
    current_user: CurrentUser,
    goal_id: str,
) -> NutritionGoalResponse:
    """Get a specific nutrition goal by ID."""
    user_id = current_user.id

    result = await db.execute(
        select(NutritionGoal).where(
            NutritionGoal.id == goal_id,
            NutritionGoal.user_id == user_id,
        )
    )
    goal = result.scalar_one_or_none()

    if not goal:
        raise NotFoundError(f"Nutrition goal {goal_id} not found")

    return NutritionGoalResponse.model_validate(goal)


@nutrition_goals_router.put("/{goal_id}", response_model=NutritionGoalResponse)
async def update_nutrition_goal(
    db: DbSession,
    current_user: CurrentUser,
    goal_id: str,
    goal_update: NutritionGoalUpdate,
) -> NutritionGoalResponse:
    """Update a nutrition goal."""
    user_id = current_user.id

    result = await db.execute(
        select(NutritionGoal).where(
            NutritionGoal.id == goal_id,
            NutritionGoal.user_id == user_id,
        )
    )
    goal = result.scalar_one_or_none()

    if not goal:
        raise NotFoundError(f"Nutrition goal {goal_id} not found")

    # Update only provided fields
    update_data = goal_update.model_dump(exclude_unset=True)
    update_data["updated_at"] = datetime.now(timezone.utc)

    for field, value in update_data.items():
        setattr(goal, field, value)

    await db.commit()
    await db.refresh(goal)

    logger.info(f"Updated nutrition goal: {goal_id}")
    return NutritionGoalResponse.model_validate(goal)


@nutrition_goals_router.delete("/{goal_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_nutrition_goal(
    db: DbSession,
    current_user: CurrentUser,
    goal_id: str,
) -> None:
    """Delete a nutrition goal."""
    user_id = current_user.id

    result = await db.execute(
        delete(NutritionGoal).where(
            NutritionGoal.id == goal_id,
            NutritionGoal.user_id == user_id,
        )
    )

    if result.rowcount == 0:
        raise NotFoundError(f"Nutrition goal {goal_id} not found")

    await db.commit()
    logger.info(f"Deleted nutrition goal: {goal_id}")


# ==================== Sync Endpoint ====================


@router.post("/sync", response_model=SyncResponse)
async def sync_recipe_rules(
    db: DbSession,
    current_user: CurrentUser,
    sync_request: SyncRequest,
) -> SyncResponse:
    """Batch sync recipe rules and nutrition goals.

    Uses Last-Write-Wins conflict resolution based on timestamps.

    Flow:
    1. Client sends all pending local changes with timestamps
    2. Server compares timestamps and merges using LWW
    3. Server returns all changes since client's last sync
    """
    user_id = current_user.id

    now = datetime.now(timezone.utc)
    synced_rule_ids: list[str] = []
    synced_goal_ids: list[str] = []
    conflict_rule_ids: list[str] = []
    conflict_goal_ids: list[str] = []

    # Process recipe rules from client
    for client_rule in sync_request.recipe_rules:
        result = await db.execute(
            select(RecipeRule).where(
                RecipeRule.id == client_rule.id,
                RecipeRule.user_id == user_id,
            )
        )
        existing = result.scalar_one_or_none()

        if existing:
            # Compare timestamps - Last Write Wins
            # Ensure both timestamps are timezone-aware for comparison
            client_ts = _ensure_tz_aware(client_rule.local_updated_at)
            server_ts = _ensure_tz_aware(existing.updated_at)
            if client_ts and server_ts and client_ts > server_ts:
                # Client wins - update server
                existing.target_type = client_rule.target_type
                existing.action = client_rule.action
                existing.target_id = client_rule.target_id
                existing.target_name = client_rule.target_name
                existing.frequency_type = client_rule.frequency_type
                existing.frequency_count = client_rule.frequency_count
                existing.frequency_days = client_rule.frequency_days
                existing.enforcement = client_rule.enforcement
                existing.meal_slot = client_rule.meal_slot
                existing.is_active = client_rule.is_active
                existing.force_override = client_rule.force_override
                existing.updated_at = now
                existing.sync_status = "SYNCED"
                synced_rule_ids.append(client_rule.id)
            else:
                # Server wins - mark as conflict
                conflict_rule_ids.append(client_rule.id)
        else:
            # New rule from client - check for duplicate before inserting
            dup_query = select(RecipeRule).where(
                RecipeRule.user_id == user_id,
                func.upper(RecipeRule.target_name)
                == client_rule.target_name.strip().upper(),
                RecipeRule.action == client_rule.action,
                RecipeRule.target_type == client_rule.target_type,
            )
            if client_rule.meal_slot is not None:
                dup_query = dup_query.where(
                    RecipeRule.meal_slot == client_rule.meal_slot
                )
            else:
                dup_query = dup_query.where(RecipeRule.meal_slot.is_(None))

            dup_result = await db.execute(dup_query)
            if dup_result.scalar_one_or_none():
                # Skip duplicate, treat as conflict
                conflict_rule_ids.append(client_rule.id)
                continue

            new_rule = RecipeRule(
                id=client_rule.id,
                user_id=user_id,
                target_type=client_rule.target_type,
                action=client_rule.action,
                target_id=client_rule.target_id,
                target_name=client_rule.target_name,
                frequency_type=client_rule.frequency_type,
                frequency_count=client_rule.frequency_count,
                frequency_days=client_rule.frequency_days,
                enforcement=client_rule.enforcement,
                meal_slot=client_rule.meal_slot,
                is_active=client_rule.is_active,
                force_override=client_rule.force_override,
                sync_status="SYNCED",
                created_at=now,
                updated_at=now,
            )
            db.add(new_rule)
            synced_rule_ids.append(client_rule.id)

    # Process nutrition goals from client
    for client_goal in sync_request.nutrition_goals:
        result = await db.execute(
            select(NutritionGoal).where(
                NutritionGoal.id == client_goal.id,
                NutritionGoal.user_id == user_id,
            )
        )
        existing = result.scalar_one_or_none()

        if existing:
            # Compare timestamps - Last Write Wins
            # Ensure both timestamps are timezone-aware for comparison
            client_ts = _ensure_tz_aware(client_goal.local_updated_at)
            server_ts = _ensure_tz_aware(existing.updated_at)
            if client_ts and server_ts and client_ts > server_ts:
                # Client wins - update server
                existing.food_category = client_goal.food_category
                existing.weekly_target = client_goal.weekly_target
                existing.current_progress = client_goal.current_progress
                existing.enforcement = client_goal.enforcement
                existing.is_active = client_goal.is_active
                existing.updated_at = now
                existing.sync_status = "SYNCED"
                synced_goal_ids.append(client_goal.id)
            else:
                # Server wins - mark as conflict
                conflict_goal_ids.append(client_goal.id)
        else:
            # New goal from client
            new_goal = NutritionGoal(
                id=client_goal.id,
                user_id=user_id,
                food_category=client_goal.food_category,
                weekly_target=client_goal.weekly_target,
                current_progress=client_goal.current_progress,
                enforcement=client_goal.enforcement,
                is_active=client_goal.is_active,
                sync_status="SYNCED",
                created_at=now,
                updated_at=now,
            )
            db.add(new_goal)
            synced_goal_ids.append(client_goal.id)

    await db.commit()

    # Get all server-side data that client might not have
    # (either newer than last sync or conflicted)
    client_rule_ids = {r.id for r in sync_request.recipe_rules}
    client_goal_ids = {g.id for g in sync_request.nutrition_goals}

    # Fetch rules updated since last sync or not in client's batch
    rules_query = select(RecipeRule).where(RecipeRule.user_id == user_id)
    if sync_request.last_sync_time:
        rules_query = rules_query.where(
            RecipeRule.updated_at > sync_request.last_sync_time
        )

    result = await db.execute(rules_query)
    server_rules = [
        RecipeRuleResponse.model_validate(r) for r in result.scalars().all()
    ]

    # Fetch goals updated since last sync or not in client's batch
    goals_query = select(NutritionGoal).where(NutritionGoal.user_id == user_id)
    if sync_request.last_sync_time:
        goals_query = goals_query.where(
            NutritionGoal.updated_at > sync_request.last_sync_time
        )

    result = await db.execute(goals_query)
    server_goals = [
        NutritionGoalResponse.model_validate(g) for g in result.scalars().all()
    ]

    logger.info(
        f"Sync completed for user {user_id}: "
        f"{len(synced_rule_ids)} rules synced, {len(conflict_rule_ids)} conflicts, "
        f"{len(synced_goal_ids)} goals synced, {len(conflict_goal_ids)} conflicts"
    )

    return SyncResponse(
        server_recipe_rules=server_rules,
        server_nutrition_goals=server_goals,
        synced_rule_ids=synced_rule_ids,
        synced_goal_ids=synced_goal_ids,
        conflict_rule_ids=conflict_rule_ids,
        conflict_goal_ids=conflict_goal_ids,
        deleted_rule_ids=[],  # TODO: Implement soft delete tracking
        deleted_goal_ids=[],
        sync_time=now,
    )
