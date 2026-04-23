package com.rasoiai.app.presentation.recipedetail

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rasoiai.app.presentation.common.TestTags
import com.rasoiai.app.presentation.theme.RasoiAITheme
import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.DietaryTag
import com.rasoiai.domain.model.Difficulty
import com.rasoiai.domain.model.Ingredient
import com.rasoiai.domain.model.IngredientCategory
import com.rasoiai.domain.model.Instruction
import com.rasoiai.domain.model.MealType
import com.rasoiai.domain.model.Nutrition
import com.rasoiai.domain.model.Recipe
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI Tests for RecipeDetailScreen
 * Tests recipe details, scaling, ingredients, and cooking mode launch
 */
@RunWith(AndroidJUnit4::class)
class RecipeDetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // region Test Data Factories

    private fun createTestIngredient(
        id: String,
        name: String,
        quantity: String,
        unit: String,
        category: IngredientCategory = IngredientCategory.VEGETABLES
    ) = Ingredient(
        id = id,
        name = name,
        quantity = quantity,
        unit = unit,
        category = category
    )

    private fun createTestInstruction(
        stepNumber: Int,
        instruction: String,
        durationMinutes: Int? = null
    ) = Instruction(
        stepNumber = stepNumber,
        instruction = instruction,
        durationMinutes = durationMinutes,
        timerRequired = durationMinutes != null,
        tips = null
    )

    private fun createTestRecipe(
        id: String = "recipe_1",
        name: String = "Paneer Butter Masala",
        cuisineType: CuisineType = CuisineType.NORTH,
        dietaryTags: List<DietaryTag> = listOf(DietaryTag.VEGETARIAN),
        difficulty: Difficulty = Difficulty.MEDIUM,
        prepTimeMinutes: Int = 15,
        cookTimeMinutes: Int = 30,
        servings: Int = 4,
        ingredients: List<Ingredient> = listOf(
            createTestIngredient("1", "Paneer", "250", "grams", IngredientCategory.DAIRY),
            createTestIngredient("2", "Onion", "2", "medium", IngredientCategory.VEGETABLES),
            createTestIngredient("3", "Tomato", "3", "medium", IngredientCategory.VEGETABLES),
            createTestIngredient("4", "Butter", "50", "grams", IngredientCategory.DAIRY),
            createTestIngredient("5", "Cream", "100", "ml", IngredientCategory.DAIRY),
            createTestIngredient("6", "Kasuri Methi", "1", "tbsp", IngredientCategory.SPICES)
        ),
        instructions: List<Instruction> = listOf(
            createTestInstruction(1, "Cut paneer into cubes and lightly fry until golden."),
            createTestInstruction(2, "Blend onions and tomatoes into a smooth puree."),
            createTestInstruction(3, "Heat butter in a pan and add the puree. Cook for 10 minutes.", 10),
            createTestInstruction(4, "Add spices and cream. Simmer for 5 minutes.", 5),
            createTestInstruction(5, "Add paneer cubes and kasuri methi. Mix well and serve.")
        ),
        nutrition: Nutrition = Nutrition(
            calories = 450,
            proteinGrams = 18,
            carbohydratesGrams = 25,
            fatGrams = 32,
            fiberGrams = 4,
            sugarGrams = 6,
            sodiumMg = 520
        ),
        isFavorite: Boolean = false
    ) = Recipe(
        id = id,
        name = name,
        description = "Creamy and rich paneer curry with butter and cream",
        cuisineType = cuisineType,
        dietaryTags = dietaryTags,
        difficulty = difficulty,
        prepTimeMinutes = prepTimeMinutes,
        cookTimeMinutes = cookTimeMinutes,
        servings = servings,
        ingredients = ingredients,
        instructions = instructions,
        nutrition = nutrition,
        imageUrl = null,
        isFavorite = isFavorite,
        mealTypes = listOf(MealType.LUNCH, MealType.DINNER)
    )

    private fun createTestUiState(
        isLoading: Boolean = false,
        errorMessage: String? = null,
        recipe: Recipe? = createTestRecipe(),
        selectedServings: Int = 4,
        checkedIngredients: Set<String> = emptySet(),
        selectedTabIndex: Int = 0,
        lockState: RecipeLockState = RecipeLockState.NO_CONTEXT
    ): RecipeDetailUiState {
        val displayTags = RecipeDetailUiState.computeDisplayTags(recipe)
        val cuisineDisplayText = RecipeDetailUiState.computeCuisineDisplayText(recipe)

        return RecipeDetailUiState(
            isLoading = isLoading,
            errorMessage = errorMessage,
            recipe = recipe,
            selectedServings = selectedServings,
            scaledIngredients = recipe?.ingredients?.toImmutableList() ?: persistentListOf(),
            scaledNutrition = recipe?.nutrition,
            checkedIngredients = checkedIngredients.toImmutableSet(),
            selectedTabIndex = selectedTabIndex,
            lockState = lockState,
            displayTags = displayTags,
            cuisineDisplayText = cuisineDisplayText
        )
    }

    private fun Set<String>.toImmutableSet() = persistentSetOf<String>().addAll(this)

    // endregion

    // region Screen Display Tests

    @Test
    fun recipeDetailScreen_displaysScreenTag() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                RecipeDetailTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.RECIPE_DETAIL_SCREEN).assertIsDisplayed()
    }

    @Test
    fun recipeDetailScreen_displaysRecipeName() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                RecipeDetailTestContent(uiState = uiState)
            }
        }

        composeTestRule.onAllNodesWithText("Paneer Butter Masala", substring = true).onFirst().assertIsDisplayed()
    }

    @Test
    fun recipeDetailScreen_displaysBackButton() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                RecipeDetailTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun recipeDetailScreen_displaysFavoriteButton() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                RecipeDetailTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithContentDescription("Add to favorites").assertIsDisplayed()
    }

    @Test
    fun recipeDetailScreen_favoriteRecipe_displaysFilled() {
        val recipe = createTestRecipe(isFavorite = true)
        val uiState = createTestUiState(recipe = recipe)

        composeTestRule.setContent {
            RasoiAITheme {
                RecipeDetailTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithContentDescription("Remove from favorites").assertIsDisplayed()
    }

    // endregion

    // region Tab Tests

    @Test
    fun recipeDetailScreen_displaysIngredientsTab() {
        val uiState = createTestUiState(selectedTabIndex = 0)

        composeTestRule.setContent {
            RasoiAITheme {
                RecipeDetailTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("INGREDIENTS").assertIsDisplayed()
    }

    @Test
    fun recipeDetailScreen_displaysInstructionsTab() {
        val uiState = createTestUiState(selectedTabIndex = 0)

        composeTestRule.setContent {
            RasoiAITheme {
                RecipeDetailTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("INSTRUCTIONS").assertIsDisplayed()
    }

    @Test
    fun ingredientsTab_click_triggersTabSelection() {
        var selectedTab = -1
        val uiState = createTestUiState(selectedTabIndex = 1)

        composeTestRule.setContent {
            RasoiAITheme {
                RecipeDetailTestContent(
                    uiState = uiState,
                    onTabSelect = { selectedTab = it }
                )
            }
        }

        composeTestRule.onNodeWithText("INGREDIENTS").performClick()

        assert(selectedTab == 0) { "Ingredients tab callback was not triggered" }
    }

    @Test
    fun instructionsTab_click_triggersTabSelection() {
        var selectedTab = -1
        val uiState = createTestUiState(selectedTabIndex = 0)

        composeTestRule.setContent {
            RasoiAITheme {
                RecipeDetailTestContent(
                    uiState = uiState,
                    onTabSelect = { selectedTab = it }
                )
            }
        }

        composeTestRule.onNodeWithText("INSTRUCTIONS").performClick()

        assert(selectedTab == 1) { "Instructions tab callback was not triggered" }
    }

    // endregion

    // region Cooking Mode Tests

    @Test
    fun recipeDetailScreen_displaysCookingModeButton() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                RecipeDetailTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("START COOKING MODE", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun cookingModeButton_click_triggersCallback() {
        var cookingModeClicked = false
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                RecipeDetailTestContent(
                    uiState = uiState,
                    onStartCookingMode = { cookingModeClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("START COOKING MODE", substring = true)
            .performScrollTo()
            .performClick()

        assert(cookingModeClicked) { "Cooking mode callback was not triggered" }
    }

    // endregion

    // region Modify with AI Tests

    @Test
    fun recipeDetailScreen_displaysModifyWithAIButton() {
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                RecipeDetailTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Modify with AI", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun modifyWithAIButton_click_triggersCallback() {
        var modifyClicked = false
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                RecipeDetailTestContent(
                    uiState = uiState,
                    onModifyWithAI = { modifyClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Modify with AI", substring = true)
            .performScrollTo()
            .performClick()

        assert(modifyClicked) { "Modify with AI callback was not triggered" }
    }

    // endregion

    // region Navigation Tests

    @Test
    fun backButton_click_triggersNavigateBack() {
        var backClicked = false
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                RecipeDetailTestContent(
                    uiState = uiState,
                    onBackClick = { backClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()

        assert(backClicked) { "Back navigation callback was not triggered" }
    }

    @Test
    fun favoriteButton_click_triggersToggle() {
        var favoriteClicked = false
        val uiState = createTestUiState()

        composeTestRule.setContent {
            RasoiAITheme {
                RecipeDetailTestContent(
                    uiState = uiState,
                    onFavoriteClick = { favoriteClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Add to favorites").performClick()

        assert(favoriteClicked) { "Favorite toggle callback was not triggered" }
    }

    // endregion

    // region Loading/Error State Tests

    @Test
    fun recipeDetailScreen_loadingState_displaysScreen() {
        val uiState = createTestUiState(isLoading = true, recipe = null)

        composeTestRule.setContent {
            RasoiAITheme {
                RecipeDetailTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithTag(TestTags.RECIPE_DETAIL_SCREEN).assertIsDisplayed()
    }

    @Test
    fun recipeDetailScreen_errorState_displaysErrorMessage() {
        val uiState = createTestUiState(
            isLoading = false,
            recipe = null,
            errorMessage = "Recipe not found"
        )

        composeTestRule.setContent {
            RasoiAITheme {
                RecipeDetailTestContent(uiState = uiState)
            }
        }

        composeTestRule.onNodeWithText("Recipe not found").assertIsDisplayed()
    }

    // endregion

    // region Lock State Tests

    @Test
    fun recipeDetailScreen_lockedState_calculatedCorrectly() {
        val uiState = createTestUiState(lockState = RecipeLockState.LOCKED)

        assert(uiState.isLocked) { "Recipe should be locked" }
    }

    @Test
    fun recipeDetailScreen_unlockedState_calculatedCorrectly() {
        val uiState = createTestUiState(lockState = RecipeLockState.UNLOCKED)

        assert(!uiState.isLocked) { "Recipe should not be locked" }
    }

    @Test
    fun recipeDetailScreen_noContextState_calculatedCorrectly() {
        val uiState = createTestUiState(lockState = RecipeLockState.NO_CONTEXT)

        assert(!uiState.isLocked) { "Recipe should not be locked in no context" }
    }

    // endregion

    // region Data Verification Tests

    @Test
    fun recipeDetailScreen_hasRecipeData() {
        val uiState = createTestUiState()

        assert(uiState.recipe != null) { "Recipe should exist" }
        assert(uiState.recipe?.name == "Paneer Butter Masala") { "Recipe name should match" }
    }

    @Test
    fun recipeDetailScreen_hasIngredients() {
        val uiState = createTestUiState()

        assert(uiState.scaledIngredients.isNotEmpty()) { "Ingredients should exist" }
        assert(uiState.ingredientCount == 6) { "Should have 6 ingredients" }
    }

    @Test
    fun recipeDetailScreen_hasInstructions() {
        val uiState = createTestUiState()

        assert(uiState.recipe?.instructions?.isNotEmpty() == true) { "Instructions should exist" }
        assert(uiState.instructionCount == 5) { "Should have 5 instructions" }
    }

    @Test
    fun recipeDetailScreen_hasNutritionData() {
        val uiState = createTestUiState()

        assert(uiState.scaledNutrition != null) { "Nutrition should exist" }
        assert(uiState.scaledNutrition?.calories == 450) { "Calories should match" }
    }

    @Test
    fun recipeDetailScreen_totalTime_calculatedCorrectly() {
        val uiState = createTestUiState()

        assert(uiState.totalTimeMinutes == 45) { "Total time should be 45 minutes (15 + 30)" }
    }

    @Test
    fun recipeDetailScreen_isVegetarian_calculatedCorrectly() {
        val uiState = createTestUiState()

        assert(uiState.isVegetarian) { "Recipe should be vegetarian" }
    }

    @Test
    fun recipeDetailScreen_displayTags_computed() {
        val uiState = createTestUiState()

        assert(uiState.displayTags.isNotEmpty()) { "Display tags should exist" }
        assert(uiState.displayTags.any { it == "Vegetarian" }) { "Should include Vegetarian tag" }
    }

    @Test
    fun recipeDetailScreen_cuisineDisplayText_computed() {
        val uiState = createTestUiState()

        assert(uiState.cuisineDisplayText.contains("North")) { "Cuisine should include North" }
    }

    @Test
    fun recipeDetailScreen_allIngredientsChecked_calculatedCorrectly() {
        val recipe = createTestRecipe()
        val allIds = recipe.ingredients.map { it.id }.toSet()
        val uiState = createTestUiState(recipe = recipe, checkedIngredients = allIds)

        assert(uiState.allIngredientsChecked) { "All ingredients should be checked" }
    }

    // endregion
}

// region Test Composable Wrapper

@Composable
private fun RecipeDetailTestContent(
    uiState: RecipeDetailUiState,
    onBackClick: () -> Unit = {},
    onFavoriteClick: () -> Unit = {},
    onTabSelect: (Int) -> Unit = {},
    onServingsChange: (Int) -> Unit = {},
    onIngredientChecked: (String) -> Unit = {},
    onAddAllToGrocery: () -> Unit = {},
    onStartCookingMode: () -> Unit = {},
    onModifyWithAI: () -> Unit = {},
    onAddToCollection: (String) -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }

    RecipeDetailContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onFavoriteClick = onFavoriteClick,
        onTabSelect = onTabSelect,
        onServingsChange = onServingsChange,
        onIngredientChecked = onIngredientChecked,
        onAddAllToGrocery = onAddAllToGrocery,
        onStartCookingMode = onStartCookingMode,
        onModifyWithAI = onModifyWithAI,
        onAddToCollection = onAddToCollection
    )
}

// endregion
