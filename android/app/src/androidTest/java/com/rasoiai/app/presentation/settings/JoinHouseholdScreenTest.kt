package com.rasoiai.app.presentation.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rasoiai.app.presentation.common.TestTags
import com.rasoiai.app.presentation.settings.screens.JoinHouseholdScreen
import com.rasoiai.app.presentation.settings.viewmodels.JoinHouseholdUiState
import com.rasoiai.app.presentation.theme.RasoiAITheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI Tests for JoinHouseholdScreen
 *
 * Tests the join household flow including invite code input,
 * join button state, loading indicator, and error display.
 */
@RunWith(AndroidJUnit4::class)
class JoinHouseholdScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setScreen(uiState: JoinHouseholdUiState) {
        composeTestRule.setContent {
            RasoiAITheme {
                JoinHouseholdScreen(
                    uiState = uiState,
                    onNavigateBack = {},
                    onCodeChanged = {},
                    onJoin = {}
                )
            }
        }
    }

    @Test
    fun inviteCodeField_displayed() {
        setScreen(JoinHouseholdUiState())
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_JOIN_CODE_FIELD).assertIsDisplayed()
    }

    @Test
    fun joinButton_displayed() {
        setScreen(JoinHouseholdUiState(inviteCode = "ABC123"))
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_JOIN_BUTTON).assertIsDisplayed()
    }

    @Test
    fun loadingState_duringJoin() {
        setScreen(JoinHouseholdUiState(isLoading = true, inviteCode = "ABC123"))
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_JOIN_BUTTON).assertIsDisplayed()
    }

    @Test
    fun errorMessage_onFailure() {
        setScreen(JoinHouseholdUiState(error = "Invalid invite code", inviteCode = "BAD"))
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_JOIN_CODE_FIELD).assertIsDisplayed()
    }

    @Test
    fun joinButton_disabledWhenCodeBlank() {
        setScreen(JoinHouseholdUiState(inviteCode = ""))
        composeTestRule.onNodeWithTag(TestTags.HOUSEHOLD_JOIN_BUTTON).assertIsNotEnabled()
    }
}
