"""Recipe repository for Firestore operations."""

import logging
import random
import uuid
from datetime import datetime, timezone
from typing import Any, Optional

from app.cache.recipe_cache import RecipeCache, get_recipe_cache
from app.db.firestore import Collections, get_firestore_client, doc_to_dict

logger = logging.getLogger(__name__)


class RecipeRepository:
    """Repository for recipe-related Firestore operations.

    Supports category-based queries for meal pairing.
    Uses caching to minimize Firestore reads.
    """

    def __init__(self):
        self.db = get_firestore_client()
        self.collection = self.db.collection(Collections.RECIPES)
        self._cache = get_recipe_cache()

    async def get_by_id(self, recipe_id: str) -> Optional[dict[str, Any]]:
        """Get recipe by ID.

        Checks cache first, fetches from Firestore if not cached.
        """
        # Check cache first
        cached = self._cache.get_recipe(recipe_id)
        if cached is not None:
            return cached

        # Fetch from Firestore
        doc = await self.collection.document(recipe_id).get()
        if doc.exists:
            recipe = doc_to_dict(doc)
            # Cache for future use
            self._cache.set_recipe(recipe_id, recipe)
            return recipe
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
        """Search recipes with filters.

        Uses caching to reduce Firestore reads for repeated searches.
        """
        # Build cache key params (exclude_ids filtered client-side)
        cache_params = {
            "method": "search",
            "cuisine_type": cuisine_type,
            "dietary_tags": sorted(dietary_tags) if dietary_tags else None,
            "meal_type": meal_type,
            "max_time_minutes": max_time_minutes,
            "is_vegetarian": is_vegetarian,
            "is_quick_meal": is_quick_meal,
            "limit": limit,
        }

        # Check cache first
        cached = self._cache.get_search_results(cache_params)
        if cached is not None:
            return cached

        # Fetch from Firestore
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

        # Cache results (also caches individual recipes)
        self._cache.set_search_results(cache_params, recipes)

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
        """Get multiple recipes by IDs using batch fetch.

        Optimized to:
        1. Check cache for all IDs first
        2. Batch fetch missing IDs in a single Firestore read
        3. Cache fetched recipes for future use

        This fixes the N+1 query problem - previously made N individual reads,
        now makes at most 1 batch read for uncached recipes.
        """
        if not recipe_ids:
            return []

        # Step 1: Check cache for all IDs
        found_recipes, missing_ids = self._cache.get_recipes_batch(recipe_ids)

        # Step 2: Batch fetch missing IDs from Firestore
        if missing_ids:
            # Create document references for batch fetch
            doc_refs = [self.collection.document(rid) for rid in missing_ids]

            # Batch fetch - single Firestore read for all documents
            docs = await self.db.get_all(doc_refs)

            # Process fetched documents
            fetched_recipes = {}
            for doc in docs:
                if doc.exists:
                    recipe = doc_to_dict(doc)
                    recipe_id = recipe.get("id")
                    if recipe_id:
                        fetched_recipes[recipe_id] = recipe

            # Step 3: Cache fetched recipes
            if fetched_recipes:
                self._cache.set_recipes_batch(fetched_recipes)
                found_recipes.update(fetched_recipes)

            logger.debug(
                f"get_by_ids: {len(recipe_ids)} requested, "
                f"{len(recipe_ids) - len(missing_ids)} cached, "
                f"{len(fetched_recipes)} fetched"
            )

        # Return in original order, filtering out not found
        return [found_recipes[rid] for rid in recipe_ids if rid in found_recipes]

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

        Uses caching to reduce Firestore reads. Cache key excludes exclude_ids
        since those are filtered client-side from cached results.

        Args:
            category: Recipe category to search for
            cuisine_type: Optional cuisine filter
            dietary_tags: Optional dietary restrictions
            meal_type: Optional meal type filter
            max_time_minutes: Optional max cooking time
            exclude_ids: Recipe IDs to exclude (filtered client-side)
            limit: Maximum results

        Returns:
            List of matching recipes
        """
        # Build cache key params (exclude_ids filtered client-side)
        cache_params = {
            "method": "search_by_category",
            "category": category,
            "cuisine_type": cuisine_type,
            "dietary_tags": sorted(dietary_tags) if dietary_tags else None,
            "meal_type": meal_type,
            "max_time_minutes": max_time_minutes,
            "limit": limit * 2,  # Cache the full result set
        }

        exclude_ids = exclude_ids or set()

        # Check cache first
        cached = self._cache.get_search_results(cache_params)
        if cached is not None:
            # Filter out excluded IDs from cached results
            filtered = [r for r in cached if r.get("id") not in exclude_ids]
            return filtered[:limit]

        # Fetch from Firestore
        query = self.collection.where("is_active", "==", True)
        query = query.where("category", "==", category)

        if cuisine_type:
            query = query.where("cuisine_type", "==", cuisine_type)

        query = query.limit(limit * 2)  # Fetch more for client-side filtering

        recipes = []

        async for doc in query.stream():
            recipe = doc_to_dict(doc)

            # Client-side filtering (but NOT exclude_ids - we cache the full result)
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

        # Cache the full results (before excluding IDs)
        self._cache.set_search_results(cache_params, recipes)

        # Now filter out excluded IDs and return
        filtered = [r for r in recipes if r.get("id") not in exclude_ids]
        return filtered[:limit]

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

        Uses caching to reduce Firestore reads. Cache key excludes exclude_ids
        since those are filtered client-side from cached results.

        Args:
            ingredient: Ingredient name to search for
            ingredient_aliases: Alternative names for the ingredient
            cuisine_type: Optional cuisine filter
            dietary_tags: Optional dietary restrictions
            meal_type: Optional meal type filter
            max_time_minutes: Optional max cooking time
            exclude_ids: Recipe IDs to exclude (filtered client-side)
            limit: Maximum results

        Returns:
            List of recipes containing the ingredient
        """
        # Build search terms
        search_terms = [ingredient.lower()]
        if ingredient_aliases:
            search_terms.extend([a.lower() for a in ingredient_aliases])
        search_terms = sorted(list(set(search_terms)))

        exclude_ids = exclude_ids or set()

        # Build cache key params (exclude_ids filtered client-side)
        cache_params = {
            "method": "search_by_ingredient",
            "search_terms": search_terms,
            "cuisine_type": cuisine_type,
            "dietary_tags": sorted(dietary_tags) if dietary_tags else None,
            "meal_type": meal_type,
            "max_time_minutes": max_time_minutes,
        }

        # Check cache first
        cached = self._cache.get_search_results(cache_params)
        if cached is not None:
            # Filter out excluded IDs from cached results
            filtered = [r for r in cached if r.get("id") not in exclude_ids]
            return filtered[:limit]

        # Build base query
        query = self.collection.where("is_active", "==", True)

        if cuisine_type:
            query = query.where("cuisine_type", "==", cuisine_type)

        query = query.limit(500)  # Need to scan more for ingredient search

        recipes = []

        async for doc in query.stream():
            recipe = doc_to_dict(doc)

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

            # Additional filters (but NOT exclude_ids - we cache the full result)
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

        # Cache the full results (before excluding IDs)
        self._cache.set_search_results(cache_params, recipes)

        # Now filter out excluded IDs and return
        filtered = [r for r in recipes if r.get("id") not in exclude_ids]
        return filtered[:limit]
