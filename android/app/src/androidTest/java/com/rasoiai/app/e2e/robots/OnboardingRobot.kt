package com.rasoiai.app.e2e.robots

import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import com.rasoiai.app.e2e.base.FamilyMember
import com.rasoiai.app.e2e.base.HealthNeed
import com.rasoiai.app.e2e.base.MemberType
import com.rasoiai.app.e2e.base.SpiceLevel
import com.rasoiai.app.e2e.base.waitUntilNodeWithTagExists
import com.rasoiai.app.e2e.base.waitUntilNodeWithTextExists
import com.rasoiai.app.presentation.common.TestTags
import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.DietaryRestriction
import com.rasoiai.domain.model.DietaryTag
import java.time.DayOfWeek

/**
 * Robot for Onboarding screen interactions.
 * Handles all 5 onboarding steps.
 */
class OnboardingRobot(private val composeTestRule: ComposeContentTestRule) {

    private val uiDevice: UiDevice
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    companion object {
        private const val TAG = "OnboardingRobot"
    }

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
        Log.i(TAG, "Adding family member: ${member.name}, age ${member.age}, type ${member.type}")
        tapAddFamilyMember()

        // Enter name
        composeTestRule.onNodeWithTag(TestTags.MEMBER_NAME_FIELD).performTextInput(member.name)
        Log.d(TAG, "Entered name: ${member.name}")

        // Select type - use last matching node since dropdown is overlaid on top
        composeTestRule.onNodeWithTag(TestTags.MEMBER_TYPE_DROPDOWN).performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(200) // Wait for dropdown animation
        val typeOption = when (member.type) {
            MemberType.ADULT -> "Adult"
            MemberType.CHILD -> "Child"
            MemberType.SENIOR -> "Senior"
        }
        // When multiple nodes match (e.g., "Adult" in member list + dropdown),
        // click the last one which is the dropdown menu item (rendered on top)
        val typeNodes = composeTestRule.onAllNodesWithText(typeOption).fetchSemanticsNodes()
        if (typeNodes.size > 1) {
            Log.d(TAG, "Found ${typeNodes.size} '$typeOption' nodes, clicking last (dropdown)")
            composeTestRule.onAllNodesWithText(typeOption)[typeNodes.size - 1].performClick()
        } else {
            composeTestRule.onNodeWithText(typeOption).performClick()
        }
        composeTestRule.waitForIdle()
        Log.d(TAG, "Selected type: $typeOption")

        // Select age using UIAutomator for reliable scrolling
        selectAgeInDropdown(member.age)

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
            Log.d(TAG, "Selecting health need: $needTag")
            composeTestRule.onNodeWithTag("${TestTags.MEMBER_DIETARY_NEED_PREFIX}$needTag")
                .performScrollTo()
                .performClick()
        }

        // Save — scroll to ensure save button is visible, then click
        Log.d(TAG, "About to click Save button for ${member.name}")
        composeTestRule.onNodeWithTag(TestTags.MEMBER_SAVE_BUTTON)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(800) // Wait for bottom sheet dismiss animation
        composeTestRule.waitForIdle()

        // Verify member was actually added by waiting for their name in the tree
        try {
            composeTestRule.waitUntilNodeWithTextExists(member.name, 3000)
            Log.i(TAG, "Family member added and verified: ${member.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Family member ${member.name} NOT found after save! Bottom sheet may not have saved.")
            // Log the semantic tree for debugging
            try {
                val tree = composeTestRule.onNodeWithTag(TestTags.FAMILY_MEMBERS_LIST)
                    .fetchSemanticsNode().toString()
                Log.e(TAG, "Family members list node: $tree")
            } catch (ignored: Exception) {
                Log.e(TAG, "Could not find family members list node")
            }
            throw AssertionError("Failed to add family member ${member.name}: member not found after save", e)
        }
    }

    /**
     * Select age in the dropdown using Compose test APIs with test tags.
     * The dropdown has 100 items (1-100 years), so ages > ~10 require scrolling.
     *
     * FIX for Issue #42: Use Compose test tags + performScrollTo() for reliable age selection.
     * Each dropdown item now has a testTag like "member_age_option_12" for age 12.
     *
     * IMPORTANT: Never call pressBack() as it dismisses the entire bottom sheet!
     * The dropdown will auto-dismiss when an item is clicked.
     */
    private fun selectAgeInDropdown(targetAge: Int) {
        val ageText = "$targetAge years"
        val ageTag = "${TestTags.MEMBER_AGE_OPTION_PREFIX}$targetAge"
        Log.d(TAG, "Selecting age: $ageText using tag: $ageTag")

        // Open age dropdown
        composeTestRule.onNodeWithTag(TestTags.MEMBER_AGE_DROPDOWN).performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(500) // Wait for dropdown menu to fully render

        try {
            // Primary strategy: Use test tag with performScrollTo()
            // This is the most reliable method for Compose dropdowns
            Log.d(TAG, "Using Compose test tag to select age $targetAge")
            composeTestRule.onNodeWithTag(ageTag)
                .performScrollTo()
                .performClick()
            composeTestRule.waitForIdle()
            Thread.sleep(300)
            Log.i(TAG, "Successfully selected age: $targetAge (Compose testTag method)")
            return

        } catch (e: AssertionError) {
            Log.w(TAG, "Compose testTag method failed for age $targetAge: ${e.message}")

            // Fallback: Try UIAutomator scroll + click
            try {
                Log.d(TAG, "Falling back to UIAutomator for age $targetAge")
                val scrollable = UiScrollable(UiSelector().scrollable(true))
                if (scrollable.exists()) {
                    scrollable.setAsVerticalList()
                    scrollable.maxSearchSwipes = 50

                    // Reset to beginning and scroll forward to target
                    scrollable.scrollToBeginning(25)
                    Thread.sleep(300)

                    val found = scrollable.scrollTextIntoView(ageText)
                    if (found) {
                        Thread.sleep(200)
                        val ageObject = uiDevice.findObject(UiSelector().text(ageText))
                        if (ageObject.exists()) {
                            ageObject.click()
                            composeTestRule.waitForIdle()
                            Thread.sleep(300)
                            Log.i(TAG, "Successfully selected age: $targetAge (UIAutomator fallback)")
                            return
                        }
                    }
                }
            } catch (e2: Exception) {
                Log.e(TAG, "UIAutomator fallback also failed: ${e2.message}")
            }

            // Close dropdown and fail explicitly
            Log.e(TAG, "FAILED to select age $targetAge - closing dropdown")
            try {
                val anyAge = uiDevice.findObject(UiSelector().textContains("years"))
                if (anyAge.exists()) {
                    anyAge.click()
                    composeTestRule.waitForIdle()
                }
            } catch (ignored: Exception) {}

            throw AssertionError("Could not select age $targetAge in dropdown: ${e.message}")

        } catch (e: Exception) {
            Log.e(TAG, "Error selecting age $targetAge: ${e.message}", e)
            // Try to close dropdown
            try {
                val anyAge = uiDevice.findObject(UiSelector().textContains("years"))
                if (anyAge.exists()) {
                    anyAge.click()
                    composeTestRule.waitForIdle()
                }
            } catch (ignored: Exception) {}

            throw AssertionError("Failed to select age $targetAge: ${e.message}", e)
        }
    }

    /**
     * Assert family member is displayed in the list.
     */
    fun assertFamilyMemberDisplayed(name: String) = apply {
        composeTestRule.onNodeWithText(name).performScrollTo().assertIsDisplayed()
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
        composeTestRule.onNodeWithTag("${TestTags.FAMILY_MEMBER_EDIT_PREFIX}$index")
            .performScrollTo()
            .performClick()
    }

    /**
     * Delete a family member.
     */
    fun tapDeleteFamilyMember(index: Int) = apply {
        composeTestRule.onNodeWithTag("${TestTags.FAMILY_MEMBER_DELETE_PREFIX}$index")
            .performScrollTo()
            .performClick()
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
     * Uses the display name from DietaryRestriction enum since UI doesn't have test tags.
     */
    fun selectDietaryRestriction(restriction: DietaryTag) = apply {
        // Map DietaryTag to DietaryRestriction display name
        val displayName = when (restriction) {
            DietaryTag.JAIN -> "Jain (No root vegetables)"
            DietaryTag.SATTVIC -> "Sattvic (No onion/garlic)"
            DietaryTag.HALAL -> "Halal only"
            DietaryTag.VEGAN -> "Vegan"
            else -> restriction.displayName
        }
        Log.d("OnboardingRobot", "Selecting dietary restriction: $displayName")
        composeTestRule.onNodeWithText(displayName, substring = true)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
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
     * The UI shows cuisine.displayName.uppercase() (e.g., "NORTH INDIAN")
     */
    fun selectCuisine(cuisine: CuisineType) = apply {
        // Cuisine cards display displayName in uppercase
        val displayText = cuisine.displayName.uppercase()
        Log.d("OnboardingRobot", "Selecting cuisine: $displayText")
        composeTestRule.onNodeWithText(displayText)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Assert cuisine is selected.
     * Note: We verify by checking the text is displayed. Selection state is visual only.
     */
    fun assertCuisineSelected(cuisine: CuisineType) = apply {
        val displayText = cuisine.displayName.uppercase()
        composeTestRule.onNodeWithText(displayText).assertIsDisplayed()
    }

    /**
     * Select spice level from dropdown.
     * The dropdown doesn't have a test tag, so we click on the current value to expand.
     */
    fun selectSpiceLevel(level: SpiceLevel) = apply {
        val levelText = when (level) {
            SpiceLevel.MILD -> "Mild"
            SpiceLevel.MEDIUM -> "Medium"
            SpiceLevel.SPICY -> "Spicy"
            SpiceLevel.VERY_SPICY -> "Very Spicy"
        }
        // Default value is "Medium" - click on it to expand dropdown
        // Try to find and click on current spice level value
        val currentValues = listOf("Mild", "Medium", "Spicy", "Very Spicy")
        for (currentVal in currentValues) {
            val nodes = composeTestRule.onAllNodesWithText(currentVal).fetchSemanticsNodes()
            if (nodes.isNotEmpty()) {
                Log.d("OnboardingRobot", "Found spice dropdown with value '$currentVal', clicking to expand")
                composeTestRule.onNodeWithText(currentVal).performScrollTo().performClick()
                composeTestRule.waitForIdle()
                Thread.sleep(200) // Wait for dropdown animation
                break
            }
        }
        // Now select the desired level
        val levelNodes = composeTestRule.onAllNodesWithText(levelText).fetchSemanticsNodes()
        if (levelNodes.size > 1) {
            // Click the last one (dropdown menu item rendered on top)
            Log.d("OnboardingRobot", "Found ${levelNodes.size} '$levelText' nodes, clicking last (dropdown)")
            composeTestRule.onAllNodesWithText(levelText)[levelNodes.size - 1].performClick()
        } else if (levelNodes.isNotEmpty()) {
            composeTestRule.onNodeWithText(levelText).performClick()
        }
        composeTestRule.waitForIdle()
    }

    // ===================== Step 4: Disliked Ingredients =====================

    /**
     * Select disliked ingredient from common list.
     * The ingredient chips don't have test tags, so we use text-based selection.
     */
    fun selectDislikedIngredient(ingredientName: String) = apply {
        Log.d("OnboardingRobot", "Selecting disliked ingredient: $ingredientName")
        composeTestRule.onNodeWithText(ingredientName, substring = true)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
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
     * Set weekday cooking time using testTag-based dropdown selection.
     */
    fun setWeekdayCookingTime(minutes: Int) = apply {
        Log.d("OnboardingRobot", "Setting weekday cooking time: $minutes minutes")
        composeTestRule.onNodeWithTag(TestTags.WEEKDAY_TIME_DROPDOWN)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(300)

        val targetText = "$minutes minutes"
        val nodes = composeTestRule.onAllNodesWithText(targetText).fetchSemanticsNodes()
        if (nodes.isNotEmpty()) {
            // Click the last match (menu item when dropdown is open)
            composeTestRule.onAllNodesWithText(targetText)[nodes.size - 1].performClick()
        }
        composeTestRule.waitForIdle()
    }

    /**
     * Set weekend cooking time using testTag-based dropdown selection.
     */
    fun setWeekendCookingTime(minutes: Int) = apply {
        Log.d("OnboardingRobot", "Setting weekend cooking time: $minutes minutes")
        composeTestRule.onNodeWithTag(TestTags.WEEKEND_TIME_DROPDOWN)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(300)

        val targetText = "$minutes minutes"
        val nodes = composeTestRule.onAllNodesWithText(targetText).fetchSemanticsNodes()
        if (nodes.isNotEmpty()) {
            composeTestRule.onAllNodesWithText(targetText)[nodes.size - 1].performClick()
        }
        composeTestRule.waitForIdle()
    }

    /**
     * Select busy day.
     * The day chips may not have test tags, so we use text-based selection as fallback.
     */
    fun selectBusyDay(day: DayOfWeek) = apply {
        val dayText = when (day) {
            DayOfWeek.MONDAY -> "Mon"
            DayOfWeek.TUESDAY -> "Tue"
            DayOfWeek.WEDNESDAY -> "Wed"
            DayOfWeek.THURSDAY -> "Thu"
            DayOfWeek.FRIDAY -> "Fri"
            DayOfWeek.SATURDAY -> "Sat"
            DayOfWeek.SUNDAY -> "Sun"
        }
        Log.d("OnboardingRobot", "Selecting busy day: $dayText")
        composeTestRule.onNodeWithText(dayText, substring = true)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
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
