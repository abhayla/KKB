---
description: Android naming conventions for screens, ViewModels, UiState, repositories, DAOs, entities, DTOs, and Hilt modules.
globs: ["android/**/*.kt"]
---

# Android Naming Conventions

## File and Class Naming

| Type | Pattern | Example |
|------|---------|---------|
| Screen composable | `[Feature]Screen.kt` | `HomeScreen.kt`, `GroceryScreen.kt` |
| ViewModel | `[Feature]ViewModel.kt` | `HomeViewModel.kt`, `GroceryViewModel.kt` |
| UiState | `[Feature]UiState` (inside ViewModel or separate) | `HomeUiState`, `GroceryUiState` |
| Navigation event | `[Feature]NavigationEvent` (sealed class) | `HomeNavigationEvent` |
| Repository interface | `[Feature]Repository.kt` (in `domain/repository/`) | `GroceryRepository`, `MealPlanRepository` |
| Repository impl | `[Feature]RepositoryImpl.kt` (in `data/repository/`) | `GroceryRepositoryImpl` |
| Room DAO | `[Feature]Dao.kt` | `MealPlanDao`, `GroceryDao` |
| Room entity | `[Feature]Entity.kt` | `MealPlanEntity`, `GroceryItemEntity` |
| API DTO | `[Feature]Response` / `[Feature]Request` / `[Feature]Dto` | `MealPlanResponse`, `IngredientDto` |
| Hilt module | `[Domain]Module.kt` | `AuthModule`, `DataModule`, `RepositoryModule` |
| Mapper function | `fun [Source].to[Target]()` | `fun DtoEntity.toDomain(): DomainModel` |
| UI component | `[Feature][ComponentType].kt` | `AddRecipeSheet.kt`, `RasoiBottomNavigation.kt` |
| Test class | `[Subject]Test.kt` | `HomeViewModelTest`, `GroceryRepositoryImplTest` |

## Package Structure

Packages follow `com.rasoiai.[module].[layer].[feature]`:

```
com.rasoiai.app.presentation.home          # Screen + ViewModel
com.rasoiai.app.presentation.home.components  # Sub-composables
com.rasoiai.app.di                          # App-level Hilt modules
com.rasoiai.data.repository                 # Repository implementations
com.rasoiai.data.local.dao                  # Room DAOs
com.rasoiai.data.local.entity               # Room entities
com.rasoiai.data.local.mapper               # Entity ↔ Domain mappers
com.rasoiai.data.remote.api                 # Retrofit service interfaces
com.rasoiai.data.remote.dto                 # API response/request DTOs
com.rasoiai.data.remote.mapper              # DTO ↔ Entity mappers
com.rasoiai.data.di                         # Data-layer Hilt modules
com.rasoiai.domain.model                    # Domain models
com.rasoiai.domain.repository               # Repository interfaces
com.rasoiai.core.network                    # NetworkMonitor
```

## Composable Function Conventions

Screen-level composables MUST:
- Accept navigation callbacks as `onNavigateTo[Destination]: () -> Unit` parameters
- Inject ViewModel via `hiltViewModel()`
- Collect state via `val uiState by viewModel.uiState.collectAsStateWithLifecycle()`
- Collect events via `LaunchedEffect` with `viewModel.navigationEvent.collect { }`
- Use `Scaffold` with `TopAppBar` as the top-level layout
- Apply `testTag(TestTags.TAG_NAME)` on key interactive elements for E2E tests

## Hilt Naming

- `@Binds` for interface→impl bindings in abstract modules (preferred over `@Provides` for simple bindings)
- `@Provides` only when construction logic is needed (database instances, OkHttpClient, Retrofit)
- Separate modules by domain: `RepositoryModule`, `DataModule`, `AuthModule`, `FirebaseModule`
- All modules use `@InstallIn(SingletonComponent::class)` unless lifecycle scoping is needed

## Anti-Patterns

- NEVER use `Impl` suffix on anything except repository implementations — ViewModels, services, and use cases get their own names
- NEVER create feature-specific mapper files — use centralized `EntityMappers.kt` and `DtoMappers.kt`
- NEVER put composable previews in separate files — `@Preview` functions belong in the same file as the composable
- NEVER use `@Provides` for simple interface bindings — use `@Binds` on abstract classes
