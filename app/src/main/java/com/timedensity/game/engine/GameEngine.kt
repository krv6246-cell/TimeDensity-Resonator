package com.timedensity.game.engine

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timedensity.game.audio.AudioEngine
import com.timedensity.game.model.Atom
import com.timedensity.game.model.Element
import com.timedensity.game.model.GamePhase
import com.timedensity.resonator.core.ResonancePhase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class GameEngine : ViewModel() {

    private val audioEngine = AudioEngine()
    private val adapter = ResonatorGameAdapter()

    private val _phase = MutableStateFlow(GamePhase.START)
    val phase: StateFlow<GamePhase> = _phase.asStateFlow()

    private val _atoms = MutableStateFlow<List<Atom>>(emptyList())
    val atoms: StateFlow<List<Atom>> = _atoms.asStateFlow()

    private val _carrierFreq = MutableStateFlow(400f)
    val carrierFreq: StateFlow<Float> = _carrierFreq.asStateFlow()

    private val _targetFreq = MutableStateFlow(0f)
    val targetFreq: StateFlow<Float> = _targetFreq.asStateFlow()
    
    private val _matchPercent = MutableStateFlow(0)
    val matchPercent: StateFlow<Int> = _matchPercent.asStateFlow()
    
    private val _resonancePhase = MutableStateFlow(ResonancePhase.IDLE)
    val resonancePhase: StateFlow<ResonancePhase> = _resonancePhase.asStateFlow()
    
    private val _holdProgress = MutableStateFlow(0f)
    val holdProgress: StateFlow<Float> = _holdProgress.asStateFlow()

    private val _timeRemaining = MutableStateFlow(180) // 3 minutes
    val timeRemaining: StateFlow<Int> = _timeRemaining.asStateFlow()

    private val _stabilizedCount = MutableStateFlow<Map<Element, Int>>(emptyMap())
    val stabilizedCount: StateFlow<Map<Element, Int>> = _stabilizedCount.asStateFlow()

    private val _blackHoleFlash = MutableStateFlow(0f)
    val blackHoleFlash: StateFlow<Float> = _blackHoleFlash.asStateFlow()

    private var atomIdCounter = 0
    private var random = Random(42)

    private var gameJob: Job? = null

    // Black Hole relative position in engine coordinates
    private val blackHoleX = 0f
    private val blackHoleY = 0f

    // Track the last active atom to detect changes
    private var lastActiveAtomId: Int? = null

    fun startGame(newSeed: Long? = null) {
        gameJob?.cancel()
        
        val seedToUse = newSeed ?: System.currentTimeMillis()
        _phase.value = GamePhase.PLAYING
        _timeRemaining.value = 180
        _atoms.value = emptyList()
        _stabilizedCount.value = emptyMap()
        _blackHoleFlash.value = 0f
        atomIdCounter = 0
        random = Random(seedToUse)
        lastActiveAtomId = null
        
        adapter.reset()
        audioEngine.start()
        audioEngine.clearTones()
        
        gameJob = viewModelScope.launch {
            launch { timerLoop() }
            launch { gameLoop() }
        }
    }

    fun setCarrierFreq(freq: Float) {
        if (_phase.value == GamePhase.PLAYING) {
            _carrierFreq.value = freq
            audioEngine.setCarrierFrequency(freq)
        }
    }

    private fun startCapture(activeAtom: Atom) {
        val updatedAtoms = _atoms.value.toMutableList()
        val index = updatedAtoms.indexOfFirst { it.id == activeAtom.id }
        if (index != -1) {
            updatedAtoms[index] = activeAtom.copy(
                isCapturing = true,
                captureProgress = 0f,
                startCaptureAngle = activeAtom.orbitAngle,
                startCaptureRadius = activeAtom.orbitRadius
            )
            _atoms.value = updatedAtoms
            
            // Add to vault immediately so HUD updates
            val map = _stabilizedCount.value.toMutableMap()
            map[activeAtom.element] = (map[activeAtom.element] ?: 0) + 1
            _stabilizedCount.value = map
            
            // Play element tone once
            audioEngine.playElementTone(activeAtom.element)
        }
    }

    private suspend fun gameLoop() {
        var lastTime = System.currentTimeMillis()
        var spawnTimer = 0f
        var timeElapsed = 0f

        while (_phase.value == GamePhase.PLAYING) {
            val currentTime = System.currentTimeMillis()
            val dt = ((currentTime - lastTime) / 1000f).coerceIn(0f, 0.1f)
            lastTime = currentTime
            timeElapsed += dt

            // Flash decay
            if (_blackHoleFlash.value > 0f) {
                _blackHoleFlash.value = (_blackHoleFlash.value - dt * 2f).coerceAtLeast(0f)
            }

            // Spawn Atoms: spawn a new one if no active (non-capturing) atoms exist
            spawnTimer += dt
            if (spawnTimer > 1f && _atoms.value.count { !it.isCapturing && !it.absorbed } == 0) {
                spawnTimer = 0f
                spawnAtom()
            }

            // Move & Capture atoms
            val currentAtoms = mutableListOf<Atom>()
            for (atom in _atoms.value) {
                if (atom.absorbed) {
                    currentAtoms.add(atom)
                    continue
                }

                if (atom.isCapturing) {
                    // Spiral capture duration ~1000ms
                    val progress = atom.captureProgress + dt / 1.0f 
                    if (progress >= 1.0f) {
                        // Animation finished, trigger flash and remove
                        _blackHoleFlash.value = 1f
                    } else {
                        // Spiral into the center
                        // 1.5 rotations during capture
                        val spiralAngle = atom.startCaptureAngle + progress * (Math.PI.toFloat() * 3f)
                        // Radius drops to 0 using a slight ease-in
                        val spiralRadius = atom.startCaptureRadius * (1f - progress * progress)
                        
                        val newX = cos(spiralAngle) * spiralRadius
                        val newY = sin(spiralAngle) * spiralRadius
                        
                        currentAtoms.add(atom.copy(
                            x = newX, 
                            y = newY,
                            orbitAngle = spiralAngle,
                            orbitRadius = spiralRadius,
                            captureProgress = progress
                        ))
                    }
                } else {
                    // Normal Orbital drift around the center
                    val newAngle = atom.orbitAngle + atom.orbitSpeed * dt
                    val newX = cos(newAngle) * atom.orbitRadius
                    val newY = sin(newAngle) * atom.orbitRadius
                    
                    currentAtoms.add(atom.copy(
                        x = newX,
                        y = newY,
                        orbitAngle = newAngle
                    ))
                }
            }
            _atoms.value = currentAtoms

            // ===== RESONANCE CORE UPDATE =====
            val activeAtom = currentAtoms.firstOrNull { !it.isCapturing && !it.absorbed }

            // Detect active atom change
            val activeAtomChanged = activeAtom?.id != lastActiveAtomId

            if (activeAtomChanged) {
                Log.d(
                    "AtomHunter",
                    "ACTIVE_ATOM_CHANGED: lastId=$lastActiveAtomId newId=${activeAtom?.id} " +
                        "symbol=${activeAtom?.element?.symbol} target=${activeAtom?.element?.frequency}"
                )

                // Reset adapter only once per atom change
                if (activeAtom != null) {
                    adapter.reset()
                    Log.d(
                        "AtomHunter",
                        "adapter.reset() called: atomId=${activeAtom.id}"
                    )
                }

                lastActiveAtomId = activeAtom?.id
            }

            if (activeAtom != null) {
                _targetFreq.value = activeAtom.element.frequency
                audioEngine.setTargetFrequency(activeAtom.element.frequency)

                Log.d(
                    "AtomHunter",
                    "before adapter.update: atomId=${activeAtom.id} " +
                        "target=${activeAtom.element.frequency} " +
                        "carrier=${_carrierFreq.value} " +
                        "phase=${_resonancePhase.value}"
                )

                val snapshot = adapter.update(activeAtom.element.frequency, _carrierFreq.value, dt)
                _matchPercent.value = (snapshot.precision * 100).toInt()
                _resonancePhase.value = snapshot.phase
                _holdProgress.value = (snapshot.heldForMs / 800f).coerceIn(0f, 1f)

                Log.d(
                    "AtomHunter",
                    "adapter.update result: atomId=${activeAtom.id} " +
                        "phase=${snapshot.phase} " +
                        "heldForMs=${snapshot.heldForMs} " +
                        "precision=${snapshot.precision}"
                )
                
                if (snapshot.phase == ResonancePhase.STABLE) {
                    Log.d(
                        "AtomHunter",
                        "STABLE reached: atomId=${activeAtom.id} calling transform()"
                    )
                    adapter.transform()
                    startCapture(activeAtom)
                }
            } else {
                _targetFreq.value = 0f
                audioEngine.setTargetFrequency(0f)
                _matchPercent.value = 0
                _resonancePhase.value = ResonancePhase.IDLE
                _holdProgress.value = 0f
                // DO NOT reset adapter here; reset happens only when activeAtom changes
            }

            delay(16) // ~60fps
        }
    }

    private fun spawnAtom() {
        val element = Element.values()[random.nextInt(Element.values().size)]
        
        // Spawn atom in an orbit
        val startAngle = random.nextFloat() * Math.PI.toFloat() * 2f
        // Ensure radius is far enough to be clearly outside the black hole
        val startRadius = 150f + random.nextFloat() * 150f
        
        // Add random slight rotational speed
        val startSpeed = (if (random.nextBoolean()) 1f else -1f) * (0.2f + random.nextFloat() * 0.3f)
        
        val startX = cos(startAngle) * startRadius
        val startY = sin(startAngle) * startRadius

        val newAtom = Atom(
            id = atomIdCounter++,
            element = element,
            x = startX,
            y = startY,
            orbitAngle = startAngle,
            orbitRadius = startRadius,
            orbitSpeed = startSpeed
        )
        _atoms.value = _atoms.value + newAtom
        
        Log.d(
            "AtomHunter",
            "spawnAtom: id=${newAtom.id} symbol=${element.symbol} target=${element.frequency}"
        )
    }
    
    private suspend fun timerLoop() {
        while (_phase.value == GamePhase.PLAYING && _timeRemaining.value > 0) {
            delay(1000)
            _timeRemaining.value -= 1
        }
        if (_timeRemaining.value <= 0 && _phase.value == GamePhase.PLAYING) {
            _phase.value = GamePhase.COLLAPSE
            startCollapse()
        }
    }

    private fun startCollapse() {
        viewModelScope.launch {
            // Wait out any capturing animations
            delay(2000)
            _phase.value = GamePhase.RESULT
        }
    }
    
    fun resetToHome() {
        gameJob?.cancel()
        _phase.value = GamePhase.START
        audioEngine.stop()
        adapter.reset()
        lastActiveAtomId = null
    }
    
    override fun onCleared() {
        super.onCleared()
        gameJob?.cancel()
        audioEngine.stop()
    }
}
