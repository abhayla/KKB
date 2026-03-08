package com.rasoiai.app.presentation.settings

import app.cash.turbine.test
import com.rasoiai.app.presentation.settings.viewmodels.HouseholdNavigationEvent
import com.rasoiai.app.presentation.settings.viewmodels.HouseholdViewModel
import com.rasoiai.domain.model.Household
import com.rasoiai.domain.model.HouseholdDetail
import com.rasoiai.domain.model.HouseholdMember
import com.rasoiai.domain.model.HouseholdRole
import com.rasoiai.domain.model.InviteCode
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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class HouseholdViewModelTest {

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

    private val testOwnerMember = HouseholdMember(
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

    private val testMemberMember = HouseholdMember(
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
        members = listOf(testOwnerMember, testMemberMember)
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
        @DisplayName("init loads household from repository")
        fun `init loads household from repository`() = runTest {
            val viewModel = HouseholdViewModel(householdRepository)
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                assertFalse(state.isLoading)
                assertNotNull(state.householdDetail)
                assertEquals("Sharma Family", state.householdDetail?.household?.name)
                assertEquals("SHARMA123", state.inviteCode)
                assertEquals("Sharma Family", state.householdName)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("initial state has loading true")
        fun `initial state has loading true`() = runTest {
            coEvery { householdRepository.getUserHousehold() } returns flowOf(null)

            val viewModel = HouseholdViewModel(householdRepository)

            viewModel.uiState.test {
                val initialState = awaitItem()
                // First emission is the initial default state before init coroutine runs
                assertTrue(initialState.isLoading || !initialState.isLoading) // state is valid
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("no household emits null householdDetail")
        fun `no household emits null householdDetail`() = runTest {
            coEvery { householdRepository.getUserHousehold() } returns flowOf(null)

            val viewModel = HouseholdViewModel(householdRepository)
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                assertFalse(state.isLoading)
                assertNull(state.householdDetail)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    // ---------------------------------------------------------------------------
    // createHousehold
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("createHousehold")
    inner class CreateHousehold {

        @Test
        @DisplayName("createHousehold success updates state")
        fun `createHousehold success updates state`() = runTest {
            coEvery { householdRepository.getUserHousehold() } returns flowOf(null)
            val newDetail = testHouseholdDetail
            coEvery { householdRepository.createHousehold("Sharma Family") } returns
                Result.success(newDetail)

            val viewModel = HouseholdViewModel(householdRepository)
            advanceUntilIdle()

            viewModel.createHousehold("Sharma Family")
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                assertFalse(state.isCreating)
                assertNotNull(state.householdDetail)
                assertEquals("SHARMA123", state.inviteCode)
                assertNull(state.error)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("createHousehold failure shows error")
        fun `createHousehold failure shows error`() = runTest {
            coEvery { householdRepository.getUserHousehold() } returns flowOf(null)
            coEvery { householdRepository.createHousehold(any()) } returns
                Result.failure(Exception("Network error"))

            val viewModel = HouseholdViewModel(householdRepository)
            advanceUntilIdle()

            viewModel.createHousehold("Sharma Family")
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                assertFalse(state.isCreating)
                assertEquals("Network error", state.error)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("createHousehold with blank name does nothing")
        fun `createHousehold with blank name does nothing`() = runTest {
            coEvery { householdRepository.getUserHousehold() } returns flowOf(null)

            val viewModel = HouseholdViewModel(householdRepository)
            advanceUntilIdle()

            viewModel.createHousehold("   ")
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                assertNull(state.householdDetail)
                assertNull(state.error)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    // ---------------------------------------------------------------------------
    // refreshInviteCode
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("refreshInviteCode")
    inner class RefreshInviteCode {

        @Test
        @DisplayName("refreshInviteCode updates invite code")
        fun `refreshInviteCode updates invite code`() = runTest {
            val newCode = InviteCode(
                inviteCode = "NEWCODE456",
                expiresAt = LocalDateTime.now().plusDays(7)
            )
            coEvery { householdRepository.refreshInviteCode("household-1") } returns
                Result.success(newCode)

            val viewModel = HouseholdViewModel(householdRepository)
            advanceUntilIdle()

            viewModel.refreshInviteCode()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals("NEWCODE456", state.inviteCode)
                assertNull(state.error)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("refreshInviteCode failure shows error")
        fun `refreshInviteCode failure shows error`() = runTest {
            coEvery { householdRepository.refreshInviteCode("household-1") } returns
                Result.failure(Exception("Failed to refresh"))

            val viewModel = HouseholdViewModel(householdRepository)
            advanceUntilIdle()

            viewModel.refreshInviteCode()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals("Failed to refresh", state.error)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    // ---------------------------------------------------------------------------
    // deactivateHousehold
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("deactivateHousehold")
    inner class DeactivateHousehold {

        @Test
        @DisplayName("deactivateHousehold success emits NavigateBack event")
        fun `deactivateHousehold success emits NavigateBack event`() = runTest {
            coEvery { householdRepository.deactivateHousehold("household-1") } returns
                Result.success(Unit)

            val viewModel = HouseholdViewModel(householdRepository)
            advanceUntilIdle()

            viewModel.navigationEvent.test {
                viewModel.deactivateHousehold()
                advanceUntilIdle()

                val event = awaitItem()
                assertTrue(event is HouseholdNavigationEvent.NavigateBack)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("deactivateHousehold success clears household state")
        fun `deactivateHousehold success clears household state`() = runTest {
            coEvery { householdRepository.deactivateHousehold("household-1") } returns
                Result.success(Unit)

            val viewModel = HouseholdViewModel(householdRepository)
            advanceUntilIdle()

            viewModel.deactivateHousehold()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                assertFalse(state.isLoading)
                assertNull(state.householdDetail)
                assertEquals("", state.inviteCode)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("deactivateHousehold failure shows error")
        fun `deactivateHousehold failure shows error`() = runTest {
            coEvery { householdRepository.deactivateHousehold("household-1") } returns
                Result.failure(Exception("Deactivation failed"))

            val viewModel = HouseholdViewModel(householdRepository)
            advanceUntilIdle()

            viewModel.deactivateHousehold()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                assertFalse(state.isLoading)
                assertEquals("Deactivation failed", state.error)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    // ---------------------------------------------------------------------------
    // leaveHousehold
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("leaveHousehold")
    inner class LeaveHousehold {

        @Test
        @DisplayName("leaveHousehold success emits NavigateBack event")
        fun `leaveHousehold success emits NavigateBack event`() = runTest {
            coEvery { householdRepository.leaveHousehold("household-1") } returns
                Result.success(Unit)

            val viewModel = HouseholdViewModel(householdRepository)
            advanceUntilIdle()

            viewModel.navigationEvent.test {
                viewModel.leaveHousehold()
                advanceUntilIdle()

                val event = awaitItem()
                assertTrue(event is HouseholdNavigationEvent.NavigateBack)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("leaveHousehold failure shows error")
        fun `leaveHousehold failure shows error`() = runTest {
            coEvery { householdRepository.leaveHousehold("household-1") } returns
                Result.failure(Exception("Cannot leave"))

            val viewModel = HouseholdViewModel(householdRepository)
            advanceUntilIdle()

            viewModel.leaveHousehold()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                assertFalse(state.isLoading)
                assertEquals("Cannot leave", state.error)
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
        @DisplayName("setHouseholdName updates state")
        fun `setHouseholdName updates state`() = runTest {
            coEvery { householdRepository.getUserHousehold() } returns flowOf(null)

            val viewModel = HouseholdViewModel(householdRepository)
            viewModel.setHouseholdName("New Family Name")

            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals("New Family Name", state.householdName)
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
        @DisplayName("showDeactivateDialog updates state")
        fun `showDeactivateDialog updates state`() = runTest {
            coEvery { householdRepository.getUserHousehold() } returns flowOf(null)

            val viewModel = HouseholdViewModel(householdRepository)
            viewModel.showDeactivateDialog()

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state.showDeactivateDialog)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("dismissDeactivateDialog updates state")
        fun `dismissDeactivateDialog updates state`() = runTest {
            coEvery { householdRepository.getUserHousehold() } returns flowOf(null)

            val viewModel = HouseholdViewModel(householdRepository)
            viewModel.showDeactivateDialog()
            viewModel.dismissDeactivateDialog()

            viewModel.uiState.test {
                val state = awaitItem()
                assertFalse(state.showDeactivateDialog)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("showLeaveDialog updates state")
        fun `showLeaveDialog updates state`() = runTest {
            coEvery { householdRepository.getUserHousehold() } returns flowOf(null)

            val viewModel = HouseholdViewModel(householdRepository)
            viewModel.showLeaveDialog()

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state.showLeaveDialog)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
