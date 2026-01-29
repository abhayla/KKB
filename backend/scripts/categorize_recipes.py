#!/usr/bin/env python3
"""
Categorize Recipes for Pairing

This script analyzes existing recipes and assigns categories for meal pairing.
Categories are based on recipe names, ingredients, and meal types.

Usage:
    python scripts/categorize_recipes.py                    # Categorize all
    python scripts/categorize_recipes.py --dry-run          # Preview changes
    python scripts/categorize_recipes.py --limit 100        # Limit to N recipes
    python scripts/categorize_recipes.py --verify           # Show category distribution
"""

import argparse
import asyncio
import logging
import re
import sys
from collections import defaultdict
from pathlib import Path

# Add parent directory to path for imports
sys.path.insert(0, str(Path(__file__).parent.parent))

from app.db.firestore import get_firestore_client

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


# Category detection patterns (regex patterns for recipe names)
CATEGORY_PATTERNS = {
    # Dal varieties
    "dal": [
        r"\bdal\b", r"\bdaal\b", r"\bsambar\b", r"\brasam\b",
        r"\blentil", r"\btoor\b", r"\bmoong\b", r"\burad\b",
        r"\bmasoor\b", r"\bchana dal\b", r"\barhar\b",
    ],

    # Rice dishes
    "rice": [
        r"\brice\b", r"\bchawal\b", r"\bpulao\b", r"\bpulav\b",
        r"\bbiryani\b", r"\bkhichdi\b", r"\bkhichri\b",
        r"\btehri\b", r"\bbath\b", r"\bsadam\b",
        r"\bcurd rice\b", r"\blemon rice\b", r"\btamarind rice\b",
    ],

    # Biryani specific
    "biryani": [
        r"\bbiryani\b", r"\bbiriyani\b",
    ],

    # Pulao specific
    "pulao": [
        r"\bpulao\b", r"\bpulav\b", r"\bpilaf\b", r"\bpilau\b",
    ],

    # Khichdi specific
    "khichdi": [
        r"\bkhichdi\b", r"\bkhichri\b", r"\bkhichdee\b",
    ],

    # Bread/Roti varieties
    "roti": [
        r"\broti\b", r"\bchapati\b", r"\bchapatti\b", r"\bphulka\b",
        r"\bbhakri\b", r"\brotla\b", r"\bmissi roti\b",
    ],

    # Paratha
    "paratha": [
        r"\bparatha\b", r"\bparantha\b", r"\baloo paratha\b",
        r"\bgobi paratha\b", r"\bpaneer paratha\b", r"\bthepla\b",
    ],

    # Naan
    "naan": [
        r"\bnaan\b", r"\bnan\b", r"\bkulcha\b", r"\bbhatura\b",
    ],

    # Puri/Bread
    "bread": [
        r"\bpuri\b", r"\bpoori\b", r"\bluchi\b", r"\bbread\b",
        r"\btoast\b", r"\bpav\b", r"\bbun\b",
    ],

    # Sabzi/Vegetable dishes
    "sabzi": [
        r"\bsabzi\b", r"\bsabji\b", r"\bsubzi\b", r"\bbhaji\b",
        r"\baloo\b", r"\bgobi\b", r"\bbhindi\b", r"\blauki\b",
        r"\btori\b", r"\bkarela\b", r"\bbaingan\b", r"\bpalak\b",
        r"\bmethi\b", r"\bmatar\b", r"\bpaneer\b(?!.*biryani)",
        r"\bmixed veg", r"\bvegetable\b", r"\bsukhi\b",
    ],

    # Curry
    "curry": [
        r"\bcurry\b", r"\bmasala\b(?!.*chai)", r"\bgravy\b",
        r"\bkorma\b", r"\brogan josh\b", r"\bvindaloo\b",
        r"\bkadai\b", r"\bkadhai\b", r"\bhandi\b",
        r"\bmakhani\b", r"\bbutter\b(?!.*milk|.*scotch)",
    ],

    # South Indian breakfast
    "dosa": [
        r"\bdosa\b", r"\bdosai\b", r"\bmasala dosa\b",
        r"\brava dosa\b", r"\bpaper dosa\b",
    ],

    "idli": [
        r"\bidli\b", r"\bidly\b",
    ],

    "uttapam": [
        r"\buttapam\b", r"\buttappam\b",
    ],

    "vada": [
        r"\bvada\b", r"\bvadai\b", r"\bmedu vada\b",
        r"\bdahi vada\b", r"\bbonda\b",
    ],

    # Sambar (separate from dal for South Indian)
    "sambar": [
        r"\bsambar\b", r"\bsambhar\b",
    ],

    # Rasam
    "rasam": [
        r"\brasam\b", r"\brasum\b",
    ],

    # Breakfast items
    "upma": [
        r"\bupma\b", r"\buppuma\b",
    ],

    "poha": [
        r"\bpoha\b", r"\bpohe\b", r"\baval\b", r"\bchivda\b",
    ],

    "breakfast_main": [
        r"\bpongal\b", r"\bpesarattu\b", r"\bappam\b",
        r"\bputtu\b", r"\bcheela\b", r"\bchilla\b",
    ],

    # Accompaniments
    "chutney": [
        r"\bchutney\b", r"\bchatni\b",
    ],

    "raita": [
        r"\braita\b", r"\bpachadi\b",
    ],

    "pickle": [
        r"\bpickle\b", r"\bachar\b", r"\bachaar\b",
    ],

    "salad": [
        r"\bsalad\b", r"\bkachumber\b", r"\bkosambari\b",
    ],

    # Beverages
    "chai": [
        r"\bchai\b", r"\btea\b(?!.*green tea)", r"\bmasala chai\b",
    ],

    "coffee": [
        r"\bcoffee\b", r"\bkaapi\b",
    ],

    "lassi": [
        r"\blassi\b", r"\bchaas\b", r"\bbuttermilk\b", r"\bmattha\b",
    ],

    "juice": [
        r"\bjuice\b", r"\bsharbat\b", r"\bsherbet\b",
        r"\bsmoothie\b", r"\bshake\b",
    ],

    # Snacks
    "snack": [
        r"\bsamosa\b", r"\bpakora\b", r"\bpakoda\b", r"\bbhajia\b",
        r"\bkachori\b", r"\bmathri\b", r"\bnamkeen\b",
        r"\bdhokla\b", r"\bkhandvi\b", r"\bfafda\b",
        r"\bkhakhra\b", r"\bsev\b", r"\bchakli\b",
        r"\bmurukku\b", r"\bchips\b", r"\bfritters\b",
        r"\bcutlet\b", r"\btikki\b", r"\bkebab\b",
        r"\bchaat\b", r"\bpani puri\b", r"\bgolgappa\b",
        r"\bpapdi\b", r"\bbhel\b",
    ],

    # Sweets/Desserts
    "sweet": [
        r"\bsweet\b", r"\bmithai\b", r"\bbarfi\b", r"\bburfi\b",
        r"\bladdu\b", r"\bladoo\b", r"\bgulab jamun\b",
        r"\brasgulla\b", r"\bras malai\b", r"\bjalebi\b",
        r"\bhalwa\b", r"\bsheera\b", r"\bkheer\b",
        r"\bpayasam\b", r"\bpudding\b", r"\bkulfi\b",
        r"\bbasundi\b", r"\brabri\b", r"\bshrikhand\b",
        r"\bsandesh\b", r"\bmishti\b", r"\bpeda\b",
        r"\bcake\b", r"\bpastry\b", r"\bcookie\b", r"\bbiscuit\b",
        r"\bmuffin\b", r"\bbrownies?\b", r"\bpancake\b",
        r"\bdessert\b", r"\bice cream\b", r"\bfalooda\b",
    ],

    # Egg dishes
    "egg_dish": [
        r"\begg\b", r"\banda\b", r"\bomelet", r"\bfrittata\b",
        r"\bscrambled\b", r"\bboiled egg\b", r"\bbhurji\b",
    ],

    # Soup
    "soup": [
        r"\bsoup\b", r"\bshorba\b",
    ],

    # Sandwich/Wrap
    "sandwich": [
        r"\bsandwich\b", r"\bwrap\b", r"\broll\b(?!.*spring)",
        r"\bburger\b", r"\bfrankies?\b",
    ],

    # One-pot meals
    "one_pot": [
        r"\bthali\b", r"\bmeal\b", r"\bplatter\b",
    ],
}

# Priority order for category assignment (if multiple match)
CATEGORY_PRIORITY = [
    "biryani", "pulao", "khichdi",  # Specific rice dishes first
    "dosa", "idli", "uttapam", "vada",  # South Indian specific
    "sambar", "rasam",  # South Indian specific
    "paratha", "naan", "bread", "roti",  # Breads
    "upma", "poha", "breakfast_main",  # Breakfast
    "dal",  # Lentils
    "curry", "sabzi",  # Main dishes
    "rice",  # Generic rice
    "chutney", "raita", "pickle", "salad",  # Accompaniments
    "chai", "coffee", "lassi", "juice",  # Beverages
    "snack",  # Snacks
    "sweet",  # Sweets
    "egg_dish",  # Egg
    "soup",  # Soup
    "sandwich",  # Sandwich
    "one_pot",  # One-pot
]


def categorize_recipe(recipe: dict) -> str:
    """
    Categorize a recipe based on its name and characteristics.

    Returns the most appropriate category for the recipe.
    """
    name = recipe.get("name", "").lower()

    # Check patterns in priority order
    for category in CATEGORY_PRIORITY:
        patterns = CATEGORY_PATTERNS.get(category, [])
        for pattern in patterns:
            if re.search(pattern, name, re.IGNORECASE):
                return category

    # Fallback: check meal_types
    meal_types = recipe.get("meal_types", [])
    if "breakfast" in meal_types:
        return "breakfast_main"
    if "snacks" in meal_types:
        return "snack"

    # Default category
    return "other"


async def get_all_recipes(db, limit: int = None) -> list:
    """Fetch all recipes from Firestore."""
    query = db.collection("recipes").where("is_active", "==", True)

    if limit:
        query = query.limit(limit)

    recipes = []
    async for doc in query.stream():
        recipe = doc.to_dict()
        recipe["id"] = doc.id
        recipes.append(recipe)

    return recipes


async def update_recipe_category(db, recipe_id: str, category: str, dry_run: bool = False):
    """Update a recipe's category in Firestore."""
    if dry_run:
        return

    await db.collection("recipes").document(recipe_id).update({
        "category": category
    })


async def verify_categories(db):
    """Show category distribution of recipes."""
    logger.info("=" * 60)
    logger.info("Recipe Category Distribution")
    logger.info("=" * 60)

    recipes = await get_all_recipes(db)

    # Count by category
    category_counts = defaultdict(int)
    uncategorized = []

    for recipe in recipes:
        category = recipe.get("category", "uncategorized")
        category_counts[category] += 1

        if category == "uncategorized" or not category:
            uncategorized.append(recipe.get("name", "Unknown"))

    # Sort by count
    sorted_counts = sorted(category_counts.items(), key=lambda x: -x[1])

    total = sum(category_counts.values())
    logger.info(f"\nTotal recipes: {total}\n")

    for category, count in sorted_counts:
        pct = 100 * count / total if total > 0 else 0
        bar = "█" * int(pct / 2)
        logger.info(f"  {category:20} {count:5} ({pct:5.1f}%) {bar}")

    if uncategorized:
        logger.info(f"\nUncategorized recipes ({len(uncategorized)}):")
        for name in uncategorized[:20]:
            logger.info(f"  - {name}")
        if len(uncategorized) > 20:
            logger.info(f"  ... and {len(uncategorized) - 20} more")


async def main():
    parser = argparse.ArgumentParser(description="Categorize recipes for pairing")
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Preview changes without updating Firestore"
    )
    parser.add_argument(
        "--limit",
        type=int,
        default=None,
        help="Limit number of recipes to process"
    )
    parser.add_argument(
        "--verify",
        action="store_true",
        help="Only show current category distribution"
    )

    args = parser.parse_args()

    logger.info("=" * 60)
    logger.info("  RECIPE CATEGORIZATION")
    logger.info("=" * 60)

    db = get_firestore_client()

    if args.verify:
        await verify_categories(db)
        return 0

    if args.dry_run:
        logger.info("  Mode: DRY RUN (no changes will be made)")
    else:
        logger.info("  Mode: LIVE (will update Firestore)")

    # Fetch recipes
    logger.info("\nFetching recipes...")
    recipes = await get_all_recipes(db, args.limit)
    logger.info(f"  Found {len(recipes)} recipes")

    # Categorize
    logger.info("\nCategorizing recipes...")
    category_counts = defaultdict(int)
    updates = []

    for recipe in recipes:
        current_category = recipe.get("category")
        new_category = categorize_recipe(recipe)

        # Only update if category changed or missing
        if current_category != new_category:
            updates.append({
                "id": recipe["id"],
                "name": recipe.get("name", "Unknown"),
                "old": current_category,
                "new": new_category
            })

        category_counts[new_category] += 1

    # Show distribution
    logger.info("\nCategory distribution:")
    sorted_counts = sorted(category_counts.items(), key=lambda x: -x[1])
    for category, count in sorted_counts:
        pct = 100 * count / len(recipes) if recipes else 0
        logger.info(f"  {category:20} {count:5} ({pct:5.1f}%)")

    # Show updates
    logger.info(f"\nRecipes to update: {len(updates)}")

    if updates and args.dry_run:
        logger.info("\nSample updates (first 20):")
        for update in updates[:20]:
            logger.info(f"  {update['name'][:40]:40} {update['old'] or 'None':15} → {update['new']}")

    # Perform updates
    if not args.dry_run and updates:
        logger.info("\nUpdating Firestore...")
        for i, update in enumerate(updates):
            await update_recipe_category(db, update["id"], update["new"])
            if (i + 1) % 100 == 0:
                logger.info(f"  Updated {i + 1}/{len(updates)} recipes")

        logger.info(f"  ✓ Updated {len(updates)} recipes")

    logger.info("\n" + "=" * 60)
    logger.info("✓ Categorization complete!")

    return 0


if __name__ == "__main__":
    exit_code = asyncio.run(main())
    sys.exit(exit_code)
