package com.rasoiai.app.e2e.util

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit
import kotlin.math.min

/**
 * Helper utilities for backend interactions in E2E tests.
 * Provides health checks, retry mechanisms, and connection management
 * for tests that depend on the backend API.
 *
 * ## Usage
 * ```kotlin
 * // Check if backend is healthy
 * if (BackendTestHelper.isBackendHealthy(BACKEND_URL)) {
 *     // proceed with test
 * }
 *
 * // Wait for backend to be ready
 * BackendTestHelper.waitForBackendReady(BACKEND_URL, timeoutSeconds = 60)
 *
 * // Retry a backend call
 * val result = BackendTestHelper.retryBackendCall(maxRetries = 3) {
 *     makeApiCall()
 * }
 * ```
 */
object BackendTestHelper {

    @PublishedApi
    internal const val TAG = "BackendTestHelper"

    // Default timeout settings
    private const val DEFAULT_CONNECT_TIMEOUT_SECONDS = 5L
    private const val DEFAULT_READ_TIMEOUT_SECONDS = 10L
    private const val DEFAULT_WRITE_TIMEOUT_SECONDS = 10L

    // Retry settings - @PublishedApi allows access from inline functions
    @PublishedApi
    internal const val DEFAULT_MAX_RETRIES = 3
    @PublishedApi
    internal const val DEFAULT_INITIAL_DELAY_MS = 500L
    @PublishedApi
    internal const val DEFAULT_MAX_DELAY_MS = 5000L
    @PublishedApi
    internal const val DEFAULT_BACKOFF_MULTIPLIER = 2.0

    /**
     * Creates a configured OkHttpClient for backend calls.
     */
    private fun createClient(
        connectTimeoutSeconds: Long = DEFAULT_CONNECT_TIMEOUT_SECONDS,
        readTimeoutSeconds: Long = DEFAULT_READ_TIMEOUT_SECONDS,
        writeTimeoutSeconds: Long = DEFAULT_WRITE_TIMEOUT_SECONDS
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(writeTimeoutSeconds, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Checks if the backend health endpoint is responding.
     *
     * @param baseUrl The base URL of the backend (e.g., "http://10.0.2.2:8000")
     * @return true if the backend is healthy, false otherwise
     */
    fun isBackendHealthy(baseUrl: String): Boolean {
        return try {
            val client = createClient(connectTimeoutSeconds = 3, readTimeoutSeconds = 3)
            val request = Request.Builder()
                .url("$baseUrl/health")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful.also {
                    Log.d(TAG, "Health check: ${response.code} (healthy=$it)")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Health check failed: ${e.message}")
            false
        }
    }

    /**
     * Waits for the backend to become ready by polling the health endpoint.
     *
     * @param baseUrl The base URL of the backend
     * @param timeoutSeconds Maximum time to wait
     * @param pollIntervalMs How often to check (with exponential backoff)
     * @return true if backend became ready, false if timeout exceeded
     */
    fun waitForBackendReady(
        baseUrl: String,
        timeoutSeconds: Long = 60,
        pollIntervalMs: Long = 1000
    ): Boolean {
        val startTime = System.currentTimeMillis()
        val timeoutMs = timeoutSeconds * 1000
        var currentInterval = pollIntervalMs

        Log.i(TAG, "Waiting for backend at $baseUrl (timeout: ${timeoutSeconds}s)")

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (isBackendHealthy(baseUrl)) {
                val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0
                Log.i(TAG, "Backend ready after ${String.format("%.1f", elapsedSeconds)}s")
                return true
            }

            Thread.sleep(currentInterval)

            // Exponential backoff, capped at 5 seconds
            currentInterval = min(currentInterval * 2, 5000)
        }

        Log.e(TAG, "Backend not ready after ${timeoutSeconds}s timeout")
        return false
    }

    /**
     * Retries a backend call with exponential backoff.
     *
     * @param maxRetries Maximum number of retry attempts
     * @param initialDelayMs Initial delay before first retry
     * @param maxDelayMs Maximum delay cap
     * @param backoffMultiplier Multiplier for each subsequent delay
     * @param call The backend call to execute
     * @return The result of the call, or null if all retries fail
     */
    inline fun <T> retryBackendCall(
        maxRetries: Int = DEFAULT_MAX_RETRIES,
        initialDelayMs: Long = DEFAULT_INITIAL_DELAY_MS,
        maxDelayMs: Long = DEFAULT_MAX_DELAY_MS,
        backoffMultiplier: Double = DEFAULT_BACKOFF_MULTIPLIER,
        call: () -> T?
    ): T? {
        var lastException: Exception? = null
        var currentDelay = initialDelayMs

        for (attempt in 1..(maxRetries + 1)) {
            try {
                val result = call()
                if (result != null) {
                    if (attempt > 1) {
                        Log.i(TAG, "Backend call succeeded on attempt $attempt")
                    }
                    return result
                }

                // Null result but no exception - treat as retriable
                Log.w(TAG, "Backend call returned null on attempt $attempt")

            } catch (e: Exception) {
                lastException = e

                if (!isRetriableException(e)) {
                    Log.e(TAG, "Non-retriable exception on attempt $attempt", e)
                    throw e
                }

                Log.w(TAG, "Backend call attempt $attempt failed: ${e.message}")
            }

            // Check if we have retries left
            if (attempt > maxRetries) {
                Log.e(TAG, "Backend call failed after $maxRetries retries")
                break
            }

            Log.d(TAG, "Retrying in ${currentDelay}ms...")
            Thread.sleep(currentDelay)

            // Calculate next delay with exponential backoff
            currentDelay = min((currentDelay * backoffMultiplier).toLong(), maxDelayMs)
        }

        // If we had an exception, rethrow it; otherwise return null
        lastException?.let { throw it }
        return null
    }

    /**
     * Determines if an exception should trigger a retry for backend calls.
     */
    fun isRetriableException(e: Exception): Boolean {
        return when (e) {
            is java.net.SocketTimeoutException -> true
            is java.net.ConnectException -> true
            is java.net.UnknownHostException -> true
            is java.io.IOException -> {
                // Check for common retriable I/O issues
                val message = e.message?.lowercase() ?: ""
                message.contains("timeout") ||
                message.contains("connection reset") ||
                message.contains("broken pipe") ||
                message.contains("connection refused")
            }
            else -> false
        }
    }

    /**
     * Result of an authentication attempt.
     */
    data class AuthResult(
        val accessToken: String,
        val userId: String
    )

    /**
     * Authenticates with the backend using a Firebase token (or fake token).
     * Uses retry logic for resilience against transient network issues.
     *
     * @param baseUrl The base URL of the backend
     * @param firebaseToken The Firebase token to exchange (or "fake-firebase-token" for testing)
     * @param maxRetries Maximum retry attempts
     * @return AuthResult if successful, null otherwise
     */
    fun authenticateWithRetry(
        baseUrl: String,
        firebaseToken: String,
        maxRetries: Int = DEFAULT_MAX_RETRIES
    ): AuthResult? {
        return retryBackendCall(maxRetries = maxRetries) {
            authenticateOnce(baseUrl, firebaseToken)
        }
    }

    /**
     * Single authentication attempt (no retry).
     */
    private fun authenticateOnce(baseUrl: String, firebaseToken: String): AuthResult? {
        val client = createClient()

        val requestBody = JSONObject()
            .put("firebase_token", firebaseToken)
            .toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$baseUrl/api/v1/auth/firebase")
            .post(requestBody)
            .build()

        Log.d(TAG, "Auth request to: $baseUrl/api/v1/auth/firebase")

        return client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: return@use null
                val json = JSONObject(responseBody)
                val accessToken = json.getString("access_token")
                val userJson = json.getJSONObject("user")
                val userId = userJson.getString("id")

                Log.d(TAG, "Auth successful: userId=$userId")
                AuthResult(accessToken, userId)
            } else {
                Log.w(TAG, "Auth failed: ${response.code} ${response.message}")
                null
            }
        }
    }

    /**
     * Makes a generic GET request to the backend with retry.
     *
     * @param baseUrl The base URL of the backend
     * @param path The API path (e.g., "/api/v1/users/me")
     * @param authToken Optional authorization token
     * @param maxRetries Maximum retry attempts
     * @return Response body as String, or null if failed
     */
    fun getWithRetry(
        baseUrl: String,
        path: String,
        authToken: String? = null,
        maxRetries: Int = DEFAULT_MAX_RETRIES
    ): String? {
        return retryBackendCall(maxRetries = maxRetries) {
            val client = createClient()

            val requestBuilder = Request.Builder()
                .url("$baseUrl$path")
                .get()

            authToken?.let {
                requestBuilder.addHeader("Authorization", "Bearer $it")
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    Log.w(TAG, "GET $path failed: ${response.code}")
                    null
                }
            }
        }
    }

    /**
     * Generates a meal plan for the authenticated user.
     * This is a synchronous operation that typically takes 4-7 seconds.
     *
     * @param baseUrl The base URL of the backend
     * @param authToken JWT Bearer token for authentication
     * @param weekStartDate Optional week start date (defaults to next Monday, format: yyyy-MM-dd)
     * @param maxRetries Maximum retry attempts
     * @return true if meal plan was generated successfully, false otherwise
     */
    fun generateMealPlan(
        baseUrl: String,
        authToken: String,
        weekStartDate: String? = null,
        maxRetries: Int = 1
    ): Boolean {
        Log.i(TAG, "Generating meal plan (this may take 4-7 seconds)...")

        return retryBackendCall(maxRetries = maxRetries) {
            generateMealPlanOnce(baseUrl, authToken, weekStartDate)
        } ?: false
    }

    /**
     * Single meal plan generation attempt (no retry).
     * Returns true on success, null on retriable failure (for use with retryBackendCall).
     */
    private fun generateMealPlanOnce(
        baseUrl: String,
        authToken: String,
        weekStartDate: String?
    ): Boolean? {
        // Gemini AI generation can take 45-90s on cold start.
        // Must be longer than Gemini's processing time to avoid orphaned requests
        // that block uvicorn's single-threaded event loop.
        val client = createClient(
            connectTimeoutSeconds = 10,
            readTimeoutSeconds = 90,
            writeTimeoutSeconds = 10
        )

        val startDate = weekStartDate ?: getNextMonday()
        val requestBody = JSONObject()
            .put("week_start_date", startDate)
            .toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$baseUrl/api/v1/meal-plans/generate")
            .addHeader("Authorization", "Bearer $authToken")
            .post(requestBody)
            .build()

        Log.d(TAG, "Meal plan request: POST $baseUrl/api/v1/meal-plans/generate (week_start_date=$startDate)")

        val startTime = System.currentTimeMillis()
        return client.newCall(request).execute().use { response ->
            val elapsed = System.currentTimeMillis() - startTime
            if (response.isSuccessful) {
                Log.i(TAG, "Meal plan generated successfully in ${elapsed}ms")
                true
            } else {
                val errorBody = response.body?.string() ?: "no body"
                Log.w(TAG, "Meal plan generation failed: ${response.code} ${response.message} - $errorBody")
                null  // Return null to trigger retry
            }
        }
    }

    /**
     * Gets the date of the next Monday (or today if today is Monday).
     * Returns date in yyyy-MM-dd format.
     */
    private fun getNextMonday(): String {
        val today = LocalDate.now()
        val nextMonday = if (today.dayOfWeek == DayOfWeek.MONDAY) {
            today
        } else {
            today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
        }
        return nextMonday.toString()  // Returns "yyyy-MM-dd" format
    }

    /**
     * Retrieves the current week's meal plan from the backend WITHOUT triggering Gemini.
     * Uses the GET /api/v1/meal-plans/current endpoint.
     *
     * @param baseUrl The base URL of the backend
     * @param authToken JWT Bearer token for authentication
     * @return JSONObject containing the meal plan, or null if none exists
     */
    fun getCurrentMealPlan(baseUrl: String, authToken: String): JSONObject? {
        Log.d(TAG, "Fetching current meal plan from: $baseUrl/api/v1/meal-plans/current")

        return try {
            retryBackendCall(maxRetries = 2) {
                val client = createClient(readTimeoutSeconds = 15)

                val request = Request.Builder()
                    .url("$baseUrl/api/v1/meal-plans/current")
                    .addHeader("Authorization", "Bearer $authToken")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string() ?: return@use null
                        Log.d(TAG, "getCurrentMealPlan: got response (${responseBody.length} chars)")
                        JSONObject(responseBody)
                    } else if (response.code == 404) {
                        Log.d(TAG, "getCurrentMealPlan: no meal plan found (404)")
                        null
                    } else {
                        Log.w(TAG, "getCurrentMealPlan failed: ${response.code} ${response.message}")
                        null
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "getCurrentMealPlan exception: ${e.message}")
            null
        }
    }

    /**
     * Retrieves the current user's profile and preferences from the backend.
     * Uses the /api/v1/users/me endpoint.
     *
     * @param baseUrl The base URL of the backend
     * @param authToken JWT Bearer token for authentication
     * @return JSONObject containing user data, or null if failed
     */
    fun getCurrentUser(baseUrl: String, authToken: String): JSONObject? {
        Log.d(TAG, "Fetching current user from: $baseUrl/api/v1/users/me")

        return retryBackendCall(maxRetries = 3) {
            val client = createClient()

            val request = Request.Builder()
                .url("$baseUrl/api/v1/users/me")
                .addHeader("Authorization", "Bearer $authToken")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: return@use null
                    Log.d(TAG, "getCurrentUser response: $responseBody")
                    JSONObject(responseBody)
                } else {
                    Log.w(TAG, "getCurrentUser failed: ${response.code} ${response.message}")
                    null
                }
            }
        }
    }

    /**
     * Updates user preferences on the backend.
     * Uses the PUT /api/v1/users/preferences endpoint.
     *
     * @param baseUrl The base URL of the backend
     * @param authToken JWT Bearer token for authentication
     * @param preferences JSONObject containing the preferences to update
     * @return true if update was successful, false otherwise
     */
    fun updateUserPreferences(
        baseUrl: String,
        authToken: String,
        preferences: JSONObject
    ): Boolean {
        Log.d(TAG, "Updating user preferences at: $baseUrl/api/v1/users/preferences")

        return retryBackendCall(maxRetries = 3) {
            val client = createClient()

            val requestBody = preferences.toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$baseUrl/api/v1/users/preferences")
                .addHeader("Authorization", "Bearer $authToken")
                .put(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "updateUserPreferences successful")
                    true
                } else {
                    Log.w(TAG, "updateUserPreferences failed: ${response.code} ${response.message}")
                    null  // Return null to trigger retry
                }
            }
        } ?: false
    }

    /**
     * Retrieves the current user's recipe rules from the backend.
     * Uses the GET /api/v1/recipe-rules endpoint.
     *
     * @param baseUrl The base URL of the backend
     * @param authToken JWT Bearer token for authentication
     * @return JSONObject containing recipe rules data, or null if failed
     */
    fun getRecipeRules(baseUrl: String, authToken: String): JSONObject? {
        Log.d(TAG, "Fetching recipe rules from: $baseUrl/api/v1/recipe-rules")

        return retryBackendCall(maxRetries = 3) {
            val client = createClient()

            val request = Request.Builder()
                .url("$baseUrl/api/v1/recipe-rules")
                .addHeader("Authorization", "Bearer $authToken")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: return@use null
                    Log.d(TAG, "getRecipeRules response: $responseBody")
                    JSONObject(responseBody)
                } else {
                    Log.w(TAG, "getRecipeRules failed: ${response.code} ${response.message}")
                    null
                }
            }
        }
    }

    /**
     * Retrieves the current user's nutrition goals from the backend.
     * Uses the GET /api/v1/nutrition-goals endpoint.
     *
     * @param baseUrl The base URL of the backend
     * @param authToken JWT Bearer token for authentication
     * @return JSONObject containing nutrition goals data, or null if failed
     */
    fun getNutritionGoals(baseUrl: String, authToken: String): JSONObject? {
        Log.d(TAG, "Fetching nutrition goals from: $baseUrl/api/v1/nutrition-goals")

        return retryBackendCall(maxRetries = 3) {
            val client = createClient()

            val request = Request.Builder()
                .url("$baseUrl/api/v1/nutrition-goals")
                .addHeader("Authorization", "Bearer $authToken")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: return@use null
                    Log.d(TAG, "getNutritionGoals response: $responseBody")
                    JSONObject(responseBody)
                } else {
                    Log.w(TAG, "getNutritionGoals failed: ${response.code} ${response.message}")
                    null
                }
            }
        }
    }

    /**
     * Deletes a single recipe rule from the backend.
     *
     * @param baseUrl The base URL of the backend
     * @param authToken JWT Bearer token for authentication
     * @param ruleId The ID of the recipe rule to delete
     * @return true if deletion was successful (204), false otherwise
     */
    fun deleteRecipeRule(baseUrl: String, authToken: String, ruleId: String): Boolean {
        return retryBackendCall(maxRetries = 2) {
            val client = createClient()
            val request = Request.Builder()
                .url("$baseUrl/api/v1/recipe-rules/$ruleId")
                .addHeader("Authorization", "Bearer $authToken")
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 204 || response.isSuccessful) {
                    Log.d(TAG, "Deleted recipe rule: $ruleId")
                    true
                } else {
                    Log.w(TAG, "Delete recipe rule $ruleId failed: ${response.code}")
                    null
                }
            }
        } ?: false
    }

    /**
     * Deletes a single nutrition goal from the backend.
     *
     * @param baseUrl The base URL of the backend
     * @param authToken JWT Bearer token for authentication
     * @param goalId The ID of the nutrition goal to delete
     * @return true if deletion was successful (204), false otherwise
     */
    fun deleteNutritionGoal(baseUrl: String, authToken: String, goalId: String): Boolean {
        return retryBackendCall(maxRetries = 2) {
            val client = createClient()
            val request = Request.Builder()
                .url("$baseUrl/api/v1/nutrition-goals/$goalId")
                .addHeader("Authorization", "Bearer $authToken")
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 204 || response.isSuccessful) {
                    Log.d(TAG, "Deleted nutrition goal: $goalId")
                    true
                } else {
                    Log.w(TAG, "Delete nutrition goal $goalId failed: ${response.code}")
                    null
                }
            }
        } ?: false
    }

    /**
     * Clears all recipe rules and nutrition goals from the backend.
     * Fetches all rules/goals via GET, then deletes each one individually.
     *
     * @param baseUrl The base URL of the backend
     * @param authToken JWT Bearer token for authentication
     * @return Pair of (rulesDeleted, goalsDeleted) counts
     */
    fun clearAllRecipeRulesAndGoals(baseUrl: String, authToken: String): Pair<Int, Int> {
        var rulesDeleted = 0
        var goalsDeleted = 0

        // Delete all recipe rules
        try {
            val rulesResponse = getRecipeRules(baseUrl, authToken)
            if (rulesResponse != null) {
                val rulesArray = rulesResponse.getJSONArray("rules")
                for (i in 0 until rulesArray.length()) {
                    val rule = rulesArray.getJSONObject(i)
                    val ruleId = rule.getString("id")
                    if (deleteRecipeRule(baseUrl, authToken, ruleId)) {
                        rulesDeleted++
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error clearing recipe rules: ${e.message}")
        }

        // Delete all nutrition goals
        try {
            val goalsResponse = getNutritionGoals(baseUrl, authToken)
            if (goalsResponse != null) {
                val goalsArray = goalsResponse.getJSONArray("goals")
                for (i in 0 until goalsArray.length()) {
                    val goal = goalsArray.getJSONObject(i)
                    val goalId = goal.getString("id")
                    if (deleteNutritionGoal(baseUrl, authToken, goalId)) {
                        goalsDeleted++
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error clearing nutrition goals: ${e.message}")
        }

        Log.i(TAG, "Cleared $rulesDeleted recipe rules and $goalsDeleted nutrition goals from backend")
        return Pair(rulesDeleted, goalsDeleted)
    }

    /**
     * Generates a meal plan and returns the full JSON response.
     * Unlike [generateMealPlan] which returns Boolean, this captures the entire
     * MealPlanResponse for AI verification testing.
     *
     * @param baseUrl The base URL of the backend
     * @param authToken JWT Bearer token for authentication
     * @param weekStartDate Optional week start date (defaults to next Monday)
     * @return JSONObject containing the full MealPlanResponse, or null if failed
     */
    fun generateMealPlanWithResponse(
        baseUrl: String,
        authToken: String,
        weekStartDate: String? = null
    ): JSONObject? {
        Log.i(TAG, "Generating meal plan with full response capture...")

        val client = createClient(
            connectTimeoutSeconds = 10,
            readTimeoutSeconds = 90,
            writeTimeoutSeconds = 10
        )

        val startDate = weekStartDate ?: getNextMonday()
        val requestBody = JSONObject()
            .put("week_start_date", startDate)
            .toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$baseUrl/api/v1/meal-plans/generate")
            .addHeader("Authorization", "Bearer $authToken")
            .post(requestBody)
            .build()

        Log.d(TAG, "Meal plan request: POST $baseUrl/api/v1/meal-plans/generate (week_start_date=$startDate)")

        val startTime = System.currentTimeMillis()
        return try {
            client.newCall(request).execute().use { response ->
                val elapsed = System.currentTimeMillis() - startTime
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@use null
                    Log.i(TAG, "Meal plan generated in ${elapsed}ms (${body.length} chars)")
                    JSONObject(body)
                } else {
                    val errorBody = response.body?.string() ?: "no body"
                    Log.w(TAG, "Meal plan generation failed in ${elapsed}ms: ${response.code} - $errorBody")
                    null
                }
            }
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            Log.e(TAG, "Meal plan generation exception after ${elapsed}ms: ${e.message}")
            null
        }
    }

    /**
     * Retrieves the current user's family members from the backend.
     *
     * @param baseUrl The base URL of the backend
     * @param authToken JWT Bearer token for authentication
     * @return JSONObject containing { members: [...], total_count: int }, or null if failed
     */
    fun getFamilyMembers(baseUrl: String, authToken: String): JSONObject? {
        Log.d(TAG, "Fetching family members from: $baseUrl/api/v1/family-members")

        return retryBackendCall(maxRetries = 3) {
            val client = createClient()

            val request = Request.Builder()
                .url("$baseUrl/api/v1/family-members")
                .addHeader("Authorization", "Bearer $authToken")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: return@use null
                    Log.d(TAG, "getFamilyMembers response: $responseBody")
                    JSONObject(responseBody)
                } else {
                    Log.w(TAG, "getFamilyMembers failed: ${response.code} ${response.message}")
                    null
                }
            }
        }
    }

    /**
     * Creates a recipe rule on the backend.
     *
     * @param baseUrl The base URL of the backend
     * @param authToken JWT Bearer token for authentication
     * @param rule JSONObject matching RecipeRuleCreate schema
     * @return JSONObject containing the created rule, or null if failed
     */
    fun createRecipeRule(baseUrl: String, authToken: String, rule: JSONObject): JSONObject? {
        Log.d(TAG, "Creating recipe rule: ${rule.optString("target_name")} (${rule.optString("action")})")

        return retryBackendCall(maxRetries = 2) {
            val client = createClient()

            val requestBody = rule.toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$baseUrl/api/v1/recipe-rules")
                .addHeader("Authorization", "Bearer $authToken")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: return@use null
                    Log.d(TAG, "createRecipeRule success: $responseBody")
                    JSONObject(responseBody)
                } else {
                    val errorBody = response.body?.string() ?: "no body"
                    Log.w(TAG, "createRecipeRule failed: ${response.code} - $errorBody")
                    null
                }
            }
        }
    }

    /**
     * Creates a family member on the backend.
     *
     * @param baseUrl The base URL of the backend
     * @param authToken JWT Bearer token for authentication
     * @param member JSONObject matching FamilyMemberCreate schema
     * @return JSONObject containing the created member, or null if failed
     */
    fun createFamilyMember(baseUrl: String, authToken: String, member: JSONObject): JSONObject? {
        Log.d(TAG, "Creating family member: ${member.optString("name")}")

        return retryBackendCall(maxRetries = 2) {
            val client = createClient()

            val requestBody = member.toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$baseUrl/api/v1/family-members")
                .addHeader("Authorization", "Bearer $authToken")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: return@use null
                    Log.d(TAG, "createFamilyMember success: $responseBody")
                    JSONObject(responseBody)
                } else {
                    val errorBody = response.body?.string() ?: "no body"
                    Log.w(TAG, "createFamilyMember failed: ${response.code} - $errorBody")
                    null
                }
            }
        }
    }

    /**
     * Deletes a single family member from the backend.
     *
     * @param baseUrl The base URL of the backend
     * @param authToken JWT Bearer token for authentication
     * @param memberId The ID of the family member to delete
     * @return true if deletion was successful (204), false otherwise
     */
    fun deleteFamilyMember(baseUrl: String, authToken: String, memberId: String): Boolean {
        return retryBackendCall(maxRetries = 2) {
            val client = createClient()
            val request = Request.Builder()
                .url("$baseUrl/api/v1/family-members/$memberId")
                .addHeader("Authorization", "Bearer $authToken")
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 204 || response.isSuccessful) {
                    Log.d(TAG, "Deleted family member: $memberId")
                    true
                } else {
                    Log.w(TAG, "Delete family member $memberId failed: ${response.code}")
                    null
                }
            }
        } ?: false
    }

    /**
     * Clears all family members from the backend.
     * Fetches all members via GET, then deletes each one individually.
     *
     * @param baseUrl The base URL of the backend
     * @param authToken JWT Bearer token for authentication
     * @return Number of members deleted
     */
    fun clearAllFamilyMembers(baseUrl: String, authToken: String): Int {
        var membersDeleted = 0

        try {
            val membersResponse = getFamilyMembers(baseUrl, authToken)
            if (membersResponse != null) {
                val membersArray = membersResponse.getJSONArray("members")
                for (i in 0 until membersArray.length()) {
                    val member = membersArray.getJSONObject(i)
                    val memberId = member.getString("id")
                    if (deleteFamilyMember(baseUrl, authToken, memberId)) {
                        membersDeleted++
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error clearing family members: ${e.message}")
        }

        Log.i(TAG, "Cleared $membersDeleted family members from backend")
        return membersDeleted
    }

    /**
     * Makes a generic POST request to the backend with retry.
     *
     * @param baseUrl The base URL of the backend
     * @param path The API path (e.g., "/api/v1/households")
     * @param body JSONObject for the request body
     * @param authToken Optional authorization token
     * @param maxRetries Maximum retry attempts
     * @return Response body as String, or null if failed
     */
    fun postWithRetry(
        baseUrl: String,
        path: String,
        body: JSONObject,
        authToken: String? = null,
        maxRetries: Int = DEFAULT_MAX_RETRIES
    ): String? {
        return retryBackendCall(maxRetries = maxRetries) {
            val client = createClient()

            val requestBody = body.toString()
                .toRequestBody("application/json".toMediaType())

            val requestBuilder = Request.Builder()
                .url("$baseUrl$path")
                .post(requestBody)

            authToken?.let {
                requestBuilder.addHeader("Authorization", "Bearer $it")
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    Log.w(TAG, "POST $path failed: ${response.code}")
                    null
                }
            }
        }
    }

    /**
     * Makes a generic DELETE request to the backend.
     *
     * @param baseUrl The base URL of the backend
     * @param path The API path
     * @param authToken Authorization token
     * @return true if successful (2xx), false otherwise
     */
    fun deleteWithRetry(
        baseUrl: String,
        path: String,
        authToken: String,
        maxRetries: Int = DEFAULT_MAX_RETRIES
    ): Boolean {
        return retryBackendCall(maxRetries = maxRetries) {
            val client = createClient()

            val request = Request.Builder()
                .url("$baseUrl$path")
                .addHeader("Authorization", "Bearer $authToken")
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 204) {
                    true
                } else {
                    Log.w(TAG, "DELETE $path failed: ${response.code}")
                    null
                }
            }
        } ?: false
    }

    /**
     * Creates a household via the backend API.
     *
     * @param baseUrl The base URL of the backend
     * @param authToken JWT Bearer token
     * @param name Household name
     * @return JSONObject containing the created household, or null if failed
     */
    fun createHousehold(baseUrl: String, authToken: String, name: String): JSONObject? {
        Log.d(TAG, "Creating household: $name")
        val body = JSONObject().put("name", name)
        val response = postWithRetry(baseUrl, "/api/v1/households", body, authToken)
        return response?.let {
            try {
                JSONObject(it).also { json ->
                    Log.d(TAG, "createHousehold success: id=${json.optString("id")}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "createHousehold parse error: ${e.message}")
                null
            }
        }
    }

    /**
     * Gets the current user's household.
     *
     * @param baseUrl The base URL of the backend
     * @param authToken JWT Bearer token
     * @return JSONObject containing household data, or null if user has no household
     */
    fun getMyHousehold(baseUrl: String, authToken: String): JSONObject? {
        Log.d(TAG, "Fetching user's household from: $baseUrl/api/v1/households/me")
        val response = getWithRetry(baseUrl, "/api/v1/households/me", authToken)
        return response?.let {
            try {
                JSONObject(it)
            } catch (e: Exception) {
                Log.w(TAG, "getMyHousehold parse error: ${e.message}")
                null
            }
        }
    }

    /**
     * Deactivates (soft-deletes) a household.
     *
     * @param baseUrl The base URL of the backend
     * @param authToken JWT Bearer token
     * @param householdId The household ID to deactivate
     * @return true if successful
     */
    fun deactivateHousehold(baseUrl: String, authToken: String, householdId: String): Boolean {
        Log.d(TAG, "Deactivating household: $householdId")
        return deleteWithRetry(baseUrl, "/api/v1/households/$householdId", authToken)
    }

    /**
     * Ensures the current user is a member of an active household.
     * Creates one if none exists.
     *
     * @param baseUrl The base URL of the backend
     * @param authToken JWT Bearer token
     * @param householdName Name for the household (if creating)
     * @return JSONObject containing household data, or null if setup failed
     */
    fun ensureHouseholdExists(
        baseUrl: String,
        authToken: String,
        householdName: String = "Test Household"
    ): JSONObject? {
        // Check if user already has a household
        val existing = getMyHousehold(baseUrl, authToken)
        if (existing != null && existing.optBoolean("is_active", true)) {
            Log.d(TAG, "User already has active household: ${existing.optString("id")}")
            return existing
        }

        // Create a new household
        return createHousehold(baseUrl, authToken, householdName)
    }

    /**
     * Lists members of a household.
     *
     * @param baseUrl The base URL of the backend
     * @param authToken JWT Bearer token
     * @param householdId The household ID
     * @return JSONArray of household members, or null if failed
     */
    fun getHouseholdMembers(
        baseUrl: String,
        authToken: String,
        householdId: String
    ): JSONArray? {
        Log.d(TAG, "Fetching household members for: $householdId")
        val response = getWithRetry(baseUrl, "/api/v1/households/$householdId/members", authToken)
        return response?.let {
            try {
                JSONArray(it)
            } catch (e: Exception) {
                Log.w(TAG, "getHouseholdMembers parse error: ${e.message}")
                null
            }
        }
    }

    /**
     * Adds a member to a household by phone number.
     *
     * @param baseUrl The base URL of the backend
     * @param authToken JWT Bearer token
     * @param householdId The household ID
     * @param phoneNumber Phone number of the member to add
     * @return JSONObject containing the created member, or null if failed
     */
    fun addHouseholdMember(
        baseUrl: String,
        authToken: String,
        householdId: String,
        phoneNumber: String
    ): JSONObject? {
        Log.d(TAG, "Adding household member with phone: $phoneNumber")
        val body = JSONObject().put("phone_number", phoneNumber)
        val response = postWithRetry(
            baseUrl, "/api/v1/households/$householdId/members", body, authToken
        )
        return response?.let {
            try {
                JSONObject(it).also { json ->
                    Log.d(TAG, "addHouseholdMember success: id=${json.optString("id")}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "addHouseholdMember parse error: ${e.message}")
                null
            }
        }
    }

    /**
     * Updates a household member (e.g., portion_size, role).
     *
     * @param baseUrl The base URL of the backend
     * @param authToken JWT Bearer token
     * @param householdId The household ID
     * @param memberId The member ID to update
     * @param updates JSONObject with update fields
     * @return JSONObject containing the updated member, or null if failed
     */
    fun updateHouseholdMember(
        baseUrl: String,
        authToken: String,
        householdId: String,
        memberId: String,
        updates: JSONObject
    ): JSONObject? {
        Log.d(TAG, "Updating household member $memberId with: $updates")
        return retryBackendCall(maxRetries = DEFAULT_MAX_RETRIES) {
            val client = createClient()
            val requestBody = updates.toString()
                .toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl/api/v1/households/$householdId/members/$memberId")
                .put(requestBody)
                .addHeader("Authorization", "Bearer $authToken")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()?.let { JSONObject(it) }
                } else {
                    Log.w(TAG, "PUT member $memberId failed: ${response.code}")
                    null
                }
            }
        }
    }

    /**
     * Removes a member from a household.
     *
     * @param baseUrl The base URL of the backend
     * @param authToken JWT Bearer token
     * @param householdId The household ID
     * @param memberId The member ID to remove
     * @return true if successful
     */
    fun removeHouseholdMember(
        baseUrl: String,
        authToken: String,
        householdId: String,
        memberId: String
    ): Boolean {
        Log.d(TAG, "Removing household member: $memberId")
        return deleteWithRetry(
            baseUrl, "/api/v1/households/$householdId/members/$memberId", authToken
        )
    }

    /**
     * Checks backend connectivity and logs diagnostic information.
     * Useful for debugging test setup issues.
     *
     * @param baseUrl The base URL of the backend
     * @return Diagnostic information as a string
     */
    fun diagnoseConnection(baseUrl: String): String {
        val sb = StringBuilder()
        sb.appendLine("╔════════════════════════════════════════╗")
        sb.appendLine("║      BACKEND CONNECTION DIAGNOSTIC     ║")
        sb.appendLine("╠════════════════════════════════════════╣")
        sb.appendLine("║ Base URL: $baseUrl")

        // Test health endpoint
        val healthStatus = try {
            val client = createClient(connectTimeoutSeconds = 3, readTimeoutSeconds = 3)
            val request = Request.Builder().url("$baseUrl/health").get().build()
            client.newCall(request).execute().use { response ->
                "${response.code} ${response.message}"
            }
        } catch (e: Exception) {
            "Error: ${e.javaClass.simpleName} - ${e.message}"
        }
        sb.appendLine("║ Health: $healthStatus")

        // Test auth endpoint (just check if reachable)
        val authStatus = try {
            val client = createClient(connectTimeoutSeconds = 3, readTimeoutSeconds = 3)
            val request = Request.Builder().url("$baseUrl/api/v1/auth/firebase").head().build()
            client.newCall(request).execute().use { response ->
                "${response.code} (reachable)"
            }
        } catch (e: Exception) {
            "Error: ${e.javaClass.simpleName} - ${e.message}"
        }
        sb.appendLine("║ Auth endpoint: $authStatus")

        sb.appendLine("╚════════════════════════════════════════╝")

        val result = sb.toString()
        Log.i(TAG, result)
        return result
    }
}
