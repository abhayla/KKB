"""API dependencies for dependency injection."""

from typing import Annotated, Any

from fastapi import Depends, Header
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import AuthenticationError, NotFoundError
from app.core.security import verify_token_and_get_user_id
from app.db.database import get_db
from app.repositories.user_repository import UserRepository

# Keep DbSession for backward compatibility with other endpoints
# These endpoints still use PostgreSQL and will fail until migrated to Firestore
DbSession = Annotated[AsyncSession, Depends(get_db)]


async def get_current_user(
    authorization: Annotated[str | None, Header()] = None,
) -> dict[str, Any]:
    """Dependency to get the current authenticated user from Firestore.

    Args:
        authorization: Bearer token from Authorization header

    Returns:
        Current user dict from Firestore

    Raises:
        AuthenticationError: If token is missing or invalid
        NotFoundError: If user not found in database
    """
    if not authorization:
        raise AuthenticationError("Missing authorization header")

    if not authorization.startswith("Bearer "):
        raise AuthenticationError("Invalid authorization header format")

    token = authorization.replace("Bearer ", "")
    user_id = verify_token_and_get_user_id(token)

    # Use Firestore-based UserRepository instead of PostgreSQL
    user_repo = UserRepository()
    user = await user_repo.get_by_id(user_id)

    if not user:
        raise NotFoundError("User not found")

    if not user.get("is_active", True) is False:
        # User is active (default to True if not set)
        pass
    else:
        raise AuthenticationError("User account is deactivated")

    return user


# Type alias for current user dependency
CurrentUser = Annotated[dict[str, Any], Depends(get_current_user)]
