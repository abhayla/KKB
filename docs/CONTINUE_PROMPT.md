# Continuation Prompt for RasoiAI Project

Use this prompt to start a new conversation/context and continue the project from where we left off.

---

## PROMPT TO USE:

```
I am building **RasoiAI** - an AI-powered meal planning app for Indian families.

## Project Status

Splash, Auth, and Onboarding screens are COMPLETE. Ready for Home screen.

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
| **Home Screen** | ⏳ **Next Step** | Weekly meal plan view |

## Screens Implemented

| Screen | Key Files | Status |
|--------|-----------|--------|
| Splash | `SplashScreen.kt`, `SplashViewModel.kt` | ✅ Complete |
| Auth | `AuthScreen.kt`, `AuthViewModel.kt`, `GoogleAuthClient.kt` | ✅ Complete |
| Onboarding | `OnboardingScreen.kt`, `OnboardingViewModel.kt`, `UserPreferencesDataStore.kt` | ✅ Complete |
| Home | - | ⏳ Next |

## Architecture Patterns Established

- **ViewModel**: StateFlow + UiState data class + NavigationEvent sealed class
- **DI**: Hilt with @HiltViewModel, @Inject constructor
- **Persistence**: DataStore for preferences, Room for data
- **Navigation**: Navigation Compose with Screen sealed class
- **Theme**: MaterialTheme with custom spacing via CompositionLocal

## Key Documents to Read

1. **Screen Wireframes** (`docs/design/RasoiAI Screen Wireframes.md`) - **Screen 4: Home** design
2. **Domain Models** (`domain/model/`) - `MealPlan.kt`, `Recipe.kt` for data structures
3. **Onboarding** (`app/presentation/onboarding/`) - Reference for patterns

## Your Task

**Implement the Home Screen** (Screen 4 - Weekly Meal Plan):

### UI Components:
- Top app bar (hamburger, "RasoiAI", notifications, profile icons)
- Festival banner (conditional, when festival is near)
- Week date selector (Mon-Sun with dates)
- 4 meal sections: Breakfast, Lunch, Dinner, Snacks
- Recipe cards with: name, time, calories, swap/lock icons
- Bottom navigation bar (Home, Grocery, Chat, Favorites, Stats)

### Features:
- Multiple recipes per meal type
- Individual recipe swap (replace with similar)
- Individual recipe lock (protect from regenerate)
- Refresh button → Bottom sheet (regenerate day/week)
- Tap recipe → Navigate to Recipe Detail

### Data:
- Use placeholder/mock data for now (backend not ready)
- Create `MealPlanRepository` with fake data
- Display current week's meals

### Bottom Navigation:
Create reusable `RasoiBottomNavigation` composable:
- Home (filled home icon)
- Grocery (list icon)
- Chat (chat bubble icon)
- Favorites (heart icon)
- Stats (chart icon)

## Reference Files

```
android/app/src/main/java/com/rasoiai/app/presentation/
├── onboarding/          # Reference for ViewModel/Screen patterns
├── navigation/
│   ├── Screen.kt        # All screen routes defined
│   └── RasoiNavHost.kt  # Navigation setup
└── theme/               # Colors, spacing, typography

android/domain/src/main/java/com/rasoiai/domain/model/
├── MealPlan.kt          # MealPlan, MealPlanDay, MealItem models
├── Recipe.kt            # Recipe, CuisineType, DietaryTag models
└── User.kt              # UserPreferences for filtering
```

## Working Directory
Project root: `D:/Abhay/VibeCoding/KKB`

Start by reading Screen Wireframes (Screen 4: Home), then implement HomeScreen with HomeViewModel.
```

---

## QUICK START PROMPT (Shorter):

```
I'm building **RasoiAI** - an AI meal planning app for Indian families.

**COMPLETED:** Splash, Auth, Onboarding screens with DataStore persistence.

**NEXT:** Implement Home Screen (Weekly Meal Plan)

**Read:** `docs/design/RasoiAI Screen Wireframes.md` - Screen 4: Home

**Home Screen Features:**
- Week selector (Mon-Sun)
- 4 meal sections (Breakfast/Lunch/Dinner/Snacks)
- Recipe cards with swap/lock icons
- Festival banner
- Bottom navigation (Home, Grocery, Chat, Favorites, Stats)
- Refresh → regenerate day/week

**Reference:** `app/presentation/onboarding/` for patterns

**Create:**
1. `HomeScreen.kt` + `HomeViewModel.kt`
2. `RasoiBottomNavigation.kt` (reusable)
3. Mock data repository for meal plans

Project root: `D:/Abhay/VibeCoding/KKB`
```

---

## FILES TO REFERENCE:

| File | Path | Priority | Description |
|------|------|----------|-------------|
| Screen Wireframes | `docs/design/RasoiAI Screen Wireframes.md` | **HIGH** | Home screen design (Screen 4) |
| MealPlan Model | `domain/model/MealPlan.kt` | **HIGH** | Data structures for meals |
| Recipe Model | `domain/model/Recipe.kt` | **HIGH** | Recipe data structure |
| Onboarding Screen | `app/presentation/onboarding/OnboardingScreen.kt` | **HIGH** | Pattern reference |
| Onboarding ViewModel | `app/presentation/onboarding/OnboardingViewModel.kt` | **HIGH** | State management pattern |
| Navigation | `app/presentation/navigation/` | MEDIUM | Screen routes, NavHost |
| Theme | `app/presentation/theme/` | MEDIUM | Colors, spacing |
| CLAUDE.md | Root | MEDIUM | Project overview |

---

## HOME SCREEN WIREFRAME SUMMARY:

```
┌─────────────────────────────────────┐
│  ☰  RasoiAI                 🔔  👤  │  ← Top bar
│─────────────────────────────────────│
│  ┌─────────────────────────────────┐│
│  │ 🎉 Festival Banner (optional)  ││  ← Conditional
│  └─────────────────────────────────┘│
│  This Week's Menu                   │
│  Jan 20 - 26                        │
│  ┌─Mo─┬─Tu─┬─We─┬─Th─┬─Fr─┬─Sa─┬─Su┐│  ← Week selector
│  │[●] │ ○  │ ○  │ ○  │ ○  │ ○  │ ○ ││
│  └────┴────┴────┴────┴────┴────┴───┘│
│  Monday, Jan 20          [🔄 Refresh]│
│                                     │
│  ┌─────────────────────────────────┐│
│  │ 🌅 BREAKFAST            [+ Add] ││  ← Meal section
│  │ ● Poha         20 min  280cal [⟲]││  ← Recipe with swap
│  │ ● Chai          5 min   80cal [⟲]││
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────────┐│
│  │ ☀️ LUNCH                [+ Add] ││
│  │ ● Dal Tadka    25 min  180cal [⟲]││
│  │ ● Jeera Rice   15 min  220cal [⟲]││
│  │ ● Roti (4)     20 min  320cal [⟲]││
│  └─────────────────────────────────┘│
│  ... (Dinner, Snacks similar)       │
│─────────────────────────────────────│
│  🏠     📋     💬     ❤️     📊    │  ← Bottom nav
│ Home  Grocery  Chat  Favs  Stats    │
└─────────────────────────────────────┘
```

### Recipe Card Actions:
- **Tap recipe** → View Recipe Detail
- **[⟲] Swap** → Bottom sheet with alternatives
- **🔒 Lock** → Protect from regeneration
- **[+ Add]** → Add recipe to meal

### Refresh Options (Bottom Sheet):
- Regenerate this day only
- Regenerate entire week
- Note: Locked recipes preserved

---

## IMPLEMENTATION CHECKLIST:

### 1. Create Home Screen Files
```
app/presentation/home/
├── HomeScreen.kt           # Main composable
├── HomeViewModel.kt        # State management
└── components/
    ├── MealSection.kt      # Breakfast/Lunch/Dinner/Snacks
    ├── RecipeCard.kt       # Individual recipe item
    ├── WeekSelector.kt     # Day picker
    └── FestivalBanner.kt   # Optional banner
```

### 2. Create Bottom Navigation
```
app/presentation/common/
└── RasoiBottomNavigation.kt  # Reusable bottom nav
```

### 3. Create Mock Repository
```
data/repository/
└── FakeMealPlanRepository.kt  # Mock data for testing
```

### 4. Update Navigation
- Add HomeScreen to RasoiNavHost
- Integrate bottom navigation

---

## PROJECT STATUS:

| # | Screen | Status | Notes |
|---|--------|--------|-------|
| 1 | Splash | ✅ Done | Logo, offline banner |
| 2 | Auth | ✅ Done | Google Sign-In |
| 3 | Onboarding | ✅ Done | 5-step DataStore |
| 4 | **Home** | ⏳ **Next** | Weekly meal plan |
| 5 | Recipe Detail | Pending | - |
| 6 | Cooking Mode | Pending | - |
| 7 | Grocery List | Pending | - |
| 8 | Favorites | Pending | - |
| 9 | Chat | Pending | - |
| 10 | Pantry Scan | Pending | - |
| 11 | Stats | Pending | - |
| 12 | Settings | Pending | - |

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
| Spacing | 4, 8, 16, 24, 32, 48dp |
| Corners | 8dp (small), 16dp (medium), 24dp (large) |

---

*Last Updated: January 2025*
*Next Step: Home Screen (Weekly Meal Plan)*
