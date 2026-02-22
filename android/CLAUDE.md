# Android Module

Kotlin 1.9.22 + Jetpack Compose BOM 2024.02.00, targeting SDK 34 (min 24).

## Build Commands

```bash
# All commands run from android/
./gradlew assembleDebug                # Quick build (no tests)
./gradlew build                        # Full build with tests
./gradlew test                         # All unit tests
./gradlew :app:testDebugUnitTest --tests "*.HomeViewModelTest"  # Single test
./gradlew :app:connectedDebugAndroidTest  # All instrumented tests (needs emulator)
./gradlew lint
./gradlew installDebug

# Troubleshooting
./gradlew --stop                       # Fix Windows daemon hangs
./gradlew clean :app:kspDebugKotlin    # Fix KSP/Hilt errors
./gradlew clean && ./gradlew assembleDebug  # Fix strange build issues
```

## Module Structure

Four modules: `app`, `domain`, `data`, `core`. Dependency graph:

```
app → core, domain, data
data → core, domain
```

- `domain` has zero Android dependencies — pure Kotlin models, repository interfaces, use cases
- `core` has shared UI components and `NetworkMonitor`
- `data` has Room entities, Retrofit DTOs, repository implementations
- `app` has screens, ViewModels, Hilt modules, navigation

## Version Catalog

All dependency versions live in `gradle/libs.versions.toml`. Never hardcode versions in module `build.gradle.kts` files — use `libs.versions.*` and `libs.plugins.*` aliases.

`settings.gradle.kts` sets `repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)` — module-level `repositories {}` blocks will cause sync failures.

## Key Build Configuration

- **`WEB_CLIENT_ID`**: Must be in `local.properties` OR set as env variable (`System.getenv` fallback). Build throws `GradleException` if missing.
- **`google-services.json`**: Required in `android/app/` from Firebase Console.
- **JUnit 5** for unit tests (`useJUnitPlatform()`), **JUnit 4 rules** for instrumented tests — don't mix.
- **`animationsDisabled = true`** in test options for UI test stability.
- **Test Orchestrator is intentionally disabled** — re-enabling breaks Compose test isolation.
- **`androidx.tracing:tracing:1.2.0`** is pinned explicitly to fix `NoSuchMethodError: forceEnableAppTracing` — do not remove.
- **`compose-stability.conf`** at `android/app/compose-stability.conf` controls Compose recomposition — changes impact performance.
- **`applicationIdSuffix`** is commented out for debug builds to match Firebase config — do not re-add.
- **Custom test runner**: `com.rasoiai.app.HiltTestRunner` (not the default).
- **Emulator**: Use API 34 locally. API 36 has Espresso compatibility issues. CI uses API 29.
