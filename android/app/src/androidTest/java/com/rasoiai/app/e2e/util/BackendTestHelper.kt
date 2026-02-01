package com.rasoiai.app.e2e.util

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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
        maxRetries: Int = 2
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
        // Use longer timeout for meal generation (can take 4-7 seconds)
        val client = createClient(
            connectTimeoutSeconds = 10,
            readTimeoutSeconds = 30,  // Long timeout for AI generation
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
