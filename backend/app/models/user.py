"""User-related database models."""

import json
import uuid
from typing import TYPE_CHECKING, Optional

from sqlalchemy import Boolean, ForeignKey, Integer, String, Text, TypeDecorator
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base, TimestampMixin

if TYPE_CHECKING:
    from app.models.chat import ChatMessage
    from app.models.grocery import GroceryList
    from app.models.meal_plan import MealPlan
    from app.models.stats import CookingStreak, UserAchievement


class JSONList(TypeDecorator):
    """Store Python list as JSON string (SQLite compatible)."""

    impl = Text
    cache_ok = True

    def process_bind_param(self, value, dialect):
        if value is not None:
            return json.dumps(value)
        return None

    def process_result_value(self, value, dialect):
        if value is not None:
            return json.loads(value)
        return None


class User(Base, TimestampMixin):
    """User account model."""

    __tablename__ = "users"

    id: Mapped[str] = mapped_column(
        String(36),
        primary_key=True,
        default=lambda: str(uuid.uuid4()),
    )
    firebase_uid: Mapped[str] = mapped_column(
        String(128),
        unique=True,
        index=True,
        nullable=False,
    )
    email: Mapped[Optional[str]] = mapped_column(String(255), nullable=True)
    name: Mapped[Optional[str]] = mapped_column(String(255), nullable=True)
    profile_picture_url: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    is_onboarded: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)

    # Relationships
    preferences: Mapped[Optional["UserPreferences"]] = relationship(
        "UserPreferences",
        back_populates="user",
        uselist=False,
        cascade="all, delete-orphan",
    )
    family_members: Mapped[list["FamilyMember"]] = relationship(
        "FamilyMember",
        back_populates="user",
        cascade="all, delete-orphan",
    )
    meal_plans: Mapped[list["MealPlan"]] = relationship(
        "MealPlan",
        back_populates="user",
        cascade="all, delete-orphan",
    )
    grocery_lists: Mapped[list["GroceryList"]] = relationship(
        "GroceryList",
        back_populates="user",
        cascade="all, delete-orphan",
    )
    chat_messages: Mapped[list["ChatMessage"]] = relationship(
        "ChatMessage",
        back_populates="user",
        cascade="all, delete-orphan",
    )
    cooking_streak: Mapped[Optional["CookingStreak"]] = relationship(
        "CookingStreak",
        back_populates="user",
        uselist=False,
        cascade="all, delete-orphan",
    )
    achievements: Mapped[list["UserAchievement"]] = relationship(
        "UserAchievement",
        back_populates="user",
        cascade="all, delete-orphan",
    )


class UserPreferences(Base, TimestampMixin):
    """User dietary and cooking preferences."""

    __tablename__ = "user_preferences"

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

    # Dietary preferences
    dietary_type: Mapped[Optional[str]] = mapped_column(
        String(50), nullable=True
    )  # vegetarian, non_vegetarian, vegan, etc.
    dietary_tags: Mapped[Optional[list[str]]] = mapped_column(
        JSONList, nullable=True
    )  # jain, sattvic, halal, etc.
    allergies: Mapped[Optional[list[str]]] = mapped_column(JSONList, nullable=True)
    disliked_ingredients: Mapped[Optional[list[str]]] = mapped_column(
        JSONList, nullable=True
    )

    # Cooking preferences
    cuisine_preferences: Mapped[Optional[list[str]]] = mapped_column(
        JSONList, nullable=True
    )  # north, south, east, west
    cooking_time_preference: Mapped[Optional[str]] = mapped_column(
        String(50), nullable=True
    )  # quick, moderate, elaborate
    spice_level: Mapped[Optional[str]] = mapped_column(
        String(20), nullable=True
    )  # mild, medium, spicy

    # Household info
    family_size: Mapped[int] = mapped_column(Integer, default=2, nullable=False)
    cooking_skill_level: Mapped[Optional[str]] = mapped_column(
        String(20), nullable=True
    )  # beginner, intermediate, advanced

    # Relationships
    user: Mapped["User"] = relationship("User", back_populates="preferences")


class FamilyMember(Base, TimestampMixin):
    """Family member details for personalized meal planning."""

    __tablename__ = "family_members"

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

    name: Mapped[str] = mapped_column(String(100), nullable=False)
    age_group: Mapped[Optional[str]] = mapped_column(
        String(20), nullable=True
    )  # child, teen, adult, senior
    dietary_restrictions: Mapped[Optional[list[str]]] = mapped_column(
        JSONList, nullable=True
    )
    health_conditions: Mapped[Optional[list[str]]] = mapped_column(
        JSONList, nullable=True
    )  # diabetes, hypertension, etc.

    # Relationships
    user: Mapped["User"] = relationship("User", back_populates="family_members")
