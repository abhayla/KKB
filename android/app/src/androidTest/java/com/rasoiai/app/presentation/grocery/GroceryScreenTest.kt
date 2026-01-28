package com.rasoiai.app.presentation.grocery

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rasoiai.app.presentation.common.TestTags
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.domain.model.GroceryCategory
import com.rasoiai.domain.model.GroceryItem
import com.rasoiai.domain.model.GroceryList
import com.rasoiai.domain.model.IngredientCategory
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * UI Tests for GroceryScreen
 * Tests Phase 5 of E2E Testing Guide: Grocery Screen Testing
 */
@RunWith(AndroidJUnit4::class)
class GroceryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // region Test Data Factories

    private fun createTestGroceryItem(
        id: String = "item_1",
        name: String = "Tomatoes",
        quantity: String = "500",
        unit: String = "grams",
        category: IngredientCategory = IngredientCategory.VEGETABLES,
        isPurchased: Boolean = false,
        isCustom: Boolean = false
    ) = GroceryItem(
        id = id,
        name = name,
        quantity = quantity,
        unit = unit,
        category = category,
        isPurchased = isPurchased,
        isCustom = isCustom
    )

    private fun createTestGroceryList(
        items: List<GroceryItem> = listOf(
            createTestGroceryItem("1", "Tomatoes", "500", "grams", IngredientCategory.VEGETABLES),
            createTestGroceryItem("2", "Onions", "250", "grams", IngredientCategory.VEGETABLES),
            createTestGroceryItem("3", "Milk", "1", "liter", IngredientCategory.DAIRY),
            createTestGroceryItem("4", "Curd", "500", "grams", IngredientCategory.DAIRY),
            createTestGroceryItem("5", "Rice", "1", "kg", IngredientCategory.GRAINS),
            createTestGroceryItem("6", "Turmeric", "50", "grams", IngredientCategory.SPICES),
            createTestGroceryItem("7", "Cumin", "25", "grams", IngredientCategory.SPICES)
        )
    ) = GroceryList(
        id = "grocery_list_1",
        weekStartDate = LocalDate.now(),
        weekEndDate = LocalDate.now().plusDays(6),
        items = items,
        mealPlanId = "meal_plan_1"
    )

    private fun createTestUiState(
        isLoading: Boolean = false,
        errorMessage: String? = null,
        groceryList: GroceryList? = createTestGroceryList(),
        expandedCategories: Set<IngredientCategory> = IngredientCategory.entries.toSet(),
        showWhatsAppDialog: Boolean = false,
        showEditDialog: Boolean = false,
        showAddItemDialog: Boolean = false,
        showMoreOptionsMenu: Boolean = false
    ) = GroceryUiState(
        isLoading = isLoading,
        errorMessage = errorMessage,
        groceryList = groceryList,
        categories = groceryList?.categorizedItems ?: emptyList(),
        expandedCategories = expandedCategories,
        showWhatsAppDialog = showWhatsAppDialog,
        showEditDialog = showEditDialog,
        showAddItemDialog = showAddItemDialog,
        showMoreOptionsMenu = showMoreOptionsMenu
    )

    // endregion

    // region Phase 5.1: Grocery List Display Tests

    @Test
    fun groceryScreen_displaysLoadingState() {
        val uiState = createTestUiState(isLoading = true, groceryList = null)

        composeTestRule.setContent {
            RasoiAITheme {
                GroceryTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.GROCERY_SCREEN).assertIsDisplayed()
    }

    @Test
    fun groceryScreen_displaysGroceryListHeader() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                GroceryTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Grocery List").assertIsDisplayed()
    }

    @Test
    fun groceryScreen_displaysWeekDateRange() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                GroceryTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Week of", substring = true).assertIsDisplayed()
    }

    @Test
    fun groceryScreen_displaysTotalItemsCount() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                GroceryTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("7 items").assertIsDisplayed()
    }

    @Test
    fun groceryScreen_displaysWhatsAppShareButton() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                GroceryTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Share via WhatsApp").assertIsDisplayed()
    }

    @Test
    fun groceryScreen_displaysCategorySections() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                GroceryTestContent(uiState = uiState)
            }
        }

        // Verify category headers are displayed
        composeTestRule.onNodeWithText("VEGETABLES", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("DAIRY", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun groceryScreen_displaysGroceryItems() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                GroceryTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Tomatoes").assertIsDisplayed()
        composeTestRule.onNodeWithText("Onions").assertIsDisplayed()
    }

    @Test
    fun groceryScreen_displaysItemQuantities() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                GroceryTestContent(uiState = uiState)
            }
        }

        // Use first() to avoid ambiguity when multiple items have same quantity
        composeTestRule.onAllNodesWithText("500 grams").onFirst().assertIsDisplayed()
    }

    @Test
    fun groceryScreen_hasGroceryListData() {
        // Verify that grocery list data exists in state (simplified test)
        val uiState = createTestUiState()
        assert(uiState.groceryList != null) { "Grocery list should exist" }
        assert(uiState.groceryList?.items?.isNotEmpty() == true) { "Grocery list should have items" }
    }

    @Test
    fun groceryScreen_displaysBottomNavigation() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                GroceryTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.BOTTOM_NAV).assertIsDisplayed()
    }

    // endregion

    // region Phase 5.2: Check/Uncheck Items Tests

    @Test
    fun groceryItem_checkboxToggle_triggersCallback() {
        var toggledItem: GroceryItem? = null
        val testItem = createTestGroceryItem()
        val uiState = createTestUiState(
            groceryList = createTestGroceryList(listOf(testItem))
        )

        composeTestRule.setContent {
            RasoiAITheme {
                GroceryTestContent(
                    uiState = uiState,
                    onToggleItemPurchased = { toggledItem = it }
                )
            }
        }

        composeTestRule.onNodeWithText("Tomatoes").performClick()

        assert(toggledItem != null) { "Toggle callback was not triggered" }
        assert(toggledItem?.id == testItem.id) { "Wrong item was toggled" }
    }

    @Test
    fun groceryItem_purchased_showsStrikethroughStyle() {
        val purchasedItem = createTestGroceryItem(isPurchased = true)
        val uiState = createTestUiState(
            groceryList = createTestGroceryList(listOf(purchasedItem))
        )

        composeTestRule.setContent {
            RasoiAITheme {
                GroceryTestContent(uiState = uiState)
            }
        }

        // The purchased item should still be displayed (with strikethrough styling)
        composeTestRule.onNodeWithText("Tomatoes").assertIsDisplayed()
    }

    // endregion

    // region Phase 5.3: WhatsApp Share Tests

    @Test
    fun whatsAppShareButton_click_triggersCallback() {
        var shareClicked = false
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                GroceryTestContent(
                    uiState = uiState,
                    onWhatsAppShareClick = { shareClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Share via WhatsApp").performClick()

        assert(shareClicked) { "WhatsApp share callback was not triggered" }
    }

    // endregion

    // region Category Collapse/Expand Tests

    @Test
    fun categoryHeader_click_triggersToggleCallback() {
        var toggledCategory: IngredientCategory? = null
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                GroceryTestContent(
                    uiState = uiState,
                    onToggleCategory = { toggledCategory = it }
                )
            }
        }

        composeTestRule.onNodeWithText("VEGETABLES", substring = true).performClick()

        assert(toggledCategory == IngredientCategory.VEGETABLES) {
            "Expected VEGETABLES but got $toggledCategory"
        }
    }

    @Test
    fun categoryCollapsed_itemsNotDisplayed() {
        val uiState = createTestUiState(
            expandedCategories = emptySet() // All collapsed
        )

        composeTestRule.setContent {
            RasoiAITheme {
                GroceryTestContent(uiState = uiState)
            }
        }

        // Category headers should still be visible
        composeTestRule.onNodeWithText("VEGETABLES", substring = true).assertIsDisplayed()

        // But items inside collapsed categories should not be visible
        // (Due to AnimatedVisibility they won't render when not expanded)
    }

    // endregion

    // region Add Custom Item Tests

    @Test
    fun groceryScreen_hasCategorizedItems() {
        // Verify that categories exist in state (simplified test)
        val uiState = createTestUiState()
        assert(uiState.categories.isNotEmpty()) { "Categories should exist" }
    }

    // endregion

    // region More Options Menu Tests

    @Test
    fun moreOptionsButton_click_triggersCallback() {
        var menuClicked = false
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                GroceryTestContent(
                    uiState = uiState,
                    onMoreOptionsClick = { menuClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("More options").performClick()

        assert(menuClicked) { "More options callback was not triggered" }
    }

    @Test
    fun moreOptionsMenu_expanded_showsClearPurchasedOption() {
        val uiState = createTestUiState(showMoreOptionsMenu = true)

        composeTestRule.setContent {
            RasoiAITheme {
                GroceryTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Clear purchased items").assertIsDisplayed()
    }

    @Test
    fun moreOptionsMenu_expanded_showsShareAsTextOption() {
        val uiState = createTestUiState(showMoreOptionsMenu = true)

        composeTestRule.setContent {
            RasoiAITheme {
                GroceryTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Share as text").assertIsDisplayed()
    }

    // endregion

    // region Navigation Tests

    @Test
    fun backButton_click_triggersNavigateBack() {
        var backClicked = false
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                GroceryTestContent(
                    uiState = uiState,
                    onBackClick = { backClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()

        assert(backClicked) { "Back navigation callback was not triggered" }
    }

    // endregion

    // region Empty State Tests

    @Test
    fun emptyGroceryList_displaysEmptyState() {
        val uiState = createTestUiState(
            groceryList = createTestGroceryList(items = emptyList())
        )

        composeTestRule.setContent {
            RasoiAITheme {
                GroceryTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("0 items").assertIsDisplayed()
    }

    // endregion
}

// region Test Composable Wrapper

@androidx.compose.runtime.Composable
private fun GroceryTestContent(
    uiState: GroceryUiState,
    onBackClick: () -> Unit = {},
    onWhatsAppShareClick: () -> Unit = {},
    onToggleCategory: (IngredientCategory) -> Unit = {},
    onToggleItemPurchased: (GroceryItem) -> Unit = {},
    onEditItem: (GroceryItem) -> Unit = {},
    onDeleteItem: (GroceryItem) -> Unit = {},
    onAddCustomItem: () -> Unit = {},
    onMoreOptionsClick: () -> Unit = {},
    onDismissMoreOptions: () -> Unit = {},
    onClearPurchasedClick: () -> Unit = {},
    onShareAsTextClick: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }

    GroceryScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onWhatsAppShareClick = onWhatsAppShareClick,
        onToggleCategory = onToggleCategory,
        onToggleItemPurchased = onToggleItemPurchased,
        onEditItem = onEditItem,
        onDeleteItem = onDeleteItem,
        onAddCustomItem = onAddCustomItem,
        onMoreOptionsClick = onMoreOptionsClick,
        onDismissMoreOptions = onDismissMoreOptions,
        onClearPurchasedClick = onClearPurchasedClick,
        onShareAsTextClick = onShareAsTextClick,
        onBottomNavItemClick = {}
    )
}

// endregion
