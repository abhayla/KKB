---
name: add-new-model
description: >
  Add a new SQLAlchemy model to the backend with all 5 required import locations,
  Pydantic schemas, Alembic migration, and test setup. Enforces the 5-location model
  import rule to prevent silent test/migration failures.
type: workflow
allowed-tools: "Bash Read Write Edit Grep Glob"
argument-hint: "<model-name> <field1:type> [field2:type ...]"
version: "1.0.0"
synthesized: true
---

# Add New Backend Model

Add a new SQLAlchemy model with all 5 required import locations, schemas, and migration.

**Request:** $ARGUMENTS

---

## STEP 1: Parse Arguments

Extract from `$ARGUMENTS`:
- **Model name**: e.g., `ShoppingList` → table name `shopping_lists`, file `shopping_list.py`
- **Fields**: e.g., `name:str user_id:uuid items:json is_active:bool`

Map types:
| Input type | SQLAlchemy type | Pydantic type |
|-----------|----------------|---------------|
| `str` | `String(255)` | `str` |
| `text` | `Text` | `str` |
| `int` | `Integer` | `int` |
| `float` | `Float` | `float` |
| `bool` | `Boolean` | `bool` |
| `uuid` | `String(36)` | `str` |
| `json` | `JSON` | `dict \| list` |
| `date` | `Date` | `date` |
| `datetime` | `DateTime(timezone=True)` | `datetime` |

## STEP 2: Create Model File (Location 1/5)

Create `backend/app/models/shopping_list.py`:

```python
"""ShoppingList model."""

import uuid
from datetime import datetime, timezone

from sqlalchemy import Boolean, Column, DateTime, JSON, String, Text
from app.db.base import Base


class ShoppingList(Base):
    __tablename__ = "shopping_lists"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = Column(String(36), nullable=False, index=True)
    name = Column(String(255), nullable=False)
    items = Column(JSON, nullable=True)
    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))
    updated_at = Column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc), onupdate=lambda: datetime.now(timezone.utc))
```

## STEP 3: Update __init__.py (Location 2/5)

Edit `backend/app/models/__init__.py`:
1. Add import: `from app.models.shopping_list import ShoppingList`
2. Add to `__all__`: `"ShoppingList"`

## STEP 4: Update postgres.py (Location 3/5)

Edit `backend/app/db/postgres.py` — add the import in ALL THREE functions:
- `init_db()`
- `create_tables()`
- `drop_tables()`

```python
from app.models.shopping_list import ShoppingList  # noqa: F401
```

## STEP 5: Update test conftest.py (Location 4/5)

Edit `backend/tests/conftest.py` — add to the model import block:

```python
from app.models import shopping_list  # noqa: F401
```

This ensures the in-memory SQLite test database creates the `shopping_lists` table.

## STEP 6: Create Pydantic Schemas

Create `backend/app/schemas/shopping_list.py`:

```python
"""ShoppingList request/response schemas."""

from datetime import datetime
from typing import Optional
from pydantic import BaseModel

class ShoppingListCreate(BaseModel):
    name: str
    items: list | None = None

class ShoppingListUpdate(BaseModel):
    name: str | None = None
    items: list | None = None
    is_active: bool | None = None

class ShoppingListResponse(BaseModel):
    id: str
    user_id: str
    name: str
    items: list | None = None
    is_active: bool
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True}
```

## STEP 7: Generate Alembic Migration (Location 5/5)

```bash
cd backend && alembic revision --autogenerate -m "add shopping_list table"
```

Review the generated migration, then:

```bash
alembic upgrade head
```

## STEP 8: Verify All 5 Locations

```bash
cd backend

# Location 1: Model file
ls app/models/shopping_list.py

# Location 2: __init__.py export
grep "ShoppingList" app/models/__init__.py

# Location 3: postgres.py (should appear 3+ times)
grep -c "shopping_list\|ShoppingList" app/db/postgres.py

# Location 4: test conftest
grep "shopping_list" tests/conftest.py

# Location 5: migration exists
ls alembic/versions/*shopping_list* 2>/dev/null

# Run tests to verify table is created
PYTHONPATH=. pytest tests/ -x -q --tb=short
```

---

## CRITICAL RULES

- MUST update ALL 5 locations — partial registration causes silent failures
- MUST add `model_config = {"from_attributes": True}` to response schemas for ORM compatibility
- MUST use `String(36)` for UUID columns — SQLite test DB doesn't support native UUID
- MUST include `user_id` column with index for all user-scoped models
- MUST run migration and tests after creation to verify everything is wired correctly
