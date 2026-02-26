"""Tests for complete family-safe meal generation.

Verifies that all user preference fields reach the AI prompt,
pre-prompt conflict filtering works correctly, and family safety
post-processing removes unsafe meals.
"""

import uuid
import pytest
from datetime import date, timedelta

from app.services.ai_meal_service import (
    AIMealService,
    DayMeals,
    GeneratedMealPlan,
    MealItem,
    UserPreferences,
)


@pytest.fixture
def service():
    """Create AIMealService instance."""
    return AIMealService()


@pytest.fixture
def week_start():
    """Next Monday."""
    today = date.today()
    return today - timedelta(days=today.weekday()) + timedelta(weeks=1)


@pytest.fixture
def base_prefs():
    """Base preferences with new fields set."""
    return UserPreferences(
        dietary_tags=["vegetarian"],
        cuisine_preferences=["north"],
        family_size=4,
        dietary_type="non-vegetarian",
        cooking_skill_level="beginner",
        allow_recipe_repeat=True,
        strict_allergen_mode=False,
        strict_dietary_mode=False,
        nutrition_goals=[
            {"food_category": "LEAFY_GREENS", "weekly_target": 5, "enforcement": "REQUIRED"},
            {"food_category": "PROTEIN", "weekly_target": 7, "enforcement": "PREFERRED"},
        ],
    )


def _make_meal_item(name, ingredients=None):
    """Helper to create a MealItem with given name and optional ingredients."""
    return MealItem(
        id=str(uuid.uuid4()),
        recipe_name=name,
        ingredients=ingredients,
    )


def _make_plan(days_data):
    """Helper to create a GeneratedMealPlan from simplified day data.

    days_data: list of dicts with keys breakfast, lunch, dinner, snacks
    each value is a list of (name, ingredients) tuples.
    """
    days = []
    start = date.today()
    for i, dd in enumerate(days_data):
        d = start + timedelta(days=i)
        days.append(DayMeals(
            date=d.isoformat(),
            day_name=d.strftime("%A"),
            breakfast=[_make_meal_item(n, ing) for n, ing in dd.get("breakfast", [])],
            lunch=[_make_meal_item(n, ing) for n, ing in dd.get("lunch", [])],
            dinner=[_make_meal_item(n, ing) for n, ing in dd.get("dinner", [])],
            snacks=[_make_meal_item(n, ing) for n, ing in dd.get("snacks", [])],
        ))
    return GeneratedMealPlan(
        week_start_date=start.isoformat(),
        week_end_date=(start + timedelta(days=6)).isoformat(),
        days=days,
    )


# ==============================================================================
# Class 1: Test Missing Fields Reach Prompt
# ==============================================================================


class TestMissingFieldsReachPrompt:
    """Verify all new UserPreferences fields appear in the AI prompt."""

    def test_dietary_type_in_prompt(self, service, base_prefs, week_start):
        prompt = service._build_prompt(base_prefs, {}, None, week_start)
        assert "Primary Diet: NON-VEGETARIAN" in prompt

    def test_dietary_type_default_vegetarian(self, service, week_start):
        prefs = UserPreferences()
        prompt = service._build_prompt(prefs, {}, None, week_start)
        assert "Primary Diet: VEGETARIAN" in prompt

    def test_cooking_skill_level_in_prompt(self, service, base_prefs, week_start):
        prompt = service._build_prompt(base_prefs, {}, None, week_start)
        assert "Cooking Skill: beginner" in prompt

    def test_allow_recipe_repeat_true(self, service, base_prefs, week_start):
        base_prefs.allow_recipe_repeat = True
        prompt = service._build_prompt(base_prefs, {}, None, week_start)
        assert "Allow repeating favorite recipes" in prompt

    def test_allow_recipe_repeat_false(self, service, week_start):
        prefs = UserPreferences(allow_recipe_repeat=False)
        prompt = service._build_prompt(prefs, {}, None, week_start)
        assert "Vary recipes across the week" in prompt

    def test_strict_allergen_mode_in_prompt(self, service, base_prefs, week_start):
        base_prefs.strict_allergen_mode = False
        prompt = service._build_prompt(base_prefs, {}, None, week_start)
        assert "minor traces acceptable" in prompt

    def test_strict_dietary_mode_in_prompt(self, service, base_prefs, week_start):
        base_prefs.strict_dietary_mode = False
        prompt = service._build_prompt(base_prefs, {}, None, week_start)
        assert "occasional exceptions" in prompt

    def test_nutrition_goals_present(self, service, base_prefs, week_start):
        prompt = service._build_prompt(base_prefs, {}, None, week_start)
        assert "LEAFY_GREENS" in prompt
        assert "5 servings/week" in prompt
        assert "PROTEIN" in prompt
        assert "7 servings/week" in prompt

    def test_nutrition_goals_empty(self, service, week_start):
        prefs = UserPreferences(nutrition_goals=[])
        prompt = service._build_prompt(prefs, {}, None, week_start)
        assert "No nutrition goals set" in prompt


# ==============================================================================
# Class 2: Test Pre-Prompt Conflict Filter
# ==============================================================================


class TestPrePromptConflictFilter:
    """Verify INCLUDE rules conflicting with family constraints are filtered."""

    def test_jain_member_filters_onion_rule(self, service):
        prefs = UserPreferences(
            include_rules=[{"target": "Onion Curry", "frequency": "WEEKLY", "times_per_week": 1}],
            family_members=[{"name": "Dadiji", "health_conditions": [], "dietary_restrictions": ["jain"]}],
        )
        filtered, report = service._filter_conflicting_rules(prefs)
        assert len(filtered) == 0
        assert len(report) == 1
        assert report[0]["member_name"] == "Dadiji"
        assert report[0]["constraint_keyword"] == "onion"

    def test_diabetic_member_filters_sweet_rule(self, service):
        prefs = UserPreferences(
            include_rules=[{"target": "Gulab Jamun", "frequency": "WEEKLY", "times_per_week": 1}],
            family_members=[{"name": "Dadaji", "health_conditions": ["diabetic"], "dietary_restrictions": []}],
        )
        filtered, report = service._filter_conflicting_rules(prefs)
        assert len(filtered) == 0
        assert report[0]["constraint_keyword"] == "gulab jamun"

    def test_no_conflict_preserves_all_rules(self, service):
        prefs = UserPreferences(
            include_rules=[
                {"target": "Dal Tadka", "frequency": "WEEKLY", "times_per_week": 2},
                {"target": "Masala Chai", "frequency": "DAILY"},
            ],
            family_members=[{"name": "Dadaji", "health_conditions": ["diabetic"], "dietary_restrictions": []}],
        )
        filtered, report = service._filter_conflicting_rules(prefs)
        assert len(filtered) == 2
        assert len(report) == 0

    def test_empty_family_preserves_all_rules(self, service):
        prefs = UserPreferences(
            include_rules=[{"target": "Onion Paratha", "frequency": "WEEKLY", "times_per_week": 1}],
            family_members=[],
        )
        filtered, report = service._filter_conflicting_rules(prefs)
        assert len(filtered) == 1
        assert len(report) == 0

    def test_multiple_conflicts_filter_multiple_rules(self, service):
        prefs = UserPreferences(
            include_rules=[
                {"target": "Onion Pakora", "frequency": "WEEKLY", "times_per_week": 1},
                {"target": "Garlic Naan", "frequency": "WEEKLY", "times_per_week": 2},
                {"target": "Dal Tadka", "frequency": "WEEKLY", "times_per_week": 3},
            ],
            family_members=[{"name": "Dadiji", "health_conditions": [], "dietary_restrictions": ["jain"]}],
        )
        filtered, report = service._filter_conflicting_rules(prefs)
        assert len(filtered) == 1  # Only Dal Tadka survives
        assert filtered[0]["target"] == "Dal Tadka"
        assert len(report) == 2

    def test_report_structure(self, service):
        prefs = UserPreferences(
            include_rules=[{"target": "Garlic Bread", "frequency": "WEEKLY", "times_per_week": 1}],
            family_members=[{"name": "Dadiji", "health_conditions": [], "dietary_restrictions": ["jain"]}],
        )
        _, report = service._filter_conflicting_rules(prefs)
        assert len(report) == 1
        entry = report[0]
        assert "rule_target" in entry
        assert "member_name" in entry
        assert "constraint_keyword" in entry
        assert entry["rule_target"] == "Garlic Bread"
        assert entry["member_name"] == "Dadiji"
        assert entry["constraint_keyword"] == "garlic"

    def test_partial_match_garlic_naan_for_jain(self, service):
        prefs = UserPreferences(
            include_rules=[{"target": "Garlic Naan", "frequency": "WEEKLY", "times_per_week": 1}],
            family_members=[{"name": "Dadiji", "health_conditions": [], "dietary_restrictions": ["jain"]}],
        )
        filtered, report = service._filter_conflicting_rules(prefs)
        assert len(filtered) == 0
        assert report[0]["constraint_keyword"] == "garlic"


# ==============================================================================
# Class 3: Test Family Safety Post-Processing
# ==============================================================================


class TestFamilySafetyPostProcessing:
    """Verify _enforce_rules removes meals unsafe for family members."""

    def _make_prefs_with_member(self, member):
        return UserPreferences(family_members=[member])

    def test_jain_onion_dish_removed(self, service):
        prefs = self._make_prefs_with_member(
            {"name": "Dadiji", "health_conditions": [], "dietary_restrictions": ["jain"]}
        )
        plan = _make_plan([{"breakfast": [("Onion Paratha", None), ("Chai", None)]}])
        result = service._enforce_rules(plan, prefs)
        names = [item.recipe_name for item in result.days[0].breakfast]
        assert "Onion Paratha" not in names
        assert "Chai" in names

    def test_jain_garlic_dish_removed(self, service):
        prefs = self._make_prefs_with_member(
            {"name": "Dadiji", "health_conditions": [], "dietary_restrictions": ["jain"]}
        )
        plan = _make_plan([{"lunch": [("Garlic Dal", None), ("Rice", None)]}])
        result = service._enforce_rules(plan, prefs)
        names = [item.recipe_name for item in result.days[0].lunch]
        assert "Garlic Dal" not in names
        assert "Rice" in names

    def test_diabetic_sweet_removed(self, service):
        prefs = self._make_prefs_with_member(
            {"name": "Dadaji", "health_conditions": ["diabetic"], "dietary_restrictions": []}
        )
        plan = _make_plan([{"snacks": [("Gulab Jamun", None), ("Chai", None)]}])
        result = service._enforce_rules(plan, prefs)
        names = [item.recipe_name for item in result.days[0].snacks]
        assert "Gulab Jamun" not in names
        assert "Chai" in names

    def test_low_salt_pickle_removed(self, service):
        prefs = self._make_prefs_with_member(
            {"name": "Dadaji", "health_conditions": ["low_salt"], "dietary_restrictions": []}
        )
        plan = _make_plan([{"lunch": [("Dal", None), ("Pickle Rice", None)]}])
        result = service._enforce_rules(plan, prefs)
        names = [item.recipe_name for item in result.days[0].lunch]
        assert "Pickle Rice" not in names
        assert "Dal" in names

    def test_no_spicy_chili_removed(self, service):
        prefs = self._make_prefs_with_member(
            {"name": "Chhoti", "health_conditions": ["no_spicy"], "dietary_restrictions": []}
        )
        plan = _make_plan([{"dinner": [("Green Chili Paneer", None), ("Roti", None)]}])
        result = service._enforce_rules(plan, prefs)
        names = [item.recipe_name for item in result.days[0].dinner]
        assert "Green Chili Paneer" not in names
        assert "Roti" in names

    def test_safe_dishes_preserved(self, service):
        prefs = self._make_prefs_with_member(
            {"name": "Dadaji", "health_conditions": ["diabetic"], "dietary_restrictions": []}
        )
        plan = _make_plan([{"lunch": [("Dal Tadka", None), ("Jeera Rice", None)]}])
        result = service._enforce_rules(plan, prefs)
        names = [item.recipe_name for item in result.days[0].lunch]
        assert "Dal Tadka" in names
        assert "Jeera Rice" in names

    def test_ingredient_level_check(self, service):
        """Items with forbidden keywords in ingredient names are removed."""
        prefs = self._make_prefs_with_member(
            {"name": "Dadiji", "health_conditions": [], "dietary_restrictions": ["jain"]}
        )
        # Recipe name is safe but ingredient contains "onion"
        ingredients = [{"name": "Onion", "quantity": 1, "unit": "medium"}]
        plan = _make_plan([{"dinner": [("Mystery Curry", ingredients), ("Roti", None)]}])
        result = service._enforce_rules(plan, prefs)
        names = [item.recipe_name for item in result.days[0].dinner]
        assert "Mystery Curry" not in names
        assert "Roti" in names

    def test_no_members_no_removal(self, service):
        prefs = UserPreferences(family_members=[])
        plan = _make_plan([{"breakfast": [("Onion Paratha", None), ("Chai", None)]}])
        result = service._enforce_rules(plan, prefs)
        names = [item.recipe_name for item in result.days[0].breakfast]
        assert "Onion Paratha" in names
        assert "Chai" in names


# ==============================================================================
# Class 4: Test Load Preferences Completeness
# ==============================================================================


class TestLoadPreferencesCompleteness:
    """Verify UserPreferences dataclass has correct defaults for new fields."""

    def test_defaults_for_new_fields(self):
        prefs = UserPreferences()
        assert prefs.dietary_type == "vegetarian"
        assert prefs.cooking_skill_level == "intermediate"
        assert prefs.allow_recipe_repeat is False
        assert prefs.strict_allergen_mode is True
        assert prefs.strict_dietary_mode is True
        assert prefs.nutrition_goals == []

    def test_custom_values_preserved(self):
        prefs = UserPreferences(
            dietary_type="non-vegetarian",
            cooking_skill_level="expert",
            allow_recipe_repeat=True,
            strict_allergen_mode=False,
            strict_dietary_mode=False,
            nutrition_goals=[{"food_category": "PROTEIN", "weekly_target": 10, "enforcement": "REQUIRED"}],
        )
        assert prefs.dietary_type == "non-vegetarian"
        assert prefs.cooking_skill_level == "expert"
        assert prefs.allow_recipe_repeat is True
        assert prefs.strict_allergen_mode is False
        assert prefs.strict_dietary_mode is False
        assert len(prefs.nutrition_goals) == 1

    def test_conflict_report_in_prompt(self):
        """Filtered rules report appears in the prompt."""
        svc = AIMealService()
        today = date.today()
        ws = today - timedelta(days=today.weekday()) + timedelta(weeks=1)
        prefs = UserPreferences()
        report = [
            {"rule_target": "Onion Curry", "member_name": "Dadiji", "constraint_keyword": "onion"},
        ]
        prompt = svc._build_prompt(prefs, {}, None, ws, filtered_rules_report=report)
        assert "REMOVED 'Onion Curry'" in prompt
        assert "Dadiji" in prompt
        assert "onion" in prompt


# ==============================================================================
# Class 5: Test Hindi Alias Constraint Enforcement
# ==============================================================================


class TestHindiAliasConstraints:
    """Verify Hindi-named dishes are caught by family constraint checks."""

    def test_jain_aloo_dish_filtered_prefilter(self, service):
        """Aloo Paratha INCLUDE rule is filtered for Jain member (Hindi alias)."""
        prefs = UserPreferences(
            include_rules=[{"target": "Aloo Paratha", "frequency": "WEEKLY", "times_per_week": 2}],
            family_members=[{"name": "Dadiji", "health_conditions": [], "dietary_restrictions": ["jain"]}],
        )
        filtered, report = service._filter_conflicting_rules(prefs)
        assert len(filtered) == 0
        assert len(report) == 1
        assert report[0]["member_name"] == "Dadiji"
        assert report[0]["constraint_keyword"] == "aloo"

    def test_jain_aloo_dish_removed_postprocess(self, service):
        """Aloo Paratha is removed in post-processing for Jain member (Hindi alias)."""
        prefs = UserPreferences(
            family_members=[{"name": "Dadiji", "health_conditions": [], "dietary_restrictions": ["jain"]}],
        )
        plan = _make_plan([{"breakfast": [("Aloo Paratha", None), ("Chai", None)]}])
        result = service._enforce_rules(plan, prefs)
        names = [item.recipe_name for item in result.days[0].breakfast]
        assert "Aloo Paratha" not in names
        assert "Chai" in names

    def test_diabetic_mithai_removed(self, service):
        """Rasgulla is removed for diabetic member (Hindi alias)."""
        prefs = UserPreferences(
            family_members=[{"name": "Dadaji", "health_conditions": ["diabetic"], "dietary_restrictions": []}],
        )
        plan = _make_plan([{"snacks": [("Rasgulla", None), ("Chai", None)]}])
        result = service._enforce_rules(plan, prefs)
        names = [item.recipe_name for item in result.days[0].snacks]
        assert "Rasgulla" not in names
        assert "Chai" in names

    def test_sattvic_pyaaz_removed(self, service):
        """Pyaaz Kachori is removed for Sattvic member (Hindi alias)."""
        prefs = UserPreferences(
            family_members=[{"name": "Dadiji", "health_conditions": [], "dietary_restrictions": ["sattvic"]}],
        )
        plan = _make_plan([{"snacks": [("Pyaaz Kachori", None), ("Chai", None)]}])
        result = service._enforce_rules(plan, prefs)
        names = [item.recipe_name for item in result.days[0].snacks]
        assert "Pyaaz Kachori" not in names
        assert "Chai" in names
