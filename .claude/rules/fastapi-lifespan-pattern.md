---
description: FastAPI lifespan startup/shutdown sequence — initialization order, non-fatal cache warming, graceful shutdown.
globs: ["backend/app/main.py", "backend/app/db/**/*.py", "backend/app/cache/**/*.py"]
---

# FastAPI Lifespan Pattern

## Startup Sequence (Order Matters)

The lifespan context manager in `backend/app/main.py` MUST follow this initialization order:

```python
@asynccontextmanager
async def lifespan(app: FastAPI):
    # 1. External auth provider (Firebase) — no DB dependency
    initialize_firebase()

    # 2. Database connection pool — required by everything below
    await init_db()

    # 3. Cache warming — depends on DB, MUST be non-fatal
    try:
        await warm_recipe_cache()
    except Exception as e:
        logger.warning(f"Cache warm-up failed (non-fatal): {e}")

    yield

    # 4. Shutdown — close DB pool last
    await close_db()
```

## Rules

1. **Firebase before DB** — Firebase auth initialization has no database dependency and MUST complete before DB init so auth is available immediately.

2. **DB before cache** — Cache warming queries the database. If DB init fails, cache warming MUST NOT run (the exception propagates and prevents startup).

3. **Cache warming is non-fatal** — Wrap in `try/except`. A failed cache warm-up degrades performance but MUST NOT prevent the application from starting. Log as `warning`, not `error`.

4. **Shutdown reversal** — Close resources in reverse order of initialization. Database pool closes last because other shutdown hooks may need it.

5. **Log every step** — Each initialization step MUST log at `info` level on success. This provides startup diagnostics when debugging deployment issues.

## Sentry Initialization

Sentry MUST be initialized BEFORE the FastAPI app is created (at module level, not in lifespan):

```python
# Module level — before app = FastAPI(...)
if settings.sentry_dsn:
    sentry_sdk.init(
        dsn=settings.sentry_dsn,
        send_default_pii=False,  # NEVER send PII
        traces_sample_rate=0.1,
        environment="production" if not settings.debug else "development",
    )
```

This ensures Sentry captures errors during lifespan startup itself.

## Structured Logging Configuration

Production logging MUST use JSON format (configured at module level before lifespan):

```python
if not settings.debug:
    from pythonjsonlogger import jsonlogger
    handler = logging.StreamHandler()
    formatter = jsonlogger.JsonFormatter(
        "%(asctime)s %(name)s %(levelname)s %(message)s",
        rename_fields={"asctime": "timestamp", "levelname": "level"},
    )
    handler.setFormatter(formatter)
    logging.root.handlers = [handler]
```

## Anti-Patterns

- NEVER put Sentry init inside the lifespan — errors during startup won't be captured
- NEVER let cache warming crash the application — always wrap in try/except
- NEVER skip logging during startup — silent startups make deployment debugging impossible
- NEVER initialize DB connections at module import time — use the lifespan handler for async initialization
