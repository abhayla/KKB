---
description: >
  Background work launched from endpoint handlers MUST use
  `asyncio.create_task()` as fire-and-forget, MUST wrap the coroutine body
  in a try/except that logs but never raises, and MUST NOT hold a reference
  to the Task in the request context.
globs: ["backend/app/api/v1/endpoints/**/*.py", "backend/app/main.py", "backend/app/services/**/*.py"]
synthesized: true
private: false
version: "1.0.0"
---

# Async Background Tasks — Fire-and-Forget Pattern

Expensive post-response work (recipe enrichment, catalog cache warming,
notification dispatch) runs in the background so the endpoint returns quickly.
RasoiAI uses raw `asyncio.create_task()` for this — NOT FastAPI
`BackgroundTasks`, NOT Celery, NOT `asyncio.gather`.

## Why not FastAPI BackgroundTasks

`fastapi.BackgroundTasks` runs AFTER the response is flushed but BEFORE the
connection closes. That means the client still waits on the socket for as
long as the background work takes — defeating the purpose. `asyncio.create_task`
schedules the coroutine on the event loop and returns immediately; the client
receives the response and reconnects are free.

## Canonical shape

```python
@router.post("/generate", response_model=MealPlanResponse)
async def generate_meal_plan(
    req: GenerateMealPlanRequest, user: CurrentUser
) -> MealPlanResponse:
    plan = await ai_meal_service.generate_meal_plan(user.id, req)

    async def _enrich_recipes() -> None:
        try:
            await recipe_service.enrich_with_nutrition(plan.id)
            logger.info("background_recipe_enrichment_done", plan_id=plan.id)
        except Exception:
            logger.exception("background_recipe_enrichment_failed",
                             plan_id=plan.id)

    asyncio.create_task(_enrich_recipes())
    logger.info("Background recipe enrichment task started",
                plan_id=plan.id)

    return plan
```

Required elements:

1. The background work is a **nested** `async def` inside the endpoint.
   This captures the request context (user_id, plan_id) in closure and
   keeps the handler self-contained.
2. The nested function's body is wrapped in `try/except Exception`.
   Unhandled exceptions in `create_task` coroutines log an
   `asyncio.Task exception was never retrieved` warning and then vanish —
   the log is hard to attribute to a specific user.
3. Log entries are structured (`background_*_done`, `background_*_failed`)
   so the observability dashboard can track success rate.
4. `asyncio.create_task(...)` is called *without* binding the result
   (`asyncio.create_task(_enrich_recipes())`, not
   `task = asyncio.create_task(...)`). A bound reference creates
   false hope that the caller will await it. If you need the result,
   use `await`, not a bg task.

## MUST NOT patterns

```python
# BAD — BackgroundTasks delays the response
@router.post(...)
async def generate(bg: BackgroundTasks, ...):
    bg.add_task(enrich)              # client waits for enrich to finish
    return plan

# BAD — fire-and-forget with no error handler
async def _enrich():
    await recipe_service.enrich(...)  # any error becomes a silent task-never-awaited

asyncio.create_task(_enrich())        # WRONG — no try/except

# BAD — binding the task and then not awaiting it
task = asyncio.create_task(_enrich()) # implies caller will await; they don't
return plan

# BAD — asyncio.gather for fire-and-forget
await asyncio.gather(_enrich(), _warm_catalog())  # this BLOCKS the response
```

## Lifespan / startup tasks

Long-lived startup work in `app.main.lifespan` (e.g. `warm_recipe_cache`)
MUST also wrap in try/except and log structured events. MUST NOT let a
startup task crash take the app down — catch + log + proceed to ready.

```python
async def lifespan(app: FastAPI):
    try:
        asyncio.create_task(warm_recipe_cache())
        logger.info("startup_cache_warming_scheduled")
    except Exception:
        logger.exception("startup_cache_warming_failed")
    yield
    # ... shutdown
```

## Shutdown consideration

Fire-and-forget tasks do NOT block shutdown. If the server is killed while
a background task is running, the task is cancelled. This is accepted —
the operations used here (recipe enrichment, cache warming) are
idempotent and can be retried on next invocation. MUST NOT use this
pattern for writes that require durability (ledger entries, audit events)
— those go through a proper persistent queue.

## Critical constraints

- MUST NOT call `create_task` with work that must be durable. Durability
  means a persistent queue (Redis, PostgreSQL table), not the event loop.
- MUST NOT use `create_task` for work that reads data written seconds ago
  and assumes it's visible. The background task can race with the request's
  commit — use `session.commit()` and pass IDs, not ORM objects.
- MUST NOT spawn more than one bg task per request. If you need multiple,
  build an orchestrator coroutine that runs them serially inside one task.
  Parallel bg tasks multiply connection pool contention.
- Every background task MUST log both a "started" and a "done/failed" event
  with the same correlation ID. One without the other makes on-call
  unable to tell whether the task ran.
