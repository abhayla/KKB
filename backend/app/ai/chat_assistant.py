"""AI-powered chat assistant using Claude with tool calling support."""

import logging
from collections import defaultdict
from datetime import date
from typing import Any, Optional

from app.ai.claude_client import (
    generate_with_tools,
    continue_with_tool_result,
    generate_chat_completion,
    ChatCompletionResult,
)
from app.ai.tools import (
    PREFERENCE_TOOLS,
    MEAL_PLAN_TOOLS,
    ALL_CHAT_TOOLS,
    CONFIG_CHAT_SYSTEM_PROMPT,
    format_config_for_display,
)
from app.services import meal_plan_chat_service
from app.repositories.chat_repository import ChatRepository
from app.repositories.user_repository import UserRepository
from app.services.preference_update_service import PreferenceUpdateService
from app.schemas.chat import ChatResponse, ChatMessageResponse

logger = logging.getLogger(__name__)

# Maximum tool call iterations to prevent infinite loops
MAX_TOOL_ITERATIONS = 5


def _build_meal_context(items: list[dict]) -> str:
    """Build meal plan context string for the system prompt.

    Args:
        items: List of dicts with recipe_name, meal_type, and date fields.

    Returns:
        Formatted string describing today's meals, or empty string if no items.
    """
    if not items:
        return ""

    meals_by_type: dict[str, list[str]] = defaultdict(list)
    for item in items:
        meal_type = item.get("meal_type", "other")
        recipe_name = item.get("recipe_name", "Unknown")
        meals_by_type[meal_type].append(recipe_name)

    lines = ["\n**User's meals for today:**"]
    slot_order = ["breakfast", "lunch", "dinner", "snacks"]
    for slot in slot_order:
        if slot in meals_by_type:
            names = ", ".join(meals_by_type[slot])
            lines.append(f"- {slot.title()}: {names}")

    # Include any meal types not in the standard order
    for slot, names_list in meals_by_type.items():
        if slot not in slot_order:
            names = ", ".join(names_list)
            lines.append(f"- {slot.title()}: {names}")

    return "\n".join(lines)


async def process_chat_message(
    user_id: str,
    message: str,
) -> ChatResponse:
    """Process a chat message with tool calling support.

    Args:
        user_id: User ID
        message: User's message

    Returns:
        ChatResponse with AI response
    """
    chat_repo = ChatRepository()
    user_repo = UserRepository()
    pref_service = PreferenceUpdateService()

    # Get user preferences for context
    preferences = await user_repo.get_preferences(user_id)

    # Build system prompt with current config
    system_prompt = CONFIG_CHAT_SYSTEM_PROMPT
    if preferences:
        config_display = format_config_for_display(preferences)
        system_prompt += f"\n\n**Current User Configuration:**\n{config_display}"

    # Add today's meal plan context
    try:
        today_meals = await meal_plan_chat_service.query_meals(
            user_id=user_id,
            date_str="today",
            meal_type="ALL",
        )
        if today_meals.get("success") and "meals" in today_meals:
            meal_items = []
            for meal_type, items in today_meals["meals"].items():
                for item in items:
                    meal_items.append({
                        "recipe_name": item.get("name", "Unknown"),
                        "meal_type": meal_type,
                        "date": date.today(),
                    })
            meal_context = _build_meal_context(meal_items)
            if meal_context:
                system_prompt += meal_context
    except Exception as e:
        logger.debug(f"Could not load meal plan context: {e}")

    # Get recent conversation context
    recent_messages = await chat_repo.get_context_for_claude(user_id, limit=6)

    # Add current message to context
    messages = recent_messages + [{"role": "user", "content": message}]

    # Store user message
    await chat_repo.save_message(
        user_id=user_id,
        role="user",
        content=message,
    )

    # Generate AI response with tools
    try:
        result = await generate_with_tools(
            system_prompt=system_prompt,
            messages=messages,
            tools=ALL_CHAT_TOOLS,
            max_tokens=1024,
            temperature=0.7,
        )

        # Handle tool calls in a loop
        iteration = 0
        while result.has_tool_calls and iteration < MAX_TOOL_ITERATIONS:
            iteration += 1
            logger.info(f"Processing {len(result.tool_calls)} tool calls (iteration {iteration})")

            # Execute all tool calls
            tool_results = []
            for tool_call in result.tool_calls:
                tool_result = await _execute_tool(
                    tool_call.name,
                    tool_call.input,
                    user_id,
                    pref_service,
                )
                tool_results.append({
                    "tool_use_id": tool_call.id,
                    "content": tool_result,
                })

            # Store assistant's tool use message
            await chat_repo.save_message(
                user_id=user_id,
                role="assistant",
                content=result.text or "",
                message_type="tool_use",
                tool_calls=[{
                    "id": tc.id,
                    "name": tc.name,
                    "input": tc.input,
                } for tc in result.tool_calls],
            )

            # Store tool results
            await chat_repo.save_message(
                user_id=user_id,
                role="user",
                content="",
                message_type="tool_result",
                tool_results=tool_results,
            )

            # Build messages for continuation
            messages.append({
                "role": "assistant",
                "content": [
                    {"type": "text", "text": result.text} if result.text else None,
                    *[{
                        "type": "tool_use",
                        "id": tc.id,
                        "name": tc.name,
                        "input": tc.input,
                    } for tc in result.tool_calls]
                ],
            })
            # Filter out None values from content
            messages[-1]["content"] = [c for c in messages[-1]["content"] if c is not None]

            # Continue with tool results
            result = await continue_with_tool_result(
                system_prompt=system_prompt,
                messages=messages,
                tool_results=tool_results,
                tools=ALL_CHAT_TOOLS,
                max_tokens=1024,
                temperature=0.7,
            )

        # Extract final response text
        ai_response = result.text or "I've updated your preferences."

    except Exception as e:
        logger.error(f"Chat generation failed: {e}")
        ai_response = _get_fallback_response(message)

    # Store final assistant response
    saved_message = await chat_repo.save_message(
        user_id=user_id,
        role="assistant",
        content=ai_response,
    )

    return ChatResponse(
        message=ChatMessageResponse(
            id=saved_message["id"],
            role="assistant",
            content=ai_response,
            message_type="text",
            created_at=saved_message["created_at"].isoformat(),
            recipe_suggestions=None,
        ),
        has_recipe_suggestions=False,
        recipe_ids=[],
    )


async def _execute_tool(
    tool_name: str,
    tool_input: dict[str, Any],
    user_id: str,
    pref_service: PreferenceUpdateService,
) -> str:
    """Execute a tool call and return the result.

    Args:
        tool_name: Name of the tool to execute
        tool_input: Tool input parameters
        user_id: User ID
        pref_service: Preference update service

    Returns:
        Tool result as string
    """
    logger.info(f"Executing tool: {tool_name} with input: {tool_input}")

    try:
        if tool_name == "update_recipe_rule":
            result = await pref_service.update_recipe_rule(
                user_id=user_id,
                action=tool_input["action"],
                rule_type=tool_input["rule_type"],
                target=tool_input["target"],
                frequency=tool_input.get("frequency"),
                times_per_week=tool_input.get("times_per_week"),
                meal_slots=tool_input.get("meal_slots"),
                reason=tool_input.get("reason"),
                force_override=tool_input.get("force_override", False),
            )

        elif tool_name == "update_allergy":
            result = await pref_service.update_allergy(
                user_id=user_id,
                action=tool_input["action"],
                ingredient=tool_input["ingredient"],
                severity=tool_input.get("severity"),
            )

        elif tool_name == "update_dislike":
            result = await pref_service.update_dislike(
                user_id=user_id,
                action=tool_input["action"],
                ingredient=tool_input["ingredient"],
            )

        elif tool_name == "update_preference":
            result = await pref_service.update_preference(
                user_id=user_id,
                preference_type=tool_input["preference_type"],
                action=tool_input["action"],
                value=tool_input["value"],
            )

        elif tool_name == "undo_last_change":
            result = await pref_service.undo_last_change(user_id=user_id)

        elif tool_name == "show_config":
            result = await pref_service.show_config(
                user_id=user_id,
                section=tool_input.get("section", "all"),
            )

        # Meal plan tools
        elif tool_name == "query_current_meals":
            result = await meal_plan_chat_service.query_meals(
                user_id=user_id,
                date_str=tool_input["date"],
                meal_type=tool_input.get("meal_type", "ALL"),
            )
            # Format meal results for display
            if result.get("success") and "meals" in result:
                meals = result["meals"]
                lines = [f"**Meals for {result.get('day_name', '')} ({result.get('date', '')}):**"]
                if result.get("festival"):
                    lines.append(f"🎉 Festival: {result['festival']}")
                for meal_type, items in meals.items():
                    if items:
                        item_names = ", ".join([i["name"] for i in items])
                        lines.append(f"- **{meal_type.title()}:** {item_names}")
                return "\n".join(lines) if len(lines) > 1 else "No meals planned for this day."

        elif tool_name == "swap_meal_recipe":
            result = await meal_plan_chat_service.swap_recipe(
                user_id=user_id,
                date_str=tool_input["date"],
                meal_type=tool_input["meal_type"],
                current_recipe_name=tool_input.get("current_recipe_name"),
                requested_recipe_name=tool_input.get("requested_recipe_name"),
            )
            # Handle suggestion responses
            if result.get("needs_confirmation") and "suggestions" in result:
                suggestions = result["suggestions"]
                lines = [result.get("message", "Here are some alternatives:")]
                for i, name in enumerate(suggestions, 1):
                    lines.append(f"  {i}. {name}")
                lines.append("\nWhich one would you like?")
                return "\n".join(lines)

        elif tool_name == "add_recipe_to_meal":
            result = await meal_plan_chat_service.add_recipe(
                user_id=user_id,
                date_str=tool_input["date"],
                meal_type=tool_input["meal_type"],
                recipe_name=tool_input["recipe_name"],
            )

        elif tool_name == "remove_recipe_from_meal":
            result = await meal_plan_chat_service.remove_recipe(
                user_id=user_id,
                date_str=tool_input["date"],
                meal_type=tool_input["meal_type"],
                recipe_name=tool_input["recipe_name"],
            )

        else:
            result = {"success": False, "message": f"Unknown tool: {tool_name}"}

        # Format result for Claude
        # Handle both dataclass and dict results
        if hasattr(result, "success"):
            # Dataclass result (UpdateResult)
            if result.success:
                return result.message or "Operation completed successfully."
            else:
                return f"Error: {result.message or 'Operation failed.'}"
        elif isinstance(result, dict):
            # Dict result
            if result.get("success"):
                return result.get("message", "Operation completed successfully.")
            else:
                return f"Error: {result.get('message', 'Operation failed.')}"
        else:
            return str(result)

    except Exception as e:
        logger.error(f"Tool execution failed: {tool_name} - {e}")
        return f"Error executing {tool_name}: {str(e)}"


async def get_chat_history(user_id: str, limit: int = 50) -> list[dict]:
    """Get chat history for a user.

    Args:
        user_id: User ID
        limit: Maximum number of messages

    Returns:
        List of messages
    """
    chat_repo = ChatRepository()
    messages = await chat_repo.get_history(user_id, limit)

    return [
        {
            "id": msg["id"],
            "role": msg["role"],
            "content": msg["content"],
            "message_type": msg.get("message_type", "text"),
            "created_at": msg["created_at"].isoformat() if hasattr(msg["created_at"], "isoformat") else str(msg["created_at"]),
        }
        for msg in messages
        # Filter out tool_use and tool_result messages from history display
        if msg.get("message_type", "text") == "text"
    ]


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
            "- Ghee -> Butter or oil\n"
            "- Cream -> Coconut cream or cashew paste\n"
            "- Paneer -> Tofu\n"
            "- Onion (for Jain) -> Asafoetida (hing)\n"
            "- Garlic -> Extra ginger\n\n"
            "Let me know what specific ingredient you're looking to substitute!"
        )

    if any(word in message_lower for word in ["hello", "hi", "namaste"]):
        return (
            "Namaste! I'm RasoiAI, your Indian cooking assistant. "
            "I can help you with recipes, meal planning, cooking tips, and ingredient questions. "
            "What would you like to cook today?"
        )

    # Config-related fallback
    if any(word in message_lower for word in ["preference", "allergy", "dislike", "exclude", "include", "rule"]):
        return (
            "I can help you update your meal planning preferences! While I'm having trouble "
            "connecting right now, you can:\n"
            "- Add allergies or dislikes\n"
            "- Include or exclude specific ingredients\n"
            "- Set cooking time preferences\n\n"
            "Please try again in a moment."
        )

    return (
        "I'm here to help with all your Indian cooking questions! "
        "You can ask me about:\n"
        "- Recipes and cooking techniques\n"
        "- Ingredient substitutions\n"
        "- Meal planning ideas\n"
        "- Regional cuisines\n"
        "- Festival foods\n"
        "- Your preferences (allergies, dislikes, rules)\n\n"
        "What would you like to know?"
    )
