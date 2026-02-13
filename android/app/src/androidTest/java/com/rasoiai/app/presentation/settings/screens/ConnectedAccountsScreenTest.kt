package com.rasoiai.app.presentation.settings.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rasoiai.app.presentation.theme.RasoiAITheme
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConnectedAccountsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setupScreen(uiState: ConnectedAccountsUiState): ConnectedAccountsViewModel {
        val mockViewModel = mockk<ConnectedAccountsViewModel>(relaxed = true)
        every { mockViewModel.uiState } returns MutableStateFlow(uiState)
        composeTestRule.setContent {
            RasoiAITheme {
                ConnectedAccountsScreen(onNavigateBack = {}, viewModel = mockViewModel)
            }
        }
        return mockViewModel
    }

    @Test
    fun screen_displaysTitle() {
        setupScreen(
            ConnectedAccountsUiState(
                isLoading = false,
                googleEmail = "abhay@gmail.com",
                googleName = "Abhay Sharma"
            )
        )
        composeTestRule.onNodeWithText("Connected Accounts", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun loadingState_hidesContent() {
        setupScreen(
            ConnectedAccountsUiState(
                isLoading = true,
                googleEmail = "",
                googleName = ""
            )
        )
        composeTestRule.onNodeWithText("abhay@gmail.com", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }

    @Test
    fun googleEmail_displayedWhenLoaded() {
        setupScreen(
            ConnectedAccountsUiState(
                isLoading = false,
                googleEmail = "abhay@gmail.com",
                googleName = "Abhay Sharma"
            )
        )
        composeTestRule.onNodeWithText("abhay@gmail.com", substring = true, ignoreCase = false)
            .assertIsDisplayed()
    }

    @Test
    fun googleName_displayedWhenLoaded() {
        setupScreen(
            ConnectedAccountsUiState(
                isLoading = false,
                googleEmail = "abhay@gmail.com",
                googleName = "Abhay Sharma"
            )
        )
        composeTestRule.onNodeWithText("Abhay Sharma", substring = true, ignoreCase = false)
            .assertIsDisplayed()
    }
}
