# Android Native App Development Best Practices
## Kotlin + Jetpack Compose + MVVM + Hilt Audit Guide (2024-2025)

This comprehensive checklist enables systematic evaluation of production Android codebases against **2024-2025 industry standards**. Covering architecture, Compose UI, dependency injection, data layer, testing, performance, security, and DevOps.

---

## 1. Architecture Best Practices

### 1.1 MVVM Pattern with Jetpack Compose

**Checklist:**
- [ ] ViewModel exposes `StateFlow<UiState>` (not MutableStateFlow) to Compose
- [ ] UiState modeled as sealed class/interface with Loading, Success, Error states
- [ ] One-time events (navigation, snackbars) use `Channel` or `SharedFlow`, not StateFlow
- [ ] Compose collects state with `collectAsStateWithLifecycle()` from lifecycle-runtime-compose
- [ ] All async operations use `viewModelScope` for automatic lifecycle management

**UiState sealed class pattern:**
```kotlin
sealed interface NewsUiState {
    data object Loading : NewsUiState
    data class Success(val articles: List<Article>) : NewsUiState
    data class Error(val message: String) : NewsUiState
}

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val repository: NewsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<NewsUiState>(NewsUiState.Loading)
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()
    
    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()
}
```

**Anti-patterns to flag:**
- ❌ Exposing `MutableStateFlow` directly to UI layer
- ❌ Using `collectAsState()` instead of lifecycle-aware version
- ❌ StateFlow for navigation events (replays on configuration change)
- ❌ Business logic in Composable functions
- ❌ LiveData in new Compose projects (prefer StateFlow)

### 1.2 Clean Architecture Layers

**Checklist:**
- [ ] Three-layer architecture: Presentation → Domain (optional) → Data
- [ ] UI layer contains only Composables and ViewModels
- [ ] Domain layer UseCases exist only when multiple ViewModels share business logic
- [ ] Data layer repositories implement interfaces defined in domain/data boundary
- [ ] Layer dependencies flow downward only—no circular dependencies

**When to use UseCases:**
- ✅ Multiple ViewModels need identical business logic
- ✅ Complex operations combining multiple repositories
- ✅ Logic would bloat the ViewModel significantly
- ❌ Simple passthrough to repository (skip UseCase)

### 1.3 Repository Pattern and Offline-First Architecture

**Checklist:**
- [ ] Local database serves as **single source of truth** (SSOT)
- [ ] Repository exposes `Flow<T>` for observable data, `suspend` for one-shot operations
- [ ] UI reads from local cache; network fetches update cache silently
- [ ] Sync operations scheduled via WorkManager with network constraints
- [ ] Separate model classes per layer (Entity, DTO, Domain model)

**Offline-first data flow:**
```kotlin
class OfflineFirstNewsRepository @Inject constructor(
    private val newsDao: NewsDao,
    private val newsApi: NewsApi
) : NewsRepository {
    // Always read from local database (SSOT)
    override fun getNews(): Flow<List<News>> = 
        newsDao.observeNews().map { it.map(NewsEntity::toDomain) }
    
    // Network fetch updates cache
    override suspend fun refreshNews(): Result<Unit> = runCatching {
        val networkNews = newsApi.getNews()
        newsDao.upsertAll(networkNews.map { it.toEntity() })
    }
}
```

### 1.4 StateFlow vs SharedFlow Usage

| Feature | StateFlow | SharedFlow | Channel |
|---------|-----------|------------|---------|
| Has current value | ✅ Always | ❌ No | ❌ No |
| Replay to new collectors | 1 (always) | Configurable | None |
| **Use for** | UI state | Broadcast events | One-time events |
| Conflation | ✅ Yes | Configurable | ❌ No |

**Rule:** StateFlow for state, Channel/SharedFlow for events.

### 1.5 Navigation Compose Patterns (Navigation 2.8+)

**Checklist:**
- [ ] Routes defined as `@Serializable` objects/data classes (type-safe navigation)
- [ ] Use `toRoute<T>()` for argument extraction in ViewModel or Composable
- [ ] Deep links implemented via `navDeepLink<Route>(basePath = "...")` 
- [ ] Navigation triggered from ViewModel via event channel, not directly in Composable
- [ ] Back stack management uses `popBackStack()` with correct inclusive parameter

```kotlin
@Serializable data class DetailRoute(val id: String)

NavHost(navController, startDestination = HomeRoute) {
    composable<DetailRoute>(
        deepLinks = listOf(navDeepLink<DetailRoute>(basePath = "https://app.example.com/detail"))
    ) { backStackEntry ->
        val route: DetailRoute = backStackEntry.toRoute()
        DetailScreen(articleId = route.id)
    }
}
```

### 1.6 Modularization Strategies

**Checklist:**
- [ ] Feature modules are self-contained with no inter-feature dependencies
- [ ] Core modules (`:core:data`, `:core:ui`, `:core:network`) provide shared functionality
- [ ] Module dependency graph is acyclic
- [ ] Convention plugins in `build-logic/` ensure consistent Gradle configuration
- [ ] Version catalog (`libs.versions.toml`) manages all dependencies

**Recommended structure (Now in Android pattern):**
```
project/
├── app/
├── feature/
│   ├── feature:home/
│   ├── feature:detail/
├── core/
│   ├── core:data/
│   ├── core:network/
│   ├── core:database/
│   ├── core:ui/
│   └── core:designsystem/
└── build-logic/
```

---

## 2. Jetpack Compose Best Practices

### 2.1 Composable Function Design

**Checklist:**
- [ ] **PascalCase** for UI composables returning Unit; **camelCase** for value-returning functions
- [ ] `modifier: Modifier = Modifier` as first optional parameter
- [ ] Event handlers prefixed with "on" (`onClick`, `onValueChange`)
- [ ] Required parameters before optional; trailing lambda named "content"
- [ ] Preview functions are private, placed at file bottom

```kotlin
@Composable
fun UserProfile(
    user: User,                    // Required first
    modifier: Modifier = Modifier, // Modifier as first optional
    onEditClick: () -> Unit = {},  // Events with "on" prefix
    content: @Composable () -> Unit // Trailing lambda last
)
```

### 2.2 Recomposition Optimization

**Stability requirements checklist:**
- [ ] Data classes use only `val` properties (no `var`)
- [ ] Use `kotlinx.collections.immutable` (`ImmutableList`) instead of standard `List`
- [ ] Apply `@Immutable` or `@Stable` annotations for classes from non-Compose modules
- [ ] Enable **Strong Skipping Mode** (Compose Compiler 1.5.4+)
- [ ] Generate Compose Compiler reports to identify unstable classes

**Stable data class pattern:**
```kotlin
@Immutable
data class ContactListState(
    val names: ImmutableList<String>,  // Not List<String>
    val isLoading: Boolean
)
```

### 2.3 State Hoisting and Management

**Checklist:**
- [ ] Hoist state to lowest common ancestor only
- [ ] Use `remember` for composition-scoped state
- [ ] Use `rememberSaveable` for state surviving configuration changes
- [ ] Create state holder classes for complex UI logic with multiple state fields
- [ ] ViewModel owns business logic state; plain state holders own UI-only state

### 2.4 Side Effects

| Effect | Use Case | Key Consideration |
|--------|----------|-------------------|
| `LaunchedEffect(key)` | Suspend functions (data loading) | Restarts when key changes |
| `DisposableEffect(key)` | Cleanup required (listeners) | Must include `onDispose` |
| `rememberCoroutineScope` | User-triggered coroutines | Tied to composition |
| `SideEffect` | Non-suspend sync with external systems | Runs every recomposition |
| `rememberUpdatedState` | Latest value without restarting effect | Long-running effects |

**Anti-pattern:** `LaunchedEffect(true)` or `LaunchedEffect(Unit)` without documented justification.

### 2.5 Performance Optimization

**Checklist:**
- [ ] Cache expensive calculations with `remember(key) { computation }`
- [ ] Use `derivedStateOf` when input changes more frequently than output
- [ ] **Always provide stable keys** to `LazyColumn`/`LazyRow` items
- [ ] Defer state reads with lambda-based modifiers (`offset { }`, `drawBehind { }`)
- [ ] Generate and include **Baseline Profiles** for release builds

```kotlin
// derivedStateOf: input (scroll position) changes constantly, output (boolean) rarely
val showButton by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }

// Lazy list keys
LazyColumn {
    items(items = notes, key = { it.id }) { note -> NoteRow(note) }
}
```

### 2.6 Theming and Accessibility

**Checklist:**
- [ ] Material 3 theme with light/dark color schemes and dynamic color (Android 12+)
- [ ] Typography uses M3 type scale consistently
- [ ] `contentDescription` for meaningful images; `null` for decorative
- [ ] Minimum **48dp touch targets** for interactive elements
- [ ] `semantics(mergeDescendants = true)` for related element groups

---

## 3. Dependency Injection with Hilt

### 3.1 Module Organization

**Checklist:**
- [ ] Every module has `@Module` and `@InstallIn` annotations
- [ ] Modules declared as `object` (not `class`) for Hilt optimization
- [ ] Use `@Binds` for interface implementations, `@Provides` for third-party classes
- [ ] Singleton dependencies in `SingletonComponent`, ViewModel-scoped in `ViewModelComponent`
- [ ] Inject `CoroutineDispatcher` for testability

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
}
```

### 3.2 Assisted Injection (Dagger 2.49+)

**Checklist:**
- [ ] Use `@AssistedInject` only for runtime parameters (navigation args)
- [ ] Define `@AssistedFactory` interface inside ViewModel
- [ ] Reference factory in `@HiltViewModel(assistedFactory = ...)`
- [ ] Use `hiltViewModel { factory -> factory.create(param) }` in Compose

### 3.3 Testing with Hilt

**Checklist:**
- [ ] Prefer `@TestInstallIn` over `@UninstallModules` for build performance
- [ ] Use `@BindValue` for simple test replacements
- [ ] Create custom `HiltTestRunner` extending `AndroidJUnitRunner`
- [ ] Use fakes over mocks following Now in Android pattern

---

## 4. Data Layer Best Practices

### 4.1 Room Database Patterns

**Checklist:**
- [ ] Return `Flow<T>` from DAOs for observable queries
- [ ] Use `suspend` functions for one-shot operations (insert, update, delete)
- [ ] Implement migrations (auto or manual)—**never `fallbackToDestructiveMigration()` in production**
- [ ] Add indices for frequently queried columns
- [ ] Test migrations with `MigrationTestHelper`

### 4.2 Retrofit/Networking

**Checklist:**
- [ ] Configure appropriate timeouts (connect: 10s, read: 30s, write: 30s)
- [ ] Singleton `OkHttpClient` via Hilt
- [ ] `HttpLoggingInterceptor` for debug builds only
- [ ] Implement `Authenticator` for automatic token refresh on 401
- [ ] Enable response caching with `Cache(cacheDir, sizeBytes)`

### 4.3 DataStore

**Checklist:**
- [ ] Preferences DataStore for simple key-value; Proto DataStore for typed schemas
- [ ] Use `SharedPreferencesMigration` for migration from SharedPreferences
- [ ] Handle `IOException` with `.catch { emit(emptyPreferences()) }`
- [ ] Provide DataStore as singleton via Hilt

### 4.4 Error Handling Patterns

**Checklist:**
- [ ] Sealed class hierarchy for Success/Error/Loading states
- [ ] Map technical errors to user-friendly messages in UI layer
- [ ] Implement retry with exponential backoff (1s→2s→4s→8s) and jitter
- [ ] Log detailed errors to Crashlytics/Sentry; show simplified messages to users

---

## 5. Kotlin Best Practices

### 5.1 Coroutines and Flow

**Checklist:**
- [ ] Inject `CoroutineDispatcher` (not hardcoded `Dispatchers.IO`)
- [ ] Use `viewModelScope` in ViewModels, `lifecycleScope` in UI
- [ ] Expose immutable `StateFlow`/`Flow`; keep mutable versions private
- [ ] Use `ensureActive()` in long-running loops to check cancellation
- [ ] Handle `CancellationException` specially—don't swallow it

### 5.2 Null Safety

**Checklist:**
- [ ] Avoid `!!` operator—use `?.`, `?:`, `let`, `require`, `check`
- [ ] Use `Result` pattern for operations that might fail
- [ ] Use `require()` for argument validation, `check()` for state validation
- [ ] Be cautious with `runCatching`—it catches `CancellationException`

### 5.3 Sealed Classes for State/Events

**Checklist:**
- [ ] Use sealed classes/interfaces for UI state (exhaustive `when`)
- [ ] Use `data object` for states without data (Kotlin 1.9+)
- [ ] Prefer sealed interfaces when subclasses need other hierarchies
- [ ] Keep sealed hierarchies flat—avoid deep nesting

### 5.4 KSP vs KAPT

**Checklist:**
- [ ] Migrate to KSP for all supported libraries (Hilt 2.48+, Room, Moshi)
- [ ] KSP version must match Kotlin version (e.g., `2.0.0-1.0.21` for Kotlin 2.0.0)
- [ ] Remove KAPT plugin when fully migrated (unless using Data Binding)
- [ ] Expected: **~2x faster** annotation processing

---

## 6. Testing Best Practices

### 6.1 Unit Testing ViewModels

**Checklist:**
- [ ] Use `runTest` with `StandardTestDispatcher` or `UnconfinedTestDispatcher`
- [ ] Implement `MainDispatcherRule` to replace `Dispatchers.Main`
- [ ] Use **Turbine** library for testing Flow/StateFlow emissions
- [ ] Create `SavedStateHandle` directly with test values
- [ ] Manual constructor injection—don't use Hilt for ViewModel unit tests

```kotlin
@Test
fun `loadUser updates state correctly`() = runTest {
    val viewModel = UserViewModel(FakeUserRepository())
    viewModel.uiState.test {
        assertEquals(UiState.Loading, awaitItem())
        assertEquals(UiState.Success(user), awaitItem())
    }
}
```

### 6.2 Repository Testing with Fakes

**Checklist:**
- [ ] Prefer fakes over mocks for repositories and data sources
- [ ] Store fakes in shared `testFixtures` module
- [ ] Make fakes configurable (`shouldReturnError`, helper methods)
- [ ] Test cache behavior, offline scenarios, sync strategies

### 6.3 Compose UI Testing

**Checklist:**
- [ ] Use `createComposeRule()` for isolated composable tests
- [ ] Find nodes with `onNodeWithText`, `onNodeWithTag`, `onNodeWithContentDescription`
- [ ] Use `Modifier.testTag()` for hard-to-find elements
- [ ] Consider screenshot testing with **Roborazzi** or **Paparazzi**
- [ ] Test accessibility with semantic matchers

### 6.4 Test Coverage Targets (70/20/10 Pyramid)

| Type | Target | Focus |
|------|--------|-------|
| Unit tests | **70%** | ViewModels, UseCases, Repositories, Utils |
| Integration tests | **20%** | Database, API clients, component interactions |
| E2E/UI tests | **10%** | Critical user journeys only |

**Tooling:** Use **Kover** for Kotlin coverage (unit tests) or **JaCoCo** for combined coverage.

### 6.5 MockK Patterns

**Checklist:**
- [ ] Use `coEvery`/`coVerify` for suspend functions
- [ ] Use relaxed mocks sparingly (`mockk<T>(relaxed = true)`)
- [ ] Capture arguments with `slot<T>()` for verification
- [ ] Clear mocks between tests with `clearAllMocks()`

---

## 7. Performance Optimization

### 7.1 App Startup (Target: <500ms Cold Start)

**Checklist:**
- [ ] Generate and include **Baseline Profiles** using Macrobenchmark
- [ ] Use **App Startup library** to consolidate ContentProvider initialization
- [ ] Defer non-critical SDK initialization with `lazy` delegates
- [ ] Move heavy work from `Application.onCreate()` to background threads
- [ ] Implement Splash Screen API (Android 12+)

### 7.2 Memory Management

**Checklist:**
- [ ] Integrate **LeakCanary** for debug builds
- [ ] Use `applicationContext` for singletons—never hold Activity references
- [ ] Cancel coroutines in `onDestroy()` or use lifecycle-aware scopes
- [ ] Implement `onTrimMemory()` callbacks to release caches

### 7.3 Battery Efficiency

**Checklist:**
- [ ] Use WorkManager for deferrable background tasks
- [ ] Batch network requests (3+ second bursts vs. frequent small calls)
- [ ] Replace persistent connections with FCM for messaging
- [ ] Test with Doze mode: `adb shell dumpsys deviceidle force-idle`

### 7.4 ProGuard/R8 Configuration

**Checklist:**
- [ ] Enable `minifyEnabled = true` and `shrinkResources = true` for release
- [ ] Use `proguard-android-optimize.txt` as base
- [ ] Configure proper keep rules for Retrofit interfaces, serialization models
- [ ] Preserve `mapping.txt` for each release (crash deobfuscation)
- [ ] Enable R8 full mode (AGP 8.0+): `android.enableR8.fullMode=true`

### 7.5 Compose-Specific Performance

**Checklist:**
- [ ] Always provide `key` parameter for lazy list items
- [ ] Minimize recomposition—check with Layout Inspector
- [ ] Use lambda-based modifiers for frequently changing values
- [ ] Analyze stability with Compose Compiler reports

---

## 8. Security Best Practices

### 8.1 API Key Management

**Checklist:**
- [ ] Use **Secrets Gradle Plugin**—never hardcode keys
- [ ] Store secrets in `secrets.properties` (gitignored)
- [ ] Access via `BuildConfig.API_KEY`, not string literals
- [ ] Use CI/CD secrets management (GitHub Secrets, etc.)
- [ ] Move sensitive operations to backend when possible

### 8.2 Network Security Configuration

**Checklist:**
- [ ] Create `network_security_config.xml` with `cleartextTrafficPermitted="false"`
- [ ] Trust only system CAs in release; allow user CAs in debug only
- [ ] Require TLS 1.2+ for all connections
- [ ] Consider certificate pinning for high-security apps (with backup pins)

### 8.3 Data Encryption

**Checklist:**
- [ ] Use **EncryptedSharedPreferences** for sensitive key-value data
- [ ] Use **EncryptedFile** for sensitive file storage
- [ ] Configure Android Keystore with `KeyGenParameterSpec`
- [ ] Never store tokens/passwords in plain SharedPreferences

### 8.4 Authentication Token Handling

**Checklist:**
- [ ] Store tokens in EncryptedSharedPreferences
- [ ] Implement automatic token refresh on 401 via OkHttp `Authenticator`
- [ ] Use short-lived access tokens with longer-lived refresh tokens
- [ ] Clear all auth data securely on logout
- [ ] Use PKCE for OAuth in native apps

### 8.5 Play Store Requirements

**Checklist:**
- [ ] Complete Data Safety section in Play Console
- [ ] Implement account deletion mechanism (in-app and web)
- [ ] Integrate Play Integrity API for device attestation
- [ ] Request only necessary permissions with runtime requests

---

## 9. Code Quality and DevOps

### 9.1 Linting Tools

**Checklist:**
- [ ] **ktlint** with Android rules enabled and `.editorconfig` configuration
- [ ] **detekt** with baseline for legacy code and Compose-specific rules
- [ ] **Android Lint** with `abortOnError = true` and SARIF reports
- [ ] Pre-commit hooks: `./gradlew addKtlintFormatGitPreCommitHook`
- [ ] Baseline management—periodically reduce baseline size

### 9.2 Gradle Best Practices

**Checklist:**
- [ ] Version Catalog (`libs.versions.toml`) for all dependencies
- [ ] Convention plugins in `build-logic/` for consistent module configuration
- [ ] Enable configuration cache: `org.gradle.configuration-cache=true`
- [ ] Enable build cache: `org.gradle.caching=true`
- [ ] Use BOMs (Compose BOM, Firebase BOM) for version alignment

**gradle.properties:**
```properties
org.gradle.configuration-cache=true
org.gradle.caching=true
org.gradle.parallel=true
org.gradle.jvmargs=-Xmx4g -XX:+UseParallelGC
android.nonTransitiveRClass=true
```

### 9.3 CI/CD Pipeline

**Checklist:**
- [ ] Run ktlint, detekt, and lint on every PR
- [ ] Execute unit tests with coverage reporting
- [ ] Run instrumented tests on Firebase Test Lab or emulators
- [ ] Upload SARIF reports to GitHub for code scanning
- [ ] Automate release distribution via Firebase App Distribution
- [ ] Implement staged Play Store rollouts (1%→10%→50%→100%)

### 9.4 Dependency Management

**Checklist:**
- [ ] Configure **Renovate** or **Dependabot** for automated updates
- [ ] Group related dependencies (Compose, Kotlin, Firebase)
- [ ] Enable vulnerability scanning (Snyk, Dependabot alerts)
- [ ] Automerge minor/patch updates; require review for major versions

---

## 10. Android-Specific Considerations

### 10.1 Lifecycle Awareness

**Checklist:**
- [ ] Use `lifecycleScope` for coroutines in Activities/Fragments
- [ ] Use `repeatOnLifecycle(Lifecycle.State.STARTED)` for Flow collection
- [ ] Collect with `collectAsStateWithLifecycle()` in Compose
- [ ] Register/unregister listeners in appropriate lifecycle callbacks

### 10.2 Configuration Changes and Process Death

**Checklist:**
- [ ] Use `rememberSaveable` for Compose state surviving configuration changes
- [ ] Use `SavedStateHandle` in ViewModels for process death survival
- [ ] Test with "Don't keep activities" developer option
- [ ] Verify navigation state restoration after process death

### 10.3 WorkManager for Background Tasks

**Checklist:**
- [ ] Use `CoroutineWorker` for Kotlin coroutine support
- [ ] Set network constraints: `setRequiredNetworkType(NetworkType.CONNECTED)`
- [ ] Use `enqueueUniqueWork` to prevent duplicate work
- [ ] Implement exponential backoff with `Result.retry()`
- [ ] Schedule periodic work with flex intervals

```kotlin
val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)
    .setConstraints(Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build())
    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
    .build()

WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "data_sync", ExistingPeriodicWorkPolicy.KEEP, syncRequest
)
```

### 10.4 Firebase Integration

**Checklist:**
- [ ] **Crashlytics:** Initialize in Application class, log non-fatal exceptions
- [ ] **Analytics:** Use screen_view events, custom events for key actions
- [ ] **Performance Monitoring:** Enable automatic traces, add custom traces for critical paths
- [ ] **Remote Config:** Use for feature flags with reasonable fetch intervals
- [ ] Use Firebase BOM for version alignment

---

## Quick Reference: Critical Audit Red Flags

These items represent the most severe violations to identify during codebase review:

1. **Architecture:** `MutableStateFlow` exposed to UI; business logic in Composables
2. **Compose:** Missing lazy list keys; `collectAsState()` without lifecycle awareness; `LaunchedEffect(true)` without justification
3. **Data:** Network as source of truth; `fallbackToDestructiveMigration()` in production
4. **Security:** Hardcoded API keys; plaintext token storage; cleartext HTTP traffic
5. **Performance:** No Baseline Profiles; heavy work in `Application.onCreate()`
6. **Testing:** No MainDispatcherRule; mocking data classes; `Thread.sleep()` in tests
7. **Build:** Hardcoded dependency versions; no R8/ProGuard for release; missing signing configuration

---

## Recommended Dependencies (2024-2025)

```toml
[versions]
kotlin = "2.0.0"
compose-bom = "2024.10.00"
lifecycle = "2.8.0"
navigation = "2.8.0"
hilt = "2.51"
room = "2.6.1"
ksp = "2.0.0-1.0.21"

[libraries]
androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigation" }
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-navigation-compose = { module = "androidx.hilt:hilt-navigation-compose", version = "1.2.0" }
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
kotlinx-collections-immutable = { module = "org.jetbrains.kotlinx:kotlinx-collections-immutable", version = "0.3.7" }
turbine = { module = "app.cash.turbine:turbine", version = "1.0.0" }
```

---

## How to Use This Guide for Code Review

### Systematic Review Workflow

1. **Start with Architecture** - Review ViewModel patterns first
2. **Check Compose Screens** - Look for recomposition issues
3. **Audit Data Layer** - Verify offline-first implementation
4. **Security Scan** - Check for hardcoded secrets and token handling
5. **Performance Check** - Review startup and list implementations
6. **Testing Coverage** - Identify missing test cases

### Sample Review Prompts for Claude

| Area | Prompt |
|------|--------|
| Architecture | "Review [ViewModel].kt against MVVM best practices" |
| Compose | "Check [Screen].kt for recomposition optimization issues" |
| Data Layer | "Audit [Repository].kt for offline-first compliance" |
| Testing | "What tests am I missing for [ViewModel/UseCase]?" |
| Security | "Review network and token handling for security issues" |

---

*This audit guide synthesizes current best practices from official Android documentation, Google's Now in Android reference implementation, and established community patterns.*
