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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rasoiai.app.presentation.onboarding.CommonDislikedIngredients
import com.rasoiai.app.presentation.theme.spacing
import com.rasoiai.domain.model.UserPreferences
import com.rasoiai.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DislikedIngredientsUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val dislikedIngredients: Set<String> = emptySet(),
    val searchQuery: String = "",
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class DislikedIngredientsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private var currentPreferences: UserPreferences? = null

    private val _uiState = MutableStateFlow(DislikedIngredientsUiState())
    val uiState: StateFlow<DislikedIngredientsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.getCurrentUser().collect { user ->
                val prefs = user?.preferences
                currentPreferences = prefs
                if (prefs != null && _uiState.value.isLoading) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            dislikedIngredients = prefs.dislikedIngredients.toSet()
                        )
                    }
                }
            }
        }
    }

    fun toggleIngredient(ingredient: String) {
        _uiState.update { state ->
            val updated = state.dislikedIngredients.toMutableSet()
            if (ingredient in updated) updated.remove(ingredient) else updated.add(ingredient)
            state.copy(dislikedIngredients = updated)
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun addCustomIngredient(ingredient: String) {
        if (ingredient.isBlank()) return
        _uiState.update { state ->
            val updated = state.dislikedIngredients.toMutableSet()
            updated.add(ingredient.trim())
            state.copy(dislikedIngredients = updated, searchQuery = "")
        }
    }

    fun save() {
        val prefs = currentPreferences ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val updatedPrefs = prefs.copy(
                dislikedIngredients = _uiState.value.dislikedIngredients.toList()
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
fun DislikedIngredientsScreen(
    onNavigateBack: () -> Unit,
    viewModel: DislikedIngredientsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) onNavigateBack()
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Disliked Ingredients", fontWeight = FontWeight.Bold) },
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
                // Search field
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    placeholder = { Text("Search ingredients...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(spacing.sm),
                    singleLine = true,
                    trailingIcon = {
                        if (uiState.searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.addCustomIngredient(uiState.searchQuery) }) {
                                Icon(Icons.Default.Add, contentDescription = "Add")
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(spacing.md))

                Text(
                    text = "Common dislikes:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(spacing.sm))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    CommonDislikedIngredients.ingredients.forEach { (name, englishName) ->
                        val isSelected = name in uiState.dislikedIngredients
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.toggleIngredient(name) },
                            label = {
                                Column {
                                    Text(name)
                                    Text(
                                        text = "($englishName)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }
                }

                // Custom ingredients
                val customIngredients = uiState.dislikedIngredients.filter { ingredient ->
                    CommonDislikedIngredients.ingredients.none { it.first == ingredient }
                }
                if (customIngredients.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(spacing.lg))
                    Text(
                        text = "Custom:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(spacing.sm))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(spacing.sm)
                    ) {
                        customIngredients.forEach { ingredient ->
                            FilterChip(
                                selected = true,
                                onClick = { viewModel.toggleIngredient(ingredient) },
                                label = { Text(ingredient) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        }
                    }
                }

                if (uiState.dislikedIngredients.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(spacing.lg))
                    Text(
                        text = "Selected: ${uiState.dislikedIngredients.joinToString(", ")}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
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
