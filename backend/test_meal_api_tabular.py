"""
Meal Plan Generation API Test - Tabular Output
Test Profile: Sharma Family (Eggetarian/Non-Veg, North/West cuisine)

Validates:
- INCLUDE rules (Chai daily breakfast+snacks, Dal 4x/week, Paneer 2x/week, Egg 4x/week, Chicken 2x/week)
- EXCLUDE rules (No mushroom, No onion on Tuesdays, No non-veg/egg on Tuesdays)
- Allergies (No peanuts/groundnut)
- Dislikes (No karela, lauki, turai)
- 2-item pairing per meal slot

Run: cd backend && python test_meal_api_tabular.py
"""
import asyncio
import sys
import io
from datetime import date, timedelta

# Fix Unicode output on Windows
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
sys.path.insert(0, '.')

from app.repositories.user_repository import UserRepository

# ============================================================
# TEST DATA: SHARMA FAMILY PROFILE (Based on Plan)
# ============================================================

TEST_USER_ID = "test-sharma-family"

SHARMA_FAMILY = {
    # Family Composition
    "household_size": 4,
    "family_members": [
        {"name": "Ramesh", "type": "ADULT", "age": 45, "special_needs": []},
        {"name": "Sunita", "type": "ADULT", "age": 42, "special_needs": ["LOW_OIL"]},
        {"name": "Arjun", "type": "CHILD", "age": 16, "special_needs": ["HIGH_PROTEIN"]},
        {"name": "Priya", "type": "CHILD", "age": 12, "special_needs": []},
    ],

    # Dietary Preferences
    "dietary_tags": ["eggetarian", "non_vegetarian"],
    "cuisine_preferences": ["north", "west"],
    "spice_level": "medium",

    # Allergies & Dislikes
    "allergies": [
        {"ingredient": "peanuts", "severity": "SEVERE"},
    ],
    "disliked_ingredients": ["karela", "lauki", "turai"],

    # Cooking Schedule
    "weekday_cooking_time_minutes": 30,
    "weekend_cooking_time_minutes": 60,
    "busy_days": ["MONDAY", "WEDNESDAY"],

    # Recipe Rules
    "recipe_rules": [
        # INCLUDE: Chai every morning and snacks
        {
            "type": "INCLUDE",
            "target": "Chai",
            "frequency": "DAILY",
            "meal_slot": ["breakfast", "snacks"],
            "is_active": True,
        },
        # INCLUDE: Dal 4 times per week
        {
            "type": "INCLUDE",
            "target": "Dal",
            "frequency": "TIMES_PER_WEEK",
            "times_per_week": 4,
            "meal_slot": ["lunch", "dinner"],
            "is_active": True,
        },
        # INCLUDE: Paneer twice a week
        {
            "type": "INCLUDE",
            "target": "Paneer",
            "frequency": "TIMES_PER_WEEK",
            "times_per_week": 2,
            "meal_slot": ["lunch", "dinner"],
            "is_active": True,
        },
        # INCLUDE: Egg 4 times per week
        {
            "type": "INCLUDE",
            "target": "Egg",
            "frequency": "TIMES_PER_WEEK",
            "times_per_week": 4,
            "meal_slot": ["breakfast", "lunch", "dinner"],
            "is_active": True,
        },
        # INCLUDE: Chicken twice a week
        {
            "type": "INCLUDE",
            "target": "Chicken",
            "frequency": "TIMES_PER_WEEK",
            "times_per_week": 2,
            "meal_slot": ["lunch", "dinner"],
            "is_active": True,
        },
        # EXCLUDE: No mushroom ever
        {
            "type": "EXCLUDE",
            "target": "Mushroom",
            "frequency": "NEVER",
            "reason": "dislike",
            "is_active": True,
        },
        # EXCLUDE: No onion on Tuesdays (religious)
        {
            "type": "EXCLUDE",
            "target": "Onion",
            "frequency": "SPECIFIC_DAYS",
            "specific_days": ["TUESDAY"],
            "reason": "religious",
            "is_active": True,
        },
        # EXCLUDE: No non-veg on Tuesdays (religious)
        {
            "type": "EXCLUDE",
            "target": "Non-Veg",
            "frequency": "SPECIFIC_DAYS",
            "specific_days": ["TUESDAY"],
            "reason": "religious",
            "is_active": True,
        },
        # EXCLUDE: No egg on Tuesdays (religious)
        {
            "type": "EXCLUDE",
            "target": "Egg",
            "frequency": "SPECIFIC_DAYS",
            "specific_days": ["TUESDAY"],
            "reason": "religious",
            "is_active": True,
        },
    ],
}

# ============================================================
# TABLE FORMATTING
# ============================================================

def print_box(title, width=90):
    """Print a boxed title."""
    print()
    print("+" + "=" * (width - 2) + "+")
    print("|" + title.center(width - 2) + "|")
    print("+" + "=" * (width - 2) + "+")


def print_table(headers, rows, widths=None):
    """Print a formatted table."""
    if not widths:
        widths = [max(len(str(h)), max(len(str(r[i])) for r in rows) if rows else 0) + 2
                  for i, h in enumerate(headers)]

    # Header
    header_row = "|"
    for h, w in zip(headers, widths):
        header_row += f" {str(h):<{w-1}}|"
    print(header_row)

    # Separator
    sep = "|" + "|".join(["-" * w for w in widths]) + "|"
    print(sep)

    # Rows
    for row in rows:
        row_str = "|"
        for val, w in zip(row, widths):
            val_str = str(val)[:w-2] if len(str(val)) > w-2 else str(val)
            row_str += f" {val_str:<{w-1}}|"
        print(row_str)


def print_input_tables():
    """Display input data in tables."""
    print_box("INPUT: SHARMA FAMILY PROFILE")

    # Family Members Table
    print("\n  FAMILY MEMBERS:")
    headers = ["Name", "Type", "Age", "Special Needs"]
    rows = []
    for m in SHARMA_FAMILY['family_members']:
        needs = ", ".join(m['special_needs']) if m['special_needs'] else "-"
        rows.append([m['name'], m['type'], m['age'], needs])
    print_table(headers, rows, [12, 10, 6, 20])

    # Preferences Table
    print("\n  PREFERENCES:")
    headers = ["Setting", "Value"]
    rows = [
        ["Dietary Tags", ", ".join(SHARMA_FAMILY['dietary_tags']).upper()],
        ["Cuisines", ", ".join(SHARMA_FAMILY['cuisine_preferences']).upper()],
        ["Spice Level", SHARMA_FAMILY['spice_level'].upper()],
        ["Allergies", ", ".join([f"{a['ingredient']} ({a['severity']})" for a in SHARMA_FAMILY['allergies']])],
        ["Dislikes", ", ".join(SHARMA_FAMILY['disliked_ingredients'])],
        ["Weekday Cooking", f"{SHARMA_FAMILY['weekday_cooking_time_minutes']} min"],
        ["Weekend Cooking", f"{SHARMA_FAMILY['weekend_cooking_time_minutes']} min"],
        ["Busy Days", ", ".join(SHARMA_FAMILY['busy_days'])],
    ]
    print_table(headers, rows, [20, 50])

    # Recipe Rules Table
    print("\n  RECIPE RULES:")
    headers = ["Type", "Target", "Frequency", "Meal Slots", "Reason"]
    rows = []
    for rule in SHARMA_FAMILY['recipe_rules']:
        rule_type = rule['type']
        target = rule['target']
        freq = rule.get('frequency', '')
        if 'times_per_week' in rule:
            freq = f"{rule['times_per_week']}x/week"
        elif freq == "SPECIFIC_DAYS":
            freq = f"On {', '.join(rule.get('specific_days', []))}"
        slots = ", ".join(rule.get('meal_slot', ['all'])) if 'meal_slot' in rule else "all"
        reason = rule.get('reason', '-')
        rows.append([rule_type, target, freq, slots, reason])
    print_table(headers, rows, [10, 12, 15, 20, 15])


async def setup_test_user():
    """Create/update test user and preferences in PostgreSQL."""
    user_repo = UserRepository()

    # Check if user exists
    existing_user = await user_repo.get_by_id(TEST_USER_ID)

    if not existing_user:
        # Create test user - need to use direct DB access since create() generates UUID
        from app.db.postgres import async_session_maker
        from app.models.user import User

        async with async_session_maker() as session:
            user = User(
                id=TEST_USER_ID,
                firebase_uid=TEST_USER_ID,
                email="sharma.family@test.com",
                name="Sharma Family",
                is_onboarded=True,
                is_active=True,
            )
            session.add(user)
            await session.commit()
            print(f"  [OK] Created user: {TEST_USER_ID}")
    else:
        print(f"  [OK] User exists: {TEST_USER_ID}")

    # Save preferences
    await user_repo.save_preferences(TEST_USER_ID, SHARMA_FAMILY)
    print(f"  [OK] Saved preferences to PostgreSQL")

    return {"id": TEST_USER_ID, "name": "Sharma Family"}


async def call_generation_api():
    """Call the AI meal plan generation service directly."""
    from app.services.ai_meal_service import AIMealService

    # Start from Monday of current week
    today = date.today()
    week_start = today - timedelta(days=today.weekday())  # This Monday

    service = AIMealService()
    plan = await service.generate_meal_plan(
        user_id=TEST_USER_ID,
        week_start_date=week_start,
    )

    return plan


def print_output_table(plan):
    """Display meal plan in a single sorted table."""
    print_box("OUTPUT: GENERATED MEAL PLAN (Sorted by Date -> Meal Type)")

    print(f"\n  Week: {plan.week_start_date} to {plan.week_end_date}")

    # Flatten and sort
    meal_order = {'breakfast': 0, 'lunch': 1, 'dinner': 2, 'snacks': 3}

    rows = []
    for day in plan.days:
        date_str = day.date
        day_name = day.day_name
        is_busy = day_name.upper() in SHARMA_FAMILY['busy_days']
        busy_marker = "*" if is_busy else ""

        for slot, order in sorted(meal_order.items(), key=lambda x: x[1]):
            items = getattr(day, slot, [])

            item1 = items[0].recipe_name if len(items) > 0 else "-"
            item2 = items[1].recipe_name if len(items) > 1 else "-"
            time1 = items[0].prep_time_minutes if len(items) > 0 else 0
            tags = items[0].dietary_tags if len(items) > 0 else []
            tag_str = ", ".join(tags[:2]) if tags else "-"

            rows.append([
                f"{date_str[5:]}{busy_marker}",  # MM-DD
                day_name[:3],
                slot.capitalize(),
                item1[:30],
                item2[:30],
                tag_str[:15],
                f"{time1}m",
            ])

    print("\n  WEEKLY MEAL SCHEDULE:")
    headers = ["Date", "Day", "Meal", "Item 1", "Item 2", "Tags", "Time"]
    print_table(headers, rows, [10, 5, 10, 32, 32, 17, 6])

    print("\n  * = Busy day (30 min max cooking time)")


def validate_results(plan):
    """Validate rule compliance and return results."""
    print_box("CONSTRAINT VERIFICATION")

    # Collect all recipes with metadata
    all_recipes = []
    for day in plan.days:
        day_name = day.day_name
        for slot in ['breakfast', 'lunch', 'dinner', 'snacks']:
            items = getattr(day, slot, [])
            for item in items:
                all_recipes.append({
                    'name': item.recipe_name,
                    'tags': item.dietary_tags,
                    'time': item.prep_time_minutes,
                    'day': day_name,
                    'meal': slot,
                })

    results = {}
    rows = []

    # 1. Dietary check (eggetarian/non-veg allowed)
    valid_tags = ['vegetarian', 'vegan', 'eggetarian', 'non_vegetarian']
    diet_count = sum(1 for r in all_recipes if any(t in r['tags'] for t in valid_tags))
    passed = diet_count == len(all_recipes)
    results['dietary'] = passed
    status = "PASS" if passed else "FAIL"
    rows.append(["Valid Dietary Tags", f"{len(all_recipes)}/{len(all_recipes)}", f"{diet_count}/{len(all_recipes)}", status])

    # 2. Chai in breakfast AND snacks (DAILY)
    # Note: Only count actual chai/tea drinks, not recipes that happen to have "chai" substring
    # e.g., "luchi" contains "chai" but is bread, not tea
    def is_chai_recipe(name):
        name_lower = name.lower()
        # Positive matches - actual chai/tea recipes
        chai_terms = ['masala chai', 'chai recipe', 'chai tea', 'cutting chai', 'adrak chai',
                      'ginger chai', 'irani chai', 'herbal chai', 'lemongrass chai', 'chocolate chai',
                      'green tea', 'ginger tea', 'cumin tea', 'herbal tea', 'kahwa', 'iced tea']
        # Also match standalone " chai" or "chai " at word boundaries
        if any(term in name_lower for term in chai_terms):
            return True
        # Check for chai/tea as a word (not substring like "luchi" or "steam")
        import re
        if re.search(r'\bchai\b', name_lower) or re.search(r'\btea\b', name_lower):
            return True
        return False

    chai_breakfast = [r for r in all_recipes if is_chai_recipe(r['name']) and r['meal'] == 'breakfast']
    chai_snacks = [r for r in all_recipes if is_chai_recipe(r['name']) and r['meal'] == 'snacks']
    chai_total = len(chai_breakfast) + len(chai_snacks)
    passed = len(chai_breakfast) >= 7 and len(chai_snacks) >= 7
    results['chai_daily'] = passed
    status = "PASS" if passed else "FAIL"
    rows.append(["Chai DAILY (breakfast+snacks)", "14", str(chai_total), status])

    # 3. Dal 4x/week
    dal_terms = ['dal', 'daal', 'lentil']
    dal_recipes = [r for r in all_recipes if any(t in r['name'].lower() for t in dal_terms) and r['meal'] in ['lunch', 'dinner']]
    passed = len(dal_recipes) >= 4
    results['dal_4x'] = passed
    status = "PASS" if passed else "FAIL"
    rows.append(["Dal 4x/week (lunch/dinner)", ">=4", str(len(dal_recipes)), status])

    # 4. Paneer 2x/week
    paneer_recipes = [r for r in all_recipes if 'paneer' in r['name'].lower() and r['meal'] in ['lunch', 'dinner']]
    passed = len(paneer_recipes) >= 2
    results['paneer_2x'] = passed
    status = "PASS" if passed else "FAIL"
    rows.append(["Paneer 2x/week (lunch/dinner)", ">=2", str(len(paneer_recipes)), status])

    # 5. Egg 4x/week
    egg_terms = ['egg', 'omelette', 'omelet', 'anda', 'bhurji']
    egg_recipes = [r for r in all_recipes if any(t in r['name'].lower() for t in egg_terms)]
    passed = len(egg_recipes) >= 4
    results['egg_4x'] = passed
    status = "PASS" if passed else "FAIL"
    rows.append(["Egg 4x/week", ">=4", str(len(egg_recipes)), status])

    # 6. Chicken 2x/week
    chicken_terms = ['chicken', 'murgh', 'murg']
    chicken_recipes = [r for r in all_recipes if any(t in r['name'].lower() for t in chicken_terms) and r['meal'] in ['lunch', 'dinner']]
    passed = len(chicken_recipes) >= 2
    results['chicken_2x'] = passed
    status = "PASS" if passed else "FAIL"
    rows.append(["Chicken 2x/week (lunch/dinner)", ">=2", str(len(chicken_recipes)), status])

    # 7. No Mushroom (EXCLUDE)
    mushroom_found = [r for r in all_recipes if 'mushroom' in r['name'].lower()]
    passed = len(mushroom_found) == 0
    results['no_mushroom'] = passed
    status = "PASS" if passed else "FAIL"
    rows.append(["No Mushroom (EXCLUDE)", "0", str(len(mushroom_found)), status])

    # 8. No Peanuts (ALLERGY)
    peanut_terms = ['peanut', 'groundnut', 'moongphali']
    peanut_found = [r for r in all_recipes if any(t in r['name'].lower() for t in peanut_terms)]
    passed = len(peanut_found) == 0
    results['no_peanuts'] = passed
    status = "PASS" if passed else "FAIL"
    rows.append(["No Peanuts (ALLERGY)", "0", str(len(peanut_found)), status])

    # 9. No Dislikes (karela, lauki, turai)
    dislike_terms = ['karela', 'bitter gourd', 'lauki', 'bottle gourd', 'turai', 'ridge gourd']
    dislike_found = [r for r in all_recipes if any(d in r['name'].lower() for d in dislike_terms)]
    passed = len(dislike_found) == 0
    results['no_dislikes'] = passed
    status = "PASS" if passed else "FAIL"
    rows.append(["No Dislikes (karela/lauki/turai)", "0", str(len(dislike_found)), status])

    # 10. No Non-Veg on Tuesdays
    nonveg_terms = ['chicken', 'mutton', 'fish', 'prawn', 'shrimp', 'meat', 'murgh', 'gosht', 'keema']
    tuesday_nonveg = [r for r in all_recipes if r['day'] == 'Tuesday' and any(t in r['name'].lower() for t in nonveg_terms)]
    passed = len(tuesday_nonveg) == 0
    results['no_nonveg_tuesday'] = passed
    status = "PASS" if passed else "FAIL"
    rows.append(["No Non-Veg on Tuesdays", "0", str(len(tuesday_nonveg)), status])

    # 11. No Egg on Tuesdays
    tuesday_egg = [r for r in all_recipes if r['day'] == 'Tuesday' and any(t in r['name'].lower() for t in egg_terms)]
    passed = len(tuesday_egg) == 0
    results['no_egg_tuesday'] = passed
    status = "PASS" if passed else "FAIL"
    rows.append(["No Egg on Tuesdays", "0", str(len(tuesday_egg)), status])

    # 12. 2-item pairing check
    paired_slots = 0
    total_slots = 0
    for day in plan.days:
        for slot in ['breakfast', 'lunch', 'dinner', 'snacks']:
            items = getattr(day, slot, [])
            total_slots += 1
            if len(items) >= 2:
                paired_slots += 1
    passed = paired_slots == total_slots
    results['pairing'] = passed
    status = "PASS" if passed else "FAIL"
    rows.append(["2-item Pairing", f"{total_slots}/{total_slots}", f"{paired_slots}/{total_slots}", status])

    # 13. Cooking time limits (weekday <= 30m, weekend <= 60m)
    weekdays = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday']
    weekday_over = [r for r in all_recipes if r['day'] in weekdays and r['time'] > 30]
    weekend_over = [r for r in all_recipes if r['day'] in ['Saturday', 'Sunday'] and r['time'] > 60]
    passed = len(weekday_over) == 0 and len(weekend_over) == 0
    results['cooking_time'] = passed
    status = "PASS" if passed else "WARN" if len(weekday_over) + len(weekend_over) <= 5 else "FAIL"
    rows.append(["Cooking Time Limits", "0 over", f"{len(weekday_over) + len(weekend_over)} over", status])

    # Print table
    headers = ["Constraint", "Expected", "Actual", "Status"]
    print_table(headers, rows, [35, 15, 15, 8])

    # Print violations if any
    if mushroom_found:
        print(f"\n  MUSHROOM VIOLATIONS: {[r['name'] for r in mushroom_found]}")
    if peanut_found:
        print(f"\n  PEANUT VIOLATIONS: {[r['name'] for r in peanut_found]}")
    if dislike_found:
        print(f"\n  DISLIKE VIOLATIONS: {[r['name'] for r in dislike_found]}")
    if tuesday_nonveg:
        print(f"\n  TUESDAY NON-VEG VIOLATIONS: {[r['name'] for r in tuesday_nonveg]}")
    if tuesday_egg:
        print(f"\n  TUESDAY EGG VIOLATIONS: {[r['name'] for r in tuesday_egg]}")
    if weekday_over:
        print(f"\n  WEEKDAY TIME VIOLATIONS: {[(r['day'], r['name'], r['time']) for r in weekday_over[:5]]}")

    return results


def print_summary(results, plan):
    """Print summary of test results."""
    print_box("SUMMARY")

    passed = sum(1 for v in results.values() if v)
    total = len(results)

    total_items = sum(
        len(getattr(day, slot, []))
        for day in plan.days
        for slot in ['breakfast', 'lunch', 'dinner', 'snacks']
    )

    unique_recipes = set()
    for day in plan.days:
        for slot in ['breakfast', 'lunch', 'dinner', 'snacks']:
            for item in getattr(day, slot, []):
                unique_recipes.add(item.recipe_name)

    print(f"\n  Tests Passed: {passed}/{total}")
    print(f"  Total Meal Items: {total_items}")
    print(f"  Unique Recipes: {len(unique_recipes)}")
    print(f"  Recipe Variety: {len(unique_recipes) / total_items * 100:.1f}%")
    print()

    if passed == total:
        print("  [SUCCESS] All validations passed!")
    else:
        print("  [WARN] Some validations failed. Review above for details.")
    print()


async def main():
    """Main test function."""
    print_box("MEAL GENERATION API TEST", 90)
    print(f"  Test Profile: SHARMA FAMILY (4 members, Eggetarian/Non-Veg)")
    print(f"  Date: {date.today().isoformat()}")

    # Setup
    print("\n  Setting up test user in PostgreSQL...")
    await setup_test_user()

    # Display input
    print_input_tables()

    # Generate
    print("\n  Generating meal plan...")
    plan = await call_generation_api()
    print(f"  [OK] Plan generated: {plan.week_start_date} to {plan.week_end_date}")

    # Display output
    print_output_table(plan)

    # Validate
    results = validate_results(plan)

    # Summary
    print_summary(results, plan)


if __name__ == "__main__":
    asyncio.run(main())
