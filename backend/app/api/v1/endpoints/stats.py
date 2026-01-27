"""Stats and gamification endpoints."""

from fastapi import APIRouter, Query

from app.api.deps import CurrentUser, DbSession
from app.schemas.stats import CookingStreakResponse, MonthlyStatsResponse
from app.services.stats_service import get_cooking_streak, get_monthly_stats

router = APIRouter(prefix="/stats", tags=["stats"])


@router.get("/streak", response_model=CookingStreakResponse)
async def streak(
    db: DbSession,
    current_user: CurrentUser,
) -> CookingStreakResponse:
    """Get cooking streak statistics.

    Returns current streak, longest streak, and weekly progress.
    """
    return await get_cooking_streak(db, current_user)


@router.get("/monthly", response_model=MonthlyStatsResponse)
async def monthly(
    db: DbSession,
    current_user: CurrentUser,
    month: str = Query(..., description="Month in yyyy-MM format"),
) -> MonthlyStatsResponse:
    """Get monthly cooking statistics.

    Includes meals cooked, recipes tried, cuisine breakdown, and achievements.
    """
    return await get_monthly_stats(db, current_user, month)
