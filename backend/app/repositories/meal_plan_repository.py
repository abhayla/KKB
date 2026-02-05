"""Meal plan repository for PostgreSQL operations."""

import logging
import uuid
from datetime import datetime, timezone, date
from typing import Any, Optional

from sqlalchemy import select, and_
from sqlalchemy.orm import selectinload

from app.db.postgres import async_session_maker
from app.models.meal_plan import MealPlan, MealPlanItem

logger = logging.getLogger(__name__)


class MealPlanRepository:
    """Repository for meal plan-related PostgreSQL operations."""

    async def get_by_id(self, plan_id: str) -> Optional[dict[str, Any]]:
        """Get meal plan by ID."""
        async with async_session_maker() as session:
            result = await session.execute(
                select(MealPlan)
                .options(selectinload(MealPlan.items))
                .where(MealPlan.id == plan_id)
            )
            plan = result.scalar_one_or_none()
            if plan:
                return self._plan_to_dict(plan)
            return None

    async def get_current_for_user(self, user_id: str) -> Optional[dict[str, Any]]:
        """Get current week's meal plan for user."""
        today = date.today()
        async with async_session_maker() as session:
            result = await session.execute(
                select(MealPlan)
                .options(selectinload(MealPlan.items))
                .where(
                    MealPlan.user_id == user_id,
                    MealPlan.is_active == True,
                )
                .order_by(MealPlan.week_start_date.desc())
                .limit(1)
            )
            plan = result.scalar_one_or_none()
            if plan:
                # Check if plan is current (within this week)
                if plan.week_end_date and plan.week_end_date >= today:
                    return self._plan_to_dict(plan)
                # Return latest even if expired
                return self._plan_to_dict(plan)
            return None

    async def get_history_for_user(
        self, user_id: str, limit: int = 10
    ) -> list[dict[str, Any]]:
        """Get meal plan history for user."""
        async with async_session_maker() as session:
            result = await session.execute(
                select(MealPlan)
                .options(selectinload(MealPlan.items))
                .where(MealPlan.user_id == user_id)
                .order_by(MealPlan.week_start_date.desc())
                .limit(limit)
            )
            plans = result.scalars().all()
            return [self._plan_to_dict(p) for p in plans]

    async def create(self, plan_data: dict[str, Any]) -> dict[str, Any]:
        """Create a new meal plan."""
        async with async_session_maker() as session:
            plan_id = plan_data.get("id") or str(uuid.uuid4())
            now = datetime.now(timezone.utc)

            plan = MealPlan(
                id=plan_id,
                user_id=plan_data.get("user_id"),
                week_start_date=plan_data.get("week_start_date"),
                week_end_date=plan_data.get("week_end_date"),
                is_active=True,
            )
            session.add(plan)

            # Create meal plan items from days array
            days = plan_data.get("days", [])
            for day_index, day in enumerate(days):
                day_date_raw = day.get("date")
                # Convert string date to date object if needed
                if isinstance(day_date_raw, str):
                    day_date = date.fromisoformat(day_date_raw)
                else:
                    day_date = day_date_raw

                # Create items for each meal type (handles both single dict and list of dicts)
                for meal_type in ["breakfast", "lunch", "dinner", "snacks"]:
                    meal_data = day.get(meal_type)
                    if meal_data:
                        # Handle list of items or single item
                        items_list = meal_data if isinstance(meal_data, list) else [meal_data]
                        for meal_item in items_list:
                            # Convert placeholder recipe_ids to None for AI-generated meals
                            recipe_id = meal_item.get("recipe_id")
                            if recipe_id in ("GENERIC", "AI_GENERATED"):
                                recipe_id = None

                            item = MealPlanItem(
                                id=str(uuid.uuid4()),
                                meal_plan_id=plan_id,
                                recipe_id=recipe_id,
                                date=day_date,
                                meal_type=meal_type,
                                servings=meal_item.get("servings", 2),
                                is_locked=meal_item.get("is_locked", False),
                                is_swapped=meal_item.get("is_swapped", False),
                                recipe_name=meal_item.get("recipe_name"),
                                festival_name=day.get("festival"),
                            )
                            session.add(item)

            await session.commit()
            await session.refresh(plan)

            logger.info(f"Created meal plan: {plan_id} for user {plan.user_id}")
            return await self.get_by_id(plan_id)

    async def update(self, plan_id: str, data: dict[str, Any]) -> Optional[dict[str, Any]]:
        """Update meal plan data."""
        async with async_session_maker() as session:
            result = await session.execute(
                select(MealPlan)
                .options(selectinload(MealPlan.items))
                .where(MealPlan.id == plan_id)
            )
            plan = result.scalar_one_or_none()
            if not plan:
                return None

            # Update basic fields
            if "is_active" in data:
                plan.is_active = data["is_active"]

            # Update days if provided
            if "days" in data:
                # This is a full replacement of meal items
                # First delete existing items
                for item in plan.items:
                    await session.delete(item)

                # Then create new items
                days = data["days"]
                for day_index, day in enumerate(days):
                    day_date_raw = day.get("date")
                    # Convert string date to date object if needed
                    if isinstance(day_date_raw, str):
                        day_date = date.fromisoformat(day_date_raw)
                    else:
                        day_date = day_date_raw

                    for meal_type in ["breakfast", "lunch", "dinner", "snacks"]:
                        meal_data = day.get(meal_type)
                        if meal_data:
                            # Convert placeholder recipe_ids to None for AI-generated meals
                            recipe_id = meal_data.get("recipe_id")
                            if recipe_id in ("GENERIC", "AI_GENERATED"):
                                recipe_id = None

                            item = MealPlanItem(
                                id=str(uuid.uuid4()),
                                meal_plan_id=plan_id,
                                recipe_id=recipe_id,
                                date=day_date,
                                meal_type=meal_type,
                                servings=meal_data.get("servings", 2),
                                is_locked=meal_data.get("is_locked", False),
                                is_swapped=meal_data.get("is_swapped", False),
                                recipe_name=meal_data.get("recipe_name"),
                                festival_name=day.get("festival"),
                            )
                            session.add(item)

            plan.updated_at = datetime.now(timezone.utc)
            await session.commit()

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
        async with async_session_maker() as session:
            result = await session.execute(
                select(MealPlan).where(MealPlan.id == plan_id)
            )
            plan = result.scalar_one_or_none()
            if not plan:
                return False

            plan.is_active = False
            plan.updated_at = datetime.now(timezone.utc)
            await session.commit()
            return True

    async def deactivate_old_plans(self, user_id: str, except_plan_id: str) -> int:
        """Deactivate all old plans for user except the specified one."""
        async with async_session_maker() as session:
            result = await session.execute(
                select(MealPlan).where(
                    MealPlan.user_id == user_id,
                    MealPlan.is_active == True,
                    MealPlan.id != except_plan_id,
                )
            )
            plans = result.scalars().all()

            count = 0
            for plan in plans:
                plan.is_active = False
                plan.updated_at = datetime.now(timezone.utc)
                count += 1

            await session.commit()
            return count

    # Helper methods
    def _plan_to_dict(self, plan: MealPlan) -> dict[str, Any]:
        """Convert MealPlan model to dictionary with days structure."""
        # Group items by date to create days array
        days_map = {}
        for item in plan.items:
            day_key = item.date.isoformat() if item.date else "unknown"
            if day_key not in days_map:
                days_map[day_key] = {
                    "date": item.date.isoformat() if item.date else "",
                    "day_name": item.date.strftime("%A") if item.date else "",
                    "festival": item.festival_name,
                    "meals": {
                        "breakfast": [],
                        "lunch": [],
                        "dinner": [],
                        "snacks": [],
                    },
                }

            # Append to list for this meal type
            days_map[day_key]["meals"][item.meal_type].append({
                "id": item.id,
                "recipe_id": item.recipe_id or "",
                "recipe_name": item.recipe_name,
                "servings": item.servings,
                "is_locked": item.is_locked,
                "is_swapped": item.is_swapped,
                "prep_time_minutes": 30,  # Default value
                "calories": 0,  # Default value
                "dietary_tags": [],
            })

        # Sort days by date
        days = sorted(days_map.values(), key=lambda d: d.get("date") or date.min)

        return {
            "id": plan.id,
            "user_id": plan.user_id,
            "week_start_date": plan.week_start_date,
            "week_end_date": plan.week_end_date,
            "is_active": plan.is_active,
            "days": days,
            "created_at": plan.created_at,
            "updated_at": plan.updated_at,
        }
