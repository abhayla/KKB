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
import com.rasoiai.app.e2e.util.GoogleAuthTestHelper
import com.rasoiai.app.e2e.util.RetryUtils
import com.rasoiai.app.presentation.auth.GoogleAuthClientInterface
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
 * Base class for E2E tests that use REAL Google Authentication.
 *
 * ## How it differs from BaseE2ETest
 * - Does NOT use FakeAuthModule - requires real Google sign-in
 * - Uses UI Automator to interact with Google sign-in system dialog
 * - Requires a Google account to be signed in on the device/emulator
 *
 * ## Prerequisites
 * 1. Emulator/device must have a Google account signed in
 * 2. Backend must be running at localhost:8000
 * 3. App must be configured with valid Firebase credentials
 *
 * ## Test Flow
 * 1. App shows Auth screen with "Sign in with Google" button
 * 2. Test clicks the button (via Compose test)
 * 3. Google shows system account picker dialog
 * 4. UI Automator selects the account
 * 5. Google returns credential to app
 * 6. App exchanges with Firebase, gets token
 * 7. App calls backend /auth/firebase with real Firebase token
 * 8. Backend returns JWT, test continues
 *
 * NOTE: Since FakeAuthModule uses @TestInstallIn (auto-replaces in tests),
 * real Google auth tests require NOT having FakeAuthModule in the test classpath,
 * OR the user needs to configure tests differently. For now, this test works
 * alongside the fake auth - the fake client will be used, which returns
 * "fake-firebase-token". The backend accepts this for E2E testing.
 */
@HiltAndroidTest
abstract class RealGoogleAuthE2ETest {

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
    lateinit var googleAuthClient: GoogleAuthClientInterface

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
     * Performs real Google sign-in flow.
     * 1. Clears any existing auth state
     * 2. Waits for Auth screen to appear
     * 3. Clicks "Sign in with Google" button (done by caller)
     * 4. Uses UI Automator to select Google account
     * 5. Waits for sign-in to complete
     *
     * @param accountEmail Optional specific email to select
     * @return true if sign-in was successful
     */
    protected fun performRealGoogleSignIn(accountEmail: String? = null): Boolean {
        Log.d(TAG, "Starting real Google sign-in flow...")

        // Take a debug screenshot before attempting sign-in
        GoogleAuthTestHelper.takeDebugScreenshot("before_google_signin")

        // Wait for Google account picker to appear
        Thread.sleep(2000) // Give time for dialog to animate in

        // Take screenshot of the dialog
        GoogleAuthTestHelper.takeDebugScreenshot("google_dialog_appeared")
        GoogleAuthTestHelper.dumpUiHierarchy()

        // Select the Google account
        val accountSelected = GoogleAuthTestHelper.selectGoogleAccount(
            accountEmail = accountEmail,
            timeoutMs = GOOGLE_DIALOG_TIMEOUT
        )

        if (!accountSelected) {
            Log.e(TAG, "Failed to select Google account")
            GoogleAuthTestHelper.takeDebugScreenshot("account_selection_failed")
            return false
        }

        // Wait for sign-in to complete
        val signInComplete = GoogleAuthTestHelper.waitForSignInComplete(
            timeoutMs = SIGN_IN_COMPLETE_TIMEOUT
        )

        if (!signInComplete) {
            Log.e(TAG, "Sign-in did not complete in time")
            GoogleAuthTestHelper.takeDebugScreenshot("signin_timeout")
            return false
        }

        Log.d(TAG, "Google sign-in flow completed successfully")
        GoogleAuthTestHelper.takeDebugScreenshot("after_google_signin")
        return true
    }

    /**
     * Clears all authentication state for a fresh test.
     */
    protected fun clearAuthState() {
        runBlocking {
            try {
                googleAuthClient.signOut()
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
        private const val TAG = "RealGoogleAuthE2ETest"

        // Backend URL
        const val BACKEND_BASE_URL = "http://10.0.2.2:8000"

        // Timeouts
        const val SHORT_TIMEOUT = 2000L
        const val MEDIUM_TIMEOUT = 5000L
        const val LONG_TIMEOUT = 10000L
        const val GOOGLE_DIALOG_TIMEOUT = 15000L
        const val SIGN_IN_COMPLETE_TIMEOUT = 20000L

        // Animation durations
        const val ANIMATION_DURATION = 300L
        const val SPLASH_DURATION = 2500L
    }
}
