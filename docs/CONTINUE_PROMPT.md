# Continuation Prompt for RasoiAI Project

Use this prompt to start a new conversation/context and continue the project from where we left off.

---

## PROMPT TO USE:

```
I am building **RasoiAI** - an AI-powered meal planning app for Indian families.

## Project Status

Splash, Auth, Onboarding, and Home screens are COMPLETE. App runs successfully. Ready for Recipe Detail screen implementation.

| Phase | Status | Document |
|-------|--------|----------|
| Requirements | ✅ Complete | `docs/requirements/RasoiAI Requirements.md` |
| Technical Design | ✅ Complete | `docs/design/RasoiAI Technical Design.md` |
| Architecture Decisions | ✅ Complete | `docs/design/Android Architecture Decisions.md` |
| Design System | ✅ Complete | `docs/design/RasoiAI Design System.md` |
| Screen Wireframes | ✅ Complete | `docs/design/RasoiAI Screen Wireframes.md` |
| Android Project Setup | ✅ Complete | `android/` folder |
| Splash Screen | ✅ Complete | `presentation/splash/` |
| Auth Screen | ✅ Complete | `presentation/auth/` |
| Onboarding Screen | ✅ Complete | `presentation/onboarding/` |
| Home Screen | ✅ Complete | `presentation/home/` |
| **Recipe Detail** | ⏳ **Next Step** | Tabs: Ingredients/Instructions |

## Recent Fixes Applied

| Issue | Fix | File |
|-------|-----|------|
| App crash - missing service | Removed `RasoiFirebaseMessagingService` from manifest | `AndroidManifest.xml` |
| App crash - Firebase package mismatch | Removed `.debug` suffix from debug build | `app/build.gradle.kts` |
| App crash - invalid google-services | Updated `src/debug/google-services.json` to match main | `google-services.json` |
| Test failures | Updated tests with mock `UserPreferencesDataStore` | `SplashViewModelTest.kt`, `AuthViewModelTest.kt` |

## App Verified Working

- ✅ App builds successfully
- ✅ All tests pass (`.\gradlew test`)
- ✅ App installs and runs on emulator
- ✅ Splash screen → Auth screen navigation works
- ✅ UI displays correctly (cream background, orange theme)

## Your Task

**Implement the Recipe Detail Screen** (Screen 5 in wireframes):

### Files to Create:
```
app/presentation/recipedetail/
├── RecipeDetailScreen.kt      # Main composable with tabs
├── RecipeDetailViewModel.kt   # State management
└── components/
    ├── RecipeHeader.kt        # Image + name + quick info
    ├── NutritionCard.kt       # Nutrition info display
    ├── IngredientsTab.kt      # Servings adjuster + checkbox list
    └── InstructionsTab.kt     # Numbered steps cards

data/repository/
└── FakeRecipeRepository.kt    # Full recipe data (implements RecipeRepository)
```

### Update Files:
- `RasoiNavHost.kt` - Replace placeholder with RecipeDetailScreen
- `DataModule.kt` - Bind FakeRecipeRepository

### UI Components (from wireframes):
1. Top app bar (back arrow, favorite heart ♡, more options ⋮)
2. Recipe hero image
3. Recipe name with dietary indicator (● green = veg)
4. Cuisine + region text (e.g., "North Indian • Punjabi")
5. Quick info row: ⏱️ time | 👥 servings | 🔥 calories
6. Dietary tags chips (Vegetarian, Gluten-Free, Easy, etc.)
7. Nutrition card (Calories, Protein, Carbs, Fat per serving)
8. Tab row: INGREDIENTS | INSTRUCTIONS
9. **Ingredients Tab:**
   - Servings dropdown adjuster
   - Checkbox list of ingredients
   - "Add All to Grocery List" button
10. **Instructions Tab:**
    - Step count header
    - Numbered step cards
11. Bottom buttons:
    - Primary: "🍳 START COOKING MODE"
    - Secondary: "💬 Modify with AI"

### Key Patterns (follow existing code):
- ViewModel: `StateFlow<UiState>` + `StateFlow<NavigationEvent?>`
- Screen receives callbacks, ViewModel handles logic
- Use `MaterialTheme.colorScheme` and `spacing` from theme
- Use `hiltViewModel()` for DI

## Reference Files

| File | Path | Why |
|------|------|-----|
| Wireframes | `docs/design/RasoiAI Screen Wireframes.md` | Screen 5 design (line 676+) |
| Recipe Model | `domain/model/Recipe.kt` | Recipe, Ingredient, Instruction, Nutrition |
| RecipeRepository | `domain/repository/RecipeRepository.kt` | Interface to implement |
| HomeScreen | `app/presentation/home/HomeScreen.kt` | Pattern reference |
| HomeViewModel | `app/presentation/home/HomeViewModel.kt` | State management pattern |
| RasoiNavHost | `app/presentation/navigation/RasoiNavHost.kt` | Navigation setup (line 97-115) |
| FakeMealPlanRepository | `data/repository/FakeMealPlanRepository.kt` | Fake data pattern |

## Domain Models Available

```kotlin
// Recipe.kt
data class Recipe(
    val id: String,
    val name: String,
    val description: String,
    val imageUrl: String?,
    val prepTimeMinutes: Int,
    val cookTimeMinutes: Int,
    val servings: Int,
    val difficulty: Difficulty,        // EASY, MEDIUM, HARD
    val cuisineType: CuisineType,      // NORTH, SOUTH, EAST, WEST
    val mealTypes: List<MealType>,
    val dietaryTags: List<DietaryTag>, // VEGETARIAN, VEGAN, JAIN, etc.
    val ingredients: List<Ingredient>,
    val instructions: List<Instruction>,
    val nutrition: Nutrition?,
    val isFavorite: Boolean = false
)

data class Ingredient(
    val id: String,
    val name: String,
    val quantity: String,
    val unit: String,
    val category: IngredientCategory,
    val isOptional: Boolean = false
)

data class Instruction(
    val stepNumber: Int,
    val instruction: String,
    val durationMinutes: Int?,
    val timerRequired: Boolean = false,
    val tips: String?
)

data class Nutrition(
    val calories: Int,
    val proteinGrams: Int,
    val carbohydratesGrams: Int,
    val fatGrams: Int,
    val fiberGrams: Int,
    val sugarGrams: Int,
    val sodiumMg: Int
)
```

## Working Directory
Project root: `D:/Abhay/VibeCoding/KKB`

Start by reading Screen 5 wireframes in `docs/design/RasoiAI Screen Wireframes.md` (line 676), then implement the Recipe Detail screen.
```

---

## QUICK START PROMPT (Shorter):

```
I'm building **RasoiAI** - an AI meal planning app for Indian families.

**COMPLETED:** Splash, Auth, Onboarding, Home screens. App runs successfully on emulator.

**NEXT:** Implement Recipe Detail Screen (Screen 5)

**Read first:** `docs/design/RasoiAI Screen Wireframes.md` - Search for "Screen 5: Recipe Detail" (line 676)

**Create these files:**
1. `app/presentation/recipedetail/RecipeDetailScreen.kt` - Tabs UI
2. `app/presentation/recipedetail/RecipeDetailViewModel.kt` - State management
3. `data/repository/FakeRecipeRepository.kt` - Mock recipe data

**Update:**
- `RasoiNavHost.kt` - Wire up RecipeDetailScreen (replace placeholder at line 108-115)
- `DataModule.kt` - Provide FakeRecipeRepository

**Recipe Detail UI:**
- Hero image + recipe name + cuisine
- Quick info: time, servings, calories
- Dietary tags + nutrition card
- Tabs: Ingredients (with checkboxes) | Instructions (step cards)
- Buttons: "Start Cooking Mode" + "Modify with AI"

**Reference:** `app/presentation/home/` for ViewModel/Screen patterns
**Domain models:** `domain/model/Recipe.kt`

Project root: `D:/Abhay/VibeCoding/KKB`
```

---

## FILES TO REFERENCE:

| File | Path | Priority | Description |
|------|------|----------|-------------|
| Screen Wireframes | `docs/design/RasoiAI Screen Wireframes.md` | **HIGH** | Recipe Detail design (Screen 5, line 676) |
| Recipe Model | `domain/model/Recipe.kt` | **HIGH** | Recipe, Ingredient, Instruction, Nutrition |
| RecipeRepository | `domain/repository/RecipeRepository.kt` | **HIGH** | Interface to implement |
| Home Screen | `app/presentation/home/HomeScreen.kt` | **HIGH** | Pattern reference |
| Home ViewModel | `app/presentation/home/HomeViewModel.kt` | **HIGH** | State management pattern |
| RasoiNavHost | `app/presentation/navigation/RasoiNavHost.kt` | **HIGH** | Navigation (line 97-115 for RecipeDetail) |
| FakeMealPlanRepository | `data/repository/FakeMealPlanRepository.kt` | MEDIUM | Fake data pattern |
| DataModule | `data/di/DataModule.kt` | MEDIUM | DI bindings |
| Theme | `app/presentation/theme/` | MEDIUM | Colors, spacing |
| CLAUDE.md | Root | MEDIUM | Project overview |

---

## RECIPE DETAIL WIREFRAME SUMMARY:

```
┌─────────────────────────────────────┐
│  ←                         ♡    ⋮   │  ← Top bar (back, favorite, menu)
│─────────────────────────────────────│
│  ┌─────────────────────────────────┐│
│  │      [RECIPE IMAGE HERE]       ││  ← Hero image
│  └─────────────────────────────────┘│
│                                     │
│  ● Dal Tadka                        │  ← Recipe name + veg indicator
│    North Indian • Punjabi           │  ← Cuisine + region
│                                     │
│  ┌────────┬────────┬────────┐       │
│  │ ⏱️     │ 👥     │ 🔥     │       │  ← Quick info
│  │ 35 min │ 4 serv │ 180cal │       │
│  └────────┴────────┴────────┘       │
│                                     │
│  [● Vegetarian] [Gluten-Free]       │  ← Tags
│  [High Protein] [Easy]              │
│                                     │
│─────────────────────────────────────│
│  NUTRITION PER SERVING              │
│  ┌────────┬────────┬────────┬─────┐ │
│  │Calories│Protein │ Carbs  │ Fat │ │
│  │  180   │  12g   │  22g   │  5g │ │
│  └────────┴────────┴────────┴─────┘ │
│─────────────────────────────────────│
│  ┌────────────────┬────────────────┐│
│  │  INGREDIENTS   │  INSTRUCTIONS  ││  ← Tab row
│  │      ━━━━      │                ││
│  └────────────────┴────────────────┘│
│                                     │
│  Servings: [4 servings ▼]           │  ← Adjuster
│                                     │
│  □ 1 cup Toor dal                   │  ← Checkbox list
│  □ 1 medium Onion, chopped          │
│  □ 2 medium Tomatoes, pureed        │
│  □ 4 cloves Garlic, minced          │
│  ... more ingredients               │
│                                     │
│  ┌─────────────────────────────────┐│
│  │   + Add All to Grocery List     ││  ← Add to grocery
│  └─────────────────────────────────┘│
│─────────────────────────────────────│
│  ┌─────────────────────────────────┐│
│  │     🍳 START COOKING MODE       ││  ← Primary action
│  └─────────────────────────────────┘│
│  ┌─────────────────────────────────┐│
│  │     💬 Modify with AI           ││  ← Secondary action
│  └─────────────────────────────────┘│
└─────────────────────────────────────┘
```

### Instructions Tab:
```
│  6 Steps                            │
│                                     │
│  ┌─────────────────────────────────┐│
│  │ Step 1                          ││
│  │ Wash and soak toor dal for 30   ││
│  │ minutes. Pressure cook with     ││
│  │ turmeric and salt for 3         ││
│  │ whistles.                       ││
│  └─────────────────────────────────┘│
│  ┌─────────────────────────────────┐│
│  │ Step 2                          ││
│  │ Heat ghee in a pan. Add cumin   ││
│  │ seeds and let them splutter.    ││
│  └─────────────────────────────────┘│
│  ... more steps                     │
```

---

## IMPLEMENTATION CHECKLIST:

### 1. Create Recipe Detail Files
```
app/presentation/recipedetail/
├── RecipeDetailScreen.kt      # Main composable with tabs
├── RecipeDetailViewModel.kt   # State management
└── components/
    ├── RecipeHeader.kt        # Image + name + info
    ├── NutritionCard.kt       # Nutrition info display
    ├── IngredientsTab.kt      # Servings + checkbox list
    └── InstructionsTab.kt     # Numbered steps
```

### 2. Create Recipe Repository
```
data/repository/
└── FakeRecipeRepository.kt    # Full recipe data with ingredients/steps
```

### 3. Update Navigation
- `RasoiNavHost.kt` line 108-115: Replace PlaceholderScreen with RecipeDetailScreen
- RecipeDetail receives `recipeId` from navigation arguments
- Add navigation to CookingMode and Chat

### 4. Update DI
- `DataModule.kt`: Bind FakeRecipeRepository to RecipeRepository interface

---

## PROJECT STATUS:

| # | Screen | Status | Notes |
|---|--------|--------|-------|
| 1 | Splash | ✅ Done | Logo, offline banner |
| 2 | Auth | ✅ Done | Google Sign-In |
| 3 | Onboarding | ✅ Done | 5-step DataStore |
| 4 | Home | ✅ Done | Weekly meal plan, bottom nav |
| 5 | **Recipe Detail** | ⏳ **Next** | Tabs: Ingredients/Instructions |
| 6 | Cooking Mode | Pending | Step-by-step with timer |
| 7 | Grocery List | Pending | Categorized, WhatsApp share |
| 8 | Favorites | Pending | Collections, grid view |
| 9 | Chat | Pending | AI assistant |
| 10 | Pantry Scan | Pending | Camera, expiry tracking |
| 11 | Stats | Pending | Streaks, achievements |
| 12 | Settings | Pending | Profile, preferences |

---

## DESIGN SYSTEM QUICK REF:

| Element | Light | Dark |
|---------|-------|------|
| Primary | `#FF6838` | `#FFB59C` |
| Secondary | `#5A822B` | `#A8D475` |
| Background | `#FDFAF4` | `#1C1B1F` |
| Vegetarian | Green dot | - |
| Non-Veg | Red dot | - |

| Token | Value |
|-------|-------|
| Spacing | 4, 8, 16, 24, 32, 48dp (use `spacing.xs`, `spacing.sm`, etc.) |
| Corners | 8dp (small), 16dp (medium), 24dp (large) |

---

## BUILD & RUN COMMANDS:

```bash
cd D:\Abhay\VibeCoding\KKB\android

# Build
.\gradlew build

# Run tests
.\gradlew test

# Install on device/emulator
.\gradlew installDebug

# Launch app via adb
"C:\Users\itsab\AppData\Local\Android\Sdk\platform-tools\adb.exe" shell am start -n com.rasoiai.app/com.rasoiai.app.MainActivity
```

---

*Last Updated: January 2025*
*Next Step: Recipe Detail Screen (Tabs: Ingredients/Instructions)*
