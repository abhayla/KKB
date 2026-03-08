import uuid

from fastapi import APIRouter, Query, Request
from sqlalchemy import extract, func, select

from app.api.deps import CurrentUser, DbSession
from app.config import settings
from app.core.exceptions import NotFoundError
from app.core.rate_limit import limiter
from app.models.meal_plan import MealPlan, MealPlanItem
from app.models.recipe_rule import RecipeRule
from app.schemas.household import (
    AddMemberByPhoneRequest,
    CreateHouseholdRecipeRuleRequest,
    HouseholdCreate,
    HouseholdDetailResponse,
    HouseholdMemberResponse,
    HouseholdRecipeRuleResponse,
    HouseholdResponse,
    HouseholdUpdate,
    InviteCodeResponse,
    JoinHouseholdRequest,
    TransferOwnershipRequest,
    UpdateMemberRequest,
)
from app.services.household_service import HouseholdService

router = APIRouter(prefix="/households", tags=["households"])


def _member_response(m) -> HouseholdMemberResponse:
    """Convert a HouseholdMember model to response schema."""
    return HouseholdMemberResponse(
        id=m.id,
        household_id=m.household_id,
        user_id=m.user_id,
        family_member_id=m.family_member_id,
        name=None,
        role=m.role,
        can_edit_shared_plan=m.can_edit_shared_plan,
        is_temporary=m.is_temporary,
        join_date=m.join_date,
        leave_date=m.leave_date,
        portion_size=m.portion_size,
        status=m.status,
    )


def _household_response(household, member_count: int) -> HouseholdResponse:
    """Convert a Household model to response schema."""
    return HouseholdResponse(
        id=household.id,
        name=household.name,
        invite_code=household.invite_code,
        owner_id=household.owner_id,
        max_members=household.max_members,
        member_count=member_count,
        is_active=household.is_active,
        created_at=household.created_at,
        updated_at=household.updated_at,
    )


def _recipe_rule_response(r) -> HouseholdRecipeRuleResponse:
    """Convert a RecipeRule model to household recipe rule response schema."""
    return HouseholdRecipeRuleResponse(
        id=r.id,
        household_id=r.household_id,
        scope=r.scope,
        target_type=r.target_type,
        action=r.action,
        target_name=r.target_name,
        frequency_type=r.frequency_type,
        frequency_count=r.frequency_count,
        frequency_days=r.frequency_days,
        enforcement=r.enforcement,
        meal_slot=r.meal_slot,
        is_active=r.is_active,
        created_at=r.created_at,
        updated_at=r.updated_at,
    )


@router.post("", response_model=HouseholdResponse, status_code=201)
@limiter.limit("500/minute" if settings.debug else "5/minute")
async def create_household(
    request: Request, body: HouseholdCreate, user: CurrentUser, db: DbSession
):
    """Create a new household. Caller becomes OWNER."""
    household = await HouseholdService.create(name=body.name, user=user, db=db)
    return _household_response(household, member_count=1)


@router.get("/{household_id}", response_model=HouseholdDetailResponse)
async def get_household(household_id: str, user: CurrentUser, db: DbSession):
    """Get household details + members. Must be a member."""
    household = await HouseholdService.get(household_id=household_id, user=user, db=db)
    active_members = [m for m in household.members if m.status == "ACTIVE"]
    return HouseholdDetailResponse(
        household=_household_response(household, len(active_members)),
        members=[_member_response(m) for m in active_members],
    )


@router.put("/{household_id}", response_model=HouseholdResponse)
async def update_household(
    household_id: str, body: HouseholdUpdate, user: CurrentUser, db: DbSession
):
    """Update household. Owner only."""
    household = await HouseholdService.update(
        household_id=household_id,
        name=body.name,
        max_members=body.max_members,
        user=user,
        db=db,
    )
    active_count = await HouseholdService._count_active_members(household_id, db)
    return _household_response(household, member_count=active_count)


@router.delete("/{household_id}", status_code=204)
async def deactivate_household(household_id: str, user: CurrentUser, db: DbSession):
    """Soft-deactivate household. Owner only."""
    await HouseholdService.deactivate(household_id=household_id, user=user, db=db)


@router.get("/{household_id}/members", response_model=list[HouseholdMemberResponse])
async def list_members(household_id: str, user: CurrentUser, db: DbSession):
    """List all household members."""
    members = await HouseholdService.list_members(
        household_id=household_id, user=user, db=db
    )
    return [_member_response(m) for m in members]


@router.post(
    "/{household_id}/members",
    response_model=HouseholdMemberResponse,
    status_code=201,
)
@limiter.limit("500/minute" if settings.debug else "10/minute")
async def add_member(
    request: Request,
    household_id: str,
    body: AddMemberByPhoneRequest,
    user: CurrentUser,
    db: DbSession,
):
    """Add member by phone number. Owner only."""
    member = await HouseholdService.add_member_by_phone(
        household_id=household_id,
        phone_number=body.phone_number,
        user=user,
        db=db,
    )
    return _member_response(member)


@router.post("/{household_id}/invite-code", response_model=InviteCodeResponse)
@limiter.limit("500/minute" if settings.debug else "5/minute")
async def refresh_invite_code(
    request: Request, household_id: str, user: CurrentUser, db: DbSession
):
    """Generate/refresh invite code. Owner only."""
    code, expires_at = await HouseholdService.refresh_invite_code(
        household_id=household_id, user=user, db=db
    )
    return InviteCodeResponse(invite_code=code, expires_at=expires_at)


@router.post("/join", response_model=HouseholdMemberResponse, status_code=201)
@limiter.limit("500/minute" if settings.debug else "10/minute")
async def join_household(
    request: Request, body: JoinHouseholdRequest, user: CurrentUser, db: DbSession
):
    """Join household via invite code."""
    member = await HouseholdService.join(invite_code=body.invite_code, user=user, db=db)
    return _member_response(member)


@router.post("/{household_id}/leave", status_code=204)
async def leave_household(household_id: str, user: CurrentUser, db: DbSession):
    """Leave a household."""
    await HouseholdService.leave(household_id=household_id, user=user, db=db)


@router.post("/{household_id}/transfer-ownership", status_code=204)
async def transfer_ownership(
    household_id: str,
    body: TransferOwnershipRequest,
    user: CurrentUser,
    db: DbSession,
):
    """Transfer household ownership."""
    await HouseholdService.transfer(
        household_id=household_id,
        new_owner_member_id=body.new_owner_member_id,
        user=user,
        db=db,
    )


@router.put(
    "/{household_id}/members/{member_id}",
    response_model=HouseholdMemberResponse,
)
async def update_member(
    household_id: str,
    member_id: str,
    body: UpdateMemberRequest,
    user: CurrentUser,
    db: DbSession,
):
    """Update member details. Owner only."""
    member = await HouseholdService.update_member(
        household_id=household_id,
        member_id=member_id,
        data=body.model_dump(exclude_unset=True),
        user=user,
        db=db,
    )
    return _member_response(member)


@router.delete("/{household_id}/members/{member_id}", status_code=204)
async def remove_member(
    household_id: str,
    member_id: str,
    user: CurrentUser,
    db: DbSession,
):
    """Remove member. Owner only."""
    await HouseholdService.remove_member(
        household_id=household_id,
        member_id=member_id,
        user=user,
        db=db,
    )


@router.get("/{household_id}/stats/monthly")
async def get_household_monthly_stats(
    household_id: str,
    user: CurrentUser,
    db: DbSession,
    month: int = Query(ge=1, le=12),
    year: int = Query(ge=2020),
):
    """Get monthly stats for a household. Member access."""
    await HouseholdService._verify_membership(household_id, user, db)

    # Count meal plan items for this household in the given month
    result = await db.execute(
        select(func.count())
        .select_from(MealPlanItem)
        .join(MealPlan, MealPlanItem.meal_plan_id == MealPlan.id)
        .where(
            MealPlan.household_id == household_id,
            extract("month", MealPlanItem.date) == month,
            extract("year", MealPlanItem.date) == year,
        )
    )
    total_items = result.scalar_one()

    # Count by meal_status
    status_result = await db.execute(
        select(MealPlanItem.meal_status, func.count())
        .join(MealPlan, MealPlanItem.meal_plan_id == MealPlan.id)
        .where(
            MealPlan.household_id == household_id,
            extract("month", MealPlanItem.date) == month,
            extract("year", MealPlanItem.date) == year,
        )
        .group_by(MealPlanItem.meal_status)
    )
    status_counts = {row[0]: row[1] for row in status_result.fetchall()}

    return {
        "household_id": household_id,
        "month": month,
        "year": year,
        "total_meals_planned": total_items,
        "meals_cooked": status_counts.get("COOKED", 0),
        "meals_skipped": status_counts.get("SKIPPED", 0),
        "meals_ordered_out": status_counts.get("ORDERED_OUT", 0),
    }


# --- Household Recipe Rules ---


@router.get(
    "/{household_id}/recipe-rules",
    response_model=list[HouseholdRecipeRuleResponse],
)
async def list_household_recipe_rules(
    household_id: str, user: CurrentUser, db: DbSession
):
    """List all recipe rules scoped to this household. Member access."""
    await HouseholdService._verify_membership(household_id, user, db)
    result = await db.execute(
        select(RecipeRule).where(
            RecipeRule.household_id == household_id,
            RecipeRule.scope == "HOUSEHOLD",
            RecipeRule.is_active == True,  # noqa: E712
        )
    )
    return [_recipe_rule_response(r) for r in result.scalars().all()]


@router.post(
    "/{household_id}/recipe-rules",
    response_model=HouseholdRecipeRuleResponse,
    status_code=201,
)
async def create_household_recipe_rule(
    household_id: str,
    body: CreateHouseholdRecipeRuleRequest,
    user: CurrentUser,
    db: DbSession,
):
    """Create a household-scoped recipe rule. Owner only."""
    await HouseholdService._verify_owner(household_id, user, db)
    rule = RecipeRule(
        id=str(uuid.uuid4()),
        user_id=str(user.id),
        household_id=household_id,
        scope="HOUSEHOLD",
        target_type=body.target_type,
        action=body.action,
        target_name=body.target_name,
        frequency_type=body.frequency_type,
        frequency_count=body.frequency_count,
        frequency_days=body.frequency_days,
        enforcement=body.enforcement or "PREFERRED",
        meal_slot=body.meal_slot,
        is_active=True,
    )
    db.add(rule)
    await db.flush()
    return _recipe_rule_response(rule)


@router.delete(
    "/{household_id}/recipe-rules/{rule_id}",
    status_code=204,
)
async def delete_household_recipe_rule(
    household_id: str, rule_id: str, user: CurrentUser, db: DbSession
):
    """Soft-delete a household recipe rule. Owner only."""
    await HouseholdService._verify_owner(household_id, user, db)
    result = await db.execute(
        select(RecipeRule).where(
            RecipeRule.id == rule_id,
            RecipeRule.household_id == household_id,
        )
    )
    rule = result.scalar_one_or_none()
    if not rule:
        raise NotFoundError("Rule not found in this household")
    rule.is_active = False
    await db.flush()


# --- Household Notifications ---


@router.get("/{household_id}/notifications")
async def list_household_notifications(
    household_id: str, user: CurrentUser, db: DbSession
):
    """List household notifications for the current user. Member access."""
    from app.models.notification import Notification

    await HouseholdService._verify_membership(household_id, user, db)
    result = await db.execute(
        select(Notification)
        .where(
            Notification.household_id == household_id,
            Notification.user_id == str(user.id),
        )
        .order_by(Notification.created_at.desc())
        .limit(50)
    )
    notifications = result.scalars().all()
    return [
        {
            "id": n.id,
            "type": n.type,
            "title": n.title,
            "body": n.body,
            "is_read": n.is_read,
            "metadata_json": n.metadata_json,
            "created_at": n.created_at.isoformat() if n.created_at else None,
        }
        for n in notifications
    ]


@router.put("/{household_id}/notifications/{notification_id}/read", status_code=204)
async def mark_notification_read(
    household_id: str,
    notification_id: str,
    user: CurrentUser,
    db: DbSession,
):
    """Mark a household notification as read."""
    from app.models.notification import Notification

    await HouseholdService._verify_membership(household_id, user, db)
    result = await db.execute(
        select(Notification).where(
            Notification.id == notification_id,
            Notification.household_id == household_id,
            Notification.user_id == str(user.id),
        )
    )
    notification = result.scalar_one_or_none()
    if not notification:
        raise NotFoundError("Notification not found")
    notification.is_read = True
    await db.flush()


# --- Household Meal Plans ---


@router.get("/{household_id}/meal-plans/current")
async def get_household_current_meal_plan(
    household_id: str, user: CurrentUser, db: DbSession
):
    """Get current household meal plan. Member access."""
    plan = await HouseholdService.get_current_meal_plan(
        household_id=household_id, user=user, db=db
    )
    if not plan:
        return {"meal_plan": None}

    # Group items by date
    items_by_date: dict = {}
    for item in plan.items:
        d = item.date.isoformat()
        if d not in items_by_date:
            items_by_date[d] = {"date": d, "meals": {}}
        meal_type = item.meal_type
        if meal_type not in items_by_date[d]["meals"]:
            items_by_date[d]["meals"][meal_type] = []
        items_by_date[d]["meals"][meal_type].append(
            {
                "id": item.id,
                "recipe_name": item.recipe_name,
                "meal_status": item.meal_status,
                "scope": item.scope,
                "for_user_id": item.for_user_id,
                "is_locked": item.is_locked,
            }
        )

    return {
        "meal_plan": {
            "id": plan.id,
            "household_id": plan.household_id,
            "week_start_date": plan.week_start_date.isoformat(),
            "week_end_date": plan.week_end_date.isoformat(),
            "days": list(items_by_date.values()),
        }
    }


@router.get("/{household_id}/constraints")
async def get_household_constraints(
    household_id: str, user: CurrentUser, db: DbSession
):
    """Get merged dietary constraints for the household. Owner access."""
    await HouseholdService._verify_owner(household_id, user, db)
    constraints = await HouseholdService.get_merged_constraints(
        household_id=household_id, db=db
    )
    return constraints


@router.put("/{household_id}/meal-plans/{plan_id}/items/{item_id}/status")
async def update_meal_item_status(
    household_id: str,
    plan_id: str,
    item_id: str,
    user: CurrentUser,
    db: DbSession,
    status: str = Query(pattern="^(PLANNED|COOKED|SKIPPED|ORDERED_OUT)$"),
):
    """Update meal item status (COOKED, SKIPPED, etc). Member with edit access."""
    from app.core.exceptions import ForbiddenError as ForbiddenErr

    member = await HouseholdService._verify_membership(household_id, user, db)
    if member.role != "OWNER" and not member.can_edit_shared_plan:
        raise ForbiddenErr("You don't have edit access to this household's meal plan")

    result = await db.execute(
        select(MealPlanItem)
        .join(MealPlan)
        .where(
            MealPlanItem.id == item_id,
            MealPlan.id == plan_id,
            MealPlan.household_id == household_id,
        )
    )
    item = result.scalar_one_or_none()
    if not item:
        raise NotFoundError("Meal item not found")

    item.meal_status = status
    await db.flush()
    return {"id": item.id, "meal_status": item.meal_status}
