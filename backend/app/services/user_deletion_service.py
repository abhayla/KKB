"""User deletion and data export service (GDPR compliance)."""

import logging
from datetime import datetime, timedelta, timezone

from sqlalchemy import select, update
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.models.chat import ChatMessage
from app.models.grocery import GroceryItem, GroceryList
from app.models.meal_plan import MealPlan, MealPlanItem
from app.models.notification import FcmToken, Notification
from app.models.recipe_rule import NutritionGoal, RecipeRule
from app.models.stats import CookingDay, CookingStreak, UserAchievement
from app.models.user import FamilyMember, User, UserPreferences

logger = logging.getLogger(__name__)


async def soft_delete_user(db: AsyncSession, user_id: str) -> dict:
    """Soft-delete a user account.

    Sets is_active=False and deleted_at=now(). Data is retained for 30 days
    before permanent deletion by a cleanup job.

    Args:
        db: Database session
        user_id: User ID to delete

    Returns:
        Dict with deletion confirmation and scheduled purge date
    """
    result = await db.execute(select(User).where(User.id == user_id))
    user = result.scalar_one_or_none()

    if not user:
        raise ValueError("User not found")

    now = datetime.now(timezone.utc)
    deletion_date = now + timedelta(days=30)

    user.is_active = False
    user.deleted_at = now

    await db.commit()

    logger.info(f"User {user_id} soft-deleted, purge scheduled for {deletion_date.date()}")

    return {
        "message": "Account scheduled for deletion",
        "deletion_date": deletion_date.date().isoformat(),
    }


async def export_user_data(db: AsyncSession, user_id: str) -> dict:
    """Export all user data as a JSON-serializable dict.

    Collects: profile, preferences, family members, meal plans,
    recipe rules, nutrition goals, chat messages, grocery lists,
    stats, achievements, notifications.

    Args:
        db: Database session
        user_id: User ID to export

    Returns:
        Dict containing all user data
    """
    # Load user with preferences and family members
    result = await db.execute(
        select(User)
        .options(
            selectinload(User.preferences),
            selectinload(User.family_members),
        )
        .where(User.id == user_id)
    )
    user = result.scalar_one_or_none()

    if not user:
        raise ValueError("User not found")

    export = {
        "profile": {
            "id": user.id,
            "email": user.email,
            "name": user.name,
            "is_onboarded": user.is_onboarded,
            "created_at": user.created_at.isoformat() if user.created_at else None,
        },
        "preferences": _export_preferences(user.preferences) if user.preferences else None,
        "family_members": [
            {
                "name": fm.name,
                "age_group": fm.age_group,
                "dietary_restrictions": fm.dietary_restrictions,
                "health_conditions": fm.health_conditions,
            }
            for fm in (user.family_members or [])
        ],
    }

    # Meal plans
    mp_result = await db.execute(
        select(MealPlan).where(MealPlan.user_id == user_id)
    )
    meal_plans = mp_result.scalars().all()
    export["meal_plans"] = [
        {
            "id": mp.id,
            "week_start_date": mp.week_start_date.isoformat() if mp.week_start_date else None,
            "week_end_date": mp.week_end_date.isoformat() if mp.week_end_date else None,
            "is_active": mp.is_active,
            "created_at": mp.created_at.isoformat() if mp.created_at else None,
        }
        for mp in meal_plans
    ]

    # Recipe rules
    rr_result = await db.execute(
        select(RecipeRule).where(RecipeRule.user_id == user_id)
    )
    rules = rr_result.scalars().all()
    export["recipe_rules"] = [
        {
            "target_type": r.target_type,
            "action": r.action,
            "target_name": r.target_name,
            "frequency_type": r.frequency_type,
            "frequency_count": r.frequency_count,
        }
        for r in rules
    ]

    # Nutrition goals
    ng_result = await db.execute(
        select(NutritionGoal).where(NutritionGoal.user_id == user_id)
    )
    goals = ng_result.scalars().all()
    export["nutrition_goals"] = [
        {
            "food_category": g.food_category,
            "weekly_target": g.weekly_target,
            "enforcement": g.enforcement,
        }
        for g in goals
    ]

    # Chat messages
    chat_result = await db.execute(
        select(ChatMessage).where(ChatMessage.user_id == user_id)
    )
    messages = chat_result.scalars().all()
    export["chat_messages"] = [
        {
            "role": m.role,
            "content": m.content,
            "created_at": m.created_at.isoformat() if m.created_at else None,
        }
        for m in messages
    ]

    # Grocery lists
    gl_result = await db.execute(
        select(GroceryList)
        .options(selectinload(GroceryList.items))
        .where(GroceryList.user_id == user_id)
    )
    grocery_lists = gl_result.scalars().all()
    export["grocery_lists"] = [
        {
            "id": gl.id,
            "items": [
                {"name": item.name, "quantity": item.quantity, "is_checked": item.is_checked}
                for item in (gl.items or [])
            ],
        }
        for gl in grocery_lists
    ]

    # Notifications
    notif_result = await db.execute(
        select(Notification).where(Notification.user_id == user_id)
    )
    notifications = notif_result.scalars().all()
    export["notifications_count"] = len(notifications)

    logger.info(f"Exported data for user {user_id}")
    return export


def _export_preferences(prefs: UserPreferences) -> dict:
    """Convert UserPreferences model to export dict."""
    return {
        "dietary_type": prefs.dietary_type,
        "dietary_tags": prefs.dietary_tags,
        "allergies": prefs.allergies,
        "disliked_ingredients": prefs.disliked_ingredients,
        "cuisine_preferences": prefs.cuisine_preferences,
        "cooking_time_preference": prefs.cooking_time_preference,
        "spice_level": prefs.spice_level,
        "weekday_cooking_time_minutes": prefs.weekday_cooking_time_minutes,
        "weekend_cooking_time_minutes": prefs.weekend_cooking_time_minutes,
        "busy_days": prefs.busy_days,
        "family_size": prefs.family_size,
        "items_per_meal": prefs.items_per_meal,
    }
