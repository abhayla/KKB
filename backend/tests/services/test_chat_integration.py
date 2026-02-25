"""Integration tests for chat tool flow.

Tests the interaction between chat assistant, Claude API tool calling,
and preference update service.

Run with: pytest tests/test_chat_integration.py -v
"""

import pytest
from unittest.mock import AsyncMock, MagicMock, patch
from datetime import datetime, timezone

from app.ai.chat_assistant import process_chat_message, _execute_tool
from app.ai.claude_client import ChatCompletionResult, ToolCall
from app.ai.tools import PREFERENCE_TOOLS, CONFIG_CHAT_SYSTEM_PROMPT, format_config_for_display
from app.services.preference_update_service import PreferenceUpdateService, UpdateResult


class TestToolDefinitions:
    """Tests for tool definitions."""

    def test_all_tools_present(self):
        """Test that all required tools are defined."""
        tool_names = [t["name"] for t in PREFERENCE_TOOLS]

        expected_tools = [
            "update_recipe_rule",
            "update_allergy",
            "update_dislike",
            "update_preference",
            "undo_last_change",
            "show_config",
        ]

        for tool in expected_tools:
            assert tool in tool_names, f"Missing tool: {tool}"

    def test_tool_schemas_valid(self):
        """Test that all tool schemas have required fields."""
        for tool in PREFERENCE_TOOLS:
            assert "name" in tool
            assert "description" in tool
            assert "input_schema" in tool
            assert tool["input_schema"]["type"] == "object"

    def test_update_recipe_rule_schema(self):
        """Test update_recipe_rule schema has correct parameters."""
        tool = next(t for t in PREFERENCE_TOOLS if t["name"] == "update_recipe_rule")
        schema = tool["input_schema"]

        assert "action" in schema["properties"]
        assert "rule_type" in schema["properties"]
        assert "target" in schema["properties"]
        assert "frequency" in schema["properties"]
        assert "meal_slots" in schema["properties"]

        assert set(schema["required"]) == {"action", "rule_type", "target"}

    def test_system_prompt_contains_key_info(self):
        """Test that system prompt contains important context."""
        assert "RasoiAI" in CONFIG_CHAT_SYSTEM_PROMPT
        assert "INCLUDE" in CONFIG_CHAT_SYSTEM_PROMPT
        assert "EXCLUDE" in CONFIG_CHAT_SYSTEM_PROMPT
        assert "allergy" in CONFIG_CHAT_SYSTEM_PROMPT.lower()
        assert "dislike" in CONFIG_CHAT_SYSTEM_PROMPT.lower()


class TestFormatConfigForDisplay:
    """Tests for format_config_for_display function."""

    def test_format_empty_config(self):
        """Test formatting empty config."""
        result = format_config_for_display({})
        assert result == "No configuration set."

    def test_format_include_rules(self):
        """Test formatting include rules."""
        config = {
            "recipe_rules": {
                "include": [
                    {"target": "Chai", "frequency": "DAILY", "meal_slots": ["BREAKFAST"]}
                ],
                "exclude": [],
            }
        }
        result = format_config_for_display(config)

        assert "INCLUDE Rules" in result
        assert "Chai" in result
        assert "DAILY" in result

    def test_format_exclude_rules(self):
        """Test formatting exclude rules."""
        config = {
            "recipe_rules": {
                "include": [],
                "exclude": [
                    {"target": "Karela", "reason": "dislike"}
                ],
            }
        }
        result = format_config_for_display(config)

        assert "EXCLUDE Rules" in result
        assert "Karela" in result

    def test_format_allergies(self):
        """Test formatting allergies."""
        config = {
            "allergies": [
                {"ingredient": "peanuts", "severity": "SEVERE"}
            ]
        }
        result = format_config_for_display(config)

        assert "Allergies" in result
        assert "peanuts" in result
        assert "SEVERE" in result

    def test_format_dislikes(self):
        """Test formatting dislikes."""
        config = {
            "dislikes": ["bhindi", "karela"]
        }
        result = format_config_for_display(config)

        assert "Dislikes" in result
        assert "bhindi" in result

    def test_format_preferences(self):
        """Test formatting preferences."""
        config = {
            "preferences": {
                "dietary_tags": ["vegetarian"],
                "cuisine_preferences": ["north", "south"],
                "spice_level": "medium",
                "cooking_time": {"weekday": 30, "weekend": 60},
                "busy_days": ["MONDAY"],
            }
        }
        result = format_config_for_display(config)

        assert "vegetarian" in result
        assert "north" in result
        assert "30" in result
        assert "60" in result


class TestExecuteTool:
    """Tests for _execute_tool function."""

    @pytest.fixture
    def mock_pref_service(self):
        """Create mock preference service."""
        service = MagicMock(spec=PreferenceUpdateService)
        return service

    @pytest.mark.asyncio
    async def test_execute_update_recipe_rule(self, mock_pref_service):
        """Test executing update_recipe_rule tool."""
        mock_pref_service.update_recipe_rule = AsyncMock(
            return_value=UpdateResult(
                success=True,
                message="Added INCLUDE rule: Chai"
            )
        )

        result = await _execute_tool(
            tool_name="update_recipe_rule",
            tool_input={
                "action": "ADD",
                "rule_type": "INCLUDE",
                "target": "Chai",
                "frequency": "DAILY",
                "meal_slots": ["BREAKFAST"],
            },
            user_id="test-user",
            pref_service=mock_pref_service,
        )

        assert "Added INCLUDE rule" in result
        mock_pref_service.update_recipe_rule.assert_called_once()

    @pytest.mark.asyncio
    async def test_execute_update_allergy(self, mock_pref_service):
        """Test executing update_allergy tool."""
        mock_pref_service.update_allergy = AsyncMock(
            return_value=UpdateResult(
                success=True,
                message="Added allergy: peanuts"
            )
        )

        result = await _execute_tool(
            tool_name="update_allergy",
            tool_input={
                "action": "ADD",
                "ingredient": "peanuts",
                "severity": "SEVERE",
            },
            user_id="test-user",
            pref_service=mock_pref_service,
        )

        assert "Added allergy" in result

    @pytest.mark.asyncio
    async def test_execute_update_dislike(self, mock_pref_service):
        """Test executing update_dislike tool."""
        mock_pref_service.update_dislike = AsyncMock(
            return_value=UpdateResult(
                success=True,
                message="Added to dislikes: bhindi"
            )
        )

        result = await _execute_tool(
            tool_name="update_dislike",
            tool_input={
                "action": "ADD",
                "ingredient": "bhindi",
            },
            user_id="test-user",
            pref_service=mock_pref_service,
        )

        assert "Added to dislikes" in result

    @pytest.mark.asyncio
    async def test_execute_undo(self, mock_pref_service):
        """Test executing undo_last_change tool."""
        mock_pref_service.undo_last_change = AsyncMock(
            return_value=UpdateResult(
                success=True,
                message="Undo successful"
            )
        )

        result = await _execute_tool(
            tool_name="undo_last_change",
            tool_input={},
            user_id="test-user",
            pref_service=mock_pref_service,
        )

        assert "Undo successful" in result

    @pytest.mark.asyncio
    async def test_execute_show_config(self, mock_pref_service):
        """Test executing show_config tool."""
        mock_pref_service.show_config = AsyncMock(
            return_value=UpdateResult(
                success=True,
                message="Current configuration: ..."
            )
        )

        result = await _execute_tool(
            tool_name="show_config",
            tool_input={"section": "all"},
            user_id="test-user",
            pref_service=mock_pref_service,
        )

        assert "Current configuration" in result

    @pytest.mark.asyncio
    async def test_execute_unknown_tool(self, mock_pref_service):
        """Test executing unknown tool returns error."""
        result = await _execute_tool(
            tool_name="unknown_tool",
            tool_input={},
            user_id="test-user",
            pref_service=mock_pref_service,
        )

        assert "Error" in result or "Unknown tool" in result

    @pytest.mark.asyncio
    async def test_execute_tool_failure(self, mock_pref_service):
        """Test tool execution failure is handled."""
        mock_pref_service.update_recipe_rule = AsyncMock(
            return_value=UpdateResult(
                success=False,
                message="Conflict detected"
            )
        )

        result = await _execute_tool(
            tool_name="update_recipe_rule",
            tool_input={
                "action": "ADD",
                "rule_type": "INCLUDE",
                "target": "Chai",
            },
            user_id="test-user",
            pref_service=mock_pref_service,
        )

        assert "Error" in result
        assert "Conflict" in result

    @pytest.mark.asyncio
    async def test_execute_tool_exception(self, mock_pref_service):
        """Test tool execution exception is handled."""
        mock_pref_service.update_recipe_rule = AsyncMock(
            side_effect=Exception("Database error")
        )

        result = await _execute_tool(
            tool_name="update_recipe_rule",
            tool_input={
                "action": "ADD",
                "rule_type": "INCLUDE",
                "target": "Chai",
            },
            user_id="test-user",
            pref_service=mock_pref_service,
        )

        assert "Error" in result
        assert "Database error" in result


class TestChatCompletionResult:
    """Tests for ChatCompletionResult dataclass."""

    def test_no_tool_calls(self):
        """Test result with no tool calls."""
        result = ChatCompletionResult(text="Hello!")

        assert result.text == "Hello!"
        assert not result.has_tool_calls
        assert result.tool_calls == []

    def test_with_tool_calls(self):
        """Test result with tool calls."""
        result = ChatCompletionResult(
            text="I'll add that for you.",
            tool_calls=[
                ToolCall(
                    id="tool_123",
                    name="update_recipe_rule",
                    input={"action": "ADD", "rule_type": "INCLUDE", "target": "Chai"},
                )
            ],
            stop_reason="tool_use",
        )

        assert result.has_tool_calls
        assert len(result.tool_calls) == 1
        assert result.tool_calls[0].name == "update_recipe_rule"


class TestProcessChatMessage:
    """Tests for process_chat_message function."""

    @pytest.fixture
    def mock_repos_and_services(self):
        """Create mock repositories and services."""
        with patch('app.ai.chat_assistant.ChatRepository') as mock_chat_repo, \
             patch('app.ai.chat_assistant.UserRepository') as mock_user_repo, \
             patch('app.ai.chat_assistant.PreferenceUpdateService') as mock_pref_service, \
             patch('app.ai.chat_assistant.generate_with_tools') as mock_generate:

            # Configure mocks
            mock_chat_repo.return_value.get_context_for_claude = AsyncMock(return_value=[])
            mock_chat_repo.return_value.save_message = AsyncMock(return_value={
                "id": "msg_123",
                "role": "assistant",
                "content": "Hello!",
                "created_at": datetime.now(timezone.utc),
            })

            mock_user_repo.return_value.get_preferences = AsyncMock(return_value={})

            yield {
                "chat_repo": mock_chat_repo,
                "user_repo": mock_user_repo,
                "pref_service": mock_pref_service,
                "generate": mock_generate,
            }

    @pytest.mark.asyncio
    async def test_simple_chat_response(self, mock_repos_and_services):
        """Test simple chat response without tool calls."""
        mock_repos_and_services["generate"].return_value = ChatCompletionResult(
            text="Namaste! How can I help you today?",
            stop_reason="end_turn",
        )

        result = await process_chat_message(
            user_id="test-user",
            message="Hello!",
        )

        assert result.message.content == "Namaste! How can I help you today?"
        assert result.message.role == "assistant"

    @pytest.mark.asyncio
    async def test_fallback_on_error(self, mock_repos_and_services):
        """Test fallback response when API fails."""
        mock_repos_and_services["generate"].side_effect = Exception("API Error")

        result = await process_chat_message(
            user_id="test-user",
            message="What is paneer?",
        )

        # Should get fallback response, not error
        assert result.message.content is not None
        assert result.message.role == "assistant"


class TestToolCallPatterns:
    """Test tool call patterns that Claude should recognize."""

    def test_include_patterns(self):
        """Verify include rule patterns in system prompt."""
        prompt = CONFIG_CHAT_SYSTEM_PROMPT

        # These patterns should be documented
        assert "every day" in prompt.lower() or "daily" in prompt.lower()
        assert "include" in prompt.lower()
        assert "twice a week" in prompt.lower() or "times_per_week" in prompt.lower()

    def test_exclude_patterns(self):
        """Verify exclude rule patterns in system prompt."""
        prompt = CONFIG_CHAT_SYSTEM_PROMPT

        assert "never" in prompt.lower()
        assert "exclude" in prompt.lower()
        assert "don't want" in prompt.lower()

    def test_allergy_patterns(self):
        """Verify allergy patterns in system prompt."""
        prompt = CONFIG_CHAT_SYSTEM_PROMPT

        assert "allergic" in prompt.lower()

    def test_dislike_patterns(self):
        """Verify dislike patterns in system prompt."""
        prompt = CONFIG_CHAT_SYSTEM_PROMPT

        assert "don't like" in prompt.lower()

    def test_show_config_patterns(self):
        """Verify show config patterns in system prompt."""
        prompt = CONFIG_CHAT_SYSTEM_PROMPT

        assert "show" in prompt.lower()
        assert "settings" in prompt.lower() or "config" in prompt.lower()
