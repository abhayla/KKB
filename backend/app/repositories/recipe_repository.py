"""Recipe repository for Firestore operations."""

import logging
import uuid
from datetime import datetime, timezone
from typing import Any, Optional

from app.db.firestore import Collections, get_firestore_client, doc_to_dict

logger = logging.getLogger(__name__)


class RecipeRepository:
    """Repository for recipe-related Firestore operations."""

    def __init__(self):
        self.db = get_firestore_client()
        self.collection = self.db.collection(Collections.RECIPES)

    async def get_by_id(self, recipe_id: str) -> Optional[dict[str, Any]]:
        """Get recipe by ID."""
        doc = await self.collection.document(recipe_id).get()
        if doc.exists:
            return doc_to_dict(doc)
        return None

    async def get_all(self, limit: int = 100) -> list[dict[str, Any]]:
        """Get all recipes."""
        recipes = []
        query = self.collection.where("is_active", "==", True).limit(limit)
        async for doc in query.stream():
            recipes.append(doc_to_dict(doc))
        return recipes

    async def search(
        self,
        cuisine_type: Optional[str] = None,
        dietary_tags: Optional[list[str]] = None,
        meal_type: Optional[str] = None,
        max_time_minutes: Optional[int] = None,
        is_vegetarian: Optional[bool] = None,
        is_quick_meal: Optional[bool] = None,
        limit: int = 50,
    ) -> list[dict[str, Any]]:
        """Search recipes with filters."""
        query = self.collection.where("is_active", "==", True)

        if cuisine_type:
            query = query.where("cuisine_type", "==", cuisine_type)

        if is_quick_meal is not None:
            query = query.where("is_quick_meal", "==", is_quick_meal)

        query = query.limit(limit)

        recipes = []
        async for doc in query.stream():
            recipe = doc_to_dict(doc)

            # Client-side filtering for array contains (Firestore limitation)
            if dietary_tags:
                recipe_tags = recipe.get("dietary_tags", [])
                if not any(tag in recipe_tags for tag in dietary_tags):
                    continue

            if meal_type:
                recipe_meals = recipe.get("meal_types", [])
                if meal_type not in recipe_meals:
                    continue

            if max_time_minutes:
                if recipe.get("total_time_minutes", 999) > max_time_minutes:
                    continue

            if is_vegetarian is not None:
                recipe_tags = recipe.get("dietary_tags", [])
                is_veg = "vegetarian" in recipe_tags or "vegan" in recipe_tags
                if is_vegetarian != is_veg:
                    continue

            recipes.append(recipe)

        return recipes

    async def create(self, recipe_data: dict[str, Any]) -> dict[str, Any]:
        """Create a new recipe."""
        recipe_id = recipe_data.get("id") or str(uuid.uuid4())
        now = datetime.now(timezone.utc)

        recipe_data["id"] = recipe_id
        recipe_data["is_active"] = True
        recipe_data["created_at"] = now
        recipe_data["updated_at"] = now

        await self.collection.document(recipe_id).set(recipe_data)

        logger.info(f"Created recipe: {recipe_data.get('name')} ({recipe_id})")
        return recipe_data

    async def update(self, recipe_id: str, data: dict[str, Any]) -> Optional[dict[str, Any]]:
        """Update recipe data."""
        data["updated_at"] = datetime.now(timezone.utc)
        await self.collection.document(recipe_id).update(data)
        return await self.get_by_id(recipe_id)

    async def delete(self, recipe_id: str) -> bool:
        """Soft delete a recipe."""
        await self.collection.document(recipe_id).update({
            "is_active": False,
            "updated_at": datetime.now(timezone.utc),
        })
        return True

    async def get_by_ids(self, recipe_ids: list[str]) -> list[dict[str, Any]]:
        """Get multiple recipes by IDs."""
        recipes = []
        for recipe_id in recipe_ids:
            recipe = await self.get_by_id(recipe_id)
            if recipe:
                recipes.append(recipe)
        return recipes

    async def get_random(
        self,
        count: int = 5,
        cuisine_type: Optional[str] = None,
        dietary_tags: Optional[list[str]] = None,
    ) -> list[dict[str, Any]]:
        """Get random recipes for meal planning."""
        # Firestore doesn't have native random, so we fetch more and slice
        all_recipes = await self.search(
            cuisine_type=cuisine_type,
            dietary_tags=dietary_tags,
            limit=count * 3,
        )

        import random
        random.shuffle(all_recipes)
        return all_recipes[:count]

    async def count(self) -> int:
        """Count total active recipes."""
        count = 0
        async for _ in self.collection.where("is_active", "==", True).stream():
            count += 1
        return count
