package com.rasoiai.data.repository

import com.rasoiai.data.local.dao.HouseholdDao
import com.rasoiai.data.local.mapper.toDomain
import com.rasoiai.data.local.mapper.toEntity
import com.rasoiai.data.local.mapper.toItemEntities
import com.rasoiai.data.local.mapper.toFestivalEntities
import com.rasoiai.data.remote.api.RasoiApiService
import com.rasoiai.data.remote.dto.AddMemberByPhoneRequest
import com.rasoiai.data.remote.dto.HouseholdCreateRequest
import com.rasoiai.data.remote.dto.HouseholdUpdateRequest
import com.rasoiai.data.remote.dto.JoinHouseholdRequest
import com.rasoiai.data.remote.dto.TransferOwnershipRequest
import com.rasoiai.data.remote.dto.UpdateMemberRequest
import com.rasoiai.domain.model.Household
import com.rasoiai.domain.model.HouseholdDetail
import com.rasoiai.domain.model.HouseholdMember
import com.rasoiai.domain.model.HouseholdNotification
import com.rasoiai.domain.model.HouseholdStats
import com.rasoiai.domain.model.InviteCode
import com.rasoiai.domain.model.MealPlan
import com.rasoiai.domain.model.RecipeRule
import com.rasoiai.domain.repository.HouseholdRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HouseholdRepositoryImpl @Inject constructor(
    private val householdDao: HouseholdDao,
    private val apiService: RasoiApiService
) : HouseholdRepository {

    // ==================== CRUD ====================

    override fun getHousehold(id: String): Flow<HouseholdDetail?> {
        return householdDao.getHousehold(id).map { entity ->
            if (entity != null) {
                val members = householdDao.getMembersSync(entity.id)
                HouseholdDetail(
                    household = entity.toDomain(),
                    members = members.map { it.toDomain() }
                )
            } else {
                null
            }
        }
    }

    override fun getUserHousehold(): Flow<HouseholdDetail?> {
        return householdDao.getActiveHousehold().map { entity ->
            if (entity != null) {
                val members = householdDao.getMembersSync(entity.id)
                HouseholdDetail(
                    household = entity.toDomain(),
                    members = members.map { it.toDomain() }
                )
            } else {
                // Try fetching from API
                null
            }
        }
    }

    override suspend fun createHousehold(name: String): Result<HouseholdDetail> {
        return try {
            val response = apiService.createHousehold(HouseholdCreateRequest(name))
            val householdEntity = response.household.toEntity()
            val memberEntities = response.members.map { it.toEntity() }
            householdDao.replaceHouseholdWithMembers(householdEntity, memberEntities)
            Timber.i("Created household: ${response.household.id}")
            Result.success(response.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to create household")
            Result.failure(e)
        }
    }

    override suspend fun updateHousehold(
        id: String,
        name: String?,
        slotConfig: Map<String, Int>?,
        maxMembers: Int?
    ): Result<HouseholdDetail> {
        return try {
            val response = apiService.updateHousehold(
                id,
                HouseholdUpdateRequest(name, slotConfig, maxMembers)
            )
            val householdEntity = response.household.toEntity()
            val memberEntities = response.members.map { it.toEntity() }
            householdDao.replaceHouseholdWithMembers(householdEntity, memberEntities)
            Timber.i("Updated household: $id")
            Result.success(response.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to update household")
            Result.failure(e)
        }
    }

    override suspend fun deactivateHousehold(id: String): Result<Unit> {
        return try {
            apiService.deactivateHousehold(id)
            householdDao.deactivateHousehold(id, LocalDateTime.now().toString())
            Timber.i("Deactivated household: $id")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to deactivate household")
            Result.failure(e)
        }
    }

    // ==================== Members ====================

    override fun getMembers(householdId: String): Flow<List<HouseholdMember>> {
        return householdDao.getMembers(householdId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addMember(
        householdId: String,
        phone: String,
        isTemporary: Boolean
    ): Result<HouseholdMember> {
        return try {
            val response = apiService.addHouseholdMember(
                householdId,
                AddMemberByPhoneRequest(phone, isTemporary)
            )
            val entity = response.toEntity()
            householdDao.insertMember(entity)
            Timber.i("Added member to household: $householdId")
            Result.success(entity.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to add member")
            Result.failure(e)
        }
    }

    override suspend fun updateMember(
        householdId: String,
        memberId: String,
        canEditSharedPlan: Boolean?,
        portionSize: Float?,
        isTemporary: Boolean?
    ): Result<HouseholdMember> {
        return try {
            val response = apiService.updateHouseholdMember(
                householdId,
                memberId,
                UpdateMemberRequest(
                    canEditSharedPlan,
                    portionSize?.let { com.rasoiai.data.local.mapper.portionSizeFloatToString(it) },
                    isTemporary
                )
            )
            val entity = response.toEntity()
            householdDao.updateMember(entity)
            Timber.i("Updated member: $memberId")
            Result.success(entity.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to update member")
            Result.failure(e)
        }
    }

    override suspend fun removeMember(householdId: String, memberId: String): Result<Unit> {
        return try {
            apiService.removeHouseholdMember(householdId, memberId)
            householdDao.deleteMember(memberId)
            Timber.i("Removed member: $memberId")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to remove member")
            Result.failure(e)
        }
    }

    // ==================== Invite ====================

    override suspend fun refreshInviteCode(householdId: String): Result<InviteCode> {
        return try {
            val response = apiService.refreshInviteCode(householdId)
            householdDao.updateInviteCode(
                householdId,
                response.inviteCode,
                LocalDateTime.now().toString()
            )
            Timber.i("Refreshed invite code for household: $householdId")
            Result.success(
                InviteCode(
                    inviteCode = response.inviteCode,
                    expiresAt = LocalDateTime.parse(response.expiresAt)
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh invite code")
            Result.failure(e)
        }
    }

    override suspend fun joinHousehold(inviteCode: String): Result<HouseholdDetail> {
        return try {
            val response = apiService.joinHousehold(JoinHouseholdRequest(inviteCode))
            val householdEntity = response.household.toEntity()
            val memberEntities = response.members.map { it.toEntity() }
            householdDao.replaceHouseholdWithMembers(householdEntity, memberEntities)
            Timber.i("Joined household: ${response.household.id}")
            Result.success(response.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to join household")
            Result.failure(e)
        }
    }

    override suspend fun leaveHousehold(householdId: String): Result<Unit> {
        return try {
            apiService.leaveHousehold(householdId)
            householdDao.deleteHousehold(householdId)
            Timber.i("Left household: $householdId")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to leave household")
            Result.failure(e)
        }
    }

    override suspend fun transferOwnership(
        householdId: String,
        newOwnerMemberId: String
    ): Result<Unit> {
        return try {
            apiService.transferOwnership(
                householdId,
                TransferOwnershipRequest(newOwnerMemberId)
            )
            Timber.i("Transferred ownership to: $newOwnerMemberId")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to transfer ownership")
            Result.failure(e)
        }
    }

    // ==================== Scoped Data ====================

    override fun getHouseholdRecipeRules(householdId: String): Flow<List<RecipeRule>> = flow {
        try {
            val response = apiService.getHouseholdRecipeRules(householdId)
            emit(response.rules.map { dto ->
                dto.toEntity().toDomain()
            })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to get household recipe rules")
            emit(emptyList())
        }
    }

    override fun getHouseholdMealPlan(householdId: String): Flow<MealPlan?> = flow {
        try {
            val response = apiService.getHouseholdMealPlan(householdId)
            val entity = response.toEntity()
            val items = response.toItemEntities()
            val festivals = response.toFestivalEntities()
            emit(entity.toDomain(items, festivals))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to get household meal plan")
            emit(null)
        }
    }

    override fun getHouseholdNotifications(householdId: String): Flow<List<HouseholdNotification>> =
        flow {
            try {
                val response = apiService.getHouseholdNotifications(householdId)
                emit(response.map { it.toDomain() })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to get household notifications")
                emit(emptyList())
            }
        }

    override suspend fun getHouseholdStats(
        householdId: String,
        month: String?
    ): Result<HouseholdStats> {
        return try {
            val response = apiService.getHouseholdStats(householdId, month)
            Result.success(response.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to get household stats")
            Result.failure(e)
        }
    }

    override suspend fun markNotificationRead(notificationId: String): Result<Unit> {
        return try {
            // Get the active household to find the ID
            val household = householdDao.getActiveHouseholdSync()
                ?: return Result.failure(IllegalStateException("No active household"))
            apiService.markHouseholdNotificationRead(household.id, notificationId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to mark notification read")
            Result.failure(e)
        }
    }
}
