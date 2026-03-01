package com.rasoiai.app.e2e.journeys

import com.rasoiai.app.e2e.flows.CoreDataFlowTest
import com.rasoiai.app.e2e.flows.MealPlanGenerationFlowTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * J2: New User First Meal Plan
 *
 * Scenario: Fresh user goes through auth + onboarding + generates first meal plan.
 *
 * Flow: Full auth->onboarding->generation->home (CoreDataFlowTest clears state first)
 *       then generation progress steps and API connectivity verification.
 *
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J02_NewUserFirstMealPlanSuite
 * ```
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    CoreDataFlowTest::class,
    MealPlanGenerationFlowTest::class
)
class J02_NewUserFirstMealPlanSuite
