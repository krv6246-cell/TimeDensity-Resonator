package com.timedensity.game.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
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

    // Tone generators
    private val activeTones = mutableListOf<Float>()

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

            while (isPlaying) {
                for (i in buffer.indices) {
                    val droneSample = sin(angleDrone) * 0.1 // 50 Hz drone
                    val carrierSample = sin(angleCarrier) * 0.2
                    val targetSample = if (targetFreq > 0) sin(angleTarget) * 0.2 else 0.0
                    
                    var sum = droneSample + carrierSample + targetSample
                    
                    // Add active stabilized tones
                    activeTones.forEachIndexed { index, freq ->
                        val phase = (angleCarrier / carrierFreq) * freq // simplistic phase
                        sum += sin(phase) * 0.1
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

    fun setCarrierFrequency(freq: Float) {
        carrierFreq = freq
    }

    fun setTargetFrequency(freq: Float) {
        targetFreq = freq
    }
    
    fun playElementTone(element: Element) {
        // Just add to active tones for now
        activeTones.add(element.frequency)
        // In a real synth, we'd trigger an envelope
    }
    
    fun clearTones() {
        activeTones.clear()
    }

    fun stop() {
        isPlaying = false
        thread?.join()
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }
}
