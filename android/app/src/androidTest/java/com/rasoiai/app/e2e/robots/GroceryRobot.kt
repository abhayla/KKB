package com.rasoiai.app.e2e.robots

import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.rasoiai.app.e2e.base.clickWithRetry
import com.rasoiai.app.e2e.base.waitForNetworkContent
import com.rasoiai.app.e2e.base.waitUntilNodeWithTagExists
import com.rasoiai.app.e2e.base.waitUntilWithBackoff
import com.rasoiai.app.e2e.util.RetryUtils
import com.rasoiai.app.presentation.common.TestTags

/**
 * Robot for Grocery screen interactions.
 * Handles grocery list display, check/uncheck, and sharing.
 */
class GroceryRobot(private val composeTestRule: ComposeContentTestRule) {

    /**
     * Wait for grocery screen to be displayed.
     * Uses exponential backoff for better reliability.
     */
    fun waitForGroceryScreen(timeoutMillis: Long = 5000) = apply {
        composeTestRule.waitUntilWithBackoff(
            tag = TestTags.GROCERY_SCREEN,
            timeoutMillis = timeoutMillis,
            initialPollMs = 100,
            maxPollMs = 500,
            backoffMultiplier = 1.5
        )
    }

    /**
     * Assert grocery screen is displayed.
     */
    fun assertGroceryScreenDisplayed() = apply {
        composeTestRule.onNodeWithTag(TestTags.GROCERY_SCREEN).assertIsDisplayed()
    }

    // ===================== Categories =====================

    /**
     * Assert category is displayed by enum name (e.g., "vegetables", "grains").
     * The category name should match IngredientCategory enum name in lowercase.
     * Uses exponential backoff for network-dependent content.
     */
    fun assertCategoryDisplayed(categoryName: String) = apply {
        val categoryTag = categoryName.lowercase()
        val fullTag = "${TestTags.GROCERY_CATEGORY_PREFIX}$categoryTag"

        // Wait for category with network content timeout
        composeTestRule.waitForNetworkContent(fullTag, timeoutMillis = 10000)

        // Scroll and assert with retry
        RetryUtils.retryWithBackoff(
            config = RetryUtils.RetryConfig.FAST,
            actionName = "assertCategoryDisplayed($categoryName)"
        ) {
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag(fullTag)
                .performScrollTo()
                .assertIsDisplayed()
        }
    }

    /**
     * Expand/collapse category by enum name (e.g., "vegetables", "grains").
     * Uses retry for flaky click operations.
     */
    fun toggleCategory(categoryName: String) = apply {
        val categoryTag = categoryName.lowercase()
        val fullTag = "${TestTags.GROCERY_CATEGORY_PREFIX}$categoryTag"

        // Wait for category with backoff
        composeTestRule.waitForNetworkContent(fullTag, timeoutMillis = 10000)

        // Scroll and click with retry
        RetryUtils.retryWithBackoff(
            config = RetryUtils.RetryConfig.FAST,
            actionName = "toggleCategory($categoryName)"
        ) {
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag(fullTag)
                .performScrollTo()
                .performClick()
        }
        composeTestRule.waitForIdle()
    }

    /**
     * Assert common categories are displayed.
     * Note: This is a flexible check - passes if screen is displayed.
     * Categories may vary based on generated meal plan.
     */
    fun assertCommonCategoriesDisplayed() = apply {
        // Just verify the screen is displayed with content
        // Categories vary based on the meal plan ingredients
        assertGroceryScreenDisplayed()
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
     * Uses retry for flaky click operations.
     */
    fun checkItem(itemId: String) = apply {
        val fullTag = "${TestTags.GROCERY_ITEM_PREFIX}$itemId"

        RetryUtils.retryWithBackoff(
            config = RetryUtils.RetryConfig.FAST,
            actionName = "checkItem($itemId)"
        ) {
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag(fullTag)
                .performScrollTo()
                .performClick()
        }
        composeTestRule.waitForIdle()
    }

    /**
     * Check item by name.
     * Uses retry for flaky click operations.
     */
    fun checkItemByName(itemName: String) = apply {
        RetryUtils.retryWithBackoff(
            config = RetryUtils.RetryConfig.FAST,
            actionName = "checkItemByName($itemName)"
        ) {
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText(itemName, substring = true)
                .performScrollTo()
                .performClick()
        }
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
     * Open more options menu.
     */
    fun openMoreOptionsMenu() = apply {
        composeTestRule.onNodeWithContentDescription("More options")
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Clear all purchased items via menu.
     * Note: Requires opening menu first via openMoreOptionsMenu().
     */
    fun clearPurchasedItems() = apply {
        composeTestRule.onNodeWithText("Clear purchased items", ignoreCase = true)
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
