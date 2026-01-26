# Continuation Prompt for RasoiAI Project

Use this prompt to start a new conversation/context and continue the project from where we left off.

---

## PROMPT TO USE:

```
I am building **RasoiAI** - an AI-powered meal planning app for Indian families.

## Project Status

Core screens (Splash → Auth → Onboarding → Home → Recipe Detail → Cooking Mode) are COMPLETE. App builds and all tests pass. Ready for Grocery List screen implementation.

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
| **Grocery List** | ⏳ **Next Step** | Categorized items, WhatsApp share |

## App Verified Working

- ✅ App builds successfully (`.\gradlew build`)
- ✅ All tests pass (`.\gradlew test`)
- ✅ Full navigation flow works: Splash → Auth → Onboarding → Home → Recipe Detail → Cooking Mode
- ✅ Timer functionality with vibration works in Cooking Mode
- ✅ Rating dialog after cooking completion

## Your Task

**Implement the Grocery List Screen** (Screen 7 in wireframes):

### Files to Create:
```
app/presentation/grocery/
├── GroceryScreen.kt           # Main composable with categorized list
├── GroceryViewModel.kt        # State management
└── components/
    ├── GroceryCategory.kt     # Expandable category section
    ├── GroceryItem.kt         # Checkbox item with quantity
    └── WhatsAppShareDialog.kt # Share preview dialog

domain/model/
└── GroceryItem.kt             # Domain model (if not exists)

domain/repository/
└── GroceryRepository.kt       # Repository interface (if not exists)

data/repository/
└── FakeGroceryRepository.kt   # Mock grocery data
```

### Update Files:
- `RasoiNavHost.kt` - Replace placeholder with GroceryScreen
- `DataModule.kt` - Bind FakeGroceryRepository

### UI Components (from wireframes - Screen 7, line 934):
1. Top app bar (← back, "Grocery List", ⋮ more)
2. Week header ("Week of Jan 20-26", "32 items")
3. WhatsApp share button (📱 Share via WhatsApp)
4. Categorized sections (expandable):
   - 🥬 VEGETABLES (10)
   - 🥛 DAIRY (5)
   - 🌾 PULSES & GRAINS (6)
   - 🌶️ SPICES & MASALA (8)
   - 🥫 OTHER (3)
5. Each item: □ checkbox, name, quantity (e.g., "1 kg")
6. Swipe actions: ✏️ Edit, 🗑️ Delete
7. "+ Add custom item" button at bottom
8. Bottom navigation bar

### WhatsApp Share Feature:
- Preview dialog before sharing
- Options: "Full list" or "Unpurchased only"
- Formatted text with categories and items
- Footer: "_Sent from RasoiAI_ 🍳"

### Key Patterns (follow existing code):
- ViewModel: `StateFlow<UiState>` + `StateFlow<NavigationEvent?>`
- Screen receives callbacks, ViewModel handles logic
- Use `MaterialTheme.colorScheme` and `spacing` from theme
- Use `hiltViewModel()` for DI

## Reference Files

| File | Path | Why |
|------|------|-----|
| Wireframes | `docs/design/RasoiAI Screen Wireframes.md` | Screen 7 design (line 934+) |
| Home Screen | `app/presentation/home/HomeScreen.kt` | Pattern reference |
| Home ViewModel | `app/presentation/home/HomeViewModel.kt` | State management pattern |
| Cooking Mode | `app/presentation/cookingmode/` | Recent implementation reference |
| RasoiNavHost | `app/presentation/navigation/RasoiNavHost.kt` | Navigation setup |
| FakeRecipeRepository | `data/repository/FakeRecipeRepository.kt` | Fake data pattern |
| Ingredient Model | `domain/model/Recipe.kt` | IngredientCategory enum |

## Domain Models to Use/Create

```kotlin
// GroceryItem.kt (create if not exists)
data class GroceryItem(
    val id: String,
    val name: String,
    val quantity: String,
    val unit: String,
    val category: IngredientCategory,  // From Recipe.kt
    val isPurchased: Boolean = false,
    val recipeIds: List<String> = emptyList()  // Which recipes need this
)

// Use existing IngredientCategory from Recipe.kt:
enum class IngredientCategory {
    VEGETABLES, FRUITS, DAIRY, GRAINS, PULSES,
    SPICES, OILS, MEAT, SEAFOOD, NUTS, SWEETENERS, OTHER
}
```

## Working Directory
Project root: `D:/Abhay/VibeCoding/KKB`

Start by reading Screen 7 wireframes in `docs/design/RasoiAI Screen Wireframes.md` (line 934), then implement the Grocery List screen.
```

---

## QUICK START PROMPT (Shorter):

```
I'm building **RasoiAI** - an AI meal planning app for Indian families.

**COMPLETED:** Splash, Auth, Onboarding, Home, Recipe Detail, Cooking Mode screens. All tests pass.

**NEXT:** Implement Grocery List Screen (Screen 7)

**Read first:** `docs/design/RasoiAI Screen Wireframes.md` - Search for "Screen 7: Grocery List" (line 934)

**Create these files:**
1. `app/presentation/grocery/GroceryScreen.kt` - Categorized list UI
2. `app/presentation/grocery/GroceryViewModel.kt` - State management
3. `data/repository/FakeGroceryRepository.kt` - Mock grocery data

**Update:**
- `RasoiNavHost.kt` - Wire up GroceryScreen (replace placeholder)
- `DataModule.kt` - Provide FakeGroceryRepository

**Grocery List UI:**
- Week header with item count
- WhatsApp share button
- Expandable categories (Vegetables, Dairy, Pulses, Spices, etc.)
- Checkbox items with quantity
- Swipe to edit/delete
- Add custom item button
- Bottom navigation

**Reference:** `app/presentation/cookingmode/` for recent ViewModel/Screen patterns
**Domain models:** `domain/model/Recipe.kt` (IngredientCategory)

Project root: `D:/Abhay/VibeCoding/KKB`
```

---

## FILES TO REFERENCE:

| File | Path | Priority | Description |
|------|------|----------|-------------|
| Screen Wireframes | `docs/design/RasoiAI Screen Wireframes.md` | **HIGH** | Grocery List design (Screen 7, line 934) |
| Ingredient Model | `domain/model/Recipe.kt` | **HIGH** | IngredientCategory enum |
| Home Screen | `app/presentation/home/HomeScreen.kt` | **HIGH** | Bottom nav pattern |
| Cooking Mode | `app/presentation/cookingmode/` | **HIGH** | Recent implementation |
| RasoiNavHost | `app/presentation/navigation/RasoiNavHost.kt` | **HIGH** | Navigation setup |
| FakeRecipeRepository | `data/repository/FakeRecipeRepository.kt` | MEDIUM | Fake data pattern |
| DataModule | `data/di/DataModule.kt` | MEDIUM | DI bindings |
| Theme | `app/presentation/theme/` | MEDIUM | Colors, spacing |
| CLAUDE.md | Root | MEDIUM | Project overview |

---

## GROCERY LIST WIREFRAME SUMMARY:

```
┌─────────────────────────────────────┐
│  ←  Grocery List                ⋮   │  ← Top bar
│─────────────────────────────────────│
│                                     │
│  Week of Jan 20-26                  │
│  32 items                           │
│                                     │
│  ┌─────────────────────────────────┐│
│  │  📱 Share via WhatsApp          ││  ← WhatsApp share
│  └─────────────────────────────────┘│
│                                     │
│─────────────────────────────────────│
│                                     │
│  🥬 VEGETABLES (10)             ▼   │  ← Expandable category
│  ┌─────────────────────────────────┐│
│  │ □ Onion                   1 kg  ││
│  │ □ Tomato                  500g  ││
│  │ □ Potato                  1 kg  ││
│  │ □ Palak (Spinach)      2 bunch  ││
│  │ □ Capsicum                250g  ││
│  │ □ Ginger                  100g  ││
│  │ □ Garlic                  100g  ││
│  │ □ Green Chili            10 pcs ││
│  │ □ Coriander             1 bunch ││
│  │ □ Lemon                   4 pcs ││
│  └─────────────────────────────────┘│
│                                     │
│  🥛 DAIRY (5)                   ▼   │
│  ┌─────────────────────────────────┐│
│  │ □ Paneer                  400g  ││
│  │ □ Curd                    500g  ││
│  │ □ Milk                      1 L ││
│  │ □ Ghee                    200g  ││
│  │ □ Butter                  100g  ││
│  └─────────────────────────────────┘│
│                                     │
│  🌾 PULSES & GRAINS (6)         ▼   │
│  🌶️ SPICES & MASALA (8)         ▼   │
│  🥫 OTHER (3)                   ▼   │
│                                     │
│  ┌─────────────────────────────────┐│
│  │ + Add custom item               ││
│  └─────────────────────────────────┘│
│                                     │
│─────────────────────────────────────│
│  🏠     📋     💬     ❤️     📊    │
│ Home  Grocery  Chat  Favs  Stats    │
└─────────────────────────────────────┘
```

### WhatsApp Share Preview:
```
┌─────────────────────────────────────┐
│  Share to WhatsApp                  │
│─────────────────────────────────────│
│                                     │
│   Preview:                          │
│   ┌─────────────────────────────┐   │
│   │ 🛒 *Grocery List*           │   │
│   │ Week: Jan 20-26             │   │
│   │                             │   │
│   │ *🥬 Vegetables*             │   │
│   │ • Onion - 1 kg              │   │
│   │ • Tomato - 500g             │   │
│   │ ...                         │   │
│   │                             │   │
│   │ _Sent from RasoiAI_ 🍳      │   │
│   └─────────────────────────────┘   │
│                                     │
│   Share:                            │
│   ○ Full list (32 items)            │
│   ○ Unpurchased only (26 items)     │
│                                     │
│   [CANCEL]        [SHARE WHATSAPP]  │
│                                     │
└─────────────────────────────────────┘
```

### Item Swipe Actions:
```
┌───────────────────────────┬────┬────┐
│ □ Onion            1 kg   │ ✏️ │ 🗑️ │
└───────────────────────────┴────┴────┘
✏️ = Edit quantity, 🗑️ = Remove
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
| 7 | **Grocery List** | ⏳ **Next** | Categorized, WhatsApp share |
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

## IMPLEMENTATION CHECKLIST:

### 1. Create Grocery Screen Files
```
app/presentation/grocery/
├── GroceryScreen.kt           # Main composable
├── GroceryViewModel.kt        # State management
└── components/
    ├── GroceryCategory.kt     # Expandable section
    ├── GroceryItem.kt         # Checkbox item
    └── WhatsAppShareDialog.kt # Share dialog
```

### 2. Create/Update Domain Models
- Check if `GroceryItem` model exists in `domain/model/`
- Create `GroceryRepository` interface in `domain/repository/`

### 3. Create Repository
```
data/repository/
└── FakeGroceryRepository.kt   # Mock data with categories
```

### 4. Update Navigation
- `RasoiNavHost.kt`: Replace PlaceholderScreen with GroceryScreen
- Grocery is part of bottom navigation

### 5. Update DI
- `DataModule.kt`: Bind FakeGroceryRepository

### 6. Key Features to Implement
- [ ] Categorized expandable sections
- [ ] Checkbox to mark purchased
- [ ] Swipe to edit/delete
- [ ] Add custom item
- [ ] WhatsApp share with preview
- [ ] Bottom navigation integration

---

*Last Updated: January 2025*
*Next Step: Grocery List Screen (Categorized items, WhatsApp share)*
