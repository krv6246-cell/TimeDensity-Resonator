import React, { useRef } from "react";
import { Minus, Plus } from "lucide-react";
import { MIN_HZ, MAX_HZ, clampHz } from "@/lib/rezonator/constants";
import PanelFrame from "@/components/atomhunter/PanelFrame";

export default function TuneSlider({ value, onChange, targetFreq, tolerance = 0, gameState = "idle", disabled }) {
  const trackRef = useRef(null);
  const dragging = useRef(false);

  const hzAt = (clientX) => {
    const el = trackRef.current;
    if (!el) return value;
    const r = el.getBoundingClientRect();
    if (r.width < 4) return value;
    const ratio = Math.max(0, Math.min(1, (clientX - r.left) / r.width));
    return clampHz(MIN_HZ + ratio * (MAX_HZ - MIN_HZ));
  };

  const onDown = (e) => {
    if (disabled) return;
    e.preventDefault();
    dragging.current = true;
    onChange(hzAt(e.clientX));
    try {
      e.currentTarget.setPointerCapture(e.pointerId);
    } catch {}
  };

  const onMove = (e) => {
    if (!dragging.current || disabled) return;
    onChange(hzAt(e.clientX));
  };

  const onUp = (e) => {
    dragging.current = false;
    try {
      e.currentTarget.releasePointerCapture(e.pointerId);
    } catch {}
  };

  const nudge = (d) => {
    if (!disabled) onChange(clampHz(value + d));
  };

  const onKey = (e) => {
    if (disabled) return;
    const step = e.shiftKey ? 10 : 1;
    if (e.key === "ArrowLeft" || e.key === "ArrowDown") {
      e.preventDefault();
      nudge(-step);
    } else if (e.key === "ArrowRight" || e.key === "ArrowUp") {
      e.preventDefault();
      nudge(step);
    } else if (e.key === "Home") {
      e.preventDefault();
      onChange(MIN_HZ);
    } else if (e.key === "End") {
      e.preventDefault();
      onChange(MAX_HZ);
    }
  };

  const ratio = Math.max(0, Math.min(1, (value - MIN_HZ) / (MAX_HZ - MIN_HZ)));
  const bandFrom = targetFreq ? Math.max(0, Math.min(1, (targetFreq - tolerance - MIN_HZ) / (MAX_HZ - MIN_HZ))) : 0;
  const bandTo = targetFreq ? Math.max(0, Math.min(1, (targetFreq + tolerance - MIN_HZ) / (MAX_HZ - MIN_HZ))) : 0;
  const tRatio = targetFreq ? Math.max(0, Math.min(1, (targetFreq - MIN_HZ) / (MAX_HZ - MIN_HZ))) : null;

  const resonant = gameState === "stable" || gameState === "transformed";
  const pct = (v) => `${v * 100}%`;

  const stepBtn =
    "hunter-step flex h-12 w-12 shrink-0 items-center justify-center rounded-full";

  return (
    <PanelFrame label="" className="hunter-tune w-full">
      <div className="px-4 py-3">
        <div className="mb-2 flex items-end justify-between px-1">
          <div className="flex items-baseline gap-2">
            <span className="text-[9px] tracking-[0.42em] text-white/35">НАСТРОЙКА</span>
            <span className="text-2xl font-light tabular-nums text-white sm:text-3xl">{value}</span>
            <span className="text-[10px] tracking-[0.3em] text-cyan-300/80">Hz</span>
          </div>
          <span className="text-[9px] tracking-[0.28em] text-white/45">
            {targetFreq ? `ЦЕЛЬ ${targetFreq} Гц` : "ЦЕЛЬ —"}
          </span>
        </div>

        <div className="flex items-center gap-3">
          <button type="button" disabled={disabled} onClick={() => nudge(-1)} aria-label="Уменьшить частоту" className={stepBtn}>
            <Minus className="h-4 w-4" />
          </button>

          <div
            ref={trackRef}
            role="slider"
            aria-label="Частота сигнала"
            aria-valuenow={value}
            aria-valuemin={MIN_HZ}
            aria-valuemax={MAX_HZ}
            aria-disabled={!!disabled}
            tabIndex={disabled ? -1 : 0}
            onPointerDown={onDown}
            onPointerMove={onMove}
            onPointerUp={onUp}
            onPointerCancel={onUp}
            onKeyDown={onKey}
            className={`relative flex-1 touch-none select-none py-3 outline-none ${disabled ? "opacity-50" : "cursor-pointer"}`}
          >
            <div className="relative h-2 rounded-full bg-white/10">
              {targetFreq && (
                <div
                  className="absolute inset-y-0 rounded-full bg-violet-400/70"
                  style={{ left: pct(bandFrom), width: pct(Math.max(0.004, bandTo - bandFrom)) }}
                />
              )}
              <div
                className={`absolute inset-y-0 left-0 rounded-full ${resonant ? "bg-amber-300" : "bg-cyan-400"}`}
                style={{
                  width: pct(ratio),
                  boxShadow: resonant ? "0 0 10px rgba(251,191,36,0.7)" : "0 0 10px rgba(34,211,238,0.6)",
                }}
              />
              {tRatio != null && (
                <div
                  className="absolute -top-1 -bottom-1 w-[2px] rounded-full bg-violet-300/90"
                  style={{ left: pct(tRatio) }}
                />
              )}
              <div
                className="absolute top-1/2 h-5 w-5 -translate-x-1/2 -translate-y-1/2 rounded-full border border-white/70 bg-white"
                style={{ left: pct(ratio), boxShadow: "0 0 12px rgba(255,255,255,0.65)" }}
              />
            </div>

            <div className="mt-2 flex justify-between text-[8px] tracking-[0.2em] text-white/25">
              <span>{MIN_HZ} HZ</span>
              <span>{MAX_HZ} HZ</span>
            </div>
          </div>

          <button type="button" disabled={disabled} onClick={() => nudge(1)} aria-label="Увеличить частоту" className={stepBtn}>
            <Plus className="h-4 w-4" />
          </button>
        </div>
      </div>
    </PanelFrame>
  );
}