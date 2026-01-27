package com.rasoiai.app.presentation.stats

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rasoiai.app.presentation.home.components.RasoiBottomNavigation
import com.rasoiai.app.presentation.navigation.Screen
import com.rasoiai.app.presentation.stats.components.AchievementsSection
import com.rasoiai.app.presentation.stats.components.ChallengeCard
import com.rasoiai.app.presentation.stats.components.CookingCalendar
import com.rasoiai.app.presentation.stats.components.CuisineBreakdownSection
import com.rasoiai.app.presentation.stats.components.LeaderboardSection
import com.rasoiai.app.presentation.stats.components.MonthlyStatsRow
import com.rasoiai.app.presentation.stats.components.StreakCard
import com.rasoiai.domain.model.Achievement
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.app.presentation.theme.spacing
import com.rasoiai.domain.model.CookingStreak
import com.rasoiai.domain.model.MonthlyStats
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun StatsScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToGrocery: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                StatsNavigationEvent.NavigateBack -> onNavigateToHome()
                StatsNavigationEvent.NavigateToHome -> onNavigateToHome()
                StatsNavigationEvent.NavigateToGrocery -> onNavigateToGrocery()
                StatsNavigationEvent.NavigateToChat -> onNavigateToChat()
                StatsNavigationEvent.NavigateToFavorites -> onNavigateToFavorites()
                StatsNavigationEvent.NavigateToAllAchievements -> { /* TODO: Navigate to full achievements screen */ }
                StatsNavigationEvent.NavigateToFullLeaderboard -> { /* TODO: Navigate to full leaderboard screen */ }
            }
        }
    }

    // Show error in snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    StatsScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBackClick = viewModel::navigateBack,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onTodayClick = viewModel::onTodayClick,
        onViewAllAchievements = viewModel::onViewAllAchievements,
        onShareAchievement = viewModel::onShareAchievement,
        onJoinChallenge = viewModel::onJoinChallenge,
        onViewFullLeaderboard = viewModel::onViewFullLeaderboard,
        onBottomNavItemClick = { screen ->
            when (screen) {
                Screen.Home -> viewModel.navigateToHome()
                Screen.Grocery -> viewModel.navigateToGrocery()
                Screen.Chat -> viewModel.navigateToChat()
                Screen.Favorites -> viewModel.navigateToFavorites()
                Screen.Stats -> { /* Already here */ }
                else -> { }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatsScreenContent(
    uiState: StatsUiState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit,
    onViewAllAchievements: () -> Unit,
    onShareAchievement: (Achievement) -> Unit,
    onJoinChallenge: () -> Unit,
    onViewFullLeaderboard: () -> Unit,
    onBottomNavItemClick: (Screen) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Cooking Stats",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            RasoiBottomNavigation(
                currentScreen = Screen.Stats,
                onItemClick = onBottomNavItemClick
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = spacing.md)
                ) {
                    // Streak Card
                    item {
                        uiState.cookingStreak?.let { streak ->
                            StreakCard(
                                streak = streak,
                                modifier = Modifier.padding(horizontal = spacing.md)
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(spacing.lg))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = spacing.md))
                        Spacer(modifier = Modifier.height(spacing.md))
                    }

                    // Calendar Section
                    item {
                        CookingCalendar(
                            yearMonth = uiState.selectedYearMonth,
                            cookingDays = uiState.cookingDays,
                            onPreviousMonth = onPreviousMonth,
                            onNextMonth = onNextMonth,
                            onTodayClick = onTodayClick,
                            modifier = Modifier.padding(horizontal = spacing.md)
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(spacing.lg))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = spacing.md))
                        Spacer(modifier = Modifier.height(spacing.md))
                    }

                    // Monthly Stats
                    item {
                        uiState.monthlyStats?.let { stats ->
                            MonthlyStatsRow(
                                stats = stats,
                                modifier = Modifier.padding(horizontal = spacing.md)
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(spacing.lg))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = spacing.md))
                        Spacer(modifier = Modifier.height(spacing.md))
                    }

                    // Achievements Section
                    item {
                        AchievementsSection(
                            achievements = uiState.achievements,
                            onViewAllClick = onViewAllAchievements,
                            onShareAchievement = onShareAchievement,
                            modifier = Modifier.padding(horizontal = spacing.md)
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(spacing.lg))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = spacing.md))
                        Spacer(modifier = Modifier.height(spacing.md))
                    }

                    // Cuisine Breakdown
                    item {
                        if (uiState.cuisineBreakdown.isNotEmpty()) {
                            CuisineBreakdownSection(
                                breakdown = uiState.cuisineBreakdown,
                                modifier = Modifier.padding(horizontal = spacing.md)
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(spacing.lg))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = spacing.md))
                        Spacer(modifier = Modifier.height(spacing.md))
                    }

                    // Weekly Challenge
                    item {
                        uiState.weeklyChallenge?.let { challenge ->
                            ChallengeCard(
                                challenge = challenge,
                                onJoinClick = onJoinChallenge,
                                isLoading = uiState.isJoiningChallenge,
                                modifier = Modifier.padding(horizontal = spacing.md)
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(spacing.lg))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = spacing.md))
                        Spacer(modifier = Modifier.height(spacing.md))
                    }

                    // Leaderboard
                    item {
                        if (uiState.leaderboard.isNotEmpty()) {
                            LeaderboardSection(
                                entries = uiState.leaderboard,
                                onViewAllClick = onViewFullLeaderboard,
                                modifier = Modifier.padding(horizontal = spacing.md)
                            )
                        }
                    }

                    // Bottom spacing
                    item {
                        Spacer(modifier = Modifier.height(spacing.xl))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFDFAF4)
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    backgroundColor = 0xFF1C1B1F
)
@Composable
private fun StatsScreenPreview() {
    RasoiAITheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            StatsScreenContent(
                uiState = StatsUiState(
                    isLoading = false,
                    cookingStreak = CookingStreak(
                        currentStreak = 12,
                        bestStreak = 23,
                        lastCookingDate = LocalDate.now()
                    ),
                    monthlyStats = MonthlyStats(
                        mealsCooked = 45,
                        newRecipes = 12,
                        averageRating = 4.2f
                    ),
                    selectedYearMonth = YearMonth.now(),
                    cuisineBreakdown = listOf(
                        CuisineBreakdown("North", 18, 40f),
                        CuisineBreakdown("South", 12, 27f),
                        CuisineBreakdown("East", 6, 13f),
                        CuisineBreakdown("West", 9, 20f)
                    )
                ),
                snackbarHostState = SnackbarHostState(),
                onBackClick = {},
                onPreviousMonth = {},
                onNextMonth = {},
                onTodayClick = {},
                onViewAllAchievements = {},
                onShareAchievement = {},
                onJoinChallenge = {},
                onViewFullLeaderboard = {},
                onBottomNavItemClick = {}
            )
        }
    }
}
