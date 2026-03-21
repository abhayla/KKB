"""Tests for preference versioning via preferences_updated_at timestamp.

Gap 13: Settings conflict resolution via timestamps.
When multiple devices update preferences concurrently, the server detects
stale updates by comparing the client's `updated_at` against the stored
`preferences_updated_at` on the User model.
"""

import uuid
from datetime import datetime, timedelta, timezone

import pytest
from sqlalchemy import select

from app.core.exceptions import ConflictError
from app.models.user import User, UserPreferences
from app.schemas.user import UserPreferencesUpdate
from app.services.user_service import update_user_preferences


@pytest.fixture
async def user_with_prefs(db_session):
    """Create a user with existing preferences in the test DB."""
    user = User(
        id=str(uuid.uuid4()),
        firebase_uid=f"fb-{uuid.uuid4().hex[:8]}",
        email="versioning@test.com",
        name="Versioning User",
        is_onboarded=True,
        is_active=True,
    )
    db_session.add(user)

    prefs = UserPreferences(
        id=str(uuid.uuid4()),
        user_id=user.id,
        family_size=4,
        dietary_type="vegetarian",
        spice_level="medium",
    )
    db_session.add(prefs)
    await db_session.commit()
    await db_session.refresh(user)
    return user


async def test_preferences_update_sets_timestamp(db_session, user_with_prefs):
    """Verify preferences_updated_at is set after a successful update."""
    user = user_with_prefs
    assert user.preferences_updated_at is None  # Not yet set

    update = UserPreferencesUpdate(spice_level="spicy")
    await update_user_preferences(db_session, user, update)

    # Reload user to check the timestamp
    result = await db_session.execute(
        select(User).where(User.id == user.id)
    )
    updated_user = result.scalar_one()
    assert updated_user.preferences_updated_at is not None
    # Timestamp should be recent (within last 10 seconds)
    now = datetime.now(timezone.utc)
    delta = now - updated_user.preferences_updated_at.replace(tzinfo=timezone.utc)
    assert delta.total_seconds() < 10


async def test_preferences_update_with_stale_timestamp_rejected(
    db_session, user_with_prefs
):
    """A client sending an older updated_at than the server's should get 409."""
    user = user_with_prefs

    # First update — sets the server timestamp
    update1 = UserPreferencesUpdate(spice_level="spicy")
    await update_user_preferences(db_session, user, update1)

    # Reload to get the timestamp
    result = await db_session.execute(
        select(User).where(User.id == user.id)
    )
    user = result.scalar_one()
    server_ts = user.preferences_updated_at

    # Second update with a stale (older) timestamp
    stale_ts = server_ts - timedelta(minutes=5)
    update2 = UserPreferencesUpdate(
        spice_level="mild",
        updated_at=stale_ts.isoformat(),
    )
    with pytest.raises(ConflictError):
        await update_user_preferences(db_session, user, update2)


async def test_preferences_update_without_timestamp_accepted(
    db_session, user_with_prefs
):
    """Backward compat: no updated_at in request means no conflict check."""
    user = user_with_prefs

    # First update — sets the server timestamp
    update1 = UserPreferencesUpdate(spice_level="spicy")
    await update_user_preferences(db_session, user, update1)

    # Second update WITHOUT updated_at — should succeed (backward compat)
    update2 = UserPreferencesUpdate(spice_level="mild")
    result = await update_user_preferences(db_session, user, update2)

    assert result.preferences.spice_level == "mild"
