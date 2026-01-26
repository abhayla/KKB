# Continuation Prompt for RasoiAI Project

Use this prompt to start a new conversation/context and continue the project from where we left off.

---

## PROMPT TO USE:

```
I am building **RasoiAI** - an AI-powered meal planning app for Indian families.

## Project Status

10 screens complete (Splash → Auth → Onboarding → Home → Recipe Detail → Cooking Mode → Grocery List → Favorites → Chat → Pantry Scan). App builds and all tests pass. Ready for Stats screen implementation.

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
| Recipe Detail | ✅ Complete | `presentation/recipedetail/` |
| Cooking Mode | ✅ Complete | `presentation/cookingmode/` |
| Grocery List | ✅ Complete | `presentation/grocery/` |
| Favorites | ✅ Complete | `presentation/favorites/` |
| Chat | ✅ Complete | `presentation/chat/` |
| Pantry Scan | ✅ Complete | `presentation/pantry/` |
| **Stats** | ⏳ **Next Step** | Cooking streak, leaderboards, achievements |

## App Verified Working

- ✅ App builds successfully (`./gradlew build`)
- ✅ All tests pass (`./gradlew test`)
- ✅ Full navigation flow works
- ✅ Bottom navigation working (Home, Grocery, Chat, Favs, Stats)
- ✅ All 10 screens cross-checked against wireframes

## Your Task

**Implement the Stats Screen** (Screen 11 in wireframes):

### Files to Create:
```
app/presentation/stats/
├── StatsScreen.kt              # Main composable with stats display
├── StatsViewModel.kt           # State management, stats data
└── components/
    ├── StreakCard.kt           # Current streak display (🔥 12 days, best streak)
    ├── CookingCalendar.kt      # Monthly calendar with cooking indicators (🍳)
    ├── MonthlyStatsRow.kt      # 3 stat cards (Meals Cooked, New Recipes, Avg Rating)
    ├── AchievementCard.kt      # Badge display (🏅 First Meal, 🥇 7-Day Streak, etc.)
    ├── ChallengeCard.kt        # Weekly challenge with progress bar
    └── LeaderboardSection.kt   # Top 3 leaderboard (🥇🥈🥉)

domain/model/
└── CookingStats.kt             # Domain models (CookingStreak, Achievement, Challenge, LeaderboardEntry)

domain/repository/
└── StatsRepository.kt          # Repository interface

data/repository/
└── FakeStatsRepository.kt      # Mock stats data
```

### Update Files:
- `RasoiNavHost.kt` - Replace placeholder with StatsScreen
- `DataModule.kt` - Bind FakeStatsRepository

### UI Components (from wireframes - Screen 11, line 1272):

1. **Top App Bar**: "My Cooking Stats" with ← back button

2. **Current Streak Card**:
   - 🔥 emoji with "CURRENT STREAK"
   - Large number (e.g., "12 days")
   - Motivational text "Keep cooking to extend!"
   - 🏆 Best streak record

3. **Calendar Section**:
   - Month/Year header with ◀ ▶ navigation + [Today] button
   - 7-column grid (Su Mo Tu We Th Fr Sa)
   - 🍳 emoji on days with cooking
   - Today highlighted with [●]
   - Empty days show ○

4. **This Month Stats Row** (3 cards):
   - Meals Cooked (count)
   - New Recipes (count)
   - Avg Rating (decimal)

5. **Achievements Section**:
   - "ACHIEVEMENTS" header + [View All]
   - Horizontal scroll of badges:
     - 🏅 First Meal
     - 🥇 7-Day Streak
     - 👨‍🍳 Master Chef
     - 🌟 50 Meals

6. **Weekly Challenge Card**:
   - 🏆 Challenge name (e.g., "South Indian Week")
   - Description: "Cook 5 South Indian dishes"
   - Progress bar with fraction (2/5)
   - Reward: "Explorer Badge"
   - [Join] button

7. **Leaderboard Section**:
   - "LEADERBOARD" header + [View All]
   - Top 3 entries:
     - 🥇 Name + meals count
     - 🥈 You (highlighted) + meals count
     - 🥉 Name + meals count

8. **Bottom Navigation Bar**

### Domain Models to Create:

```kotlin
// CookingStats.kt
data class CookingStreak(
    val currentStreak: Int,
    val bestStreak: Int,
    val lastCookingDate: LocalDate?
)

data class MonthlyStats(
    val mealsCooked: Int,
    val newRecipes: Int,
    val averageRating: Float
)

data class CookingDay(
    val date: LocalDate,
    val didCook: Boolean,
    val mealsCount: Int = 0
)

data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val isUnlocked: Boolean,
    val unlockedDate: LocalDate? = null
)

data class WeeklyChallenge(
    val id: String,
    val name: String,
    val description: String,
    val targetCount: Int,
    val currentProgress: Int,
    val rewardBadge: String,
    val isJoined: Boolean
)

data class LeaderboardEntry(
    val rank: Int,
    val userName: String,
    val mealsCount: Int,
    val isCurrentUser: Boolean
)
```

### Mock Data for FakeStatsRepository:
- Current streak: 12 days, Best: 23 days
- This month: 45 meals, 12 new recipes, 4.2 avg rating
- Calendar: Random cooking days in current month
- Achievements: 4 unlocked (First Meal, 7-Day Streak, Master Chef, 50 Meals)
- Challenge: "South Indian Week" - 2/5 progress
- Leaderboard: 3 entries with current user in 2nd place

### Key Patterns (follow existing code):
- ViewModel: `StateFlow<UiState>` + `StateFlow<NavigationEvent?>`
- Screen receives callbacks, ViewModel handles logic
- Use `MaterialTheme.colorScheme` and `spacing` from theme
- Use `hiltViewModel()` for DI

## Reference Files

| File | Path | Why |
|------|------|-----|
| Wireframes | `docs/design/RasoiAI Screen Wireframes.md` | Screen 11 design (line 1272+) |
| Pantry Screen | `app/presentation/pantry/PantryScreen.kt` | Most recent implementation pattern |
| Pantry ViewModel | `app/presentation/pantry/PantryViewModel.kt` | State management pattern |
| RasoiNavHost | `app/presentation/navigation/RasoiNavHost.kt` | Navigation setup |
| DataModule | `data/di/DataModule.kt` | DI bindings to update |
| PantryItem Model | `domain/model/PantryItem.kt` | Domain model pattern |

## Working Directory
Project root: `D:/Abhay/VibeCoding/KKB`

Start by reading Screen 11 wireframes in `docs/design/RasoiAI Screen Wireframes.md` (line 1272), then implement the Stats screen following the patterns in `app/presentation/pantry/`.
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
2. Update CLAUDE.md status table
3. Update this file for next screen (Settings)
4. Commit with message format: "Implement [Screen] screen with [key features]"
