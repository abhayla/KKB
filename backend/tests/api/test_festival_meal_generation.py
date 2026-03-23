"""Tests for festival meal generation integration.

Covers:
- POST /api/v1/festivals — create festival (DEBUG mode only)
- Festival CRUD and response structure validation
- Festival service integration (date queries, date range, inactive filtering)
- Festival-to-meal-plan data flow
"""

import uuid
from datetime import date, timedelta
from unittest.mock import MagicMock, patch

import pytest
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.festival import Festival
from app.services.festival_service import (
    get_festival_by_date,
    get_festivals_for_date_range,
)


# ==================== Helpers ====================


def _festival_payload(
    *,
    name: str = "Navratri",
    festival_date: date | None = None,
    is_fasting_day: bool = False,
    fasting_type: str | None = None,
    regions: list[str] | None = None,
    special_foods: list[str] | None = None,
    avoided_foods: list[str] | None = None,
    name_hindi: str | None = None,
    description: str | None = None,
) -> dict:
    """Build a valid FestivalCreate payload."""
    if festival_date is None:
        festival_date = date.today()
    payload = {
        "name": name,
        "date": festival_date.isoformat(),
        "is_fasting_day": is_fasting_day,
    }
    if fasting_type is not None:
        payload["fasting_type"] = fasting_type
    if regions is not None:
        payload["regions"] = regions
    if special_foods is not None:
        payload["special_foods"] = special_foods
    if avoided_foods is not None:
        payload["avoided_foods"] = avoided_foods
    if name_hindi is not None:
        payload["name_hindi"] = name_hindi
    if description is not None:
        payload["description"] = description
    return payload


def _mock_debug_settings(debug: bool = True):
    """Return a mock settings object with configurable debug flag."""
    mock = MagicMock()
    mock.debug = debug
    return mock


# ==================== Festival CRUD (POST endpoint) ====================


class TestCreateFestival:
    """Tests for POST /api/v1/festivals."""

    async def test_create_festival_success(self, client: AsyncClient):
        """POST /festivals with valid data returns 200 + FestivalResponse."""
        payload = _festival_payload(
            name="Dussehra",
            festival_date=date(2026, 10, 2),
            regions=["north", "west"],
            special_foods=["Jalebi", "Fafda"],
            name_hindi="दशहरा",
            description="Victory of good over evil",
        )

        from app.config import get_settings
        get_settings.cache_clear()
        with patch(
            "app.api.v1.endpoints.festivals.get_settings",
            return_value=_mock_debug_settings(debug=True),
        ):
            response = await client.post("/api/v1/festivals", json=payload)
        get_settings.cache_clear()

        assert response.status_code == 200
        data = response.json()
        assert data["name"] == "Dussehra"
        assert data["name_hindi"] == "दशहरा"
        assert data["description"] == "Victory of good over evil"
        assert data["date"] == "2026-10-02"
        assert data["regions"] == ["north", "west"]
        assert data["is_fasting_day"] is False
        assert "Jalebi" in data["special_foods"]
        assert "Fafda" in data["special_foods"]
        assert data["id"] is not None

    async def test_create_festival_unauthenticated(
        self, unauthenticated_client: AsyncClient
    ):
        """POST /festivals without auth returns 401."""
        payload = _festival_payload(name="TestFest")

        with patch(
            "app.api.v1.endpoints.festivals.get_settings",
            return_value=_mock_debug_settings(debug=True),
        ):
            response = await unauthenticated_client.post(
                "/api/v1/festivals", json=payload
            )

        assert response.status_code == 401

    async def test_create_festival_fasting_day(self, client: AsyncClient):
        """POST /festivals with fasting data creates fasting festival."""
        payload = _festival_payload(
            name="Ekadashi",
            festival_date=date(2026, 4, 15),
            is_fasting_day=True,
            fasting_type="partial",
            special_foods=["Sabudana Khichdi", "Fruit Chaat"],
            avoided_foods=["Rice", "Wheat", "Onion", "Garlic"],
        )

        from app.config import get_settings
        get_settings.cache_clear()
        with patch(
            "app.api.v1.endpoints.festivals.get_settings",
            return_value=_mock_debug_settings(debug=True),
        ):
            response = await client.post("/api/v1/festivals", json=payload)
        get_settings.cache_clear()

        assert response.status_code == 200
        data = response.json()
        assert data["is_fasting_day"] is True
        assert data["fasting_type"] == "partial"
        assert "Sabudana Khichdi" in data["special_foods"]
        assert "Rice" in data["avoided_foods"]
        assert "Garlic" in data["avoided_foods"]

    async def test_create_festival_appears_in_upcoming(
        self, client: AsyncClient
    ):
        """Festival created via POST shows up in GET /upcoming."""
        today = date.today()
        payload = _festival_payload(
            name="TestFestival",
            festival_date=today,
        )

        with patch(
            "app.api.v1.endpoints.festivals.get_settings",
            return_value=_mock_debug_settings(debug=True),
        ):
            create_resp = await client.post("/api/v1/festivals", json=payload)

        assert create_resp.status_code == 200

        upcoming_resp = await client.get("/api/v1/festivals/upcoming?days=1")
        assert upcoming_resp.status_code == 200
        data = upcoming_resp.json()
        names = [f["name"] for f in data]
        assert "TestFestival" in names

    async def test_create_festival_forbidden_non_debug(
        self, client: AsyncClient
    ):
        """POST /festivals returns 403 when DEBUG=false."""
        payload = _festival_payload(name="Blocked")

        with patch(
            "app.api.v1.endpoints.festivals.get_settings",
            return_value=_mock_debug_settings(debug=False),
        ):
            response = await client.post("/api/v1/festivals", json=payload)

        assert response.status_code == 403


# ==================== Festival in Meal Plan Integration ====================


class TestFestivalServiceIntegration:
    """Service-level tests for festival queries used during meal generation."""

    async def test_festival_included_in_date_range_query(
        self, db_session: AsyncSession
    ):
        """get_festivals_for_date_range returns seeded festival."""
        festival = Festival(
            id=str(uuid.uuid4()),
            name="Pongal",
            date=date(2026, 1, 14),
            year=2026,
            regions=["south"],
            is_fasting_day=False,
            special_foods=["Pongal Rice", "Sugarcane"],
            is_active=True,
        )
        db_session.add(festival)
        await db_session.commit()

        result = await get_festivals_for_date_range(
            db_session, date(2026, 1, 10), date(2026, 1, 20)
        )

        assert date(2026, 1, 14) in result
        assert result[date(2026, 1, 14)].name == "Pongal"

    async def test_fasting_festival_has_avoided_foods(
        self, db_session: AsyncSession
    ):
        """Fasting festival preserves avoided_foods for meal gen filtering."""
        festival = Festival(
            id=str(uuid.uuid4()),
            name="Nirjala Ekadashi",
            date=date(2026, 6, 4),
            year=2026,
            regions=["all"],
            is_fasting_day=True,
            fasting_type="complete",
            special_foods=["Water"],
            avoided_foods=["Rice", "Wheat", "Lentils", "Onion", "Garlic"],
            is_active=True,
        )
        db_session.add(festival)
        await db_session.commit()

        result = await get_festival_by_date(db_session, date(2026, 6, 4))

        assert result is not None
        assert result.is_fasting_day is True
        assert result.fasting_type == "complete"
        assert "Rice" in result.avoided_foods
        assert "Onion" in result.avoided_foods
        assert len(result.avoided_foods) == 5

    async def test_festival_by_date_returns_correct(
        self, db_session: AsyncSession
    ):
        """get_festival_by_date returns the festival for an exact date."""
        target = date(2026, 8, 15)
        festival = Festival(
            id=str(uuid.uuid4()),
            name="Independence Day",
            date=target,
            year=2026,
            regions=["all"],
            is_fasting_day=False,
            special_foods=["Tricolor Barfi"],
            is_active=True,
        )
        db_session.add(festival)
        await db_session.commit()

        result = await get_festival_by_date(db_session, target)

        assert result is not None
        assert result.name == "Independence Day"
        assert result.date == target

    async def test_no_festival_for_empty_date(
        self, db_session: AsyncSession
    ):
        """get_festival_by_date returns None for date with no festival."""
        result = await get_festival_by_date(db_session, date(2026, 7, 7))

        assert result is None

    async def test_inactive_festival_excluded(
        self, db_session: AsyncSession
    ):
        """Inactive festival not returned by service queries."""
        festival = Festival(
            id=str(uuid.uuid4()),
            name="Cancelled Festival",
            date=date(2026, 9, 1),
            year=2026,
            regions=["north"],
            is_fasting_day=False,
            is_active=False,
        )
        db_session.add(festival)
        await db_session.commit()

        by_date = await get_festival_by_date(db_session, date(2026, 9, 1))
        assert by_date is None

        by_range = await get_festivals_for_date_range(
            db_session, date(2026, 8, 25), date(2026, 9, 5)
        )
        assert date(2026, 9, 1) not in by_range


# ==================== Festival Response Structure ====================


class TestFestivalResponseStructure:
    """Validate response schemas from the POST and GET endpoints."""

    async def test_festival_response_has_all_fields(
        self, client: AsyncClient
    ):
        """POST /festivals response includes all FestivalResponse fields."""
        payload = _festival_payload(
            name="Ganesh Chaturthi",
            festival_date=date(2026, 8, 27),
            name_hindi="गणेश चतुर्थी",
            description="Lord Ganesha festival",
            regions=["west", "south"],
            is_fasting_day=True,
            fasting_type="partial",
            special_foods=["Modak", "Puran Poli"],
            avoided_foods=["Non-veg"],
        )

        from app.config import get_settings
        get_settings.cache_clear()
        with patch(
            "app.api.v1.endpoints.festivals.get_settings",
            return_value=_mock_debug_settings(debug=True),
        ):
            response = await client.post("/api/v1/festivals", json=payload)
        get_settings.cache_clear()

        assert response.status_code == 200
        data = response.json()

        required_fields = [
            "id",
            "name",
            "name_hindi",
            "description",
            "date",
            "regions",
            "is_fasting_day",
            "fasting_type",
            "special_foods",
            "avoided_foods",
        ]
        for field in required_fields:
            assert field in data, f"Missing field: {field}"

        assert data["name"] == "Ganesh Chaturthi"
        assert data["name_hindi"] == "गणेश चतुर्थी"
        assert data["description"] == "Lord Ganesha festival"
        assert data["date"] == "2026-08-27"
        assert data["regions"] == ["west", "south"]
        assert data["is_fasting_day"] is True
        assert data["fasting_type"] == "partial"
        assert "Modak" in data["special_foods"]
        assert "Non-veg" in data["avoided_foods"]

    async def test_upcoming_festivals_sorted_by_date(
        self, client: AsyncClient, db_session: AsyncSession
    ):
        """GET /upcoming returns festivals sorted chronologically."""
        today = date.today()
        festivals = [
            Festival(
                id=str(uuid.uuid4()),
                name="Third",
                date=today + timedelta(days=15),
                year=today.year,
                regions=["all"],
                is_fasting_day=False,
                is_active=True,
            ),
            Festival(
                id=str(uuid.uuid4()),
                name="First",
                date=today + timedelta(days=1),
                year=today.year,
                regions=["all"],
                is_fasting_day=False,
                is_active=True,
            ),
            Festival(
                id=str(uuid.uuid4()),
                name="Second",
                date=today + timedelta(days=7),
                year=today.year,
                regions=["all"],
                is_fasting_day=False,
                is_active=True,
            ),
        ]
        for f in festivals:
            db_session.add(f)
        await db_session.commit()

        response = await client.get("/api/v1/festivals/upcoming?days=30")

        assert response.status_code == 200
        data = response.json()
        names = [f["name"] for f in data]
        assert names.index("First") < names.index("Second")
        assert names.index("Second") < names.index("Third")
