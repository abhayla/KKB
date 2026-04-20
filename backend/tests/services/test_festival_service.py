"""Tests for festival_service.

Requirement: #35 - Service-level unit tests for festival lookup functions.
Verifies get_festival_by_date() and get_festivals_for_date_range()
including inactive festival filtering.
"""

import uuid
from datetime import date

import pytest

from app.models.festival import Festival
from datetime import timedelta

from app.services.festival_service import (
    get_festival_by_date,
    get_festivals_for_date_range,
    get_upcoming_festivals,
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


# ==================== get_upcoming_festivals ====================


def _make_festival(
    name: str,
    d: date,
    *,
    is_active: bool = True,
    is_fasting_day: bool = False,
    special_foods: list[str] | None = None,
) -> Festival:
    return Festival(
        id=str(uuid.uuid4()),
        name=name,
        date=d,
        year=d.year,
        regions=["all"],
        is_fasting_day=is_fasting_day,
        special_foods=special_foods or [],
        avoided_foods=None,
        is_active=is_active,
    )


class TestGetUpcomingFestivals:
    """Tests for get_upcoming_festivals()."""

    @pytest.mark.asyncio
    async def test_empty_db_returns_empty_list(self, db_session):
        """No festivals in DB -> empty list (not an exception)."""
        result = await get_upcoming_festivals(db_session)
        assert result == []

    @pytest.mark.asyncio
    async def test_returns_festivals_within_window_ordered_by_date(self, db_session):
        """Festivals within the next N days are returned in date order."""
        today = date.today()
        # Insert in non-sorted order to prove the service sorts.
        db_session.add_all([
            _make_festival("Later", today + timedelta(days=15)),
            _make_festival("Sooner", today + timedelta(days=3)),
            _make_festival("Mid", today + timedelta(days=8)),
        ])
        await db_session.commit()

        result = await get_upcoming_festivals(db_session, days=30)

        assert [f.name for f in result] == ["Sooner", "Mid", "Later"]

    @pytest.mark.asyncio
    async def test_excludes_past_festivals(self, db_session):
        """A festival with date < today must NOT be returned."""
        today = date.today()
        db_session.add_all([
            _make_festival("Yesterday", today - timedelta(days=1)),
            _make_festival("Future", today + timedelta(days=5)),
        ])
        await db_session.commit()

        result = await get_upcoming_festivals(db_session, days=30)

        names = [f.name for f in result]
        assert "Yesterday" not in names
        assert "Future" in names

    @pytest.mark.asyncio
    async def test_excludes_festivals_beyond_window(self, db_session):
        """Festivals beyond today+days must NOT be returned."""
        today = date.today()
        db_session.add_all([
            _make_festival("InWindow", today + timedelta(days=5)),
            _make_festival("OutOfWindow", today + timedelta(days=31)),
        ])
        await db_session.commit()

        result = await get_upcoming_festivals(db_session, days=30)

        names = [f.name for f in result]
        assert "InWindow" in names
        assert "OutOfWindow" not in names

    @pytest.mark.asyncio
    async def test_excludes_inactive_festivals(self, db_session):
        """Inactive festivals must NOT leak into upcoming results even when in window."""
        today = date.today()
        db_session.add_all([
            _make_festival("Active", today + timedelta(days=4)),
            _make_festival("Inactive", today + timedelta(days=5), is_active=False),
        ])
        await db_session.commit()

        result = await get_upcoming_festivals(db_session, days=30)

        names = [f.name for f in result]
        assert "Active" in names
        assert "Inactive" not in names

    @pytest.mark.asyncio
    async def test_days_away_computed_correctly(self, db_session):
        """Each UpcomingFestivalResponse.days_away equals (festival.date - today).days."""
        today = date.today()
        db_session.add_all([
            _make_festival("In7Days", today + timedelta(days=7)),
            _make_festival("In20Days", today + timedelta(days=20)),
        ])
        await db_session.commit()

        result = await get_upcoming_festivals(db_session, days=30)

        by_name = {f.name: f for f in result}
        assert by_name["In7Days"].days_away == 7
        assert by_name["In20Days"].days_away == 20

    @pytest.mark.asyncio
    async def test_default_window_is_30_days(self, db_session):
        """Calling without `days` should behave as days=30 (festival at +31 excluded)."""
        today = date.today()
        db_session.add_all([
            _make_festival("Day30", today + timedelta(days=30)),  # inclusive boundary
            _make_festival("Day31", today + timedelta(days=31)),
        ])
        await db_session.commit()

        result = await get_upcoming_festivals(db_session)  # no `days` arg

        names = [f.name for f in result]
        assert "Day30" in names
        assert "Day31" not in names

    @pytest.mark.asyncio
    async def test_today_festival_included_with_zero_days_away(self, db_session):
        """A festival dated today is upcoming (days_away == 0)."""
        today = date.today()
        db_session.add(_make_festival("Today", today))
        await db_session.commit()

        result = await get_upcoming_festivals(db_session)

        assert len(result) == 1
        assert result[0].name == "Today"
        assert result[0].days_away == 0
