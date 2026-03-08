package com.rasoiai.domain.model

import java.time.LocalDateTime

/**
 * Household domain model representing a shared cooking/meal-planning unit.
 */
data class Household(
    val id: String,
    val name: String,
    val inviteCode: String,
    val ownerId: String,
    val slotConfig: Map<String, Int>? = null,
    val maxMembers: Int = 8,
    val memberCount: Int = 0,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

/**
 * A member belonging to a household, linked either to a registered user
 * or an unregistered family member profile.
 */
data class HouseholdMember(
    val id: String,
    val userId: String?,
    val familyMemberId: String?,
    val name: String,
    val role: HouseholdRole,
    val canEditSharedPlan: Boolean = false,
    val isTemporary: Boolean = false,
    val joinDate: LocalDateTime,
    val leaveDate: LocalDateTime? = null,
    val portionSize: Float = 1.0f,
    val status: MemberStatus = MemberStatus.ACTIVE
)

/**
 * Combines a household with its full member list for detail screens.
 */
data class HouseholdDetail(
    val household: Household,
    val members: List<HouseholdMember>
)

/**
 * An invite code with its expiry timestamp.
 */
data class InviteCode(
    val inviteCode: String,
    val expiresAt: LocalDateTime
)

/**
 * A notification scoped to a household, surfaced in the household feed.
 */
data class HouseholdNotification(
    val id: String,
    val householdId: String,
    val type: HouseholdNotificationType,
    val title: String,
    val message: String,
    val isRead: Boolean = false,
    val metadata: Map<String, String>? = null,
    val createdAt: LocalDateTime
)

/**
 * Status update for a single meal plan item within a household context.
 */
data class HouseholdMealStatus(
    val itemId: String,
    val status: MealItemStatus
)

/**
 * Aggregate cooking statistics for a household over a given period.
 */
data class HouseholdStats(
    val totalMeals: Int,
    val cookedCount: Int,
    val skippedCount: Int,
    val orderedOutCount: Int
)

// ---------------------------------------------------------------------------
// Enums
// ---------------------------------------------------------------------------

enum class HouseholdRole(val value: String) {
    OWNER("owner"),
    MEMBER("member"),
    GUEST("guest");

    companion object {
        fun fromValue(value: String): HouseholdRole =
            entries.find { it.value.equals(value, ignoreCase = true) } ?: MEMBER
    }
}

enum class MemberStatus(val value: String) {
    ACTIVE("active"),
    INACTIVE("inactive"),
    PENDING("pending");

    companion object {
        fun fromValue(value: String): MemberStatus =
            entries.find { it.value.equals(value, ignoreCase = true) } ?: ACTIVE
    }
}

enum class HouseholdNotificationType(val value: String) {
    MEMBER_JOINED("member_joined"),
    MEMBER_LEFT("member_left"),
    PLAN_REGENERATED("plan_regenerated"),
    RULE_ADDED("rule_added"),
    RULE_REMOVED("rule_removed"),
    MEAL_STATUS_UPDATED("meal_status_updated"),
    OWNERSHIP_TRANSFERRED("ownership_transferred"),
    GENERAL("general");

    companion object {
        fun fromValue(value: String): HouseholdNotificationType =
            entries.find { it.value.equals(value, ignoreCase = true) } ?: GENERAL
    }
}

enum class MealItemStatus(val value: String) {
    PLANNED("planned"),
    COOKED("cooked"),
    SKIPPED("skipped"),
    ORDERED_OUT("ordered_out");

    companion object {
        fun fromValue(value: String): MealItemStatus =
            entries.find { it.value.equals(value, ignoreCase = true) } ?: PLANNED
    }
}

/**
 * Controls whether a query or operation targets the shared household plan
 * or the user's personal plan.
 */
enum class DataScope {
    FAMILY,
    PERSONAL
}
