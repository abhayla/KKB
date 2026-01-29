# Continuation Prompt for RasoiAI Project

Use this prompt to start a new conversation/context and continue the project from where we left off.

---

## PROMPT TO USE:

```
I am building **RasoiAI** - an AI-powered meal planning app for Indian families.

## Current State: Algorithm Design Approved

Backend runs on Firebase Firestore with a comprehensive recipe database. Meal generation algorithm has been fully documented and all 7 Key Design Decisions have been reviewed and approved.

**Backend Status:**
- Firestore database: `rasoiai-6dcdd`
- **3,590 recipes** (3,580 imported from khanakyabanega + 10 seed recipes)
- 12 festivals seeded
- Auth accepts `fake-firebase-token` for testing
- 92 backend tests passing

**Key Documentation:**
| Document | Path |
|----------|------|
| Architecture | `CLAUDE.md` |
| Meal Generation Algorithm | `docs/design/Meal-Generation-Algorithm.md` |
| Meal Generation Config | `docs/design/Meal-Generation-Config-Architecture.md` |
| E2E Testing Guide | `docs/testing/E2E-Testing-Prompt.md` |

**To start backend:**
```bash
cd backend
.\venv\Scripts\activate          # Windows
# source venv/bin/activate       # Linux/Mac
uvicorn app.main:app --reload --port 8000
```

### Session 31 Completed Work: Algorithm Design Review

**Created Documentation:**
- `docs/design/Meal-Generation-Algorithm.md` - Comprehensive algorithm documentation
- Cross-referenced with existing `Meal-Generation-Config-Architecture.md`

**7 Key Design Decisions Reviewed & Approved:**

| # | Decision | Key Points |
|---|----------|------------|
| **1** | Meal Structure | Main item(s) + Complementary item(s); 1-2+ mains based on cooking time (≤30m=1, 30-45m=2, >45m=2+); User configurable (Items per meal, Per meal-type, Override); Generic items allowed without database recipe |
| **2** | Weekly Deduplication | User configurable; Default: Main no repeat, Complementary can repeat; Per-rule "Allow repeat" checkbox (default OFF); Priority: Per rule > Per item type > Global toggle |
| **3** | Daily Ingredient Tracking | Only main item categories tracked (dal, sabzi, curry, etc.); Accompaniments (rice, roti) can repeat same day; Classification based on recipe category |
| **4** | DAILY Rules | Merged into Decision #2 (per-rule "Allow repeat" checkbox) |
| **5** | Allergen Expansion | User configurable toggle (default OFF); 8 allergen groups: peanut, dairy, gluten, shellfish, tree nuts, soy, egg, sesame; Regional variants included |
| **6** | Cooking Time Minimums | Global minimum (15 min default) + meal-type overrides; Dinner override enabled by default (45 min) |
| **7** | Progressive Fallbacks | 6-level fallback sequence; Never relax allergies; Dietary tags: user configurable "Strict dietary" toggle (default ON); Dislikes can relax with warning; Generic suggestion as final fallback |

**Important Design Clarifications:**
- Both main and complementary items can be suggested WITHOUT a database recipe (generic suggestions marked as "No recipe - make your own")
- Complementary selection: Variety first (avoid duplicates in same meal), then cuisine preference
- Pairing is config-driven with expandable categories
- Fallbacks try database first, generic suggestion only as final option

### Remaining Work

**Implementation Required:**
1. Update `MealGenerationService` to implement approved design decisions
2. Add user preference settings for:
   - Items per meal (B), Per meal-type (C), Override time (D)
   - Weekly deduplication settings (global + per item type + per rule)
   - Allergen variant toggle
   - Cooking time minimums (global + per meal-type)
   - Strict dietary toggle
3. Update recipe category classification (main vs accompaniment)
4. Implement generic suggestion support (items without database recipes)
5. Add fallback warning notifications for dislikes

**Android/Testing:**
6. GenerationScreenTest.kt - UI tests for meal plan generation screen
7. Connect Android app to real backend API
8. Integration tests for navigation flows

### Key Files Reference

**Documentation:**
- Architecture: `CLAUDE.md`
- Algorithm (NEW): `docs/design/Meal-Generation-Algorithm.md`
- Config Architecture: `docs/design/Meal-Generation-Config-Architecture.md`
- E2E Testing Guide: `docs/testing/E2E-Testing-Prompt.md`

**Backend:**
- Meal Generation Service: `backend/app/services/meal_generation_service.py`
- Config Service: `backend/app/services/config_service.py`
- Recipe Repository: `backend/app/repositories/recipe_repository.py`
- Config YAML files: `backend/config/`

**Test Script:**
- Tabular API Test: `backend/test_meal_api_tabular.py`
```

---

## APPROVED DESIGN DECISIONS SUMMARY

### Decision #1: Meal Structure

```
Meal Slot = Main Item(s) + Complementary Item(s)

Cooking Time → Number of Mains:
- ≤ 30 min  → 1 main + complementary (simple)
- 30-45 min → 2 mains + complementary (full)
- > 45 min  → 2+ mains + complementary (elaborate)

User Settings (Priority: C > B > D > Time):
- B: Items per meal ("2-3", "3-4", "4+")
- C: Per meal-type (different for breakfast/lunch/dinner/snacks)
- D: Override time logic ("Always full", "Always simple")

Complementary Selection:
1. Avoid duplicates in same meal (variety first)
2. Use cuisine preference (North=Roti, South=Rice)
3. If variety impossible, allow duplicate

Generic Items:
- Both main and complementary can be without database recipe
- Marked as "No recipe - make your own"
- Database recipes and generic treated equally
```

### Decision #2: Weekly Deduplication

```
Settings:
- Global toggle: "Allow recipe repeats" (default OFF)
- Per item type - Main: "Can repeat" (default OFF)
- Per item type - Complementary: "Can repeat" (default ON)
- Per INCLUDE rule: "Allow repeat" checkbox (default OFF)

Priority: Per rule > Per item type (D) > Global toggle (A)
```

### Decision #3: Daily Ingredient Tracking

```
Main Item Categories (tracked):     Accompaniment Categories (not tracked):
- dal                               - rice
- sabzi                             - roti
- curry                             - paratha
- biryani                           - naan
- pulao                             - chutney
- khichdi                           - raita
- dosa                              - sambar
- idli                              - pickle
- poha                              - papad
- upma                              - salad
- paneer_dish                       - beverage
- egg_dish                          - chai
```

### Decision #5: Allergen Variants

```python
allergen_variants = {
    "peanut": ["peanuts", "groundnut", "groundnuts", "moongphali"],
    "dairy": ["milk", "cheese", "paneer", "curd", "yogurt", "cream", "butter", "ghee"],
    "gluten": ["wheat", "maida", "atta", "bread", "roti", "naan"],
    "shellfish": ["shrimp", "prawn", "crab", "lobster"],
    "tree nuts": ["almond", "cashew", "walnut", "pistachio", "kaju", "badam"],
    "soy": ["soya", "soybean", "soybeans", "tofu", "soy sauce", "soya chunks"],
    "egg": ["eggs", "anda", "omelette", "egg white", "egg yolk"],
    "sesame": ["til", "sesame seeds", "tahini", "gingelly"],
}
# Default: expansion OFF, user enables via toggle
```

### Decision #6: Cooking Time Minimums

```
Settings:
- Global minimum: 15 min (default)
- Dinner override: Enabled, 45 min (default)
- Other meal overrides: Disabled (user can enable)

Logic: Final time = max(user_time, global_min, meal_override)
```

### Decision #7: Progressive Fallbacks

```
Level 1: Full filters (cuisine + meal_type + time + dietary + excludes)
Level 2: Remove meal_type filter
Level 3: Remove cuisine_type filter
Level 4: Remove cooking time limit
Level 5: Relax dislikes (with warning)
Level 6: Suggest generic item (no database recipe)

NEVER RELAX:
- Allergies (safety critical)
- Dietary tags (if "Strict dietary" ON - default)
```

---

## BACKEND SCRIPTS

```
backend/scripts/
├── inspect_source_recipes.py      # Analyzes khanakyabanega schema
├── import_recipes_from_kkb.py     # Imports & transforms recipes
├── verify_recipe_import.py        # Verifies import results
├── seed_firestore.py              # Seeds initial data
├── seed_recipes.py                # Original recipe seeds
├── seed_festivals.py              # Festival data seeds
├── sync_config.py                 # Syncs YAML config → Firestore
├── categorize_recipes.py          # Adds category field to recipes
└── test_meal_generation.py        # Tests pairing logic

backend/
└── test_meal_api_tabular.py       # Tabular output API test (NEW)
```

---

## TEST FILES CREATED (Sessions 21-25)

```
android/app/src/androidTest/java/com/rasoiai/app/presentation/
├── auth/
│   ├── AuthScreenTest.kt           # 18 UI tests
│   └── AuthIntegrationTest.kt      # 9 integration tests
├── chat/
│   └── ChatScreenTest.kt           # 17 UI tests
├── cookingmode/
│   └── CookingModeScreenTest.kt    # 27 UI tests
├── favorites/
│   └── FavoritesScreenTest.kt      # 17 UI tests
├── grocery/
│   └── GroceryScreenTest.kt        # 21 UI tests
├── home/
│   └── HomeScreenTest.kt           # 22 UI tests
├── onboarding/
│   └── OnboardingScreenTest.kt     # 41 UI tests
├── pantry/
│   └── PantryScreenTest.kt         # 18 UI tests
├── recipedetail/
│   └── RecipeDetailScreenTest.kt   # 26 UI tests
├── reciperules/
│   └── RecipeRulesScreenTest.kt    # 22 UI tests
├── settings/
│   └── SettingsScreenTest.kt       # 15 UI tests
└── stats/
    └── StatsScreenTest.kt          # 21 UI tests
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
- Split wireframes into 16 files

### Sessions 14-18: Android Backend Integration
- Auth token storage in DataStore
- AuthInterceptor for API requests
- DTO and Entity mappers
- Repository implementations for Auth, MealPlan, Recipe, Grocery
- Firebase auth flow verified

### Session 19: Python Backend Implementation
- Created complete FastAPI backend structure
- Firebase Admin SDK integration
- JWT authentication
- Claude AI client for meal planning and chat

### Session 20: E2E Espresso Test Framework (Initial)
- Robot pattern framework created
- 14 flow test classes (phases 1-14)
- Test DI modules

### Sessions 21-23: Compose UI Tests
- Pivoted from Espresso to Compose UI Testing
- Created UI tests for 10 screens
- Established test wrapper composable pattern

### Session 24: Backend Migration to Firestore
- Replaced SQLite/SQLAlchemy with Firebase Firestore
- Created Firestore repositories for all entities

### Session 25: Complete UI Test Coverage
- Created remaining screen tests
- Total: ~400 UI tests across 15 screens

### Session 26: Recipe Import from khanakyabanega
- Imported 3,580 recipes with structured ingredients/instructions
- Verified import: 3,590 total recipes in RasoiAI

### Session 27: Meal Generation Config (Phase 1)
- Created config YAML files as source of truth
- Synced to Firestore

### Session 28: Meal Generation Service (Phase 2)
- Created ConfigService and MealGenerationService
- 2-item pairing logic implemented

### Session 29: Chat Integration (Phase 3)
- Tool calling support with 6 preference tools
- PreferenceUpdateService for CRUD operations

### Session 30: Phase 4 Testing Complete
- 92 backend tests created and passing
- All phases of Meal Generation Config complete

### Session 31: Algorithm Design Review (Current)
- Created comprehensive algorithm documentation
- Reviewed and approved all 7 Key Design Decisions
- Clarified meal structure, deduplication, fallback logic
- Ready for implementation updates

---

## ARCHITECTURE REMINDER

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

MEAL STRUCTURE (Approved Design):
┌─────────────────────────────────────────────────────────────┐
│  Meal Slot                                                  │
│  ├── Main Item 1 + Complementary (recommended)              │
│  ├── Main Item 2 + Complementary (if time allows)           │
│  └── ...                                                    │
│                                                             │
│  Number of mains based on:                                  │
│  - Cooking time: ≤30m=1, 30-45m=2, >45m=2+                 │
│  - User preference (B, C, D options)                        │
│  - Priority: Per meal-type > Items/meal > Override > Time   │
│                                                             │
│  Items can be:                                              │
│  - Database recipe (full details)                           │
│  - Generic suggestion (marked "No recipe - make your own")  │
└─────────────────────────────────────────────────────────────┘
```

---

*Last Updated: January 29, 2026*
*Session 31: Algorithm Design Review - 7 Key Design Decisions approved*
*3,590 recipes. 92 backend tests. ~400 UI tests.*
