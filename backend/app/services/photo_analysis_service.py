"""Food photo analysis service using Gemini Vision API.

Analyzes food photos to identify dishes, ingredients, and provide
nutritional estimates using Google Gemini's multimodal capabilities.
"""

import base64
import json
import logging
from typing import Any

from app.ai.gemini_client import get_gemini_client

logger = logging.getLogger(__name__)

ANALYSIS_PROMPT = """Analyze this food photo and identify:

1. **Food Items**: List all identifiable food items/dishes in the image
2. **Ingredients**: List the likely ingredients used in each dish
3. **Cuisine**: Identify the cuisine type (Indian, North Indian, South Indian, etc.)
4. **Meal Type**: Is this breakfast, lunch, dinner, or snack?
5. **Estimated Nutrition**: Rough calorie and macro estimates per serving

Return your analysis as valid JSON with this structure:
{
  "identified_foods": [
    {
      "name": "Dish Name",
      "confidence": 0.9,
      "cuisine": "North Indian",
      "meal_type": "lunch",
      "estimated_ingredients": ["ingredient1", "ingredient2"],
      "estimated_calories": 350,
      "estimated_nutrition": {
        "protein_g": 12,
        "carbs_g": 45,
        "fat_g": 15
      }
    }
  ],
  "all_ingredients": ["ingredient1", "ingredient2", "ingredient3"],
  "overall_cuisine": "North Indian",
  "overall_meal_type": "lunch"
}

If you cannot identify the food, return:
{"identified_foods": [], "all_ingredients": [], "overall_cuisine": "unknown", "overall_meal_type": "unknown"}

Return ONLY valid JSON, no markdown or explanation."""


async def analyze_food_photo(
    image_data: bytes,
    mime_type: str = "image/jpeg",
) -> dict[str, Any]:
    """Analyze a food photo using Gemini Vision API.

    Args:
        image_data: Raw image bytes
        mime_type: Image MIME type (image/jpeg, image/png)

    Returns:
        Analysis result with identified foods, ingredients, and nutrition.
        On failure, returns a dict with empty results and an 'error' key.
    """
    logger.info(f"Analyzing food photo ({len(image_data)} bytes, {mime_type})")

    client = get_gemini_client()
    if not client:
        return {
            "identified_foods": [],
            "all_ingredients": [],
            "overall_cuisine": "unknown",
            "overall_meal_type": "unknown",
            "error": "AI vision service is not configured",
        }

    try:
        from google.genai import types

        image_part = types.Part.from_bytes(data=image_data, mime_type=mime_type)

        response = await client.aio.models.generate_content(
            model="gemini-2.5-flash",
            contents=[ANALYSIS_PROMPT, image_part],
        )

        response_text = response.text.strip() if response.text else ""

        # Strip markdown code blocks if present
        if response_text.startswith("```"):
            lines = response_text.split("\n")
            response_text = "\n".join(lines[1:-1])

        result = json.loads(response_text)
        logger.info(f"Identified {len(result.get('identified_foods', []))} food items")
        return result

    except json.JSONDecodeError as e:
        logger.error(f"Failed to parse Gemini response: {e}")
        return {
            "identified_foods": [],
            "all_ingredients": [],
            "overall_cuisine": "unknown",
            "overall_meal_type": "unknown",
            "error": "Failed to parse AI response",
        }
    except Exception as e:
        logger.error(f"Gemini Vision API error: {e}")
        return {
            "identified_foods": [],
            "all_ingredients": [],
            "overall_cuisine": "unknown",
            "overall_meal_type": "unknown",
            "error": "Photo analysis failed. Please try again later.",
        }
