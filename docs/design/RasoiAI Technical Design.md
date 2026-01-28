# RasoiAI - Technical Design Document (TDD)

## Version 1.1 | January 2026

> **⚠️ Database Update (Jan 2026):** Backend database migrated from PostgreSQL/SQLAlchemy to **Firebase Firestore** for simplified deployment and real-time sync. The document below reflects the original PostgreSQL design; actual implementation uses Firestore repositories. See `backend/app/repositories/` for current implementation.

---

## Table of Contents

1. [Overview](#1-overview)
2. [System Architecture](#2-system-architecture)
3. [Android App Architecture](#3-android-app-architecture)
4. [Backend Architecture](#4-backend-architecture)
5. [Database Design](#5-database-design)
6. [API Contracts](#6-api-contracts)
7. [LLM Integration Design](#7-llm-integration-design)
8. [Offline Architecture](#8-offline-architecture)
9. [Security Design](#9-security-design)
10. [Screen Flows](#10-screen-flows)

---

## 1. Overview

### 1.1 Purpose

This document provides the technical blueprint for building RasoiAI, an AI-powered meal planning app for Indian families. It translates the PRD requirements into implementable technical specifications.

### 1.2 Scope

| Component | Technology | Status |
|-----------|------------|--------|
| Android App | Kotlin + Jetpack Compose | MVP |
| Backend API | Python (FastAPI) | MVP |
| Database | Firebase Firestore | MVP *(updated from PostgreSQL)* |
| AI/LLM | Google Gemini / Claude API | MVP |
| Auth | Firebase Auth | MVP |
| Storage | Firebase Storage | MVP |

### 1.3 Design Principles

1. **Offline-First**: Core features must work without internet
2. **Cost-Efficient**: Minimize LLM API calls through caching
3. **Scalable**: Support 100K+ users in Year 1
4. **Bilingual**: English + Hindi from day one
5. **Simple**: Minimal dependencies, clean architecture

---

## 2. System Architecture

### 2.1 High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              ANDROID CLIENT                                   │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                    RasoiAI Android App                               │    │
│  │                    Kotlin + Jetpack Compose                          │    │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌────────────┐  │    │
│  │  │Presentation │  │   Domain    │  │    Data     │  │   Local    │  │    │
│  │  │   Layer     │  │   Layer     │  │   Layer     │  │  Storage   │  │    │
│  │  │ (Compose UI)│  │ (UseCases)  │  │(Repositories)│ │  (Room)    │  │    │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └────────────┘  │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                    │                                          │
│                                    │ HTTPS (REST API)                        │
└────────────────────────────────────┼────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CLOUD SERVICES                                   │
│                                                                               │
│  ┌──────────────────┐     ┌─────────────────────────────────────────────┐   │
│  │  Firebase Auth   │     │              API Server                      │   │
│  │  ┌────────────┐  │     │           Python FastAPI                     │   │
│  │  │Google OAuth│  │     │  ┌─────────────────────────────────────┐    │   │
│  │  │ Phone OTP  │  │     │  │         API Gateway Layer           │    │   │
│  │  └────────────┘  │     │  │   Rate Limiting | Auth | Routing    │    │   │
│  └──────────────────┘     │  └─────────────────────────────────────┘    │   │
│           │               │                    │                         │   │
│           │ JWT Token     │  ┌─────────────────┼─────────────────┐      │   │
│           └──────────────►│  │                 │                 │      │   │
│                           │  ▼                 ▼                 ▼      │   │
│                           │ ┌──────┐      ┌──────┐         ┌──────┐    │   │
│                           │ │ User │      │ Meal │         │Recipe│    │   │
│                           │ │ Svc  │      │ Svc  │         │ Svc  │    │   │
│                           │ └──────┘      └──────┘         └──────┘    │   │
│                           │                    │                        │   │
│                           │  ┌─────────────────┼─────────────────┐     │   │
│                           │  │                 │                 │     │   │
│                           │  ▼                 ▼                 ▼     │   │
│                           │ ┌──────┐      ┌──────┐         ┌──────┐   │   │
│                           │ │Grocry│      │Festvl│         │Notif │   │   │
│                           │ │ Svc  │      │ Svc  │         │ Svc  │   │   │
│                           │ └──────┘      └──────┘         └──────┘   │   │
│                           │                    │                        │   │
│                           │  ┌─────────────────┼─────────────────┐     │   │
│                           │  │                 │                 │     │   │
│                           │  ▼                 ▼                 ▼     │   │
│                           │ ┌──────┐      ┌──────┐         ┌──────┐   │   │
│                           │ │ Chat │      │Vision│         │Gamify│   │   │
│                           │ │ Svc  │      │ Svc  │         │ Svc  │   │   │
│                           │ └──────┘      └──────┘         └──────┘   │   │
│                           └─────────────────────────────────────────────┘   │
│                                          │                                   │
│                    ┌─────────────────────┼─────────────────────┐            │
│                    │                     │                     │            │
│                    ▼                     ▼                     ▼            │
│            ┌──────────────┐      ┌──────────────┐      ┌──────────────┐    │
│            │  PostgreSQL  │      │    Redis     │      │   AWS S3     │    │
│            │  (Primary)   │      │   (Cache)    │      │  (Images)    │    │
│            └──────────────┘      └──────────────┘      └──────────────┘    │
│                                                                              │
│                    ┌─────────────────────────────────────────┐              │
│                    │            External Services            │              │
│                    │  ┌────────────┐    ┌────────────────┐  │              │
│                    │  │ Claude API │    │ Firebase FCM   │  │              │
│                    │  │   (LLM)    │    │ (Push Notifs)  │  │              │
│                    │  └────────────┘    └────────────────┘  │              │
│                    │  ┌────────────┐                        │              │
│                    │  │ Vision AI  │                        │              │
│                    │  │ (Pantry)   │                        │              │
│                    │  └────────────┘                        │              │
│                    └─────────────────────────────────────────┘              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Component Overview

| Component | Responsibility | Technology |
|-----------|---------------|------------|
| Android App | User interface, offline storage, local logic | Kotlin, Compose, Room |
| API Gateway | Rate limiting, auth validation, routing | FastAPI middleware |
| User Service | User management, preferences, family profiles | FastAPI + PostgreSQL |
| Meal Service | Meal plan generation, CRUD, swapping | FastAPI + Claude API |
| Recipe Service | Recipe storage, search, favorites, import | FastAPI + PostgreSQL |
| Grocery Service | List generation, WhatsApp formatting | FastAPI |
| Festival Service | Festival calendar, fasting logic | FastAPI + PostgreSQL |
| Notification Service | Push notifications scheduling | FastAPI + FCM |
| Chat Service | Natural language recipe modifications, Q&A | FastAPI + Claude API |
| Vision Service | Pantry scanning, ingredient recognition | FastAPI + Vision AI |
| Gamification Service | Cooking streaks, meal ratings, stats | FastAPI + PostgreSQL |

### 2.3 Technology Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Backend Language | Python (FastAPI) | Fast development, good AI/ML ecosystem, async support |
| Android Architecture | MVVM + Clean Architecture | Google recommended, testable, scalable |
| Database | PostgreSQL | Relational data, JSONB for flexibility, reliable |
| Cache | Redis | Fast, session management, rate limiting |
| LLM Provider | Claude API | Better reasoning, cost-effective, reliable |
| Vision AI | Google Cloud Vision / Claude Vision | Ingredient recognition, pantry scanning |
| Auth | Firebase Auth | Phone OTP support, Google OAuth, easy integration |
| Image Storage | AWS S3 | Scalable, CDN integration, cost-effective |

---

## 3. Android App Architecture

### 3.1 Architecture Pattern: Clean Architecture + MVVM

```
┌─────────────────────────────────────────────────────────────────────┐
│                        PRESENTATION LAYER                            │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │                    Jetpack Compose UI                        │    │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐        │    │
│  │  │Onboarding│ │   Home   │ │  Recipe  │ │ Grocery  │  ...   │    │
│  │  │ Screens  │ │ Screens  │ │ Screens  │ │ Screens  │        │    │
│  │  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘        │    │
│  │       │            │            │            │               │    │
│  │       ▼            ▼            ▼            ▼               │    │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐        │    │
│  │  │Onboarding│ │   Home   │ │  Recipe  │ │ Grocery  │        │    │
│  │  │ViewModel │ │ViewModel │ │ViewModel │ │ViewModel │        │    │
│  │  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘        │    │
│  └───────┼────────────┼────────────┼────────────┼───────────────┘    │
│          │            │            │            │                    │
│          └────────────┴─────┬──────┴────────────┘                    │
└─────────────────────────────┼────────────────────────────────────────┘
                              │
┌─────────────────────────────┼────────────────────────────────────────┐
│                        DOMAIN LAYER                                   │
│                              │                                        │
│  ┌───────────────────────────┴───────────────────────────┐           │
│  │                      USE CASES                         │           │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐      │           │
│  │  │GenerateMeal │ │  GetRecipe  │ │GetGroceryLst│ ...  │           │
│  │  │  PlanUseCase│ │  UseCase    │ │  UseCase    │      │           │
│  │  └─────────────┘ └─────────────┘ └─────────────┘      │           │
│  └───────────────────────────┬───────────────────────────┘           │
│                              │                                        │
│  ┌───────────────────────────┴───────────────────────────┐           │
│  │                    DOMAIN MODELS                       │           │
│  │   User | MealPlan | Recipe | GroceryList | Festival    │           │
│  └───────────────────────────┬───────────────────────────┘           │
│                              │                                        │
│  ┌───────────────────────────┴───────────────────────────┐           │
│  │                 REPOSITORY INTERFACES                  │           │
│  │   IUserRepository | IMealPlanRepository | IRecipeRepo  │           │
│  └───────────────────────────────────────────────────────┘           │
└──────────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────┼────────────────────────────────────────┐
│                         DATA LAYER                                    │
│                              │                                        │
│  ┌───────────────────────────┴───────────────────────────┐           │
│  │                   REPOSITORIES (Impl)                  │           │
│  │   UserRepositoryImpl | MealPlanRepositoryImpl | ...    │           │
│  └───────────────────────────┬───────────────────────────┘           │
│                              │                                        │
│           ┌──────────────────┼──────────────────┐                    │
│           │                  │                  │                    │
│           ▼                  ▼                  ▼                    │
│  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐        │
│  │   Remote Data   │ │   Local Data    │ │   Data Sync     │        │
│  │     Source      │ │    Source       │ │    Manager      │        │
│  │  (Retrofit API) │ │   (Room DB)     │ │                 │        │
│  └─────────────────┘ └─────────────────┘ └─────────────────┘        │
└──────────────────────────────────────────────────────────────────────┘
```

### 3.2 Package Structure

```
com.rasoiai.app/
├── RasoiAIApplication.kt
├── MainActivity.kt
│
├── di/                              # Dependency Injection (Hilt)
│   ├── AppModule.kt
│   ├── NetworkModule.kt
│   ├── DatabaseModule.kt
│   └── RepositoryModule.kt
│
├── data/
│   ├── local/
│   │   ├── RasoiDatabase.kt
│   │   ├── dao/
│   │   │   ├── UserDao.kt
│   │   │   ├── MealPlanDao.kt
│   │   │   ├── RecipeDao.kt
│   │   │   ├── GroceryDao.kt
│   │   │   └── FestivalDao.kt
│   │   └── entity/
│   │       ├── UserEntity.kt
│   │       ├── MealPlanEntity.kt
│   │       ├── RecipeEntity.kt
│   │       └── ...
│   │
│   ├── remote/
│   │   ├── api/
│   │   │   ├── RasoiApiService.kt
│   │   │   ├── AuthInterceptor.kt
│   │   │   └── NetworkErrorHandler.kt
│   │   └── dto/
│   │       ├── UserDto.kt
│   │       ├── MealPlanDto.kt
│   │       ├── RecipeDto.kt
│   │       └── ...
│   │
│   ├── repository/
│   │   ├── UserRepositoryImpl.kt
│   │   ├── MealPlanRepositoryImpl.kt
│   │   ├── RecipeRepositoryImpl.kt
│   │   ├── GroceryRepositoryImpl.kt
│   │   └── FestivalRepositoryImpl.kt
│   │
│   └── sync/
│       ├── SyncManager.kt
│       └── OfflineQueueManager.kt
│
├── domain/
│   ├── model/
│   │   ├── User.kt
│   │   ├── FamilyMember.kt
│   │   ├── UserPreferences.kt
│   │   ├── MealPlan.kt
│   │   ├── MealPlanItem.kt
│   │   ├── Recipe.kt
│   │   ├── Ingredient.kt
│   │   ├── GroceryList.kt
│   │   ├── GroceryItem.kt
│   │   ├── Festival.kt
│   │   ├── ChatMessage.kt
│   │   ├── PantryItem.kt
│   │   ├── MealRating.kt
│   │   └── CookingStreak.kt
│   │
│   ├── repository/
│   │   ├── IUserRepository.kt
│   │   ├── IMealPlanRepository.kt
│   │   ├── IRecipeRepository.kt
│   │   ├── IGroceryRepository.kt
│   │   ├── IFestivalRepository.kt
│   │   ├── IChatRepository.kt
│   │   ├── IPantryRepository.kt
│   │   └── IGamificationRepository.kt
│   │
│   └── usecase/
│       ├── auth/
│       │   ├── LoginWithGoogleUseCase.kt
│       │   ├── LoginWithPhoneUseCase.kt
│       │   └── LogoutUseCase.kt
│       ├── onboarding/
│       │   ├── SavePreferencesUseCase.kt
│       │   └── AddFamilyMemberUseCase.kt
│       ├── mealplan/
│       │   ├── GenerateMealPlanUseCase.kt
│       │   ├── GetCurrentMealPlanUseCase.kt
│       │   ├── SwapMealUseCase.kt
│       │   └── SkipMealUseCase.kt
│       ├── recipe/
│       │   ├── GetRecipeDetailUseCase.kt
│       │   ├── AddToFavoritesUseCase.kt
│       │   ├── ScaleRecipeUseCase.kt
│       │   └── ImportRecipeUseCase.kt
│       ├── grocery/
│       │   ├── GetGroceryListUseCase.kt
│       │   ├── ToggleGroceryItemUseCase.kt
│       │   └── FormatForWhatsAppUseCase.kt
│       ├── festival/
│       │   ├── GetUpcomingFestivalsUseCase.kt
│       │   └── ActivateFastingModeUseCase.kt
│       ├── chat/
│       │   ├── SendChatMessageUseCase.kt
│       │   └── ModifyRecipeViaChatUseCase.kt
│       ├── pantry/
│       │   ├── ScanPantryUseCase.kt
│       │   └── GetPantryItemsUseCase.kt
│       └── gamification/
│           ├── GetCookingStreakUseCase.kt
│           ├── RateMealUseCase.kt
│           └── GetCookingStatsUseCase.kt
│
├── presentation/
│   ├── navigation/
│   │   ├── RasoiNavHost.kt
│   │   ├── Screen.kt
│   │   └── NavigationUtils.kt
│   │
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Type.kt
│   │   ├── Theme.kt
│   │   └── Dimens.kt
│   │
│   ├── common/
│   │   ├── components/
│   │   │   ├── RasoiTopBar.kt
│   │   │   ├── RasoiButton.kt
│   │   │   ├── RasoiCard.kt
│   │   │   ├── LoadingIndicator.kt
│   │   │   ├── ErrorView.kt
│   │   │   ├── OfflineBanner.kt
│   │   │   └── ...
│   │   └── util/
│   │       ├── UiState.kt
│   │       └── Extensions.kt
│   │
│   ├── splash/
│   │   └── SplashScreen.kt
│   │
│   ├── auth/
│   │   ├── AuthScreen.kt
│   │   └── AuthViewModel.kt
│   │
│   ├── onboarding/
│   │   ├── OnboardingScreen.kt
│   │   ├── OnboardingViewModel.kt
│   │   └── steps/
│   │       ├── HouseholdStep.kt
│   │       ├── DietaryStep.kt
│   │       ├── CuisineStep.kt
│   │       ├── DislikesStep.kt
│   │       └── CookingTimeStep.kt
│   │
│   ├── home/
│   │   ├── HomeScreen.kt
│   │   ├── HomeViewModel.kt
│   │   └── components/
│   │       ├── WeeklyCalendar.kt
│   │       ├── DayMealsCard.kt
│   │       └── MealItemCard.kt
│   │
│   ├── recipe/
│   │   ├── RecipeDetailScreen.kt
│   │   ├── RecipeDetailViewModel.kt
│   │   ├── CookingModeScreen.kt
│   │   └── components/
│   │       ├── IngredientsList.kt
│   │       ├── InstructionStep.kt
│   │       └── NutritionCard.kt
│   │
│   ├── grocery/
│   │   ├── GroceryListScreen.kt
│   │   ├── GroceryViewModel.kt
│   │   └── components/
│   │       ├── GroceryCategory.kt
│   │       ├── GroceryItemRow.kt
│   │       └── WhatsAppShareButton.kt
│   │
│   ├── favorites/
│   │   ├── FavoritesScreen.kt
│   │   └── FavoritesViewModel.kt
│   │
│   ├── chat/
│   │   ├── ChatScreen.kt
│   │   ├── ChatViewModel.kt
│   │   └── components/
│   │       ├── ChatBubble.kt
│   │       └── ChatInput.kt
│   │
│   ├── pantry/
│   │   ├── PantryScanScreen.kt
│   │   ├── PantryViewModel.kt
│   │   └── components/
│   │       ├── CameraPreview.kt
│   │       └── DetectedIngredients.kt
│   │
│   ├── stats/
│   │   ├── StatsScreen.kt
│   │   ├── StatsViewModel.kt
│   │   └── components/
│   │       ├── StreakCard.kt
│   │       ├── CookingCalendar.kt
│   │       └── MonthlyStats.kt
│   │
│   └── settings/
│       ├── SettingsScreen.kt
│       ├── SettingsViewModel.kt
│       ├── FamilyScreen.kt
│       ├── PreferencesScreen.kt
│       └── LanguageScreen.kt
│
└── util/
    ├── Constants.kt
    ├── DateUtils.kt
    ├── StringUtils.kt
    ├── NetworkUtils.kt
    └── LocaleUtils.kt
```

### 3.3 Key Dependencies (build.gradle)

```kotlin
// Versions
kotlinVersion = "1.9.22"
composeVersion = "1.6.0"
hiltVersion = "2.50"
roomVersion = "2.6.1"
retrofitVersion = "2.9.0"

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2024.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // Hilt (Dependency Injection)
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    kapt("com.google.dagger:hilt-compiler:$hiltVersion")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Room (Local Database)
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // Retrofit (Networking)
    implementation("com.squareup.retrofit2:retrofit:$retrofitVersion")
    implementation("com.squareup.retrofit2:converter-gson:$retrofitVersion")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coil (Image Loading)
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")

    // DataStore (Preferences)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
```

### 3.4 Room Database Schema

```kotlin
@Database(
    entities = [
        UserEntity::class,
        FamilyMemberEntity::class,
        UserPreferencesEntity::class,
        MealPlanEntity::class,
        MealPlanItemEntity::class,
        RecipeEntity::class,
        RecipeIngredientEntity::class,
        RecipeStepEntity::class,
        GroceryListEntity::class,
        GroceryItemEntity::class,
        FavoriteEntity::class,
        FestivalEntity::class,
        SyncQueueEntity::class,
        ChatMessageEntity::class,
        PantryItemEntity::class,
        PantryScanEntity::class,
        MealRatingEntity::class,
        CookingStreakEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class RasoiDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun mealPlanDao(): MealPlanDao
    abstract fun recipeDao(): RecipeDao
    abstract fun groceryDao(): GroceryDao
    abstract fun festivalDao(): FestivalDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun chatDao(): ChatDao
    abstract fun pantryDao(): PantryDao
    abstract fun gamificationDao(): GamificationDao
}
```

---

## 4. Backend Architecture

### 4.1 Project Structure (Python FastAPI)

```
rasoiai-backend/
├── app/
│   ├── __init__.py
│   ├── main.py                    # FastAPI app entry point
│   ├── config.py                  # Configuration settings
│   │
│   ├── api/
│   │   ├── __init__.py
│   │   ├── deps.py                # Dependency injection
│   │   ├── v1/
│   │   │   ├── __init__.py
│   │   │   ├── router.py          # API router aggregator
│   │   │   ├── auth.py            # Auth endpoints
│   │   │   ├── users.py           # User endpoints
│   │   │   ├── meal_plans.py      # Meal plan endpoints
│   │   │   ├── recipes.py         # Recipe endpoints
│   │   │   ├── grocery.py         # Grocery endpoints
│   │   │   ├── festivals.py       # Festival endpoints
│   │   │   ├── chat.py            # Chat/AI assistant endpoints
│   │   │   ├── pantry.py          # Pantry scanning endpoints
│   │   │   └── gamification.py    # Streaks, ratings, stats endpoints
│   │   └── middleware/
│   │       ├── auth_middleware.py
│   │       ├── rate_limiter.py
│   │       └── error_handler.py
│   │
│   ├── core/
│   │   ├── __init__.py
│   │   ├── security.py            # JWT, hashing utilities
│   │   ├── firebase_auth.py       # Firebase token verification
│   │   └── exceptions.py          # Custom exceptions
│   │
│   ├── db/
│   │   ├── __init__.py
│   │   ├── database.py            # Database connection
│   │   ├── redis_client.py        # Redis connection
│   │   └── migrations/            # Alembic migrations
│   │
│   ├── models/
│   │   ├── __init__.py
│   │   ├── user.py
│   │   ├── family_member.py
│   │   ├── user_preferences.py
│   │   ├── meal_plan.py
│   │   ├── recipe.py
│   │   ├── grocery.py
│   │   ├── festival.py
│   │   ├── chat.py
│   │   ├── pantry.py
│   │   └── gamification.py
│   │
│   ├── schemas/
│   │   ├── __init__.py
│   │   ├── user.py                # Pydantic schemas
│   │   ├── meal_plan.py
│   │   ├── recipe.py
│   │   ├── grocery.py
│   │   ├── festival.py
│   │   ├── chat.py
│   │   ├── pantry.py
│   │   └── gamification.py
│   │
│   ├── services/
│   │   ├── __init__.py
│   │   ├── user_service.py
│   │   ├── meal_plan_service.py
│   │   ├── recipe_service.py
│   │   ├── grocery_service.py
│   │   ├── festival_service.py
│   │   ├── notification_service.py
│   │   ├── chat_service.py
│   │   ├── pantry_service.py
│   │   └── gamification_service.py
│   │
│   ├── ai/
│   │   ├── __init__.py
│   │   ├── llm_client.py          # Claude API client
│   │   ├── meal_planner.py        # AI meal planning logic
│   │   ├── chat_assistant.py      # Chat/conversation handler
│   │   ├── vision_client.py       # Vision AI for pantry scanning
│   │   ├── ingredient_detector.py # Ingredient recognition
│   │   ├── prompts/
│   │   │   ├── meal_plan_prompt.py
│   │   │   ├── recipe_prompt.py
│   │   │   ├── swap_prompt.py
│   │   │   └── chat_prompt.py
│   │   └── cache.py               # LLM response caching
│   │
│   └── utils/
│       ├── __init__.py
│       ├── date_utils.py
│       ├── text_utils.py
│       └── whatsapp_formatter.py
│
├── tests/
│   ├── __init__.py
│   ├── test_auth.py
│   ├── test_meal_plans.py
│   └── ...
│
├── alembic/                        # Database migrations
├── requirements.txt
├── Dockerfile
├── docker-compose.yml
└── .env.example
```

### 4.2 FastAPI Main Application

```python
# app/main.py
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.api.v1.router import api_router
from app.api.middleware.error_handler import error_handler_middleware
from app.api.middleware.rate_limiter import RateLimiterMiddleware
from app.db.database import engine
from app.config import settings

app = FastAPI(
    title="RasoiAI API",
    description="AI-powered meal planning API for Indian families",
    version="1.0.0",
)

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.ALLOWED_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Custom middleware
app.middleware("http")(error_handler_middleware)
app.add_middleware(RateLimiterMiddleware)

# Routes
app.include_router(api_router, prefix="/api/v1")

@app.get("/health")
async def health_check():
    return {"status": "healthy", "version": "1.0.0"}
```

### 4.3 Configuration

```python
# app/config.py
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    # App
    APP_NAME: str = "RasoiAI"
    DEBUG: bool = False

    # Database
    DATABASE_URL: str
    REDIS_URL: str

    # Firebase
    FIREBASE_PROJECT_ID: str

    # Claude API
    ANTHROPIC_API_KEY: str
    CLAUDE_MODEL: str = "claude-3-sonnet-20240229"

    # AWS S3
    AWS_ACCESS_KEY_ID: str
    AWS_SECRET_ACCESS_KEY: str
    S3_BUCKET_NAME: str
    S3_REGION: str = "ap-south-1"

    # JWT
    JWT_SECRET_KEY: str
    JWT_ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60 * 24 * 7  # 7 days

    # Rate Limiting
    RATE_LIMIT_REQUESTS: int = 100
    RATE_LIMIT_WINDOW: int = 60  # seconds

    class Config:
        env_file = ".env"

settings = Settings()
```

---

## 5. Database Design

### 5.1 Entity Relationship Diagram (ERD)

```
┌─────────────────────┐       ┌─────────────────────┐
│       USERS         │       │   FAMILY_MEMBERS    │
├─────────────────────┤       ├─────────────────────┤
│ PK user_id (UUID)   │───┐   │ PK member_id (UUID) │
│    phone_number     │   │   │ FK user_id          │◄──┐
│    email            │   │   │    name             │   │
│    name             │   │   │    member_type      │   │
│    auth_provider    │   │   │    age              │   │
│    preferred_lang   │   └──►│    dietary_restrs   │   │
│    created_at       │       │    health_conditions│   │
└─────────┬───────────┘       └─────────────────────┘   │
          │                                              │
          │ 1:1                                          │
          ▼                                              │
┌─────────────────────┐                                  │
│  USER_PREFERENCES   │                                  │
├─────────────────────┤                                  │
│ PK preference_id    │                                  │
│ FK user_id          │◄─────────────────────────────────┘
│    household_size   │
│    primary_diet     │
│    special_diets[]  │
│    cuisine_zones[]  │
│    spice_level      │
│    weekday_time     │
│    weekend_time     │
│    busy_days[]      │
│    kitchen_equip[]  │
│    dislikes[]       │
└─────────────────────┘

┌─────────────────────┐       ┌─────────────────────┐
│     MEAL_PLANS      │       │   MEAL_PLAN_ITEMS   │
├─────────────────────┤       ├─────────────────────┤
│ PK plan_id (UUID)   │───┐   │ PK item_id (UUID)   │
│ FK user_id          │   │   │ FK plan_id          │◄──┐
│    week_start_date  │   │   │ FK recipe_id        │   │
│    week_end_date    │   └──►│    day_of_week      │   │
│    status           │       │    meal_type        │   │
│    created_at       │       │    status           │   │
└─────────────────────┘       │    is_locked        │   │
                              └─────────────────────┘   │
                                        │               │
                                        │ N:1           │
                                        ▼               │
┌─────────────────────┐       ┌─────────────────────┐   │
│      RECIPES        │       │ RECIPE_INGREDIENTS  │   │
├─────────────────────┤       ├─────────────────────┤   │
│ PK recipe_id (UUID) │◄──────│ PK id (UUID)        │   │
│    name_en          │       │ FK recipe_id        │   │
│    name_hi          │       │    ingredient_en    │   │
│    description_en   │       │    ingredient_hi    │   │
│    description_hi   │       │    quantity         │   │
│    cuisine_zone     │       │    unit             │   │
│    cuisine_type     │       │    category         │   │
│    prep_time        │       │    is_optional      │   │
│    cook_time        │       └─────────────────────┘   │
│    servings         │                                 │
│    difficulty       │       ┌─────────────────────┐   │
│    dietary_tags[]   │       │    RECIPE_STEPS     │   │
│    meal_type[]      │       ├─────────────────────┤   │
│    festival_tags[]  │       │ PK step_id (UUID)   │   │
│    calories         │       │ FK recipe_id        │   │
│    protein          │       │    step_number      │   │
│    carbs            │       │    instruction_en   │   │
│    fat              │       │    instruction_hi   │   │
│    image_url        │       │    duration_mins    │   │
└─────────────────────┘       └─────────────────────┘   │
          │                                             │
          │                                             │
          ▼                                             │
┌─────────────────────┐       ┌─────────────────────┐   │
│   USER_FAVORITES    │       │   GROCERY_LISTS     │   │
├─────────────────────┤       ├─────────────────────┤   │
│ PK favorite_id      │       │ PK list_id (UUID)   │───┘
│ FK user_id          │       │ FK user_id          │
│ FK recipe_id        │       │ FK plan_id          │
│    collection_name  │       │    created_at       │
│    created_at       │       └─────────┬───────────┘
└─────────────────────┘                 │
                                        │ 1:N
                                        ▼
                              ┌─────────────────────┐
                              │  GROCERY_ITEMS      │
                              ├─────────────────────┤
                              │ PK item_id (UUID)   │
                              │ FK list_id          │
                              │    ingredient_en    │
                              │    ingredient_hi    │
                              │    quantity         │
                              │    unit             │
                              │    category         │
                              │    is_checked       │
                              │    is_manual        │
                              └─────────────────────┘

┌─────────────────────┐
│     FESTIVALS       │
├─────────────────────┤
│ PK festival_id      │
│    name_en          │
│    name_hi          │
│    festival_type    │
│    regions[]        │
│    date_type        │
│    fixed_date       │
│    requires_fasting │
│    fasting_type     │
│    food_focus       │
│    recipe_tags[]    │
└─────────────────────┘

┌─────────────────────┐       ┌─────────────────────┐
│   CHAT_MESSAGES     │       │    PANTRY_ITEMS     │
├─────────────────────┤       ├─────────────────────┤
│ PK message_id (UUID)│       │ PK item_id (UUID)   │
│ FK user_id          │       │ FK user_id          │
│    role (user/asst) │       │    ingredient_en    │
│    content          │       │    ingredient_hi    │
│    context_json     │       │    quantity         │
│    actions_json     │       │    added_date       │
│    created_at       │       │    expiry_estimate  │
└─────────────────────┘       │    source (scan/    │
                              │            manual)  │
                              └─────────────────────┘

┌─────────────────────┐       ┌─────────────────────┐
│    MEAL_RATINGS     │       │   COOKING_STREAKS   │
├─────────────────────┤       ├─────────────────────┤
│ PK rating_id (UUID) │       │ PK streak_id (UUID) │
│ FK user_id          │       │ FK user_id          │
│ FK recipe_id        │       │    current_streak   │
│ FK meal_plan_item_id│       │    longest_streak   │
│    rating (1-5)     │       │    last_cooked_date │
│    feedback         │       │    updated_at       │
│    cooked_date      │       └─────────────────────┘
│    created_at       │
└─────────────────────┘

┌─────────────────────┐
│   PANTRY_SCANS      │
├─────────────────────┤
│ PK scan_id (UUID)   │
│ FK user_id          │
│    image_url        │
│    detected_items[] │
│    confidence_scores│
│    created_at       │
└─────────────────────┘
```

### 5.2 SQLAlchemy Models

```python
# app/models/user.py
from sqlalchemy import Column, String, Enum, DateTime
from sqlalchemy.dialects.postgresql import UUID, JSONB
from sqlalchemy.orm import relationship
import uuid
from datetime import datetime
from app.db.database import Base

class User(Base):
    __tablename__ = "users"

    user_id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    phone_number = Column(String(15), unique=True, nullable=True)
    email = Column(String(255), unique=True, nullable=True)
    name = Column(String(100), nullable=False)
    auth_provider = Column(Enum('google', 'phone', 'email', name='auth_provider_enum'), nullable=False)
    preferred_language = Column(Enum('en', 'hi', name='language_enum'), default='en')
    firebase_uid = Column(String(128), unique=True, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    # Relationships
    preferences = relationship("UserPreferences", back_populates="user", uselist=False)
    family_members = relationship("FamilyMember", back_populates="user")
    meal_plans = relationship("MealPlan", back_populates="user")
    favorites = relationship("UserFavorite", back_populates="user")


# app/models/user_preferences.py
class UserPreferences(Base):
    __tablename__ = "user_preferences"

    preference_id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id = Column(UUID(as_uuid=True), ForeignKey("users.user_id", ondelete="CASCADE"), unique=True)
    household_size = Column(Integer, nullable=False, default=2)
    primary_diet = Column(Enum('vegetarian', 'eggetarian', 'non_vegetarian', name='diet_enum'), nullable=False)
    special_diets = Column(JSONB, default=[])  # ['jain', 'sattvic', 'halal']
    cuisine_zones = Column(JSONB, default=[])  # ['north', 'south', 'east', 'west']
    spice_level = Column(Enum('mild', 'medium', 'spicy', 'very_spicy', name='spice_enum'), default='medium')
    weekday_cooking_time = Column(Integer, default=30)
    weekend_cooking_time = Column(Integer, default=60)
    busy_days = Column(JSONB, default=[])  # [1, 3, 5] for Mon, Wed, Fri
    kitchen_equipment = Column(JSONB, default=['gas_stove', 'pressure_cooker', 'mixer'])
    disliked_ingredients = Column(JSONB, default=[])

    user = relationship("User", back_populates="preferences")


# app/models/recipe.py
class Recipe(Base):
    __tablename__ = "recipes"

    recipe_id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    name_en = Column(String(200), nullable=False)
    name_hi = Column(String(200))
    description_en = Column(Text)
    description_hi = Column(Text)
    cuisine_zone = Column(Enum('north', 'south', 'east', 'west', 'fusion', name='zone_enum'))
    cuisine_type = Column(String(50))  # 'punjabi', 'tamil', etc.
    prep_time = Column(Integer, nullable=False)
    cook_time = Column(Integer, nullable=False)
    servings = Column(Integer, default=4)
    difficulty = Column(Enum('easy', 'medium', 'hard', name='difficulty_enum'), default='easy')
    dietary_tags = Column(JSONB, default=[])  # ['vegetarian', 'jain', 'fasting']
    meal_type = Column(JSONB, default=[])  # ['breakfast', 'lunch', 'dinner']
    season_tags = Column(JSONB, default=[])  # ['summer', 'monsoon', 'winter']
    festival_tags = Column(JSONB, default=[])  # ['navratri', 'diwali']
    calories = Column(Integer)
    protein = Column(Numeric(5, 2))
    carbs = Column(Numeric(5, 2))
    fat = Column(Numeric(5, 2))
    image_url = Column(String(500))
    is_ai_generated = Column(Boolean, default=False)
    created_at = Column(DateTime, default=datetime.utcnow)

    ingredients = relationship("RecipeIngredient", back_populates="recipe")
    steps = relationship("RecipeStep", back_populates="recipe", order_by="RecipeStep.step_number")
```

---

## 6. API Contracts

### 6.1 Authentication APIs

#### POST /api/v1/auth/firebase
Verify Firebase token and create/login user.

**Request:**
```json
{
  "firebase_token": "eyJhbGciOiJS...",
  "provider": "google" | "phone"
}
```

**Response (200):**
```json
{
  "access_token": "eyJhbGciOiJS...",
  "token_type": "bearer",
  "user": {
    "user_id": "uuid",
    "name": "Priya Sharma",
    "email": "priya@example.com",
    "phone_number": "+919876543210",
    "preferred_language": "hi",
    "is_onboarded": false
  }
}
```

### 6.2 User APIs

#### GET /api/v1/users/me
Get current user profile.

**Response (200):**
```json
{
  "user_id": "uuid",
  "name": "Priya Sharma",
  "email": "priya@example.com",
  "phone_number": "+919876543210",
  "preferred_language": "hi",
  "created_at": "2025-01-15T10:30:00Z"
}
```

#### PUT /api/v1/users/preferences
Update user preferences.

**Request:**
```json
{
  "household_size": 4,
  "primary_diet": "vegetarian",
  "special_diets": ["sattvic"],
  "cuisine_zones": ["north", "south"],
  "spice_level": "medium",
  "weekday_cooking_time": 30,
  "weekend_cooking_time": 60,
  "busy_days": [1, 3, 5],
  "kitchen_equipment": ["gas_stove", "pressure_cooker", "mixer", "microwave"],
  "disliked_ingredients": ["karela", "lauki"]
}
```

#### POST /api/v1/users/family
Add family member.

**Request:**
```json
{
  "name": "Dadi",
  "member_type": "senior",
  "age": 72,
  "dietary_restrictions": ["low_oil", "no_spicy"],
  "health_conditions": ["diabetic", "high_bp"]
}
```

### 6.3 Meal Plan APIs

#### POST /api/v1/meal-plans/generate
Generate a new weekly meal plan.

**Request:**
```json
{
  "week_start_date": "2025-01-20",
  "include_festivals": true,
  "fasting_mode": null
}
```

**Response (200):**
```json
{
  "plan_id": "uuid",
  "week_start_date": "2025-01-20",
  "week_end_date": "2025-01-26",
  "status": "active",
  "items": [
    {
      "item_id": "uuid",
      "day_of_week": 1,
      "meal_type": "breakfast",
      "recipe": {
        "recipe_id": "uuid",
        "name_en": "Poha",
        "name_hi": "पोहा",
        "cuisine_zone": "west",
        "prep_time": 5,
        "cook_time": 15,
        "total_time": 20,
        "dietary_tags": ["vegetarian", "gluten_free"],
        "calories": 280,
        "image_url": "https://..."
      },
      "status": "planned",
      "is_locked": false
    },
    // ... more items for all 7 days x 3 meals
  ],
  "festivals_this_week": [
    {
      "festival_id": "uuid",
      "name_en": "Republic Day",
      "name_hi": "गणतंत्र दिवस",
      "date": "2025-01-26"
    }
  ]
}
```

#### POST /api/v1/meal-plans/{plan_id}/items/{item_id}/swap
Get swap suggestions for a meal.

**Request:**
```json
{
  "reason": "want_different" | "no_time" | "missing_ingredients"
}
```

**Response (200):**
```json
{
  "suggestions": [
    {
      "recipe_id": "uuid",
      "name_en": "Upma",
      "name_hi": "उपमा",
      "total_time": 20,
      "match_reason": "Same cooking time, vegetarian"
    },
    {
      "recipe_id": "uuid",
      "name_en": "Idli",
      "name_hi": "इडली",
      "total_time": 25,
      "match_reason": "South Indian breakfast, healthy"
    }
  ]
}
```

#### PUT /api/v1/meal-plans/{plan_id}/items/{item_id}
Update a meal plan item (swap, skip, lock).

**Request:**
```json
{
  "recipe_id": "new-recipe-uuid",  // for swap
  "status": "skipped",             // or "planned"
  "is_locked": true
}
```

### 6.4 Recipe APIs

#### GET /api/v1/recipes/{recipe_id}
Get full recipe details.

**Response (200):**
```json
{
  "recipe_id": "uuid",
  "name_en": "Dal Tadka",
  "name_hi": "दाल तड़का",
  "description_en": "A classic North Indian comfort food...",
  "description_hi": "उत्तर भारतीय क्लासिक आरामदायक व्यंजन...",
  "cuisine_zone": "north",
  "cuisine_type": "punjabi",
  "prep_time": 10,
  "cook_time": 30,
  "total_time": 40,
  "servings": 4,
  "difficulty": "easy",
  "dietary_tags": ["vegetarian", "gluten_free", "high_protein"],
  "meal_type": ["lunch", "dinner"],
  "calories": 320,
  "protein": 15.5,
  "carbs": 45.2,
  "fat": 8.3,
  "image_url": "https://...",
  "ingredients": [
    {
      "ingredient_en": "Toor dal (split pigeon peas)",
      "ingredient_hi": "तूर दाल",
      "quantity": 1,
      "unit": "cup",
      "category": "grocery",
      "is_optional": false
    },
    {
      "ingredient_en": "Onion",
      "ingredient_hi": "प्याज़",
      "quantity": 1,
      "unit": "medium",
      "category": "vegetable",
      "is_optional": false
    }
    // ... more ingredients
  ],
  "steps": [
    {
      "step_number": 1,
      "instruction_en": "Wash and soak toor dal for 30 minutes. Pressure cook with turmeric and salt for 3 whistles.",
      "instruction_hi": "तूर दाल को धोकर 30 मिनट भिगोएं। हल्दी और नमक के साथ 3 सीटी तक प्रेशर कुक करें।",
      "duration_minutes": 20
    },
    {
      "step_number": 2,
      "instruction_en": "Heat ghee in a pan. Add cumin seeds and let them splutter.",
      "instruction_hi": "एक पैन में घी गरम करें। जीरा डालें और चटकने दें।",
      "duration_minutes": 2
    }
    // ... more steps
  ]
}
```

#### GET /api/v1/recipes/{recipe_id}/scale?servings=6
Get scaled ingredients.

**Response (200):**
```json
{
  "recipe_id": "uuid",
  "original_servings": 4,
  "scaled_servings": 6,
  "scale_factor": 1.5,
  "ingredients": [
    {
      "ingredient_en": "Toor dal",
      "ingredient_hi": "तूर दाल",
      "original_quantity": 1,
      "scaled_quantity": 1.5,
      "unit": "cup"
    }
    // ... scaled ingredients
  ]
}
```

### 6.5 Grocery APIs

#### GET /api/v1/grocery
Get current grocery list.

**Response (200):**
```json
{
  "list_id": "uuid",
  "plan_id": "uuid",
  "total_items": 23,
  "checked_items": 5,
  "categories": [
    {
      "category": "vegetable",
      "category_hi": "सब्ज़ी",
      "items": [
        {
          "item_id": "uuid",
          "ingredient_en": "Onion",
          "ingredient_hi": "प्याज़",
          "quantity": 1,
          "unit": "kg",
          "is_checked": false
        },
        {
          "item_id": "uuid",
          "ingredient_en": "Tomato",
          "ingredient_hi": "टमाटर",
          "quantity": 500,
          "unit": "gram",
          "is_checked": true
        }
      ]
    },
    {
      "category": "dairy",
      "category_hi": "दूध/दही",
      "items": [...]
    }
    // ... more categories
  ]
}
```

#### GET /api/v1/grocery/whatsapp
Get WhatsApp-formatted grocery list.

**Response (200):**
```json
{
  "formatted_text": "🛒 *RasoiAI Grocery List*\nWeek: Jan 20-26\n\n*🥬 सब्ज़ी (8 items)*\n• प्याज़/Onion - 1 kg\n• टमाटर/Tomato - 500g\n...\n\n_Generated by RasoiAI_ 🍳",
  "whatsapp_url": "https://wa.me/?text=..."
}
```

### 6.6 Festival APIs

#### GET /api/v1/festivals/upcoming
Get upcoming festivals.

**Response (200):**
```json
{
  "festivals": [
    {
      "festival_id": "uuid",
      "name_en": "Makar Sankranti",
      "name_hi": "मकर संक्रांति",
      "date": "2025-01-14",
      "days_until": 5,
      "festival_type": "hindu",
      "requires_fasting": false,
      "food_focus": "Til-gur sweets, khichdi",
      "suggested_recipes_count": 12
    },
    {
      "festival_id": "uuid",
      "name_en": "Republic Day",
      "name_hi": "गणतंत्र दिवस",
      "date": "2025-01-26",
      "days_until": 17,
      "festival_type": "national",
      "requires_fasting": false,
      "food_focus": "Tricolor themed foods",
      "suggested_recipes_count": 8
    }
  ]
}
```

#### POST /api/v1/fasting/activate
Activate fasting mode.

**Request:**
```json
{
  "fasting_type": "navratri",
  "start_date": "2025-10-03",
  "end_date": "2025-10-11"
}
```

### 6.7 Chat APIs

#### POST /api/v1/chat/message
Send a message to the AI assistant.

**Request:**
```json
{
  "message": "Make today's dinner less spicy",
  "context": {
    "meal_plan_id": "uuid",
    "recipe_id": "uuid"
  }
}
```

**Response (200):**
```json
{
  "message_id": "uuid",
  "response": "I've updated today's dinner to use less chili. The new version uses mild spices and yogurt to balance the flavors.",
  "actions_taken": [
    {
      "action": "recipe_modified",
      "recipe_id": "uuid",
      "changes": ["Reduced chili from 2 tsp to 1/2 tsp", "Added 1/4 cup yogurt"]
    }
  ],
  "suggestions": [
    "Would you like me to update the grocery list?",
    "Should I make this a preference for future meals?"
  ]
}
```

#### GET /api/v1/chat/history
Get chat history.

**Response (200):**
```json
{
  "messages": [
    {
      "message_id": "uuid",
      "role": "user",
      "content": "Make today's dinner less spicy",
      "timestamp": "2025-01-20T18:30:00Z"
    },
    {
      "message_id": "uuid",
      "role": "assistant",
      "content": "I've updated today's dinner...",
      "timestamp": "2025-01-20T18:30:02Z"
    }
  ]
}
```

### 6.8 Pantry APIs

#### POST /api/v1/pantry/scan
Scan pantry image and detect ingredients.

**Request:**
```
Content-Type: multipart/form-data
image: <binary image data>
```

**Response (200):**
```json
{
  "scan_id": "uuid",
  "detected_ingredients": [
    {
      "ingredient_id": "uuid",
      "name_en": "Tomatoes",
      "name_hi": "टमाटर",
      "confidence": 0.95,
      "quantity_estimate": "4-5 pieces"
    },
    {
      "ingredient_id": "uuid",
      "name_en": "Onions",
      "name_hi": "प्याज",
      "confidence": 0.92,
      "quantity_estimate": "3 pieces"
    },
    {
      "ingredient_id": "uuid",
      "name_en": "Coriander",
      "name_hi": "धनिया",
      "confidence": 0.88,
      "quantity_estimate": "1 bunch"
    }
  ],
  "suggested_recipes_count": 8
}
```

#### GET /api/v1/pantry/items
Get saved pantry items.

**Response (200):**
```json
{
  "pantry_items": [
    {
      "item_id": "uuid",
      "ingredient_id": "uuid",
      "name_en": "Rice",
      "name_hi": "चावल",
      "added_date": "2025-01-18",
      "expiry_estimate": "2025-03-18"
    }
  ]
}
```

#### GET /api/v1/pantry/recipes
Get recipes using pantry ingredients.

**Response (200):**
```json
{
  "recipes": [
    {
      "recipe_id": "uuid",
      "name_en": "Tomato Rice",
      "name_hi": "टमाटर चावल",
      "pantry_match_percentage": 85,
      "missing_ingredients": ["curry_leaves"],
      "total_time": 25
    }
  ]
}
```

### 6.9 Gamification APIs

#### GET /api/v1/stats/streak
Get current cooking streak.

**Response (200):**
```json
{
  "current_streak": 12,
  "longest_streak": 23,
  "last_cooked_date": "2025-01-20",
  "streak_status": "active"
}
```

#### POST /api/v1/meals/{meal_id}/rate
Rate a cooked meal.

**Request:**
```json
{
  "rating": 4,
  "feedback": "Loved it, but slightly too salty",
  "cooked_date": "2025-01-20"
}
```

**Response (200):**
```json
{
  "rating_id": "uuid",
  "streak_updated": true,
  "new_streak": 13,
  "message": "Great! Your streak is now 13 days!"
}
```

#### GET /api/v1/stats/monthly
Get monthly cooking stats.

**Response (200):**
```json
{
  "month": "2025-01",
  "meals_cooked": 45,
  "new_recipes_tried": 12,
  "favorite_cuisine": "north",
  "average_rating": 4.2,
  "cooking_calendar": [
    {
      "date": "2025-01-01",
      "meals_cooked": ["breakfast", "dinner"],
      "rating_avg": 4.5
    }
  ]
}
```

#### GET /api/v1/recipes/{recipe_id}/import
Import recipe from URL.

**Request:**
```json
{
  "url": "https://example.com/recipe/dal-makhani"
}
```

**Response (200):**
```json
{
  "recipe_id": "uuid",
  "name_en": "Dal Makhani",
  "name_hi": "दाल मखनी",
  "source_url": "https://example.com/recipe/dal-makhani",
  "status": "imported",
  "ingredients_parsed": 12,
  "instructions_parsed": 8
}
```

---

## 7. LLM Integration Design

### 7.1 Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        LLM Integration Flow                          │
└─────────────────────────────────────────────────────────────────────┘

    User Request                                              Response
         │                                                        ▲
         ▼                                                        │
┌─────────────────┐                                    ┌─────────────────┐
│  Meal Service   │                                    │  Format Output  │
│                 │                                    │                 │
│ • Validate req  │                                    │ • Parse JSON    │
│ • Load prefs    │                                    │ • Validate      │
│ • Check cache   │                                    │ • Store recipes │
└────────┬────────┘                                    └────────▲────────┘
         │                                                      │
         │ Cache Miss                                           │
         ▼                                                      │
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│ Prompt Builder  │────►│   Claude API    │────►│ Response Parser │
│                 │     │                 │     │                 │
│ • User prefs    │     │ claude-3-sonnet │     │ • Extract meals │
│ • Family data   │     │                 │     │ • Extract recipes│
│ • Festivals     │     │                 │     │ • Calc nutrition │
│ • Constraints   │     │                 │     │                 │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

### 7.2 Meal Plan Generation Prompt

```python
# app/ai/prompts/meal_plan_prompt.py

MEAL_PLAN_SYSTEM_PROMPT = """You are RasoiAI, an expert Indian meal planning assistant.
You create personalized weekly meal plans for Indian families.

IMPORTANT RULES:
1. Generate authentic Indian recipes from the specified cuisine zones
2. Strictly follow dietary restrictions (vegetarian, Jain, fasting, etc.)
3. Never include disliked ingredients
4. Respect cooking time constraints for each day
5. Ensure variety - no same protein on consecutive days
6. Reuse ingredients across recipes to minimize waste
7. Include festival-appropriate recipes when applicable
8. Provide recipes in both English and Hindi

OUTPUT FORMAT: Return valid JSON only, no markdown."""

def build_meal_plan_prompt(
    preferences: UserPreferences,
    family_members: list[FamilyMember],
    week_start: date,
    festivals: list[Festival],
    fasting_mode: Optional[str] = None
) -> str:

    prompt = f"""Generate a 7-day meal plan for an Indian family.

FAMILY PROFILE:
- Household size: {preferences.household_size}
- Primary diet: {preferences.primary_diet}
- Special diets: {', '.join(preferences.special_diets) or 'None'}
- Preferred cuisines: {', '.join(preferences.cuisine_zones)}
- Spice level: {preferences.spice_level}
- Disliked ingredients: {', '.join(preferences.disliked_ingredients) or 'None'}

FAMILY MEMBERS:
{format_family_members(family_members)}

TIME CONSTRAINTS:
- Weekday cooking time: {preferences.weekday_cooking_time} minutes max
- Weekend cooking time: {preferences.weekend_cooking_time} minutes max
- Busy days (quick meals only): {format_busy_days(preferences.busy_days)}

WEEK DETAILS:
- Start date: {week_start.strftime('%Y-%m-%d')} ({week_start.strftime('%A')})
- End date: {(week_start + timedelta(days=6)).strftime('%Y-%m-%d')}

FESTIVALS THIS WEEK:
{format_festivals(festivals) or 'None'}

FASTING MODE: {fasting_mode or 'Not active'}

Generate a complete meal plan with breakfast, lunch, and dinner for each day.
For each recipe, provide:
- name_en, name_hi
- cuisine_zone, cuisine_type
- prep_time, cook_time (in minutes)
- servings (for {preferences.household_size} people)
- dietary_tags
- brief description
- ingredients list with Hindi names
- step-by-step instructions
- estimated calories, protein, carbs, fat

Return as JSON with structure:
{{
  "days": [
    {{
      "day": 1,
      "date": "2025-01-20",
      "day_name": "Monday",
      "meals": {{
        "breakfast": {{ recipe object }},
        "lunch": {{ recipe object }},
        "dinner": {{ recipe object }}
      }}
    }}
  ]
}}"""

    return prompt
```

### 7.3 LLM Client

```python
# app/ai/llm_client.py
import anthropic
from app.config import settings
from app.ai.cache import LLMCache

class ClaudeClient:
    def __init__(self):
        self.client = anthropic.Anthropic(api_key=settings.ANTHROPIC_API_KEY)
        self.cache = LLMCache()
        self.model = settings.CLAUDE_MODEL

    async def generate_meal_plan(
        self,
        prompt: str,
        cache_key: str
    ) -> dict:
        # Check cache first
        cached = await self.cache.get(cache_key)
        if cached:
            return cached

        # Call Claude API
        response = self.client.messages.create(
            model=self.model,
            max_tokens=8000,
            system=MEAL_PLAN_SYSTEM_PROMPT,
            messages=[{"role": "user", "content": prompt}]
        )

        # Parse and validate response
        result = self._parse_response(response.content[0].text)

        # Cache for 1 hour (same preferences = same base plan)
        await self.cache.set(cache_key, result, ttl=3600)

        return result

    def _parse_response(self, text: str) -> dict:
        # Extract JSON from response
        import json
        try:
            return json.loads(text)
        except json.JSONDecodeError:
            # Try to extract JSON from markdown code blocks
            import re
            match = re.search(r'```json\s*(.*?)\s*```', text, re.DOTALL)
            if match:
                return json.loads(match.group(1))
            raise ValueError("Could not parse LLM response as JSON")
```

### 7.4 Cost Optimization Strategies

| Strategy | Implementation | Savings |
|----------|----------------|---------|
| **Response Caching** | Cache meal plans for same preference hash | 60-70% |
| **Recipe Database** | Store generated recipes, reuse in future plans | 40-50% |
| **Prompt Compression** | Minimize prompt tokens, use abbreviations | 20-30% |
| **Batch Processing** | Generate multiple days in one call | 30-40% |
| **Model Selection** | Use Haiku for simple swaps, Sonnet for full plans | 50% |

---

## 8. Offline Architecture

### 8.1 Offline Data Strategy

```
┌─────────────────────────────────────────────────────────────────────┐
│                     Offline Architecture                             │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                        ROOM DATABASE                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌────────────┐  │
│  │   Users &   │  │  Meal Plans │  │   Recipes   │  │  Grocery   │  │
│  │ Preferences │  │   (cached)  │  │  (cached)   │  │   Lists    │  │
│  └─────────────┘  └─────────────┘  └─────────────┘  └────────────┘  │
│                                                                      │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                  │
│  │  Festivals  │  │  Favorites  │  │ Sync Queue  │                  │
│  │ (pre-loaded)│  │             │  │ (pending)   │                  │
│  └─────────────┘  └─────────────┘  └─────────────┘                  │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              │ Sync Manager
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     SYNC STRATEGY                                    │
│                                                                      │
│  ONLINE:                                                             │
│  • Fetch latest meal plan from server                               │
│  • Update local cache                                                │
│  • Process pending sync queue                                        │
│  • Download recipe images                                            │
│                                                                      │
│  OFFLINE:                                                            │
│  • Read from Room database                                           │
│  • Queue mutations (grocery checks, etc.)                           │
│  • Show offline banner                                               │
│  • Block features requiring server (generate new plan)              │
└─────────────────────────────────────────────────────────────────────┘
```

### 8.2 Sync Manager Implementation

```kotlin
// data/sync/SyncManager.kt
@Singleton
class SyncManager @Inject constructor(
    private val networkMonitor: NetworkMonitor,
    private val mealPlanDao: MealPlanDao,
    private val recipeDao: RecipeDao,
    private val syncQueueDao: SyncQueueDao,
    private val apiService: RasoiApiService
) {
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    init {
        // Observe network changes
        networkMonitor.isOnline.onEach { isOnline ->
            if (isOnline) {
                processPendingSync()
            }
        }.launchIn(CoroutineScope(Dispatchers.IO))
    }

    suspend fun syncMealPlan() {
        if (!networkMonitor.isOnline.value) return

        _syncState.value = SyncState.Syncing
        try {
            // Fetch latest from server
            val remotePlan = apiService.getCurrentMealPlan()

            // Update local database
            mealPlanDao.insertMealPlan(remotePlan.toEntity())

            // Cache all recipes
            remotePlan.items.forEach { item ->
                val recipe = apiService.getRecipe(item.recipeId)
                recipeDao.insertRecipe(recipe.toEntity())
            }

            _syncState.value = SyncState.Success
        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.message)
        }
    }

    suspend fun queueOfflineAction(action: SyncAction) {
        syncQueueDao.insert(
            SyncQueueEntity(
                action = action.type,
                payload = action.toJson(),
                createdAt = System.currentTimeMillis()
            )
        )
    }

    private suspend fun processPendingSync() {
        val pendingActions = syncQueueDao.getAllPending()
        pendingActions.forEach { action ->
            try {
                executeAction(action)
                syncQueueDao.delete(action.id)
            } catch (e: Exception) {
                // Keep in queue for retry
            }
        }
    }
}

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    object Success : SyncState()
    data class Error(val message: String?) : SyncState()
}
```

### 8.3 Offline Feature Matrix

| Feature | Offline | Implementation |
|---------|---------|----------------|
| View meal plan | ✅ Full | Room cache |
| View recipes | ✅ Full | Room cache |
| Cooking mode | ✅ Full | Local data |
| View grocery list | ✅ Full | Room cache |
| Check grocery items | ✅ Queued | Sync queue |
| Generate new plan | ❌ | Requires LLM |
| Swap meals | ❌ | Requires LLM |
| Change preferences | ❌ | Requires sync |
| View festivals | ✅ Full | Pre-loaded |
| Add favorites | ✅ Queued | Sync queue |

---

## 9. Security Design

### 9.1 Authentication Flow

```
┌──────────────────────────────────────────────────────────────────────┐
│                    Authentication Flow                                │
└──────────────────────────────────────────────────────────────────────┘

     Android App                Firebase                  RasoiAI Backend
          │                        │                            │
          │  1. Google/Phone Auth  │                            │
          │───────────────────────►│                            │
          │                        │                            │
          │  2. Firebase ID Token  │                            │
          │◄───────────────────────│                            │
          │                        │                            │
          │  3. POST /auth/firebase (token)                     │
          │────────────────────────────────────────────────────►│
          │                        │                            │
          │                        │  4. Verify Token           │
          │                        │◄───────────────────────────│
          │                        │                            │
          │                        │  5. Token Valid + User Info│
          │                        │───────────────────────────►│
          │                        │                            │
          │                        │           6. Create/Get User
          │                        │           7. Generate JWT  │
          │                        │                            │
          │  8. JWT Access Token + User                         │
          │◄────────────────────────────────────────────────────│
          │                        │                            │
          │  9. Store token locally                             │
          │                        │                            │
          │  10. API calls with Bearer token                    │
          │────────────────────────────────────────────────────►│
```

### 9.2 Security Measures

| Layer | Measure | Implementation |
|-------|---------|----------------|
| **Transport** | TLS 1.3 | HTTPS only, certificate pinning |
| **Auth** | Firebase Auth | Google OAuth, Phone OTP |
| **API** | JWT tokens | 7-day expiry, refresh on use |
| **Rate Limiting** | Redis-based | 100 req/min per user |
| **Data** | Encryption at rest | PostgreSQL encryption |
| **Secrets** | Environment variables | .env, AWS Secrets Manager |
| **Input** | Validation | Pydantic schemas, sanitization |

### 9.3 API Security Middleware

```python
# app/api/middleware/auth_middleware.py
from fastapi import Request, HTTPException
from fastapi.security import HTTPBearer
from app.core.firebase_auth import verify_firebase_token
from app.core.security import verify_jwt_token

security = HTTPBearer()

async def auth_middleware(request: Request, call_next):
    # Skip auth for public endpoints
    if request.url.path in ["/health", "/api/v1/auth/firebase"]:
        return await call_next(request)

    # Get token from header
    auth_header = request.headers.get("Authorization")
    if not auth_header or not auth_header.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Missing authorization")

    token = auth_header.split(" ")[1]

    try:
        # Verify JWT
        payload = verify_jwt_token(token)
        request.state.user_id = payload["user_id"]
    except Exception:
        raise HTTPException(status_code=401, detail="Invalid token")

    return await call_next(request)
```

---

## 10. Screen Flows

### 10.1 Main Navigation Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                       App Navigation Flow                            │
└─────────────────────────────────────────────────────────────────────┘

                              ┌─────────────┐
                              │   SPLASH    │
                              │   SCREEN    │
                              └──────┬──────┘
                                     │
                         ┌───────────┴───────────┐
                         │                       │
                    No Token                Has Token
                         │                       │
                         ▼                       ▼
                  ┌─────────────┐        ┌─────────────┐
                  │    AUTH     │        │   Check if  │
                  │   SCREEN    │        │  Onboarded  │
                  └──────┬──────┘        └──────┬──────┘
                         │                      │
                    Login Success        ┌──────┴──────┐
                         │               │             │
                         ▼              No            Yes
                  ┌─────────────┐        │             │
                  │ ONBOARDING  │◄───────┘             │
                  │  (5 Steps)  │                      │
                  └──────┬──────┘                      │
                         │                             │
                    Complete                           │
                         │                             │
                         └──────────────┬──────────────┘
                                        │
                                        ▼
                              ┌─────────────────┐
                              │   MAIN SHELL    │
                              │  (Bottom Nav)   │
                              └────────┬────────┘
                                       │
           ┌───────────┬───────────┬───┴───┬───────────┐
           │           │           │       │           │
           ▼           ▼           ▼       ▼           ▼
     ┌──────────┐┌──────────┐┌──────────┐┌──────────┐┌──────────┐
     │   HOME   ││  GROCERY ││ FAVORITES││ FESTIVALS││ SETTINGS │
     │(Meal Plan)│  LIST    ││          ││ CALENDAR ││          │
     └────┬─────┘└────┬─────┘└────┬─────┘└──────────┘└──────────┘
          │           │           │
          ▼           │           │
     ┌──────────┐    │           │
     │  RECIPE  │◄───┴───────────┘
     │  DETAIL  │
     └────┬─────┘
          │
          ▼
     ┌──────────┐
     │ COOKING  │
     │   MODE   │
     └──────────┘
```

### 10.2 Screen Specifications

#### Home Screen (Meal Plan)
- **Tab**: Home icon
- **Content**: Weekly calendar view with meal cards
- **Actions**: Tap meal → Recipe detail, Swap button, Regenerate week

#### Recipe Detail Screen
- **Navigation**: From Home meal tap or Favorites
- **Content**: Image, title (EN/HI), nutrition, ingredients, steps
- **Actions**: Start cooking, Add to favorites, Scale servings

#### Cooking Mode Screen
- **Navigation**: From Recipe detail
- **Content**: Full-screen steps, large text, timer
- **Behavior**: Keep screen on, step navigation, offline capable

#### Grocery List Screen
- **Tab**: Cart icon
- **Content**: Categorized list, checkboxes, WhatsApp share button
- **Actions**: Check items, Add manual item, Share to WhatsApp

#### Settings Screen
- **Tab**: Profile icon
- **Content**: Family members, Preferences, Language, Account
- **Actions**: Edit preferences (triggers plan regeneration prompt)

---

## Appendix A: Environment Variables

```bash
# .env.example

# App
APP_NAME=RasoiAI
DEBUG=false
ALLOWED_ORIGINS=["https://rasoiai.app"]

# Database
DATABASE_URL=postgresql://user:pass@localhost:5432/rasoiai
REDIS_URL=redis://localhost:6379/0

# Firebase
FIREBASE_PROJECT_ID=rasoiai-prod
GOOGLE_APPLICATION_CREDENTIALS=/path/to/firebase-service-account.json

# Claude API
ANTHROPIC_API_KEY=sk-ant-...
CLAUDE_MODEL=claude-3-sonnet-20240229

# AWS
AWS_ACCESS_KEY_ID=AKIA...
AWS_SECRET_ACCESS_KEY=...
S3_BUCKET_NAME=rasoiai-images
S3_REGION=ap-south-1

# JWT
JWT_SECRET_KEY=your-256-bit-secret
JWT_ALGORITHM=HS256
ACCESS_TOKEN_EXPIRE_MINUTES=10080

# Rate Limiting
RATE_LIMIT_REQUESTS=100
RATE_LIMIT_WINDOW=60
```

---

## Appendix B: API Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| AUTH_001 | 401 | Missing authorization header |
| AUTH_002 | 401 | Invalid or expired token |
| AUTH_003 | 403 | Insufficient permissions |
| USER_001 | 404 | User not found |
| USER_002 | 400 | Invalid preferences data |
| PLAN_001 | 404 | Meal plan not found |
| PLAN_002 | 400 | Cannot generate plan (missing preferences) |
| PLAN_003 | 429 | Rate limit exceeded for plan generation |
| RECIPE_001 | 404 | Recipe not found |
| GROCERY_001 | 404 | Grocery list not found |
| SERVER_001 | 500 | Internal server error |
| SERVER_002 | 503 | LLM service unavailable |

---

*Document Version: 1.0*
*Last Updated: January 2025*
