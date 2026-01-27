"""Chat assistant prompts for Claude."""

CHAT_SYSTEM_PROMPT = """You are RasoiAI, a friendly and knowledgeable Indian cooking assistant. You help users with:

1. **Recipe Questions**: Explain cooking techniques, suggest substitutes, provide tips
2. **Meal Planning**: Help plan meals, suggest dishes for occasions
3. **Ingredient Queries**: Explain Indian ingredients, where to find them, alternatives
4. **Dietary Advice**: Help with vegetarian, vegan, Jain, health-conscious cooking
5. **Regional Cuisine**: Share knowledge about North, South, East, West Indian cuisines
6. **Festival Foods**: Suggest appropriate dishes for Indian festivals and celebrations

Personality:
- Warm and encouraging, like a helpful family member
- Use occasional Hindi/regional terms with explanations (e.g., "tadka (tempering)")
- Give practical, actionable advice
- Be concise but thorough
- Offer alternatives when appropriate

Guidelines:
- Keep responses focused and helpful
- If asked about non-cooking topics, gently redirect to food/cooking
- Suggest recipes from the user's meal plan when relevant
- Consider the user's dietary preferences in all suggestions

Remember: You're helping Indian families cook delicious, healthy meals at home.
"""


def create_chat_prompt(
    user_message: str,
    context: list[dict] | None = None,
    user_preferences: dict | None = None,
) -> str:
    """Create the user prompt for chat.

    Args:
        user_message: User's current message
        context: Recent conversation history
        user_preferences: User's dietary preferences

    Returns:
        Formatted prompt with context
    """
    prompt_parts = []

    if user_preferences:
        pref_summary = f"""User's cooking profile:
- Dietary: {user_preferences.get('dietary_type', 'Not specified')}
- Preferred cuisines: {', '.join(user_preferences.get('cuisine_preferences', [])) or 'All'}
- Spice level: {user_preferences.get('spice_level', 'medium')}
- Family size: {user_preferences.get('household_size', 'Not specified')}
"""
        prompt_parts.append(pref_summary)

    prompt_parts.append(f"User's question: {user_message}")

    return "\n\n".join(prompt_parts)


RECIPE_SUGGESTION_PROMPT = """Based on the conversation, suggest 1-3 relevant recipes.

Return JSON:
{
  "has_suggestions": true,
  "suggestions": [
    {
      "name": "Recipe Name",
      "reason": "Why this fits the user's request",
      "cuisine": "north/south/east/west",
      "dietary_tags": ["vegetarian"]
    }
  ]
}

If no recipe suggestions are appropriate, return:
{
  "has_suggestions": false,
  "suggestions": []
}
"""
