package com.rasoiai.app.presentation.settings.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.domain.model.DietaryRestriction
import com.rasoiai.domain.model.PrimaryDiet
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DietaryRestrictionsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setupScreen(uiState: DietaryRestrictionsUiState): DietaryRestrictionsViewModel {
        val mockViewModel = mockk<DietaryRestrictionsViewModel>(relaxed = true)
        every { mockViewModel.uiState } returns MutableStateFlow(uiState)
        composeTestRule.setContent {
            RasoiAITheme {
                DietaryRestrictionsScreen(onNavigateBack = {}, viewModel = mockViewModel)
            }
        }
        return mockViewModel
    }

    @Test
    fun screen_displaysTitle() {
        setupScreen(
            DietaryRestrictionsUiState(
                isLoading = false,
                isSaving = false,
                primaryDiet = PrimaryDiet.VEGETARIAN,
                dietaryRestrictions = emptySet(),
                saveSuccess = false,
                errorMessage = null
            )
        )

        composeTestRule.onNodeWithText("Dietary Restrictions").assertIsDisplayed()
    }

    @Test
    fun screen_displaysPrimaryDietSectionHeader() {
        setupScreen(
            DietaryRestrictionsUiState(
                isLoading = false,
                isSaving = false,
                primaryDiet = PrimaryDiet.VEGETARIAN,
                dietaryRestrictions = emptySet(),
                saveSuccess = false,
                errorMessage = null
            )
        )

        composeTestRule.onNodeWithText("Primary Diet").assertIsDisplayed()
    }

    @Test
    fun screen_displaysAllPrimaryDietOptions() {
        setupScreen(
            DietaryRestrictionsUiState(
                isLoading = false,
                isSaving = false,
                primaryDiet = PrimaryDiet.VEGETARIAN,
                dietaryRestrictions = emptySet(),
                saveSuccess = false,
                errorMessage = null
            )
        )

        PrimaryDiet.entries.forEach { diet ->
            composeTestRule.onNodeWithText(diet.displayName).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun screen_displaysSpecialDietaryRestrictionsSectionHeader() {
        setupScreen(
            DietaryRestrictionsUiState(
                isLoading = false,
                isSaving = false,
                primaryDiet = PrimaryDiet.VEGETARIAN,
                dietaryRestrictions = emptySet(),
                saveSuccess = false,
                errorMessage = null
            )
        )

        composeTestRule.onNodeWithText("Special Dietary Restrictions").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun screen_displaysSaveButton() {
        setupScreen(
            DietaryRestrictionsUiState(
                isLoading = false,
                isSaving = false,
                primaryDiet = PrimaryDiet.VEGETARIAN,
                dietaryRestrictions = emptySet(),
                saveSuccess = false,
                errorMessage = null
            )
        )

        composeTestRule.onNodeWithText("Save").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun loadingState_hidesContent() {
        setupScreen(
            DietaryRestrictionsUiState(
                isLoading = true,
                isSaving = false,
                primaryDiet = PrimaryDiet.VEGETARIAN,
                dietaryRestrictions = emptySet(),
                saveSuccess = false,
                errorMessage = null
            )
        )

        composeTestRule.onNodeWithText("Primary Diet").assertDoesNotExist()
    }

    @Test
    fun saveButton_callsViewModelSave() {
        val mockViewModel = setupScreen(
            DietaryRestrictionsUiState(
                isLoading = false,
                isSaving = false,
                primaryDiet = PrimaryDiet.VEGETARIAN,
                dietaryRestrictions = emptySet(),
                saveSuccess = false,
                errorMessage = null
            )
        )

        composeTestRule.onNodeWithText("Save").performScrollTo().performClick()

        verify { mockViewModel.save() }
    }
}
