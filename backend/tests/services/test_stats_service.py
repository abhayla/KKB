"""Tests for stats_service — get_cooking_streak and get_monthly_stats.

Requirement: #35 - Service-level unit tests for stats read paths.

Scope: this file complements the existing test_stats_log_cooking.py and
test_achievement_earning.py. It focuses on the *read* paths
(get_cooking_streak, get_monthly_stats) which had no dedicated coverage.
"""

import uuid
from datetime import date, timedelta

import pytest
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.stats import CookingStreak
from app.models.user import User
from app.services.stats_service import get_cooking_streak, get_monthly_stats


# ==================== get_cooking_streak ====================


@pytest.mark.asyncio
async def test_no_streak_returns_zeroed_response(
    db_session: AsyncSession, test_user: User
):
    """A user with no CookingStreak row should get a zeroed response, not an exception."""
    result = await get_cooking_streak(db_session, test_user)

    assert result.current_streak == 0
    assert result.longest_streak == 0
    assert result.total_meals_cooked == 0
    assert result.last_cooking_date is None
    assert result.streak_start_date is None
    assert result.days_this_week == 0


@pytest.mark.asyncio
async def test_populated_streak_exposes_start_date(
    db_session: AsyncSession, test_user: User
):
    """current_streak > 0 implies streak_start_date = last - (current-1) days."""
    today = date.today()
    streak = CookingStreak(
        id=str(uuid.uuid4()),
        user_id=test_user.id,
        current_streak=3,
        longest_streak=5,
        total_meals_cooked=10,
        last_cooking_date=today,
    )
    db_session.add(streak)
    await db_session.commit()

    result = await get_cooking_streak(db_session, test_user)

    assert result.current_streak == 3
    assert result.longest_streak == 5
    assert result.total_meals_cooked == 10
    assert result.last_cooking_date == today.isoformat()
    assert result.streak_start_date == (today - timedelta(days=2)).isoformat()


@pytest.mark.asyncio
async def test_streak_with_no_last_cooking_date_has_null_start(
    db_session: AsyncSession, test_user: User
):
    """If last_cooking_date is None, streak_start_date must also be None
    regardless of current_streak value."""
    streak = CookingStreak(
        id=str(uuid.uuid4()),
        user_id=test_user.id,
        current_streak=2,
        longest_streak=2,
        total_meals_cooked=2,
        last_cooking_date=None,  # unusual but must not crash
    )
    db_session.add(streak)
    await db_session.commit()

    result = await get_cooking_streak(db_session, test_user)

    assert result.last_cooking_date is None
    assert result.streak_start_date is None


# ==================== get_monthly_stats ====================


@pytest.mark.asyncio
async def test_malformed_month_falls_back_to_current(
    db_session: AsyncSession, test_user: User
):
    """Passing a non-parseable month must not raise — fall back to current month."""
    result = await get_monthly_stats(db_session, test_user, "garbage")

    today = date.today()
    expected = f"{today.year}-{today.month:02d}"
    assert result.month == expected
    assert result.total_meals_cooked == 0
    assert result.cuisine_breakdown == []


@pytest.mark.asyncio
async def test_malformed_month_with_partial_format_falls_back(
    db_session: AsyncSession, test_user: User
):
    """Only a year supplied (no month separator) should also gracefully fall back."""
    result = await get_monthly_stats(db_session, test_user, "2020")

    today = date.today()
    expected = f"{today.year}-{today.month:02d}"
    assert result.month == expected


@pytest.mark.asyncio
async def test_empty_month_returns_zero_stats(
    db_session: AsyncSession, test_user: User
):
    """A valid month with no seeded data returns zeroed aggregates."""
    result = await get_monthly_stats(db_session, test_user, "2020-01")

    assert result.month == "2020-01"
    assert result.total_meals_cooked == 0
    assert result.unique_recipes_tried == 0
    assert result.total_cooking_days == 0
    assert result.favorite_cuisine is None
    assert result.cuisine_breakdown == []
    assert result.daily_records == []
    assert result.achievements_unlocked == []


@pytest.mark.asyncio
async def test_december_month_edge_case_handles_year_rollover(
    db_session: AsyncSession, test_user: User
):
    """Month=12 must not blow up when computing month_end (year+1 rollover)."""
    result = await get_monthly_stats(db_session, test_user, "2024-12")

    assert result.month == "2024-12"
    # No data, all zeroes — but it must have computed month bounds without exception.
    assert result.total_meals_cooked == 0
