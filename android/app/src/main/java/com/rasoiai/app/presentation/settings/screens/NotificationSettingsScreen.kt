package com.rasoiai.app.presentation.settings.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rasoiai.app.presentation.theme.spacing
import com.rasoiai.domain.model.AppSettings
import com.rasoiai.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationSettingsUiState(
    val isLoading: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val mealReminders: Boolean = true,
    val shoppingReminders: Boolean = true,
    val cookingReminders: Boolean = true,
    val festivalSuggestions: Boolean = true,
    val achievementNotifications: Boolean = true
)

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.getAppSettings().collect { settings ->
                if (_uiState.value.isLoading) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            notificationsEnabled = settings.notificationsEnabled
                        )
                    }
                }
            }
        }
    }

    fun toggleMasterNotifications(enabled: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = enabled) }
        viewModelScope.launch {
            settingsRepository.updateNotifications(enabled)
        }
    }

    fun toggleMealReminders(enabled: Boolean) {
        _uiState.update { it.copy(mealReminders = enabled) }
    }

    fun toggleShoppingReminders(enabled: Boolean) {
        _uiState.update { it.copy(shoppingReminders = enabled) }
    }

    fun toggleCookingReminders(enabled: Boolean) {
        _uiState.update { it.copy(cookingReminders = enabled) }
    }

    fun toggleFestivalSuggestions(enabled: Boolean) {
        _uiState.update { it.copy(festivalSuggestions = enabled) }
    }

    fun toggleAchievementNotifications(enabled: Boolean) {
        _uiState.update { it.copy(achievementNotifications = enabled) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotificationSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    NotificationSettingsScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onToggleMasterNotifications = viewModel::toggleMasterNotifications,
        onToggleMealReminders = viewModel::toggleMealReminders,
        onToggleShoppingReminders = viewModel::toggleShoppingReminders,
        onToggleCookingReminders = viewModel::toggleCookingReminders,
        onToggleFestivalSuggestions = viewModel::toggleFestivalSuggestions,
        onToggleAchievementNotifications = viewModel::toggleAchievementNotifications
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotificationSettingsScreenContent(
    uiState: NotificationSettingsUiState,
    onNavigateBack: () -> Unit = {},
    onToggleMasterNotifications: (Boolean) -> Unit = {},
    onToggleMealReminders: (Boolean) -> Unit = {},
    onToggleShoppingReminders: (Boolean) -> Unit = {},
    onToggleCookingReminders: (Boolean) -> Unit = {},
    onToggleFestivalSuggestions: (Boolean) -> Unit = {},
    onToggleAchievementNotifications: (Boolean) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold) },
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
            // Master toggle
            NotificationToggleRow(
                title = "All Notifications",
                subtitle = "Enable or disable all notifications",
                isChecked = uiState.notificationsEnabled,
                onToggle = onToggleMasterNotifications
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = spacing.md))

            Text(
                text = "NOTIFICATION TYPES",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(spacing.sm))

            NotificationToggleRow(
                title = "Meal Plan Reminders",
                subtitle = "Daily reminder to check your meal plan",
                isChecked = uiState.mealReminders && uiState.notificationsEnabled,
                enabled = uiState.notificationsEnabled,
                onToggle = onToggleMealReminders
            )

            NotificationToggleRow(
                title = "Shopping List Reminders",
                subtitle = "Reminder to pick up groceries",
                isChecked = uiState.shoppingReminders && uiState.notificationsEnabled,
                enabled = uiState.notificationsEnabled,
                onToggle = onToggleShoppingReminders
            )

            NotificationToggleRow(
                title = "Cooking Time Reminders",
                subtitle = "Time to start cooking notification",
                isChecked = uiState.cookingReminders && uiState.notificationsEnabled,
                enabled = uiState.notificationsEnabled,
                onToggle = onToggleCookingReminders
            )

            NotificationToggleRow(
                title = "Festival Food Suggestions",
                subtitle = "Special dish suggestions for festivals",
                isChecked = uiState.festivalSuggestions && uiState.notificationsEnabled,
                enabled = uiState.notificationsEnabled,
                onToggle = onToggleFestivalSuggestions
            )

            NotificationToggleRow(
                title = "Achievements",
                subtitle = "Cooking streak and badge notifications",
                isChecked = uiState.achievementNotifications && uiState.notificationsEnabled,
                enabled = uiState.notificationsEnabled,
                onToggle = onToggleAchievementNotifications
            )
        }
    }
}

@Composable
private fun NotificationToggleRow(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    enabled: Boolean = true,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onToggle(!isChecked) }
            .padding(vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (enabled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onToggle,
            enabled = enabled
        )
    }
}
