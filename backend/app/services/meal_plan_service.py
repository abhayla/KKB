"""Meal plan service for meal planning operations."""

import uuid
from datetime import date, datetime, timedelta, timezone

from sqlalchemy import and_, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.core.exceptions import NotFoundError
from app.models.meal_plan import MealPlan, MealPlanItem
from app.models.recipe import Recipe
from app.models.user import User
from app.schemas.meal_plan import (
    FestivalDto,
    GenerateMealPlanRequest,
    MealItemDto,
    MealPlanDayDto,
    MealPlanResponse,
    MealsByTypeDto,
    SwapMealRequest,
)


def _get_day_name(d: date) -> str:
    """Get day name from date."""
    return d.strftime("%A")


def _build_meal_plan_response(
    meal_plan: MealPlan,
    items_by_day: dict[date, list[MealPlanItem]],
) -> MealPlanResponse:
    """Build MealPlanResponse from MealPlan model.

    Args:
        meal_plan: MealPlan model
        items_by_day: Items grouped by date

    Returns:
        MealPlanResponse schema
    """
    days = []

    # Generate all 7 days
    current_date = meal_plan.week_start_date
    while current_date <= meal_plan.week_end_date:
        day_items = items_by_day.get(current_date, [])

        # Group items by meal type
        meals_by_type = {
            "breakfast": [],
            "lunch": [],
            "dinner": [],
            "snacks": [],
        }

        festival = None
        for item in day_items:
            if item.festival_name and not festival:
                festival = FestivalDto(
                    id=str(item.id),
                    name=item.festival_name,
                    is_fasting_day=False,
                    suggested_dishes=None,
                )

            if item.recipe:
                meal_dto = MealItemDto(
                    id=str(item.id),
                    recipe_id=str(item.recipe_id),
                    recipe_name=item.recipe.name,
                    recipe_image_url=item.recipe.image_url,
                    prep_time_minutes=item.recipe.prep_time_minutes,
                    calories=item.recipe.nutrition.calories if item.recipe.nutrition else 0,
                    is_locked=item.is_locked,
                    order=0,
                    dietary_tags=item.recipe.dietary_tags,
                )
                meal_type = item.meal_type.lower()
                if meal_type in meals_by_type:
                    meals_by_type[meal_type].append(meal_dto)

        # Set order based on position in list
        for meal_list in meals_by_type.values():
            for i, meal in enumerate(meal_list):
                meal.order = i

        days.append(
            MealPlanDayDto(
                date=current_date.isoformat(),
                day_name=_get_day_name(current_date),
                meals=MealsByTypeDto(**meals_by_type),
                festival=festival,
            )
        )

        current_date += timedelta(days=1)

    return MealPlanResponse(
        id=str(meal_plan.id),
        week_start_date=meal_plan.week_start_date.isoformat(),
        week_end_date=meal_plan.week_end_date.isoformat(),
        days=days,
        created_at=meal_plan.created_at.isoformat(),
        updated_at=meal_plan.updated_at.isoformat(),
    )


async def get_current_meal_plan(
    db: AsyncSession,
    user: User,
) -> MealPlanResponse:
    """Get current week's meal plan for user.

    Args:
        db: Database session
        user: Current user

    Returns:
        MealPlanResponse

    Raises:
        NotFoundError: If no current meal plan exists
    """
    # Calculate current week's start (Monday)
    today = date.today()
    week_start = today - timedelta(days=today.weekday())

    result = await db.execute(
        select(MealPlan)
        .options(
            selectinload(MealPlan.items).selectinload(MealPlanItem.recipe).selectinload(Recipe.nutrition)
        )
        .where(
            MealPlan.user_id == user.id,
            MealPlan.week_start_date == week_start,
            MealPlan.is_active == True,
        )
    )
    meal_plan = result.scalar_one_or_none()

    if not meal_plan:
        raise NotFoundError("No meal plan found for current week")

    # Group items by date
    items_by_day = {}
    for item in meal_plan.items:
        if item.date not in items_by_day:
            items_by_day[item.date] = []
        items_by_day[item.date].append(item)

    return _build_meal_plan_response(meal_plan, items_by_day)


async def get_meal_plan_by_id(
    db: AsyncSession,
    user: User,
    plan_id: str,
) -> MealPlanResponse:
    """Get meal plan by ID.

    Args:
        db: Database session
        user: Current user
        plan_id: Meal plan UUID

    Returns:
        MealPlanResponse

    Raises:
        NotFoundError: If meal plan not found
    """
    try:
        plan_uuid = uuid.UUID(plan_id)
    except ValueError:
        raise NotFoundError("Invalid meal plan ID")

    result = await db.execute(
        select(MealPlan)
        .options(
            selectinload(MealPlan.items).selectinload(MealPlanItem.recipe).selectinload(Recipe.nutrition)
        )
        .where(
            MealPlan.id == plan_uuid,
            MealPlan.user_id == user.id,
        )
    )
    meal_plan = result.scalar_one_or_none()

    if not meal_plan:
        raise NotFoundError("Meal plan not found")

    # Group items by date
    items_by_day = {}
    for item in meal_plan.items:
        if item.date not in items_by_day:
            items_by_day[item.date] = []
        items_by_day[item.date].append(item)

    return _build_meal_plan_response(meal_plan, items_by_day)


async def swap_meal_item(
    db: AsyncSession,
    user: User,
    plan_id: str,
    item_id: str,
    request: SwapMealRequest,
) -> MealPlanResponse:
    """Swap a meal item with a new recipe.

    Args:
        db: Database session
        user: Current user
        plan_id: Meal plan UUID
        item_id: Meal plan item UUID
        request: Swap request with optional specific recipe

    Returns:
        Updated MealPlanResponse

    Raises:
        NotFoundError: If meal plan or item not found
    """
    try:
        plan_uuid = uuid.UUID(plan_id)
        item_uuid = uuid.UUID(item_id)
    except ValueError:
        raise NotFoundError("Invalid ID")

    # Get meal plan
    result = await db.execute(
        select(MealPlan)
        .options(
            selectinload(MealPlan.items).selectinload(MealPlanItem.recipe).selectinload(Recipe.nutrition)
        )
        .where(
            MealPlan.id == plan_uuid,
            MealPlan.user_id == user.id,
        )
    )
    meal_plan = result.scalar_one_or_none()

    if not meal_plan:
        raise NotFoundError("Meal plan not found")

    # Find the item to swap
    item_to_swap = None
    for item in meal_plan.items:
        if item.id == item_uuid:
            item_to_swap = item
            break

    if not item_to_swap:
        raise NotFoundError("Meal item not found")

    if item_to_swap.is_locked:
        raise NotFoundError("Cannot swap a locked meal")

    # Get new recipe
    if request.specific_recipe_id:
        # Use specified recipe
        try:
            new_recipe_uuid = uuid.UUID(request.specific_recipe_id)
        except ValueError:
            raise NotFoundError("Invalid recipe ID")

        recipe_result = await db.execute(
            select(Recipe)
            .options(selectinload(Recipe.nutrition))
            .where(Recipe.id == new_recipe_uuid, Recipe.is_active == True)
        )
        new_recipe = recipe_result.scalar_one_or_none()
        if not new_recipe:
            raise NotFoundError("Recipe not found")
    else:
        # Find alternative recipe with same meal type
        exclude_ids = [item_to_swap.recipe_id]
        if request.exclude_recipe_ids:
            for rid in request.exclude_recipe_ids:
                try:
                    exclude_ids.append(uuid.UUID(rid))
                except ValueError:
                    continue

        # Note: meal_type filter intentionally removed
        # Users should be able to swap to any recipe regardless of meal type
        recipe_result = await db.execute(
            select(Recipe)
            .options(selectinload(Recipe.nutrition))
            .where(
                Recipe.is_active == True,
                ~Recipe.id.in_(exclude_ids),
            )
            .limit(1)
        )
        new_recipe = recipe_result.scalar_one_or_none()

        if not new_recipe:
            raise NotFoundError("No alternative recipe found")

    # Update item
    item_to_swap.recipe_id = new_recipe.id
    item_to_swap.recipe = new_recipe

    await db.commit()

    # Return updated meal plan
    items_by_day = {}
    for item in meal_plan.items:
        if item.date not in items_by_day:
            items_by_day[item.date] = []
        items_by_day[item.date].append(item)

    return _build_meal_plan_response(meal_plan, items_by_day)


async def lock_meal_item(
    db: AsyncSession,
    user: User,
    plan_id: str,
    item_id: str,
) -> MealPlanResponse:
    """Toggle lock status on a meal item.

    Args:
        db: Database session
        user: Current user
        plan_id: Meal plan UUID
        item_id: Meal plan item UUID

    Returns:
        Updated MealPlanResponse

    Raises:
        NotFoundError: If meal plan or item not found
    """
    try:
        plan_uuid = uuid.UUID(plan_id)
        item_uuid = uuid.UUID(item_id)
    except ValueError:
        raise NotFoundError("Invalid ID")

    # Get meal plan
    result = await db.execute(
        select(MealPlan)
        .options(
            selectinload(MealPlan.items).selectinload(MealPlanItem.recipe).selectinload(Recipe.nutrition)
        )
        .where(
            MealPlan.id == plan_uuid,
            MealPlan.user_id == user.id,
        )
    )
    meal_plan = result.scalar_one_or_none()

    if not meal_plan:
        raise NotFoundError("Meal plan not found")

    # Find and toggle lock on item
    for item in meal_plan.items:
        if item.id == item_uuid:
            item.is_locked = not item.is_locked
            break
    else:
        raise NotFoundError("Meal item not found")

    await db.commit()

    # Return updated meal plan
    items_by_day = {}
    for item in meal_plan.items:
        if item.date not in items_by_day:
            items_by_day[item.date] = []
        items_by_day[item.date].append(item)

    return _build_meal_plan_response(meal_plan, items_by_day)


async def create_meal_plan(
    db: AsyncSession,
    user: User,
    week_start: date,
    items: list[dict],
) -> MealPlan:
    """Create a new meal plan.

    Args:
        db: Database session
        user: Current user
        week_start: Start date of the week (Monday)
        items: List of meal plan items

    Returns:
        Created MealPlan model
    """
    week_end = week_start + timedelta(days=6)

    # Deactivate existing plan for this week
    result = await db.execute(
        select(MealPlan).where(
            MealPlan.user_id == user.id,
            MealPlan.week_start_date == week_start,
        )
    )
    existing_plan = result.scalar_one_or_none()
    if existing_plan:
        existing_plan.is_active = False

    # Create new plan
    meal_plan = MealPlan(
        user_id=user.id,
        week_start_date=week_start,
        week_end_date=week_end,
        is_active=True,
    )
    db.add(meal_plan)
    await db.flush()

    # Add items
    for item_data in items:
        item = MealPlanItem(
            meal_plan_id=meal_plan.id,
            recipe_id=item_data["recipe_id"],
            date=item_data["date"],
            meal_type=item_data["meal_type"],
            servings=item_data.get("servings", 2),
            is_locked=item_data.get("is_locked", False),
            festival_name=item_data.get("festival_name"),
        )
        db.add(item)

    await db.commit()
    await db.refresh(meal_plan)

    return meal_plan
