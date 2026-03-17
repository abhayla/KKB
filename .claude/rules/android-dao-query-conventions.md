---
description: Room DAO query naming, suspend vs Flow, @Transaction usage, and OnConflictStrategy conventions.
globs: ["android/data/src/main/java/**/dao/*.kt"]
---

# Android DAO Query Conventions

## Method Naming

All DAO interface methods MUST follow this naming pattern:

| Prefix | Operation | Example |
|--------|-----------|---------|
| `get*` | Read single or list | `getMealPlanById(id: String)` |
| `insert*` | Create new records | `insertMealPlan(mealPlan: MealPlanEntity)` |
| `update*` | Modify existing records | `updateSyncStatus(id: String, isSynced: Boolean)` |
| `delete*` | Remove records | `deleteMealPlan(id: String)` |
| `count*` | Count queries | `countUnsynced(): Int` |
| `search*` | Full-text or filtered queries | `searchRecipes(query: String): Flow<List<RecipeEntity>>` |

## Return Types: Flow vs suspend

Choose the return type based on the query's use case:

### `Flow<T>` — Reactive queries (UI observes changes)
```kotlin
@Query("SELECT * FROM meal_plans WHERE weekStartDate <= :date AND weekEndDate >= :date LIMIT 1")
fun getMealPlanForDate(date: String): Flow<MealPlanEntity?>
```
- Use when the UI needs to react to database changes in real-time
- Room automatically re-emits when underlying data changes
- Collected in ViewModel via `collectAsStateWithLifecycle()`

### `suspend fun` — One-shot queries (fire and forget)
```kotlin
@Query("SELECT * FROM meal_plans WHERE id = :id")
suspend fun getMealPlanById(id: String): MealPlanEntity?
```
- Use for lookups, inserts, updates, deletes
- Called from coroutine scope in ViewModel or Repository

### Both patterns for the same entity
When both are needed, suffix the one-shot version with `Sync`:
```kotlin
fun getRecipeById(id: String): Flow<RecipeEntity?>       // Reactive
suspend fun getRecipeByIdSync(id: String): RecipeEntity?  // One-shot
```

## @Transaction

Use `@Transaction` when an operation touches multiple tables or requires atomicity:

```kotlin
@Transaction
suspend fun insertMealPlanWithItems(
    mealPlan: MealPlanEntity,
    items: List<MealPlanItemEntity>,
    festivals: List<MealPlanFestivalEntity> = emptyList()
) {
    insertMealPlan(mealPlan)
    insertMealPlanItems(items)
    if (festivals.isNotEmpty()) insertMealPlanFestivals(festivals)
}
```

MUST use `@Transaction` when:
- Inserting parent + child records together
- Reading parent + children (to avoid partial reads)
- Delete + re-insert (replace pattern)

## OnConflictStrategy

Use `OnConflictStrategy.REPLACE` for upsert semantics on sync operations:
```kotlin
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertRecipes(recipes: List<RecipeEntity>)
```

Use `OnConflictStrategy.IGNORE` only when duplicates are expected and harmless.

NEVER use `OnConflictStrategy.ABORT` (the default) for sync operations — it throws on conflict instead of updating.

## Section Comments

Group methods by entity type using section comments in larger DAOs:
```kotlin
// ==================== Meal Plans ====================
// ==================== Meal Plan Items ====================
```

## Anti-Patterns

- NEVER put business logic in DAOs — DAOs are data access only
- NEVER use `@RawQuery` unless absolutely necessary — prefer `@Query` with compile-time checking
- NEVER return mutable collections — Room returns immutable by default, keep it that way
- NEVER mix blocking and suspend calls — all DAO methods should be either `suspend` or return `Flow`
