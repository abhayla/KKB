---
paths:
  - "android/app/src/main/java/com/rasoiai/app/presentation/**/*.kt"
---

# Compose UI Rules

## Screen Structure
Every screen directory follows: `FeatureScreen.kt` + `FeatureViewModel.kt` + optional `components/` subdirectory.

## ViewModel Pattern
```kotlin
@HiltViewModel
class FeatureViewModel @Inject constructor(
    private val repository: Repository
) : BaseViewModel<FeatureUiState>(FeatureUiState()) {
    // Use updateState {} — provided by BaseViewModel
    // Use Channel<NavigationEvent> for one-time navigation
}
```

## State Management
- Single `UiState` data class per screen implementing `BaseUiState`
- Derived state via `get()` computed properties, not separate StateFlows
- `Resource<T>` sealed class (`Success`, `Error`, `Loading`) in `common/UiState.kt`

## Bottom Navigation
- Screens with bottom nav use `RasoiBottomNavigation(selectedItem, onItemSelected)`
- Items: HOME, GROCERY, CHAT, FAVORITES, STATS
- Adding new items: update `NavigationItem` enum AND `Screen.kt` `bottomNavScreens`

## Test Tags
- Every interactive/assertable element needs `Modifier.testTag(TestTags.CONSTANT)`
- Tags defined in `presentation/common/TestTags.kt` — never use raw strings

## Design System

| Element | Light | Dark |
|---------|-------|------|
| Primary | `#FF6838` (Orange) | `#FFB59C` |
| Secondary | `#5A822B` (Green) | `#A8D475` |
| Background | `#FDFAF4` (Cream) | `#1C1B1F` |

| Token | Value |
|-------|-------|
| Spacing | 8dp grid (4, 8, 16, 24, 32, 48dp) |
| Shapes | Rounded corners (8dp small, 16dp medium, 24dp large) |

## Reference Implementations

| Pattern | Reference | Key Features |
|---------|-----------|--------------|
| Tabs + Bottom Sheets | `presentation/reciperules/` | 2-tab layout (Rules, Nutrition), modal sheets |
| Form-based Settings | `presentation/settings/` | Sections, toggles |
| Bottom Navigation | `presentation/home/` | RasoiBottomNavigation |
| List with Filtering | `presentation/favorites/` | Tab/list pattern |
