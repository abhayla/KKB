package com.rasoiai.data.mapper

import com.rasoiai.data.local.entity.AchievementEntity
import com.rasoiai.data.local.entity.ChatMessageEntity
import com.rasoiai.data.local.entity.CookingDayEntity
import com.rasoiai.data.local.entity.CookingStreakEntity
import com.rasoiai.data.local.entity.FavoriteCollectionEntity
import com.rasoiai.data.local.entity.GroceryItemEntity
import com.rasoiai.data.local.entity.MealPlanEntity
import com.rasoiai.data.local.entity.MealPlanFestivalEntity
import com.rasoiai.data.local.entity.MealPlanItemEntity
import com.rasoiai.data.local.entity.NutritionGoalEntity
import com.rasoiai.data.local.entity.PantryItemEntity
import com.rasoiai.data.local.entity.RecipeEntity
import com.rasoiai.data.local.entity.RecipeRuleEntity
import com.rasoiai.data.local.entity.WeeklyChallengeEntity
import com.rasoiai.data.local.mapper.toDomain
import com.rasoiai.data.local.mapper.toEntity
import com.rasoiai.data.local.mapper.toGroceryList
import com.rasoiai.domain.model.Achievement
import com.rasoiai.domain.model.ChatMessage
import com.rasoiai.domain.model.CookingDay
import com.rasoiai.domain.model.CookingStreak
import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.Difficulty
import com.rasoiai.domain.model.FavoriteCollection
import com.rasoiai.domain.model.FoodCategory
import com.rasoiai.domain.model.FrequencyType
import com.rasoiai.domain.model.GroceryItem
import com.rasoiai.domain.model.IngredientCategory
import com.rasoiai.domain.model.MealType
import com.rasoiai.domain.model.NutritionGoal
import com.rasoiai.domain.model.PantryCategory
import com.rasoiai.domain.model.PantryItem
import com.rasoiai.domain.model.RecipeRule
import com.rasoiai.domain.model.RecipeSuggestion
import com.rasoiai.domain.model.RuleAction
import com.rasoiai.domain.model.RuleEnforcement
import com.rasoiai.domain.model.RuleFrequency
import com.rasoiai.domain.model.RuleType
import com.rasoiai.domain.model.WeeklyChallenge
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class EntityMappersTest {

    @Nested
    @DisplayName("Recipe Entity Mappers")
    inner class RecipeEntityMappers {

        @Test
        @DisplayName("Should map RecipeEntity to Recipe domain model")
        fun `should map RecipeEntity to Recipe domain model`() {
            // Given
            val entity = RecipeEntity(
                id = "recipe-1",
                name = "Paneer Butter Masala",
                description = "Creamy tomato-based curry",
                imageUrl = "https://example.com/image.jpg",
                prepTimeMinutes = 15,
                cookTimeMinutes = 30,
                servings = 4,
                difficulty = "medium",
                cuisineType = "north",
                mealTypes = listOf("LUNCH", "DINNER"),
                dietaryTags = listOf("vegetarian"),
                ingredients = """[{"id":"ing-1","name":"Paneer","quantity":"250","unit":"g","category":"dairy","isOptional":false}]""",
                instructions = """[{"stepNumber":1,"instruction":"Cut paneer","durationMinutes":5,"timerRequired":false,"tips":"Use fresh"}]""",
                nutritionInfo = """{"calories":350,"protein":15.0,"carbohydrates":20.0,"fat":25.0,"fiber":3.0}""",
                calories = 350,
                isFavorite = true,
                cachedAt = System.currentTimeMillis()
            )

            // When
            val domain = entity.toDomain()

            // Then
            assertEquals("recipe-1", domain.id)
            assertEquals("Paneer Butter Masala", domain.name)
            assertEquals(CuisineType.NORTH, domain.cuisineType)
            assertEquals(Difficulty.MEDIUM, domain.difficulty)
            assertEquals(2, domain.mealTypes.size)
            assertTrue(domain.mealTypes.contains(MealType.LUNCH))
            assertTrue(domain.mealTypes.contains(MealType.DINNER))
            assertEquals(1, domain.ingredients.size)
            assertEquals("Paneer", domain.ingredients.first().name)
            assertEquals(1, domain.instructions.size)
            assertTrue(domain.isFavorite)
        }

        @Test
        @DisplayName("Should handle empty ingredients JSON gracefully")
        fun `should handle empty ingredients JSON gracefully`() {
            // Given
            val entity = RecipeEntity(
                id = "recipe-1",
                name = "Test Recipe",
                description = "Test",
                imageUrl = null,
                prepTimeMinutes = 10,
                cookTimeMinutes = 20,
                servings = 2,
                difficulty = "easy",
                cuisineType = "north",
                mealTypes = emptyList(),
                dietaryTags = emptyList(),
                ingredients = "[]",
                instructions = "[]",
                nutritionInfo = null,
                calories = null,
                isFavorite = false,
                cachedAt = System.currentTimeMillis()
            )

            // When
            val domain = entity.toDomain()

            // Then
            assertTrue(domain.ingredients.isEmpty())
            assertTrue(domain.instructions.isEmpty())
            assertNull(domain.nutrition)
        }
    }

    @Nested
    @DisplayName("MealPlan Entity Mappers")
    inner class MealPlanEntityMappers {

        @Test
        @DisplayName("Should map MealPlanEntity with items to MealPlan domain")
        fun `should map MealPlanEntity with items to MealPlan domain`() {
            // Given
            val entity = MealPlanEntity(
                id = "plan-1",
                weekStartDate = "2026-01-27",
                weekEndDate = "2026-02-02",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                isSynced = true
            )

            val items = listOf(
                MealPlanItemEntity(
                    mealPlanId = "plan-1",
                    date = "2026-01-27",
                    dayName = "Monday",
                    mealType = "BREAKFAST",
                    recipeId = "recipe-1",
                    recipeName = "Poha",
                    recipeImageUrl = null,
                    prepTimeMinutes = 20,
                    calories = 300,
                    dietaryTags = listOf("vegetarian"),
                    isLocked = false,
                    order = 0
                )
            )

            val festivals = emptyList<MealPlanFestivalEntity>()

            // When
            val domain = entity.toDomain(items, festivals)

            // Then
            assertEquals("plan-1", domain.id)
            assertEquals(LocalDate.of(2026, 1, 27), domain.weekStartDate)
            assertEquals(7, domain.days.size)
            assertEquals(1, domain.days.first().breakfast.size)
            assertEquals("Poha", domain.days.first().breakfast.first().recipeName)
        }

        @Test
        @DisplayName("Should map MealPlanItemEntity to MealItem domain")
        fun `should map MealPlanItemEntity to MealItem domain`() {
            // Given
            val entity = MealPlanItemEntity(
                mealPlanId = "plan-1",
                date = "2026-01-27",
                dayName = "Monday",
                mealType = "LUNCH",
                recipeId = "recipe-1",
                recipeName = "Dal Rice",
                recipeImageUrl = "https://example.com/dal.jpg",
                prepTimeMinutes = 30,
                calories = 450,
                dietaryTags = listOf("vegetarian", "vegan"),
                isLocked = true,
                order = 1
            )

            // When
            val domain = entity.toDomain()

            // Then
            assertEquals("recipe-1", domain.recipeId)
            assertEquals("Dal Rice", domain.recipeName)
            assertTrue(domain.isLocked)
            assertEquals(1, domain.order)
            assertEquals(2, domain.dietaryTags.size)
        }

        @Test
        @DisplayName("Should map MealPlanFestivalEntity to Festival domain")
        fun `should map MealPlanFestivalEntity to Festival domain`() {
            // Given
            val entity = MealPlanFestivalEntity(
                id = "fest-1",
                mealPlanId = "plan-1",
                date = "2026-01-27",
                name = "Republic Day",
                isFastingDay = false,
                suggestedDishes = listOf("Kheer", "Halwa")
            )

            val date = LocalDate.of(2026, 1, 27)

            // When
            val domain = entity.toDomain(date)

            // Then
            assertEquals("fest-1", domain.id)
            assertEquals("Republic Day", domain.name)
            assertEquals(date, domain.date)
            assertFalse(domain.isFastingDay)
            assertEquals(2, domain.suggestedDishes.size)
        }
    }

    @Nested
    @DisplayName("Grocery Entity Mappers")
    inner class GroceryEntityMappers {

        @Test
        @DisplayName("Should map GroceryItemEntity to GroceryItem domain")
        fun `should map GroceryItemEntity to GroceryItem domain`() {
            // Given
            val entity = GroceryItemEntity(
                id = "grocery-1",
                name = "Onion",
                quantity = "500",
                unit = "g",
                category = "vegetables",
                isChecked = true,
                mealPlanId = "plan-1",
                recipeIds = listOf("recipe-1", "recipe-2"),
                notes = "Get red onions",
                createdAt = System.currentTimeMillis()
            )

            // When
            val domain = entity.toDomain()

            // Then
            assertEquals("grocery-1", domain.id)
            assertEquals("Onion", domain.name)
            assertEquals("500", domain.quantity)
            assertEquals(IngredientCategory.VEGETABLES, domain.category)
            assertTrue(domain.isPurchased)
            assertEquals(2, domain.recipeIds.size)
            assertFalse(domain.isCustom)
        }

        @Test
        @DisplayName("Should map GroceryItem domain to GroceryItemEntity")
        fun `should map GroceryItem domain to GroceryItemEntity`() {
            // Given
            val domain = GroceryItem(
                id = "grocery-1",
                name = "Tomato",
                quantity = "1",
                unit = "kg",
                category = IngredientCategory.VEGETABLES,
                isPurchased = false,
                recipeIds = listOf("recipe-1"),
                isCustom = false
            )

            // When
            val entity = domain.toEntity("plan-1")

            // Then
            assertEquals("grocery-1", entity.id)
            assertEquals("Tomato", entity.name)
            assertEquals("vegetables", entity.category)
            assertEquals("plan-1", entity.mealPlanId)
            assertFalse(entity.isChecked)
        }

        @Test
        @DisplayName("Should map list of GroceryItemEntity to GroceryList")
        fun `should map list of GroceryItemEntity to GroceryList`() {
            // Given
            val entities = listOf(
                GroceryItemEntity(
                    id = "grocery-1",
                    name = "Onion",
                    quantity = "500",
                    unit = "g",
                    category = "vegetables",
                    isChecked = false,
                    mealPlanId = "plan-1",
                    recipeIds = emptyList(),
                    notes = null,
                    createdAt = System.currentTimeMillis()
                ),
                GroceryItemEntity(
                    id = "grocery-2",
                    name = "Milk",
                    quantity = "1",
                    unit = "L",
                    category = "dairy",
                    isChecked = false,
                    mealPlanId = "plan-1",
                    recipeIds = emptyList(),
                    notes = null,
                    createdAt = System.currentTimeMillis()
                )
            )

            val startDate = LocalDate.of(2026, 1, 27)
            val endDate = LocalDate.of(2026, 2, 2)

            // When
            val groceryList = entities.toGroceryList("plan-1", startDate, endDate)

            // Then
            assertEquals("grocery-plan-1", groceryList.id)
            assertEquals("plan-1", groceryList.mealPlanId)
            assertEquals(startDate, groceryList.weekStartDate)
            assertEquals(2, groceryList.items.size)
        }
    }

    @Nested
    @DisplayName("Pantry Entity Mappers")
    inner class PantryEntityMappers {

        @Test
        @DisplayName("Should map PantryItemEntity to PantryItem domain")
        fun `should map PantryItemEntity to PantryItem domain`() {
            // Given
            val entity = PantryItemEntity(
                id = "pantry-1",
                name = "Rice",
                category = "GRAINS",
                quantity = 5,
                unit = "kg",
                addedDate = "2026-01-20",
                expiryDate = "2026-06-20",
                imageUrl = null
            )

            // When
            val domain = entity.toDomain()

            // Then
            assertEquals("pantry-1", domain.id)
            assertEquals("Rice", domain.name)
            assertEquals(PantryCategory.GRAINS, domain.category)
            assertEquals(5, domain.quantity)
            assertEquals(LocalDate.of(2026, 1, 20), domain.addedDate)
            assertEquals(LocalDate.of(2026, 6, 20), domain.expiryDate)
        }

        @Test
        @DisplayName("Should map PantryItem domain to PantryItemEntity")
        fun `should map PantryItem domain to PantryItemEntity`() {
            // Given
            val domain = PantryItem(
                id = "pantry-1",
                name = "Milk",
                category = PantryCategory.DAIRY_MILK,
                quantity = 2,
                unit = "L",
                addedDate = LocalDate.of(2026, 1, 27),
                expiryDate = LocalDate.of(2026, 2, 3),
                imageUrl = null
            )

            // When
            val entity = domain.toEntity()

            // Then
            assertEquals("pantry-1", entity.id)
            assertEquals("Milk", entity.name)
            assertEquals("DAIRY_MILK", entity.category)
            assertEquals("2026-01-27", entity.addedDate)
            assertEquals("2026-02-03", entity.expiryDate)
        }
    }

    @Nested
    @DisplayName("Stats Entity Mappers")
    inner class StatsEntityMappers {

        @Test
        @DisplayName("Should map CookingStreakEntity to CookingStreak domain")
        fun `should map CookingStreakEntity to CookingStreak domain`() {
            // Given
            val entity = CookingStreakEntity(
                id = "user_streak",
                currentStreak = 5,
                bestStreak = 10,
                lastCookingDate = "2026-01-27"
            )

            // When
            val domain = entity.toDomain()

            // Then
            assertEquals(5, domain.currentStreak)
            assertEquals(10, domain.bestStreak)
            assertEquals(LocalDate.of(2026, 1, 27), domain.lastCookingDate)
        }

        @Test
        @DisplayName("Should map CookingDayEntity to CookingDay domain")
        fun `should map CookingDayEntity to CookingDay domain`() {
            // Given
            val entity = CookingDayEntity(
                date = "2026-01-27",
                didCook = true,
                mealsCount = 3
            )

            // When
            val domain = entity.toDomain()

            // Then
            assertEquals(LocalDate.of(2026, 1, 27), domain.date)
            assertTrue(domain.didCook)
            assertEquals(3, domain.mealsCount)
        }

        @Test
        @DisplayName("Should map AchievementEntity to Achievement domain")
        fun `should map AchievementEntity to Achievement domain`() {
            // Given
            val entity = AchievementEntity(
                id = "first_meal",
                name = "First Meal",
                description = "Cook your first meal",
                emoji = "👨‍🍳",
                isUnlocked = true,
                unlockedDate = "2026-01-15"
            )

            // When
            val domain = entity.toDomain()

            // Then
            assertEquals("first_meal", domain.id)
            assertEquals("First Meal", domain.name)
            assertTrue(domain.isUnlocked)
            assertEquals(LocalDate.of(2026, 1, 15), domain.unlockedDate)
        }

        @Test
        @DisplayName("Should map WeeklyChallengeEntity to WeeklyChallenge domain")
        fun `should map WeeklyChallengeEntity to WeeklyChallenge domain`() {
            // Given
            val entity = WeeklyChallengeEntity(
                id = "weekly-1",
                name = "Home Chef Week",
                description = "Cook 5 meals",
                targetCount = 5,
                currentProgress = 2,
                rewardBadge = "🏆",
                isJoined = true,
                weekStartDate = "2026-01-27",
                weekEndDate = "2026-02-02"
            )

            // When
            val domain = entity.toDomain()

            // Then
            assertEquals("weekly-1", domain.id)
            assertEquals("Home Chef Week", domain.name)
            assertEquals(5, domain.targetCount)
            assertEquals(2, domain.currentProgress)
            assertTrue(domain.isJoined)
        }
    }

    @Nested
    @DisplayName("Recipe Rules Entity Mappers")
    inner class RecipeRulesEntityMappers {

        @Test
        @DisplayName("Should map RecipeRuleEntity to RecipeRule domain")
        fun `should map RecipeRuleEntity to RecipeRule domain`() {
            // Given
            val entity = RecipeRuleEntity(
                id = "rule-1",
                type = "ingredient",
                action = "exclude",
                targetId = "onion",
                targetName = "Onion",
                frequencyType = "never",
                frequencyCount = null,
                frequencyDays = null,
                enforcement = "required",
                mealSlot = null,
                isActive = true,
                createdAt = "2026-01-27T10:00:00",
                updatedAt = "2026-01-27T10:00:00"
            )

            // When
            val domain = entity.toDomain()

            // Then
            assertEquals("rule-1", domain.id)
            assertEquals(RuleType.INGREDIENT, domain.type)
            assertEquals(RuleAction.EXCLUDE, domain.action)
            assertEquals("Onion", domain.targetName)
            assertEquals(FrequencyType.NEVER, domain.frequency.type)
            assertEquals(RuleEnforcement.REQUIRED, domain.enforcement)
            assertTrue(domain.isActive)
        }

        @Test
        @DisplayName("Should map NutritionGoalEntity to NutritionGoal domain")
        fun `should map NutritionGoalEntity to NutritionGoal domain`() {
            // Given
            val entity = NutritionGoalEntity(
                id = "goal-1",
                foodCategory = "green_leafy",
                weeklyTarget = 3,
                currentProgress = 1,
                enforcement = "preferred",
                isActive = true,
                createdAt = "2026-01-27T10:00:00",
                updatedAt = "2026-01-27T10:00:00"
            )

            // When
            val domain = entity.toDomain()

            // Then
            assertEquals("goal-1", domain.id)
            assertEquals(FoodCategory.GREEN_LEAFY, domain.foodCategory)
            assertEquals(3, domain.weeklyTarget)
            assertEquals(1, domain.currentProgress)
            assertEquals(RuleEnforcement.PREFERRED, domain.enforcement)
            assertTrue(domain.isActive)
        }
    }

    @Nested
    @DisplayName("Chat Message Entity Mappers")
    inner class ChatMessageEntityMappers {

        @Test
        @DisplayName("Should map ChatMessageEntity to ChatMessage domain")
        fun `should map ChatMessageEntity to ChatMessage domain`() {
            // Given
            val entity = ChatMessageEntity(
                id = "msg-1",
                content = "Hello, I want paneer recipes",
                isFromUser = true,
                timestamp = 1706345600000L,
                quickActionsJson = null,
                recipeSuggestionsJson = null
            )

            // When
            val domain = entity.toDomain()

            // Then
            assertEquals("msg-1", domain.id)
            assertEquals("Hello, I want paneer recipes", domain.content)
            assertTrue(domain.isFromUser)
            assertNull(domain.quickActions)
            assertNull(domain.recipeSuggestions)
        }

        @Test
        @DisplayName("Should map ChatMessage with suggestions to entity")
        fun `should map ChatMessage with suggestions to entity`() {
            // Given
            val domain = ChatMessage(
                id = "msg-2",
                content = "Here are some paneer recipes",
                isFromUser = false,
                timestamp = 1706345700000L,
                quickActions = listOf("More recipes", "Different cuisine"),
                recipeSuggestions = listOf(
                    RecipeSuggestion("recipe-1", "Paneer Butter Masala", 40, null)
                )
            )

            // When
            val entity = domain.toEntity()

            // Then
            assertEquals("msg-2", entity.id)
            assertFalse(entity.isFromUser)
            assertNotNull(entity.quickActionsJson)
            assertNotNull(entity.recipeSuggestionsJson)
        }
    }

    @Nested
    @DisplayName("Favorite Collection Entity Mappers")
    inner class FavoriteCollectionEntityMappers {

        @Test
        @DisplayName("Should map FavoriteCollectionEntity to FavoriteCollection domain")
        fun `should map FavoriteCollectionEntity to FavoriteCollection domain`() {
            // Given
            val entity = FavoriteCollectionEntity(
                id = "coll-1",
                name = "Weeknight Dinners",
                coverImageUrl = "https://example.com/cover.jpg",
                order = 0,
                isDefault = false,
                createdAt = System.currentTimeMillis()
            )

            val recipeIds = listOf("recipe-1", "recipe-2", "recipe-3")

            // When
            val domain = entity.toDomain(recipeIds)

            // Then
            assertEquals("coll-1", domain.id)
            assertEquals("Weeknight Dinners", domain.name)
            assertEquals(3, domain.recipeIds.size)
            assertFalse(domain.isDefault)
        }
    }
}
