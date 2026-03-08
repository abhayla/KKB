package com.rasoiai.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for a household member.
 *
 * [role] stores the raw HouseholdRole enum name (e.g. "OWNER", "MEMBER", "GUEST").
 * [status] stores the raw MemberStatus enum name (e.g. "active", "inactive").
 * Either [userId] or [familyMemberId] will be non-null depending on whether the member
 * is a registered user or a locally-managed family profile entry.
 */
@Entity(
    tableName = "household_members",
    foreignKeys = [
        ForeignKey(
            entity = HouseholdEntity::class,
            parentColumns = ["id"],
            childColumns = ["householdId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("householdId")]
)
data class HouseholdMemberEntity(
    @PrimaryKey
    val id: String,
    val householdId: String,
    val userId: String?,
    val familyMemberId: String?,
    val name: String,
    val role: String, // HouseholdRole value
    val canEditSharedPlan: Boolean = false,
    val isTemporary: Boolean = false,
    val joinDate: String, // ISO datetime
    val leaveDate: String? = null, // ISO datetime
    val portionSize: Float = 1.0f,
    val status: String = "active" // MemberStatus value
)
