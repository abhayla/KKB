package com.rasoiai.app

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.test.runner.AndroidJUnitRunner
import com.rasoiai.app.e2e.util.BackendTestHelper
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Custom test runner for instrumented tests that use Hilt.
 * This runner should be configured in the app's build.gradle.kts:
 * testInstrumentationRunner = "com.rasoiai.app.HiltTestRunner"
 *
 * Pre-warms the backend auth token on startup so the first test doesn't
 * waste 2.6s on a remote DB round-trip (which exceeds the 2s Splash window).
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }

    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        // Pre-warm auth token before any test starts.
        // This caches the JWT in BaseE2ETest.cachedAuthToken so the first
        // test's setUpAuthenticatedState() completes in <0.1s (within the 2s Splash window).
        Thread {
            try {
                val result = BackendTestHelper.authenticateWithRetry(
                    baseUrl = "http://10.0.2.2:8000",
                    firebaseToken = "fake-firebase-token",
                    maxRetries = 2
                )
                if (result != null) {
                    PreWarmedAuth.token = result.accessToken
                    PreWarmedAuth.userId = result.userId
                    Log.i("HiltTestRunner", "Pre-warmed auth: userId=${result.userId}")
                }
            } catch (e: Exception) {
                Log.w("HiltTestRunner", "Pre-warm auth failed (non-fatal): ${e.message}")
            }
        }.apply { start(); join(5000) } // Wait up to 5s for pre-warm
    }
}

/** Holds pre-warmed auth credentials from the test runner. */
object PreWarmedAuth {
    @Volatile var token: String? = null
    @Volatile var userId: String? = null
}
