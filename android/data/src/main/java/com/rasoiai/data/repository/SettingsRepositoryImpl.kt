package com.rasoiai.data.repository

import android.content.Context
import com.rasoiai.core.network.NetworkMonitor
import com.rasoiai.data.local.datastore.UserPreferencesDataStoreInterface
import com.rasoiai.data.remote.api.RasoiApiService
import com.rasoiai.data.remote.dto.toCreateRequest
import com.rasoiai.data.remote.dto.toUpdateRequest
import com.rasoiai.domain.model.AppSettings
import com.rasoiai.domain.model.DarkModePreference
import com.rasoiai.domain.model.FamilyMember
import com.rasoiai.domain.model.User
import com.rasoiai.domain.model.UserPreferences
import com.rasoiai.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real implementation of SettingsRepository with offline-first architecture.
 *
 * Strategy:
 * - User preferences stored in DataStore (single source of truth)
 * - App settings stored in DataStore
 * - User profile can sync with server when online
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesDataStore: UserPreferencesDataStoreInterface,
    private val apiService: RasoiApiService,
    private val networkMonitor: NetworkMonitor
) : SettingsRepository {

    // Cached user data
    private var cachedUser: User? = null

    override fun getCurrentUser(): Flow<User?> {
        return combine(
            userPreferencesDataStore.userId,
            userPreferencesDataStore.userEmail,
            userPreferencesDataStore.userPreferences,
            userPreferencesDataStore.isOnboarded
        ) { userId, email, preferences, isOnboarded ->
            if (userId != null) {
                User(
                    id = userId,
                    email = email ?: "",
                    name = preferences?.familyMembers?.firstOrNull()?.name ?: "User",
                    profileImageUrl = null,
                    isOnboarded = isOnboarded,
                    preferences = preferences
                ).also { cachedUser = it }
            } else {
                null
            }
        }
    }

    override fun getAppSettings(): Flow<AppSettings> {
        return userPreferencesDataStore.appSettings
    }

    override suspend fun updateDarkMode(preference: DarkModePreference): Result<Unit> {
        return try {
            val current = userPreferencesDataStore.appSettings.first()
            userPreferencesDataStore.saveAppSettings(current.copy(darkMode = preference))
            Timber.d("Updated dark mode: ${preference.displayName}")
            Result.success(Unit)
        } catch (e: IOException) {
            Timber.w(e, "IO error updating dark mode")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update dark mode")
            Result.failure(e)
        }
    }

    override suspend fun updateNotifications(enabled: Boolean): Result<Unit> {
        return try {
            val current = userPreferencesDataStore.appSettings.first()
            userPreferencesDataStore.saveAppSettings(current.copy(notificationsEnabled = enabled))
            Timber.d("Updated notifications: $enabled")
            Result.success(Unit)
        } catch (e: IOException) {
            Timber.w(e, "IO error updating notifications")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update notifications")
            Result.failure(e)
        }
    }

    override suspend fun updateUserPreferences(preferences: UserPreferences): Result<Unit> {
        return try {
            // Save to DataStore (synchronous - must complete before returning)
            userPreferencesDataStore.saveOnboardingComplete(preferences)

            // Sync to backend asynchronously (non-blocking)
            // Use GlobalScope to ensure sync continues even if caller scope is cancelled
            @Suppress("OPT_IN_USAGE")
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    if (networkMonitor.isOnline.first()) {
                        val prefsMap = mapOf(
                            "household_size" to preferences.householdSize,
                            "primary_diet" to preferences.primaryDiet.value,
                            "spice_level" to preferences.spiceLevel.value,
                            "dietary_restrictions" to preferences.dietaryRestrictions.map { it.value },
                            "cuisine_preferences" to preferences.cuisinePreferences.map { it.value },
                            "disliked_ingredients" to preferences.dislikedIngredients,
                            "weekday_cooking_time" to preferences.weekdayCookingTimeMinutes,
                            "weekend_cooking_time" to preferences.weekendCookingTimeMinutes,
                            "busy_days" to preferences.busyDays.map { it.value },
                            // Meal generation settings
                            "items_per_meal" to preferences.itemsPerMeal,
                            "strict_allergen_mode" to preferences.strictAllergenMode,
                            "strict_dietary_mode" to preferences.strictDietaryMode,
                            "allow_recipe_repeat" to preferences.allowRecipeRepeat
                        )
                        apiService.updateUserPreferences(prefsMap)
                        Timber.i("Synced preferences to server")
                    }
                } catch (e: IOException) {
                    Timber.w(e, "Network error syncing preferences to server, saved locally")
                } catch (e: Exception) {
                    Timber.w(e, "Failed to sync preferences to server, saved locally")
                }
            }

            Timber.i("Updated user preferences")
            Result.success(Unit)
        } catch (e: IOException) {
            Timber.w(e, "IO error updating user preferences")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update user preferences")
            Result.failure(e)
        }
    }

    override suspend fun addFamilyMember(member: FamilyMember): Result<Unit> {
        return try {
            val memberWithId = member.copy(
                id = if (member.id.isEmpty()) UUID.randomUUID().toString() else member.id
            )

            // Save locally first
            val currentPrefs = userPreferencesDataStore.userPreferences.first()
                ?: return Result.failure(Exception("No user preferences found"))

            val updatedMembers = currentPrefs.familyMembers + memberWithId
            val updatedPrefs = currentPrefs.copy(
                familyMembers = updatedMembers,
                householdSize = updatedMembers.size
            )
            userPreferencesDataStore.saveOnboardingComplete(updatedPrefs)

            // Sync to backend
            viewModelScopeSafeSync {
                apiService.createFamilyMember(memberWithId.toCreateRequest())
            }

            Timber.i("Added family member: ${member.name}")
            Result.success(Unit)
        } catch (e: IOException) {
            Timber.w(e, "IO error adding family member")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "Failed to add family member")
            Result.failure(e)
        }
    }

    override suspend fun updateFamilyMember(member: FamilyMember): Result<Unit> {
        return try {
            val currentPrefs = userPreferencesDataStore.userPreferences.first()
                ?: return Result.failure(Exception("No user preferences found"))

            val updatedMembers = currentPrefs.familyMembers.map {
                if (it.id == member.id) member else it
            }

            val updatedPrefs = currentPrefs.copy(familyMembers = updatedMembers)
            userPreferencesDataStore.saveOnboardingComplete(updatedPrefs)

            // Sync to backend
            viewModelScopeSafeSync {
                apiService.updateFamilyMemberApi(member.id, member.toUpdateRequest())
            }

            Timber.i("Updated family member: ${member.name}")
            Result.success(Unit)
        } catch (e: IOException) {
            Timber.w(e, "IO error updating family member")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update family member")
            Result.failure(e)
        }
    }

    override suspend fun removeFamilyMember(memberId: String): Result<Unit> {
        return try {
            val currentPrefs = userPreferencesDataStore.userPreferences.first()
                ?: return Result.failure(Exception("No user preferences found"))

            val updatedMembers = currentPrefs.familyMembers.filter { it.id != memberId }

            val updatedPrefs = currentPrefs.copy(
                familyMembers = updatedMembers,
                householdSize = updatedMembers.size.coerceAtLeast(1)
            )

            userPreferencesDataStore.saveOnboardingComplete(updatedPrefs)

            // Sync to backend
            viewModelScopeSafeSync {
                apiService.deleteFamilyMember(memberId)
            }

            Timber.i("Removed family member: $memberId")
            Result.success(Unit)
        } catch (e: IOException) {
            Timber.w(e, "IO error removing family member")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "Failed to remove family member")
            Result.failure(e)
        }
    }

    /**
     * Fire-and-forget sync to backend. Catches all errors silently.
     * Uses Dispatchers.IO to avoid blocking the caller.
     */
    @Suppress("OPT_IN_USAGE")
    private fun viewModelScopeSafeSync(action: suspend () -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                if (networkMonitor.isOnline.first()) {
                    action()
                    Timber.d("Backend sync completed")
                }
            } catch (e: Exception) {
                Timber.w(e, "Backend sync failed, saved locally only")
            }
        }
    }

    override suspend fun updateAppSettings(settings: AppSettings): Result<Unit> {
        return try {
            userPreferencesDataStore.saveAppSettings(settings)
            Timber.d("Updated app settings")
            Result.success(Unit)
        } catch (e: IOException) {
            Timber.w(e, "IO error updating app settings")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update app settings")
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            // Clear all local data (clearPreferences wipes DataStore including app settings)
            userPreferencesDataStore.clearAuthTokens()
            userPreferencesDataStore.clearPreferences()
            cachedUser = null

            Timber.i("User signed out")
            Result.success(Unit)
        } catch (e: IOException) {
            Timber.w(e, "IO error on sign out")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "Failed to sign out")
            Result.failure(e)
        }
    }

    override fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    override suspend fun updateMealGenerationSettings(
        itemsPerMeal: Int?,
        strictAllergenMode: Boolean?,
        strictDietaryMode: Boolean?,
        allowRecipeRepeat: Boolean?
    ): Result<Unit> {
        return try {
            val currentPrefs = userPreferencesDataStore.userPreferences.first()
                ?: return Result.failure(Exception("No user preferences found"))

            val updatedPrefs = currentPrefs.copy(
                itemsPerMeal = itemsPerMeal ?: currentPrefs.itemsPerMeal,
                strictAllergenMode = strictAllergenMode ?: currentPrefs.strictAllergenMode,
                strictDietaryMode = strictDietaryMode ?: currentPrefs.strictDietaryMode,
                allowRecipeRepeat = allowRecipeRepeat ?: currentPrefs.allowRecipeRepeat
            )

            // Save locally and sync to server
            updateUserPreferences(updatedPrefs)
        } catch (e: IOException) {
            Timber.w(e, "IO error updating meal generation settings")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update meal generation settings")
            Result.failure(e)
        }
    }
}
