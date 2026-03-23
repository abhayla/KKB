package com.rasoiai.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests Room migration from version 8 to 9.
 *
 * Migration 8->9 performs three changes:
 * - Recreates recipe_rules table renaming mealSlot -> mealSlots, adding syncStatus column
 * - Adds syncStatus column to nutrition_goals
 * - Creates new cooked_recipes table with indexes on cuisineType and cookedDate
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest_8_9 {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        RasoiDatabase::class.java
    )

    /**
     * Inserts recipe_rules data at v8 (with mealSlot column), runs migration,
     * and verifies all data is preserved in the new schema (mealSlots column).
     */
    @Test
    fun migration_8_9_preservesRecipeRulesData() {
        // Create DB at version 8 and insert recipe_rules data
        val dbV8 = helper.createDatabase(TEST_DB, 8)
        val ruleValues = ContentValues().apply {
            put("id", "rule-001")
            put("type", "INGREDIENT")
            put("action", "INCLUDE")
            put("targetId", "paneer-001")
            put("targetName", "Paneer")
            put("frequencyType", "WEEKLY")
            put("frequencyCount", 3)
            put("frequencyDays", "[\"MONDAY\",\"WEDNESDAY\",\"FRIDAY\"]")
            put("enforcement", "STRICT")
            put("mealSlot", "DINNER")
            put("isActive", 1)
            put("createdAt", "2026-03-01T10:00:00Z")
            put("updatedAt", "2026-03-01T10:00:00Z")
        }
        dbV8.insert("recipe_rules", SQLiteDatabase.CONFLICT_FAIL, ruleValues)

        // Insert a second rule with null mealSlot
        val ruleValues2 = ContentValues().apply {
            put("id", "rule-002")
            put("type", "RECIPE")
            put("action", "EXCLUDE")
            put("targetId", "recipe-egg-001")
            put("targetName", "Egg Curry")
            put("frequencyType", "ALWAYS")
            putNull("frequencyCount")
            putNull("frequencyDays")
            put("enforcement", "STRICT")
            putNull("mealSlot")
            put("isActive", 1)
            put("createdAt", "2026-03-02T08:00:00Z")
            put("updatedAt", "2026-03-02T08:00:00Z")
        }
        dbV8.insert("recipe_rules", SQLiteDatabase.CONFLICT_FAIL, ruleValues2)
        dbV8.close()

        // Run migration 8->9
        val db = helper.runMigrationsAndValidate(
            TEST_DB, 9, true, RasoiDatabase.MIGRATION_8_9
        )

        // Verify first rule data preserved, mealSlot value now in mealSlots column
        val cursor = db.query("SELECT * FROM recipe_rules WHERE id = 'rule-001'")
        assertTrue("Rule-001 should exist after migration", cursor.moveToFirst())
        assertEquals("INGREDIENT", cursor.getString(cursor.getColumnIndex("type")))
        assertEquals("INCLUDE", cursor.getString(cursor.getColumnIndex("action")))
        assertEquals("Paneer", cursor.getString(cursor.getColumnIndex("targetName")))
        assertEquals("WEEKLY", cursor.getString(cursor.getColumnIndex("frequencyType")))
        assertEquals(3, cursor.getInt(cursor.getColumnIndex("frequencyCount")))
        assertEquals("DINNER", cursor.getString(cursor.getColumnIndex("mealSlots")))
        assertEquals("SYNCED", cursor.getString(cursor.getColumnIndex("syncStatus")))
        assertEquals(1, cursor.getInt(cursor.getColumnIndex("isActive")))
        cursor.close()

        // Verify second rule with null mealSlot migrated correctly
        val cursor2 = db.query("SELECT * FROM recipe_rules WHERE id = 'rule-002'")
        assertTrue("Rule-002 should exist after migration", cursor2.moveToFirst())
        assertEquals("EXCLUDE", cursor2.getString(cursor2.getColumnIndex("action")))
        assertTrue("mealSlots should be null", cursor2.isNull(cursor2.getColumnIndex("mealSlots")))
        assertEquals("SYNCED", cursor2.getString(cursor2.getColumnIndex("syncStatus")))
        cursor2.close()

        db.close()
    }

    /**
     * Verifies the column rename from mealSlot to mealSlots happened correctly.
     * The old column name should not exist; the new one should.
     */
    @Test
    fun migration_8_9_renamesMealSlotColumn() {
        helper.createDatabase(TEST_DB, 8).close()

        val db = helper.runMigrationsAndValidate(
            TEST_DB, 9, true, RasoiDatabase.MIGRATION_8_9
        )

        // Query PRAGMA to get column info for recipe_rules
        val cursor = db.query("PRAGMA table_info(recipe_rules)")
        val columnNames = mutableListOf<String>()
        while (cursor.moveToNext()) {
            columnNames.add(cursor.getString(cursor.getColumnIndex("name")))
        }
        cursor.close()

        assertTrue("mealSlots column should exist", columnNames.contains("mealSlots"))
        assertFalse("Old mealSlot column should not exist", columnNames.contains("mealSlot"))
        assertTrue("syncStatus column should exist", columnNames.contains("syncStatus"))

        db.close()
    }

    /**
     * Verifies the cooked_recipes table is created with correct schema and indexes.
     */
    @Test
    fun migration_8_9_createsCookedRecipesTable() {
        helper.createDatabase(TEST_DB, 8).close()

        val db = helper.runMigrationsAndValidate(
            TEST_DB, 9, true, RasoiDatabase.MIGRATION_8_9
        )

        // Verify table exists by inserting a row
        val values = ContentValues().apply {
            put("id", "cooked-001")
            put("recipeId", "recipe-paneer-001")
            put("recipeName", "Paneer Butter Masala")
            put("cuisineType", "North Indian")
            put("cookedDate", "2026-03-15")
            put("createdAt", System.currentTimeMillis())
        }
        db.insert("cooked_recipes", SQLiteDatabase.CONFLICT_FAIL, values)

        // Verify the row
        val cursor = db.query("SELECT * FROM cooked_recipes WHERE id = 'cooked-001'")
        assertTrue("Cooked recipe row should exist", cursor.moveToFirst())
        assertEquals("Paneer Butter Masala", cursor.getString(cursor.getColumnIndex("recipeName")))
        assertEquals("North Indian", cursor.getString(cursor.getColumnIndex("cuisineType")))
        cursor.close()

        // Verify indexes exist
        val cuisineIndex = db.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND name='index_cooked_recipes_cuisineType'"
        )
        assertTrue("Index on cuisineType should exist", cuisineIndex.moveToFirst())
        cuisineIndex.close()

        val dateIndex = db.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND name='index_cooked_recipes_cookedDate'"
        )
        assertTrue("Index on cookedDate should exist", dateIndex.moveToFirst())
        dateIndex.close()

        db.close()
    }

    companion object {
        private const val TEST_DB = "migration-test-8-9"
    }
}
