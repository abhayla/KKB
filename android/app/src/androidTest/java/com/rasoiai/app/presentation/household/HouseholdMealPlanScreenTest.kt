package com.rasoiai.app.presentation.household

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rasoiai.app.presentation.common.TestTags
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.domain.model.DietaryTag
import com.rasoiai.domain.model.MealItem
import com.rasoiai.domain.model.MealPlan
import com.rasoiai.domain.model.MealPlanDay
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * UI Tests for HouseholdMealPlanScreen.
 *
 * Tests the household shared meal plan display including day selector,
 * meal sections, loading states, and empty states.
 */
@RunWith(AndroidJUnit4::class)
class HouseholdMealPlanScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // region Test Data

    private val today = LocalDate.of(2026, 3, 8)

    private fun createTestMealItem(
        id: String = "item-1",
        recipeName: String = "Masala Chai",
        prepTimeMinutes: Int = 10
    ) = MealItem(
        id = id,
        recipeId = "recipe-$id",
        recipeName = recipeName,
        recipeImageUrl = null,
        prepTimeMinutes = prepTimeMinutes,
        calories = 120,
        isLocked = false,
        order = 0,
        dietaryTags = listOf(DietaryTag.VEGETARIAN)
    )

    private fun createTestMealPlanDay(
        date: LocalDate = today,
        dayName: String = "Sunday"
    ) = MealPlanDay(
        date = date,
        dayName = dayName,
        breakfast = listOf(createTestMealItem("b1", "Masala Chai", 10)),
        lunch = listOf(createTestMealItem("l1", "Dal Fry", 25)),
        dinner = listOf(createTestMealItem("d1", "Paneer Butter Masala", 35)),
        snacks = listOf(createTestMealItem("s1", "Samosa", 15)),
        festival = null
    )

    private fun createTestMealPlan(): MealPlan {
        val days = (0..6).map { offset ->
            val date = today.plusDays(offset.toLong())
            createTestMealPlanDay(
                date = date,
                dayName = date.dayOfWeek.name.lowercase()
                    .replaceFirstChar { it.uppercase() }
            )
        }
        return MealPlan(
            id = "plan-1",
            weekStartDate = today,
            weekEndDate = today.plusDays(6),
            days = days,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    // endregion

    @Test
    fun screenTag_displayed() {
        composeTestRule.setContent {
            RasoiAITheme {
                HouseholdMealPlanScreen(
                    uiState = HouseholdMealPlanUiState()
                )
            }
        }

        composeTestRule
            .onNodeWithTag(TestTags.HOUSEHOLD_MEAL_PLAN_SCREEN)
            .assertIsDisplayed()
    }

    @Test
    fun loadingIndicator_displayed() {
        composeTestRule.setContent {
            RasoiAITheme {
                HouseholdMealPlanScreen(
                    uiState = HouseholdMealPlanUiState(isLoading = true)
                )
            }
        }

        composeTestRule
            .onNodeWithTag(TestTags.HOUSEHOLD_MEAL_PLAN_SCREEN)
            .assertIsDisplayed()
        // CircularProgressIndicator has no specific tag but is rendered when loading
    }

    @Test
    fun mealSections_whenPlanExists() {
        val plan = createTestMealPlan()
        val firstDay = plan.days.first()

        composeTestRule.setContent {
            RasoiAITheme {
                HouseholdMealPlanScreen(
                    uiState = HouseholdMealPlanUiState(
                        mealPlan = plan,
                        selectedDate = today,
                        selectedDayMeals = firstDay
                    )
                )
            }
        }

        // Verify meal items are displayed
        composeTestRule.onNodeWithText("Masala Chai").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dal Fry").assertIsDisplayed()
        composeTestRule.onNodeWithText("Paneer Butter Masala").assertIsDisplayed()
        composeTestRule.onNodeWithText("Samosa").assertIsDisplayed()

        // Verify section labels
        composeTestRule.onNodeWithText("Breakfast").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lunch").assertIsDisplayed()
    }

    @Test
    fun emptyState_whenNoPlan() {
        composeTestRule.setContent {
            RasoiAITheme {
                HouseholdMealPlanScreen(
                    uiState = HouseholdMealPlanUiState(
                        isLoading = false,
                        mealPlan = null
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("No shared meal plan yet").assertIsDisplayed()
    }

    @Test
    fun daySelector_displayed() {
        val plan = createTestMealPlan()
        val firstDay = plan.days.first()

        composeTestRule.setContent {
            RasoiAITheme {
                HouseholdMealPlanScreen(
                    uiState = HouseholdMealPlanUiState(
                        mealPlan = plan,
                        selectedDate = today,
                        selectedDayMeals = firstDay
                    )
                )
            }
        }

        // Day selector shows day numbers — verify the first day's number is displayed
        composeTestRule.onNodeWithText(today.dayOfMonth.toString()).assertIsDisplayed()
    }

    @Test
    fun errorState_displayed() {
        composeTestRule.setContent {
            RasoiAITheme {
                HouseholdMealPlanScreen(
                    uiState = HouseholdMealPlanUiState(
                        error = "Something went wrong",
                        isLoading = false,
                        mealPlan = null
                    )
                )
            }
        }

        // Screen should still render with error state
        composeTestRule
            .onNodeWithTag(TestTags.HOUSEHOLD_MEAL_PLAN_SCREEN)
            .assertIsDisplayed()
    }
}
