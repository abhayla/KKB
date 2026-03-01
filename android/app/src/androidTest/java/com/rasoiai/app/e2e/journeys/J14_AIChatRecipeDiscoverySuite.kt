package com.rasoiai.app.e2e.journeys

import com.rasoiai.app.e2e.flows.ChatFlowTest
import com.rasoiai.app.e2e.flows.RecipeInteractionFlowTest
import com.rasoiai.app.e2e.flows.FavoritesFlowTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * J14: AI Chat and Recipe Discovery
 *
 * Scenario: User explores recipes via AI chat and search, saves favorites.
 *
 * Flow: AI chat and recipe suggestions -> recipe search and catalog browsing ->
 *       save and manage favorites
 *
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J14_AIChatRecipeDiscoverySuite
 * ```
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    ChatFlowTest::class,
    RecipeInteractionFlowTest::class,
    FavoritesFlowTest::class
)
class J14_AIChatRecipeDiscoverySuite
