package com.rasoiai.app.e2e.journeys

import com.rasoiai.app.e2e.flows.HouseholdMealPlanFlowTest
import com.rasoiai.app.e2e.flows.HouseholdNotificationFlowTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * J17: Household Notifications & Awareness
 *
 * Scenario: User checks household notifications and monitors shared meal plan activity.
 *
 * Flow: Open notifications -> Check badge count -> Mark notifications read ->
 *       View shared meal plan -> Return to home
 *
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J17_HouseholdNotificationsSuite
 * ```
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    HouseholdNotificationFlowTest::class,
    HouseholdMealPlanFlowTest::class
)
class J17_HouseholdNotificationsSuite
