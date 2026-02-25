"""
Tests for recipe rule lifecycle operations: edit lifecycle, normalization,
partial updates, cross-user isolation, and ordering.
"""

import pytest
from datetime import datetime, timezone

import pytest_asyncio
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.user import User

from tests.api.conftest import make_api_client
from tests.factories import make_user, make_preferences


# ==================== Fixtures ====================


@pytest_asyncio.fixture
async def test_user(db_session: AsyncSession) -> User:
    """Create a test user in the test database."""
    user = make_user(name="Lifecycle Test User")
    db_session.add(user)
    prefs = make_preferences(user.id)
    db_session.add(prefs)
    await db_session.commit()
    await db_session.refresh(user)
    return user


@pytest_asyncio.fixture
async def test_user_b(db_session: AsyncSession) -> User:
    """Create a second test user for cross-user isolation tests."""
    user = make_user(name="Lifecycle Test User B")
    db_session.add(user)
    prefs = make_preferences(user.id, dietary_type="non-vegetarian", family_size=2)
    db_session.add(prefs)
    await db_session.commit()
    await db_session.refresh(user)
    return user


@pytest_asyncio.fixture
async def authenticated_client(
    db_session: AsyncSession, test_user: User
) -> AsyncClient:
    """Create a test client authenticated as test_user."""
    async with make_api_client(db_session, test_user) as c:
        yield c


# Helper to create a rule and return its ID
async def _create_rule(client: AsyncClient, **overrides) -> str:
    defaults = {
        "target_type": "INGREDIENT",
        "action": "INCLUDE",
        "target_name": "Chai",
        "frequency_type": "DAILY",
        "enforcement": "REQUIRED",
        "is_active": True,
    }
    defaults.update(overrides)
    response = await client.post("/api/v1/recipe-rules", json=defaults)
    assert response.status_code == 201
    return response.json()["id"]


# ==================== Edit Lifecycle Tests ====================


@pytest.mark.asyncio
async def test_update_rule_change_frequency_type(authenticated_client: AsyncClient):
    """Test changing frequency type from DAILY to TIMES_PER_WEEK."""
    rule_id = await _create_rule(
        authenticated_client, target_name="Dosa", frequency_type="DAILY"
    )

    response = await authenticated_client.put(
        f"/api/v1/recipe-rules/{rule_id}",
        json={"frequency_type": "TIMES_PER_WEEK", "frequency_count": 3},
    )

    assert response.status_code == 200
    data = response.json()
    assert data["frequency_type"] == "TIMES_PER_WEEK"
    assert data["frequency_count"] == 3


@pytest.mark.asyncio
async def test_update_rule_change_frequency_count(authenticated_client: AsyncClient):
    """Test changing frequency count from 2 to 5."""
    rule_id = await _create_rule(
        authenticated_client,
        target_name="Paneer",
        frequency_type="TIMES_PER_WEEK",
        frequency_count=2,
    )

    response = await authenticated_client.put(
        f"/api/v1/recipe-rules/{rule_id}",
        json={"frequency_count": 5},
    )

    assert response.status_code == 200
    assert response.json()["frequency_count"] == 5


@pytest.mark.asyncio
async def test_update_rule_change_enforcement(authenticated_client: AsyncClient):
    """Test changing enforcement from REQUIRED to PREFERRED."""
    rule_id = await _create_rule(
        authenticated_client, target_name="Rice", enforcement="REQUIRED"
    )

    response = await authenticated_client.put(
        f"/api/v1/recipe-rules/{rule_id}",
        json={"enforcement": "PREFERRED"},
    )

    assert response.status_code == 200
    assert response.json()["enforcement"] == "PREFERRED"


@pytest.mark.asyncio
async def test_update_rule_change_meal_slot(authenticated_client: AsyncClient):
    """Test changing meal slot from BREAKFAST to DINNER."""
    rule_id = await _create_rule(
        authenticated_client, target_name="Dal", meal_slot="BREAKFAST"
    )

    response = await authenticated_client.put(
        f"/api/v1/recipe-rules/{rule_id}",
        json={"meal_slot": "DINNER"},
    )

    assert response.status_code == 200
    assert response.json()["meal_slot"] == "DINNER"


@pytest.mark.asyncio
async def test_update_rule_toggle_active(authenticated_client: AsyncClient):
    """Test toggling is_active True -> False -> True."""
    rule_id = await _create_rule(
        authenticated_client, target_name="Poha", is_active=True
    )

    # Deactivate
    r1 = await authenticated_client.put(
        f"/api/v1/recipe-rules/{rule_id}", json={"is_active": False}
    )
    assert r1.status_code == 200
    assert r1.json()["is_active"] is False

    # Reactivate
    r2 = await authenticated_client.put(
        f"/api/v1/recipe-rules/{rule_id}", json={"is_active": True}
    )
    assert r2.status_code == 200
    assert r2.json()["is_active"] is True


@pytest.mark.asyncio
async def test_update_rule_change_multiple_fields_at_once(
    authenticated_client: AsyncClient,
):
    """Test updating multiple fields in a single PUT."""
    rule_id = await _create_rule(
        authenticated_client,
        target_name="Idli",
        frequency_type="DAILY",
        enforcement="REQUIRED",
        meal_slot="BREAKFAST",
    )

    response = await authenticated_client.put(
        f"/api/v1/recipe-rules/{rule_id}",
        json={
            "frequency_type": "TIMES_PER_WEEK",
            "frequency_count": 4,
            "enforcement": "PREFERRED",
            "meal_slot": "LUNCH",
            "is_active": False,
        },
    )

    assert response.status_code == 200
    data = response.json()
    assert data["frequency_type"] == "TIMES_PER_WEEK"
    assert data["frequency_count"] == 4
    assert data["enforcement"] == "PREFERRED"
    assert data["meal_slot"] == "LUNCH"
    assert data["is_active"] is False


# ==================== Normalization Tests ====================


@pytest.mark.asyncio
async def test_create_rule_meal_slot_case_normalized(
    authenticated_client: AsyncClient,
):
    """Test that lowercase meal_slot is normalized to UPPERCASE."""
    response = await authenticated_client.post(
        "/api/v1/recipe-rules",
        json={
            "target_type": "INGREDIENT",
            "action": "INCLUDE",
            "target_name": "Paratha",
            "frequency_type": "DAILY",
            "enforcement": "REQUIRED",
            "meal_slot": "breakfast",
        },
    )

    assert response.status_code == 201
    assert response.json()["meal_slot"] == "BREAKFAST"


@pytest.mark.asyncio
async def test_update_rule_meal_slot_case_normalized(
    authenticated_client: AsyncClient,
):
    """Test that lowercase meal_slot in update is normalized to UPPERCASE."""
    rule_id = await _create_rule(
        authenticated_client, target_name="Chole", meal_slot="BREAKFAST"
    )

    response = await authenticated_client.put(
        f"/api/v1/recipe-rules/{rule_id}",
        json={"meal_slot": "lunch"},
    )

    assert response.status_code == 200
    assert response.json()["meal_slot"] == "LUNCH"


# ==================== Partial Update Tests ====================


@pytest.mark.asyncio
async def test_update_rule_partial_only_name(authenticated_client: AsyncClient):
    """Test partial update changing only target_name."""
    rule_id = await _create_rule(
        authenticated_client,
        target_name="Old Name",
        frequency_type="DAILY",
        enforcement="REQUIRED",
    )

    response = await authenticated_client.put(
        f"/api/v1/recipe-rules/{rule_id}",
        json={"target_name": "New Name"},
    )

    assert response.status_code == 200
    data = response.json()
    assert data["target_name"] == "New Name"
    assert data["frequency_type"] == "DAILY"  # unchanged
    assert data["enforcement"] == "REQUIRED"  # unchanged


@pytest.mark.asyncio
async def test_update_rule_partial_only_active(authenticated_client: AsyncClient):
    """Test partial update changing only is_active."""
    rule_id = await _create_rule(
        authenticated_client,
        target_name="Samosa",
        frequency_type="TIMES_PER_WEEK",
        frequency_count=2,
        enforcement="PREFERRED",
    )

    response = await authenticated_client.put(
        f"/api/v1/recipe-rules/{rule_id}",
        json={"is_active": False},
    )

    assert response.status_code == 200
    data = response.json()
    assert data["is_active"] is False
    assert data["target_name"] == "Samosa"  # unchanged
    assert data["frequency_type"] == "TIMES_PER_WEEK"  # unchanged
    assert data["frequency_count"] == 2  # unchanged


# ==================== Cross-User Isolation Tests ====================


@pytest.mark.asyncio
async def test_user_b_cannot_see_user_a_rules(
    db_session: AsyncSession,
    test_user: User,
    test_user_b: User,
):
    """Test that user B cannot see user A's rules."""
    # Create rule as user A
    async with make_api_client(db_session, test_user) as client_a:
        await _create_rule(client_a, target_name="Secret Chai")

    # List as user B - should see nothing
    async with make_api_client(db_session, test_user_b) as client_b:
        response = await client_b.get("/api/v1/recipe-rules")
        assert response.status_code == 200
        assert response.json()["total_count"] == 0


@pytest.mark.asyncio
async def test_user_b_cannot_update_user_a_rules(
    db_session: AsyncSession,
    test_user: User,
    test_user_b: User,
):
    """Test that user B cannot update user A's rules."""
    # Create rule as user A
    async with make_api_client(db_session, test_user) as client_a:
        rule_id = await _create_rule(client_a, target_name="Guarded Dal")

    # Try to update as user B
    async with make_api_client(db_session, test_user_b) as client_b:
        response = await client_b.put(
            f"/api/v1/recipe-rules/{rule_id}",
            json={"target_name": "Hacked Dal"},
        )
        assert response.status_code == 404


@pytest.mark.asyncio
async def test_user_b_cannot_delete_user_a_rules(
    db_session: AsyncSession,
    test_user: User,
    test_user_b: User,
):
    """Test that user B cannot delete user A's rules."""
    # Create rule as user A
    async with make_api_client(db_session, test_user) as client_a:
        rule_id = await _create_rule(client_a, target_name="Protected Rice")

    # Try to delete as user B
    async with make_api_client(db_session, test_user_b) as client_b:
        response = await client_b.delete(f"/api/v1/recipe-rules/{rule_id}")
        assert response.status_code == 404

    # Verify rule still exists for user A
    async with make_api_client(db_session, test_user) as client_a:
        get_response = await client_a.get(f"/api/v1/recipe-rules/{rule_id}")
        assert get_response.status_code == 200


# ==================== Ordering Tests ====================


@pytest.mark.asyncio
async def test_rules_ordered_by_created_at_desc(authenticated_client: AsyncClient):
    """Test that rules are returned newest first."""
    names = ["Alpha", "Beta", "Gamma"]
    for name in names:
        await authenticated_client.post(
            "/api/v1/recipe-rules",
            json={
                "target_type": "INGREDIENT",
                "action": "INCLUDE",
                "target_name": name,
                "frequency_type": "DAILY",
                "enforcement": "REQUIRED",
                "is_active": True,
            },
        )

    response = await authenticated_client.get("/api/v1/recipe-rules")
    assert response.status_code == 200
    rules = response.json()["rules"]
    assert len(rules) == 3

    # Newest first: Gamma was created last
    assert rules[0]["target_name"] == "Gamma"
    assert rules[2]["target_name"] == "Alpha"


@pytest.mark.asyncio
async def test_goals_ordered_by_created_at_desc(authenticated_client: AsyncClient):
    """Test that nutrition goals are returned newest first."""
    categories = ["LEAFY_GREENS", "PROTEIN", "FERMENTED"]
    for cat in categories:
        await authenticated_client.post(
            "/api/v1/nutrition-goals",
            json={
                "food_category": cat,
                "weekly_target": 3,
                "enforcement": "PREFERRED",
                "is_active": True,
            },
        )

    response = await authenticated_client.get("/api/v1/nutrition-goals")
    assert response.status_code == 200
    goals = response.json()["goals"]
    assert len(goals) == 3

    # Newest first: FERMENTED was created last
    assert goals[0]["food_category"] == "FERMENTED"
    assert goals[2]["food_category"] == "LEAFY_GREENS"
