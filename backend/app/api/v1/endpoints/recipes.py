"""Recipe endpoints."""

from typing import Any

from fastapi import APIRouter, Body, Query
from sqlalchemy import select

from app.api.deps import CurrentUser, DbSession
from app.models.user import UserPreferences
from app.schemas.recipe import (
    AiRecipeCatalogResponse,
    RecipeRatingRequest,
    RecipeRatingResponse,
    RecipeResponse,
    RecipeSearchParams,
)
from app.services.ai_recipe_catalog_service import search_catalog
from app.services.recipe_service import get_recipe_by_id, rate_recipe, scale_recipe, search_recipes, suggest_from_pantry

router = APIRouter(prefix="/recipes", tags=["recipes"])


@router.get("/ai-catalog/search", response_model=list[AiRecipeCatalogResponse])
async def search_ai_catalog(
    db: DbSession,
    current_user: CurrentUser,
    q: str = Query(default="", description="Search query"),
    favorites: str = Query(default="", description="Comma-separated favorite recipe names"),
    limit: int = Query(default=10, ge=1, le=50),
) -> list[AiRecipeCatalogResponse]:
    """Search the AI recipe catalog for recipe names.

    Returns recipes filtered by dietary compatibility and sorted with
    favorites first, then by popularity (usage_count).

    - **q**: Text search in recipe name
    - **favorites**: Comma-separated list of favorite recipe names for priority sorting
    - **limit**: Maximum results (1-50, default 10)
    """
    # Load user dietary preferences
    user_dietary_tags = []
    result = await db.execute(
        select(UserPreferences).where(UserPreferences.user_id == current_user.id)
    )
    prefs = result.scalar_one_or_none()
    if prefs and prefs.dietary_type:
        user_dietary_tags = [prefs.dietary_type]

    # Parse favorites
    favorite_names = [n.strip() for n in favorites.split(",") if n.strip()] if favorites else []

    results = await search_catalog(
        db=db,
        query=q,
        user_dietary_tags=user_dietary_tags,
        favorite_names=favorite_names,
        limit=limit,
    )

    return [AiRecipeCatalogResponse(**r) for r in results]


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


@router.post("/suggest-from-pantry")
async def suggest_recipes_from_pantry(
    db: DbSession,
    current_user: CurrentUser,
    ingredients: list[str] = Body(..., description="List of pantry ingredient names"),
    limit: int = Query(default=10, ge=1, le=50, description="Max suggestions"),
) -> list[dict[str, Any]]:
    """Suggest recipes based on available pantry ingredients.

    Matches pantry ingredients against recipe ingredient lists and returns
    recipes sorted by match percentage (highest first).

    Each result includes:
    - **recipe**: Full recipe details
    - **match_percentage**: How many recipe ingredients are in your pantry
    - **missing_ingredients**: Ingredients you'd need to buy
    """
    return await suggest_from_pantry(db, ingredients, limit)


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


@router.post("/{recipe_id}/rate", response_model=RecipeRatingResponse)
async def rate(
    recipe_id: str,
    body: RecipeRatingRequest,
    db: DbSession,
    current_user: CurrentUser,
) -> RecipeRatingResponse:
    """Rate a recipe. Creates or updates the user's rating for this recipe.

    - **rating**: Score from 1.0 to 5.0
    - **feedback**: Optional text feedback
    """
    return await rate_recipe(
        db=db,
        recipe_id=recipe_id,
        user_id=current_user.id,
        rating=body.rating,
        feedback=body.feedback,
    )
