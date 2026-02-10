"""
Tests for Festival API endpoints.

Covers:
- GET /api/v1/festivals/upcoming — get upcoming festivals
"""

import pytest
from datetime import date, timedelta
from uuid import uuid4

import pytest_asyncio
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.festival import Festival


# ==================== Fixtures ====================


@pytest_asyncio.fixture
async def festival_data(db_session: AsyncSession) -> list[Festival]:
    """Create festival records at various dates relative to today."""
    today = date.today()
    festivals = []

    # Festival today (0 days away)
    f1 = Festival(
        id=str(uuid4()),
        name="Diwali",
        name_hindi="दीवाली",
        description="Festival of lights",
        date=today,
        year=today.year,
        regions=["all"],
        is_fasting_day=False,
        special_foods=["Ladoo", "Kaju Katli", "Gulab Jamun"],
        is_active=True,
    )
    festivals.append(f1)

    # Festival in 5 days
    f2 = Festival(
        id=str(uuid4()),
        name="Bhai Dooj",
        date=today + timedelta(days=5),
        year=today.year,
        regions=["north", "east"],
        is_fasting_day=False,
        special_foods=["Coconut Barfi"],
        is_active=True,
    )
    festivals.append(f2)

    # Festival in 40 days (outside default 30-day window)
    f3 = Festival(
        id=str(uuid4()),
        name="Makar Sankranti",
        date=today + timedelta(days=40),
        year=today.year,
        regions=["all"],
        is_fasting_day=False,
        special_foods=["Til Ladoo", "Pongal"],
        is_active=True,
    )
    festivals.append(f3)

    # Inactive festival (should be filtered out)
    f4 = Festival(
        id=str(uuid4()),
        name="Deprecated Festival",
        date=today + timedelta(days=3),
        year=today.year,
        regions=["north"],
        is_fasting_day=False,
        is_active=False,
    )
    festivals.append(f4)

    # Festival in 100 days
    f5 = Festival(
        id=str(uuid4()),
        name="Holi",
        date=today + timedelta(days=100),
        year=today.year,
        regions=["all"],
        is_fasting_day=False,
        special_foods=["Gujiya", "Thandai"],
        is_active=True,
    )
    festivals.append(f5)

    # Festival with fasting
    f6 = Festival(
        id=str(uuid4()),
        name="Ekadashi",
        date=today + timedelta(days=10),
        year=today.year,
        regions=["all"],
        is_fasting_day=True,
        fasting_type="partial",
        special_foods=["Sabudana Khichdi", "Fruit Salad"],
        avoided_foods=["Rice", "Wheat"],
        is_active=True,
    )
    festivals.append(f6)

    for f in festivals:
        db_session.add(f)
    await db_session.commit()

    return festivals


# ==================== Tests ====================


@pytest.mark.asyncio
async def test_upcoming_festivals_default_30_days(
    client: AsyncClient, festival_data: list[Festival]
):
    """GET /upcoming returns festivals in next 30 days by default."""
    response = await client.get("/api/v1/festivals/upcoming")

    assert response.status_code == 200
    data = response.json()
    # Should include: Diwali (today), Bhai Dooj (+5d), Ekadashi (+10d)
    # Should exclude: Deprecated (inactive), Makar Sankranti (+40d), Holi (+100d)
    assert len(data) == 3
    names = [f["name"] for f in data]
    assert "Diwali" in names
    assert "Bhai Dooj" in names
    assert "Ekadashi" in names
    assert "Deprecated Festival" not in names
    assert "Makar Sankranti" not in names


@pytest.mark.asyncio
async def test_upcoming_festivals_custom_days(
    client: AsyncClient, festival_data: list[Festival]
):
    """GET /upcoming?days=7 respects custom day range."""
    response = await client.get("/api/v1/festivals/upcoming?days=7")

    assert response.status_code == 200
    data = response.json()
    # Only: Diwali (today), Bhai Dooj (+5d)
    assert len(data) == 2
    names = [f["name"] for f in data]
    assert "Diwali" in names
    assert "Bhai Dooj" in names


@pytest.mark.asyncio
async def test_upcoming_festivals_sorted_by_date(
    client: AsyncClient, festival_data: list[Festival]
):
    """GET /upcoming returns festivals in chronological order."""
    response = await client.get("/api/v1/festivals/upcoming")

    assert response.status_code == 200
    data = response.json()
    dates = [f["date"] for f in data]
    assert dates == sorted(dates)


@pytest.mark.asyncio
async def test_upcoming_festivals_days_away(
    client: AsyncClient, festival_data: list[Festival]
):
    """GET /upcoming calculates days_away correctly."""
    response = await client.get("/api/v1/festivals/upcoming")

    assert response.status_code == 200
    data = response.json()

    diwali = next(f for f in data if f["name"] == "Diwali")
    assert diwali["days_away"] == 0

    bhai_dooj = next(f for f in data if f["name"] == "Bhai Dooj")
    assert bhai_dooj["days_away"] == 5

    ekadashi = next(f for f in data if f["name"] == "Ekadashi")
    assert ekadashi["days_away"] == 10


@pytest.mark.asyncio
async def test_upcoming_festivals_excludes_inactive(
    client: AsyncClient, festival_data: list[Festival]
):
    """GET /upcoming filters out is_active=False festivals."""
    response = await client.get("/api/v1/festivals/upcoming?days=365")

    assert response.status_code == 200
    data = response.json()
    names = [f["name"] for f in data]
    assert "Deprecated Festival" not in names


@pytest.mark.asyncio
async def test_upcoming_festivals_empty(client: AsyncClient):
    """GET /upcoming returns empty array when no festivals upcoming."""
    response = await client.get("/api/v1/festivals/upcoming?days=1")

    assert response.status_code == 200
    data = response.json()
    assert data == []


@pytest.mark.asyncio
async def test_upcoming_festivals_no_auth_required(
    unauthenticated_client: AsyncClient, festival_data: list[Festival]
):
    """GET /upcoming works without authentication."""
    response = await unauthenticated_client.get("/api/v1/festivals/upcoming")

    assert response.status_code == 200
    data = response.json()
    assert len(data) >= 1


@pytest.mark.asyncio
async def test_upcoming_festivals_max_days(
    client: AsyncClient, festival_data: list[Festival]
):
    """GET /upcoming?days=365 returns full year range."""
    response = await client.get("/api/v1/festivals/upcoming?days=365")

    assert response.status_code == 200
    data = response.json()
    # Should include all active: Diwali, Bhai Dooj, Ekadashi, Makar Sankranti, Holi
    assert len(data) == 5
    names = [f["name"] for f in data]
    assert "Holi" in names
    assert "Makar Sankranti" in names


@pytest.mark.asyncio
async def test_upcoming_festivals_special_foods(
    client: AsyncClient, festival_data: list[Festival]
):
    """GET /upcoming returns special_foods for festivals that have them."""
    response = await client.get("/api/v1/festivals/upcoming")

    assert response.status_code == 200
    data = response.json()

    diwali = next(f for f in data if f["name"] == "Diwali")
    assert "Ladoo" in diwali["special_foods"]
    assert "Kaju Katli" in diwali["special_foods"]

    ekadashi = next(f for f in data if f["name"] == "Ekadashi")
    assert ekadashi["is_fasting_day"] is True
    assert "Sabudana Khichdi" in ekadashi["special_foods"]
