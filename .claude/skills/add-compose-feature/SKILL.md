---
name: add-compose-feature
description: >
  Scaffold a new feature screen in the Android app with ViewModel, UiState,
  navigation route, TestTags, and NavHost registration. Ensures all 6+ files
  are created consistently.
type: workflow
allowed-tools: "Bash Read Write Edit Grep Glob"
argument-hint: "<FeatureName>"
version: "1.0.0"
synthesized: true
private: false
---

# Add Compose Feature Screen

Scaffold a complete feature following the project's presentation layer conventions.

**Arguments:** $ARGUMENTS — e.g., `NutritionTracker`

## STEP 1: Define the Navigation Route

Add to `android/app/src/main/java/com/rasoiai/app/presentation/navigation/Screen.kt`:

```kotlin
// Simple route (no args):
data object <FeatureName> : Screen("<feature-name>")

// Route with args:
data object <FeatureName> : Screen("<feature-name>/{<argName>}") {
    fun createRoute(<argName>: String) = "<feature-name>/$<argName>"
    const val ARG_<ARG_NAME> = "<argName>"
}
```

MUST use `createRoute()` helper for routes with arguments. NEVER construct route strings manually.

## STEP 2: Create UiState Data Class

Create `android/app/src/main/java/com/rasoiai/app/presentation/<featurename>/<FeatureName>ViewModel.kt` with the UiState at the top:

```kotlin
data class <FeatureName>UiState(
    override val isLoading: Boolean = true,
    override val error: String? = null,
    // Feature-specific fields here
) : BaseUiState
```

The UiState MUST implement `BaseUiState` (requires `isLoading` and `error` fields).

## STEP 3: Create the ViewModel

In the same file, below the UiState:

```kotlin
@HiltViewModel
class <FeatureName>ViewModel @Inject constructor(
    // Inject repositories and use cases via constructor
) : BaseViewModel<<FeatureName>UiState>(<FeatureName>UiState()) {

    init {
        load<FeatureName>()
    }

    private fun load<FeatureName>() {
        updateState { it.copy(isLoading = true) }
        viewModelScope.launch {
            // Load data
            updateState { it.copy(isLoading = false) }
        }
    }
}
```

Use `updateState { }` from `BaseViewModel` — NEVER use `_uiState.update {}` directly.

If the feature needs one-shot navigation events, add:
```kotlin
private val _navigationEvent = Channel<<FeatureName>NavigationEvent>(Channel.BUFFERED)
val navigationEvent: Flow<<FeatureName>NavigationEvent> = _navigationEvent.receiveAsFlow()
```

## STEP 4: Create the Screen Composable

Create `android/app/src/main/java/com/rasoiai/app/presentation/<featurename>/<FeatureName>Screen.kt`:

```kotlin
@Composable
fun <FeatureName>Screen(
    viewModel: <FeatureName>ViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Compose UI here
}
```

Place reusable sub-components in a `components/` subdirectory.

## STEP 5: Add TestTags

Add constants to `android/app/src/main/java/com/rasoiai/app/presentation/common/TestTags.kt`:

```kotlin
// <FeatureName>
const val <FEATURE>_SCREEN = "<feature>_screen"
const val <FEATURE>_LOADING = "<feature>_loading"
const val <FEATURE>_ERROR = "<feature>_error"
// Add feature-specific tags
```

Use `_PREFIX` suffix for tags that get dynamic IDs appended.

## STEP 6: Register in NavHost

Add the composable to `android/app/src/main/java/com/rasoiai/app/presentation/navigation/RasoiNavHost.kt`:

```kotlin
composable(Screen.<FeatureName>.route) {
    <FeatureName>Screen(
        onNavigateBack = { navController.popBackStack() }
    )
}
```

For routes with arguments, add `arguments = listOf(navArgument(...))`.

## CRITICAL RULES

- NEVER skip NavHost registration — the screen compiles but is unreachable without it
- NEVER put UiState in a separate file — it lives at the top of the ViewModel file
- MUST implement `BaseUiState` interface — `isLoading` and `error` fields are required
- MUST use `BaseViewModel<T>` with `updateState {}` — not raw `MutableStateFlow`
- MUST add TestTags BEFORE using `testTag()` in the Screen composable
- MUST use `collectAsStateWithLifecycle()` (not `collectAsState()`) for lifecycle awareness
