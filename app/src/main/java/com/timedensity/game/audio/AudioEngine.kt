package com.timedensity.game.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.timedensity.game.model.Element
import kotlinx.coroutines.delay
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.sin
import kotlin.math.tanh

class AudioEngine {
    private val sampleRate = 44100
    private var isPlaying = false
    private var audioTrack: AudioTrack? = null
    private var thread: Thread? = null

    @Volatile private var carrierFreq = 400f
    @Volatile private var targetFreq = 0f
    @Volatile private var volume = 0.4f
    @Volatile private var isMelodyPlaying = false

    private class ToneVoice(
        val frequency: Float,
        val totalSamples: Int,
        var currentSample: Int = 0,
        val amplitude: Float = 0.12f
    ) {
        fun getNextSample(sampleRate: Int): Float {
            if (currentSample >= totalSamples) return 0f

            // Envelope: 15ms attack, smooth parabolic decay
            val attackSamples = (sampleRate * 0.015f).toInt().coerceAtLeast(1)
            val envelope = when {
                currentSample < attackSamples -> currentSample.toFloat() / attackSamples
                else -> {
                    val decayProgress = (currentSample - attackSamples).toFloat() / (totalSamples - attackSamples)
                    val remaining = (1f - decayProgress).coerceIn(0f, 1f)
                    remaining * remaining
                }
            }

            val phase = 2.0 * Math.PI * frequency * currentSample / sampleRate
            currentSample++
            return (sin(phase) * envelope * amplitude).toFloat()
        }

        fun isFinished(): Boolean = currentSample >= totalSamples
    }

    private val activeVoices = CopyOnWriteArrayList<ToneVoice>()

    fun start() {
        synchronized(this) {
            if (isPlaying) return
            isPlaying = true
        }

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

            val twoPi = 2.0 * Math.PI

            while (isPlaying) {
                for (i in buffer.indices) {
                    // Subdued 50Hz drone
                    val droneSample = sin(angleDrone) * 0.03

                    // Subdued Carrier
                    val carrierSample = sin(angleCarrier) * 0.08

                    // Subdued Target
                    val targetSample = if (targetFreq > 0f) sin(angleTarget) * 0.08 else 0.0

                    var sum = droneSample + carrierSample + targetSample

                    // Add active element tone voices
                    if (!activeVoices.isEmpty()) {
                        var voiceSum = 0f
                        for (voice in activeVoices) {
                            voiceSum += voice.getNextSample(sampleRate)
                            if (voice.isFinished()) {
                                activeVoices.remove(voice)
                            }
                        }
                        sum += voiceSum
                    }

                    // Soft limiter using tanh to prevent harsh clipping/distortion
                    val limitedSum = tanh(sum * 0.8)

                    // Convert to 16-bit PCM safely
                    val pcmValue = (limitedSum * Short.MAX_VALUE * volume).toInt().coerceIn(
                        Short.MIN_VALUE.toInt(),
                        Short.MAX_VALUE.toInt()
                    )
                    buffer[i] = pcmValue.toShort()

                    // Keep phase angles within [0, 2π) to prevent double precision loss over time
                    angleDrone += twoPi * 50.0 / sampleRate
                    if (angleDrone >= twoPi) angleDrone -= twoPi

                    angleCarrier += twoPi * carrierFreq / sampleRate
                    if (angleCarrier >= twoPi) angleCarrier -= twoPi

                    if (targetFreq > 0f) {
                        angleTarget += twoPi * targetFreq / sampleRate
                        if (angleTarget >= twoPi) angleTarget -= twoPi
                    }
                }
                audioTrack?.write(buffer, 0, buffer.size)
            }
        }
        thread?.start()
    }

    fun setCarrierFrequency(freq: Float) {
        if (!freq.isNaN() && !freq.isInfinite() && freq >= 0f) {
            carrierFreq = freq
        }
    }

    fun setTargetFrequency(freq: Float) {
        if (!freq.isNaN() && !freq.isInfinite() && freq >= 0f) {
            targetFreq = freq
        } else {
            targetFreq = 0f
        }
    }

    fun playElementTone(element: Element) {
        playToneVoice(element.frequency, durationMs = 800L, amplitude = 0.15f)
    }

    fun playMelodyTone(frequency: Float, durationMs: Long = 380L) {
        playToneVoice(frequency, durationMs = durationMs, amplitude = 0.20f)
    }

    private fun playToneVoice(frequency: Float, durationMs: Long, amplitude: Float) {
        val totalSamples = ((durationMs / 1000f) * sampleRate).toInt().coerceAtLeast(100)
        // Limit active voices to 5 to prevent audio overload
        if (activeVoices.size >= 5) {
            activeVoices.removeAt(0)
        }
        activeVoices.add(ToneVoice(frequency, totalSamples, 0, amplitude))
    }

    suspend fun playMelodySequence(
        sequence: List<Element>,
        onNotePlayed: (index: Int, element: Element) -> Unit
    ) {
        if (sequence.isEmpty()) return
        isMelodyPlaying = true
        for (i in sequence.indices) {
            if (!isMelodyPlaying || !isPlaying) break
            val element = sequence[i]
            onNotePlayed(i, element)
            playMelodyTone(element.frequency, durationMs = 380L)
            delay(420L)
        }
        isMelodyPlaying = false
    }

    fun stopMelody() {
        isMelodyPlaying = false
    }

    fun clearTones() {
        activeVoices.clear()
    }

    fun stop() {
        synchronized(this) {
            if (!isPlaying) return
            isPlaying = false
        }
        isMelodyPlaying = false
        activeVoices.clear()
        try {
            thread?.join(500)
        } catch (_: Exception) {}
        thread = null

        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }
}
