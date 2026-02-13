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
@DisplayName("DislikedIngredientsViewModel")
class DislikedIngredientsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockSettingsRepository: SettingsRepository
    private lateinit var viewModel: DislikedIngredientsViewModel

    private val testPreferences = UserPreferences(
        householdSize = 4,
        familyMembers = emptyList(),
        primaryDiet = PrimaryDiet.VEGETARIAN,
        dietaryRestrictions = emptyList(),
        cuisinePreferences = listOf(CuisineType.NORTH),
        spiceLevel = SpiceLevel.MEDIUM,
        dislikedIngredients = listOf("Karela", "Lauki"),
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

    private fun createViewModel(): DislikedIngredientsViewModel {
        return DislikedIngredientsViewModel(
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
        @DisplayName("after loading, state populated from repository")
        fun `after loading state populated from repository`() = runTest {
            every { mockSettingsRepository.getCurrentUser() } returns flowOf(testUser)

            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertFalse(state.isLoading)
                assertTrue(state.dislikedIngredients.contains("Karela"))
                assertTrue(state.dislikedIngredients.contains("Lauki"))
                assertEquals(2, state.dislikedIngredients.size)
            }
        }
    }

    @Nested
    @DisplayName("toggleIngredient")
    inner class ToggleIngredient {

        @Test
        @DisplayName("toggleIngredient adds ingredient")
        fun `toggleIngredient adds ingredient`() = runTest {
            every { mockSettingsRepository.getCurrentUser() } returns flowOf(testUser)

            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.toggleIngredient("Tinda")
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertTrue(state.dislikedIngredients.contains("Tinda"))
                assertEquals(3, state.dislikedIngredients.size)
            }
        }

        @Test
        @DisplayName("toggleIngredient removes ingredient when already present")
        fun `toggleIngredient removes ingredient`() = runTest {
            every { mockSettingsRepository.getCurrentUser() } returns flowOf(testUser)

            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.toggleIngredient("Karela")
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertFalse(state.dislikedIngredients.contains("Karela"))
                assertEquals(1, state.dislikedIngredients.size)
            }
        }
    }

    @Nested
    @DisplayName("Search")
    inner class Search {

        @Test
        @DisplayName("updateSearchQuery updates query")
        fun `updateSearchQuery updates query`() = runTest {
            every { mockSettingsRepository.getCurrentUser() } returns flowOf(testUser)

            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.updateSearchQuery("paneer")
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertEquals("paneer", state.searchQuery)
            }
        }

        @Test
        @DisplayName("addCustomIngredient adds ingredient")
        fun `addCustomIngredient adds ingredient`() = runTest {
            every { mockSettingsRepository.getCurrentUser() } returns flowOf(testUser)

            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.addCustomIngredient("Capsicum")
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertTrue(state.dislikedIngredients.contains("Capsicum"))
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
    }
}
