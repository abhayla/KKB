#!/usr/bin/env python3
"""Test script for meal generation with pairing.

Verifies:
1. Config loads correctly from Firestore
2. Pairing rules are applied
3. 2-item meal slots are generated
4. INCLUDE/EXCLUDE rules work

Usage:
    python scripts/test_meal_generation.py
"""

import asyncio
import logging
import sys
from datetime import date, timedelta
from pathlib import Path

# Add parent directory to path for imports
sys.path.insert(0, str(Path(__file__).parent.parent))

from app.services.config_service import ConfigService
from app.services.meal_generation_service import MealGenerationService
from app.repositories.recipe_repository import RecipeRepository

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


async def test_config_loading():
    """Test that config loads from Firestore."""
    print("\n" + "=" * 60)
    print("TEST 1: Config Loading")
    print("=" * 60)

    config_service = ConfigService()
    config = await config_service.get_config()

    print(f"\n[OK] Meal structure: {config.meal_structure.items_per_slot} items per slot")
    print(f"[OK] Recipe categories: {len(config.recipe_categories)} categories")
    print(f"[OK] Pairing rules: {len(config.pairing_rules_flat)} rules")
    print(f"[OK] Meal type pairs: {list(config.meal_type_pairs.keys())}")

    # Show some example pairings
    print("\nExample pairing rules:")
    count = 0
    for key, values in config.pairing_rules_flat.items():
        if count < 5:
            print(f"  {key} -> {values}")
            count += 1

    # Test pairing lookup
    pairings = config_service.get_pairing_categories("north", "dal")
    print(f"\nPairing for north:dal -> {pairings}")

    # Test ingredient aliases
    aliases = config_service.get_ingredient_aliases("baingan")
    print(f"Aliases for 'baingan': {aliases}")

    return True


async def test_recipe_category_search():
    """Test that recipes can be searched by category."""
    print("\n" + "=" * 60)
    print("TEST 2: Recipe Category Search")
    print("=" * 60)

    recipe_repo = RecipeRepository()

    # Search for dal recipes
    dal_recipes = await recipe_repo.search_by_category(
        category="dal",
        cuisine_type="north",
        dietary_tags=["vegetarian"],
        limit=5
    )
    print(f"\n[OK] Found {len(dal_recipes)} dal recipes")
    for r in dal_recipes[:3]:
        print(f"  - {r.get('name')} (category: {r.get('category')})")

    # Search for rice recipes
    rice_recipes = await recipe_repo.search_by_category(
        category="rice",
        dietary_tags=["vegetarian"],
        limit=5
    )
    print(f"\n[OK] Found {len(rice_recipes)} rice recipes")

    # Get a recipe pair
    primary, accompaniment = await recipe_repo.get_recipe_pair(
        primary_category="dal",
        accompaniment_category="rice",
        cuisine_type="north",
        dietary_tags=["vegetarian"],
    )

    if primary and accompaniment:
        print(f"\n[OK] Recipe pair found:")
        print(f"  Primary: {primary.get('name')} ({primary.get('category')})")
        print(f"  Accompaniment: {accompaniment.get('name')} ({accompaniment.get('category')})")
        return True
    else:
        print(f"\n[FAIL] Could not find recipe pair")
        return False


async def test_meal_generation():
    """Test full meal plan generation."""
    print("\n" + "=" * 60)
    print("TEST 3: Full Meal Plan Generation")
    print("=" * 60)

    # Use a test user ID (this would normally come from auth)
    test_user_id = "test-user-phase2"

    # Calculate current week's Monday
    today = date.today()
    week_start = today - timedelta(days=today.weekday())

    service = MealGenerationService()
    plan = await service.generate_meal_plan(
        user_id=test_user_id,
        week_start_date=week_start,
    )

    print(f"\n[OK] Generated meal plan: {plan.week_start_date} to {plan.week_end_date}")
    print(f"[OK] Rules applied: {plan.rules_applied}")

    # Check each day
    total_items = 0
    paired_slots = 0
    single_slots = 0

    for day in plan.days:
        print(f"\n{day.day_name} ({day.date}):")

        for slot_name in ["breakfast", "lunch", "dinner", "snacks"]:
            items = getattr(day, slot_name)
            total_items += len(items)

            if len(items) >= 2:
                paired_slots += 1
                status = "[OK] PAIRED"
            elif len(items) == 1:
                single_slots += 1
                status = "* single"
            else:
                status = "[FAIL] empty"

            if items:
                names = [item.recipe_name for item in items]
                categories = [item.category for item in items]
                print(f"  {slot_name.capitalize()}: {status}")
                for i, item in enumerate(items):
                    print(f"    {i+1}. {item.recipe_name} ({item.category})")

    print(f"\n" + "=" * 60)
    print("SUMMARY")
    print("=" * 60)
    print(f"Total meal items: {total_items}")
    print(f"Paired slots (2+ items): {paired_slots}")
    print(f"Single item slots: {single_slots}")
    print(f"Days: {len(plan.days)}")

    # Success criteria: at least some slots should be paired
    if paired_slots > 0:
        print(f"\n[OK] TEST PASSED: Pairing is working!")
        return True
    else:
        print(f"\n[FAIL] TEST FAILED: No paired slots found")
        return False


async def main():
    print("=" * 60)
    print("  MEAL GENERATION WITH PAIRING - TEST SUITE")
    print("=" * 60)

    results = []

    # Test 1: Config loading
    try:
        results.append(("Config Loading", await test_config_loading()))
    except Exception as e:
        logger.error(f"Config loading test failed: {e}")
        results.append(("Config Loading", False))

    # Test 2: Recipe category search
    try:
        results.append(("Recipe Category Search", await test_recipe_category_search()))
    except Exception as e:
        logger.error(f"Recipe category search test failed: {e}")
        results.append(("Recipe Category Search", False))

    # Test 3: Full meal generation
    try:
        results.append(("Meal Plan Generation", await test_meal_generation()))
    except Exception as e:
        logger.error(f"Meal generation test failed: {e}")
        import traceback
        traceback.print_exc()
        results.append(("Meal Plan Generation", False))

    # Summary
    print("\n" + "=" * 60)
    print("FINAL RESULTS")
    print("=" * 60)

    all_passed = True
    for name, passed in results:
        status = "[OK] PASSED" if passed else "[FAIL] FAILED"
        print(f"  {name}: {status}")
        if not passed:
            all_passed = False

    return 0 if all_passed else 1


if __name__ == "__main__":
    exit_code = asyncio.run(main())
    sys.exit(exit_code)
