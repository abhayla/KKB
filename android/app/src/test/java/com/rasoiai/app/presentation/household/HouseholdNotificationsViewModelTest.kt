package com.rasoiai.app.presentation.household

import app.cash.turbine.test
import com.rasoiai.domain.model.Household
import com.rasoiai.domain.model.HouseholdDetail
import com.rasoiai.domain.model.HouseholdMember
import com.rasoiai.domain.model.HouseholdNotification
import com.rasoiai.domain.model.HouseholdNotificationType
import com.rasoiai.domain.model.HouseholdRole
import com.rasoiai.domain.model.MemberStatus
import com.rasoiai.domain.repository.HouseholdRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class HouseholdNotificationsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var householdRepository: HouseholdRepository

    private val testHousehold = Household(
        id = "household-1",
        name = "Sharma Family",
        inviteCode = "SHARMA123",
        ownerId = "user-1",
        maxMembers = 8,
        memberCount = 2,
        isActive = true,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    private val testHouseholdDetail = HouseholdDetail(
        household = testHousehold,
        members = listOf(
            HouseholdMember(
                id = "member-1",
                userId = "user-1",
                familyMemberId = null,
                name = "Ramesh Sharma",
                role = HouseholdRole.OWNER,
                canEditSharedPlan = true,
                isTemporary = false,
                joinDate = LocalDateTime.now(),
                status = MemberStatus.ACTIVE
            )
        )
    )

    private val unreadNotification = HouseholdNotification(
        id = "notif-1",
        householdId = "household-1",
        type = HouseholdNotificationType.MEMBER_JOINED,
        title = "New member joined",
        message = "Sunita joined your household",
        isRead = false,
        createdAt = LocalDateTime.now()
    )

    private val readNotification = HouseholdNotification(
        id = "notif-2",
        householdId = "household-1",
        type = HouseholdNotificationType.PLAN_REGENERATED,
        title = "Meal plan updated",
        message = "Your weekly meal plan has been regenerated",
        isRead = true,
        createdAt = LocalDateTime.now().minusHours(2)
    )

    private val anotherUnreadNotification = HouseholdNotification(
        id = "notif-3",
        householdId = "household-1",
        type = HouseholdNotificationType.RULE_ADDED,
        title = "New rule added",
        message = "A new recipe rule was added",
        isRead = false,
        createdAt = LocalDateTime.now().minusMinutes(30)
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        householdRepository = mockk(relaxed = true)
        coEvery { householdRepository.getUserHousehold() } returns flowOf(testHouseholdDetail)
        coEvery { householdRepository.getHouseholdNotifications("household-1") } returns
            flowOf(listOf(unreadNotification, readNotification, anotherUnreadNotification))
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ---------------------------------------------------------------------------
    // Initial load
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("Initial Load")
    inner class InitialLoad {

        @Test
        @DisplayName("init loads notifications from repository")
        fun `init loads notifications from repository`() = runTest {
            val viewModel = HouseholdNotificationsViewModel(householdRepository)
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                assertFalse(state.isLoading)
                assertEquals(3, state.notifications.size)
                assertEquals("household-1", state.householdId)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("unread count calculated correctly")
        fun `unread count calculated correctly`() = runTest {
            val viewModel = HouseholdNotificationsViewModel(householdRepository)
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                // unreadNotification and anotherUnreadNotification are unread; readNotification is read
                assertEquals(2, state.unreadCount)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("no household shows empty state")
        fun `no household shows empty state`() = runTest {
            coEvery { householdRepository.getUserHousehold() } returns flowOf(null)

            val viewModel = HouseholdNotificationsViewModel(householdRepository)
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                assertFalse(state.isLoading)
                assertTrue(state.notifications.isEmpty())
                assertEquals(0, state.unreadCount)
                assertNull(state.householdId)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("all read notifications give zero unread count")
        fun `all read notifications give zero unread count`() = runTest {
            val allReadNotifications = listOf(
                readNotification,
                readNotification.copy(id = "notif-4", title = "Another read notification")
            )
            coEvery { householdRepository.getHouseholdNotifications("household-1") } returns
                flowOf(allReadNotifications)

            val viewModel = HouseholdNotificationsViewModel(householdRepository)
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals(0, state.unreadCount)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    // ---------------------------------------------------------------------------
    // markAsRead
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("markAsRead")
    inner class MarkAsRead {

        @Test
        @DisplayName("markAsRead calls repository with correct id")
        fun `markAsRead calls repository with correct id`() = runTest {
            coEvery { householdRepository.markNotificationRead("notif-1") } returns
                Result.success(Unit)

            val viewModel = HouseholdNotificationsViewModel(householdRepository)
            advanceUntilIdle()

            viewModel.markAsRead("notif-1")
            advanceUntilIdle()

            coVerify(exactly = 1) { householdRepository.markNotificationRead("notif-1") }
        }

        @Test
        @DisplayName("markAsRead failure shows error")
        fun `markAsRead failure shows error`() = runTest {
            coEvery { householdRepository.markNotificationRead(any()) } returns
                Result.failure(Exception("Mark read failed"))

            val viewModel = HouseholdNotificationsViewModel(householdRepository)
            advanceUntilIdle()

            viewModel.markAsRead("notif-1")
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals("Mark read failed", state.error)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
