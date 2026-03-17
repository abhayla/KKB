---
description: Backend logging conventions — module-level loggers, log levels, structured logging in production.
globs: ["backend/app/**/*.py"]
---

# Backend Logging Standards

## Module-Level Logger

Every Python module in `backend/app/` that performs I/O, handles requests, or contains business logic MUST declare a module-level logger:

```python
import logging

logger = logging.getLogger(__name__)
```

Place this at the top of the file, after imports, before class/function definitions.

## Log Level Usage

| Level | When to Use | Example |
|-------|-------------|---------|
| `logger.debug()` | Detailed flow tracing (only visible when `DEBUG=true`) | `logger.debug(f"Fetching meal plan {plan_id} for user {user_id}")` |
| `logger.info()` | Operation boundaries — start/complete of significant actions | `logger.info(f"User {user_id} created successfully")` |
| `logger.warning()` | Recoverable issues that don't fail the request | `logger.warning(f"Token reuse detected for user {user_id}")` |
| `logger.error()` | Failures that cause a request to fail or a feature to degrade | `logger.error(f"Failed to generate meal plan: {e}")` |
| `logger.exception()` | Same as error, but includes traceback — use inside `except` blocks | `logger.exception(f"Unhandled error in chat: {e}")` |

## What to Log

- Service method entry with key parameters (at `info` level)
- External API calls (Gemini, Firebase, Claude) — start and result
- Cache hits/misses
- Authentication events (login, token refresh, token reuse)
- Background task start/completion
- Retry attempts with attempt number

## What NOT to Log

- NEVER log passwords, API keys, tokens, or credentials
- NEVER log full request/response bodies (may contain PII)
- NEVER log at `error` level for expected user errors (bad input, 404s)
- Avoid excessive logging inside tight loops

## Production Structured Logging

In production (`DEBUG=false`), `main.py` configures JSON structured logging via `python-json-logger`:

```python
{"timestamp": "2026-03-17T10:30:00", "level": "INFO", "name": "app.services.auth_service", "message": "User abc123 authenticated"}
```

This is automatic — no code changes needed per-module. Just use the standard `logger` instance.

## f-string Convention

Use f-strings for log messages with contextual IDs:

```python
# CORRECT — includes user context for debugging
logger.info(f"Generated meal plan {plan.id} for user {user_id} ({len(items)} items)")

# WRONG — no context
logger.info("Meal plan generated")
```

Include relevant entity IDs (user_id, plan_id, recipe_id) so logs can be correlated.
