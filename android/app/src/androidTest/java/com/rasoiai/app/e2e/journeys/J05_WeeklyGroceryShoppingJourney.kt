package com.rasoiai.app.e2e.journeys

import android.util.Log
import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.GroceryRobot
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.util.BackendTestHelper
import com.rasoiai.app.e2e.util.JourneyStepLogger
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * J05: Weekly Grocery Shopping (single Activity session)
 *
 * Scenario: User views meal plan, navigates to grocery list, interacts with
 * categories and items, checks off purchases, and verifies backend data.
 *
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J05_WeeklyGroceryShoppingJourney
 * ```
 */
@HiltAndroidTest
class J05_WeeklyGroceryShoppingJourney : BaseE2ETest() {

    private lateinit var homeRobot: HomeRobot
    private lateinit var groceryRobot: GroceryRobot
    private val logger = JourneyStepLogger("J05")

    @Before
    override fun setUp() {
        super.setUp()
        setUpAuthenticatedState()
        homeRobot = HomeRobot(composeTestRule)
        groceryRobot = GroceryRobot(composeTestRule)
    }

    @Test
    fun weeklyGroceryShoppingJourney() {
        val totalSteps = 10

        try {
            logger.step(1, totalSteps, "Home screen loads") {
                homeRobot.waitForHomeScreen(HOME_SCREEN_TIMEOUT_MS)
                homeRobot.assertHomeScreenDisplayed()
                homeRobot.waitForMealListToLoad(MEAL_DATA_TIMEOUT_MS)
            }

            logger.step(2, totalSteps, "Navigate to Grocery with performance timing") {
                val startTime = System.currentTimeMillis()
                homeRobot.navigateToGrocery()
                groceryRobot.waitForGroceryScreen()
                groceryRobot.assertGroceryScreenDisplayed()
                val loadTime = System.currentTimeMillis() - startTime
                Log.i("J05", "Grocery screen load time: ${loadTime}ms")
                assertTrue(
                    "Grocery screen load took ${loadTime}ms, expected under 3000ms",
                    loadTime < 3000
                )
            }

            logger.step(3, totalSteps, "Verify grocery list is not empty") {
                groceryRobot.assertItemCountAtLeast(1)
            }

            logger.step(4, totalSteps, "Verify items count text is visible") {
                groceryRobot.assertItemsCountVisible()
            }

            logger.step(5, totalSteps, "Verify at least one category is displayed") {
                // Indian meal plans always have spices; try common categories
                val commonCategories = listOf("spices", "vegetables", "dairy", "grains", "pulses", "oils")
                var foundCategory: String? = null
                for (category in commonCategories) {
                    try {
                        groceryRobot.assertCategoryDisplayed(category)
                        foundCategory = category
                        Log.i("J05", "Found category: $category")
                        break
                    } catch (e: Throwable) {
                        Log.d("J05", "Category '$category' not found, trying next")
                    }
                }
                assertNotNull(
                    "Expected at least one common grocery category (spices, vegetables, dairy, grains, pulses, oils) to be displayed",
                    foundCategory
                )
            }

            logger.step(6, totalSteps, "Toggle a category to collapse/expand") {
                // Find a displayed category and toggle it
                val categoriesToTry = listOf("spices", "vegetables", "dairy", "grains", "pulses")
                var toggled = false
                for (category in categoriesToTry) {
                    try {
                        groceryRobot.toggleCategory(category)
                        Log.i("J05", "Toggled category: $category (collapsed)")
                        // Toggle back to expand
                        groceryRobot.toggleCategory(category)
                        Log.i("J05", "Toggled category: $category (expanded)")
                        toggled = true
                        break
                    } catch (e: Throwable) {
                        Log.d("J05", "Could not toggle '$category', trying next")
                    }
                }
                assertTrue("Expected to toggle at least one category", toggled)
            }

            logger.step(7, totalSteps, "Check off a grocery item by name") {
                // Find an item text to check off. Grocery items contain ingredient names.
                // We look for common Indian cooking items that are almost always present.
                val itemsToTry = listOf("Salt", "Oil", "Onion", "Cumin", "Turmeric", "Rice", "Garam Masala", "Garlic")
                var checkedItem: String? = null
                for (item in itemsToTry) {
                    try {
                        groceryRobot.checkItemByName(item)
                        checkedItem = item
                        Log.i("J05", "Checked off item: $item")
                        break
                    } catch (e: Throwable) {
                        Log.d("J05", "Item '$item' not found or not clickable, trying next")
                    }
                }
                assertNotNull(
                    "Expected to check off at least one common grocery item",
                    checkedItem
                )
            }

            logger.step(8, totalSteps, "Verify WhatsApp share button is available") {
                groceryRobot.assertWhatsAppShareDisplayed()
            }

            logger.step(9, totalSteps, "Verify backend grocery endpoint returns data") {
                val authToken = runBlocking { userPreferencesDataStore.accessToken.first() }
                assertNotNull("Auth token should be available", authToken)
                val groceryResponse = BackendTestHelper.getWithRetry(
                    baseUrl = BACKEND_BASE_URL,
                    path = "/api/v1/grocery",
                    authToken = authToken
                )
                assertNotNull(
                    "Backend /api/v1/grocery should return data for authenticated user",
                    groceryResponse
                )
                Log.i("J05", "Backend grocery response length: ${groceryResponse!!.length} chars")
            }

            logger.step(10, totalSteps, "Return to Home") {
                homeRobot.navigateToHome()
                homeRobot.assertHomeScreenDisplayed()
            }
        } finally {
            logger.printSummary()
        }
    }
}
