"""Application configuration using Pydantic Settings."""

from functools import lru_cache
from typing import Optional

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Application settings loaded from environment variables."""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
    )

    # Database
    database_url: str = "postgresql+asyncpg://postgres:password@localhost:5432/rasoiai"

    # Firebase
    firebase_credentials_path: Optional[str] = None

    # JWT
    jwt_secret_key: str  # No default — crashes if missing (security: prevents running with weak secret)
    jwt_algorithm: str = "HS256"
    access_token_expire_minutes: int = 10080  # 7 days

    # Anthropic Claude
    anthropic_api_key: Optional[str] = None

    # Google AI (Gemini)
    google_ai_api_key: Optional[str] = None

    # Sentry
    sentry_dsn: Optional[str] = None

    # Server
    api_v1_prefix: str = "/api/v1"
    debug: bool = False  # Must opt-in to debug mode
    sql_echo: bool = False  # SQL query logging (separate from debug)

    # Usage limits (daily per user)
    daily_chat_limit: int = 50
    daily_meal_generation_limit: int = 5
    daily_photo_analysis_limit: int = 10

    # CORS
    cors_origins: list[str] = (
        []
    )  # Empty = no CORS middleware (Android app doesn't need CORS)


@lru_cache
def get_settings() -> Settings:
    """Get cached settings instance."""
    return Settings()


settings = get_settings()
