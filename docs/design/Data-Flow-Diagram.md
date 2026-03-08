# RasoiAI Data Flow Architecture

This document describes the complete data flow from Onboarding through Generation to Home and other screens.

## Overview

```
Onboarding → DataStore → Generation API → PostgreSQL → Room Cache → Home Screen → Other Screens
```

---

## Complete Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                            ONBOARDING SCREEN (5 Steps)                          │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  Step 1: HOUSEHOLD        Step 2: DIETARY         Step 3: CUISINE               │
│  ┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐          │
│  │ • Household Size │    │ • Primary Diet   │    │ • Cuisine Zones  │          │
│  │ • Family Members │    │   (VEG/EGGE/NON) │    │   (N/S/E/W)      │          │
│  │   - Name, Age    │    │ • Restrictions   │    │ • Spice Level    │          │
│  │   - Type, Health │    │   (JAIN/SATTVIC) │    │   (MILD→VERY)    │          │
│  └────────┬─────────┘    └────────┬─────────┘    └────────┬─────────┘          │
│           │                       │                       │                     │
│  Step 4: INGREDIENTS      Step 5: COOKING TIME                                  │
│  ┌──────────────────┐    ┌──────────────────┐                                  │
│  │ • Allergies      │    │ • Weekday Time   │                                  │
│  │ • Dislikes       │    │ • Weekend Time   │                                  │
│  │ • Recipe Rules   │    │ • Busy Days      │                                  │
│  └────────┬─────────┘    └────────┬─────────┘                                  │
│           │                       │                                             │
│           └───────────┬───────────┘                                             │
│                       ▼                                                         │
│            ┌─────────────────────┐                                              │
│            │ OnboardingViewModel │                                              │
│            │   OnboardingUiState │                                              │
│            └──────────┬──────────┘                                              │
│                       │                                                         │
└───────────────────────┼─────────────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         LOCAL STORAGE (Android DataStore)                       │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│   UserPreferencesDataStore.saveOnboardingComplete()                             │
│   ┌─────────────────────────────────────────────────────────────────────┐      │
│   │  IS_ONBOARDED = true                                                 │      │
│   │  HOUSEHOLD_SIZE = 3                                                  │      │
│   │  FAMILY_MEMBERS_JSON = "[{name:Ramesh,age:45,health:[DIABETIC]}...]" │      │
│   │  PRIMARY_DIET = "vegetarian"                                         │      │
│   │  DIETARY_RESTRICTIONS = ["sattvic"]                                  │      │
│   │  CUISINE_PREFERENCES = ["north", "south"]                            │      │
│   │  ALLERGIES = [{ingredient:Peanuts,severity:SEVERE}]                  │      │
│   │  DISLIKED_INGREDIENTS = ["Karela", "Baingan"]                        │      │
│   │  RECIPE_RULES = [{type:EXCLUDE,target:Paneer,frequency:NEVER}]       │      │
│   │  WEEKDAY_COOKING_TIME = 30                                           │      │
│   │  WEEKEND_COOKING_TIME = 60                                           │      │
│   │  BUSY_DAYS = ["MONDAY", "WEDNESDAY", "FRIDAY"]                       │      │
│   └─────────────────────────────────────────────────────────────────────┘      │
│                       │                                                         │
└───────────────────────┼─────────────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         MEAL PLAN GENERATION (4 Steps)                          │
│              (Embedded in OnboardingViewModel or HomeViewModel)                  │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│   Generation Progress (shown in Onboarding completion or Home regenerate)       │
│   ┌─────────────────────────────────────────────────────────────────────┐      │
│   │  Step 1: "Analyzing your preferences..."      ████░░░░░░  25%       │      │
│   │  Step 2: "Checking upcoming festivals..."     ████████░░  50%       │      │
│   │  Step 3: "Generating personalized recipes..." ████████████░ 75%     │      │
│   │  Step 4: "Building your grocery list..."      ██████████████ 100%   │      │
│   └─────────────────────────────────────────────────────────────────────┘      │
│                       │                                                         │
│                       ▼                                                         │
│   ┌─────────────────────────────────────────────────────────────────────┐      │
│   │  POST /api/v1/meal-plans/generate                                    │      │
│   │  Body: { "week_start_date": "2026-01-26" }                          │      │
│   │  Headers: Authorization: Bearer <JWT>                                │      │
│   │  Typical time: 45-90 seconds (Gemini AI + DB writes)                │      │
│   └─────────────────────────────────────────────────────────────────────┘      │
│                       │                                                         │
└───────────────────────┼─────────────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                      BACKEND API (Python FastAPI + PostgreSQL)                  │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│   meal_plans.py::generate()                                                     │
│   ┌─────────────────────────────────────────────────────────────────────┐      │
│   │  1. UserRepository.get_preferences(user_id)                          │      │
│   │     └─ Fetches: dietary_tags, allergies, dislikes, recipe_rules,    │      │
│   │        cooking_time, busy_days from PostgreSQL                       │      │
│   │                                                                      │      │
│   │  2. Parse Recipe Rules                                               │      │
│   │     └─ INCLUDE rules (Chai daily, Moringa weekly)                   │      │
│   │     └─ EXCLUDE rules (Paneer never)                                 │      │
│   │     └─ NUTRITION_GOAL rules (Green leafy 5x/week)                   │      │
│   │                                                                      │      │
│   │  3. RecipeRepository.search(cuisine, dietary_tags, limit=500)       │      │
│   │     └─ Queries 3,580 recipes from PostgreSQL                        │      │
│   │                                                                      │      │
│   │  4. _filter_by_exclude_rules(recipes, exclude_rules, allergies)     │      │
│   │     └─ Removes Paneer, Peanuts, Baingan (with aliases)              │      │
│   │                                                                      │      │
│   │  5. _pick_recipe_with_constraints(pool, used_ids, max_time)         │      │
│   │     └─ Respects busy day ≤30min, weekend ≤60min                     │      │
│   │     └─ Avoids duplicate recipes in same week                         │      │
│   │                                                                      │      │
│   │  6. Build 7-day meal plan (28 meals)                                │      │
│   │     └─ Apply INCLUDE rules first (Chai → breakfast/snacks)          │      │
│   │     └─ Fill remaining slots with filtered recipes                    │      │
│   │                                                                      │      │
│   │  7. MealPlanRepository.create(plan_data) → PostgreSQL               │      │
│   └─────────────────────────────────────────────────────────────────────┘      │
│                       │                                                         │
│                       ▼                                                         │
│   ┌─────────────────────────────────────────────────────────────────────┐      │
│   │  POSTGRESQL DATABASE                                                 │      │
│   │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐      │      │
│   │  │   users     │  │  recipes    │  │      meal_plans         │      │      │
│   │  │ ─────────── │  │ ─────────── │  │ ───────────────────────  │      │      │
│   │  │ preferences │  │ 3,580 rows  │  │ user_id, week_start     │      │      │
│   │  │ onboarded   │  │ ingredients │  │ days[]: date, meals{}   │      │      │
│   │  │ recipe_rules│  │ nutrition   │  │   breakfast, lunch,     │      │      │
│   │  └─────────────┘  └─────────────┘  │   dinner, snacks        │      │      │
│   │                                    │ festival: name, fasting  │      │      │
│   │                                    └─────────────────────────┘      │      │
│   └─────────────────────────────────────────────────────────────────────┘      │
│                       │                                                         │
│                       ▼                                                         │
│   Response: MealPlanResponse (JSON)                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐      │
│   │  { "id": "e7f54add-...",                                             │      │
│   │    "week_start_date": "2026-01-26",                                  │      │
│   │    "days": [                                                         │      │
│   │      { "date": "2026-01-26", "day_name": "Monday",                   │      │
│   │        "meals": {                                                    │      │
│   │          "breakfast": [{ "recipe_name": "Masala Chai...", ... }],   │      │
│   │          "lunch": [...], "dinner": [...], "snacks": [...]           │      │
│   │        }, "festival": null },                                        │      │
│   │      ... 6 more days                                                 │      │
│   │  ]}                                                                  │      │
│   └─────────────────────────────────────────────────────────────────────┘      │
│                       │                                                         │
└───────────────────────┼─────────────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                      ANDROID DATA LAYER (Offline-First)                         │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│   MealPlanRepositoryImpl                                                        │
│   ┌─────────────────────────────────────────────────────────────────────┐      │
│   │  1. API Response → DTO Mapping                                       │      │
│   │     MealPlanResponse → MealPlanDto, MealItemDto, FestivalDto        │      │
│   │                                                                      │      │
│   │  2. DTO → Entity Conversion (EntityMappers.kt)                      │      │
│   │     toEntity() → MealPlanEntity                                      │      │
│   │     toItemEntities() → List<MealPlanItemEntity>                     │      │
│   │     toFestivalEntities() → List<MealPlanFestivalEntity>             │      │
│   │                                                                      │      │
│   │  3. Room Transaction (Atomic Insert)                                 │      │
│   │     MealPlanDao.replaceMealPlan(plan, items, festivals)             │      │
│   └─────────────────────────────────────────────────────────────────────┘      │
│                       │                                                         │
│                       ▼                                                         │
│   ┌─────────────────────────────────────────────────────────────────────┐      │
│   │  ROOM DATABASE (Single Source of Truth)                              │      │
│   │  ┌─────────────────┐ ┌───────────────────┐ ┌──────────────────────┐ │      │
│   │  │  meal_plans     │ │ meal_plan_items   │ │meal_plan_festivals   │ │      │
│   │  │ ─────────────── │ │ ───────────────── │ │────────────────────  │ │      │
│   │  │ id (PK)         │ │ mealPlanId (FK)   │ │ id (PK)              │ │      │
│   │  │ weekStartDate   │ │ date              │ │ mealPlanId (FK)      │ │      │
│   │  │ weekEndDate     │ │ mealType          │ │ date                 │ │      │
│   │  │ createdAt       │ │ recipeId          │ │ name                 │ │      │
│   │  │ updatedAt       │ │ recipeName        │ │ isFastingDay         │ │      │
│   │  │ isSynced        │ │ prepTimeMinutes   │ │ suggestedDishes      │ │      │
│   │  └─────────────────┘ │ isLocked          │ └──────────────────────┘ │      │
│   │                      │ dietaryTags       │                          │      │
│   │                      └───────────────────┘                          │      │
│   └─────────────────────────────────────────────────────────────────────┘      │
│                       │                                                         │
│                       ▼                                                         │
│   Entity → Domain Mapping                                                       │
│   ┌─────────────────────────────────────────────────────────────────────┐      │
│   │  MealPlanEntity.toDomain(items, festivals) → MealPlan               │      │
│   │  • Groups items by date → List<MealPlanDay>                         │      │
│   │  • Each day has: breakfast[], lunch[], dinner[], snacks[]           │      │
│   │  • Attaches festival info if present                                 │      │
│   └─────────────────────────────────────────────────────────────────────┘      │
│                       │                                                         │
└───────────────────────┼─────────────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                               HOME SCREEN                                       │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│   HomeViewModel                                                                 │
│   ┌─────────────────────────────────────────────────────────────────────┐      │
│   │  loadMealPlan()                                                      │      │
│   │  ├─ mealPlanRepository.getMealPlanForDate(LocalDate.now())          │      │
│   │  │   └─ Flow<MealPlan?> from Room (offline-first)                   │      │
│   │  ├─ Updates HomeUiState:                                             │      │
│   │  │   • mealPlan: MealPlan                                            │      │
│   │  │   • selectedDate: LocalDate                                       │      │
│   │  │   • weekDates: List<WeekDay> (Mon-Sun selector)                  │      │
│   │  │   • selectedDayMeals: MealPlanDay                                 │      │
│   │  │   • upcomingFestival: FestivalInfo?                               │      │
│   │  └─ Emits state to UI via StateFlow                                 │      │
│   └─────────────────────────────────────────────────────────────────────┘      │
│                       │                                                         │
│                       ▼                                                         │
│   ┌─────────────────────────────────────────────────────────────────────┐      │
│   │  HOME UI                                                             │      │
│   │  ┌───────────────────────────────────────────────────────────────┐  │      │
│   │  │  ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┐ ← Week Selector  │  │      │
│   │  │  │ Mon │ Tue │ Wed │ Thu │ Fri │ Sat │ Sun │                  │  │      │
│   │  │  └─────┴─────┴─────┴─────┴─────┴─────┴─────┘                  │  │      │
│   │  │                                                                │  │      │
│   │  │  Breakfast: Masala Chai Creme Brulee (20 min)                 │  │      │
│   │  │  Lunch: Butter Dal Fry (10 min)                               │  │      │
│   │  │  Dinner: Bagara Rice (16 min)                                 │  │      │
│   │  │  Snacks: Eggless Mawa Cake (20 min)                           │  │      │
│   │  │                                                                │  │      │
│   │  │  [Lock] [Swap] → Opens SwapSheet with suggestions             │  │      │
│   │  └───────────────────────────────────────────────────────────────┘  │      │
│   └─────────────────────────────────────────────────────────────────────┘      │
│                                                                                 │
│   User Actions:                                                                 │
│   ┌─────────────────────────────────────────────────────────────────────┐      │
│   │  swapMeal(mealId) → POST /api/v1/meal-plans/{id}/items/{id}/swap   │      │
│   │  lockMeal(mealId) → PUT /api/v1/meal-plans/{id}/items/{id}/lock    │      │
│   │  regenerate()     → POST /api/v1/meal-plans/generate                │      │
│   └─────────────────────────────────────────────────────────────────────┘      │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
                        │
        ┌───────────────┼───────────────┬───────────────┬───────────────┐
        ▼               ▼               ▼               ▼               ▼
┌───────────────┐ ┌───────────────┐ ┌───────────────┐ ┌───────────────┐ ┌───────────────┐
│ RECIPE DETAIL │ │    GROCERY    │ │   FAVORITES   │ │     STATS     │ │     CHAT      │
├───────────────┤ ├───────────────┤ ├───────────────┤ ├───────────────┤ ├───────────────┤
│               │ │               │ │               │ │               │ │               │
│ Click meal →  │ │ Derives from  │ │ RecipeRepo    │ │ Aggregates    │ │ AI Assistant  │
│ RecipeRepo    │ │ MealPlan      │ │ .getFavorites │ │ from MealPlan │ │ Recipe Q&A    │
│ .getById(id)  │ │ ingredients   │ │               │ │ history       │ │               │
│               │ │               │ │ ┌───────────┐ │ │               │ │ Uses meal     │
│ ┌───────────┐ │ │ ┌───────────┐ │ │ │ Fav 1     │ │ │ ┌───────────┐ │ │ plan context  │
│ │ Recipe    │ │ │ │ Carrot    │ │ │ │ Fav 2     │ │ │ │ Streak    │ │ │               │
│ │ Name      │ │ │ │ Onion     │ │ │ │ Fav 3     │ │ │ │ Cuisine   │ │ │ ┌───────────┐ │
│ │ Time: 20m │ │ │ │ Tomato    │ │ │ └───────────┘ │ │ │ Trends    │ │ │ │ Ask about │ │
│ │ Calories  │ │ │ │ Check     │ │ │               │ │ └───────────┘ │ │ │ recipes   │ │
│ │ Ingredients│ │ │ └───────────┘ │ │ Toggle fav:  │ │               │ │ └───────────┘ │
│ │ Steps     │ │ │               │ │ RecipeRepo    │ │ MealPlanRepo  │ │               │
│ │ Nutrition │ │ │ Share via    │ │ .toggleFav()  │ │ .getHistory() │ │ POST /chat    │
│ └───────────┘ │ │ WhatsApp     │ │               │ │               │ │ /message      │
│               │ │               │ │               │ │               │ │               │
│ Scale recipe │ │ GET /grocery  │ │               │ │ GET /stats    │ │               │
│ GET /recipes │ │ /whatsapp     │ │               │ │ /monthly      │ │               │
│ /{id}/scale  │ │               │ │               │ │               │ │               │
└───────────────┘ └───────────────┘ └───────────────┘ └───────────────┘ └───────────────┘
```

---

## Simplified Flow Diagram

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  ONBOARDING │────▶│  DATASTORE  │────▶│  GENERATION │────▶│    HOME     │
│  (5 Steps)  │     │   (Local)   │     │    (API)    │     │  (Display)  │
└─────────────┘     └─────────────┘     └──────┬──────┘     └──────┬──────┘
                                               │                   │
                                               ▼                   │
                                        ┌─────────────┐            │
                                        │ POSTGRESQL  │            │
                                        │  (Backend)  │            │
                                        └──────┬──────┘            │
                                               │                   │
                                               ▼                   │
                                        ┌─────────────┐            │
                                        │    ROOM     │◀───────────┘
                                        │   (Cache)   │
                                        └──────┬──────┘
                                               │
                    ┌──────────┬───────────────┼───────────────┬──────────┐
                    ▼          ▼               ▼               ▼          ▼
              ┌─────────┐ ┌─────────┐   ┌─────────┐   ┌─────────┐ ┌─────────┐
              │ Recipe  │ │ Grocery │   │Favorites│   │  Stats  │ │  Chat   │
              │ Detail  │ │  List   │   │         │   │         │ │         │
              └─────────┘ └─────────┘   └─────────┘   └─────────┘ └─────────┘
```

---

## Key Architecture Patterns

| Pattern | Description |
|---------|-------------|
| **Offline-First** | Room DB is single source of truth; API syncs when online |
| **Repository Pattern** | Abstracts data sources (Room + Retrofit) behind interfaces |
| **StateFlow + UiState** | Single immutable state object per ViewModel |
| **DTO → Entity → Domain** | Clean separation of API, cache, and business layers |
| **Atomic Transactions** | `replaceMealPlan()` updates 3 tables atomically |

---

## Data Models by Layer

### Presentation Layer (UI State)

```kotlin
// OnboardingUiState
data class OnboardingUiState(
    val currentStep: OnboardingStep,
    val householdSize: Int,
    val familyMembers: List<FamilyMember>,
    val primaryDiet: PrimaryDiet,
    val dietaryRestrictions: Set<DietaryRestriction>,
    val selectedCuisines: Set<CuisineType>,
    val spiceLevel: SpiceLevel,
    val dislikedIngredients: Set<String>,
    val weekdayCookingTimeMinutes: Int,
    val weekendCookingTimeMinutes: Int,
    val busyDays: Set<DayOfWeek>
)

// HomeUiState
data class HomeUiState(
    val isLoading: Boolean,
    val mealPlan: MealPlan?,
    val selectedDate: LocalDate,
    val weekDates: List<WeekDay>,
    val selectedDayMeals: MealPlanDay?,
    val upcomingFestival: FestivalInfo?
)
```

### Domain Layer (Business Models)

```kotlin
data class MealPlan(
    val id: String,
    val weekStartDate: LocalDate,
    val weekEndDate: LocalDate,
    val days: List<MealPlanDay>,
    val createdAt: Long,
    val updatedAt: Long
)

data class MealPlanDay(
    val date: LocalDate,
    val dayName: String,
    val breakfast: List<MealItem>,
    val lunch: List<MealItem>,
    val dinner: List<MealItem>,
    val snacks: List<MealItem>,
    val festival: Festival?
)

data class MealItem(
    val id: String,
    val recipeId: String,
    val recipeName: String,
    val prepTimeMinutes: Int,
    val calories: Int,
    val isLocked: Boolean,
    val dietaryTags: List<DietaryTag>
)
```

### Data Layer (Entities & DTOs)

```kotlin
// Room Entity
@Entity(tableName = "meal_plans")
data class MealPlanEntity(
    @PrimaryKey val id: String,
    val weekStartDate: String,
    val weekEndDate: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isSynced: Boolean
)

// API DTO
data class MealPlanResponse(
    val id: String,
    val weekStartDate: String,
    val weekEndDate: String,
    val days: List<MealPlanDayDto>,
    val createdAt: String,
    val updatedAt: String
)
```

---

## Key Files Reference

| Layer | File | Purpose |
|-------|------|---------|
| **Presentation** | `OnboardingViewModel.kt` | Collects 5-step preferences |
| **Presentation** | `OnboardingViewModel.kt` / `HomeViewModel.kt` | Generation progress (no separate GenerationViewModel) |
| **Presentation** | `HomeViewModel.kt` | Displays/manages meal plan |
| **Domain** | `MealPlan.kt` | Core business models |
| **Domain** | `MealPlanRepository.kt` | Repository interface |
| **Data** | `MealPlanRepositoryImpl.kt` | Offline-first logic |
| **Data** | `MealPlanDao.kt` | Room queries |
| **Data** | `MealPlanEntity.kt` | Room schema |
| **Data** | `MealPlanDtos.kt` | API DTOs |
| **Data** | `EntityMappers.kt` | DTO↔Entity↔Domain |
| **Data** | `RasoiApiService.kt` | Retrofit API |
| **Backend** | `meal_plans.py` | Rule enforcement + generation |

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/meal-plans/generate` | Generate new meal plan |
| GET | `/api/v1/meal-plans/current` | Get current week's plan |
| GET | `/api/v1/meal-plans/{id}` | Get specific plan |
| POST | `/api/v1/meal-plans/{id}/items/{itemId}/swap` | Swap a meal |
| PUT | `/api/v1/meal-plans/{id}/items/{itemId}/lock` | Lock/unlock meal |
| GET | `/api/v1/recipes/{id}` | Get recipe details |
| GET | `/api/v1/grocery` | Get grocery list |

---

## Recipe Rule Enforcement (Backend)

The backend enforces these rules during generation:

1. **EXCLUDE Rules** - Never include specified ingredients/recipes
2. **INCLUDE Rules** - Must include at specified frequency (daily/weekly)
3. **Allergies** - Always excluded with ingredient aliases
4. **Dislikes** - Always excluded with ingredient aliases
5. **Cooking Time** - Respects busy day and weekend limits
6. **Dietary Tags** - Filters by vegetarian/vegan/sattvic
7. **Force Override** - INCLUDE rules with `force_override=true` bypass family safety conflict filtering

```python
# Example rule enforcement flow
recipes = await recipe_repo.search(cuisine, dietary_tags, limit=500)
filtered = _filter_by_exclude_rules(recipes, exclude_rules, allergies, dislikes)
# For each day, apply INCLUDE rules first, then fill remaining slots
```

---

## Household Data Flow

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                          HOUSEHOLD DATA FLOW                                     │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│   Household Screens (6 screens)                                                 │
│   ┌────────────────────────────────────────────────────────────────────────┐    │
│   │  HouseholdScreen → HouseholdMembersScreen → HouseholdMemberDetail    │    │
│   │  JoinHouseholdScreen → HouseholdMealPlanScreen                        │    │
│   │  HouseholdNotificationsScreen                                         │    │
│   └────────────────────────────────┬───────────────────────────────────────┘    │
│                                    │                                            │
│                                    ▼                                            │
│   HouseholdViewModel / HouseholdMembersViewModel / etc.                        │
│   ┌────────────────────────────────────────────────────────────────────────┐    │
│   │  HouseholdRepository (domain interface)                                │    │
│   │  └─ HouseholdRepositoryImpl (data layer)                               │    │
│   │     ├─ Room: HouseholdDao (households, household_members tables)       │    │
│   │     └─ Retrofit: RasoiApiService (~18 household endpoints)             │    │
│   └────────────────────────────────────────────────────────────────────────┘    │
│                                                                                 │
│   Room DB v13 (new tables):                                                     │
│   ┌──────────────────┐  ┌───────────────────────┐                              │
│   │   households     │  │  household_members    │                              │
│   │ ──────────────── │  │ ───────────────────── │                              │
│   │ id (PK)          │  │ id (PK)               │                              │
│   │ name             │  │ householdId (FK)       │                              │
│   │ inviteCode       │  │ userId                 │                              │
│   │ ownerId          │  │ role (OWNER/ADMIN/     │                              │
│   │ isActive         │  │       MEMBER)           │                              │
│   │ createdAt        │  │ joinedAt               │                              │
│   └──────────────────┘  └───────────────────────┘                              │
│                                                                                 │
│   Backend API (~18 endpoints):                                                  │
│   ┌────────────────────────────────────────────────────────────────────────┐    │
│   │  POST   /api/v1/households              Create household               │    │
│   │  GET    /api/v1/households/me            Get user's household           │    │
│   │  PUT    /api/v1/households/{id}          Update household               │    │
│   │  DELETE /api/v1/households/{id}          Deactivate household           │    │
│   │  POST   /api/v1/households/{id}/invite   Generate invite code          │    │
│   │  POST   /api/v1/households/join          Join via invite code           │    │
│   │  GET    /api/v1/households/{id}/members  List members                   │    │
│   │  DELETE /api/v1/households/{id}/members/{mid}  Remove member            │    │
│   │  PUT    /api/v1/households/{id}/members/{mid}/role  Update role         │    │
│   │  GET    /api/v1/households/{id}/meal-plans  Household meal plans        │    │
│   │  GET    /api/v1/households/{id}/notifications  Household notifications  │    │
│   │  ... (+ constraints, stats, etc.)                                       │    │
│   └────────────────────────────────────────────────────────────────────────┘    │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Scope Toggle (DataScope)

Five screens support switching between personal and household-scoped data:

```
┌─────────────────────────────────────────────────────────────────┐
│                     SCOPE TOGGLE (ScopeToggle composable)       │
│                                                                 │
│   ┌──────────────┐      ┌──────────────┐                       │
│   │   PERSONAL   │◄────►│    FAMILY    │                       │
│   │  (DataScope) │      │  (DataScope) │                       │
│   └──────┬───────┘      └──────┬───────┘                       │
│          │                     │                                │
│          ▼                     ▼                                │
│   Existing repos         HouseholdRepository                   │
│   (user-scoped data)     (household-scoped data)               │
│                                                                 │
│   Screens with scope toggle:                                    │
│   • Stats        — personal vs household cooking stats          │
│   • Grocery      — personal vs household grocery list           │
│   • Favorites    — personal vs household favorites              │
│   • Recipe Rules — personal vs household rules                  │
│   • Chat         — personal vs household context                │
└─────────────────────────────────────────────────────────────────┘
```

---

*Last updated: March 8, 2026*
