# Continuation Prompt for RasoiAI Project

Use this prompt to start a new conversation/context and continue the project from where we left off.

---

## PROMPT TO USE:

```
I am building **RasoiAI** - an AI-powered meal planning app for Indian families.

## Current State: Production-Ready (All Major Features Complete)

Backend running on PostgreSQL with SQLAlchemy async ORM (44 endpoints, 11 routers). Android app with Compose UI, Hilt DI, Room DB v11. Full E2E tests passing with real API calls. All 18 Settings sub-screens implemented. Gap analysis complete with Phase 1-3 fixes applied.

**Recent Work Highlights:**
- **FR-010 through FR-014:** AI Recipe Catalog, Sharma Recipe Rules, Dedup, Family Members CRUD, Onboarding E2E
- **FullJourneyFlowTest:** 7-step E2E (Auth → Onboarding → MealGen → Home → RecipeRules → MealGen2 → Home2)
- **Email Uniqueness + Auth Merge Fix:** Unique email constraint, account merging on Firebase UID change
- **Settings Screens (Phase A-G):** All 18 navigation destinations with real screens, 76+ backend tests
- **Gap Analysis:** 39 gaps identified, Phases 1A/2/3 implemented (auth URLs, chat nav, meal gen timeout, recipe search)
- **Quick-Win Features:** Notification Badge (#57), Voice Input (#12), Camera/Gallery for Pantry (#14/#15), Recipe Detail menu (#24), Stats share (#25), Chat context (#30)
- **Recipe Rating endpoint, Room DB v11** (meal_plan_items PK fix, known ingredients seed)

**Requirements Documentation Structure:**
```
docs/requirements/
├── README.md                      # Index with navigation
├── screens/
│   ├── 01-splash-auth.md         # 17 requirements (SPLASH/AUTH)
│   ├── 02-onboarding.md          # 35 requirements (ONB)
│   ├── 03-home.md                # 42 requirements (HOME)
│   ├── 04-recipe-detail.md       # 36 requirements (REC/COOK)
│   ├── 05-grocery.md             # 25 requirements (GRO)
│   ├── 06-chat.md                # 20 requirements (CHAT)
│   ├── 07-favorites.md           # 25 requirements (FAV)
│   ├── 08-recipe-rules.md        # 29 requirements (RULE)
│   ├── 09-settings.md            # 33 requirements (SET)
│   ├── 10-stats.md               # 25 requirements (STAT)
│   ├── 11-notifications.md       # 18 requirements (NOTIF)
│   └── 12-common-components.md   # 17 requirements (COM)
├── api/
│   └── backend-api.md            # 27 API endpoints documented
└── _archive/
    ├── RasoiAI Requirements.md   # Original PRD (archived)
    └── wireframes/               # 15 original wireframes (archived)
```

**Test Results Summary:**
| Platform | Tests | Status |
|----------|-------|--------|
| Backend | ~351 (26 files) | PASS |
| Android Unit | ~330 | PASS |
| Android UI | 750+ | PASS |
| Android E2E | 67+ | PASS |

**Key Documentation:**
| Document | Path |
|----------|------|
| Requirements Index | `docs/requirements/README.md` |
| Architecture | `CLAUDE.md` |
| Meal Generation Algorithm | `docs/design/Meal-Generation-Algorithm.md` |
| E2E Test Plan | `docs/testing/E2E-Test-Plan.md` |

**To start backend:**
```bash
cd backend
source venv/bin/activate         # Linux/Mac/Git Bash
# .\venv\Scripts\activate        # Windows PowerShell
uvicorn app.main:app --reload --port 8000
```

**To run tests:**
```bash
# Backend
cd backend && PYTHONPATH=. pytest

# Android Unit Tests
cd android && ./gradlew test

# Android E2E Tests (requires emulator API 34)
./gradlew :app:connectedDebugAndroidTest
```

### Requirements Format Reference

Each requirement in the documentation follows this BDD-style format:

| Field | Value |
|-------|-------|
| **Screen** | [Screen Name] |
| **Element** | [UI Element] |
| **Trigger** | [User Action] |
| **Status** | Implemented / Partial / Planned |
| **Test** | `TestFile.kt:testMethodName` |

**Acceptance Criteria:**
- Given: [precondition]
- When: [action]
- Then: [outcome]
- And: [additional outcomes]

### Backend API Endpoints (44 total across 11 routers)

| Router | Endpoints | Purpose |
|--------|-----------|---------|
| auth | 1 | Firebase token exchange (with account merging) |
| users | 3 | User profile, preferences, onboarding |
| meal_plans | 7 | Meal plan CRUD, swap, lock, generation |
| recipes | 5 | Recipe details, search, rating |
| grocery | 5 | Grocery list management |
| festivals | 2 | Festival calendar |
| chat | 2 | AI chat, image analysis |
| stats | 3 | Cooking statistics |
| notifications | 3 | Push notifications |
| family_members | 4 | Family member CRUD |
| recipe_rules | 9 | Recipe rules CRUD + nutrition goals |
```

---

## IMPLEMENTATION STATUS (MVP)

| Feature | Status | Notes |
|---------|--------|-------|
| Requirements Documentation | DONE | 12 screen files, ~525 requirements |
| PostgreSQL migration | DONE | SQLAlchemy async ORM |
| 2-item pairing logic | DONE | Default 2 items per slot |
| INCLUDE/EXCLUDE rules | DONE | Full tracking across week |
| Android Compose UI | DONE | 18+ screens implemented |
| E2E Test Suite | DONE | 67+ tests passing |
| UI Tests | DONE | 750+ tests |
| Photo Attachment | DONE | Issue #13 - Gemini Vision |
| Items per Meal Dialog | DONE | Issue #16 - Settings |
| AI Recipe Catalog | DONE | Issue #47 (FR-010) - Shared recipe search |
| Sharma Recipe Rules Tests | DONE | Issue #48 (FR-011) - 13 backend tests |
| Recipe Rules Dedup | DONE | Issue #49 (FR-012) - Case normalization, 409 on dup |
| Family Members CRUD | DONE | Issue #50 (FR-013) - Preferences sync + CRUD |
| Sharma Onboarding E2E | DONE | Issue #52 (FR-014) - 5-step onboarding verification |
| FullJourneyFlowTest | DONE | 7-step E2E: Auth→Onboarding→MealGen→Home→Rules→MealGen2→Home2 |
| Email Uniqueness | DONE | Unique email constraint, Alembic migration |
| Auth Merge Fix | DONE | Account merging on Firebase UID change |
| Settings Screens (A-G) | DONE | All 18 destinations with real screens, 76+ backend tests |
| Gap Analysis | DONE | 39 gaps identified across all screens |
| Phase 1A Quick Wins | DONE | Auth URLs, Chat nav, etc. |
| Phase 2 Code Quality | DONE | Error handling, validation fixes |
| Phase 3 Performance | DONE | Meal gen timeout, recipe search optimization |
| Notification Badge | DONE | Issue #57 - Unread count badge |
| Voice Input for Chat | DONE | Issue #12 - Speech-to-text |
| Camera/Gallery for Pantry | DONE | Issues #14/#15 - Image capture |
| Recipe Detail Menu | DONE | Issue #24 - Share/favorite/actions |
| Stats Share | DONE | Issue #25 - Share cooking stats |
| Chat Context | DONE | Issue #30 - Contextual chat |
| Recipe Rating | DONE | Backend endpoint for recipe ratings |
| Room DB v11 | DONE | meal_plan_items PK fix, known ingredients seed |

---

## REQUIREMENTS DOCUMENTATION SUMMARY

### Screen Files Created

| File | Requirements | Key Elements |
|------|--------------|--------------|
| `01-splash-auth.md` | 17 | Splash animation, Google Sign-In |
| `02-onboarding.md` | 35 | 5-step wizard, family members, dietary preferences |
| `03-home.md` | 42 | Week view, meal cards, 3-level locking, swap/add sheets |
| `04-recipe-detail.md` | 36 | Recipe view, cooking mode, step-by-step |
| `05-grocery.md` | 25 | Grocery list, WhatsApp share, check-off |
| `06-chat.md` | 20 | AI assistant, tool calling, image analysis |
| `07-favorites.md` | 25 | Collections, quick access, bulk actions |
| `08-recipe-rules.md` | 29 | 4-tab layout, include/exclude rules |
| `09-settings.md` | 33 | Profile, family, preferences, dark mode |
| `10-stats.md` | 25 | Streak, calendar, achievements, leaderboard |
| `11-notifications.md` | 18 | Festival alerts, meal reminders |
| `12-common-components.md` | 17 | Bottom nav, dialogs, empty states |

### Requirement ID Format

- `SPLASH-001` through `SPLASH-007` - Splash screen
- `AUTH-001` through `AUTH-010` - Authentication
- `ONB-001` through `ONB-035` - Onboarding
- `HOME-001` through `HOME-042` - Home screen
- `REC-001` through `REC-020` - Recipe detail
- `COOK-001` through `COOK-016` - Cooking mode
- `GRO-001` through `GRO-025` - Grocery
- `CHAT-001` through `CHAT-020` - Chat
- `FAV-001` through `FAV-025` - Favorites
- `RULE-001` through `RULE-029` - Recipe rules
- `SET-001` through `SET-033` - Settings
- `STAT-001` through `STAT-025` - Stats
- `NOTIF-001` through `NOTIF-018` - Notifications
- `COM-001` through `COM-017` - Common components

---

## TEST SUMMARY

### Backend Tests (351 total, 26 files)

| Test File | Tests | Purpose |
|-----------|-------|---------|
| `test_health.py` | 2 | Health check |
| `test_auth.py` | 6 | Firebase auth |
| `test_auth_merge.py` | 5 | Auth account merging |
| `test_preference_service.py` | 26 | PreferenceUpdateService |
| `test_chat_integration.py` | 27 | Chat tool calling |
| `test_ai_meal_service.py` | 22 | AI meal generation service |
| `test_chat_api.py` | 12 | Chat API |
| `test_recipe_cache.py` | 35 | Recipe cache |
| `test_recipe_rules_api.py` | 20 | Recipe rules API |
| `test_recipe_search.py` | 10 | Recipe search |
| `test_notification_service.py` | 19 | Notification service |
| `test_notification_api.py` | 11 | Notification API |
| `test_migrate_legacy_rules.py` | 11 | Legacy rule migration |
| `test_ai_recipe_catalog.py` | 16 | AI recipe catalog (FR-010) |
| `test_sharma_recipe_rules.py` | 13 | Sharma family rules (FR-011/FR-014) |
| `test_recipe_rules_dedup.py` | 5 | Recipe rules dedup (FR-012) |
| `test_family_members_api.py` | 8 | Family members CRUD (FR-013) |
| `test_email_uniqueness.py` | 7 | Email uniqueness enforcement |
| `test_recipe_rating.py` | 11 | Recipe rating endpoint |
| `test_recipe_creation_service.py` | 7 | Recipe creation service |
| `test_recipes_api.py` | 13 | Recipes API |
| `test_users_api.py` | 10 | Users API |
| `test_meal_plans_api.py` | 27 | Meal plans API |
| `test_grocery_api.py` | 9 | Grocery API |
| `test_festivals_api.py` | 9 | Festivals API |
| `test_stats_api.py` | 10 | Stats API |

### Android Tests

| Category | Tests | Notes |
|----------|-------|-------|
| Unit Tests | ~330 | ViewModels, repositories |
| UI Tests | 750+ | Compose UI testing |
| E2E Tests | 67+ | Full user flows (incl. FullJourneyFlowTest) |

---

## ENVIRONMENT SETUP

**Required Environment Variables** (in `backend/.env`):
```
DATABASE_URL=postgresql+asyncpg://user:password@host:5432/rasoiai
FIREBASE_CREDENTIALS_PATH=./rasoiai-firebase-service-account.json
ANTHROPIC_API_KEY=sk-ant-...
GOOGLE_AI_API_KEY=your-gemini-api-key
JWT_SECRET_KEY=your-secret-key
DEBUG=true
```

**Android Emulator:**
- Use API 34 (NOT API 36)
- Recommended: Pixel_6_API_34

---

## PREVIOUS SESSIONS SUMMARY

### Post-Session 40: Feature Implementation & Completion
- FR-010 (Issue #47): AI Recipe Catalog
- FR-011 (Issue #48): Sharma Recipe Rules Test Suite
- FR-012 (Issue #49): Recipe Rules Dedup & Case Normalization
- FR-013 (Issue #50): Sync Missing Preferences + Family Members CRUD
- FR-014 (Issue #52): Sharma Onboarding E2E Verification
- Auth test fix (Issue #51): unauthenticated_client fixture
- FullJourneyFlowTest: 7-step E2E (Auth→Onboarding→MealGen→Home→Rules→MealGen2→Home2)
- Email Uniqueness Enforcement: unique index, email normalization
- Auth Merge Fix: account merging on Firebase UID change (was "Failed to save preferences" bug)
- Settings Screens (Phase A-G): All 18 navigation destinations, 76+ backend tests
- Gap Analysis: 39 gaps identified, Phases 1A/2/3 fixes applied
- Quick-Win Features: Notification Badge, Voice Input, Camera/Gallery, Recipe Detail menu, Stats share, Chat context
- Recipe Rating endpoint, Recipe Creation Service
- Room DB v11: meal_plan_items PK fix, known ingredients seed
- Meal plan enhancements: flow definitions, validation script

### Session 40: Requirements Documentation
- Created comprehensive requirements documentation system
- 12 screen files with ~525 BDD-style requirements
- API requirements file for 44 backend endpoints
- Archived original PRD and wireframes
- README.md index with navigation

### Session 39: Issues #13 and #16
- Photo Attachment for Chat (Gemini Vision)
- Items per Meal Selection Dialog

### Session 38: Home Screen E2E Tests
- 24 tests for locking, actions, navigation

### Sessions 1-37: Core Implementation
- Android Compose UI with 18+ screens
- PostgreSQL backend with FastAPI (44 endpoints, 11 routers)
- E2E test infrastructure
- 3,580 recipes imported

---

## ARCHITECTURE DIAGRAM

```
+-------------------------------------------------------------+
|  ANDROID APP                                                 |
|  UI (Compose) -> ViewModel -> UseCase -> Repository          |
|                                            |                 |
|                              +-------------+-------------+   |
|                              |                           |   |
|                         Room (Local)              Retrofit   |
|                         (Cache)                   (Remote)   |
+-------------------------------------------------------------+
                                                      |
+-------------------------------------------------------------+
|  PYTHON BACKEND (FastAPI)                                    |
|  Endpoints -> Services -> Repositories -> PostgreSQL         |
|                                                              |
|  Database: PostgreSQL (asyncpg + SQLAlchemy async ORM)       |
|  Recipes: 3,580 (imported from khanakyabanega)               |
|  Auth: Accepts "fake-firebase-token" in debug mode           |
+-------------------------------------------------------------+
```

---

### Session 41: ADB Flow Test — New User Journey
- **ADB Test:** `/adb-test new-user-journey` — Flow 01 complete, 79/79 steps PASS
- **Bug Fix:** Recipe endpoint 500 error — `recipe_service.py` compared `uuid.UUID` with `String(36)` column in PostgreSQL → type mismatch. Fixed 3 functions (`get_recipe_by_id`, `scale_recipe`, `get_recipes_by_ids`) to compare as strings.
- **Backend tests:** 351/351 passing after fix
- **Key learnings:** Shell chaining for dropdowns works inconsistently (onboarding yes, settings no). Backend API fallback is the reliable strategy. Python bytecache (.pyc) can serve stale code — clear `__pycache__` after fixes.

*Last Updated: February 13, 2026*
*All major features complete. 351 backend tests (26 files). ~330 Android unit tests. 67+ E2E tests. 750+ UI tests.*
*44 API endpoints across 11 routers. 3,580 recipes. ~525 requirements across 12 screen files. Room DB v11.*
