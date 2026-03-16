---
description: >
  All ViewModels MUST extend BaseViewModel<T : BaseUiState> and follow the single UiState
  data class pattern with StateFlow. Never use LiveData or multiple state flows.
globs: ["android/app/src/main/java/**/presentation/**/*.kt"]
synthesized: true
private: false
---

# BaseViewModel Pattern

All ViewModels in the Android app MUST extend `BaseViewModel<T : BaseUiState>` from `presentation/common/BaseViewModel.kt`. This provides consistent state management across all screens.

## Required structure

```kotlin
@HiltViewModel
class FeatureViewModel @Inject constructor(
    private val someUseCase: SomeUseCase
) : BaseViewModel<FeatureUiState>(FeatureUiState()) {

    // One-time events via Channel (navigation, snackbars, toasts)
    private val _events = Channel<FeatureEvent>(Channel.BUFFERED)
    val events: Flow<FeatureEvent> = _events.receiveAsFlow()

    fun onAction(action: FeatureAction) {
        when (action) {
            is FeatureAction.Load -> loadData()
            is FeatureAction.DismissError -> onErrorDismissed()
        }
    }

    private fun loadData() {
        updateState { it.copy(isLoading = true) }
        viewModelScope.launch {
            when (val result = someUseCase()) {
                is Resource.Success -> updateState { it.copy(isLoading = false, data = result.data) }
                is Resource.Error -> updateState { it.copy(isLoading = false, error = result.message) }
                is Resource.Loading -> { /* already set */ }
            }
        }
    }
}
```

## UiState data class requirements

Every UiState MUST implement `BaseUiState` which requires `isLoading: Boolean` and `error: String?`:

```kotlin
data class FeatureUiState(
    override val isLoading: Boolean = true,
    override val error: String? = null,
    // Screen-specific fields
    val data: List<Item> = emptyList(),
    val selectedItem: Item? = null
) : BaseUiState
```

## State update methods

| Method | Use when |
|--------|----------|
| `updateState { it.copy(...) }` | Modifying individual fields — preserves other state |
| `setState(newState)` | Replacing the entire state (rare — usually after full reload) |
| `currentState` | Reading current state inside the ViewModel |

## Error handling with Resource sealed class

The `Resource<T>` sealed class in `presentation/common/UiState.kt` has three variants: `Success`, `Error`, `Loading`. All repository/use case results MUST be wrapped in `Resource<T>`.

## MUST NOT

- MUST NOT use `LiveData` — the project uses `StateFlow` exclusively via `BaseViewModel`
- MUST NOT create multiple `StateFlow` instances for different pieces of state — use a single `UiState` data class
- MUST NOT expose `MutableStateFlow` publicly — only `StateFlow` via `uiState` property from `BaseViewModel`
- MUST NOT put navigation logic in the UiState — use `Channel` + `receiveAsFlow()` for one-time events
