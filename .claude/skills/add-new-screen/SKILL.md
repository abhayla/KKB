---
name: add-new-screen
description: >
  Scaffold a complete new screen in the RasoiAI Android app: Screen route, NavHost entry,
  ViewModel with BaseUiState, Composable screen, Hilt DI wiring, and test stubs.
  Use when adding a new feature screen to the app.
type: workflow
allowed-tools: "Bash Read Write Edit Grep Glob"
argument-hint: "<screen-name> [--with-args arg1:Type arg2:Type]"
version: "1.0.0"
synthesized: true
---

# Add New Screen

Scaffold a complete new screen following RasoiAI conventions.

**Request:** $ARGUMENTS

---

## STEP 1: Parse Arguments

Extract from `$ARGUMENTS`:
- **Screen name**: e.g., `NutritionTracker`
- **Arguments** (optional): e.g., `--with-args userId:String weekId:String`

Derive naming:
- Package: `presentation/nutritiontracker/`
- ViewModel: `NutritionTrackerViewModel`
- UiState: `NutritionTrackerUiState`
- Screen route: `nutrition-tracker` (kebab-case)
- Screen sealed class: `NutritionTracker`

## STEP 2: Create Screen Route

Add sealed class member to `android/app/src/main/java/com/rasoiai/app/presentation/navigation/Screen.kt`:

For no-argument screens:
```kotlin
data object NutritionTracker : Screen("nutrition-tracker")
```

For screens with arguments:
```kotlin
data object NutritionTracker : Screen("nutrition-tracker/{userId}") {
    fun createRoute(userId: String) = "nutrition-tracker/$userId"
    const val ARG_USER_ID = "userId"
}
```

## STEP 3: Create UiState

Create `android/app/src/main/java/com/rasoiai/app/presentation/nutritiontracker/NutritionTrackerUiState.kt`:

```kotlin
package com.rasoiai.app.presentation.nutritiontracker

import com.rasoiai.app.presentation.common.BaseUiState

data class NutritionTrackerUiState(
    override val isLoading: Boolean = true,
    override val error: String? = null,
    // Add screen-specific fields here
) : BaseUiState
```

## STEP 4: Create ViewModel

Create `android/app/src/main/java/com/rasoiai/app/presentation/nutritiontracker/NutritionTrackerViewModel.kt`:

```kotlin
package com.rasoiai.app.presentation.nutritiontracker

import androidx.lifecycle.viewModelScope
import com.rasoiai.app.presentation.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NutritionTrackerViewModel @Inject constructor(
    // Inject use cases / repositories here
) : BaseViewModel<NutritionTrackerUiState>(NutritionTrackerUiState()) {

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            // Load data here
            updateState { it.copy(isLoading = false) }
        }
    }

    override fun onErrorDismissed() {
        updateState { it.copy(error = null) }
    }
}
```

## STEP 5: Create Screen Composable

Create `android/app/src/main/java/com/rasoiai/app/presentation/nutritiontracker/NutritionTrackerScreen.kt`:

```kotlin
package com.rasoiai.app.presentation.nutritiontracker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun NutritionTrackerScreen(
    onNavigateBack: () -> Unit,
    viewModel: NutritionTrackerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Screen content here
}
```

## STEP 6: Register in NavHost

Add composable block to `android/app/src/main/java/com/rasoiai/app/presentation/navigation/RasoiNavHost.kt`:

```kotlin
composable(route = Screen.NutritionTracker.route) {
    NutritionTrackerScreen(
        onNavigateBack = { navController.popBackStack() }
    )
}
```

For screens with arguments, add `arguments` and `navArgument`:
```kotlin
composable(
    route = Screen.NutritionTracker.route,
    arguments = listOf(
        navArgument(Screen.NutritionTracker.ARG_USER_ID) { type = NavType.StringType }
    )
) { backStackEntry ->
    val userId = backStackEntry.arguments?.getString(Screen.NutritionTracker.ARG_USER_ID) ?: return@composable
    NutritionTrackerScreen(userId = userId, onNavigateBack = { navController.popBackStack() })
}
```

## STEP 7: Add TestTags

Add constants to `android/app/src/main/java/com/rasoiai/app/presentation/common/TestTags.kt`:

```kotlin
// NutritionTracker
const val NUTRITION_TRACKER_SCREEN = "nutrition_tracker_screen"
const val NUTRITION_TRACKER_LOADING = "nutrition_tracker_loading"
// Add more as needed
```

## STEP 8: Create Test Stubs

Create unit test stub at `android/app/src/androidTest/java/com/rasoiai/app/presentation/nutritiontracker/NutritionTrackerScreenTest.kt` and ViewModel test at the corresponding test directory.

## STEP 9: Verify

```bash
cd android && ./gradlew assembleDebug
```

If build succeeds, the screen is properly wired.

---

## CRITICAL RULES

- MUST extend `BaseViewModel<T : BaseUiState>` — never raw `ViewModel()`
- MUST define Screen route in `Screen.kt` sealed class — never hardcode routes
- MUST register in `RasoiNavHost.kt` — unregistered screens cause runtime crashes
- MUST add `TestTags` constants for every interactive element
- If the screen needs a new repository, also update `RepositoryModule.kt` with a `@Binds` entry
