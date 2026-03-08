"""
Tests for household-scoped endpoints (Layer 3).

Covers: recipe rules (list/create/delete), monthly stats.
"""

import uuid
from datetime import date

from tests.factories import make_household, make_household_member, make_user


# ──────────────────────────────────────────────
# HOUSEHOLD RECIPE RULES
# ──────────────────────────────────────────────


async def test_create_household_recipe_rule(client):
    """Owner can create a household-scoped recipe rule."""
    create_resp = await client.post("/api/v1/households", json={"name": "Rule Test HH"})
    assert create_resp.status_code == 201
    hh_id = create_resp.json()["id"]

    resp = await client.post(
        f"/api/v1/households/{hh_id}/recipe-rules",
        json={
            "target_type": "INGREDIENT",
            "action": "EXCLUDE",
            "target_name": "Mushroom",
            "frequency_type": "NEVER",
        },
    )
    assert resp.status_code == 201
    data = resp.json()
    assert data["target_name"] == "Mushroom"
    assert data["scope"] == "HOUSEHOLD"
    assert data["household_id"] == hh_id
    assert data["is_active"] is True


async def test_list_household_recipe_rules(client):
    """Members can list household-scoped rules."""
    create_resp = await client.post(
        "/api/v1/households", json={"name": "List Rules HH"}
    )
    hh_id = create_resp.json()["id"]

    # Create two rules
    await client.post(
        f"/api/v1/households/{hh_id}/recipe-rules",
        json={
            "target_type": "INGREDIENT",
            "action": "EXCLUDE",
            "target_name": "Onion",
            "frequency_type": "NEVER",
        },
    )
    await client.post(
        f"/api/v1/households/{hh_id}/recipe-rules",
        json={
            "target_type": "INGREDIENT",
            "action": "INCLUDE",
            "target_name": "Dal",
            "frequency_type": "DAILY",
            "meal_slot": "LUNCH",
        },
    )

    resp = await client.get(f"/api/v1/households/{hh_id}/recipe-rules")
    assert resp.status_code == 200
    rules = resp.json()
    assert len(rules) == 2
    names = {r["target_name"] for r in rules}
    assert names == {"Onion", "Dal"}


async def test_delete_household_recipe_rule(client):
    """Owner can soft-delete a household rule."""
    create_resp = await client.post(
        "/api/v1/households", json={"name": "Delete Rule HH"}
    )
    hh_id = create_resp.json()["id"]

    rule_resp = await client.post(
        f"/api/v1/households/{hh_id}/recipe-rules",
        json={
            "target_type": "INGREDIENT",
            "action": "EXCLUDE",
            "target_name": "Garlic",
            "frequency_type": "NEVER",
        },
    )
    rule_id = rule_resp.json()["id"]

    # Delete it
    del_resp = await client.delete(f"/api/v1/households/{hh_id}/recipe-rules/{rule_id}")
    assert del_resp.status_code == 204

    # Should no longer appear in list
    list_resp = await client.get(f"/api/v1/households/{hh_id}/recipe-rules")
    assert len(list_resp.json()) == 0


async def test_delete_household_rule_not_found(client):
    """Deleting non-existent rule returns 404."""
    create_resp = await client.post("/api/v1/households", json={"name": "No Rule HH"})
    hh_id = create_resp.json()["id"]

    resp = await client.delete(
        f"/api/v1/households/{hh_id}/recipe-rules/{uuid.uuid4()}"
    )
    assert resp.status_code == 404


async def test_create_rule_not_owner(client, db_session):
    """Non-owner cannot create household rules."""
    second_user = make_user(name="Rule Owner", phone_number="+911111100010")
    db_session.add(second_user)
    await db_session.flush()

    household = make_household(owner_id=second_user.id)
    db_session.add(household)
    await db_session.flush()

    owner_member = make_household_member(
        household_id=household.id,
        user_id=second_user.id,
        role="OWNER",
        can_edit_shared_plan=True,
    )
    db_session.add(owner_member)
    await db_session.commit()

    resp = await client.post(
        f"/api/v1/households/{household.id}/recipe-rules",
        json={
            "target_type": "INGREDIENT",
            "action": "EXCLUDE",
            "target_name": "Paneer",
            "frequency_type": "NEVER",
        },
    )
    assert resp.status_code == 403


async def test_list_rules_not_member(client, db_session):
    """Non-member cannot list household rules."""
    second_user = make_user(name="Rule Non-Member", phone_number="+911111100011")
    db_session.add(second_user)
    await db_session.flush()

    household = make_household(owner_id=second_user.id)
    db_session.add(household)
    await db_session.flush()

    owner_member = make_household_member(
        household_id=household.id,
        user_id=second_user.id,
        role="OWNER",
        can_edit_shared_plan=True,
    )
    db_session.add(owner_member)
    await db_session.commit()

    resp = await client.get(f"/api/v1/households/{household.id}/recipe-rules")
    assert resp.status_code == 403


async def test_delete_rule_not_owner(client, db_session):
    """Non-owner cannot delete household rules."""
    second_user = make_user(name="Del Rule Owner", phone_number="+911111100013")
    db_session.add(second_user)
    await db_session.flush()

    household = make_household(owner_id=second_user.id)
    db_session.add(household)
    await db_session.flush()

    owner_member = make_household_member(
        household_id=household.id,
        user_id=second_user.id,
        role="OWNER",
        can_edit_shared_plan=True,
    )
    db_session.add(owner_member)
    await db_session.commit()

    resp = await client.delete(
        f"/api/v1/households/{household.id}/recipe-rules/{uuid.uuid4()}"
    )
    assert resp.status_code == 403


async def test_create_include_rule_with_details(client):
    """Create INCLUDE rule with frequency_count and meal_slot."""
    create_resp = await client.post(
        "/api/v1/households", json={"name": "Include Rule HH"}
    )
    hh_id = create_resp.json()["id"]

    resp = await client.post(
        f"/api/v1/households/{hh_id}/recipe-rules",
        json={
            "target_type": "INGREDIENT",
            "action": "INCLUDE",
            "target_name": "Chai",
            "frequency_type": "TIMES_PER_WEEK",
            "frequency_count": 5,
            "meal_slot": "BREAKFAST",
            "enforcement": "REQUIRED",
        },
    )
    assert resp.status_code == 201
    data = resp.json()
    assert data["frequency_count"] == 5
    assert data["meal_slot"] == "BREAKFAST"
    assert data["enforcement"] == "REQUIRED"


# ──────────────────────────────────────────────
# HOUSEHOLD MONTHLY STATS
# ──────────────────────────────────────────────


async def test_monthly_stats_empty(client):
    """Stats for household with no meal plans returns zeros."""
    create_resp = await client.post(
        "/api/v1/households", json={"name": "Stats Empty HH"}
    )
    hh_id = create_resp.json()["id"]

    resp = await client.get(
        f"/api/v1/households/{hh_id}/stats/monthly",
        params={"month": 3, "year": 2026},
    )
    assert resp.status_code == 200
    data = resp.json()
    assert data["household_id"] == hh_id
    assert data["total_meals_planned"] == 0
    assert data["meals_cooked"] == 0
    assert data["meals_skipped"] == 0


async def test_monthly_stats_not_member(client, db_session):
    """Non-member cannot access household stats."""
    second_user = make_user(name="Stats Non-Member", phone_number="+911111100012")
    db_session.add(second_user)
    await db_session.flush()

    household = make_household(owner_id=second_user.id)
    db_session.add(household)
    await db_session.flush()

    owner_member = make_household_member(
        household_id=household.id,
        user_id=second_user.id,
        role="OWNER",
        can_edit_shared_plan=True,
    )
    db_session.add(owner_member)
    await db_session.commit()

    resp = await client.get(
        f"/api/v1/households/{household.id}/stats/monthly",
        params={"month": 3, "year": 2026},
    )
    assert resp.status_code == 403


async def test_monthly_stats_with_meal_data(client, db_session, test_user):
    """Stats correctly count meal plan items for a household."""
    from app.models.meal_plan import MealPlan, MealPlanItem

    # Create household via API
    create_resp = await client.post(
        "/api/v1/households", json={"name": "Stats With Data HH"}
    )
    hh_id = create_resp.json()["id"]

    # Create a meal plan linked to the household
    plan = MealPlan(
        id=str(uuid.uuid4()),
        user_id=test_user.id,
        household_id=hh_id,
        week_start_date=date(2026, 3, 2),
        week_end_date=date(2026, 3, 8),
        is_active=True,
    )
    db_session.add(plan)
    await db_session.flush()

    # Add meal items with different statuses
    items = [
        MealPlanItem(
            id=str(uuid.uuid4()),
            meal_plan_id=plan.id,
            date=date(2026, 3, 2),
            meal_type="breakfast",
            recipe_name="Masala Chai",
            meal_status="COOKED",
        ),
        MealPlanItem(
            id=str(uuid.uuid4()),
            meal_plan_id=plan.id,
            date=date(2026, 3, 2),
            meal_type="lunch",
            recipe_name="Dal Fry",
            meal_status="PLANNED",
        ),
        MealPlanItem(
            id=str(uuid.uuid4()),
            meal_plan_id=plan.id,
            date=date(2026, 3, 3),
            meal_type="dinner",
            recipe_name="Paneer Masala",
            meal_status="SKIPPED",
        ),
    ]
    for item in items:
        db_session.add(item)
    await db_session.commit()

    resp = await client.get(
        f"/api/v1/households/{hh_id}/stats/monthly",
        params={"month": 3, "year": 2026},
    )
    assert resp.status_code == 200
    data = resp.json()
    assert data["total_meals_planned"] == 3
    assert data["meals_cooked"] == 1
    assert data["meals_skipped"] == 1
