"""Chat endpoints with tool calling support for preference updates."""

from datetime import datetime
from uuid import uuid4

from fastapi import APIRouter

from app.ai.chat_assistant import process_chat_message, get_chat_history
from app.ai.gemini_client import analyze_food_image
from app.api.deps import CurrentUser
from app.schemas.chat import (
    ChatHistoryResponse,
    ChatImageRequest,
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


@router.post("/image", response_model=ChatResponse)
async def send_image_message(
    request: ChatImageRequest,
    current_user: CurrentUser,
) -> ChatResponse:
    """Analyze a food image using Gemini Vision and respond.

    Upload a photo of food and get:
    - Dish identification
    - Ingredient recognition
    - Recipe suggestions
    - Cooking tips

    The image should be a clear photo of food, preferably Indian cuisine.
    """
    # Analyze the image with Gemini Vision
    response = await analyze_food_image(
        image_base64=request.image_base64,
        media_type=request.media_type,
        prompt=request.message if request.message != "Please analyze this food image" else None
    )

    message_id = str(uuid4())
    created_at = datetime.utcnow().isoformat()

    return ChatResponse(
        message=ChatMessageResponse(
            id=message_id,
            role="assistant",
            content=response["message"],
            message_type="image_analysis",
            created_at=created_at,
            recipe_suggestions=response.get("recipe_suggestions"),
        ),
        has_recipe_suggestions=bool(response.get("recipe_suggestions")),
        recipe_ids=response.get("recipe_suggestions", []),
    )
