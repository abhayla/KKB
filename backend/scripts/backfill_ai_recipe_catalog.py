"""Backfill AI recipe catalog from existing meal plan items.

This script populates the ai_recipe_catalog table from historical
meal plan data. Recipes that were AI-generated (recipe_id IS NULL or 'AI_GENERATED')
are grouped by normalized name, with usage_count reflecting how many times
each recipe was generated.

Usage:
    cd backend
    PYTHONPATH=. python scripts/backfill_ai_recipe_catalog.py
"""

import asyncio
import json
import logging
import uuid
from collections import defaultdict
from datetime import datetime, timezone

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.postgres import async_session_maker
from app.models.ai_recipe_catalog import AiRecipeCatalog
from app.models.meal_plan import MealPlan, MealPlanItem
from app.models.user import UserPreferences
from app.services.ai_recipe_catalog_service import normalize_recipe_name

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


async def backfill():
    """Backfill AI recipe catalog from existing meal plan items."""
    async with async_session_maker() as db:
        # Get all meal plan items that are AI-generated
        result = await db.execute(
            select(MealPlanItem, MealPlan.user_id)
            .join(MealPlan, MealPlanItem.meal_plan_id == MealPlan.id)
            .where(
                (MealPlanItem.recipe_id == None) |  # noqa: E711
                (MealPlanItem.recipe_id == "AI_GENERATED")
            )
        )
        items = result.all()

        if not items:
            logger.info("No AI-generated meal plan items found to backfill")
            return

        logger.info(f"Found {len(items)} AI-generated meal plan items")

        # Group by normalized recipe name
        recipe_groups: dict[str, dict] = defaultdict(lambda: {
            "display_name": "",
            "count": 0,
            "first_user_id": None,
            "meal_type": None,
            "dietary_tags": [],
        })

        for item, user_id in items:
            name = item.recipe_name
            if not name:
                continue

            normalized = normalize_recipe_name(name)
            group = recipe_groups[normalized]
            group["count"] += 1
            if not group["display_name"]:
                group["display_name"] = name.strip()
            if not group["first_user_id"]:
                group["first_user_id"] = user_id
            if item.meal_type:
                group["meal_type"] = item.meal_type

        logger.info(f"Found {len(recipe_groups)} unique recipe names")

        # Get user preferences for cuisine_type context
        user_prefs_cache: dict[str, str] = {}

        # Upsert into catalog
        created = 0
        updated = 0

        for normalized, group in recipe_groups.items():
            # Check if already exists
            existing_result = await db.execute(
                select(AiRecipeCatalog).where(
                    AiRecipeCatalog.normalized_name == normalized
                )
            )
            existing = existing_result.scalar_one_or_none()

            if existing:
                existing.usage_count += group["count"]
                existing.updated_at = datetime.now(timezone.utc)
                updated += 1
            else:
                # Get user's cuisine preference
                user_id = group["first_user_id"]
                cuisine_type = "north"
                if user_id and user_id not in user_prefs_cache:
                    prefs_result = await db.execute(
                        select(UserPreferences).where(
                            UserPreferences.user_id == user_id
                        )
                    )
                    prefs = prefs_result.scalar_one_or_none()
                    if prefs and prefs.cuisine_preferences:
                        cp = prefs.cuisine_preferences
                        if isinstance(cp, str):
                            cp = json.loads(cp)
                        if cp:
                            user_prefs_cache[user_id] = cp[0]
                    else:
                        user_prefs_cache[user_id] = "north"

                if user_id:
                    cuisine_type = user_prefs_cache.get(user_id, "north")

                meal_types = [group["meal_type"]] if group["meal_type"] else None

                new_entry = AiRecipeCatalog(
                    id=str(uuid.uuid4()),
                    display_name=group["display_name"],
                    normalized_name=normalized,
                    dietary_tags=None,  # Not available for backfilled entries
                    cuisine_type=cuisine_type,
                    meal_types=json.dumps(meal_types) if meal_types else None,
                    category=None,
                    prep_time_minutes=None,
                    calories=None,
                    ingredients=None,  # Not available for backfilled entries
                    nutrition=None,    # Not available for backfilled entries
                    usage_count=group["count"],
                    first_generated_by=user_id,
                    created_at=datetime.now(timezone.utc),
                    updated_at=datetime.now(timezone.utc),
                )
                db.add(new_entry)
                created += 1

        await db.commit()
        logger.info(
            f"Backfill complete: {created} new entries, {updated} updated entries"
        )


if __name__ == "__main__":
    asyncio.run(backfill())
