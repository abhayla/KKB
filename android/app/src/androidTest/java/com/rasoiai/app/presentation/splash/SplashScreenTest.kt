package com.rasoiai.app.presentation.splash

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rasoiai.app.presentation.theme.RasoiAITheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Requirement: #37 - FR: Add UI tests for SplashScreen
 *
 * Tests the SplashScreen composable with various UI states.
 * Uses the internal SplashScreenContent composable for isolation from ViewModel.
 *
 * Covers:
 * - SPLASH-001: App logo and branding display
 * - SPLASH-002: Loading indicator display
 * - SPLASH-003: Offline banner visibility
 */
@RunWith(AndroidJUnit4::class)
class SplashScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // region SPLASH-001: Branding Display Tests

    @Test
    fun splashScreen_displaysAppName() {
        composeTestRule.setContent {
            RasoiAITheme {
                SplashScreenContent(isOnline = true)
            }
        }

        composeTestRule.onNodeWithText("RasoiAI").assertIsDisplayed()
    }

    @Test
    fun splashScreen_displaysTagline() {
        composeTestRule.setContent {
            RasoiAITheme {
                SplashScreenContent(isOnline = true)
            }
        }

        composeTestRule.onNodeWithText("AI Meal Planning for Indian Families").assertIsDisplayed()
    }

    // endregion

    // region SPLASH-002: Loading Indicator Tests

    @Test
    fun splashScreen_displaysLoadingIndicator() {
        composeTestRule.setContent {
            RasoiAITheme {
                SplashScreenContent(isOnline = true)
            }
        }

        composeTestRule.onNodeWithText("Loading\u2026").assertIsDisplayed()
    }

    // endregion

    // region SPLASH-003: Offline Banner Tests

    @Test
    fun splashScreen_hidesOfflineBanner_whenOnline() {
        composeTestRule.setContent {
            RasoiAITheme {
                SplashScreenContent(isOnline = true)
            }
        }

        composeTestRule.onNodeWithText("You're offline").assertDoesNotExist()
    }

    @Test
    fun splashScreen_showsOfflineBanner_whenOffline() {
        composeTestRule.setContent {
            RasoiAITheme {
                SplashScreenContent(isOnline = false)
            }
        }

        composeTestRule.onNodeWithText("You're offline").assertIsDisplayed()
    }

    // endregion

    // region Combined State Tests

    @Test
    fun splashScreen_displaysAllBrandingElements_whenOnline() {
        composeTestRule.setContent {
            RasoiAITheme {
                SplashScreenContent(isOnline = true)
            }
        }

        composeTestRule.onNodeWithText("RasoiAI").assertIsDisplayed()
        composeTestRule.onNodeWithText("AI Meal Planning for Indian Families").assertIsDisplayed()
        composeTestRule.onNodeWithText("Loading\u2026").assertIsDisplayed()
        composeTestRule.onNodeWithText("You're offline").assertDoesNotExist()
    }

    @Test
    fun splashScreen_displaysAllElements_whenOffline() {
        composeTestRule.setContent {
            RasoiAITheme {
                SplashScreenContent(isOnline = false)
            }
        }

        composeTestRule.onNodeWithText("RasoiAI").assertIsDisplayed()
        composeTestRule.onNodeWithText("AI Meal Planning for Indian Families").assertIsDisplayed()
        composeTestRule.onNodeWithText("Loading\u2026").assertIsDisplayed()
        composeTestRule.onNodeWithText("You're offline").assertIsDisplayed()
    }

    // endregion
}
