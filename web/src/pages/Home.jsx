import React, { useCallback, useEffect, useRef, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import GameCockpit from "@/components/atomhunter/GameCockpit";
import StartScreen from "@/components/atomhunter/StartScreen";
import ResultScreen from "@/components/atomhunter/ResultScreen";

import { ELEMENTS, ELEMENT_KEYS } from "@/lib/rezonator/elements";
import { SESSION_MS, HOLD_MS, toleranceFor } from "@/lib/rezonator/constants";
import { ensureAudio, startDrone, playTargetTone, playConfirm, playCapture, playCarrierTone, setMuted, setVolume, stopAll } from "@/lib/rezonator/audio";

function sessionMs() {
  try {
    const raw = new URLSearchParams(window.location.search).get("t");
    const n = raw ? parseInt(raw, 10) : NaN;
    if (!isNaN(n) && n >= 5 && n <= 600) return n * 1000;
  } catch {}
  return SESSION_MS;
}

function readBestScore() {
  try {
    return Number(window.localStorage.getItem("atom-hunter-best-score")) || 0;
  } catch {
    return 0;
  }
}

export default function Home() {
  const [phase, setPhase] = useState("start");
  const [target, setTarget] = useState(null);
  const [carrier, setCarrier] = useState(320);
  const [progress, setProgress] = useState(0);
  const [transformed, setTransformed] = useState(false);
  const [captured, setCaptured] = useState([]);
  const [counts, setCounts] = useState({});
  const [score, setScore] = useState(0);
  const [bestScore, setBestScore] = useState(readBestScore);
  const [streak, setStreak] = useState(0);
  const [remaining, setRemaining] = useState(SESSION_MS);
  const [captureToken, setCaptureToken] = useState(0);
  const [muted, setMutedState] = useState(false);
  const [volume, setVolumeState] = useState(0.8);

  const carrierRef = useRef(320);
  const targetRef = useRef(null);
  const progressRef = useRef(0);
  const remRef = useRef(SESSION_MS);
  const transformedRef = useRef(false);
  const pendingEndRef = useRef(false);
  const endedRef = useRef(false);
  const spawnIdRef = useRef(0);
  const recordedIdRef = useRef(null);
  const scoreRef = useRef(0);
  const streakRef = useRef(0);
  const runBestStartRef = useRef(bestScore);

  useEffect(() => {
    setMuted(muted);
  }, [muted]);

  useEffect(() => {
    setVolume(volume);
  }, [volume]);

  useEffect(
    () => () => {
      stopAll();
    },
    []
  );

  const spawnTarget = useCallback(() => {
    const key = ELEMENT_KEYS[Math.floor(Math.random() * ELEMENT_KEYS.length)];
    const spec = { element: key, ...ELEMENTS[key], id: ++spawnIdRef.current };
    targetRef.current = spec;
    setTarget(spec);
    progressRef.current = 0;
    setProgress(0);
    transformedRef.current = false;
    setTransformed(false);
    playTargetTone(spec.freq);
  }, []);

  const beginCapture = useCallback(() => {
    const t = targetRef.current;
    if (!t || transformedRef.current) return;
    transformedRef.current = true;
    setTransformed(true);
    playCapture(t.freq);
    setCaptureToken((n) => n + 1);
  }, []);

  const endSession = useCallback(() => {
    if (endedRef.current) return;
    endedRef.current = true;
    stopAll();
    setPhase("result");
  }, []);

  const handleCaptureDone = useCallback(() => {
    const t = targetRef.current;
    if (!t || !transformedRef.current || recordedIdRef.current === t.id) return;
    recordedIdRef.current = t.id;
    const error = Math.abs(carrierRef.current - t.freq);
    const precision = Math.max(0, 1 - error / toleranceFor(t.freq));
    streakRef.current += 1;
    const points = Math.round(100 + precision * 150 + Math.min(streakRef.current - 1, 8) * 25);
    scoreRef.current += points;
    setScore(scoreRef.current);
    setStreak(streakRef.current);
    setCaptured((list) => [...list, t.element]);
    setCounts((c) => ({ ...c, [t.element]: (c[t.element] || 0) + 1 }));
    setBestScore((currentBest) => {
      const nextBest = Math.max(currentBest, scoreRef.current);
      if (nextBest > currentBest) {
        try {
          window.localStorage.setItem("atom-hunter-best-score", String(nextBest));
        } catch {}
      }
      return nextBest;
    });
    if (pendingEndRef.current || remRef.current <= 0) {
      endSession();
      return;
    }
    spawnTarget();
  }, [endSession, spawnTarget]);

  const handleCarrier = useCallback((value) => {
    carrierRef.current = value;
    setCarrier(value);
    playCarrierTone(value);
  }, []);

  const startHunt = useCallback(() => {
    ensureAudio();
    startDrone(110);
    endedRef.current = false;
    pendingEndRef.current = false;
    const ms = sessionMs();
    remRef.current = ms;
    setRemaining(ms);
    carrierRef.current = 320;
    setCarrier(320);
    progressRef.current = 0;
    setProgress(0);
    setCaptured([]);
    setCounts({});
    scoreRef.current = 0;
    streakRef.current = 0;
    setScore(0);
    setStreak(0);
    runBestStartRef.current = readBestScore();
    setBestScore(runBestStartRef.current);
    setCaptureToken(0);
    targetRef.current = null;
    setPhase("playing");
    spawnTarget();

  }, [spawnTarget]);

  useEffect(() => {
    if (phase !== "playing") return undefined;
    let raf = 0;
    let last = performance.now();
    let uiAcc = 0;

    const step = (now) => {
      const elapsed = now - last;
      const dt = Math.min(80, elapsed);
      last = now;
      remRef.current = Math.max(0, remRef.current - elapsed);
      // An expired hunt must not award a new lock when a background tab resumes.
      if (remRef.current <= 0 && !transformedRef.current) {
        endSession();
        return;
      }

      const tgt = targetRef.current;
      if (tgt && !transformedRef.current) {
        const tol = toleranceFor(tgt.freq);
        const within = Math.abs(carrierRef.current - tgt.freq) <= tol;
        const wasStable = progressRef.current >= 0.35;
        const pr = within
          ? Math.min(1, progressRef.current + dt / HOLD_MS)
          : Math.max(0, progressRef.current - (dt * 2) / HOLD_MS);
        progressRef.current = pr;
        if (within && !wasStable && pr >= 0.35) playConfirm(tgt.freq);
        if (pr >= 1) beginCapture();
      }

      uiAcc += dt;
      if (uiAcc >= 80) {
        uiAcc = 0;
        setRemaining(remRef.current);
        setProgress(progressRef.current);
      }

      if (remRef.current <= 0) {
        if (transformedRef.current) {
          pendingEndRef.current = true;
        } else {
          endSession();
          return;
        }
      }

      raf = requestAnimationFrame(step);
    };

    raf = requestAnimationFrame(step);
    return () => cancelAnimationFrame(raf);
  }, [phase, beginCapture, endSession]);

  const goHome = useCallback(() => {
    stopAll();
    endedRef.current = true;
    setPhase("start");
  }, []);

  const tol = target ? toleranceFor(target.freq) : 0;
  const matched = phase === "playing" && !!target && Math.abs(carrier - target.freq) <= tol;
  const stable = matched && progress >= 0.35;
  const gameState = transformed ? "transformed" : stable ? "stable" : matched ? "matched" : "idle";

  return (
    <div className="hunter-app fixed inset-0 overflow-hidden">
      <AnimatePresence mode="wait">
        {phase === "start" && (
          <motion.div
            key="start"
            exit={{ opacity: 0, scale: 1.05 }}
            transition={{ duration: 0.5 }}
            className="absolute inset-0"
          >
            <StartScreen onStart={startHunt} bestScore={bestScore} />
          </motion.div>
        )}

        {phase === "playing" && (
          <motion.div
            key="game"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.45 }}
            className="absolute inset-0"
          >
            <GameCockpit {...{ target, carrier, progress, gameState, captured, remaining, captureToken, muted, volume, score, streak }} onCaptureDone={handleCaptureDone} onCarrier={handleCarrier} tolerance={tol} onToggleMute={() => setMutedState(m => !m)} onVolumeChange={setVolumeState} />
          </motion.div>
        )}

        {phase === "result" && (
          <motion.div
            key="result"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 0.6 }}
            className="absolute inset-0"
          >
            <ResultScreen
              captured={captured}
              counts={counts}
              score={score}
              bestScore={bestScore}
              newRecord={score > runBestStartRef.current}
              onNewHunt={startHunt}
              onHome={goHome}
              muted={muted}
              volume={volume}
              onToggleMute={() => setMutedState((m) => !m)}
              onVolumeChange={setVolumeState}
            />
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}