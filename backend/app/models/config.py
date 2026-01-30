"""System configuration and reference data models."""

import uuid
from typing import Any, Optional

from sqlalchemy import String, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base, TimestampMixin
from app.models.user import JSONList


class SystemConfig(Base, TimestampMixin):
    """System configuration storage (e.g., meal_generation config).

    Stores configuration as JSON in the config_data column.
    Key is a unique identifier like 'meal_generation'.
    """

    __tablename__ = "system_config"

    id: Mapped[str] = mapped_column(
        String(36),
        primary_key=True,
        default=lambda: str(uuid.uuid4()),
    )
    key: Mapped[str] = mapped_column(String(100), unique=True, nullable=False, index=True)
    description: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    config_data: Mapped[dict[str, Any]] = mapped_column(JSONList, nullable=False)


class ReferenceData(Base, TimestampMixin):
    """Reference data storage (ingredients, dishes, cuisines).

    Stores reference data as JSON in the data column.
    Category is a unique identifier like 'ingredients', 'dishes', 'cuisines'.
    """

    __tablename__ = "reference_data"

    id: Mapped[str] = mapped_column(
        String(36),
        primary_key=True,
        default=lambda: str(uuid.uuid4()),
    )
    category: Mapped[str] = mapped_column(String(100), unique=True, nullable=False, index=True)
    description: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    data: Mapped[dict[str, Any]] = mapped_column(JSONList, nullable=False)
