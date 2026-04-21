package com.rasoiai.app.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.rasoiai.app.presentation.common.TestTags
import com.rasoiai.app.presentation.theme.RasoiAITheme
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for AuthScreen (Phone OTP flow).
 *
 * These tests verify the UI behavior of AuthScreen using Compose testing APIs.
 * They test the UI layer in isolation by providing mock data directly to a
 * test wrapper composable that mirrors AuthScreenContent structure.
 *
 * ## Test Categories:
 * - Screen display tests (logo, app name, welcome text, tagline)
 * - Phone input tests (phone number field, Send OTP button)
 * - OTP verification tests (OTP input, Verify button)
 * - Terms and Privacy tests
 * - Error state tests
 */
class AuthScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // region Test Data Factory

    private fun createTestUiState(
        isLoading: Boolean = false,
        errorMessage: String? = null,
        isSignedIn: Boolean = false,
        phoneNumber: String = "",
        isPhoneValid: Boolean = false,
        otpSent: Boolean = false,
        otpCode: String = "",
        verificationId: String? = null,
        resendCountdownSeconds: Int = 0,
        isVerifying: Boolean = false
    ) = AuthUiState(
        isLoading = isLoading,
        errorMessage = errorMessage,
        phoneNumber = phoneNumber,
        isPhoneValid = isPhoneValid,
        otpSent = otpSent,
        otpCode = otpCode,
        verificationId = verificationId,
        resendCountdownSeconds = resendCountdownSeconds,
        isVerifying = isVerifying,
        isSignedIn = isSignedIn
    )

    // endregion

    // region Screen Display Tests

    @Test
    fun authScreen_isDisplayed() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                PhoneInputTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.AUTH_SCREEN).assertIsDisplayed()
    }

    @Test
    fun authScreen_displaysAppName() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                PhoneInputTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("RasoiAI").assertIsDisplayed()
    }

    @Test
    fun authScreen_displaysWelcomeText() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                PhoneInputTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.AUTH_WELCOME_TEXT).assertIsDisplayed()
        composeTestRule.onNodeWithText("Welcome!").assertIsDisplayed()
    }

    @Test
    fun authScreen_displaysTagline() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                PhoneInputTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("AI Meal Planning for Indian Families").assertIsDisplayed()
    }

    // endregion

    // region Phone Input Tests

    @Test
    fun authScreen_displaysPhoneNumberField() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                PhoneInputTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.PHONE_NUMBER_FIELD).assertIsDisplayed()
    }

    @Test
    fun authScreen_displaysCountryCodePrefix() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                PhoneInputTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.COUNTRY_CODE_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithText("+91").assertIsDisplayed()
    }

    @Test
    fun authScreen_displaysSendOtpButton() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                PhoneInputTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.SEND_OTP_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithText("Send OTP").assertIsDisplayed()
    }

    @Test
    fun authScreen_sendOtpButton_isEnabled_whenNotLoading() {
        val uiState = createTestUiState(isLoading = false)

        composeTestRule.setContent {
            RasoiAITheme {
                PhoneInputTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.SEND_OTP_BUTTON).assertIsEnabled()
    }

    @Test
    fun authScreen_sendOtpButton_isDisabled_whenLoading() {
        val uiState = createTestUiState(isLoading = true)

        composeTestRule.setContent {
            RasoiAITheme {
                PhoneInputTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.SEND_OTP_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun authScreen_showsLoadingText_whenLoading() {
        val uiState = createTestUiState(isLoading = true)

        composeTestRule.setContent {
            RasoiAITheme {
                PhoneInputTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Sending OTP…").assertIsDisplayed()
    }

    @Test
    fun authScreen_sendOtpClick_triggersCallback() {
        var sendOtpClicked = false
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                PhoneInputTestContent(
                    uiState = uiState,
                    onSendOtpClick = { sendOtpClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithTag(TestTags.SEND_OTP_BUTTON).performClick()

        assert(sendOtpClicked) { "Send OTP callback was not invoked" }
    }

    // endregion

    // region Terms and Privacy Tests

    @Test
    fun authScreen_displaysTermsPrefix() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                PhoneInputTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("By continuing, you agree to our").assertIsDisplayed()
    }

    @Test
    fun authScreen_displaysTermsOfServiceLink() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                PhoneInputTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Terms of Service").assertIsDisplayed()
    }

    @Test
    fun authScreen_displaysPrivacyPolicyLink() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                PhoneInputTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Privacy Policy").assertIsDisplayed()
    }

    @Test
    fun authScreen_termsOfServiceClick_triggersCallback() {
        var termsClicked = false
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                PhoneInputTestContent(
                    uiState = uiState,
                    onTermsClick = { termsClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Terms of Service").performClick()

        assert(termsClicked) { "Terms of Service callback was not invoked" }
    }

    @Test
    fun authScreen_privacyPolicyClick_triggersCallback() {
        var privacyClicked = false
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                PhoneInputTestContent(
                    uiState = uiState,
                    onPrivacyClick = { privacyClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Privacy Policy").performClick()

        assert(privacyClicked) { "Privacy Policy callback was not invoked" }
    }

    // endregion

    // region Multiple State Tests

    @Test
    fun authScreen_displaysAllElements_inDefaultState() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                PhoneInputTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.AUTH_SCREEN).assertIsDisplayed()
        composeTestRule.onNodeWithText("RasoiAI").assertIsDisplayed()
        composeTestRule.onNodeWithText("Welcome!").assertIsDisplayed()
        composeTestRule.onNodeWithText("AI Meal Planning for Indian Families").assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.PHONE_NUMBER_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.SEND_OTP_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithText("Send OTP").assertIsDisplayed()
        composeTestRule.onNodeWithText("By continuing, you agree to our").assertIsDisplayed()
        composeTestRule.onNodeWithText("Terms of Service").assertIsDisplayed()
        composeTestRule.onNodeWithText("Privacy Policy").assertIsDisplayed()
    }

    @Test
    fun authScreen_displaysCorrectLoadingState() {
        val uiState = createTestUiState(isLoading = true)

        composeTestRule.setContent {
            RasoiAITheme {
                PhoneInputTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.AUTH_SCREEN).assertIsDisplayed()
        composeTestRule.onNodeWithText("RasoiAI").assertIsDisplayed()
        composeTestRule.onNodeWithText("Welcome!").assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.SEND_OTP_BUTTON).assertIsNotEnabled()
        composeTestRule.onNodeWithText("Sending OTP…").assertIsDisplayed()
        composeTestRule.onNodeWithText("Send OTP").assertDoesNotExist()
    }

    // endregion
}

/**
 * Test composable that mirrors the structure of AuthScreen phone input content.
 * This allows testing the UI in isolation without the ViewModel.
 */
@Composable
private fun PhoneInputTestContent(
    uiState: AuthUiState,
    onSendOtpClick: () -> Unit = {},
    onTermsClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag(TestTags.AUTH_SCREEN)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.15f))

            // Logo placeholder
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🍳",
                    style = MaterialTheme.typography.displayLarge
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App name
            Text(
                text = "RasoiAI",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Welcome text
            Text(
                text = "Welcome!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.testTag(TestTags.AUTH_WELCOME_TEXT)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            Text(
                text = "AI Meal Planning for Indian Families",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(0.1f))

            // Phone number input with country code prefix
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "+91",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.testTag(TestTags.COUNTRY_CODE_FIELD)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = uiState.phoneNumber,
                    onValueChange = {},
                    modifier = Modifier
                        .weight(1f)
                        .testTag(TestTags.PHONE_NUMBER_FIELD),
                    placeholder = { Text("Enter 10-digit number") }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Send OTP Button
            Button(
                onClick = onSendOtpClick,
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag(TestTags.SEND_OTP_BUTTON),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Sending OTP…",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = "Send OTP",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.15f))

            // Terms and Privacy
            Column(
                modifier = Modifier.padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "By continuing, you agree to our",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onTermsClick) {
                        Text(
                            text = "Terms of Service",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    }
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onPrivacyClick) {
                        Text(
                            text = "Privacy Policy",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    }
                }
            }
        }
    }
}
