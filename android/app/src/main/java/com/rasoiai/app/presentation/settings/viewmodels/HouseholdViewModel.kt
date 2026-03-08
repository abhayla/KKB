package com.rasoiai.app.presentation.settings.viewmodels

import androidx.lifecycle.viewModelScope
import com.rasoiai.app.presentation.common.BaseUiState
import com.rasoiai.app.presentation.common.BaseViewModel
import com.rasoiai.domain.model.HouseholdDetail
import com.rasoiai.domain.model.HouseholdRole
import com.rasoiai.domain.repository.HouseholdRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HouseholdUiState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val householdDetail: HouseholdDetail? = null,
    val inviteCode: String = "",
    val showDeactivateDialog: Boolean = false,
    val showTransferDialog: Boolean = false,
    val showLeaveDialog: Boolean = false,
    val isCreating: Boolean = false,
    val householdName: String = ""
) : BaseUiState {

    /**
     * True when the current user is the owner of the household. Derived from
     * the member list — the backend guarantees exactly one OWNER per household.
     */
    val isOwner: Boolean
        get() = householdDetail?.members?.any { it.role == HouseholdRole.OWNER } ?: false
}

sealed class HouseholdNavigationEvent {
    data object NavigateToMembers : HouseholdNavigationEvent()
    data object NavigateBack : HouseholdNavigationEvent()
    data class ShowSnackbar(val message: String) : HouseholdNavigationEvent()
}

@HiltViewModel
class HouseholdViewModel @Inject constructor(
    private val householdRepository: HouseholdRepository
) : BaseViewModel<HouseholdUiState>(HouseholdUiState()) {

    private val _navigationEvent = Channel<HouseholdNavigationEvent>(Channel.BUFFERED)
    val navigationEvent = _navigationEvent.receiveAsFlow()

    init {
        loadHousehold()
    }

    // ---------------------------------------------------------------------------
    // Data loading
    // ---------------------------------------------------------------------------

    private fun loadHousehold() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            householdRepository.getUserHousehold().collect { detail ->
                updateState {
                    it.copy(
                        isLoading = false,
                        householdDetail = detail,
                        householdName = detail?.household?.name ?: it.householdName,
                        inviteCode = detail?.household?.inviteCode ?: it.inviteCode
                    )
                }
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Household mutations
    // ---------------------------------------------------------------------------

    /**
     * Create a new household with [name] and make the current user its owner.
     */
    fun createHousehold(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            updateState { it.copy(isCreating = true, error = null) }
            householdRepository.createHousehold(trimmed).fold(
                onSuccess = { detail ->
                    updateState {
                        it.copy(
                            isCreating = false,
                            householdDetail = detail,
                            inviteCode = detail.household.inviteCode,
                            householdName = detail.household.name
                        )
                    }
                    _navigationEvent.send(HouseholdNavigationEvent.ShowSnackbar("Household created!"))
                },
                onFailure = { e ->
                    updateState { it.copy(isCreating = false, error = e.message) }
                }
            )
        }
    }

    /**
     * Refresh the invite code for the current household. Only the owner may do this.
     */
    fun refreshInviteCode() {
        val householdId = currentState.householdDetail?.household?.id ?: return
        viewModelScope.launch {
            householdRepository.refreshInviteCode(householdId).fold(
                onSuccess = { newCode ->
                    updateState { it.copy(inviteCode = newCode.inviteCode) }
                    _navigationEvent.send(HouseholdNavigationEvent.ShowSnackbar("Invite code refreshed"))
                },
                onFailure = { e ->
                    updateState { it.copy(error = e.message) }
                }
            )
        }
    }

    /**
     * Deactivate the household. Navigates back on success.
     */
    fun deactivateHousehold() {
        val householdId = currentState.householdDetail?.household?.id ?: return
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, showDeactivateDialog = false) }
            householdRepository.deactivateHousehold(householdId).fold(
                onSuccess = {
                    updateState {
                        it.copy(
                            isLoading = false,
                            householdDetail = null,
                            inviteCode = ""
                        )
                    }
                    _navigationEvent.send(HouseholdNavigationEvent.NavigateBack)
                },
                onFailure = { e ->
                    updateState { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }

    /**
     * Leave the current household (non-owner only). Navigates back on success.
     */
    fun leaveHousehold() {
        val householdId = currentState.householdDetail?.household?.id ?: return
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, showLeaveDialog = false) }
            householdRepository.leaveHousehold(householdId).fold(
                onSuccess = {
                    updateState {
                        it.copy(
                            isLoading = false,
                            householdDetail = null,
                            inviteCode = ""
                        )
                    }
                    _navigationEvent.send(HouseholdNavigationEvent.NavigateBack)
                },
                onFailure = { e ->
                    updateState { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }

    /**
     * Transfer household ownership to [memberId]. Reloads household on success.
     */
    fun transferOwnership(memberId: String) {
        val householdId = currentState.householdDetail?.household?.id ?: return
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, showTransferDialog = false) }
            householdRepository.transferOwnership(householdId, memberId).fold(
                onSuccess = {
                    updateState { it.copy(isLoading = false) }
                    _navigationEvent.send(
                        HouseholdNavigationEvent.ShowSnackbar("Ownership transferred")
                    )
                    loadHousehold()
                },
                onFailure = { e ->
                    updateState { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }

    // ---------------------------------------------------------------------------
    // Field updates
    // ---------------------------------------------------------------------------

    /** Update the household name text field value without persisting to the server. */
    fun setHouseholdName(name: String) {
        updateState { it.copy(householdName = name) }
    }

    // ---------------------------------------------------------------------------
    // Navigation
    // ---------------------------------------------------------------------------

    fun navigateToMembers() {
        viewModelScope.launch {
            _navigationEvent.send(HouseholdNavigationEvent.NavigateToMembers)
        }
    }

    // ---------------------------------------------------------------------------
    // Dialog visibility
    // ---------------------------------------------------------------------------

    fun showDeactivateDialog() = updateState { it.copy(showDeactivateDialog = true) }
    fun dismissDeactivateDialog() = updateState { it.copy(showDeactivateDialog = false) }
    fun showTransferDialog() = updateState { it.copy(showTransferDialog = true) }
    fun dismissTransferDialog() = updateState { it.copy(showTransferDialog = false) }
    fun showLeaveDialog() = updateState { it.copy(showLeaveDialog = true) }
    fun dismissLeaveDialog() = updateState { it.copy(showLeaveDialog = false) }

    fun onInviteCodeCopied() {
        viewModelScope.launch {
            _navigationEvent.send(HouseholdNavigationEvent.ShowSnackbar("Invite code copied"))
        }
    }

    override fun onErrorDismissed() = updateState { it.copy(error = null) }
}
