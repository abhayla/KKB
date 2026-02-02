package com.rasoiai.app.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rasoiai.domain.model.Notification
import com.rasoiai.domain.model.NotificationActionType
import com.rasoiai.domain.model.NotificationType
import com.rasoiai.domain.repository.NotificationRepository
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
import javax.inject.Inject

/**
 * Filter options for notifications list.
 */
enum class NotificationFilter {
    ALL,
    UNREAD
}

/**
 * UI state for the Notifications screen.
 */
data class NotificationsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val notifications: List<Notification> = emptyList(),
    val unreadCount: Int = 0,
    val filter: NotificationFilter = NotificationFilter.ALL
) {
    /** Filtered notifications based on current filter */
    val filteredNotifications: List<Notification>
        get() = when (filter) {
            NotificationFilter.ALL -> notifications
            NotificationFilter.UNREAD -> notifications.filter { !it.isRead }
        }

    /** Check if the list is empty (considering filter) */
    val isEmpty: Boolean
        get() = filteredNotifications.isEmpty()

    /** Check if there are any unread notifications */
    val hasUnread: Boolean
        get() = unreadCount > 0

    /** Group notifications by date for display */
    val groupedNotifications: Map<String, List<Notification>>
        get() = filteredNotifications.groupBy { notification ->
            getDateGroup(notification.createdAt)
        }

    private fun getDateGroup(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val oneDay = 24 * 60 * 60 * 1000L
        val oneWeek = 7 * oneDay

        return when {
            diff < oneDay -> "Today"
            diff < 2 * oneDay -> "Yesterday"
            diff < oneWeek -> "This Week"
            else -> "Earlier"
        }
    }
}

/**
 * Navigation events from Notifications screen.
 */
sealed class NotificationsNavigationEvent {
    data object NavigateBack : NotificationsNavigationEvent()
    data class NavigateToRecipe(val recipeId: String) : NotificationsNavigationEvent()
    data object NavigateToMealPlan : NotificationsNavigationEvent()
    data object NavigateToGrocery : NotificationsNavigationEvent()
    data object NavigateToStats : NotificationsNavigationEvent()
}

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private val _navigationEvent = Channel<NotificationsNavigationEvent>()
    val navigationEvent: Flow<NotificationsNavigationEvent> = _navigationEvent.receiveAsFlow()

    init {
        loadNotifications()
        observeUnreadCount()
    }

    // region Data Loading

    private fun loadNotifications() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            notificationRepository.getNotifications().collect { notifications ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        notifications = notifications,
                        errorMessage = null
                    )
                }
            }
        }
    }

    private fun observeUnreadCount() {
        viewModelScope.launch {
            notificationRepository.getUnreadCount().collect { count ->
                _uiState.update { it.copy(unreadCount = count) }
            }
        }
    }

    fun refreshNotifications() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            notificationRepository.refreshNotifications()
                .onSuccess {
                    Timber.d("Notifications refreshed successfully")
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to refresh notifications")
                    _uiState.update { it.copy(errorMessage = "Failed to refresh notifications") }
                }

            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    // endregion

    // region Filter Actions

    fun setFilter(filter: NotificationFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    // endregion

    // region Notification Actions

    fun onNotificationClick(notification: Notification) {
        viewModelScope.launch {
            // Mark as read
            if (!notification.isRead) {
                notificationRepository.markAsRead(notification.id)
            }

            // Navigate based on action type
            when (notification.actionType) {
                NotificationActionType.OPEN_RECIPE -> {
                    notification.actionData?.recipeId?.let { recipeId ->
                        _navigationEvent.send(NotificationsNavigationEvent.NavigateToRecipe(recipeId))
                    }
                }

                NotificationActionType.OPEN_MEAL_PLAN -> {
                    _navigationEvent.send(NotificationsNavigationEvent.NavigateToMealPlan)
                }

                NotificationActionType.OPEN_GROCERY -> {
                    _navigationEvent.send(NotificationsNavigationEvent.NavigateToGrocery)
                }

                NotificationActionType.OPEN_STATS -> {
                    _navigationEvent.send(NotificationsNavigationEvent.NavigateToStats)
                }

                NotificationActionType.NONE -> {
                    // Just mark as read, no navigation
                }
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId)
                .onFailure { e ->
                    Timber.e(e, "Failed to mark notification as read")
                    _uiState.update { it.copy(errorMessage = "Failed to mark as read") }
                }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            notificationRepository.markAllAsRead()
                .onSuccess {
                    Timber.d("All notifications marked as read")
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to mark all notifications as read")
                    _uiState.update { it.copy(errorMessage = "Failed to mark all as read") }
                }
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.deleteNotification(notificationId)
                .onSuccess {
                    Timber.d("Notification deleted: $notificationId")
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to delete notification")
                    _uiState.update { it.copy(errorMessage = "Failed to delete notification") }
                }
        }
    }

    // endregion

    // region Navigation

    fun navigateBack() {
        _navigationEvent.trySend(NotificationsNavigationEvent.NavigateBack)
    }

    // endregion

    // region Error Handling

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // endregion

    // region Helpers

    /**
     * Get icon for notification type.
     */
    fun getNotificationIcon(type: NotificationType): String {
        return when (type) {
            NotificationType.FESTIVAL_REMINDER -> "celebration"
            NotificationType.MEAL_PLAN_UPDATE -> "restaurant_menu"
            NotificationType.SHOPPING_REMINDER -> "shopping_cart"
            NotificationType.RECIPE_SUGGESTION -> "lightbulb"
            NotificationType.STREAK_MILESTONE -> "local_fire_department"
        }
    }

    // endregion
}
