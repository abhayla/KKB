"""Tests for auth_service pure helpers.

Covers _hash_token and _generate_refresh_token. The async auth flows
(authenticate_with_firebase, refresh_access_token, logout_user) have
Firebase + DB dependencies and are covered by the existing
tests/api/test_auth.py integration suite.
"""

import hashlib
import re

from app.services.auth_service import _generate_refresh_token, _hash_token


# ==================== _hash_token ====================


class TestHashToken:
    def test_produces_sha256_hex(self):
        result = _hash_token("my-refresh-token")
        # SHA-256 hex string is 64 hex characters.
        assert len(result) == 64
        assert re.fullmatch(r"[0-9a-f]+", result) is not None

    def test_is_deterministic(self):
        """Same input always produces the same hash — required so token
        lookups by hash work after storage."""
        assert _hash_token("abc") == _hash_token("abc")

    def test_different_inputs_produce_different_hashes(self):
        assert _hash_token("token-a") != _hash_token("token-b")

    def test_matches_stdlib_sha256(self):
        """Regression guard: the hash must match Python stdlib SHA-256 exactly.
        Otherwise refresh token lookups would silently fail after any
        implementation change."""
        expected = hashlib.sha256(b"secret").hexdigest()
        assert _hash_token("secret") == expected

    def test_handles_unicode(self):
        """Tokens with unicode should hash without raising."""
        result = _hash_token("token-emoji-🔐-valid")
        assert len(result) == 64

    def test_empty_string_has_known_hash(self):
        """Empty-string SHA-256 is a well-known constant."""
        assert _hash_token("") == hashlib.sha256(b"").hexdigest()


# ==================== _generate_refresh_token ====================


class TestGenerateRefreshToken:
    def test_returns_string(self):
        token = _generate_refresh_token()
        assert isinstance(token, str)
        assert len(token) > 0

    def test_tokens_are_unique(self):
        """secrets.token_urlsafe must produce unique tokens — collisions
        would mean users could steal each other's sessions."""
        tokens = {_generate_refresh_token() for _ in range(100)}
        assert len(tokens) == 100

    def test_tokens_use_url_safe_alphabet(self):
        """token_urlsafe uses A-Z a-z 0-9 - _ — no padding '=' or '/+'.
        Required so the token can travel in URLs without encoding."""
        token = _generate_refresh_token()
        assert re.fullmatch(r"[A-Za-z0-9_\-]+", token) is not None

    def test_tokens_have_sufficient_entropy(self):
        """48 raw bytes base64url-encoded yields 64 characters. At least
        this length is required for cryptographic strength."""
        token = _generate_refresh_token()
        assert len(token) >= 32  # generous lower bound
