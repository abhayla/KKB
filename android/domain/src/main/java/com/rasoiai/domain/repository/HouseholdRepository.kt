package com.rasoiai.domain.repository

import com.rasoiai.domain.model.HouseholdDetail
import com.rasoiai.domain.model.HouseholdMember
import com.rasoiai.domain.model.HouseholdNotification
import com.rasoiai.domain.model.HouseholdStats
import com.rasoiai.domain.model.InviteCode
import com.rasoiai.domain.model.MealPlan
import com.rasoiai.domain.model.RecipeRule
import kotlinx.coroutines.flow.Flow

/**
 * Repository for household operations, including membership management,
 * invite codes, and household-scoped data access.
 */
interface HouseholdRepository {

    // region Household CRUD

    /**
     * Get a specific household by ID as a flow.
     */
    fun getHousehold(id: String): Flow<HouseholdDetail?>

    /**
     * Get the current user's household as a flow.
     */
    fun getUserHousehold(): Flow<HouseholdDetail?>

    /**
     * Create a new household with the current user as owner.
     */
    suspend fun createHousehold(name: String): Result<HouseholdDetail>

    /**
     * Update household settings. Only non-null fields are updated.
     */
    suspend fun updateHousehold(
        id: String,
        name: String? = null,
        slotConfig: Map<String, Int>? = null,
        maxMembers: Int? = null,
    ): Result<HouseholdDetail>

    /**
     * Deactivate a household. Only the owner may do this.
     */
    suspend fun deactivateHousehold(id: String): Result<Unit>

    // endregion

    // region Members

    /**
     * Get all members of a household as a flow.
     */
    fun getMembers(householdId: String): Flow<List<HouseholdMember>>

    /**
     * Add a member to a household by phone number.
     * Set [isTemporary] for guest members with a limited duration.
     */
    suspend fun addMember(
        householdId: String,
        phone: String,
        isTemporary: Boolean = false,
    ): Result<HouseholdMember>

    /**
     * Update an existing member's settings. Only non-null fields are updated.
     */
    suspend fun updateMember(
        householdId: String,
        memberId: String,
        canEditSharedPlan: Boolean? = null,
        portionSize: Float? = null,
        isTemporary: Boolean? = null,
    ): Result<HouseholdMember>

    /**
     * Remove a member from a household.
     */
    suspend fun removeMember(householdId: String, memberId: String): Result<Unit>

    // endregion

    // region Invite & Join

    /**
     * Refresh the household invite code, invalidating the previous one.
     */
    suspend fun refreshInviteCode(householdId: String): Result<InviteCode>

    /**
     * Join a household using an invite code.
     */
    suspend fun joinHousehold(inviteCode: String): Result<HouseholdDetail>

    /**
     * Leave the current household. The owner cannot leave without first
     * transferring ownership.
     */
    suspend fun leaveHousehold(householdId: String): Result<Unit>

    /**
     * Transfer household ownership to another member.
     */
    suspend fun transferOwnership(
        householdId: String,
        newOwnerMemberId: String,
    ): Result<Unit>

    // endregion

    // region Household-Scoped Data

    /**
     * Get recipe rules shared across a household as a flow.
     */
    fun getHouseholdRecipeRules(householdId: String): Flow<List<RecipeRule>>

    /**
     * Get the shared meal plan for a household as a flow.
     */
    fun getHouseholdMealPlan(householdId: String): Flow<MealPlan?>

    /**
     * Get household-level notifications as a flow.
     */
    fun getHouseholdNotifications(householdId: String): Flow<List<HouseholdNotification>>

    /**
     * Get aggregated cooking stats for a household.
     * [month] is an optional ISO-8601 month string (e.g. "2026-03") to scope the query.
     */
    suspend fun getHouseholdStats(
        householdId: String,
        month: String? = null,
    ): Result<HouseholdStats>

    /**
     * Mark a household notification as read.
     */
    suspend fun markNotificationRead(notificationId: String): Result<Unit>

    // endregion
}
