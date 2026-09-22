package com.timedensity.resonator.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReceiverStartupTest {
    private val startup = ReceiverStartup()

    @Test
    fun `startup begins in cosmic noise without a detected frequency`() {
        assertEquals(
            ReceiverSnapshot(ReceiverPhase.COSMIC_NOISE, null, "COSMIC NOISE"),
            startup.snapshot()
        )
    }

    @Test
    fun `startup reveals and locks the hydrogen line before coming online`() {
        assertNull(startup.advance().frequencyHz)
        assertEquals(ReceiverPhase.SIGNAL_DETECTED, startup.advance().phase)

        val hydrogen = startup.advance()
        assertEquals(ReceiverPhase.HYDROGEN_LINE_FOUND, hydrogen.phase)
        assertEquals(ReceiverStartup.HYDROGEN_LINE_FREQUENCY_HZ, hydrogen.frequencyHz)

        assertEquals(ReceiverPhase.FREQUENCY_IDENTIFIED, startup.advance().phase)
        assertEquals(ReceiverPhase.SIGNAL_LOCKED, startup.advance().phase)
        assertEquals(ReceiverPhase.RESONATOR_ONLINE, startup.advance().phase)
    }

    @Test
    fun `online state is stable until reset`() {
        repeat(6) { startup.advance() }
        assertEquals(ReceiverPhase.RESONATOR_ONLINE, startup.advance().phase)
        startup.reset()
        assertEquals(ReceiverPhase.COSMIC_NOISE, startup.snapshot().phase)
    }
}
