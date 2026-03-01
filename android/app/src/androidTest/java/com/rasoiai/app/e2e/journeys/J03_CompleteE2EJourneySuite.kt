package com.rasoiai.app.e2e.journeys

import com.rasoiai.app.e2e.flows.FullJourneyFlowTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * J3: Complete End-to-End Journey
 *
 * Scenario: Full app walkthrough -- auth through every major screen.
 *
 * Flow: 13-phase deep journey covering Auth -> Onboarding -> MealGen1 -> Home1 ->
 *       RecipeRules -> MealGen2 -> Home2 -> RecipeDetail -> Grocery -> Favorites ->
 *       Chat -> Stats -> Settings
 *
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J03_CompleteE2EJourneySuite
 * ```
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    FullJourneyFlowTest::class
)
class J03_CompleteE2EJourneySuite
