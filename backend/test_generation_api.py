"""
Meal Plan Generation API Test
Test Profile: Sharma Family (Vegetarian + SATTVIC)
"""
import asyncio
import json
import httpx
import sys
import io
from datetime import datetime

# Fix Unicode output on Windows
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

# Add backend app to path
import sys
sys.path.insert(0, '.')

from app.repositories.user_repository import UserRepository
from app.repositories.meal_plan_repository import MealPlanRepository

BASE_URL = "http://localhost:8000"

# ============================================================
# TEST DATA: SHARMA FAMILY PROFILE
# (Based on Onboarding Screen Inputs)
# ============================================================

SHARMA_FAMILY = {
    # Step 1: Household Information
    "household_size": 3,
    "family_members": [
        {
            "name": "Ramesh",
            "type": "ADULT",
            "age": 45,
            "health_conditions": ["DIABETIC", "LOW_OIL"],
            "notes": "Family head, needs low-sugar meals"
        },
        {
            "name": "Sunita",
            "type": "ADULT",
            "age": 42,
            "health_conditions": ["LOW_SALT"],
            "notes": "Heart-healthy diet preferred"
        },
        {
            "name": "Aarav",
            "type": "CHILD",
            "age": 12,
            "health_conditions": ["NO_SPICY"],
            "notes": "School-going, prefers mild food"
        }
    ],

    # Step 2: Dietary Preferences
    "dietary_type": "vegetarian",
    "dietary_tags": ["vegetarian", "sattvic"],  # SATTVIC = no onion/garlic

    # Step 3: Cuisine Preferences
    "cuisine_preferences": ["north", "south"],
    "spice_level": "medium",

    # Step 4: Allergies & Dislikes
    "allergies": [
        {"ingredient": "Peanuts", "severity": "SEVERE"},
        {"ingredient": "Cashews", "severity": "MILD"}
    ],
    "disliked_ingredients": ["Karela", "Baingan", "Mushroom"],

    # Step 5: Cooking Schedule
    "weekday_cooking_time_minutes": 30,
    "weekend_cooking_time_minutes": 60,
    "busy_days": ["MONDAY", "WEDNESDAY", "FRIDAY"]
}

# Recipe Rules (from Recipe Rules Screen)
RECIPE_RULES = [
    {
        "type": "INCLUDE",
        "target": "Chai",
        "frequency": "DAILY",
        "meal_slot": ["BREAKFAST", "SNACKS"],
        "reason": "Family tradition - chai with every breakfast"
    },
    {
        "type": "INCLUDE",
        "target": "Moringa/Drumstick",
        "frequency": "WEEKLY",
        "times_per_week": 1,
        "reason": "Nutritional boost"
    },
    {
        "type": "EXCLUDE",
        "target": "Paneer",
        "frequency": "NEVER",
        "reason": "Ramesh's diabetic diet"
    },
    {
        "type": "NUTRITION_GOAL",
        "target": "Green Leafy Vegetables",
        "frequency": "WEEKLY",
        "times_per_week": 5,
        "reason": "Iron for Sunita"
    }
]

def print_section(title):
    print()
    print("=" * 70)
    print(f"  {title}")
    print("=" * 70)

def print_subsection(title):
    print()
    print(f"--- {title} ---")

async def main():
    print_section("MEAL PLAN GENERATION API TEST")
    print(f"  Test Profile: Sharma Family")
    print(f"  Date: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")

    # ============================================================
    # DISPLAY INPUT DATA
    # ============================================================
    print_section("INPUT: SHARMA FAMILY PROFILE (Onboarding Data)")

    print_subsection("Step 1: Household")
    print(f"  Household Size: {SHARMA_FAMILY['household_size']} members")
    print()
    print("  Family Members:")
    for i, member in enumerate(SHARMA_FAMILY['family_members'], 1):
        print(f"    {i}. {member['name']} ({member['type']}, {member['age']} yrs)")
        print(f"       Health: {', '.join(member['health_conditions'])}")

    print_subsection("Step 2: Dietary Preferences")
    print(f"  Primary Diet: {SHARMA_FAMILY['dietary_type'].upper()}")
    print(f"  Restrictions: {', '.join(SHARMA_FAMILY['dietary_tags'])}")
    print(f"  Note: SATTVIC means NO onion, NO garlic")

    print_subsection("Step 3: Cuisine Preferences")
    print(f"  Cuisines: {', '.join([c.upper() for c in SHARMA_FAMILY['cuisine_preferences']])}")
    print(f"  Spice Level: {SHARMA_FAMILY['spice_level'].upper()}")

    print_subsection("Step 4: Allergies & Dislikes")
    print("  Allergies (CRITICAL):")
    for allergy in SHARMA_FAMILY['allergies']:
        print(f"    - {allergy['ingredient']}: {allergy['severity']}")
    print()
    print("  Disliked Ingredients:")
    for item in SHARMA_FAMILY['disliked_ingredients']:
        print(f"    - {item}")

    print_subsection("Step 5: Cooking Schedule")
    print(f"  Weekday Max Time: {SHARMA_FAMILY['weekday_cooking_time_minutes']} minutes")
    print(f"  Weekend Max Time: {SHARMA_FAMILY['weekend_cooking_time_minutes']} minutes")
    print(f"  Busy Days: {', '.join(SHARMA_FAMILY['busy_days'])}")

    print_section("INPUT: RECIPE RULES")
    for i, rule in enumerate(RECIPE_RULES, 1):
        print(f"  {i}. {rule['type']}: {rule['target']}")
        freq_str = f"     Frequency: {rule['frequency']}"
        if 'times_per_week' in rule:
            freq_str += f" ({rule['times_per_week']}x/week)"
        if 'meal_slot' in rule:
            freq_str += f" | Meals: {', '.join(rule['meal_slot'])}"
        print(freq_str)
        print(f"     Reason: {rule['reason']}")
        print()

    # ============================================================
    # API CALLS
    # ============================================================
    print_section("API EXECUTION")

    async with httpx.AsyncClient(timeout=60.0) as client:
        # Step 1: Authenticate
        print_subsection("Step 1: Authenticate")
        print("  Request: POST /api/v1/auth/firebase")
        print('  Body: {"firebase_token": "fake-firebase-token"}')

        auth_response = await client.post(
            f"{BASE_URL}/api/v1/auth/firebase",
            json={"firebase_token": "fake-firebase-token"}
        )
        auth_data = auth_response.json()

        print(f"  Response: {auth_response.status_code}")
        print(f"  User ID: {auth_data['user']['id']}")

        token = auth_data["access_token"]
        user_id = auth_data["user"]["id"]
        headers = {"Authorization": f"Bearer {token}"}

        # Step 2: Save Preferences to Firestore
        print_subsection("Step 2: Save Preferences to Firestore")
        print(f"  Saving Sharma Family preferences for user: {user_id}")

        user_repo = UserRepository()
        preferences_to_save = {
            "household_size": SHARMA_FAMILY["household_size"],
            "family_members": SHARMA_FAMILY["family_members"],
            "dietary_type": SHARMA_FAMILY["dietary_type"],
            "dietary_tags": SHARMA_FAMILY["dietary_tags"],
            "cuisine_preferences": SHARMA_FAMILY["cuisine_preferences"],
            "spice_level": SHARMA_FAMILY["spice_level"],
            "allergies": SHARMA_FAMILY["allergies"],
            "disliked_ingredients": SHARMA_FAMILY["disliked_ingredients"],
            "weekday_cooking_time_minutes": SHARMA_FAMILY["weekday_cooking_time_minutes"],
            "weekend_cooking_time_minutes": SHARMA_FAMILY["weekend_cooking_time_minutes"],
            "busy_days": SHARMA_FAMILY["busy_days"],
            "recipe_rules": RECIPE_RULES
        }

        saved_prefs = await user_repo.save_preferences(user_id, preferences_to_save)
        print(f"  Result: Preferences saved successfully")
        print(f"  User marked as onboarded: True")

        # Step 3: Generate Meal Plan
        print_subsection("Step 3: Generate Meal Plan")
        print("  Request: POST /api/v1/meal-plans/generate")
        print('  Body: {"week_start_date": "2026-01-26"}')

        gen_response = await client.post(
            f"{BASE_URL}/api/v1/meal-plans/generate",
            headers=headers,
            json={"week_start_date": "2026-01-26"}
        )

        print(f"  Response: {gen_response.status_code}")

        if gen_response.status_code != 200:
            print(f"  Error: {gen_response.text}")
            return

        meal_plan = gen_response.json()

    # ============================================================
    # DISPLAY OUTPUT
    # ============================================================
    print_section("OUTPUT: GENERATED MEAL PLAN")

    print(f"  Meal Plan ID: {meal_plan.get('id')}")
    print(f"  Week: {meal_plan.get('week_start_date')} to {meal_plan.get('week_end_date')}")
    print(f"  Created: {meal_plan.get('created_at')}")
    print(f"  Total Days: {len(meal_plan.get('days', []))}")

    # Count meals
    total_meals = 0
    for day in meal_plan.get('days', []):
        meals = day.get('meals', {})
        for meal_type in ['breakfast', 'lunch', 'dinner', 'snacks']:
            total_meals += len(meals.get(meal_type, []))
    print(f"  Total Meals: {total_meals}")

    print_section("OUTPUT: WEEKLY MEAL SCHEDULE")

    for day in meal_plan.get('days', []):
        day_name = day.get('day_name', '')
        date = day.get('date', '')
        is_busy = day_name.upper() in SHARMA_FAMILY['busy_days']

        print()
        busy_marker = "[BUSY DAY]" if is_busy else ""
        print(f"  {day_name.upper()} ({date}) {busy_marker}")
        print(f"  {'-' * 60}")

        meals = day.get('meals', {})
        for meal_type in ['breakfast', 'lunch', 'dinner', 'snacks']:
            items = meals.get(meal_type, [])
            if items:
                recipe = items[0]
                name = recipe.get('recipe_name', 'N/A')
                time = recipe.get('prep_time_minutes', 'N/A')
                tags = recipe.get('dietary_tags', [])

                # Truncate long names
                if len(name) > 40:
                    name = name[:37] + "..."

                time_str = f"{time:3}" if isinstance(time, int) else str(time)
                print(f"    {meal_type.capitalize():10} | {name:40} | {time_str} min | {', '.join(tags)}")

    print_section("OUTPUT: CONSTRAINT VERIFICATION")

    # Verify constraints
    all_recipes = []
    for day in meal_plan.get('days', []):
        meals = day.get('meals', {})
        for meal_type, items in meals.items():
            for item in items:
                all_recipes.append({
                    'name': item.get('recipe_name', ''),
                    'tags': item.get('dietary_tags', []),
                    'time': item.get('prep_time_minutes', 0),
                    'day': day.get('day_name', ''),
                    'meal': meal_type
                })

    # Check vegetarian
    veg_count = sum(1 for r in all_recipes if 'vegetarian' in r['tags'] or 'vegan' in r['tags'])
    pct = 100*veg_count//len(all_recipes) if all_recipes else 0
    status = "✓" if pct == 100 else "✗"
    print(f"  {status} Vegetarian recipes: {veg_count}/{len(all_recipes)} ({pct}%)")

    # Check for allergens (with aliases)
    allergen_terms = ['peanut', 'groundnut', 'moongfali', 'cashew', 'kaju']
    allergen_found = [r for r in all_recipes if any(a in r['name'].lower() for a in allergen_terms)]
    status = "✓" if len(allergen_found) == 0 else "✗"
    print(f"  {status} Allergen check (Peanuts/Cashews): {len(allergen_found)} found")
    if allergen_found:
        for r in allergen_found:
            print(f"      WARNING: {r['name']}")

    # Check dislikes (with aliases)
    dislike_terms = ['karela', 'bitter gourd', 'baingan', 'brinjal', 'eggplant', 'vangi', 'mushroom']
    dislike_found = [r for r in all_recipes if any(d in r['name'].lower() for d in dislike_terms)]
    status = "✓" if len(dislike_found) == 0 else "✗"
    print(f"  {status} Dislike check (Karela/Baingan/Mushroom): {len(dislike_found)} found")
    if dislike_found:
        for r in dislike_found:
            print(f"      WARNING: {r['name']}")

    # Check EXCLUDE rule: Paneer
    paneer_terms = ['paneer', 'cottage cheese']
    paneer_found = [r for r in all_recipes if any(p in r['name'].lower() for p in paneer_terms)]
    status = "✓" if len(paneer_found) == 0 else "✗"
    print(f"  {status} EXCLUDE rule (Paneer): {len(paneer_found)} found")
    if paneer_found:
        for r in paneer_found:
            print(f"      WARNING: {r['name']}")

    # Check INCLUDE rule: Chai (DAILY in breakfast/snacks)
    chai_recipes = [r for r in all_recipes if 'chai' in r['name'].lower() and r['meal'] in ['breakfast', 'snacks']]
    status = "✓" if len(chai_recipes) >= 7 else "~"  # Should be 7 for DAILY
    print(f"  {status} INCLUDE rule (Chai DAILY): {len(chai_recipes)}/7 days")
    if chai_recipes:
        chai_days = sorted(set(r['day'] for r in chai_recipes))
        print(f"      Found on: {', '.join(chai_days)}")

    # Check busy day timing
    busy_days = [d.upper() for d in SHARMA_FAMILY['busy_days']]
    busy_day_recipes = [r for r in all_recipes if r['day'].upper() in busy_days]
    over_time = [r for r in busy_day_recipes if isinstance(r['time'], int) and r['time'] > SHARMA_FAMILY['weekday_cooking_time_minutes']]
    within = len(busy_day_recipes) - len(over_time)
    status = "✓" if len(over_time) == 0 else "✗"
    print(f"  {status} Busy day timing (Mon/Wed/Fri <=30 min): {within}/{len(busy_day_recipes)} within limit")
    if over_time:
        for r in over_time:
            print(f"      WARNING: {r['day']} {r['meal']}: {r['name']} ({r['time']} min)")

    print_section("RULE ENFORCEMENT SUMMARY")
    print()
    print("  | Rule Type      | Target                  | Expected        | Result          |")
    print("  |----------------|-------------------------|-----------------|-----------------|")

    # Summarize each rule
    print(f"  | EXCLUDE        | Paneer                  | 0 recipes       | {len(paneer_found)} recipes      |")
    print(f"  | EXCLUDE        | Peanuts (allergy)       | 0 recipes       | {len([r for r in all_recipes if any(t in r['name'].lower() for t in ['peanut', 'groundnut'])])} recipes      |")
    print(f"  | EXCLUDE        | Baingan (dislike)       | 0 recipes       | {len([r for r in all_recipes if any(t in r['name'].lower() for t in ['baingan', 'brinjal', 'eggplant'])])} recipes      |")
    print(f"  | INCLUDE        | Chai (DAILY)            | 7 days          | {len(chai_recipes)} days          |")
    print(f"  | COOKING TIME   | Busy days <=30min       | 12/12 meals     | {within}/{len(busy_day_recipes)} meals      |")
    print()

    print_section("TEST COMPLETE")
    print(f"  Meal plan generated successfully with {total_meals} meals")
    print(f"  Meal Plan ID: {meal_plan.get('id')}")

# Run the test
if __name__ == "__main__":
    asyncio.run(main())
