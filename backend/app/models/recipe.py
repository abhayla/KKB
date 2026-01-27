"""Recipe-related database models."""

import uuid
from typing import TYPE_CHECKING, Optional

from sqlalchemy import Boolean, Float, ForeignKey, Integer, String, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base, TimestampMixin
from app.models.user import JSONList

if TYPE_CHECKING:
    from app.models.meal_plan import MealPlanItem


class Recipe(Base, TimestampMixin):
    """Recipe catalog model."""

    __tablename__ = "recipes"

    id: Mapped[str] = mapped_column(
        String(36),
        primary_key=True,
        default=lambda: str(uuid.uuid4()),
    )
    name: Mapped[str] = mapped_column(String(255), nullable=False, index=True)
    description: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    image_url: Mapped[Optional[str]] = mapped_column(Text, nullable=True)

    # Classification
    cuisine_type: Mapped[str] = mapped_column(
        String(20), nullable=False, index=True
    )  # north, south, east, west
    meal_types: Mapped[list[str]] = mapped_column(
        JSONList, nullable=False
    )  # breakfast, lunch, dinner, snacks
    dietary_tags: Mapped[list[str]] = mapped_column(
        JSONList, nullable=False
    )  # vegetarian, vegan, jain, etc.
    course_type: Mapped[Optional[str]] = mapped_column(
        String(50), nullable=True
    )  # main, side, dessert, beverage

    # Timing
    prep_time_minutes: Mapped[int] = mapped_column(Integer, nullable=False, default=15)
    cook_time_minutes: Mapped[int] = mapped_column(Integer, nullable=False, default=30)
    total_time_minutes: Mapped[int] = mapped_column(Integer, nullable=False, default=45)

    # Servings
    servings: Mapped[int] = mapped_column(Integer, nullable=False, default=4)

    # Difficulty
    difficulty_level: Mapped[Optional[str]] = mapped_column(
        String(20), nullable=True
    )  # easy, medium, hard

    # Flags
    is_festive: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    is_fasting_friendly: Mapped[bool] = mapped_column(
        Boolean, default=False, nullable=False
    )
    is_quick_meal: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    is_kid_friendly: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)

    # Relationships
    ingredients: Mapped[list["RecipeIngredient"]] = relationship(
        "RecipeIngredient",
        back_populates="recipe",
        cascade="all, delete-orphan",
        order_by="RecipeIngredient.order",
    )
    instructions: Mapped[list["RecipeInstruction"]] = relationship(
        "RecipeInstruction",
        back_populates="recipe",
        cascade="all, delete-orphan",
        order_by="RecipeInstruction.step_number",
    )
    nutrition: Mapped[Optional["RecipeNutrition"]] = relationship(
        "RecipeNutrition",
        back_populates="recipe",
        uselist=False,
        cascade="all, delete-orphan",
    )
    meal_plan_items: Mapped[list["MealPlanItem"]] = relationship(
        "MealPlanItem",
        back_populates="recipe",
    )


class RecipeIngredient(Base):
    """Individual ingredient for a recipe."""

    __tablename__ = "recipe_ingredients"

    id: Mapped[str] = mapped_column(
        String(36),
        primary_key=True,
        default=lambda: str(uuid.uuid4()),
    )
    recipe_id: Mapped[str] = mapped_column(
        String(36),
        ForeignKey("recipes.id", ondelete="CASCADE"),
        nullable=False,
    )

    name: Mapped[str] = mapped_column(String(100), nullable=False)
    quantity: Mapped[float] = mapped_column(Float, nullable=False)
    unit: Mapped[str] = mapped_column(String(30), nullable=False)  # grams, cups, tbsp, etc.
    category: Mapped[str] = mapped_column(
        String(30), nullable=False
    )  # vegetables, spices, grains, etc.
    notes: Mapped[Optional[str]] = mapped_column(String(255), nullable=True)
    is_optional: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    order: Mapped[int] = mapped_column(Integer, default=0, nullable=False)

    # Relationships
    recipe: Mapped["Recipe"] = relationship("Recipe", back_populates="ingredients")


class RecipeInstruction(Base):
    """Step-by-step instruction for a recipe."""

    __tablename__ = "recipe_instructions"

    id: Mapped[str] = mapped_column(
        String(36),
        primary_key=True,
        default=lambda: str(uuid.uuid4()),
    )
    recipe_id: Mapped[str] = mapped_column(
        String(36),
        ForeignKey("recipes.id", ondelete="CASCADE"),
        nullable=False,
    )

    step_number: Mapped[int] = mapped_column(Integer, nullable=False)
    instruction: Mapped[str] = mapped_column(Text, nullable=False)
    duration_minutes: Mapped[Optional[int]] = mapped_column(Integer, nullable=True)
    timer_required: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    tips: Mapped[Optional[str]] = mapped_column(Text, nullable=True)

    # Relationships
    recipe: Mapped["Recipe"] = relationship("Recipe", back_populates="instructions")


class RecipeNutrition(Base):
    """Nutritional information for a recipe (per serving)."""

    __tablename__ = "recipe_nutrition"

    id: Mapped[str] = mapped_column(
        String(36),
        primary_key=True,
        default=lambda: str(uuid.uuid4()),
    )
    recipe_id: Mapped[str] = mapped_column(
        String(36),
        ForeignKey("recipes.id", ondelete="CASCADE"),
        unique=True,
        nullable=False,
    )

    calories: Mapped[int] = mapped_column(Integer, nullable=False)
    protein_grams: Mapped[float] = mapped_column(Float, nullable=False)
    carbohydrates_grams: Mapped[float] = mapped_column(Float, nullable=False)
    fat_grams: Mapped[float] = mapped_column(Float, nullable=False)
    fiber_grams: Mapped[float] = mapped_column(Float, nullable=False)
    sugar_grams: Mapped[Optional[float]] = mapped_column(Float, nullable=True)
    sodium_mg: Mapped[Optional[float]] = mapped_column(Float, nullable=True)

    # Relationships
    recipe: Mapped["Recipe"] = relationship("Recipe", back_populates="nutrition")
