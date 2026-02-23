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
├── ai/            # Claude + Gemini clients, prompts, tools
├── api/v1/        # 12 router files (~44 endpoints)
├── cache/         # recipe_cache.py (warmed on startup, non-fatal)
├── config.py      # Pydantic Settings (JWT secret required, DEBUG=false default, CORS=[] default, usage limits)
├── core/          # firebase.py, security.py, exceptions.py
├── db/            # postgres.py, database.py, firestore.py (legacy)
├── main.py        # FastAPI app, Sentry init, lifespan
├── models/        # SQLAlchemy ORM (13 files, incl. usage_log, refresh_token)
├── repositories/  # Data access (5 files)
├── schemas/       # Pydantic request/response
└── services/      # Business logic (21 files, incl. cleanup, usage_limit, user_deletion)
```

## Adding a New Model (5 mandatory locations)

When adding a SQLAlchemy model, update ALL of these or tests/migrations will silently break:

1. `app/models/your_model.py` — define the model
2. `app/models/__init__.py` — re-export it
3. `app/db/postgres.py` — import in `init_db()` block
4. `app/db/postgres.py` — import in `create_tables()` block
5. `app/db/postgres.py` — import in `drop_tables()` block
6. `tests/conftest.py` — import so SQLite creates the table
7. `alembic revision --autogenerate` — generate migration

## Model Location Gotchas

- `FamilyMember` is in `models/user.py`, not its own file
- `NutritionGoal` is in `models/recipe_rule.py`, not its own file
- `AiRecipeCatalog` is in `models/ai_recipe_catalog.py`
- `UsageLog` is in `models/usage_log.py` (tracks AI feature usage per user)
- `RefreshToken` is in `models/refresh_token.py` (stores hashed opaque tokens for rotation)

## Router Gotchas

- `recipe_rules.py` defines **two routers**: one for recipe rules, one for nutrition goals. Don't create a separate `nutrition_goals.py`.
- `family_members.py` is registered under `/users` prefix — full path is `/api/v1/users/family-members/`.
- Swagger UI (`/docs`) is only available when `DEBUG=true`.

## AI Module (`app/ai/`)

```
ai/
├── chat_assistant.py     # Claude tool-calling loop (MAX_TOOL_ITERATIONS=5)
├── claude_client.py      # Anthropic SDK wrapper
├── gemini_client.py      # google-genai SDK, lazy init, MODEL_NAME constant
├── prompts/              # chat_prompt.py, meal_plan_prompt.py
└── tools/
    └── preference_tools.py  # ALL_CHAT_TOOLS — add new tools here AND in chat_assistant.py
```

- Uses `google-genai` SDK (NOT old `google-generativeai`) with native async `client.aio`. Do NOT revert — the old SDK blocked uvicorn's event loop.
- Gemini model name is `MODEL_NAME = "gemini-2.5-flash"` in `gemini_client.py` — change it there only.
- Chat context is limited to last 6 messages via `ChatRepository.get_context_for_claude(limit=6)`.

## Service Patterns

- Services take `db: AsyncSession` as parameter (DI from endpoint). They do NOT create their own sessions (except `auth_service.py` which uses `async_session_maker` directly for token rotation/logout — must be patched in test fixtures).
- `family_constraints.py` is a shared module imported by BOTH `ai_meal_service.py` AND the `recipe_rules.py` endpoint — changes affect meal generation AND rule validation simultaneously.
- `ai_meal_service.py` defines a local `UserPreferences` dataclass that shadows `app.models.user.UserPreferences` — don't confuse them.

## SQLAlchemy Async Rules

- Use `selectinload()` for eager loading — `joinedload` and lazy loading raise `MissingGreenlet`.
- `expire_on_commit=False` is set in session maker — required for async; without it, post-commit attribute access fails.
- Compare recipe IDs as strings in PostgreSQL queries — `uuid.UUID` vs `String(36)` type mismatch causes 500 errors.

## Debug Auth Bypass

When `DEBUG=true`, `core/firebase.py` accepts `"fake-firebase-token"` and returns a test user. This enables E2E tests from the Android emulator. **E2E tests fail against a non-debug backend.**

## Stale Cache

After fixing backend code, clear `__pycache__` to avoid stale `.pyc` files:
```bash
find . -name "*.pyc" -delete && find . -name "__pycache__" -type d -exec rm -rf {} +
```

## Rate Limiting

Uses `slowapi` with per-endpoint decorators (`@limiter.limit("10/minute")`). Endpoints with rate limits must have `request: Request` as first parameter. Key limits: auth 10/min, chat 30/min, meal generation 5/hr, photo analysis 10/hr.

## Security

- **SecurityHeadersMiddleware** in `main.py`: X-Content-Type-Options, X-Frame-Options, X-XSS-Protection, HSTS (non-debug), X-API-Version
- **Sentry**: `send_default_pii=False`, `traces_sample_rate=0.1`
- **CORS**: Empty by default (`[]`). Set via `CORS_ORIGINS` env var. Only added when non-empty.
- **JWT**: `jwt_secret_key` has no default — app crashes on startup if missing. `access_token_expire_minutes=30`.
- **Token rotation**: Refresh tokens are opaque (SHA-256 hashed, stored in `refresh_tokens` table). Token reuse detection revokes all user tokens.
- **Usage limits**: Configurable daily limits per AI action (chat, meal gen, photo analysis). Returns 429 when exceeded.

## Startup Sequence

`main.py` lifespan handler: Sentry init (`send_default_pii=False`) → `init_db()` → `warm_recipe_cache()` (non-fatal) → app ready.
