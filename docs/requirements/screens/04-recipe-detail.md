# Screen 4: Recipe Detail & Cooking Mode

## Summary Table

| ID | Element | Behavior | Status | Test Reference |
|----|---------|----------|--------|----------------|
| REC-001 | Recipe Detail Screen | Display full recipe | Implemented | `RecipeDetailScreenTest.kt` |
| REC-002 | Back Navigation | Return to previous | Implemented | `RecipeDetailScreenTest.kt` |
| REC-003 | Favorite Button | Toggle favorite state | Implemented | `RecipeDetailScreenTest.kt` |
| REC-004 | More Options Menu | Show additional actions | Implemented | `RecipeDetailScreenTest.kt` |
| REC-005 | Recipe Image | Display hero image | Implemented | `RecipeDetailScreenTest.kt` |
| REC-006 | Recipe Name | Show title with lock indicator | Implemented | `RecipeDetailScreenTest.kt` |
| REC-007 | Cuisine Info | Show region and type | Implemented | `RecipeDetailScreenTest.kt` |
| REC-008 | Quick Stats | Time, servings, calories | Implemented | `RecipeDetailScreenTest.kt` |
| REC-009 | Dietary Tags | Show veg/restrictions | Implemented | `RecipeDetailScreenTest.kt` |
| REC-010 | Nutrition Info | Per-serving macros | Implemented | `RecipeDetailScreenTest.kt` |
| REC-011 | Tab Bar | Ingredients/Instructions | Implemented | `RecipeDetailScreenTest.kt` |
| REC-012 | Ingredients Tab | List all ingredients | Implemented | `RecipeDetailScreenTest.kt` |
| REC-013 | Serving Adjuster | Scale ingredient quantities | Implemented | `RecipeDetailScreenTest.kt` |
| REC-014 | Ingredient Checkbox | Check off items | Implemented | `RecipeDetailScreenTest.kt` |
| REC-015 | Add to Grocery Button | Add all ingredients | Implemented | `RecipeDetailScreenTest.kt` |
| REC-016 | Instructions Tab | List all steps | Implemented | `RecipeDetailScreenTest.kt` |
| REC-017 | Step Card | Display step content | Implemented | `RecipeDetailScreenTest.kt` |
| REC-018 | Start Cooking Button | Enter cooking mode | Implemented | `RecipeDetailScreenTest.kt` |
| REC-019 | Modify with AI Button | Open chat with context | Implemented | `RecipeDetailScreenTest.kt` |
| REC-020 | Lock Indicator | Show meal plan lock state | Implemented | `RecipeDetailScreenTest.kt` |
| COOK-001 | Cooking Mode Screen | Step-by-step display | Implemented | `CookingModeScreenTest.kt` |
| COOK-002 | Close Button | Exit cooking mode | Implemented | `CookingModeScreenTest.kt` |
| COOK-003 | Progress Indicator | Show step progress | Implemented | `CookingModeScreenTest.kt` |
| COOK-004 | Step Content | Large readable text | Implemented | `CookingModeScreenTest.kt` |
| COOK-005 | Step Image | Show relevant visual | Implemented | `CookingModeScreenTest.kt` |
| COOK-006 | Timer Button | Set cooking timer | Implemented | `CookingModeScreenTest.kt` |
| COOK-007 | Timer Running | Display countdown | Implemented | `CookingModeScreenTest.kt` |
| COOK-008 | Timer Complete | Alert notification | Implemented | `CookingModeScreenTest.kt` |
| COOK-009 | Previous Button | Go to previous step | Implemented | `CookingModeScreenTest.kt` |
| COOK-010 | Next Button | Go to next step | Implemented | `CookingModeScreenTest.kt` |
| COOK-011 | Screen Wake Lock | Keep screen on | Implemented | `CookingModeScreenTest.kt` |
| COOK-012 | Completion Screen | Show finish options | Implemented | `CookingModeScreenTest.kt` |
| COOK-013 | Rating Stars | Rate the dish | Implemented | `CookingModeScreenTest.kt` |
| COOK-014 | Feedback Input | Optional text feedback | Implemented | `CookingModeScreenTest.kt` |
| COOK-015 | Done Button | Complete and exit | Implemented | `CookingModeScreenTest.kt` |
| COOK-016 | Skip Rating | Exit without rating | Implemented | `CookingModeScreenTest.kt` |

---

## Detailed Requirements

### REC-001: Recipe Detail Screen

| Field | Value |
|-------|-------|
| **Screen** | Recipe Detail |
| **Element** | Full screen |
| **Trigger** | Navigate from Home/Favorites |
| **Status** | Implemented |
| **Test** | `RecipeDetailScreenTest.kt:recipeDetail_displaysCorrectly` |

**Acceptance Criteria:**
- Given: User taps on a recipe
- When: Detail screen opens
- Then: Hero image at top
- And: Recipe info section below
- And: Tab bar for Ingredients/Instructions
- And: Action buttons at bottom

---

### REC-003: Favorite Button

| Field | Value |
|-------|-------|
| **Screen** | Recipe Detail |
| **Element** | Heart icon in app bar |
| **Trigger** | User tap |
| **Status** | Implemented |
| **Test** | `RecipeDetailScreenTest.kt:favoriteButton_togglesState` |

**Acceptance Criteria:**
- Given: Recipe detail is displayed
- When: User taps favorite button
- Then: Recipe added to/removed from favorites
- And: Heart icon fills/unfills accordingly
- And: ♡ (outline) = not favorited
- And: ♥ (filled) = favorited

---

### REC-006: Recipe Name with Lock Indicator

| Field | Value |
|-------|-------|
| **Screen** | Recipe Detail |
| **Element** | Title with optional lock |
| **Trigger** | Opened from meal plan |
| **Status** | Implemented |
| **Test** | `RecipeDetailScreenTest.kt:recipeName_showsLockWhenApplicable` |

**Lock Indicator Rules:**
| Scenario | Icon | Meaning |
|----------|------|---------|
| From locked meal plan item | 🔒 | Protected from regeneration |
| From unlocked meal plan item | 🔓 | Can be swapped/regenerated |
| From Favorites | (none) | Not in meal plan context |
| From Search | (none) | Not in meal plan context |
| From Chat suggestions | (none) | Not in meal plan context |

**Acceptance Criteria:**
- Given: Recipe opened from meal plan
- When: Lock indicator should appear
- Then: Icon shows next to recipe name
- And: Icon is informational only (no tap action)

---

### REC-008: Quick Stats Display

| Field | Value |
|-------|-------|
| **Screen** | Recipe Detail |
| **Element** | Stats row |
| **Trigger** | Screen display |
| **Status** | Implemented |
| **Test** | `RecipeDetailScreenTest.kt:quickStats_displaysCorrectly` |

**Stats Display:**
| Stat | Icon | Example |
|------|------|---------|
| Time | ⏱️ | 35 min |
| Servings | 👥 | 4 serv |
| Calories | 🔥 | 180cal |

**Acceptance Criteria:**
- Given: Recipe detail displayed
- When: Stats section renders
- Then: Three stat boxes in horizontal row
- And: Each shows icon, value, and label

---

### REC-009: Dietary Tags

| Field | Value |
|-------|-------|
| **Screen** | Recipe Detail |
| **Element** | Tag chips |
| **Trigger** | Screen display |
| **Status** | Implemented |
| **Test** | `RecipeDetailScreenTest.kt:dietaryTags_displayCorrectly` |

**Example Tags:**
- ● Vegetarian
- Gluten-Free
- High Protein
- Easy

**Acceptance Criteria:**
- Given: Recipe has dietary tags
- When: Tags section renders
- Then: Each tag as chip with indicator
- And: Green dot for veg, red for non-veg

---

### REC-010: Nutrition Information

| Field | Value |
|-------|-------|
| **Screen** | Recipe Detail |
| **Element** | Nutrition row |
| **Trigger** | Screen display |
| **Status** | Implemented |
| **Test** | `RecipeDetailScreenTest.kt:nutritionInfo_displaysCorrectly` |

**Nutrition Fields:**
| Field | Example |
|-------|---------|
| Calories | 180 |
| Protein | 12g |
| Carbs | 22g |
| Fat | 5g |

**Acceptance Criteria:**
- Given: Recipe detail displayed
- When: Nutrition section renders
- Then: Four-column layout with values
- And: "Per Serving" label shown

---

### REC-011: Tab Bar (Ingredients/Instructions)

| Field | Value |
|-------|-------|
| **Screen** | Recipe Detail |
| **Element** | Tab selector |
| **Trigger** | User tap |
| **Status** | Implemented |
| **Test** | `RecipeDetailScreenTest.kt:tabBar_switchesBetweenTabs` |

**Tabs:**
| Tab | Content |
|-----|---------|
| INGREDIENTS | Ingredient list with checkboxes |
| INSTRUCTIONS | Step-by-step instructions |

**Acceptance Criteria:**
- Given: Recipe detail displayed
- When: User taps tab
- Then: Selected tab underlined
- And: Content switches to selected tab
- And: Default is INGREDIENTS

---

### REC-012: Ingredients Tab Content

| Field | Value |
|-------|-------|
| **Screen** | Recipe Detail |
| **Element** | Ingredient list |
| **Trigger** | Ingredients tab selected |
| **Status** | Implemented |
| **Test** | `RecipeDetailScreenTest.kt:ingredientsTab_listsAllIngredients` |

**Acceptance Criteria:**
- Given: Ingredients tab selected
- When: Content displays
- Then: Serving adjuster at top
- And: Each ingredient as checkbox row
- And: Format: "□ [quantity] [unit] [name]"
- And: "+ Add All to Grocery List" button

---

### REC-013: Serving Adjuster

| Field | Value |
|-------|-------|
| **Screen** | Recipe Detail |
| **Element** | Servings dropdown |
| **Trigger** | User changes value |
| **Status** | Implemented |
| **Test** | `RecipeDetailScreenTest.kt:servingAdjuster_scalesIngredients` |

**Acceptance Criteria:**
- Given: Ingredients tab displayed
- When: User changes serving count
- Then: All ingredient quantities recalculate
- And: Options: 2, 4, 6, 8 servings
- And: Cooking time may adjust for larger quantities

---

### REC-015: Add All to Grocery Button

| Field | Value |
|-------|-------|
| **Screen** | Recipe Detail |
| **Element** | Add button |
| **Trigger** | User tap |
| **Status** | Implemented |
| **Test** | `RecipeDetailScreenTest.kt:addToGrocery_addsAllIngredients` |

**Acceptance Criteria:**
- Given: Ingredients tab displayed
- When: User taps "+ Add All to Grocery List"
- Then: All unchecked ingredients added to grocery
- And: Confirmation snackbar shown
- And: Button text changes to "Added ✓" briefly

---

### REC-016: Instructions Tab Content

| Field | Value |
|-------|-------|
| **Screen** | Recipe Detail |
| **Element** | Step list |
| **Trigger** | Instructions tab selected |
| **Status** | Implemented |
| **Test** | `RecipeDetailScreenTest.kt:instructionsTab_listsAllSteps` |

**Acceptance Criteria:**
- Given: Instructions tab selected
- When: Content displays
- Then: Step count shown (e.g., "6 Steps")
- And: Each step in numbered card
- And: Cards vertically scrollable

---

### REC-018: Start Cooking Mode Button

| Field | Value |
|-------|-------|
| **Screen** | Recipe Detail |
| **Element** | Primary action button |
| **Trigger** | User tap |
| **Status** | Implemented |
| **Test** | `RecipeDetailScreenTest.kt:startCooking_navigatesToCookingMode` |

**Acceptance Criteria:**
- Given: Recipe detail displayed
- When: User taps "🍳 START COOKING MODE"
- Then: Navigate to Cooking Mode screen
- And: Recipe data passed to cooking mode

---

### REC-019: Modify with AI Button

| Field | Value |
|-------|-------|
| **Screen** | Recipe Detail |
| **Element** | Secondary action button |
| **Trigger** | User tap |
| **Status** | Implemented |
| **Test** | `RecipeDetailScreenTest.kt:modifyWithAI_opensChatWithContext` |

**Acceptance Criteria:**
- Given: Recipe detail displayed
- When: User taps "💬 Modify with AI"
- Then: Navigate to Chat screen
- And: Chat pre-populated with recipe context
- And: User can ask for modifications

---

## Cooking Mode Requirements

### COOK-001: Cooking Mode Screen

| Field | Value |
|-------|-------|
| **Screen** | Cooking Mode |
| **Element** | Full screen |
| **Trigger** | Start Cooking tapped |
| **Status** | Implemented |
| **Test** | `CookingModeScreenTest.kt:cookingMode_displaysCorrectly` |

**Acceptance Criteria:**
- Given: User entered cooking mode
- When: Screen displays
- Then: Header with recipe name and step counter
- And: Progress bar showing completion
- And: Large step content area
- And: Navigation buttons at bottom
- And: "Screen stays ON" indicator

---

### COOK-003: Progress Indicator

| Field | Value |
|-------|-------|
| **Screen** | Cooking Mode |
| **Element** | Step progress bar |
| **Trigger** | Step navigation |
| **Status** | Implemented |
| **Test** | `CookingModeScreenTest.kt:progressBar_updatesWithSteps` |

**Acceptance Criteria:**
- Given: User is on step N of M
- When: Step changes
- Then: Progress bar fills to N/M
- And: Step counter shows "Step N / M"

---

### COOK-004: Step Content Display

| Field | Value |
|-------|-------|
| **Screen** | Cooking Mode |
| **Element** | Step text area |
| **Trigger** | Step navigation |
| **Status** | Implemented |
| **Test** | `CookingModeScreenTest.kt:stepContent_displaysLargeReadableText` |

**Acceptance Criteria:**
- Given: User viewing a step
- When: Step content renders
- Then: Step number prominently displayed
- And: Instruction text is large and readable
- And: Text optimized for kitchen viewing distance

---

### COOK-006: Timer Button

| Field | Value |
|-------|-------|
| **Screen** | Cooking Mode |
| **Element** | Set Timer button |
| **Trigger** | User tap |
| **Status** | Implemented |
| **Test** | `CookingModeScreenTest.kt:timerButton_startsCountdown` |

**Acceptance Criteria:**
- Given: Step has timer suggestion
- When: User taps "⏱️ SET TIMER [duration]"
- Then: Countdown timer starts
- And: Timer displays in prominent area
- And: Pause and Stop buttons appear

---

### COOK-007: Timer Running State

| Field | Value |
|-------|-------|
| **Screen** | Cooking Mode |
| **Element** | Running timer display |
| **Trigger** | Timer started |
| **Status** | Implemented |
| **Test** | `CookingModeScreenTest.kt:timerRunning_showsCountdown` |

**Acceptance Criteria:**
- Given: Timer is running
- When: Time passes
- Then: Countdown updates every second
- And: Format: "MM:SS" or "H:MM:SS"
- And: PAUSE and STOP buttons visible

---

### COOK-008: Timer Complete

| Field | Value |
|-------|-------|
| **Screen** | Cooking Mode |
| **Element** | Timer alert |
| **Trigger** | Timer reaches 0 |
| **Status** | Implemented |
| **Test** | `CookingModeScreenTest.kt:timerComplete_alertsUser` |

**Acceptance Criteria:**
- Given: Timer running
- When: Timer reaches 0:00
- Then: "⏱️ TIME'S UP!" alert displays
- And: Device vibrates
- And: Sound plays (if not muted)
- And: DISMISS button stops alert

---

### COOK-011: Screen Wake Lock

| Field | Value |
|-------|-------|
| **Screen** | Cooking Mode |
| **Element** | System wake lock |
| **Trigger** | Enter cooking mode |
| **Status** | Implemented |
| **Test** | `CookingModeScreenTest.kt:screenWakeLock_keepScreenOn` |

**Acceptance Criteria:**
- Given: User in cooking mode
- When: Time passes without interaction
- Then: Screen does NOT turn off
- And: "🔒 Screen stays ON" indicator visible
- And: Wake lock released on exit

---

### COOK-012: Cooking Complete Screen

| Field | Value |
|-------|-------|
| **Screen** | Cooking Mode |
| **Element** | Completion overlay |
| **Trigger** | Final step completed |
| **Status** | Implemented |
| **Test** | `CookingModeScreenTest.kt:completionScreen_displaysCorrectly` |

**Acceptance Criteria:**
- Given: User completed final step
- When: User taps Next
- Then: "🎉 Cooking Complete!" screen displays
- And: Recipe name shown
- And: Rating stars displayed
- And: Optional feedback field
- And: DONE and SKIP RATING buttons

---

### COOK-013: Rating Stars

| Field | Value |
|-------|-------|
| **Screen** | Cooking Mode - Complete |
| **Element** | 5-star rating |
| **Trigger** | User tap |
| **Status** | Implemented |
| **Test** | `CookingModeScreenTest.kt:ratingStars_captureRating` |

**Acceptance Criteria:**
- Given: Completion screen displayed
- When: User taps a star
- Then: That star and all before it fill
- And: Rating value stored (1-5)

---

### COOK-015: Done Button

| Field | Value |
|-------|-------|
| **Screen** | Cooking Mode - Complete |
| **Element** | Submit button |
| **Trigger** | User tap |
| **Status** | Implemented |
| **Test** | `CookingModeScreenTest.kt:doneButton_submitsAndExits` |

**Acceptance Criteria:**
- Given: User on completion screen
- When: User taps DONE
- Then: Rating submitted to backend
- And: Feedback saved if provided
- And: Navigate back to recipe detail
- And: Stats updated for achievements

---

## Implementation Files

| Component | File Path |
|-----------|-----------|
| Recipe Detail Screen | `presentation/recipedetail/RecipeDetailScreen.kt` |
| Recipe Detail ViewModel | `presentation/recipedetail/RecipeDetailViewModel.kt` |
| Cooking Mode Screen | `presentation/cookingmode/CookingModeScreen.kt` |
| Cooking Mode ViewModel | `presentation/cookingmode/CookingModeViewModel.kt` |
| Ingredient List | `presentation/recipedetail/components/IngredientList.kt` |
| Step Card | `presentation/recipedetail/components/StepCard.kt` |

## Test Files

| Test Type | File Path |
|-----------|-----------|
| Recipe Detail UI | `app/src/androidTest/java/com/rasoiai/app/presentation/recipedetail/RecipeDetailScreenTest.kt` |
| Recipe Detail Unit | `app/src/test/java/com/rasoiai/app/presentation/recipedetail/RecipeDetailViewModelTest.kt` |
| Cooking Mode UI | `app/src/androidTest/java/com/rasoiai/app/presentation/cookingmode/CookingModeScreenTest.kt` |
| Cooking Mode Unit | `app/src/test/java/com/rasoiai/app/presentation/cookingmode/CookingModeViewModelTest.kt` |

---

*Requirements derived from wireframes: `05-recipe-detail.md`, `06-cooking-mode.md`*
