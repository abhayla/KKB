package com.rasoiai.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.rasoiai.app.presentation.auth.AuthScreen
import com.rasoiai.app.presentation.chat.ChatScreen
import com.rasoiai.app.presentation.cookingmode.CookingModeScreen
import com.rasoiai.app.presentation.favorites.FavoritesScreen
import com.rasoiai.app.presentation.grocery.GroceryScreen
import com.rasoiai.app.presentation.home.HomeScreen
import com.rasoiai.app.presentation.notifications.NotificationsScreen
import com.rasoiai.app.presentation.onboarding.OnboardingScreen
import com.rasoiai.app.presentation.pantry.PantryScreen
import com.rasoiai.app.presentation.recipedetail.RecipeDetailScreen
import com.rasoiai.app.presentation.reciperules.RecipeRulesScreen
import com.rasoiai.app.presentation.settings.SettingsScreen
import com.rasoiai.app.presentation.settings.screens.CookingTimeScreen
import com.rasoiai.app.presentation.settings.screens.CuisinePreferencesScreen
import com.rasoiai.app.presentation.settings.screens.DietaryRestrictionsScreen
import com.rasoiai.app.presentation.settings.screens.DislikedIngredientsScreen
import com.rasoiai.app.presentation.settings.screens.FamilyMembersScreen
import com.rasoiai.app.presentation.settings.screens.NotificationSettingsScreen
import com.rasoiai.app.presentation.settings.screens.SpiceLevelScreen
import com.rasoiai.app.presentation.settings.screens.ConnectedAccountsScreen
import com.rasoiai.app.presentation.settings.screens.EditProfileScreen
import com.rasoiai.app.presentation.settings.screens.FriendsLeaderboardScreen
import com.rasoiai.app.presentation.settings.screens.UnitsScreen
import com.rasoiai.app.presentation.splash.SplashScreen
import com.rasoiai.app.presentation.stats.StatsScreen

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
                onNavigateToRecipeDetail = { recipeId, isLocked, fromMealPlan ->
                    navController.navigate(Screen.RecipeDetail.createRoute(recipeId, isLocked, fromMealPlan))
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
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications.route)
                }
            )
        }

        // Notifications
        composable(route = Screen.Notifications.route) {
            NotificationsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Recipe Detail with deep link
        composable(
            route = Screen.RecipeDetail.route,
            arguments = listOf(
                navArgument(Screen.RecipeDetail.ARG_RECIPE_ID) {
                    type = NavType.StringType
                },
                navArgument(Screen.RecipeDetail.ARG_IS_LOCKED) {
                    type = NavType.BoolType
                    defaultValue = false
                },
                navArgument(Screen.RecipeDetail.ARG_FROM_MEAL_PLAN) {
                    type = NavType.BoolType
                    defaultValue = false
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
            GroceryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
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

        // Favorites
        composable(route = Screen.Favorites.route) {
            FavoritesScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToGrocery = {
                    navController.navigate(Screen.Grocery.route)
                },
                onNavigateToChat = {
                    navController.navigate(Screen.Chat.route)
                },
                onNavigateToStats = {
                    navController.navigate(Screen.Stats.route)
                },
                onNavigateToRecipeDetail = { recipeId ->
                    navController.navigate(Screen.RecipeDetail.createRoute(recipeId))
                }
            )
        }

        // Chat
        composable(route = Screen.Chat.route) {
            ChatScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToGrocery = {
                    navController.navigate(Screen.Grocery.route)
                },
                onNavigateToFavorites = {
                    navController.navigate(Screen.Favorites.route)
                },
                onNavigateToStats = {
                    navController.navigate(Screen.Stats.route)
                },
                onNavigateToRecipeDetail = { recipeId ->
                    navController.navigate(Screen.RecipeDetail.createRoute(recipeId))
                }
            )
        }

        // Pantry
        composable(route = Screen.Pantry.route) {
            PantryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
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

        // Stats
        composable(route = Screen.Stats.route) {
            StatsScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToGrocery = {
                    navController.navigate(Screen.Grocery.route)
                },
                onNavigateToChat = {
                    navController.navigate(Screen.Chat.route)
                },
                onNavigateToFavorites = {
                    navController.navigate(Screen.Favorites.route)
                }
            )
        }

        // Settings
        composable(route = Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAuth = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToRecipeRules = {
                    navController.navigate(Screen.RecipeRules.route)
                },
                onNavigateToDietaryRestrictions = {
                    navController.navigate(Screen.DietaryRestrictions.route)
                },
                onNavigateToDislikedIngredients = {
                    navController.navigate(Screen.DislikedIngredients.route)
                },
                onNavigateToCuisinePreferences = {
                    navController.navigate(Screen.CuisinePreferences.route)
                },
                onNavigateToSpiceLevel = {
                    navController.navigate(Screen.SpiceLevelSettings.route)
                },
                onNavigateToCookingTime = {
                    navController.navigate(Screen.CookingTimeSettings.route)
                },
                onNavigateToFamilyMembers = {
                    navController.navigate(Screen.FamilyMembersSettings.route)
                },
                onNavigateToNotificationSettings = {
                    navController.navigate(Screen.NotificationSettings.route)
                },
                onNavigateToUnits = {
                    navController.navigate(Screen.UnitsSettings.route)
                },
                onNavigateToEditProfile = {
                    navController.navigate(Screen.EditProfile.route)
                },
                onNavigateToFriendsLeaderboard = {
                    navController.navigate(Screen.FriendsLeaderboard.route)
                },
                onNavigateToConnectedAccounts = {
                    navController.navigate(Screen.ConnectedAccounts.route)
                }
            )
        }

        // Recipe Rules
        composable(route = Screen.RecipeRules.route) {
            RecipeRulesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Settings Sub-Screens
        composable(route = Screen.DietaryRestrictions.route) {
            DietaryRestrictionsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.DislikedIngredients.route) {
            DislikedIngredientsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.CuisinePreferences.route) {
            CuisinePreferencesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.SpiceLevelSettings.route) {
            SpiceLevelScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.CookingTimeSettings.route) {
            CookingTimeScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.FamilyMembersSettings.route) {
            FamilyMembersScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.NotificationSettings.route) {
            NotificationSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.UnitsSettings.route) {
            UnitsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.EditProfile.route) {
            EditProfileScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.FriendsLeaderboard.route) {
            FriendsLeaderboardScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.ConnectedAccounts.route) {
            ConnectedAccountsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
