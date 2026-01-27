# RasoiAI Android Architecture Decisions

## Document Version: 1.0 | January 2025

This document captures all architectural decisions for the RasoiAI Android application, based on industry best practices and project requirements.

---

## Summary of Decisions

| # | Area | Decision | Rationale |
|---|------|----------|-----------|
| 1 | Dependency Injection | Hilt | Google official, compile-time safety, Jetpack integration |
| 2 | Annotation Processing | KSP | 2x faster builds, lower memory, future-proof |
| 3 | State Management | StateFlow + UiState | Thread-safe, testable, single source of truth |
| 4 | Navigation | Navigation Compose | Official library, Hilt integration, deep linking |
| 5 | Build Configuration | Kotlin DSL + TOML | Type-safe, centralized dependencies |
| 6 | Minimum SDK | API 24 (Android 7.0) | 94% India coverage, optimal Compose performance |
| 7 | Testing Strategy | Comprehensive (70/20/10) | Production-ready, offline-first reliability |
| 8 | Modularization | By-Layer → Hybrid | Clean Architecture, MVP-friendly, scalable |

---

## 1. Dependency Injection: Hilt

### Decision
Use **Hilt** as the dependency injection framework.

### Configuration
```kotlin
// build.gradle.kts (app module)
plugins {
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

dependencies {
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
}
```

### Usage Pattern
```kotlin
// Application
@HiltAndroidApp
class RasoiAIApplication : Application()

// ViewModel injection in Compose
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) { ... }

// Repository injection
@Singleton
class MealPlanRepositoryImpl @Inject constructor(
    private val apiService: RasoiApiService,
    private val mealPlanDao: MealPlanDao
) : IMealPlanRepository
```

### Rationale
- Google's official DI solution for Android
- Compile-time error detection
- Seamless integration with ViewModel, Navigation, WorkManager
- Better runtime performance (critical for tier 2-3 city devices)

---

## 2. Annotation Processing: KSP

### Decision
Use **KSP (Kotlin Symbol Processing)** instead of KAPT.

### Configuration
```kotlin
// build.gradle.kts
plugins {
    id("com.google.devtools.ksp") version "1.9.22-1.0.17"
}

dependencies {
    // Hilt
    ksp(libs.hilt.compiler)

    // Room
    ksp(libs.room.compiler)
}
```

### Rationale
- Up to 2x faster build times than KAPT
- 30-50% less memory consumption
- Native Kotlin support (no Java stub generation)
- KAPT is in maintenance mode
- All project libraries (Hilt, Room) support KSP

---

## 3. State Management: StateFlow + Single UiState

### Decision
Use **StateFlow** with a **single UiState data class** per screen.

### Pattern
```kotlin
// UiState definition
data class HomeUiState(
    val isLoading: Boolean = false,
    val mealPlan: MealPlan? = null,
    val upcomingFestivals: List<Festival> = emptyList(),
    val currentStreak: Int = 0,
    val error: String? = null,
    val isOffline: Boolean = false
)

// ViewModel
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getMealPlanUseCase: GetCurrentMealPlanUseCase,
    private val getFestivalsUseCase: GetUpcomingFestivalsUseCase,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
        observeNetworkStatus()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val mealPlan = getMealPlanUseCase()
                val festivals = getFestivalsUseCase()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        mealPlan = mealPlan,
                        upcomingFestivals = festivals
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message)
                }
            }
        }
    }

    private fun observeNetworkStatus() {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { isOnline ->
                _uiState.update { it.copy(isOffline = !isOnline) }
            }
        }
    }

    fun onSwapMealClicked(mealItem: MealPlanItem) {
        // Handle user action
    }

    fun onErrorDismissed() {
        _uiState.update { it.copy(error = null) }
    }
}

// Composable consumption
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeContent(
        uiState = uiState,
        onSwapMeal = viewModel::onSwapMealClicked,
        onErrorDismiss = viewModel::onErrorDismissed
    )
}
```

### Rationale
- Single source of truth for screen state
- Thread-safe updates with `update{}`
- Lifecycle-aware collection prevents memory leaks
- Easy to test (single state object)
- Flow operators available (combine, map, filter)

---

## 4. Navigation: Navigation Compose

### Decision
Use **Navigation Compose** with type-safe routes.

### Configuration
```kotlin
// build.gradle.kts
dependencies {
    implementation(libs.navigation.compose)
    implementation(libs.hilt.navigation.compose)
}
```

### Navigation Structure
```kotlin
// Screen routes
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Auth : Screen("auth")
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object RecipeDetail : Screen("recipe/{recipeId}") {
        fun createRoute(recipeId: String) = "recipe/$recipeId"
    }
    object CookingMode : Screen("cooking/{recipeId}") {
        fun createRoute(recipeId: String) = "cooking/$recipeId"
    }
    object Grocery : Screen("grocery")
    object Chat : Screen("chat")
    object Pantry : Screen("pantry")
    object Stats : Screen("stats")
    object Settings : Screen("settings")
}

// NavHost setup
@Composable
fun RasoiNavHost(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToAuth = { navController.navigate(Screen.Auth.route) },
                onNavigateToHome = { navController.navigate(Screen.Home.route) }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onRecipeClick = { recipeId ->
                    navController.navigate(Screen.RecipeDetail.createRoute(recipeId))
                }
            )
        }

        composable(
            route = Screen.RecipeDetail.route,
            arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getString("recipeId") ?: return@composable
            RecipeDetailScreen(
                recipeId = recipeId,
                onStartCooking = {
                    navController.navigate(Screen.CookingMode.createRoute(recipeId))
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
```

### Deep Linking (for Push Notifications)
```kotlin
composable(
    route = Screen.RecipeDetail.route,
    deepLinks = listOf(
        navDeepLink { uriPattern = "rasoiai://recipe/{recipeId}" }
    )
) { ... }
```

### Rationale
- Official Google library with long-term support
- Seamless Hilt integration with `hiltViewModel()`
- Built-in deep linking for push notifications
- Type-safe navigation with Safe Args
- Largest community and documentation

---

## 5. Build Configuration: Kotlin DSL + Version Catalog

### Decision
Use **Kotlin DSL** for build scripts and **Version Catalog (TOML)** for dependency management.

### Version Catalog (gradle/libs.versions.toml)
```toml
[versions]
# Kotlin & Android
kotlin = "1.9.22"
agp = "8.2.2"
ksp = "1.9.22-1.0.17"

# Compose
compose-bom = "2024.02.00"
compose-compiler = "1.5.10"

# AndroidX
core-ktx = "1.12.0"
lifecycle = "2.7.0"
activity-compose = "1.8.2"
navigation-compose = "2.7.7"

# Hilt
hilt = "2.50"
hilt-navigation-compose = "1.1.0"

# Room
room = "2.6.1"

# Networking
retrofit = "2.9.0"
okhttp = "4.12.0"

# Image Loading
coil = "2.5.0"

# Firebase
firebase-bom = "32.7.2"

# DataStore
datastore = "1.0.0"

# Coroutines
coroutines = "1.7.3"

# Testing
junit = "5.10.1"
mockk = "1.13.9"
turbine = "1.0.0"

[libraries]
# Core
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "core-ktx" }
androidx-lifecycle-runtime = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activity-compose" }

# Compose
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }

# Navigation
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation-compose" }

# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hilt-navigation-compose" }

# Room
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }

# Networking
retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-gson = { group = "com.squareup.retrofit2", name = "converter-gson", version.ref = "retrofit" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
okhttp-mockwebserver = { group = "com.squareup.okhttp3", name = "mockwebserver", version.ref = "okhttp" }

# Image Loading
coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }

# Firebase
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebase-bom" }
firebase-auth = { group = "com.google.firebase", name = "firebase-auth-ktx" }
firebase-messaging = { group = "com.google.firebase", name = "firebase-messaging-ktx" }

# DataStore
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }

# Coroutines
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }

# Testing
junit5 = { group = "org.junit.jupiter", name = "junit-jupiter", version.ref = "junit" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }

[bundles]
compose = ["compose-ui", "compose-ui-graphics", "compose-ui-tooling-preview", "compose-material3"]
compose-debug = ["compose-ui-tooling", "compose-ui-test-manifest"]
room = ["room-runtime", "room-ktx"]
retrofit = ["retrofit", "retrofit-gson", "okhttp-logging"]
```

### App Module (app/build.gradle.kts)
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.rasoiai.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.rasoiai.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }
}

dependencies {
    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.bundles.compose.debug)

    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)

    // Networking
    implementation(libs.bundles.retrofit)

    // Image Loading
    implementation(libs.coil.compose)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.messaging)

    // DataStore
    implementation(libs.datastore.preferences)

    // Coroutines
    implementation(libs.coroutines.android)

    // Testing
    testImplementation(libs.junit5)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.room.testing)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.compose.ui.test.junit4)
}
```

### Rationale
- Type-safe dependency accessors with IDE autocomplete
- Centralized version management in single TOML file
- Easy dependency updates with Dependabot/Renovate
- Kotlin DSL provides compile-time error detection
- Industry standard for modern Android projects

---

## 6. Minimum SDK: API 24 (Android 7.0)

### Decision
Target **API 24** as minimum SDK.

### Configuration
```kotlin
android {
    defaultConfig {
        minSdk = 24
        targetSdk = 34
    }
}
```

### Coverage Impact
- **India Market**: ~94% of Android devices
- **Tier 2-3 Cities**: Captures most budget devices from 2017+

### Rationale
- Optimal Jetpack Compose performance
- Full Java 8 language features (lambdas, streams)
- Multi-window support
- Good balance between reach and API capabilities
- Same minimum as Now in Android reference app

---

## 7. Testing Strategy: Comprehensive (70/20/10)

### Decision
Adopt **Comprehensive testing** with 70% unit, 20% integration, 10% UI tests.

### Test Structure
```
app/
├── src/
│   ├── main/
│   ├── test/                    # Unit tests (70%)
│   │   ├── domain/usecase/
│   │   ├── data/repository/
│   │   └── presentation/viewmodel/
│   └── androidTest/             # Integration + UI tests (30%)
│       ├── data/local/          # Room tests
│       └── presentation/        # Compose UI tests
```

### Dependencies
```kotlin
// Unit Testing
testImplementation(libs.junit5)
testImplementation(libs.mockk)
testImplementation(libs.turbine)              // StateFlow testing
testImplementation(libs.coroutines.test)
testImplementation(libs.room.testing)
testImplementation(libs.okhttp.mockwebserver)

// UI Testing
androidTestImplementation(libs.compose.ui.test.junit4)
debugImplementation(libs.compose.ui.test.manifest)
```

### Example Unit Test (ViewModel)
```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: HomeViewModel
    private lateinit var getMealPlanUseCase: GetCurrentMealPlanUseCase
    private lateinit var getFestivalsUseCase: GetUpcomingFestivalsUseCase

    @BeforeEach
    fun setup() {
        getMealPlanUseCase = mockk()
        getFestivalsUseCase = mockk()
    }

    @Test
    fun `when loadData succeeds, uiState contains meal plan`() = runTest {
        // Given
        val expectedMealPlan = createTestMealPlan()
        coEvery { getMealPlanUseCase() } returns expectedMealPlan
        coEvery { getFestivalsUseCase() } returns emptyList()

        // When
        viewModel = HomeViewModel(getMealPlanUseCase, getFestivalsUseCase, mockk())

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.mealPlan).isEqualTo(expectedMealPlan)
            assertThat(state.isLoading).isFalse()
            assertThat(state.error).isNull()
        }
    }

    @Test
    fun `when loadData fails, uiState contains error`() = runTest {
        // Given
        coEvery { getMealPlanUseCase() } throws IOException("Network error")
        coEvery { getFestivalsUseCase() } returns emptyList()

        // When
        viewModel = HomeViewModel(getMealPlanUseCase, getFestivalsUseCase, mockk())

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.error).isEqualTo("Network error")
            assertThat(state.isLoading).isFalse()
        }
    }
}
```

### Example UI Test (Espresso)
```kotlin
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun homeScreen_displaysMealPlan_whenDataLoaded() {
        // Wait for loading to complete
        onView(withId(R.id.loading_indicator))
            .check(matches(not(isDisplayed())))

        // Verify meal plan is displayed
        onView(withText("Today's Meals")).check(matches(isDisplayed()))
        onView(withText("Breakfast")).check(matches(isDisplayed()))
        onView(withText("Lunch")).check(matches(isDisplayed()))
        onView(withText("Dinner")).check(matches(isDisplayed()))
    }

    @Test
    fun homeScreen_navigatesToRecipe_whenMealClicked() {
        // Click on a meal item
        onView(withId(R.id.meal_item_breakfast)).perform(click())

        // Verify navigation to recipe detail
        onView(withId(R.id.recipe_detail_screen)).check(matches(isDisplayed()))
    }
}
```

### Rationale
- Offline-first architecture requires thorough unit testing
- Complex business logic (dietary restrictions, fasting) needs coverage
- StateFlow testing with Turbine is straightforward
- Espresso UI/E2E testing verifies critical user flows
- CI/CD optimized: fast unit tests for PRs, slower UI tests nightly

---

## 8. Modularization: By-Layer (Evolving to Hybrid)

### Decision
Start with **By-Layer** modularization, evolve to **Hybrid** post-MVP.

### Phase 1: MVP Structure (By-Layer)
```
RasoiAI/
├── app/                          # Application module
│   ├── src/main/
│   │   ├── java/com/rasoiai/app/
│   │   │   ├── RasoiAIApplication.kt
│   │   │   ├── MainActivity.kt
│   │   │   ├── di/               # App-level DI modules
│   │   │   └── presentation/     # All screens & ViewModels
│   │   │       ├── navigation/
│   │   │       ├── theme/
│   │   │       ├── common/
│   │   │       ├── splash/
│   │   │       ├── auth/
│   │   │       ├── onboarding/
│   │   │       ├── home/
│   │   │       ├── recipe/
│   │   │       ├── grocery/
│   │   │       ├── chat/
│   │   │       ├── pantry/
│   │   │       ├── stats/
│   │   │       └── settings/
│   │   └── res/
│   └── build.gradle.kts
│
├── core/                         # Shared utilities & UI
│   ├── src/main/java/com/rasoiai/core/
│   │   ├── ui/
│   │   │   ├── theme/            # Colors, Typography, Theme
│   │   │   └── components/       # Shared composables
│   │   ├── util/                 # Extensions, constants
│   │   └── network/              # NetworkMonitor
│   └── build.gradle.kts
│
├── data/                         # Data layer
│   ├── src/main/java/com/rasoiai/data/
│   │   ├── di/                   # Data DI modules
│   │   ├── local/
│   │   │   ├── RasoiDatabase.kt
│   │   │   ├── dao/
│   │   │   └── entity/
│   │   ├── remote/
│   │   │   ├── api/
│   │   │   └── dto/
│   │   ├── repository/           # Repository implementations
│   │   └── sync/                 # SyncManager, OfflineQueue
│   └── build.gradle.kts
│
├── domain/                       # Domain layer
│   ├── src/main/java/com/rasoiai/domain/
│   │   ├── model/                # Domain models
│   │   ├── repository/           # Repository interfaces
│   │   └── usecase/              # Use cases
│   └── build.gradle.kts
│
├── gradle/
│   └── libs.versions.toml        # Version catalog
│
├── build.gradle.kts              # Root build file
└── settings.gradle.kts
```

### Module Dependencies (Phase 1)
```
app → core, data, domain
data → core, domain
domain → (none - pure Kotlin)
core → (none - pure Kotlin/Compose)
```

### settings.gradle.kts
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "RasoiAI"

include(":app")
include(":core")
include(":data")
include(":domain")
```

### Phase 2: Post-MVP (Hybrid)
When features grow, extract into feature modules:
```
RasoiAI/
├── app/
├── core/
│   ├── common/
│   ├── ui/
│   ├── data/
│   ├── domain/
│   └── testing/
├── feature/
│   ├── onboarding/
│   ├── home/
│   ├── recipe/
│   ├── grocery/
│   ├── chat/
│   ├── pantry/
│   ├── gamification/
│   └── settings/
└── build-logic/                  # Convention plugins
```

### Rationale
- Clean Architecture enforced from day one
- Manageable for MVP with 4 modules
- Easy to evolve to feature modules as app grows
- Faster builds than monolithic
- Clear separation of concerns

---

## Technology Stack Summary

| Layer | Technology | Version |
|-------|------------|---------|
| **Language** | Kotlin | 1.9.22 |
| **Min SDK** | Android 7.0 | API 24 |
| **Target SDK** | Android 14 | API 34 |
| **UI** | Jetpack Compose | BOM 2024.02.00 |
| **Architecture** | MVVM + Clean Architecture | - |
| **DI** | Hilt | 2.50 |
| **Navigation** | Navigation Compose | 2.7.7 |
| **Local DB** | Room | 2.6.1 |
| **Networking** | Retrofit + OkHttp | 2.9.0 / 4.12.0 |
| **Image Loading** | Coil | 2.5.0 |
| **Auth** | Firebase Auth | BOM 32.7.2 |
| **Push** | Firebase Cloud Messaging | BOM 32.7.2 |
| **Preferences** | DataStore | 1.0.0 |
| **Async** | Kotlin Coroutines + Flow | 1.7.3 |
| **Testing** | JUnit5, MockK, Turbine | 5.10.1 / 1.13.9 / 1.0.0 |
| **Build** | Gradle (Kotlin DSL) | 8.2.2 |
| **Annotation Processing** | KSP | 1.9.22-1.0.17 |

---

## References

- [Now in Android - Google Reference App](https://github.com/android/nowinandroid)
- [Android Architecture Guide](https://developer.android.com/topic/architecture)
- [Jetpack Compose Documentation](https://developer.android.com/develop/ui/compose)
- [Hilt Dependency Injection](https://developer.android.com/training/dependency-injection/hilt-android)
- [Navigation Compose](https://developer.android.com/develop/ui/compose/navigation)
- [KSP Migration Guide](https://developer.android.com/build/migrate-to-ksp)
- [Version Catalogs](https://developer.android.com/build/migrate-to-catalogs)
- [Espresso Testing](https://developer.android.com/training/testing/espresso)
- [App Modularization](https://developer.android.com/topic/modularization)

---

*Document Created: January 2025*
*Project: RasoiAI Android App*
