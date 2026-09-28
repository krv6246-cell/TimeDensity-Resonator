package com.timedensity

import org.junit.Test
import kotlin.math.sin

class AudioTest {
    @Test
    fun testAudio() {
        var angleCarrier = 0.0
        var carrierFreq = 400.0
        val sampleRate = 44100
        val freq = 659.0

        for (j in 0..(44100 * 150)) { // 150 seconds
            angleCarrier += 2.0 * Math.PI * carrierFreq / sampleRate
            val phase = (angleCarrier / carrierFreq) * freq
            val sample = sin(phase)
            if (sample.isNaN()) {
                println("NaN at $j")
                break
            }
        }
        println("Done AudioTest")
    }
}
