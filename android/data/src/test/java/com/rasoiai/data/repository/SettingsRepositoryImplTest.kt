package com.rasoiai.data.repository

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import app.cash.turbine.test
import com.rasoiai.core.network.NetworkMonitor
import com.rasoiai.data.local.datastore.UserPreferencesDataStore
import com.rasoiai.data.remote.api.RasoiApiService
import com.rasoiai.domain.model.AppSettings
import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.DarkModePreference
import com.rasoiai.domain.model.DayOfWeek
import com.rasoiai.domain.model.DietaryRestriction
import com.rasoiai.domain.model.FamilyMember
import com.rasoiai.domain.model.MemberType
import com.rasoiai.domain.model.PrimaryDiet
import com.rasoiai.domain.model.SpiceLevel
import com.rasoiai.domain.model.UserPreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockContext: Context
    private lateinit var mockUserPreferencesDataStore: UserPreferencesDataStore
    private lateinit var mockApiService: RasoiApiService
    private lateinit var mockNetworkMonitor: NetworkMonitor
    private lateinit var repository: SettingsRepositoryImpl

    private val testPreferences = UserPreferences(
        householdSize = 4,
        familyMembers = listOf(
            FamilyMember(
                id = "member-1",
                name = "Test User",
                type = MemberType.ADULT,
                age = 30,
                specialNeeds = emptyList()
            )
        ),
        primaryDiet = PrimaryDiet.VEGETARIAN,
        dietaryRestrictions = listOf(DietaryRestriction.JAIN),
        cuisinePreferences = listOf(CuisineType.NORTH, CuisineType.SOUTH),
        spiceLevel = SpiceLevel.MEDIUM,
        dislikedIngredients = listOf("Bitter Gourd"),
        weekdayCookingTimeMinutes = 30,
        weekendCookingTimeMinutes = 60,
        busyDays = listOf(DayOfWeek.MONDAY)
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockContext = mockk(relaxed = true)
        mockUserPreferencesDataStore = mockk(relaxed = true)
        mockApiService = mockk(relaxed = true)
        mockNetworkMonitor = mockk(relaxed = true)

        repository = SettingsRepositoryImpl(
            context = mockContext,
            userPreferencesDataStore = mockUserPreferencesDataStore,
            apiService = mockApiService,
            networkMonitor = mockNetworkMonitor
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("getCurrentUser")
    inner class GetCurrentUser {

        @Test
        @DisplayName("Should return user when userId exists")
        fun `should return user when userId exists`() = runTest {
            // Given
            every { mockUserPreferencesDataStore.userId } returns flowOf("user-1")
            every { mockUserPreferencesDataStore.userPreferences } returns flowOf(testPreferences)
            every { mockUserPreferencesDataStore.isOnboarded } returns flowOf(true)

            // When & Then
            repository.getCurrentUser().test {
                val user = awaitItem()

                assertNotNull(user)
                assertEquals("user-1", user?.id)
                assertTrue(user?.isOnboarded == true)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("Should return null when no userId")
        fun `should return null when no userId`() = runTest {
            // Given
            every { mockUserPreferencesDataStore.userId } returns flowOf(null)
            every { mockUserPreferencesDataStore.userPreferences } returns flowOf(null)
            every { mockUserPreferencesDataStore.isOnboarded } returns flowOf(false)

            // When & Then
            repository.getCurrentUser().test {
                val user = awaitItem()

                assertNull(user)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("getAppSettings")
    inner class GetAppSettings {

        @Test
        @DisplayName("Should return default app settings")
        fun `should return default app settings`() = runTest {
            // When & Then
            repository.getAppSettings().test {
                val settings = awaitItem()

                assertEquals(DarkModePreference.SYSTEM, settings.darkMode)
                assertTrue(settings.notificationsEnabled)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("updateDarkMode")
    inner class UpdateDarkMode {

        @Test
        @DisplayName("Should update dark mode preference")
        fun `should update dark mode preference`() = runTest {
            // When
            val result = repository.updateDarkMode(DarkModePreference.DARK)

            // Then
            assertTrue(result.isSuccess)

            repository.getAppSettings().test {
                val settings = awaitItem()
                assertEquals(DarkModePreference.DARK, settings.darkMode)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("updateNotifications")
    inner class UpdateNotifications {

        @Test
        @DisplayName("Should update notifications setting")
        fun `should update notifications setting`() = runTest {
            // When
            val result = repository.updateNotifications(false)

            // Then
            assertTrue(result.isSuccess)

            repository.getAppSettings().test {
                val settings = awaitItem()
                assertEquals(false, settings.notificationsEnabled)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("updateUserPreferences")
    inner class UpdateUserPreferences {

        @Test
        @DisplayName("Should save preferences and sync when online")
        fun `should save preferences and sync when online`() = runTest {
            // Given
            every { mockNetworkMonitor.isOnline } returns flowOf(true)
            coEvery { mockApiService.updateUserPreferences(any()) } returns mockk(relaxed = true)

            // When
            val result = repository.updateUserPreferences(testPreferences)

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockUserPreferencesDataStore.saveOnboardingComplete(testPreferences) }
            coVerify { mockApiService.updateUserPreferences(any()) }
        }

        @Test
        @DisplayName("Should save preferences locally when offline")
        fun `should save preferences locally when offline`() = runTest {
            // Given
            every { mockNetworkMonitor.isOnline } returns flowOf(false)

            // When
            val result = repository.updateUserPreferences(testPreferences)

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockUserPreferencesDataStore.saveOnboardingComplete(testPreferences) }
            coVerify(exactly = 0) { mockApiService.updateUserPreferences(any()) }
        }
    }

    @Nested
    @DisplayName("Family Members")
    inner class FamilyMembers {

        @Test
        @DisplayName("Should add family member")
        fun `should add family member`() = runTest {
            // Given
            every { mockUserPreferencesDataStore.userPreferences } returns flowOf(testPreferences)

            val newMember = FamilyMember(
                id = "",
                name = "New Member",
                type = MemberType.ADULT,
                age = 25,
                specialNeeds = emptyList()
            )

            // When
            val result = repository.addFamilyMember(newMember)

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockUserPreferencesDataStore.saveOnboardingComplete(any()) }
        }

        @Test
        @DisplayName("Should fail to add member when no preferences")
        fun `should fail to add member when no preferences`() = runTest {
            // Given
            every { mockUserPreferencesDataStore.userPreferences } returns flowOf(null)

            val newMember = FamilyMember(
                id = "",
                name = "New Member",
                type = MemberType.ADULT,
                age = 25,
                specialNeeds = emptyList()
            )

            // When
            val result = repository.addFamilyMember(newMember)

            // Then
            assertTrue(result.isFailure)
            assertEquals("No user preferences found", result.exceptionOrNull()?.message)
        }

        @Test
        @DisplayName("Should update family member")
        fun `should update family member`() = runTest {
            // Given
            every { mockUserPreferencesDataStore.userPreferences } returns flowOf(testPreferences)

            val updatedMember = testPreferences.familyMembers.first().copy(name = "Updated Name")

            // When
            val result = repository.updateFamilyMember(updatedMember)

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockUserPreferencesDataStore.saveOnboardingComplete(any()) }
        }

        @Test
        @DisplayName("Should remove family member")
        fun `should remove family member`() = runTest {
            // Given
            every { mockUserPreferencesDataStore.userPreferences } returns flowOf(testPreferences)

            // When
            val result = repository.removeFamilyMember("member-1")

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockUserPreferencesDataStore.saveOnboardingComplete(any()) }
        }
    }

    @Nested
    @DisplayName("signOut")
    inner class SignOut {

        @Test
        @DisplayName("Should clear all local data")
        fun `should clear all local data`() = runTest {
            // When
            val result = repository.signOut()

            // Then
            assertTrue(result.isSuccess)
            coVerify { mockUserPreferencesDataStore.clearAuthTokens() }
            coVerify { mockUserPreferencesDataStore.clearPreferences() }
        }
    }

    @Nested
    @DisplayName("getAppVersion")
    inner class GetAppVersion {

        @Test
        @DisplayName("Should return app version from PackageManager")
        fun `should return app version from PackageManager`() {
            // Given
            val mockPackageManager = mockk<PackageManager>()
            val mockPackageInfo = mockk<PackageInfo>()
            mockPackageInfo.versionName = "2.1.0"

            every { mockContext.packageManager } returns mockPackageManager
            every { mockContext.packageName } returns "com.rasoiai.app"
            every { mockPackageManager.getPackageInfo("com.rasoiai.app", 0) } returns mockPackageInfo

            // When
            val version = repository.getAppVersion()

            // Then
            assertEquals("2.1.0", version)
        }

        @Test
        @DisplayName("Should return default version on error")
        fun `should return default version on error`() {
            // Given
            every { mockContext.packageManager } throws RuntimeException("Error")

            // When
            val version = repository.getAppVersion()

            // Then
            assertEquals("1.0.0", version)
        }
    }
}
