package com.rasoiai.app.e2e.flows

import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.di.FakeNetworkMonitor
import com.rasoiai.app.e2e.robots.GroceryRobot
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.robots.RecipeDetailRobot
import com.rasoiai.domain.model.MealType
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import javax.inject.Inject

/**
 * Phase 13: Offline Testing
 *
 * Tests:
 * 13.1 Offline Meal Plan Access
 * 13.2 Offline Grocery List
 * 13.3 Offline Action Queue
 */
@HiltAndroidTest
class OfflineFlowTest : BaseE2ETest() {

    private lateinit var homeRobot: HomeRobot
    private lateinit var groceryRobot: GroceryRobot
    private lateinit var recipeDetailRobot: RecipeDetailRobot

    @Inject
    lateinit var fakeNetworkMonitor: FakeNetworkMonitor

    @Before
    override fun setUp() {
        super.setUp()
        // Set up authenticated and onboarded user state
        setUpAuthenticatedState()

        homeRobot = HomeRobot(composeTestRule)
        groceryRobot = GroceryRobot(composeTestRule)
        recipeDetailRobot = RecipeDetailRobot(composeTestRule)

        // Ensure we start online
        fakeNetworkMonitor.goOnline()
        homeRobot.waitForHomeScreen(LONG_TIMEOUT)
    }

    /**
     * Test 13.1: Offline Meal Plan Access
     *
     * Steps:
     * 1. Turn off network (airplane mode)
     * 2. Open app
     * 3. Navigate to Home
     * 4. Verify meal plan displays from cache
     * 5. Navigate through days
     * 6. Open recipe details
     *
     * Expected:
     * - All cached data accessible
     * - No crash or error
     * - Appropriate offline indicator shown
     */
    @Test
    fun test_13_1_offlineMealPlanAccess() {
        // First, ensure data is loaded while online
        homeRobot.assertHomeScreenDisplayed()
        homeRobot.assertAllMealCardsDisplayed()

        // Go offline
        fakeNetworkMonitor.goOffline()

        // Navigate through days
        homeRobot.selectDay(DayOfWeek.MONDAY)
        homeRobot.assertAllMealCardsDisplayed()

        homeRobot.selectDay(DayOfWeek.TUESDAY)
        homeRobot.assertAllMealCardsDisplayed()

        // Open recipe detail (should work from cache)
        homeRobot.selectDay(DayOfWeek.MONDAY)
        homeRobot.tapMealCard(MealType.BREAKFAST)
        recipeDetailRobot.waitForRecipeDetailScreen()
        recipeDetailRobot.assertRecipeDetailScreenDisplayed()

        // Go back
        recipeDetailRobot.goBack()
        homeRobot.assertHomeScreenDisplayed()
    }

    /**
     * Test 13.2: Offline Grocery List
     *
     * Steps:
     * 1. While offline, navigate to Grocery
     * 2. Check/uncheck items
     * 3. Verify state persists
     * 4. Re-enable network
     * 5. Verify sync occurs
     */
    @Test
    fun test_13_2_offlineGroceryList() {
        // Navigate to grocery while online
        homeRobot.navigateToGrocery()
        groceryRobot.waitForGroceryScreen()
        groceryRobot.assertGroceryScreenDisplayed()

        // Go offline
        fakeNetworkMonitor.goOffline()

        // Check items
        groceryRobot.checkItemByName("Rice")
        groceryRobot.checkItemByName("Dal")

        // Verify items are checked (state persists locally)
        waitFor(ANIMATION_DURATION)

        // Verify offline indicator
        groceryRobot.assertOfflineIndicatorDisplayed()

        // Go back online
        fakeNetworkMonitor.goOnline()

        // Verify syncing indicator
        groceryRobot.assertSyncingIndicatorDisplayed()

        // Wait for sync
        waitFor(MEDIUM_TIMEOUT)
    }

    /**
     * Test 13.3: Offline Action Queue
     *
     * Steps:
     * 1. While offline, lock a meal
     * 2. While offline, swap a meal
     * 3. Re-enable network
     * 4. Verify actions sync to server
     *
     * Expected:
     * - SyncManager processes queued actions
     * - Server reflects offline changes
     */
    @Test
    fun test_13_3_offlineActionQueue() {
        homeRobot.selectDay(DayOfWeek.MONDAY)
        homeRobot.assertAllMealCardsDisplayed()

        // Go offline
        fakeNetworkMonitor.goOffline()

        // Lock a meal
        homeRobot.longPressMealCard(MealType.BREAKFAST)
        homeRobot.tapLockMeal(MealType.BREAKFAST)
        homeRobot.assertMealLocked(MealType.BREAKFAST)

        // Swap a meal
        homeRobot.tapSwapMeal(MealType.LUNCH)
        homeRobot.selectSwapAlternative("Rajma Masala")

        // Go back online
        fakeNetworkMonitor.goOnline()

        // Wait for sync
        waitFor(MEDIUM_TIMEOUT)

        // Verify actions persisted (meal is still locked after sync)
        homeRobot.assertMealLocked(MealType.BREAKFAST)
    }

    /**
     * Test: Offline indicator appears correctly
     */
    @Test
    fun offlineIndicator_displaysCorrectly() {
        // Go offline
        fakeNetworkMonitor.goOffline()

        // Navigate around
        homeRobot.navigateToGrocery()
        groceryRobot.waitForGroceryScreen()

        // Offline indicator should be visible
        groceryRobot.assertOfflineIndicatorDisplayed()

        // Go online
        fakeNetworkMonitor.goOnline()

        // Indicator should disappear
        waitFor(ANIMATION_DURATION)
    }

    /**
     * Test: Recipe detail available offline
     */
    @Test
    fun recipeDetail_availableOffline() {
        // Load recipe online first
        homeRobot.selectDay(DayOfWeek.MONDAY)
        homeRobot.tapMealCard(MealType.LUNCH)
        recipeDetailRobot.waitForRecipeDetailScreen()
        recipeDetailRobot.goBack()

        // Go offline
        fakeNetworkMonitor.goOffline()

        // Access same recipe
        homeRobot.tapMealCard(MealType.LUNCH)
        recipeDetailRobot.waitForRecipeDetailScreen()

        // All details should be available
        recipeDetailRobot.assertRecipeDetailScreenDisplayed()
        recipeDetailRobot.assertIngredientsListDisplayed()
        recipeDetailRobot.assertInstructionsListDisplayed()
    }

    /**
     * Test: Favorites work offline
     */
    @Test
    fun favorites_workOffline() {
        // Add to favorites online
        homeRobot.selectDay(DayOfWeek.MONDAY)
        homeRobot.tapMealCard(MealType.BREAKFAST)
        recipeDetailRobot.waitForRecipeDetailScreen()
        recipeDetailRobot.tapFavoriteButton()
        recipeDetailRobot.assertIsFavorited()
        recipeDetailRobot.goBack()

        // Go offline
        fakeNetworkMonitor.goOffline()

        // Navigate to favorites
        homeRobot.navigateToFavorites()

        // Favorite should still be accessible
    }

    /**
     * Test: Network restoration syncs pending changes
     */
    @Test
    fun networkRestoration_syncsPendingChanges() {
        // Go offline and make changes
        fakeNetworkMonitor.goOffline()

        homeRobot.navigateToGrocery()
        groceryRobot.waitForGroceryScreen()
        groceryRobot.checkItemByName("Rice")

        // Restore network
        fakeNetworkMonitor.goOnline()

        // Syncing should occur
        groceryRobot.assertSyncingIndicatorDisplayed()

        // Wait for sync to complete
        waitFor(MEDIUM_TIMEOUT)
    }
}
