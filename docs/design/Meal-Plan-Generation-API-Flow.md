# Meal Plan Generation API — Complete Flow Diagram

This document traces every function call, database query, and external API call in the `POST /api/v1/meal-plans/generate` endpoint.

**Related:**
- [Meal-Generation-Algorithm.md](./Meal-Generation-Algorithm.md) — Algorithm design and decisions
- [Meal-Generation-Config-Architecture.md](./Meal-Generation-Config-Architecture.md) — Config structure
- [Data-Flow-Diagram.md](./Data-Flow-Diagram.md) — Full app data flow (Android + Backend)

---

## Request → Response Overview

```
POST /api/v1/meal-plans/generate
Body: { "week_start_date": "2026-02-23" }
Headers: Authorization: Bearer <JWT>

                      │
  ┌───────────────────┼──────────────────────────────────────────┐
  │                   ▼                                          │
  │   Phase 0:  Auth (JWT → User)                    ~0.1s      │
  │   Phase 2:  Load Preferences (5 DB reads)        ~1-2s      │
  │   Phase 3:  Filter Conflicting Rules (in-memory) ~0s        │
  │   Phase 4:  Load Festivals (1 DB read)           ~0.5s      │
  │   Phase 5:  Load Config (cached singleton)       ~0s        │
  │   Phase 6:  Build AI Prompt (in-memory)          ~0s        │
  │   Phase 7:  🌐 Gemini AI Call                     ~5-90s     │
  │   Phase 8:  Parse JSON Response (in-memory)      ~0s        │
  │   Phase 9:  Post-Process Enforcement (in-memory) ~0s        │
  │   Phase 10: Re-load Preferences (1 DB read)      ~0.5s      │
  │   Phase 11: 🐌 Create Recipe Records (DB writes)  ~30-60s    │
  │   Phase 12: Save Meal Plan (DB writes)           ~2-3s      │
  │   Phase 13: Catalog AI Recipes (non-critical)    ~1-2s      │
  │   Phase 14: Build HTTP Response (in-memory)      ~0s        │
  │                   │                                          │
  │     asyncio.wait_for(timeout=120s) wraps all phases          │
  └───────────────────┼──────────────────────────────────────────┘
                      ▼
HTTP 200 OK + MealPlanResponse JSON
```

---

## Detailed Phase Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│  POST /api/v1/meal-plans/generate                                       │
│  Body: { "week_start_date": "2026-02-23" }                              │
│  Headers: Authorization: Bearer <JWT>                                   │
└─────────────────────┬───────────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  PHASE 0: AUTHENTICATION (deps.py)                                      │
│  ┌──────────────────────────────────────────────────┐                  │
│  │  verify_token_and_get_user_id(jwt)                │                  │
│  │  DB READ: users WHERE id = :user_id               │  ←── Session 1  │
│  │  Check: user.is_active == True                    │                  │
│  └──────────────────────────────────────────────────┘                  │
│  Error: 401 if invalid JWT | 404 if user not found                      │
└─────────────────────┬───────────────────────────────────────────────────┘
                      │ User object
                      ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  PHASE 1: TIMEOUT WRAPPER (meal_plans.py:138)                           │
│  ┌──────────────────────────────────────────────────┐                  │
│  │  asyncio.wait_for(generate_meal_plan(), timeout=120s)               │
│  │  Error: 504 Gateway Timeout if exceeded           │                  │
│  └──────────────────────────────────────────────────┘                  │
└─────────────────────┬───────────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  PHASE 2: LOAD USER PREFERENCES (ai_meal_service.py)                    │
│                                                                         │
│  ┌──────────────────────────────────────────────────┐                  │
│  │  2a. UserRepository.get_preferences(user_id)      │  ←── Session 2  │
│  │      DB READ: user_preferences                    │                  │
│  │      Returns: dietary_tags, allergies, dislikes,  │                  │
│  │        cooking_times, busy_days, items_per_meal   │                  │
│  └──────────────────────────────────────────────────┘                  │
│                      │                                                  │
│  ┌──────────────────────────────────────────────────┐                  │
│  │  2b. Load INCLUDE rules                           │  ←── Session 3  │
│  │      DB READ: recipe_rules WHERE action='INCLUDE' │                  │
│  │                AND user_id AND is_active=true      │                  │
│  │                                                   │                  │
│  │  2c. Load EXCLUDE rules                           │                  │
│  │      DB READ: recipe_rules WHERE action='EXCLUDE' │                  │
│  │                AND user_id AND is_active=true      │                  │
│  │                                                   │                  │
│  │  2d. Load nutrition goals                         │                  │
│  │      DB READ: nutrition_goals WHERE user_id       │                  │
│  │                AND is_active=true                  │                  │
│  └──────────────────────────────────────────────────┘                  │
│                      │                                                  │
│  ┌──────────────────────────────────────────────────┐                  │
│  │  2e. UserRepository.get_family_members(user_id)   │  ←── Session 4  │
│  │      DB READ: family_members WHERE user_id        │                  │
│  │      Returns: names, age_groups, health_conditions│                  │
│  └──────────────────────────────────────────────────┘                  │
│                                                                         │
│  Assemble all data into UserPreferences dataclass                       │
└─────────────────────┬───────────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  PHASE 3: FILTER CONFLICTING INCLUDE RULES (family_constraints.py)      │
│  ┌──────────────────────────────────────────────────┐                  │
│  │  No DB calls — in-memory only                     │                  │
│  │  Check INCLUDE rules vs family health conditions  │                  │
│  │  e.g., Jain member -> remove "Aloo Paratha" rule  │                  │
│  │  Output: filtered_include_rules, removed_report   │                  │
│  └──────────────────────────────────────────────────┘                  │
└─────────────────────┬───────────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  PHASE 4: LOAD FESTIVALS (festival_service.py)                          │
│  ┌──────────────────────────────────────────────────┐                  │
│  │  DB READ: festivals WHERE date BETWEEN            │  ←── Session 5  │
│  │           :week_start AND :week_end               │                  │
│  │           AND is_active = true                    │                  │
│  │  Returns: {date: {name, is_fasting_day,           │                  │
│  │            special_foods, avoided_foods}}          │                  │
│  └──────────────────────────────────────────────────┘                  │
│  Returns empty dict on error (non-critical)                             │
└─────────────────────┬───────────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  PHASE 5: LOAD CONFIG (config_service.py — cached singleton)            │
│  ┌──────────────────────────────────────────────────┐                  │
│  │  First call only:                                 │  ←── Session 6  │
│  │  DB READ: system_config WHERE key='meal_generation'│ (if not cached)│
│  │  Returns: pairing_rules, ingredient_aliases,      │                  │
│  │           meal_types, recipe_categories            │                  │
│  │  Subsequent calls: returns from memory cache      │                  │
│  └──────────────────────────────────────────────────┘                  │
└─────────────────────┬───────────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  PHASE 6: BUILD AI PROMPT (ai_meal_service.py — no DB)                  │
│  ┌──────────────────────────────────────────────────┐                  │
│  │  ~3000-5000 char prompt including:                │                  │
│  │  - Diet, cuisines, spice level, family size       │                  │
│  │  - Family members + health conditions             │                  │
│  │  - Allergies (NEVER — safety critical)            │                  │
│  │  - Dislikes (AVOID)                               │                  │
│  │  - INCLUDE rules (DAILY / Nx/week + meal slots)   │                  │
│  │  - EXCLUDE rules (NEVER / specific days)          │                  │
│  │  - Cooking time limits per day                    │                  │
│  │  - Festivals + fasting days                       │                  │
│  │  - Pairing guidance (dal+rice, sabzi+roti)        │                  │
│  │  - Nutrition goals                                │                  │
│  │  - JSON output format spec                        │                  │
│  └──────────────────────────────────────────────────┘                  │
└─────────────────────┬───────────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  PHASE 7: GEMINI AI CALL (gemini_client.py)           ~5-90 seconds    │
│  ┌──────────────────────────────────────────────────┐                  │
│  │  EXTERNAL API: Google Gemini gemini-2.5-flash     │                  │
│  │     temperature=0.8                               │                  │
│  │     max_output_tokens=65536                       │                  │
│  │     response_mime_type="application/json"         │                  │
│  │                                                   │                  │
│  │  Retry: 3 attempts, exponential backoff (1s, 2s)  │                  │
│  │  Validate: 7 days x 4 slots x >=2 items each     │                  │
│  └──────────────────────────────────────────────────┘                  │
│  Error: 503 if all 3 retries fail                                       │
└─────────────────────┬───────────────────────────────────────────────────┘
                      │ JSON: 7 days x 4 slots x ~2 items = ~56 items
                      ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  PHASE 8: PARSE RESPONSE (ai_meal_service.py — no DB)                   │
│  ┌──────────────────────────────────────────────────┐                  │
│  │  JSON -> GeneratedMealPlan dataclass               │                  │
│  │  Each item gets: UUID, recipe_name, prep_time,    │                  │
│  │  dietary_tags, category, calories, ingredients,   │                  │
│  │  nutrition, instructions                          │                  │
│  │  recipe_id = "AI_GENERATED" (placeholder)         │                  │
│  └──────────────────────────────────────────────────┘                  │
└─────────────────────┬───────────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  PHASE 9: POST-PROCESSING ENFORCEMENT (ai_meal_service.py — no DB)      │
│  ┌──────────────────────────────────────────────────┐                  │
│  │  Safety Net — 3 passes over all ~56 items:        │                  │
│  │                                                   │                  │
│  │  Pass 1: ALLERGEN CHECK                           │                  │
│  │  "peanut" -> also checks "groundnut", "moongphali"│                  │
│  │  Match in recipe_name -> REMOVE item              │                  │
│  │                                                   │                  │
│  │  Pass 2: EXCLUDE RULE CHECK                       │                  │
│  │  NEVER targets + day-specific targets             │                  │
│  │  Match in recipe_name -> REMOVE item              │                  │
│  │                                                   │                  │
│  │  Pass 3: FAMILY CONSTRAINT SAFETY NET             │                  │
│  │  Checks recipe_name + ingredients against         │                  │
│  │  family members' health condition keywords        │                  │
│  │  Match -> REMOVE item                             │                  │
│  └──────────────────────────────────────────────────┘                  │
└─────────────────────┬───────────────────────────────────────────────────┘
                      │ Cleaned GeneratedMealPlan
                      ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  PHASE 10: RE-LOAD PREFERENCES (meal_plans.py)                          │
│  ┌──────────────────────────────────────────────────┐                  │
│  │  DB READ: user_preferences (again)                │  ←── Session 7  │
│  │  Extracts: cuisine_type, family_size              │                  │
│  │  (Needed for recipe creation, not cached from P2) │                  │
│  └──────────────────────────────────────────────────┘                  │
└─────────────────────┬───────────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  PHASE 11: CREATE RECIPE RECORDS (recipe_creation_service.py)           │
│  *** BOTTLENECK — ~30-60 seconds for ~56 items ***                      │
│  ┌──────────────────────────────────────────────────┐                  │
│  │  Retry: 2 attempts                                │  ←── Session 8  │
│  │                                                   │                  │
│  │  For each of ~56 items:                           │                  │
│  │  ┌────────────────────────────────────────────┐   │                  │
│  │  │  a. In-memory cache check (same generation) │   │                  │
│  │  │     -> skip if same normalized name seen    │   │                  │
│  │  │                                              │   │                  │
│  │  │  b. DB READ: recipes WHERE name = :normalized│   │                  │
│  │  │     -> reuse existing recipe_id if found     │   │                  │
│  │  │                                              │   │                  │
│  │  │  c. If new recipe — 4 DB WRITES:             │   │                  │
│  │  │     INSERT -> recipes                         │   │                  │
│  │  │     INSERT -> recipe_ingredients (per item)   │   │                  │
│  │  │     INSERT -> recipe_instructions             │   │                  │
│  │  │     INSERT -> recipe_nutrition                │   │                  │
│  │  │                                              │   │                  │
│  │  │  d. Mutate item.recipe_id = real UUID         │   │                  │
│  │  └────────────────────────────────────────────┘   │                  │
│  │                                                   │                  │
│  │  Single COMMIT at end                             │                  │
│  └──────────────────────────────────────────────────┘                  │
│  Tables written: recipes, recipe_ingredients,                           │
│                  recipe_instructions, recipe_nutrition                   │
└─────────────────────┬───────────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  PHASE 12: SAVE MEAL PLAN (meal_plan_repository.py)                     │
│  ┌──────────────────────────────────────────────────┐                  │
│  │  12a. Deactivate old plans                        │  ←── Session 9  │
│  │       DB READ+WRITE: meal_plans                   │                  │
│  │       SET is_active=false WHERE user_id AND active │                  │
│  └──────────────────────────────────────────────────┘                  │
│  ┌──────────────────────────────────────────────────┐                  │
│  │  12b. Create new plan                             │  ←── Session 10 │
│  │       DB WRITE: INSERT meal_plans (1 row)         │                  │
│  │       DB WRITE: INSERT meal_plan_items (~56 rows) │                  │
│  │       COMMIT                                      │                  │
│  └──────────────────────────────────────────────────┘                  │
│  ┌──────────────────────────────────────────────────┐                  │
│  │  12c. Re-read plan for response                   │  ←── Session 11 │
│  │       DB READ: meal_plans + meal_plan_items       │                  │
│  └──────────────────────────────────────────────────┘                  │
└─────────────────────┬───────────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  PHASE 13: CATALOG AI RECIPES (ai_recipe_catalog_service.py)            │
│  Non-critical — failure is swallowed (logged only)                      │
│  ┌──────────────────────────────────────────────────┐                  │
│  │  For each ~56 items:                              │  ←── Session 12 │
│  │  DB READ: ai_recipe_catalog WHERE normalized_name │                  │
│  │  If exists -> UPDATE usage_count + 1              │                  │
│  │  If new   -> INSERT with display_name, tags, etc. │                  │
│  │  COMMIT                                           │                  │
│  └──────────────────────────────────────────────────┘                  │
└─────────────────────┬───────────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  PHASE 14: BUILD HTTP RESPONSE (meal_plans.py — no DB)                  │
│  ┌──────────────────────────────────────────────────┐                  │
│  │  MealPlanResponse {                               │                  │
│  │    id, week_start_date, week_end_date,            │                  │
│  │    days: [                                        │                  │
│  │      { date, day_name, festival,                  │                  │
│  │        meals: {                                   │                  │
│  │          breakfast: [{recipe_name, prep_time,     │                  │
│  │                       calories, recipe_id, ...}], │                  │
│  │          lunch: [...],                            │                  │
│  │          dinner: [...],                           │                  │
│  │          snacks: [...]                            │                  │
│  │        }                                          │                  │
│  │      } x 7 days                                   │                  │
│  │    ]                                              │                  │
│  │  }                                                │                  │
│  └──────────────────────────────────────────────────┘                  │
│  HTTP 200 OK + JSON body                                                │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Database Sessions Summary

| Phase | Session | Table(s) | Operation |
|:-----:|:-------:|----------|-----------|
| 0 | 1 | `users` | READ |
| 2a | 2 | `user_preferences` | READ |
| 2b-d | 3 | `recipe_rules`, `nutrition_goals` | READ |
| 2e | 4 | `family_members` | READ |
| 4 | 5 | `festivals` | READ |
| 5 | 6 | `system_config` (cached) | READ (once ever) |
| 10 | 7 | `user_preferences` | READ (duplicate!) |
| 11 | 8 | `recipes`, `recipe_ingredients`, `recipe_instructions`, `recipe_nutrition` | READ + WRITE (~56 finds + inserts) |
| 12a | 9 | `meal_plans` | READ + WRITE |
| 12b | 10 | `meal_plans`, `meal_plan_items` | WRITE |
| 12c | 11 | `meal_plans`, `meal_plan_items` | READ |
| 13 | 12 | `ai_recipe_catalog` | READ + WRITE |

**Total: 10-12 independent sessions. No single transaction wraps the flow.**

---

## Database Tables Accessed

| Table | Read | Write | Phase | Purpose |
|-------|:----:|:-----:|:-----:|---------|
| `users` | Y | | 0 | Auth: verify JWT, load user |
| `user_preferences` | Y | | 2, 10 | Dietary prefs, cooking times, family size |
| `recipe_rules` | Y | | 2 | INCLUDE/EXCLUDE rules |
| `nutrition_goals` | Y | | 2 | Nutrition goal targets |
| `family_members` | Y | | 2 | Family member health conditions |
| `festivals` | Y | | 4 | Festivals for the week |
| `system_config` | Y | | 5 | Meal generation config (cached) |
| `recipes` | Y | Y | 11 | Find or create recipe records |
| `recipe_ingredients` | | Y | 11 | Create ingredient records |
| `recipe_instructions` | | Y | 11 | Create instruction records |
| `recipe_nutrition` | | Y | 11 | Create nutrition records |
| `meal_plans` | Y | Y | 12 | Deactivate old, create new |
| `meal_plan_items` | | Y | 12 | Create ~56 meal plan items |
| `ai_recipe_catalog` | Y | Y | 13 | Catalog recipe names |

**Total: 14 tables, 7 reads-only, 4 writes-only, 3 read+write**

---

## External API Calls

| Service | Model | Phase | Timeout | Retries |
|---------|-------|:-----:|---------|:-------:|
| Google Gemini | `gemini-2.5-flash` | 7 | 120s (outer) | 3 attempts, exponential backoff (1s, 2s) |

---

## Time Breakdown (typical ~100s total)

```
 0s  ├──── Auth + Parse request           ~0.1s
     ├──── Load preferences (5 queries)   ~1-2s
     ├──── Filter rules + Load festivals  ~0.5s
     ├──── Load config (cached)           ~0s
     ├──── Build prompt                   ~0s
     │
 3s  ├──── Gemini AI call                 ~5-90s (typically 5-15s)
     │     ├── Attempt 1
     │     ├── Retry 1: +1s backoff
     │     ├── Retry 2: +2s backoff
     │     └── Retry 3: fail -> 503
     │
45s  ├──── Parse + Post-process           ~0.1s
     │
     ├──── Recipe creation (BOTTLENECK)   ~30-60s
     │     └── 56 x (SELECT + INSERT x 4) to remote PostgreSQL
     │
95s  ├──── Save meal plan                 ~2-3s
     │     └── Deactivate old + INSERT 1 plan + 56 items
     │
     ├──── Catalog recipes                ~1-2s (non-critical)
     │
100s └──── Return response                ~0s
     ─────────────────────────────────────────────
     Total: ~100s (within 120s timeout)
```

---

## Error Handling Summary

| Error | HTTP Code | Source File | Trigger |
|-------|:---------:|-------------|---------|
| Missing/invalid JWT | 401 | `deps.py` | Bad or expired token |
| User not found | 404 | `deps.py` | User deleted or ID mismatch |
| Generation timeout | 504 | `meal_plans.py` | Total time exceeds 120s |
| Gemini not configured | 503 | `gemini_client.py` | Missing `GOOGLE_AI_API_KEY` |
| All Gemini retries fail | 503 | `ai_meal_service.py` | 3 consecutive API failures |
| Recipe creation fails | (logged only) | `meal_plans.py` | Items keep "AI_GENERATED" IDs |
| Catalog fails | (logged only) | `meal_plans.py` | Warning logged, response still returned |
| Unhandled exception | 500 | `meal_plans.py` | Any unexpected error |

---

## Simplified Flow (High Level)

```
Request ──> Auth ──> Load Data (6 DB reads) ──> Build Prompt
                                                     │
                                                     ▼
Response <── Save Plan <── Create Recipes <── Gemini AI <──┘
  200 OK      (3 writes)    (56 x 4 writes)   (3-90s)
                                 ^
                                 │
                            BOTTLENECK
                           30-60 seconds
```

---

## Key Optimization Opportunities

| Issue | Current State | Potential Fix |
|-------|---------------|---------------|
| User prefs read twice | Phase 2 and Phase 10 both read `user_preferences` | Pass prefs from Phase 2 to the endpoint |
| 56 individual recipe inserts | Sequential SELECT + INSERT for each item | Batch INSERT with `executemany()` or `insert().values([...])` |
| 12 independent sessions | Each repository opens its own session | Share a single session across the request |
| Plan re-read after create | Phase 12c reads back what 12b just wrote | Return the in-memory plan instead |
| Catalog is synchronous | Phase 13 blocks the response | Run catalog as background task (`asyncio.create_task`) |

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
return _build_response_from_firestore(created_plan)  # returns immediately
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
