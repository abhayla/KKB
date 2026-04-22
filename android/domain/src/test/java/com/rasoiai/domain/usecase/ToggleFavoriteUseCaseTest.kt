package com.rasoiai.domain.usecase

import com.rasoiai.domain.repository.RecipeRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ToggleFavoriteUseCaseTest {

    private lateinit var repository: RecipeRepository
    private lateinit var useCase: ToggleFavoriteUseCase

    @BeforeEach
    fun setup() {
        repository = mockk()
        useCase = ToggleFavoriteUseCase(repository)
    }

    @Test
    @DisplayName("Should return success(true) when recipe becomes favorited")
    fun `should return success true when recipe becomes favorited`() = runTest {
        coEvery { repository.toggleFavorite("r-1") } returns Result.success(true)

        val result = useCase("r-1")

        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrNull())
    }

    @Test
    @DisplayName("Should return success(false) when recipe becomes unfavorited")
    fun `should return success false when recipe becomes unfavorited`() = runTest {
        coEvery { repository.toggleFavorite("r-2") } returns Result.success(false)

        val result = useCase("r-2")

        assertTrue(result.isSuccess)
        assertEquals(false, result.getOrNull())
    }

    @Test
    @DisplayName("Should propagate repository failure without swallowing")
    fun `should propagate repository failure without swallowing`() = runTest {
        val boom = IllegalStateException("disk full")
        coEvery { repository.toggleFavorite(any()) } returns Result.failure(boom)

        val result = useCase("r-3")

        assertTrue(result.isFailure)
        assertEquals(boom, result.exceptionOrNull())
    }

    @Test
    @DisplayName("Should forward the recipeId to repository verbatim")
    fun `should forward the recipeId to repository verbatim`() = runTest {
        coEvery { repository.toggleFavorite(any()) } returns Result.success(true)

        useCase("abc-xyz-999")

        coVerify { repository.toggleFavorite("abc-xyz-999") }
    }
}
