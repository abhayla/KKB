---
description: >
  Enforce the three-tier backend architecture: endpoints delegate to services,
  services delegate to repositories, only repositories touch the database directly.
globs: ["backend/app/api/**/*.py", "backend/app/services/**/*.py", "backend/app/repositories/**/*.py"]
synthesized: true
private: false
---

# Backend Service Layer Pattern

## Architecture

```
Endpoint (api/v1/endpoints/) → Service (services/) → Repository (repositories/)
                                                            ↓
                                                     SQLAlchemy (models/)
```

## Layer Responsibilities

### Endpoints (`api/v1/endpoints/*.py`)

- Parse request parameters and validate via Pydantic schemas
- Call service methods — NEVER call repositories directly
- Return response schemas — NEVER return raw SQLAlchemy models
- Use `CurrentUser` and `DbSession` dependencies from `api/deps.py`

### Services (`services/*.py`)

- Contain ALL business logic: validation rules, AI orchestration, generation algorithms
- Accept domain-level parameters (not raw request objects where possible)
- Call repositories for data access
- May call other services (e.g., `MealPlanService` calls `RecipeRepository` + `AiMealService`)
- 20 service files covering: auth, chat, meal plans, recipes, grocery, households, notifications, AI meal generation, photo analysis, festivals, cleanup, config, usage tracking

### Repositories (`repositories/*.py`)

- Pure data access — no business logic
- Accept and return SQLAlchemy models or primitives
- Use `select()`, `selectinload()`, async session methods
- 5 repository files: chat, festival, meal_plan, recipe, user

## MUST DO

- Pass `db: AsyncSession` to service/repository methods — do not create sessions inside services
- Use `selectinload()` for eager loading relationships in repositories — never lazy load across an async boundary
- Keep endpoint handlers thin — 5-15 lines max. If an endpoint has more than 20 lines of logic, extract to a service

## MUST NOT

- NEVER import `AsyncSession`, `select`, or SQLAlchemy functions in endpoint files — all DB access goes through repositories
- NEVER put business logic (conditionals, loops over data, AI calls) in repository files — only query construction
- NEVER return SQLAlchemy model instances from endpoints — always convert to Pydantic response schemas
