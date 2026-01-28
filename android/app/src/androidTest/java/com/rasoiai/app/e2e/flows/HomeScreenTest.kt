package com.rasoiai.app.e2e.flows

import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.ChatRobot
import com.rasoiai.app.e2e.robots.FavoritesRobot
import com.rasoiai.app.e2e.robots.GroceryRobot
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.robots.RecipeDetailRobot
import com.rasoiai.app.e2e.robots.StatsRobot
import com.rasoiai.domain.model.MealType
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek

/**
 * Phase 4: Home Screen Testing
 *
 * Tests:
 * 4.1 Initial Load & Week View
 * 4.2 Meal Card Interactions
 * 4.3 Recipe Detail Navigation
 * 4.4 Navigation to Other Screens
 */
@HiltAndroidTest
class HomeScreenTest : BaseE2ETest() {

    private lateinit var homeRobot: HomeRobot
    private lateinit var recipeDetailRobot: RecipeDetailRobot
    private lateinit var groceryRobot: GroceryRobot
    private lateinit var chatRobot: ChatRobot
    private lateinit var favoritesRobot: FavoritesRobot
    private lateinit var statsRobot: StatsRobot

    @Before
    override fun setUp() {
        super.setUp()
        // Set up authenticated and onboarded user state
        setUpAuthenticatedState()

        homeRobot = HomeRobot(composeTestRule)
        recipeDetailRobot = RecipeDetailRobot(composeTestRule)
        groceryRobot = GroceryRobot(composeTestRule)
        chatRobot = ChatRobot(composeTestRule)
        favoritesRobot = FavoritesRobot(composeTestRule)
        statsRobot = StatsRobot(composeTestRule)

        // Navigate to home screen
        homeRobot.waitForHomeScreen(LONG_TIMEOUT)
    }

    /**
     * Test 4.1: Initial Load & Week View
     *
     * Steps:
     * 1. Verify Home screen displays with bottom navigation
     * 2. Check current week dates shown in header
     * 3. Verify 7-day horizontal scroll with day names
     * 4. Tap each day to view meals
     *
     * Expected:
     * - Current day is highlighted/selected
     * - Each day shows: Breakfast, Lunch, Dinner, Snacks
     * - Bottom nav shows HOME selected
     */
    @Test
    fun test_4_1_weekView_displaysCorrectly() {
        // Step 1: Verify home screen with bottom nav
        homeRobot.assertHomeScreenDisplayed()
        homeRobot.assertBottomNavDisplayed()
        homeRobot.assertHomeNavSelected()

        // Step 2 & 3: Verify week selector
        homeRobot.assertWeekSelectorDisplayed()

        // Step 4: Navigate through days
        for (day in DayOfWeek.values()) {
            homeRobot.selectDay(day)
            homeRobot.assertAllMealCardsDisplayed()
        }
    }

    /**
     * Test 4.2: Meal Card Interactions
     *
     * Steps:
     * 1. Long-press on a meal card (e.g., Monday Breakfast)
     * 2. Verify lock icon appears
     * 3. Tap lock to lock the meal
     * 4. Verify meal shows locked state
     * 5. Tap swap icon on another meal
     * 6. Verify swap suggestions sheet appears
     * 7. Select alternative recipe
     * 8. Verify meal card updates
     */
    @Test
    fun test_4_2_mealCardInteractions() {
        // Select Monday
        homeRobot.selectDay(DayOfWeek.MONDAY)
        homeRobot.assertAllMealCardsDisplayed()

        // Steps 1-4: Lock a meal
        homeRobot.longPressMealCard(MealType.BREAKFAST)
        homeRobot.tapLockMeal(MealType.BREAKFAST)
        homeRobot.assertMealLocked(MealType.BREAKFAST)

        // Steps 5-8: Swap another meal
        homeRobot.tapSwapMeal(MealType.LUNCH)
        // Swap sheet should appear with alternatives
        waitFor(ANIMATION_DURATION)
        homeRobot.selectSwapAlternative("Rajma Masala") // Alternative recipe
    }

    /**
     * Test 4.3: Recipe Detail Navigation
     *
     * Steps:
     * 1. Tap on any meal card
     * 2. Verify navigation to Recipe Detail screen
     * 3. Verify recipe info: name, image, time, difficulty
     * 4. Check ingredients list with quantities
     * 5. Check step-by-step instructions
     * 6. Verify nutrition info displayed
     * 7. Tap back to return to Home
     */
    @Test
    fun test_4_3_recipeDetailNavigation() {
        // Select a day and tap on breakfast
        homeRobot.selectDay(DayOfWeek.MONDAY)
        homeRobot.tapMealCard(MealType.BREAKFAST)

        // Verify navigation to recipe detail
        recipeDetailRobot.waitForRecipeDetailScreen()
        recipeDetailRobot.assertRecipeDetailScreenDisplayed()

        // Verify recipe info
        recipeDetailRobot.assertIngredientsListDisplayed()
        recipeDetailRobot.assertInstructionsListDisplayed()
        recipeDetailRobot.assertNutritionPanelDisplayed()

        // Go back to home
        recipeDetailRobot.goBack()
        homeRobot.waitForHomeScreen()
        homeRobot.assertHomeScreenDisplayed()
    }

    /**
     * Test 4.4: Navigation to Other Screens
     *
     * Steps:
     * 1. Tap GROCERY in bottom nav → verify Grocery screen
     * 2. Tap CHAT in bottom nav → verify Chat screen
     * 3. Tap FAVORITES in bottom nav → verify Favorites screen
     * 4. Tap STATS in bottom nav → verify Stats screen
     * 5. Return to HOME
     *
     * Expected:
     * - Each screen loads without crash
     * - Bottom nav highlights current selection
     * - State preserved when switching tabs
     */
    @Test
    fun test_4_4_bottomNavigation() {
        // Navigate to Grocery
        homeRobot.navigateToGrocery()
        groceryRobot.waitForGroceryScreen()
        groceryRobot.assertGroceryScreenDisplayed()

        // Navigate to Chat
        homeRobot.navigateToChat()
        chatRobot.waitForChatScreen()
        chatRobot.assertChatScreenDisplayed()

        // Navigate to Favorites
        homeRobot.navigateToFavorites()
        favoritesRobot.waitForFavoritesScreen()
        favoritesRobot.assertFavoritesScreenDisplayed()

        // Navigate to Stats
        homeRobot.navigateToStats()
        statsRobot.waitForStatsScreen()
        statsRobot.assertStatsScreenDisplayed()

        // Return to Home
        homeRobot.navigateToHome()
        homeRobot.waitForHomeScreen()
        homeRobot.assertHomeScreenDisplayed()
        homeRobot.assertHomeNavSelected()
    }

    /**
     * Test: Week view navigation via swipe
     */
    @Test
    fun weekView_swipeNavigation() {
        homeRobot.selectDay(DayOfWeek.MONDAY)
        homeRobot.assertDaySelected(DayOfWeek.MONDAY)

        homeRobot.swipeToNextDay()
        homeRobot.assertDaySelected(DayOfWeek.TUESDAY)

        homeRobot.swipeToPreviousDay()
        homeRobot.assertDaySelected(DayOfWeek.MONDAY)
    }

    /**
     * Test: All meal types are displayed for each day
     */
    @Test
    fun eachDay_displaysAllMealTypes() {
        for (day in DayOfWeek.values()) {
            homeRobot.selectDay(day)
            homeRobot.assertMealCardDisplayed(MealType.BREAKFAST)
            homeRobot.assertMealCardDisplayed(MealType.LUNCH)
            homeRobot.assertMealCardDisplayed(MealType.DINNER)
            homeRobot.assertMealCardDisplayed(MealType.SNACKS)
        }
    }
}
