package com.cgaraydev.seismicexplorer.presentation.map

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapCameraCommandCoordinatorTest {

    @Test
    fun lastRequestWins_whenFitIsReplacedByUserLocation() {
        val commands = MapCameraCommandCoordinator()
        commands.request(MapCameraCommandKind.FIT_EVENTS)
        commands.request(MapCameraCommandKind.CENTER_USER)

        assertFalse(commands.isPending(MapCameraCommandKind.FIT_EVENTS))
        assertFalse(commands.consume(MapCameraCommandKind.FIT_EVENTS))
        assertTrue(commands.consume(MapCameraCommandKind.CENTER_USER))
    }

    @Test
    fun resetOverridesPreviousUserCenter() {
        val commands = MapCameraCommandCoordinator()
        commands.request(MapCameraCommandKind.CENTER_USER)
        commands.request(MapCameraCommandKind.RESET)

        assertFalse(commands.consume(MapCameraCommandKind.CENTER_USER))
        assertTrue(commands.consume(MapCameraCommandKind.RESET))
    }

    @Test
    fun fitOverridesPreviousReset() {
        val commands = MapCameraCommandCoordinator()
        commands.request(MapCameraCommandKind.RESET)
        commands.request(MapCameraCommandKind.FIT_EVENTS)

        assertFalse(commands.consume(MapCameraCommandKind.RESET))
        assertTrue(commands.consume(MapCameraCommandKind.FIT_EVENTS))
    }

    @Test
    fun consumedRequestIsNotRepeated() {
        val commands = MapCameraCommandCoordinator()
        commands.request(MapCameraCommandKind.CENTER_USER)

        assertTrue(commands.consume(MapCameraCommandKind.CENTER_USER))
        assertFalse(commands.consume(MapCameraCommandKind.CENTER_USER))
        assertFalse(commands.isPending(MapCameraCommandKind.CENTER_USER))
    }

    @Test
    fun clearDiscardsPendingCameraRequest() {
        val commands = MapCameraCommandCoordinator()
        commands.request(MapCameraCommandKind.FIT_EVENTS)
        commands.clear()

        assertFalse(commands.consume(MapCameraCommandKind.FIT_EVENTS))
    }
}
