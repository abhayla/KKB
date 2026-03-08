package com.rasoiai.data.local.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.rasoiai.data.local.entity.HouseholdEntity
import com.rasoiai.data.local.entity.HouseholdMemberEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HouseholdDaoTest : BaseDaoTest() {

    private val householdDao: HouseholdDao get() = database.householdDao()

    private val testHousehold = HouseholdEntity(
        id = "hh-1",
        name = "Sharma Family",
        inviteCode = "ABC123",
        ownerId = "user-1",
        slotConfigJson = null,
        maxMembers = 8,
        memberCount = 3,
        isActive = true,
        createdAt = "2026-03-01T10:00:00",
        updatedAt = "2026-03-01T10:00:00"
    )

    private val testMember1 = HouseholdMemberEntity(
        id = "mem-1",
        householdId = "hh-1",
        userId = "user-1",
        familyMemberId = null,
        name = "Ramesh",
        role = "owner",
        canEditSharedPlan = true,
        isTemporary = false,
        joinDate = "2026-03-01T10:00:00",
        leaveDate = null,
        portionSize = 1.0f,
        status = "active"
    )

    private val testMember2 = HouseholdMemberEntity(
        id = "mem-2",
        householdId = "hh-1",
        userId = "user-2",
        familyMemberId = null,
        name = "Sunita",
        role = "member",
        canEditSharedPlan = false,
        isTemporary = false,
        joinDate = "2026-03-01T10:00:00",
        leaveDate = null,
        portionSize = 1.0f,
        status = "active"
    )

    @Test
    fun insertHousehold_andGetById_returnsHousehold() = runTest {
        householdDao.insertHousehold(testHousehold)

        householdDao.getHousehold("hh-1").test {
            val result = awaitItem()
            assertNotNull(result)
            assertEquals("Sharma Family", result!!.name)
            assertEquals("ABC123", result.inviteCode)
            assertEquals("user-1", result.ownerId)
            assertTrue(result.isActive)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getActiveHousehold_returnsOnlyActive() = runTest {
        val inactiveHousehold = testHousehold.copy(
            id = "hh-2",
            name = "Inactive Family",
            isActive = false
        )
        householdDao.insertHousehold(testHousehold)
        householdDao.insertHousehold(inactiveHousehold)

        householdDao.getActiveHousehold().test {
            val result = awaitItem()
            assertNotNull(result)
            assertEquals("hh-1", result!!.id)
            assertEquals("Sharma Family", result.name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deactivateHousehold_setsIsActiveFalse() = runTest {
        householdDao.insertHousehold(testHousehold)

        householdDao.deactivateHousehold("hh-1", "2026-03-02T10:00:00")

        householdDao.getHousehold("hh-1").test {
            val result = awaitItem()
            assertNotNull(result)
            assertFalse(result!!.isActive)
            assertEquals("2026-03-02T10:00:00", result.updatedAt)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun updateInviteCode_updatesCode() = runTest {
        householdDao.insertHousehold(testHousehold)

        householdDao.updateInviteCode("hh-1", "XYZ789", "2026-03-02T12:00:00")

        householdDao.getHousehold("hh-1").test {
            val result = awaitItem()
            assertNotNull(result)
            assertEquals("XYZ789", result!!.inviteCode)
            assertEquals("2026-03-02T12:00:00", result.updatedAt)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun replaceHouseholdWithMembers_atomicTransaction() = runTest {
        // Insert household with initial members
        householdDao.insertHousehold(testHousehold)
        householdDao.insertMember(testMember1)

        // Replace with updated household and new member list
        val updatedHousehold = testHousehold.copy(
            name = "Updated Sharma Family",
            memberCount = 2,
            updatedAt = "2026-03-02T10:00:00"
        )
        val newMembers = listOf(testMember1, testMember2)

        householdDao.replaceHouseholdWithMembers(updatedHousehold, newMembers)

        // Verify household was updated
        householdDao.getHousehold("hh-1").test {
            val result = awaitItem()
            assertNotNull(result)
            assertEquals("Updated Sharma Family", result!!.name)
            assertEquals(2, result.memberCount)
            cancelAndIgnoreRemainingEvents()
        }

        // Verify members were replaced
        householdDao.getMembers("hh-1").test {
            val members = awaitItem()
            assertEquals(2, members.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun insertMember_andGetMembers_returnsMembers() = runTest {
        householdDao.insertHousehold(testHousehold)
        householdDao.insertMember(testMember1)
        householdDao.insertMember(testMember2)

        householdDao.getMembers("hh-1").test {
            val members = awaitItem()
            assertEquals(2, members.size)
            assertEquals("Ramesh", members[0].name)
            assertEquals("Sunita", members[1].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteMember_removesSingleMember() = runTest {
        householdDao.insertHousehold(testHousehold)
        householdDao.insertMember(testMember1)
        householdDao.insertMember(testMember2)

        householdDao.deleteMember("mem-1")

        householdDao.getMembers("hh-1").test {
            val members = awaitItem()
            assertEquals(1, members.size)
            assertEquals("Sunita", members[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getMemberCount_returnsCorrectCount() = runTest {
        householdDao.insertHousehold(testHousehold)
        householdDao.insertMember(testMember1)
        householdDao.insertMember(testMember2)

        val count = householdDao.getMemberCount("hh-1")
        assertEquals(2, count)
    }

    @Test
    fun deleteHousehold_cascadesToMembers() = runTest {
        householdDao.insertHousehold(testHousehold)
        householdDao.insertMember(testMember1)
        householdDao.insertMember(testMember2)

        householdDao.deleteHousehold("hh-1")

        // Household should be gone
        householdDao.getHousehold("hh-1").test {
            val result = awaitItem()
            assertNull(result)
            cancelAndIgnoreRemainingEvents()
        }

        // Members should be cascade-deleted
        householdDao.getMembers("hh-1").test {
            val members = awaitItem()
            assertTrue(members.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getActiveHouseholdSync_returnsNonFlowResult() = runTest {
        householdDao.insertHousehold(testHousehold)

        val result = householdDao.getActiveHouseholdSync()
        assertNotNull(result)
        assertEquals("hh-1", result!!.id)
        assertEquals("Sharma Family", result.name)
    }
}
