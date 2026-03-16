---
description: >
  Android repositories MUST follow the offline-first pattern: Room is source of truth,
  API syncs in the background. Never read directly from network for display data.
globs: ["android/app/src/main/java/**/data/**/*.kt", "android/app/src/main/java/**/domain/repository/**/*.kt"]
synthesized: true
private: false
---

# Offline-First Repository Pattern

All Android data repositories MUST follow the offline-first pattern where Room is the source of truth and Retrofit handles background sync.

## Data flow

```
UI → ViewModel → UseCase → Repository
                              ↓
                 ┌────────────┴────────────┐
                 ↓                         ↓
            Room (Local)            Retrofit (Remote)
            Source of Truth         Background Sync
                 ↓                         ↓
            EntityMappers              DtoMappers
                 └──────────┬──────────────┘
                            ↓
                      Domain Models
```

## Repository implementation pattern

```kotlin
class MealPlanRepositoryImpl @Inject constructor(
    private val localDataSource: MealPlanDao,
    private val remoteDataSource: MealPlanApi,
    private val networkMonitor: NetworkMonitor
) : MealPlanRepository {

    // READ: Always from Room
    override fun getMealPlans(): Flow<List<MealPlan>> =
        localDataSource.getAllMealPlans().map { entities ->
            entities.map { it.toDomain() }
        }

    // WRITE: Local first, then sync
    override suspend fun saveMealPlan(plan: MealPlan): Result<Unit> {
        // 1. Save to Room immediately
        localDataSource.insert(plan.toEntity())

        // 2. Sync to server if online
        if (networkMonitor.isOnline()) {
            try {
                remoteDataSource.createMealPlan(plan.toDto())
            } catch (e: Exception) {
                // Mark as unsynced — will retry later
                localDataSource.markUnsynced(plan.id)
            }
        }
        return Result.success(Unit)
    }
}
```

## Mapper conventions

| Mapper file | Direction | Location |
|------------|-----------|---------|
| `data/local/mapper/EntityMappers.kt` | Room Entity <-> Domain Model | Extension functions on Entity classes |
| `data/remote/mapper/DtoMappers.kt` | API DTO -> Domain Model and Entity | Extension functions on DTO classes |

## Module dependency direction

```
app → domain ← data → core
```

- `domain` defines repository interfaces — `data` implements them
- `domain` MUST NOT depend on `data` — only interfaces, no Room/Retrofit imports
- `app` depends on everything for DI wiring via Hilt modules

## MUST NOT

- MUST NOT read directly from Retrofit for display data — always read from Room and sync in background
- MUST NOT put Room or Retrofit imports in the `domain` module — domain defines interfaces only
- MUST NOT skip the local save on write operations — even if online, write to Room first for consistency
- MUST NOT use raw Entity or DTO objects in ViewModels — always map to Domain Models via the mapper files
