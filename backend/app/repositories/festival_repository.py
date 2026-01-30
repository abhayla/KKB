"""Festival repository for PostgreSQL operations."""

import logging
import uuid
from datetime import datetime, timezone, date, timedelta
from typing import Any, Optional

from sqlalchemy import select, func

from app.db.postgres import async_session_maker
from app.models.festival import Festival

logger = logging.getLogger(__name__)


class FestivalRepository:
    """Repository for festival-related PostgreSQL operations."""

    async def get_by_id(self, festival_id: str) -> Optional[dict[str, Any]]:
        """Get festival by ID."""
        async with async_session_maker() as session:
            result = await session.execute(
                select(Festival).where(Festival.id == festival_id)
            )
            festival = result.scalar_one_or_none()
            if festival:
                return self._festival_to_dict(festival)
            return None

    async def get_all(self, year: Optional[int] = None) -> list[dict[str, Any]]:
        """Get all festivals, optionally filtered by year."""
        async with async_session_maker() as session:
            query = select(Festival).where(Festival.is_active == True)

            if year:
                query = query.where(Festival.year == year)

            query = query.order_by(Festival.date)

            result = await session.execute(query)
            festivals = result.scalars().all()

            return [self._festival_to_dict(f) for f in festivals]

    async def get_upcoming(self, days: int = 30) -> list[dict[str, Any]]:
        """Get festivals in the next N days."""
        today = date.today()
        end_date = today + timedelta(days=days)

        async with async_session_maker() as session:
            result = await session.execute(
                select(Festival)
                .where(
                    Festival.is_active == True,
                    Festival.date >= today,
                    Festival.date <= end_date,
                )
                .order_by(Festival.date)
            )
            festivals = result.scalars().all()

            return [self._festival_to_dict(f) for f in festivals]

    async def get_by_date(self, target_date: date) -> list[dict[str, Any]]:
        """Get festivals on a specific date."""
        async with async_session_maker() as session:
            result = await session.execute(
                select(Festival)
                .where(
                    Festival.is_active == True,
                    Festival.date == target_date,
                )
            )
            festivals = result.scalars().all()

            return [self._festival_to_dict(f) for f in festivals]

    async def get_by_region(self, region: str) -> list[dict[str, Any]]:
        """Get festivals by region.

        Note: This searches for region in the JSON array stored in regions column.
        """
        async with async_session_maker() as session:
            # For JSON arrays stored as text, we need to use a LIKE search
            # or cast and use PostgreSQL array operators
            result = await session.execute(
                select(Festival)
                .where(
                    Festival.is_active == True,
                    Festival.regions.cast(str).ilike(f'%"{region}"%'),
                )
                .order_by(Festival.date)
            )
            festivals = result.scalars().all()

            return [self._festival_to_dict(f) for f in festivals]

    async def get_by_date_range(
        self, start_date: date, end_date: date
    ) -> list[dict[str, Any]]:
        """Get festivals within a date range."""
        async with async_session_maker() as session:
            result = await session.execute(
                select(Festival)
                .where(
                    Festival.is_active == True,
                    Festival.date >= start_date,
                    Festival.date <= end_date,
                )
                .order_by(Festival.date)
            )
            festivals = result.scalars().all()

            return [self._festival_to_dict(f) for f in festivals]

    async def create(self, festival_data: dict[str, Any]) -> dict[str, Any]:
        """Create a new festival."""
        async with async_session_maker() as session:
            festival_id = festival_data.get("id") or str(uuid.uuid4())

            festival = Festival(
                id=festival_id,
                name=festival_data.get("name"),
                name_hindi=festival_data.get("name_hindi"),
                description=festival_data.get("description"),
                date=festival_data.get("date"),
                year=festival_data.get("year", date.today().year),
                regions=festival_data.get("regions", ["all"]),
                is_fasting_day=festival_data.get("is_fasting_day", False),
                fasting_type=festival_data.get("fasting_type"),
                special_foods=festival_data.get("special_foods"),
                avoided_foods=festival_data.get("avoided_foods"),
                is_active=True,
            )
            session.add(festival)
            await session.commit()
            await session.refresh(festival)

            logger.info(f"Created festival: {festival.name} ({festival_id})")
            return self._festival_to_dict(festival)

    async def update(
        self, festival_id: str, data: dict[str, Any]
    ) -> Optional[dict[str, Any]]:
        """Update festival data."""
        async with async_session_maker() as session:
            result = await session.execute(
                select(Festival).where(Festival.id == festival_id)
            )
            festival = result.scalar_one_or_none()
            if not festival:
                return None

            # Update allowed fields
            allowed_fields = [
                "name",
                "name_hindi",
                "description",
                "date",
                "year",
                "regions",
                "is_fasting_day",
                "fasting_type",
                "special_foods",
                "avoided_foods",
                "is_active",
            ]
            for field in allowed_fields:
                if field in data:
                    setattr(festival, field, data[field])

            await session.commit()
            await session.refresh(festival)

            return self._festival_to_dict(festival)

    async def delete(self, festival_id: str) -> bool:
        """Soft delete a festival (set is_active to False)."""
        async with async_session_maker() as session:
            result = await session.execute(
                select(Festival).where(Festival.id == festival_id)
            )
            festival = result.scalar_one_or_none()
            if not festival:
                return False

            festival.is_active = False
            await session.commit()
            return True

    async def count(self) -> int:
        """Count total active festivals."""
        async with async_session_maker() as session:
            result = await session.execute(
                select(func.count(Festival.id)).where(Festival.is_active == True)
            )
            return result.scalar() or 0

    def _festival_to_dict(self, festival: Festival) -> dict[str, Any]:
        """Convert Festival model to dictionary."""
        return {
            "id": festival.id,
            "name": festival.name,
            "name_hindi": festival.name_hindi,
            "description": festival.description,
            "date": festival.date,
            "year": festival.year,
            "regions": festival.regions or [],
            "is_fasting_day": festival.is_fasting_day,
            "fasting_type": festival.fasting_type,
            "special_foods": festival.special_foods or [],
            "avoided_foods": festival.avoided_foods or [],
            "is_active": festival.is_active,
        }
