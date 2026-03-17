---
description: FastAPI endpoint structure conventions — rate limiting, DI, response models, docstrings.
globs: ["backend/app/api/**/*.py"]
---

# Backend Endpoint Structure

## Router Registration

All endpoint routers live in `backend/app/api/v1/endpoints/`. Each router file:
- Creates `router = APIRouter(prefix="/resource", tags=["Resource"])`
- Is imported and included in `backend/app/api/v1/router.py`

## Endpoint Signature Pattern

Every authenticated endpoint MUST follow this parameter order:

```python
@router.post("/action", response_model=ResponseSchema)
@limiter.limit("500/minute" if settings.debug else "N/minute")
async def endpoint_name(
    request: Request,           # 1. Always first (required by slowapi)
    body: RequestSchema,        # 2. Request body (Pydantic model)
    current_user: CurrentUser,  # 3. Auth dependency (from deps.py)
    db: DbSession,              # 4. Database session (if needed)
    query_param: str = Query(), # 5. Query parameters last
) -> ResponseSchema:
    """One-line summary of what this endpoint does.

    Longer description if needed with parameter explanations.
    """
```

## Rate Limiting

- Every mutating endpoint (POST, PUT, DELETE) MUST have `@limiter.limit()`
- Use conditional rates: `"500/minute" if settings.debug else "N/minute"`
- `request: Request` MUST be the first parameter — slowapi reads it for client IP
- Read-only GET endpoints with low cost MAY skip rate limiting

## Response Models

- Every endpoint MUST declare `response_model=SchemaName` in the decorator
- Return type hint MUST match: `-> SchemaName`
- Use Pydantic schemas from `app/schemas/` — never return raw dicts or ORM objects

## Dependencies

Import from `app/api/deps.py`:
- `CurrentUser` — authenticated user (raises 401 if missing/invalid token)
- `DbSession` — async SQLAlchemy session
- `OptionalUser` — for endpoints that work both authenticated and anonymous

## Docstrings

Every endpoint MUST have a docstring — FastAPI uses it for the Swagger `/docs` page.

## Anti-Patterns

- NEVER use `request.json()` manually — use Pydantic request bodies for validation
- NEVER catch exceptions to return custom error dicts — let the exception handlers in `main.py` handle it
- NEVER create database sessions inside endpoints — use the `DbSession` dependency
- NEVER put `request: Request` anywhere except first position — slowapi will silently skip rate limiting
