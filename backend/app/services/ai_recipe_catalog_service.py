"""AI Recipe Catalog service — catalog and search AI-generated recipe names.

Provides two main functions:
1. catalog_recipes() — called after meal plan generation to upsert recipes
2. search_catalog() — search with dietary filtering and favorites-first sorting
"""

import json
import logging
import uuid
from datetime import datetime, timezone

from sqlalchemy import cast, func, or_, select, String, update
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.ai_recipe_catalog import AiRecipeCatalog

logger = logging.getLogger(__name__)


def normalize_recipe_name(name: str) -> str:
    """Normalize a recipe name for deduplication.

    Strips whitespace and lowercases for consistent matching.
    """
    return name.strip().lower()


async def catalog_recipes(
    db: AsyncSession,
    user_id: str,
    generated_plan: dict,
    cuisine_type: str = "north",
) -> int:
    """Catalog AI-generated recipes from a meal plan into the shared catalog.

    For each recipe in the plan:
    - If the normalized name already exists, increment usage_count
    - If new, insert with all available metadata

    Args:
        db: Database session
        user_id: ID of the user who generated the plan
        generated_plan: The generated meal plan (dict with "days" key)
        cuisine_type: User's primary cuisine preference

    Returns:
        Number of recipes cataloged (new + updated)
    """
    count = 0
    days = generated_plan.get("days", [])

    for day_data in days:
        for slot in ["breakfast", "lunch", "dinner", "snacks"]:
            items = day_data.get(slot, [])
            if isinstance(day_data.get("meals"), dict):
                items = day_data["meals"].get(slot, [])

            for item in items:
                recipe_name = item.get("recipe_name", "")
                if not recipe_name:
                    continue

                normalized = normalize_recipe_name(recipe_name)

                # Check if already exists
                result = await db.execute(
                    select(AiRecipeCatalog).where(
                        AiRecipeCatalog.normalized_name == normalized
                    )
                )
                existing = result.scalar_one_or_none()

                if existing:
                    # Increment usage_count
                    existing.usage_count += 1
                    existing.updated_at = datetime.now(timezone.utc)
                else:
                    # Build metadata
                    dietary_tags = item.get("dietary_tags", [])
                    ingredients_data = item.get("ingredients")
                    nutrition_data = item.get("nutrition")

                    new_entry = AiRecipeCatalog(
                        id=str(uuid.uuid4()),
                        display_name=recipe_name.strip(),
                        normalized_name=normalized,
                        dietary_tags=json.dumps(dietary_tags) if dietary_tags else None,
                        cuisine_type=cuisine_type,
                        meal_types=json.dumps([slot]),
                        category=item.get("category"),
                        prep_time_minutes=item.get("prep_time_minutes"),
                        calories=item.get("calories"),
                        ingredients=json.dumps(ingredients_data) if ingredients_data else None,
                        nutrition=json.dumps(nutrition_data) if nutrition_data else None,
                        usage_count=1,
                        first_generated_by=user_id,
                        created_at=datetime.now(timezone.utc),
                        updated_at=datetime.now(timezone.utc),
                    )
                    db.add(new_entry)

                count += 1

    await db.commit()
    logger.info(f"Cataloged {count} recipe items for user {user_id}")
    return count


# Dietary filtering: which tags conflict with each diet type
DIETARY_EXCLUSIONS = {
    "vegetarian": {"non_vegetarian"},
    "vegan": {"non_vegetarian"},  # Also require "vegan" tag, handled in filter
    "jain": {"non_vegetarian"},   # Also require "jain" tag, handled in filter
    "sattvic": {"non_vegetarian"},  # Also require "sattvic" tag, handled in filter
    "eggetarian": {"non_vegetarian"},  # Unless also has "eggetarian"
    "non_vegetarian": set(),  # No exclusions
    "halal": {"non_vegetarian"},  # Unless also has "halal"
}

# Diets that require their own tag to be present
REQUIRE_OWN_TAG = {"vegan", "jain", "sattvic"}


def _passes_dietary_filter(
    recipe_dietary_tags: list[str],
    user_dietary_tags: list[str],
) -> bool:
    """Check if a recipe passes dietary filtering for a user.

    Args:
        recipe_dietary_tags: Tags on the recipe (e.g., ["vegetarian", "vegan"])
        user_dietary_tags: User's dietary preferences (e.g., ["vegetarian"])

    Returns:
        True if the recipe is compatible with the user's diet
    """
    if not user_dietary_tags:
        return True

    for user_diet in user_dietary_tags:
        user_diet_lower = user_diet.lower()
        excluded_tags = DIETARY_EXCLUSIONS.get(user_diet_lower, set())

        # Check for excluded tags
        for tag in recipe_dietary_tags:
            if tag.lower() in excluded_tags:
                # Special case: eggetarian can eat eggetarian-tagged non_veg
                if user_diet_lower == "eggetarian" and "eggetarian" in [t.lower() for t in recipe_dietary_tags]:
                    continue
                return False

        # For strict diets, require the recipe to have the matching tag
        if user_diet_lower in REQUIRE_OWN_TAG:
            if user_diet_lower not in [t.lower() for t in recipe_dietary_tags]:
                return False

    return True


async def search_catalog(
    db: AsyncSession,
    query: str = "",
    user_dietary_tags: list[str] | None = None,
    favorite_names: list[str] | None = None,
    limit: int = 10,
) -> list[dict]:
    """Search the AI recipe catalog with dietary filtering and favorites sorting.

    Args:
        db: Database session
        query: Search text (matched against normalized_name via LIKE)
        user_dietary_tags: User's dietary preferences for filtering
        favorite_names: List of favorite recipe names to sort first
        limit: Maximum results to return

    Returns:
        List of recipe catalog entries as dicts
    """
    # Build base query
    stmt = select(AiRecipeCatalog)

    if query.strip():
        normalized_query = normalize_recipe_name(query)
        stmt = stmt.where(
            or_(
                AiRecipeCatalog.normalized_name.contains(normalized_query),
                AiRecipeCatalog.ingredients.cast(String).ilike(f"%{normalized_query}%"),
            )
        )

    # Order by usage_count descending, fetch more than limit for post-filtering
    stmt = stmt.order_by(AiRecipeCatalog.usage_count.desc()).limit(limit * 3)

    result = await db.execute(stmt)
    entries = result.scalars().all()

    # Post-filter by dietary compatibility
    filtered = []
    for entry in entries:
        recipe_tags = json.loads(entry.dietary_tags) if entry.dietary_tags else []
        if _passes_dietary_filter(recipe_tags, user_dietary_tags or []):
            filtered.append(entry)

    # Sort: favorites first, then by usage_count
    normalized_favorites = set()
    if favorite_names:
        normalized_favorites = {normalize_recipe_name(n) for n in favorite_names}

    def sort_key(entry: AiRecipeCatalog):
        is_favorite = 0 if entry.normalized_name in normalized_favorites else 1
        return (is_favorite, -entry.usage_count)

    filtered.sort(key=sort_key)

    # Convert to dicts and limit
    results = []
    for entry in filtered[:limit]:
        results.append({
            "id": entry.id,
            "display_name": entry.display_name,
            "normalized_name": entry.normalized_name,
            "dietary_tags": json.loads(entry.dietary_tags) if entry.dietary_tags else [],
            "cuisine_type": entry.cuisine_type,
            "meal_types": json.loads(entry.meal_types) if entry.meal_types else [],
            "category": entry.category,
            "prep_time_minutes": entry.prep_time_minutes,
            "calories": entry.calories,
            "ingredients": json.loads(entry.ingredients) if entry.ingredients else None,
            "nutrition": json.loads(entry.nutrition) if entry.nutrition else None,
            "usage_count": entry.usage_count,
        })

    return results
