package com.rasoiai.app.e2e.journeys

import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.robots.SettingsRobot
import com.rasoiai.app.e2e.util.JourneyStepLogger
import dagger.hilt.android.testing.HiltAndroidTest
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

    @Before
    override fun setUp() {
        super.setUp()
        setUpAuthenticatedStateWithoutMealPlan()
        homeRobot = HomeRobot(composeTestRule)
        settingsRobot = SettingsRobot(composeTestRule)
    }

    @Test
    fun familyProfileMgmtJourney() {
        val totalSteps = 6

        try {
            logger.step(1, totalSteps, "Wait for Home") {
                homeRobot.waitForHomeScreen(HOME_SCREEN_TIMEOUT_MS)
                homeRobot.assertHomeScreenDisplayed()
            }

            logger.step(2, totalSteps, "Navigate to Settings") {
                homeRobot.navigateToSettings()
                settingsRobot.waitForSettingsScreen()
                settingsRobot.assertSettingsScreenDisplayed()
            }

            logger.step(3, totalSteps, "Profile section visible") {
                settingsRobot.assertProfileSectionDisplayed()
                settingsRobot.assertEmailDisplayed(activeProfile.email)
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
        } finally {
            logger.printSummary()
        }
    }
}
