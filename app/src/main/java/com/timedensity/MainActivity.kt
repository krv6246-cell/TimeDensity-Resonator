package com.timedensity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.timedensity.game.engine.GameEngine
import com.timedensity.game.ui.RezonatorApp
import com.timedensity.ui.theme.REZONATORTheme

class MainActivity : ComponentActivity() {
    private val gameEngine: GameEngine by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            REZONATORTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RezonatorApp(engine = gameEngine, onExit = { finish() })
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        gameEngine.resumeAudioForForeground()
    }

    override fun onStop() {
        gameEngine.stopAudioForBackground()
        super.onStop()
    }
}
