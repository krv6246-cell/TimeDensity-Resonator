package com.timedensity.game.ui

import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
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
        // Just a simple static cosmic field for now
        // A real implementation would animate these
        val random = Random(123)
        for (i in 0..100) {
            val x = random.nextFloat() * size.width
            val y = random.nextFloat() * size.height
            val color = if (random.nextBoolean()) Color(0xFFFF7F50) else Color(0xFF708090) // red/orange or blue/gray
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
                // Simplified visualization of frequency
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
                    
                    // Magnetic snap feedback at 10Hz
                    val activeFreq = if (target > 0f && abs(rawFreq - target) <= 10f) target else rawFreq
                    onFreqChange(activeFreq)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val trackHeight = 12.dp.toPx()
            val midY = size.height / 2
            
            // Gradient track
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
            
            // Target frequency visual zone
            if (target > 0f) {
                val targetPos = ((target - minFreq) / range).coerceIn(0f, 1f)
                val targetX = targetPos * size.width
                
                // Entry tolerance is 25 Hz. So total visual width is 50 Hz.
                val zoneWidth = (50f / range) * size.width
                
                // Draw target area
                drawRect(
                    color = Color(0xFFB04CFF).copy(alpha = 0.4f),
                    topLeft = Offset(targetX - zoneWidth / 2, midY - trackHeight),
                    size = Size(zoneWidth, trackHeight * 2)
                )
                
                // Draw exact target line
                drawLine(
                    color = Color(0xFFB04CFF),
                    start = Offset(targetX, midY - trackHeight * 1.5f),
                    end = Offset(targetX, midY + trackHeight * 1.5f),
                    strokeWidth = 2.dp.toPx()
                )
            }
            
            // Thumb
            val thumbX = currentPos * size.width
            val thumbRadius = if (isDragging) 20.dp.toPx() else 16.dp.toPx()
            
            // Thumb glow
            drawCircle(
                color = Color.White.copy(alpha = 0.3f),
                radius = thumbRadius * 1.5f,
                center = Offset(thumbX, midY)
            )
            
            // Thumb core
            drawCircle(
                color = Color.White,
                radius = thumbRadius,
                center = Offset(thumbX, midY)
            )
            
            // Center dot
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
    val stats by engine.stabilizedCount.collectAsState()
    
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("ATOM VAULT", fontSize = 48.sp, color = Color.White, fontWeight = FontWeight.Light, letterSpacing = 4.sp)
        Spacer(modifier = Modifier.height(32.dp))
        
        // Abstract representation
        Canvas(modifier = Modifier.size(200.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            stats.forEach { (element, count) ->
                drawCircle(
                    color = element.color.copy(alpha = 0.3f),
                    radius = (40 + count * 10).dp.toPx(),
                    center = center
                )
            }
            drawCircle(color = Color.White, radius = 10.dp.toPx(), center = center)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Element.values().forEach { element ->
            val count = stats[element] ?: 0
            Text("${element.symbol} × $count", color = element.color, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Text("COLLECTION READY FOR TRANSFORMATION", color = Color(0xFFFFBF00), fontSize = 14.sp, letterSpacing = 2.sp)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row {
            Button(onClick = { /* SAVE MELODY */ }) { Text("SAVE COLLECTION") }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = { /* SAVE IMAGE */ }) { Text("SAVE IMAGE") }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row {
            Button(onClick = { 
                engine.startGame()
            }) { Text("NEW HUNT") }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = { engine.resetToHome() }) { Text("HOME") }
        }
    }
}
