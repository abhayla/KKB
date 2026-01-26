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
    ├── StreakCard.kt           # Current streak display with fire emoji
    ├── CalendarView.kt         # Monthly calendar with cooking indicators
    ├── MonthlyStatsRow.kt      # Meals cooked, new recipes, avg rating
    ├── AchievementCard.kt      # Badge/achievement display
    ├── ChallengeCard.kt        # Weekly challenge with progress bar
    └── LeaderboardSection.kt   # Top cookers leaderboard

domain/model/
└── CookingStats.kt             # Stats domain models (Streak, Achievement, Challenge)

domain/repository/
└── StatsRepository.kt          # Repository interface for cooking stats

data/repository/
└── FakeStatsRepository.kt      # Mock stats data
```

### Update Files:
- `RasoiNavHost.kt` - Replace placeholder with StatsScreen
- `DataModule.kt` - Bind FakeStatsRepository

### UI Components (from wireframes - Screen 11, line 1272):
1. Top app bar ("My Cooking Stats", ← back)
2. Current Streak card (fire emoji, days count, best streak)
3. Calendar view:
   - Month/year header with navigation
   - Weekly grid with cooking indicators (emoji per day)
   - Today highlighted
4. This Month stats row (3 cards):
   - Meals Cooked (count)
   - New Recipes (count)
   - Avg Rating (number)
5. Achievements section:
   - Horizontal scroll of badges
   - [View All] button
6. Weekly Challenge card:
   - Challenge name and description
   - Progress bar (e.g., 2/5)
   - Reward badge
7. Leaderboard section (top 3):
   - Rank, name, meals count
8. Bottom navigation bar

### Key Features:
- Cooking streak tracking (consecutive days)
- Monthly calendar with cooking history
- Achievement badges (First Meal, 7-Day Streak, Master Chef, 50 Meals)
- Weekly challenges with progress
- Leaderboard (mock data for now)

### Key Patterns (follow existing code):
- ViewModel: `StateFlow<UiState>` + `StateFlow<NavigationEvent?>`
- Screen receives callbacks, ViewModel handles logic
- Use `MaterialTheme.colorScheme` and `spacing` from theme
- Use `hiltViewModel()` for DI

## Reference Files

| File | Path | Why |
|------|------|-----|
| Wireframes | `docs/design/RasoiAI Screen Wireframes.md` | Screen 11 design (line 1272+) |
| Pantry Screen | `app/presentation/pantry/PantryScreen.kt` | Recent implementation pattern |
| Pantry ViewModel | `app/presentation/pantry/PantryViewModel.kt` | State management pattern |
| RasoiNavHost | `app/presentation/navigation/RasoiNavHost.kt` | Navigation setup |
| DataModule | `data/di/DataModule.kt` | DI bindings to update |

## Working Directory
Project root: `D:/Abhay/VibeCoding/KKB`

Start by reading Screen 11 wireframes in `docs/design/RasoiAI Screen Wireframes.md` (line 1272), then implement the Stats screen following the patterns in `app/presentation/pantry/`.
```

---

## QUICK START PROMPT (Shorter):

```
I'm building **RasoiAI** - an AI meal planning app for Indian families.

**COMPLETED:** Splash, Auth, Onboarding, Home, Recipe Detail, Cooking Mode, Grocery List, Favorites (8 screens). All tests pass.

**NEXT:** Implement Chat Screen (Screen 9)

**Read first:** `docs/design/RasoiAI Screen Wireframes.md` - Search for "Screen 9: Chat" (line 1111)

**Create these files:**
1. `app/presentation/chat/ChatScreen.kt` - Message list + input bar
2. `app/presentation/chat/ChatViewModel.kt` - State management
3. `app/presentation/chat/components/` - ChatMessageItem, QuickActionChips, ChatInputBar, RecipeSuggestionCard
4. `domain/model/ChatMessage.kt` - Message data class
5. `domain/repository/ChatRepository.kt` - Repository interface
6. `data/repository/FakeChatRepository.kt` - Mock chat with AI responses

**Update:**
- `RasoiNavHost.kt` - Wire up ChatScreen (replace placeholder)
- `DataModule.kt` - Provide FakeChatRepository

**Chat UI:**
- Top bar: "RasoiAI Assistant" with back arrow and menu
- Message bubbles (AI left with 🤖, User right with 👤)
- Recipe suggestion cards with [View Recipe] buttons
- Quick action chips (time-based: breakfast/lunch/dinner/snack)
- Input bar with text field, attachment 📎, voice 🎤, send buttons
- Bottom navigation

**Mock AI behavior:**
- "paneer" → suggest Palak Paneer, Paneer Butter Masala
- "breakfast" → suggest Aloo Paratha, Idli, Dosa
- "quick" → suggest recipes under 30 min
- Default → helpful response with random suggestions

**Reference:** `app/presentation/favorites/` for ViewModel/Screen patterns
**Domain models:** `domain/model/Recipe.kt`

Project root: `D:/Abhay/VibeCoding/KKB`
```

---

## CHAT WIREFRAME SUMMARY:

```
┌─────────────────────────────────────┐
│  ←  RasoiAI Assistant           ⋮   │  ← Top bar with back + menu
│─────────────────────────────────────│
│                                     │
│  ┌─────────────────────────────────┐│
│  │ 🤖 RasoiAI                      ││  ← AI message (left)
│  │                                 ││
│  │ Hi! I'm your AI cooking         ││
│  │ assistant. How can I help you   ││
│  │ today?                          ││
│  │                                 ││
│  │ Quick actions:                  ││
│  │ [Suggest dinner] [Swap a meal]  ││  ← Quick action chips
│  │ [What can I cook?] [Diet tips]  ││
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────────┐│
│  │                          👤 You ││  ← User message (right)
│  │                                 ││
│  │ What can I make with paneer,    ││
│  │ tomatoes and spinach?           ││
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────────┐│
│  │ 🤖 RasoiAI                      ││
│  │                                 ││
│  │ Great ingredients! Here are     ││
│  │ some recipes you can make:      ││
│  │                                 ││
│  │ 1. Palak Paneer (40 min)        ││  ← Recipe suggestions
│  │ 2. Paneer Tikka Masala (35 min) ││
│  │ 3. Paneer Bhurji (20 min)       ││
│  │                                 ││
│  │ [View Palak Paneer]             ││  ← Clickable buttons
│  │ [View Paneer Tikka Masala]      ││
│  │ [View Paneer Bhurji]            ││
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────────┐│
│  │ Type a message...     📎  🎤  ➤ ││  ← Input bar
│  └─────────────────────────────────┘│
│─────────────────────────────────────│
│  🏠     📋     💬     ❤️     📊    │
│ Home  Grocery  Chat  Favs  Stats    │
└─────────────────────────────────────┘
```

### More Menu (⋮):
```
┌─────────────────────────────────────┐
│   ┌─────────────────────────────┐   │
│   │ 🗑️ Clear Chat History       │   │
│   └─────────────────────────────┘   │
│                                     │
│   ┌─────────────────────────────┐   │
│   │ ⚙️ Chat Settings            │   │
│   └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

### Time-Based Quick Actions:
| Time | Actions |
|------|---------|
| Morning (6-11 AM) | "Quick breakfast", "Healthy start" |
| Afternoon (11 AM-4 PM) | "Lunch ideas", "Light meal" |
| Evening (4-9 PM) | "Dinner suggestions", "Family meal" |
| Night (9 PM+) | "Light snack", "Quick bite" |

---

## PROJECT STATUS:

| # | Screen | Status | Notes |
|---|--------|--------|-------|
| 1 | Splash | ✅ Done | Logo, offline banner |
| 2 | Auth | ✅ Done | Google Sign-In |
| 3 | Onboarding | ✅ Done | 5-step DataStore |
| 4 | Home | ✅ Done | Weekly meal plan, bottom nav |
| 5 | Recipe Detail | ✅ Done | Tabs: Ingredients/Instructions |
| 6 | Cooking Mode | ✅ Done | Step-by-step with timer, rating |
| 7 | Grocery List | ✅ Done | Categorized, WhatsApp share |
| 8 | Favorites | ✅ Done | Collections, 2-column grid, filters, reorder |
| 9 | **Chat** | ⏳ **Next** | AI assistant, chat history |
| 10 | Pantry Scan | Pending | Camera, expiry tracking |
| 11 | Stats | Pending | Streaks, achievements |
| 12 | Settings | Pending | Profile, preferences |

---

## FILES TO REFERENCE:

| File | Path | Priority | Description |
|------|------|----------|-------------|
| Screen Wireframes | `docs/design/RasoiAI Screen Wireframes.md` | **HIGH** | Chat design (Screen 9, line 1111) |
| Favorites Screen | `app/presentation/favorites/FavoritesScreen.kt` | **HIGH** | Recent screen pattern |
| Favorites ViewModel | `app/presentation/favorites/FavoritesViewModel.kt` | **HIGH** | State management pattern |
| Recipe Model | `domain/model/Recipe.kt` | **HIGH** | Recipe data structure |
| FakeRecipeRepository | `data/repository/FakeRecipeRepository.kt` | **HIGH** | Recipe data for suggestions |
| Grocery Screen | `app/presentation/grocery/GroceryScreen.kt` | MEDIUM | Bottom nav pattern |
| RasoiNavHost | `app/presentation/navigation/RasoiNavHost.kt` | **HIGH** | Navigation setup |
| DataModule | `data/di/DataModule.kt` | **HIGH** | DI bindings to update |
| Theme | `app/presentation/theme/` | MEDIUM | Colors, spacing |
| CLAUDE.md | Root | MEDIUM | Project overview |

---

## DESIGN SYSTEM QUICK REF:

| Element | Light | Dark |
|---------|-------|------|
| Primary | `#FF6838` | `#FFB59C` |
| Secondary | `#5A822B` | `#A8D475` |
| Background | `#FDFAF4` | `#1C1B1F` |
| AI Message Bg | `surfaceVariant` | - |
| User Message Bg | `primaryContainer` | - |

| Token | Value |
|-------|-------|
| Spacing | 4, 8, 16, 24, 32, 48dp (use `spacing.xs`, `spacing.sm`, etc.) |
| Corners | 8dp (small), 16dp (medium), 24dp (large) |

---

## BUILD & RUN COMMANDS:

**Note:** Use forward slashes `/` in bash (not backslashes `\`). The shell is Unix-style bash.

```bash
cd "D:/Abhay/VibeCoding/KKB/android"

# Build
./gradlew build

# Run tests
./gradlew test

# Install on device/emulator
./gradlew installDebug
```

---

## IMPLEMENTATION CHECKLIST FOR CHAT:

### 1. Create Domain Models
- [ ] `domain/model/ChatMessage.kt` - ChatMessage + RecipeSuggestion data classes

### 2. Create Repository
- [ ] `domain/repository/ChatRepository.kt` - Interface
- [ ] `data/repository/FakeChatRepository.kt` - Mock implementation with AI responses

### 3. Create Chat Screen Files
```
app/presentation/chat/
├── ChatScreen.kt              # Main composable
├── ChatViewModel.kt           # State management
└── components/
    ├── ChatMessageItem.kt     # AI/User message bubbles
    ├── QuickActionChips.kt    # Time-based suggestions
    ├── ChatInputBar.kt        # Text input + buttons
    └── RecipeSuggestionCard.kt # Clickable recipe cards
```

### 4. Update Navigation
- [ ] `RasoiNavHost.kt`: Replace PlaceholderScreen with ChatScreen
- [ ] Add navigation to Recipe Detail from recipe suggestion cards

### 5. Update DI
- [ ] `DataModule.kt`: Bind FakeChatRepository to ChatRepository

### 6. Key Features
- [ ] Welcome message from AI on first load
- [ ] Message list with reverse layout (newest at bottom)
- [ ] AI message bubbles (left, with 🤖 icon)
- [ ] User message bubbles (right, with 👤 icon)
- [ ] Quick action chips that change based on time of day
- [ ] Text input with send button
- [ ] Voice/attachment buttons (placeholder - just show toast)
- [ ] Recipe suggestion cards with [View Recipe] button
- [ ] Navigate to Recipe Detail when clicking recipe
- [ ] Clear chat history in overflow menu
- [ ] Bottom navigation integration

---

## PREVIOUS IMPLEMENTATION REFERENCE (Favorites):

The Favorites screen was implemented with these files as a reference pattern:

### Domain Layer
- `domain/model/FavoriteCollection.kt` - Data classes
- `domain/repository/FavoritesRepository.kt` - Repository interface

### Data Layer
- `data/repository/FakeFavoritesRepository.kt` - Mock implementation

### Presentation Layer
- `app/presentation/favorites/FavoritesScreen.kt` - Main screen
- `app/presentation/favorites/FavoritesViewModel.kt` - ViewModel with UiState + NavigationEvent
- `app/presentation/favorites/components/` - UI components

Follow this same pattern for Chat screen.

---

*Last Updated: January 26, 2026*
*Next Step: Chat Screen (AI assistant, chat history, recipe suggestions)*
