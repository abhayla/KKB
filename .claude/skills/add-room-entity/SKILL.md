---
name: add-room-entity
description: >
  Add a new Room entity to the Android data layer with DAO, database registration,
  migration, and centralized mapper extensions. Ensures all 6+ locations are updated.
type: workflow
allowed-tools: "Bash Read Write Edit Grep Glob"
argument-hint: "<EntityName> <table_name>"
version: "1.0.0"
synthesized: true
private: false
---

# Add Room Entity

Add a new Room entity with all required registrations across the data layer.

**Arguments:** $ARGUMENTS — e.g., `ShoppingList shopping_lists`

## STEP 1: Create the Entity

Create `android/data/src/main/java/com/rasoiai/data/local/entity/<EntityName>Entity.kt`:

```kotlin
@Entity(tableName = "<table_name>")
data class <EntityName>Entity(
    @PrimaryKey val id: String,
    // Columns here
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
```

Use `@ColumnInfo(name = "snake_case")` for column names that differ from the Kotlin property name.

## STEP 2: Create the DAO

Create `android/data/src/main/java/com/rasoiai/data/local/dao/<EntityName>Dao.kt`:

```kotlin
@Dao
interface <EntityName>Dao {
    @Query("SELECT * FROM <table_name> WHERE id = :id")
    suspend fun getById(id: String): <EntityName>Entity?

    @Query("SELECT * FROM <table_name>")
    fun getAll(): Flow<List<<EntityName>Entity>>

    @Upsert
    suspend fun upsert(entity: <EntityName>Entity)

    @Query("DELETE FROM <table_name> WHERE id = :id")
    suspend fun deleteById(id: String)
}
```

Use `@Upsert` instead of `@Insert(onConflict = REPLACE)` + `@Update` — `@Upsert` is atomic (Room 2.5+).

## STEP 3: Register in RasoiDatabase

Edit `android/data/src/main/java/com/rasoiai/data/local/RasoiDatabase.kt`:

1. Add entity to `@Database(entities = [...])` annotation
2. Add abstract DAO accessor: `abstract fun <entityName>Dao(): <EntityName>Dao`
3. Increment the database version number

## STEP 4: Create Migration

Add migration in `RasoiDatabase.kt`:

```kotlin
val MIGRATION_<N>_<N+1> = object : Migration(<N>, <N+1>) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `<table_name>` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL
            )
        """)
    }
}
```

Add the migration to the database builder's `.addMigrations(...)` call.

## STEP 5: Add Mapper Extensions

Add to `android/data/src/main/java/com/rasoiai/data/local/mapper/EntityMappers.kt`:

```kotlin
fun <EntityName>Entity.toDomain(): <DomainModel> = <DomainModel>(
    id = id,
    // map fields
)

fun <DomainModel>.toEntity(): <EntityName>Entity = <EntityName>Entity(
    id = id,
    // map fields
)
```

If there's a corresponding API DTO, also add to `DtoMappers.kt`:
```kotlin
fun <DtoName>.toEntity(): <EntityName>Entity = <EntityName>Entity(...)
```

## STEP 6: Register DAO in DataModule

Add to `android/data/src/main/java/com/rasoiai/data/di/DataModule.kt`:

```kotlin
@Provides
fun provide<EntityName>Dao(database: RasoiDatabase): <EntityName>Dao =
    database.<entityName>Dao()
```

## STEP 7: Export Room Schema

```bash
cd android
./gradlew :data:kspDebugKotlin
```

Verify a new schema JSON is generated in `android/data/schemas/com.rasoiai.data.local.RasoiDatabase/<version>.json`.

## CRITICAL RULES

- NEVER skip the migration — Room crashes at runtime with `IllegalStateException` if version changes without a migration
- NEVER use `fallbackToDestructiveMigration()` — it deletes all user data
- MUST use `@Upsert` over `@Insert(onConflict = REPLACE)` — the separate pair has a race window
- MUST add mapper extensions to the CENTRALIZED `EntityMappers.kt` file — never put mapping logic in the entity class
- MUST export the Room schema after adding the entity — CI validates schema files
