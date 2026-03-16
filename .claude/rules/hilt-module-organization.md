---
description: >
  Enforce the project's Hilt DI module split: data-layer modules in data/di/,
  app-layer modules in app/di/. Prevents monolithic modules and circular dependencies.
globs: ["android/**/di/**/*.kt"]
synthesized: true
private: false
---

# Hilt DI Module Organization

## Module Layout

Hilt modules are split across two locations by layer:

### `data/di/` — Data layer modules (SingletonComponent)

| Module | Provides |
|--------|----------|
| `DataModule` | Room database, all DAOs, API services, OkHttpClient |
| `NetworkModule` | Retrofit instance, base URL config |
| `RepositoryModule` | All repository implementations (binds interfaces to impls) |
| `DispatchersModule` | Coroutine dispatchers (`@IoDispatcher`, `@DefaultDispatcher`) |
| `DataStoreModule` | DataStore preferences, `UserPreferencesDataStore` |

### `app/di/` — App layer modules (SingletonComponent)

| Module | Provides |
|--------|----------|
| `AuthModule` | `PhoneAuthClient` bound to `PhoneAuthClientInterface` |
| `FirebaseModule` | Firebase Auth, Firestore, Messaging instances |

## Key Patterns

### LongTimeout Qualifier

AI-powered endpoints (meal generation, chat) call Gemini and can take 45-90 seconds. Use the `@LongTimeout` qualifier for services that hit these endpoints:

```kotlin
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LongTimeout

// In DataModule:
@Provides @LongTimeout
fun provideLongTimeoutApiService(...): RasoiApiService { ... }
```

### Repository Binding

All repositories MUST be bound via `@Binds` in `RepositoryModule`, not `@Provides`:

```kotlin
@Binds
abstract fun bindMealPlanRepository(impl: MealPlanRepositoryImpl): MealPlanRepository
```

## MUST NOT

- NEVER create a per-feature DI module (e.g., `ChatModule`, `GroceryModule`) — repositories go in `RepositoryModule`, not feature-specific modules
- NEVER provide concrete repository classes directly — always bind interface to implementation
- NEVER put data-layer bindings in `app/di/` — they belong in `data/di/`
- NEVER use `@ActivityScoped` or `@ViewModelScoped` for repositories — all repositories are `@Singleton`
