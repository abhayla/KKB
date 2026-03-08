package com.rasoiai.app.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rasoiai.domain.model.Achievement
import com.rasoiai.domain.model.CookingDay
import com.rasoiai.domain.model.CookingStreak
import com.rasoiai.domain.model.DataScope
import com.rasoiai.domain.model.LeaderboardEntry
import com.rasoiai.domain.model.MonthlyStats
import com.rasoiai.domain.model.WeeklyChallenge
import com.rasoiai.domain.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.YearMonth
import javax.inject.Inject

/**
 * Cuisine breakdown data for pie chart
 */
data class CuisineBreakdown(
    val cuisine: String,
    val count: Int,
    val percentage: Float
)

/**
 * UI state for the Stats screen.
 */
data class StatsUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val cookingStreak: CookingStreak? = null,
    val monthlyStats: MonthlyStats? = null,
    val cookingDays: List<CookingDay> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
    val weeklyChallenge: WeeklyChallenge? = null,
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val selectedYearMonth: YearMonth = YearMonth.now(),
    val isJoiningChallenge: Boolean = false,
    val cuisineBreakdown: List<CuisineBreakdown> = emptyList(),
    val selectedScope: DataScope = DataScope.PERSONAL
) {
    val unlockedAchievements: List<Achievement>
        get() = achievements.filter { it.isUnlocked }

    val lockedAchievements: List<Achievement>
        get() = achievements.filter { !it.isUnlocked }

    val hasStreak: Boolean
        get() = (cookingStreak?.currentStreak ?: 0) > 0
}

/**
 * Navigation events from Stats screen.
 */
sealed class StatsNavigationEvent {
    data object NavigateBack : StatsNavigationEvent()
    data object NavigateToHome : StatsNavigationEvent()
    data object NavigateToGrocery : StatsNavigationEvent()
    data object NavigateToChat : StatsNavigationEvent()
    data object NavigateToFavorites : StatsNavigationEvent()
    data object NavigateToAllAchievements : StatsNavigationEvent()
    data object NavigateToFullLeaderboard : StatsNavigationEvent()
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val statsRepository: StatsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    private val _navigationEvent = Channel<StatsNavigationEvent>(Channel.BUFFERED)
    val navigationEvent: Flow<StatsNavigationEvent> = _navigationEvent.receiveAsFlow()

    init {
        loadInitialData()
        observeStreams()
    }

    // region Data Loading

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Load monthly stats and calendar
            loadMonthData(_uiState.value.selectedYearMonth)

            // Load leaderboard
            loadLeaderboard()

            // Load cuisine breakdown (mock data for now)
            loadCuisineBreakdown()

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun loadCuisineBreakdown() {
        viewModelScope.launch {
            statsRepository.getCuisineBreakdown()
                .onSuccess { breakdown ->
                    val total = breakdown.sumOf { it.second }.toFloat()
                    val cuisineBreakdown = if (total > 0) {
                        breakdown.map { (cuisine, count) ->
                            CuisineBreakdown(
                                cuisine = cuisine,
                                count = count,
                                percentage = (count / total) * 100f
                            )
                        }
                    } else {
                        // Show empty state with placeholder data
                        emptyList()
                    }
                    _uiState.update { it.copy(cuisineBreakdown = cuisineBreakdown) }
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to load cuisine breakdown")
                    // Keep existing data or show empty state
                }
        }
    }

    private fun observeStreams() {
        // Observe cooking streak
        viewModelScope.launch {
            statsRepository.getCookingStreak().collect { streak ->
                _uiState.update { it.copy(cookingStreak = streak) }
            }
        }

        // Observe achievements
        viewModelScope.launch {
            statsRepository.getAchievements().collect { achievements ->
                _uiState.update { it.copy(achievements = achievements) }
            }
        }

        // Observe weekly challenge
        viewModelScope.launch {
            statsRepository.getWeeklyChallenge().collect { challenge ->
                _uiState.update { it.copy(weeklyChallenge = challenge) }
            }
        }
    }

    private suspend fun loadMonthData(yearMonth: YearMonth) {
        // Load monthly stats
        statsRepository.getMonthlyStats(yearMonth)
            .onSuccess { stats ->
                _uiState.update { it.copy(monthlyStats = stats) }
            }
            .onFailure { e ->
                Timber.e(e, "Error loading monthly stats")
            }

        // Load cooking days for calendar
        statsRepository.getCookingDays(yearMonth)
            .onSuccess { days ->
                _uiState.update { it.copy(cookingDays = days) }
            }
            .onFailure { e ->
                Timber.e(e, "Error loading cooking days")
            }
    }

    private fun loadLeaderboard() {
        viewModelScope.launch {
            statsRepository.getLeaderboard(limit = 3)
                .onSuccess { entries ->
                    _uiState.update { it.copy(leaderboard = entries) }
                }
                .onFailure { e ->
                    Timber.e(e, "Error loading leaderboard")
                }
        }
    }

    // endregion

    // region Calendar Navigation

    fun onPreviousMonth() {
        val newMonth = _uiState.value.selectedYearMonth.minusMonths(1)
        _uiState.update { it.copy(selectedYearMonth = newMonth) }
        viewModelScope.launch {
            loadMonthData(newMonth)
        }
    }

    fun onNextMonth() {
        val newMonth = _uiState.value.selectedYearMonth.plusMonths(1)
        // Don't allow navigating to future months
        if (newMonth.isAfter(YearMonth.now())) return

        _uiState.update { it.copy(selectedYearMonth = newMonth) }
        viewModelScope.launch {
            loadMonthData(newMonth)
        }
    }

    fun onTodayClick() {
        val currentMonth = YearMonth.now()
        if (_uiState.value.selectedYearMonth != currentMonth) {
            _uiState.update { it.copy(selectedYearMonth = currentMonth) }
            viewModelScope.launch {
                loadMonthData(currentMonth)
            }
        }
    }

    // endregion

    // region Challenge Actions

    fun onJoinChallenge() {
        val challenge = _uiState.value.weeklyChallenge ?: return
        if (challenge.isJoined) return

        viewModelScope.launch {
            _uiState.update { it.copy(isJoiningChallenge = true) }

            statsRepository.joinChallenge(challenge.id)
                .onSuccess {
                    Timber.i("Joined challenge: ${challenge.name}")
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to join challenge")
                    _uiState.update { it.copy(errorMessage = "Failed to join challenge") }
                }

            _uiState.update { it.copy(isJoiningChallenge = false) }
        }
    }

    // endregion

    // region Share Actions

    fun onShareAchievement(achievement: Achievement) {
        Timber.i("Share achievement: ${achievement.name}")
        _uiState.update { it.copy(errorMessage = "Share: ${achievement.emoji} ${achievement.name} - Unlocked on RasoiAI!") }
    }

    fun buildShareText(): String {
        val state = _uiState.value
        return buildString {
            append("My RasoiAI Cooking Stats\n\n")
            state.cookingStreak?.let {
                append("Current Streak: ${it.currentStreak} days\n")
                append("Best Streak: ${it.bestStreak} days\n\n")
            }
            state.monthlyStats?.let {
                append("This Month:\n")
                append("  Meals Cooked: ${it.mealsCooked}\n")
                append("  New Recipes: ${it.newRecipes}\n")
                append("  Avg Rating: ${String.format("%.1f", it.averageRating)}\n\n")
            }
            val unlocked = state.unlockedAchievements
            if (unlocked.isNotEmpty()) {
                append("Achievements: ${unlocked.size} unlocked\n")
            }
            append("\nCooked with RasoiAI")
        }
    }

    // endregion

    // region Navigation

    fun navigateBack() {
        _navigationEvent.trySend(StatsNavigationEvent.NavigateBack)
    }

    fun navigateToHome() {
        _navigationEvent.trySend(StatsNavigationEvent.NavigateToHome)
    }

    fun navigateToGrocery() {
        _navigationEvent.trySend(StatsNavigationEvent.NavigateToGrocery)
    }

    fun navigateToChat() {
        _navigationEvent.trySend(StatsNavigationEvent.NavigateToChat)
    }

    fun navigateToFavorites() {
        _navigationEvent.trySend(StatsNavigationEvent.NavigateToFavorites)
    }

    fun onViewAllAchievements() {
        _navigationEvent.trySend(StatsNavigationEvent.NavigateToAllAchievements)
    }

    fun onViewFullLeaderboard() {
        _navigationEvent.trySend(StatsNavigationEvent.NavigateToFullLeaderboard)
    }

    // endregion

    // region Scope

    fun setScope(scope: DataScope) {
        _uiState.update { it.copy(selectedScope = scope) }
    }

    // endregion

    // region Error Handling

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // endregion
}
