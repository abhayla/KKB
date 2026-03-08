"""Shared test data factories for RasoiAI backend tests.

Function-based factories with sensible defaults. All parameters can be
overridden via keyword arguments. No external dependencies (no factory_boy).

Usage:
    from tests.factories import make_user, make_preferences

    user = make_user(name="Custom Name")
    prefs = make_preferences(user.id, dietary_type="vegan")
"""

import uuid

from app.models.user import FamilyMember, User, UserPreferences


def make_user(**overrides) -> User:
    """Create a User instance with sensible defaults.

    All fields can be overridden. ID and firebase_uid are randomized
    to avoid collisions across tests.
    """
    user_id = overrides.pop("id", str(uuid.uuid4()))
    defaults = {
        "id": user_id,
        "firebase_uid": f"firebase-test-{user_id[:8]}",
        "email": f"test-{user_id[:8]}@example.com",
        "name": "Test User",
        "is_onboarded": True,
        "is_active": True,
    }
    defaults.update(overrides)
    return User(**defaults)


def make_preferences(user_id: str, **overrides) -> UserPreferences:
    """Create a UserPreferences instance for the given user.

    Provides minimal defaults; override as needed for specific tests.
    """
    defaults = {
        "id": str(uuid.uuid4()),
        "user_id": user_id,
        "dietary_type": "vegetarian",
        "family_size": 4,
    }
    defaults.update(overrides)
    return UserPreferences(**defaults)


def make_family_member(user_id: str, **overrides) -> FamilyMember:
    """Create a FamilyMember instance for the given user."""
    defaults = {
        "id": str(uuid.uuid4()),
        "user_id": user_id,
        "name": "Test Member",
        "age_group": "adult",
        "dietary_restrictions": [],
        "health_conditions": [],
    }
    defaults.update(overrides)
    return FamilyMember(**defaults)


def make_rule_payload(**overrides) -> dict:
    """Create a recipe rule API request payload."""
    defaults = {
        "target_type": "INGREDIENT",
        "action": "INCLUDE",
        "target_name": "Dal",
        "frequency_type": "DAILY",
        "enforcement": "PREFERRED",
        "is_active": True,
    }
    defaults.update(overrides)
    return defaults


def make_goal_payload(**overrides) -> dict:
    """Create a nutrition goal API request payload."""
    defaults = {
        "food_category": "LEAFY_GREENS",
        "weekly_target": 5,
        "enforcement": "PREFERRED",
        "is_active": True,
    }
    defaults.update(overrides)
    return defaults


from app.models.household import Household, HouseholdMember


def make_household(owner_id: str, **overrides) -> Household:
    """Create a Household instance with sensible defaults."""
    defaults = {
        "id": str(uuid.uuid4()),
        "name": "Test Household",
        "invite_code": None,
        "invite_code_expires_at": None,
        "owner_id": owner_id,
        "max_members": 6,
        "is_active": True,
    }
    defaults.update(overrides)
    return Household(**defaults)


def make_household_member(household_id: str, **overrides) -> HouseholdMember:
    """Create a HouseholdMember instance with sensible defaults."""
    from datetime import date as date_type

    defaults = {
        "id": str(uuid.uuid4()),
        "household_id": household_id,
        "user_id": None,
        "family_member_id": None,
        "role": "MEMBER",
        "can_edit_shared_plan": False,
        "is_temporary": False,
        "join_date": date_type.today(),
        "leave_date": None,
        "previous_household_id": None,
        "portion_size": "REGULAR",
        "active_meal_slots": None,
        "status": "ACTIVE",
    }
    defaults.update(overrides)
    return HouseholdMember(**defaults)
