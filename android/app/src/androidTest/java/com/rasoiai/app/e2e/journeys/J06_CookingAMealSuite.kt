package com.rasoiai.app.e2e.journeys

import com.rasoiai.app.e2e.flows.HomeScreenComprehensiveTest
import com.rasoiai.app.e2e.flows.CookingModeFlowTest
import com.rasoiai.app.e2e.flows.RecipeInteractionFlowTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * J6: Cooking a Meal
 *
 * Scenario: User picks a recipe from home, views details, enters cooking mode.
 *
 * Flow: Tap recipe from home -> recipe scaling and step-by-step cooking ->
 *       recipe search and catalog browsing
 *
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J06_CookingAMealSuite
 * ```
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    HomeScreenComprehensiveTest::class,
    CookingModeFlowTest::class,
    RecipeInteractionFlowTest::class
)
class J06_CookingAMealSuite
