package com.rasoiai.app.e2e.journeys

import com.rasoiai.app.e2e.flows.FavoritesFlowTest
import com.rasoiai.app.e2e.flows.ChatFlowTest
import com.rasoiai.app.e2e.flows.StatsScreenTest
import com.rasoiai.app.e2e.flows.PantryFlowTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * J10: Exploring App Features
 *
 * Scenario: User browses favorites, chats with AI, checks cooking stats, manages pantry.
 *
 * Flow: Add/remove favorites -> AI chat and recipe suggestions ->
 *       cooking streak and achievements -> pantry items and expiry tracking
 *
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J10_ExploringAppFeaturesSuite
 * ```
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    FavoritesFlowTest::class,
    ChatFlowTest::class,
    StatsScreenTest::class,
    PantryFlowTest::class
)
class J10_ExploringAppFeaturesSuite
