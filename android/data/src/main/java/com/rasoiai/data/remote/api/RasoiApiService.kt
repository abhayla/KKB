package com.rasoiai.data.remote.api

import com.rasoiai.data.remote.dto.AddMemberByPhoneRequest
import com.rasoiai.data.remote.dto.AiRecipeCatalogResponse
import com.rasoiai.data.remote.dto.AuthRequest
import com.rasoiai.data.remote.dto.AuthResponse
import com.rasoiai.data.remote.dto.FamilyMemberCreateRequest
import com.rasoiai.data.remote.dto.FamilyMemberDto
import com.rasoiai.data.remote.dto.FamilyMemberUpdateRequest
import com.rasoiai.data.remote.dto.FamilyMembersListResponse
import com.rasoiai.data.remote.dto.ChatImageRequest
import com.rasoiai.data.remote.dto.ChatImageResponse
import com.rasoiai.data.remote.dto.FcmTokenRequest
import com.rasoiai.data.remote.dto.HouseholdCreateRequest
import com.rasoiai.data.remote.dto.HouseholdDetailResponse
import com.rasoiai.data.remote.dto.HouseholdMemberResponse
import com.rasoiai.data.remote.dto.HouseholdNotificationResponse
import com.rasoiai.data.remote.dto.HouseholdStatsResponse
import com.rasoiai.data.remote.dto.HouseholdUpdateRequest
import com.rasoiai.data.remote.dto.InviteCodeResponse
import com.rasoiai.data.remote.dto.JoinHouseholdRequest
import com.rasoiai.data.remote.dto.TransferOwnershipRequest
import com.rasoiai.data.remote.dto.UpdateMemberRequest
import com.rasoiai.data.remote.dto.GenerateMealPlanRequest
import com.rasoiai.data.remote.dto.MealPlanResponse
import com.rasoiai.data.remote.dto.NotificationsResponse
import com.rasoiai.data.remote.dto.NutritionGoalCreateRequest
import com.rasoiai.data.remote.dto.NutritionGoalDto
import com.rasoiai.data.remote.dto.NutritionGoalsListResponse
import com.rasoiai.data.remote.dto.NutritionGoalUpdateRequest
import com.rasoiai.data.remote.dto.RecipeRatingRequest
import com.rasoiai.data.remote.dto.RecipeRatingResponse
import com.rasoiai.data.remote.dto.RecipeResponse
import com.rasoiai.data.remote.dto.RecipeRuleCreateRequest
import com.rasoiai.data.remote.dto.RecipeRuleDto
import com.rasoiai.data.remote.dto.RecipeRulesListResponse
import com.rasoiai.data.remote.dto.RecipeRuleUpdateRequest
import com.rasoiai.data.remote.dto.RefreshTokenRequest
import com.rasoiai.data.remote.dto.RefreshTokenResponse
import com.rasoiai.data.remote.dto.SuccessResponse
import com.rasoiai.data.remote.dto.SwapMealRequest
import com.rasoiai.data.remote.dto.SyncRequest
import com.rasoiai.data.remote.dto.SyncResponse
import com.rasoiai.data.remote.dto.UserResponse
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

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): RefreshTokenResponse

    // User
    @GET("api/v1/users/me")
    suspend fun getCurrentUser(): UserResponse

    @PUT("api/v1/users/preferences")
    suspend fun updateUserPreferences(@Body preferences: Map<String, @JvmSuppressWildcards Any>): UserResponse

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
    @GET("api/v1/recipes/ai-catalog/search")
    suspend fun searchAiRecipeCatalog(
        @Query("q") query: String,
        @Query("favorites") favorites: String? = null,
        @Query("limit") limit: Int = 10
    ): List<AiRecipeCatalogResponse>

    @GET("api/v1/recipes/{id}")
    suspend fun getRecipeById(@Path("id") id: String): RecipeResponse

    @GET("api/v1/recipes/{id}/scale")
    suspend fun scaleRecipe(
        @Path("id") id: String,
        @Query("servings") servings: Int
    ): RecipeResponse

    @POST("api/v1/recipes/{id}/rate")
    suspend fun rateRecipe(
        @Path("id") recipeId: String,
        @Body request: RecipeRatingRequest
    ): RecipeRatingResponse

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

    @POST("api/v1/chat/image")
    suspend fun sendImageChatMessage(@Body request: ChatImageRequest): ChatImageResponse

    @GET("api/v1/chat/history")
    suspend fun getChatHistory(): List<Map<String, Any>>

    // Stats
    @GET("api/v1/stats/streak")
    suspend fun getCookingStreak(): Map<String, Any>

    @GET("api/v1/stats/monthly")
    suspend fun getMonthlyStats(@Query("month") month: String): Map<String, Any>

    // Notifications
    @GET("api/v1/notifications")
    suspend fun getNotifications(): NotificationsResponse

    @PUT("api/v1/notifications/{id}/read")
    suspend fun markNotificationAsRead(@Path("id") id: String): SuccessResponse

    @PUT("api/v1/notifications/read-all")
    suspend fun markAllNotificationsAsRead(): SuccessResponse

    @DELETE("api/v1/notifications/{id}")
    suspend fun deleteNotification(@Path("id") id: String): SuccessResponse

    @POST("api/v1/notifications/fcm-token")
    suspend fun registerFcmToken(@Body request: FcmTokenRequest): SuccessResponse

    @DELETE("api/v1/notifications/fcm-token")
    suspend fun unregisterFcmToken(@Query("fcm_token") fcmToken: String): SuccessResponse

    // Family Members
    @GET("api/v1/family-members")
    suspend fun getFamilyMembers(): FamilyMembersListResponse

    @POST("api/v1/family-members")
    suspend fun createFamilyMember(@Body member: FamilyMemberCreateRequest): FamilyMemberDto

    @PUT("api/v1/family-members/{id}")
    suspend fun updateFamilyMemberApi(
        @Path("id") id: String,
        @Body member: FamilyMemberUpdateRequest
    ): FamilyMemberDto

    @DELETE("api/v1/family-members/{id}")
    suspend fun deleteFamilyMember(@Path("id") id: String)

    // Recipe Rules
    @GET("api/v1/recipe-rules")
    suspend fun getRecipeRules(): RecipeRulesListResponse

    @POST("api/v1/recipe-rules")
    suspend fun createRecipeRule(@Body rule: RecipeRuleCreateRequest): retrofit2.Response<RecipeRuleDto>

    @GET("api/v1/recipe-rules/{id}")
    suspend fun getRecipeRuleById(@Path("id") id: String): RecipeRuleDto

    @PUT("api/v1/recipe-rules/{id}")
    suspend fun updateRecipeRule(
        @Path("id") id: String,
        @Body rule: RecipeRuleUpdateRequest
    ): RecipeRuleDto

    @DELETE("api/v1/recipe-rules/{id}")
    suspend fun deleteRecipeRule(@Path("id") id: String)

    @POST("api/v1/recipe-rules/sync")
    suspend fun syncRecipeRules(@Body request: SyncRequest): SyncResponse

    // Nutrition Goals
    @GET("api/v1/nutrition-goals")
    suspend fun getNutritionGoals(): NutritionGoalsListResponse

    @POST("api/v1/nutrition-goals")
    suspend fun createNutritionGoal(@Body goal: NutritionGoalCreateRequest): NutritionGoalDto

    @GET("api/v1/nutrition-goals/{id}")
    suspend fun getNutritionGoalById(@Path("id") id: String): NutritionGoalDto

    @PUT("api/v1/nutrition-goals/{id}")
    suspend fun updateNutritionGoal(
        @Path("id") id: String,
        @Body goal: NutritionGoalUpdateRequest
    ): NutritionGoalDto

    @DELETE("api/v1/nutrition-goals/{id}")
    suspend fun deleteNutritionGoal(@Path("id") id: String)

    // Households
    @POST("api/v1/households")
    suspend fun createHousehold(@Body request: HouseholdCreateRequest): HouseholdDetailResponse

    @GET("api/v1/households/{id}")
    suspend fun getHousehold(@Path("id") id: String): HouseholdDetailResponse

    @PUT("api/v1/households/{id}")
    suspend fun updateHousehold(
        @Path("id") id: String,
        @Body request: HouseholdUpdateRequest
    ): HouseholdDetailResponse

    @DELETE("api/v1/households/{id}")
    suspend fun deactivateHousehold(@Path("id") id: String): SuccessResponse

    // Household Members
    @GET("api/v1/households/{id}/members")
    suspend fun getHouseholdMembers(@Path("id") id: String): List<HouseholdMemberResponse>

    @POST("api/v1/households/{id}/members")
    suspend fun addHouseholdMember(
        @Path("id") id: String,
        @Body request: AddMemberByPhoneRequest
    ): HouseholdMemberResponse

    @PUT("api/v1/households/{id}/members/{memberId}")
    suspend fun updateHouseholdMember(
        @Path("id") id: String,
        @Path("memberId") memberId: String,
        @Body request: UpdateMemberRequest
    ): HouseholdMemberResponse

    @DELETE("api/v1/households/{id}/members/{memberId}")
    suspend fun removeHouseholdMember(
        @Path("id") id: String,
        @Path("memberId") memberId: String
    ): SuccessResponse

    // Household Invite & Membership
    @POST("api/v1/households/{id}/invite-code")
    suspend fun refreshInviteCode(@Path("id") id: String): InviteCodeResponse

    @POST("api/v1/households/join")
    suspend fun joinHousehold(@Body request: JoinHouseholdRequest): HouseholdDetailResponse

    @POST("api/v1/households/{id}/leave")
    suspend fun leaveHousehold(@Path("id") id: String): SuccessResponse

    @POST("api/v1/households/{id}/transfer-ownership")
    suspend fun transferOwnership(
        @Path("id") id: String,
        @Body request: TransferOwnershipRequest
    ): SuccessResponse

    // Household Scoped Data
    @GET("api/v1/households/{id}/recipe-rules")
    suspend fun getHouseholdRecipeRules(@Path("id") id: String): RecipeRulesListResponse

    @POST("api/v1/households/{id}/recipe-rules")
    suspend fun createHouseholdRecipeRule(
        @Path("id") id: String,
        @Body rule: RecipeRuleCreateRequest
    ): retrofit2.Response<RecipeRuleDto>

    @DELETE("api/v1/households/{id}/recipe-rules/{ruleId}")
    suspend fun deleteHouseholdRecipeRule(
        @Path("id") id: String,
        @Path("ruleId") ruleId: String
    ): SuccessResponse

    @GET("api/v1/households/{id}/meal-plans/current")
    suspend fun getHouseholdMealPlan(@Path("id") id: String): MealPlanResponse

    @GET("api/v1/households/{id}/notifications")
    suspend fun getHouseholdNotifications(@Path("id") id: String): List<HouseholdNotificationResponse>

    @PUT("api/v1/households/{id}/notifications/{notificationId}/read")
    suspend fun markHouseholdNotificationRead(
        @Path("id") id: String,
        @Path("notificationId") notificationId: String
    ): SuccessResponse

    @GET("api/v1/households/{id}/stats/monthly")
    suspend fun getHouseholdStats(
        @Path("id") id: String,
        @Query("month") month: String? = null
    ): HouseholdStatsResponse
}
