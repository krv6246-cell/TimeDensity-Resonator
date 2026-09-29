package com.timedensity.game.engine

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

    private val _timeRemaining = MutableStateFlow(120) // 2 minutes
    val timeRemaining: StateFlow<Int> = _timeRemaining.asStateFlow()

    private val _stabilizedCount = MutableStateFlow<Map<Element, Int>>(emptyMap())
    val stabilizedCount: StateFlow<Map<Element, Int>> = _stabilizedCount.asStateFlow()

    private val _capturedSequence = MutableStateFlow<List<Element>>(emptyList())
    val capturedSequence: StateFlow<List<Element>> = _capturedSequence.asStateFlow()

    private val _playingNoteIndex = MutableStateFlow(-1)
    val playingNoteIndex: StateFlow<Int> = _playingNoteIndex.asStateFlow()

    private val _isMelodyPlaying = MutableStateFlow(false)
    val isMelodyPlaying: StateFlow<Boolean> = _isMelodyPlaying.asStateFlow()

    private val _blackHoleFlash = MutableStateFlow(0f)
    val blackHoleFlash: StateFlow<Float> = _blackHoleFlash.asStateFlow()

    private var atomIdCounter = 0
    private var random = Random(42)

    private var gameJob: Job? = null
    private var melodyJob: Job? = null

    private var lastActiveAtomId: Int? = null

    fun startGame(newSeed: Long? = null) {
        gameJob?.cancel()
        stopResultMelody()

        val seedToUse = newSeed ?: System.currentTimeMillis()
        _phase.value = GamePhase.PLAYING
        _timeRemaining.value = 120
        _atoms.value = emptyList()
        _stabilizedCount.value = emptyMap()
        _capturedSequence.value = emptyList()
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

            // Add to count map
            val map = _stabilizedCount.value.toMutableMap()
            map[activeAtom.element] = (map[activeAtom.element] ?: 0) + 1
            _stabilizedCount.value = map

            // Append to actual sequence order
            val list = _capturedSequence.value.toMutableList()
            list.add(activeAtom.element)
            _capturedSequence.value = list

            // Play element tone once
            audioEngine.playElementTone(activeAtom.element)
        }

        adapter.reset()
    }

    private suspend fun gameLoop() {
        var lastTime = System.currentTimeMillis()
        var spawnTimer = 0f

        while (_phase.value == GamePhase.PLAYING) {
            val currentTime = System.currentTimeMillis()
            val dt = ((currentTime - lastTime) / 1000f).coerceIn(0f, 0.1f)
            lastTime = currentTime

            // Flash decay
            if (_blackHoleFlash.value > 0f) {
                _blackHoleFlash.value = (_blackHoleFlash.value - dt * 2f).coerceAtLeast(0f)
            }

            // Spawn Atoms: spawn a new one if no active (non-capturing) atoms exist
            if (_atoms.value.count { !it.isCapturing && !it.absorbed } == 0) {
                spawnTimer += dt
                if (spawnTimer > 1f) {
                    spawnTimer = 0f
                    spawnAtom()
                }
            } else {
                spawnTimer = 0f
            }

            // Move & Capture atoms
            val currentAtoms = mutableListOf<Atom>()
            for (atom in _atoms.value) {
                if (atom.absorbed) {
                    continue
                }

                if (atom.isCapturing) {
                    // Spiral capture duration ~1000ms
                    val progress = atom.captureProgress + dt / 1.0f
                    if (progress >= 1.0f) {
                        _blackHoleFlash.value = 1f
                        currentAtoms.add(atom.copy(absorbed = true))
                    } else {
                        // Spiral into the center
                        val spiralAngle = atom.startCaptureAngle + progress * (Math.PI.toFloat() * 3f)
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

            val activeAtomChanged = activeAtom?.id != lastActiveAtomId
            if (activeAtomChanged) {
                if (activeAtom != null) {
                    adapter.reset()
                }
                lastActiveAtomId = activeAtom?.id
            }

            if (activeAtom != null) {
                _targetFreq.value = activeAtom.element.frequency
                audioEngine.setTargetFrequency(activeAtom.element.frequency)

                val snapshot = adapter.update(activeAtom.element.frequency, _carrierFreq.value, dt)
                _matchPercent.value = (snapshot.precision * 100).toInt()
                _resonancePhase.value = snapshot.phase
                _holdProgress.value = (snapshot.heldForMs / 800f).coerceIn(0f, 1f)

                if (snapshot.phase == ResonancePhase.STABLE) {
                    adapter.transform()
                    startCapture(activeAtom)
                }
            } else {
                _targetFreq.value = 0f
                audioEngine.setTargetFrequency(0f)
                _matchPercent.value = 0
                _resonancePhase.value = ResonancePhase.IDLE
                _holdProgress.value = 0f
            }

            delay(16) // ~60fps
        }
    }

    private fun spawnAtom() {
        val element = Element.values()[random.nextInt(Element.values().size)]

        val startAngle = random.nextFloat() * Math.PI.toFloat() * 2f
        val startRadius = 150f + random.nextFloat() * 150f
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
            audioEngine.setTargetFrequency(0f)
            delay(2000)
            _phase.value = GamePhase.RESULT
        }
    }

    fun playResultMelody() {
        val sequence = _capturedSequence.value
        if (sequence.isEmpty()) return

        stopResultMelody()
        audioEngine.start()

        melodyJob = viewModelScope.launch {
            _isMelodyPlaying.value = true
            audioEngine.playMelodySequence(sequence) { index, _ ->
                _playingNoteIndex.value = index
            }
            _playingNoteIndex.value = -1
            _isMelodyPlaying.value = false
        }
    }

    fun stopResultMelody() {
        melodyJob?.cancel()
        melodyJob = null
        audioEngine.stopMelody()
        _playingNoteIndex.value = -1
        _isMelodyPlaying.value = false
    }

    fun replayResultAnimationAndMelody() {
        stopResultMelody()
        playResultMelody()
    }

    fun resetToHome() {
        gameJob?.cancel()
        stopResultMelody()
        _phase.value = GamePhase.START
        audioEngine.stop()
        adapter.reset()
        lastActiveAtomId = null
    }

    override fun onCleared() {
        super.onCleared()
        gameJob?.cancel()
        stopResultMelody()
        audioEngine.stop()
    }
}
