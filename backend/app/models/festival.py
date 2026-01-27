"""Festival database model."""

import uuid
from datetime import date
from typing import Optional

from sqlalchemy import Boolean, Date, Integer, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base
from app.models.user import JSONList


class Festival(Base):
    """Indian festival reference data."""

    __tablename__ = "festivals"

    id: Mapped[str] = mapped_column(
        String(36),
        primary_key=True,
        default=lambda: str(uuid.uuid4()),
    )
    name: Mapped[str] = mapped_column(String(100), nullable=False)
    name_hindi: Mapped[Optional[str]] = mapped_column(String(100), nullable=True)
    description: Mapped[Optional[str]] = mapped_column(Text, nullable=True)

    # Date info (for recurring calculation or fixed dates)
    date: Mapped[Optional[date]] = mapped_column(
        Date, nullable=True
    )  # Fixed date for current year
    year: Mapped[int] = mapped_column(Integer, nullable=False)  # Year this date applies to

    # Regional relevance
    regions: Mapped[list[str]] = mapped_column(
        JSONList, nullable=False
    )  # north, south, east, west, all

    # Fasting info
    is_fasting_day: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    fasting_type: Mapped[Optional[str]] = mapped_column(
        String(50), nullable=True
    )  # complete, partial, specific

    # Food recommendations
    special_foods: Mapped[Optional[list[str]]] = mapped_column(
        JSONList, nullable=True
    )  # Traditional foods for this festival
    avoided_foods: Mapped[Optional[list[str]]] = mapped_column(
        JSONList, nullable=True
    )  # Foods to avoid

    is_active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
