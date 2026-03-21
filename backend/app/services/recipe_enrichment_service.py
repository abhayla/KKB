"""Recipe instruction enrichment via Gemini AI.

After meal plan generation, AI-generated recipes have placeholder instructions.
This service calls Gemini to generate real cooking steps and updates the recipe
in the database. Designed to run as a background task — failures are logged but
never propagated.
"""

import logging
import uuid

from sqlalchemy import delete, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.ai.gemini_client import generate_text
from app.models.recipe import Recipe, RecipeInstruction

logger = logging.getLogger(__name__)

# Recipes with more than this many instructions are considered already enriched.
# Placeholder recipes have 4 generic steps; enriched recipes have 5+.
ENRICHMENT_THRESHOLD = 4


def _parse_instructions(text: str) -> list[dict]:
    """Parse numbered instruction lines from Gemini response.

    Expected format:
        1. Step one text.
        2. Step two text.
    """
    steps = []
    for line in text.strip().split("\n"):
        line = line.strip()
        if not line:
            continue
        # Strip leading number + period/parenthesis
        for i, ch in enumerate(line):
            if ch in ".)" and i > 0:
                instruction_text = line[i + 1 :].strip()
                if instruction_text:
                    steps.append(
                        {
                            "step_number": len(steps) + 1,
                            "instruction": instruction_text,
                        }
                    )
                break
        else:
            # No numbered prefix — use the line as-is
            steps.append(
                {
                    "step_number": len(steps) + 1,
                    "instruction": line,
                }
            )
    return steps


async def enrich_recipe_instructions(
    db: AsyncSession,
    recipe_id: str,
) -> None:
    """Fetch a recipe and replace placeholder instructions with AI-generated ones.

    Skips recipes that already have more than ENRICHMENT_THRESHOLD instructions.
    Failures are logged but never raised — this is a best-effort background task.

    Args:
        db: Async database session
        recipe_id: UUID of the recipe to enrich
    """
    try:
        result = await db.execute(
            select(Recipe)
            .options(selectinload(Recipe.instructions))
            .where(Recipe.id == recipe_id)
        )
        recipe = result.scalar_one_or_none()

        if not recipe:
            logger.warning(f"Recipe {recipe_id} not found for enrichment")
            return

        # Skip if already enriched
        if len(recipe.instructions) > ENRICHMENT_THRESHOLD:
            logger.debug(
                f"Recipe '{recipe.name}' already has {len(recipe.instructions)} "
                f"instructions — skipping enrichment"
            )
            return

        # Call Gemini for real instructions
        prompt = (
            f"Generate 5-7 detailed cooking steps for the Indian dish "
            f"'{recipe.name}'. Include ingredient quantities where relevant. "
            f"Format as numbered steps (1. 2. 3. etc.), one per line. "
            f"Keep each step concise but actionable."
        )
        response_text = await generate_text(
            prompt=prompt,
            temperature=0.7,
            max_output_tokens=2048,
        )

        steps = _parse_instructions(response_text)
        if len(steps) < 3:
            logger.warning(
                f"Gemini returned only {len(steps)} steps for '{recipe.name}' "
                f"— keeping placeholder instructions"
            )
            return

        # Delete old placeholder instructions
        await db.execute(
            delete(RecipeInstruction).where(
                RecipeInstruction.recipe_id == recipe_id
            )
        )

        # Insert new instructions
        for step in steps:
            db.add(
                RecipeInstruction(
                    id=str(uuid.uuid4()),
                    recipe_id=recipe_id,
                    step_number=step["step_number"],
                    instruction=step["instruction"],
                )
            )

        await db.commit()
        logger.info(
            f"Enriched recipe '{recipe.name}' with {len(steps)} AI-generated steps"
        )

    except Exception as e:
        logger.error(f"Failed to enrich recipe {recipe_id}: {e}")
        await db.rollback()
