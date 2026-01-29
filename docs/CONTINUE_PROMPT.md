# Continuation Prompt for RasoiAI Project

Use this prompt to start a new conversation/context and continue the project from where we left off.

---

## PROMPT TO USE:

```
I am building **RasoiAI** - an AI-powered meal planning app for Indian families.

## Current State: Meal Generation Algorithm Implemented

Backend runs on Firebase Firestore with a comprehensive recipe database. Meal generation algorithm has been implemented with variable items per meal, generic suggestions fallback, and comprehensive E2E testing.

**Backend Status:**
- Firestore database: `rasoiai-6dcdd`
- **3,590 recipes** (3,580 imported from khanakyabanega + 10 seed recipes)
- 12 festivals seeded
- Auth accepts `fake-firebase-token` for testing
- **136 backend tests** (8 test files)

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
uvicorn app.main:app --reload --port 8000
```

### Session 32 Completed Work: Algorithm Implementation

**Implemented Features:**
1. **Variable items per meal** (per Design Decision #1)
   - Config-driven time thresholds in `meal_generation.yaml`
   - `_calculate_items_for_slot()` method
   - ≤30 min → 1 main + complementary
   - 30-45 min → 2 mains + complementary
   - >45 min → 2+ mains + complementary

2. **Generic suggestions fallback**
   - `is_generic` field added to `MealItem` dataclass
   - `_get_generic_dishes_for_slot()` method with dishes for all cuisines/slots
   - `_create_generic_meal_item()` creates "make your own" items
   - Final fallback when database lacks recipes (e.g., East Indian with only 23 recipes)

3. **Integration tests** (29 tests)
   - Allergy enforcement (peanut, dairy, gluten variants)
   - Dislike filtering
   - INCLUDE/EXCLUDE rule tracking
   - Cooking time enforcement
   - Weekly deduplication
   - Generic suggestions for all cuisines/slots

4. **E2E tests against real Firestore** (15 tests)
   - Sharma Family profile verification
   - East Indian cuisine (triggers generic fallback)
   - South Indian cuisine with Idli INCLUDE rule
   - Verification report generation

**Test Files Created:**
| File | Tests | Purpose |
|------|-------|---------|
| `test_meal_generation.py` | 22 | Unit tests (data structures) |
| `test_meal_generation_integration.py` | 29 | Integration tests (rule enforcement) |
| `test_meal_generation_e2e.py` | 15 | E2E tests (real Firestore) |

**Files Modified:**
| File | Changes |
|------|---------|
| `meal_generation_service.py` | `is_generic`, `_calculate_items_for_slot()`, `_get_generic_dishes_for_slot()` |
| `config_service.py` | `time_based_items` in `MealStructure` |
| `meal_generation.yaml` | Time thresholds config |
| `Meal-Generation-Algorithm.md` | Implementation Status table |
| `E2E-Test-Plan.md` | Backend E2E Testing section |
| `CLAUDE.md` | Updated test counts (136 backend tests) |

### Running E2E Tests

**Backend Integration/Unit Tests (no Firestore):**
```bash
cd backend
PYTHONPATH=. pytest tests/test_meal_generation.py tests/test_meal_generation_integration.py -v
# 51 tests, ~0.2 seconds
```

**E2E Tests (real Firestore - watch quota limits):**
```bash
cd backend
PYTHONPATH=. pytest tests/test_meal_generation_e2e.py -v -s
# 15 tests, hits real Firestore
# May get 429 Quota exceeded if run too frequently
```

**Verification Report (recommended):**
```bash
PYTHONPATH=. pytest tests/test_meal_generation_e2e.py::TestVerificationReport -v -s
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
1. User preference settings for:
   - Items per meal (B), Per meal-type (C), Override time (D)
   - Weekly deduplication settings
   - Allergen variant toggle
   - Strict dietary toggle
2. Connect Android app to backend meal generation API
3. Run full E2E verification when Firestore quota resets

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

**Backend Service:**
- Meal Generation: `backend/app/services/meal_generation_service.py`
- Config Service: `backend/app/services/config_service.py`
- Recipe Repository: `backend/app/repositories/recipe_repository.py`

**Config Files:**
- Meal Generation: `backend/config/meal_generation.yaml`
- Dishes Reference: `backend/config/reference_data/dishes.yaml`

**Tests:**
- Unit: `backend/tests/test_meal_generation.py`
- Integration: `backend/tests/test_meal_generation_integration.py`
- E2E: `backend/tests/test_meal_generation_e2e.py`
```

---

## IMPLEMENTATION STATUS (MVP)

| Feature | Status | Notes |
|---------|--------|-------|
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

| Test File | Tests | Firestore | Purpose |
|-----------|-------|-----------|---------|
| `test_health.py` | 2 | No | Health check endpoints |
| `test_auth.py` | 3 | No | Firebase authentication |
| `test_preference_service.py` | 26 | No | PreferenceUpdateService |
| `test_chat_integration.py` | 27 | No | Chat tool calling |
| `test_meal_generation.py` | 22 | No | Data structures |
| `test_meal_generation_integration.py` | 29 | No | Rule enforcement |
| `test_meal_generation_e2e.py` | 15 | **Yes** | Real Firestore |
| `test_chat_api.py` | 12 | No | Chat API endpoints |
| **TOTAL** | **136** | | |

---

## FIRESTORE QUOTA NOTES

E2E tests hit real Firestore and can exhaust daily quota:

- Free tier: ~50K reads/day
- Each E2E run: ~100-200 reads
- If `429 Quota exceeded`: wait 24 hours or upgrade to Blaze plan
- Run E2E tests sparingly (once per day during development)

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
- Total: 3,590 recipes

### Sessions 27-30: Meal Generation Config
- Config YAML files, ConfigService
- MealGenerationService with pairing
- Chat tool calling (6 tools)
- 92 backend tests

### Session 31: Algorithm Design Review
- 7 Key Design Decisions approved
- Comprehensive documentation

### Session 32: Algorithm Implementation (Current)
- Variable items per meal implemented
- Generic suggestions fallback added
- 29 integration tests + 15 E2E tests
- Total: 136 backend tests

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
│  Endpoints → Services → Repositories → Firestore            │
│                                                             │
│  Database: Firebase Firestore (project: rasoiai-6dcdd)      │
│  Recipes: 3,590 (imported from khanakyabanega)              │
│  Auth: Accepts "fake-firebase-token" in debug mode          │
└─────────────────────────────────────────────────────────────┘

MEAL GENERATION FLOW:
┌─────────────────────────────────────────────────────────────┐
│  1. Load User Preferences (Firestore)                       │
│  2. Load Config (YAML → Firestore)                          │
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

*Last Updated: January 29, 2026*
*Session 32: Meal Generation Algorithm Implementation Complete*
*3,590 recipes. 136 backend tests. ~400 UI tests.*
