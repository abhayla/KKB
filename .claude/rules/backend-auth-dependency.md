---
description: >
  Enforce the typed CurrentUser dependency for all authenticated backend endpoints.
  Prevents raw token parsing in endpoint handlers and centralizes auth logic.
globs: ["backend/app/api/**/*.py"]
synthesized: true
private: true
---

# Backend Auth Dependency Pattern

## The Rule

All authenticated API endpoints MUST use the `CurrentUser` type alias from `api/deps.py`. NEVER parse tokens or query users directly in endpoint handlers.

## Correct Usage

```python
from app.api.deps import CurrentUser, DbSession

@router.get("/me")
async def get_current_user_profile(
    user: CurrentUser,   # Automatically authenticated
    db: DbSession,       # Automatically injected
):
    return user
```

## How It Works

`CurrentUser` is a FastAPI `Annotated` type that chains two dependencies:

1. `DbSession` — provides an async SQLAlchemy session via `get_db()`
2. `get_current_user()` — extracts Bearer token from `Authorization` header, verifies it via `verify_token_and_get_user_id()`, queries the `User` model from PostgreSQL, and checks `is_active`

```python
# In api/deps.py:
CurrentUser = Annotated[User, Depends(get_current_user)]
DbSession = Annotated[AsyncSession, Depends(get_db)]
```

## Error Handling

The dependency raises structured exceptions — endpoints do NOT need try/catch:

| Condition | Exception | HTTP Code |
|-----------|-----------|-----------|
| Missing `Authorization` header | `AuthenticationError` | 401 |
| Invalid header format (not `Bearer ...`) | `AuthenticationError` | 401 |
| Invalid/expired token | `AuthenticationError` | 401 |
| User not found in DB | `NotFoundError` | 404 |
| User deactivated (`is_active=False`) | `AuthenticationError` | 401 |

## MUST NOT

- NEVER import `verify_token_and_get_user_id` directly in endpoint files — use `CurrentUser` dependency
- NEVER query the `User` model in endpoint handlers for auth purposes — `CurrentUser` already provides the full user object
- NEVER skip auth by omitting `CurrentUser` from protected endpoints — if an endpoint needs auth, add `user: CurrentUser` as a parameter
