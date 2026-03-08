"""Meal plan database models."""

import uuid
from datetime import date
from typing import TYPE_CHECKING, Optional

from sqlalchemy import Boolean, Date, ForeignKey, Integer, String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base, TimestampMixin

if TYPE_CHECKING:
    from app.models.recipe import Recipe
    from app.models.user import User


class MealPlan(Base, TimestampMixin):
    """Weekly meal plan model."""

    __tablename__ = "meal_plans"

    id: Mapped[str] = mapped_column(
        String(36),
        primary_key=True,
        default=lambda: str(uuid.uuid4()),
    )
    user_id: Mapped[str] = mapped_column(
        String(36),
        ForeignKey("users.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )

    week_start_date: Mapped[date] = mapped_column(Date, nullable=False, index=True)
    week_end_date: Mapped[date] = mapped_column(Date, nullable=False)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)

    # Household scoping
    household_id: Mapped[Optional[str]] = mapped_column(
        String(36),
        ForeignKey("households.id", ondelete="SET NULL"),
        nullable=True,
        index=True,
    )
    slot_scope: Mapped[str] = mapped_column(
        String(20), default="ALL", nullable=False
    )  # ALL, SHARED, PERSONAL

    # Relationships
    user: Mapped["User"] = relationship("User", back_populates="meal_plans")
    items: Mapped[list["MealPlanItem"]] = relationship(
        "MealPlanItem",
        back_populates="meal_plan",
        cascade="all, delete-orphan",
        order_by="MealPlanItem.date, MealPlanItem.meal_type",
    )


class MealPlanItem(Base, TimestampMixin):
    """Individual meal item in a meal plan."""

    __tablename__ = "meal_plan_items"

    id: Mapped[str] = mapped_column(
        String(36),
        primary_key=True,
        default=lambda: str(uuid.uuid4()),
    )
    meal_plan_id: Mapped[str] = mapped_column(
        String(36),
        ForeignKey("meal_plans.id", ondelete="CASCADE"),
        nullable=False,
    )
    recipe_id: Mapped[Optional[str]] = mapped_column(
        String(36),
        ForeignKey("recipes.id", ondelete="SET NULL"),
        nullable=True,
    )

    date: Mapped[date] = mapped_column(Date, nullable=False, index=True)
    meal_type: Mapped[str] = mapped_column(
        String(20), nullable=False
    )  # breakfast, lunch, dinner, snacks
    servings: Mapped[int] = mapped_column(Integer, nullable=False, default=2)
    is_locked: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    is_swapped: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)

    # Cached recipe name for display
    recipe_name: Mapped[Optional[str]] = mapped_column(String(255), nullable=True)

    # Optional festival info for this day
    festival_name: Mapped[Optional[str]] = mapped_column(String(100), nullable=True)

    # Household scoping
    scope: Mapped[str] = mapped_column(
        String(20), default="FAMILY", nullable=False
    )  # FAMILY or PERSONAL
    for_user_id: Mapped[Optional[str]] = mapped_column(
        String(36),
        ForeignKey("users.id", ondelete="SET NULL"),
        nullable=True,
    )
    meal_status: Mapped[str] = mapped_column(
        String(20), default="PLANNED", nullable=False
    )  # PLANNED, COOKED, SKIPPED, ORDERED_OUT

    # Relationships
    meal_plan: Mapped["MealPlan"] = relationship("MealPlan", back_populates="items")
    recipe: Mapped[Optional["Recipe"]] = relationship(
        "Recipe", back_populates="meal_plan_items"
    )
