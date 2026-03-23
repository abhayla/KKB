"""Tests verifying rate limit decorators exist on key endpoints.

slowapi rate limiting is difficult to test directly because the test client
shares the same IP for all requests and the limiter state is global. Instead,
we verify that the @limiter.limit decorators are correctly applied by
inspecting the source code of endpoint modules.

Run with: PYTHONPATH=. pytest tests/api/test_rate_limits.py -v
"""

import inspect

import pytest

from app.api.v1.endpoints import auth, chat, photos, meal_plans, households


def _source_has_rate_limit(module, func_name: str) -> bool:
    """Check if a function in a module has @limiter.limit above it in source."""
    source = inspect.getsource(module)
    lines = source.split("\n")
    for i, line in enumerate(lines):
        if f"def {func_name}" in line:
            # Check the preceding lines for @limiter.limit
            for j in range(max(0, i - 5), i):
                if "@limiter.limit" in lines[j]:
                    return True
            return False
    return False


# ==================== Auth Rate Limits ====================


class TestAuthRateLimits:
    """Verify rate limit decorators on auth endpoints."""

    def test_firebase_auth_has_rate_limit(self):
        """POST /auth/firebase must have a rate limit decorator."""
        assert _source_has_rate_limit(auth, "firebase_auth"), (
            "POST /auth/firebase is missing @limiter.limit decorator"
        )

    def test_refresh_token_has_rate_limit(self):
        """POST /auth/refresh must have a rate limit decorator."""
        assert _source_has_rate_limit(auth, "refresh_token"), (
            "POST /auth/refresh is missing @limiter.limit decorator"
        )


# ==================== Chat Rate Limits ====================


class TestChatRateLimits:
    """Verify rate limit decorators on chat endpoints."""

    def test_send_message_has_rate_limit(self):
        """POST /chat/message must have a rate limit decorator."""
        assert _source_has_rate_limit(chat, "send_message"), (
            "POST /chat/message is missing @limiter.limit decorator"
        )

    def test_send_image_has_rate_limit(self):
        """POST /chat/image must have a rate limit decorator."""
        assert _source_has_rate_limit(chat, "send_image_message"), (
            "POST /chat/image is missing @limiter.limit decorator"
        )


# ==================== Photo Rate Limits ====================


class TestPhotoRateLimits:
    """Verify rate limit decorator on photo analysis endpoint."""

    def test_analyze_photo_has_rate_limit(self):
        """POST /photos/analyze must have a rate limit decorator."""
        assert _source_has_rate_limit(photos, "analyze_photo"), (
            "POST /photos/analyze is missing @limiter.limit decorator"
        )


# ==================== Meal Plan Rate Limits ====================


class TestMealPlanRateLimits:
    """Verify rate limit decorator on meal plan generation endpoint."""

    def test_generate_has_rate_limit(self):
        """POST /meal-plans/generate must have a rate limit decorator."""
        assert _source_has_rate_limit(meal_plans, "generate"), (
            "POST /meal-plans/generate is missing @limiter.limit decorator"
        )


# ==================== Household Rate Limits ====================


class TestHouseholdRateLimits:
    """Verify rate limit decorators on household endpoints."""

    def test_join_household_has_rate_limit(self):
        """POST /households/join must have a rate limit decorator."""
        assert _source_has_rate_limit(households, "join_household"), (
            "POST /households/join is missing @limiter.limit decorator"
        )

    def test_add_member_has_rate_limit(self):
        """POST /households/{id}/members must have a rate limit decorator."""
        assert _source_has_rate_limit(households, "add_member"), (
            "POST /households/{id}/members is missing @limiter.limit decorator"
        )


# ==================== Rate Limit Handler ====================


@pytest.mark.asyncio
async def test_rate_limit_exception_handler_registered(client):
    """Verify the 429 exception handler is registered on the app."""
    from slowapi.errors import RateLimitExceeded
    from app.main import app

    handlers = app.exception_handlers
    assert RateLimitExceeded in handlers, (
        "RateLimitExceeded exception handler not registered on app"
    )
