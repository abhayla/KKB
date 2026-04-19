---
description: >
  Backend Pydantic schema organization — one module per domain under
  backend/app/schemas/, split into Request/Response/DTO classes, with
  ConfigDict(from_attributes=True) on any class consumed from SQLAlchemy
  models. Short-keyed DTOs are reserved for Gemini structured output.
globs: ["backend/app/schemas/**/*.py", "backend/app/ai/*_client.py"]
synthesized: true
private: false
version: "1.0.0"
---

# Pydantic Schema Organization — Request / Response / DTO

Every public schema in `backend/app/schemas/` MUST be classified as one of
three kinds. Keeping these kinds distinct (and named) is how the codebase
signals *direction of flow* — which schemas are inputs vs outputs vs
DTOs that cross the API↔Android boundary.

## Classification

| Suffix | Purpose | Usage site |
|--------|---------|------------|
| `...Request` | Deserializes an inbound HTTP body. `@router.post(..., body: FooRequest)` | Endpoint parameters |
| `...Response` | Serializes an outbound HTTP body. `response_model=FooResponse` | Endpoint `response_model` |
| `...Dto` | Portable object shared between API, AI prompts, and Android (via `pydantic-android-schema-sync`) | Nested inside Response schemas; also referenced from Android |

MUST NOT name a class after its domain alone (e.g. `Household`). The suffix is
load-bearing — it tells the reader whether mutations are allowed and who owns
the shape.

## File layout

- One file per domain: `auth.py`, `household.py`, `meal_plan.py`,
  `recipe_rules.py`, etc.
- Request classes go first, Response classes next, DTOs last within the file.
- Cross-domain DTOs that don't fit one domain (e.g. `FestivalDto` used by both
  meal_plan and calendar) live in the domain they were born in and are imported
  by the other — they MUST NOT be centralized into a `schemas/common.py`
  catch-all.

## SQLAlchemy conversion

Any Pydantic class that will be constructed from a SQLAlchemy model
(`Response.model_validate(orm_obj)`) MUST declare:

```python
model_config = ConfigDict(from_attributes=True)
```

MUST NOT use the deprecated `class Config: orm_mode = True` — Pydantic v2
projects drop this eventually. Evidence: `meal_plan.py::MealPlanResponse`,
`household.py::HouseholdResponse`, `auth.py::AuthResponse` all use
`ConfigDict(from_attributes=True)`.

## Short-keyed Gemini DTOs

DTOs consumed by the Gemini `response_json_schema` (meal plan generation,
recipe drafting) MUST use single-letter or 2-letter keys:

```python
class _MealItemDto(BaseModel):
    n: str     # name
    t: int     # time (minutes)
    tags: str
    c: str     # cuisine
    cal: int   # calories
```

Why: Gemini's structured output has a state-machine limit. Long property
names explode the state count and produce `"too many states"` errors on
large meal plans. See `backend/app/ai/gemini_client.py::MEAL_PLAN_SCHEMA`
for the exhaustive key list (`d`, `dn`, `b`, `l`, `di`, `s`, `n`, `t`,
`tags`, `c`, `cal`). Adding a new key means also updating the response
expansion in `ai_meal_service.py` where short keys are rehydrated to full
model fields.

## Critical constraints

- MUST NOT expose `_MealItemDto`-style short-key schemas on public endpoints.
  They are internal to the Gemini adapter. Public meal-plan responses return
  `MealItemDto` with full names.
- MUST NOT reuse a Request class as a Response. If the shape is the same
  today, create both classes — they diverge fast (Response gains `id`,
  `created_at`, etc.).
- MUST NOT embed validation logic in the SQLAlchemy model. Validation lives
  in the Pydantic schema; the model stores whatever the schema accepted.
- When a schema adds or removes a field, search `android/data/src/main/java`
  for the matching Kotlin DTO and update both in the same commit — this is
  enforced by the companion rule `pydantic-android-schema-sync.md`.
