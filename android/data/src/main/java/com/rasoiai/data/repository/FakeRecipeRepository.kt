package com.rasoiai.data.repository

import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.DietaryTag
import com.rasoiai.domain.model.Difficulty
import com.rasoiai.domain.model.Ingredient
import com.rasoiai.domain.model.IngredientCategory
import com.rasoiai.domain.model.Instruction
import com.rasoiai.domain.model.MealType
import com.rasoiai.domain.model.Nutrition
import com.rasoiai.domain.model.Recipe
import com.rasoiai.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake implementation of RecipeRepository for development and testing.
 * Provides sample Indian recipe data.
 */
@Singleton
class FakeRecipeRepository @Inject constructor() : RecipeRepository {

    private val recipes = MutableStateFlow(createSampleRecipes())

    override fun getRecipeById(id: String): Flow<Recipe?> {
        return recipes.map { list -> list.find { it.id == id } }
    }

    override fun getRecipesByIds(ids: List<String>): Flow<List<Recipe>> {
        return recipes.map { list -> list.filter { it.id in ids } }
    }

    override suspend fun searchRecipes(
        query: String?,
        cuisine: CuisineType?,
        dietary: DietaryTag?,
        mealType: MealType?,
        page: Int,
        limit: Int
    ): Result<List<Recipe>> {
        return try {
            var filtered = recipes.value

            query?.let { q ->
                filtered = filtered.filter {
                    it.name.contains(q, ignoreCase = true) ||
                    it.description.contains(q, ignoreCase = true)
                }
            }
            cuisine?.let { c -> filtered = filtered.filter { it.cuisineType == c } }
            dietary?.let { d -> filtered = filtered.filter { d in it.dietaryTags } }
            mealType?.let { m -> filtered = filtered.filter { m in it.mealTypes } }

            val offset = (page - 1) * limit
            Result.success(filtered.drop(offset).take(limit))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun scaleRecipe(recipeId: String, servings: Int): Result<Recipe> {
        return try {
            val recipe = recipes.value.find { it.id == recipeId }
                ?: return Result.failure(Exception("Recipe not found"))

            val scaleFactor = servings.toDouble() / recipe.servings
            val scaledRecipe = recipe.copy(
                servings = servings,
                ingredients = recipe.ingredients.map { ingredient ->
                    val originalQty = ingredient.quantity.toDoubleOrNull()
                    if (originalQty != null) {
                        val scaledQty = originalQty * scaleFactor
                        val formattedQty = if (scaledQty == scaledQty.toLong().toDouble()) {
                            scaledQty.toLong().toString()
                        } else {
                            String.format("%.1f", scaledQty)
                        }
                        ingredient.copy(quantity = formattedQty)
                    } else {
                        ingredient
                    }
                },
                nutrition = recipe.nutrition?.let { n ->
                    Nutrition(
                        calories = (n.calories * scaleFactor).toInt(),
                        proteinGrams = (n.proteinGrams * scaleFactor).toInt(),
                        carbohydratesGrams = (n.carbohydratesGrams * scaleFactor).toInt(),
                        fatGrams = (n.fatGrams * scaleFactor).toInt(),
                        fiberGrams = (n.fiberGrams * scaleFactor).toInt(),
                        sugarGrams = (n.sugarGrams * scaleFactor).toInt(),
                        sodiumMg = (n.sodiumMg * scaleFactor).toInt()
                    )
                }
            )
            Result.success(scaledRecipe)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleFavorite(recipeId: String): Result<Boolean> {
        return try {
            val updatedRecipes = recipes.value.map { recipe ->
                if (recipe.id == recipeId) {
                    recipe.copy(isFavorite = !recipe.isFavorite)
                } else recipe
            }
            recipes.value = updatedRecipes
            val newState = updatedRecipes.find { it.id == recipeId }?.isFavorite ?: false
            Result.success(newState)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getFavoriteRecipes(): Flow<List<Recipe>> {
        return recipes.map { list -> list.filter { it.isFavorite } }
    }

    private fun createSampleRecipes(): List<Recipe> {
        return listOf(
            createDalTadka().copy(isFavorite = true),
            createPalakPaneer().copy(isFavorite = true),
            createAlooParatha().copy(isFavorite = true),
            createMasalaDosa().copy(isFavorite = true),
            createRajmaMasala(),
            createPaneerButterMasala().copy(isFavorite = true),
            createVegBiryani().copy(isFavorite = true),
            createCholeBhature(),
            createIdliSambar().copy(isFavorite = true),
            createMalaiKofta().copy(isFavorite = true)
        )
    }

    private fun createDalTadka(): Recipe {
        return Recipe(
            id = "dal-tadka",
            name = "Dal Tadka",
            description = "A classic North Indian comfort food made with yellow lentils tempered with aromatic spices. Perfect with rice or roti.",
            imageUrl = null,
            prepTimeMinutes = 10,
            cookTimeMinutes = 25,
            servings = 4,
            difficulty = Difficulty.EASY,
            cuisineType = CuisineType.NORTH,
            mealTypes = listOf(MealType.LUNCH, MealType.DINNER),
            dietaryTags = listOf(DietaryTag.VEGETARIAN, DietaryTag.VEGAN),
            ingredients = listOf(
                Ingredient("1", "Toor dal (split pigeon peas)", "1", "cup", IngredientCategory.PULSES),
                Ingredient("2", "Onion, finely chopped", "1", "medium", IngredientCategory.VEGETABLES),
                Ingredient("3", "Tomatoes, pureed", "2", "medium", IngredientCategory.VEGETABLES),
                Ingredient("4", "Garlic, minced", "4", "cloves", IngredientCategory.VEGETABLES),
                Ingredient("5", "Ginger, grated", "1", "inch", IngredientCategory.VEGETABLES),
                Ingredient("6", "Cumin seeds", "1", "tsp", IngredientCategory.SPICES),
                Ingredient("7", "Turmeric powder", "0.5", "tsp", IngredientCategory.SPICES),
                Ingredient("8", "Red chili powder", "1", "tsp", IngredientCategory.SPICES),
                Ingredient("9", "Ghee", "2", "tbsp", IngredientCategory.OILS),
                Ingredient("10", "Fresh coriander for garnish", "2", "tbsp", IngredientCategory.VEGETABLES, isOptional = true),
                Ingredient("11", "Salt", "1", "tsp", IngredientCategory.SPICES)
            ),
            instructions = listOf(
                Instruction(1, "Wash and soak toor dal for 30 minutes. Pressure cook with turmeric and salt for 3 whistles.", 35, false, "You can also cook in an Instant Pot for 15 minutes.", "https://images.unsplash.com/photo-1546833999-b9f581a1996d?w=800"),
                Instruction(2, "Heat ghee in a pan. Add cumin seeds and let them splutter.", 2, false, null, "https://images.unsplash.com/photo-1585937421612-70a008356fbe?w=800"),
                Instruction(3, "Add chopped onions and saute until golden brown (5-7 minutes).", 7, false, "Low heat gives better color and flavor.", "https://images.unsplash.com/photo-1580959375944-abd7e991f971?w=800"),
                Instruction(4, "Add ginger-garlic and saute for 1 minute until fragrant.", 1, false, null, "https://images.unsplash.com/photo-1615485290382-441e4d049cb5?w=800"),
                Instruction(5, "Add tomato puree and cook until oil separates (5 minutes).", 5, false, null, "https://images.unsplash.com/photo-1596560548464-f010549b84d7?w=800"),
                Instruction(6, "Add red chili powder and mix well. Add the cooked dal and simmer for 5 minutes.", 5, false, "Adjust consistency with water if needed.", "https://images.unsplash.com/photo-1546833999-b9f581a1996d?w=800")
            ),
            nutrition = Nutrition(
                calories = 180,
                proteinGrams = 12,
                carbohydratesGrams = 22,
                fatGrams = 5,
                fiberGrams = 6,
                sugarGrams = 3,
                sodiumMg = 380
            )
        )
    }

    private fun createPalakPaneer(): Recipe {
        return Recipe(
            id = "palak-paneer",
            name = "Palak Paneer",
            description = "Creamy spinach curry with soft paneer cubes. A protein-rich vegetarian dish loved across India.",
            imageUrl = null,
            prepTimeMinutes = 15,
            cookTimeMinutes = 25,
            servings = 4,
            difficulty = Difficulty.MEDIUM,
            cuisineType = CuisineType.NORTH,
            mealTypes = listOf(MealType.LUNCH, MealType.DINNER),
            dietaryTags = listOf(DietaryTag.VEGETARIAN),
            ingredients = listOf(
                Ingredient("1", "Fresh spinach", "500", "grams", IngredientCategory.VEGETABLES),
                Ingredient("2", "Paneer, cubed", "250", "grams", IngredientCategory.DAIRY),
                Ingredient("3", "Onion, chopped", "1", "large", IngredientCategory.VEGETABLES),
                Ingredient("4", "Tomatoes, chopped", "2", "medium", IngredientCategory.VEGETABLES),
                Ingredient("5", "Ginger-garlic paste", "1", "tbsp", IngredientCategory.SPICES),
                Ingredient("6", "Green chilies", "2", "pieces", IngredientCategory.VEGETABLES),
                Ingredient("7", "Cumin seeds", "1", "tsp", IngredientCategory.SPICES),
                Ingredient("8", "Garam masala", "0.5", "tsp", IngredientCategory.SPICES),
                Ingredient("9", "Fresh cream", "2", "tbsp", IngredientCategory.DAIRY, isOptional = true),
                Ingredient("10", "Oil", "3", "tbsp", IngredientCategory.OILS),
                Ingredient("11", "Salt to taste", "1", "tsp", IngredientCategory.SPICES)
            ),
            instructions = listOf(
                Instruction(1, "Blanch spinach in boiling water for 2 minutes. Drain and blend into a smooth puree.", 5, false, "Adding ice water after blanching keeps the green color bright.", "https://images.unsplash.com/photo-1576045057995-568f588f82fb?w=800"),
                Instruction(2, "Heat oil and fry paneer cubes until golden. Set aside.", 5, false, "Don't over-fry; paneer should be soft inside.", "https://images.unsplash.com/photo-1631452180519-c014fe946bc7?w=800"),
                Instruction(3, "In the same oil, add cumin seeds. When they splutter, add onions and saute until golden.", 5, false, null, "https://images.unsplash.com/photo-1580959375944-abd7e991f971?w=800"),
                Instruction(4, "Add ginger-garlic paste and green chilies. Cook for 1 minute.", 1, false, null, "https://images.unsplash.com/photo-1615485290382-441e4d049cb5?w=800"),
                Instruction(5, "Add tomatoes and cook until soft (3-4 minutes).", 4, false, null, "https://images.unsplash.com/photo-1596560548464-f010549b84d7?w=800"),
                Instruction(6, "Add spinach puree, garam masala, and salt. Simmer for 5 minutes.", 5, false, null, "https://images.unsplash.com/photo-1574653853027-5382a3d23a15?w=800"),
                Instruction(7, "Add fried paneer and cream. Mix gently and cook for 2 more minutes.", 2, false, "Add cream off heat for better taste.", "https://images.unsplash.com/photo-1609502665737-4d9fd81a8e1c?w=800")
            ),
            nutrition = Nutrition(
                calories = 320,
                proteinGrams = 18,
                carbohydratesGrams = 12,
                fatGrams = 24,
                fiberGrams = 4,
                sugarGrams = 5,
                sodiumMg = 450
            )
        )
    }

    private fun createAlooParatha(): Recipe {
        return Recipe(
            id = "paratha",
            name = "Aloo Paratha",
            description = "Stuffed Indian flatbread with spiced potato filling. A hearty breakfast favorite from Punjab.",
            imageUrl = null,
            prepTimeMinutes = 20,
            cookTimeMinutes = 20,
            servings = 4,
            difficulty = Difficulty.MEDIUM,
            cuisineType = CuisineType.NORTH,
            mealTypes = listOf(MealType.BREAKFAST),
            dietaryTags = listOf(DietaryTag.VEGETARIAN),
            ingredients = listOf(
                Ingredient("1", "Whole wheat flour", "2", "cups", IngredientCategory.GRAINS),
                Ingredient("2", "Potatoes, boiled and mashed", "3", "medium", IngredientCategory.VEGETABLES),
                Ingredient("3", "Green chilies, chopped", "2", "pieces", IngredientCategory.VEGETABLES),
                Ingredient("4", "Fresh coriander, chopped", "3", "tbsp", IngredientCategory.VEGETABLES),
                Ingredient("5", "Cumin powder", "1", "tsp", IngredientCategory.SPICES),
                Ingredient("6", "Red chili powder", "0.5", "tsp", IngredientCategory.SPICES),
                Ingredient("7", "Amchur (dry mango powder)", "0.5", "tsp", IngredientCategory.SPICES),
                Ingredient("8", "Ghee for cooking", "4", "tbsp", IngredientCategory.OILS),
                Ingredient("9", "Salt to taste", "1", "tsp", IngredientCategory.SPICES)
            ),
            instructions = listOf(
                Instruction(1, "Make a soft dough with flour, salt, and water. Rest for 15 minutes.", 15, false, "Soft dough makes softer parathas."),
                Instruction(2, "Mix mashed potatoes with green chilies, coriander, and all spices.", 5, false, null),
                Instruction(3, "Divide dough into 8 balls. Roll each into a small circle.", 2, false, null),
                Instruction(4, "Place potato filling in center, seal edges, and roll gently into a flat circle.", 3, false, "Don't press too hard or filling will come out."),
                Instruction(5, "Cook on hot tawa with ghee until golden brown on both sides.", 4, true, null)
            ),
            nutrition = Nutrition(
                calories = 380,
                proteinGrams = 8,
                carbohydratesGrams = 52,
                fatGrams = 16,
                fiberGrams = 5,
                sugarGrams = 2,
                sodiumMg = 520
            )
        )
    }

    private fun createMasalaDosa(): Recipe {
        return Recipe(
            id = "dosa",
            name = "Masala Dosa",
            description = "Crispy South Indian crepe filled with spiced potato masala. Served with sambar and chutneys.",
            imageUrl = null,
            prepTimeMinutes = 15,
            cookTimeMinutes = 20,
            servings = 4,
            difficulty = Difficulty.MEDIUM,
            cuisineType = CuisineType.SOUTH,
            mealTypes = listOf(MealType.BREAKFAST, MealType.DINNER),
            dietaryTags = listOf(DietaryTag.VEGETARIAN, DietaryTag.VEGAN),
            ingredients = listOf(
                Ingredient("1", "Dosa batter (fermented)", "3", "cups", IngredientCategory.GRAINS),
                Ingredient("2", "Potatoes, boiled and cubed", "4", "medium", IngredientCategory.VEGETABLES),
                Ingredient("3", "Onion, sliced", "2", "large", IngredientCategory.VEGETABLES),
                Ingredient("4", "Mustard seeds", "1", "tsp", IngredientCategory.SPICES),
                Ingredient("5", "Curry leaves", "10", "pieces", IngredientCategory.SPICES),
                Ingredient("6", "Green chilies, slit", "3", "pieces", IngredientCategory.VEGETABLES),
                Ingredient("7", "Turmeric powder", "0.5", "tsp", IngredientCategory.SPICES),
                Ingredient("8", "Oil", "4", "tbsp", IngredientCategory.OILS),
                Ingredient("9", "Salt to taste", "1", "tsp", IngredientCategory.SPICES)
            ),
            instructions = listOf(
                Instruction(1, "Heat oil, add mustard seeds and let them splutter. Add curry leaves.", 2, false, null),
                Instruction(2, "Add sliced onions and green chilies. Saute until onions are soft.", 5, false, null),
                Instruction(3, "Add turmeric and potatoes. Mix well and season with salt. Keep warm.", 3, false, null),
                Instruction(4, "Heat a non-stick tawa. Pour a ladleful of batter and spread in circular motion.", 1, false, "The tawa should be hot but not smoking."),
                Instruction(5, "Drizzle oil around edges. Cook until golden and crispy.", 3, false, null),
                Instruction(6, "Place potato filling in center and fold dosa. Serve with sambar and chutney.", 1, false, null)
            ),
            nutrition = Nutrition(
                calories = 350,
                proteinGrams = 8,
                carbohydratesGrams = 58,
                fatGrams = 10,
                fiberGrams = 4,
                sugarGrams = 4,
                sodiumMg = 380
            )
        )
    }

    private fun createRajmaMasala(): Recipe {
        return Recipe(
            id = "rajma",
            name = "Rajma Masala",
            description = "Creamy kidney bean curry in tomato-onion gravy. A Punjabi comfort food classic, best with steamed rice.",
            imageUrl = null,
            prepTimeMinutes = 20,
            cookTimeMinutes = 40,
            servings = 4,
            difficulty = Difficulty.EASY,
            cuisineType = CuisineType.NORTH,
            mealTypes = listOf(MealType.LUNCH, MealType.DINNER),
            dietaryTags = listOf(DietaryTag.VEGETARIAN, DietaryTag.VEGAN),
            ingredients = listOf(
                Ingredient("1", "Rajma (kidney beans), soaked overnight", "1.5", "cups", IngredientCategory.PULSES),
                Ingredient("2", "Onions, finely chopped", "2", "large", IngredientCategory.VEGETABLES),
                Ingredient("3", "Tomatoes, pureed", "3", "medium", IngredientCategory.VEGETABLES),
                Ingredient("4", "Ginger-garlic paste", "1.5", "tbsp", IngredientCategory.SPICES),
                Ingredient("5", "Cumin seeds", "1", "tsp", IngredientCategory.SPICES),
                Ingredient("6", "Coriander powder", "1", "tbsp", IngredientCategory.SPICES),
                Ingredient("7", "Red chili powder", "1", "tsp", IngredientCategory.SPICES),
                Ingredient("8", "Garam masala", "0.5", "tsp", IngredientCategory.SPICES),
                Ingredient("9", "Oil", "3", "tbsp", IngredientCategory.OILS),
                Ingredient("10", "Salt to taste", "1.5", "tsp", IngredientCategory.SPICES),
                Ingredient("11", "Fresh coriander for garnish", "2", "tbsp", IngredientCategory.VEGETABLES, isOptional = true)
            ),
            instructions = listOf(
                Instruction(1, "Pressure cook soaked rajma with salt for 6-7 whistles until soft.", 30, true, "Add a pinch of baking soda for faster cooking."),
                Instruction(2, "Heat oil, add cumin seeds. When they splutter, add onions and cook until brown.", 8, false, null),
                Instruction(3, "Add ginger-garlic paste and cook for 2 minutes.", 2, false, null),
                Instruction(4, "Add tomato puree and all dry spices. Cook until oil separates.", 8, false, null),
                Instruction(5, "Add cooked rajma with its water. Simmer for 15-20 minutes.", 20, false, "Mash a few beans for thicker gravy."),
                Instruction(6, "Garnish with coriander and serve with rice.", 1, false, null)
            ),
            nutrition = Nutrition(
                calories = 220,
                proteinGrams = 14,
                carbohydratesGrams = 35,
                fatGrams = 4,
                fiberGrams = 11,
                sugarGrams = 4,
                sodiumMg = 480
            )
        )
    }

    private fun createPaneerButterMasala(): Recipe {
        return Recipe(
            id = "paneer-butter-masala",
            name = "Paneer Butter Masala",
            description = "Rich and creamy tomato-based curry with soft paneer cubes. A restaurant-style North Indian favorite.",
            imageUrl = null,
            prepTimeMinutes = 15,
            cookTimeMinutes = 25,
            servings = 4,
            difficulty = Difficulty.MEDIUM,
            cuisineType = CuisineType.NORTH,
            mealTypes = listOf(MealType.LUNCH, MealType.DINNER),
            dietaryTags = listOf(DietaryTag.VEGETARIAN),
            ingredients = listOf(
                Ingredient("1", "Paneer, cubed", "300", "grams", IngredientCategory.DAIRY),
                Ingredient("2", "Tomatoes, blanched and pureed", "4", "large", IngredientCategory.VEGETABLES),
                Ingredient("3", "Cashew nuts, soaked", "15", "pieces", IngredientCategory.NUTS),
                Ingredient("4", "Butter", "3", "tbsp", IngredientCategory.DAIRY),
                Ingredient("5", "Fresh cream", "4", "tbsp", IngredientCategory.DAIRY),
                Ingredient("6", "Kashmiri red chili powder", "1", "tsp", IngredientCategory.SPICES),
                Ingredient("7", "Garam masala", "0.5", "tsp", IngredientCategory.SPICES),
                Ingredient("8", "Sugar", "1", "tsp", IngredientCategory.SWEETENERS),
                Ingredient("9", "Kasuri methi (dried fenugreek)", "1", "tbsp", IngredientCategory.SPICES),
                Ingredient("10", "Salt to taste", "1", "tsp", IngredientCategory.SPICES)
            ),
            instructions = listOf(
                Instruction(1, "Blend soaked cashews with 2 tbsp water to make a smooth paste.", 2, false, null),
                Instruction(2, "Heat butter in a pan. Add tomato puree and cook for 10 minutes.", 10, false, "Stir occasionally to prevent burning."),
                Instruction(3, "Add red chili powder, salt, and sugar. Mix well.", 2, false, null),
                Instruction(4, "Add cashew paste and cook for 5 minutes until gravy thickens.", 5, false, null),
                Instruction(5, "Add paneer cubes and simmer for 3-4 minutes.", 4, false, null),
                Instruction(6, "Add cream, garam masala, and crushed kasuri methi. Mix gently.", 2, false, "Crush kasuri methi between palms for better aroma.")
            ),
            nutrition = Nutrition(
                calories = 380,
                proteinGrams = 16,
                carbohydratesGrams = 18,
                fatGrams = 28,
                fiberGrams = 3,
                sugarGrams = 8,
                sodiumMg = 520
            )
        )
    }

    private fun createVegBiryani(): Recipe {
        return Recipe(
            id = "biryani",
            name = "Veg Biryani",
            description = "Fragrant basmati rice layered with spiced vegetables and aromatic herbs. A celebration dish for special occasions.",
            imageUrl = null,
            prepTimeMinutes = 30,
            cookTimeMinutes = 45,
            servings = 6,
            difficulty = Difficulty.HARD,
            cuisineType = CuisineType.SOUTH,
            mealTypes = listOf(MealType.LUNCH, MealType.DINNER),
            dietaryTags = listOf(DietaryTag.VEGETARIAN),
            ingredients = listOf(
                Ingredient("1", "Basmati rice, soaked", "2", "cups", IngredientCategory.GRAINS),
                Ingredient("2", "Mixed vegetables (carrots, beans, peas)", "2", "cups", IngredientCategory.VEGETABLES),
                Ingredient("3", "Onions, thinly sliced", "3", "large", IngredientCategory.VEGETABLES),
                Ingredient("4", "Yogurt", "1", "cup", IngredientCategory.DAIRY),
                Ingredient("5", "Biryani masala", "2", "tbsp", IngredientCategory.SPICES),
                Ingredient("6", "Saffron, soaked in milk", "0.25", "tsp", IngredientCategory.SPICES),
                Ingredient("7", "Ghee", "4", "tbsp", IngredientCategory.OILS),
                Ingredient("8", "Mint leaves", "0.5", "cup", IngredientCategory.VEGETABLES),
                Ingredient("9", "Coriander leaves", "0.5", "cup", IngredientCategory.VEGETABLES),
                Ingredient("10", "Green chilies, slit", "4", "pieces", IngredientCategory.VEGETABLES),
                Ingredient("11", "Whole spices (bay leaf, cinnamon, cardamom, cloves)", "1", "set", IngredientCategory.SPICES),
                Ingredient("12", "Salt to taste", "2", "tsp", IngredientCategory.SPICES)
            ),
            instructions = listOf(
                Instruction(1, "Fry sliced onions in ghee until deep golden brown (birista). Set half aside for garnish.", 15, false, "Be patient - good birista takes time."),
                Instruction(2, "Add whole spices and vegetables. Saute for 5 minutes.", 5, false, null),
                Instruction(3, "Add yogurt, biryani masala, mint, and coriander. Cook for 10 minutes.", 10, false, null),
                Instruction(4, "Parboil rice with whole spices until 70% done. Drain.", 8, false, "Rice should still have a bite."),
                Instruction(5, "Layer rice over vegetables. Top with saffron milk and fried onions.", 3, false, null),
                Instruction(6, "Cover tightly and cook on low heat (dum) for 25 minutes.", 25, true, "Use a heavy-bottomed pot or seal with dough."),
                Instruction(7, "Gently mix and serve with raita.", 2, false, null)
            ),
            nutrition = Nutrition(
                calories = 450,
                proteinGrams = 10,
                carbohydratesGrams = 65,
                fatGrams = 16,
                fiberGrams = 5,
                sugarGrams = 6,
                sodiumMg = 680
            )
        )
    }

    private fun createCholeBhature(): Recipe {
        return Recipe(
            id = "chole-bhature",
            name = "Chole Bhature",
            description = "Spicy chickpea curry served with fluffy deep-fried bread. A popular Punjabi street food and breakfast dish.",
            imageUrl = null,
            prepTimeMinutes = 30,
            cookTimeMinutes = 40,
            servings = 4,
            difficulty = Difficulty.MEDIUM,
            cuisineType = CuisineType.NORTH,
            mealTypes = listOf(MealType.BREAKFAST, MealType.LUNCH),
            dietaryTags = listOf(DietaryTag.VEGETARIAN),
            ingredients = listOf(
                Ingredient("1", "Chickpeas, soaked overnight", "2", "cups", IngredientCategory.PULSES),
                Ingredient("2", "Tea bags (for dark color)", "2", "pieces", IngredientCategory.OTHER),
                Ingredient("3", "Onions, finely chopped", "2", "large", IngredientCategory.VEGETABLES),
                Ingredient("4", "Tomatoes, pureed", "3", "medium", IngredientCategory.VEGETABLES),
                Ingredient("5", "Chole masala", "2", "tbsp", IngredientCategory.SPICES),
                Ingredient("6", "All-purpose flour (maida)", "2", "cups", IngredientCategory.GRAINS),
                Ingredient("7", "Semolina (sooji)", "2", "tbsp", IngredientCategory.GRAINS),
                Ingredient("8", "Yogurt", "0.25", "cup", IngredientCategory.DAIRY),
                Ingredient("9", "Baking powder", "0.5", "tsp", IngredientCategory.OTHER),
                Ingredient("10", "Oil for frying", "500", "ml", IngredientCategory.OILS),
                Ingredient("11", "Salt to taste", "1.5", "tsp", IngredientCategory.SPICES)
            ),
            instructions = listOf(
                Instruction(1, "Pressure cook chickpeas with tea bags and salt for 5-6 whistles.", 25, true, "Tea gives the authentic dark color."),
                Instruction(2, "Make bhature dough with maida, sooji, yogurt, baking powder, and salt. Rest 2 hours.", 5, false, "Dough should be soft but not sticky."),
                Instruction(3, "Heat oil, saute onions until golden. Add tomato puree and chole masala.", 10, false, null),
                Instruction(4, "Add cooked chickpeas and simmer for 15 minutes.", 15, false, "Mash a few chickpeas for thicker gravy."),
                Instruction(5, "Roll bhature into oval shapes. Deep fry until puffed and golden.", 10, false, "Oil should be hot but not smoking.")
            ),
            nutrition = Nutrition(
                calories = 520,
                proteinGrams = 15,
                carbohydratesGrams = 68,
                fatGrams = 20,
                fiberGrams = 8,
                sugarGrams = 6,
                sodiumMg = 720
            )
        )
    }

    private fun createIdliSambar(): Recipe {
        return Recipe(
            id = "idli",
            name = "Idli Sambar",
            description = "Soft steamed rice cakes served with flavorful lentil vegetable stew and coconut chutney. A healthy South Indian breakfast.",
            imageUrl = null,
            prepTimeMinutes = 20,
            cookTimeMinutes = 30,
            servings = 4,
            difficulty = Difficulty.EASY,
            cuisineType = CuisineType.SOUTH,
            mealTypes = listOf(MealType.BREAKFAST),
            dietaryTags = listOf(DietaryTag.VEGETARIAN, DietaryTag.VEGAN),
            ingredients = listOf(
                Ingredient("1", "Idli batter (fermented)", "3", "cups", IngredientCategory.GRAINS),
                Ingredient("2", "Toor dal", "0.5", "cup", IngredientCategory.PULSES),
                Ingredient("3", "Sambar powder", "2", "tbsp", IngredientCategory.SPICES),
                Ingredient("4", "Drumstick, cut into pieces", "1", "piece", IngredientCategory.VEGETABLES),
                Ingredient("5", "Carrots, cubed", "1", "medium", IngredientCategory.VEGETABLES),
                Ingredient("6", "Onion, quartered", "1", "medium", IngredientCategory.VEGETABLES),
                Ingredient("7", "Tomato, chopped", "1", "medium", IngredientCategory.VEGETABLES),
                Ingredient("8", "Tamarind paste", "1", "tbsp", IngredientCategory.SPICES),
                Ingredient("9", "Mustard seeds", "1", "tsp", IngredientCategory.SPICES),
                Ingredient("10", "Curry leaves", "10", "pieces", IngredientCategory.SPICES),
                Ingredient("11", "Oil", "2", "tbsp", IngredientCategory.OILS),
                Ingredient("12", "Salt to taste", "1.5", "tsp", IngredientCategory.SPICES)
            ),
            instructions = listOf(
                Instruction(1, "Grease idli moulds and pour batter. Steam for 10-12 minutes.", 12, true, "Don't over-steam or idlis become hard."),
                Instruction(2, "Pressure cook toor dal until soft (3 whistles). Mash and set aside.", 15, false, null),
                Instruction(3, "Cook vegetables with tamarind paste, sambar powder, and salt.", 10, false, null),
                Instruction(4, "Add mashed dal and simmer for 5 minutes.", 5, false, null),
                Instruction(5, "Temper with oil, mustard seeds, and curry leaves. Pour over sambar.", 2, false, null),
                Instruction(6, "Serve idlis hot with sambar and coconut chutney.", 1, false, null)
            ),
            nutrition = Nutrition(
                calories = 320,
                proteinGrams = 12,
                carbohydratesGrams = 58,
                fatGrams = 6,
                fiberGrams = 5,
                sugarGrams = 4,
                sodiumMg = 420
            )
        )
    }

    private fun createMalaiKofta(): Recipe {
        return Recipe(
            id = "malai-kofta",
            name = "Malai Kofta",
            description = "Crispy paneer-potato balls in rich cashew cream gravy. A royal Mughlai dish for special occasions.",
            imageUrl = null,
            prepTimeMinutes = 30,
            cookTimeMinutes = 30,
            servings = 4,
            difficulty = Difficulty.HARD,
            cuisineType = CuisineType.NORTH,
            mealTypes = listOf(MealType.DINNER),
            dietaryTags = listOf(DietaryTag.VEGETARIAN),
            ingredients = listOf(
                Ingredient("1", "Paneer, grated", "200", "grams", IngredientCategory.DAIRY),
                Ingredient("2", "Potatoes, boiled and mashed", "2", "medium", IngredientCategory.VEGETABLES),
                Ingredient("3", "Cornflour", "2", "tbsp", IngredientCategory.GRAINS),
                Ingredient("4", "Cashew nuts", "20", "pieces", IngredientCategory.NUTS),
                Ingredient("5", "Fresh cream", "100", "ml", IngredientCategory.DAIRY),
                Ingredient("6", "Onions, chopped", "2", "large", IngredientCategory.VEGETABLES),
                Ingredient("7", "Tomatoes, pureed", "3", "medium", IngredientCategory.VEGETABLES),
                Ingredient("8", "Ginger-garlic paste", "1", "tbsp", IngredientCategory.SPICES),
                Ingredient("9", "Kashmiri red chili powder", "1", "tsp", IngredientCategory.SPICES),
                Ingredient("10", "Garam masala", "0.5", "tsp", IngredientCategory.SPICES),
                Ingredient("11", "Oil for frying", "300", "ml", IngredientCategory.OILS),
                Ingredient("12", "Salt to taste", "1.5", "tsp", IngredientCategory.SPICES)
            ),
            instructions = listOf(
                Instruction(1, "Mix grated paneer, mashed potatoes, cornflour, and salt. Shape into balls.", 10, false, "Add breadcrumbs if mixture is too soft."),
                Instruction(2, "Deep fry koftas until golden brown. Set aside.", 8, false, "Fry on medium heat for even cooking."),
                Instruction(3, "Blend soaked cashews with cream to make a smooth paste.", 2, false, null),
                Instruction(4, "Saute onions until golden. Add ginger-garlic paste and cook 2 minutes.", 8, false, null),
                Instruction(5, "Add tomato puree and spices. Cook until oil separates.", 8, false, null),
                Instruction(6, "Add cashew-cream paste and 1 cup water. Simmer for 5 minutes.", 5, false, null),
                Instruction(7, "Place koftas in gravy just before serving.", 1, false, "Don't add koftas too early or they'll become soggy.")
            ),
            nutrition = Nutrition(
                calories = 420,
                proteinGrams = 14,
                carbohydratesGrams = 28,
                fatGrams = 30,
                fiberGrams = 3,
                sugarGrams = 6,
                sodiumMg = 580
            )
        )
    }
}
