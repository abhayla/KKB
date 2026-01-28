"""Meal plan repository for Firestore operations."""

import logging
import uuid
from datetime import datetime, timezone, date
from typing import Any, Optional

from app.db.firestore import Collections, get_firestore_client, doc_to_dict

logger = logging.getLogger(__name__)


class MealPlanRepository:
    """Repository for meal plan-related Firestore operations."""

    def __init__(self):
        self.db = get_firestore_client()
        self.collection = self.db.collection(Collections.MEAL_PLANS)

    async def get_by_id(self, plan_id: str) -> Optional[dict[str, Any]]:
        """Get meal plan by ID."""
        doc = await self.collection.document(plan_id).get()
        if doc.exists:
            return doc_to_dict(doc)
        return None

    async def get_current_for_user(self, user_id: str) -> Optional[dict[str, Any]]:
        """Get current week's meal plan for user."""
        today = date.today()
        query = (
            self.collection
            .where("user_id", "==", user_id)
            .where("is_active", "==", True)
            .order_by("week_start_date", direction="DESCENDING")
            .limit(1)
        )

        async for doc in query.stream():
            plan = doc_to_dict(doc)
            # Check if plan is current (within this week)
            week_end = plan.get("week_end_date")
            if week_end and isinstance(week_end, datetime):
                if week_end.date() >= today:
                    return plan
            return plan  # Return latest even if expired

        return None

    async def get_history_for_user(
        self, user_id: str, limit: int = 10
    ) -> list[dict[str, Any]]:
        """Get meal plan history for user."""
        plans = []
        query = (
            self.collection
            .where("user_id", "==", user_id)
            .order_by("week_start_date", direction="DESCENDING")
            .limit(limit)
        )

        async for doc in query.stream():
            plans.append(doc_to_dict(doc))

        return plans

    async def create(self, plan_data: dict[str, Any]) -> dict[str, Any]:
        """Create a new meal plan."""
        plan_id = plan_data.get("id") or str(uuid.uuid4())
        now = datetime.now(timezone.utc)

        plan_data["id"] = plan_id
        plan_data["is_active"] = True
        plan_data["created_at"] = now
        plan_data["updated_at"] = now

        await self.collection.document(plan_id).set(plan_data)

        logger.info(f"Created meal plan: {plan_id} for user {plan_data.get('user_id')}")
        return plan_data

    async def update(self, plan_id: str, data: dict[str, Any]) -> Optional[dict[str, Any]]:
        """Update meal plan data."""
        data["updated_at"] = datetime.now(timezone.utc)
        await self.collection.document(plan_id).update(data)
        return await self.get_by_id(plan_id)

    async def swap_meal(
        self,
        plan_id: str,
        day_index: int,
        meal_type: str,
        new_recipe_id: str,
        new_recipe_name: str,
    ) -> Optional[dict[str, Any]]:
        """Swap a meal in the plan."""
        plan = await self.get_by_id(plan_id)
        if not plan:
            return None

        days = plan.get("days", [])
        if day_index >= len(days):
            return None

        # Update the specific meal
        day = days[day_index]
        if meal_type in day:
            day[meal_type]["recipe_id"] = new_recipe_id
            day[meal_type]["recipe_name"] = new_recipe_name
            day[meal_type]["is_swapped"] = True

        return await self.update(plan_id, {"days": days})

    async def lock_meal(
        self, plan_id: str, day_index: int, meal_type: str, locked: bool = True
    ) -> Optional[dict[str, Any]]:
        """Lock or unlock a meal."""
        plan = await self.get_by_id(plan_id)
        if not plan:
            return None

        days = plan.get("days", [])
        if day_index >= len(days):
            return None

        day = days[day_index]
        if meal_type in day:
            day[meal_type]["is_locked"] = locked

        return await self.update(plan_id, {"days": days})

    async def delete(self, plan_id: str) -> bool:
        """Soft delete a meal plan."""
        await self.collection.document(plan_id).update({
            "is_active": False,
            "updated_at": datetime.now(timezone.utc),
        })
        return True

    async def deactivate_old_plans(self, user_id: str, except_plan_id: str) -> int:
        """Deactivate all old plans for user except the specified one."""
        count = 0
        query = (
            self.collection
            .where("user_id", "==", user_id)
            .where("is_active", "==", True)
        )

        async for doc in query.stream():
            if doc.id != except_plan_id:
                await doc.reference.update({
                    "is_active": False,
                    "updated_at": datetime.now(timezone.utc),
                })
                count += 1

        return count
