package com.rasoiai.app.presentation.settings.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rasoiai.app.presentation.theme.spacing
import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.UserPreferences
import com.rasoiai.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CuisinePreferencesUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val selectedCuisines: Set<CuisineType> = emptySet(),
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class CuisinePreferencesViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private var currentPreferences: UserPreferences? = null

    private val _uiState = MutableStateFlow(CuisinePreferencesUiState())
    val uiState: StateFlow<CuisinePreferencesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.getCurrentUser().collect { user ->
                val prefs = user?.preferences
                currentPreferences = prefs
                if (prefs != null && _uiState.value.isLoading) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            selectedCuisines = prefs.cuisinePreferences.toSet()
                        )
                    }
                }
            }
        }
    }

    fun toggleCuisine(cuisine: CuisineType) {
        _uiState.update { state ->
            val updated = state.selectedCuisines.toMutableSet()
            if (cuisine in updated) updated.remove(cuisine) else updated.add(cuisine)
            state.copy(selectedCuisines = updated)
        }
    }

    fun save() {
        val prefs = currentPreferences ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val updatedPrefs = prefs.copy(
                cuisinePreferences = _uiState.value.selectedCuisines.toList()
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
fun CuisinePreferencesScreen(
    onNavigateBack: () -> Unit,
    viewModel: CuisinePreferencesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) onNavigateBack()
    }

    CuisinePreferencesScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onToggleCuisine = viewModel::toggleCuisine,
        onSave = viewModel::save
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CuisinePreferencesScreenContent(
    uiState: CuisinePreferencesUiState,
    onNavigateBack: () -> Unit = {},
    onToggleCuisine: (CuisineType) -> Unit = {},
    onSave: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cuisine Preferences", fontWeight = FontWeight.Bold) },
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
                Text(
                    text = "Select all that apply",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(spacing.lg))

                // Cuisine grid (2x2)
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        CuisineCard(
                            cuisine = CuisineType.NORTH,
                            isSelected = CuisineType.NORTH in uiState.selectedCuisines,
                            onClick = { onToggleCuisine(CuisineType.NORTH) },
                            modifier = Modifier.weight(1f),
                            description = "Punjabi, Mughlai"
                        )
                        CuisineCard(
                            cuisine = CuisineType.SOUTH,
                            isSelected = CuisineType.SOUTH in uiState.selectedCuisines,
                            onClick = { onToggleCuisine(CuisineType.SOUTH) },
                            modifier = Modifier.weight(1f),
                            description = "Tamil, Kerala"
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        CuisineCard(
                            cuisine = CuisineType.EAST,
                            isSelected = CuisineType.EAST in uiState.selectedCuisines,
                            onClick = { onToggleCuisine(CuisineType.EAST) },
                            modifier = Modifier.weight(1f),
                            description = "Bengali, Odia"
                        )
                        CuisineCard(
                            cuisine = CuisineType.WEST,
                            isSelected = CuisineType.WEST in uiState.selectedCuisines,
                            onClick = { onToggleCuisine(CuisineType.WEST) },
                            modifier = Modifier.weight(1f),
                            description = "Gujarati, Maharashtrian"
                        )
                    }
                }
            }

            Button(
                onClick = onSave,
                enabled = !uiState.isSaving && uiState.selectedCuisines.isNotEmpty(),
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
private fun CuisineCard(
    cuisine: CuisineType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null,
        shape = RoundedCornerShape(spacing.md)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (cuisine) {
                    CuisineType.NORTH -> "\uD83E\uDD58"
                    CuisineType.SOUTH -> "\uD83C\uDF5A"
                    CuisineType.EAST -> "\uD83C\uDF5B"
                    CuisineType.WEST -> "\uD83E\uDD57"
                },
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(spacing.xs))
            Text(
                text = cuisine.displayName.uppercase(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (isSelected) {
                Spacer(modifier = Modifier.height(spacing.xs))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
