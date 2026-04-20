"""Tests for config_service pure accessor methods.

Focus: the sync accessor methods (get_pairing_categories, get_meal_type_pairs,
get_ingredient_aliases) that operate on cached in-memory config. The async
DB-loading paths (_load_config, _load_reference_data) are out of scope here —
they're integration-level and exercised by the AI meal generation tests.

Singleton handling: ConfigService is a process singleton. Each test resets
the instance attributes so tests are independent.
"""

import pytest

from app.services.config_service import (
    ConfigService,
    IngredientInfo,
    MealGenerationConfig,
    ReferenceData,
)


@pytest.fixture(autouse=True)
def reset_config_singleton():
    """Clear the ConfigService singleton state between tests."""
    svc = ConfigService()
    svc._config = None
    svc._reference_data = None
    yield
    svc._config = None
    svc._reference_data = None


# ==================== get_pairing_categories ====================


class TestGetPairingCategories:
    def test_returns_empty_list_when_config_not_loaded(self):
        """With no config loaded, accessor returns [] (not raises)."""
        svc = ConfigService()
        assert svc.get_pairing_categories("north", "dal") == []

    def test_returns_direct_pairing_for_cuisine_category(self):
        """A cuisine:category key in the flat map returns its pairings."""
        svc = ConfigService()
        svc._config = MealGenerationConfig(
            pairing_rules_flat={
                "north:dal": ["rice", "roti"],
                "south:dal": ["rice"],
            }
        )
        assert svc.get_pairing_categories("north", "dal") == ["rice", "roti"]
        assert svc.get_pairing_categories("south", "dal") == ["rice"]

    def test_falls_back_to_any_cuisine_when_specific_missing(self):
        """If the exact cuisine:category key is missing, any *:category match
        is returned as a fallback."""
        svc = ConfigService()
        svc._config = MealGenerationConfig(
            pairing_rules_flat={
                "south:sabzi": ["rice", "chapati"],
            }
        )
        # north:sabzi isn't in the map; fallback pulls south:sabzi.
        assert svc.get_pairing_categories("north", "sabzi") == ["rice", "chapati"]

    def test_returns_empty_list_when_no_match_anywhere(self):
        svc = ConfigService()
        svc._config = MealGenerationConfig(
            pairing_rules_flat={"north:dal": ["rice"]}
        )
        assert svc.get_pairing_categories("east", "unknown_category") == []


# ==================== get_meal_type_pairs ====================


class TestGetMealTypePairs:
    def test_returns_empty_list_when_config_not_loaded(self):
        svc = ConfigService()
        assert svc.get_meal_type_pairs("breakfast") == []

    def test_parses_colon_separated_pair_strings(self):
        """meal_type_pairs stores strings like 'dal:rice'; accessor splits them."""
        svc = ConfigService()
        svc._config = MealGenerationConfig(
            meal_type_pairs={
                "dinner": ["dal:rice", "sabzi:roti"],
                "breakfast": ["paratha:chutney"],
            }
        )
        assert svc.get_meal_type_pairs("dinner") == [
            ("dal", "rice"),
            ("sabzi", "roti"),
        ]
        assert svc.get_meal_type_pairs("breakfast") == [("paratha", "chutney")]

    def test_skips_malformed_pairs_without_colon(self):
        """A string without a colon is silently skipped (defensive)."""
        svc = ConfigService()
        svc._config = MealGenerationConfig(
            meal_type_pairs={"lunch": ["dal:rice", "malformed-no-colon", "sabzi:roti"]}
        )
        assert svc.get_meal_type_pairs("lunch") == [
            ("dal", "rice"),
            ("sabzi", "roti"),
        ]

    def test_returns_empty_list_for_unknown_meal_type(self):
        svc = ConfigService()
        svc._config = MealGenerationConfig(meal_type_pairs={"dinner": ["dal:rice"]})
        assert svc.get_meal_type_pairs("brunch") == []


# ==================== get_ingredient_aliases ====================


class TestGetIngredientAliases:
    def test_returns_lowercased_ingredient_when_no_config(self):
        svc = ConfigService()
        assert svc.get_ingredient_aliases("Paneer") == ["paneer"]

    def test_returns_aliases_from_config_when_ingredient_is_primary_key(self):
        svc = ConfigService()
        svc._config = MealGenerationConfig(
            ingredient_aliases={"paneer": ["cottage cheese", "chenna"]}
        )
        result = svc.get_ingredient_aliases("Paneer")
        assert "paneer" in result
        assert "cottage cheese" in result
        assert "chenna" in result

    def test_finds_ingredient_via_alias_and_returns_primary(self):
        """Searching by an alias should surface the primary name + siblings."""
        svc = ConfigService()
        svc._config = MealGenerationConfig(
            ingredient_aliases={"paneer": ["cottage cheese", "chenna"]}
        )
        result = svc.get_ingredient_aliases("cottage cheese")
        assert "paneer" in result  # primary surfaced
        assert "cottage cheese" in result
        assert "chenna" in result

    def test_merges_aliases_from_reference_data(self):
        """Reference data ingredients contribute aliases too."""
        svc = ConfigService()
        svc._config = MealGenerationConfig()
        svc._reference_data = ReferenceData(
            ingredients=[
                IngredientInfo(
                    name="Tomato",
                    aliases=["tamatar", "tomaatar"],
                    category="vegetables",
                )
            ]
        )
        result = svc.get_ingredient_aliases("tamatar")
        assert "tamatar" in result
        assert "tomaatar" in result
        assert "tomato" in result

    def test_case_insensitive_matching(self):
        svc = ConfigService()
        svc._config = MealGenerationConfig(
            ingredient_aliases={"paneer": ["chenna"]}
        )
        result_upper = svc.get_ingredient_aliases("PANEER")
        assert "paneer" in result_upper
        assert "chenna" in result_upper
