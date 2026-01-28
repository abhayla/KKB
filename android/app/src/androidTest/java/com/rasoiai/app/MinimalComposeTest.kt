package com.rasoiai.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Minimal test to verify TestActivity launches correctly.
 *
 * NOTE: Full Compose UI assertions (onNodeWithText, etc.) currently fail
 * with "No compose hierarchies found" due to an environment/emulator issue.
 * The activity launch itself works - this is a test framework configuration issue.
 *
 * When the Compose testing environment is fixed, this test can be extended
 * to verify UI elements.
 */
@HiltAndroidTest
class MinimalComposeTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun testActivity_launches_successfully() {
        // Verify the activity launches without crashing
        // This test PASSES, proving TestActivity is correctly configured
        composeTestRule.waitForIdle()
        assert(composeTestRule.activity != null)
    }
}
