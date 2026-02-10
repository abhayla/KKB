"""Meal plan endpoints using Firestore repositories."""

import asyncio
import logging
import random
import uuid
from datetime import date, datetime, timedelta, timezone
from typing import Any, Optional

from fastapi import APIRouter, HTTPException

from app.api.deps import CurrentUser
from app.core.exceptions import NotFoundError
from app.repositories.meal_plan_repository import MealPlanRepository
from app.repositories.recipe_repository import RecipeRepository
from app.repositories.user_repository import UserRepository
from app.schemas.meal_plan import (
    GenerateMealPlanRequest,
    MealPlanResponse,
    MealPlanDayDto,
    MealsByTypeDto,
    MealItemDto,
    FestivalDto,
    SwapMealRequest,
)
from app.services.ai_meal_service import AIMealService

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/meal-plans", tags=["meal-plans"])


# ==============================================================================
# RESPONSE BUILDING HELPERS
# ==============================================================================

def _build_response_from_firestore(plan: dict[str, Any]) -> MealPlanResponse:
    """Build MealPlanResponse from Firestore document."""
    days = []
    for day_data in plan.get("days", []):
        meals = day_data.get("meals", {})

        def _build_meal_items(meal_list: list) -> list[MealItemDto]:
            items = []
            for i, m in enumerate(meal_list or []):
                items.append(MealItemDto(
                    id=m.get("id", str(uuid.uuid4())),
                    recipe_id=m.get("recipe_id", ""),
                    recipe_name=m.get("recipe_name", ""),
                    recipe_image_url=m.get("recipe_image_url"),
                    prep_time_minutes=m.get("prep_time_minutes", 30),
                    calories=m.get("calories", 0),
                    is_locked=m.get("is_locked", False),
                    order=i,
                    dietary_tags=m.get("dietary_tags", []),
                ))
            return items

        festival = None
        if day_data.get("festival"):
            f = day_data["festival"]
            festival = FestivalDto(
                id=f.get("id", ""),
                name=f.get("name", ""),
                is_fasting_day=f.get("is_fasting_day", False),
                suggested_dishes=f.get("suggested_dishes"),
            )

        days.append(MealPlanDayDto(
            date=day_data.get("date", ""),
            day_name=day_data.get("day_name", ""),
            meals=MealsByTypeDto(
                breakfast=_build_meal_items(meals.get("breakfast", [])),
                lunch=_build_meal_items(meals.get("lunch", [])),
                dinner=_build_meal_items(meals.get("dinner", [])),
                snacks=_build_meal_items(meals.get("snacks", [])),
            ),
            festival=festival,
        ))

    created_at = plan.get("created_at")
    updated_at = plan.get("updated_at")

    # Convert date objects to ISO strings for response
    week_start = plan.get("week_start_date", "")
    week_end = plan.get("week_end_date", "")
    if isinstance(week_start, date):
        week_start = week_start.isoformat()
    if isinstance(week_end, date):
        week_end = week_end.isoformat()

    return MealPlanResponse(
        id=plan.get("id", ""),
        week_start_date=week_start,
        week_end_date=week_end,
        days=days,
        created_at=created_at.isoformat() if isinstance(created_at, datetime) else str(created_at),
        updated_at=updated_at.isoformat() if isinstance(updated_at, datetime) else str(updated_at),
    )


@router.post("/generate", response_model=MealPlanResponse)
async def generate(
    request: GenerateMealPlanRequest,
    current_user: CurrentUser,
) -> MealPlanResponse:
    """Generate a new meal plan with paired recipes.

    Creates a personalized 7-day meal plan based on user preferences,
    dietary restrictions, recipe rules, and available recipes in Firestore.

    Each meal slot contains 2 complementary items (e.g., Dal + Rice) by default.

    Enforces:
    - EXCLUDE rules (never include certain ingredients/recipes)
    - INCLUDE rules (must include certain items at specified frequency, paired with complements)
    - Allergies (always excluded)
    - Dislikes (always excluded)
    - Cooking time limits (weekday vs weekend, busy days)
    - Pairing rules from config (by cuisine and meal type)
    """
    import traceback
    try:
        user_id = current_user.id
        logger.info(f"Generating meal plan for user {user_id}")

        # Parse week start date
        try:
            week_start = date.fromisoformat(request.week_start_date)
        except ValueError:
            week_start = date.today()
            week_start = week_start - timedelta(days=week_start.weekday())

        # Generate meal plan using the AI service (with 60s timeout)
        ai_service = AIMealService()
        try:
            generated_plan = await asyncio.wait_for(
                ai_service.generate_meal_plan(
                    user_id=user_id,
                    week_start_date=week_start,
                ),
                timeout=60,
            )
        except asyncio.TimeoutError:
            logger.error(f"Meal generation timed out after 60s for user {user_id}")
            raise HTTPException(
                status_code=504,
                detail="Meal generation timed out. Please try again.",
            )

        # Convert to repository format
        # Repository expects lists of items per meal type
        days = []
        for day in generated_plan.days:
            days.append({
                "date": day.date,
                "day_name": day.day_name,
                "festival": day.festival,
                "breakfast": [_meal_item_to_dict(item) for item in day.breakfast],
                "lunch": [_meal_item_to_dict(item) for item in day.lunch],
                "dinner": [_meal_item_to_dict(item) for item in day.dinner],
                "snacks": [_meal_item_to_dict(item) for item in day.snacks],
            })

        # Save to PostgreSQL
        meal_plan_repo = MealPlanRepository()
        await meal_plan_repo.deactivate_old_plans(user_id, "")

        plan_data = {
            "user_id": user_id,
            "week_start_date": date.fromisoformat(generated_plan.week_start_date),
            "week_end_date": date.fromisoformat(generated_plan.week_end_date),
            "days": days,
            "rules_applied": generated_plan.rules_applied,
        }

        created_plan = await meal_plan_repo.create(plan_data)
        logger.info(f"Created meal plan {created_plan.get('id')} for user {user_id}")

        # Catalog AI-generated recipes into the shared catalog
        try:
            from app.db.postgres import async_session_maker
            from app.services.ai_recipe_catalog_service import catalog_recipes as catalog_recipes_fn

            # Get user cuisine preference
            user_repo = UserRepository()
            prefs_data = await user_repo.get_preferences(user_id)
            cuisine_type = "north"
            if prefs_data and prefs_data.get("cuisine_preferences"):
                cuisine_type = prefs_data["cuisine_preferences"][0]

            async with async_session_maker() as catalog_db:
                await catalog_recipes_fn(
                    db=catalog_db,
                    user_id=user_id,
                    generated_plan={"days": days},
                    cuisine_type=cuisine_type,
                )
        except Exception as e:
            # Non-critical: don't fail meal plan creation if cataloging fails
            logger.warning(f"Failed to catalog recipes: {e}")

        return _build_response_from_firestore(created_plan)
    except Exception as e:
        logger.error(f"Error generating meal plan: {e}")
        logger.error(traceback.format_exc())
        raise


def _meal_item_to_dict(item) -> dict:
    """Convert MealItem dataclass to dictionary."""
    d = {
        "id": item.id,
        "recipe_id": item.recipe_id,
        "recipe_name": item.recipe_name,
        "recipe_image_url": item.recipe_image_url,
        "prep_time_minutes": item.prep_time_minutes,
        "calories": item.calories,
        "is_locked": item.is_locked,
        "dietary_tags": item.dietary_tags,
    }
    # Include rich data for catalog (ingredients/nutrition may be None)
    if hasattr(item, "ingredients") and item.ingredients:
        d["ingredients"] = item.ingredients
    if hasattr(item, "nutrition") and item.nutrition:
        d["nutrition"] = item.nutrition
    if hasattr(item, "category"):
        d["category"] = item.category
    return d


@router.get("/current", response_model=MealPlanResponse)
async def get_current(
    current_user: CurrentUser,
) -> MealPlanResponse:
    """Get the current week's meal plan from Firestore."""
    user_id = current_user.id

    meal_plan_repo = MealPlanRepository()
    plan = await meal_plan_repo.get_current_for_user(user_id)

    if not plan:
        raise NotFoundError("No meal plan found for current week")

    return _build_response_from_firestore(plan)


@router.get("/{plan_id}", response_model=MealPlanResponse)
async def get_by_id(
    plan_id: str,
    current_user: CurrentUser,
) -> MealPlanResponse:
    """Get a specific meal plan by ID from Firestore."""
    user_id = current_user.id

    meal_plan_repo = MealPlanRepository()
    plan = await meal_plan_repo.get_by_id(plan_id)

    if not plan:
        raise NotFoundError("Meal plan not found")

    if plan.get("user_id") != user_id:
        raise NotFoundError("Meal plan not found")

    return _build_response_from_firestore(plan)


@router.post("/{plan_id}/items/{item_id}/swap", response_model=MealPlanResponse)
async def swap_item(
    plan_id: str,
    item_id: str,
    request: SwapMealRequest,
    current_user: CurrentUser,
) -> MealPlanResponse:
    """Swap a meal item with an alternative recipe.

    Can optionally specify a specific recipe or let the system choose randomly.
    """
    user_id = current_user.id

    meal_plan_repo = MealPlanRepository()
    plan = await meal_plan_repo.get_by_id(plan_id)

    if not plan or plan.get("user_id") != user_id:
        raise NotFoundError("Meal plan not found")

    # Find the item to swap
    days = plan.get("days", [])
    found = False

    for day_idx, day in enumerate(days):
        for meal_type in ["breakfast", "lunch", "dinner", "snacks"]:
            meals = day.get("meals", {}).get(meal_type, [])
            for meal_idx, meal in enumerate(meals):
                if meal.get("id") == item_id:
                    if meal.get("is_locked"):
                        raise NotFoundError("Cannot swap a locked meal")

                    # Get new recipe
                    recipe_repo = RecipeRepository()
                    if request.specific_recipe_id:
                        new_recipe = await recipe_repo.get_by_id(request.specific_recipe_id)
                    else:
                        recipes = await recipe_repo.search(meal_type=meal_type, limit=20)
                        exclude_ids = set(request.exclude_recipe_ids or [])
                        exclude_ids.add(meal.get("recipe_id", ""))
                        recipes = [r for r in recipes if r.get("id") not in exclude_ids]
                        new_recipe = random.choice(recipes) if recipes else None

                    if not new_recipe:
                        raise NotFoundError("No alternative recipe found")

                    # Update the meal
                    days[day_idx]["meals"][meal_type][meal_idx] = {
                        "id": str(uuid.uuid4()),
                        "recipe_id": new_recipe.get("id", ""),
                        "recipe_name": new_recipe.get("name", ""),
                        "recipe_image_url": new_recipe.get("image_url"),
                        "prep_time_minutes": new_recipe.get("prep_time_minutes", 30),
                        "calories": new_recipe.get("nutrition", {}).get("calories", 0) if new_recipe.get("nutrition") else 0,
                        "is_locked": False,
                        "dietary_tags": new_recipe.get("dietary_tags", []),
                    }
                    found = True
                    break
            if found:
                break
        if found:
            break

    if not found:
        raise NotFoundError("Meal item not found")

    updated_plan = await meal_plan_repo.update(plan_id, {"days": days})
    return _build_response_from_firestore(updated_plan)


@router.put("/{plan_id}/items/{item_id}/lock", response_model=MealPlanResponse)
async def toggle_lock(
    plan_id: str,
    item_id: str,
    current_user: CurrentUser,
) -> MealPlanResponse:
    """Toggle the lock status of a meal item.

    Locked meals won't be changed when regenerating the plan.
    """
    user_id = current_user.id

    meal_plan_repo = MealPlanRepository()
    plan = await meal_plan_repo.get_by_id(plan_id)

    if not plan or plan.get("user_id") != user_id:
        raise NotFoundError("Meal plan not found")

    # Find and toggle lock on item
    days = plan.get("days", [])
    found = False

    for day_idx, day in enumerate(days):
        for meal_type in ["breakfast", "lunch", "dinner", "snacks"]:
            meals = day.get("meals", {}).get(meal_type, [])
            for meal_idx, meal in enumerate(meals):
                if meal.get("id") == item_id:
                    days[day_idx]["meals"][meal_type][meal_idx]["is_locked"] = not meal.get("is_locked", False)
                    found = True
                    break
            if found:
                break
        if found:
            break

    if not found:
        raise NotFoundError("Meal item not found")

    updated_plan = await meal_plan_repo.update(plan_id, {"days": days})
    return _build_response_from_firestore(updated_plan)


@router.delete("/{plan_id}/items/{item_id}", response_model=MealPlanResponse)
async def remove_item(
    plan_id: str,
    item_id: str,
    current_user: CurrentUser,
) -> MealPlanResponse:
    """Remove a meal item from the plan.

    Locked meals cannot be removed.
    """
    user_id = current_user.id

    meal_plan_repo = MealPlanRepository()
    plan = await meal_plan_repo.get_by_id(plan_id)

    if not plan or plan.get("user_id") != user_id:
        raise NotFoundError("Meal plan not found")

    # Find and remove the item
    days = plan.get("days", [])
    found = False

    for day_idx, day in enumerate(days):
        for meal_type in ["breakfast", "lunch", "dinner", "snacks"]:
            meals = day.get("meals", {}).get(meal_type, [])
            for meal_idx, meal in enumerate(meals):
                if meal.get("id") == item_id:
                    if meal.get("is_locked"):
                        raise NotFoundError("Cannot remove a locked meal")
                    # Remove the item from the list
                    days[day_idx]["meals"][meal_type].pop(meal_idx)
                    found = True
                    break
            if found:
                break
        if found:
            break

    if not found:
        raise NotFoundError("Meal item not found")

    updated_plan = await meal_plan_repo.update(plan_id, {"days": days})
    logger.info(f"Removed meal item {item_id} from plan {plan_id}")
    return _build_response_from_firestore(updated_plan)
