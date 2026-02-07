package com.rasoiai.app.presentation.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rasoiai.app.presentation.common.TestTags
import com.rasoiai.app.presentation.notifications.components.NotificationItem
import com.rasoiai.app.presentation.theme.spacing

/**
 * Notifications screen showing user notifications with filtering and actions.
 *
 * Features:
 * - Pull-to-refresh
 * - Filter by All/Unread
 * - Swipe-to-delete
 * - Mark all as read
 * - Click to navigate to relevant content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRecipe: (String) -> Unit = {},
    onNavigateToMealPlan: () -> Unit = {},
    onNavigateToGrocery: () -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val pullToRefreshState = rememberPullToRefreshState()

    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is NotificationsNavigationEvent.NavigateBack -> onNavigateBack()
                is NotificationsNavigationEvent.NavigateToRecipe -> onNavigateToRecipe(event.recipeId)
                is NotificationsNavigationEvent.NavigateToMealPlan -> onNavigateToMealPlan()
                is NotificationsNavigationEvent.NavigateToGrocery -> onNavigateToGrocery()
                is NotificationsNavigationEvent.NavigateToStats -> onNavigateToStats()
            }
        }
    }

    // Handle error messages
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    // Handle pull-to-refresh
    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.refreshNotifications()
        }
    }

    // Update pull-to-refresh state when refresh completes
    LaunchedEffect(uiState.isRefreshing) {
        if (!uiState.isRefreshing) {
            pullToRefreshState.endRefresh()
        }
    }

    NotificationsScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onMarkAllRead = viewModel::markAllAsRead,
        onFilterSelected = viewModel::setFilter,
        onNotificationClick = viewModel::onNotificationClick,
        onDeleteNotification = viewModel::deleteNotification
    )
}

/**
 * Stateless content composable for NotificationsScreen.
 * Extracted for testability - tests can pass UiState directly without a ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotificationsScreenContent(
    uiState: NotificationsUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onNavigateBack: () -> Unit = {},
    onMarkAllRead: () -> Unit = {},
    onFilterSelected: (NotificationFilter) -> Unit = {},
    onNotificationClick: (com.rasoiai.domain.model.Notification) -> Unit = {},
    onDeleteNotification: (String) -> Unit = {}
) {
    Scaffold(
        modifier = Modifier.testTag(TestTags.NOTIFICATIONS_SCREEN),
        topBar = {
            NotificationsTopBar(
                hasUnread = uiState.hasUnread,
                onNavigateBack = onNavigateBack,
                onMarkAllRead = onMarkAllRead
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter chips
            FilterRow(
                selectedFilter = uiState.filter,
                unreadCount = uiState.unreadCount,
                onFilterSelected = onFilterSelected
            )

            // Content
            when {
                uiState.isLoading -> {
                    LoadingContent()
                }

                uiState.isEmpty -> {
                    EmptyContent(filter = uiState.filter)
                }

                else -> {
                    NotificationsList(
                        groupedNotifications = uiState.groupedNotifications,
                        onNotificationClick = onNotificationClick,
                        onDeleteNotification = onDeleteNotification
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationsTopBar(
    hasUnread: Boolean,
    onNavigateBack: () -> Unit,
    onMarkAllRead: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Notifications",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            if (hasUnread) {
                TextButton(
                    onClick = onMarkAllRead,
                    modifier = Modifier.testTag(TestTags.NOTIFICATIONS_MARK_ALL_READ)
                ) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Mark all read",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
private fun FilterRow(
    selectedFilter: NotificationFilter,
    unreadCount: Int,
    onFilterSelected: (NotificationFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        FilterChip(
            selected = selectedFilter == NotificationFilter.ALL,
            onClick = { onFilterSelected(NotificationFilter.ALL) },
            label = { Text("All") },
            modifier = Modifier.testTag(TestTags.NOTIFICATION_FILTER_ALL),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        FilterChip(
            selected = selectedFilter == NotificationFilter.UNREAD,
            onClick = { onFilterSelected(NotificationFilter.UNREAD) },
            label = {
                Text(
                    text = if (unreadCount > 0) "Unread ($unreadCount)" else "Unread"
                )
            },
            modifier = Modifier.testTag(TestTags.NOTIFICATION_FILTER_UNREAD),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(TestTags.NOTIFICATIONS_LOADING),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyContent(filter: NotificationFilter) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.lg)
            .testTag(TestTags.NOTIFICATIONS_EMPTY),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.NotificationsNone,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(spacing.md))

        Text(
            text = when (filter) {
                NotificationFilter.ALL -> "No notifications yet"
                NotificationFilter.UNREAD -> "No unread notifications"
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(spacing.sm))

        Text(
            text = when (filter) {
                NotificationFilter.ALL -> "You'll see festival reminders, meal updates, and shopping list notifications here."
                NotificationFilter.UNREAD -> "All caught up! Check 'All' to see previous notifications."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NotificationsList(
    groupedNotifications: Map<String, List<com.rasoiai.domain.model.Notification>>,
    onNotificationClick: (com.rasoiai.domain.model.Notification) -> Unit,
    onDeleteNotification: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(TestTags.NOTIFICATIONS_LIST),
        contentPadding = PaddingValues(
            horizontal = spacing.md,
            vertical = spacing.sm
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        groupedNotifications.forEach { (dateGroup, notifications) ->
            // Section header
            item(key = "header_$dateGroup") {
                Text(
                    text = dateGroup,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(
                        top = spacing.md,
                        bottom = spacing.xs
                    )
                )
            }

            // Notifications in this group
            items(
                items = notifications,
                key = { it.id }
            ) { notification ->
                NotificationItem(
                    notification = notification,
                    onClick = { onNotificationClick(notification) },
                    onDelete = { onDeleteNotification(notification.id) }
                )
            }
        }
    }
}
