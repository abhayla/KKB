---
description: >
  Enforce the 5-location model import rule for SQLAlchemy models. Missing any location causes
  silent test failures or missing migration tables. This is the most common source of bugs in
  the backend codebase.
globs: ["backend/app/models/**/*.py", "backend/app/db/postgres.py", "backend/tests/conftest.py"]
synthesized: true
private: false
---

# 5-Location Model Import Rule

When adding or modifying a SQLAlchemy model, ALL five locations MUST be updated. Missing any location causes silent failures — tests pass but migrations break, or tables silently don't get created.

## The 5 locations

| # | Location | What to do | Failure if missing |
|---|----------|------------|-------------------|
| 1 | `backend/app/models/your_model.py` | Define the SQLAlchemy model class | Model doesn't exist |
| 2 | `backend/app/models/__init__.py` | Re-export via `from app.models.your_model import YourModel` and add to `__all__` | Other modules can't import from `app.models` |
| 3 | `backend/app/db/postgres.py` | Import in ALL THREE functions: `init_db()`, `create_tables()`, AND `drop_tables()` | Tables not created/dropped during DB lifecycle |
| 4 | `backend/tests/conftest.py` | Import the model module so SQLite test DB creates the table | Tests silently skip table — queries return empty results instead of errors |
| 5 | Alembic migration | Run `alembic revision --autogenerate -m "add your_model"` | Production DB missing the table |

## Why this matters

SQLAlchemy only creates tables for models it has seen (imported). The in-memory SQLite test database and the PostgreSQL production database discover models independently. If a model is imported in production code but not in `tests/conftest.py`, tests run against a database without that table — and queries return empty results instead of raising errors.

## Verification

After adding a model, verify all 5 locations:

```bash
# Check model file exists
ls backend/app/models/your_model.py

# Check __init__.py exports
grep "your_model" backend/app/models/__init__.py

# Check postgres.py has all 3 imports
grep -c "your_model\|YourModel" backend/app/db/postgres.py  # Should be >= 3

# Check test conftest imports
grep "your_model" backend/tests/conftest.py

# Check migration exists
ls backend/alembic/versions/*your_model* 2>/dev/null || echo "MISSING: run alembic revision"
```

## MUST NOT

- MUST NOT add a model to only some of the 5 locations — partial registration causes silent failures
- MUST NOT rely on transitive imports — each location MUST explicitly import the model
- MUST NOT skip the `__all__` update in `__init__.py` — other modules depend on the public API
