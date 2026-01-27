"""Festival service."""

from datetime import date, timedelta

from sqlalchemy import and_, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.festival import Festival
from app.schemas.festival import FestivalResponse, UpcomingFestivalResponse


async def get_upcoming_festivals(
    db: AsyncSession,
    days: int = 30,
) -> list[UpcomingFestivalResponse]:
    """Get festivals upcoming in the next N days.

    Args:
        db: Database session
        days: Number of days to look ahead

    Returns:
        List of upcoming festivals
    """
    today = date.today()
    end_date = today + timedelta(days=days)

    result = await db.execute(
        select(Festival)
        .where(
            Festival.is_active == True,
            Festival.date >= today,
            Festival.date <= end_date,
        )
        .order_by(Festival.date)
    )
    festivals = result.scalars().all()

    return [
        UpcomingFestivalResponse(
            id=str(festival.id),
            name=festival.name,
            date=festival.date.isoformat() if festival.date else "",
            days_away=(festival.date - today).days if festival.date else 0,
            is_fasting_day=festival.is_fasting_day,
            special_foods=festival.special_foods,
        )
        for festival in festivals
    ]


async def get_festival_by_date(
    db: AsyncSession,
    target_date: date,
) -> Festival | None:
    """Get festival for a specific date.

    Args:
        db: Database session
        target_date: Date to check

    Returns:
        Festival if found, None otherwise
    """
    result = await db.execute(
        select(Festival).where(
            Festival.is_active == True,
            Festival.date == target_date,
        )
    )
    return result.scalar_one_or_none()


async def get_festivals_for_date_range(
    db: AsyncSession,
    start_date: date,
    end_date: date,
) -> dict[date, Festival]:
    """Get festivals for a date range.

    Args:
        db: Database session
        start_date: Start date
        end_date: End date

    Returns:
        Dictionary mapping dates to festivals
    """
    result = await db.execute(
        select(Festival).where(
            Festival.is_active == True,
            Festival.date >= start_date,
            Festival.date <= end_date,
        )
    )
    festivals = result.scalars().all()

    return {festival.date: festival for festival in festivals if festival.date}
