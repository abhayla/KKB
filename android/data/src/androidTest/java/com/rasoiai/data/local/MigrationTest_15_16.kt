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
 * Tests Room migration from version 15 to 16.
 *
 * Migration 15→16 adds three rating aggregate columns to the `recipes` table
 * so the backend's RecipeResponse.average_rating / rating_count / user_rating
 * survive an offline-open round-trip (issue #21 deferred acceptance criterion).
 *
 * - averageRating REAL (nullable) — backend avg across all raters
 * - ratingCount  INTEGER NOT NULL DEFAULT 0 — total rater count
 * - userRating   REAL (nullable) — caller's own rating, if any
 *
 * The migration is additive-only: existing rows get NULL/0 defaults and no
 * data is lost. Downgrade would drop the three columns.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest_15_16 {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        RasoiDatabase::class.java
    )

    /**
     * Verifies that the migration preserves an existing recipe row and
     * that the three new columns are populated with NULL/0 defaults.
     */
    @Test
    fun migration_preservesExistingRecipeAndAddsDefaults() {
        // Create DB at v15 and insert a pre-migration recipe row
        val dbV15 = helper.createDatabase(TEST_DB, 15)
        val values = ContentValues().apply {
            put("id", "recipe-legacy-001")
            put("name", "Paneer Butter Masala")
            put("description", "Creamy tomato-based curry")
            put("imageUrl", "https://example.com/p.jpg")
            put("prepTimeMinutes", 15)
            put("cookTimeMinutes", 30)
            put("servings", 4)
            put("difficulty", "medium")
            put("cuisineType", "north")
            put("mealTypes", "lunch,dinner")
            put("dietaryTags", "vegetarian")
            put("ingredients", "[]")
            put("instructions", "[]")
            put("nutritionInfo", null as String?)
            put("calories", 350)
            put("isFavorite", 1)
            put("cachedAt", 1709380800000L)
        }
        dbV15.insert("recipes", SQLiteDatabase.CONFLICT_FAIL, values)
        dbV15.close()

        // Run migration 15→16
        val db = helper.runMigrationsAndValidate(
            TEST_DB, 16, true, RasoiDatabase.MIGRATION_15_16
        )

        // Existing row survives untouched
        val cursor = db.query("SELECT * FROM recipes WHERE id = 'recipe-legacy-001'")
        assertTrue("Pre-migration recipe should survive", cursor.moveToFirst())
        assertEquals("Paneer Butter Masala", cursor.getString(cursor.getColumnIndex("name")))
        assertEquals(350, cursor.getInt(cursor.getColumnIndex("calories")))
        assertEquals(1, cursor.getInt(cursor.getColumnIndex("isFavorite")))

        // New columns exist and carry safe defaults
        val avgIdx = cursor.getColumnIndex("averageRating")
        val countIdx = cursor.getColumnIndex("ratingCount")
        val userIdx = cursor.getColumnIndex("userRating")
        assertTrue("averageRating column must exist", avgIdx >= 0)
        assertTrue("ratingCount column must exist", countIdx >= 0)
        assertTrue("userRating column must exist", userIdx >= 0)
        assertTrue("averageRating defaults to NULL for pre-migration rows", cursor.isNull(avgIdx))
        assertEquals("ratingCount defaults to 0", 0, cursor.getInt(countIdx))
        assertTrue("userRating defaults to NULL for pre-migration rows", cursor.isNull(userIdx))
        cursor.close()
        db.close()
    }

    /**
     * Verifies that a new recipe with rating fields written post-migration
     * round-trips through SELECT, confirming the columns are writable.
     */
    @Test
    fun migration_allowsWritingRatingFieldsAfterMigration() {
        helper.createDatabase(TEST_DB, 15).close()
        val db = helper.runMigrationsAndValidate(
            TEST_DB, 16, true, RasoiDatabase.MIGRATION_15_16
        )

        val values = ContentValues().apply {
            put("id", "recipe-new-002")
            put("name", "Masala Dosa")
            put("description", "Crispy rice-lentil crepe")
            put("imageUrl", null as String?)
            put("prepTimeMinutes", 20)
            put("cookTimeMinutes", 15)
            put("servings", 2)
            put("difficulty", "medium")
            put("cuisineType", "south")
            put("mealTypes", "breakfast")
            put("dietaryTags", "vegetarian")
            put("ingredients", "[]")
            put("instructions", "[]")
            put("nutritionInfo", null as String?)
            put("calories", null as Int?)
            put("isFavorite", 0)
            put("cachedAt", 1709467200000L)
            put("averageRating", 4.25)
            put("ratingCount", 12)
            put("userRating", 5.0)
        }
        db.insert("recipes", SQLiteDatabase.CONFLICT_FAIL, values)

        val cursor = db.query("SELECT * FROM recipes WHERE id = 'recipe-new-002'")
        assertTrue(cursor.moveToFirst())
        assertEquals(4.25, cursor.getDouble(cursor.getColumnIndex("averageRating")), 0.001)
        assertEquals(12, cursor.getInt(cursor.getColumnIndex("ratingCount")))
        assertEquals(5.0, cursor.getDouble(cursor.getColumnIndex("userRating")), 0.001)
        cursor.close()
        db.close()
    }

    companion object {
        private const val TEST_DB = "migration-test-15-16"
    }
}
