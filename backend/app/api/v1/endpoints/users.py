"""User endpoints."""

from fastapi import APIRouter

from app.api.deps import CurrentUser, DbSession
from app.schemas.user import UserPreferencesUpdate, UserResponse
from app.services.user_service import get_user_with_preferences, update_user_preferences

router = APIRouter(prefix="/users", tags=["users"])


@router.get("/me", response_model=UserResponse)
async def get_current_user(
    db: DbSession,
    current_user: CurrentUser,
) -> UserResponse:
    """Get the current authenticated user."""
    return await get_user_with_preferences(db, current_user)


@router.put("/preferences", response_model=UserResponse)
async def update_preferences(
    preferences: UserPreferencesUpdate,
    db: DbSession,
    current_user: CurrentUser,
) -> UserResponse:
    """Update current user's preferences.

    This also marks the user as onboarded if not already.
    """
    return await update_user_preferences(db, current_user, preferences)
