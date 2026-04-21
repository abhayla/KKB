package com.rasoiai.data.mapper

import com.rasoiai.data.local.mapper.toDomain
import com.rasoiai.data.local.mapper.toEntity
import com.rasoiai.data.remote.dto.FestivalDto
import com.rasoiai.data.remote.dto.HouseholdResponse
import com.rasoiai.data.remote.dto.HouseholdMemberResponse
import com.rasoiai.data.remote.dto.HouseholdDetailResponse
import com.rasoiai.data.remote.dto.HouseholdNotificationResponse
import com.rasoiai.data.remote.dto.HouseholdStatsResponse
import com.rasoiai.data.remote.dto.IngredientDto
import com.rasoiai.data.remote.dto.InstructionDto
import com.rasoiai.data.remote.dto.MealItemDto
import com.rasoiai.data.remote.dto.MealPlanDayDto
import com.rasoiai.data.remote.dto.MealPlanResponse
import com.rasoiai.data.remote.dto.MealsByTypeDto
import com.rasoiai.data.remote.dto.NutritionDto
import com.rasoiai.data.remote.dto.RecipeResponse
import com.rasoiai.data.remote.dto.UserPreferencesDto
import com.rasoiai.data.remote.dto.UserResponse
import com.rasoiai.data.remote.mapper.toDomain
import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.HouseholdNotificationType
import com.rasoiai.domain.model.HouseholdRole
import com.rasoiai.domain.model.MemberStatus
import com.rasoiai.domain.model.DietaryTag
import com.rasoiai.domain.model.Difficulty
import com.rasoiai.domain.model.IngredientCategory
import com.rasoiai.domain.model.MealType
import com.rasoiai.domain.model.PrimaryDiet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DtoMappersTest {

    @Nested
    @DisplayName("Recipe DTO Mappers")
    inner class RecipeDtoMappers {

        @Test
        @DisplayName("Should map RecipeResponse to Recipe domain model")
        fun `should map RecipeResponse to Recipe domain model`() {
            // Given
            val dto = RecipeResponse(
                id = "recipe-1",
                name = "Paneer Butter Masala",
                description = "Creamy tomato-based curry with paneer",
                imageUrl = "https://example.com/image.jpg",
                prepTimeMinutes = 15,
                cookTimeMinutes = 30,
                servings = 4,
                difficulty = "medium",
                cuisineType = "north",
                mealTypes = listOf("lunch", "dinner"),
                dietaryTags = listOf("vegetarian"),
                ingredients = listOf(
                    IngredientDto(
                        id = "ing-1",
                        name = "Paneer",
                        quantity = "250",
                        unit = "g",
                        category = "dairy",
                        isOptional = false,
                        substituteFor = null
                    )
                ),
                instructions = listOf(
                    InstructionDto(
                        stepNumber = 1,
                        instruction = "Cut paneer into cubes",
                        durationMinutes = 5,
                        timerRequired = false,
                        tips = "Use fresh paneer"
                    )
                ),
                nutrition = NutritionDto(
                    calories = 350,
                    protein = 15,
                    carbohydrates = 20,
                    fat = 25,
                    fiber = 3,
                    sugar = 5,
                    sodium = 400
                )
            )

            // When
            val domain = dto.toDomain()

            // Then
            assertEquals("recipe-1", domain.id)
            assertEquals("Paneer Butter Masala", domain.name)
            assertEquals(CuisineType.NORTH, domain.cuisineType)
            assertEquals(Difficulty.MEDIUM, domain.difficulty)
            assertEquals(2, domain.mealTypes.size)
            assertTrue(domain.mealTypes.contains(MealType.LUNCH))
            assertTrue(domain.mealTypes.contains(MealType.DINNER))
            assertEquals(1, domain.dietaryTags.size)
            assertTrue(domain.dietaryTags.contains(DietaryTag.VEGETARIAN))
            assertEquals(1, domain.ingredients.size)
            assertEquals("Paneer", domain.ingredients.first().name)
            assertEquals(IngredientCategory.DAIRY, domain.ingredients.first().category)
            assertNotNull(domain.nutrition)
            assertEquals(350, domain.nutrition?.calories)
        }

        @Test
        @DisplayName("Should map IngredientDto to Ingredient domain")
        fun `should map IngredientDto to Ingredient domain`() {
            // Given
            val dto = IngredientDto(
                id = "ing-1",
                name = "Tomato",
                quantity = "3",
                unit = "medium",
                category = "vegetables",
                isOptional = true,
                substituteFor = "Cherry tomatoes"
            )

            // When
            val domain = dto.toDomain()

            // Then
            assertEquals("ing-1", domain.id)
            assertEquals("Tomato", domain.name)
            assertEquals("3", domain.quantity)
            assertEquals(IngredientCategory.VEGETABLES, domain.category)
            assertTrue(domain.isOptional)
            assertEquals("Cherry tomatoes", domain.substituteFor)
        }

        @Test
        @DisplayName("Should map InstructionDto to Instruction domain")
        fun `should map InstructionDto to Instruction domain`() {
            // Given
            val dto = InstructionDto(
                stepNumber = 1,
                instruction = "Heat oil in a pan",
                durationMinutes = 2,
                timerRequired = true,
                tips = "Use medium heat"
            )

            // When
            val domain = dto.toDomain()

            // Then
            assertEquals(1, domain.stepNumber)
            assertEquals("Heat oil in a pan", domain.instruction)
            assertEquals(2, domain.durationMinutes)
            assertTrue(domain.timerRequired)
            assertEquals("Use medium heat", domain.tips)
        }

        @Test
        @DisplayName("Should map NutritionDto to Nutrition domain")
        fun `should map NutritionDto to Nutrition domain`() {
            // Given
            val dto = NutritionDto(
                calories = 350,
                protein = 15,
                carbohydrates = 40,
                fat = 10,
                fiber = 5,
                sugar = 8,
                sodium = 500
            )

            // When
            val domain = dto.toDomain()

            // Then
            assertEquals(350, domain.calories)
            assertEquals(15, domain.proteinGrams)
            assertEquals(40, domain.carbohydratesGrams)
            assertEquals(10, domain.fatGrams)
            assertEquals(5, domain.fiberGrams)
        }

        @Test
        @DisplayName("Should default rating fields when absent from response (issue #21)")
        fun `should default rating fields when absent from response`() {
            val dto = RecipeResponse(
                id = "r1", name = "x", description = "y", imageUrl = null,
                prepTimeMinutes = 1, cookTimeMinutes = 1, servings = 1,
                difficulty = "easy", cuisineType = "north",
                mealTypes = emptyList(), dietaryTags = emptyList(),
                ingredients = emptyList(), instructions = emptyList(),
                nutrition = null
            )

            val domain = dto.toDomain()

            assertNull(domain.averageRating)
            assertEquals(0, domain.ratingCount)
            assertNull(domain.userRating)
        }

        @Test
        @DisplayName("Should propagate rating fields when populated (issue #21)")
        fun `should propagate rating fields when populated`() {
            val dto = RecipeResponse(
                id = "r1", name = "x", description = "y", imageUrl = null,
                prepTimeMinutes = 1, cookTimeMinutes = 1, servings = 1,
                difficulty = "easy", cuisineType = "north",
                mealTypes = emptyList(), dietaryTags = emptyList(),
                ingredients = emptyList(), instructions = emptyList(),
                nutrition = null,
                averageRating = 4.25,
                ratingCount = 12,
                userRating = 5.0
            )

            val domain = dto.toDomain()

            assertEquals(4.25, domain.averageRating)
            assertEquals(12, domain.ratingCount)
            assertEquals(5.0, domain.userRating)
        }
    }

    @Nested
    @DisplayName("MealPlan DTO Mappers")
    inner class MealPlanDtoMappers {

        @Test
        @DisplayName("Should map MealPlanResponse to MealPlan domain")
        fun `should map MealPlanResponse to MealPlan domain`() {
            // Given
            val dto = MealPlanResponse(
                id = "plan-1",
                weekStartDate = "2026-01-27",
                weekEndDate = "2026-02-02",
                days = listOf(
                    MealPlanDayDto(
                        date = "2026-01-27",
                        dayName = "Monday",
                        meals = MealsByTypeDto(
                            breakfast = listOf(
                                MealItemDto(
                                    id = "item-1",
                                    recipeId = "recipe-1",
                                    recipeName = "Poha",
                                    recipeImageUrl = null,
                                    prepTimeMinutes = 20,
                                    calories = 300,
                                    isLocked = false,
                                    order = 0,
                                    dietaryTags = listOf("vegetarian")
                                )
                            ),
                            lunch = emptyList(),
                            dinner = emptyList(),
                            snacks = emptyList()
                        ),
                        festival = null
                    )
                ),
                createdAt = "2026-01-27T10:00:00Z",
                updatedAt = "2026-01-27T10:00:00Z"
            )

            // When
            val domain = dto.toDomain()

            // Then
            assertEquals("plan-1", domain.id)
            assertEquals(LocalDate.of(2026, 1, 27), domain.weekStartDate)
            assertEquals(LocalDate.of(2026, 2, 2), domain.weekEndDate)
            assertEquals(1, domain.days.size)
            assertEquals(1, domain.days.first().breakfast.size)
            assertEquals("Poha", domain.days.first().breakfast.first().recipeName)
        }

        @Test
        @DisplayName("Should map MealItemDto to MealItem domain")
        fun `should map MealItemDto to MealItem domain`() {
            // Given
            val dto = MealItemDto(
                id = "item-1",
                recipeId = "recipe-1",
                recipeName = "Dal Tadka",
                recipeImageUrl = "https://example.com/dal.jpg",
                prepTimeMinutes = 35,
                calories = 400,
                isLocked = true,
                order = 0,
                dietaryTags = listOf("vegetarian", "vegan")
            )

            // When
            val domain = dto.toDomain()

            // Then
            assertEquals("item-1", domain.id)
            assertEquals("recipe-1", domain.recipeId)
            assertEquals("Dal Tadka", domain.recipeName)
            assertTrue(domain.isLocked)
            assertEquals(2, domain.dietaryTags.size)
        }

        @Test
        @DisplayName("Should map FestivalDto to Festival domain")
        fun `should map FestivalDto to Festival domain`() {
            // Given
            val dto = FestivalDto(
                id = "fest-1",
                name = "Diwali",
                isFastingDay = false,
                suggestedDishes = listOf("Gulab Jamun", "Kheer", "Samosa")
            )

            // When
            val domain = dto.toDomain()

            // Then
            assertEquals("fest-1", domain.id)
            assertEquals("Diwali", domain.name)
            assertFalse(domain.isFastingDay)
            assertEquals(3, domain.suggestedDishes.size)
        }

        @Test
        @DisplayName("Should handle festival with fasting day")
        fun `should handle festival with fasting day`() {
            // Given
            val dto = FestivalDto(
                id = "fest-2",
                name = "Ekadashi",
                isFastingDay = true,
                suggestedDishes = listOf("Sabudana Khichdi", "Fruit Salad")
            )

            // When
            val domain = dto.toDomain()

            // Then
            assertTrue(domain.isFastingDay)
            assertEquals(2, domain.suggestedDishes.size)
        }
    }

    @Nested
    @DisplayName("User DTO Mappers")
    inner class UserDtoMappers {

        @Test
        @DisplayName("Should map UserResponse to User domain")
        fun `should map UserResponse to User domain`() {
            // Given
            val dto = UserResponse(
                id = "user-1",
                email = "test@example.com",
                name = "Test User",
                profileImageUrl = "https://example.com/profile.jpg",
                isOnboarded = true,
                preferences = UserPreferencesDto(
                    householdSize = 4,
                    cuisinePreferences = listOf("north", "south"),
                    dietaryRestrictions = listOf("vegetarian"),
                    spiceLevel = "medium",
                    dislikedIngredients = listOf("Bitter Gourd"),
                    cookingTimePreference = "medium"
                )
            )

            // When
            val domain = dto.toDomain()

            // Then
            assertEquals("user-1", domain.id)
            assertEquals("test@example.com", domain.email)
            assertEquals("Test User", domain.name)
            assertTrue(domain.isOnboarded)
            assertNotNull(domain.preferences)
            assertEquals(4, domain.preferences?.householdSize)
            assertEquals(2, domain.preferences?.cuisinePreferences?.size)
        }

        @Test
        @DisplayName("Should map UserPreferencesDto to UserPreferences domain")
        fun `should map UserPreferencesDto to UserPreferences domain`() {
            // Given
            val dto = UserPreferencesDto(
                householdSize = 3,
                cuisinePreferences = listOf("north", "west"),
                dietaryRestrictions = listOf("vegetarian"),
                spiceLevel = "mild",
                dislikedIngredients = listOf("Onion", "Garlic"),
                cookingTimePreference = "quick"
            )

            // When
            val domain = dto.toDomain()

            // Then
            assertEquals(3, domain.householdSize)
            assertEquals(PrimaryDiet.VEGETARIAN, domain.primaryDiet)
            assertEquals(2, domain.cuisinePreferences.size)
            assertTrue(domain.cuisinePreferences.contains(CuisineType.NORTH))
            assertTrue(domain.cuisinePreferences.contains(CuisineType.WEST))
            assertEquals(2, domain.dislikedIngredients.size)
        }

        @Test
        @DisplayName("Should handle user without preferences")
        fun `should handle user without preferences`() {
            // Given
            val dto = UserResponse(
                id = "user-1",
                email = "new@example.com",
                name = "New User",
                profileImageUrl = null,
                isOnboarded = false,
                preferences = null
            )

            // When
            val domain = dto.toDomain()

            // Then
            assertEquals("user-1", domain.id)
            assertFalse(domain.isOnboarded)
            assertNull(domain.preferences)
            assertNull(domain.profileImageUrl)
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    inner class EdgeCases {

        @Test
        @DisplayName("Should handle unknown cuisine type gracefully")
        fun `should handle unknown cuisine type gracefully`() {
            // Given
            val dto = RecipeResponse(
                id = "recipe-1",
                name = "Mystery Dish",
                description = "Unknown origin",
                imageUrl = null,
                prepTimeMinutes = 10,
                cookTimeMinutes = 20,
                servings = 2,
                difficulty = "easy",
                cuisineType = "unknown_cuisine",
                mealTypes = emptyList(),
                dietaryTags = emptyList(),
                ingredients = emptyList(),
                instructions = emptyList(),
                nutrition = null
            )

            // When
            val domain = dto.toDomain()

            // Then - Should default to some cuisine type without crashing
            assertNotNull(domain)
            assertEquals("Mystery Dish", domain.name)
        }

        @Test
        @DisplayName("Should handle empty lists in recipe")
        fun `should handle empty lists in recipe`() {
            // Given
            val dto = RecipeResponse(
                id = "recipe-1",
                name = "Simple Recipe",
                description = "Basic",
                imageUrl = null,
                prepTimeMinutes = 5,
                cookTimeMinutes = 5,
                servings = 1,
                difficulty = "easy",
                cuisineType = "north",
                mealTypes = emptyList(),
                dietaryTags = emptyList(),
                ingredients = emptyList(),
                instructions = emptyList(),
                nutrition = null
            )

            // When
            val domain = dto.toDomain()

            // Then
            assertTrue(domain.mealTypes.isEmpty())
            assertTrue(domain.dietaryTags.isEmpty())
            assertTrue(domain.ingredients.isEmpty())
            assertTrue(domain.instructions.isEmpty())
            assertNull(domain.nutrition)
        }

        @Test
        @DisplayName("Should handle null optional fields")
        fun `should handle null optional fields`() {
            // Given
            val dto = IngredientDto(
                id = "ing-1",
                name = "Salt",
                quantity = "to taste",
                unit = "",
                category = "spices",
                isOptional = false,
                substituteFor = null
            )

            // When
            val domain = dto.toDomain()

            // Then
            assertNull(domain.substituteFor)
            assertEquals(IngredientCategory.SPICES, domain.category)
        }
    }

    @Nested
    @DisplayName("Household DTO Mappers")
    inner class HouseholdDtoMappers {

        @Test
        @DisplayName("Should map HouseholdResponse to HouseholdEntity")
        fun `should map HouseholdResponse to HouseholdEntity`() {
            // Given
            val dto = HouseholdResponse(
                id = "hh-1",
                name = "Sharma Family",
                inviteCode = "ABC123",
                ownerId = "user-1",
                slotConfig = mapOf("breakfast" to 2, "lunch" to 2),
                maxMembers = 8,
                memberCount = 3,
                isActive = true,
                createdAt = "2026-03-01T10:00:00",
                updatedAt = "2026-03-01T12:00:00"
            )

            // When
            val entity = dto.toEntity()

            // Then
            assertEquals("hh-1", entity.id)
            assertEquals("Sharma Family", entity.name)
            assertEquals("ABC123", entity.inviteCode)
            assertEquals("user-1", entity.ownerId)
            assertNotNull(entity.slotConfigJson)
            assertEquals(8, entity.maxMembers)
            assertEquals(3, entity.memberCount)
            assertTrue(entity.isActive)
            assertEquals("2026-03-01T10:00:00", entity.createdAt)
        }

        @Test
        @DisplayName("Should map HouseholdMemberResponse to HouseholdMemberEntity")
        fun `should map HouseholdMemberResponse to HouseholdMemberEntity`() {
            // Given
            val dto = HouseholdMemberResponse(
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
                portionSize = "REGULAR",
                status = "active"
            )

            // When
            val entity = dto.toEntity()

            // Then
            assertEquals("mem-1", entity.id)
            assertEquals("hh-1", entity.householdId)
            assertEquals("user-1", entity.userId)
            assertNull(entity.familyMemberId)
            assertEquals("Ramesh Sharma", entity.name)
            assertEquals("owner", entity.role)
            assertTrue(entity.canEditSharedPlan)
            assertFalse(entity.isTemporary)
            assertNull(entity.leaveDate)
            assertEquals(1.0f, entity.portionSize) // REGULAR maps to 1.0f
        }

        @Test
        @DisplayName("Should map HouseholdDetailResponse to HouseholdDetail domain")
        fun `should map HouseholdDetailResponse to HouseholdDetail domain`() {
            // Given
            val householdDto = HouseholdResponse(
                id = "hh-1",
                name = "Sharma Family",
                inviteCode = "ABC123",
                ownerId = "user-1",
                slotConfig = null,
                maxMembers = 8,
                memberCount = 2,
                isActive = true,
                createdAt = "2026-03-01T10:00:00",
                updatedAt = "2026-03-01T10:00:00"
            )
            val memberDto = HouseholdMemberResponse(
                id = "mem-1",
                householdId = "hh-1",
                userId = "user-1",
                familyMemberId = null,
                name = "Ramesh",
                role = "owner",
                canEditSharedPlan = true,
                isTemporary = false,
                joinDate = "2026-03-01T10:00:00",
                leaveDate = null,
                portionSize = "REGULAR",
                status = "active"
            )
            val dto = HouseholdDetailResponse(
                household = householdDto,
                members = listOf(memberDto)
            )

            // When
            val domain = dto.toDomain()

            // Then
            assertEquals("hh-1", domain.household.id)
            assertEquals("Sharma Family", domain.household.name)
            assertEquals(1, domain.members.size)
            assertEquals("Ramesh", domain.members[0].name)
            assertEquals(HouseholdRole.OWNER, domain.members[0].role)
        }

        @Test
        @DisplayName("Should map HouseholdNotificationResponse to HouseholdNotification domain")
        fun `should map HouseholdNotificationResponse to HouseholdNotification domain`() {
            // Given
            val dto = HouseholdNotificationResponse(
                id = "notif-1",
                householdId = "hh-1",
                type = "member_joined",
                title = "New Member",
                message = "Sunita joined the household",
                isRead = false,
                metadata = mapOf("member_name" to "Sunita"),
                createdAt = "2026-03-01T10:00:00"
            )

            // When
            val domain = dto.toDomain()

            // Then
            assertEquals("notif-1", domain.id)
            assertEquals("hh-1", domain.householdId)
            assertEquals(HouseholdNotificationType.MEMBER_JOINED, domain.type)
            assertEquals("New Member", domain.title)
            assertEquals("Sunita joined the household", domain.message)
            assertFalse(domain.isRead)
            assertNotNull(domain.metadata)
            assertEquals("Sunita", domain.metadata?.get("member_name"))
        }
    }
}
