package com.rasoiai.app.presentation.settings.viewmodels

import androidx.lifecycle.viewModelScope
import com.rasoiai.app.presentation.common.BaseUiState
import com.rasoiai.app.presentation.common.BaseViewModel
import com.rasoiai.domain.model.HouseholdMember
import com.rasoiai.domain.repository.HouseholdRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HouseholdMembersUiState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val members: List<HouseholdMember> = emptyList(),
    val householdId: String? = null,
    val isOwner: Boolean = false,
    val showAddMemberDialog: Boolean = false,
    val phoneNumber: String = ""
) : BaseUiState

sealed class HouseholdMembersNavigationEvent {
    data class NavigateToMemberDetail(val memberId: String) : HouseholdMembersNavigationEvent()
    data class ShowSnackbar(val message: String) : HouseholdMembersNavigationEvent()
}

@HiltViewModel
class HouseholdMembersViewModel @Inject constructor(
    private val householdRepository: HouseholdRepository
) : BaseViewModel<HouseholdMembersUiState>(HouseholdMembersUiState()) {

    private val _navigationEvent = Channel<HouseholdMembersNavigationEvent>(Channel.BUFFERED)
    val navigationEvent = _navigationEvent.receiveAsFlow()

    init {
        loadMembers()
    }

    private fun loadMembers() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            householdRepository.getUserHousehold().collect { detail ->
                if (detail != null) {
                    val currentUserId = detail.members
                        .firstOrNull { it.role.value == "owner" }
                        ?.userId
                    updateState {
                        it.copy(
                            isLoading = false,
                            members = detail.members,
                            householdId = detail.household.id,
                            isOwner = detail.household.ownerId == currentUserId
                        )
                    }
                } else {
                    updateState { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun onPhoneChanged(phone: String) = updateState { it.copy(phoneNumber = phone) }

    fun addMember() {
        val householdId = uiState.value.householdId ?: return
        val phone = uiState.value.phoneNumber.trim()
        if (phone.isBlank()) return
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, showAddMemberDialog = false) }
            householdRepository.addMember(householdId, phone).fold(
                onSuccess = {
                    updateState { it.copy(isLoading = false, phoneNumber = "") }
                    _navigationEvent.send(HouseholdMembersNavigationEvent.ShowSnackbar("Member added"))
                },
                onFailure = { e ->
                    updateState { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }

    fun removeMember(memberId: String) {
        val householdId = uiState.value.householdId ?: return
        viewModelScope.launch {
            householdRepository.removeMember(householdId, memberId).fold(
                onSuccess = {
                    _navigationEvent.send(HouseholdMembersNavigationEvent.ShowSnackbar("Member removed"))
                },
                onFailure = { e ->
                    updateState { it.copy(error = e.message) }
                }
            )
        }
    }

    fun showAddMemberDialog() = updateState { it.copy(showAddMemberDialog = true) }

    fun dismissAddMemberDialog() = updateState { it.copy(showAddMemberDialog = false, phoneNumber = "") }

    override fun onErrorDismissed() = updateState { it.copy(error = null) }
}
