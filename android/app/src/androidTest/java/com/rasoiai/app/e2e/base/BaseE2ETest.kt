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
import com.rasoiai.data.local.dao.MealPlanDao
import com.rasoiai.data.local.dao.RecipeRulesDao
import com.rasoiai.data.local.datastore.UserPreferencesDataStoreInterface
import com.rasoiai.data.local.entity.MealPlanEntity
import com.rasoiai.data.local.entity.MealPlanFestivalEntity
import com.rasoiai.data.local.entity.MealPlanItemEntity
import org.json.JSONObject
import java.util.UUID
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

    @Inject
    lateinit var mealPlanDao: MealPlanDao

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
     * ## Timing constraint
     * `createAndroidComposeRule` launches the Activity BEFORE setUp() runs.
     * SplashViewModel checks `googleAuthClient.isSignedIn` ONCE after a 2-second
     * delay (one-shot, not reactive). `simulateSignedIn()` + token storage MUST
     * complete before that 2s check fires, or the app navigates to Auth (stuck).
     *
     * ## Why we do NOT call generateMealPlan() here
     * `BackendTestHelper.generateMealPlan()` calls Gemini AI (4-45s). If we block
     * on it before storing tokens, the 2s window expires and the app gets stuck.
     * If we call it AFTER storing tokens, HomeViewModel also calls Gemini (Room is
     * empty) → two concurrent Gemini calls → SocketTimeoutException.
     *
     * Instead, we store tokens FIRST (so the app reaches Home), then do a fast GET
     * to check if the backend already has a meal plan and seed Room. If Room gets
     * seeded before HomeVM loads, HomeVM skips generation. If not (first-ever run),
     * HomeVM handles generation itself (single Gemini call — no race condition).
     *
     * ## Timeline (backend has meal plan — common case after first run)
     * T=0.0s  Activity launches → SplashVM starts 2s timer
     * T=0.2s  setUp() → setUpAuthenticatedState()
     * T=0.7s  authenticateWithBackend() completes (~0.5s)
     * T=0.8s  simulateSignedIn + saveAuthTokens (instant) ← BEFORE 2s deadline
     * T=2.0s  SplashVM: isSignedIn=true → NavigateToHome
     * T=1-4s  getCurrentMealPlan() → found, seedMealPlanFromBackend() → Room populated
     * T=2.0s  HomeVM: Room has data → NO Gemini call ✓
     *
     * ## Timeline (first-ever run — no meal plan on backend)
     * T=0.8s  tokens stored
     * T=2.0s  SplashVM → Home, HomeVM: Room empty → generateNewMealPlan() (single call)
     * T=6-47s Gemini completes → meal plan in Room → UI updates
     */
    protected fun setUpAuthenticatedState() {
        // Step 1: Get JWT from backend (~0.5s)
        val authResult = authenticateWithBackend() ?: run {
            Log.w(TAG, "Failed to authenticate with backend")
            return
        }

        // Step 2: Store auth tokens + simulate signed in IMMEDIATELY
        // CRITICAL: Must complete before SplashViewModel's 2-second check.
        // At ~T=0.8s this is well within the window.
        fakeGoogleAuthClient.simulateSignedIn()
        runBlocking {
            userPreferencesDataStore.saveAuthTokens(
                accessToken = authResult.accessToken,
                refreshToken = "",
                expiresInSeconds = 3600,
                userId = authResult.userId
            )
            userPreferencesDataStore.saveOnboardingComplete(createTestPreferences())
        }
        Log.d(TAG, "Auth tokens stored at ~T=${System.currentTimeMillis()}ms — SplashVM will navigate to Home")

        // Step 3: Best-effort Room seeding — check if backend has a meal plan
        // If seeded before HomeVM loads (T=2s), HomeVM finds data and skips Gemini.
        // If not seeded in time (first-ever run with no plan), HomeVM generates
        // via a single Gemini call — no race condition, no SocketTimeout.
        if (!backendMealPlanGenerated) {
            val existingPlan = BackendTestHelper.getCurrentMealPlan(
                BACKEND_BASE_URL, authResult.accessToken
            )
            if (existingPlan != null) {
                backendMealPlanGenerated = true
                Log.i(TAG, "Backend has existing meal plan — seeding Room")
            } else {
                Log.i(TAG, "No meal plan on backend — HomeVM will generate on first load")
            }
        }
        seedMealPlanFromBackend(authResult.accessToken)
    }

    /**
     * Authenticates and marks user as onboarded WITHOUT generating a meal plan.
     * Use this for tests that don't need meal cards (e.g., Recipe Rules, Settings).
     */
    protected fun setUpAuthenticatedStateWithoutMealPlan() {
        val authResult = authenticateWithBackend()

        fakeGoogleAuthClient.simulateSignedIn()

        if (authResult != null) {
            runBlocking {
                userPreferencesDataStore.saveAuthTokens(
                    accessToken = authResult.accessToken,
                    refreshToken = "",
                    expiresInSeconds = 3600,
                    userId = authResult.userId
                )
                userPreferencesDataStore.saveOnboardingComplete(createTestPreferences())
            }
            Log.d(TAG, "Authenticated without meal plan: userId=${authResult.userId}")
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
     * Clears all family members from the backend.
     * Call this in test @Before methods to ensure clean family member state.
     */
    protected fun clearFamilyMembers() {
        val authToken = runBlocking { userPreferencesDataStore.accessToken.first() }
        if (authToken != null) {
            val membersDeleted = BackendTestHelper.clearAllFamilyMembers(
                BACKEND_BASE_URL, authToken
            )
            Log.d(TAG, "Cleared backend: $membersDeleted family members")
        } else {
            Log.w(TAG, "No auth token available — skipping family members cleanup")
        }
    }

    /**
     * Fetches the current meal plan from the backend API and inserts it into Room DB.
     */
    protected fun seedMealPlanFromBackend(authToken: String) {
        val mealPlanJson = BackendTestHelper.getCurrentMealPlan(BACKEND_BASE_URL, authToken)
        if (mealPlanJson == null) {
            Log.w(TAG, "No meal plan on backend to seed into Room")
            return
        }

        try {
            val planId = mealPlanJson.getString("id")
            val weekStartDate = mealPlanJson.getString("week_start_date")
            val weekEndDate = mealPlanJson.getString("week_end_date")
            val now = System.currentTimeMillis()

            val mealPlanEntity = MealPlanEntity(
                id = planId,
                weekStartDate = weekStartDate,
                weekEndDate = weekEndDate,
                createdAt = now,
                updatedAt = now,
                isSynced = true
            )

            val items = mutableListOf<MealPlanItemEntity>()
            val festivals = mutableListOf<MealPlanFestivalEntity>()
            val daysArray = mealPlanJson.getJSONArray("days")

            for (d in 0 until daysArray.length()) {
                val day = daysArray.getJSONObject(d)
                val date = day.getString("date")
                val dayName = day.getString("day_name")
                val meals = day.getJSONObject("meals")

                for (mealType in listOf("breakfast", "lunch", "dinner", "snacks")) {
                    if (!meals.has(mealType)) continue
                    val mealItems = meals.getJSONArray(mealType)
                    for (m in 0 until mealItems.length()) {
                        val item = mealItems.getJSONObject(m)
                        val tags = mutableListOf<String>()
                        if (item.has("dietary_tags")) {
                            val tagsArray = item.getJSONArray("dietary_tags")
                            for (t in 0 until tagsArray.length()) {
                                tags.add(tagsArray.getString(t))
                            }
                        }
                        items.add(
                            MealPlanItemEntity(
                                id = item.optString("id", UUID.randomUUID().toString()),
                                mealPlanId = planId,
                                date = date,
                                dayName = dayName,
                                mealType = mealType,
                                recipeId = item.optString("recipe_id", UUID.randomUUID().toString()),
                                recipeName = item.optString("recipe_name", "Unknown"),
                                recipeImageUrl = if (item.has("recipe_image_url") && !item.isNull("recipe_image_url")) item.getString("recipe_image_url") else null,
                                prepTimeMinutes = item.optInt("prep_time_minutes", 30),
                                calories = item.optInt("calories", 0),
                                dietaryTags = tags,
                                isLocked = item.optBoolean("is_locked", false),
                                order = item.optInt("order", m)
                            )
                        )
                    }
                }

                if (day.has("festival") && !day.isNull("festival")) {
                    val festival = day.getJSONObject("festival")
                    val suggestedDishes = mutableListOf<String>()
                    if (festival.has("suggested_dishes") && !festival.isNull("suggested_dishes")) {
                        val dishesArray = festival.getJSONArray("suggested_dishes")
                        for (i in 0 until dishesArray.length()) {
                            suggestedDishes.add(dishesArray.getString(i))
                        }
                    }
                    festivals.add(
                        MealPlanFestivalEntity(
                            id = festival.optString("id", UUID.randomUUID().toString()),
                            mealPlanId = planId,
                            date = date,
                            name = festival.getString("name"),
                            isFastingDay = festival.optBoolean("is_fasting_day", false),
                            suggestedDishes = suggestedDishes
                        )
                    )
                }
            }

            runBlocking {
                mealPlanDao.replaceMealPlan(mealPlanEntity, items, festivals)
            }
            Log.i(TAG, "Seeded meal plan into Room: $planId (${items.size} items, ${festivals.size} festivals)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to seed meal plan into Room: ${e.message}", e)
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

        // One-time flag: generate meal plan via Gemini only once across all tests
        private var backendMealPlanGenerated = false

        // Common timeout values
        const val SHORT_TIMEOUT = 2000L
        const val MEDIUM_TIMEOUT = 5000L
        const val LONG_TIMEOUT = 10000L

        // Animation durations
        const val ANIMATION_DURATION = 300L
        const val SPLASH_DURATION = 2500L
    }
}
