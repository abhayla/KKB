"""
Tests for Layer 4 household endpoints.

Covers: meal plan current, constraints, item status, notifications.
"""

import json
import uuid
from datetime import date

from app.models.meal_plan import MealPlan, MealPlanItem
from app.models.notification import Notification
from app.models.user import UserPreferences
from tests.factories import make_household, make_household_member, make_user


# ──────────────────────────────────────────────
# CURRENT MEAL PLAN
# ──────────────────────────────────────────────


async def test_get_current_meal_plan_none(client):
    """Returns null when no meal plan exists."""
    create_resp = await client.post("/api/v1/households", json={"name": "No Plan HH"})
    hh_id = create_resp.json()["id"]

    resp = await client.get(f"/api/v1/households/{hh_id}/meal-plans/current")
    assert resp.status_code == 200
    assert resp.json()["meal_plan"] is None


async def test_get_current_meal_plan_with_items(client, db_session, test_user):
    """Returns meal plan grouped by date."""
    create_resp = await client.post("/api/v1/households", json={"name": "Plan HH"})
    hh_id = create_resp.json()["id"]

    plan = MealPlan(
        id=str(uuid.uuid4()),
        user_id=test_user.id,
        household_id=hh_id,
        week_start_date=date(2026, 3, 9),
        week_end_date=date(2026, 3, 15),
        is_active=True,
    )
    db_session.add(plan)
    await db_session.flush()

    item = MealPlanItem(
        id=str(uuid.uuid4()),
        meal_plan_id=plan.id,
        date=date(2026, 3, 9),
        meal_type="breakfast",
        recipe_name="Masala Dosa",
    )
    db_session.add(item)
    await db_session.commit()

    resp = await client.get(f"/api/v1/households/{hh_id}/meal-plans/current")
    assert resp.status_code == 200
    data = resp.json()["meal_plan"]
    assert data["household_id"] == hh_id
    assert len(data["days"]) == 1
    assert "breakfast" in data["days"][0]["meals"]
    assert data["days"][0]["meals"]["breakfast"][0]["recipe_name"] == "Masala Dosa"


async def test_get_current_meal_plan_not_member(client, db_session):
    """Non-member cannot view household meal plan."""
    other = make_user(name="Plan Owner", phone_number="+911111100020")
    db_session.add(other)
    await db_session.flush()

    household = make_household(owner_id=other.id)
    db_session.add(household)
    await db_session.flush()

    member = make_household_member(
        household_id=household.id,
        user_id=other.id,
        role="OWNER",
        can_edit_shared_plan=True,
    )
    db_session.add(member)
    await db_session.commit()

    resp = await client.get(f"/api/v1/households/{household.id}/meal-plans/current")
    assert resp.status_code == 403


# ──────────────────────────────────────────────
# MERGED CONSTRAINTS
# ──────────────────────────────────────────────


async def test_get_constraints_empty_household(client):
    """Constraints for household with no prefs returns empty lists."""
    create_resp = await client.post(
        "/api/v1/households", json={"name": "Constraints HH"}
    )
    hh_id = create_resp.json()["id"]

    resp = await client.get(f"/api/v1/households/{hh_id}/constraints")
    assert resp.status_code == 200
    data = resp.json()
    assert "allergies" in data
    assert "dislikes" in data
    assert "dietary_tags" in data
    assert data["member_count"] >= 1


async def test_get_constraints_with_prefs(client, db_session, test_user):
    """Constraints merge allergies from members."""
    create_resp = await client.post("/api/v1/households", json={"name": "Prefs HH"})
    hh_id = create_resp.json()["id"]

    # Set up test_user preferences with allergies
    prefs = UserPreferences(
        id=str(uuid.uuid4()),
        user_id=test_user.id,
        dietary_type="vegetarian",
        allergies=[{"ingredient": "peanuts", "severity": "SEVERE"}],
        disliked_ingredients=["karela", "baingan"],
    )
    db_session.add(prefs)
    await db_session.commit()

    resp = await client.get(f"/api/v1/households/{hh_id}/constraints")
    assert resp.status_code == 200
    data = resp.json()
    assert "peanuts" in data["allergies"]
    assert "karela" in data["dislikes"]
    assert "baingan" in data["dislikes"]


async def test_get_constraints_not_owner(client, db_session, test_user):
    """Non-owner cannot view constraints."""
    other = make_user(name="Const Owner", phone_number="+911111100021")
    db_session.add(other)
    await db_session.flush()

    household = make_household(owner_id=other.id)
    db_session.add(household)
    await db_session.flush()

    owner_member = make_household_member(
        household_id=household.id,
        user_id=other.id,
        role="OWNER",
        can_edit_shared_plan=True,
    )
    # Add test_user as regular member
    regular_member = make_household_member(
        household_id=household.id,
        user_id=test_user.id,
        role="MEMBER",
        can_edit_shared_plan=False,
    )
    db_session.add(owner_member)
    db_session.add(regular_member)
    await db_session.commit()

    resp = await client.get(f"/api/v1/households/{household.id}/constraints")
    assert resp.status_code == 403


# ──────────────────────────────────────────────
# MEAL ITEM STATUS UPDATE
# ──────────────────────────────────────────────


async def test_update_meal_item_status(client, db_session, test_user):
    """Owner can update meal item status to COOKED."""
    create_resp = await client.post("/api/v1/households", json={"name": "Status HH"})
    hh_id = create_resp.json()["id"]

    plan = MealPlan(
        id=str(uuid.uuid4()),
        user_id=test_user.id,
        household_id=hh_id,
        week_start_date=date(2026, 3, 9),
        week_end_date=date(2026, 3, 15),
        is_active=True,
    )
    db_session.add(plan)
    await db_session.flush()

    item = MealPlanItem(
        id=str(uuid.uuid4()),
        meal_plan_id=plan.id,
        date=date(2026, 3, 9),
        meal_type="lunch",
        recipe_name="Dal Fry",
    )
    db_session.add(item)
    await db_session.commit()

    resp = await client.put(
        f"/api/v1/households/{hh_id}/meal-plans/{plan.id}/items/{item.id}/status",
        params={"status": "COOKED"},
    )
    assert resp.status_code == 200
    assert resp.json()["meal_status"] == "COOKED"


async def test_update_meal_item_status_with_edit_access(client, db_session, test_user):
    """Member WITH can_edit_shared_plan can update status."""
    other = make_user(name="Edit Owner", phone_number="+911111100031")
    db_session.add(other)
    await db_session.flush()

    household = make_household(owner_id=other.id)
    db_session.add(household)
    await db_session.flush()

    owner_member = make_household_member(
        household_id=household.id,
        user_id=other.id,
        role="OWNER",
        can_edit_shared_plan=True,
    )
    editor_member = make_household_member(
        household_id=household.id,
        user_id=test_user.id,
        role="MEMBER",
        can_edit_shared_plan=True,
    )
    db_session.add(owner_member)
    db_session.add(editor_member)
    await db_session.flush()

    plan = MealPlan(
        id=str(uuid.uuid4()),
        user_id=other.id,
        household_id=household.id,
        week_start_date=date(2026, 3, 9),
        week_end_date=date(2026, 3, 15),
        is_active=True,
    )
    db_session.add(plan)
    await db_session.flush()

    item = MealPlanItem(
        id=str(uuid.uuid4()),
        meal_plan_id=plan.id,
        date=date(2026, 3, 9),
        meal_type="dinner",
        recipe_name="Paneer Masala",
    )
    db_session.add(item)
    await db_session.commit()

    resp = await client.put(
        f"/api/v1/households/{household.id}/meal-plans/{plan.id}/items/{item.id}/status",
        params={"status": "COOKED"},
    )
    assert resp.status_code == 200
    assert resp.json()["meal_status"] == "COOKED"


async def test_update_meal_item_status_no_edit_access(client, db_session, test_user):
    """Member without can_edit_shared_plan cannot update status."""
    other = make_user(name="Status Owner", phone_number="+911111100030")
    db_session.add(other)
    await db_session.flush()

    household = make_household(owner_id=other.id)
    db_session.add(household)
    await db_session.flush()

    owner_member = make_household_member(
        household_id=household.id,
        user_id=other.id,
        role="OWNER",
        can_edit_shared_plan=True,
    )
    # Add test_user as regular member WITHOUT edit access
    regular_member = make_household_member(
        household_id=household.id,
        user_id=test_user.id,
        role="MEMBER",
        can_edit_shared_plan=False,
    )
    db_session.add(owner_member)
    db_session.add(regular_member)
    await db_session.flush()

    plan = MealPlan(
        id=str(uuid.uuid4()),
        user_id=other.id,
        household_id=household.id,
        week_start_date=date(2026, 3, 9),
        week_end_date=date(2026, 3, 15),
        is_active=True,
    )
    db_session.add(plan)
    await db_session.flush()

    item = MealPlanItem(
        id=str(uuid.uuid4()),
        meal_plan_id=plan.id,
        date=date(2026, 3, 9),
        meal_type="lunch",
        recipe_name="Dal Fry",
    )
    db_session.add(item)
    await db_session.commit()

    resp = await client.put(
        f"/api/v1/households/{household.id}/meal-plans/{plan.id}/items/{item.id}/status",
        params={"status": "COOKED"},
    )
    assert resp.status_code == 403


async def test_update_meal_item_status_not_found(client, db_session, test_user):
    """Returns 404 for non-existent item."""
    create_resp = await client.post("/api/v1/households", json={"name": "Not Found HH"})
    hh_id = create_resp.json()["id"]

    plan = MealPlan(
        id=str(uuid.uuid4()),
        user_id=test_user.id,
        household_id=hh_id,
        week_start_date=date(2026, 3, 9),
        week_end_date=date(2026, 3, 15),
        is_active=True,
    )
    db_session.add(plan)
    await db_session.commit()

    resp = await client.put(
        f"/api/v1/households/{hh_id}/meal-plans/{plan.id}/items/{uuid.uuid4()}/status",
        params={"status": "COOKED"},
    )
    assert resp.status_code == 404


# ──────────────────────────────────────────────
# NOTIFICATIONS
# ──────────────────────────────────────────────


async def test_list_notifications_empty(client):
    """Returns empty list when no notifications."""
    create_resp = await client.post(
        "/api/v1/households", json={"name": "Notif Empty HH"}
    )
    hh_id = create_resp.json()["id"]

    resp = await client.get(f"/api/v1/households/{hh_id}/notifications")
    assert resp.status_code == 200
    assert resp.json() == []


async def test_list_notifications_with_data(client, db_session, test_user):
    """Returns notifications for the current user."""
    create_resp = await client.post("/api/v1/households", json={"name": "Notif HH"})
    hh_id = create_resp.json()["id"]

    notif = Notification(
        id=str(uuid.uuid4()),
        user_id=test_user.id,
        household_id=hh_id,
        type="JOIN",
        title="New member joined",
        body="Someone joined your household",
        metadata_json=json.dumps({"member_name": "Sunita"}),
    )
    db_session.add(notif)
    await db_session.commit()

    resp = await client.get(f"/api/v1/households/{hh_id}/notifications")
    assert resp.status_code == 200
    data = resp.json()
    assert len(data) == 1
    assert data[0]["type"] == "JOIN"
    assert data[0]["is_read"] is False
    assert '"member_name"' in data[0]["metadata_json"]


async def test_mark_notification_read(client, db_session, test_user):
    """Mark a notification as read."""
    create_resp = await client.post("/api/v1/households", json={"name": "Mark Read HH"})
    hh_id = create_resp.json()["id"]

    notif = Notification(
        id=str(uuid.uuid4()),
        user_id=test_user.id,
        household_id=hh_id,
        type="LEAVE",
        title="Member left",
        body="A member left your household",
    )
    db_session.add(notif)
    await db_session.commit()

    resp = await client.put(f"/api/v1/households/{hh_id}/notifications/{notif.id}/read")
    assert resp.status_code == 204

    # Verify it's marked as read
    list_resp = await client.get(f"/api/v1/households/{hh_id}/notifications")
    assert list_resp.json()[0]["is_read"] is True


async def test_mark_notification_not_found(client):
    """Marking non-existent notification returns 404."""
    create_resp = await client.post("/api/v1/households", json={"name": "Notif 404 HH"})
    hh_id = create_resp.json()["id"]

    resp = await client.put(
        f"/api/v1/households/{hh_id}/notifications/{uuid.uuid4()}/read"
    )
    assert resp.status_code == 404


async def test_notifications_not_member(client, db_session):
    """Non-member cannot view household notifications."""
    other = make_user(name="Notif Owner", phone_number="+911111100022")
    db_session.add(other)
    await db_session.flush()

    household = make_household(owner_id=other.id)
    db_session.add(household)
    await db_session.flush()

    member = make_household_member(
        household_id=household.id,
        user_id=other.id,
        role="OWNER",
        can_edit_shared_plan=True,
    )
    db_session.add(member)
    await db_session.commit()

    resp = await client.get(f"/api/v1/households/{household.id}/notifications")
    assert resp.status_code == 403
