"""Authentication endpoints."""

from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import DbSession
from app.schemas.auth import AuthRequest, AuthResponse
from app.services.auth_service import authenticate_with_firebase

router = APIRouter(prefix="/auth", tags=["auth"])


@router.post("/firebase", response_model=AuthResponse)
async def firebase_auth(
    request: AuthRequest,
    db: DbSession,
) -> AuthResponse:
    """Exchange Firebase ID token for JWT access token.

    This endpoint verifies the Firebase token, creates the user if needed,
    and returns a JWT token for subsequent API calls.
    """
    return await authenticate_with_firebase(db, request.firebase_token)
