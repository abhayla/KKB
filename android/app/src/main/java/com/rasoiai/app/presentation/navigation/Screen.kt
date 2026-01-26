package com.rasoiai.app.presentation.navigation

sealed class Screen(val route: String) {
    // Auth flow
    data object Splash : Screen("splash")
    data object Auth : Screen("auth")
    data object Onboarding : Screen("onboarding")

    // Main screens
    data object Home : Screen("home")
    data object Grocery : Screen("grocery")
    data object Favorites : Screen("favorites")
    data object Chat : Screen("chat")
    data object Settings : Screen("settings")

    // Detail screens
    data object RecipeDetail : Screen("recipe/{recipeId}?isLocked={isLocked}") {
        fun createRoute(recipeId: String, isLocked: Boolean = false) = "recipe/$recipeId?isLocked=$isLocked"
        const val ARG_RECIPE_ID = "recipeId"
        const val ARG_IS_LOCKED = "isLocked"
    }

    data object CookingMode : Screen("cooking/{recipeId}") {
        fun createRoute(recipeId: String) = "cooking/$recipeId"
        const val ARG_RECIPE_ID = "recipeId"
    }

    // Feature screens
    data object Pantry : Screen("pantry")
    data object Stats : Screen("stats")
    data object RecipeRules : Screen("recipe-rules")

    companion object {
        val bottomNavScreens = listOf(Home, Grocery, Chat, Favorites, Stats)
    }
}
