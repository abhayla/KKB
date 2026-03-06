"""Notification Pydantic schemas."""

from datetime import datetime
from typing import Any, Optional

from pydantic import BaseModel, ConfigDict, Field


class NotificationActionData(BaseModel):
    """Action data for notification clicks."""

    recipe_id: Optional[str] = None
    meal_plan_id: Optional[str] = None
    festival_id: Optional[str] = None
    streak_count: Optional[int] = None


class NotificationResponse(BaseModel):
    """Single notification response."""

    id: str
    type: str = Field(
        ...,
        description="Type: festival_reminder, meal_plan_update, shopping_reminder, recipe_suggestion, streak_milestone",
    )
    title: str
    body: str
    image_url: Optional[str] = None
    action_type: Optional[str] = Field(
        None,
        description="Action: open_recipe, open_meal_plan, open_grocery, open_stats, none",
    )
    action_data: Optional[NotificationActionData] = None
    is_read: bool = False
    created_at: str
    expires_at: Optional[str] = None

    model_config = ConfigDict(from_attributes=True)


class NotificationsListResponse(BaseModel):
    """Response containing a list of notifications."""

    notifications: list[NotificationResponse]
    unread_count: int
    total_count: int


class CreateNotificationRequest(BaseModel):
    """Request to create a notification (internal use)."""

    user_id: str
    type: str
    title: str
    body: str
    image_url: Optional[str] = None
    action_type: Optional[str] = None
    action_data: Optional[dict[str, Any]] = None
    expires_at: Optional[datetime] = None


class FcmTokenRequest(BaseModel):
    """Request to register/unregister FCM token."""

    fcm_token: str = Field(..., min_length=1, max_length=500)
    device_type: str = Field(default="android", max_length=20)


class SuccessResponse(BaseModel):
    """Generic success response."""

    success: bool = True
    message: Optional[str] = None
