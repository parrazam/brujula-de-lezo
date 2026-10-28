package com.brujuladelezo.presentation

import app.cash.turbine.test
import com.brujuladelezo.core.DispatcherProvider
import com.brujuladelezo.domain.model.CardinalDirection
import com.brujuladelezo.domain.model.CompassAccuracy
import com.brujuladelezo.domain.model.LondonDirection
import com.brujuladelezo.domain.usecase.ObserveLocationEnabledUseCase
import com.brujuladelezo.domain.usecase.ObserveLondonDirectionUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CompassViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val directionUseCase: ObserveLondonDirectionUseCase = mockk()
    private val locationEnabledUseCase: ObserveLocationEnabledUseCase = mockk {
        every { this@mockk.invoke() } returns flowOf(true)
    }

    private val testDispatchers = object : DispatcherProvider {
        override val main = mainDispatcherRule.dispatcher
        override val io = mainDispatcherRule.dispatcher
        override val default = mainDispatcherRule.dispatcher
    }

    private fun buildViewModel(hasCompassSensor: Boolean = true) =
        CompassViewModel(directionUseCase, locationEnabledUseCase, testDispatchers, hasCompassSensor)

    private val direction = LondonDirection(
        arrowRotationDegrees = 42f,
        isPointingAtLondon = false,
        accuracy = CompassAccuracy.ALTA,
        bearingDegrees = 11.4f,
        distanceKm = 1264.0,
        turnDegrees = -30f,
    )

    @Test
    fun `initial state has no permission and is not loading`() {
        val state = buildViewModel().uiState.value
        assertFalse(state.hasLocationPermission)
        assertFalse(state.isLoading)
    }

    @Test
    fun `onPermissionResult false does not start tracking`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()
        vm.uiState.test {
            vm.onPermissionResult(false)
            assertFalse(awaitItem().hasLocationPermission)
            cancelAndIgnoreRemainingEvents()
        }
        verify(exactly = 0) { directionUseCase.invoke() }
    }

    @Test
    fun `onPermissionResult true updates state with direction`() = runTest(mainDispatcherRule.dispatcher) {
        every { directionUseCase.invoke() } returns flowOf(direction)
        val vm = buildViewModel()
        vm.uiState.test {
            awaitItem() // inicial
            vm.onPermissionResult(true)
            val last = expectMostRecentItem()
            assertTrue(last.hasLocationPermission)
            assertFalse(last.isLoading)
            assertEquals(42f, last.arrowRotation)
            assertEquals(11.4f, last.bearingDegrees)
            assertEquals(CardinalDirection.NNE, last.cardinal)
            assertEquals(1264.0, last.distanceKm, 0.0)
            assertEquals(-30f, last.turnDegrees)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `hasCompassSensor false is reflected in state and never tracks`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel(hasCompassSensor = false)
        vm.uiState.test {
            vm.onPermissionResult(true)
            assertFalse(expectMostRecentItem().hasCompassSensor)
            cancelAndIgnoreRemainingEvents()
        }
        verify(exactly = 0) { directionUseCase.invoke() }
    }

    @Test
    fun `sin suscriptores los sensores se sueltan tras el margen`() = runTest(mainDispatcherRule.dispatcher) {
        val sensors = MutableSharedFlow<LondonDirection>()
        every { directionUseCase.invoke() } returns sensors
        val vm = buildViewModel()
        vm.onPermissionResult(true)

        val collector = launch { vm.uiState.collect {} }
        runCurrent()
        assertEquals(1, sensors.subscriptionCount.value)

        collector.cancel()
        advanceTimeBy(STOP_TIMEOUT_MILLIS - 1)
        runCurrent()
        assertEquals("Un giro de pantalla no debe soltarlos", 1, sensors.subscriptionCount.value)

        advanceTimeBy(2)
        runCurrent()
        assertEquals(0, sensors.subscriptionCount.value)
    }

    @Test
    fun `sin ubicacion y sin primer rumbo se pide activarla`() = runTest(mainDispatcherRule.dispatcher) {
        every { directionUseCase.invoke() } returns MutableSharedFlow()
        every { locationEnabledUseCase.invoke() } returns flowOf(false)
        val vm = buildViewModel()
        vm.uiState.test {
            vm.onPermissionResult(true)
            val state = expectMostRecentItem()
            assertTrue(state.isLoading)
            assertTrue(state.showLocationDisabled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `si la ubicacion se apaga con un rumbo ya calculado se sigue mostrando la brujula`() =
        runTest(mainDispatcherRule.dispatcher) {
            val enabled = MutableStateFlow(true)
            every { directionUseCase.invoke() } returns flowOf(direction)
            every { locationEnabledUseCase.invoke() } returns enabled
            val vm = buildViewModel()
            vm.uiState.test {
                vm.onPermissionResult(true)
                enabled.value = false
                val state = expectMostRecentItem()
                assertFalse(state.isLocationEnabled)
                assertFalse(state.showLocationDisabled)
                assertEquals(42f, state.arrowRotation)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `un fallo del sensor no muestra una aguja falsa y se puede reintentar`() =
        runTest(mainDispatcherRule.dispatcher) {
            var calls = 0
            every { directionUseCase.invoke() } answers {
                calls++
                if (calls == 1) failing() else flowOf(direction)
            }
            val vm = buildViewModel()
            vm.uiState.test {
                vm.onPermissionResult(true)
                val failed = expectMostRecentItem()
                assertTrue(failed.trackingFailed)
                assertFalse(failed.isLoading)

                vm.retry()
                val recovered = expectMostRecentItem()
                assertFalse(recovered.trackingFailed)
                assertEquals(42f, recovered.arrowRotation)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun failing(): Flow<LondonDirection> = flow { throw SecurityException("permiso revocado") }
}
