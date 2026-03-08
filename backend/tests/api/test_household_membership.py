"""
Tests for household membership flow endpoints:
invite code, join, leave, transfer ownership, update member, remove member.
"""

from datetime import datetime, timedelta, timezone

from tests.factories import make_household, make_household_member, make_user


# ── Invite Code ──────────────────────────────────────────────────────────────


async def test_refresh_invite_code(client, db_session):
    """Owner refreshes invite code — 200 with 8-char code + expires_at."""
    resp = await client.post("/api/v1/households", json={"name": "Sharma House"})
    assert resp.status_code == 201
    household_id = resp.json()["id"]

    resp = await client.post(f"/api/v1/households/{household_id}/invite-code")
    assert resp.status_code == 200
    data = resp.json()
    assert len(data["invite_code"]) == 8
    assert "expires_at" in data


async def test_refresh_invite_code_not_owner(client, db_session):
    """Non-owner tries to refresh invite code — 403."""
    other_user = make_user(name="Other Owner", phone_number="+919000000001")
    db_session.add(other_user)
    await db_session.flush()
    household = make_household(owner_id=other_user.id)
    db_session.add(household)
    await db_session.flush()
    owner_member = make_household_member(
        household_id=household.id,
        user_id=other_user.id,
        role="OWNER",
        can_edit_shared_plan=True,
    )
    db_session.add(owner_member)
    await db_session.commit()

    resp = await client.post(f"/api/v1/households/{household.id}/invite-code")
    assert resp.status_code == 403


# ── Join ─────────────────────────────────────────────────────────────────────


async def test_join_via_invite_code(client, db_session):
    """User joins household via valid invite code — 201 with role=MEMBER."""
    other_user = make_user(name="Host User", phone_number="+919000000002")
    db_session.add(other_user)
    await db_session.flush()
    household = make_household(owner_id=other_user.id)
    db_session.add(household)
    await db_session.flush()
    owner_member = make_household_member(
        household_id=household.id,
        user_id=other_user.id,
        role="OWNER",
        can_edit_shared_plan=True,
    )
    db_session.add(owner_member)

    household.invite_code = "TESTCODE"
    household.invite_code_expires_at = datetime.now(timezone.utc) + timedelta(days=7)
    await db_session.commit()

    resp = await client.post(
        "/api/v1/households/join", json={"invite_code": "TESTCODE"}
    )
    assert resp.status_code == 201
    data = resp.json()
    assert data["role"] == "MEMBER"


async def test_join_invalid_code(client):
    """Join with non-existent invite code — 404."""
    resp = await client.post(
        "/api/v1/households/join", json={"invite_code": "ZZZZZZZZ"}
    )
    assert resp.status_code == 404


async def test_join_expired_code(client, db_session):
    """Join with expired invite code — 404."""
    other_user = make_user(name="Expired Host", phone_number="+919000000003")
    db_session.add(other_user)
    await db_session.flush()
    household = make_household(owner_id=other_user.id)
    db_session.add(household)
    await db_session.flush()
    owner_member = make_household_member(
        household_id=household.id,
        user_id=other_user.id,
        role="OWNER",
        can_edit_shared_plan=True,
    )
    db_session.add(owner_member)

    household.invite_code = "EXPIRED1"
    household.invite_code_expires_at = datetime.now(timezone.utc) - timedelta(days=1)
    await db_session.commit()

    resp = await client.post(
        "/api/v1/households/join", json={"invite_code": "EXPIRED1"}
    )
    assert resp.status_code == 404


async def test_join_already_member(client, db_session):
    """User already a member tries to join again — 409."""
    other_user = make_user(name="Dup Host", phone_number="+919000000004")
    db_session.add(other_user)
    await db_session.flush()
    household = make_household(owner_id=other_user.id)
    db_session.add(household)
    await db_session.flush()
    owner_member = make_household_member(
        household_id=household.id,
        user_id=other_user.id,
        role="OWNER",
        can_edit_shared_plan=True,
    )
    db_session.add(owner_member)

    household.invite_code = "DUPJOIN1"
    household.invite_code_expires_at = datetime.now(timezone.utc) + timedelta(days=7)
    await db_session.commit()

    # First join — should succeed
    resp = await client.post(
        "/api/v1/households/join", json={"invite_code": "DUPJOIN1"}
    )
    assert resp.status_code == 201

    # Second join — should conflict
    resp = await client.post(
        "/api/v1/households/join", json={"invite_code": "DUPJOIN1"}
    )
    assert resp.status_code == 409


async def test_join_at_capacity(client, db_session):
    """Join when household is at max capacity — 400."""
    other_user = make_user(name="Full Host", phone_number="+919000000020")
    db_session.add(other_user)
    await db_session.flush()

    household = make_household(owner_id=other_user.id, max_members=1)
    db_session.add(household)
    await db_session.flush()

    owner_member = make_household_member(
        household_id=household.id,
        user_id=other_user.id,
        role="OWNER",
        can_edit_shared_plan=True,
    )
    db_session.add(owner_member)

    household.invite_code = "FULLHH01"
    household.invite_code_expires_at = datetime.now(timezone.utc) + timedelta(days=7)
    await db_session.commit()

    resp = await client.post(
        "/api/v1/households/join", json={"invite_code": "FULLHH01"}
    )
    assert resp.status_code == 400


# ── Leave ────────────────────────────────────────────────────────────────────


async def test_leave_household(client, db_session, test_user):
    """MEMBER leaves household — 204."""
    other_user = make_user(name="Leave Host", phone_number="+919000000005")
    db_session.add(other_user)
    await db_session.flush()
    household = make_household(owner_id=other_user.id)
    db_session.add(household)
    await db_session.flush()
    owner_member = make_household_member(
        household_id=household.id,
        user_id=other_user.id,
        role="OWNER",
        can_edit_shared_plan=True,
    )
    member = make_household_member(
        household_id=household.id, user_id=test_user.id, role="MEMBER"
    )
    db_session.add_all([owner_member, member])
    await db_session.commit()

    resp = await client.post(f"/api/v1/households/{household.id}/leave")
    assert resp.status_code == 204


async def test_leave_household_owner_blocked(client, db_session):
    """Owner tries to leave — 400 (must transfer ownership first)."""
    resp = await client.post("/api/v1/households", json={"name": "Owner Leave Test"})
    assert resp.status_code == 201
    household_id = resp.json()["id"]

    resp = await client.post(f"/api/v1/households/{household_id}/leave")
    assert resp.status_code == 400


# ── Transfer Ownership ───────────────────────────────────────────────────────


async def test_transfer_ownership(client, db_session):
    """Owner transfers ownership to another MEMBER — 204."""
    # Create household (test_user is OWNER)
    resp = await client.post("/api/v1/households", json={"name": "Transfer Test"})
    assert resp.status_code == 201
    household_id = resp.json()["id"]

    # Add second user as member
    second_user = make_user(name="New Owner", phone_number="+919000000006")
    db_session.add(second_user)
    await db_session.commit()

    resp = await client.post(
        f"/api/v1/households/{household_id}/members",
        json={"phone_number": "+919000000006"},
    )
    assert resp.status_code == 201
    member_id = resp.json()["id"]

    # Transfer ownership
    resp = await client.post(
        f"/api/v1/households/{household_id}/transfer-ownership",
        json={"new_owner_member_id": member_id},
    )
    assert resp.status_code == 204


async def test_transfer_to_metadata_only_member(client, db_session):
    """Transfer to member with user_id=None — 400."""
    resp = await client.post(
        "/api/v1/households", json={"name": "Metadata Transfer Test"}
    )
    assert resp.status_code == 201
    household_id = resp.json()["id"]

    # Add metadata-only member (no phone_number → no user_id linkage)
    metadata_member = make_household_member(
        household_id=household_id, user_id=None, role="MEMBER"
    )
    db_session.add(metadata_member)
    await db_session.commit()

    resp = await client.post(
        f"/api/v1/households/{household_id}/transfer-ownership",
        json={"new_owner_member_id": str(metadata_member.id)},
    )
    assert resp.status_code == 400


async def test_transfer_not_owner(client, db_session, test_user):
    """Non-owner tries to transfer — 403."""
    other_user = make_user(name="Real Owner", phone_number="+919000000007")
    db_session.add(other_user)
    await db_session.flush()
    household = make_household(owner_id=other_user.id)
    db_session.add(household)
    await db_session.flush()
    owner_member = make_household_member(
        household_id=household.id,
        user_id=other_user.id,
        role="OWNER",
        can_edit_shared_plan=True,
    )
    test_member = make_household_member(
        household_id=household.id, user_id=test_user.id, role="MEMBER"
    )
    db_session.add_all([owner_member, test_member])
    await db_session.commit()

    resp = await client.post(
        f"/api/v1/households/{household.id}/transfer-ownership",
        json={"new_owner_member_id": str(owner_member.id)},
    )
    assert resp.status_code == 403


# ── Update Member ────────────────────────────────────────────────────────────


async def test_update_member_portion_size(client, db_session):
    """Owner updates member's portion_size — 200."""
    resp = await client.post("/api/v1/households", json={"name": "Update Test"})
    assert resp.status_code == 201
    household_id = resp.json()["id"]

    second_user = make_user(name="Portion User", phone_number="+919000000008")
    db_session.add(second_user)
    await db_session.commit()

    resp = await client.post(
        f"/api/v1/households/{household_id}/members",
        json={"phone_number": "+919000000008"},
    )
    assert resp.status_code == 201
    member_id = resp.json()["id"]

    resp = await client.put(
        f"/api/v1/households/{household_id}/members/{member_id}",
        json={"portion_size": "LARGE"},
    )
    assert resp.status_code == 200
    assert resp.json()["portion_size"] == "LARGE"


async def test_update_member_change_owner_role_blocked(client, db_session):
    """Try to change OWNER's role — 400."""
    resp = await client.post("/api/v1/households", json={"name": "Role Block Test"})
    assert resp.status_code == 201
    household_id = resp.json()["id"]

    # Get the owner's member record
    resp = await client.get(f"/api/v1/households/{household_id}/members")
    assert resp.status_code == 200
    members = resp.json()
    owner_member = next(m for m in members if m["role"] == "OWNER")

    resp = await client.put(
        f"/api/v1/households/{household_id}/members/{owner_member['id']}",
        json={"role": "MEMBER"},
    )
    assert resp.status_code == 400


async def test_update_member_not_owner(client, db_session, test_user):
    """Non-owner cannot update member attributes — 403."""
    other_user = make_user(name="Update Owner", phone_number="+919000000021")
    db_session.add(other_user)
    await db_session.flush()

    household = make_household(owner_id=other_user.id)
    db_session.add(household)
    await db_session.flush()

    owner_member = make_household_member(
        household_id=household.id,
        user_id=other_user.id,
        role="OWNER",
        can_edit_shared_plan=True,
    )
    test_member = make_household_member(
        household_id=household.id, user_id=test_user.id, role="MEMBER"
    )
    db_session.add_all([owner_member, test_member])
    await db_session.commit()

    resp = await client.put(
        f"/api/v1/households/{household.id}/members/{test_member.id}",
        json={"portion_size": "LARGE"},
    )
    assert resp.status_code == 403


async def test_update_member_can_edit_shared_plan(client, db_session):
    """Owner grants can_edit_shared_plan to a member — 200."""
    resp = await client.post(
        "/api/v1/households", json={"name": "Edit Perm Test"}
    )
    assert resp.status_code == 201
    household_id = resp.json()["id"]

    second_user = make_user(name="Edit User", phone_number="+919000000022")
    db_session.add(second_user)
    await db_session.commit()

    resp = await client.post(
        f"/api/v1/households/{household_id}/members",
        json={"phone_number": "+919000000022"},
    )
    assert resp.status_code == 201
    member_id = resp.json()["id"]
    assert resp.json()["can_edit_shared_plan"] is False

    resp = await client.put(
        f"/api/v1/households/{household_id}/members/{member_id}",
        json={"can_edit_shared_plan": True},
    )
    assert resp.status_code == 200
    assert resp.json()["can_edit_shared_plan"] is True


# ── Remove Member ────────────────────────────────────────────────────────────


async def test_remove_member(client, db_session):
    """Owner removes a MEMBER — 204."""
    resp = await client.post("/api/v1/households", json={"name": "Remove Test"})
    assert resp.status_code == 201
    household_id = resp.json()["id"]

    second_user = make_user(name="Removable User", phone_number="+919000000009")
    db_session.add(second_user)
    await db_session.commit()

    resp = await client.post(
        f"/api/v1/households/{household_id}/members",
        json={"phone_number": "+919000000009"},
    )
    assert resp.status_code == 201
    member_id = resp.json()["id"]

    resp = await client.delete(f"/api/v1/households/{household_id}/members/{member_id}")
    assert resp.status_code == 204


async def test_remove_self_blocked(client, db_session):
    """Owner tries to remove own member record — 400."""
    resp = await client.post("/api/v1/households", json={"name": "Self Remove Test"})
    assert resp.status_code == 201
    household_id = resp.json()["id"]

    # Get the owner's member record
    resp = await client.get(f"/api/v1/households/{household_id}/members")
    assert resp.status_code == 200
    members = resp.json()
    owner_member = next(m for m in members if m["role"] == "OWNER")

    resp = await client.delete(
        f"/api/v1/households/{household_id}/members/{owner_member['id']}"
    )
    assert resp.status_code == 400


async def test_remove_member_not_owner(client, db_session, test_user):
    """Non-owner cannot remove a member — 403."""
    other_user = make_user(name="Remove Owner", phone_number="+919000000023")
    db_session.add(other_user)
    await db_session.flush()

    household = make_household(owner_id=other_user.id)
    db_session.add(household)
    await db_session.flush()

    owner_member = make_household_member(
        household_id=household.id,
        user_id=other_user.id,
        role="OWNER",
        can_edit_shared_plan=True,
    )
    test_member = make_household_member(
        household_id=household.id, user_id=test_user.id, role="MEMBER"
    )
    db_session.add_all([owner_member, test_member])
    await db_session.commit()

    resp = await client.delete(
        f"/api/v1/households/{household.id}/members/{owner_member.id}"
    )
    assert resp.status_code == 403


async def test_remove_member_not_found(client, db_session):
    """Remove non-existent member — 404."""
    import uuid

    resp = await client.post(
        "/api/v1/households", json={"name": "Remove 404 Test"}
    )
    assert resp.status_code == 201
    household_id = resp.json()["id"]

    resp = await client.delete(
        f"/api/v1/households/{household_id}/members/{uuid.uuid4()}"
    )
    assert resp.status_code == 404
