package com.rasoiai.app.presentation.settings.screens

import app.cash.turbine.test
import com.rasoiai.domain.model.CuisineType
import com.rasoiai.domain.model.DayOfWeek
import com.rasoiai.domain.model.FamilyMember
import com.rasoiai.domain.model.MemberType
import com.rasoiai.domain.model.PrimaryDiet
import com.rasoiai.domain.model.SpecialDietaryNeed
import com.rasoiai.domain.model.SpiceLevel
import com.rasoiai.domain.model.User
import com.rasoiai.domain.model.UserPreferences
import com.rasoiai.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("FamilyMembersViewModel")
class FamilyMembersViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockSettingsRepository: SettingsRepository
    private lateinit var viewModel: FamilyMembersViewModel

    private val testFamilyMember = FamilyMember(
        id = "member-1",
        name = "Aarav",
        type = MemberType.CHILD,
        age = 8,
        specialNeeds = emptyList()
    )

    private val testFamilyMember2 = FamilyMember(
        id = "member-2",
        name = "Priya",
        type = MemberType.ADULT,
        age = 35,
        specialNeeds = listOf(SpecialDietaryNeed.DIABETIC)
    )

    private val testPreferences = UserPreferences(
        householdSize = 4,
        familyMembers = listOf(testFamilyMember, testFamilyMember2),
        primaryDiet = PrimaryDiet.VEGETARIAN,
        dietaryRestrictions = emptyList(),
        cuisinePreferences = listOf(CuisineType.NORTH),
        spiceLevel = SpiceLevel.MEDIUM,
        dislikedIngredients = emptyList(),
        weekdayCookingTimeMinutes = 30,
        weekendCookingTimeMinutes = 60,
        busyDays = listOf(DayOfWeek.MONDAY)
    )

    private val testUser = User(
        id = "user-1",
        email = "test@example.com",
        name = "Test User",
        profileImageUrl = null,
        isOnboarded = true,
        preferences = testPreferences
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockSettingsRepository = mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): FamilyMembersViewModel {
        return FamilyMembersViewModel(
            settingsRepository = mockSettingsRepository
        ).also { viewModel = it }
    }

    @Nested
    @DisplayName("Initialization")
    inner class Initialization {

        @Test
        @DisplayName("initial state is loading")
        fun `initial state is loading`() = runTest {
            every { mockSettingsRepository.getCurrentUser() } returns flowOf(testUser)

            val vm = createViewModel()

            vm.uiState.test {
                val state = awaitItem()
                assertTrue(state.isLoading)
            }
        }
    }

    @Nested
    @DisplayName("Sheet Management")
    inner class SheetManagement {

        @Test
        @DisplayName("showAddSheet sets showAddEditSheet to true")
        fun `showAddSheet sets showAddEditSheet to true`() = runTest {
            every { mockSettingsRepository.getCurrentUser() } returns flowOf(testUser)

            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.showAddSheet()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertTrue(state.showAddEditSheet)
                assertNull(state.editingMember)
            }
        }

        @Test
        @DisplayName("showEditSheet sets showAddEditSheet to true and editingMember")
        fun `showEditSheet sets showAddEditSheet to true and editingMember`() = runTest {
            every { mockSettingsRepository.getCurrentUser() } returns flowOf(testUser)

            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.showEditSheet(testFamilyMember)
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertTrue(state.showAddEditSheet)
                assertNotNull(state.editingMember)
                assertEquals("Aarav", state.editingMember?.name)
                assertEquals(MemberType.CHILD, state.editingMember?.type)
            }
        }

        @Test
        @DisplayName("dismissSheet sets showAddEditSheet to false")
        fun `dismissSheet sets showAddEditSheet to false`() = runTest {
            every { mockSettingsRepository.getCurrentUser() } returns flowOf(testUser)

            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.showAddSheet()
            testDispatcher.scheduler.advanceUntilIdle()
            vm.dismissSheet()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertFalse(state.showAddEditSheet)
                assertNull(state.editingMember)
            }
        }
    }

    @Nested
    @DisplayName("Delete Dialog")
    inner class DeleteDialog {

        @Test
        @DisplayName("showDeleteDialog sets showDeleteDialog to true and deletingMemberId")
        fun `showDeleteDialog sets showDeleteDialog to true and deletingMemberId`() = runTest {
            every { mockSettingsRepository.getCurrentUser() } returns flowOf(testUser)

            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.showDeleteDialog("member-1")
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertTrue(state.showDeleteDialog)
                assertEquals("member-1", state.deletingMemberId)
            }
        }

        @Test
        @DisplayName("dismissDeleteDialog sets showDeleteDialog to false")
        fun `dismissDeleteDialog sets showDeleteDialog to false`() = runTest {
            every { mockSettingsRepository.getCurrentUser() } returns flowOf(testUser)

            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.showDeleteDialog("member-1")
            testDispatcher.scheduler.advanceUntilIdle()
            vm.dismissDeleteDialog()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertFalse(state.showDeleteDialog)
                assertNull(state.deletingMemberId)
            }
        }
    }

    @Nested
    @DisplayName("saveMember")
    inner class SaveMember {

        @Test
        @DisplayName("saveMember calls repository addFamilyMember")
        fun `saveMember calls repository`() = runTest {
            every { mockSettingsRepository.getCurrentUser() } returns flowOf(testUser)
            coEvery { mockSettingsRepository.addFamilyMember(any()) } returns Result.success(Unit)

            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.saveMember(
                name = "Dadi",
                type = MemberType.SENIOR,
                age = 65,
                specialNeeds = listOf(SpecialDietaryNeed.DIABETIC)
            )
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { mockSettingsRepository.addFamilyMember(any()) }
        }
    }

    @Nested
    @DisplayName("deleteMember")
    inner class DeleteMember {

        @Test
        @DisplayName("deleteMember calls repository removeFamilyMember")
        fun `deleteMember calls repository`() = runTest {
            every { mockSettingsRepository.getCurrentUser() } returns flowOf(testUser)
            coEvery { mockSettingsRepository.removeFamilyMember(any()) } returns Result.success(Unit)

            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.showDeleteDialog("member-1")
            testDispatcher.scheduler.advanceUntilIdle()

            vm.deleteMember()
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { mockSettingsRepository.removeFamilyMember("member-1") }
        }
    }
}
