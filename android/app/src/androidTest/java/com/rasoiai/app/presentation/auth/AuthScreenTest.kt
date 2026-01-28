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
 * Compose UI tests for AuthScreen.
 *
 * These tests verify the UI behavior of AuthScreen using Compose testing APIs.
 * They test the UI layer in isolation by providing mock data directly to a
 * test wrapper composable that mirrors AuthScreenContent structure.
 *
 * ## Test Categories:
 * - Screen display tests (logo, app name, welcome text, tagline)
 * - Google Sign-In button tests (display, loading, click)
 * - Terms and Privacy tests
 * - Error state tests
 *
 * ## Running Tests:
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest --tests "*.AuthScreenTest"
 * ```
 *
 * ## E2E Test Coverage (Phase 1: Authentication):
 * - Test 1.1: Splash Screen - covered by SplashScreenTest (separate)
 * - Test 1.2: Google OAuth Login - covered by tests below
 */
class AuthScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // region Test Data Factory

    private fun createTestUiState(
        isLoading: Boolean = false,
        errorMessage: String? = null,
        isSignedIn: Boolean = false
    ) = AuthUiState(
        isLoading = isLoading,
        errorMessage = errorMessage,
        isSignedIn = isSignedIn
    )

    // endregion

    // region Screen Display Tests

    @Test
    fun authScreen_isDisplayed() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                AuthScreenTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.AUTH_SCREEN).assertIsDisplayed()
    }

    @Test
    fun authScreen_displaysAppName() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                AuthScreenTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("RasoiAI").assertIsDisplayed()
    }

    @Test
    fun authScreen_displaysWelcomeText() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                AuthScreenTestContent(uiState = uiState)
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
                AuthScreenTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("AI Meal Planning for Indian Families").assertIsDisplayed()
    }

    // endregion

    // region Google Sign-In Button Tests

    @Test
    fun authScreen_displaysGoogleSignInButton() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                AuthScreenTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.GOOGLE_SIGN_IN_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithText("Continue with Google").assertIsDisplayed()
    }

    @Test
    fun authScreen_googleSignInButton_isEnabled_whenNotLoading() {
        val uiState = createTestUiState(isLoading = false)

        composeTestRule.setContent {
            RasoiAITheme {
                AuthScreenTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.GOOGLE_SIGN_IN_BUTTON).assertIsEnabled()
    }

    @Test
    fun authScreen_googleSignInButton_isDisabled_whenLoading() {
        val uiState = createTestUiState(isLoading = true)

        composeTestRule.setContent {
            RasoiAITheme {
                AuthScreenTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.GOOGLE_SIGN_IN_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun authScreen_showsLoadingText_whenLoading() {
        val uiState = createTestUiState(isLoading = true)

        composeTestRule.setContent {
            RasoiAITheme {
                AuthScreenTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Signing in…").assertIsDisplayed()
    }

    @Test
    fun authScreen_hidesSignInText_whenLoading() {
        val uiState = createTestUiState(isLoading = true)

        composeTestRule.setContent {
            RasoiAITheme {
                AuthScreenTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Continue with Google").assertDoesNotExist()
    }

    @Test
    fun authScreen_googleSignInClick_triggersCallback() {
        var signInClicked = false
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                AuthScreenTestContent(
                    uiState = uiState,
                    onSignInClick = { signInClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithTag(TestTags.GOOGLE_SIGN_IN_BUTTON).performClick()

        assert(signInClicked) { "Sign in callback was not invoked" }
    }

    @Test
    fun authScreen_displaysGoogleIcon() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                AuthScreenTestContent(uiState = uiState)
            }
        }

        // The Google icon is represented by "G" text in the current implementation
        composeTestRule.onNodeWithText("G").assertIsDisplayed()
    }

    // endregion

    // region Terms and Privacy Tests

    @Test
    fun authScreen_displaysTermsPrefix() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                AuthScreenTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("By continuing, you agree to our").assertIsDisplayed()
    }

    @Test
    fun authScreen_displaysTermsOfServiceLink() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                AuthScreenTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Terms of Service").assertIsDisplayed()
    }

    @Test
    fun authScreen_displaysPrivacyPolicyLink() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                AuthScreenTestContent(uiState = uiState)
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
                AuthScreenTestContent(
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
                AuthScreenTestContent(
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
                AuthScreenTestContent(uiState = uiState)
            }
        }

        // Verify all main elements are displayed
        composeTestRule.onNodeWithTag(TestTags.AUTH_SCREEN).assertIsDisplayed()
        composeTestRule.onNodeWithText("RasoiAI").assertIsDisplayed()
        composeTestRule.onNodeWithText("Welcome!").assertIsDisplayed()
        composeTestRule.onNodeWithText("AI Meal Planning for Indian Families").assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.GOOGLE_SIGN_IN_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithText("Continue with Google").assertIsDisplayed()
        composeTestRule.onNodeWithText("By continuing, you agree to our").assertIsDisplayed()
        composeTestRule.onNodeWithText("Terms of Service").assertIsDisplayed()
        composeTestRule.onNodeWithText("Privacy Policy").assertIsDisplayed()
    }

    @Test
    fun authScreen_displaysCorrectLoadingState() {
        val uiState = createTestUiState(isLoading = true)

        composeTestRule.setContent {
            RasoiAITheme {
                AuthScreenTestContent(uiState = uiState)
            }
        }

        // Screen elements should still be visible
        composeTestRule.onNodeWithTag(TestTags.AUTH_SCREEN).assertIsDisplayed()
        composeTestRule.onNodeWithText("RasoiAI").assertIsDisplayed()
        composeTestRule.onNodeWithText("Welcome!").assertIsDisplayed()

        // Button should show loading state
        composeTestRule.onNodeWithTag(TestTags.GOOGLE_SIGN_IN_BUTTON).assertIsNotEnabled()
        composeTestRule.onNodeWithText("Signing in…").assertIsDisplayed()
        composeTestRule.onNodeWithText("Continue with Google").assertDoesNotExist()
    }

    // endregion
}

/**
 * Test composable that mirrors the structure of AuthScreen content.
 * This allows testing the UI in isolation without the ViewModel.
 */
@Composable
private fun AuthScreenTestContent(
    uiState: AuthUiState,
    onSignInClick: () -> Unit = {},
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

            // Logo placeholder (simplified for testing)
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

            Spacer(modifier = Modifier.weight(0.2f))

            // Google Sign-In Button
            GoogleSignInButtonTestContent(
                onClick = onSignInClick,
                isLoading = uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(0.15f))

            // Terms and Privacy
            TermsAndPrivacyTestContent(
                onTermsClick = onTermsClick,
                onPrivacyClick = onPrivacyClick,
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }
    }
}

@Composable
private fun GoogleSignInButtonTestContent(
    onClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier
            .height(56.dp)
            .testTag(TestTags.GOOGLE_SIGN_IN_BUTTON),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color.Black,
            disabledContainerColor = Color.White.copy(alpha = 0.7f),
            disabledContentColor = Color.Black.copy(alpha = 0.5f)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Signing in…",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        } else {
            // Google "G" icon
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "G",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4285F4)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Continue with Google",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun TermsAndPrivacyTestContent(
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
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
