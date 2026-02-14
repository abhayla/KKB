package com.rasoiai.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rasoiai.domain.model.DietaryTag
import com.rasoiai.domain.model.Festival
import com.rasoiai.domain.model.MealItem
import com.rasoiai.domain.model.MealPlan
import com.rasoiai.domain.model.MealPlanDay
import com.rasoiai.domain.model.MealType
import com.rasoiai.domain.model.Recipe
import com.rasoiai.core.network.NetworkMonitor
import com.rasoiai.domain.repository.MealPlanRepository
import com.rasoiai.domain.repository.NotificationRepository
import com.rasoiai.domain.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    val isLoadingSwapSuggestions: Boolean = false,
    // 3-level locking system state
    val dayLockStates: Map<LocalDate, Boolean> = emptyMap(),
    val mealLockStates: Map<Pair<LocalDate, MealType>, Boolean> = emptyMap(),
    // Add Recipe Sheet state
    val showAddRecipeSheet: Boolean = false,
    val addRecipeMealType: MealType? = null,
    val addRecipeSuggestions: List<Recipe> = emptyList(),
    val addRecipeFavorites: List<Recipe> = emptyList(),
    val isLoadingAddRecipeSuggestions: Boolean = false,
    // Festival Recipes Sheet state
    val showFestivalRecipesSheet: Boolean = false,
    val festivalRecipes: List<Recipe> = emptyList(),
    val isLoadingFestivalRecipes: Boolean = false,
    val selectedFestivalMealType: MealType = MealType.LUNCH,
    // Favorite added feedback message (for Snackbar)
    val favoriteAddedMessage: String? = null,
    // Offline state
    val isOffline: Boolean = false,
    // Notification badge
    val notificationBadgeCount: Int = 0,
    // Week navigation
    val selectedWeekStart: LocalDate = LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)),
    val isCurrentWeek: Boolean = true,
    val hasNoMealPlanForWeek: Boolean = false
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
    private val mealPlanRepository: MealPlanRepository,
    private val recipeRepository: RecipeRepository,
    private val networkMonitor: NetworkMonitor,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _navigationEvent = Channel<HomeNavigationEvent>()
    val navigationEvent: Flow<HomeNavigationEvent> = _navigationEvent.receiveAsFlow()

    init {
        loadMealPlan()
        observeNetworkState()
        observeNotificationCount()
    }

    private fun observeNetworkState() {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.update { it.copy(isOffline = !online) }
            }
        }
    }

    private fun observeNotificationCount() {
        viewModelScope.launch {
            notificationRepository.getUnreadCount().collect { count ->
                _uiState.update { it.copy(notificationBadgeCount = count) }
            }
        }
    }

    // region Data Loading

    private fun loadMealPlan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                // Check once for existing meal plan using first()
                val existingPlan = mealPlanRepository.getMealPlanForDate(LocalDate.now()).first()

                if (existingPlan != null) {
                    // Meal plan exists - update UI and start observing changes
                    updateStateWithMealPlan(existingPlan)
                    _uiState.update { it.copy(isLoading = false) }
                    observeMealPlan()
                } else {
                    // No local meal plan - try fetching from backend first
                    val fetched = mealPlanRepository.fetchCurrentMealPlan()
                    if (fetched != null) {
                        Timber.i("Fetched existing meal plan from backend: ${fetched.id}")
                        updateStateWithMealPlan(fetched)
                        _uiState.update { it.copy(isLoading = false) }
                        observeMealPlan()
                    } else {
                        // No plan on backend either - generate new one
                        generateNewMealPlan()
                    }
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

    /**
     * Observe meal plan changes after initial load.
     * This starts a Flow collection that updates UI when Room data changes.
     */
    private fun observeMealPlan() {
        viewModelScope.launch {
            mealPlanRepository.getMealPlanForDate(LocalDate.now()).collect { mealPlan ->
                if (mealPlan != null) {
                    updateStateWithMealPlan(mealPlan)
                }
            }
        }
    }

    private fun updateStateWithMealPlan(mealPlan: MealPlan) {
        Timber.d("updateStateWithMealPlan: ${mealPlan.days.size} days")
        mealPlan.days.forEach { day ->
            Timber.d("  ${day.date}: B=${day.breakfast.size}, L=${day.lunch.size}, D=${day.dinner.size}, S=${day.snacks.size}")
        }

        val selectedDate = _uiState.value.selectedDate
        val selectedDayMeals = mealPlan.days.find { it.date == selectedDate }
        Timber.d("Looking for selectedDate=$selectedDate, found=${selectedDayMeals != null}")
        if (selectedDayMeals != null) {
            Timber.d("Selected day meals: B=${selectedDayMeals.breakfast.size}, L=${selectedDayMeals.lunch.size}, D=${selectedDayMeals.dinner.size}, S=${selectedDayMeals.snacks.size}")
        }
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
            .onSuccess { mealPlan ->
                Timber.i("Generated new meal plan: ${mealPlan.id}")
                updateStateWithMealPlan(mealPlan)
                _uiState.update { it.copy(isLoading = false) }
                // Start observing for future changes
                observeMealPlan()
            }
            .onFailure { e ->
                Timber.e(e, "Failed to generate meal plan")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to generate meal plan. Please check your connection and try again."
                    )
                }
            }
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

    // region Week Navigation

    fun navigateToPreviousWeek() {
        val currentWeekStart = _uiState.value.selectedWeekStart
        val previousWeekStart = currentWeekStart.minusWeeks(1)
        loadMealPlanForWeek(previousWeekStart)
    }

    fun navigateToNextWeek() {
        val currentWeekStart = _uiState.value.selectedWeekStart
        val nextWeekStart = currentWeekStart.plusWeeks(1)
        loadMealPlanForWeek(nextWeekStart)
    }

    fun navigateToCurrentWeek() {
        val today = LocalDate.now()
        val currentWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        loadMealPlanForWeek(currentWeekStart)
    }

    private fun loadMealPlanForWeek(weekStart: LocalDate) {
        val today = LocalDate.now()
        val currentWeekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val isCurrentWeek = weekStart == currentWeekMonday

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    selectedWeekStart = weekStart,
                    isCurrentWeek = isCurrentWeek,
                    hasNoMealPlanForWeek = false
                )
            }

            try {
                // Try to load meal plan for the target week from local DB
                val targetDate = if (isCurrentWeek) today else weekStart
                val existingPlan = mealPlanRepository.getMealPlanForDate(targetDate).first()

                if (existingPlan != null) {
                    updateStateWithMealPlan(existingPlan)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            selectedDate = targetDate,
                            hasNoMealPlanForWeek = false
                        )
                    }
                } else {
                    // No local plan - show empty state
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            mealPlan = null,
                            selectedDayMeals = null,
                            weekDates = generateWeekDates(weekStart, targetDate),
                            selectedDate = targetDate,
                            hasNoMealPlanForWeek = true
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading meal plan for week $weekStart")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load meal plan for this week."
                    )
                }
            }
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
        if (mealItem.recipeId.isBlank()) {
            Timber.w("viewRecipe: recipeId is blank for '${mealItem.recipeName}', skipping navigation")
            dismissRecipeActionSheet()
            _uiState.update { it.copy(errorMessage = "Recipe details not available") }
            return
        }
        val isLocked = state.isRecipeEffectivelyLocked(mealItem, mealType)
        dismissRecipeActionSheet()
        _navigationEvent.trySend(HomeNavigationEvent.NavigateToRecipeDetail(mealItem.recipeId, isLocked))
    }

    fun showSwapOptions() {
        // Capture selected item/type BEFORE dismissing, since dismissRecipeActionSheet nulls them
        val mealItem = _uiState.value.selectedMealItem
        val mealType = _uiState.value.selectedMealType
        _uiState.update {
            it.copy(
                showRecipeActionSheet = false,
                showSwapSheet = true,
                // Preserve selection for fetchSwapSuggestions
                selectedMealItem = mealItem,
                selectedMealType = mealType
            )
        }
        fetchSwapSuggestions()
    }

    private fun fetchSwapSuggestions() {
        val state = _uiState.value
        val mealType = state.selectedMealType ?: return
        val currentRecipeId = state.selectedMealItem?.recipeId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSwapSuggestions = true) }

            recipeRepository.searchRecipes(
                mealType = mealType,
                limit = 20
            ).onSuccess { recipes ->
                // Convert Recipe to MealItem and filter out current recipe
                val suggestions = recipes
                    .filter { it.id != currentRecipeId }
                    .mapIndexed { index, recipe ->
                        MealItem(
                            id = "swap_${recipe.id}",
                            recipeId = recipe.id,
                            recipeName = recipe.name,
                            recipeImageUrl = recipe.imageUrl,
                            prepTimeMinutes = recipe.prepTimeMinutes,
                            calories = recipe.nutrition?.calories ?: 0,
                            isLocked = false,
                            order = index,
                            dietaryTags = recipe.dietaryTags
                        )
                    }
                _uiState.update {
                    it.copy(
                        swapSuggestions = suggestions,
                        isLoadingSwapSuggestions = false
                    )
                }
            }.onFailure { e ->
                Timber.e(e, "Failed to fetch swap suggestions")
                _uiState.update {
                    it.copy(
                        swapSuggestions = emptyList(),
                        isLoadingSwapSuggestions = false
                    )
                }
            }
        }
    }

    fun dismissSwapSheet() {
        _uiState.update {
            it.copy(
                showSwapSheet = false,
                swapSuggestions = emptyList(),
                selectedMealItem = null,
                selectedMealType = null
            )
        }
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
                    swapSuggestions = emptyList(),
                    selectedMealItem = null,
                    selectedMealType = null
                )
            }
        }
    }

    fun selectSwapRecipe(mealItem: MealItem) {
        swapRecipe(mealItem.recipeId)
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
        val mealPlan = state.mealPlan ?: return

        // Check if removal is allowed (not locked at any level)
        if (state.isRecipeEffectivelyLocked(mealItem, mealType)) {
            _uiState.update { it.copy(errorMessage = "Cannot remove locked recipe") }
            dismissRecipeActionSheet()
            return
        }

        viewModelScope.launch {
            mealPlanRepository.removeRecipeFromMeal(
                mealPlanId = mealPlan.id,
                date = state.selectedDate,
                mealType = mealType,
                recipeId = mealItem.recipeId
            ).onSuccess {
                Timber.i("Recipe removed: ${mealItem.recipeName} from $mealType")
            }.onFailure { e ->
                Timber.e(e, "Failed to remove recipe")
                _uiState.update { it.copy(errorMessage = "Failed to remove recipe") }
            }
        }
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
        val mealPlan = state.mealPlan ?: return

        // Check if removal is allowed (not locked at any level)
        if (state.isRecipeEffectivelyLocked(mealItem, mealType)) {
            _uiState.update { it.copy(errorMessage = "Cannot remove locked recipe") }
            return
        }

        viewModelScope.launch {
            mealPlanRepository.removeRecipeFromMeal(
                mealPlanId = mealPlan.id,
                date = state.selectedDate,
                mealType = mealType,
                recipeId = mealItem.recipeId
            ).onSuccess {
                Timber.i("Recipe removed directly: ${mealItem.recipeName} from $mealType")
            }.onFailure { e ->
                Timber.e(e, "Failed to remove recipe")
                _uiState.update { it.copy(errorMessage = "Failed to remove recipe") }
            }
        }
    }

    // endregion

    // region Add Recipe Actions

    /**
     * Show the Add Recipe sheet for a specific meal type
     */
    fun showAddRecipeSheet(mealType: MealType) {
        _uiState.update {
            it.copy(
                showAddRecipeSheet = true,
                addRecipeMealType = mealType,
                isLoadingAddRecipeSuggestions = true
            )
        }
        Timber.i("Show Add Recipe sheet for $mealType")
        fetchAddRecipeSuggestions(mealType)
    }

    /**
     * Fetch recipe suggestions for the Add Recipe sheet
     */
    private fun fetchAddRecipeSuggestions(mealType: MealType, query: String? = null) {
        viewModelScope.launch {
            // Fetch suggestions based on meal type and optional search query
            recipeRepository.searchRecipes(query = query, mealType = mealType, limit = 20)
                .onSuccess { recipes ->
                    _uiState.update {
                        it.copy(
                            addRecipeSuggestions = recipes,
                            isLoadingAddRecipeSuggestions = false
                        )
                    }
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to fetch recipe suggestions")
                    _uiState.update {
                        it.copy(
                            addRecipeSuggestions = emptyList(),
                            isLoadingAddRecipeSuggestions = false
                        )
                    }
                }
        }

        // Fetch favorites (only on initial load, not on search)
        if (query.isNullOrBlank()) {
            viewModelScope.launch {
                try {
                    val favorites = recipeRepository.getFavoriteRecipes().first()
                    _uiState.update { it.copy(addRecipeFavorites = favorites) }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to fetch favorites")
                }
            }
        }
    }

    /**
     * Search recipes for the Add Recipe sheet with a query
     */
    fun searchAddRecipes(query: String) {
        val mealType = _uiState.value.addRecipeMealType ?: return
        _uiState.update { it.copy(isLoadingAddRecipeSuggestions = true) }
        fetchAddRecipeSuggestions(mealType, query.takeIf { it.isNotBlank() })
    }

    /**
     * Add a recipe to the current meal slot.
     * If added from Suggestions tab (isFromSuggestions=true), also auto-add to favorites.
     */
    fun addRecipeToMeal(recipe: Recipe, isFromSuggestions: Boolean = true) {
        val state = _uiState.value
        val mealPlan = state.mealPlan ?: return
        val mealType = state.addRecipeMealType ?: return
        val date = state.selectedDate

        viewModelScope.launch {
            mealPlanRepository.addRecipeToMeal(
                mealPlanId = mealPlan.id,
                date = date,
                mealType = mealType,
                recipeId = recipe.id,
                recipeName = recipe.name,
                recipeImageUrl = recipe.imageUrl,
                prepTimeMinutes = recipe.prepTimeMinutes,
                calories = recipe.nutrition?.calories ?: 0
            ).onSuccess { updatedPlan ->
                Timber.i("Recipe added to meal: ${recipe.name}")
                updateStateWithMealPlan(updatedPlan)

                // Auto-add to favorites if from Suggestions tab
                if (isFromSuggestions) {
                    autoAddToFavorites(recipe)
                }
            }.onFailure { e ->
                Timber.e(e, "Failed to add recipe to meal")
                _uiState.update { it.copy(errorMessage = "Failed to add recipe") }
            }
        }

        dismissAddRecipeSheet()
    }

    /**
     * Auto-add recipe to favorites (if not already a favorite).
     * Shows feedback message via Snackbar.
     */
    private suspend fun autoAddToFavorites(recipe: Recipe) {
        // toggleFavorite returns the new favorite state
        // We only want to add, not toggle off, so check first if already favorite
        // The recipe.isFavorite might not be reliable, so use toggleFavorite which handles this
        // If recipe is already a favorite, toggleFavorite would remove it, which we don't want
        // So we check if the recipe is already favorite from the favorites list
        val isAlreadyFavorite = _uiState.value.addRecipeFavorites.any { it.id == recipe.id }

        if (!isAlreadyFavorite) {
            recipeRepository.toggleFavorite(recipe.id)
                .onSuccess { isFavorite ->
                    if (isFavorite) {
                        Timber.i("Recipe auto-added to favorites: ${recipe.name}")
                        _uiState.update {
                            it.copy(favoriteAddedMessage = "${recipe.name} added to favorites")
                        }
                    }
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to auto-add recipe to favorites")
                    // Don't show error - this is a secondary action
                }
        }
    }

    /**
     * Clear the favorite added message after showing Snackbar
     */
    fun clearFavoriteAddedMessage() {
        _uiState.update { it.copy(favoriteAddedMessage = null) }
    }

    /**
     * Dismiss the Add Recipe sheet
     */
    fun dismissAddRecipeSheet() {
        _uiState.update {
            it.copy(
                showAddRecipeSheet = false,
                addRecipeMealType = null,
                addRecipeSuggestions = emptyList(),
                addRecipeFavorites = emptyList()
            )
        }
    }

    // endregion

    // region Festival Recipes Actions

    /**
     * Show the Festival Recipes sheet when banner is clicked
     */
    fun onFestivalBannerClick() {
        val festival = _uiState.value.upcomingFestival ?: return
        _uiState.update {
            it.copy(
                showFestivalRecipesSheet = true,
                isLoadingFestivalRecipes = true
            )
        }
        Timber.i("Show Festival Recipes sheet for ${festival.name}")
        fetchFestivalRecipes(festival.suggestedDishes)
    }

    /**
     * Fetch recipes for the festival based on suggested dishes
     */
    private fun fetchFestivalRecipes(suggestedDishes: List<String>) {
        viewModelScope.launch {
            // Search for recipes matching the suggested dishes
            val recipes = mutableListOf<Recipe>()

            for (dish in suggestedDishes.take(10)) {
                recipeRepository.searchRecipes(query = dish, limit = 1)
                    .onSuccess { results ->
                        recipes.addAll(results)
                    }
            }

            // If we didn't find specific dishes, fall back to general search
            if (recipes.isEmpty()) {
                recipeRepository.searchRecipes(limit = 10)
                    .onSuccess { results ->
                        recipes.addAll(results)
                    }
            }

            _uiState.update {
                it.copy(
                    festivalRecipes = recipes,
                    isLoadingFestivalRecipes = false
                )
            }
        }
    }

    /**
     * Navigate to recipe detail from festival sheet
     */
    fun onFestivalRecipeClick(recipe: Recipe) {
        dismissFestivalRecipesSheet()
        _navigationEvent.trySend(HomeNavigationEvent.NavigateToRecipeDetail(recipe.id, isLocked = false))
    }

    /**
     * Select a meal type for adding festival recipes to the meal plan
     */
    fun selectFestivalMealType(mealType: MealType) {
        _uiState.update { it.copy(selectedFestivalMealType = mealType) }
    }

    /**
     * Add a festival recipe to the current day's selected meal slot
     */
    fun addFestivalRecipeToMeal(recipe: Recipe) {
        val state = _uiState.value
        val mealPlan = state.mealPlan ?: return
        val mealType = state.selectedFestivalMealType
        val date = state.selectedDate

        viewModelScope.launch {
            mealPlanRepository.addRecipeToMeal(
                mealPlanId = mealPlan.id,
                date = date,
                mealType = mealType,
                recipeId = recipe.id,
                recipeName = recipe.name,
                recipeImageUrl = recipe.imageUrl,
                prepTimeMinutes = recipe.prepTimeMinutes,
                calories = recipe.nutrition?.calories ?: 0
            ).onSuccess { updatedPlan ->
                Timber.i("Festival recipe added to meal: ${recipe.name} -> $mealType")
                updateStateWithMealPlan(updatedPlan)
                _uiState.update {
                    it.copy(favoriteAddedMessage = "${recipe.name} added to ${mealType.name.lowercase()}")
                }
            }.onFailure { e ->
                Timber.e(e, "Failed to add festival recipe to meal")
                _uiState.update { it.copy(errorMessage = "Failed to add recipe to meal") }
            }
        }

        dismissFestivalRecipesSheet()
    }

    /**
     * Dismiss the Festival Recipes sheet
     */
    fun dismissFestivalRecipesSheet() {
        _uiState.update {
            it.copy(
                showFestivalRecipesSheet = false,
                festivalRecipes = emptyList()
            )
        }
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
