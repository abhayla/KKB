"""Meal plan endpoints using Firestore repositories."""

import logging
import random
import uuid
from datetime import date, datetime, timedelta, timezone
from typing import Any

from fastapi import APIRouter

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

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/meal-plans", tags=["meal-plans"])


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

    return MealPlanResponse(
        id=plan.get("id", ""),
        week_start_date=plan.get("week_start_date", ""),
        week_end_date=plan.get("week_end_date", ""),
        days=days,
        created_at=created_at.isoformat() if isinstance(created_at, datetime) else str(created_at),
        updated_at=updated_at.isoformat() if isinstance(updated_at, datetime) else str(updated_at),
    )


@router.post("/generate", response_model=MealPlanResponse)
async def generate(
    request: GenerateMealPlanRequest,
    current_user: CurrentUser,
) -> MealPlanResponse:
    """Generate a new meal plan using Firestore recipes.

    Creates a personalized 7-day meal plan based on user preferences,
    dietary restrictions, and available recipes in Firestore.
    """
    user_id = current_user.get("id")
    logger.info(f"Generating meal plan for user {user_id}")

    # Parse week start date
    try:
        week_start = date.fromisoformat(request.week_start_date)
    except ValueError:
        week_start = date.today()
        week_start = week_start - timedelta(days=week_start.weekday())

    week_end = week_start + timedelta(days=6)

    # Get user preferences
    user_repo = UserRepository()
    prefs = await user_repo.get_preferences(user_id)

    dietary_tags = []
    cuisine_type = None
    if prefs:
        dietary_tags = prefs.get("dietary_tags", ["vegetarian"])
        cuisines = prefs.get("cuisine_preferences", [])
        cuisine_type = cuisines[0].lower() if cuisines else None

    # Get recipes from Firestore
    recipe_repo = RecipeRepository()
    recipes = await recipe_repo.search(
        cuisine_type=cuisine_type,
        dietary_tags=dietary_tags if dietary_tags else ["vegetarian"],
        limit=200,
    )

    logger.info(f"Found {len(recipes)} matching recipes for meal plan generation")

    if not recipes:
        # Fallback: get any recipes
        recipes = await recipe_repo.get_all(limit=200)
        logger.warning(f"No filtered recipes, using all {len(recipes)} recipes")

    # Categorize recipes by meal type
    breakfast_recipes = [r for r in recipes if "breakfast" in r.get("meal_types", [])]
    lunch_recipes = [r for r in recipes if "lunch" in r.get("meal_types", [])]
    dinner_recipes = [r for r in recipes if "dinner" in r.get("meal_types", [])]
    snack_recipes = [r for r in recipes if "snacks" in r.get("meal_types", [])]

    # Fallback if categories are empty
    if not breakfast_recipes:
        breakfast_recipes = recipes[:50] if recipes else []
    if not lunch_recipes:
        lunch_recipes = recipes[:50] if recipes else []
    if not dinner_recipes:
        dinner_recipes = recipes[:50] if recipes else []
    if not snack_recipes:
        snack_recipes = recipes[:30] if recipes else []

    # Build 7-day meal plan
    days = []
    current_date = week_start

    for i in range(7):
        day_name = current_date.strftime("%A")

        # Select random recipes for each meal
        def _pick_recipe(recipe_list: list) -> dict:
            if not recipe_list:
                return {
                    "id": str(uuid.uuid4()),
                    "recipe_id": "",
                    "recipe_name": "No recipe available",
                    "recipe_image_url": None,
                    "prep_time_minutes": 30,
                    "calories": 0,
                    "is_locked": False,
                    "dietary_tags": [],
                }
            r = random.choice(recipe_list)
            return {
                "id": str(uuid.uuid4()),
                "recipe_id": r.get("id", ""),
                "recipe_name": r.get("name", "Unknown Recipe"),
                "recipe_image_url": r.get("image_url"),
                "prep_time_minutes": r.get("prep_time_minutes", 30),
                "calories": r.get("nutrition", {}).get("calories", 0) if r.get("nutrition") else 0,
                "is_locked": False,
                "dietary_tags": r.get("dietary_tags", []),
            }

        days.append({
            "date": current_date.isoformat(),
            "day_name": day_name,
            "meals": {
                "breakfast": [_pick_recipe(breakfast_recipes)],
                "lunch": [_pick_recipe(lunch_recipes)],
                "dinner": [_pick_recipe(dinner_recipes)],
                "snacks": [_pick_recipe(snack_recipes)],
            },
            "festival": None,
        })

        current_date += timedelta(days=1)

    # Create meal plan in Firestore
    meal_plan_repo = MealPlanRepository()

    # Deactivate old plans
    await meal_plan_repo.deactivate_old_plans(user_id, "")

    plan_data = {
        "user_id": user_id,
        "week_start_date": week_start.isoformat(),
        "week_end_date": week_end.isoformat(),
        "days": days,
    }

    created_plan = await meal_plan_repo.create(plan_data)
    logger.info(f"Created meal plan {created_plan.get('id')} for user {user_id}")

    return _build_response_from_firestore(created_plan)


@router.get("/current", response_model=MealPlanResponse)
async def get_current(
    current_user: CurrentUser,
) -> MealPlanResponse:
    """Get the current week's meal plan from Firestore."""
    user_id = current_user.get("id")

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
    user_id = current_user.get("id")

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
    user_id = current_user.get("id")

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
    user_id = current_user.get("id")

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
