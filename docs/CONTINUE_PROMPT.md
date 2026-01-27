# Continuation Prompt for RasoiAI Project

Use this prompt to start a new conversation/context and continue the project from where we left off.

---

## PROMPT TO USE:

```
I am building **RasoiAI** - an AI-powered meal planning app for Indian families.

## Current Status

**Android app complete with E2E test framework. Backend implemented.**

### Android App
| Component | Status |
|-----------|--------|
| UI Screens | ✅ Complete (13 screens) |
| Auth Integration | ✅ Complete (Firebase + Backend JWT) |
| API Layer | ✅ Complete (Retrofit + AuthInterceptor) |
| DTO Mappers | ✅ Complete (API → Domain) |
| Entity Mappers | ✅ Complete (Room ↔ Domain) |
| MealPlan Repository | ✅ Complete (offline-first) |
| Recipe Repository | ✅ Complete (offline-first) |
| Grocery Repository | ✅ Complete (offline-first) |
| Other Repositories | ⏳ Fake (Favorites, Chat, Stats) |
| **E2E Test Framework** | ✅ Complete (33 files, 42 tests) |

### E2E Testing Framework (NEW)
| Component | Count | Status |
|-----------|-------|--------|
| Base Infrastructure | 3 files | ✅ Complete |
| Robot Classes | 12 files | ✅ Complete |
| Flow Tests (Phases 1-14) | 14 files | ✅ Complete |
| Performance Tests (Phase 15) | 1 file | ✅ Complete |
| DI Test Modules | 3 files | ✅ Complete |
| **Total** | **33 files** | ✅ Build passes |

### Backend (Python FastAPI)
| Component | Status |
|-----------|--------|
| Project Structure | ✅ Complete |
| Database Models | ✅ 17 tables (SQLite dev, PostgreSQL prod) |
| All 18 API Endpoints | ✅ Implemented |
| Firebase Auth | ✅ Configured (service account loaded) |
| JWT Authentication | ✅ Working |
| Recipe Seed Data | ✅ 17 Indian recipes |
| Festival Seed Data | ✅ 23 festivals (need 2026 dates) |
| Claude AI Integration | ✅ Code ready (needs API key) |

## IMMEDIATE NEXT STEPS

**Choose based on priority:**

### Option 1: Run E2E Tests on Device/Emulator
```bash
cd D:/Abhay/VibeCoding/KKB/android

# Run all E2E tests
./gradlew :app:connectedAndroidTest

# Run specific phase
./gradlew :app:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.flows.OnboardingFlowTest

# Run performance tests
./gradlew :app:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.performance.PerformanceTest
```

### Option 2: Implement Remaining Android Repositories
- `FavoritesRepositoryImpl` - offline-first pattern
- `ChatRepositoryImpl` - connect to chat endpoints
- `StatsRepositoryImpl` - cooking streaks and achievements

### Option 3: Add Claude API Key for AI Features
1. Get API key from console.anthropic.com
2. Add to `backend/.env`: `ANTHROPIC_API_KEY=sk-ant-...`
3. Test meal plan generation with AI

### Option 4: Update Festival Data for 2026
- Current festivals are for 2025
- Update `backend/scripts/seed_festivals.py` with 2026 dates
- Re-run seed script

### Option 5: Production Deployment
- Set up PostgreSQL database
- Configure production environment
- Deploy to cloud (Railway, Render, etc.)

## Key Files Reference

### E2E Testing (NEW)
| Purpose | File |
|---------|------|
| Base Test Class | `app/src/androidTest/java/com/rasoiai/app/e2e/base/BaseE2ETest.kt` |
| Test Data | `app/src/androidTest/java/com/rasoiai/app/e2e/base/TestDataFactory.kt` |
| Test Extensions | `app/src/androidTest/java/com/rasoiai/app/e2e/base/ComposeTestExtensions.kt` |
| Robot Classes | `app/src/androidTest/java/com/rasoiai/app/e2e/robots/*.kt` (12 files) |
| Flow Tests | `app/src/androidTest/java/com/rasoiai/app/e2e/flows/*.kt` (14 files) |
| Performance Tests | `app/src/androidTest/java/com/rasoiai/app/e2e/performance/PerformanceTest.kt` |
| DI Modules | `app/src/androidTest/java/com/rasoiai/app/e2e/di/*.kt` (3 files) |
| E2E Test Guide | `docs/testing/E2E-Testing-Prompt.md` |

### Backend (Python)
| Purpose | File |
|---------|------|
| Main App | `backend/app/main.py` |
| Config | `backend/app/config.py` |
| Environment | `backend/.env` |
| Firebase Auth | `backend/app/core/firebase.py` |
| JWT Security | `backend/app/core/security.py` |
| All Endpoints | `backend/app/api/v1/endpoints/` |
| Database Models | `backend/app/models/` |
| Services | `backend/app/services/` |
| Claude AI | `backend/app/ai/` |
| Seed Scripts | `backend/scripts/seed_*.py` |

### Android
| Purpose | File |
|---------|------|
| Project Context | `CLAUDE.md` |
| API Service | `data/remote/api/RasoiApiService.kt` |
| Auth Repository | `data/repository/AuthRepositoryImpl.kt` |
| MealPlan Repo | `data/repository/MealPlanRepositoryImpl.kt` |
| Recipe Repo | `data/repository/RecipeRepositoryImpl.kt` |
| DTO Mappers | `data/remote/mapper/DtoMappers.kt` |
| Entity Mappers | `data/local/mapper/EntityMappers.kt` |

## Commands Reference

### Android
```bash
cd D:/Abhay/VibeCoding/KKB/android

./gradlew assembleDebug           # Build
./gradlew test                    # Unit tests
./gradlew installDebug            # Install on device
./gradlew :app:connectedAndroidTest  # E2E tests (requires device/emulator)
```

### Backend
```bash
cd D:/Abhay/VibeCoding/KKB/backend

# Activate venv and start server
./venv/Scripts/uvicorn app.main:app --reload

# Run seed scripts (if needed)
./venv/Scripts/python -m scripts.seed_recipes
./venv/Scripts/python -m scripts.seed_festivals

# Test health
curl http://localhost:8000/health
```

## E2E Test Coverage (15 Phases, 42 Tests)

| Phase | Test File | Tests |
|-------|-----------|-------|
| 1 | AuthFlowTest | Splash screen, Google OAuth |
| 2 | OnboardingFlowTest | 5 onboarding steps |
| 3 | MealPlanGenerationTest | Generation progress |
| 4 | HomeScreenTest | Week view, meal interactions |
| 5 | GroceryFlowTest | List display, check/uncheck |
| 6 | ChatFlowTest | Chat interface, suggestions |
| 7 | FavoritesFlowTest | Add favorites, collections |
| 8 | StatsScreenTest | Streak, charts, achievements |
| 9 | SettingsFlowTest | Profile, preferences |
| 10 | PantryFlowTest | Add items, expiring soon |
| 11 | RecipeRulesFlowTest | Include/exclude rules |
| 12 | CookingModeFlowTest | Scaling, cooking mode |
| 13 | OfflineFlowTest | Offline access, sync |
| 14 | EdgeCasesTest | Timeouts, errors, validation |
| 15 | PerformanceTest | Cold start, transitions, memory |

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  ANDROID APP                                                │
│  UI (Compose) → ViewModel → UseCase → Repository            │
│                                           ↓                 │
│                              ┌────────────┴────────────┐    │
│                              ↓                         ↓    │
│                         Room (Local)            Retrofit    │
│                         Source of Truth         (Remote)    │
└─────────────────────────────────┼───────────────────────────┘
                                  │ HTTP/JWT
┌─────────────────────────────────┼───────────────────────────┐
│  BACKEND (FastAPI)              ↓                           │
│  Endpoints → Services → SQLAlchemy → SQLite/PostgreSQL      │
│       ↓                                                     │
│  Claude AI (meal planning, chat)                            │
└─────────────────────────────────────────────────────────────┘
```

Continue from here based on the immediate next step you choose.
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
- AuthRepositoryImpl, MealPlanRepositoryImpl, RecipeRepositoryImpl, GroceryRepositoryImpl
- Firebase auth flow verified

### Session 19: Python Backend Implementation
- Created complete FastAPI backend structure
- 17 SQLAlchemy models (SQLite compatible)
- 18 API endpoints matching Android DTOs
- Firebase Admin SDK integration
- JWT authentication
- Claude AI client for meal planning and chat
- Seed scripts: 17 recipes, 23 festivals
- Server running at localhost:8000
- Firebase service account configured

### Session 20: E2E Espresso Test Framework
- Implemented complete E2E test framework (33 files)
- Robot pattern for all 12 screens
- 14 flow test classes covering phases 1-14
- Performance test class for phase 15
- Test DI modules (TestDataModule, FakeAuthModule, FakeNetworkModule)
- TestDataFactory with Sharma Family test profile
- ComposeTestExtensions for fluent test API
- Build verified passing

---

## E2E TEST FILES CREATED (Session 20)

```
android/app/src/androidTest/java/com/rasoiai/app/e2e/
├── base/
│   ├── BaseE2ETest.kt              # Common setup, Hilt, Compose rules
│   ├── TestDataFactory.kt          # Sharma Family test data
│   └── ComposeTestExtensions.kt    # Fluent test API extensions
├── di/
│   ├── TestDataModule.kt           # Replaces RepositoryModule with fakes
│   ├── FakeAuthModule.kt           # FakeGoogleAuthClient, FakeAuthRepository
│   └── FakeNetworkModule.kt        # FakeNetworkMonitor for offline testing
├── robots/
│   ├── AuthRobot.kt
│   ├── OnboardingRobot.kt
│   ├── HomeRobot.kt
│   ├── GroceryRobot.kt
│   ├── ChatRobot.kt
│   ├── FavoritesRobot.kt
│   ├── StatsRobot.kt
│   ├── SettingsRobot.kt
│   ├── PantryRobot.kt
│   ├── RecipeRulesRobot.kt
│   ├── RecipeDetailRobot.kt
│   └── CookingModeRobot.kt
├── flows/
│   ├── AuthFlowTest.kt             # Phase 1
│   ├── OnboardingFlowTest.kt       # Phase 2
│   ├── MealPlanGenerationTest.kt   # Phase 3
│   ├── HomeScreenTest.kt           # Phase 4
│   ├── GroceryFlowTest.kt          # Phase 5
│   ├── ChatFlowTest.kt             # Phase 6
│   ├── FavoritesFlowTest.kt        # Phase 7
│   ├── StatsScreenTest.kt          # Phase 8
│   ├── SettingsFlowTest.kt         # Phase 9
│   ├── PantryFlowTest.kt           # Phase 10
│   ├── RecipeRulesFlowTest.kt      # Phase 11
│   ├── CookingModeFlowTest.kt      # Phase 12
│   ├── OfflineFlowTest.kt          # Phase 13
│   └── EdgeCasesTest.kt            # Phase 14
└── performance/
    └── PerformanceTest.kt          # Phase 15
```

---

## BACKEND FILES CREATED (Session 19)

```
backend/
├── app/
│   ├── main.py                    # FastAPI entry point
│   ├── config.py                  # Pydantic settings
│   ├── api/
│   │   ├── deps.py                # Auth dependencies
│   │   └── v1/
│   │       ├── router.py          # Route aggregator
│   │       └── endpoints/         # 8 endpoint files
│   ├── core/
│   │   ├── security.py            # JWT handling
│   │   ├── firebase.py            # Firebase Admin SDK
│   │   └── exceptions.py          # Custom exceptions
│   ├── db/
│   │   ├── database.py            # SQLAlchemy async engine
│   │   └── base.py                # Base model class
│   ├── models/                    # 7 model files, 17 tables
│   ├── schemas/                   # 8 Pydantic schema files
│   ├── services/                  # 8 service files
│   └── ai/                        # Claude integration
│       ├── claude_client.py
│       ├── meal_planner.py
│       ├── chat_assistant.py
│       └── prompts/
├── scripts/
│   ├── seed_recipes.py            # 17 Indian recipes
│   └── seed_festivals.py          # 23 festivals
├── .env                           # Environment config
├── requirements.txt
├── Dockerfile
└── docker-compose.yml
```

---

*Last Updated: January 2026*
*E2E test framework complete (33 files). Backend implemented. Ready to run E2E tests on device/emulator.*
