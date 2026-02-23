"""User endpoints."""

from fastapi import APIRouter

from app.api.deps import CurrentUser, DbSession
from app.schemas.user import UserPreferencesUpdate, UserResponse
from app.services.user_deletion_service import export_user_data, soft_delete_user
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


@router.delete("/me")
async def delete_current_user(
    db: DbSession,
    current_user: CurrentUser,
) -> dict:
    """Delete the current user's account (soft delete).

    Sets the account as inactive and schedules permanent deletion after 30 days.
    """
    return await soft_delete_user(db, current_user.id)


@router.get("/me/export")
async def export_current_user_data(
    db: DbSession,
    current_user: CurrentUser,
) -> dict:
    """Export all data for the current user (GDPR data portability).

    Returns a JSON object containing all user data: profile, preferences,
    family members, meal plans, recipe rules, chat messages, grocery lists,
    and notification counts.
    """
    return await export_user_data(db, current_user.id)
