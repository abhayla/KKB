"""Tests for favorites API endpoints."""

import uuid

import pytest
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.user import User


async def test_add_favorite_201(client: AsyncClient):
    """POST /api/v1/favorites returns 201 with favorite data."""
    recipe_id = str(uuid.uuid4())
    response = await client.post(
        "/api/v1/favorites",
        json={"recipe_id": recipe_id},
    )
    assert response.status_code == 201
    data = response.json()
    assert data["recipe_id"] == recipe_id
    assert "id" in data
    assert "user_id" in data
    assert "created_at" in data


async def test_remove_favorite_204(client: AsyncClient):
    """DELETE /api/v1/favorites/{recipe_id} returns 204 after adding."""
    recipe_id = str(uuid.uuid4())
    # Add first
    add_resp = await client.post(
        "/api/v1/favorites",
        json={"recipe_id": recipe_id},
    )
    assert add_resp.status_code == 201

    # Remove
    del_resp = await client.delete(f"/api/v1/favorites/{recipe_id}")
    assert del_resp.status_code == 204


async def test_get_favorites_list(client: AsyncClient):
    """GET /api/v1/favorites returns list with correct count."""
    recipe_ids = [str(uuid.uuid4()) for _ in range(3)]
    for rid in recipe_ids:
        resp = await client.post(
            "/api/v1/favorites",
            json={"recipe_id": rid},
        )
        assert resp.status_code == 201

    response = await client.get("/api/v1/favorites")
    assert response.status_code == 200
    data = response.json()
    assert data["total"] == 3
    assert len(data["favorites"]) == 3
    returned_ids = {f["recipe_id"] for f in data["favorites"]}
    assert returned_ids == set(recipe_ids)


async def test_duplicate_favorite_409(client: AsyncClient):
    """POST same recipe twice returns 409."""
    recipe_id = str(uuid.uuid4())
    resp1 = await client.post(
        "/api/v1/favorites",
        json={"recipe_id": recipe_id},
    )
    assert resp1.status_code == 201

    resp2 = await client.post(
        "/api/v1/favorites",
        json={"recipe_id": recipe_id},
    )
    assert resp2.status_code == 409


async def test_favorites_scoped_to_user(
    db_session: AsyncSession, test_user: User, client: AsyncClient
):
    """User A can't see User B's favorites."""
    from tests.api.conftest import make_api_client

    # User A (default test user) adds a favorite
    recipe_a = str(uuid.uuid4())
    resp = await client.post(
        "/api/v1/favorites",
        json={"recipe_id": recipe_a},
    )
    assert resp.status_code == 201

    # Create User B
    user_b = User(
        id=str(uuid.uuid4()),
        firebase_uid="user-b-firebase-uid",
        email="userb@example.com",
        name="User B",
        is_onboarded=True,
        is_active=True,
    )
    db_session.add(user_b)
    await db_session.commit()

    # User B gets their own favorites — should be empty
    async with make_api_client(db_session, user_b) as client_b:
        resp_b = await client_b.get("/api/v1/favorites")
        assert resp_b.status_code == 200
        data_b = resp_b.json()
        assert data_b["total"] == 0
        assert len(data_b["favorites"]) == 0


async def test_get_favorites_unauthorized_401(
    unauthenticated_client: AsyncClient,
):
    """GET /api/v1/favorites without auth returns 401."""
    response = await unauthenticated_client.get("/api/v1/favorites")
    assert response.status_code == 401


async def test_remove_nonexistent_404(client: AsyncClient):
    """DELETE non-existent favorite returns 404."""
    fake_id = str(uuid.uuid4())
    response = await client.delete(f"/api/v1/favorites/{fake_id}")
    assert response.status_code == 404
