---
description: >
  Gemini structured-output schemas for meal generation MUST use short
  property names (d, dn, b, l, di, s, n, t, tags, c, cal), wrap generation
  in exponential-backoff retry (3 attempts, 1s→2s→4s), validate the parsed
  JSON post-response, and log attempt counts.
globs: ["backend/app/services/ai_meal_service.py", "backend/app/ai/*.py", "backend/app/ai/**/*.py"]
synthesized: true
private: false
version: "1.0.0"
---

# Gemini Structured Output — Token & State Budget

Gemini's `response_json_schema` is compiled into a state machine server-side.
Long property names, deep nesting, and large `required` lists explode the
state count. In RasoiAI this caused `"too many states"` errors on 7-day meal
plans until the schema was compressed. This rule encodes that compression so
it does not regress.

## Short-keyed schema

Top-level meal plan schemas MUST use the canonical short-key vocabulary:

| Key | Meaning | Used in |
|-----|---------|---------|
| `d` | date (ISO) | `MEAL_PLAN_SCHEMA.days[].d` |
| `dn` | day_name (Monday, Tuesday...) | `.days[].dn` |
| `b` | breakfast | `.days[].b` |
| `l` | lunch | `.days[].l` |
| `di` | dinner | `.days[].di` |
| `s` | snack | `.days[].s` |
| `n` | meal item name | `.days[].b.n` |
| `t` | time in minutes | `.days[].b.t` |
| `tags` | comma-separated tags | `.days[].b.tags` |
| `c` | cuisine | `.days[].b.c` |
| `cal` | calories (integer) | `.days[].b.cal` |

MUST NOT introduce new long-form keys (`breakfast_name`, `cooking_time`) into
the Gemini schema. If a new attribute is needed, add a short key and document
it in the table above.

Source of truth: `backend/app/services/ai_meal_service.py::_MEAL_ITEM_SCHEMA`
and `backend/app/ai/gemini_client.py::MEAL_PLAN_SCHEMA`. These MUST stay in
sync.

## Retry & backoff

Meal generation MUST be wrapped in this backoff contract:

- **Max attempts:** 3 (configurable via `settings.gemini_max_retries`, default 3)
- **Delays:** 1.0s, 2.0s, 4.0s between attempts (base=1, factor=2)
- **Retry on:** `google.genai.errors.ServerError`, JSON parse failure on
  the response, `GeminiResponseValidationError`
- **Do NOT retry on:** 4xx client errors (bad API key, quota exceeded,
  `PermissionDenied`). These will not succeed on retry and waste quota.

```python
for attempt in range(settings.gemini_max_retries):
    try:
        response = await client.aio.models.generate_content(...)
        parsed = _parse_and_validate(response.text)
        logger.info("gemini_meal_gen_success", attempt=attempt + 1)
        return parsed
    except (ServerError, GeminiResponseValidationError) as e:
        if attempt == settings.gemini_max_retries - 1:
            logger.error("gemini_meal_gen_exhausted", attempts=attempt + 1)
            raise
        await asyncio.sleep(2 ** attempt)
```

## Post-response validation

After `response.text` parses as JSON, MUST validate:

1. All 7 days are present (`len(parsed["days"]) == 7`).
2. Each day has at least `b`, `l`, `di` entries (snack optional).
3. Meal item `n` (name) is non-empty and `t` (time) is a positive int.

Validation failure MUST raise `GeminiResponseValidationError` — this triggers
retry. MUST NOT return a partial meal plan to the caller; a 6-day plan breaks
the Android UI which assumes a full week.

## Observability

Every generation call MUST emit structured logs with keys:

- `gemini_meal_gen_start` — `user_id`, `week_start_date`
- `gemini_meal_gen_success` — `attempt` (1-indexed), `duration_ms`, `tokens_used`
- `gemini_meal_gen_retry` — `attempt`, `error_type`, `error_msg`
- `gemini_meal_gen_exhausted` — `attempts`

These feed the Sentry dashboard and the weekly generation SLO review.

## Critical constraints

- MUST NOT change schema keys without checking the Kotlin DTO counterpart
  (`android/domain/src/main/java/com/rasoiai/domain/model/MealItem.kt`). The
  Android client rehydrates short keys to long names; a silent key rename
  produces blank meal cards on-device.
- MUST NOT set `temperature` above 0.4 for meal generation. Higher values
  yield creative but unserveable combinations (e.g. "breakfast pizza with
  curry leaves") that fail the household safety check.
- The endpoint timeout is **180s** (`/api/v1/meal-plans/generate`). Typical
  run is ~35s. If p95 exceeds 60s for a week, investigate before raising
  the timeout — slower generation usually signals a prompt or schema
  regression, not load.
