package com.timedensity.game.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.timedensity.game.model.Element
import com.timedensity.game.music.TrackNote
import kotlinx.coroutines.delay
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.sin
import kotlin.math.tanh

class AudioEngine {
    private val sampleRate = 44100
    @Volatile private var isPlaying = false
    private var audioTrack: AudioTrack? = null
    private var thread: Thread? = null

    @Volatile private var carrierFreq = 400f
    @Volatile private var targetFreq = 0f
    @Volatile private var volume = 0.4f
    @Volatile private var isMelodyPlaying = false

    private enum class Waveform { SINE, SAW, TRIANGLE, KICK, NOISE }

    private class ToneVoice(
        val frequency: Float,
        val totalSamples: Int,
        var currentSample: Int = 0,
        val amplitude: Float = 0.12f,
        private val waveform: Waveform = Waveform.SINE
    ) {
        private var phase = 0.0
        private var noiseState = (frequency.toLong() * 31L + totalSamples).toInt()

        fun getNextSample(sampleRate: Int): Float {
            if (currentSample >= totalSamples) return 0f

            val attackSamples = (sampleRate * 0.015f).toInt().coerceAtLeast(1)
            val envelope = when {
                currentSample < attackSamples -> currentSample.toFloat() / attackSamples
                else -> {
                    val decayProgress = (currentSample - attackSamples).toFloat() /
                        (totalSamples - attackSamples).coerceAtLeast(1)
                    val remaining = (1f - decayProgress).coerceIn(0f, 1f)
                    remaining * remaining
                }
            }

            val progress = currentSample.toFloat() / totalSamples
            val currentFrequency = if (waveform == Waveform.KICK) {
                frequency * (1f - 0.82f * progress)
            } else frequency
            phase += 2.0 * Math.PI * currentFrequency / sampleRate
            if (phase >= 2.0 * Math.PI) phase -= 2.0 * Math.PI
            val normalizedPhase = phase / (2.0 * Math.PI)
            val oscillator = when (waveform) {
                Waveform.SINE, Waveform.KICK -> sin(phase)
                Waveform.SAW -> 2.0 * normalizedPhase - 1.0
                Waveform.TRIANGLE -> 1.0 - 4.0 * kotlin.math.abs(normalizedPhase - 0.5)
                Waveform.NOISE -> {
                    noiseState = noiseState * 1_664_525 + 1_013_904_223
                    (noiseState.toDouble() / Int.MAX_VALUE).coerceIn(-1.0, 1.0)
                }
            }
            currentSample++
            return (oscillator * envelope * amplitude).toFloat()
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
        if (minSize <= 0) {
            isPlaying = false
            return
        }

        val track = runCatching { AudioTrack.Builder()
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
        }.getOrNull()
        if (track == null || track.state != AudioTrack.STATE_INITIALIZED) {
            track?.release()
            isPlaying = false
            return
        }

        audioTrack = track
        runCatching { track.play() }.onFailure {
            isPlaying = false
            track.release()
            audioTrack = null
            return
        }

        thread = Thread {
            val buffer = ShortArray((minSize / Short.SIZE_BYTES).coerceAtLeast(1))
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

    fun setVolume(level: Float) {
        if (level.isFinite()) volume = level.coerceIn(0f, 1f)
    }

    fun playElementTone(element: Element) {
        playToneVoice(element.frequency, durationMs = 800L, amplitude = 0.15f)
    }

    fun playMelodyTone(frequency: Float, durationMs: Long = 380L) {
        playToneVoice(frequency, durationMs = durationMs, amplitude = 0.20f)
    }

    private fun playToneVoice(frequency: Float, durationMs: Long, amplitude: Float) {
        val totalSamples = ((durationMs / 1000f) * sampleRate).toInt().coerceAtLeast(100)
        if (activeVoices.size >= 18) {
            activeVoices.removeAt(0)
        }
        activeVoices.add(ToneVoice(frequency, totalSamples, 0, amplitude))
    }

    private fun playSynthVoice(
        frequency: Float,
        durationMs: Long,
        amplitude: Float,
        waveform: Waveform
    ) {
        val safeFrequency = frequency.coerceIn(35f, 12_000f)
        val totalSamples = ((durationMs / 1000f) * sampleRate).toInt().coerceAtLeast(100)
        if (activeVoices.size >= 18) activeVoices.removeAt(0)
        activeVoices.add(ToneVoice(safeFrequency, totalSamples, 0, amplitude, waveform))
    }

    private fun playIndustrialKick() {
        playSynthVoice(128f, 220L, 0.24f, Waveform.KICK)
        playSynthVoice(64f, 260L, 0.11f, Waveform.SINE)
    }

    private fun playIndustrialHit() {
        playSynthVoice(190f, 110L, 0.08f, Waveform.TRIANGLE)
        playSynthVoice(5_200f, 75L, 0.045f, Waveform.NOISE)
    }

    private fun playHiHat() {
        playSynthVoice(7_800f, 45L, 0.035f, Waveform.NOISE)
    }

    private fun playCompositionLead(note: TrackNote) {
        val energy = 0.55f + note.precision * 0.45f
        playSynthVoice(note.fundamentalHz, note.durationMs, 0.15f * energy, Waveform.TRIANGLE)
        playSynthVoice(note.bassHz, note.durationMs + 80L, 0.10f * energy, Waveform.SINE)
        playSynthVoice(note.fundamentalHz * 2f, note.durationMs / 2L, 0.035f * energy, Waveform.SINE)
    }

    suspend fun playComposition(
        notes: List<TrackNote>,
        onNotePlayed: (index: Int, elementSymbol: String) -> Unit
    ) {
        if (notes.isEmpty()) return
        isMelodyPlaying = true
        var noteIndex = 0
        var nextBeat = 0L
        var nextHat = 0L
        val startedAt = System.nanoTime() / 1_000_000L
        val finalOnset = notes.last().onsetMs + notes.last().durationMs + 100L
        while (isMelodyPlaying && isPlaying) {
            val elapsed = (System.nanoTime() / 1_000_000L - startedAt).coerceAtLeast(0L)
            if (elapsed > finalOnset) break

            while (noteIndex < notes.size && notes[noteIndex].onsetMs <= elapsed) {
                val note = notes[noteIndex]
                onNotePlayed(note.eventIndex, note.elementSymbol)
                playCompositionLead(note)
                noteIndex++
            }

            val beat = elapsed / 500L
            if (beat >= nextBeat) {
                if (beat % 4L == 0L || beat % 4L == 2L) playIndustrialKick() else playIndustrialHit()
                nextBeat = beat + 1L
            }
            val hat = elapsed / 250L
            if (hat >= nextHat) {
                playHiHat()
                nextHat = hat + 1L
            }
            delay(20L)
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
