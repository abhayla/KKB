"""Recipe service for recipe operations."""

import uuid
from typing import Any, Optional

from sqlalchemy import func, or_, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.core.exceptions import NotFoundError
from app.models.recipe import Recipe, RecipeIngredient, RecipeInstruction, RecipeNutrition, RecipeRating
from app.schemas.recipe import (
    IngredientDto,
    InstructionDto,
    NutritionDto,
    RecipeRatingResponse,
    RecipeResponse,
    RecipeSearchParams,
)


def build_recipe_response(
    recipe: Recipe,
    scale_factor: float = 1.0,
    rating_summary: Optional[dict] = None,
) -> RecipeResponse:
    """Build RecipeResponse from Recipe model.

    Args:
        recipe: Recipe model with relations loaded
        scale_factor: Factor to scale ingredient quantities
        rating_summary: Optional dict with keys average_rating (Optional[float]),
            rating_count (int), user_rating (Optional[float]). When None, the
            response reports no ratings (null/0/null).

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

    avg_rating: Optional[float] = None
    count: int = 0
    user_rating_value: Optional[float] = None
    if rating_summary is not None:
        avg_rating = rating_summary.get("average_rating")
        count = int(rating_summary.get("rating_count") or 0)
        user_rating_value = rating_summary.get("user_rating")

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
        average_rating=avg_rating,
        rating_count=count,
        user_rating=user_rating_value,
    )


async def _get_rating_summary(
    db: AsyncSession,
    recipe_id: str,
    user_id: Optional[str] = None,
) -> dict:
    """Compute rating aggregates for a recipe.

    Returns a dict with keys:
      - average_rating (Optional[float]): AVG across all ratings, or None when no ratings
      - rating_count (int): total number of ratings
      - user_rating (Optional[float]): the given user's rating, or None when user_id
        is not provided or the user has not rated this recipe
    """
    agg_row = (
        await db.execute(
            select(
                func.avg(RecipeRating.rating),
                func.count(RecipeRating.id),
            ).where(RecipeRating.recipe_id == recipe_id)
        )
    ).one()
    avg_value, count_value = agg_row
    avg_float: Optional[float] = float(avg_value) if avg_value is not None else None

    user_rating_value: Optional[float] = None
    if user_id is not None:
        user_rating_value = (
            await db.execute(
                select(RecipeRating.rating).where(
                    RecipeRating.recipe_id == recipe_id,
                    RecipeRating.user_id == user_id,
                )
            )
        ).scalar_one_or_none()

    return {
        "average_rating": avg_float,
        "rating_count": int(count_value or 0),
        "user_rating": user_rating_value,
    }


async def get_recipe_by_id(
    db: AsyncSession,
    recipe_id: str,
    user_id: Optional[str] = None,
) -> RecipeResponse:
    """Get recipe by ID.

    Args:
        db: Database session
        recipe_id: Recipe UUID
        user_id: Optional requesting user UUID — when provided, the response
            populates user_rating with that user's own rating (or null if
            they have not rated this recipe).

    Returns:
        RecipeResponse with average_rating, rating_count, and (when user_id
        is provided) user_rating populated.

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
        .where(Recipe.id == str(recipe_uuid), Recipe.is_active == True)
    )
    recipe = result.scalar_one_or_none()

    if not recipe:
        raise NotFoundError("Recipe not found")

    rating_summary = await _get_rating_summary(db, str(recipe_uuid), user_id)
    return build_recipe_response(recipe, rating_summary=rating_summary)


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
        .where(Recipe.id == str(recipe_uuid), Recipe.is_active == True)
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

    # Text search (name, description, and ingredient names)
    if params.q:
        search_term = f"%{params.q}%"
        query = query.where(
            or_(
                Recipe.name.ilike(search_term),
                Recipe.description.ilike(search_term),
                Recipe.id.in_(
                    select(RecipeIngredient.recipe_id).where(
                        RecipeIngredient.name.ilike(search_term)
                    )
                ),
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

    # Deterministic ordering for stable pagination results
    query = query.order_by(Recipe.name)

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
    valid_ids = []
    for rid in recipe_ids:
        try:
            uuid.UUID(rid)  # validate format
            valid_ids.append(rid)
        except ValueError:
            continue

    if not valid_ids:
        return []

    result = await db.execute(
        select(Recipe)
        .options(
            selectinload(Recipe.ingredients),
            selectinload(Recipe.instructions),
            selectinload(Recipe.nutrition),
        )
        .where(Recipe.id.in_(valid_ids), Recipe.is_active == True)
    )
    return list(result.scalars().all())


async def rate_recipe(
    db: AsyncSession,
    recipe_id: str,
    user_id: str,
    rating: float,
    feedback: Optional[str] = None,
) -> RecipeRatingResponse:
    """Rate a recipe (upsert: create or update existing rating).

    Args:
        db: Database session
        recipe_id: Recipe UUID
        user_id: User UUID
        rating: Rating value (1.0-5.0)
        feedback: Optional feedback text

    Returns:
        RecipeRatingResponse

    Raises:
        NotFoundError: If recipe not found
    """
    # Validate UUID format
    try:
        uuid.UUID(recipe_id)
    except ValueError:
        raise NotFoundError("Recipe not found")

    # Verify recipe exists (compare as string for SQLite compatibility)
    result = await db.execute(
        select(Recipe).where(Recipe.id == recipe_id, Recipe.is_active == True)
    )
    if not result.scalar_one_or_none():
        raise NotFoundError("Recipe not found")

    # Check for existing rating by this user on this recipe
    result = await db.execute(
        select(RecipeRating).where(
            RecipeRating.recipe_id == recipe_id,
            RecipeRating.user_id == user_id,
        )
    )
    existing = result.scalar_one_or_none()

    if existing:
        existing.rating = rating
        existing.feedback = feedback
        await db.commit()
        await db.refresh(existing)
        return RecipeRatingResponse(
            id=existing.id,
            recipe_id=existing.recipe_id,
            rating=existing.rating,
            feedback=existing.feedback,
            created_at=existing.created_at,
            updated_at=existing.updated_at,
        )
    else:
        new_rating = RecipeRating(
            recipe_id=recipe_id,
            user_id=user_id,
            rating=rating,
            feedback=feedback,
        )
        db.add(new_rating)
        await db.commit()
        await db.refresh(new_rating)
        return RecipeRatingResponse(
            id=new_rating.id,
            recipe_id=new_rating.recipe_id,
            rating=new_rating.rating,
            feedback=new_rating.feedback,
            created_at=new_rating.created_at,
            updated_at=new_rating.updated_at,
        )


async def suggest_from_pantry(
    db: AsyncSession,
    ingredients: list[str],
    limit: int = 10,
) -> list[dict[str, Any]]:
    """Suggest recipes based on available pantry ingredients.

    Matches pantry ingredients against recipe ingredient lists,
    scoring by match percentage. Returns top recipes sorted by match.

    Args:
        db: Database session
        ingredients: List of pantry ingredient names
        limit: Maximum number of suggestions to return

    Returns:
        List of dicts with recipe, match_percentage, and missing_ingredients
    """
    if not ingredients:
        return []

    # Normalize ingredient names for matching
    normalized = [ing.strip().lower() for ing in ingredients if ing.strip()]
    if not normalized:
        return []

    # Get all active recipes with ingredients
    result = await db.execute(
        select(Recipe)
        .options(
            selectinload(Recipe.ingredients),
            selectinload(Recipe.instructions),
            selectinload(Recipe.nutrition),
        )
        .where(Recipe.is_active == True)
    )
    recipes = result.scalars().all()

    suggestions = []
    for recipe in recipes:
        if not recipe.ingredients:
            continue

        recipe_ingredient_names = [
            ing.name.strip().lower() for ing in recipe.ingredients
        ]
        total_ingredients = len(recipe_ingredient_names)

        # Count matches (partial matching - pantry item contained in recipe ingredient or vice versa)
        matched = 0
        missing = []
        for recipe_ing in recipe_ingredient_names:
            found = any(
                pantry_ing in recipe_ing or recipe_ing in pantry_ing
                for pantry_ing in normalized
            )
            if found:
                matched += 1
            else:
                missing.append(recipe_ing)

        if matched == 0:
            continue

        match_pct = round((matched / total_ingredients) * 100, 1)

        suggestions.append({
            "recipe": build_recipe_response(recipe),
            "match_percentage": match_pct,
            "matched_count": matched,
            "total_ingredients": total_ingredients,
            "missing_ingredients": missing,
        })

    # Sort by match percentage descending, then by fewer missing ingredients
    suggestions.sort(key=lambda x: (-x["match_percentage"], len(x["missing_ingredients"])))

    return suggestions[:limit]
