"""Tests for cooking activity recording endpoint."""
import pytest
from httpx import AsyncClient


async def test_record_cooking_activity_returns_200(client: AsyncClient):
    """POST /stats/cooking-activity should return status and streak."""
    response = await client.post("/api/v1/stats/cooking-activity")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "recorded"
    assert "new_streak" in data


async def test_record_cooking_activity_increments_on_second_call(client: AsyncClient):
    """Two calls on the same day should increment meals_cooked."""
    response1 = await client.post("/api/v1/stats/cooking-activity")
    assert response1.status_code == 200

    response2 = await client.post("/api/v1/stats/cooking-activity")
    assert response2.status_code == 200
    assert response2.json()["status"] == "recorded"


async def test_record_cooking_activity_unauthorized(unauthenticated_client: AsyncClient):
    """Unauthenticated request should return 401."""
    response = await unauthenticated_client.post("/api/v1/stats/cooking-activity")
    assert response.status_code == 401
