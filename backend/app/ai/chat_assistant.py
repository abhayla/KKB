"""AI-powered chat assistant using Claude."""

import logging

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.ai.claude_client import generate_chat_completion
from app.ai.prompts.chat_prompt import CHAT_SYSTEM_PROMPT, create_chat_prompt
from app.models.user import User, UserPreferences
from app.schemas.chat import ChatResponse
from app.services.chat_service import get_recent_context, send_message

logger = logging.getLogger(__name__)


async def process_chat_message(
    db: AsyncSession,
    user: User,
    message: str,
) -> ChatResponse:
    """Process a chat message and generate AI response.

    Args:
        db: Database session
        user: Current user
        message: User's message

    Returns:
        ChatResponse with AI response
    """
    # Get user preferences
    result = await db.execute(
        select(UserPreferences).where(UserPreferences.user_id == user.id)
    )
    prefs = result.scalar_one_or_none()

    user_preferences = None
    if prefs:
        user_preferences = {
            "dietary_type": prefs.dietary_type,
            "cuisine_preferences": prefs.cuisine_preferences or [],
            "spice_level": prefs.spice_level,
            "household_size": prefs.family_size,
        }

    # Get recent conversation context
    recent_messages = await get_recent_context(db, user, limit=6)

    # Add current message to context
    messages = recent_messages + [{"role": "user", "content": message}]

    # Generate AI response
    try:
        ai_response = await generate_chat_completion(
            system_prompt=CHAT_SYSTEM_PROMPT,
            messages=messages,
            max_tokens=1024,
            temperature=0.7,
        )
    except Exception as e:
        logger.error(f"Chat generation failed: {e}")
        ai_response = _get_fallback_response(message)

    # Store messages and return response
    return await send_message(db, user, message, ai_response)


def _get_fallback_response(message: str) -> str:
    """Get fallback response when AI is unavailable."""
    message_lower = message.lower()

    if any(word in message_lower for word in ["recipe", "cook", "make", "prepare"]):
        return (
            "I'd love to help you with that recipe! While I'm having trouble connecting "
            "to my knowledge base right now, you can check your meal plan for great recipe "
            "suggestions, or try searching in the Favorites section for saved recipes."
        )

    if any(word in message_lower for word in ["substitute", "replace", "instead"]):
        return (
            "For ingredient substitutions, here are some common Indian cooking swaps:\n"
            "- Ghee → Butter or oil\n"
            "- Cream → Coconut cream or cashew paste\n"
            "- Paneer → Tofu\n"
            "- Onion (for Jain) → Asafoetida (hing)\n"
            "- Garlic → Extra ginger\n\n"
            "Let me know what specific ingredient you're looking to substitute!"
        )

    if any(word in message_lower for word in ["hello", "hi", "namaste"]):
        return (
            "Namaste! 🙏 I'm RasoiAI, your Indian cooking assistant. "
            "I can help you with recipes, meal planning, cooking tips, and ingredient questions. "
            "What would you like to cook today?"
        )

    return (
        "I'm here to help with all your Indian cooking questions! "
        "You can ask me about:\n"
        "- Recipes and cooking techniques\n"
        "- Ingredient substitutions\n"
        "- Meal planning ideas\n"
        "- Regional cuisines\n"
        "- Festival foods\n\n"
        "What would you like to know?"
    )
