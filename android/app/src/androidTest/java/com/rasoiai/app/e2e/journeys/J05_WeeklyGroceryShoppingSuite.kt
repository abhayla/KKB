package com.rasoiai.app.e2e.journeys

import com.rasoiai.app.e2e.flows.HomeScreenComprehensiveTest
import com.rasoiai.app.e2e.flows.GroceryFlowTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * J5: Weekly Grocery Shopping
 *
 * Scenario: User reviews meal plan then generates and uses grocery list.
 *
 * Flow: Review meal cards and day navigation -> grocery list generation and management
 *
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J05_WeeklyGroceryShoppingSuite
 * ```
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    HomeScreenComprehensiveTest::class,
    GroceryFlowTest::class
)
class J05_WeeklyGroceryShoppingSuite
