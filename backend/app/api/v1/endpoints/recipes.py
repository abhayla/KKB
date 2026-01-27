"""Recipe endpoints."""

from fastapi import APIRouter, Query

from app.api.deps import CurrentUser, DbSession
from app.schemas.recipe import RecipeResponse, RecipeSearchParams
from app.services.recipe_service import get_recipe_by_id, scale_recipe, search_recipes

router = APIRouter(prefix="/recipes", tags=["recipes"])


@router.get("/search", response_model=list[RecipeResponse])
async def search(
    db: DbSession,
    current_user: CurrentUser,
    q: str = Query(default="", description="Search query"),
    cuisine: str | None = Query(default=None, description="Cuisine filter"),
    dietary: str | None = Query(default=None, description="Dietary tag filter"),
    mealType: str | None = Query(default=None, alias="mealType", description="Meal type filter"),
    page: int = Query(default=1, ge=1, description="Page number"),
    limit: int = Query(default=20, ge=1, le=100, description="Items per page"),
) -> list[RecipeResponse]:
    """Search recipes with optional filters.

    - **q**: Text search in recipe name and description
    - **cuisine**: Filter by cuisine type (north, south, east, west)
    - **dietary**: Filter by dietary tag (vegetarian, vegan, jain, etc.)
    - **mealType**: Filter by meal type (breakfast, lunch, dinner, snacks)
    """
    params = RecipeSearchParams(
        q=q,
        cuisine=cuisine,
        dietary=dietary,
        meal_type=mealType,
        page=page,
        limit=limit,
    )
    return await search_recipes(db, params)


@router.get("/{recipe_id}", response_model=RecipeResponse)
async def get_by_id(
    recipe_id: str,
    db: DbSession,
    current_user: CurrentUser,
) -> RecipeResponse:
    """Get a specific recipe by ID."""
    return await get_recipe_by_id(db, recipe_id)


@router.get("/{recipe_id}/scale", response_model=RecipeResponse)
async def scale(
    recipe_id: str,
    servings: int,
    db: DbSession,
    current_user: CurrentUser,
) -> RecipeResponse:
    """Scale a recipe to a target number of servings.

    Adjusts all ingredient quantities and nutrition values proportionally.
    """
    return await scale_recipe(db, recipe_id, servings)
