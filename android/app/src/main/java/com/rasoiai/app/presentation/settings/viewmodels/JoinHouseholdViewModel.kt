package com.rasoiai.app.presentation.settings.viewmodels

import androidx.lifecycle.viewModelScope
import com.rasoiai.app.presentation.common.BaseUiState
import com.rasoiai.app.presentation.common.BaseViewModel
import com.rasoiai.domain.repository.HouseholdRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JoinHouseholdUiState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val inviteCode: String = "",
    val joinSuccess: Boolean = false
) : BaseUiState

sealed class JoinHouseholdNavigationEvent {
    data object NavigateToHousehold : JoinHouseholdNavigationEvent()
}

@HiltViewModel
class JoinHouseholdViewModel @Inject constructor(
    private val householdRepository: HouseholdRepository
) : BaseViewModel<JoinHouseholdUiState>(JoinHouseholdUiState()) {

    private val _navigationEvent = Channel<JoinHouseholdNavigationEvent>(Channel.BUFFERED)
    val navigationEvent = _navigationEvent.receiveAsFlow()

    fun onCodeChanged(code: String) = updateState { it.copy(inviteCode = code, error = null) }

    fun joinHousehold() {
        val code = uiState.value.inviteCode.trim()
        if (code.isBlank()) {
            updateState { it.copy(error = "Please enter an invite code") }
            return
        }
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }
            householdRepository.joinHousehold(code).fold(
                onSuccess = {
                    updateState { it.copy(isLoading = false, joinSuccess = true) }
                    _navigationEvent.send(JoinHouseholdNavigationEvent.NavigateToHousehold)
                },
                onFailure = { e ->
                    val errorMsg = when {
                        e.message?.contains("expired", ignoreCase = true) == true ->
                            "This invite code has expired"
                        e.message?.contains("full", ignoreCase = true) == true ->
                            "This household is full"
                        e.message?.contains("404", ignoreCase = true) == true ->
                            "Invalid invite code"
                        else -> e.message ?: "Failed to join household"
                    }
                    updateState { it.copy(isLoading = false, error = errorMsg) }
                }
            )
        }
    }

    override fun onErrorDismissed() = updateState { it.copy(error = null) }
}
