---
description: >
  Enforce abbreviated schema keys when calling Gemini structured output API.
  Full property names cause Gemini's "too many states" error on complex schemas.
globs: ["backend/app/ai/**/*.py", "backend/app/services/ai_*.py"]
synthesized: true
private: false
---

# Gemini Structured Output — Abbreviated Keys

## The Problem

Google Gemini's structured output API rejects schemas with long property names or deeply nested structures, throwing a "too many states" error. This is a Gemini-specific limitation, not a general JSON schema issue.

## The Convention

All Gemini structured output schemas MUST use abbreviated single-letter or short keys:

| Full name | Abbreviated | Used in |
|-----------|------------|---------|
| `recipe_name` | `n` | Meal item |
| `prep_time_minutes` | `t` | Meal item |
| `dietary_tags` | `tags` | Meal item |
| `category` | `c` | Meal item |
| `calories` | `cal` | Meal item |
| `date` | `d` | Meal plan day |
| `day_name` | `dn` | Meal plan day |
| `breakfast` | `b` | Meal plan day |
| `lunch` | `l` | Meal plan day |
| `dinner` | `di` | Meal plan day |
| `snacks` | `s` | Meal plan day |

## MUST DO

- Define schemas as flat Python dicts with short keys (see `MEAL_PLAN_SCHEMA` in `ai_meal_service.py`)
- Explain abbreviations in the prompt text so Gemini knows what to emit
- Post-process the abbreviated response to expand keys back to full names before storing in the database
- Keep schema depth to max 2 levels (object → array → object) — deeper nesting triggers the error

## MUST NOT

- NEVER use full property names (`recipe_name`, `prep_time_minutes`) in Gemini schemas — they will be rejected
- NEVER nest schemas deeper than 2 levels — flatten complex structures
- NEVER assume Gemini's structured output works like OpenAI's — the constraints are different
- NEVER skip the prompt explanation of abbreviations — Gemini needs context to emit the right keys
