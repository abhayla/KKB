# Continuation Prompt for RasoiAI Project

Use this prompt to start a new conversation/context and continue the project from where we left off.

---

## PROMPT TO USE:

```
I am building **RasoiAI** - an AI-powered meal planning app for Indian families.

## Project Status

Core screens (Splash → Auth → Onboarding → Home → Recipe Detail → Cooking Mode → Grocery List → Favorites) are COMPLETE. App builds and all tests pass. Ready for Chat screen implementation.

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
| **Chat** | ⏳ **Next Step** | AI assistant, chat history |

## App Verified Working

- ✅ App builds successfully (`.\gradlew build`)
- ✅ All tests pass (`.\gradlew test`)
- ✅ Full navigation flow works: Splash → Auth → Onboarding → Home → Recipe Detail → Cooking Mode
- ✅ Grocery List with categorized items, WhatsApp share, checkbox toggle
- ✅ Favorites screen with collections, 2-column grid, filters, reorder mode
- ✅ Bottom navigation working (Home, Grocery, Chat, Favs, Stats)
- ✅ All 8 screens cross-checked against wireframes (all match)

## Your Task

**Implement the Chat Screen** (Screen 9 in wireframes):

### Files to Create:
```
app/presentation/chat/
├── ChatScreen.kt              # Main composable with message list
├── ChatViewModel.kt           # State management, AI interaction
└── components/
    ├── ChatMessageItem.kt     # User/AI message bubbles
    ├── QuickActionChips.kt    # Suggested actions (Suggest dinner, etc.)
    ├── ChatInputBar.kt        # Text input, voice button, attachment
    └── RecipeSuggestionCard.kt # Recipe card in chat

domain/model/
└── ChatMessage.kt             # Chat message domain model

domain/repository/
└── ChatRepository.kt          # Repository interface for chat history

data/repository/
└── FakeChatRepository.kt      # Mock chat data and responses
```

### Update Files:
- `RasoiNavHost.kt` - Replace placeholder with ChatScreen
- `DataModule.kt` - Bind FakeChatRepository

### UI Components (from wireframes - Screen 9, line 1111):
1. Top app bar ("RasoiAI Assistant", ⋮ menu)
2. Chat messages list (scrollable):
   - AI messages (left aligned, 🤖 icon)
   - User messages (right aligned)
   - Recipe suggestion cards with [View Recipe] buttons
3. Quick action chips (time-based):
   - Morning: "Quick breakfast", "Healthy start"
   - Afternoon: "Lunch ideas", "Light meal"
   - Evening: "Dinner suggestions", "Family meal"
   - Night: "Light snack", "Quick bite"
4. Text input bar with:
   - Text field placeholder
   - 📎 Attachment button (for pantry photos)
   - 🎤 Voice input button
5. Bottom navigation bar

### Key Features:
- Chat history persisted from previous sessions
- Clear Chat option in menu
- Recipe suggestions with clickable cards
- Time-based quick actions

### Key Patterns (follow existing code):
- ViewModel: `StateFlow<UiState>` + `StateFlow<NavigationEvent?>`
- Screen receives callbacks, ViewModel handles logic
- Use `MaterialTheme.colorScheme` and `spacing` from theme
- Use `hiltViewModel()` for DI
- Use LazyColumn for message list (reverse layout)

## Reference Files

| File | Path | Why |
|------|------|-----|
| Wireframes | `docs/design/RasoiAI Screen Wireframes.md` | Screen 9 design (line 1111+) |
| Favorites Screen | `app/presentation/favorites/FavoritesScreen.kt` | Recent implementation reference |
| Favorites ViewModel | `app/presentation/favorites/FavoritesViewModel.kt` | State pattern reference |
| Grocery Screen | `app/presentation/grocery/GroceryScreen.kt` | Bottom nav pattern |
| RasoiNavHost | `app/presentation/navigation/RasoiNavHost.kt` | Navigation setup |
| Recipe Model | `domain/model/Recipe.kt` | Recipe data structure |

## Domain Models to Create

```kotlin
// ChatMessage.kt
data class ChatMessage(
    val id: String,
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long,
    val recipeSuggestions: List<RecipeSuggestion>? = null
)

data class RecipeSuggestion(
    val recipeId: String,
    val recipeName: String,
    val cookTimeMinutes: Int,
    val imageUrl: String?
)
```

## Working Directory
Project root: `D:/Abhay/VibeCoding/KKB`

Start by reading Screen 9 wireframes in `docs/design/RasoiAI Screen Wireframes.md` (line 1111), then implement the Chat screen.
```

---

## QUICK START PROMPT (Shorter):

```
I'm building **RasoiAI** - an AI meal planning app for Indian families.

**COMPLETED:** Splash, Auth, Onboarding, Home, Recipe Detail, Cooking Mode, Grocery List, Favorites. All screens verified against wireframes. All tests pass.

**NEXT:** Implement Chat Screen (Screen 9)

**Read first:** `docs/design/RasoiAI Screen Wireframes.md` - Search for "Screen 9: Chat" (line 1111)

**Create these files:**
1. `app/presentation/chat/ChatScreen.kt` - Message list + input bar
2. `app/presentation/chat/ChatViewModel.kt` - State management
3. `app/presentation/chat/components/` - ChatMessageItem, QuickActionChips, ChatInputBar
4. `data/repository/FakeChatRepository.kt` - Mock chat data

**Update:**
- `RasoiNavHost.kt` - Wire up ChatScreen (replace placeholder)
- `DataModule.kt` - Provide FakeChatRepository

**Chat UI:**
- Top bar: "RasoiAI Assistant" with menu
- Message bubbles (AI left, User right)
- Recipe suggestion cards with [View Recipe]
- Quick action chips (time-based suggestions)
- Input bar with text field, attachment, voice buttons

**Reference:** `app/presentation/favorites/` for recent ViewModel/Screen patterns
**Domain models:** `domain/model/Recipe.kt` (Recipe structure)

Project root: `D:/Abhay/VibeCoding/KKB`
```

---

## FILES TO REFERENCE:

| File | Path | Priority | Description |
|------|------|----------|-------------|
| Screen Wireframes | `docs/design/RasoiAI Screen Wireframes.md` | **HIGH** | Chat design (Screen 9, line 1111) |
| Favorites Screen | `app/presentation/favorites/FavoritesScreen.kt` | **HIGH** | Recent implementation pattern |
| Favorites ViewModel | `app/presentation/favorites/FavoritesViewModel.kt` | **HIGH** | State management pattern |
| Recipe Model | `domain/model/Recipe.kt` | **HIGH** | Recipe data structure |
| Grocery Screen | `app/presentation/grocery/GroceryScreen.kt` | **HIGH** | Bottom nav pattern |
| RasoiNavHost | `app/presentation/navigation/RasoiNavHost.kt` | **HIGH** | Navigation setup |
| FakeRecipeRepository | `data/repository/FakeRecipeRepository.kt` | MEDIUM | Recipe data source |
| DataModule | `data/di/DataModule.kt` | MEDIUM | DI bindings |
| Theme | `app/presentation/theme/` | MEDIUM | Colors, spacing |
| CLAUDE.md | Root | MEDIUM | Project overview |

---

## CHAT WIREFRAME SUMMARY:

```
┌─────────────────────────────────────┐
│  ←  RasoiAI Assistant           ⋮   │  ← Top bar with menu
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
│  │ Type a message...        📎  🎤 ││  ← Input bar
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
- Morning (6-11 AM): "Quick breakfast", "Healthy start"
- Afternoon (11 AM-4 PM): "Lunch ideas", "Light meal"
- Evening (4-9 PM): "Dinner suggestions", "Family meal"
- Night (9 PM+): "Light snack", "Quick bite"

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
| 8 | Favorites | ✅ Done | Collections, 2-column grid, filters |
| 9 | **Chat** | ⏳ **Next** | AI assistant, chat history |
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
| Vegetarian | Green dot (●) | - |
| Non-Veg | Red dot (🔴) | - |

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

## FAVORITES SCREEN IMPLEMENTATION SUMMARY:

The Favorites screen was implemented with the following files:

### Domain Layer
- `domain/model/FavoriteCollection.kt` - Collection and FavoriteItem data classes
- `domain/repository/FavoritesRepository.kt` - Repository interface

### Data Layer
- `data/repository/FakeFavoritesRepository.kt` - Mock implementation with sample collections

### Presentation Layer
- `app/presentation/favorites/FavoritesScreen.kt` - Main screen composable
- `app/presentation/favorites/FavoritesViewModel.kt` - State management
- `app/presentation/favorites/components/CollectionCard.kt` - Collection thumbnail
- `app/presentation/favorites/components/RecipeGridItem.kt` - 2-column recipe card
- `app/presentation/favorites/components/FilterChips.kt` - Cuisine/Time filters
- `app/presentation/favorites/components/CreateCollectionDialog.kt` - New collection dialog

### Key Features Implemented
- Collections row with horizontal scroll
- Default collections: All, Recently Viewed
- Custom collections: Weekend Specials, Quick Meals, Kids Friendly
- Create new collection dialog
- Filter by cuisine (North/South/East/West)
- Filter by time (< 15/30/45/60 min)
- 2-column recipe grid with images
- Recipe cards with veg/non-veg indicator
- Reorder mode with drag handles
- Search functionality
- Add to collection menu
- Remove from favorites

---

## IMPLEMENTATION CHECKLIST FOR CHAT:

### 1. Create Chat Screen Files
```
app/presentation/chat/
├── ChatScreen.kt              # Main composable
├── ChatViewModel.kt           # State management
└── components/
    ├── ChatMessageItem.kt     # Message bubbles
    ├── QuickActionChips.kt    # Suggested actions
    ├── ChatInputBar.kt        # Text input bar
    └── RecipeSuggestionCard.kt # Recipe cards in chat
```

### 2. Create/Update Domain Models
- Create `ChatMessage` model in `domain/model/`
- Create `ChatRepository` interface in `domain/repository/`

### 3. Create Repository
```
data/repository/
└── FakeChatRepository.kt   # Mock data with chat history
```

### 4. Update Navigation
- `RasoiNavHost.kt`: Replace PlaceholderScreen with ChatScreen
- Chat is part of bottom navigation (3rd tab)

### 5. Update DI
- `DataModule.kt`: Bind FakeChatRepository

### 6. Key Features to Implement
- [ ] Message list with AI/User bubbles
- [ ] Quick action chips (time-based)
- [ ] Text input with send button
- [ ] Voice input button (placeholder)
- [ ] Attachment button (placeholder)
- [ ] Recipe suggestion cards
- [ ] Click to navigate to recipe detail
- [ ] Clear chat history option
- [ ] Bottom navigation integration

---

*Last Updated: January 26, 2026*
*Next Step: Chat Screen (AI assistant, chat history, recipe suggestions)*
