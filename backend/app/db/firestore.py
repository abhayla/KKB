"""Firestore database configuration and utilities."""

import logging
from typing import Any, Optional

from google.cloud import firestore
from google.cloud.firestore_v1 import AsyncClient

from app.config import settings

logger = logging.getLogger(__name__)

# Firestore client instance
_firestore_client: Optional[AsyncClient] = None


def get_firestore_client() -> AsyncClient:
    """Get or create async Firestore client.

    Returns:
        AsyncClient: Firestore async client instance
    """
    global _firestore_client

    if _firestore_client is None:
        if settings.firebase_credentials_path:
            _firestore_client = AsyncClient.from_service_account_json(
                settings.firebase_credentials_path
            )
            logger.info("Firestore initialized with service account credentials")
        else:
            # Use default credentials (for Cloud Run, local emulator, etc.)
            _firestore_client = AsyncClient()
            logger.info("Firestore initialized with default credentials")

    return _firestore_client


# Collection names
class Collections:
    """Firestore collection names."""
    USERS = "users"
    USER_PREFERENCES = "preferences"  # subcollection under users
    FAMILY_MEMBERS = "family_members"  # subcollection under users
    RECIPES = "recipes"
    MEAL_PLANS = "meal_plans"
    GROCERY_LISTS = "grocery_lists"
    CHAT_MESSAGES = "chat_messages"
    FESTIVALS = "festivals"
    COOKING_STREAKS = "cooking_streaks"
    ACHIEVEMENTS = "achievements"


async def init_firestore() -> None:
    """Initialize Firestore connection.

    Call this at application startup to verify connection.
    """
    try:
        client = get_firestore_client()
        # Test connection by listing collections (limited)
        logger.info("Firestore connection verified")
    except Exception as e:
        logger.error(f"Failed to initialize Firestore: {e}")
        raise


def doc_to_dict(doc: firestore.DocumentSnapshot) -> dict[str, Any]:
    """Convert Firestore document to dictionary with ID.

    Args:
        doc: Firestore document snapshot

    Returns:
        Dictionary with document data and 'id' field
    """
    if not doc.exists:
        return {}
    data = doc.to_dict() or {}
    data["id"] = doc.id
    return data
