"""Photo analysis endpoints."""

from typing import Any

from fastapi import APIRouter, File, Request, UploadFile

from app.api.deps import CurrentUser
from app.config import settings
from app.core.rate_limit import limiter
from app.services.photo_analysis_service import analyze_food_photo

router = APIRouter(prefix="/photos", tags=["photos"])

# Magic bytes for image format validation
MAGIC_BYTES = {
    b"\xff\xd8\xff": "image/jpeg",
    b"\x89PNG": "image/png",
    b"RIFF": "image/webp",
}


@router.post("/analyze")
@limiter.limit("100/hour" if settings.debug else "10/hour")
async def analyze_photo(
    request: Request,
    current_user: CurrentUser,
    file: UploadFile = File(..., description="Food photo to analyze (JPEG or PNG)"),
) -> dict[str, Any]:
    """Analyze a food photo using Gemini Vision AI.

    Upload a food photo and get back:
    - Identified food items/dishes
    - Estimated ingredients
    - Cuisine type
    - Estimated nutrition

    Accepts JPEG and PNG images. Max 5MB.
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

    # Limit file size (5MB)
    if len(image_data) > 5 * 1024 * 1024:
        return {
            "identified_foods": [],
            "all_ingredients": [],
            "overall_cuisine": "unknown",
            "overall_meal_type": "unknown",
            "error": "Image too large. Maximum size is 5MB.",
        }

    # Validate magic bytes
    if not _validate_image_magic_bytes(image_data):
        return {
            "identified_foods": [],
            "all_ingredients": [],
            "overall_cuisine": "unknown",
            "overall_meal_type": "unknown",
            "error": "Invalid image file. File header does not match a supported image format.",
        }

    return await analyze_food_photo(image_data, content_type)


def _validate_image_magic_bytes(data: bytes) -> bool:
    """Validate that image data starts with known magic bytes."""
    for magic in MAGIC_BYTES:
        if data[: len(magic)] == magic:
            return True
    return False
