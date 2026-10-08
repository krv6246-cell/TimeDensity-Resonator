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
                // Softer pitch envelope for ambient thud instead of sharp EDM kick
                frequency * (1f + 1.5f * kotlin.math.exp(-progress * 8.0).toFloat())
            } else frequency
            phase += 2.0 * Math.PI * currentFrequency / sampleRate
            if (phase >= 2.0 * Math.PI) phase -= 2.0 * Math.PI
            val normalizedPhase = phase / (2.0 * Math.PI)
            val oscillator = when (waveform) {
                Waveform.SINE -> sin(phase)
                Waveform.KICK -> sin(phase) // Softer sine-based kick
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
            var angleAmbient = 0.0

            val twoPi = 2.0 * Math.PI

            while (isPlaying) {
                for (i in buffer.indices) {
                    var sum = 0.0
                    
                    if (isAmbientPhase) {
                        // Ominous, swirling black hole ambient drone for the start screen
                        val baseDrone = sin(angleAmbient) * 0.12
                        val subRumble = sin(angleAmbient * 0.4) * 0.08
                        val sweep = sin(angleAmbient * 2.5 + sin(angleAmbient * 0.1)) * 0.04
                        sum = baseDrone + subRumble + sweep
                        
                        angleAmbient += twoPi * 42.0 / sampleRate // Deep 42Hz fundamental
                        if (angleAmbient >= twoPi) angleAmbient -= twoPi
                    } else {
                        // In-game signals
                        val droneSample = sin(angleDrone) * 0.03
                        val carrierSample = sin(angleCarrier) * 0.08
                        val targetSample = if (targetFreq > 0f) sin(angleTarget) * 0.08 else 0.0
                        sum = droneSample + carrierSample + targetSample

                        angleDrone += twoPi * 50.0 / sampleRate
                        if (angleDrone >= twoPi) angleDrone -= twoPi

                        angleCarrier += twoPi * carrierFreq / sampleRate
                        if (angleCarrier >= twoPi) angleCarrier -= twoPi

                        if (targetFreq > 0f) {
                            angleTarget += twoPi * targetFreq / sampleRate
                            if (angleTarget >= twoPi) angleTarget -= twoPi
                        }
                    }

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
                }
                audioTrack?.write(buffer, 0, buffer.size)
            }
        }
        thread?.start()
    }

    @Volatile var isAmbientPhase = false

    fun startAmbientPhase() {
        isAmbientPhase = true
        start()
    }

    fun stopAmbientPhase() {
        isAmbientPhase = false
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

    private fun playCompositionLead(note: TrackNote, tempoMultiplier: Float = 1f) {
        val energy = 0.55f + note.precision * 0.45f
        val dur = (note.durationMs / tempoMultiplier).toLong()
        
        // Deep ambient pads (SINE waves to avoid metallic sound)
        playSynthVoice(note.fundamentalHz, dur + 600L, 0.16f * energy, Waveform.SINE)
        playSynthVoice(note.bassHz, dur + 1000L, 0.14f * energy, Waveform.SINE)
        // Ethereal overtones
        playSynthVoice(note.fundamentalHz * 1.5f, dur + 400L, 0.05f * energy, Waveform.SINE)
        playSynthVoice(note.fundamentalHz * 2f, dur + 200L, 0.03f * energy, Waveform.SINE)
    }

    private fun playSoftBeat() {
        // Deep ambient heartbeat thud
        playSynthVoice(45f, 600L, 0.25f, Waveform.KICK)
        playSynthVoice(90f, 800L, 0.10f, Waveform.SINE)
    }

    private fun playSoftAccent() {
        // Soft atmospheric drone
        playSynthVoice(220f, 1500L, 0.03f, Waveform.SINE)
    }

    private fun playSnare() {
        // Replaced snare with a low textural rumble
        playSynthVoice(110f, 1200L, 0.06f, Waveform.NOISE)
    }

    suspend fun playComposition(
        notes: List<TrackNote>,
        onNotePlayed: (index: Int, elementSymbol: String) -> Unit
    ) {
        if (notes.isEmpty()) return
        isMelodyPlaying = true
        var noteIndex = 0
        var nextBeat = 0L
        val tempoMultiplier = 0.7f // Slower, relaxed ambient pace
        val startedAt = System.nanoTime() / 1_000_000L
        val finalOnset = notes.last().onsetMs + notes.last().durationMs + 100L

        while (isMelodyPlaying && isPlaying) {
            val realElapsed = (System.nanoTime() / 1_000_000L - startedAt).coerceAtLeast(0L)
            val elapsed = (realElapsed * tempoMultiplier).toLong()

            if (elapsed > finalOnset + 1500L) break

            while (noteIndex < notes.size && notes[noteIndex].onsetMs <= elapsed) {
                val note = notes[noteIndex]
                onNotePlayed(note.eventIndex, note.elementSymbol)
                playCompositionLead(note, tempoMultiplier)
                noteIndex++
            }

            // Slow ambient drone/heartbeat
            val beatInterval = 1000L // 1 second per pulse
            val beat = elapsed / beatInterval
            if (beat >= nextBeat) {
                when (beat % 4L) {
                    0L, 2L -> {
                        playSoftBeat()
                    }
                    1L, 3L -> {
                        playSoftAccent()
                    }
                }
                nextBeat = beat + 1L
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
