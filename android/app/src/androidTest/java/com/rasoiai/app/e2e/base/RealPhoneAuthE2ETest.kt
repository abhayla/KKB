package com.rasoiai.app.e2e.base

import android.content.Context
import android.util.Log
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.rasoiai.app.TestActivity
import com.rasoiai.app.e2e.rules.RetryRule
import com.rasoiai.app.e2e.util.BackendTestHelper
import com.rasoiai.app.e2e.util.RetryUtils
import com.rasoiai.app.presentation.auth.PhoneAuthClientInterface
import com.rasoiai.data.local.datastore.UserPreferencesDataStoreInterface
import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.DayOfWeek
import com.rasoiai.domain.model.PrimaryDiet
import com.rasoiai.domain.model.SpiceLevel
import com.rasoiai.domain.model.UserPreferences
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import javax.inject.Inject
import kotlin.math.min

/**
 * Base class for E2E tests that use REAL Firebase Phone Authentication.
 *
 * ## How it differs from BaseE2ETest
 * - Does NOT use FakeAuthModule - requires real Firebase phone auth
 * - Uses Firebase test phone number (+91 11 1111 1111 / OTP 123456) for auto-verification
 * - Requires backend running at localhost:8000
 *
 * ## Prerequisites
 * 1. Backend must be running at localhost:8000
 * 2. App must be configured with valid Firebase credentials
 * 3. Firebase Console must have test phone number configured
 *
 * NOTE: Since FakeAuthModule uses @TestInstallIn (auto-replaces in tests),
 * real phone auth tests require NOT having FakeAuthModule in the test classpath.
 * For now, the fake client will be used, which returns "fake-firebase-token".
 * The backend accepts this for E2E testing.
 */
@HiltAndroidTest
abstract class RealPhoneAuthE2ETest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    @get:Rule(order = 2)
    val retryRule = RetryRule(
        maxRetries = 0,
        onRetry = { attempt, error ->
            Log.w(TAG, "Test retry #$attempt after: ${error.message}")
        }
    )

    @Inject
    lateinit var phoneAuthClient: PhoneAuthClientInterface

    @Inject
    lateinit var userPreferencesDataStore: UserPreferencesDataStoreInterface

    protected val context: Context
        get() = ApplicationProvider.getApplicationContext()

    protected val uiDevice: UiDevice
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    open fun setUp() {
        hiltRule.inject()
        RetryUtils.stats.reset()
    }

    @After
    open fun tearDown() {
        if (RetryUtils.stats.totalAttempts > 0) {
            RetryUtils.stats.printSummary()
        }
    }

    /**
     * Clears all authentication state for a fresh test.
     */
    protected fun clearAuthState() {
        runBlocking {
            try {
                phoneAuthClient.signOut()
            } catch (e: Exception) {
                Log.w(TAG, "Error signing out: ${e.message}")
            }
            userPreferencesDataStore.clearPreferences()
        }
        Log.d(TAG, "Auth state cleared")
    }

    /**
     * Saves onboarding preferences to skip onboarding flow.
     * Use this after sign-in to go directly to Home screen.
     */
    protected fun saveOnboardingPreferences() {
        runBlocking {
            userPreferencesDataStore.saveOnboardingComplete(createTestPreferences())
        }
        Log.d(TAG, "Onboarding preferences saved")
    }

    /**
     * Creates test user preferences.
     */
    private fun createTestPreferences(): UserPreferences {
        return UserPreferences(
            householdSize = 4,
            familyMembers = emptyList(),
            primaryDiet = PrimaryDiet.VEGETARIAN,
            dietaryRestrictions = emptyList(),
            cuisinePreferences = listOf(CuisineType.NORTH, CuisineType.SOUTH),
            spiceLevel = SpiceLevel.MEDIUM,
            dislikedIngredients = emptyList(),
            weekdayCookingTimeMinutes = 30,
            weekendCookingTimeMinutes = 60,
            busyDays = listOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
            itemsPerMeal = 2,
            strictAllergenMode = true,
            strictDietaryMode = true,
            allowRecipeRepeat = false
        )
    }

    /**
     * Wait for a condition to be true with timeout.
     */
    protected fun waitUntil(
        timeoutMillis: Long = 5000,
        initialPollMs: Long = 50,
        maxPollMs: Long = 500,
        backoffMultiplier: Double = 1.5,
        conditionDescription: String = "condition",
        condition: () -> Boolean
    ) {
        val startTime = System.currentTimeMillis()
        var currentPoll = initialPollMs

        while ((System.currentTimeMillis() - startTime) < timeoutMillis) {
            if (condition()) return
            Thread.sleep(currentPoll)
            currentPoll = min((currentPoll * backoffMultiplier).toLong(), maxPollMs)
        }

        if (!condition()) {
            throw AssertionError("Timeout waiting for $conditionDescription after ${timeoutMillis}ms")
        }
    }

    protected fun waitForIdle() {
        composeTestRule.waitForIdle()
    }

    protected fun waitFor(millis: Long) {
        Thread.sleep(millis)
    }

    companion object {
        private const val TAG = "RealPhoneAuthE2ETest"

        // Backend URL
        const val BACKEND_BASE_URL = "http://10.0.2.2:8000"

        // Timeouts
        const val SHORT_TIMEOUT = 2000L
        const val MEDIUM_TIMEOUT = 5000L
        const val LONG_TIMEOUT = 10000L

        // Animation durations
        const val ANIMATION_DURATION = 300L
        const val SPLASH_DURATION = 2500L
    }
}
