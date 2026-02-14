"""Stats and gamification service."""

import logging
import uuid
from collections import defaultdict
from datetime import date, timedelta

from sqlalchemy import and_, func, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.models.meal_plan import MealPlan, MealPlanItem
from app.models.recipe import Recipe
from app.models.stats import Achievement, CookingDay, CookingStreak, UserAchievement
from app.models.user import User
from app.schemas.stats import (
    AchievementResponse,
    CookingStreakResponse,
    CuisineBreakdown,
    DailyCookingRecord,
    MonthlyStatsResponse,
)

logger = logging.getLogger(__name__)


async def get_cooking_streak(
    db: AsyncSession,
    user: User,
) -> CookingStreakResponse:
    """Get user's cooking streak stats.

    Args:
        db: Database session
        user: Current user

    Returns:
        CookingStreakResponse
    """
    result = await db.execute(
        select(CookingStreak)
        .options(selectinload(CookingStreak.cooking_days))
        .where(CookingStreak.user_id == user.id)
    )
    streak = result.scalar_one_or_none()

    if not streak:
        return CookingStreakResponse(
            current_streak=0,
            longest_streak=0,
            total_meals_cooked=0,
            last_cooking_date=None,
            streak_start_date=None,
            days_this_week=0,
        )

    # Calculate days this week
    today = date.today()
    week_start = today - timedelta(days=today.weekday())
    days_this_week = sum(
        1
        for day in streak.cooking_days
        if day.date >= week_start and day.meals_cooked > 0
    )

    # Calculate streak start date
    streak_start = None
    if streak.current_streak > 0 and streak.last_cooking_date:
        streak_start = streak.last_cooking_date - timedelta(days=streak.current_streak - 1)

    return CookingStreakResponse(
        current_streak=streak.current_streak,
        longest_streak=streak.longest_streak,
        total_meals_cooked=streak.total_meals_cooked,
        last_cooking_date=streak.last_cooking_date.isoformat() if streak.last_cooking_date else None,
        streak_start_date=streak_start.isoformat() if streak_start else None,
        days_this_week=days_this_week,
    )


async def get_monthly_stats(
    db: AsyncSession,
    user: User,
    month: str,  # yyyy-MM
) -> MonthlyStatsResponse:
    """Get user's monthly cooking stats.

    Args:
        db: Database session
        user: Current user
        month: Month in yyyy-MM format

    Returns:
        MonthlyStatsResponse
    """
    # Parse month
    try:
        year, month_num = map(int, month.split("-"))
        month_start = date(year, month_num, 1)
        if month_num == 12:
            month_end = date(year + 1, 1, 1) - timedelta(days=1)
        else:
            month_end = date(year, month_num + 1, 1) - timedelta(days=1)
    except (ValueError, IndexError):
        # Default to current month
        today = date.today()
        month_start = date(today.year, today.month, 1)
        if today.month == 12:
            month_end = date(today.year + 1, 1, 1) - timedelta(days=1)
        else:
            month_end = date(today.year, today.month + 1, 1) - timedelta(days=1)

    # Get cooking streak data for this month
    result = await db.execute(
        select(CookingStreak)
        .options(selectinload(CookingStreak.cooking_days))
        .where(CookingStreak.user_id == user.id)
    )
    streak = result.scalar_one_or_none()

    daily_records = []
    total_meals = 0
    cooking_days_count = 0

    if streak:
        for day in streak.cooking_days:
            if month_start <= day.date <= month_end:
                daily_records.append(
                    DailyCookingRecord(
                        date=day.date.isoformat(),
                        meals_cooked=day.meals_cooked,
                        breakfast_cooked=day.breakfast_cooked,
                        lunch_cooked=day.lunch_cooked,
                        dinner_cooked=day.dinner_cooked,
                    )
                )
                total_meals += day.meals_cooked
                if day.meals_cooked > 0:
                    cooking_days_count += 1

    # Get meal plans for cuisine breakdown
    result = await db.execute(
        select(MealPlan)
        .options(
            selectinload(MealPlan.items).selectinload(MealPlanItem.recipe)
        )
        .where(
            MealPlan.user_id == user.id,
            MealPlan.week_start_date >= month_start,
            MealPlan.week_start_date <= month_end,
        )
    )
    meal_plans = result.scalars().all()

    # Count cuisines and unique recipes
    cuisine_counts: dict[str, int] = defaultdict(int)
    unique_recipes: set[uuid.UUID] = set()

    for plan in meal_plans:
        for item in plan.items:
            if item.recipe:
                unique_recipes.add(item.recipe_id)
                cuisine_counts[item.recipe.cuisine_type] += 1

    total_cuisine = sum(cuisine_counts.values())
    cuisine_breakdown = [
        CuisineBreakdown(
            cuisine_type=cuisine,
            count=count,
            percentage=round(count / total_cuisine * 100, 1) if total_cuisine else 0,
        )
        for cuisine, count in sorted(cuisine_counts.items(), key=lambda x: -x[1])
    ]

    favorite_cuisine = cuisine_breakdown[0].cuisine_type if cuisine_breakdown else None

    # Get achievements unlocked this month
    result = await db.execute(
        select(UserAchievement)
        .options(selectinload(UserAchievement.achievement))
        .where(
            UserAchievement.user_id == user.id,
            UserAchievement.unlocked_at >= month_start,
            UserAchievement.unlocked_at <= month_end,
        )
    )
    user_achievements = result.scalars().all()

    achievements = [
        AchievementResponse(
            id=str(ua.achievement.id),
            name=ua.achievement.name,
            description=ua.achievement.description,
            icon=ua.achievement.icon,
            category=ua.achievement.category,
            is_unlocked=True,
            unlocked_at=ua.unlocked_at.isoformat(),
            progress=1.0,
        )
        for ua in user_achievements
    ]

    return MonthlyStatsResponse(
        month=f"{month_start.year}-{month_start.month:02d}",
        total_meals_cooked=total_meals,
        unique_recipes_tried=len(unique_recipes),
        total_cooking_days=cooking_days_count,
        favorite_cuisine=favorite_cuisine,
        cuisine_breakdown=cuisine_breakdown,
        daily_records=daily_records,
        achievements_unlocked=achievements,
    )


async def log_cooking(
    db: AsyncSession,
    user: User,
    meal_type: str,
    cooking_date: date | None = None,
) -> CookingStreakResponse:
    """Log a cooking session.

    Args:
        db: Database session
        user: Current user
        meal_type: Type of meal (breakfast, lunch, dinner)
        cooking_date: Date of cooking (defaults to today)

    Returns:
        Updated CookingStreakResponse
    """
    target_date = cooking_date or date.today()

    # Get or create streak
    result = await db.execute(
        select(CookingStreak)
        .options(selectinload(CookingStreak.cooking_days))
        .where(CookingStreak.user_id == user.id)
    )
    streak = result.scalar_one_or_none()

    if not streak:
        streak = CookingStreak(user_id=user.id)
        db.add(streak)
        await db.flush()

    # Find or create cooking day
    cooking_day = None
    for day in streak.cooking_days:
        if day.date == target_date:
            cooking_day = day
            break

    if not cooking_day:
        cooking_day = CookingDay(
            cooking_streak_id=streak.id,
            date=target_date,
        )
        db.add(cooking_day)
        streak.cooking_days.append(cooking_day)

    # Update cooking day
    if meal_type == "breakfast" and not cooking_day.breakfast_cooked:
        cooking_day.breakfast_cooked = True
        cooking_day.meals_cooked += 1
        streak.total_meals_cooked += 1
    elif meal_type == "lunch" and not cooking_day.lunch_cooked:
        cooking_day.lunch_cooked = True
        cooking_day.meals_cooked += 1
        streak.total_meals_cooked += 1
    elif meal_type == "dinner" and not cooking_day.dinner_cooked:
        cooking_day.dinner_cooked = True
        cooking_day.meals_cooked += 1
        streak.total_meals_cooked += 1

    # Update streak
    if streak.last_cooking_date:
        days_diff = (target_date - streak.last_cooking_date).days
        if days_diff == 1:
            # Consecutive day
            streak.current_streak += 1
        elif days_diff > 1:
            # Streak broken
            streak.current_streak = 1
        # days_diff == 0 means same day, no streak change
    else:
        streak.current_streak = 1

    streak.last_cooking_date = target_date
    streak.longest_streak = max(streak.longest_streak, streak.current_streak)

    await db.commit()

    # Check and grant achievements after cooking
    await check_and_grant_achievements(db, user, streak)

    return await get_cooking_streak(db, user)


async def check_and_grant_achievements(
    db: AsyncSession,
    user: User,
    streak: CookingStreak,
) -> list[str]:
    """Check and grant achievements based on current stats.

    Checks total meals cooked, streak milestones, and grants
    any achievements whose requirements are met.

    Args:
        db: Database session
        user: Current user
        streak: User's cooking streak data

    Returns:
        List of newly unlocked achievement names
    """
    # Get all active achievements
    result = await db.execute(
        select(Achievement).where(Achievement.is_active == True)
    )
    all_achievements = result.scalars().all()

    # Get already unlocked achievement IDs
    result = await db.execute(
        select(UserAchievement.achievement_id).where(
            UserAchievement.user_id == user.id
        )
    )
    unlocked_ids = set(row[0] for row in result.all())

    newly_unlocked = []

    for achievement in all_achievements:
        if achievement.id in unlocked_ids:
            continue

        earned = False
        req_type = achievement.requirement_type
        req_value = achievement.requirement_value

        if req_type == "meals_cooked" and streak.total_meals_cooked >= req_value:
            earned = True
        elif req_type == "streak_days" and streak.current_streak >= req_value:
            earned = True
        elif req_type == "longest_streak" and streak.longest_streak >= req_value:
            earned = True

        if earned:
            user_achievement = UserAchievement(
                id=str(uuid.uuid4()),
                user_id=user.id,
                achievement_id=achievement.id,
                unlocked_at=date.today(),
            )
            db.add(user_achievement)
            newly_unlocked.append(achievement.name)
            logger.info(f"Achievement unlocked for user {user.id}: {achievement.name}")

            # Create notification for achievement
            try:
                from app.services.notification_service import notify_achievement_unlocked
                await notify_achievement_unlocked(
                    db, user.id, achievement.name, achievement.icon
                )
            except Exception as e:
                logger.warning(f"Failed to notify achievement: {e}")

    if newly_unlocked:
        await db.commit()

    # Check streak milestones for notification
    try:
        from app.services.notification_service import notify_cooking_streak_milestone
        await notify_cooking_streak_milestone(db, user.id, streak.current_streak)
    except Exception as e:
        logger.warning(f"Failed to notify streak milestone: {e}")

    return newly_unlocked
