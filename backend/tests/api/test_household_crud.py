"""
Tests for household CRUD API endpoints.

Covers: create, get, update, deactivate households + member management.
"""

import uuid


from tests.factories import make_household, make_household_member, make_user


# ──────────────────────────────────────────────
# CREATE
# ──────────────────────────────────────────────


async def test_create_household(client):
    resp = await client.post("/api/v1/households", json={"name": "Sharma Family"})
    assert resp.status_code == 201
    data = resp.json()
    assert data["name"] == "Sharma Family"
    assert data["is_active"] is True
    assert data["member_count"] == 1
    assert "owner_id" in data


async def test_create_household_unauthenticated(unauthenticated_client):
    resp = await unauthenticated_client.post(
        "/api/v1/households", json={"name": "Unauthenticated"}
    )
    assert resp.status_code == 401


# ──────────────────────────────────────────────
# GET
# ──────────────────────────────────────────────


async def test_get_household(client):
    # Create via API so test_user is the owner/member
    create_resp = await client.post(
        "/api/v1/households", json={"name": "Get Test Household"}
    )
    assert create_resp.status_code == 201
    household_id = create_resp.json()["id"]

    resp = await client.get(f"/api/v1/households/{household_id}")
    assert resp.status_code == 200
    data = resp.json()
    assert data["household"]["name"] == "Get Test Household"
    assert "members" in data


async def test_get_household_not_member(client, db_session, test_user):
    # Create household owned by a different user
    second_user = make_user(name="Other Owner", phone_number="+911111100001")
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

    resp = await client.get(f"/api/v1/households/{household.id}")
    assert resp.status_code == 403


async def test_get_household_not_found(client):
    random_id = str(uuid.uuid4())
    resp = await client.get(f"/api/v1/households/{random_id}")
    assert resp.status_code == 404


# ──────────────────────────────────────────────
# UPDATE
# ──────────────────────────────────────────────


async def test_update_household_name(client):
    create_resp = await client.post("/api/v1/households", json={"name": "Old Name"})
    assert create_resp.status_code == 201
    household_id = create_resp.json()["id"]

    resp = await client.put(
        f"/api/v1/households/{household_id}", json={"name": "New Name"}
    )
    assert resp.status_code == 200
    assert resp.json()["name"] == "New Name"


async def test_update_household_max_members(client):
    create_resp = await client.post(
        "/api/v1/households", json={"name": "Capacity Test"}
    )
    assert create_resp.status_code == 201
    household_id = create_resp.json()["id"]

    resp = await client.put(
        f"/api/v1/households/{household_id}", json={"max_members": 10}
    )
    assert resp.status_code == 200
    assert resp.json()["max_members"] == 10


async def test_update_household_not_owner(client, db_session):
    second_user = make_user(name="Real Owner", phone_number="+911111100002")
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

    resp = await client.put(
        f"/api/v1/households/{household.id}", json={"name": "Hijacked"}
    )
    assert resp.status_code == 403


# ──────────────────────────────────────────────
# DEACTIVATE
# ──────────────────────────────────────────────


async def test_deactivate_household(client):
    create_resp = await client.post(
        "/api/v1/households", json={"name": "To Deactivate"}
    )
    assert create_resp.status_code == 201
    household_id = create_resp.json()["id"]

    resp = await client.delete(f"/api/v1/households/{household_id}")
    assert resp.status_code == 204


async def test_deactivate_household_with_members(client, db_session):
    # Create household with owner (test_user) + an extra member
    create_resp = await client.post("/api/v1/households", json={"name": "Has Members"})
    assert create_resp.status_code == 201
    household_id = create_resp.json()["id"]

    second_user = make_user(name="Extra Member", phone_number="+911111100003")
    db_session.add(second_user)
    await db_session.flush()

    extra_member = make_household_member(
        household_id=household_id,
        user_id=second_user.id,
        role="MEMBER",
        can_edit_shared_plan=False,
    )
    db_session.add(extra_member)
    await db_session.commit()

    resp = await client.delete(f"/api/v1/households/{household_id}")
    assert resp.status_code == 400


# ──────────────────────────────────────────────
# LIST MEMBERS
# ──────────────────────────────────────────────


async def test_list_members(client):
    create_resp = await client.post(
        "/api/v1/households", json={"name": "Members List Test"}
    )
    assert create_resp.status_code == 201
    household_id = create_resp.json()["id"]

    resp = await client.get(f"/api/v1/households/{household_id}/members")
    assert resp.status_code == 200
    members = resp.json()
    assert isinstance(members, list)
    assert len(members) == 1


# ──────────────────────────────────────────────
# ADD MEMBER
# ──────────────────────────────────────────────


async def test_add_member_existing_user(client, db_session):
    create_resp = await client.post(
        "/api/v1/households", json={"name": "Add Member Test"}
    )
    assert create_resp.status_code == 201
    household_id = create_resp.json()["id"]

    second_user = make_user(name="Invitee", phone_number="+911234567890")
    db_session.add(second_user)
    await db_session.commit()

    resp = await client.post(
        f"/api/v1/households/{household_id}/members",
        json={"phone_number": "+911234567890", "is_temporary": False},
    )
    assert resp.status_code == 201


async def test_add_member_unknown_phone(client):
    create_resp = await client.post(
        "/api/v1/households", json={"name": "Unknown Phone Test"}
    )
    assert create_resp.status_code == 201
    household_id = create_resp.json()["id"]

    resp = await client.post(
        f"/api/v1/households/{household_id}/members",
        json={"phone_number": "+919999999999", "is_temporary": False},
    )
    assert resp.status_code == 201
    data = resp.json()
    assert data.get("user_id") is None


async def test_add_member_duplicate(client, db_session):
    create_resp = await client.post(
        "/api/v1/households", json={"name": "Dup Member Test"}
    )
    assert create_resp.status_code == 201
    household_id = create_resp.json()["id"]

    dup_user = make_user(name="Dup User", phone_number="+911111100004")
    db_session.add(dup_user)
    await db_session.commit()

    await client.post(
        f"/api/v1/households/{household_id}/members",
        json={"phone_number": "+911111100004", "is_temporary": False},
    )

    resp = await client.post(
        f"/api/v1/households/{household_id}/members",
        json={"phone_number": "+911111100004", "is_temporary": False},
    )
    assert resp.status_code == 409


async def test_add_member_at_capacity(client, db_session, test_user):
    # Create household with max_members=1 directly in DB (schema rejects <2 via API)
    household = make_household(owner_id=test_user.id, max_members=1)
    db_session.add(household)
    await db_session.flush()
    owner_member = make_household_member(
        household_id=household.id,
        user_id=test_user.id,
        role="OWNER",
        can_edit_shared_plan=True,
    )
    db_session.add(owner_member)
    await db_session.commit()

    resp = await client.post(
        f"/api/v1/households/{household.id}/members",
        json={"phone_number": "+919876543210", "is_temporary": False},
    )
    assert resp.status_code == 400


async def test_add_member_not_owner(client, db_session):
    second_user = make_user(name="Actual Owner", phone_number="+911111100005")
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
        f"/api/v1/households/{household.id}/members",
        json={"phone_number": "+910000000000", "is_temporary": False},
    )
    assert resp.status_code == 403
