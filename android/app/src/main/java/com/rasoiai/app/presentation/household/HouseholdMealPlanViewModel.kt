package com.rasoiai.app.presentation.household

import androidx.lifecycle.viewModelScope
import com.rasoiai.app.presentation.common.BaseUiState
import com.rasoiai.app.presentation.common.BaseViewModel
import com.rasoiai.domain.model.MealPlan
import com.rasoiai.domain.model.MealPlanDay
import com.rasoiai.domain.repository.HouseholdRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HouseholdMealPlanUiState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val mealPlan: MealPlan? = null,
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedDayMeals: MealPlanDay? = null,
    val householdId: String? = null
) : BaseUiState

sealed class HouseholdMealPlanNavigationEvent {
    data object NavigateBack : HouseholdMealPlanNavigationEvent()
}

@HiltViewModel
class HouseholdMealPlanViewModel @Inject constructor(
    private val householdRepository: HouseholdRepository
) : BaseViewModel<HouseholdMealPlanUiState>(HouseholdMealPlanUiState()) {

    private val _navigationEvent = Channel<HouseholdMealPlanNavigationEvent>(Channel.BUFFERED)
    val navigationEvent = _navigationEvent.receiveAsFlow()

    init {
        loadHouseholdAndPlan()
    }

    private fun loadHouseholdAndPlan() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            householdRepository.getUserHousehold().collect { detail ->
                if (detail != null) {
                    updateState { it.copy(householdId = detail.household.id) }
                    loadMealPlan(detail.household.id)
                } else {
                    updateState { it.copy(isLoading = false) }
                }
            }
        }
    }

    private fun loadMealPlan(householdId: String) {
        viewModelScope.launch {
            householdRepository.getHouseholdMealPlan(householdId).collect { plan ->
                val selectedDay = plan?.days?.find { it.date == uiState.value.selectedDate }
                    ?: plan?.days?.firstOrNull()
                updateState {
                    it.copy(
                        isLoading = false,
                        mealPlan = plan,
                        selectedDayMeals = selectedDay
                    )
                }
            }
        }
    }

    fun selectDate(date: LocalDate) {
        val day = uiState.value.mealPlan?.days?.find { it.date == date }
        updateState { it.copy(selectedDate = date, selectedDayMeals = day) }
    }

    fun navigateBack() {
        viewModelScope.launch {
            _navigationEvent.send(HouseholdMealPlanNavigationEvent.NavigateBack)
        }
    }

    override fun onErrorDismissed() = updateState { it.copy(error = null) }
}
