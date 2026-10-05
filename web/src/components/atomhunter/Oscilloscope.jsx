import React, { useEffect, useRef } from "react";
import PanelFrame from "@/components/atomhunter/PanelFrame";
import scopeRenderer from "@/components/atomhunter/scopeRenderer";

export default function Oscilloscope({ targetFreq, carrierFreq, gameState, progress = 0 }) {
  const canvasRef = useRef(null), state = useRef(/** @type {any} */ ({}));
  state.current = { targetFreq, carrierFreq, gameState, progress };
  useEffect(() => {
    const canvas = canvasRef.current, ctx = canvas.getContext("2d");
    const style = getComputedStyle(canvas), colors = {};
    for (const key of ['surface', 'ink', 'muted', 'cyan', 'violet', 'gold']) colors[key] = `hsl(${style.getPropertyValue(`--hunter-${key}`).trim()})`;
    const reduced = window.matchMedia('(prefers-reduced-motion: reduce)');
    let width = 0, height = 0, raf;
    const paint = now => scopeRenderer(ctx, width, height, reduced.matches ? 0 : now / 1000, state.current, colors);
    const resize = () => {
      const box = canvas.getBoundingClientRect(), dpr = Math.min(window.devicePixelRatio || 1, 2);
      width = box.width; height = box.height;
      canvas.width = Math.round(width * dpr); canvas.height = Math.round(height * dpr);
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0); paint(performance.now());
    };
    const observer = new ResizeObserver(resize); observer.observe(canvas); resize();
    const draw = now => { paint(now); raf = requestAnimationFrame(draw); };
    raf = requestAnimationFrame(draw);
    return () => { observer.disconnect(); cancelAnimationFrame(raf); };
  }, []);
  const delta = Math.round((targetFreq || carrierFreq) - carrierFreq);
  const status = gameState === 'transformed' ? 'ПОЙМАН' : gameState === 'stable' ? 'ЗАХВАТ' : gameState === 'matched' ? 'ДЕРЖИ' : 'ПОИСК';
  return (
    <PanelFrame label="РЕЗОНАНС" className="h-full w-full">
      <canvas ref={canvasRef} role="img" aria-label="Resonance lens: violet target and cyan tuning waves align as frequencies match" className="block h-full w-full" />
      <output className="hunter-scope-readout" data-state={gameState} aria-live="off" aria-label={`Резонанс ${Math.round(progress * 100)} процентов; разница частот ${delta} герц`}>
        <strong>{Math.round(progress * 100)}%</strong><span>{delta === 0 ? 'Δ 0 Hz' : `${delta > 0 ? '↑' : '↓'} ${Math.abs(delta)} Hz`}</span><small>{status}</small>
      </output>
    </PanelFrame>
  );
}