package com.timedensity

import com.timedensity.game.music.CaptureEvent
import com.timedensity.game.music.CaptureSession
import com.timedensity.game.music.TrackComposer
import com.timedensity.game.music.UniverseLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureCompositionTest {
    private val session = CaptureSession(
        seed = 27L,
        events = listOf(
            CaptureEvent("Fe", 110f, 1_000L, 0.9f, 800L),
            CaptureEvent("N", 261f, 1_620L, 0.7f, 800L),
            CaptureEvent("H", 659f, 2_100L, 1f, 800L)
        )
    )

    @Test
    fun compositionUsesRealFrequenciesAndCaptureRhythm() {
        val notes = TrackComposer.compose(session)

        assertEquals(listOf(110f, 261f, 659f), notes.map { it.fundamentalHz })
        assertEquals(listOf(0L, 500L, 1_000L), notes.map { it.onsetMs })
        assertEquals(listOf(55f, 130.5f, 329.5f), notes.map { it.bassHz })
        assertTrue(notes.zipWithNext().all { (first, second) -> second.onsetMs > first.onsetMs })
    }

    @Test
    fun universeLayoutIsSeededBoundedAndIncludesEveryCapturedAtom() {
        val first = UniverseLayout.generate(session, maxParticles = 30)
        val replay = UniverseLayout.generate(session, maxParticles = 30)

        assertEquals(first, replay)
        assertEquals(30, first.size)
        assertEquals(3, first.count { it.isAtom })
        assertTrue(first.all { it.x in 0f..1f && it.y in 0f..1f })
    }

    @Test
    fun emptySessionsProduceNoNotesOrStarsAndCanBeExported() {
        val empty = CaptureSession(seed = 5L, events = emptyList())

        assertTrue(TrackComposer.compose(empty).isEmpty())
        assertTrue(UniverseLayout.generate(empty).isEmpty())
        assertEquals("{\"seed\":5,\"events\":[]}", empty.toJson())
    }

    @Test
    fun sessionExportRetainsCaptureMetadata() {
        val json = session.toJson()

        assertTrue(json.contains("\"element\":\"Fe\""))
        assertTrue(json.contains("\"frequencyHz\":110.0"))
        assertTrue(json.contains("\"capturedAtMs\":1000"))
        assertTrue(json.contains("\"phase\":\"TRANSFORMED\""))
    }
}
