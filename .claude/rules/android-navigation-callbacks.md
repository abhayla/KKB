---
description: Compose screen navigation callback conventions — onNavigate* lambdas, parameter order, ViewModel injection.
globs: ["android/app/src/main/java/**/presentation/**/*.kt"]
---

# Android Navigation Callback Conventions

## Screen Composable Signature

Every screen composable MUST follow this parameter order:

```kotlin
@Composable
fun FeatureScreen(
    // 1. Navigation callbacks (onNavigate* lambdas)
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (id: String) -> Unit,
    onNavigateToSettings: () -> Unit,
    // 2. Optional callbacks with defaults
    onNavigateToNotifications: () -> Unit = {},
    // 3. ViewModel — always LAST
    viewModel: FeatureViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // ...
}
```

## Callback Naming

All navigation callbacks MUST use the `onNavigate` prefix:

| Pattern | Use When |
|---------|----------|
| `onNavigateBack: () -> Unit` | Going back in the navigation stack |
| `onNavigateTo{Screen}: () -> Unit` | Simple navigation to a screen |
| `onNavigateTo{Screen}: (id: String) -> Unit` | Navigation with a single ID |
| `onNavigateTo{Screen}: (id: String, flag: Boolean) -> Unit` | Navigation with multiple params |

Non-navigation callbacks use the `on` prefix without `Navigate`:
```kotlin
onLogout: () -> Unit
onDismiss: () -> Unit
```

## Data in Callbacks

Pass ONLY the minimum data needed for the destination:
- IDs (String) — for loading data on the destination screen
- Display flags (Boolean) — for UI state (`isLocked`, `fromMealPlan`)
- NEVER pass entire domain objects through navigation callbacks

```kotlin
// CORRECT — minimal data
onNavigateToRecipeDetail: (recipeId: String, isLocked: Boolean, fromMealPlan: Boolean) -> Unit

// WRONG — passing domain object
onNavigateToRecipeDetail: (recipe: Recipe) -> Unit
```

## Central Dispatch in NavHost

All navigation callbacks are wired in `RasoiNavHost.kt`:

```kotlin
composable(route = Screen.Home.route) {
    HomeScreen(
        onNavigateToRecipeDetail = { recipeId, isLocked, fromMealPlan ->
            navController.navigate(Screen.RecipeDetail.createRoute(recipeId, isLocked, fromMealPlan))
        },
        onNavigateToSettings = {
            navController.navigate(Screen.Settings.route)
        }
    )
}
```

Screen composables NEVER receive `NavController` directly. Navigation is always through lambdas dispatched from `RasoiNavHost`.

## Default Parameters

Optional navigation callbacks (rarely used screens) MAY have empty lambda defaults:
```kotlin
onNavigateToNotifications: () -> Unit = {}
```

This allows screens to be used in previews without providing all callbacks.

## ViewModel Injection

The ViewModel parameter MUST be:
- Last in the parameter list
- Have a default value of `hiltViewModel()`
- Use the screen-specific ViewModel type (not `BaseViewModel`)

```kotlin
viewModel: HomeViewModel = hiltViewModel()  // CORRECT
viewModel: BaseViewModel<*> = hiltViewModel()  // WRONG
```

## Anti-Patterns

- NEVER pass `NavController` to screen composables — use lambda callbacks
- NEVER put navigation logic inside ViewModels — ViewModels emit events, screens navigate
- NEVER use `rememberNavController()` inside screen composables
- NEVER pass domain objects through navigation — pass IDs and load on the destination
