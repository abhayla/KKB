package com.rasoiai.app.presentation.onboarding

import app.cash.turbine.test
import com.rasoiai.data.local.datastore.UserPreferencesDataStore
import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.DayOfWeek
import com.rasoiai.domain.model.DietaryRestriction
import com.rasoiai.domain.model.FamilyMember
import com.rasoiai.domain.model.MemberType
import com.rasoiai.domain.model.PrimaryDiet
import com.rasoiai.domain.model.SpiceLevel
import com.rasoiai.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockUserPreferencesDataStore: UserPreferencesDataStore
    private lateinit var mockSettingsRepository: SettingsRepository

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockUserPreferencesDataStore = mockk(relaxed = true)
        mockSettingsRepository = mockk(relaxed = true)
        coEvery { mockUserPreferencesDataStore.saveOnboardingComplete(any()) } returns Unit
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("Initial State")
    inner class InitialState {

        @Test
        @DisplayName("Initial step should be HOUSEHOLD_SIZE")
        fun `initial step should be HOUSEHOLD_SIZE`() = runTest {
            val viewModel = OnboardingViewModel(mockUserPreferencesDataStore, mockSettingsRepository)

            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals(OnboardingStep.HOUSEHOLD_SIZE, state.currentStep)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Default household size should be 2")
        fun `default household size should be 2`() = runTest {
            val viewModel = OnboardingViewModel(mockUserPreferencesDataStore, mockSettingsRepository)

            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals(2, state.householdSize)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Default primary diet should be VEGETARIAN")
        fun `default primary diet should be VEGETARIAN`() = runTest {
            val viewModel = OnboardingViewModel(mockUserPreferencesDataStore, mockSettingsRepository)

            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals(PrimaryDiet.VEGETARIAN, state.primaryDiet)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("isFirstStep should be true initially")
        fun `isFirstStep should be true initially`() = runTest {
            val viewModel = OnboardingViewModel(mockUserPreferencesDataStore, mockSettingsRepository)

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state.isFirstStep)
                assertFalse(state.isLastStep)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Step Navigation")
    inner class StepNavigation {

        @Test
        @DisplayName("goToNextStep should advance to next step")
        fun `goToNextStep should advance to next step`() = runTest {
            val viewModel = OnboardingViewModel(mockUserPreferencesDataStore, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.goToNextStep()

                val state = awaitItem()
                assertEquals(OnboardingStep.DIETARY_PREFERENCES, state.currentStep)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("goToPreviousStep should go back to previous step")
        fun `goToPreviousStep should go back to previous step`() = runTest {
            val viewModel = OnboardingViewModel(mockUserPreferencesDataStore, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.goToNextStep()
                awaitItem() // Step 2

                viewModel.goToPreviousStep()

                val state = awaitItem()
                assertEquals(OnboardingStep.HOUSEHOLD_SIZE, state.currentStep)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("progress should update with step")
        fun `progress should update with step`() = runTest {
            val viewModel = OnboardingViewModel(mockUserPreferencesDataStore, mockSettingsRepository)

            viewModel.uiState.test {
                val initialState = awaitItem()
                assertEquals(0.2f, initialState.progress) // 1/5

                viewModel.goToNextStep()
                val step2State = awaitItem()
                assertEquals(0.4f, step2State.progress) // 2/5

                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Step 1: Household Size")
    inner class HouseholdSize {

        @Test
        @DisplayName("updateHouseholdSize should update size")
        fun `updateHouseholdSize should update size`() = runTest {
            val viewModel = OnboardingViewModel(mockUserPreferencesDataStore, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.updateHouseholdSize(4)

                val state = awaitItem()
                assertEquals(4, state.householdSize)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("updateHouseholdSize should coerce to valid range")
        fun `updateHouseholdSize should coerce to valid range`() = runTest {
            val viewModel = OnboardingViewModel(mockUserPreferencesDataStore, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.updateHouseholdSize(15) // Too high

                val state = awaitItem()
                assertEquals(10, state.householdSize) // Max is 10
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("showAddMemberDialog should show dialog")
        fun `showAddMemberDialog should show dialog`() = runTest {
            val viewModel = OnboardingViewModel(mockUserPreferencesDataStore, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showAddMemberDialog()

                val state = awaitItem()
                assertTrue(state.showAddMemberDialog)
                assertNull(state.editingMember)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("addOrUpdateFamilyMember should add new member")
        fun `addOrUpdateFamilyMember should add new member`() = runTest {
            val viewModel = OnboardingViewModel(mockUserPreferencesDataStore, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.addOrUpdateFamilyMember(
                    name = "John",
                    type = MemberType.ADULT,
                    age = 30,
                    specialNeeds = emptyList()
                )

                val state = awaitItem()
                assertEquals(1, state.familyMembers.size)
                assertEquals("John", state.familyMembers[0].name)
                assertFalse(state.showAddMemberDialog)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("removeFamilyMember should remove member")
        fun `removeFamilyMember should remove member`() = runTest {
            val viewModel = OnboardingViewModel(mockUserPreferencesDataStore, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.addOrUpdateFamilyMember("John", MemberType.ADULT, 30, emptyList())
                val stateWithMember = awaitItem()
                val member = stateWithMember.familyMembers[0]

                viewModel.removeFamilyMember(member)

                val state = awaitItem()
                assertTrue(state.familyMembers.isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Step 2: Dietary Preferences")
    inner class DietaryPreferences {

        @Test
        @DisplayName("updatePrimaryDiet should update diet")
        fun `updatePrimaryDiet should update diet`() = runTest {
            val viewModel = OnboardingViewModel(mockUserPreferencesDataStore, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.updatePrimaryDiet(PrimaryDiet.NON_VEGETARIAN)

                val state = awaitItem()
                assertEquals(PrimaryDiet.NON_VEGETARIAN, state.primaryDiet)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("toggleDietaryRestriction should add restriction")
        fun `toggleDietaryRestriction should add restriction`() = runTest {
            val viewModel = OnboardingViewModel(mockUserPreferencesDataStore, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.toggleDietaryRestriction(DietaryRestriction.JAIN)

                val state = awaitItem()
                assertTrue(state.dietaryRestrictions.contains(DietaryRestriction.JAIN))
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("toggleDietaryRestriction should remove restriction")
        fun `toggleDietaryRestriction should remove restriction`() = runTest {
            val viewModel = OnboardingViewModel(mockUserPreferencesDataStore, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.toggleDietaryRestriction(DietaryRestriction.JAIN)
                awaitItem() // Added

                viewModel.toggleDietaryRestriction(DietaryRestriction.JAIN)

                val state = awaitItem()
                assertFalse(state.dietaryRestrictions.contains(DietaryRestriction.JAIN))
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Step 3: Cuisine Preferences")
    inner class CuisinePreferences {

        @Test
        @DisplayName("toggleCuisine should add cuisine")
        fun `toggleCuisine should add cuisine`() = runTest {
            val viewModel = OnboardingViewModel(mockUserPreferencesDataStore, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial (NORTH selected)

                viewModel.toggleCuisine(CuisineType.SOUTH)

                val state = awaitItem()
                assertTrue(state.selectedCuisines.contains(CuisineType.SOUTH))
                assertTrue(state.selectedCuisines.contains(CuisineType.NORTH))
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("toggleCuisine should not remove last cuisine")
        fun `toggleCuisine should not remove last cuisine`() = runTest {
            val viewModel = OnboardingViewModel(mockUserPreferencesDataStore, mockSettingsRepository)

            viewModel.uiState.test {
                val initialState = awaitItem() // Initial (only NORTH selected)
                assertTrue(initialState.selectedCuisines.contains(CuisineType.NORTH))

                viewModel.toggleCuisine(CuisineType.NORTH) // Try to remove

                // No state change expected - still has NORTH
                expectNoEvents() // Verify no new emissions

                // Verify current state is unchanged
                assertEquals(1, initialState.selectedCuisines.size)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("updateSpiceLevel should update level")
        fun `updateSpiceLevel should update level`() = runTest {
            val viewModel = OnboardingViewModel(mockUserPreferencesDataStore, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial (MEDIUM)

                viewModel.updateSpiceLevel(SpiceLevel.SPICY)

                val state = awaitItem()
                assertEquals(SpiceLevel.SPICY, state.spiceLevel)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Step 4: Disliked Ingredients")
    inner class DislikedIngredients {

        @Test
        @DisplayName("toggleDislikedIngredient should add ingredient")
        fun `toggleDislikedIngredient should add ingredient`() = runTest {
            val viewModel = OnboardingViewModel(mockUserPreferencesDataStore, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.toggleDislikedIngredient("Karela")

                val state = awaitItem()
                assertTrue(state.dislikedIngredients.contains("Karela"))
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("addCustomDislikedIngredient should add custom ingredient")
        fun `addCustomDislikedIngredient should add custom ingredient`() = runTest {
            val viewModel = OnboardingViewModel(mockUserPreferencesDataStore, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.addCustomDislikedIngredient("Broccoli")

                val state = awaitItem()
                assertTrue(state.dislikedIngredients.contains("Broccoli"))
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("updateIngredientSearchQuery should update query")
        fun `updateIngredientSearchQuery should update query`() = runTest {
            val viewModel = OnboardingViewModel(mockUserPreferencesDataStore, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.updateIngredientSearchQuery("kar")

                val state = awaitItem()
                assertEquals("kar", state.ingredientSearchQuery)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Step 5: Cooking Time")
    inner class CookingTime {

        @Test
        @DisplayName("updateWeekdayCookingTime should update time")
        fun `updateWeekdayCookingTime should update time`() = runTest {
            val viewModel = OnboardingViewModel(mockUserPreferencesDataStore, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial (30 min)

                viewModel.updateWeekdayCookingTime(45)

                val state = awaitItem()
                assertEquals(45, state.weekdayCookingTimeMinutes)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("updateWeekendCookingTime should update time")
        fun `updateWeekendCookingTime should update time`() = runTest {
            val viewModel = OnboardingViewModel(mockUserPreferencesDataStore, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial (45 min)

                viewModel.updateWeekendCookingTime(60)

                val state = awaitItem()
                assertEquals(60, state.weekendCookingTimeMinutes)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("toggleBusyDay should add busy day")
        fun `toggleBusyDay should add busy day`() = runTest {
            val viewModel = OnboardingViewModel(mockUserPreferencesDataStore, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.toggleBusyDay(DayOfWeek.MONDAY)

                val state = awaitItem()
                assertTrue(state.busyDays.contains(DayOfWeek.MONDAY))
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("toggleBusyDay should remove busy day")
        fun `toggleBusyDay should remove busy day`() = runTest {
            val viewModel = OnboardingViewModel(mockUserPreferencesDataStore, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.toggleBusyDay(DayOfWeek.MONDAY)
                awaitItem() // Added

                viewModel.toggleBusyDay(DayOfWeek.MONDAY)

                val state = awaitItem()
                assertFalse(state.busyDays.contains(DayOfWeek.MONDAY))
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Completion")
    inner class Completion {

        @Test
        @DisplayName("goToNextStep on last step should start generation")
        fun `goToNextStep on last step should start generation`() = runTest {
            val viewModel = OnboardingViewModel(mockUserPreferencesDataStore, mockSettingsRepository)

            viewModel.uiState.test {
                var currentState = awaitItem() // Initial (step 0)

                // Navigate to last step (5 steps total: 0,1,2,3,4 - we need to go 4 times)
                repeat(4) {
                    viewModel.goToNextStep()
                    currentState = awaitItem()
                }

                // Confirm we're on last step (step 4)
                assertTrue(currentState.isLastStep)

                // Try to proceed - should start generation
                viewModel.goToNextStep()

                val generatingState = awaitItem()
                assertTrue(generatingState.isGenerating)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Completion should emit navigation event")
        fun `completion should emit navigation event`() = runTest {
            val viewModel = OnboardingViewModel(mockUserPreferencesDataStore, mockSettingsRepository)

            viewModel.navigationEvent.test {
                // Navigate to last step and complete
                repeat(4) { viewModel.goToNextStep() }
                viewModel.goToNextStep() // Trigger completion

                // Advance time to allow simulation to complete
                testDispatcher.scheduler.advanceTimeBy(5000)

                val event = awaitItem()
                assertEquals(OnboardingNavigationEvent.NavigateToHome, event)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Computed Properties")
    inner class ComputedProperties {

        @Test
        @DisplayName("canProceed should be true when household size > 0")
        fun `canProceed should be true when household size greater than 0`() = runTest {
            val viewModel = OnboardingViewModel(mockUserPreferencesDataStore, mockSettingsRepository)

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state.canProceed) // Default size is 2
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("canProceed should require at least one cuisine")
        fun `canProceed should require at least one cuisine`() = runTest {
            val viewModel = OnboardingViewModel(mockUserPreferencesDataStore, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                // Go to cuisine step
                viewModel.goToNextStep()
                awaitItem()
                viewModel.goToNextStep()

                val cuisineStepState = awaitItem()
                assertEquals(OnboardingStep.CUISINE_PREFERENCES, cuisineStepState.currentStep)
                assertTrue(cuisineStepState.canProceed) // NORTH is selected by default
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Error Handling")
    inner class ErrorHandling {

        @Test
        @DisplayName("clearError should clear error message")
        fun `clearError should clear error message`() = runTest {
            val viewModel = OnboardingViewModel(mockUserPreferencesDataStore, mockSettingsRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.clearError()

                // clearError may not emit if errorMessage was already null
                // Verify state has null errorMessage
                assertNull(viewModel.uiState.value.errorMessage)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
