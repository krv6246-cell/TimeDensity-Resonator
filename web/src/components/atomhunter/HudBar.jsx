import React from "react";
import { Timer } from "lucide-react";

export default function HudBar({ remaining, capturedCount, target, progress, gameState, score = 0 }) {
  const seconds = Math.max(0, Math.ceil(remaining / 1000));
  return (
    <section className="hunter-hud" aria-label="Состояние охоты">
      <div className="hunter-clock" data-low={seconds <= 15}><Timer size={16} /><strong>{Math.floor(seconds / 60)}:{String(seconds % 60).padStart(2, "0")}</strong><small>ОСТАЛОСЬ</small></div>
      <div className="hunter-target"><small>ЦЕЛЕВОЙ СИГНАЛ</small><strong style={{ color: target?.color }}>{target?.symbol} <span>{target?.freq} <small>Гц</small></span></strong></div>
      <div className="hunter-count"><strong>{String(capturedCount).padStart(2, "0")}</strong><small>ПОЙМАНО</small></div>
      <div className="hunter-score"><strong>{score.toLocaleString("ru-RU")}</strong><small>ОЧКИ</small></div>
      <div className="hunter-hud-progress" data-state={gameState}><div style={{ width: `${progress * 100}%` }} /></div>
    </section>
  );
}