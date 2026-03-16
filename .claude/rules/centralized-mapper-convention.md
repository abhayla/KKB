---
description: >
  Enforce centralized data mapping through two dedicated mapper files.
  Prevents scattered mapping logic and ensures consistency across layers.
globs: ["android/data/src/main/java/**/mapper/**", "android/data/src/main/java/**/entity/**", "android/data/src/main/java/**/dto/**"]
synthesized: true
private: false
---

# Centralized Mapper Convention

## The Rule

All data transformations between layers MUST go through exactly two mapper files:

| File | Direction | Location |
|------|-----------|----------|
| `EntityMappers.kt` | Room Entity <-> Domain Model | `data/local/mapper/` |
| `DtoMappers.kt` | API DTO -> Domain Model & Entity | `data/remote/mapper/` |

## Mapper Style

All mappers are implemented as **extension functions**, not static methods or mapper classes:

```kotlin
// CORRECT: extension function in EntityMappers.kt
fun MealPlanEntity.toDomain(): MealPlan { ... }
fun MealPlan.toEntity(): MealPlanEntity { ... }

// CORRECT: extension function in DtoMappers.kt
fun MealPlanResponse.toDomain(): MealPlan { ... }
fun MealPlanResponse.toEntity(): MealPlanEntity { ... }

// WRONG: mapper class
class MealPlanMapper {
    fun toDomain(entity: MealPlanEntity): MealPlan { ... }
}

// WRONG: mapper in the entity/model file itself
data class MealPlanEntity(...) {
    fun toDomain(): MealPlan { ... }  // NO — goes in EntityMappers.kt
}
```

## MUST DO

- When adding a new entity or DTO, add ALL relevant mappers to the centralized file
- Include both directions (toDomain + toEntity) for entities, even if only one is used now
- Handle nullable fields explicitly — never rely on implicit defaults during mapping
- Use `Gson` for JSON list fields stored as strings in Room (e.g., `ingredients`, `instructions`)

## MUST NOT

- NEVER put mapping logic inside entity classes, DTO classes, or domain models
- NEVER create per-feature mapper files (e.g., `MealPlanMapper.kt`) — all mappers stay centralized
- NEVER use mappers in the `domain` module — it has no Android dependencies and cannot reference entities or DTOs
