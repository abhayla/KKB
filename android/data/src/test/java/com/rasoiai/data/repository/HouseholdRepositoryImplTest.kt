package com.rasoiai.data.repository

import android.database.sqlite.SQLiteException
import app.cash.turbine.test
import com.rasoiai.data.local.dao.HouseholdDao
import com.rasoiai.data.local.entity.HouseholdEntity
import com.rasoiai.data.local.entity.HouseholdMemberEntity
import com.rasoiai.data.remote.api.RasoiApiService
import com.rasoiai.data.remote.dto.HouseholdDetailResponse
import com.rasoiai.data.remote.dto.HouseholdMemberResponse
import com.rasoiai.data.remote.dto.HouseholdNotificationResponse
import com.rasoiai.data.remote.dto.HouseholdResponse
import com.rasoiai.data.remote.dto.InviteCodeResponse
import com.rasoiai.data.remote.dto.SuccessResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class HouseholdRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockHouseholdDao: HouseholdDao
    private lateinit var mockApiService: RasoiApiService
    private lateinit var repository: HouseholdRepositoryImpl

    // ==================== Test Fixtures ====================

    private val testHouseholdEntity = HouseholdEntity(
        id = "hh-1",
        name = "Sharma Family",
        inviteCode = "ABC123",
        ownerId = "user-1",
        slotConfigJson = null,
        maxMembers = 8,
        memberCount = 2,
        isActive = true,
        createdAt = "2026-01-27T10:00:00",
        updatedAt = "2026-01-27T10:00:00"
    )

    private val testMemberEntity = HouseholdMemberEntity(
        id = "mem-1",
        householdId = "hh-1",
        userId = "user-1",
        familyMemberId = null,
        name = "Ramesh Sharma",
        role = "OWNER",
        canEditSharedPlan = true,
        isTemporary = false,
        joinDate = "2026-01-27T10:00:00",
        leaveDate = null,
        portionSize = 1.0f,
        status = "active"
    )

    private val testMemberEntity2 = HouseholdMemberEntity(
        id = "mem-2",
        householdId = "hh-1",
        userId = "user-2",
        familyMemberId = null,
        name = "Sunita Sharma",
        role = "MEMBER",
        canEditSharedPlan = false,
        isTemporary = false,
        joinDate = "2026-01-28T10:00:00",
        leaveDate = null,
        portionSize = 1.0f,
        status = "active"
    )

    private val testHouseholdResponse = HouseholdResponse(
        id = "hh-1",
        name = "Sharma Family",
        inviteCode = "ABC123",
        ownerId = "user-1",
        slotConfig = null,
        maxMembers = 8,
        memberCount = 2,
        isActive = true,
        createdAt = "2026-01-27T10:00:00",
        updatedAt = "2026-01-27T10:00:00"
    )

    private val testMemberResponse = HouseholdMemberResponse(
        id = "mem-1",
        householdId = "hh-1",
        userId = "user-1",
        familyMemberId = null,
        name = "Ramesh Sharma",
        role = "OWNER",
        canEditSharedPlan = true,
        isTemporary = false,
        joinDate = "2026-01-27T10:00:00",
        leaveDate = null,
        portionSize = "REGULAR",
        status = "active"
    )

    private val testMemberResponse2 = HouseholdMemberResponse(
        id = "mem-2",
        householdId = "hh-1",
        userId = "user-2",
        familyMemberId = null,
        name = "Sunita Sharma",
        role = "MEMBER",
        canEditSharedPlan = false,
        isTemporary = false,
        joinDate = "2026-01-28T10:00:00",
        leaveDate = null,
        portionSize = "REGULAR",
        status = "active"
    )

    private val testDetailResponse = HouseholdDetailResponse(
        household = testHouseholdResponse,
        members = listOf(testMemberResponse, testMemberResponse2)
    )

    private val testNotificationResponse = HouseholdNotificationResponse(
        id = "notif-1",
        householdId = "hh-1",
        type = "member_joined",
        title = "New Member",
        message = "Sunita joined the household",
        isRead = false,
        metadata = null,
        createdAt = "2026-01-28T10:00:00"
    )

    // ==================== Setup / Teardown ====================

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockHouseholdDao = mockk(relaxed = true)
        mockApiService = mockk(relaxed = true)
        repository = HouseholdRepositoryImpl(mockHouseholdDao, mockApiService)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== Tests ====================

    @Nested
    @DisplayName("getHousehold")
    inner class GetHousehold {

        @Test
        @DisplayName("returns household detail from DAO when exists")
        fun returnsHouseholdDetailWhenExists() = runTest {
            every { mockHouseholdDao.getHousehold("hh-1") } returns flowOf(testHouseholdEntity)
            coEvery { mockHouseholdDao.getMembersSync("hh-1") } returns listOf(testMemberEntity, testMemberEntity2)

            repository.getHousehold("hh-1").test {
                val detail = awaitItem()
                assertNotNull(detail)
                assertEquals("hh-1", detail!!.household.id)
                assertEquals("Sharma Family", detail.household.name)
                assertEquals(2, detail.members.size)
                assertEquals("Ramesh Sharma", detail.members[0].name)
                awaitComplete()
            }
        }

        @Test
        @DisplayName("returns null when no household found")
        fun returnsNullWhenNotFound() = runTest {
            every { mockHouseholdDao.getHousehold("nonexistent") } returns flowOf(null)

            repository.getHousehold("nonexistent").test {
                val detail = awaitItem()
                assertNull(detail)
                awaitComplete()
            }
        }
    }

    @Nested
    @DisplayName("getUserHousehold")
    inner class GetUserHousehold {

        @Test
        @DisplayName("returns active household from DAO")
        fun returnsActiveHousehold() = runTest {
            every { mockHouseholdDao.getActiveHousehold() } returns flowOf(testHouseholdEntity)
            coEvery { mockHouseholdDao.getMembersSync("hh-1") } returns listOf(testMemberEntity)

            repository.getUserHousehold().test {
                val detail = awaitItem()
                assertNotNull(detail)
                assertEquals("hh-1", detail!!.household.id)
                assertTrue(detail.household.isActive)
                awaitComplete()
            }
        }

        @Test
        @DisplayName("returns null when no active household")
        fun returnsNullWhenNoActiveHousehold() = runTest {
            every { mockHouseholdDao.getActiveHousehold() } returns flowOf(null)

            repository.getUserHousehold().test {
                val detail = awaitItem()
                assertNull(detail)
                awaitComplete()
            }
        }
    }

    @Nested
    @DisplayName("createHousehold")
    inner class CreateHousehold {

        @Test
        @DisplayName("creates via API and caches in Room on success")
        fun createsAndCaches() = runTest {
            coEvery { mockApiService.createHousehold(any()) } returns testDetailResponse

            val result = repository.createHousehold("Sharma Family")

            assertTrue(result.isSuccess)
            assertEquals("hh-1", result.getOrNull()!!.household.id)
            assertEquals(2, result.getOrNull()!!.members.size)
            coVerify { mockHouseholdDao.replaceHouseholdWithMembers(any(), any()) }
        }

        @Test
        @DisplayName("returns failure on API error")
        fun returnsFailureOnError() = runTest {
            // Realistic API error — issue #34 narrowed broad catch so bare RuntimeException now propagates.
            coEvery { mockApiService.createHousehold(any()) } throws retrofit2.HttpException(
                retrofit2.Response.error<Any>(500, okhttp3.ResponseBody.create(null, ""))
            )

            val result = repository.createHousehold("Sharma Family")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is retrofit2.HttpException)
        }
    }

    @Nested
    @DisplayName("updateHousehold")
    inner class UpdateHousehold {

        @Test
        @DisplayName("updates via API and refreshes local cache")
        fun updatesAndRefreshesCache() = runTest {
            coEvery { mockApiService.updateHousehold("hh-1", any()) } returns testDetailResponse

            val result = repository.updateHousehold("hh-1", name = "Updated Family", slotConfig = null, maxMembers = 10)

            assertTrue(result.isSuccess)
            assertEquals("hh-1", result.getOrNull()!!.household.id)
            coVerify { mockHouseholdDao.replaceHouseholdWithMembers(any(), any()) }
        }

        @Test
        @DisplayName("returns failure on API error")
        fun returnsFailureOnError() = runTest {
            // Realistic API error — issue #34 narrowed broad catch so bare RuntimeException now propagates.
            coEvery { mockApiService.updateHousehold("hh-1", any()) } throws retrofit2.HttpException(
                retrofit2.Response.error<Any>(403, okhttp3.ResponseBody.create(null, ""))
            )

            val result = repository.updateHousehold("hh-1", name = "Updated Family", slotConfig = null, maxMembers = null)

            assertTrue(result.isFailure)
        }
    }

    @Nested
    @DisplayName("deactivateHousehold")
    inner class DeactivateHousehold {

        @Test
        @DisplayName("deactivates via API and updates local DB")
        fun deactivatesSuccessfully() = runTest {
            coEvery { mockApiService.deactivateHousehold("hh-1") } returns SuccessResponse(success = true, message = "OK")

            val result = repository.deactivateHousehold("hh-1")

            assertTrue(result.isSuccess)
            coVerify { mockApiService.deactivateHousehold("hh-1") }
            coVerify { mockHouseholdDao.deactivateHousehold("hh-1", any()) }
        }

        @Test
        @DisplayName("returns failure on API error")
        fun returnsFailureOnError() = runTest {
            // Realistic API error — issue #34 narrowed broad catch so bare RuntimeException now propagates.
            coEvery { mockApiService.deactivateHousehold("hh-1") } throws retrofit2.HttpException(
                retrofit2.Response.error<Any>(404, okhttp3.ResponseBody.create(null, ""))
            )

            val result = repository.deactivateHousehold("hh-1")

            assertTrue(result.isFailure)
        }
    }

    @Nested
    @DisplayName("Member Management")
    inner class MemberManagement {

        @Test
        @DisplayName("addMember calls API and inserts locally")
        fun addMemberSuccess() = runTest {
            coEvery { mockApiService.addHouseholdMember("hh-1", any()) } returns testMemberResponse2

            val result = repository.addMember("hh-1", "+919876543210", isTemporary = false)

            assertTrue(result.isSuccess)
            assertEquals("Sunita Sharma", result.getOrNull()!!.name)
            coVerify { mockHouseholdDao.insertMember(any()) }
        }

        @Test
        @DisplayName("updateMember calls API and updates locally")
        fun updateMemberSuccess() = runTest {
            val updatedResponse = testMemberResponse2.copy(canEditSharedPlan = true, portionSize = "LARGE")
            coEvery { mockApiService.updateHouseholdMember("hh-1", "mem-2", any()) } returns updatedResponse

            val result = repository.updateMember(
                householdId = "hh-1",
                memberId = "mem-2",
                canEditSharedPlan = true,
                portionSize = 1.5f,
                isTemporary = null
            )

            assertTrue(result.isSuccess)
            assertEquals(true, result.getOrNull()!!.canEditSharedPlan)
            coVerify { mockHouseholdDao.updateMember(any()) }
        }

        @Test
        @DisplayName("removeMember calls API and deletes locally")
        fun removeMemberSuccess() = runTest {
            coEvery { mockApiService.removeHouseholdMember("hh-1", "mem-2") } returns SuccessResponse(success = true, message = "OK")

            val result = repository.removeMember("hh-1", "mem-2")

            assertTrue(result.isSuccess)
            coVerify { mockApiService.removeHouseholdMember("hh-1", "mem-2") }
            coVerify { mockHouseholdDao.deleteMember("mem-2") }
        }
    }

    @Nested
    @DisplayName("Invite and Join")
    inner class InviteAndJoin {

        @Test
        @DisplayName("refreshInviteCode updates local invite code")
        fun refreshInviteCodeSuccess() = runTest {
            val inviteResponse = InviteCodeResponse(
                inviteCode = "NEW456",
                expiresAt = "2026-02-03T10:00:00"
            )
            coEvery { mockApiService.refreshInviteCode("hh-1") } returns inviteResponse

            val result = repository.refreshInviteCode("hh-1")

            assertTrue(result.isSuccess)
            assertEquals("NEW456", result.getOrNull()!!.inviteCode)
            coVerify { mockHouseholdDao.updateInviteCode("hh-1", "NEW456", any()) }
        }

        @Test
        @DisplayName("joinHousehold replaces local household data atomically")
        fun joinHouseholdSuccess() = runTest {
            coEvery { mockApiService.joinHousehold(any()) } returns testDetailResponse

            val result = repository.joinHousehold("ABC123")

            assertTrue(result.isSuccess)
            assertEquals("hh-1", result.getOrNull()!!.household.id)
            assertEquals(2, result.getOrNull()!!.members.size)
            coVerify { mockHouseholdDao.replaceHouseholdWithMembers(any(), any()) }
        }

        @Test
        @DisplayName("leaveHousehold deletes local household")
        fun leaveHouseholdSuccess() = runTest {
            coEvery { mockApiService.leaveHousehold("hh-1") } returns SuccessResponse(success = true, message = "OK")

            val result = repository.leaveHousehold("hh-1")

            assertTrue(result.isSuccess)
            coVerify { mockApiService.leaveHousehold("hh-1") }
            coVerify { mockHouseholdDao.deleteHousehold("hh-1") }
        }
    }

    @Nested
    @DisplayName("Scoped Data")
    inner class ScopedData {

        @Test
        @DisplayName("getHouseholdNotifications emits notifications from API")
        fun getNotificationsSuccess() = runTest {
            coEvery { mockApiService.getHouseholdNotifications("hh-1") } returns listOf(testNotificationResponse)

            repository.getHouseholdNotifications("hh-1").test {
                val notifications = awaitItem()
                assertEquals(1, notifications.size)
                assertEquals("notif-1", notifications[0].id)
                assertEquals("New Member", notifications[0].title)
                assertEquals("Sunita joined the household", notifications[0].message)
                awaitComplete()
            }
        }

        @Test
        @DisplayName("getHouseholdNotifications emits empty list on error")
        fun getNotificationsEmptyOnError() = runTest {
            // Realistic API error — issue #34 narrowed broad catch so bare RuntimeException now propagates.
            coEvery { mockApiService.getHouseholdNotifications("hh-1") } throws retrofit2.HttpException(
                retrofit2.Response.error<Any>(500, okhttp3.ResponseBody.create(null, ""))
            )

            repository.getHouseholdNotifications("hh-1").test {
                val notifications = awaitItem()
                assertTrue(notifications.isEmpty())
                awaitComplete()
            }
        }
    }

    @Nested
    @DisplayName("markNotificationRead")
    inner class MarkNotificationRead {

        @Test
        @DisplayName("calls API with active household ID")
        fun markReadSuccess() = runTest {
            coEvery { mockHouseholdDao.getActiveHouseholdSync() } returns testHouseholdEntity
            coEvery { mockApiService.markHouseholdNotificationRead("hh-1", "notif-1") } returns SuccessResponse(success = true, message = "OK")

            val result = repository.markNotificationRead("notif-1")

            assertTrue(result.isSuccess)
            coVerify { mockApiService.markHouseholdNotificationRead("hh-1", "notif-1") }
        }

        @Test
        @DisplayName("fails when no active household exists")
        fun failsWhenNoActiveHousehold() = runTest {
            coEvery { mockHouseholdDao.getActiveHouseholdSync() } returns null

            val result = repository.markNotificationRead("notif-1")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IllegalStateException)
        }
    }

    @Nested
    @DisplayName("CancellationException propagation (structured concurrency)")
    inner class CancellationPropagation {

        @Test
        @DisplayName("createHousehold should propagate CancellationException instead of wrapping in Result.failure")
        fun `createHousehold should propagate CancellationException`() = runTest {
            coEvery { mockApiService.createHousehold(any()) } throws CancellationException("cancelled")
            try {
                repository.createHousehold("Sharma Family")
                fail("Expected CancellationException to propagate, got Result wrapper instead")
            } catch (e: CancellationException) {
                assertEquals("cancelled", e.message)
            }
        }
    }

    @Nested
    @DisplayName("Unexpected exception propagation (issue #34)")
    inner class UnexpectedExceptionPropagation {

        // ---- createHousehold ----

        @Test
        @DisplayName("createHousehold wraps SQLiteException in Result.failure")
        fun `createHousehold wraps SQLiteException`() = runTest {
            coEvery { mockApiService.createHousehold(any()) } returns testDetailResponse
            coEvery { mockHouseholdDao.replaceHouseholdWithMembers(any(), any()) } throws SQLiteException("disk full")

            val result = repository.createHousehold("Sharma Family")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is SQLiteException)
        }

        @Test
        @DisplayName("createHousehold wraps IOException in Result.failure")
        fun `createHousehold wraps IOException`() = runTest {
            coEvery { mockApiService.createHousehold(any()) } throws IOException("no network")

            val result = repository.createHousehold("Sharma Family")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IOException)
        }

        @Test
        @DisplayName("createHousehold propagates IllegalStateException instead of wrapping")
        fun `createHousehold propagates IllegalStateException`() = runTest {
            coEvery { mockApiService.createHousehold(any()) } throws IllegalStateException("db closed")
            try {
                repository.createHousehold("Sharma Family")
                fail("Expected IllegalStateException to propagate, got Result wrapper instead")
            } catch (e: IllegalStateException) {
                assertEquals("db closed", e.message)
            }
        }

        // ---- updateHousehold ----

        @Test
        @DisplayName("updateHousehold wraps SQLiteException in Result.failure")
        fun `updateHousehold wraps SQLiteException`() = runTest {
            coEvery { mockApiService.updateHousehold("hh-1", any()) } returns testDetailResponse
            coEvery { mockHouseholdDao.replaceHouseholdWithMembers(any(), any()) } throws SQLiteException("disk full")

            val result = repository.updateHousehold("hh-1", name = "x", slotConfig = null, maxMembers = null)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is SQLiteException)
        }

        @Test
        @DisplayName("updateHousehold propagates IllegalStateException instead of wrapping")
        fun `updateHousehold propagates IllegalStateException`() = runTest {
            coEvery { mockApiService.updateHousehold("hh-1", any()) } throws IllegalStateException("db closed")
            try {
                repository.updateHousehold("hh-1", name = "x", slotConfig = null, maxMembers = null)
                fail("Expected IllegalStateException to propagate, got Result wrapper instead")
            } catch (e: IllegalStateException) {
                assertEquals("db closed", e.message)
            }
        }

        // ---- deactivateHousehold ----

        @Test
        @DisplayName("deactivateHousehold propagates IllegalStateException instead of wrapping")
        fun `deactivateHousehold propagates IllegalStateException`() = runTest {
            coEvery { mockApiService.deactivateHousehold("hh-1") } throws IllegalStateException("db closed")
            try {
                repository.deactivateHousehold("hh-1")
                fail("Expected IllegalStateException to propagate, got Result wrapper instead")
            } catch (e: IllegalStateException) {
                assertEquals("db closed", e.message)
            }
        }

        // ---- addMember ----

        @Test
        @DisplayName("addMember wraps HttpException in Result.failure")
        fun `addMember wraps HttpException`() = runTest {
            coEvery { mockApiService.addHouseholdMember("hh-1", any()) } throws retrofit2.HttpException(
                retrofit2.Response.error<Any>(400, okhttp3.ResponseBody.create(null, ""))
            )

            val result = repository.addMember("hh-1", "+919876543210", isTemporary = false)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is retrofit2.HttpException)
        }

        @Test
        @DisplayName("addMember propagates IllegalStateException instead of wrapping")
        fun `addMember propagates IllegalStateException`() = runTest {
            coEvery { mockApiService.addHouseholdMember("hh-1", any()) } throws IllegalStateException("unexpected")
            try {
                repository.addMember("hh-1", "+919876543210", isTemporary = false)
                fail("Expected IllegalStateException to propagate, got Result wrapper instead")
            } catch (e: IllegalStateException) {
                assertEquals("unexpected", e.message)
            }
        }

        // ---- updateMember ----

        @Test
        @DisplayName("updateMember propagates IllegalStateException instead of wrapping")
        fun `updateMember propagates IllegalStateException`() = runTest {
            coEvery { mockApiService.updateHouseholdMember("hh-1", "mem-2", any()) } throws
                IllegalStateException("unexpected")
            try {
                repository.updateMember(
                    householdId = "hh-1",
                    memberId = "mem-2",
                    canEditSharedPlan = true,
                    portionSize = 1.0f,
                    isTemporary = null
                )
                fail("Expected IllegalStateException to propagate, got Result wrapper instead")
            } catch (e: IllegalStateException) {
                assertEquals("unexpected", e.message)
            }
        }

        // ---- removeMember ----

        @Test
        @DisplayName("removeMember propagates IllegalStateException instead of wrapping")
        fun `removeMember propagates IllegalStateException`() = runTest {
            coEvery { mockApiService.removeHouseholdMember("hh-1", "mem-2") } throws
                IllegalStateException("unexpected")
            try {
                repository.removeMember("hh-1", "mem-2")
                fail("Expected IllegalStateException to propagate, got Result wrapper instead")
            } catch (e: IllegalStateException) {
                assertEquals("unexpected", e.message)
            }
        }

        // ---- refreshInviteCode ----

        @Test
        @DisplayName("refreshInviteCode propagates IllegalStateException instead of wrapping")
        fun `refreshInviteCode propagates IllegalStateException`() = runTest {
            coEvery { mockApiService.refreshInviteCode("hh-1") } throws IllegalStateException("unexpected")
            try {
                repository.refreshInviteCode("hh-1")
                fail("Expected IllegalStateException to propagate, got Result wrapper instead")
            } catch (e: IllegalStateException) {
                assertEquals("unexpected", e.message)
            }
        }

        // ---- joinHousehold ----

        @Test
        @DisplayName("joinHousehold propagates IllegalStateException instead of wrapping")
        fun `joinHousehold propagates IllegalStateException`() = runTest {
            coEvery { mockApiService.joinHousehold(any()) } throws IllegalStateException("unexpected")
            try {
                repository.joinHousehold("ABC123")
                fail("Expected IllegalStateException to propagate, got Result wrapper instead")
            } catch (e: IllegalStateException) {
                assertEquals("unexpected", e.message)
            }
        }

        // ---- leaveHousehold ----

        @Test
        @DisplayName("leaveHousehold propagates IllegalStateException instead of wrapping")
        fun `leaveHousehold propagates IllegalStateException`() = runTest {
            coEvery { mockApiService.leaveHousehold("hh-1") } throws IllegalStateException("unexpected")
            try {
                repository.leaveHousehold("hh-1")
                fail("Expected IllegalStateException to propagate, got Result wrapper instead")
            } catch (e: IllegalStateException) {
                assertEquals("unexpected", e.message)
            }
        }

        // ---- transferOwnership ----

        @Test
        @DisplayName("transferOwnership propagates IllegalStateException instead of wrapping")
        fun `transferOwnership propagates IllegalStateException`() = runTest {
            coEvery { mockApiService.transferOwnership("hh-1", any()) } throws IllegalStateException("unexpected")
            try {
                repository.transferOwnership("hh-1", "mem-2")
                fail("Expected IllegalStateException to propagate, got Result wrapper instead")
            } catch (e: IllegalStateException) {
                assertEquals("unexpected", e.message)
            }
        }

        // ---- getHouseholdRecipeRules (Flow) ----

        @Test
        @DisplayName("getHouseholdRecipeRules emits empty list on HttpException")
        fun `getHouseholdRecipeRules emits empty on HttpException`() = runTest {
            coEvery { mockApiService.getHouseholdRecipeRules("hh-1") } throws retrofit2.HttpException(
                retrofit2.Response.error<Any>(500, okhttp3.ResponseBody.create(null, ""))
            )

            repository.getHouseholdRecipeRules("hh-1").test {
                val rules = awaitItem()
                assertTrue(rules.isEmpty())
                awaitComplete()
            }
        }

        @Test
        @DisplayName("getHouseholdRecipeRules propagates IllegalStateException instead of emitting empty")
        fun `getHouseholdRecipeRules propagates IllegalStateException`() = runTest {
            coEvery { mockApiService.getHouseholdRecipeRules("hh-1") } throws IllegalStateException("unexpected")
            try {
                repository.getHouseholdRecipeRules("hh-1").collect()
                fail("Expected IllegalStateException to propagate")
            } catch (e: IllegalStateException) {
                assertEquals("unexpected", e.message)
            }
        }

        // ---- getHouseholdMealPlan (Flow) ----

        @Test
        @DisplayName("getHouseholdMealPlan emits null on HttpException")
        fun `getHouseholdMealPlan emits null on HttpException`() = runTest {
            coEvery { mockApiService.getHouseholdMealPlan("hh-1") } throws retrofit2.HttpException(
                retrofit2.Response.error<Any>(500, okhttp3.ResponseBody.create(null, ""))
            )

            repository.getHouseholdMealPlan("hh-1").test {
                val mealPlan = awaitItem()
                assertNull(mealPlan)
                awaitComplete()
            }
        }

        @Test
        @DisplayName("getHouseholdMealPlan propagates IllegalStateException instead of emitting null")
        fun `getHouseholdMealPlan propagates IllegalStateException`() = runTest {
            coEvery { mockApiService.getHouseholdMealPlan("hh-1") } throws IllegalStateException("unexpected")
            try {
                repository.getHouseholdMealPlan("hh-1").collect()
                fail("Expected IllegalStateException to propagate")
            } catch (e: IllegalStateException) {
                assertEquals("unexpected", e.message)
            }
        }

        // ---- getHouseholdNotifications (Flow) ----

        @Test
        @DisplayName("getHouseholdNotifications propagates IllegalStateException instead of emitting empty")
        fun `getHouseholdNotifications propagates IllegalStateException`() = runTest {
            coEvery { mockApiService.getHouseholdNotifications("hh-1") } throws IllegalStateException("unexpected")
            try {
                repository.getHouseholdNotifications("hh-1").collect()
                fail("Expected IllegalStateException to propagate")
            } catch (e: IllegalStateException) {
                assertEquals("unexpected", e.message)
            }
        }

        // ---- getHouseholdStats ----

        @Test
        @DisplayName("getHouseholdStats propagates IllegalStateException instead of wrapping")
        fun `getHouseholdStats propagates IllegalStateException`() = runTest {
            coEvery { mockApiService.getHouseholdStats("hh-1", null) } throws IllegalStateException("unexpected")
            try {
                repository.getHouseholdStats("hh-1", null)
                fail("Expected IllegalStateException to propagate, got Result wrapper instead")
            } catch (e: IllegalStateException) {
                assertEquals("unexpected", e.message)
            }
        }

        // ---- markNotificationRead ----

        @Test
        @DisplayName("markNotificationRead propagates IllegalStateException from API instead of wrapping")
        fun `markNotificationRead propagates IllegalStateException`() = runTest {
            coEvery { mockHouseholdDao.getActiveHouseholdSync() } returns testHouseholdEntity
            coEvery { mockApiService.markHouseholdNotificationRead("hh-1", "notif-1") } throws
                IllegalStateException("unexpected")
            try {
                repository.markNotificationRead("notif-1")
                fail("Expected IllegalStateException to propagate, got Result wrapper instead")
            } catch (e: IllegalStateException) {
                assertEquals("unexpected", e.message)
            }
        }
    }
}
