"""Tests for chat history pagination, ordering, and context window behavior.

Tests the GET /chat/history endpoint (limit, ordering, tool message filtering)
and the ChatRepository.get_context_for_claude() context window (last 6 messages).

Run with: PYTHONPATH=. pytest tests/api/test_chat_pagination.py -v
"""

import uuid
from datetime import datetime, timedelta, timezone
from unittest.mock import patch

import pytest
import pytest_asyncio
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.chat import ChatMessage
from app.models.user import User
from app.repositories.chat_repository import ChatRepository


# ==================== Helpers ====================


async def _create_message(
    db_session: AsyncSession,
    user_id: str,
    *,
    role: str = "user",
    content: str = "Hello",
    message_type: str = "text",
    created_at: datetime | None = None,
    tool_calls: list | None = None,
    tool_results: list | None = None,
) -> ChatMessage:
    """Insert a ChatMessage directly via the test session."""
    msg = ChatMessage(
        id=str(uuid.uuid4()),
        user_id=user_id,
        role=role,
        content=content,
        message_type=message_type,
        tool_calls=tool_calls,
        tool_results=tool_results,
    )
    db_session.add(msg)
    await db_session.flush()

    # Manually set created_at if provided (after flush so the row exists)
    if created_at is not None:
        msg.created_at = created_at
        await db_session.flush()

    return msg


async def _create_conversation(
    db_session: AsyncSession,
    user_id: str,
    count: int,
    *,
    base_time: datetime | None = None,
) -> list[ChatMessage]:
    """Create `count` alternating user/assistant text messages with increasing timestamps."""
    if base_time is None:
        base_time = datetime(2026, 1, 1, tzinfo=timezone.utc)

    messages = []
    for i in range(count):
        role = "user" if i % 2 == 0 else "assistant"
        msg = await _create_message(
            db_session,
            user_id,
            role=role,
            content=f"Message {i + 1}",
            created_at=base_time + timedelta(seconds=i),
        )
        messages.append(msg)

    await db_session.commit()
    return messages


# ==================== API Endpoint Tests ====================


class TestChatHistoryReturnsMessages:
    """GET /chat/history returns the correct messages."""

    @pytest.mark.asyncio
    async def test_chat_history_returns_messages(
        self, client: AsyncClient, test_user: User, db_session: AsyncSession
    ):
        """Create 5 messages, GET /history, verify 5 returned."""
        msgs = []
        for i in range(5):
            role = "user" if i % 2 == 0 else "assistant"
            msgs.append(
                {
                    "id": str(uuid.uuid4()),
                    "role": role,
                    "content": f"Message {i + 1}",
                    "message_type": "text",
                    "created_at": (
                        datetime(2026, 1, 1, tzinfo=timezone.utc)
                        + timedelta(seconds=i)
                    ).isoformat(),
                }
            )

        with patch(
            "app.api.v1.endpoints.chat.get_chat_history",
            return_value=msgs,
        ):
            response = await client.get("/api/v1/chat/history")

        assert response.status_code == 200
        data = response.json()
        assert len(data["messages"]) == 5
        assert data["total_count"] == 5

    @pytest.mark.asyncio
    async def test_chat_history_empty(self, client: AsyncClient, test_user: User):
        """No messages, GET /history, verify empty list."""
        with patch(
            "app.api.v1.endpoints.chat.get_chat_history",
            return_value=[],
        ):
            response = await client.get("/api/v1/chat/history")

        assert response.status_code == 200
        data = response.json()
        assert data["messages"] == []
        assert data["total_count"] == 0


class TestChatHistoryFiltering:
    """GET /chat/history excludes tool_use and tool_result messages."""

    @pytest.mark.asyncio
    async def test_chat_history_excludes_tool_messages(
        self, client: AsyncClient, test_user: User
    ):
        """Only text messages are returned; tool_use and tool_result are filtered."""
        # Simulate what get_chat_history returns (already filtered)
        text_msgs = [
            {
                "id": "msg-1",
                "role": "user",
                "content": "Add chai to my breakfast",
                "message_type": "text",
                "created_at": datetime.now(timezone.utc).isoformat(),
            },
            {
                "id": "msg-4",
                "role": "assistant",
                "content": "Done! Added chai to breakfast.",
                "message_type": "text",
                "created_at": datetime.now(timezone.utc).isoformat(),
            },
        ]

        with patch(
            "app.api.v1.endpoints.chat.get_chat_history",
            return_value=text_msgs,
        ):
            response = await client.get("/api/v1/chat/history")

        assert response.status_code == 200
        data = response.json()
        assert len(data["messages"]) == 2
        for msg in data["messages"]:
            assert msg["message_type"] == "text"

    @pytest.mark.asyncio
    async def test_chat_history_unauthenticated(
        self, unauthenticated_client: AsyncClient
    ):
        """401 without auth."""
        response = await unauthenticated_client.get("/api/v1/chat/history")
        assert response.status_code == 401


# ==================== Repository-Level Tests ====================
# These test ChatRepository directly with the test DB to verify
# ordering, limit/default-limit, context window, and tool filtering.


def _make_patched_repo(test_session_maker):
    """Return a mock_session_maker that wraps the test session maker."""

    def mock_sm():
        return test_session_maker()

    return mock_sm


class TestChatRepositoryHistory:
    """Tests for ChatRepository.get_history() and get_recent_messages()."""

    @pytest.mark.asyncio
    async def test_default_limit_caps_at_50(
        self, db_session: AsyncSession, test_user: User
    ):
        """Create 60 messages, get_history with default limit, verify 50 returned."""
        from tests.conftest import _test_session_maker

        await _create_conversation(db_session, test_user.id, 60)

        repo = ChatRepository()
        with patch(
            "app.repositories.chat_repository.async_session_maker",
            _make_patched_repo(_test_session_maker),
        ):
            messages = await repo.get_history(test_user.id)

        assert len(messages) == 50

    @pytest.mark.asyncio
    async def test_custom_limit(
        self, db_session: AsyncSession, test_user: User
    ):
        """get_history(limit=10) returns exactly 10 messages."""
        from tests.conftest import _test_session_maker

        await _create_conversation(db_session, test_user.id, 20)

        repo = ChatRepository()
        with patch(
            "app.repositories.chat_repository.async_session_maker",
            _make_patched_repo(_test_session_maker),
        ):
            messages = await repo.get_history(test_user.id, limit=10)

        assert len(messages) == 10

    @pytest.mark.asyncio
    async def test_history_ordered_chronologically(
        self, db_session: AsyncSession, test_user: User
    ):
        """Messages are returned in chronological order (oldest first)."""
        from tests.conftest import _test_session_maker

        base = datetime(2026, 1, 1, tzinfo=timezone.utc)
        await _create_conversation(db_session, test_user.id, 5, base_time=base)

        repo = ChatRepository()
        with patch(
            "app.repositories.chat_repository.async_session_maker",
            _make_patched_repo(_test_session_maker),
        ):
            messages = await repo.get_history(test_user.id)

        assert len(messages) == 5
        # Verify chronological order: each message's created_at >= previous
        for i in range(1, len(messages)):
            assert messages[i]["created_at"] >= messages[i - 1]["created_at"]

        # Verify the content matches expected order
        assert messages[0]["content"] == "Message 1"
        assert messages[4]["content"] == "Message 5"

    @pytest.mark.asyncio
    async def test_history_returns_most_recent_when_limited(
        self, db_session: AsyncSession, test_user: User
    ):
        """When limit < total, the most recent messages are returned."""
        from tests.conftest import _test_session_maker

        await _create_conversation(db_session, test_user.id, 20)

        repo = ChatRepository()
        with patch(
            "app.repositories.chat_repository.async_session_maker",
            _make_patched_repo(_test_session_maker),
        ):
            messages = await repo.get_history(test_user.id, limit=5)

        assert len(messages) == 5
        # Should be the last 5 messages (16-20), in chronological order
        assert messages[0]["content"] == "Message 16"
        assert messages[4]["content"] == "Message 20"

    @pytest.mark.asyncio
    async def test_history_empty_for_no_messages(
        self, db_session: AsyncSession, test_user: User
    ):
        """Empty list when no messages exist."""
        from tests.conftest import _test_session_maker

        repo = ChatRepository()
        with patch(
            "app.repositories.chat_repository.async_session_maker",
            _make_patched_repo(_test_session_maker),
        ):
            messages = await repo.get_history(test_user.id)

        assert messages == []


class TestChatContextWindow:
    """Tests for ChatRepository.get_context_for_claude() — the 6-message context window."""

    @pytest.mark.asyncio
    async def test_context_window_last_6(
        self, db_session: AsyncSession, test_user: User
    ):
        """Create 10 messages, verify context window returns only last 6."""
        from tests.conftest import _test_session_maker

        await _create_conversation(db_session, test_user.id, 10)

        repo = ChatRepository()
        with patch(
            "app.repositories.chat_repository.async_session_maker",
            _make_patched_repo(_test_session_maker),
        ):
            context = await repo.get_context_for_claude(test_user.id)

        assert len(context) == 6
        # Should be messages 5-10 (the last 6), in chronological order
        assert context[0]["content"] == "Message 5"
        assert context[5]["content"] == "Message 10"

    @pytest.mark.asyncio
    async def test_context_window_fewer_than_limit(
        self, db_session: AsyncSession, test_user: User
    ):
        """When fewer than 6 messages exist, all are returned."""
        from tests.conftest import _test_session_maker

        await _create_conversation(db_session, test_user.id, 3)

        repo = ChatRepository()
        with patch(
            "app.repositories.chat_repository.async_session_maker",
            _make_patched_repo(_test_session_maker),
        ):
            context = await repo.get_context_for_claude(test_user.id)

        assert len(context) == 3

    @pytest.mark.asyncio
    async def test_context_window_includes_role_and_content(
        self, db_session: AsyncSession, test_user: User
    ):
        """Context messages have role and content keys for Claude API."""
        from tests.conftest import _test_session_maker

        await _create_conversation(db_session, test_user.id, 4)

        repo = ChatRepository()
        with patch(
            "app.repositories.chat_repository.async_session_maker",
            _make_patched_repo(_test_session_maker),
        ):
            context = await repo.get_context_for_claude(test_user.id)

        for msg in context:
            assert "role" in msg
            assert "content" in msg
            assert msg["role"] in ("user", "assistant")

    @pytest.mark.asyncio
    async def test_context_window_formats_tool_calls(
        self, db_session: AsyncSession, test_user: User
    ):
        """Tool call messages are formatted with tool_use blocks for Claude."""
        from tests.conftest import _test_session_maker

        base = datetime(2026, 1, 1, tzinfo=timezone.utc)

        # User message
        await _create_message(
            db_session,
            test_user.id,
            role="user",
            content="Add chai to breakfast",
            created_at=base,
        )

        # Assistant message with tool calls
        await _create_message(
            db_session,
            test_user.id,
            role="assistant",
            content="I'll add chai for you.",
            message_type="tool_use",
            tool_calls=[
                {
                    "id": "tc_1",
                    "name": "update_recipe_rule",
                    "input": {"action": "ADD", "item": "chai"},
                }
            ],
            created_at=base + timedelta(seconds=1),
        )

        # Tool result message
        await _create_message(
            db_session,
            test_user.id,
            role="user",
            content="",
            message_type="tool_result",
            tool_results=[
                {"tool_use_id": "tc_1", "content": "Rule added successfully"}
            ],
            created_at=base + timedelta(seconds=2),
        )

        # Final assistant text response
        await _create_message(
            db_session,
            test_user.id,
            role="assistant",
            content="Done! Chai added to breakfast.",
            created_at=base + timedelta(seconds=3),
        )

        await db_session.commit()

        repo = ChatRepository()
        with patch(
            "app.repositories.chat_repository.async_session_maker",
            _make_patched_repo(_test_session_maker),
        ):
            context = await repo.get_context_for_claude(test_user.id)

        assert len(context) == 4

        # First: plain user message
        assert context[0]["role"] == "user"
        assert context[0]["content"] == "Add chai to breakfast"

        # Second: assistant with tool_use blocks
        assert context[1]["role"] == "assistant"
        assert isinstance(context[1]["content"], list)
        content_types = [block["type"] for block in context[1]["content"]]
        assert "text" in content_types
        assert "tool_use" in content_types

        # Third: tool result (mapped to user role for Claude)
        assert context[2]["role"] == "user"
        assert isinstance(context[2]["content"], list)
        assert context[2]["content"][0]["type"] == "tool_result"

        # Fourth: plain assistant response
        assert context[3]["role"] == "assistant"
        assert context[3]["content"] == "Done! Chai added to breakfast."


class TestToolMessageFilteringAtServiceLevel:
    """Test that get_chat_history (chat_assistant) filters tool messages."""

    @pytest.mark.asyncio
    async def test_tool_messages_excluded_from_history(
        self, db_session: AsyncSession, test_user: User
    ):
        """Tool_use and tool_result messages are excluded; only text messages returned."""
        from tests.conftest import _test_session_maker
        from app.ai.chat_assistant import get_chat_history

        base = datetime(2026, 1, 1, tzinfo=timezone.utc)

        # Text message
        await _create_message(
            db_session,
            test_user.id,
            role="user",
            content="Add chai to breakfast",
            created_at=base,
        )

        # Tool use message (should be filtered)
        await _create_message(
            db_session,
            test_user.id,
            role="assistant",
            content="Calling tool...",
            message_type="tool_use",
            tool_calls=[{"id": "tc_1", "name": "update_recipe_rule", "input": {}}],
            created_at=base + timedelta(seconds=1),
        )

        # Tool result message (should be filtered)
        await _create_message(
            db_session,
            test_user.id,
            role="user",
            content="",
            message_type="tool_result",
            tool_results=[{"tool_use_id": "tc_1", "content": "Done"}],
            created_at=base + timedelta(seconds=2),
        )

        # Text response
        await _create_message(
            db_session,
            test_user.id,
            role="assistant",
            content="Done! Chai added.",
            created_at=base + timedelta(seconds=3),
        )

        await db_session.commit()

        with patch(
            "app.repositories.chat_repository.async_session_maker",
            _make_patched_repo(_test_session_maker),
        ):
            messages = await get_chat_history(test_user.id)

        # Only the 2 text messages should be returned
        assert len(messages) == 2
        assert messages[0]["content"] == "Add chai to breakfast"
        assert messages[0]["message_type"] == "text"
        assert messages[1]["content"] == "Done! Chai added."
        assert messages[1]["message_type"] == "text"

    @pytest.mark.asyncio
    async def test_history_messages_have_required_fields(
        self, db_session: AsyncSession, test_user: User
    ):
        """Each message dict from get_chat_history has id, role, content, message_type, created_at."""
        from tests.conftest import _test_session_maker
        from app.ai.chat_assistant import get_chat_history

        await _create_conversation(db_session, test_user.id, 3)

        with patch(
            "app.repositories.chat_repository.async_session_maker",
            _make_patched_repo(_test_session_maker),
        ):
            messages = await get_chat_history(test_user.id)

        assert len(messages) == 3
        for msg in messages:
            assert "id" in msg
            assert "role" in msg
            assert "content" in msg
            assert "message_type" in msg
            assert "created_at" in msg


class TestChatHistoryUserIsolation:
    """Messages from one user are not visible to another."""

    @pytest.mark.asyncio
    async def test_history_scoped_to_user(
        self, db_session: AsyncSession, test_user: User
    ):
        """User A's messages are not returned in User B's history."""
        from tests.conftest import _test_session_maker

        # Create a second user
        other_user = User(
            id=str(uuid.uuid4()),
            firebase_uid="other-firebase-uid",
            email="other@example.com",
            name="Other User",
            is_onboarded=True,
            is_active=True,
        )
        db_session.add(other_user)
        await db_session.commit()

        # Create messages for both users
        await _create_conversation(db_session, test_user.id, 5)
        await _create_conversation(db_session, other_user.id, 3)

        repo = ChatRepository()
        with patch(
            "app.repositories.chat_repository.async_session_maker",
            _make_patched_repo(_test_session_maker),
        ):
            user_a_msgs = await repo.get_history(test_user.id)
            user_b_msgs = await repo.get_history(other_user.id)

        assert len(user_a_msgs) == 5
        assert len(user_b_msgs) == 3

        # Verify no cross-contamination
        user_a_ids = {m["user_id"] for m in user_a_msgs}
        user_b_ids = {m["user_id"] for m in user_b_msgs}
        assert user_a_ids == {test_user.id}
        assert user_b_ids == {other_user.id}
