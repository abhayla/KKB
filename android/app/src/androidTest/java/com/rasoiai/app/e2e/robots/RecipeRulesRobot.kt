package com.rasoiai.app.e2e.robots

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.rasoiai.app.e2e.base.FoodCategory
import com.rasoiai.app.e2e.base.FrequencyType
import com.rasoiai.app.e2e.base.MealSlot
import com.rasoiai.app.e2e.base.NutritionGoalTestData
import com.rasoiai.app.e2e.base.RecipeRuleTestData
import com.rasoiai.app.e2e.base.RuleEnforcement
import com.rasoiai.app.e2e.base.RuleType
import com.rasoiai.app.e2e.base.waitUntilNodeWithTextExists

/**
 * Robot for Recipe Rules screen interactions.
 * Handles include rules, exclude rules, and nutrition goals.
 */
class RecipeRulesRobot(private val composeTestRule: ComposeContentTestRule) {

    /**
     * Wait for recipe rules screen to be displayed.
     */
    fun waitForRecipeRulesScreen(timeoutMillis: Long = 5000) = apply {
        composeTestRule.waitUntilNodeWithTextExists("Recipe Rules", timeoutMillis)
    }

    /**
     * Assert recipe rules screen is displayed.
     */
    fun assertRecipeRulesScreenDisplayed() = apply {
        composeTestRule.onNodeWithText("Recipe Rules", ignoreCase = true).assertIsDisplayed()
    }

    // ===================== Tab Navigation =====================

    /**
     * Select Include tab.
     */
    fun selectIncludeTab() = apply {
        composeTestRule.onNodeWithText("Include", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Select Exclude tab.
     */
    fun selectExcludeTab() = apply {
        composeTestRule.onNodeWithText("Exclude", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Select Nutrition tab.
     */
    fun selectNutritionTab() = apply {
        composeTestRule.onNodeWithText("Nutrition", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Select Settings tab.
     */
    fun selectSettingsTab() = apply {
        composeTestRule.onNodeWithText("Settings", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    // ===================== Add Include Rule =====================

    /**
     * Tap add rule button.
     */
    fun tapAddRule() = apply {
        composeTestRule.onNodeWithText("Add Rule", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Select rule type.
     */
    fun selectRuleType(type: RuleType) = apply {
        val typeText = when (type) {
            RuleType.RECIPE -> "Recipe"
            RuleType.INGREDIENT -> "Ingredient"
            RuleType.MEAL_SLOT -> "Meal Slot"
            RuleType.NUTRITION -> "Nutrition"
        }
        composeTestRule.onNodeWithText("Type", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(typeText, ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Enter target name (recipe or ingredient).
     */
    fun enterTargetName(name: String) = apply {
        composeTestRule.onNodeWithText("Target", substring = true, ignoreCase = true)
            .performTextInput(name)
        composeTestRule.waitForIdle()
    }

    /**
     * Select target from suggestions.
     */
    fun selectTarget(name: String) = apply {
        composeTestRule.onNodeWithText(name, substring = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Set frequency.
     */
    fun setFrequency(count: Int, type: FrequencyType) = apply {
        // Enter count
        composeTestRule.onNodeWithText("Frequency", substring = true, ignoreCase = true)
            .performTextInput(count.toString())

        // Select frequency type
        val typeText = when (type) {
            FrequencyType.DAILY -> "Daily"
            FrequencyType.TIMES_PER_WEEK -> "Times per week"
            FrequencyType.SPECIFIC_DAYS -> "Specific days"
            FrequencyType.NEVER -> "Never"
        }
        composeTestRule.onNodeWithText(typeText, ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Select meal slots.
     */
    fun selectMealSlots(slots: List<MealSlot>) = apply {
        for (slot in slots) {
            val slotText = when (slot) {
                MealSlot.BREAKFAST -> "Breakfast"
                MealSlot.LUNCH -> "Lunch"
                MealSlot.DINNER -> "Dinner"
                MealSlot.SNACKS -> "Snacks"
            }
            composeTestRule.onNodeWithText(slotText, ignoreCase = true)
                .performScrollTo()
                .performClick()
        }
        composeTestRule.waitForIdle()
    }

    /**
     * Select enforcement level.
     */
    fun selectEnforcement(enforcement: RuleEnforcement) = apply {
        val enforcementText = when (enforcement) {
            RuleEnforcement.REQUIRED -> "Required"
            RuleEnforcement.PREFERRED -> "Preferred"
        }
        composeTestRule.onNodeWithText(enforcementText, ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Save rule.
     */
    fun saveRule() = apply {
        composeTestRule.onNodeWithText("Save", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Add a complete include rule.
     */
    fun addIncludeRule(rule: RecipeRuleTestData) = apply {
        selectIncludeTab()
        tapAddRule()
        selectRuleType(rule.type)
        enterTargetName(rule.targetName)
        selectTarget(rule.targetName)
        setFrequency(rule.frequency, rule.frequencyType)
        if (rule.mealSlot.isNotEmpty()) {
            selectMealSlots(rule.mealSlot)
        }
        selectEnforcement(rule.enforcement)
        saveRule()
    }

    /**
     * Add a complete exclude rule.
     */
    fun addExcludeRule(rule: RecipeRuleTestData) = apply {
        selectExcludeTab()
        tapAddRule()
        selectRuleType(rule.type)
        enterTargetName(rule.targetName)
        selectTarget(rule.targetName)
        setFrequency(rule.frequency, rule.frequencyType)
        selectEnforcement(rule.enforcement)
        saveRule()
    }

    // ===================== Nutrition Goals =====================

    /**
     * Tap add nutrition goal button.
     */
    fun tapAddNutritionGoal() = apply {
        composeTestRule.onNodeWithText("Add Goal", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Select food category.
     */
    fun selectFoodCategory(category: FoodCategory) = apply {
        val categoryText = when (category) {
            FoodCategory.GREEN_LEAFY -> "Green Leafy"
            FoodCategory.CITRUS_VITAMIN_C -> "Citrus/Vitamin C"
            FoodCategory.IRON_RICH -> "Iron Rich"
            FoodCategory.HIGH_PROTEIN -> "High Protein"
            FoodCategory.CALCIUM_RICH -> "Calcium Rich"
            FoodCategory.FIBER_RICH -> "Fiber Rich"
            FoodCategory.OMEGA_3 -> "Omega-3"
            FoodCategory.ANTIOXIDANT -> "Antioxidant"
        }
        composeTestRule.onNodeWithText("Category", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(categoryText, ignoreCase = true)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Set weekly target.
     */
    fun setWeeklyTarget(servings: Int) = apply {
        composeTestRule.onNodeWithText("Weekly Target", substring = true, ignoreCase = true)
            .performTextInput(servings.toString())
    }

    /**
     * Add a nutrition goal.
     */
    fun addNutritionGoal(goal: NutritionGoalTestData) = apply {
        selectNutritionTab()
        tapAddNutritionGoal()
        selectFoodCategory(goal.foodCategory)
        setWeeklyTarget(goal.weeklyTarget)
        selectEnforcement(goal.enforcement)
        saveRule()
    }

    // ===================== Rule Cards =====================

    /**
     * Assert rule card is displayed.
     */
    fun assertRuleCardDisplayed(targetName: String) = apply {
        composeTestRule.onNodeWithText(targetName, substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * Assert rule with frequency is displayed.
     */
    fun assertRuleWithFrequency(targetName: String, frequency: String) = apply {
        assertRuleCardDisplayed(targetName)
        composeTestRule.onNodeWithText(frequency, substring = true).assertIsDisplayed()
    }

    /**
     * Tap on rule card to edit.
     */
    fun tapRuleCard(targetName: String) = apply {
        composeTestRule.onNodeWithText(targetName, substring = true)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Delete rule.
     */
    fun deleteRule(targetName: String) = apply {
        tapRuleCard(targetName)
        composeTestRule.onNodeWithText("Delete", ignoreCase = true).performClick()
        composeTestRule.onNodeWithText("Confirm", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Toggle rule active state.
     */
    fun toggleRuleActive(targetName: String) = apply {
        composeTestRule.onNodeWithText(targetName, substring = true)
            .performScrollTo()
        // Find and click the toggle for this rule
        composeTestRule.onNodeWithText("Active", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    // ===================== Empty State =====================

    /**
     * Assert empty state is displayed.
     */
    fun assertEmptyStateDisplayed() = apply {
        composeTestRule.onNodeWithText("No rules", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    // ===================== Nutrition Goal Progress =====================

    /**
     * Assert nutrition goal progress is displayed.
     */
    fun assertNutritionGoalProgress(category: String, progress: String) = apply {
        composeTestRule.onNodeWithText(category, substring = true, ignoreCase = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(progress, substring = true).assertIsDisplayed()
    }
}
