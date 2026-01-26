package com.rasoiai.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.rasoiai.app.presentation.auth.AuthScreen
import com.rasoiai.app.presentation.cookingmode.CookingModeScreen
import com.rasoiai.app.presentation.home.HomeScreen
import com.rasoiai.app.presentation.onboarding.OnboardingScreen
import com.rasoiai.app.presentation.recipedetail.RecipeDetailScreen
import com.rasoiai.app.presentation.splash.SplashScreen

@Composable
fun RasoiNavHost(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Splash
        composable(route = Screen.Splash.route) {
            SplashScreen(
                onNavigateToAuth = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // Auth
        composable(route = Screen.Auth.route) {
            AuthScreen(
                onNavigateToOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        // Onboarding
        composable(route = Screen.Onboarding.route) {
            OnboardingScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        // Home
        composable(route = Screen.Home.route) {
            HomeScreen(
                onNavigateToRecipeDetail = { recipeId ->
                    navController.navigate(Screen.RecipeDetail.createRoute(recipeId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToGrocery = {
                    navController.navigate(Screen.Grocery.route)
                },
                onNavigateToChat = {
                    navController.navigate(Screen.Chat.route)
                },
                onNavigateToFavorites = {
                    navController.navigate(Screen.Favorites.route)
                },
                onNavigateToStats = {
                    navController.navigate(Screen.Stats.route)
                }
            )
        }

        // Recipe Detail with deep link
        composable(
            route = Screen.RecipeDetail.route,
            arguments = listOf(
                navArgument(Screen.RecipeDetail.ARG_RECIPE_ID) {
                    type = NavType.StringType
                }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "rasoiai://recipe/{recipeId}" },
                navDeepLink { uriPattern = "https://rasoiai.app/recipe/{recipeId}" }
            )
        ) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getString(Screen.RecipeDetail.ARG_RECIPE_ID) ?: return@composable
            RecipeDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCookingMode = { id ->
                    navController.navigate(Screen.CookingMode.createRoute(id))
                },
                onNavigateToChat = { context ->
                    navController.navigate(Screen.Chat.route)
                    // TODO: Pass context to chat screen
                }
            )
        }

        // Cooking Mode
        composable(
            route = Screen.CookingMode.route,
            arguments = listOf(
                navArgument(Screen.CookingMode.ARG_RECIPE_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getString(Screen.CookingMode.ARG_RECIPE_ID) ?: return@composable
            CookingModeScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        // Grocery
        composable(route = Screen.Grocery.route) {
            // TODO: GroceryScreen
            PlaceholderScreen(title = "Grocery List")
        }

        // Favorites
        composable(route = Screen.Favorites.route) {
            // TODO: FavoritesScreen
            PlaceholderScreen(title = "Favorites")
        }

        // Chat
        composable(route = Screen.Chat.route) {
            // TODO: ChatScreen
            PlaceholderScreen(title = "AI Chat")
        }

        // Pantry
        composable(route = Screen.Pantry.route) {
            // TODO: PantryScreen
            PlaceholderScreen(title = "Pantry")
        }

        // Stats
        composable(route = Screen.Stats.route) {
            // TODO: StatsScreen
            PlaceholderScreen(title = "Stats")
        }

        // Settings
        composable(route = Screen.Settings.route) {
            // TODO: SettingsScreen
            PlaceholderScreen(title = "Settings")
        }
    }
}
