---
description: >
  Gemini AI calls for meal generation use a flat schema with short property names to avoid the
  "too many states" error. Post-processing validates and enforces rules the AI might miss.
globs: ["backend/app/services/ai_meal_service.py", "backend/app/ai/**/*.py"]
synthesized: true
private: false
---

# Gemini Structured Output Pattern

Meal generation via Gemini uses `response_json_schema` with a carefully designed flat schema. Deeply nested or verbose schemas trigger Gemini's "too many states" error.

## Schema design — short keys

The schema uses abbreviated property names to reduce state space:

| Short key | Meaning | Example |
|-----------|---------|---------|
| `d` | date | "2025-01-20" |
| `dn` | day_name | "Monday" |
| `b` | breakfast | [...] |
| `l` | lunch | [...] |
| `di` | dinner | [...] |
| `s` | snacks | [...] |
| `n` | recipe_name | "Paneer Butter Masala" |
| `t` | prep_time_minutes | 30 |
| `tags` | dietary_tags | ["VEG", "PROTEIN_RICH"] |
| `c` | category | "North Indian" |

The prompt explains these abbreviations so the AI knows what to emit. The response is then mapped back to full property names in post-processing.

## Generation flow

```
User Request
    ↓
Build prompt (preferences, rules, festivals, family constraints)
    ↓
Gemini API call (gemini-2.5-flash, thinking_budget=0, response_json_schema)
    ↓
Parse JSON response (short keys → full keys)
    ↓
Post-processing validation:
  - Enforce EXCLUDE rules (remove forbidden items)
  - Enforce allergen restrictions
  - Validate meal type distribution
  - Apply festival/fasting day overrides
    ↓
Return validated MealPlan
```

## Retry logic

Generation uses exponential backoff with 3 retries. Each retry adds more explicit constraints to the prompt if the previous response violated rules.

## Key config

| Setting | Value | Location |
|---------|-------|---------|
| Model | `gemini-2.5-flash` | `app/ai/gemini_client.py` — `MODEL_NAME` constant |
| SDK | `google-genai` (NOT `google-generativeai`) | Native async via `client.aio` |
| Thinking budget | 0 | Disables chain-of-thought for faster structured output |
| Endpoint timeout | 180s | AI takes ~30-40s typically |
| E2E test timeout | 30s | For test assertions waiting on meal plan |

## MUST NOT

- MUST NOT use verbose/nested JSON schemas with Gemini — triggers "too many states" error
- MUST NOT rely solely on AI to enforce dietary rules — post-processing validation is mandatory
- MUST NOT use `google-generativeai` SDK — the project uses `google-genai` with native async
- MUST NOT set `thinking_budget > 0` for structured output — it adds latency without improving quality for schema-constrained responses
- MUST NOT skip post-processing even if the AI response looks correct — rule enforcement must be deterministic, not probabilistic
