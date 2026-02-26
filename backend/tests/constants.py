"""Shared test constants for RasoiAI backend tests.

Extracted from test_sharma_recipe_rules.py and common fixtures.
Import these instead of redefining in each test file.
"""

# ==================== Auth / Identity Constants ====================

TEST_EMAIL = "test@example.com"
FAKE_FIREBASE_TOKEN = "fake-firebase-token"
FAKE_FIREBASE_UID = "test-firebase-uid"

# ==================== Sharma Family Onboarding Data ====================

SHARMA_ONBOARDING_PREFERENCES = {
    "household_size": 3,
    "primary_diet": "non_vegetarian",
    "dietary_restrictions": [],
    "cuisine_preferences": ["north", "west"],
    "spice_level": "medium",
    "disliked_ingredients": ["Karela", "Baingan"],
    "weekday_cooking_time": 30,
    "weekend_cooking_time": 60,
    "busy_days": ["MONDAY", "WEDNESDAY"],
}

SHARMA_FAMILY_MEMBERS = [
    {
        "name": "Priya Sharma",
        "age_group": "adult",
        "dietary_restrictions": [],
        "health_conditions": [],
    },
    {
        "name": "Amit Sharma",
        "age_group": "child",
        "dietary_restrictions": [],
        "health_conditions": [],
    },
    {
        "name": "Dadi Sharma",
        "age_group": "senior",
        "dietary_restrictions": ["low_salt"],
        "health_conditions": ["diabetes"],
    },
]

# ==================== Sharma Recipe Rule Constants ====================

CHAI_BREAKFAST_RULE = {
    "target_type": "INGREDIENT",
    "action": "INCLUDE",
    "target_name": "Chai",
    "frequency_type": "DAILY",
    "enforcement": "REQUIRED",
    "meal_slot": "BREAKFAST",
    "is_active": True,
}

CHAI_SNACKS_RULE = {
    "target_type": "INGREDIENT",
    "action": "INCLUDE",
    "target_name": "Chai",
    "frequency_type": "DAILY",
    "enforcement": "REQUIRED",
    "meal_slot": "SNACKS",
    "is_active": True,
}

MORINGA_RULE = {
    "target_type": "INGREDIENT",
    "action": "INCLUDE",
    "target_name": "Moringa",
    "frequency_type": "TIMES_PER_WEEK",
    "frequency_count": 1,
    "enforcement": "PREFERRED",
    "is_active": True,
}

PANEER_EXCLUDE_RULE = {
    "target_type": "INGREDIENT",
    "action": "EXCLUDE",
    "target_name": "Paneer",
    "frequency_type": "NEVER",
    "enforcement": "REQUIRED",
    "is_active": True,
}

EGGS_RULE = {
    "target_type": "INGREDIENT",
    "action": "INCLUDE",
    "target_name": "Eggs",
    "frequency_type": "TIMES_PER_WEEK",
    "frequency_count": 4,
    "enforcement": "PREFERRED",
    "is_active": True,
}

CHICKEN_RULE = {
    "target_type": "INGREDIENT",
    "action": "INCLUDE",
    "target_name": "Chicken",
    "frequency_type": "TIMES_PER_WEEK",
    "frequency_count": 2,
    "enforcement": "PREFERRED",
    "is_active": True,
}

GREEN_LEAFY_GOAL = {
    "food_category": "LEAFY_GREENS",
    "weekly_target": 5,
    "enforcement": "PREFERRED",
    "is_active": True,
}
