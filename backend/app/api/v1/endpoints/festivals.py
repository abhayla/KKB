"""Festival endpoints."""

import logging
import uuid

from fastapi import APIRouter, Query

from app.api.deps import CurrentUser, DbSession
from app.config import get_settings
from app.core.exceptions import ForbiddenError
from app.models.festival import Festival
from app.schemas.festival import (
    FestivalCreate,
    FestivalResponse,
    UpcomingFestivalResponse,
)
from app.services.festival_service import get_upcoming_festivals

logger = logging.getLogger(__name__)

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


@router.post("", response_model=FestivalResponse)
async def create_festival(
    body: FestivalCreate,
    current_user: CurrentUser,
    db: DbSession,
) -> FestivalResponse:
    """Create a festival (DEBUG mode only).

    Used by E2E tests to seed festival data for specific dates.
    """
    settings = get_settings()
    if not settings.debug:
        raise ForbiddenError("Festival creation is only available in DEBUG mode")

    festival = Festival(
        id=str(uuid.uuid4()),
        name=body.name,
        name_hindi=body.name_hindi,
        description=body.description,
        date=body.date,
        year=body.date.year if body.date else 2026,
        regions=body.regions or ["all"],
        is_fasting_day=body.is_fasting_day,
        fasting_type=body.fasting_type,
        special_foods=body.special_foods,
        avoided_foods=body.avoided_foods,
        is_active=True,
    )
    db.add(festival)
    await db.commit()
    await db.refresh(festival)

    logger.info(f"Created test festival: {festival.name} on {festival.date}")
    return FestivalResponse.model_validate(festival)
