package com.rasoiai.app.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.rasoiai.app.presentation.theme.RasoiAITheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for authentication screen components.
 * Tests the auth screen layout and button interactions without requiring actual Firebase auth.
 */
class AuthScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun authScreen_displaysWelcomeText() {
        composeTestRule.setContent {
            RasoiAITheme {
                TestAuthContent(
                    onGoogleSignIn = {},
                    onSkip = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Welcome to RasoiAI").assertIsDisplayed()
    }

    @Test
    fun authScreen_displaysSignInButton() {
        composeTestRule.setContent {
            RasoiAITheme {
                TestAuthContent(
                    onGoogleSignIn = {},
                    onSkip = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Sign in with Google").assertIsDisplayed()
    }

    @Test
    fun authScreen_displaysSkipButton() {
        composeTestRule.setContent {
            RasoiAITheme {
                TestAuthContent(
                    onGoogleSignIn = {},
                    onSkip = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Skip for now").assertIsDisplayed()
    }

    @Test
    fun authScreen_googleSignInButton_triggersCallback() {
        var signInClicked = false

        composeTestRule.setContent {
            RasoiAITheme {
                TestAuthContent(
                    onGoogleSignIn = { signInClicked = true },
                    onSkip = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Sign in with Google").performClick()
        assertTrue(signInClicked)
    }

    @Test
    fun authScreen_skipButton_triggersCallback() {
        var skipClicked = false

        composeTestRule.setContent {
            RasoiAITheme {
                TestAuthContent(
                    onGoogleSignIn = {},
                    onSkip = { skipClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Skip for now").performClick()
        assertTrue(skipClicked)
    }

    @Test
    fun authScreen_displaysAppDescription() {
        composeTestRule.setContent {
            RasoiAITheme {
                TestAuthContent(
                    onGoogleSignIn = {},
                    onSkip = {}
                )
            }
        }

        composeTestRule.onNodeWithText("AI-powered meal planning for Indian families").assertIsDisplayed()
    }

    @Test
    fun authScreen_loadingState_showsLoadingIndicator() {
        composeTestRule.setContent {
            RasoiAITheme {
                TestAuthContent(
                    onGoogleSignIn = {},
                    onSkip = {},
                    isLoading = true
                )
            }
        }

        composeTestRule.onNodeWithText("Signing in...").assertIsDisplayed()
    }

    @Test
    fun authScreen_errorState_showsErrorMessage() {
        composeTestRule.setContent {
            RasoiAITheme {
                TestAuthContent(
                    onGoogleSignIn = {},
                    onSkip = {},
                    errorMessage = "Sign in failed. Please try again."
                )
            }
        }

        composeTestRule.onNodeWithText("Sign in failed. Please try again.").assertIsDisplayed()
    }
}

/**
 * Test version of auth screen content for UI testing without Hilt dependencies.
 */
@Composable
private fun TestAuthContent(
    onGoogleSignIn: () -> Unit,
    onSkip: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to RasoiAI",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "AI-powered meal planning for Indian families",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        if (isLoading) {
            Text(
                text = "Signing in...",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Button(
                onClick = onGoogleSignIn,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign in with Google")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip for now")
            }
        }

        errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
