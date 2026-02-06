"""Notification API endpoints."""

from fastapi import APIRouter, HTTPException, status

from app.api.deps import CurrentUser, DbSession
from app.core.exceptions import NotFoundError
from app.schemas.notification import (
    FcmTokenRequest,
    NotificationsListResponse,
    SuccessResponse,
)
from app.services import notification_service

router = APIRouter(prefix="/notifications", tags=["notifications"])


@router.get("", response_model=NotificationsListResponse)
async def get_notifications(
    db: DbSession,
    current_user: CurrentUser,
) -> NotificationsListResponse:
    """Get all notifications for the current user.

    Returns notifications sorted by creation date (newest first).
    Excludes expired notifications by default.
    """
    user_id = current_user.id
    if not user_id:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED)

    return await notification_service.get_notifications(db, user_id)


@router.put("/read-all", response_model=SuccessResponse)
async def mark_all_notifications_as_read(
    db: DbSession,
    current_user: CurrentUser,
) -> SuccessResponse:
    """Mark all notifications as read for the current user."""
    user_id = current_user.id
    if not user_id:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED)

    count = await notification_service.mark_all_as_read(db, user_id)
    return SuccessResponse(success=True, message=f"Marked {count} notifications as read")


# FCM Token endpoints - must be before /{notification_id} routes to avoid route conflicts
@router.post("/fcm-token", response_model=SuccessResponse)
async def register_fcm_token(
    db: DbSession,
    current_user: CurrentUser,
    request: FcmTokenRequest,
) -> SuccessResponse:
    """Register an FCM token for push notifications.

    Call this when:
    - App receives a new FCM token
    - User logs in
    - App is restored from background
    """
    user_id = current_user.id
    if not user_id:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED)

    await notification_service.register_fcm_token(
        db, user_id, request.fcm_token, request.device_type
    )
    return SuccessResponse(success=True, message="FCM token registered")


@router.delete("/fcm-token", response_model=SuccessResponse)
async def unregister_fcm_token(
    db: DbSession,
    current_user: CurrentUser,
    fcm_token: str,
) -> SuccessResponse:
    """Unregister an FCM token.

    Call this when:
    - User logs out
    - User disables notifications

    Args:
        fcm_token: The FCM token to unregister (query parameter)
    """
    # Note: We don't require user validation here since the token
    # is unique and the operation just deactivates it
    await notification_service.unregister_fcm_token(db, fcm_token)
    return SuccessResponse(success=True, message="FCM token unregistered")


# Dynamic {notification_id} routes - must be after static routes
@router.put("/{notification_id}/read", response_model=SuccessResponse)
async def mark_notification_as_read(
    db: DbSession,
    current_user: CurrentUser,
    notification_id: str,
) -> SuccessResponse:
    """Mark a notification as read."""
    user_id = current_user.id
    if not user_id:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED)

    success = await notification_service.mark_as_read(db, notification_id, user_id)
    if not success:
        raise NotFoundError("Notification not found")

    return SuccessResponse(success=True, message="Notification marked as read")


@router.delete("/{notification_id}", response_model=SuccessResponse)
async def delete_notification(
    db: DbSession,
    current_user: CurrentUser,
    notification_id: str,
) -> SuccessResponse:
    """Delete a notification."""
    user_id = current_user.id
    if not user_id:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED)

    success = await notification_service.delete_notification(db, notification_id, user_id)
    if not success:
        raise NotFoundError("Notification not found")

    return SuccessResponse(success=True, message="Notification deleted")
