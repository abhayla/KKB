"""Recipe repository for PostgreSQL operations."""

import json
import logging
import random
import uuid
from datetime import datetime, timezone
from typing import Any, Optional

from sqlalchemy import select, and_, or_, func
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.cache.recipe_cache import get_recipe_cache
from app.db.postgres import async_session_maker
from app.models.recipe import Recipe, RecipeIngredient, RecipeInstruction, RecipeNutrition

logger = logging.getLogger(__name__)


class RecipeRepository:
    """Repository for recipe-related PostgreSQL operations.

    Supports category-based queries for meal pairing.
    Uses caching to minimize database reads.
    """

    def __init__(self):
        self._cache = get_recipe_cache()

    async def get_by_id(self, recipe_id: str) -> Optional[dict[str, Any]]:
        """Get recipe by ID.

        Checks cache first, fetches from PostgreSQL if not cached.
        """
        # Check cache first
        cached = self._cache.get_recipe(recipe_id)
        if cached is not None:
            return cached

        # Fetch from PostgreSQL
        async with async_session_maker() as session:
            result = await session.execute(
                select(Recipe)
                .options(
                    selectinload(Recipe.ingredients),
                    selectinload(Recipe.instructions),
                    selectinload(Recipe.nutrition),
                )
                .where(Recipe.id == recipe_id)
            )
            recipe = result.scalar_one_or_none()
            if recipe:
                recipe_dict = self._recipe_to_dict(recipe)
                # Cache for future use
                self._cache.set_recipe(recipe_id, recipe_dict)
                return recipe_dict
            return None

    async def get_all(self, limit: int = 100) -> list[dict[str, Any]]:
        """Get all active recipes."""
        async with async_session_maker() as session:
            result = await session.execute(
                select(Recipe)
                .options(
                    selectinload(Recipe.ingredients),
                    selectinload(Recipe.instructions),
                    selectinload(Recipe.nutrition),
                )
                .where(Recipe.is_active == True)
                .limit(limit)
            )
            recipes = result.scalars().all()
            return [self._recipe_to_dict(r) for r in recipes]

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

        Uses caching to reduce database reads for repeated searches.
        """
        # Build cache key params
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

        # Fetch from PostgreSQL
        async with async_session_maker() as session:
            query = select(Recipe).options(
                selectinload(Recipe.ingredients),
                selectinload(Recipe.instructions),
                selectinload(Recipe.nutrition),
            ).where(Recipe.is_active == True)

            if cuisine_type:
                query = query.where(Recipe.cuisine_type == cuisine_type)

            if is_quick_meal is not None:
                query = query.where(Recipe.is_quick_meal == is_quick_meal)

            query = query.limit(limit * 2)  # Fetch more for client-side filtering

            result = await session.execute(query)
            recipes = result.scalars().all()

            # Client-side filtering for JSON fields
            filtered = []
            for recipe in recipes:
                # Parse JSON arrays from database
                recipe_tags = self._parse_json_array(recipe.dietary_tags)
                recipe_meals = self._parse_json_array(recipe.meal_types)

                if dietary_tags:
                    if not any(tag in recipe_tags for tag in dietary_tags):
                        continue

                if meal_type:
                    if meal_type not in recipe_meals:
                        continue

                if max_time_minutes:
                    if recipe.total_time_minutes and recipe.total_time_minutes > max_time_minutes:
                        continue

                if is_vegetarian is not None:
                    is_veg = "vegetarian" in recipe_tags or "vegan" in recipe_tags
                    if is_vegetarian != is_veg:
                        continue

                filtered.append(self._recipe_to_dict(recipe))

                if len(filtered) >= limit:
                    break

            # Cache results
            self._cache.set_search_results(cache_params, filtered)

            return filtered

    async def create(self, recipe_data: dict[str, Any]) -> dict[str, Any]:
        """Create a new recipe."""
        async with async_session_maker() as session:
            recipe = Recipe(
                id=recipe_data.get("id") or str(uuid.uuid4()),
                name=recipe_data.get("name", ""),
                description=recipe_data.get("description"),
                image_url=recipe_data.get("image_url"),
                cuisine_type=recipe_data.get("cuisine_type", "north"),
                meal_types=json.dumps(recipe_data.get("meal_types", ["lunch"])),
                dietary_tags=json.dumps(recipe_data.get("dietary_tags", ["vegetarian"])),
                course_type=recipe_data.get("course_type"),
                category=recipe_data.get("category"),
                prep_time_minutes=recipe_data.get("prep_time_minutes", 15),
                cook_time_minutes=recipe_data.get("cook_time_minutes", 30),
                total_time_minutes=recipe_data.get("total_time_minutes", 45),
                servings=recipe_data.get("servings", 4),
                difficulty_level=recipe_data.get("difficulty_level"),
                is_festive=recipe_data.get("is_festive", False),
                is_fasting_friendly=recipe_data.get("is_fasting_friendly", False),
                is_quick_meal=recipe_data.get("is_quick_meal", False),
                is_kid_friendly=recipe_data.get("is_kid_friendly", False),
                is_active=True,
            )
            session.add(recipe)
            await session.commit()
            await session.refresh(recipe)

            logger.info(f"Created recipe: {recipe.name} ({recipe.id})")
            return self._recipe_to_dict(recipe)

    async def update(self, recipe_id: str, data: dict[str, Any]) -> Optional[dict[str, Any]]:
        """Update recipe data."""
        async with async_session_maker() as session:
            result = await session.execute(
                select(Recipe).where(Recipe.id == recipe_id)
            )
            recipe = result.scalar_one_or_none()
            if not recipe:
                return None

            # Update allowed fields
            for field in ['name', 'description', 'cuisine_type', 'category',
                          'prep_time_minutes', 'cook_time_minutes', 'total_time_minutes',
                          'servings', 'difficulty_level', 'is_festive', 'is_fasting_friendly',
                          'is_quick_meal', 'is_kid_friendly']:
                if field in data:
                    setattr(recipe, field, data[field])

            # Handle JSON fields
            if 'meal_types' in data:
                recipe.meal_types = json.dumps(data['meal_types'])
            if 'dietary_tags' in data:
                recipe.dietary_tags = json.dumps(data['dietary_tags'])

            recipe.updated_at = datetime.now(timezone.utc)
            await session.commit()

            # Invalidate cache
            self._cache.invalidate_recipe(recipe_id)

            return await self.get_by_id(recipe_id)

    async def delete(self, recipe_id: str) -> bool:
        """Soft delete a recipe."""
        async with async_session_maker() as session:
            result = await session.execute(
                select(Recipe).where(Recipe.id == recipe_id)
            )
            recipe = result.scalar_one_or_none()
            if not recipe:
                return False

            recipe.is_active = False
            recipe.updated_at = datetime.now(timezone.utc)
            await session.commit()

            # Invalidate cache
            self._cache.invalidate_recipe(recipe_id)
            return True

    async def get_by_ids(self, recipe_ids: list[str]) -> list[dict[str, Any]]:
        """Get multiple recipes by IDs using batch fetch.

        Optimized to:
        1. Check cache for all IDs first
        2. Batch fetch missing IDs in a single query
        3. Cache fetched recipes for future use
        """
        if not recipe_ids:
            return []

        # Step 1: Check cache for all IDs
        found_recipes, missing_ids = self._cache.get_recipes_batch(recipe_ids)

        # Step 2: Batch fetch missing IDs from PostgreSQL
        if missing_ids:
            async with async_session_maker() as session:
                result = await session.execute(
                    select(Recipe)
                    .options(
                        selectinload(Recipe.ingredients),
                        selectinload(Recipe.instructions),
                        selectinload(Recipe.nutrition),
                    )
                    .where(Recipe.id.in_(missing_ids))
                )
                recipes = result.scalars().all()

                # Process fetched recipes
                fetched_recipes = {}
                for recipe in recipes:
                    recipe_dict = self._recipe_to_dict(recipe)
                    fetched_recipes[recipe.id] = recipe_dict

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
        all_recipes = await self.search(
            cuisine_type=cuisine_type,
            dietary_tags=dietary_tags,
            limit=count * 3,
        )

        random.shuffle(all_recipes)
        return all_recipes[:count]

    async def count(self) -> int:
        """Count total active recipes."""
        async with async_session_maker() as session:
            result = await session.execute(
                select(func.count()).select_from(Recipe).where(Recipe.is_active == True)
            )
            return result.scalar_one()

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
        """Search recipes by category (dal, sabzi, rice, etc.)."""
        # Build cache key params (exclude_ids filtered client-side)
        cache_params = {
            "method": "search_by_category",
            "category": category,
            "cuisine_type": cuisine_type,
            "dietary_tags": sorted(dietary_tags) if dietary_tags else None,
            "meal_type": meal_type,
            "max_time_minutes": max_time_minutes,
            "limit": limit * 2,
        }

        exclude_ids = exclude_ids or set()

        # Check cache first
        cached = self._cache.get_search_results(cache_params)
        if cached is not None:
            filtered = [r for r in cached if r.get("id") not in exclude_ids]
            return filtered[:limit]

        # Fetch from PostgreSQL
        # Since most recipes don't have category set, search by name containing the category term
        async with async_session_maker() as session:
            # Use OR: match category column OR name contains the term
            from sqlalchemy import or_, func
            query = select(Recipe).options(
                selectinload(Recipe.ingredients),
                selectinload(Recipe.instructions),
                selectinload(Recipe.nutrition),
            ).where(
                Recipe.is_active == True,
                or_(
                    Recipe.category == category,
                    func.lower(Recipe.name).contains(category.lower())
                )
            )

            if cuisine_type:
                query = query.where(Recipe.cuisine_type == cuisine_type)

            query = query.limit(limit * 2)

            result = await session.execute(query)
            recipes = result.scalars().all()

            # Client-side filtering
            filtered = []
            for recipe in recipes:
                recipe_tags = self._parse_json_array(recipe.dietary_tags)
                recipe_meals = self._parse_json_array(recipe.meal_types)

                if dietary_tags:
                    if not any(tag in recipe_tags for tag in dietary_tags):
                        continue

                if meal_type:
                    if meal_type not in recipe_meals:
                        continue

                if max_time_minutes:
                    total_time = recipe.total_time_minutes or (recipe.prep_time_minutes or 0) + (recipe.cook_time_minutes or 0)
                    if total_time and total_time > max_time_minutes:
                        continue

                filtered.append(self._recipe_to_dict(recipe))

            # Cache the full results (before excluding IDs)
            self._cache.set_search_results(cache_params, filtered)

            # Now filter out excluded IDs and return
            final = [r for r in filtered if r.get("id") not in exclude_ids]
            return final[:limit]

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
        """Search recipes matching any of the given categories."""
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
        """Get a complementary recipe to pair with the primary recipe."""
        exclude_ids = exclude_ids or set()
        exclude_ids.add(primary_recipe.get("id", ""))

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
        """Get a pair of complementary recipes."""
        exclude_ids = exclude_ids or set()

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
        """Search recipes containing a specific ingredient."""
        search_terms = [ingredient.lower()]
        if ingredient_aliases:
            search_terms.extend([a.lower() for a in ingredient_aliases])
        search_terms = sorted(list(set(search_terms)))

        exclude_ids = exclude_ids or set()

        cache_params = {
            "method": "search_by_ingredient",
            "search_terms": search_terms,
            "cuisine_type": cuisine_type,
            "dietary_tags": sorted(dietary_tags) if dietary_tags else None,
            "meal_type": meal_type,
            "max_time_minutes": max_time_minutes,
        }

        cached = self._cache.get_search_results(cache_params)
        if cached is not None:
            filtered = [r for r in cached if r.get("id") not in exclude_ids]
            return filtered[:limit]

        # Fetch from PostgreSQL
        async with async_session_maker() as session:
            query = select(Recipe).options(
                selectinload(Recipe.ingredients),
                selectinload(Recipe.instructions),
                selectinload(Recipe.nutrition),
            ).where(Recipe.is_active == True)

            if cuisine_type:
                query = query.where(Recipe.cuisine_type == cuisine_type)

            query = query.limit(500)

            result = await session.execute(query)
            recipes = result.scalars().all()

            filtered = []

            for recipe in recipes:
                matched = False

                # Check recipe name
                name = recipe.name.lower()
                for term in search_terms:
                    if term in name:
                        matched = True
                        break

                # Check ingredients
                if not matched:
                    for ing in recipe.ingredients:
                        ing_name = ing.name.lower()
                        for term in search_terms:
                            if term in ing_name:
                                matched = True
                                break
                        if matched:
                            break

                if not matched:
                    continue

                recipe_tags = self._parse_json_array(recipe.dietary_tags)
                recipe_meals = self._parse_json_array(recipe.meal_types)

                if dietary_tags:
                    if not any(tag in recipe_tags for tag in dietary_tags):
                        continue

                if meal_type:
                    if meal_type not in recipe_meals:
                        continue

                if max_time_minutes:
                    total_time = recipe.total_time_minutes or (recipe.prep_time_minutes or 0) + (recipe.cook_time_minutes or 0)
                    if total_time and total_time > max_time_minutes:
                        continue

                filtered.append(self._recipe_to_dict(recipe))

            # Cache the full results
            self._cache.set_search_results(cache_params, filtered)

            final = [r for r in filtered if r.get("id") not in exclude_ids]
            return final[:limit]

    # Helper methods
    def _parse_json_array(self, value: Any) -> list[str]:
        """Parse JSON array from database value."""
        if isinstance(value, list):
            return value
        if isinstance(value, str):
            try:
                return json.loads(value)
            except json.JSONDecodeError:
                return []
        return []

    def _recipe_to_dict(self, recipe: Recipe) -> dict[str, Any]:
        """Convert Recipe model to dictionary."""
        result = {
            "id": recipe.id,
            "name": recipe.name,
            "description": recipe.description,
            "image_url": recipe.image_url,
            "cuisine_type": recipe.cuisine_type,
            "meal_types": self._parse_json_array(recipe.meal_types),
            "dietary_tags": self._parse_json_array(recipe.dietary_tags),
            "course_type": recipe.course_type,
            "category": recipe.category,
            "prep_time_minutes": recipe.prep_time_minutes,
            "cook_time_minutes": recipe.cook_time_minutes,
            "total_time_minutes": recipe.total_time_minutes,
            "servings": recipe.servings,
            "difficulty_level": recipe.difficulty_level,
            "is_festive": recipe.is_festive,
            "is_fasting_friendly": recipe.is_fasting_friendly,
            "is_quick_meal": recipe.is_quick_meal,
            "is_kid_friendly": recipe.is_kid_friendly,
            "is_active": recipe.is_active,
            "created_at": recipe.created_at,
            "updated_at": recipe.updated_at,
        }

        # Include related data if loaded
        if recipe.ingredients:
            result["ingredients"] = [
                {
                    "id": ing.id,
                    "name": ing.name,
                    "quantity": ing.quantity,
                    "unit": ing.unit,
                    "category": ing.category,
                    "notes": ing.notes,
                    "is_optional": ing.is_optional,
                    "order": ing.order,
                }
                for ing in recipe.ingredients
            ]

        if recipe.instructions:
            result["instructions"] = [
                {
                    "id": inst.id,
                    "step_number": inst.step_number,
                    "instruction": inst.instruction,
                    "duration_minutes": inst.duration_minutes,
                    "timer_required": inst.timer_required,
                    "tips": inst.tips,
                }
                for inst in recipe.instructions
            ]

        if recipe.nutrition:
            result["nutrition"] = {
                "calories": recipe.nutrition.calories,
                "protein_grams": recipe.nutrition.protein_grams,
                "carbohydrates_grams": recipe.nutrition.carbohydrates_grams,
                "fat_grams": recipe.nutrition.fat_grams,
                "fiber_grams": recipe.nutrition.fiber_grams,
                "sugar_grams": recipe.nutrition.sugar_grams,
                "sodium_mg": recipe.nutrition.sodium_mg,
            }

        return result
