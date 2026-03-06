"""Tests for festival_service.

Requirement: #35 - Service-level unit tests for festival lookup functions.
Verifies get_festival_by_date() and get_festivals_for_date_range()
including inactive festival filtering.
"""

import uuid
from datetime import date

import pytest

from app.models.festival import Festival
from app.services.festival_service import (
    get_festival_by_date,
    get_festivals_for_date_range,
)


@pytest.fixture
async def diwali(db_session):
    """Seed an active Diwali festival."""
    festival = Festival(
        id=str(uuid.uuid4()),
        name="Diwali",
        date=date(2026, 10, 20),
        year=2026,
        regions=["all"],
        is_fasting_day=False,
        special_foods=["Laddu", "Kaju Katli"],
        avoided_foods=None,
        is_active=True,
    )
    db_session.add(festival)
    await db_session.commit()
    return festival


@pytest.fixture
async def ekadashi(db_session):
    """Seed an active Ekadashi fasting festival."""
    festival = Festival(
        id=str(uuid.uuid4()),
        name="Ekadashi",
        date=date(2026, 10, 22),
        year=2026,
        regions=["north", "west"],
        is_fasting_day=True,
        special_foods=["Sabudana Khichdi"],
        avoided_foods=["grains", "onion"],
        is_active=True,
    )
    db_session.add(festival)
    await db_session.commit()
    return festival


@pytest.fixture
async def inactive_festival(db_session):
    """Seed an inactive festival."""
    festival = Festival(
        id=str(uuid.uuid4()),
        name="OldFestival",
        date=date(2026, 10, 21),
        year=2026,
        regions=["south"],
        is_fasting_day=False,
        special_foods=None,
        avoided_foods=None,
        is_active=False,
    )
    db_session.add(festival)
    await db_session.commit()
    return festival


class TestGetFestivalByDate:
    """Tests for get_festival_by_date()."""

    async def test_get_festival_by_date_found(self, db_session, diwali):
        """Returns Festival for matching date."""
        result = await get_festival_by_date(db_session, date(2026, 10, 20))

        assert result is not None
        assert result.name == "Diwali"
        assert result.is_fasting_day is False
        assert result.special_foods == ["Laddu", "Kaju Katli"]

    async def test_get_festival_by_date_not_found(self, db_session):
        """Returns None for date with no festival."""
        result = await get_festival_by_date(db_session, date(2026, 6, 15))

        assert result is None

    async def test_get_festival_by_date_ignores_inactive(
        self, db_session, inactive_festival
    ):
        """is_active=False festival is not returned."""
        result = await get_festival_by_date(db_session, date(2026, 10, 21))

        assert result is None


class TestGetFestivalsForDateRange:
    """Tests for get_festivals_for_date_range()."""

    async def test_get_festivals_for_range_returns_dict(
        self, db_session, diwali, ekadashi
    ):
        """Returns {date: Festival} mapping for active festivals in range."""
        result = await get_festivals_for_date_range(
            db_session, date(2026, 10, 19), date(2026, 10, 23)
        )

        assert len(result) == 2
        assert date(2026, 10, 20) in result
        assert result[date(2026, 10, 20)].name == "Diwali"
        assert date(2026, 10, 22) in result
        assert result[date(2026, 10, 22)].name == "Ekadashi"

    async def test_get_festivals_for_range_empty(self, db_session):
        """Empty dict for range with no festivals."""
        result = await get_festivals_for_date_range(
            db_session, date(2026, 3, 1), date(2026, 3, 7)
        )

        assert result == {}

    async def test_get_festivals_for_range_filters_inactive(
        self, db_session, diwali, inactive_festival
    ):
        """Inactive festivals excluded from range results."""
        result = await get_festivals_for_date_range(
            db_session, date(2026, 10, 19), date(2026, 10, 23)
        )

        assert len(result) == 1
        assert date(2026, 10, 20) in result
        # inactive_festival on 10/21 should NOT be present
        assert date(2026, 10, 21) not in result
