package com.timedensity.game.model

import androidx.compose.ui.graphics.Color

enum class Element(val symbol: String, val frequency: Float, val color: Color) {
    H("H", 659f, Color(0xFF00FFFF)),
    He("He", 392f, Color(0xFF8A2BE2)),
    C("C", 293f, Color(0xFFFFBF00)),
    N("N", 261f, Color(0xFF00FF00)),
    O("O", 220f, Color(0xFFFF7F50)),
    Fe("Fe", 110f, Color(0xFFB87333))
}

data class Atom(
    val id: Int,
    val element: Element,
    val x: Float, // current logical X
    val y: Float, // current logical Y
    val orbitAngle: Float = 0f,
    val orbitRadius: Float = 0f,
    val orbitSpeed: Float = 0f,
    val isCapturing: Boolean = false,
    val captureProgress: Float = 0f,
    val startCaptureAngle: Float = 0f,
    val startCaptureRadius: Float = 0f,
    val absorbed: Boolean = false
)
