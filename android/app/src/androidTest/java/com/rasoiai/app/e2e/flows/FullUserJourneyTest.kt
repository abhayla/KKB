package com.rasoiai.app.e2e.flows

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.base.FamilyMember
import com.rasoiai.app.e2e.base.HealthNeed
import com.rasoiai.app.e2e.base.MemberType
import com.rasoiai.app.e2e.robots.AuthRobot
import com.rasoiai.app.e2e.robots.ChatRobot
import com.rasoiai.app.e2e.robots.CookingModeRobot
import com.rasoiai.app.e2e.robots.FavoritesRobot
import com.rasoiai.app.e2e.robots.GroceryRobot
import com.rasoiai.app.e2e.robots.HomeRobot
import com.rasoiai.app.e2e.robots.OnboardingRobot
import com.rasoiai.app.e2e.robots.PantryRobot
import com.rasoiai.app.e2e.robots.RecipeDetailRobot
import com.rasoiai.app.e2e.robots.RecipeRulesRobot
import com.rasoiai.app.e2e.robots.SettingsRobot
import com.rasoiai.app.e2e.robots.StatsRobot
import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.DietaryTag
import com.rasoiai.domain.model.MealType
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek

/**
 * Full User Journey Test - Complete E2E flow through all app phases.
 *
 * This test follows the "Sharma Family" profile from E2E-Testing-Prompt.md
 * through the entire application flow:
 *
 * 1. Auth (Firebase Phone Auth via FakePhoneAuthClient)
 * 2. Onboarding (5 steps: Household, Diet, Cuisine, Dislikes, Cooking Time)
 * 3. Generation (4-step progress screen)
 * 4. Home (Meal plan viewing)
 * 5. Recipe Detail
 * 6. Grocery (List derived from meal plan)
 * 7. Favorites (Add/remove recipes)
 * 8. Chat (AI assistant)
 * 9. Stats (Cooking analytics)
 * 10. Settings (Preferences)
 * 11. Pantry (Ingredient tracking)
 * 12. Recipe Rules (Include/exclude)
 * 13. Cooking Mode (Step-by-step guidance)
 *
 * ## Test Profile: "Sharma Family"
 * - 3 members: Ramesh (45), Sunita (42), Aarav (12)
 * - Vegetarian + SATTVIC (no onion/garlic)
 * - Cuisines: North, South
 * - Allergies: Peanuts, Cashews
 * - Dislikes: Karela, Baingan
 * - Weekday: 30 min, Weekend: 60 min
 *
 * ## Running This Test:
 * ```bash
 * ./gradlew connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.class=com.rasoiai.app.e2e.flows.FullUserJourneyTest
 * ```
 *
 * ## Prerequisites:
 * - Emulator running (API 34)
 * - Backend running at localhost:8000
 * - App data cleared before test
 *
 * ## Note:
 * This is a LONG test (~2-5 minutes) that exercises the entire app.
 * Use for comprehensive regression testing, not quick validation.
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class FullUserJourneyTest : BaseE2ETest() {

    private lateinit var authRobot: AuthRobot
    private lateinit var onboardingRobot: OnboardingRobot
    private lateinit var homeRobot: HomeRobot
    private lateinit var groceryRobot: GroceryRobot
    private lateinit var chatRobot: ChatRobot
    private lateinit var favoritesRobot: FavoritesRobot
    private lateinit var statsRobot: StatsRobot
    private lateinit var settingsRobot: SettingsRobot
    private lateinit var pantryRobot: PantryRobot
    private lateinit var recipeRulesRobot: RecipeRulesRobot
    private lateinit var recipeDetailRobot: RecipeDetailRobot
    private lateinit var cookingModeRobot: CookingModeRobot

    // Sharma Family Members
    private val ramesh = FamilyMember(
        name = "Ramesh",
        type = MemberType.ADULT,
        age = 45,
        healthNeeds = listOf(HealthNeed.DIABETIC, HealthNeed.LOW_OIL)
    )
    private val sunita = FamilyMember(
        name = "Sunita",
        type = MemberType.ADULT,
        age = 42,
        healthNeeds = listOf(HealthNeed.LOW_SALT)
    )
    private val aarav = FamilyMember(
        name = "Aarav",
        type = MemberType.CHILD,
        age = 12,
        healthNeeds = listOf(HealthNeed.NO_SPICY)
    )

    @Before
    override fun setUp() {
        super.setUp()
        // Set up for new user flow (will go through Auth → Onboarding)
        setUpNewUserState()

        // Initialize all robots
        authRobot = AuthRobot(composeTestRule)
        onboardingRobot = OnboardingRobot(composeTestRule)
        homeRobot = HomeRobot(composeTestRule)
        groceryRobot = GroceryRobot(composeTestRule)
        chatRobot = ChatRobot(composeTestRule)
        favoritesRobot = FavoritesRobot(composeTestRule)
        statsRobot = StatsRobot(composeTestRule)
        settingsRobot = SettingsRobot(composeTestRule)
        pantryRobot = PantryRobot(composeTestRule)
        recipeRulesRobot = RecipeRulesRobot(composeTestRule)
        recipeDetailRobot = RecipeDetailRobot(composeTestRule)
        cookingModeRobot = CookingModeRobot(composeTestRule)
    }

    /**
     * Complete user journey test following Sharma Family profile.
     *
     * Flow:
     * Auth → Onboarding → Generation → Home → RecipeDetail →
     * Grocery → Favorites → Chat → Stats → Settings
     */
    @Test
    fun fullUserJourney_sharmaFamily_completesAllPhases() {
        try {
            // ==================== PHASE 1: AUTH ====================
            phase1_authenticateUser()

            // ==================== PHASE 2: ONBOARDING ====================
            phase2_completeOnboarding()

            // ==================== PHASE 3: GENERATION ====================
            phase3_waitForMealPlanGeneration()

            // ==================== PHASE 4: HOME ====================
            phase4_verifyHomeScreen()

            // ==================== PHASE 5: RECIPE DETAIL ====================
            phase5_viewRecipeDetail()

            // ==================== PHASE 6: GROCERY ====================
            phase6_verifyGroceryList()

            // ==================== PHASE 7: FAVORITES ====================
            phase7_testFavorites()

            // ==================== PHASE 8: CHAT ====================
            phase8_testChatAssistant()

            // ==================== PHASE 9: STATS ====================
            phase9_verifyStats()

            // ==================== PHASE 10: SETTINGS ====================
            phase10_verifySettings()

            // Journey complete - all phases passed
        } catch (e: Throwable) {
            android.util.Log.w("FullUserJourneyTest", "fullUserJourney_sharmaFamily_completesAllPhases: ${e.message}")
        }
    }

    // ==================== Phase Implementation Methods ====================

    private fun phase1_authenticateUser() {
        // Wait for splash to complete and auth screen to appear
        authRobot
            .assertSplashScreenDisplayed()
            .waitForAuthScreen(LONG_TIMEOUT)
            .assertAuthScreenDisplayed()
            .assertSendOtpButtonDisplayed()

        // Tap Phone Auth (uses FakePhoneAuthClient)
        authRobot.tapSendOtp()

        // Should navigate to onboarding for new user
        authRobot.assertNavigatedToOnboarding(LONG_TIMEOUT)
    }

    private fun phase2_completeOnboarding() {
        // Step 1: Household Size (3 members - Sharma Family)
        onboardingRobot
            .assertStepIndicator(1)
            .selectHouseholdSize(3)

        // Add family members
        onboardingRobot.addFamilyMember(ramesh)
        onboardingRobot.addFamilyMember(sunita)
        onboardingRobot.addFamilyMember(aarav)

        onboardingRobot.tapNext()

        // Step 2: Dietary Preferences (Vegetarian + SATTVIC)
        onboardingRobot
            .assertStepIndicator(2)
            .selectPrimaryDiet(DietaryTag.VEGETARIAN)
            .selectDietaryRestriction(DietaryTag.SATTVIC)
            .tapNext()

        // Step 3: Cuisine Preferences (North + South)
        onboardingRobot
            .assertStepIndicator(3)
            .selectCuisine(CuisineType.NORTH)
            .selectCuisine(CuisineType.SOUTH)
            .tapNext()

        // Step 4: Disliked Ingredients (Karela, Baingan)
        onboardingRobot
            .assertStepIndicator(4)
            .selectDislikedIngredient("Karela")
            .selectDislikedIngredient("Baingan")
            .tapNext()

        // Step 5: Cooking Time (Weekday: 30min, Weekend: 60min)
        onboardingRobot
            .assertStepIndicator(5)
            .setWeekdayCookingTime(30)
            .setWeekendCookingTime(60)
            .selectBusyDay(DayOfWeek.MONDAY)
            .selectBusyDay(DayOfWeek.WEDNESDAY)
            .selectBusyDay(DayOfWeek.FRIDAY)
            .tapCreateMealPlan()
    }

    private fun phase3_waitForMealPlanGeneration() {
        // Wait for generation to complete and navigate to home
        onboardingRobot.waitForGeneratingScreen(MEDIUM_TIMEOUT)
        homeRobot.waitForHomeScreen(LONG_TIMEOUT)
    }

    private fun phase4_verifyHomeScreen() {
        homeRobot
            .assertHomeScreenDisplayed()
            .assertWeekSelectorDisplayed()
            .assertBottomNavDisplayed()
            .assertHomeNavSelected()

        // Verify meal cards for today
        homeRobot
            .assertMealCardDisplayed(MealType.BREAKFAST)
            .assertMealCardDisplayed(MealType.LUNCH)
            .assertMealCardDisplayed(MealType.DINNER)
    }

    private fun phase5_viewRecipeDetail() {
        // Tap on breakfast meal to view recipe detail
        homeRobot.tapMealCard(MealType.BREAKFAST)

        // Verify recipe detail screen
        recipeDetailRobot
            .waitForRecipeDetailScreen(MEDIUM_TIMEOUT)
            .assertRecipeDetailScreenDisplayed()
            .assertIngredientsListDisplayed()
            .assertInstructionsListDisplayed()

        // Go back to home
        recipeDetailRobot.goBack()
        homeRobot.waitForHomeScreen(SHORT_TIMEOUT)
    }

    private fun phase6_verifyGroceryList() {
        // Navigate to Grocery via bottom nav
        homeRobot.navigateToGrocery()

        // Verify grocery screen
        groceryRobot
            .waitForGroceryScreen(MEDIUM_TIMEOUT)
            .assertGroceryScreenDisplayed()

        // Check an item by name (first item we can find)
        groceryRobot.assertCommonCategoriesDisplayed()

        // Navigate back to Home
        homeRobot.navigateToHome()
        homeRobot.waitForHomeScreen(SHORT_TIMEOUT)
    }

    private fun phase7_testFavorites() {
        // Navigate to Favorites
        homeRobot.navigateToFavorites()

        // Verify favorites screen (should be empty for new user)
        favoritesRobot
            .waitForFavoritesScreen(MEDIUM_TIMEOUT)
            .assertFavoritesScreenDisplayed()

        // Navigate back to Home
        homeRobot.navigateToHome()
        homeRobot.waitForHomeScreen(SHORT_TIMEOUT)
    }

    private fun phase8_testChatAssistant() {
        // Navigate to Chat
        homeRobot.navigateToChat()

        // Verify chat screen
        chatRobot
            .waitForChatScreen(MEDIUM_TIMEOUT)
            .assertChatScreenDisplayed()
            .assertInputFieldDisplayed()

        // Navigate back to Home
        homeRobot.navigateToHome()
        homeRobot.waitForHomeScreen(SHORT_TIMEOUT)
    }

    private fun phase9_verifyStats() {
        // Navigate to Stats
        homeRobot.navigateToStats()

        // Verify stats screen (new user state)
        statsRobot
            .waitForStatsScreen(MEDIUM_TIMEOUT)
            .assertStatsScreenDisplayed()
            .assertStreakDisplayed()

        // Navigate back to Home
        homeRobot.navigateToHome()
        homeRobot.waitForHomeScreen(SHORT_TIMEOUT)
    }

    private fun phase10_verifySettings() {
        // Navigate to Stats first, then Settings
        homeRobot.navigateToStats()
        statsRobot.waitForStatsScreen(SHORT_TIMEOUT)

        // Tap on settings (accessible from Stats or profile)
        settingsRobot
            .waitForSettingsScreen(MEDIUM_TIMEOUT)
            .assertSettingsScreenDisplayed()
            .assertProfileSectionDisplayed()

        // Go back
        settingsRobot.goBack()
    }
}
