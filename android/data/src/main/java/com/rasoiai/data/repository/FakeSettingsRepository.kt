package com.rasoiai.data.repository

import com.rasoiai.domain.model.AppSettings
import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.DarkModePreference
import com.rasoiai.domain.model.DietaryRestriction
import com.rasoiai.domain.model.FamilyMember
import com.rasoiai.domain.model.MemberType
import com.rasoiai.domain.model.PrimaryDiet
import com.rasoiai.domain.model.SmallMeasurementUnit
import com.rasoiai.domain.model.SpecialDietaryNeed
import com.rasoiai.domain.model.SpiceLevel
import com.rasoiai.domain.model.User
import com.rasoiai.domain.model.UserPreferences
import com.rasoiai.domain.model.VolumeUnit
import com.rasoiai.domain.model.WeightUnit
import com.rasoiai.domain.repository.SettingsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake implementation of SettingsRepository for development and testing.
 */
@Singleton
class FakeSettingsRepository @Inject constructor() : SettingsRepository {

    private val _currentUser = MutableStateFlow<User?>(createMockUser())
    private val _appSettings = MutableStateFlow(AppSettings())

    override fun getCurrentUser(): Flow<User?> = _currentUser.asStateFlow()

    override fun getAppSettings(): Flow<AppSettings> = _appSettings.asStateFlow()

    override suspend fun updateDarkMode(preference: DarkModePreference): Result<Unit> {
        delay(200)
        _appSettings.value = _appSettings.value.copy(darkMode = preference)
        return Result.success(Unit)
    }

    override suspend fun updateNotifications(enabled: Boolean): Result<Unit> {
        delay(200)
        _appSettings.value = _appSettings.value.copy(notificationsEnabled = enabled)
        return Result.success(Unit)
    }

    override suspend fun updateUserPreferences(preferences: UserPreferences): Result<Unit> {
        delay(300)
        _currentUser.value = _currentUser.value?.copy(preferences = preferences)
        return Result.success(Unit)
    }

    override suspend fun addFamilyMember(member: FamilyMember): Result<Unit> {
        delay(300)
        val currentPrefs = _currentUser.value?.preferences ?: return Result.failure(Exception("No user"))
        val updatedMembers = currentPrefs.familyMembers + member
        _currentUser.value = _currentUser.value?.copy(
            preferences = currentPrefs.copy(
                familyMembers = updatedMembers,
                householdSize = updatedMembers.size
            )
        )
        return Result.success(Unit)
    }

    override suspend fun updateFamilyMember(member: FamilyMember): Result<Unit> {
        delay(300)
        val currentPrefs = _currentUser.value?.preferences ?: return Result.failure(Exception("No user"))
        val updatedMembers = currentPrefs.familyMembers.map {
            if (it.id == member.id) member else it
        }
        _currentUser.value = _currentUser.value?.copy(
            preferences = currentPrefs.copy(familyMembers = updatedMembers)
        )
        return Result.success(Unit)
    }

    override suspend fun removeFamilyMember(memberId: String): Result<Unit> {
        delay(300)
        val currentPrefs = _currentUser.value?.preferences ?: return Result.failure(Exception("No user"))
        val updatedMembers = currentPrefs.familyMembers.filter { it.id != memberId }
        _currentUser.value = _currentUser.value?.copy(
            preferences = currentPrefs.copy(
                familyMembers = updatedMembers,
                householdSize = updatedMembers.size
            )
        )
        return Result.success(Unit)
    }

    override suspend fun updateAppSettings(settings: AppSettings): Result<Unit> {
        delay(200)
        _appSettings.value = settings
        return Result.success(Unit)
    }

    override suspend fun signOut(): Result<Unit> {
        delay(500)
        _currentUser.value = null
        return Result.success(Unit)
    }

    override fun getAppVersion(): String = "1.0.0"

    private fun createMockUser(): User = User(
        id = "user-1",
        email = "priya.sharma@gmail.com",
        name = "Priya Sharma",
        profileImageUrl = null,
        isOnboarded = true,
        preferences = UserPreferences(
            householdSize = 4,
            familyMembers = listOf(
                FamilyMember(
                    id = "member-1",
                    name = "Priya",
                    type = MemberType.ADULT,
                    age = 32,
                    specialNeeds = emptyList()
                ),
                FamilyMember(
                    id = "member-2",
                    name = "Rahul",
                    type = MemberType.ADULT,
                    age = 35,
                    specialNeeds = emptyList()
                ),
                FamilyMember(
                    id = "member-3",
                    name = "Ananya",
                    type = MemberType.CHILD,
                    age = 8,
                    specialNeeds = listOf(SpecialDietaryNeed.NO_SPICY)
                ),
                FamilyMember(
                    id = "member-4",
                    name = "Dadi",
                    type = MemberType.SENIOR,
                    age = 72,
                    specialNeeds = listOf(SpecialDietaryNeed.DIABETIC, SpecialDietaryNeed.SOFT_FOOD)
                )
            ),
            primaryDiet = PrimaryDiet.VEGETARIAN,
            dietaryRestrictions = listOf(DietaryRestriction.SATTVIC),
            cuisinePreferences = listOf(CuisineType.NORTH, CuisineType.SOUTH),
            spiceLevel = SpiceLevel.MEDIUM,
            dislikedIngredients = listOf("Bitter gourd", "Raw onion"),
            weekdayCookingTimeMinutes = 30,
            weekendCookingTimeMinutes = 60,
            busyDays = emptyList()
        )
    )
}
