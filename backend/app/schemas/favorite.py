"""Pydantic schemas for favorites."""

from datetime import datetime

from pydantic import BaseModel, ConfigDict


class FavoriteCreate(BaseModel):
    recipe_id: str


class FavoriteResponse(BaseModel):
    id: str
    user_id: str
    recipe_id: str
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)


class FavoriteListResponse(BaseModel):
    favorites: list[FavoriteResponse]
    total: int
