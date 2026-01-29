"""Recipe repository for Firestore operations."""

import logging
import random
import uuid
from datetime import datetime, timezone
from typing import Any, Optional

from app.db.firestore import Collections, get_firestore_client, doc_to_dict

logger = logging.getLogger(__name__)


class RecipeRepository:
    """Repository for recipe-related Firestore operations.

    Supports category-based queries for meal pairing.
    """

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

    async def search_by_category(
        self,
        category: str,
        cuisine_type: Optional[str] = None,
        dietary_tags: Optional[list[str]] = None,
        meal_type: Optional[str] = None,
        max_time_minutes: Optional[int] = None,
        exclude_ids: Optional[set[str]] = None,
        limit: int = 50,
    ) -> list[dict[str, Any]]:
        """Search recipes by category (dal, sabzi, rice, etc.).

        Args:
            category: Recipe category to search for
            cuisine_type: Optional cuisine filter
            dietary_tags: Optional dietary restrictions
            meal_type: Optional meal type filter
            max_time_minutes: Optional max cooking time
            exclude_ids: Recipe IDs to exclude
            limit: Maximum results

        Returns:
            List of matching recipes
        """
        query = self.collection.where("is_active", "==", True)
        query = query.where("category", "==", category)

        if cuisine_type:
            query = query.where("cuisine_type", "==", cuisine_type)

        query = query.limit(limit * 2)  # Fetch more for client-side filtering

        recipes = []
        exclude_ids = exclude_ids or set()

        async for doc in query.stream():
            recipe = doc_to_dict(doc)

            # Skip excluded recipes
            if recipe.get("id") in exclude_ids:
                continue

            # Client-side filtering
            if dietary_tags:
                recipe_tags = recipe.get("dietary_tags", [])
                if not any(tag in recipe_tags for tag in dietary_tags):
                    continue

            if meal_type:
                recipe_meals = recipe.get("meal_types", [])
                if meal_type not in recipe_meals:
                    continue

            if max_time_minutes:
                total_time = recipe.get("total_time_minutes") or recipe.get("prep_time_minutes", 0) + recipe.get("cook_time_minutes", 0)
                if total_time and total_time > max_time_minutes:
                    continue

            recipes.append(recipe)
            if len(recipes) >= limit:
                break

        return recipes

    async def search_by_categories(
        self,
        categories: list[str],
        cuisine_type: Optional[str] = None,
        dietary_tags: Optional[list[str]] = None,
        meal_type: Optional[str] = None,
        max_time_minutes: Optional[int] = None,
        exclude_ids: Optional[set[str]] = None,
        limit: int = 50,
    ) -> list[dict[str, Any]]:
        """Search recipes matching any of the given categories.

        Args:
            categories: List of categories to search for
            cuisine_type: Optional cuisine filter
            dietary_tags: Optional dietary restrictions
            meal_type: Optional meal type filter
            max_time_minutes: Optional max cooking time
            exclude_ids: Recipe IDs to exclude
            limit: Maximum results

        Returns:
            List of matching recipes (combined from all categories)
        """
        all_recipes = []
        seen_ids = set()
        exclude_ids = exclude_ids or set()

        for category in categories:
            recipes = await self.search_by_category(
                category=category,
                cuisine_type=cuisine_type,
                dietary_tags=dietary_tags,
                meal_type=meal_type,
                max_time_minutes=max_time_minutes,
                exclude_ids=exclude_ids,
                limit=limit,
            )

            for recipe in recipes:
                recipe_id = recipe.get("id")
                if recipe_id and recipe_id not in seen_ids:
                    seen_ids.add(recipe_id)
                    all_recipes.append(recipe)

        return all_recipes[:limit]

    async def get_pairing_recipe(
        self,
        primary_recipe: dict[str, Any],
        pairing_categories: list[str],
        cuisine_type: Optional[str] = None,
        dietary_tags: Optional[list[str]] = None,
        meal_type: Optional[str] = None,
        max_time_minutes: Optional[int] = None,
        exclude_ids: Optional[set[str]] = None,
    ) -> Optional[dict[str, Any]]:
        """Get a complementary recipe to pair with the primary recipe.

        Args:
            primary_recipe: The main recipe to find a pair for
            pairing_categories: Categories that pair well with the primary
            cuisine_type: Optional cuisine filter
            dietary_tags: Optional dietary restrictions
            meal_type: Optional meal type filter
            max_time_minutes: Optional max cooking time
            exclude_ids: Recipe IDs to exclude

        Returns:
            A complementary recipe or None if not found
        """
        exclude_ids = exclude_ids or set()
        exclude_ids.add(primary_recipe.get("id", ""))

        # Try each pairing category in order
        for category in pairing_categories:
            recipes = await self.search_by_category(
                category=category,
                cuisine_type=cuisine_type,
                dietary_tags=dietary_tags,
                meal_type=meal_type,
                max_time_minutes=max_time_minutes,
                exclude_ids=exclude_ids,
                limit=10,
            )
            if recipes:
                return random.choice(recipes)

        return None

    async def get_recipe_pair(
        self,
        primary_category: str,
        accompaniment_category: str,
        cuisine_type: Optional[str] = None,
        dietary_tags: Optional[list[str]] = None,
        meal_type: Optional[str] = None,
        max_time_minutes: Optional[int] = None,
        exclude_ids: Optional[set[str]] = None,
    ) -> tuple[Optional[dict[str, Any]], Optional[dict[str, Any]]]:
        """Get a pair of complementary recipes.

        Args:
            primary_category: Category for the main item (e.g., 'dal')
            accompaniment_category: Category for the side (e.g., 'rice')
            cuisine_type: Optional cuisine filter
            dietary_tags: Optional dietary restrictions
            meal_type: Optional meal type filter
            max_time_minutes: Optional max cooking time
            exclude_ids: Recipe IDs to exclude

        Returns:
            Tuple of (primary_recipe, accompaniment_recipe)
        """
        exclude_ids = exclude_ids or set()

        # Get primary recipe
        primary_recipes = await self.search_by_category(
            category=primary_category,
            cuisine_type=cuisine_type,
            dietary_tags=dietary_tags,
            meal_type=meal_type,
            max_time_minutes=max_time_minutes,
            exclude_ids=exclude_ids,
            limit=10,
        )

        if not primary_recipes:
            return None, None

        primary = random.choice(primary_recipes)
        exclude_ids.add(primary.get("id", ""))

        # Get accompaniment recipe
        accompaniment_recipes = await self.search_by_category(
            category=accompaniment_category,
            cuisine_type=cuisine_type,
            dietary_tags=dietary_tags,
            meal_type=meal_type,
            max_time_minutes=max_time_minutes,
            exclude_ids=exclude_ids,
            limit=10,
        )

        if not accompaniment_recipes:
            return primary, None

        accompaniment = random.choice(accompaniment_recipes)
        return primary, accompaniment

    async def search_by_ingredient(
        self,
        ingredient: str,
        ingredient_aliases: Optional[list[str]] = None,
        cuisine_type: Optional[str] = None,
        dietary_tags: Optional[list[str]] = None,
        meal_type: Optional[str] = None,
        max_time_minutes: Optional[int] = None,
        exclude_ids: Optional[set[str]] = None,
        limit: int = 20,
    ) -> list[dict[str, Any]]:
        """Search recipes containing a specific ingredient.

        Args:
            ingredient: Ingredient name to search for
            ingredient_aliases: Alternative names for the ingredient
            cuisine_type: Optional cuisine filter
            dietary_tags: Optional dietary restrictions
            meal_type: Optional meal type filter
            max_time_minutes: Optional max cooking time
            exclude_ids: Recipe IDs to exclude
            limit: Maximum results

        Returns:
            List of recipes containing the ingredient
        """
        # Build search terms
        search_terms = [ingredient.lower()]
        if ingredient_aliases:
            search_terms.extend([a.lower() for a in ingredient_aliases])
        search_terms = list(set(search_terms))

        # Build base query
        query = self.collection.where("is_active", "==", True)

        if cuisine_type:
            query = query.where("cuisine_type", "==", cuisine_type)

        query = query.limit(500)  # Need to scan more for ingredient search

        recipes = []
        exclude_ids = exclude_ids or set()

        async for doc in query.stream():
            recipe = doc_to_dict(doc)

            # Skip excluded
            if recipe.get("id") in exclude_ids:
                continue

            # Check if any search term matches recipe name or ingredients
            matched = False

            # Check recipe name
            name = recipe.get("name", "").lower()
            for term in search_terms:
                if term in name:
                    matched = True
                    break

            # Check ingredients
            if not matched:
                for ing in recipe.get("ingredients", []):
                    if isinstance(ing, dict):
                        ing_name = ing.get("name", "").lower()
                    else:
                        ing_name = str(ing).lower()

                    for term in search_terms:
                        if term in ing_name:
                            matched = True
                            break
                    if matched:
                        break

            if not matched:
                continue

            # Additional filters
            if dietary_tags:
                recipe_tags = recipe.get("dietary_tags", [])
                if not any(tag in recipe_tags for tag in dietary_tags):
                    continue

            if meal_type:
                recipe_meals = recipe.get("meal_types", [])
                if meal_type not in recipe_meals:
                    continue

            if max_time_minutes:
                total_time = recipe.get("total_time_minutes") or recipe.get("prep_time_minutes", 0) + recipe.get("cook_time_minutes", 0)
                if total_time and total_time > max_time_minutes:
                    continue

            recipes.append(recipe)
            if len(recipes) >= limit:
                break

        return recipes
