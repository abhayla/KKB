package com.rasoiai.data.repository

import com.rasoiai.domain.model.DietaryTag
import com.rasoiai.domain.model.Festival
import com.rasoiai.domain.model.MealItem
import com.rasoiai.domain.model.MealPlan
import com.rasoiai.domain.model.MealPlanDay
import com.rasoiai.domain.model.MealType
import com.rasoiai.domain.repository.MealPlanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake implementation of MealPlanRepository for development and testing.
 * Provides sample Indian meal plan data.
 */
@Singleton
class FakeMealPlanRepository @Inject constructor() : MealPlanRepository {

    private val mealPlans = MutableStateFlow<List<MealPlan>>(emptyList())

    init {
        // Initialize with a sample meal plan for the current week
        val currentWeekPlan = generateSampleMealPlan(getWeekStartDate(LocalDate.now()))
        mealPlans.value = listOf(currentWeekPlan)
    }

    override fun getMealPlanForDate(date: LocalDate): Flow<MealPlan?> {
        return mealPlans.map { plans ->
            plans.find { plan ->
                !date.isBefore(plan.weekStartDate) && !date.isAfter(plan.weekEndDate)
            }
        }
    }

    override suspend fun generateMealPlan(weekStartDate: LocalDate): Result<MealPlan> {
        return try {
            val newPlan = generateSampleMealPlan(weekStartDate)
            mealPlans.value = mealPlans.value.filter {
                it.weekStartDate != weekStartDate
            } + newPlan
            Result.success(newPlan)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun swapMeal(
        mealPlanId: String,
        date: LocalDate,
        mealType: MealType,
        currentRecipeId: String,
        excludeRecipeIds: List<String>
    ): Result<MealPlan> {
        return try {
            val updatedPlans = mealPlans.value.map { plan ->
                if (plan.id == mealPlanId) {
                    plan.copy(
                        days = plan.days.map { day ->
                            if (day.date == date) {
                                swapMealInDay(day, mealType, currentRecipeId)
                            } else day
                        },
                        updatedAt = System.currentTimeMillis()
                    )
                } else plan
            }
            mealPlans.value = updatedPlans
            val updatedPlan = updatedPlans.find { it.id == mealPlanId }
                ?: return Result.failure(Exception("Meal plan not found"))
            Result.success(updatedPlan)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setMealLockState(
        mealPlanId: String,
        date: LocalDate,
        mealType: MealType,
        recipeId: String,
        isLocked: Boolean
    ): Result<Unit> {
        return try {
            val updatedPlans = mealPlans.value.map { plan ->
                if (plan.id == mealPlanId) {
                    plan.copy(
                        days = plan.days.map { day ->
                            if (day.date == date) {
                                updateMealLockState(day, mealType, recipeId, isLocked)
                            } else day
                        },
                        updatedAt = System.currentTimeMillis()
                    )
                } else plan
            }
            mealPlans.value = updatedPlans
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncMealPlans(): Result<Unit> {
        // Fake sync - always succeeds
        return Result.success(Unit)
    }

    private fun getWeekStartDate(date: LocalDate): LocalDate {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }

    private fun swapMealInDay(day: MealPlanDay, mealType: MealType, currentRecipeId: String): MealPlanDay {
        val alternatives = getAlternativeRecipes(mealType)
        val newRecipe = alternatives.firstOrNull { it.recipeId != currentRecipeId }
            ?: alternatives.first()

        fun updateMeals(meals: List<MealItem>): List<MealItem> {
            return meals.map { meal ->
                if (meal.recipeId == currentRecipeId && !meal.isLocked) newRecipe else meal
            }
        }

        return when (mealType) {
            MealType.BREAKFAST -> day.copy(breakfast = updateMeals(day.breakfast))
            MealType.LUNCH -> day.copy(lunch = updateMeals(day.lunch))
            MealType.DINNER -> day.copy(dinner = updateMeals(day.dinner))
            MealType.SNACKS -> day.copy(snacks = updateMeals(day.snacks))
        }
    }

    private fun updateMealLockState(
        day: MealPlanDay,
        mealType: MealType,
        recipeId: String,
        isLocked: Boolean
    ): MealPlanDay {
        fun updateMeals(meals: List<MealItem>): List<MealItem> {
            return meals.map { meal ->
                if (meal.recipeId == recipeId) meal.copy(isLocked = isLocked) else meal
            }
        }

        return when (mealType) {
            MealType.BREAKFAST -> day.copy(breakfast = updateMeals(day.breakfast))
            MealType.LUNCH -> day.copy(lunch = updateMeals(day.lunch))
            MealType.DINNER -> day.copy(dinner = updateMeals(day.dinner))
            MealType.SNACKS -> day.copy(snacks = updateMeals(day.snacks))
        }
    }

    private fun generateSampleMealPlan(weekStart: LocalDate): MealPlan {
        val days = (0..6).map { dayOffset ->
            val date = weekStart.plusDays(dayOffset.toLong())
            generateMealPlanDay(date)
        }

        return MealPlan(
            id = UUID.randomUUID().toString(),
            weekStartDate = weekStart,
            weekEndDate = weekStart.plusDays(6),
            days = days,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun generateMealPlanDay(date: LocalDate): MealPlanDay {
        val dayOfWeek = date.dayOfWeek
        val dayName = dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }

        // Check for upcoming festival
        val festival = getUpcomingFestival(date)

        return MealPlanDay(
            date = date,
            dayName = dayName,
            breakfast = getBreakfastMeals(dayOfWeek),
            lunch = getLunchMeals(dayOfWeek),
            dinner = getDinnerMeals(dayOfWeek),
            snacks = getSnacksMeals(dayOfWeek),
            festival = festival
        )
    }

    private fun getUpcomingFestival(date: LocalDate): Festival? {
        // Sample festivals - in real app this would come from a database
        val festivals = listOf(
            Festival(
                id = "makar-sankranti",
                name = "Makar Sankranti",
                date = LocalDate.of(date.year, 1, 14),
                isFastingDay = false,
                suggestedDishes = listOf("Til Ladoo", "Khichdi", "Pongal", "Undhiyu")
            ),
            Festival(
                id = "republic-day",
                name = "Republic Day",
                date = LocalDate.of(date.year, 1, 26),
                isFastingDay = false,
                suggestedDishes = listOf("Kheer", "Biryani", "Gulab Jamun")
            ),
            Festival(
                id = "holi",
                name = "Holi",
                date = LocalDate.of(date.year, 3, 14),
                isFastingDay = false,
                suggestedDishes = listOf("Gujiya", "Thandai", "Malpua", "Dahi Vada")
            ),
            Festival(
                id = "diwali",
                name = "Diwali",
                date = LocalDate.of(date.year, 11, 1),
                isFastingDay = false,
                suggestedDishes = listOf("Kaju Katli", "Samosa", "Aloo Tikki", "Kheer")
            )
        )

        // Return festival if it's within 3 days
        return festivals.find { festival ->
            val daysUntil = java.time.temporal.ChronoUnit.DAYS.between(date, festival.date)
            daysUntil in 0..3
        }
    }

    private fun getBreakfastMeals(dayOfWeek: DayOfWeek): List<MealItem> {
        val breakfasts = when (dayOfWeek) {
            DayOfWeek.MONDAY -> listOf(
                createMealItem("poha", "Poha", 20, 280, isLocked = true), // Pre-locked for testing
                createMealItem("chai", "Masala Chai", 5, 80)
            )
            DayOfWeek.TUESDAY -> listOf(
                createMealItem("idli", "Idli Sambar", 25, 320),
                createMealItem("filter-coffee", "Filter Coffee", 5, 60)
            )
            DayOfWeek.WEDNESDAY -> listOf(
                createMealItem("paratha", "Aloo Paratha", 30, 380),
                createMealItem("curd", "Dahi", 0, 60)
            )
            DayOfWeek.THURSDAY -> listOf(
                createMealItem("upma", "Rava Upma", 20, 260),
                createMealItem("chai", "Masala Chai", 5, 80)
            )
            DayOfWeek.FRIDAY -> listOf(
                createMealItem("dosa", "Masala Dosa", 25, 350),
                createMealItem("filter-coffee", "Filter Coffee", 5, 60)
            )
            DayOfWeek.SATURDAY -> listOf(
                createMealItem("chole-bhature", "Chole Bhature", 35, 520),
                createMealItem("lassi", "Sweet Lassi", 5, 120)
            )
            DayOfWeek.SUNDAY -> listOf(
                createMealItem("puri-sabzi", "Puri Aloo Sabzi", 40, 480),
                createMealItem("chai", "Masala Chai", 5, 80)
            )
        }
        return breakfasts.mapIndexed { index, item -> item.copy(order = index) }
    }

    private fun getLunchMeals(dayOfWeek: DayOfWeek): List<MealItem> {
        val lunches = when (dayOfWeek) {
            DayOfWeek.MONDAY -> listOf(
                createMealItem("dal-tadka", "Dal Tadka", 25, 180, isLocked = true), // Pre-locked for testing
                createMealItem("jeera-rice", "Jeera Rice", 15, 220),
                createMealItem("roti", "Roti (4)", 20, 320),
                createMealItem("salad", "Mixed Salad", 5, 50)
            )
            DayOfWeek.TUESDAY -> listOf(
                createMealItem("rajma", "Rajma Masala", 30, 220),
                createMealItem("plain-rice", "Steamed Rice", 15, 200),
                createMealItem("raita", "Boondi Raita", 10, 80)
            )
            DayOfWeek.WEDNESDAY -> listOf(
                createMealItem("paneer-bhurji", "Paneer Bhurji", 20, 280),
                createMealItem("roti", "Roti (4)", 20, 320),
                createMealItem("dal-fry", "Dal Fry", 20, 160)
            )
            DayOfWeek.THURSDAY -> listOf(
                createMealItem("chana-masala", "Chana Masala", 25, 200),
                createMealItem("bhatura", "Bhatura (2)", 15, 280),
                createMealItem("onion-salad", "Onion Salad", 5, 30)
            )
            DayOfWeek.FRIDAY -> listOf(
                createMealItem("sambar-rice", "Sambar Rice", 30, 380),
                createMealItem("papad", "Papad", 5, 60),
                createMealItem("pickle", "Mango Pickle", 0, 20)
            )
            DayOfWeek.SATURDAY -> listOf(
                createMealItem("biryani", "Veg Biryani", 45, 450),
                createMealItem("raita", "Cucumber Raita", 10, 80),
                createMealItem("mirchi-ka-salan", "Mirchi Ka Salan", 25, 180)
            )
            DayOfWeek.SUNDAY -> listOf(
                createMealItem("paneer-butter-masala", "Paneer Butter Masala", 35, 380),
                createMealItem("butter-naan", "Butter Naan (3)", 15, 420),
                createMealItem("dal-makhani", "Dal Makhani", 40, 280)
            )
        }
        return lunches.mapIndexed { index, item -> item.copy(order = index) }
    }

    private fun getDinnerMeals(dayOfWeek: DayOfWeek): List<MealItem> {
        val dinners = when (dayOfWeek) {
            DayOfWeek.MONDAY -> listOf(
                createMealItem("palak-paneer", "Palak Paneer", 30, 320),
                createMealItem("butter-naan", "Butter Naan (2)", 15, 280),
                createMealItem("raita", "Boondi Raita", 10, 80)
            )
            DayOfWeek.TUESDAY -> listOf(
                createMealItem("aloo-gobi", "Aloo Gobi", 25, 180),
                createMealItem("roti", "Roti (3)", 15, 240),
                createMealItem("dal", "Moong Dal", 20, 140)
            )
            DayOfWeek.WEDNESDAY -> listOf(
                createMealItem("bhindi-masala", "Bhindi Masala", 20, 160),
                createMealItem("roti", "Roti (3)", 15, 240),
                createMealItem("rice", "Plain Rice", 15, 180)
            )
            DayOfWeek.THURSDAY -> listOf(
                createMealItem("kadhi-pakora", "Kadhi Pakora", 35, 280),
                createMealItem("jeera-rice", "Jeera Rice", 15, 220),
                createMealItem("papad", "Roasted Papad", 5, 60)
            )
            DayOfWeek.FRIDAY -> listOf(
                createMealItem("matar-paneer", "Matar Paneer", 30, 340),
                createMealItem("paratha", "Plain Paratha (2)", 20, 280),
                createMealItem("pickle", "Mixed Pickle", 0, 20)
            )
            DayOfWeek.SATURDAY -> listOf(
                createMealItem("malai-kofta", "Malai Kofta", 40, 420),
                createMealItem("garlic-naan", "Garlic Naan (2)", 15, 300),
                createMealItem("raita", "Mix Veg Raita", 10, 90)
            )
            DayOfWeek.SUNDAY -> listOf(
                createMealItem("shahi-paneer", "Shahi Paneer", 35, 380),
                createMealItem("pulao", "Veg Pulao", 25, 320),
                createMealItem("gulab-jamun", "Gulab Jamun (2)", 0, 180)
            )
        }
        return dinners.mapIndexed { index, item -> item.copy(order = index) }
    }

    private fun getSnacksMeals(dayOfWeek: DayOfWeek): List<MealItem> {
        val snacks = when (dayOfWeek) {
            DayOfWeek.MONDAY -> listOf(
                createMealItem("samosa", "Samosa (2)", 0, 240),
                createMealItem("green-tea", "Green Tea", 5, 5)
            )
            DayOfWeek.TUESDAY -> listOf(
                createMealItem("pakora", "Mix Pakora", 15, 280),
                createMealItem("chai", "Masala Chai", 5, 80)
            )
            DayOfWeek.WEDNESDAY -> listOf(
                createMealItem("dhokla", "Khaman Dhokla", 0, 180),
                createMealItem("chai", "Ginger Tea", 5, 60)
            )
            DayOfWeek.THURSDAY -> listOf(
                createMealItem("bhel-puri", "Bhel Puri", 10, 200),
                createMealItem("nimbu-pani", "Nimbu Pani", 5, 40)
            )
            DayOfWeek.FRIDAY -> listOf(
                createMealItem("kachori", "Pyaaz Kachori (2)", 0, 320),
                createMealItem("chai", "Masala Chai", 5, 80)
            )
            DayOfWeek.SATURDAY -> listOf(
                createMealItem("pani-puri", "Pani Puri", 0, 180),
                createMealItem("lassi", "Mango Lassi", 5, 160)
            )
            DayOfWeek.SUNDAY -> listOf(
                createMealItem("aloo-tikki", "Aloo Tikki (2)", 15, 260),
                createMealItem("chai", "Masala Chai", 5, 80)
            )
        }
        return snacks.mapIndexed { index, item -> item.copy(order = index) }
    }

    private fun createMealItem(
        id: String,
        name: String,
        prepTimeMinutes: Int,
        calories: Int,
        isVeg: Boolean = true,
        isLocked: Boolean = false
    ): MealItem {
        return MealItem(
            id = UUID.randomUUID().toString(),
            recipeId = id,
            recipeName = name,
            recipeImageUrl = null,
            prepTimeMinutes = prepTimeMinutes,
            calories = calories,
            isLocked = isLocked,
            order = 0,
            dietaryTags = if (isVeg) listOf(DietaryTag.VEGETARIAN) else listOf(DietaryTag.NON_VEGETARIAN)
        )
    }

    private fun getAlternativeRecipes(mealType: MealType): List<MealItem> {
        return when (mealType) {
            MealType.BREAKFAST -> listOf(
                createMealItem("upma", "Rava Upma", 20, 260),
                createMealItem("poha", "Poha", 20, 280),
                createMealItem("paratha", "Aloo Paratha", 30, 380),
                createMealItem("idli", "Idli Sambar", 25, 320),
                createMealItem("dosa", "Plain Dosa", 20, 280)
            )
            MealType.LUNCH -> listOf(
                createMealItem("dal-tadka", "Dal Tadka", 25, 180),
                createMealItem("rajma", "Rajma Masala", 30, 220),
                createMealItem("chana-masala", "Chana Masala", 25, 200),
                createMealItem("kadhi", "Kadhi Pakora", 35, 280),
                createMealItem("paneer-bhurji", "Paneer Bhurji", 20, 280)
            )
            MealType.DINNER -> listOf(
                createMealItem("palak-paneer", "Palak Paneer", 30, 320),
                createMealItem("matar-paneer", "Matar Paneer", 30, 340),
                createMealItem("aloo-gobi", "Aloo Gobi", 25, 180),
                createMealItem("malai-kofta", "Malai Kofta", 40, 420),
                createMealItem("bhindi-masala", "Bhindi Masala", 20, 160)
            )
            MealType.SNACKS -> listOf(
                createMealItem("samosa", "Samosa (2)", 0, 240),
                createMealItem("pakora", "Mix Pakora", 15, 280),
                createMealItem("dhokla", "Khaman Dhokla", 0, 180),
                createMealItem("bhel-puri", "Bhel Puri", 10, 200),
                createMealItem("kachori", "Pyaaz Kachori", 0, 160)
            )
        }
    }
}
