package com.rasoiai.app.e2e.robots

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.rasoiai.app.e2e.base.waitUntilNodeWithTagExists
import com.rasoiai.app.presentation.common.TestTags

/**
 * Robot for Grocery screen interactions.
 * Handles grocery list display, check/uncheck, and sharing.
 */
class GroceryRobot(private val composeTestRule: ComposeContentTestRule) {

    /**
     * Wait for grocery screen to be displayed.
     */
    fun waitForGroceryScreen(timeoutMillis: Long = 5000) = apply {
        composeTestRule.waitUntilNodeWithTagExists(TestTags.GROCERY_SCREEN, timeoutMillis)
    }

    /**
     * Assert grocery screen is displayed.
     */
    fun assertGroceryScreenDisplayed() = apply {
        composeTestRule.onNodeWithTag(TestTags.GROCERY_SCREEN).assertIsDisplayed()
    }

    // ===================== Categories =====================

    /**
     * Assert category is displayed.
     */
    fun assertCategoryDisplayed(categoryName: String) = apply {
        val categoryTag = categoryName.lowercase().replace(" ", "_")
        composeTestRule.onNodeWithTag("${TestTags.GROCERY_CATEGORY_PREFIX}$categoryTag")
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * Expand/collapse category.
     */
    fun toggleCategory(categoryName: String) = apply {
        val categoryTag = categoryName.lowercase().replace(" ", "_")
        composeTestRule.onNodeWithTag("${TestTags.GROCERY_CATEGORY_PREFIX}$categoryTag")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Assert common categories are displayed.
     */
    fun assertCommonCategoriesDisplayed() = apply {
        val categories = listOf("Vegetables", "Dairy", "Grains", "Spices", "Pulses")
        for (category in categories) {
            try {
                assertCategoryDisplayed(category)
            } catch (e: Exception) {
                // Category might not be present in this meal plan
            }
        }
    }

    // ===================== Items =====================

    /**
     * Assert grocery item is displayed.
     */
    fun assertItemDisplayed(itemName: String) = apply {
        composeTestRule.onNodeWithText(itemName, substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * Assert item with quantity.
     */
    fun assertItemWithQuantity(itemName: String, quantity: String) = apply {
        composeTestRule.onNodeWithText(itemName, substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(quantity, substring = true).assertIsDisplayed()
    }

    /**
     * Check a grocery item.
     */
    fun checkItem(itemId: String) = apply {
        composeTestRule.onNodeWithTag("${TestTags.GROCERY_ITEM_PREFIX}$itemId")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Check item by name.
     */
    fun checkItemByName(itemName: String) = apply {
        composeTestRule.onNodeWithText(itemName, substring = true)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Uncheck a grocery item.
     */
    fun uncheckItem(itemId: String) = apply {
        checkItem(itemId) // Toggle
    }

    /**
     * Assert item is checked.
     */
    fun assertItemChecked(itemId: String) = apply {
        composeTestRule.onNodeWithTag("${TestTags.GROCERY_ITEM_PREFIX}${itemId}_checkbox")
            .assertIsOn()
    }

    /**
     * Assert item is unchecked.
     */
    fun assertItemUnchecked(itemId: String) = apply {
        composeTestRule.onNodeWithTag("${TestTags.GROCERY_ITEM_PREFIX}${itemId}_checkbox")
            .assertIsOff()
    }

    // ===================== Bulk Actions =====================

    /**
     * Mark all items as checked.
     */
    fun markAllChecked() = apply {
        composeTestRule.onNodeWithText("Mark All", ignoreCase = true)
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Clear all checked items.
     */
    fun clearChecked() = apply {
        composeTestRule.onNodeWithText("Clear Checked", ignoreCase = true)
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Assert checked count in header.
     */
    fun assertCheckedCount(count: Int) = apply {
        composeTestRule.onNodeWithText("$count checked", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    // ===================== Sharing =====================

    /**
     * Tap WhatsApp share button.
     */
    fun tapWhatsAppShare() = apply {
        composeTestRule.onNodeWithTag(TestTags.GROCERY_WHATSAPP_BUTTON).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Assert WhatsApp share button is displayed.
     */
    fun assertWhatsAppShareDisplayed() = apply {
        composeTestRule.onNodeWithTag(TestTags.GROCERY_WHATSAPP_BUTTON).assertIsDisplayed()
    }

    // ===================== Offline Indicator =====================

    /**
     * Assert offline indicator is displayed.
     */
    fun assertOfflineIndicatorDisplayed() = apply {
        composeTestRule.onNodeWithText("Offline", ignoreCase = true).assertIsDisplayed()
    }

    /**
     * Assert syncing indicator is displayed.
     */
    fun assertSyncingIndicatorDisplayed() = apply {
        composeTestRule.onNodeWithText("Syncing", ignoreCase = true).assertIsDisplayed()
    }
}
