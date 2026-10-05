import industrialHit from '@/lib/rezonator/industrial';

let sequencePlayback = null;
export function getSequenceFrame() {
  if (!ctx || ctx.state !== 'running' || !sequencePlayback) return { index: -1, pulse: 0 };
  const elapsed = ctx.currentTime - sequencePlayback.start;
  if (elapsed < 0) return { index: -1, pulse: 0 };
  return { index: Math.min(sequencePlayback.length - 1, Math.floor(elapsed / sequencePlayback.spacing)), pulse: Math.exp(-((elapsed % sequencePlayback.beat) / sequencePlayback.beat) * 7) };
}

let ctx = null;
let master = null;
let limiter = null;
let muted = false;
let volume = 0.8;

let droneNodes = null;
const live = new Set();
const seq = new Set();
let seqTimer = null;
let sequenceRequest = 0;
let lastCarrierTime = -1;

function applyGain() {
  if (!master || !ctx) return;
  try {
    master.gain.setTargetAtTime(muted ? 0 : volume * 0.55, ctx.currentTime, 0.05);
  } catch {}
}

export function ensureAudio() {
  try {
    if (typeof window === "undefined") return null;
    if (!ctx) {
      const AC = window.AudioContext || /** @type {any} */ (window).webkitAudioContext;
      if (!AC) return null;
      ctx = new AC();
      limiter = ctx.createDynamicsCompressor();
      limiter.threshold.value = -10;
      limiter.knee.value = 8;
      limiter.ratio.value = 12;
      limiter.attack.value = 0.004;
      limiter.release.value = 0.25;
      master = ctx.createGain();
      master.gain.value = muted ? 0 : volume * 0.55;
      master.connect(limiter);
      limiter.connect(ctx.destination);
    }
    if (ctx.state === "suspended") ctx.resume().catch(() => {});
    return ctx;
  } catch {
    return null;
  }
}

export function setMuted(value) {
  muted = !!value;
  applyGain();
}

export function isMuted() {
  return muted;
}

export function setVolume(value) {
  volume = Math.max(0, Math.min(1, value));
  applyGain();
}

export function getVolume() {
  return volume;
}

function voice(freq, opts = {}) {
  const c = ctx;
  if (!c || !isFinite(freq) || freq <= 0 || live.size + seq.size >= 40) return null;
  const {
    start = 0,
    duration = 1,
    gain = 0.12,
    attack = 0.04,
    release = null,
    harmonic = 0.07,
    filterHz = 1600,
    glideTo = null,
    bucket = live,
  } = opts;

  const t = c.currentTime + start;
  const rel = release != null ? release : Math.min(0.9, Math.max(0.4, duration * 0.45));
  const peak = Math.max(0.0002, gain);

  const g = c.createGain();
  g.gain.setValueAtTime(0.0001, t);
  g.gain.exponentialRampToValueAtTime(peak, t + attack);
  g.gain.setValueAtTime(peak, t + Math.max(attack, duration - rel));
  g.gain.exponentialRampToValueAtTime(0.0001, t + duration);

  const filter = c.createBiquadFilter();
  filter.type = "lowpass";
  filter.frequency.value = filterHz;
  filter.Q.value = 0.5;

  const o1 = c.createOscillator();
  o1.type = "sine";
  o1.frequency.setValueAtTime(freq, t);
  if (glideTo) o1.frequency.exponentialRampToValueAtTime(Math.max(20, glideTo), t + duration);

  const o2 = c.createOscillator();
  o2.type = "triangle";
  o2.frequency.value = freq * 2;
  o2.detune.value = 6;
  const g2 = c.createGain();
  g2.gain.value = harmonic;

  o1.connect(g);
  o2.connect(g2);
  g2.connect(g);
  g.connect(filter);
  filter.connect(master);

  const end = t + duration + 0.06;
  try {
    o1.start(t);
    o2.start(t);
    o1.stop(end);
    o2.stop(end);
  } catch {}

  const entry = {
    nodes: [o1, o2],
    gain: g,
    cleanup: () => {
      try {
        g.disconnect();
        g2.disconnect();
        filter.disconnect();
      } catch {}
    },
  };
  bucket.add(entry);
  o1.onended = () => {
    bucket.delete(entry);
    entry.cleanup();
  };
  return entry;
}

export function playCarrierTone(freq) {
  if (!ctx || ctx.currentTime - lastCarrierTime < 0.12) return;
  lastCarrierTime = ctx.currentTime;
  voice(freq, { duration: 0.2, gain: 0.018, attack: 0.025, release: 0.1, harmonic: 0.02 });
}

export function playTargetTone(freq) {
  if (!ensureAudio()) return;
  voice(freq, { duration: 0.9, gain: 0.05, attack: 0.06, filterHz: 1400 });
}

export function playConfirm(freq) {
  if (!ensureAudio()) return;
  voice(freq, { duration: 1.2, gain: 0.09, attack: 0.03 });
  voice(freq * 1.5, { start: 0.06, duration: 1.1, gain: 0.045, attack: 0.03, harmonic: 0.05 });
}

export function playCapture(freq) {
  if (!ensureAudio()) return;
  voice(freq, { duration: 0.95, gain: 0.13, attack: 0.02, glideTo: freq * 0.35 });
  voice(freq * 0.5, { start: 0.05, duration: 1.25, gain: 0.08, attack: 0.06, glideTo: freq * 0.25, filterHz: 1200 });
}

export function startDrone(freq = 110) {
  if (!ensureAudio() || droneNodes) return;
  const c = ctx;
  const t = c.currentTime;

  const g = c.createGain();
  g.gain.setValueAtTime(0.0001, t);
  g.gain.exponentialRampToValueAtTime(0.05, t + 1.8);

  const filter = c.createBiquadFilter();
  filter.type = "lowpass";
  filter.frequency.value = 320;
  filter.Q.value = 0.4;

  const o1 = c.createOscillator();
  o1.type = "sine";
  o1.frequency.value = freq * 0.5;
  const o2 = c.createOscillator();
  o2.type = "sine";
  o2.frequency.value = freq;
  o2.detune.value = -4;
  const g2 = c.createGain();
  g2.gain.value = 0.4;

  const lfo = c.createOscillator();
  lfo.type = "sine";
  lfo.frequency.value = 0.07;
  const lfoGain = c.createGain();
  lfoGain.gain.value = 0.018;
  lfo.connect(lfoGain);
  lfoGain.connect(g.gain);

  o1.connect(g);
  o2.connect(g2);
  g2.connect(g);
  g.connect(filter);
  filter.connect(master);

  try {
    o1.start(t);
    o2.start(t);
    lfo.start(t);
  } catch {}

  droneNodes = { nodes: [o1, o2, lfo], gain: g, filter, lfoGain };
}

export function stopDrone() {
  if (!droneNodes) return;
  const { nodes, gain, filter, lfoGain } = droneNodes;
  droneNodes = null;
  if (!ctx) return;
  const t = ctx.currentTime;
  try {
    gain.gain.cancelScheduledValues(t);
    gain.gain.setValueAtTime(Math.max(0.0001, gain.gain.value), t);
    gain.gain.exponentialRampToValueAtTime(0.0001, t + 1.2);
    nodes.forEach((o) => {
      try {
        o.stop(t + 1.3);
      } catch {}
    });
    nodes[0].onended = () => {
      try {
        gain.disconnect();
        filter.disconnect();
        lfoGain.disconnect();
      } catch {}
    };
  } catch {}
}

/** @param {number[]} freqs @param {() => void} onEnd @param {{ arranged?: boolean, onNote?: (index: number) => void }} [options] */
export async function playSequence(freqs, onEnd, { arranged = false, onNote } = {}) {
  stopSequence();
  const request = sequenceRequest;
  const c = ensureAudio();
  if (!c) return false;
  if (c.state !== 'running') {
    let timeout;
    const ready = await Promise.race([
      c.resume().then(() => c.state === 'running', () => false),
      new Promise(resolve => { timeout = setTimeout(() => resolve(false), 1800); }),
    ]);
    clearTimeout(timeout);
    if (!ready || request !== sequenceRequest) return false;
  }
  const list = (freqs || []).filter(f => Number.isFinite(f) && f > 0);
  if (!list.length) return false;
  const beat = 60 / 112;
  const spacing = arranged ? beat * 4 : 0.68;
  const start = c.currentTime + 0.08;
  const duration = 1.35;
  let scheduled = 0, highlighted = -1, drumStep = 0;
  sequencePlayback = { start, spacing, beat: arranged ? beat : spacing, length: list.length };
  // Short look-ahead keeps even long hunts bounded, instead of creating every oscillator upfront.
  const tick = () => {
    const now = c.currentTime;
    while (scheduled < list.length && start + scheduled * spacing <= now + 0.12) {
      const i = scheduled++, f = list[i], delay = Math.max(0, start + i * spacing - now);
      voice(f, { start: delay, duration, gain: 0.12, attack: 0.04, release: 0.8, bucket: seq });

    }
    while (arranged && drumStep < list.length * 8 && start + drumStep * beat / 2 <= now + 0.12) {
      const when = start + drumStep * beat / 2;
      if (when >= now - 0.08 && seq.size + live.size < 32) industrialHit(c, master, seq, list[Math.floor(drumStep / 8)], Math.max(now, when), drumStep % 8);
      drumStep++;
    }
    const index = Math.min(list.length - 1, Math.floor((now - start) / spacing));
    if (index >= 0 && index !== highlighted) { highlighted = index; if (onNote) onNote(index); }
    if (now >= start + (arranged ? list.length * spacing + 0.25 : (list.length - 1) * spacing + duration + 0.1)) {
      stopSequence(); if (onEnd) onEnd();
    }
  };
  seqTimer = setInterval(tick, 35);
  tick();
  return true;
}

export function stopSequence() {
  sequenceRequest++;
  sequencePlayback = null;
  if (seqTimer) { clearInterval(seqTimer); seqTimer = null; }
  seq.forEach(entry => {
    const t = ctx.currentTime;
    entry.gain.gain.cancelScheduledValues(t);
    entry.gain.gain.setTargetAtTime(0.0001, t, 0.008);
    entry.nodes.forEach(o => { try { o.stop(t + 0.04); } catch {} });
  });
  seq.clear();
}

export function stopAll() {
  stopSequence();
  stopDrone();
  live.forEach((entry) => {
    entry.nodes.forEach((o) => {
      try {
        o.stop();
      } catch {}
    });
    entry.cleanup();
  });
  live.clear();
}