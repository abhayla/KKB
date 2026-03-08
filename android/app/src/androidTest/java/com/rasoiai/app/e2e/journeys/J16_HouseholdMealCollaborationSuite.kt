package com.rasoiai.app.e2e.journeys

import com.rasoiai.app.e2e.flows.HouseholdMealPlanFlowTest
import com.rasoiai.app.e2e.flows.HouseholdRecipeRulesFlowTest
import com.rasoiai.app.e2e.flows.ScopeToggleFlowTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * J16: Household Meal Collaboration
 *
 * Scenario: Household members manage shared recipe rules, collaborate on meal plans,
 * and use the Family/Personal scope toggle across screens.
 *
 * Flow: Add household recipe rules -> View merged constraints -> Open shared meal plan ->
 *       Mark items cooked/skipped -> View monthly stats -> Toggle scope on 5 screens
 *
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J16_HouseholdMealCollaborationSuite
 * ```
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    HouseholdRecipeRulesFlowTest::class,
    HouseholdMealPlanFlowTest::class,
    ScopeToggleFlowTest::class
)
class J16_HouseholdMealCollaborationSuite
