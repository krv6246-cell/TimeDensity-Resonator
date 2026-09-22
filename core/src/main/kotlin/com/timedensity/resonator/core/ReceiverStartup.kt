package com.timedensity.resonator.core

/**
 * Receiver startup sequence shown before the host begins frequency capture.
 * The sequence is intentionally advanced by the host so UI timing remains outside the core.
 */
enum class ReceiverPhase {
    COSMIC_NOISE,
    SEARCHING,
    SIGNAL_DETECTED,
    HYDROGEN_LINE_FOUND,
    FREQUENCY_IDENTIFIED,
    SIGNAL_LOCKED,
    RESONATOR_ONLINE
}

data class ReceiverSnapshot(
    val phase: ReceiverPhase,
    val frequencyHz: Double? = null,
    val displayText: String
)

class ReceiverStartup(
    val hydrogenLineFrequencyHz: Double = HYDROGEN_LINE_FREQUENCY_HZ
) {
    init {
        require(hydrogenLineFrequencyHz > 0.0) {
            "Hydrogen line frequency must be greater than zero."
        }
    }

    private var phase = ReceiverPhase.COSMIC_NOISE

    fun snapshot(): ReceiverSnapshot = snapshotFor(phase)

    /** Advances one receiver state. Repeated calls after online remain online. */
    fun advance(): ReceiverSnapshot {
        phase = when (phase) {
            ReceiverPhase.COSMIC_NOISE -> ReceiverPhase.SEARCHING
            ReceiverPhase.SEARCHING -> ReceiverPhase.SIGNAL_DETECTED
            ReceiverPhase.SIGNAL_DETECTED -> ReceiverPhase.HYDROGEN_LINE_FOUND
            ReceiverPhase.HYDROGEN_LINE_FOUND -> ReceiverPhase.FREQUENCY_IDENTIFIED
            ReceiverPhase.FREQUENCY_IDENTIFIED -> ReceiverPhase.SIGNAL_LOCKED
            ReceiverPhase.SIGNAL_LOCKED -> ReceiverPhase.RESONATOR_ONLINE
            ReceiverPhase.RESONATOR_ONLINE -> ReceiverPhase.RESONATOR_ONLINE
        }
        return snapshot()
    }

    fun reset() {
        phase = ReceiverPhase.COSMIC_NOISE
    }

    private fun snapshotFor(currentPhase: ReceiverPhase): ReceiverSnapshot {
        val frequency = if (currentPhase >= ReceiverPhase.HYDROGEN_LINE_FOUND) {
            hydrogenLineFrequencyHz
        } else {
            null
        }
        val text = when (currentPhase) {
            ReceiverPhase.COSMIC_NOISE -> "COSMIC NOISE"
            ReceiverPhase.SEARCHING -> "SEARCHING"
            ReceiverPhase.SIGNAL_DETECTED -> "SIGNAL DETECTED"
            ReceiverPhase.HYDROGEN_LINE_FOUND -> "HYDROGEN LINE FOUND"
            ReceiverPhase.FREQUENCY_IDENTIFIED -> "1420.40575 MHz"
            ReceiverPhase.SIGNAL_LOCKED -> "SIGNAL LOCKED"
            ReceiverPhase.RESONATOR_ONLINE -> "RESONATOR ONLINE"
        }
        return ReceiverSnapshot(currentPhase, frequency, text)
    }

    companion object {
        /** Neutral-hydrogen 21 cm line used as the opening receiver signal. */
        const val HYDROGEN_LINE_FREQUENCY_HZ: Double = 1_420_405_750.0
        const val HYDROGEN_LINE_FREQUENCY_MHZ: Double = 1_420.40575
    }
}
