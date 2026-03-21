"""Tests for fasting day post-validation in _enforce_rules().

Verifies that fasting day constraints are enforced after AI generation,
even if Gemini includes forbidden items on fasting days.
"""
import pytest

from app.services.ai_meal_service import (
    AIMealService,
    MealItem,
    DayMeals,
    GeneratedMealPlan,
    UserPreferences,
)


@pytest.fixture
def service():
    return AIMealService()


@pytest.fixture
def base_prefs():
    return UserPreferences(
        dietary_tags=["vegetarian"],
        cuisine_preferences=["north"],
        allergies=[],
        dislikes=[],
        weekday_cooking_time=30,
        weekend_cooking_time=60,
        busy_days=[],
        include_rules=[],
        exclude_rules=[],
    )


def _make_item(name, category="other"):
    return MealItem(
        id=f"item-{name.lower().replace(' ', '-')}",
        recipe_name=name,
        prep_time_minutes=20,
        dietary_tags=["vegetarian"],
        category=category,
    )


def _make_day(date_str, day_name, breakfast, lunch, dinner, snacks, festival=None):
    return DayMeals(
        date=date_str,
        day_name=day_name,
        breakfast=breakfast,
        lunch=lunch,
        dinner=dinner,
        snacks=snacks,
        festival=festival,
    )


class TestFastingDayValidation:

    def test_fasting_day_removes_grain_recipes(self, service, base_prefs):
        plan = GeneratedMealPlan(
            week_start_date="2026-03-23",
            week_end_date="2026-03-29",
            days=[
                _make_day(
                    "2026-03-24", "Tuesday",
                    breakfast=[_make_item("Sabudana Khichdi"), _make_item("Wheat Paratha")],
                    lunch=[_make_item("Jeera Rice"), _make_item("Fruit Salad")],
                    dinner=[_make_item("Makhana Curry")],
                    snacks=[_make_item("Roti Roll")],
                    festival={"name": "Ekadashi", "is_fasting_day": True, "special_foods": [], "avoided_foods": []},
                )
            ],
            rules_applied={},
        )
        result = service._enforce_rules(plan, base_prefs)
        day = result.days[0]
        assert len(day.breakfast) == 1
        assert day.breakfast[0].recipe_name == "Sabudana Khichdi"
        assert len(day.lunch) == 1
        assert day.lunch[0].recipe_name == "Fruit Salad"
        assert len(day.dinner) == 1
        assert len(day.snacks) == 0

    def test_fasting_day_removes_onion_garlic_recipes(self, service, base_prefs):
        plan = GeneratedMealPlan(
            week_start_date="2026-03-23",
            week_end_date="2026-03-29",
            days=[
                _make_day(
                    "2026-03-24", "Tuesday",
                    breakfast=[_make_item("Potato Cutlet")],
                    lunch=[_make_item("Onion Pakora"), _make_item("Pumpkin Soup")],
                    dinner=[_make_item("Garlic Naan"), _make_item("Paneer Tikka")],
                    snacks=[],
                    festival={"name": "Ekadashi", "is_fasting_day": True, "special_foods": [], "avoided_foods": []},
                )
            ],
            rules_applied={},
        )
        result = service._enforce_rules(plan, base_prefs)
        day = result.days[0]
        assert len(day.lunch) == 1
        assert day.lunch[0].recipe_name == "Pumpkin Soup"
        assert len(day.dinner) == 1
        assert day.dinner[0].recipe_name == "Paneer Tikka"

    def test_non_fasting_day_keeps_all_recipes(self, service, base_prefs):
        plan = GeneratedMealPlan(
            week_start_date="2026-03-23",
            week_end_date="2026-03-29",
            days=[
                _make_day(
                    "2026-03-25", "Wednesday",
                    breakfast=[_make_item("Wheat Paratha"), _make_item("Onion Uttapam")],
                    lunch=[_make_item("Jeera Rice")],
                    dinner=[_make_item("Garlic Naan")],
                    snacks=[_make_item("Roti Roll")],
                    festival=None,
                )
            ],
            rules_applied={},
        )
        result = service._enforce_rules(plan, base_prefs)
        day = result.days[0]
        assert len(day.breakfast) == 2
        assert len(day.lunch) == 1
        assert len(day.dinner) == 1
        assert len(day.snacks) == 1

    def test_fasting_day_allows_special_foods(self, service, base_prefs):
        plan = GeneratedMealPlan(
            week_start_date="2026-03-23",
            week_end_date="2026-03-29",
            days=[
                _make_day(
                    "2026-03-24", "Tuesday",
                    breakfast=[_make_item("Sabudana Khichdi"), _make_item("Makhana Kheer"), _make_item("Fruit Chaat")],
                    lunch=[_make_item("Sweet Potato Curry")],
                    dinner=[_make_item("Paneer Tikka")],
                    snacks=[_make_item("Coconut Ladoo")],
                    festival={"name": "Ekadashi", "is_fasting_day": True, "special_foods": ["Sabudana"], "avoided_foods": []},
                )
            ],
            rules_applied={},
        )
        result = service._enforce_rules(plan, base_prefs)
        day = result.days[0]
        assert len(day.breakfast) == 3
        assert len(day.lunch) == 1
        assert len(day.snacks) == 1

    def test_multiple_fasting_days_validated(self, service, base_prefs):
        plan = GeneratedMealPlan(
            week_start_date="2026-03-23",
            week_end_date="2026-03-29",
            days=[
                _make_day(
                    "2026-03-24", "Tuesday",
                    breakfast=[_make_item("Wheat Paratha")],
                    lunch=[_make_item("Fruit Salad")],
                    dinner=[], snacks=[],
                    festival={"name": "Ekadashi", "is_fasting_day": True, "special_foods": [], "avoided_foods": []},
                ),
                _make_day(
                    "2026-03-26", "Thursday",
                    breakfast=[_make_item("Poha")],
                    lunch=[_make_item("Rice Bowl")],
                    dinner=[], snacks=[],
                    festival={"name": "Purnima", "is_fasting_day": True, "special_foods": [], "avoided_foods": ["poha"]},
                ),
            ],
            rules_applied={},
        )
        result = service._enforce_rules(plan, base_prefs)
        assert len(result.days[0].breakfast) == 0
        assert len(result.days[0].lunch) == 1
        assert len(result.days[1].breakfast) == 0
        assert len(result.days[1].lunch) == 0
