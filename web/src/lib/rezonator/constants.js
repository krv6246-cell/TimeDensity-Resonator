export const MIN_HZ = 80;
export const MAX_HZ = 800;
export const SESSION_MS = 120000;
export const HOLD_MS = 1200;
export const CAPTURE_MS = 950;

const TOLERANCE_MIN = 12;
const TOLERANCE_RATIO = 0.04;

export function toleranceFor(freq) {
  return Math.max(TOLERANCE_MIN, freq * TOLERANCE_RATIO);
}

export function clampHz(v) {
  return Math.max(MIN_HZ, Math.min(MAX_HZ, Math.round(v)));
}