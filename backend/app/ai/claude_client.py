"""Claude API client wrapper with tool calling support."""

import json
import logging
from dataclasses import dataclass
from typing import Any, Optional

from anthropic import Anthropic, APIError

from app.config import settings

logger = logging.getLogger(__name__)


@dataclass
class ToolCall:
    """Represents a tool call from Claude."""
    id: str
    name: str
    input: dict[str, Any]


@dataclass
class ChatCompletionResult:
    """Result from a chat completion that may include tool calls."""
    text: Optional[str] = None
    tool_calls: list[ToolCall] = None
    stop_reason: str = "end_turn"

    def __post_init__(self):
        if self.tool_calls is None:
            self.tool_calls = []

    @property
    def has_tool_calls(self) -> bool:
        return len(self.tool_calls) > 0

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


async def generate_with_tools(
    system_prompt: str,
    messages: list[dict],
    tools: list[dict],
    max_tokens: int = 2048,
    temperature: float = 0.7,
    model: str = "claude-3-sonnet-20240229",
) -> ChatCompletionResult:
    """Generate a chat completion with tool calling support.

    Args:
        system_prompt: System instructions
        messages: List of message dicts with 'role' and 'content'
        tools: List of tool definitions in Anthropic format
        max_tokens: Maximum tokens in response
        temperature: Sampling temperature
        model: Model to use

    Returns:
        ChatCompletionResult with text and/or tool calls
    """
    client = get_claude_client()

    if not client:
        if settings.debug:
            logger.warning("Claude not configured, returning mock response")
            return ChatCompletionResult(
                text="I'm a mock AI assistant. Claude API is not configured.",
                stop_reason="end_turn"
            )
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
            tools=tools,
        )

        # Parse response
        text_content = None
        tool_calls = []

        for block in message.content:
            if block.type == "text":
                text_content = block.text
            elif block.type == "tool_use":
                tool_calls.append(ToolCall(
                    id=block.id,
                    name=block.name,
                    input=block.input,
                ))

        return ChatCompletionResult(
            text=text_content,
            tool_calls=tool_calls,
            stop_reason=message.stop_reason,
        )

    except APIError as e:
        logger.error(f"Claude API error: {e}")
        raise Exception(f"AI service error: {str(e)}")


async def continue_with_tool_result(
    system_prompt: str,
    messages: list[dict],
    tool_results: list[dict],
    tools: list[dict],
    max_tokens: int = 2048,
    temperature: float = 0.7,
    model: str = "claude-3-sonnet-20240229",
) -> ChatCompletionResult:
    """Continue a conversation after tool execution.

    Args:
        system_prompt: System instructions
        messages: Previous messages including assistant's tool_use response
        tool_results: Results from executing tools
        tools: Tool definitions
        max_tokens: Maximum tokens in response
        temperature: Sampling temperature
        model: Model to use

    Returns:
        ChatCompletionResult with Claude's response to tool results
    """
    client = get_claude_client()

    if not client:
        if settings.debug:
            logger.warning("Claude not configured, returning mock response")
            return ChatCompletionResult(
                text="Tool executed successfully (mock mode).",
                stop_reason="end_turn"
            )
        raise Exception("Claude API not configured")

    try:
        # Build the full message history with tool results
        formatted_messages = []

        for msg in messages:
            formatted_messages.append({
                "role": msg["role"],
                "content": msg["content"]
            })

        # Add tool results as user message
        tool_result_content = []
        for result in tool_results:
            tool_result_content.append({
                "type": "tool_result",
                "tool_use_id": result["tool_use_id"],
                "content": result["content"],
            })

        formatted_messages.append({
            "role": "user",
            "content": tool_result_content,
        })

        message = client.messages.create(
            model=model,
            max_tokens=max_tokens,
            temperature=temperature,
            system=system_prompt,
            messages=formatted_messages,
            tools=tools,
        )

        # Parse response
        text_content = None
        tool_calls = []

        for block in message.content:
            if block.type == "text":
                text_content = block.text
            elif block.type == "tool_use":
                tool_calls.append(ToolCall(
                    id=block.id,
                    name=block.name,
                    input=block.input,
                ))

        return ChatCompletionResult(
            text=text_content,
            tool_calls=tool_calls,
            stop_reason=message.stop_reason,
        )

    except APIError as e:
        logger.error(f"Claude API error: {e}")
        raise Exception(f"AI service error: {str(e)}")
