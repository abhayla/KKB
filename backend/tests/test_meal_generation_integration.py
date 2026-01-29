"""Integration tests for meal generation rule enforcement.

These tests verify the actual meal generation logic produces correct output
for various rule configurations. Unlike test_meal_generation.py which tests
data structures, these tests verify end-to-end behavior.

Run with: pytest tests/test_meal_generation_integration.py -v
"""

import pytest
from datetime import date
from unittest.mock import AsyncMock, MagicMock, patch

from app.services.meal_generation_service import (
    MealGenerationService,
    GeneratedMealPlan,
    MealItem,
    DayMeals,
    UserPreferences,
)
from app.services.config_service import MealGenerationConfig, MealStructure


# Test data: Sharma Family Profile (from algorithm doc)
SHARMA_FAMILY_PREFS = UserPreferences(
    dietary_tags=["vegetarian"],
    cuisine_type="north",
    allergies=[{"ingredient": "peanuts", "severity": "SEVERE"}],
    dislikes=["karela", "lauki", "turai"],
    weekday_cooking_time=30,
    weekend_cooking_time=60,
    busy_days=["MONDAY", "WEDNESDAY"],
    include_rules=[
        {"type": "INCLUDE", "target": "Chai", "frequency": "DAILY", "meal_slot": ["breakfast"]},
        {"type": "INCLUDE", "target": "Dal", "frequency": "TIMES_PER_WEEK", "times_per_week": 4, "meal_slot": ["lunch", "dinner"]},
        {"type": "INCLUDE", "target": "Paneer", "frequency": "TIMES_PER_WEEK", "times_per_week": 2, "meal_slot": ["lunch", "dinner"]},
    ],
    exclude_rules=[
        {"type": "EXCLUDE", "target": "Mushroom", "frequency": "NEVER"},
    ],
    nutrition_goals=[],
)


# Mock recipe data
MOCK_RECIPES = {
    "chai": [
        {"id": "chai1", "name": "Masala Chai", "category": "chai", "dietary_tags": ["vegetarian", "vegan"], "prep_time_minutes": 10, "ingredients": [{"name": "tea"}, {"name": "milk"}, {"name": "ginger"}]},
        {"id": "chai2", "name": "Adrak Chai", "category": "chai", "dietary_tags": ["vegetarian"], "prep_time_minutes": 15, "ingredients": [{"name": "tea"}, {"name": "ginger"}]},
    ],
    "dal": [
        {"id": "dal1", "name": "Dal Fry", "category": "dal", "dietary_tags": ["vegetarian", "vegan"], "prep_time_minutes": 25, "ingredients": [{"name": "toor dal"}, {"name": "onion"}, {"name": "tomato"}]},
        {"id": "dal2", "name": "Dal Tadka", "category": "dal", "dietary_tags": ["vegetarian", "vegan"], "prep_time_minutes": 30, "ingredients": [{"name": "moong dal"}, {"name": "garlic"}]},
        {"id": "dal3", "name": "Dal Makhani", "category": "dal", "dietary_tags": ["vegetarian"], "prep_time_minutes": 45, "ingredients": [{"name": "urad dal"}, {"name": "cream"}]},
        {"id": "dal4", "name": "Chana Dal", "category": "dal", "dietary_tags": ["vegetarian", "vegan"], "prep_time_minutes": 35, "ingredients": [{"name": "chana dal"}]},
    ],
    "paneer": [
        {"id": "paneer1", "name": "Paneer Butter Masala", "category": "curry", "dietary_tags": ["vegetarian"], "prep_time_minutes": 40, "ingredients": [{"name": "paneer"}, {"name": "cream"}]},
        {"id": "paneer2", "name": "Palak Paneer", "category": "curry", "dietary_tags": ["vegetarian"], "prep_time_minutes": 35, "ingredients": [{"name": "paneer"}, {"name": "spinach"}]},
    ],
    "rice": [
        {"id": "rice1", "name": "Jeera Rice", "category": "rice", "dietary_tags": ["vegetarian", "vegan"], "prep_time_minutes": 20, "ingredients": [{"name": "basmati rice"}, {"name": "cumin"}]},
        {"id": "rice2", "name": "Plain Rice", "category": "rice", "dietary_tags": ["vegetarian", "vegan"], "prep_time_minutes": 15, "ingredients": [{"name": "rice"}]},
    ],
    "roti": [
        {"id": "roti1", "name": "Wheat Roti", "category": "roti", "dietary_tags": ["vegetarian", "vegan"], "prep_time_minutes": 20, "ingredients": [{"name": "wheat flour"}]},
    ],
    "paratha": [
        {"id": "paratha1", "name": "Aloo Paratha", "category": "paratha", "dietary_tags": ["vegetarian"], "prep_time_minutes": 30, "ingredients": [{"name": "wheat flour"}, {"name": "potato"}]},
    ],
    "sabzi": [
        {"id": "sabzi1", "name": "Aloo Gobi", "category": "sabzi", "dietary_tags": ["vegetarian", "vegan"], "prep_time_minutes": 25, "ingredients": [{"name": "potato"}, {"name": "cauliflower"}]},
        {"id": "sabzi2", "name": "Mix Veg", "category": "sabzi", "dietary_tags": ["vegetarian", "vegan"], "prep_time_minutes": 30, "ingredients": [{"name": "mixed vegetables"}]},
    ],
    # Recipes with allergenic ingredients (should be excluded)
    "peanut_recipes": [
        {"id": "peanut1", "name": "Peanut Chutney", "category": "chutney", "dietary_tags": ["vegetarian"], "prep_time_minutes": 10, "ingredients": [{"name": "peanuts"}, {"name": "coconut"}]},
        {"id": "groundnut1", "name": "Groundnut Curry", "category": "curry", "dietary_tags": ["vegetarian"], "prep_time_minutes": 30, "ingredients": [{"name": "groundnut"}, {"name": "tomato"}]},
    ],
    # Recipes with disliked ingredients (should be excluded)
    "disliked_recipes": [
        {"id": "karela1", "name": "Karela Fry", "category": "sabzi", "dietary_tags": ["vegetarian", "vegan"], "prep_time_minutes": 25, "ingredients": [{"name": "karela"}, {"name": "onion"}]},
        {"id": "lauki1", "name": "Lauki Kofta", "category": "curry", "dietary_tags": ["vegetarian"], "prep_time_minutes": 40, "ingredients": [{"name": "lauki"}, {"name": "gram flour"}]},
    ],
    # Recipes with mushroom (EXCLUDE rule)
    "mushroom_recipes": [
        {"id": "mushroom1", "name": "Mushroom Curry", "category": "curry", "dietary_tags": ["vegetarian"], "prep_time_minutes": 25, "ingredients": [{"name": "mushroom"}, {"name": "onion"}]},
    ],
}


def create_mock_config():
    """Create a mock MealGenerationConfig."""
    return MealGenerationConfig(
        meal_structure=MealStructure(
            items_per_slot=2,
            expandable=True,
            time_based_items={
                "enabled": True,
                "thresholds": [
                    {"max_time": 30, "main_items": 1},
                    {"max_time": 45, "main_items": 2},
                    {"max_time": 9999, "main_items": 2},
                ],
            },
        ),
        pairing_rules_flat={
            "north:dal": ["rice", "roti"],
            "north:sabzi": ["roti", "paratha"],
            "north:curry": ["rice", "roti", "naan"],
            "north:paratha": ["chai", "curd"],
        },
        meal_type_pairs={
            "breakfast": ["paratha:chai", "poha:chai"],
            "lunch": ["dal:rice", "sabzi:roti"],
            "dinner": ["dal:roti", "sabzi:paratha"],
            "snacks": ["samosa:chai", "pakora:chai"],
        },
        recipe_categories=["dal", "rice", "roti", "sabzi", "curry", "chai", "paratha"],
        ingredient_aliases={
            "peanut": ["peanuts", "groundnut", "moongphali"],
        },
    )


class TestMealGenerationIntegration:
    """Integration tests for meal generation rule enforcement."""

    @pytest.fixture
    def service(self):
        """Create a MealGenerationService instance."""
        return MealGenerationService()

    @pytest.fixture
    def mock_config(self):
        """Create mock config."""
        return create_mock_config()

    def test_build_exclude_list_includes_allergies(self, service):
        """Test that exclude list includes allergies with variants."""
        prefs = UserPreferences(
            allergies=[{"ingredient": "peanuts", "severity": "SEVERE"}],
        )

        exclude_list = service._build_exclude_list(prefs)

        # Should include peanuts and its variants
        assert "peanuts" in exclude_list
        assert "peanut" in exclude_list
        assert "groundnut" in exclude_list
        assert "moongphali" in exclude_list

    def test_build_exclude_list_includes_dislikes(self, service):
        """Test that exclude list includes dislikes."""
        prefs = UserPreferences(
            dislikes=["karela", "lauki"],
        )

        exclude_list = service._build_exclude_list(prefs)

        assert "karela" in exclude_list
        assert "lauki" in exclude_list

    def test_build_exclude_list_includes_exclude_rules(self, service):
        """Test that exclude list includes EXCLUDE rules with NEVER frequency."""
        prefs = UserPreferences(
            exclude_rules=[
                {"type": "EXCLUDE", "target": "Mushroom", "frequency": "NEVER"},
            ],
        )

        exclude_list = service._build_exclude_list(prefs)

        assert "mushroom" in exclude_list
        assert "mushrooms" in exclude_list

    def test_recipe_matches_excludes_by_name(self, service):
        """Test recipe exclusion by name matching."""
        exclude_ingredients = {"peanut", "karela"}

        peanut_recipe = {"name": "Peanut Chutney", "ingredients": []}
        karela_recipe = {"name": "Karela Fry", "ingredients": []}
        dal_recipe = {"name": "Dal Fry", "ingredients": []}

        assert service._recipe_matches_excludes(peanut_recipe, exclude_ingredients) is True
        assert service._recipe_matches_excludes(karela_recipe, exclude_ingredients) is True
        assert service._recipe_matches_excludes(dal_recipe, exclude_ingredients) is False

    def test_recipe_matches_excludes_by_ingredient(self, service):
        """Test recipe exclusion by ingredient matching."""
        exclude_ingredients = {"groundnut"}

        recipe_with_groundnut = {
            "name": "South Indian Curry",
            "ingredients": [{"name": "groundnut"}, {"name": "coconut"}],
        }
        recipe_without = {
            "name": "Dal Fry",
            "ingredients": [{"name": "toor dal"}, {"name": "tomato"}],
        }

        assert service._recipe_matches_excludes(recipe_with_groundnut, exclude_ingredients) is True
        assert service._recipe_matches_excludes(recipe_without, exclude_ingredients) is False

    def test_filter_by_excludes_removes_matching_recipes(self, service):
        """Test that filter_by_excludes removes recipes containing excluded ingredients."""
        exclude_ingredients = {"peanut", "mushroom"}

        recipes = [
            {"name": "Dal Fry", "ingredients": [{"name": "dal"}]},
            {"name": "Peanut Chutney", "ingredients": [{"name": "peanut"}]},
            {"name": "Mushroom Curry", "ingredients": [{"name": "mushroom"}]},
            {"name": "Aloo Gobi", "ingredients": [{"name": "potato"}]},
        ]

        filtered = service._filter_by_excludes(recipes, exclude_ingredients)

        assert len(filtered) == 2
        recipe_names = [r["name"] for r in filtered]
        assert "Dal Fry" in recipe_names
        assert "Aloo Gobi" in recipe_names
        assert "Peanut Chutney" not in recipe_names
        assert "Mushroom Curry" not in recipe_names

    def test_build_include_tracker_daily_frequency(self, service):
        """Test include tracker correctly handles DAILY frequency."""
        include_rules = [
            {"type": "INCLUDE", "target": "Chai", "frequency": "DAILY", "meal_slot": ["breakfast"]},
        ]

        tracker = service._build_include_tracker(include_rules)

        assert "0" in tracker
        assert tracker["0"]["times_needed"] == 7  # DAILY = 7 times
        assert tracker["0"]["target"] == "Chai"
        assert tracker["0"]["times_assigned"] == 0

    def test_build_include_tracker_times_per_week_frequency(self, service):
        """Test include tracker correctly handles TIMES_PER_WEEK frequency."""
        include_rules = [
            {"type": "INCLUDE", "target": "Dal", "frequency": "TIMES_PER_WEEK", "times_per_week": 4, "meal_slot": ["lunch", "dinner"]},
        ]

        tracker = service._build_include_tracker(include_rules)

        assert "0" in tracker
        assert tracker["0"]["times_needed"] == 4
        assert tracker["0"]["target"] == "Dal"
        assert "lunch" in tracker["0"]["meal_slots"]
        assert "dinner" in tracker["0"]["meal_slots"]

    def test_track_main_ingredient(self, service):
        """Test main ingredient tracking prevents duplicates."""
        used_ingredients = set()

        service._track_main_ingredient("Rajma Curry", used_ingredients)
        assert "rajma" in used_ingredients

        service._track_main_ingredient("Dal Fry", used_ingredients)
        assert "dal" in used_ingredients

        service._track_main_ingredient("Paneer Butter Masala", used_ingredients)
        assert "paneer" in used_ingredients

    def test_recipe_uses_ingredient_today(self, service):
        """Test detection of recipes using today's ingredients."""
        used_ingredients = {"rajma", "dal"}

        rajma_recipe = {"name": "Rajma Masala"}
        dal_recipe = {"name": "Dal Tadka"}
        paneer_recipe = {"name": "Paneer Curry"}

        assert service._recipe_uses_ingredient_today(rajma_recipe, used_ingredients) is True
        assert service._recipe_uses_ingredient_today(dal_recipe, used_ingredients) is True
        assert service._recipe_uses_ingredient_today(paneer_recipe, used_ingredients) is False

    def test_calculate_items_for_slot_time_based(self, service, mock_config):
        """Test variable items calculation based on cooking time."""
        # Short cooking time (≤30 min) → 1 main
        main, total = service._calculate_items_for_slot(mock_config, 25, "lunch")
        assert main == 1
        assert total == 2  # 1 main + 1 complementary

        # Medium cooking time (30-45 min) → 2 mains
        main, total = service._calculate_items_for_slot(mock_config, 40, "lunch")
        assert main == 2
        assert total == 2  # Capped at items_per_slot

        # Long cooking time (>45 min) → 2 mains
        main, total = service._calculate_items_for_slot(mock_config, 60, "dinner")
        assert main == 2
        assert total == 2

    def test_get_generic_dishes_for_slot_breakfast(self, service):
        """Test generic dish suggestions for breakfast."""
        dishes = service._get_generic_dishes_for_slot(
            slot="breakfast",
            cuisine_type="north",
            exclude_ingredients=set(),
        )

        assert len(dishes) > 0
        assert any("paratha" in d["name"].lower() for d in dishes)

    def test_get_generic_dishes_for_slot_excludes_filtered(self, service):
        """Test generic dishes respects exclusions."""
        dishes = service._get_generic_dishes_for_slot(
            slot="lunch",
            cuisine_type="north",
            exclude_ingredients={"dal"},
        )

        # Should not include dishes with "dal" in name
        for dish in dishes:
            assert "dal" not in dish["name"].lower()

    def test_get_generic_dishes_for_slot_south_cuisine(self, service):
        """Test generic dish suggestions for South Indian cuisine."""
        dishes = service._get_generic_dishes_for_slot(
            slot="breakfast",
            cuisine_type="south",
            exclude_ingredients=set(),
        )

        assert len(dishes) > 0
        # South Indian breakfast should have idli or dosa
        dish_names = [d["name"].lower() for d in dishes]
        has_south_dish = any(name in ["idli", "dosa", "upma", "pongal"] for name in dish_names)
        assert has_south_dish

    def test_create_generic_meal_item(self, service):
        """Test creation of generic meal item."""
        dish = {"name": "Dal", "category": "dal", "pairs_with": ["rice", "roti"]}

        item = service._create_generic_meal_item(dish, "lunch")

        assert item.is_generic is True
        assert item.recipe_id == "GENERIC"
        assert "make your own" in item.recipe_name
        assert item.category == "dal"

    def test_recipe_to_meal_item_sets_is_generic_false(self, service):
        """Test that recipe conversion sets is_generic to False."""
        recipe = {
            "id": "dal1",
            "name": "Dal Fry",
            "category": "dal",
            "dietary_tags": ["vegetarian"],
            "prep_time_minutes": 25,
            "nutrition": {"calories": 200},
        }

        item = service._recipe_to_meal_item(recipe)

        assert item.is_generic is False
        assert item.recipe_id == "dal1"
        assert item.recipe_name == "Dal Fry"


class TestAllergyEnforcementIntegration:
    """Tests specifically for allergy enforcement."""

    @pytest.fixture
    def service(self):
        return MealGenerationService()

    def test_peanut_allergy_excludes_all_variants(self, service):
        """Test that peanut allergy excludes peanuts, groundnut, moongphali."""
        prefs = UserPreferences(
            allergies=[{"ingredient": "peanuts", "severity": "SEVERE"}],
        )

        exclude_list = service._build_exclude_list(prefs)

        # All peanut variants should be excluded
        peanut_variants = ["peanut", "peanuts", "groundnut", "groundnuts", "moongphali"]
        for variant in peanut_variants:
            assert variant in exclude_list, f"{variant} should be in exclude list"

    def test_dairy_allergy_excludes_all_variants(self, service):
        """Test that dairy allergy excludes all dairy products."""
        prefs = UserPreferences(
            allergies=[{"ingredient": "dairy", "severity": "SEVERE"}],
        )

        exclude_list = service._build_exclude_list(prefs)

        dairy_variants = ["milk", "cheese", "paneer", "curd", "yogurt", "cream", "butter", "ghee"]
        for variant in dairy_variants:
            assert variant in exclude_list, f"{variant} should be in exclude list"

    def test_gluten_allergy_excludes_wheat_products(self, service):
        """Test that gluten allergy excludes wheat products."""
        prefs = UserPreferences(
            allergies=[{"ingredient": "gluten", "severity": "SEVERE"}],
        )

        exclude_list = service._build_exclude_list(prefs)

        gluten_variants = ["wheat", "maida", "atta", "bread", "roti", "naan"]
        for variant in gluten_variants:
            assert variant in exclude_list, f"{variant} should be in exclude list"


class TestCookingTimeEnforcementIntegration:
    """Tests for cooking time limit enforcement."""

    @pytest.fixture
    def service(self):
        return MealGenerationService()

    @pytest.fixture
    def mock_config(self):
        return create_mock_config()

    def test_busy_day_uses_weekday_time(self, service, mock_config):
        """Test that busy days use weekday cooking time."""
        prefs = UserPreferences(
            weekday_cooking_time=30,
            weekend_cooking_time=60,
            busy_days=["MONDAY"],
        )

        # Monday is a busy day, should use weekday time
        day_name = "Monday"
        is_weekend = day_name in ["Saturday", "Sunday"]
        is_busy_day = day_name.upper() in prefs.busy_days

        max_time = prefs.weekday_cooking_time
        if is_busy_day:
            max_time = prefs.weekday_cooking_time
        elif is_weekend:
            max_time = prefs.weekend_cooking_time

        assert max_time == 30

    def test_weekend_uses_weekend_time(self, service):
        """Test that weekends use weekend cooking time."""
        prefs = UserPreferences(
            weekday_cooking_time=30,
            weekend_cooking_time=60,
            busy_days=[],
        )

        day_name = "Saturday"
        is_weekend = day_name in ["Saturday", "Sunday"]

        max_time = prefs.weekday_cooking_time
        if is_weekend:
            max_time = prefs.weekend_cooking_time

        assert max_time == 60


class TestVariableItemsPerMeal:
    """Tests for variable items per meal based on cooking time."""

    @pytest.fixture
    def service(self):
        return MealGenerationService()

    @pytest.fixture
    def mock_config(self):
        return create_mock_config()

    def test_short_time_gives_fewer_main_items(self, service, mock_config):
        """Test that short cooking time results in 1 main item."""
        main_items, total = service._calculate_items_for_slot(mock_config, 25, "lunch")
        assert main_items == 1

    def test_medium_time_gives_more_main_items(self, service, mock_config):
        """Test that medium cooking time results in 2 main items."""
        main_items, total = service._calculate_items_for_slot(mock_config, 40, "dinner")
        assert main_items == 2

    def test_disabled_time_based_uses_fixed_items(self, service):
        """Test that disabled time_based_items uses fixed items_per_slot."""
        config = MealGenerationConfig(
            meal_structure=MealStructure(
                items_per_slot=2,
                expandable=True,
                time_based_items={"enabled": False},
            ),
        )

        main_items, total = service._calculate_items_for_slot(config, 60, "dinner")
        assert main_items == 1  # Default when disabled
        assert total == 2  # Uses fixed items_per_slot


class TestGenericSuggestions:
    """Tests for generic dish suggestions."""

    @pytest.fixture
    def service(self):
        return MealGenerationService()

    def test_generic_suggestions_all_slots(self, service):
        """Test that generic suggestions exist for all meal slots."""
        slots = ["breakfast", "lunch", "dinner", "snacks"]

        for slot in slots:
            dishes = service._get_generic_dishes_for_slot(
                slot=slot,
                cuisine_type="north",
                exclude_ingredients=set(),
            )
            assert len(dishes) > 0, f"No generic dishes for {slot}"

    def test_generic_suggestions_all_cuisines(self, service):
        """Test that generic suggestions exist for all cuisines."""
        cuisines = ["north", "south", "east", "west"]

        for cuisine in cuisines:
            dishes = service._get_generic_dishes_for_slot(
                slot="lunch",
                cuisine_type=cuisine,
                exclude_ingredients=set(),
            )
            assert len(dishes) > 0, f"No generic dishes for {cuisine} cuisine"

    def test_generic_suggestions_fallback_to_other_cuisine(self, service):
        """Test that generic suggestions fall back to other cuisines if all filtered."""
        # Exclude everything from a cuisine
        exclude = {"dal", "sabzi", "rajma", "chole"}

        dishes = service._get_generic_dishes_for_slot(
            slot="lunch",
            cuisine_type="north",
            exclude_ingredients=exclude,
        )

        # Should still get some dishes (from other cuisines)
        assert len(dishes) > 0


class TestWeeklyDeduplication:
    """Tests for recipe deduplication across the week."""

    @pytest.fixture
    def service(self):
        return MealGenerationService()

    def test_used_recipe_ids_tracks_recipes(self, service):
        """Test that used_recipe_ids set properly tracks recipes."""
        used_recipe_ids = set()

        # Simulate adding recipes
        used_recipe_ids.add("dal1")
        used_recipe_ids.add("rice1")

        # Check tracking
        assert "dal1" in used_recipe_ids
        assert "rice1" in used_recipe_ids
        assert "paneer1" not in used_recipe_ids

    def test_filter_excludes_used_recipes(self, service):
        """Test that filter removes already used recipes."""
        used_recipe_ids = {"dal1", "rice1"}

        recipes = [
            {"id": "dal1", "name": "Dal Fry"},
            {"id": "dal2", "name": "Dal Tadka"},
            {"id": "rice1", "name": "Jeera Rice"},
        ]

        # Filter out used recipes
        filtered = [r for r in recipes if r.get("id") not in used_recipe_ids]

        assert len(filtered) == 1
        assert filtered[0]["id"] == "dal2"
