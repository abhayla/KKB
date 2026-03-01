package com.rasoiai.app.e2e.journeys

import com.rasoiai.app.e2e.flows.MealPlanAIVerificationTest
import com.rasoiai.app.e2e.flows.MealPlanGenerationFlowTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * J8: AI Meal Plan Quality Assurance
 *
 * Scenario: Verify AI respects dietary constraints, allergies, and rules in generated plans.
 *
 * Flow: Vegetarian enforcement, allergen exclusion, INCLUDE rule compliance ->
 *       generation API connectivity and progress steps
 *
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J08_AIMealPlanQualitySuite
 * ```
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    MealPlanAIVerificationTest::class,
    MealPlanGenerationFlowTest::class
)
class J08_AIMealPlanQualitySuite
