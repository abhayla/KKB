package com.rasoiai.app.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rasoiai.data.local.datastore.UserPreferencesDataStoreInterface
import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.DayOfWeek
import com.rasoiai.domain.model.DietaryRestriction
import com.rasoiai.domain.model.FamilyMember
import com.rasoiai.domain.model.MemberType
import com.rasoiai.domain.model.PrimaryDiet
import com.rasoiai.domain.model.SpecialDietaryNeed
import com.rasoiai.domain.model.SpiceLevel
import com.rasoiai.domain.model.UserPreferences
import com.rasoiai.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * Onboarding steps
 */
enum class OnboardingStep(val stepNumber: Int, val title: String) {
    HOUSEHOLD_SIZE(1, "Household Size"),
    DIETARY_PREFERENCES(2, "Dietary Preferences"),
    CUISINE_PREFERENCES(3, "Cuisine Preferences"),
    DISLIKED_INGREDIENTS(4, "Disliked Ingredients"),
    COOKING_TIME(5, "Cooking Time")
}

/**
 * UI state for the Onboarding screen
 */
data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.HOUSEHOLD_SIZE,
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val generatingProgress: GeneratingProgress = GeneratingProgress(),
    val errorMessage: String? = null,

    // Step 1: Household Size
    val householdSize: Int = 2,
    val familyMembers: List<FamilyMember> = emptyList(),
    val showAddMemberDialog: Boolean = false,
    val editingMember: FamilyMember? = null,

    // Step 2: Dietary Preferences
    val primaryDiet: PrimaryDiet = PrimaryDiet.VEGETARIAN,
    val dietaryRestrictions: Set<DietaryRestriction> = emptySet(),

    // Step 3: Cuisine Preferences
    val selectedCuisines: Set<CuisineType> = setOf(CuisineType.NORTH),
    val spiceLevel: SpiceLevel = SpiceLevel.MEDIUM,

    // Step 4: Disliked Ingredients
    val dislikedIngredients: Set<String> = emptySet(),
    val ingredientSearchQuery: String = "",

    // Step 5: Cooking Time
    val weekdayCookingTimeMinutes: Int = 30,
    val weekendCookingTimeMinutes: Int = 45,
    val busyDays: Set<DayOfWeek> = emptySet()
) {
    val canProceed: Boolean
        get() = when (currentStep) {
            OnboardingStep.HOUSEHOLD_SIZE -> householdSize > 0
            OnboardingStep.DIETARY_PREFERENCES -> true
            OnboardingStep.CUISINE_PREFERENCES -> selectedCuisines.isNotEmpty()
            OnboardingStep.DISLIKED_INGREDIENTS -> true
            OnboardingStep.COOKING_TIME -> true
        }

    val isFirstStep: Boolean get() = currentStep == OnboardingStep.HOUSEHOLD_SIZE
    val isLastStep: Boolean get() = currentStep == OnboardingStep.COOKING_TIME
    val progress: Float get() = currentStep.stepNumber / 5f
}

data class GeneratingProgress(
    val analyzingPreferences: Boolean = false,
    val analyzingPreferencesDone: Boolean = false,
    val checkingFestivals: Boolean = false,
    val checkingFestivalsDone: Boolean = false,
    val generatingRecipes: Boolean = false,
    val generatingRecipesDone: Boolean = false,
    val buildingGroceryList: Boolean = false,
    val buildingGroceryListDone: Boolean = false
)

/**
 * Navigation events from Onboarding screen
 */
sealed class OnboardingNavigationEvent {
    data object NavigateToHome : OnboardingNavigationEvent()
}

/**
 * Common disliked ingredients in Indian cuisine
 */
object CommonDislikedIngredients {
    val ingredients = listOf(
        "Karela" to "Bitter Gourd",
        "Lauki" to "Bottle Gourd",
        "Turai" to "Ridge Gourd",
        "Baingan" to "Eggplant",
        "Bhindi" to "Okra",
        "Arbi" to "Colocasia",
        "Coriander" to "Cilantro",
        "Methi" to "Fenugreek",
        "Mushroom" to "Mushroom",
        "Capsicum" to "Bell Pepper",
        "Cabbage" to "Cabbage",
        "Cauliflower" to "Gobi"
    )
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferencesDataStore: UserPreferencesDataStoreInterface,
    private val settingsRepository: SettingsRepository,
    private val generateMealPlanUseCase: com.rasoiai.domain.usecase.GenerateMealPlanUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _navigationEvent = Channel<OnboardingNavigationEvent>()
    val navigationEvent: Flow<OnboardingNavigationEvent> = _navigationEvent.receiveAsFlow()

    // region Navigation

    fun goToNextStep() {
        val currentStep = _uiState.value.currentStep
        if (currentStep.isLastStep()) {
            completeOnboarding()
        } else {
            val nextStep = OnboardingStep.entries.getOrNull(currentStep.ordinal + 1)
            if (nextStep != null) {
                _uiState.update { it.copy(currentStep = nextStep) }
            }
        }
    }

    fun goToPreviousStep() {
        val currentStep = _uiState.value.currentStep
        val previousStep = OnboardingStep.entries.getOrNull(currentStep.ordinal - 1)
        if (previousStep != null) {
            _uiState.update { it.copy(currentStep = previousStep) }
        }
    }

    private fun OnboardingStep.isLastStep() = this == OnboardingStep.COOKING_TIME

    // endregion

    // region Step 1: Household Size

    fun updateHouseholdSize(size: Int) {
        _uiState.update { it.copy(householdSize = size.coerceIn(1, 10)) }
    }

    fun showAddMemberDialog() {
        _uiState.update { it.copy(showAddMemberDialog = true, editingMember = null) }
    }

    fun showEditMemberDialog(member: FamilyMember) {
        _uiState.update { it.copy(showAddMemberDialog = true, editingMember = member) }
    }

    fun dismissMemberDialog() {
        _uiState.update { it.copy(showAddMemberDialog = false, editingMember = null) }
    }

    fun addOrUpdateFamilyMember(
        name: String,
        type: MemberType,
        age: Int?,
        specialNeeds: List<SpecialDietaryNeed>
    ) {
        val editingMember = _uiState.value.editingMember
        val newMember = FamilyMember(
            id = editingMember?.id ?: UUID.randomUUID().toString(),
            name = name,
            type = type,
            age = age,
            specialNeeds = specialNeeds
        )

        _uiState.update { state ->
            val updatedMembers = if (editingMember != null) {
                state.familyMembers.map { if (it.id == editingMember.id) newMember else it }
            } else {
                state.familyMembers + newMember
            }
            state.copy(
                familyMembers = updatedMembers,
                showAddMemberDialog = false,
                editingMember = null
            )
        }
    }

    fun removeFamilyMember(member: FamilyMember) {
        _uiState.update { state ->
            state.copy(familyMembers = state.familyMembers.filter { it.id != member.id })
        }
    }

    // endregion

    // region Step 2: Dietary Preferences

    fun updatePrimaryDiet(diet: PrimaryDiet) {
        _uiState.update { it.copy(primaryDiet = diet) }
    }

    fun toggleDietaryRestriction(restriction: DietaryRestriction) {
        _uiState.update { state ->
            val current = state.dietaryRestrictions
            val updated = if (restriction in current) {
                current - restriction
            } else {
                current + restriction
            }
            state.copy(dietaryRestrictions = updated)
        }
    }

    // endregion

    // region Step 3: Cuisine Preferences

    fun toggleCuisine(cuisine: CuisineType) {
        _uiState.update { state ->
            val current = state.selectedCuisines
            val updated = if (cuisine in current) {
                if (current.size > 1) current - cuisine else current
            } else {
                current + cuisine
            }
            state.copy(selectedCuisines = updated)
        }
    }

    fun updateSpiceLevel(level: SpiceLevel) {
        _uiState.update { it.copy(spiceLevel = level) }
    }

    // endregion

    // region Step 4: Disliked Ingredients

    fun toggleDislikedIngredient(ingredient: String) {
        _uiState.update { state ->
            val current = state.dislikedIngredients
            val updated = if (ingredient in current) {
                current - ingredient
            } else {
                current + ingredient
            }
            state.copy(dislikedIngredients = updated)
        }
    }

    fun updateIngredientSearchQuery(query: String) {
        _uiState.update { it.copy(ingredientSearchQuery = query) }
    }

    fun addCustomDislikedIngredient(ingredient: String) {
        if (ingredient.isNotBlank()) {
            _uiState.update { state ->
                state.copy(
                    dislikedIngredients = state.dislikedIngredients + ingredient.trim(),
                    ingredientSearchQuery = ""
                )
            }
        }
    }

    // endregion

    // region Step 5: Cooking Time

    fun updateWeekdayCookingTime(minutes: Int) {
        _uiState.update { it.copy(weekdayCookingTimeMinutes = minutes) }
    }

    fun updateWeekendCookingTime(minutes: Int) {
        _uiState.update { it.copy(weekendCookingTimeMinutes = minutes) }
    }

    fun toggleBusyDay(day: DayOfWeek) {
        _uiState.update { state ->
            val current = state.busyDays
            val updated = if (day in current) current - day else current + day
            state.copy(busyDays = updated)
        }
    }

    // endregion

    // region Complete Onboarding

    private fun completeOnboarding() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }

            try {
                // Save preferences FIRST (required for meal generation)
                val state = _uiState.value
                val preferences = UserPreferences(
                    householdSize = state.householdSize,
                    familyMembers = state.familyMembers,
                    primaryDiet = state.primaryDiet,
                    dietaryRestrictions = state.dietaryRestrictions.toList(),
                    cuisinePreferences = state.selectedCuisines.toList(),
                    spiceLevel = state.spiceLevel,
                    dislikedIngredients = state.dislikedIngredients.toList(),
                    weekdayCookingTimeMinutes = state.weekdayCookingTimeMinutes,
                    weekendCookingTimeMinutes = state.weekendCookingTimeMinutes,
                    busyDays = state.busyDays.toList()
                )

                // Save locally and sync to backend PostgreSQL
                settingsRepository.updateUserPreferences(preferences)
                Timber.i("Onboarding complete, preferences saved and synced to backend")

                // Actually generate meal plan with real progress UI
                generateMealPlanWithProgress()

                _navigationEvent.send(OnboardingNavigationEvent.NavigateToHome)

            } catch (e: Exception) {
                Timber.e(e, "Error completing onboarding")
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        errorMessage = "Failed to save preferences. Please try again."
                    )
                }
            }
        }
    }

    /**
     * Generate meal plan with real progress UI.
     *
     * Shows progress stages as the backend AI service works:
     * 1. Analyzing preferences (delay for UI smoothness)
     * 2. Checking festivals (delay for UI smoothness)
     * 3. Generating recipes (actual API call to backend Gemini service)
     * 4. Building grocery list (delay for UI smoothness)
     *
     * Fix for Issue #43: Actually call the meal plan generation API instead of
     * just simulating progress. The backend AI generation takes 4-7 seconds.
     */
    private suspend fun generateMealPlanWithProgress() {
        // Analyzing preferences (UI stage)
        _uiState.update {
            it.copy(generatingProgress = it.generatingProgress.copy(analyzingPreferences = true))
        }
        delay(800)
        _uiState.update {
            it.copy(
                generatingProgress = it.generatingProgress.copy(
                    analyzingPreferences = false,
                    analyzingPreferencesDone = true,
                    checkingFestivals = true
                )
            )
        }

        // Checking festivals (UI stage)
        delay(600)
        _uiState.update {
            it.copy(
                generatingProgress = it.generatingProgress.copy(
                    checkingFestivals = false,
                    checkingFestivalsDone = true,
                    generatingRecipes = true
                )
            )
        }

        // Generating recipes (ACTUAL API CALL - 4-7 seconds)
        Timber.d("Calling backend to generate meal plan...")
        val result = generateMealPlanUseCase()

        if (result.isFailure) {
            Timber.e(result.exceptionOrNull(), "Failed to generate meal plan during onboarding")
            throw result.exceptionOrNull() ?: Exception("Failed to generate meal plan")
        }

        Timber.i("Meal plan generated successfully: ${result.getOrNull()?.id}")

        _uiState.update {
            it.copy(
                generatingProgress = it.generatingProgress.copy(
                    generatingRecipes = false,
                    generatingRecipesDone = true,
                    buildingGroceryList = true
                )
            )
        }

        // Building grocery list (UI stage)
        delay(600)
        _uiState.update {
            it.copy(
                generatingProgress = it.generatingProgress.copy(
                    buildingGroceryList = false,
                    buildingGroceryListDone = true
                )
            )
        }
        delay(400)
    }

    /**
     * Simulate generating meal plan with progress (OLD METHOD - DEPRECATED).
     * Kept for reference but no longer used.
     *
     * @deprecated Use generateMealPlanWithProgress() instead which makes real API calls.
     */
    @Deprecated("Use generateMealPlanWithProgress() instead")
    private suspend fun simulateGeneratingProgress() {
        // Analyzing preferences
        _uiState.update {
            it.copy(generatingProgress = it.generatingProgress.copy(analyzingPreferences = true))
        }
        delay(800)
        _uiState.update {
            it.copy(
                generatingProgress = it.generatingProgress.copy(
                    analyzingPreferences = false,
                    analyzingPreferencesDone = true,
                    checkingFestivals = true
                )
            )
        }

        // Checking festivals
        delay(600)
        _uiState.update {
            it.copy(
                generatingProgress = it.generatingProgress.copy(
                    checkingFestivals = false,
                    checkingFestivalsDone = true,
                    generatingRecipes = true
                )
            )
        }

        // Generating recipes
        delay(1200)
        _uiState.update {
            it.copy(
                generatingProgress = it.generatingProgress.copy(
                    generatingRecipes = false,
                    generatingRecipesDone = true,
                    buildingGroceryList = true
                )
            )
        }

        // Building grocery list
        delay(600)
        _uiState.update {
            it.copy(
                generatingProgress = it.generatingProgress.copy(
                    buildingGroceryList = false,
                    buildingGroceryListDone = true
                )
            )
        }
        delay(400)
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // endregion
}
