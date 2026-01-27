"""Claude API client wrapper."""

import logging
from typing import Optional

from anthropic import Anthropic, APIError

from app.config import settings

logger = logging.getLogger(__name__)

# Claude client instance
_client: Optional[Anthropic] = None


def get_claude_client() -> Optional[Anthropic]:
    """Get or create Claude client instance.

    Returns:
        Anthropic client or None if not configured
    """
    global _client

    if _client is not None:
        return _client

    if not settings.anthropic_api_key:
        logger.warning("ANTHROPIC_API_KEY not configured")
        return None

    _client = Anthropic(api_key=settings.anthropic_api_key)
    return _client


async def generate_completion(
    system_prompt: str,
    user_message: str,
    max_tokens: int = 4096,
    temperature: float = 0.7,
    model: str = "claude-3-sonnet-20240229",
) -> str:
    """Generate a completion using Claude.

    Args:
        system_prompt: System instructions
        user_message: User's message
        max_tokens: Maximum tokens in response
        temperature: Sampling temperature
        model: Model to use

    Returns:
        Generated text response

    Raises:
        Exception: If API call fails
    """
    client = get_claude_client()

    if not client:
        # Return mock response in development
        if settings.debug:
            logger.warning("Claude not configured, returning mock response")
            return "I'm a mock AI assistant. Claude API is not configured."
        raise Exception("Claude API not configured")

    try:
        message = client.messages.create(
            model=model,
            max_tokens=max_tokens,
            temperature=temperature,
            system=system_prompt,
            messages=[{"role": "user", "content": user_message}],
        )

        # Extract text from response
        if message.content and len(message.content) > 0:
            return message.content[0].text

        return ""

    except APIError as e:
        logger.error(f"Claude API error: {e}")
        raise Exception(f"AI service error: {str(e)}")


async def generate_chat_completion(
    system_prompt: str,
    messages: list[dict],
    max_tokens: int = 2048,
    temperature: float = 0.7,
    model: str = "claude-3-sonnet-20240229",
) -> str:
    """Generate a chat completion using Claude.

    Args:
        system_prompt: System instructions
        messages: List of message dicts with 'role' and 'content'
        max_tokens: Maximum tokens in response
        temperature: Sampling temperature
        model: Model to use

    Returns:
        Generated text response
    """
    client = get_claude_client()

    if not client:
        if settings.debug:
            logger.warning("Claude not configured, returning mock response")
            return "I'm a mock AI assistant. How can I help you with cooking today?"
        raise Exception("Claude API not configured")

    try:
        # Ensure messages have correct format
        formatted_messages = [
            {"role": msg["role"], "content": msg["content"]} for msg in messages
        ]

        message = client.messages.create(
            model=model,
            max_tokens=max_tokens,
            temperature=temperature,
            system=system_prompt,
            messages=formatted_messages,
        )

        if message.content and len(message.content) > 0:
            return message.content[0].text

        return ""

    except APIError as e:
        logger.error(f"Claude API error: {e}")
        raise Exception(f"AI service error: {str(e)}")
