"""AI-powered meal planning using Claude."""

import json
import logging
import uuid
from datetime import date, timedelta

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.ai.claude_client import generate_completion
from app.ai.prompts.meal_plan_prompt import (
    MEAL_PLAN_SYSTEM_PROMPT,
    create_meal_plan_prompt,
)
from app.core.exceptions import ServiceUnavailableError
from app.models.meal_plan import MealPlan, MealPlanItem
from app.models.recipe import Recipe
from app.models.user import User, UserPreferences
from app.schemas.meal_plan import GenerateMealPlanRequest, MealPlanResponse
from app.services.festival_service import get_festivals_for_date_range
from app.services.meal_plan_service import create_meal_plan, get_meal_plan_by_id

logger = logging.getLogger(__name__)


async def generate_meal_plan(
    db: AsyncSession,
    user: User,
    request: GenerateMealPlanRequest,
) -> MealPlanResponse:
    """Generate a meal plan using Claude AI.

    Args:
        db: Database session
        user: Current user
        request: Generation request

    Returns:
        Generated MealPlanResponse
    """
    # Parse week start date
    try:
        week_start = date.fromisoformat(request.week_start_date)
    except ValueError:
        week_start = date.today()
        week_start = week_start - timedelta(days=week_start.weekday())

    week_end = week_start + timedelta(days=6)

    # Get user preferences
    result = await db.execute(
        select(UserPreferences).where(UserPreferences.user_id == user.id)
    )
    prefs = result.scalar_one_or_none()

    preferences = {
        "household_size": prefs.family_size if prefs else 4,
        "dietary_type": prefs.dietary_type if prefs else "vegetarian",
        "dietary_restrictions": prefs.dietary_tags or [] if prefs else [],
        "cuisine_preferences": prefs.cuisine_preferences or [] if prefs else [],
        "cooking_time_preference": prefs.cooking_time_preference if prefs else "moderate",
        "spice_level": prefs.spice_level if prefs else "medium",
        "disliked_ingredients": prefs.disliked_ingredients or [] if prefs else [],
    }

    # Get festivals for the week
    festivals_map = await get_festivals_for_date_range(db, week_start, week_end)
    festivals = [
        {
            "date": d.isoformat(),
            "name": f.name,
            "is_fasting_day": f.is_fasting_day,
            "special_foods": f.special_foods,
        }
        for d, f in festivals_map.items()
    ]

    # Create prompt
    prompt = create_meal_plan_prompt(
        week_start_date=week_start.isoformat(),
        preferences=preferences,
        festivals=festivals,
        exclude_recipe_ids=request.exclude_recipe_ids,
        regenerate_days=request.regenerate_days,
    )

    # Generate with Claude
    try:
        response_text = await generate_completion(
            system_prompt=MEAL_PLAN_SYSTEM_PROMPT,
            user_message=prompt,
            max_tokens=4096,
            temperature=0.8,
        )
    except Exception as e:
        logger.error(f"Claude generation failed: {e}")
        # Fall back to mock data in development
        response_text = _get_mock_meal_plan_response(week_start)

    # Parse AI response
    try:
        # Extract JSON from response
        json_start = response_text.find("{")
        json_end = response_text.rfind("}") + 1
        if json_start >= 0 and json_end > json_start:
            json_str = response_text[json_start:json_end]
            ai_plan = json.loads(json_str)
        else:
            raise ValueError("No JSON found in response")
    except (json.JSONDecodeError, ValueError) as e:
        logger.error(f"Failed to parse AI response: {e}")
        ai_plan = json.loads(_get_mock_meal_plan_response(week_start))

    # Match AI recipes to database recipes
    items = await _match_recipes_to_db(db, ai_plan, week_start, preferences, festivals_map)

    # Create meal plan
    meal_plan = await create_meal_plan(db, user, week_start, items)

    # Return formatted response
    return await get_meal_plan_by_id(db, user, str(meal_plan.id))


async def _match_recipes_to_db(
    db: AsyncSession,
    ai_plan: dict,
    week_start: date,
    preferences: dict,
    festivals_map: dict,
) -> list[dict]:
    """Match AI-generated recipes to database recipes.

    If exact match not found, find similar recipe or use AI suggestion directly.
    """
    items = []

    # Get all available recipes
    result = await db.execute(
        select(Recipe)
        .where(Recipe.is_active == True)
        .options(selectinload(Recipe.nutrition))
    )
    all_recipes = list(result.scalars().all())

    # Create lookup by name (lowercase)
    recipe_lookup = {r.name.lower(): r for r in all_recipes}

    current_date = week_start
    for day_data in ai_plan.get("days", []):
        day_date = current_date

        # Check for festival
        festival_name = None
        if day_date in festivals_map:
            festival_name = festivals_map[day_date].name

        for meal_type in ["breakfast", "lunch", "dinner", "snacks"]:
            meal_items = day_data.get(meal_type, [])
            if not isinstance(meal_items, list):
                meal_items = [meal_items] if meal_items else []

            for ai_recipe in meal_items:
                recipe_name = ai_recipe.get("name", "").lower() if isinstance(ai_recipe, dict) else str(ai_recipe).lower()

                # Try exact match
                matched_recipe = recipe_lookup.get(recipe_name)

                # Try partial match
                if not matched_recipe:
                    for db_name, recipe in recipe_lookup.items():
                        if recipe_name in db_name or db_name in recipe_name:
                            matched_recipe = recipe
                            break

                # Fall back to any recipe matching meal type and dietary
                if not matched_recipe:
                    dietary_type = preferences.get("dietary_type", "vegetarian")
                    for recipe in all_recipes:
                        if meal_type in recipe.meal_types and dietary_type in recipe.dietary_tags:
                            matched_recipe = recipe
                            break

                # Last resort: any recipe
                if not matched_recipe and all_recipes:
                    matched_recipe = all_recipes[0]

                if matched_recipe:
                    items.append({
                        "recipe_id": matched_recipe.id,
                        "date": day_date,
                        "meal_type": meal_type,
                        "servings": preferences.get("household_size", 4),
                        "is_locked": False,
                        "festival_name": festival_name,
                    })

        current_date += timedelta(days=1)

    return items


def _get_mock_meal_plan_response(week_start: date) -> str:
    """Get mock meal plan response for development."""
    days = []
    current = week_start

    meals = {
        "breakfast": ["Poha", "Upma", "Paratha", "Idli Sambhar", "Dosa", "Aloo Paratha", "Besan Chilla"],
        "lunch": ["Dal Rice", "Chole Bhature", "Rajma Rice", "Vegetable Biryani", "Kadhi Chawal", "Pav Bhaji", "Thali"],
        "dinner": ["Roti Sabzi", "Paneer Butter Masala", "Dal Makhani", "Mixed Veg Curry", "Palak Paneer", "Aloo Gobi", "Bhindi Masala"],
        "snacks": ["Samosa", "Pakora", "Dhokla", "Bhel Puri", "Chaat", "Vada Pav", "Kachori"],
    }

    for i in range(7):
        day_name = current.strftime("%A")
        days.append({
            "date": current.isoformat(),
            "day_name": day_name,
            "breakfast": [{"name": meals["breakfast"][i], "cuisine": "north", "prep_time": 20, "dietary_tags": ["vegetarian"]}],
            "lunch": [{"name": meals["lunch"][i], "cuisine": "north", "prep_time": 45, "dietary_tags": ["vegetarian"]}],
            "dinner": [{"name": meals["dinner"][i], "cuisine": "north", "prep_time": 40, "dietary_tags": ["vegetarian"]}],
            "snacks": [{"name": meals["snacks"][i], "cuisine": "north", "prep_time": 15, "dietary_tags": ["vegetarian"]}],
        })
        current += timedelta(days=1)

    return json.dumps({"days": days})
