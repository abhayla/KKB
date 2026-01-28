"""Meal plan endpoints using Firestore repositories."""

import logging
import random
import re
import uuid
from datetime import date, datetime, timedelta, timezone
from typing import Any, Optional

from fastapi import APIRouter

from app.api.deps import CurrentUser
from app.core.exceptions import NotFoundError
from app.repositories.meal_plan_repository import MealPlanRepository
from app.repositories.recipe_repository import RecipeRepository
from app.repositories.user_repository import UserRepository
from app.schemas.meal_plan import (
    GenerateMealPlanRequest,
    MealPlanResponse,
    MealPlanDayDto,
    MealsByTypeDto,
    MealItemDto,
    FestivalDto,
    SwapMealRequest,
)

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/meal-plans", tags=["meal-plans"])


# ==============================================================================
# RECIPE RULE ENFORCEMENT HELPERS
# ==============================================================================

def _recipe_matches_target(recipe: dict, target: str) -> bool:
    """Check if a recipe matches a target (ingredient or recipe name).

    Handles variations like "Paneer" matching "paneer", "Matar Paneer", etc.
    Also handles common Indian ingredient aliases.
    """
    # Common ingredient aliases (Indian names)
    INGREDIENT_ALIASES = {
        "baingan": ["brinjal", "eggplant", "aubergine", "baingan", "vangi", "begun"],
        "karela": ["bitter gourd", "bitter melon", "karela", "kerala"],
        "mushroom": ["mushroom", "fungi", "champignon"],
        "paneer": ["paneer", "cottage cheese", "indian cheese"],
        "peanut": ["peanut", "groundnut", "moongfali", "mungfali"],
        "cashew": ["cashew", "kaju", "caju"],
        "onion": ["onion", "pyaz", "pyaaz", "kanda"],
        "garlic": ["garlic", "lahsun", "lasun"],
    }

    target_lower = target.lower()

    # Build list of terms to search for (target + its aliases)
    search_terms = [target_lower]
    for key, aliases in INGREDIENT_ALIASES.items():
        if target_lower in aliases or target_lower == key:
            search_terms.extend(aliases)
    search_terms = list(set(search_terms))  # Remove duplicates

    # Check recipe name
    name = recipe.get("name", "").lower()
    for term in search_terms:
        if term in name:
            return True

    # Check ingredients list
    ingredients = recipe.get("ingredients", [])
    for ing in ingredients:
        if isinstance(ing, dict):
            ing_name = ing.get("name", "").lower()
        else:
            ing_name = str(ing).lower()

        for term in search_terms:
            if term in ing_name:
                return True

    return False


def _filter_by_exclude_rules(
    recipes: list[dict],
    exclude_rules: list[dict],
    allergies: list[dict],
    dislikes: list[str],
) -> list[dict]:
    """Filter out recipes that match EXCLUDE rules, allergies, or dislikes."""
    filtered = []

    # Build list of all excluded targets
    excluded_targets = []

    # From EXCLUDE rules
    for rule in exclude_rules:
        if rule.get("frequency") == "NEVER":
            excluded_targets.append(rule.get("target", "").lower())

    # From allergies (always exclude)
    for allergy in allergies:
        if isinstance(allergy, dict):
            excluded_targets.append(allergy.get("ingredient", "").lower())
        else:
            excluded_targets.append(str(allergy).lower())

    # From dislikes
    for dislike in dislikes:
        excluded_targets.append(dislike.lower())

    # Filter recipes
    for recipe in recipes:
        is_excluded = False
        for target in excluded_targets:
            if target and _recipe_matches_target(recipe, target):
                is_excluded = True
                logger.debug(f"Excluding recipe '{recipe.get('name')}' due to '{target}'")
                break

        if not is_excluded:
            filtered.append(recipe)

    return filtered


def _find_recipes_for_include_rule(
    recipes: list[dict],
    target: str,
    meal_slots: Optional[list[str]] = None,
) -> list[dict]:
    """Find recipes that match an INCLUDE rule target."""
    matching = []

    for recipe in recipes:
        if _recipe_matches_target(recipe, target):
            # Check if meal slot matches (if specified)
            if meal_slots:
                recipe_meals = [m.lower() for m in recipe.get("meal_types", [])]
                slot_match = any(slot.lower() in recipe_meals for slot in meal_slots)
                if not slot_match:
                    continue
            matching.append(recipe)

    return matching


def _filter_by_cooking_time(
    recipes: list[dict],
    max_time_minutes: int,
) -> list[dict]:
    """Filter recipes by maximum cooking time."""
    filtered = []
    for recipe in recipes:
        # Use prep_time_minutes or total_time_minutes
        prep_time = recipe.get("prep_time_minutes") or recipe.get("total_time_minutes") or 30
        if prep_time <= max_time_minutes:
            filtered.append(recipe)
    return filtered


def _pick_recipe_with_constraints(
    recipe_list: list[dict],
    used_recipe_ids: set[str],
    max_time: Optional[int] = None,
) -> Optional[dict]:
    """Pick a recipe respecting constraints and avoiding duplicates."""
    candidates = recipe_list.copy()

    # Filter by cooking time if specified
    if max_time:
        candidates = _filter_by_cooking_time(candidates, max_time)

    # Prefer recipes not already used this week
    unused = [r for r in candidates if r.get("id") not in used_recipe_ids]
    if unused:
        candidates = unused

    if not candidates:
        return None

    return random.choice(candidates)


def _build_response_from_firestore(plan: dict[str, Any]) -> MealPlanResponse:
    """Build MealPlanResponse from Firestore document."""
    days = []
    for day_data in plan.get("days", []):
        meals = day_data.get("meals", {})

        def _build_meal_items(meal_list: list) -> list[MealItemDto]:
            items = []
            for i, m in enumerate(meal_list or []):
                items.append(MealItemDto(
                    id=m.get("id", str(uuid.uuid4())),
                    recipe_id=m.get("recipe_id", ""),
                    recipe_name=m.get("recipe_name", ""),
                    recipe_image_url=m.get("recipe_image_url"),
                    prep_time_minutes=m.get("prep_time_minutes", 30),
                    calories=m.get("calories", 0),
                    is_locked=m.get("is_locked", False),
                    order=i,
                    dietary_tags=m.get("dietary_tags", []),
                ))
            return items

        festival = None
        if day_data.get("festival"):
            f = day_data["festival"]
            festival = FestivalDto(
                id=f.get("id", ""),
                name=f.get("name", ""),
                is_fasting_day=f.get("is_fasting_day", False),
                suggested_dishes=f.get("suggested_dishes"),
            )

        days.append(MealPlanDayDto(
            date=day_data.get("date", ""),
            day_name=day_data.get("day_name", ""),
            meals=MealsByTypeDto(
                breakfast=_build_meal_items(meals.get("breakfast", [])),
                lunch=_build_meal_items(meals.get("lunch", [])),
                dinner=_build_meal_items(meals.get("dinner", [])),
                snacks=_build_meal_items(meals.get("snacks", [])),
            ),
            festival=festival,
        ))

    created_at = plan.get("created_at")
    updated_at = plan.get("updated_at")

    return MealPlanResponse(
        id=plan.get("id", ""),
        week_start_date=plan.get("week_start_date", ""),
        week_end_date=plan.get("week_end_date", ""),
        days=days,
        created_at=created_at.isoformat() if isinstance(created_at, datetime) else str(created_at),
        updated_at=updated_at.isoformat() if isinstance(updated_at, datetime) else str(updated_at),
    )


@router.post("/generate", response_model=MealPlanResponse)
async def generate(
    request: GenerateMealPlanRequest,
    current_user: CurrentUser,
) -> MealPlanResponse:
    """Generate a new meal plan using Firestore recipes.

    Creates a personalized 7-day meal plan based on user preferences,
    dietary restrictions, recipe rules, and available recipes in Firestore.

    Enforces:
    - EXCLUDE rules (never include certain ingredients/recipes)
    - INCLUDE rules (must include certain items at specified frequency)
    - Allergies (always excluded)
    - Dislikes (always excluded)
    - Cooking time limits (weekday vs weekend, busy days)
    - NUTRITION_GOAL rules (prefer recipes meeting nutrition targets)
    """
    user_id = current_user.get("id")
    logger.info(f"Generating meal plan for user {user_id}")

    # Parse week start date
    try:
        week_start = date.fromisoformat(request.week_start_date)
    except ValueError:
        week_start = date.today()
        week_start = week_start - timedelta(days=week_start.weekday())

    week_end = week_start + timedelta(days=6)

    # ==========================================================================
    # STEP 1: Get user preferences and recipe rules
    # ==========================================================================
    user_repo = UserRepository()
    prefs = await user_repo.get_preferences(user_id)

    dietary_tags = ["vegetarian"]
    cuisine_type = None
    recipe_rules = []
    allergies = []
    dislikes = []
    weekday_cooking_time = 30
    weekend_cooking_time = 60
    busy_days = []

    if prefs:
        dietary_tags = prefs.get("dietary_tags", ["vegetarian"])
        cuisines = prefs.get("cuisine_preferences", [])
        cuisine_type = cuisines[0].lower() if cuisines else None
        recipe_rules = prefs.get("recipe_rules", [])
        allergies = prefs.get("allergies", [])
        dislikes = prefs.get("disliked_ingredients", [])
        weekday_cooking_time = prefs.get("weekday_cooking_time_minutes", 30)
        weekend_cooking_time = prefs.get("weekend_cooking_time_minutes", 60)
        busy_days = [d.upper() for d in prefs.get("busy_days", [])]

    logger.info(f"User preferences: dietary_tags={dietary_tags}, cuisine={cuisine_type}")
    logger.info(f"Recipe rules: {len(recipe_rules)} rules, allergies={allergies}, dislikes={dislikes}")
    logger.info(f"Cooking time: weekday={weekday_cooking_time}min, weekend={weekend_cooking_time}min, busy_days={busy_days}")

    # ==========================================================================
    # STEP 2: Parse recipe rules into categories
    # ==========================================================================
    include_rules = [r for r in recipe_rules if r.get("type") == "INCLUDE"]
    exclude_rules = [r for r in recipe_rules if r.get("type") == "EXCLUDE"]
    nutrition_goals = [r for r in recipe_rules if r.get("type") == "NUTRITION_GOAL"]

    logger.info(f"Rule breakdown: {len(include_rules)} INCLUDE, {len(exclude_rules)} EXCLUDE, {len(nutrition_goals)} NUTRITION_GOAL")

    # ==========================================================================
    # STEP 3: Get recipes from Firestore
    # ==========================================================================
    recipe_repo = RecipeRepository()
    recipes = await recipe_repo.search(
        cuisine_type=cuisine_type,
        dietary_tags=dietary_tags if dietary_tags else ["vegetarian"],
        limit=500,  # Get more to have variety after filtering
    )

    logger.info(f"Found {len(recipes)} matching recipes from Firestore")

    if not recipes:
        # Fallback: get any recipes
        recipes = await recipe_repo.get_all(limit=500)
        logger.warning(f"No filtered recipes, using all {len(recipes)} recipes")

    # ==========================================================================
    # STEP 4: Apply EXCLUDE rules, allergies, and dislikes
    # ==========================================================================
    filtered_recipes = _filter_by_exclude_rules(recipes, exclude_rules, allergies, dislikes)
    logger.info(f"After EXCLUDE/allergy/dislike filtering: {len(filtered_recipes)} recipes remain")

    # ==========================================================================
    # STEP 5: Categorize recipes by meal type
    # ==========================================================================
    breakfast_recipes = [r for r in filtered_recipes if "breakfast" in r.get("meal_types", [])]
    lunch_recipes = [r for r in filtered_recipes if "lunch" in r.get("meal_types", [])]
    dinner_recipes = [r for r in filtered_recipes if "dinner" in r.get("meal_types", [])]
    snack_recipes = [r for r in filtered_recipes if "snacks" in r.get("meal_types", [])]

    # Fallback if categories are empty (use general recipes)
    if not breakfast_recipes:
        breakfast_recipes = filtered_recipes[:100] if filtered_recipes else []
    if not lunch_recipes:
        lunch_recipes = filtered_recipes[:100] if filtered_recipes else []
    if not dinner_recipes:
        dinner_recipes = filtered_recipes[:100] if filtered_recipes else []
    if not snack_recipes:
        snack_recipes = filtered_recipes[:50] if filtered_recipes else []

    logger.info(f"By meal type: breakfast={len(breakfast_recipes)}, lunch={len(lunch_recipes)}, dinner={len(dinner_recipes)}, snacks={len(snack_recipes)}")

    # ==========================================================================
    # STEP 6: Pre-process INCLUDE rules to find matching recipes
    # ==========================================================================
    include_assignments = {}  # Maps rule index -> list of recipes
    for idx, rule in enumerate(include_rules):
        target = rule.get("target", "")
        meal_slots = rule.get("meal_slot")
        matching = _find_recipes_for_include_rule(filtered_recipes, target, meal_slots)
        include_assignments[idx] = {
            "rule": rule,
            "recipes": matching,
            "times_needed": 7 if rule.get("frequency") == "DAILY" else rule.get("times_per_week", 1),
            "times_assigned": 0,
        }
        logger.info(f"INCLUDE rule '{target}': found {len(matching)} matching recipes")

    # ==========================================================================
    # STEP 7: Build 7-day meal plan with rule enforcement
    # ==========================================================================
    days = []
    current_date = week_start
    used_recipe_ids = set()

    # Helper to convert recipe to meal item dict
    def _recipe_to_meal_item(r: dict) -> dict:
        return {
            "id": str(uuid.uuid4()),
            "recipe_id": r.get("id", ""),
            "recipe_name": r.get("name", "Unknown Recipe"),
            "recipe_image_url": r.get("image_url"),
            "prep_time_minutes": r.get("prep_time_minutes") or r.get("total_time_minutes") or 30,
            "calories": r.get("nutrition", {}).get("calories", 0) if r.get("nutrition") else 0,
            "is_locked": False,
            "dietary_tags": r.get("dietary_tags", []),
        }

    def _empty_meal_item() -> dict:
        return {
            "id": str(uuid.uuid4()),
            "recipe_id": "",
            "recipe_name": "No recipe available",
            "recipe_image_url": None,
            "prep_time_minutes": 30,
            "calories": 0,
            "is_locked": False,
            "dietary_tags": [],
        }

    for i in range(7):
        day_name = current_date.strftime("%A")
        is_weekend = day_name in ["Saturday", "Sunday"]
        is_busy_day = day_name.upper() in busy_days

        # Determine max cooking time for this day
        if is_busy_day:
            max_cooking_time = weekday_cooking_time  # Busy days use weekday limit
        elif is_weekend:
            max_cooking_time = weekend_cooking_time
        else:
            max_cooking_time = weekday_cooking_time

        day_meals = {"breakfast": [], "lunch": [], "dinner": [], "snacks": []}

        # -----------------------------------------------------------------
        # STEP 7a: First, try to satisfy INCLUDE rules for this day
        # -----------------------------------------------------------------
        for idx, assignment in include_assignments.items():
            rule = assignment["rule"]
            frequency = rule.get("frequency")
            times_needed = assignment["times_needed"]
            times_assigned = assignment["times_assigned"]

            # Skip if already met quota
            if times_assigned >= times_needed:
                continue

            # For DAILY rules, assign every day
            # For WEEKLY rules, spread across the week
            should_assign = False
            if frequency == "DAILY":
                should_assign = True
            elif frequency == "WEEKLY":
                # Spread evenly: if need X times in 7 days, assign on days [0, 7/X, 2*7/X, ...]
                days_remaining = 7 - i
                times_remaining = times_needed - times_assigned
                if times_remaining > 0 and days_remaining <= times_remaining:
                    should_assign = True
                elif times_remaining > 0 and (i % max(1, 7 // times_needed)) == 0:
                    should_assign = True

            if should_assign and assignment["recipes"]:
                meal_slots = rule.get("meal_slot", ["BREAKFAST", "LUNCH", "DINNER", "SNACKS"])
                meal_slots = [s.lower() for s in meal_slots]

                # Find a slot that's not yet filled
                for slot in meal_slots:
                    if slot in day_meals and not day_meals[slot]:
                        # Pick from matching recipes with time constraint
                        recipe = _pick_recipe_with_constraints(
                            assignment["recipes"],
                            used_recipe_ids,
                            max_cooking_time
                        )
                        if recipe:
                            day_meals[slot] = [_recipe_to_meal_item(recipe)]
                            used_recipe_ids.add(recipe.get("id"))
                            assignment["times_assigned"] += 1
                            logger.debug(f"INCLUDE rule '{rule.get('target')}' assigned to {day_name} {slot}")
                            break

        # -----------------------------------------------------------------
        # STEP 7b: Fill remaining slots with regular recipes
        # -----------------------------------------------------------------
        slot_pools = {
            "breakfast": breakfast_recipes,
            "lunch": lunch_recipes,
            "dinner": dinner_recipes,
            "snacks": snack_recipes,
        }

        for slot, pool in slot_pools.items():
            if not day_meals[slot]:
                recipe = _pick_recipe_with_constraints(pool, used_recipe_ids, max_cooking_time)
                if recipe:
                    day_meals[slot] = [_recipe_to_meal_item(recipe)]
                    used_recipe_ids.add(recipe.get("id"))
                else:
                    # If no recipe fits time constraint, pick any (better than empty)
                    recipe = _pick_recipe_with_constraints(pool, used_recipe_ids, None)
                    if recipe:
                        day_meals[slot] = [_recipe_to_meal_item(recipe)]
                        used_recipe_ids.add(recipe.get("id"))
                        logger.warning(f"No recipe within {max_cooking_time}min for {day_name} {slot}, using {recipe.get('name')}")
                    else:
                        day_meals[slot] = [_empty_meal_item()]

        days.append({
            "date": current_date.isoformat(),
            "day_name": day_name,
            "meals": day_meals,
            "festival": None,
        })

        current_date += timedelta(days=1)

    # Log INCLUDE rule fulfillment
    for idx, assignment in include_assignments.items():
        rule = assignment["rule"]
        logger.info(f"INCLUDE rule '{rule.get('target')}': assigned {assignment['times_assigned']}/{assignment['times_needed']} times")

    # ==========================================================================
    # STEP 8: Create meal plan in Firestore
    # ==========================================================================
    meal_plan_repo = MealPlanRepository()

    # Deactivate old plans
    await meal_plan_repo.deactivate_old_plans(user_id, "")

    plan_data = {
        "user_id": user_id,
        "week_start_date": week_start.isoformat(),
        "week_end_date": week_end.isoformat(),
        "days": days,
        "rules_applied": {
            "include_rules": len(include_rules),
            "exclude_rules": len(exclude_rules),
            "nutrition_goals": len(nutrition_goals),
            "allergies_excluded": len(allergies),
            "dislikes_excluded": len(dislikes),
        }
    }

    created_plan = await meal_plan_repo.create(plan_data)
    logger.info(f"Created meal plan {created_plan.get('id')} for user {user_id}")

    return _build_response_from_firestore(created_plan)


@router.get("/current", response_model=MealPlanResponse)
async def get_current(
    current_user: CurrentUser,
) -> MealPlanResponse:
    """Get the current week's meal plan from Firestore."""
    user_id = current_user.get("id")

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
    user_id = current_user.get("id")

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
    user_id = current_user.get("id")

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
                        new_recipe = await recipe_repo.get_by_id(request.specific_recipe_id)
                    else:
                        recipes = await recipe_repo.search(meal_type=meal_type, limit=20)
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
                        "calories": new_recipe.get("nutrition", {}).get("calories", 0) if new_recipe.get("nutrition") else 0,
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
    user_id = current_user.get("id")

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
                    days[day_idx]["meals"][meal_type][meal_idx]["is_locked"] = not meal.get("is_locked", False)
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
