"""API endpoint tests for chat.

Tests /chat/message and /chat/history endpoints.

Run with: pytest tests/test_chat_api.py -v
"""

import pytest
from unittest.mock import AsyncMock, patch, MagicMock
from datetime import datetime, timezone
from httpx import ASGITransport, AsyncClient

from app.main import app
from app.schemas.chat import ChatResponse, ChatMessageResponse, ChatHistoryResponse


class TestChatMessageEndpoint:
    """Tests for POST /api/v1/chat/message endpoint."""

    @pytest.fixture
    def mock_auth_and_services(self):
        """Mock authentication and services."""
        # Patch at the endpoint module level where it's imported
        with patch('app.api.deps.verify_token_and_get_user_id') as mock_verify, \
             patch('app.api.deps.UserRepository') as mock_user_repo, \
             patch('app.api.v1.endpoints.chat.process_chat_message') as mock_process:

            # Configure auth mock
            mock_verify.return_value = "test-user-id"

            # Configure user repo mock
            mock_user_repo.return_value.get_by_id = AsyncMock(return_value={
                "id": "test-user-id",
                "firebase_uid": "test-firebase-uid",
                "email": "test@example.com",
                "is_active": True,
            })

            # Configure chat processing mock - needs to be AsyncMock since it's awaited
            mock_process.return_value = ChatResponse(
                message=ChatMessageResponse(
                    id="msg_123",
                    role="assistant",
                    content="Namaste! How can I help you today?",
                    message_type="text",
                    created_at=datetime.now(timezone.utc).isoformat(),
                    recipe_suggestions=None,
                ),
                has_recipe_suggestions=False,
                recipe_ids=[],
            )

            yield {
                "verify": mock_verify,
                "user_repo": mock_user_repo,
                "process": mock_process,
            }

    @pytest.mark.asyncio
    async def test_send_message_success(self, mock_auth_and_services):
        """Test successful message send."""
        async with AsyncClient(
            transport=ASGITransport(app=app),
            base_url="http://test",
        ) as client:
            response = await client.post(
                "/api/v1/chat/message",
                json={"message": "Hello!"},
                headers={"Authorization": "Bearer valid-token"},
            )

        assert response.status_code == 200
        data = response.json()
        assert data["message"]["role"] == "assistant"
        assert data["message"]["content"] is not None

    @pytest.mark.asyncio
    async def test_send_message_empty_fails(self, mock_auth_and_services):
        """Test that empty message fails validation."""
        async with AsyncClient(
            transport=ASGITransport(app=app),
            base_url="http://test",
        ) as client:
            response = await client.post(
                "/api/v1/chat/message",
                json={"message": ""},
                headers={"Authorization": "Bearer valid-token"},
            )

        assert response.status_code == 422  # Validation error

    @pytest.mark.asyncio
    async def test_send_message_no_auth(self):
        """Test that missing auth returns 401."""
        async with AsyncClient(
            transport=ASGITransport(app=app),
            base_url="http://test",
        ) as client:
            response = await client.post(
                "/api/v1/chat/message",
                json={"message": "Hello!"},
            )

        assert response.status_code == 401

    @pytest.mark.asyncio
    async def test_send_message_tool_response(self, mock_auth_and_services):
        """Test message that triggers tool usage."""
        # Configure mock to return tool-triggered response
        mock_auth_and_services["process"].return_value = ChatResponse(
            message=ChatMessageResponse(
                id="msg_456",
                role="assistant",
                content="Done! I've added chai to your breakfast every morning.",
                message_type="text",
                created_at=datetime.now(timezone.utc).isoformat(),
                recipe_suggestions=None,
            ),
            has_recipe_suggestions=False,
            recipe_ids=[],
        )

        async with AsyncClient(
            transport=ASGITransport(app=app),
            base_url="http://test",
        ) as client:
            response = await client.post(
                "/api/v1/chat/message",
                json={"message": "I want chai every morning"},
                headers={"Authorization": "Bearer valid-token"},
            )

        assert response.status_code == 200
        data = response.json()
        assert "chai" in data["message"]["content"].lower()


class TestChatHistoryEndpoint:
    """Tests for GET /api/v1/chat/history endpoint."""

    @pytest.fixture
    def mock_auth_and_history(self):
        """Mock authentication and history service."""
        # Patch at the endpoint module level where it's imported
        with patch('app.api.deps.verify_token_and_get_user_id') as mock_verify, \
             patch('app.api.deps.UserRepository') as mock_user_repo, \
             patch('app.api.v1.endpoints.chat.get_chat_history') as mock_history:

            # Configure auth mock
            mock_verify.return_value = "test-user-id"

            # Configure user repo mock
            mock_user_repo.return_value.get_by_id = AsyncMock(return_value={
                "id": "test-user-id",
                "firebase_uid": "test-firebase-uid",
                "email": "test@example.com",
                "is_active": True,
            })

            # Configure history mock
            mock_history.return_value = [
                {
                    "id": "msg_1",
                    "role": "user",
                    "content": "Hello!",
                    "message_type": "text",
                    "created_at": datetime.now(timezone.utc).isoformat(),
                },
                {
                    "id": "msg_2",
                    "role": "assistant",
                    "content": "Namaste! How can I help you?",
                    "message_type": "text",
                    "created_at": datetime.now(timezone.utc).isoformat(),
                },
            ]

            yield {
                "verify": mock_verify,
                "user_repo": mock_user_repo,
                "history": mock_history,
            }

    @pytest.mark.asyncio
    async def test_get_history_success(self, mock_auth_and_history):
        """Test successful history retrieval."""
        async with AsyncClient(
            transport=ASGITransport(app=app),
            base_url="http://test",
        ) as client:
            response = await client.get(
                "/api/v1/chat/history",
                headers={"Authorization": "Bearer valid-token"},
            )

        assert response.status_code == 200
        data = response.json()
        assert "messages" in data
        assert len(data["messages"]) == 2
        assert data["total_count"] == 2

    @pytest.mark.asyncio
    async def test_get_history_no_auth(self):
        """Test that missing auth returns 401."""
        async with AsyncClient(
            transport=ASGITransport(app=app),
            base_url="http://test",
        ) as client:
            response = await client.get("/api/v1/chat/history")

        assert response.status_code == 401

    @pytest.mark.asyncio
    async def test_get_history_empty(self, mock_auth_and_history):
        """Test empty history."""
        mock_auth_and_history["history"].return_value = []

        async with AsyncClient(
            transport=ASGITransport(app=app),
            base_url="http://test",
        ) as client:
            response = await client.get(
                "/api/v1/chat/history",
                headers={"Authorization": "Bearer valid-token"},
            )

        assert response.status_code == 200
        data = response.json()
        assert data["messages"] == []
        assert data["total_count"] == 0


class TestChatSchemas:
    """Tests for chat request/response schemas."""

    def test_chat_message_request_validation(self):
        """Test ChatMessageRequest validation."""
        from app.schemas.chat import ChatMessageRequest

        # Valid message
        request = ChatMessageRequest(message="Hello!")
        assert request.message == "Hello!"

        # Message too short (empty)
        with pytest.raises(ValueError):
            ChatMessageRequest(message="")

    def test_chat_message_response_structure(self):
        """Test ChatMessageResponse structure."""
        response = ChatMessageResponse(
            id="msg_123",
            role="assistant",
            content="Hello!",
            message_type="text",
            created_at="2026-01-27T10:00:00Z",
            recipe_suggestions=None,
        )

        assert response.id == "msg_123"
        assert response.role == "assistant"
        assert response.content == "Hello!"
        assert response.message_type == "text"

    def test_chat_response_structure(self):
        """Test ChatResponse structure."""
        response = ChatResponse(
            message=ChatMessageResponse(
                id="msg_123",
                role="assistant",
                content="Hello!",
                message_type="text",
                created_at="2026-01-27T10:00:00Z",
                recipe_suggestions=None,
            ),
            has_recipe_suggestions=False,
            recipe_ids=[],
        )

        assert response.message.id == "msg_123"
        assert response.has_recipe_suggestions is False
        assert response.recipe_ids == []

    def test_chat_history_response_structure(self):
        """Test ChatHistoryResponse structure."""
        response = ChatHistoryResponse(
            messages=[
                ChatMessageResponse(
                    id="msg_1",
                    role="user",
                    content="Hello!",
                    message_type="text",
                    created_at="2026-01-27T10:00:00Z",
                    recipe_suggestions=None,
                ),
            ],
            total_count=1,
        )

        assert len(response.messages) == 1
        assert response.total_count == 1


class TestChatEndpointDocumentation:
    """Tests for chat endpoint documentation."""

    @pytest.mark.asyncio
    async def test_openapi_has_chat_endpoints(self):
        """Test that OpenAPI schema includes chat endpoints."""
        async with AsyncClient(
            transport=ASGITransport(app=app),
            base_url="http://test",
        ) as client:
            response = await client.get("/openapi.json")

        assert response.status_code == 200
        openapi = response.json()

        # Check that chat endpoints exist
        assert "/api/v1/chat/message" in openapi["paths"]
        assert "/api/v1/chat/history" in openapi["paths"]

        # Check that POST method is documented
        assert "post" in openapi["paths"]["/api/v1/chat/message"]

        # Check that GET method is documented
        assert "get" in openapi["paths"]["/api/v1/chat/history"]
