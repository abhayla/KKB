"""Recipe creation service — creates real Recipe records from AI-generated meal items.

During meal generation, Gemini returns recipe names with ingredients and nutrition.
This service creates full Recipe records (with RecipeIngredient, RecipeInstruction,
RecipeNutrition) so Android's Recipe Detail screen can display them.

Deduplicates by normalized name to avoid creating duplicates across generations.
"""

import logging
import uuid
from typing import Optional

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.recipe import Recipe, RecipeIngredient, RecipeInstruction, RecipeNutrition

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
            db.add(RecipeIngredient(
                id=str(uuid.uuid4()),
                recipe_id=recipe_id,
                name=ing.get("name", "Ingredient"),
                quantity=float(ing.get("quantity", 1)),
                unit=ing.get("unit", "piece"),
                category=ing.get("category", "other"),
                order=idx,
            ))

    # Create instructions
    instruction_data = instructions or _generate_placeholder_instructions(recipe_name, prep_time)
    for step in instruction_data:
        db.add(RecipeInstruction(
            id=str(uuid.uuid4()),
            recipe_id=recipe_id,
            step_number=step.get("step_number", 1),
            instruction=step.get("instruction", ""),
            duration_minutes=step.get("duration_minutes"),
            tips=step.get("tips"),
        ))

    # Create nutrition
    if nutrition:
        db.add(RecipeNutrition(
            id=str(uuid.uuid4()),
            recipe_id=recipe_id,
            calories=calories or nutrition.get("calories", 0),
            protein_grams=float(nutrition.get("protein_g", 0)),
            carbohydrates_grams=float(nutrition.get("carbs_g", 0)),
            fat_grams=float(nutrition.get("fat_g", 0)),
            fiber_grams=float(nutrition.get("fiber_g", 0)),
        ))
    elif calories > 0:
        db.add(RecipeNutrition(
            id=str(uuid.uuid4()),
            recipe_id=recipe_id,
            calories=calories,
            protein_grams=0.0,
            carbohydrates_grams=0.0,
            fat_grams=0.0,
            fiber_grams=0.0,
        ))

    return recipe_id


async def create_recipes_for_meal_plan(
    db: AsyncSession,
    generated_plan,
    cuisine_type: str = "north",
    family_size: int = 4,
) -> dict[str, str]:
    """Create Recipe records for all items in a generated meal plan.

    Iterates all days/slots/items, calls find_or_create_recipe for each,
    and mutates item.recipe_id in place with the real UUID.

    Args:
        db: Async database session
        generated_plan: GeneratedMealPlan dataclass from ai_meal_service
        cuisine_type: User's cuisine preference
        family_size: Number of family members

    Returns:
        Dict mapping recipe_name -> recipe_id for all created/found recipes
    """
    recipe_map: dict[str, str] = {}
    created_count = 0
    reused_count = 0

    for day in generated_plan.days:
        for slot_name in ["breakfast", "lunch", "dinner", "snacks"]:
            items = getattr(day, slot_name, [])
            for item in items:
                try:
                    normalized = _normalize_name(item.recipe_name)

                    # Check in-memory cache first (same name in multiple slots)
                    if normalized in recipe_map:
                        item.recipe_id = recipe_map[normalized]
                        reused_count += 1
                        continue

                    recipe_id = await find_or_create_recipe(
                        db=db,
                        recipe_name=item.recipe_name,
                        prep_time=item.prep_time_minutes,
                        dietary_tags=item.dietary_tags,
                        category=getattr(item, "category", "other"),
                        calories=item.calories,
                        cuisine_type=cuisine_type,
                        meal_type=slot_name,
                        ingredients=item.ingredients,
                        nutrition=item.nutrition,
                        instructions=getattr(item, "instructions", None),
                        family_size=family_size,
                    )

                    item.recipe_id = recipe_id
                    recipe_map[normalized] = recipe_id
                    created_count += 1

                except Exception as e:
                    logger.error(
                        f"Failed to create recipe for '{item.recipe_name}': {e}",
                        exc_info=True,
                    )
                    # Non-critical: item keeps its original recipe_id

    await db.commit()

    logger.info(
        f"Recipe creation complete: {created_count} created/found, "
        f"{reused_count} reused from cache"
    )
    return recipe_map
