"""Tests for build_user_response() in user_service.

Requirement: #35 - Service-level unit tests for user response building.
Verifies field mapping from User + UserPreferences models to UserResponse DTO.
This is a pure sync function — no DB session needed.
"""

import uuid

from app.models.user import User, UserPreferences
from app.schemas.user import UserResponse
from app.services.user_service import build_user_response


def _make_user(**overrides) -> User:
    """Create a User object with sensible defaults."""
    defaults = dict(
        id=str(uuid.uuid4()),
        firebase_uid=f"firebase-{uuid.uuid4().hex[:8]}",
        email="test@example.com",
        name="Test User",
        profile_picture_url=None,
        is_onboarded=True,
        is_active=True,
    )
    defaults.update(overrides)
    user = User(**defaults)
    # Initialize preferences to None by default (relationship not loaded via DB)
    if "preferences" not in overrides:
        user.preferences = None
    return user


def _make_preferences(user_id: str, **overrides) -> UserPreferences:
    """Create a UserPreferences object with sensible defaults."""
    defaults = dict(
        id=str(uuid.uuid4()),
        user_id=user_id,
        family_size=4,
        dietary_type="vegetarian",
        dietary_tags=["sattvic"],
        cuisine_preferences=["north", "south"],
        disliked_ingredients=["karela", "baingan"],
        cooking_time_preference="moderate",
        spice_level="medium",
        busy_days=["MONDAY", "WEDNESDAY"],
        weekday_cooking_time_minutes=30,
        weekend_cooking_time_minutes=60,
        items_per_meal=2,
        strict_allergen_mode=True,
        strict_dietary_mode=True,
        allow_recipe_repeat=False,
    )
    defaults.update(overrides)
    return UserPreferences(**defaults)


class TestBuildUserResponse:
    """Tests for build_user_response()."""

    def test_build_response_with_preferences(self):
        """Full response with all fields mapped correctly."""
        user = _make_user()
        prefs = _make_preferences(user.id)
        user.preferences = prefs

        result = build_user_response(user)

        assert isinstance(result, UserResponse)
        assert result.id == user.id
        assert result.email == "test@example.com"
        assert result.name == "Test User"
        assert result.is_onboarded is True
        assert result.preferences is not None
        assert result.preferences.household_size == 4
        assert result.preferences.dietary_type == "vegetarian"
        assert result.preferences.dietary_restrictions == ["sattvic"]
        assert result.preferences.cuisine_preferences == ["north", "south"]
        assert result.preferences.items_per_meal == 2

    def test_build_response_without_preferences(self):
        """preferences=None yields null preferences in response."""
        user = _make_user()
        user.preferences = None

        result = build_user_response(user)

        assert isinstance(result, UserResponse)
        assert result.id == user.id
        assert result.preferences is None

    def test_build_response_maps_fields(self):
        """Verify specific field name mappings between model and DTO."""
        user = _make_user(
            email="sharma@example.com",
            name="Ramesh Sharma",
            profile_picture_url="https://img.example.com/pic.jpg",
        )
        prefs = _make_preferences(
            user.id,
            family_size=3,
            spice_level="spicy",
            weekday_cooking_time_minutes=45,
            weekend_cooking_time_minutes=90,
            strict_allergen_mode=False,
            allow_recipe_repeat=True,
        )
        user.preferences = prefs

        result = build_user_response(user)

        # User field mappings
        assert result.email == "sharma@example.com"
        assert result.name == "Ramesh Sharma"
        assert result.profile_image_url == "https://img.example.com/pic.jpg"

        # Preferences field mappings (model name -> DTO name)
        assert result.preferences.household_size == 3  # family_size -> household_size
        assert result.preferences.spice_level == "spicy"
        assert result.preferences.weekday_cooking_time_minutes == 45
        assert result.preferences.weekend_cooking_time_minutes == 90
        assert result.preferences.strict_allergen_mode is False
        assert result.preferences.allow_recipe_repeat is True

    def test_build_response_none_optional_fields(self):
        """Optional fields like profile_picture_url=None handled gracefully."""
        user = _make_user(
            email=None,
            name=None,
            profile_picture_url=None,
        )
        prefs = _make_preferences(
            user.id,
            dietary_type=None,
            dietary_tags=None,
            cuisine_preferences=None,
            disliked_ingredients=None,
            cooking_time_preference=None,
            spice_level=None,
            busy_days=None,
            weekday_cooking_time_minutes=None,
            weekend_cooking_time_minutes=None,
        )
        user.preferences = prefs

        result = build_user_response(user)

        assert result.email == ""  # None -> "" via `user.email or ""`
        assert result.name == ""  # None -> "" via `user.name or ""`
        assert result.profile_image_url is None
        assert result.preferences.dietary_type is None
        assert result.preferences.dietary_restrictions == []  # None -> [] via `or []`
        assert result.preferences.cuisine_preferences == []
        assert result.preferences.disliked_ingredients == []
        assert (
            result.preferences.cooking_time_preference == "moderate"
        )  # None -> default
        assert result.preferences.spice_level == "medium"  # None -> default
        assert result.preferences.busy_days == []
