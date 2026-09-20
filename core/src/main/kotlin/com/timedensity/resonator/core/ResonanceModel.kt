package com.timedensity.resonator.core

data class ResonanceConfig(
    val toleranceHz: Double = 5.0,
    val holdDurationMs: Long = 1_200L
) {
    init {
        require(toleranceHz > 0.0) { "Tolerance must be greater than zero." }
        require(holdDurationMs > 0L) { "Hold duration must be greater than zero." }
    }
}

enum class ResonancePhase {
    IDLE,
    TUNING,
    MATCHED,
    STABLE,
    TRANSFORMED
}

data class ResonanceSnapshot(
    val targetFrequencyHz: Double?,
    val carrierFrequencyHz: Double,
    val precision: Double,
    val heldForMs: Long,
    val phase: ResonancePhase
)
