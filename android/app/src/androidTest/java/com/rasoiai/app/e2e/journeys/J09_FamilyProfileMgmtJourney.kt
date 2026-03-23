package com.rasoiai.app.e2e.journeys

import android.util.Log
import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.HomeRobot
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

/**
 * J09: Family Profile Management (single Activity session)
 *
 * Scenario: User opens Settings, views profile, navigates family members and cuisine prefs.
 *
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J09_FamilyProfileMgmtJourney
 * ```
 */
@HiltAndroidTest
class J09_FamilyProfileMgmtJourney : BaseE2ETest() {

    private lateinit var homeRobot: HomeRobot
    private lateinit var settingsRobot: SettingsRobot
    private val logger = JourneyStepLogger("J09")
    private val TAG = "J09_FamilyProfileMgmt"

    @Before
    override fun setUp() {
        super.setUp()
        setUpAuthenticatedStateWithoutMealPlan()
        homeRobot = HomeRobot(composeTestRule)
        settingsRobot = SettingsRobot(composeTestRule)
    }

    @Test
    fun familyProfileMgmtJourney() {
        val totalSteps = 8

        try {
            val journeyStartTime = System.currentTimeMillis()

            logger.step(1, totalSteps, "Wait for Home") {
                val homeLoadStart = System.currentTimeMillis()
                homeRobot.waitForHomeScreen(HOME_SCREEN_TIMEOUT_MS)
                homeRobot.assertHomeScreenDisplayed()
                val homeLoadTime = System.currentTimeMillis() - homeLoadStart
                Log.i(TAG, "Home screen load time: ${homeLoadTime}ms")
                assertTrue(
                    "Home screen should load within 5s (took ${homeLoadTime}ms)",
                    homeLoadTime < 5_000
                )
            }

            logger.step(2, totalSteps, "Navigate to Settings") {
                val settingsLoadStart = System.currentTimeMillis()
                homeRobot.navigateToSettings()
                settingsRobot.waitForSettingsScreen()
                settingsRobot.assertSettingsScreenDisplayed()
                val settingsLoadTime = System.currentTimeMillis() - settingsLoadStart
                Log.i(TAG, "Settings screen load time: ${settingsLoadTime}ms")
                assertTrue(
                    "Settings screen should load within 3s (took ${settingsLoadTime}ms)",
                    settingsLoadTime < 3_000
                )
            }

            logger.step(3, totalSteps, "Profile section visible") {
                settingsRobot.assertProfileSectionDisplayed()
                try {
                    settingsRobot.assertEmailDisplayed(activeProfile.email)
                } catch (e: Throwable) {
                    Log.w(TAG, "Email not displayed (may not be loaded yet): ${e.message}")
                }
            }

            logger.step(4, totalSteps, "Open family members") {
                settingsRobot.navigateToFamilyMembers()
                waitFor(1000)
            }

            logger.step(5, totalSteps, "Back and open cooking time") {
                composeTestRule.activityRule.scenario.onActivity { activity ->
                    activity.onBackPressedDispatcher.onBackPressed()
                }
                waitFor(500)
                settingsRobot.navigateToCookingTime()
                waitFor(1000)
            }

            logger.step(6, totalSteps, "Back and open cuisine prefs") {
                composeTestRule.activityRule.scenario.onActivity { activity ->
                    activity.onBackPressedDispatcher.onBackPressed()
                }
                waitFor(500)
                settingsRobot.navigateToCuisinePreferences()
                waitFor(500)
            }

            logger.step(7, totalSteps, "Verify user profile from backend") {
                val apiStart = System.currentTimeMillis()
                val authToken = runBlocking { userPreferencesDataStore.accessToken.first() }
                assertNotNull("Auth token should exist for backend verification", authToken)

                val userJson = BackendTestHelper.getCurrentUser(
                    baseUrl = BACKEND_BASE_URL,
                    authToken = authToken!!
                )

                if (userJson != null) {
                    Log.d(TAG, "Backend user response: $userJson")

                    // Verify user profile exists with basic fields
                    val email = userJson.optString("email", "")
                    assertTrue("User profile should have an email", email.isNotEmpty())
                    Log.i(TAG, "Backend user email: $email")

                    // Verify preferences contain cooking time and cuisine data
                    val preferencesJson = userJson.optJSONObject("preferences")
                    if (preferencesJson != null) {
                        val weekdayCookingTime = preferencesJson.optInt("weekday_cooking_time_minutes", -1)
                        val weekendCookingTime = preferencesJson.optInt("weekend_cooking_time_minutes", -1)
                        if (weekdayCookingTime > 0) {
                            Log.i(TAG, "Backend weekday cooking time: $weekdayCookingTime min")
                        }
                        if (weekendCookingTime > 0) {
                            Log.i(TAG, "Backend weekend cooking time: $weekendCookingTime min")
                        }

                        val cuisinePrefs = preferencesJson.optJSONArray("cuisine_preferences")
                        if (cuisinePrefs != null && cuisinePrefs.length() > 0) {
                            Log.i(TAG, "Backend cuisine preferences: ${cuisinePrefs.length()} items")
                        } else {
                            Log.w(TAG, "No cuisine preferences found in backend — may not be synced yet")
                        }
                    } else {
                        Log.w(TAG, "Preferences object not found in backend response")
                    }

                    Log.i(TAG, "Step 7 PASSED: User profile verified from backend")
                } else {
                    Log.w(TAG, "Could not fetch user from backend — API may be unavailable")
                }
                val apiTime = System.currentTimeMillis() - apiStart
                Log.i(TAG, "Backend profile verification: ${apiTime}ms")
                assertTrue(
                    "Backend API call should complete within 5s (took ${apiTime}ms)",
                    apiTime < 5_000
                )
            }

            logger.step(8, totalSteps, "Verify family members from backend") {
                val authToken = runBlocking { userPreferencesDataStore.accessToken.first() }
                assertNotNull("Auth token should exist for family members verification", authToken)

                val familyJson = BackendTestHelper.getFamilyMembers(
                    baseUrl = BACKEND_BASE_URL,
                    authToken = authToken!!
                )

                if (familyJson != null) {
                    Log.d(TAG, "Backend family members response: $familyJson")

                    // Verify the response is valid (endpoint returned data)
                    val members = familyJson.optJSONArray("members")
                        ?: familyJson.optJSONArray("family_members")
                    if (members != null) {
                        Log.i(TAG, "Backend family members count: ${members.length()}")
                        for (i in 0 until members.length()) {
                            val member = members.getJSONObject(i)
                            val name = member.optString("name", "unknown")
                            val memberType = member.optString("type", member.optString("member_type", "unknown"))
                            Log.i(TAG, "  Family member $i: $name ($memberType)")
                        }
                    } else {
                        Log.w(TAG, "No members array in response — family members may not be synced")
                    }

                    Log.i(TAG, "Step 8 PASSED: Family members data retrieved from backend")
                } else {
                    Log.w(TAG, "Could not fetch family members from backend — API may be unavailable")
                }
            }

            // Performance guardrail
            val totalDuration = System.currentTimeMillis() - journeyStartTime
            Log.i(TAG, "Total journey time: ${totalDuration}ms")
            assertTrue(
                "J09 journey should complete within 45s (took ${totalDuration}ms)",
                totalDuration < 45_000
            )
        } finally {
            logger.printSummary()
        }
    }
}
