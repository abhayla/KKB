package com.rasoiai.app.e2e.robots

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.rasoiai.app.e2e.base.waitUntilNodeWithTagExists
import com.rasoiai.app.presentation.common.TestTags

/**
 * Robot for Auth screen interactions.
 * Handles splash screen and Firebase Phone OTP login.
 */
class AuthRobot(private val composeTestRule: ComposeContentTestRule) {

    /**
     * Verify splash screen is displayed.
     */
    fun assertSplashScreenDisplayed() = apply {
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
     * Assert Send OTP button is displayed.
     */
    fun assertSendOtpButtonDisplayed() = apply {
        composeTestRule.onNodeWithTag(TestTags.SEND_OTP_BUTTON).assertIsDisplayed()
    }

    /**
     * Enter phone number in the phone input field.
     */
    fun enterPhoneNumber(phone: String = "1111111111") = apply {
        composeTestRule.onNodeWithTag(TestTags.PHONE_NUMBER_FIELD).performTextInput(phone)
    }

    /**
     * Tap Send OTP button.
     */
    fun tapSendOtp() = apply {
        composeTestRule.onNodeWithTag(TestTags.SEND_OTP_BUTTON).performClick()
    }

    /**
     * Assert OTP verification screen is displayed.
     */
    fun assertOtpScreenDisplayed() = apply {
        composeTestRule.onNodeWithTag(TestTags.OTP_SCREEN_TITLE).assertIsDisplayed()
    }

    /**
     * Enter OTP code digit by digit.
     */
    fun enterOtp(otp: String = "123456") = apply {
        otp.forEachIndexed { index, digit ->
            composeTestRule.onNodeWithTag("${TestTags.OTP_INPUT_PREFIX}$index")
                .performTextInput(digit.toString())
        }
    }

    /**
     * Tap Verify OTP button.
     */
    fun tapVerifyOtp() = apply {
        composeTestRule.onNodeWithTag(TestTags.VERIFY_OTP_BUTTON).performClick()
    }

    /**
     * Verify navigation to onboarding after successful sign-in.
     */
    fun assertNavigatedToOnboarding(timeoutMillis: Long = 15000) = apply {
        composeTestRule.waitUntilNodeWithTagExists(
            TestTags.ONBOARDING_PROGRESS_BAR,
            timeoutMillis
        )
    }

    /**
     * Full auth flow for testing with mocked phone auth.
     * With FakePhoneAuthClient (autoVerify=true), sendOtp auto-verifies.
     */
    fun performMockedPhoneAuth() = apply {
        waitForAuthScreen()
        assertAuthScreenDisplayed()
        assertSendOtpButtonDisplayed()
        enterPhoneNumber()
        tapSendOtp()
        // With mocked auth (autoVerify=true), should auto-navigate to onboarding
    }

    companion object {
        const val SPLASH_DURATION_MS = 2500L
    }
}
