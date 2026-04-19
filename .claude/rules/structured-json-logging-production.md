---
description: >
  Production logging uses python-json-logger JsonFormatter writing to stdout
  in single-line JSON (timestamp, level, name, message, exception). Dev
  logging falls back to plain text. Sentry is configured when SENTRY_DSN
  is set.
globs: ["backend/app/main.py", "backend/app/config.py", "backend/app/**/*.py"]
synthesized: true
private: false
version: "1.0.0"
---

# Structured JSON Logging — Production Configuration

The backend's log configuration lives in `backend/app/main.py` at app
bootstrap. Production (`settings.debug == False`) runs JSON logs for the
log aggregator; development runs plain text for human readability.

## Wiring (authoritative snippet)

```python
if not settings.debug:
    from pythonjsonlogger import jsonlogger

    handler = logging.StreamHandler()
    formatter = jsonlogger.JsonFormatter(
        "%(asctime)s %(name)s %(levelname)s %(message)s",
        rename_fields={"asctime": "timestamp", "levelname": "level"},
    )
    handler.setFormatter(formatter)
    root = logging.getLogger()
    root.handlers = [handler]
    root.setLevel(logging.INFO)
```

Required elements:

- `rename_fields={"asctime": "timestamp", "levelname": "level"}` — the log
  aggregator expects `timestamp`/`level`, not the defaults.
- `StreamHandler` → stdout. Logs are collected by the container runtime.
  MUST NOT log to a file — file paths differ across dev/VPS/Docker.
- Replace root handlers wholesale (`root.handlers = [handler]`). MUST NOT
  append — uvicorn adds its own handler during import and you'd get double
  lines.
- Level is `INFO` in prod. `DEBUG` floods the aggregator and costs real
  money at scale.

## Log event shape

Every structured log call MUST use keyword `extra=` fields, not f-strings:

```python
# GOOD — structured fields queryable in aggregator
logger.info("meal_plan_generated",
            extra={"user_id": user.id, "plan_id": plan.id, "duration_ms": dt})

# BAD — buried in message text, unqueryable
logger.info(f"generated plan {plan.id} for user {user.id} in {dt}ms")
```

Event name convention: snake_case, past tense verb. `meal_plan_generated`,
`household_member_added`, `auth_token_refreshed`. MUST NOT mix prose and
structured — pick one per line.

## Sentry integration

`backend/app/main.py` imports and initializes Sentry at module top, BEFORE
the FastAPI app is created:

```python
if settings.sentry_dsn:
    import sentry_sdk
    sentry_sdk.init(
        dsn=settings.sentry_dsn,
        traces_sample_rate=0.1,
        environment=settings.environment,
    )
```

`SENTRY_DSN` is optional — its absence means Sentry is disabled. MUST NOT
hard-require Sentry; dev environments should run without it.

## Exception logging

`logger.exception(...)` (NOT `logger.error(...)`) MUST be used inside
`except` blocks so the traceback is attached:

```python
try:
    await ai_meal_service.generate(...)
except Exception:
    logger.exception("meal_plan_generation_failed",
                     extra={"user_id": user.id})
    raise
```

The JsonFormatter automatically includes `exc_info` when `logger.exception`
is used. MUST NOT manually format tracebacks into the message.

## Dev environment behavior

When `settings.debug == True`, the JSON formatter is skipped entirely.
Python's default logging kicks in — text output, DEBUG level, suitable for
`uvicorn --reload`. This means:

- `/docs` is available (Swagger UI)
- Fake phone auth is enabled (see `e2e-fake-auth.md`)
- JSON formatting is off

MUST NOT deploy with `DEBUG=true`. The `settings-configuration-validation.md`
rule enforces that `DEBUG=true` is rejected when `ENVIRONMENT=production`.

## Correlation IDs

Incoming requests SHOULD carry `X-Correlation-ID`. If present, the header
MUST be echoed in the response and included in every log line produced
during that request handling. Middleware lives in
`app/middleware/correlation_id.py` (create it if missing — do not scatter
`request.headers.get(...)` calls across endpoints).

## Critical constraints

- MUST NOT log raw token values, passwords, Firebase UIDs (beyond the
  first 8 chars), or `X-API-Key` headers. Redact via a filter, or
  drop the field entirely.
- MUST NOT put PII (email addresses, phone numbers, addresses) in
  structured log fields. Log an opaque user_id instead and let the
  aggregator join against the user DB if needed.
- MUST NOT `print()` inside app code. `print` bypasses the logger and
  produces unstructured noise in the aggregator. Use
  `logger.debug/info/warning/error` always.
- MUST NOT change the logger configuration from inside a request handler.
  Module-level config only — per-request log tweaks cause cross-request
  pollution.
