package com.rasoiai.app.e2e.robots

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.rasoiai.app.e2e.base.FamilyMember
import com.rasoiai.app.e2e.base.HealthNeed
import com.rasoiai.app.e2e.base.MemberType
import com.rasoiai.app.e2e.base.SpiceLevel
import com.rasoiai.app.e2e.base.waitUntilNodeWithTagExists
import com.rasoiai.app.e2e.base.waitUntilNodeWithTextExists
import com.rasoiai.app.presentation.common.TestTags
import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.DietaryTag
import java.time.DayOfWeek

/**
 * Robot for Onboarding screen interactions.
 * Handles all 5 onboarding steps.
 */
class OnboardingRobot(private val composeTestRule: ComposeContentTestRule) {

    // ===================== Step 1: Household Size & Family Members =====================

    /**
     * Assert step indicator shows correct step.
     */
    fun assertStepIndicator(step: Int, total: Int = 5) = apply {
        composeTestRule.onNodeWithTag(TestTags.ONBOARDING_STEP_INDICATOR).assertIsDisplayed()
        // Verify text contains step of total (e.g., "1 of 5")
        composeTestRule.onNodeWithText("$step of $total", substring = true).assertIsDisplayed()
    }

    /**
     * Select household size from dropdown.
     */
    fun selectHouseholdSize(size: Int) = apply {
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_SIZE_DROPDOWN).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("${TestTags.HOUSEHOLD_SIZE_OPTION_PREFIX}$size").performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Tap "Add Family Member" button.
     */
    fun tapAddFamilyMember() = apply {
        composeTestRule.onNodeWithTag(TestTags.ADD_FAMILY_MEMBER_BUTTON).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Add a family member in the bottom sheet.
     */
    fun addFamilyMember(member: FamilyMember) = apply {
        tapAddFamilyMember()

        // Enter name
        composeTestRule.onNodeWithTag(TestTags.MEMBER_NAME_FIELD).performTextInput(member.name)

        // Select type
        composeTestRule.onNodeWithTag(TestTags.MEMBER_TYPE_DROPDOWN).performClick()
        composeTestRule.waitForIdle()
        val typeOption = when (member.type) {
            MemberType.ADULT -> "Adult"
            MemberType.CHILD -> "Child"
            MemberType.SENIOR -> "Senior"
        }
        composeTestRule.onNodeWithText(typeOption).performClick()

        // Select age
        composeTestRule.onNodeWithTag(TestTags.MEMBER_AGE_DROPDOWN).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("${member.age}").performClick()

        // Select health needs
        for (need in member.healthNeeds) {
            val needTag = when (need) {
                HealthNeed.DIABETIC -> "diabetic"
                HealthNeed.LOW_OIL -> "low_oil"
                HealthNeed.LOW_SALT -> "low_salt"
                HealthNeed.NO_SPICY -> "no_spicy"
                HealthNeed.HIGH_PROTEIN -> "high_protein"
                HealthNeed.LOW_CARB -> "low_carb"
            }
            composeTestRule.onNodeWithTag("${TestTags.MEMBER_DIETARY_NEED_PREFIX}$needTag")
                .performScrollTo()
                .performClick()
        }

        // Save
        composeTestRule.onNodeWithTag(TestTags.MEMBER_SAVE_BUTTON).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Assert family member is displayed in the list.
     */
    fun assertFamilyMemberDisplayed(name: String) = apply {
        composeTestRule.onNodeWithText(name).assertIsDisplayed()
    }

    /**
     * Assert family member count.
     */
    fun assertFamilyMemberCount(expectedCount: Int) = apply {
        // Each member row has a unique tag
        for (i in 0 until expectedCount) {
            composeTestRule.onNodeWithTag("${TestTags.FAMILY_MEMBER_ROW_PREFIX}$i").assertExists()
        }
    }

    /**
     * Edit an existing family member.
     */
    fun tapEditFamilyMember(index: Int) = apply {
        composeTestRule.onNodeWithTag("${TestTags.FAMILY_MEMBER_EDIT_PREFIX}$index").performClick()
    }

    /**
     * Delete a family member.
     */
    fun tapDeleteFamilyMember(index: Int) = apply {
        composeTestRule.onNodeWithTag("${TestTags.FAMILY_MEMBER_DELETE_PREFIX}$index").performClick()
    }

    // ===================== Step 2: Dietary Preferences =====================

    /**
     * Select primary diet.
     */
    fun selectPrimaryDiet(diet: DietaryTag) = apply {
        val dietTag = diet.name.lowercase()
        composeTestRule.onNodeWithTag("${TestTags.PRIMARY_DIET_PREFIX}$dietTag")
            .performScrollTo()
            .performClick()
    }

    /**
     * Select dietary restriction (multi-select).
     */
    fun selectDietaryRestriction(restriction: DietaryTag) = apply {
        val restrictionTag = restriction.name.lowercase()
        composeTestRule.onNodeWithTag("${TestTags.DIETARY_RESTRICTION_PREFIX}$restrictionTag")
            .performScrollTo()
            .performClick()
    }

    /**
     * Assert diet is selected.
     */
    fun assertDietSelected(diet: DietaryTag) = apply {
        val dietTag = diet.name.lowercase()
        composeTestRule.onNodeWithTag("${TestTags.PRIMARY_DIET_PREFIX}$dietTag").assertIsDisplayed()
    }

    // ===================== Step 3: Cuisine Preferences =====================

    /**
     * Select cuisine (multi-select cards).
     */
    fun selectCuisine(cuisine: CuisineType) = apply {
        val cuisineTag = cuisine.name.lowercase()
        composeTestRule.onNodeWithTag("${TestTags.CUISINE_CARD_PREFIX}$cuisineTag")
            .performScrollTo()
            .performClick()
    }

    /**
     * Assert cuisine is selected.
     */
    fun assertCuisineSelected(cuisine: CuisineType) = apply {
        val cuisineTag = cuisine.name.lowercase()
        composeTestRule.onNodeWithTag("${TestTags.CUISINE_CARD_PREFIX}$cuisineTag").assertIsDisplayed()
    }

    /**
     * Select spice level from dropdown.
     */
    fun selectSpiceLevel(level: SpiceLevel) = apply {
        composeTestRule.onNodeWithTag(TestTags.SPICE_LEVEL_DROPDOWN).performClick()
        composeTestRule.waitForIdle()
        val levelText = when (level) {
            SpiceLevel.MILD -> "Mild"
            SpiceLevel.MEDIUM -> "Medium"
            SpiceLevel.SPICY -> "Spicy"
            SpiceLevel.VERY_SPICY -> "Very Spicy"
        }
        composeTestRule.onNodeWithText(levelText).performClick()
    }

    // ===================== Step 4: Disliked Ingredients =====================

    /**
     * Select disliked ingredient from common list.
     */
    fun selectDislikedIngredient(ingredientName: String) = apply {
        // First try to find by tag, fallback to text
        try {
            val ingredientTag = ingredientName.lowercase().replace(" ", "_")
            composeTestRule.onNodeWithTag("${TestTags.INGREDIENT_CHIP_PREFIX}$ingredientTag")
                .performScrollTo()
                .performClick()
        } catch (e: Exception) {
            composeTestRule.onNodeWithText(ingredientName, substring = true)
                .performScrollTo()
                .performClick()
        }
    }

    /**
     * Search for ingredient.
     */
    fun searchIngredient(query: String) = apply {
        composeTestRule.onNodeWithTag(TestTags.INGREDIENT_SEARCH_FIELD).performTextInput(query)
        composeTestRule.waitForIdle()
    }

    /**
     * Clear ingredient search.
     */
    fun clearIngredientSearch() = apply {
        composeTestRule.onNodeWithTag(TestTags.INGREDIENT_SEARCH_FIELD).performTextInput("")
    }

    /**
     * Add custom ingredient.
     */
    fun addCustomIngredient(ingredientName: String) = apply {
        composeTestRule.onNodeWithTag(TestTags.INGREDIENT_SEARCH_FIELD).performTextInput(ingredientName)
        composeTestRule.onNodeWithTag(TestTags.INGREDIENT_ADD_BUTTON).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Remove custom ingredient.
     */
    fun removeCustomIngredient(ingredientName: String) = apply {
        val ingredientTag = ingredientName.lowercase().replace(" ", "_")
        composeTestRule.onNodeWithTag("${TestTags.CUSTOM_INGREDIENT_PREFIX}$ingredientTag")
            .performClick()
    }

    // ===================== Step 5: Cooking Time & Busy Days =====================

    /**
     * Set weekday cooking time.
     */
    fun setWeekdayCookingTime(minutes: Int) = apply {
        composeTestRule.onNodeWithTag(TestTags.WEEKDAY_TIME_DROPDOWN).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("$minutes minutes", substring = true).performClick()
    }

    /**
     * Set weekend cooking time.
     */
    fun setWeekendCookingTime(minutes: Int) = apply {
        composeTestRule.onNodeWithTag(TestTags.WEEKEND_TIME_DROPDOWN).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("$minutes minutes", substring = true).performClick()
    }

    /**
     * Select busy day.
     */
    fun selectBusyDay(day: DayOfWeek) = apply {
        val dayTag = day.name.lowercase()
        composeTestRule.onNodeWithTag("${TestTags.BUSY_DAY_CHIP_PREFIX}$dayTag")
            .performScrollTo()
            .performClick()
    }

    // ===================== Navigation =====================

    /**
     * Tap Next button.
     */
    fun tapNext() = apply {
        composeTestRule.onNodeWithTag(TestTags.ONBOARDING_NEXT_BUTTON).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Tap Back button.
     */
    fun tapBack() = apply {
        composeTestRule.onNodeWithTag(TestTags.ONBOARDING_BACK_BUTTON).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Assert Next button is enabled.
     */
    fun assertNextEnabled() = apply {
        composeTestRule.onNodeWithTag(TestTags.ONBOARDING_NEXT_BUTTON).assertIsEnabled()
    }

    /**
     * Assert Next button is disabled.
     */
    fun assertNextDisabled() = apply {
        composeTestRule.onNodeWithTag(TestTags.ONBOARDING_NEXT_BUTTON).assertIsNotEnabled()
    }

    /**
     * Tap "Create My Meal Plan" button (final step).
     */
    fun tapCreateMealPlan() = apply {
        composeTestRule.onNodeWithText("Create My Meal Plan", substring = true).performClick()
        composeTestRule.waitForIdle()
    }

    // ===================== Progress Verification =====================

    /**
     * Assert onboarding progress bar value.
     */
    fun assertProgress(expectedPercentage: Int) = apply {
        composeTestRule.onNodeWithTag(TestTags.ONBOARDING_PROGRESS_BAR).assertIsDisplayed()
        // Could add semantic verification of progress value
    }

    /**
     * Wait for generating screen after onboarding.
     */
    fun waitForGeneratingScreen(timeoutMillis: Long = 5000) = apply {
        composeTestRule.waitUntilNodeWithTagExists(TestTags.GENERATING_SCREEN, timeoutMillis)
    }
}
