package com.rasoiai.data.local.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.rasoiai.data.local.entity.RecipeEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecipeDaoTest : BaseDaoTest() {
    private val recipeDao: RecipeDao get() = database.recipeDao()

    private val testRecipe = RecipeEntity(
        id = "recipe-1",
        name = "Paneer Butter Masala",
        description = "Creamy tomato-based curry with paneer",
        imageUrl = "https://example.com/paneer.jpg",
        prepTimeMinutes = 15,
        cookTimeMinutes = 30,
        servings = 4,
        difficulty = "medium",
        cuisineType = "north",
        mealTypes = listOf("LUNCH", "DINNER"),
        dietaryTags = listOf("vegetarian"),
        ingredients = """[{"id":"1","name":"Paneer","quantity":"250","unit":"g"}]""",
        instructions = """[{"stepNumber":1,"instruction":"Cut paneer into cubes"}]""",
        nutritionInfo = """{"calories":350,"protein":15}""",
        calories = 350,
        isFavorite = false,
        cachedAt = System.currentTimeMillis()
    )

    @Test
    fun insertRecipe_andGetById_returnsRecipe() = runTest {
        // Given
        recipeDao.insertRecipe(testRecipe)

        // When & Then
        recipeDao.getRecipeById(testRecipe.id).test {
            val recipe = awaitItem()
            assertNotNull(recipe)
            assertEquals("Paneer Butter Masala", recipe?.name)
            assertEquals("north", recipe?.cuisineType)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun insertRecipe_andGetByIdSync_returnsRecipe() = runTest {
        // Given
        recipeDao.insertRecipe(testRecipe)

        // When
        val recipe = recipeDao.getRecipeByIdSync(testRecipe.id)

        // Then
        assertNotNull(recipe)
        assertEquals("Paneer Butter Masala", recipe?.name)
    }

    @Test
    fun getRecipeById_whenNotExists_returnsNull() = runTest {
        // When & Then
        recipeDao.getRecipeById("non-existent").test {
            val recipe = awaitItem()
            assertNull(recipe)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun insertMultipleRecipes_andGetAll_returnsAllSorted() = runTest {
        // Given
        val recipes = listOf(
            testRecipe.copy(id = "recipe-1", name = "Zebra Curry"),
            testRecipe.copy(id = "recipe-2", name = "Aloo Gobi"),
            testRecipe.copy(id = "recipe-3", name = "Matar Paneer")
        )
        recipeDao.insertRecipes(recipes)

        // When & Then
        recipeDao.getAllRecipes().test {
            val result = awaitItem()
            assertEquals(3, result.size)
            // Should be sorted by name
            assertEquals("Aloo Gobi", result[0].name)
            assertEquals("Matar Paneer", result[1].name)
            assertEquals("Zebra Curry", result[2].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getRecipesByIds_returnsMatchingRecipes() = runTest {
        // Given
        val recipes = listOf(
            testRecipe.copy(id = "recipe-1", name = "Recipe One"),
            testRecipe.copy(id = "recipe-2", name = "Recipe Two"),
            testRecipe.copy(id = "recipe-3", name = "Recipe Three")
        )
        recipeDao.insertRecipes(recipes)

        // When & Then
        recipeDao.getRecipesByIds(listOf("recipe-1", "recipe-3")).test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertTrue(result.any { it.id == "recipe-1" })
            assertTrue(result.any { it.id == "recipe-3" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun updateFavoriteStatus_updatesRecipe() = runTest {
        // Given
        recipeDao.insertRecipe(testRecipe)

        // When
        recipeDao.updateFavoriteStatus(testRecipe.id, true)

        // Then
        recipeDao.getRecipeById(testRecipe.id).test {
            val recipe = awaitItem()
            assertTrue(recipe?.isFavorite == true)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getFavoriteRecipes_returnsOnlyFavorites() = runTest {
        // Given
        val recipes = listOf(
            testRecipe.copy(id = "recipe-1", name = "Favorite Recipe", isFavorite = true),
            testRecipe.copy(id = "recipe-2", name = "Normal Recipe", isFavorite = false),
            testRecipe.copy(id = "recipe-3", name = "Another Favorite", isFavorite = true)
        )
        recipeDao.insertRecipes(recipes)

        // When & Then
        recipeDao.getFavoriteRecipes().test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertTrue(result.all { it.isFavorite })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getRecipesByCuisine_returnsMatchingCuisine() = runTest {
        // Given
        val recipes = listOf(
            testRecipe.copy(id = "recipe-1", name = "North Recipe", cuisineType = "north"),
            testRecipe.copy(id = "recipe-2", name = "South Recipe", cuisineType = "south"),
            testRecipe.copy(id = "recipe-3", name = "Another North", cuisineType = "north")
        )
        recipeDao.insertRecipes(recipes)

        // When & Then
        recipeDao.getRecipesByCuisine("north").test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertTrue(result.all { it.cuisineType == "north" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteRecipe_removesRecipe() = runTest {
        // Given
        recipeDao.insertRecipe(testRecipe)

        // When
        recipeDao.deleteRecipe(testRecipe.id)

        // Then
        val recipe = recipeDao.getRecipeByIdSync(testRecipe.id)
        assertNull(recipe)
    }

    @Test
    fun getRecipeCount_returnsCorrectCount() = runTest {
        // Given
        assertEquals(0, recipeDao.getRecipeCount())

        // When
        recipeDao.insertRecipes(listOf(
            testRecipe.copy(id = "recipe-1"),
            testRecipe.copy(id = "recipe-2"),
            testRecipe.copy(id = "recipe-3")
        ))

        // Then
        assertEquals(3, recipeDao.getRecipeCount())
    }

    @Test
    fun deleteOldCachedRecipes_removesOldRecipes() = runTest {
        // Given
        val oldTimestamp = System.currentTimeMillis() - 86400000 // 1 day ago
        val newTimestamp = System.currentTimeMillis()

        val recipes = listOf(
            testRecipe.copy(id = "old-recipe", cachedAt = oldTimestamp),
            testRecipe.copy(id = "new-recipe", cachedAt = newTimestamp)
        )
        recipeDao.insertRecipes(recipes)

        // When - delete recipes older than 12 hours
        val cutoff = System.currentTimeMillis() - 43200000
        recipeDao.deleteOldCachedRecipes(cutoff)

        // Then
        assertEquals(1, recipeDao.getRecipeCount())
        assertNull(recipeDao.getRecipeByIdSync("old-recipe"))
        assertNotNull(recipeDao.getRecipeByIdSync("new-recipe"))
    }

    @Test
    fun insertRecipe_withConflict_replacesExisting() = runTest {
        // Given
        recipeDao.insertRecipe(testRecipe)

        // When - insert with same ID but different name
        val updatedRecipe = testRecipe.copy(name = "Updated Name")
        recipeDao.insertRecipe(updatedRecipe)

        // Then
        recipeDao.getRecipeById(testRecipe.id).test {
            val recipe = awaitItem()
            assertEquals("Updated Name", recipe?.name)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
