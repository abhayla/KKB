"""Unit tests for AI Meal Service.

Tests cover:
- Prompt building with user preferences
- INCLUDE/EXCLUDE rule formatting
- Response parsing
- Post-processing enforcement
- Retry logic (mocked)
"""

import json
import pytest
from datetime import date, timedelta
from unittest.mock import AsyncMock, patch, MagicMock

from app.services.ai_meal_service import (
    AIMealService,
    UserPreferences,
    MealItem,
    DayMeals,
    GeneratedMealPlan,
)


# ==============================================================================
# FIXTURES
# ==============================================================================


@pytest.fixture
def service():
    """Create AIMealService instance."""
    return AIMealService()


@pytest.fixture
def sample_preferences():
    """Sample user preferences for testing."""
    return UserPreferences(
        dietary_tags=["vegetarian", "eggetarian"],
        cuisine_preferences=["north", "west"],
        allergies=[{"ingredient": "peanuts", "severity": "SEVERE"}],
        dislikes=["karela", "lauki"],
        weekday_cooking_time=30,
        weekend_cooking_time=60,
        busy_days=["MONDAY", "WEDNESDAY"],
        include_rules=[
            {
                "type": "INCLUDE",
                "target": "Chai",
                "frequency": "DAILY",
                "meal_slot": ["breakfast", "snacks"],
                "is_active": True,
            },
            {
                "type": "INCLUDE",
                "target": "Dal",
                "frequency": "TIMES_PER_WEEK",
                "times_per_week": 4,
                "meal_slot": ["lunch", "dinner"],
                "is_active": True,
            },
        ],
        exclude_rules=[
            {
                "type": "EXCLUDE",
                "target": "Mushroom",
                "frequency": "NEVER",
                "is_active": True,
            },
            {
                "type": "EXCLUDE",
                "target": "Onion",
                "frequency": "SPECIFIC_DAYS",
                "specific_days": ["TUESDAY"],
                "is_active": True,
            },
        ],
        family_size=4,
        spice_level="medium",
    )


@pytest.fixture
def sample_festivals():
    """Sample festivals for testing."""
    today = date.today()
    return {
        today
        + timedelta(days=2): {
            "name": "Ekadashi",
            "is_fasting_day": True,
            "special_foods": ["Sabudana Khichdi", "Fruits"],
            "avoided_foods": ["grains", "onion", "garlic"],
        }
    }


@pytest.fixture
def valid_ai_response():
    """Valid AI response JSON for testing."""
    week_start = date.today() - timedelta(days=date.today().weekday())
    days = []

    for i in range(7):
        current = week_start + timedelta(days=i)
        days.append(
            {
                "date": current.isoformat(),
                "day_name": current.strftime("%A"),
                "breakfast": [
                    {
                        "recipe_name": "Aloo Paratha",
                        "prep_time_minutes": 25,
                        "dietary_tags": ["vegetarian"],
                        "category": "paratha",
                    },
                    {
                        "recipe_name": "Masala Chai",
                        "prep_time_minutes": 10,
                        "dietary_tags": ["vegetarian"],
                        "category": "chai",
                    },
                ],
                "lunch": [
                    {
                        "recipe_name": "Dal Tadka",
                        "prep_time_minutes": 30,
                        "dietary_tags": ["vegetarian"],
                        "category": "dal",
                    },
                    {
                        "recipe_name": "Jeera Rice",
                        "prep_time_minutes": 20,
                        "dietary_tags": ["vegetarian"],
                        "category": "rice",
                    },
                ],
                "dinner": [
                    {
                        "recipe_name": "Paneer Tikka",
                        "prep_time_minutes": 30,
                        "dietary_tags": ["vegetarian"],
                        "category": "curry",
                    },
                    {
                        "recipe_name": "Butter Naan",
                        "prep_time_minutes": 15,
                        "dietary_tags": ["vegetarian"],
                        "category": "naan",
                    },
                ],
                "snacks": [
                    {
                        "recipe_name": "Samosa",
                        "prep_time_minutes": 20,
                        "dietary_tags": ["vegetarian"],
                        "category": "snack",
                    },
                    {
                        "recipe_name": "Masala Chai",
                        "prep_time_minutes": 10,
                        "dietary_tags": ["vegetarian"],
                        "category": "chai",
                    },
                ],
            }
        )

    return json.dumps({"days": days})


# ==============================================================================
# PROMPT BUILDING TESTS
# ==============================================================================


class TestPromptBuilding:
    """Tests for prompt building functionality."""

    def test_build_prompt_includes_dietary_tags(
        self, service, sample_preferences, sample_festivals
    ):
        """Prompt should include dietary tags."""
        week_start = date.today()
        config = MagicMock()

        prompt = service._build_prompt(
            sample_preferences, sample_festivals, config, week_start
        )

        assert "vegetarian" in prompt.lower()
        assert "eggetarian" in prompt.lower()

    def test_build_prompt_includes_allergies(
        self, service, sample_preferences, sample_festivals
    ):
        """Prompt should include allergies with NEVER INCLUDE emphasis."""
        week_start = date.today()
        config = MagicMock()

        prompt = service._build_prompt(
            sample_preferences, sample_festivals, config, week_start
        )

        assert "peanuts" in prompt.lower()
        assert "SEVERE" in prompt or "severe" in prompt.lower()
        assert "NEVER INCLUDE" in prompt or "never include" in prompt.lower()

    def test_build_prompt_includes_include_rules(
        self, service, sample_preferences, sample_festivals
    ):
        """Prompt should format INCLUDE rules correctly."""
        week_start = date.today()
        config = MagicMock()

        prompt = service._build_prompt(
            sample_preferences, sample_festivals, config, week_start
        )

        assert "Chai" in prompt
        assert "DAILY" in prompt
        assert "Dal" in prompt
        assert "4x/week" in prompt

    def test_build_prompt_includes_exclude_rules(
        self, service, sample_preferences, sample_festivals
    ):
        """Prompt should format EXCLUDE rules correctly."""
        week_start = date.today()
        config = MagicMock()

        prompt = service._build_prompt(
            sample_preferences, sample_festivals, config, week_start
        )

        assert "Mushroom" in prompt
        assert "NEVER" in prompt
        assert "Onion" in prompt
        assert "TUESDAY" in prompt

    def test_build_prompt_includes_festivals(
        self, service, sample_preferences, sample_festivals
    ):
        """Prompt should include festival information."""
        week_start = date.today()
        config = MagicMock()

        prompt = service._build_prompt(
            sample_preferences, sample_festivals, config, week_start
        )

        assert "Ekadashi" in prompt
        assert "fasting" in prompt.lower()
        assert "Sabudana" in prompt

    def test_build_prompt_includes_cooking_times(
        self, service, sample_preferences, sample_festivals
    ):
        """Prompt should include cooking time limits."""
        week_start = date.today()
        config = MagicMock()

        prompt = service._build_prompt(
            sample_preferences, sample_festivals, config, week_start
        )

        assert "30" in prompt  # weekday time
        assert "60" in prompt  # weekend time
        assert "MONDAY" in prompt or "Monday" in prompt  # busy day

    def test_build_prompt_includes_cuisine_preferences(
        self, service, sample_preferences, sample_festivals
    ):
        """Prompt should include cuisine preferences."""
        week_start = date.today()
        config = MagicMock()

        prompt = service._build_prompt(
            sample_preferences, sample_festivals, config, week_start
        )

        assert "north" in prompt.lower()
        assert "west" in prompt.lower()


# ==============================================================================
# RESPONSE PARSING TESTS
# ==============================================================================


class TestResponseParsing:
    """Tests for response parsing functionality."""

    def test_parse_valid_response(self, service, valid_ai_response, sample_preferences):
        """Should parse valid AI response correctly."""
        week_start = date.today() - timedelta(days=date.today().weekday())
        festivals = {}

        plan = service._parse_response(
            valid_ai_response, week_start, sample_preferences, festivals
        )

        assert isinstance(plan, GeneratedMealPlan)
        assert len(plan.days) == 7
        assert plan.week_start_date == week_start.isoformat()

    def test_parse_response_creates_meal_items(
        self, service, valid_ai_response, sample_preferences
    ):
        """Should create MealItem objects with correct fields."""
        week_start = date.today() - timedelta(days=date.today().weekday())
        festivals = {}

        plan = service._parse_response(
            valid_ai_response, week_start, sample_preferences, festivals
        )

        first_day = plan.days[0]
        assert len(first_day.breakfast) == 2
        assert first_day.breakfast[0].recipe_name == "Aloo Paratha"
        assert first_day.breakfast[0].prep_time_minutes == 25
        assert "vegetarian" in first_day.breakfast[0].dietary_tags

    def test_parse_response_assigns_ids(
        self, service, valid_ai_response, sample_preferences
    ):
        """Should assign unique IDs to each meal item."""
        week_start = date.today() - timedelta(days=date.today().weekday())
        festivals = {}

        plan = service._parse_response(
            valid_ai_response, week_start, sample_preferences, festivals
        )

        ids = set()
        for day in plan.days:
            for slot in ["breakfast", "lunch", "dinner", "snacks"]:
                for item in getattr(day, slot):
                    assert item.id not in ids
                    ids.add(item.id)


# ==============================================================================
# VALIDATION TESTS
# ==============================================================================


class TestValidation:
    """Tests for response validation."""

    def test_validate_valid_response(self, service, valid_ai_response):
        """Should not raise for valid response."""
        # Should not raise
        service._validate_response_structure(valid_ai_response)

    def test_validate_missing_days(self, service):
        """Should raise for response missing days field."""
        invalid = json.dumps({"meals": []})

        with pytest.raises(ValueError, match="missing 'days'"):
            service._validate_response_structure(invalid)

    def test_validate_wrong_day_count(self, service):
        """Should raise for response with wrong number of days."""
        invalid = json.dumps(
            {"days": [{"breakfast": [], "lunch": [], "dinner": [], "snacks": []}]}
        )

        with pytest.raises(ValueError, match="Expected 7 days"):
            service._validate_response_structure(invalid)

    def test_validate_missing_meal_slot(self, service):
        """Should raise for day missing a meal slot."""
        days = [
            {
                "breakfast": [{"recipe_name": "A"}, {"recipe_name": "B"}],
                "lunch": [{"recipe_name": "A"}, {"recipe_name": "B"}],
                "dinner": [{"recipe_name": "A"}, {"recipe_name": "B"}],
            }
            for _ in range(7)
        ]  # Missing snacks
        invalid = json.dumps({"days": days})

        with pytest.raises(ValueError, match="missing 'snacks'"):
            service._validate_response_structure(invalid)

    def test_validate_insufficient_items(self, service):
        """Should raise for meal slot with less than 2 items."""
        days = [
            {
                "breakfast": [{"recipe_name": "A"}],  # Only 1 item
                "lunch": [{"recipe_name": "A"}, {"recipe_name": "B"}],
                "dinner": [{"recipe_name": "A"}, {"recipe_name": "B"}],
                "snacks": [{"recipe_name": "A"}, {"recipe_name": "B"}],
            }
            for _ in range(7)
        ]
        invalid = json.dumps({"days": days})

        with pytest.raises(ValueError, match="should have 2 items"):
            service._validate_response_structure(invalid)


# ==============================================================================
# ENFORCEMENT TESTS
# ==============================================================================


class TestEnforcement:
    """Tests for post-processing rule enforcement."""

    def test_enforce_removes_allergens(self, service, sample_preferences):
        """Should remove items containing allergens."""
        # Create plan with peanut dish
        plan = GeneratedMealPlan(
            week_start_date="2026-02-09",
            week_end_date="2026-02-15",
            days=[
                DayMeals(
                    date="2026-02-09",
                    day_name="Monday",
                    breakfast=[
                        MealItem(
                            id="1", recipe_name="Peanut Chutney"
                        ),  # Should be removed
                        MealItem(id="2", recipe_name="Masala Dosa"),
                    ],
                    lunch=[
                        MealItem(id="3", recipe_name="Dal Rice"),
                        MealItem(id="4", recipe_name="Roti"),
                    ],
                    dinner=[
                        MealItem(id="5", recipe_name="Paneer Curry"),
                        MealItem(id="6", recipe_name="Naan"),
                    ],
                    snacks=[
                        MealItem(id="7", recipe_name="Samosa"),
                        MealItem(id="8", recipe_name="Chai"),
                    ],
                )
            ],
            rules_applied={},
        )

        result = service._enforce_rules(plan, sample_preferences)

        # Peanut Chutney should be removed
        breakfast_names = [item.recipe_name for item in result.days[0].breakfast]
        assert "Peanut Chutney" not in breakfast_names

    def test_enforce_removes_excluded_items_on_specific_days(
        self, service, sample_preferences
    ):
        """Should remove excluded items on specific days."""
        # Create plan with onion dish on Tuesday
        plan = GeneratedMealPlan(
            week_start_date="2026-02-09",
            week_end_date="2026-02-15",
            days=[
                DayMeals(
                    date="2026-02-10",
                    day_name="Tuesday",  # Onion excluded on Tuesday
                    breakfast=[
                        MealItem(
                            id="1", recipe_name="Onion Paratha"
                        ),  # Should be removed
                        MealItem(id="2", recipe_name="Chai"),
                    ],
                    lunch=[
                        MealItem(id="3", recipe_name="Dal"),
                        MealItem(id="4", recipe_name="Rice"),
                    ],
                    dinner=[
                        MealItem(id="5", recipe_name="Paneer"),
                        MealItem(id="6", recipe_name="Roti"),
                    ],
                    snacks=[
                        MealItem(id="7", recipe_name="Samosa"),
                        MealItem(id="8", recipe_name="Chai"),
                    ],
                )
            ],
            rules_applied={},
        )

        result = service._enforce_rules(plan, sample_preferences)

        # Onion Paratha should be removed on Tuesday
        breakfast_names = [item.recipe_name for item in result.days[0].breakfast]
        assert "Onion Paratha" not in breakfast_names

    def test_enforce_keeps_excluded_items_on_other_days(
        self, service, sample_preferences
    ):
        """Should keep excluded items on days they're allowed."""
        # Create plan with onion dish on Wednesday (onion only excluded on Tuesday)
        plan = GeneratedMealPlan(
            week_start_date="2026-02-09",
            week_end_date="2026-02-15",
            days=[
                DayMeals(
                    date="2026-02-11",
                    day_name="Wednesday",  # Onion allowed
                    breakfast=[
                        MealItem(id="1", recipe_name="Onion Paratha"),  # Should be kept
                        MealItem(id="2", recipe_name="Chai"),
                    ],
                    lunch=[
                        MealItem(id="3", recipe_name="Dal"),
                        MealItem(id="4", recipe_name="Rice"),
                    ],
                    dinner=[
                        MealItem(id="5", recipe_name="Paneer"),
                        MealItem(id="6", recipe_name="Roti"),
                    ],
                    snacks=[
                        MealItem(id="7", recipe_name="Samosa"),
                        MealItem(id="8", recipe_name="Chai"),
                    ],
                )
            ],
            rules_applied={},
        )

        result = service._enforce_rules(plan, sample_preferences)

        # Onion Paratha should be kept on Wednesday
        breakfast_names = [item.recipe_name for item in result.days[0].breakfast]
        assert "Onion Paratha" in breakfast_names

    def test_enforce_removes_never_excluded_items(self, service, sample_preferences):
        """Should remove NEVER excluded items on any day."""
        # Create plan with mushroom dish
        plan = GeneratedMealPlan(
            week_start_date="2026-02-09",
            week_end_date="2026-02-15",
            days=[
                DayMeals(
                    date="2026-02-09",
                    day_name="Monday",
                    breakfast=[
                        MealItem(id="1", recipe_name="Paratha"),
                        MealItem(id="2", recipe_name="Chai"),
                    ],
                    lunch=[
                        MealItem(
                            id="3", recipe_name="Mushroom Masala"
                        ),  # Should be removed
                        MealItem(id="4", recipe_name="Rice"),
                    ],
                    dinner=[
                        MealItem(id="5", recipe_name="Paneer"),
                        MealItem(id="6", recipe_name="Roti"),
                    ],
                    snacks=[
                        MealItem(id="7", recipe_name="Samosa"),
                        MealItem(id="8", recipe_name="Chai"),
                    ],
                )
            ],
            rules_applied={},
        )

        result = service._enforce_rules(plan, sample_preferences)

        # Mushroom Masala should be removed
        lunch_names = [item.recipe_name for item in result.days[0].lunch]
        assert "Mushroom Masala" not in lunch_names


# ==============================================================================
# INTEGRATION TESTS (WITH MOCKS)
# ==============================================================================


class TestIntegration:
    """Integration tests with mocked dependencies."""

    @pytest.mark.asyncio
    async def test_generate_meal_plan_success(
        self, service, valid_ai_response, sample_preferences
    ):
        """Should generate meal plan successfully with mocked dependencies."""
        week_start = date.today() - timedelta(days=date.today().weekday())

        with patch.object(
            service, "_load_user_preferences", new_callable=AsyncMock
        ) as mock_prefs, patch.object(
            service, "_load_festivals", new_callable=AsyncMock
        ) as mock_festivals, patch.object(
            service.config_service, "get_config", new_callable=AsyncMock
        ) as mock_config, patch(
            "app.services.ai_meal_service.generate_text", new_callable=AsyncMock
        ) as mock_generate:

            mock_prefs.return_value = sample_preferences
            mock_festivals.return_value = {}
            mock_config.return_value = MagicMock()
            mock_generate.return_value = valid_ai_response

            result = await service.generate_meal_plan("test-user", week_start)

            assert isinstance(result, GeneratedMealPlan)
            assert len(result.days) == 7
            mock_generate.assert_called_once()

    @pytest.mark.asyncio
    async def test_generate_meal_plan_retries_on_failure(
        self, service, sample_preferences
    ):
        """Should retry on AI generation failure."""
        week_start = date.today() - timedelta(days=date.today().weekday())

        # Create valid response for final attempt
        days = []
        for i in range(7):
            current = week_start + timedelta(days=i)
            days.append(
                {
                    "date": current.isoformat(),
                    "day_name": current.strftime("%A"),
                    "breakfast": [
                        {
                            "recipe_name": "A",
                            "prep_time_minutes": 10,
                            "dietary_tags": [],
                            "category": "x",
                        },
                        {
                            "recipe_name": "B",
                            "prep_time_minutes": 10,
                            "dietary_tags": [],
                            "category": "x",
                        },
                    ],
                    "lunch": [
                        {
                            "recipe_name": "C",
                            "prep_time_minutes": 10,
                            "dietary_tags": [],
                            "category": "x",
                        },
                        {
                            "recipe_name": "D",
                            "prep_time_minutes": 10,
                            "dietary_tags": [],
                            "category": "x",
                        },
                    ],
                    "dinner": [
                        {
                            "recipe_name": "E",
                            "prep_time_minutes": 10,
                            "dietary_tags": [],
                            "category": "x",
                        },
                        {
                            "recipe_name": "F",
                            "prep_time_minutes": 10,
                            "dietary_tags": [],
                            "category": "x",
                        },
                    ],
                    "snacks": [
                        {
                            "recipe_name": "G",
                            "prep_time_minutes": 10,
                            "dietary_tags": [],
                            "category": "x",
                        },
                        {
                            "recipe_name": "H",
                            "prep_time_minutes": 10,
                            "dietary_tags": [],
                            "category": "x",
                        },
                    ],
                }
            )
        valid_response = json.dumps({"days": days})

        with patch.object(
            service, "_load_user_preferences", new_callable=AsyncMock
        ) as mock_prefs, patch.object(
            service, "_load_festivals", new_callable=AsyncMock
        ) as mock_festivals, patch.object(
            service.config_service, "get_config", new_callable=AsyncMock
        ) as mock_config, patch(
            "app.services.ai_meal_service.generate_text", new_callable=AsyncMock
        ) as mock_generate, patch(
            "asyncio.sleep", new_callable=AsyncMock
        ):  # Skip sleep in tests

            mock_prefs.return_value = sample_preferences
            mock_festivals.return_value = {}
            mock_config.return_value = MagicMock()

            # Fail twice, succeed on third
            mock_generate.side_effect = [
                Exception("API Error 1"),
                Exception("API Error 2"),
                valid_response,
            ]

            result = await service.generate_meal_plan("test-user", week_start)

            assert mock_generate.call_count == 3
            assert isinstance(result, GeneratedMealPlan)

    @pytest.mark.asyncio
    async def test_generate_meal_plan_fails_after_max_retries(
        self, service, sample_preferences
    ):
        """Should raise ServiceUnavailableError after max retries."""
        week_start = date.today() - timedelta(days=date.today().weekday())

        with patch.object(
            service, "_load_user_preferences", new_callable=AsyncMock
        ) as mock_prefs, patch.object(
            service, "_load_festivals", new_callable=AsyncMock
        ) as mock_festivals, patch.object(
            service.config_service, "get_config", new_callable=AsyncMock
        ) as mock_config, patch(
            "app.services.ai_meal_service.generate_text", new_callable=AsyncMock
        ) as mock_generate, patch(
            "asyncio.sleep", new_callable=AsyncMock
        ):  # Skip sleep in tests

            mock_prefs.return_value = sample_preferences
            mock_festivals.return_value = {}
            mock_config.return_value = MagicMock()

            # Always fail
            mock_generate.side_effect = Exception("API Error")

            from app.core.exceptions import ServiceUnavailableError

            with pytest.raises(
                ServiceUnavailableError, match="failed after 3 attempts"
            ):
                await service.generate_meal_plan("test-user", week_start)

            assert mock_generate.call_count == 3


# ==============================================================================
# SCHEMA TESTS
# ==============================================================================


def test_enforce_rules_does_not_remove_unsweetened_items():
    """Regression test: 'unsweetened' should NOT match 'sweet' keyword."""
    from app.services.ai_meal_service import (
        AIMealService,
        MealItem,
        DayMeals,
        GeneratedMealPlan,
        UserPreferences,
    )

    service = AIMealService()
    plan = GeneratedMealPlan(
        week_start_date="2026-03-02",
        week_end_date="2026-03-08",
        days=[
            DayMeals(
                date="2026-03-02",
                day_name="Monday",
                breakfast=[
                    MealItem(id="1", recipe_name="Masala Chai"),
                    MealItem(id="2", recipe_name="Aloo Paratha"),
                ],
                lunch=[
                    MealItem(id="3", recipe_name="Dal Tadka"),
                    MealItem(id="4", recipe_name="Jeera Rice"),
                ],
                dinner=[
                    MealItem(id="5", recipe_name="Paneer Curry"),
                    MealItem(id="6", recipe_name="Roti"),
                ],
                snacks=[
                    MealItem(id="7", recipe_name="Herbal Tea (unsweetened)"),
                    MealItem(id="8", recipe_name="Roasted Makhana"),
                ],
            ),
        ]
        + [
            DayMeals(
                date=f"2026-03-0{i}",
                day_name=[
                    "Tuesday",
                    "Wednesday",
                    "Thursday",
                    "Friday",
                    "Saturday",
                    "Sunday",
                ][i - 3],
                breakfast=[MealItem(id=f"b{i}", recipe_name="Poha")],
                lunch=[MealItem(id=f"l{i}", recipe_name="Rice")],
                dinner=[MealItem(id=f"d{i}", recipe_name="Roti")],
                snacks=[MealItem(id=f"s{i}", recipe_name="Chai")],
            )
            for i in range(3, 9)
        ],
    )

    prefs = UserPreferences(
        family_members=[
            {
                "name": "Ramesh",
                "health_conditions": ["DIABETIC"],
                "dietary_restrictions": [],
            },
        ],
    )

    result = service._enforce_rules(plan, prefs)

    # "Herbal Tea (unsweetened)" should NOT be removed
    snack_names = [item.recipe_name for item in result.days[0].snacks]
    assert (
        "Herbal Tea (unsweetened)" in snack_names
    ), f"'unsweetened' was incorrectly removed. Remaining snacks: {snack_names}"


def test_enforce_rules_still_removes_actual_sweets():
    """Verify that actual sweet items ARE still removed for diabetic members."""
    from app.services.ai_meal_service import (
        AIMealService,
        MealItem,
        DayMeals,
        GeneratedMealPlan,
        UserPreferences,
    )

    service = AIMealService()
    plan = GeneratedMealPlan(
        week_start_date="2026-03-02",
        week_end_date="2026-03-08",
        days=[
            DayMeals(
                date="2026-03-02",
                day_name="Monday",
                breakfast=[MealItem(id="1", recipe_name="Chai")],
                lunch=[MealItem(id="2", recipe_name="Rice")],
                dinner=[MealItem(id="3", recipe_name="Roti")],
                snacks=[
                    MealItem(id="4", recipe_name="Gulab Jamun"),
                    MealItem(id="5", recipe_name="Sweet Lassi"),
                ],
            ),
        ]
        + [
            DayMeals(
                date=f"2026-03-0{i}",
                day_name="Day",
                breakfast=[MealItem(id=f"b{i}", recipe_name="Poha")],
                lunch=[MealItem(id=f"l{i}", recipe_name="Rice")],
                dinner=[MealItem(id=f"d{i}", recipe_name="Roti")],
                snacks=[MealItem(id=f"s{i}", recipe_name="Chai")],
            )
            for i in range(3, 9)
        ],
    )

    prefs = UserPreferences(
        family_members=[
            {
                "name": "Ramesh",
                "health_conditions": ["DIABETIC"],
                "dietary_restrictions": [],
            },
        ],
    )

    result = service._enforce_rules(plan, prefs)

    snack_names = [item.recipe_name for item in result.days[0].snacks]
    assert (
        "Gulab Jamun" not in snack_names
    ), "Gulab Jamun should be removed (contains 'gulab jamun')"
    assert (
        "Sweet Lassi" not in snack_names
    ), "Sweet Lassi should be removed (contains 'sweet')"


def test_meal_plan_schema_is_disabled():
    """Gemini 2.5 Flash rejects response_schema for meal plans ('too many states').
    Schema is set to None; structure is enforced by prompt + post-validation."""
    from app.services.ai_meal_service import MEAL_PLAN_SCHEMA

    assert MEAL_PLAN_SCHEMA is None
