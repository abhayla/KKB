package com.rasoiai.app.presentation.settings

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.rasoiai.app.presentation.common.TestTags
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rasoiai.app.presentation.settings.components.DarkModeDialog
import com.rasoiai.app.presentation.settings.components.FamilySection
import com.rasoiai.app.presentation.settings.components.ItemsPerMealDialog
import com.rasoiai.app.presentation.settings.components.ProfileSection
import com.rasoiai.app.presentation.settings.components.SettingsItem
import com.rasoiai.app.presentation.settings.components.SettingsSection
import com.rasoiai.app.presentation.settings.components.SettingsSectionWithToggles
import com.rasoiai.app.presentation.settings.components.SettingsToggleItem
import com.rasoiai.app.presentation.settings.components.SignOutButton
import com.rasoiai.app.presentation.settings.components.SignOutConfirmDialog
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.app.presentation.theme.spacing
import com.rasoiai.domain.model.AppSettings
import com.rasoiai.domain.model.DarkModePreference

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAuth: () -> Unit,
    onNavigateToRecipeRules: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                SettingsNavigationEvent.NavigateBack -> onNavigateBack()
                SettingsNavigationEvent.NavigateToAuth -> onNavigateToAuth()
                SettingsNavigationEvent.NavigateToRecipeRules -> onNavigateToRecipeRules()
                // Handle other navigation events - for now show snackbar as placeholder
                is SettingsNavigationEvent.NavigateToEditProfile,
                is SettingsNavigationEvent.NavigateToEditFamilyMember,
                SettingsNavigationEvent.NavigateToAddFamilyMember,
                SettingsNavigationEvent.NavigateToDietaryRestrictions,
                SettingsNavigationEvent.NavigateToDislikedIngredients,
                SettingsNavigationEvent.NavigateToCuisinePreferences,
                SettingsNavigationEvent.NavigateToCookingTime,
                SettingsNavigationEvent.NavigateToSpiceLevel,
                SettingsNavigationEvent.NavigateToNotifications,
                SettingsNavigationEvent.NavigateToUnits,
                SettingsNavigationEvent.NavigateToFriendsLeaderboard,
                SettingsNavigationEvent.NavigateToConnectedAccounts,
                SettingsNavigationEvent.NavigateToShareApp,
                SettingsNavigationEvent.NavigateToHelpFaq,
                SettingsNavigationEvent.NavigateToContactUs,
                SettingsNavigationEvent.NavigateToRateApp,
                SettingsNavigationEvent.NavigateToPrivacyPolicy,
                SettingsNavigationEvent.NavigateToTermsOfService -> {
                    snackbarHostState.showSnackbar("Coming soon!")
                }
            }
        }
    }

    // Show error in snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    SettingsScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBackClick = viewModel::navigateBack,
        onEditProfileClick = viewModel::onEditProfileClick,
        onEditMemberClick = viewModel::onEditFamilyMemberClick,
        onAddMemberClick = viewModel::onAddFamilyMemberClick,
        onDietaryRestrictionsClick = viewModel::onDietaryRestrictionsClick,
        onDislikedIngredientsClick = viewModel::onDislikedIngredientsClick,
        onCuisinePreferencesClick = viewModel::onCuisinePreferencesClick,
        onCookingTimeClick = viewModel::onCookingTimeClick,
        onSpiceLevelClick = viewModel::onSpiceLevelClick,
        onRecipeRulesClick = viewModel::onRecipeRulesClick,
        // Meal generation settings
        onItemsPerMealClick = viewModel::onItemsPerMealClick,
        onStrictAllergenModeToggle = viewModel::onStrictAllergenModeToggle,
        onStrictDietaryModeToggle = viewModel::onStrictDietaryModeToggle,
        onAllowRecipeRepeatToggle = viewModel::onAllowRecipeRepeatToggle,
        // App settings
        onNotificationsClick = viewModel::onNotificationsClick,
        onDarkModeClick = viewModel::showDarkModeDialog,
        onUnitsClick = viewModel::onUnitsClick,
        onFriendsLeaderboardClick = viewModel::onFriendsLeaderboardClick,
        onConnectedAccountsClick = viewModel::onConnectedAccountsClick,
        onShareAppClick = viewModel::onShareAppClick,
        onHelpFaqClick = viewModel::onHelpFaqClick,
        onContactUsClick = viewModel::onContactUsClick,
        onRateAppClick = viewModel::onRateAppClick,
        onPrivacyPolicyClick = viewModel::onPrivacyPolicyClick,
        onTermsOfServiceClick = viewModel::onTermsOfServiceClick,
        onSignOutClick = viewModel::showSignOutDialog
    )

    // Dialogs
    if (uiState.showSignOutDialog) {
        SignOutConfirmDialog(
            onConfirm = viewModel::onSignOutConfirmed,
            onDismiss = viewModel::dismissSignOutDialog
        )
    }

    if (uiState.showDarkModeDialog) {
        DarkModeDialog(
            currentPreference = uiState.appSettings.darkMode,
            onPreferenceSelected = viewModel::onDarkModeSelected,
            onDismiss = viewModel::dismissDarkModeDialog
        )
    }

    if (uiState.showItemsPerMealDialog) {
        ItemsPerMealDialog(
            currentValue = uiState.itemsPerMeal,
            onSelected = viewModel::onItemsPerMealSelected,
            onDismiss = viewModel::dismissItemsPerMealDialog
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreenContent(
    uiState: SettingsUiState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onEditMemberClick: (String) -> Unit,
    onAddMemberClick: () -> Unit,
    onDietaryRestrictionsClick: () -> Unit,
    onDislikedIngredientsClick: () -> Unit,
    onCuisinePreferencesClick: () -> Unit,
    onCookingTimeClick: () -> Unit,
    onSpiceLevelClick: () -> Unit,
    onRecipeRulesClick: () -> Unit,
    // Meal generation settings
    onItemsPerMealClick: () -> Unit,
    onStrictAllergenModeToggle: (Boolean) -> Unit,
    onStrictDietaryModeToggle: (Boolean) -> Unit,
    onAllowRecipeRepeatToggle: (Boolean) -> Unit,
    // App settings
    onNotificationsClick: () -> Unit,
    onDarkModeClick: () -> Unit,
    onUnitsClick: () -> Unit,
    onFriendsLeaderboardClick: () -> Unit,
    onConnectedAccountsClick: () -> Unit,
    onShareAppClick: () -> Unit,
    onHelpFaqClick: () -> Unit,
    onContactUsClick: () -> Unit,
    onRateAppClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onTermsOfServiceClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    Scaffold(
        modifier = Modifier.testTag(TestTags.SETTINGS_SCREEN),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = spacing.md)
                ) {
                    // Profile Section
                    item {
                        ProfileSection(
                            userName = uiState.userName,
                            userEmail = uiState.userEmail,
                            profileImageUrl = uiState.user?.profileImageUrl,
                            onEditProfileClick = onEditProfileClick,
                            modifier = Modifier.padding(horizontal = spacing.md)
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(spacing.lg))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = spacing.md))
                        Spacer(modifier = Modifier.height(spacing.md))
                    }

                    // Family Section
                    item {
                        FamilySection(
                            familyMembers = uiState.familyMembers,
                            currentUserId = uiState.familyMembers.firstOrNull()?.id ?: "",
                            onEditMemberClick = onEditMemberClick,
                            onAddMemberClick = onAddMemberClick,
                            modifier = Modifier.padding(horizontal = spacing.md)
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(spacing.lg))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = spacing.md))
                        Spacer(modifier = Modifier.height(spacing.md))
                    }

                    // Meal Preferences Section
                    item {
                        SettingsSection(
                            title = "MEAL PREFERENCES",
                            items = listOf(
                                SettingsItem(
                                    title = "Dietary Restrictions",
                                    onClick = onDietaryRestrictionsClick
                                ),
                                SettingsItem(
                                    title = "Disliked Ingredients",
                                    onClick = onDislikedIngredientsClick
                                ),
                                SettingsItem(
                                    title = "Cuisine Preferences",
                                    onClick = onCuisinePreferencesClick
                                ),
                                SettingsItem(
                                    title = "Cooking Time",
                                    onClick = onCookingTimeClick
                                ),
                                SettingsItem(
                                    title = "Spice Level",
                                    onClick = onSpiceLevelClick
                                ),
                                SettingsItem(
                                    title = "Recipe Rules",
                                    onClick = onRecipeRulesClick
                                )
                            ),
                            modifier = Modifier.padding(horizontal = spacing.md)
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(spacing.lg))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = spacing.md))
                        Spacer(modifier = Modifier.height(spacing.md))
                    }

                    // Meal Generation Section
                    item {
                        SettingsSectionWithToggles(
                            title = "MEAL GENERATION",
                            items = listOf(
                                SettingsItem(
                                    title = "Items per Meal",
                                    value = uiState.itemsPerMealDisplay,
                                    testTag = TestTags.SETTINGS_ITEMS_PER_MEAL,
                                    onClick = onItemsPerMealClick
                                )
                            ),
                            toggleItems = listOf(
                                SettingsToggleItem(
                                    title = "Strict Allergen Mode",
                                    subtitle = "Always exclude allergens from meals",
                                    isChecked = uiState.strictAllergenMode,
                                    testTag = TestTags.SETTINGS_STRICT_ALLERGEN_TOGGLE,
                                    onToggle = onStrictAllergenModeToggle
                                ),
                                SettingsToggleItem(
                                    title = "Strict Dietary Mode",
                                    subtitle = "Strictly enforce dietary restrictions",
                                    isChecked = uiState.strictDietaryMode,
                                    testTag = TestTags.SETTINGS_STRICT_DIETARY_TOGGLE,
                                    onToggle = onStrictDietaryModeToggle
                                ),
                                SettingsToggleItem(
                                    title = "Allow Recipe Repeat",
                                    subtitle = "Allow same recipe multiple times per week",
                                    isChecked = uiState.allowRecipeRepeat,
                                    testTag = TestTags.SETTINGS_ALLOW_REPEAT_TOGGLE,
                                    onToggle = onAllowRecipeRepeatToggle
                                )
                            ),
                            sectionTestTag = TestTags.SETTINGS_MEAL_GENERATION_SECTION,
                            modifier = Modifier.padding(horizontal = spacing.md)
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(spacing.lg))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = spacing.md))
                        Spacer(modifier = Modifier.height(spacing.md))
                    }

                    // App Settings Section
                    item {
                        SettingsSection(
                            title = "APP SETTINGS",
                            items = listOf(
                                SettingsItem(
                                    title = "Notifications",
                                    onClick = onNotificationsClick
                                ),
                                SettingsItem(
                                    title = "Dark Mode",
                                    value = uiState.appSettings.darkMode.displayName,
                                    onClick = onDarkModeClick
                                ),
                                SettingsItem(
                                    title = "Units & Measurements",
                                    onClick = onUnitsClick
                                )
                            ),
                            modifier = Modifier.padding(horizontal = spacing.md)
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(spacing.lg))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = spacing.md))
                        Spacer(modifier = Modifier.height(spacing.md))
                    }

                    // Social Section
                    item {
                        SettingsSection(
                            title = "SOCIAL",
                            items = listOf(
                                SettingsItem(
                                    title = "Friends & Leaderboard",
                                    onClick = onFriendsLeaderboardClick
                                ),
                                SettingsItem(
                                    title = "Connected Accounts",
                                    onClick = onConnectedAccountsClick
                                ),
                                SettingsItem(
                                    title = "Share App with Friends",
                                    onClick = onShareAppClick
                                )
                            ),
                            modifier = Modifier.padding(horizontal = spacing.md)
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(spacing.lg))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = spacing.md))
                        Spacer(modifier = Modifier.height(spacing.md))
                    }

                    // Support Section
                    item {
                        SettingsSection(
                            title = "SUPPORT",
                            items = listOf(
                                SettingsItem(
                                    title = "Help & FAQ",
                                    onClick = onHelpFaqClick
                                ),
                                SettingsItem(
                                    title = "Contact Us",
                                    onClick = onContactUsClick
                                ),
                                SettingsItem(
                                    title = "Rate App on Play Store",
                                    onClick = onRateAppClick
                                ),
                                SettingsItem(
                                    title = "Privacy Policy",
                                    onClick = onPrivacyPolicyClick
                                ),
                                SettingsItem(
                                    title = "Terms of Service",
                                    onClick = onTermsOfServiceClick
                                )
                            ),
                            modifier = Modifier.padding(horizontal = spacing.md)
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(spacing.lg))
                    }

                    // Sign Out Button
                    item {
                        SignOutButton(
                            onClick = onSignOutClick,
                            isLoading = uiState.isSigningOut,
                            modifier = Modifier.padding(horizontal = spacing.md)
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(spacing.lg))
                    }

                    // App Version
                    item {
                        Text(
                            text = "App Version ${uiState.appVersion}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = spacing.md)
                        )
                    }

                    // Bottom spacing
                    item {
                        Spacer(modifier = Modifier.height(spacing.xl))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFDFAF4)
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    backgroundColor = 0xFF1C1B1F
)
@Composable
private fun SettingsScreenPreview() {
    RasoiAITheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            SettingsScreenContent(
                uiState = SettingsUiState(
                    isLoading = false,
                    appSettings = AppSettings(darkMode = DarkModePreference.SYSTEM)
                ),
                snackbarHostState = SnackbarHostState(),
                onBackClick = {},
                onEditProfileClick = {},
                onEditMemberClick = {},
                onAddMemberClick = {},
                onDietaryRestrictionsClick = {},
                onDislikedIngredientsClick = {},
                onCuisinePreferencesClick = {},
                onCookingTimeClick = {},
                onSpiceLevelClick = {},
                onRecipeRulesClick = {},
                onItemsPerMealClick = {},
                onStrictAllergenModeToggle = {},
                onStrictDietaryModeToggle = {},
                onAllowRecipeRepeatToggle = {},
                onNotificationsClick = {},
                onDarkModeClick = {},
                onUnitsClick = {},
                onFriendsLeaderboardClick = {},
                onConnectedAccountsClick = {},
                onShareAppClick = {},
                onHelpFaqClick = {},
                onContactUsClick = {},
                onRateAppClick = {},
                onPrivacyPolicyClick = {},
                onTermsOfServiceClick = {},
                onSignOutClick = {}
            )
        }
    }
}
