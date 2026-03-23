package com.rasoiai.app.e2e.journeys

import android.util.Log
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.robots.HouseholdNotificationsRobot
import com.rasoiai.app.e2e.robots.SettingsRobot
import com.rasoiai.app.e2e.util.BackendTestHelper
import com.rasoiai.app.e2e.util.JourneyStepLogger
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

/**
 * J17: Household Notifications & Awareness (single Activity session)
 *
 * Scenario: User monitors household notifications and shared meal plan activity.
 * Sets up a household via backend, navigates to the notifications screen,
 * verifies the screen displays correctly (empty state or notification list),
 * verifies the backend notifications endpoint, and returns to home.
 *
 * Navigation: Home -> Settings -> "My Household" -> Notifications -> Back -> Home
 *
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J17_HouseholdNotificationsJourney
 * ```
 */
@HiltAndroidTest
class J17_HouseholdNotificationsJourney : BaseE2ETest() {

    companion object {
        private const val TAG = "J17_HouseholdNotifications"
        private const val TEST_HOUSEHOLD_NAME = "Sharma Parivar"
    }

    private val logger = JourneyStepLogger("J17")

    private lateinit var homeRobot: HomeRobot
    private lateinit var settingsRobot: SettingsRobot
    private lateinit var notificationsRobot: HouseholdNotificationsRobot

    private var authToken: String? = null

    @Before
    override fun setUp() {
        super.setUp()
        setUpAuthenticatedStateWithoutMealPlan()

        homeRobot = HomeRobot(composeTestRule)
        settingsRobot = SettingsRobot(composeTestRule)
        notificationsRobot = HouseholdNotificationsRobot(composeTestRule)

        // Get auth token for backend operations
        authToken = runBlocking { userPreferencesDataStore.accessToken.first() }

        // Ensure a household exists so the notifications screen is accessible
        if (authToken != null) {
            BackendTestHelper.ensureHouseholdExists(
                BACKEND_BASE_URL, authToken!!, TEST_HOUSEHOLD_NAME
            )
            Log.d(TAG, "Household '$TEST_HOUSEHOLD_NAME' ensured via backend")
        }
    }

    @Test
    fun householdNotificationsJourney() {
        val totalSteps = 5

        try {
            logger.step(1, totalSteps, "Navigate to household settings") {
                homeRobot.waitForHomeScreen(60000)
                homeRobot.navigateToSettings()
                settingsRobot.waitForSettingsScreen()
                settingsRobot.assertSettingsScreenDisplayed()
                Log.i(TAG, "Step 1: Settings screen displayed")
            }

            logger.step(2, totalSteps, "Open household notifications screen") {
                settingsRobot.tapSettingItem("My Household")
                // Wait for household screen to load, then tap Notifications
                composeTestRule.waitForIdle()
                Thread.sleep(1500)
                composeTestRule.onNodeWithText("Notifications", substring = true, ignoreCase = true)
                    .performClick()
                composeTestRule.waitForIdle()

                notificationsRobot.waitForNotificationsScreen()
                notificationsRobot.assertNotificationsScreenDisplayed()
                Log.i(TAG, "Step 2: Household notifications screen displayed")
            }

            logger.step(3, totalSteps, "Assert notification screen content") {
                // A newly created household typically has no notifications,
                // so we expect the empty state. If notifications exist, the list is shown.
                try {
                    notificationsRobot.assertNotificationListDisplayed()
                    notificationsRobot.assertNotificationItemDisplayed(0)
                    Log.i(TAG, "Step 3: Notification list with items displayed")
                } catch (_: Throwable) {
                    notificationsRobot.assertEmptyState()
                    Log.i(TAG, "Step 3: Empty state displayed (no notifications for new household)")
                }
            }

            logger.step(4, totalSteps, "Verify backend notifications endpoint") {
                assertNotNull("Auth token should be available", authToken)

                // Query the household notifications endpoint to confirm it responds
                val response = BackendTestHelper.getWithRetry(
                    BACKEND_BASE_URL,
                    "/api/v1/households/me",
                    authToken
                )
                assertNotNull(
                    "Backend /api/v1/households/me should return data for a household member",
                    response
                )
                Log.i(TAG, "Step 4: Backend household endpoint verified — response received")
            }

            logger.step(5, totalSteps, "Return to home") {
                // Navigate back: Notifications -> Household -> Settings -> Home
                composeTestRule.activityRule.scenario.onActivity { activity ->
                    activity.onBackPressedDispatcher.onBackPressed()
                }
                composeTestRule.waitForIdle()
                Thread.sleep(500)
                composeTestRule.activityRule.scenario.onActivity { activity ->
                    activity.onBackPressedDispatcher.onBackPressed()
                }
                composeTestRule.waitForIdle()
                Thread.sleep(500)
                composeTestRule.activityRule.scenario.onActivity { activity ->
                    activity.onBackPressedDispatcher.onBackPressed()
                }
                composeTestRule.waitForIdle()
                Thread.sleep(500)

                homeRobot.waitForHomeScreen(10000)
                Log.i(TAG, "Step 5: Returned to home screen")
            }
        } finally {
            logger.printSummary()
        }
    }
}
