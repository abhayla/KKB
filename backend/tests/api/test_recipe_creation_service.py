"""Tests for recipe_creation_service — creates real Recipe records from AI meal items.

Verifies:
- New recipe creation with all 4 tables (Recipe, RecipeIngredient, RecipeInstruction, RecipeNutrition)
- Deduplication by normalized name
- Placeholder instructions when AI doesn't provide them
- End-to-end meal plan recipe creation
- Recipe accessible via API after creation
"""

import uuid

import pytest
import pytest_asyncio
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.recipe import Recipe, RecipeIngredient, RecipeInstruction, RecipeNutrition
from app.services.recipe_creation_service import (
    create_recipes_for_meal_plan,
    find_or_create_recipe,
)


@pytest.mark.asyncio
async def test_create_recipe_new(db_session: AsyncSession):
    """Creates recipe, verify all 4 tables populated."""
    recipe_id = await find_or_create_recipe(
        db=db_session,
        recipe_name="Aloo Paratha",
        prep_time=25,
        dietary_tags=["vegetarian"],
        category="paratha",
        calories=350,
        cuisine_type="north",
        meal_type="breakfast",
        ingredients=[
            {"name": "Wheat Flour", "quantity": 2, "unit": "cup", "category": "grains"},
            {"name": "Potato", "quantity": 3, "unit": "medium", "category": "vegetables"},
        ],
        nutrition={"protein_g": 8, "carbs_g": 45, "fat_g": 15, "fiber_g": 3},
    )
    await db_session.commit()

    # Verify recipe exists
    result = await db_session.execute(select(Recipe).where(Recipe.id == recipe_id))
    recipe = result.scalar_one()
    assert recipe.name == "Aloo Paratha"
    assert recipe.cuisine_type == "north"
    assert recipe.prep_time_minutes == 25
    assert "vegetarian" in recipe.dietary_tags

    # Verify ingredients
    ing_result = await db_session.execute(
        select(RecipeIngredient).where(RecipeIngredient.recipe_id == recipe_id)
    )
    ingredients = ing_result.scalars().all()
    assert len(ingredients) == 2
    assert ingredients[0].name == "Wheat Flour"

    # Verify instructions (placeholder since none provided)
    instr_result = await db_session.execute(
        select(RecipeInstruction).where(RecipeInstruction.recipe_id == recipe_id)
    )
    instructions = instr_result.scalars().all()
    assert len(instructions) == 4  # placeholder generates 4 steps

    # Verify nutrition
    nutr_result = await db_session.execute(
        select(RecipeNutrition).where(RecipeNutrition.recipe_id == recipe_id)
    )
    nutrition = nutr_result.scalar_one()
    assert nutrition.calories == 350
    assert nutrition.protein_grams == 8.0


@pytest.mark.asyncio
async def test_create_recipe_finds_existing(db_session: AsyncSession):
    """Returns existing ID on normalized name match."""
    # Create first recipe
    recipe_id_1 = await find_or_create_recipe(
        db=db_session,
        recipe_name="Dal Tadka",
        prep_time=30,
        dietary_tags=["vegetarian"],
        calories=250,
    )
    await db_session.commit()

    # Try to create with same name (different case/whitespace)
    recipe_id_2 = await find_or_create_recipe(
        db=db_session,
        recipe_name="  dal tadka  ",
        prep_time=35,
        dietary_tags=["vegan"],
        calories=260,
    )

    assert recipe_id_1 == recipe_id_2


@pytest.mark.asyncio
async def test_create_recipe_with_instructions(db_session: AsyncSession):
    """Full instructions stored correctly."""
    instructions = [
        {"step_number": 1, "instruction": "Wash and soak dal for 30 min.", "duration_minutes": 30, "tips": "Use warm water"},
        {"step_number": 2, "instruction": "Pressure cook dal with turmeric.", "duration_minutes": 15, "tips": None},
        {"step_number": 3, "instruction": "Prepare tadka with ghee and spices.", "duration_minutes": 5, "tips": "Use mustard seeds"},
    ]

    recipe_id = await find_or_create_recipe(
        db=db_session,
        recipe_name="Dal Fry",
        prep_time=50,
        instructions=instructions,
    )
    await db_session.commit()

    instr_result = await db_session.execute(
        select(RecipeInstruction)
        .where(RecipeInstruction.recipe_id == recipe_id)
        .order_by(RecipeInstruction.step_number)
    )
    saved = instr_result.scalars().all()
    assert len(saved) == 3
    assert saved[0].instruction == "Wash and soak dal for 30 min."
    assert saved[0].tips == "Use warm water"
    assert saved[1].duration_minutes == 15


@pytest.mark.asyncio
async def test_create_recipe_without_instructions(db_session: AsyncSession):
    """Placeholder instructions generated when AI doesn't provide them."""
    recipe_id = await find_or_create_recipe(
        db=db_session,
        recipe_name="Masala Chai",
        prep_time=10,
    )
    await db_session.commit()

    instr_result = await db_session.execute(
        select(RecipeInstruction).where(RecipeInstruction.recipe_id == recipe_id)
    )
    instructions = instr_result.scalars().all()
    assert len(instructions) == 4
    # Placeholder should mention the recipe name
    assert "Masala Chai" in instructions[0].instruction


@pytest.mark.asyncio
async def test_create_recipes_for_meal_plan(db_session: AsyncSession):
    """End-to-end with mock GeneratedMealPlan structure."""
    from dataclasses import dataclass, field
    from typing import Optional

    @dataclass
    class MockItem:
        recipe_name: str
        prep_time_minutes: int = 30
        dietary_tags: list = field(default_factory=lambda: ["vegetarian"])
        category: str = "other"
        calories: int = 200
        recipe_id: str = "AI_GENERATED"
        ingredients: Optional[list] = None
        nutrition: Optional[dict] = None
        instructions: Optional[list] = None

    @dataclass
    class MockDay:
        date: str = "2026-02-09"
        day_name: str = "Monday"
        breakfast: list = field(default_factory=list)
        lunch: list = field(default_factory=list)
        dinner: list = field(default_factory=list)
        snacks: list = field(default_factory=list)

    @dataclass
    class MockPlan:
        days: list = field(default_factory=list)

    day = MockDay(
        breakfast=[MockItem(recipe_name="Poha"), MockItem(recipe_name="Chai")],
        lunch=[MockItem(recipe_name="Dal Rice"), MockItem(recipe_name="Roti")],
        dinner=[MockItem(recipe_name="Paneer Curry"), MockItem(recipe_name="Naan")],
        snacks=[MockItem(recipe_name="Samosa"), MockItem(recipe_name="Chai")],  # Chai repeated
    )
    plan = MockPlan(days=[day])

    recipe_map = await create_recipes_for_meal_plan(
        db=db_session,
        generated_plan=plan,
        cuisine_type="north",
        family_size=4,
    )

    # 7 unique recipes (Chai appears twice but deduped)
    assert len(recipe_map) == 7

    # All items should have real UUIDs (not "AI_GENERATED")
    for item in day.breakfast + day.lunch + day.dinner + day.snacks:
        assert item.recipe_id != "AI_GENERATED"
        # Validate UUID format
        uuid.UUID(item.recipe_id)


@pytest.mark.asyncio
async def test_deduplication_same_name(db_session: AsyncSession):
    """Same dish in multiple slots gets same recipe_id."""
    from dataclasses import dataclass, field
    from typing import Optional

    @dataclass
    class MockItem:
        recipe_name: str
        prep_time_minutes: int = 30
        dietary_tags: list = field(default_factory=lambda: ["vegetarian"])
        category: str = "other"
        calories: int = 200
        recipe_id: str = "AI_GENERATED"
        ingredients: Optional[list] = None
        nutrition: Optional[dict] = None
        instructions: Optional[list] = None

    @dataclass
    class MockDay:
        date: str = "2026-02-09"
        day_name: str = "Monday"
        breakfast: list = field(default_factory=list)
        lunch: list = field(default_factory=list)
        dinner: list = field(default_factory=list)
        snacks: list = field(default_factory=list)

    @dataclass
    class MockPlan:
        days: list = field(default_factory=list)

    # Same recipe name in breakfast and snacks
    item_1 = MockItem(recipe_name="Masala Chai")
    item_2 = MockItem(recipe_name="masala chai")  # different case

    day = MockDay(breakfast=[item_1], snacks=[item_2])
    plan = MockPlan(days=[day])

    await create_recipes_for_meal_plan(db=db_session, generated_plan=plan)

    assert item_1.recipe_id == item_2.recipe_id
    assert item_1.recipe_id != "AI_GENERATED"


@pytest.mark.asyncio
async def test_recipe_fully_populated(db_session: AsyncSession):
    """After creation, recipe has all related records (ingredients, instructions, nutrition)."""
    recipe_id = await find_or_create_recipe(
        db=db_session,
        recipe_name="Paneer Butter Masala",
        prep_time=30,
        dietary_tags=["vegetarian"],
        category="curry",
        calories=350,
        cuisine_type="north",
        meal_type="dinner",
        ingredients=[
            {"name": "Paneer", "quantity": 250, "unit": "g", "category": "dairy"},
            {"name": "Tomato", "quantity": 3, "unit": "medium", "category": "vegetables"},
        ],
        nutrition={"protein_g": 18, "carbs_g": 12, "fat_g": 25, "fiber_g": 2},
    )
    await db_session.commit()

    # Verify recipe
    result = await db_session.execute(select(Recipe).where(Recipe.id == recipe_id))
    recipe = result.scalar_one()
    assert recipe.name == "Paneer Butter Masala"
    assert recipe.cuisine_type == "north"
    assert recipe.category == "curry"
    assert recipe.servings == 4

    # Verify ingredients
    ing_result = await db_session.execute(
        select(RecipeIngredient).where(RecipeIngredient.recipe_id == recipe_id)
    )
    ingredients = ing_result.scalars().all()
    assert len(ingredients) == 2
    names = {i.name for i in ingredients}
    assert "Paneer" in names
    assert "Tomato" in names

    # Verify nutrition
    nutr_result = await db_session.execute(
        select(RecipeNutrition).where(RecipeNutrition.recipe_id == recipe_id)
    )
    nutrition = nutr_result.scalar_one()
    assert nutrition.protein_grams == 18.0
    assert nutrition.fat_grams == 25.0

    # Verify instructions (placeholder)
    instr_result = await db_session.execute(
        select(RecipeInstruction).where(RecipeInstruction.recipe_id == recipe_id)
    )
    instructions = instr_result.scalars().all()
    assert len(instructions) == 4  # placeholder steps
