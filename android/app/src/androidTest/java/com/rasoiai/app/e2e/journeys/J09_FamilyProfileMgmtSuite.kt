package com.rasoiai.app.e2e.journeys

import com.rasoiai.app.e2e.flows.SharmaOnboardingVerificationTest
import com.rasoiai.app.e2e.flows.FamilyProfileFlowTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * J9: Family Profile Management
 *
 * Scenario: User manages family members, verifies data persists across onboarding and settings.
 *
 * Flow: DataStore + Backend persistence of Sharma family members ->
 *       family member CRUD and preferences sync
 *
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J09_FamilyProfileMgmtSuite
 * ```
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    SharmaOnboardingVerificationTest::class,
    FamilyProfileFlowTest::class
)
class J09_FamilyProfileMgmtSuite
