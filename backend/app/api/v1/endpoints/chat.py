"""Chat endpoints with tool calling support for preference updates."""

from fastapi import APIRouter

from app.ai.chat_assistant import process_chat_message, get_chat_history
from app.api.deps import CurrentUser
from app.schemas.chat import (
    ChatHistoryResponse,
    ChatMessageRequest,
    ChatMessageResponse,
    ChatResponse,
)

router = APIRouter(prefix="/chat", tags=["chat"])


@router.post("/message", response_model=ChatResponse)
async def send_message(
    request: ChatMessageRequest,
    current_user: CurrentUser,
) -> ChatResponse:
    """Send a message to the AI cooking assistant.

    The assistant can help with:
    - Recipe questions and cooking tips
    - Ingredient substitutions
    - Meal planning suggestions
    - Indian cuisine knowledge

    **NEW: Preference Updates via Chat**
    The assistant can now update your meal planning configuration:
    - "I want chai every morning" -> Adds INCLUDE rule for chai at breakfast
    - "I don't eat mushrooms" -> Adds EXCLUDE rule for mushrooms
    - "I'm allergic to peanuts" -> Adds peanut allergy
    - "I don't like karela" -> Adds karela to dislikes
    - "Show my settings" -> Displays current configuration
    - "Undo that" -> Reverts the last change
    """
    user_id = current_user["id"]
    return await process_chat_message(user_id, request.message)


@router.get("/history", response_model=ChatHistoryResponse)
async def get_history(
    current_user: CurrentUser,
) -> ChatHistoryResponse:
    """Get chat conversation history.

    Returns text messages only (tool_use and tool_result messages are filtered).
    """
    user_id = current_user["id"]
    messages = await get_chat_history(user_id)

    return ChatHistoryResponse(
        messages=[
            ChatMessageResponse(
                id=msg["id"],
                role=msg["role"],
                content=msg["content"],
                message_type=msg.get("message_type", "text"),
                created_at=msg["created_at"],
                recipe_suggestions=None,
            )
            for msg in messages
        ],
        total_count=len(messages),
    )
