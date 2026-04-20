package com.rasoiai.domain.usecase

import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.Difficulty
import com.rasoiai.domain.model.Recipe
import com.rasoiai.domain.repository.RecipeRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class GetRecipeUseCaseTest {

    private lateinit var repository: RecipeRepository
    private lateinit var useCase: GetRecipeUseCase

    @BeforeEach
    fun setup() {
        repository = mockk()
        useCase = GetRecipeUseCase(repository)
    }

    @Test
    @DisplayName("Should emit recipe when repository has it")
    fun `should emit recipe when repository has it`() = runTest {
        val recipe = testRecipe("r-1", "Dal Tadka")
        every { repository.getRecipeById("r-1") } returns flowOf(recipe)

        val result = useCase("r-1").first()

        assertEquals(recipe, result)
    }

    @Test
    @DisplayName("Should emit null when recipe does not exist")
    fun `should emit null when recipe does not exist`() = runTest {
        every { repository.getRecipeById("missing") } returns flowOf(null)

        val result = useCase("missing").first()

        assertNull(result)
    }

    @Test
    @DisplayName("Should pass recipeId through to repository unchanged")
    fun `should pass recipeId through to repository unchanged`() = runTest {
        every { repository.getRecipeById(any()) } returns flowOf(null)

        useCase("some-id-123").first()

        verify { repository.getRecipeById("some-id-123") }
    }

    private fun testRecipe(id: String, name: String) = Recipe(
        id = id,
        name = name,
        description = "",
        imageUrl = null,
        prepTimeMinutes = 15,
        cookTimeMinutes = 30,
        servings = 4,
        difficulty = Difficulty.EASY,
        cuisineType = CuisineType.NORTH,
        mealTypes = emptyList(),
        dietaryTags = emptyList(),
        ingredients = emptyList(),
        instructions = emptyList(),
        nutrition = null,
        isFavorite = false
    )
}
