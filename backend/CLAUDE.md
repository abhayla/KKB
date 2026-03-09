# Backend

Python FastAPI + PostgreSQL + SQLAlchemy async.

## Commands

```bash
# All commands run from backend/
source venv/bin/activate                # Activate venv (Git Bash)
uvicorn app.main:app --reload           # Dev server → http://localhost:8000/docs
PYTHONPATH=. pytest                     # All tests
PYTHONPATH=. pytest tests/test_auth.py -v                              # Single file
PYTHONPATH=. pytest tests/test_preference_service.py::test_add_include_rule -v  # Single test
PYTHONPATH=. pytest --collect-only -q   # Count all tests
alembic upgrade head                    # Apply migrations
alembic revision --autogenerate -m "description"  # New migration
```

`PYTHONPATH=.` is required — running without it causes import errors.

## App Structure

```
app/
├── ai/            # Claude + Gemini clients (gemini_client.py: structured output with response_json_schema), prompts, tools
├── api/v1/        # 12 router files (~44 endpoints)
├── cache/         # recipe_cache.py (warmed on startup, non-fatal)
├── config.py      # Pydantic Settings (JWT secret required, DEBUG=false default, CORS=[] default, usage limits)
├── core/          # firebase.py, security.py, exceptions.py
├── db/            # postgres.py, database.py, firestore.py (legacy)
├── main.py        # FastAPI app, Sentry init, lifespan
├── models/        # SQLAlchemy ORM (14 files, incl. usage_log, refresh_token, household)
├── repositories/  # Data access (5 files)
├── schemas/       # Pydantic request/response
└── services/      # Business logic (21 files, incl. ai_meal_service, generation_tracker, cleanup, usage_limit, user_deletion)
```

## Stale Cache

After fixing backend code, clear `__pycache__` to avoid stale `.pyc` files:
```bash
find . -name "*.pyc" -delete && find . -name "__pycache__" -type d -exec rm -rf {} +
```

## Detailed Rules

For model locations, API gotchas, service patterns, SQLAlchemy rules, meal generation, rate limiting, and security details, see `.claude/rules/backend.md` (auto-loaded when editing `.py` files).
