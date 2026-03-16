---
description: >
  All HTTP error responses MUST use the typed exception classes from core/exceptions.py.
  Never raise raw HTTPException or return plain error dicts from endpoints.
globs: ["backend/app/api/**/*.py", "backend/app/services/**/*.py"]
synthesized: true
private: false
---

# Custom Exception Hierarchy

All backend error responses MUST use the typed exception classes defined in `backend/app/core/exceptions.py`. These provide consistent error responses and centralized error handling via FastAPI's exception handlers in `main.py`.

## Available exception classes

| Exception | HTTP Status | Default message | When to use |
|-----------|------------|-----------------|-------------|
| `AuthenticationError` | 401 | "Could not validate credentials" | Invalid/expired tokens, failed Firebase verification |
| `ForbiddenError` | 403 | "Not enough permissions" | User lacks permission for the resource |
| `NotFoundError` | 404 | "Resource not found" | Entity lookup returns None |
| `BadRequestError` | 400 | "Bad request" | Invalid input that passes schema validation but fails business rules |
| `ConflictError` | 409 | "Resource already exists" | Duplicate creation attempts, uniqueness violations |
| `ServiceUnavailableError` | 503 | "Service temporarily unavailable" | External service (Firebase, Gemini, Claude) is down |

## Usage pattern

```python
from app.core.exceptions import NotFoundError, BadRequestError

async def get_meal_plan(plan_id: str, user_id: str):
    plan = await meal_plan_repo.get_by_id(plan_id)
    if not plan:
        raise NotFoundError(f"Meal plan {plan_id} not found")
    if plan.user_id != user_id:
        raise ForbiddenError("Not your meal plan")
    return plan
```

## MUST NOT

- MUST NOT raise raw `HTTPException` — use the typed exceptions instead. They ensure consistent `WWW-Authenticate` headers on 401s and consistent error response format.
- MUST NOT return error dicts manually (e.g., `return {"error": "not found"}`) — raise exceptions and let FastAPI's exception handlers format the response.
- MUST NOT catch typed exceptions and re-raise as generic `HTTPException` — this loses the specific error semantics.
- MUST NOT use `ServiceUnavailableError` for internal bugs — it's reserved for external service failures (Firebase, AI APIs). Internal errors should be unhandled 500s that trigger Sentry alerts.
