"""Authentication service."""

import uuid
from datetime import timedelta

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.config import settings
from app.core.firebase import verify_firebase_token
from app.core.security import create_access_token
from app.models.user import User, UserPreferences
from app.schemas.auth import AuthResponse, UserResponseForAuth
from app.schemas.user import UserPreferencesDto


async def authenticate_with_firebase(
    db: AsyncSession,
    firebase_token: str,
) -> AuthResponse:
    """Authenticate user with Firebase token and return JWT.

    Args:
        db: Database session
        firebase_token: Firebase ID token from client

    Returns:
        AuthResponse with JWT tokens and user info
    """
    # Verify Firebase token
    firebase_user = verify_firebase_token(firebase_token)
    firebase_uid = firebase_user["uid"]

    # Find or create user
    result = await db.execute(
        select(User)
        .options(selectinload(User.preferences))
        .where(User.firebase_uid == firebase_uid)
    )
    user = result.scalar_one_or_none()

    if not user:
        # Create new user
        user = User(
            firebase_uid=firebase_uid,
            email=firebase_user.get("email"),
            name=firebase_user.get("name"),
            profile_picture_url=firebase_user.get("picture"),
            is_onboarded=False,
        )
        db.add(user)
        await db.commit()
        await db.refresh(user)

    # Create JWT tokens
    access_token = create_access_token(
        data={"sub": str(user.id)},
        expires_delta=timedelta(minutes=settings.access_token_expire_minutes),
    )

    # For simplicity, refresh token is same as access token with longer expiry
    refresh_token = create_access_token(
        data={"sub": str(user.id), "type": "refresh"},
        expires_delta=timedelta(days=30),
    )

    # Build preferences DTO
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

    # Build user response
    user_response = UserResponseForAuth(
        id=str(user.id),
        email=user.email or "",
        name=user.name or "",
        profile_image_url=user.profile_picture_url,
        is_onboarded=user.is_onboarded,
        preferences=preferences_dto,
    )

    return AuthResponse(
        access_token=access_token,
        refresh_token=refresh_token,
        token_type="bearer",
        expires_in=settings.access_token_expire_minutes * 60,
        user=user_response,
    )
