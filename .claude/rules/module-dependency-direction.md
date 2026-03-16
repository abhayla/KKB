---
description: >
  Enforce the 4-module dependency direction: app → domain ← data → core.
  Domain MUST NOT depend on data or app. Violations cause circular dependencies and break clean architecture.
globs: ["android/app/src/main/java/**/*.kt"]
synthesized: true
private: false
---

# 4-Module Dependency Direction

The Android project follows a strict 4-module clean architecture. Dependencies flow in one direction — violations cause circular imports and break the build.

## Module structure

| Module | Package | Purpose | May depend on |
|--------|---------|---------|--------------|
| `app` | `com.rasoiai.app.*` | Screens, ViewModels, Hilt modules, navigation | `domain`, `data`, `core` |
| `domain` | `com.rasoiai.domain.*` | Models, repository interfaces, use cases | **Nothing** (pure Kotlin) |
| `data` | `com.rasoiai.data.*` | Room DAOs, Retrofit APIs, repository implementations | `domain`, `core` |
| `core` | `com.rasoiai.core.*` | Shared UI components, utilities, NetworkMonitor | Android framework only |

## Dependency graph

```
app ─────┬──────> core
         ├──────> domain  <────┐
         └──────> data ────────┴──────> core
```

## What lives where

| Artifact | Module | Example |
|----------|--------|---------|
| Domain models (data classes) | `domain` | `MealPlan`, `Recipe`, `User` |
| Repository interfaces | `domain` | `MealPlanRepository`, `RecipeRepository` |
| Use cases | `domain` | `GetMealPlanUseCase` |
| Room entities | `data` | `MealPlanEntity`, `RecipeEntity` |
| Room DAOs | `data` | `MealPlanDao` |
| Retrofit API interfaces | `data` | `MealPlanApi` |
| Repository implementations | `data` | `MealPlanRepositoryImpl` |
| Entity ↔ Domain mappers | `data` | `EntityMappers.kt` |
| DTO → Domain mappers | `data` | `DtoMappers.kt` |
| ViewModels | `app` | `HomeViewModel` |
| Composable screens | `app` | `HomeScreen` |
| Hilt DI modules | `app` | `RepositoryModule`, `NetworkModule` |
| NetworkMonitor | `core` | Shared utility for online/offline detection |

## MUST NOT

- MUST NOT import from `com.rasoiai.data.*` or `com.rasoiai.app.*` in the `domain` module — domain is pure Kotlin with zero Android/framework dependencies
- MUST NOT put Room annotations (`@Entity`, `@Dao`) in the `domain` module — database details belong in `data`
- MUST NOT put Retrofit annotations (`@GET`, `@POST`) in the `domain` module — network details belong in `data`
- MUST NOT define repository implementations in `domain` — domain defines interfaces, `data` implements them
- MUST NOT use Domain Models directly in Room queries — map via `EntityMappers.kt` in the `data` module
