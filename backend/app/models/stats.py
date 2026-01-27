"""Stats and gamification database models."""

import uuid
from datetime import date
from typing import TYPE_CHECKING, Optional

from sqlalchemy import Boolean, Date, ForeignKey, Integer, String, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base, TimestampMixin

if TYPE_CHECKING:
    from app.models.user import User


class CookingStreak(Base, TimestampMixin):
    """Cooking streak tracking for gamification."""

    __tablename__ = "cooking_streaks"

    id: Mapped[str] = mapped_column(
        String(36),
        primary_key=True,
        default=lambda: str(uuid.uuid4()),
    )
    user_id: Mapped[str] = mapped_column(
        String(36),
        ForeignKey("users.id", ondelete="CASCADE"),
        unique=True,
        nullable=False,
    )

    current_streak: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
    longest_streak: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
    total_meals_cooked: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
    last_cooking_date: Mapped[Optional[date]] = mapped_column(Date, nullable=True)

    # Relationships
    user: Mapped["User"] = relationship("User", back_populates="cooking_streak")
    cooking_days: Mapped[list["CookingDay"]] = relationship(
        "CookingDay",
        back_populates="cooking_streak",
        cascade="all, delete-orphan",
    )


class CookingDay(Base):
    """Daily cooking record."""

    __tablename__ = "cooking_days"

    id: Mapped[str] = mapped_column(
        String(36),
        primary_key=True,
        default=lambda: str(uuid.uuid4()),
    )
    cooking_streak_id: Mapped[str] = mapped_column(
        String(36),
        ForeignKey("cooking_streaks.id", ondelete="CASCADE"),
        nullable=False,
    )

    date: Mapped[date] = mapped_column(Date, nullable=False, index=True)
    meals_cooked: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
    breakfast_cooked: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    lunch_cooked: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    dinner_cooked: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)

    # Relationships
    cooking_streak: Mapped["CookingStreak"] = relationship(
        "CookingStreak", back_populates="cooking_days"
    )


class Achievement(Base):
    """Achievement definitions."""

    __tablename__ = "achievements"

    id: Mapped[str] = mapped_column(
        String(36),
        primary_key=True,
        default=lambda: str(uuid.uuid4()),
    )
    name: Mapped[str] = mapped_column(String(100), nullable=False, unique=True)
    description: Mapped[str] = mapped_column(Text, nullable=False)
    icon: Mapped[str] = mapped_column(String(50), nullable=False)  # emoji or icon name
    category: Mapped[str] = mapped_column(
        String(50), nullable=False
    )  # streak, variety, health, etc.
    requirement_type: Mapped[str] = mapped_column(
        String(50), nullable=False
    )  # streak_days, meals_cooked, cuisines_tried, etc.
    requirement_value: Mapped[int] = mapped_column(Integer, nullable=False)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)


class UserAchievement(Base, TimestampMixin):
    """User's unlocked achievements."""

    __tablename__ = "user_achievements"

    id: Mapped[str] = mapped_column(
        String(36),
        primary_key=True,
        default=lambda: str(uuid.uuid4()),
    )
    user_id: Mapped[str] = mapped_column(
        String(36),
        ForeignKey("users.id", ondelete="CASCADE"),
        nullable=False,
    )
    achievement_id: Mapped[str] = mapped_column(
        String(36),
        ForeignKey("achievements.id", ondelete="CASCADE"),
        nullable=False,
    )

    unlocked_at: Mapped[date] = mapped_column(Date, nullable=False)

    # Relationships
    user: Mapped["User"] = relationship("User", back_populates="achievements")
    achievement: Mapped["Achievement"] = relationship("Achievement")
