"""Seed script for achievements data to PostgreSQL."""

import asyncio
import os
import sys
import uuid
from pathlib import Path

from dotenv import load_dotenv

# Add parent directory to path for imports
sys.path.insert(0, str(Path(__file__).parent.parent))

# Load environment variables
load_dotenv()

from sqlalchemy import select
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession

from app.models.stats import Achievement

# PostgreSQL connection settings from environment
DATABASE_URL = os.getenv("DATABASE_URL")
if not DATABASE_URL:
    raise ValueError("DATABASE_URL environment variable is required")

# Achievement definitions
ACHIEVEMENTS = [
    # Streak achievements
    {
        "name": "First Steps",
        "description": "Cook your first meal using the app",
        "icon": "🍳",
        "category": "streak",
        "requirement_type": "meals_cooked",
        "requirement_value": 1,
    },
    {
        "name": "Week Warrior",
        "description": "Maintain a 7-day cooking streak",
        "icon": "🔥",
        "category": "streak",
        "requirement_type": "streak_days",
        "requirement_value": 7,
    },
    {
        "name": "Monthly Master",
        "description": "Maintain a 30-day cooking streak",
        "icon": "🏆",
        "category": "streak",
        "requirement_type": "streak_days",
        "requirement_value": 30,
    },
    {
        "name": "Century Chef",
        "description": "Cook 100 meals using the app",
        "icon": "👨‍🍳",
        "category": "streak",
        "requirement_type": "meals_cooked",
        "requirement_value": 100,
    },
    # Variety achievements
    {
        "name": "North Explorer",
        "description": "Try 10 North Indian recipes",
        "icon": "🏔️",
        "category": "variety",
        "requirement_type": "north_recipes",
        "requirement_value": 10,
    },
    {
        "name": "South Specialist",
        "description": "Try 10 South Indian recipes",
        "icon": "🌴",
        "category": "variety",
        "requirement_type": "south_recipes",
        "requirement_value": 10,
    },
    {
        "name": "East Enthusiast",
        "description": "Try 5 East Indian recipes",
        "icon": "🎋",
        "category": "variety",
        "requirement_type": "east_recipes",
        "requirement_value": 5,
    },
    {
        "name": "West Wonder",
        "description": "Try 5 West Indian recipes",
        "icon": "🏜️",
        "category": "variety",
        "requirement_type": "west_recipes",
        "requirement_value": 5,
    },
    {
        "name": "Pan-India Chef",
        "description": "Try recipes from all 4 regions",
        "icon": "🇮🇳",
        "category": "variety",
        "requirement_type": "cuisines_tried",
        "requirement_value": 4,
    },
    # Health achievements
    {
        "name": "Green Champion",
        "description": "Include green leafy vegetables 7 times in a week",
        "icon": "🥬",
        "category": "health",
        "requirement_type": "green_vegetables_weekly",
        "requirement_value": 7,
    },
    {
        "name": "Protein Power",
        "description": "Meet protein goals for 14 consecutive days",
        "icon": "💪",
        "category": "health",
        "requirement_type": "protein_streak",
        "requirement_value": 14,
    },
    {
        "name": "Balanced Bhojan",
        "description": "Complete a balanced meal plan for 4 weeks",
        "icon": "⚖️",
        "category": "health",
        "requirement_type": "balanced_weeks",
        "requirement_value": 4,
    },
    # Festival achievements
    {
        "name": "Festival Foodie",
        "description": "Cook festival special meals 5 times",
        "icon": "🎉",
        "category": "festival",
        "requirement_type": "festival_meals",
        "requirement_value": 5,
    },
    {
        "name": "Sattvic Soul",
        "description": "Complete 7 days of sattvic meals",
        "icon": "🧘",
        "category": "festival",
        "requirement_type": "sattvic_days",
        "requirement_value": 7,
    },
    # Skill achievements
    {
        "name": "Quick Cook",
        "description": "Make 20 quick meals (under 30 minutes)",
        "icon": "⚡",
        "category": "skill",
        "requirement_type": "quick_meals",
        "requirement_value": 20,
    },
    {
        "name": "Weekend Chef",
        "description": "Make 10 elaborate meals (over 60 minutes)",
        "icon": "👩‍🍳",
        "category": "skill",
        "requirement_type": "elaborate_meals",
        "requirement_value": 10,
    },
]


async def seed_achievements():
    """Seed the database with achievement data."""
    # Create engine and session
    engine = create_async_engine(DATABASE_URL, echo=False)
    async_session_maker = async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)

    async with async_session_maker() as session:
        # Check if achievements already exist
        result = await session.execute(select(Achievement).limit(1))
        if result.scalar_one_or_none():
            print("Achievements already exist. Skipping seed.")
            return

        for achievement_data in ACHIEVEMENTS:
            achievement = Achievement(
                id=str(uuid.uuid4()),
                name=achievement_data["name"],
                description=achievement_data["description"],
                icon=achievement_data["icon"],
                category=achievement_data["category"],
                requirement_type=achievement_data["requirement_type"],
                requirement_value=achievement_data["requirement_value"],
            )
            session.add(achievement)

        await session.commit()
        print(f"Seeded {len(ACHIEVEMENTS)} achievements successfully!")


if __name__ == "__main__":
    asyncio.run(seed_achievements())
