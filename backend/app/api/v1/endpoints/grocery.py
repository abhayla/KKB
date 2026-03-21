"""Grocery list endpoints."""

from fastapi import APIRouter, Query

from app.api.deps import CurrentUser, DbSession
from app.schemas.grocery import GroceryListResponse, WhatsAppGroceryResponse
from app.services.grocery_service import (
    get_grocery_list_for_meal_plan,
    get_grocery_list_whatsapp,
)

router = APIRouter(prefix="/grocery", tags=["grocery"])


@router.get("", response_model=GroceryListResponse)
async def get_grocery_list(
    db: DbSession,
    current_user: CurrentUser,
    mealPlanId: str | None = Query(default=None, alias="mealPlanId"),
    scope: str = Query("personal", description="Data scope: personal or family"),
) -> GroceryListResponse:
    """Get aggregated grocery list for a meal plan.

    If no meal plan ID is provided, uses the current week's plan.
    Items are grouped by category and aggregated across all meals.
    When scope=family, uses household meal plan if available.
    Falls back to personal data if user has no household.
    """
    return await get_grocery_list_for_meal_plan(db, current_user, mealPlanId)


@router.get("/whatsapp")
async def get_whatsapp_format(
    mealPlanId: str,
    db: DbSession,
    current_user: CurrentUser,
) -> str:
    """Get grocery list formatted for WhatsApp sharing.

    Returns a text format with emojis suitable for sharing via WhatsApp
    to local kirana stores.
    """
    result = await get_grocery_list_whatsapp(db, current_user, mealPlanId)
    return result.formatted_text
