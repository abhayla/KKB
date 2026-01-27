"""Meal plan endpoints."""

from fastapi import APIRouter

from app.ai.meal_planner import generate_meal_plan
from app.api.deps import CurrentUser, DbSession
from app.schemas.meal_plan import (
    GenerateMealPlanRequest,
    MealPlanResponse,
    SwapMealRequest,
)
from app.services.meal_plan_service import (
    get_current_meal_plan,
    get_meal_plan_by_id,
    lock_meal_item,
    swap_meal_item,
)

router = APIRouter(prefix="/meal-plans", tags=["meal-plans"])


@router.post("/generate", response_model=MealPlanResponse)
async def generate(
    request: GenerateMealPlanRequest,
    db: DbSession,
    current_user: CurrentUser,
) -> MealPlanResponse:
    """Generate a new meal plan using AI.

    Creates a personalized 7-day meal plan based on user preferences,
    dietary restrictions, and any festivals during the week.
    """
    return await generate_meal_plan(db, current_user, request)


@router.get("/current", response_model=MealPlanResponse)
async def get_current(
    db: DbSession,
    current_user: CurrentUser,
) -> MealPlanResponse:
    """Get the current week's meal plan."""
    return await get_current_meal_plan(db, current_user)


@router.get("/{plan_id}", response_model=MealPlanResponse)
async def get_by_id(
    plan_id: str,
    db: DbSession,
    current_user: CurrentUser,
) -> MealPlanResponse:
    """Get a specific meal plan by ID."""
    return await get_meal_plan_by_id(db, current_user, plan_id)


@router.post("/{plan_id}/items/{item_id}/swap", response_model=MealPlanResponse)
async def swap_item(
    plan_id: str,
    item_id: str,
    request: SwapMealRequest,
    db: DbSession,
    current_user: CurrentUser,
) -> MealPlanResponse:
    """Swap a meal item with an alternative recipe.

    Can optionally specify a specific recipe or let the AI choose.
    """
    return await swap_meal_item(db, current_user, plan_id, item_id, request)


@router.put("/{plan_id}/items/{item_id}/lock", response_model=MealPlanResponse)
async def toggle_lock(
    plan_id: str,
    item_id: str,
    db: DbSession,
    current_user: CurrentUser,
) -> MealPlanResponse:
    """Toggle the lock status of a meal item.

    Locked meals won't be changed when regenerating the plan.
    """
    return await lock_meal_item(db, current_user, plan_id, item_id)
