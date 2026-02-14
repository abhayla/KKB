"""Photo analysis endpoints."""

from typing import Any

from fastapi import APIRouter, File, UploadFile

from app.api.deps import CurrentUser
from app.services.photo_analysis_service import analyze_food_photo

router = APIRouter(prefix="/photos", tags=["photos"])


@router.post("/analyze")
async def analyze_photo(
    current_user: CurrentUser,
    file: UploadFile = File(..., description="Food photo to analyze (JPEG or PNG)"),
) -> dict[str, Any]:
    """Analyze a food photo using Gemini Vision AI.

    Upload a food photo and get back:
    - Identified food items/dishes
    - Estimated ingredients
    - Cuisine type
    - Estimated nutrition

    Accepts JPEG and PNG images.
    """
    # Validate file type
    allowed_types = {"image/jpeg", "image/png", "image/jpg"}
    content_type = file.content_type or "image/jpeg"
    if content_type not in allowed_types:
        return {
            "identified_foods": [],
            "all_ingredients": [],
            "overall_cuisine": "unknown",
            "overall_meal_type": "unknown",
            "error": f"Unsupported file type: {content_type}. Use JPEG or PNG.",
        }

    image_data = await file.read()

    # Limit file size (10MB)
    if len(image_data) > 10 * 1024 * 1024:
        return {
            "identified_foods": [],
            "all_ingredients": [],
            "overall_cuisine": "unknown",
            "overall_meal_type": "unknown",
            "error": "Image too large. Maximum size is 10MB.",
        }

    return await analyze_food_photo(image_data, content_type)
