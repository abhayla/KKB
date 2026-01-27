package com.rasoiai.app.e2e.flows

import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.base.TestDataFactory
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.robots.PantryRobot
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test

/**
 * Phase 10: Pantry Screen Testing
 *
 * Tests:
 * 10.1 Add Pantry Items
 * 10.2 Expiring Soon Section
 */
@HiltAndroidTest
class PantryFlowTest : BaseE2ETest() {

    private lateinit var homeRobot: HomeRobot
    private lateinit var pantryRobot: PantryRobot

    @Before
    override fun setUp() {
        super.setUp()
        homeRobot = HomeRobot(composeTestRule)
        pantryRobot = PantryRobot(composeTestRule)

        // Navigate to pantry screen
        homeRobot.waitForHomeScreen(LONG_TIMEOUT)
        // Pantry navigation path depends on UI implementation
    }

    /**
     * Test 10.1: Add Pantry Items
     *
     * Steps:
     * 1. Navigate to Pantry screen
     * 2. Tap "Add Item" button
     * 3. Enter: Rice, Grains category, 2 kg
     * 4. Save item
     * 5. Verify item appears in list
     *
     * Test Variations:
     * - Add item with expiry date
     * - Add multiple items
     * - Delete item
     */
    @Test
    fun test_10_1_addPantryItems() {
        pantryRobot.waitForPantryScreen()
        pantryRobot.assertPantryScreenDisplayed()

        // Add Rice item
        pantryRobot.addItem(TestDataFactory.PantryItems.rice)

        // Verify item appears
        pantryRobot.assertItemDisplayed("Rice")
        pantryRobot.assertItemWithQuantity("Rice", "2 kg")
    }

    /**
     * Test 10.2: Expiring Soon Section
     *
     * Steps:
     * 1. Add item with expiry in 2 days
     * 2. Verify item shows in "Expiring Soon" section
     * 3. Add item with past expiry
     * 4. Verify item shows in "Expired" section
     */
    @Test
    fun test_10_2_expiringSoonSection() {
        pantryRobot.waitForPantryScreen()

        // Add item expiring soon (2 days)
        pantryRobot.addItem(TestDataFactory.PantryItems.milk)

        // Verify in expiring soon section
        pantryRobot.assertExpiringSoonSectionDisplayed()
        pantryRobot.assertItemExpiringSoon("Milk")

        // Add expired item
        pantryRobot.addItem(TestDataFactory.PantryItems.yogurt)

        // Verify in expired section
        pantryRobot.assertExpiredSectionDisplayed()
        pantryRobot.assertItemExpired("Yogurt")
    }

    /**
     * Test: Delete pantry item
     */
    @Test
    fun deletePantryItem() {
        pantryRobot.waitForPantryScreen()

        // Add item
        pantryRobot.addItem(TestDataFactory.PantryItems.rice)
        pantryRobot.assertItemDisplayed("Rice")

        // Delete item
        pantryRobot.deleteItem("Rice")

        // Verify empty state or item removed
        // Note: May show empty state if no other items
    }

    /**
     * Test: Edit pantry item
     */
    @Test
    fun editPantryItem() {
        pantryRobot.waitForPantryScreen()

        // Add item
        pantryRobot.addItem(TestDataFactory.PantryItems.rice)

        // Edit item
        pantryRobot.tapItem("Rice")

        // Edit sheet should open
        waitFor(ANIMATION_DURATION)
    }

    /**
     * Test: Search pantry items
     */
    @Test
    fun searchPantryItems() {
        pantryRobot.waitForPantryScreen()

        // Add items
        pantryRobot.addItem(TestDataFactory.PantryItems.rice)
        pantryRobot.addItem(TestDataFactory.PantryItems.milk)

        // Search
        pantryRobot.searchItem("Rice")
        pantryRobot.assertItemDisplayed("Rice")

        // Clear search
        pantryRobot.clearSearch()
    }

    /**
     * Test: Categories display correctly
     */
    @Test
    fun categoriesDisplay() {
        pantryRobot.waitForPantryScreen()

        // Add items in different categories
        pantryRobot.addItem(TestDataFactory.PantryItems.rice)
        pantryRobot.addItem(TestDataFactory.PantryItems.milk)

        // Verify categories
        pantryRobot.assertCategoryDisplayed("Grains")
        pantryRobot.assertCategoryDisplayed("Dairy")
    }

    /**
     * Test: Cancel adding item
     */
    @Test
    fun cancelAddingItem() {
        pantryRobot.waitForPantryScreen()

        pantryRobot.tapAddItem()
        pantryRobot.enterItemName("Test")
        pantryRobot.cancelAddItem()

        // Item should not be added
    }
}
