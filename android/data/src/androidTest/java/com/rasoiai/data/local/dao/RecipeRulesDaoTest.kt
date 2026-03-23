package com.rasoiai.data.local.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.rasoiai.data.local.entity.RecipeRuleEntity
import com.rasoiai.data.local.entity.SyncStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecipeRulesDaoTest : BaseDaoTest() {

    private val recipeRulesDao: RecipeRulesDao get() = database.recipeRulesDao()

    private fun makeRule(
        id: String = "rule-1",
        type: String = "INGREDIENT",
        action: String = "INCLUDE",
        targetId: String = "target-1",
        targetName: String = "Paneer",
        frequencyType: String = "WEEKLY",
        frequencyCount: Int? = 3,
        frequencyDays: String? = null,
        enforcement: String = "STRICT",
        mealSlots: String? = null,
        isActive: Boolean = true,
        forceOverride: Boolean = false,
        syncStatus: String = SyncStatus.SYNCED,
        createdAt: String = "2026-03-01T10:00:00",
        updatedAt: String = "2026-03-01T10:00:00"
    ) = RecipeRuleEntity(
        id = id,
        type = type,
        action = action,
        targetId = targetId,
        targetName = targetName,
        frequencyType = frequencyType,
        frequencyCount = frequencyCount,
        frequencyDays = frequencyDays,
        enforcement = enforcement,
        mealSlots = mealSlots,
        isActive = isActive,
        forceOverride = forceOverride,
        syncStatus = syncStatus,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    // ==================== findDuplicate Tests ====================

    @Test
    fun findDuplicate_exactMatch_returnsExistingRule() = runTest {
        val rule = makeRule(targetName = "Paneer", action = "INCLUDE")
        recipeRulesDao.insertRule(rule)

        val duplicate = recipeRulesDao.findDuplicate(
            targetName = "Paneer",
            action = "INCLUDE",
            mealSlots = null
        )

        assertNotNull(duplicate)
        assertEquals("rule-1", duplicate!!.id)
        assertEquals("Paneer", duplicate.targetName)
    }

    @Test
    fun findDuplicate_caseInsensitive_matchesUpperLower() = runTest {
        val rule = makeRule(targetName = "paneer", action = "include")
        recipeRulesDao.insertRule(rule)

        val duplicate = recipeRulesDao.findDuplicate(
            targetName = "PANEER",
            action = "INCLUDE",
            mealSlots = null
        )

        assertNotNull(duplicate)
        assertEquals("rule-1", duplicate!!.id)
    }

    @Test
    fun findDuplicate_differentAction_returnsNull() = runTest {
        val rule = makeRule(targetName = "Paneer", action = "INCLUDE")
        recipeRulesDao.insertRule(rule)

        val duplicate = recipeRulesDao.findDuplicate(
            targetName = "Paneer",
            action = "EXCLUDE",
            mealSlots = null
        )

        assertNull(duplicate)
    }

    @Test
    fun findDuplicate_noMatch_returnsNull() = runTest {
        val rule = makeRule(targetName = "Paneer", action = "INCLUDE")
        recipeRulesDao.insertRule(rule)

        val duplicate = recipeRulesDao.findDuplicate(
            targetName = "Chicken",
            action = "INCLUDE",
            mealSlots = null
        )

        assertNull(duplicate)
    }

    @Test
    fun findDuplicate_differentTargetType_sameNameSameAction_stillMatches() = runTest {
        // findDuplicate does not filter by type — it matches on targetName + action + mealSlots
        val rule = makeRule(
            targetName = "Dal",
            action = "INCLUDE",
            type = "INGREDIENT"
        )
        recipeRulesDao.insertRule(rule)

        val duplicate = recipeRulesDao.findDuplicate(
            targetName = "Dal",
            action = "INCLUDE",
            mealSlots = null
        )

        // findDuplicate query matches on targetName + action + mealSlots only (not type)
        assertNotNull(duplicate)
    }

    @Test
    fun findDuplicate_withMealSlots_matchesExactSlots() = runTest {
        val rule = makeRule(
            targetName = "Eggs",
            action = "INCLUDE",
            mealSlots = "BREAKFAST,LUNCH"
        )
        recipeRulesDao.insertRule(rule)

        // Same meal slots (case-insensitive)
        val matchingDup = recipeRulesDao.findDuplicate(
            targetName = "Eggs",
            action = "INCLUDE",
            mealSlots = "breakfast,lunch"
        )
        assertNotNull(matchingDup)

        // Different meal slots
        val nonMatchingDup = recipeRulesDao.findDuplicate(
            targetName = "Eggs",
            action = "INCLUDE",
            mealSlots = "DINNER"
        )
        assertNull(nonMatchingDup)
    }

    // ==================== getAllRules Tests ====================

    @Test
    fun getAllRules_returnsInsertedRules() = runTest {
        val rules = listOf(
            makeRule(id = "rule-1", targetName = "Paneer"),
            makeRule(id = "rule-2", targetName = "Chicken"),
            makeRule(id = "rule-3", targetName = "Dal")
        )
        recipeRulesDao.insertRules(rules)

        recipeRulesDao.getAllRules().test {
            val result = awaitItem()
            assertEquals(3, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== updateRuleSyncStatus Tests ====================

    @Test
    fun updateRuleSyncStatus_updatesStatusAndTimestamp() = runTest {
        val rule = makeRule(syncStatus = SyncStatus.PENDING)
        recipeRulesDao.insertRule(rule)

        recipeRulesDao.updateRuleSyncStatus(
            ruleId = "rule-1",
            syncStatus = SyncStatus.SYNCED,
            updatedAt = "2026-03-02T12:00:00"
        )

        val updated = recipeRulesDao.getRuleByIdSync("rule-1")
        assertNotNull(updated)
        assertEquals(SyncStatus.SYNCED, updated!!.syncStatus)
        assertEquals("2026-03-02T12:00:00", updated.updatedAt)
    }
}
