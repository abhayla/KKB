from fastapi import APIRouter

from app.api.deps import CurrentUser, DbSession
from app.schemas.household import (
    AddMemberByPhoneRequest,
    HouseholdCreate,
    HouseholdDetailResponse,
    HouseholdMemberResponse,
    HouseholdResponse,
    HouseholdUpdate,
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


@router.post("", response_model=HouseholdResponse, status_code=201)
async def create_household(body: HouseholdCreate, user: CurrentUser, db: DbSession):
    """Create a new household. Caller becomes OWNER."""
    household = await HouseholdService.create(name=body.name, user=user, db=db)
    return _household_response(household, member_count=1)


@router.get("/{household_id}", response_model=HouseholdDetailResponse)
async def get_household(household_id: str, user: CurrentUser, db: DbSession):
    """Get household details + members. Must be a member."""
    household = await HouseholdService.get(
        household_id=household_id, user=user, db=db
    )
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
    return _household_response(household, member_count=0)


@router.delete("/{household_id}", status_code=204)
async def deactivate_household(
    household_id: str, user: CurrentUser, db: DbSession
):
    """Soft-deactivate household. Owner only."""
    await HouseholdService.deactivate(
        household_id=household_id, user=user, db=db
    )


@router.get(
    "/{household_id}/members", response_model=list[HouseholdMemberResponse]
)
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
async def add_member(
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
