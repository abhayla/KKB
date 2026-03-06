"""Firebase Admin SDK integration for token verification."""

import logging
from typing import Optional

import firebase_admin
from firebase_admin import auth, credentials

from app.config import settings
from app.core.exceptions import AuthenticationError, ServiceUnavailableError

logger = logging.getLogger(__name__)

# Firebase app instance
_firebase_app: Optional[firebase_admin.App] = None


def initialize_firebase() -> None:
    """Initialize Firebase Admin SDK.

    Call this once at application startup.
    """
    global _firebase_app

    if _firebase_app is not None:
        return

    try:
        if settings.firebase_credentials_path:
            cred = credentials.Certificate(settings.firebase_credentials_path)
            _firebase_app = firebase_admin.initialize_app(cred)
            logger.info("Firebase initialized with service account credentials")
        else:
            # Use default credentials (for Cloud Run, etc.)
            _firebase_app = firebase_admin.initialize_app()
            logger.info("Firebase initialized with default credentials")
    except Exception as e:
        logger.warning(f"Firebase initialization failed: {e}")
        logger.warning("Firebase auth will be mocked in development mode")


def verify_firebase_token(id_token: str) -> dict:
    """Verify a Firebase ID token and return the decoded claims.

    Args:
        id_token: Firebase ID token from the client

    Returns:
        Decoded token containing user info (uid, email, name, etc.)

    Raises:
        AuthenticationError: If token is invalid
        ServiceUnavailableError: If Firebase is not available
    """
    global _firebase_app

    # E2E Test mode: accept fake-firebase-token and fake-firebase-token-{suffix}
    if settings.debug and id_token.startswith("fake-firebase-token"):
        suffix = id_token.removeprefix("fake-firebase-token").lstrip("-")
        uid = f"fake-user-{suffix}" if suffix else "fake-user-id"
        name = suffix.replace("-", " ").title() if suffix else "E2E Test User"
        import hashlib

        phone_suffix = (
            int(hashlib.md5(suffix.encode()).hexdigest(), 16) % 9000000000 + 1000000000
            if suffix
            else 1111111111
        )
        logger.info(f"E2E Test: Using fake Firebase token for testing (uid={uid})")
        return {
            "uid": uid,
            "email": f"{suffix or 'e2e-test'}@rasoiai.test",
            "name": name or "E2E Test User",
            "picture": None,
            "phone_number": f"+91{phone_suffix}",
        }

    # Development mode: mock Firebase verification
    if _firebase_app is None:
        if settings.debug:
            logger.warning("Firebase not initialized, using mock verification")
            # Return mock user data for development
            return {
                "uid": "dev-user-123",
                "email": "dev@example.com",
                "name": "Development User",
                "picture": None,
                "phone_number": "+919999999999",
            }
        raise ServiceUnavailableError("Firebase authentication not available")

    try:
        # Allow 60 seconds of clock skew to handle emulator/server time differences
        decoded_token = auth.verify_id_token(id_token, clock_skew_seconds=60)
        return {
            "uid": decoded_token["uid"],
            "email": decoded_token.get("email"),
            "name": decoded_token.get("name"),
            "picture": decoded_token.get("picture"),
            "phone_number": decoded_token.get("phone_number"),
        }
    except auth.InvalidIdTokenError as e:
        logger.error(f"Invalid Firebase token: {e}")
        raise AuthenticationError("Invalid Firebase token")
    except auth.ExpiredIdTokenError:
        raise AuthenticationError("Firebase token has expired")
    except auth.RevokedIdTokenError:
        raise AuthenticationError("Firebase token has been revoked")
    except Exception as e:
        logger.error(f"Firebase token verification failed: {e}")
        raise AuthenticationError("Could not verify Firebase token")
