# Continuation Prompt for RasoiAI Project

Use this prompt to start a new conversation/context and continue the project from where we left off.

---

## PROMPT TO USE:

```
I am building **RasoiAI** - an AI-powered meal planning app for Indian families.

## Project Status

Core screens (Splash → Auth → Onboarding → Home → Recipe Detail → Cooking Mode → Grocery List) are COMPLETE. App builds and all tests pass. Ready for Favorites screen implementation.

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
| **Favorites** | ⏳ **Next Step** | Collections, 2-column grid, Recently Viewed |

## App Verified Working

- ✅ App builds successfully (`.\gradlew build`)
- ✅ All tests pass (`.\gradlew test`)
- ✅ Full navigation flow works: Splash → Auth → Onboarding → Home → Recipe Detail → Cooking Mode
- ✅ Grocery List with categorized items, WhatsApp share, checkbox toggle
- ✅ Bottom navigation working (Home, Grocery, Chat, Favs, Stats)

## Your Task

**Implement the Favorites Screen** (Screen 8 in wireframes):

### Files to Create:
```
app/presentation/favorites/
├── FavoritesScreen.kt         # Main composable with collections & grid
├── FavoritesViewModel.kt      # State management
└── components/
    ├── CollectionCard.kt      # Collection thumbnail card
    ├── RecipeGridItem.kt      # 2-column recipe card
    ├── FilterChips.kt         # Filter dropdowns (All, Cuisine, Time)
    └── CreateCollectionDialog.kt # New collection dialog

domain/model/
└── FavoriteCollection.kt      # Collection domain model (if not exists)

domain/repository/
└── FavoritesRepository.kt     # Repository interface (if not exists)

data/repository/
└── FakeFavoritesRepository.kt # Mock favorites data
```

### Update Files:
- `RasoiNavHost.kt` - Replace placeholder with FavoritesScreen
- `DataModule.kt` - Bind FakeFavoritesRepository

### UI Components (from wireframes - Screen 8, line 1026):
1. Top app bar ("Favorites", 🔍 search)
2. Collections row (horizontal scroll):
   - All (24) ✓
   - Recently Viewed (12)
   - Weekend Specials (8)
   - Quick Meals (10)
   - Kids Friendly (6)
   - [+New] create collection
3. Filter chips: [All ▼] [Cuisine ▼] [Time ▼]
4. Recipe count with [Reorder] button
5. 2-column recipe grid:
   - Image, recipe name, cuisine, time, calories
   - ● Green dot (veg) or 🔴 Red dot (non-veg)
   - ♥ favorite button, ⋮ more menu
6. Bottom navigation bar

### Reorder Mode:
- Drag handles (≡) appear on each card
- Drag to reorder recipes within collection
- [Done] button to exit reorder mode

### Key Patterns (follow existing code):
- ViewModel: `StateFlow<UiState>` + `StateFlow<NavigationEvent?>`
- Screen receives callbacks, ViewModel handles logic
- Use `MaterialTheme.colorScheme` and `spacing` from theme
- Use `hiltViewModel()` for DI
- Use LazyVerticalGrid for 2-column layout

## Reference Files

| File | Path | Why |
|------|------|-----|
| Wireframes | `docs/design/RasoiAI Screen Wireframes.md` | Screen 8 design (line 1026+) |
| Grocery Screen | `app/presentation/grocery/GroceryScreen.kt` | Recent implementation reference |
| Grocery ViewModel | `app/presentation/grocery/GroceryViewModel.kt` | State pattern reference |
| Home Screen | `app/presentation/home/HomeScreen.kt` | Bottom nav pattern |
| RasoiNavHost | `app/presentation/navigation/RasoiNavHost.kt` | Navigation setup |
| Recipe Model | `domain/model/Recipe.kt` | Recipe data structure |
| FakeRecipeRepository | `data/repository/FakeRecipeRepository.kt` | Recipe data for favorites |

## Domain Models to Use/Create

```kotlin
// FavoriteCollection.kt (create if not exists)
data class FavoriteCollection(
    val id: String,
    val name: String,
    val recipeIds: List<String>,
    val coverImageUrl: String?,
    val isDefault: Boolean = false,  // "All" and "Recently Viewed" are default
    val createdAt: Long
) {
    val recipeCount: Int get() = recipeIds.size
}

// Use existing Recipe model from domain/model/Recipe.kt
// FavoriteItem could wrap Recipe with additional favorite-specific data
data class FavoriteItem(
    val recipe: Recipe,
    val addedAt: Long,
    val collectionIds: List<String>
)
```

## Working Directory
Project root: `D:/Abhay/VibeCoding/KKB`

Start by reading Screen 8 wireframes in `docs/design/RasoiAI Screen Wireframes.md` (line 1026), then implement the Favorites screen.
```

---

## QUICK START PROMPT (Shorter):

```
I'm building **RasoiAI** - an AI meal planning app for Indian families.

**COMPLETED:** Splash, Auth, Onboarding, Home, Recipe Detail, Cooking Mode, Grocery List. All tests pass.

**NEXT:** Implement Favorites Screen (Screen 8)

**Read first:** `docs/design/RasoiAI Screen Wireframes.md` - Search for "Screen 8: Favorites" (line 1026)

**Create these files:**
1. `app/presentation/favorites/FavoritesScreen.kt` - Collections + 2-column grid
2. `app/presentation/favorites/FavoritesViewModel.kt` - State management
3. `app/presentation/favorites/components/` - CollectionCard, RecipeGridItem, FilterChips
4. `data/repository/FakeFavoritesRepository.kt` - Mock favorites data

**Update:**
- `RasoiNavHost.kt` - Wire up FavoritesScreen (replace placeholder)
- `DataModule.kt` - Provide FakeFavoritesRepository

**Favorites UI:**
- Collections row (All, Recently Viewed, Weekend Specials, Quick Meals, Kids Friendly, +New)
- Filter chips (All, Cuisine, Time dropdowns)
- 2-column recipe grid with images
- Recipe cards: name, cuisine, time, calories, veg/non-veg dot
- Reorder mode with drag handles

**Reference:** `app/presentation/grocery/` for recent ViewModel/Screen patterns
**Domain models:** `domain/model/Recipe.kt` (Recipe structure)

Project root: `D:/Abhay/VibeCoding/KKB`
```

---

## FILES TO REFERENCE:

| File | Path | Priority | Description |
|------|------|----------|-------------|
| Screen Wireframes | `docs/design/RasoiAI Screen Wireframes.md` | **HIGH** | Favorites design (Screen 8, line 1026) |
| Grocery Screen | `app/presentation/grocery/GroceryScreen.kt` | **HIGH** | Recent implementation pattern |
| Grocery ViewModel | `app/presentation/grocery/GroceryViewModel.kt` | **HIGH** | State management pattern |
| Recipe Model | `domain/model/Recipe.kt` | **HIGH** | Recipe data structure |
| Home Screen | `app/presentation/home/HomeScreen.kt` | **HIGH** | Bottom nav pattern |
| RasoiNavHost | `app/presentation/navigation/RasoiNavHost.kt` | **HIGH** | Navigation setup |
| FakeRecipeRepository | `data/repository/FakeRecipeRepository.kt` | MEDIUM | Recipe data source |
| DataModule | `data/di/DataModule.kt` | MEDIUM | DI bindings |
| Theme | `app/presentation/theme/` | MEDIUM | Colors, spacing |
| CLAUDE.md | Root | MEDIUM | Project overview |

---

## FAVORITES WIREFRAME SUMMARY:

```
┌─────────────────────────────────────┐
│  Favorites                     🔍   │  ← Top bar with search
│─────────────────────────────────────│
│                                     │
│  Collections:                       │
│  ┌─────────┐ ┌─────────┐ ┌────────┐ │
│  │ [Image] │ │ [Image] │ │[Image] │ │  ← Horizontal scroll
│  │   All   │ │Recently │ │Weekend │ │
│  │   (24)  │ │ Viewed  │ │Specials│ │
│  │    ✓    │ │  (12)   │ │  (8)   │ │
│  └─────────┘ └─────────┘ └────────┘ │
│                                     │
│  ┌─────────┐ ┌─────────┐ ┌────────┐ │
│  │ [Image] │ │ [Image] │ │        │ │
│  │  Quick  │ │  Kids   │ │ [+New] │ │  ← Create collection
│  │  Meals  │ │Friendly │ │        │ │
│  │  (10)   │ │  (6)    │ │        │ │
│  └─────────┘ └─────────┘ └────────┘ │
│                                     │
│─────────────────────────────────────│
│                                     │
│  Filter:                            │
│  [All ▼] [Cuisine ▼] [Time ▼]       │  ← Filter dropdowns
│                                     │
│  All (24)                  [Reorder]│  ← Recipe count + reorder
│─────────────────────────────────────│
│                                     │
│  ┌───────────────┐ ┌───────────────┐│
│  │    [Image]    │ │    [Image]    ││  ← 2-column grid
│  │               │ │               ││
│  │ ● Dal Tadka   │ │ ● Palak Paneer││
│  │   North       │ │   North       ││
│  │   35m • 180cal│ │   40m • 320cal││
│  │          ♥  ⋮ │ │          ♥  ⋮ ││
│  └───────────────┘ └───────────────┘│
│                                     │
│  ┌───────────────┐ ┌───────────────┐│
│  │    [Image]    │ │    [Image]    ││
│  │               │ │               ││
│  │ ● Masala Dosa │ │🔴Butter Chicken│
│  │   South       │ │   North       ││
│  │   30m • 280cal│ │   45m • 480cal││
│  │          ♥  ⋮ │ │          ♥  ⋮ ││
│  └───────────────┘ └───────────────┘│
│                                     │
│─────────────────────────────────────│
│  🏠     📋     💬     ❤️     📊    │
│ Home  Grocery  Chat  Favs  Stats    │
└─────────────────────────────────────┘
```

### Reorder Mode:
```
┌─────────────────────────────────────┐
│  Favorites                   [Done] │
│─────────────────────────────────────│
│                                     │
│  Drag to reorder recipes            │
│                                     │
│  ┌───────────────┐ ┌───────────────┐│
│  │ ≡  [Image]    │ │ ≡  [Image]    ││  ← Drag handles
│  │ ● Dal Tadka   │ │ ● Palak Paneer││
│  └───────────────┘ └───────────────┘│
│         ↕                   ↕       │
│  ┌───────────────┐ ┌───────────────┐│
│  │ ≡  [Image]    │ │ ≡  [Image]    ││
│  │ ● Masala Dosa │ │🔴Butter Chicken│
│  └───────────────┘ └───────────────┘│
│                                     │
└─────────────────────────────────────┘
```

### Create Collection Dialog:
```
┌─────────────────────────────────────┐
│  Create Collection                  │
│─────────────────────────────────────│
│                                     │
│  Collection name:                   │
│  ┌─────────────────────────────────┐│
│  │ Weekend Specials                ││
│  └─────────────────────────────────┘│
│                                     │
│  Cover image:                       │
│  ○ Use first recipe image           │
│  ○ Choose from gallery              │
│                                     │
│   [CANCEL]              [CREATE]    │
│                                     │
└─────────────────────────────────────┘
```

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
| 8 | **Favorites** | ⏳ **Next** | Collections, 2-column grid |
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

## IMPLEMENTATION CHECKLIST:

### 1. Create Favorites Screen Files
```
app/presentation/favorites/
├── FavoritesScreen.kt         # Main composable
├── FavoritesViewModel.kt      # State management
└── components/
    ├── CollectionCard.kt      # Collection thumbnail
    ├── RecipeGridItem.kt      # 2-column recipe card
    ├── FilterChips.kt         # Filter dropdowns
    └── CreateCollectionDialog.kt # New collection dialog
```

### 2. Create/Update Domain Models
- Create `FavoriteCollection` model in `domain/model/`
- Create `FavoritesRepository` interface in `domain/repository/`

### 3. Create Repository
```
data/repository/
└── FakeFavoritesRepository.kt   # Mock data with collections
```

### 4. Update Navigation
- `RasoiNavHost.kt`: Replace PlaceholderScreen with FavoritesScreen
- Favorites is part of bottom navigation (4th tab)

### 5. Update DI
- `DataModule.kt`: Bind FakeFavoritesRepository

### 6. Key Features to Implement
- [ ] Collections row with horizontal scroll
- [ ] Create new collection dialog
- [ ] Filter chips (All, Cuisine, Time)
- [ ] 2-column LazyVerticalGrid
- [ ] Recipe cards with image, details, veg/non-veg indicator
- [ ] Reorder mode with drag handles
- [ ] Recently Viewed auto-populated collection
- [ ] Bottom navigation integration

---

*Last Updated: January 2025*
*Next Step: Favorites Screen (Collections, 2-column grid, Recently Viewed)*
