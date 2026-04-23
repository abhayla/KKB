package com.rasoiai.app.presentation.settings.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rasoiai.app.presentation.common.BaseUiState
import com.rasoiai.app.presentation.common.BaseViewModel
import com.rasoiai.app.presentation.common.TestTags
import com.rasoiai.domain.model.HouseholdMember
import com.rasoiai.domain.repository.HouseholdRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

// ==================== ViewModel ====================

data class HouseholdMemberDetailUiState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val member: HouseholdMember? = null,
    val canEditSharedPlan: Boolean = false,
    val portionSize: Float = 1.0f,
    val isTemporary: Boolean = false,
    val isOwner: Boolean = false,
    val showRemoveDialog: Boolean = false
) : BaseUiState

@HiltViewModel
class HouseholdMemberDetailViewModel @Inject constructor(
    private val householdRepository: HouseholdRepository
) : BaseViewModel<HouseholdMemberDetailUiState>(HouseholdMemberDetailUiState()) {

    private val _navigationEvent = Channel<String>(Channel.BUFFERED)
    val navigationEvent = _navigationEvent.receiveAsFlow()

    fun loadMember(memberId: String) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            householdRepository.getUserHousehold().collect { detail ->
                if (detail != null) {
                    val member = detail.members.find { it.id == memberId }
                    updateState { it.copy(
                        isLoading = false,
                        member = member,
                        canEditSharedPlan = member?.canEditSharedPlan ?: false,
                        portionSize = member?.portionSize ?: 1.0f,
                        isTemporary = member?.isTemporary ?: false,
                        isOwner = detail.members.any { m -> m.role.value == "owner" && m.userId != null }
                    ) }
                }
            }
        }
    }

    fun updatePermissions() {
        val member = uiState.value.member ?: return
        viewModelScope.launch {
            householdRepository.getUserHousehold().collect { detail ->
                if (detail != null) {
                    householdRepository.updateMember(
                        detail.household.id,
                        member.id,
                        canEditSharedPlan = uiState.value.canEditSharedPlan,
                        portionSize = uiState.value.portionSize,
                        isTemporary = uiState.value.isTemporary
                    )
                }
            }
        }
    }

    fun removeMember() {
        val member = uiState.value.member ?: return
        viewModelScope.launch {
            householdRepository.getUserHousehold().collect { detail ->
                if (detail != null) {
                    householdRepository.removeMember(detail.household.id, member.id).fold(
                        onSuccess = { _navigationEvent.send("back") },
                        onFailure = { e -> updateState { it.copy(error = e.message) } }
                    )
                }
            }
        }
    }

    fun setCanEditSharedPlan(value: Boolean) = updateState { it.copy(canEditSharedPlan = value) }
    fun setPortionSize(value: Float) = updateState { it.copy(portionSize = value) }
    fun setIsTemporary(value: Boolean) = updateState { it.copy(isTemporary = value) }
    fun showRemoveDialog() = updateState { it.copy(showRemoveDialog = true) }
    fun dismissRemoveDialog() = updateState { it.copy(showRemoveDialog = false) }
}

// ==================== Screen ====================

@Composable
fun HouseholdMemberDetailRoute(
    memberId: String,
    onNavigateBack: () -> Unit,
    viewModel: HouseholdMemberDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(memberId) {
        viewModel.loadMember(memberId)
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect {
            onNavigateBack()
        }
    }

    HouseholdMemberDetailScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onCanEditChanged = viewModel::setCanEditSharedPlan,
        onPortionSizeChanged = viewModel::setPortionSize,
        onTemporaryChanged = viewModel::setIsTemporary,
        onSave = viewModel::updatePermissions,
        onRemove = viewModel::removeMember,
        onShowRemoveDialog = viewModel::showRemoveDialog,
        onDismissRemoveDialog = viewModel::dismissRemoveDialog
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdMemberDetailScreen(
    uiState: HouseholdMemberDetailUiState,
    onNavigateBack: () -> Unit,
    onCanEditChanged: (Boolean) -> Unit,
    onPortionSizeChanged: (Float) -> Unit,
    onTemporaryChanged: (Boolean) -> Unit,
    onSave: () -> Unit,
    onRemove: () -> Unit,
    onShowRemoveDialog: () -> Unit,
    onDismissRemoveDialog: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.member?.name ?: "Member Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            val member = uiState.member ?: return@Column

            // Role badge
            Text(
                text = member.role.value.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Permissions card
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Permissions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Can edit shared plan", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = uiState.canEditSharedPlan,
                            onCheckedChange = onCanEditChanged,
                            enabled = uiState.isOwner
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Temporary guest", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = uiState.isTemporary,
                            onCheckedChange = onTemporaryChanged,
                            enabled = uiState.isOwner
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Portion size card
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.HOUSEHOLD_MEMBER_PORTION_SIZE_PREFIX + member.id)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Portion Size",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${String.format(Locale.US, "%.1f", uiState.portionSize)}x",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Slider(
                        value = uiState.portionSize,
                        onValueChange = onPortionSizeChanged,
                        valueRange = 0.5f..3.0f,
                        steps = 4,
                        enabled = uiState.isOwner
                    )
                    Text(
                        text = "Adjusts ingredient quantities for this member",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isOwner) {
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Changes")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onShowRemoveDialog,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Remove Member")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Remove confirmation dialog
    if (uiState.showRemoveDialog) {
        AlertDialog(
            onDismissRequest = onDismissRemoveDialog,
            title = { Text("Remove Member") },
            text = { Text("Are you sure you want to remove ${uiState.member?.name} from the household?") },
            confirmButton = {
                TextButton(onClick = onRemove) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRemoveDialog) {
                    Text("Cancel")
                }
            }
        )
    }
}
