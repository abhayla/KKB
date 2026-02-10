package com.rasoiai.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rasoiai.domain.model.AppSettings
import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.DarkModePreference
import com.rasoiai.domain.model.DayOfWeek
import com.rasoiai.domain.model.DietaryRestriction
import com.rasoiai.domain.model.FamilyMember
import com.rasoiai.domain.model.MemberType
import com.rasoiai.domain.model.PrimaryDiet
import com.rasoiai.domain.model.SmallMeasurementUnit
import com.rasoiai.domain.model.SpecialDietaryNeed
import com.rasoiai.domain.model.SpiceLevel
import com.rasoiai.domain.model.UserPreferences
import com.rasoiai.domain.model.VolumeUnit
import com.rasoiai.domain.model.WeightUnit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) : UserPreferencesDataStoreInterface {
    private object PreferencesKeys {
        // Auth tokens
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val TOKEN_EXPIRY = longPreferencesKey("token_expiry")
        val USER_ID = stringPreferencesKey("user_id")

        // Onboarding and preferences
        val IS_ONBOARDED = booleanPreferencesKey("is_onboarded")
        val HOUSEHOLD_SIZE = intPreferencesKey("household_size")
        val FAMILY_MEMBERS_JSON = stringPreferencesKey("family_members_json")
        val PRIMARY_DIET = stringPreferencesKey("primary_diet")
        val DIETARY_RESTRICTIONS = stringSetPreferencesKey("dietary_restrictions")
        val CUISINE_PREFERENCES = stringSetPreferencesKey("cuisine_preferences")
        val SPICE_LEVEL = stringPreferencesKey("spice_level")
        val DISLIKED_INGREDIENTS = stringSetPreferencesKey("disliked_ingredients")
        val WEEKDAY_COOKING_TIME = intPreferencesKey("weekday_cooking_time")
        val WEEKEND_COOKING_TIME = intPreferencesKey("weekend_cooking_time")
        val BUSY_DAYS = stringSetPreferencesKey("busy_days")

        // Meal generation settings
        val ITEMS_PER_MEAL = intPreferencesKey("items_per_meal")
        val STRICT_ALLERGEN_MODE = booleanPreferencesKey("strict_allergen_mode")
        val STRICT_DIETARY_MODE = booleanPreferencesKey("strict_dietary_mode")
        val ALLOW_RECIPE_REPEAT = booleanPreferencesKey("allow_recipe_repeat")

        // User email (saved from auth response)
        val EMAIL = stringPreferencesKey("user_email")

        // App settings (persisted across restarts)
        val DARK_MODE = stringPreferencesKey("dark_mode")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val MEAL_REMINDER_TIME = stringPreferencesKey("meal_reminder_time")
        val VOLUME_UNIT = stringPreferencesKey("volume_unit")
        val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
        val SMALL_MEASUREMENT_UNIT = stringPreferencesKey("small_measurement_unit")
    }

    private val json = Json { ignoreUnknownKeys = true }

    // Auth state flows
    override val isAuthenticated: Flow<Boolean> = context.dataStore.data.map { preferences ->
        val token = preferences[PreferencesKeys.ACCESS_TOKEN]
        val expiry = preferences[PreferencesKeys.TOKEN_EXPIRY] ?: 0L
        !token.isNullOrEmpty() && System.currentTimeMillis() < expiry
    }

    override val accessToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ACCESS_TOKEN]
    }

    override val userId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USER_ID]
    }

    override val userEmail: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.EMAIL]
    }

    override val appSettings: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            darkMode = preferences[PreferencesKeys.DARK_MODE]?.let { name ->
                try { DarkModePreference.valueOf(name) } catch (_: Exception) { null }
            } ?: DarkModePreference.SYSTEM,
            notificationsEnabled = preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true,
            mealReminderTime = preferences[PreferencesKeys.MEAL_REMINDER_TIME] ?: "07:00",
            volumeUnit = preferences[PreferencesKeys.VOLUME_UNIT]?.let { name ->
                try { VolumeUnit.valueOf(name) } catch (_: Exception) { null }
            } ?: VolumeUnit.INDIAN,
            weightUnit = preferences[PreferencesKeys.WEIGHT_UNIT]?.let { name ->
                try { WeightUnit.valueOf(name) } catch (_: Exception) { null }
            } ?: WeightUnit.METRIC,
            smallMeasurementUnit = preferences[PreferencesKeys.SMALL_MEASUREMENT_UNIT]?.let { name ->
                try { SmallMeasurementUnit.valueOf(name) } catch (_: Exception) { null }
            } ?: SmallMeasurementUnit.INDIAN
        )
    }

    override suspend fun saveAuthTokens(
        accessToken: String,
        refreshToken: String,
        expiresInSeconds: Long,
        userId: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.ACCESS_TOKEN] = accessToken
            prefs[PreferencesKeys.REFRESH_TOKEN] = refreshToken
            prefs[PreferencesKeys.TOKEN_EXPIRY] = System.currentTimeMillis() + (expiresInSeconds * 1000)
            prefs[PreferencesKeys.USER_ID] = userId
        }
    }

    override suspend fun getAccessTokenSync(): String? {
        return context.dataStore.data.map { preferences ->
            val expiry = preferences[PreferencesKeys.TOKEN_EXPIRY] ?: 0L
            if (System.currentTimeMillis() < expiry) {
                preferences[PreferencesKeys.ACCESS_TOKEN]
            } else {
                null
            }
        }.first()
    }

    override suspend fun getRefreshToken(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.REFRESH_TOKEN]
        }.first()
    }

    override suspend fun updateAccessToken(accessToken: String, expiresInSeconds: Long) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.ACCESS_TOKEN] = accessToken
            prefs[PreferencesKeys.TOKEN_EXPIRY] = System.currentTimeMillis() + (expiresInSeconds * 1000)
        }
    }

    override suspend fun clearAuthTokens() {
        context.dataStore.edit { prefs ->
            prefs.remove(PreferencesKeys.ACCESS_TOKEN)
            prefs.remove(PreferencesKeys.REFRESH_TOKEN)
            prefs.remove(PreferencesKeys.TOKEN_EXPIRY)
            prefs.remove(PreferencesKeys.USER_ID)
            prefs.remove(PreferencesKeys.EMAIL)
        }
    }

    override suspend fun saveEmail(email: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.EMAIL] = email
        }
    }

    override suspend fun saveAppSettings(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DARK_MODE] = settings.darkMode.name
            prefs[PreferencesKeys.NOTIFICATIONS_ENABLED] = settings.notificationsEnabled
            prefs[PreferencesKeys.MEAL_REMINDER_TIME] = settings.mealReminderTime
            prefs[PreferencesKeys.VOLUME_UNIT] = settings.volumeUnit.name
            prefs[PreferencesKeys.WEIGHT_UNIT] = settings.weightUnit.name
            prefs[PreferencesKeys.SMALL_MEASUREMENT_UNIT] = settings.smallMeasurementUnit.name
        }
    }

    override val isOnboarded: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_ONBOARDED] ?: false
    }

    override val userPreferences: Flow<UserPreferences?> = context.dataStore.data.map { preferences ->
        val isOnboarded = preferences[PreferencesKeys.IS_ONBOARDED] ?: false
        if (!isOnboarded) return@map null

        val familyMembersJson = preferences[PreferencesKeys.FAMILY_MEMBERS_JSON] ?: "[]"
        val familyMembers = try {
            json.decodeFromString<List<FamilyMemberDto>>(familyMembersJson).map { it.toDomain() }
        } catch (e: Exception) {
            emptyList()
        }

        UserPreferences(
            householdSize = preferences[PreferencesKeys.HOUSEHOLD_SIZE] ?: 2,
            familyMembers = familyMembers,
            primaryDiet = PrimaryDiet.fromValue(
                preferences[PreferencesKeys.PRIMARY_DIET] ?: PrimaryDiet.VEGETARIAN.value
            ),
            dietaryRestrictions = preferences[PreferencesKeys.DIETARY_RESTRICTIONS]
                ?.mapNotNull { DietaryRestriction.fromValue(it) } ?: emptyList(),
            cuisinePreferences = preferences[PreferencesKeys.CUISINE_PREFERENCES]
                ?.map { CuisineType.fromValue(it) } ?: listOf(CuisineType.NORTH),
            spiceLevel = SpiceLevel.fromValue(
                preferences[PreferencesKeys.SPICE_LEVEL] ?: SpiceLevel.MEDIUM.value
            ),
            dislikedIngredients = preferences[PreferencesKeys.DISLIKED_INGREDIENTS]?.toList()
                ?: emptyList(),
            weekdayCookingTimeMinutes = preferences[PreferencesKeys.WEEKDAY_COOKING_TIME] ?: 30,
            weekendCookingTimeMinutes = preferences[PreferencesKeys.WEEKEND_COOKING_TIME] ?: 45,
            busyDays = preferences[PreferencesKeys.BUSY_DAYS]
                ?.mapNotNull { DayOfWeek.fromValue(it) } ?: emptyList(),
            // Meal generation settings
            itemsPerMeal = preferences[PreferencesKeys.ITEMS_PER_MEAL] ?: 2,
            strictAllergenMode = preferences[PreferencesKeys.STRICT_ALLERGEN_MODE] ?: true,
            strictDietaryMode = preferences[PreferencesKeys.STRICT_DIETARY_MODE] ?: true,
            allowRecipeRepeat = preferences[PreferencesKeys.ALLOW_RECIPE_REPEAT] ?: false
        )
    }

    override suspend fun saveOnboardingComplete(preferences: UserPreferences) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.IS_ONBOARDED] = true
            prefs[PreferencesKeys.HOUSEHOLD_SIZE] = preferences.householdSize
            prefs[PreferencesKeys.FAMILY_MEMBERS_JSON] = json.encodeToString(
                preferences.familyMembers.map { it.toDto() }
            )
            prefs[PreferencesKeys.PRIMARY_DIET] = preferences.primaryDiet.value
            prefs[PreferencesKeys.DIETARY_RESTRICTIONS] =
                preferences.dietaryRestrictions.map { it.value }.toSet()
            prefs[PreferencesKeys.CUISINE_PREFERENCES] =
                preferences.cuisinePreferences.map { it.value }.toSet()
            prefs[PreferencesKeys.SPICE_LEVEL] = preferences.spiceLevel.value
            prefs[PreferencesKeys.DISLIKED_INGREDIENTS] = preferences.dislikedIngredients.toSet()
            prefs[PreferencesKeys.WEEKDAY_COOKING_TIME] = preferences.weekdayCookingTimeMinutes
            prefs[PreferencesKeys.WEEKEND_COOKING_TIME] = preferences.weekendCookingTimeMinutes
            prefs[PreferencesKeys.BUSY_DAYS] = preferences.busyDays.map { it.value }.toSet()
            // Meal generation settings
            prefs[PreferencesKeys.ITEMS_PER_MEAL] = preferences.itemsPerMeal
            prefs[PreferencesKeys.STRICT_ALLERGEN_MODE] = preferences.strictAllergenMode
            prefs[PreferencesKeys.STRICT_DIETARY_MODE] = preferences.strictDietaryMode
            prefs[PreferencesKeys.ALLOW_RECIPE_REPEAT] = preferences.allowRecipeRepeat
        }
    }

    override suspend fun clearPreferences() {
        context.dataStore.edit { it.clear() }
    }
}

@kotlinx.serialization.Serializable
private data class FamilyMemberDto(
    val id: String,
    val name: String,
    val type: String,
    val age: Int?,
    val specialNeeds: List<String>
) {
    fun toDomain(): FamilyMember = FamilyMember(
        id = id,
        name = name,
        type = MemberType.fromValue(type),
        age = age,
        specialNeeds = specialNeeds.mapNotNull { SpecialDietaryNeed.fromValue(it) }
    )
}

private fun FamilyMember.toDto(): FamilyMemberDto = FamilyMemberDto(
    id = id,
    name = name,
    type = type.value,
    age = age,
    specialNeeds = specialNeeds.map { it.value }
)
