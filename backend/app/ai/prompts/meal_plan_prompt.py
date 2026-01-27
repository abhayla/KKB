"""Meal plan generation prompts for Claude."""

MEAL_PLAN_SYSTEM_PROMPT = """You are RasoiAI, an expert Indian meal planner. You create personalized weekly meal plans for Indian families.

Your expertise includes:
- All regional Indian cuisines: North Indian, South Indian, East Indian, West Indian
- Dietary considerations: Vegetarian, Vegan, Jain (no root vegetables), Sattvic (no onion/garlic)
- Festival and fasting foods
- Balanced nutrition with Indian ingredients
- Quick weekday meals and elaborate weekend cooking

Guidelines:
1. Create diverse meals - don't repeat recipes in the same week
2. Balance nutrition across the day
3. Consider cooking time preferences
4. Respect all dietary restrictions strictly
5. Suggest appropriate festival foods when applicable
6. Include breakfast, lunch, dinner, and optional snacks

Output format: Return a valid JSON object with the meal plan structure.
"""


def create_meal_plan_prompt(
    week_start_date: str,
    preferences: dict,
    festivals: list[dict],
    exclude_recipe_ids: list[str] | None = None,
    regenerate_days: list[str] | None = None,
) -> str:
    """Create the user prompt for meal plan generation.

    Args:
        week_start_date: Start date of the week (Monday)
        preferences: User preferences dict
        festivals: List of festivals during the week
        exclude_recipe_ids: Recipe IDs to exclude
        regenerate_days: Specific days to regenerate (None = all)

    Returns:
        Formatted user prompt
    """
    prompt = f"""Generate a 7-day Indian meal plan starting from {week_start_date}.

User Preferences:
- Family size: {preferences.get('household_size', 4)} people
- Dietary type: {preferences.get('dietary_type', 'vegetarian')}
- Dietary restrictions: {', '.join(preferences.get('dietary_restrictions', [])) or 'None'}
- Cuisine preferences: {', '.join(preferences.get('cuisine_preferences', ['north', 'south'])) or 'All regions'}
- Cooking time: {preferences.get('cooking_time_preference', 'moderate')}
- Spice level: {preferences.get('spice_level', 'medium')}
- Disliked ingredients: {', '.join(preferences.get('disliked_ingredients', [])) or 'None'}
"""

    if festivals:
        prompt += "\nFestivals this week:\n"
        for f in festivals:
            prompt += f"- {f['date']}: {f['name']}"
            if f.get('is_fasting_day'):
                prompt += " (Fasting day)"
            if f.get('special_foods'):
                prompt += f" - Suggested: {', '.join(f['special_foods'])}"
            prompt += "\n"

    if exclude_recipe_ids:
        prompt += f"\nExclude these recipes (recently used): {', '.join(exclude_recipe_ids)}\n"

    if regenerate_days:
        prompt += f"\nOnly regenerate meals for: {', '.join(regenerate_days)}\n"

    prompt += """

Return a JSON object with this structure:
{
  "days": [
    {
      "date": "2024-01-15",
      "day_name": "Monday",
      "breakfast": [{"name": "Recipe Name", "cuisine": "north/south/east/west", "prep_time": 15, "dietary_tags": ["vegetarian"]}],
      "lunch": [...],
      "dinner": [...],
      "snacks": [...]
    }
  ]
}

Include 1-2 items per meal. Ensure variety and balance across the week.
"""

    return prompt


SWAP_MEAL_SYSTEM_PROMPT = """You are RasoiAI, an expert Indian meal planner. The user wants to swap a meal with an alternative.

Suggest an alternative recipe that:
1. Fits the same meal type (breakfast/lunch/dinner/snacks)
2. Respects dietary restrictions
3. Is different from the current recipe
4. Matches the user's preferences

Return a JSON object with the alternative recipe.
"""


def create_swap_meal_prompt(
    current_recipe: str,
    meal_type: str,
    preferences: dict,
    exclude_recipes: list[str],
) -> str:
    """Create prompt for swapping a meal.

    Args:
        current_recipe: Current recipe name
        meal_type: Type of meal (breakfast, lunch, etc.)
        preferences: User preferences
        exclude_recipes: Recipes to exclude

    Returns:
        Formatted prompt
    """
    return f"""The user wants to replace "{current_recipe}" for {meal_type}.

User preferences:
- Dietary: {preferences.get('dietary_type', 'vegetarian')}
- Restrictions: {', '.join(preferences.get('dietary_restrictions', [])) or 'None'}
- Cuisine: {', '.join(preferences.get('cuisine_preferences', [])) or 'All'}
- Spice level: {preferences.get('spice_level', 'medium')}

Don't suggest: {', '.join(exclude_recipes) if exclude_recipes else 'None'}

Return JSON:
{{
  "recipe": {{
    "name": "Recipe Name",
    "cuisine": "north/south/east/west",
    "prep_time": 20,
    "dietary_tags": ["vegetarian"]
  }}
}}
"""
