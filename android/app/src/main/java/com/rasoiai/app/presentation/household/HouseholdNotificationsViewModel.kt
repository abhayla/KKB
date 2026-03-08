package com.rasoiai.app.presentation.household

import androidx.lifecycle.viewModelScope
import com.rasoiai.app.presentation.common.BaseUiState
import com.rasoiai.app.presentation.common.BaseViewModel
import com.rasoiai.domain.model.HouseholdNotification
import com.rasoiai.domain.repository.HouseholdRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HouseholdNotificationsUiState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val notifications: List<HouseholdNotification> = emptyList(),
    val unreadCount: Int = 0,
    val householdId: String? = null
) : BaseUiState

sealed class HouseholdNotificationsNavigationEvent {
    data object NavigateBack : HouseholdNotificationsNavigationEvent()
}

@HiltViewModel
class HouseholdNotificationsViewModel @Inject constructor(
    private val householdRepository: HouseholdRepository
) : BaseViewModel<HouseholdNotificationsUiState>(HouseholdNotificationsUiState()) {

    private val _navigationEvent = Channel<HouseholdNotificationsNavigationEvent>(Channel.BUFFERED)
    val navigationEvent = _navigationEvent.receiveAsFlow()

    init {
        loadNotifications()
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            householdRepository.getUserHousehold().collect { detail ->
                if (detail != null) {
                    updateState { it.copy(householdId = detail.household.id) }
                    householdRepository.getHouseholdNotifications(detail.household.id)
                        .collect { notifications ->
                            updateState {
                                it.copy(
                                    isLoading = false,
                                    notifications = notifications,
                                    unreadCount = notifications.count { n -> !n.isRead }
                                )
                            }
                        }
                } else {
                    updateState { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            householdRepository.markNotificationRead(notificationId).onFailure { e ->
                updateState { it.copy(error = e.message) }
            }
        }
    }

    fun navigateBack() {
        viewModelScope.launch {
            _navigationEvent.send(HouseholdNotificationsNavigationEvent.NavigateBack)
        }
    }

    override fun onErrorDismissed() = updateState { it.copy(error = null) }
}
