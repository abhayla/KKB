package com.rasoiai.app.e2e.base

import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.rasoiai.app.TestActivity
import com.rasoiai.app.e2e.di.FakeGoogleAuthClient
import com.rasoiai.app.e2e.di.FakeUserPreferencesDataStore
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import javax.inject.Inject

/**
 * Base class for E2E tests providing common setup and utilities.
 * All E2E flow tests should extend this class.
 *
 * ## Test Architecture
 * This test setup uses "Real Backend + Fake Google Auth Only":
 * - FakeGoogleAuthClient: Bypasses Google OAuth, returns "fake-firebase-token"
 * - FakeUserPreferencesDataStore: Controls navigation state (onboarded/not)
 * - All Repositories: REAL implementations calling real backend APIs
 *
 * ## Authentication Setup
 * Tests that need to start at Home screen should call [setUpAuthenticatedState]
 * in their @Before method after calling super.setUp().
 *
 * Example:
 * ```kotlin
 * @Before
 * override fun setUp() {
 *     super.setUp()
 *     setUpAuthenticatedState()  // Simulates logged-in, onboarded user
 *     // Now you can wait for Home screen
 * }
 * ```
 */
@HiltAndroidTest
abstract class BaseE2ETest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    @Inject
    lateinit var fakeGoogleAuthClient: FakeGoogleAuthClient

    @Inject
    lateinit var fakeUserPreferencesDataStore: FakeUserPreferencesDataStore

    protected val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    open fun setUp() {
        hiltRule.inject()
    }

    /**
     * Sets up a fully authenticated and onboarded user state.
     * Call this in tests that need to start at the Home screen.
     *
     * Note: This bypasses actual authentication. The SplashViewModel checks:
     * - googleAuthClient.isSignedIn (controlled by FakeGoogleAuthClient)
     * - userPreferencesDataStore.isOnboarded (controlled by FakeUserPreferencesDataStore)
     *
     * With both set to true, the app navigates directly to Home screen.
     */
    protected fun setUpAuthenticatedState() {
        fakeGoogleAuthClient.simulateSignedIn()
        fakeUserPreferencesDataStore.simulateOnboarded()
    }

    /**
     * Sets up an authenticated but NOT onboarded user state.
     * Call this in tests that need to test the Onboarding flow.
     */
    protected fun setUpNewUserState() {
        fakeGoogleAuthClient.setSignInSuccess()
        fakeUserPreferencesDataStore.simulateNewUser()
    }

    /**
     * Resets auth state to logged out.
     */
    protected fun resetAuthState() {
        fakeGoogleAuthClient.reset()
        fakeUserPreferencesDataStore.reset()
    }

    /**
     * Wait for a condition to be true with timeout.
     * Useful for async operations.
     */
    protected fun waitUntil(
        timeoutMillis: Long = 5000,
        conditionDescription: String = "condition",
        condition: () -> Boolean
    ) {
        val startTime = System.currentTimeMillis()
        while (!condition() && (System.currentTimeMillis() - startTime) < timeoutMillis) {
            Thread.sleep(100)
        }
        if (!condition()) {
            throw AssertionError("Timeout waiting for $conditionDescription after ${timeoutMillis}ms")
        }
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
        // Common timeout values
        const val SHORT_TIMEOUT = 2000L
        const val MEDIUM_TIMEOUT = 5000L
        const val LONG_TIMEOUT = 10000L

        // Animation durations
        const val ANIMATION_DURATION = 300L
        const val SPLASH_DURATION = 2500L
    }
}
