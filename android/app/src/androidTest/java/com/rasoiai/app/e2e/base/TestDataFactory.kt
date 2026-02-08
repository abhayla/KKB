package com.rasoiai.app.e2e.base

import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.DietaryTag
import java.time.DayOfWeek

/**
 * Test data factory providing the "Sharma Family" test profile
 * as defined in E2E-Testing-Prompt.md.
 */
object TestDataFactory {

    /**
     * The Sharma Family test data profile.
     * Used across all E2E tests for consistent testing.
     */
    val sharmaFamily = FamilyTestData(
        email = "e2e-test@rasoiai.test",
        householdSize = 3,
        primaryDiet = DietaryTag.VEGETARIAN,
        dietaryRestrictions = listOf(DietaryTag.SATTVIC),
        cuisines = listOf(CuisineType.NORTH, CuisineType.SOUTH),
        spiceLevel = SpiceLevel.MEDIUM,
        members = listOf(
            FamilyMember(
                name = "Ramesh",
                type = MemberType.ADULT,
                age = 45,
                healthNeeds = listOf(HealthNeed.DIABETIC, HealthNeed.LOW_OIL)
            ),
            FamilyMember(
                name = "Sunita",
                type = MemberType.ADULT,
                age = 42,
                healthNeeds = listOf(HealthNeed.LOW_SALT)
            ),
            FamilyMember(
                name = "Aarav",
                type = MemberType.CHILD,
                age = 12,
                healthNeeds = listOf(HealthNeed.NO_SPICY)
            )
        ),
        dislikedIngredients = listOf("Karela", "Baingan", "Mushroom"),
        weekdayCookingTime = 30,
        weekendCookingTime = 60,
        busyDays = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
    )

    /**
     * Sharma Family non-vegetarian profile for onboarding verification (FR-014, Issue #52).
     * Uses NORTH + WEST cuisines, NON_VEGETARIAN diet, and different family members
     * from the original sharmaFamily to avoid breaking existing tests.
     */
    val sharmaFamilyNonVeg = FamilyTestData(
        email = "e2e-test@rasoiai.test",
        householdSize = 3,
        primaryDiet = DietaryTag.NON_VEGETARIAN,
        dietaryRestrictions = emptyList(),
        cuisines = listOf(CuisineType.NORTH, CuisineType.WEST),
        spiceLevel = SpiceLevel.MEDIUM,
        members = listOf(
            FamilyMember(
                name = "Priya Sharma",
                type = MemberType.ADULT,
                age = 35,
                healthNeeds = emptyList()
            ),
            FamilyMember(
                name = "Amit Sharma",
                type = MemberType.CHILD,
                age = 16,
                healthNeeds = emptyList()
            ),
            FamilyMember(
                name = "Dadi Sharma",
                type = MemberType.SENIOR,
                age = 68,
                healthNeeds = listOf(HealthNeed.DIABETIC, HealthNeed.LOW_SALT)
            )
        ),
        dislikedIngredients = listOf("Karela", "Baingan"),
        weekdayCookingTime = 30,
        weekendCookingTime = 60,
        busyDays = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)
    )

    /**
     * Sample chat messages for testing
     */
    object ChatMessages {
        const val DINNER_SUGGESTION = "What can I make for dinner tonight?"
        const val QUICK_BREAKFAST = "Suggest a quick breakfast recipe"
        const val DIABETIC_FRIENDLY = "I need a diabetic-friendly dessert"
    }

    /**
     * Sample recipe rules for testing
     */
    object RecipeRules {
        val includeDalTadka = RecipeRuleTestData(
            type = RuleType.RECIPE,
            targetName = "Dal Tadka",
            frequency = 2,
            frequencyType = FrequencyType.TIMES_PER_WEEK,
            mealSlot = listOf(MealSlot.LUNCH, MealSlot.DINNER),
            enforcement = RuleEnforcement.PREFERRED
        )

        val excludePaneer = RecipeRuleTestData(
            type = RuleType.INGREDIENT,
            targetName = "Paneer",
            frequency = 0,
            frequencyType = FrequencyType.NEVER,
            mealSlot = emptyList(),
            enforcement = RuleEnforcement.REQUIRED
        )

        val greenLeafyGoal = NutritionGoalTestData(
            foodCategory = FoodCategory.GREEN_LEAFY,
            weeklyTarget = 5,
            enforcement = RuleEnforcement.PREFERRED
        )

        // Sharma family specific rules
        val includeChaiBreakfast = RecipeRuleTestData(
            type = RuleType.INGREDIENT,
            targetName = "Chai",
            frequency = 1,
            frequencyType = FrequencyType.DAILY,
            mealSlot = listOf(MealSlot.BREAKFAST),
            enforcement = RuleEnforcement.REQUIRED
        )

        val includeChaiSnacks = RecipeRuleTestData(
            type = RuleType.INGREDIENT,
            targetName = "Chai",
            frequency = 1,
            frequencyType = FrequencyType.DAILY,
            mealSlot = listOf(MealSlot.SNACKS),
            enforcement = RuleEnforcement.REQUIRED
        )

        val includeMoringa = RecipeRuleTestData(
            type = RuleType.INGREDIENT,
            targetName = "Moringa",
            frequency = 1,
            frequencyType = FrequencyType.TIMES_PER_WEEK,
            mealSlot = emptyList(),
            enforcement = RuleEnforcement.PREFERRED
        )

        // FR-015 (Issue #53): Additional Sharma family rules for E2E verification
        val includeEggs = RecipeRuleTestData(
            type = RuleType.INGREDIENT,
            targetName = "Eggs",
            frequency = 4,
            frequencyType = FrequencyType.TIMES_PER_WEEK,
            mealSlot = emptyList(),
            enforcement = RuleEnforcement.PREFERRED
        )

        val includeChicken = RecipeRuleTestData(
            type = RuleType.INGREDIENT,
            targetName = "Chicken",
            frequency = 2,
            frequencyType = FrequencyType.TIMES_PER_WEEK,
            mealSlot = emptyList(),
            enforcement = RuleEnforcement.PREFERRED
        )
    }

    /**
     * Sample pantry items for testing
     */
    object PantryItems {
        val rice = PantryItemTestData(
            name = "Rice",
            category = "Grains",
            quantity = 2.0,
            unit = "kg",
            expiryDays = null
        )

        val milk = PantryItemTestData(
            name = "Milk",
            category = "Dairy",
            quantity = 1.0,
            unit = "L",
            expiryDays = 2  // Expiring soon
        )

        val yogurt = PantryItemTestData(
            name = "Yogurt",
            category = "Dairy",
            quantity = 500.0,
            unit = "g",
            expiryDays = -1  // Already expired
        )
    }

    /**
     * Edge case test values
     */
    object EdgeCases {
        const val MIN_AGE = 1
        const val MAX_AGE = 100
        const val MIN_HOUSEHOLD = 1
        const val MAX_HOUSEHOLD = 10
        const val MIN_COOKING_TIME = 15
        const val MAX_COOKING_TIME = 90
    }
}

// Data classes for test data

data class FamilyTestData(
    val email: String,
    val householdSize: Int,
    val primaryDiet: DietaryTag,
    val dietaryRestrictions: List<DietaryTag>,
    val cuisines: List<CuisineType>,
    val spiceLevel: SpiceLevel,
    val members: List<FamilyMember>,
    val dislikedIngredients: List<String>,
    val weekdayCookingTime: Int,
    val weekendCookingTime: Int,
    val busyDays: List<DayOfWeek>
)

data class FamilyMember(
    val name: String,
    val type: MemberType,
    val age: Int,
    val healthNeeds: List<HealthNeed>
)

enum class MemberType {
    ADULT, CHILD, SENIOR
}

enum class SpiceLevel {
    MILD, MEDIUM, SPICY, VERY_SPICY
}

enum class HealthNeed {
    DIABETIC, LOW_OIL, LOW_SALT, NO_SPICY, HIGH_PROTEIN, LOW_CARB
}

data class RecipeRuleTestData(
    val type: RuleType,
    val targetName: String,
    val frequency: Int,
    val frequencyType: FrequencyType,
    val mealSlot: List<MealSlot>,
    val enforcement: RuleEnforcement
)

enum class RuleType {
    RECIPE, INGREDIENT, MEAL_SLOT, NUTRITION
}

enum class FrequencyType {
    DAILY, TIMES_PER_WEEK, SPECIFIC_DAYS, NEVER
}

enum class MealSlot {
    BREAKFAST, LUNCH, DINNER, SNACKS
}

enum class RuleEnforcement {
    REQUIRED, PREFERRED
}

enum class FoodCategory {
    GREEN_LEAFY, CITRUS_VITAMIN_C, IRON_RICH, HIGH_PROTEIN, CALCIUM_RICH, FIBER_RICH, OMEGA_3, ANTIOXIDANT
}

data class NutritionGoalTestData(
    val foodCategory: FoodCategory,
    val weeklyTarget: Int,
    val enforcement: RuleEnforcement
)

data class PantryItemTestData(
    val name: String,
    val category: String,
    val quantity: Double,
    val unit: String,
    val expiryDays: Int?  // Days from today, null for no expiry
)
