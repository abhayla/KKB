"""Authentication schemas matching Android DTOs."""

from typing import TYPE_CHECKING, Optional

from pydantic import BaseModel, Field

if TYPE_CHECKING:
    from app.schemas.user import UserPreferencesDto


class UserResponseForAuth(BaseModel):
    """User response embedded in auth response."""

    id: str
    email: str
    name: str
    profile_image_url: Optional[str] = None
    is_onboarded: bool
    preferences: Optional["UserPreferencesDto"] = None

    class Config:
        from_attributes = True


class AuthRequest(BaseModel):
    """Firebase token exchange request."""

    firebase_token: str = Field(..., description="Firebase ID token")


class AuthResponse(BaseModel):
    """Authentication response with JWT tokens."""

    access_token: str
    refresh_token: str
    token_type: str = "bearer"
    expires_in: int  # seconds
    user: UserResponseForAuth

    class Config:
        from_attributes = True


class RefreshTokenRequest(BaseModel):
    """Request to refresh access token."""

    refresh_token: str = Field(..., description="Refresh token from initial auth")


class RefreshTokenResponse(BaseModel):
    """Response with new access token."""

    access_token: str
    token_type: str = "bearer"
    expires_in: int  # seconds


# Import for forward reference resolution
from app.schemas.user import UserPreferencesDto  # noqa: E402, F811

UserResponseForAuth.model_rebuild()
AuthResponse.model_rebuild()
