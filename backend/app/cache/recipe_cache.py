"""Recipe caching for Firestore read optimization.

This module implements a TTL-based cache for recipes and search results
to reduce Firestore reads during meal plan generation.

Cache design:
- Recipe cache: 2000 items, 1-hour TTL (individual recipes by ID)
- Search cache: 500 items, 30-minute TTL (search results by query hash)
- Thread-safe singleton pattern
- Cache stats tracking for monitoring

Expected impact:
- 70-90% reduction in Firestore reads for warm cache scenarios
- 95-99% reduction after cache warmup with popular categories
"""

import hashlib
import json
import logging
import threading
from dataclasses import dataclass, field
from typing import Any, Optional

from cachetools import TTLCache

logger = logging.getLogger(__name__)

# Cache configuration
RECIPE_CACHE_SIZE = 2000  # Max individual recipes cached
RECIPE_CACHE_TTL = 3600   # 1 hour in seconds

SEARCH_CACHE_SIZE = 500   # Max search results cached
SEARCH_CACHE_TTL = 1800   # 30 minutes in seconds


@dataclass
class CacheStats:
    """Statistics for cache performance monitoring."""
    recipe_hits: int = 0
    recipe_misses: int = 0
    search_hits: int = 0
    search_misses: int = 0
    batch_hits: int = 0
    batch_misses: int = 0

    @property
    def recipe_hit_rate(self) -> float:
        """Calculate recipe cache hit rate."""
        total = self.recipe_hits + self.recipe_misses
        return (self.recipe_hits / total * 100) if total > 0 else 0.0

    @property
    def search_hit_rate(self) -> float:
        """Calculate search cache hit rate."""
        total = self.search_hits + self.search_misses
        return (self.search_hits / total * 100) if total > 0 else 0.0

    @property
    def overall_hit_rate(self) -> float:
        """Calculate overall cache hit rate."""
        total_hits = self.recipe_hits + self.search_hits + self.batch_hits
        total_misses = self.recipe_misses + self.search_misses + self.batch_misses
        total = total_hits + total_misses
        return (total_hits / total * 100) if total > 0 else 0.0


class RecipeCache:
    """Thread-safe singleton cache for recipes and search results.

    Usage:
        cache = RecipeCache()  # Always returns same instance

        # Individual recipe caching
        cache.set_recipe("recipe-123", recipe_dict)
        recipe = cache.get_recipe("recipe-123")

        # Batch operations
        found, missing = cache.get_recipes_batch(["id1", "id2", "id3"])
        cache.set_recipes_batch({"id1": recipe1, "id2": recipe2})

        # Search result caching
        cache.set_search_results(params_dict, results_list)
        results = cache.get_search_results(params_dict)

        # Statistics
        stats = cache.get_stats()
        print(f"Hit rate: {stats.overall_hit_rate:.1f}%")
    """

    _instance: Optional["RecipeCache"] = None
    _lock = threading.Lock()

    def __new__(cls) -> "RecipeCache":
        """Singleton pattern - return existing instance or create new one."""
        if cls._instance is None:
            with cls._lock:
                # Double-check locking
                if cls._instance is None:
                    instance = super().__new__(cls)
                    instance._initialized = False
                    cls._instance = instance
        return cls._instance

    def __init__(self):
        """Initialize cache stores (only runs once due to singleton)."""
        if getattr(self, "_initialized", False):
            return

        # Recipe cache: recipe_id -> recipe dict
        self._recipe_cache: TTLCache = TTLCache(
            maxsize=RECIPE_CACHE_SIZE,
            ttl=RECIPE_CACHE_TTL
        )

        # Search cache: cache_key -> list of recipe dicts
        self._search_cache: TTLCache = TTLCache(
            maxsize=SEARCH_CACHE_SIZE,
            ttl=SEARCH_CACHE_TTL
        )

        # Statistics
        self._stats = CacheStats()

        # Thread safety for stats
        self._stats_lock = threading.Lock()

        self._initialized = True
        logger.info(
            f"RecipeCache initialized: recipe_cache={RECIPE_CACHE_SIZE} items/{RECIPE_CACHE_TTL}s TTL, "
            f"search_cache={SEARCH_CACHE_SIZE} items/{SEARCH_CACHE_TTL}s TTL"
        )

    # ===== Individual Recipe Operations =====

    def get_recipe(self, recipe_id: str) -> Optional[dict[str, Any]]:
        """Get a recipe by ID from cache.

        Args:
            recipe_id: The recipe ID to look up

        Returns:
            Recipe dict if found in cache, None otherwise
        """
        recipe = self._recipe_cache.get(recipe_id)

        with self._stats_lock:
            if recipe is not None:
                self._stats.recipe_hits += 1
            else:
                self._stats.recipe_misses += 1

        return recipe

    def set_recipe(self, recipe_id: str, recipe: dict[str, Any]) -> None:
        """Cache a recipe by ID.

        Args:
            recipe_id: The recipe ID
            recipe: The recipe dictionary to cache
        """
        if recipe_id and recipe:
            self._recipe_cache[recipe_id] = recipe

    def has_recipe(self, recipe_id: str) -> bool:
        """Check if a recipe is in cache without updating stats.

        Args:
            recipe_id: The recipe ID to check

        Returns:
            True if recipe is cached, False otherwise
        """
        return recipe_id in self._recipe_cache

    # ===== Batch Recipe Operations =====

    def get_recipes_batch(
        self,
        recipe_ids: list[str]
    ) -> tuple[dict[str, dict[str, Any]], list[str]]:
        """Get multiple recipes from cache, returning found and missing IDs.

        This enables efficient batch fetching - get what's cached,
        then fetch only the missing ones from Firestore.

        Args:
            recipe_ids: List of recipe IDs to look up

        Returns:
            Tuple of (found_recipes dict, missing_ids list)
            - found_recipes: {recipe_id: recipe_dict} for cached recipes
            - missing_ids: List of recipe_ids not in cache
        """
        found: dict[str, dict[str, Any]] = {}
        missing: list[str] = []

        for recipe_id in recipe_ids:
            recipe = self._recipe_cache.get(recipe_id)
            if recipe is not None:
                found[recipe_id] = recipe
            else:
                missing.append(recipe_id)

        # Update stats
        with self._stats_lock:
            self._stats.batch_hits += len(found)
            self._stats.batch_misses += len(missing)

        return found, missing

    def set_recipes_batch(self, recipes: dict[str, dict[str, Any]]) -> None:
        """Cache multiple recipes at once.

        Args:
            recipes: Dictionary of {recipe_id: recipe_dict}
        """
        for recipe_id, recipe in recipes.items():
            if recipe_id and recipe:
                self._recipe_cache[recipe_id] = recipe

    # ===== Search Result Caching =====

    def _make_search_key(self, params: dict[str, Any]) -> str:
        """Generate a cache key from search parameters.

        Excludes 'exclude_ids' since those are filtered client-side.

        Args:
            params: Search parameters dictionary

        Returns:
            Hash string for cache lookup
        """
        # Remove exclude_ids from key - we filter those client-side
        key_params = {k: v for k, v in sorted(params.items()) if k != "exclude_ids"}

        # Convert to stable JSON string
        param_str = json.dumps(key_params, sort_keys=True, default=str)

        # Hash for compact key
        return hashlib.md5(param_str.encode()).hexdigest()

    def get_search_results(
        self,
        params: dict[str, Any]
    ) -> Optional[list[dict[str, Any]]]:
        """Get cached search results.

        Args:
            params: Search parameters (cuisine_type, dietary_tags, etc.)

        Returns:
            List of recipe dicts if cached, None otherwise
        """
        cache_key = self._make_search_key(params)
        results = self._search_cache.get(cache_key)

        with self._stats_lock:
            if results is not None:
                self._stats.search_hits += 1
            else:
                self._stats.search_misses += 1

        return results

    def set_search_results(
        self,
        params: dict[str, Any],
        results: list[dict[str, Any]]
    ) -> None:
        """Cache search results and opportunistically cache individual recipes.

        Args:
            params: Search parameters used for the query
            results: List of recipe dicts returned by the search
        """
        cache_key = self._make_search_key(params)
        self._search_cache[cache_key] = results

        # Opportunistically cache individual recipes from search results
        for recipe in results:
            recipe_id = recipe.get("id")
            if recipe_id and not self.has_recipe(recipe_id):
                self.set_recipe(recipe_id, recipe)

    def has_search_results(self, params: dict[str, Any]) -> bool:
        """Check if search results are cached without updating stats.

        Args:
            params: Search parameters

        Returns:
            True if results are cached, False otherwise
        """
        cache_key = self._make_search_key(params)
        return cache_key in self._search_cache

    # ===== Cache Management =====

    def clear(self) -> None:
        """Clear all caches. Useful for testing."""
        self._recipe_cache.clear()
        self._search_cache.clear()
        with self._stats_lock:
            self._stats = CacheStats()
        logger.info("RecipeCache cleared")

    def get_stats(self) -> CacheStats:
        """Get current cache statistics.

        Returns:
            CacheStats object with hit/miss counts and rates
        """
        with self._stats_lock:
            # Return a copy to prevent external modification
            return CacheStats(
                recipe_hits=self._stats.recipe_hits,
                recipe_misses=self._stats.recipe_misses,
                search_hits=self._stats.search_hits,
                search_misses=self._stats.search_misses,
                batch_hits=self._stats.batch_hits,
                batch_misses=self._stats.batch_misses,
            )

    def get_size(self) -> dict[str, int]:
        """Get current cache sizes.

        Returns:
            Dict with recipe_count and search_count
        """
        return {
            "recipe_count": len(self._recipe_cache),
            "search_count": len(self._search_cache),
            "recipe_max": RECIPE_CACHE_SIZE,
            "search_max": SEARCH_CACHE_SIZE,
        }

    def log_stats(self) -> None:
        """Log current cache statistics."""
        stats = self.get_stats()
        size = self.get_size()
        logger.info(
            f"RecipeCache stats: "
            f"recipes={size['recipe_count']}/{size['recipe_max']}, "
            f"searches={size['search_count']}/{size['search_max']}, "
            f"recipe_hit_rate={stats.recipe_hit_rate:.1f}%, "
            f"search_hit_rate={stats.search_hit_rate:.1f}%, "
            f"overall_hit_rate={stats.overall_hit_rate:.1f}%"
        )


# ===== Cache Warming =====

# Popular categories to pre-load on startup
WARM_CATEGORIES = ["dal", "rice", "roti", "sabzi", "curry", "chai", "paratha", "dosa", "idli", "sambar"]

# Common cuisines to warm cache for
WARM_CUISINES = ["north", "south"]


async def warm_recipe_cache() -> None:
    """Warm the recipe cache with popular categories on startup.

    Pre-loads common recipe categories for popular cuisines to ensure
    the first meal plan generation has high cache hit rates.

    Expected to cache ~300-500 recipes, reducing cold-start reads by ~30%.
    """
    from app.repositories.recipe_repository import RecipeRepository

    cache = get_recipe_cache()
    repo = RecipeRepository()

    logger.info("Starting recipe cache warm-up...")
    total_cached = 0

    for cuisine in WARM_CUISINES:
        for category in WARM_CATEGORIES:
            try:
                # Search by category - this will cache the results and individual recipes
                recipes = await repo.search_by_category(
                    category=category,
                    cuisine_type=cuisine,
                    dietary_tags=["vegetarian"],  # Most common dietary preference
                    limit=30,
                )
                total_cached += len(recipes)
                logger.debug(f"Warmed cache: {category}/{cuisine} = {len(recipes)} recipes")
            except Exception as e:
                logger.warning(f"Failed to warm cache for {category}/{cuisine}: {e}")

    # Also warm cache for ingredient searches (common INCLUDE rule targets)
    common_ingredients = ["chai", "coffee", "poha", "upma", "paratha", "dal", "paneer"]
    for ingredient in common_ingredients:
        try:
            recipes = await repo.search_by_ingredient(
                ingredient=ingredient,
                cuisine_type=None,  # All cuisines
                dietary_tags=["vegetarian"],
                limit=20,
            )
            total_cached += len(recipes)
            logger.debug(f"Warmed cache: ingredient/{ingredient} = {len(recipes)} recipes")
        except Exception as e:
            logger.warning(f"Failed to warm cache for ingredient {ingredient}: {e}")

    cache.log_stats()
    logger.info(f"Recipe cache warm-up complete: {total_cached} recipes cached")


# Convenience function for getting the singleton
def get_recipe_cache() -> RecipeCache:
    """Get the RecipeCache singleton instance.

    Returns:
        The global RecipeCache instance
    """
    return RecipeCache()
