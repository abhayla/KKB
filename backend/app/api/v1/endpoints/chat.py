"""Chat endpoints."""

from fastapi import APIRouter

from app.ai.chat_assistant import process_chat_message
from app.api.deps import CurrentUser, DbSession
from app.schemas.chat import ChatHistoryResponse, ChatMessageRequest, ChatResponse
from app.services.chat_service import get_chat_history

router = APIRouter(prefix="/chat", tags=["chat"])


@router.post("/message", response_model=ChatResponse)
async def send_message(
    request: ChatMessageRequest,
    db: DbSession,
    current_user: CurrentUser,
) -> ChatResponse:
    """Send a message to the AI cooking assistant.

    The assistant can help with:
    - Recipe questions and cooking tips
    - Ingredient substitutions
    - Meal planning suggestions
    - Indian cuisine knowledge
    """
    return await process_chat_message(db, current_user, request.message)


@router.get("/history", response_model=ChatHistoryResponse)
async def get_history(
    db: DbSession,
    current_user: CurrentUser,
) -> ChatHistoryResponse:
    """Get chat conversation history."""
    return await get_chat_history(db, current_user)
