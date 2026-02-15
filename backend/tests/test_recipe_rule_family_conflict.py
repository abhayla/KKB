"""Tests for family safety conflict detection on recipe rule creation.

Verifies that INCLUDE rules conflicting with family member health conditions
or dietary restrictions are blocked with 409, unless ?force=true is used.
"""

import pytest
import pytest_asyncio
from httpx import AsyncClient

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


def _rule_payload(target_name: str, action: str = "INCLUDE") -> dict:
    """Helper to create a minimal recipe rule payload."""
    return {
        "target_type": "RECIPE",
        "action": action,
        "target_name": target_name,
        "frequency_type": "WEEKLY",
        "frequency_count": 1,
        "enforcement": "REQUIRED",
        "is_active": True,
    }


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
        detail = response.json()["detail"]
        assert "Gulab Jamun" in detail
        assert "diabetic" in detail.lower()

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
        detail = response.json()["detail"]
        assert "Onion Curry" in detail
        assert "jain" in detail.lower()

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
        detail = response.json()["detail"]
        assert "Aloo Paratha" in detail
        assert "aloo" in detail

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
        """?force=true bypasses conflict check and creates rule."""
        response = await client.post(
            "/api/v1/recipe-rules?force=true",
            json=_rule_payload("Gulab Jamun"),
        )
        assert response.status_code == 201
        data = response.json()
        assert data["target_name"] == "Gulab Jamun"

    @pytest.mark.anyio
    async def test_conflict_error_includes_member_name(
        self, client: AsyncClient, jain_member
    ):
        """409 detail includes member name and conflicting keyword."""
        response = await client.post(
            "/api/v1/recipe-rules",
            json=_rule_payload("Garlic Naan"),
        )
        assert response.status_code == 409
        detail = response.json()["detail"]
        assert "Dadiji" in detail
        assert "garlic" in detail
