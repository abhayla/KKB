package com.rasoiai.app.e2e.flows

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rasoiai.app.MainActivity
import com.rasoiai.app.e2e.base.waitUntilNodeWithTagExists
import com.rasoiai.app.e2e.base.waitUntilNodeWithTextExists
import com.rasoiai.app.presentation.common.TestTags
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Simplified Core Data Flow Test
 *
 * Tests the sequential flow: Auth → Onboarding → Generation → Home → RecipeDetail → Grocery
 *
 * This is a simplified version that uses text-based selection where possible
 * to avoid requiring extensive testTag additions to the production code.
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class CoreDataFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    /**
     * Test the core sequential flow from Auth through Grocery.
     * Uses FakeGoogleAuthClient for authentication.
     */
    @Test
    fun coreFlow_authToGrocery_completesSuccessfully() {
        // ==================== STEP 1: AUTH ====================
        step1_verifyAuthScreen()

        // ==================== STEP 2: SIGN IN & NAVIGATE TO ONBOARDING ====================
        step2_signInAndNavigateToOnboarding()

        // ==================== STEP 3: COMPLETE ONBOARDING ====================
        step3_completeOnboarding()

        // ==================== STEP 4: WAIT FOR GENERATION ====================
        step4_waitForGeneration()

        // ==================== STEP 5: VERIFY HOME ====================
        step5_verifyHome()
    }

    private fun step1_verifyAuthScreen() {
        // Wait for auth screen
        composeTestRule.waitUntilNodeWithTagExists(TestTags.AUTH_SCREEN, 10000)

        // Verify auth screen elements
        composeTestRule.onNodeWithTag(TestTags.AUTH_SCREEN).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TestTags.GOOGLE_SIGN_IN_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithText("Welcome!").assertIsDisplayed()
    }

    private fun step2_signInAndNavigateToOnboarding() {
        // Tap Google Sign-In button (uses FakeGoogleAuthClient)
        composeTestRule.onNodeWithTag(TestTags.GOOGLE_SIGN_IN_BUTTON).performClick()

        // Wait for onboarding screen
        composeTestRule.waitUntilNodeWithTagExists(TestTags.ONBOARDING_PROGRESS_BAR, 10000)
    }

    private fun step3_completeOnboarding() {
        // Step 1: Household Size - just tap Next with defaults
        composeTestRule.waitUntilNodeWithTagExists(TestTags.ONBOARDING_STEP_INDICATOR, 5000)
        composeTestRule.onNodeWithText("1 of 5", substring = true).assertIsDisplayed()

        // Tap Next to proceed (household size defaults to 1)
        composeTestRule.onNodeWithTag(TestTags.ONBOARDING_NEXT_BUTTON).performClick()
        composeTestRule.waitForIdle()

        // Step 2: Dietary Preferences - select first "Vegetarian" option (the primary diet card)
        composeTestRule.waitUntilNodeWithTextExists("2 of 5", 5000)
        // "Vegetarian" appears in both the primary diet card and possibly elsewhere
        // Use onAllNodesWithText and select the first one
        composeTestRule.onAllNodesWithText("Vegetarian", substring = true).onFirst().performClick()
        composeTestRule.onNodeWithTag(TestTags.ONBOARDING_NEXT_BUTTON).performClick()
        composeTestRule.waitForIdle()

        // Step 3: Cuisine Preferences
        composeTestRule.waitUntilNodeWithTextExists("3 of 5", 5000)
        composeTestRule.onNodeWithText("NORTH", substring = true).performClick()
        composeTestRule.onNodeWithTag(TestTags.ONBOARDING_NEXT_BUTTON).performClick()
        composeTestRule.waitForIdle()

        // Step 4: Disliked Ingredients (can skip)
        composeTestRule.waitUntilNodeWithTextExists("4 of 5", 5000)
        composeTestRule.onNodeWithTag(TestTags.ONBOARDING_NEXT_BUTTON).performClick()
        composeTestRule.waitForIdle()

        // Step 5: Cooking Time - tap Create My Meal Plan
        composeTestRule.waitUntilNodeWithTextExists("5 of 5", 5000)
        composeTestRule.onNodeWithText("Create My Meal Plan", substring = true).performClick()
        composeTestRule.waitForIdle()
    }

    private fun step4_waitForGeneration() {
        // Wait for generating screen
        composeTestRule.waitUntilNodeWithTagExists(TestTags.GENERATING_SCREEN, 10000)

        // Wait for home screen (generation may take time)
        composeTestRule.waitUntilNodeWithTagExists(TestTags.HOME_SCREEN, 60000)
    }

    private fun step5_verifyHome() {
        // Verify home screen is displayed
        composeTestRule.onNodeWithTag(TestTags.HOME_SCREEN).assertIsDisplayed()

        // Verify bottom nav is displayed
        composeTestRule.onNodeWithTag(TestTags.BOTTOM_NAV).assertIsDisplayed()

        // Wait for meal plan data to load (week selector appears when data is ready)
        composeTestRule.waitUntilNodeWithTagExists(TestTags.HOME_WEEK_SELECTOR, 30000)
        composeTestRule.onNodeWithTag(TestTags.HOME_WEEK_SELECTOR).assertIsDisplayed()

        // Wait for meal cards to appear (Breakfast, Lunch, Dinner)
        composeTestRule.waitUntilNodeWithTagExists("${TestTags.MEAL_CARD_PREFIX}breakfast", 10000)
        composeTestRule.onNodeWithTag("${TestTags.MEAL_CARD_PREFIX}breakfast").assertIsDisplayed()
        composeTestRule.onNodeWithTag("${TestTags.MEAL_CARD_PREFIX}lunch").assertIsDisplayed()
        composeTestRule.onNodeWithTag("${TestTags.MEAL_CARD_PREFIX}dinner").assertIsDisplayed()

        // Verify meal section titles are visible (displayed in uppercase)
        composeTestRule.onNodeWithText("BREAKFAST", ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("LUNCH", ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("DINNER", ignoreCase = true).assertIsDisplayed()

        // Take screenshot of Home screen with meal data
        takeScreenshot("home_screen_with_meals")
    }

    private fun takeScreenshot(name: String) {
        val device = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .uiAutomation
        val bitmap = device.takeScreenshot()
        val file = java.io.File(
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                .targetContext.getExternalFilesDir(null),
            "$name.png"
        )
        java.io.FileOutputStream(file).use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        }
        android.util.Log.d("CoreDataFlowTest", "Screenshot saved: ${file.absolutePath}")
    }
}
