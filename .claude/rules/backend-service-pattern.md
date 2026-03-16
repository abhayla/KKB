---
description: >
  Backend services use standalone async functions (not classes) with async_session_maker
  for database access. Services are the business logic layer between API endpoints and repositories.
globs: ["backend/app/services/**/*.py"]
synthesized: true
private: false
---

# Backend Service Layer Pattern

Backend business logic lives in `backend/app/services/` as standalone async functions. Services are NOT classes — they are module-level async functions that use `async_session_maker` for database access.

## Service function pattern

```python
"""Feature service with business logic."""

import logging
from sqlalchemy import select
from app.db.postgres import async_session_maker
from app.core.exceptions import NotFoundError, BadRequestError
from app.models.feature import Feature
from app.schemas.feature import FeatureCreate, FeatureResponse

logger = logging.getLogger(__name__)

async def create_feature(user_id: str, data: FeatureCreate) -> FeatureResponse:
    """Create a new feature for the user."""
    async with async_session_maker() as session:
        # Business logic validation
        existing = await session.execute(
            select(Feature).where(Feature.user_id == user_id, Feature.name == data.name)
        )
        if existing.scalar_one_or_none():
            raise ConflictError(f"Feature '{data.name}' already exists")

        feature = Feature(user_id=user_id, **data.model_dump())
        session.add(feature)
        await session.commit()
        await session.refresh(feature)
        return FeatureResponse.model_validate(feature)
```

## Key conventions

| Convention | Pattern |
|-----------|---------|
| Database access | `async with async_session_maker() as session:` — services create their own sessions |
| Error handling | Raise typed exceptions from `core/exceptions.py` — never return error dicts |
| Logging | `logger = logging.getLogger(__name__)` at module level |
| Input/Output types | Accept Pydantic schemas (`FeatureCreate`), return Pydantic schemas (`FeatureResponse`) |
| User scoping | Always accept `user_id: str` parameter — never trust implicit auth context |

## How endpoints call services

```python
# In backend/app/api/v1/endpoints/feature.py
from app.services import feature_service

@router.post("/features")
async def create(data: FeatureCreate, current_user: User = Depends(get_current_user)):
    return await feature_service.create_feature(current_user.id, data)
```

## Testing services directly

Services that use `async_session_maker` directly (instead of receiving a session via DI) need the session maker patched in tests:

```python
async def test_create_feature(db_session):
    # Patch async_session_maker to use the test session
    with patch("app.services.feature_service.async_session_maker", return_value=db_session):
        result = await create_feature("user-1", FeatureCreate(name="test"))
        assert result.name == "test"
```

## MUST NOT

- MUST NOT use classes for services — use standalone async functions at module level
- MUST NOT pass raw SQLAlchemy models to/from endpoints — always use Pydantic schemas for serialization
- MUST NOT hardcode user IDs or skip the `user_id` parameter — all service functions must be explicitly scoped to a user
- MUST NOT catch and suppress exceptions in services — let them propagate to FastAPI's exception handlers
