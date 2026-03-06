"""Google Gemini API client for vision and text generation."""

import base64
import logging
import re

from app.config import settings

logger = logging.getLogger(__name__)

# Lazy client initialization
_client = None

MODEL_NAME = "gemini-2.5-flash"


def get_gemini_client():
    """Get or create Gemini client instance."""
    global _client

    if _client is not None:
        return _client

    if not settings.google_ai_api_key:
        logger.warning("GOOGLE_AI_API_KEY not configured")
        return None

    try:
        from google import genai

        _client = genai.Client(api_key=settings.google_ai_api_key)
        logger.info("Gemini client initialized successfully")
        return _client
    except Exception as e:
        logger.error(f"Failed to initialize Gemini client: {e}")
        return None


async def analyze_food_image(
    image_base64: str, media_type: str = "image/jpeg", prompt: str = None
) -> dict:
    """Analyze a food image using Gemini Vision.

    Args:
        image_base64: Base64 encoded image
        media_type: Image MIME type (image/jpeg or image/png)
        prompt: Custom prompt (optional)

    Returns:
        Dict with 'message' and optional 'recipe_suggestions'
    """
    client = get_gemini_client()

    if not client:
        return {
            "message": "AI vision service is not configured. Please try again later.",
            "recipe_suggestions": [],
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
        from google.genai import types

        # Decode base64 image
        image_data = base64.b64decode(image_base64)

        # Create image part for Gemini
        image_part = types.Part.from_bytes(data=image_data, mime_type=media_type)

        # Generate response using async client
        response = await client.aio.models.generate_content(
            model=MODEL_NAME,
            contents=[prompt or default_prompt, image_part],
        )

        response_text = (
            response.text
            if response.text
            else "I couldn't analyze that image. Please try with a clearer food photo."
        )

        return {
            "message": response_text,
            "recipe_suggestions": _extract_recipe_suggestions(response_text),
        }

    except Exception as e:
        logger.error(f"Gemini Vision error: {e}")
        return {
            "message": "I had trouble analyzing that image. Please try again with a clearer food photo, or ask me any cooking question!",
            "recipe_suggestions": [],
        }


async def generate_text(
    prompt: str,
    temperature: float = 0.8,
    max_output_tokens: int = 65536,
    response_schema=None,
    response_json_schema: dict = None,
) -> str:
    """Generate text using Gemini 2.5 Flash.

    Args:
        prompt: The prompt to send to Gemini
        temperature: Sampling temperature (0.0-1.0, higher = more creative)
        max_output_tokens: Maximum tokens in response
        response_schema: Optional google.genai.types.Schema for structured output
        response_json_schema: Optional plain dict schema (avoids "too many states" error
            that affects complex genai.types.Schema objects)

    Returns:
        Generated text (JSON format when response_mime_type is set)

    Raises:
        ServiceUnavailableError: If Gemini is not configured
        Exception: If generation fails
    """
    from app.core.exceptions import ServiceUnavailableError

    client = get_gemini_client()

    if not client:
        raise ServiceUnavailableError(
            "Gemini AI service not configured. Set GOOGLE_AI_API_KEY."
        )

    try:
        from google.genai import types

        # Configure generation for JSON output
        config = types.GenerateContentConfig(
            temperature=temperature,
            max_output_tokens=max_output_tokens,
            response_mime_type="application/json",
            response_schema=response_schema,
            response_json_schema=response_json_schema,
        )

        response = await client.aio.models.generate_content(
            model=MODEL_NAME,
            contents=prompt,
            config=config,
        )

        if not response.text:
            raise ValueError("Empty response from Gemini")

        return response.text

    except Exception as e:
        logger.error(f"Gemini text generation failed: {e}")
        raise


async def generate_text_with_metadata(
    prompt: str,
    temperature: float = 0.8,
    max_output_tokens: int = 65536,
    response_schema=None,
    response_json_schema: dict = None,
) -> tuple[str, dict]:
    """Generate text and return (text, metadata) with token usage.

    Same as generate_text but also returns usage metadata from the response.

    Args:
        prompt: The prompt to send to Gemini
        temperature: Sampling temperature (0.0-1.0, higher = more creative)
        max_output_tokens: Maximum tokens in response
        response_schema: Optional google.genai.types.Schema for structured output
        response_json_schema: Optional plain dict schema (avoids "too many states" error
            that affects complex genai.types.Schema objects)

    Returns:
        Tuple of (response_text, metadata_dict)
    """
    from app.core.exceptions import ServiceUnavailableError

    client = get_gemini_client()

    if not client:
        raise ServiceUnavailableError(
            "Gemini AI service not configured. Set GOOGLE_AI_API_KEY."
        )

    try:
        from google.genai import types

        config = types.GenerateContentConfig(
            temperature=temperature,
            max_output_tokens=max_output_tokens,
            response_mime_type="application/json",
            response_schema=response_schema,
            response_json_schema=response_json_schema,
        )

        response = await client.aio.models.generate_content(
            model=MODEL_NAME,
            contents=prompt,
            config=config,
        )

        if not response.text:
            raise ValueError("Empty response from Gemini")

        metadata = {"model_name": MODEL_NAME}
        if hasattr(response, "usage_metadata") and response.usage_metadata:
            um = response.usage_metadata
            metadata["prompt_tokens"] = getattr(um, "prompt_token_count", None)
            metadata["completion_tokens"] = getattr(um, "candidates_token_count", None)
            metadata["total_tokens"] = getattr(um, "total_token_count", None)
            metadata["thinking_tokens"] = getattr(um, "thoughts_token_count", None)

        return response.text, metadata

    except Exception as e:
        logger.error(f"Gemini text generation failed: {e}")
        raise


def _extract_recipe_suggestions(text: str) -> list:
    """Extract recipe names from response text.

    This is a simple extraction - looks for common patterns where recipes
    might be mentioned.
    """
    suggestions = []

    # Common Indian dishes to look for
    common_dishes = [
        "dal",
        "paneer",
        "biryani",
        "curry",
        "masala",
        "tikka",
        "samosa",
        "dosa",
        "idli",
        "paratha",
        "roti",
        "naan",
        "chana",
        "aloo",
        "bhaji",
        "pakora",
        "raita",
        "chutney",
        "halwa",
        "kheer",
        "gulab jamun",
        "ladoo",
        "barfi",
        "palak",
        "bhindi",
        "gobi",
        "matar",
        "rajma",
        "chole",
        "korma",
        "vindaloo",
        "tandoori",
        "kebab",
        "pulao",
    ]

    text_lower = text.lower()

    for dish in common_dishes:
        if dish in text_lower:
            # Try to find the full dish name (e.g., "Palak Paneer" not just "paneer")
            pattern = rf"\b\w*\s*{dish}\s*\w*\b"
            matches = re.findall(pattern, text_lower)
            for match in matches[:3]:  # Limit to first 3 matches
                cleaned = match.strip().title()
                if cleaned and cleaned not in suggestions:
                    suggestions.append(cleaned)

    return suggestions[:5]  # Return at most 5 suggestions
