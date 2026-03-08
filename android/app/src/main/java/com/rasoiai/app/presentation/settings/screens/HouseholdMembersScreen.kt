package com.rasoiai.app.presentation.settings.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rasoiai.app.presentation.common.TestTags
import com.rasoiai.app.presentation.settings.viewmodels.HouseholdMembersNavigationEvent
import com.rasoiai.app.presentation.settings.viewmodels.HouseholdMembersUiState
import com.rasoiai.app.presentation.settings.viewmodels.HouseholdMembersViewModel
import com.rasoiai.app.presentation.theme.spacing
import com.rasoiai.domain.model.HouseholdMember
import com.rasoiai.domain.model.HouseholdRole

// ---------------------------------------------------------------------------
// Route (hiltViewModel injection point — Screen/Route split pattern)
// ---------------------------------------------------------------------------

@Composable
fun HouseholdMembersRoute(
    onNavigateBack: () -> Unit,
    onNavigateToMemberDetail: (String) -> Unit,
    viewModel: HouseholdMembersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.navigationEvent) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is HouseholdMembersNavigationEvent.NavigateToMemberDetail ->
                    onNavigateToMemberDetail(event.memberId)
                is HouseholdMembersNavigationEvent.ShowSnackbar ->
                    snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    HouseholdMembersScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onPhoneChanged = viewModel::onPhoneChanged,
        onAddMember = viewModel::addMember,
        onRemoveMember = viewModel::removeMember,
        onShowAddDialog = viewModel::showAddMemberDialog,
        onDismissAddDialog = viewModel::dismissAddMemberDialog
    )
}

// ---------------------------------------------------------------------------
// Screen (pure — no ViewModel reference; receives UiState + lambdas only)
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdMembersScreen(
    uiState: HouseholdMembersUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onPhoneChanged: (String) -> Unit,
    onAddMember: () -> Unit,
    onRemoveMember: (String) -> Unit,
    onShowAddDialog: () -> Unit,
    onDismissAddDialog: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Members", fontWeight = FontWeight.Bold) },
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
        floatingActionButton = {
            if (uiState.isOwner) {
                FloatingActionButton(
                    onClick = onShowAddDialog,
                    modifier = Modifier
                        .testTag(TestTags.HOUSEHOLD_ADD_MEMBER_BUTTON)
                        .semantics { contentDescription = "Add household member" }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (uiState.members.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = spacing.md),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No members in this household yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = spacing.md)
                    .testTag(TestTags.HOUSEHOLD_MEMBERS_LIST),
                verticalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                item { Spacer(modifier = Modifier.height(spacing.sm)) }

                items(
                    items = uiState.members,
                    key = { member -> member.id }
                ) { member ->
                    MemberRow(
                        member = member,
                        isOwner = uiState.isOwner,
                        onRemove = { onRemoveMember(member.id) }
                    )
                }

                item { Spacer(modifier = Modifier.height(spacing.xl)) }
            }
        }
    }

    if (uiState.showAddMemberDialog) {
        AddMemberDialog(
            phoneNumber = uiState.phoneNumber,
            onPhoneChanged = onPhoneChanged,
            onConfirm = onAddMember,
            onDismiss = onDismissAddDialog
        )
    }
}

// ---------------------------------------------------------------------------
// Member row
// ---------------------------------------------------------------------------

@Composable
private fun MemberRow(
    member: HouseholdMember,
    isOwner: Boolean,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TestTags.HOUSEHOLD_MEMBER_ROW_PREFIX + member.id),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(spacing.md)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(spacing.md))

            // Name + portion size
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Portion: ${"%.1f".format(member.portionSize)}×",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .testTag(TestTags.HOUSEHOLD_MEMBER_PORTION_SIZE_PREFIX + member.id)
                        .semantics {
                            contentDescription = "Portion size ${"%.1f".format(member.portionSize)}"
                        }
                )
            }

            // Role badge
            RoleBadge(
                role = member.role,
                modifier = Modifier
                    .testTag(TestTags.HOUSEHOLD_MEMBER_ROLE_PREFIX + member.id)
                    .semantics { contentDescription = "Role ${member.role.value}" }
            )

            // Remove button — owner only, cannot remove the household owner themselves
            if (isOwner && member.role != HouseholdRole.OWNER) {
                Spacer(modifier = Modifier.width(spacing.sm))
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .testTag(TestTags.HOUSEHOLD_MEMBER_REMOVE_PREFIX + member.id)
                        .semantics { contentDescription = "Remove ${member.name}" }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Role badge
// ---------------------------------------------------------------------------

@Composable
private fun RoleBadge(
    role: HouseholdRole,
    modifier: Modifier = Modifier
) {
    val (label, containerColor, contentColor) = when (role) {
        HouseholdRole.OWNER -> Triple(
            "Owner",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        HouseholdRole.MEMBER -> Triple(
            "Member",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        HouseholdRole.GUEST -> Triple(
            "Guest",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(spacing.xs),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xxs)
        )
    }
}

// ---------------------------------------------------------------------------
// Add member dialog
// ---------------------------------------------------------------------------

@Composable
private fun AddMemberDialog(
    phoneNumber: String,
    onPhoneChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Member", fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                Text(
                    text = "Enter the phone number of the person you want to add.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(spacing.md))
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = onPhoneChanged,
                    label = { Text("Phone number") },
                    placeholder = { Text("+91 98765 43210") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(spacing.sm),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.HOUSEHOLD_ADD_MEMBER_PHONE_FIELD)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = phoneNumber.isNotBlank(),
                shape = RoundedCornerShape(spacing.sm)
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
