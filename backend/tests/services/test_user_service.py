"""Tests for user_service.

Requirement: #35 - Service-level unit tests for user operations.
Covers build_user_response (sync, no DB) plus get_user_with_preferences
and update_user_preferences (async, DB-backed).
"""

import uuid
from datetime import datetime, timedelta, timezone

import pytest
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import ConflictError
from app.models.user import User, UserPreferences
from app.schemas.user import UserPreferencesUpdate, UserResponse
from app.services.user_service import (
    build_user_response,
    get_user_with_preferences,
    update_user_preferences,
)


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


# ==================== get_user_with_preferences ====================


class TestGetUserWithPreferences:
    """Tests for the DB-backed get_user_with_preferences()."""

    @pytest.mark.asyncio
    async def test_returns_response_when_preferences_exist(
        self, db_session: AsyncSession, test_user: User
    ):
        prefs = UserPreferences(
            id=str(uuid.uuid4()),
            user_id=test_user.id,
            family_size=3,
            dietary_type="vegan",
            cuisine_preferences=["north"],
        )
        db_session.add(prefs)
        await db_session.commit()

        result = await get_user_with_preferences(db_session, test_user)

        assert result.id == test_user.id
        assert result.preferences is not None
        assert result.preferences.household_size == 3
        assert result.preferences.dietary_type == "vegan"
        assert result.preferences.cuisine_preferences == ["north"]

    @pytest.mark.asyncio
    async def test_returns_response_with_null_preferences_when_none_exist(
        self, db_session: AsyncSession, test_user: User
    ):
        """A user without a UserPreferences row returns preferences=None."""
        # Explicitly confirm no preferences exist for this user.
        existing = (
            await db_session.execute(
                select(UserPreferences).where(UserPreferences.user_id == test_user.id)
            )
        ).scalar_one_or_none()
        assert existing is None

        result = await get_user_with_preferences(db_session, test_user)

        assert result.id == test_user.id
        assert result.preferences is None


# ==================== update_user_preferences ====================


class TestUpdateUserPreferences:
    """Tests for the DB-backed update_user_preferences()."""

    @pytest.mark.asyncio
    async def test_creates_preferences_row_if_missing(
        self, db_session: AsyncSession, test_user: User
    ):
        """A user with no UserPreferences row gets one created on first update."""
        update = UserPreferencesUpdate(household_size=5)
        result = await update_user_preferences(db_session, test_user, update)

        assert result.preferences is not None
        assert result.preferences.household_size == 5

        # Confirm a row was actually created in the DB.
        rows = (
            await db_session.execute(
                select(UserPreferences).where(UserPreferences.user_id == test_user.id)
            )
        ).scalars().all()
        assert len(rows) == 1

    @pytest.mark.asyncio
    async def test_updates_existing_preferences(
        self, db_session: AsyncSession, test_user: User
    ):
        """Existing preferences are updated in place, not duplicated."""
        prefs = UserPreferences(
            id=str(uuid.uuid4()),
            user_id=test_user.id,
            family_size=2,
            spice_level="mild",
        )
        db_session.add(prefs)
        await db_session.commit()

        update = UserPreferencesUpdate(household_size=6, spice_level="spicy")
        result = await update_user_preferences(db_session, test_user, update)

        assert result.preferences.household_size == 6
        assert result.preferences.spice_level == "spicy"

        rows = (
            await db_session.execute(
                select(UserPreferences).where(UserPreferences.user_id == test_user.id)
            )
        ).scalars().all()
        assert len(rows) == 1  # updated in place, no duplicate

    @pytest.mark.asyncio
    async def test_primary_diet_maps_to_dietary_type(
        self, db_session: AsyncSession, test_user: User
    ):
        """primary_diet field on the update schema maps to the dietary_type column."""
        update = UserPreferencesUpdate(primary_diet="jain")
        result = await update_user_preferences(db_session, test_user, update)

        assert result.preferences.dietary_type == "jain"

    @pytest.mark.asyncio
    async def test_partial_update_preserves_unspecified_fields(
        self, db_session: AsyncSession, test_user: User
    ):
        """Only fields explicitly set on the update are changed; others are preserved."""
        prefs = UserPreferences(
            id=str(uuid.uuid4()),
            user_id=test_user.id,
            family_size=4,
            spice_level="medium",
            cooking_time_preference="moderate",
        )
        db_session.add(prefs)
        await db_session.commit()

        update = UserPreferencesUpdate(spice_level="spicy")
        result = await update_user_preferences(db_session, test_user, update)

        assert result.preferences.spice_level == "spicy"
        # Untouched fields stay the same.
        assert result.preferences.household_size == 4
        assert result.preferences.cooking_time_preference == "moderate"

    @pytest.mark.asyncio
    async def test_marks_user_onboarded_on_first_update(
        self, db_session: AsyncSession, test_user: User
    ):
        """A user that was not onboarded becomes onboarded after preferences update."""
        test_user.is_onboarded = False
        await db_session.commit()

        update = UserPreferencesUpdate(household_size=3)
        await update_user_preferences(db_session, test_user, update)

        await db_session.refresh(test_user)
        assert test_user.is_onboarded is True

    @pytest.mark.asyncio
    async def test_stale_update_raises_conflict_error(
        self, db_session: AsyncSession, test_user: User
    ):
        """An update with client updated_at older than server's must raise ConflictError."""
        now = datetime.now(timezone.utc)
        test_user.preferences_updated_at = now
        prefs = UserPreferences(
            id=str(uuid.uuid4()), user_id=test_user.id, family_size=2
        )
        db_session.add(prefs)
        await db_session.commit()

        # Client thinks preferences were last updated 1 hour ago — stale.
        stale = (now - timedelta(hours=1)).isoformat()
        update = UserPreferencesUpdate(household_size=7, updated_at=stale)

        with pytest.raises(ConflictError):
            await update_user_preferences(db_session, test_user, update)

    @pytest.mark.asyncio
    async def test_updated_at_timestamp_is_recorded(
        self, db_session: AsyncSession, test_user: User
    ):
        """A successful update must record user.preferences_updated_at."""
        test_user.preferences_updated_at = None
        await db_session.commit()

        update = UserPreferencesUpdate(household_size=3)
        await update_user_preferences(db_session, test_user, update)

        await db_session.refresh(test_user)
        assert test_user.preferences_updated_at is not None
