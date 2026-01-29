# Continuation Prompt for RasoiAI Project

Use this prompt to start a new conversation/context and continue the project from where we left off.

---

## PROMPT TO USE:

```
I am building **RasoiAI** - an AI-powered meal planning app for Indian families.

## Current State: Production-Ready Backend with 3,590 Recipes

Backend runs on Firebase Firestore with a comprehensive recipe database imported from khanakyabanega.

**Backend Status:**
- Firestore database: `rasoiai-6dcdd`
- **3,590 recipes** (3,580 imported from khanakyabanega + 10 seed recipes)
- 12 festivals seeded
- Auth accepts `fake-firebase-token` for testing

**Recipe Distribution:**
| Category | Count |
|----------|-------|
| North Indian | 3,124 |
| South Indian | 358 |
| West Indian | 85 |
| East Indian | 23 |
| Vegetarian | 3,482 |
| Vegan | 1,347 |

**Recipe Categories (for pairing):**
| Category | Count | % |
|----------|-------|---|
| snack | 705 | 19.6% |
| other | 576 | 16.0% |
| sweet | 365 | 10.2% |
| sabzi | 357 | 9.9% |
| curry | 307 | 8.6% |
| bread | 141 | 3.9% |
| dal | 116 | 3.2% |

**To start backend:**
```bash
cd backend
.\venv\Scripts\activate          # Windows
# source venv/bin/activate       # Linux/Mac
uvicorn app.main:app --reload --port 8000
```

**To run Android tests:**
```bash
cd android
./gradlew :app:connectedDebugAndroidTest
```

### Session 26 Completed Work

**Recipe Import from khanakyabanega:**
- Imported 3,580 recipes from source Firebase project
- Created transformation pipeline:
  - Flat ingredient strings → structured objects (name, quantity, unit, category)
  - Flat instructions → step objects with step_number
  - Cuisine names normalized (e.g., "North Indian" → "north")
  - Diet types mapped to dietary_tags array
  - Meal types normalized

**Scripts Created:**
| Script | Purpose |
|--------|---------|
| `scripts/inspect_source_recipes.py` | Analyze source database schema |
| `scripts/import_recipes_from_kkb.py` | Transform and import recipes |
| `scripts/verify_recipe_import.py` | Verify import results |

### Test Coverage Status

~400 UI tests across 15 screens (Compose UI Testing):

| Phase | Screen | UI Tests | Status |
|-------|--------|----------|--------|
| 1 | Auth | 18 | DONE |
| 2 | Onboarding | 41 | DONE |
| 3 | Generation | - | TODO |
| 4 | Home | 22 | DONE |
| 4b | Recipe Detail | 26 | DONE |
| 5 | Grocery | 21 | DONE |
| 6 | Chat | 17 | DONE |
| 7 | Favorites | 17 | DONE |
| 8 | Stats | 21 | DONE |
| 9 | Settings | 15 | DONE |
| 10 | Pantry | 18 | DONE |
| 11 | Recipe Rules | 22 | DONE |
| 12 | Cooking Mode | 27 | DONE |

### Remaining Work

**Meal Generation Config (All Phases Complete!):**
1. ~~**Phase 2: Backend Updates** - Implement pairing logic in meal generation service~~ DONE
2. ~~**Phase 3: Chat Integration** - LLM function calling for config updates via chat~~ DONE
3. ~~**Phase 4: Testing** - Additional edge case and integration testing~~ DONE (92 backend tests)

**Android/Testing:**
4. **GenerationScreenTest.kt** - UI tests for meal plan generation screen
5. Integration tests for navigation flows
6. Offline mode tests
7. Edge cases and error handling tests
8. Connect Android app to real backend API

### Key Files Reference

- Architecture: `CLAUDE.md`
- E2E Testing Guide: `docs/testing/E2E-Testing-Prompt.md`
- Meal Generation Config: `docs/design/Meal-Generation-Config-Architecture.md`
- Config YAML files: `backend/config/`
- Recipe Import: `backend/scripts/import_recipes_from_kkb.py`
- Backend API: `backend/app/api/v1/endpoints/`
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
├── sync_config.py                 # Syncs YAML config → Firestore (Session 27)
├── categorize_recipes.py          # Adds category field to recipes (Session 27)
└── test_meal_generation.py        # Tests pairing logic (Session 28)
```

**Recipe Import Usage:**
```bash
cd backend
.\venv\Scripts\activate

# Dry run (preview only)
python scripts/import_recipes_from_kkb.py --dry-run --limit 10

# Import specific count
python scripts/import_recipes_from_kkb.py --limit 100

# Import all
python scripts/import_recipes_from_kkb.py --all

# Verify results
python scripts/verify_recipe_import.py
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
- 17 SQLAlchemy models (later migrated to Firestore)
- 18 API endpoints matching Android DTOs
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
- Updated auth to accept `fake-firebase-token` for testing

### Session 25: Complete UI Test Coverage
- Created remaining screen tests (RecipeRules, CookingMode, RecipeDetail)
- Total: ~400 UI tests across 15 screens (including E2E flows)

### Session 26: Recipe Import from khanakyabanega
- Created inspection script to analyze source database
- Built transformation pipeline for recipe data
- Imported 3,580 recipes with structured ingredients/instructions
- Verified import: 3,590 total recipes in RasoiAI

### Session 27: Meal Generation Config (Phase 1 Complete)
- Created config YAML files as source of truth:
  - `backend/config/meal_generation.yaml` - Pairing rules, meal structure
  - `backend/config/reference_data/ingredients.yaml` - 54 ingredients with aliases
  - `backend/config/reference_data/dishes.yaml` - 68 common dishes
  - `backend/config/reference_data/cuisines.yaml` - 4 regional cuisines
- Created `scripts/sync_config.py` to sync YAML → Firestore
- Created `scripts/categorize_recipes.py` to add category field to recipes
- Categorized all 3,590 recipes (84% meaningful categories, 16% "other")
- Firestore collections created: `system_config/meal_generation`, `reference_data/*`

### Session 28: Meal Generation Service (Phase 2 Complete)
- Created `ConfigService` to load pairing rules from Firestore
- Updated `RecipeRepository` with category/pairing search methods:
  - `search_by_category()`, `search_by_categories()`
  - `get_pairing_recipe()`, `get_recipe_pair()`
  - `search_by_ingredient()` with alias support
- Created `MealGenerationService` with 2-item pairing logic:
  - Each meal slot has 2 complementary items (Dal+Rice, Sabzi+Roti, Dosa+Chutney)
  - INCLUDE rules force items with complementary pairs
  - EXCLUDE rules, allergies, dislikes properly enforced
  - Config-driven pairing by cuisine and meal type
- Refactored `/generate` endpoint to use service (cleaned up ~150 lines)
- All tests passing: 46 meal items, 23 paired slots, 0 single slots

### Session 29: Chat Integration (Phase 3 Complete)
- Added tool calling support to Claude client:
  - `ToolCall` and `ChatCompletionResult` dataclasses
  - `generate_with_tools()` function for initial tool calls
  - `continue_with_tool_result()` function for multi-turn tool conversations
- Created `PreferenceUpdateService` with full preference management:
  - `update_recipe_rule()` - ADD/REMOVE/MODIFY INCLUDE/EXCLUDE rules
  - `update_allergy()` - manage food allergies with severity
  - `update_dislike()` - manage disliked ingredients
  - `update_preference()` - cooking time, busy days, dietary tags, cuisine
  - `undo_last_change()` - revert last preference change
  - `show_config()` - display current configuration
  - Conflict detection between INCLUDE and EXCLUDE rules
- Created tool definitions in `app/ai/tools/preference_tools.py`:
  - 6 tools: update_recipe_rule, update_allergy, update_dislike, update_preference, undo_last_change, show_config
  - `CONFIG_CHAT_SYSTEM_PROMPT` for RasoiAI cooking assistant
  - `format_config_for_display()` for readable config output
- Created `ChatRepository` for Firestore-based chat storage
- Updated chat endpoint to remove SQLAlchemy dependency
- Created `scripts/test_chat_tools.py` - all tests passing

### Session 30: Phase 4 Testing Complete
- Created comprehensive test suite (92 backend tests total):
  - `tests/test_preference_service.py` - 26 edge case tests for PreferenceUpdateService
  - `tests/test_chat_integration.py` - 27 integration tests for chat tool flow
  - `tests/test_meal_generation.py` - 22 tests for meal generation structures and logic
  - `tests/test_chat_api.py` - 12 API endpoint tests for chat
- Tests cover:
  - INCLUDE/EXCLUDE rule enforcement
  - Conflict detection between rules
  - Allergy and dislike filtering
  - Dietary tag validation (vegetarian, vegan, jain)
  - Cooking time constraints
  - Tool execution and error handling
  - API authentication and validation
- All phases of Meal Generation Config complete!

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

RECIPE DATA STRUCTURE:
┌─────────────────────────────────────────────────────────────┐
│  Recipe                                                     │
│  ├── id, name, description, image_url                       │
│  ├── cuisine_type: north | south | east | west              │
│  ├── meal_types: [breakfast, lunch, dinner, snacks]         │
│  ├── dietary_tags: [vegetarian, vegan, jain, ...]           │
│  ├── category: dal | sabzi | curry | rice | snack | ...     │  ← NEW
│  ├── prep_time_minutes, cook_time_minutes, servings         │
│  ├── ingredients: [{name, quantity, unit, category}, ...]   │
│  ├── instructions: [{step_number, instruction}, ...]        │
│  └── nutrition: {calories, protein, carbs, fat, fiber}      │
└─────────────────────────────────────────────────────────────┘

MEAL GENERATION CONFIG (Phase 1 Complete):
┌─────────────────────────────────────────────────────────────┐
│  system_config/meal_generation                              │
│  ├── meal_structure: {items_per_slot: 2, expandable: true}  │
│  ├── pairing_rules_flat: {"north:dal": ["rice", "roti"]}    │
│  ├── meal_type_pairs: {breakfast: ["paratha:chai", ...]}    │
│  └── recipe_categories: [dal, rice, sabzi, curry, ...]      │
│                                                             │
│  reference_data/ingredients: 54 with aliases                │
│  reference_data/dishes: 68 with pairing info                │
│  reference_data/cuisines: 4 regional cuisines               │
└─────────────────────────────────────────────────────────────┘
```

---

*Last Updated: January 29, 2026*
*3,590 recipes. 92 backend tests. ~400 UI tests. Meal Generation Config complete (all 4 phases).*
