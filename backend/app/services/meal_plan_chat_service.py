"""Service for handling meal plan operations from chat.

This service provides functions to query and modify meal plans
through the chat interface using Claude tool calling.
"""

from contextlib import asynccontextmanager
from datetime import date, datetime, timedelta
from typing import Optional, Dict, Any

from sqlalchemy import select, and_
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.postgres import async_session_maker
from app.models.meal_plan import MealPlan, MealPlanItem
from app.schemas.recipe import RecipeSearchParams
from app.services import recipe_service


@asynccontextmanager
async def get_session():
    """Get a database session."""
    async with async_session_maker() as session:
        try:
            yield session
        finally:
            await session.close()


def parse_date(date_str: str) -> date:
    """Parse date string including 'today' and 'tomorrow' keywords."""
    today = date.today()
    lower = date_str.lower().strip()

    if lower == "today":
        return today
    elif lower == "tomorrow":
        return today + timedelta(days=1)
    else:
        # Try parsing as YYYY-MM-DD
        try:
            return datetime.strptime(date_str, "%Y-%m-%d").date()
        except ValueError:
            raise ValueError(f"Invalid date format: {date_str}. Use 'today', 'tomorrow', or YYYY-MM-DD")


async def query_meals(
    user_id: str,
    date_str: str,
    meal_type: Optional[str] = None,
    db: Optional[AsyncSession] = None
) -> Dict[str, Any]:
    """
    Query the user's meal plan for a specific date.

    Args:
        user_id: The user's ID
        date_str: Date to query ('today', 'tomorrow', or YYYY-MM-DD)
        meal_type: Optional meal type filter (BREAKFAST, LUNCH, DINNER, SNACKS, ALL)
        db: Database session (optional, will create if not provided)

    Returns:
        Dictionary with meals for the requested date
    """
    target_date = parse_date(date_str)

    async with get_session() as session:
        if db:
            session = db

        # Find the meal plan that contains this date
        stmt = select(MealPlan).where(
            and_(
                MealPlan.user_id == user_id,
                MealPlan.week_start_date <= target_date,
                MealPlan.week_end_date >= target_date,
                MealPlan.is_active == True
            )
        )
        result = await session.execute(stmt)
        meal_plan = result.scalar_one_or_none()

        if not meal_plan:
            return {
                "success": False,
                "message": f"No meal plan found for {target_date.strftime('%A, %B %d')}. Would you like me to generate one?",
                "date": target_date.isoformat()
            }

        # Get meal items for this date
        stmt = select(MealPlanItem).where(
            and_(
                MealPlanItem.meal_plan_id == meal_plan.id,
                MealPlanItem.date == target_date
            )
        )
        if meal_type and meal_type.upper() != "ALL":
            stmt = stmt.where(MealPlanItem.meal_type == meal_type.lower())

        result = await session.execute(stmt)
        items = result.scalars().all()

        if not items:
            return {
                "success": False,
                "message": f"No meals planned for {target_date.strftime('%A, %B %d')}.",
                "date": target_date.isoformat()
            }

        # Group by meal type
        meals_by_type = {}
        festival_name = None
        for item in items:
            meal_type_key = item.meal_type.upper()
            if meal_type_key not in meals_by_type:
                meals_by_type[meal_type_key] = []
            meals_by_type[meal_type_key].append({
                "name": item.recipe_name or "Unknown recipe",
                "recipe_id": item.recipe_id,
                "is_locked": item.is_locked
            })
            if item.festival_name:
                festival_name = item.festival_name

        return {
            "success": True,
            "date": target_date.isoformat(),
            "day_name": target_date.strftime("%A"),
            "meals": meals_by_type,
            "festival": festival_name
        }


async def swap_recipe(
    user_id: str,
    date_str: str,
    meal_type: str,
    current_recipe_name: Optional[str] = None,
    requested_recipe_name: Optional[str] = None,
    db: Optional[AsyncSession] = None
) -> Dict[str, Any]:
    """
    Swap a recipe in the meal plan with a different one.

    Args:
        user_id: The user's ID
        date_str: Date of the meal ('today', 'tomorrow', or YYYY-MM-DD)
        meal_type: Which meal slot (BREAKFAST, LUNCH, DINNER, SNACKS)
        current_recipe_name: Name of recipe to replace (optional)
        requested_recipe_name: Name of recipe user wants (optional)
        db: Database session

    Returns:
        Result of the swap operation
    """
    target_date = parse_date(date_str)
    meal_type_lower = meal_type.lower()

    async with get_session() as session:
        if db:
            session = db

        # Find the meal plan
        stmt = select(MealPlan).where(
            and_(
                MealPlan.user_id == user_id,
                MealPlan.week_start_date <= target_date,
                MealPlan.week_end_date >= target_date,
                MealPlan.is_active == True
            )
        )
        result = await session.execute(stmt)
        meal_plan = result.scalar_one_or_none()

        if not meal_plan:
            return {
                "success": False,
                "message": f"No meal plan found for {target_date.strftime('%A, %B %d')}."
            }

        # Find the item to swap
        stmt = select(MealPlanItem).where(
            and_(
                MealPlanItem.meal_plan_id == meal_plan.id,
                MealPlanItem.date == target_date,
                MealPlanItem.meal_type == meal_type_lower
            )
        )
        if current_recipe_name:
            stmt = stmt.where(MealPlanItem.recipe_name.ilike(f"%{current_recipe_name}%"))
        result = await session.execute(stmt)
        item = result.scalars().first()

        if not item:
            return {
                "success": False,
                "message": f"No {meal_type.lower()} recipe found to swap on {target_date.strftime('%A')}."
            }

        if item.is_locked:
            return {
                "success": False,
                "message": f"The {meal_type.lower()} recipe '{item.recipe_name}' is locked and cannot be swapped."
            }

        old_name = item.recipe_name or "the current recipe"

        # If a specific recipe is requested, try to find it
        if requested_recipe_name:
            search_params = RecipeSearchParams(
                query=requested_recipe_name,
                meal_type=meal_type_lower,
                limit=1
            )
            recipes = await recipe_service.search_recipes(session, search_params)

            if recipes:
                new_recipe = recipes[0]
                item.recipe_id = new_recipe.id
                item.recipe_name = new_recipe.name
                item.is_swapped = True
                await session.commit()

                return {
                    "success": True,
                    "message": f"Swapped {old_name} with {new_recipe.name} for {meal_type.lower()} on {target_date.strftime('%A')}.",
                    "old_recipe": old_name,
                    "new_recipe": new_recipe.name
                }
            else:
                return {
                    "success": False,
                    "message": f"Couldn't find a recipe matching '{requested_recipe_name}'. Would you like me to suggest alternatives?"
                }
        else:
            # Get suggestions for alternatives
            search_params = RecipeSearchParams(
                meal_type=meal_type_lower,
                limit=5
            )
            suggestions = await recipe_service.search_recipes(session, search_params)

            # Filter out current recipe
            suggestions = [r for r in suggestions if r.id != item.recipe_id][:3]

            if suggestions:
                suggestion_names = [r.name for r in suggestions]
                return {
                    "success": True,
                    "needs_confirmation": True,
                    "message": f"I can replace {old_name}. Here are some alternatives for {meal_type.lower()}:",
                    "current_recipe": old_name,
                    "suggestions": suggestion_names
                }
            else:
                return {
                    "success": False,
                    "message": f"Couldn't find alternative recipes. Please specify what you'd like instead."
                }


async def add_recipe(
    user_id: str,
    date_str: str,
    meal_type: str,
    recipe_name: str,
    db: Optional[AsyncSession] = None
) -> Dict[str, Any]:
    """
    Add a recipe to a meal slot.

    Args:
        user_id: The user's ID
        date_str: Date to add the recipe ('today', 'tomorrow', or YYYY-MM-DD)
        meal_type: Which meal slot (BREAKFAST, LUNCH, DINNER, SNACKS)
        recipe_name: Name of the recipe to add
        db: Database session

    Returns:
        Result of the add operation
    """
    target_date = parse_date(date_str)
    meal_type_lower = meal_type.lower()

    async with get_session() as session:
        if db:
            session = db

        # Find recipe in database
        search_params = RecipeSearchParams(
            query=recipe_name,
            meal_type=meal_type_lower,
            limit=1
        )
        recipes = await recipe_service.search_recipes(session, search_params)

        if not recipes:
            return {
                "success": False,
                "message": f"Couldn't find a recipe matching '{recipe_name}'. Could you be more specific?"
            }

        recipe = recipes[0]

        # Find the meal plan
        stmt = select(MealPlan).where(
            and_(
                MealPlan.user_id == user_id,
                MealPlan.week_start_date <= target_date,
                MealPlan.week_end_date >= target_date,
                MealPlan.is_active == True
            )
        )
        result = await session.execute(stmt)
        meal_plan = result.scalar_one_or_none()

        if not meal_plan:
            return {
                "success": False,
                "message": f"No meal plan found for {target_date.strftime('%A, %B %d')}. Would you like me to generate one?"
            }

        # Add the new item
        new_item = MealPlanItem(
            meal_plan_id=meal_plan.id,
            recipe_id=recipe.id,
            date=target_date,
            meal_type=meal_type_lower,
            recipe_name=recipe.name,
            is_locked=False,
            is_swapped=False
        )
        session.add(new_item)
        await session.commit()

        return {
            "success": True,
            "message": f"Added {recipe.name} to {meal_type.lower()} on {target_date.strftime('%A, %B %d')}.",
            "recipe_name": recipe.name
        }


async def remove_recipe(
    user_id: str,
    date_str: str,
    meal_type: str,
    recipe_name: str,
    db: Optional[AsyncSession] = None
) -> Dict[str, Any]:
    """
    Remove a recipe from a meal slot.

    Args:
        user_id: The user's ID
        date_str: Date of the meal ('today', 'tomorrow', or YYYY-MM-DD)
        meal_type: Which meal slot (BREAKFAST, LUNCH, DINNER, SNACKS)
        recipe_name: Name of the recipe to remove
        db: Database session

    Returns:
        Result of the remove operation
    """
    target_date = parse_date(date_str)
    meal_type_lower = meal_type.lower()

    async with get_session() as session:
        if db:
            session = db

        # Find the meal plan
        stmt = select(MealPlan).where(
            and_(
                MealPlan.user_id == user_id,
                MealPlan.week_start_date <= target_date,
                MealPlan.week_end_date >= target_date,
                MealPlan.is_active == True
            )
        )
        result = await session.execute(stmt)
        meal_plan = result.scalar_one_or_none()

        if not meal_plan:
            return {
                "success": False,
                "message": f"No meal plan found for {target_date.strftime('%A, %B %d')}."
            }

        # Find the item to remove
        stmt = select(MealPlanItem).where(
            and_(
                MealPlanItem.meal_plan_id == meal_plan.id,
                MealPlanItem.date == target_date,
                MealPlanItem.meal_type == meal_type_lower,
                MealPlanItem.recipe_name.ilike(f"%{recipe_name}%")
            )
        )
        result = await session.execute(stmt)
        item = result.scalars().first()

        if not item:
            return {
                "success": False,
                "message": f"Couldn't find '{recipe_name}' in {meal_type.lower()} on {target_date.strftime('%A')}."
            }

        if item.is_locked:
            return {
                "success": False,
                "message": f"'{item.recipe_name}' is locked and cannot be removed."
            }

        removed_name = item.recipe_name
        await session.delete(item)
        await session.commit()

        return {
            "success": True,
            "message": f"Removed {removed_name} from {meal_type.lower()} on {target_date.strftime('%A, %B %d')}.",
            "recipe_name": removed_name
        }
