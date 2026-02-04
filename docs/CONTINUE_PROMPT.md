# Continuation Prompt for RasoiAI Project

Use this prompt to start a new conversation/context and continue the project from where we left off.

---

## PROMPT TO USE:

```
I am building **RasoiAI** - an AI-powered meal planning app for Indian families.

## Current State: Requirements Documentation Complete

Backend running on PostgreSQL with SQLAlchemy async ORM. Android app with Compose UI, Hilt DI, Room DB. Full E2E tests passing with real API calls.

**Latest Session (Session 40): Comprehensive Requirements Documentation**
- Created **single source of truth** requirements documentation at `docs/requirements/`
- **12 screen requirement files** with BDD-style (Given/When/Then) format
- **~525 requirements** documented across all screens
- Archived original PRD and 15 wireframe files
- Each requirement has: ID, status, test references, acceptance criteria

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

**Previous Sessions:**
- Session 39: Issues #13 (Photo Attachment) and #16 (Items per Meal Dialog)
- Session 38: Home Screen E2E Tests (24 tests)
- Session 37: E2E Test Reliability Phase 2

**Test Results Summary:**
| Platform | Tests | Status |
|----------|-------|--------|
| Backend | 170 | PASS |
| Android Unit | 319 | PASS |
| Android UI | 400+ | PASS |
| Android E2E | 65+ | PASS |

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

### Backend API Endpoints (27 total)

| Router | Endpoints | Purpose |
|--------|-----------|---------|
| auth | 1 | Firebase token exchange |
| users | 2 | User profile management |
| meal_plans | 5 | Meal plan CRUD, swap, lock |
| recipes | 4 | Recipe details, search |
| grocery | 5 | Grocery list management |
| festivals | 2 | Festival calendar |
| chat | 2 | AI chat, image analysis |
| stats | 3 | Cooking statistics |
| notifications | 3 | Push notifications |
```

---

## IMPLEMENTATION STATUS (MVP)

| Feature | Status | Notes |
|---------|--------|-------|
| Requirements Documentation | DONE | 12 screen files, ~525 requirements |
| PostgreSQL migration | DONE | SQLAlchemy async ORM |
| 2-item pairing logic | DONE | Default 2 items per slot |
| INCLUDE/EXCLUDE rules | DONE | Full tracking across week |
| Android Compose UI | DONE | 15 screens implemented |
| E2E Test Suite | DONE | 65+ tests passing |
| UI Tests | DONE | ~400 tests |
| Photo Attachment | DONE | Issue #13 - Gemini Vision |
| Items per Meal Dialog | DONE | Issue #16 - Settings |

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

### Backend Tests (170 total)

| Test File | Tests | Purpose |
|-----------|-------|---------|
| `test_health.py` | 2 | Health check |
| `test_auth.py` | 3 | Firebase auth |
| `test_preference_service.py` | 26 | PreferenceUpdateService |
| `test_chat_integration.py` | 27 | Chat tool calling |
| `test_meal_generation.py` | 22 | Data structures |
| `test_meal_generation_integration.py` | 29 | Rule enforcement |
| `test_meal_generation_e2e.py` | 14 | PostgreSQL E2E |
| `test_chat_api.py` | 12 | Chat API |
| `test_recipe_cache.py` | 35 | Recipe cache |
| `test_notification_service.py` | 19 | Notification service |
| `test_notification_api.py` | 10 | Notification API |

### Android Tests

| Category | Tests | Notes |
|----------|-------|-------|
| Unit Tests | 319 | ViewModels, repositories |
| UI Tests | 400+ | Compose UI testing |
| E2E Tests | 65+ | Full user flows |

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

### Session 40: Requirements Documentation (Current)
- Created comprehensive requirements documentation system
- 12 screen files with ~525 BDD-style requirements
- API requirements file for 27 backend endpoints
- Archived original PRD and wireframes
- README.md index with navigation

### Session 39: Issues #13 and #16
- Photo Attachment for Chat (Gemini Vision)
- Items per Meal Selection Dialog

### Session 38: Home Screen E2E Tests
- 24 tests for locking, actions, navigation

### Sessions 1-37: Core Implementation
- Android Compose UI with 15 screens
- PostgreSQL backend with FastAPI
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

*Last Updated: February 4, 2026*
*Session 40: Requirements Documentation (~525 requirements across 12 screen files)*
*3,580 recipes. 170 backend tests. 319 Android unit tests. 65+ Android E2E tests. ~400 UI tests.*
