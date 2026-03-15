---
description: >
  Hilt DI modules MUST use @Binds for repository bindings (not @Provides) and live in specific
  locations by layer. Misplaced or mis-typed DI modules cause runtime crashes with opaque Hilt errors.
globs: ["android/**/di/**/*.kt", "android/**/di/*.kt"]
synthesized: true
private: false
---

# Hilt DI Module Conventions

Hilt DI modules follow strict placement and binding rules. Violations cause runtime crashes with cryptic Hilt/Dagger error messages.

## Module placement by layer

| Module | Location | Scope | What it binds |
|--------|----------|-------|--------------|
| `RepositoryModule` | `android/data/src/main/java/com/rasoiai/data/di/` | `@SingletonComponent` | Repository interface → implementation (12 repositories) |
| `NetworkModule` | `android/data/src/main/java/com/rasoiai/data/di/` | `@SingletonComponent` | Retrofit, OkHttp, API service instances |
| `DataStoreModule` | `android/data/src/main/java/com/rasoiai/data/di/` | `@SingletonComponent` | DataStore preferences |
| `DispatchersModule` | `android/data/src/main/java/com/rasoiai/data/di/` | `@SingletonComponent` | Coroutine dispatchers |
| `AuthModule` | `android/app/src/main/java/com/rasoiai/app/di/` | `@SingletonComponent` | Firebase Auth, PhoneAuthClient |
| `FirebaseModule` | `android/app/src/main/java/com/rasoiai/app/di/` | `@SingletonComponent` | Firebase instances |

## @Binds vs @Provides

Repository bindings MUST use `@Binds` on an `abstract` class — NOT `@Provides`:

```kotlin
// CORRECT — @Binds is optimized by Hilt, no allocation
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindMealPlanRepository(impl: MealPlanRepositoryImpl): MealPlanRepository
}

// WRONG — @Provides allocates a wrapper, less efficient for simple bindings
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides @Singleton
    fun provideMealPlanRepository(impl: MealPlanRepositoryImpl): MealPlanRepository = impl
}
```

Use `@Provides` only when construction requires logic (e.g., building Retrofit instance with interceptors).

## Adding a new repository

When adding a new repository, update these in order:

1. Define interface in `domain/repository/` (e.g., `NewFeatureRepository`)
2. Implement in `data/repository/` (e.g., `NewFeatureRepositoryImpl`) with `@Inject constructor`
3. Add `@Binds` entry in `data/di/RepositoryModule.kt`
4. Inject the interface (not implementation) in ViewModels/UseCases

## E2E test DI overrides

E2E tests override specific DI modules via Hilt test rules:

| Test module | Location | What it replaces |
|------------|----------|-----------------|
| `FakeAuthModule` | `e2e/di/FakeAuthModule.kt` | Replaces real `PhoneAuthClient` with `FakePhoneAuthClient` |
| `FakeNetworkModule` | `e2e/di/FakeNetworkModule.kt` | Points Retrofit to `http://10.0.2.2:8000` |

Test DI modules use `@TestInstallIn(components = [SingletonComponent::class], replaces = [AuthModule::class])`.

## MUST NOT

- MUST NOT place data-layer DI modules in the `app` module — they belong in `data/di/`
- MUST NOT use `@Provides` for simple interface-to-implementation bindings — use `@Binds`
- MUST NOT inject concrete implementations (`MealPlanRepositoryImpl`) in ViewModels — always inject the interface (`MealPlanRepository`)
- MUST NOT skip `@Singleton` scope on repository bindings — stateful repositories (caches, sessions) break if instantiated multiple times
