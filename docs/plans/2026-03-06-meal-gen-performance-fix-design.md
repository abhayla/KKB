# Meal Generation Performance Fix — Design Document

**Date:** March 6, 2026
**Context:** Locust performance test revealed 98% of generation time is Gemini AI, 60% first-attempt failure rate, and test infrastructure issues blocking meaningful load testing.

---

## Problem Statement

The meal plan generation API (`POST /api/v1/meal-plans/generate`) has three categories of issues:

1. **Test infrastructure** — Rate limiting (5/hr) blocks 81.5% of Locust requests; all test users share one backend identity; no recipe rules seeded.
2. **Gemini reliability** — 60% first-attempt failure rate ("Day 0 missing snacks"), doubling average latency from ~50s to ~90s.
3. **Bug** — Substring matching in family constraint filter removes "unsweetened" items for DIABETIC members.

## Approach

Three phases executed sequentially:

- **Phase 1** — Fix test infrastructure (no production code changes except rate limit conditional)
- **Phase 2** — Improve Gemini reliability via `response_schema` + prompt simplification
- **Phase 3** — Fix filtering bug + update documentation

---

## Phase 1: Test Infrastructure

### 1A. Conditional Rate Limit in DEBUG Mode

**File:** `backend/app/api/v1/endpoints/meal_plans.py`

Change `@limiter.limit("5/hour")` to `@limiter.limit("500/hour" if settings.debug else "5/hour")`.

Same pattern for `photos.py` analyze endpoint: `10/hour` to `100/hour` in debug.

Production rate limits unchanged.

### 1B. Unique Backend Users Per Locust Profile

**File:** `backend/app/core/firebase.py`

Expand the debug auth handler from exact match (`== "fake-firebase-token"`) to prefix match (`.startswith("fake-firebase-token")`). Derive unique UID from the token suffix.

```
fake-firebase-token          → uid: fake-user-id (backward compatible)
fake-firebase-token-sharma   → uid: fake-user-sharma
fake-firebase-token-gupta    → uid: fake-user-gupta
```

**File:** `backend/tests/performance/test_profiles.json`

Update each profile's `firebase_token` to include the family name suffix.

### 1C. Seed Rules via API in Locust on_start

**File:** `backend/tests/performance/locustfile.py`

After authentication in `MealGenHeavyUser.on_start()`:
1. `PUT /api/v1/users/preferences` — dietary tags, allergies, dislikes, cooking times
2. `POST /api/v1/recipe-rules` — INCLUDE/EXCLUDE rules from profile
3. `POST /api/v1/users/family-members` — family member data

Add recipe rules to `test_profiles.json` for each family (e.g., Sharma: Chai DAILY, Mushroom NEVER).

---

## Phase 2: Gemini Reliability

### 2A. Add response_schema to Gemini Config

**File:** `backend/app/ai/gemini_client.py`

Add optional `response_schema` parameter to `generate_text()` and `generate_text_with_metadata()`. When provided, pass it to `GenerateContentConfig`.

**File:** `backend/app/services/ai_meal_service.py`

Define a `MEAL_PLAN_SCHEMA` constant using `google.genai.types.Schema` that enforces:
- Root: object with required `days` array
- Each day: required fields `date`, `day_name`, `breakfast`, `lunch`, `dinner`, `snacks`
- Each meal slot: array of objects with required `recipe_name`, `prep_time_minutes`, `dietary_tags`, `category`

Pass this schema to the Gemini call in `_generate_with_retry()`.

**Expected impact:** First-attempt success rate ~40% to ~95%+. Average generation time ~90s to ~50s.

### 2B. Simplify Prompt

**File:** `backend/app/services/ai_meal_service.py` — `_build_prompt()`

Remove the 2 full-day JSON examples (~2,500 chars). With `response_schema` handling structure, the prompt only needs preferences/rules/constraints. Add a brief note: "Return valid JSON with 7 days, 4 slots per day, 2 items per slot."

Prompt size: ~6,300 chars to ~3,800 chars.

---

## Phase 3: Bug Fix + Documentation

### 3. Word-Boundary Matching

**File:** `backend/app/services/ai_meal_service.py` — `_enforce_rules()`

Replace:
```python
if keyword in recipe_lower
```
With:
```python
if re.search(r'\b' + re.escape(keyword) + r'\b', recipe_lower)
```

Fixes: "unsweetened" no longer matches "sweet". "Sweet Lassi" still matches.

### 4. Update Performance Targets

**File:** `backend/tests/performance/README.md`

| Metric | Old Target | New Target |
|--------|-----------|-----------|
| Meal gen p50 | <30s | <60s |
| Meal gen p95 | <60s | <90s |
| Meal gen p99 | <120s | <120s (unchanged) |

---

## Files Changed

| File | Phase | Production Impact |
|------|-------|-------------------|
| `app/api/v1/endpoints/meal_plans.py` | 1A | Conditional rate limit (debug only) |
| `app/api/v1/endpoints/photos.py` | 1A | Conditional rate limit (debug only) |
| `app/core/firebase.py` | 1B | Expanded debug token matching |
| `tests/performance/test_profiles.json` | 1B, 1C | Test data only |
| `tests/performance/locustfile.py` | 1C | Test code only |
| `app/ai/gemini_client.py` | 2A | Optional response_schema param |
| `app/services/ai_meal_service.py` | 2A, 2B, 3 | Schema, shorter prompt, regex fix |
| `tests/performance/README.md` | 4 | Docs only |

**Total: 8 files** (4 production, 2 test, 2 docs)

---

## Verification Plan

After each phase, re-run Locust `mealgen` profile and compare:

| Metric | Current | After Phase 1 | After Phase 2 | After Phase 3 |
|--------|---------|--------------|--------------|--------------|
| Requests blocked by 429 | 81.5% | ~0% | ~0% | ~0% |
| Unique users | 1 | 5 | 5 | 5 |
| INCLUDE/EXCLUDE rules | 0 | Per profile | Per profile | Per profile |
| First-attempt success | ~40% | ~40% | ~95% | ~95% |
| Avg generation time | ~90s | ~90s | ~50s | ~50s |
| False positive removals | 2/plan | 2/plan | 2/plan | 0/plan |
