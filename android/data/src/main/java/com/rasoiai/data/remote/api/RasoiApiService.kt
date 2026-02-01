package com.rasoiai.data.remote.api

import com.rasoiai.data.remote.dto.GenerateMealPlanRequest
import com.rasoiai.data.remote.dto.MealPlanResponse
import com.rasoiai.data.remote.dto.RecipeResponse
import com.rasoiai.data.remote.dto.SwapMealRequest
import com.rasoiai.data.remote.dto.UserResponse
import com.rasoiai.data.remote.dto.AuthRequest
import com.rasoiai.data.remote.dto.AuthResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface RasoiApiService {

    // Auth
    @POST("api/v1/auth/firebase")
    suspend fun authenticateWithFirebase(@Body request: AuthRequest): AuthResponse

    // User
    @GET("api/v1/users/me")
    suspend fun getCurrentUser(): UserResponse

    @PUT("api/v1/users/preferences")
    suspend fun updateUserPreferences(@Body preferences: Map<String, Any>): UserResponse

    // Meal Plans
    @POST("api/v1/meal-plans/generate")
    suspend fun generateMealPlan(@Body request: GenerateMealPlanRequest): MealPlanResponse

    @GET("api/v1/meal-plans/current")
    suspend fun getCurrentMealPlan(): MealPlanResponse

    @GET("api/v1/meal-plans/{id}")
    suspend fun getMealPlanById(@Path("id") id: String): MealPlanResponse

    @POST("api/v1/meal-plans/{planId}/items/{itemId}/swap")
    suspend fun swapMealItem(
        @Path("planId") planId: String,
        @Path("itemId") itemId: String,
        @Body request: SwapMealRequest
    ): MealPlanResponse

    @PUT("api/v1/meal-plans/{planId}/items/{itemId}/lock")
    suspend fun lockMealItem(
        @Path("planId") planId: String,
        @Path("itemId") itemId: String
    ): MealPlanResponse

    @DELETE("api/v1/meal-plans/{planId}/items/{itemId}")
    suspend fun removeMealItem(
        @Path("planId") planId: String,
        @Path("itemId") itemId: String
    ): MealPlanResponse

    // Recipes
    @GET("api/v1/recipes/{id}")
    suspend fun getRecipeById(@Path("id") id: String): RecipeResponse

    @GET("api/v1/recipes/{id}/scale")
    suspend fun scaleRecipe(
        @Path("id") id: String,
        @Query("servings") servings: Int
    ): RecipeResponse

    @GET("api/v1/recipes/search")
    suspend fun searchRecipes(
        @Query("q") query: String,
        @Query("cuisine") cuisine: String? = null,
        @Query("dietary") dietary: String? = null,
        @Query("mealType") mealType: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): List<RecipeResponse>

    // Grocery
    @GET("api/v1/grocery")
    suspend fun getGroceryList(@Query("mealPlanId") mealPlanId: String? = null): List<Map<String, Any>>

    @GET("api/v1/grocery/whatsapp")
    suspend fun getGroceryListForWhatsApp(@Query("mealPlanId") mealPlanId: String): String

    // Festivals
    @GET("api/v1/festivals/upcoming")
    suspend fun getUpcomingFestivals(@Query("days") days: Int = 30): List<Map<String, Any>>

    // Chat
    @POST("api/v1/chat/message")
    suspend fun sendChatMessage(@Body message: Map<String, String>): Map<String, Any>

    @GET("api/v1/chat/history")
    suspend fun getChatHistory(): List<Map<String, Any>>

    // Stats
    @GET("api/v1/stats/streak")
    suspend fun getCookingStreak(): Map<String, Any>

    @GET("api/v1/stats/monthly")
    suspend fun getMonthlyStats(@Query("month") month: String): Map<String, Any>
}
