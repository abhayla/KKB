"""Recipe rule and nutrition goal database models."""

import uuid
from typing import TYPE_CHECKING, Optional

from sqlalchemy import Boolean, ForeignKey, Integer, String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base, TimestampMixin

if TYPE_CHECKING:
    from app.models.user import User


class RecipeRule(Base, TimestampMixin):
    """Recipe rule model for INCLUDE/EXCLUDE rules.

    Rules can target recipes, ingredients, or meal slots with various
    frequency options (daily, times per week, specific days, never).
    """

    __tablename__ = "recipe_rules"

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

    # Rule type and action
    target_type: Mapped[str] = mapped_column(
        String(20), nullable=False
    )  # RECIPE, INGREDIENT, MEAL_SLOT
    action: Mapped[str] = mapped_column(
        String(10), nullable=False
    )  # INCLUDE, EXCLUDE
    target_id: Mapped[Optional[str]] = mapped_column(
        String(255), nullable=True
    )  # Recipe ID (optional)
    target_name: Mapped[str] = mapped_column(
        String(255), nullable=False
    )  # Display name

    # Frequency settings
    frequency_type: Mapped[str] = mapped_column(
        String(20), nullable=False
    )  # DAILY, TIMES_PER_WEEK, SPECIFIC_DAYS, NEVER
    frequency_count: Mapped[Optional[int]] = mapped_column(
        Integer, nullable=True
    )  # For TIMES_PER_WEEK (1-7)
    frequency_days: Mapped[Optional[str]] = mapped_column(
        String(100), nullable=True
    )  # Comma-separated: "MONDAY,WEDNESDAY"

    # Enforcement and meal slot
    enforcement: Mapped[str] = mapped_column(
        String(10), default="REQUIRED", nullable=False
    )  # REQUIRED, PREFERRED
    meal_slot: Mapped[Optional[str]] = mapped_column(
        String(20), nullable=True
    )  # BREAKFAST, LUNCH, DINNER, SNACKS

    # Status fields
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
    sync_status: Mapped[str] = mapped_column(
        String(20), default="SYNCED", nullable=False
    )  # SYNCED, PENDING, CONFLICT

    # Relationships
    user: Mapped["User"] = relationship("User", back_populates="recipe_rules")


class NutritionGoal(Base, TimestampMixin):
    """Nutrition goal model for tracking weekly food category targets.

    Goals track consumption of food categories like leafy greens,
    proteins, fermented foods, etc.
    """

    __tablename__ = "nutrition_goals"

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

    # Goal details
    food_category: Mapped[str] = mapped_column(
        String(30), nullable=False
    )  # LEAFY_GREENS, PROTEIN, FERMENTED, etc.
    weekly_target: Mapped[int] = mapped_column(Integer, default=3, nullable=False)
    current_progress: Mapped[int] = mapped_column(Integer, default=0, nullable=False)

    # Enforcement and status
    enforcement: Mapped[str] = mapped_column(
        String(10), default="PREFERRED", nullable=False
    )  # REQUIRED, PREFERRED
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
    sync_status: Mapped[str] = mapped_column(
        String(20), default="SYNCED", nullable=False
    )  # SYNCED, PENDING, CONFLICT

    # Relationships
    user: Mapped["User"] = relationship("User", back_populates="nutrition_goals")
