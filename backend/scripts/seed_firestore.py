"""Seed script for Firestore with recipes and festivals."""

import asyncio
import uuid
from datetime import datetime, timezone

from app.repositories.recipe_repository import RecipeRepository
from app.repositories.festival_repository import FestivalRepository

# Sample Indian recipes covering all cuisines and meal types
RECIPES = [
    {
        "name": "Aloo Paratha",
        "description": "Crispy whole wheat flatbread stuffed with spiced mashed potatoes.",
        "cuisine_type": "north",
        "meal_types": ["breakfast"],
        "dietary_tags": ["vegetarian"],
        "prep_time_minutes": 20,
        "cook_time_minutes": 20,
        "total_time_minutes": 40,
        "servings": 4,
        "difficulty_level": "medium",
        "is_quick_meal": False,
        "is_kid_friendly": True,
        "is_fasting_friendly": False,
        "ingredients": [
            {"name": "Whole wheat flour", "quantity": 2, "unit": "cups", "category": "grains"},
            {"name": "Potatoes", "quantity": 3, "unit": "medium", "category": "vegetables"},
            {"name": "Green chilies", "quantity": 2, "unit": "pieces", "category": "vegetables"},
            {"name": "Ghee", "quantity": 4, "unit": "tbsp", "category": "dairy"},
        ],
        "instructions": [
            {"step": 1, "text": "Boil and mash potatoes with spices.", "time": 15},
            {"step": 2, "text": "Knead dough, rest 10 minutes.", "time": 10},
            {"step": 3, "text": "Roll, fill, and cook on tawa with ghee.", "time": 15},
        ],
        "nutrition": {"calories": 280, "protein": 7, "carbs": 42, "fat": 10, "fiber": 4},
    },
    {
        "name": "Poha",
        "description": "Light flattened rice with peanuts and mild spices.",
        "cuisine_type": "west",
        "meal_types": ["breakfast", "snacks"],
        "dietary_tags": ["vegetarian", "vegan"],
        "prep_time_minutes": 10,
        "cook_time_minutes": 15,
        "total_time_minutes": 25,
        "servings": 4,
        "difficulty_level": "easy",
        "is_quick_meal": True,
        "is_kid_friendly": True,
        "is_fasting_friendly": False,
        "ingredients": [
            {"name": "Flattened rice", "quantity": 2, "unit": "cups", "category": "grains"},
            {"name": "Peanuts", "quantity": 0.25, "unit": "cups", "category": "nuts"},
            {"name": "Onion", "quantity": 1, "unit": "medium", "category": "vegetables"},
        ],
        "instructions": [
            {"step": 1, "text": "Rinse poha and set aside.", "time": 3},
            {"step": 2, "text": "Temper with mustard seeds and peanuts.", "time": 5},
            {"step": 3, "text": "Add poha and cook covered.", "time": 7},
        ],
        "nutrition": {"calories": 220, "protein": 6, "carbs": 38, "fat": 6, "fiber": 2},
    },
    {
        "name": "Idli Sambhar",
        "description": "Soft steamed rice cakes with lentil vegetable stew.",
        "cuisine_type": "south",
        "meal_types": ["breakfast"],
        "dietary_tags": ["vegetarian", "vegan"],
        "prep_time_minutes": 30,
        "cook_time_minutes": 30,
        "total_time_minutes": 60,
        "servings": 4,
        "difficulty_level": "medium",
        "is_quick_meal": False,
        "is_kid_friendly": True,
        "is_fasting_friendly": False,
        "ingredients": [
            {"name": "Idli rice", "quantity": 2, "unit": "cups", "category": "grains"},
            {"name": "Urad dal", "quantity": 0.5, "unit": "cups", "category": "pulses"},
            {"name": "Toor dal", "quantity": 0.5, "unit": "cups", "category": "pulses"},
        ],
        "instructions": [
            {"step": 1, "text": "Grind batter and ferment overnight.", "time": 20},
            {"step": 2, "text": "Steam idlis for 12 minutes.", "time": 12},
            {"step": 3, "text": "Cook sambhar with vegetables.", "time": 20},
        ],
        "nutrition": {"calories": 250, "protein": 9, "carbs": 45, "fat": 3, "fiber": 5},
    },
    {
        "name": "Dal Makhani",
        "description": "Creamy black lentils slow-cooked with butter and cream.",
        "cuisine_type": "north",
        "meal_types": ["lunch", "dinner"],
        "dietary_tags": ["vegetarian"],
        "prep_time_minutes": 20,
        "cook_time_minutes": 60,
        "total_time_minutes": 80,
        "servings": 6,
        "difficulty_level": "medium",
        "is_quick_meal": False,
        "is_kid_friendly": True,
        "is_fasting_friendly": False,
        "ingredients": [
            {"name": "Black urad dal", "quantity": 1, "unit": "cups", "category": "pulses"},
            {"name": "Butter", "quantity": 4, "unit": "tbsp", "category": "dairy"},
            {"name": "Cream", "quantity": 0.5, "unit": "cups", "category": "dairy"},
        ],
        "instructions": [
            {"step": 1, "text": "Soak and pressure cook dal.", "time": 30},
            {"step": 2, "text": "Prepare masala base.", "time": 10},
            {"step": 3, "text": "Simmer dal with cream.", "time": 30},
        ],
        "nutrition": {"calories": 350, "protein": 14, "carbs": 35, "fat": 18, "fiber": 8},
    },
    {
        "name": "Paneer Butter Masala",
        "description": "Soft paneer in rich, creamy tomato gravy.",
        "cuisine_type": "north",
        "meal_types": ["lunch", "dinner"],
        "dietary_tags": ["vegetarian"],
        "prep_time_minutes": 15,
        "cook_time_minutes": 25,
        "total_time_minutes": 40,
        "servings": 4,
        "difficulty_level": "easy",
        "is_quick_meal": False,
        "is_kid_friendly": True,
        "is_fasting_friendly": False,
        "ingredients": [
            {"name": "Paneer", "quantity": 250, "unit": "grams", "category": "dairy"},
            {"name": "Tomatoes", "quantity": 4, "unit": "medium", "category": "vegetables"},
            {"name": "Cream", "quantity": 0.25, "unit": "cups", "category": "dairy"},
        ],
        "instructions": [
            {"step": 1, "text": "Blend tomatoes to puree.", "time": 10},
            {"step": 2, "text": "Cook gravy until thick.", "time": 15},
            {"step": 3, "text": "Add paneer and simmer.", "time": 10},
        ],
        "nutrition": {"calories": 380, "protein": 16, "carbs": 12, "fat": 32, "fiber": 2},
    },
    {
        "name": "Vegetable Biryani",
        "description": "Fragrant basmati rice layered with vegetables and spices.",
        "cuisine_type": "south",
        "meal_types": ["lunch", "dinner"],
        "dietary_tags": ["vegetarian"],
        "prep_time_minutes": 30,
        "cook_time_minutes": 40,
        "total_time_minutes": 70,
        "servings": 6,
        "difficulty_level": "medium",
        "is_quick_meal": False,
        "is_kid_friendly": True,
        "is_fasting_friendly": False,
        "ingredients": [
            {"name": "Basmati rice", "quantity": 2, "unit": "cups", "category": "grains"},
            {"name": "Mixed vegetables", "quantity": 2, "unit": "cups", "category": "vegetables"},
            {"name": "Biryani masala", "quantity": 2, "unit": "tbsp", "category": "spices"},
        ],
        "instructions": [
            {"step": 1, "text": "Parboil rice.", "time": 15},
            {"step": 2, "text": "Layer vegetables and rice.", "time": 15},
            {"step": 3, "text": "Cook on dum for 25 minutes.", "time": 25},
        ],
        "nutrition": {"calories": 380, "protein": 10, "carbs": 68, "fat": 8, "fiber": 5},
    },
    {
        "name": "Pav Bhaji",
        "description": "Spiced mashed vegetables with butter-toasted bread.",
        "cuisine_type": "west",
        "meal_types": ["dinner", "snacks"],
        "dietary_tags": ["vegetarian"],
        "prep_time_minutes": 20,
        "cook_time_minutes": 30,
        "total_time_minutes": 50,
        "servings": 4,
        "difficulty_level": "easy",
        "is_quick_meal": False,
        "is_kid_friendly": True,
        "is_fasting_friendly": False,
        "ingredients": [
            {"name": "Mixed vegetables", "quantity": 3, "unit": "cups", "category": "vegetables"},
            {"name": "Pav", "quantity": 8, "unit": "pieces", "category": "grains"},
            {"name": "Butter", "quantity": 6, "unit": "tbsp", "category": "dairy"},
        ],
        "instructions": [
            {"step": 1, "text": "Boil and mash vegetables.", "time": 20},
            {"step": 2, "text": "Cook with pav bhaji masala.", "time": 15},
            {"step": 3, "text": "Toast pav with butter.", "time": 5},
        ],
        "nutrition": {"calories": 420, "protein": 10, "carbs": 55, "fat": 20, "fiber": 6},
    },
    {
        "name": "Rajma Chawal",
        "description": "Hearty kidney bean curry with steamed rice.",
        "cuisine_type": "north",
        "meal_types": ["lunch", "dinner"],
        "dietary_tags": ["vegetarian", "vegan"],
        "prep_time_minutes": 20,
        "cook_time_minutes": 45,
        "total_time_minutes": 65,
        "servings": 4,
        "difficulty_level": "easy",
        "is_quick_meal": False,
        "is_kid_friendly": True,
        "is_fasting_friendly": False,
        "ingredients": [
            {"name": "Kidney beans", "quantity": 1.5, "unit": "cups", "category": "pulses"},
            {"name": "Basmati rice", "quantity": 1.5, "unit": "cups", "category": "grains"},
            {"name": "Tomatoes", "quantity": 3, "unit": "medium", "category": "vegetables"},
        ],
        "instructions": [
            {"step": 1, "text": "Pressure cook rajma.", "time": 30},
            {"step": 2, "text": "Prepare gravy.", "time": 15},
            {"step": 3, "text": "Serve with rice.", "time": 15},
        ],
        "nutrition": {"calories": 380, "protein": 14, "carbs": 68, "fat": 5, "fiber": 12},
    },
    {
        "name": "Masala Dosa",
        "description": "Crispy fermented crepe with spiced potato filling.",
        "cuisine_type": "south",
        "meal_types": ["breakfast", "dinner"],
        "dietary_tags": ["vegetarian", "vegan"],
        "prep_time_minutes": 30,
        "cook_time_minutes": 25,
        "total_time_minutes": 55,
        "servings": 4,
        "difficulty_level": "medium",
        "is_quick_meal": False,
        "is_kid_friendly": True,
        "is_fasting_friendly": False,
        "ingredients": [
            {"name": "Dosa rice", "quantity": 2, "unit": "cups", "category": "grains"},
            {"name": "Urad dal", "quantity": 0.5, "unit": "cups", "category": "pulses"},
            {"name": "Potatoes", "quantity": 4, "unit": "medium", "category": "vegetables"},
        ],
        "instructions": [
            {"step": 1, "text": "Grind and ferment batter.", "time": 15},
            {"step": 2, "text": "Prepare potato masala.", "time": 15},
            {"step": 3, "text": "Spread and cook dosa.", "time": 10},
        ],
        "nutrition": {"calories": 320, "protein": 8, "carbs": 55, "fat": 8, "fiber": 4},
    },
    {
        "name": "Sabudana Khichdi",
        "description": "Tapioca pearls with peanuts, popular fasting dish.",
        "cuisine_type": "west",
        "meal_types": ["breakfast", "snacks"],
        "dietary_tags": ["vegetarian"],
        "prep_time_minutes": 30,
        "cook_time_minutes": 15,
        "total_time_minutes": 45,
        "servings": 4,
        "difficulty_level": "easy",
        "is_quick_meal": False,
        "is_kid_friendly": True,
        "is_fasting_friendly": True,
        "ingredients": [
            {"name": "Sabudana", "quantity": 1, "unit": "cups", "category": "grains"},
            {"name": "Peanuts", "quantity": 0.25, "unit": "cups", "category": "nuts"},
            {"name": "Potatoes", "quantity": 2, "unit": "medium", "category": "vegetables"},
        ],
        "instructions": [
            {"step": 1, "text": "Soak sabudana overnight.", "time": 5},
            {"step": 2, "text": "Cook potatoes with spices.", "time": 10},
            {"step": 3, "text": "Add sabudana and peanuts.", "time": 10},
        ],
        "nutrition": {"calories": 280, "protein": 6, "carbs": 45, "fat": 9, "fiber": 2},
    },
]

# Indian festivals for 2025-2026
FESTIVALS = [
    {
        "name": "Makar Sankranti",
        "date": datetime(2025, 1, 14, tzinfo=timezone.utc),
        "regions": ["north", "west", "south"],
        "description": "Harvest festival celebrating the sun's transition into Capricorn.",
        "is_fasting": False,
        "special_foods": ["Til ladoo", "Gajak", "Khichdi", "Pongal"],
        "avoided_foods": [],
    },
    {
        "name": "Maha Shivaratri",
        "date": datetime(2025, 2, 26, tzinfo=timezone.utc),
        "regions": ["north", "south", "east", "west"],
        "description": "Night dedicated to Lord Shiva.",
        "is_fasting": True,
        "special_foods": ["Fruits", "Milk", "Thandai"],
        "avoided_foods": ["Grains", "Rice", "Regular salt"],
    },
    {
        "name": "Holi",
        "date": datetime(2025, 3, 14, tzinfo=timezone.utc),
        "regions": ["north", "east", "west"],
        "description": "Festival of colors celebrating spring.",
        "is_fasting": False,
        "special_foods": ["Gujiya", "Thandai", "Malpua", "Dahi Bhalla"],
        "avoided_foods": [],
    },
    {
        "name": "Ugadi",
        "date": datetime(2025, 3, 30, tzinfo=timezone.utc),
        "regions": ["south"],
        "description": "Telugu and Kannada New Year.",
        "is_fasting": False,
        "special_foods": ["Ugadi Pachadi", "Pulihora", "Bobbatlu"],
        "avoided_foods": [],
    },
    {
        "name": "Ram Navami",
        "date": datetime(2025, 4, 6, tzinfo=timezone.utc),
        "regions": ["north", "south", "east", "west"],
        "description": "Birthday of Lord Rama.",
        "is_fasting": True,
        "special_foods": ["Panakam", "Kosambari", "Fruits"],
        "avoided_foods": ["Non-vegetarian", "Onion", "Garlic"],
    },
    {
        "name": "Navratri",
        "date": datetime(2025, 9, 22, tzinfo=timezone.utc),
        "regions": ["north", "west", "east"],
        "description": "Nine nights celebrating Goddess Durga.",
        "is_fasting": True,
        "special_foods": ["Sabudana Khichdi", "Kuttu Puri", "Singhare Atta", "Fruits"],
        "avoided_foods": ["Grains", "Onion", "Garlic", "Non-vegetarian"],
    },
    {
        "name": "Dussehra",
        "date": datetime(2025, 10, 2, tzinfo=timezone.utc),
        "regions": ["north", "south", "east", "west"],
        "description": "Victory of good over evil.",
        "is_fasting": False,
        "special_foods": ["Jalebi", "Fafda", "Kheer"],
        "avoided_foods": [],
    },
    {
        "name": "Karwa Chauth",
        "date": datetime(2025, 10, 10, tzinfo=timezone.utc),
        "regions": ["north"],
        "description": "Married women fast for husband's longevity.",
        "is_fasting": True,
        "special_foods": ["Sargi", "Puri", "Halwa", "Fruits"],
        "avoided_foods": ["Everything until moonrise"],
    },
    {
        "name": "Diwali",
        "date": datetime(2025, 10, 20, tzinfo=timezone.utc),
        "regions": ["north", "south", "east", "west"],
        "description": "Festival of lights celebrating Lord Rama's return.",
        "is_fasting": False,
        "special_foods": ["Kaju Katli", "Ladoo", "Chakli", "Gulab Jamun", "Soan Papdi"],
        "avoided_foods": [],
    },
    {
        "name": "Bhai Dooj",
        "date": datetime(2025, 10, 22, tzinfo=timezone.utc),
        "regions": ["north", "east"],
        "description": "Celebration of brother-sister bond.",
        "is_fasting": False,
        "special_foods": ["Sweets", "Tikka ceremony items"],
        "avoided_foods": [],
    },
    {
        "name": "Chhath Puja",
        "date": datetime(2025, 10, 26, tzinfo=timezone.utc),
        "regions": ["east", "north"],
        "description": "Sun worship festival.",
        "is_fasting": True,
        "special_foods": ["Thekua", "Kheer", "Fruits"],
        "avoided_foods": ["Onion", "Garlic", "Non-vegetarian"],
    },
    {
        "name": "Pongal",
        "date": datetime(2026, 1, 14, tzinfo=timezone.utc),
        "regions": ["south"],
        "description": "Tamil harvest festival.",
        "is_fasting": False,
        "special_foods": ["Pongal", "Vadai", "Payasam", "Sugarcane"],
        "avoided_foods": [],
    },
]


async def seed_recipes():
    """Seed Firestore with sample recipes."""
    repo = RecipeRepository()

    # Check if recipes already exist
    existing = await repo.get_all(limit=1)
    if existing:
        print("Recipes already exist. Skipping seed.")
        return

    for recipe_data in RECIPES:
        await repo.create(recipe_data)

    print(f"Seeded {len(RECIPES)} recipes successfully!")


async def seed_festivals():
    """Seed Firestore with festival data."""
    repo = FestivalRepository()

    # Check if festivals already exist
    count = await repo.count()
    if count > 0:
        print("Festivals already exist. Skipping seed.")
        return

    for festival_data in FESTIVALS:
        await repo.create(festival_data)

    print(f"Seeded {len(FESTIVALS)} festivals successfully!")


async def main():
    """Run all seed functions."""
    print("Seeding Firestore database...")
    await seed_recipes()
    await seed_festivals()
    print("Done!")


if __name__ == "__main__":
    asyncio.run(main())
