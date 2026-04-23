package com.rasoiai.app.presentation.settings.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rasoiai.app.presentation.theme.spacing
import com.rasoiai.domain.model.DietaryRestriction
import com.rasoiai.domain.model.PrimaryDiet
import com.rasoiai.domain.model.UserPreferences
import com.rasoiai.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DietaryRestrictionsUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val primaryDiet: PrimaryDiet = PrimaryDiet.VEGETARIAN,
    val dietaryRestrictions: Set<DietaryRestriction> = emptySet(),
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class DietaryRestrictionsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private var currentPreferences: UserPreferences? = null

    private val _uiState = MutableStateFlow(DietaryRestrictionsUiState())
    val uiState: StateFlow<DietaryRestrictionsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.getCurrentUser().collect { user ->
                val prefs = user?.preferences
                currentPreferences = prefs
                if (prefs != null && _uiState.value.isLoading) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            primaryDiet = prefs.primaryDiet,
                            dietaryRestrictions = prefs.dietaryRestrictions.toSet()
                        )
                    }
                }
            }
        }
    }

    fun updatePrimaryDiet(diet: PrimaryDiet) {
        _uiState.update { it.copy(primaryDiet = diet) }
    }

    fun toggleDietaryRestriction(restriction: DietaryRestriction) {
        _uiState.update { state ->
            val updated = state.dietaryRestrictions.toMutableSet()
            if (restriction in updated) updated.remove(restriction) else updated.add(restriction)
            state.copy(dietaryRestrictions = updated)
        }
    }

    fun save() {
        val prefs = currentPreferences ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val state = _uiState.value
            val updatedPrefs = prefs.copy(
                primaryDiet = state.primaryDiet,
                dietaryRestrictions = state.dietaryRestrictions.toList()
            )
            settingsRepository.updateUserPreferences(updatedPrefs)
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSaving = false, errorMessage = e.message) }
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietaryRestrictionsScreen(
    onNavigateBack: () -> Unit,
    viewModel: DietaryRestrictionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) onNavigateBack()
    }

    DietaryRestrictionsScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onUpdatePrimaryDiet = viewModel::updatePrimaryDiet,
        onToggleDietaryRestriction = viewModel::toggleDietaryRestriction,
        onSave = viewModel::save
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DietaryRestrictionsScreenContent(
    uiState: DietaryRestrictionsUiState,
    onNavigateBack: () -> Unit = {},
    onUpdatePrimaryDiet: (PrimaryDiet) -> Unit = {},
    onToggleDietaryRestriction: (DietaryRestriction) -> Unit = {},
    onSave: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dietary Restrictions", fontWeight = FontWeight.Bold) },
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (uiState.isLoading) {
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.md)
            ) {
                Text(
                    text = "Primary Diet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(spacing.sm))

                Column(modifier = Modifier.selectableGroup()) {
                    PrimaryDiet.entries.forEach { diet ->
                        DietOptionCard(
                            diet = diet,
                            isSelected = diet == uiState.primaryDiet,
                            onClick = { onUpdatePrimaryDiet(diet) }
                        )
                        Spacer(modifier = Modifier.height(spacing.sm))
                    }
                }

                Spacer(modifier = Modifier.height(spacing.xl))

                Text(
                    text = "Special Dietary Restrictions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(spacing.sm))

                DietaryRestriction.entries.forEach { restriction ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleDietaryRestriction(restriction) }
                            .padding(vertical = spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = restriction in uiState.dietaryRestrictions,
                            onCheckedChange = { onToggleDietaryRestriction(restriction) }
                        )
                        Spacer(modifier = Modifier.width(spacing.sm))
                        Text(
                            text = restriction.displayName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            Button(
                onClick = onSave,
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.md)
                    .height(56.dp),
                shape = RoundedCornerShape(spacing.sm)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(24.dp).width(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun DietOptionCard(
    diet: PrimaryDiet,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onClick,
                role = Role.RadioButton
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null,
        shape = RoundedCornerShape(spacing.md)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = isSelected, onClick = null)
            Spacer(modifier = Modifier.width(spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = diet.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = diet.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
