---
name: add-api-endpoint
description: >
  Add a new FastAPI endpoint with Pydantic schemas, service method, optional
  repository, and router registration. Ensures the 3-tier architecture is followed.
type: workflow
allowed-tools: "Bash Read Write Edit Grep Glob"
argument-hint: "<resource_name>"
version: "1.0.0"
synthesized: true
private: false
---

# Add API Endpoint

Add a new backend API endpoint following the 3-tier architecture: endpoint → service → repository.

**Arguments:** $ARGUMENTS — e.g., `collections`

## STEP 1: Create Pydantic Schemas

Create `backend/app/schemas/<resource_name>.py`:

```python
"""Pydantic schemas for <resource_name> endpoints."""

from pydantic import BaseModel, Field
from datetime import datetime
from uuid import UUID


class Create<Resource>Request(BaseModel):
    """Request body for creating a <resource>."""
    # fields here


class <Resource>Response(BaseModel):
    """Response body for a single <resource>."""
    id: UUID
    created_at: datetime
    # fields here

    model_config = {"from_attributes": True}


class <Resource>ListResponse(BaseModel):
    """Response body for a list of <resources>."""
    items: list[<Resource>Response]
    total: int
```

Use `model_config = {"from_attributes": True}` to allow constructing from SQLAlchemy models.

## STEP 2: Create Service Methods

Add to existing service or create `backend/app/services/<resource_name>_service.py`:

```python
"""<Resource> service for business logic."""

from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from app.models.<resource_name> import <Resource>
from app.core.exceptions import NotFoundError


async def create_<resource>(db: AsyncSession, user_id: str, data: dict) -> <Resource>:
    """Create a new <resource>."""
    obj = <Resource>(user_id=user_id, **data)
    db.add(obj)
    await db.commit()
    await db.refresh(obj)
    return obj
```

ALL business logic goes in services. NEVER put logic in endpoint handlers.

## STEP 3: Create Endpoint File

Create `backend/app/api/v1/endpoints/<resource_name>.py`:

```python
"""<Resource> API endpoints."""

from fastapi import APIRouter
from app.api.deps import CurrentUser, DbSession
from app.schemas.<resource_name> import (
    Create<Resource>Request,
    <Resource>Response,
)
from app.services.<resource_name>_service import create_<resource>

router = APIRouter(prefix="/<resource-name>", tags=["<resource-name>"])


@router.post("/", response_model=<Resource>Response, status_code=201)
async def create(
    request: Create<Resource>Request,
    user: CurrentUser,
    db: DbSession,
):
    """Create a new <resource>."""
    return await create_<resource>(db, str(user.id), request.model_dump())
```

Use `CurrentUser` for authenticated endpoints. Use `DbSession` for database access. NEVER import `AsyncSession` or `get_db` directly.

## STEP 4: Register the Router

Add to `backend/app/api/v1/router.py`:

```python
from app.api.v1.endpoints import <resource_name>

api_router.include_router(<resource_name>.router)
```

## STEP 5: Verify

```bash
cd backend
# Start the server
uvicorn app.main:app --reload

# Check endpoint appears in docs (DEBUG=true required)
# Visit http://localhost:8000/docs
```

## CRITICAL RULES

- NEVER put business logic in endpoint handlers — endpoints are thin wrappers that call services
- NEVER import `AsyncSession` or `select` in endpoint files — use `DbSession` and `CurrentUser` from `api/deps.py`
- NEVER return SQLAlchemy model instances from endpoints — always convert to Pydantic response schemas
- NEVER forget to register the router in `router.py` — the endpoint compiles but returns 404
- MUST use `CurrentUser` dependency for all authenticated endpoints — never parse tokens manually
