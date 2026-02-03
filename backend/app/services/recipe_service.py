"""Recipe service for recipe operations."""

import uuid
from typing import Optional

from sqlalchemy import or_, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.core.exceptions import NotFoundError
from app.models.recipe import Recipe, RecipeIngredient, RecipeInstruction, RecipeNutrition
from app.schemas.recipe import (
    IngredientDto,
    InstructionDto,
    NutritionDto,
    RecipeResponse,
    RecipeSearchParams,
)


def build_recipe_response(recipe: Recipe, scale_factor: float = 1.0) -> RecipeResponse:
    """Build RecipeResponse from Recipe model.

    Args:
        recipe: Recipe model with relations loaded
        scale_factor: Factor to scale ingredient quantities

    Returns:
        RecipeResponse schema
    """
    # Build ingredients
    ingredients = []
    for ing in recipe.ingredients:
        scaled_qty = ing.quantity * scale_factor
        # Format quantity as string (remove decimals if whole number)
        qty_str = f"{scaled_qty:.1f}".rstrip("0").rstrip(".")
        ingredients.append(
            IngredientDto(
                id=str(ing.id),
                name=ing.name,
                quantity=qty_str,
                unit=ing.unit,
                category=ing.category,
                is_optional=ing.is_optional,
                substitute_for=ing.notes,
            )
        )

    # Build instructions
    instructions = [
        InstructionDto(
            step_number=inst.step_number,
            instruction=inst.instruction,
            duration_minutes=inst.duration_minutes,
            timer_required=inst.timer_required,
            tips=inst.tips,
        )
        for inst in recipe.instructions
    ]

    # Build nutrition
    nutrition = None
    if recipe.nutrition:
        nutrition = NutritionDto(
            calories=int(recipe.nutrition.calories * scale_factor),
            protein=int(recipe.nutrition.protein_grams * scale_factor),
            carbohydrates=int(recipe.nutrition.carbohydrates_grams * scale_factor),
            fat=int(recipe.nutrition.fat_grams * scale_factor),
            fiber=int(recipe.nutrition.fiber_grams * scale_factor),
            sugar=int((recipe.nutrition.sugar_grams or 0) * scale_factor),
            sodium=int((recipe.nutrition.sodium_mg or 0) * scale_factor),
        )

    return RecipeResponse(
        id=str(recipe.id),
        name=recipe.name,
        description=recipe.description or "",
        image_url=recipe.image_url,
        prep_time_minutes=recipe.prep_time_minutes,
        cook_time_minutes=recipe.cook_time_minutes,
        servings=int(recipe.servings * scale_factor),
        difficulty=recipe.difficulty_level or "medium",
        cuisine_type=recipe.cuisine_type,
        meal_types=recipe.meal_types,
        dietary_tags=recipe.dietary_tags,
        ingredients=ingredients,
        instructions=instructions,
        nutrition=nutrition,
    )


async def get_recipe_by_id(
    db: AsyncSession,
    recipe_id: str,
) -> RecipeResponse:
    """Get recipe by ID.

    Args:
        db: Database session
        recipe_id: Recipe UUID

    Returns:
        RecipeResponse

    Raises:
        NotFoundError: If recipe not found
    """
    try:
        recipe_uuid = uuid.UUID(recipe_id)
    except ValueError:
        raise NotFoundError("Invalid recipe ID")

    result = await db.execute(
        select(Recipe)
        .options(
            selectinload(Recipe.ingredients),
            selectinload(Recipe.instructions),
            selectinload(Recipe.nutrition),
        )
        .where(Recipe.id == recipe_uuid, Recipe.is_active == True)
    )
    recipe = result.scalar_one_or_none()

    if not recipe:
        raise NotFoundError("Recipe not found")

    return build_recipe_response(recipe)


async def scale_recipe(
    db: AsyncSession,
    recipe_id: str,
    target_servings: int,
) -> RecipeResponse:
    """Scale recipe to target servings.

    Args:
        db: Database session
        recipe_id: Recipe UUID
        target_servings: Target number of servings

    Returns:
        Scaled RecipeResponse

    Raises:
        NotFoundError: If recipe not found
    """
    try:
        recipe_uuid = uuid.UUID(recipe_id)
    except ValueError:
        raise NotFoundError("Invalid recipe ID")

    result = await db.execute(
        select(Recipe)
        .options(
            selectinload(Recipe.ingredients),
            selectinload(Recipe.instructions),
            selectinload(Recipe.nutrition),
        )
        .where(Recipe.id == recipe_uuid, Recipe.is_active == True)
    )
    recipe = result.scalar_one_or_none()

    if not recipe:
        raise NotFoundError("Recipe not found")

    scale_factor = target_servings / recipe.servings
    return build_recipe_response(recipe, scale_factor)


async def search_recipes(
    db: AsyncSession,
    params: RecipeSearchParams,
) -> list[RecipeResponse]:
    """Search recipes with filters.

    Args:
        db: Database session
        params: Search parameters

    Returns:
        List of matching recipes
    """
    query = select(Recipe).options(
        selectinload(Recipe.ingredients),
        selectinload(Recipe.instructions),
        selectinload(Recipe.nutrition),
    ).where(Recipe.is_active == True)

    # Text search
    if params.q:
        search_term = f"%{params.q}%"
        query = query.where(
            or_(
                Recipe.name.ilike(search_term),
                Recipe.description.ilike(search_term),
            )
        )

    # Cuisine filter
    if params.cuisine:
        query = query.where(Recipe.cuisine_type == params.cuisine)

    # Dietary filter
    if params.dietary:
        query = query.where(Recipe.dietary_tags.contains([params.dietary]))

    # Note: meal_type filter intentionally removed
    # Users should be able to add any recipe to any meal slot
    # (e.g., biryani for breakfast, chai for dinner)
    # The meal_types field is kept for informational/suggestion purposes only

    # Pagination
    offset = (params.page - 1) * params.limit
    query = query.offset(offset).limit(params.limit)

    result = await db.execute(query)
    recipes = result.scalars().all()

    return [build_recipe_response(recipe) for recipe in recipes]


async def get_recipes_by_ids(
    db: AsyncSession,
    recipe_ids: list[str],
) -> list[Recipe]:
    """Get multiple recipes by IDs.

    Args:
        db: Database session
        recipe_ids: List of recipe UUIDs

    Returns:
        List of Recipe models
    """
    uuids = []
    for rid in recipe_ids:
        try:
            uuids.append(uuid.UUID(rid))
        except ValueError:
            continue

    if not uuids:
        return []

    result = await db.execute(
        select(Recipe)
        .options(
            selectinload(Recipe.ingredients),
            selectinload(Recipe.instructions),
            selectinload(Recipe.nutrition),
        )
        .where(Recipe.id.in_(uuids), Recipe.is_active == True)
    )
    return list(result.scalars().all())
