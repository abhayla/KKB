package com.rasoiai.app.presentation.settings

import app.cash.turbine.test
import com.rasoiai.app.presentation.settings.viewmodels.JoinHouseholdNavigationEvent
import com.rasoiai.app.presentation.settings.viewmodels.JoinHouseholdViewModel
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
class JoinHouseholdViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var householdRepository: HouseholdRepository

    private val testHouseholdDetail = HouseholdDetail(
        household = Household(
            id = "household-1",
            name = "Verma Family",
            inviteCode = "VERMA123",
            ownerId = "owner-1",
            maxMembers = 8,
            memberCount = 1,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        ),
        members = listOf(
            HouseholdMember(
                id = "member-1",
                userId = "owner-1",
                familyMemberId = null,
                name = "Vikram Verma",
                role = HouseholdRole.OWNER,
                canEditSharedPlan = true,
                isTemporary = false,
                joinDate = LocalDateTime.now(),
                status = MemberStatus.ACTIVE
            )
        )
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        householdRepository = mockk(relaxed = true)
        coEvery { householdRepository.getUserHousehold() } returns flowOf(null)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ---------------------------------------------------------------------------
    // Initial state
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("Initial State")
    inner class InitialState {

        @Test
        @DisplayName("initial state is correct")
        fun `initial state is correct`() = runTest {
            val viewModel = JoinHouseholdViewModel(householdRepository)

            viewModel.uiState.test {
                val state = awaitItem()
                assertFalse(state.isLoading)
                assertNull(state.error)
                assertEquals("", state.inviteCode)
                assertFalse(state.joinSuccess)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    // ---------------------------------------------------------------------------
    // setInviteCode
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("setInviteCode")
    inner class SetInviteCode {

        @Test
        @DisplayName("setInviteCode updates state")
        fun `setInviteCode updates state`() = runTest {
            val viewModel = JoinHouseholdViewModel(householdRepository)
            viewModel.onCodeChanged("VERMA123")

            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals("VERMA123", state.inviteCode)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("setInviteCode clears error")
        fun `setInviteCode clears error`() = runTest {
            val viewModel = JoinHouseholdViewModel(householdRepository)
            // Trigger an error first
            viewModel.joinHousehold() // blank code error
            viewModel.onCodeChanged("VERMA123")

            viewModel.uiState.test {
                val state = awaitItem()
                assertNull(state.error)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    // ---------------------------------------------------------------------------
    // joinHousehold
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("joinHousehold")
    inner class JoinHousehold {

        @Test
        @DisplayName("joinHousehold with empty code shows error")
        fun `joinHousehold with empty code shows error`() = runTest {
            val viewModel = JoinHouseholdViewModel(householdRepository)

            viewModel.joinHousehold()

            viewModel.uiState.test {
                val state = awaitItem()
                assertFalse(state.isLoading)
                assertEquals("Please enter an invite code", state.error)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("joinHousehold success updates state and emits navigation event")
        fun `joinHousehold success updates state and emits navigation event`() = runTest {
            coEvery { householdRepository.joinHousehold("VERMA123") } returns
                Result.success(testHouseholdDetail)

            val viewModel = JoinHouseholdViewModel(householdRepository)
            viewModel.onCodeChanged("VERMA123")

            viewModel.navigationEvent.test {
                viewModel.joinHousehold()
                advanceUntilIdle()

                val event = awaitItem()
                assertTrue(event is JoinHouseholdNavigationEvent.NavigateToHousehold)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("joinHousehold success sets joinSuccess flag")
        fun `joinHousehold success sets joinSuccess flag`() = runTest {
            coEvery { householdRepository.joinHousehold("VERMA123") } returns
                Result.success(testHouseholdDetail)

            val viewModel = JoinHouseholdViewModel(householdRepository)
            viewModel.onCodeChanged("VERMA123")
            viewModel.joinHousehold()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                assertFalse(state.isLoading)
                assertTrue(state.joinSuccess)
                assertNull(state.error)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("joinHousehold failure shows error")
        fun `joinHousehold failure shows error`() = runTest {
            coEvery { householdRepository.joinHousehold(any()) } returns
                Result.failure(Exception("Something went wrong"))

            val viewModel = JoinHouseholdViewModel(householdRepository)
            viewModel.onCodeChanged("BADCODE")
            viewModel.joinHousehold()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                assertFalse(state.isLoading)
                assertEquals("Something went wrong", state.error)
                assertFalse(state.joinSuccess)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("joinHousehold expired code shows specific error")
        fun `joinHousehold expired code shows specific error`() = runTest {
            coEvery { householdRepository.joinHousehold(any()) } returns
                Result.failure(Exception("Code has expired"))

            val viewModel = JoinHouseholdViewModel(householdRepository)
            viewModel.onCodeChanged("OLDCODE1")
            viewModel.joinHousehold()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                assertFalse(state.isLoading)
                assertEquals("This invite code has expired", state.error)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        @DisplayName("joinHousehold 404 error shows invalid code message")
        fun `joinHousehold 404 error shows invalid code message`() = runTest {
            coEvery { householdRepository.joinHousehold(any()) } returns
                Result.failure(Exception("404 Not Found"))

            val viewModel = JoinHouseholdViewModel(householdRepository)
            viewModel.onCodeChanged("UNKNOWN1")
            viewModel.joinHousehold()
            advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                assertFalse(state.isLoading)
                assertEquals("Invalid invite code", state.error)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
