# Screen 3: Home (Weekly Meal Plan)

## Summary Table

| ID | Element | Behavior | Status | Test Reference |
|----|---------|----------|--------|----------------|
| HOME-001 | Home Screen | Display weekly meal plan | Implemented | `HomeScreenTest.kt` |
| HOME-002 | App Bar | Show title, notifications, profile | Implemented | `HomeScreenTest.kt` |
| HOME-003 | Festival Banner | Show upcoming festival | Implemented | `HomeScreenTest.kt` |
| HOME-004 | Week Date Display | Show week range | Implemented | `HomeScreenTest.kt` |
| HOME-005 | Day Selector | Navigate between days | Implemented | `HomeScreenTest.kt` |
| HOME-006 | Day Lock Toggle | Lock/unlock entire day | Implemented | `HomeScreenTest.kt` |
| HOME-007 | Refresh Button | Regenerate meals | Implemented | `HomeScreenTest.kt` |
| HOME-008 | Meal Section - Breakfast | Display breakfast items | Implemented | `HomeScreenTest.kt` |
| HOME-009 | Meal Section - Lunch | Display lunch items | Implemented | `HomeScreenTest.kt` |
| HOME-010 | Meal Section - Dinner | Display dinner items | Implemented | `HomeScreenTest.kt` |
| HOME-011 | Meal Section - Snacks | Display snack items | Implemented | `HomeScreenTest.kt` |
| HOME-012 | Meal Lock Toggle | Lock/unlock meal section | Implemented | `HomeScreenTest.kt` |
| HOME-013 | Meal Add Button | Add recipe to meal | Implemented | `HomeScreenTest.kt` |
| HOME-014 | Meal Item Card | Display recipe in meal | Implemented | `HomeScreenTest.kt` |
| HOME-015 | Recipe Veg Indicator | Show veg/non-veg status | Implemented | `HomeScreenTest.kt` |
| HOME-016 | Recipe Time Display | Show prep time | Implemented | `HomeScreenTest.kt` |
| HOME-017 | Recipe Calorie Display | Show calories | Implemented | `HomeScreenTest.kt` |
| HOME-018 | Recipe Swap Button | Open swap sheet (unlocked) | Implemented | `HomeScreenTest.kt` |
| HOME-019 | Recipe Lock Button | Show locked state | Implemented | `HomeScreenTest.kt` |
| HOME-020 | Recipe Tap Action | Open recipe detail | Implemented | `HomeScreenTest.kt` |
| HOME-021 | Meal Total Summary | Show time/calories | Implemented | `HomeScreenTest.kt` |
| HOME-022 | Recipe Actions Sheet | Show action options | Implemented | `HomeScreenTest.kt` |
| HOME-023 | View Recipe Action | Navigate to detail | Implemented | `HomeScreenTest.kt` |
| HOME-024 | Swap Recipe Action | Show swap alternatives | Implemented | `HomeScreenTest.kt` |
| HOME-025 | Lock Recipe Action | Lock individual recipe | Implemented | `HomeScreenTest.kt` |
| HOME-026 | Remove Recipe Action | Remove from meal | Implemented | `HomeScreenTest.kt` |
| HOME-027 | Unlock Recipe Action | Unlock locked recipe | Implemented | `HomeScreenTest.kt` |
| HOME-028 | Swap Recipe Sheet | Display alternatives | Implemented | `SwapRecipeSheetTest.kt` |
| HOME-029 | Swap Search | Search for recipes | Implemented | `SwapRecipeSheetTest.kt` |
| HOME-030 | Swap Selection | Replace with chosen | Implemented | `SwapRecipeSheetTest.kt` |
| HOME-031 | Add Recipe Sheet | Display recipe options | Implemented | `AddRecipeSheetTest.kt` |
| HOME-032 | Add Search | Search recipes to add | Implemented | `AddRecipeSheetTest.kt` |
| HOME-033 | Add Suggestions | Show contextual recipes | Implemented | `AddRecipeSheetTest.kt` |
| HOME-034 | Add Selection | Add chosen recipe | Implemented | `AddRecipeSheetTest.kt` |
| HOME-035 | Refresh Options Sheet | Regeneration choices | Implemented | `HomeScreenTest.kt` |
| HOME-036 | Refresh This Day | Regenerate single day | Implemented | `HomeScreenTest.kt` |
| HOME-037 | Refresh Entire Week | Regenerate full week | Implemented | `HomeScreenTest.kt` |
| HOME-038 | Lock Inheritance | Soft lock cascade | Implemented | `HomeViewModelTest.kt` |
| HOME-039 | Swipe Actions | Reveal lock/delete | Implemented | `HomeScreenTest.kt` |
| HOME-040 | Long Press Meal | Quick lock toggle | Implemented | `HomeScreenTest.kt` |
| HOME-041 | Bottom Navigation | 5 nav items | Implemented | `HomeScreenTest.kt` |
| HOME-042 | Auto-Favorite | Add to favorites from suggestions | Implemented | `HomeViewModelTest.kt` |

---

## Detailed Requirements

### HOME-001: Home Screen Display

| Field | Value |
|-------|-------|
| **Screen** | Home |
| **Element** | Full screen |
| **Trigger** | Navigation to Home |
| **Status** | Implemented |
| **Test** | `HomeScreenTest.kt:homeScreen_displaysCorrectly` |

**Acceptance Criteria:**
- Given: User is authenticated with meal plan
- When: Home screen displays
- Then: App bar with title "RasoiAI" appears
- And: Week date range displayed
- And: Day selector shows current week
- And: Today's meals are shown by default
- And: Bottom navigation is visible

---

### HOME-003: Festival Banner

| Field | Value |
|-------|-------|
| **Screen** | Home |
| **Element** | Festival notification card |
| **Trigger** | Festival within 7 days |
| **Status** | Implemented |
| **Test** | `HomeScreenTest.kt:festivalBanner_showsWhenUpcoming` |

**Acceptance Criteria:**
- Given: Festival is within 7 days
- When: Home screen displays
- Then: Banner shows festival name and countdown
- And: "View festive recipes" link is tappable
- And: Tap navigates to festival recipes

**Example:**
```
🎉 Makar Sankranti in 3 days!
   View festive recipes →
```

---

### HOME-005: Day Selector

| Field | Value |
|-------|-------|
| **Screen** | Home |
| **Element** | Horizontal day tabs |
| **Trigger** | User tap on day |
| **Status** | Implemented |
| **Test** | `HomeScreenTest.kt:daySelector_changesDisplayedDay` |

**Acceptance Criteria:**
- Given: Week view is displayed
- When: User taps a day tab
- Then: Selected day is highlighted
- And: Meals for that day display below
- And: Today is marked distinctly

---

### HOME-006: Day Lock Toggle

| Field | Value |
|-------|-------|
| **Screen** | Home |
| **Element** | Lock button in day header |
| **Trigger** | User tap |
| **Status** | Implemented |
| **Test** | `HomeScreenTest.kt:dayLockToggle_changesState` |

**Acceptance Criteria:**
- Given: User viewing a day
- When: User taps lock button in day header
- Then: Day lock state toggles
- And: 🔓 → 🔒 (locking) or 🔒 → 🔓 (unlocking)
- And: All meals inherit lock state (soft inheritance)

**Lock Inheritance:**
- Locking day → all meals show locked
- Unlocking day → explicitly locked items remain locked

---

### HOME-007: Refresh Button

| Field | Value |
|-------|-------|
| **Screen** | Home |
| **Element** | Refresh icon in day header |
| **Trigger** | User tap |
| **Status** | Implemented |
| **Test** | `HomeScreenTest.kt:refreshButton_opensOptionsSheet` |

**Acceptance Criteria:**
- Given: User viewing a day
- When: User taps refresh button (🔄)
- Then: Refresh Options Bottom Sheet opens
- And: Shows "This Day Only" and "Entire Week" options

---

### HOME-012: Meal Section Lock Toggle

| Field | Value |
|-------|-------|
| **Screen** | Home |
| **Element** | Lock button in meal header |
| **Trigger** | User tap |
| **Status** | Implemented |
| **Test** | `HomeScreenTest.kt:mealLockToggle_changesState` |

**Acceptance Criteria:**
- Given: User viewing a meal section
- When: User taps lock button in meal header
- Then: Meal lock state toggles
- And: All recipes in meal inherit lock state

---

### HOME-013: Meal Add Button

| Field | Value |
|-------|-------|
| **Screen** | Home |
| **Element** | "+ Add" button in meal header |
| **Trigger** | User tap |
| **Status** | Implemented |
| **Test** | `HomeScreenTest.kt:addButton_opensAddRecipeSheet` |

**Acceptance Criteria:**
- Given: User viewing a meal section
- When: User taps "+ Add"
- Then: Add Recipe Bottom Sheet opens
- And: Shows search and suggestions
- And: Suggestions match meal type (Breakfast/Lunch/etc.)

---

### HOME-014: Meal Item Card

| Field | Value |
|-------|-------|
| **Screen** | Home |
| **Element** | Recipe card in meal |
| **Trigger** | Meal section display |
| **Status** | Implemented |
| **Test** | `HomeScreenTest.kt:mealItemCard_displaysCorrectly` |

**Card Elements:**
| Element | Position | Content |
|---------|----------|---------|
| Veg indicator | Left | ● (green) or 🔴 (red) |
| Recipe name | Center-left | "Dal Tadka" |
| Prep time | Center-right | "25 min" |
| Calories | Right-center | "180cal" |
| Action button | Right | [⟲] or [🔒] |

**Acceptance Criteria:**
- Given: Meal has recipes
- When: Meal section displays
- Then: Each recipe shows as card with all elements
- And: Cards are vertically stacked

---

### HOME-018: Recipe Swap Button (Unlocked State)

| Field | Value |
|-------|-------|
| **Screen** | Home |
| **Element** | [⟲] button on recipe card |
| **Trigger** | Recipe is unlocked |
| **Status** | Implemented |
| **Test** | `HomeScreenTest.kt:swapButton_visibleWhenUnlocked` |

**Acceptance Criteria:**
- Given: Recipe is NOT locked
- When: Recipe card displays
- Then: Swap button [⟲] is visible on right
- And: Tap opens Recipe Actions Bottom Sheet

---

### HOME-019: Recipe Lock Button (Locked State)

| Field | Value |
|-------|-------|
| **Screen** | Home |
| **Element** | [🔒] button on recipe card |
| **Trigger** | Recipe is locked |
| **Status** | Implemented |
| **Test** | `HomeScreenTest.kt:lockButton_visibleWhenLocked` |

**Acceptance Criteria:**
- Given: Recipe IS locked (directly or inherited)
- When: Recipe card displays
- Then: Lock button [🔒] replaces swap button
- And: Tap opens Recipe Actions (with Unlock option)

---

### HOME-022: Recipe Actions Bottom Sheet

| Field | Value |
|-------|-------|
| **Screen** | Home |
| **Element** | Actions modal |
| **Trigger** | Tap on recipe action button |
| **Status** | Implemented |
| **Test** | `HomeScreenTest.kt:recipeActionsSheet_displaysOptions` |

**Unlocked Recipe Options:**
| Option | Icon | Action |
|--------|------|--------|
| View Recipe | 👁️ | Navigate to detail |
| Swap Recipe | 🔄 | Open swap sheet |
| Lock Recipe | 🔒 | Lock from changes |
| Remove from Meal | ✕ | Delete from meal |

**Locked Recipe Options:**
| Option | Icon | Action |
|--------|------|--------|
| View Recipe | 👁️ | Navigate to detail |
| Unlock Recipe | 🔓 | Allow changes |
| Swap Recipe | 🔄 | Disabled (grayed) |
| Remove from Meal | ✕ | Disabled (grayed) |

---

### HOME-028: Swap Recipe Bottom Sheet

| Field | Value |
|-------|-------|
| **Screen** | Home |
| **Element** | Swap alternatives modal |
| **Trigger** | "Swap Recipe" action |
| **Status** | Implemented |
| **Test** | `SwapRecipeSheetTest.kt:swapSheet_displaysAlternatives` |

**Acceptance Criteria:**
- Given: User tapped Swap Recipe
- When: Swap sheet opens
- Then: Header shows "Swap [Recipe Name]"
- And: Search field at top
- And: Similar recipes in 2-column grid
- And: Recipes match dietary restrictions

---

### HOME-031: Add Recipe Bottom Sheet

| Field | Value |
|-------|-------|
| **Screen** | Home |
| **Element** | Add recipe modal |
| **Trigger** | "+ Add" in meal header |
| **Status** | Implemented |
| **Test** | `AddRecipeSheetTest.kt:addSheet_displaysSuggestions` |

**Acceptance Criteria:**
- Given: User tapped "+ Add" on meal
- When: Add sheet opens
- Then: Header shows "Add to [Meal Type]"
- And: Search field at top
- And: "Suggestions for [Meal]" section
- And: "Recently Added" section
- And: 2-column recipe grid

---

### HOME-035: Refresh Options Bottom Sheet

| Field | Value |
|-------|-------|
| **Screen** | Home |
| **Element** | Regeneration options modal |
| **Trigger** | Refresh button tap |
| **Status** | Implemented |
| **Test** | `HomeScreenTest.kt:refreshSheet_displaysOptions` |

**Options:**
| Option | Description |
|--------|-------------|
| 📅 This Day Only | Regenerate selected day |
| 📆 Entire Week | Regenerate full week |

**Warning Message:**
```
⚠️ Locked items (🔒) will not be changed during regeneration.
This includes locked days, meals, and individual recipes.
```

---

### HOME-038: Lock Inheritance (Soft)

| Field | Value |
|-------|-------|
| **Screen** | Home |
| **Element** | Lock state logic |
| **Trigger** | Lock toggle at any level |
| **Status** | Implemented |
| **Test** | `HomeViewModelTest.kt:lockInheritance_softCascade` |

**Inheritance Rules:**

| Action | Effect |
|--------|--------|
| Lock Day | All meals & recipes show locked |
| Unlock Day | Only day unlocks; explicitly locked items stay locked |
| Lock Meal | All recipes in meal show locked |
| Unlock Meal | Only meal unlocks; explicitly locked recipes stay locked |
| Lock Recipe | Only that recipe locked |
| Unlock Recipe | Recipe unlocked (overrides parent) |

**Acceptance Criteria:**
- Given: User locks a day
- When: Viewing recipes in that day
- Then: All recipes show [🔒] button
- And: User can individually unlock recipes
- And: Individually unlocked recipes show [⟲]

---

### HOME-039: Swipe Actions on Recipe

| Field | Value |
|-------|-------|
| **Screen** | Home |
| **Element** | Swipe reveal actions |
| **Trigger** | Swipe left on recipe card |
| **Status** | Implemented |
| **Test** | `HomeScreenTest.kt:swipeLeft_revealsActions` |

**Unlocked Recipe Swipe:**
| Action | Icon | Description |
|--------|------|-------------|
| Lock | 🔒 | Lock recipe |
| Delete | ✕ | Remove from meal |

**Locked Recipe Swipe:**
| Action | Icon | Description |
|--------|------|-------------|
| Unlock | 🔓 | Unlock recipe |

---

### HOME-040: Long Press Meal Header

| Field | Value |
|-------|-------|
| **Screen** | Home |
| **Element** | Meal header gesture |
| **Trigger** | Long press on meal header |
| **Status** | Implemented |
| **Test** | `HomeScreenTest.kt:longPressMeal_togglesLock` |

**Acceptance Criteria:**
- Given: User views meal section
- When: User long-presses meal header
- Then: Meal lock state toggles
- And: Haptic feedback provided
- And: Same effect as tapping lock button

---

### HOME-041: Bottom Navigation

| Field | Value |
|-------|-------|
| **Screen** | Home |
| **Element** | Bottom nav bar |
| **Trigger** | Screen display |
| **Status** | Implemented |
| **Test** | `HomeScreenTest.kt:bottomNav_displaysAllItems` |

**Navigation Items:**
| Item | Icon | Screen |
|------|------|--------|
| Home | 🏠 | Home (selected) |
| Grocery | 📋 | Grocery List |
| Chat | 💬 | AI Chat |
| Favs | ❤️ | Favorites |
| Stats | 📊 | Statistics |

**Acceptance Criteria:**
- Given: Home screen displays
- When: User views bottom
- Then: 5 navigation items visible
- And: Home is highlighted (active)
- And: Tap navigates to respective screen

---

### HOME-042: Auto-Add to Favorites from Suggestions

| Field | Value |
|-------|-------|
| **Screen** | Home (Swap/Add Sheet) |
| **Element** | Recipe selection in Suggestions tab |
| **Trigger** | User selects from Suggestions |
| **Status** | Implemented |
| **Test** | `HomeViewModelTest.kt:selectFromSuggestions_autoAddsFavorite` |

**Notes:** `[Added Post-MVP]`

**Acceptance Criteria:**
- Given: User is in swap/add recipe sheet
- When: User selects a recipe from "Suggestions" tab
- Then: Recipe is added/swapped in meal plan
- And: Recipe is automatically added to Favorites
- And: Snackbar shows "Added to Favorites"
- And: Does NOT auto-favorite from "All Recipes" or "Search"

---

## Visual States Summary

### Recipe Card States

| State | Button | Swipe Actions |
|-------|--------|---------------|
| Unlocked | [⟲] | Lock, Delete |
| Locked (direct) | [🔒] | Unlock |
| Locked (inherited) | [🔒] | Unlock |
| Unlocked (override) | [⟲] | Lock, Delete |

### Day Header States

| State | Icon | Meaning |
|-------|------|---------|
| Unlocked | [🔓] | Can be modified |
| Locked | [🔒] | Protected from changes |

### Meal Header States

| State | Icon | Meaning |
|-------|------|---------|
| Unlocked | [🔓] | Can add/swap recipes |
| Locked | [🔒] | Recipes protected |

---

## Implementation Files

| Component | File Path |
|-----------|-----------|
| Home Screen | `presentation/home/HomeScreen.kt` |
| Home ViewModel | `presentation/home/HomeViewModel.kt` |
| Meal Card | `presentation/home/components/MealCard.kt` |
| Meal Item | `presentation/home/components/MealItem.kt` |
| Day Selector | `presentation/home/components/DaySelector.kt` |
| Swap Recipe Sheet | `presentation/home/components/SwapRecipeSheet.kt` |
| Add Recipe Sheet | `presentation/home/components/AddRecipeSheet.kt` |
| Recipe Actions Sheet | `presentation/home/components/RecipeActionsSheet.kt` |
| Bottom Navigation | `presentation/home/components/RasoiBottomNavigation.kt` |

## Test Files

| Test Type | File Path |
|-----------|-----------|
| UI Tests | `app/src/androidTest/java/com/rasoiai/app/presentation/home/HomeScreenTest.kt` |
| Unit Tests | `app/src/test/java/com/rasoiai/app/presentation/home/HomeViewModelTest.kt` |
| Swap Sheet Tests | `app/src/androidTest/java/com/rasoiai/app/presentation/home/SwapRecipeSheetTest.kt` |
| E2E Flow | `app/src/androidTest/java/com/rasoiai/app/e2e/flows/HomeE2EFlowTest.kt` |

---

*Requirements derived from wireframe: `04-home.md`*
