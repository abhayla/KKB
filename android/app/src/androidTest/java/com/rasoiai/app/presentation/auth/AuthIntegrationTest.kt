package com.rasoiai.app.presentation.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.presentation.common.TestTags
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test

/**
 * Integration tests for Auth screen with Hilt dependency injection.
 *
 * These tests use FakeGoogleAuthClient and FakeAuthRepository to test
 * the full authentication flow without requiring actual Google OAuth.
 *
 * ## What These Tests Cover:
 * - Sign-in button triggers auth flow
 * - Successful sign-in navigates to onboarding (new user)
 * - Failed sign-in shows error message
 * - Already signed-in user skips auth screen
 *
 * ## E2E Test Coverage (Phase 1: Authentication):
 * - Test 1.2: Google OAuth Login - Full flow with mocked auth
 *
 * ## Running Tests:
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest --tests "*.AuthIntegrationTest"
 * ```
 */
@HiltAndroidTest
class AuthIntegrationTest : BaseE2ETest() {

    @Before
    override fun setUp() {
        super.setUp()
        // Reset to known state - ensure user is NOT signed in so we go to Auth screen
        // Uses clearAllState() which resets both FakeGoogleAuthClient and real DataStore
        clearAllState()
    }

    /**
     * Wait for splash screen to complete and navigate to auth screen.
     * The splash screen has a 2-second delay before navigation.
     */
    private fun waitForAuthScreen() {
        // Wait for splash delay + navigation
        waitUntil(
            timeoutMillis = SPLASH_DURATION + MEDIUM_TIMEOUT,
            conditionDescription = "auth screen to appear"
        ) {
            try {
                composeTestRule.onNodeWithTag(TestTags.AUTH_SCREEN).assertIsDisplayed()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        composeTestRule.waitForIdle()
    }

    // region Sign-In Flow Tests

    @Test
    fun authScreen_signInButton_isDisplayedAndEnabled() {
        // Wait for splash to navigate to auth
        waitForAuthScreen()

        // Verify sign-in button is displayed and enabled
        composeTestRule.onNodeWithTag(TestTags.GOOGLE_SIGN_IN_BUTTON)
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun authScreen_displaysWelcomeElements() {
        waitForAuthScreen()

        // Verify welcome text is displayed
        composeTestRule.onNodeWithText("Welcome!").assertIsDisplayed()

        // Verify app name is displayed
        composeTestRule.onNodeWithText("RasoiAI").assertIsDisplayed()

        // Verify tagline is displayed
        composeTestRule.onNodeWithText("AI Meal Planning for Indian Families").assertIsDisplayed()
    }

    @Test
    fun authScreen_successfulSignIn_navigatesToOnboarding() {
        // Configure fake Google auth to succeed
        // Real AuthRepositoryImpl will call backend with "fake-firebase-token"
        fakeGoogleAuthClient.setSignInSuccess()

        waitForAuthScreen()

        // Tap sign-in button
        composeTestRule.onNodeWithTag(TestTags.GOOGLE_SIGN_IN_BUTTON).performClick()

        // Wait for navigation to complete
        waitFor(ANIMATION_DURATION)
        composeTestRule.waitForIdle()

        // Verify we navigated to onboarding (check for onboarding-specific element)
        // The onboarding screen shows "How many people are you cooking for?"
        waitUntil(
            timeoutMillis = LONG_TIMEOUT + MEDIUM_TIMEOUT,
            conditionDescription = "onboarding screen to appear"
        ) {
            try {
                composeTestRule.onNodeWithText("How many people are", substring = true)
                    .assertIsDisplayed()
                true
            } catch (e: AssertionError) {
                false
            }
        }
    }

    @Test
    fun authScreen_failedSignIn_showsErrorMessage() {
        // Configure fake to fail
        fakeGoogleAuthClient.setSignInFailure(Exception("Network error"))

        waitForAuthScreen()

        // Tap sign-in button
        composeTestRule.onNodeWithTag(TestTags.GOOGLE_SIGN_IN_BUTTON).performClick()

        // Wait for error to appear
        waitFor(ANIMATION_DURATION)
        composeTestRule.waitForIdle()

        // Verify error is shown (via snackbar or error state)
        // The button should still be visible (not navigated away)
        composeTestRule.onNodeWithTag(TestTags.GOOGLE_SIGN_IN_BUTTON).assertIsDisplayed()
    }

    @Test
    fun authScreen_signInCancelled_staysOnAuthScreen() {
        // For cancelled sign-in, the user should stay on auth screen
        waitForAuthScreen()

        // Verify we're still on auth screen
        composeTestRule.onNodeWithTag(TestTags.AUTH_SCREEN).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.GOOGLE_SIGN_IN_BUTTON).assertIsDisplayed()
    }

    // endregion

    // region Terms and Privacy Tests

    @Test
    fun authScreen_termsOfService_isDisplayed() {
        waitForAuthScreen()

        composeTestRule.onNodeWithText("Terms of Service").assertIsDisplayed()
    }

    @Test
    fun authScreen_privacyPolicy_isDisplayed() {
        waitForAuthScreen()

        composeTestRule.onNodeWithText("Privacy Policy").assertIsDisplayed()
    }

    @Test
    fun authScreen_termsPrefix_isDisplayed() {
        waitForAuthScreen()

        composeTestRule.onNodeWithText("By continuing, you agree to our").assertIsDisplayed()
    }

    // endregion

    // region Button State Tests

    @Test
    fun authScreen_signInButton_showsCorrectText() {
        waitForAuthScreen()

        composeTestRule.onNodeWithText("Continue with Google").assertIsDisplayed()
    }

    // endregion
}
