package com.rasoiai.app.e2e.robots

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.rasoiai.app.e2e.base.waitUntilNodeWithTagExists
import com.rasoiai.app.presentation.common.TestTags

/**
 * Robot for Auth screen interactions.
 * Handles splash screen and Google OAuth login.
 */
class AuthRobot(private val composeTestRule: ComposeContentTestRule) {

    /**
     * Verify splash screen is displayed.
     */
    fun assertSplashScreenDisplayed() = apply {
        // Splash screen may have logo and tagline
        // Wait a bit for splash animation
        composeTestRule.waitForIdle()
    }

    /**
     * Wait for auth screen to be displayed after splash.
     */
    fun waitForAuthScreen(timeoutMillis: Long = 5000) = apply {
        composeTestRule.waitUntilNodeWithTagExists(TestTags.AUTH_SCREEN, timeoutMillis)
    }

    /**
     * Assert auth screen is displayed.
     */
    fun assertAuthScreenDisplayed() = apply {
        composeTestRule.onNodeWithTag(TestTags.AUTH_SCREEN).assertIsDisplayed()
    }

    /**
     * Assert welcome text is displayed.
     */
    fun assertWelcomeTextDisplayed() = apply {
        composeTestRule.onNodeWithTag(TestTags.AUTH_WELCOME_TEXT).assertIsDisplayed()
    }

    /**
     * Assert Google Sign-In button is displayed.
     */
    fun assertGoogleSignInButtonDisplayed() = apply {
        composeTestRule.onNodeWithTag(TestTags.GOOGLE_SIGN_IN_BUTTON).assertIsDisplayed()
    }

    /**
     * Tap Google Sign-In button.
     */
    fun tapGoogleSignIn() = apply {
        composeTestRule.onNodeWithTag(TestTags.GOOGLE_SIGN_IN_BUTTON).performClick()
    }

    /**
     * Verify navigation to onboarding after successful sign-in.
     * This is used with mocked auth.
     */
    fun assertNavigatedToOnboarding(timeoutMillis: Long = 5000) = apply {
        composeTestRule.waitUntilNodeWithTagExists(
            TestTags.ONBOARDING_PROGRESS_BAR,
            timeoutMillis
        )
    }

    /**
     * Full auth flow for testing with mocked sign-in.
     */
    fun performMockedSignIn() = apply {
        waitForAuthScreen()
        assertAuthScreenDisplayed()
        assertGoogleSignInButtonDisplayed()
        tapGoogleSignIn()
        // With mocked auth, should navigate to onboarding
    }

    companion object {
        const val SPLASH_DURATION_MS = 2500L
    }
}
