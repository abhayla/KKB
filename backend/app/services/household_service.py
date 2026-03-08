"""Household management service.

Handles creation, membership, and lifecycle of households.
"""

import logging
import random
import string
import uuid
from datetime import date, datetime, timedelta, timezone

from sqlalchemy import select, func
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.core.exceptions import (
    BadRequestError,
    ConflictError,
    ForbiddenError,
    NotFoundError,
)
from app.models.household import Household, HouseholdMember
from app.models.user import User

logger = logging.getLogger(__name__)


class HouseholdService:
    """Service for household CRUD and membership management."""

    @staticmethod
    async def create(name: str, user: User, db: AsyncSession) -> Household:
        """Create a new household. The caller becomes OWNER with can_edit=True.

        Args:
            name: Display name for the household.
            user: The authenticated user who will become the owner.
            db: Async database session.

        Returns:
            The newly created Household.
        """
        household_id = str(uuid.uuid4())
        household = Household(
            id=household_id,
            name=name,
            owner_id=str(user.id),
            invite_code=None,
        )
        db.add(household)

        member = HouseholdMember(
            id=str(uuid.uuid4()),
            household_id=household_id,
            user_id=str(user.id),
            role="OWNER",
            can_edit_shared_plan=True,
            status="ACTIVE",
            join_date=date.today(),
        )
        db.add(member)

        await db.flush()
        logger.info("Household %s created by user %s", household_id, user.id)
        return household

    @staticmethod
    async def get(household_id: str, user: User, db: AsyncSession) -> Household:
        """Get a household by ID with members eagerly loaded.

        Args:
            household_id: The household UUID.
            user: The authenticated user (must be an active member).
            db: Async database session.

        Returns:
            The Household with members loaded.

        Raises:
            NotFoundError: If the household does not exist.
            ForbiddenError: If the user is not an active member.
        """
        result = await db.execute(
            select(Household)
            .options(selectinload(Household.members))
            .where(Household.id == str(household_id))
        )
        household = result.scalar_one_or_none()

        if not household:
            raise NotFoundError(f"Household {household_id} not found")

        await HouseholdService._verify_membership(household_id, user, db)
        return household

    @staticmethod
    async def update(
        household_id: str,
        name: str | None,
        max_members: int | None,
        user: User,
        db: AsyncSession,
    ) -> Household:
        """Update household details. Owner only.

        Args:
            household_id: The household UUID.
            name: New display name (or None to keep current).
            max_members: New member cap (or None to keep current).
            user: The authenticated user (must be owner).
            db: Async database session.

        Returns:
            The updated Household.

        Raises:
            ForbiddenError: If the user is not the owner.
        """
        household, _ = await HouseholdService._verify_owner(household_id, user, db)

        if name is not None:
            household.name = name
        if max_members is not None:
            household.max_members = max_members

        await db.flush()
        logger.info("Household %s updated by user %s", household_id, user.id)
        return household

    @staticmethod
    async def deactivate(household_id: str, user: User, db: AsyncSession) -> None:
        """Soft-deactivate a household. Owner only.

        The household must have no other ACTIVE linked members (besides
        the owner). If others remain, raise BadRequestError so the owner
        can remove or transfer them first.

        Args:
            household_id: The household UUID.
            user: The authenticated user (must be owner).
            db: Async database session.

        Raises:
            ForbiddenError: If the user is not the owner.
            BadRequestError: If other active members still exist.
        """
        household, owner_member = await HouseholdService._verify_owner(
            household_id, user, db
        )

        active_count = await HouseholdService._count_active_members(household_id, db)
        if active_count > 1:
            raise BadRequestError(
                "Cannot deactivate household with other active members. "
                "Remove or transfer them first."
            )

        household.is_active = False
        owner_member.status = "LEFT"

        await db.flush()
        logger.info(
            "Household %s deactivated by user %s",
            household_id,
            user.id,
        )

    @staticmethod
    async def list_members(
        household_id: str, user: User, db: AsyncSession
    ) -> list[HouseholdMember]:
        """List all members of a household.

        Args:
            household_id: The household UUID.
            user: The authenticated user (must be an active member).
            db: Async database session.

        Returns:
            List of HouseholdMember records.

        Raises:
            ForbiddenError: If the user is not an active member.
        """
        await HouseholdService._verify_membership(household_id, user, db)

        result = await db.execute(
            select(HouseholdMember).where(
                HouseholdMember.household_id == str(household_id)
            )
        )
        return list(result.scalars().all())

    @staticmethod
    async def add_member_by_phone(
        household_id: str,
        phone_number: str,
        user: User,
        db: AsyncSession,
    ) -> HouseholdMember:
        """Add a member to the household by phone number. Owner only.

        If a user with the given phone number exists, they are linked.
        Otherwise a metadata-only placeholder member is created with
        user_id=None.

        Args:
            household_id: The household UUID.
            phone_number: Phone number to look up.
            user: The authenticated user (must be owner).
            db: Async database session.

        Returns:
            The newly created HouseholdMember.

        Raises:
            ForbiddenError: If the user is not the owner.
            BadRequestError: If the household is at max capacity.
            ConflictError: If the user is already an active member.
        """
        household, _ = await HouseholdService._verify_owner(household_id, user, db)

        # Check capacity
        active_count = await HouseholdService._count_active_members(household_id, db)
        if active_count >= household.max_members:
            raise BadRequestError(
                f"Household is at maximum capacity "
                f"({household.max_members} members)"
            )

        # Look up target user by phone number
        result = await db.execute(select(User).where(User.phone_number == phone_number))
        target_user = result.scalar_one_or_none()

        target_user_id = str(target_user.id) if target_user else None

        # Check for duplicate active membership
        if target_user_id is not None:
            dup_result = await db.execute(
                select(HouseholdMember).where(
                    HouseholdMember.household_id == str(household_id),
                    HouseholdMember.user_id == target_user_id,
                    HouseholdMember.status == "ACTIVE",
                )
            )
            if dup_result.scalar_one_or_none() is not None:
                raise ConflictError(
                    "User is already an active member of this household"
                )

        member = HouseholdMember(
            id=str(uuid.uuid4()),
            household_id=str(household_id),
            user_id=target_user_id,
            role="MEMBER",
            can_edit_shared_plan=False,
            status="ACTIVE",
            join_date=date.today(),
        )
        db.add(member)

        await db.flush()
        logger.info(
            "Member added to household %s (phone=%s, linked_user=%s)",
            household_id,
            phone_number,
            target_user_id,
        )
        return member

    @staticmethod
    async def refresh_invite_code(
        household_id: str, user: User, db: AsyncSession
    ) -> tuple[str, datetime]:
        """Generate/refresh 8-char invite code with 7-day expiry. Owner only.

        Args:
            household_id: The household UUID.
            user: The authenticated user (must be owner).
            db: Async database session.

        Returns:
            Tuple of (invite_code, expires_at).

        Raises:
            ForbiddenError: If the user is not the owner.
        """
        household, _ = await HouseholdService._verify_owner(household_id, user, db)

        code = "".join(random.choices(string.ascii_uppercase + string.digits, k=8))
        expires_at = datetime.now(timezone.utc) + timedelta(days=7)

        household.invite_code = code
        household.invite_code_expires_at = expires_at

        await db.flush()
        logger.info(
            "Invite code refreshed for household %s by user %s",
            household_id,
            user.id,
        )
        return code, expires_at

    @staticmethod
    async def join(invite_code: str, user: User, db: AsyncSession) -> HouseholdMember:
        """Join household via invite code.

        Validates the code exists, is not expired, the household is active,
        and is not at maximum capacity. The user must not already be an
        active member.

        Args:
            invite_code: The 8-char invite code.
            user: The authenticated user wanting to join.
            db: Async database session.

        Returns:
            The newly created HouseholdMember.

        Raises:
            NotFoundError: If the code is invalid or expired.
            BadRequestError: If the household is at capacity.
            ConflictError: If the user is already an active member.
        """
        result = await db.execute(
            select(Household).where(
                Household.invite_code == invite_code,
            )
        )
        household = result.scalar_one_or_none()

        if not household:
            raise NotFoundError("Invalid invite code")

        if (
            household.invite_code_expires_at is not None
            and household.invite_code_expires_at < datetime.now(timezone.utc)
        ):
            raise NotFoundError("Invite code has expired")

        if not household.is_active:
            raise NotFoundError("Invalid invite code")

        # Check capacity
        active_count = await HouseholdService._count_active_members(household.id, db)
        if active_count >= household.max_members:
            raise BadRequestError(
                f"Household is at maximum capacity "
                f"({household.max_members} members)"
            )

        # Check user not already active member
        dup_result = await db.execute(
            select(HouseholdMember).where(
                HouseholdMember.household_id == household.id,
                HouseholdMember.user_id == str(user.id),
                HouseholdMember.status == "ACTIVE",
            )
        )
        if dup_result.scalar_one_or_none() is not None:
            raise ConflictError("You are already an active member of this household")

        member = HouseholdMember(
            id=str(uuid.uuid4()),
            household_id=household.id,
            user_id=str(user.id),
            role="MEMBER",
            can_edit_shared_plan=False,
            status="ACTIVE",
            join_date=date.today(),
            previous_household_id=getattr(user, "active_household_id", None),
        )
        db.add(member)

        await db.flush()
        logger.info(
            "User %s joined household %s via invite code",
            user.id,
            household.id,
        )
        return member

    @staticmethod
    async def leave(household_id: str, user: User, db: AsyncSession) -> None:
        """Leave household. Cannot leave if OWNER (must transfer first).

        Args:
            household_id: The household UUID.
            user: The authenticated user.
            db: Async database session.

        Raises:
            ForbiddenError: If the user is not an active member.
            BadRequestError: If the user is the OWNER.
        """
        member = await HouseholdService._verify_membership(household_id, user, db)

        if member.role == "OWNER":
            raise BadRequestError("Owner cannot leave. Transfer ownership first.")

        member.status = "LEFT"
        await db.flush()
        logger.info("User %s left household %s", user.id, household_id)

    @staticmethod
    async def transfer(
        household_id: str,
        new_owner_member_id: str,
        user: User,
        db: AsyncSession,
    ) -> None:
        """Transfer ownership. Owner only.

        The target member must be ACTIVE with a linked user_id
        (not metadata-only).

        Args:
            household_id: The household UUID.
            new_owner_member_id: HouseholdMember ID of the new owner.
            user: The authenticated user (must be current owner).
            db: Async database session.

        Raises:
            ForbiddenError: If the user is not the owner.
            NotFoundError: If the target member does not exist.
            BadRequestError: If the target is metadata-only or not ACTIVE.
        """
        household, old_owner_member = await HouseholdService._verify_owner(
            household_id, user, db
        )

        result = await db.execute(
            select(HouseholdMember).where(
                HouseholdMember.id == new_owner_member_id,
                HouseholdMember.household_id == str(household_id),
            )
        )
        new_owner_member = result.scalar_one_or_none()

        if not new_owner_member:
            raise NotFoundError("Target member not found in this household")

        if new_owner_member.status != "ACTIVE":
            raise BadRequestError("Target member is not active")

        if new_owner_member.user_id is None:
            raise BadRequestError("Cannot transfer ownership to a metadata-only member")

        old_owner_member.role = "MEMBER"
        new_owner_member.role = "OWNER"
        new_owner_member.can_edit_shared_plan = True
        household.owner_id = new_owner_member.user_id

        await db.flush()
        logger.info(
            "Household %s ownership transferred from user %s to member %s",
            household_id,
            user.id,
            new_owner_member_id,
        )

    @staticmethod
    async def update_member(
        household_id: str,
        member_id: str,
        data: dict,
        user: User,
        db: AsyncSession,
    ) -> HouseholdMember:
        """Update member attributes. Owner only.

        Args:
            household_id: The household UUID.
            member_id: The HouseholdMember UUID to update.
            data: Dict of fields to update (can_edit_shared_plan,
                  portion_size, is_temporary, leave_date, role).
            user: The authenticated user (must be owner).
            db: Async database session.

        Returns:
            The updated HouseholdMember.

        Raises:
            ForbiddenError: If the user is not the owner.
            NotFoundError: If the member does not exist.
            BadRequestError: If trying to change the OWNER's role.
        """
        await HouseholdService._verify_owner(household_id, user, db)

        result = await db.execute(
            select(HouseholdMember).where(
                HouseholdMember.id == member_id,
                HouseholdMember.household_id == str(household_id),
            )
        )
        member = result.scalar_one_or_none()

        if not member:
            raise NotFoundError("Member not found in this household")

        if member.role == "OWNER" and "role" in data:
            raise BadRequestError("Cannot change the owner's role")

        allowed_fields = {
            "can_edit_shared_plan",
            "portion_size",
            "is_temporary",
            "leave_date",
            "role",
        }
        for field, value in data.items():
            if field in allowed_fields:
                setattr(member, field, value)

        await db.flush()
        logger.info(
            "Member %s updated in household %s by user %s",
            member_id,
            household_id,
            user.id,
        )
        return member

    @staticmethod
    async def remove_member(
        household_id: str,
        member_id: str,
        user: User,
        db: AsyncSession,
    ) -> None:
        """Remove member. Owner only. Cannot remove self.

        Args:
            household_id: The household UUID.
            member_id: The HouseholdMember UUID to remove.
            user: The authenticated user (must be owner).
            db: Async database session.

        Raises:
            ForbiddenError: If the user is not the owner.
            NotFoundError: If the member does not exist.
            BadRequestError: If trying to remove self.
        """
        _, owner_member = await HouseholdService._verify_owner(household_id, user, db)

        if owner_member.id == member_id:
            raise BadRequestError("Cannot remove yourself. Transfer ownership first.")

        result = await db.execute(
            select(HouseholdMember).where(
                HouseholdMember.id == member_id,
                HouseholdMember.household_id == str(household_id),
            )
        )
        member = result.scalar_one_or_none()

        if not member:
            raise NotFoundError("Member not found in this household")

        member.status = "LEFT"
        await db.flush()
        logger.info(
            "Member %s removed from household %s by user %s",
            member_id,
            household_id,
            user.id,
        )

    # ------------------------------------------------------------------
    # Private helpers
    # ------------------------------------------------------------------

    @staticmethod
    async def _verify_membership(
        household_id: str, user: User, db: AsyncSession
    ) -> HouseholdMember:
        """Verify the user is an ACTIVE member of the household.

        Args:
            household_id: The household UUID.
            user: The authenticated user.
            db: Async database session.

        Returns:
            The user's HouseholdMember record.

        Raises:
            ForbiddenError: If the user is not an active member.
        """
        result = await db.execute(
            select(HouseholdMember).where(
                HouseholdMember.household_id == str(household_id),
                HouseholdMember.user_id == str(user.id),
                HouseholdMember.status == "ACTIVE",
            )
        )
        member = result.scalar_one_or_none()

        if not member:
            raise ForbiddenError("You are not an active member of this household")
        return member

    @staticmethod
    async def _verify_owner(
        household_id: str, user: User, db: AsyncSession
    ) -> tuple[Household, HouseholdMember]:
        """Verify the user is the OWNER of the household.

        Args:
            household_id: The household UUID.
            user: The authenticated user.
            db: Async database session.

        Returns:
            Tuple of (Household, owner's HouseholdMember record).

        Raises:
            NotFoundError: If the household does not exist.
            ForbiddenError: If the user is not the owner.
        """
        result = await db.execute(
            select(Household).where(Household.id == str(household_id))
        )
        household = result.scalar_one_or_none()

        if not household:
            raise NotFoundError(f"Household {household_id} not found")

        result = await db.execute(
            select(HouseholdMember).where(
                HouseholdMember.household_id == str(household_id),
                HouseholdMember.user_id == str(user.id),
                HouseholdMember.role == "OWNER",
                HouseholdMember.status == "ACTIVE",
            )
        )
        member = result.scalar_one_or_none()

        if not member:
            raise ForbiddenError("Only the household owner can perform this action")
        return household, member

    @staticmethod
    async def _count_active_members(household_id: str, db: AsyncSession) -> int:
        """Count ACTIVE members in a household.

        Args:
            household_id: The household UUID.
            db: Async database session.

        Returns:
            Number of active members.
        """
        result = await db.execute(
            select(func.count())
            .select_from(HouseholdMember)
            .where(
                HouseholdMember.household_id == str(household_id),
                HouseholdMember.status == "ACTIVE",
            )
        )
        return result.scalar_one()
