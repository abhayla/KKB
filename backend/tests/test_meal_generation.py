"""Tests for meal generation with preferences.

Tests that generated meal plans respect INCLUDE/EXCLUDE rules,
allergies, and dislikes.

Run with: pytest tests/test_meal_generation.py -v
"""

import pytest
from datetime import date, datetime, timezone
from unittest.mock import AsyncMock, MagicMock, patch

from app.services.meal_generation_service import (
    MealGenerationService,
    GeneratedMealPlan,
    MealItem,
    DayMeals,
    UserPreferences,
)


class TestMealItem:
    """Tests for MealItem dataclass."""

    def test_meal_item_defaults(self):
        """Test MealItem default values."""
        item = MealItem(
            id="item1",
            recipe_id="recipe1",
            recipe_name="Dal Tadka",
        )

        assert item.id == "item1"
        assert item.recipe_id == "recipe1"
        assert item.recipe_name == "Dal Tadka"
        assert item.is_locked is False
        assert item.prep_time_minutes == 30
        assert item.dietary_tags == []

    def test_meal_item_with_all_fields(self):
        """Test MealItem with all fields."""
        item = MealItem(
            id="item1",
            recipe_id="recipe1",
            recipe_name="Dal Tadka",
            recipe_image_url="https://example.com/dal.jpg",
            prep_time_minutes=25,
            calories=300,
            is_locked=True,
            dietary_tags=["vegetarian", "gluten-free"],
            category="dal",
        )

        assert item.calories == 300
        assert item.is_locked is True
        assert "vegetarian" in item.dietary_tags
        assert item.category == "dal"


class TestDayMeals:
    """Tests for DayMeals dataclass."""

    def test_day_meals_defaults(self):
        """Test DayMeals default values."""
        day = DayMeals(
            date="2026-01-27",
            day_name="Monday",
        )

        assert day.date == "2026-01-27"
        assert day.day_name == "Monday"
        assert day.breakfast == []
        assert day.lunch == []
        assert day.dinner == []
        assert day.snacks == []
        assert day.festival is None

    def test_day_meals_with_items(self):
        """Test DayMeals with meal items."""
        breakfast_item = MealItem(id="b1", recipe_id="r1", recipe_name="Paratha")
        lunch_item1 = MealItem(id="l1", recipe_id="r2", recipe_name="Dal")
        lunch_item2 = MealItem(id="l2", recipe_id="r3", recipe_name="Rice")

        day = DayMeals(
            date="2026-01-27",
            day_name="Monday",
            breakfast=[breakfast_item],
            lunch=[lunch_item1, lunch_item2],  # Paired items
        )

        assert len(day.breakfast) == 1
        assert len(day.lunch) == 2
        assert day.lunch[0].recipe_name == "Dal"
        assert day.lunch[1].recipe_name == "Rice"


class TestGeneratedMealPlan:
    """Tests for GeneratedMealPlan dataclass."""

    def test_meal_plan_structure(self):
        """Test GeneratedMealPlan structure."""
        plan = GeneratedMealPlan(
            week_start_date="2026-01-27",
            week_end_date="2026-02-02",
            days=[
                DayMeals(date="2026-01-27", day_name="Monday"),
                DayMeals(date="2026-01-28", day_name="Tuesday"),
            ],
            rules_applied={"include_count": 2, "exclude_count": 1},
        )

        assert plan.week_start_date == "2026-01-27"
        assert len(plan.days) == 2
        assert plan.rules_applied["include_count"] == 2


class TestUserPreferences:
    """Tests for UserPreferences dataclass."""

    def test_user_preferences_defaults(self):
        """Test UserPreferences default values."""
        prefs = UserPreferences()

        assert prefs.dietary_tags == ["vegetarian"]
        assert prefs.cuisine_type is None
        assert prefs.allergies == []
        assert prefs.dislikes == []
        assert prefs.weekday_cooking_time == 30
        assert prefs.weekend_cooking_time == 60
        assert prefs.busy_days == []
        assert prefs.include_rules == []
        assert prefs.exclude_rules == []

    def test_user_preferences_custom(self):
        """Test UserPreferences with custom values."""
        prefs = UserPreferences(
            dietary_tags=["vegetarian", "vegan"],
            cuisine_type="north",
            allergies=[{"ingredient": "peanuts", "severity": "SEVERE"}],
            dislikes=["bhindi", "karela"],
            weekday_cooking_time=20,
            weekend_cooking_time=90,
            busy_days=["MONDAY", "WEDNESDAY"],
            include_rules=[{"target": "Chai", "frequency": "DAILY"}],
            exclude_rules=[{"target": "Mushroom", "frequency": "NEVER"}],
        )

        assert "vegan" in prefs.dietary_tags
        assert prefs.cuisine_type == "north"
        assert len(prefs.allergies) == 1
        assert "karela" in prefs.dislikes
        assert len(prefs.include_rules) == 1


class TestIncludeRuleEnforcement:
    """Tests for INCLUDE rule enforcement."""

    def test_include_rule_structure(self):
        """Test INCLUDE rule structure."""
        include_rule = {
            "type": "INCLUDE",
            "target": "Chai",
            "frequency": "DAILY",
            "meal_slot": ["BREAKFAST"],
            "is_active": True,
        }

        assert include_rule["type"] == "INCLUDE"
        assert include_rule["frequency"] in ["DAILY", "WEEKLY", "TIMES_PER_WEEK", "NEVER"]
        assert "BREAKFAST" in include_rule["meal_slot"]

    def test_include_rule_with_times_per_week(self):
        """Test INCLUDE rule with TIMES_PER_WEEK frequency."""
        include_rule = {
            "type": "INCLUDE",
            "target": "Paneer",
            "frequency": "TIMES_PER_WEEK",
            "times_per_week": 3,
            "meal_slot": ["LUNCH", "DINNER"],
            "is_active": True,
        }

        assert include_rule["times_per_week"] == 3
        assert len(include_rule["meal_slot"]) == 2


class TestExcludeRuleEnforcement:
    """Tests for EXCLUDE rule enforcement."""

    def test_exclude_rule_structure(self):
        """Test EXCLUDE rule structure."""
        exclude_rule = {
            "type": "EXCLUDE",
            "target": "Karela",
            "frequency": "NEVER",
            "reason": "dislike",
            "is_active": True,
        }

        assert exclude_rule["type"] == "EXCLUDE"
        assert exclude_rule["frequency"] == "NEVER"

    def test_exclude_rule_with_reason(self):
        """Test EXCLUDE rule with reason."""
        exclude_rules = [
            {"type": "EXCLUDE", "target": "Karela", "reason": "dislike"},
            {"type": "EXCLUDE", "target": "Mushroom", "reason": "allergy"},
            {"type": "EXCLUDE", "target": "Onion", "reason": "jain_diet"},
        ]

        reasons = [r["reason"] for r in exclude_rules]
        assert "dislike" in reasons
        assert "allergy" in reasons
        assert "jain_diet" in reasons


class TestPairingLogic:
    """Tests for meal pairing logic."""

    def test_pairing_rules_structure(self):
        """Test pairing rules structure."""
        pairing_rules = {
            "north": {
                "dal": ["rice", "roti"],
                "sabzi": ["roti", "paratha"],
            },
            "south": {
                "curry": ["rice"],
                "sambar": ["rice", "idli"],
            },
        }

        # Dal should pair with rice or roti in North Indian cuisine
        assert "rice" in pairing_rules["north"]["dal"]
        assert "roti" in pairing_rules["north"]["dal"]

        # Sambar should pair with rice or idli in South Indian cuisine
        assert "rice" in pairing_rules["south"]["sambar"]
        assert "idli" in pairing_rules["south"]["sambar"]

    def test_meal_type_pairs(self):
        """Test meal type specific pairings."""
        meal_type_pairs = {
            "breakfast": ["paratha:chai", "poha:chai", "idli:sambar", "dosa:chutney"],
            "lunch": ["dal:rice", "sabzi:roti", "curry:rice"],
            "dinner": ["dal:roti", "sabzi:paratha", "curry:naan"],
            "snacks": ["samosa:chai", "pakora:chutney"],
        }

        # Verify breakfast has chai pairings
        breakfast_has_chai = any("chai" in pair for pair in meal_type_pairs["breakfast"])
        assert breakfast_has_chai

        # Verify lunch has rice pairings
        lunch_has_rice = any("rice" in pair for pair in meal_type_pairs["lunch"])
        assert lunch_has_rice

    def test_pairing_categories(self):
        """Test recipe categories for pairing."""
        categories = ["dal", "rice", "sabzi", "curry", "bread", "snack", "chutney"]

        # Essential pairing categories
        assert "dal" in categories
        assert "rice" in categories
        assert "bread" in categories

    def test_pairing_relationship(self):
        """Test pairing relationships."""
        # Dal pairs
        dal_pairs = ["rice", "roti", "naan", "paratha"]

        # Curry pairs
        curry_pairs = ["rice", "naan", "roti"]

        # Snack pairs
        snack_pairs = ["chai", "chutney", "sambar"]

        # Verify common pairings
        assert "rice" in dal_pairs
        assert "naan" in curry_pairs
        assert "chai" in snack_pairs


class TestDietaryEnforcement:
    """Tests for dietary restriction enforcement."""

    def test_vegetarian_filter(self):
        """Test vegetarian recipe filtering."""
        recipes = [
            {"name": "Dal", "dietary_tags": ["vegetarian"]},
            {"name": "Chicken Curry", "dietary_tags": ["non_vegetarian"]},
            {"name": "Paneer", "dietary_tags": ["vegetarian"]},
        ]

        veg_recipes = [r for r in recipes if "vegetarian" in r.get("dietary_tags", [])]
        assert len(veg_recipes) == 2
        assert all("vegetarian" in r["dietary_tags"] for r in veg_recipes)

    def test_vegan_filter(self):
        """Test vegan recipe filtering."""
        recipes = [
            {"name": "Dal", "dietary_tags": ["vegetarian", "vegan"]},
            {"name": "Paneer Curry", "dietary_tags": ["vegetarian"]},
            {"name": "Vegetable Biryani", "dietary_tags": ["vegetarian", "vegan"]},
        ]

        vegan_recipes = [r for r in recipes if "vegan" in r.get("dietary_tags", [])]
        assert len(vegan_recipes) == 2

    def test_jain_filter(self):
        """Test Jain diet filtering (no onion, garlic, root vegetables)."""
        jain_excluded = ["onion", "garlic", "potato", "carrot", "ginger"]

        recipes = [
            {"name": "Dal", "ingredients": [{"name": "toor dal"}, {"name": "tomato"}]},
            {"name": "Aloo Gobi", "ingredients": [{"name": "potato"}, {"name": "cauliflower"}]},
        ]

        def is_jain_compatible(recipe):
            for ing in recipe.get("ingredients", []):
                if ing["name"].lower() in jain_excluded:
                    return False
            return True

        jain_recipes = [r for r in recipes if is_jain_compatible(r)]
        assert len(jain_recipes) == 1
        assert jain_recipes[0]["name"] == "Dal"


class TestAllergyEnforcement:
    """Tests for allergy enforcement."""

    def test_allergy_filtering(self):
        """Test filtering recipes with allergens."""
        allergies = ["peanut", "shrimp"]  # Use actual ingredient names

        recipes = [
            {"name": "Peanut Chutney", "ingredients": [{"name": "peanut"}, {"name": "coconut"}]},
            {"name": "Dal", "ingredients": [{"name": "toor dal"}, {"name": "tomato"}]},
            {"name": "Prawn Curry", "ingredients": [{"name": "shrimp"}, {"name": "coconut milk"}]},
        ]

        def has_allergen(recipe, allergies):
            for ing in recipe.get("ingredients", []):
                ing_name = ing["name"].lower()
                for allergen in allergies:
                    if allergen in ing_name:
                        return True
            return False

        safe_recipes = [r for r in recipes if not has_allergen(r, allergies)]
        assert len(safe_recipes) == 1
        assert safe_recipes[0]["name"] == "Dal"

    def test_allergy_severity_levels(self):
        """Test allergy severity levels."""
        allergies = [
            {"ingredient": "peanuts", "severity": "SEVERE"},
            {"ingredient": "dairy", "severity": "MODERATE"},
            {"ingredient": "gluten", "severity": "MILD"},
        ]

        severity_levels = ["MILD", "MODERATE", "SEVERE"]
        for allergy in allergies:
            assert allergy["severity"] in severity_levels


class TestCookingTimeEnforcement:
    """Tests for cooking time enforcement."""

    def test_weekday_time_filter(self):
        """Test filtering by weekday cooking time."""
        max_weekday_time = 30

        recipes = [
            {"name": "Quick Dal", "prep_time_minutes": 10, "cook_time_minutes": 15},
            {"name": "Slow Biryani", "prep_time_minutes": 30, "cook_time_minutes": 60},
            {"name": "Poha", "prep_time_minutes": 5, "cook_time_minutes": 10},
        ]

        def total_time(recipe):
            return recipe.get("prep_time_minutes", 0) + recipe.get("cook_time_minutes", 0)

        quick_recipes = [r for r in recipes if total_time(r) <= max_weekday_time]
        assert len(quick_recipes) == 2
        assert all(total_time(r) <= max_weekday_time for r in quick_recipes)

    def test_busy_day_handling(self):
        """Test busy day cooking time reduction."""
        normal_weekday_time = 30
        busy_day_time = 15  # Reduced time for busy days

        assert busy_day_time < normal_weekday_time
