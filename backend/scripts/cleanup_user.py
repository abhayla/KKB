"""Cleanup script to delete a user and all related data from PostgreSQL.

All child tables have CASCADE delete on user_id foreign key,
so deleting the user row cleanly removes everything.

Usage:
    cd backend
    python scripts/cleanup_user.py e2e-test@rasoiai.test
"""

import asyncio
import os
import sys
from pathlib import Path

# Add parent directory to path for imports
sys.path.insert(0, str(Path(__file__).parent.parent))

from sqlalchemy import select, delete, func, text
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession

from app.models.user import User, UserPreferences, FamilyMember
from app.models.recipe_rule import RecipeRule, NutritionGoal
from app.models.meal_plan import MealPlan, MealPlanItem
from app.models.grocery import GroceryList, GroceryItem
from app.models.chat import ChatMessage
from app.models.stats import CookingStreak, CookingDay, UserAchievement
from app.models.notification import Notification, FcmToken

# PostgreSQL connection — reads from env var with fallback to VPS
DATABASE_URL = os.environ.get(
    "DATABASE_URL",
    "postgresql+asyncpg://rasoiai_user:RasoiAI2024Secure@103.118.16.189:5432/rasoiai"
)

# Tables to count before deletion (in dependency order)
CHILD_TABLES = [
    ("user_preferences", UserPreferences),
    ("family_members", FamilyMember),
    ("recipe_rules", RecipeRule),
    ("nutrition_goals", NutritionGoal),
    ("meal_plans", MealPlan),
    ("grocery_lists", GroceryList),
    ("chat_messages", ChatMessage),
    ("cooking_streaks", CookingStreak),
    ("user_achievements", UserAchievement),
    ("notifications", Notification),
    ("fcm_tokens", FcmToken),
]


async def cleanup_user(email: str):
    engine = create_async_engine(DATABASE_URL, echo=False)
    async_session_maker = async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)

    async with async_session_maker() as session:
        # Look up user by email
        result = await session.execute(select(User).where(User.email == email))
        user = result.scalar_one_or_none()

        if not user:
            print(f"No user found with email: {email}")
            await engine.dispose()
            return

        user_id = user.id
        print(f"Found user: {user.name or '(no name)'} ({user.email})")
        print(f"  ID: {user_id}")
        print(f"  Firebase UID: {user.firebase_uid}")
        print(f"  Onboarded: {user.is_onboarded}")
        print(f"  Created: {user.created_at}")
        print()

        # Count rows in each child table
        print("Data to be deleted:")
        print("-" * 40)
        total_rows = 0
        for table_name, model in CHILD_TABLES:
            count_result = await session.execute(
                select(func.count()).select_from(model).where(model.user_id == user_id)
            )
            count = count_result.scalar()
            if count > 0:
                print(f"  {table_name}: {count} rows")
            total_rows += count

        # Count grandchild tables (no direct user_id FK)
        # meal_plan_items (via meal_plans)
        mp_result = await session.execute(
            select(MealPlan.id).where(MealPlan.user_id == user_id)
        )
        mp_ids = [row[0] for row in mp_result.all()]
        if mp_ids:
            mpi_count = await session.execute(
                select(func.count()).select_from(MealPlanItem).where(MealPlanItem.meal_plan_id.in_(mp_ids))
            )
            mpi_val = mpi_count.scalar()
            if mpi_val > 0:
                print(f"  meal_plan_items: {mpi_val} rows")
            total_rows += mpi_val

        # grocery_items (via grocery_lists)
        gl_result = await session.execute(
            select(GroceryList.id).where(GroceryList.user_id == user_id)
        )
        gl_ids = [row[0] for row in gl_result.all()]
        if gl_ids:
            gi_count = await session.execute(
                select(func.count()).select_from(GroceryItem).where(GroceryItem.grocery_list_id.in_(gl_ids))
            )
            gi_val = gi_count.scalar()
            if gi_val > 0:
                print(f"  grocery_items: {gi_val} rows")
            total_rows += gi_val

        # cooking_days (via cooking_streaks)
        cs_result = await session.execute(
            select(CookingStreak.id).where(CookingStreak.user_id == user_id)
        )
        cs_ids = [row[0] for row in cs_result.all()]
        if cs_ids:
            cd_count = await session.execute(
                select(func.count()).select_from(CookingDay).where(CookingDay.cooking_streak_id.in_(cs_ids))
            )
            cd_val = cd_count.scalar()
            if cd_val > 0:
                print(f"  cooking_days: {cd_val} rows")
            total_rows += cd_val

        print("-" * 40)
        print(f"  Total: {total_rows} rows + 1 user row")
        print()

        # Delete the user (CASCADE handles all child tables)
        await session.execute(delete(User).where(User.id == user_id))
        await session.commit()

        print(f"Deleted user '{email}' and all {total_rows} related rows.")

        # Verify deletion
        verify = await session.execute(select(User).where(User.email == email))
        if verify.scalar_one_or_none() is None:
            print("Verification: User no longer exists in database.")
        else:
            print("WARNING: User still exists after deletion!")

    await engine.dispose()


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python scripts/cleanup_user.py <email>")
        print("Example: python scripts/cleanup_user.py e2e-test@rasoiai.test")
        sys.exit(1)

    email = sys.argv[1]
    print(f"Cleaning up user: {email}")
    print("=" * 40)
    asyncio.run(cleanup_user(email))
