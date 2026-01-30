# Continuation Prompt for RasoiAI Project

Use this prompt to start a new conversation/context and continue the project from where we left off.

---

## PROMPT TO USE:

```
I am building **RasoiAI** - an AI-powered meal planning app for Indian families.

## Current State: E2E Tests Fixed & Meal Generation Verified

Backend running on PostgreSQL with SQLAlchemy async ORM. All 14 E2E tests passing. Meal generation algorithm fully functional with proper pairing logic.

**Backend Status:**
- Database: PostgreSQL (asyncpg + SQLAlchemy)
- **3,580 recipes** (imported from khanakyabanega)
- 12 festivals seeded
- Auth accepts `fake-firebase-token` for testing
- **170 backend tests** (all passing)
- **14 E2E tests** (all passing)

**Key Documentation:**
| Document | Path |
|----------|------|
| Architecture | `CLAUDE.md` |
| Meal Generation Algorithm | `docs/design/Meal-Generation-Algorithm.md` |
| Meal Generation Config | `docs/design/Meal-Generation-Config-Architecture.md` |
| E2E Test Plan | `docs/testing/E2E-Test-Plan.md` |

**To start backend:**
```bash
cd backend
.\venv\Scripts\activate          # Windows
# source venv/bin/activate       # Linux/Mac

# Ensure DATABASE_URL is set in .env file
uvicorn app.main:app --reload --port 8000
```

### Session 34 Completed Work: E2E Test Fixes

**Issue 1: SQLAlchemy MissingGreenlet Error (13 tests failing)**
- Root Cause: Recipe queries didn't eagerly load `instructions` and `nutrition` relationships
- Fix: Added `selectinload(Recipe.instructions)` and `selectinload(Recipe.nutrition)` to all repository methods:
  - `get_all()`, `search()`, `search_by_category()`, `search_for_meal_generation()`

**Issue 2: Breakfast Pairing Not Working**
- Root Cause: All 3,580 recipes have `category=NULL`. `search_by_category()` filtered by category column returning nothing
- Fixes:
  1. Modified `search_by_category()` to search by name containing the category term
  2. Added `_infer_category_from_name()` helper to determine category from recipe name
  3. Added "chai", "tea", "coffee", "other" to default pairing categories

**Issue 3: Paneer INCLUDE Rule Not Satisfied (flaky test)**
- Root Cause: `search_by_ingredient("Paneer")` returned recipes with paneer in ingredients but not in name
- Fixes:
  1. Added time constraint fallback for INCLUDE rules (try without time limit if no results)
  2. Added generic fallback when no database recipe found
  3. Added preference for recipes with target in name over ingredient-only matches

**Files Modified:**
- `backend/app/repositories/recipe_repository.py` - Eager loading and category search
- `backend/app/services/meal_generation_service.py` - Pairing logic and INCLUDE rule improvements

### Running Tests

**All Tests (170 total):**
```bash
cd backend
source venv/Scripts/activate  # Windows
PYTHONPATH=. pytest tests/ -q
# 170 passed
```

**E2E Tests (hits real PostgreSQL):**
```bash
PYTHONPATH=. pytest tests/test_meal_generation_e2e.py -v
# 14 passed
```

### Sharma Family Verification Results (All Passing)

| Check | Expected | Actual | Status |
|-------|----------|--------|--------|
| Chai in breakfast (DAILY) | 7 | 7 | PASS |
| Dal in lunch+dinner (4x/week) | >=4 | 7 | PASS |
| Paneer in lunch+dinner (2x/week) | >=2 | 3 | PASS |
| No peanut/groundnut (ALLERGY) | 0 | 0 | PASS |
| No karela/lauki/turai (DISLIKES) | 0 | 0 | PASS |
| No mushroom (EXCLUDE) | 0 | 0 | PASS |
| 2 items per slot (<=4 exceptions) | <=4 | 0 | PASS |

### Remaining Work

**High Priority:**
1. Connect Android app to backend meal generation API
2. User preference settings for:
   - Items per meal, weekly deduplication
   - Allergen variant toggle, strict dietary toggle

**Medium Priority:**
3. Add user-configurable deduplication settings
4. Implement per meal-type item counts
5. Add fallback warning notifications for dislikes

**Future Scope:**
- Nutrition goals enforcement
- Festival/fasting day integration
- Leftovers handling
- Seasonal ingredients preference
- Cost optimization

### Key Files Reference

**Documentation:**
- Architecture: `CLAUDE.md`
- Algorithm: `docs/design/Meal-Generation-Algorithm.md`
- Config Architecture: `docs/design/Meal-Generation-Config-Architecture.md`
- E2E Test Plan: `docs/testing/E2E-Test-Plan.md`

**Backend Database:**
- PostgreSQL Pool: `backend/app/db/postgres.py`
- Session Factory: `backend/app/db/database.py`
- Models: `backend/app/models/`

**Backend Service:**
- Meal Generation: `backend/app/services/meal_generation_service.py`
- Config Service: `backend/app/services/config_service.py`
- Recipe Repository: `backend/app/repositories/recipe_repository.py`

**Config Files:**
- Meal Generation: `backend/config/meal_generation.yaml`
- Dishes Reference: `backend/config/reference_data/dishes.yaml`

**Scripts:**
- Recipe Import: `backend/scripts/import_recipes_postgres.py`
- Config Sync: `backend/scripts/sync_config_postgres.py`
- Seed Achievements: `backend/scripts/seed_achievements.py`

**Tests:**
- Unit: `backend/tests/test_meal_generation.py`
- Integration: `backend/tests/test_meal_generation_integration.py`
- E2E: `backend/tests/test_meal_generation_e2e.py`
```

---

## IMPLEMENTATION STATUS (MVP)

| Feature | Status | Notes |
|---------|--------|-------|
| PostgreSQL migration | ✅ Implemented | SQLAlchemy async ORM |
| 2-item pairing logic | ✅ Implemented | Default 2 items per slot |
| Variable items per cooking time | ✅ Implemented | Config-driven, defaults to 2 |
| INCLUDE rules (DAILY/TIMES_PER_WEEK) | ✅ Implemented | Full tracking across week |
| EXCLUDE rules (NEVER frequency) | ✅ Implemented | Ingredient-level filtering |
| Allergy exclusion with variants | ✅ Implemented | Peanut, dairy, gluten, etc. |
| Dislike filtering | ✅ Implemented | Simple name matching |
| Cooking time limits | ✅ Implemented | Weekday/weekend/busy day |
| Weekly deduplication | ✅ Implemented | Main recipes don't repeat |
| Daily ingredient tracking | ✅ Implemented | Same ingredient not in lunch AND dinner |
| Generic suggestions fallback | ✅ Implemented | "Make your own" when no DB recipe |
| Progressive fallbacks | ✅ Implemented | 4 levels implemented |
| SQLAlchemy eager loading | ✅ Fixed | All relationships loaded properly |
| Category-based pairing | ✅ Fixed | Works without category column |
| INCLUDE rule name matching | ✅ Fixed | Prefers recipes with target in name |
| User-configurable dedup settings | 🔮 Future | Currently hardcoded |
| Per meal-type item override | 🔮 Future | Uses global setting |
| Nutrition goals enforcement | 🔮 Future | Not implemented |
| Festival/fasting day integration | 🔮 Future | Not implemented |
| Allergen expansion toggle | 🔮 Future | Currently auto-expanded |

---

## BACKEND TEST SUMMARY

| Test File | Tests | Database | Purpose |
|-----------|-------|----------|---------|
| `test_health.py` | 2 | No | Health check endpoints |
| `test_auth.py` | 3 | No | Firebase authentication |
| `test_preference_service.py` | 26 | No | PreferenceUpdateService |
| `test_chat_integration.py` | 27 | No | Chat tool calling |
| `test_meal_generation.py` | 22 | No | Data structures |
| `test_meal_generation_integration.py` | 29 | No | Rule enforcement |
| `test_meal_generation_e2e.py` | 14 | **PostgreSQL** | Real database E2E |
| `test_chat_api.py` | 12 | No | Chat API endpoints |
| `test_recipe_cache.py` | 35 | No | Recipe cache operations |
| **TOTAL** | **170** | | All passing |

---

## ENVIRONMENT SETUP

**Required Environment Variables** (in `backend/.env`):
```
DATABASE_URL=postgresql+asyncpg://user:password@host:5432/rasoiai
FIREBASE_CREDENTIALS_PATH=./rasoiai-firebase-service-account.json
ANTHROPIC_API_KEY=sk-ant-...
JWT_SECRET_KEY=your-secret-key
DEBUG=true
```

**Database Setup:**
```sql
CREATE DATABASE rasoiai;
CREATE USER rasoiai_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE rasoiai TO rasoiai_user;
```

**Run Migrations:**
```bash
cd backend
alembic upgrade head
```

**Seed Data:**
```bash
PYTHONPATH=. python scripts/seed_festivals.py
PYTHONPATH=. python scripts/seed_achievements.py
PYTHONPATH=. python scripts/sync_config_postgres.py
PYTHONPATH=. python scripts/import_recipes_postgres.py --all
```

---

## PREVIOUS SESSIONS SUMMARY

### Sessions 1-10: Core UI Implementation
- All 13 core screens implemented
- ViewModel pattern with StateFlow
- Hilt DI, Navigation Compose setup

### Sessions 11-13: Wireframe Review & Recipe Rules
- Redesigned Home with 3-level locking
- Recipe Rules screen with 4 tabs

### Sessions 14-18: Android Backend Integration
- Auth token storage, interceptors
- DTO and Entity mappers
- Repository implementations

### Session 19: Python Backend Implementation
- FastAPI backend structure
- Firebase Admin SDK, JWT auth
- Claude AI client

### Sessions 20-25: E2E & UI Tests
- Compose UI Testing framework
- ~400 UI tests across 15 screens

### Session 26: Recipe Import
- 3,580 recipes from khanakyabanega

### Sessions 27-30: Meal Generation Config
- Config YAML files, ConfigService
- MealGenerationService with pairing
- Chat tool calling (6 tools)

### Session 31: Algorithm Design Review
- 7 Key Design Decisions approved
- Comprehensive documentation

### Session 32: Algorithm Implementation
- Variable items per meal implemented
- Generic suggestions fallback added
- 29 integration tests + 15 E2E tests

### Session 33: PostgreSQL Migration
- Migrated from Firestore to PostgreSQL
- All repositories updated for SQLAlchemy
- Added recipe import with --missing-only flag
- 3,580 recipes successfully imported

### Session 34: E2E Test Fixes (Current)
- Fixed SQLAlchemy eager loading (MissingGreenlet error)
- Fixed breakfast pairing (category search by name)
- Fixed INCLUDE rule satisfaction (name preference, fallbacks)
- All 14 E2E tests now passing
- Verified meal generation with Sharma Family profile

---

## ARCHITECTURE DIAGRAM

```
┌─────────────────────────────────────────────────────────────┐
│  ANDROID APP                                                │
│  UI (Compose) → ViewModel → UseCase → Repository            │
│                                           ↓                 │
│                              ┌────────────┴────────────┐    │
│                              ↓                         ↓    │
│                         Room (Local)            Retrofit    │
│                         (Cache)                 (Remote)    │
└─────────────────────────────────────────────────────────────┘
                                                      ↓
┌─────────────────────────────────────────────────────────────┐
│  PYTHON BACKEND (FastAPI)                                   │
│  Endpoints → Services → Repositories → PostgreSQL           │
│                                                             │
│  Database: PostgreSQL (asyncpg + SQLAlchemy async ORM)      │
│  Recipes: 3,580 (imported from khanakyabanega)              │
│  Auth: Accepts "fake-firebase-token" in debug mode          │
└─────────────────────────────────────────────────────────────┘

MEAL GENERATION FLOW:
┌─────────────────────────────────────────────────────────────┐
│  1. Load User Preferences (PostgreSQL)                      │
│  2. Load Config (YAML → PostgreSQL system_config)           │
│  3. Build Exclude List (allergies + dislikes + rules)       │
│  4. For each day (7 days):                                  │
│     a. Determine cooking time (weekday/weekend/busy)        │
│     b. Calculate items per slot (time-based)                │
│     c. Process INCLUDE rules (prefer name matches)          │
│     d. Generate paired meals (main + complementary)         │
│     e. Apply fallbacks if needed (time relaxation)          │
│     f. Use generic suggestions as final fallback            │
│  5. Return GeneratedMealPlan                                │
└─────────────────────────────────────────────────────────────┘

INCLUDE RULE PROCESSING:
┌─────────────────────────────────────────────────────────────┐
│  1. Search for recipes with target ingredient               │
│  2. Prefer recipes with target in NAME (not just ingredient)│
│  3. If no results within time limit → try without time limit│
│  4. If still no results → create generic suggestion         │
│  5. Track assignments to ensure weekly quotas met           │
└─────────────────────────────────────────────────────────────┘

PAIRING LOGIC:
┌─────────────────────────────────────────────────────────────┐
│  1. Infer category from recipe name (chai, dal, paratha...) │
│  2. Look up pairing categories (chai → paratha, poha, toast)│
│  3. Search by category term in recipe NAME (not column)     │
│  4. All recipes have category=NULL, so name search required │
└─────────────────────────────────────────────────────────────┘
```

---

*Last Updated: January 30, 2026*
*Session 34: E2E Tests Fixed & Verified*
*3,580 recipes. 170 backend tests (all passing). 14 E2E tests (all passing). ~400 UI tests.*
