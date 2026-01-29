"""Cache module for RasoiAI backend.

This module provides caching infrastructure to reduce Firestore reads
and improve meal generation performance.
"""

from app.cache.recipe_cache import RecipeCache, get_recipe_cache, warm_recipe_cache

__all__ = ["RecipeCache", "get_recipe_cache", "warm_recipe_cache"]
