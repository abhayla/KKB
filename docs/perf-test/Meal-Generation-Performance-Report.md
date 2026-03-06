# Meal Plan Generation API - Comprehensive Performance Test Report

**Date:** March 6, 2026
**Tool:** Locust 2.43.3
**Backend:** FastAPI + PostgreSQL + Gemini 2.5 Flash
**Test Profile:** `MealGenHeavyUser` (focused AI-only)
**Config:** 3 concurrent users, 5 minutes, all requests = meal plan generation
**Report Author:** Claude Code (automated performance analysis)

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Test Setup & Configuration](#2-test-setup--configuration)
3. [Test Results Overview](#3-test-results-overview)
4. [Request Timeline (Chronological)](#4-request-timeline-chronological)
5. [Detailed Pipeline Breakdown](#5-detailed-pipeline-breakdown)
6. [API Call Chain Analysis](#6-api-call-chain-analysis)
7. [Database Query Analysis](#7-database-query-analysis)
8. [Gemini AI Analysis](#8-gemini-ai-analysis)
9. [Rate Limiting Analysis](#9-rate-limiting-analysis)
10. [Family Member Handling](#10-family-member-handling)
11. [INCLUDE/EXCLUDE Rules Analysis](#11-includeexclude-rules-analysis)
12. [False Positive Filtering Bug](#12-false-positive-filtering-bug)
13. [Latency Distribution](#13-latency-distribution)
14. [Performance Targets vs Actuals](#14-performance-targets-vs-actuals)
15. [Issues & Recommendations](#15-issues--recommendations)
16. [Source Code References](#16-source-code-references)
17. [Raw Data](#17-raw-data)

---

## 1. Executive Summary

| Metric | Value |
|--------|-------|
| Total requests | **27 meal generations + 3 auth** |
| Successful generations | **2 of 27 (7.4%)** |
| 504 Timeouts | **3 (11.1%)** |
| 429 Rate Limited | **22 (81.5%)** |
| Avg latency (all requests) | **22.1s** |
| Median latency | **2.3s** (skewed by fast 429 responses) |
| Max latency | **122.4s** (timeout) |
| Auth latency (avg) | **3.3s** |
| Real generation latency | **81-100s** (successful only) |
| Gemini tokens per call | **21,000-25,000** |

**Key Finding:** The Gemini AI call accounts for **98% of total generation time** (80-98s out of 81-100s total). Everything else (DB queries, recipe creation, saving) totals ~2s. Rate limiting (5/hour) blocked 81.5% of test requests, making the test infrastructure itself a bottleneck.

---

## 2. Test Setup & Configuration

### 2.1 Backend Environment

| Component | Value |
|-----------|-------|
| Backend | FastAPI with uvicorn `--reload` |
| Database | PostgreSQL (remote) |
| AI Model | Gemini 2.5 Flash (`gemini-2.5-flash`) via `google-genai` SDK |
| Auth Mode | `DEBUG=true` (fake-firebase-token accepted) |
| Rate Limiting | `slowapi` with per-endpoint decorators |
| Server Timeout | `asyncio.wait_for(timeout=120)` on generation |
| Recipes in DB | 3,580 |

### 2.2 Locust Configuration

**Profile file:** `backend/tests/performance/profiles/meal_gen_focused.conf`

```conf
locustfile = tests/performance/locustfile.py
headless = true
users = 3
spawn-rate = 1
run-time = 5m
host = http://localhost:8000
html = tests/performance/reports/meal_gen_report.html
csv = tests/performance/reports/meal_gen
```

**User class:** `MealGenHeavyUser` — every request is a `/api/v1/meal-plans/generate` POST. Wait time between requests: 5-15 seconds (random).

### 2.3 Test Profiles

5 varied Indian family profiles in `test_profiles.json`, randomly assigned to Locust users:

| Profile | Diet | Allergies | Members | Busy Days |
|---------|------|-----------|---------|-----------|
| **Sharma** | Vegetarian + Sattvic | Peanuts (SEVERE), Cashews (MILD) | 3 (Ramesh 45 DIABETIC, Sunita 42 LOW_SALT, Aarav 12 NO_SPICY) | Mon/Wed/Fri |
| **Gupta** | Eggetarian | Shellfish (SEVERE) | 4 (Rajesh 50, Priya 47 HIGH_PROTEIN, Rohit 22, Neha 18) | Tue/Thu |
| **Reddy** | South Indian Vegetarian | None | 2 (Venkat 35, Lakshmi 33) | All weekdays |
| **Khan** | Non-Veg + Halal | Soy (MODERATE) | 5 (incl. Dadi 78 SOFT_FOOD) | Wed only |
| **Jain** | Strict Jain | None | 3 (Hemant 40, Meena 38, Tanvi 8 NO_SPICY) | None |

**Critical limitation:** All 5 profiles use `"firebase_token": "fake-firebase-token"`, meaning they all authenticate as the **same backend user** (`5f8d9d89-55b3-4929-bdd8-c98abc423d3e`). This causes them to share a single rate limit bucket.

### 2.4 Validation Performed by Locust

Each successful response is validated for:
1. HTTP 200 status code
2. 7 days present in `days` array
3. All 4 meal slots per day (`breakfast`, `lunch`, `dinner`, `snacks`)
4. Each slot has >= 1 item with a `recipe_name`
5. No allergens in recipe names (matched against test profile)

---

## 3. Test Results Overview

### 3.1 Request Summary

| Endpoint | Requests | Failures | Avg Latency | Min | Max | Median |
|----------|----------|----------|-------------|-----|-----|--------|
| `POST /api/v1/auth/firebase [MealGen]` | 3 | 0 | 3,337ms | 2,944ms | 3,971ms | 3,100ms |
| `POST /api/v1/meal-plans/generate [FOCUSED]` | 27 | 25 | 22,062ms | 2,137ms | 122,436ms | 2,300ms |
| **Aggregated** | **30** | **25** | **20,190ms** | **2,137ms** | **122,436ms** | **2,300ms** |

### 3.2 Failure Breakdown

| Failure Type | Count | Percentage | Meaning |
|-------------|-------|------------|---------|
| HTTP 429 (Rate Limited) | 22 | 81.5% | `slowapi` rejected — exceeded 5/hour limit |
| HTTP 504 (Timeout) | 3 | 11.1% | Server-side `asyncio.wait_for(timeout=120)` expired |
| **Success (HTTP 200)** | **2** | **7.4%** | Full generation completed |

### 3.3 Successful Generations Detail

| # | Total Time | AI Time | Recipe Creation | DB Save | Tokens | New Recipes | Existing Recipes | Items |
|---|-----------|---------|----------------|---------|--------|-------------|-----------------|-------|
| 1 | **99.5s** | 97.7s | 1.5s | 0.4s | 25,322 | 20 | 22 | 54 |
| 2 | **81.4s** | 80.2s | 0.9s | 0.3s | 21,426 | 14 | 31 | 54 |

---

## 4. Request Timeline (Chronological)

Complete timeline reconstructed from backend logs:

```
TIME        EVENT                                    DURATION    STATUS
────────────────────────────────────────────────────────────────────────
13:36:44    User 1 authenticates                     2.9s        200 OK
13:36:45    User 1 → generation #1 starts            -           -
13:36:46    User 2 authenticates                     3.1s        200 OK
13:36:46    User 2 → generation #2 starts            -           -
13:36:48    User 3 authenticates                     4.0s        200 OK
13:36:48    User 3 → generation #3 starts            -           -

            [3 concurrent Gemini calls in flight for same user]

13:38:06    Gen #1: Attempt 1 fails                  ~77s        "Day 0 missing 'snacks'"
13:38:07    Gen #1: Retry attempt 2 starts           -           -
13:38:24    Gen #2: Attempt 1 fails                  ~98s        "Day 0 missing 'snacks'"
13:38:25    Gen #2: Retry attempt 2 starts           -           -
13:38:26    Gen #1: Attempt 2 SUCCEEDS               ~19s        200 OK (total: 99.5s)
13:38:28    Gen #1: Recipe creation + DB save         1.9s        54 items, 20 new recipes

13:38:42    User 1 → generation #4 starts            -           -
13:38:45    Gen #2: TIMEOUT                          120s        504 Gateway Timeout
13:38:46    Gen #3: TIMEOUT                          120s        504 Gateway Timeout

13:39:00    User 2 → generation #5 starts            -           -
13:39:01    >>> RATE LIMIT HIT (5/hr) <<<            -           429 Too Many Requests

            [All subsequent requests rate-limited]

13:39:01    Rate limited                              -           429
13:39:17    Rate limited                              -           429
13:39:29    Rate limited                              -           429
13:39:45    Rate limited                              -           429
13:40:00    Rate limited                              -           429

13:40:04    Gen #4: SUCCEEDS (was in-flight before rate limit)
                                                     81.4s       200 OK

13:40:12-   22 more rate-limited requests             ~2.3s each  429
13:41:42

13:41:42    TEST ENDS (5 minutes elapsed)
```

**Key observation:** Only the first ~2 minutes of the 5-minute test produced real Gemini calls. After the 5th request, the rate limiter blocked everything.

---

## 5. Detailed Pipeline Breakdown

### 5.1 Full Pipeline for a Successful Generation (99.5s)

```
                         Total: 99.5 seconds
┌──────────────────────────────────────────────────────────────────┐
│                                                                  │
│  ┌─── Load Preferences (0.9s) ─────────────────────────────┐    │
│  │  1. user_repo.get_preferences()           [DB Query #1]  │    │
│  │  2. SELECT RecipeRule WHERE INCLUDE        [DB Query #2]  │    │
│  │  3. SELECT RecipeRule WHERE EXCLUDE        [DB Query #3]  │    │
│  │  4. SELECT NutritionGoal                   [DB Query #4]  │    │
│  │  5. user_repo.get_family_members()         [DB Query #5]  │    │
│  │     Result: 3 members (Ramesh, Sunita, Aarav)             │    │
│  └───────────────────────────────────────────────────────────┘    │
│                                                                  │
│  ┌─── Filter Conflicting Rules (~0.001s) ───────────────────┐    │
│  │  Check INCLUDE rules vs family constraints                │    │
│  │  (0 rules to filter — no INCLUDE rules exist)             │    │
│  └───────────────────────────────────────────────────────────┘    │
│                                                                  │
│  ┌─── Load Festivals (0.2s) ───────────────────────────────┐     │
│  │  get_festivals_for_date_range(week_start, week_end)      │     │
│  │  Result: no festivals this week                           │     │
│  └───────────────────────────────────────────────────────────┘    │
│                                                                  │
│  ┌─── Load Config (0.3s) ──────────────────────────────────┐     │
│  │  config_service.get_config()                              │     │
│  │  Result: 50 pairing rules, 44 categories                 │     │
│  └───────────────────────────────────────────────────────────┘    │
│                                                                  │
│  ┌─── Build Prompt (~0.001s) ──────────────────────────────┐     │
│  │  String formatting: 6,297 characters                      │     │
│  │  Contains: user prefs, family members, rules,             │     │
│  │  festivals, pairing guidance, JSON schema example         │     │
│  └───────────────────────────────────────────────────────────┘    │
│                                                                  │
│  ┌─── GEMINI AI CALL — ATTEMPT 1 (~77s) ───────────────────┐    │
│  │  generate_text_with_metadata(prompt)                      │    │
│  │  → Gemini processes 6,297 char prompt                     │    │
│  │  → Returns JSON... but INVALID                            │    │
│  │  → _validate_response_structure() FAILS:                  │    │
│  │    "Day 0 missing 'snacks' field"                         │    │
│  └───────────────────────────────────────────────────────────┘    │
│                                                                  │
│  ┌─── Backoff Sleep (1s) ──────────────────────────────────┐     │
│  │  await asyncio.sleep(2^0) = 1 second                      │     │
│  └───────────────────────────────────────────────────────────┘    │
│                                                                  │
│  ┌─── GEMINI AI CALL — ATTEMPT 2 (~19s) ───────────────────┐    │
│  │  generate_text_with_metadata(prompt)  [SAME prompt]       │    │
│  │  → Returns VALID JSON                                     │    │
│  │  → _validate_response_structure() PASSES                  │    │
│  │  → 25,322 tokens consumed (both attempts combined)        │    │
│  └───────────────────────────────────────────────────────────┘    │
│                                                                  │
│  ┌─── Parse Response (~0.001s) ────────────────────────────┐     │
│  │  JSON → GeneratedMealPlan with 7 DayMeals               │     │
│  │  56 MealItem objects created (7 days × 4 slots × 2)      │     │
│  └───────────────────────────────────────────────────────────┘    │
│                                                                  │
│  ┌─── Post-Processing / Enforce Rules (~0.001s) ───────────┐    │
│  │  Check allergens (Peanuts, Cashews) in recipe names       │    │
│  │  Check family constraints (DIABETIC → "sweet" keyword)    │    │
│  │  REMOVED: "Herbal Tea (unsweetened)" ← FALSE POSITIVE    │    │
│  │  REMOVED: "Plain Curd Lassi (unsweetened)" ← FALSE POS.  │    │
│  │  Result: 54 items remaining (2 removed)                   │    │
│  └───────────────────────────────────────────────────────────┘    │
│                                                                  │
│  ═══════════ AI Phase Complete: 97.7s ═══════════                │
│                                                                  │
│  ┌─── Recipe Creation (1.5s) ──────────────────────────────┐    │
│  │  create_recipes_for_meal_plan():                          │    │
│  │  • 20 NEW recipes created in PostgreSQL                   │    │
│  │  • 22 matched to EXISTING recipes                         │    │
│  │  • 12 deduplicated within plan                            │    │
│  │  • Result: 54/54 items have real recipe UUIDs             │    │
│  └───────────────────────────────────────────────────────────┘    │
│                                                                  │
│  ┌─── Save Meal Plan (0.4s) ───────────────────────────────┐    │
│  │  meal_plan_repo.create_and_deactivate_old():              │    │
│  │  • INSERT new meal plan                                   │    │
│  │  • Deactivated 1 old plan                                 │    │
│  │  • Plan ID: d9896f6d-7006-4a25-bebc-008150af2fa0          │    │
│  └───────────────────────────────────────────────────────────┘    │
│                                                                  │
│  ┌─── Background Catalog (async, ~0.3s) ───────────────────┐    │
│  │  catalog_recipes_fn():                                    │    │
│  │  • 42 unique recipes cataloged                            │    │
│  │  • 22 updated, 20 new                                     │    │
│  │  • Fire-and-forget (doesn't block response)               │    │
│  └───────────────────────────────────────────────────────────┘    │
│                                                                  │
│  ┌─── Tracking Log ───────────────────────────────────────┐     │
│  │  emit_structured_log() → JSON file written                │     │
│  │  MEAL_PLAN-20260306T080828Z-0551.json                     │     │
│  └───────────────────────────────────────────────────────────┘    │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### 5.2 Time Distribution Summary

| Phase | Time | % of Total | Notes |
|-------|------|-----------|-------|
| Load preferences + rules | 0.9s | 0.9% | 5 DB queries across 2 sessions |
| Load festivals | 0.2s | 0.2% | 1 DB query |
| Load config | 0.3s | 0.3% | YAML → PostgreSQL config read |
| Build prompt | <0.001s | ~0% | String formatting |
| **Gemini AI (attempt 1 — failed)** | **~77s** | **77.4%** | Failed: "Day 0 missing 'snacks'" |
| Backoff sleep | 1s | 1.0% | Exponential: `2^0 = 1s` |
| **Gemini AI (attempt 2 — success)** | **~19s** | **19.1%** | Valid JSON returned |
| Parse + enforce | <0.001s | ~0% | JSON parsing + substring checks |
| Recipe creation | 1.5s | 1.5% | 20 new + 22 existing in PostgreSQL |
| DB save | 0.4s | 0.4% | INSERT + deactivate old plan |
| **TOTAL** | **99.5s** | **100%** | |

**AI accounts for 97.5s of 99.5s = 98.0%**

---

## 6. API Call Chain Analysis

### 6.1 External API Calls Per Generation

There is exactly **ONE external API call** per generation attempt — the Gemini call. With retries, a single generation can make 1-3 Gemini calls.

```
Endpoint: POST /api/v1/meal-plans/generate
│
├── INTERNAL DB CALLS (fast, ~1s total):
│   ├── user_repo.get_preferences()              → PostgreSQL
│   ├── SELECT RecipeRule WHERE action='INCLUDE'  → PostgreSQL
│   ├── SELECT RecipeRule WHERE action='EXCLUDE'  → PostgreSQL
│   ├── SELECT NutritionGoal                      → PostgreSQL
│   ├── user_repo.get_family_members()            → PostgreSQL
│   ├── get_festivals_for_date_range()            → PostgreSQL
│   └── config_service.get_config()               → PostgreSQL
│
├── EXTERNAL API CALL (slow, 40-100s):
│   └── generate_text_with_metadata(prompt)       → Gemini 2.5 Flash API
│       └── May retry 1-2 more times (same prompt)
│
└── INTERNAL DB WRITES (fast, ~2s total):
    ├── create_recipes_for_meal_plan()            → PostgreSQL (INSERT/SELECT)
    ├── meal_plan_repo.create_and_deactivate_old()→ PostgreSQL (INSERT/UPDATE)
    └── catalog_recipes_fn() [background]         → PostgreSQL (INSERT/UPDATE)
```

### 6.2 No Separate API Calls Per Family Member

Family members are **NOT** sent as separate API calls. They are:
1. Loaded from PostgreSQL in a single query (`user_repo.get_family_members(user_id)`)
2. Concatenated into the Gemini prompt as text
3. Sent to Gemini as part of ONE API call

The prompt includes family members like this:
```
## FAMILY MEMBERS (Adapt meals to accommodate ALL members)
- Ramesh (adult): Health: DIABETIC, LOW_OIL
- Sunita (adult): Health: LOW_SALT
- Aarav (child): Health: NO_SPICY
```

---

## 7. Database Query Analysis

### 7.1 Queries During Preference Loading

The `_load_user_preferences()` method makes 5 DB queries across 2 sessions:

**Session 1** (opened at `ai_meal_service.py:297`):
```python
async with async_session_maker() as db:
    # Query 1: INCLUDE rules
    include_result = await db.execute(
        select(RecipeRule).where(
            RecipeRule.user_id == user_id,
            RecipeRule.action == "INCLUDE",
            RecipeRule.is_active == True,
        )
    )

    # Query 2: EXCLUDE rules
    exclude_result = await db.execute(
        select(RecipeRule).where(
            RecipeRule.user_id == user_id,
            RecipeRule.action == "EXCLUDE",
            RecipeRule.is_active == True,
        )
    )

    # Query 3: Nutrition goals
    goals_result = await db.execute(
        select(NutritionGoal).where(
            NutritionGoal.user_id == user_id,
            NutritionGoal.is_active == True,
        )
    )
```

**Session 2** (opened internally by `user_repo.get_family_members()`):
```python
# Query 4 (inside get_preferences): user preferences
# Query 5 (separate call): family members
members = await self.user_repo.get_family_members(user_id)
```

**Optimization opportunity:** These could be consolidated into 1 session, but savings would be ~50ms — negligible compared to the 80-100s Gemini call.

### 7.2 Queries During Post-AI Processing

| Operation | Queries | Time |
|-----------|---------|------|
| Recipe creation | Multiple INSERT/SELECT | 0.9-1.5s |
| Meal plan save + deactivate old | 2 (INSERT + UPDATE) | 0.3-0.4s |
| Recipe cataloging (background) | Multiple INSERT/UPDATE | ~0.3s |

---

## 8. Gemini AI Analysis

### 8.1 Model Configuration

From `backend/app/ai/gemini_client.py`:

| Setting | Value |
|---------|-------|
| Model | `gemini-2.5-flash` |
| SDK | `google-genai` (native async via `client.aio`) |
| Response format | `application/json` |
| Max output tokens | 65,536 |
| Temperature | 0.8 |

### 8.2 Prompt Size

- **6,297 characters** for the Sharma family profile
- Contains: user preferences, family members, dietary rules, cooking time limits, dates, festival context, pairing guidance, nutrition goals, JSON schema example with 2 full day examples
- The JSON schema example alone is ~2,500 characters (includes ingredients + nutrition per item)

### 8.3 Token Usage

| Generation | Input Tokens | Output Tokens | Total | Cost (est.) |
|-----------|-------------|--------------|-------|-------------|
| #1 (99.5s) | ~4,000 | ~21,000 | 25,322 | ~$0.002 |
| #2 (81.4s) | ~4,000 | ~17,000 | 21,426 | ~$0.002 |

### 8.4 First-Attempt Failure Rate

**3 of 5 real generation attempts (60%)** failed on the first try with:
```
Day 0 missing 'snacks' field
```

This means Gemini returned JSON with 7 days but omitted the `snacks` array from at least one day. The validation at `ai_meal_service.py:853-856` catches this:

```python
for i, day in enumerate(days):
    for slot in ["breakfast", "lunch", "dinner", "snacks"]:
        if slot not in day:
            raise ValueError(f"Day {i} missing '{slot}' field")
```

**Impact:** Each failed attempt wastes 40-80 seconds of Gemini API time, then the retry takes another 40-80 seconds. A generation that should take ~45s ends up taking ~100s.

### 8.5 Retry Logic

From `ai_meal_service.py:774-831`:

```
Attempt 1 → Gemini call → validate structure
  ↓ (if fails)
  sleep(2^0 = 1s)
Attempt 2 → Gemini call → validate structure
  ↓ (if fails)
  sleep(2^1 = 2s)
Attempt 3 → Gemini call → validate structure
  ↓ (if fails)
  raise ServiceUnavailableError
```

### 8.6 Concurrency Behavior

When 3 concurrent Gemini calls were in flight:
- 1 succeeded after retry (99.5s)
- 2 timed out at 120s

This suggests Gemini may throttle concurrent requests from the same API key, or the model simply takes longer when multiple requests compete.

---

## 9. Rate Limiting Analysis

### 9.1 Configuration

From `meal_plans.py:116`:
```python
@router.post("/generate", response_model=MealPlanResponse)
@limiter.limit("5/hour")
async def generate(request: Request, ...):
```

### 9.2 All Rate Limits in the Backend

| Endpoint | Limit | Rationale |
|----------|-------|-----------|
| `POST /api/v1/auth/firebase` | 10/min | Brute-force protection |
| `POST /api/v1/auth/refresh` | 20/min | Token rotation |
| `POST /api/v1/chat/message` | 30/min | Claude API cost |
| `POST /api/v1/chat/clear-history` | 10/hour | Destructive action |
| **`POST /api/v1/meal-plans/generate`** | **5/hour** | **Expensive Gemini call** |
| `POST /api/v1/photos/analyze` | 10/hour | Gemini Vision cost |

### 9.3 Why 5/Hour for Meal Generation

This was a deliberate **pre-production hardening** decision (Session 43). Reasoning:

1. **Cost protection:** Each call uses ~21,000-25,000 tokens (~$0.002). At 5/hour max: $0.01/user/hour, $0.24/user/day.
2. **Usage pattern:** Normal users generate 1-2 meal plans per week. 5/hour is generous for real use.
3. **Abuse prevention:** Without limits, a bugged or malicious client could trigger hundreds of calls.

### 9.4 Impact on Load Testing

The rate limiter uses `slowapi` which keys by **client IP address** (`127.0.0.1` for all local Locust users). Combined with the fact that all 3 test users authenticate as the **same backend user**, the entire 5/hour budget was shared.

Timeline of rate limit exhaustion:
```
Request 1: generation #1 starts      → allowed (1/5 used)
Request 2: generation #2 starts      → allowed (2/5 used)
Request 3: generation #3 starts      → allowed (3/5 used)
Request 4: generation #4 starts      → allowed (4/5 used)
Request 5: generation #5 starts      → allowed (5/5 used)
Request 6+: ALL remaining            → 429 Too Many Requests
```

This happened at **13:39:01** — only 2 minutes and 19 seconds into the 5-minute test.

---

## 10. Family Member Handling

### 10.1 Data Flow

```
PostgreSQL                    ai_meal_service.py              Gemini Prompt
──────────                    ──────────────────              ─────────────
family_members table  ──1 query──►  list[dict]  ──string format──►  Text in prompt
                                                                    │
                                                                    ▼
                                                              "## FAMILY MEMBERS
                                                               - Ramesh (adult):
                                                                 Health: DIABETIC, LOW_OIL
                                                               - Sunita (adult):
                                                                 Health: LOW_SALT
                                                               - Aarav (child):
                                                                 Health: NO_SPICY"
```

### 10.2 How Family Members Affect Generation

1. **In the prompt (pre-generation):** Family members and their constraints are described to Gemini. The prompt includes guidance like "Diabetic members: avoid high sugar/GI foods."

2. **In post-processing (post-generation):** `family_constraints.py` builds per-member forbidden keyword sets and checks all recipe names:

```python
# family_constraints.py
FAMILY_CONSTRAINT_MAP = {
    "diabetic": {"sugar", "jaggery", "gulab jamun", "jalebi", "halwa",
                 "ladoo", "barfi", "kheer", "sweet", "mithai", ...},
    "low_salt": {"pickle", "papad", "achaar"},
    "no_spicy": {"green chili", "red chili", "chilli", "mirchi", ...},
    "low_oil": {"pakora", "pakoda", "bhajiya", "puri", "kachori", "deep fried"},
    ...
}
```

3. **Mixed-diet detection:** If family members have different dietary types (e.g., one is vegetarian, another non-vegetarian), the prompt asks Gemini to include alternatives.

### 10.3 No Per-Member API Calls

To be explicit: there is **zero per-member API overhead**. Family members contribute:
- 1 DB query (loading)
- ~100 characters per member in the prompt
- A few microseconds of constraint checking in post-processing

---

## 11. INCLUDE/EXCLUDE Rules Analysis

### 11.1 Why "0 INCLUDE, 0 EXCLUDE"

The backend log showed:
```
Loaded 0 INCLUDE rules, 0 EXCLUDE rules, and 1 nutrition goals from database
```

**Root cause:** The Locust test users authenticate with `fake-firebase-token` → same backend user. This user was auto-created by the debug auth flow with **default empty preferences**. Nobody ever called the recipe rules API to add rules like "Chai DAILY" or "Exclude Mushroom NEVER."

The test profiles in `test_profiles.json` contain preferences (allergies, dislikes, dietary tags) and family members, but:
- **Allergies and dislikes** are loaded from the `users` table (via `get_preferences()`) — these ARE populated
- **INCLUDE/EXCLUDE rules** are stored in the `recipe_rules` table — this table has NO rows for this user
- The `test_profiles.json` data is never synced to the database — it's just local metadata for Locust

### 11.2 What Rules Would Exist for Real Sharma Family

If the Sharma family had gone through full onboarding with rules:

| Rule Type | Target | Frequency | Meal Slot |
|-----------|--------|-----------|-----------|
| INCLUDE | Chai | DAILY | breakfast, snacks |
| INCLUDE | Dal | 4x/week | lunch, dinner |
| EXCLUDE | Mushroom | NEVER | all |
| EXCLUDE | Onion | SPECIFIC_DAYS (Tuesday) | all |

### 11.3 Impact of Missing Rules

Without INCLUDE/EXCLUDE rules, the Gemini prompt has empty rule sections:
```
### INCLUDE Rules (MUST APPEAR):
- None specified

### EXCLUDE Rules (NEVER INCLUDE):
- None specified
```

This means the test is **not fully representative** of production generation. With rules, the prompt would be longer and Gemini would have more constraints to satisfy, potentially increasing both latency and failure rate.

---

## 12. False Positive Filtering Bug

### 12.1 The Problem

The post-processing in `_enforce_rules()` uses **substring matching** to check if recipe names contain forbidden keywords. For the DIABETIC constraint, one of the forbidden keywords is `"sweet"`.

Items removed:
```
Generation #1:
  - "Herbal Tea (unsweetened)" from 2026-03-03 snacks
  - "Plain Curd Lassi (unsweetened)" from 2026-03-07 snacks

Generation #2:
  - "Unsweetened Almond Milk" from 2026-03-18 snacks
  - "Plain Lassi (Unsweetened)" from 2026-03-20 snacks
```

### 12.2 Why It Happens

From `family_constraints.py:35-38`:
```python
"diabetic": {
    "sugar", "jaggery", "gulab jamun", "jalebi", "halwa",
    "ladoo", "barfi", "kheer", "sweet",  # ← "sweet" is a keyword
    "mithai", "rasgulla", "rasmalai", "kulfi", ...
}
```

The check in `_enforce_rules()` does:
```python
recipe_lower = item.recipe_name.lower()
for keyword in forbidden_keywords:
    if keyword in recipe_lower:  # ← substring match!
        # Remove this item
```

`"unsweetened"` contains the substring `"sweet"` → item removed.

### 12.3 Scope of Impact

Every generation for a family with a DIABETIC member will incorrectly remove any recipe containing "sweet" as a substring — including `unsweetened`, `sweetpotato` (if written without space), `sweetcorn`, etc.

The AI is actually being smart by suggesting unsweetened alternatives for a diabetic family member, but the post-processing removes them.

---

## 13. Latency Distribution

### 13.1 Percentile Table (from Locust CSV)

| p50 | p66 | p75 | p80 | p90 | p95 | p98 | p99 | p100 |
|-----|-----|-----|-----|-----|-----|-----|-----|------|
| 2.3s | 2.3s | 2.4s | 2.4s | 120s | 120s | 122s | 122s | 122.4s |

### 13.2 Bimodal Distribution

The latency is **bimodal** — two distinct clusters, not a normal distribution:

```
  Count
  │
22│ ████████████████████████████████████████████  ← 429 rejections (~2.3s)
  │
  │
 2│ ████                                          ← Successful generations (81-100s)
  │
 3│ ██████                                        ← Timeouts (~120s)
  │
  └──────────────────────────────────────────────── Latency (seconds)
       2s              80s        100s       120s
```

- **Cluster 1 (2.3s):** Rate-limited responses — the server immediately returns 429 without doing any work
- **Cluster 2 (81-100s):** Successful generations — real Gemini + DB work
- **Cluster 3 (120s):** Timeouts — Gemini didn't respond in time

The p50 of 2.3s is **misleading** — it represents the time to be rejected, not the time to generate a plan.

### 13.3 True Generation Latency (Successful Only)

| Metric | Value |
|--------|-------|
| Min | 81.4s |
| Max | 99.5s |
| Average | 90.5s |
| AI portion | 80.2-97.7s (98%) |
| Post-AI portion | 1.2-1.9s (2%) |

---

## 14. Performance Targets vs Actuals

### 14.1 Targets (from `tests/performance/README.md`)

| Endpoint | p50 Target | p95 Target | p99 Target |
|----------|-----------|-----------|-----------|
| Health check | <50ms | <100ms | <200ms |
| CRUD reads | <200ms | <500ms | <1s |
| Recipe search | <300ms | <800ms | <2s |
| **Meal generation** | **<30s** | **<60s** | **<120s** |

### 14.2 Actuals (Successful Generations Only)

| Metric | Target | Actual | Status | Gap |
|--------|--------|--------|--------|-----|
| p50 | <30s | ~90s | **MISS** | 3x over |
| p95 | <60s | ~120s | **MISS** | 2x over |
| p99 | <120s | 122s | **BORDERLINE** | Just over |
| Success rate | >95% | 40% (2/5 real attempts) | **MISS** | Rate limit excluded |

### 14.3 Context

The p50 target of <30s was likely set when the project used **database recipe lookup** (the old algorithm). The migration to Gemini AI fundamentally changed the latency profile:

| Phase | Old (DB Lookup) | New (Gemini AI) |
|-------|----------------|-----------------|
| Recipe search | <1s (SQL query) | N/A (AI generates) |
| AI generation | N/A | 40-100s |
| Total | ~5-15s | 80-100s |

The targets need to be updated to reflect the AI-powered architecture.

---

## 15. Issues & Recommendations

### Issue 1: Rate Limit Blocks Load Testing (CRITICAL for testing)

| Aspect | Detail |
|--------|--------|
| **Problem** | `5/hour` rate limit on `/api/v1/meal-plans/generate` blocks 81.5% of test requests |
| **Root Cause** | All Locust users share same IP (`127.0.0.1`) and same backend user (single `fake-firebase-token`) |
| **Impact** | Only 5 real generation attempts possible per hour of testing |
| **Severity** | High (test infrastructure) |
| **Recommendation** | Option A: Add `RATE_LIMIT_DISABLED=true` env var for load test environments. Option B: Increase limit when `DEBUG=true`. Option C: Create unique backend users per Locust profile with different auth tokens. |

### Issue 2: Gemini 60% First-Attempt Failure Rate (MEDIUM)

| Aspect | Detail |
|--------|--------|
| **Problem** | 3 of 5 first attempts returned JSON missing the `snacks` field |
| **Root Cause** | Gemini sometimes omits one of the 4 required meal slots despite the prompt and JSON example |
| **Impact** | Each failure wastes 40-80s of API time and doubles generation latency |
| **Severity** | Medium (reliability + cost) |
| **Recommendation** | Option A: Add Gemini's `response_schema` parameter with strict JSON schema. Option B: Make `snacks` more prominent in the prompt (e.g., "CRITICAL: Every day MUST have exactly 4 slots: breakfast, lunch, dinner, snacks"). Option C: Reduce output complexity (fewer fields per item) to improve compliance. |

### Issue 3: False Positive "sweet" Filter (LOW)

| Aspect | Detail |
|--------|--------|
| **Problem** | Items containing "unsweetened" are removed because "sweet" is a substring |
| **Root Cause** | `family_constraints.py` uses `if keyword in recipe_lower` (substring match) |
| **Impact** | 2 healthy items removed per generation for DIABETIC families |
| **Severity** | Low (functionality bug) |
| **File** | `backend/app/services/family_constraints.py:35` and `ai_meal_service.py` enforce loop |
| **Recommendation** | Use word-boundary matching: `re.search(r'\b' + re.escape(keyword) + r'\b', recipe_lower)` instead of `keyword in recipe_lower`. Or add "unsweetened" to an allow-list. |

### Issue 4: Performance Targets Unrealistic for AI (INFO)

| Aspect | Detail |
|--------|--------|
| **Problem** | p50 target is <30s but actual AI generation takes 80-100s |
| **Root Cause** | Targets were set for database-lookup generation, not AI generation |
| **Impact** | Targets are permanently unmet, making reports misleading |
| **Severity** | Info (documentation) |
| **Recommendation** | Update `tests/performance/README.md` targets to: p50 <90s, p95 <120s, p99 <150s. Or set separate targets for "AI time" vs "post-AI time." |

### Issue 5: Test Profiles Don't Populate Database Rules (LOW)

| Aspect | Detail |
|--------|--------|
| **Problem** | Locust test shows "0 INCLUDE, 0 EXCLUDE rules" — profiles aren't synced to DB |
| **Root Cause** | `test_profiles.json` contains preference data but no setup step writes it to `recipe_rules` table |
| **Impact** | Tests don't exercise the full rule enforcement pipeline |
| **Severity** | Low (test coverage gap) |
| **Recommendation** | Add a `on_start()` setup in `MealGenHeavyUser` that POSTs recipe rules to `/api/v1/recipe-rules` after authentication. Or add a seed script for test users. |

### Issue 6: All Locust Users Share Same Backend User (MEDIUM)

| Aspect | Detail |
|--------|--------|
| **Problem** | All profiles use `"firebase_token": "fake-firebase-token"` → same user ID |
| **Root Cause** | Backend maps fake token to a single user in debug mode |
| **Impact** | Rate limit shared, no multi-user concurrency testing, plans overwrite each other |
| **Severity** | Medium (test accuracy) |
| **Recommendation** | Generate unique fake tokens per profile (e.g., `fake-firebase-token-sharma`, `fake-firebase-token-gupta`) and update the backend's debug auth to create distinct users per token. |

---

## 16. Source Code References

| File | Lines | Purpose |
|------|-------|---------|
| `backend/app/services/ai_meal_service.py` | 184-277 | `generate_meal_plan()` — main orchestrator |
| `backend/app/services/ai_meal_service.py` | 279-433 | `_load_user_preferences()` — 5 DB queries |
| `backend/app/services/ai_meal_service.py` | 459-772 | `_build_prompt()` — 6,297 char prompt construction |
| `backend/app/services/ai_meal_service.py` | 774-831 | `_generate_with_retry()` — Gemini call + retry |
| `backend/app/services/ai_meal_service.py` | 833-861 | `_validate_response_structure()` — JSON validation |
| `backend/app/api/v1/endpoints/meal_plans.py` | 115-348 | `generate()` endpoint — timeout, recipe creation, save |
| `backend/app/services/family_constraints.py` | 15-50 | `FAMILY_CONSTRAINT_MAP` — keyword sets |
| `backend/app/services/family_constraints.py` | 53-83 | `get_family_forbidden_keywords()` — per-member sets |
| `backend/app/ai/gemini_client.py` | - | Gemini SDK configuration |
| `backend/app/core/rate_limit.py` | - | `slowapi` limiter setup |
| `backend/tests/performance/locustfile.py` | 336-441 | `MealGenHeavyUser` class |
| `backend/tests/performance/test_profiles.json` | 1-116 | 5 test family profiles |
| `backend/tests/performance/profiles/meal_gen_focused.conf` | 1-14 | Locust config |

---

## 17. Raw Data

### 17.1 Locust Stats CSV

```
Type,Name,Request Count,Failure Count,Median,Average,Min,Max,Avg Content Size,Req/s,Failures/s,p50,p66,p75,p80,p90,p95,p98,p99,p99.9,p99.99,p100
POST,/api/v1/auth/firebase [MealGen],3,0,3100,3337,2944,3971,927,0.010,0.0,3100,3100,4000,4000,4000,4000,4000,4000,4000,4000,4000
POST,/api/v1/meal-plans/generate [FOCUSED],27,25,2300,22062,2137,122436,1092,0.091,0.085,2300,2300,2400,2400,120000,120000,122000,122000,122000,122000,122000
Aggregated,30,25,2300,20190,2137,122436,1075,0.102,0.085,2300,2300,2900,4000,120000,120000,122000,122000,122000,122000,122000
```

### 17.2 Failure CSV

```
Method,Name,Error,Occurrences
POST,/api/v1/meal-plans/generate [FOCUSED],CatchResponseError('HTTP 429'),22
POST,/api/v1/meal-plans/generate [FOCUSED],CatchResponseError('Timeout 504'),3
```

### 17.3 Backend PERF Logs

```
PERF meal-gen user=5f8d9d89-55b3-4929-bdd8-c98abc423d3e: total=99.5s, ai=97.7s, recipes=1.5s, save=0.4s, post-ai=1.9s
PERF meal-gen user=5f8d9d89-55b3-4929-bdd8-c98abc423d3e: total=81.4s, ai=80.2s, recipes=0.9s, save=0.3s, post-ai=1.2s
```

### 17.4 Generation Tracker Files

```
MEAL_PLAN-20260306T080828Z-0551.json  (success, 99.5s, 25322 tokens)
MEAL_PLAN-20260306T080845Z-5113.json  (timeout, 120s, 20474 tokens)
MEAL_PLAN-20260306T080846Z-7595.json  (timeout, 120s, 25329 tokens)
MEAL_PLAN-20260306T081004Z-1620.json  (success, 81.4s, 21426 tokens)
MEAL_PLAN-20260306T081100Z-1339.json  (timeout, 120s, 24889 tokens)
```

### 17.5 Reports Location

| File | Description |
|------|-------------|
| `backend/tests/performance/reports/meal_gen_report.html` | Locust HTML report with charts |
| `backend/tests/performance/reports/meal_gen_stats.csv` | Aggregated stats |
| `backend/tests/performance/reports/meal_gen_failures.csv` | Failure details |
| `backend/tests/performance/reports/meal_gen_exceptions.csv` | Exception traces |
| `backend/tests/performance/reports/meal_gen_stats_history.csv` | Time-series data |

---

*Generated: March 6, 2026*
*Test duration: 5 minutes (300 seconds)*
*Locust version: 2.43.3*
*Backend: FastAPI + PostgreSQL + Gemini 2.5 Flash*
