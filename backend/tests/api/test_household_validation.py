"""
Tests for household schema validation (422 rejection paths).

Covers: invalid inputs that should be rejected by Pydantic schemas.
"""

import uuid


# ──────────────────────────────────────────────
# HouseholdCreate validation
# ──────────────────────────────────────────────


async def test_create_household_empty_name(client):
    """Empty name should be rejected."""
    resp = await client.post("/api/v1/households", json={"name": ""})
    assert resp.status_code == 422


async def test_create_household_name_too_long(client):
    """Name exceeding 100 chars should be rejected."""
    resp = await client.post("/api/v1/households", json={"name": "A" * 101})
    assert resp.status_code == 422


async def test_create_household_missing_name(client):
    """Missing name field should be rejected."""
    resp = await client.post("/api/v1/households", json={})
    assert resp.status_code == 422


# ──────────────────────────────────────────────
# HouseholdUpdate validation
# ──────────────────────────────────────────────


async def test_update_max_members_too_low(client):
    """max_members below 2 should be rejected."""
    create_resp = await client.post(
        "/api/v1/households", json={"name": "Validation HH"}
    )
    hh_id = create_resp.json()["id"]

    resp = await client.put(
        f"/api/v1/households/{hh_id}", json={"max_members": 1}
    )
    assert resp.status_code == 422


async def test_update_max_members_too_high(client):
    """max_members above 20 should be rejected."""
    create_resp = await client.post(
        "/api/v1/households", json={"name": "Max HH"}
    )
    hh_id = create_resp.json()["id"]

    resp = await client.put(
        f"/api/v1/households/{hh_id}", json={"max_members": 21}
    )
    assert resp.status_code == 422


async def test_update_empty_name(client):
    """Empty name string should be rejected."""
    create_resp = await client.post(
        "/api/v1/households", json={"name": "Empty Name HH"}
    )
    hh_id = create_resp.json()["id"]

    resp = await client.put(f"/api/v1/households/{hh_id}", json={"name": ""})
    assert resp.status_code == 422


# ──────────────────────────────────────────────
# JoinHouseholdRequest validation
# ──────────────────────────────────────────────


async def test_join_code_too_short(client):
    """Invite code shorter than 8 chars should be rejected."""
    resp = await client.post(
        "/api/v1/households/join", json={"invite_code": "SHORT"}
    )
    assert resp.status_code == 422


async def test_join_code_too_long(client):
    """Invite code longer than 8 chars should be rejected."""
    resp = await client.post(
        "/api/v1/households/join", json={"invite_code": "TOOLONGCODE1"}
    )
    assert resp.status_code == 422


async def test_join_missing_code(client):
    """Missing invite_code field should be rejected."""
    resp = await client.post("/api/v1/households/join", json={})
    assert resp.status_code == 422


# ──────────────────────────────────────────────
# AddMemberByPhoneRequest validation
# ──────────────────────────────────────────────


async def test_add_member_phone_too_short(client):
    """Phone number shorter than 10 chars should be rejected."""
    create_resp = await client.post(
        "/api/v1/households", json={"name": "Phone Val HH"}
    )
    hh_id = create_resp.json()["id"]

    resp = await client.post(
        f"/api/v1/households/{hh_id}/members",
        json={"phone_number": "123"},
    )
    assert resp.status_code == 422


async def test_add_member_missing_phone(client):
    """Missing phone_number field should be rejected."""
    create_resp = await client.post(
        "/api/v1/households", json={"name": "No Phone HH"}
    )
    hh_id = create_resp.json()["id"]

    resp = await client.post(
        f"/api/v1/households/{hh_id}/members", json={}
    )
    assert resp.status_code == 422


# ──────────────────────────────────────────────
# UpdateMemberRequest validation
# ──────────────────────────────────────────────


async def test_update_member_invalid_portion_size(client, db_session):
    """Invalid portion_size enum should be rejected."""
    from tests.factories import make_user, make_household_member

    create_resp = await client.post(
        "/api/v1/households", json={"name": "Portion Val HH"}
    )
    hh_id = create_resp.json()["id"]

    second_user = make_user(name="Portion User", phone_number="+911111100060")
    db_session.add(second_user)
    await db_session.commit()

    add_resp = await client.post(
        f"/api/v1/households/{hh_id}/members",
        json={"phone_number": "+911111100060"},
    )
    member_id = add_resp.json()["id"]

    resp = await client.put(
        f"/api/v1/households/{hh_id}/members/{member_id}",
        json={"portion_size": "EXTRA_LARGE"},
    )
    assert resp.status_code == 422


async def test_update_member_invalid_role(client, db_session):
    """Invalid role enum should be rejected."""
    from tests.factories import make_user

    create_resp = await client.post(
        "/api/v1/households", json={"name": "Role Val HH"}
    )
    hh_id = create_resp.json()["id"]

    second_user = make_user(name="Role User", phone_number="+911111100061")
    db_session.add(second_user)
    await db_session.commit()

    add_resp = await client.post(
        f"/api/v1/households/{hh_id}/members",
        json={"phone_number": "+911111100061"},
    )
    member_id = add_resp.json()["id"]

    resp = await client.put(
        f"/api/v1/households/{hh_id}/members/{member_id}",
        json={"role": "ADMIN"},
    )
    assert resp.status_code == 422


# ──────────────────────────────────────────────
# CreateHouseholdRecipeRuleRequest validation
# ──────────────────────────────────────────────


async def test_create_rule_invalid_target_type(client):
    """Invalid target_type should be rejected."""
    create_resp = await client.post(
        "/api/v1/households", json={"name": "Rule Val HH"}
    )
    hh_id = create_resp.json()["id"]

    resp = await client.post(
        f"/api/v1/households/{hh_id}/recipe-rules",
        json={
            "target_type": "INVALID",
            "action": "EXCLUDE",
            "target_name": "Test",
            "frequency_type": "NEVER",
        },
    )
    assert resp.status_code == 422


async def test_create_rule_invalid_action(client):
    """Invalid action should be rejected."""
    create_resp = await client.post(
        "/api/v1/households", json={"name": "Action Val HH"}
    )
    hh_id = create_resp.json()["id"]

    resp = await client.post(
        f"/api/v1/households/{hh_id}/recipe-rules",
        json={
            "target_type": "INGREDIENT",
            "action": "MODIFY",
            "target_name": "Test",
            "frequency_type": "NEVER",
        },
    )
    assert resp.status_code == 422


async def test_create_rule_frequency_count_too_low(client):
    """frequency_count below 1 should be rejected."""
    create_resp = await client.post(
        "/api/v1/households", json={"name": "Freq Val HH"}
    )
    hh_id = create_resp.json()["id"]

    resp = await client.post(
        f"/api/v1/households/{hh_id}/recipe-rules",
        json={
            "target_type": "INGREDIENT",
            "action": "INCLUDE",
            "target_name": "Dal",
            "frequency_type": "TIMES_PER_WEEK",
            "frequency_count": 0,
        },
    )
    assert resp.status_code == 422


async def test_create_rule_frequency_count_too_high(client):
    """frequency_count above 7 should be rejected."""
    create_resp = await client.post(
        "/api/v1/households", json={"name": "Freq Hi HH"}
    )
    hh_id = create_resp.json()["id"]

    resp = await client.post(
        f"/api/v1/households/{hh_id}/recipe-rules",
        json={
            "target_type": "INGREDIENT",
            "action": "INCLUDE",
            "target_name": "Dal",
            "frequency_type": "TIMES_PER_WEEK",
            "frequency_count": 8,
        },
    )
    assert resp.status_code == 422


async def test_create_rule_empty_target_name(client):
    """Empty target_name should be rejected."""
    create_resp = await client.post(
        "/api/v1/households", json={"name": "Empty Target HH"}
    )
    hh_id = create_resp.json()["id"]

    resp = await client.post(
        f"/api/v1/households/{hh_id}/recipe-rules",
        json={
            "target_type": "INGREDIENT",
            "action": "EXCLUDE",
            "target_name": "",
            "frequency_type": "NEVER",
        },
    )
    assert resp.status_code == 422


async def test_create_rule_invalid_meal_slot(client):
    """Invalid meal_slot should be rejected."""
    create_resp = await client.post(
        "/api/v1/households", json={"name": "Slot Val HH"}
    )
    hh_id = create_resp.json()["id"]

    resp = await client.post(
        f"/api/v1/households/{hh_id}/recipe-rules",
        json={
            "target_type": "INGREDIENT",
            "action": "INCLUDE",
            "target_name": "Chai",
            "frequency_type": "DAILY",
            "meal_slot": "BRUNCH",
        },
    )
    assert resp.status_code == 422


# ──────────────────────────────────────────────
# Query param validation
# ──────────────────────────────────────────────


async def test_stats_invalid_month(client):
    """Month outside 1-12 should be rejected."""
    create_resp = await client.post(
        "/api/v1/households", json={"name": "Stats Val HH"}
    )
    hh_id = create_resp.json()["id"]

    resp = await client.get(
        f"/api/v1/households/{hh_id}/stats/monthly",
        params={"month": 13, "year": 2026},
    )
    assert resp.status_code == 422


async def test_stats_invalid_year(client):
    """Year below 2020 should be rejected."""
    create_resp = await client.post(
        "/api/v1/households", json={"name": "Year Val HH"}
    )
    hh_id = create_resp.json()["id"]

    resp = await client.get(
        f"/api/v1/households/{hh_id}/stats/monthly",
        params={"month": 3, "year": 2019},
    )
    assert resp.status_code == 422


async def test_stats_missing_params(client):
    """Missing month/year should be rejected."""
    create_resp = await client.post(
        "/api/v1/households", json={"name": "Params Val HH"}
    )
    hh_id = create_resp.json()["id"]

    resp = await client.get(f"/api/v1/households/{hh_id}/stats/monthly")
    assert resp.status_code == 422


# ──────────────────────────────────────────────
# Business logic edge cases
# ──────────────────────────────────────────────


async def test_list_members_includes_left_members(client, db_session):
    """List members returns LEFT members too (not just ACTIVE)."""
    from tests.factories import make_user

    create_resp = await client.post(
        "/api/v1/households", json={"name": "Left Members HH"}
    )
    hh_id = create_resp.json()["id"]

    second_user = make_user(name="Will Leave", phone_number="+911111100062")
    db_session.add(second_user)
    await db_session.commit()

    add_resp = await client.post(
        f"/api/v1/households/{hh_id}/members",
        json={"phone_number": "+911111100062"},
    )
    member_id = add_resp.json()["id"]

    # Remove the member (sets status=LEFT)
    await client.delete(f"/api/v1/households/{hh_id}/members/{member_id}")

    # List should still include the LEFT member
    resp = await client.get(f"/api/v1/households/{hh_id}/members")
    assert resp.status_code == 200
    members = resp.json()
    statuses = {m["status"] for m in members}
    assert "LEFT" in statuses
    assert len(members) == 2  # Owner (ACTIVE) + removed (LEFT)


async def test_update_meal_item_invalid_status(client, db_session, test_user):
    """Invalid status value should be rejected by query param regex."""
    import uuid as uuid_mod
    from datetime import date

    from app.models.meal_plan import MealPlan, MealPlanItem

    create_resp = await client.post(
        "/api/v1/households", json={"name": "Status Val HH"}
    )
    hh_id = create_resp.json()["id"]

    plan = MealPlan(
        id=str(uuid_mod.uuid4()),
        user_id=test_user.id,
        household_id=hh_id,
        week_start_date=date(2026, 3, 9),
        week_end_date=date(2026, 3, 15),
        is_active=True,
    )
    db_session.add(plan)
    await db_session.flush()

    item = MealPlanItem(
        id=str(uuid_mod.uuid4()),
        meal_plan_id=plan.id,
        date=date(2026, 3, 9),
        meal_type="lunch",
        recipe_name="Dal Fry",
    )
    db_session.add(item)
    await db_session.commit()

    resp = await client.put(
        f"/api/v1/households/{hh_id}/meal-plans/{plan.id}/items/{item.id}/status",
        params={"status": "BURNED"},
    )
    assert resp.status_code == 422
