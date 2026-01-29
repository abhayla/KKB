"""Chat repository for Firestore operations."""

import logging
import uuid
from datetime import datetime, timezone
from typing import Any, Optional

from app.db.firestore import Collections, get_firestore_client, doc_to_dict

logger = logging.getLogger(__name__)


class ChatRepository:
    """Repository for chat message Firestore operations."""

    def __init__(self):
        self.db = get_firestore_client()
        self.collection = self.db.collection(Collections.CHAT_MESSAGES)

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
        message_id = str(uuid.uuid4())
        now = datetime.now(timezone.utc)

        message_data = {
            "user_id": user_id,
            "role": role,
            "content": content,
            "message_type": message_type,
            "created_at": now,
        }

        if tool_calls:
            message_data["tool_calls"] = tool_calls

        if tool_results:
            message_data["tool_results"] = tool_results

        await self.collection.document(message_id).set(message_data)

        message_data["id"] = message_id
        return message_data

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
        # Note: Using a simple where query and sorting in memory to avoid
        # requiring a composite index on (user_id, created_at)
        query = self.collection.where("user_id", "==", user_id)

        messages = []
        async for doc in query.stream():
            messages.append(doc_to_dict(doc))

        # Sort by created_at descending, then take limit
        messages.sort(key=lambda x: x.get("created_at", ""), reverse=True)
        messages = messages[:limit]

        # Reverse to get chronological order
        return list(reversed(messages))

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
        query = self.collection.where("user_id", "==", user_id)

        count = 0
        async for doc in query.stream():
            await doc.reference.delete()
            count += 1

        logger.info(f"Cleared {count} messages for user {user_id}")
        return count
