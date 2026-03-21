"""Favorite service — business logic for user recipe favorites."""

import logging
import uuid

from sqlalchemy import delete, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import ConflictError, NotFoundError
from app.models.favorite import Favorite
from app.schemas.favorite import FavoriteListResponse, FavoriteResponse

logger = logging.getLogger(__name__)


async def add_favorite(
    db: AsyncSession, user_id: str, recipe_id: str
) -> FavoriteResponse:
    """Add a recipe to user's favorites. Raises ConflictError on duplicate."""
    existing = await db.execute(
        select(Favorite).where(
            Favorite.user_id == user_id, Favorite.recipe_id == recipe_id
        )
    )
    if existing.scalar_one_or_none():
        raise ConflictError(f"Recipe {recipe_id} is already a favorite")

    favorite = Favorite(
        id=str(uuid.uuid4()),
        user_id=user_id,
        recipe_id=recipe_id,
    )
    db.add(favorite)
    await db.commit()
    await db.refresh(favorite)
    logger.info(f"User {user_id} added favorite recipe {recipe_id}")
    return FavoriteResponse.model_validate(favorite)


async def remove_favorite(
    db: AsyncSession, user_id: str, recipe_id: str
) -> None:
    """Remove a recipe from user's favorites. Raises NotFoundError if not found."""
    result = await db.execute(
        select(Favorite).where(
            Favorite.user_id == user_id, Favorite.recipe_id == recipe_id
        )
    )
    favorite = result.scalar_one_or_none()
    if not favorite:
        raise NotFoundError(f"Favorite for recipe {recipe_id} not found")

    await db.execute(
        delete(Favorite).where(
            Favorite.user_id == user_id, Favorite.recipe_id == recipe_id
        )
    )
    await db.commit()
    logger.info(f"User {user_id} removed favorite recipe {recipe_id}")


async def get_favorites(
    db: AsyncSession, user_id: str
) -> FavoriteListResponse:
    """Get all favorites for a user."""
    result = await db.execute(
        select(Favorite).where(Favorite.user_id == user_id)
    )
    favorites = result.scalars().all()
    return FavoriteListResponse(
        favorites=[FavoriteResponse.model_validate(f) for f in favorites],
        total=len(favorites),
    )


async def is_favorite(
    db: AsyncSession, user_id: str, recipe_id: str
) -> bool:
    """Check if a recipe is in user's favorites."""
    result = await db.execute(
        select(Favorite).where(
            Favorite.user_id == user_id, Favorite.recipe_id == recipe_id
        )
    )
    return result.scalar_one_or_none() is not None
