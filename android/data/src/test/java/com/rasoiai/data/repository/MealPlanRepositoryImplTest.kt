package com.rasoiai.data.repository

import app.cash.turbine.test
import com.rasoiai.core.network.NetworkMonitor
import com.rasoiai.data.local.dao.MealPlanDao
import com.rasoiai.data.local.entity.MealPlanEntity
import com.rasoiai.data.local.entity.MealPlanFestivalEntity
import com.rasoiai.data.local.entity.MealPlanItemEntity
import com.rasoiai.data.remote.api.RasoiApiService
import com.rasoiai.data.remote.dto.MealPlanResponse
import com.rasoiai.data.remote.dto.MealPlanDayDto
import com.rasoiai.data.remote.dto.MealsByTypeDto
import com.rasoiai.data.remote.dto.MealItemDto
import com.rasoiai.domain.model.MealType
import com.rasoiai.domain.repository.GroceryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class MealPlanRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockApiService: RasoiApiService
    private lateinit var mockMealPlanDao: MealPlanDao
    private lateinit var mockNetworkMonitor: NetworkMonitor
    private lateinit var mockGroceryRepository: GroceryRepository
    private lateinit var repository: MealPlanRepositoryImpl

    private val testDate = LocalDate.of(2026, 1, 27)
    private val testDateString = "2026-01-27"

    private val testMealPlanEntity = MealPlanEntity(
        id = "plan-1",
        weekStartDate = "2026-01-27",
        weekEndDate = "2026-02-02",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        isSynced = true
    )

    private val testMealItems = listOf(
        MealPlanItemEntity(
            id = "item-1",
            mealPlanId = "plan-1",
            date = "2026-01-27",
            dayName = "Monday",
            mealType = "BREAKFAST",
            recipeId = "recipe-1",
            recipeName = "Poha",
            recipeImageUrl = null,
            prepTimeMinutes = 20,
            calories = 300,
            dietaryTags = listOf("vegetarian"),
            isLocked = false,
            order = 0
        ),
        MealPlanItemEntity(
            id = "item-2",
            mealPlanId = "plan-1",
            date = "2026-01-27",
            dayName = "Monday",
            mealType = "LUNCH",
            recipeId = "recipe-2",
            recipeName = "Dal Rice",
            recipeImageUrl = null,
            prepTimeMinutes = 45,
            calories = 450,
            dietaryTags = listOf("vegetarian"),
            isLocked = true,
            order = 0
        )
    )

    private val testFestivals = emptyList<MealPlanFestivalEntity>()

    private val testMealPlanResponse = MealPlanResponse(
        id = "plan-1",
        weekStartDate = "2026-01-27",
        weekEndDate = "2026-02-02",
        days = listOf(
            MealPlanDayDto(
                date = "2026-01-27",
                dayName = "Monday",
                meals = MealsByTypeDto(
                    breakfast = listOf(MealItemDto(
                        id = "item-1",
                        recipeId = "recipe-1",
                        recipeName = "Poha",
                        recipeImageUrl = null,
                        prepTimeMinutes = 20,
                        calories = 300,
                        isLocked = false,
                        order = 0,
                        dietaryTags = listOf("vegetarian")
                    )),
                    lunch = listOf(MealItemDto(
                        id = "item-2",
                        recipeId = "recipe-2",
                        recipeName = "Dal Rice",
                        recipeImageUrl = null,
                        prepTimeMinutes = 45,
                        calories = 450,
                        isLocked = true,
                        order = 0,
                        dietaryTags = listOf("vegetarian")
                    )),
                    dinner = emptyList(),
                    snacks = emptyList()
                ),
                festival = null
            )
        ),
        createdAt = "2026-01-27T00:00:00Z",
        updatedAt = "2026-01-27T00:00:00Z"
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockApiService = mockk(relaxed = true)
        mockMealPlanDao = mockk(relaxed = true)
        mockNetworkMonitor = mockk(relaxed = true)
        mockGroceryRepository = mockk(relaxed = true)

        repository = MealPlanRepositoryImpl(
            apiService = mockApiService,
            longTimeoutApiService = mockApiService,
            mealPlanDao = mockMealPlanDao,
            networkMonitor = mockNetworkMonitor,
            groceryRepository = mockGroceryRepository
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("getMealPlanForDate")
    inner class GetMealPlanForDate {

        @Test
        @DisplayName("Should return meal plan from local database when available")
        fun `should return meal plan from local database when available`() = runTest {
            // Given
            every { mockMealPlanDao.getMealPlanForDate(testDateString) } returns flowOf(testMealPlanEntity)
            coEvery { mockMealPlanDao.getMealPlanItemsSync("plan-1") } returns testMealItems
            coEvery { mockMealPlanDao.getFestivalsForMealPlan("plan-1") } returns testFestivals

            // When & Then
            repository.getMealPlanForDate(testDate).test {
                val mealPlan = awaitItem()

                assertNotNull(mealPlan)
                assertEquals("plan-1", mealPlan?.id)
                assertEquals(testDate, mealPlan?.weekStartDate)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Should return null when no local data and offline")
        fun `should return null when no local data and offline`() = runTest {
            // Given
            every { mockMealPlanDao.getMealPlanForDate(testDateString) } returns flowOf(null)
            every { mockNetworkMonitor.isOnline } returns flowOf(false)

            // When & Then
            repository.getMealPlanForDate(testDate).test {
                val mealPlan = awaitItem()

                assertNull(mealPlan)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Should return null when no local data and online - ViewModel handles generation")
        fun `should return null when no local data and online`() = runTest {
            // Given - Repository no longer fetches from API in getMealPlanForDate
            // This is now the ViewModel's responsibility to avoid race conditions
            every { mockMealPlanDao.getMealPlanForDate(testDateString) } returns flowOf(null)
            every { mockNetworkMonitor.isOnline } returns flowOf(true)

            // When & Then
            repository.getMealPlanForDate(testDate).test {
                val mealPlan = awaitItem()

                // Should return null - ViewModel will handle generation
                assertNull(mealPlan)
                cancelAndIgnoreRemainingEvents()
            }

            // API should NOT be called from repository's getMealPlanForDate
            coVerify(exactly = 0) { mockApiService.getCurrentMealPlan() }
        }
    }

    @Nested
    @DisplayName("hasMealPlanForCurrentWeek")
    inner class HasMealPlanForCurrentWeek {

        @Test
        @DisplayName("Should return true when meal plan exists for today")
        fun `should return true when meal plan exists for today`() = runTest {
            // Given
            val today = LocalDate.now()
            val todayString = today.toString()
            coEvery { mockMealPlanDao.hasMealPlanForDate(todayString) } returns true

            // When
            val result = repository.hasMealPlanForCurrentWeek()

            // Then
            assertTrue(result)
        }

        @Test
        @DisplayName("Should return false when no meal plan exists for today")
        fun `should return false when no meal plan exists for today`() = runTest {
            // Given
            val today = LocalDate.now()
            val todayString = today.toString()
            coEvery { mockMealPlanDao.hasMealPlanForDate(todayString) } returns false

            // When
            val result = repository.hasMealPlanForCurrentWeek()

            // Then
            assertTrue(!result)
        }
    }

    @Nested
    @DisplayName("generateMealPlan")
    inner class GenerateMealPlan {

        @Test
        @DisplayName("Should fail when offline")
        fun `should fail when offline`() = runTest {
            // Given
            every { mockNetworkMonitor.isOnline } returns flowOf(false)

            // When
            val result = repository.generateMealPlan(testDate)

            // Then
            assertTrue(result.isFailure)
            assertEquals("No network connection. Cannot generate meal plan offline.", result.exceptionOrNull()?.message)
        }

        @Test
        @DisplayName("Should generate and cache meal plan when online")
        fun `should generate and cache meal plan when online`() = runTest {
            // Given
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockApiService.generateMealPlan(any()) } returns testMealPlanResponse

            // When
            val result = repository.generateMealPlan(testDate)

            // Then
            assertTrue(result.isSuccess)
            assertEquals("plan-1", result.getOrNull()?.id)
            coVerify { mockMealPlanDao.replaceMealPlan(any(), any(), any()) }
        }

        @Test
        @DisplayName("Should trigger grocery generation after successful meal plan generation")
        fun `should trigger grocery generation on success`() = runTest {
            // Given
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockApiService.generateMealPlan(any()) } returns testMealPlanResponse

            // When
            val result = repository.generateMealPlan(testDate)

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockGroceryRepository.generateFromMealPlan("plan-1") }
        }

        @Test
        @DisplayName("Should return failure on API error")
        fun `should return failure on API error`() = runTest {
            // Given
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockApiService.generateMealPlan(any()) } throws RuntimeException("API error")

            // When
            val result = repository.generateMealPlan(testDate)

            // Then
            assertTrue(result.isFailure)
        }
    }

    @Nested
    @DisplayName("setMealLockState")
    inner class SetMealLockState {

        @Test
        @DisplayName("Should update lock state locally")
        fun `should update lock state locally`() = runTest {
            // Given
            every { mockNetworkMonitor.isOnline } returns flowOf(false)

            // When
            val result = repository.setMealLockState(
                mealPlanId = "plan-1",
                date = testDate,
                mealType = MealType.BREAKFAST,
                recipeId = "recipe-1",
                isLocked = true
            )

            // Then
            assertTrue(result.isSuccess)
            coVerify {
                mockMealPlanDao.updateMealItemLockState(
                    mealPlanId = "plan-1",
                    date = testDateString,
                    mealType = "BREAKFAST",
                    recipeId = "recipe-1",
                    isLocked = true
                )
            }
        }

        @Test
        @DisplayName("Should mark as unsynced when offline")
        fun `should mark as unsynced when offline`() = runTest {
            // Given
            every { mockNetworkMonitor.isOnline } returns flowOf(false)

            // When
            repository.setMealLockState(
                mealPlanId = "plan-1",
                date = testDate,
                mealType = MealType.BREAKFAST,
                recipeId = "recipe-1",
                isLocked = true
            )

            // Then
            coVerify { mockMealPlanDao.updateSyncStatus("plan-1", false) }
        }

        @Test
        @DisplayName("Should sync to server when online")
        fun `should sync to server when online`() = runTest {
            // Given
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockApiService.lockMealItem(any(), any()) } returns testMealPlanResponse

            // When
            repository.setMealLockState(
                mealPlanId = "plan-1",
                date = testDate,
                mealType = MealType.BREAKFAST,
                recipeId = "recipe-1",
                isLocked = true
            )

            // Then
            coVerify { mockApiService.lockMealItem(any(), any()) }
        }
    }

    @Nested
    @DisplayName("syncMealPlans")
    inner class SyncMealPlans {

        @Test
        @DisplayName("Should fail when offline")
        fun `should fail when offline`() = runTest {
            // Given
            every { mockNetworkMonitor.isOnline } returns flowOf(false)

            // When
            val result = repository.syncMealPlans()

            // Then
            assertTrue(result.isFailure)
            assertEquals("No network connection", result.exceptionOrNull()?.message)
        }

        @Test
        @DisplayName("Should sync unsynced meal plans when online")
        fun `should sync unsynced meal plans when online`() = runTest {
            // Given
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockMealPlanDao.getUnsyncedMealPlans() } returns listOf(testMealPlanEntity)
            coEvery { mockApiService.getMealPlanById("plan-1") } returns testMealPlanResponse

            // When
            val result = repository.syncMealPlans()

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockApiService.getMealPlanById("plan-1") }
            coVerify { mockMealPlanDao.replaceMealPlan(any(), any(), any()) }
        }

        @Test
        @DisplayName("Should continue syncing other plans if one fails")
        fun `should continue syncing other plans if one fails`() = runTest {
            // Given
            val secondPlan = testMealPlanEntity.copy(id = "plan-2")
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockMealPlanDao.getUnsyncedMealPlans() } returns listOf(testMealPlanEntity, secondPlan)
            coEvery { mockApiService.getMealPlanById("plan-1") } throws RuntimeException("API error")
            coEvery { mockApiService.getMealPlanById("plan-2") } returns testMealPlanResponse.copy(id = "plan-2")

            // When
            val result = repository.syncMealPlans()

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockApiService.getMealPlanById("plan-1") }
            coVerify { mockApiService.getMealPlanById("plan-2") }
        }
    }
}
