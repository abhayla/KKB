# Continuation Prompt for RasoiAI Project

Use this prompt to start a new conversation/context and continue the project from where we left off.

---

## PROMPT TO USE:

```
I am building **RasoiAI** - an AI-powered meal planning app for Indian families.

## Project Status

12 screens implemented (Splash → Auth → Onboarding → Home → Recipe Detail → Cooking Mode → Grocery List → Favorites → Chat → Pantry Scan → Stats → Settings). All wireframes complete (13 screens). Ready to implement Recipe Rules screen.

| Phase | Status | Document |
|-------|--------|----------|
| Requirements | ✅ Complete | `docs/requirements/RasoiAI Requirements.md` |
| Technical Design | ✅ Complete | `docs/design/RasoiAI Technical Design.md` |
| Architecture Decisions | ✅ Complete | `docs/design/Android Architecture Decisions.md` |
| Design System | ✅ Complete | `docs/design/RasoiAI Design System.md` |
| Screen Wireframes | ✅ Complete | `docs/design/RasoiAI Screen Wireframes.md` (v3.1, 13 screens) |
| Android Project Setup | ✅ Complete | `android/` folder |
| Splash Screen | ✅ Complete | `presentation/splash/` |
| Auth Screen | ✅ Complete | `presentation/auth/` |
| Onboarding Screen | ✅ Complete | `presentation/onboarding/` |
| Home Screen | ✅ Complete | `presentation/home/` |
| Recipe Detail | ✅ Complete | `presentation/recipedetail/` |
| Cooking Mode | ✅ Complete | `presentation/cookingmode/` |
| Grocery List | ✅ Complete | `presentation/grocery/` |
| Favorites | ✅ Complete | `presentation/favorites/` |
| Chat | ✅ Complete | `presentation/chat/` |
| Pantry Scan | ✅ Complete | `presentation/pantry/` |
| Stats | ✅ Complete | `presentation/stats/` |
| Settings | ✅ Complete | `presentation/settings/` |
| Recipe Rules Reqs | ✅ Complete | `docs/requirements/Recipe Rules Screen Requirements.md` |
| **Recipe Rules** | ⏳ **Next Step** | 4 tabs, rule management, nutrition goals |

## App Verified Working

- ✅ App builds successfully (`./gradlew build`)
- ✅ All tests pass (`./gradlew test`)
- ✅ Full navigation flow works
- ✅ Bottom navigation working (Home, Grocery, Chat, Favs, Stats)
- ✅ All 12 screens implemented and working

## Your Task

**Implement the Recipe Rules Screen** (Screen 13 in wireframes):

### Files to Create:
```
app/presentation/reciperules/
├── RecipeRulesScreen.kt           # Main composable with 4 tabs
├── RecipeRulesViewModel.kt        # State management, CRUD operations
└── components/
    ├── RulesTabBar.kt             # 4-tab bar (Recipe/Ingredient/Meal-Slot/Nutrition)
    ├── RecipeRulesList.kt         # List of recipe-based rules
    ├── IngredientRulesList.kt     # List of ingredient-based rules
    ├── MealSlotRulesList.kt       # List of meal-slot rules
    ├── NutritionGoalsList.kt      # List with progress bars
    ├── RuleCard.kt                # Reusable card for displaying rules
    ├── AddRuleBottomSheet.kt      # Bottom sheet for adding rules
    └── AddNutritionGoalSheet.kt   # Bottom sheet for nutrition goals

domain/model/
└── RecipeRule.kt                  # Domain models (RecipeRule, RuleType, etc.)

domain/repository/
└── RecipeRulesRepository.kt       # Repository interface

data/repository/
└── FakeRecipeRulesRepository.kt   # Mock rules data
```

### Update Files:
- `RasoiNavHost.kt` - Add RecipeRulesScreen route
- `Screen.kt` - Add RecipeRules route definition
- `DataModule.kt` - Bind FakeRecipeRulesRepository
- `SettingsScreen.kt` - Add navigation to Recipe Rules
- `HomeScreen.kt` - Add gear icon for quick access (optional)

### UI Components (from wireframes - Screen 13, line 1736):

1. **Top App Bar**: "Recipe Rules" with ← back button

2. **Tab Bar** (4 tabs):
   - 📖 Recipe - Include/exclude specific recipes
   - 🥕 Ingredient - Include/exclude ingredients
   - 🍽️ Meal-Slot - Lock recipes to meal times
   - 🥗 Nutrition - Weekly food-category goals

3. **Recipe Rules Tab**:
   - List of rules with: rule name, frequency, enforcement badge, edit button
   - Example: "Include Rajma weekly" | "At least 1x per week" | [Required] ● Active
   - "+ ADD RECIPE RULE" button at bottom

4. **Ingredient Rules Tab**:
   - Include rules (🥕) and Exclude rules (🚫)
   - Same card layout as recipe rules

5. **Meal-Slot Rules Tab**:
   - Shows recipe → meal mapping
   - Example: "Chai → Breakfast" | "Every day"

6. **Nutrition Goals Tab**:
   - Progress bars for weekly goals
   - Food categories: Green leafy, Citrus/Vitamin C, Iron-rich, etc.
   - Shows current progress (e.g., "4/7 days")

7. **Add Rule Bottom Sheet**:
   - Rule Type: Include/Exclude radio
   - Recipe/Ingredient search with suggestions
   - Frequency: "At least [X] times per [week]" OR specific days checkboxes
   - Enforcement: Required/Preferred radio
   - Save button

8. **Nutrition Goal Bottom Sheet**:
   - Food category dropdown
   - Weekly target input
   - Save button

### Domain Models (from requirements doc):

```kotlin
// RecipeRule.kt
enum class RuleType { RECIPE, INGREDIENT, MEAL_SLOT, NUTRITION }
enum class RuleAction { INCLUDE, EXCLUDE }
enum class RuleEnforcement { REQUIRED, PREFERRED }

data class RuleFrequency(
    val type: FrequencyType,
    val count: Int? = null,
    val specificDays: List<DayOfWeek>? = null
)

enum class FrequencyType { DAILY, TIMES_PER_WEEK, SPECIFIC_DAYS, NEVER }

data class RecipeRule(
    val id: String,
    val type: RuleType,
    val action: RuleAction,
    val targetId: String,           // Recipe ID or Ingredient ID
    val targetName: String,
    val frequency: RuleFrequency,
    val enforcement: RuleEnforcement,
    val mealSlot: MealType? = null, // For MEAL_SLOT type
    val isActive: Boolean = true,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class NutritionGoal(
    val id: String,
    val foodCategory: FoodCategory,
    val weeklyTarget: Int,
    val currentProgress: Int = 0,
    val isActive: Boolean = true
)

enum class FoodCategory(val displayName: String) {
    GREEN_LEAFY("Green leafy vegetables"),
    CITRUS_VITAMIN_C("Citrus/Vitamin C rich foods"),
    IRON_RICH("Iron-rich foods"),
    HIGH_PROTEIN("High protein foods"),
    CALCIUM_RICH("Calcium-rich foods"),
    FIBER_RICH("Fiber-rich foods"),
    OMEGA_3("Omega-3 rich foods"),
    ANTIOXIDANT("Antioxidant-rich foods")
}
```

### Mock Data for FakeRecipeRulesRepository:
- Recipe rules: Rajma (1x/week, Required), Moringa Curry (3x/week, Preferred), Chai (daily breakfast, Required)
- Ingredient rules: Include Spinach (2x/week), Exclude Bitter Gourd (never)
- Meal-slot rules: Chai → Breakfast (daily), Dosa → Weekend Breakfast
- Nutrition goals: Green leafy (4/7), Citrus (2/5), Iron-rich (5/6)

### Key Patterns (follow existing code):
- ViewModel: `StateFlow<UiState>` + `StateFlow<NavigationEvent?>`
- Screen receives callbacks, ViewModel handles logic
- Use `MaterialTheme.colorScheme` and `spacing` from theme
- Use `hiltViewModel()` for DI
- Bottom sheets with `ModalBottomSheet` from Material 3

### Access Points:
1. Settings Screen → "Recipe Rules" menu item under MEAL PREFERENCES
2. Home Screen → ⚙️ gear icon in header (optional quick access)

## Reference Files

| File | Path | Why |
|------|------|-----|
| Wireframes | `docs/design/RasoiAI Screen Wireframes.md` | Screen 13 design (line 1736+) |
| Requirements | `docs/requirements/Recipe Rules Screen Requirements.md` | Full spec with data models |
| Settings Screen | `app/presentation/settings/SettingsScreen.kt` | Most recent implementation pattern |
| Settings ViewModel | `app/presentation/settings/SettingsViewModel.kt` | State management pattern |
| Favorites Screen | `app/presentation/favorites/FavoritesScreen.kt` | Tab/list pattern reference |
| RasoiNavHost | `app/presentation/navigation/RasoiNavHost.kt` | Navigation setup |
| Screen.kt | `app/presentation/navigation/Screen.kt` | Route definitions |
| DataModule | `data/di/DataModule.kt` | DI bindings to update |
| Recipe Model | `domain/model/Recipe.kt` | Domain model pattern with enums |
| MealPlan Model | `domain/model/MealPlan.kt` | MealType enum reference |

## Working Directory
Project root: `D:/Abhay/VibeCoding/KKB`

Start by reading:
1. Screen 13 wireframes in `docs/design/RasoiAI Screen Wireframes.md` (line 1736)
2. Full requirements in `docs/requirements/Recipe Rules Screen Requirements.md`
3. Settings screen patterns in `app/presentation/settings/`

Then implement the Recipe Rules screen following the established patterns.
```

---

## QUICK REFERENCE

### Build Commands (use forward slashes `/` in bash):
```bash
cd "D:/Abhay/VibeCoding/KKB/android"
./gradlew build      # Build
./gradlew test       # Run tests
./gradlew installDebug  # Install on device
```

### Key Architecture:
- **ViewModel Pattern**: `UiState` data class + `NavigationEvent` sealed class
- **DI**: Hilt with `@HiltViewModel` and `@Inject constructor`
- **State**: `MutableStateFlow` with `.update { it.copy(...) }`
- **Navigation**: Callbacks passed to Screen, ViewModel emits events
- **Tabs**: Use `TabRow` + `HorizontalPager` for swipeable tabs

### Design System:
| Element | Light | Dark |
|---------|-------|------|
| Primary | `#FF6838` | `#FFB59C` |
| Secondary | `#5A822B` | `#A8D475` |
| Background | `#FDFAF4` | `#1C1B1F` |

| Token | Usage |
|-------|-------|
| Spacing | `spacing.xs` (4dp), `spacing.sm` (8dp), `spacing.md` (16dp), `spacing.lg` (24dp) |
| Corners | 8dp (small), 16dp (medium), 24dp (large) |

### After Implementation:
1. Build and test: `./gradlew build && ./gradlew test`
2. Update CLAUDE.md status table (Recipe Rules → ✅ Complete)
3. Update this file for next phase (Backend integration)
4. Commit with message format: "Implement Recipe Rules screen with 4-tab rule management"

---

## PREVIOUS SESSIONS SUMMARY

### Session 1-10: Core UI Implementation
- Implemented all 12 core screens
- Established ViewModel pattern with StateFlow
- Set up Hilt DI, Navigation Compose
- All screens follow wireframe designs

### Session 11: Wireframe Review & Recipe Rules Design
- Reviewed all 12 wireframes with user approval
- Redesigned Screen 4 (Home) with 3-level locking system
- Added lock indicator to Screen 5 (Recipe Detail)
- Created Screen 13 (Recipe Rules) wireframe with:
  - 4 tabs: Recipe, Ingredient, Meal-Slot, Nutrition
  - Required vs Preferred enforcement
  - Food-category based nutrition goals
  - Dual access points (Settings + Home)
- Created comprehensive requirements document

### Next Phase After Recipe Rules:
- Backend API integration
- Real data connections
- Firebase Auth setup with `google-services.json`
