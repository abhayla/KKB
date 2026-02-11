# Flow 03: Recipe Interaction

## Metadata
- **Flow Name:** `recipe-interaction`
- **Goal:** Test recipe detail viewing, favoriting, cooking mode completion, and unfavoriting
- **Preconditions:** User has a meal plan with recipes
- **Estimated Duration:** 5-8 minutes
- **Screens Covered:** Home, Recipe Detail, Cooking Mode, Favorites
- **Depends On:** none (needs any meal plan; will use existing or generate)
- **State Produced:** One recipe marked as cooked (cooking stats updated)

## Prerequisites

Beyond standard D1-D7 prerequisites:
- [ ] User has at least one meal plan (generated via onboarding or API)
- [ ] App on Home screen with meal cards visible

## Test User Persona

Uses existing Sharma family data. No settings changes in this flow.

## Steps

### Phase A: Recipe Detail Exploration (Steps 1-4)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| A1 | On Home, tap a BREAKFAST meal card | Action sheet: "View Recipe", "Swap", "Lock", "Remove" | — | — |
| A2 | Tap "View Recipe" | Recipe Detail screen loads | `flow03_recipe_detail.png` | — |
| A3 | Verify recipe content | Recipe name, cuisine, prep time, Ingredients section | — | — |
| A4 | Verify servings selector | Servings count visible (should match household size ~4) | — | — |

### Phase B: Favorite Toggle (Steps 5-7)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| B1 | Tap Favorite button (heart icon) | Heart fills/toggles, snackbar "Added to favorites" | `flow03_favorited.png` | — |
| B2 | Note the recipe name | Record name for later verification in Favorites screen | — | — |
| B3 | Verify favorite state persisted in UI | Heart icon remains filled after scroll up/down | — | — |

### Phase C: Cooking Mode (Steps 8-12)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| C1 | Scroll down to "Start Cooking" button | Button visible | — | — |
| C2 | Tap "Start Cooking" | Cooking Mode screen loads | `flow03_cooking_mode.png` | — |
| C3 | Verify step 1 | "Step 1 of N" visible, instruction text | — | — |
| C4 | Tap Next until last step | Each step advances, counter increments | — | — |
| C5 | Tap "Complete" / "Finish Cooking" on last step | Completion dialog or return to Recipe Detail | `flow03_cooking_complete.png` | — |

### Phase D: Verify Favorites (Steps 13-16)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| D1 | Navigate to Home (press BACK if needed) | Home screen | — | — |
| D2 | Tap bottom nav "Favs" | Favorites screen | `flow03_favorites.png` | — |
| D3 | Verify the favorited recipe appears | Recipe name from B2 visible in list | — | — |
| D4 | Tap the favorited recipe card | Recipe Detail loads for that recipe | — | — |

### Phase E: Unfavorite (Steps 17-19)

| Step | Action | Expected | Screenshot | Validation |
|------|--------|----------|------------|------------|
| E1 | On Recipe Detail, tap Favorite button again | Heart unfills, snackbar "Removed from favorites" | — | — |
| E2 | Press BACK to Favorites | Favorites screen | — | — |
| E3 | Verify recipe removed from favorites | Empty state OR recipe no longer in list | `flow03_unfavorited.png` | — |

## Validation Checkpoints

No `validate_meal_plan.py` checkpoints in this flow — validation is UI-based:
- Favorite toggle state persists across navigation
- Cooking mode step counter is accurate
- Completion registers (cooking stats may update)

## Fix Strategy

**Relevant files for this flow:**
- Recipe Detail: `app/presentation/recipe/RecipeDetailViewModel.kt`, `RecipeDetailScreen.kt`
- Cooking Mode: `app/presentation/cooking/CookingModeViewModel.kt`, `CookingModeScreen.kt`
- Favorites: `app/presentation/favorites/FavoritesViewModel.kt`, `FavoritesScreen.kt`
- Favorite DAO: `data/local/dao/FavoriteDao.kt`
- Cooked recipe tracking: `data/local/dao/StatsDao.kt`

**Common issues:**
- Favorite state not persisting → check Room FavoriteDao insert/delete
- Cooking mode stuck on step → check step navigation logic
- "Start Cooking" not visible → need scroll, check element bounds

## Screen Coverage

| Screen | Step(s) | Verification |
|--------|---------|-------------|
| Home | A1, D1 | Meal card tap, action sheet |
| Recipe Detail | A2-A4, B1-B3, D4, E1 | Content, favorite toggle |
| Cooking Mode | C1-C5 | Step nav, completion |
| Favorites | D2-D3, E2-E3 | List, empty state |
