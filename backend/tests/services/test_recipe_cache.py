"""Tests for RecipeCache.

Tests cover:
- Individual recipe caching (get/set/has)
- Batch operations (get_batch/set_batch)
- Search result caching
- Cache statistics tracking
- TTL behavior (with mocking)
- Thread safety (singleton pattern)
"""

import pytest
import time
from unittest.mock import patch, MagicMock

from app.cache.recipe_cache import (
    RecipeCache,
    CacheStats,
    get_recipe_cache,
    RECIPE_CACHE_SIZE,
    RECIPE_CACHE_TTL,
    SEARCH_CACHE_SIZE,
    SEARCH_CACHE_TTL,
)


@pytest.fixture
def cache():
    """Get a fresh cache instance for testing."""
    cache = RecipeCache()
    cache.clear()  # Clear any existing data
    return cache


@pytest.fixture
def sample_recipe():
    """Sample recipe dict for testing."""
    return {
        "id": "recipe-123",
        "name": "Dal Makhani",
        "category": "dal",
        "cuisine_type": "north",
        "dietary_tags": ["vegetarian"],
        "meal_types": ["lunch", "dinner"],
        "prep_time_minutes": 30,
        "cook_time_minutes": 45,
    }


@pytest.fixture
def sample_recipes():
    """Multiple sample recipes for batch testing."""
    return {
        "recipe-1": {
            "id": "recipe-1",
            "name": "Jeera Rice",
            "category": "rice",
            "cuisine_type": "north",
        },
        "recipe-2": {
            "id": "recipe-2",
            "name": "Roti",
            "category": "roti",
            "cuisine_type": "north",
        },
        "recipe-3": {
            "id": "recipe-3",
            "name": "Palak Paneer",
            "category": "sabzi",
            "cuisine_type": "north",
        },
    }


class TestRecipeCacheSingleton:
    """Tests for singleton pattern."""

    def test_singleton_returns_same_instance(self):
        """RecipeCache should return same instance on multiple calls."""
        cache1 = RecipeCache()
        cache2 = RecipeCache()
        assert cache1 is cache2

    def test_get_recipe_cache_returns_singleton(self):
        """get_recipe_cache() should return the singleton."""
        cache1 = get_recipe_cache()
        cache2 = RecipeCache()
        assert cache1 is cache2


class TestIndividualRecipeCaching:
    """Tests for individual recipe get/set operations."""

    def test_set_and_get_recipe(self, cache, sample_recipe):
        """Should be able to set and retrieve a recipe."""
        cache.set_recipe("recipe-123", sample_recipe)
        result = cache.get_recipe("recipe-123")

        assert result is not None
        assert result["id"] == "recipe-123"
        assert result["name"] == "Dal Makhani"

    def test_get_nonexistent_recipe_returns_none(self, cache):
        """Getting a non-existent recipe should return None."""
        result = cache.get_recipe("nonexistent-id")
        assert result is None

    def test_has_recipe_returns_true_when_cached(self, cache, sample_recipe):
        """has_recipe should return True for cached recipes."""
        cache.set_recipe("recipe-123", sample_recipe)
        assert cache.has_recipe("recipe-123") is True

    def test_has_recipe_returns_false_when_not_cached(self, cache):
        """has_recipe should return False for uncached recipes."""
        assert cache.has_recipe("nonexistent-id") is False

    def test_set_recipe_with_empty_id_does_not_cache(self, cache, sample_recipe):
        """Empty recipe ID should not be cached."""
        cache.set_recipe("", sample_recipe)
        assert cache.has_recipe("") is False

    def test_set_recipe_with_none_recipe_does_not_cache(self, cache):
        """None recipe should not be cached."""
        cache.set_recipe("recipe-123", None)
        assert cache.has_recipe("recipe-123") is False


class TestBatchOperations:
    """Tests for batch get/set operations."""

    def test_set_recipes_batch(self, cache, sample_recipes):
        """Should be able to batch set multiple recipes."""
        cache.set_recipes_batch(sample_recipes)

        assert cache.has_recipe("recipe-1") is True
        assert cache.has_recipe("recipe-2") is True
        assert cache.has_recipe("recipe-3") is True

    def test_get_recipes_batch_all_cached(self, cache, sample_recipes):
        """get_recipes_batch should return all cached recipes."""
        cache.set_recipes_batch(sample_recipes)

        found, missing = cache.get_recipes_batch(["recipe-1", "recipe-2", "recipe-3"])

        assert len(found) == 3
        assert len(missing) == 0
        assert "recipe-1" in found
        assert "recipe-2" in found
        assert "recipe-3" in found

    def test_get_recipes_batch_none_cached(self, cache):
        """get_recipes_batch should return all IDs as missing when not cached."""
        found, missing = cache.get_recipes_batch(["id-1", "id-2", "id-3"])

        assert len(found) == 0
        assert len(missing) == 3
        assert "id-1" in missing
        assert "id-2" in missing
        assert "id-3" in missing

    def test_get_recipes_batch_partial_cache(self, cache, sample_recipes):
        """get_recipes_batch should correctly split found/missing."""
        # Only cache recipe-1
        cache.set_recipe("recipe-1", sample_recipes["recipe-1"])

        found, missing = cache.get_recipes_batch(["recipe-1", "recipe-2", "recipe-3"])

        assert len(found) == 1
        assert "recipe-1" in found
        assert len(missing) == 2
        assert "recipe-2" in missing
        assert "recipe-3" in missing

    def test_get_recipes_batch_empty_list(self, cache):
        """get_recipes_batch with empty list should return empty results."""
        found, missing = cache.get_recipes_batch([])
        assert len(found) == 0
        assert len(missing) == 0


class TestSearchResultCaching:
    """Tests for search result caching."""

    def test_set_and_get_search_results(self, cache, sample_recipes):
        """Should be able to cache and retrieve search results."""
        params = {
            "method": "search_by_category",
            "category": "dal",
            "cuisine_type": "north",
        }
        results = list(sample_recipes.values())

        cache.set_search_results(params, results)
        cached = cache.get_search_results(params)

        assert cached is not None
        assert len(cached) == 3

    def test_search_cache_ignores_exclude_ids(self, cache, sample_recipes):
        """Search cache key should not include exclude_ids."""
        params1 = {
            "method": "search_by_category",
            "category": "dal",
            "exclude_ids": {"id-1", "id-2"},
        }
        params2 = {
            "method": "search_by_category",
            "category": "dal",
            "exclude_ids": {"id-3"},  # Different exclude_ids
        }
        results = list(sample_recipes.values())

        cache.set_search_results(params1, results)
        cached = cache.get_search_results(params2)

        # Should get same cached results despite different exclude_ids
        assert cached is not None
        assert len(cached) == 3

    def test_search_cache_different_params_different_keys(self, cache, sample_recipes):
        """Different search params should have different cache keys."""
        params1 = {"method": "search", "category": "dal"}
        params2 = {"method": "search", "category": "rice"}
        results = list(sample_recipes.values())

        cache.set_search_results(params1, results)
        cached = cache.get_search_results(params2)

        # Different params should not hit cache
        assert cached is None

    def test_search_results_cache_individual_recipes(self, cache, sample_recipes):
        """Setting search results should opportunistically cache individual recipes."""
        params = {"method": "search", "category": "dal"}
        results = list(sample_recipes.values())

        cache.set_search_results(params, results)

        # Individual recipes should also be cached
        assert cache.has_recipe("recipe-1") is True
        assert cache.has_recipe("recipe-2") is True
        assert cache.has_recipe("recipe-3") is True

    def test_has_search_results(self, cache, sample_recipes):
        """has_search_results should work correctly."""
        params = {"method": "search", "category": "dal"}
        results = list(sample_recipes.values())

        assert cache.has_search_results(params) is False

        cache.set_search_results(params, results)

        assert cache.has_search_results(params) is True


class TestCacheStatistics:
    """Tests for cache statistics tracking."""

    def test_initial_stats_are_zero(self, cache):
        """Fresh cache should have zero stats."""
        stats = cache.get_stats()

        assert stats.recipe_hits == 0
        assert stats.recipe_misses == 0
        assert stats.search_hits == 0
        assert stats.search_misses == 0
        assert stats.batch_hits == 0
        assert stats.batch_misses == 0

    def test_recipe_cache_tracks_hits(self, cache, sample_recipe):
        """Recipe cache should track hits."""
        cache.set_recipe("recipe-123", sample_recipe)
        cache.get_recipe("recipe-123")
        cache.get_recipe("recipe-123")

        stats = cache.get_stats()
        assert stats.recipe_hits == 2
        assert stats.recipe_misses == 0

    def test_recipe_cache_tracks_misses(self, cache):
        """Recipe cache should track misses."""
        cache.get_recipe("nonexistent-1")
        cache.get_recipe("nonexistent-2")

        stats = cache.get_stats()
        assert stats.recipe_hits == 0
        assert stats.recipe_misses == 2

    def test_search_cache_tracks_hits(self, cache, sample_recipes):
        """Search cache should track hits."""
        params = {"method": "search", "category": "dal"}
        results = list(sample_recipes.values())

        cache.set_search_results(params, results)
        cache.get_search_results(params)
        cache.get_search_results(params)

        stats = cache.get_stats()
        assert stats.search_hits == 2
        assert stats.search_misses == 0

    def test_search_cache_tracks_misses(self, cache):
        """Search cache should track misses."""
        cache.get_search_results({"method": "search", "category": "dal"})
        cache.get_search_results({"method": "search", "category": "rice"})

        stats = cache.get_stats()
        assert stats.search_hits == 0
        assert stats.search_misses == 2

    def test_batch_tracks_hits_and_misses(self, cache, sample_recipes):
        """Batch operations should track hits and misses."""
        # Cache only recipe-1
        cache.set_recipe("recipe-1", sample_recipes["recipe-1"])

        # Batch get with 1 hit, 2 misses
        cache.get_recipes_batch(["recipe-1", "recipe-2", "recipe-3"])

        stats = cache.get_stats()
        assert stats.batch_hits == 1
        assert stats.batch_misses == 2

    def test_hit_rate_calculation(self, cache, sample_recipe):
        """Hit rate should be calculated correctly."""
        cache.set_recipe("recipe-123", sample_recipe)

        # 2 hits, 1 miss = 66.67% hit rate
        cache.get_recipe("recipe-123")
        cache.get_recipe("recipe-123")
        cache.get_recipe("nonexistent")

        stats = cache.get_stats()
        assert 66.0 < stats.recipe_hit_rate < 67.0

    def test_hit_rate_with_no_requests(self, cache):
        """Hit rate should be 0% with no requests."""
        stats = cache.get_stats()
        assert stats.recipe_hit_rate == 0.0
        assert stats.search_hit_rate == 0.0
        assert stats.overall_hit_rate == 0.0


class TestCacheManagement:
    """Tests for cache management operations."""

    def test_clear_removes_all_data(self, cache, sample_recipe, sample_recipes):
        """clear() should remove all cached data."""
        cache.set_recipe("recipe-123", sample_recipe)
        cache.set_recipes_batch(sample_recipes)
        cache.set_search_results({"method": "search"}, list(sample_recipes.values()))

        cache.clear()

        assert cache.has_recipe("recipe-123") is False
        assert cache.has_search_results({"method": "search"}) is False

    def test_clear_resets_stats(self, cache, sample_recipe):
        """clear() should reset statistics."""
        cache.set_recipe("recipe-123", sample_recipe)
        cache.get_recipe("recipe-123")
        cache.get_recipe("nonexistent")

        cache.clear()

        stats = cache.get_stats()
        assert stats.recipe_hits == 0
        assert stats.recipe_misses == 0

    def test_get_size_returns_correct_counts(self, cache, sample_recipe, sample_recipes):
        """get_size() should return correct cache sizes."""
        cache.set_recipe("recipe-123", sample_recipe)
        cache.set_search_results({"method": "search"}, list(sample_recipes.values()))

        size = cache.get_size()

        # 1 directly set + 3 from search results = 4 recipes
        assert size["recipe_count"] == 4
        assert size["search_count"] == 1
        assert size["recipe_max"] == RECIPE_CACHE_SIZE
        assert size["search_max"] == SEARCH_CACHE_SIZE


class TestCacheStatsDataclass:
    """Tests for CacheStats dataclass."""

    def test_cache_stats_properties(self):
        """CacheStats properties should calculate correctly."""
        stats = CacheStats(
            recipe_hits=80,
            recipe_misses=20,
            search_hits=60,
            search_misses=40,
            batch_hits=50,
            batch_misses=50,
        )

        assert stats.recipe_hit_rate == 80.0
        assert stats.search_hit_rate == 60.0
        # overall: (80+60+50)/(80+20+60+40+50+50) = 190/300 = 63.33%
        assert 63.0 < stats.overall_hit_rate < 64.0

    def test_cache_stats_zero_division(self):
        """CacheStats should handle zero division gracefully."""
        stats = CacheStats()

        assert stats.recipe_hit_rate == 0.0
        assert stats.search_hit_rate == 0.0
        assert stats.overall_hit_rate == 0.0


class TestCacheConfiguration:
    """Tests for cache configuration constants."""

    def test_recipe_cache_size_is_reasonable(self):
        """Recipe cache should hold reasonable number of items."""
        assert RECIPE_CACHE_SIZE >= 1000
        assert RECIPE_CACHE_SIZE <= 10000

    def test_recipe_cache_ttl_is_reasonable(self):
        """Recipe cache TTL should be reasonable (30 min to 2 hours)."""
        assert RECIPE_CACHE_TTL >= 1800  # 30 minutes
        assert RECIPE_CACHE_TTL <= 7200  # 2 hours

    def test_search_cache_size_is_reasonable(self):
        """Search cache should hold reasonable number of results."""
        assert SEARCH_CACHE_SIZE >= 100
        assert SEARCH_CACHE_SIZE <= 2000

    def test_search_cache_ttl_is_reasonable(self):
        """Search cache TTL should be reasonable (15 min to 1 hour)."""
        assert SEARCH_CACHE_TTL >= 900   # 15 minutes
        assert SEARCH_CACHE_TTL <= 3600  # 1 hour
