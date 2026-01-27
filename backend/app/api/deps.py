"""API dependencies for dependency injection."""

import uuid
from typing import Annotated

from fastapi import Depends, Header
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import AuthenticationError, NotFoundError
from app.core.security import verify_token_and_get_user_id
from app.db.database import get_db
from app.models.user import User

# Type alias for database session dependency
DbSession = Annotated[AsyncSession, Depends(get_db)]


async def get_current_user(
    authorization: Annotated[str | None, Header()] = None,
    db: DbSession = None,
) -> User:
    """Dependency to get the current authenticated user.

    Args:
        authorization: Bearer token from Authorization header
        db: Database session

    Returns:
        Current user object

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

    try:
        user_uuid = uuid.UUID(user_id)
    except ValueError:
        raise AuthenticationError("Invalid user ID in token")

    result = await db.execute(select(User).where(User.id == user_uuid))
    user = result.scalar_one_or_none()

    if not user:
        raise NotFoundError("User not found")

    if not user.is_active:
        raise AuthenticationError("User account is deactivated")

    return user


# Type alias for current user dependency
CurrentUser = Annotated[User, Depends(get_current_user)]
