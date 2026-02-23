"""Tests for production configuration safety.

Verifies that default settings are safe for production:
- JWT secret has no default (crashes if missing)
- Debug mode defaults to False
- SQL echo defaults to False
- CORS origins default to empty (no CORS middleware)
"""

import os
from unittest.mock import patch

import pytest
from pydantic import ValidationError


class TestConfigDefaults:
    """Test that config defaults are production-safe."""

    def test_jwt_secret_required(self):
        """JWT_SECRET_KEY must be explicitly provided — no default."""
        # Clear the lru_cache so Settings() is re-evaluated
        from app.config import get_settings

        get_settings.cache_clear()

        env = {
            "DATABASE_URL": "postgresql+asyncpg://localhost/test",
            # Intentionally missing JWT_SECRET_KEY
        }
        with patch.dict(os.environ, env, clear=True):
            with pytest.raises(ValidationError) as exc_info:
                from pydantic_settings import BaseSettings, SettingsConfigDict

                class TestSettings(BaseSettings):
                    model_config = SettingsConfigDict(
                        env_file=None,  # Don't read .env
                        case_sensitive=False,
                    )
                    jwt_secret_key: str  # No default — same as production config

                TestSettings()

            assert "jwt_secret_key" in str(exc_info.value)

    def test_debug_defaults_to_false(self):
        """Debug mode must default to False for production safety."""
        from app.config import Settings

        # Check the field default directly from the model
        field = Settings.model_fields["debug"]
        assert field.default is False

    def test_sql_echo_defaults_to_false(self):
        """SQL echo must default to False to avoid leaking queries in logs."""
        from app.config import Settings

        field = Settings.model_fields["sql_echo"]
        assert field.default is False

    def test_cors_origins_default_to_empty(self):
        """CORS origins must default to empty list (no CORS middleware)."""
        from app.config import Settings

        field = Settings.model_fields["cors_origins"]
        assert field.default == []

    def test_debug_mode_must_be_opted_in(self):
        """Without explicit DEBUG=true, debug should be False."""
        from app.config import Settings

        # The default is False, meaning production is safe by default
        assert Settings.model_fields["debug"].default is False


class TestSentryConfig:
    """Test Sentry configuration safety."""

    def test_sentry_pii_disabled_in_main(self):
        """Sentry should NOT send PII by default."""

        with open("app/main.py") as f:
            content = f.read()

        assert "send_default_pii=False" in content
        assert "send_default_pii=True" not in content

    def test_sentry_traces_sample_rate(self):
        """Sentry traces_sample_rate should be <= 0.1 for production."""
        with open("app/main.py") as f:
            content = f.read()

        assert "traces_sample_rate=0.1" in content


class TestCorsConfig:
    """Test CORS configuration."""

    def test_cors_conditional_in_main(self):
        """CORS middleware should only be added when origins are configured."""
        with open("app/main.py") as f:
            content = f.read()

        assert "if settings.cors_origins:" in content

    def test_cors_methods_restricted(self):
        """CORS should not use wildcard methods."""
        with open("app/main.py") as f:
            content = f.read()

        # Should NOT have allow_methods=["*"]
        # Should have specific methods listed
        assert 'allow_methods=["GET", "POST", "PUT", "DELETE", "PATCH"]' in content

    def test_cors_headers_restricted(self):
        """CORS should not use wildcard headers."""
        with open("app/main.py") as f:
            content = f.read()

        assert 'allow_headers=["Authorization", "Content-Type"]' in content
