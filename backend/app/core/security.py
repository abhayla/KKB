"""JWT token handling and security utilities."""

from datetime import datetime, timedelta, timezone
from typing import Optional

from jose import JWTError, jwt

from app.config import settings
from app.core.exceptions import AuthenticationError


def create_access_token(
    data: dict,
    expires_delta: Optional[timedelta] = None,
) -> str:
    """Create a JWT access token.

    Args:
        data: Payload data to encode in the token
        expires_delta: Optional custom expiration time

    Returns:
        Encoded JWT token string
    """
    to_encode = data.copy()
    if expires_delta:
        expire = datetime.now(timezone.utc) + expires_delta
    else:
        expire = datetime.now(timezone.utc) + timedelta(
            minutes=settings.access_token_expire_minutes
        )
    to_encode.update({"exp": expire})
    encoded_jwt = jwt.encode(
        to_encode,
        settings.jwt_secret_key,
        algorithm=settings.jwt_algorithm,
    )
    return encoded_jwt


def decode_access_token(token: str) -> dict:
    """Decode and validate a JWT access token.

    Args:
        token: JWT token string to decode

    Returns:
        Decoded token payload

    Raises:
        AuthenticationError: If token is invalid or expired
    """
    try:
        payload = jwt.decode(
            token,
            settings.jwt_secret_key,
            algorithms=[settings.jwt_algorithm],
        )
        return payload
    except JWTError as e:
        raise AuthenticationError(f"Invalid token: {str(e)}")


def verify_token_and_get_user_id(token: str) -> str:
    """Verify token and extract user ID.

    Args:
        token: JWT token string

    Returns:
        User ID from the token

    Raises:
        AuthenticationError: If token is invalid or doesn't contain user_id
    """
    payload = decode_access_token(token)
    user_id: Optional[str] = payload.get("sub")
    if user_id is None:
        raise AuthenticationError("Token missing user ID")
    return user_id
