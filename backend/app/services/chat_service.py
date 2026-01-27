"""Chat service for AI-powered conversations."""

import uuid
from datetime import datetime, timezone

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.chat import ChatMessage
from app.models.user import User
from app.schemas.chat import ChatHistoryResponse, ChatMessageResponse, ChatResponse


async def send_message(
    db: AsyncSession,
    user: User,
    message: str,
    ai_response: str,
) -> ChatResponse:
    """Store user message and AI response.

    Args:
        db: Database session
        user: Current user
        message: User's message
        ai_response: AI's response

    Returns:
        ChatResponse with assistant message
    """
    # Store user message
    user_msg = ChatMessage(
        user_id=user.id,
        role="user",
        content=message,
        message_type="text",
    )
    db.add(user_msg)

    # Store AI response
    assistant_msg = ChatMessage(
        user_id=user.id,
        role="assistant",
        content=ai_response,
        message_type="text",
    )
    db.add(assistant_msg)

    await db.commit()
    await db.refresh(assistant_msg)

    return ChatResponse(
        message=ChatMessageResponse(
            id=str(assistant_msg.id),
            role="assistant",
            content=ai_response,
            message_type="text",
            created_at=assistant_msg.created_at.isoformat(),
            recipe_suggestions=None,
        ),
        has_recipe_suggestions=False,
        recipe_ids=[],
    )


async def get_chat_history(
    db: AsyncSession,
    user: User,
    limit: int = 50,
) -> ChatHistoryResponse:
    """Get user's chat history.

    Args:
        db: Database session
        user: Current user
        limit: Maximum number of messages

    Returns:
        ChatHistoryResponse with messages
    """
    result = await db.execute(
        select(ChatMessage)
        .where(ChatMessage.user_id == user.id)
        .order_by(ChatMessage.created_at.desc())
        .limit(limit)
    )
    messages = result.scalars().all()

    # Reverse to get chronological order
    messages = list(reversed(messages))

    return ChatHistoryResponse(
        messages=[
            ChatMessageResponse(
                id=str(msg.id),
                role=msg.role,
                content=msg.content,
                message_type=msg.message_type,
                created_at=msg.created_at.isoformat(),
                recipe_suggestions=None,
            )
            for msg in messages
        ],
        total_count=len(messages),
    )


async def get_recent_context(
    db: AsyncSession,
    user: User,
    limit: int = 10,
) -> list[dict]:
    """Get recent messages for AI context.

    Args:
        db: Database session
        user: Current user
        limit: Maximum number of messages

    Returns:
        List of message dicts for AI context
    """
    result = await db.execute(
        select(ChatMessage)
        .where(ChatMessage.user_id == user.id)
        .order_by(ChatMessage.created_at.desc())
        .limit(limit)
    )
    messages = result.scalars().all()

    # Reverse for chronological order
    messages = list(reversed(messages))

    return [{"role": msg.role, "content": msg.content} for msg in messages]
