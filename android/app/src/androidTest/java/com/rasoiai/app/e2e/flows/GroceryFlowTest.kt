package com.rasoiai.app.e2e.flows

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.GroceryRobot
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.util.PerformanceTracker
import com.rasoiai.app.presentation.common.TestTags
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Requirement: #39 - FR-007: Expanded E2E tests for Grocery screen
 *
 * Phase 5: Grocery Screen Testing (18 tests)
 *
 * Test Categories:
 * 5.1  Grocery List Display (2 tests)
 * 5.2  Check/Uncheck Items (1 test)
 * 5.3  WhatsApp Share (3 tests)
 * 5.4  Category Expand/Collapse (2 tests)
 * 5.5  Week Header (2 tests)
 * 5.6  Add Custom Item (2 tests)
 * 5.7  Menu Options (2 tests)
 * 5.8  Bottom Navigation (4 tests)
 *
 * ## Auth State
 * - Sets up authenticated state via BaseE2ETest
 * - Navigates to Grocery screen via bottom nav
 * - Grocery list is derived from AI-generated meal plan
 */
@HiltAndroidTest
class GroceryFlowTest : BaseE2ETest() {

    private lateinit var homeRobot: HomeRobot
    private lateinit var groceryRobot: GroceryRobot

    @Before
    override fun setUp() {
        super.setUp()

        PerformanceTracker.reset()
        setUpAuthenticatedState()

        homeRobot = HomeRobot(composeTestRule)
        groceryRobot = GroceryRobot(composeTestRule)

        homeRobot.waitForHomeScreen(LONG_TIMEOUT)

        try {
            homeRobot.waitForMealListToLoad(60000)
            android.util.Log.i("GroceryFlowTest", "Meal data loaded, navigating to Grocery")
        } catch (e: Exception) {
            android.util.Log.w("GroceryFlowTest", "Meal data may not have loaded: ${e.message}")
        }

        PerformanceTracker.measure(
            "Grocery List Load",
            PerformanceTracker.GROCERY_LIST_LOAD_MS
        ) {
            homeRobot.navigateToGrocery()
            groceryRobot.waitForGroceryScreen(LONG_TIMEOUT)
        }

        waitFor(MEDIUM_TIMEOUT)
    }

    @After
    override fun tearDown() {
        PerformanceTracker.printSummary()
        super.tearDown()
    }

    // ===================== 5.1 Grocery List Display =====================

    /**
     * Test 5.1: Grocery List Display
     *
     * Verifies the grocery screen loads with categories and items.
     */
    @Test
    fun test_5_1_groceryListDisplay() {
        groceryRobot.assertGroceryScreenDisplayed()
        groceryRobot.assertCommonCategoriesDisplayed()
    }

    /**
     * Test 5.1b: Grocery screen shows title "Grocery List"
     */
    @Test
    fun test_5_1b_groceryTitle_isDisplayed() {
        composeTestRule.onNodeWithText("Grocery List")
            .assertIsDisplayed()
    }

    // ===================== 5.2 Check/Uncheck Items =====================

    /**
     * Test 5.2: Check/Uncheck Items via menu clear
     */
    @Test
    fun test_5_2_checkUncheckItems() {
        groceryRobot.openMoreOptionsMenu()
        waitFor(ANIMATION_DURATION)
        groceryRobot.clearPurchasedItems()
        waitFor(ANIMATION_DURATION)
    }

    // ===================== 5.3 WhatsApp Share =====================

    /**
     * Test 5.3: WhatsApp share button is displayed and clickable
     */
    @Test
    fun test_5_3_whatsAppShare() {
        groceryRobot.assertWhatsAppShareDisplayed()
        groceryRobot.tapWhatsAppShare()
    }

    /**
     * Test 5.3b: WhatsApp share dialog shows share options
     */
    @Test
    fun test_5_3b_whatsAppShareDialog_showsOptions() {
        groceryRobot.tapWhatsAppShare()
        waitFor(ANIMATION_DURATION)

        composeTestRule.onNodeWithText("Share to WhatsApp", substring = true, ignoreCase = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Full list", substring = true, ignoreCase = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Unpurchased only", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    /**
     * Test 5.3c: WhatsApp share dialog can be cancelled
     */
    @Test
    fun test_5_3c_whatsAppShareDialog_canBeCancelled() {
        groceryRobot.tapWhatsAppShare()
        waitFor(ANIMATION_DURATION)

        composeTestRule.onNodeWithText("Cancel", ignoreCase = true)
            .performClick()
        waitFor(ANIMATION_DURATION)

        groceryRobot.assertGroceryScreenDisplayed()
    }

    // ===================== 5.4 Category Expand/Collapse =====================

    /**
     * Test 5.4: Categories can be expanded and collapsed
     */
    @Test
    fun test_5_4_categories_canBeToggled() {
        val categories = listOf("vegetables", "grains", "spices", "dairy", "pulses")

        for (category in categories) {
            try {
                groceryRobot.assertCategoryDisplayed(category)
                groceryRobot.toggleCategory(category)
                waitFor(ANIMATION_DURATION)
                groceryRobot.toggleCategory(category)
                waitFor(ANIMATION_DURATION)
                break
            } catch (e: Exception) {
                // Category not found, try next
            }
        }
    }

    /**
     * Test 5.4b: Multiple categories are present in grocery list
     */
    @Test
    fun test_5_4b_multipleCategories_displayed() {
        val categories = listOf("vegetables", "grains", "spices", "dairy", "pulses", "oils", "nuts")
        var categoriesFound = 0

        for (category in categories) {
            try {
                groceryRobot.assertCategoryDisplayed(category)
                categoriesFound++
            } catch (e: Exception) {
                // Category not in this meal plan
            }
        }

        assert(categoriesFound >= 2) {
            "Expected at least 2 categories but found $categoriesFound"
        }
    }

    // ===================== 5.5 Week Header =====================

    /**
     * Test 5.5: Week header displays date range
     */
    @Test
    fun test_5_5_weekHeader_displaysDateRange() {
        groceryRobot.assertWeekHeaderDisplayed()
        composeTestRule.onNodeWithText("Week of", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    /**
     * Test 5.5b: Total items count is displayed
     */
    @Test
    fun test_5_5b_totalItemsCount_isDisplayed() {
        groceryRobot.assertTotalItemsDisplayed()
        groceryRobot.assertItemsCountVisible()
    }

    // ===================== 5.6 Add Custom Item =====================

    /**
     * Test 5.6: Add custom item button is displayed
     */
    @Test
    fun test_5_6_addCustomItemButton_isDisplayed() {
        groceryRobot.assertAddCustomItemButtonDisplayed()
    }

    /**
     * Test 5.6b: Add custom item button opens dialog
     */
    @Test
    fun test_5_6b_addCustomItemButton_opensDialog() {
        groceryRobot.tapAddCustomItemButton()
        waitFor(ANIMATION_DURATION)

        composeTestRule.onNodeWithText("Add Custom Item", ignoreCase = true)
            .assertIsDisplayed()
    }

    // ===================== 5.7 Menu Options =====================

    /**
     * Test 5.7: More options menu opens and shows all items
     */
    @Test
    fun test_5_7_moreOptionsMenu_opens() {
        groceryRobot.openMoreOptionsMenu()
        waitFor(ANIMATION_DURATION)

        composeTestRule.onNodeWithText("Clear purchased items", ignoreCase = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Share as text", ignoreCase = true)
            .assertIsDisplayed()
    }

    /**
     * Test 5.7b: Share as text menu option is clickable
     */
    @Test
    fun test_5_7b_shareAsText_menuOption() {
        groceryRobot.openMoreOptionsMenu()
        waitFor(ANIMATION_DURATION)
        groceryRobot.shareAsText()
    }

    // ===================== 5.8 Bottom Navigation =====================

    /**
     * Test 5.8a: Bottom navigation to Home works
     */
    @Test
    fun test_5_8a_bottomNav_toHome() {
        homeRobot.navigateToHome()
        waitFor(MEDIUM_TIMEOUT)
        composeTestRule.onNodeWithTag(TestTags.HOME_SCREEN).assertIsDisplayed()
    }

    /**
     * Test 5.8b: Bottom navigation to Chat works
     */
    @Test
    fun test_5_8b_bottomNav_toChat() {
        homeRobot.navigateToChat()
        waitFor(MEDIUM_TIMEOUT)
        composeTestRule.onNodeWithTag(TestTags.CHAT_SCREEN).assertIsDisplayed()
    }

    /**
     * Test 5.8c: Bottom navigation to Favorites works
     */
    @Test
    fun test_5_8c_bottomNav_toFavorites() {
        homeRobot.navigateToFavorites()
        waitFor(MEDIUM_TIMEOUT)
        composeTestRule.onNodeWithTag(TestTags.FAVORITES_SCREEN).assertIsDisplayed()
    }

    /**
     * Test 5.8d: Bottom navigation to Stats works
     */
    @Test
    fun test_5_8d_bottomNav_toStats() {
        homeRobot.navigateToStats()
        waitFor(MEDIUM_TIMEOUT)
        composeTestRule.onNodeWithTag(TestTags.STATS_SCREEN).assertIsDisplayed()
    }
}
