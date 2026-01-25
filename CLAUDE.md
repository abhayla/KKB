# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**RasoiAI** (रसोई AI) is an AI-powered meal planning application for Indian families. It generates personalized weekly meal plans based on family preferences, dietary restrictions, regional cuisines, and cultural considerations including festivals and fasting days.

| Attribute | Details |
|-----------|---------|
| **Platform** | Android Native (Kotlin + Jetpack Compose) |
| **Backend** | Python (FastAPI) |
| **Languages** | English + Hindi |
| **Target Market** | Pan-India (Tier 1, 2, 3 cities) |

## Project Status

| Phase | Status | Document |
|-------|--------|----------|
| Requirements | ✅ Complete | `docs/requirements/RasoiAI Requirements.md` |
| Technical Design | ✅ Complete | `docs/design/Technical Design Document.md` |
| Development | ⏳ Pending | |

## Architecture Overview

### Android App (Clean Architecture + MVVM)

```
com.rasoiai.app/
├── di/                    # Hilt dependency injection
├── data/
│   ├── local/             # Room DB, DAOs, Entities
│   ├── remote/            # Retrofit API, DTOs
│   ├── repository/        # Repository implementations
│   └── sync/              # SyncManager, OfflineQueueManager
├── domain/
│   ├── model/             # Domain models (User, MealPlan, Recipe, etc.)
│   ├── repository/        # Repository interfaces (IUserRepository, etc.)
│   └── usecase/           # Business logic (GenerateMealPlanUseCase, etc.)
├── presentation/
│   ├── navigation/        # Compose navigation
│   ├── theme/             # Colors, Typography, Theme
│   ├── common/components/ # Shared composables
│   └── [feature]/         # Screen + ViewModel per feature
└── util/                  # Constants, extensions, helpers
```

### Backend (Python FastAPI)

```
rasoiai-backend/
├── app/
│   ├── api/v1/            # Endpoints: auth, users, meal_plans, recipes, grocery, festivals
│   ├── core/              # Security, Firebase auth, exceptions
│   ├── db/                # Database connection, Redis, Alembic migrations
│   ├── models/            # SQLAlchemy models
│   ├── schemas/           # Pydantic request/response schemas
│   ├── services/          # Business logic services
│   └── ai/
│       ├── llm_client.py  # Claude API integration
│       ├── meal_planner.py
│       ├── prompts/       # LLM prompt templates
│       └── cache.py       # LLM response caching
├── tests/
├── alembic/               # DB migrations
└── requirements.txt
```

## Technical Stack

| Layer | Technology |
|-------|------------|
| Android | Kotlin, Jetpack Compose, Hilt, Room, Retrofit, Coil |
| Backend | Python, FastAPI, SQLAlchemy, Pydantic |
| Database | PostgreSQL, Redis (cache) |
| Auth | Firebase Auth (Phone OTP + Google OAuth) |
| LLM | Claude API (claude-3-sonnet) |
| Storage | AWS S3 (images) |
| Push | Firebase Cloud Messaging |

## Key Design Decisions

1. **Offline-First**: Room DB caches meal plans, recipes, grocery lists. SyncManager handles queued offline actions.
2. **LLM Cost Optimization**: Cache meal plans by preference hash (60-70% savings), store generated recipes for reuse.
3. **Bilingual Content**: All recipes, ingredients, instructions in both English and Hindi.
4. **Festival Intelligence**: 30+ festivals with fasting modes and auto-suggested menus.

## API Structure

Base URL: `/api/v1/`

| Endpoint Group | Key Endpoints |
|----------------|---------------|
| Auth | `POST /auth/firebase` - Firebase token verification |
| Users | `GET/PUT /users/me`, `PUT /users/preferences`, `POST /users/family` |
| Meal Plans | `POST /meal-plans/generate`, `POST /meal-plans/{id}/items/{id}/swap` |
| Recipes | `GET /recipes/{id}`, `GET /recipes/{id}/scale?servings=N` |
| Grocery | `GET /grocery`, `GET /grocery/whatsapp` |
| Festivals | `GET /festivals/upcoming`, `POST /fasting/activate` |

## Database Schema (Key Tables)

- `users` - User profiles with auth provider, preferred language
- `user_preferences` - Dietary preferences, cuisine zones, cooking times
- `family_members` - Up to 8 members with individual dietary restrictions
- `recipes` - Bilingual name/description, dietary tags, cuisine zone
- `meal_plans` / `meal_plan_items` - Weekly plans with swap/skip/lock support
- `grocery_lists` / `grocery_list_items` - Auto-generated, categorized
- `festivals` - 30+ festivals with fasting types and recipe tags

## India-Specific Considerations

When working on this project:
- **Dietary patterns**: Vegetarian majority, Jain (no root vegetables), Sattvic (no onion/garlic), fasting days, Halal
- **Regional cuisines**: North, South, East, West (4 zones) with distinct ingredients and styles
- **Family structures**: Nuclear + joint families (3-8 members), multi-generational support
- **Measurements**: Support katori (bowl), chammach (spoon) alongside metric
- **Grocery**: WhatsApp sharing to kirana stores (no platform APIs in MVP)
- **Offline**: Essential for tier 2-3 cities with connectivity issues

## Key Documentation

| Document | Path |
|----------|------|
| Product Requirements | `docs/requirements/RasoiAI Requirements.md` |
| Technical Design | `docs/design/Technical Design Document.md` |
| Ollie.ai Research | `docs/research/Ollie App Research.md` |
