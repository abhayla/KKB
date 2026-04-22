"""Tests for ai_recipe_catalog_service pure helpers.

Covers:
- normalize_recipe_name: whitespace/case normalisation
- _passes_dietary_filter: all diet compatibility paths

The async catalog_recipes / search_catalog functions are integration-level
(bulk SQL) and are exercised by the AI meal generation pipeline tests.
"""

import pytest

from app.services.ai_recipe_catalog_service import (
    DIETARY_EXCLUSIONS,
    REQUIRE_OWN_TAG,
    _passes_dietary_filter,
    normalize_recipe_name,
)


# ==================== normalize_recipe_name ====================


class TestNormalizeRecipeName:
    def test_lowercases(self):
        assert normalize_recipe_name("Paneer Butter Masala") == "paneer butter masala"

    def test_strips_leading_and_trailing_whitespace(self):
        assert normalize_recipe_name("  Dal Tadka  ") == "dal tadka"

    def test_preserves_internal_whitespace(self):
        """Internal single spaces should be preserved — only leading/trailing stripped."""
        assert normalize_recipe_name("  Aloo  Paratha  ") == "aloo  paratha"

    def test_idempotent(self):
        once = normalize_recipe_name("Paneer Tikka")
        twice = normalize_recipe_name(once)
        assert once == twice

    def test_empty_string(self):
        assert normalize_recipe_name("") == ""


# ==================== _passes_dietary_filter ====================


class TestPassesDietaryFilter:
    def test_no_user_diet_means_no_filter(self):
        """Empty user_dietary_tags means every recipe passes."""
        assert _passes_dietary_filter(["non_vegetarian"], []) is True
        assert _passes_dietary_filter([], []) is True

    def test_vegetarian_rejects_non_veg_recipe(self):
        assert _passes_dietary_filter(["non_vegetarian"], ["vegetarian"]) is False

    def test_vegetarian_accepts_vegetarian_recipe(self):
        assert _passes_dietary_filter(["vegetarian"], ["vegetarian"]) is True

    def test_vegetarian_accepts_untagged_recipe(self):
        """vegetarian is not in REQUIRE_OWN_TAG, so untagged recipe passes."""
        assert _passes_dietary_filter([], ["vegetarian"]) is True

    def test_vegan_requires_vegan_tag_even_without_non_veg(self):
        """vegan is in REQUIRE_OWN_TAG — a plain vegetarian recipe fails."""
        assert _passes_dietary_filter(["vegetarian"], ["vegan"]) is False

    def test_vegan_accepts_vegan_tagged_recipe(self):
        assert _passes_dietary_filter(["vegetarian", "vegan"], ["vegan"]) is True

    def test_vegan_rejects_non_veg(self):
        assert _passes_dietary_filter(["non_vegetarian"], ["vegan"]) is False

    def test_jain_requires_jain_tag(self):
        assert _passes_dietary_filter(["vegetarian"], ["jain"]) is False
        assert _passes_dietary_filter(["vegetarian", "jain"], ["jain"]) is True

    def test_sattvic_requires_sattvic_tag(self):
        assert _passes_dietary_filter(["vegetarian"], ["sattvic"]) is False
        assert _passes_dietary_filter(["vegetarian", "sattvic"], ["sattvic"]) is True

    def test_eggetarian_accepts_non_veg_when_also_eggetarian(self):
        """Special-case: a non_veg recipe that's ALSO tagged eggetarian is OK."""
        assert _passes_dietary_filter(
            ["non_vegetarian", "eggetarian"], ["eggetarian"]
        ) is True

    def test_eggetarian_rejects_plain_non_veg(self):
        """Without the eggetarian tag, a non_veg recipe is still rejected."""
        assert _passes_dietary_filter(["non_vegetarian"], ["eggetarian"]) is False

    def test_non_vegetarian_has_no_exclusions(self):
        assert _passes_dietary_filter(["non_vegetarian"], ["non_vegetarian"]) is True
        assert _passes_dietary_filter(["vegetarian"], ["non_vegetarian"]) is True

    def test_case_insensitive(self):
        """Both tag sets are compared lowercased."""
        assert _passes_dietary_filter(["VEGETARIAN"], ["VEGETARIAN"]) is True
        assert _passes_dietary_filter(["Non_Vegetarian"], ["Vegetarian"]) is False


# ==================== Constant shape ====================


class TestDietaryConstants:
    @pytest.mark.parametrize(
        "diet",
        ["vegetarian", "vegan", "jain", "sattvic", "eggetarian", "non_vegetarian", "halal"],
    )
    def test_every_expected_diet_is_in_exclusions_map(self, diet):
        assert diet in DIETARY_EXCLUSIONS

    def test_require_own_tag_is_subset_of_exclusions(self):
        """Every diet in REQUIRE_OWN_TAG must also have an exclusions entry."""
        assert REQUIRE_OWN_TAG.issubset(set(DIETARY_EXCLUSIONS.keys()))
