"""User service for user operations."""

from typing import Optional

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.models.user import User, UserPreferences
from app.schemas.user import UserPreferencesDto, UserPreferencesUpdate, UserResponse


def build_user_response(user: User) -> UserResponse:
    """Build UserResponse from User model.

    Args:
        user: User model with preferences loaded

    Returns:
        UserResponse schema
    """
    preferences_dto = None
    if user.preferences:
        preferences_dto = UserPreferencesDto(
            household_size=user.preferences.family_size,
            dietary_restrictions=user.preferences.dietary_tags or [],
            cuisine_preferences=user.preferences.cuisine_preferences or [],
            disliked_ingredients=user.preferences.disliked_ingredients or [],
            cooking_time_preference=user.preferences.cooking_time_preference or "moderate",
            spice_level=user.preferences.spice_level or "medium",
        )

    return UserResponse(
        id=str(user.id),
        email=user.email or "",
        name=user.name or "",
        profile_image_url=user.profile_picture_url,
        is_onboarded=user.is_onboarded,
        preferences=preferences_dto,
    )


async def get_user_with_preferences(
    db: AsyncSession,
    user: User,
) -> UserResponse:
    """Get user with preferences loaded.

    Args:
        db: Database session
        user: User model

    Returns:
        UserResponse with preferences
    """
    # Reload user with preferences if not already loaded
    result = await db.execute(
        select(User)
        .options(selectinload(User.preferences))
        .where(User.id == user.id)
    )
    user = result.scalar_one()
    return build_user_response(user)


async def update_user_preferences(
    db: AsyncSession,
    user: User,
    preferences_update: UserPreferencesUpdate,
) -> UserResponse:
    """Update user preferences.

    Args:
        db: Database session
        user: User model
        preferences_update: Preferences to update

    Returns:
        Updated UserResponse
    """
    # Get or create preferences
    result = await db.execute(
        select(UserPreferences).where(UserPreferences.user_id == user.id)
    )
    preferences = result.scalar_one_or_none()

    if not preferences:
        preferences = UserPreferences(user_id=user.id)
        db.add(preferences)

    # Update fields
    if preferences_update.household_size is not None:
        preferences.family_size = preferences_update.household_size
    if preferences_update.dietary_restrictions is not None:
        preferences.dietary_tags = preferences_update.dietary_restrictions
    if preferences_update.cuisine_preferences is not None:
        preferences.cuisine_preferences = preferences_update.cuisine_preferences
    if preferences_update.disliked_ingredients is not None:
        preferences.disliked_ingredients = preferences_update.disliked_ingredients
    if preferences_update.cooking_time_preference is not None:
        preferences.cooking_time_preference = preferences_update.cooking_time_preference
    if preferences_update.spice_level is not None:
        preferences.spice_level = preferences_update.spice_level

    # Mark user as onboarded if not already
    if not user.is_onboarded:
        user.is_onboarded = True

    await db.commit()

    # Reload and return
    return await get_user_with_preferences(db, user)
