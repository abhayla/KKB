package com.rasoiai.app.e2e.robots

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import com.rasoiai.app.e2e.base.waitUntilNodeWithTagExists
import com.rasoiai.app.presentation.common.TestTags

/**
 * Robot for Recipe Detail screen interactions.
 * Handles recipe info, scaling, favorites, and cooking mode launch.
 */
class RecipeDetailRobot(private val composeTestRule: ComposeContentTestRule) {

    /**
     * Wait for recipe detail screen to be displayed.
     */
    fun waitForRecipeDetailScreen(timeoutMillis: Long = 5000) = apply {
        composeTestRule.waitUntilNodeWithTagExists(TestTags.RECIPE_DETAIL_SCREEN, timeoutMillis)
    }

    /**
     * Assert recipe detail screen is displayed.
     */
    fun assertRecipeDetailScreenDisplayed() = apply {
        composeTestRule.onNodeWithTag(TestTags.RECIPE_DETAIL_SCREEN).assertIsDisplayed()
    }

    // ===================== Recipe Info =====================

    /**
     * Assert recipe name is displayed.
     */
    fun assertRecipeNameDisplayed(name: String) = apply {
        composeTestRule.onNodeWithText(name, substring = true).assertIsDisplayed()
    }

    /**
     * Assert prep time is displayed.
     */
    fun assertPrepTimeDisplayed(minutes: Int) = apply {
        composeTestRule.onNodeWithText("$minutes min", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    /**
     * Assert difficulty is displayed.
     */
    fun assertDifficultyDisplayed(difficulty: String) = apply {
        composeTestRule.onNodeWithText(difficulty, substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    /**
     * Assert cuisine type is displayed.
     */
    fun assertCuisineDisplayed(cuisine: String) = apply {
        composeTestRule.onNodeWithText(cuisine, substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    // ===================== Servings =====================

    /**
     * Assert servings selector is displayed.
     */
    fun assertServingsSelectorDisplayed() = apply {
        composeTestRule.onNodeWithTag(TestTags.RECIPE_SERVINGS_SELECTOR).assertIsDisplayed()
    }

    /**
     * Increase servings.
     */
    fun increaseServings() = apply {
        composeTestRule.onNodeWithText("+", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Decrease servings.
     */
    fun decreaseServings() = apply {
        composeTestRule.onNodeWithText("-", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Set specific servings.
     */
    fun setServings(count: Int) = apply {
        // Tap on servings selector and select count
        composeTestRule.onNodeWithTag(TestTags.RECIPE_SERVINGS_SELECTOR).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("$count", substring = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Assert current servings count.
     */
    fun assertServingsCount(count: Int) = apply {
        composeTestRule.onNodeWithText("$count servings", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    // ===================== Ingredients =====================

    /**
     * Assert ingredients list is displayed.
     */
    fun assertIngredientsListDisplayed() = apply {
        composeTestRule.onNodeWithTag(TestTags.RECIPE_INGREDIENTS_LIST)
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * Assert ingredient is displayed.
     */
    fun assertIngredientDisplayed(ingredientName: String) = apply {
        composeTestRule.onNodeWithText(ingredientName, substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * Assert ingredient with quantity.
     */
    fun assertIngredientWithQuantity(ingredientName: String, quantity: String) = apply {
        composeTestRule.onNodeWithText(ingredientName, substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(quantity, substring = true).assertIsDisplayed()
    }

    /**
     * Assert ingredient quantity changed after scaling.
     */
    fun assertScaledIngredientQuantity(ingredientName: String, expectedQuantity: String) = apply {
        composeTestRule.onNodeWithText(ingredientName, substring = true).performScrollTo()
        composeTestRule.onNodeWithText(expectedQuantity, substring = true).assertIsDisplayed()
    }

    // ===================== Instructions =====================

    /**
     * Assert instructions list is displayed.
     */
    fun assertInstructionsListDisplayed() = apply {
        composeTestRule.onNodeWithTag(TestTags.RECIPE_INSTRUCTIONS_LIST)
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * Assert instruction step is displayed.
     */
    fun assertInstructionStepDisplayed(stepNumber: Int) = apply {
        composeTestRule.onNodeWithText("Step $stepNumber", substring = true, ignoreCase = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * Assert instruction text is displayed.
     */
    fun assertInstructionTextDisplayed(text: String) = apply {
        composeTestRule.onNodeWithText(text, substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    // ===================== Nutrition =====================

    /**
     * Assert nutrition panel is displayed.
     */
    fun assertNutritionPanelDisplayed() = apply {
        composeTestRule.onNodeWithText("Nutrition", substring = true, ignoreCase = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * Assert calorie count is displayed.
     */
    fun assertCaloriesDisplayed(calories: Int) = apply {
        composeTestRule.onNodeWithText("$calories", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("cal", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    /**
     * Assert protein is displayed.
     */
    fun assertProteinDisplayed(grams: Int) = apply {
        composeTestRule.onNodeWithText("${grams}g", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Protein", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    // ===================== Favorites =====================

    /**
     * Tap favorite button.
     */
    fun tapFavoriteButton() = apply {
        composeTestRule.onNodeWithTag(TestTags.RECIPE_FAVORITE_BUTTON).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Assert recipe is favorited.
     */
    fun assertIsFavorited() = apply {
        composeTestRule.onNodeWithTag(TestTags.RECIPE_FAVORITE_BUTTON).assertIsOn()
    }

    /**
     * Assert recipe is not favorited.
     */
    fun assertIsNotFavorited() = apply {
        composeTestRule.onNodeWithTag(TestTags.RECIPE_FAVORITE_BUTTON).assertIsOff()
    }

    // ===================== Cooking Mode =====================

    /**
     * Tap start cooking button.
     */
    fun tapStartCooking() = apply {
        composeTestRule.onNodeWithTag(TestTags.RECIPE_START_COOKING_BUTTON)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Assert start cooking button is displayed.
     */
    fun assertStartCookingDisplayed() = apply {
        composeTestRule.onNodeWithTag(TestTags.RECIPE_START_COOKING_BUTTON)
            .performScrollTo()
            .assertIsDisplayed()
    }

    // ===================== Navigation =====================

    /**
     * Go back to previous screen.
     */
    fun goBack() = apply {
        composeTestRule.onNodeWithText("Back", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    // ===================== Tips =====================

    /**
     * Assert cooking tip is displayed.
     */
    fun assertTipDisplayed(tipText: String) = apply {
        composeTestRule.onNodeWithText(tipText, substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    // ===================== Tags =====================

    /**
     * Assert dietary tag is displayed.
     */
    fun assertDietaryTagDisplayed(tag: String) = apply {
        composeTestRule.onNodeWithText(tag, substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }
}
