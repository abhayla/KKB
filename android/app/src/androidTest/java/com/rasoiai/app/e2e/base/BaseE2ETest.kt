package com.rasoiai.app.e2e.base

import android.content.Context
import android.util.Log
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.rasoiai.app.TestActivity
import com.rasoiai.app.e2e.di.FakeGoogleAuthClient
import com.rasoiai.app.e2e.rules.RetryRule
import com.rasoiai.app.e2e.util.BackendTestHelper
import com.rasoiai.app.e2e.util.RetryUtils
import com.rasoiai.data.local.dao.RecipeRulesDao
import com.rasoiai.data.local.datastore.UserPreferencesDataStoreInterface
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.DayOfWeek
import com.rasoiai.domain.model.PrimaryDiet
import com.rasoiai.domain.model.SpiceLevel
import com.rasoiai.domain.model.UserPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import javax.inject.Inject
import kotlin.math.min

/**
 * Base class for E2E tests providing common setup and utilities.
 * All E2E flow tests should extend this class.
 *
 * ## Test Architecture
 * This test setup uses "Real Backend + Fake Google Auth + Real DataStore":
 * - FakeGoogleAuthClient: Bypasses Google OAuth, returns "fake-firebase-token"
 * - UserPreferencesDataStore: REAL DataStore (persists to disk)
 * - All Repositories: REAL implementations calling real backend APIs
 *
 * ## Test Suite Execution Order
 * When running via E2ETestSuite:
 * 1. CoreDataFlowTest runs first - clears state, does full auth/onboarding, persists state
 * 2. HomeScreenTest, GroceryFlowTest, etc. - inherit persisted state from real DataStore
 *
 * ## Authentication Setup
 * Tests that need to start at Home screen should call [setUpAuthenticatedState]
 * in their @Before method after calling super.setUp().
 */
@HiltAndroidTest
abstract class BaseE2ETest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    /**
     * Retry rule for handling flaky tests.
     * NOTE: Disabled (maxRetries=0) because Hilt doesn't support re-injection
     * when retrying tests. The action-level retries in RetryUtils still work.
     * Order = 2 means it runs after Hilt and Compose rules are set up.
     */
    @get:Rule(order = 2)
    val retryRule = RetryRule(
        maxRetries = 0,  // Disabled - Hilt doesn't support re-injection on retry
        onRetry = { attempt, error ->
            Log.w(TAG, "Test retry #$attempt after: ${error.message}")
            onTestRetry(attempt, error)
        }
    )

    @Inject
    lateinit var fakeGoogleAuthClient: FakeGoogleAuthClient

    @Inject
    lateinit var userPreferencesDataStore: UserPreferencesDataStoreInterface

    @Inject
    lateinit var recipeRulesDao: RecipeRulesDao

    protected val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    open fun setUp() {
        hiltRule.inject()
        RetryUtils.stats.reset()
    }

    @After
    open fun tearDown() {
        // Print retry statistics if any retries occurred
        if (RetryUtils.stats.totalAttempts > 0) {
            RetryUtils.stats.printSummary()
        }
    }

    /**
     * Called when a test is about to be retried.
     * Override in subclasses to perform cleanup between retries.
     */
    protected open fun onTestRetry(attempt: Int, error: Throwable) {
        // Default implementation: wait for compose to settle
        try {
            composeTestRule.waitForIdle()
        } catch (e: Exception) {
            Log.w(TAG, "Error during retry cleanup: ${e.message}")
        }
    }

    /**
     * Sets up a fully authenticated and onboarded user state.
     * Call this in tests that need to start at the Home screen.
     *
     * Note: The activity is already launched by createAndroidComposeRule.
     * This method sets up the auth state which will be used by subsequent API calls.
     * The SplashViewModel will navigate based on this state.
     *
     * This method:
     * 1. Calls the backend API with fake-firebase-token to get a real JWT
     * 2. Stores the JWT in REAL UserPreferencesDataStore (persists to disk)
     * 3. Sets isSignedIn via FakeGoogleAuthClient
     * 4. Generates a meal plan for the test user (ensures meal cards are available)
     *
     * This allows the app to make real API calls.
     */
    protected fun setUpAuthenticatedState() {
        // Step 1: Get a real JWT token from the backend
        val authResult = authenticateWithBackend()

        // Step 2: Set up fake auth client state
        fakeGoogleAuthClient.simulateSignedIn()

        // Step 3: Store the JWT in REAL DataStore (persists to disk)
        if (authResult != null) {
            runBlocking {
                userPreferencesDataStore.saveAuthTokens(
                    accessToken = authResult.accessToken,
                    refreshToken = "",
                    expiresInSeconds = 3600,
                    userId = authResult.userId
                )

                // Step 4: Save test preferences to mark as onboarded
                // This is required for SplashViewModel to navigate to Home instead of Onboarding
                userPreferencesDataStore.saveOnboardingComplete(createTestPreferences())
            }
            Log.d(TAG, "Authenticated with backend: userId=${authResult.userId}")

            // Step 5: Generate meal plan for test user (ensures meal cards are available)
            // This is an async operation that can take 4-7 seconds
            val mealPlanGenerated = BackendTestHelper.generateMealPlan(
                baseUrl = BACKEND_BASE_URL,
                authToken = authResult.accessToken
            )

            if (mealPlanGenerated) {
                Log.i(TAG, "Meal plan generated for test user")
            } else {
                Log.w(TAG, "Failed to generate meal plan - some tests requiring meal cards may fail")
            }
        } else {
            Log.w(TAG, "Failed to authenticate with backend")
        }
    }

    /**
     * Creates minimal test preferences for E2E tests.
     * This is used to mark the user as onboarded.
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
     * Clears all user state for a fresh test start.
     * Call this in CoreDataFlowTest @Before to ensure clean state.
     */
    protected fun clearAllState() {
        runBlocking {
            userPreferencesDataStore.clearPreferences()
        }
        fakeGoogleAuthClient.reset()
        FakeGoogleAuthClient.resetStaticState()
        Log.d(TAG, "Cleared all state for fresh test")
    }

    /**
     * Clears all recipe rules and nutrition goals from both Room DB and backend.
     * Call this in test @Before methods to prevent DuplicateRuleException
     * when rules from prior test runs still exist.
     */
    protected fun clearRecipeRulesAndGoals() {
        // 1. Clear Room DB
        runBlocking {
            recipeRulesDao.deleteAllRules()
            recipeRulesDao.deleteAllNutritionGoals()
        }
        Log.d(TAG, "Cleared recipe rules and nutrition goals from Room DB")

        // 2. Clear backend (needs auth token from DataStore)
        val authToken = runBlocking { userPreferencesDataStore.accessToken.first() }
        if (authToken != null) {
            val (rulesDeleted, goalsDeleted) = BackendTestHelper.clearAllRecipeRulesAndGoals(
                BACKEND_BASE_URL, authToken
            )
            Log.d(TAG, "Cleared backend: $rulesDeleted rules, $goalsDeleted goals")
        } else {
            Log.w(TAG, "No auth token available — skipping backend cleanup")
        }
    }

    /**
     * Authenticates with the backend using fake-firebase-token.
     * Uses BackendTestHelper for retry logic on transient failures.
     * Returns the JWT access token and user ID if successful.
     */
    private fun authenticateWithBackend(): AuthResult? {
        Log.d(TAG, "Calling backend auth: $BACKEND_BASE_URL/api/v1/auth/firebase")

        val result = BackendTestHelper.authenticateWithRetry(
            baseUrl = BACKEND_BASE_URL,
            firebaseToken = "fake-firebase-token",
            maxRetries = 3
        )

        return if (result != null) {
            Log.d(TAG, "Backend auth successful: userId=${result.userId}")
            AuthResult(result.accessToken, result.userId)
        } else {
            Log.e(TAG, "Backend auth failed after retries")
            // Log diagnostic info for debugging
            BackendTestHelper.diagnoseConnection(BACKEND_BASE_URL)
            null
        }
    }

    private data class AuthResult(val accessToken: String, val userId: String)

    /**
     * Sets up an authenticated but NOT onboarded user state.
     * Call this in tests that need to test the Onboarding flow.
     */
    protected fun setUpNewUserState() {
        fakeGoogleAuthClient.setSignInSuccess()
        // Real DataStore - just clear it to simulate new user
        runBlocking {
            userPreferencesDataStore.clearPreferences()
        }
    }

    /**
     * Resets auth state to logged out.
     */
    protected fun resetAuthState() {
        fakeGoogleAuthClient.reset()
        runBlocking {
            userPreferencesDataStore.clearPreferences()
        }
    }

    /**
     * Wait for a condition to be true with timeout and exponential backoff.
     * Uses exponential backoff polling to reduce CPU usage and flakiness.
     *
     * @param timeoutMillis Maximum time to wait
     * @param initialPollMs Initial polling interval
     * @param maxPollMs Maximum polling interval (cap for backoff)
     * @param backoffMultiplier Multiplier for each poll interval
     * @param conditionDescription Description for error messages
     * @param condition The condition to wait for
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
            if (condition()) {
                return
            }

            Thread.sleep(currentPoll)

            // Exponential backoff, capped at maxPollMs
            currentPoll = min((currentPoll * backoffMultiplier).toLong(), maxPollMs)
        }

        // Final check
        if (!condition()) {
            throw AssertionError("Timeout waiting for $conditionDescription after ${timeoutMillis}ms")
        }
    }

    /**
     * Wait for a condition with standard timeout and backoff settings.
     * Shorthand for common use case.
     */
    protected fun waitUntilWithBackoff(
        timeoutMillis: Long = MEDIUM_TIMEOUT,
        conditionDescription: String = "condition",
        condition: () -> Boolean
    ) {
        waitUntil(
            timeoutMillis = timeoutMillis,
            initialPollMs = 100,
            maxPollMs = 500,
            backoffMultiplier = 1.5,
            conditionDescription = conditionDescription,
            condition = condition
        )
    }

    /**
     * Wait for idle state in Compose.
     */
    protected fun waitForIdle() {
        composeTestRule.waitForIdle()
    }

    /**
     * Wait for a specific duration (use sparingly).
     */
    protected fun waitFor(millis: Long) {
        Thread.sleep(millis)
    }

    companion object {
        private const val TAG = "BaseE2ETest"

        // Backend URL - use 10.0.2.2 for Android emulator to access localhost
        const val BACKEND_BASE_URL = "http://10.0.2.2:8000"

        // Common timeout values
        const val SHORT_TIMEOUT = 2000L
        const val MEDIUM_TIMEOUT = 5000L
        const val LONG_TIMEOUT = 10000L

        // Animation durations
        const val ANIMATION_DURATION = 300L
        const val SPLASH_DURATION = 2500L
    }
}
