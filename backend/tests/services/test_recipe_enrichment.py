"""Tests for AI recipe instruction enrichment.

Gap 14: After meal plan generation, enqueue a background task to call Gemini
for real cooking instructions for AI-generated recipes (replacing generic
placeholders).
"""

import uuid
from unittest.mock import AsyncMock, patch

import pytest
from sqlalchemy import select
from sqlalchemy.orm import selectinload

from app.models.recipe import Recipe, RecipeInstruction
from app.services.recipe_enrichment_service import enrich_recipe_instructions


def _make_placeholder_instructions(recipe_id: str) -> list[RecipeInstruction]:
    """Create 4 placeholder instructions (like recipe_creation_service does)."""
    texts = [
        "Gather and prepare all ingredients for Test Recipe.",
        "Heat oil or ghee in a pan on medium flame.",
        "Cook the Test Recipe following traditional preparation method.",
        "Serve hot with accompaniments.",
    ]
    return [
        RecipeInstruction(
            id=str(uuid.uuid4()),
            recipe_id=recipe_id,
            step_number=i + 1,
            instruction=text,
        )
        for i, text in enumerate(texts)
    ]


def _make_real_instructions(recipe_id: str, count: int = 6) -> list[RecipeInstruction]:
    """Create real (non-placeholder) instructions."""
    return [
        RecipeInstruction(
            id=str(uuid.uuid4()),
            recipe_id=recipe_id,
            step_number=i + 1,
            instruction=f"Detailed step {i + 1}: do something specific.",
        )
        for i in range(count)
    ]


@pytest.fixture
async def recipe_with_placeholders(db_session):
    """Create a recipe with placeholder instructions (<=3 meaningful steps)."""
    recipe_id = str(uuid.uuid4())
    recipe = Recipe(
        id=recipe_id,
        name="Paneer Butter Masala",
        cuisine_type="north",
        meal_types=["dinner"],
        dietary_tags=["vegetarian"],
        prep_time_minutes=30,
        cook_time_minutes=30,
        total_time_minutes=30,
        servings=4,
    )
    db_session.add(recipe)
    for inst in _make_placeholder_instructions(recipe_id):
        db_session.add(inst)
    await db_session.commit()
    return recipe_id


@pytest.fixture
async def recipe_already_enriched(db_session):
    """Create a recipe with real instructions (5+ steps)."""
    recipe_id = str(uuid.uuid4())
    recipe = Recipe(
        id=recipe_id,
        name="Dal Tadka",
        cuisine_type="north",
        meal_types=["lunch"],
        dietary_tags=["vegetarian"],
        prep_time_minutes=25,
        cook_time_minutes=25,
        total_time_minutes=25,
        servings=4,
    )
    db_session.add(recipe)
    for inst in _make_real_instructions(recipe_id, count=6):
        db_session.add(inst)
    await db_session.commit()
    return recipe_id


MOCK_GEMINI_RESPONSE = """1. Soak paneer cubes in warm water for 10 minutes.
2. Heat butter in a heavy-bottomed pan over medium heat.
3. Add cumin seeds and let them splutter, then add onion paste.
4. Cook the onion paste until golden brown, about 5 minutes.
5. Add tomato puree, red chili powder, and salt. Cook for 8 minutes.
6. Add cream and kasuri methi. Stir well.
7. Add paneer cubes and simmer for 5 minutes. Serve with naan."""


async def test_enrich_generates_real_instructions(
    db_session, recipe_with_placeholders
):
    """Mock Gemini and verify that instructions get updated in DB."""
    recipe_id = recipe_with_placeholders

    with patch(
        "app.services.recipe_enrichment_service.generate_text",
        new_callable=AsyncMock,
        return_value=MOCK_GEMINI_RESPONSE,
    ) as mock_gemini:
        await enrich_recipe_instructions(db_session, recipe_id)
        mock_gemini.assert_called_once()

    # Reload recipe with instructions
    result = await db_session.execute(
        select(Recipe)
        .options(selectinload(Recipe.instructions))
        .where(Recipe.id == recipe_id)
    )
    recipe = result.scalar_one()

    # Should have the new instructions (7 steps from mock)
    assert len(recipe.instructions) >= 5
    # First instruction should not be the placeholder text
    first_text = recipe.instructions[0].instruction
    assert "Gather and prepare" not in first_text


async def test_enrich_skips_already_enriched_recipes(
    db_session, recipe_already_enriched
):
    """Recipe with 5+ instructions should NOT trigger a Gemini call."""
    recipe_id = recipe_already_enriched

    with patch(
        "app.services.recipe_enrichment_service.generate_text",
        new_callable=AsyncMock,
    ) as mock_gemini:
        await enrich_recipe_instructions(db_session, recipe_id)
        mock_gemini.assert_not_called()


async def test_enrich_handles_gemini_failure_gracefully(
    db_session, recipe_with_placeholders
):
    """If Gemini fails, the recipe should remain unchanged."""
    recipe_id = recipe_with_placeholders

    # Get original instruction count
    result = await db_session.execute(
        select(Recipe)
        .options(selectinload(Recipe.instructions))
        .where(Recipe.id == recipe_id)
    )
    recipe = result.scalar_one()
    original_count = len(recipe.instructions)
    original_first = recipe.instructions[0].instruction

    with patch(
        "app.services.recipe_enrichment_service.generate_text",
        new_callable=AsyncMock,
        side_effect=Exception("Gemini API error"),
    ):
        # Should NOT raise — handles gracefully
        await enrich_recipe_instructions(db_session, recipe_id)

    # Reload and verify unchanged
    result = await db_session.execute(
        select(Recipe)
        .options(selectinload(Recipe.instructions))
        .where(Recipe.id == recipe_id)
    )
    recipe = result.scalar_one()
    assert len(recipe.instructions) == original_count
    assert recipe.instructions[0].instruction == original_first
