"""Authentication endpoints."""

from fastapi import APIRouter, Request

from app.core.rate_limit import limiter
from app.schemas.auth import (
    AuthRequest,
    AuthResponse,
    RefreshTokenRequest,
    RefreshTokenResponse,
)
from app.api.deps import CurrentUser
from app.services.auth_service import authenticate_with_firebase, logout_user, refresh_access_token

router = APIRouter(prefix="/auth", tags=["auth"])


@router.post("/firebase", response_model=AuthResponse)
@limiter.limit("10/minute")
async def firebase_auth(request: Request, auth_request: AuthRequest) -> AuthResponse:
    """Exchange Firebase ID token for JWT access token.

    This endpoint verifies the Firebase token, creates the user if needed,
    and returns a JWT token for subsequent API calls.
    """
    return await authenticate_with_firebase(auth_request.firebase_token)


@router.post("/refresh", response_model=RefreshTokenResponse)
@limiter.limit("20/minute")
async def refresh_token(request: Request, refresh_request: RefreshTokenRequest) -> RefreshTokenResponse:
    """Refresh an access token using a valid refresh token.

    This endpoint takes a refresh token and returns a new access token.
    Use this when the access token expires to avoid re-authentication.
    """
    return await refresh_access_token(refresh_request.refresh_token)


@router.post("/logout")
async def logout(
    current_user: CurrentUser,
) -> dict:
    """Logout the current user by revoking all refresh tokens."""
    return await logout_user(current_user.id)
