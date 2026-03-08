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
 * Tests Room migration from version 12 to 13.
 *
 * Migration 12→13 adds two new tables:
 * - households (household management)
 * - household_members (members with FK to households)
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest_12_13 {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        RasoiDatabase::class.java
    )

    /**
     * Verifies that the migration creates the households table
     * and that rows can be inserted and queried.
     */
    @Test
    fun migration_createsHouseholdsTable() {
        // Create DB at version 12
        helper.createDatabase(TEST_DB, 12).close()

        // Run migration 12→13
        val db = helper.runMigrationsAndValidate(
            TEST_DB, 13, true, RasoiDatabase.MIGRATION_12_13
        )

        // Insert a household row
        val values = ContentValues().apply {
            put("id", "household-001")
            put("name", "Sharma Family")
            put("inviteCode", "ABC123")
            put("ownerId", "user-001")
            put("maxMembers", 8)
            put("memberCount", 1)
            put("isActive", 1)
            put("createdAt", "2026-03-08T10:00:00Z")
            put("updatedAt", "2026-03-08T10:00:00Z")
        }
        db.insert("households", SQLiteDatabase.CONFLICT_FAIL, values)

        // Verify the row was inserted
        val cursor = db.query("SELECT * FROM households WHERE id = 'household-001'")
        assertTrue("Household row should exist", cursor.moveToFirst())
        assertEquals("Sharma Family", cursor.getString(cursor.getColumnIndex("name")))
        assertEquals("ABC123", cursor.getString(cursor.getColumnIndex("inviteCode")))
        assertEquals("user-001", cursor.getString(cursor.getColumnIndex("ownerId")))
        assertEquals(8, cursor.getInt(cursor.getColumnIndex("maxMembers")))
        assertEquals(1, cursor.getInt(cursor.getColumnIndex("memberCount")))
        assertEquals(1, cursor.getInt(cursor.getColumnIndex("isActive")))
        cursor.close()
        db.close()
    }

    /**
     * Verifies that the migration creates the household_members table
     * with a valid foreign key to households.
     */
    @Test
    fun migration_createsHouseholdMembersTable() {
        // Create DB at version 12
        helper.createDatabase(TEST_DB, 12).close()

        // Run migration 12→13
        val db = helper.runMigrationsAndValidate(
            TEST_DB, 13, true, RasoiDatabase.MIGRATION_12_13
        )

        // Insert a household first (FK target)
        val householdValues = ContentValues().apply {
            put("id", "household-002")
            put("name", "Gupta Household")
            put("inviteCode", "XYZ789")
            put("ownerId", "user-010")
            put("maxMembers", 6)
            put("memberCount", 2)
            put("isActive", 1)
            put("createdAt", "2026-03-08T12:00:00Z")
            put("updatedAt", "2026-03-08T12:00:00Z")
        }
        db.insert("households", SQLiteDatabase.CONFLICT_FAIL, householdValues)

        // Insert a household member referencing the household
        val memberValues = ContentValues().apply {
            put("id", "member-001")
            put("householdId", "household-002")
            put("userId", "user-010")
            put("name", "Ramesh Gupta")
            put("role", "OWNER")
            put("canEditSharedPlan", 1)
            put("isTemporary", 0)
            put("joinDate", "2026-03-08T12:00:00Z")
            put("portionSize", 1.0)
            put("status", "active")
        }
        db.insert("household_members", SQLiteDatabase.CONFLICT_FAIL, memberValues)

        // Verify the member was inserted
        val cursor = db.query(
            "SELECT * FROM household_members WHERE householdId = 'household-002'"
        )
        assertTrue("Member row should exist", cursor.moveToFirst())
        assertEquals("member-001", cursor.getString(cursor.getColumnIndex("id")))
        assertEquals("Ramesh Gupta", cursor.getString(cursor.getColumnIndex("name")))
        assertEquals("OWNER", cursor.getString(cursor.getColumnIndex("role")))
        assertEquals(1, cursor.getInt(cursor.getColumnIndex("canEditSharedPlan")))
        assertEquals(1.0, cursor.getDouble(cursor.getColumnIndex("portionSize")), 0.001)
        cursor.close()

        // Verify the index on householdId exists
        val indexCursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND name='index_household_members_householdId'"
        )
        assertTrue("Index on householdId should exist", indexCursor.moveToFirst())
        indexCursor.close()

        db.close()
    }

    /**
     * Verifies that existing data in the meal_plans table is preserved
     * after running the migration from v12 to v13.
     */
    @Test
    fun migration_preservesExistingData() {
        // Create DB at version 12 and insert a meal_plan row
        val dbV12 = helper.createDatabase(TEST_DB, 12)
        val mealPlanValues = ContentValues().apply {
            put("id", "plan-existing-001")
            put("weekStartDate", "2026-03-02")
            put("weekEndDate", "2026-03-08")
            put("createdAt", 1709380800000L)
            put("updatedAt", 1709380800000L)
            put("isSynced", 1)
        }
        dbV12.insert("meal_plans", SQLiteDatabase.CONFLICT_FAIL, mealPlanValues)
        dbV12.close()

        // Run migration 12→13
        val db = helper.runMigrationsAndValidate(
            TEST_DB, 13, true, RasoiDatabase.MIGRATION_12_13
        )

        // Verify meal_plan data is still present
        val cursor = db.query("SELECT * FROM meal_plans WHERE id = 'plan-existing-001'")
        assertTrue("Existing meal_plan row should survive migration", cursor.moveToFirst())
        assertEquals("plan-existing-001", cursor.getString(cursor.getColumnIndex("id")))
        assertEquals("2026-03-02", cursor.getString(cursor.getColumnIndex("weekStartDate")))
        assertEquals("2026-03-08", cursor.getString(cursor.getColumnIndex("weekEndDate")))
        assertEquals(1709380800000L, cursor.getLong(cursor.getColumnIndex("createdAt")))
        assertEquals(1, cursor.getInt(cursor.getColumnIndex("isSynced")))
        cursor.close()

        // Also verify the new tables exist (migration ran fully)
        val tablesCursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name IN ('households', 'household_members') ORDER BY name"
        )
        assertEquals("Both new tables should exist", 2, tablesCursor.count)
        tablesCursor.close()

        db.close()
    }

    companion object {
        private const val TEST_DB = "migration-test-12-13"
    }
}
