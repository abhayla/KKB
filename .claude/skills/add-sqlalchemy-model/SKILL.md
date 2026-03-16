---
name: add-sqlalchemy-model
description: >
  Add a new SQLAlchemy model to the backend with all 5 required registration locations.
  Prevents silent migration and test failures caused by missing imports.
type: workflow
allowed-tools: "Bash Read Write Edit Grep Glob"
argument-hint: "<ModelName> <table_name>"
version: "1.0.0"
synthesized: true
private: false
---

# Add SQLAlchemy Model

Add a new SQLAlchemy model with all required registrations. Missing ANY location causes migrations or tests to silently fail.

**Arguments:** $ARGUMENTS — e.g., `RecipeCollection recipe_collections`

## STEP 1: Create the Model File

Create `backend/app/models/<model_name>.py`:

```python
"""<ModelName> model."""

import uuid
from datetime import datetime, timezone

from sqlalchemy import Column, DateTime, ForeignKey, String, Text
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import relationship

from app.db.base import Base


class <ModelName>(Base):
    __tablename__ = "<table_name>"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=False)
    # Add your columns here
    created_at = Column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))
    updated_at = Column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc), onupdate=lambda: datetime.now(timezone.utc))
```

## STEP 2: Register in models/__init__.py

Add TWO things to `backend/app/models/__init__.py`:

1. Import at the top:
   ```python
   from app.models.<model_file> import <ModelName>
   ```

2. Add to `__all__` list:
   ```python
   __all__ = [
       # ... existing models ...
       "<ModelName>",
   ]
   ```

## STEP 3: Register in postgres.py (3 import blocks)

Add `<model_file>,` to ALL THREE import blocks in `backend/app/db/postgres.py`:

1. **`init_db()`** function — the `from app.models import (...)` block
2. **`create_tables()`** function — the `from app.models import (...)` block
3. **`drop_tables()`** function — the `from app.models import (...)` block

All three blocks MUST have identical imports. Search for `# noqa: F401` to find them.

## STEP 4: Generate Alembic Migration

```bash
cd backend
alembic revision --autogenerate -m "add <table_name> table"
```

Review the generated migration — verify it has both `upgrade()` and `downgrade()` functions.

## STEP 5: Verify

```bash
cd backend
# Apply migration
alembic upgrade head

# Verify table exists
python -c "from app.models import <ModelName>; print('<ModelName> registered')"

# Run tests to catch any import issues
PYTHONPATH=. python -m pytest tests/ -x -q
```

## CRITICAL RULES

- NEVER skip any of the 5 locations — missing imports cause `sqlalchemy.exc.NoReferencedTableError` at migration time or silent test failures
- NEVER import models from `app.models.<file>` in endpoint/service files — always import from `app.models` (the `__init__.py` re-export)
- ALL THREE import blocks in `postgres.py` MUST be identical — if you add to one, add to all three
- Always generate an Alembic migration after adding the model — never manually create tables
