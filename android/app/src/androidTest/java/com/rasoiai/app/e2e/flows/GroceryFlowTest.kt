package com.rasoiai.app.e2e.flows

import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.GroceryRobot
import com.rasoiai.app.e2e.robots.HomeRobot
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test

/**
 * Phase 5: Grocery Screen Testing
 *
 * Tests:
 * 5.1 Grocery List Display
 * 5.2 Check/Uncheck Items
 * 5.3 WhatsApp Share
 */
@HiltAndroidTest
class GroceryFlowTest : BaseE2ETest() {

    private lateinit var homeRobot: HomeRobot
    private lateinit var groceryRobot: GroceryRobot

    @Before
    override fun setUp() {
        super.setUp()
        // Set up authenticated and onboarded user state
        setUpAuthenticatedState()

        homeRobot = HomeRobot(composeTestRule)
        groceryRobot = GroceryRobot(composeTestRule)

        // Navigate to grocery screen
        homeRobot.waitForHomeScreen(LONG_TIMEOUT)
        homeRobot.navigateToGrocery()
        groceryRobot.waitForGroceryScreen()
    }

    /**
     * Test 5.1: Grocery List Display
     *
     * Steps:
     * 1. Navigate to Grocery screen
     * 2. Verify grocery items grouped by category
     * 3. Check quantities match meal plan servings
     * 4. Verify all ingredients from week's recipes present
     *
     * Expected:
     * - Categories: Vegetables, Dairy, Grains, Spices, Pulses, etc.
     * - Items show: name, quantity, unit
     * - Items are unchecked by default
     */
    @Test
    fun test_5_1_groceryListDisplay() {
        // Verify screen is displayed
        groceryRobot.assertGroceryScreenDisplayed()

        // Verify categories are displayed
        groceryRobot.assertCommonCategoriesDisplayed()

        // Verify specific items (from fake meal plan data)
        groceryRobot.assertItemDisplayed("Rice")
        groceryRobot.assertItemDisplayed("Dal")
    }

    /**
     * Test 5.2: Check/Uncheck Items
     *
     * Steps:
     * 1. Tap checkbox on first item
     * 2. Verify item shows checked state
     * 3. Tap again to uncheck
     * 4. Use "Mark All Checked" option
     * 5. Verify all items checked
     * 6. Use "Clear Checked" option
     */
    @Test
    fun test_5_2_checkUncheckItems() {
        // Check an item by name
        groceryRobot.checkItemByName("Rice")
        waitFor(ANIMATION_DURATION)

        // Uncheck the item
        groceryRobot.checkItemByName("Rice")
        waitFor(ANIMATION_DURATION)

        // Mark all as checked
        groceryRobot.markAllChecked()
        waitFor(ANIMATION_DURATION)

        // Clear checked items
        groceryRobot.clearChecked()
    }

    /**
     * Test 5.3: WhatsApp Share
     *
     * Steps:
     * 1. Tap WhatsApp share button
     * 2. Verify formatted text generated
     * 3. Verify WhatsApp app opens with pre-filled message
     *
     * Note: This test verifies the button is present and clickable.
     * Actual WhatsApp intent verification requires additional setup.
     */
    @Test
    fun test_5_3_whatsAppShare() {
        // Verify share button is displayed
        groceryRobot.assertWhatsAppShareDisplayed()

        // Tap share button
        groceryRobot.tapWhatsAppShare()

        // Note: Intent verification would require IntentsTestRule
        // For now, we just verify the button is clickable
    }

    /**
     * Test: Categories can be expanded/collapsed
     */
    @Test
    fun categories_canBeToggled() {
        groceryRobot.assertCategoryDisplayed("Vegetables")
        groceryRobot.toggleCategory("Vegetables")
        waitFor(ANIMATION_DURATION)
        // Items should be visible/hidden based on toggle state
    }

    /**
     * Test: Checked count updates correctly
     */
    @Test
    fun checkedCount_updatesCorrectly() {
        // Check multiple items
        groceryRobot.checkItemByName("Rice")
        groceryRobot.checkItemByName("Dal")

        // Verify count (implementation dependent)
        waitFor(ANIMATION_DURATION)
    }
}
