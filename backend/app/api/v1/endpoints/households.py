from fastapi import APIRouter, Request

from app.api.deps import CurrentUser, DbSession
from app.config import settings
from app.core.rate_limit import limiter
from app.schemas.household import (
    AddMemberByPhoneRequest,
    HouseholdCreate,
    HouseholdDetailResponse,
    HouseholdMemberResponse,
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
