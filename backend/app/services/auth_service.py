"""Authentication service using Firestore."""

from datetime import timedelta

from app.config import settings
from app.core.firebase import verify_firebase_token
from app.core.security import create_access_token
from app.repositories.user_repository import UserRepository
from app.schemas.auth import AuthResponse, UserResponseForAuth
from app.schemas.user import UserPreferencesDto


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
        # Create new user
        user = await user_repo.create(
            firebase_uid=firebase_uid,
            email=firebase_user.get("email"),
            name=firebase_user.get("name"),
            profile_picture_url=firebase_user.get("picture"),
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
            household_size=preferences.get("family_size", 2),
            dietary_restrictions=preferences.get("dietary_tags", []),
            cuisine_preferences=preferences.get("cuisine_preferences", []),
            disliked_ingredients=preferences.get("disliked_ingredients", []),
            cooking_time_preference=preferences.get("cooking_time_preference", "moderate"),
            spice_level=preferences.get("spice_level", "medium"),
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
