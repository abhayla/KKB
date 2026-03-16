---
description: >
  All navigation routes MUST be defined as sealed class members in Screen.kt with typed argument
  builders. Never use hardcoded route strings in composables. Screens with arguments MUST have
  a createRoute() factory method.
globs: ["android/app/src/main/java/**/navigation/**/*.kt", "android/app/src/main/java/**/presentation/**/*.kt"]
synthesized: true
private: false
---

# Navigation Screen Pattern

All navigation routes are defined in `presentation/navigation/Screen.kt` as a sealed class hierarchy. Routes are never hardcoded as strings in composables.

## Screen definition pattern

Simple screen (no arguments):
```kotlin
data object Home : Screen("home")
```

Screen with arguments — MUST have `createRoute()` and `ARG_*` constants:
```kotlin
data object RecipeDetail : Screen("recipe/{recipeId}?isLocked={isLocked}&fromMealPlan={fromMealPlan}") {
    fun createRoute(recipeId: String, isLocked: Boolean = false, fromMealPlan: Boolean = false) =
        "recipe/$recipeId?isLocked=$isLocked&fromMealPlan=$fromMealPlan"
    const val ARG_RECIPE_ID = "recipeId"
    const val ARG_IS_LOCKED = "isLocked"
    const val ARG_FROM_MEAL_PLAN = "fromMealPlan"
}
```

## Navigation pattern in composables

```kotlin
// CORRECT — use Screen sealed class members
navController.navigate(Screen.RecipeDetail.createRoute(recipeId = "abc", fromMealPlan = true))

// WRONG — hardcoded route string
navController.navigate("recipe/abc?isLocked=false&fromMealPlan=true")
```

## Route registration in RasoiNavHost.kt

All routes are registered in `RasoiNavHost` composable using `NavHost { composable(...) { } }`:

```kotlin
composable(
    route = Screen.RecipeDetail.route,
    arguments = listOf(
        navArgument(Screen.RecipeDetail.ARG_RECIPE_ID) { type = NavType.StringType },
        navArgument(Screen.RecipeDetail.ARG_IS_LOCKED) { type = NavType.BoolType; defaultValue = false },
    )
) { backStackEntry ->
    val recipeId = backStackEntry.arguments?.getString(Screen.RecipeDetail.ARG_RECIPE_ID) ?: return@composable
    RecipeDetailScreen(recipeId = recipeId, ...)
}
```

## Auth flow navigation

Auth-related navigation uses `popUpTo` with `inclusive = true` to clear the back stack:

```kotlin
navController.navigate(Screen.Home.route) {
    popUpTo(Screen.Auth.route) { inclusive = true }
}
```

## Current screens (as of last sync)

| Category | Screens |
|----------|---------|
| Auth flow | Splash, Auth, Onboarding |
| Main | Home, Grocery, Favorites, Chat, Settings, Notifications |
| Detail | RecipeDetail, CookingMode |
| Feature | Pantry, Stats, RecipeRules, Achievements |
| Household | HouseholdSettings, HouseholdMembers, HouseholdMemberDetail, JoinHousehold, HouseholdMealPlan, HouseholdRecipeRules, HouseholdNotifications, HouseholdStats |
| Settings sub | CuisinePreferences, DietaryRestrictions, DislikedIngredients, CookingTime, SpiceLevel, FamilyMembers, NotificationSettings, ConnectedAccounts, EditProfile, FriendsLeaderboard, Units |

## Adding a new screen

1. Add sealed class member to `Screen.kt` (with `createRoute()` if it takes arguments)
2. Add `composable(route = Screen.NewScreen.route)` block in `RasoiNavHost.kt`
3. Create the screen composable in `presentation/newfeature/NewFeatureScreen.kt`
4. Create the ViewModel extending `BaseViewModel<NewFeatureUiState>`

## MUST NOT

- MUST NOT hardcode route strings anywhere outside `Screen.kt` — all routes come from the sealed class
- MUST NOT navigate without `popUpTo` on auth transitions — stale auth screens on the back stack cause crashes
- MUST NOT add arguments to a Screen route without providing `createRoute()` and `ARG_*` constants
- MUST NOT create new Screen entries without registering them in `RasoiNavHost.kt`
