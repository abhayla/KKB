"""Authentication service using Firestore."""

import logging
from datetime import timedelta

from app.config import settings
from sqlalchemy.exc import IntegrityError

from app.core.exceptions import AuthenticationError, ConflictError
from app.core.firebase import verify_firebase_token
from app.core.security import create_access_token, decode_access_token
from app.repositories.user_repository import UserRepository
from app.schemas.auth import AuthResponse, RefreshTokenResponse, UserResponseForAuth
from app.schemas.user import UserPreferencesDto

logger = logging.getLogger(__name__)


async def authenticate_with_firebase(firebase_token: str) -> AuthResponse:
    """Authenticate user with Firebase token and return JWT.

    Args:
        firebase_token: Firebase ID token from client

    Returns:
        AuthResponse with JWT tokens and user info
    """
    # Verify Firebase token
    firebase_user = verify_firebase_token(firebase_token)
    firebase_uid = firebase_user["uid"]

    # Find or create user in Firestore
    user_repo = UserRepository()
    user = await user_repo.get_by_firebase_uid(firebase_uid)

    if not user:
        email = firebase_user.get("email")
        if email:
            existing = await user_repo.get_by_email(email)
            if existing:
                # Same person, different Firebase UID → merge account
                await user_repo.update_firebase_uid(existing["id"], firebase_uid)
                user = existing
                user["firebase_uid"] = firebase_uid
                logger.info(f"Merged Firebase UID for user {user['id']}")

        if not user:
            # Create new user
            try:
                user = await user_repo.create(
                    firebase_uid=firebase_uid,
                    email=email,
                    name=firebase_user.get("name"),
                    profile_picture_url=firebase_user.get("picture"),
                )
            except IntegrityError:
                raise ConflictError(
                    f"An account with email '{email.strip().lower() if email else ''}' already exists"
                )

    # Create JWT tokens
    access_token = create_access_token(
        data={"sub": user["id"]},
        expires_delta=timedelta(minutes=settings.access_token_expire_minutes),
    )

    # For simplicity, refresh token is same as access token with longer expiry
    refresh_token = create_access_token(
        data={"sub": user["id"], "type": "refresh"},
        expires_delta=timedelta(days=30),
    )

    # Get preferences if exists
    preferences = await user_repo.get_preferences(user["id"])
    preferences_dto = None
    if preferences:
        preferences_dto = UserPreferencesDto(
            household_size=preferences.get("family_size") or 2,
            dietary_restrictions=preferences.get("dietary_tags") or [],
            cuisine_preferences=preferences.get("cuisine_preferences") or [],
            disliked_ingredients=preferences.get("disliked_ingredients") or [],
            cooking_time_preference=preferences.get("cooking_time_preference") or "moderate",
            spice_level=preferences.get("spice_level") or "medium",
        )

    # Build user response
    user_response = UserResponseForAuth(
        id=user["id"],
        email=user.get("email") or "",
        name=user.get("name") or "",
        profile_image_url=user.get("profile_picture_url"),
        is_onboarded=user.get("is_onboarded", False),
        preferences=preferences_dto,
    )

    return AuthResponse(
        access_token=access_token,
        refresh_token=refresh_token,
        token_type="bearer",
        expires_in=settings.access_token_expire_minutes * 60,
        user=user_response,
    )


async def refresh_access_token(refresh_token: str) -> RefreshTokenResponse:
    """Refresh an access token using a valid refresh token.

    Args:
        refresh_token: The refresh token from initial authentication

    Returns:
        RefreshTokenResponse with new access token

    Raises:
        AuthenticationError: If refresh token is invalid or not a refresh token type
    """
    # Decode and validate the refresh token
    payload = decode_access_token(refresh_token)

    # Verify this is actually a refresh token
    if payload.get("type") != "refresh":
        raise AuthenticationError("Invalid token type: expected refresh token")

    user_id = payload.get("sub")
    if not user_id:
        raise AuthenticationError("Token missing user ID")

    # Verify user still exists
    user_repo = UserRepository()
    user = await user_repo.get_by_id(user_id)
    if not user:
        raise AuthenticationError("User not found")

    # Create new access token
    new_access_token = create_access_token(
        data={"sub": user_id},
        expires_delta=timedelta(minutes=settings.access_token_expire_minutes),
    )

    return RefreshTokenResponse(
        access_token=new_access_token,
        token_type="bearer",
        expires_in=settings.access_token_expire_minutes * 60,
    )
