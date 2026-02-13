package com.rasoiai.app.presentation.settings.screens

import app.cash.turbine.test
import com.rasoiai.domain.model.AppSettings
import com.rasoiai.domain.model.SmallMeasurementUnit
import com.rasoiai.domain.model.VolumeUnit
import com.rasoiai.domain.model.WeightUnit
import com.rasoiai.domain.repository.SettingsRepository
import io.mockk.coEvery
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
@DisplayName("UnitsViewModel")
class UnitsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockSettingsRepository: SettingsRepository
    private lateinit var viewModel: UnitsViewModel

    private val testSettings = AppSettings(
        volumeUnit = VolumeUnit.INDIAN,
        weightUnit = WeightUnit.METRIC,
        smallMeasurementUnit = SmallMeasurementUnit.INDIAN
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockSettingsRepository = mockk(relaxed = true)
        every { mockSettingsRepository.getAppSettings() } returns flowOf(testSettings)
        coEvery { mockSettingsRepository.updateAppSettings(any()) } returns Result.success(Unit)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): UnitsViewModel {
        return UnitsViewModel(mockSettingsRepository).also { viewModel = it }
    }

    @Nested
    @DisplayName("Initial State")
    inner class InitialState {

        @Test
        @DisplayName("initial state is loading")
        fun `initial state is loading`() = runTest {
            val vm = createViewModel()
            vm.uiState.test {
                val state = awaitItem()
                assertTrue(state.isLoading)
            }
        }
    }

    @Nested
    @DisplayName("Loading")
    inner class Loading {

        @Test
        @DisplayName("after loading, units populated")
        fun `after loading, units populated`() = runTest {
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertFalse(state.isLoading)
                assertNotNull(state.volumeUnit)
                assertNotNull(state.weightUnit)
                assertNotNull(state.smallMeasurementUnit)
            }
        }
    }

    @Nested
    @DisplayName("Update Units")
    inner class UpdateUnits {

        @Test
        @DisplayName("updateVolumeUnit changes volume unit")
        fun `updateVolumeUnit changes volume unit`() = runTest {
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.updateVolumeUnit(VolumeUnit.METRIC)
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertEquals(VolumeUnit.METRIC, state.volumeUnit)
            }
        }

        @Test
        @DisplayName("updateWeightUnit changes weight unit")
        fun `updateWeightUnit changes weight unit`() = runTest {
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.updateWeightUnit(WeightUnit.US)
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertEquals(WeightUnit.US, state.weightUnit)
            }
        }

        @Test
        @DisplayName("updateSmallMeasurementUnit changes measurement unit")
        fun `updateSmallMeasurementUnit changes measurement unit`() = runTest {
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.updateSmallMeasurementUnit(SmallMeasurementUnit.METRIC)
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertEquals(SmallMeasurementUnit.METRIC, state.smallMeasurementUnit)
            }
        }
    }

    @Nested
    @DisplayName("Save")
    inner class Save {

        @Test
        @DisplayName("save success sets saveSuccess true")
        fun `save success sets saveSuccess true`() = runTest {
            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.save()
            testDispatcher.scheduler.advanceUntilIdle()

            vm.uiState.test {
                val state = awaitItem()
                assertTrue(state.saveSuccess)
            }
        }
    }
}
