package com.rasoiai.app.e2e.flows

import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.di.FakeAuthRepository
import com.rasoiai.app.e2e.di.FakeGoogleAuthClient
import com.rasoiai.app.e2e.robots.AuthRobot
import com.rasoiai.app.e2e.robots.OnboardingRobot
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test
import javax.inject.Inject

/**
 * Phase 1: Authentication Testing
 *
 * Tests:
 * 1.1 Splash Screen - Launch app, observe splash, verify redirect
 * 1.2 Google OAuth Login - Sign in with Google, verify navigation
 */
@HiltAndroidTest
class AuthFlowTest : BaseE2ETest() {

    private lateinit var authRobot: AuthRobot
    private lateinit var onboardingRobot: OnboardingRobot

    @Inject
    lateinit var fakeGoogleAuthClient: FakeGoogleAuthClient

    @Inject
    lateinit var fakeAuthRepository: FakeAuthRepository

    @Before
    override fun setUp() {
        super.setUp()
        authRobot = AuthRobot(composeTestRule)
        onboardingRobot = OnboardingRobot(composeTestRule)

        // Configure fake auth for successful sign-in by default
        fakeGoogleAuthClient.setSignInSuccess()
        fakeAuthRepository.setAuthSuccess()
    }

    /**
     * Test 1.1: Splash Screen
     *
     * Steps:
     * 1. Launch app
     * 2. Observe splash screen animation (RasoiAI logo + tagline)
     *
     * Expected:
     * - Splash displays for 2-3 seconds
     * - Redirects to Auth screen (new user)
     * - No crash or ANR
     */
    @Test
    fun test_1_1_splashScreen_displaysAndRedirectsToAuth() {
        // Given: App is launched (happens automatically via composeTestRule)

        // When: Splash screen is displayed
        authRobot.assertSplashScreenDisplayed()

        // Then: Wait for splash duration and verify redirect to auth
        waitFor(AuthRobot.SPLASH_DURATION_MS)
        authRobot.waitForAuthScreen(MEDIUM_TIMEOUT)
        authRobot.assertAuthScreenDisplayed()
    }

    /**
     * Test 1.2: Google OAuth Login
     *
     * Steps:
     * 1. Tap "Sign in with Google" button
     * 2. Complete OAuth flow (mocked)
     *
     * Expected:
     * - Successful authentication
     * - Redirects to Onboarding screen (not Home for new user)
     */
    @Test
    fun test_1_2_googleOAuthLogin_navigatesToOnboarding() {
        // Given: Auth screen is displayed
        authRobot.waitForAuthScreen()
        authRobot.assertAuthScreenDisplayed()
        authRobot.assertWelcomeTextDisplayed()
        authRobot.assertGoogleSignInButtonDisplayed()

        // When: User taps Google Sign-In
        authRobot.tapGoogleSignIn()

        // Then: Should navigate to onboarding (new user)
        authRobot.assertNavigatedToOnboarding()
    }

    /**
     * Test: Auth screen displays all expected elements.
     */
    @Test
    fun authScreen_displaysAllElements() {
        // Given: Auth screen is displayed
        authRobot.waitForAuthScreen()

        // Then: All elements should be visible
        authRobot.assertAuthScreenDisplayed()
        authRobot.assertWelcomeTextDisplayed()
        authRobot.assertGoogleSignInButtonDisplayed()
    }

    /**
     * Test: Sign-in failure shows error.
     */
    @Test
    fun signIn_failure_showsError() {
        // Given: Auth is configured to fail
        fakeGoogleAuthClient.setSignInFailure(Exception("Network error"))
        fakeAuthRepository.setAuthFailure()

        authRobot.waitForAuthScreen()

        // When: User attempts sign-in
        authRobot.tapGoogleSignIn()

        // Then: Should remain on auth screen (sign-in failed)
        waitFor(ANIMATION_DURATION)
        authRobot.assertAuthScreenDisplayed()
    }
}
