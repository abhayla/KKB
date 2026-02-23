"""Grocery list service."""

import uuid
from collections import defaultdict
from typing import Optional

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.core.exceptions import NotFoundError
from app.models.meal_plan import MealPlan, MealPlanItem
from app.models.recipe import Recipe
from app.models.user import User
from app.schemas.grocery import (
    GroceryCategoryResponse,
    GroceryItemResponse,
    GroceryListResponse,
    WhatsAppGroceryResponse,
)


# Category display order and emojis
CATEGORY_INFO = {
    "vegetables": {"order": 1, "emoji": "🥬"},
    "fruits": {"order": 2, "emoji": "🍎"},
    "dairy": {"order": 3, "emoji": "🥛"},
    "grains": {"order": 4, "emoji": "🌾"},
    "pulses": {"order": 5, "emoji": "🫘"},
    "spices": {"order": 6, "emoji": "🌶️"},
    "oils": {"order": 7, "emoji": "🫒"},
    "meat": {"order": 8, "emoji": "🍖"},
    "seafood": {"order": 9, "emoji": "🐟"},
    "nuts": {"order": 10, "emoji": "🥜"},
    "sweeteners": {"order": 11, "emoji": "🍯"},
    "other": {"order": 99, "emoji": "📦"},
}


def _normalize_unit(unit: str) -> str:
    """Normalize unit for aggregation."""
    unit = unit.lower().strip()
    # Map variations to standard units
    unit_map = {
        "g": "grams",
        "gram": "grams",
        "kg": "kg",
        "kilogram": "kg",
        "kilograms": "kg",
        "ml": "ml",
        "milliliter": "ml",
        "l": "liters",
        "liter": "liters",
        "cup": "cups",
        "tbsp": "tbsp",
        "tablespoon": "tbsp",
        "tablespoons": "tbsp",
        "tsp": "tsp",
        "teaspoon": "tsp",
        "teaspoons": "tsp",
        "piece": "pieces",
        "pc": "pieces",
        "pcs": "pieces",
    }
    return unit_map.get(unit, unit)


async def get_grocery_list_for_meal_plan(
    db: AsyncSession,
    user: User,
    meal_plan_id: Optional[str] = None,
) -> GroceryListResponse:
    """Get aggregated grocery list for a meal plan.

    Args:
        db: Database session
        user: Current user
        meal_plan_id: Optional specific meal plan ID

    Returns:
        GroceryListResponse with aggregated items
    """
    # Get meal plan
    if meal_plan_id:
        try:
            plan_uuid = uuid.UUID(meal_plan_id)
        except ValueError:
            raise NotFoundError("Invalid meal plan ID")

        result = await db.execute(
            select(MealPlan)
            .options(
                selectinload(MealPlan.items)
                .selectinload(MealPlanItem.recipe)
                .selectinload(Recipe.ingredients)
            )
            .where(
                MealPlan.id == str(plan_uuid),
                MealPlan.user_id == user.id,
            )
        )
        meal_plan = result.scalar_one_or_none()
    else:
        # Get current week's plan
        from datetime import date, timedelta

        today = date.today()
        week_start = today - timedelta(days=today.weekday())

        result = await db.execute(
            select(MealPlan)
            .options(
                selectinload(MealPlan.items)
                .selectinload(MealPlanItem.recipe)
                .selectinload(Recipe.ingredients)
            )
            .where(
                MealPlan.user_id == user.id,
                MealPlan.week_start_date == week_start,
                MealPlan.is_active == True,
            )
        )
        meal_plan = result.scalar_one_or_none()

    if not meal_plan:
        raise NotFoundError("No meal plan found")

    # Aggregate ingredients
    # Key: (name, unit) -> total quantity
    aggregated: dict[tuple[str, str, str], float] = defaultdict(float)

    for item in meal_plan.items:
        if item.recipe:
            scale_factor = (
                item.servings / item.recipe.servings if item.recipe.servings else 1
            )
            for ing in item.recipe.ingredients:
                if not ing.is_optional:
                    key = (
                        ing.name.lower(),
                        _normalize_unit(ing.unit),
                        ing.category.lower(),
                    )
                    aggregated[key] += ing.quantity * scale_factor

    # Group by category
    categories_dict: dict[str, list[GroceryItemResponse]] = defaultdict(list)

    for (name, unit, category), quantity in aggregated.items():
        item_response = GroceryItemResponse(
            id=str(uuid.uuid4()),
            name=name.title(),
            quantity=round(quantity, 2),
            unit=unit,
            category=category,
            notes=None,
            is_checked=False,
            is_in_pantry=False,
        )
        categories_dict[category].append(item_response)

    # Sort categories by predefined order
    sorted_categories = []
    for cat_name in sorted(
        categories_dict.keys(), key=lambda c: CATEGORY_INFO.get(c, {}).get("order", 50)
    ):
        sorted_categories.append(
            GroceryCategoryResponse(
                category=cat_name.title(),
                items=sorted(categories_dict[cat_name], key=lambda x: x.name),
            )
        )

    total_items = sum(len(cat.items) for cat in sorted_categories)

    return GroceryListResponse(
        id=str(meal_plan.id),
        name=f"Week of {meal_plan.week_start_date.isoformat()}",
        meal_plan_id=str(meal_plan.id),
        categories=sorted_categories,
        total_items=total_items,
        checked_items=0,
    )


async def get_grocery_list_whatsapp(
    db: AsyncSession,
    user: User,
    meal_plan_id: str,
) -> WhatsAppGroceryResponse:
    """Get grocery list formatted for WhatsApp sharing.

    Args:
        db: Database session
        user: Current user
        meal_plan_id: Meal plan ID

    Returns:
        WhatsAppGroceryResponse with formatted text
    """
    grocery_list = await get_grocery_list_for_meal_plan(db, user, meal_plan_id)

    lines = ["🛒 *Grocery List*", f"📅 {grocery_list.name}", ""]

    total_items = 0
    for category in grocery_list.categories:
        emoji = CATEGORY_INFO.get(category.category.lower(), {}).get("emoji", "📦")
        lines.append(f"{emoji} *{category.category}*")

        for item in category.items:
            qty_str = f"{item.quantity:.1f}".rstrip("0").rstrip(".")
            lines.append(f"  • {item.name} - {qty_str} {item.unit}")
            total_items += 1

        lines.append("")

    lines.append(f"📝 Total items: {total_items}")
    lines.append("")
    lines.append("_Generated by RasoiAI_ 🍳")

    return WhatsAppGroceryResponse(
        formatted_text="\n".join(lines),
        item_count=total_items,
    )
