package com.rasoiai.data.repository

import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.DietaryTag
import com.rasoiai.domain.model.Difficulty
import com.rasoiai.domain.model.FoodCategory
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
import com.rasoiai.domain.repository.RecipeRulesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake implementation of RecipeRulesRepository for development and testing.
 */
@Singleton
class FakeRecipeRulesRepository @Inject constructor() : RecipeRulesRepository {

    private val _rules = MutableStateFlow(createMockRules())
    private val _nutritionGoals = MutableStateFlow(createMockNutritionGoals())

    // region Recipe Rules

    override fun getAllRules(): Flow<List<RecipeRule>> = _rules.asStateFlow()

    override fun getRulesByType(type: RuleType): Flow<List<RecipeRule>> =
        _rules.map { rules -> rules.filter { it.type == type } }

    override fun getRuleById(ruleId: String): Flow<RecipeRule?> =
        _rules.map { rules -> rules.find { it.id == ruleId } }

    override fun getActiveRules(): Flow<List<RecipeRule>> =
        _rules.map { rules -> rules.filter { it.isActive } }

    override suspend fun createRule(rule: RecipeRule): Result<RecipeRule> {
        delay(300)
        val newRule = rule.copy(
            id = "rule-${System.currentTimeMillis()}",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        _rules.value = _rules.value + newRule
        return Result.success(newRule)
    }

    override suspend fun updateRule(rule: RecipeRule): Result<Unit> {
        delay(200)
        _rules.value = _rules.value.map {
            if (it.id == rule.id) rule.copy(updatedAt = LocalDateTime.now()) else it
        }
        return Result.success(Unit)
    }

    override suspend fun deleteRule(ruleId: String): Result<Unit> {
        delay(200)
        _rules.value = _rules.value.filter { it.id != ruleId }
        return Result.success(Unit)
    }

    override suspend fun toggleRuleActive(ruleId: String, isActive: Boolean): Result<Unit> {
        delay(100)
        _rules.value = _rules.value.map {
            if (it.id == ruleId) it.copy(isActive = isActive, updatedAt = LocalDateTime.now()) else it
        }
        return Result.success(Unit)
    }

    // endregion

    // region Nutrition Goals

    override fun getAllNutritionGoals(): Flow<List<NutritionGoal>> = _nutritionGoals.asStateFlow()

    override fun getNutritionGoalById(goalId: String): Flow<NutritionGoal?> =
        _nutritionGoals.map { goals -> goals.find { it.id == goalId } }

    override fun getActiveNutritionGoals(): Flow<List<NutritionGoal>> =
        _nutritionGoals.map { goals -> goals.filter { it.isActive } }

    override suspend fun createNutritionGoal(goal: NutritionGoal): Result<NutritionGoal> {
        delay(300)
        val newGoal = goal.copy(
            id = "goal-${System.currentTimeMillis()}",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        _nutritionGoals.value = _nutritionGoals.value + newGoal
        return Result.success(newGoal)
    }

    override suspend fun updateNutritionGoal(goal: NutritionGoal): Result<Unit> {
        delay(200)
        _nutritionGoals.value = _nutritionGoals.value.map {
            if (it.id == goal.id) goal.copy(updatedAt = LocalDateTime.now()) else it
        }
        return Result.success(Unit)
    }

    override suspend fun deleteNutritionGoal(goalId: String): Result<Unit> {
        delay(200)
        _nutritionGoals.value = _nutritionGoals.value.filter { it.id != goalId }
        return Result.success(Unit)
    }

    override suspend fun toggleNutritionGoalActive(goalId: String, isActive: Boolean): Result<Unit> {
        delay(100)
        _nutritionGoals.value = _nutritionGoals.value.map {
            if (it.id == goalId) it.copy(isActive = isActive, updatedAt = LocalDateTime.now()) else it
        }
        return Result.success(Unit)
    }

    override suspend fun updateNutritionGoalProgress(goalId: String, progress: Int): Result<Unit> {
        delay(100)
        _nutritionGoals.value = _nutritionGoals.value.map {
            if (it.id == goalId) it.copy(currentProgress = progress, updatedAt = LocalDateTime.now()) else it
        }
        return Result.success(Unit)
    }

    override suspend fun resetWeeklyProgress(): Result<Unit> {
        delay(100)
        _nutritionGoals.value = _nutritionGoals.value.map {
            it.copy(currentProgress = 0, updatedAt = LocalDateTime.now())
        }
        return Result.success(Unit)
    }

    // endregion

    // region Search & Suggestions

    override fun searchRecipes(query: String): Flow<List<Recipe>> =
        MutableStateFlow(
            mockRecipes.filter {
                it.name.contains(query, ignoreCase = true)
            }
        )

    override fun getPopularRecipes(): Flow<List<Recipe>> =
        MutableStateFlow(mockRecipes.take(8))

    override fun searchIngredients(query: String): Flow<List<String>> =
        MutableStateFlow(
            mockIngredients.filter {
                it.contains(query, ignoreCase = true)
            }
        )

    override fun getPopularIngredients(): Flow<List<String>> =
        MutableStateFlow(mockIngredients.take(10))

    override fun getAvailableFoodCategories(): Flow<List<FoodCategory>> =
        _nutritionGoals.map { goals ->
            val usedCategories = goals.map { it.foodCategory }.toSet()
            FoodCategory.entries.filter { it !in usedCategories }
        }

    // endregion

    // region Mock Data

    private fun createMockRules(): List<RecipeRule> = listOf(
        // Recipe Rules
        RecipeRule(
            id = "rule-1",
            type = RuleType.RECIPE,
            action = RuleAction.INCLUDE,
            targetId = "recipe-rajma",
            targetName = "Rajma",
            frequency = RuleFrequency.timesPerWeek(1),
            enforcement = RuleEnforcement.REQUIRED,
            isActive = true
        ),
        RecipeRule(
            id = "rule-2",
            type = RuleType.RECIPE,
            action = RuleAction.INCLUDE,
            targetId = "recipe-moringa",
            targetName = "Moringa Curry",
            frequency = RuleFrequency.timesPerWeek(3),
            enforcement = RuleEnforcement.PREFERRED,
            isActive = true
        ),
        RecipeRule(
            id = "rule-3",
            type = RuleType.RECIPE,
            action = RuleAction.INCLUDE,
            targetId = "recipe-chai",
            targetName = "Chai",
            frequency = RuleFrequency.DAILY,
            enforcement = RuleEnforcement.REQUIRED,
            mealSlot = MealType.BREAKFAST,
            isActive = true
        ),

        // Ingredient Rules
        RecipeRule(
            id = "rule-4",
            type = RuleType.INGREDIENT,
            action = RuleAction.INCLUDE,
            targetId = "ingredient-spinach",
            targetName = "Spinach",
            frequency = RuleFrequency.timesPerWeek(2),
            enforcement = RuleEnforcement.REQUIRED,
            isActive = true
        ),
        RecipeRule(
            id = "rule-5",
            type = RuleType.INGREDIENT,
            action = RuleAction.EXCLUDE,
            targetId = "ingredient-bitter-gourd",
            targetName = "Bitter Gourd",
            frequency = RuleFrequency.NEVER,
            enforcement = RuleEnforcement.REQUIRED,
            isActive = true
        ),

        // Meal-Slot Rules
        RecipeRule(
            id = "rule-6",
            type = RuleType.MEAL_SLOT,
            action = RuleAction.INCLUDE,
            targetId = "recipe-chai",
            targetName = "Chai",
            frequency = RuleFrequency.DAILY,
            enforcement = RuleEnforcement.REQUIRED,
            mealSlot = MealType.BREAKFAST,
            isActive = true
        ),
        RecipeRule(
            id = "rule-7",
            type = RuleType.MEAL_SLOT,
            action = RuleAction.INCLUDE,
            targetId = "recipe-dosa",
            targetName = "Dosa",
            frequency = RuleFrequency.specificDays(listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)),
            enforcement = RuleEnforcement.PREFERRED,
            mealSlot = MealType.BREAKFAST,
            isActive = true
        )
    )

    private fun createMockNutritionGoals(): List<NutritionGoal> = listOf(
        NutritionGoal(
            id = "goal-1",
            foodCategory = FoodCategory.GREEN_LEAFY,
            weeklyTarget = 7,
            currentProgress = 4,
            isActive = true
        ),
        NutritionGoal(
            id = "goal-2",
            foodCategory = FoodCategory.CITRUS_VITAMIN_C,
            weeklyTarget = 5,
            currentProgress = 2,
            isActive = true
        ),
        NutritionGoal(
            id = "goal-3",
            foodCategory = FoodCategory.IRON_RICH,
            weeklyTarget = 6,
            currentProgress = 5,
            isActive = true
        )
    )

    private val mockRecipes = listOf(
        createMockRecipe("recipe-rajma", "Rajma", "Punjabi kidney beans curry"),
        createMockRecipe("recipe-dal-makhani", "Dal Makhani", "Creamy black lentils"),
        createMockRecipe("recipe-chole", "Chole", "Spiced chickpea curry"),
        createMockRecipe("recipe-paneer-butter-masala", "Paneer Butter Masala", "Cottage cheese in tomato gravy"),
        createMockRecipe("recipe-palak-paneer", "Palak Paneer", "Cottage cheese in spinach gravy"),
        createMockRecipe("recipe-aloo-gobi", "Aloo Gobi", "Potato and cauliflower"),
        createMockRecipe("recipe-chai", "Chai", "Indian spiced tea"),
        createMockRecipe("recipe-dosa", "Dosa", "Crispy rice crepe"),
        createMockRecipe("recipe-idli", "Idli", "Steamed rice cakes"),
        createMockRecipe("recipe-poha", "Poha", "Flattened rice breakfast"),
        createMockRecipe("recipe-moringa", "Moringa Curry", "Drumstick leaves curry"),
        createMockRecipe("recipe-moringa-paratha", "Moringa Paratha", "Drumstick leaves flatbread"),
        createMockRecipe("recipe-sambar", "Sambar", "South Indian lentil stew")
    )

    private val mockIngredients = listOf(
        "Spinach",
        "Moringa",
        "Tomatoes",
        "Onions",
        "Garlic",
        "Ginger",
        "Paneer",
        "Potatoes",
        "Cauliflower",
        "Bitter Gourd",
        "Drumstick",
        "Curry Leaves",
        "Coriander",
        "Mint",
        "Turmeric",
        "Cumin",
        "Mustard Seeds",
        "Ghee",
        "Oil"
    )

    private fun createMockRecipe(id: String, name: String, description: String): Recipe = Recipe(
        id = id,
        name = name,
        description = description,
        imageUrl = null,
        prepTimeMinutes = 15,
        cookTimeMinutes = 30,
        servings = 4,
        difficulty = Difficulty.MEDIUM,
        cuisineType = CuisineType.NORTH,
        mealTypes = listOf(MealType.LUNCH, MealType.DINNER),
        dietaryTags = listOf(DietaryTag.VEGETARIAN),
        ingredients = emptyList(),
        instructions = emptyList(),
        nutrition = null,
        isFavorite = false
    )

    // endregion
}
