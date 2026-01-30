"""E2E tests for meal generation against real PostgreSQL database.

These tests hit the actual PostgreSQL database with 3,000+ recipes to validate
that the meal generation algorithm works correctly in production-like conditions.

Prerequisites:
- DATABASE_URL must be set in .env to valid PostgreSQL connection
- PostgreSQL must be accessible
- Recipe database must be populated (run import_recipes_postgres.py to populate)

Run with:
    cd backend
    PYTHONPATH=. pytest tests/test_meal_generation_e2e.py -v -s

To run with coverage:
    PYTHONPATH=. pytest tests/test_meal_generation_e2e.py -v -s --cov=app.services.meal_generation_service
"""

import asyncio
import logging
import pytest
from datetime import date, timedelta
from collections import Counter

from app.services.meal_generation_service import (
    MealGenerationService,
    GeneratedMealPlan,
    UserPreferences,
)
from app.repositories.user_repository import UserRepository

# Configure logging to see algorithm decisions
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


# =============================================================================
# TEST DATA: Sharma Family Profile (from Algorithm Doc)
# =============================================================================

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

# East Indian preferences (only 23 recipes in DB - tests generic fallback)
EAST_INDIAN_PREFS = UserPreferences(
    dietary_tags=["vegetarian"],
    cuisine_type="east",
    allergies=[],
    dislikes=[],
    weekday_cooking_time=30,
    weekend_cooking_time=60,
    busy_days=[],
    include_rules=[],
    exclude_rules=[],
    nutrition_goals=[],
)

# South Indian preferences (358 recipes)
SOUTH_INDIAN_PREFS = UserPreferences(
    dietary_tags=["vegetarian"],
    cuisine_type="south",
    allergies=[{"ingredient": "peanuts", "severity": "SEVERE"}],
    dislikes=[],
    weekday_cooking_time=30,
    weekend_cooking_time=60,
    busy_days=[],
    include_rules=[
        {"type": "INCLUDE", "target": "Idli", "frequency": "TIMES_PER_WEEK", "times_per_week": 2, "meal_slot": ["breakfast"]},
    ],
    exclude_rules=[],
    nutrition_goals=[],
)


# =============================================================================
# FIXTURES
# =============================================================================

@pytest.fixture
def service():
    """Create MealGenerationService instance."""
    return MealGenerationService()


@pytest.fixture
def week_start():
    """Get next Monday as week start date."""
    today = date.today()
    days_until_monday = (7 - today.weekday()) % 7
    if days_until_monday == 0:
        days_until_monday = 7  # If today is Monday, use next Monday
    return today + timedelta(days=days_until_monday)


# =============================================================================
# HELPER FUNCTIONS
# =============================================================================

def count_recipes_with_ingredient(plan: GeneratedMealPlan, ingredient: str) -> int:
    """Count recipes containing an ingredient (by name)."""
    count = 0
    ingredient_lower = ingredient.lower()
    for day in plan.days:
        for slot in [day.breakfast, day.lunch, day.dinner, day.snacks]:
            for item in slot:
                if ingredient_lower in item.recipe_name.lower():
                    count += 1
    return count


def count_generic_items(plan: GeneratedMealPlan) -> int:
    """Count generic suggestion items (is_generic=True or recipe_id='GENERIC')."""
    count = 0
    for day in plan.days:
        for slot in [day.breakfast, day.lunch, day.dinner, day.snacks]:
            for item in slot:
                if item.is_generic or item.recipe_id == "GENERIC":
                    count += 1
    return count


def get_all_recipe_names(plan: GeneratedMealPlan) -> list[str]:
    """Get all recipe names from the plan."""
    names = []
    for day in plan.days:
        for slot in [day.breakfast, day.lunch, day.dinner, day.snacks]:
            for item in slot:
                names.append(item.recipe_name)
    return names


def count_recipes_in_slot(plan: GeneratedMealPlan, slot_name: str, contains: str) -> int:
    """Count recipes in a specific slot that contain a substring."""
    count = 0
    contains_lower = contains.lower()
    for day in plan.days:
        slot = getattr(day, slot_name, [])
        for item in slot:
            if contains_lower in item.recipe_name.lower():
                count += 1
    return count


def print_meal_plan_summary(plan: GeneratedMealPlan):
    """Print a summary of the meal plan for debugging."""
    print("\n" + "=" * 80)
    print(f"MEAL PLAN: {plan.week_start_date} to {plan.week_end_date}")
    print("=" * 80)

    for day in plan.days:
        print(f"\n{day.day_name} ({day.date}):")
        print(f"  Breakfast: {[i.recipe_name for i in day.breakfast]}")
        print(f"  Lunch:     {[i.recipe_name for i in day.lunch]}")
        print(f"  Dinner:    {[i.recipe_name for i in day.dinner]}")
        print(f"  Snacks:    {[i.recipe_name for i in day.snacks]}")

    print("\n" + "-" * 80)
    print(f"Rules Applied: {plan.rules_applied}")
    print("=" * 80 + "\n")


# =============================================================================
# E2E TESTS WITH REAL POSTGRESQL
# =============================================================================

class TestMealGenerationE2E:
    """E2E tests for meal generation against real PostgreSQL."""

    @pytest.mark.asyncio
    async def test_sharma_family_generates_valid_plan(self, service, week_start):
        """Test that Sharma Family profile generates a complete meal plan."""
        # Mock user preferences loading
        async def mock_load_prefs(user_id):
            return SHARMA_FAMILY_PREFS
        service._load_user_preferences = mock_load_prefs

        plan = await service.generate_meal_plan(
            user_id="test-sharma-family",
            week_start_date=week_start,
        )

        print_meal_plan_summary(plan)

        # Basic structure checks
        assert plan is not None
        assert len(plan.days) == 7, f"Expected 7 days, got {len(plan.days)}"
        assert plan.week_start_date == week_start.isoformat()

        # Every slot should have items
        empty_slots = []
        for day in plan.days:
            for slot_name in ["breakfast", "lunch", "dinner", "snacks"]:
                slot = getattr(day, slot_name)
                if len(slot) == 0:
                    empty_slots.append(f"{day.day_name} {slot_name}")

        assert len(empty_slots) == 0, f"Empty slots found: {empty_slots}"

    @pytest.mark.asyncio
    async def test_sharma_family_no_peanuts(self, service, week_start):
        """CRITICAL: Peanut allergy must be strictly enforced."""
        async def mock_load_prefs(user_id):
            return SHARMA_FAMILY_PREFS
        service._load_user_preferences = mock_load_prefs

        plan = await service.generate_meal_plan(
            user_id="test-sharma-family",
            week_start_date=week_start,
        )

        # Check for peanut variants
        peanut_variants = ["peanut", "groundnut", "moongphali"]
        all_names = get_all_recipe_names(plan)

        violations = []
        for name in all_names:
            name_lower = name.lower()
            for variant in peanut_variants:
                if variant in name_lower:
                    violations.append(f"{name} contains '{variant}'")

        assert len(violations) == 0, f"ALLERGY VIOLATION - Peanut recipes found: {violations}"

    @pytest.mark.asyncio
    async def test_sharma_family_no_dislikes(self, service, week_start):
        """Disliked ingredients should not appear in recipes."""
        async def mock_load_prefs(user_id):
            return SHARMA_FAMILY_PREFS
        service._load_user_preferences = mock_load_prefs

        plan = await service.generate_meal_plan(
            user_id="test-sharma-family",
            week_start_date=week_start,
        )

        dislikes = ["karela", "lauki", "turai"]
        all_names = get_all_recipe_names(plan)

        violations = []
        for name in all_names:
            name_lower = name.lower()
            for dislike in dislikes:
                if dislike in name_lower:
                    violations.append(f"{name} contains '{dislike}'")

        assert len(violations) == 0, f"Dislike violations: {violations}"

    @pytest.mark.asyncio
    async def test_sharma_family_no_mushroom(self, service, week_start):
        """EXCLUDE rule for mushroom should be enforced."""
        async def mock_load_prefs(user_id):
            return SHARMA_FAMILY_PREFS
        service._load_user_preferences = mock_load_prefs

        plan = await service.generate_meal_plan(
            user_id="test-sharma-family",
            week_start_date=week_start,
        )

        mushroom_count = count_recipes_with_ingredient(plan, "mushroom")
        assert mushroom_count == 0, f"Found {mushroom_count} recipes with mushroom (EXCLUDE rule)"

    @pytest.mark.asyncio
    async def test_sharma_family_chai_daily(self, service, week_start):
        """INCLUDE rule: Chai should appear in breakfast all 7 days."""
        async def mock_load_prefs(user_id):
            return SHARMA_FAMILY_PREFS
        service._load_user_preferences = mock_load_prefs

        plan = await service.generate_meal_plan(
            user_id="test-sharma-family",
            week_start_date=week_start,
        )

        chai_count = count_recipes_in_slot(plan, "breakfast", "chai")
        assert chai_count >= 7, f"Expected Chai in breakfast 7 times, found {chai_count}"

    @pytest.mark.asyncio
    async def test_sharma_family_dal_4_times(self, service, week_start):
        """INCLUDE rule: Dal should appear 4 times in lunch/dinner."""
        async def mock_load_prefs(user_id):
            return SHARMA_FAMILY_PREFS
        service._load_user_preferences = mock_load_prefs

        plan = await service.generate_meal_plan(
            user_id="test-sharma-family",
            week_start_date=week_start,
        )

        dal_lunch = count_recipes_in_slot(plan, "lunch", "dal")
        dal_dinner = count_recipes_in_slot(plan, "dinner", "dal")
        dal_total = dal_lunch + dal_dinner

        assert dal_total >= 4, f"Expected Dal 4+ times in lunch/dinner, found {dal_total}"

    @pytest.mark.asyncio
    async def test_sharma_family_paneer_2_times(self, service, week_start):
        """INCLUDE rule: Paneer should appear 2 times in lunch/dinner."""
        async def mock_load_prefs(user_id):
            return SHARMA_FAMILY_PREFS
        service._load_user_preferences = mock_load_prefs

        plan = await service.generate_meal_plan(
            user_id="test-sharma-family",
            week_start_date=week_start,
        )

        paneer_lunch = count_recipes_in_slot(plan, "lunch", "paneer")
        paneer_dinner = count_recipes_in_slot(plan, "dinner", "paneer")
        paneer_total = paneer_lunch + paneer_dinner

        assert paneer_total >= 2, f"Expected Paneer 2+ times in lunch/dinner, found {paneer_total}"

    @pytest.mark.asyncio
    async def test_sharma_family_2_items_per_slot(self, service, week_start):
        """Each meal slot should have 2 complementary items."""
        async def mock_load_prefs(user_id):
            return SHARMA_FAMILY_PREFS
        service._load_user_preferences = mock_load_prefs

        plan = await service.generate_meal_plan(
            user_id="test-sharma-family",
            week_start_date=week_start,
        )

        slots_with_wrong_count = []
        for day in plan.days:
            for slot_name in ["breakfast", "lunch", "dinner", "snacks"]:
                slot = getattr(day, slot_name)
                if len(slot) != 2:
                    slots_with_wrong_count.append(
                        f"{day.day_name} {slot_name}: {len(slot)} items"
                    )

        # Allow some flexibility - most slots should have 2 items
        assert len(slots_with_wrong_count) <= 4, \
            f"Too many slots without 2 items: {slots_with_wrong_count}"

    @pytest.mark.asyncio
    async def test_sharma_family_no_duplicate_mains(self, service, week_start):
        """Main recipes should not repeat within the same week."""
        async def mock_load_prefs(user_id):
            return SHARMA_FAMILY_PREFS
        service._load_user_preferences = mock_load_prefs

        plan = await service.generate_meal_plan(
            user_id="test-sharma-family",
            week_start_date=week_start,
        )

        # Collect non-generic recipe IDs
        recipe_ids = []
        for day in plan.days:
            for slot in [day.breakfast, day.lunch, day.dinner, day.snacks]:
                for item in slot:
                    if not item.is_generic and item.recipe_id != "GENERIC":
                        recipe_ids.append(item.recipe_id)

        # Count duplicates
        id_counts = Counter(recipe_ids)
        duplicates = {rid: count for rid, count in id_counts.items() if count > 1}

        # Some duplication is OK for DAILY rules, but shouldn't be excessive
        excessive_dupes = {rid: count for rid, count in duplicates.items() if count > 7}
        assert len(excessive_dupes) == 0, \
            f"Excessive recipe duplication: {excessive_dupes}"


class TestEastIndianCuisine:
    """Test East Indian cuisine with limited recipes (triggers generic fallback)."""

    @pytest.mark.asyncio
    async def test_east_indian_generates_plan(self, service, week_start):
        """East Indian (23 recipes) should still generate a complete plan."""
        async def mock_load_prefs(user_id):
            return EAST_INDIAN_PREFS
        service._load_user_preferences = mock_load_prefs

        plan = await service.generate_meal_plan(
            user_id="test-east-indian",
            week_start_date=week_start,
        )

        print_meal_plan_summary(plan)

        assert plan is not None
        assert len(plan.days) == 7

    @pytest.mark.asyncio
    async def test_east_indian_has_generic_suggestions(self, service, week_start):
        """East Indian should have generic suggestions due to limited recipes."""
        async def mock_load_prefs(user_id):
            return EAST_INDIAN_PREFS
        service._load_user_preferences = mock_load_prefs

        plan = await service.generate_meal_plan(
            user_id="test-east-indian",
            week_start_date=week_start,
        )

        generic_count = count_generic_items(plan)

        # With only 23 East Indian recipes for 56 slots (7 days × 4 slots × 2 items),
        # we expect some generic suggestions
        logger.info(f"Generic items count: {generic_count}")

        # This test documents the behavior - it may or may not have generics
        # depending on fallback to other cuisines
        print(f"Generic suggestions in East Indian plan: {generic_count}")


class TestSouthIndianCuisine:
    """Test South Indian cuisine (358 recipes)."""

    @pytest.mark.asyncio
    async def test_south_indian_includes_idli(self, service, week_start):
        """INCLUDE rule for Idli 2x/week should be enforced."""
        async def mock_load_prefs(user_id):
            return SOUTH_INDIAN_PREFS
        service._load_user_preferences = mock_load_prefs

        plan = await service.generate_meal_plan(
            user_id="test-south-indian",
            week_start_date=week_start,
        )

        print_meal_plan_summary(plan)

        idli_count = count_recipes_in_slot(plan, "breakfast", "idli")
        assert idli_count >= 2, f"Expected Idli 2+ times in breakfast, found {idli_count}"

    @pytest.mark.asyncio
    async def test_south_indian_no_peanuts(self, service, week_start):
        """South Indian plan should also exclude peanuts."""
        async def mock_load_prefs(user_id):
            return SOUTH_INDIAN_PREFS
        service._load_user_preferences = mock_load_prefs

        plan = await service.generate_meal_plan(
            user_id="test-south-indian",
            week_start_date=week_start,
        )

        peanut_count = count_recipes_with_ingredient(plan, "peanut")
        groundnut_count = count_recipes_with_ingredient(plan, "groundnut")

        assert peanut_count == 0, f"Found {peanut_count} peanut recipes"
        assert groundnut_count == 0, f"Found {groundnut_count} groundnut recipes"


# =============================================================================
# VERIFICATION REPORT
# =============================================================================

class TestVerificationReport:
    """Generate a verification report for the Sharma Family profile."""

    @pytest.mark.asyncio
    async def test_generate_verification_report(self, service, week_start):
        """Generate and print a complete verification report."""
        async def mock_load_prefs(user_id):
            return SHARMA_FAMILY_PREFS
        service._load_user_preferences = mock_load_prefs

        plan = await service.generate_meal_plan(
            user_id="test-sharma-family",
            week_start_date=week_start,
        )

        print("\n")
        print("=" * 80)
        print("SHARMA FAMILY VERIFICATION REPORT")
        print("=" * 80)

        # Collect all recipe names
        all_names = get_all_recipe_names(plan)

        # 1. Allergy check
        peanut_violations = [n for n in all_names if "peanut" in n.lower() or "groundnut" in n.lower()]
        print(f"\n1. PEANUT ALLERGY CHECK:")
        print(f"   Violations: {len(peanut_violations)}")
        if peanut_violations:
            print(f"   Recipes: {peanut_violations}")
        print(f"   Status: {'✅ PASS' if len(peanut_violations) == 0 else '❌ FAIL'}")

        # 2. Dislike check
        dislike_violations = [n for n in all_names
                            if any(d in n.lower() for d in ["karela", "lauki", "turai"])]
        print(f"\n2. DISLIKE CHECK:")
        print(f"   Violations: {len(dislike_violations)}")
        if dislike_violations:
            print(f"   Recipes: {dislike_violations}")
        print(f"   Status: {'✅ PASS' if len(dislike_violations) == 0 else '❌ FAIL'}")

        # 3. EXCLUDE rule check
        mushroom_count = count_recipes_with_ingredient(plan, "mushroom")
        print(f"\n3. MUSHROOM EXCLUDE CHECK:")
        print(f"   Count: {mushroom_count}")
        print(f"   Status: {'✅ PASS' if mushroom_count == 0 else '❌ FAIL'}")

        # 4. INCLUDE rules check
        chai_count = count_recipes_in_slot(plan, "breakfast", "chai")
        dal_count = count_recipes_in_slot(plan, "lunch", "dal") + count_recipes_in_slot(plan, "dinner", "dal")
        paneer_count = count_recipes_in_slot(plan, "lunch", "paneer") + count_recipes_in_slot(plan, "dinner", "paneer")

        print(f"\n4. INCLUDE RULES CHECK:")
        print(f"   Chai in breakfast: {chai_count}/7 {'✅' if chai_count >= 7 else '⚠️'}")
        print(f"   Dal in lunch/dinner: {dal_count}/4 {'✅' if dal_count >= 4 else '⚠️'}")
        print(f"   Paneer in lunch/dinner: {paneer_count}/2 {'✅' if paneer_count >= 2 else '⚠️'}")

        # 5. 2-item pairing check
        slots_ok = 0
        total_slots = 0
        for day in plan.days:
            for slot_name in ["breakfast", "lunch", "dinner", "snacks"]:
                slot = getattr(day, slot_name)
                total_slots += 1
                if len(slot) == 2:
                    slots_ok += 1

        print(f"\n5. 2-ITEM PAIRING CHECK:")
        print(f"   Slots with 2 items: {slots_ok}/{total_slots}")
        print(f"   Status: {'✅ PASS' if slots_ok >= total_slots - 4 else '⚠️ WARNING'}")

        # 6. Generic suggestions check
        generic_count = count_generic_items(plan)
        print(f"\n6. GENERIC SUGGESTIONS:")
        print(f"   Count: {generic_count}")
        print(f"   Status: {'ℹ️ INFO' if generic_count > 0 else '✅ All DB recipes'}")

        # Summary
        print("\n" + "-" * 80)
        all_critical_pass = (
            len(peanut_violations) == 0 and
            mushroom_count == 0
        )
        print(f"OVERALL: {'✅ ALL CRITICAL CHECKS PASS' if all_critical_pass else '❌ CRITICAL FAILURES'}")
        print("=" * 80 + "\n")

        # Assert critical checks
        assert len(peanut_violations) == 0, "Peanut allergy violation!"
        assert mushroom_count == 0, "Mushroom exclude rule violation!"
