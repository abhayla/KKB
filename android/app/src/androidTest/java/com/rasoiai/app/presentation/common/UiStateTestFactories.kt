package com.rasoiai.app.presentation.common

import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.DietaryTag
import com.rasoiai.domain.model.Difficulty
import com.rasoiai.domain.model.FamilyMember
import com.rasoiai.domain.model.Ingredient
import com.rasoiai.domain.model.IngredientCategory
import com.rasoiai.domain.model.Instruction
import com.rasoiai.domain.model.MealItem
import com.rasoiai.domain.model.MealPlan
import com.rasoiai.domain.model.MealPlanDay
import com.rasoiai.domain.model.MealType
import com.rasoiai.domain.model.MemberType
import com.rasoiai.domain.model.Recipe
import java.time.LocalDate

/**
 * Shared test factories for domain models used across UI screen tests.
 *
 * These provide sensible defaults for all fields, with named parameter overrides
 * for customization. Use these instead of creating per-file factory methods.
 *
 * ## Usage
 * ```kotlin
 * import com.rasoiai.app.presentation.common.UiStateTestFactories.mealItem
 * import com.rasoiai.app.presentation.common.UiStateTestFactories.recipe
 *
 * val item = mealItem(name = "Chai", prepTime = 10)
 * val fullRecipe = recipe(name = "Paneer Butter Masala", difficulty = Difficulty.MEDIUM)
 * ```
 */
object UiStateTestFactories {

    // region Meal Domain Models

    fun mealItem(
        id: String = "meal-1",
        recipeId: String = "recipe-1",
        name: String = "Dal Tadka",
        prepTime: Int = 30,
        calories: Int = 250,
        isLocked: Boolean = false,
        order: Int = 0,
        dietaryTags: List<DietaryTag> = listOf(DietaryTag.VEGETARIAN)
    ) = MealItem(
        id = id,
        recipeId = recipeId,
        recipeName = name,
        recipeImageUrl = null,
        prepTimeMinutes = prepTime,
        calories = calories,
        isLocked = isLocked,
        order = order,
        dietaryTags = dietaryTags
    )

    fun mealPlanDay(
        date: LocalDate = LocalDate.now(),
        breakfast: List<MealItem> = listOf(mealItem(id = "b1", name = "Poha")),
        lunch: List<MealItem> = listOf(mealItem(id = "l1", name = "Dal Tadka")),
        dinner: List<MealItem> = listOf(mealItem(id = "d1", name = "Paneer Tikka")),
        snacks: List<MealItem> = listOf(mealItem(id = "s1", name = "Samosa"))
    ) = MealPlanDay(
        date = date,
        dayName = date.dayOfWeek.name,
        breakfast = breakfast,
        lunch = lunch,
        dinner = dinner,
        snacks = snacks,
        festival = null
    )

    fun mealPlan(
        id: String = "plan-1",
        weekStartDate: LocalDate = LocalDate.now().minusDays(
            LocalDate.now().dayOfWeek.ordinal.toLong()
        ),
        days: List<MealPlanDay> = (0..6).map { offset ->
            mealPlanDay(date = weekStartDate.plusDays(offset.toLong()))
        }
    ) = MealPlan(
        id = id,
        weekStartDate = weekStartDate,
        weekEndDate = weekStartDate.plusDays(6),
        days = days,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )

    // endregion

    // region Recipe Domain Models

    fun ingredient(
        id: String = "1",
        name: String = "Paneer",
        quantity: String = "250",
        unit: String = "grams",
        category: IngredientCategory = IngredientCategory.DAIRY
    ) = Ingredient(
        id = id,
        name = name,
        quantity = quantity,
        unit = unit,
        category = category
    )

    fun instruction(
        stepNumber: Int = 1,
        text: String = "Cut paneer into cubes",
        durationMinutes: Int? = null
    ) = Instruction(
        stepNumber = stepNumber,
        instruction = text,
        durationMinutes = durationMinutes,
        timerRequired = durationMinutes != null,
        tips = null
    )

    fun recipe(
        id: String = "recipe-1",
        name: String = "Paneer Butter Masala",
        description: String = "Creamy tomato-based curry with paneer",
        cuisineType: CuisineType = CuisineType.NORTH,
        difficulty: Difficulty = Difficulty.MEDIUM,
        prepTimeMinutes: Int = 15,
        cookTimeMinutes: Int = 30,
        servings: Int = 4,
        dietaryTags: List<DietaryTag> = listOf(DietaryTag.VEGETARIAN),
        ingredients: List<Ingredient> = listOf(
            ingredient("1", "Paneer", "250", "grams"),
            ingredient("2", "Onion", "2", "medium", IngredientCategory.VEGETABLES),
            ingredient("3", "Tomato", "3", "medium", IngredientCategory.VEGETABLES)
        ),
        instructions: List<Instruction> = listOf(
            instruction(1, "Cut paneer into cubes and lightly fry until golden."),
            instruction(2, "Blend onions and tomatoes into a smooth puree."),
            instruction(3, "Cook puree with spices, add paneer, simmer.", 10)
        )
    ) = Recipe(
        id = id,
        name = name,
        description = description,
        imageUrl = null,
        prepTimeMinutes = prepTimeMinutes,
        cookTimeMinutes = cookTimeMinutes,
        servings = servings,
        difficulty = difficulty,
        cuisineType = cuisineType,
        mealTypes = listOf(MealType.LUNCH, MealType.DINNER),
        dietaryTags = dietaryTags,
        ingredients = ingredients,
        instructions = instructions,
        nutrition = null,
        isFavorite = false
    )

    // endregion

    // region Family Domain Models

    fun familyMember(
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

    // endregion
}
