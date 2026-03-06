"""Tests for debug firebase token pattern matching."""

import pytest
from unittest.mock import patch

from app.core.firebase import verify_firebase_token


@pytest.fixture(autouse=True)
def enable_debug():
    """Enable debug mode for all tests in this file."""
    with patch("app.core.firebase.settings") as mock_settings:
        mock_settings.debug = True
        mock_settings.firebase_credentials_path = None
        yield mock_settings


def test_plain_fake_token_returns_default_uid(enable_debug):
    result = verify_firebase_token("fake-firebase-token")
    assert result["uid"] == "fake-user-id"
    assert result["name"] == "E2E Test User"


def test_suffixed_fake_token_returns_unique_uid(enable_debug):
    result = verify_firebase_token("fake-firebase-token-sharma")
    assert result["uid"] == "fake-user-sharma"
    assert "sharma" in result["name"].lower()


def test_different_suffixes_return_different_uids(enable_debug):
    r1 = verify_firebase_token("fake-firebase-token-sharma")
    r2 = verify_firebase_token("fake-firebase-token-gupta")
    assert r1["uid"] != r2["uid"]
    assert r1["uid"] == "fake-user-sharma"
    assert r2["uid"] == "fake-user-gupta"
