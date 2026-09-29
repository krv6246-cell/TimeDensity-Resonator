package com.timedensity.game.audio

import java.util.concurrent.CopyOnWriteArrayList
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.timedensity.game.model.Element
import kotlin.math.sin

class AudioEngine {
    private val sampleRate = 44100
    private var isPlaying = false
    private var audioTrack: AudioTrack? = null
    private var thread: Thread? = null

    private var carrierFreq = 400f
    private var targetFreq = 0f
    private var volume = 0.5f

    // Tone generators - thread-safe collection
    private val activeTones = CopyOnWriteArrayList<Float>()

    // Melody recording and playback
    private val capturedSequence = CopyOnWriteArrayList<Float>()
    @Volatile
    private var isMelodyPlaying = false
    private var melodyDurationMs = 600L
    private var melodyStartedAtMs = 0L

    fun start() {
        if (isPlaying) return
        isPlaying = true

        val minSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()

        thread = Thread {
            val buffer = ShortArray(minSize)
            var angleCarrier = 0.0
            var angleTarget = 0.0
            var angleDrone = 0.0
            var melodyPhase = 0.0

            while (isPlaying) {
                for (i in buffer.indices) {
                    val droneSample = sin(angleDrone) * 0.1 // 50 Hz drone
                    val carrierSample = sin(angleCarrier) * 0.2
                    val targetSample = if (targetFreq > 0) sin(angleTarget) * 0.2 else 0.0
                    
                    var sum = droneSample + carrierSample + targetSample
                    
                    // Add active stabilized tones (safe iteration)
                    val tonesSnapshot = activeTones.toList()
                    for (freq in tonesSnapshot) {
                        val phase = (angleCarrier / carrierFreq) * freq // simplistic phase
                        sum += sin(phase) * 0.1
                    }

                    // Melody playback with proper note cycling
                    if (isMelodyPlaying && capturedSequence.isNotEmpty()) {
                        val elapsed = System.currentTimeMillis() - melodyStartedAtMs
                        val totalDuration = capturedSequence.size * melodyDurationMs

                        if (elapsed >= totalDuration) {
                            // Melody finished
                            isMelodyPlaying = false
                            Log.d(
                                "AtomHunter",
                                "Melody playback finished: totalDuration=$totalDuration elapsed=$elapsed"
                            )
                        } else {
                            // Determine current note and phase
                            val noteIndex = (elapsed / melodyDurationMs).toInt()
                                .coerceIn(0, capturedSequence.size - 1)
                            val freq = capturedSequence[noteIndex]
                            val notePhase = elapsed % melodyDurationMs
                            val envelope = computeEnvelope(notePhase, melodyDurationMs)

                            sum += sin(melodyPhase) * 0.25 * envelope
                            melodyPhase += 2.0 * Math.PI * freq / sampleRate
                        }
                    }

                    // Soft clip
                    sum = sum.coerceIn(-1.0, 1.0)
                    buffer[i] = (sum * Short.MAX_VALUE * volume).toInt().toShort()

                    angleDrone += 2.0 * Math.PI * 50.0 / sampleRate
                    angleCarrier += 2.0 * Math.PI * carrierFreq / sampleRate
                    angleTarget += 2.0 * Math.PI * targetFreq / sampleRate
                }
                audioTrack?.write(buffer, 0, buffer.size)
            }
        }
        thread?.start()
    }

    private fun computeEnvelope(notePhaseMs: Long, noteDurationMs: Long): Float {
        val attackMs = 50L
        val decayMs = 90L
        val releaseMs = 90L
        val sustainLevel = 0.82f

        return when {
            notePhaseMs < attackMs -> {
                (notePhaseMs.toFloat() / attackMs.toFloat()).coerceIn(0f, 1f)
            }
            notePhaseMs < attackMs + decayMs -> {
                val t = (notePhaseMs - attackMs).toFloat() / decayMs.toFloat()
                (1f - t * (1f - sustainLevel)).coerceIn(0f, 1f)
            }
            notePhaseMs < noteDurationMs - releaseMs -> sustainLevel
            else -> {
                val t = ((noteDurationMs - notePhaseMs).toFloat() / releaseMs.toFloat()).coerceIn(0f, 1f)
                (sustainLevel * t).coerceIn(0f, 1f)
            }
        }
    }

    fun setCarrierFrequency(freq: Float) {
        carrierFreq = freq
    }

    fun setTargetFrequency(freq: Float) {
        targetFreq = freq
    }
    
    fun playElementTone(element: Element) {
        // Just add to active tones for now
        activeTones.add(element.frequency)
    }
    
    fun clearTones() {
        activeTones.clear()
    }

    fun recordElementCapture(element: Element) {
        capturedSequence.add(element.frequency)
        Log.d(
            "AtomHunter",
            "recordElementCapture: symbol=${element.symbol} freq=${element.frequency} " +
                "sequenceSize=${capturedSequence.size}"
        )
    }

    fun clearMelodySequence() {
        capturedSequence.clear()
        isMelodyPlaying = false
        melodyStartedAtMs = 0L
        Log.d("AtomHunter", "clearMelodySequence")
    }

    fun getCapturedSequence(): List<Float> = capturedSequence.toList()

    fun startMelodyPlayback(noteDurationMs: Long = 600L) {
        if (capturedSequence.isEmpty()) {
            Log.d("AtomHunter", "startMelodyPlayback: empty captured sequence")
            return
        }

        isMelodyPlaying = true
        melodyDurationMs = noteDurationMs.coerceAtLeast(120L)
        melodyStartedAtMs = System.currentTimeMillis()
        Log.d(
            "AtomHunter",
            "startMelodyPlayback: sequenceSize=${capturedSequence.size} noteDurationMs=$melodyDurationMs"
        )
    }

    fun stopMelodyPlayback() {
        isMelodyPlaying = false
        Log.d("AtomHunter", "stopMelodyPlayback")
    }

    fun isMelodyPlaying(): Boolean = isMelodyPlaying

    fun stop() {
        isPlaying = false
        isMelodyPlaying = false
        thread?.join()
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
        Log.d("AtomHunter", "AudioEngine.stop()")
    }
}
