package com.rasoiai.app.e2e.journeys

import com.rasoiai.app.e2e.flows.OnboardingNavigationTest
import com.rasoiai.app.e2e.flows.HomeScreenComprehensiveTest
import com.rasoiai.app.e2e.flows.GroceryFlowTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * J13: Returning User Quick Check
 *
 * Scenario: Existing user opens app, checks meals, reviews grocery list.
 *
 * Flow: Returning user bypasses onboarding -> meal card display and day navigation ->
 *       grocery list review
 *
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J13_ReturningUserQuickCheckSuite
 * ```
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    OnboardingNavigationTest::class,
    HomeScreenComprehensiveTest::class,
    GroceryFlowTest::class
)
class J13_ReturningUserQuickCheckSuite
