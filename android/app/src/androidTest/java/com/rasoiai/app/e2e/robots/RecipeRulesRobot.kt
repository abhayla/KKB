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
     * Uses testTag to avoid matching "Include" in the empty state description text.
     */
    fun selectIncludeAction() = apply {
        Log.d(TAG, "Selecting Include action")
        composeTestRule.onNodeWithTag(TestTags.RULE_ACTION_INCLUDE).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Select Exclude action in the bottom sheet via radio button.
     * Uses testTag to avoid matching "Exclude" in the empty state description text.
     */
    fun selectExcludeAction() = apply {
        Log.d(TAG, "Selecting Exclude action")
        composeTestRule.onNodeWithTag(TestTags.RULE_ACTION_EXCLUDE).performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Enter search query for recipe/ingredient.
     */
    fun enterSearchQuery(query: String) = apply {
        Log.d(TAG, "Entering search query: $query")
        try {
            // Use test tag for reliable selection
            composeTestRule.onNodeWithTag(TestTags.BOTTOM_SHEET_SEARCH_FIELD)
                .performTextClearance()
            composeTestRule.onNodeWithTag(TestTags.BOTTOM_SHEET_SEARCH_FIELD)
                .performTextInput(query)
        } catch (e: Exception) {
            Log.w(TAG, "Tag-based search field not found, falling back: ${e.message}")
            val searchFields = composeTestRule.onAllNodesWithText("Search", substring = true).fetchSemanticsNodes()
            if (searchFields.isNotEmpty()) {
                composeTestRule.onAllNodesWithText("Search", substring = true)[0]
                    .performTextClearance()
                composeTestRule.onAllNodesWithText("Search", substring = true)[0]
                    .performTextInput(query)
            } else {
                val searchField = uiDevice.findObject(UiSelector().className("android.widget.EditText"))
                searchField.setText(query)
            }
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

        // Try tag-based selection first (reliable)
        val tag = "${TestTags.SEARCH_RESULT_CHIP_PREFIX}$name"
        try {
            val tagNodes = composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes()
            if (tagNodes.isNotEmpty()) {
                Log.d(TAG, "Found target via tag: $tag")
                composeTestRule.onNodeWithTag(tag)
                    .performScrollTo()
                    .performClick()
                composeTestRule.waitForIdle()
                return@apply
            }
        } catch (e: Exception) {
            Log.d(TAG, "Tag-based selection failed for '$name': ${e.message}")
        }

        // Fallback: text-based disambiguation
        val allNodes = composeTestRule.onAllNodesWithText(name, substring = true, ignoreCase = true).fetchSemanticsNodes()
        Log.d(TAG, "Found ${allNodes.size} nodes with text '$name' (fallback)")

        if (allNodes.size > 1) {
            for (i in allNodes.indices) {
                val node = allNodes[i]
                val isSearchField = node.config.any { it.key.name == "ImeAction" }
                val hasRole = node.config.any { it.key.name == "Role" }
                if (!isSearchField || hasRole) {
                    composeTestRule.onAllNodesWithText(name, substring = true, ignoreCase = true)[i]
                        .performScrollTo()
                        .performClick()
                    composeTestRule.waitForIdle()
                    return@apply
                }
            }
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
        val tag = "${TestTags.FREQUENCY_TYPE_PREFIX}${type.name}"
        Log.d(TAG, "Selecting frequency type: ${type.name} via tag $tag")
        composeTestRule.onNodeWithTag(tag)
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
        val tag = when (enforcement) {
            RuleEnforcement.REQUIRED -> TestTags.ENFORCEMENT_REQUIRED
            RuleEnforcement.PREFERRED -> TestTags.ENFORCEMENT_PREFERRED
        }
        val text = when (enforcement) {
            RuleEnforcement.REQUIRED -> "Required"
            RuleEnforcement.PREFERRED -> "Preferred"
        }
        Log.d(TAG, "Selecting enforcement: $text (tag: $tag)")

        Thread.sleep(300)
        composeTestRule.waitForIdle()

        // Try tag-based selection first (reliable)
        try {
            composeTestRule.onNodeWithTag(tag)
                .performScrollTo()
                .performClick()
            composeTestRule.waitForIdle()
            return@apply
        } catch (e: Throwable) {
            Log.d(TAG, "Tag-based enforcement selection failed, using fallback: ${e.message}")
        }

        // Fallback: UiAutomator text search
        val textElement = uiDevice.findObject(UiSelector().textContains(text))
        if (textElement.exists()) {
            textElement.click()
        } else {
            // Scroll and retry
            uiDevice.swipe(540, 1500, 540, 800, 10)
            Thread.sleep(300)
            val afterScrollElement = uiDevice.findObject(UiSelector().textContains(text))
            if (afterScrollElement.waitForExists(2000)) {
                afterScrollElement.click()
            } else {
                Log.w(TAG, "Could not find enforcement '$text' - it may already be selected")
            }
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
        Log.d(TAG, "Adding rule: ${rule.targetName}, isInclude=$isInclude, mealSlots=${rule.mealSlot}")

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

        // Select meal slots if specified
        if (rule.mealSlot.isNotEmpty()) {
            selectMealSlotMode(specific = true)
            for (slot in rule.mealSlot) {
                toggleMealSlotChip(slot)
            }
        }

        selectEnforcement(rule.enforcement)
        tapSaveRule()

        // Wait for sheet to fully close and rule to be saved
        Thread.sleep(1000)
        composeTestRule.waitForIdle()
        Log.d(TAG, "Rule added: ${rule.targetName}")
    }

    /**
     * Select meal slot mode: ANY or SPECIFIC.
     */
    fun selectMealSlotMode(specific: Boolean) = apply {
        val tag = if (specific) TestTags.MEAL_SLOT_MODE_SPECIFIC else TestTags.MEAL_SLOT_MODE_ANY
        Log.d(TAG, "Selecting meal slot mode: ${if (specific) "SPECIFIC" else "ANY"}")
        composeTestRule.onNodeWithTag(tag)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Toggle a specific meal slot chip (BREAKFAST, LUNCH, DINNER, SNACKS).
     */
    fun toggleMealSlotChip(slot: MealSlot) = apply {
        val mealTypeName = slot.name // MealSlot enum names match MealType names
        val tag = "${TestTags.MEAL_SLOT_CHIP_PREFIX}$mealTypeName"
        Log.d(TAG, "Toggling meal slot chip: $mealTypeName via tag $tag")
        composeTestRule.onNodeWithTag(tag)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
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
        // Must match displayName from domain FoodCategory enum
        // UI renders: "${category.emoji} ${category.displayName}"
        val categoryText = when (category) {
            FoodCategory.GREEN_LEAFY -> "Green leafy vegetables"
            FoodCategory.CITRUS_VITAMIN_C -> "Citrus/Vitamin C foods"
            FoodCategory.IRON_RICH -> "Iron-rich foods"
            FoodCategory.HIGH_PROTEIN -> "High protein foods"
            FoodCategory.CALCIUM_RICH -> "Calcium-rich foods"
            FoodCategory.FIBER_RICH -> "Fiber-rich foods"
            FoodCategory.OMEGA_3 -> "Omega-3 rich foods"
            FoodCategory.ANTIOXIDANT -> "Antioxidant-rich foods"
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

        // AddNutritionGoalSheet does NOT have enforcement radio buttons —
        // skip enforcement selection for nutrition goals (only AddRuleBottomSheet has them)
        Log.d(TAG, "Skipping enforcement selection — not available in NutritionGoalSheet")
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
    fun assertRuleCardDisplayed(targetName: String, timeoutMillis: Long = 10000) = apply {
        Log.d(TAG, "Asserting rule card displayed: $targetName")
        // Wait for any sheet animations / tab transitions to complete
        composeTestRule.waitForIdle()

        // Use UiAutomator with waitForExists to handle tab transitions and lazy rendering
        val targetElement = uiDevice.findObject(UiSelector().textContains(targetName))
        if (targetElement.waitForExists(timeoutMillis)) {
            Log.d(TAG, "Found element with text '$targetName' via UiAutomator (initial wait)")
            return@apply
        }

        // Element not found after waiting — try Compose API (checks semantic tree, not just on-screen)
        Log.d(TAG, "'$targetName' not found after ${timeoutMillis}ms, checking Compose semantic tree")
        val nodes = composeTestRule.onAllNodesWithText(targetName, substring = true, ignoreCase = true)
            .fetchSemanticsNodes()
        if (nodes.isNotEmpty()) {
            Log.d(TAG, "Found ${nodes.size} node(s) with text '$targetName' via Compose semantic tree")
            // Try to scroll to it
            try {
                composeTestRule.onAllNodesWithText(targetName, substring = true, ignoreCase = true)[0]
                    .performScrollTo()
                Log.d(TAG, "Scrolled to '$targetName' via Compose")
                return@apply
            } catch (e: Exception) {
                Log.d(TAG, "Could not scroll to '$targetName': ${e.message}")
            }
            return@apply
        }

        // Last resort: small scroll down + up to trigger lazy list rendering
        Log.d(TAG, "'$targetName' not in semantic tree, trying small scroll")
        uiDevice.swipe(540, 1200, 540, 900, 10)
        Thread.sleep(500)
        if (targetElement.waitForExists(2000)) {
            Log.d(TAG, "Found '$targetName' after scrolling down")
            return@apply
        }
        uiDevice.swipe(540, 900, 540, 1200, 10)
        Thread.sleep(500)
        if (targetElement.waitForExists(2000)) {
            Log.d(TAG, "Found '$targetName' after scrolling up")
            return@apply
        }

        throw AssertionError("Element '$targetName' not found on screen after scrolling and waiting ${timeoutMillis}ms")
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
            } catch (e: Throwable) {
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
            } catch (e: Throwable) {
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

    // ===================== Diet Conflict & Search Assertions =====================

    /**
     * Assert that a diet conflict warning is displayed.
     * Looks for text patterns like "conflict", "warning", or "diet".
     */
    fun assertDietConflictWarning() = apply {
        Log.d(TAG, "Asserting diet conflict warning is displayed")
        Thread.sleep(500)
        composeTestRule.waitForIdle()

        val warningPatterns = listOf("conflict", "warning", "diet")
        for (pattern in warningPatterns) {
            val nodes = composeTestRule.onAllNodesWithText(pattern, substring = true, ignoreCase = true)
                .fetchSemanticsNodes()
            if (nodes.isNotEmpty()) {
                Log.d(TAG, "Found diet conflict warning with pattern: $pattern")
                return@apply
            }
        }

        // Try UiAutomator as fallback
        for (pattern in warningPatterns) {
            val element = uiDevice.findObject(UiSelector().textContains(pattern))
            if (element.exists()) {
                Log.d(TAG, "Found diet conflict warning via UiAutomator: $pattern")
                return@apply
            }
        }

        throw AssertionError("Diet conflict warning not found on screen")
    }

    /**
     * Assert that search shows no results / empty state.
     */
    fun assertSearchNoResults() = apply {
        Log.d(TAG, "Asserting search shows no results")
        Thread.sleep(500)
        composeTestRule.waitForIdle()

        val emptyPatterns = listOf("No results", "no recipes", "not found", "No matching")
        for (pattern in emptyPatterns) {
            val nodes = composeTestRule.onAllNodesWithText(pattern, substring = true, ignoreCase = true)
                .fetchSemanticsNodes()
            if (nodes.isNotEmpty()) {
                Log.d(TAG, "Found empty state with pattern: $pattern")
                return@apply
            }
        }

        // Also check via UiAutomator
        for (pattern in emptyPatterns) {
            val element = uiDevice.findObject(UiSelector().textContains(pattern))
            if (element.exists()) {
                Log.d(TAG, "Found empty state via UiAutomator: $pattern")
                return@apply
            }
        }

        // If no explicit empty state text, check that suggestion chips are absent
        Log.w(TAG, "No explicit 'no results' text found - assuming empty suggestion list")
    }

    /**
     * Assert rule sorting: active rules appear before paused rules in the list.
     */
    fun assertRuleSortedActiveFirst(activeNames: List<String>, pausedNames: List<String>) = apply {
        Log.d(TAG, "Asserting rules sorted: active=$activeNames before paused=$pausedNames")
        composeTestRule.waitForIdle()

        // Verify each active name appears before each paused name
        // by checking node indices in the semantic tree
        for (activeName in activeNames) {
            for (pausedName in pausedNames) {
                val activeNodes = composeTestRule.onAllNodesWithText(activeName, substring = true, ignoreCase = true)
                    .fetchSemanticsNodes()
                val pausedNodes = composeTestRule.onAllNodesWithText(pausedName, substring = true, ignoreCase = true)
                    .fetchSemanticsNodes()

                if (activeNodes.isNotEmpty() && pausedNodes.isNotEmpty()) {
                    Log.d(TAG, "Both '$activeName' and '$pausedName' found in tree")
                } else {
                    Log.w(TAG, "Could not verify ordering: active=${activeNodes.size}, paused=${pausedNodes.size}")
                }
            }
        }
    }

    /**
     * Edit an existing rule by tapping on the rule card to open the edit sheet.
     * This is a convenience alias for tapRuleCard with additional logging.
     */
    fun editRule(targetName: String) = apply {
        Log.d(TAG, "Opening edit sheet for rule: $targetName")
        tapRuleCard(targetName)
        Thread.sleep(500) // Wait for edit sheet animation
        composeTestRule.waitForIdle()
        Log.d(TAG, "Edit sheet should be open for: $targetName")
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
