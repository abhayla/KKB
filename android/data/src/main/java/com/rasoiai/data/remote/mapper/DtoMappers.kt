package com.rasoiai.data.remote.mapper

import com.rasoiai.data.remote.dto.AuthResponse
import com.rasoiai.data.remote.dto.FestivalDto
import com.rasoiai.data.remote.dto.IngredientDto
import com.rasoiai.data.remote.dto.InstructionDto
import com.rasoiai.data.remote.dto.MealItemDto
import com.rasoiai.data.remote.dto.MealPlanDayDto
import com.rasoiai.data.remote.dto.MealPlanResponse
import com.rasoiai.data.remote.dto.NutritionDto
import com.rasoiai.data.remote.dto.RecipeResponse
import com.rasoiai.data.remote.dto.UserPreferencesDto
import com.rasoiai.data.remote.dto.UserResponse
import com.rasoiai.domain.model.CookingTimePreference
import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.DietaryTag
import com.rasoiai.domain.model.Difficulty
import com.rasoiai.domain.model.Festival
import com.rasoiai.domain.model.Ingredient
import com.rasoiai.domain.model.IngredientCategory
import com.rasoiai.domain.model.Instruction
import com.rasoiai.domain.model.MealItem
import com.rasoiai.domain.model.MealPlan
import com.rasoiai.domain.model.MealPlanDay
import com.rasoiai.domain.model.MealType
import com.rasoiai.domain.model.Nutrition
import com.rasoiai.domain.model.PrimaryDiet
import com.rasoiai.domain.model.Recipe
import com.rasoiai.domain.model.SpiceLevel
import com.rasoiai.domain.model.User
import com.rasoiai.domain.model.UserPreferences
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

// ==================== Recipe Mappers ====================

fun RecipeResponse.toDomain(): Recipe = Recipe(
    id = id,
    name = name,
    description = description,
    imageUrl = imageUrl,
    prepTimeMinutes = prepTimeMinutes,
    cookTimeMinutes = cookTimeMinutes,
    servings = servings,
    difficulty = Difficulty.fromValue(difficulty),
    cuisineType = CuisineType.fromValue(cuisineType),
    mealTypes = mealTypes.mapNotNull { MealType.fromValue(it) },
    dietaryTags = dietaryTags.mapNotNull { DietaryTag.fromValue(it) },
    ingredients = ingredients.map { it.toDomain() },
    instructions = instructions.map { it.toDomain() },
    nutrition = nutrition?.toDomain()
)

fun IngredientDto.toDomain(): Ingredient = Ingredient(
    id = id,
    name = name,
    quantity = quantity,
    unit = unit,
    category = IngredientCategory.fromValue(category),
    isOptional = isOptional,
    substituteFor = substituteFor
)

fun InstructionDto.toDomain(): Instruction = Instruction(
    stepNumber = stepNumber,
    instruction = instruction,
    durationMinutes = durationMinutes,
    timerRequired = timerRequired,
    tips = tips
)

fun NutritionDto.toDomain(): Nutrition = Nutrition(
    calories = calories,
    proteinGrams = protein,
    carbohydratesGrams = carbohydrates,
    fatGrams = fat,
    fiberGrams = fiber,
    sugarGrams = sugar,
    sodiumMg = sodium
)

// ==================== MealPlan Mappers ====================

fun MealPlanResponse.toDomain(): MealPlan {
    val dateFormatter = DateTimeFormatter.ISO_DATE
    val dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME

    return MealPlan(
        id = id,
        weekStartDate = LocalDate.parse(weekStartDate, dateFormatter),
        weekEndDate = LocalDate.parse(weekEndDate, dateFormatter),
        days = days.map { it.toDomain() },
        createdAt = parseTimestamp(createdAt),
        updatedAt = parseTimestamp(updatedAt)
    )
}

fun MealPlanDayDto.toDomain(): MealPlanDay {
    val dateFormatter = DateTimeFormatter.ISO_DATE

    return MealPlanDay(
        date = LocalDate.parse(date, dateFormatter),
        dayName = dayName,
        breakfast = meals.breakfast.map { it.toDomain() },
        lunch = meals.lunch.map { it.toDomain() },
        dinner = meals.dinner.map { it.toDomain() },
        snacks = meals.snacks.map { it.toDomain() },
        festival = festival?.toDomain()
    )
}

fun MealItemDto.toDomain(): MealItem = MealItem(
    id = id,
    recipeId = recipeId,
    recipeName = recipeName,
    recipeImageUrl = recipeImageUrl,
    prepTimeMinutes = prepTimeMinutes,
    calories = calories,
    isLocked = isLocked,
    order = order,
    dietaryTags = dietaryTags.mapNotNull { DietaryTag.fromValue(it) }
)

fun FestivalDto.toDomain(): Festival {
    // Festival date comes with the meal plan day, so we parse it from context
    // For now, use current date as placeholder - actual date comes from MealPlanDay
    return Festival(
        id = id,
        name = name,
        date = LocalDate.now(), // Will be set from parent context
        isFastingDay = isFastingDay,
        suggestedDishes = suggestedDishes ?: emptyList()
    )
}

// ==================== User Mappers ====================

fun UserResponse.toDomain(): User = User(
    id = id,
    email = email,
    name = name,
    profileImageUrl = profileImageUrl,
    isOnboarded = isOnboarded,
    preferences = preferences?.toDomain()
)

fun UserPreferencesDto.toDomain(): UserPreferences = UserPreferences(
    householdSize = householdSize,
    familyMembers = emptyList(), // Family members come from separate API
    primaryDiet = when {
        dietaryRestrictions.contains("vegetarian") -> PrimaryDiet.VEGETARIAN
        dietaryRestrictions.contains("eggetarian") -> PrimaryDiet.EGGETARIAN
        else -> PrimaryDiet.NON_VEGETARIAN
    },
    dietaryRestrictions = emptyList(), // Mapped separately
    cuisinePreferences = cuisinePreferences.map { CuisineType.fromValue(it) },
    spiceLevel = SpiceLevel.fromValue(spiceLevel),
    dislikedIngredients = dislikedIngredients,
    weekdayCookingTimeMinutes = CookingTimePreference.fromValue(cookingTimePreference).maxMinutes.coerceAtMost(60),
    weekendCookingTimeMinutes = CookingTimePreference.fromValue(cookingTimePreference).maxMinutes.coerceAtMost(90),
    busyDays = emptyList()
)

fun AuthResponse.toUser(): User = user.toDomain()

// ==================== Helper Functions ====================

private fun parseTimestamp(timestamp: String): Long {
    return try {
        ZonedDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME).toInstant().toEpochMilli()
    } catch (e: Exception) {
        try {
            LocalDate.parse(timestamp, DateTimeFormatter.ISO_DATE).atStartOfDay()
                .toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
        } catch (e2: Exception) {
            System.currentTimeMillis()
        }
    }
}
