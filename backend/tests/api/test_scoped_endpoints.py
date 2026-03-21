"""Tests for scope query parameter on stats, grocery, and recipe_rules endpoints.

Gap 1: Add `scope` query parameter to stats, grocery, recipe_rules endpoints.
When scope=family and user has no household, graceful fallback to personal data.
When scope=personal (default), existing behavior unchanged.
"""

import uuid
from datetime import date, timedelta, timezone, datetime

import pytest
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.meal_plan import MealPlan, MealPlanItem
from app.models.recipe_rule import RecipeRule
from app.models.stats import CookingStreak, CookingDay
from app.models.user import User


# ==================== Stats Scope Tests ====================


async def test_stats_personal_scope(client: AsyncClient, db_session: AsyncSession, test_user: User):
    """GET /stats/streak?scope=personal returns user-only data (existing behavior)."""
    resp = await client.get("/api/v1/stats/streak", params={"scope": "personal"})
    assert resp.status_code == 200
    data = resp.json()
    assert "current_streak" in data
    assert "longest_streak" in data


async def test_stats_family_scope_no_household(client: AsyncClient, db_session: AsyncSession, test_user: User):
    """GET /stats/streak?scope=family without household returns personal data (graceful fallback)."""
    resp = await client.get("/api/v1/stats/streak", params={"scope": "family"})
    assert resp.status_code == 200
    data = resp.json()
    # Should still return valid stats (fallback to personal)
    assert "current_streak" in data
    assert "longest_streak" in data


# ==================== Grocery Scope Tests ====================


async def test_grocery_personal_scope(client: AsyncClient, db_session: AsyncSession, test_user: User):
    """GET /grocery?scope=personal works (returns 404 if no plan, which is existing behavior)."""
    resp = await client.get("/api/v1/grocery", params={"scope": "personal"})
    # Without a meal plan, grocery returns 404 — that's expected existing behavior
    assert resp.status_code == 404


async def test_grocery_family_scope_no_household(client: AsyncClient, db_session: AsyncSession, test_user: User):
    """GET /grocery?scope=family without household gracefully falls back to personal data."""
    resp = await client.get("/api/v1/grocery", params={"scope": "family"})
    # Without a meal plan and no household, should still return 404 (same as personal)
    assert resp.status_code == 404


# ==================== Recipe Rules Scope Tests ====================


async def test_recipe_rules_personal_scope(client: AsyncClient, db_session: AsyncSession, test_user: User):
    """GET /recipe-rules?scope=personal returns only user's own rules."""
    # Create a recipe rule for the test user
    rule = RecipeRule(
        id=str(uuid.uuid4()),
        user_id=test_user.id,
        target_type="INGREDIENT",
        action="EXCLUDE",
        target_name="Mushroom",
        frequency_type="NEVER",
        is_active=True,
        sync_status="SYNCED",
        created_at=datetime.now(timezone.utc),
        updated_at=datetime.now(timezone.utc),
    )
    db_session.add(rule)
    await db_session.commit()

    resp = await client.get("/api/v1/recipe-rules", params={"scope": "personal"})
    assert resp.status_code == 200
    data = resp.json()
    assert data["total_count"] == 1
    assert data["rules"][0]["target_name"] == "Mushroom"


async def test_recipe_rules_family_scope_no_household(client: AsyncClient, db_session: AsyncSession, test_user: User):
    """GET /recipe-rules?scope=family without household gracefully returns personal rules."""
    # Create a recipe rule for the test user
    rule = RecipeRule(
        id=str(uuid.uuid4()),
        user_id=test_user.id,
        target_type="INGREDIENT",
        action="EXCLUDE",
        target_name="Onion",
        frequency_type="NEVER",
        is_active=True,
        sync_status="SYNCED",
        created_at=datetime.now(timezone.utc),
        updated_at=datetime.now(timezone.utc),
    )
    db_session.add(rule)
    await db_session.commit()

    resp = await client.get("/api/v1/recipe-rules", params={"scope": "family"})
    assert resp.status_code == 200
    data = resp.json()
    # Should fallback to personal rules since user has no household
    assert data["total_count"] == 1
    assert data["rules"][0]["target_name"] == "Onion"
