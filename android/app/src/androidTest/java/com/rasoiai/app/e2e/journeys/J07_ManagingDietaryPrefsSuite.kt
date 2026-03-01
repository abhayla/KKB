package com.rasoiai.app.e2e.journeys

import com.rasoiai.app.e2e.flows.RecipeRulesFlowTest
import com.rasoiai.app.e2e.flows.NutritionGoalsFlowTest
import com.rasoiai.app.e2e.flows.SharmaRecipeRulesVerificationTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * J7: Managing Dietary Preferences
 *
 * Scenario: User configures include/exclude rules and nutrition goals.
 *
 * Flow: Add/delete/toggle include & exclude rules -> nutrition goal CRUD ->
 *       deep persistence verification (Room DB + Backend sync)
 *
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J07_ManagingDietaryPrefsSuite
 * ```
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    RecipeRulesFlowTest::class,
    NutritionGoalsFlowTest::class,
    SharmaRecipeRulesVerificationTest::class
)
class J07_ManagingDietaryPrefsSuite
