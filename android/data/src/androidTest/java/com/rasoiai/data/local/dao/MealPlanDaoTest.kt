package com.rasoiai.data.local.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.rasoiai.data.local.entity.MealPlanEntity
import com.rasoiai.data.local.entity.MealPlanFestivalEntity
import com.rasoiai.data.local.entity.MealPlanItemEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MealPlanDaoTest : BaseDaoTest() {
    private val mealPlanDao: MealPlanDao get() = database.mealPlanDao()

    private val testMealPlan = MealPlanEntity(
        id = "plan-1",
        weekStartDate = "2026-01-27",
        weekEndDate = "2026-02-02",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        isSynced = false
    )

    private val testMealPlanItem = MealPlanItemEntity(
        id = "item-1",
        mealPlanId = "plan-1",
        date = "2026-01-27",
        dayName = "Monday",
        mealType = "breakfast",
        recipeId = "recipe-1",
        recipeName = "Poha",
        recipeImageUrl = "https://example.com/poha.jpg",
        prepTimeMinutes = 20,
        calories = 300,
        dietaryTags = listOf("vegetarian"),
        isLocked = false,
        order = 0
    )

    private val testFestival = MealPlanFestivalEntity(
        id = "fest-1",
        mealPlanId = "plan-1",
        date = "2026-01-27",
        name = "Republic Day",
        isFastingDay = false,
        suggestedDishes = listOf("Tricolor Salad", "Tiranga Pulao")
    )

    // ==================== Meal Plan Tests ====================

    @Test
    fun insertMealPlan_andGetById_returnsMealPlan() = runTest {
        // Given
        mealPlanDao.insertMealPlan(testMealPlan)

        // When
        val result = mealPlanDao.getMealPlanById(testMealPlan.id)

        // Then
        assertNotNull(result)
        assertEquals("plan-1", result?.id)
        assertEquals("2026-01-27", result?.weekStartDate)
    }

    @Test
    fun getMealPlanForDate_returnsCorrectPlan() = runTest {
        // Given
        mealPlanDao.insertMealPlan(testMealPlan)

        // When & Then - date within range
        mealPlanDao.getMealPlanForDate("2026-01-29").test {
            val plan = awaitItem()
            assertNotNull(plan)
            assertEquals("plan-1", plan?.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getMealPlanForDate_whenOutOfRange_returnsNull() = runTest {
        // Given
        mealPlanDao.insertMealPlan(testMealPlan)

        // When & Then - date outside range
        mealPlanDao.getMealPlanForDate("2026-02-10").test {
            val plan = awaitItem()
            assertNull(plan)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun updateSyncStatus_updatesMealPlan() = runTest {
        // Given
        mealPlanDao.insertMealPlan(testMealPlan)
        assertFalse(testMealPlan.isSynced)

        // When
        mealPlanDao.updateSyncStatus(testMealPlan.id, true)

        // Then
        val result = mealPlanDao.getMealPlanById(testMealPlan.id)
        assertTrue(result?.isSynced == true)
    }

    @Test
    fun getUnsyncedMealPlans_returnsOnlyUnsynced() = runTest {
        // Given
        val plans = listOf(
            testMealPlan.copy(id = "plan-1", isSynced = false),
            testMealPlan.copy(id = "plan-2", weekStartDate = "2026-02-03", weekEndDate = "2026-02-09", isSynced = true),
            testMealPlan.copy(id = "plan-3", weekStartDate = "2026-02-10", weekEndDate = "2026-02-16", isSynced = false)
        )
        plans.forEach { mealPlanDao.insertMealPlan(it) }

        // When
        val result = mealPlanDao.getUnsyncedMealPlans()

        // Then
        assertEquals(2, result.size)
        assertTrue(result.none { it.isSynced })
    }

    @Test
    fun deleteMealPlan_removesPlan() = runTest {
        // Given
        mealPlanDao.insertMealPlan(testMealPlan)

        // When
        mealPlanDao.deleteMealPlan(testMealPlan.id)

        // Then
        val result = mealPlanDao.getMealPlanById(testMealPlan.id)
        assertNull(result)
    }

    // ==================== Meal Plan Items Tests ====================

    @Test
    fun insertMealPlanItems_andGetItems_returnsItems() = runTest {
        // Given
        mealPlanDao.insertMealPlan(testMealPlan)
        mealPlanDao.insertMealPlanItems(listOf(testMealPlanItem))

        // When & Then
        mealPlanDao.getMealPlanItems(testMealPlan.id).test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("Poha", items.first().recipeName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getMealPlanItemsSync_returnsItemsSynchronously() = runTest {
        // Given
        mealPlanDao.insertMealPlan(testMealPlan)
        mealPlanDao.insertMealPlanItems(listOf(testMealPlanItem))

        // When
        val items = mealPlanDao.getMealPlanItemsSync(testMealPlan.id)

        // Then
        assertEquals(1, items.size)
        assertEquals("Poha", items.first().recipeName)
    }

    @Test
    fun getMealPlanItemsForDate_returnsItemsForSpecificDate() = runTest {
        // Given
        mealPlanDao.insertMealPlan(testMealPlan)
        val items = listOf(
            testMealPlanItem.copy(date = "2026-01-27", recipeName = "Day 1 Breakfast"),
            testMealPlanItem.copy(date = "2026-01-28", recipeId = "recipe-2", recipeName = "Day 2 Breakfast")
        )
        mealPlanDao.insertMealPlanItems(items)

        // When & Then
        mealPlanDao.getMealPlanItemsForDate(testMealPlan.id, "2026-01-27").test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Day 1 Breakfast", result.first().recipeName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun updateMealItemLockState_updatesItem() = runTest {
        // Given
        mealPlanDao.insertMealPlan(testMealPlan)
        mealPlanDao.insertMealPlanItems(listOf(testMealPlanItem))

        // When
        mealPlanDao.updateMealItemLockState(
            mealPlanId = testMealPlan.id,
            date = "2026-01-27",
            mealType = "breakfast",
            recipeId = "recipe-1",
            isLocked = true
        )

        // Then
        mealPlanDao.getMealPlanItems(testMealPlan.id).test {
            val items = awaitItem()
            assertTrue(items.first().isLocked)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteMealPlanItem_removesSpecificItem() = runTest {
        // Given
        mealPlanDao.insertMealPlan(testMealPlan)
        val items = listOf(
            testMealPlanItem,
            testMealPlanItem.copy(mealType = "lunch", recipeId = "recipe-2", recipeName = "Lunch Item")
        )
        mealPlanDao.insertMealPlanItems(items)

        // When
        mealPlanDao.deleteMealPlanItem(
            mealPlanId = testMealPlan.id,
            date = "2026-01-27",
            mealType = "breakfast",
            recipeId = "recipe-1"
        )

        // Then
        mealPlanDao.getMealPlanItems(testMealPlan.id).test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Lunch Item", result.first().recipeName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteMealPlanItems_removesAllItemsForPlan() = runTest {
        // Given
        mealPlanDao.insertMealPlan(testMealPlan)
        mealPlanDao.insertMealPlanItems(listOf(
            testMealPlanItem,
            testMealPlanItem.copy(mealType = "lunch", recipeId = "recipe-2"),
            testMealPlanItem.copy(mealType = "dinner", recipeId = "recipe-3")
        ))

        // When
        mealPlanDao.deleteMealPlanItems(testMealPlan.id)

        // Then
        val items = mealPlanDao.getMealPlanItemsSync(testMealPlan.id)
        assertTrue(items.isEmpty())
    }

    // ==================== Festival Tests ====================

    @Test
    fun insertFestivals_andGetForMealPlan_returnsFestivals() = runTest {
        // Given
        mealPlanDao.insertMealPlan(testMealPlan)
        mealPlanDao.insertFestivals(listOf(testFestival))

        // When
        val festivals = mealPlanDao.getFestivalsForMealPlan(testMealPlan.id)

        // Then
        assertEquals(1, festivals.size)
        assertEquals("Republic Day", festivals.first().name)
    }

    @Test
    fun getFestivalForDate_returnsCorrectFestival() = runTest {
        // Given
        mealPlanDao.insertMealPlan(testMealPlan)
        mealPlanDao.insertFestivals(listOf(testFestival))

        // When
        val festival = mealPlanDao.getFestivalForDate(testMealPlan.id, "2026-01-27")

        // Then
        assertNotNull(festival)
        assertEquals("Republic Day", festival?.name)
        assertFalse(festival?.isFastingDay == true)
    }

    @Test
    fun getFestivalForDate_whenNoFestival_returnsNull() = runTest {
        // Given
        mealPlanDao.insertMealPlan(testMealPlan)
        mealPlanDao.insertFestivals(listOf(testFestival))

        // When
        val festival = mealPlanDao.getFestivalForDate(testMealPlan.id, "2026-01-28")

        // Then
        assertNull(festival)
    }

    @Test
    fun deleteFestivalsForMealPlan_removesAllFestivals() = runTest {
        // Given
        mealPlanDao.insertMealPlan(testMealPlan)
        mealPlanDao.insertFestivals(listOf(
            testFestival,
            testFestival.copy(id = "fest-2", date = "2026-01-30", name = "Festival 2")
        ))

        // When
        mealPlanDao.deleteFestivalsForMealPlan(testMealPlan.id)

        // Then
        val festivals = mealPlanDao.getFestivalsForMealPlan(testMealPlan.id)
        assertTrue(festivals.isEmpty())
    }

    // ==================== Transaction Tests ====================

    @Test
    fun insertMealPlanWithItems_insertsAllAtomically() = runTest {
        // Given
        val items = listOf(
            testMealPlanItem,
            testMealPlanItem.copy(mealType = "lunch", recipeId = "recipe-2", recipeName = "Lunch"),
            testMealPlanItem.copy(mealType = "dinner", recipeId = "recipe-3", recipeName = "Dinner")
        )
        val festivals = listOf(testFestival)

        // When
        mealPlanDao.insertMealPlanWithItems(testMealPlan, items, festivals)

        // Then
        val plan = mealPlanDao.getMealPlanById(testMealPlan.id)
        assertNotNull(plan)

        val resultItems = mealPlanDao.getMealPlanItemsSync(testMealPlan.id)
        assertEquals(3, resultItems.size)

        val resultFestivals = mealPlanDao.getFestivalsForMealPlan(testMealPlan.id)
        assertEquals(1, resultFestivals.size)
    }

    @Test
    fun replaceMealPlan_replacesAllData() = runTest {
        // Given - initial data
        mealPlanDao.insertMealPlanWithItems(
            testMealPlan,
            listOf(testMealPlanItem),
            listOf(testFestival)
        )

        // When - replace with new items
        val updatedPlan = testMealPlan.copy(updatedAt = System.currentTimeMillis())
        val newItems = listOf(
            testMealPlanItem.copy(recipeName = "New Breakfast", recipeId = "new-recipe-1"),
            testMealPlanItem.copy(mealType = "lunch", recipeId = "new-recipe-2", recipeName = "New Lunch")
        )
        val newFestivals = listOf(
            testFestival.copy(id = "new-fest", name = "New Festival")
        )
        mealPlanDao.replaceMealPlan(updatedPlan, newItems, newFestivals)

        // Then
        val items = mealPlanDao.getMealPlanItemsSync(testMealPlan.id)
        assertEquals(2, items.size)
        assertTrue(items.any { it.recipeName == "New Breakfast" })
        assertTrue(items.any { it.recipeName == "New Lunch" })
        assertTrue(items.none { it.recipeName == "Poha" })

        val festivals = mealPlanDao.getFestivalsForMealPlan(testMealPlan.id)
        assertEquals(1, festivals.size)
        assertEquals("New Festival", festivals.first().name)
    }

    @Test
    fun mealPlanWithItemsSorted_returnsSortedByDateMealTypeOrder() = runTest {
        // Given
        mealPlanDao.insertMealPlan(testMealPlan)
        val items = listOf(
            testMealPlanItem.copy(date = "2026-01-27", mealType = "dinner", order = 0, recipeId = "r1", recipeName = "Dinner"),
            testMealPlanItem.copy(date = "2026-01-28", mealType = "breakfast", order = 0, recipeId = "r2", recipeName = "Next Day Breakfast"),
            testMealPlanItem.copy(date = "2026-01-27", mealType = "breakfast", order = 1, recipeId = "r3", recipeName = "Second Breakfast Item"),
            testMealPlanItem.copy(date = "2026-01-27", mealType = "breakfast", order = 0, recipeId = "r4", recipeName = "First Breakfast Item")
        )
        mealPlanDao.insertMealPlanItems(items)

        // When
        val result = mealPlanDao.getMealPlanItemsSync(testMealPlan.id)

        // Then - should be sorted by date, mealType, order
        assertEquals(4, result.size)
        // First day breakfast items (order 0, then order 1)
        assertEquals("First Breakfast Item", result[0].recipeName)
        assertEquals("Second Breakfast Item", result[1].recipeName)
        // First day dinner
        assertEquals("Dinner", result[2].recipeName)
        // Second day breakfast
        assertEquals("Next Day Breakfast", result[3].recipeName)
    }

    @Test
    fun cascadeDelete_deletesItemsWhenPlanDeleted() = runTest {
        // Given
        mealPlanDao.insertMealPlanWithItems(
            testMealPlan,
            listOf(testMealPlanItem),
            emptyList()
        )

        // When
        mealPlanDao.deleteMealPlan(testMealPlan.id)

        // Then - items should also be deleted due to foreign key cascade
        val items = mealPlanDao.getMealPlanItemsSync(testMealPlan.id)
        assertTrue(items.isEmpty())
    }
}
