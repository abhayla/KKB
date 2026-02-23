"""Tests for security headers middleware and rate limiting."""

import pytest


class TestSecurityHeaders:
    """Test that security headers are present on responses."""

    async def test_health_has_security_headers(self, client):
        """Health endpoint should include security headers."""
        response = await client.get("/health")
        assert response.headers.get("X-Content-Type-Options") == "nosniff"
        assert response.headers.get("X-Frame-Options") == "DENY"
        assert response.headers.get("X-XSS-Protection") == "1; mode=block"
        assert response.headers.get("Referrer-Policy") == "strict-origin-when-cross-origin"

    async def test_api_version_header(self, client):
        """All responses should include X-API-Version header."""
        response = await client.get("/health")
        assert response.headers.get("X-API-Version") == "1.0"

    async def test_api_endpoint_has_security_headers(self, client):
        """API endpoints should include security headers."""
        response = await client.get("/api/v1/users/me")
        assert response.headers.get("X-Content-Type-Options") == "nosniff"
        assert response.headers.get("X-Frame-Options") == "DENY"


class TestImageValidation:
    """Test image magic byte validation in photos endpoint."""

    async def test_photos_rejects_non_image(self, client):
        """Photo upload should reject non-image files based on magic bytes."""
        import io

        # Send a text file disguised as an image
        fake_image = io.BytesIO(b"This is not an image")
        response = await client.post(
            "/api/v1/photos/analyze",
            files={"file": ("test.png", fake_image, "image/png")},
        )
        assert response.status_code == 200  # Returns 200 with error field
        data = response.json()
        assert "error" in data
        assert "Invalid image file" in data["error"]

    async def test_photos_rejects_oversized(self, client):
        """Photo upload should reject files over 5MB."""
        import io

        # PNG magic bytes + 6MB of zeros
        large_image = io.BytesIO(b"\x89PNG" + b"\x00" * (6 * 1024 * 1024))
        response = await client.post(
            "/api/v1/photos/analyze",
            files={"file": ("large.png", large_image, "image/png")},
        )
        assert response.status_code == 200
        data = response.json()
        assert "error" in data
        assert "too large" in data["error"]
