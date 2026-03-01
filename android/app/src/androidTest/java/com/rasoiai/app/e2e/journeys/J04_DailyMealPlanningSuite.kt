package com.rasoiai.app.e2e.journeys

import com.rasoiai.app.e2e.flows.HomeScreenComprehensiveTest
import com.rasoiai.app.e2e.flows.RecipeInteractionFlowTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * J4: Daily Meal Planning
 *
 * Scenario: Returning user checks today's meals, swaps/locks items, adds a recipe.
 *
 * Flow: Home screen navigation and meal management -> recipe search and interaction
 *
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J04_DailyMealPlanningSuite
 * ```
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    HomeScreenComprehensiveTest::class,
    RecipeInteractionFlowTest::class
)
class J04_DailyMealPlanningSuite
