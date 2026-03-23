package com.rasoiai.app.e2e.journeys

import android.util.Log
import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.robots.HouseholdMembersRobot
import com.rasoiai.app.e2e.robots.HouseholdRobot
import com.rasoiai.app.e2e.robots.RecipeRulesRobot
import com.rasoiai.app.e2e.robots.SettingsRobot
import com.rasoiai.app.e2e.util.BackendTestHelper
import com.rasoiai.app.e2e.util.JourneyStepLogger
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import javax.inject.Inject
import com.rasoiai.data.local.dao.HouseholdDao

/**
 * J16: Household Meal Collaboration (single Activity session)
 *
 * Scenario: User creates a household via backend, navigates to recipe rules
 * to verify browsing, then visits household settings to confirm the household
 * exists with members. Finally, verifies data via backend API calls.
 *
 * Navigation: Home -> Settings -> Recipe Rules -> back -> Settings -> My Household
 *             -> backend verification
 *
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J16_HouseholdMealCollaborationJourney
 * ```
 */
@HiltAndroidTest
class J16_HouseholdMealCollaborationJourney : BaseE2ETest() {

    companion object {
        private const val TAG = "J16_HouseholdCollab"
        private const val TEST_HOUSEHOLD_NAME = "Sharma Kitchen"
    }

    private val logger = JourneyStepLogger("J16")

    private lateinit var homeRobot: HomeRobot
    private lateinit var settingsRobot: SettingsRobot
    private lateinit var householdRobot: HouseholdRobot
    private lateinit var householdMembersRobot: HouseholdMembersRobot
    private lateinit var recipeRulesRobot: RecipeRulesRobot

    @Inject
    lateinit var householdDao: HouseholdDao

    private var authToken: String? = null

    @Before
    override fun setUp() {
        super.setUp()
        setUpAuthenticatedStateWithoutMealPlan()

        homeRobot = HomeRobot(composeTestRule)
        settingsRobot = SettingsRobot(composeTestRule)
        householdRobot = HouseholdRobot(composeTestRule)
        householdMembersRobot = HouseholdMembersRobot(composeTestRule)
        recipeRulesRobot = RecipeRulesRobot(composeTestRule)

        // Get auth token for backend operations
        authToken = runBlocking { userPreferencesDataStore.accessToken.first() }

        // Clean up any existing household from prior test runs
        if (authToken != null) {
            val existing = BackendTestHelper.getMyHousehold(BACKEND_BASE_URL, authToken!!)
            if (existing != null) {
                val existingId = existing.optString("id", "")
                if (existingId.isNotEmpty()) {
                    BackendTestHelper.deactivateHousehold(BACKEND_BASE_URL, authToken!!, existingId)
                    Log.d(TAG, "Cleaned up existing household: $existingId")
                }
            }
        }

        // Create household via backend API (pre-condition for this journey)
        if (authToken != null) {
            val created = BackendTestHelper.createHousehold(
                BACKEND_BASE_URL, authToken!!, TEST_HOUSEHOLD_NAME
            )
            if (created != null) {
                Log.i(TAG, "Pre-created household: id=${created.optString("id")}, name=$TEST_HOUSEHOLD_NAME")
            } else {
                Log.w(TAG, "Failed to pre-create household — journey may fail at household steps")
            }
        }
    }

    @Test
    fun householdMealCollaborationJourney() {
        val totalSteps = 7

        try {
            logger.step(1, totalSteps, "Navigate to recipe rules screen") {
                homeRobot.waitForHomeScreen(60000)
                homeRobot.navigateToSettings()
                settingsRobot.waitForSettingsScreen()
                settingsRobot.navigateToRecipeRules()
                recipeRulesRobot.waitForRecipeRulesScreen()
                recipeRulesRobot.assertRecipeRulesScreenDisplayed()
                Log.i(TAG, "Step 1: Recipe rules screen displayed")
            }

            logger.step(2, totalSteps, "Verify rules can be browsed") {
                // Verify the Rules tab is accessible and screen content loads
                recipeRulesRobot.selectRulesTab()
                composeTestRule.waitForIdle()
                Thread.sleep(500)
                recipeRulesRobot.assertRecipeRulesScreenDisplayed()

                // Switch to Nutrition tab and back to verify tab navigation works
                recipeRulesRobot.selectNutritionTab()
                composeTestRule.waitForIdle()
                Thread.sleep(500)
                recipeRulesRobot.selectRulesTab()
                composeTestRule.waitForIdle()

                Log.i(TAG, "Step 2: Rules tabs browsable (Rules + Nutrition)")
            }

            logger.step(3, totalSteps, "Navigate back to settings") {
                // Press back to return to Settings
                composeTestRule.activityRule.scenario.onActivity { activity ->
                    activity.onBackPressedDispatcher.onBackPressed()
                }
                composeTestRule.waitForIdle()
                Thread.sleep(500)
                settingsRobot.waitForSettingsScreen()
                settingsRobot.assertSettingsScreenDisplayed()
                Log.i(TAG, "Step 3: Back on Settings screen")
            }

            logger.step(4, totalSteps, "Navigate to household settings") {
                settingsRobot.tapSettingItem("My Household")
                householdRobot.waitForHouseholdScreen()
                householdRobot.assertHouseholdScreenDisplayed()
                Log.i(TAG, "Step 4: Household screen displayed")
            }

            logger.step(5, totalSteps, "Verify household exists and shows members") {
                // Household was pre-created in @Before — verify name displayed
                householdRobot.assertHouseholdNameDisplayed(TEST_HOUSEHOLD_NAME)

                // Verify members list is displayed with the owner
                householdMembersRobot.waitForMembersScreen(15000)
                householdMembersRobot.assertMembersListDisplayed()
                householdMembersRobot.assertMemberDisplayed(0)
                householdMembersRobot.assertMemberRole(0, "owner")

                Log.i(TAG, "Step 5: Household '$TEST_HOUSEHOLD_NAME' with owner member verified")
            }

            logger.step(6, totalSteps, "Backend verification: recipe rules API") {
                assertNotNull("Auth token should be available", authToken)

                val rulesResponse = BackendTestHelper.getRecipeRules(
                    BACKEND_BASE_URL, authToken!!
                )
                // Recipe rules endpoint should return successfully (may be empty list)
                // The key assertion is that the API is reachable and returns valid JSON
                assertNotNull(
                    "Recipe rules API should return a response",
                    rulesResponse
                )

                Log.i(TAG, "Step 6: Backend recipe rules API verified — response received")
            }

            logger.step(7, totalSteps, "Backend verification: household data") {
                assertNotNull("Auth token should be available", authToken)

                val fetchedHousehold = BackendTestHelper.getMyHousehold(
                    BACKEND_BASE_URL, authToken!!
                )
                assertNotNull(
                    "Household should be retrievable from backend",
                    fetchedHousehold
                )

                val apiName = fetchedHousehold!!.optString("name", "")
                assertTrue(
                    "Backend household name should match '$TEST_HOUSEHOLD_NAME' but was '$apiName'",
                    apiName == TEST_HOUSEHOLD_NAME
                )

                val householdId = fetchedHousehold.optString("id", "")
                assertTrue("Household ID should not be empty", householdId.isNotEmpty())

                // Verify household has at least one member (the owner)
                val members = fetchedHousehold.optJSONArray("members")
                if (members != null) {
                    assertTrue(
                        "Household should have at least 1 member (owner) but has ${members.length()}",
                        members.length() >= 1
                    )
                    Log.i(TAG, "Step 7: Backend household has ${members.length()} member(s)")
                } else {
                    Log.w(TAG, "Step 7: No 'members' array in response — structure may differ")
                }

                Log.i(TAG, "Step 7: Backend household verification passed — id=$householdId, name=$apiName")
            }
        } finally {
            logger.printSummary()
        }
    }
}
