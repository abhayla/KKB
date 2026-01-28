"""Festival repository for Firestore operations."""

import logging
import uuid
from datetime import datetime, timezone, date, timedelta
from typing import Any, Optional

from app.db.firestore import Collections, get_firestore_client, doc_to_dict

logger = logging.getLogger(__name__)


class FestivalRepository:
    """Repository for festival-related Firestore operations."""

    def __init__(self):
        self.db = get_firestore_client()
        self.collection = self.db.collection(Collections.FESTIVALS)

    async def get_by_id(self, festival_id: str) -> Optional[dict[str, Any]]:
        """Get festival by ID."""
        doc = await self.collection.document(festival_id).get()
        if doc.exists:
            return doc_to_dict(doc)
        return None

    async def get_all(self, year: Optional[int] = None) -> list[dict[str, Any]]:
        """Get all festivals, optionally filtered by year."""
        festivals = []
        query = self.collection.order_by("date")

        if year:
            start_date = datetime(year, 1, 1, tzinfo=timezone.utc)
            end_date = datetime(year, 12, 31, 23, 59, 59, tzinfo=timezone.utc)
            query = query.where("date", ">=", start_date).where("date", "<=", end_date)

        async for doc in query.stream():
            festivals.append(doc_to_dict(doc))

        return festivals

    async def get_upcoming(self, days: int = 30) -> list[dict[str, Any]]:
        """Get festivals in the next N days."""
        today = datetime.now(timezone.utc).replace(hour=0, minute=0, second=0, microsecond=0)
        end_date = today + timedelta(days=days)

        festivals = []
        query = (
            self.collection
            .where("date", ">=", today)
            .where("date", "<=", end_date)
            .order_by("date")
        )

        async for doc in query.stream():
            festivals.append(doc_to_dict(doc))

        return festivals

    async def get_by_date(self, target_date: date) -> list[dict[str, Any]]:
        """Get festivals on a specific date."""
        start = datetime.combine(target_date, datetime.min.time()).replace(tzinfo=timezone.utc)
        end = datetime.combine(target_date, datetime.max.time()).replace(tzinfo=timezone.utc)

        festivals = []
        query = self.collection.where("date", ">=", start).where("date", "<=", end)

        async for doc in query.stream():
            festivals.append(doc_to_dict(doc))

        return festivals

    async def get_by_region(self, region: str) -> list[dict[str, Any]]:
        """Get festivals by region."""
        festivals = []
        query = self.collection.where("regions", "array_contains", region)

        async for doc in query.stream():
            festivals.append(doc_to_dict(doc))

        return festivals

    async def create(self, festival_data: dict[str, Any]) -> dict[str, Any]:
        """Create a new festival."""
        festival_id = festival_data.get("id") or str(uuid.uuid4())
        now = datetime.now(timezone.utc)

        festival_data["id"] = festival_id
        festival_data["created_at"] = now
        festival_data["updated_at"] = now

        await self.collection.document(festival_id).set(festival_data)

        logger.info(f"Created festival: {festival_data.get('name')} ({festival_id})")
        return festival_data

    async def update(self, festival_id: str, data: dict[str, Any]) -> Optional[dict[str, Any]]:
        """Update festival data."""
        data["updated_at"] = datetime.now(timezone.utc)
        await self.collection.document(festival_id).update(data)
        return await self.get_by_id(festival_id)

    async def delete(self, festival_id: str) -> bool:
        """Delete a festival."""
        await self.collection.document(festival_id).delete()
        return True

    async def count(self) -> int:
        """Count total festivals."""
        count = 0
        async for _ in self.collection.stream():
            count += 1
        return count
