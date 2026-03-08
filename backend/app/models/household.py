"""Household and membership database models."""

import uuid
from datetime import date
from typing import TYPE_CHECKING, Optional

from sqlalchemy import Boolean, Date, DateTime, ForeignKey, Integer, String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base, TimestampMixin
from app.models.user import JSONList

if TYPE_CHECKING:
    from app.models.user import FamilyMember, User


class Household(Base, TimestampMixin):
    """Household model — a family unit that shares meal plans."""

    __tablename__ = "households"

    id: Mapped[str] = mapped_column(
        String(36),
        primary_key=True,
        default=lambda: str(uuid.uuid4()),
    )
    name: Mapped[str] = mapped_column(String(100), nullable=False)
    invite_code: Mapped[Optional[str]] = mapped_column(
        String(8), unique=True, index=True, nullable=True
    )
    invite_code_expires_at: Mapped[Optional] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    owner_id: Mapped[str] = mapped_column(
        String(36),
        ForeignKey("users.id", ondelete="CASCADE"),
        nullable=False,
    )
    slot_config: Mapped[Optional[list]] = mapped_column(
        JSONList,
        nullable=True,
        default=None,
    )
    max_members: Mapped[int] = mapped_column(Integer, default=6, nullable=False)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)

    # Relationships
    owner: Mapped["User"] = relationship(
        "User",
        foreign_keys=[owner_id],
        backref="owned_households",
    )
    members: Mapped[list["HouseholdMember"]] = relationship(
        "HouseholdMember",
        back_populates="household",
        cascade="all, delete-orphan",
    )


class HouseholdMember(Base, TimestampMixin):
    """Membership linking a user to a household with role and permissions."""

    __tablename__ = "household_members"

    id: Mapped[str] = mapped_column(
        String(36),
        primary_key=True,
        default=lambda: str(uuid.uuid4()),
    )
    household_id: Mapped[str] = mapped_column(
        String(36),
        ForeignKey("households.id", ondelete="CASCADE"),
        nullable=False,
    )
    user_id: Mapped[Optional[str]] = mapped_column(
        String(36),
        ForeignKey("users.id", ondelete="SET NULL"),
        nullable=True,
    )
    family_member_id: Mapped[Optional[str]] = mapped_column(
        String(36),
        ForeignKey("family_members.id", ondelete="SET NULL"),
        nullable=True,
    )

    # Role and permissions
    role: Mapped[str] = mapped_column(
        String(20), nullable=False, default="MEMBER"
    )  # OWNER, MEMBER, GUEST
    can_edit_shared_plan: Mapped[bool] = mapped_column(
        Boolean, default=False, nullable=False
    )
    is_temporary: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)

    # Dates
    join_date: Mapped[Optional[date]] = mapped_column(Date, nullable=True)
    leave_date: Mapped[Optional[date]] = mapped_column(Date, nullable=True)

    # Guest return tracking
    previous_household_id: Mapped[Optional[str]] = mapped_column(
        String(36), nullable=True
    )

    # Per-member customization
    portion_size: Mapped[str] = mapped_column(
        String(20), default="REGULAR", nullable=False
    )  # SMALL, REGULAR, LARGE
    active_meal_slots: Mapped[Optional[list]] = mapped_column(JSONList, nullable=True)

    # Status
    status: Mapped[str] = mapped_column(
        String(20), default="ACTIVE", nullable=False
    )  # ACTIVE, PAUSED, LEFT

    # Relationships
    household: Mapped["Household"] = relationship("Household", back_populates="members")
    user: Mapped[Optional["User"]] = relationship(
        "User",
        foreign_keys=[user_id],
    )
    family_member: Mapped[Optional["FamilyMember"]] = relationship(
        "FamilyMember",
        foreign_keys=[family_member_id],
    )
