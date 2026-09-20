package com.timedensity.resonator.core

/**
 * Platform-neutral resonance engine. UI, audio and game progression stay outside this class.
 */
class ResonanceEngine(
    private val config: ResonanceConfig = ResonanceConfig()
) {
    private var targetFrequencyHz: Double? = null
    private var carrierFrequencyHz: Double = 0.0
    private var heldForMs: Long = 0L
    private var transformed: Boolean = false

    fun setTargetFrequency(frequencyHz: Double) {
        require(frequencyHz > 0.0) { "Target frequency must be greater than zero." }
        targetFrequencyHz = frequencyHz
        heldForMs = 0L
        transformed = false
    }

    /** Updates the carrier and advances the resonance hold timer. */
    fun update(carrierFrequencyHz: Double, elapsedMs: Long): ResonanceSnapshot {
        require(carrierFrequencyHz >= 0.0) { "Carrier frequency cannot be negative." }
        require(elapsedMs >= 0L) { "Elapsed time cannot be negative." }
        this.carrierFrequencyHz = carrierFrequencyHz

        val target = targetFrequencyHz
            ?: return snapshot(ResonancePhase.IDLE, 0.0, 0L)
        val distance = kotlin.math.abs(target - carrierFrequencyHz)
        val matched = distance <= config.toleranceHz
        val precision = if (matched) {
            (1.0 - distance / config.toleranceHz).coerceIn(0.0, 1.0)
        } else 0.0

        if (matched && !transformed) heldForMs += elapsedMs else if (!matched) heldForMs = 0L

        val phase = when {
            transformed -> ResonancePhase.TRANSFORMED
            matched && heldForMs >= config.holdDurationMs -> ResonancePhase.STABLE
            matched -> ResonancePhase.MATCHED
            else -> ResonancePhase.TUNING
        }
        return snapshot(phase, precision, heldForMs)
    }

    /** Host applications call this after handling a stable resonance event. */
    fun transform(): ResonanceSnapshot {
        check(targetFrequencyHz != null) { "A target frequency must be set before transformation." }
        check(heldForMs >= config.holdDurationMs) { "Resonance has not been held long enough." }
        transformed = true
        return update(carrierFrequencyHz, 0L)
    }

    fun reset() {
        targetFrequencyHz = null
        carrierFrequencyHz = 0.0
        heldForMs = 0L
        transformed = false
    }

    private fun snapshot(phase: ResonancePhase, precision: Double, heldMs: Long) =
        ResonanceSnapshot(targetFrequencyHz, carrierFrequencyHz, precision, heldMs, phase)
}
