package com.rasoiai.app.e2e.journeys

import com.rasoiai.app.e2e.flows.OfflineFlowTest
import com.rasoiai.app.e2e.flows.EdgeCasesTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * J12: Offline and Error Resilience
 *
 * Scenario: User encounters network issues, app handles errors gracefully.
 *
 * Flow: Cached data access and local mutations -> error handling and validation
 *
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J12_OfflineErrorResilienceSuite
 * ```
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    OfflineFlowTest::class,
    EdgeCasesTest::class
)
class J12_OfflineErrorResilienceSuite
