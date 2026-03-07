"""Tests for family safety conflict detection on recipe rule creation.

Verifies that INCLUDE rules conflicting with family member health conditions
or dietary restrictions are blocked with 409 structured ConflictResponse,
unless force_override=true is set in the request body.
"""

import pytest
import pytest_asyncio
from httpx import AsyncClient

from app.models.recipe_rule import RecipeRule
from app.models.user import FamilyMember


@pytest_asyncio.fixture
async def diabetic_member(db_session, test_user):
    """Create a diabetic family member for the test user."""
    member = FamilyMember(
        id="fm-diabetic-001",
        user_id=test_user.id,
        name="Dadaji",
        age_group="senior",
        dietary_restrictions=[],
        health_conditions=["diabetic"],
    )
    db_session.add(member)
    await db_session.commit()
    return member


@pytest_asyncio.fixture
async def jain_member(db_session, test_user):
    """Create a Jain family member for the test user."""
    member = FamilyMember(
        id="fm-jain-001",
        user_id=test_user.id,
        name="Dadiji",
        age_group="senior",
        dietary_restrictions=["jain"],
        health_conditions=[],
    )
    db_session.add(member)
    await db_session.commit()
    return member


def _rule_payload(target_name: str, action: str = "INCLUDE", **kwargs) -> dict:
    """Helper to create a minimal recipe rule payload."""
    payload = {
        "target_type": "RECIPE",
        "action": action,
        "target_name": target_name,
        "frequency_type": "WEEKLY",
        "frequency_count": 1,
        "enforcement": "REQUIRED",
        "is_active": True,
    }
    payload.update(kwargs)
    return payload


class TestRecipeRuleFamilyConflict:
    """Test family safety conflict detection on recipe rule creation."""

    @pytest.mark.anyio
    async def test_include_sweet_blocked_for_diabetic(
        self, client: AsyncClient, diabetic_member
    ):
        """Gulab Jamun INCLUDE blocked when diabetic family member exists."""
        response = await client.post(
            "/api/v1/recipe-rules",
            json=_rule_payload("Gulab Jamun"),
        )
        assert response.status_code == 409
        data = response.json()
        assert data["conflict_type"] == "family_safety"
        assert len(data["conflict_details"]) >= 1
        detail = data["conflict_details"][0]
        assert detail["member_name"] == "Dadaji"
        assert detail["rule_target"] == "Gulab Jamun"

    @pytest.mark.anyio
    async def test_include_onion_blocked_for_jain(
        self, client: AsyncClient, jain_member
    ):
        """Onion Curry INCLUDE blocked when Jain family member exists."""
        response = await client.post(
            "/api/v1/recipe-rules",
            json=_rule_payload("Onion Curry"),
        )
        assert response.status_code == 409
        data = response.json()
        assert data["conflict_type"] == "family_safety"
        assert any(
            d["member_name"] == "Dadiji" for d in data["conflict_details"]
        )

    @pytest.mark.anyio
    async def test_include_aloo_blocked_for_jain(
        self, client: AsyncClient, jain_member
    ):
        """Aloo Paratha INCLUDE blocked for Jain (Hindi alias)."""
        response = await client.post(
            "/api/v1/recipe-rules",
            json=_rule_payload("Aloo Paratha"),
        )
        assert response.status_code == 409
        data = response.json()
        assert data["conflict_type"] == "family_safety"
        assert any(
            d["keyword"] == "aloo" for d in data["conflict_details"]
        )

    @pytest.mark.anyio
    async def test_exclude_rule_no_conflict_check(
        self, client: AsyncClient, diabetic_member
    ):
        """EXCLUDE rules are always allowed regardless of family members."""
        response = await client.post(
            "/api/v1/recipe-rules",
            json=_rule_payload("Gulab Jamun", action="EXCLUDE"),
        )
        assert response.status_code == 201

    @pytest.mark.anyio
    async def test_no_family_members_no_conflict(self, client: AsyncClient):
        """INCLUDE rule allowed when no family members exist."""
        response = await client.post(
            "/api/v1/recipe-rules",
            json=_rule_payload("Gulab Jamun"),
        )
        assert response.status_code == 201

    @pytest.mark.anyio
    async def test_force_override_creates_rule(
        self, client: AsyncClient, diabetic_member
    ):
        """force_override=true in body bypasses conflict check and creates rule."""
        response = await client.post(
            "/api/v1/recipe-rules",
            json=_rule_payload("Gulab Jamun", force_override=True),
        )
        assert response.status_code == 201
        data = response.json()
        assert data["target_name"] == "Gulab Jamun"
        assert data["force_override"] is True

    @pytest.mark.anyio
    async def test_conflict_response_structured_details(
        self, client: AsyncClient, jain_member
    ):
        """409 body has structured conflict_details array."""
        response = await client.post(
            "/api/v1/recipe-rules",
            json=_rule_payload("Garlic Naan"),
        )
        assert response.status_code == 409
        data = response.json()
        assert "conflict_type" in data
        assert "conflict_details" in data
        assert isinstance(data["conflict_details"], list)
        assert len(data["conflict_details"]) >= 1
        detail = data["conflict_details"][0]
        assert "member_name" in detail
        assert "condition" in detail
        assert "keyword" in detail
        assert "rule_target" in detail
        assert detail["member_name"] == "Dadiji"
        assert "garlic" in detail["keyword"]

    @pytest.mark.anyio
    async def test_force_override_persisted(
        self, client: AsyncClient, db_session, diabetic_member
    ):
        """force_override flag is persisted and returned on GET."""
        # Create with override
        create_resp = await client.post(
            "/api/v1/recipe-rules",
            json=_rule_payload("Gulab Jamun", force_override=True),
        )
        assert create_resp.status_code == 201
        rule_id = create_resp.json()["id"]

        # GET it back
        get_resp = await client.get(f"/api/v1/recipe-rules/{rule_id}")
        assert get_resp.status_code == 200
        assert get_resp.json()["force_override"] is True

    @pytest.mark.anyio
    async def test_force_override_survives_sync(
        self, client: AsyncClient, db_session, diabetic_member
    ):
        """force_override roundtrips through sync endpoint."""
        from datetime import datetime, timezone

        # Create rule with override
        create_resp = await client.post(
            "/api/v1/recipe-rules",
            json=_rule_payload("Gulab Jamun", force_override=True),
        )
        assert create_resp.status_code == 201
        rule_id = create_resp.json()["id"]

        # Sync with the rule (client update)
        sync_resp = await client.post(
            "/api/v1/recipe-rules/sync",
            json={
                "recipe_rules": [
                    {
                        "id": rule_id,
                        "target_type": "RECIPE",
                        "action": "INCLUDE",
                        "target_name": "Gulab Jamun",
                        "frequency_type": "WEEKLY",
                        "frequency_count": 1,
                        "enforcement": "REQUIRED",
                        "is_active": True,
                        "force_override": True,
                        "local_updated_at": datetime.now(timezone.utc).isoformat(),
                    }
                ],
                "nutrition_goals": [],
            },
        )
        assert sync_resp.status_code == 200

        # Verify rule still has force_override
        get_resp = await client.get(f"/api/v1/recipe-rules/{rule_id}")
        assert get_resp.status_code == 200
        assert get_resp.json()["force_override"] is True
