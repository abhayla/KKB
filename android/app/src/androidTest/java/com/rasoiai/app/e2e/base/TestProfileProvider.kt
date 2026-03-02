package com.rasoiai.app.e2e.base

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Reads the active test profile from instrumentation arguments.
 *
 * Usage:
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.profile=gupta_eggetarian
 * ```
 *
 * Defaults to "sharma_veg" when no profile argument is provided.
 */
object TestProfileProvider {

    private const val TAG = "TestProfileProvider"
    private const val ARG_PROFILE = "profile"
    private const val DEFAULT_PROFILE = "sharma_veg"

    val profileKey: String by lazy {
        val key = InstrumentationRegistry.getArguments().getString(ARG_PROFILE, DEFAULT_PROFILE)
        Log.i(TAG, "Active test profile: $key")
        key
    }

    val activeProfile: FamilyTestData by lazy {
        val profile = TestDataFactory.getProfile(profileKey)
        Log.i(TAG, "Loaded profile '$profileKey': ${profile.householdSize} members, ${profile.primaryDiet}")
        profile
    }
}
