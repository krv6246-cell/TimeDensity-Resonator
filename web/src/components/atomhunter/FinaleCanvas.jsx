import React, { useEffect, useRef } from 'react';
import drawGalaxy, { galaxyCloud } from '@/lib/rezonator/galaxy';
import { getSequenceFrame } from '@/lib/rezonator/audio';

export default function FinaleCanvas({ captured }) {
  const canvasRef = useRef(null);
  useEffect(() => {
    const canvas = canvasRef.current, ctx = canvas.getContext('2d');
    const reduced = window.matchMedia('(prefers-reduced-motion: reduce)');
    const dust = galaxyCloud(captured), start = performance.now();
    let raf, width = 0, height = 0;
    const resize = () => {
      const box = canvas.getBoundingClientRect(), dpr = Math.min(devicePixelRatio || 1, 2);
      width = box.width; height = box.height;
      canvas.width = Math.round(width * dpr); canvas.height = Math.round(height * dpr);
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      drawGalaxy(ctx, width, height, performance.now() - start, captured, dust, getSequenceFrame(), reduced.matches);
    };
    const observer = new ResizeObserver(resize); observer.observe(canvas); resize();
    const draw = now => {
      drawGalaxy(ctx, width, height, now - start, captured, dust, getSequenceFrame(), reduced.matches);
      raf = requestAnimationFrame(draw);
    };
    raf = requestAnimationFrame(draw);
    return () => { cancelAnimationFrame(raf); observer.disconnect(); };
  }, [captured]);
  return <canvas ref={canvasRef} role="img" aria-label={`Живая галактика из ${captured.length} пойманных атомов. Звёзды следуют порядку захвата и пульсируют под музыку.`} className="absolute inset-0 block h-full w-full" />;
}