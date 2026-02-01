package com.rasoiai.app.presentation.pantry

import app.cash.turbine.test
import com.rasoiai.domain.model.PantryCategory
import com.rasoiai.domain.model.PantryItem
import com.rasoiai.domain.repository.PantryRepository
import io.mockk.coEvery
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class PantryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockPantryRepository: PantryRepository

    private val testPantryItems = listOf(
        PantryItem(
            id = "item-1",
            name = "Tomatoes",
            category = PantryCategory.VEGETABLES,
            quantity = 5,
            unit = "piece",
            addedDate = LocalDate.now().minusDays(2),
            expiryDate = LocalDate.now().plusDays(3)
        ),
        PantryItem(
            id = "item-2",
            name = "Milk",
            category = PantryCategory.DAIRY_MILK,
            quantity = 1,
            unit = "liter",
            addedDate = LocalDate.now().minusDays(1),
            expiryDate = LocalDate.now().plusDays(1)
        )
    )

    private val expiredItems = listOf(
        PantryItem(
            id = "item-3",
            name = "Yogurt",
            category = PantryCategory.DAIRY_MILK,
            quantity = 1,
            unit = "cup",
            addedDate = LocalDate.now().minusDays(5),
            expiryDate = LocalDate.now().minusDays(2)
        )
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockPantryRepository = mockk(relaxed = true)
        coEvery { mockPantryRepository.getPantryItems() } returns flowOf(testPantryItems)
        coEvery { mockPantryRepository.getExpiredItems() } returns flowOf(expiredItems)
        coEvery { mockPantryRepository.getMatchingRecipeCount() } returns Result.success(5)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("Initial State")
    inner class InitialState {

        @Test
        @DisplayName("Initial state should be loading")
        fun `initial state should be loading`() = runTest {
            val viewModel = PantryViewModel(mockPantryRepository)

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state.isLoading)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("After loading, pantry items should be populated")
        fun `after loading pantry items should be populated`() = runTest {
            val viewModel = PantryViewModel(mockPantryRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                testDispatcher.scheduler.advanceUntilIdle()

                val state = awaitItem()
                assertFalse(state.isLoading)
                assertEquals(2, state.pantryItems.size)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Expired items should be loaded")
        fun `expired items should be loaded`() = runTest {
            val viewModel = PantryViewModel(mockPantryRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                assertEquals(1, state.expiredItems.size)
                assertTrue(state.hasExpiredItems)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Matching recipe count should be loaded")
        fun `matching recipe count should be loaded`() = runTest {
            val viewModel = PantryViewModel(mockPantryRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                assertEquals(5, state.matchingRecipeCount)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Dialog Actions")
    inner class DialogActions {

        @Test
        @DisplayName("showAddItemDialog should show dialog")
        fun `showAddItemDialog should show dialog`() = runTest {
            val viewModel = PantryViewModel(mockPantryRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showAddItemDialog()

                val state = awaitItem()
                assertTrue(state.showAddItemDialog)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("dismissAddItemDialog should hide dialog")
        fun `dismissAddItemDialog should hide dialog`() = runTest {
            val viewModel = PantryViewModel(mockPantryRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showAddItemDialog()
                awaitItem()

                viewModel.dismissAddItemDialog()

                val state = awaitItem()
                assertFalse(state.showAddItemDialog)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("showRemoveExpiredDialog should show dialog")
        fun `showRemoveExpiredDialog should show dialog`() = runTest {
            val viewModel = PantryViewModel(mockPantryRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showRemoveExpiredDialog()

                val state = awaitItem()
                assertTrue(state.showRemoveExpiredDialog)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("showAllItemsSheet should show sheet")
        fun `showAllItemsSheet should show sheet`() = runTest {
            val viewModel = PantryViewModel(mockPantryRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showAllItemsSheet()

                val state = awaitItem()
                assertTrue(state.showAllItemsSheet)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Scan Actions")
    inner class ScanActions {

        @Test
        @DisplayName("simulateScan should show results after completion")
        fun `simulateScan should show results after completion`() = runTest {
            val viewModel = PantryViewModel(mockPantryRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.simulateScan()
                testDispatcher.scheduler.advanceTimeBy(2000)
                testDispatcher.scheduler.advanceUntilIdle()

                val resultsState = expectMostRecentItem()
                assertFalse(resultsState.isScanning)
                assertTrue(resultsState.showScanResultsSheet)
                assertTrue(resultsState.scannedItems.isNotEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("dismissScanResultsSheet should hide sheet")
        fun `dismissScanResultsSheet should hide sheet`() = runTest {
            val viewModel = PantryViewModel(mockPantryRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.simulateScan()
                testDispatcher.scheduler.advanceTimeBy(2000)
                awaitItem() // Scanning
                awaitItem() // Results

                viewModel.dismissScanResultsSheet()

                val state = awaitItem()
                assertFalse(state.showScanResultsSheet)
                assertTrue(state.scannedItems.isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Navigation")
    inner class Navigation {

        @Test
        @DisplayName("navigateBack should emit back event")
        fun `navigateBack should emit back event`() = runTest {
            val viewModel = PantryViewModel(mockPantryRepository)

            viewModel.navigationEvent.test {
                viewModel.navigateBack()
                val event = awaitItem()
                assertEquals(PantryNavigationEvent.NavigateBack, event)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("navigateToHome should emit home event")
        fun `navigateToHome should emit home event`() = runTest {
            val viewModel = PantryViewModel(mockPantryRepository)

            viewModel.navigationEvent.test {
                viewModel.navigateToHome()
                val event = awaitItem()
                assertEquals(PantryNavigationEvent.NavigateToHome, event)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("onFindRecipesClick should emit recipe search event")
        fun `onFindRecipesClick should emit recipe search event`() = runTest {
            val viewModel = PantryViewModel(mockPantryRepository)

            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.navigationEvent.test {
                viewModel.onFindRecipesClick()
                val event = awaitItem()
                assertTrue(event is PantryNavigationEvent.NavigateToRecipeSearch)
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
            val viewModel = PantryViewModel(mockPantryRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.clearError()

                val state = awaitItem()
                assertNull(state.errorMessage)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Computed Properties")
    inner class ComputedProperties {

        @Test
        @DisplayName("itemCount should return correct count")
        fun `itemCount should return correct count`() = runTest {
            val viewModel = PantryViewModel(mockPantryRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()

                val state = expectMostRecentItem()
                assertEquals(2, state.itemCount)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
