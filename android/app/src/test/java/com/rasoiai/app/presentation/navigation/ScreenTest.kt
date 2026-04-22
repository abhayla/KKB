package com.rasoiai.app.presentation.navigation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ScreenTest {

    @Nested
    @DisplayName("Chat.createRoute")
    inner class ChatCreateRoute {
        @Test
        @DisplayName("null context produces empty context param")
        fun `null context produces empty context param`() {
            assertEquals("chat?context=", Screen.Chat.createRoute(null))
        }

        @Test
        @DisplayName("simple context is URL-encoded")
        fun `simple context is URL encoded`() {
            // Spaces must be encoded so the query param survives navigation.
            val route = Screen.Chat.createRoute("hello world")
            assertEquals("chat?context=hello+world", route)
        }

        @Test
        @DisplayName("special characters are percent-encoded")
        fun `special characters are percent encoded`() {
            val route = Screen.Chat.createRoute("a&b=c")
            assertTrue(route.startsWith("chat?context="))
            // & and = would break the query string if unencoded.
            assertTrue("%26" in route || "&amp;" in route || "a%26b" in route)
            assertTrue("%3D" in route)
        }

        @Test
        @DisplayName("ARG_CONTEXT constant matches route template variable")
        fun `ARG_CONTEXT constant matches route template variable`() {
            // Guards against drift: if route template changes but ARG_CONTEXT doesn't,
            // NavHost argument lookup silently breaks.
            assertEquals("context", Screen.Chat.ARG_CONTEXT)
            assertTrue("{${Screen.Chat.ARG_CONTEXT}}" in Screen.Chat.route)
        }
    }

    @Nested
    @DisplayName("RecipeDetail.createRoute")
    inner class RecipeDetailCreateRoute {
        @Test
        @DisplayName("minimal call uses default flags")
        fun `minimal call uses default flags`() {
            assertEquals(
                "recipe/r-123?isLocked=false&fromMealPlan=false",
                Screen.RecipeDetail.createRoute("r-123"),
            )
        }

        @Test
        @DisplayName("flags propagate to route")
        fun `flags propagate to route`() {
            val route = Screen.RecipeDetail.createRoute(
                recipeId = "r-123",
                isLocked = true,
                fromMealPlan = true,
            )
            assertEquals("recipe/r-123?isLocked=true&fromMealPlan=true", route)
        }

        @Test
        @DisplayName("ARG_ constants match template variables")
        fun `ARG_ constants match template variables`() {
            assertEquals("recipeId", Screen.RecipeDetail.ARG_RECIPE_ID)
            assertEquals("isLocked", Screen.RecipeDetail.ARG_IS_LOCKED)
            assertEquals("fromMealPlan", Screen.RecipeDetail.ARG_FROM_MEAL_PLAN)
            assertTrue("{${Screen.RecipeDetail.ARG_RECIPE_ID}}" in Screen.RecipeDetail.route)
            assertTrue("{${Screen.RecipeDetail.ARG_IS_LOCKED}}" in Screen.RecipeDetail.route)
            assertTrue("{${Screen.RecipeDetail.ARG_FROM_MEAL_PLAN}}" in Screen.RecipeDetail.route)
        }
    }

    @Nested
    @DisplayName("CookingMode.createRoute")
    inner class CookingModeCreateRoute {
        @Test
        @DisplayName("embeds recipeId verbatim")
        fun `embeds recipeId verbatim`() {
            assertEquals("cooking/r-42", Screen.CookingMode.createRoute("r-42"))
        }

        @Test
        @DisplayName("ARG_RECIPE_ID matches template")
        fun `ARG_RECIPE_ID matches template`() {
            assertEquals("recipeId", Screen.CookingMode.ARG_RECIPE_ID)
            assertTrue("{${Screen.CookingMode.ARG_RECIPE_ID}}" in Screen.CookingMode.route)
        }
    }

    @Nested
    @DisplayName("HouseholdMemberDetail.createRoute")
    inner class HouseholdMemberDetailCreateRoute {
        @Test
        @DisplayName("embeds memberId into nested settings path")
        fun `embeds memberId into nested settings path`() {
            assertEquals(
                "settings/household/members/m-1",
                Screen.HouseholdMemberDetail.createRoute("m-1"),
            )
        }

        @Test
        @DisplayName("ARG_MEMBER_ID matches template")
        fun `ARG_MEMBER_ID matches template`() {
            assertEquals("memberId", Screen.HouseholdMemberDetail.ARG_MEMBER_ID)
            assertTrue("{${Screen.HouseholdMemberDetail.ARG_MEMBER_ID}}" in Screen.HouseholdMemberDetail.route)
        }
    }

    @Nested
    @DisplayName("Bottom nav screens")
    inner class BottomNavScreens {
        @Test
        @DisplayName("contains five tabs with expected route strings in order")
        fun `contains five tabs with expected route strings in order`() {
            // Order matters — it determines tab positions in the UI. Compare by
            // route string rather than object identity because `data object`
            // references inside the companion object's list literal can be
            // captured pre-init in some JVM class-loading orders.
            val routes = Screen.bottomNavScreens.map { it?.route }
            assertEquals(
                listOf("home", "grocery", "chat?context={context}", "favorites", "stats"),
                routes,
            )
        }
    }

    @Nested
    @DisplayName("Static routes")
    inner class StaticRoutes {
        @Test
        @DisplayName("household screens use consistent nesting")
        fun `household screens use consistent nesting`() {
            assertEquals("settings/household", Screen.HouseholdSettings.route)
            assertEquals("settings/household/members", Screen.HouseholdMembers.route)
            assertEquals("settings/join-household", Screen.JoinHousehold.route)
        }

        @Test
        @DisplayName("settings sub-screens live under settings/")
        fun `settings sub-screens live under settings`() {
            assertTrue(Screen.DietaryRestrictions.route.startsWith("settings/"))
            assertTrue(Screen.CuisinePreferences.route.startsWith("settings/"))
            assertTrue(Screen.NotificationSettings.route.startsWith("settings/"))
            assertTrue(Screen.EditProfile.route.startsWith("settings/"))
        }
    }
}
