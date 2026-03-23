"""Integration tests: recipe rule enforcement during meal generation.

Tests the deterministic post-processing logic in AIMealService._enforce_rules()
which removes EXCLUDE/allergen/fasting violations from AI-generated plans,
and verifies that INCLUDE/EXCLUDE rules appear correctly in the Gemini prompt.

Gemini is mocked throughout — these tests verify OUR enforcement logic, not AI behavior.
"""

import json
import uuid
from datetime import date, timedelta
from unittest.mock import AsyncMock, patch

import pytest
import pytest_asyncio
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.recipe_rule import RecipeRule
from app.models.user import User, UserPreferences as UserPreferencesModel
from app.services.ai_meal_service import (
    AIMealService,
    DayMeals,
    GeneratedMealPlan,
    MealItem,
    UserPreferences,
)
from tests.factories import make_user, make_preferences


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _make_item(name: str, **kwargs) -> MealItem:
    """Create a MealItem with sensible defaults."""
    return MealItem(
        id=str(uuid.uuid4()),
        recipe_name=name,
        prep_time_minutes=kwargs.get("prep_time_minutes", 30),
        dietary_tags=kwargs.get("dietary_tags", ["vegetarian"]),
        category=kwargs.get("category", "curry"),
        calories=kwargs.get("calories", 300),
    )


def _make_day(day_date: date, items_per_slot: int = 2, festival: dict = None,
              breakfast=None, lunch=None, dinner=None, snacks=None) -> DayMeals:
    """Create a DayMeals with default items unless overridden."""
    default_b = [_make_item("Poha"), _make_item("Masala Chai")]
    default_l = [_make_item("Dal Tadka"), _make_item("Jeera Rice")]
    default_d = [_make_item("Paneer Butter Masala"), _make_item("Roti")]
    default_s = [_make_item("Samosa"), _make_item("Chai")]

    return DayMeals(
        date=day_date.isoformat(),
        day_name=day_date.strftime("%A"),
        breakfast=breakfast if breakfast is not None else default_b[:items_per_slot],
        lunch=lunch if lunch is not None else default_l[:items_per_slot],
        dinner=dinner if dinner is not None else default_d[:items_per_slot],
        snacks=snacks if snacks is not None else default_s[:items_per_slot],
        festival=festival,
    )


def _make_plan(start: date, days: list[DayMeals] = None) -> GeneratedMealPlan:
    """Create a GeneratedMealPlan spanning 7 days."""
    if days is None:
        days = [_make_day(start + timedelta(days=i)) for i in range(7)]
    return GeneratedMealPlan(
        week_start_date=start.isoformat(),
        week_end_date=(start + timedelta(days=6)).isoformat(),
        days=days,
    )


def _build_gemini_response(start: date, overrides: dict[int, dict] = None) -> str:
    """Build a fake Gemini JSON response (short-key format) for 7 days.

    ``overrides`` maps day-index (0-6) to a dict of slot overrides, e.g.
    ``{0: {"b": [{"n": "Onion Paratha", "t": 25}]}}`` to inject specific items.
    """
    overrides = overrides or {}
    days = []
    for i in range(7):
        d = start + timedelta(days=i)
        day = {
            "d": d.isoformat(),
            "dn": d.strftime("%A"),
            "b": [{"n": "Poha", "t": 15}, {"n": "Chai", "t": 10}],
            "l": [{"n": "Dal Tadka", "t": 30}, {"n": "Jeera Rice", "t": 20}],
            "di": [{"n": "Aloo Gobi", "t": 35}, {"n": "Roti", "t": 15}],
            "s": [{"n": "Samosa", "t": 20}, {"n": "Masala Chai", "t": 10}],
        }
        if i in overrides:
            day.update(overrides[i])
        days.append(day)
    return json.dumps({"days": days})


async def _insert_rule(db: AsyncSession, user_id: str, **kwargs) -> RecipeRule:
    """Insert a RecipeRule directly into the database."""
    defaults = {
        "id": str(uuid.uuid4()),
        "user_id": user_id,
        "target_type": "INGREDIENT",
        "action": "INCLUDE",
        "target_name": "Dal",
        "frequency_type": "DAILY",
        "frequency_count": None,
        "frequency_days": None,
        "enforcement": "REQUIRED",
        "meal_slot": None,
        "is_active": True,
        "sync_status": "SYNCED",
        "force_override": False,
        "scope": "PERSONAL",
    }
    defaults.update(kwargs)
    rule = RecipeRule(**defaults)
    db.add(rule)
    await db.commit()
    await db.refresh(rule)
    return rule


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------

WEEK_START = date(2026, 3, 23)  # Monday


@pytest_asyncio.fixture
async def meal_user(db_session: AsyncSession) -> User:
    """User with preferences, ready for meal generation tests."""
    user = make_user(name="Rule Test User")
    db_session.add(user)
    prefs = make_preferences(user.id, dietary_type="vegetarian")
    db_session.add(prefs)
    await db_session.commit()
    await db_session.refresh(user)
    return user


# ---------------------------------------------------------------------------
# Tests
# ---------------------------------------------------------------------------


class TestIncludeRuleAppearsInPrompt:
    """Verify that INCLUDE rules inserted in the DB appear in the Gemini prompt."""

    async def test_include_rule_appears_in_generation_prompt(
        self, db_session: AsyncSession, meal_user: User
    ):
        # Arrange: create an INCLUDE rule for "Paneer"
        await _insert_rule(
            db_session, meal_user.id,
            action="INCLUDE",
            target_name="Paneer",
            frequency_type="TIMES_PER_WEEK",
            frequency_count=3,
        )

        service = AIMealService()

        captured_prompt = None

        async def fake_generate(prompt, **kwargs):
            nonlocal captured_prompt
            captured_prompt = prompt
            return _build_gemini_response(WEEK_START)

        from tests.conftest import _test_session_maker

        def mock_sm():
            return _test_session_maker()

        with patch("app.services.ai_meal_service.generate_text", side_effect=fake_generate), \
             patch("app.services.ai_meal_service.async_session_maker", mock_sm), \
             patch("app.repositories.user_repository.async_session_maker", mock_sm):
            await service.generate_meal_plan(meal_user.id, WEEK_START)

        assert captured_prompt is not None, "Prompt was never captured — generate_text not called"
        assert "Paneer" in captured_prompt, (
            "INCLUDE rule target 'Paneer' should appear in the Gemini prompt"
        )
        assert "3x/week" in captured_prompt, (
            "Frequency '3x/week' should appear in the prompt for the Paneer rule"
        )


class TestExcludeRuleRemovesFromResult:
    """Verify that EXCLUDE rules remove matching items from the generated plan."""

    async def test_exclude_rule_removes_from_result(self):
        """EXCLUDE 'Onion' (NEVER) removes items with 'Onion' in their name."""
        service = AIMealService()

        prefs = UserPreferences(
            exclude_rules=[
                {"target": "onion", "frequency": "NEVER", "specific_days": []},
            ],
        )

        # Plant an "Onion Paratha" in Monday breakfast
        days = [_make_day(WEEK_START + timedelta(days=i)) for i in range(7)]
        days[0].breakfast = [_make_item("Onion Paratha"), _make_item("Masala Chai")]

        plan = _make_plan(WEEK_START, days=days)

        result = service._enforce_rules(plan, prefs)

        monday_breakfast_names = [item.recipe_name for item in result.days[0].breakfast]
        assert "Onion Paratha" not in monday_breakfast_names, (
            "Onion Paratha should have been removed by EXCLUDE rule for 'onion'"
        )
        assert "Masala Chai" in monday_breakfast_names, (
            "Non-excluded items should remain"
        )

    async def test_exclude_specific_days_only_removes_on_those_days(self):
        """EXCLUDE 'Rice' on MONDAY should remove rice on Monday but not Tuesday."""
        service = AIMealService()

        prefs = UserPreferences(
            exclude_rules=[
                {
                    "target": "rice",
                    "frequency": "SPECIFIC_DAYS",
                    "specific_days": ["MONDAY"],
                },
            ],
        )

        days = [_make_day(WEEK_START + timedelta(days=i)) for i in range(7)]
        days[0].lunch = [_make_item("Jeera Rice"), _make_item("Dal Tadka")]
        days[1].lunch = [_make_item("Jeera Rice"), _make_item("Dal Tadka")]

        plan = _make_plan(WEEK_START, days=days)
        result = service._enforce_rules(plan, prefs)

        monday_lunch = [item.recipe_name for item in result.days[0].lunch]
        tuesday_lunch = [item.recipe_name for item in result.days[1].lunch]

        assert "Jeera Rice" not in monday_lunch, "Rice should be excluded on Monday"
        assert "Jeera Rice" in tuesday_lunch, "Rice should remain on Tuesday"


class TestFastingDayExcludesAvoidedFoods:
    """Verify that fasting day enforcement removes avoided foods."""

    async def test_fasting_day_excludes_avoided_foods(self):
        service = AIMealService()

        prefs = UserPreferences()

        # Monday is a fasting day with custom avoided foods
        festival_info = {
            "name": "Ekadashi",
            "is_fasting_day": True,
            "avoided_foods": ["lentil", "tomato"],
            "special_foods": ["Sabudana Khichdi"],
        }

        days = [_make_day(WEEK_START + timedelta(days=i)) for i in range(7)]
        days[0] = _make_day(
            WEEK_START,
            festival=festival_info,
            lunch=[_make_item("Dal Tadka"), _make_item("Tomato Rice")],
        )

        plan = _make_plan(WEEK_START, days=days)
        result = service._enforce_rules(plan, prefs)

        monday_lunch = [item.recipe_name for item in result.days[0].lunch]
        # "rice" is in the default fasting exclusions, and "tomato" is in avoided_foods
        assert "Tomato Rice" not in monday_lunch, (
            "Tomato Rice should be removed on fasting day (rice + tomato both excluded)"
        )

    async def test_fasting_default_exclusions_remove_grain_and_onion(self):
        """Default fasting exclusions include grain, rice, onion, garlic, etc."""
        service = AIMealService()

        prefs = UserPreferences()

        festival_info = {
            "name": "Navratri",
            "is_fasting_day": True,
            "avoided_foods": [],
        }

        days = [_make_day(WEEK_START + timedelta(days=i)) for i in range(7)]
        days[0] = _make_day(
            WEEK_START,
            festival=festival_info,
            dinner=[_make_item("Garlic Naan"), _make_item("Sabudana Khichdi")],
        )

        plan = _make_plan(WEEK_START, days=days)
        result = service._enforce_rules(plan, prefs)

        monday_dinner = [item.recipe_name for item in result.days[0].dinner]
        assert "Garlic Naan" not in monday_dinner, (
            "Garlic Naan should be removed: both 'garlic' and 'naan' are in default fasting exclusions"
        )
        # Sabudana Khichdi contains "khichdi" but the keyword match checks whole words;
        # the default exclusion set has "rice", "wheat", "garlic", etc., not "khichdi".
        assert "Sabudana Khichdi" in monday_dinner, (
            "Sabudana Khichdi should survive — it is a traditional fasting food"
        )


class TestMultipleRulesAppliedTogether:
    """Verify that INCLUDE + EXCLUDE rules are enforced simultaneously."""

    async def test_multiple_rules_applied_together(self):
        """INCLUDE 'Dal' + EXCLUDE 'Paneer' — Dal stays, Paneer removed."""
        service = AIMealService()

        prefs = UserPreferences(
            include_rules=[
                {
                    "target": "dal",
                    "frequency": "WEEKLY",
                    "times_per_week": 3,
                    "meal_slot": ["lunch", "dinner"],
                },
            ],
            exclude_rules=[
                {"target": "paneer", "frequency": "NEVER", "specific_days": []},
            ],
        )

        days = [_make_day(WEEK_START + timedelta(days=i)) for i in range(7)]
        # Plant Paneer in several dinners
        days[0].dinner = [_make_item("Paneer Tikka"), _make_item("Naan")]
        days[1].dinner = [_make_item("Paneer Butter Masala"), _make_item("Roti")]
        # Plant Dal in lunches
        days[0].lunch = [_make_item("Dal Tadka"), _make_item("Jeera Rice")]
        days[2].lunch = [_make_item("Dal Fry"), _make_item("Roti")]

        plan = _make_plan(WEEK_START, days=days)
        result = service._enforce_rules(plan, prefs)

        # Paneer should be removed from all days
        for day in result.days:
            for slot_name in ["breakfast", "lunch", "dinner", "snacks"]:
                items = getattr(day, slot_name, [])
                for item in items:
                    assert not item.recipe_name.lower().startswith("paneer"), (
                        f"Paneer item '{item.recipe_name}' should have been excluded "
                        f"from {day.date} {slot_name}"
                    )

        # Dal items should survive (INCLUDE does not add items, but it should not remove them)
        monday_lunch = [item.recipe_name for item in result.days[0].lunch]
        assert "Dal Tadka" in monday_lunch, "INCLUDE rule items should not be removed"

        wednesday_lunch = [item.recipe_name for item in result.days[2].lunch]
        assert "Dal Fry" in wednesday_lunch, "INCLUDE rule items should not be removed"


class TestAllergenStrictModeRemovesAllergens:
    """Verify allergen enforcement removes items containing allergens."""

    async def test_allergen_removes_peanut_items(self):
        """Peanut allergen removes items with 'peanut' and variants like 'groundnut'."""
        service = AIMealService()

        prefs = UserPreferences(
            allergies=[{"ingredient": "peanut", "severity": "SEVERE"}],
            strict_allergen_mode=True,
        )

        days = [_make_day(WEEK_START + timedelta(days=i)) for i in range(7)]
        days[0].snacks = [_make_item("Peanut Chikki"), _make_item("Masala Chai")]
        days[1].lunch = [_make_item("Groundnut Curry"), _make_item("Rice")]

        plan = _make_plan(WEEK_START, days=days)
        result = service._enforce_rules(plan, prefs)

        monday_snacks = [item.recipe_name for item in result.days[0].snacks]
        assert "Peanut Chikki" not in monday_snacks, (
            "Peanut Chikki should be removed for peanut allergy"
        )
        assert "Masala Chai" in monday_snacks, "Non-allergenic items should remain"

        tuesday_lunch = [item.recipe_name for item in result.days[1].lunch]
        assert "Groundnut Curry" not in tuesday_lunch, (
            "Groundnut Curry should be removed — 'groundnut' is a peanut variant"
        )

    async def test_allergen_dairy_removes_paneer_and_curd(self):
        """Dairy allergen removes items with paneer, curd, yogurt, etc."""
        service = AIMealService()

        prefs = UserPreferences(
            allergies=[{"ingredient": "dairy", "severity": "MODERATE"}],
        )

        days = [_make_day(WEEK_START + timedelta(days=i)) for i in range(7)]
        days[0].dinner = [_make_item("Paneer Tikka"), _make_item("Roti")]
        days[1].breakfast = [_make_item("Curd Rice"), _make_item("Pickle")]

        plan = _make_plan(WEEK_START, days=days)
        result = service._enforce_rules(plan, prefs)

        monday_dinner = [item.recipe_name for item in result.days[0].dinner]
        assert "Paneer Tikka" not in monday_dinner, (
            "Paneer should be removed for dairy allergy"
        )

        tuesday_breakfast = [item.recipe_name for item in result.days[1].breakfast]
        assert "Curd Rice" not in tuesday_breakfast, (
            "Curd should be removed for dairy allergy"
        )

    async def test_allergen_string_format_also_works(self):
        """Allergies can also be plain strings, not just dicts."""
        service = AIMealService()

        prefs = UserPreferences(
            allergies=["egg"],
        )

        days = [_make_day(WEEK_START + timedelta(days=i)) for i in range(7)]
        days[0].breakfast = [_make_item("Egg Bhurji"), _make_item("Toast")]

        plan = _make_plan(WEEK_START, days=days)
        result = service._enforce_rules(plan, prefs)

        monday_breakfast = [item.recipe_name for item in result.days[0].breakfast]
        assert "Egg Bhurji" not in monday_breakfast, (
            "Egg Bhurji should be removed for egg allergy (string format)"
        )


class TestIncludeRuleFrequencyRespected:
    """Verify that INCLUDE rule frequency constraints reach the prompt."""

    async def test_rule_with_frequency_respected(
        self, db_session: AsyncSession, meal_user: User
    ):
        """INCLUDE 'Curd' TIMES_PER_WEEK=3 should produce '3x/week' in the prompt."""
        await _insert_rule(
            db_session, meal_user.id,
            action="INCLUDE",
            target_name="Curd",
            frequency_type="TIMES_PER_WEEK",
            frequency_count=3,
        )

        service = AIMealService()

        captured_prompt = None

        async def fake_generate(prompt, **kwargs):
            nonlocal captured_prompt
            captured_prompt = prompt
            return _build_gemini_response(WEEK_START)

        from tests.conftest import _test_session_maker

        def mock_sm():
            return _test_session_maker()

        with patch("app.services.ai_meal_service.generate_text", side_effect=fake_generate), \
             patch("app.services.ai_meal_service.async_session_maker", mock_sm), \
             patch("app.repositories.user_repository.async_session_maker", mock_sm):
            await service.generate_meal_plan(meal_user.id, WEEK_START)

        assert captured_prompt is not None
        assert "Curd" in captured_prompt
        assert "3x/week" in captured_prompt, (
            "INCLUDE rule with TIMES_PER_WEEK=3 should produce '3x/week' in prompt"
        )

    async def test_include_daily_frequency_in_prompt(
        self, db_session: AsyncSession, meal_user: User
    ):
        """INCLUDE 'Roti' DAILY should produce 'DAILY' in the prompt."""
        await _insert_rule(
            db_session, meal_user.id,
            action="INCLUDE",
            target_name="Roti",
            frequency_type="DAILY",
        )

        service = AIMealService()

        captured_prompt = None

        async def fake_generate(prompt, **kwargs):
            nonlocal captured_prompt
            captured_prompt = prompt
            return _build_gemini_response(WEEK_START)

        from tests.conftest import _test_session_maker

        def mock_sm():
            return _test_session_maker()

        with patch("app.services.ai_meal_service.generate_text", side_effect=fake_generate), \
             patch("app.services.ai_meal_service.async_session_maker", mock_sm), \
             patch("app.repositories.user_repository.async_session_maker", mock_sm):
            await service.generate_meal_plan(meal_user.id, WEEK_START)

        assert captured_prompt is not None
        assert "Roti" in captured_prompt
        assert "DAILY" in captured_prompt, (
            "INCLUDE rule with DAILY frequency should produce 'DAILY' in prompt"
        )


class TestExcludeRuleAppearsInPrompt:
    """Verify that EXCLUDE rules are also included in the Gemini prompt."""

    async def test_exclude_rule_appears_in_prompt(
        self, db_session: AsyncSession, meal_user: User
    ):
        await _insert_rule(
            db_session, meal_user.id,
            action="EXCLUDE",
            target_name="Bitter Gourd",
            frequency_type="NEVER",
        )

        service = AIMealService()

        captured_prompt = None

        async def fake_generate(prompt, **kwargs):
            nonlocal captured_prompt
            captured_prompt = prompt
            return _build_gemini_response(WEEK_START)

        from tests.conftest import _test_session_maker

        def mock_sm():
            return _test_session_maker()

        with patch("app.services.ai_meal_service.generate_text", side_effect=fake_generate), \
             patch("app.services.ai_meal_service.async_session_maker", mock_sm), \
             patch("app.repositories.user_repository.async_session_maker", mock_sm):
            await service.generate_meal_plan(meal_user.id, WEEK_START)

        assert captured_prompt is not None
        assert "Bitter Gourd" in captured_prompt, (
            "EXCLUDE rule target should appear in the Gemini prompt"
        )
        assert "NEVER" in captured_prompt, (
            "EXCLUDE NEVER frequency should appear in the prompt"
        )
