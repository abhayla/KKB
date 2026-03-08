package com.rasoiai.app.presentation.settings

import app.cash.turbine.test
import com.rasoiai.app.presentation.settings.viewmodels.HouseholdMembersNavigationEvent
import com.rasoiai.app.presentation.settings.viewmodels.HouseholdMembersViewModel
import com.rasoiai.domain.model.Household
import com.rasoiai.domain.model.HouseholdDetail
import com.rasoiai.domain.model.HouseholdMember
import com.rasoiai.domain.model.HouseholdRole
import com.rasoiai.domain.model.MemberStatus
import com.rasoiai.domain.repository.HouseholdRepository
import io.mockk.coEvery
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
class HouseholdMembersViewModelTest {

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

    private val ownerMember = HouseholdMember(
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

    private val regularMember = HouseholdMember(
        id = "member-2",
        userId = "user-2",
        familyMemberId = null,
        name = "Sunita Sharma",
        role = HouseholdRole.MEMBER,
        canEditSharedPlan = false,
        isTemporary = false,
        joinDate = LocalDateTime.now(),
        status = MemberStatus.ACTIVE
    )

    private val testHouseholdDetail = HouseholdDetail(
        household = testHousehold,
        members = listOf(ownerMember, regularMember)
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        householdRepository = mockk(relaxed = true)
        coEvery { householdRepository.getUserHousehold() } returns flowOf(testHouseholdDetail)
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
        @DisplayName("init loads members from repository")
        fun `init loads members from repository`() = runTest {
            val viewModel = HouseholdMembersViewModel(householdRepository)
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                assertFalse(state.isLoading)
                assertEquals(2, state.members.size)
                assertEquals("household-1", state.householdId)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("no household emits empty members list")
        fun `no household emits empty members list`() = runTest {
            coEvery { householdRepository.getUserHousehold() } returns flowOf(null)

            val viewModel = HouseholdMembersViewModel(householdRepository)
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                assertFalse(state.isLoading)
                assertTrue(state.members.isEmpty())
                assertNull(state.householdId)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    // ---------------------------------------------------------------------------
    // addMember
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("addMember")
    inner class AddMember {

        @Test
        @DisplayName("addMember success emits ShowSnackbar event")
        fun `addMember success emits ShowSnackbar event`() = runTest {
            val newMember = HouseholdMember(
                id = "member-3",
                userId = "user-3",
                familyMemberId = null,
                name = "Aarav Sharma",
                role = HouseholdRole.MEMBER,
                canEditSharedPlan = false,
                isTemporary = false,
                joinDate = LocalDateTime.now(),
                status = MemberStatus.ACTIVE
            )
            coEvery { householdRepository.addMember("household-1", "+919876543210") } returns
                Result.success(newMember)

            val viewModel = HouseholdMembersViewModel(householdRepository)
            advanceUntilIdle()

            viewModel.onPhoneChanged("+919876543210")

            viewModel.navigationEvent.test {
                viewModel.addMember()
                advanceUntilIdle()

                val event = awaitItem()
                assertTrue(event is HouseholdMembersNavigationEvent.ShowSnackbar)
                assertEquals("Member added", (event as HouseholdMembersNavigationEvent.ShowSnackbar).message)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("addMember success clears phone number")
        fun `addMember success clears phone number`() = runTest {
            val newMember = HouseholdMember(
                id = "member-3",
                userId = "user-3",
                familyMemberId = null,
                name = "Aarav Sharma",
                role = HouseholdRole.MEMBER,
                canEditSharedPlan = false,
                isTemporary = false,
                joinDate = LocalDateTime.now(),
                status = MemberStatus.ACTIVE
            )
            coEvery { householdRepository.addMember("household-1", "+919876543210") } returns
                Result.success(newMember)

            val viewModel = HouseholdMembersViewModel(householdRepository)
            advanceUntilIdle()

            viewModel.onPhoneChanged("+919876543210")
            viewModel.addMember()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                assertFalse(state.isLoading)
                assertEquals("", state.phoneNumber)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("addMember failure shows error")
        fun `addMember failure shows error`() = runTest {
            coEvery { householdRepository.addMember(any(), any()) } returns
                Result.failure(Exception("User not found"))

            val viewModel = HouseholdMembersViewModel(householdRepository)
            advanceUntilIdle()

            viewModel.onPhoneChanged("+919876543210")
            viewModel.addMember()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                assertFalse(state.isLoading)
                assertEquals("User not found", state.error)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("addMember with blank phone does nothing")
        fun `addMember with blank phone does nothing`() = runTest {
            val viewModel = HouseholdMembersViewModel(householdRepository)
            advanceUntilIdle()

            // phone is blank by default
            viewModel.addMember()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                assertNull(state.error)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    // ---------------------------------------------------------------------------
    // removeMember
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("removeMember")
    inner class RemoveMember {

        @Test
        @DisplayName("removeMember success emits ShowSnackbar event")
        fun `removeMember success emits ShowSnackbar event`() = runTest {
            coEvery { householdRepository.removeMember("household-1", "member-2") } returns
                Result.success(Unit)

            val viewModel = HouseholdMembersViewModel(householdRepository)
            advanceUntilIdle()

            viewModel.navigationEvent.test {
                viewModel.removeMember("member-2")
                advanceUntilIdle()

                val event = awaitItem()
                assertTrue(event is HouseholdMembersNavigationEvent.ShowSnackbar)
                assertEquals("Member removed", (event as HouseholdMembersNavigationEvent.ShowSnackbar).message)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("removeMember failure shows error")
        fun `removeMember failure shows error`() = runTest {
            coEvery { householdRepository.removeMember(any(), any()) } returns
                Result.failure(Exception("Cannot remove owner"))

            val viewModel = HouseholdMembersViewModel(householdRepository)
            advanceUntilIdle()

            viewModel.removeMember("member-1")
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals("Cannot remove owner", state.error)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Dialog visibility
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("Dialog Visibility")
    inner class DialogVisibility {

        @Test
        @DisplayName("showAddMemberDialog updates state")
        fun `showAddMemberDialog updates state`() = runTest {
            coEvery { householdRepository.getUserHousehold() } returns flowOf(null)

            val viewModel = HouseholdMembersViewModel(householdRepository)
            viewModel.showAddMemberDialog()

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state.showAddMemberDialog)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("dismissAddMemberDialog clears phone number and dialog")
        fun `dismissAddMemberDialog clears phone number and dialog`() = runTest {
            coEvery { householdRepository.getUserHousehold() } returns flowOf(null)

            val viewModel = HouseholdMembersViewModel(householdRepository)
            viewModel.showAddMemberDialog()
            viewModel.onPhoneChanged("+919876543210")
            viewModel.dismissAddMemberDialog()

            viewModel.uiState.test {
                val state = awaitItem()
                assertFalse(state.showAddMemberDialog)
                assertEquals("", state.phoneNumber)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Field updates
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("Field Updates")
    inner class FieldUpdates {

        @Test
        @DisplayName("setPhoneNumber updates state")
        fun `setPhoneNumber updates state`() = runTest {
            coEvery { householdRepository.getUserHousehold() } returns flowOf(null)

            val viewModel = HouseholdMembersViewModel(householdRepository)
            viewModel.onPhoneChanged("+911234567890")

            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals("+911234567890", state.phoneNumber)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
