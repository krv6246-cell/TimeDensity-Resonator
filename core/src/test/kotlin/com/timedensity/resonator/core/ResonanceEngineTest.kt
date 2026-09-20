package com.timedensity.resonator.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ResonanceEngineTest {
    private val engine = ResonanceEngine(ResonanceConfig(toleranceHz = 5.0, holdDurationMs = 1_000L))

    @Test
    fun `matching frequency becomes stable after hold duration`() {
        engine.setTargetFrequency(659.0)
        assertEquals(ResonancePhase.MATCHED, engine.update(657.0, 500).phase)
        val stable = engine.update(659.0, 500)
        assertEquals(ResonancePhase.STABLE, stable.phase)
        assertTrue(stable.precision > 0.99)
    }

    @Test
    fun `leaving tolerance resets the hold`() {
        engine.setTargetFrequency(220.0)
        engine.update(220.0, 900)
        val tuning = engine.update(240.0, 100)
        assertEquals(ResonancePhase.TUNING, tuning.phase)
        assertEquals(0L, tuning.heldForMs)
    }

    @Test
    fun `transformation requires stable resonance`() {
        engine.setTargetFrequency(110.0)
        engine.update(110.0, 1_000)
        assertEquals(ResonancePhase.TRANSFORMED, engine.transform().phase)
    }

    @Test
    fun `invalid target is rejected`() {
        assertFailsWith<IllegalArgumentException> { engine.setTargetFrequency(0.0) }
    }
}
