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
 * Tests Room migration from version 10 to 11.
 *
 * Migration 10->11 drops and recreates meal_plan_items with a new primary key:
 * - Old PK: composite (mealPlanId, date, mealType, recipeId)
 * - New PK: single column (id) — backend-provided UUID
 * Data loss is acceptable because meal_plan_items is a cache re-fetched from backend.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest_10_11 {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        RasoiDatabase::class.java
    )

    /**
     * Verifies the meal_plan_items table exists after migration and accepts
     * rows with the new schema (id as TEXT PRIMARY KEY).
     */
    @Test
    fun migration_10_11_recreatesMealPlanItems() {
        // Create DB at v10 with a meal_plan parent row
        val dbV10 = helper.createDatabase(TEST_DB, 10)
        insertMealPlanAt(dbV10, "plan-001")
        dbV10.close()

        // Run migration 10->11
        val db = helper.runMigrationsAndValidate(
            TEST_DB, 11, true, RasoiDatabase.MIGRATION_10_11
        )

        // Insert a meal_plan_item with the new schema (id column as PK)
        val itemValues = ContentValues().apply {
            put("id", "item-uuid-001")
            put("mealPlanId", "plan-001")
            put("date", "2026-03-17")
            put("dayName", "Monday")
            put("mealType", "BREAKFAST")
            put("recipeId", "recipe-poha-001")
            put("recipeName", "Poha")
            putNull("recipeImageUrl")
            put("prepTimeMinutes", 15)
            put("calories", 250)
            put("dietaryTags", "[\"VEG\"]")
            put("isLocked", 0)
            put("order", 0)
        }
        db.insert("meal_plan_items", SQLiteDatabase.CONFLICT_FAIL, itemValues)

        // Verify the row was inserted
        val cursor = db.query("SELECT * FROM meal_plan_items WHERE id = 'item-uuid-001'")
        assertTrue("Meal plan item should exist", cursor.moveToFirst())
        assertEquals("plan-001", cursor.getString(cursor.getColumnIndex("mealPlanId")))
        assertEquals("Poha", cursor.getString(cursor.getColumnIndex("recipeName")))
        assertEquals("BREAKFAST", cursor.getString(cursor.getColumnIndex("mealType")))
        assertEquals(15, cursor.getInt(cursor.getColumnIndex("prepTimeMinutes")))
        cursor.close()

        db.close()
    }

    /**
     * Verifies the new primary key is the id column (single TEXT column),
     * not the old composite key (mealPlanId, date, mealType, recipeId).
     * Inserting two rows with the same id should fail (PK violation).
     */
    @Test
    fun migration_10_11_newPrimaryKeyIsUUID() {
        val dbV10 = helper.createDatabase(TEST_DB, 10)
        insertMealPlanAt(dbV10, "plan-002")
        dbV10.close()

        val db = helper.runMigrationsAndValidate(
            TEST_DB, 11, true, RasoiDatabase.MIGRATION_10_11
        )

        // Insert first item
        val item1 = ContentValues().apply {
            put("id", "item-duplicate-id")
            put("mealPlanId", "plan-002")
            put("date", "2026-03-17")
            put("dayName", "Monday")
            put("mealType", "LUNCH")
            put("recipeId", "recipe-dal-001")
            put("recipeName", "Dal Tadka")
            putNull("recipeImageUrl")
            put("prepTimeMinutes", 30)
            put("calories", 300)
            put("dietaryTags", "[\"VEG\",\"PROTEIN_RICH\"]")
            put("isLocked", 0)
            put("order", 0)
        }
        val firstInsert = db.insert("meal_plan_items", SQLiteDatabase.CONFLICT_FAIL, item1)
        assertTrue("First insert should succeed", firstInsert > 0)

        // Insert second item with SAME id but different data — should fail
        val item2 = ContentValues().apply {
            put("id", "item-duplicate-id")
            put("mealPlanId", "plan-002")
            put("date", "2026-03-18")
            put("dayName", "Tuesday")
            put("mealType", "DINNER")
            put("recipeId", "recipe-biryani-001")
            put("recipeName", "Veg Biryani")
            putNull("recipeImageUrl")
            put("prepTimeMinutes", 45)
            put("calories", 500)
            put("dietaryTags", "[\"VEG\"]")
            put("isLocked", 0)
            put("order", 0)
        }
        try {
            db.insert("meal_plan_items", SQLiteDatabase.CONFLICT_FAIL, item2)
            fail("Inserting duplicate id should throw — PK constraint violation")
        } catch (e: Exception) {
            // Expected: UNIQUE constraint failed on id column
            assertTrue(
                "Error should mention constraint violation",
                e.message?.contains("UNIQUE constraint failed") == true ||
                    e.message?.contains("PRIMARY KEY") == true ||
                    e.message?.contains("constraint") == true
            )
        }

        // Verify only one row exists
        val cursor = db.query("SELECT COUNT(*) FROM meal_plan_items WHERE id = 'item-duplicate-id'")
        cursor.moveToFirst()
        assertEquals("Only one row should exist for duplicate id", 1, cursor.getInt(0))
        cursor.close()

        db.close()
    }

    /**
     * Verifies that the parent meal_plans table is unaffected by the migration.
     * Data in meal_plans should be preserved even though meal_plan_items is dropped.
     */
    @Test
    fun migration_10_11_preservesMealPlans() {
        // Create DB at v10 with meal_plan data
        val dbV10 = helper.createDatabase(TEST_DB, 10)
        insertMealPlanAt(dbV10, "plan-preserved-001")

        // Also insert a second plan
        val plan2 = ContentValues().apply {
            put("id", "plan-preserved-002")
            put("weekStartDate", "2026-03-10")
            put("weekEndDate", "2026-03-16")
            put("createdAt", 1710100000000L)
            put("updatedAt", 1710100000000L)
            put("isSynced", 0)
        }
        dbV10.insert("meal_plans", SQLiteDatabase.CONFLICT_FAIL, plan2)
        dbV10.close()

        // Run migration 10->11
        val db = helper.runMigrationsAndValidate(
            TEST_DB, 11, true, RasoiDatabase.MIGRATION_10_11
        )

        // Verify both meal_plans rows survive
        val cursor = db.query("SELECT * FROM meal_plans ORDER BY id")
        assertEquals("Both meal plans should exist", 2, cursor.count)

        cursor.moveToFirst()
        assertEquals("plan-preserved-001", cursor.getString(cursor.getColumnIndex("id")))
        assertEquals("2026-03-03", cursor.getString(cursor.getColumnIndex("weekStartDate")))
        assertEquals(1, cursor.getInt(cursor.getColumnIndex("isSynced")))

        cursor.moveToNext()
        assertEquals("plan-preserved-002", cursor.getString(cursor.getColumnIndex("id")))
        assertEquals("2026-03-10", cursor.getString(cursor.getColumnIndex("weekStartDate")))
        assertEquals(0, cursor.getInt(cursor.getColumnIndex("isSynced")))

        cursor.close()

        // Verify meal_plan_items is empty (dropped and recreated)
        val itemsCursor = db.query("SELECT COUNT(*) FROM meal_plan_items")
        itemsCursor.moveToFirst()
        assertEquals("meal_plan_items should be empty after drop+recreate", 0, itemsCursor.getInt(0))
        itemsCursor.close()

        db.close()
    }

    private fun insertMealPlanAt(db: androidx.sqlite.db.SupportSQLiteDatabase, planId: String) {
        val values = ContentValues().apply {
            put("id", planId)
            put("weekStartDate", "2026-03-03")
            put("weekEndDate", "2026-03-09")
            put("createdAt", 1709467200000L)
            put("updatedAt", 1709467200000L)
            put("isSynced", 1)
        }
        db.insert("meal_plans", SQLiteDatabase.CONFLICT_FAIL, values)
    }

    companion object {
        private const val TEST_DB = "migration-test-10-11"
    }
}
