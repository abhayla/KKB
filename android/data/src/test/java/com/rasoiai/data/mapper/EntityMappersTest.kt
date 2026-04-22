package com.rasoiai.data.mapper

import com.rasoiai.data.local.entity.AchievementEntity
import com.rasoiai.data.local.entity.HouseholdEntity
import com.rasoiai.data.local.entity.HouseholdMemberEntity
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
import com.rasoiai.domain.model.Household
import com.rasoiai.domain.model.HouseholdMember
import com.rasoiai.domain.model.HouseholdRole
import com.rasoiai.domain.model.MemberStatus
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
import com.rasoiai.data.local.entity.NotificationEntity
import com.rasoiai.data.local.mapper.portionSizeFloatToString
import com.rasoiai.data.remote.dto.HouseholdMemberResponse
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
                mealTypes = listOf("lunch", "dinner"),
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

        @Test
        @DisplayName("Should round-trip rating fields through RecipeEntity (issue #21 offline cache)")
        fun `should round-trip rating fields through RecipeEntity`() {
            // Given — an entity with all three rating aggregate fields populated
            val entity = RecipeEntity(
                id = "recipe-r1",
                name = "Test",
                description = "desc",
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
                cachedAt = System.currentTimeMillis(),
                averageRating = 4.5,
                ratingCount = 10,
                userRating = 5.0
            )

            // When — map entity to domain
            val domain = entity.toDomain()

            // Then — rating fields survive the mapping
            assertEquals(4.5, domain.averageRating)
            assertEquals(10, domain.ratingCount)
            assertEquals(5.0, domain.userRating)
        }

        @Test
        @DisplayName("Should default rating fields to null/0 when absent from RecipeEntity")
        fun `should default rating fields to null and zero when absent`() {
            // Given — an entity constructed without specifying rating fields
            val entity = RecipeEntity(
                id = "recipe-r2",
                name = "Test",
                description = "desc",
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

            // Then — matches the defaults defined on Recipe + RecipeEntity
            assertNull(domain.averageRating)
            assertEquals(0, domain.ratingCount)
            assertNull(domain.userRating)
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
                    id = "item-1",
                    mealPlanId = "plan-1",
                    date = "2026-01-27",
                    dayName = "Monday",
                    mealType = "breakfast",
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
                id = "item-2",
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
                mealSlots = null,
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
            assertFalse(domain.forceOverride)
        }

        @Test
        @DisplayName("Should roundtrip forceOverride through entity mapping")
        fun `should roundtrip forceOverride through entity mapping`() {
            // Given - entity with forceOverride = true
            val entity = RecipeRuleEntity(
                id = "rule-override",
                type = "ingredient",
                action = "include",
                targetId = "gulab-jamun",
                targetName = "Gulab Jamun",
                frequencyType = "daily",
                frequencyCount = null,
                frequencyDays = null,
                enforcement = "required",
                mealSlots = null,
                isActive = true,
                forceOverride = true,
                createdAt = "2026-01-27T10:00:00",
                updatedAt = "2026-01-27T10:00:00"
            )

            // When - entity -> domain -> entity
            val domain = entity.toDomain()
            val roundTripped = domain.toEntity()

            // Then
            assertTrue(domain.forceOverride)
            assertEquals(RuleAction.INCLUDE, domain.action)
            assertTrue(roundTripped.forceOverride)
            assertEquals("rule-override", roundTripped.id)
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

    @Nested
    @DisplayName("Household Entity Mappers")
    inner class HouseholdEntityMappers {

        @Test
        @DisplayName("Should map HouseholdEntity to Household domain model")
        fun `should map HouseholdEntity to Household domain model`() {
            // Given
            val entity = HouseholdEntity(
                id = "hh-1",
                name = "Sharma Family",
                inviteCode = "ABC123",
                ownerId = "user-1",
                slotConfigJson = """{"breakfast":2,"lunch":2,"dinner":2,"snacks":1}""",
                maxMembers = 8,
                memberCount = 3,
                isActive = true,
                createdAt = "2026-03-01T10:00:00",
                updatedAt = "2026-03-01T12:00:00"
            )

            // When
            val domain = entity.toDomain()

            // Then
            assertEquals("hh-1", domain.id)
            assertEquals("Sharma Family", domain.name)
            assertEquals("ABC123", domain.inviteCode)
            assertEquals("user-1", domain.ownerId)
            assertNotNull(domain.slotConfig)
            assertEquals(2, domain.slotConfig?.get("breakfast"))
            assertEquals(1, domain.slotConfig?.get("snacks"))
            assertEquals(8, domain.maxMembers)
            assertEquals(3, domain.memberCount)
            assertTrue(domain.isActive)
            assertEquals(LocalDateTime.of(2026, 3, 1, 10, 0, 0), domain.createdAt)
        }

        @Test
        @DisplayName("Should map HouseholdEntity with null slotConfig")
        fun `should map HouseholdEntity with null slotConfig`() {
            // Given
            val entity = HouseholdEntity(
                id = "hh-2",
                name = "Test Household",
                inviteCode = "XYZ789",
                ownerId = "user-2",
                slotConfigJson = null,
                maxMembers = 4,
                memberCount = 1,
                isActive = true,
                createdAt = "2026-03-01T10:00:00",
                updatedAt = "2026-03-01T10:00:00"
            )

            // When
            val domain = entity.toDomain()

            // Then
            assertNull(domain.slotConfig)
        }

        @Test
        @DisplayName("Should map HouseholdMemberEntity to HouseholdMember domain model")
        fun `should map HouseholdMemberEntity to HouseholdMember domain model`() {
            // Given
            val entity = HouseholdMemberEntity(
                id = "mem-1",
                householdId = "hh-1",
                userId = "user-1",
                familyMemberId = null,
                name = "Ramesh Sharma",
                role = "owner",
                canEditSharedPlan = true,
                isTemporary = false,
                joinDate = "2026-03-01T10:00:00",
                leaveDate = null,
                portionSize = 1.0f,
                status = "active"
            )

            // When
            val domain = entity.toDomain()

            // Then
            assertEquals("mem-1", domain.id)
            assertEquals("user-1", domain.userId)
            assertNull(domain.familyMemberId)
            assertEquals("Ramesh Sharma", domain.name)
            assertEquals(HouseholdRole.OWNER, domain.role)
            assertTrue(domain.canEditSharedPlan)
            assertFalse(domain.isTemporary)
            assertNull(domain.leaveDate)
            assertEquals(1.0f, domain.portionSize)
            assertEquals(MemberStatus.ACTIVE, domain.status)
        }

        @Test
        @DisplayName("Should map HouseholdMemberEntity with guest fields")
        fun `should map HouseholdMemberEntity with guest fields`() {
            // Given
            val entity = HouseholdMemberEntity(
                id = "mem-2",
                householdId = "hh-1",
                userId = null,
                familyMemberId = "fam-1",
                name = "Guest User",
                role = "guest",
                canEditSharedPlan = false,
                isTemporary = true,
                joinDate = "2026-03-01T10:00:00",
                leaveDate = "2026-03-08T10:00:00",
                portionSize = 0.5f,
                status = "inactive"
            )

            // When
            val domain = entity.toDomain()

            // Then
            assertNull(domain.userId)
            assertEquals("fam-1", domain.familyMemberId)
            assertEquals(HouseholdRole.GUEST, domain.role)
            assertTrue(domain.isTemporary)
            assertNotNull(domain.leaveDate)
            assertEquals(LocalDateTime.of(2026, 3, 8, 10, 0, 0), domain.leaveDate)
            assertEquals(0.5f, domain.portionSize)
            assertEquals(MemberStatus.INACTIVE, domain.status)
        }
    }

    @Nested
    @DisplayName("Recipe Entity JSON Round-Trip")
    inner class RecipeEntityJsonRoundTrip {

        @Test
        @DisplayName("Should round-trip ingredients JSON through entity-domain mapping")
        fun `should roundTrip ingredients JSON through entity-domain mapping`() {
            // Given
            val ingredientsJson = """[{"id":"ing-1","name":"Paneer","quantity":"250","unit":"g","category":"dairy","is_optional":false},{"id":"ing-2","name":"Tomato","quantity":"200","unit":"g","category":"vegetables","is_optional":false}]"""
            val entity = createRecipeEntity(ingredients = ingredientsJson)

            // When
            val domain = entity.toDomain()

            // Then
            assertEquals(2, domain.ingredients.size)
            assertEquals("Paneer", domain.ingredients[0].name)
            assertEquals("250", domain.ingredients[0].quantity)
            assertEquals("g", domain.ingredients[0].unit)
            assertEquals("Tomato", domain.ingredients[1].name)
        }

        @Test
        @DisplayName("Should round-trip instructions JSON through entity-domain mapping")
        fun `should roundTrip instructions JSON through entity-domain mapping`() {
            // Given
            val instructionsJson = """[{"step_number":1,"instruction":"Cut paneer into cubes","duration_minutes":5,"timer_required":false,"tips":"Use fresh paneer"},{"step_number":2,"instruction":"Cook in gravy","duration_minutes":15,"timer_required":true,"tips":null}]"""
            val entity = createRecipeEntity(instructions = instructionsJson)

            // When
            val domain = entity.toDomain()

            // Then
            assertEquals(2, domain.instructions.size)
            assertEquals(1, domain.instructions[0].stepNumber)
            assertEquals("Cut paneer into cubes", domain.instructions[0].instruction)
            assertEquals(5, domain.instructions[0].durationMinutes)
            assertEquals(2, domain.instructions[1].stepNumber)
            assertTrue(domain.instructions[1].timerRequired)
        }

        @Test
        @DisplayName("Should handle empty JSON arrays gracefully")
        fun `should handle empty JSON arrays gracefully`() {
            // Given
            val entity = createRecipeEntity(ingredients = "[]", instructions = "[]")

            // When
            val domain = entity.toDomain()

            // Then
            assertTrue(domain.ingredients.isEmpty())
            assertTrue(domain.instructions.isEmpty())
        }

        @Test
        @DisplayName("Should handle malformed JSON by returning empty list")
        fun `should handle malformed JSON by returning empty list`() {
            // Given
            val entity = createRecipeEntity(
                ingredients = "this is not valid json{{{",
                instructions = "also broken["
            )

            // When
            val domain = entity.toDomain()

            // Then
            assertTrue(domain.ingredients.isEmpty())
            assertTrue(domain.instructions.isEmpty())
        }

        private fun createRecipeEntity(
            ingredients: String = "[]",
            instructions: String = "[]",
            nutritionInfo: String? = null
        ) = RecipeEntity(
            id = "recipe-rt",
            name = "Round Trip Recipe",
            description = "Test",
            imageUrl = null,
            prepTimeMinutes = 10,
            cookTimeMinutes = 20,
            servings = 2,
            difficulty = "easy",
            cuisineType = "north",
            mealTypes = emptyList(),
            dietaryTags = emptyList(),
            ingredients = ingredients,
            instructions = instructions,
            nutritionInfo = nutritionInfo,
            calories = null,
            isFavorite = false,
            cachedAt = System.currentTimeMillis()
        )
    }

    @Nested
    @DisplayName("ChatMessage Entity JSON Round-Trip")
    inner class ChatMessageEntityJsonRoundTrip {

        @Test
        @DisplayName("Should round-trip quickActions through entity-domain-entity mapping")
        fun `should roundTrip quickActions through entity-domain-entity mapping`() {
            // Given
            val domain = ChatMessage(
                id = "msg-rt-1",
                content = "Here are some options",
                isFromUser = false,
                timestamp = 1706345600000L,
                quickActions = listOf("Show more", "Different cuisine", "Vegetarian only"),
                recipeSuggestions = null
            )

            // When: domain -> entity -> domain
            val entity = domain.toEntity()
            val roundTripped = entity.toDomain()

            // Then
            assertNotNull(roundTripped.quickActions)
            assertEquals(3, roundTripped.quickActions!!.size)
            assertEquals("Show more", roundTripped.quickActions!![0])
            assertEquals("Different cuisine", roundTripped.quickActions!![1])
            assertEquals("Vegetarian only", roundTripped.quickActions!![2])
        }

        @Test
        @DisplayName("Should round-trip recipeSuggestions through entity-domain-entity mapping")
        fun `should roundTrip recipeSuggestions through entity-domain-entity mapping`() {
            // Given
            val domain = ChatMessage(
                id = "msg-rt-2",
                content = "Try these recipes",
                isFromUser = false,
                timestamp = 1706345600000L,
                quickActions = null,
                recipeSuggestions = listOf(
                    RecipeSuggestion("r-1", "Paneer Tikka", 25, "https://img.com/1.jpg"),
                    RecipeSuggestion("r-2", "Dal Makhani", 45, null)
                )
            )

            // When: domain -> entity -> domain
            val entity = domain.toEntity()
            val roundTripped = entity.toDomain()

            // Then
            assertNotNull(roundTripped.recipeSuggestions)
            assertEquals(2, roundTripped.recipeSuggestions!!.size)
            assertEquals("r-1", roundTripped.recipeSuggestions!![0].recipeId)
            assertEquals("Paneer Tikka", roundTripped.recipeSuggestions!![0].recipeName)
            assertEquals(25, roundTripped.recipeSuggestions!![0].cookTimeMinutes)
            assertEquals("https://img.com/1.jpg", roundTripped.recipeSuggestions!![0].imageUrl)
            assertEquals("r-2", roundTripped.recipeSuggestions!![1].recipeId)
            assertNull(roundTripped.recipeSuggestions!![1].imageUrl)
        }

        @Test
        @DisplayName("Should handle null JSON fields gracefully")
        fun `should handle null JSON fields gracefully`() {
            // Given
            val entity = ChatMessageEntity(
                id = "msg-rt-3",
                content = "Plain message",
                isFromUser = true,
                timestamp = 1706345600000L,
                quickActionsJson = null,
                recipeSuggestionsJson = null
            )

            // When
            val domain = entity.toDomain()

            // Then
            assertNull(domain.quickActions)
            assertNull(domain.recipeSuggestions)
        }
    }

    @Nested
    @DisplayName("Notification Entity JSON Round-Trip")
    inner class NotificationEntityJsonRoundTrip {

        @Test
        @DisplayName("Should round-trip actionData JSON through entity-domain-entity mapping")
        fun `should roundTrip actionData JSON through entity-domain-entity mapping`() {
            // Given
            val entity = NotificationEntity(
                id = "notif-rt-1",
                type = "meal_plan_update",
                title = "Meal Plan Ready",
                body = "Your weekly meal plan has been generated",
                actionType = "open_meal_plan",
                actionData = """{"mealPlanId":"plan-123","recipeId":null,"festivalId":null,"streakCount":null}""",
                isRead = false,
                createdAt = 1706345600000L
            )

            // When: entity -> domain -> entity
            val domain = entity.toDomain()
            val roundTripped = domain.toEntity()

            // Then
            assertNotNull(domain.actionData)
            assertEquals("plan-123", domain.actionData!!.mealPlanId)
            assertNull(domain.actionData!!.recipeId)
            assertNotNull(roundTripped.actionData)
        }

        @Test
        @DisplayName("Should handle null actionData gracefully")
        fun `should handle null actionData gracefully`() {
            // Given
            val entity = NotificationEntity(
                id = "notif-rt-2",
                type = "shopping_reminder",
                title = "Time to Shop",
                body = "Don't forget your groceries",
                actionType = "open_grocery",
                actionData = null,
                isRead = false,
                createdAt = 1706345600000L
            )

            // When
            val domain = entity.toDomain()
            val roundTripped = domain.toEntity()

            // Then
            assertNull(domain.actionData)
            assertNull(roundTripped.actionData)
        }
    }

    @Nested
    @DisplayName("Portion Size Mapping")
    inner class PortionSizeMapping {

        @Test
        @DisplayName("Should map SMALL portion size string to 0.5f via DTO")
        fun `should map SMALL portion size string to 05f via DTO`() {
            // Given
            val dto = createHouseholdMemberDto(portionSize = "SMALL")

            // When: DTO -> Entity (uses portionSizeStringToFloat internally)
            val entity = dto.toEntity()

            // Then
            assertEquals(0.5f, entity.portionSize)
        }

        @Test
        @DisplayName("Should map REGULAR portion size string to 1.0f via DTO")
        fun `should map REGULAR portion size string to 10f via DTO`() {
            // Given
            val dto = createHouseholdMemberDto(portionSize = "REGULAR")

            // When
            val entity = dto.toEntity()

            // Then
            assertEquals(1.0f, entity.portionSize)
        }

        @Test
        @DisplayName("Should map LARGE portion size string to 1.5f via DTO")
        fun `should map LARGE portion size string to 15f via DTO`() {
            // Given
            val dto = createHouseholdMemberDto(portionSize = "LARGE")

            // When
            val entity = dto.toEntity()

            // Then
            assertEquals(1.5f, entity.portionSize)
        }

        @Test
        @DisplayName("Should default unknown portion size string to 1.0f via DTO")
        fun `should default unknown portion size string to 10f via DTO`() {
            // Given
            val dto = createHouseholdMemberDto(portionSize = "UNKNOWN")

            // When
            val entity = dto.toEntity()

            // Then
            assertEquals(1.0f, entity.portionSize)
        }

        @Test
        @DisplayName("Should map small float to SMALL string")
        fun `should map small float to SMALL string`() {
            assertEquals("SMALL", portionSizeFloatToString(0.3f))
            assertEquals("SMALL", portionSizeFloatToString(0.5f))
            assertEquals("SMALL", portionSizeFloatToString(0.75f))
        }

        @Test
        @DisplayName("Should map regular float to REGULAR string")
        fun `should map regular float to REGULAR string`() {
            assertEquals("REGULAR", portionSizeFloatToString(0.76f))
            assertEquals("REGULAR", portionSizeFloatToString(1.0f))
            assertEquals("REGULAR", portionSizeFloatToString(1.25f))
        }

        @Test
        @DisplayName("Should map large float to LARGE string")
        fun `should map large float to LARGE string`() {
            assertEquals("LARGE", portionSizeFloatToString(1.26f))
            assertEquals("LARGE", portionSizeFloatToString(1.5f))
            assertEquals("LARGE", portionSizeFloatToString(2.0f))
        }

        private fun createHouseholdMemberDto(portionSize: String = "REGULAR") =
            HouseholdMemberResponse(
                id = "mem-ps",
                householdId = "hh-1",
                userId = "user-1",
                name = "Test User",
                role = "member",
                canEditSharedPlan = false,
                isTemporary = false,
                joinDate = "2026-03-01T10:00:00",
                portionSize = portionSize,
                status = "active"
            )
    }

    @Nested
    @DisplayName("Malformed JSON Safety")
    inner class MalformedJsonSafety {

        // ==================== Recipe Entity malformed JSON ====================

        @Test
        @DisplayName("Recipe with completely invalid ingredients JSON returns empty list")
        fun test_recipe_ingredients_invalidJson_returnsEmptyList() {
            val entity = createRecipeEntity(ingredients = "not json at all")
            val domain = entity.toDomain()
            assertTrue(domain.ingredients.isEmpty())
        }

        @Test
        @DisplayName("Recipe with partial/unclosed ingredients JSON returns empty list")
        fun test_recipe_ingredients_partialJson_returnsEmptyList() {
            val entity = createRecipeEntity(ingredients = """[{"name":"Paneer"""")
            val domain = entity.toDomain()
            assertTrue(domain.ingredients.isEmpty())
        }

        @Test
        @DisplayName("Recipe with wrong type instructions JSON returns empty list")
        fun test_recipe_instructions_wrongType_returnsEmptyList() {
            val entity = createRecipeEntity(instructions = "42")
            val domain = entity.toDomain()
            assertTrue(domain.instructions.isEmpty())
        }

        @Test
        @DisplayName("Recipe with null-value instructions JSON returns empty list")
        fun test_recipe_instructions_nullValue_returnsEmptyList() {
            val entity = createRecipeEntity(instructions = "null")
            val domain = entity.toDomain()
            assertTrue(domain.instructions.isEmpty())
        }

        @Test
        @DisplayName("Recipe with malformed nutritionInfo JSON returns null")
        fun test_recipe_nutritionInfo_malformed_returnsNull() {
            val entity = createRecipeEntity(nutritionInfo = "{broken")
            val domain = entity.toDomain()
            assertNull(domain.nutrition)
        }

        // ==================== Chat Message malformed JSON ====================

        @Test
        @DisplayName("ChatMessage with malformed quickActionsJson handled gracefully")
        fun test_chatMessage_quickActionsJson_malformed_handledGracefully() {
            val entity = ChatMessageEntity(
                id = "msg-malformed-1",
                content = "Test message",
                isFromUser = false,
                timestamp = 1706345600000L,
                quickActionsJson = "{{bad}}",
                recipeSuggestionsJson = null
            )
            val domain = entity.toDomain()
            assertNull(domain.quickActions)
        }

        @Test
        @DisplayName("ChatMessage with wrong type recipeSuggestionsJson handled gracefully")
        fun test_chatMessage_recipeSuggestionsJson_wrongType() {
            val entity = ChatMessageEntity(
                id = "msg-malformed-2",
                content = "Test message",
                isFromUser = false,
                timestamp = 1706345600000L,
                quickActionsJson = null,
                recipeSuggestionsJson = "true"
            )
            val domain = entity.toDomain()
            assertNull(domain.recipeSuggestions)
        }

        // ==================== Notification Entity malformed JSON ====================

        @Test
        @DisplayName("Notification with malformed actionData returns null")
        fun test_notification_actionData_malformed_returnsNull() {
            val entity = NotificationEntity(
                id = "notif-malformed-1",
                type = "meal_plan_update",
                title = "Test",
                body = "Test body",
                actionType = "open_meal_plan",
                actionData = "not{json",
                isRead = false,
                createdAt = 1706345600000L
            )
            val domain = entity.toDomain()
            assertNull(domain.actionData)
        }

        @Test
        @DisplayName("Notification with empty string actionData returns null")
        fun test_notification_actionData_emptyString_returnsNull() {
            val entity = NotificationEntity(
                id = "notif-malformed-2",
                type = "meal_plan_update",
                title = "Test",
                body = "Test body",
                actionType = "open_meal_plan",
                actionData = "",
                isRead = false,
                createdAt = 1706345600000L
            )
            val domain = entity.toDomain()
            assertNull(domain.actionData)
        }

        // ==================== Household portion size edge cases ====================

        @Test
        @DisplayName("Empty string portion size defaults to REGULAR (1.0f) via DTO")
        fun test_portionSize_emptyString_defaultsToRegular() {
            val dto = createHouseholdMemberDto(portionSize = "")
            val entity = dto.toEntity()
            assertEquals(1.0f, entity.portionSize)
        }

        @Test
        @DisplayName("Numeric string portion size defaults to REGULAR (1.0f) via DTO")
        fun test_portionSize_numericString_defaultsToRegular() {
            val dto = createHouseholdMemberDto(portionSize = "1.5")
            val entity = dto.toEntity()
            assertEquals(1.0f, entity.portionSize)
        }

        @Test
        @DisplayName("Default portion size maps to REGULAR (1.0f) via DTO")
        fun test_portionSize_null_defaultsToRegular() {
            // HouseholdMemberResponse.portionSize defaults to "REGULAR" when not provided
            val dto = HouseholdMemberResponse(
                id = "mem-default",
                householdId = "hh-1",
                userId = "user-1",
                name = "Test User",
                role = "member",
                joinDate = "2026-03-01T10:00:00"
            )
            val entity = dto.toEntity()
            assertEquals(1.0f, entity.portionSize)
        }

        private fun createRecipeEntity(
            ingredients: String = "[]",
            instructions: String = "[]",
            nutritionInfo: String? = null
        ) = RecipeEntity(
            id = "recipe-malformed",
            name = "Malformed JSON Recipe",
            description = "Test",
            imageUrl = null,
            prepTimeMinutes = 10,
            cookTimeMinutes = 20,
            servings = 2,
            difficulty = "easy",
            cuisineType = "north",
            mealTypes = emptyList(),
            dietaryTags = emptyList(),
            ingredients = ingredients,
            instructions = instructions,
            nutritionInfo = nutritionInfo,
            calories = null,
            isFavorite = false,
            cachedAt = System.currentTimeMillis()
        )

        private fun createHouseholdMemberDto(portionSize: String = "REGULAR") =
            HouseholdMemberResponse(
                id = "mem-malformed",
                householdId = "hh-1",
                userId = "user-1",
                name = "Test User",
                role = "member",
                canEditSharedPlan = false,
                isTemporary = false,
                joinDate = "2026-03-01T10:00:00",
                portionSize = portionSize,
                status = "active"
            )
    }
}
