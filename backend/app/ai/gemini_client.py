"""Google Gemini API client for vision and text generation."""

import base64
import logging
import re
from typing import Optional

from app.config import settings

logger = logging.getLogger(__name__)

# Lazy import and model initialization
_model = None


def get_gemini_model():
    """Get or create Gemini model instance."""
    global _model

    if _model is not None:
        return _model

    if not settings.google_ai_api_key:
        logger.warning("GOOGLE_AI_API_KEY not configured")
        return None

    try:
        import google.generativeai as genai

        genai.configure(api_key=settings.google_ai_api_key)
        _model = genai.GenerativeModel("gemini-1.5-flash")
        logger.info("Gemini model initialized successfully")
        return _model
    except Exception as e:
        logger.error(f"Failed to initialize Gemini model: {e}")
        return None


async def analyze_food_image(
    image_base64: str,
    media_type: str = "image/jpeg",
    prompt: str = None
) -> dict:
    """Analyze a food image using Gemini Vision.

    Args:
        image_base64: Base64 encoded image
        media_type: Image MIME type (image/jpeg or image/png)
        prompt: Custom prompt (optional)

    Returns:
        Dict with 'message' and optional 'recipe_suggestions'
    """
    model = get_gemini_model()

    if not model:
        return {
            "message": "AI vision service is not configured. Please try again later.",
            "recipe_suggestions": []
        }

    default_prompt = """You are RasoiAI, a friendly and knowledgeable Indian cooking assistant.

Analyze this food image and provide a helpful response:

1. **Dish Identification**: If this is a known dish (especially Indian), identify it by name (in English and Hindi if applicable)

2. **Visible Ingredients**: List the main ingredients you can see or infer from the image

3. **Recipe Suggestions**: Suggest 2-3 similar or complementary Indian recipes that the user might enjoy

4. **Cooking Tips**: Share a relevant cooking tip if applicable

Be warm, conversational, and use occasional Hindi food terms naturally (like "tadka", "masala", "roti").
If you cannot identify the food or it's not food-related, politely mention that and offer to help with cooking questions instead.

Format your response in a conversational, easy-to-read way."""

    try:
        # Decode base64 image
        image_data = base64.b64decode(image_base64)

        # Create image part for Gemini
        image_part = {
            "mime_type": media_type,
            "data": image_data
        }

        # Generate response
        response = model.generate_content([
            prompt or default_prompt,
            image_part
        ])

        response_text = response.text if response.text else "I couldn't analyze that image. Please try with a clearer food photo."

        return {
            "message": response_text,
            "recipe_suggestions": _extract_recipe_suggestions(response_text)
        }

    except Exception as e:
        logger.error(f"Gemini Vision error: {e}")
        return {
            "message": "I had trouble analyzing that image. Please try again with a clearer food photo, or ask me any cooking question!",
            "recipe_suggestions": []
        }


def _extract_recipe_suggestions(text: str) -> list:
    """Extract recipe names from response text.

    This is a simple extraction - looks for common patterns where recipes
    might be mentioned.
    """
    suggestions = []

    # Common Indian dishes to look for
    common_dishes = [
        "dal", "paneer", "biryani", "curry", "masala", "tikka",
        "samosa", "dosa", "idli", "paratha", "roti", "naan",
        "chana", "aloo", "bhaji", "pakora", "raita", "chutney",
        "halwa", "kheer", "gulab jamun", "ladoo", "barfi",
        "palak", "bhindi", "gobi", "matar", "rajma", "chole",
        "korma", "vindaloo", "tandoori", "kebab", "pulao"
    ]

    text_lower = text.lower()

    for dish in common_dishes:
        if dish in text_lower:
            # Try to find the full dish name (e.g., "Palak Paneer" not just "paneer")
            pattern = rf'\b\w*\s*{dish}\s*\w*\b'
            matches = re.findall(pattern, text_lower)
            for match in matches[:3]:  # Limit to first 3 matches
                cleaned = match.strip().title()
                if cleaned and cleaned not in suggestions:
                    suggestions.append(cleaned)

    return suggestions[:5]  # Return at most 5 suggestions
