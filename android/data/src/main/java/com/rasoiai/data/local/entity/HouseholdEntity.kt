package com.rasoiai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for a household.
 *
 * [slotConfigJson] stores a JSON-serialised Map<String, Int> of meal slot overrides,
 * e.g. {"breakfast":1,"lunch":2}.
 * [role] and [status] are stored as raw String values matching their respective enum names.
 */
@Entity(tableName = "households")
data class HouseholdEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val inviteCode: String,
    val ownerId: String,
    val slotConfigJson: String? = null, // JSON serialized Map<String, Int>
    val maxMembers: Int = 8,
    val memberCount: Int = 0,
    val isActive: Boolean = true,
    val createdAt: String, // ISO datetime
    val updatedAt: String  // ISO datetime
)
