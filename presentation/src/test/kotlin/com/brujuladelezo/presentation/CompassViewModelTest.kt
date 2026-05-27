package com.brujuladelezo.presentation

import app.cash.turbine.test
import com.brujuladelezo.domain.model.CompassAccuracy
import com.brujuladelezo.domain.model.LondonDirection
import com.brujuladelezo.domain.usecase.ObserveLondonDirectionUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CompassViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val useCase: ObserveLondonDirectionUseCase = mockk()

    private fun buildViewModel(hasCompassSensor: Boolean = true) =
        CompassViewModel(useCase, mainDispatcherRule.dispatcher.let {
            object : com.brujuladelezo.core.DispatcherProvider {
                override val main = it
                override val io = it
                override val default = it
            }
        }, hasCompassSensor)

    @Test
    fun `initial state has no permission and is not loading`() {
        val vm = buildViewModel()
        val state = vm.uiState.value
        assertFalse(state.hasLocationPermission)
        assertFalse(state.isLoading)
    }

    @Test
    fun `onPermissionResult false does not start tracking`() = runTest {
        val vm = buildViewModel()
        vm.onPermissionResult(false)
        assertFalse(vm.uiState.value.hasLocationPermission)
    }

    @Test
    fun `onPermissionResult true updates state with direction`() = runTest {
        val direction = LondonDirection(42f, false, CompassAccuracy.ALTA)
        every { useCase.invoke() } returns flowOf(direction)

        val vm = buildViewModel()
        vm.uiState.test {
            awaitItem() // initial state

            vm.onPermissionResult(true)

            // Collect all intermediate states until arrowRotation is set
            val states = mutableListOf<CompassUiState>()
            while (true) {
                val item = awaitItem()
                states.add(item)
                if (item.arrowRotation == 42f) break
            }

            assertTrue("Some state should have permission=true", states.any { it.hasLocationPermission })
            assertEquals(42f, states.last().arrowRotation)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `hasCompassSensor false is reflected in state`() {
        val vm = buildViewModel(hasCompassSensor = false)
        assertFalse(vm.uiState.value.hasCompassSensor)
    }
}
