package com.timedensity.game.music

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class CaptureEvent(
    val elementSymbol: String,
    val frequencyHz: Float,
    val capturedAtMs: Long,
    val precision: Float,
    val heldForMs: Long,
    val resonancePhase: String = "TRANSFORMED"
)

data class CaptureSession(
    val seed: Long,
    val events: List<CaptureEvent>
) {
    fun toJson(): String = buildString {
        append("{\"seed\":").append(seed).append(",\"events\":[")
        events.forEachIndexed { index, event ->
            if (index > 0) append(',')
            append("{\"element\":\"").append(event.elementSymbol.escapeJson())
                .append("\",\"frequencyHz\":").append(event.frequencyHz)
                .append(",\"capturedAtMs\":").append(event.capturedAtMs)
                .append(",\"precision\":").append(event.precision)
                .append(",\"heldForMs\":").append(event.heldForMs)
                .append(",\"phase\":\"").append(event.resonancePhase.escapeJson())
                .append("\"}")
        }
        append("]}")
    }
}

data class TrackNote(
    val eventIndex: Int,
    val elementSymbol: String,
    val fundamentalHz: Float,
    val bassHz: Float,
    val onsetMs: Long,
    val durationMs: Long,
    val precision: Float
)

object TrackComposer {
    const val TEMPO_BPM = 120
    const val GRID_MS = 250L

    fun compose(session: CaptureSession): List<TrackNote> {
        var previousOnset = -GRID_MS
        val firstCapture = session.events.firstOrNull()?.capturedAtMs ?: return emptyList()
        return session.events.mapIndexed { index, event ->
            val elapsed = (event.capturedAtMs - firstCapture).coerceAtLeast(0L)
            val quantized = ((elapsed + GRID_MS / 2) / GRID_MS) * GRID_MS
            val onset = maxOf(quantized, previousOnset + GRID_MS)
            previousOnset = onset
            TrackNote(
                eventIndex = index,
                elementSymbol = event.elementSymbol,
                fundamentalHz = event.frequencyHz,
                bassHz = event.frequencyHz / 2f,
                onsetMs = onset,
                durationMs = 430L,
                precision = event.precision.coerceIn(0f, 1f)
            )
        }
    }
}

data class GalaxyStar(
    val eventIndex: Int,
    val elementSymbol: String,
    val x: Float,
    val y: Float,
    val size: Float,
    val opacity: Float,
    val isAtom: Boolean
)

object UniverseLayout {
    fun generate(session: CaptureSession, maxParticles: Int = 2_200): List<GalaxyStar> {
        if (session.events.isEmpty() || maxParticles <= 0) return emptyList()
        val particlesPerAtom = (maxParticles / session.events.size).coerceAtLeast(1)
        return buildList {
            session.events.forEachIndexed { index, event ->
                val baseRadius = 0.28f + 0.60f * sqrt((index + 1f) / (session.events.size + 1f))
                val seed = session.seed + index * 509L
                val capturePhase = (event.capturedAtMs % 60_000L) / 60_000f
                val atomAngle = index * GOLDEN_ANGLE + capturePhase * (2f * PI.toFloat())
                add(
                    GalaxyStar(
                        eventIndex = index,
                        elementSymbol = event.elementSymbol,
                        x = 0.5f + cos(atomAngle) * baseRadius * 0.43f,
                        y = 0.48f + sin(atomAngle) * baseRadius * 0.30f,
                        size = 2.5f + event.precision.coerceIn(0f, 1f) * 2.5f,
                        opacity = 1f,
                        isAtom = true
                    )
                )
                for (particle in 1 until particlesPerAtom) {
                    val randomA = unitNoise(seed + particle * 2L)
                    val randomB = unitNoise(seed + particle * 2L + 1L)
                    val radius = (baseRadius + (randomA - 0.5f) * 0.55f).coerceIn(0.05f, 1f)
                    val angle = index * GOLDEN_ANGLE + radius * 4.7f + (randomB - 0.5f) * 0.6f
                    add(
                        GalaxyStar(
                            eventIndex = index,
                            elementSymbol = event.elementSymbol,
                            x = 0.5f + cos(angle) * radius * 0.43f,
                            y = 0.48f + sin(angle) * radius * 0.30f,
                            size = 0.5f + randomA * 1.2f,
                            opacity = 0.2f + randomB * 0.6f,
                            isAtom = false
                        )
                    )
                }
            }
        }
    }

    private fun unitNoise(value: Long): Float {
        val sample = sin(value * 127.1 + 311.7) * 43_758.5453
        return (sample - kotlin.math.floor(sample)).toFloat()
    }

    private const val GOLDEN_ANGLE = 2.3999632f
}

private fun String.escapeJson(): String = replace("\\", "\\\\").replace("\"", "\\\"")
