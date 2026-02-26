"""Tests for PreferenceUpdateService edge cases.

Run with: pytest tests/test_preference_service.py -v
"""

import pytest
import asyncio
from datetime import datetime, timezone
from unittest.mock import AsyncMock, MagicMock, patch

from app.services.preference_update_service import (
    PreferenceUpdateService,
    UpdateResult,
    RuleAction,
    RuleType,
    Frequency,
    Severity,
)


class TestUpdateRecipeRule:
    """Tests for update_recipe_rule method."""

    @pytest.fixture
    def service(self):
        """Create service with mocked repository."""
        service = PreferenceUpdateService()
        service.user_repo = MagicMock()
        return service

    @pytest.fixture
    def empty_prefs(self):
        """Empty preferences dict."""
        return {
            "recipe_rules": {"include": [], "exclude": []},
            "allergies": [],
            "dislikes": [],
        }

    @pytest.mark.asyncio
    async def test_add_include_rule_success(self, service, empty_prefs):
        """Test adding an INCLUDE rule successfully."""
        service.user_repo.get_preferences = AsyncMock(return_value=empty_prefs)
        service.user_repo.save_preferences = AsyncMock(return_value=empty_prefs)

        result = await service.update_recipe_rule(
            user_id="test-user",
            action="ADD",
            rule_type="INCLUDE",
            target="Chai",
            frequency="DAILY",
            meal_slots=["BREAKFAST"],
        )

        assert result.success is True
        assert "INCLUDE" in result.message
        assert "Chai" in result.message
        service.user_repo.save_preferences.assert_called_once()

    @pytest.mark.asyncio
    async def test_add_exclude_rule_success(self, service, empty_prefs):
        """Test adding an EXCLUDE rule successfully."""
        service.user_repo.get_preferences = AsyncMock(return_value=empty_prefs)
        service.user_repo.save_preferences = AsyncMock(return_value=empty_prefs)

        result = await service.update_recipe_rule(
            user_id="test-user",
            action="ADD",
            rule_type="EXCLUDE",
            target="Karela",
            frequency="NEVER",
            reason="dislike",
        )

        assert result.success is True
        assert "EXCLUDE" in result.message
        assert "Karela" in result.message

    @pytest.mark.asyncio
    async def test_add_duplicate_rule_fails(self, service):
        """Test that adding a duplicate rule fails."""
        prefs = {
            "recipe_rules": {
                "include": [{"target": "Chai", "type": "INCLUDE", "frequency": "DAILY"}],
                "exclude": [],
            }
        }
        service.user_repo.get_preferences = AsyncMock(return_value=prefs)

        result = await service.update_recipe_rule(
            user_id="test-user",
            action="ADD",
            rule_type="INCLUDE",
            target="Chai",  # Duplicate
            frequency="WEEKLY",
        )

        assert result.success is False
        assert "already exists" in result.message
        assert result.conflict is not None
        assert result.conflict["conflict_type"] == "duplicate"

    @pytest.mark.asyncio
    async def test_add_conflicting_rule_detected(self, service):
        """Test that adding an INCLUDE when EXCLUDE exists is detected."""
        prefs = {
            "recipe_rules": {
                "include": [],
                "exclude": [{"target": "Mushroom", "type": "EXCLUDE", "frequency": "NEVER"}],
            }
        }
        service.user_repo.get_preferences = AsyncMock(return_value=prefs)

        result = await service.update_recipe_rule(
            user_id="test-user",
            action="ADD",
            rule_type="INCLUDE",
            target="Mushroom",  # Conflicts with EXCLUDE
            frequency="WEEKLY",
        )

        assert result.success is False
        assert "Conflict" in result.message
        assert result.conflict is not None
        assert result.conflict["conflict_type"] == "include_exclude"

    @pytest.mark.asyncio
    async def test_remove_nonexistent_rule_fails(self, service, empty_prefs):
        """Test that removing a non-existent rule fails."""
        service.user_repo.get_preferences = AsyncMock(return_value=empty_prefs)

        result = await service.update_recipe_rule(
            user_id="test-user",
            action="REMOVE",
            rule_type="INCLUDE",
            target="NonExistent",
        )

        assert result.success is False
        assert "no" in result.message.lower() and "rule" in result.message.lower()

    @pytest.mark.asyncio
    async def test_modify_existing_rule(self, service):
        """Test modifying an existing rule."""
        prefs = {
            "recipe_rules": {
                "include": [{"target": "Chai", "type": "INCLUDE", "frequency": "DAILY", "meal_slot": ["BREAKFAST"]}],
                "exclude": [],
            }
        }
        service.user_repo.get_preferences = AsyncMock(return_value=prefs)
        service.user_repo.save_preferences = AsyncMock(return_value=prefs)

        result = await service.update_recipe_rule(
            user_id="test-user",
            action="MODIFY",
            rule_type="INCLUDE",
            target="Chai",
            frequency="WEEKLY",
            meal_slots=["BREAKFAST", "SNACKS"],
        )

        assert result.success is True
        assert "Modified" in result.message

    @pytest.mark.asyncio
    async def test_invalid_action_fails(self, service, empty_prefs):
        """Test that an invalid action fails."""
        service.user_repo.get_preferences = AsyncMock(return_value=empty_prefs)

        result = await service.update_recipe_rule(
            user_id="test-user",
            action="INVALID",
            rule_type="INCLUDE",
            target="Test",
        )

        assert result.success is False
        assert "Invalid action" in result.message

    @pytest.mark.asyncio
    async def test_case_insensitive_target_matching(self, service):
        """Test that target matching is case-insensitive."""
        prefs = {
            "recipe_rules": {
                "include": [{"target": "Chai", "type": "INCLUDE"}],
                "exclude": [],
            }
        }
        service.user_repo.get_preferences = AsyncMock(return_value=prefs)
        service.user_repo.save_preferences = AsyncMock(return_value=prefs)

        # Try to remove with different case
        result = await service.update_recipe_rule(
            user_id="test-user",
            action="REMOVE",
            rule_type="INCLUDE",
            target="CHAI",  # Different case
        )

        assert result.success is True

    @pytest.mark.asyncio
    async def test_handles_missing_preferences(self, service):
        """Test handling when user has no preferences."""
        service.user_repo.get_preferences = AsyncMock(return_value=None)
        service.user_repo.save_preferences = AsyncMock(return_value={})

        result = await service.update_recipe_rule(
            user_id="test-user",
            action="ADD",
            rule_type="INCLUDE",
            target="Chai",
        )

        assert result.success is True


class TestUpdateAllergy:
    """Tests for update_allergy method."""

    @pytest.fixture
    def service(self):
        service = PreferenceUpdateService()
        service.user_repo = MagicMock()
        return service

    @pytest.mark.asyncio
    async def test_add_allergy_success(self, service):
        """Test adding an allergy."""
        prefs = {"allergies": []}
        service.user_repo.get_preferences = AsyncMock(return_value=prefs)
        service.user_repo.save_preferences = AsyncMock(return_value=prefs)

        result = await service.update_allergy(
            user_id="test-user",
            action="ADD",
            ingredient="peanuts",
            severity="SEVERE",
        )

        assert result.success is True
        assert "peanuts" in result.message.lower()

    @pytest.mark.asyncio
    async def test_add_duplicate_allergy_fails(self, service):
        """Test that adding a duplicate allergy fails."""
        prefs = {"allergies": [{"ingredient": "peanuts", "severity": "SEVERE"}]}
        service.user_repo.get_preferences = AsyncMock(return_value=prefs)

        result = await service.update_allergy(
            user_id="test-user",
            action="ADD",
            ingredient="peanuts",
        )

        assert result.success is False
        assert "already" in result.message.lower()

    @pytest.mark.asyncio
    async def test_remove_nonexistent_allergy_fails(self, service):
        """Test removing a non-existent allergy fails."""
        prefs = {"allergies": []}
        service.user_repo.get_preferences = AsyncMock(return_value=prefs)

        result = await service.update_allergy(
            user_id="test-user",
            action="REMOVE",
            ingredient="peanuts",
        )

        assert result.success is False
        assert "no" in result.message.lower() and "allergy" in result.message.lower()

    @pytest.mark.asyncio
    async def test_default_severity(self, service):
        """Test that default severity is applied."""
        prefs = {"allergies": []}
        service.user_repo.get_preferences = AsyncMock(return_value=prefs)
        service.user_repo.save_preferences = AsyncMock(return_value=prefs)

        result = await service.update_allergy(
            user_id="test-user",
            action="ADD",
            ingredient="shellfish",
            # No severity specified
        )

        assert result.success is True
        # Check that save was called with default severity
        call_args = service.user_repo.save_preferences.call_args
        saved_prefs = call_args[0][1]
        assert saved_prefs["allergies"][0]["severity"] == "MODERATE"


class TestUpdateDislike:
    """Tests for update_dislike method."""

    @pytest.fixture
    def service(self):
        service = PreferenceUpdateService()
        service.user_repo = MagicMock()
        return service

    @pytest.mark.asyncio
    async def test_add_dislike_success(self, service):
        """Test adding a dislike."""
        prefs = {"dislikes": []}
        service.user_repo.get_preferences = AsyncMock(return_value=prefs)
        service.user_repo.save_preferences = AsyncMock(return_value=prefs)

        result = await service.update_dislike(
            user_id="test-user",
            action="ADD",
            ingredient="bhindi",
        )

        assert result.success is True
        assert "bhindi" in result.message.lower()

    @pytest.mark.asyncio
    async def test_add_duplicate_dislike_fails(self, service):
        """Test that adding a duplicate dislike fails."""
        prefs = {"disliked_ingredients": ["bhindi"]}  # Correct key
        service.user_repo.get_preferences = AsyncMock(return_value=prefs)

        result = await service.update_dislike(
            user_id="test-user",
            action="ADD",
            ingredient="bhindi",
        )

        assert result.success is False
        assert "already" in result.message.lower()

    @pytest.mark.asyncio
    async def test_case_insensitive_dislike_check(self, service):
        """Test that dislike check is case-insensitive."""
        prefs = {"disliked_ingredients": ["Bhindi"]}  # Correct key
        service.user_repo.get_preferences = AsyncMock(return_value=prefs)

        result = await service.update_dislike(
            user_id="test-user",
            action="ADD",
            ingredient="BHINDI",
        )

        assert result.success is False


class TestUpdatePreference:
    """Tests for update_preference method."""

    @pytest.fixture
    def service(self):
        service = PreferenceUpdateService()
        service.user_repo = MagicMock()
        return service

    @pytest.mark.asyncio
    async def test_set_cooking_time_weekday(self, service):
        """Test setting weekday cooking time."""
        prefs = {}
        service.user_repo.get_preferences = AsyncMock(return_value=prefs)
        service.user_repo.save_preferences = AsyncMock(return_value=prefs)

        result = await service.update_preference(
            user_id="test-user",
            preference_type="cooking_time",
            action="SET",
            value="weekday:30",
        )

        assert result.success is True
        assert "30" in result.message

    @pytest.mark.asyncio
    async def test_set_cooking_time_weekend(self, service):
        """Test setting weekend cooking time."""
        prefs = {}
        service.user_repo.get_preferences = AsyncMock(return_value=prefs)
        service.user_repo.save_preferences = AsyncMock(return_value=prefs)

        result = await service.update_preference(
            user_id="test-user",
            preference_type="cooking_time",
            action="SET",
            value="weekend:60",
        )

        assert result.success is True
        assert "60" in result.message

    @pytest.mark.asyncio
    async def test_add_dietary_tag(self, service):
        """Test adding a dietary tag."""
        prefs = {"dietary_tags": []}
        service.user_repo.get_preferences = AsyncMock(return_value=prefs)
        service.user_repo.save_preferences = AsyncMock(return_value=prefs)

        result = await service.update_preference(
            user_id="test-user",
            preference_type="dietary_tags",
            action="ADD",
            value="vegetarian",
        )

        assert result.success is True

    @pytest.mark.asyncio
    async def test_add_busy_day(self, service):
        """Test adding a busy day."""
        prefs = {"busy_days": []}
        service.user_repo.get_preferences = AsyncMock(return_value=prefs)
        service.user_repo.save_preferences = AsyncMock(return_value=prefs)

        result = await service.update_preference(
            user_id="test-user",
            preference_type="busy_days",
            action="ADD",
            value="MONDAY",
        )

        assert result.success is True

    @pytest.mark.asyncio
    async def test_invalid_cooking_time_format(self, service):
        """Test invalid cooking time format raises error."""
        prefs = {}
        service.user_repo.get_preferences = AsyncMock(return_value=prefs)

        # Invalid format (not a number and no colon) should raise ValueError
        with pytest.raises(ValueError):
            await service.update_preference(
                user_id="test-user",
                preference_type="cooking_time",
                action="SET",
                value="invalid_format",
            )


class TestUndoLastChange:
    """Tests for undo_last_change method."""

    @pytest.fixture
    def service(self):
        service = PreferenceUpdateService()
        service.user_repo = MagicMock()
        return service

    @pytest.mark.asyncio
    async def test_undo_with_no_history(self, service):
        """Test undo when there's no change history."""
        prefs = {}  # No last_change
        service.user_repo.get_preferences = AsyncMock(return_value=prefs)

        result = await service.undo_last_change(user_id="test-user")

        assert result.success is False
        assert "no" in result.message.lower() and "undo" in result.message.lower()

    @pytest.mark.asyncio
    async def test_undo_recipe_rule_change(self, service):
        """Test undoing a recipe rule change."""
        prefs = {
            "recipe_rules": {"include": [{"target": "Chai"}], "exclude": []},
            "last_change": {
                "action": "ADD_RECIPE_RULE",
                "previous_state": {
                    "type": "recipe_rules",
                    "data": {"include": [], "exclude": []},
                },
                "timestamp": datetime.now(timezone.utc).isoformat(),
            },
        }
        service.user_repo.get_preferences = AsyncMock(return_value=prefs)
        service.user_repo.save_preferences = AsyncMock(return_value=prefs)

        result = await service.undo_last_change(user_id="test-user")

        assert result.success is True
        assert "undo" in result.message.lower()


class TestShowConfig:
    """Tests for show_config method."""

    @pytest.fixture
    def service(self):
        service = PreferenceUpdateService()
        service.user_repo = MagicMock()
        return service

    @pytest.mark.asyncio
    async def test_show_all_config(self, service):
        """Test showing all configuration."""
        prefs = {
            "recipe_rules": {
                "include": [{"target": "Chai", "frequency": "DAILY", "meal_slot": ["BREAKFAST"]}],
                "exclude": [{"target": "Karela", "reason": "dislike"}],
            },
            "allergies": [{"ingredient": "peanuts", "severity": "SEVERE"}],
            "dislikes": ["bhindi"],
        }
        service.user_repo.get_preferences = AsyncMock(return_value=prefs)

        result = await service.show_config(user_id="test-user", section="all")

        assert result.success is True

    @pytest.mark.asyncio
    async def test_show_rules_only(self, service):
        """Test showing only rules section."""
        prefs = {
            "recipe_rules": {
                "include": [{"target": "Chai"}],
                "exclude": [],
            },
        }
        service.user_repo.get_preferences = AsyncMock(return_value=prefs)

        result = await service.show_config(user_id="test-user", section="rules")

        assert result.success is True

    @pytest.mark.asyncio
    async def test_show_empty_config(self, service):
        """Test showing empty configuration."""
        service.user_repo.get_preferences = AsyncMock(return_value=None)

        result = await service.show_config(user_id="test-user", section="all")

        assert result.success is True
