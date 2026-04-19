---
description: >
  Backend repositories use `async with async_session_maker() as session`
  per operation (never stored as instance state), with `selectinload()`
  for eager loading relationships to avoid MissingGreenlet errors.
globs: ["backend/app/repositories/**/*.py", "backend/app/db/postgres.py"]
synthesized: true
private: false
version: "1.0.0"
---

# Async Repository — `async_session_maker()` Pattern

RasoiAI's data layer uses async SQLAlchemy. All repositories MUST open a
fresh session per operation via `async_session_maker()` — the canonical
factory defined in `backend/app/db/postgres.py`. Stored sessions and
ambient context vars are banned.

## Why per-operation sessions

Async SQLAlchemy sessions are NOT thread-safe and are NOT coroutine-safe
when shared. A session opened in repository `__init__` and reused across
calls produces `MissingGreenlet: greenlet_spawn has not been called` or
connection-pool starvation under concurrent requests.

The per-operation pattern guarantees:

- Each query owns its own session, transaction, and connection checkout.
- Connections return to the pool immediately after the `async with` block.
- No shared mutable state between concurrent callers.

## Canonical shape

```python
from app.db.postgres import async_session_maker
from sqlalchemy import select
from sqlalchemy.orm import selectinload

class MealPlanRepository:
    async def get_by_id(self, plan_id: str) -> Optional[MealPlan]:
        async with async_session_maker() as session:
            result = await session.execute(
                select(MealPlan)
                .options(selectinload(MealPlan.items))
                .where(MealPlan.id == plan_id)
            )
            return result.scalar_one_or_none()
```

Required elements:

1. `async with async_session_maker() as session:` opens and closes the session.
2. `select(Model).where(...)` — use the v2-style SQLAlchemy query API.
3. `selectinload(Model.relation)` for every relationship accessed outside the
   `async with` block. Lazy loading after session close raises MissingGreenlet.
4. `result.scalar_one_or_none()` / `result.scalars().all()` to unwrap.

## MUST NOT patterns

```python
# BAD — session stored as instance attribute
class MealPlanRepository:
    def __init__(self, session):
        self.session = session                       # WRONG
    async def get_by_id(self, plan_id):
        return await self.session.execute(...)       # MissingGreenlet under load

# BAD — module-level session singleton
_session = async_sessionmaker(engine)()              # WRONG
async def get_user(): return await _session.execute(...)

# BAD — lazy loading relationship outside session
async def get_plan(plan_id):
    async with async_session_maker() as session:
        plan = (await session.execute(select(MealPlan).where(...))).scalar_one()
    return plan.items  # MissingGreenlet — session is closed
```

## Transactions across multiple queries

When two operations must be atomic, MUST use the same session inside one
`async with` block. MUST NOT open two sessions and hope autocommit lines up:

```python
async def create_household_with_owner(user_id: str, name: str) -> Household:
    async with async_session_maker() as session:
        household = Household(id=..., name=name, owner_id=user_id)
        session.add(household)
        await session.flush()                # emit INSERT, keep txn
        session.add(HouseholdMember(household_id=household.id, user_id=user_id))
        await session.commit()
        return household
```

## Write endpoints

Endpoints that perform writes MUST call repositories that wrap their own
sessions. MUST NOT inject `AsyncSession` as a FastAPI `Depends` parameter and
thread it through multiple repository calls — that silently re-uses one
session for operations that should be independent.

## Critical constraints

- MUST call `selectinload()` for every `relationship()` that the caller will
  traverse after the session closes. When in doubt, add it — the performance
  cost is minor; MissingGreenlet in prod is a P1.
- MUST NOT mix `asyncio.gather()` across separate sessions for a single
  logical transaction. Gather's concurrency interacts badly with the connection
  pool and can double-checkout from the same greenlet context.
- MUST NOT call `asyncio.run()` inside a repository. The event loop is owned
  by the FastAPI server; spawning another loop corrupts the session state.
- Read-only repositories may reuse the pattern unchanged. Nothing special is
  required for reads beyond `selectinload()` for relationships.
