"""Tests for chat_service — message persistence and history retrieval.

Covers:
- send_message: persists both user + assistant messages, returns assistant
- get_chat_history: chronological order, limit respected, user isolation
- get_recent_context: role+content dicts in chronological order
"""

import uuid

import pytest
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.chat import ChatMessage
from app.models.user import User
from app.services.chat_service import (
    get_chat_history,
    get_recent_context,
    send_message,
)


# ==================== send_message ====================


@pytest.mark.asyncio
async def test_send_message_persists_both_user_and_assistant_rows(
    db_session: AsyncSession, test_user: User
):
    await send_message(db_session, test_user, "Hi", "Hello!")

    result = await db_session.execute(
        select(ChatMessage).where(ChatMessage.user_id == test_user.id)
    )
    rows = result.scalars().all()
    roles = [r.role for r in rows]
    contents = [r.content for r in rows]

    assert len(rows) == 2
    assert set(roles) == {"user", "assistant"}
    assert "Hi" in contents
    assert "Hello!" in contents


@pytest.mark.asyncio
async def test_send_message_returns_assistant_response(
    db_session: AsyncSession, test_user: User
):
    response = await send_message(db_session, test_user, "Who?", "I'm RasoiAI.")

    assert response.message.role == "assistant"
    assert response.message.content == "I'm RasoiAI."
    assert response.message.message_type == "text"
    assert response.has_recipe_suggestions is False
    assert response.recipe_ids == []


# ==================== get_chat_history ====================


@pytest.mark.asyncio
async def test_get_chat_history_empty_when_no_messages(
    db_session: AsyncSession, test_user: User
):
    result = await get_chat_history(db_session, test_user)

    assert result.total_count == 0
    assert result.messages == []


@pytest.mark.asyncio
async def test_get_chat_history_returns_chronological_order(
    db_session: AsyncSession, test_user: User
):
    """Persistence inserts rows at monotonically increasing timestamps;
    get_chat_history should return them oldest-first."""
    await send_message(db_session, test_user, "msg1", "reply1")
    await send_message(db_session, test_user, "msg2", "reply2")

    result = await get_chat_history(db_session, test_user)

    assert result.total_count == 4  # 2 user + 2 assistant
    contents_in_order = [m.content for m in result.messages]
    # msg1 and reply1 must come before msg2 and reply2
    assert contents_in_order.index("msg1") < contents_in_order.index("msg2")
    assert contents_in_order.index("reply1") < contents_in_order.index("reply2")


@pytest.mark.asyncio
async def test_get_chat_history_respects_limit(
    db_session: AsyncSession, test_user: User
):
    for i in range(5):
        await send_message(db_session, test_user, f"q{i}", f"a{i}")

    result = await get_chat_history(db_session, test_user, limit=3)

    # limit applied to the SQL query (picks 3 most-recent before reversing).
    assert result.total_count == 3
    assert len(result.messages) == 3


@pytest.mark.asyncio
async def test_get_chat_history_isolates_by_user(
    db_session: AsyncSession, test_user: User
):
    other = User(
        id=str(uuid.uuid4()),
        firebase_uid=f"firebase-{uuid.uuid4().hex[:8]}",
        email=f"o-{uuid.uuid4().hex[:6]}@example.com",
        name="Other",
        is_active=True,
    )
    db_session.add(other)
    await db_session.commit()

    await send_message(db_session, test_user, "mine", "mine-reply")
    await send_message(db_session, other, "theirs", "theirs-reply")

    a = await get_chat_history(db_session, test_user)
    b = await get_chat_history(db_session, other)

    a_contents = {m.content for m in a.messages}
    b_contents = {m.content for m in b.messages}
    assert a_contents == {"mine", "mine-reply"}
    assert b_contents == {"theirs", "theirs-reply"}


# ==================== get_recent_context ====================


@pytest.mark.asyncio
async def test_get_recent_context_returns_role_content_dicts(
    db_session: AsyncSession, test_user: User
):
    await send_message(db_session, test_user, "hi", "hello")

    result = await get_recent_context(db_session, test_user)

    assert isinstance(result, list)
    assert all(isinstance(d, dict) and set(d.keys()) == {"role", "content"} for d in result)
    # chronological order: user first, then assistant
    assert result[0]["role"] == "user"
    assert result[0]["content"] == "hi"
    assert result[1]["role"] == "assistant"
    assert result[1]["content"] == "hello"


@pytest.mark.asyncio
async def test_get_recent_context_respects_limit(
    db_session: AsyncSession, test_user: User
):
    for i in range(6):
        await send_message(db_session, test_user, f"q{i}", f"a{i}")

    result = await get_recent_context(db_session, test_user, limit=4)

    assert len(result) == 4


@pytest.mark.asyncio
async def test_get_recent_context_empty_when_no_messages(
    db_session: AsyncSession, test_user: User
):
    result = await get_recent_context(db_session, test_user)
    assert result == []
