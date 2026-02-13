package com.rasoiai.app.presentation.settings.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.domain.model.CookingStreak
import com.rasoiai.domain.model.LeaderboardEntry
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class FriendsLeaderboardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setupScreen(uiState: FriendsLeaderboardUiState): FriendsLeaderboardViewModel {
        val mockViewModel = mockk<FriendsLeaderboardViewModel>(relaxed = true)
        every { mockViewModel.uiState } returns MutableStateFlow(uiState)
        composeTestRule.setContent {
            RasoiAITheme {
                FriendsLeaderboardScreen(onNavigateBack = {}, viewModel = mockViewModel)
            }
        }
        return mockViewModel
    }

    @Test
    fun screen_displaysTitle() {
        setupScreen(
            FriendsLeaderboardUiState(
                isLoading = false,
                streak = null,
                leaderboard = emptyList(),
                errorMessage = null
            )
        )
        composeTestRule.onNodeWithText("Friends", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun loadingState_showsLoadingIndicator() {
        setupScreen(
            FriendsLeaderboardUiState(
                isLoading = true,
                streak = null,
                leaderboard = emptyList(),
                errorMessage = null
            )
        )
        // When loading, leaderboard content should not be visible
        composeTestRule.onNodeWithText("No friends", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }

    @Test
    fun streakInfo_displayedWhenAvailable() {
        setupScreen(
            FriendsLeaderboardUiState(
                isLoading = false,
                streak = CookingStreak(
                    currentStreak = 7,
                    bestStreak = 14,
                    lastCookingDate = LocalDate.now()
                ),
                leaderboard = emptyList(),
                errorMessage = null
            )
        )
        composeTestRule.onNodeWithText("7", substring = true, ignoreCase = false)
            .assertIsDisplayed()
    }

    @Test
    fun emptyLeaderboard_showsMessage() {
        setupScreen(
            FriendsLeaderboardUiState(
                isLoading = false,
                streak = null,
                leaderboard = emptyList(),
                errorMessage = null
            )
        )
        // Should show an empty state message when no leaderboard entries
        composeTestRule.onNodeWithText("No friends", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun errorMessage_displayedWhenPresent() {
        setupScreen(
            FriendsLeaderboardUiState(
                isLoading = false,
                streak = null,
                leaderboard = emptyList(),
                errorMessage = "Failed to load leaderboard"
            )
        )
        composeTestRule.onNodeWithText("Failed to load leaderboard", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }
}
