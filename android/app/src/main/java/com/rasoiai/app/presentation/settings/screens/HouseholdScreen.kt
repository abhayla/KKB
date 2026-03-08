package com.rasoiai.app.presentation.settings.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rasoiai.app.presentation.common.TestTags
import com.rasoiai.app.presentation.settings.viewmodels.HouseholdNavigationEvent
import com.rasoiai.app.presentation.settings.viewmodels.HouseholdUiState
import com.rasoiai.app.presentation.settings.viewmodels.HouseholdViewModel
import com.rasoiai.app.presentation.theme.spacing
import com.rasoiai.domain.model.HouseholdDetail
import com.rasoiai.domain.model.HouseholdMember
import com.rasoiai.domain.model.HouseholdRole

// ---------------------------------------------------------------------------
// Route — injects ViewModel, wires navigation events
// ---------------------------------------------------------------------------

@Composable
fun HouseholdRoute(
    onNavigateBack: () -> Unit,
    onNavigateToMembers: () -> Unit,
    viewModel: HouseholdViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is HouseholdNavigationEvent.NavigateBack -> onNavigateBack()
                is HouseholdNavigationEvent.NavigateToMembers -> onNavigateToMembers()
                is HouseholdNavigationEvent.ShowSnackbar ->
                    snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    HouseholdScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onHouseholdNameChanged = viewModel::setHouseholdName,
        onCreateHousehold = { viewModel.createHousehold(uiState.householdName) },
        onRefreshInviteCode = viewModel::refreshInviteCode,
        onNavigateToMembers = viewModel::navigateToMembers,
        onInviteCodeCopied = {
            val code = uiState.inviteCode.ifBlank {
                uiState.householdDetail?.household?.inviteCode ?: ""
            }
            if (code.isNotBlank()) {
                val clipboard =
                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Invite Code", code))
                viewModel.onInviteCodeCopied()
            }
        },
        onShareInviteCode = { code ->
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    android.content.Intent.EXTRA_TEXT,
                    "Join my household on RasoiAI! Use code: $code"
                )
            }
            context.startActivity(
                android.content.Intent.createChooser(intent, "Share invite code")
            )
        },
        onShowDeactivateDialog = viewModel::showDeactivateDialog,
        onDismissDeactivateDialog = viewModel::dismissDeactivateDialog,
        onConfirmDeactivate = viewModel::deactivateHousehold,
        onShowLeaveDialog = viewModel::showLeaveDialog,
        onDismissLeaveDialog = viewModel::dismissLeaveDialog,
        onConfirmLeave = viewModel::leaveHousehold,
        onShowTransferDialog = viewModel::showTransferDialog,
        onDismissTransferDialog = viewModel::dismissTransferDialog,
        onConfirmTransfer = viewModel::transferOwnership
    )
}

// ---------------------------------------------------------------------------
// Screen — pure composable, receives UiState + lambdas only
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdScreen(
    uiState: HouseholdUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onHouseholdNameChanged: (String) -> Unit,
    onCreateHousehold: () -> Unit,
    onRefreshInviteCode: () -> Unit,
    onNavigateToMembers: () -> Unit,
    onInviteCodeCopied: () -> Unit,
    onShareInviteCode: (String) -> Unit,
    onShowDeactivateDialog: () -> Unit,
    onDismissDeactivateDialog: () -> Unit,
    onConfirmDeactivate: () -> Unit,
    onShowLeaveDialog: () -> Unit,
    onDismissLeaveDialog: () -> Unit,
    onConfirmLeave: () -> Unit,
    onShowTransferDialog: () -> Unit,
    onDismissTransferDialog: () -> Unit,
    onConfirmTransfer: (String) -> Unit
) {
    Scaffold(
        modifier = Modifier.testTag(TestTags.HOUSEHOLD_SCREEN),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Household",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Spacer(modifier = Modifier.height(spacing.xs))

            if (uiState.householdDetail == null) {
                NoHouseholdContent(
                    householdName = uiState.householdName,
                    isCreating = uiState.isCreating,
                    onNameChanged = onHouseholdNameChanged,
                    onCreateHousehold = onCreateHousehold
                )
            } else {
                HouseholdDetailContent(
                    detail = uiState.householdDetail,
                    householdName = uiState.householdName,
                    inviteCode = uiState.inviteCode.ifBlank {
                        uiState.householdDetail.household.inviteCode
                    },
                    isOwner = uiState.isOwner,
                    onNameChanged = onHouseholdNameChanged,
                    onRefreshInviteCode = onRefreshInviteCode,
                    onNavigateToMembers = onNavigateToMembers,
                    onInviteCodeCopied = onInviteCodeCopied,
                    onShareInviteCode = onShareInviteCode,
                    onShowDeactivateDialog = onShowDeactivateDialog,
                    onShowLeaveDialog = onShowLeaveDialog,
                    onShowTransferDialog = onShowTransferDialog
                )
            }

            Spacer(modifier = Modifier.height(spacing.md))
        }
    }

    // Deactivate Confirmation Dialog
    if (uiState.showDeactivateDialog) {
        AlertDialog(
            onDismissRequest = onDismissDeactivateDialog,
            title = { Text("Deactivate Household") },
            text = {
                Text(
                    "This will deactivate the household and remove all members. " +
                        "This action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirmDeactivate,
                    modifier = Modifier.testTag(TestTags.HOUSEHOLD_DEACTIVATE_BUTTON),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Deactivate")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeactivateDialog) {
                    Text("Cancel")
                }
            }
        )
    }

    // Leave Confirmation Dialog
    if (uiState.showLeaveDialog) {
        AlertDialog(
            onDismissRequest = onDismissLeaveDialog,
            title = { Text("Leave Household") },
            text = {
                Text(
                    "Are you sure you want to leave this household? " +
                        "Your personal meal plan will not be affected."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirmLeave,
                    modifier = Modifier.testTag(TestTags.HOUSEHOLD_LEAVE_BUTTON),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissLeaveDialog) {
                    Text("Cancel")
                }
            }
        )
    }

    // Transfer Ownership Dialog
    if (uiState.showTransferDialog) {
        val eligibleMembers = uiState.householdDetail?.members
            ?.filter { it.role != HouseholdRole.OWNER }
            ?: emptyList()

        TransferOwnershipDialog(
            members = eligibleMembers,
            onDismiss = onDismissTransferDialog,
            onConfirm = onConfirmTransfer
        )
    }
}

// ---------------------------------------------------------------------------
// No-household state
// ---------------------------------------------------------------------------

@Composable
private fun NoHouseholdContent(
    householdName: String,
    isCreating: Boolean,
    onNameChanged: (String) -> Unit,
    onCreateHousehold: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Text(
                text = "Create a Household",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Create a shared household to plan meals together with your family.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = householdName,
                onValueChange = onNameChanged,
                label = { Text("Household name") },
                placeholder = { Text("e.g. Sharma Family") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.HOUSEHOLD_NAME_FIELD),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                enabled = !isCreating
            )

            Button(
                onClick = onCreateHousehold,
                enabled = householdName.isNotBlank() && !isCreating,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.HOUSEHOLD_CREATE_BUTTON),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(spacing.sm))
                }
                Text(if (isCreating) "Creating..." else "Create Household")
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Existing household — full detail
// ---------------------------------------------------------------------------

@Composable
private fun HouseholdDetailContent(
    detail: HouseholdDetail,
    householdName: String,
    inviteCode: String,
    isOwner: Boolean,
    onNameChanged: (String) -> Unit,
    onRefreshInviteCode: () -> Unit,
    onNavigateToMembers: () -> Unit,
    onInviteCodeCopied: () -> Unit,
    onShareInviteCode: (String) -> Unit,
    onShowDeactivateDialog: () -> Unit,
    onShowLeaveDialog: () -> Unit,
    onShowTransferDialog: () -> Unit
) {
    // Household Name Card
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Text(
                text = "Household Name",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isOwner) {
                OutlinedTextField(
                    value = householdName,
                    onValueChange = onNameChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.HOUSEHOLD_NAME_FIELD),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
            } else {
                Text(
                    text = detail.household.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    // Invite Code Card
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Text(
                text = "Invite Code",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = inviteCode,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag(TestTags.HOUSEHOLD_INVITE_CODE)
            )
            Text(
                text = "Share this code with family members to join your household.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                OutlinedButton(
                    onClick = onInviteCodeCopied,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy invite code",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(spacing.xs))
                    Text("Copy")
                }
                OutlinedButton(
                    onClick = { onShareInviteCode(inviteCode) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag(TestTags.HOUSEHOLD_INVITE_SHARE_BUTTON),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share invite code",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(spacing.xs))
                    Text("Share")
                }
                if (isOwner) {
                    IconButton(
                        onClick = onRefreshInviteCode,
                        modifier = Modifier.testTag(TestTags.HOUSEHOLD_INVITE_REFRESH_BUTTON)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh invite code"
                        )
                    }
                }
            }
        }
    }

    // Members Card
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Group,
                contentDescription = "Members",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Members",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${detail.members.size} of ${detail.household.maxMembers} members",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onNavigateToMembers) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "View members"
                )
            }
        }
    }

    // Actions Card (danger zone)
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Text(
                text = "Actions",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isOwner) {
                OutlinedButton(
                    onClick = onShowTransferDialog,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.HOUSEHOLD_TRANSFER_BUTTON),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Transfer Ownership")
                }
                OutlinedButton(
                    onClick = onShowDeactivateDialog,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.HOUSEHOLD_DEACTIVATE_BUTTON),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Deactivate Household")
                }
            } else {
                OutlinedButton(
                    onClick = onShowLeaveDialog,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.HOUSEHOLD_LEAVE_BUTTON),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Leave Household")
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Transfer Ownership Dialog
// ---------------------------------------------------------------------------

@Composable
private fun TransferOwnershipDialog(
    members: List<HouseholdMember>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedMemberId by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        modifier = Modifier.testTag(TestTags.HOUSEHOLD_TRANSFER_DIALOG),
        onDismissRequest = onDismiss,
        title = { Text("Transfer Ownership") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Text(
                    text = "Select a member to transfer ownership to. " +
                        "You will become a regular member.",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (members.isEmpty()) {
                    Text(
                        text = "No other members available to transfer ownership to.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    members.forEach { member ->
                        val isSelected = member.id == selectedMemberId
                        TextButton(
                            onClick = { selectedMemberId = member.id },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text(
                                text = member.name,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedMemberId?.let { onConfirm(it) } },
                enabled = selectedMemberId != null
            ) {
                Text("Transfer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
