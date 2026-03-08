package com.rasoiai.app.presentation.grocery

import app.cash.turbine.test
import com.rasoiai.domain.model.DataScope
import com.rasoiai.domain.model.GroceryItem
import com.rasoiai.domain.model.GroceryList
import com.rasoiai.domain.model.IngredientCategory
import com.rasoiai.domain.repository.GroceryRepository
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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class GroceryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockGroceryRepository: GroceryRepository

    private val testGroceryItems = listOf(
        GroceryItem(
            id = "item-1",
            name = "Tomatoes",
            quantity = "500",
            unit = "g",
            category = IngredientCategory.VEGETABLES,
            isPurchased = false
        ),
        GroceryItem(
            id = "item-2",
            name = "Onions",
            quantity = "1",
            unit = "kg",
            category = IngredientCategory.VEGETABLES,
            isPurchased = true
        ),
        GroceryItem(
            id = "item-3",
            name = "Milk",
            quantity = "1",
            unit = "L",
            category = IngredientCategory.DAIRY,
            isPurchased = false
        )
    )

    private val testGroceryList = GroceryList(
        id = "list-1",
        weekStartDate = LocalDate.now().minusDays(LocalDate.now().dayOfWeek.ordinal.toLong()),
        weekEndDate = LocalDate.now().plusDays(6 - LocalDate.now().dayOfWeek.ordinal.toLong()),
        items = testGroceryItems,
        mealPlanId = "meal-plan-1"
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockGroceryRepository = mockk(relaxed = true)
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
            coEvery { mockGroceryRepository.getCurrentGroceryList() } returns flowOf(null)

            val viewModel = GroceryViewModel(mockGroceryRepository)

            viewModel.uiState.test {
                val initialState = awaitItem()
                assertTrue(initialState.isLoading)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("After loading, isLoading should be false")
        fun `after loading isLoading should be false`() = runTest {
            coEvery { mockGroceryRepository.getCurrentGroceryList() } returns flowOf(testGroceryList)

            val viewModel = GroceryViewModel(mockGroceryRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                testDispatcher.scheduler.advanceUntilIdle()

                val loadedState = awaitItem()
                assertFalse(loadedState.isLoading)
                assertNotNull(loadedState.groceryList)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("All categories should be expanded by default")
        fun `all categories should be expanded by default`() = runTest {
            coEvery { mockGroceryRepository.getCurrentGroceryList() } returns flowOf(testGroceryList)

            val viewModel = GroceryViewModel(mockGroceryRepository)

            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals(IngredientCategory.entries.size, state.expandedCategories.size)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Category Expansion")
    inner class CategoryExpansion {

        @Test
        @DisplayName("toggleCategoryExpanded should collapse expanded category")
        fun `toggleCategoryExpanded should collapse expanded category`() = runTest {
            coEvery { mockGroceryRepository.getCurrentGroceryList() } returns flowOf(testGroceryList)

            val viewModel = GroceryViewModel(mockGroceryRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.toggleCategoryExpanded(IngredientCategory.VEGETABLES)

                val state = awaitItem()
                assertFalse(state.expandedCategories.contains(IngredientCategory.VEGETABLES))
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("toggleCategoryExpanded should expand collapsed category")
        fun `toggleCategoryExpanded should expand collapsed category`() = runTest {
            coEvery { mockGroceryRepository.getCurrentGroceryList() } returns flowOf(testGroceryList)

            val viewModel = GroceryViewModel(mockGroceryRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                // Collapse first
                viewModel.toggleCategoryExpanded(IngredientCategory.VEGETABLES)
                awaitItem()

                // Expand again
                viewModel.toggleCategoryExpanded(IngredientCategory.VEGETABLES)

                val state = awaitItem()
                assertTrue(state.expandedCategories.contains(IngredientCategory.VEGETABLES))
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Dialogs")
    inner class Dialogs {

        @Test
        @DisplayName("showEditDialog should show dialog with selected item")
        fun `showEditDialog should show dialog with selected item`() = runTest {
            coEvery { mockGroceryRepository.getCurrentGroceryList() } returns flowOf(testGroceryList)

            val viewModel = GroceryViewModel(mockGroceryRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                val item = testGroceryItems[0]
                viewModel.showEditDialog(item)

                val state = awaitItem()
                assertTrue(state.showEditDialog)
                assertEquals(item, state.selectedItem)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("dismissEditDialog should hide dialog")
        fun `dismissEditDialog should hide dialog`() = runTest {
            coEvery { mockGroceryRepository.getCurrentGroceryList() } returns flowOf(testGroceryList)

            val viewModel = GroceryViewModel(mockGroceryRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showEditDialog(testGroceryItems[0])
                awaitItem()

                viewModel.dismissEditDialog()

                val state = awaitItem()
                assertFalse(state.showEditDialog)
                assertNull(state.selectedItem)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("showAddItemDialog should show dialog")
        fun `showAddItemDialog should show dialog`() = runTest {
            coEvery { mockGroceryRepository.getCurrentGroceryList() } returns flowOf(testGroceryList)

            val viewModel = GroceryViewModel(mockGroceryRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showAddItemDialog()

                val state = awaitItem()
                assertTrue(state.showAddItemDialog)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("showWhatsAppDialog should show dialog")
        fun `showWhatsAppDialog should show dialog`() = runTest {
            coEvery { mockGroceryRepository.getCurrentGroceryList() } returns flowOf(testGroceryList)

            val viewModel = GroceryViewModel(mockGroceryRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showWhatsAppDialog()

                val state = awaitItem()
                assertTrue(state.showWhatsAppDialog)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Share Options")
    inner class ShareOptions {

        @Test
        @DisplayName("setShareOption should update share option")
        fun `setShareOption should update share option`() = runTest {
            coEvery { mockGroceryRepository.getCurrentGroceryList() } returns flowOf(testGroceryList)

            val viewModel = GroceryViewModel(mockGroceryRepository)

            viewModel.uiState.test {
                awaitItem() // Initial (FULL_LIST by default)

                viewModel.setShareOption(ShareOption.UNPURCHASED_ONLY)

                val state = awaitItem()
                assertEquals(ShareOption.UNPURCHASED_ONLY, state.shareOption)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("shareViaWhatsApp should emit navigation event")
        fun `shareViaWhatsApp should emit navigation event`() = runTest {
            coEvery { mockGroceryRepository.getCurrentGroceryList() } returns flowOf(testGroceryList)

            val viewModel = GroceryViewModel(mockGroceryRepository)

            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.showWhatsAppDialog()

            viewModel.navigationEvent.test {
                viewModel.shareViaWhatsApp()
                val event = awaitItem()
                assertTrue(event is GroceryNavigationEvent.ShareViaWhatsApp)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("More Options Menu")
    inner class MoreOptionsMenu {

        @Test
        @DisplayName("showMoreOptionsMenu should show menu")
        fun `showMoreOptionsMenu should show menu`() = runTest {
            coEvery { mockGroceryRepository.getCurrentGroceryList() } returns flowOf(testGroceryList)

            val viewModel = GroceryViewModel(mockGroceryRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showMoreOptionsMenu()

                val state = awaitItem()
                assertTrue(state.showMoreOptionsMenu)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("dismissMoreOptionsMenu should hide menu")
        fun `dismissMoreOptionsMenu should hide menu`() = runTest {
            coEvery { mockGroceryRepository.getCurrentGroceryList() } returns flowOf(testGroceryList)

            val viewModel = GroceryViewModel(mockGroceryRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.showMoreOptionsMenu()
                awaitItem()

                viewModel.dismissMoreOptionsMenu()

                val state = awaitItem()
                assertFalse(state.showMoreOptionsMenu)
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
            coEvery { mockGroceryRepository.getCurrentGroceryList() } returns flowOf(testGroceryList)

            val viewModel = GroceryViewModel(mockGroceryRepository)

            viewModel.navigationEvent.test {
                viewModel.navigateBack()
                val event = awaitItem()
                assertEquals(GroceryNavigationEvent.NavigateBack, event)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("navigateToHome should emit home event")
        fun `navigateToHome should emit home event`() = runTest {
            coEvery { mockGroceryRepository.getCurrentGroceryList() } returns flowOf(testGroceryList)

            val viewModel = GroceryViewModel(mockGroceryRepository)

            viewModel.navigationEvent.test {
                viewModel.navigateToHome()
                val event = awaitItem()
                assertEquals(GroceryNavigationEvent.NavigateToHome, event)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("navigateToChat should emit chat event")
        fun `navigateToChat should emit chat event`() = runTest {
            coEvery { mockGroceryRepository.getCurrentGroceryList() } returns flowOf(testGroceryList)

            val viewModel = GroceryViewModel(mockGroceryRepository)

            viewModel.navigationEvent.test {
                viewModel.navigateToChat()
                val event = awaitItem()
                assertEquals(GroceryNavigationEvent.NavigateToChat, event)
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
            coEvery { mockGroceryRepository.getCurrentGroceryList() } returns flowOf(testGroceryList)

            val viewModel = GroceryViewModel(mockGroceryRepository)

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
        @DisplayName("totalItems should return correct count")
        fun `totalItems should return correct count`() = runTest {
            coEvery { mockGroceryRepository.getCurrentGroceryList() } returns flowOf(testGroceryList)

            val viewModel = GroceryViewModel(mockGroceryRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()

                val state = awaitItem()
                assertEquals(3, state.totalItems)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("purchasedItems should return correct count")
        fun `purchasedItems should return correct count`() = runTest {
            coEvery { mockGroceryRepository.getCurrentGroceryList() } returns flowOf(testGroceryList)

            val viewModel = GroceryViewModel(mockGroceryRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()

                val state = awaitItem()
                assertEquals(1, state.purchasedItems) // Only onions purchased
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("unpurchasedItems should return correct count")
        fun `unpurchasedItems should return correct count`() = runTest {
            coEvery { mockGroceryRepository.getCurrentGroceryList() } returns flowOf(testGroceryList)

            val viewModel = GroceryViewModel(mockGroceryRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()

                val state = awaitItem()
                assertEquals(2, state.unpurchasedItems) // Tomatoes and Milk
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Scope Toggle")
    inner class ScopeToggleTests {

        @Test
        @DisplayName("initial selectedScope should be PERSONAL")
        fun `initial selectedScope should be PERSONAL`() = runTest {
            coEvery { mockGroceryRepository.getCurrentGroceryList() } returns flowOf(null)

            val viewModel = GroceryViewModel(mockGroceryRepository)

            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals(DataScope.PERSONAL, state.selectedScope)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("setScope to FAMILY should update selectedScope")
        fun `setScope to FAMILY should update selectedScope`() = runTest {
            coEvery { mockGroceryRepository.getCurrentGroceryList() } returns flowOf(testGroceryList)

            val viewModel = GroceryViewModel(mockGroceryRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.setScope(DataScope.FAMILY)

                val state = awaitItem()
                assertEquals(DataScope.FAMILY, state.selectedScope)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("setScope back to PERSONAL should restore selectedScope")
        fun `setScope back to PERSONAL should restore selectedScope`() = runTest {
            coEvery { mockGroceryRepository.getCurrentGroceryList() } returns flowOf(testGroceryList)

            val viewModel = GroceryViewModel(mockGroceryRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                viewModel.setScope(DataScope.FAMILY)
                awaitItem() // FAMILY

                viewModel.setScope(DataScope.PERSONAL)

                val state = awaitItem()
                assertEquals(DataScope.PERSONAL, state.selectedScope)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("setScope should not affect other state fields")
        fun `setScope should not affect other state fields`() = runTest {
            coEvery { mockGroceryRepository.getCurrentGroceryList() } returns flowOf(testGroceryList)

            val viewModel = GroceryViewModel(mockGroceryRepository)

            viewModel.uiState.test {
                awaitItem() // Initial

                testDispatcher.scheduler.advanceUntilIdle()
                val beforeScope = expectMostRecentItem()

                viewModel.setScope(DataScope.FAMILY)
                val afterScope = awaitItem()

                assertEquals(beforeScope.groceryList, afterScope.groceryList)
                assertEquals(beforeScope.totalItems, afterScope.totalItems)
                assertEquals(DataScope.FAMILY, afterScope.selectedScope)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("setScope should not trigger additional repository calls")
        fun `setScope should not trigger additional repository calls`() = runTest {
            coEvery { mockGroceryRepository.getCurrentGroceryList() } returns flowOf(testGroceryList)

            val viewModel = GroceryViewModel(mockGroceryRepository)

            viewModel.uiState.test {
                awaitItem() // Initial
                testDispatcher.scheduler.advanceUntilIdle()
                expectMostRecentItem()

                viewModel.setScope(DataScope.FAMILY)
                awaitItem()

                // setScope only updates local state, no new repository calls
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
