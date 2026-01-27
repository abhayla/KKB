package com.rasoiai.app.e2e.robots

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import com.rasoiai.app.e2e.base.waitUntilNodeWithTagExists
import com.rasoiai.app.presentation.common.TestTags
import com.rasoiai.domain.model.MealType
import java.time.DayOfWeek

/**
 * Robot for Home screen interactions.
 * Handles week view, meal cards, and navigation.
 */
class HomeRobot(private val composeTestRule: ComposeContentTestRule) {

    /**
     * Wait for home screen to be displayed.
     */
    fun waitForHomeScreen(timeoutMillis: Long = 5000) = apply {
        composeTestRule.waitUntilNodeWithTagExists(TestTags.HOME_SCREEN, timeoutMillis)
    }

    /**
     * Assert home screen is displayed.
     */
    fun assertHomeScreenDisplayed() = apply {
        composeTestRule.onNodeWithTag(TestTags.HOME_SCREEN).assertIsDisplayed()
    }

    /**
     * Assert week selector is displayed.
     */
    fun assertWeekSelectorDisplayed() = apply {
        composeTestRule.onNodeWithTag(TestTags.HOME_WEEK_SELECTOR).assertIsDisplayed()
    }

    // ===================== Day Selection =====================

    /**
     * Select a specific day.
     */
    fun selectDay(day: DayOfWeek) = apply {
        val dayTag = day.name.lowercase()
        composeTestRule.onNodeWithTag("${TestTags.HOME_DAY_TAB_PREFIX}$dayTag").performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Assert day is selected.
     */
    fun assertDaySelected(day: DayOfWeek) = apply {
        val dayTag = day.name.lowercase()
        composeTestRule.onNodeWithTag("${TestTags.HOME_DAY_TAB_PREFIX}$dayTag").assertIsSelected()
    }

    /**
     * Swipe to next day.
     */
    fun swipeToNextDay() = apply {
        composeTestRule.onNodeWithTag(TestTags.HOME_WEEK_SELECTOR)
            .performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
    }

    /**
     * Swipe to previous day.
     */
    fun swipeToPreviousDay() = apply {
        composeTestRule.onNodeWithTag(TestTags.HOME_WEEK_SELECTOR)
            .performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()
    }

    // ===================== Meal Cards =====================

    /**
     * Assert meal card is displayed.
     */
    fun assertMealCardDisplayed(mealType: MealType) = apply {
        val mealTag = mealType.name.lowercase()
        composeTestRule.onNodeWithTag("${TestTags.MEAL_CARD_PREFIX}$mealTag").assertIsDisplayed()
    }

    /**
     * Assert all meal cards displayed for a day.
     */
    fun assertAllMealCardsDisplayed() = apply {
        assertMealCardDisplayed(MealType.BREAKFAST)
        assertMealCardDisplayed(MealType.LUNCH)
        assertMealCardDisplayed(MealType.DINNER)
        assertMealCardDisplayed(MealType.SNACKS)
    }

    /**
     * Tap on a meal card to navigate to recipe detail.
     */
    fun tapMealCard(mealType: MealType) = apply {
        val mealTag = mealType.name.lowercase()
        composeTestRule.onNodeWithTag("${TestTags.MEAL_CARD_PREFIX}$mealTag")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Long press on a meal card to show options.
     */
    fun longPressMealCard(mealType: MealType) = apply {
        val mealTag = mealType.name.lowercase()
        composeTestRule.onNodeWithTag("${TestTags.MEAL_CARD_PREFIX}$mealTag")
            .performTouchInput { longClick() }
        composeTestRule.waitForIdle()
    }

    /**
     * Tap lock button on a meal.
     */
    fun tapLockMeal(mealType: MealType) = apply {
        val mealTag = mealType.name.lowercase()
        composeTestRule.onNodeWithTag("${TestTags.MEAL_LOCK_BUTTON_PREFIX}$mealTag").performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Tap swap button on a meal.
     */
    fun tapSwapMeal(mealType: MealType) = apply {
        val mealTag = mealType.name.lowercase()
        composeTestRule.onNodeWithTag("${TestTags.MEAL_SWAP_BUTTON_PREFIX}$mealTag").performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Assert meal is locked.
     */
    fun assertMealLocked(mealType: MealType) = apply {
        // Look for lock icon or locked state indicator
        val mealTag = mealType.name.lowercase()
        composeTestRule.onNodeWithTag("${TestTags.MEAL_CARD_PREFIX}${mealTag}_locked")
            .assertExists()
    }

    /**
     * Assert recipe name is displayed on meal card.
     */
    fun assertRecipeNameOnCard(recipeName: String) = apply {
        composeTestRule.onNodeWithText(recipeName).assertIsDisplayed()
    }

    // ===================== Festival Badge =====================

    /**
     * Assert festival badge is displayed.
     */
    fun assertFestivalBadgeDisplayed(festivalName: String) = apply {
        composeTestRule.onNodeWithText(festivalName, substring = true).assertIsDisplayed()
    }

    // ===================== Bottom Navigation =====================

    /**
     * Assert bottom navigation is displayed.
     */
    fun assertBottomNavDisplayed() = apply {
        composeTestRule.onNodeWithTag(TestTags.BOTTOM_NAV).assertIsDisplayed()
    }

    /**
     * Assert HOME is selected in bottom nav.
     */
    fun assertHomeNavSelected() = apply {
        composeTestRule.onNodeWithTag(TestTags.BOTTOM_NAV_HOME).assertIsSelected()
    }

    /**
     * Navigate to Grocery via bottom nav.
     */
    fun navigateToGrocery() = apply {
        composeTestRule.onNodeWithTag(TestTags.BOTTOM_NAV_GROCERY).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Navigate to Chat via bottom nav.
     */
    fun navigateToChat() = apply {
        composeTestRule.onNodeWithTag(TestTags.BOTTOM_NAV_CHAT).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Navigate to Favorites via bottom nav.
     */
    fun navigateToFavorites() = apply {
        composeTestRule.onNodeWithTag(TestTags.BOTTOM_NAV_FAVORITES).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Navigate to Stats via bottom nav.
     */
    fun navigateToStats() = apply {
        composeTestRule.onNodeWithTag(TestTags.BOTTOM_NAV_STATS).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Navigate to Home via bottom nav.
     */
    fun navigateToHome() = apply {
        composeTestRule.onNodeWithTag(TestTags.BOTTOM_NAV_HOME).performClick()
        composeTestRule.waitForIdle()
    }

    // ===================== Swap Sheet =====================

    /**
     * Select alternative recipe from swap sheet.
     */
    fun selectSwapAlternative(recipeName: String) = apply {
        composeTestRule.onNodeWithText(recipeName)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Dismiss swap sheet.
     */
    fun dismissSwapSheet() = apply {
        // Tap outside or press back
        composeTestRule.onNodeWithText("Cancel", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }
}
