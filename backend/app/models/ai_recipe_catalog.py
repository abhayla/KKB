"""AI Recipe Catalog model — shared catalog of AI-generated recipe names."""

import uuid
from typing import Optional

from sqlalchemy import DateTime, ForeignKey, Index, Integer, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base, TimestampMixin


class AiRecipeCatalog(Base, TimestampMixin):
    """Shared catalog of AI-generated recipe names across all users.

    Each unique recipe name (normalized) gets one row. When the same name
    is generated again, usage_count is incremented. Dietary tags, cuisine,
    ingredients, and nutrition are stored for filtering and future features.
    """

    __tablename__ = "ai_recipe_catalog"

    id: Mapped[str] = mapped_column(
        String(36),
        primary_key=True,
        default=lambda: str(uuid.uuid4()),
    )
    display_name: Mapped[str] = mapped_column(
        String(255), nullable=False
    )
    normalized_name: Mapped[str] = mapped_column(
        String(255), nullable=False, unique=True
    )

    # Recipe metadata
    dietary_tags: Mapped[Optional[str]] = mapped_column(
        Text, nullable=True
    )  # JSON array: ["vegetarian", "vegan"]
    cuisine_type: Mapped[Optional[str]] = mapped_column(
        String(20), nullable=True
    )  # north, south, east, west
    meal_types: Mapped[Optional[str]] = mapped_column(
        Text, nullable=True
    )  # JSON array: ["lunch", "dinner"]
    category: Mapped[Optional[str]] = mapped_column(
        String(50), nullable=True
    )  # dal, sabzi, rice, roti, snack, etc.
    prep_time_minutes: Mapped[Optional[int]] = mapped_column(
        Integer, nullable=True
    )
    calories: Mapped[Optional[int]] = mapped_column(
        Integer, nullable=True
    )

    # Rich data for future features (nullable for backfilled entries)
    ingredients: Mapped[Optional[str]] = mapped_column(
        Text, nullable=True
    )  # JSON: [{"name": "Toor Dal", "quantity": 1, "unit": "cup", "category": "pulses"}]
    nutrition: Mapped[Optional[str]] = mapped_column(
        Text, nullable=True
    )  # JSON: {"protein_g": 12, "carbs_g": 35, "fat_g": 8, "fiber_g": 6}

    # Popularity tracking
    usage_count: Mapped[int] = mapped_column(
        Integer, default=1, nullable=False
    )

    # Provenance
    first_generated_by: Mapped[Optional[str]] = mapped_column(
        String(36),
        ForeignKey("users.id", ondelete="SET NULL"),
        nullable=True,
    )

    __table_args__ = (
        Index("ix_ai_recipe_catalog_normalized_name", "normalized_name", unique=True),
        Index("ix_ai_recipe_catalog_usage_count", "usage_count"),
        Index("ix_ai_recipe_catalog_cuisine_type", "cuisine_type"),
    )
