package com.timedensity.game.ui

import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timedensity.game.engine.GameEngine
import com.timedensity.game.model.Atom
import com.timedensity.game.model.Element
import com.timedensity.game.model.GamePhase
import com.timedensity.resonator.core.ResonancePhase
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun RezonatorApp(engine: GameEngine) {
    val phase by engine.phase.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050816)) // Deep black background
    ) {
        when (phase) {
            GamePhase.START -> StartScreen(onStart = { 
                engine.startGame()
            })
            GamePhase.PLAYING, GamePhase.COLLAPSE -> GameScreen(engine)
            GamePhase.RESULT -> ResultScreen(engine)
        }
    }
}

@Composable
fun StartScreen(onStart: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blackhole_pulse"
    )
    val rotate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "blackhole_rotate"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 48.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1 & 2. Logo and Subtitle
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "∞", fontSize = 32.sp, color = Color(0xFF00D9FF), fontWeight = FontWeight.Light)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "TIME DENSITY", fontSize = 12.sp, color = Color.Gray, letterSpacing = 4.sp)
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // 3, 4 & 5. Main Title Area
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "ATOM", 
                fontSize = 56.sp, 
                color = Color(0xFF00D9FF), 
                fontWeight = FontWeight.Medium, 
                letterSpacing = 6.sp,
                lineHeight = 60.sp
            )
            Text(
                text = "HUNTER", 
                fontSize = 56.sp, 
                color = Color(0xFFB04CFF), 
                fontWeight = FontWeight.Light, 
                letterSpacing = 6.sp,
                lineHeight = 60.sp,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color(0xFFB04CFF).copy(alpha = 0.8f),
                        blurRadius = 16f
                    )
                )
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Powered by Time Density", 
                fontSize = 14.sp, 
                color = Color(0xFFB04CFF),
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
        Canvas(modifier = Modifier
            .size(160.dp)
            .clickable { onStart() }
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2 * scale
            
            // Outer Violet ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color(0xFFB04CFF).copy(alpha = 0.3f), Color.Transparent),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
            
            // Middle Cyan ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color(0xFF00D9FF).copy(alpha = 0.4f), Color.Transparent),
                    center = center,
                    radius = radius * 0.8f
                ),
                radius = radius * 0.8f,
                center = center
            )
            
            // Inner Gold Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color(0xFFFFBF00).copy(alpha = 0.5f), Color.Transparent),
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
                color = Color(0xFF00D9FF).copy(alpha = 0.8f),
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
                color = Color(0xFF050816), // Match deep black background
                radius = radius * 0.45f,
                center = center
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun GameScreen(engine: GameEngine) {
    val atoms by engine.atoms.collectAsState()
    val time by engine.timeRemaining.collectAsState()
    val match by engine.matchPercent.collectAsState()
    val carrier by engine.carrierFreq.collectAsState()
    val target by engine.targetFreq.collectAsState()
    val phase by engine.phase.collectAsState()
    val resPhase by engine.resonancePhase.collectAsState()
    val flash by engine.blackHoleFlash.collectAsState()
    
    val bhPulse by rememberInfiniteTransition("bhPulse").animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
        label = "bhPulse"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        ParticleField()

        // Main game canvas for atoms & black hole
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            
            // Draw Black Hole exactly in the center
            val bhCenter = Offset(centerX, centerY)
            
            // Flash glow
            if (flash > 0f) {
                // Expanding gold ring
                drawCircle(
                    color = Color(0xFFFFBF00).copy(alpha = flash),
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
                        color = Color(0xFFFFBF00).copy(alpha = flash),
                        radius = 4.dp.toPx() * flash,
                        center = Offset(bhCenter.x + cos(angle) * dist, bhCenter.y + sin(angle) * dist)
                    )
                }
            }
            
            // Deep violet inner glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFB04CFF).copy(alpha = 0.4f), Color.Transparent),
                    center = bhCenter,
                    radius = 80.dp.toPx() * bhPulse
                ),
                center = bhCenter,
                radius = 80.dp.toPx() * bhPulse
            )
            
            // Event horizon
            drawCircle(color = Color(0xFF050816), radius = 35.dp.toPx(), center = bhCenter)
            
            // Rotating rings
            // Speed up rotation when flash > 0
            val currentRotation = (System.currentTimeMillis() % 10000L) / 10000f * 360f
            val flashRotationBonus = (1f - flash) * 180f
            
            drawContext.canvas.nativeCanvas.save()
            drawContext.canvas.nativeCanvas.rotate(currentRotation + flashRotationBonus, bhCenter.x, bhCenter.y)
            drawCircle(
                color = Color(0xFF00D9FF).copy(alpha = 0.6f + flash * 0.4f),
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
                color = Color(0xFFB04CFF).copy(alpha = 0.6f + flash * 0.4f),
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
                color = Color(0xFF00D9FF).copy(alpha = 0.2f + flash * 0.3f), 
                radius = 40.dp.toPx() * bhPulse, 
                center = bhCenter,
                style = Stroke(width = 4.dp.toPx())
            )
            
            // Draw Atoms
            for (atom in atoms) {
                if (atom.absorbed) continue
                val atomCenter = Offset(centerX + atom.x.dp.toPx(), centerY + atom.y.dp.toPx())
                
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
                            -atom.orbitRadius.dp.toPx(), -atom.orbitRadius.dp.toPx(),
                            atom.orbitRadius.dp.toPx(), atom.orbitRadius.dp.toPx()
                        )
                        // Simple arc trail behind the atom
                        drawContext.canvas.nativeCanvas.drawArc(rectF, Math.toDegrees(atom.orbitAngle.toDouble()).toFloat() - 45f, 45f, false, paint)
                        drawContext.canvas.nativeCanvas.restore()
                    }
                    
                    drawCircle(
                        color = Color(0xFFFFBF00).copy(alpha = alpha),
                        radius = 28.dp.toPx() * scale,
                        center = atomCenter,
                        style = Stroke(width = 2.dp.toPx() * scale)
                    )
                } else {
                    // Slight halo for active atom
                    drawCircle(
                        color = Color(0xFF00D9FF).copy(alpha = 0.3f),
                        radius = 22.dp.toPx(),
                        center = atomCenter,
                        style = Stroke(width = 1.dp.toPx())
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
                        ResonancePhase.MATCHED -> Color(0xFF00D9FF) // Electric Cyan
                        ResonancePhase.STABLE -> Color(0xFFFFBF00) // Gold
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
                    Text("$count", color = Color(0xFFFFBF00), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (phase == GamePhase.PLAYING || phase == GamePhase.COLLAPSE) {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    WaveformDisplay(target = target, carrier = carrier, resPhase = resPhase)
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
                    Text("TARGET: ${target.toInt()} Hz", color = Color(0xFFB04CFF), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("CARRIER: ${carrier.toInt()} Hz", color = Color(0xFF00D9FF), fontSize = 14.sp, fontWeight = FontWeight.Bold)
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

@Composable
fun ParticleField() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val random = Random(123)
        for (i in 0..100) {
            val x = random.nextFloat() * size.width
            val y = random.nextFloat() * size.height
            val color = if (random.nextBoolean()) Color(0xFFFF7F50) else Color(0xFF708090)
            drawCircle(
                color = color.copy(alpha = random.nextFloat() * 0.5f),
                radius = random.nextFloat() * 3.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun WaveformDisplay(target: Float, carrier: Float, resPhase: ResonancePhase = ResonancePhase.IDLE) {
    val phase by rememberInfiniteTransition("wave").animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "phase"
    )
    
    val targetColor = Color(0xFFB026FF) // Neon Violet
    val carrierColor = Color(0xFF00FFFF) // Electric Cyan
    
    val glowColor = when(resPhase) {
        ResonancePhase.STABLE -> Color(0xFFFFBF00) // Gold
        ResonancePhase.TRANSFORMED -> Color.White
        else -> carrierColor
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .background(Color(0xFF050505), shape = RoundedCornerShape(8.dp))
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
            drawLine(Color.DarkGray.copy(alpha = 0.3f), Offset(0f, midY), Offset(width, midY), 1.dp.toPx())
            drawLine(Color.DarkGray.copy(alpha = 0.3f), Offset(width / 2, 0f), Offset(width / 2, size.height), 1.dp.toPx())
            
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
                    colors = listOf(Color(0xFF00D9FF), Color(0xFFB04CFF), Color(0xFFFFBF00))
                )
            )
            
            if (target > 0f) {
                val targetPos = ((target - minFreq) / range).coerceIn(0f, 1f)
                val targetX = targetPos * size.width
                val zoneWidth = (50f / range) * size.width
                
                drawRect(
                    color = Color(0xFFB04CFF).copy(alpha = 0.4f),
                    topLeft = Offset(targetX - zoneWidth / 2, midY - trackHeight),
                    size = Size(zoneWidth, trackHeight * 2)
                )
                
                drawLine(
                    color = Color(0xFFB04CFF),
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
                color = Color(0xFF050816),
                radius = thumbRadius * 0.4f,
                center = Offset(thumbX, midY)
            )
        }
    }
}

@Composable
fun ResultScreen(engine: GameEngine) {
    val sequence by engine.capturedSequence.collectAsState()
    val stats by engine.stabilizedCount.collectAsState()
    val playingIndex by engine.playingNoteIndex.collectAsState()
    val isMelodyPlaying by engine.isMelodyPlaying.collectAsState()

    var animTrigger by remember { mutableStateOf(0) }
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(animTrigger) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2500, easing = FastOutSlowInEasing)
        )
    }

    val pulseAnim = remember { Animatable(0f) }
    LaunchedEffect(playingIndex) {
        if (playingIndex >= 0) {
            pulseAnim.snapTo(1f)
            pulseAnim.animateTo(0f, animationSpec = tween(350))
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
            text = "ATOM HUNT COMPLETE",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00D9FF),
            letterSpacing = 3.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${sequence.size} ATOMS CAPTURED",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFFFBF00),
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Black Hole / Universe Visual Animation Canvas
        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val maxRadius = size.width / 2
                val progress = animProgress.value
                val pulse = pulseAnim.value

                if (progress < 0.7f) {
                    val compressFactor = if (progress < 0.4f) 1f else 1f - ((progress - 0.4f) / 0.3f)
                    val baseRadius = maxRadius * 0.7f * compressFactor

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFB04CFF).copy(alpha = 0.5f * compressFactor), Color.Transparent),
                            center = center,
                            radius = maxRadius
                        ),
                        radius = maxRadius,
                        center = center
                    )

                    drawCircle(
                        color = Color(0xFF050816),
                        radius = baseRadius * 0.5f,
                        center = center
                    )

                    val atomCount = sequence.size.coerceAtMost(12)
                    for (i in 0 until atomCount) {
                        val angle = (i * (2 * Math.PI / atomCount) + progress * 4 * Math.PI).toFloat()
                        val orbitR = baseRadius * (0.6f + 0.3f * sin(i * 1.5).toFloat())
                        val x = center.x + cos(angle) * orbitR
                        val y = center.y + sin(angle) * orbitR
                        val elem = sequence[i % sequence.size]

                        drawCircle(
                            color = elem.color,
                            radius = 6.dp.toPx() * compressFactor,
                            center = Offset(x, y)
                        )
                    }
                } else {
                    val expandFactor = (progress - 0.7f) / 0.3f

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 1f - expandFactor),
                                Color(0xFFFFBF00).copy(alpha = 0.8f * (1f - expandFactor)),
                                Color(0xFFB04CFF).copy(alpha = 0.5f * (1f - expandFactor)),
                                Color.Transparent
                            ),
                            center = center,
                            radius = maxRadius * (0.3f + expandFactor * 1.2f)
                        ),
                        radius = maxRadius * (0.3f + expandFactor * 1.2f),
                        center = center
                    )

                    drawCircle(
                        color = Color(0xFF00D9FF).copy(alpha = 1f - expandFactor),
                        radius = maxRadius * expandFactor,
                        center = center,
                        style = Stroke(width = 4.dp.toPx() * (1f - expandFactor))
                    )

                    val starRandom = Random(42)
                    for (s in 0..24) {
                        val angle = starRandom.nextFloat() * 2f * Math.PI.toFloat()
                        val dist = maxRadius * expandFactor * (0.3f + starRandom.nextFloat() * 0.7f)
                        val starColor = if (s % 2 == 0) Color(0xFF00D9FF) else Color(0xFFFFBF00)
                        drawCircle(
                            color = starColor.copy(alpha = expandFactor),
                            radius = (2 + starRandom.nextFloat() * 3).dp.toPx(),
                            center = Offset(center.x + cos(angle) * dist, center.y + sin(angle) * dist)
                        )
                    }
                }

                if (pulse > 0f) {
                    drawCircle(
                        color = Color(0xFFFFBF00).copy(alpha = pulse * 0.8f),
                        radius = maxRadius * (0.4f + (1f - pulse) * 0.6f),
                        center = center,
                        style = Stroke(width = 3.dp.toPx() * pulse)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. CAPTURED SEQUENCE IN ACTUAL ORDER
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
                                color = if (isPlayingThis) elem.color.copy(alpha = 0.35f) else Color(0xFF0D1329),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .border(
                                width = if (isPlayingThis) 2.dp else 1.dp,
                                color = if (isPlayingThis) Color(0xFFFFBF00) else elem.color.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = elem.symbol,
                                color = if (isPlayingThis) Color.White else elem.color,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${elem.frequency.toInt()}Hz",
                                color = Color.White.copy(alpha = 0.8f),
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

        // 5. BUTTON CONTROLS
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
        }
    }
}
