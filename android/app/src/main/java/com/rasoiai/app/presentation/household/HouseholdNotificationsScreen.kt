package com.rasoiai.app.presentation.household

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rasoiai.app.presentation.common.TestTags
import com.rasoiai.app.presentation.theme.spacing
import com.rasoiai.domain.model.HouseholdNotification
import com.rasoiai.domain.model.HouseholdNotificationType
import java.time.format.DateTimeFormatter

// ---------------------------------------------------------------------------
// Route — owns ViewModel, collects state, wires lambdas
// ---------------------------------------------------------------------------

@Composable
fun HouseholdNotificationsRoute(
    onNavigateBack: () -> Unit,
    viewModel: HouseholdNotificationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                HouseholdNotificationsNavigationEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onErrorDismissed()
        }
    }

    HouseholdNotificationsScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateBack = viewModel::navigateBack,
        onMarkAsRead = viewModel::markAsRead
    )
}

// ---------------------------------------------------------------------------
// Screen — stateless, receives UiState + lambdas
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HouseholdNotificationsScreen(
    uiState: HouseholdNotificationsUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onNavigateBack: () -> Unit = {},
    onMarkAsRead: (String) -> Unit = {}
) {
    Scaffold(
        modifier = Modifier.testTag(TestTags.HOUSEHOLD_NOTIFICATION_SCREEN),
        topBar = {
            HouseholdNotificationsTopBar(
                unreadCount = uiState.unreadCount,
                onNavigateBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.notifications.isEmpty() -> {
                    HouseholdNotificationsEmptyState(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    NotificationList(
                        notifications = uiState.notifications,
                        onMarkAsRead = onMarkAsRead
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Top bar
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HouseholdNotificationsTopBar(
    unreadCount: Int,
    onNavigateBack: () -> Unit
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
            if (unreadCount > 0) {
                BadgedBox(
                    badge = {
                        Badge(modifier = Modifier.testTag(TestTags.HOUSEHOLD_NOTIFICATION_BADGE)) {
                            Text(
                                text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    },
                    modifier = Modifier.padding(end = spacing.md)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "$unreadCount unread notifications"
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

// ---------------------------------------------------------------------------
// Notification list
// ---------------------------------------------------------------------------

@Composable
private fun NotificationList(
    notifications: List<HouseholdNotification>,
    onMarkAsRead: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag(TestTags.HOUSEHOLD_NOTIFICATION_LIST),
        contentPadding = PaddingValues(
            horizontal = spacing.md,
            vertical = spacing.sm
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        items(
            items = notifications,
            key = { it.id }
        ) { notification ->
            HouseholdNotificationItem(
                notification = notification,
                onMarkAsRead = { onMarkAsRead(notification.id) }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Notification item card
// ---------------------------------------------------------------------------

private val timestampFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")

@Composable
private fun HouseholdNotificationItem(
    notification: HouseholdNotification,
    onMarkAsRead: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUnread = !notification.isRead
    val containerColor = if (isUnread) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TestTags.HOUSEHOLD_NOTIFICATION_ITEM_PREFIX + notification.id),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isUnread) 2.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.Top
        ) {
            // Type icon
            NotificationTypeIcon(type = notification.type)

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(spacing.xxs))

                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(spacing.xs))

                Text(
                    text = notification.createdAt.format(timestampFormatter),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                if (isUnread) {
                    Spacer(modifier = Modifier.height(spacing.xs))
                    OutlinedButton(
                        onClick = onMarkAsRead,
                        modifier = Modifier.testTag(
                            TestTags.HOUSEHOLD_NOTIFICATION_MARK_READ_PREFIX + notification.id
                        ),
                        contentPadding = PaddingValues(horizontal = spacing.md, vertical = spacing.xs)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Mark as read",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = spacing.xs)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationTypeIcon(type: HouseholdNotificationType) {
    val tint = when (type) {
        HouseholdNotificationType.MEMBER_JOINED,
        HouseholdNotificationType.MEMBER_LEFT -> MaterialTheme.colorScheme.secondary

        HouseholdNotificationType.PLAN_REGENERATED,
        HouseholdNotificationType.MEAL_STATUS_UPDATED -> MaterialTheme.colorScheme.primary

        HouseholdNotificationType.RULE_ADDED,
        HouseholdNotificationType.RULE_REMOVED -> MaterialTheme.colorScheme.tertiary

        HouseholdNotificationType.OWNERSHIP_TRANSFERRED,
        HouseholdNotificationType.GENERAL -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Icon(
        imageVector = Icons.Default.Notifications,
        contentDescription = null,
        tint = tint,
        modifier = Modifier
            .size(24.dp)
            .padding(top = 2.dp)
    )
}

// ---------------------------------------------------------------------------
// Empty state
// ---------------------------------------------------------------------------

@Composable
private fun HouseholdNotificationsEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(spacing.xl)
            .testTag(TestTags.HOUSEHOLD_NOTIFICATION_EMPTY),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .padding(bottom = spacing.md),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )

        Text(
            text = "No notifications yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(spacing.sm))

        Text(
            text = "You'll see updates here when household members join, meal plans change, or rules are added.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
