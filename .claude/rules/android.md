---
paths:
  - "android/**/*.kt"
  - "android/**/*.kts"
  - "android/**/*.xml"
---

# Android Rules

## Build & Run
- All Gradle commands run from `android/` directory using `./gradlew` (Unix syntax, not `.\gradlew`)
- Never hardcode dependency versions in module `build.gradle.kts` — use `libs.versions.*` from `gradle/libs.versions.toml`
- Module-level `repositories {}` blocks are forbidden (`FAIL_ON_PROJECT_REPOS`) — add repos in `settings.gradle.kts` only

## Architecture
- ViewModels extend `BaseViewModel<T : BaseUiState>` — use `updateState {}` not raw `_uiState.update {}`
- Navigation routes: define in `Screen.kt` only, use `createRoute()` helpers, never construct route strings manually
- All features must be offline-first: Room is source of truth, sync to backend when online
- Use `NetworkMonitor.isOnline` to check connectivity before API calls

## Compose
- Use `TestTags` constants from `presentation/common/TestTags.kt` for all `testTag()` modifiers — UI tests break if tags are missing
- `RasoiBottomNavigation` lives in `home/components/`, not `common/`
- Adding a bottom-nav screen requires updating both `Screen.kt` AND the `NavigationItem` enum

## Testing
- Unit tests use JUnit 5 (`useJUnitPlatform()`), instrumented tests use JUnit 4 rules — don't mix
- Custom test runner: `com.rasoiai.app.HiltTestRunner`
- Emulator: API 34 locally (API 36 has Espresso issues, CI uses API 29)
- `animationsDisabled = true` is set intentionally for test stability

## Do Not Remove
- `androidx.tracing:tracing:1.2.0` pin — fixes `NoSuchMethodError: forceEnableAppTracing`
- `applicationIdSuffix` comment-out — matches Firebase config
- Test Orchestrator disabled state — re-enabling breaks Compose isolation
