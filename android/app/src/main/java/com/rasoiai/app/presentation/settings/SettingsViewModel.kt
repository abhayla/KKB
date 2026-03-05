package com.rasoiai.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rasoiai.domain.model.AppSettings
import com.rasoiai.domain.model.DarkModePreference
import com.rasoiai.domain.model.FamilyMember
import com.rasoiai.domain.model.User
import com.rasoiai.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * UI state for the Settings screen.
 */
data class SettingsUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val user: User? = null,
    val appSettings: AppSettings = AppSettings(),
    val appVersion: String = "1.0.0",
    val isSigningOut: Boolean = false,
    val showSignOutDialog: Boolean = false,
    val showDarkModeDialog: Boolean = false,
    val showItemsPerMealDialog: Boolean = false
) {
    val familyMembers: List<FamilyMember>
        get() = user?.preferences?.familyMembers ?: emptyList()

    val userName: String
        get() = user?.name ?: "Guest"

    val userEmail: String
        get() = user?.email ?: ""

    val primaryDietDisplay: String
        get() = user?.preferences?.primaryDiet?.displayName ?: "Not set"

    val spiceLevelDisplay: String
        get() = user?.preferences?.spiceLevel?.displayName ?: "Not set"

    val cuisinePreferencesDisplay: String
        get() = user?.preferences?.cuisinePreferences
            ?.joinToString(", ") { it.displayName }
            ?: "Not set"

    val cookingTimeDisplay: String
        get() {
            val weekday = user?.preferences?.weekdayCookingTimeMinutes ?: 30
            val weekend = user?.preferences?.weekendCookingTimeMinutes ?: 60
            return "Weekday: ${weekday}min, Weekend: ${weekend}min"
        }

    // Meal generation settings
    val itemsPerMeal: Int
        get() = user?.preferences?.itemsPerMeal ?: 2

    val itemsPerMealDisplay: String
        get() = "${itemsPerMeal} items"

    val strictAllergenMode: Boolean
        get() = user?.preferences?.strictAllergenMode ?: true

    val strictDietaryMode: Boolean
        get() = user?.preferences?.strictDietaryMode ?: true

    val allowRecipeRepeat: Boolean
        get() = user?.preferences?.allowRecipeRepeat ?: false
}

/**
 * Navigation events from Settings screen.
 */
sealed class SettingsNavigationEvent {
    data object NavigateBack : SettingsNavigationEvent()
    data object NavigateToEditProfile : SettingsNavigationEvent()
    data class NavigateToEditFamilyMember(val memberId: String) : SettingsNavigationEvent()
    data object NavigateToAddFamilyMember : SettingsNavigationEvent()
    data object NavigateToDietaryRestrictions : SettingsNavigationEvent()
    data object NavigateToDislikedIngredients : SettingsNavigationEvent()
    data object NavigateToCuisinePreferences : SettingsNavigationEvent()
    data object NavigateToCookingTime : SettingsNavigationEvent()
    data object NavigateToSpiceLevel : SettingsNavigationEvent()
    data object NavigateToRecipeRules : SettingsNavigationEvent()
    data object NavigateToPantry : SettingsNavigationEvent()
    data object NavigateToNotifications : SettingsNavigationEvent()
    data object NavigateToUnits : SettingsNavigationEvent()
    data object NavigateToFriendsLeaderboard : SettingsNavigationEvent()
    data object NavigateToConnectedAccounts : SettingsNavigationEvent()
    data object NavigateToShareApp : SettingsNavigationEvent()
    data object NavigateToHelpFaq : SettingsNavigationEvent()
    data object NavigateToContactUs : SettingsNavigationEvent()
    data object NavigateToRateApp : SettingsNavigationEvent()
    data object NavigateToPrivacyPolicy : SettingsNavigationEvent()
    data object NavigateToTermsOfService : SettingsNavigationEvent()
    data object NavigateToAuth : SettingsNavigationEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _navigationEvent = Channel<SettingsNavigationEvent>(Channel.BUFFERED)
    val navigationEvent: Flow<SettingsNavigationEvent> = _navigationEvent.receiveAsFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Load app version
            val version = settingsRepository.getAppVersion()
            _uiState.update { it.copy(appVersion = version) }

            // Observe user
            settingsRepository.getCurrentUser().collect { user ->
                _uiState.update { it.copy(user = user, isLoading = false) }
            }
        }

        viewModelScope.launch {
            settingsRepository.getAppSettings().collect { settings ->
                _uiState.update { it.copy(appSettings = settings) }
            }
        }
    }

    // region Dark Mode

    fun showDarkModeDialog() {
        _uiState.update { it.copy(showDarkModeDialog = true) }
    }

    fun dismissDarkModeDialog() {
        _uiState.update { it.copy(showDarkModeDialog = false) }
    }

    fun onDarkModeSelected(preference: DarkModePreference) {
        viewModelScope.launch {
            settingsRepository.updateDarkMode(preference)
                .onSuccess {
                    Timber.i("Dark mode updated to: $preference")
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to update dark mode")
                    _uiState.update { it.copy(errorMessage = "Failed to update dark mode") }
                }
        }
        dismissDarkModeDialog()
    }

    // endregion

    // region Sign Out

    fun showSignOutDialog() {
        _uiState.update { it.copy(showSignOutDialog = true) }
    }

    fun dismissSignOutDialog() {
        _uiState.update { it.copy(showSignOutDialog = false) }
    }

    fun onSignOutConfirmed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSigningOut = true) }
            dismissSignOutDialog()

            settingsRepository.signOut()
                .onSuccess {
                    Timber.i("User signed out")
                    _navigationEvent.send(SettingsNavigationEvent.NavigateToAuth)
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to sign out")
                    _uiState.update {
                        it.copy(
                            isSigningOut = false,
                            errorMessage = "Failed to sign out"
                        )
                    }
                }
        }
    }

    // endregion

    // region Navigation

    fun navigateBack() {
        _navigationEvent.trySend(SettingsNavigationEvent.NavigateBack)
    }

    fun onEditProfileClick() {
        _navigationEvent.trySend(SettingsNavigationEvent.NavigateToEditProfile)
    }

    fun onEditFamilyMemberClick(memberId: String) {
        _navigationEvent.trySend(SettingsNavigationEvent.NavigateToEditFamilyMember(memberId))
    }

    fun onAddFamilyMemberClick() {
        _navigationEvent.trySend(SettingsNavigationEvent.NavigateToAddFamilyMember)
    }

    fun onDietaryRestrictionsClick() {
        _navigationEvent.trySend(SettingsNavigationEvent.NavigateToDietaryRestrictions)
    }

    fun onDislikedIngredientsClick() {
        _navigationEvent.trySend(SettingsNavigationEvent.NavigateToDislikedIngredients)
    }

    fun onCuisinePreferencesClick() {
        _navigationEvent.trySend(SettingsNavigationEvent.NavigateToCuisinePreferences)
    }

    fun onCookingTimeClick() {
        _navigationEvent.trySend(SettingsNavigationEvent.NavigateToCookingTime)
    }

    fun onSpiceLevelClick() {
        _navigationEvent.trySend(SettingsNavigationEvent.NavigateToSpiceLevel)
    }

    fun onRecipeRulesClick() {
        _navigationEvent.trySend(SettingsNavigationEvent.NavigateToRecipeRules)
    }

    fun onPantryClick() {
        _navigationEvent.trySend(SettingsNavigationEvent.NavigateToPantry)
    }

    fun onNotificationsClick() {
        _navigationEvent.trySend(SettingsNavigationEvent.NavigateToNotifications)
    }

    fun onUnitsClick() {
        _navigationEvent.trySend(SettingsNavigationEvent.NavigateToUnits)
    }

    fun onFriendsLeaderboardClick() {
        _navigationEvent.trySend(SettingsNavigationEvent.NavigateToFriendsLeaderboard)
    }

    fun onConnectedAccountsClick() {
        _navigationEvent.trySend(SettingsNavigationEvent.NavigateToConnectedAccounts)
    }

    fun onShareAppClick() {
        _navigationEvent.trySend(SettingsNavigationEvent.NavigateToShareApp)
    }

    fun onHelpFaqClick() {
        _navigationEvent.trySend(SettingsNavigationEvent.NavigateToHelpFaq)
    }

    fun onContactUsClick() {
        _navigationEvent.trySend(SettingsNavigationEvent.NavigateToContactUs)
    }

    fun onRateAppClick() {
        _navigationEvent.trySend(SettingsNavigationEvent.NavigateToRateApp)
    }

    fun onPrivacyPolicyClick() {
        _navigationEvent.trySend(SettingsNavigationEvent.NavigateToPrivacyPolicy)
    }

    fun onTermsOfServiceClick() {
        _navigationEvent.trySend(SettingsNavigationEvent.NavigateToTermsOfService)
    }

    // endregion

    // region Meal Generation Settings

    fun showItemsPerMealDialog() {
        _uiState.update { it.copy(showItemsPerMealDialog = true) }
    }

    fun dismissItemsPerMealDialog() {
        _uiState.update { it.copy(showItemsPerMealDialog = false) }
    }

    fun onItemsPerMealClick() {
        showItemsPerMealDialog()
    }

    fun onItemsPerMealSelected(count: Int) {
        viewModelScope.launch {
            settingsRepository.updateMealGenerationSettings(itemsPerMeal = count)
                .onSuccess {
                    Timber.i("Items per meal updated to: $count")
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to update items per meal")
                    _uiState.update { it.copy(errorMessage = "Failed to update setting") }
                }
        }
        dismissItemsPerMealDialog()
    }

    fun onStrictAllergenModeToggle(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateMealGenerationSettings(strictAllergenMode = enabled)
                .onSuccess {
                    Timber.i("Strict allergen mode updated: $enabled")
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to update strict allergen mode")
                    _uiState.update { it.copy(errorMessage = "Failed to update setting") }
                }
        }
    }

    fun onStrictDietaryModeToggle(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateMealGenerationSettings(strictDietaryMode = enabled)
                .onSuccess {
                    Timber.i("Strict dietary mode updated: $enabled")
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to update strict dietary mode")
                    _uiState.update { it.copy(errorMessage = "Failed to update setting") }
                }
        }
    }

    fun onAllowRecipeRepeatToggle(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateMealGenerationSettings(allowRecipeRepeat = enabled)
                .onSuccess {
                    Timber.i("Allow recipe repeat updated: $enabled")
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to update allow recipe repeat")
                    _uiState.update { it.copy(errorMessage = "Failed to update setting") }
                }
        }
    }

    // endregion

    // region Error Handling

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // endregion
}
