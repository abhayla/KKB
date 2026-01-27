package com.rasoiai.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rasoiai.domain.model.Festival
import com.rasoiai.domain.model.MealItem
import com.rasoiai.domain.model.MealPlan
import com.rasoiai.domain.model.MealPlanDay
import com.rasoiai.domain.model.MealType
import com.rasoiai.domain.repository.MealPlanRepository
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

/**
 * UI state for the Home screen
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val mealPlan: MealPlan? = null,
    val selectedDate: LocalDate = LocalDate.now(),
    val weekDates: List<WeekDay> = emptyList(),
    val selectedDayMeals: MealPlanDay? = null,
    val upcomingFestival: FestivalInfo? = null,
    val showRecipeActionSheet: Boolean = false,
    val selectedMealItem: MealItem? = null,
    val selectedMealType: MealType? = null,
    val showRefreshSheet: Boolean = false,
    val showSwapSheet: Boolean = false,
    val swapSuggestions: List<MealItem> = emptyList(),
    // 3-level locking system state
    val dayLockStates: Map<LocalDate, Boolean> = emptyMap(),
    val mealLockStates: Map<Pair<LocalDate, MealType>, Boolean> = emptyMap()
) {
    /** Check if the selected day is locked */
    val isSelectedDayLocked: Boolean
        get() = dayLockStates[selectedDate] == true

    /** Check if a specific meal slot is locked (inherits from day lock) */
    fun isMealLocked(mealType: MealType): Boolean {
        // Day lock takes precedence
        if (isSelectedDayLocked) return true
        // Check meal-level lock
        return mealLockStates[Pair(selectedDate, mealType)] == true
    }

    /** Check if a recipe is effectively locked (considers all lock levels) */
    fun isRecipeEffectivelyLocked(mealItem: MealItem, mealType: MealType): Boolean {
        // Day lock takes precedence
        if (isSelectedDayLocked) return true
        // Meal lock takes second precedence
        if (isMealLocked(mealType)) return true
        // Individual recipe lock
        return mealItem.isLocked
    }

    val formattedDateRange: String
        get() = mealPlan?.let {
            val formatter = DateTimeFormatter.ofPattern("MMM d")
            "${it.weekStartDate.format(formatter)} - ${it.weekEndDate.format(formatter)}"
        } ?: ""

    val formattedSelectedDay: String
        get() {
            val formatter = DateTimeFormatter.ofPattern("EEEE, MMM d")
            return selectedDate.format(formatter)
        }

    val isToday: Boolean
        get() = selectedDate == LocalDate.now()
}

/**
 * Represents a day in the week selector
 */
data class WeekDay(
    val date: LocalDate,
    val dayName: String,
    val dayNumber: Int,
    val isSelected: Boolean,
    val isToday: Boolean
)

/**
 * Festival information for the banner
 */
data class FestivalInfo(
    val name: String,
    val daysUntil: Int,
    val suggestedDishes: List<String>
)

/**
 * Navigation events from Home screen
 */
sealed class HomeNavigationEvent {
    data class NavigateToRecipeDetail(
        val recipeId: String,
        val isLocked: Boolean = false,
        val fromMealPlan: Boolean = true  // Always true when navigating from Home
    ) : HomeNavigationEvent()
    data object NavigateToSettings : HomeNavigationEvent()
    data object NavigateToNotifications : HomeNavigationEvent()
    data object NavigateToGrocery : HomeNavigationEvent()
    data object NavigateToChat : HomeNavigationEvent()
    data object NavigateToFavorites : HomeNavigationEvent()
    data object NavigateToStats : HomeNavigationEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mealPlanRepository: MealPlanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _navigationEvent = Channel<HomeNavigationEvent>()
    val navigationEvent: Flow<HomeNavigationEvent> = _navigationEvent.receiveAsFlow()

    init {
        loadMealPlan()
    }

    // region Data Loading

    private fun loadMealPlan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                mealPlanRepository.getMealPlanForDate(LocalDate.now()).collect { mealPlan ->
                    if (mealPlan != null) {
                        updateStateWithMealPlan(mealPlan)
                    } else {
                        // Generate a new meal plan if none exists
                        generateNewMealPlan()
                    }
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading meal plan")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load meal plan. Please try again."
                    )
                }
            }
        }
    }

    private fun updateStateWithMealPlan(mealPlan: MealPlan) {
        val selectedDate = _uiState.value.selectedDate
        val selectedDayMeals = mealPlan.days.find { it.date == selectedDate }
        val weekDates = generateWeekDates(mealPlan.weekStartDate, selectedDate)
        val upcomingFestival = findUpcomingFestival(mealPlan.days)

        _uiState.update {
            it.copy(
                mealPlan = mealPlan,
                selectedDayMeals = selectedDayMeals,
                weekDates = weekDates,
                upcomingFestival = upcomingFestival
            )
        }
    }

    private suspend fun generateNewMealPlan() {
        val weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        mealPlanRepository.generateMealPlan(weekStart)
            .onSuccess { Timber.i("Generated new meal plan") }
            .onFailure { e -> Timber.e(e, "Failed to generate meal plan") }
    }

    private fun generateWeekDates(weekStart: LocalDate, selectedDate: LocalDate): List<WeekDay> {
        val today = LocalDate.now()
        return (0..6).map { offset ->
            val date = weekStart.plusDays(offset.toLong())
            WeekDay(
                date = date,
                dayName = date.dayOfWeek.name.take(2),
                dayNumber = date.dayOfMonth,
                isSelected = date == selectedDate,
                isToday = date == today
            )
        }
    }

    private fun findUpcomingFestival(days: List<MealPlanDay>): FestivalInfo? {
        val today = LocalDate.now()
        return days
            .mapNotNull { it.festival }
            .filter { !it.date.isBefore(today) }
            .minByOrNull { it.date }
            ?.let { festival ->
                val daysUntil = ChronoUnit.DAYS.between(today, festival.date).toInt()
                FestivalInfo(
                    name = festival.name,
                    daysUntil = daysUntil,
                    suggestedDishes = festival.suggestedDishes
                )
            }
    }

    // endregion

    // region Date Selection

    fun selectDate(date: LocalDate) {
        val mealPlan = _uiState.value.mealPlan ?: return
        val selectedDayMeals = mealPlan.days.find { it.date == date }
        val weekDates = generateWeekDates(mealPlan.weekStartDate, date)

        _uiState.update {
            it.copy(
                selectedDate = date,
                selectedDayMeals = selectedDayMeals,
                weekDates = weekDates
            )
        }
    }

    // endregion

    // region Recipe Actions

    fun onRecipeClick(mealItem: MealItem, mealType: MealType) {
        _uiState.update {
            it.copy(
                showRecipeActionSheet = true,
                selectedMealItem = mealItem,
                selectedMealType = mealType
            )
        }
    }

    fun dismissRecipeActionSheet() {
        _uiState.update {
            it.copy(
                showRecipeActionSheet = false,
                selectedMealItem = null,
                selectedMealType = null
            )
        }
    }

    fun viewRecipe() {
        val state = _uiState.value
        val mealItem = state.selectedMealItem ?: return
        val mealType = state.selectedMealType ?: return
        val isLocked = state.isRecipeEffectivelyLocked(mealItem, mealType)
        dismissRecipeActionSheet()
        _navigationEvent.trySend(HomeNavigationEvent.NavigateToRecipeDetail(mealItem.recipeId, isLocked))
    }

    fun showSwapOptions() {
        dismissRecipeActionSheet()
        _uiState.update { it.copy(showSwapSheet = true) }
    }

    fun dismissSwapSheet() {
        _uiState.update { it.copy(showSwapSheet = false) }
    }

    fun swapRecipe(newRecipeId: String? = null) {
        val state = _uiState.value
        val mealPlan = state.mealPlan ?: return
        val mealItem = state.selectedMealItem ?: return
        val mealType = state.selectedMealType ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            mealPlanRepository.swapMeal(
                mealPlanId = mealPlan.id,
                date = state.selectedDate,
                mealType = mealType,
                currentRecipeId = mealItem.recipeId
            ).onSuccess {
                Timber.i("Recipe swapped successfully")
            }.onFailure { e ->
                Timber.e(e, "Failed to swap recipe")
                _uiState.update { it.copy(errorMessage = "Failed to swap recipe") }
            }

            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    showSwapSheet = false,
                    selectedMealItem = null,
                    selectedMealType = null
                )
            }
        }
    }

    fun toggleLockRecipe() {
        val state = _uiState.value
        val mealPlan = state.mealPlan ?: return
        val mealItem = state.selectedMealItem ?: return
        val mealType = state.selectedMealType ?: return

        viewModelScope.launch {
            mealPlanRepository.setMealLockState(
                mealPlanId = mealPlan.id,
                date = state.selectedDate,
                mealType = mealType,
                recipeId = mealItem.recipeId,
                isLocked = !mealItem.isLocked
            ).onSuccess {
                Timber.i("Recipe lock toggled")
            }.onFailure { e ->
                Timber.e(e, "Failed to toggle lock")
                _uiState.update { it.copy(errorMessage = "Failed to update recipe") }
            }

            dismissRecipeActionSheet()
        }
    }

    /**
     * Toggle lock state for the entire selected day (Level 1 - Day Lock)
     * When locked, all meals and recipes for that day are protected from regeneration
     */
    fun toggleDayLock() {
        val state = _uiState.value
        val selectedDate = state.selectedDate
        val currentLockState = state.dayLockStates[selectedDate] == true

        _uiState.update {
            it.copy(
                dayLockStates = it.dayLockStates + (selectedDate to !currentLockState)
            )
        }
        Timber.i("Day lock toggled for $selectedDate: ${!currentLockState}")
    }

    /**
     * Toggle lock state for a specific meal slot (Level 2 - Meal Lock)
     * When locked, all recipes in that meal slot are protected from regeneration
     */
    fun toggleMealLock(mealType: MealType) {
        val state = _uiState.value
        val selectedDate = state.selectedDate
        val key = Pair(selectedDate, mealType)
        val currentLockState = state.mealLockStates[key] == true

        _uiState.update {
            it.copy(
                mealLockStates = it.mealLockStates + (key to !currentLockState)
            )
        }
        Timber.i("Meal lock toggled for $selectedDate $mealType: ${!currentLockState}")
    }

    /**
     * Remove a recipe from a meal (only if not locked at any level)
     */
    fun removeRecipeFromMeal() {
        val state = _uiState.value
        val mealItem = state.selectedMealItem ?: return
        val mealType = state.selectedMealType ?: return

        // Check if removal is allowed (not locked at any level)
        if (state.isRecipeEffectivelyLocked(mealItem, mealType)) {
            _uiState.update { it.copy(errorMessage = "Cannot remove locked recipe") }
            dismissRecipeActionSheet()
            return
        }

        // TODO: Implement actual removal via repository
        Timber.i("Remove recipe ${mealItem.recipeName} from $mealType")
        dismissRecipeActionSheet()
    }

    /**
     * Toggle lock state for a recipe directly (from swipe action)
     * This doesn't go through the action sheet, so no need to dismiss it
     */
    fun toggleRecipeLockDirect(mealItem: MealItem, mealType: MealType) {
        val state = _uiState.value
        val mealPlan = state.mealPlan ?: return

        // Check if day or meal is locked - can't toggle individual recipe lock
        if (state.isSelectedDayLocked || state.isMealLocked(mealType)) {
            _uiState.update { it.copy(errorMessage = "Unlock day/meal first to change recipe lock") }
            return
        }

        viewModelScope.launch {
            mealPlanRepository.setMealLockState(
                mealPlanId = mealPlan.id,
                date = state.selectedDate,
                mealType = mealType,
                recipeId = mealItem.recipeId,
                isLocked = !mealItem.isLocked
            ).onSuccess {
                Timber.i("Recipe lock toggled directly: ${mealItem.recipeName}")
            }.onFailure { e ->
                Timber.e(e, "Failed to toggle recipe lock")
                _uiState.update { it.copy(errorMessage = "Failed to update recipe") }
            }
        }
    }

    /**
     * Remove a recipe from a meal directly (from swipe action)
     * This doesn't go through the action sheet
     */
    fun removeRecipeFromMealDirect(mealItem: MealItem, mealType: MealType) {
        val state = _uiState.value

        // Check if removal is allowed (not locked at any level)
        if (state.isRecipeEffectivelyLocked(mealItem, mealType)) {
            _uiState.update { it.copy(errorMessage = "Cannot remove locked recipe") }
            return
        }

        // TODO: Implement actual removal via repository
        Timber.i("Remove recipe directly: ${mealItem.recipeName} from $mealType")
    }

    // endregion

    // region Refresh Actions

    fun showRefreshOptions() {
        _uiState.update { it.copy(showRefreshSheet = true) }
    }

    fun dismissRefreshSheet() {
        _uiState.update { it.copy(showRefreshSheet = false) }
    }

    fun regenerateDay() {
        val state = _uiState.value
        val mealPlan = state.mealPlan ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, showRefreshSheet = false) }

            // For now, regenerate the whole plan (in real app, would regenerate just the day)
            val weekStart = mealPlan.weekStartDate
            mealPlanRepository.generateMealPlan(weekStart)
                .onSuccess { Timber.i("Day regenerated") }
                .onFailure { e ->
                    Timber.e(e, "Failed to regenerate day")
                    _uiState.update { it.copy(errorMessage = "Failed to regenerate meals") }
                }

            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun regenerateWeek() {
        val state = _uiState.value
        val mealPlan = state.mealPlan ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, showRefreshSheet = false) }

            mealPlanRepository.generateMealPlan(mealPlan.weekStartDate)
                .onSuccess { Timber.i("Week regenerated") }
                .onFailure { e ->
                    Timber.e(e, "Failed to regenerate week")
                    _uiState.update { it.copy(errorMessage = "Failed to regenerate meals") }
                }

            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    // endregion

    // region Navigation

    fun navigateToSettings() {
        _navigationEvent.trySend(HomeNavigationEvent.NavigateToSettings)
    }

    fun navigateToNotifications() {
        _navigationEvent.trySend(HomeNavigationEvent.NavigateToNotifications)
    }

    fun navigateToGrocery() {
        _navigationEvent.trySend(HomeNavigationEvent.NavigateToGrocery)
    }

    fun navigateToChat() {
        _navigationEvent.trySend(HomeNavigationEvent.NavigateToChat)
    }

    fun navigateToFavorites() {
        _navigationEvent.trySend(HomeNavigationEvent.NavigateToFavorites)
    }

    fun navigateToStats() {
        _navigationEvent.trySend(HomeNavigationEvent.NavigateToStats)
    }

    // endregion

    // region Error Handling

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // endregion
}
