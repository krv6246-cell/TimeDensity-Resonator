package com.timedensity.game.engine

import android.util.Log
import com.timedensity.resonator.core.ResonanceConfig
import com.timedensity.resonator.core.ResonanceEngine
import com.timedensity.resonator.core.ResonancePhase
import com.timedensity.resonator.core.ResonanceSnapshot
import kotlin.math.abs
import kotlin.math.sign

class ResonatorGameAdapter {
    // 30 Hz entry tolerance, 800 ms hold
    private val core = ResonanceEngine(
        ResonanceConfig(
            toleranceHz = 30.0,
            holdDurationMs = 800L
        )
    )
    
    private var currentTarget: Float? = null
    private var wasMatched = false
    private var graceTimerMs = 0L
    private var softDropTimerMs = 1000L

    fun update(targetFreq: Float, carrierFreq: Float, dtSeconds: Float): ResonanceSnapshot {
        // FAIL_SAFE check
        if (targetFreq <= 0f || targetFreq.isNaN() || targetFreq.isInfinite()) {
            reset()
            return core.update(0.0, 0L)
        }

        // Set target if changed
        if (currentTarget != targetFreq) {
            Log.d(
                "AtomHunter",
                "adapter.setTarget: oldTarget=$currentTarget newTarget=$targetFreq " +
                    "wasMatched=$wasMatched graceTimerMs=$graceTimerMs softDropTimerMs=$softDropTimerMs"
            )
            core.setTargetFrequency(targetFreq.toDouble())
            currentTarget = targetFreq
            wasMatched = false
            graceTimerMs = 0L
            softDropTimerMs = 1000L
        }

        val safeCarrier = if (carrierFreq.isNaN() || carrierFreq.isInfinite() || carrierFreq < 0f) 0f else carrierFreq
        val rawDist = abs(safeCarrier - targetFreq)
        val dtMs = if (dtSeconds.isNaN() || dtSeconds.isInfinite() || dtSeconds < 0f) 0L else (dtSeconds * 1000f).toLong()

        var effectiveCarrier = safeCarrier
        var effectiveDtMs = dtMs

        // Magnetic snap to target when very close (±10 Hz)
        if (rawDist <= 12f) {
            effectiveCarrier = targetFreq
        }

        val inEntryZone = rawDist <= 30f
        val inExitZone = rawDist <= 50f // Extended exit tolerance

        if (wasMatched && inExitZone) {
            // Hysteresis active: clamp distance to 29.9 so Core sees it as MATCHED
            effectiveCarrier = targetFreq + sign(safeCarrier - targetFreq) * minOf(rawDist, 29.9f)
            graceTimerMs = 0L
            softDropTimerMs = 0L
        } else if (wasMatched) {
            // Out of bounds, apply grace period
            if (graceTimerMs < 400L) { // 400ms grace period
                graceTimerMs += dtMs
                effectiveCarrier = targetFreq + 29.9f // Fake matched
                effectiveDtMs = 0L // Pause progress, don't reset
            } else {
                // Grace period over, start soft drop
                wasMatched = false
                softDropTimerMs = 0L
            }
        } else if (inEntryZone) {
            // New valid match
            wasMatched = true
            graceTimerMs = 0L
            softDropTimerMs = 0L
        }

        var snapshot = core.update(effectiveCarrier.toDouble(), effectiveDtMs)

        Log.d(
            "AtomHunter",
            "adapter.update: targetFreq=$targetFreq effectiveCarrier=$effectiveCarrier " +
                "phase=${snapshot.phase} heldForMs=${snapshot.heldForMs} " +
                "precision=${snapshot.precision} wasMatched=$wasMatched currentTarget=$currentTarget"
        )

        // Soft drop override: visually step down to MATCHED instead of jumping to TUNING instantly
        if (!wasMatched && (snapshot.phase == ResonancePhase.TUNING || snapshot.phase == ResonancePhase.IDLE)) {
            if (softDropTimerMs < 300L) { // 300ms soft drop visualization
                softDropTimerMs += dtMs
                snapshot = snapshot.copy(phase = ResonancePhase.MATCHED, precision = 5.0)
            }
        }

        // Update matching state based on core return
        if (snapshot.phase == ResonancePhase.MATCHED || snapshot.phase == ResonancePhase.STABLE || snapshot.phase == ResonancePhase.TRANSFORMED) {
            wasMatched = true
        } else if (softDropTimerMs >= 300L) {
            wasMatched = false
        }

        return snapshot
    }

    fun transform(): ResonanceSnapshot {
        Log.d(
            "AtomHunter",
            "adapter.transform BEFORE: currentTarget=$currentTarget wasMatched=$wasMatched"
        )
        val result = core.transform()
        Log.d(
            "AtomHunter",
            "adapter.transform AFTER: phase=${result.phase} heldForMs=${result.heldForMs}"
        )
        return result
    }

    fun reset() {
        Log.d(
            "AtomHunter",
            "adapter.reset: currentTarget=$currentTarget wasMatched=$wasMatched " +
                "graceTimerMs=$graceTimerMs softDropTimerMs=$softDropTimerMs"
        )
        core.reset()
        currentTarget = null
        wasMatched = false
        graceTimerMs = 0L
        softDropTimerMs = 1000L
    }
}
