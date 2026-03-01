package com.rasoiai.app.e2e.journeys

import com.rasoiai.app.e2e.flows.AuthFlowTest
import com.rasoiai.app.e2e.flows.OnboardingFlowTest
import com.rasoiai.app.e2e.flows.OnboardingNavigationTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * J1: First-Time User Gets Started
 *
 * Scenario: Brand new user downloads app, signs up, completes onboarding.
 *
 * Flow: Phone auth sign-up -> 5-step onboarding -> first-time vs returning user routing
 *
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.journeys.J01_FirstTimeUserSuite
 * ```
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    AuthFlowTest::class,
    OnboardingFlowTest::class,
    OnboardingNavigationTest::class
)
class J01_FirstTimeUserSuite
