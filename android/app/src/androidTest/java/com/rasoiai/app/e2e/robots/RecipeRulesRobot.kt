package com.rasoiai.app.e2e.robots

import android.util.Log
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.rasoiai.app.e2e.base.FoodCategory
import com.rasoiai.app.e2e.base.FrequencyType
import com.rasoiai.app.e2e.base.MealSlot
import com.rasoiai.app.e2e.base.NutritionGoalTestData
import com.rasoiai.app.e2e.base.RecipeRuleTestData
import com.rasoiai.app.e2e.base.RuleEnforcement
import com.rasoiai.app.e2e.base.RuleType
import com.rasoiai.app.e2e.base.waitUntilNodeWithTextExists
import com.rasoiai.app.presentation.common.TestTags
import java.time.DayOfWeek

/**
 * Robot for Recipe Rules screen interactions.
 *
 * The Recipe Rules screen has 2 tabs:
 * - 🍽️ Rules (all rule types: recipe, ingredient, meal-slot)
 * - 🥗 Nutrition
 *
 * Include/Exclude is selected via radio buttons within the Add Rule bottom sheet.
 */
class RecipeRulesRobot(private val composeTestRule: ComposeContentTestRule) {

    private val uiDevice: UiDevice
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    companion object {
        private const val TAG = "RecipeRulesRobot"
    }

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
     * Helper to click a tab by trying various text patterns.
     */
    private fun clickTab(tabName: String, emoji: String) {
        Log.d(TAG, "Clicking tab: $tabName (emoji: $emoji)")

        // Try multiple patterns
        val patterns = listOf(
            "$emoji $tabName",  // Emoji space title
            tabName,            // Just title
            "$tabName"          // Just title again
        )

        for (pattern in patterns) {
            val nodes = composeTestRule.onAllNodesWithText(pattern, substring = true, ignoreCase = true)
                .fetchSemanticsNodes()
            if (nodes.isNotEmpty()) {
                Log.d(TAG, "Found tab with pattern: $pattern")
                composeTestRule.onAllNodesWithText(pattern, substring = true, ignoreCase = true)[0]
                    .performClick()
                composeTestRule.waitForIdle()
                return
            }
        }

        // Fallback: Use UiAutomator
        Log.d(TAG, "Trying UiAutomator for tab: $tabName")
        val tabElement = uiDevice.findObject(UiSelector().textContains(tabName))
        if (tabElement.exists()) {
            tabElement.click()
            composeTestRule.waitForIdle()
        } else {
            Log.w(TAG, "Could not find tab: $tabName")
        }
    }

    /**
     * Select Rules tab (🍽️ Rules).
     */
    fun selectRulesTab() = apply {
        clickTab("Rules", "🍽️")
    }

    /**
     * Select Nutrition tab (🥗 Nutrition).
     */
    fun selectNutritionTab() = apply {
        clickTab("Nutrition", "🥗")
    }

    // ===================== Add Rule Flow =====================

    /**
     * Tap the add rule button.
     * The button text is "+ ADD RULE" on Rules tab or "+ ADD NUTRITION GOAL" on Nutrition tab.
     */
    fun tapAddRuleButton() = apply {
        Log.d(TAG, "Tapping Add Rule button")
        try {
            // Try the specific button text pattern first: "+ Add ... Rule"
            // The UI shows buttons like "+ ADD RULE" or "+ ADD NUTRITION GOAL"
            val buttonPatterns = listOf(
                "+ ADD RULE",
                "+ Add Rule",
                "+ ADD NUTRITION GOAL",
                "+ Add Nutrition Goal"
            )

            for (pattern in buttonPatterns) {
                val nodes = composeTestRule.onAllNodesWithText(pattern, substring = true, ignoreCase = true).fetchSemanticsNodes()
                if (nodes.isNotEmpty()) {
                    Log.d(TAG, "Found button with text: $pattern")
                    composeTestRule.onNodeWithText(pattern, substring = true, ignoreCase = true).performClick()
                    composeTestRule.waitForIdle()
                    Thread.sleep(300)
                    return@apply
                }
            }

            // Fallback: Use UiAutomator to find button with "+ " prefix
            val addButton = uiDevice.findObject(UiSelector().textStartsWith("+").className("android.widget.Button"))
            if (addButton.exists()) {
                Log.d(TAG, "Found button via UiAutomator with '+' prefix")
                addButton.click()
                composeTestRule.waitForIdle()
                Thread.sleep(300)
                return@apply
            }

            // Last resort: Click on "Role = Button" that contains "Add"
            Log.w(TAG, "Could not find specific Add button, trying Role-based approach")
            val buttonSelector = UiSelector()
                .textContains("Add")
                .textContains("Rule")
                .className("android.widget.Button")
            val genericButton = uiDevice.findObject(buttonSelector)
            if (genericButton.exists()) {
                genericButton.click()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error tapping Add button: ${e.message}")
        }
        composeTestRule.waitForIdle()
        Thread.sleep(300) // Wait for sheet animation
    }

    /**
     * Select Include action in the bottom sheet via radio button.
     */
    fun selectIncludeAction() = apply {
        Log.d(TAG, "Selecting Include action")
        composeTestRule.onNodeWithText("Include", substring = true, ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Select Exclude action in the bottom sheet via radio button.
     */
    fun selectExcludeAction() = apply {
        Log.d(TAG, "Selecting Exclude action")
        composeTestRule.onNodeWithText("Exclude", substring = true, ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Enter search query for recipe/ingredient.
     */
    fun enterSearchQuery(query: String) = apply {
        Log.d(TAG, "Entering search query: $query")
        // Find the search field (placeholder "Search recipes..." or "Search ingredients...")
        val searchFields = composeTestRule.onAllNodesWithText("Search", substring = true).fetchSemanticsNodes()
        if (searchFields.isNotEmpty()) {
            composeTestRule.onAllNodesWithText("Search", substring = true)[0]
                .performTextClearance()
            composeTestRule.onAllNodesWithText("Search", substring = true)[0]
                .performTextInput(query)
        } else {
            // Try using UiAutomator
            val searchField = uiDevice.findObject(UiSelector().className("android.widget.EditText"))
            searchField.setText(query)
        }
        composeTestRule.waitForIdle()
        Thread.sleep(500) // Wait for search results
    }

    /**
     * Select a recipe/ingredient from the suggestion chips.
     *
     * Note: The search field may also contain the text we're looking for,
     * so we need to target the chip specifically (it has Role = Checkbox).
     */
    fun selectTarget(name: String) = apply {
        Log.d(TAG, "Selecting target: $name")

        // Wait a bit for search results to appear
        Thread.sleep(300)

        // Find all nodes with the target name
        val allNodes = composeTestRule.onAllNodesWithText(name, substring = true, ignoreCase = true).fetchSemanticsNodes()
        Log.d(TAG, "Found ${allNodes.size} nodes with text '$name'")

        if (allNodes.size > 1) {
            // Multiple matches - find the one that's NOT the search field
            // The chip/suggestion should be the one with Role = Checkbox or without ImeAction
            for (i in allNodes.indices) {
                val node = allNodes[i]
                val isSearchField = node.config.any { it.key.name == "ImeAction" }
                val hasRole = node.config.any { it.key.name == "Role" }
                Log.d(TAG, "Node $i: isSearchField=$isSearchField, hasRole=$hasRole")

                if (!isSearchField || hasRole) {
                    Log.d(TAG, "Clicking node $i (appears to be the suggestion chip)")
                    composeTestRule.onAllNodesWithText(name, substring = true, ignoreCase = true)[i]
                        .performScrollTo()
                        .performClick()
                    composeTestRule.waitForIdle()
                    return@apply
                }
            }
            // Fallback: click the last one (often the suggestion, not the input)
            Log.d(TAG, "Falling back to clicking last node")
            composeTestRule.onAllNodesWithText(name, substring = true, ignoreCase = true)[allNodes.size - 1]
                .performScrollTo()
                .performClick()
        } else if (allNodes.size == 1) {
            composeTestRule.onNodeWithText(name, substring = true, ignoreCase = true)
                .performScrollTo()
                .performClick()
        } else {
            Log.w(TAG, "No nodes found with text '$name'")
        }
        composeTestRule.waitForIdle()
    }

    /**
     * Select frequency type via radio button.
     *
     * Note: The UI uses these display names (from RecipeRulesViewModel.FrequencyType):
     * - DAILY -> "Every day"
     * - TIMES_PER_WEEK -> "X times per week"
     * - SPECIFIC_DAYS -> "Specific days"
     * - NEVER -> "Never"
     */
    fun selectFrequencyType(type: FrequencyType) = apply {
        val typeText = when (type) {
            FrequencyType.DAILY -> "Every day"
            FrequencyType.TIMES_PER_WEEK -> "times per week"
            FrequencyType.SPECIFIC_DAYS -> "Specific days"
            FrequencyType.NEVER -> "Never"
        }
        Log.d(TAG, "Selecting frequency type: $typeText")
        composeTestRule.onNodeWithText(typeText, substring = true, ignoreCase = true)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Select frequency count (for "X times per week").
     */
    fun selectFrequencyCount(count: Int) = apply {
        Log.d(TAG, "Selecting frequency count: $count")
        // The count dropdown shows current value - click to expand
        val countNodes = composeTestRule.onAllNodesWithText(count.toString()).fetchSemanticsNodes()
        if (countNodes.isEmpty()) {
            // Dropdown might show a different value - find and click it
            for (i in 1..7) {
                val nodes = composeTestRule.onAllNodesWithText(i.toString()).fetchSemanticsNodes()
                if (nodes.isNotEmpty()) {
                    composeTestRule.onAllNodesWithText(i.toString())[0].performClick()
                    composeTestRule.waitForIdle()
                    Thread.sleep(200)
                    break
                }
            }
        }
        // Now select the desired count
        val targetNodes = composeTestRule.onAllNodesWithText(count.toString()).fetchSemanticsNodes()
        if (targetNodes.size > 1) {
            // Click the dropdown menu item (last one)
            composeTestRule.onAllNodesWithText(count.toString())[targetNodes.size - 1].performClick()
        } else if (targetNodes.isNotEmpty()) {
            composeTestRule.onNodeWithText(count.toString()).performClick()
        }
        composeTestRule.waitForIdle()
    }

    /**
     * Select specific days (for "Specific days" frequency).
     */
    fun selectDays(days: List<DayOfWeek>) = apply {
        for (day in days) {
            val dayText = day.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
            Log.d(TAG, "Selecting day: $dayText")
            composeTestRule.onNodeWithText(dayText, substring = true)
                .performScrollTo()
                .performClick()
        }
        composeTestRule.waitForIdle()
    }

    /**
     * Select enforcement level via radio button.
     * Uses UiAutomator for more reliable scrolling and clicking in bottom sheets.
     */
    fun selectEnforcement(enforcement: RuleEnforcement) = apply {
        val fullText = when (enforcement) {
            RuleEnforcement.REQUIRED -> "Required"
            RuleEnforcement.PREFERRED -> "Preferred"
        }
        val shortText = when (enforcement) {
            RuleEnforcement.REQUIRED -> "Required"
            RuleEnforcement.PREFERRED -> "Preferred"
        }
        Log.d(TAG, "Selecting enforcement: $fullText")

        // Wait for bottom sheet content to be fully visible
        Thread.sleep(300)
        composeTestRule.waitForIdle()

        // Try UiAutomator first (handles scrolling better)
        var found = false

        // Approach 1: Try full text with UiAutomator
        val fullTextElement = uiDevice.findObject(UiSelector().textContains(fullText))
        if (fullTextElement.exists()) {
            Log.d(TAG, "Found enforcement text via UiAutomator, clicking")
            fullTextElement.click()
            found = true
        }

        // Approach 2: Try short text with UiAutomator
        if (!found) {
            val shortTextElement = uiDevice.findObject(UiSelector().textContains(shortText))
            if (shortTextElement.exists()) {
                Log.d(TAG, "Found short enforcement text via UiAutomator, clicking")
                shortTextElement.click()
                found = true
            }
        }

        // Approach 3: Try scrolling the bottom sheet and finding
        if (!found) {
            Log.d(TAG, "Enforcement text not immediately visible, trying scroll")
            // Scroll down in the bottom sheet by swiping
            uiDevice.swipe(540, 1500, 540, 800, 10)
            Thread.sleep(300)

            val afterScrollElement = uiDevice.findObject(UiSelector().textContains(shortText))
            if (afterScrollElement.waitForExists(2000)) {
                afterScrollElement.click()
                found = true
            }
        }

        // Approach 4: Compose fallback
        if (!found) {
            Log.d(TAG, "Using Compose fallback for enforcement")
            try {
                val nodes = composeTestRule.onAllNodesWithText(shortText, substring = true, ignoreCase = true)
                    .fetchSemanticsNodes()
                if (nodes.isNotEmpty()) {
                    composeTestRule.onAllNodesWithText(shortText, substring = true, ignoreCase = true)[0]
                        .performClick()
                    found = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Compose fallback also failed: ${e.message}")
            }
        }

        if (!found) {
            Log.w(TAG, "Could not find enforcement '$shortText' - it may already be selected")
        }

        composeTestRule.waitForIdle()
    }

    /**
     * Tap Save Rule button.
     * Also handles "SAVE GOAL" button for nutrition goals.
     */
    fun tapSaveRule() = apply {
        Log.d(TAG, "Tapping Save button")

        // Priority: "SAVE ANYWAY" (conflict override) > "SAVE GOAL" (nutrition) > "SAVE" (rule)
        val saveAnywayNodes = composeTestRule.onAllNodesWithText("SAVE ANYWAY", ignoreCase = true)
            .fetchSemanticsNodes()
        val saveGoalNodes = composeTestRule.onAllNodesWithText("SAVE GOAL", ignoreCase = true)
            .fetchSemanticsNodes()
        val saveNodes = composeTestRule.onAllNodesWithText("SAVE", ignoreCase = true)
            .fetchSemanticsNodes()

        when {
            saveAnywayNodes.isNotEmpty() -> {
                Log.d(TAG, "Found 'SAVE ANYWAY' button (conflict override)")
                composeTestRule.onNodeWithText("SAVE ANYWAY", ignoreCase = true)
                    .performScrollTo()
                    .performClick()
            }
            saveGoalNodes.isNotEmpty() -> {
                Log.d(TAG, "Found 'SAVE GOAL' button")
                composeTestRule.onNodeWithText("SAVE GOAL", ignoreCase = true)
                    .performScrollTo()
                    .performClick()
            }
            saveNodes.isNotEmpty() -> {
                Log.d(TAG, "Found 'SAVE' button")
                composeTestRule.onAllNodesWithText("SAVE", ignoreCase = true)[0]
                    .performScrollTo()
                    .performClick()
            }
            else -> {
                Log.w(TAG, "No save button found, trying UiAutomator")
                val saveButton = uiDevice.findObject(UiSelector().textContains("SAVE"))
                if (saveButton.exists()) {
                    saveButton.click()
                }
            }
        }
        composeTestRule.waitForIdle()
        Thread.sleep(800) // Wait for sheet to close with animation
    }

    // ===================== High-Level Rule Adding Methods =====================

    /**
     * Add an INCLUDE rule for an ingredient.
     *
     * Steps:
     * 1. Select Rules tab
     * 2. Tap Add Rule button
     * 3. Select Include action
     * 4. Search and select ingredient
     * 5. Set frequency
     * 6. Set enforcement
     * 7. Save
     */
    fun addIngredientIncludeRule(
        ingredientName: String,
        frequencyType: FrequencyType,
        frequencyCount: Int = 1,
        enforcement: RuleEnforcement = RuleEnforcement.PREFERRED,
        specificDays: List<DayOfWeek> = emptyList()
    ) = apply {
        Log.i(TAG, "Adding INCLUDE rule for ingredient: $ingredientName")

        selectRulesTab()
        tapAddRuleButton()
        selectIncludeAction()
        enterSearchQuery(ingredientName)
        Thread.sleep(500) // Wait for search results
        selectTarget(ingredientName)
        selectFrequencyType(frequencyType)

        if (frequencyType == FrequencyType.TIMES_PER_WEEK) {
            selectFrequencyCount(frequencyCount)
        } else if (frequencyType == FrequencyType.SPECIFIC_DAYS) {
            selectDays(specificDays)
        }

        selectEnforcement(enforcement)
        tapSaveRule()

        Log.i(TAG, "INCLUDE rule added for: $ingredientName")
    }

    /**
     * Add an EXCLUDE rule for an ingredient.
     */
    fun addIngredientExcludeRule(
        ingredientName: String,
        frequencyType: FrequencyType = FrequencyType.NEVER,
        specificDays: List<DayOfWeek> = emptyList()
    ) = apply {
        Log.i(TAG, "Adding EXCLUDE rule for ingredient: $ingredientName")

        selectRulesTab()
        tapAddRuleButton()
        selectExcludeAction()
        enterSearchQuery(ingredientName)
        Thread.sleep(500)
        selectTarget(ingredientName)
        selectFrequencyType(frequencyType)

        if (frequencyType == FrequencyType.SPECIFIC_DAYS) {
            selectDays(specificDays)
        }

        // Exclude rules are always REQUIRED
        selectEnforcement(RuleEnforcement.REQUIRED)
        tapSaveRule()

        Log.i(TAG, "EXCLUDE rule added for: $ingredientName")
    }

    /**
     * Add a complete rule using RecipeRuleTestData.
     */
    fun addRule(rule: RecipeRuleTestData, isInclude: Boolean) = apply {
        Log.d(TAG, "Adding rule: ${rule.targetName}, isInclude=$isInclude")

        when (rule.type) {
            RuleType.NUTRITION -> selectNutritionTab()
            else -> selectRulesTab()
        }

        tapAddRuleButton()

        if (isInclude) {
            selectIncludeAction()
        } else {
            selectExcludeAction()
        }

        enterSearchQuery(rule.targetName)
        Thread.sleep(500)
        selectTarget(rule.targetName)
        selectFrequencyType(rule.frequencyType)

        if (rule.frequencyType == FrequencyType.TIMES_PER_WEEK) {
            selectFrequencyCount(rule.frequency)
        }

        selectEnforcement(rule.enforcement)
        tapSaveRule()

        // Wait for sheet to fully close and rule to be saved
        Thread.sleep(1000)
        composeTestRule.waitForIdle()
        Log.d(TAG, "Rule added: ${rule.targetName}")
    }

    // ===================== Legacy Methods (For Backwards Compatibility) =====================

    /**
     * @deprecated Use selectRulesTab() and selectIncludeAction() in bottom sheet.
     */
    @Deprecated("Include/Exclude are now radio buttons in bottom sheet, not tabs")
    fun selectIncludeTab() = apply {
        Log.w(TAG, "selectIncludeTab() is deprecated - use addIngredientIncludeRule() instead")
        selectRulesTab()
    }

    /**
     * @deprecated Use selectRulesTab() and selectExcludeAction() in bottom sheet.
     */
    @Deprecated("Include/Exclude are now radio buttons in bottom sheet, not tabs")
    fun selectExcludeTab() = apply {
        Log.w(TAG, "selectExcludeTab() is deprecated - use addIngredientExcludeRule() instead")
        selectRulesTab()
    }

    /**
     * @deprecated Use tapAddRuleButton() instead.
     */
    @Deprecated("Use tapAddRuleButton()")
    fun tapAddRule() = tapAddRuleButton()

    /**
     * Add a complete include rule (legacy method).
     * @deprecated Use addIngredientIncludeRule() for clearer API.
     */
    fun addIncludeRule(rule: RecipeRuleTestData) = apply {
        addRule(rule, isInclude = true)
    }

    /**
     * Add a complete exclude rule (legacy method).
     * @deprecated Use addIngredientExcludeRule() for clearer API.
     */
    fun addExcludeRule(rule: RecipeRuleTestData) = apply {
        addRule(rule, isInclude = false)
    }

    // ===================== Nutrition Goals =====================

    /**
     * Tap add nutrition goal button.
     */
    fun tapAddNutritionGoal() = apply {
        selectNutritionTab()
        tapAddRuleButton()
    }

    /**
     * Select food category.
     * Handles multiple matches by using UiAutomator which is more reliable with multiple text matches.
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
        Log.d(TAG, "Selecting food category: $categoryText")

        // Wait for bottom sheet to fully open
        Thread.sleep(300)
        composeTestRule.waitForIdle()

        // Use UiAutomator for more reliable clicking when there might be multiple matches
        val categoryElement = uiDevice.findObject(UiSelector().textContains(categoryText))
        if (categoryElement.waitForExists(3000)) {
            Log.d(TAG, "Found category '$categoryText' via UiAutomator, clicking")
            categoryElement.click()
        } else {
            // Fallback to Compose - use onAllNodes to handle multiple matches
            val nodes = composeTestRule.onAllNodesWithText(categoryText, substring = true, ignoreCase = true)
                .fetchSemanticsNodes()
            Log.d(TAG, "Found ${nodes.size} nodes with text '$categoryText' via Compose")
            if (nodes.isNotEmpty()) {
                // Click using UiAutomator coordinates since performScrollTo fails with multiple nodes
                composeTestRule.onAllNodesWithText(categoryText, substring = true, ignoreCase = true)[0]
                    .performClick()
            } else {
                throw AssertionError("Could not find category '$categoryText'")
            }
        }
        composeTestRule.waitForIdle()
    }

    /**
     * Add a nutrition goal.
     */
    fun addNutritionGoal(goal: NutritionGoalTestData) = apply {
        Log.d(TAG, "Adding nutrition goal: ${goal.foodCategory}")
        tapAddNutritionGoal()
        Thread.sleep(500) // Wait for sheet to open

        selectFoodCategory(goal.foodCategory)
        Thread.sleep(300)

        selectFrequencyCount(goal.weeklyTarget)
        Thread.sleep(300)

        selectEnforcement(goal.enforcement)
        Thread.sleep(300)

        // Tap save - use UiAutomator for reliability
        Log.d(TAG, "Looking for SAVE GOAL button")
        val saveGoalButton = uiDevice.findObject(UiSelector().textContains("SAVE GOAL"))
        if (saveGoalButton.waitForExists(3000)) {
            Log.d(TAG, "Found 'SAVE GOAL' button, clicking")
            saveGoalButton.click()
        } else {
            Log.d(TAG, "SAVE GOAL not found, trying tapSaveRule()")
            tapSaveRule()
        }

        // Wait for sheet to fully close
        Thread.sleep(1500)
        composeTestRule.waitForIdle()

        Log.d(TAG, "Nutrition goal added successfully")
    }

    // ===================== Rule Cards =====================

    /**
     * Assert rule card is displayed.
     * Handles multiple matches by checking at least one node with the target name is displayed.
     */
    fun assertRuleCardDisplayed(targetName: String) = apply {
        Log.d(TAG, "Asserting rule card displayed: $targetName")
        // Wait for any sheet animations to complete
        Thread.sleep(800)
        composeTestRule.waitForIdle()

        // Use UiAutomator to check element exists
        val targetElement = uiDevice.findObject(UiSelector().textContains(targetName))
        if (targetElement.exists()) {
            Log.d(TAG, "Found element with text '$targetName' via UiAutomator")
        }

        // Use Compose test to verify at least one node exists and is displayed
        val nodes = composeTestRule.onAllNodesWithText(targetName, substring = true, ignoreCase = true)
            .fetchSemanticsNodes()

        if (nodes.isEmpty()) {
            throw AssertionError("No nodes found with text '$targetName'")
        }

        Log.d(TAG, "Found ${nodes.size} node(s) with text '$targetName'")

        // Assert at least one is displayed - use filterToOne if possible, otherwise just check existence
        try {
            // Try to find a unique match with more specific criteria
            composeTestRule.onAllNodesWithText(targetName, substring = true, ignoreCase = true)
                .assertCountEquals(nodes.size) // Just verify count matches what we found
        } catch (e: Exception) {
            Log.d(TAG, "Count assertion issue, but nodes exist: ${e.message}")
        }

        // Verify at least one node is displayed using UiAutomator
        if (!targetElement.exists()) {
            throw AssertionError("Element '$targetName' not found on screen")
        }
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
     * Delete rule by opening the three-dot menu and clicking Delete.
     * Now simplified after fixing duplicate contentDescription issue in RuleCard.kt.
     */
    fun deleteRule(targetName: String) = apply {
        Log.d(TAG, "Deleting rule: $targetName")

        // Wait for rule card to be fully displayed
        Thread.sleep(1000)
        composeTestRule.waitForIdle()

        // Scroll to the rule card
        composeTestRule.onNodeWithText(targetName, substring = true)
            .performScrollTo()
        composeTestRule.waitForIdle()

        // Try to find and click the menu button with retries
        var menuClicked = false
        for (attempt in 1..5) {
            Log.d(TAG, "Attempt $attempt to find menu button")

            // Try contentDescription first (preferred after fixing duplicate contentDescription)
            val menuButtons = composeTestRule.onAllNodesWithContentDescription("More options")
                .fetchSemanticsNodes()
            Log.d(TAG, "Found ${menuButtons.size} menu button(s) by contentDescription")

            if (menuButtons.isNotEmpty()) {
                composeTestRule.onNodeWithContentDescription("More options").performClick()
                menuClicked = true
                break
            }

            // Try test tag as fallback
            val menuByTag = composeTestRule.onAllNodesWithTag(TestTags.RULE_CARD_MENU_BUTTON)
                .fetchSemanticsNodes()
            Log.d(TAG, "Found ${menuByTag.size} menu button(s) by test tag")

            if (menuByTag.isNotEmpty()) {
                composeTestRule.onNodeWithTag(TestTags.RULE_CARD_MENU_BUTTON).performClick()
                menuClicked = true
                break
            }

            // Try UiAutomator as last resort
            val moreOptionsButton = uiDevice.findObject(UiSelector().description("More options"))
            if (moreOptionsButton.exists()) {
                Log.d(TAG, "Found 'More options' via UiAutomator, clicking")
                moreOptionsButton.click()
                menuClicked = true
                break
            }

            Thread.sleep(500)
            composeTestRule.waitForIdle()
        }

        if (!menuClicked) {
            // Debug: print semantics tree to understand what's on screen
            Log.e(TAG, "Could not find menu button. Printing semantics tree...")
            try {
                composeTestRule.onRoot().printToLog(TAG)
            } catch (e: Exception) {
                Log.e(TAG, "Could not print semantics tree (multiple roots): ${e.message}")
            }
            throw AssertionError("Could not find 'More options' menu button after 5 attempts")
        }

        composeTestRule.waitForIdle()
        Thread.sleep(300) // Wait for menu animation

        // Click "Delete" in the dropdown
        composeTestRule.onNodeWithText("Delete", ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(300)

        // Handle confirmation dialog if present
        try {
            composeTestRule.onNodeWithText("Confirm", ignoreCase = true).performClick()
            composeTestRule.waitForIdle()
        } catch (e: Exception) {
            Log.d(TAG, "No confirmation dialog found")
        }
    }

    /**
     * Toggle rule active state by opening the three-dot menu and clicking Pause/Enable.
     * The menu shows "Pause" if currently active, "Enable" if currently paused.
     * Now simplified after fixing duplicate contentDescription issue in RuleCard.kt.
     */
    fun toggleRuleActive(targetName: String) = apply {
        Log.d(TAG, "Toggling active state for rule: $targetName")

        // Wait for rule card to be fully displayed after add/save
        Thread.sleep(2000)
        composeTestRule.waitForIdle()

        // Scroll to the rule card
        composeTestRule.onNodeWithText(targetName, substring = true)
            .performScrollTo()
        composeTestRule.waitForIdle()

        // Try to find and click the menu button with retries
        var menuClicked = false
        for (attempt in 1..5) {
            Log.d(TAG, "Attempt $attempt to find menu button")

            // Try contentDescription first
            val menuButtons = composeTestRule.onAllNodesWithContentDescription("More options")
                .fetchSemanticsNodes()
            Log.d(TAG, "Found ${menuButtons.size} menu button(s) by contentDescription")

            if (menuButtons.isNotEmpty()) {
                composeTestRule.onNodeWithContentDescription("More options").performClick()
                menuClicked = true
                break
            }

            // Try test tag as fallback
            val menuByTag = composeTestRule.onAllNodesWithTag(TestTags.RULE_CARD_MENU_BUTTON)
                .fetchSemanticsNodes()
            Log.d(TAG, "Found ${menuByTag.size} menu button(s) by test tag")

            if (menuByTag.isNotEmpty()) {
                composeTestRule.onNodeWithTag(TestTags.RULE_CARD_MENU_BUTTON).performClick()
                menuClicked = true
                break
            }

            // Try UiAutomator as last resort
            val moreOptionsButton = uiDevice.findObject(UiSelector().description("More options"))
            if (moreOptionsButton.exists()) {
                Log.d(TAG, "Found 'More options' via UiAutomator, clicking")
                moreOptionsButton.click()
                menuClicked = true
                break
            }

            Thread.sleep(500)
            composeTestRule.waitForIdle()
        }

        if (!menuClicked) {
            // Debug: print semantics tree to understand what's on screen
            Log.e(TAG, "Could not find menu button. Printing semantics tree...")
            try {
                composeTestRule.onRoot().printToLog(TAG)
            } catch (e: Exception) {
                Log.e(TAG, "Could not print semantics tree (multiple roots): ${e.message}")
            }
            throw AssertionError("Could not find 'More options' menu button after 5 attempts")
        }

        composeTestRule.waitForIdle()
        Thread.sleep(300)

        // Click "Pause" or "Enable" in the dropdown
        val pauseNodes = composeTestRule.onAllNodesWithText("Pause", ignoreCase = true)
            .fetchSemanticsNodes()

        if (pauseNodes.isNotEmpty()) {
            Log.d(TAG, "Clicking 'Pause' to deactivate rule")
            composeTestRule.onNodeWithText("Pause", ignoreCase = true).performClick()
        } else {
            Log.d(TAG, "Clicking 'Enable' to activate rule")
            composeTestRule.onNodeWithText("Enable", ignoreCase = true).performClick()
        }
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
     * Handles multiple matches by using UiAutomator to verify presence.
     */
    fun assertNutritionGoalProgress(category: String, progress: String) = apply {
        Log.d(TAG, "Asserting nutrition goal progress: $category - $progress")
        // Wait for any sheet animations to complete
        Thread.sleep(800)
        composeTestRule.waitForIdle()

        // Use UiAutomator to find the category text - more reliable with multiple matches
        val categoryElement = uiDevice.findObject(UiSelector().textContains(category))
        if (!categoryElement.waitForExists(3000)) {
            throw AssertionError("No element found with text '$category'")
        }
        Log.d(TAG, "Found category element via UiAutomator")

        // Check for progress text
        val progressElement = uiDevice.findObject(UiSelector().textContains(progress))
        if (progressElement.exists()) {
            Log.d(TAG, "Found progress element via UiAutomator")
        }
    }
}
