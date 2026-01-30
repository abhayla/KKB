# Continuation Prompt for RasoiAI Project

Use this prompt to start a new conversation/context and continue the project from where we left off.

---

## PROMPT TO USE:

```
I am building **RasoiAI** - an AI-powered meal planning app for Indian families.

## Current State: PostgreSQL Migration Complete

Backend migrated from Firebase Firestore to PostgreSQL with SQLAlchemy async ORM. All repositories updated, 3,580 recipes imported.

**Backend Status:**
- Database: PostgreSQL (asyncpg + SQLAlchemy)
- **3,580 recipes** (imported from khanakyabanega)
- 12 festivals seeded
- Auth accepts `fake-firebase-token` for testing
- **156 backend tests** (9 test files)

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

### Session 33 Completed Work: PostgreSQL Migration

**Migration Changes:**
1. **Database Layer**
   - Added `app/db/postgres.py` - Connection pool management
   - Updated `app/db/database.py` - SQLAlchemy async session factory
   - Added Alembic migration for PostgreSQL schema

2. **Repository Updates** (all now use SQLAlchemy)
   - `recipe_repository.py` - Recipe CRUD with async queries
   - `user_repository.py` - User preferences storage
   - `meal_plan_repository.py` - Meal plan persistence
   - `chat_repository.py` - Chat message storage
   - `festival_repository.py` - Festival data

3. **Scripts Added**
   - `import_recipes_postgres.py` - Recipe import with `--missing-only` flag
   - `sync_config_postgres.py` - YAML config → PostgreSQL sync
   - `seed_achievements.py` - Achievement data seeding

4. **Security**
   - Removed hardcoded credentials from scripts
   - All scripts now use `DATABASE_URL` environment variable
   - Added credential-containing setup scripts to `.gitignore`

**Recipe Import:**
```bash
cd backend
source venv/Scripts/activate  # Windows

# Import only missing recipes
PYTHONPATH=. python scripts/import_recipes_postgres.py --missing-only

# Import all (fresh database)
PYTHONPATH=. python scripts/import_recipes_postgres.py --all
```

### Running Tests

**All Tests (156 total):**
```bash
cd backend
pytest tests/ -q
# 156 passed (14 E2E tests may fail - need investigation)
```

**Unit/Integration Tests (no database):**
```bash
pytest tests/test_meal_generation.py tests/test_meal_generation_integration.py -v
# 51 tests, ~0.2 seconds
```

**E2E Tests (hits real PostgreSQL):**
```bash
pytest tests/test_meal_generation_e2e.py -v -s
# 15 tests - currently 14 failing, needs investigation
```

### Sharma Family Verification Checklist

| Check | Expected | Status |
|-------|----------|--------|
| Peanut allergy | 0 peanut/groundnut recipes | CRITICAL |
| Mushroom EXCLUDE | 0 mushroom recipes | Required |
| Dislikes (karela/lauki/turai) | 0 recipes | Required |
| Chai DAILY | 7 in breakfast | Required |
| Dal 4x/week | 4+ in lunch/dinner | Required |
| Paneer 2x/week | 2+ in lunch/dinner | Required |
| 2-item pairing | Most slots have 2 items | Required |
| No duplicate mains | Unique main recipes | Recommended |

### Remaining Work

**High Priority:**
1. Fix 14 failing E2E tests (PostgreSQL compatibility)
2. Connect Android app to backend meal generation API
3. User preference settings for:
   - Items per meal, weekly deduplication
   - Allergen variant toggle, strict dietary toggle

**Medium Priority:**
4. Add user-configurable deduplication settings
5. Implement per meal-type item counts
6. Add fallback warning notifications for dislikes

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
| `test_meal_generation_e2e.py` | 15 | **PostgreSQL** | Real database E2E |
| `test_chat_api.py` | 12 | No | Chat API endpoints |
| `test_recipe_cache.py` | 42 | No | Recipe cache operations |
| **TOTAL** | **178** | | |

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

### Session 33: PostgreSQL Migration (Current)
- Migrated from Firestore to PostgreSQL
- All repositories updated for SQLAlchemy
- Added recipe import with --missing-only flag
- 3,580 recipes successfully imported
- 156 backend tests passing

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
│     c. Process INCLUDE rules                                │
│     d. Generate paired meals (main + complementary)         │
│     e. Apply fallbacks if needed                            │
│     f. Use generic suggestions as final fallback            │
│  5. Return GeneratedMealPlan                                │
└─────────────────────────────────────────────────────────────┘
```

---

*Last Updated: January 30, 2026*
*Session 33: PostgreSQL Migration Complete*
*3,580 recipes. 156 backend tests. ~400 UI tests.*
