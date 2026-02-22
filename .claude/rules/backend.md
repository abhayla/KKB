---
paths:
  - "backend/**/*.py"
---

# Backend Rules

## Commands
- Always prefix with `PYTHONPATH=.` when running from `backend/`
- Clear stale cache after fixes: `find . -name "*.pyc" -delete && find . -name "__pycache__" -type d -exec rm -rf {} +`

## Adding Models (5 mandatory locations)
When adding a new SQLAlchemy model, ALL of these must be updated:
1. `app/models/your_model.py` — define the model
2. `app/models/__init__.py` — re-export
3. `app/db/postgres.py` `init_db()` — import
4. `app/db/postgres.py` `create_tables()` — import
5. `app/db/postgres.py` `drop_tables()` — import
6. `tests/conftest.py` — import (so SQLite creates the table)
7. Generate Alembic migration

## Model Location Gotchas
- `FamilyMember` → `models/user.py` (not its own file)
- `NutritionGoal` → `models/recipe_rule.py` (not its own file)
- `recipe_rules.py` endpoint has TWO routers (recipe rules + nutrition goals)

## SQLAlchemy Async
- Use `selectinload()` for eager loading — `joinedload`/lazy loading raises `MissingGreenlet`
- `expire_on_commit=False` is required — removing it breaks post-commit attribute access
- Compare recipe IDs as strings — `uuid.UUID` vs `String(36)` mismatch causes 500 errors

## Services Pattern
- Services take `db: AsyncSession` as parameter (not creating own sessions)
- `family_constraints.py` is shared by both `ai_meal_service.py` AND `recipe_rules.py` endpoint — changes affect both
- `ai_meal_service.py` has a local `UserPreferences` that shadows `app.models.user.UserPreferences`

## AI Module
- Uses `google-genai` SDK (NOT `google-generativeai`) with `client.aio` for native async — do NOT revert
- Model name constant in `gemini_client.py` — change it there only
- When adding chat tools: add definition in `tools/preference_tools.py` AND handling in `chat_assistant.py`

## Auth Debug Bypass
- `DEBUG=true` makes `firebase.py` accept `"fake-firebase-token"` for E2E tests
- E2E tests fail against non-debug backend
