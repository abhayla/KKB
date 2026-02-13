package com.rasoiai.app.presentation.settings.screens

import app.cash.turbine.test
import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.DayOfWeek
import com.rasoiai.domain.model.PrimaryDiet
import com.rasoiai.domain.model.SpiceLevel
import com.rasoiai.domain.model.User
import com.rasoiai.domain.model.UserPreferences
import com.rasoiai.domain.repository.SettingsRepository
import io.mockk.coEvery
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
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("SpiceLevelViewModel")
class SpiceLevelViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockSettingsRepository: SettingsRepository
    private lateinit var viewModel: SpiceLevelViewModel

    private val testPreferences = UserPreferences(
        householdSize = 4,
        familyMembers = emptyList(),
        primaryDiet = PrimaryDiet.VEGETARIAN,
        dietaryRestrictions = emptyList(),
        cuisinePreferences = listOf(CuisineType.NORTH),
        spiceLevel = SpiceLevel.MEDIUM,
        dislikedIngredients = emptyList(),
        weekdayCookingTimeMinutes = 30,
        weekendCookingTimeMinutes = 60,
        busyDays = listOf(DayOfWeek.MONDAY)
    )

    private val testUser = User(
        id = "user-1",
        email = "test@example.com",
        name = "Test User",
        profileImageUrl = null,
        isOnboarded = true,
        preferences = testPreferences
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockSettingsRepository = mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): SpiceLevelViewModel {
        return SpiceLevelViewModel(
            settingsRepository = mockSettingsRepository
        ).also { viewModel = it }
    }

    @Nested
    @DisplayName("Initialization")
    inner class Initialization {

        @Test
        @DisplayName("initial state is loading")
        fun `initial state is loading`() = runTest {
            every { mockSettingsRepository.getCurrentUser() } returns flowOf(testUser)

            val vm = createViewModel()

            vm.uiState.test {
                val state = awaitItem()
                assertTrue(state.isLoading)
            }
        }

        @Test
        @DisplayName("after loading, spice level populated from repository")
        fun `after loading spice level populated from repository`() = runTest {
            every { mockSettingsRepository.getCurrentUser() } returns flowOf(testUser)

            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertFalse(state.isLoading)
                assertEquals(SpiceLevel.MEDIUM, state.spiceLevel)
            }
        }
    }

    @Nested
    @DisplayName("updateSpiceLevel")
    inner class UpdateSpiceLevel {

        @Test
        @DisplayName("updateSpiceLevel changes level")
        fun `updateSpiceLevel changes level`() = runTest {
            every { mockSettingsRepository.getCurrentUser() } returns flowOf(testUser)

            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.updateSpiceLevel(SpiceLevel.VERY_SPICY)
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertEquals(SpiceLevel.VERY_SPICY, state.spiceLevel)
            }
        }
    }

    @Nested
    @DisplayName("save")
    inner class Save {

        @Test
        @DisplayName("save success sets saveSuccess to true")
        fun `save success sets saveSuccess to true`() = runTest {
            every { mockSettingsRepository.getCurrentUser() } returns flowOf(testUser)
            coEvery { mockSettingsRepository.updateUserPreferences(any()) } returns Result.success(Unit)

            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.save()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertTrue(state.saveSuccess)
                assertFalse(state.isSaving)
            }
        }

        @Test
        @DisplayName("save failure sets errorMessage")
        fun `save failure sets errorMessage`() = runTest {
            every { mockSettingsRepository.getCurrentUser() } returns flowOf(testUser)
            coEvery { mockSettingsRepository.updateUserPreferences(any()) } returns Result.failure(
                RuntimeException("Network error")
            )

            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.save()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertFalse(state.saveSuccess)
                assertNotNull(state.errorMessage)
                assertFalse(state.isSaving)
            }
        }
    }
}
