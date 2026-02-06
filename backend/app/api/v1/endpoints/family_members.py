"""Family members CRUD API endpoints."""

import logging
from uuid import uuid4

from fastapi import APIRouter, status
from sqlalchemy import delete, select

from app.api.deps import CurrentUser, DbSession
from app.core.exceptions import NotFoundError
from app.models.user import FamilyMember
from app.schemas.user import (
    FamilyMemberCreate,
    FamilyMemberResponse,
    FamilyMembersListResponse,
    FamilyMemberUpdate,
)

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/family-members", tags=["family-members"])


@router.get("", response_model=FamilyMembersListResponse)
async def get_family_members(
    db: DbSession,
    current_user: CurrentUser,
) -> FamilyMembersListResponse:
    """Get all family members for the current user."""
    user_id = current_user.id

    result = await db.execute(
        select(FamilyMember)
        .where(FamilyMember.user_id == user_id)
        .order_by(FamilyMember.name)
    )
    members = result.scalars().all()

    return FamilyMembersListResponse(
        members=[FamilyMemberResponse.model_validate(m) for m in members],
        total_count=len(members),
    )


@router.post("", response_model=FamilyMemberResponse, status_code=status.HTTP_201_CREATED)
async def create_family_member(
    db: DbSession,
    current_user: CurrentUser,
    member: FamilyMemberCreate,
) -> FamilyMemberResponse:
    """Create a new family member."""
    user_id = current_user.id

    new_member = FamilyMember(
        id=str(uuid4()),
        user_id=user_id,
        name=member.name,
        age_group=member.age_group,
        dietary_restrictions=member.dietary_restrictions,
        health_conditions=member.health_conditions,
    )

    db.add(new_member)
    await db.commit()
    await db.refresh(new_member)

    logger.info(f"Created family member: {new_member.name} for user {user_id}")
    return FamilyMemberResponse.model_validate(new_member)


@router.put("/{member_id}", response_model=FamilyMemberResponse)
async def update_family_member(
    db: DbSession,
    current_user: CurrentUser,
    member_id: str,
    member_update: FamilyMemberUpdate,
) -> FamilyMemberResponse:
    """Update a family member."""
    user_id = current_user.id

    result = await db.execute(
        select(FamilyMember).where(
            FamilyMember.id == member_id,
            FamilyMember.user_id == user_id,
        )
    )
    member = result.scalar_one_or_none()

    if not member:
        raise NotFoundError(f"Family member {member_id} not found")

    # Update only provided fields
    update_data = member_update.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(member, field, value)

    await db.commit()
    await db.refresh(member)

    logger.info(f"Updated family member: {member_id}")
    return FamilyMemberResponse.model_validate(member)


@router.delete("/{member_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_family_member(
    db: DbSession,
    current_user: CurrentUser,
    member_id: str,
) -> None:
    """Delete a family member."""
    user_id = current_user.id

    result = await db.execute(
        delete(FamilyMember).where(
            FamilyMember.id == member_id,
            FamilyMember.user_id == user_id,
        )
    )

    if result.rowcount == 0:
        raise NotFoundError(f"Family member {member_id} not found")

    await db.commit()
    logger.info(f"Deleted family member: {member_id}")
