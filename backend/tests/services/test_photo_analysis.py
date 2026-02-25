"""Tests for food photo analysis service.

Verifies the photo analysis service correctly processes images
and returns structured food identification data.
Uses mocked Gemini responses.
"""

import json
import pytest
from unittest.mock import AsyncMock, MagicMock, patch

from app.services.photo_analysis_service import analyze_food_photo


class TestPhotoAnalysis:
    """Test food photo analysis with mocked Gemini."""

    @pytest.mark.asyncio
    @patch("app.services.photo_analysis_service.get_gemini_client")
    async def test_successful_analysis(self, mock_get_client):
        """Successful photo analysis returns identified foods."""
        mock_response = MagicMock()
        mock_response.text = json.dumps({
            "identified_foods": [
                {
                    "name": "Dal Tadka",
                    "confidence": 0.95,
                    "cuisine": "North Indian",
                    "meal_type": "lunch",
                    "estimated_ingredients": ["toor dal", "ghee", "cumin"],
                    "estimated_calories": 250,
                    "estimated_nutrition": {"protein_g": 12, "carbs_g": 35, "fat_g": 8},
                }
            ],
            "all_ingredients": ["toor dal", "ghee", "cumin", "turmeric"],
            "overall_cuisine": "North Indian",
            "overall_meal_type": "lunch",
        })

        mock_client = MagicMock()
        mock_client.aio.models.generate_content = AsyncMock(return_value=mock_response)
        mock_get_client.return_value = mock_client

        result = await analyze_food_photo(b"fake-image-data", "image/jpeg")

        assert len(result["identified_foods"]) == 1
        assert result["identified_foods"][0]["name"] == "Dal Tadka"
        assert result["overall_cuisine"] == "North Indian"
        assert "toor dal" in result["all_ingredients"]

    @pytest.mark.asyncio
    @patch("app.services.photo_analysis_service.get_gemini_client")
    async def test_no_client_returns_error(self, mock_get_client):
        """Returns error when Gemini client not configured."""
        mock_get_client.return_value = None

        result = await analyze_food_photo(b"fake-image-data")

        assert result["error"] == "AI vision service is not configured"
        assert len(result["identified_foods"]) == 0

    @pytest.mark.asyncio
    @patch("app.services.photo_analysis_service.get_gemini_client")
    async def test_invalid_json_response(self, mock_get_client):
        """Invalid JSON from Gemini returns error result."""
        mock_response = MagicMock()
        mock_response.text = "This is not JSON"

        mock_client = MagicMock()
        mock_client.aio.models.generate_content = AsyncMock(return_value=mock_response)
        mock_get_client.return_value = mock_client

        result = await analyze_food_photo(b"fake-image-data")

        assert "error" in result
        assert len(result["identified_foods"]) == 0

    @pytest.mark.asyncio
    @patch("app.services.photo_analysis_service.get_gemini_client")
    async def test_markdown_code_blocks_stripped(self, mock_get_client):
        """Markdown code blocks are stripped from response."""
        inner_json = json.dumps({
            "identified_foods": [{"name": "Samosa", "confidence": 0.8}],
            "all_ingredients": ["potato", "maida"],
            "overall_cuisine": "North Indian",
            "overall_meal_type": "snack",
        })
        mock_response = MagicMock()
        mock_response.text = f"```json\n{inner_json}\n```"

        mock_client = MagicMock()
        mock_client.aio.models.generate_content = AsyncMock(return_value=mock_response)
        mock_get_client.return_value = mock_client

        result = await analyze_food_photo(b"fake-image-data")

        assert result["identified_foods"][0]["name"] == "Samosa"

    @pytest.mark.asyncio
    @patch("app.services.photo_analysis_service.get_gemini_client")
    async def test_api_exception_returns_error(self, mock_get_client):
        """Gemini API exceptions return error dict instead of propagating."""
        mock_client = MagicMock()
        mock_client.aio.models.generate_content = AsyncMock(
            side_effect=Exception("API quota exceeded")
        )
        mock_get_client.return_value = mock_client

        result = await analyze_food_photo(b"fake-image-data")

        assert "error" in result
        assert result["error"] == "Photo analysis failed. Please try again later."
        assert len(result["identified_foods"]) == 0
