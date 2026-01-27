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
    jwt_secret_key: str = "development-secret-key-change-in-production"
    jwt_algorithm: str = "HS256"
    access_token_expire_minutes: int = 10080  # 7 days

    # Anthropic Claude
    anthropic_api_key: Optional[str] = None

    # Server
    api_v1_prefix: str = "/api/v1"
    debug: bool = True

    # CORS
    cors_origins: list[str] = ["*"]


@lru_cache
def get_settings() -> Settings:
    """Get cached settings instance."""
    return Settings()


settings = get_settings()
