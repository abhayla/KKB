"""Systematic error case testing for API endpoints.

Covers error responses (400, 401, 404, 422) for endpoints that lack
dedicated error case tests. Avoids duplicating tests that already exist
in endpoint-specific test files.

Run with: PYTHONPATH=. pytest tests/api/test_error_cases.py -v
"""

import pytest
from datetime import datetime, timezone
from unittest.mock import AsyncMock, patch
from uuid import uuid4

from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.user import User
from app.schemas.chat import ChatResponse, ChatMessageResponse


# ==================== Recipe Error Cases ====================


@pytest.mark.asyncio
async def test_get_recipe_nonexistent_returns_404(client: AsyncClient):
    """GET /recipes/{id} with a nonexistent UUID returns 404."""
    fake_id = str(uuid4())
    response = await client.get(f"/api/v1/recipes/{fake_id}")
    assert response.status_code == 404
    data = response.json()
    assert "detail" in data


# ==================== Chat Error Cases ====================


@pytest.mark.asyncio
async def test_chat_message_empty_content_422(authenticated_client: AsyncClient):
    """POST /chat/message with empty string fails Pydantic validation (min_length=1)."""
    response = await authenticated_client.post(
        "/api/v1/chat/message",
        json={"message": ""},
    )
    # ChatMessageRequest has min_length=1, so empty string returns 422
    assert response.status_code == 422


@pytest.mark.asyncio
async def test_chat_message_missing_content_422(authenticated_client: AsyncClient):
    """POST /chat/message with missing 'message' field fails validation."""
    response = await authenticated_client.post(
        "/api/v1/chat/message",
        json={},
    )
    assert response.status_code == 422


@pytest.mark.asyncio
async def test_chat_unauthenticated_401(unauthenticated_client: AsyncClient):
    """POST /chat/message without auth returns 401."""
    response = await unauthenticated_client.post(
        "/api/v1/chat/message",
        json={"message": "Hello"},
    )
    assert response.status_code == 401


@pytest.mark.asyncio
async def test_chat_history_unauthenticated_401(unauthenticated_client: AsyncClient):
    """GET /chat/history without auth returns 401."""
    response = await unauthenticated_client.get("/api/v1/chat/history")
    assert response.status_code == 401


# ==================== Scope Parameter Error Cases ====================


@pytest.mark.asyncio
async def test_scope_parameter_invalid_value_treated_as_personal(
    client: AsyncClient, db_session: AsyncSession, test_user: User
):
    """GET /stats/streak?scope=INVALID should not crash.

    The endpoint accepts any string for scope but only acts on 'family'.
    An invalid value should be treated like 'personal' (the default).
    """
    response = await client.get(
        "/api/v1/stats/streak", params={"scope": "INVALID"}
    )
    assert response.status_code == 200
    data = response.json()
    assert "current_streak" in data


@pytest.mark.asyncio
async def test_scope_parameter_missing_defaults_to_personal(
    client: AsyncClient, db_session: AsyncSession, test_user: User
):
    """GET /stats/streak without scope parameter defaults to personal."""
    response = await client.get("/api/v1/stats/streak")
    assert response.status_code == 200
    data = response.json()
    assert "current_streak" in data
    assert "longest_streak" in data


# ==================== Notification Error Cases ====================


@pytest.mark.asyncio
async def test_delete_notification_not_found_404(authenticated_client: AsyncClient):
    """DELETE /notifications/{id} with nonexistent ID returns 404."""
    response = await authenticated_client.delete(
        "/api/v1/notifications/nonexistent-notification-id"
    )
    assert response.status_code == 404


@pytest.mark.asyncio
async def test_delete_notification_unauthenticated_401(
    unauthenticated_client: AsyncClient,
):
    """DELETE /notifications/{id} without auth returns 401."""
    response = await unauthenticated_client.delete(
        "/api/v1/notifications/some-notification-id"
    )
    assert response.status_code == 401


# ==================== User Preferences Error Cases ====================


@pytest.mark.asyncio
async def test_update_preferences_invalid_household_size_422(
    authenticated_client: AsyncClient,
):
    """PUT /users/preferences with household_size=0 fails validation (ge=1)."""
    response = await authenticated_client.put(
        "/api/v1/users/preferences",
        json={"household_size": 0},
    )
    assert response.status_code == 422


@pytest.mark.asyncio
async def test_update_preferences_household_size_too_large_422(
    authenticated_client: AsyncClient,
):
    """PUT /users/preferences with household_size=100 fails validation (le=20)."""
    response = await authenticated_client.put(
        "/api/v1/users/preferences",
        json={"household_size": 100},
    )
    assert response.status_code == 422


@pytest.mark.asyncio
async def test_update_preferences_items_per_meal_out_of_range_422(
    authenticated_client: AsyncClient,
):
    """PUT /users/preferences with items_per_meal=10 fails validation (le=4)."""
    response = await authenticated_client.put(
        "/api/v1/users/preferences",
        json={"items_per_meal": 10},
    )
    assert response.status_code == 422


# ==================== Meal Plan Error Cases ====================


@pytest.mark.asyncio
async def test_get_current_meal_plan_not_found(client: AsyncClient, test_user: User):
    """GET /meal-plans/current returns 404 when no meal plan exists."""
    with patch(
        "app.api.v1.endpoints.meal_plans.MealPlanRepository"
    ) as MockRepo:
        mock_repo = MockRepo.return_value
        mock_repo.get_current_for_user = AsyncMock(return_value=None)

        response = await client.get("/api/v1/meal-plans/current")
        assert response.status_code == 404
        data = response.json()
        assert "detail" in data


@pytest.mark.asyncio
async def test_get_meal_plan_by_id_not_found(client: AsyncClient, test_user: User):
    """GET /meal-plans/{id} returns 404 for nonexistent plan."""
    with patch(
        "app.api.v1.endpoints.meal_plans.MealPlanRepository"
    ) as MockRepo:
        mock_repo = MockRepo.return_value
        mock_repo.get_by_id = AsyncMock(return_value=None)

        fake_id = str(uuid4())
        response = await client.get(f"/api/v1/meal-plans/{fake_id}")
        assert response.status_code == 404


@pytest.mark.asyncio
async def test_meal_plans_unauthenticated_401(unauthenticated_client: AsyncClient):
    """GET /meal-plans/current without auth returns 401."""
    response = await unauthenticated_client.get("/api/v1/meal-plans/current")
    assert response.status_code == 401
