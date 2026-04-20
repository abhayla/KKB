"""Tests for meal_plan_service — read paths.

Covers:
- _get_day_name: pure helper (4 tests)
- get_current_meal_plan: NotFound when no plan (DB-backed, 1 test)
- get_meal_plan_by_id: invalid UUID + not-found + user-scoped (3 tests)

The write paths (swap_meal_item, lock_meal_item, create_meal_plan) need
substantial Recipe/MealPlanItem/Nutrition fixtures and are covered by the
existing integration tests under tests/services/test_ai_meal_service.py
and test_family_aware_meal_generation.py.
"""

import uuid
from datetime import date, datetime, timedelta, timezone

import pytest
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.exceptions import NotFoundError
from app.models.meal_plan import MealPlan
from app.models.user import User
from app.services.meal_plan_service import (
    _get_day_name,
    get_current_meal_plan,
    get_meal_plan_by_id,
)


# ==================== _get_day_name ====================


class TestGetDayName:
    def test_monday(self):
        # 2026-05-11 is a Monday
        assert _get_day_name(date(2026, 5, 11)) == "Monday"

    def test_wednesday(self):
        assert _get_day_name(date(2026, 5, 13)) == "Wednesday"

    def test_sunday(self):
        assert _get_day_name(date(2026, 5, 17)) == "Sunday"

    def test_leap_year_day(self):
        # 2024-02-29 is a Thursday — sanity on leap-year arithmetic
        assert _get_day_name(date(2024, 2, 29)) == "Thursday"


# ==================== get_current_meal_plan ====================


@pytest.mark.asyncio
async def test_get_current_meal_plan_raises_not_found_when_none(
    db_session: AsyncSession, test_user: User
):
    with pytest.raises(NotFoundError):
        await get_current_meal_plan(db_session, test_user)


# ==================== get_meal_plan_by_id ====================


@pytest.mark.asyncio
async def test_get_meal_plan_by_id_rejects_invalid_uuid(
    db_session: AsyncSession, test_user: User
):
    with pytest.raises(NotFoundError):
        await get_meal_plan_by_id(db_session, test_user, "not-a-uuid")


@pytest.mark.asyncio
async def test_get_meal_plan_by_id_raises_not_found_when_missing(
    db_session: AsyncSession, test_user: User
):
    missing_id = str(uuid.uuid4())
    with pytest.raises(NotFoundError):
        await get_meal_plan_by_id(db_session, test_user, missing_id)


@pytest.mark.asyncio
async def test_get_meal_plan_by_id_is_user_scoped(
    db_session: AsyncSession, test_user: User
):
    """A plan owned by user B must not be returned to user A (info-leak guard)."""
    other = User(
        id=str(uuid.uuid4()),
        firebase_uid=f"firebase-{uuid.uuid4().hex[:8]}",
        email=f"o-{uuid.uuid4().hex[:6]}@example.com",
        name="Other",
        is_active=True,
    )
    db_session.add(other)
    await db_session.commit()

    plan = MealPlan(
        id=str(uuid.uuid4()),
        user_id=other.id,
        week_start_date=date(2026, 5, 11),
        week_end_date=date(2026, 5, 17),
        is_active=True,
    )
    db_session.add(plan)
    await db_session.commit()

    # test_user asks for other user's plan -> NotFound (not Forbidden).
    with pytest.raises(NotFoundError):
        await get_meal_plan_by_id(db_session, test_user, plan.id)
