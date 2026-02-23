"""Tests for user deletion and data export (GDPR compliance).

Tests soft-delete, data export, and the DELETE /me and GET /me/export endpoints.
"""

import uuid

import pytest
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.chat import ChatMessage
from app.models.recipe_rule import RecipeRule
from app.models.user import FamilyMember, User, UserPreferences


class TestUserDeletionService:
    """Test the user deletion service directly."""

    async def test_soft_delete_sets_inactive(self, db_session: AsyncSession, test_user: User):
        """Soft delete should set is_active=False."""
        from app.services.user_deletion_service import soft_delete_user

        result = await soft_delete_user(db_session, test_user.id)

        assert result["message"] == "Account scheduled for deletion"
        assert "deletion_date" in result

        # Verify user is inactive
        db_result = await db_session.execute(
            select(User).where(User.id == test_user.id)
        )
        user = db_result.scalar_one()
        assert user.is_active is False
        assert user.deleted_at is not None

    async def test_soft_delete_nonexistent_user(self, db_session: AsyncSession):
        """Soft delete of nonexistent user should raise ValueError."""
        from app.services.user_deletion_service import soft_delete_user

        with pytest.raises(ValueError, match="User not found"):
            await soft_delete_user(db_session, "nonexistent-id")

    async def test_export_user_data_basic(self, db_session: AsyncSession, test_user: User):
        """Export should return user profile data."""
        from app.services.user_deletion_service import export_user_data

        data = await export_user_data(db_session, test_user.id)

        assert data["profile"]["id"] == test_user.id
        assert data["profile"]["email"] == test_user.email
        assert data["profile"]["name"] == test_user.name
        assert "preferences" in data
        assert "family_members" in data
        assert "meal_plans" in data
        assert "recipe_rules" in data
        assert "chat_messages" in data

    async def test_export_includes_preferences(self, db_session: AsyncSession, test_user: User):
        """Export should include preferences when they exist."""
        from app.services.user_deletion_service import export_user_data

        # Create preferences
        prefs = UserPreferences(
            user_id=test_user.id,
            dietary_type="vegetarian",
            family_size=3,
            spice_level="medium",
        )
        db_session.add(prefs)
        await db_session.commit()

        data = await export_user_data(db_session, test_user.id)

        assert data["preferences"] is not None
        assert data["preferences"]["dietary_type"] == "vegetarian"
        assert data["preferences"]["family_size"] == 3

    async def test_export_includes_family_members(self, db_session: AsyncSession, test_user: User):
        """Export should include family members."""
        from app.services.user_deletion_service import export_user_data

        fm = FamilyMember(
            user_id=test_user.id,
            name="Test Child",
            age_group="child",
        )
        db_session.add(fm)
        await db_session.commit()

        data = await export_user_data(db_session, test_user.id)

        assert len(data["family_members"]) == 1
        assert data["family_members"][0]["name"] == "Test Child"

    async def test_export_includes_recipe_rules(self, db_session: AsyncSession, test_user: User):
        """Export should include recipe rules."""
        from app.services.user_deletion_service import export_user_data

        rule = RecipeRule(
            user_id=test_user.id,
            target_type="INGREDIENT",
            action="EXCLUDE",
            target_name="mushroom",
            frequency_type="NEVER",
        )
        db_session.add(rule)
        await db_session.commit()

        data = await export_user_data(db_session, test_user.id)

        assert len(data["recipe_rules"]) == 1
        assert data["recipe_rules"][0]["target_name"] == "mushroom"

    async def test_export_nonexistent_user(self, db_session: AsyncSession):
        """Export of nonexistent user should raise ValueError."""
        from app.services.user_deletion_service import export_user_data

        with pytest.raises(ValueError, match="User not found"):
            await export_user_data(db_session, "nonexistent-id")


class TestUserDeletionAPI:
    """Test the DELETE /me and GET /me/export endpoints."""

    async def test_delete_me_endpoint(self, client):
        """DELETE /api/v1/users/me should soft-delete the user."""
        response = await client.delete("/api/v1/users/me")
        assert response.status_code == 200

        data = response.json()
        assert data["message"] == "Account scheduled for deletion"
        assert "deletion_date" in data

    async def test_export_me_endpoint(self, client):
        """GET /api/v1/users/me/export should return user data."""
        response = await client.get("/api/v1/users/me/export")
        assert response.status_code == 200

        data = response.json()
        assert "profile" in data
        assert "preferences" in data
        assert "family_members" in data
        assert "meal_plans" in data
        assert "recipe_rules" in data
        assert "chat_messages" in data

    async def test_export_includes_all_sections(self, client):
        """Export should include all required data sections."""
        response = await client.get("/api/v1/users/me/export")
        data = response.json()

        required_sections = [
            "profile", "preferences", "family_members", "meal_plans",
            "recipe_rules", "nutrition_goals", "chat_messages",
            "grocery_lists", "notifications_count",
        ]
        for section in required_sections:
            assert section in data, f"Missing section: {section}"
