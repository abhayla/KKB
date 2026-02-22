"""Recipe creation service — creates real Recipe records from AI-generated meal items.

During meal generation, Gemini returns recipe names with ingredients and nutrition.
This service creates full Recipe records (with RecipeIngredient, RecipeInstruction,
RecipeNutrition) so Android's Recipe Detail screen can display them.

Deduplicates by normalized name to avoid creating duplicates across generations.
"""

import logging
import uuid
from typing import Optional

from sqlalchemy import func, insert, select  # noqa: F401 - insert used in bulk ops
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.recipe import (
    Recipe,
    RecipeIngredient,
    RecipeInstruction,
    RecipeNutrition,
)

logger = logging.getLogger(__name__)


def _normalize_name(name: str) -> str:
    """Normalize recipe name for deduplication."""
    return name.strip().lower()


def _generate_placeholder_instructions(recipe_name: str, prep_time: int) -> list[dict]:
    """Generate placeholder cooking instructions when AI doesn't provide them."""
    return [
        {
            "step_number": 1,
            "instruction": f"Gather and prepare all ingredients for {recipe_name}.",
            "duration_minutes": max(5, prep_time // 4),
            "tips": "Wash vegetables and measure spices beforehand.",
        },
        {
            "step_number": 2,
            "instruction": "Heat oil or ghee in a pan on medium flame.",
            "duration_minutes": 2,
            "tips": None,
        },
        {
            "step_number": 3,
            "instruction": f"Cook the {recipe_name} following traditional preparation method.",
            "duration_minutes": max(10, prep_time // 2),
            "tips": "Adjust spices to taste.",
        },
        {
            "step_number": 4,
            "instruction": "Serve hot with accompaniments.",
            "duration_minutes": 2,
            "tips": "Garnish with fresh coriander if desired.",
        },
    ]


async def find_or_create_recipe(
    db: AsyncSession,
    recipe_name: str,
    prep_time: int = 30,
    dietary_tags: Optional[list[str]] = None,
    category: str = "other",
    calories: int = 0,
    cuisine_type: str = "north",
    meal_type: str = "lunch",
    ingredients: Optional[list[dict]] = None,
    nutrition: Optional[dict] = None,
    instructions: Optional[list[dict]] = None,
    family_size: int = 4,
) -> str:
    """Find existing recipe by normalized name, or create a new one.

    Returns the recipe ID (UUID string).
    """
    normalized = _normalize_name(recipe_name)

    # Check for existing recipe
    result = await db.execute(
        select(Recipe.id).where(
            func.lower(func.trim(Recipe.name)) == normalized,
            Recipe.is_active == True,
        )
    )
    existing_id = result.scalar_one_or_none()
    if existing_id:
        return existing_id

    # Create new recipe
    recipe_id = str(uuid.uuid4())
    recipe = Recipe(
        id=recipe_id,
        name=recipe_name.strip(),
        cuisine_type=cuisine_type,
        meal_types=[meal_type],
        dietary_tags=dietary_tags or ["vegetarian"],
        category=category,
        prep_time_minutes=prep_time,
        cook_time_minutes=prep_time,
        total_time_minutes=prep_time,
        servings=family_size,
        difficulty_level="medium" if prep_time > 30 else "easy",
        is_quick_meal=prep_time <= 20,
    )
    db.add(recipe)

    # Create ingredients
    if ingredients:
        for idx, ing in enumerate(ingredients):
            db.add(
                RecipeIngredient(
                    id=str(uuid.uuid4()),
                    recipe_id=recipe_id,
                    name=ing.get("name", "Ingredient"),
                    quantity=float(ing.get("quantity", 1)),
                    unit=ing.get("unit", "piece"),
                    category=ing.get("category", "other"),
                    order=idx,
                )
            )

    # Create instructions
    instruction_data = instructions or _generate_placeholder_instructions(
        recipe_name, prep_time
    )
    for step in instruction_data:
        db.add(
            RecipeInstruction(
                id=str(uuid.uuid4()),
                recipe_id=recipe_id,
                step_number=step.get("step_number", 1),
                instruction=step.get("instruction", ""),
                duration_minutes=step.get("duration_minutes"),
                tips=step.get("tips"),
            )
        )

    # Create nutrition
    if nutrition:
        db.add(
            RecipeNutrition(
                id=str(uuid.uuid4()),
                recipe_id=recipe_id,
                calories=calories or nutrition.get("calories", 0),
                protein_grams=float(nutrition.get("protein_g", 0)),
                carbohydrates_grams=float(nutrition.get("carbs_g", 0)),
                fat_grams=float(nutrition.get("fat_g", 0)),
                fiber_grams=float(nutrition.get("fiber_g", 0)),
            )
        )
    elif calories > 0:
        db.add(
            RecipeNutrition(
                id=str(uuid.uuid4()),
                recipe_id=recipe_id,
                calories=calories,
                protein_grams=0.0,
                carbohydrates_grams=0.0,
                fat_grams=0.0,
                fiber_grams=0.0,
            )
        )

    return recipe_id


async def _bulk_find_existing_recipes(
    db: AsyncSession,
    normalized_names: set[str],
) -> dict[str, str]:
    """Find existing recipes by normalized names in a single query.

    Args:
        db: Async database session
        normalized_names: Set of normalized (lowercase, stripped) recipe names

    Returns:
        Dict mapping normalized_name -> recipe_id for all found recipes
    """
    if not normalized_names:
        return {}

    result = await db.execute(
        select(Recipe.id, func.lower(func.trim(Recipe.name))).where(
            func.lower(func.trim(Recipe.name)).in_(normalized_names),
            Recipe.is_active == True,
        )
    )
    rows = result.all()
    return {row[1]: row[0] for row in rows}


async def create_recipes_for_meal_plan(
    db: AsyncSession,
    generated_plan,
    cuisine_type: str = "north",
    family_size: int = 4,
) -> dict[str, str]:
    """Create Recipe records for all items in a generated meal plan.

    Uses bulk operations for performance:
    1. Collects all unique recipe names from the plan
    2. Single SELECT to find existing recipes
    3. Bulk INSERT for new recipes, ingredients, instructions, nutrition

    Mutates item.recipe_id in place with the real UUID.

    Args:
        db: Async database session
        generated_plan: GeneratedMealPlan dataclass from ai_meal_service
        cuisine_type: User's cuisine preference
        family_size: Number of family members

    Returns:
        Dict mapping normalized_name -> recipe_id for all created/found recipes
    """
    # --- Pass 1: Collect all unique items by normalized name ---
    unique_items: dict[str, tuple] = {}  # normalized_name -> (item, slot_name)
    all_items: list[tuple] = []  # (normalized_name, item)

    for day in generated_plan.days:
        for slot_name in ["breakfast", "lunch", "dinner", "snacks"]:
            items = getattr(day, slot_name, [])
            for item in items:
                normalized = _normalize_name(item.recipe_name)
                all_items.append((normalized, item))
                if normalized not in unique_items:
                    unique_items[normalized] = (item, slot_name)

    if not unique_items:
        return {}

    # --- Pass 2: Bulk lookup existing recipes (1 SELECT instead of ~28) ---
    recipe_map: dict[str, str] = await _bulk_find_existing_recipes(
        db, set(unique_items.keys())
    )
    existing_count = len(recipe_map)

    # --- Pass 3: Prepare bulk insert data for new recipes ---
    all_recipes: list[dict] = []
    all_ingredients: list[dict] = []
    all_instructions: list[dict] = []
    all_nutrition: list[dict] = []

    for normalized, (item, slot_name) in unique_items.items():
        if normalized in recipe_map:
            continue  # Already exists in DB

        recipe_id = str(uuid.uuid4())
        recipe_map[normalized] = recipe_id
        prep_time = item.prep_time_minutes
        dietary_tags = item.dietary_tags or ["vegetarian"]
        calories = item.calories

        # Recipe record
        all_recipes.append(
            {
                "id": recipe_id,
                "name": item.recipe_name.strip(),
                "cuisine_type": cuisine_type,
                "meal_types": dietary_tags,  # JSONList column — will be serialized
                "dietary_tags": dietary_tags,
                "category": getattr(item, "category", "other"),
                "prep_time_minutes": prep_time,
                "cook_time_minutes": prep_time,
                "total_time_minutes": prep_time,
                "servings": family_size,
                "difficulty_level": "medium" if prep_time > 30 else "easy",
                "is_quick_meal": prep_time <= 20,
            }
        )

        # Fix meal_types to actual meal type (not dietary tags)
        all_recipes[-1]["meal_types"] = [slot_name]

        # Ingredients
        ingredients = item.ingredients
        if ingredients:
            for idx, ing in enumerate(ingredients):
                all_ingredients.append(
                    {
                        "id": str(uuid.uuid4()),
                        "recipe_id": recipe_id,
                        "name": ing.get("name", "Ingredient"),
                        "quantity": float(ing.get("quantity", 1)),
                        "unit": ing.get("unit", "piece"),
                        "category": ing.get("category", "other"),
                        "order": idx,
                    }
                )

        # Instructions
        instruction_data = getattr(
            item, "instructions", None
        ) or _generate_placeholder_instructions(item.recipe_name, prep_time)
        for step in instruction_data:
            all_instructions.append(
                {
                    "id": str(uuid.uuid4()),
                    "recipe_id": recipe_id,
                    "step_number": step.get("step_number", 1),
                    "instruction": step.get("instruction", ""),
                    "duration_minutes": step.get("duration_minutes"),
                    "tips": step.get("tips"),
                }
            )

        # Nutrition
        nutrition = item.nutrition
        if nutrition:
            all_nutrition.append(
                {
                    "id": str(uuid.uuid4()),
                    "recipe_id": recipe_id,
                    "calories": calories or nutrition.get("calories", 0),
                    "protein_grams": float(nutrition.get("protein_g", 0)),
                    "carbohydrates_grams": float(nutrition.get("carbs_g", 0)),
                    "fat_grams": float(nutrition.get("fat_g", 0)),
                    "fiber_grams": float(nutrition.get("fiber_g", 0)),
                }
            )
        elif calories > 0:
            all_nutrition.append(
                {
                    "id": str(uuid.uuid4()),
                    "recipe_id": recipe_id,
                    "calories": calories,
                    "protein_grams": 0.0,
                    "carbohydrates_grams": 0.0,
                    "fat_grams": 0.0,
                    "fiber_grams": 0.0,
                }
            )

    # --- Pass 4: Execute bulk inserts (4 statements instead of ~450 db.add) ---
    new_count = len(all_recipes)
    if all_recipes:
        await db.execute(insert(Recipe), all_recipes)
    if all_ingredients:
        await db.execute(insert(RecipeIngredient), all_ingredients)
    if all_instructions:
        await db.execute(insert(RecipeInstruction), all_instructions)
    if all_nutrition:
        await db.execute(insert(RecipeNutrition), all_nutrition)

    await db.commit()

    # --- Pass 5: Assign recipe_ids to all items ---
    reused_count = 0
    for normalized, item in all_items:
        rid = recipe_map.get(normalized)
        if rid:
            if item.recipe_id == rid:
                reused_count += 1
            item.recipe_id = rid

    logger.info(
        f"Recipe creation complete: {new_count} new, {existing_count} existing, "
        f"{len(all_items) - len(unique_items)} reused from in-plan dedup"
    )
    return recipe_map
