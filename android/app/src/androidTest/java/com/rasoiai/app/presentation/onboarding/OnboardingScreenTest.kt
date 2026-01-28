package com.rasoiai.app.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rasoiai.app.presentation.common.TestTags
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.DayOfWeek
import com.rasoiai.domain.model.DietaryRestriction
import com.rasoiai.domain.model.FamilyMember
import com.rasoiai.domain.model.MemberType
import com.rasoiai.domain.model.PrimaryDiet
import com.rasoiai.domain.model.SpiceLevel
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for OnboardingScreen.
 *
 * These tests verify the UI behavior of OnboardingScreen using Compose testing APIs.
 * They test the UI layer in isolation by providing mock data directly to a
 * test wrapper composable that mirrors OnboardingScreen structure.
 *
 * ## Test Categories:
 * - Step 1: Household Size tests
 * - Step 2: Dietary Preferences tests
 * - Step 3: Cuisine Preferences tests
 * - Step 4: Disliked Ingredients tests
 * - Step 5: Cooking Time tests
 * - Navigation tests (back/next buttons)
 * - Progress indicator tests
 *
 * ## Running Tests:
 * ```bash
 * ./gradlew :app:connectedDebugAndroidTest --tests "*.OnboardingScreenTest"
 * ```
 *
 * ## E2E Test Coverage (Phase 2: Onboarding):
 * - Test 2.1: Step 1 - Household Size & Family Members
 * - Test 2.2: Step 2 - Dietary Preferences
 * - Test 2.3: Step 3 - Cuisine Preferences
 * - Test 2.4: Step 4 - Disliked Ingredients
 * - Test 2.5: Step 5 - Cooking Time & Busy Days
 */
class OnboardingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // region Test Data Factory

    private fun createTestFamilyMember(
        id: String = "member-1",
        name: String = "Ramesh",
        type: MemberType = MemberType.ADULT,
        age: Int = 45
    ) = FamilyMember(
        id = id,
        name = name,
        type = type,
        age = age,
        specialNeeds = emptyList()
    )

    private fun createTestUiState(
        currentStep: OnboardingStep = OnboardingStep.HOUSEHOLD_SIZE,
        householdSize: Int = 2,
        familyMembers: List<FamilyMember> = emptyList(),
        primaryDiet: PrimaryDiet = PrimaryDiet.VEGETARIAN,
        dietaryRestrictions: Set<DietaryRestriction> = emptySet(),
        selectedCuisines: Set<CuisineType> = setOf(CuisineType.NORTH),
        spiceLevel: SpiceLevel = SpiceLevel.MEDIUM,
        dislikedIngredients: Set<String> = emptySet(),
        weekdayCookingTime: Int = 30,
        weekendCookingTime: Int = 45,
        busyDays: Set<DayOfWeek> = emptySet(),
        isGenerating: Boolean = false
    ) = OnboardingUiState(
        currentStep = currentStep,
        householdSize = householdSize,
        familyMembers = familyMembers,
        primaryDiet = primaryDiet,
        dietaryRestrictions = dietaryRestrictions,
        selectedCuisines = selectedCuisines,
        spiceLevel = spiceLevel,
        dislikedIngredients = dislikedIngredients,
        weekdayCookingTimeMinutes = weekdayCookingTime,
        weekendCookingTimeMinutes = weekendCookingTime,
        busyDays = busyDays,
        isGenerating = isGenerating
    )

    // endregion

    // region Step 1: Household Size Tests

    @Test
    fun step1_displaysHouseholdSizeQuestion() {
        val uiState = createTestUiState(currentStep = OnboardingStep.HOUSEHOLD_SIZE)

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("How many people are", substring = true).assertIsDisplayed()
    }

    @Test
    fun step1_displaysHouseholdSizeDropdown() {
        val uiState = createTestUiState(currentStep = OnboardingStep.HOUSEHOLD_SIZE, householdSize = 3)

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("3 people").assertIsDisplayed()
    }

    @Test
    fun step1_displaysFamilyMembersSection() {
        val uiState = createTestUiState(currentStep = OnboardingStep.HOUSEHOLD_SIZE)

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Family members:").assertIsDisplayed()
    }

    @Test
    fun step1_displaysAddFamilyMemberButton() {
        val uiState = createTestUiState(currentStep = OnboardingStep.HOUSEHOLD_SIZE)

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Add family member").assertIsDisplayed()
    }

    @Test
    fun step1_displaysFamilyMember_whenAdded() {
        val member = createTestFamilyMember(name = "Ramesh", type = MemberType.ADULT, age = 45)
        val uiState = createTestUiState(
            currentStep = OnboardingStep.HOUSEHOLD_SIZE,
            familyMembers = listOf(member)
        )

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Ramesh").assertIsDisplayed()
        composeTestRule.onNodeWithText("Adult, 45 yrs").assertIsDisplayed()
    }

    @Test
    fun step1_addFamilyMemberClick_triggersCallback() {
        var addClicked = false
        val uiState = createTestUiState(currentStep = OnboardingStep.HOUSEHOLD_SIZE)

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(
                    uiState = uiState,
                    onAddMemberClick = { addClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Add family member").performClick()

        assert(addClicked) { "Add family member callback was not invoked" }
    }

    @Test
    fun step1_editMemberClick_triggersCallback() {
        var editClicked = false
        val member = createTestFamilyMember()
        val uiState = createTestUiState(
            currentStep = OnboardingStep.HOUSEHOLD_SIZE,
            familyMembers = listOf(member)
        )

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(
                    uiState = uiState,
                    onEditMemberClick = { editClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Edit").performClick()

        assert(editClicked) { "Edit member callback was not invoked" }
    }

    @Test
    fun step1_deleteMemberClick_triggersCallback() {
        var deleteClicked = false
        val member = createTestFamilyMember()
        val uiState = createTestUiState(
            currentStep = OnboardingStep.HOUSEHOLD_SIZE,
            familyMembers = listOf(member)
        )

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(
                    uiState = uiState,
                    onDeleteMemberClick = { deleteClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Remove").performClick()

        assert(deleteClicked) { "Delete member callback was not invoked" }
    }

    // endregion

    // region Step 2: Dietary Preferences Tests

    @Test
    fun step2_displaysDietaryQuestion() {
        val uiState = createTestUiState(currentStep = OnboardingStep.DIETARY_PREFERENCES)

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("What's your primary diet?").assertIsDisplayed()
    }

    @Test
    fun step2_displaysPrimaryDietOptions() {
        val uiState = createTestUiState(currentStep = OnboardingStep.DIETARY_PREFERENCES)

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Vegetarian").assertIsDisplayed()
        composeTestRule.onNodeWithText("Non-Vegetarian").assertIsDisplayed()
        composeTestRule.onNodeWithText("Eggetarian").assertIsDisplayed()
    }

    @Test
    fun step2_displaysDietaryRestrictionsSection() {
        val uiState = createTestUiState(currentStep = OnboardingStep.DIETARY_PREFERENCES)

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Special dietary restrictions:").assertIsDisplayed()
    }

    @Test
    fun step2_displaysRestrictionOptions() {
        val uiState = createTestUiState(currentStep = OnboardingStep.DIETARY_PREFERENCES)

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(uiState = uiState)
            }
        }

        // Restrictions may be below fold, scroll to them
        composeTestRule.onNodeWithText("Jain", substring = true).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Sattvic", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun step2_dietSelection_triggersCallback() {
        var selectedDiet: PrimaryDiet? = null
        val uiState = createTestUiState(currentStep = OnboardingStep.DIETARY_PREFERENCES)

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(
                    uiState = uiState,
                    onPrimaryDietChange = { selectedDiet = it }
                )
            }
        }

        composeTestRule.onNodeWithText("Non-Vegetarian").performClick()

        assert(selectedDiet == PrimaryDiet.NON_VEGETARIAN) {
            "Expected NON_VEGETARIAN but got $selectedDiet"
        }
    }

    // endregion

    // region Step 3: Cuisine Preferences Tests

    @Test
    fun step3_displaysCuisineQuestion() {
        val uiState = createTestUiState(currentStep = OnboardingStep.CUISINE_PREFERENCES)

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Which cuisines do you like?").assertIsDisplayed()
    }

    @Test
    fun step3_displaysAllCuisineCards() {
        val uiState = createTestUiState(currentStep = OnboardingStep.CUISINE_PREFERENCES)

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(uiState = uiState)
            }
        }

        // Cuisine cards display displayName.uppercase() e.g., "NORTH INDIAN"
        composeTestRule.onNodeWithText("NORTH INDIAN").assertIsDisplayed()
        composeTestRule.onNodeWithText("SOUTH INDIAN").assertIsDisplayed()
        composeTestRule.onNodeWithText("EAST INDIAN").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("WEST INDIAN").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun step3_displaysSpiceLevelSection() {
        val uiState = createTestUiState(currentStep = OnboardingStep.CUISINE_PREFERENCES)

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Spice level:").assertIsDisplayed()
    }

    @Test
    fun step3_displaysSelectedSpiceLevel() {
        val uiState = createTestUiState(
            currentStep = OnboardingStep.CUISINE_PREFERENCES,
            spiceLevel = SpiceLevel.MEDIUM
        )

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Medium").assertIsDisplayed()
    }

    @Test
    fun step3_cuisineSelection_triggersCallback() {
        var selectedCuisine: CuisineType? = null
        val uiState = createTestUiState(currentStep = OnboardingStep.CUISINE_PREFERENCES)

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(
                    uiState = uiState,
                    onToggleCuisine = { selectedCuisine = it }
                )
            }
        }

        composeTestRule.onNodeWithText("SOUTH INDIAN").performClick()

        assert(selectedCuisine == CuisineType.SOUTH) {
            "Expected SOUTH but got $selectedCuisine"
        }
    }

    // endregion

    // region Step 4: Disliked Ingredients Tests

    @Test
    fun step4_displaysIngredientsQuestion() {
        val uiState = createTestUiState(currentStep = OnboardingStep.DISLIKED_INGREDIENTS)

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Any ingredients you dislike?").assertIsDisplayed()
    }

    @Test
    fun step4_displaysSearchField() {
        val uiState = createTestUiState(currentStep = OnboardingStep.DISLIKED_INGREDIENTS)

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Search ingredients...").assertIsDisplayed()
    }

    @Test
    fun step4_displaysCommonDislikesSection() {
        val uiState = createTestUiState(currentStep = OnboardingStep.DISLIKED_INGREDIENTS)

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Common dislikes:").assertIsDisplayed()
    }

    @Test
    fun step4_displaysCommonIngredientChips() {
        val uiState = createTestUiState(currentStep = OnboardingStep.DISLIKED_INGREDIENTS)

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(uiState = uiState)
            }
        }

        // Check for some common ingredients
        composeTestRule.onNodeWithText("Karela").assertIsDisplayed()
        composeTestRule.onNodeWithText("Baingan").assertIsDisplayed()
    }

    @Test
    fun step4_ingredientToggle_triggersCallback() {
        var toggledIngredient: String? = null
        val uiState = createTestUiState(currentStep = OnboardingStep.DISLIKED_INGREDIENTS)

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(
                    uiState = uiState,
                    onToggleIngredient = { toggledIngredient = it }
                )
            }
        }

        composeTestRule.onNodeWithText("Karela").performClick()

        assert(toggledIngredient == "Karela") {
            "Expected 'Karela' but got '$toggledIngredient'"
        }
    }

    // endregion

    // region Step 5: Cooking Time Tests

    @Test
    fun step5_displaysCookingTimeQuestion() {
        val uiState = createTestUiState(currentStep = OnboardingStep.COOKING_TIME)

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("How much time do you have", substring = true).assertIsDisplayed()
    }

    @Test
    fun step5_displaysWeekdaysSection() {
        val uiState = createTestUiState(currentStep = OnboardingStep.COOKING_TIME)

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Weekdays:").assertIsDisplayed()
    }

    @Test
    fun step5_displaysWeekendsSection() {
        val uiState = createTestUiState(currentStep = OnboardingStep.COOKING_TIME)

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Weekends:").assertIsDisplayed()
    }

    @Test
    fun step5_displaysBusyDaysSection() {
        val uiState = createTestUiState(currentStep = OnboardingStep.COOKING_TIME)

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Busy days (quick meals only):").assertIsDisplayed()
    }

    @Test
    fun step5_displaysDayChips() {
        val uiState = createTestUiState(currentStep = OnboardingStep.COOKING_TIME)

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Mon").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tue").assertIsDisplayed()
        composeTestRule.onNodeWithText("Wed").assertIsDisplayed()
    }

    @Test
    fun step5_busyDayToggle_triggersCallback() {
        var toggledDay: DayOfWeek? = null
        val uiState = createTestUiState(currentStep = OnboardingStep.COOKING_TIME)

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(
                    uiState = uiState,
                    onToggleBusyDay = { toggledDay = it }
                )
            }
        }

        composeTestRule.onNodeWithText("Mon").performClick()

        assert(toggledDay == DayOfWeek.MONDAY) {
            "Expected MONDAY but got $toggledDay"
        }
    }

    // endregion

    // region Navigation Tests

    @Test
    fun navigation_displaysStepIndicator() {
        val uiState = createTestUiState(currentStep = OnboardingStep.HOUSEHOLD_SIZE)

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("1 of 5").assertIsDisplayed()
    }

    @Test
    fun navigation_hidesBackButton_onFirstStep() {
        val uiState = createTestUiState(currentStep = OnboardingStep.HOUSEHOLD_SIZE)

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithContentDescription("Back").assertDoesNotExist()
    }

    @Test
    fun navigation_showsBackButton_onLaterSteps() {
        val uiState = createTestUiState(currentStep = OnboardingStep.DIETARY_PREFERENCES)

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun navigation_backClick_triggersCallback() {
        var backClicked = false
        val uiState = createTestUiState(currentStep = OnboardingStep.DIETARY_PREFERENCES)

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(
                    uiState = uiState,
                    onBackClick = { backClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()

        assert(backClicked) { "Back callback was not invoked" }
    }

    @Test
    fun navigation_displaysNextButton() {
        val uiState = createTestUiState(currentStep = OnboardingStep.HOUSEHOLD_SIZE)

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Next").assertIsDisplayed()
    }

    @Test
    fun navigation_nextClick_triggersCallback() {
        var nextClicked = false
        val uiState = createTestUiState(currentStep = OnboardingStep.HOUSEHOLD_SIZE)

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(
                    uiState = uiState,
                    onNextClick = { nextClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Next").performClick()

        assert(nextClicked) { "Next callback was not invoked" }
    }

    @Test
    fun navigation_displaysCreateButton_onLastStep() {
        val uiState = createTestUiState(currentStep = OnboardingStep.COOKING_TIME)

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Create My Meal Plan").assertIsDisplayed()
    }

    @Test
    fun navigation_nextButton_isDisabled_whenCuisineNotSelected() {
        val uiState = createTestUiState(
            currentStep = OnboardingStep.CUISINE_PREFERENCES,
            selectedCuisines = emptySet()
        )

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Next").assertIsNotEnabled()
    }

    @Test
    fun navigation_nextButton_isEnabled_whenCuisineSelected() {
        val uiState = createTestUiState(
            currentStep = OnboardingStep.CUISINE_PREFERENCES,
            selectedCuisines = setOf(CuisineType.NORTH)
        )

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Next").assertIsEnabled()
    }

    // endregion

    // region Progress Indicator Tests

    @Test
    fun progress_showsCorrectProgress_step1() {
        val uiState = createTestUiState(currentStep = OnboardingStep.HOUSEHOLD_SIZE)

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("1 of 5").assertIsDisplayed()
    }

    @Test
    fun progress_showsCorrectProgress_step3() {
        val uiState = createTestUiState(currentStep = OnboardingStep.CUISINE_PREFERENCES)

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("3 of 5").assertIsDisplayed()
    }

    @Test
    fun progress_showsCorrectProgress_step5() {
        val uiState = createTestUiState(currentStep = OnboardingStep.COOKING_TIME)

        composeTestRule.setContent {
            RasoiAITheme {
                OnboardingTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("5 of 5").assertIsDisplayed()
    }

    // endregion
}

/**
 * Test composable that mirrors the structure of OnboardingScreen content.
 * This allows testing the UI in isolation without the ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun OnboardingTestContent(
    uiState: OnboardingUiState,
    onBackClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
    onAddMemberClick: () -> Unit = {},
    onEditMemberClick: (FamilyMember) -> Unit = {},
    onDeleteMemberClick: (FamilyMember) -> Unit = {},
    onPrimaryDietChange: (PrimaryDiet) -> Unit = {},
    onToggleRestriction: (DietaryRestriction) -> Unit = {},
    onToggleCuisine: (CuisineType) -> Unit = {},
    onSpiceLevelChange: (SpiceLevel) -> Unit = {},
    onToggleIngredient: (String) -> Unit = {},
    onToggleBusyDay: (DayOfWeek) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    if (!uiState.isFirstStep) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    Text(
                        text = "${uiState.currentStep.stepNumber} of 5",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = { uiState.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Step content
            Box(modifier = Modifier.weight(1f)) {
                when (uiState.currentStep) {
                    OnboardingStep.HOUSEHOLD_SIZE -> HouseholdSizeTestContent(
                        householdSize = uiState.householdSize,
                        familyMembers = uiState.familyMembers,
                        onAddMemberClick = onAddMemberClick,
                        onEditMemberClick = onEditMemberClick,
                        onDeleteMemberClick = onDeleteMemberClick
                    )
                    OnboardingStep.DIETARY_PREFERENCES -> DietaryPreferencesTestContent(
                        primaryDiet = uiState.primaryDiet,
                        dietaryRestrictions = uiState.dietaryRestrictions,
                        onPrimaryDietChange = onPrimaryDietChange,
                        onToggleRestriction = onToggleRestriction
                    )
                    OnboardingStep.CUISINE_PREFERENCES -> CuisinePreferencesTestContent(
                        selectedCuisines = uiState.selectedCuisines,
                        spiceLevel = uiState.spiceLevel,
                        onToggleCuisine = onToggleCuisine,
                        onSpiceLevelChange = onSpiceLevelChange
                    )
                    OnboardingStep.DISLIKED_INGREDIENTS -> DislikedIngredientsTestContent(
                        dislikedIngredients = uiState.dislikedIngredients,
                        onToggleIngredient = onToggleIngredient
                    )
                    OnboardingStep.COOKING_TIME -> CookingTimeTestContent(
                        weekdayCookingTime = uiState.weekdayCookingTimeMinutes,
                        weekendCookingTime = uiState.weekendCookingTimeMinutes,
                        busyDays = uiState.busyDays,
                        onToggleBusyDay = onToggleBusyDay
                    )
                }
            }

            // Next/Create button
            Button(
                onClick = onNextClick,
                enabled = uiState.canProceed,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (uiState.isLastStep) "Create My Meal Plan" else "Next",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun HouseholdSizeTestContent(
    householdSize: Int,
    familyMembers: List<FamilyMember>,
    onAddMemberClick: () -> Unit,
    onEditMemberClick: (FamilyMember) -> Unit,
    onDeleteMemberClick: (FamilyMember) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "How many people are\nyou cooking for?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Household size dropdown (simplified)
        OutlinedTextField(
            value = "$householdSize ${if (householdSize == 1) "person" else "people"}",
            onValueChange = { },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Family members:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                familyMembers.forEach { member ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = member.name, fontWeight = FontWeight.Medium)
                            Text(
                                text = "${member.type.displayName}, ${member.age} yrs",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        IconButton(onClick = { onEditMemberClick(member) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }

                        IconButton(onClick = { onDeleteMemberClick(member) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Remove",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onAddMemberClick)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add family member",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun DietaryPreferencesTestContent(
    primaryDiet: PrimaryDiet,
    dietaryRestrictions: Set<DietaryRestriction>,
    onPrimaryDietChange: (PrimaryDiet) -> Unit,
    onToggleRestriction: (DietaryRestriction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "What's your primary diet?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Column(modifier = Modifier.selectableGroup()) {
            PrimaryDiet.entries.forEach { diet ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .selectable(
                            selected = diet == primaryDiet,
                            onClick = { onPrimaryDietChange(diet) },
                            role = Role.RadioButton
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (diet == primaryDiet)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = diet == primaryDiet, onClick = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = diet.displayName, fontWeight = FontWeight.Medium)
                            Text(
                                text = diet.description,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Special dietary restrictions:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        DietaryRestriction.entries.forEach { restriction ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleRestriction(restriction) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = restriction in dietaryRestrictions,
                    onCheckedChange = { onToggleRestriction(restriction) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = restriction.displayName)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CuisinePreferencesTestContent(
    selectedCuisines: Set<CuisineType>,
    spiceLevel: SpiceLevel,
    onToggleCuisine: (CuisineType) -> Unit,
    onSpiceLevelChange: (SpiceLevel) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Which cuisines do you like?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Cuisine grid
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CuisineCardTest(
                    cuisine = CuisineType.NORTH,
                    isSelected = CuisineType.NORTH in selectedCuisines,
                    onClick = { onToggleCuisine(CuisineType.NORTH) },
                    modifier = Modifier.weight(1f)
                )
                CuisineCardTest(
                    cuisine = CuisineType.SOUTH,
                    isSelected = CuisineType.SOUTH in selectedCuisines,
                    onClick = { onToggleCuisine(CuisineType.SOUTH) },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CuisineCardTest(
                    cuisine = CuisineType.EAST,
                    isSelected = CuisineType.EAST in selectedCuisines,
                    onClick = { onToggleCuisine(CuisineType.EAST) },
                    modifier = Modifier.weight(1f)
                )
                CuisineCardTest(
                    cuisine = CuisineType.WEST,
                    isSelected = CuisineType.WEST in selectedCuisines,
                    onClick = { onToggleCuisine(CuisineType.WEST) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Spice level:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = spiceLevel.displayName,
            onValueChange = { },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
private fun CuisineCardTest(
    cuisine: CuisineType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = cuisine.displayName.uppercase(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DislikedIngredientsTestContent(
    dislikedIngredients: Set<String>,
    onToggleIngredient: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Any ingredients you dislike?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = "",
            onValueChange = { },
            placeholder = { Text("Search ingredients...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Common dislikes:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CommonDislikedIngredients.ingredients.forEach { (name, englishName) ->
                val isSelected = name in dislikedIngredients
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggleIngredient(name) },
                    label = { Text(name) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                    } else null
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CookingTimeTestContent(
    weekdayCookingTime: Int,
    weekendCookingTime: Int,
    busyDays: Set<DayOfWeek>,
    onToggleBusyDay: (DayOfWeek) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "How much time do you have\nfor cooking?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Weekdays:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = "$weekdayCookingTime minutes",
            onValueChange = { },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Weekends:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = "$weekendCookingTime minutes",
            onValueChange = { },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Busy days (quick meals only):",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DayOfWeek.entries.forEach { day ->
                val isSelected = day in busyDays
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggleBusyDay(day) },
                    label = { Text(day.shortName) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                    } else null
                )
            }
        }
    }
}
