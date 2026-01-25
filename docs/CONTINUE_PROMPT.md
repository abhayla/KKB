# Continuation Prompt for RasoiAI Project

Use this prompt to start a new conversation/context and continue the project from where we left off.

---

## PROMPT TO USE:

```
I am building **RasoiAI** - an AI-powered meal planning app for Indian families.

## Project Status

Android project is FULLY SET UP with infrastructure. Ready for feature development.

| Phase | Status | Document |
|-------|--------|----------|
| Requirements | ✅ Complete | `docs/requirements/RasoiAI Requirements.md` |
| Technical Design | ✅ Complete | `docs/design/RasoiAI Technical Design.md` |
| Architecture Decisions | ✅ Complete | `docs/design/Android Architecture Decisions.md` |
| Design System | ✅ Complete | `docs/design/RasoiAI Design System.md` |
| Screen Wireframes | ✅ Complete | `docs/design/RasoiAI Screen Wireframes.md` |
| Android Project Setup | ✅ Complete | `android/` folder |
| Pre-Dev Infrastructure | ✅ Complete | CI/CD, Testing, Firebase, Logging |
| Feature Development | ⏳ Next Step | Auth, Onboarding, Home screens |

## Infrastructure Already Set Up

| Category | Status | Details |
|----------|--------|---------|
| CI/CD | ✅ | GitHub Actions (`android-ci.yml`) |
| Firebase | ✅ | Plugins configured (google-services, crashlytics) |
| Logging | ✅ | Timber in `RasoiAIApplication` |
| Background Sync | ✅ | WorkManager + `SyncWorker` |
| Network Security | ✅ | `network_security_config.xml` |
| Gradle Wrapper | ✅ | `gradlew` / `gradlew.bat` |
| Test Infrastructure | ✅ | Sample tests in app, domain, data modules |

## Key Documents to Read

Please read these documents in order:

1. **CLAUDE.md** (root) - Project overview, architecture summary, all key decisions
2. **Screen Wireframes** - All 12 approved screens with navigation flow
3. **Architecture Decisions** - Kotlin/Compose setup, dependencies, patterns

## Key Decisions Made

| Decision | Choice |
|----------|--------|
| Language | English only (no Hindi) |
| Auth | Google OAuth only (no Phone OTP) |
| Meal Types | 4 types: Breakfast, Lunch, Dinner, Snacks |
| Recipes per Meal | Multiple recipes allowed |
| Recipe Actions | Individual swap/lock per recipe |
| Recipe Detail | Tabs for Ingredients/Instructions |
| Favorites Layout | 2-column grid with reorder |
| Pantry | Expiry tracking with category-based shelf life |
| Gamification | Leaderboards, shareable achievements, challenges |

## Technology Stack

| Layer | Technology |
|-------|------------|
| Platform | Android Native (Kotlin + Jetpack Compose) |
| Min SDK | API 24 (Android 7.0) |
| DI | Hilt + KSP |
| Navigation | Navigation Compose |
| State | StateFlow + Single UiState pattern |
| Database | Room |
| Network | Retrofit |
| Build | Kotlin DSL + Version Catalog (TOML) |
| Modularization | By-Layer (app, core, data, domain) |

## Design System

| Element | Value |
|---------|-------|
| Primary Color | Orange `#FF6838` |
| Secondary Color | Green `#5A822B` |
| Background | Cream `#FDFAF4` |
| Typography | System Default (Roboto) |
| Dark Mode | System-follow (auto-switch) |
| Shapes | Rounded (8dp / 16dp / 24dp) |

## Your Task

**Start feature development** - implement screens in this order:
1. Splash Screen (with SplashScreen API for Android 12+)
2. Auth Screen (Google OAuth)
3. Onboarding Flow (5 steps)
4. Home Screen (meal calendar, recipe cards)

## Before You Start

1. **Firebase Setup** - User needs to create Firebase project and add `google-services.json` to `android/app/`
2. **Release Signing** - Configure keystore when ready for release builds

## Working Directory
Project root: `D:/Abhay/VibeCoding/KKB`

Start by reading CLAUDE.md and the Screen Wireframes doc, then implement the first feature.
```

---

## QUICK START PROMPT (Shorter Version):

```
I'm building **RasoiAI** - an AI meal planning app for Indian families.

**Android project is FULLY SET UP** with:
- Multi-module architecture (app, core, data, domain)
- Hilt DI, Room, Retrofit, Navigation Compose
- CI/CD (GitHub Actions), Timber logging, WorkManager
- Test infrastructure with sample tests
- Firebase plugins configured

**Read these docs**:
- `CLAUDE.md` - Project overview & all key decisions
- `docs/design/RasoiAI Screen Wireframes.md` - All 12 approved screens
- `docs/design/Android Architecture Decisions.md` - Tech stack & patterns

**Key Decisions**: English only, Google OAuth only, 4 meal types with multiple recipes, individual swap/lock, tabs in recipe detail, 2-column favorites, expiry tracking in pantry, leaderboards & challenges.

**Next Step**: Start feature development - Splash, Auth, Onboarding, then Home screen.

**Note**: Firebase project needs to be created and `google-services.json` added before running the app.

Start by reading the docs, then help me implement the first feature.
```

---

## FILES TO REFERENCE:

| File | Path | Priority | Description |
|------|------|----------|-------------|
| Project Guide | `CLAUDE.md` | **HIGH** | Project overview, all summaries & decisions |
| Screen Wireframes | `docs/design/RasoiAI Screen Wireframes.md` | **HIGH** | All 12 approved screens + navigation flow |
| Architecture Decisions | `docs/design/Android Architecture Decisions.md` | **HIGH** | Tech stack, code patterns |
| Design System | `docs/design/RasoiAI Design System.md` | MEDIUM | Colors, typography, Theme.kt code |
| Technical Design | `docs/design/RasoiAI Technical Design.md` | MEDIUM | Database schema, APIs (~2100 lines) |
| Requirements | `docs/requirements/RasoiAI Requirements.md` | LOW | Full PRD with features |
| Ollie Research | `docs/research/Ollie App Research.md` | LOW | Reference only (~1900 lines) |

---

## PROJECT STATUS:

| Phase | Status | Notes |
|-------|--------|-------|
| Ollie.ai Research | ✅ Complete | Reference material |
| RasoiAI Requirements | ✅ Complete | India-specific PRD |
| Technical Design | ✅ Complete | Architecture, DB, APIs |
| Architecture Decisions | ✅ Complete | Kotlin, Hilt, KSP, Compose |
| Design System | ✅ Complete | Colors, typography, theme code |
| Screen Wireframes | ✅ Complete | 12 screens approved (v2.0) |
| Android Project Setup | ✅ Complete | Multi-module, Gradle, Hilt, Theme |
| Pre-Dev Infrastructure | ✅ Complete | CI/CD, Testing, Firebase, Logging |
| **Feature Development** | ⏳ **Next Step** | Splash, Auth, Onboarding, Home |

## INFRASTRUCTURE SETUP (Complete):

| Category | File/Location | Description |
|----------|---------------|-------------|
| CI/CD | `.github/workflows/android-ci.yml` | Build, test, lint on push/PR |
| Firebase | `android/app/build.gradle.kts` | google-services & crashlytics plugins |
| Logging | `RasoiAIApplication.kt` | Timber initialized |
| Background Sync | `data/sync/SyncWorker.kt` | WorkManager periodic sync |
| Network Security | `res/xml/network_security_config.xml` | Cleartext blocked |
| Gradle Wrapper | `android/gradlew`, `gradlew.bat` | Build scripts |
| Tests - App | `app/src/test/.../SplashViewModelTest.kt` | ViewModel test example |
| Tests - Domain | `domain/src/test/.../GetCurrentMealPlanUseCaseTest.kt` | UseCase test example |
| Tests - Data | `data/src/test/.../ConvertersTest.kt` | Room converter test |

## BEFORE FEATURE DEVELOPMENT:

1. **Firebase Setup** - Create Firebase project, download `google-services.json` to `android/app/`
2. **Release Signing** - Configure keystore in `android/app/build.gradle.kts` (placeholder exists)

---

## APPROVED SCREEN CHANGES SUMMARY:

| Screen | Key Changes from Original Design |
|--------|----------------------------------|
| All Screens | English only (Hindi removed) |
| 2. Auth | Google OAuth only (Phone OTP removed) |
| 3. Onboarding | Dropdowns for selections, dietary needs for all member types |
| 4. Home | 4 meal types (Breakfast/Lunch/Dinner/Snacks), multiple recipes per meal, individual swap/lock, refresh for selected date |
| 5. Recipe Detail | Tabs for Ingredients/Instructions |
| 8. Favorites | 2-column grid, reorder within collections, cover images, "Recently Viewed" collection |
| 9. Chat | Chat history persisted, Clear Chat option, time-based quick actions |
| 10. Pantry | Expiry tracking (category-based), grocery integration, auto-remove confirmation |
| 11. Stats | Leaderboards, shareable achievements, weekly/monthly challenges |

---

## ARCHITECTURE DECISIONS SUMMARY:

| Decision | Choice |
|----------|--------|
| Dependency Injection | Hilt |
| Annotation Processing | KSP |
| State Management | StateFlow + Single UiState Data Class |
| Navigation | Navigation Compose |
| Build Configuration | Kotlin DSL + Version Catalog (TOML) |
| Minimum SDK | API 24 (Android 7.0) |
| Testing Strategy | 70% Unit / 20% Integration / 10% UI |
| Modularization | By-Layer (app, core, data, domain) |

---

## MODULE STRUCTURE TO CREATE:

```
RasoiAI/
├── app/                          # Main application module
│   └── presentation/             # All screens & ViewModels
│       ├── navigation/
│       ├── theme/
│       ├── common/
│       └── [feature]/            # splash, auth, onboarding, home, etc.
│
├── core/                         # Shared utilities & UI components
│   ├── ui/                       # Theme, shared composables
│   ├── util/                     # Extensions, constants
│   └── network/                  # NetworkMonitor
│
├── data/                         # Data layer module
│   ├── local/                    # Room DB, DAOs, Entities
│   ├── remote/                   # Retrofit API, DTOs
│   ├── repository/               # Repository implementations
│   └── sync/                     # SyncManager, OfflineQueueManager
│
├── domain/                       # Domain layer module (pure Kotlin)
│   ├── model/                    # Domain models
│   ├── repository/               # Repository interfaces
│   └── usecase/                  # Business logic use cases
│
└── gradle/
    └── libs.versions.toml        # Centralized dependency versions
```

---

## DESIGN SYSTEM QUICK REFERENCE:

| Element | Light Mode | Dark Mode |
|---------|------------|-----------|
| Primary | `#FF6838` | `#FFB59C` |
| Secondary | `#5A822B` | `#A8D475` |
| Background | `#FDFAF4` | `#1C1B1F` |
| Surface | `#FFFFFF` | `#2B2930` |

| Token | Value |
|-------|-------|
| Typography | Roboto (System Default) |
| Spacing | 8dp grid (4, 8, 16, 24, 32, 48dp) |
| Shapes | Rounded (8dp small, 16dp medium, 24dp large) |

---

## APP SCREENS (12 Total - All Approved):

| # | Screen | Status | Key Features |
|---|--------|--------|--------------|
| 1 | Splash | ✅ | Logo, loading state |
| 2 | Auth | ✅ | Google OAuth only |
| 3 | Onboarding | ✅ | 5 steps with dropdowns |
| 4 | Home | ✅ | 4 meal types, multiple recipes, individual lock/swap |
| 5 | Recipe Detail | ✅ | Tabs (Ingredients/Instructions) |
| 6 | Cooking Mode | ✅ | Full-screen steps, timer |
| 7 | Grocery List | ✅ | Categorized, WhatsApp share |
| 8 | Favorites | ✅ | 2-column grid, reorder, Recently Viewed |
| 9 | Chat | ✅ | History, clear chat, time-based actions |
| 10 | Pantry Scan | ✅ | Expiry tracking, grocery integration |
| 11 | Stats | ✅ | Leaderboards, challenges, shareable |
| 12 | Settings | ✅ | Profile, family, preferences |

---

*Last Updated: January 2025*
*Project: RasoiAI - AI Meal Planning for Indian Families*
*Next Step: Feature Development (Splash → Auth → Onboarding → Home)*
