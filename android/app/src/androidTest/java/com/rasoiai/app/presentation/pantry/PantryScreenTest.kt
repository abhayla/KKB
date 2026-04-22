package com.rasoiai.app.presentation.pantry

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rasoiai.app.presentation.common.TestTags
import com.rasoiai.app.presentation.navigation.Screen
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.domain.model.PantryCategory
import com.rasoiai.domain.model.PantryItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * UI Tests for PantryScreen
 * Tests Phase 10 of E2E Testing Guide: Pantry Screen Testing
 */
@RunWith(AndroidJUnit4::class)
class PantryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // region Test Data Factories

    private fun createTestPantryItem(
        id: String = "item_1",
        name: String = "Tomatoes",
        category: PantryCategory = PantryCategory.VEGETABLES,
        quantity: Int = 5,
        unit: String = "pieces",
        expiryDate: LocalDate? = LocalDate.now().plusDays(3)
    ) = PantryItem(
        id = id,
        name = name,
        category = category,
        quantity = quantity,
        unit = unit,
        addedDate = LocalDate.now(),
        expiryDate = expiryDate
    )

    private fun createTestUiState(
        isLoading: Boolean = false,
        errorMessage: String? = null,
        pantryItems: List<PantryItem> = listOf(
            createTestPantryItem("1", "Tomatoes", PantryCategory.VEGETABLES, 5, expiryDate = LocalDate.now().plusDays(3)),
            createTestPantryItem("2", "Milk", PantryCategory.DAIRY_MILK, 1, "liter", LocalDate.now().plusDays(2)),
            createTestPantryItem("3", "Rice", PantryCategory.GRAINS, 2, "kg", null),
            createTestPantryItem("4", "Paneer", PantryCategory.DAIRY_PANEER, 200, "grams", LocalDate.now().plusDays(5)),
            createTestPantryItem("5", "Spinach", PantryCategory.LEAFY_VEGETABLES, 1, "bunch", LocalDate.now().plusDays(1))
        ),
        expiredItems: List<PantryItem> = emptyList(),
        matchingRecipeCount: Int = 12,
        showAddItemDialog: Boolean = false,
        showRemoveExpiredDialog: Boolean = false,
        showAllItemsSheet: Boolean = false,
        showScanResultsSheet: Boolean = false,
        scannedItems: List<ScannedItemData> = emptyList(),
        isScanning: Boolean = false
    ) = PantryUiState(
        isLoading = isLoading,
        errorMessage = errorMessage,
        pantryItems = pantryItems,
        expiredItems = expiredItems,
        matchingRecipeCount = matchingRecipeCount,
        showAddItemDialog = showAddItemDialog,
        showRemoveExpiredDialog = showRemoveExpiredDialog,
        showAllItemsSheet = showAllItemsSheet,
        showScanResultsSheet = showScanResultsSheet,
        scannedItems = scannedItems,
        isScanning = isScanning
    )

    // endregion

    // region Phase 10.1: Add Pantry Items Tests

    @Test
    fun pantryScreen_displaysScreenTag() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                PantryTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.PANTRY_SCREEN).assertIsDisplayed()
    }

    @Test
    fun pantryScreen_displaysTitle() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                PantryTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Pantry Scan").assertIsDisplayed()
    }

    @Test
    fun pantryScreen_displaysPantryItems() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                PantryTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Tomatoes", substring = true).assertIsDisplayed()
    }

    @Test
    fun pantryScreen_displaysAddItemButton() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                PantryTestContent(uiState = uiState)
            }
        }

        // Add button is in the TopAppBar with content description "Add item manually"
        composeTestRule.onNodeWithContentDescription("Add item manually").assertIsDisplayed()
    }

    @Test
    fun addItemButton_click_triggersCallback() {
        var addClicked = false
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                PantryTestContent(
                    uiState = uiState,
                    onAddItemClick = { addClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Add item manually").performClick()

        assert(addClicked) { "Add item callback was not triggered" }
    }

    @Test
    fun pantryScreen_displaysBottomNavigation() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                PantryTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.BOTTOM_NAV).assertIsDisplayed()
    }

    // endregion

    // region Phase 10.2: Expiry Warning Tests

    @Test
    fun pantryScreen_displaysExpiryWarningLegend() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                PantryTestContent(uiState = uiState)
            }
        }

        // Screen shows expiry legend "⚠️ = Expiring soon" — inside the pantry LazyColumn,
        // which renders below camera scan + item list on small screens.
        composeTestRule.onNodeWithTag(TestTags.PANTRY_LAZY_COLUMN)
            .performScrollToNode(hasText("Expiring soon", substring = true))
        composeTestRule.onNodeWithText("Expiring soon", substring = true).assertIsDisplayed()
    }

    @Test
    fun pantryScreen_displaysPantryItemCount() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                PantryTestContent(uiState = uiState)
            }
        }

        // "My Pantry (X items)" section
        composeTestRule.onNodeWithText("My Pantry", substring = true).assertIsDisplayed()
    }

    // endregion

    // region Camera Scan Tests

    @Test
    fun pantryScreen_displaysCameraScanSection() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                PantryTestContent(uiState = uiState)
            }
        }

        // CameraScanSection shows instruction text
        composeTestRule.onNodeWithText("Point at your fridge", substring = true).assertIsDisplayed()
    }

    @Test
    fun captureButton_click_triggersCallback() {
        var captureClicked = false
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                PantryTestContent(
                    uiState = uiState,
                    onCaptureClick = { captureClicked = true }
                )
            }
        }

        // Capture button has "Capture" content description
        composeTestRule.onNodeWithContentDescription("Capture").performClick()

        assert(captureClicked) { "Capture callback was not triggered" }
    }

    // endregion

    // region Find Recipes Tests

    @Test
    fun pantryScreen_displaysFindRecipesButton() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                PantryTestContent(uiState = uiState)
            }
        }

        // Find Recipes is the last item in the pantry LazyColumn — performScrollTo() on a
        // LazyColumn child doesn't compose the target; scroll the list itself instead.
        composeTestRule.onNodeWithTag(TestTags.PANTRY_LAZY_COLUMN)
            .performScrollToNode(hasText("Find Recipes", substring = true))
        composeTestRule.onNodeWithText("Find Recipes", substring = true).assertIsDisplayed()
    }

    @Test
    fun pantryScreen_displaysMatchingRecipeCount() {
        val uiState = createTestUiState(matchingRecipeCount = 12)

        composeTestRule.setContent {
            RasoiAITheme {
                PantryTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.PANTRY_LAZY_COLUMN)
            .performScrollToNode(hasText("12", substring = true))
        composeTestRule.onNodeWithText("12", substring = true).assertIsDisplayed()
    }

    @Test
    fun findRecipesButton_click_triggersCallback() {
        var findRecipesClicked = false
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                PantryTestContent(
                    uiState = uiState,
                    onFindRecipesClick = { findRecipesClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithTag(TestTags.PANTRY_LAZY_COLUMN)
            .performScrollToNode(hasText("Find Recipes", substring = true))
        composeTestRule.onNodeWithText("Find Recipes", substring = true).performClick()

        assert(findRecipesClicked) { "Find recipes callback was not triggered" }
    }

    // endregion

    // region Navigation Tests

    @Test
    fun backButton_click_triggersNavigateBack() {
        var backClicked = false
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                PantryTestContent(
                    uiState = uiState,
                    onBackClick = { backClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()

        assert(backClicked) { "Back navigation callback was not triggered" }
    }

    // endregion

    // region Loading State Tests

    @Test
    fun pantryScreen_loadingState_displaysScreen() {
        val uiState = createTestUiState(isLoading = true, pantryItems = emptyList())

        composeTestRule.setContent {
            RasoiAITheme {
                PantryTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.PANTRY_SCREEN).assertIsDisplayed()
    }

    // endregion

    // region Empty State Tests

    @Test
    fun emptyPantry_displaysEmptyState() {
        val uiState = createTestUiState(pantryItems = emptyList(), matchingRecipeCount = 0)

        composeTestRule.setContent {
            RasoiAITheme {
                PantryTestContent(uiState = uiState)
            }
        }

        // Empty pantry should still display the screen
        composeTestRule.onNodeWithTag(TestTags.PANTRY_SCREEN).assertIsDisplayed()
    }

    // endregion

    // region View All Tests

    @Test
    fun viewAllButton_click_triggersCallback() {
        var viewAllClicked = false
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                PantryTestContent(
                    uiState = uiState,
                    onViewAllClick = { viewAllClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithTag(TestTags.PANTRY_LAZY_COLUMN)
            .performScrollToNode(hasText("View All", substring = true))
        composeTestRule.onNodeWithText("View All", substring = true).performClick()

        assert(viewAllClicked) { "View All callback was not triggered" }
    }

    // endregion
}

// region Test Composable Wrapper

@androidx.compose.runtime.Composable
private fun PantryTestContent(
    uiState: PantryUiState,
    onBackClick: () -> Unit = {},
    onCaptureClick: () -> Unit = {},
    onGalleryClick: () -> Unit = {},
    onViewAllClick: () -> Unit = {},
    onItemClick: (String) -> Unit = {},
    onAddItemClick: () -> Unit = {},
    onFindRecipesClick: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }

    PantryScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onCaptureClick = onCaptureClick,
        onGalleryClick = onGalleryClick,
        onViewAllClick = onViewAllClick,
        onItemClick = onItemClick,
        onAddItemClick = onAddItemClick,
        onFindRecipesClick = onFindRecipesClick,
        onBottomNavItemClick = {}
    )
}

// endregion
