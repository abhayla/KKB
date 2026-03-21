"""Stats and gamification endpoints."""

import logging

from fastapi import APIRouter, Query
from sqlalchemy import select
from sqlalchemy.orm import selectinload

from app.api.deps import CurrentUser, DbSession
from app.models.household import Household, HouseholdMember
from app.models.user import User
from app.schemas.stats import CookingStreakResponse, MonthlyStatsResponse
from app.services.stats_service import get_cooking_streak, get_monthly_stats

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/stats", tags=["stats"])


async def _get_household_members(db, user_id: str) -> list[User] | None:
    """Look up all users in the same household as the given user.

    Returns None if the user has no household.
    """
    # Check if user owns a household
    result = await db.execute(
        select(Household)
        .options(selectinload(Household.members))
        .where(Household.owner_id == user_id, Household.is_active == True)
    )
    household = result.scalar_one_or_none()

    if not household:
        # Check if user is a member of a household
        result = await db.execute(
            select(HouseholdMember)
            .where(
                HouseholdMember.user_id == user_id,
                HouseholdMember.status == "ACTIVE",
            )
        )
        membership = result.scalar_one_or_none()
        if not membership:
            return None

        result = await db.execute(
            select(Household)
            .options(selectinload(Household.members))
            .where(Household.id == membership.household_id, Household.is_active == True)
        )
        household = result.scalar_one_or_none()
        if not household:
            return None

    # Gather all user IDs from household members
    member_user_ids = [
        m.user_id for m in household.members
        if m.user_id and m.status == "ACTIVE"
    ]
    if household.owner_id not in member_user_ids:
        member_user_ids.append(household.owner_id)

    # Fetch User objects
    result = await db.execute(
        select(User).where(User.id.in_(member_user_ids))
    )
    return list(result.scalars().all())


@router.get("/streak", response_model=CookingStreakResponse)
async def streak(
    db: DbSession,
    current_user: CurrentUser,
    scope: str = Query("personal", description="Data scope: personal or family"),
) -> CookingStreakResponse:
    """Get cooking streak statistics.

    Returns current streak, longest streak, and weekly progress.
    When scope=family, aggregates across household members.
    Falls back to personal data if user has no household.
    """
    if scope == "family":
        members = await _get_household_members(db, current_user.id)
        if members and len(members) > 1:
            # Aggregate streaks across all household members
            combined = CookingStreakResponse(
                current_streak=0,
                longest_streak=0,
                total_meals_cooked=0,
                last_cooking_date=None,
                streak_start_date=None,
                days_this_week=0,
            )
            for member in members:
                member_streak = await get_cooking_streak(db, member)
                combined.total_meals_cooked += member_streak.total_meals_cooked
                combined.days_this_week += member_streak.days_this_week
                if member_streak.current_streak > combined.current_streak:
                    combined.current_streak = member_streak.current_streak
                if member_streak.longest_streak > combined.longest_streak:
                    combined.longest_streak = member_streak.longest_streak
            return combined

    return await get_cooking_streak(db, current_user)


@router.get("/monthly", response_model=MonthlyStatsResponse)
async def monthly(
    db: DbSession,
    current_user: CurrentUser,
    month: str = Query(..., description="Month in yyyy-MM format"),
    scope: str = Query("personal", description="Data scope: personal or family"),
) -> MonthlyStatsResponse:
    """Get monthly cooking statistics.

    Includes meals cooked, recipes tried, cuisine breakdown, and achievements.
    When scope=family, returns stats for the current user (household aggregation
    for monthly stats is complex and deferred to a future release).
    """
    return await get_monthly_stats(db, current_user, month)


@router.post("/cooking-activity")
async def record_cooking_activity(
    db: DbSession,
    current_user: CurrentUser,
) -> dict:
    """Record that the user completed cooking a meal.

    Called from CookingMode when user finishes all steps.
    Updates streak counter and checks achievement milestones.
    """
    from app.services.stats_service import record_cooked_meal

    result = await record_cooked_meal(db, current_user)
    return {"status": "recorded", "new_streak": result.get("current_streak", 0)}
