"""Favorites API endpoints."""

from fastapi import APIRouter, Request

from app.api.deps import CurrentUser, DbSession
from app.schemas.favorite import FavoriteCreate, FavoriteListResponse, FavoriteResponse
from app.services import favorite_service

router = APIRouter(prefix="/favorites", tags=["Favorites"])


@router.post("", response_model=FavoriteResponse, status_code=201)
async def add_favorite(
    request: Request,
    body: FavoriteCreate,
    user: CurrentUser,
    db: DbSession,
) -> FavoriteResponse:
    """Add a recipe to the current user's favorites."""
    return await favorite_service.add_favorite(db, user.id, body.recipe_id)


@router.delete("/{recipe_id}", status_code=204)
async def remove_favorite(
    request: Request,
    recipe_id: str,
    user: CurrentUser,
    db: DbSession,
) -> None:
    """Remove a recipe from the current user's favorites."""
    await favorite_service.remove_favorite(db, user.id, recipe_id)


@router.get("", response_model=FavoriteListResponse)
async def get_favorites(
    request: Request,
    user: CurrentUser,
    db: DbSession,
) -> FavoriteListResponse:
    """Get all favorites for the current user."""
    return await favorite_service.get_favorites(db, user.id)
