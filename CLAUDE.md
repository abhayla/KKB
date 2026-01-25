# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**RasoiAI** (रसोई AI) is an AI-powered meal planning application for Indian families. It generates personalized weekly meal plans based on family preferences, dietary restrictions, regional cuisines, and cultural considerations including festivals and fasting days.

| Attribute | Details |
|-----------|---------|
| **Platform** | Android Native (Kotlin + Jetpack Compose) |
| **Backend** | Python (FastAPI) |
| **Language** | English only |
| **Target Market** | Pan-India (Tier 1, 2, 3 cities) |

## Current Status

**Android project structure is set up.** Ready for feature development.

| Phase | Status | Document |
|-------|--------|----------|
| Requirements | ✅ Complete | `docs/requirements/RasoiAI Requirements.md` |
| Technical Design | ✅ Complete | `docs/design/RasoiAI Technical Design.md` |
| Architecture Decisions | ✅ Complete | `docs/design/Android Architecture Decisions.md` |
| Design System | ✅ Complete | `docs/design/RasoiAI Design System.md` |
| Screen Wireframes | ✅ Complete | `docs/design/RasoiAI Screen Wireframes.md` |
| Android Project Setup | ✅ Complete | `android/` folder |
| Feature Development | ⏳ Next Step | Auth, Onboarding, Home screens |

## Key Architecture Decisions

| Area | Decision |
|------|----------|
| Dependency Injection | Hilt |
| Annotation Processing | KSP |
| State Management | StateFlow + Single UiState Data Class |
| Navigation | Navigation Compose |
| Build Configuration | Kotlin DSL + Version Catalog (TOML) |
| Minimum SDK | API 24 (Android 7.0) |
| Testing Strategy | 70% Unit / 20% Integration / 10% UI |
| Modularization | By-Layer (app, core, data, domain) → Hybrid later |

## Design System

| Element | Light | Dark |
|---------|-------|------|
| Primary | `#FF6838` (Orange) | `#FFB59C` |
| Secondary | `#5A822B` (Green) | `#A8D475` |
| Background | `#FDFAF4` (Cream) | `#1C1B1F` |
| Surface | `#FFFFFF` | `#2B2930` |

| Token | Value |
|-------|-------|
| Typography | Roboto (System Default) |
| Spacing | 8dp grid (4, 8, 16, 24, 32, 48dp) |
| Shapes | Rounded corners (8dp small, 16dp medium, 24dp large) |
| Dark Mode | System-follow (auto-switch) |

## Module Structure

```
RasoiAI/
├── app/                          # Main application module
│   └── presentation/             # All screens & ViewModels
│       ├── navigation/
│       ├── theme/
│       ├── common/
│       └── [feature]/            # splash, auth, onboarding, home, recipe, etc.
├── core/                         # Shared utilities & UI components
│   ├── ui/                       # Theme, shared composables
│   ├── util/                     # Extensions, constants
│   └── network/                  # NetworkMonitor
├── data/                         # Data layer module
│   ├── local/                    # Room DB, DAOs, Entities
│   ├── remote/                   # Retrofit API, DTOs
│   ├── repository/               # Repository implementations
│   └── sync/                     # SyncManager, OfflineQueueManager
├── domain/                       # Domain layer module (pure Kotlin)
│   ├── model/                    # Domain models
│   ├── repository/               # Repository interfaces
│   └── usecase/                  # Business logic use cases
└── gradle/
    └── libs.versions.toml        # Centralized dependency versions
```

**Module Dependencies:** `app → core, data, domain` | `data → core, domain` | `domain → (none)` | `core → (none)`

## Technical Stack

| Component | Technology |
|-----------|------------|
| Android | Kotlin 1.9.22, Jetpack Compose (BOM 2024.02), Min SDK 24, Target SDK 34 |
| DI & Processing | Hilt 2.50, KSP 1.9.22 |
| Android Libraries | Room 2.6.1, Retrofit 2.9.0, Coil 2.5.0, Navigation Compose 2.7.7, DataStore 1.0.0 |
| Backend | Python, FastAPI, SQLAlchemy, Pydantic |
| Database | PostgreSQL, Redis (cache) |
| Auth | Firebase Auth (Google OAuth only) |
| LLM | Claude API (claude-3-sonnet) |
| Testing | JUnit5, MockK, Turbine, Compose Testing |

## Key Design Decisions

1. **Offline-First**: Room DB caches meal plans, recipes, grocery lists. SyncManager handles queued offline actions.
2. **LLM Cost Optimization**: Cache meal plans by preference hash (60-70% savings), store generated recipes for reuse.
3. **Auth**: Google OAuth only (Phone OTP removed for MVP simplicity).
4. **Festival Intelligence**: 30+ festivals with fasting modes and auto-suggested menus.

## India-Specific Considerations

- **Dietary patterns**: Vegetarian majority, Jain (no root vegetables), Sattvic (no onion/garlic), fasting days, Halal
- **Regional cuisines**: North, South, East, West (4 zones) with distinct ingredients and styles
- **Family structures**: Nuclear + joint families (3-8 members), multi-generational support
- **Measurements**: Support katori (bowl), chammach (spoon) alongside metric
- **Grocery**: WhatsApp sharing to kirana stores
- **Offline**: Essential for tier 2-3 cities with connectivity issues

## App Screens (12 Total)

| # | Screen | Key Features |
|---|--------|--------------|
| 1 | Splash | Logo, loading state |
| 2 | Auth | Google OAuth only |
| 3 | Onboarding | 5 steps with dropdowns |
| 4 | Home | 4 meal types (Breakfast/Lunch/Dinner/Snacks), multiple recipes per meal, individual lock/swap |
| 5 | Recipe Detail | Tabs (Ingredients/Instructions) |
| 6 | Cooking Mode | Full-screen steps, timer, keep-awake |
| 7 | Grocery List | Categorized items, WhatsApp share |
| 8 | Favorites | 2-column grid, reorder, Recently Viewed |
| 9 | Chat | AI assistant, history, clear chat, time-based actions |
| 10 | Pantry Scan | Camera, expiry tracking, grocery integration |
| 11 | Stats | Cooking streak, leaderboards, challenges, shareable achievements |
| 12 | Settings | Profile, family, preferences |

## Development Commands

### Android App (Windows - run from `android/` folder)
```bash
# Build
gradlew build

# Run unit tests
gradlew test

# Run single test class
gradlew test --tests "com.rasoiai.app.ClassName"

# Run instrumented tests
gradlew connectedAndroidTest

# Lint
gradlew lint
```

### Backend (Python)
```bash
# Install dependencies
pip install -r requirements.txt

# Run server
uvicorn app.main:app --reload

# Run tests
pytest

# Run single test file
pytest tests/test_meal_plans.py -v

# Database migrations
alembic upgrade head
alembic revision --autogenerate -m "description"

# Lint
ruff check .
```

## Rules for Claude

1. **Document Output Location**: All documents generated by Claude must be saved in `docs/claude-docs/` folder by default, until manually moved by the user.

2. **Dietary Tags**: Use these standard tags: `vegetarian`, `vegan`, `jain`, `sattvic`, `halal`, `eggetarian`. Recipes can have multiple tags.

3. **Cuisine Zones**: Use `north`, `south`, `east`, `west` for regional classification.

4. **Measurements**: Support both metric and Indian traditional units (katori, chammach, glass).

5. **Offline Consideration**: Any feature design must account for offline-first behavior.

## Key Documentation

| Document | Path | Priority |
|----------|------|----------|
| Project Guide | `CLAUDE.md` | HIGH |
| Screen Wireframes | `docs/design/RasoiAI Screen Wireframes.md` | HIGH |
| Architecture Decisions | `docs/design/Android Architecture Decisions.md` | HIGH |
| Design System | `docs/design/RasoiAI Design System.md` | MEDIUM |
| Technical Design | `docs/design/RasoiAI Technical Design.md` | MEDIUM |
| Requirements | `docs/requirements/RasoiAI Requirements.md` | LOW |
| Continue Prompt | `docs/CONTINUE_PROMPT.md` | Reference |
