"""Test script for Meal Plan Generation API.

Sets up test user preferences (Sharma Family) and calls the generation API
to verify the output matches expectations.

Run with:
    cd backend
    .\\venv\\Scripts\\activate  # Windows
    python scripts/test_generation_api.py
"""

import asyncio
import json
import sys
import io
from datetime import date, timedelta
from typing import Any

# Fix Windows console encoding
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

# Initialize Firebase first
import firebase_admin
from firebase_admin import credentials, firestore

# Check if already initialized
if not firebase_admin._apps:
    cred = credentials.Certificate("rasoiai-firebase-service-account.json")
    firebase_admin.initialize_app(cred)

db = firestore.client()


# =============================================================================
# TEST DATA: SHARMA FAMILY PREFERENCES
# =============================================================================

TEST_USER_ID = "test-sharma-family"

SHARMA_FAMILY_PREFERENCES = {
    # Family composition
    "household_size": 4,
    "family_members": [
        {"name": "Ramesh", "type": "ADULT", "age": 45, "special_needs": []},
        {"name": "Sunita", "type": "ADULT", "age": 42, "special_needs": ["LOW_OIL"]},
        {"name": "Arjun", "type": "CHILD", "age": 16, "special_needs": ["HIGH_PROTEIN"]},
        {"name": "Priya", "type": "CHILD", "age": 12, "special_needs": []},
    ],

    # Diet preferences
    "dietary_tags": ["vegetarian"],
    "cuisine_preferences": ["north", "west"],
    "spice_level": "medium",

    # Dislikes and allergies
    "disliked_ingredients": ["karela", "lauki", "turai"],
    "allergies": [
        {"ingredient": "peanuts", "severity": "SEVERE"},
    ],

    # Cooking time
    "weekday_cooking_time_minutes": 30,
    "weekend_cooking_time_minutes": 60,
    "busy_days": ["MONDAY", "WEDNESDAY"],

    # Recipe Rules
    "recipe_rules": [
        # INCLUDE: Chai every morning
        {
            "type": "INCLUDE",
            "target": "Chai",
            "frequency": "DAILY",
            "meal_slot": ["breakfast"],
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
        # EXCLUDE: No mushroom ever
        {
            "type": "EXCLUDE",
            "target": "Mushroom",
            "frequency": "NEVER",
            "reason": "dislike",
            "is_active": True,
        },
        # EXCLUDE: No onion on Tuesdays (spiritual reasons)
        {
            "type": "EXCLUDE",
            "target": "Onion",
            "frequency": "SPECIFIC_DAYS",
            "specific_days": ["TUESDAY"],
            "reason": "religious",
            "is_active": True,
        },
    ],
}


# =============================================================================
# HELPER FUNCTIONS
# =============================================================================

def print_section(title: str, width: int = 80):
    """Print a section header."""
    print("\n" + "=" * width)
    print(f" {title}")
    print("=" * width)


def print_subsection(title: str):
    """Print a subsection header."""
    print(f"\n--- {title} ---")


def format_meal_items(items: list[dict], indent: int = 4) -> str:
    """Format meal items for display."""
    if not items:
        return " " * indent + "(no items)"

    lines = []
    for i, item in enumerate(items, 1):
        name = item.get("recipe_name", "Unknown")
        time = item.get("prep_time_minutes", "?")
        calories = item.get("calories", 0)
        tags = ", ".join(item.get("dietary_tags", []))
        locked = "[LOCKED]" if item.get("is_locked") else ""

        lines.append(f"{' ' * indent}{i}. {name} ({time} min, {calories} cal) {locked}")
        if tags:
            lines.append(f"{' ' * (indent + 3)}Tags: {tags}")

    return "\n".join(lines)


async def setup_test_user():
    """Create/update test user and preferences in Firestore."""
    print_section("SETTING UP TEST USER")

    # Create test user
    users_ref = db.collection("users")
    user_doc = users_ref.document(TEST_USER_ID)

    user_data = {
        "id": TEST_USER_ID,
        "email": "sharma.family@test.com",
        "display_name": "Sharma Family",
        "firebase_uid": TEST_USER_ID,
        "is_active": True,
        "is_onboarded": True,
    }
    user_doc.set(user_data)
    print(f"[OK] Created user: {TEST_USER_ID}")

    # Set preferences in the correct subcollection: users/{user_id}/preferences/settings
    prefs_doc = user_doc.collection("preferences").document("settings")
    prefs_doc.set(SHARMA_FAMILY_PREFERENCES)
    print(f"[OK] Set preferences in users/{TEST_USER_ID}/preferences/settings")

    return user_data


def display_input_preferences():
    """Display the input preferences in a nice format."""
    print_section("INPUT: SHARMA FAMILY PREFERENCES")

    prefs = SHARMA_FAMILY_PREFERENCES

    print_subsection("Family Composition")
    print(f"  Household Size: {prefs['household_size']} members")
    for member in prefs["family_members"]:
        needs = ", ".join(member.get("special_needs", [])) or "None"
        print(f"    - {member['name']} ({member['type']}, age {member.get('age', '?')})")
        print(f"      Special needs: {needs}")

    print_subsection("Diet & Cuisine")
    print(f"  Primary Diet: {', '.join(prefs['dietary_tags'])}")
    print(f"  Cuisines: {', '.join(prefs['cuisine_preferences'])}")
    print(f"  Spice Level: {prefs['spice_level']}")

    print_subsection("Restrictions")
    print(f"  Disliked: {', '.join(prefs['disliked_ingredients'])}")
    for allergy in prefs["allergies"]:
        print(f"  Allergy: {allergy['ingredient']} ({allergy['severity']})")

    print_subsection("Cooking Time")
    print(f"  Weekday: {prefs['weekday_cooking_time_minutes']} minutes")
    print(f"  Weekend: {prefs['weekend_cooking_time_minutes']} minutes")
    print(f"  Busy Days: {', '.join(prefs['busy_days'])}")

    print_subsection("Recipe Rules")
    for rule in prefs["recipe_rules"]:
        rule_type = rule["type"]
        target = rule["target"]
        freq = rule.get("frequency", "")
        slots = ", ".join(rule.get("meal_slot", []))

        if rule_type == "INCLUDE":
            if freq == "DAILY":
                desc = f"Every day"
            elif freq == "TIMES_PER_WEEK":
                desc = f"{rule.get('times_per_week', 1)}x per week"
            else:
                desc = freq
            print(f"  [+] INCLUDE: {target} - {desc} ({slots})")
        else:
            reason = rule.get("reason", "")
            if freq == "NEVER":
                print(f"  [-] EXCLUDE: {target} - Never ({reason})")
            elif freq == "SPECIFIC_DAYS":
                days = ", ".join(rule.get("specific_days", []))
                print(f"  [-] EXCLUDE: {target} - On {days} ({reason})")


async def call_generation_api() -> dict[str, Any]:
    """Call the meal plan generation API."""
    print_section("CALLING GENERATION API")

    # Import services
    from app.services.meal_generation_service import MealGenerationService

    # Calculate week start (next Monday)
    today = date.today()
    days_until_monday = (7 - today.weekday()) % 7
    if days_until_monday == 0:
        days_until_monday = 7  # Next Monday if today is Monday
    week_start = today + timedelta(days=days_until_monday)

    # Could also use today for immediate testing
    week_start = today - timedelta(days=today.weekday())  # This Monday

    print(f"  Week Start: {week_start.isoformat()} ({week_start.strftime('%A')})")

    # Generate meal plan
    service = MealGenerationService()

    print("  Generating meal plan...")
    plan = await service.generate_meal_plan(
        user_id=TEST_USER_ID,
        week_start_date=week_start,
    )

    print(f"  [OK] Generated plan: {plan.week_start_date} to {plan.week_end_date}")

    # Convert to dict for display
    return {
        "week_start_date": plan.week_start_date,
        "week_end_date": plan.week_end_date,
        "days": [
            {
                "date": day.date,
                "day_name": day.day_name,
                "meals": {
                    "breakfast": [
                        {
                            "recipe_name": item.recipe_name,
                            "recipe_id": item.recipe_id,
                            "prep_time_minutes": item.prep_time_minutes,
                            "calories": item.calories,
                            "dietary_tags": item.dietary_tags,
                            "category": item.category,
                            "is_locked": item.is_locked,
                        }
                        for item in day.breakfast
                    ],
                    "lunch": [
                        {
                            "recipe_name": item.recipe_name,
                            "recipe_id": item.recipe_id,
                            "prep_time_minutes": item.prep_time_minutes,
                            "calories": item.calories,
                            "dietary_tags": item.dietary_tags,
                            "category": item.category,
                            "is_locked": item.is_locked,
                        }
                        for item in day.lunch
                    ],
                    "dinner": [
                        {
                            "recipe_name": item.recipe_name,
                            "recipe_id": item.recipe_id,
                            "prep_time_minutes": item.prep_time_minutes,
                            "calories": item.calories,
                            "dietary_tags": item.dietary_tags,
                            "category": item.category,
                            "is_locked": item.is_locked,
                        }
                        for item in day.dinner
                    ],
                    "snacks": [
                        {
                            "recipe_name": item.recipe_name,
                            "recipe_id": item.recipe_id,
                            "prep_time_minutes": item.prep_time_minutes,
                            "calories": item.calories,
                            "dietary_tags": item.dietary_tags,
                            "category": item.category,
                            "is_locked": item.is_locked,
                        }
                        for item in day.snacks
                    ],
                },
            }
            for day in plan.days
        ],
        "rules_applied": plan.rules_applied,
    }


def display_output(plan: dict[str, Any]):
    """Display the generated meal plan output."""
    print_section("OUTPUT: GENERATED MEAL PLAN")

    print(f"\nWeek: {plan['week_start_date']} to {plan['week_end_date']}")
    print(f"Rules Applied: {json.dumps(plan['rules_applied'], indent=2)}")

    for day in plan["days"]:
        print_subsection(f"{day['day_name']} ({day['date']})")

        for meal_type in ["breakfast", "lunch", "dinner", "snacks"]:
            items = day["meals"].get(meal_type, [])
            print(f"\n  {meal_type.upper()}:")
            print(format_meal_items(items, indent=4))


def analyze_results(plan: dict[str, Any]):
    """Analyze the results for rule compliance."""
    print_section("ANALYSIS: RULE COMPLIANCE")

    prefs = SHARMA_FAMILY_PREFERENCES

    # Track INCLUDE rule fulfillment
    chai_count = 0
    dal_count = 0
    paneer_count = 0

    # Track EXCLUDE violations
    mushroom_found = []
    karela_found = []
    peanut_found = []

    for day in plan["days"]:
        day_name = day["day_name"]

        for meal_type in ["breakfast", "lunch", "dinner", "snacks"]:
            for item in day["meals"].get(meal_type, []):
                name = item["recipe_name"].lower()

                # Check INCLUDE rules
                if "chai" in name or "tea" in name:
                    chai_count += 1
                if "dal" in name or "daal" in name:
                    dal_count += 1
                if "paneer" in name:
                    paneer_count += 1

                # Check EXCLUDE violations
                if "mushroom" in name:
                    mushroom_found.append(f"{day_name} {meal_type}: {item['recipe_name']}")
                if "karela" in name or "bitter gourd" in name:
                    karela_found.append(f"{day_name} {meal_type}: {item['recipe_name']}")
                if "peanut" in name or "groundnut" in name:
                    peanut_found.append(f"{day_name} {meal_type}: {item['recipe_name']}")

    print_subsection("INCLUDE Rule Fulfillment")
    print(f"  Chai (expected: 7/day): {chai_count} found {'[OK]' if chai_count >= 7 else '[WARN]'}")
    print(f"  Dal (expected: 4/week): {dal_count} found {'[OK]' if dal_count >= 4 else '[WARN]'}")
    print(f"  Paneer (expected: 2/week): {paneer_count} found {'[OK]' if paneer_count >= 2 else '[WARN]'}")

    print_subsection("EXCLUDE Rule Compliance")
    if mushroom_found:
        print(f"  [FAIL] Mushroom VIOLATION: {len(mushroom_found)} found")
        for v in mushroom_found:
            print(f"      - {v}")
    else:
        print("  [OK] Mushroom: None found (correct)")

    if karela_found:
        print(f"  [FAIL] Karela VIOLATION: {len(karela_found)} found")
        for v in karela_found:
            print(f"      - {v}")
    else:
        print("  [OK] Karela: None found (correct)")

    if peanut_found:
        print(f"  [FAIL] Peanut ALLERGY VIOLATION: {len(peanut_found)} found")
        for v in peanut_found:
            print(f"      - {v}")
    else:
        print("  [OK] Peanuts: None found (correct)")

    # Count unique recipes
    all_recipes = set()
    for day in plan["days"]:
        for meal_type in ["breakfast", "lunch", "dinner", "snacks"]:
            for item in day["meals"].get(meal_type, []):
                all_recipes.add(item["recipe_name"])

    print_subsection("Statistics")
    total_items = sum(
        len(day["meals"].get(mt, []))
        for day in plan["days"]
        for mt in ["breakfast", "lunch", "dinner", "snacks"]
    )
    print(f"  Total meal items: {total_items}")
    print(f"  Unique recipes: {len(all_recipes)}")
    print(f"  Recipe variety: {len(all_recipes) / total_items * 100:.1f}%")


async def main():
    """Main test function."""
    print("\n" + "=" * 80)
    print(" RASOIAI MEAL PLAN GENERATION API TEST")
    print(" Test Family: Sharma Family (4 members)")
    print("=" * 80)

    try:
        # Setup test data
        await setup_test_user()

        # Display input
        display_input_preferences()

        # Call API
        plan = await call_generation_api()

        # Display output
        display_output(plan)

        # Analyze results
        analyze_results(plan)

        print_section("TEST COMPLETE")
        print("\n[SUCCESS] All steps completed successfully!")

    except Exception as e:
        print(f"\n[ERROR] {e}")
        import traceback
        traceback.print_exc()
        return 1

    return 0


if __name__ == "__main__":
    exit(asyncio.run(main()))
