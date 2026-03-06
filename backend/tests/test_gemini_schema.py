"""Tests that generate_text accepts and passes response_schema / response_json_schema."""

import pytest
from unittest.mock import AsyncMock, MagicMock, patch


@pytest.mark.asyncio
async def test_generate_text_passes_schema_to_config():
    """Verify response_schema is included in GenerateContentConfig."""
    from google.genai import types

    mock_schema = types.Schema(
        type="OBJECT",
        properties={"days": types.Schema(type="ARRAY")},
    )

    mock_client = MagicMock()
    mock_response = MagicMock()
    mock_response.text = '{"days": []}'
    mock_client.aio.models.generate_content = AsyncMock(return_value=mock_response)

    with patch("app.ai.gemini_client.get_gemini_client", return_value=mock_client):
        from app.ai.gemini_client import generate_text

        result = await generate_text("test prompt", response_schema=mock_schema)
        assert result == '{"days": []}'

        # Verify schema was passed in config
        call_kwargs = mock_client.aio.models.generate_content.call_args
        config = call_kwargs.kwargs.get("config") or call_kwargs[1].get("config")
        assert config.response_schema == mock_schema


@pytest.mark.asyncio
async def test_generate_text_without_schema_works():
    """Verify generate_text still works without response_schema."""
    mock_client = MagicMock()
    mock_response = MagicMock()
    mock_response.text = '{"days": []}'
    mock_client.aio.models.generate_content = AsyncMock(return_value=mock_response)

    with patch("app.ai.gemini_client.get_gemini_client", return_value=mock_client):
        from app.ai.gemini_client import generate_text

        result = await generate_text("test prompt")
        assert result == '{"days": []}'

        call_kwargs = mock_client.aio.models.generate_content.call_args
        config = call_kwargs.kwargs.get("config") or call_kwargs[1].get("config")
        assert config.response_schema is None


@pytest.mark.asyncio
async def test_generate_text_passes_json_schema_to_config():
    """Verify response_json_schema (plain dict) is forwarded to GenerateContentConfig.

    This is the preferred path for meal plan generation — avoids the Gemini
    'too many states' error that complex genai.types.Schema objects trigger.
    """
    plain_schema = {
        "type": "OBJECT",
        "properties": {"days": {"type": "ARRAY", "items": {"type": "OBJECT"}}},
        "required": ["days"],
    }

    mock_client = MagicMock()
    mock_response = MagicMock()
    mock_response.text = '{"days": []}'
    mock_client.aio.models.generate_content = AsyncMock(return_value=mock_response)

    with patch("app.ai.gemini_client.get_gemini_client", return_value=mock_client):
        from app.ai.gemini_client import generate_text

        result = await generate_text("test prompt", response_json_schema=plain_schema)
        assert result == '{"days": []}'

        call_kwargs = mock_client.aio.models.generate_content.call_args
        config = call_kwargs.kwargs.get("config") or call_kwargs[1].get("config")
        assert config.response_json_schema == plain_schema
        # response_schema should be None when only json_schema is provided
        assert config.response_schema is None


@pytest.mark.asyncio
async def test_generate_text_with_metadata_passes_json_schema():
    """Verify generate_text_with_metadata also forwards response_json_schema."""
    plain_schema = {"type": "OBJECT", "properties": {"days": {"type": "ARRAY"}}}

    mock_client = MagicMock()
    mock_response = MagicMock()
    mock_response.text = '{"days": []}'
    mock_response.usage_metadata = None
    mock_client.aio.models.generate_content = AsyncMock(return_value=mock_response)

    with patch("app.ai.gemini_client.get_gemini_client", return_value=mock_client):
        from app.ai.gemini_client import generate_text_with_metadata

        text, metadata = await generate_text_with_metadata(
            "test prompt", response_json_schema=plain_schema
        )
        assert text == '{"days": []}'

        call_kwargs = mock_client.aio.models.generate_content.call_args
        config = call_kwargs.kwargs.get("config") or call_kwargs[1].get("config")
        assert config.response_json_schema == plain_schema
