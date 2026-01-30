package com.rasoiai.domain.repository

import com.rasoiai.domain.model.AppSettings
import com.rasoiai.domain.model.DarkModePreference
import com.rasoiai.domain.model.FamilyMember
import com.rasoiai.domain.model.User
import com.rasoiai.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for user settings and profile management.
 */
interface SettingsRepository {

    /**
     * Get the current user profile.
     */
    fun getCurrentUser(): Flow<User?>

    /**
     * Get app settings.
     */
    fun getAppSettings(): Flow<AppSettings>

    /**
     * Update dark mode preference.
     */
    suspend fun updateDarkMode(preference: DarkModePreference): Result<Unit>

    /**
     * Update notifications setting.
     */
    suspend fun updateNotifications(enabled: Boolean): Result<Unit>

    /**
     * Update user preferences.
     */
    suspend fun updateUserPreferences(preferences: UserPreferences): Result<Unit>

    /**
     * Add a family member.
     */
    suspend fun addFamilyMember(member: FamilyMember): Result<Unit>

    /**
     * Update a family member.
     */
    suspend fun updateFamilyMember(member: FamilyMember): Result<Unit>

    /**
     * Remove a family member.
     */
    suspend fun removeFamilyMember(memberId: String): Result<Unit>

    /**
     * Update app settings.
     */
    suspend fun updateAppSettings(settings: AppSettings): Result<Unit>

    /**
     * Sign out the current user.
     */
    suspend fun signOut(): Result<Unit>

    /**
     * Get app version string.
     */
    fun getAppVersion(): String

    /**
     * Update meal generation settings.
     */
    suspend fun updateMealGenerationSettings(
        itemsPerMeal: Int? = null,
        strictAllergenMode: Boolean? = null,
        strictDietaryMode: Boolean? = null,
        allowRecipeRepeat: Boolean? = null
    ): Result<Unit>
}
