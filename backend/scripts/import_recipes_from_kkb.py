"""
Import recipes from khanakyabanega Firebase project to RasoiAI.

This script reads recipes from the source database and transforms them
to match the RasoiAI schema before inserting into the target database.

Usage:
    cd backend
    source venv/bin/activate  # or venv\\Scripts\\activate on Windows

    # Dry run (preview only)
    python scripts/import_recipes_from_kkb.py --dry-run

    # Import first 100 recipes
    python scripts/import_recipes_from_kkb.py --limit 100

    # Import all recipes
    python scripts/import_recipes_from_kkb.py --all
"""

import argparse
import asyncio
import json
import os
import re
import sys
import uuid
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Optional

# Add parent directory to path for imports
sys.path.insert(0, str(Path(__file__).parent.parent))

from google.cloud.firestore_v1 import AsyncClient


# Source Firebase credentials (khanakyabanega project)
SOURCE_CREDENTIALS_PATH = "khanakyabanega-firebase-adminsdk-fbsvc-4351e5a2e3.json"

# Target Firebase credentials (RasoiAI project)
TARGET_CREDENTIALS_PATH = "rasoiai-firebase-service-account.json"


# Mapping for cuisine types
CUISINE_MAPPING = {
    "north indian": "north",
    "south indian": "south",
    "east indian": "east",
    "west indian": "west",
    "bengali": "east",
    "gujarati": "west",
    "maharashtrian": "west",
    "punjabi": "north",
    "rajasthani": "north",
    "kashmiri": "north",
    "tamil": "south",
    "kerala": "south",
    "andhra": "south",
    "karnataka": "south",
    "telugu": "south",
    "hyderabadi": "south",
    "mughlai": "north",
    "awadhi": "north",
    "chettinad": "south",
    "indo chinese": "east",
    "street food": "north",  # Default for street food
    "indian": "north",  # Default
    "fusion": "north",  # Default
}

# Mapping for diet types
DIET_MAPPING = {
    "vegetarian": ["vegetarian"],
    "vegan": ["vegetarian", "vegan"],
    "non-vegetarian": ["non_vegetarian"],
    "non vegetarian": ["non_vegetarian"],
    "eggetarian": ["vegetarian", "eggetarian"],
    "jain": ["vegetarian", "jain"],
}

# Mapping for meal types
MEAL_TYPE_MAPPING = {
    "breakfast": "breakfast",
    "lunch": "lunch",
    "dinner": "dinner",
    "snack": "snacks",
    "snacks": "snacks",
    "dessert": "snacks",  # Group desserts with snacks
    "appetizer": "snacks",
    "side dish": "snacks",
    "main course": "lunch",  # Default to lunch for main course
    "beverage": "snacks",
    "brunch": "breakfast",
}

# Difficulty mapping
DIFFICULTY_MAPPING = {
    "easy": "easy",
    "medium": "medium",
    "hard": "hard",
    "difficult": "hard",
    "moderate": "medium",
}

# Ingredient category keywords
INGREDIENT_CATEGORIES = {
    "vegetables": [
        "onion", "tomato", "potato", "carrot", "spinach", "cabbage", "cauliflower",
        "broccoli", "capsicum", "pepper", "beans", "peas", "corn", "cucumber",
        "eggplant", "brinjal", "baingan", "zucchini", "pumpkin", "gourd", "lauki",
        "tinda", "parwal", "karela", "bhindi", "okra", "mushroom", "garlic", "ginger",
        "chili", "chilli", "mirch", "coriander", "cilantro", "mint", "methi",
        "palak", "saag", "kale", "lettuce", "radish", "mooli", "turnip", "shalgam",
        "beetroot", "chuka", "colocasia", "arbi", "yam", "suran"
    ],
    "fruits": [
        "mango", "banana", "apple", "orange", "lemon", "lime", "grapes", "pomegranate",
        "coconut", "papaya", "pineapple", "guava", "watermelon", "melon", "berry",
        "strawberry", "kiwi", "tamarind", "imli", "amla", "jamun", "chikoo", "sitaphal"
    ],
    "dairy": [
        "milk", "cream", "butter", "ghee", "cheese", "paneer", "curd", "yogurt",
        "dahi", "khoya", "mawa", "malai", "chenna"
    ],
    "grains": [
        "rice", "wheat", "flour", "atta", "maida", "rava", "suji", "semolina",
        "oats", "barley", "millet", "bajra", "jowar", "ragi", "bread", "roti",
        "chapati", "paratha", "naan", "puri", "bhature", "poha", "sabudana",
        "tapioca", "vermicelli", "seviyan", "besan", "gram flour"
    ],
    "pulses": [
        "dal", "lentil", "chana", "chickpea", "rajma", "kidney bean", "urad",
        "moong", "toor", "arhar", "masoor", "chole", "lobiya", "black gram",
        "red gram", "bengal gram", "split pea"
    ],
    "spices": [
        "cumin", "jeera", "coriander", "dhania", "turmeric", "haldi", "pepper",
        "chili", "mirch", "mustard", "rai", "fenugreek", "methi", "cardamom",
        "elaichi", "clove", "laung", "cinnamon", "dalchini", "nutmeg", "jaiphal",
        "mace", "javitri", "bay leaf", "tej patta", "fennel", "saunf", "star anise",
        "carom", "ajwain", "asafoetida", "hing", "saffron", "kesar", "masala",
        "garam", "curry", "tandoori", "chaat", "kashmiri", "paprika"
    ],
    "oils": [
        "oil", "ghee", "butter", "vanaspati", "dalda", "shortening"
    ],
    "meat": [
        "chicken", "mutton", "lamb", "goat", "beef", "pork", "keema", "mince"
    ],
    "seafood": [
        "fish", "prawn", "shrimp", "crab", "lobster", "squid", "mussel", "clam"
    ],
    "nuts": [
        "cashew", "kaju", "almond", "badam", "walnut", "akhrot", "pistachio",
        "pista", "peanut", "mungfali", "coconut"
    ],
    "sweeteners": [
        "sugar", "jaggery", "gur", "honey", "shahad", "syrup", "molasses"
    ],
}


def get_source_client() -> AsyncClient:
    """Get Firestore client for the source (khanakyabanega) project."""
    creds_path = Path(__file__).parent.parent / SOURCE_CREDENTIALS_PATH
    if not creds_path.exists():
        raise FileNotFoundError(f"Source credentials not found at: {creds_path}")
    return AsyncClient.from_service_account_json(str(creds_path))


def get_target_client() -> AsyncClient:
    """Get Firestore client for the target (RasoiAI) project."""
    creds_path = Path(__file__).parent.parent / TARGET_CREDENTIALS_PATH
    if not creds_path.exists():
        raise FileNotFoundError(f"Target credentials not found at: {creds_path}")
    return AsyncClient.from_service_account_json(str(creds_path))


def parse_ingredient_string(ingredient_str: str) -> dict:
    """Parse a free-form ingredient string into structured data.

    Examples:
        "250 grams paneer" -> {name: "paneer", quantity: 250, unit: "grams", ...}
        "1/2 cup milk" -> {name: "milk", quantity: 0.5, unit: "cup", ...}
        "salt to taste" -> {name: "salt", quantity: 1, unit: "to taste", ...}
    """
    # Clean up HTML entities
    ingredient_str = ingredient_str.replace("&amp;", "&").replace("&#039;", "'")
    ingredient_str = ingredient_str.replace("&quot;", '"').replace("&lt;", "<")
    ingredient_str = ingredient_str.replace("&gt;", ">")

    # Remove content in parentheses (optional notes)
    notes_match = re.search(r'\(([^)]+)\)', ingredient_str)
    notes = notes_match.group(1) if notes_match else None
    ingredient_str = re.sub(r'\([^)]+\)', '', ingredient_str).strip()

    # Common fractions
    fractions = {
        "1/2": 0.5, "1/4": 0.25, "3/4": 0.75, "1/3": 0.33,
        "2/3": 0.67, "1/8": 0.125, "3/8": 0.375,
        "½": 0.5, "¼": 0.25, "¾": 0.75, "⅓": 0.33, "⅔": 0.67,
    }

    # Try to extract quantity
    quantity = 1.0
    unit = "unit"
    name = ingredient_str.strip()

    # Pattern: number + optional fraction + unit + name
    # e.g., "2 1/2 cups flour" or "250 grams paneer"
    patterns = [
        # "2 cups flour" or "2.5 cups flour"
        r'^([\d.]+)\s*(cups?|tablespoons?|tbsp|teaspoons?|tsp|grams?|g|kg|ml|liters?|l|pieces?|bunch|medium|large|small|inch|inches|cm|cloves?|sprigs?|pinch|handfuls?)\s+(.+)',
        # "1/2 cup milk"
        r'^([½¼¾⅓⅔]|\d+/\d+)\s*(cups?|tablespoons?|tbsp|teaspoons?|tsp|grams?|g|kg|ml|liters?|l|pieces?|bunch|medium|large|small|inch|inches|cm|cloves?|sprigs?|pinch|handfuls?)\s+(.+)',
        # "2 1/2 cups flour" (mixed number)
        r'^(\d+)\s+([½¼¾⅓⅔]|\d+/\d+)\s*(cups?|tablespoons?|tbsp|teaspoons?|tsp|grams?|g|kg|ml|liters?|l|pieces?|bunch|medium|large|small|inch|inches|cm|cloves?|sprigs?|pinch|handfuls?)\s+(.+)',
    ]

    for pattern in patterns:
        match = re.match(pattern, ingredient_str, re.IGNORECASE)
        if match:
            groups = match.groups()
            if len(groups) == 3:
                qty_str, unit, name = groups
                if qty_str in fractions:
                    quantity = fractions[qty_str]
                else:
                    try:
                        quantity = float(qty_str)
                    except:
                        quantity = 1.0
            elif len(groups) == 4:
                whole, frac, unit, name = groups
                try:
                    quantity = float(whole) + fractions.get(frac, 0)
                except:
                    quantity = 1.0
            break
    else:
        # Try simple number at start
        num_match = re.match(r'^([\d.]+)\s+(.+)', ingredient_str)
        if num_match:
            try:
                quantity = float(num_match.group(1))
                name = num_match.group(2)
            except:
                pass

    # Normalize unit
    unit_mapping = {
        "tbsp": "tablespoon", "tablespoons": "tablespoon",
        "tsp": "teaspoon", "teaspoons": "teaspoon",
        "cups": "cup", "g": "grams", "kg": "kilograms",
        "ml": "milliliters", "l": "liters", "liters": "liter",
        "pieces": "piece", "cloves": "clove", "sprigs": "sprig",
        "handfuls": "handful",
    }
    unit = unit_mapping.get(unit.lower(), unit.lower())

    # Categorize ingredient
    category = "other"
    name_lower = name.lower()
    for cat, keywords in INGREDIENT_CATEGORIES.items():
        if any(kw in name_lower for kw in keywords):
            category = cat
            break

    return {
        "id": str(uuid.uuid4()),
        "name": name.strip(" -,"),
        "quantity": quantity,
        "unit": unit,
        "category": category,
        "notes": notes,
        "is_optional": notes is not None and "optional" in notes.lower() if notes else False,
    }


def transform_recipe(source: dict) -> dict:
    """Transform a source recipe to RasoiAI schema."""
    recipe_id = source.get("id") or str(uuid.uuid4())
    now = datetime.now(timezone.utc)

    # Map cuisine
    cuisine_raw = source.get("cuisine", "").lower().strip()
    cuisine_type = CUISINE_MAPPING.get(cuisine_raw, "north")

    # Map diet type to dietary tags
    diet_raw = source.get("dietType", "").lower().strip()
    dietary_tags = DIET_MAPPING.get(diet_raw, ["vegetarian"])

    # Map meal types
    meal_types_raw = source.get("mealTypes", [])
    meal_types = []
    for mt in meal_types_raw:
        mapped = MEAL_TYPE_MAPPING.get(mt.lower().strip(), None)
        if mapped and mapped not in meal_types:
            meal_types.append(mapped)
    if not meal_types:
        meal_types = ["lunch"]  # Default

    # Map difficulty
    difficulty_raw = source.get("difficulty", "medium").lower().strip()
    difficulty = DIFFICULTY_MAPPING.get(difficulty_raw, "medium")

    # Parse ingredients
    ingredients_raw = source.get("ingredients", [])
    ingredients = []
    for i, ing_str in enumerate(ingredients_raw):
        if isinstance(ing_str, str):
            parsed = parse_ingredient_string(ing_str)
            parsed["order"] = i
            ingredients.append(parsed)

    # Parse instructions
    instructions_raw = source.get("instructions", [])
    instructions = []
    for i, inst_str in enumerate(instructions_raw):
        if isinstance(inst_str, str):
            # Clean HTML entities
            inst_str = inst_str.replace("&amp;", "&").replace("&#039;", "'")
            inst_str = inst_str.replace("&quot;", '"')

            instructions.append({
                "id": str(uuid.uuid4()),
                "step_number": i + 1,
                "instruction": inst_str.strip(),
                "duration_minutes": None,  # Not available in source
                "timer_required": False,
                "tips": None,
            })

    # Build nutrition (only calories available in source)
    calories = source.get("calories", 0)
    nutrition = {
        "calories": calories,
        "protein_grams": 0,  # Not available
        "carbohydrates_grams": 0,  # Not available
        "fat_grams": 0,  # Not available
        "fiber_grams": 0,  # Not available
    }

    # Calculate total time (source only has cookingTimeMinutes)
    cook_time = source.get("cookingTimeMinutes", 30)
    prep_time = max(10, cook_time // 3)  # Estimate prep as 1/3 of cook time

    # Determine flags
    is_quick = cook_time <= 30
    is_fasting = "jain" in dietary_tags or any(
        "fast" in tag.lower() for tag in dietary_tags
    )

    return {
        "id": recipe_id,
        "name": source.get("title", "Unknown Recipe"),
        "description": source.get("description", ""),
        "image_url": source.get("imageUrl"),
        "source_url": source.get("sourceUrl"),
        "cuisine_type": cuisine_type,
        "meal_types": meal_types,
        "dietary_tags": dietary_tags,
        "course_type": "main",  # Default
        "prep_time_minutes": prep_time,
        "cook_time_minutes": cook_time,
        "total_time_minutes": prep_time + cook_time,
        "servings": source.get("servings", 4),
        "difficulty_level": difficulty,
        "is_festive": False,
        "is_fasting_friendly": is_fasting,
        "is_quick_meal": is_quick,
        "is_kid_friendly": False,
        "is_active": True,
        "ingredients": ingredients,
        "instructions": instructions,
        "nutrition": nutrition,
        "created_at": now,
        "updated_at": now,
        "imported_from": "khanakyabanega",
        "original_created_at": source.get("createdAt"),
    }


async def import_recipes(
    limit: Optional[int] = None,
    dry_run: bool = True,
    offset: int = 0,
):
    """Import recipes from source to target database."""
    print("="*60)
    print("Recipe Import: khanakyabanega -> RasoiAI")
    print("="*60)
    print(f"Mode: {'DRY RUN (no changes)' if dry_run else 'LIVE IMPORT'}")
    print(f"Limit: {limit if limit else 'ALL'}")
    print(f"Offset: {offset}")
    print()

    # Connect to source
    try:
        source_client = get_source_client()
        print("[OK] Connected to source (khanakyabanega)")
    except Exception as e:
        print(f"[ERROR] Failed to connect to source: {e}")
        return

    # Connect to target (only if not dry run)
    target_client = None
    if not dry_run:
        try:
            target_client = get_target_client()
            print("[OK] Connected to target (RasoiAI)")
        except Exception as e:
            print(f"[ERROR] Failed to connect to target: {e}")
            return

    # Fetch recipes from source
    print("\nFetching recipes from source...")
    source_recipes = []
    source_collection = source_client.collection("recipes")

    query = source_collection.order_by("createdAt")
    if offset > 0:
        query = query.offset(offset)
    if limit:
        query = query.limit(limit)

    async for doc in query.stream():
        data = doc.to_dict()
        data["id"] = doc.id
        source_recipes.append(data)

    print(f"Fetched {len(source_recipes)} recipes from source")

    if not source_recipes:
        print("No recipes to import.")
        return

    # Transform recipes
    print("\nTransforming recipes...")
    transformed = []
    errors = []

    for i, source in enumerate(source_recipes):
        try:
            recipe = transform_recipe(source)
            transformed.append(recipe)
        except Exception as e:
            errors.append({
                "id": source.get("id"),
                "title": source.get("title"),
                "error": str(e),
            })

    print(f"Successfully transformed: {len(transformed)}")
    if errors:
        print(f"Errors: {len(errors)}")
        for err in errors[:5]:
            print(f"  - {err['title']}: {err['error']}")

    # Preview first few
    print("\n" + "-"*40)
    print("SAMPLE TRANSFORMED RECIPES")
    print("-"*40)
    for recipe in transformed[:3]:
        print(f"\n[{recipe['cuisine_type'].upper()}] {recipe['name']}")
        print(f"  Meal types: {recipe['meal_types']}")
        print(f"  Diet: {recipe['dietary_tags']}")
        print(f"  Time: {recipe['total_time_minutes']} min")
        print(f"  Ingredients: {len(recipe['ingredients'])}")
        print(f"  Instructions: {len(recipe['instructions'])} steps")
        if recipe['ingredients']:
            print(f"    First ingredient: {recipe['ingredients'][0]['quantity']} {recipe['ingredients'][0]['unit']} {recipe['ingredients'][0]['name']}")

    if dry_run:
        print("\n" + "="*60)
        print("DRY RUN COMPLETE - No changes made")
        print("="*60)
        print(f"\nTo import, run: python scripts/import_recipes_from_kkb.py --limit {limit or 'ALL'}")
        return

    # Import to target
    print("\n" + "-"*40)
    print("IMPORTING TO RASOIAI...")
    print("-"*40)

    target_collection = target_client.collection("recipes")
    imported = 0
    skipped = 0

    for recipe in transformed:
        try:
            # Check if already exists
            existing = await target_collection.document(recipe["id"]).get()
            if existing.exists:
                skipped += 1
                continue

            # Insert
            await target_collection.document(recipe["id"]).set(recipe)
            imported += 1

            if imported % 100 == 0:
                print(f"  Imported {imported} recipes...")

        except Exception as e:
            errors.append({
                "id": recipe.get("id"),
                "name": recipe.get("name"),
                "error": str(e),
            })

    print(f"\nImport complete!")
    print(f"  Imported: {imported}")
    print(f"  Skipped (existing): {skipped}")
    if errors:
        print(f"  Errors: {len(errors)}")


def main():
    parser = argparse.ArgumentParser(description="Import recipes from khanakyabanega to RasoiAI")
    parser.add_argument("--dry-run", action="store_true", help="Preview only, don't import")
    parser.add_argument("--limit", type=int, help="Maximum recipes to import")
    parser.add_argument("--offset", type=int, default=0, help="Skip first N recipes")
    parser.add_argument("--all", action="store_true", help="Import all recipes")

    args = parser.parse_args()

    if args.all:
        limit = None
    else:
        limit = args.limit or 10  # Default to 10 for safety

    # Default to dry-run if not explicitly importing all
    dry_run = args.dry_run or (not args.all and args.limit is None)

    asyncio.run(import_recipes(
        limit=limit,
        dry_run=dry_run,
        offset=args.offset,
    ))


if __name__ == "__main__":
    main()
