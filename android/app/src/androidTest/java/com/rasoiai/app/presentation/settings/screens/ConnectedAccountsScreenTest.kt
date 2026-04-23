package com.rasoiai.app.presentation.settings.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rasoiai.app.presentation.theme.RasoiAITheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConnectedAccountsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun screen_displaysTitle() {
        composeTestRule.setContent {
            RasoiAITheme {
                ConnectedAccountsTestContent(
                    uiState = ConnectedAccountsUiState(
                        isLoading = false,
                        googleEmail = "abhay@gmail.com",
                        googleName = "Abhay Sharma"
                    )
                )
            }
        }
        composeTestRule.onNodeWithText("Connected Accounts", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun loadingState_hidesContent() {
        composeTestRule.setContent {
            RasoiAITheme {
                ConnectedAccountsTestContent(
                    uiState = ConnectedAccountsUiState(
                        isLoading = true,
                        googleEmail = "",
                        googleName = ""
                    )
                )
            }
        }
        composeTestRule.onNodeWithText("abhay@gmail.com", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }

    @Test
    fun googleEmail_displayedWhenLoaded() {
        composeTestRule.setContent {
            RasoiAITheme {
                ConnectedAccountsTestContent(
                    uiState = ConnectedAccountsUiState(
                        isLoading = false,
                        googleEmail = "abhay@gmail.com",
                        googleName = "Abhay Sharma"
                    )
                )
            }
        }
        composeTestRule.onNodeWithText("abhay@gmail.com", substring = true, ignoreCase = false)
            .assertIsDisplayed()
    }

    @Test
    fun googleName_displayedWhenLoaded() {
        composeTestRule.setContent {
            RasoiAITheme {
                ConnectedAccountsTestContent(
                    uiState = ConnectedAccountsUiState(
                        isLoading = false,
                        googleEmail = "abhay@gmail.com",
                        googleName = "Abhay Sharma"
                    )
                )
            }
        }
        composeTestRule.onNodeWithText("Abhay Sharma", substring = true, ignoreCase = false)
            .assertIsDisplayed()
    }
}

@androidx.compose.runtime.Composable
private fun ConnectedAccountsTestContent(
    uiState: ConnectedAccountsUiState,
    onNavigateBack: () -> Unit = {}
) {
    ConnectedAccountsScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack
    )
}
