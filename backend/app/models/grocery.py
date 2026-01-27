"""Grocery list database models."""

import uuid
from typing import TYPE_CHECKING, Optional

from sqlalchemy import Boolean, Float, ForeignKey, String, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base, TimestampMixin

if TYPE_CHECKING:
    from app.models.meal_plan import MealPlan
    from app.models.user import User


class GroceryList(Base, TimestampMixin):
    """Aggregated grocery list for a meal plan."""

    __tablename__ = "grocery_lists"

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
    meal_plan_id: Mapped[Optional[str]] = mapped_column(
        String(36),
        ForeignKey("meal_plans.id", ondelete="SET NULL"),
        nullable=True,
    )

    name: Mapped[str] = mapped_column(String(100), nullable=False)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)

    # Relationships
    user: Mapped["User"] = relationship("User", back_populates="grocery_lists")
    items: Mapped[list["GroceryItem"]] = relationship(
        "GroceryItem",
        back_populates="grocery_list",
        cascade="all, delete-orphan",
        order_by="GroceryItem.category, GroceryItem.name",
    )


class GroceryItem(Base, TimestampMixin):
    """Individual grocery item."""

    __tablename__ = "grocery_items"

    id: Mapped[str] = mapped_column(
        String(36),
        primary_key=True,
        default=lambda: str(uuid.uuid4()),
    )
    grocery_list_id: Mapped[str] = mapped_column(
        String(36),
        ForeignKey("grocery_lists.id", ondelete="CASCADE"),
        nullable=False,
    )

    name: Mapped[str] = mapped_column(String(100), nullable=False)
    quantity: Mapped[float] = mapped_column(Float, nullable=False)
    unit: Mapped[str] = mapped_column(String(30), nullable=False)
    category: Mapped[str] = mapped_column(
        String(30), nullable=False, index=True
    )  # vegetables, spices, etc.
    notes: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    is_checked: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    is_in_pantry: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)

    # Relationships
    grocery_list: Mapped["GroceryList"] = relationship(
        "GroceryList", back_populates="items"
    )
