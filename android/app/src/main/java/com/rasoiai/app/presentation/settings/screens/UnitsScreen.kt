package com.rasoiai.app.presentation.settings.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import com.rasoiai.domain.model.AppSettings
import com.rasoiai.domain.model.SmallMeasurementUnit
import com.rasoiai.domain.model.VolumeUnit
import com.rasoiai.domain.model.WeightUnit
import com.rasoiai.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UnitsUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val volumeUnit: VolumeUnit = VolumeUnit.INDIAN,
    val weightUnit: WeightUnit = WeightUnit.METRIC,
    val smallMeasurementUnit: SmallMeasurementUnit = SmallMeasurementUnit.INDIAN,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class UnitsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private var currentSettings: AppSettings? = null

    private val _uiState = MutableStateFlow(UnitsUiState())
    val uiState: StateFlow<UnitsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.getAppSettings().collect { settings ->
                currentSettings = settings
                if (_uiState.value.isLoading) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            volumeUnit = settings.volumeUnit,
                            weightUnit = settings.weightUnit,
                            smallMeasurementUnit = settings.smallMeasurementUnit
                        )
                    }
                }
            }
        }
    }

    fun updateVolumeUnit(unit: VolumeUnit) {
        _uiState.update { it.copy(volumeUnit = unit) }
    }

    fun updateWeightUnit(unit: WeightUnit) {
        _uiState.update { it.copy(weightUnit = unit) }
    }

    fun updateSmallMeasurementUnit(unit: SmallMeasurementUnit) {
        _uiState.update { it.copy(smallMeasurementUnit = unit) }
    }

    fun save() {
        val settings = currentSettings ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val state = _uiState.value
            val updatedSettings = settings.copy(
                volumeUnit = state.volumeUnit,
                weightUnit = state.weightUnit,
                smallMeasurementUnit = state.smallMeasurementUnit
            )
            settingsRepository.updateAppSettings(updatedSettings)
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
fun UnitsScreen(
    onNavigateBack: () -> Unit,
    viewModel: UnitsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) onNavigateBack()
    }

    UnitsScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onUpdateVolumeUnit = viewModel::updateVolumeUnit,
        onUpdateWeightUnit = viewModel::updateWeightUnit,
        onUpdateSmallMeasurementUnit = viewModel::updateSmallMeasurementUnit,
        onSave = viewModel::save
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UnitsScreenContent(
    uiState: UnitsUiState,
    onNavigateBack: () -> Unit = {},
    onUpdateVolumeUnit: (VolumeUnit) -> Unit = {},
    onUpdateWeightUnit: (WeightUnit) -> Unit = {},
    onUpdateSmallMeasurementUnit: (SmallMeasurementUnit) -> Unit = {},
    onSave: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Units & Measurements", fontWeight = FontWeight.Bold) },
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
        if (uiState.isLoading) return@Scaffold

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
                // Volume Units
                Text(
                    text = "VOLUME",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(spacing.sm))
                VolumeUnit.entries.forEach { unit ->
                    UnitRadioRow(
                        label = unit.displayName,
                        isSelected = unit == uiState.volumeUnit,
                        onClick = { onUpdateVolumeUnit(unit) }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = spacing.md))

                // Weight Units
                Text(
                    text = "WEIGHT",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(spacing.sm))
                WeightUnit.entries.forEach { unit ->
                    UnitRadioRow(
                        label = unit.displayName,
                        isSelected = unit == uiState.weightUnit,
                        onClick = { onUpdateWeightUnit(unit) }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = spacing.md))

                // Small Measurement Units
                Text(
                    text = "SMALL MEASUREMENTS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(spacing.sm))
                SmallMeasurementUnit.entries.forEach { unit ->
                    UnitRadioRow(
                        label = unit.displayName,
                        isSelected = unit == uiState.smallMeasurementUnit,
                        onClick = { onUpdateSmallMeasurementUnit(unit) }
                    )
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
private fun UnitRadioRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = null)
        Spacer(modifier = Modifier.width(spacing.md))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
