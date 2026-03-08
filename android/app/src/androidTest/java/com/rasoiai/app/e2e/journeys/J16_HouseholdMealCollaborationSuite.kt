package com.rasoiai.app.e2e.journeys

import com.rasoiai.app.e2e.flows.HouseholdMealPlanFlowTest
import com.rasoiai.app.e2e.flows.HouseholdRecipeRulesFlowTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * J16: Household Meal Collaboration
 *
 * Scenario: Household members manage shared recipe rules and collaborate on meal plans.
 *
 * Flow: Add household recipe rules -> View merged constraints -> Open shared meal plan ->
 *       Mark items cooked/skipped -> View monthly stats
 *
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J16_HouseholdMealCollaborationSuite
 * ```
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    HouseholdRecipeRulesFlowTest::class,
    HouseholdMealPlanFlowTest::class
)
class J16_HouseholdMealCollaborationSuite
