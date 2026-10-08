package com.timedensity.game.ui

import android.animation.ValueAnimator
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timedensity.game.engine.GameEngine
import com.timedensity.game.model.Atom
import com.timedensity.game.model.Element
import com.timedensity.game.model.GamePhase
import com.timedensity.game.music.UniverseLayout
import com.timedensity.resonator.core.ResonancePhase
import com.timedensity.ui.theme.HunterBackground
import com.timedensity.ui.theme.HunterCyan
import com.timedensity.ui.theme.HunterMetal
import com.timedensity.ui.theme.HunterSurface
import com.timedensity.ui.theme.HunterTurquoise
import com.timedensity.ui.theme.HunterViolet
import com.timedensity.ui.theme.AtomHunterWordmarkStyle
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun RezonatorApp(engine: GameEngine, onExit: () -> Unit) {
    val phase by engine.phase.collectAsState()
    val context = LocalContext.current
    var showExitConfirmation by remember { mutableStateOf(false) }
    var exportStatus by remember { mutableStateOf("") }
    var pendingExport by remember { mutableStateOf("") }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        exportStatus = if (uri == null) {
            "Save cancelled"
        } else {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    writer.write(pendingExport)
                } ?: error("Could not open the selected file")
            }.fold(
                onSuccess = { "Pattern saved" },
                onFailure = { "Could not save pattern" }
            )
        }
    }
    BackHandler {
        if (phase == GamePhase.START) showExitConfirmation = true else engine.resetToHome()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HunterBackground)
    ) {
        when (phase) {
            GamePhase.START -> {
                val session by engine.captureSession.collectAsState()
                StartScreen(
                    canSaveSession = session.events.isNotEmpty(),
                    exportStatus = exportStatus,
                    onStart = { engine.startGame() },
                    onSaveSession = {
                        pendingExport = engine.resultSessionJson()
                        exportStatus = "Choose where to save your session pattern"
                        exportLauncher.launch("atom-hunter-${System.currentTimeMillis()}.json")
                    },
                    onExit = { showExitConfirmation = true }
                )
            }
            GamePhase.PLAYING, GamePhase.COLLAPSE -> GameScreen(
                engine = engine,
                onExit = { showExitConfirmation = true }
            )
            GamePhase.RESULT -> ResultScreen(
                engine = engine,
                onExit = { showExitConfirmation = true }
            )
        }
    }
    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = { Text("Exit Atom Hunter?") },
            text = { Text("Your current hunt will end.") },
            confirmButton = {
                TextButton(onClick = {
                    showExitConfirmation = false
                    engine.stopResultMelody()
                    engine.resetToHome()
                    onExit()
                }) { Text("EXIT") }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmation = false }) { Text("CANCEL") }
            },
            containerColor = HunterSurface,
            titleContentColor = HunterCyan,
            textContentColor = Color.White
        )
    }
}

@Composable
fun StartScreen(
    canSaveSession: Boolean,
    exportStatus: String,
    onStart: () -> Unit,
    onSaveSession: () -> Unit,
    onExit: () -> Unit
) {
    val compactLayout = LocalConfiguration.current.screenHeightDp < 700
    val reducedMotion = remember { reducedMotionEnabled() }
    val (scale, rotate) = if (reducedMotion) {
        1f to 0f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
            label = "blackhole_pulse"
        )
        val rotate by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
            label = "blackhole_rotate"
        )
        scale to rotate
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = if (compactLayout) 24.dp else 48.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "∞", fontSize = 34.sp, color = HunterCyan, fontWeight = FontWeight.Light)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "TIME  /  DENSITY", fontSize = 11.sp, color = HunterMetal, letterSpacing = 3.sp)
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(HunterCyan.copy(alpha = 0.1f), HunterViolet.copy(alpha = 0.12f), HunterBackground)
                        )
                    )
                    .border(1.dp, HunterCyan.copy(alpha = 0.28f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 24.dp, vertical = if (compactLayout) 14.dp else 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ATOM",
                        color = HunterCyan,
                        style = AtomHunterWordmarkStyle.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = if (compactLayout) 44.sp else 58.sp,
                            letterSpacing = 5.sp,
                            lineHeight = if (compactLayout) 48.sp else 62.sp,
                            shadow = Shadow(HunterCyan.copy(alpha = 0.7f), blurRadius = 18f)
                        )
                    )
                    Text(
                        text = "HUNTER",
                        color = HunterViolet,
                        style = AtomHunterWordmarkStyle.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = if (compactLayout) 30.sp else 38.sp,
                            letterSpacing = 7.sp,
                            lineHeight = if (compactLayout) 34.sp else 42.sp,
                            shadow = Shadow(HunterViolet.copy(alpha = 0.65f), blurRadius = 16f)
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "FREQUENCY  /  MATTER  /  TIME",
                        color = HunterTurquoise,
                        fontSize = 9.sp,
                        letterSpacing = 2.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Hunt the frequency. Capture the atom.",
                fontSize = 14.sp,
                color = Color(0xFFF2F5FF).copy(alpha = 0.82f),
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.7.sp
            )
        }
        
        Spacer(modifier = Modifier.weight(1.5f))
        
        Canvas(modifier = Modifier.size(if (compactLayout) 128.dp else 160.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2 * scale
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, HunterViolet.copy(alpha = 0.3f), Color.Transparent),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, HunterCyan.copy(alpha = 0.4f), Color.Transparent),
                    center = center,
                    radius = radius * 0.8f
                ),
                radius = radius * 0.8f,
                center = center
            )
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, HunterTurquoise.copy(alpha = 0.32f), Color.Transparent),
                    center = center,
                    radius = radius * 0.6f
                ),
                radius = radius * 0.6f,
                center = center
            )

            drawContext.canvas.nativeCanvas.save()
            drawContext.canvas.nativeCanvas.rotate(rotate, center.x, center.y)
            drawCircle(
                color = HunterCyan.copy(alpha = 0.8f),
                radius = radius * 0.5f,
                center = center,
                style = Stroke(
                    width = 2.dp.toPx(), 
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
                )
            )
            drawContext.canvas.nativeCanvas.restore()

            drawCircle(
                color = HunterBackground,
                radius = radius * 0.45f,
                center = center
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = HunterTurquoise)
        ) {
            Text("BEGIN HUNT", letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onExit) {
            Text("EXIT", color = HunterMetal, letterSpacing = 2.sp)
        }
        if (canSaveSession) {
            TextButton(onClick = onSaveSession) {
                Text("SAVE LAST SESSION", color = HunterCyan, fontSize = 11.sp, letterSpacing = 1.sp)
            }
        }
        if (exportStatus.isNotEmpty()) {
            Text(exportStatus, color = HunterCyan, fontSize = 11.sp)
        }
    }
}

@Composable
fun GameScreen(engine: GameEngine, onExit: () -> Unit) {
    val atoms by engine.atoms.collectAsState()
    val time by engine.timeRemaining.collectAsState()
    val match by engine.matchPercent.collectAsState()
    val carrier by engine.carrierFreq.collectAsState()
    val target by engine.targetFreq.collectAsState()
    val phase by engine.phase.collectAsState()
    val resPhase by engine.resonancePhase.collectAsState()
    val flash by engine.blackHoleFlash.collectAsState()
    val targetAtomId by engine.targetAtomId.collectAsState()
    
    val reducedMotion = remember { reducedMotionEnabled() }
    val compactLayout = LocalConfiguration.current.screenHeightDp < 700
    val density = LocalDensity.current
    var topOverlayHeight by remember { mutableIntStateOf(0) }
    var bottomOverlayHeight by remember { mutableIntStateOf(0) }
    val topInset = with(density) { topOverlayHeight.toDp() }
    val bottomInset = if (phase == GamePhase.PLAYING) {
        with(density) { bottomOverlayHeight.toDp() }
    } else {
        0.dp
    }
    val bhPulse = if (reducedMotion) 1f else {
        val pulse by rememberInfiniteTransition("bhPulse").animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
            label = "bhPulse"
        )
        pulse
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GravityGrid(
            atoms = atoms,
            resonancePhase = resPhase,
            targetAtomId = targetAtomId,
            matchPercent = match,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topInset, bottom = bottomInset)
        )

        // Main game canvas for atoms & black hole
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topInset, bottom = bottomInset)
        ) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val fieldScale = minOf(size.width, size.height) * 0.38f / 300.dp.toPx()
            
            // Draw Black Hole exactly in the center
            val bhCenter = Offset(centerX, centerY)
            
            // Flash glow
            if (flash > 0f) {
                // Expanding gold ring
                drawCircle(
                    color = HunterCyan.copy(alpha = flash),
                    radius = 90.dp.toPx() * (1f + (1f - flash)),
                    center = bhCenter,
                    style = Stroke(width = 4.dp.toPx() * flash)
                )
                // Particles flying outwards
                val random = Random(time.toLong())
                for (i in 0..7) {
                    val angle = random.nextFloat() * Math.PI.toFloat() * 2f
                    val dist = 50.dp.toPx() + (1f - flash) * 120.dp.toPx()
                    drawCircle(
                        color = HunterViolet.copy(alpha = flash),
                        radius = 4.dp.toPx() * flash,
                        center = Offset(bhCenter.x + cos(angle) * dist, bhCenter.y + sin(angle) * dist)
                    )
                }
            }
            
            // Deep violet inner glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(HunterViolet.copy(alpha = 0.32f), Color.Transparent),
                    center = bhCenter,
                    radius = 80.dp.toPx() * bhPulse
                ),
                center = bhCenter,
                radius = 80.dp.toPx() * bhPulse
            )
            
            // Event horizon
            drawCircle(color = HunterBackground, radius = 35.dp.toPx(), center = bhCenter)
            
            // Rotating rings
            // Speed up rotation when flash > 0
            val currentRotation = if (reducedMotion) 0f else (System.currentTimeMillis() % 10000L) / 10000f * 360f
            val flashRotationBonus = (1f - flash) * 180f
            
            drawContext.canvas.nativeCanvas.save()
            drawContext.canvas.nativeCanvas.rotate(currentRotation + flashRotationBonus, bhCenter.x, bhCenter.y)
            drawCircle(
                color = HunterCyan.copy(alpha = 0.6f + flash * 0.4f),
                radius = 45.dp.toPx() * bhPulse,
                center = bhCenter,
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(40f, 20f), 0f)
                )
            )
            drawContext.canvas.nativeCanvas.restore()

            drawContext.canvas.nativeCanvas.save()
            drawContext.canvas.nativeCanvas.rotate(-currentRotation * 1.5f - flashRotationBonus, bhCenter.x, bhCenter.y)
            drawCircle(
                color = HunterViolet.copy(alpha = 0.6f + flash * 0.4f),
                radius = 55.dp.toPx() * bhPulse,
                center = bhCenter,
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                )
            )
            drawContext.canvas.nativeCanvas.restore()
            
            // Accretion disk highlight
            drawCircle(
                color = HunterTurquoise.copy(alpha = 0.2f + flash * 0.3f),
                radius = 40.dp.toPx() * bhPulse, 
                center = bhCenter,
                style = Stroke(width = 4.dp.toPx())
            )
            
            // Draw Atoms
            for (atom in atoms) {
                if (atom.absorbed) continue
                val atomCenter = Offset(
                    centerX + atom.x.dp.toPx() * fieldScale,
                    centerY + atom.y.dp.toPx() * fieldScale
                )
                
                val scale = if (atom.isCapturing) 1f - atom.captureProgress * 0.85f else 1f
                val alpha = if (atom.isCapturing) 1f - atom.captureProgress else 1f
                
                // Outer ring
                drawCircle(
                    color = atom.element.color.copy(alpha = 0.5f * alpha),
                    radius = 24.dp.toPx() * scale,
                    center = atomCenter,
                    style = Stroke(width = 2.dp.toPx() * scale)
                )
                
                // Nucleus
                drawCircle(
                    color = atom.element.color.copy(alpha = alpha),
                    radius = 5.dp.toPx() * scale,
                    center = atomCenter
                )
                
                // Active/Capture halo
                if (atom.isCapturing) {
                    // Gold trail
                    if (atom.captureProgress > 0f) {
                        drawContext.canvas.nativeCanvas.save()
                        // Move to center
                        drawContext.canvas.nativeCanvas.translate(bhCenter.x, bhCenter.y)
                        val paint = Paint().apply {
                            color = android.graphics.Color.parseColor("#FFFFBF00")
                            this.alpha = (100 * alpha).toInt()
                            style = Paint.Style.STROKE
                            strokeWidth = 4.dp.toPx() * scale
                            strokeCap = Paint.Cap.ROUND
                            isAntiAlias = true
                        }
                        
                        val rectF = RectF(
                            -atom.orbitRadius.dp.toPx() * fieldScale, -atom.orbitRadius.dp.toPx() * fieldScale,
                            atom.orbitRadius.dp.toPx() * fieldScale, atom.orbitRadius.dp.toPx() * fieldScale
                        )
                        // Simple arc trail behind the atom
                        drawContext.canvas.nativeCanvas.drawArc(rectF, Math.toDegrees(atom.orbitAngle.toDouble()).toFloat() - 45f, 45f, false, paint)
                        drawContext.canvas.nativeCanvas.restore()
                    }
                    
                    drawCircle(
                        color = HunterCyan.copy(alpha = alpha),
                        radius = 28.dp.toPx() * scale,
                        center = atomCenter,
                        style = Stroke(width = 2.dp.toPx() * scale)
                    )
                } else {
                    val focused = atom.id == targetAtomId
                    drawCircle(
                        color = (if (focused) HunterCyan else HunterMetal).copy(alpha = if (focused) 0.45f else 0.18f),
                        radius = if (focused) 25.dp.toPx() else 20.dp.toPx(),
                        center = atomCenter,
                        style = Stroke(width = if (focused) 2.dp.toPx() else 1.dp.toPx())
                    )
                }
                
                // Symbol
                if (alpha > 0.2f) {
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = Paint().apply {
                            color = android.graphics.Color.WHITE
                            this.alpha = (alpha * 255).toInt()
                            textSize = 14.dp.toPx() * scale
                            textAlign = Paint.Align.CENTER
                            isAntiAlias = true
                        }
                        drawText(atom.element.symbol, atomCenter.x, atomCenter.y - 14.dp.toPx() * scale, paint)
                    }
                }
            }
        }
        
        // Top Layout (HUD + Resonator)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .onSizeChanged { topOverlayHeight = it.height }
        ) {
            // COMPACT TOP HUD
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("TUNE", color = HunterCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("DRAG TO MATCH THE SELECTED ATOM", color = HunterMetal, fontSize = 9.sp, letterSpacing = 0.7.sp)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = if (compactLayout) 12.dp else 48.dp,
                        bottom = if (compactLayout) 8.dp else 16.dp
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Timer
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TIME", color = Color.Gray, fontSize = 10.sp, letterSpacing = 2.sp)
                    Text(
                        text = String.format("%02d:%02d", time / 60, time % 60),
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 2.sp
                    )
                }
                
                // State & Match
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val stateColor = when(resPhase) {
                        ResonancePhase.MATCHED -> HunterCyan
                        ResonancePhase.STABLE -> HunterViolet
                        ResonancePhase.TRANSFORMED -> Color(0xFFF2F5FF) // Soft White
                        else -> Color.Gray
                    }
                    Text(resPhase.name, color = stateColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("$match%", color = stateColor, fontSize = 14.sp)
                }
                
                // Atoms Captured
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("COLLECTION", color = Color.Gray, fontSize = 10.sp, letterSpacing = 2.sp)
                    val stats by engine.stabilizedCount.collectAsState()
                    val count = stats.values.sum()
                    Text("$count", color = HunterTurquoise, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (phase == GamePhase.PLAYING || phase == GamePhase.COLLAPSE) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .height(if (compactLayout) 82.dp else 110.dp)
                ) {
                    WaveformDisplay(target = target, carrier = carrier, resPhase = resPhase)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { engine.resetToHome() }) {
                    Text("HOME", color = HunterMetal, letterSpacing = 1.sp)
                }
                TextButton(onClick = onExit) {
                    Text("EXIT", color = HunterViolet, letterSpacing = 1.sp)
                }
            }
        }
        
        // BOTTOM TUNE CONTROLS
        if (phase == GamePhase.PLAYING) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(
                        bottom = if (compactLayout) 12.dp else 36.dp,
                        start = 24.dp,
                        end = 24.dp
                    )
                    .onSizeChanged { bottomOverlayHeight = it.height }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("TARGET", color = HunterViolet, fontSize = 9.sp, letterSpacing = 1.5.sp)
                        Text("${target.toInt()} Hz", color = HunterViolet, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    val gap = abs(target - carrier).toInt()
                    val tuningMessage = when {
                        target <= 0f -> "WAITING FOR ATOM"
                        resPhase == ResonancePhase.MATCHED -> "MATCHED · HOLD STEADY"
                        resPhase == ResonancePhase.STABLE -> "STABLE · KEEP TUNING"
                        resPhase == ResonancePhase.TRANSFORMED -> "TRANSFORMED"
                        gap <= 30 -> "IN RESONANCE · Δ ${gap} Hz"
                        gap <= 100 -> "CLOSING IN · Δ ${gap} Hz"
                        else -> "TUNE CARRIER · Δ ${gap} Hz"
                    }
                    val tuningColor = when (resPhase) {
                        ResonancePhase.MATCHED -> HunterCyan
                        ResonancePhase.STABLE, ResonancePhase.TRANSFORMED -> HunterTurquoise
                        else -> HunterMetal
                    }
                    Text(
                        tuningMessage,
                        color = tuningColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text("CARRIER", color = HunterCyan, fontSize = 9.sp, letterSpacing = 1.5.sp)
                        Text("${carrier.toInt()} Hz", color = HunterCyan, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth().height(if (compactLayout) 40.dp else 44.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    itemsIndexed(atoms.filter { !it.isCapturing && !it.absorbed }) { _, atom ->
                        val selected = atom.id == targetAtomId
                        Button(
                            onClick = { engine.selectAtom(atom.id) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) HunterTurquoise else HunterSurface
                            )
                        ) {
                            Text(
                                "${atom.element.symbol} · ${atom.element.frequency.toInt()} Hz",
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Slider
                TuneSlider(
                    target = target,
                    carrier = carrier,
                    onFreqChange = { engine.setCarrierFreq(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (compactLayout) 48.dp else 54.dp)
                )
            }
        }
    }
}

private fun reducedMotionEnabled(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !ValueAnimator.areAnimatorsEnabled()

@Composable
fun GravityGrid(
    atoms: List<Atom>,
    resonancePhase: ResonancePhase,
    targetAtomId: Int? = null,
    matchPercent: Int = 0,
    modifier: Modifier = Modifier
) {
    val reducedMotion = remember { reducedMotionEnabled() }
    val animatedPhase = if (reducedMotion) 0f else {
        val phase by rememberInfiniteTransition(label = "gravity_grid").animateFloat(
            initialValue = 0f,
            targetValue = 2f * Math.PI.toFloat(),
            animationSpec = infiniteRepeatable(tween(6_000, easing = LinearEasing)),
            label = "gravity_phase"
        )
        phase
    }
    Canvas(modifier = modifier) {
        val phase = animatedPhase
        val center = Offset(size.width / 2f, size.height / 2f)
        drawRect(Brush.verticalGradient(listOf(HunterBackground, Color(0xFF071321), HunterBackground)))
        val random = Random(123)
        for (i in 0..54) {
            val x = random.nextFloat() * size.width
            val y = random.nextFloat() * size.height
            val color = if (random.nextBoolean()) HunterCyan else HunterMetal
            drawCircle(
                color = color.copy(alpha = random.nextFloat() * 0.28f),
                radius = random.nextFloat() * 1.5.dp.toPx(),
                center = Offset(x, y)
            )
        }

        val resonanceBoost = when (resonancePhase) {
            ResonancePhase.MATCHED -> 1.18f
            ResonancePhase.STABLE -> 1.42f
            ResonancePhase.TRANSFORMED -> 1.6f
            else -> 0.92f + (matchPercent.coerceIn(0, 100) / 100f) * 0.24f
        }
        val fieldScale = minOf(size.width, size.height) * 0.38f / 300.dp.toPx()
        val lensAtoms = atoms.asSequence().filter { !it.absorbed }.take(4).toList()
        val lensPositions = lensAtoms.map {
            Offset(center.x + it.x.dp.toPx() * fieldScale, center.y + it.y.dp.toPx() * fieldScale)
        }
        val lensRange = 100.dp.toPx()
        val lensRangeSquared = lensRange * lensRange
        val lineCount = 14
        val segments = 40
        val fieldPulse = if (reducedMotion) 0f else (sin(phase) + 1f) * 0.5f
        for (axis in 0..1) {
            for (line in 0..lineCount) {
                val path = Path()
                for (segment in 0..segments) {
                    val progress = segment.toFloat() / segments
                    val x = if (axis == 0) progress * size.width else line * size.width / lineCount
                    val y = if (axis == 0) line * size.height / lineCount else progress * size.height
                    var dx = 0f
                    var dy = 0f
                    lensPositions.forEachIndexed { index, atomCenter ->
                        val diffX = x - atomCenter.x
                        val diffY = y - atomCenter.y
                        val distanceSquared = diffX * diffX + diffY * diffY
                        val focused = lensAtoms[index].id == targetAtomId
                        val influence = (if (focused) 0.24f else 0.16f) * resonanceBoost /
                            (1f + distanceSquared / lensRangeSquared)
                        dx -= diffX * influence
                        dy -= diffY * influence
                    }
                    val lensWave = if (reducedMotion) 0f else
                        sin(progress * (2f * Math.PI.toFloat()) + phase) * 2.dp.toPx()
                    if (axis == 0) dy += lensWave else dx += lensWave
                    val point = Offset(x + dx, y + dy)
                    if (segment == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                }
                drawPath(
                    path,
                    color = if (line % 3 == 0) HunterCyan.copy(alpha = 0.19f) else HunterTurquoise.copy(alpha = 0.11f),
                    style = Stroke(width = if (line % 3 == 0) 1.1.dp.toPx() else 0.7.dp.toPx())
                )
            }
        }
        lensPositions.forEachIndexed { index, position ->
            val focused = lensAtoms[index].id == targetAtomId
            val glowRadius = (if (focused) 42.dp else 32.dp).toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        (if (focused) HunterCyan else HunterViolet).copy(alpha = 0.17f + fieldPulse * 0.08f),
                        Color.Transparent
                    ),
                    center = position,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = position
            )
            drawCircle(
                color = (if (focused) HunterCyan else HunterViolet)
                    .copy(alpha = if (reducedMotion) 0.28f else 0.22f + fieldPulse * 0.18f),
                radius = (if (focused) 13.dp else 9.dp).toPx() + if (focused) fieldPulse * 3.dp.toPx() else 0f,
                center = position,
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

@Composable
fun WaveformDisplay(target: Float, carrier: Float, resPhase: ResonancePhase = ResonancePhase.IDLE) {
    val reducedMotion = remember { reducedMotionEnabled() }
    val phase = if (reducedMotion) 0f else {
        val animatedPhase by rememberInfiniteTransition("wave").animateFloat(
            initialValue = 0f,
            targetValue = 2f * Math.PI.toFloat(),
            animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
            label = "phase"
        )
        animatedPhase
    }
    
    val targetColor = HunterViolet
    val carrierColor = HunterCyan
    
    val glowColor = when(resPhase) {
        ResonancePhase.STABLE -> HunterTurquoise
        ResonancePhase.TRANSFORMED -> Color.White
        else -> carrierColor
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .background(HunterSurface, shape = RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(listOf(targetColor.copy(alpha = 0.5f), glowColor.copy(alpha = 0.5f))),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val midY = size.height / 2
            val width = size.width
            
            val targetPath = Path()
            val carrierPath = Path()
            
            // Grid lines
            drawLine(HunterMetal.copy(alpha = 0.3f), Offset(0f, midY), Offset(width, midY), 1.dp.toPx())
            drawLine(HunterMetal.copy(alpha = 0.3f), Offset(width / 2, 0f), Offset(width / 2, size.height), 1.dp.toPx())
            
            for (x in 0..width.toInt() step 2) {
                val px = x.toFloat()
                val ty = midY + sin((px / width) * (target / 15f) + phase) * (size.height / 3)
                val cy = midY + sin((px / width) * (carrier / 15f) + phase * 1.5f) * (size.height / 3)
                
                if (x == 0) {
                    targetPath.moveTo(px, ty)
                    carrierPath.moveTo(px, cy)
                } else {
                    targetPath.lineTo(px, ty)
                    carrierPath.lineTo(px, cy)
                }
            }
            
            // Outer glow strokes
            drawPath(targetPath, color = targetColor.copy(alpha = 0.3f), style = Stroke(width = 6.dp.toPx()))
            drawPath(carrierPath, color = glowColor.copy(alpha = 0.3f), style = Stroke(width = 6.dp.toPx()))
            
            // Inner core strokes
            drawPath(targetPath, color = targetColor, style = Stroke(width = 2.dp.toPx()))
            drawPath(carrierPath, color = glowColor, style = Stroke(width = 2.dp.toPx()))
        }
    }
}

@Composable
fun TuneSlider(target: Float, carrier: Float, onFreqChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    val minFreq = 100f
    val maxFreq = 700f
    val range = maxFreq - minFreq
    var isDragging by remember { mutableStateOf(false) }
    val currentPos = ((carrier - minFreq) / range).coerceIn(0f, 1f)
    val thumbPosition by animateFloatAsState(currentPos, animationSpec = tween(90), label = "tuner_thumb")
    val pulse by rememberInfiniteTransition("sliderPulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "tuner_pulse"
    )

    Box(
        modifier = modifier
            .semantics {
                contentDescription = "Carrier frequency"
                stateDescription = if (target > 0f) {
                    "${carrier.toInt()} hertz, ${abs(carrier - target).toInt()} hertz from target"
                } else {
                    "${carrier.toInt()} hertz, no target selected"
                }
                progressBarRangeInfo = androidx.compose.ui.semantics.ProgressBarRangeInfo(currentPos, 0f..1f)
                setProgress { progress ->
                    onFreqChange(minFreq + progress.coerceIn(0f, 1f) * range)
                    true
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false }
                ) { change, _ ->
                    val width = size.width.toFloat()
                    val newX = (change.position.x / width).coerceIn(0f, 1f)
                    onFreqChange(minFreq + newX * range)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val midY = size.height / 2
            
            // Cyberpunk grid/track lines
            drawLine(
                color = HunterCyan.copy(alpha = 0.2f),
                start = Offset(0f, midY),
                end = Offset(size.width, midY),
                strokeWidth = 1.dp.toPx()
            )
            
            // Glowing energy segment up to thumb
            val thumbX = thumbPosition * size.width
            drawLine(
                brush = Brush.horizontalGradient(listOf(HunterViolet.copy(alpha = 0.3f), HunterCyan)),
                start = Offset(0f, midY),
                end = Offset(thumbX, midY),
                strokeWidth = 3.dp.toPx(),
                cap = Stroke.DefaultCap
            )
            
            // Fine ticks
            for (tick in 0..20) {
                val tickX = (tick / 20f) * size.width
                val isMajor = tick % 5 == 0
                val tickHeight = if (isMajor) 8.dp.toPx() else 4.dp.toPx()
                val tickAlpha = if (tickX <= thumbX) 0.6f else 0.2f
                drawLine(
                    color = HunterCyan.copy(alpha = tickAlpha),
                    start = Offset(tickX, midY - tickHeight),
                    end = Offset(tickX, midY + tickHeight),
                    strokeWidth = (if (isMajor) 1.5.dp else 1.dp).toPx()
                )
            }
            
            // Target Bracket Highlight
            if (target > 0f) {
                val targetPos = ((target - minFreq) / range).coerceIn(0f, 1f)
                val targetX = targetPos * size.width
                val zoneWidth = (50f / range) * size.width
                
                // Target Zone Glow
                drawRect(
                    color = HunterViolet.copy(alpha = 0.15f + pulse * 0.15f),
                    topLeft = Offset(targetX - zoneWidth / 2, midY - 12.dp.toPx()),
                    size = Size(zoneWidth, 24.dp.toPx())
                )
                
                // Left Bracket
                val brWidth = 3.dp.toPx()
                val brHeight = 10.dp.toPx()
                val leftEdge = targetX - zoneWidth / 2
                drawLine(HunterViolet, Offset(leftEdge, midY - brHeight), Offset(leftEdge, midY + brHeight), strokeWidth = 1.5.dp.toPx())
                drawLine(HunterViolet, Offset(leftEdge, midY - brHeight), Offset(leftEdge + brWidth, midY - brHeight), strokeWidth = 1.5.dp.toPx())
                drawLine(HunterViolet, Offset(leftEdge, midY + brHeight), Offset(leftEdge + brWidth, midY + brHeight), strokeWidth = 1.5.dp.toPx())
                
                // Right Bracket
                val rightEdge = targetX + zoneWidth / 2
                drawLine(HunterViolet, Offset(rightEdge, midY - brHeight), Offset(rightEdge, midY + brHeight), strokeWidth = 1.5.dp.toPx())
                drawLine(HunterViolet, Offset(rightEdge, midY - brHeight), Offset(rightEdge - brWidth, midY - brHeight), strokeWidth = 1.5.dp.toPx())
                drawLine(HunterViolet, Offset(rightEdge, midY + brHeight), Offset(rightEdge - brWidth, midY + brHeight), strokeWidth = 1.5.dp.toPx())
            }
            
            // Thumb
            val thumbRadius = if (isDragging) 18.dp.toPx() else 14.dp.toPx()
            
            // Outer Ring
            drawCircle(
                color = HunterTurquoise.copy(alpha = if (isDragging) 0.6f else 0.2f),
                radius = thumbRadius * (1f + pulse * 0.5f),
                center = Offset(thumbX, midY),
                style = Stroke(width = 1.dp.toPx())
            )
            // Core
            drawCircle(
                color = HunterCyan,
                radius = thumbRadius,
                center = Offset(thumbX, midY)
            )
            // Inner hollow
            drawCircle(
                color = HunterBackground,
                radius = thumbRadius * 0.5f,
                center = Offset(thumbX, midY)
            )
            
            // Frequency Text on Thumb
            if (isDragging) {
                drawContext.canvas.nativeCanvas.apply {
                    val paint = Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 12.dp.toPx()
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    drawText("${carrier.toInt()}Hz", thumbX, midY - thumbRadius - 8.dp.toPx(), paint)
                }
            }
        }
    }
}

@Composable
fun ResultScreen(
    engine: GameEngine,
    onExit: () -> Unit
) {
    val sequence by engine.capturedSequence.collectAsState()
    val session by engine.captureSession.collectAsState()
    val playingIndex by engine.playingNoteIndex.collectAsState()
    val isMelodyPlaying by engine.isMelodyPlaying.collectAsState()
    val galaxyStars = remember(session) { UniverseLayout.generate(session, maxParticles = 2_000) }
    val atomStars = remember(galaxyStars) { galaxyStars.filter { it.isAtom } }
    val reducedMotion = remember { reducedMotionEnabled() }
    
    // Spinning galaxy core rotation
    val galaxyRotation = if (reducedMotion) 0f else {
        val rotation by rememberInfiniteTransition(label = "galaxy_drift").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                tween(durationMillis = 40_000, easing = LinearEasing) // Majestic slow spin
            ),
            label = "galaxy_rotation"
        )
        rotation
    }

    var hasPlayed by remember(session) { mutableStateOf(false) }
    var animTrigger by remember { mutableStateOf(0) }
    val animProgress = remember { Animatable(0f) }

    DisposableEffect(engine) {
        onDispose { engine.stopResultMelody() }
    }

    LaunchedEffect(session) {
        if (session.events.isNotEmpty()) {
            hasPlayed = true
            engine.playResultMelody()
        }
    }

    LaunchedEffect(animTrigger) {
        animProgress.snapTo(0f)
        if (reducedMotion) {
            animProgress.snapTo(1f)
        } else {
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 3500, easing = FastOutSlowInEasing)
            )
        }
    }

    val pulseAnim = remember { Animatable(0f) }
    LaunchedEffect(playingIndex) {
        if (playingIndex >= 0) {
            pulseAnim.snapTo(1f)
            if (reducedMotion) pulseAnim.snapTo(0f)
            else pulseAnim.animateTo(0f, animationSpec = tween(600)) // Smooth ambient pulse
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ATOM HUNTER",
            fontSize = 18.sp,
            color = HunterCyan,
            style = AtomHunterWordmarkStyle.copy(fontSize = 18.sp, letterSpacing = 3.sp),
            modifier = Modifier.padding(vertical = 6.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { 
                        rotationX = 55f // 3D Perspective Tilt!
                        rotationZ = galaxyRotation 
                        cameraDistance = 12 * density
                    }
            ) {
                val center = Offset(size.width / 2, size.height / 2)
                val maxRadius = minOf(size.width * 0.49f, size.height * 0.49f)
                val progress = animProgress.value
                val pulse = pulseAnim.value
                
                val collapsePhase = 0.4f
                val flashPhase = 0.5f

                val collapse = if (progress < collapsePhase) 1f - (progress / collapsePhase) else 0.01f
                val expansion = if (progress < flashPhase) 0f else ((progress - flashPhase) / (1f - flashPhase)).coerceIn(0f, 1f)
                
                val flashIntensity = when {
                    progress < collapsePhase -> 0f
                    progress < flashPhase -> (progress - collapsePhase) / (flashPhase - collapsePhase)
                    progress < flashPhase + 0.1f -> 1f - ((progress - flashPhase) / 0.1f)
                    else -> 0f
                }

                // 1. NEBULA BACKGROUND (Fades in during expansion)
                if (expansion > 0f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                HunterViolet.copy(alpha = 0.35f * expansion),
                                HunterTurquoise.copy(alpha = 0.1f * expansion),
                                Color.Transparent
                            ),
                            center = center,
                            radius = maxRadius * 1.5f
                        ),
                        radius = maxRadius * 1.5f,
                        center = center
                    )
                }

                // 2. SUPERNOVA FLASH
                if (flashIntensity > 0f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = flashIntensity), HunterCyan.copy(alpha = flashIntensity * 0.5f), Color.Transparent),
                            center = center,
                            radius = maxRadius * 2f
                        ),
                        radius = maxRadius * 2f,
                        center = center
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = flashIntensity),
                        radius = maxRadius * 0.8f * flashIntensity,
                        center = center,
                        style = Stroke(width = 4.dp.toPx() * flashIntensity)
                    )
                }

                // 3. COLLAPSING CORE & EXPANDING GALAXY
                val radiusScale = if (progress < collapsePhase) collapse else 0.1f + expansion * 0.9f
                
                if (progress < flashPhase) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(HunterViolet.copy(alpha = 0.5f * collapse), Color.Transparent),
                            center = center,
                            radius = maxRadius * collapse
                        ),
                        radius = maxRadius * collapse,
                        center = center
                    )
                    drawCircle(HunterBackground, radius = maxRadius * 0.15f * collapse, center = center)
                }

                // Calculate geometric layout positions for atoms
                val geomRadius = maxRadius * 0.45f
                val N = sequence.size
                val atomPositions = sequence.mapIndexed { index, _ ->
                    val angle = (index.toFloat() / N.coerceAtLeast(1)) * 2f * Math.PI.toFloat() + (progress * 2f)
                    val targetX = center.x + cos(angle) * geomRadius
                    val targetY = center.y + sin(angle) * geomRadius
                    
                    val star = atomStars.getOrNull(index)
                    if (star != null) {
                        val startX = center.x + (star.x - 0.5f) * maxRadius * 2f * radiusScale
                        val startY = center.y + (star.y - 0.48f) * maxRadius * 2f * radiusScale
                        
                        val currentX = startX + (targetX - startX) * expansion
                        val currentY = startY + (targetY - startY) * expansion
                        Offset(currentX, currentY)
                    } else {
                        Offset(targetX, targetY)
                    }
                }

                // Sacred geometry connections forming the geometric figure
                if (atomPositions.size > 1 && expansion > 0f) {
                    val path = Path()
                    atomPositions.forEachIndexed { i, pos ->
                        if (i == 0) path.moveTo(pos.x, pos.y) else path.lineTo(pos.x, pos.y)
                    }
                    path.close()
                    drawPath(path, HunterCyan.copy(alpha = 0.5f * expansion), style = Stroke(width = 3.dp.toPx()))
                    
                    if (atomPositions.size in 3..12) {
                        for (i in 0 until atomPositions.size) {
                            for (j in i + 2 until atomPositions.size) {
                                if (i == 0 && j == atomPositions.size - 1) continue
                                drawLine(HunterCyan.copy(alpha = 0.15f * expansion), atomPositions[i], atomPositions[j], 1.dp.toPx())
                            }
                        }
                    }
                }

                var atomIndex = 0
                galaxyStars.forEach { star ->
                    val drift = if (reducedMotion) 0f else progress * 0.05f
                    
                    val point = if (star.isAtom && atomIndex < atomPositions.size) {
                        atomPositions[atomIndex++]
                    } else {
                        Offset(
                            center.x + (star.x - 0.5f) * maxRadius * 2f * radiusScale,
                            center.y + (star.y - 0.48f) * maxRadius * 2f * radiusScale
                        )
                    }
                    
                    val highlighted = star.isAtom && star.eventIndex == playingIndex
                    val color = galaxyColor(star.elementSymbol)
                    val alpha = if (progress < collapsePhase) {
                        star.opacity * collapse 
                    } else {
                        star.opacity * if (star.isAtom) 0.8f + expansion * 0.2f else expansion 
                    }

                    drawCircle(
                        color = color.copy(alpha = (alpha + if (highlighted) pulse * 0.6f else 0f).coerceIn(0f, 1f)),
                        radius = star.size.dp.toPx() * (1f + drift + if (highlighted) pulse * 0.5f else 0f),
                        center = point
                    )
                    
                    // Atom highlights during melody
                    if (highlighted && expansion > 0f) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(HunterCyan.copy(alpha = pulse * 0.5f), Color.Transparent),
                                center = point,
                                radius = star.size.dp.toPx() * 8f
                            ),
                            radius = star.size.dp.toPx() * 8f,
                            center = point
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = pulse),
                            radius = star.size.dp.toPx() * (2.5f + pulse),
                            center = point,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }

                if (pulse > 0f && expansion > 0.5f) {
                    drawCircle(
                        color = HunterTurquoise.copy(alpha = pulse * 0.5f),
                        radius = maxRadius * (0.6f + (1f - pulse) * 0.4f),
                        center = center,
                        style = Stroke(width = 2.dp.toPx() * pulse)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    if (isMelodyPlaying) {
                        engine.stopResultMelody()
                    } else {
                        animTrigger++
                        engine.replayResultAnimationAndMelody()
                    }
                },
                enabled = sequence.isNotEmpty(),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMelodyPlaying) HunterViolet else HunterTurquoise
                )
            ) {
                Text(
                    if (isMelodyPlaying) "STOP" else if (hasPlayed) "REPLAY" else "PLAY",
                    modifier = Modifier.semantics { contentDescription = if (isMelodyPlaying) "Stop playback" else if (hasPlayed) "Replay composition" else "Play composition" },
                    color = HunterBackground,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            Button(
                onClick = onExit,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = HunterSurface)
            ) {
                Text("EXIT", color = HunterCyan, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}


private fun galaxyColor(symbol: String): Color = when (symbol) {
    "H", "N", "O" -> HunterCyan
    "He", "C" -> HunterViolet
    else -> HunterMetal
}
