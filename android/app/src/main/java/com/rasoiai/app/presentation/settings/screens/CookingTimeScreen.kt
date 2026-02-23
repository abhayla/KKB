package com.rasoiai.app.presentation.settings.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rasoiai.app.presentation.theme.spacing
import com.rasoiai.domain.model.DayOfWeek
import com.rasoiai.domain.model.UserPreferences
import com.rasoiai.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CookingTimeUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val weekdayCookingTime: Int = 30,
    val weekendCookingTime: Int = 45,
    val busyDays: Set<DayOfWeek> = emptySet(),
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class CookingTimeViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private var currentPreferences: UserPreferences? = null

    private val _uiState = MutableStateFlow(CookingTimeUiState())
    val uiState: StateFlow<CookingTimeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.getCurrentUser().collect { user ->
                val prefs = user?.preferences
                currentPreferences = prefs
                if (prefs != null && _uiState.value.isLoading) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            weekdayCookingTime = prefs.weekdayCookingTimeMinutes,
                            weekendCookingTime = prefs.weekendCookingTimeMinutes,
                            busyDays = prefs.busyDays.toSet()
                        )
                    }
                }
            }
        }
    }

    fun updateWeekdayCookingTime(minutes: Int) {
        _uiState.update { it.copy(weekdayCookingTime = minutes) }
    }

    fun updateWeekendCookingTime(minutes: Int) {
        _uiState.update { it.copy(weekendCookingTime = minutes) }
    }

    fun toggleBusyDay(day: DayOfWeek) {
        _uiState.update { state ->
            val updated = state.busyDays.toMutableSet()
            if (day in updated) updated.remove(day) else updated.add(day)
            state.copy(busyDays = updated)
        }
    }

    fun save() {
        val prefs = currentPreferences ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val state = _uiState.value
            val updatedPrefs = prefs.copy(
                weekdayCookingTimeMinutes = state.weekdayCookingTime,
                weekendCookingTimeMinutes = state.weekendCookingTime,
                busyDays = state.busyDays.toList()
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CookingTimeScreen(
    onNavigateBack: () -> Unit,
    viewModel: CookingTimeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val cookingTimeOptions = listOf(15, 30, 45, 60, 90)

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) onNavigateBack()
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cooking Time", fontWeight = FontWeight.Bold) },
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
                // Weekday cooking time
                Text(
                    text = "Weekdays:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(spacing.xs))
                var weekdayExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = weekdayExpanded,
                    onExpandedChange = { weekdayExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = "${uiState.weekdayCookingTime} minutes",
                        onValueChange = { },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = weekdayExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(spacing.sm)
                    )
                    ExposedDropdownMenu(
                        expanded = weekdayExpanded,
                        onDismissRequest = { weekdayExpanded = false }
                    ) {
                        cookingTimeOptions.forEach { time ->
                            DropdownMenuItem(
                                text = { Text("$time minutes") },
                                onClick = {
                                    viewModel.updateWeekdayCookingTime(time)
                                    weekdayExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(spacing.lg))

                // Weekend cooking time
                Text(
                    text = "Weekends:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(spacing.xs))
                var weekendExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = weekendExpanded,
                    onExpandedChange = { weekendExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = "${uiState.weekendCookingTime} minutes",
                        onValueChange = { },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = weekendExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(spacing.sm)
                    )
                    ExposedDropdownMenu(
                        expanded = weekendExpanded,
                        onDismissRequest = { weekendExpanded = false }
                    ) {
                        cookingTimeOptions.forEach { time ->
                            DropdownMenuItem(
                                text = { Text("$time minutes") },
                                onClick = {
                                    viewModel.updateWeekendCookingTime(time)
                                    weekendExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(spacing.xl))

                // Busy days
                Text(
                    text = "Busy days (quick meals only):",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(spacing.sm))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    DayOfWeek.entries.forEach { day ->
                        val isSelected = day in uiState.busyDays
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.toggleBusyDay(day) },
                            label = { Text(day.shortName) },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else null
                        )
                    }
                }
            }

            Button(
                onClick = viewModel::save,
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
