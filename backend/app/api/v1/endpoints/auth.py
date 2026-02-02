"""Authentication endpoints."""

from fastapi import APIRouter

from app.schemas.auth import (
    AuthRequest,
    AuthResponse,
    RefreshTokenRequest,
    RefreshTokenResponse,
)
from app.services.auth_service import authenticate_with_firebase, refresh_access_token

router = APIRouter(prefix="/auth", tags=["auth"])


@router.post("/firebase", response_model=AuthResponse)
async def firebase_auth(request: AuthRequest) -> AuthResponse:
    """Exchange Firebase ID token for JWT access token.

    This endpoint verifies the Firebase token, creates the user if needed,
    and returns a JWT token for subsequent API calls.
    """
    return await authenticate_with_firebase(request.firebase_token)


@router.post("/refresh", response_model=RefreshTokenResponse)
async def refresh_token(request: RefreshTokenRequest) -> RefreshTokenResponse:
    """Refresh an access token using a valid refresh token.

    This endpoint takes a refresh token and returns a new access token.
    Use this when the access token expires to avoid re-authentication.
    """
    return await refresh_access_token(request.refresh_token)
