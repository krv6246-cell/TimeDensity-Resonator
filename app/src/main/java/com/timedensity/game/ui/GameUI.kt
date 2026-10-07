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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
            GamePhase.START -> StartScreen(
                onStart = { engine.startGame() },
                onExit = { showExitConfirmation = true }
            )
            GamePhase.PLAYING, GamePhase.COLLAPSE -> GameScreen(
                engine = engine,
                onExit = { showExitConfirmation = true }
            )
            GamePhase.RESULT -> ResultScreen(
                engine = engine,
                exportStatus = exportStatus,
                onSavePattern = {
                    pendingExport = engine.resultSessionJson()
                    exportStatus = "Choose where to save your session pattern"
                    exportLauncher.launch("atom-hunter-${System.currentTimeMillis()}.json")
                },
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
fun StartScreen(onStart: () -> Unit, onExit: () -> Unit) {
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
        // 1 & 2. Logo and Subtitle
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "∞", fontSize = 32.sp, color = HunterCyan, fontWeight = FontWeight.Light)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "TIME DENSITY", fontSize = 12.sp, color = Color.Gray, letterSpacing = 4.sp)
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // 3, 4 & 5. Main Title Area
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "ATOM", 
                color = HunterCyan,
                style = AtomHunterWordmarkStyle.copy(
                    fontSize = if (compactLayout) 46.sp else 56.sp,
                    lineHeight = if (compactLayout) 50.sp else 60.sp
                )
            )
            Text(
                text = "HUNTER", 
                color = HunterViolet,
                style = TextStyle(
                    fontFamily = AtomHunterWordmarkStyle.fontFamily,
                    fontWeight = FontWeight.Medium,
                    fontStyle = AtomHunterWordmarkStyle.fontStyle,
                    fontSize = if (compactLayout) 46.sp else 56.sp,
                    letterSpacing = AtomHunterWordmarkStyle.letterSpacing,
                    lineHeight = if (compactLayout) 50.sp else 60.sp,
                    shadow = Shadow(
                        color = HunterViolet.copy(alpha = 0.55f),
                        blurRadius = 16f
                    )
                )
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Powered by Time Density", 
                fontSize = 14.sp, 
                color = HunterViolet,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Hunt the frequency. Capture the atom.", 
                fontSize = 14.sp, 
                color = Color(0xFFF2F5FF).copy(alpha = 0.7f),
                fontWeight = FontWeight.Light,
                letterSpacing = 0.5.sp
            )
        }
        
        Spacer(modifier = Modifier.weight(1.5f))
        
        // 6. Black Hole Entry
        Canvas(modifier = Modifier        .size(if (compactLayout) 128.dp else 160.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2 * scale
            
            // Outer Violet ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, HunterViolet.copy(alpha = 0.3f), Color.Transparent),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
            
            // Middle Cyan ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, HunterCyan.copy(alpha = 0.4f), Color.Transparent),
                    center = center,
                    radius = radius * 0.8f
                ),
                radius = radius * 0.8f,
                center = center
            )
            
            // Inner Gold Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, HunterTurquoise.copy(alpha = 0.32f), Color.Transparent),
                    center = center,
                    radius = radius * 0.6f
                ),
                radius = radius * 0.6f,
                center = center
            )

            // Cyan mechanical ring (spinning)
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

            // Core
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
        GravityGrid(atoms = atoms, resonancePhase = resPhase)

        // Main game canvas for atoms & black hole
        Canvas(modifier = Modifier.fillMaxSize()) {
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
        ) {
            // COMPACT TOP HUD
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 48.dp, bottom = 16.dp),
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
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
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
                    .padding(bottom = 48.dp, start = 24.dp, end = 24.dp)
            ) {
                // ATOM VAULT INDICATOR
                val stats by engine.stabilizedCount.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Element.values().forEach { el ->
                        val count = stats[el] ?: 0
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(el.symbol, color = el.color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(count.toString(), color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
                
                // Header for frequencies
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("TARGET: ${target.toInt()} Hz", color = HunterViolet, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("CARRIER: ${carrier.toInt()} Hz", color = HunterCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth().height(44.dp),
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
                        .height(48.dp)
                )
            }
        }
    }
}

private fun reducedMotionEnabled(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !ValueAnimator.areAnimatorsEnabled()

@Composable
fun GravityGrid(atoms: List<Atom>, resonancePhase: ResonancePhase) {
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
    Canvas(modifier = Modifier.fillMaxSize()) {
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

        val atomMass = if (resonancePhase == ResonancePhase.STABLE) 1.55f else 1f
        val fieldScale = minOf(size.width, size.height) * 0.38f / 300.dp.toPx()
        val lensPositions = atoms.asSequence().filter { !it.absorbed }.take(5)
            .map { Offset(center.x + it.x.dp.toPx() * fieldScale, center.y + it.y.dp.toPx() * fieldScale) }.toList()
        val lensRangeSquared = 90.dp.toPx() * 90.dp.toPx()
        val lineCount = 12
        val segments = 32
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
                        val influence = (if (index == 0) 0.16f else 0.09f) * atomMass /
                            (1f + distanceSquared / lensRangeSquared)
                        dx -= diffX * influence
                        dy -= diffY * influence
                    }
                    val lensWave = if (reducedMotion) 0f else sin(progress * Math.PI * 2 + phase) * 3.dp.toPx()
                    if (axis == 0) dy += lensWave else dx += lensWave
                    val point = Offset(x + dx, y + dy)
                    if (segment == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                }
                drawPath(
                    path,
                    color = if (line % 3 == 0) HunterCyan.copy(alpha = 0.10f) else HunterTurquoise.copy(alpha = 0.055f),
                    style = Stroke(width = if (line % 3 == 0) 1.dp.toPx() else 0.6.dp.toPx())
                )
            }
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
    
    Box(
        modifier = modifier
            .semantics {
                contentDescription = "Carrier frequency"
                stateDescription = "${carrier.toInt()} hertz"
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
                    val rawFreq = minFreq + newX * range
                    
                    val activeFreq = if (target > 0f && abs(rawFreq - target) <= 10f) target else rawFreq
                    onFreqChange(activeFreq)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val trackHeight = 12.dp.toPx()
            val midY = size.height / 2
            
            val trackRect = RoundRect(
                rect = Rect(
                    left = 0f, 
                    top = midY - trackHeight / 2, 
                    right = size.width, 
                    bottom = midY + trackHeight / 2
                ),
                cornerRadius = CornerRadius(trackHeight / 2, trackHeight / 2)
            )
            
            drawPath(
                path = Path().apply { addRoundRect(trackRect) },
                brush = Brush.horizontalGradient(
                colors = listOf(HunterCyan, HunterViolet, HunterTurquoise)
                )
            )
            
            if (target > 0f) {
                val targetPos = ((target - minFreq) / range).coerceIn(0f, 1f)
                val targetX = targetPos * size.width
                val zoneWidth = (50f / range) * size.width
                
                drawRect(
                    color = HunterViolet.copy(alpha = 0.4f),
                    topLeft = Offset(targetX - zoneWidth / 2, midY - trackHeight),
                    size = Size(zoneWidth, trackHeight * 2)
                )
                
                drawLine(
                    color = HunterViolet,
                    start = Offset(targetX, midY - trackHeight * 1.5f),
                    end = Offset(targetX, midY + trackHeight * 1.5f),
                    strokeWidth = 2.dp.toPx()
                )
            }
            
            val thumbX = currentPos * size.width
            val thumbRadius = if (isDragging) 20.dp.toPx() else 16.dp.toPx()
            
            drawCircle(
                color = Color.White.copy(alpha = 0.3f),
                radius = thumbRadius * 1.5f,
                center = Offset(thumbX, midY)
            )
            
            drawCircle(
                color = Color.White,
                radius = thumbRadius,
                center = Offset(thumbX, midY)
            )
            
            drawCircle(
                color = HunterBackground,
                radius = thumbRadius * 0.4f,
                center = Offset(thumbX, midY)
            )
        }
    }
}

@Composable
fun ResultScreen(
    engine: GameEngine,
    exportStatus: String,
    onSavePattern: () -> Unit,
    onExit: () -> Unit
) {
    val sequence by engine.capturedSequence.collectAsState()
    val session by engine.captureSession.collectAsState()
    val stats by engine.stabilizedCount.collectAsState()
    val playingIndex by engine.playingNoteIndex.collectAsState()
    val isMelodyPlaying by engine.isMelodyPlaying.collectAsState()
    val volume by engine.musicVolume.collectAsState()
    val galaxyStars = remember(session) { UniverseLayout.generate(session) }
    val reducedMotion = remember { reducedMotionEnabled() }

    var animTrigger by remember { mutableStateOf(0) }
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(animTrigger) {
        animProgress.snapTo(0f)
        if (reducedMotion) {
            animProgress.snapTo(1f)
        } else {
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 2500, easing = FastOutSlowInEasing)
            )
        }
    }

    val pulseAnim = remember { Animatable(0f) }
    LaunchedEffect(playingIndex) {
        if (playingIndex >= 0) {
            pulseAnim.snapTo(1f)
            if (reducedMotion) pulseAnim.snapTo(0f)
            else pulseAnim.animateTo(0f, animationSpec = tween(350))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Header
        Text(
            text = "ATOM HUNT / COMPLETE",
            fontSize = 26.sp,
            color = HunterCyan,
            style = AtomHunterWordmarkStyle.copy(fontSize = 26.sp, letterSpacing = 2.sp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${sequence.size} ATOMS CAPTURED",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = HunterTurquoise,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Black Hole / Universe Visual Animation Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val maxRadius = minOf(size.width * 0.46f, size.height * 0.48f)
                val progress = animProgress.value
                val pulse = pulseAnim.value
                val collapse = (1f - progress / 0.34f).coerceIn(0.04f, 1f)
                val expansion = if (progress < 0.34f) 0.08f else ((progress - 0.34f) / 0.66f).coerceIn(0f, 1f)
                val radiusScale = if (progress < 0.34f) collapse else 0.18f + expansion * 0.82f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(HunterViolet.copy(alpha = 0.20f + pulse * 0.1f), HunterCyan.copy(alpha = 0.05f), Color.Transparent),
                        center = center,
                        radius = maxRadius
                    ),
                    radius = maxRadius,
                    center = center
                )
                drawCircle(HunterBackground, radius = maxRadius * 0.14f * collapse, center = center)

                val atomStars = galaxyStars.filter { it.isAtom }
                if (atomStars.size > 1) {
                    val constellation = Path()
                    atomStars.forEachIndexed { index, star ->
                        val point = Offset(
                            center.x + (star.x - 0.5f) * maxRadius * 2f * radiusScale,
                            center.y + (star.y - 0.48f) * maxRadius * 2f * radiusScale
                        )
                        if (index == 0) constellation.moveTo(point.x, point.y) else constellation.lineTo(point.x, point.y)
                    }
                    drawPath(constellation, HunterCyan.copy(alpha = 0.18f * expansion), style = Stroke(width = 1.dp.toPx()))
                }

                galaxyStars.forEach { star ->
                    val drift = if (reducedMotion) 0f else progress * 0.04f
                    val point = Offset(
                        center.x + (star.x - 0.5f) * maxRadius * 2f * radiusScale,
                        center.y + (star.y - 0.48f) * maxRadius * 2f * radiusScale
                    )
                    val highlighted = star.isAtom && star.eventIndex == playingIndex
                    val color = galaxyColor(star.elementSymbol)
                    val alpha = star.opacity * if (star.isAtom) 0.72f + expansion * 0.28f else expansion
                    drawCircle(
                        color = color.copy(alpha = (alpha + if (highlighted) pulse * 0.4f else 0f).coerceIn(0f, 1f)),
                        radius = star.size.dp.toPx() * (1f + drift + if (highlighted) pulse * 0.5f else 0f),
                        center = point
                    )
                    if (highlighted) {
                        drawCircle(
                            color = HunterCyan.copy(alpha = 0.75f),
                            radius = star.size.dp.toPx() * (2.3f + pulse),
                            center = point,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }

                if (pulse > 0f) {
                    drawCircle(
                        color = HunterViolet.copy(alpha = pulse * 0.7f),
                        radius = maxRadius * (0.4f + (1f - pulse) * 0.6f),
                        center = center,
                        style = Stroke(width = 3.dp.toPx() * pulse)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "120 BPM  ·  ATOM-FREQUENCY LEAD  ·  INDUSTRIAL PULSE",
            color = HunterMetal,
            fontSize = 10.sp,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Captures, arrangement, and galaxy all share this ordered event sequence.
        Text(
            text = "CAPTURED SEQUENCE",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (sequence.isEmpty()) {
            Text(
                text = "No atoms captured during this hunt.",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        } else {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                itemsIndexed(sequence) { index, elem ->
                    val isPlayingThis = playingIndex == index

                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isPlayingThis) galaxyColor(elem.symbol).copy(alpha = 0.35f) else HunterSurface,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .border(
                                width = if (isPlayingThis) 2.dp else 1.dp,
                                color = if (isPlayingThis) HunterCyan else galaxyColor(elem.symbol).copy(alpha = 0.6f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = elem.symbol,
                                color = if (isPlayingThis) Color.White else galaxyColor(elem.symbol),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${elem.frequency.toInt()}Hz",
                                color = HunterMetal,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. ATOM VAULT BREAKDOWN
        Text(
            text = "ATOM VAULT",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Element.values().forEach { el ->
                val count = stats[el] ?: 0
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = el.symbol,
                        color = el.color,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "×$count",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSavePattern,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = HunterTurquoise)
        ) {
            Text("SAVE SESSION PATTERN", letterSpacing = 1.sp)
        }
        if (exportStatus.isNotEmpty()) {
            Text(exportStatus, color = HunterCyan, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("VOLUME", color = HunterMetal, fontSize = 11.sp, letterSpacing = 1.sp)
            Slider(
                value = volume,
                onValueChange = { engine.setMusicVolume(it) },
                modifier = Modifier.weight(1f).padding(start = 12.dp),
                valueRange = 0f..1f
            )
            Text("${(volume * 100).toInt()}%", color = HunterCyan, fontSize = 11.sp)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            Button(
                onClick = { engine.playResultMelody() },
                enabled = sequence.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isMelodyPlaying) "PLAYING..." else "PLAY MELODY", fontSize = 12.sp)
            }

            Button(
                onClick = { engine.stopResultMelody() },
                enabled = isMelodyPlaying,
                modifier = Modifier.weight(1f)
            ) {
                Text("STOP", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            Button(
                onClick = {
                    animTrigger++
                    engine.replayResultAnimationAndMelody()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("REPLAY", fontSize = 11.sp)
            }

            Button(
                onClick = {
                    engine.stopResultMelody()
                    engine.startGame()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("NEW HUNT", fontSize = 11.sp)
            }

            Button(
                onClick = {
                    engine.stopResultMelody()
                    engine.resetToHome()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("HOME", fontSize = 11.sp)
            }
            Button(
                onClick = onExit,
                modifier = Modifier.weight(1f)
            ) {
                Text("EXIT", fontSize = 11.sp)
            }
        }
    }
}

private fun galaxyColor(symbol: String): Color = when (symbol) {
    "H", "N", "O" -> HunterCyan
    "He", "C" -> HunterViolet
    else -> HunterMetal
}
