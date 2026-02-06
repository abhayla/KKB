"""Recipe rules and nutrition goals API endpoints."""

import logging
from datetime import datetime, timezone
from uuid import uuid4

from fastapi import APIRouter, HTTPException, status
from sqlalchemy import delete, select

from app.api.deps import CurrentUser, DbSession
from app.core.exceptions import NotFoundError
from app.models.recipe_rule import NutritionGoal, RecipeRule
from app.schemas.recipe_rule import (
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

router = APIRouter(prefix="/recipe-rules", tags=["recipe-rules"])


# ==================== Recipe Rules Endpoints ====================


@router.get("", response_model=RecipeRulesListResponse)
async def get_recipe_rules(
    db: DbSession,
    current_user: CurrentUser,
) -> RecipeRulesListResponse:
    """Get all recipe rules for the current user.

    Returns rules sorted by creation date (newest first).
    """
    user_id = current_user.id

    result = await db.execute(
        select(RecipeRule)
        .where(RecipeRule.user_id == user_id)
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
) -> RecipeRuleResponse:
    """Create a new recipe rule."""
    user_id = current_user.id

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
                existing.updated_at = now
                existing.sync_status = "SYNCED"
                synced_rule_ids.append(client_rule.id)
            else:
                # Server wins - mark as conflict
                conflict_rule_ids.append(client_rule.id)
        else:
            # New rule from client
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
