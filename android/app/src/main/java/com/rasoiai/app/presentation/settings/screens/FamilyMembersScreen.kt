package com.rasoiai.app.presentation.settings.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rasoiai.app.presentation.theme.spacing
import com.rasoiai.domain.model.FamilyMember
import com.rasoiai.domain.model.MemberType
import com.rasoiai.domain.model.SpecialDietaryNeed
import com.rasoiai.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FamilyMembersUiState(
    val isLoading: Boolean = true,
    val familyMembers: List<FamilyMember> = emptyList(),
    val showAddEditSheet: Boolean = false,
    val editingMember: FamilyMember? = null,
    val showDeleteDialog: Boolean = false,
    val deletingMemberId: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class FamilyMembersViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FamilyMembersUiState())
    val uiState: StateFlow<FamilyMembersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.getCurrentUser().collect { user ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        familyMembers = user?.preferences?.familyMembers ?: emptyList()
                    )
                }
            }
        }
    }

    fun showAddSheet() {
        _uiState.update { it.copy(showAddEditSheet = true, editingMember = null) }
    }

    fun showEditSheet(member: FamilyMember) {
        _uiState.update { it.copy(showAddEditSheet = true, editingMember = member) }
    }

    fun dismissSheet() {
        _uiState.update { it.copy(showAddEditSheet = false, editingMember = null) }
    }

    fun showDeleteDialog(memberId: String) {
        _uiState.update { it.copy(showDeleteDialog = true, deletingMemberId = memberId) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false, deletingMemberId = null) }
    }

    fun saveMember(name: String, type: MemberType, age: Int?, specialNeeds: List<SpecialDietaryNeed>) {
        viewModelScope.launch {
            val editing = _uiState.value.editingMember
            if (editing != null) {
                val updated = editing.copy(name = name, type = type, age = age, specialNeeds = specialNeeds)
                settingsRepository.updateFamilyMember(updated)
            } else {
                val member = FamilyMember(id = "", name = name, type = type, age = age, specialNeeds = specialNeeds)
                settingsRepository.addFamilyMember(member)
            }
            dismissSheet()
        }
    }

    fun deleteMember() {
        val memberId = _uiState.value.deletingMemberId ?: return
        viewModelScope.launch {
            settingsRepository.removeFamilyMember(memberId)
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
            dismissDeleteDialog()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyMembersScreen(
    onNavigateBack: () -> Unit,
    viewModel: FamilyMembersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Family Members", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::showAddSheet) {
                Icon(Icons.Default.Add, contentDescription = "Add family member")
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (uiState.isLoading) return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.md)
        ) {
            if (uiState.familyMembers.isEmpty()) {
                Text(
                    text = "No family members added yet.\nTap + to add a member.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = spacing.xl)
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(spacing.md)
                ) {
                    Column(modifier = Modifier.padding(spacing.sm)) {
                        uiState.familyMembers.forEach { member ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(spacing.sm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(spacing.md))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = member.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    val details = buildString {
                                        append(member.type.displayName)
                                        member.age?.let { append(", $it yrs") }
                                    }
                                    Text(
                                        text = details,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { viewModel.showEditSheet(member) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { viewModel.showDeleteDialog(member.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add/Edit Bottom Sheet
    if (uiState.showAddEditSheet) {
        FamilyMemberSheet(
            editingMember = uiState.editingMember,
            onDismiss = viewModel::dismissSheet,
            onSave = viewModel::saveMember
        )
    }

    // Delete Confirmation Dialog
    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteDialog,
            title = { Text("Remove Family Member") },
            text = { Text("Are you sure you want to remove this family member?") },
            confirmButton = {
                TextButton(onClick = viewModel::deleteMember) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteDialog) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FamilyMemberSheet(
    editingMember: FamilyMember?,
    onDismiss: () -> Unit,
    onSave: (String, MemberType, Int?, List<SpecialDietaryNeed>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf(editingMember?.name ?: "") }
    var memberType by remember { mutableStateOf(editingMember?.type ?: MemberType.ADULT) }
    var age by remember { mutableIntStateOf(editingMember?.age ?: 30) }
    val specialNeeds = remember {
        mutableStateListOf<SpecialDietaryNeed>().apply {
            editingMember?.specialNeeds?.let { addAll(it) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(spacing.md)
        ) {
            Text(
                text = if (editingMember != null) "Edit Family Member" else "Add Family Member",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(spacing.lg))

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(spacing.sm),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(spacing.md))

            // Type dropdown
            var typeExpanded by remember { mutableStateOf(false) }
            Text("Type", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(spacing.xs))
            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = memberType.displayName,
                    onValueChange = { },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(spacing.sm)
                )
                ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                    MemberType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.displayName) },
                            onClick = { memberType = type; typeExpanded = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.md))

            // Age dropdown
            var ageExpanded by remember { mutableStateOf(false) }
            Text("Age", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(spacing.xs))
            ExposedDropdownMenuBox(
                expanded = ageExpanded,
                onExpandedChange = { ageExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = "$age years",
                    onValueChange = { },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ageExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(spacing.sm)
                )
                ExposedDropdownMenu(expanded = ageExpanded, onDismissRequest = { ageExpanded = false }) {
                    (1..100).forEach { a ->
                        DropdownMenuItem(
                            text = { Text("$a years") },
                            onClick = { age = a; ageExpanded = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.lg))

            // Special dietary needs
            Text("Special dietary needs:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(spacing.sm))

            SpecialDietaryNeed.entries.forEach { need ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (need in specialNeeds) specialNeeds.remove(need) else specialNeeds.add(need)
                        }
                        .padding(vertical = spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = need in specialNeeds,
                        onCheckedChange = { if (it) specialNeeds.add(need) else specialNeeds.remove(need) }
                    )
                    Spacer(modifier = Modifier.width(spacing.sm))
                    Text(need.displayName, style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(modifier = Modifier.height(spacing.lg))

            // Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(spacing.sm)
                ) { Text("Cancel") }
                Button(
                    onClick = { if (name.isNotBlank()) onSave(name, memberType, age, specialNeeds.toList()) },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(spacing.sm)
                ) { Text(if (editingMember != null) "Update" else "Add") }
            }

            Spacer(modifier = Modifier.height(spacing.xl))
        }
    }
}
