"""Chat repository for PostgreSQL operations."""

import logging
import uuid
from datetime import datetime, timezone
from typing import Any, Optional

from sqlalchemy import select, delete

from app.db.postgres import async_session_maker
from app.models.chat import ChatMessage

logger = logging.getLogger(__name__)


class ChatRepository:
    """Repository for chat message PostgreSQL operations."""

    async def save_message(
        self,
        user_id: str,
        role: str,
        content: str,
        message_type: str = "text",
        tool_calls: Optional[list[dict]] = None,
        tool_results: Optional[list[dict]] = None,
    ) -> dict[str, Any]:
        """Save a chat message.

        Args:
            user_id: User ID
            role: Message role (user, assistant)
            content: Message content
            message_type: Type of message (text, tool_use, tool_result)
            tool_calls: Tool calls if assistant used tools
            tool_results: Tool results if this is a tool result message

        Returns:
            Saved message data
        """
        async with async_session_maker() as session:
            message = ChatMessage(
                id=str(uuid.uuid4()),
                user_id=user_id,
                role=role,
                content=content,
                message_type=message_type,
                tool_calls=tool_calls,
                tool_results=tool_results,
            )
            session.add(message)
            await session.commit()
            await session.refresh(message)

            return self._message_to_dict(message)

    async def get_recent_messages(
        self,
        user_id: str,
        limit: int = 10,
    ) -> list[dict[str, Any]]:
        """Get recent messages for a user.

        Args:
            user_id: User ID
            limit: Maximum number of messages

        Returns:
            List of messages in chronological order
        """
        async with async_session_maker() as session:
            result = await session.execute(
                select(ChatMessage)
                .where(ChatMessage.user_id == user_id)
                .order_by(ChatMessage.created_at.desc())
                .limit(limit)
            )
            messages = result.scalars().all()

            # Reverse to get chronological order
            return [self._message_to_dict(m) for m in reversed(messages)]

    async def get_history(
        self,
        user_id: str,
        limit: int = 50,
    ) -> list[dict[str, Any]]:
        """Get chat history for a user.

        Args:
            user_id: User ID
            limit: Maximum number of messages

        Returns:
            List of messages in chronological order
        """
        return await self.get_recent_messages(user_id, limit)

    async def get_context_for_claude(
        self,
        user_id: str,
        limit: int = 6,
    ) -> list[dict]:
        """Get recent messages formatted for Claude API.

        Args:
            user_id: User ID
            limit: Maximum number of messages

        Returns:
            List of message dicts with role and content for Claude
        """
        messages = await self.get_recent_messages(user_id, limit)

        claude_messages = []
        for msg in messages:
            # Handle different message types
            if msg.get("tool_calls"):
                # Assistant message with tool use - include both text and tool_use blocks
                content = []
                if msg.get("content"):
                    content.append({"type": "text", "text": msg["content"]})
                for tc in msg["tool_calls"]:
                    content.append({
                        "type": "tool_use",
                        "id": tc["id"],
                        "name": tc["name"],
                        "input": tc["input"],
                    })
                claude_messages.append({"role": "assistant", "content": content})
            elif msg.get("tool_results"):
                # Tool result message
                content = []
                for tr in msg["tool_results"]:
                    content.append({
                        "type": "tool_result",
                        "tool_use_id": tr["tool_use_id"],
                        "content": tr["content"],
                    })
                claude_messages.append({"role": "user", "content": content})
            else:
                # Regular text message
                claude_messages.append({
                    "role": msg["role"],
                    "content": msg["content"],
                })

        return claude_messages

    async def clear_history(self, user_id: str) -> int:
        """Clear all chat history for a user.

        Args:
            user_id: User ID

        Returns:
            Number of messages deleted
        """
        async with async_session_maker() as session:
            # Count messages first
            count_result = await session.execute(
                select(ChatMessage).where(ChatMessage.user_id == user_id)
            )
            count = len(count_result.scalars().all())

            # Delete all messages
            await session.execute(
                delete(ChatMessage).where(ChatMessage.user_id == user_id)
            )
            await session.commit()

            logger.info(f"Cleared {count} messages for user {user_id}")
            return count

    # Helper methods
    def _message_to_dict(self, message: ChatMessage) -> dict[str, Any]:
        """Convert ChatMessage model to dictionary."""
        return {
            "id": message.id,
            "user_id": message.user_id,
            "role": message.role,
            "content": message.content,
            "message_type": message.message_type,
            "tool_calls": message.tool_calls,
            "tool_results": message.tool_results,
            "created_at": message.created_at,
            "updated_at": message.updated_at,
        }
