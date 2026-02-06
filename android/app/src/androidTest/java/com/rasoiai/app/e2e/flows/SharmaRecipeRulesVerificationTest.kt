package com.rasoiai.app.e2e.flows

import android.util.Log
import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.base.FrequencyType
import com.rasoiai.app.e2e.base.RuleEnforcement
import com.rasoiai.app.e2e.base.TestDataFactory
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.robots.RecipeRulesRobot
import com.rasoiai.app.e2e.robots.SettingsRobot
import com.rasoiai.app.e2e.util.BackendTestHelper
import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.DayOfWeek
import com.rasoiai.domain.model.PrimaryDiet
import com.rasoiai.domain.model.SpiceLevel
import com.rasoiai.domain.model.UserPreferences
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Requirement: #53 - FR-015: Sharma Family Recipe Rules E2E Verification Test
 *
 * Enters 5 Sharma family recipe rules + 1 nutrition goal through the Recipe Rules UI,
 * then verifies the data persisted correctly to:
 * 1. UI (assertRuleCardDisplayed for each rule)
 * 2. Local Room DB (recipeRulesDao.getAllRules + getAllNutritionGoals)
 * 3. Backend PostgreSQL (GET /api/v1/recipe-rules + /api/v1/nutrition-goals)
 *
 * Note: The UI's AddRuleBottomSheet does NOT show a meal_slot dropdown for INGREDIENT
 * tab rules. This means the backend test's 2 separate Chai rules (BREAKFAST/SNACKS)
 * cannot both be created through the UI. We enter 5 rules instead of 6.
 *
 * Test data source: backend/tests/test_sharma_recipe_rules.py
 */
@HiltAndroidTest
class SharmaRecipeRulesVerificationTest : BaseE2ETest() {

    private lateinit var homeRobot: HomeRobot
    private lateinit var settingsRobot: SettingsRobot
    private lateinit var recipeRulesRobot: RecipeRulesRobot

    companion object {
        private const val TAG = "SharmaRecipeRulesVerify"
        private const val EXPECTED_RULE_COUNT = 5
        private const val EXPECTED_GOAL_COUNT = 1
    }

    @Before
    override fun setUp() {
        super.setUp()
        // Custom auth setup: authenticate + onboard WITHOUT generating a meal plan.
        // generateMealPlan() triggers Gemini AI which blocks uvicorn's single-threaded
        // event loop for ~45s, causing SocketTimeoutException. Recipe Rules tests
        // don't need meal plans — they just need an authenticated, onboarded user.
        setUpAuthenticatedStateWithoutMealPlan()
        clearRecipeRulesAndGoals()  // Prevent duplicate detection from prior runs

        homeRobot = HomeRobot(composeTestRule)
        settingsRobot = SettingsRobot(composeTestRule)
        recipeRulesRobot = RecipeRulesRobot(composeTestRule)
    }

    /**
     * Authenticates and marks user as onboarded WITHOUT generating a meal plan.
     * This avoids the Gemini-induced SocketTimeoutException in setUpAuthenticatedState().
     */
    private fun setUpAuthenticatedStateWithoutMealPlan() {
        val authResult = BackendTestHelper.authenticateWithRetry(
            baseUrl = BACKEND_BASE_URL,
            firebaseToken = "fake-firebase-token",
            maxRetries = 3
        )

        fakeGoogleAuthClient.simulateSignedIn()

        if (authResult != null) {
            runBlocking {
                userPreferencesDataStore.saveAuthTokens(
                    accessToken = authResult.accessToken,
                    refreshToken = "",
                    expiresInSeconds = 3600,
                    userId = authResult.userId
                )
                // Use NON_VEGETARIAN diet to avoid diet conflict warnings
                // when adding Eggs and Chicken as INCLUDE rules (Issue #42 conflict check)
                userPreferencesDataStore.saveOnboardingComplete(
                    UserPreferences(
                        householdSize = 3,
                        familyMembers = emptyList(),
                        primaryDiet = PrimaryDiet.NON_VEGETARIAN,
                        dietaryRestrictions = emptyList(),
                        cuisinePreferences = listOf(CuisineType.NORTH, CuisineType.WEST),
                        spiceLevel = SpiceLevel.MEDIUM,
                        dislikedIngredients = listOf("Karela", "Baingan"),
                        weekdayCookingTimeMinutes = 30,
                        weekendCookingTimeMinutes = 60,
                        busyDays = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                        itemsPerMeal = 2,
                        strictAllergenMode = true,
                        strictDietaryMode = true,
                        allowRecipeRepeat = false
                    )
                )
            }
            Log.i(TAG, "Authenticated without meal plan: userId=${authResult.userId}")
        } else {
            Log.e(TAG, "Failed to authenticate with backend")
            BackendTestHelper.diagnoseConnection(BACKEND_BASE_URL)
        }
    }

    /**
     * Full E2E test: enter 5 recipe rules + 1 nutrition goal → verify UI + Room + Backend.
     *
     * Rules entered:
     * 1. Chai: INCLUDE, DAILY, REQUIRED
     * 2. Moringa: INCLUDE, 1x/week, PREFERRED
     * 3. Paneer: EXCLUDE, NEVER, REQUIRED
     * 4. Eggs: INCLUDE, 4x/week, PREFERRED
     * 5. Chicken: INCLUDE, 2x/week, PREFERRED
     * Goal: Green Leafy, target=5, PREFERRED
     */
    @Test
    fun test_sharma_recipe_rules_persist_to_room_and_backend() {
        // ==================== NAVIGATE TO RECIPE RULES ====================
        Log.i(TAG, "Navigating: Home → Settings → Recipe Rules")
        homeRobot.waitForHomeScreen(LONG_TIMEOUT)
        homeRobot.navigateToSettings()
        settingsRobot.waitForSettingsScreen()
        settingsRobot.navigateToRecipeRules()
        recipeRulesRobot.waitForRecipeRulesScreen()
        recipeRulesRobot.assertRecipeRulesScreenDisplayed()

        // ==================== ENTER 5 INGREDIENT RULES ====================
        Log.i(TAG, "Entering Rule 1/5: Chai (INCLUDE, DAILY, REQUIRED)")
        recipeRulesRobot.addIngredientIncludeRule(
            ingredientName = "Chai",
            frequencyType = FrequencyType.DAILY,
            enforcement = RuleEnforcement.REQUIRED
        )

        Log.i(TAG, "Entering Rule 2/5: Moringa (INCLUDE, 1x/week, PREFERRED)")
        recipeRulesRobot.addIngredientIncludeRule(
            ingredientName = "Moringa",
            frequencyType = FrequencyType.TIMES_PER_WEEK,
            frequencyCount = 1,
            enforcement = RuleEnforcement.PREFERRED
        )

        Log.i(TAG, "Entering Rule 3/5: Paneer (EXCLUDE, NEVER, REQUIRED)")
        recipeRulesRobot.addIngredientExcludeRule("Paneer")

        Log.i(TAG, "Entering Rule 4/5: Eggs (INCLUDE, 4x/week, PREFERRED)")
        recipeRulesRobot.addIngredientIncludeRule(
            ingredientName = "Eggs",
            frequencyType = FrequencyType.TIMES_PER_WEEK,
            frequencyCount = 4,
            enforcement = RuleEnforcement.PREFERRED
        )

        Log.i(TAG, "Entering Rule 5/5: Chicken (INCLUDE, 2x/week, PREFERRED)")
        recipeRulesRobot.addIngredientIncludeRule(
            ingredientName = "Chicken",
            frequencyType = FrequencyType.TIMES_PER_WEEK,
            frequencyCount = 2,
            enforcement = RuleEnforcement.PREFERRED
        )

        // ==================== ENTER 1 NUTRITION GOAL ====================
        Log.i(TAG, "Entering Nutrition Goal: Green Leafy (target=5, PREFERRED)")
        recipeRulesRobot.addNutritionGoal(TestDataFactory.RecipeRules.greenLeafyGoal)

        // ==================== VERIFY UI ====================
        Log.i(TAG, "Verifying UI: all rules displayed on Rules tab")
        recipeRulesRobot.selectRulesTab()
        composeTestRule.waitForIdle()
        Thread.sleep(2000) // Wait for tab content to load and rule cards to render
        composeTestRule.waitForIdle()

        recipeRulesRobot.assertRuleCardDisplayed("Chai")
        recipeRulesRobot.assertRuleCardDisplayed("Moringa")
        recipeRulesRobot.assertRuleCardDisplayed("Paneer")
        recipeRulesRobot.assertRuleCardDisplayed("Eggs")
        recipeRulesRobot.assertRuleCardDisplayed("Chicken")
        Log.i(TAG, "UI verification PASSED — all 5 rules displayed")

        Log.i(TAG, "Verifying UI: nutrition goal displayed on Nutrition tab")
        recipeRulesRobot.selectNutritionTab()
        composeTestRule.waitForIdle()
        Thread.sleep(2000)
        composeTestRule.waitForIdle()

        recipeRulesRobot.assertRuleCardDisplayed("Green Leafy")
        Log.i(TAG, "UI verification PASSED — nutrition goal displayed")

        // ==================== VERIFY ROOM DB ====================
        Log.i(TAG, "Verifying Room DB persistence")
        verifyRoomDbPersistence()

        // ==================== VERIFY BACKEND ====================
        Log.i(TAG, "Verifying backend persistence")
        verifyBackendPersistence()

        Log.i(TAG, "ALL VERIFICATIONS PASSED")
    }

    // ==================== Room DB Verification ====================

    private fun verifyRoomDbPersistence() {
        val rules = runBlocking { recipeRulesDao.getAllRules().first() }
        Log.i(TAG, "Room DB: found ${rules.size} rules")

        // We expect exactly 5 rules since we clear before each run
        assertEquals(
            "Room DB should have exactly $EXPECTED_RULE_COUNT rules, found ${rules.size}",
            EXPECTED_RULE_COUNT, rules.size
        )

        // Verify each rule exists with correct fields
        verifyRoomRule(rules, "Chai", "INGREDIENT", "INCLUDE", "DAILY", null, "REQUIRED")
        verifyRoomRule(rules, "Moringa", "INGREDIENT", "INCLUDE", "TIMES_PER_WEEK", 1, "PREFERRED")
        verifyRoomRule(rules, "Paneer", "INGREDIENT", "EXCLUDE", "NEVER", null, "REQUIRED")
        verifyRoomRule(rules, "Eggs", "INGREDIENT", "INCLUDE", "TIMES_PER_WEEK", 4, "PREFERRED")
        verifyRoomRule(rules, "Chicken", "INGREDIENT", "INCLUDE", "TIMES_PER_WEEK", 2, "PREFERRED")

        Log.i(TAG, "Room DB recipe rules verification PASSED")

        // Verify nutrition goals
        val goals = runBlocking { recipeRulesDao.getAllNutritionGoals().first() }
        Log.i(TAG, "Room DB: found ${goals.size} nutrition goals")

        assertEquals(
            "Room DB should have exactly $EXPECTED_GOAL_COUNT nutrition goal(s), found ${goals.size}",
            EXPECTED_GOAL_COUNT, goals.size
        )

        val greenLeafyGoal = goals.find {
            it.foodCategory.equals("green_leafy", ignoreCase = true) ||
            it.foodCategory.equals("GREEN_LEAFY", ignoreCase = true)
        }
        assertNotNull("Room DB should have a Green Leafy nutrition goal", greenLeafyGoal)
        assertEquals("Weekly target should be 5", 5, greenLeafyGoal!!.weeklyTarget)
        assertEquals(
            "Enforcement should be PREFERRED",
            "PREFERRED", greenLeafyGoal.enforcement.uppercase()
        )
        assertTrue("Goal should be active", greenLeafyGoal.isActive)

        Log.i(TAG, "Room DB nutrition goals verification PASSED")
    }

    private fun verifyRoomRule(
        rules: List<com.rasoiai.data.local.entity.RecipeRuleEntity>,
        targetName: String,
        expectedType: String,
        expectedAction: String,
        expectedFrequencyType: String,
        expectedFrequencyCount: Int?,
        expectedEnforcement: String
    ) {
        val rule = rules.find { it.targetName.equals(targetName, ignoreCase = true) }
        assertNotNull("Room DB should have rule for '$targetName'", rule)
        rule!!

        assertEquals(
            "$targetName: type should be $expectedType",
            expectedType, rule.type.uppercase()
        )
        assertEquals(
            "$targetName: action should be $expectedAction",
            expectedAction, rule.action.uppercase()
        )
        assertEquals(
            "$targetName: frequencyType should be $expectedFrequencyType",
            expectedFrequencyType, rule.frequencyType.uppercase()
        )
        if (expectedFrequencyCount != null) {
            assertEquals(
                "$targetName: frequencyCount should be $expectedFrequencyCount",
                expectedFrequencyCount, rule.frequencyCount
            )
        }
        assertEquals(
            "$targetName: enforcement should be $expectedEnforcement",
            expectedEnforcement, rule.enforcement.uppercase()
        )
        assertTrue("$targetName: rule should be active", rule.isActive)

        Log.d(TAG, "Room rule verified: $targetName ✓")
    }

    // ==================== Backend Verification ====================

    private fun verifyBackendPersistence() {
        val authToken = runBlocking {
            userPreferencesDataStore.accessToken.first()
        }
        assertNotNull("Auth token should be available", authToken)

        // Wait for backend to be responsive
        waitForBackendAvailable()

        // Verify recipe rules
        verifyBackendRecipeRules(authToken!!)

        // Verify nutrition goals
        verifyBackendNutritionGoals(authToken)
    }

    private fun verifyBackendRecipeRules(authToken: String) {
        val rulesResponse = BackendTestHelper.getRecipeRules(BACKEND_BASE_URL, authToken)
        assertNotNull("Backend should return recipe rules", rulesResponse)

        val totalCount = rulesResponse!!.getInt("total_count")
        Log.i(TAG, "Backend: found $totalCount recipe rules")
        assertTrue(
            "Backend should have at least $EXPECTED_RULE_COUNT rules, found $totalCount",
            totalCount >= EXPECTED_RULE_COUNT
        )

        val rulesArray = rulesResponse.getJSONArray("rules")

        verifyBackendRule(rulesArray, "Chai", "INGREDIENT", "INCLUDE", "DAILY", null, "REQUIRED")
        verifyBackendRule(rulesArray, "Moringa", "INGREDIENT", "INCLUDE", "TIMES_PER_WEEK", 1, "PREFERRED")
        verifyBackendRule(rulesArray, "Paneer", "INGREDIENT", "EXCLUDE", "NEVER", null, "REQUIRED")
        verifyBackendRule(rulesArray, "Eggs", "INGREDIENT", "INCLUDE", "TIMES_PER_WEEK", 4, "PREFERRED")
        verifyBackendRule(rulesArray, "Chicken", "INGREDIENT", "INCLUDE", "TIMES_PER_WEEK", 2, "PREFERRED")

        Log.i(TAG, "Backend recipe rules verification PASSED")
    }

    private fun verifyBackendRule(
        rulesArray: JSONArray,
        targetName: String,
        expectedTargetType: String,
        expectedAction: String,
        expectedFrequencyType: String,
        expectedFrequencyCount: Int?,
        expectedEnforcement: String
    ) {
        val rule = findJsonObjectByField(rulesArray, "target_name", targetName)
        assertNotNull("Backend should have rule for '$targetName'", rule)
        rule!!

        assertEquals(
            "$targetName: target_type should be $expectedTargetType",
            expectedTargetType, rule.getString("target_type").uppercase()
        )
        assertEquals(
            "$targetName: action should be $expectedAction",
            expectedAction, rule.getString("action").uppercase()
        )
        assertEquals(
            "$targetName: frequency_type should be $expectedFrequencyType",
            expectedFrequencyType, rule.getString("frequency_type").uppercase()
        )
        if (expectedFrequencyCount != null) {
            assertEquals(
                "$targetName: frequency_count should be $expectedFrequencyCount",
                expectedFrequencyCount, rule.getInt("frequency_count")
            )
        }
        assertEquals(
            "$targetName: enforcement should be $expectedEnforcement",
            expectedEnforcement, rule.getString("enforcement").uppercase()
        )

        Log.d(TAG, "Backend rule verified: $targetName ✓")
    }

    private fun verifyBackendNutritionGoals(authToken: String) {
        val goalsResponse = BackendTestHelper.getNutritionGoals(BACKEND_BASE_URL, authToken)
        assertNotNull("Backend should return nutrition goals", goalsResponse)

        val totalCount = goalsResponse!!.getInt("total_count")
        Log.i(TAG, "Backend: found $totalCount nutrition goals")
        assertTrue(
            "Backend should have at least $EXPECTED_GOAL_COUNT goal(s), found $totalCount",
            totalCount >= EXPECTED_GOAL_COUNT
        )

        val goalsArray = goalsResponse.getJSONArray("goals")

        // Find the green leafy goal (case-insensitive match)
        var greenLeafyGoal: JSONObject? = null
        for (i in 0 until goalsArray.length()) {
            val goal = goalsArray.getJSONObject(i)
            val category = goal.getString("food_category").uppercase()
            if (category.contains("GREEN") || category.contains("LEAFY")) {
                greenLeafyGoal = goal
                break
            }
        }
        assertNotNull("Backend should have a Green Leafy nutrition goal", greenLeafyGoal)

        assertEquals(
            "Green Leafy: weekly_target should be 5",
            5, greenLeafyGoal!!.getInt("weekly_target")
        )
        assertEquals(
            "Green Leafy: enforcement should be PREFERRED",
            "PREFERRED", greenLeafyGoal.getString("enforcement").uppercase()
        )

        Log.i(TAG, "Backend nutrition goals verification PASSED")
    }

    // ==================== Utility Methods ====================

    private fun waitForBackendAvailable(maxWaitSeconds: Int = 120) {
        Log.i(TAG, "Waiting for backend to become responsive...")
        val start = System.currentTimeMillis()
        var attempt = 0
        while (System.currentTimeMillis() - start < maxWaitSeconds * 1000L) {
            attempt++
            if (BackendTestHelper.isBackendHealthy(BACKEND_BASE_URL)) {
                val elapsed = (System.currentTimeMillis() - start) / 1000
                Log.i(TAG, "Backend responsive after ${elapsed}s (attempt $attempt)")
                return
            }
            Thread.sleep(3000)
        }
        Log.w(TAG, "Backend still unresponsive after ${maxWaitSeconds}s, proceeding anyway")
    }

    private fun findJsonObjectByField(
        array: JSONArray,
        fieldName: String,
        fieldValue: String
    ): JSONObject? {
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            if (obj.getString(fieldName).equals(fieldValue, ignoreCase = true)) {
                return obj
            }
        }
        return null
    }
}
