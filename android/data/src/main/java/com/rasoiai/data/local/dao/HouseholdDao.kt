package com.rasoiai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.rasoiai.data.local.entity.HouseholdEntity
import com.rasoiai.data.local.entity.HouseholdMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HouseholdDao {

    // ==================== Households ====================

    @Query("SELECT * FROM households WHERE id = :id")
    fun getHousehold(id: String): Flow<HouseholdEntity?>

    @Query("SELECT * FROM households WHERE isActive = 1 LIMIT 1")
    fun getActiveHousehold(): Flow<HouseholdEntity?>

    @Query("SELECT * FROM households WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveHouseholdSync(): HouseholdEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHousehold(household: HouseholdEntity)

    @Update
    suspend fun updateHousehold(household: HouseholdEntity)

    @Query("UPDATE households SET isActive = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun deactivateHousehold(id: String, updatedAt: String)

    @Query("DELETE FROM households WHERE id = :id")
    suspend fun deleteHousehold(id: String)

    @Query("DELETE FROM households")
    suspend fun deleteAllHouseholds()

    @Query("UPDATE households SET inviteCode = :inviteCode, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateInviteCode(id: String, inviteCode: String, updatedAt: String)

    // ==================== Household Members ====================

    @Query("SELECT * FROM household_members WHERE householdId = :householdId ORDER BY joinDate ASC")
    fun getMembers(householdId: String): Flow<List<HouseholdMemberEntity>>

    @Query("SELECT * FROM household_members WHERE householdId = :householdId ORDER BY joinDate ASC")
    suspend fun getMembersSync(householdId: String): List<HouseholdMemberEntity>

    @Query("SELECT * FROM household_members WHERE id = :memberId")
    fun getMember(memberId: String): Flow<HouseholdMemberEntity?>

    @Query("SELECT * FROM household_members WHERE id = :memberId")
    suspend fun getMemberSync(memberId: String): HouseholdMemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: HouseholdMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<HouseholdMemberEntity>)

    @Update
    suspend fun updateMember(member: HouseholdMemberEntity)

    @Query("DELETE FROM household_members WHERE id = :memberId")
    suspend fun deleteMember(memberId: String)

    @Query("DELETE FROM household_members WHERE householdId = :householdId")
    suspend fun deleteAllMembers(householdId: String)

    @Query("SELECT COUNT(*) FROM household_members WHERE householdId = :householdId")
    suspend fun getMemberCount(householdId: String): Int

    // ==================== Transactions ====================

    @Transaction
    suspend fun replaceHouseholdWithMembers(
        household: HouseholdEntity,
        members: List<HouseholdMemberEntity>
    ) {
        deleteAllMembers(household.id)
        insertHousehold(household)
        insertMembers(members)
    }
}
