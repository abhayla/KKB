package com.rasoiai.app.e2e.robots

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.rasoiai.app.e2e.base.PantryItemTestData
import com.rasoiai.app.e2e.base.waitUntilNodeWithTextExists

/**
 * Robot for Pantry screen interactions.
 * Handles pantry item management and expiry tracking.
 */
class PantryRobot(private val composeTestRule: ComposeContentTestRule) {

    /**
     * Wait for pantry screen to be displayed.
     */
    fun waitForPantryScreen(timeoutMillis: Long = 5000) = apply {
        composeTestRule.waitUntilNodeWithTextExists("Pantry", timeoutMillis)
    }

    /**
     * Assert pantry screen is displayed.
     */
    fun assertPantryScreenDisplayed() = apply {
        composeTestRule.onNodeWithText("Pantry", ignoreCase = true).assertIsDisplayed()
    }

    // ===================== Add Items =====================

    /**
     * Tap add item button.
     */
    fun tapAddItem() = apply {
        composeTestRule.onNodeWithText("Add Item", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Enter item name.
     */
    fun enterItemName(name: String) = apply {
        composeTestRule.onNodeWithText("Item Name", substring = true, ignoreCase = true)
            .performTextInput(name)
    }

    /**
     * Select item category.
     */
    fun selectCategory(category: String) = apply {
        composeTestRule.onNodeWithText("Category", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(category, ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Enter item quantity.
     */
    fun enterQuantity(quantity: Double) = apply {
        composeTestRule.onNodeWithText("Quantity", substring = true, ignoreCase = true)
            .performTextInput(quantity.toString())
    }

    /**
     * Select unit.
     */
    fun selectUnit(unit: String) = apply {
        composeTestRule.onNodeWithText("Unit", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(unit, ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Set expiry date (days from now).
     */
    fun setExpiryDays(days: Int) = apply {
        composeTestRule.onNodeWithText("Expiry", substring = true, ignoreCase = true)
            .performClick()
        composeTestRule.waitForIdle()
        // Select date from date picker
        // This is simplified - actual implementation would use date picker
    }

    /**
     * Save pantry item.
     */
    fun saveItem() = apply {
        composeTestRule.onNodeWithText("Save", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Add a complete pantry item.
     */
    fun addItem(item: PantryItemTestData) = apply {
        tapAddItem()
        enterItemName(item.name)
        selectCategory(item.category)
        enterQuantity(item.quantity)
        selectUnit(item.unit)
        if (item.expiryDays != null) {
            setExpiryDays(item.expiryDays)
        }
        saveItem()
    }

    /**
     * Cancel adding item.
     */
    fun cancelAddItem() = apply {
        composeTestRule.onNodeWithText("Cancel", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    // ===================== Item List =====================

    /**
     * Assert item is displayed in pantry.
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
     * Tap on item to edit.
     */
    fun tapItem(itemName: String) = apply {
        composeTestRule.onNodeWithText(itemName, substring = true)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Delete item.
     */
    fun deleteItem(itemName: String) = apply {
        tapItem(itemName)
        composeTestRule.onNodeWithText("Delete", ignoreCase = true).performClick()
        composeTestRule.onNodeWithText("Confirm", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    // ===================== Expiry Sections =====================

    /**
     * Assert expiring soon section is displayed.
     */
    fun assertExpiringSoonSectionDisplayed() = apply {
        composeTestRule.onNodeWithText("Expiring Soon", substring = true, ignoreCase = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * Assert item is in expiring soon section.
     */
    fun assertItemExpiringSoon(itemName: String) = apply {
        assertExpiringSoonSectionDisplayed()
        composeTestRule.onNodeWithText(itemName, substring = true).assertIsDisplayed()
    }

    /**
     * Assert expired section is displayed.
     */
    fun assertExpiredSectionDisplayed() = apply {
        composeTestRule.onNodeWithText("Expired", substring = true, ignoreCase = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * Assert item is in expired section.
     */
    fun assertItemExpired(itemName: String) = apply {
        assertExpiredSectionDisplayed()
        composeTestRule.onNodeWithText(itemName, substring = true).assertIsDisplayed()
    }

    // ===================== Scanner =====================

    /**
     * Tap scan button.
     */
    fun tapScan() = apply {
        composeTestRule.onNodeWithText("Scan", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Assert scan results sheet is displayed.
     */
    fun assertScanResultsDisplayed() = apply {
        composeTestRule.onNodeWithText("Scan Results", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    // ===================== Categories =====================

    /**
     * Assert category is displayed.
     */
    fun assertCategoryDisplayed(categoryName: String) = apply {
        composeTestRule.onNodeWithText(categoryName, substring = true, ignoreCase = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * Toggle category expansion.
     */
    fun toggleCategory(categoryName: String) = apply {
        composeTestRule.onNodeWithText(categoryName, substring = true, ignoreCase = true)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    // ===================== Empty State =====================

    /**
     * Assert empty state is displayed.
     */
    fun assertEmptyStateDisplayed() = apply {
        composeTestRule.onNodeWithText("No items", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    // ===================== Search =====================

    /**
     * Search for item.
     */
    fun searchItem(query: String) = apply {
        composeTestRule.onNodeWithText("Search", substring = true, ignoreCase = true)
            .performTextInput(query)
        composeTestRule.waitForIdle()
    }

    /**
     * Clear search.
     */
    fun clearSearch() = apply {
        composeTestRule.onNodeWithText("Clear", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }
}
