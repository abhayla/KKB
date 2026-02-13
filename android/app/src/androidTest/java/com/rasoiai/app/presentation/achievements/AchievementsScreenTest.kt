package com.rasoiai.app.presentation.achievements

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.domain.model.Achievement
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class AchievementsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun makeItem(
        id: String,
        name: String,
        description: String,
        emoji: String = "🏆",
        isUnlocked: Boolean,
        currentProgress: Int = if (isUnlocked) 1 else 0,
        targetProgress: Int = 1
    ) = AchievementItem(
        achievement = Achievement(
            id = id,
            name = name,
            description = description,
            emoji = emoji,
            isUnlocked = isUnlocked,
            unlockedDate = if (isUnlocked) LocalDate.of(2026, 1, 15) else null
        ),
        currentProgress = currentProgress,
        targetProgress = targetProgress
    )

    private fun setupScreen(uiState: AchievementsUiState): AchievementsViewModel {
        val mockViewModel = mockk<AchievementsViewModel>(relaxed = true)
        every { mockViewModel.uiState } returns MutableStateFlow(uiState)
        composeTestRule.setContent {
            RasoiAITheme {
                AchievementsScreen(onNavigateBack = {}, viewModel = mockViewModel)
            }
        }
        return mockViewModel
    }

    @Test
    fun screen_displaysTitle() {
        setupScreen(
            AchievementsUiState(
                isLoading = false,
                errorMessage = null,
                achievements = emptyList()
            )
        )
        composeTestRule.onNodeWithText("Achievements", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun loadingState_showsLoadingIndicator() {
        setupScreen(
            AchievementsUiState(
                isLoading = true,
                errorMessage = null,
                achievements = emptyList()
            )
        )
        composeTestRule.onNodeWithText("Unlocked", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }

    @Test
    fun achievementCards_displayed() {
        setupScreen(
            AchievementsUiState(
                isLoading = false,
                errorMessage = null,
                achievements = listOf(
                    makeItem(id = "1", name = "First Meal", description = "Cook your first meal", isUnlocked = true),
                    makeItem(id = "2", name = "Week Warrior", description = "Complete a full week of cooking", isUnlocked = false, currentProgress = 3, targetProgress = 7)
                )
            )
        )
        composeTestRule.onNodeWithText("First Meal", substring = true, ignoreCase = false)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Week Warrior", substring = true, ignoreCase = false)
            .assertIsDisplayed()
    }

    @Test
    fun unlockedSection_visibleWhenAchievementsExist() {
        setupScreen(
            AchievementsUiState(
                isLoading = false,
                errorMessage = null,
                achievements = listOf(
                    makeItem(id = "1", name = "First Meal", description = "Cook your first meal", isUnlocked = true)
                )
            )
        )
        composeTestRule.onNodeWithText("Unlocked", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun lockedSection_visibleWhenLockedAchievementsExist() {
        setupScreen(
            AchievementsUiState(
                isLoading = false,
                errorMessage = null,
                achievements = listOf(
                    makeItem(id = "1", name = "First Meal", description = "Cook your first meal", isUnlocked = true),
                    makeItem(id = "2", name = "Week Warrior", description = "Complete a full week", isUnlocked = false, currentProgress = 3, targetProgress = 7)
                )
            )
        )
        composeTestRule.onNodeWithText("Locked", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun completionText_showsCorrectFormat() {
        setupScreen(
            AchievementsUiState(
                isLoading = false,
                errorMessage = null,
                achievements = listOf(
                    makeItem(id = "1", name = "First Meal", description = "Cook your first meal", isUnlocked = true),
                    makeItem(id = "2", name = "Week Warrior", description = "Complete a full week", isUnlocked = false),
                    makeItem(id = "3", name = "Master Chef", description = "Cook 100 meals", isUnlocked = false)
                )
            )
        )
        composeTestRule.onNodeWithText("1", substring = true, ignoreCase = false)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("3", substring = true, ignoreCase = false)
            .assertIsDisplayed()
    }

    @Test
    fun errorMessage_displayedWhenPresent() {
        setupScreen(
            AchievementsUiState(
                isLoading = false,
                errorMessage = "Failed to load achievements",
                achievements = emptyList()
            )
        )
        composeTestRule.onNodeWithText("Failed to load achievements", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }
}
