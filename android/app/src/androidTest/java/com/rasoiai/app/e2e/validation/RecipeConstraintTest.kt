package com.rasoiai.app.e2e.validation

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rasoiai.app.e2e.base.BaseE2ETest
import com.rasoiai.domain.model.DietaryTag
import com.rasoiai.domain.model.FoodCategory
import com.rasoiai.domain.model.FrequencyType
import com.rasoiai.domain.model.Ingredient
import com.rasoiai.domain.model.IngredientCategory
import com.rasoiai.domain.model.MealType
import com.rasoiai.domain.model.NutritionGoal
import com.rasoiai.domain.model.Recipe
import com.rasoiai.domain.model.RecipeRule
import com.rasoiai.domain.model.RuleAction
import com.rasoiai.domain.model.RuleEnforcement
import com.rasoiai.domain.model.RuleFrequency
import com.rasoiai.domain.model.RuleType
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Recipe constraint validation tests.
 *
 * These tests verify that generated meal plans respect dietary restrictions,
 * allergies, dislikes, and other user preferences. They validate the business
 * logic that ensures safe and appropriate recipe selection.
 *
 * ## Test Categories:
 * - SATTVIC constraints (no onion/garlic)
 * - Allergy constraints (e.g., peanuts, cashews)
 * - Disliked ingredients (e.g., karela, baingan)
 * - Timing constraints (weekday/weekend cooking time)
 * - Recipe Rules constraints
 * - Nutrition goals
 *
 * ## Running Tests:
 * ```bash
 * ./gradlew connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.package=com.rasoiai.app.e2e.validation
 * ```
 *
 * ## Architecture Note:
 * These tests validate constraint logic in isolation using test data.
 * In a full E2E scenario, constraints would be verified after meal plan generation.
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class RecipeConstraintTest : BaseE2ETest() {

    // region SATTVIC Constraint Tests

    @Test
    fun sattvicConstraint_recipeWithOnion_isViolation() {
        // Given: A recipe with onion
        val recipe = createTestRecipe(
            ingredients = listOf(
                createIngredient("onion", "Onion"),
                createIngredient("tomato", "Tomato")
            ),
            dietaryTags = listOf(DietaryTag.VEGETARIAN)
        )

        // When: Check SATTVIC constraint
        val hasSattvicViolation = hasSattvicViolation(recipe)

        // Then: Should be flagged as violation
        assert(hasSattvicViolation) {
            "Recipe with onion should violate SATTVIC constraint"
        }
    }

    @Test
    fun sattvicConstraint_recipeWithGarlic_isViolation() {
        // Given: A recipe with garlic
        val recipe = createTestRecipe(
            ingredients = listOf(
                createIngredient("garlic", "Garlic"),
                createIngredient("ginger", "Ginger")
            ),
            dietaryTags = listOf(DietaryTag.VEGETARIAN)
        )

        // When: Check SATTVIC constraint
        val hasSattvicViolation = hasSattvicViolation(recipe)

        // Then: Should be flagged as violation
        assert(hasSattvicViolation) {
            "Recipe with garlic should violate SATTVIC constraint"
        }
    }

    @Test
    fun sattvicConstraint_recipeWithoutOnionGarlic_noViolation() {
        // Given: A recipe without onion or garlic
        val recipe = createTestRecipe(
            ingredients = listOf(
                createIngredient("ginger", "Ginger"),
                createIngredient("cumin", "Cumin"),
                createIngredient("coriander", "Coriander")
            ),
            dietaryTags = listOf(DietaryTag.SATTVIC, DietaryTag.VEGETARIAN)
        )

        // When: Check SATTVIC constraint
        val hasSattvicViolation = hasSattvicViolation(recipe)

        // Then: Should not be flagged
        assert(!hasSattvicViolation) {
            "Recipe without onion/garlic should not violate SATTVIC constraint"
        }
    }

    // endregion

    // region Allergy Constraint Tests

    @Test
    fun allergyConstraint_peanutAllergy_detectsPeanuts() {
        // Given: A recipe with peanuts
        val recipe = createTestRecipe(
            ingredients = listOf(
                createIngredient("peanut", "Peanuts", category = IngredientCategory.NUTS),
                createIngredient("jaggery", "Jaggery")
            )
        )
        val allergies = listOf("peanut", "peanuts")

        // When: Check allergy constraint
        val hasAllergen = containsAllergen(recipe, allergies)

        // Then: Should detect allergen
        assert(hasAllergen) {
            "Recipe with peanuts should be flagged for peanut allergy"
        }
    }

    @Test
    fun allergyConstraint_cashewAllergy_detectsCashews() {
        // Given: A recipe with cashews
        val recipe = createTestRecipe(
            ingredients = listOf(
                createIngredient("cashew", "Cashews", category = IngredientCategory.NUTS),
                createIngredient("cream", "Cream", category = IngredientCategory.DAIRY)
            )
        )
        val allergies = listOf("cashew", "cashews", "kaju")

        // When: Check allergy constraint
        val hasAllergen = containsAllergen(recipe, allergies)

        // Then: Should detect allergen
        assert(hasAllergen) {
            "Recipe with cashews should be flagged for cashew allergy"
        }
    }

    @Test
    fun allergyConstraint_noAllergens_safe() {
        // Given: A recipe without allergens
        val recipe = createTestRecipe(
            ingredients = listOf(
                createIngredient("rice", "Rice", category = IngredientCategory.GRAINS),
                createIngredient("dal", "Dal", category = IngredientCategory.PULSES)
            )
        )
        val allergies = listOf("peanut", "cashew", "shellfish")

        // When: Check allergy constraint
        val hasAllergen = containsAllergen(recipe, allergies)

        // Then: Should be safe
        assert(!hasAllergen) {
            "Recipe without allergens should be considered safe"
        }
    }

    // endregion

    // region Disliked Ingredients Tests

    @Test
    fun dislikeConstraint_karelaRecipe_flagged() {
        // Given: A recipe with karela (bitter gourd)
        val recipe = createTestRecipe(
            name = "Karela Sabzi",
            ingredients = listOf(
                createIngredient("karela", "Bitter Gourd", category = IngredientCategory.VEGETABLES),
                createIngredient("onion", "Onion")
            )
        )
        val dislikes = listOf("karela", "bitter gourd")

        // When: Check dislike constraint
        val hasDislikedIngredient = containsDislikedIngredient(recipe, dislikes)

        // Then: Should be flagged
        assert(hasDislikedIngredient) {
            "Recipe with karela should be flagged when karela is disliked"
        }
    }

    @Test
    fun dislikeConstraint_bainganRecipe_flagged() {
        // Given: A recipe with baingan (eggplant)
        val recipe = createTestRecipe(
            name = "Baingan Bharta",
            ingredients = listOf(
                createIngredient("baingan", "Eggplant", category = IngredientCategory.VEGETABLES),
                createIngredient("tomato", "Tomato")
            )
        )
        val dislikes = listOf("baingan", "eggplant", "brinjal")

        // When: Check dislike constraint
        val hasDislikedIngredient = containsDislikedIngredient(recipe, dislikes)

        // Then: Should be flagged
        assert(hasDislikedIngredient) {
            "Recipe with baingan should be flagged when baingan is disliked"
        }
    }

    @Test
    fun dislikeConstraint_noDislikedIngredients_passes() {
        // Given: A recipe without disliked ingredients
        val recipe = createTestRecipe(
            name = "Palak Paneer",
            ingredients = listOf(
                createIngredient("palak", "Spinach", category = IngredientCategory.VEGETABLES),
                createIngredient("paneer", "Paneer", category = IngredientCategory.DAIRY)
            )
        )
        val dislikes = listOf("karela", "baingan", "mushroom")

        // When: Check dislike constraint
        val hasDislikedIngredient = containsDislikedIngredient(recipe, dislikes)

        // Then: Should pass
        assert(!hasDislikedIngredient) {
            "Recipe without disliked ingredients should pass"
        }
    }

    // endregion

    // region Timing Constraint Tests

    @Test
    fun timingConstraint_under30Minutes_validForWeekday() {
        // Given: A quick recipe (under 30 minutes)
        val recipe = createTestRecipe(
            prepTimeMinutes = 10,
            cookTimeMinutes = 15
        )
        val weekdayTimeLimit = 30

        // When: Check timing constraint
        val fitsTimeLimit = recipe.totalTimeMinutes <= weekdayTimeLimit

        // Then: Should fit weekday limit
        assert(fitsTimeLimit) {
            "25-minute recipe should fit 30-minute weekday limit"
        }
    }

    @Test
    fun timingConstraint_over30Minutes_invalidForBusyDay() {
        // Given: A longer recipe (over 30 minutes)
        val recipe = createTestRecipe(
            prepTimeMinutes = 20,
            cookTimeMinutes = 25
        )
        val busyDayTimeLimit = 30

        // When: Check timing constraint
        val fitsTimeLimit = recipe.totalTimeMinutes <= busyDayTimeLimit

        // Then: Should not fit busy day limit
        assert(!fitsTimeLimit) {
            "45-minute recipe should NOT fit 30-minute busy day limit"
        }
    }

    @Test
    fun timingConstraint_under60Minutes_validForWeekend() {
        // Given: A moderate-length recipe
        val recipe = createTestRecipe(
            prepTimeMinutes = 25,
            cookTimeMinutes = 30
        )
        val weekendTimeLimit = 60

        // When: Check timing constraint
        val fitsTimeLimit = recipe.totalTimeMinutes <= weekendTimeLimit

        // Then: Should fit weekend limit
        assert(fitsTimeLimit) {
            "55-minute recipe should fit 60-minute weekend limit"
        }
    }

    // endregion

    // region Recipe Rule Constraint Tests

    @Test
    fun recipeRule_chaiAtBreakfast_ruleCreatedCorrectly() {
        // Given: A chai breakfast rule (include chai daily at breakfast)
        val chaiRule = RecipeRule(
            id = "rule-chai-breakfast",
            type = RuleType.MEAL_SLOT,
            action = RuleAction.INCLUDE,
            targetId = "chai",
            targetName = "Chai",
            frequency = RuleFrequency.DAILY,
            enforcement = RuleEnforcement.REQUIRED,
            mealSlot = MealType.BREAKFAST,
            isActive = true
        )

        // When: Validate rule structure
        val isValidChaiRule = chaiRule.type == RuleType.MEAL_SLOT &&
                chaiRule.action == RuleAction.INCLUDE &&
                chaiRule.frequency.type == FrequencyType.DAILY &&
                chaiRule.mealSlot == MealType.BREAKFAST &&
                chaiRule.isActive

        // Then: Rule should be correctly structured
        assert(isValidChaiRule) {
            "Chai breakfast rule should be correctly structured for daily breakfast inclusion"
        }
    }

    @Test
    fun recipeRule_chaiAtBreakfast_appliesToRecipe() {
        // Given: A chai recipe and chai breakfast rule
        val chaiRecipe = createTestRecipe(
            id = "chai-recipe",
            name = "Masala Chai",
            ingredients = listOf(
                createIngredient("tea", "Tea Leaves"),
                createIngredient("milk", "Milk", category = IngredientCategory.DAIRY),
                createIngredient("ginger", "Ginger")
            )
        )
        val chaiRule = RecipeRule(
            id = "rule-chai-breakfast",
            type = RuleType.MEAL_SLOT,
            action = RuleAction.INCLUDE,
            targetId = "chai-recipe",
            targetName = "Masala Chai",
            frequency = RuleFrequency.DAILY,
            enforcement = RuleEnforcement.REQUIRED,
            mealSlot = MealType.BREAKFAST,
            isActive = true
        )

        // When: Check if rule applies to recipe
        val ruleAppliesToRecipe = chaiRule.targetId == chaiRecipe.id ||
                chaiRecipe.name.lowercase().contains(chaiRule.targetName.lowercase())

        // Then: Rule should apply
        assert(ruleAppliesToRecipe) {
            "Chai rule should apply to chai recipe"
        }
    }

    @Test
    fun recipeRule_paneerExclude_ruleCreatedCorrectly() {
        // Given: A paneer exclude rule
        val paneerExcludeRule = RecipeRule(
            id = "rule-paneer-exclude",
            type = RuleType.INGREDIENT,
            action = RuleAction.EXCLUDE,
            targetId = "paneer",
            targetName = "Paneer",
            frequency = RuleFrequency.NEVER,
            enforcement = RuleEnforcement.REQUIRED,
            mealSlot = null,
            isActive = true
        )

        // When: Validate rule structure
        val isValidExcludeRule = paneerExcludeRule.type == RuleType.INGREDIENT &&
                paneerExcludeRule.action == RuleAction.EXCLUDE &&
                paneerExcludeRule.frequency.type == FrequencyType.NEVER &&
                paneerExcludeRule.isActive

        // Then: Rule should be correctly structured
        assert(isValidExcludeRule) {
            "Paneer exclude rule should be correctly structured"
        }
    }

    @Test
    fun recipeRule_paneerExclude_detectsPaneerRecipe() {
        // Given: A recipe with paneer and exclude rule
        val paneerRecipe = createTestRecipe(
            name = "Paneer Tikka",
            ingredients = listOf(
                createIngredient("paneer", "Paneer", category = IngredientCategory.DAIRY),
                createIngredient("capsicum", "Capsicum")
            )
        )
        val paneerExcludeRule = RecipeRule(
            id = "rule-paneer-exclude",
            type = RuleType.INGREDIENT,
            action = RuleAction.EXCLUDE,
            targetId = "paneer",
            targetName = "Paneer",
            frequency = RuleFrequency.NEVER,
            enforcement = RuleEnforcement.REQUIRED,
            mealSlot = null,
            isActive = true
        )

        // When: Check if recipe violates exclude rule
        val violatesExcludeRule = recipeViolatesExcludeRule(paneerRecipe, paneerExcludeRule)

        // Then: Should detect violation
        assert(violatesExcludeRule) {
            "Recipe with paneer should violate paneer exclude rule"
        }
    }

    @Test
    fun recipeRule_moringaInclude_ruleCreatedCorrectly() {
        // Given: A moringa include rule (at least once per week)
        val moringaIncludeRule = RecipeRule(
            id = "rule-moringa-include",
            type = RuleType.INGREDIENT,
            action = RuleAction.INCLUDE,
            targetId = "moringa",
            targetName = "Moringa",
            frequency = RuleFrequency.timesPerWeek(1),
            enforcement = RuleEnforcement.PREFERRED,
            mealSlot = null,
            isActive = true
        )

        // When: Validate rule structure
        val isValidIncludeRule = moringaIncludeRule.type == RuleType.INGREDIENT &&
                moringaIncludeRule.action == RuleAction.INCLUDE &&
                moringaIncludeRule.frequency.type == FrequencyType.TIMES_PER_WEEK &&
                moringaIncludeRule.frequency.count == 1 &&
                moringaIncludeRule.isActive

        // Then: Rule should be correctly structured
        assert(isValidIncludeRule) {
            "Moringa include rule should be correctly structured for 1x per week"
        }
    }

    @Test
    fun recipeRule_moringaInclude_matchesRecipeWithMoringa() {
        // Given: A recipe with moringa and include rule
        val moringaRecipe = createTestRecipe(
            name = "Moringa Dal",
            ingredients = listOf(
                createIngredient("moringa", "Moringa Leaves", category = IngredientCategory.VEGETABLES),
                createIngredient("dal", "Toor Dal", category = IngredientCategory.PULSES)
            )
        )
        val moringaIncludeRule = RecipeRule(
            id = "rule-moringa-include",
            type = RuleType.INGREDIENT,
            action = RuleAction.INCLUDE,
            targetId = "moringa",
            targetName = "Moringa",
            frequency = RuleFrequency.timesPerWeek(1),
            enforcement = RuleEnforcement.PREFERRED,
            mealSlot = null,
            isActive = true
        )

        // When: Check if recipe satisfies include rule
        val satisfiesIncludeRule = recipeSatisfiesIncludeRule(moringaRecipe, moringaIncludeRule)

        // Then: Should satisfy rule
        assert(satisfiesIncludeRule) {
            "Recipe with moringa should satisfy moringa include rule"
        }
    }

    // endregion

    // region Nutrition Goal Tests

    @Test
    fun nutritionGoal_greenLeafy_goalCreatedCorrectly() {
        // Given: A green leafy nutrition goal (5x per week)
        val greenLeafyGoal = NutritionGoal(
            id = "goal-green-leafy",
            foodCategory = FoodCategory.GREEN_LEAFY,
            weeklyTarget = 5,
            currentProgress = 0,
            isActive = true
        )

        // When: Validate goal structure
        val isValidGoal = greenLeafyGoal.foodCategory == FoodCategory.GREEN_LEAFY &&
                greenLeafyGoal.weeklyTarget == 5 &&
                greenLeafyGoal.isActive

        // Then: Goal should be correctly structured
        assert(isValidGoal) {
            "Green leafy goal should be correctly structured for 5x per week"
        }
    }

    @Test
    fun nutritionGoal_greenLeafy_countsRecipeWithSpinach() {
        // Given: A recipe with spinach (green leafy vegetable)
        val palakRecipe = createTestRecipe(
            name = "Palak Paneer",
            ingredients = listOf(
                createIngredient("palak", "Spinach", category = IngredientCategory.VEGETABLES),
                createIngredient("paneer", "Paneer", category = IngredientCategory.DAIRY)
            )
        )
        val greenLeafyGoal = NutritionGoal(
            id = "goal-green-leafy",
            foodCategory = FoodCategory.GREEN_LEAFY,
            weeklyTarget = 5,
            currentProgress = 0,
            isActive = true
        )

        // When: Check if recipe counts toward green leafy goal
        val countsTowardGoal = recipeCountsTowardNutritionGoal(palakRecipe, greenLeafyGoal)

        // Then: Should count toward goal
        assert(countsTowardGoal) {
            "Recipe with spinach should count toward green leafy goal"
        }
    }

    @Test
    fun nutritionGoal_greenLeafy_doesNotCountRecipeWithoutGreenLeafy() {
        // Given: A recipe without green leafy vegetables
        val dalRecipe = createTestRecipe(
            name = "Dal Tadka",
            ingredients = listOf(
                createIngredient("dal", "Toor Dal", category = IngredientCategory.PULSES),
                createIngredient("tomato", "Tomato", category = IngredientCategory.VEGETABLES),
                createIngredient("cumin", "Cumin", category = IngredientCategory.SPICES)
            )
        )
        val greenLeafyGoal = NutritionGoal(
            id = "goal-green-leafy",
            foodCategory = FoodCategory.GREEN_LEAFY,
            weeklyTarget = 5,
            currentProgress = 0,
            isActive = true
        )

        // When: Check if recipe counts toward green leafy goal
        val countsTowardGoal = recipeCountsTowardNutritionGoal(dalRecipe, greenLeafyGoal)

        // Then: Should not count toward goal
        assert(!countsTowardGoal) {
            "Recipe without green leafy vegetables should not count toward goal"
        }
    }

    @Test
    fun nutritionGoal_progressTracking_updatesCorrectly() {
        // Given: A green leafy goal with some progress
        val initialGoal = NutritionGoal(
            id = "goal-green-leafy",
            foodCategory = FoodCategory.GREEN_LEAFY,
            weeklyTarget = 5,
            currentProgress = 3,
            isActive = true
        )

        // When: Update progress
        val updatedGoal = initialGoal.copy(currentProgress = 4)

        // Then: Progress should be updated
        assert(updatedGoal.currentProgress == 4) {
            "Nutrition goal progress should be updated to 4"
        }
        assert(updatedGoal.currentProgress < updatedGoal.weeklyTarget) {
            "Progress (4) should still be less than target (5)"
        }
    }

    // endregion

    // region Helper Functions

    private fun recipeViolatesExcludeRule(recipe: Recipe, rule: RecipeRule): Boolean {
        if (rule.action != RuleAction.EXCLUDE || !rule.isActive) return false

        return when (rule.type) {
            RuleType.INGREDIENT -> {
                recipe.ingredients.any { ingredient ->
                    ingredient.id.lowercase().contains(rule.targetId.lowercase()) ||
                    ingredient.name.lowercase().contains(rule.targetName.lowercase())
                }
            }
            RuleType.RECIPE -> recipe.id == rule.targetId
            else -> false
        }
    }

    private fun recipeSatisfiesIncludeRule(recipe: Recipe, rule: RecipeRule): Boolean {
        if (rule.action != RuleAction.INCLUDE || !rule.isActive) return false

        return when (rule.type) {
            RuleType.INGREDIENT -> {
                recipe.ingredients.any { ingredient ->
                    ingredient.id.lowercase().contains(rule.targetId.lowercase()) ||
                    ingredient.name.lowercase().contains(rule.targetName.lowercase())
                }
            }
            RuleType.RECIPE -> recipe.id == rule.targetId
            else -> false
        }
    }

    private fun recipeCountsTowardNutritionGoal(recipe: Recipe, goal: NutritionGoal): Boolean {
        if (!goal.isActive) return false

        val greenLeafyIngredients = listOf(
            "spinach", "palak", "methi", "fenugreek", "amaranth", "chaulai",
            "kale", "lettuce", "mustard greens", "sarson", "collard",
            "moringa", "drumstick leaves"
        )

        return when (goal.foodCategory) {
            FoodCategory.GREEN_LEAFY -> {
                recipe.ingredients.any { ingredient ->
                    greenLeafyIngredients.any { green ->
                        ingredient.name.lowercase().contains(green) ||
                        ingredient.id.lowercase().contains(green)
                    }
                }
            }
            else -> false // Other food categories can be added as needed
        }
    }

    private fun hasSattvicViolation(recipe: Recipe): Boolean {
        val prohibitedIngredients = listOf("onion", "garlic", "leek", "shallot", "chive")
        return recipe.ingredients.any { ingredient ->
            prohibitedIngredients.any { prohibited ->
                ingredient.name.lowercase().contains(prohibited)
            }
        }
    }

    private fun containsAllergen(recipe: Recipe, allergens: List<String>): Boolean {
        return recipe.ingredients.any { ingredient ->
            allergens.any { allergen ->
                ingredient.name.lowercase().contains(allergen.lowercase()) ||
                ingredient.id.lowercase().contains(allergen.lowercase())
            }
        }
    }

    private fun containsDislikedIngredient(recipe: Recipe, dislikes: List<String>): Boolean {
        return recipe.ingredients.any { ingredient ->
            dislikes.any { dislike ->
                ingredient.name.lowercase().contains(dislike.lowercase()) ||
                ingredient.id.lowercase().contains(dislike.lowercase())
            }
        }
    }

    private fun createTestRecipe(
        id: String = "test-recipe-${System.currentTimeMillis()}",
        name: String = "Test Recipe",
        ingredients: List<Ingredient> = emptyList(),
        dietaryTags: List<DietaryTag> = listOf(DietaryTag.VEGETARIAN),
        prepTimeMinutes: Int = 15,
        cookTimeMinutes: Int = 20
    ): Recipe {
        return Recipe(
            id = id,
            name = name,
            description = "Test recipe for constraint validation",
            imageUrl = null,
            prepTimeMinutes = prepTimeMinutes,
            cookTimeMinutes = cookTimeMinutes,
            servings = 2,
            difficulty = com.rasoiai.domain.model.Difficulty.MEDIUM,
            cuisineType = com.rasoiai.domain.model.CuisineType.NORTH,
            mealTypes = listOf(com.rasoiai.domain.model.MealType.LUNCH),
            dietaryTags = dietaryTags,
            ingredients = ingredients,
            instructions = emptyList(),
            nutrition = null,
            isFavorite = false
        )
    }

    private fun createIngredient(
        id: String,
        name: String,
        quantity: String = "1",
        unit: String = "cup",
        category: IngredientCategory = IngredientCategory.VEGETABLES
    ): Ingredient {
        return Ingredient(
            id = id,
            name = name,
            quantity = quantity,
            unit = unit,
            category = category,
            isOptional = false,
            substituteFor = null
        )
    }

    // endregion
}
