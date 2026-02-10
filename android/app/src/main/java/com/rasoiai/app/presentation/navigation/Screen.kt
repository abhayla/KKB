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
    data object Notifications : Screen("notifications")

    // Detail screens
    data object RecipeDetail : Screen("recipe/{recipeId}?isLocked={isLocked}&fromMealPlan={fromMealPlan}") {
        /**
         * Creates route for recipe detail screen.
         * @param recipeId The recipe ID
         * @param isLocked Whether the recipe is locked in the meal plan (only relevant when fromMealPlan=true)
         * @param fromMealPlan Whether navigating from meal plan context (determines if lock icon is shown)
         */
        fun createRoute(recipeId: String, isLocked: Boolean = false, fromMealPlan: Boolean = false) =
            "recipe/$recipeId?isLocked=$isLocked&fromMealPlan=$fromMealPlan"
        const val ARG_RECIPE_ID = "recipeId"
        const val ARG_IS_LOCKED = "isLocked"
        const val ARG_FROM_MEAL_PLAN = "fromMealPlan"
    }

    data object CookingMode : Screen("cooking/{recipeId}") {
        fun createRoute(recipeId: String) = "cooking/$recipeId"
        const val ARG_RECIPE_ID = "recipeId"
    }

    // Feature screens
    data object Pantry : Screen("pantry")
    data object Stats : Screen("stats")
    data object RecipeRules : Screen("recipe-rules")

    // Settings sub-screens
    data object DietaryRestrictions : Screen("settings/dietary-restrictions")
    data object DislikedIngredients : Screen("settings/disliked-ingredients")
    data object CuisinePreferences : Screen("settings/cuisine-preferences")
    data object SpiceLevelSettings : Screen("settings/spice-level")
    data object CookingTimeSettings : Screen("settings/cooking-time")
    data object FamilyMembersSettings : Screen("settings/family-members")
    data object NotificationSettings : Screen("settings/notifications")
    data object UnitsSettings : Screen("settings/units")
    data object EditProfile : Screen("settings/edit-profile")
    data object FriendsLeaderboard : Screen("settings/friends-leaderboard")
    data object ConnectedAccounts : Screen("settings/connected-accounts")

    companion object {
        val bottomNavScreens = listOf(Home, Grocery, Chat, Favorites, Stats)
    }
}
