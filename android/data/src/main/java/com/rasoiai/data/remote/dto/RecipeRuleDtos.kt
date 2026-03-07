package com.rasoiai.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Recipe rule response from the API.
 */
data class RecipeRuleDto(
    val id: String,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("target_type")
    val targetType: String,
    val action: String,
    @SerializedName("target_id")
    val targetId: String? = null,
    @SerializedName("target_name")
    val targetName: String,
    @SerializedName("frequency_type")
    val frequencyType: String,
    @SerializedName("frequency_count")
    val frequencyCount: Int? = null,
    @SerializedName("frequency_days")
    val frequencyDays: String? = null,
    val enforcement: String,
    @SerializedName("meal_slot")
    val mealSlot: String? = null,
    @SerializedName("is_active")
    val isActive: Boolean = true,
    @SerializedName("sync_status")
    val syncStatus: String = "SYNCED",
    @SerializedName("force_override")
    val forceOverride: Boolean = false,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
)

/**
 * Request to create a recipe rule.
 */
data class RecipeRuleCreateRequest(
    @SerializedName("target_type")
    val targetType: String,
    val action: String,
    @SerializedName("target_id")
    val targetId: String? = null,
    @SerializedName("target_name")
    val targetName: String,
    @SerializedName("frequency_type")
    val frequencyType: String,
    @SerializedName("frequency_count")
    val frequencyCount: Int? = null,
    @SerializedName("frequency_days")
    val frequencyDays: String? = null,
    val enforcement: String = "REQUIRED",
    @SerializedName("meal_slot")
    val mealSlot: String? = null,
    @SerializedName("is_active")
    val isActive: Boolean = true,
    @SerializedName("force_override")
    val forceOverride: Boolean = false
)

/**
 * Request to update a recipe rule.
 */
data class RecipeRuleUpdateRequest(
    @SerializedName("target_type")
    val targetType: String? = null,
    val action: String? = null,
    @SerializedName("target_id")
    val targetId: String? = null,
    @SerializedName("target_name")
    val targetName: String? = null,
    @SerializedName("frequency_type")
    val frequencyType: String? = null,
    @SerializedName("frequency_count")
    val frequencyCount: Int? = null,
    @SerializedName("frequency_days")
    val frequencyDays: String? = null,
    val enforcement: String? = null,
    @SerializedName("meal_slot")
    val mealSlot: String? = null,
    @SerializedName("is_active")
    val isActive: Boolean? = null
)

/**
 * A single family safety conflict detail from the API.
 */
data class ConflictDetailDto(
    @SerializedName("member_name")
    val memberName: String,
    val condition: String,
    val keyword: String,
    @SerializedName("rule_target")
    val ruleTarget: String
)

/**
 * Structured 409 response for family safety conflicts.
 */
data class ConflictResponseDto(
    val detail: String,
    @SerializedName("conflict_type")
    val conflictType: String,
    @SerializedName("conflict_details")
    val conflictDetails: List<ConflictDetailDto>
)

/**
 * Response containing a list of recipe rules.
 */
data class RecipeRulesListResponse(
    val rules: List<RecipeRuleDto>,
    @SerializedName("total_count")
    val totalCount: Int
)

/**
 * Nutrition goal response from the API.
 */
data class NutritionGoalDto(
    val id: String,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("food_category")
    val foodCategory: String,
    @SerializedName("weekly_target")
    val weeklyTarget: Int,
    @SerializedName("current_progress")
    val currentProgress: Int = 0,
    val enforcement: String,
    @SerializedName("is_active")
    val isActive: Boolean = true,
    @SerializedName("sync_status")
    val syncStatus: String = "SYNCED",
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
)

/**
 * Request to create a nutrition goal.
 */
data class NutritionGoalCreateRequest(
    @SerializedName("food_category")
    val foodCategory: String,
    @SerializedName("weekly_target")
    val weeklyTarget: Int = 3,
    val enforcement: String = "PREFERRED",
    @SerializedName("is_active")
    val isActive: Boolean = true
)

/**
 * Request to update a nutrition goal.
 */
data class NutritionGoalUpdateRequest(
    @SerializedName("food_category")
    val foodCategory: String? = null,
    @SerializedName("weekly_target")
    val weeklyTarget: Int? = null,
    @SerializedName("current_progress")
    val currentProgress: Int? = null,
    val enforcement: String? = null,
    @SerializedName("is_active")
    val isActive: Boolean? = null
)

/**
 * Response containing a list of nutrition goals.
 */
data class NutritionGoalsListResponse(
    val goals: List<NutritionGoalDto>,
    @SerializedName("total_count")
    val totalCount: Int
)

/**
 * Single recipe rule for sync request.
 */
data class RecipeRuleSyncItem(
    val id: String,
    @SerializedName("target_type")
    val targetType: String,
    val action: String,
    @SerializedName("target_id")
    val targetId: String? = null,
    @SerializedName("target_name")
    val targetName: String,
    @SerializedName("frequency_type")
    val frequencyType: String,
    @SerializedName("frequency_count")
    val frequencyCount: Int? = null,
    @SerializedName("frequency_days")
    val frequencyDays: String? = null,
    val enforcement: String = "REQUIRED",
    @SerializedName("meal_slot")
    val mealSlot: String? = null,
    @SerializedName("is_active")
    val isActive: Boolean = true,
    @SerializedName("force_override")
    val forceOverride: Boolean = false,
    @SerializedName("local_updated_at")
    val localUpdatedAt: String
)

/**
 * Single nutrition goal for sync request.
 */
data class NutritionGoalSyncItem(
    val id: String,
    @SerializedName("food_category")
    val foodCategory: String,
    @SerializedName("weekly_target")
    val weeklyTarget: Int = 3,
    @SerializedName("current_progress")
    val currentProgress: Int = 0,
    val enforcement: String = "PREFERRED",
    @SerializedName("is_active")
    val isActive: Boolean = true,
    @SerializedName("local_updated_at")
    val localUpdatedAt: String
)

/**
 * Batch sync request for rules and goals.
 */
data class SyncRequest(
    @SerializedName("recipe_rules")
    val recipeRules: List<RecipeRuleSyncItem> = emptyList(),
    @SerializedName("nutrition_goals")
    val nutritionGoals: List<NutritionGoalSyncItem> = emptyList(),
    @SerializedName("last_sync_time")
    val lastSyncTime: String? = null
)

/**
 * Batch sync response.
 */
data class SyncResponse(
    @SerializedName("server_recipe_rules")
    val serverRecipeRules: List<RecipeRuleDto> = emptyList(),
    @SerializedName("server_nutrition_goals")
    val serverNutritionGoals: List<NutritionGoalDto> = emptyList(),
    @SerializedName("synced_rule_ids")
    val syncedRuleIds: List<String> = emptyList(),
    @SerializedName("synced_goal_ids")
    val syncedGoalIds: List<String> = emptyList(),
    @SerializedName("conflict_rule_ids")
    val conflictRuleIds: List<String> = emptyList(),
    @SerializedName("conflict_goal_ids")
    val conflictGoalIds: List<String> = emptyList(),
    @SerializedName("deleted_rule_ids")
    val deletedRuleIds: List<String> = emptyList(),
    @SerializedName("deleted_goal_ids")
    val deletedGoalIds: List<String> = emptyList(),
    @SerializedName("sync_time")
    val syncTime: String
)
