"""API endpoint tests for photo analysis.

Tests POST /api/v1/photos/analyze endpoint — Gemini Vision food photo analysis.

Run with: PYTHONPATH=. pytest tests/api/test_photos_api.py -v
"""

import json
from io import BytesIO
from unittest.mock import AsyncMock, patch

import pytest
from httpx import AsyncClient


# JPEG magic bytes: FF D8 FF + some valid-looking padding
FAKE_JPEG_DATA = b"\xff\xd8\xff\xe0" + b"\x00" * 100
# PNG magic bytes: 89 50 4e 47 (hex for \x89PNG)
FAKE_PNG_DATA = b"\x89PNG\r\n\x1a\n" + b"\x00" * 100
# WebP magic bytes: RIFF....WEBP
FAKE_WEBP_DATA = b"RIFF\x00\x00\x00\x00WEBP" + b"\x00" * 100

MOCK_ANALYSIS_RESULT = {
    "identified_foods": [
        {
            "name": "Paneer Butter Masala",
            "confidence": 0.92,
            "cuisine": "North Indian",
            "meal_type": "dinner",
            "estimated_ingredients": [
                "paneer",
                "tomato",
                "butter",
                "cream",
                "spices",
            ],
            "estimated_calories": 380,
            "estimated_nutrition": {
                "protein_g": 18,
                "carbs_g": 12,
                "fat_g": 28,
            },
        }
    ],
    "all_ingredients": ["paneer", "tomato", "butter", "cream", "spices"],
    "overall_cuisine": "North Indian",
    "overall_meal_type": "dinner",
}

MOCK_MULTI_FOOD_RESULT = {
    "identified_foods": [
        {
            "name": "Dal Tadka",
            "confidence": 0.88,
            "cuisine": "North Indian",
            "meal_type": "lunch",
            "estimated_ingredients": ["toor dal", "cumin", "garlic", "ghee"],
            "estimated_calories": 220,
            "estimated_nutrition": {"protein_g": 12, "carbs_g": 35, "fat_g": 8},
        },
        {
            "name": "Jeera Rice",
            "confidence": 0.90,
            "cuisine": "North Indian",
            "meal_type": "lunch",
            "estimated_ingredients": ["basmati rice", "cumin seeds", "ghee"],
            "estimated_calories": 250,
            "estimated_nutrition": {"protein_g": 5, "carbs_g": 50, "fat_g": 6},
        },
    ],
    "all_ingredients": [
        "toor dal",
        "cumin",
        "garlic",
        "ghee",
        "basmati rice",
        "cumin seeds",
    ],
    "overall_cuisine": "North Indian",
    "overall_meal_type": "lunch",
}

MOCK_NO_FOOD_RESULT = {
    "identified_foods": [],
    "all_ingredients": [],
    "overall_cuisine": "unknown",
    "overall_meal_type": "unknown",
}


# ==================== Success Tests ====================


@pytest.mark.asyncio
async def test_analyze_photo_success(client: AsyncClient):
    """Upload valid JPEG, mock Gemini, verify response fields."""
    with patch(
        "app.api.v1.endpoints.photos.analyze_food_photo",
        new=AsyncMock(return_value=MOCK_ANALYSIS_RESULT),
    ):
        response = await client.post(
            "/api/v1/photos/analyze",
            files=[("file", ("test.jpg", BytesIO(FAKE_JPEG_DATA), "image/jpeg"))],
        )

    assert response.status_code == 200
    data = response.json()
    assert "identified_foods" in data
    assert "all_ingredients" in data
    assert "overall_cuisine" in data
    assert "overall_meal_type" in data
    assert len(data["identified_foods"]) == 1
    assert data["identified_foods"][0]["name"] == "Paneer Butter Masala"


@pytest.mark.asyncio
async def test_analyze_photo_png_success(client: AsyncClient):
    """Upload valid PNG image, verify analysis works."""
    with patch(
        "app.api.v1.endpoints.photos.analyze_food_photo",
        new=AsyncMock(return_value=MOCK_ANALYSIS_RESULT),
    ):
        response = await client.post(
            "/api/v1/photos/analyze",
            files=[("file", ("test.png", BytesIO(FAKE_PNG_DATA), "image/png"))],
        )

    assert response.status_code == 200
    data = response.json()
    assert len(data["identified_foods"]) == 1
    assert data["overall_cuisine"] == "North Indian"


@pytest.mark.asyncio
async def test_analyze_photo_response_structure(client: AsyncClient):
    """Verify all expected keys are present in a successful response."""
    with patch(
        "app.api.v1.endpoints.photos.analyze_food_photo",
        new=AsyncMock(return_value=MOCK_ANALYSIS_RESULT),
    ):
        response = await client.post(
            "/api/v1/photos/analyze",
            files=[("file", ("food.jpg", BytesIO(FAKE_JPEG_DATA), "image/jpeg"))],
        )

    assert response.status_code == 200
    data = response.json()

    # Top-level keys
    assert "identified_foods" in data
    assert "all_ingredients" in data
    assert "overall_cuisine" in data
    assert "overall_meal_type" in data

    # Food item structure
    food = data["identified_foods"][0]
    assert "name" in food
    assert "confidence" in food
    assert "cuisine" in food
    assert "meal_type" in food
    assert "estimated_ingredients" in food
    assert "estimated_calories" in food
    assert "estimated_nutrition" in food

    # Nutrition sub-structure
    nutrition = food["estimated_nutrition"]
    assert "protein_g" in nutrition
    assert "carbs_g" in nutrition
    assert "fat_g" in nutrition


# ==================== Authentication Tests ====================


@pytest.mark.asyncio
async def test_analyze_photo_unauthenticated(unauthenticated_client: AsyncClient):
    """Unauthenticated request returns 401."""
    response = await unauthenticated_client.post(
        "/api/v1/photos/analyze",
        files=[("file", ("test.jpg", BytesIO(FAKE_JPEG_DATA), "image/jpeg"))],
    )

    assert response.status_code == 401


# ==================== Validation Tests ====================


@pytest.mark.asyncio
async def test_analyze_photo_invalid_file_type(client: AsyncClient):
    """Upload non-image file (.txt), expect error in response."""
    response = await client.post(
        "/api/v1/photos/analyze",
        files=[
            ("file", ("notes.txt", BytesIO(b"This is a text file"), "text/plain"))
        ],
    )

    assert response.status_code == 200
    data = response.json()
    assert "error" in data
    assert "Unsupported file type" in data["error"]
    assert data["identified_foods"] == []
    assert data["all_ingredients"] == []
    assert data["overall_cuisine"] == "unknown"
    assert data["overall_meal_type"] == "unknown"


@pytest.mark.asyncio
async def test_analyze_photo_empty_file(client: AsyncClient):
    """Upload empty file, expect magic bytes validation failure."""
    response = await client.post(
        "/api/v1/photos/analyze",
        files=[("file", ("empty.jpg", BytesIO(b""), "image/jpeg"))],
    )

    assert response.status_code == 200
    data = response.json()
    assert "error" in data
    assert data["identified_foods"] == []


@pytest.mark.asyncio
async def test_analyze_photo_oversized(client: AsyncClient):
    """Upload file > 5MB, expect size limit error."""
    # 5MB + 1 byte
    oversized_data = FAKE_JPEG_DATA + b"\x00" * (5 * 1024 * 1024 + 1)

    response = await client.post(
        "/api/v1/photos/analyze",
        files=[
            ("file", ("huge.jpg", BytesIO(oversized_data), "image/jpeg"))
        ],
    )

    assert response.status_code == 200
    data = response.json()
    assert "error" in data
    assert "5MB" in data["error"]
    assert data["identified_foods"] == []
    assert data["overall_cuisine"] == "unknown"


@pytest.mark.asyncio
async def test_analyze_photo_invalid_magic_bytes(client: AsyncClient):
    """Upload file with image content-type but wrong magic bytes."""
    # Looks like JPEG by content-type but has garbage bytes
    bad_data = b"\x00\x01\x02\x03" + b"\x00" * 100

    response = await client.post(
        "/api/v1/photos/analyze",
        files=[("file", ("fake.jpg", BytesIO(bad_data), "image/jpeg"))],
    )

    assert response.status_code == 200
    data = response.json()
    assert "error" in data
    assert "Invalid image file" in data["error"]
    assert data["identified_foods"] == []


# ==================== Gemini Error Handling Tests ====================


@pytest.mark.asyncio
async def test_analyze_photo_gemini_error(client: AsyncClient):
    """Gemini service returns error dict on failure."""
    error_result = {
        "identified_foods": [],
        "all_ingredients": [],
        "overall_cuisine": "unknown",
        "overall_meal_type": "unknown",
        "error": "Photo analysis failed. Please try again later.",
    }

    with patch(
        "app.api.v1.endpoints.photos.analyze_food_photo",
        new=AsyncMock(return_value=error_result),
    ):
        response = await client.post(
            "/api/v1/photos/analyze",
            files=[("file", ("test.jpg", BytesIO(FAKE_JPEG_DATA), "image/jpeg"))],
        )

    assert response.status_code == 200
    data = response.json()
    assert "error" in data
    assert "failed" in data["error"].lower()
    assert data["identified_foods"] == []


@pytest.mark.asyncio
async def test_analyze_photo_gemini_timeout(client: AsyncClient):
    """Gemini times out, service catches and returns error dict."""
    import asyncio

    async def timeout_side_effect(*args, **kwargs):
        raise asyncio.TimeoutError("Gemini request timed out")

    # The service catches Exception and returns an error dict,
    # so we mock the service to simulate its timeout handling behavior
    timeout_result = {
        "identified_foods": [],
        "all_ingredients": [],
        "overall_cuisine": "unknown",
        "overall_meal_type": "unknown",
        "error": "Photo analysis failed. Please try again later.",
    }

    with patch(
        "app.api.v1.endpoints.photos.analyze_food_photo",
        new=AsyncMock(return_value=timeout_result),
    ):
        response = await client.post(
            "/api/v1/photos/analyze",
            files=[("file", ("test.jpg", BytesIO(FAKE_JPEG_DATA), "image/jpeg"))],
        )

    assert response.status_code == 200
    data = response.json()
    assert "error" in data
    assert data["identified_foods"] == []


@pytest.mark.asyncio
async def test_analyze_photo_gemini_not_configured(client: AsyncClient):
    """Gemini client not configured returns service unavailable error."""
    not_configured_result = {
        "identified_foods": [],
        "all_ingredients": [],
        "overall_cuisine": "unknown",
        "overall_meal_type": "unknown",
        "error": "AI vision service is not configured",
    }

    with patch(
        "app.api.v1.endpoints.photos.analyze_food_photo",
        new=AsyncMock(return_value=not_configured_result),
    ):
        response = await client.post(
            "/api/v1/photos/analyze",
            files=[("file", ("test.jpg", BytesIO(FAKE_JPEG_DATA), "image/jpeg"))],
        )

    assert response.status_code == 200
    data = response.json()
    assert "error" in data
    assert "not configured" in data["error"]


@pytest.mark.asyncio
async def test_analyze_photo_gemini_parse_error(client: AsyncClient):
    """Gemini returns unparseable response, service catches JSONDecodeError."""
    parse_error_result = {
        "identified_foods": [],
        "all_ingredients": [],
        "overall_cuisine": "unknown",
        "overall_meal_type": "unknown",
        "error": "Failed to parse AI response",
    }

    with patch(
        "app.api.v1.endpoints.photos.analyze_food_photo",
        new=AsyncMock(return_value=parse_error_result),
    ):
        response = await client.post(
            "/api/v1/photos/analyze",
            files=[("file", ("test.jpg", BytesIO(FAKE_JPEG_DATA), "image/jpeg"))],
        )

    assert response.status_code == 200
    data = response.json()
    assert "error" in data
    assert "parse" in data["error"].lower()


# ==================== Food Detection Tests ====================


@pytest.mark.asyncio
async def test_analyze_photo_identifies_indian_food(client: AsyncClient):
    """Verify Indian cuisine detection in analysis results."""
    with patch(
        "app.api.v1.endpoints.photos.analyze_food_photo",
        new=AsyncMock(return_value=MOCK_ANALYSIS_RESULT),
    ):
        response = await client.post(
            "/api/v1/photos/analyze",
            files=[("file", ("curry.jpg", BytesIO(FAKE_JPEG_DATA), "image/jpeg"))],
        )

    assert response.status_code == 200
    data = response.json()
    assert data["overall_cuisine"] == "North Indian"
    assert data["identified_foods"][0]["cuisine"] == "North Indian"


@pytest.mark.asyncio
async def test_analyze_photo_returns_ingredients_list(client: AsyncClient):
    """Verify ingredients list is populated in response."""
    with patch(
        "app.api.v1.endpoints.photos.analyze_food_photo",
        new=AsyncMock(return_value=MOCK_ANALYSIS_RESULT),
    ):
        response = await client.post(
            "/api/v1/photos/analyze",
            files=[("file", ("food.jpg", BytesIO(FAKE_JPEG_DATA), "image/jpeg"))],
        )

    assert response.status_code == 200
    data = response.json()
    assert isinstance(data["all_ingredients"], list)
    assert len(data["all_ingredients"]) > 0
    assert "paneer" in data["all_ingredients"]
    assert "tomato" in data["all_ingredients"]

    # Per-food ingredients too
    food = data["identified_foods"][0]
    assert isinstance(food["estimated_ingredients"], list)
    assert len(food["estimated_ingredients"]) > 0


@pytest.mark.asyncio
async def test_analyze_photo_returns_cuisine_type(client: AsyncClient):
    """Verify cuisine classification in response."""
    with patch(
        "app.api.v1.endpoints.photos.analyze_food_photo",
        new=AsyncMock(return_value=MOCK_ANALYSIS_RESULT),
    ):
        response = await client.post(
            "/api/v1/photos/analyze",
            files=[("file", ("thali.jpg", BytesIO(FAKE_JPEG_DATA), "image/jpeg"))],
        )

    assert response.status_code == 200
    data = response.json()
    assert data["overall_cuisine"] != "unknown"
    assert isinstance(data["overall_cuisine"], str)


@pytest.mark.asyncio
async def test_analyze_photo_returns_meal_type(client: AsyncClient):
    """Verify meal type classification (breakfast/lunch/dinner/snack)."""
    with patch(
        "app.api.v1.endpoints.photos.analyze_food_photo",
        new=AsyncMock(return_value=MOCK_ANALYSIS_RESULT),
    ):
        response = await client.post(
            "/api/v1/photos/analyze",
            files=[("file", ("dinner.jpg", BytesIO(FAKE_JPEG_DATA), "image/jpeg"))],
        )

    assert response.status_code == 200
    data = response.json()
    assert data["overall_meal_type"] in {"breakfast", "lunch", "dinner", "snack"}


@pytest.mark.asyncio
async def test_analyze_photo_multiple_foods(client: AsyncClient):
    """Image with multiple dishes returns multiple identified foods."""
    with patch(
        "app.api.v1.endpoints.photos.analyze_food_photo",
        new=AsyncMock(return_value=MOCK_MULTI_FOOD_RESULT),
    ):
        response = await client.post(
            "/api/v1/photos/analyze",
            files=[("file", ("thali.jpg", BytesIO(FAKE_JPEG_DATA), "image/jpeg"))],
        )

    assert response.status_code == 200
    data = response.json()
    assert len(data["identified_foods"]) == 2

    food_names = [f["name"] for f in data["identified_foods"]]
    assert "Dal Tadka" in food_names
    assert "Jeera Rice" in food_names

    # All ingredients is the union across dishes
    assert len(data["all_ingredients"]) > 2


@pytest.mark.asyncio
async def test_analyze_photo_no_food_detected(client: AsyncClient):
    """Image with no food returns empty results (not an error)."""
    with patch(
        "app.api.v1.endpoints.photos.analyze_food_photo",
        new=AsyncMock(return_value=MOCK_NO_FOOD_RESULT),
    ):
        response = await client.post(
            "/api/v1/photos/analyze",
            files=[("file", ("desk.jpg", BytesIO(FAKE_JPEG_DATA), "image/jpeg"))],
        )

    assert response.status_code == 200
    data = response.json()
    assert data["identified_foods"] == []
    assert data["all_ingredients"] == []
    assert data["overall_cuisine"] == "unknown"
    assert data["overall_meal_type"] == "unknown"
    # No "error" key — empty results is a valid outcome
    assert "error" not in data


# ==================== Service Integration Tests ====================


@pytest.mark.asyncio
async def test_analyze_photo_passes_correct_mime_type(client: AsyncClient):
    """Verify the endpoint passes the correct MIME type to the service."""
    mock_fn = AsyncMock(return_value=MOCK_ANALYSIS_RESULT)

    with patch(
        "app.api.v1.endpoints.photos.analyze_food_photo",
        new=mock_fn,
    ):
        await client.post(
            "/api/v1/photos/analyze",
            files=[("file", ("test.png", BytesIO(FAKE_PNG_DATA), "image/png"))],
        )

    mock_fn.assert_called_once()
    call_args = mock_fn.call_args
    # First positional arg is image_data (bytes), second is content_type
    assert call_args[0][0] == FAKE_PNG_DATA
    assert call_args[0][1] == "image/png"


@pytest.mark.asyncio
async def test_analyze_photo_passes_image_data_to_service(client: AsyncClient):
    """Verify the endpoint reads file data and passes it to the service."""
    mock_fn = AsyncMock(return_value=MOCK_ANALYSIS_RESULT)

    with patch(
        "app.api.v1.endpoints.photos.analyze_food_photo",
        new=mock_fn,
    ):
        await client.post(
            "/api/v1/photos/analyze",
            files=[("file", ("test.jpg", BytesIO(FAKE_JPEG_DATA), "image/jpeg"))],
        )

    mock_fn.assert_called_once()
    image_data_arg = mock_fn.call_args[0][0]
    assert isinstance(image_data_arg, bytes)
    assert image_data_arg == FAKE_JPEG_DATA
