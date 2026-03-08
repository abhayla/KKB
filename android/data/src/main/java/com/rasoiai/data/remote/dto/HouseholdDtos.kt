package com.rasoiai.data.remote.dto

import com.google.gson.annotations.SerializedName

// ==================== Request DTOs ====================

data class HouseholdCreateRequest(
    val name: String
)

data class HouseholdUpdateRequest(
    val name: String? = null,
    @SerializedName("slot_config") val slotConfig: Map<String, Int>? = null,
    @SerializedName("max_members") val maxMembers: Int? = null
)

data class JoinHouseholdRequest(
    @SerializedName("invite_code") val inviteCode: String
)

data class AddMemberByPhoneRequest(
    @SerializedName("phone_number") val phoneNumber: String,
    @SerializedName("is_temporary") val isTemporary: Boolean = false
)

data class UpdateMemberRequest(
    @SerializedName("can_edit_shared_plan") val canEditSharedPlan: Boolean? = null,
    @SerializedName("portion_size") val portionSize: Float? = null,
    @SerializedName("is_temporary") val isTemporary: Boolean? = null
)

data class TransferOwnershipRequest(
    @SerializedName("new_owner_member_id") val newOwnerMemberId: String
)

// ==================== Response DTOs ====================

data class HouseholdResponse(
    val id: String,
    val name: String,
    @SerializedName("invite_code") val inviteCode: String,
    @SerializedName("owner_id") val ownerId: String,
    @SerializedName("slot_config") val slotConfig: Map<String, Int>? = null,
    @SerializedName("max_members") val maxMembers: Int = 8,
    @SerializedName("member_count") val memberCount: Int = 0,
    @SerializedName("is_active") val isActive: Boolean = true,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

data class HouseholdMemberResponse(
    val id: String,
    @SerializedName("household_id") val householdId: String,
    @SerializedName("user_id") val userId: String? = null,
    @SerializedName("family_member_id") val familyMemberId: String? = null,
    val name: String,
    val role: String,
    @SerializedName("can_edit_shared_plan") val canEditSharedPlan: Boolean = false,
    @SerializedName("is_temporary") val isTemporary: Boolean = false,
    @SerializedName("join_date") val joinDate: String,
    @SerializedName("leave_date") val leaveDate: String? = null,
    @SerializedName("portion_size") val portionSize: Float = 1.0f,
    val status: String = "active"
)

data class HouseholdDetailResponse(
    val household: HouseholdResponse,
    val members: List<HouseholdMemberResponse>
)

data class InviteCodeResponse(
    @SerializedName("invite_code") val inviteCode: String,
    @SerializedName("expires_at") val expiresAt: String
)

data class HouseholdStatsResponse(
    @SerializedName("total_meals") val totalMeals: Int,
    @SerializedName("cooked_count") val cookedCount: Int,
    @SerializedName("skipped_count") val skippedCount: Int,
    @SerializedName("ordered_out_count") val orderedOutCount: Int
)

data class HouseholdNotificationResponse(
    val id: String,
    @SerializedName("household_id") val householdId: String,
    val type: String,
    val title: String,
    val message: String,
    @SerializedName("is_read") val isRead: Boolean = false,
    val metadata: Map<String, String>? = null,
    @SerializedName("created_at") val createdAt: String
)
