package com.rasoiai.app.e2e.flows

import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.app.e2e.base.TestDataFactory
import com.rasoiai.app.e2e.di.FakeAuthRepository
import com.rasoiai.app.e2e.di.FakeGoogleAuthClient
import com.rasoiai.app.e2e.robots.AuthRobot
import com.rasoiai.app.e2e.robots.OnboardingRobot
import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.DietaryTag
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import javax.inject.Inject

/**
 * Phase 2: Onboarding Testing (5 Steps)
 *
 * Tests:
 * 2.1 Step 1 - Household Size & Family Members
 * 2.2 Step 2 - Dietary Preferences
 * 2.3 Step 3 - Cuisine Preferences
 * 2.4 Step 4 - Disliked Ingredients
 * 2.5 Step 5 - Cooking Time & Busy Days
 */
@HiltAndroidTest
class OnboardingFlowTest : BaseE2ETest() {

    private lateinit var authRobot: AuthRobot
    private lateinit var onboardingRobot: OnboardingRobot

    @Inject
    lateinit var fakeGoogleAuthClient: FakeGoogleAuthClient

    @Inject
    lateinit var fakeAuthRepository: FakeAuthRepository

    private val sharmaFamily = TestDataFactory.sharmaFamily

    @Before
    override fun setUp() {
        super.setUp()
        authRobot = AuthRobot(composeTestRule)
        onboardingRobot = OnboardingRobot(composeTestRule)

        // Configure for successful auth
        fakeGoogleAuthClient.setSignInSuccess()
        fakeAuthRepository.setAuthSuccess()

        // Navigate to onboarding
        navigateToOnboarding()
    }

    private fun navigateToOnboarding() {
        authRobot.waitForAuthScreen()
        authRobot.tapGoogleSignIn()
        authRobot.assertNavigatedToOnboarding()
    }

    /**
     * Test 2.1: Step 1 - Household Size & Family Members
     *
     * Steps:
     * 1. Verify step indicator shows 1/5 (20% progress)
     * 2. Select household size: 3
     * 3. Add family member 1: Ramesh (ADULT, 45, DIABETIC, LOW_OIL)
     * 4. Add family member 2: Sunita (ADULT, 42, LOW_SALT)
     * 5. Add family member 3: Aarav (CHILD, 12, NO_SPICY)
     * 6. Verify all 3 members shown in list
     * 7. Tap "Next"
     */
    @Test
    fun test_2_1_step1_householdSizeAndFamilyMembers() {
        // Step 1: Verify step indicator
        onboardingRobot.assertStepIndicator(1, 5)
        onboardingRobot.assertProgress(20)

        // Step 2: Select household size
        onboardingRobot.selectHouseholdSize(sharmaFamily.householdSize)

        // Steps 3-5: Add all family members
        for (member in sharmaFamily.members) {
            onboardingRobot.addFamilyMember(member)
        }

        // Step 6: Verify all members displayed
        for (member in sharmaFamily.members) {
            onboardingRobot.assertFamilyMemberDisplayed(member.name)
        }
        onboardingRobot.assertFamilyMemberCount(3)

        // Step 7: Next should be enabled
        onboardingRobot.assertNextEnabled()
        onboardingRobot.tapNext()

        // Should be on step 2
        onboardingRobot.assertStepIndicator(2, 5)
    }

    /**
     * Test 2.2: Step 2 - Dietary Preferences
     *
     * Steps:
     * 1. Verify step indicator shows 2/5 (40% progress)
     * 2. Verify VEGETARIAN is pre-selected (default)
     * 3. Select dietary restriction: SATTVIC
     * 4. Tap "Next"
     */
    @Test
    fun test_2_2_step2_dietaryPreferences() {
        // Complete step 1 first
        completeStep1()

        // Step 1: Verify step indicator
        onboardingRobot.assertStepIndicator(2, 5)
        onboardingRobot.assertProgress(40)

        // Step 2: Verify VEGETARIAN is default
        onboardingRobot.assertDietSelected(DietaryTag.VEGETARIAN)

        // Step 3: Select SATTVIC restriction
        onboardingRobot.selectDietaryRestriction(DietaryTag.SATTVIC)

        // Step 4: Next
        onboardingRobot.assertNextEnabled()
        onboardingRobot.tapNext()

        // Should be on step 3
        onboardingRobot.assertStepIndicator(3, 5)
    }

    /**
     * Test 2.3: Step 3 - Cuisine Preferences
     *
     * Steps:
     * 1. Verify step indicator shows 3/5 (60% progress)
     * 2. Select cuisines: NORTH, SOUTH
     * 3. Select spice level: MEDIUM
     * 4. Tap "Next"
     */
    @Test
    fun test_2_3_step3_cuisinePreferences() {
        // Complete steps 1-2
        completeStep1()
        completeStep2()

        // Step 1: Verify step indicator
        onboardingRobot.assertStepIndicator(3, 5)
        onboardingRobot.assertProgress(60)

        // Step 2: Select cuisines
        onboardingRobot.selectCuisine(CuisineType.NORTH)
        onboardingRobot.selectCuisine(CuisineType.SOUTH)
        onboardingRobot.assertCuisineSelected(CuisineType.NORTH)
        onboardingRobot.assertCuisineSelected(CuisineType.SOUTH)

        // Step 3: Select spice level
        onboardingRobot.selectSpiceLevel(sharmaFamily.spiceLevel)

        // Step 4: Next
        onboardingRobot.assertNextEnabled()
        onboardingRobot.tapNext()

        // Should be on step 4
        onboardingRobot.assertStepIndicator(4, 5)
    }

    /**
     * Test 2.4: Step 4 - Disliked Ingredients
     *
     * Steps:
     * 1. Verify step indicator shows 4/5 (80% progress)
     * 2. Select disliked ingredients: Karela, Baingan, Mushroom
     * 3. Test search functionality
     * 4. Tap "Next"
     */
    @Test
    fun test_2_4_step4_dislikedIngredients() {
        // Complete steps 1-3
        completeStep1()
        completeStep2()
        completeStep3()

        // Step 1: Verify step indicator
        onboardingRobot.assertStepIndicator(4, 5)
        onboardingRobot.assertProgress(80)

        // Step 2: Select disliked ingredients
        for (ingredient in sharmaFamily.dislikedIngredients) {
            onboardingRobot.selectDislikedIngredient(ingredient)
        }

        // Step 3: Test search
        onboardingRobot.searchIngredient("Ca")
        waitFor(ANIMATION_DURATION)
        onboardingRobot.clearIngredientSearch()

        // Step 4: Next
        onboardingRobot.assertNextEnabled()
        onboardingRobot.tapNext()

        // Should be on step 5
        onboardingRobot.assertStepIndicator(5, 5)
    }

    /**
     * Test 2.5: Step 5 - Cooking Time & Busy Days
     *
     * Steps:
     * 1. Verify step indicator shows 5/5 (100% progress)
     * 2. Set weekday cooking time: 30 minutes
     * 3. Set weekend cooking time: 60 minutes
     * 4. Select busy days: MON, WED, FRI
     * 5. Tap "Create My Meal Plan"
     */
    @Test
    fun test_2_5_step5_cookingTimeAndBusyDays() {
        // Complete steps 1-4
        completeStep1()
        completeStep2()
        completeStep3()
        completeStep4()

        // Step 1: Verify step indicator
        onboardingRobot.assertStepIndicator(5, 5)
        onboardingRobot.assertProgress(100)

        // Step 2: Set weekday cooking time
        onboardingRobot.setWeekdayCookingTime(sharmaFamily.weekdayCookingTime)

        // Step 3: Set weekend cooking time
        onboardingRobot.setWeekendCookingTime(sharmaFamily.weekendCookingTime)

        // Step 4: Select busy days
        for (day in sharmaFamily.busyDays) {
            onboardingRobot.selectBusyDay(day)
        }

        // Step 5: Create Meal Plan
        onboardingRobot.tapCreateMealPlan()

        // Should navigate to generating screen
        onboardingRobot.waitForGeneratingScreen()
    }

    /**
     * Test: Next button is disabled without required selections
     */
    @Test
    fun step1_nextButtonDisabled_withoutHouseholdSize() {
        // Without selecting household size, next should be disabled
        onboardingRobot.assertNextDisabled()
    }

    /**
     * Test: Can edit existing family member
     */
    @Test
    fun step1_canEditFamilyMember() {
        // Add a family member
        onboardingRobot.selectHouseholdSize(1)
        onboardingRobot.addFamilyMember(sharmaFamily.members[0])

        // Edit the member
        onboardingRobot.tapEditFamilyMember(0)
        // Verify edit sheet opens
        waitFor(ANIMATION_DURATION)
    }

    /**
     * Test: Can delete family member
     */
    @Test
    fun step1_canDeleteFamilyMember() {
        // Add two members
        onboardingRobot.selectHouseholdSize(2)
        onboardingRobot.addFamilyMember(sharmaFamily.members[0])
        onboardingRobot.addFamilyMember(sharmaFamily.members[1])

        // Delete one
        onboardingRobot.tapDeleteFamilyMember(0)

        // Should have 1 member left
        onboardingRobot.assertFamilyMemberCount(1)
    }

    // Helper methods to complete each step

    private fun completeStep1() {
        onboardingRobot.selectHouseholdSize(sharmaFamily.householdSize)
        for (member in sharmaFamily.members) {
            onboardingRobot.addFamilyMember(member)
        }
        onboardingRobot.tapNext()
    }

    private fun completeStep2() {
        onboardingRobot.selectDietaryRestriction(DietaryTag.SATTVIC)
        onboardingRobot.tapNext()
    }

    private fun completeStep3() {
        onboardingRobot.selectCuisine(CuisineType.NORTH)
        onboardingRobot.selectCuisine(CuisineType.SOUTH)
        onboardingRobot.selectSpiceLevel(sharmaFamily.spiceLevel)
        onboardingRobot.tapNext()
    }

    private fun completeStep4() {
        for (ingredient in sharmaFamily.dislikedIngredients) {
            onboardingRobot.selectDislikedIngredient(ingredient)
        }
        onboardingRobot.tapNext()
    }
}
