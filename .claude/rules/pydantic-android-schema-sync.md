---
description: Backend Pydantic schemas and Android DTOs must stay in sync — 1:1 field mapping protocol.
globs: ["backend/app/schemas/**/*.py", "android/data/src/main/java/**/dto/*.kt", "android/data/src/main/java/**/dto/**"]
---

# Pydantic-Android Schema Sync Protocol

## Core Rule

Backend Pydantic schemas in `backend/app/schemas/` and Android DTOs in `android/data/src/main/java/com/rasoiai/data/remote/dto/` MUST maintain 1:1 field correspondence. A field added to one side MUST be added to the other in the same PR.

## Field Name Mapping

Backend uses `snake_case`, Android uses `camelCase` with `@SerializedName` annotations:

| Backend (Python) | Android (Kotlin) |
|---|---|
| `recipe_id: str` | `@SerializedName("recipe_id") val recipeId: String` |
| `prep_time_minutes: int` | `@SerializedName("prep_time_minutes") val prepTimeMinutes: Int` |
| `is_locked: bool` | `@SerializedName("is_locked") val isLocked: Boolean` |
| `dietary_tags: list[str]` | `@SerializedName("dietary_tags") val dietaryTags: List<String>` |

Fields that are single words (no underscore) don't need `@SerializedName`:
```kotlin
val id: String        // matches backend "id"
val calories: Int     // matches backend "calories"
val order: Int        // matches backend "order"
```

## Type Mapping

| Backend (Python) | Android (Kotlin) | Notes |
|---|---|---|
| `str` | `String` | |
| `int` | `Int` | |
| `float` | `Double` | |
| `bool` | `Boolean` | |
| `Optional[str]` | `String?` | Backend `= None`, Android nullable |
| `list[str]` | `List<String>` | |
| `list[NestedDto]` | `List<NestedDto>` | Nested DTOs must also match |
| `Optional[NestedDto]` | `NestedDto?` | |

## Default Values

Defaults MUST match between sides:
```python
# Backend
calories: int = 0
```
```kotlin
// Android
val calories: Int = 0
```

## Adding a New Field

When adding a field to a schema/DTO:

1. Add to the **backend Pydantic schema** in `backend/app/schemas/`
2. Add to the **Android DTO** in `android/data/src/main/java/.../dto/`
3. Add to the **Android Entity** if it needs local persistence (Room)
4. Update **EntityMappers.kt** if a new Entity field was added
5. Update **DtoMappers.kt** to map the new DTO field to domain model
6. Run backend tests: `PYTHONPATH=. pytest`
7. Run Android build: `./gradlew assembleDebug`

## Schema File Pairs

| Backend Schema | Android DTO |
|---|---|
| `schemas/meal_plan.py` | `dto/MealPlanDtos.kt` |
| `schemas/recipe.py` | `dto/RecipeDtos.kt` |
| `schemas/auth.py` | `dto/AuthDtos.kt` |
| `schemas/grocery.py` | `dto/GroceryDtos.kt` |
| `schemas/user.py` | `dto/UserDtos.kt` |

## Pydantic Config

All response schemas MUST include `model_config = ConfigDict(from_attributes=True)` to support conversion from SQLAlchemy ORM objects.

## Anti-Patterns

- NEVER add a field to only one side — it causes silent serialization failures
- NEVER rename a backend field without updating the Android `@SerializedName`
- NEVER use `Any` or `dict` in schemas — always use typed fields or nested DTOs
- NEVER use `Optional` for fields the Android app requires — mark required fields as non-nullable on both sides
