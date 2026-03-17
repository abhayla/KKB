---
description: Pydantic Settings conventions — required vs optional fields, safe defaults, environment variable patterns.
globs: ["backend/app/config.py", "backend/.env*"]
---

# Settings Configuration Validation

## Configuration File

All backend settings live in `backend/app/config.py` using Pydantic `BaseSettings`. Environment variables are loaded from `.env` via `SettingsConfigDict(env_file=".env")`.

## Field Classification

### Required (no default) — Security-Critical

Fields that MUST be provided or the app crashes on startup:

```python
jwt_secret_key: str  # No default — prevents running with weak/missing secret
```

Use this pattern for:
- JWT secrets
- Database URLs in production
- Any credential where a default would be dangerous

The crash is intentional — it's better to fail loudly than run with an insecure default.

### Optional (default None) — External Services

Fields for services that may not be configured in all environments:

```python
firebase_credentials_path: Optional[str] = None
anthropic_api_key: Optional[str] = None
google_ai_api_key: Optional[str] = None
sentry_dsn: Optional[str] = None
```

Code MUST check for `None` before using these:
```python
if settings.anthropic_api_key:
    # Initialize Anthropic client
```

### Boolean Flags — Opt-In Only

All boolean feature flags MUST default to `False` (opt-in):

```python
debug: bool = False       # Must opt-in to debug mode
sql_echo: bool = False    # SQL query logging (separate from debug)
```

NEVER default a debug/test flag to `True` — it risks leaking debug behavior to production.

### Numeric Limits — Sensible Defaults

Usage limits and timeouts MUST have production-safe defaults:

```python
daily_chat_limit: int = 50
daily_meal_generation_limit: int = 5
daily_photo_analysis_limit: int = 10
access_token_expire_minutes: int = 10080  # 7 days
```

### List Fields — Empty Default

List fields MUST default to empty, not permissive:

```python
cors_origins: list[str] = []  # Empty = no CORS middleware (more secure)
```

## Adding a New Setting

When adding a new environment variable:

1. Add the field to `Settings` class in `config.py` with appropriate default
2. Add the variable to `.env.example` with a placeholder value and comment
3. Add to the CLAUDE.md "Environment Setup" section if user-facing
4. Document whether it's required or optional in a comment

## Comments Convention

Every non-obvious setting MUST have an inline comment explaining:
- Why the default was chosen
- What happens if missing
- Any security implications

```python
jwt_secret_key: str  # No default — crashes if missing (security: prevents running with weak secret)
debug: bool = False  # Must opt-in to debug mode
```

## Anti-Patterns

- NEVER hardcode credentials or API keys in `config.py`
- NEVER use `os.environ` directly — always go through the `Settings` class
- NEVER add a default for security-critical fields
- NEVER use `True` as default for debug/test flags
- NEVER commit `.env` files — only `.env.example` with placeholder values
