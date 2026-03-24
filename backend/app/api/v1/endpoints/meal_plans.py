"""Meal plan endpoints using Firestore repositories."""

import asyncio
import logging
import random
import uuid
from datetime import date, datetime, timedelta
from typing import Any

from fastapi import APIRouter, HTTPException, Request

from app.api.deps import CurrentUser
from app.config import settings
from app.core.rate_limit import limiter
from app.core.exceptions import NotFoundError
from app.repositories.meal_plan_repository import MealPlanRepository
from app.repositories.recipe_repository import RecipeRepository
from app.schemas.meal_plan import (
    GenerateMealPlanRequest,
    MealPlanResponse,
    MealPlanDayDto,
    MealsByTypeDto,
    MealItemDto,
    FestivalDto,
    SwapMealRequest,
)
from app.services.ai_meal_service import AIMealService

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/meal-plans", tags=["meal-plans"])


# ==============================================================================
# RESPONSE BUILDING HELPERS
# ==============================================================================


def _build_response_from_firestore(plan: dict[str, Any]) -> MealPlanResponse:
    """Build MealPlanResponse from Firestore document."""
    days = []
    for day_data in plan.get("days", []):
        meals = day_data.get("meals", {})

        def _build_meal_items(meal_list: list) -> list[MealItemDto]:
            items = []
            for i, m in enumerate(meal_list or []):
                items.append(
                    MealItemDto(
                        id=m.get("id", str(uuid.uuid4())),
                        recipe_id=m.get("recipe_id", ""),
                        recipe_name=m.get("recipe_name", ""),
                        recipe_image_url=m.get("recipe_image_url"),
                        prep_time_minutes=m.get("prep_time_minutes", 30),
                        calories=m.get("calories", 0),
                        is_locked=m.get("is_locked", False),
                        order=i,
                        dietary_tags=m.get("dietary_tags", []),
                    )
                )
            return items

        festival = None
        if day_data.get("festival"):
            f = day_data["festival"]
            if isinstance(f, dict):
                festival = FestivalDto(
                    id=f.get("id", ""),
                    name=f.get("name", ""),
                    is_fasting_day=f.get("is_fasting_day", False),
                    suggested_dishes=f.get("suggested_dishes"),
                )
            elif isinstance(f, str):
                festival = FestivalDto(
                    id="",
                    name=f,
                    is_fasting_day=False,
                    suggested_dishes=None,
                )

        days.append(
            MealPlanDayDto(
                date=day_data.get("date", ""),
                day_name=day_data.get("day_name", ""),
                meals=MealsByTypeDto(
                    breakfast=_build_meal_items(meals.get("breakfast", [])),
                    lunch=_build_meal_items(meals.get("lunch", [])),
                    dinner=_build_meal_items(meals.get("dinner", [])),
                    snacks=_build_meal_items(meals.get("snacks", [])),
                ),
                festival=festival,
            )
        )

    created_at = plan.get("created_at")
    updated_at = plan.get("updated_at")

    # Convert date objects to ISO strings for response
    week_start = plan.get("week_start_date", "")
    week_end = plan.get("week_end_date", "")
    if isinstance(week_start, date):
        week_start = week_start.isoformat()
    if isinstance(week_end, date):
        week_end = week_end.isoformat()

    return MealPlanResponse(
        id=plan.get("id", ""),
        week_start_date=week_start,
        week_end_date=week_end,
        days=days,
        created_at=(
            created_at.isoformat()
            if isinstance(created_at, datetime)
            else str(created_at)
        ),
        updated_at=(
            updated_at.isoformat()
            if isinstance(updated_at, datetime)
            else str(updated_at)
        ),
    )


@router.post("/generate", response_model=MealPlanResponse)
@limiter.limit("500/hour" if settings.debug else "5/hour")
async def generate(
    request: Request,
    gen_request: GenerateMealPlanRequest,
    current_user: CurrentUser,
) -> MealPlanResponse:
    """Generate a new meal plan with paired recipes.

    Creates a personalized 7-day meal plan based on user preferences,
    dietary restrictions, recipe rules, and available recipes in Firestore.

    Each meal slot contains 2 complementary items (e.g., Dal + Rice) by default.

    Enforces:
    - EXCLUDE rules (never include certain ingredients/recipes)
    - INCLUDE rules (must include certain items at specified frequency, paired with complements)
    - Allergies (always excluded)
    - Dislikes (always excluded)
    - Cooking time limits (weekday vs weekend, busy days)
    - Pairing rules from config (by cuisine and meal type)
    """
    import time

    from app.services.generation_tracker import (  # noqa: F811
        MealGenerationContext,
    )

    try:
        t_start = time.monotonic()
        user_id = current_user.id
        logger.info(f"Generating meal plan for user {user_id}")

        # Parse week start date
        try:
            week_start = date.fromisoformat(gen_request.week_start_date)
        except ValueError:
            week_start = date.today()
            week_start = week_start - timedelta(days=week_start.weekday())

        # Create tracking context
        trigger_source = request.headers.get("X-Trigger-Source", "api")
        gen_context = MealGenerationContext(
            user_id=user_id,
            week_start_date=week_start.isoformat(),
            trigger_source=trigger_source,
            start_time=t_start,
        )

        # Generate meal plan using the AI service (with 180s timeout)
        # With response_json_schema, single attempt ~30-40s. 180s allows
        # for 1 retry + exponential backoff (3 max retries × ~40s + delays).
        ai_service = AIMealService()
        try:
            generated_plan = await asyncio.wait_for(
                ai_service.generate_meal_plan(
                    user_id=user_id,
                    week_start_date=week_start,
                    context=gen_context,
                ),
                timeout=180,
            )
        except asyncio.TimeoutError:
            logger.error(f"Meal generation timed out after 180s for user {user_id}")
            gen_context.error_message = "Timeout after 180s"
            gen_context.ai_done_time = time.monotonic()
            try:
                from app.services.generation_tracker import (
                    emit_structured_log as _emit,
                )

                _emit(gen_context)
            except Exception:
                pass
            raise HTTPException(
                status_code=504,
                detail="Meal generation timed out. Please try again.",
            )
        t_ai_done = time.monotonic()
        gen_context.ai_done_time = t_ai_done

        # Reuse preferences cached by AIMealService (avoids duplicate DB call)
        from app.db.postgres import async_session_maker
        from app.services.recipe_creation_service import create_recipes_for_meal_plan

        cached_prefs = ai_service.last_preferences
        cuisine_type = "north"
        if cached_prefs and cached_prefs.cuisine_preferences:
            cuisine_type = cached_prefs.cuisine_preferences[0]
        family_size = cached_prefs.family_size if cached_prefs else 4

        # Create real Recipe records for all AI-generated items
        # This replaces "AI_GENERATED" recipe_ids with real UUIDs
        # Retry once on failure — recipe creation is critical for Recipe Detail screen
        recipe_creation_success = False
        for attempt in range(1, 3):
            try:
                async with async_session_maker() as recipe_db:
                    await create_recipes_for_meal_plan(
                        db=recipe_db,
                        generated_plan=generated_plan,
                        cuisine_type=cuisine_type,
                        family_size=family_size,
                    )
                recipe_creation_success = True
                break
            except Exception as e:
                logger.error(
                    f"Recipe creation attempt {attempt}/2 failed for user {user_id}: {e}",
                    exc_info=True,
                )

        # Background enrichment: replace placeholder instructions with AI-generated ones
        if recipe_creation_success:
            from app.services.recipe_enrichment_service import enrich_recipe_instructions

            async def _enrich_recipes():
                for day in generated_plan.days:
                    for slot in ["breakfast", "lunch", "dinner", "snacks"]:
                        for item in getattr(day, slot, []):
                            if item.recipe_id and item.recipe_id not in ("AI_GENERATED", "GENERIC"):
                                try:
                                    async with async_session_maker() as enrich_db:
                                        await enrich_recipe_instructions(enrich_db, item.recipe_id)
                                except Exception as e:
                                    logger.warning(f"Enrichment failed for {item.recipe_id}: {e}")

            asyncio.create_task(_enrich_recipes())
            logger.info("Background recipe enrichment task started")

        # Audit: count items with real UUIDs vs still "AI_GENERATED"
        total_items = 0
        real_ids = 0
        for day in generated_plan.days:
            for slot in ["breakfast", "lunch", "dinner", "snacks"]:
                for item in getattr(day, slot, []):
                    total_items += 1
                    if item.recipe_id and item.recipe_id not in (
                        "AI_GENERATED",
                        "GENERIC",
                    ):
                        real_ids += 1
        t_recipes_done = time.monotonic()

        if not recipe_creation_success:
            logger.error(
                f"Recipe creation FAILED after 2 attempts. "
                f"{real_ids}/{total_items} items have real recipe IDs"
            )
        else:
            logger.info(
                f"Recipe creation OK: {real_ids}/{total_items} items have real recipe IDs"
            )

        # Convert to repository format
        # Repository expects lists of items per meal type
        days = []
        for day in generated_plan.days:
            days.append(
                {
                    "date": day.date,
                    "day_name": day.day_name,
                    "festival": day.festival,
                    "breakfast": [_meal_item_to_dict(item) for item in day.breakfast],
                    "lunch": [_meal_item_to_dict(item) for item in day.lunch],
                    "dinner": [_meal_item_to_dict(item) for item in day.dinner],
                    "snacks": [_meal_item_to_dict(item) for item in day.snacks],
                }
            )

        # Save to PostgreSQL (deactivate old + create new in single session)
        meal_plan_repo = MealPlanRepository()

        plan_data = {
            "user_id": user_id,
            "week_start_date": date.fromisoformat(generated_plan.week_start_date),
            "week_end_date": date.fromisoformat(generated_plan.week_end_date),
            "days": days,
            "rules_applied": generated_plan.rules_applied,
        }

        created_plan = await meal_plan_repo.create_and_deactivate_old(plan_data)
        t_save_done = time.monotonic()
        logger.info(f"Created meal plan {created_plan.get('id')} for user {user_id}")

        # Catalog AI-generated recipes in background (fire-and-forget)
        # This is non-critical — failures are logged but don't affect the response
        async def _background_catalog():
            try:
                from app.services.ai_recipe_catalog_service import (
                    catalog_recipes as catalog_recipes_fn,
                )

                async with async_session_maker() as catalog_db:
                    await catalog_recipes_fn(
                        db=catalog_db,
                        user_id=user_id,
                        generated_plan={"days": days},
                        cuisine_type=cuisine_type,
                    )
            except Exception as e:
                logger.warning(f"Background catalog failed: {e}")

        asyncio.create_task(_background_catalog())

        t_end = time.monotonic()
        logger.info(
            f"PERF meal-gen user={user_id}: "
            f"total={t_end - t_start:.1f}s, "
            f"ai={t_ai_done - t_start:.1f}s, "
            f"recipes={t_recipes_done - t_ai_done:.1f}s, "
            f"save={t_save_done - t_recipes_done:.1f}s, "
            f"post-ai={t_end - t_ai_done:.1f}s"
        )

        # Update tracking context with final state
        gen_context.meal_plan_id = created_plan.get("id")
        gen_context.items_generated = total_items
        gen_context.save_done_time = t_save_done
        gen_context.success = True

        # Build response and capture for tracking
        response = _build_response_from_firestore(created_plan)
        gen_context.client_response_data = response.model_dump()

        # Write tracking data to per-call JSON file
        from app.services.generation_tracker import emit_structured_log

        emit_structured_log(gen_context)

        return response
    except HTTPException:
        raise  # Re-raise HTTP exceptions (like 504 timeout) as-is
    except Exception as e:
        logger.error("Error generating meal plan", exc_info=True)

        # Track failed generation
        try:
            gen_context.error_message = str(e)
            gen_context.ai_done_time = gen_context.ai_done_time or time.monotonic()

            from app.services.generation_tracker import (
                emit_structured_log as emit_log,
            )

            emit_log(gen_context)
        except Exception:
            logger.warning("Failed to track generation error", exc_info=True)

        raise HTTPException(
            status_code=500,
            detail="Meal plan generation failed. Please try again.",
        )


def _meal_item_to_dict(item) -> dict:
    """Convert MealItem dataclass to dictionary."""
    d = {
        "id": item.id,
        "recipe_id": item.recipe_id,
        "recipe_name": item.recipe_name,
        "recipe_image_url": item.recipe_image_url,
        "prep_time_minutes": item.prep_time_minutes,
        "calories": item.calories,
        "is_locked": item.is_locked,
        "dietary_tags": item.dietary_tags,
    }
    # Include rich data for catalog (ingredients/nutrition may be None)
    if hasattr(item, "ingredients") and item.ingredients:
        d["ingredients"] = item.ingredients
    if hasattr(item, "nutrition") and item.nutrition:
        d["nutrition"] = item.nutrition
    if hasattr(item, "category"):
        d["category"] = item.category
    return d


@router.get("/current", response_model=MealPlanResponse)
async def get_current(
    current_user: CurrentUser,
) -> MealPlanResponse:
    """Get the current week's meal plan from Firestore."""
    user_id = current_user.id

    meal_plan_repo = MealPlanRepository()
    plan = await meal_plan_repo.get_current_for_user(user_id)

    if not plan:
        raise NotFoundError("No meal plan found for current week")

    return _build_response_from_firestore(plan)


@router.get("/{plan_id}", response_model=MealPlanResponse)
async def get_by_id(
    plan_id: str,
    current_user: CurrentUser,
) -> MealPlanResponse:
    """Get a specific meal plan by ID from Firestore."""
    user_id = current_user.id

    meal_plan_repo = MealPlanRepository()
    plan = await meal_plan_repo.get_by_id(plan_id)

    if not plan:
        raise NotFoundError("Meal plan not found")

    if plan.get("user_id") != user_id:
        raise NotFoundError("Meal plan not found")

    return _build_response_from_firestore(plan)


@router.post("/{plan_id}/items/{item_id}/swap", response_model=MealPlanResponse)
async def swap_item(
    plan_id: str,
    item_id: str,
    request: SwapMealRequest,
    current_user: CurrentUser,
) -> MealPlanResponse:
    """Swap a meal item with an alternative recipe.

    Can optionally specify a specific recipe or let the system choose randomly.
    """
    user_id = current_user.id

    meal_plan_repo = MealPlanRepository()
    plan = await meal_plan_repo.get_by_id(plan_id)

    if not plan or plan.get("user_id") != user_id:
        raise NotFoundError("Meal plan not found")

    # Find the item to swap
    days = plan.get("days", [])
    found = False

    for day_idx, day in enumerate(days):
        for meal_type in ["breakfast", "lunch", "dinner", "snacks"]:
            meals = day.get("meals", {}).get(meal_type, [])
            for meal_idx, meal in enumerate(meals):
                if meal.get("id") == item_id:
                    if meal.get("is_locked"):
                        raise NotFoundError("Cannot swap a locked meal")

                    # Get new recipe
                    recipe_repo = RecipeRepository()
                    if request.specific_recipe_id:
                        new_recipe = await recipe_repo.get_by_id(
                            request.specific_recipe_id
                        )
                    else:
                        recipes = await recipe_repo.search(
                            meal_type=meal_type, limit=20
                        )
                        exclude_ids = set(request.exclude_recipe_ids or [])
                        exclude_ids.add(meal.get("recipe_id", ""))
                        recipes = [r for r in recipes if r.get("id") not in exclude_ids]
                        new_recipe = random.choice(recipes) if recipes else None

                    if not new_recipe:
                        raise NotFoundError("No alternative recipe found")

                    # Update the meal
                    days[day_idx]["meals"][meal_type][meal_idx] = {
                        "id": str(uuid.uuid4()),
                        "recipe_id": new_recipe.get("id", ""),
                        "recipe_name": new_recipe.get("name", ""),
                        "recipe_image_url": new_recipe.get("image_url"),
                        "prep_time_minutes": new_recipe.get("prep_time_minutes", 30),
                        "calories": (
                            new_recipe.get("nutrition", {}).get("calories", 0)
                            if new_recipe.get("nutrition")
                            else 0
                        ),
                        "is_locked": False,
                        "dietary_tags": new_recipe.get("dietary_tags", []),
                    }
                    found = True
                    break
            if found:
                break
        if found:
            break

    if not found:
        raise NotFoundError("Meal item not found")

    updated_plan = await meal_plan_repo.update(plan_id, {"days": days})
    return _build_response_from_firestore(updated_plan)


@router.put("/{plan_id}/items/{item_id}/lock", response_model=MealPlanResponse)
async def toggle_lock(
    plan_id: str,
    item_id: str,
    current_user: CurrentUser,
) -> MealPlanResponse:
    """Toggle the lock status of a meal item.

    Locked meals won't be changed when regenerating the plan.
    """
    user_id = current_user.id

    meal_plan_repo = MealPlanRepository()
    plan = await meal_plan_repo.get_by_id(plan_id)

    if not plan or plan.get("user_id") != user_id:
        raise NotFoundError("Meal plan not found")

    # Find and toggle lock on item
    days = plan.get("days", [])
    found = False

    for day_idx, day in enumerate(days):
        for meal_type in ["breakfast", "lunch", "dinner", "snacks"]:
            meals = day.get("meals", {}).get(meal_type, [])
            for meal_idx, meal in enumerate(meals):
                if meal.get("id") == item_id:
                    days[day_idx]["meals"][meal_type][meal_idx]["is_locked"] = (
                        not meal.get("is_locked", False)
                    )
                    found = True
                    break
            if found:
                break
        if found:
            break

    if not found:
        raise NotFoundError("Meal item not found")

    updated_plan = await meal_plan_repo.update(plan_id, {"days": days})
    return _build_response_from_firestore(updated_plan)


@router.delete("/{plan_id}/items/{item_id}", response_model=MealPlanResponse)
async def remove_item(
    plan_id: str,
    item_id: str,
    current_user: CurrentUser,
) -> MealPlanResponse:
    """Remove a meal item from the plan.

    Locked meals cannot be removed.
    """
    user_id = current_user.id

    meal_plan_repo = MealPlanRepository()
    plan = await meal_plan_repo.get_by_id(plan_id)

    if not plan or plan.get("user_id") != user_id:
        raise NotFoundError("Meal plan not found")

    # Find and remove the item
    days = plan.get("days", [])
    found = False

    for day_idx, day in enumerate(days):
        for meal_type in ["breakfast", "lunch", "dinner", "snacks"]:
            meals = day.get("meals", {}).get(meal_type, [])
            for meal_idx, meal in enumerate(meals):
                if meal.get("id") == item_id:
                    if meal.get("is_locked"):
                        raise NotFoundError("Cannot remove a locked meal")
                    # Remove the item from the list
                    days[day_idx]["meals"][meal_type].pop(meal_idx)
                    found = True
                    break
            if found:
                break
        if found:
            break

    if not found:
        raise NotFoundError("Meal item not found")

    updated_plan = await meal_plan_repo.update(plan_id, {"days": days})
    logger.info(f"Removed meal item {item_id} from plan {plan_id}")
    return _build_response_from_firestore(updated_plan)
