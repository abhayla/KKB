package com.rasoiai.app.e2e.journeys

import com.rasoiai.app.e2e.flows.HouseholdMemberFlowTest
import com.rasoiai.app.e2e.flows.HouseholdSetupFlowTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * J15: Household Setup & Member Management
 *
 * Scenario: User creates a household, manages members, generates invite codes.
 *
 * Flow: Create household -> Verify owner -> Add member by phone -> Generate invite code ->
 *       Update member role -> Leave/transfer ownership
 *
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J15_HouseholdSetupSuite
 * ```
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    HouseholdSetupFlowTest::class,
    HouseholdMemberFlowTest::class
)
class J15_HouseholdSetupSuite
