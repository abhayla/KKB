"""
Tests for the recipe rules sync endpoint (/api/v1/recipe-rules/sync).

Covers: basic sync, conflict resolution (LWW), batch operations,
edge cases (duplicates, last_sync_time filtering, timezone).
"""

import pytest
import pytest_asyncio
from datetime import datetime, timezone, timedelta
from uuid import uuid4

from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from tests.factories import make_preferences


# ==================== Fixtures ====================


@pytest_asyncio.fixture(autouse=True)
async def _ensure_preferences(db_session: AsyncSession, test_user):
    """Ensure user preferences exist (required by sync endpoint)."""
    prefs = make_preferences(test_user.id)
    db_session.add(prefs)
    await db_session.commit()


# ==================== Helpers ====================


def _make_rule_sync_item(**overrides) -> dict:
    """Helper to create a sync item for recipe rules."""
    defaults = {
        "id": str(uuid4()),
        "target_type": "INGREDIENT",
        "action": "INCLUDE",
        "target_name": "Test Item",
        "frequency_type": "DAILY",
        "enforcement": "REQUIRED",
        "is_active": True,
        "local_updated_at": datetime.now(timezone.utc).isoformat(),
    }
    defaults.update(overrides)
    return defaults


def _make_goal_sync_item(**overrides) -> dict:
    """Helper to create a sync item for nutrition goals."""
    defaults = {
        "id": str(uuid4()),
        "food_category": "PROTEIN",
        "weekly_target": 5,
        "current_progress": 0,
        "enforcement": "PREFERRED",
        "is_active": True,
        "local_updated_at": datetime.now(timezone.utc).isoformat(),
    }
    defaults.update(overrides)
    return defaults


# ==================== Basic Sync Tests ====================


@pytest.mark.asyncio
async def test_sync_empty_both_directions(client: AsyncClient):
    """Test sync with no pending changes in either direction."""
    response = await client.post(
        "/api/v1/recipe-rules/sync",
        json={
            "recipe_rules": [],
            "nutrition_goals": [],
            "last_sync_time": None,
        },
    )

    assert response.status_code == 200
    data = response.json()
    assert data["synced_rule_ids"] == []
    assert data["synced_goal_ids"] == []
    assert data["conflict_rule_ids"] == []
    assert data["conflict_goal_ids"] == []
    assert data["server_recipe_rules"] == []
    assert data["server_nutrition_goals"] == []
    assert "sync_time" in data


@pytest.mark.asyncio
async def test_sync_new_rule_from_client(client: AsyncClient):
    """Test syncing a new rule from client to server."""
    rule_item = _make_rule_sync_item(target_name="Paratha")

    response = await client.post(
        "/api/v1/recipe-rules/sync",
        json={
            "recipe_rules": [rule_item],
            "nutrition_goals": [],
            "last_sync_time": None,
        },
    )

    assert response.status_code == 200
    data = response.json()
    assert rule_item["id"] in data["synced_rule_ids"]

    # Verify rule exists on server
    get_response = await client.get(
        f"/api/v1/recipe-rules/{rule_item['id']}"
    )
    assert get_response.status_code == 200
    assert get_response.json()["target_name"] == "Paratha"


@pytest.mark.asyncio
async def test_sync_new_goal_from_client(client: AsyncClient):
    """Test syncing a new nutrition goal from client to server."""
    goal_item = _make_goal_sync_item(food_category="LEAFY_GREENS", weekly_target=7)

    response = await client.post(
        "/api/v1/recipe-rules/sync",
        json={
            "recipe_rules": [],
            "nutrition_goals": [goal_item],
            "last_sync_time": None,
        },
    )

    assert response.status_code == 200
    data = response.json()
    assert goal_item["id"] in data["synced_goal_ids"]


# ==================== Conflict Resolution Tests ====================


@pytest.mark.asyncio
async def test_sync_conflict_server_wins_older_client_timestamp(
    client: AsyncClient,
):
    """Test that server wins when client timestamp is older."""
    # Create rule on server (gets current timestamp)
    create_response = await client.post(
        "/api/v1/recipe-rules",
        json={
            "target_type": "INGREDIENT",
            "action": "INCLUDE",
            "target_name": "Samosa",
            "frequency_type": "TIMES_PER_WEEK",
            "frequency_count": 2,
            "enforcement": "PREFERRED",
            "is_active": True,
        },
    )
    rule_id = create_response.json()["id"]

    # Sync with old timestamp - server should win
    old_time = "2020-01-01T00:00:00+00:00"
    response = await client.post(
        "/api/v1/recipe-rules/sync",
        json={
            "recipe_rules": [
                _make_rule_sync_item(
                    id=rule_id,
                    target_name="Samosa Updated",
                    frequency_type="DAILY",
                    local_updated_at=old_time,
                )
            ],
            "nutrition_goals": [],
            "last_sync_time": None,
        },
    )

    assert response.status_code == 200
    data = response.json()
    assert rule_id in data["conflict_rule_ids"]
    assert rule_id not in data["synced_rule_ids"]


@pytest.mark.asyncio
async def test_sync_conflict_client_wins_newer_client_timestamp(
    client: AsyncClient,
):
    """Test that client wins when client timestamp is newer."""
    # Create rule on server
    create_response = await client.post(
        "/api/v1/recipe-rules",
        json={
            "target_type": "INGREDIENT",
            "action": "INCLUDE",
            "target_name": "Vada Pav",
            "frequency_type": "DAILY",
            "enforcement": "REQUIRED",
            "is_active": True,
        },
    )
    rule_id = create_response.json()["id"]

    # Sync with future timestamp - client should win
    future_time = (datetime.now(timezone.utc) + timedelta(hours=1)).isoformat()
    response = await client.post(
        "/api/v1/recipe-rules/sync",
        json={
            "recipe_rules": [
                _make_rule_sync_item(
                    id=rule_id,
                    target_name="Vada Pav Updated",
                    frequency_type="TIMES_PER_WEEK",
                    frequency_count=3,
                    local_updated_at=future_time,
                )
            ],
            "nutrition_goals": [],
            "last_sync_time": None,
        },
    )

    assert response.status_code == 200
    data = response.json()
    assert rule_id in data["synced_rule_ids"]

    # Verify the update was applied
    get_response = await client.get(f"/api/v1/recipe-rules/{rule_id}")
    assert get_response.json()["target_name"] == "Vada Pav Updated"


@pytest.mark.asyncio
async def test_sync_goal_conflict_server_wins(client: AsyncClient):
    """Test that server wins for goal sync with older client timestamp."""
    # Create goal on server
    create_response = await client.post(
        "/api/v1/nutrition-goals",
        json={
            "food_category": "PROTEIN",
            "weekly_target": 5,
            "enforcement": "PREFERRED",
            "is_active": True,
        },
    )
    goal_id = create_response.json()["id"]

    old_time = "2020-01-01T00:00:00+00:00"
    response = await client.post(
        "/api/v1/recipe-rules/sync",
        json={
            "recipe_rules": [],
            "nutrition_goals": [
                _make_goal_sync_item(
                    id=goal_id,
                    food_category="PROTEIN",
                    weekly_target=10,
                    local_updated_at=old_time,
                )
            ],
            "last_sync_time": None,
        },
    )

    assert response.status_code == 200
    data = response.json()
    assert goal_id in data["conflict_goal_ids"]


@pytest.mark.asyncio
async def test_sync_goal_conflict_client_wins(client: AsyncClient):
    """Test that client wins for goal sync with newer client timestamp."""
    create_response = await client.post(
        "/api/v1/nutrition-goals",
        json={
            "food_category": "FERMENTED",
            "weekly_target": 3,
            "enforcement": "PREFERRED",
            "is_active": True,
        },
    )
    goal_id = create_response.json()["id"]

    future_time = (datetime.now(timezone.utc) + timedelta(hours=1)).isoformat()
    response = await client.post(
        "/api/v1/recipe-rules/sync",
        json={
            "recipe_rules": [],
            "nutrition_goals": [
                _make_goal_sync_item(
                    id=goal_id,
                    food_category="FERMENTED",
                    weekly_target=8,
                    local_updated_at=future_time,
                )
            ],
            "last_sync_time": None,
        },
    )

    assert response.status_code == 200
    data = response.json()
    assert goal_id in data["synced_goal_ids"]


# ==================== Batch Operation Tests ====================


@pytest.mark.asyncio
async def test_sync_batch_five_rules(client: AsyncClient):
    """Test syncing 5 new rules in a single batch."""
    rules = [
        _make_rule_sync_item(target_name=name)
        for name in ["Chai", "Dosa", "Idli", "Poha", "Upma"]
    ]

    response = await client.post(
        "/api/v1/recipe-rules/sync",
        json={
            "recipe_rules": rules,
            "nutrition_goals": [],
            "last_sync_time": None,
        },
    )

    assert response.status_code == 200
    data = response.json()
    assert len(data["synced_rule_ids"]) == 5

    # Verify all exist
    list_response = await client.get("/api/v1/recipe-rules")
    assert list_response.json()["total_count"] == 5


@pytest.mark.asyncio
async def test_sync_batch_three_goals(client: AsyncClient):
    """Test syncing 3 new goals in a single batch."""
    goals = [
        _make_goal_sync_item(food_category=cat)
        for cat in ["PROTEIN", "LEAFY_GREENS", "FERMENTED"]
    ]

    response = await client.post(
        "/api/v1/recipe-rules/sync",
        json={
            "recipe_rules": [],
            "nutrition_goals": goals,
            "last_sync_time": None,
        },
    )

    assert response.status_code == 200
    data = response.json()
    assert len(data["synced_goal_ids"]) == 3


@pytest.mark.asyncio
async def test_sync_batch_mixed_rules_and_goals(client: AsyncClient):
    """Test syncing rules and goals together."""
    rules = [
        _make_rule_sync_item(target_name="Roti"),
        _make_rule_sync_item(target_name="Sabzi"),
    ]
    goals = [_make_goal_sync_item(food_category="OMEGA_3")]

    response = await client.post(
        "/api/v1/recipe-rules/sync",
        json={
            "recipe_rules": rules,
            "nutrition_goals": goals,
            "last_sync_time": None,
        },
    )

    assert response.status_code == 200
    data = response.json()
    assert len(data["synced_rule_ids"]) == 2
    assert len(data["synced_goal_ids"]) == 1


@pytest.mark.asyncio
async def test_sync_large_batch_ten_rules(client: AsyncClient):
    """Test syncing 10 rules in a single batch."""
    items = [f"Item_{i}" for i in range(10)]
    rules = [_make_rule_sync_item(target_name=name) for name in items]

    response = await client.post(
        "/api/v1/recipe-rules/sync",
        json={
            "recipe_rules": rules,
            "nutrition_goals": [],
            "last_sync_time": None,
        },
    )

    assert response.status_code == 200
    assert len(response.json()["synced_rule_ids"]) == 10


# ==================== Edge Case Tests ====================


@pytest.mark.asyncio
async def test_sync_duplicate_rule_in_batch(client: AsyncClient):
    """Test that syncing a duplicate rule (same target/action) is treated as conflict."""
    # Create rule on server first
    await client.post(
        "/api/v1/recipe-rules",
        json={
            "target_type": "INGREDIENT",
            "action": "INCLUDE",
            "target_name": "Existing Chai",
            "frequency_type": "DAILY",
            "enforcement": "REQUIRED",
            "is_active": True,
        },
    )

    # Try to sync a new rule with the same target_name + action (different ID)
    dup_rule = _make_rule_sync_item(
        target_name="Existing Chai",
        action="INCLUDE",
        target_type="INGREDIENT",
    )

    response = await client.post(
        "/api/v1/recipe-rules/sync",
        json={
            "recipe_rules": [dup_rule],
            "nutrition_goals": [],
            "last_sync_time": None,
        },
    )

    assert response.status_code == 200
    data = response.json()
    # Duplicate should be treated as conflict, not synced
    assert dup_rule["id"] in data["conflict_rule_ids"]


@pytest.mark.asyncio
async def test_sync_with_last_sync_time_filters(client: AsyncClient):
    """Test that last_sync_time filters server response."""
    # Create a rule via direct API (gets server timestamp)
    create_response = await client.post(
        "/api/v1/recipe-rules",
        json={
            "target_type": "INGREDIENT",
            "action": "INCLUDE",
            "target_name": "Old Rule",
            "frequency_type": "DAILY",
            "enforcement": "REQUIRED",
            "is_active": True,
        },
    )
    assert create_response.status_code == 201

    # Use a future last_sync_time - should filter out the rule
    future_time = (datetime.now(timezone.utc) + timedelta(hours=1)).isoformat()
    response = await client.post(
        "/api/v1/recipe-rules/sync",
        json={
            "recipe_rules": [],
            "nutrition_goals": [],
            "last_sync_time": future_time,
        },
    )

    assert response.status_code == 200
    data = response.json()
    # Rule was created before last_sync_time, so should not appear
    assert len(data["server_recipe_rules"]) == 0


@pytest.mark.asyncio
async def test_sync_returns_all_server_rules(client: AsyncClient):
    """Test that sync without last_sync_time returns all server rules."""
    # Create 3 rules via API
    for name in ["Rule1", "Rule2", "Rule3"]:
        await client.post(
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

    response = await client.post(
        "/api/v1/recipe-rules/sync",
        json={
            "recipe_rules": [],
            "nutrition_goals": [],
            "last_sync_time": None,
        },
    )

    assert response.status_code == 200
    data = response.json()
    assert len(data["server_recipe_rules"]) == 3


@pytest.mark.asyncio
async def test_sync_timezone_aware_comparison(client: AsyncClient):
    """Test that timezone-naive and timezone-aware timestamps are handled correctly."""
    # Create rule on server
    create_response = await client.post(
        "/api/v1/recipe-rules",
        json={
            "target_type": "INGREDIENT",
            "action": "INCLUDE",
            "target_name": "TZ Test",
            "frequency_type": "DAILY",
            "enforcement": "REQUIRED",
            "is_active": True,
        },
    )
    rule_id = create_response.json()["id"]

    # Sync with timezone-aware future timestamp (should win)
    future_time = (datetime.now(timezone.utc) + timedelta(hours=2)).isoformat()
    response = await client.post(
        "/api/v1/recipe-rules/sync",
        json={
            "recipe_rules": [
                _make_rule_sync_item(
                    id=rule_id,
                    target_name="TZ Updated",
                    local_updated_at=future_time,
                )
            ],
            "nutrition_goals": [],
            "last_sync_time": None,
        },
    )

    assert response.status_code == 200
    data = response.json()
    assert rule_id in data["synced_rule_ids"]
