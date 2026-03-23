"""
Tests for household notification cross-isolation.

Verifies that users in one household cannot see, read, or interact with
another household's notifications. Uses make_api_client for multi-user
setups and direct db_session inserts for notification setup.
"""

import uuid

from app.models.notification import Notification
from tests.api.conftest import make_api_client
from tests.factories import make_household, make_household_member, make_user


async def _setup_two_households(db_session):
    """Create two households with separate owners and return all entities.

    Returns:
        (user_a, user_b, household_a, household_b)
    """
    user_a = make_user(name="User A", phone_number="+911111100001")
    user_b = make_user(name="User B", phone_number="+911111100002")
    db_session.add_all([user_a, user_b])
    await db_session.flush()

    household_a = make_household(owner_id=user_a.id, name="Household A")
    household_b = make_household(owner_id=user_b.id, name="Household B")
    db_session.add_all([household_a, household_b])
    await db_session.flush()

    member_a = make_household_member(
        household_id=household_a.id, user_id=user_a.id,
        role="OWNER", can_edit_shared_plan=True,
    )
    member_b = make_household_member(
        household_id=household_b.id, user_id=user_b.id,
        role="OWNER", can_edit_shared_plan=True,
    )
    db_session.add_all([member_a, member_b])
    await db_session.commit()

    return user_a, user_b, household_a, household_b


def _make_notification(user_id: str, household_id: str, **overrides) -> Notification:
    """Create a Notification instance scoped to a household."""
    defaults = {
        "id": str(uuid.uuid4()),
        "user_id": user_id,
        "household_id": household_id,
        "type": "meal_plan_update",
        "title": "Test notification",
        "body": "Test notification body",
        "is_read": False,
    }
    defaults.update(overrides)
    return Notification(**defaults)


# ──────────────────────────────────────────────
# 1. User sees only own household notifications
# ──────────────────────────────────────────────


async def test_user_sees_only_own_household_notifications(db_session):
    """User A lists notifications for household A and sees only h1 notifications,
    not household B's notifications."""
    user_a, user_b, h_a, h_b = await _setup_two_households(db_session)

    # Create notifications for each household
    notif_a1 = _make_notification(user_a.id, h_a.id, title="A meal ready")
    notif_a2 = _make_notification(user_a.id, h_a.id, title="A grocery reminder")
    notif_b1 = _make_notification(user_b.id, h_b.id, title="B meal ready")
    db_session.add_all([notif_a1, notif_a2, notif_b1])
    await db_session.commit()

    async with make_api_client(db_session, user_a) as client_a:
        resp = await client_a.get(f"/api/v1/households/{h_a.id}/notifications")
        assert resp.status_code == 200
        data = resp.json()

        # User A sees exactly their 2 notifications
        assert len(data) == 2
        titles = {n["title"] for n in data}
        assert "A meal ready" in titles
        assert "A grocery reminder" in titles
        assert "B meal ready" not in titles


# ──────────────────────────────────────────────
# 2. User cannot mark another household's notification as read
# ──────────────────────────────────────────────


async def test_user_cannot_read_other_household_notification(db_session):
    """User A tries to mark a notification in household B as read — should get 403."""
    user_a, user_b, h_a, h_b = await _setup_two_households(db_session)

    notif_b = _make_notification(user_b.id, h_b.id, title="B private notif")
    db_session.add(notif_b)
    await db_session.commit()

    async with make_api_client(db_session, user_a) as client_a:
        # User A is not a member of household B — should get 403
        resp = await client_a.put(
            f"/api/v1/households/{h_b.id}/notifications/{notif_b.id}/read"
        )
        assert resp.status_code == 403


# ──────────────────────────────────────────────
# 3. User cannot delete another household's notification
# ──────────────────────────────────────────────


async def test_user_cannot_delete_other_household_notification(db_session):
    """User A tries to access household B's notification list — should get 403.

    There is no DELETE endpoint on the household notification router,
    so we verify that even listing (GET) another household's notifications
    is blocked with 403.
    """
    user_a, user_b, h_a, h_b = await _setup_two_households(db_session)

    notif_b = _make_notification(user_b.id, h_b.id, title="B secret notif")
    db_session.add(notif_b)
    await db_session.commit()

    async with make_api_client(db_session, user_a) as client_a:
        # User A cannot even list household B's notifications
        resp = await client_a.get(f"/api/v1/households/{h_b.id}/notifications")
        assert resp.status_code == 403


# ──────────────────────────────────────────────
# 4. Notification disappears after leaving household
# ──────────────────────────────────────────────


async def test_notification_disappears_when_leaving_household(db_session):
    """After a member leaves a household, they can no longer see its notifications."""
    owner = make_user(name="Owner", phone_number="+911111100010")
    member_user = make_user(name="Member", phone_number="+911111100011")
    db_session.add_all([owner, member_user])
    await db_session.flush()

    household = make_household(owner_id=owner.id, name="Shared Household")
    db_session.add(household)
    await db_session.flush()

    owner_member = make_household_member(
        household_id=household.id, user_id=owner.id,
        role="OWNER", can_edit_shared_plan=True,
    )
    regular_member = make_household_member(
        household_id=household.id, user_id=member_user.id,
        role="MEMBER",
    )
    db_session.add_all([owner_member, regular_member])
    await db_session.flush()

    # Create a notification for the member in this household
    notif = _make_notification(member_user.id, household.id, title="Shared notif")
    db_session.add(notif)
    await db_session.commit()

    # Member can see notification before leaving
    async with make_api_client(db_session, member_user) as client_member:
        resp = await client_member.get(
            f"/api/v1/households/{household.id}/notifications"
        )
        assert resp.status_code == 200
        assert len(resp.json()) == 1

    # Member leaves the household
    async with make_api_client(db_session, member_user) as client_member:
        leave_resp = await client_member.post(
            f"/api/v1/households/{household.id}/leave"
        )
        assert leave_resp.status_code == 204

    # After leaving, member should get 403 when trying to list notifications
    async with make_api_client(db_session, member_user) as client_member:
        resp = await client_member.get(
            f"/api/v1/households/{household.id}/notifications"
        )
        assert resp.status_code == 403


# ──────────────────────────────────────────────
# 5. Non-owner member can see household notifications
# ──────────────────────────────────────────────


async def test_household_member_sees_household_notifications(db_session):
    """A non-owner MEMBER of a household can list their own household notifications."""
    owner = make_user(name="Owner", phone_number="+911111100020")
    member_user = make_user(name="Regular Member", phone_number="+911111100021")
    db_session.add_all([owner, member_user])
    await db_session.flush()

    household = make_household(owner_id=owner.id, name="Family Household")
    db_session.add(household)
    await db_session.flush()

    owner_member = make_household_member(
        household_id=household.id, user_id=owner.id,
        role="OWNER", can_edit_shared_plan=True,
    )
    regular_member = make_household_member(
        household_id=household.id, user_id=member_user.id,
        role="MEMBER",
    )
    db_session.add_all([owner_member, regular_member])
    await db_session.flush()

    # Create notifications for the regular member in this household
    notif1 = _make_notification(member_user.id, household.id, title="Member notif 1")
    notif2 = _make_notification(member_user.id, household.id, title="Member notif 2")
    db_session.add_all([notif1, notif2])
    await db_session.commit()

    async with make_api_client(db_session, member_user) as client_member:
        resp = await client_member.get(
            f"/api/v1/households/{household.id}/notifications"
        )
        assert resp.status_code == 200
        data = resp.json()
        assert len(data) == 2
        titles = {n["title"] for n in data}
        assert "Member notif 1" in titles
        assert "Member notif 2" in titles

    # Owner should NOT see the member's notifications (scoped by user_id)
    async with make_api_client(db_session, owner) as client_owner:
        resp = await client_owner.get(
            f"/api/v1/households/{household.id}/notifications"
        )
        assert resp.status_code == 200
        data = resp.json()
        assert len(data) == 0  # No notifications for owner in this household
