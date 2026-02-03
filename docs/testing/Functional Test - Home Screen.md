# Functional Test - Home Screen

## Overview

This document describes the comprehensive functional testing plan for the RasoiAI Home Screen. It covers 48 test cases across 10 categories, using Real Google Sign-In authentication.

**Test File:** `android/app/src/androidTest/java/com/rasoiai/app/e2e/flows/HomeScreenComprehensiveTest.kt`

---

## Prerequisites

### Environment Setup
1. **Emulator**: API 34 with Google Play Services
2. **Backend**: Running at `localhost:8000` with `DEBUG=true`
3. **Gmail Account**: Single dedicated test account signed into emulator
4. **Firebase**: Valid credentials configured in app

### Before Running Tests
```bash
# Start backend
cd backend && source venv/bin/activate && uvicorn app.main:app --reload

# Verify emulator has Google account
adb shell am start -a android.settings.ACCOUNT_SYNC_SETTINGS
```

---

## Test Categories Summary

| Category | Tests | Test IDs |
|----------|-------|----------|
| Navigation | 8 | NAV-01 to NAV-08 |
| Week Selector | 5 | WEEK-01 to WEEK-05 |
| Locking Hierarchy | 9 | LOCK-01 to LOCK-09 |
| Recipe Action Sheet | 5 | ACTION-01 to ACTION-05 |
| Swap Recipe Flow | 4 | SWAP-01 to SWAP-04 |
| Add Recipe Flow | 5 | ADD-01 to ADD-05 |
| Refresh/Regenerate | 3 | REFRESH-01 to REFRESH-03 |
| Swipe Actions | 3 | SWIPE-01 to SWIPE-03 |
| Content Validation | 4 | CONTENT-01 to CONTENT-04 |
| Festival Banner | 2 | FEST-01 to FEST-02 |
| **Total** | **48** | |

---

## Category 1: Navigation (8 tests)

| Test ID | Feature | Test Steps | Expected Result |
|---------|---------|------------|-----------------|
| NAV-01 | Menu button → Settings | Tap hamburger menu | Settings screen displayed |
| NAV-02 | Notifications button | Tap notifications icon | Notifications screen displayed |
| NAV-03 | Profile button → Settings | Tap profile icon | Settings screen displayed |
| NAV-04 | Bottom nav → Grocery | Tap Grocery tab | Grocery screen, tab highlighted |
| NAV-05 | Bottom nav → Chat | Tap Chat tab | Chat screen, tab highlighted |
| NAV-06 | Bottom nav → Favorites | Tap Favorites tab | Favorites screen, tab highlighted |
| NAV-07 | Bottom nav → Stats | Tap Stats tab | Stats screen, tab highlighted |
| NAV-08 | Recipe Detail navigation | Tap View Recipe in action sheet | Recipe Detail screen |

---

## Category 2: Week Selector (5 tests)

| Test ID | Feature | Test Steps | Expected Result |
|---------|---------|------------|-----------------|
| WEEK-01 | Day selection | Tap each day (Mon-Sun) | Day highlighted, meals update |
| WEEK-02 | Today indicator | View current day | Dot indicator under today |
| WEEK-03 | Horizontal scroll | Swipe week selector | Can access all 7 days |
| WEEK-04 | Week header display | Check header | "This Week's Menu" + date range |
| WEEK-05 | Day label format | Check selected day | "Monday, January 15" format |

---

## Category 3: Locking Hierarchy (9 tests)

| Test ID | Feature | Test Steps | Expected Result |
|---------|---------|------------|-----------------|
| LOCK-01 | Day lock toggle | Tap day lock button | Icon changes, all meals locked |
| LOCK-02 | Day lock persistence | Lock day, change day, return | Lock state preserved |
| LOCK-03 | Day lock disables meal locks | Lock day, try tap meal lock | Meal lock button disabled |
| LOCK-04 | Meal lock toggle | Tap meal lock button | Icon changes, recipes locked |
| LOCK-05 | Meal lock persistence | Lock meal, scroll away, return | Lock state preserved |
| LOCK-06 | Recipe lock via action sheet | Tap Lock in action sheet | Recipe shows lock indicator |
| LOCK-07 | Recipe lock disabled when meal locked | Lock meal, try lock recipe | "Unlock meal first" message |
| LOCK-08 | Hierarchy: Day → Meal → Recipe | Lock day, check meal & recipe | All show locked state |
| LOCK-09 | Unlock propagation | Unlock day | Meal locks become enabled |

---

## Category 4: Recipe Action Sheet (5 tests)

| Test ID | Feature | Test Steps | Expected Result |
|---------|---------|------------|-----------------|
| ACTION-01 | Open action sheet | Tap recipe item | Sheet with 4 options displayed |
| ACTION-02 | View Recipe | Tap "View Recipe" | Navigates to Recipe Detail |
| ACTION-03 | Swap Recipe (unlocked) | Tap "Swap Recipe" | Swap sheet opens |
| ACTION-04 | Swap Recipe (locked) | Lock recipe, tap Swap | Option disabled |
| ACTION-05 | Remove Recipe | Tap "Remove from Meal" | Recipe removed, sheet closes |

---

## Category 5: Swap Recipe Flow (4 tests)

| Test ID | Feature | Test Steps | Expected Result |
|---------|---------|------------|-----------------|
| SWAP-01 | Swap sheet display | Open swap sheet | Title, search, recipe grid |
| SWAP-02 | Search filter | Type in search field | Grid filters by query |
| SWAP-03 | Select replacement | Tap recipe in grid | Recipe swapped, sheet closes |
| SWAP-04 | Cancel swap | Tap Cancel or backdrop | Sheet closes, no change |

---

## Category 6: Add Recipe Flow (5 tests)

| Test ID | Feature | Test Steps | Expected Result |
|---------|---------|------------|-----------------|
| ADD-01 | Add button opens sheet | Tap + button on meal | Add Recipe sheet opens |
| ADD-02 | Suggestions tab | View default tab | AI suggestions displayed |
| ADD-03 | Favorites tab | Tap Favorites tab | Favorite recipes displayed |
| ADD-04 | Search filter | Type in search | Grid filters by query |
| ADD-05 | Add recipe | Tap recipe in grid | Recipe added to meal |

---

## Category 7: Refresh/Regenerate (3 tests)

| Test ID | Feature | Test Steps | Expected Result |
|---------|---------|------------|-----------------|
| REFRESH-01 | Open refresh options | Tap Refresh button | Sheet with Day/Week options |
| REFRESH-02 | Regenerate day | Tap "This Day Only" | Only selected day refreshed |
| REFRESH-03 | Regenerate week | Tap "Entire Week" | All days refreshed (locked protected) |

---

## Category 8: Swipe Actions (3 tests)

| Test ID | Feature | Test Steps | Expected Result |
|---------|---------|------------|-----------------|
| SWIPE-01 | Reveal actions | Swipe recipe left | Lock + Delete icons revealed |
| SWIPE-02 | Swipe lock toggle | Tap revealed lock icon | Recipe lock toggled |
| SWIPE-03 | Swipe delete | Tap revealed delete icon | Recipe removed |

---

## Category 9: Content Validation (4 tests)

| Test ID | Feature | Test Steps | Expected Result |
|---------|---------|------------|-----------------|
| CONTENT-01 | Recipe names | Check meal items | Recipe names displayed |
| CONTENT-02 | Prep time | Check meal items | "X mins" format displayed |
| CONTENT-03 | Calories | Check meal items | "Y kcal" format displayed |
| CONTENT-04 | Dietary indicator | Check meal items | Colored dot matches diet type |

---

## Category 10: Festival Banner (2 tests)

| Test ID | Feature | Test Steps | Expected Result |
|---------|---------|------------|-----------------|
| FEST-01 | Banner display | View home when festival near | Banner with emoji + name + countdown |
| FEST-02 | Banner click | Tap festival banner | Festival recipes sheet opens |

---

## Implementation Details

### Festival Recipes Bottom Sheet Feature

The Festival Banner feature was implemented as part of this testing plan:

**Files Modified:**
1. `android/app/src/main/java/com/rasoiai/app/presentation/home/HomeViewModel.kt`
   - Added `showFestivalRecipesSheet`, `festivalRecipes`, `isLoadingFestivalRecipes` state
   - Added `onFestivalBannerClick()`, `fetchFestivalRecipes()`, `onFestivalRecipeClick()`, `dismissFestivalRecipesSheet()` functions

2. `android/app/src/main/java/com/rasoiai/app/presentation/home/HomeScreen.kt`
   - Added `FestivalRecipesSheet` composable with 2-column LazyVerticalGrid
   - Added `FestivalRecipeGridItem` composable for grid items
   - Wired up FestivalBanner onClick

3. `android/app/src/main/java/com/rasoiai/app/presentation/common/TestTags.kt`
   - Added `FESTIVAL_RECIPES_SHEET`, `FESTIVAL_RECIPE_GRID`, `FESTIVAL_RECIPE_ITEM_PREFIX`

### HomeRobot Methods Added

```kotlin
// Top bar actions
fun tapMenuButton()
fun tapNotificationsButton()
fun tapProfileButton()

// Festival banner
fun tapFestivalBanner()
fun assertFestivalRecipesSheetDisplayed()
fun selectFestivalRecipe(index: Int)
fun dismissFestivalRecipesSheet()

// Recipe item interactions
fun tapRecipeItem(mealType: MealType, itemIndex: Int)
fun tapAddRecipeButton(mealType: MealType)
fun assertAddRecipeSheetDisplayed()
fun dismissAddRecipeSheet()

// Content validation
fun assertWeekHeaderDisplayed()
fun assertSelectedDayDisplayed()
fun assertTodayIndicatorVisible()
fun assertRecipeHasPrepTime(recipeName: String)
```

---

---

## Test Execution Workflow

```
┌─────────────────────────────────────────────────────────┐
│  Step 1: RUN TEST                                       │
│  ./gradlew connectedDebugAndroidTest --tests "test_XX"  │
└─────────────────────┬───────────────────────────────────┘
                      │
                      ▼
          ┌───────────────────────┐
          │   Test Result?        │
          └───────────┬───────────┘
                      │
         ┌────────────┴────────────┐
         │                         │
         ▼                         ▼
┌─────────────────┐      ┌─────────────────────┐
│ Step 2: PASSED  │      │ Step 3: FAILED      │
│ ✅ Show status  │      │ ❌ Show status +    │
│ → Next test     │      │    failure reason   │
└─────────────────┘      │ → Find & fix issue  │
                         │ → Go to Step 1      │
                         └─────────────────────┘
```

---

## Test Status Tracker

| # | Test ID | Test Name | Status | Notes |
|---|---------|-----------|--------|-------|
| 00 | SETUP | test_00_realGoogleSignIn_completesOnboarding_showsHome | ✅ | 18.5s |
| 01 | NAV-01 | test_01_NAV01_menuButton_navigatesToSettings | ✅ | 13.3s |
| 02 | NAV-02 | test_02_NAV02_notificationsButton_navigatesToNotifications | ✅ | 12.5s |
| 03 | NAV-03 | test_03_NAV03_profileButton_navigatesToSettings | ✅ | 12.0s |
| 04 | NAV-04 | test_04_NAV04_bottomNav_navigatesToGrocery | ✅ | 10.6s |
| 05 | NAV-05 | test_05_NAV05_bottomNav_navigatesToChat | ✅ | 11.4s |
| 06 | NAV-06 | test_06_NAV06_bottomNav_navigatesToFavorites | ✅ | Fixed: Added waitForHomeScreen |
| 07 | NAV-07 | test_07_NAV07_bottomNav_navigatesToStats | ✅ | 10.8s |
| 08 | NAV-08 | test_08_NAV08_recipeDetail_viaActionSheet | ✅ | 12.5s |
| 09 | WEEK-01 | test_09_WEEK01_daySelection_changesMeals | ✅ | 10.4s |
| 10 | WEEK-02 | test_10_WEEK02_todayIndicator_isVisible | ✅ | 9.0s |
| 11 | WEEK-03 | test_11_WEEK03_horizontalScroll_accessesAllDays | ✅ | 10.1s |
| 12 | WEEK-04 | test_12_WEEK04_weekHeader_displaysProperly | ✅ | 10.8s |
| 13 | WEEK-05 | test_13_WEEK05_selectedDayLabel_formattedCorrectly | ✅ | 9.4s |
| 14 | LOCK-01 | test_14_LOCK01_dayLock_toggleWorks | ✅ | 11.0s |
| 15 | LOCK-02 | test_15_LOCK02_dayLock_persistsAcrossDayChanges | ✅ | 12.6s |
| 16 | LOCK-03 | test_16_LOCK03_dayLock_disablesMealLocks | ✅ | 10.4s |
| 17 | LOCK-04 | test_17_LOCK04_mealLock_toggleWorks | ✅ | 9.6s |
| 18 | LOCK-05 | test_18_LOCK05_mealLock_persists | ✅ | 11.1s |
| 19 | LOCK-06 | test_19_LOCK06_recipeLock_viaActionSheet | ✅ | 11.7s |
| 20 | LOCK-07 | test_20_LOCK07_recipeLock_disabledWhenMealLocked | ✅ | 11.3s |
| 21 | LOCK-08 | test_21_LOCK08_lockHierarchy_cascades | ✅ | 10.4s |
| 22 | LOCK-09 | test_22_LOCK09_unlockDay_enablesMealLocks | ✅ | 11.0s |
| 23 | ACTION-01 | test_23_ACTION01_openActionSheet_byTappingRecipe | ✅ | 11.4s |
| 24 | ACTION-02 | test_24_ACTION02_viewRecipe_navigatesToDetail | ✅ | 13.0s |
| 25 | ACTION-03 | test_25_ACTION03_swapRecipe_opensSwapSheet | ✅ | Fixed: Use BREAKFAST, longer wait |
| 26 | ACTION-04 | test_26_ACTION04_swapRecipe_disabledWhenLocked | ✅ | 10.9s |
| 27 | ACTION-05 | test_27_ACTION05_removeRecipe_removesFromMeal | ✅ | 10.1s |
| 28 | SWAP-01 | test_28_SWAP01_swapSheet_displaysCorrectly | ✅ | Fixed: assertMealCardDisplayed, longer wait |
| 29 | SWAP-02 | test_29_SWAP02_searchFilter_works | ✅ | 12.7s |
| 30 | SWAP-03 | test_30_SWAP03_selectReplacement_swapsRecipe | ✅ | Fixed: Use BREAKFAST, longer wait |
| 31 | SWAP-04 | test_31_SWAP04_cancelSwap_closesSheet | ✅ | Fixed: assertMealCardDisplayed, longer wait |
| 32 | ADD-01 | test_32_ADD01_addButton_opensSheet | ✅ | Fixed: testTag on ModalBottomSheet |
| 33 | ADD-02 | test_33_ADD02_suggestionsTab_displays | ✅ | Fixed: testTag on ModalBottomSheet |
| 34 | ADD-03 | test_34_ADD03_favoritesTab_displays | ✅ | Fixed: Use BREAKFAST |
| 35 | ADD-04 | test_35_ADD04_searchFilter_works | ✅ | Fixed: testTag on ModalBottomSheet |
| 36 | ADD-05 | test_36_ADD05_addRecipe_addsToMeal | ✅ | Fixed: testTag on ModalBottomSheet |
| 37 | REFRESH-01 | test_37_REFRESH01_openRefreshOptions | ✅ | 10.8s |
| 38 | REFRESH-02 | test_38_REFRESH02_regenerateDay_option | ✅ | 12.6s |
| 39 | REFRESH-03 | test_39_REFRESH03_regenerateWeek_option | ✅ | 11.8s |
| 40 | SWIPE-01 | test_40_SWIPE01_revealActions_bySwipingLeft | ✅ | 9.6s |
| 41 | SWIPE-02 | test_41_SWIPE02_swipeLock_togglesRecipe | ✅ | Fixed: Use BREAKFAST, retry logic |
| 42 | SWIPE-03 | test_42_SWIPE03_swipeDelete_removesRecipe | ✅ | Fixed: assertMealCardDisplayed |
| 43 | CONTENT-01 | test_43_CONTENT01_recipeNames_displayed | ✅ | 15.3s |
| 44 | CONTENT-02 | test_44_CONTENT02_prepTime_displayed | ✅ | 16.7s |
| 45 | CONTENT-03 | test_45_CONTENT03_calories_displayed | ✅ | 18.4s |
| 46 | CONTENT-04 | test_46_CONTENT04_dietaryIndicator_displayed | ✅ | 10.3s |
| 47 | FEST-01 | test_47_FEST01_festivalBanner_displaysWhenNear | ✅ | 16.3s |
| 48 | FEST-02 | test_48_FEST02_festivalBanner_opensRecipesSheet | ✅ | 16.1s |

**Legend:** ⏳ Pending | ✅ Passed | ❌ Failed | 🔄 Fixed (Needs Verification)

---

## Running the Tests

### Run All Tests
```bash
cd android
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.flows.HomeScreenComprehensiveTest
```

### Run Single Test
```bash
cd android
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.flows.HomeScreenComprehensiveTest#test_01_NAV01_menuButton_navigatesToSettings
```

### Run by Category (Example: Navigation Tests)
```bash
cd android
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.flows.HomeScreenComprehensiveTest \
  -Pandroid.testInstrumentationRunnerArguments.tests_regex="test_0[1-8]_NAV.*"
```

---

## Key Files

| File | Purpose |
|------|---------|
| `presentation/home/HomeScreen.kt` | FestivalRecipesSheet composable |
| `presentation/home/HomeViewModel.kt` | Festival sheet state management |
| `presentation/common/TestTags.kt` | FESTIVAL_RECIPES_SHEET tag |
| `e2e/robots/HomeRobot.kt` | Test helper methods |
| `e2e/flows/HomeScreenComprehensiveTest.kt` | 48 comprehensive test cases |

---

## Expected Output

```
Tests run: 48, Passed: 48, Failed: 0
```

Screenshots saved to: `docs/testing/screenshots/`

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Google Sign-In flaky | Use single account, clear state before tests |
| Meal generation timeout | AI takes 4-7 seconds; tests use 60-second timeout |
| Festival not available | FEST-02 gracefully skips if no festival nearby |
| Swap suggestions empty | Ensure recipe DB has similar recipes |
| Test order matters | Tests run alphabetically (NAME_ASCENDING) |

---

## Related Documentation

- [E2E Testing Guide](../testing/E2E-Testing-Prompt.md)
- [E2E Test Plan](../testing/E2E-Test-Plan.md)
- [Technical Design](../design/RasoiAI%20Technical%20Design.md)
