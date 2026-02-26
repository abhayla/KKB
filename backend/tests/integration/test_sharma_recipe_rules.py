"""
Requirement: #48 - FR-011: Sharma family Recipe Rules test suite with UI-to-DB verification

Tests all 7 Sharma family recipe rules:
1. Chai → Breakfast (INCLUDE, DAILY, REQUIRED)
2. Chai → Snacks (INCLUDE, DAILY, REQUIRED)
3. Moringa (INCLUDE, 1x/week, PREFERRED)
4. Paneer (EXCLUDE, NEVER, REQUIRED)
5. Eggs (INCLUDE, 4x/week, PREFERRED)
6. Chicken (INCLUDE, 2x/week, PREFERRED)
7. Green Leafy goal (5/week, PREFERRED)
"""

import pytest
from uuid import uuid4

import pytest_asyncio
from httpx import ASGITransport, AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user
from app.db.database import get_db
from app.main import app
from app.models.user import User, UserPreferences


# ==================== Fixtures ====================


@pytest_asyncio.fixture
async def sharma_user(db_session: AsyncSession) -> User:
    """Create the Sharma family test user (vegetarian, sattvic, family_size=3)."""
    user_id = str(uuid4())
    user = User(
        id=user_id,
        firebase_uid=f"firebase-sharma-{user_id}",
        email=f"sharma-{user_id}@example.com",
        name="Sharma Family",
        is_onboarded=True,
        is_active=True,
    )
    db_session.add(user)

    prefs = UserPreferences(
        id=str(uuid4()),
        user_id=user_id,
        dietary_type="non_vegetarian",
        family_size=3,
    )
    db_session.add(prefs)

    await db_session.commit()
    await db_session.refresh(user)
    return user


@pytest_asyncio.fixture
async def sharma_client(
    db_session: AsyncSession, sharma_user: User
) -> AsyncClient:
    """Create a test client authenticated as the Sharma user."""

    async def override_get_db():
        yield db_session

    async def override_get_current_user():
        return sharma_user

    app.dependency_overrides[get_db] = override_get_db
    app.dependency_overrides[get_current_user] = override_get_current_user

    async with AsyncClient(
        transport=ASGITransport(app=app),
        base_url="http://test",
    ) as ac:
        yield ac

    app.dependency_overrides.clear()


# ==================== Onboarding Data Constants ====================


SHARMA_ONBOARDING_PREFERENCES = {
    "household_size": 3,
    "primary_diet": "non_vegetarian",
    "dietary_restrictions": [],
    "cuisine_preferences": ["north", "west"],
    "spice_level": "medium",
    "disliked_ingredients": ["Karela", "Baingan"],
    "weekday_cooking_time": 30,
    "weekend_cooking_time": 60,
    "busy_days": ["MONDAY", "WEDNESDAY"],
}

SHARMA_FAMILY_MEMBERS = [
    {"name": "Priya Sharma", "age_group": "adult", "dietary_restrictions": [], "health_conditions": []},
    {"name": "Amit Sharma", "age_group": "child", "dietary_restrictions": [], "health_conditions": []},
    {"name": "Dadi Sharma", "age_group": "senior", "dietary_restrictions": ["low_salt"], "health_conditions": ["diabetes"]},
]


# ==================== Recipe Rule Constants ====================


CHAI_BREAKFAST_RULE = {
    "target_type": "INGREDIENT",
    "action": "INCLUDE",
    "target_name": "Chai",
    "frequency_type": "DAILY",
    "enforcement": "REQUIRED",
    "meal_slot": "BREAKFAST",
    "is_active": True,
}

CHAI_SNACKS_RULE = {
    "target_type": "INGREDIENT",
    "action": "INCLUDE",
    "target_name": "Chai",
    "frequency_type": "DAILY",
    "enforcement": "REQUIRED",
    "meal_slot": "SNACKS",
    "is_active": True,
}

MORINGA_RULE = {
    "target_type": "INGREDIENT",
    "action": "INCLUDE",
    "target_name": "Moringa",
    "frequency_type": "TIMES_PER_WEEK",
    "frequency_count": 1,
    "enforcement": "PREFERRED",
    "is_active": True,
}

PANEER_EXCLUDE_RULE = {
    "target_type": "INGREDIENT",
    "action": "EXCLUDE",
    "target_name": "Paneer",
    "frequency_type": "NEVER",
    "enforcement": "REQUIRED",
    "is_active": True,
}

EGGS_RULE = {
    "target_type": "INGREDIENT",
    "action": "INCLUDE",
    "target_name": "Eggs",
    "frequency_type": "TIMES_PER_WEEK",
    "frequency_count": 4,
    "enforcement": "PREFERRED",
    "is_active": True,
}

CHICKEN_RULE = {
    "target_type": "INGREDIENT",
    "action": "INCLUDE",
    "target_name": "Chicken",
    "frequency_type": "TIMES_PER_WEEK",
    "frequency_count": 2,
    "enforcement": "PREFERRED",
    "is_active": True,
}

GREEN_LEAFY_GOAL = {
    "food_category": "LEAFY_GREENS",
    "weekly_target": 5,
    "enforcement": "PREFERRED",
    "is_active": True,
}


# ==================== Individual Rule Creation Tests ====================


@pytest.mark.asyncio
async def test_sharma_create_chai_breakfast_rule(sharma_client: AsyncClient):
    """Test creating Chai breakfast INCLUDE rule (DAILY, REQUIRED)."""
    response = await sharma_client.post(
        "/api/v1/recipe-rules",
        json=CHAI_BREAKFAST_RULE,
    )

    assert response.status_code == 201
    data = response.json()
    assert data["target_name"] == "Chai"
    assert data["action"] == "INCLUDE"
    assert data["frequency_type"] == "DAILY"
    assert data["meal_slot"] == "BREAKFAST"
    assert data["enforcement"] == "REQUIRED"
    assert data["is_active"] is True


@pytest.mark.asyncio
async def test_sharma_create_chai_snacks_rule(sharma_client: AsyncClient):
    """Test creating Chai snacks INCLUDE rule (DAILY, REQUIRED)."""
    response = await sharma_client.post(
        "/api/v1/recipe-rules",
        json=CHAI_SNACKS_RULE,
    )

    assert response.status_code == 201
    data = response.json()
    assert data["target_name"] == "Chai"
    assert data["action"] == "INCLUDE"
    assert data["frequency_type"] == "DAILY"
    assert data["meal_slot"] == "SNACKS"
    assert data["enforcement"] == "REQUIRED"


@pytest.mark.asyncio
async def test_sharma_create_moringa_rule(sharma_client: AsyncClient):
    """Test creating Moringa INCLUDE rule (1x/week, PREFERRED)."""
    response = await sharma_client.post(
        "/api/v1/recipe-rules",
        json=MORINGA_RULE,
    )

    assert response.status_code == 201
    data = response.json()
    assert data["target_name"] == "Moringa"
    assert data["action"] == "INCLUDE"
    assert data["frequency_type"] == "TIMES_PER_WEEK"
    assert data["frequency_count"] == 1
    assert data["enforcement"] == "PREFERRED"


@pytest.mark.asyncio
async def test_sharma_create_paneer_exclude_rule(sharma_client: AsyncClient):
    """Test creating Paneer EXCLUDE rule (NEVER, REQUIRED)."""
    response = await sharma_client.post(
        "/api/v1/recipe-rules",
        json=PANEER_EXCLUDE_RULE,
    )

    assert response.status_code == 201
    data = response.json()
    assert data["target_name"] == "Paneer"
    assert data["action"] == "EXCLUDE"
    assert data["frequency_type"] == "NEVER"
    assert data["enforcement"] == "REQUIRED"


@pytest.mark.asyncio
async def test_sharma_create_eggs_rule(sharma_client: AsyncClient):
    """Test creating Eggs INCLUDE rule (4x/week, PREFERRED)."""
    response = await sharma_client.post(
        "/api/v1/recipe-rules",
        json=EGGS_RULE,
    )

    assert response.status_code == 201
    data = response.json()
    assert data["target_name"] == "Eggs"
    assert data["action"] == "INCLUDE"
    assert data["frequency_type"] == "TIMES_PER_WEEK"
    assert data["frequency_count"] == 4
    assert data["enforcement"] == "PREFERRED"


@pytest.mark.asyncio
async def test_sharma_create_chicken_rule(sharma_client: AsyncClient):
    """Test creating Chicken INCLUDE rule (2x/week, PREFERRED)."""
    response = await sharma_client.post(
        "/api/v1/recipe-rules",
        json=CHICKEN_RULE,
    )

    assert response.status_code == 201
    data = response.json()
    assert data["target_name"] == "Chicken"
    assert data["action"] == "INCLUDE"
    assert data["frequency_type"] == "TIMES_PER_WEEK"
    assert data["frequency_count"] == 2
    assert data["enforcement"] == "PREFERRED"


@pytest.mark.asyncio
async def test_sharma_create_green_leafy_goal(sharma_client: AsyncClient):
    """Test creating Green Leafy nutrition goal (5/week, PREFERRED)."""
    response = await sharma_client.post(
        "/api/v1/nutrition-goals",
        json=GREEN_LEAFY_GOAL,
    )

    assert response.status_code == 201
    data = response.json()
    assert data["food_category"] == "LEAFY_GREENS"
    assert data["weekly_target"] == 5
    assert data["enforcement"] == "PREFERRED"
    assert data["current_progress"] == 0


# ==================== Composite Tests ====================


@pytest.mark.asyncio
async def test_sharma_all_rules_listed(sharma_client: AsyncClient):
    """Create all 6 rules + 1 goal, verify GET returns correct counts."""
    # Create all 6 recipe rules
    for rule_data in [CHAI_BREAKFAST_RULE, CHAI_SNACKS_RULE, MORINGA_RULE, PANEER_EXCLUDE_RULE, EGGS_RULE, CHICKEN_RULE]:
        resp = await sharma_client.post("/api/v1/recipe-rules", json=rule_data)
        assert resp.status_code == 201

    # Create nutrition goal
    resp = await sharma_client.post("/api/v1/nutrition-goals", json=GREEN_LEAFY_GOAL)
    assert resp.status_code == 201

    # Verify recipe rules count
    rules_resp = await sharma_client.get("/api/v1/recipe-rules")
    assert rules_resp.status_code == 200
    rules_data = rules_resp.json()
    assert rules_data["total_count"] == 6
    assert len(rules_data["rules"]) == 6

    # Verify nutrition goals count
    goals_resp = await sharma_client.get("/api/v1/nutrition-goals")
    assert goals_resp.status_code == 200
    goals_data = goals_resp.json()
    assert goals_data["total_count"] == 1
    assert len(goals_data["goals"]) == 1


@pytest.mark.asyncio
async def test_sharma_delete_and_recreate_rule(sharma_client: AsyncClient):
    """Delete a rule then recreate it, verify new ID is assigned."""
    # Create chai breakfast rule
    create_resp = await sharma_client.post(
        "/api/v1/recipe-rules", json=CHAI_BREAKFAST_RULE
    )
    assert create_resp.status_code == 201
    original_id = create_resp.json()["id"]

    # Delete it
    delete_resp = await sharma_client.delete(f"/api/v1/recipe-rules/{original_id}")
    assert delete_resp.status_code == 204

    # Verify deleted
    get_resp = await sharma_client.get(f"/api/v1/recipe-rules/{original_id}")
    assert get_resp.status_code == 404

    # Recreate
    recreate_resp = await sharma_client.post(
        "/api/v1/recipe-rules", json=CHAI_BREAKFAST_RULE
    )
    assert recreate_resp.status_code == 201
    new_id = recreate_resp.json()["id"]

    # Verify new ID
    assert new_id != original_id


@pytest.mark.asyncio
async def test_sharma_rule_fields_match(sharma_client: AsyncClient):
    """Create chai breakfast rule and verify every field in response."""
    response = await sharma_client.post(
        "/api/v1/recipe-rules", json=CHAI_BREAKFAST_RULE
    )
    assert response.status_code == 201
    data = response.json()

    # Verify all fields
    assert "id" in data
    assert data["target_type"] == "INGREDIENT"
    assert data["action"] == "INCLUDE"
    assert data["target_name"] == "Chai"
    assert data["frequency_type"] == "DAILY"
    assert data["enforcement"] == "REQUIRED"
    assert data["meal_slot"] == "BREAKFAST"
    assert data["is_active"] is True
    assert "created_at" in data

    # Verify via GET
    rule_id = data["id"]
    get_resp = await sharma_client.get(f"/api/v1/recipe-rules/{rule_id}")
    assert get_resp.status_code == 200
    get_data = get_resp.json()
    assert get_data["id"] == rule_id
    assert get_data["target_name"] == "Chai"
    assert get_data["meal_slot"] == "BREAKFAST"


@pytest.mark.asyncio
async def test_sharma_nutrition_goal_fields_match(sharma_client: AsyncClient):
    """Create green leafy goal and verify every field in response."""
    response = await sharma_client.post(
        "/api/v1/nutrition-goals", json=GREEN_LEAFY_GOAL
    )
    assert response.status_code == 201
    data = response.json()

    assert "id" in data
    assert data["food_category"] == "LEAFY_GREENS"
    assert data["weekly_target"] == 5
    assert data["current_progress"] == 0
    assert data["enforcement"] == "PREFERRED"
    assert data["is_active"] is True
    assert "created_at" in data


@pytest.mark.asyncio
async def test_sharma_duplicate_nutrition_goal_rejected(sharma_client: AsyncClient):
    """Creating the same LEAFY_GREENS goal twice should return 409."""
    # First creation
    resp1 = await sharma_client.post("/api/v1/nutrition-goals", json=GREEN_LEAFY_GOAL)
    assert resp1.status_code == 201

    # Duplicate should fail
    resp2 = await sharma_client.post("/api/v1/nutrition-goals", json=GREEN_LEAFY_GOAL)
    assert resp2.status_code == 409


# ==================== Onboarding Preferences Roundtrip Test ====================


@pytest.mark.asyncio
async def test_sharma_onboarding_preferences_roundtrip(sharma_client: AsyncClient):
    """Requirement: #52 - FR-014: PUT preferences → GET /users/me → verify all fields roundtrip."""
    # PUT preferences using the onboarding data
    put_resp = await sharma_client.put(
        "/api/v1/users/preferences",
        json=SHARMA_ONBOARDING_PREFERENCES,
    )
    assert put_resp.status_code == 200

    # GET /users/me to verify persistence
    get_resp = await sharma_client.get("/api/v1/users/me")
    assert get_resp.status_code == 200
    data = get_resp.json()

    # Verify user is marked as onboarded
    assert data["is_onboarded"] is True

    # Verify preferences roundtrip
    prefs = data["preferences"]
    assert prefs["household_size"] == SHARMA_ONBOARDING_PREFERENCES["household_size"]
    assert prefs["dietary_type"] == SHARMA_ONBOARDING_PREFERENCES["primary_diet"]
    assert prefs["dietary_restrictions"] == SHARMA_ONBOARDING_PREFERENCES["dietary_restrictions"]
    assert prefs["cuisine_preferences"] == SHARMA_ONBOARDING_PREFERENCES["cuisine_preferences"]
    assert prefs["spice_level"] == SHARMA_ONBOARDING_PREFERENCES["spice_level"]
    assert prefs["disliked_ingredients"] == SHARMA_ONBOARDING_PREFERENCES["disliked_ingredients"]
    assert prefs["weekday_cooking_time_minutes"] == SHARMA_ONBOARDING_PREFERENCES["weekday_cooking_time"]
    assert prefs["weekend_cooking_time_minutes"] == SHARMA_ONBOARDING_PREFERENCES["weekend_cooking_time"]
    assert prefs["busy_days"] == SHARMA_ONBOARDING_PREFERENCES["busy_days"]
