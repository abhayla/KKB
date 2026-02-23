"""Authentication service with refresh token rotation."""

import hashlib
import logging
import secrets
from datetime import datetime, timedelta, timezone

from sqlalchemy import select, update
from sqlalchemy.exc import IntegrityError

from app.config import settings
from app.core.exceptions import AuthenticationError, ConflictError
from app.core.firebase import verify_firebase_token
from app.core.security import create_access_token
from app.db.postgres import async_session_maker
from app.models.refresh_token import RefreshToken
from app.repositories.user_repository import UserRepository
from app.schemas.auth import AuthResponse, RefreshTokenResponse, UserResponseForAuth
from app.schemas.user import UserPreferencesDto

logger = logging.getLogger(__name__)


def _hash_token(token: str) -> str:
    """Hash a refresh token for storage."""
    return hashlib.sha256(token.encode()).hexdigest()


def _generate_refresh_token() -> str:
    """Generate a cryptographically secure opaque refresh token."""
    return secrets.token_urlsafe(48)


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

    # Create JWT access token
    access_token = create_access_token(
        data={"sub": user["id"]},
        expires_delta=timedelta(minutes=settings.access_token_expire_minutes),
    )

    # Create and store refresh token
    raw_refresh_token = _generate_refresh_token()
    token_hash = _hash_token(raw_refresh_token)
    expires_at = datetime.now(timezone.utc) + timedelta(days=30)

    async with async_session_maker() as db:
        rt = RefreshToken(
            user_id=user["id"],
            token_hash=token_hash,
            expires_at=expires_at,
        )
        db.add(rt)
        await db.commit()

    # Get preferences if exists
    preferences = await user_repo.get_preferences(user["id"])
    preferences_dto = None
    if preferences:
        preferences_dto = UserPreferencesDto(
            household_size=preferences.get("family_size") or 2,
            dietary_restrictions=preferences.get("dietary_tags") or [],
            cuisine_preferences=preferences.get("cuisine_preferences") or [],
            disliked_ingredients=preferences.get("disliked_ingredients") or [],
            cooking_time_preference=preferences.get("cooking_time_preference")
            or "moderate",
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
        refresh_token=raw_refresh_token,
        token_type="bearer",
        expires_in=settings.access_token_expire_minutes * 60,
        user=user_response,
    )


async def refresh_access_token(refresh_token: str) -> RefreshTokenResponse:
    """Refresh an access token using a valid refresh token.

    Implements token rotation: the old refresh token is revoked and
    a new one is issued alongside the new access token.

    Args:
        refresh_token: The opaque refresh token from initial authentication

    Returns:
        RefreshTokenResponse with new access token and new refresh token

    Raises:
        AuthenticationError: If refresh token is invalid, expired, or revoked
    """
    token_hash = _hash_token(refresh_token)

    async with async_session_maker() as db:
        # Find the token
        result = await db.execute(
            select(RefreshToken).where(RefreshToken.token_hash == token_hash)
        )
        stored_token = result.scalar_one_or_none()

        if not stored_token:
            raise AuthenticationError("Invalid refresh token")

        if stored_token.is_revoked:
            # Token reuse detected — revoke ALL tokens for this user (security measure)
            await db.execute(
                update(RefreshToken)
                .where(RefreshToken.user_id == stored_token.user_id)
                .values(is_revoked=True)
            )
            await db.commit()
            logger.warning(
                f"Refresh token reuse detected for user {stored_token.user_id}. "
                "All tokens revoked."
            )
            raise AuthenticationError("Token has been revoked. Please sign in again.")

        expires_at = stored_token.expires_at
        if expires_at.tzinfo is None:
            expires_at = expires_at.replace(tzinfo=timezone.utc)
        if expires_at < datetime.now(timezone.utc):
            raise AuthenticationError("Refresh token expired")

        user_id = stored_token.user_id

        # Revoke the old token
        stored_token.is_revoked = True

        # Create new refresh token (rotation)
        new_raw_token = _generate_refresh_token()
        new_token_hash = _hash_token(new_raw_token)
        new_expires_at = datetime.now(timezone.utc) + timedelta(days=30)

        new_rt = RefreshToken(
            user_id=user_id,
            token_hash=new_token_hash,
            expires_at=new_expires_at,
        )
        db.add(new_rt)
        await db.commit()

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
        refresh_token=new_raw_token,
        token_type="bearer",
        expires_in=settings.access_token_expire_minutes * 60,
    )


async def logout_user(user_id: str) -> dict:
    """Revoke all refresh tokens for a user.

    Args:
        user_id: User ID to log out

    Returns:
        Dict with logout confirmation
    """
    async with async_session_maker() as db:
        await db.execute(
            update(RefreshToken)
            .where(RefreshToken.user_id == user_id)
            .values(is_revoked=True)
        )
        await db.commit()

    logger.info(f"All refresh tokens revoked for user {user_id}")
    return {"message": "Logged out successfully"}
