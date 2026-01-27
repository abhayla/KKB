# Continuation Prompt for RasoiAI Project

Use this prompt to start a new conversation/context and continue the project from where we left off.

---

## PROMPT TO USE:

```
I am building **RasoiAI** - an AI-powered meal planning app for Indian families.

## Current Status

**Android app complete. Backend implemented and running.**

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

### Server Status
- Backend running at `http://localhost:8000`
- API docs at `http://localhost:8000/docs`
- Health check: `curl http://localhost:8000/health`

## IMMEDIATE NEXT STEPS

**Choose based on priority:**

### Option 1: End-to-End Testing
1. Start backend: `cd backend && ./venv/Scripts/uvicorn app.main:app --reload`
2. Install Android app: `cd android && ./gradlew installDebug`
3. Test full auth flow (Google Sign-In → Firebase → Backend JWT)
4. Test meal plan generation

### Option 2: Add Claude API Key for AI Features
1. Get API key from console.anthropic.com
2. Add to `backend/.env`: `ANTHROPIC_API_KEY=sk-ant-...`
3. Test meal plan generation with AI

### Option 3: Update Festival Data for 2026
- Current festivals are for 2025
- Update `backend/scripts/seed_festivals.py` with 2026 dates
- Re-run seed script

### Option 4: Implement Remaining Android Repositories
- `FavoritesRepositoryImpl` - offline-first pattern
- `ChatRepositoryImpl` - connect to chat endpoints
- `StatsRepositoryImpl` - cooking streaks and achievements

### Option 5: Production Deployment
- Set up PostgreSQL database
- Configure production environment
- Deploy to cloud (Railway, Render, etc.)

## Key Files Reference

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

### Android
```bash
cd D:/Abhay/VibeCoding/KKB/android

./gradlew assembleDebug   # Build
./gradlew test            # Run tests
./gradlew installDebug    # Install on device
```

## API Endpoints (18 total)

| Method | Endpoint | Auth |
|--------|----------|------|
| POST | `/api/v1/auth/firebase` | No |
| GET | `/api/v1/users/me` | Yes |
| PUT | `/api/v1/users/preferences` | Yes |
| POST | `/api/v1/meal-plans/generate` | Yes |
| GET | `/api/v1/meal-plans/current` | Yes |
| GET | `/api/v1/meal-plans/{id}` | Yes |
| POST | `/api/v1/meal-plans/{id}/items/{itemId}/swap` | Yes |
| PUT | `/api/v1/meal-plans/{id}/items/{itemId}/lock` | Yes |
| GET | `/api/v1/recipes/{id}` | Yes |
| GET | `/api/v1/recipes/{id}/scale` | Yes |
| GET | `/api/v1/recipes/search` | Yes |
| GET | `/api/v1/grocery` | Yes |
| GET | `/api/v1/grocery/whatsapp` | Yes |
| GET | `/api/v1/festivals/upcoming` | No |
| POST | `/api/v1/chat/message` | Yes |
| GET | `/api/v1/chat/history` | Yes |
| GET | `/api/v1/stats/streak` | Yes |
| GET | `/api/v1/stats/monthly` | Yes |

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

---

## BACKEND FILES CREATED

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
*Backend fully implemented. Server running. Firebase auth configured. Ready for end-to-end testing.*
