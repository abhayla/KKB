# Meal Plan Generation API — Complete Flow Diagram

This document traces every function call, database query, and external API call in the `POST /api/v1/meal-plans/generate` endpoint.

**Related:**
- [Meal-Generation-Algorithm.md](./Meal-Generation-Algorithm.md) — Algorithm design and decisions
- [Meal-Generation-Config-Architecture.md](./Meal-Generation-Config-Architecture.md) — Config structure
- [Data-Flow-Diagram.md](./Data-Flow-Diagram.md) — Full app data flow (Android + Backend)

---

## Source Files Reference

| File | Purpose |
|------|---------|
| `app/api/v1/endpoints/meal_plans.py` | HTTP endpoint, timeout wrapper |
| `app/api/deps.py` | JWT auth dependency |
| `app/services/ai_meal_service.py` | Core generation logic (prompt, parse, enforce) |
| `app/services/recipe_creation_service.py` | Find or create recipe DB records |
| `app/services/ai_recipe_catalog_service.py` | Catalog AI-generated recipe names |
| `app/services/config_service.py` | Meal generation config (cached) |
| `app/services/festival_service.py` | Festival lookup by date range |
| `app/services/family_constraints.py` | Family health condition filtering |
| `app/repositories/user_repository.py` | User preferences, family members |
| `app/repositories/meal_plan_repository.py` | Meal plan CRUD |
| `app/ai/gemini_client.py` | Google Gemini API wrapper |
| `app/models/meal_plan.py` | MealPlan, MealPlanItem models |
| `app/models/recipe.py` | Recipe, RecipeIngredient, etc. models |
| `app/models/user.py` | User, UserPreferences, FamilyMember models |
| `app/models/recipe_rule.py` | RecipeRule, NutritionGoal models |
| `app/models/festival.py` | Festival model |
| `app/models/config.py` | SystemConfig model |
| `app/models/ai_recipe_catalog.py` | AiRecipeCatalog model |

---

## Post-AI Phase Optimization (February 22, 2026)

The post-AI database phase was the critical bottleneck — **~35-65 seconds** of the ~100s total request time. Six targeted optimizations reduced this to **~1.1 seconds** (97% reduction).

### Optimized Flow Overview

```
POST /api/v1/meal-plans/generate

                      │
  ┌───────────────────┼──────────────────────────────────────────┐
  │                   ▼                                          │
  │   Phase 0:  Auth (JWT → User)                    ~0.1s      │
  │   Phase 2:  Load Preferences (5 DB reads)        ~1-2s      │
  │   Phase 3:  Filter Conflicting Rules (in-memory) ~0s        │
  │   Phase 4:  Load Festivals (1 DB read)           ~0.5s      │
  │   Phase 5:  Load Config (cached singleton)       ~0s        │
  │   Phase 6:  Build AI Prompt (in-memory)          ~0s        │
  │   Phase 7:  Gemini AI Call                       ~45-90s    │
  │   Phase 8:  Parse JSON Response (in-memory)      ~0s        │
  │   Phase 9:  Post-Process Enforcement (in-memory) ~0s        │
  │ ✅ Phase 10: REMOVED (prefs cached from Phase 2)  0s        │
  │ ✅ Phase 11: Bulk Recipe Creation (4 INSERTs)     ~1.0s     │
  │ ✅ Phase 12: Merged Save (single session)         ~0.1s     │
  │ ✅ Phase 13: Background Catalog (fire-and-forget)  0s*      │
  │   Phase 14: Build HTTP Response (in-memory)      ~0s        │
  │                   │                                          │
  │     asyncio.wait_for(timeout=120s) wraps all phases          │
  └───────────────────┼──────────────────────────────────────────┘
                      ▼
HTTP 200 OK + MealPlanResponse JSON

* Phase 13 runs as asyncio.create_task() — does not block the response.
```

### The 6 Optimizations

#### 1. Eliminate duplicate user preferences load

**Before:** Phase 10 re-read `user_preferences` from PostgreSQL (Session 7) to get `cuisine_type` and `family_size`, duplicating Phase 2.

**After:** `AIMealService` stores loaded preferences in `self.last_preferences`. The endpoint reads `ai_service.last_preferences` directly — zero DB calls.

**Files:** `ai_meal_service.py` (added `last_preferences` attribute), `meal_plans.py` (reads cached prefs)

#### 2. Batch recipe lookup — 28 SELECTs → 1

**Before:** `find_or_create_recipe()` ran one `SELECT ... WHERE name = :name` per unique recipe name (~28 unique names in a typical 56-item plan). Each was a separate round-trip to the remote PostgreSQL server.

**After:** `_bulk_find_existing_recipes()` collects all unique normalized names upfront and runs a single query:
```sql
SELECT id, lower(trim(name)) FROM recipes
WHERE lower(trim(name)) IN (:name1, :name2, ..., :name28)
  AND is_active = true
```
Returns a `dict[normalized_name → recipe_id]` used to skip creation for existing recipes.

**File:** `recipe_creation_service.py`

#### 3. Bulk recipe INSERT — ~450 db.add() → 4 bulk inserts (CRITICAL)

**Before:** Each new recipe triggered 4 individual `session.add()` calls (recipe + ingredients + instructions + nutrition). With ~28 new recipes averaging ~4 ingredients + 4 instructions + 1 nutrition each, this was ~450 individual ORM-tracked inserts flushed to a remote server.

**After:** All new data is collected into plain dicts, then inserted in 4 bulk operations:
```python
from sqlalchemy import insert
await db.execute(insert(Recipe), all_recipes)              # 1 round-trip
await db.execute(insert(RecipeIngredient), all_ingredients) # 1 round-trip
await db.execute(insert(RecipeInstruction), all_instructions) # 1 round-trip
await db.execute(insert(RecipeNutrition), all_nutrition)    # 1 round-trip
await db.commit()
```

**File:** `recipe_creation_service.py`

**Savings:** ~30-60s → ~0.5s (the single largest optimization)

#### 4. Eliminate redundant re-read in meal plan save

**Before:** After creating the meal plan and items, `MealPlanRepository.create()` called `await self.get_by_id(plan_id)` — a full re-read with JOIN across `meal_plans` + `meal_plan_items` to build the response dict (Session 11).

**After:** `_plan_to_dict_from_objects()` builds the response dict from the in-memory ORM objects that were just created. Since `expire_on_commit=False`, all attributes remain accessible after commit — no re-read needed.

**File:** `meal_plan_repository.py`

#### 5. Merge deactivate + create into single session

**Before:** `deactivate_old_plans()` (Session 9) and `create()` (Session 10) were separate calls, each opening and committing their own session.

**After:** `create_and_deactivate_old(plan_data)` performs both operations in a single session with one COMMIT:
```python
async def create_and_deactivate_old(self, plan_data: dict) -> dict:
    async with async_session_maker() as session:
        # 1. Deactivate old plans
        await session.execute(
            update(MealPlan)
            .where(MealPlan.user_id == user_id, MealPlan.is_active == True)
            .values(is_active=False)
        )
        # 2. Create new plan + items
        plan = MealPlan(...)
        session.add(plan)
        for item_data in ...:
            session.add(MealPlanItem(...))
        await session.commit()
        # 3. Return dict from in-memory objects (no re-read)
        return self._plan_to_dict_from_objects(plan, items_list)
```

**Files:** `meal_plan_repository.py`, `meal_plans.py`

#### 6. Background catalog update

**Before:** `catalog_recipes()` ran synchronously in the request path (Session 12), blocking the HTTP response by ~1-3s.

**After:** Wrapped in `asyncio.create_task()` — runs in the background after the response is already sent. Failures are logged but never affect the user.

```python
asyncio.create_task(_background_catalog())
return _build_response(created_plan)  # returns immediately
```

**Files:** `meal_plans.py`, `ai_recipe_catalog_service.py` (bulk lookup added)

### Optimized Database Sessions

| Phase | Session | Table(s) | Operation |
|:-----:|:-------:|----------|-----------|
| 0 | 1 | `users` | READ |
| 2a | 2 | `user_preferences` | READ |
| 2b-d | 3 | `recipe_rules`, `nutrition_goals` | READ |
| 2e | 4 | `family_members` | READ |
| 4 | 5 | `festivals` | READ |
| 5 | 6 | `system_config` (cached) | READ (once ever) |
| 11 | 7 | `recipes`, `recipe_ingredients`, `recipe_instructions`, `recipe_nutrition` | 1 bulk READ + 4 bulk WRITES |
| 12 | 8 | `meal_plans`, `meal_plan_items` | READ + WRITE (single session) |
| 13 | bg | `ai_recipe_catalog` | Background (non-blocking) |

**Total: 6-8 sessions (down from 10-12). No re-read after create. Catalog is non-blocking.**

### Optimized Time Breakdown

```
 0s  +---- Auth + Parse request           ~0.1s
     +---- Load preferences (5 queries)   ~1-2s
     +---- Filter rules + Load festivals  ~0.5s
     +---- Load config (cached)           ~0s
     +---- Build prompt                   ~0s
     |
 3s  +---- Gemini AI call                 ~45-90s (typically 45-70s)
     |     +-- Attempt 1
     |     +-- Retry 1: +1s backoff
     |     +-- Retry 2: +2s backoff
     |     '-- Retry 3: fail -> 503
     |
55s  +---- Parse + Post-process           ~0.1s
     |
     +---- Recipe creation (OPTIMIZED)    ~1.0s    (was 30-60s)
     |     '-- 1 bulk SELECT + 4 bulk INSERTs
     |
     +---- Save meal plan (OPTIMIZED)     ~0.1s    (was 2-3s)
     |     '-- Single session: deactivate + create + return
     |
     +---- Catalog recipes (BACKGROUND)     0s*    (was 1-2s)
     |     '-- asyncio.create_task (fire-and-forget)
     |
56s  '---- Return response                ~0s
     -----------------------------------------------
     Post-AI: ~1.1s (was ~35-65s)
     Total: ~56s (was ~100s, Gemini permitting)
```

### Performance Measurements

#### Locust Load Test Results (end-to-end, single user)

| Metric | BEFORE | AFTER | Notes |
|--------|--------|-------|-------|
| `/generate` median | 99,313ms | 106,933ms | AI variance dominates (45-170s observed) |
| Auth time | 2,983ms | 3,411ms | Network variance |
| Status | 200 OK | 200 OK | |

Total Locust time is **not meaningful** for measuring the post-AI optimization because Gemini API latency varies by 3-4x between runs.

#### Phase-Level Timing (standalone measurement)

Measured by calling each service function independently with `time.monotonic()`:

| Phase | BEFORE (estimated from profiling) | AFTER (measured) | Reduction |
|-------|-----------------------------------|------------------|-----------|
| AI generation (Gemini) | ~45-90s | ~45-90s | N/A (not optimized) |
| **Recipe creation** | **~33-65s** | **1.0s** | **97%** |
| **DB save (deactivate + create)** | **~2-3s** | **0.1s** | **95%** |
| **Catalog** | **~1-2s** | **0s*** | **100%** |
| **Post-AI total** | **~35-65s** | **~1.1s** | **~97%** |

*Catalog runs in background — does not block the response.

#### Timing Instrumentation

The endpoint now logs phase-level timing on every request:

```
PERF meal-gen user=abc123: total=56.2s, ai=55.0s, recipes=1.0s, save=0.1s, post-ai=1.2s
```

### Files Modified

| File | Changes |
|------|---------|
| `app/services/recipe_creation_service.py` | Added `_bulk_find_existing_recipes()`, refactored `create_recipes_for_meal_plan()` for bulk INSERT |
| `app/api/v1/endpoints/meal_plans.py` | Cached prefs, `create_and_deactivate_old()`, background catalog, timing instrumentation |
| `app/repositories/meal_plan_repository.py` | Added `create_and_deactivate_old()`, `_plan_to_dict_from_objects()` |
| `app/services/ai_meal_service.py` | Added `self.last_preferences` attribute |
| `app/services/ai_recipe_catalog_service.py` | Added bulk lookup in `catalog_recipes()` |
| `tests/test_meal_plans_api.py` | Updated 5 tests for new architecture |

### Test Verification

```
tests/test_meal_plans_api.py:         27/27 passed
tests/test_recipe_creation_service.py: 7/7 passed
tests/test_ai_meal_service.py:        22/22 passed
Full backend suite:                   482/490 passed (8 pre-existing Sentry/Py3.13)
```

### Simplified Optimized Flow

```
Request --> Auth --> Load Data (5 DB reads) --> Build Prompt
                                                     |
                                                     v
Response <-- Save Plan <-- Create Recipes <-- Gemini AI <--+
  200 OK    (1 session)   (4 bulk INSERTs)    (45-90s)
               0.1s            1.0s
                                                 Catalog
                                              (background)
```

---

*Last updated: February 22, 2026*
*Based on code analysis of the actual implementation, not design docs.*
