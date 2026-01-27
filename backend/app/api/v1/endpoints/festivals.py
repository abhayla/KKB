"""Festival endpoints."""

from fastapi import APIRouter, Query

from app.api.deps import DbSession
from app.schemas.festival import UpcomingFestivalResponse
from app.services.festival_service import get_upcoming_festivals

router = APIRouter(prefix="/festivals", tags=["festivals"])


@router.get("/upcoming", response_model=list[UpcomingFestivalResponse])
async def upcoming(
    db: DbSession,
    days: int = Query(default=30, ge=1, le=365, description="Days to look ahead"),
) -> list[UpcomingFestivalResponse]:
    """Get upcoming Indian festivals.

    Returns festivals in the next N days, including fasting days
    and suggested traditional foods.
    """
    return await get_upcoming_festivals(db, days)
