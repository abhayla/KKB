"""Tests for items_per_meal preference persistence, retrieval, and prompt generation.

Validates GAP-004 fix: items_per_meal was silently dropped by the update handler
and never passed to the AI meal generation prompt.
"""

import pytest
import pytest_asyncio
from httpx import AsyncClient
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.user import User, UserPreferences


class TestItemsPerMealPersistence:
    """Test that items_per_meal is properly persisted via the preferences API."""

    @pytest.mark.asyncio
    async def test_update_items_per_meal(self, client: AsyncClient, test_user: User, db_session: AsyncSession):
        """PUT /users/preferences with items_per_meal should persist the value."""
        response = await client.put(
            "/api/v1/users/preferences",
            json={"items_per_meal": 3},
        )
        assert response.status_code == 200

        # Verify persisted in DB
        result = await db_session.execute(
            select(UserPreferences).where(UserPreferences.user_id == test_user.id)
        )
        prefs = result.scalar_one()
        assert prefs.items_per_meal == 3

    @pytest.mark.asyncio
    async def test_items_per_meal_default_value(self, client: AsyncClient, test_user: User, db_session: AsyncSession):
        """New preferences should default to items_per_meal=2."""
        # Create preferences with any field to trigger creation
        response = await client.put(
            "/api/v1/users/preferences",
            json={"household_size": 4},
        )
        assert response.status_code == 200

        result = await db_session.execute(
            select(UserPreferences).where(UserPreferences.user_id == test_user.id)
        )
        prefs = result.scalar_one()
        assert prefs.items_per_meal == 2  # default

    @pytest.mark.asyncio
    async def test_items_per_meal_in_response(self, client: AsyncClient, test_user: User):
        """GET /users/me should include items_per_meal in preferences."""
        # First set items_per_meal
        await client.put(
            "/api/v1/users/preferences",
            json={"items_per_meal": 3},
        )

        # Get user profile
        response = await client.get("/api/v1/users/me")
        assert response.status_code == 200
        data = response.json()
        assert data["preferences"]["items_per_meal"] == 3

    @pytest.mark.asyncio
    async def test_items_per_meal_validation_range(self, client: AsyncClient, test_user: User):
        """items_per_meal must be between 1 and 4 (schema validation)."""
        # Too low
        response = await client.put(
            "/api/v1/users/preferences",
            json={"items_per_meal": 0},
        )
        assert response.status_code == 422

        # Too high
        response = await client.put(
            "/api/v1/users/preferences",
            json={"items_per_meal": 5},
        )
        assert response.status_code == 422

    @pytest.mark.asyncio
    async def test_items_per_meal_update_preserves_other_fields(self, client: AsyncClient, test_user: User, db_session: AsyncSession):
        """Updating items_per_meal should not clobber other preference fields."""
        # Set initial preferences
        await client.put(
            "/api/v1/users/preferences",
            json={
                "household_size": 5,
                "spice_level": "spicy",
                "items_per_meal": 2,
            },
        )

        # Update only items_per_meal
        await client.put(
            "/api/v1/users/preferences",
            json={"items_per_meal": 3},
        )

        result = await db_session.execute(
            select(UserPreferences).where(UserPreferences.user_id == test_user.id)
        )
        prefs = result.scalar_one()
        assert prefs.items_per_meal == 3
        assert prefs.family_size == 5
        assert prefs.spice_level == "spicy"


class TestMealGenerationSettings:
    """Test that other meal generation settings are persisted."""

    @pytest.mark.asyncio
    async def test_strict_allergen_mode_persistence(self, client: AsyncClient, test_user: User, db_session: AsyncSession):
        """strict_allergen_mode should be updatable via preferences API."""
        response = await client.put(
            "/api/v1/users/preferences",
            json={"strict_allergen_mode": False},
        )
        assert response.status_code == 200

        result = await db_session.execute(
            select(UserPreferences).where(UserPreferences.user_id == test_user.id)
        )
        prefs = result.scalar_one()
        assert prefs.strict_allergen_mode is False

    @pytest.mark.asyncio
    async def test_allow_recipe_repeat_persistence(self, client: AsyncClient, test_user: User, db_session: AsyncSession):
        """allow_recipe_repeat should be updatable via preferences API."""
        response = await client.put(
            "/api/v1/users/preferences",
            json={"allow_recipe_repeat": True},
        )
        assert response.status_code == 200

        result = await db_session.execute(
            select(UserPreferences).where(UserPreferences.user_id == test_user.id)
        )
        prefs = result.scalar_one()
        assert prefs.allow_recipe_repeat is True


class TestPreferencesResponseFreshness:
    """Test that PUT /users/preferences response reflects the updated values (not stale cache).

    Validates fix for stale read-back bug caused by expire_on_commit=False in postgres.py.
    The db.refresh() calls after commit ensure the response contains fresh values.
    """

    @pytest.mark.asyncio
    async def test_put_response_contains_updated_items_per_meal(self, client: AsyncClient, test_user: User):
        """PUT items_per_meal=3 should return items_per_meal=3 in the response body."""
        response = await client.put(
            "/api/v1/users/preferences",
            json={"items_per_meal": 3},
        )
        assert response.status_code == 200
        data = response.json()
        assert data["preferences"]["items_per_meal"] == 3

    @pytest.mark.asyncio
    async def test_put_response_contains_updated_allow_recipe_repeat(self, client: AsyncClient, test_user: User):
        """PUT allow_recipe_repeat=True should return allow_recipe_repeat=True in the response body."""
        response = await client.put(
            "/api/v1/users/preferences",
            json={"allow_recipe_repeat": True},
        )
        assert response.status_code == 200
        data = response.json()
        assert data["preferences"]["allow_recipe_repeat"] is True

    @pytest.mark.asyncio
    async def test_put_response_multiple_fields_fresh(self, client: AsyncClient, test_user: User):
        """PUT multiple fields simultaneously should reflect all updated values in the response."""
        response = await client.put(
            "/api/v1/users/preferences",
            json={
                "items_per_meal": 4,
                "allow_recipe_repeat": True,
                "strict_dietary_mode": False,
            },
        )
        assert response.status_code == 200
        data = response.json()
        assert data["preferences"]["items_per_meal"] == 4
        assert data["preferences"]["allow_recipe_repeat"] is True
        assert data["preferences"]["strict_dietary_mode"] is False


class TestItemsPerMealPrompt:
    """Test that items_per_meal is included in AI generation prompt."""

    @pytest.mark.asyncio
    async def test_prompt_includes_items_per_meal(self):
        """The AI prompt should reflect the user's items_per_meal setting."""
        from app.services.ai_meal_service import AIMealService, UserPreferences
        from datetime import date

        service = AIMealService()
        prefs = UserPreferences(items_per_meal=3)

        prompt = service._build_prompt(prefs, {}, None, date(2026, 2, 9))
        assert "exactly 3 items" in prompt

    @pytest.mark.asyncio
    async def test_prompt_default_items_per_meal(self):
        """Default prompt should use 2 items per meal."""
        from app.services.ai_meal_service import AIMealService, UserPreferences
        from datetime import date

        service = AIMealService()
        prefs = UserPreferences()  # default items_per_meal=2

        prompt = service._build_prompt(prefs, {}, None, date(2026, 2, 9))
        assert "exactly 2 items" in prompt

    @pytest.mark.asyncio
    async def test_dataclass_loads_items_per_meal(self):
        """UserPreferences dataclass should accept items_per_meal."""
        from app.services.ai_meal_service import UserPreferences

        prefs = UserPreferences(items_per_meal=4)
        assert prefs.items_per_meal == 4

        prefs_default = UserPreferences()
        assert prefs_default.items_per_meal == 2
