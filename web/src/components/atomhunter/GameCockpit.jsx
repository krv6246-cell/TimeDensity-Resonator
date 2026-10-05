import React from "react";
import AtomField from "@/components/atomhunter/AtomField";
import HudBar from "@/components/atomhunter/HudBar";
import VaultStrip from "@/components/atomhunter/VaultStrip";
import TuneSlider from "@/components/atomhunter/TuneSlider";
import Oscilloscope from "@/components/atomhunter/Oscilloscope";
import AudioControls from "@/components/atomhunter/AudioControls";

export default function GameCockpit({ target, carrier, progress, gameState, captured, remaining, captureToken, onCaptureDone, onCarrier, tolerance, muted, volume, onToggleMute, onVolumeChange, score = 0, streak = 0 }) {
  const feedback = { idle: "ИЩИ СИГНАЛ", matched: "ЧАСТОТА ПОЙМАНА · УДЕРЖИВАЙ", stable: "РЕЗОНАНС СТАБИЛЕН · ИДЁТ ЗАХВАТ", transformed: "АТОМ ЗАХВАЧЕН · ЗАПИСАН В АРХИВ" };
  return (
    <main className="hunter-cockpit" data-state={gameState}>
      <header className="hunter-brand"><span className="hunter-wordmark"><span className="hunter-logo-mark">A</span> ATOM <b>HUNTER</b></span><span className="hunter-run-label"><i /> ОХОТА АКТИВНА</span><AudioControls {...{ muted, volume, onToggleMute, onVolumeChange }} /></header>
      <HudBar {...{ remaining, capturedCount: captured.length, target, carrier, progress, gameState, score, streak }} />
      <div className="hunter-arena">
        <AtomField {...{ target, gameState, progress, captured, captureToken, onCaptureDone }} />
        <div className="hunter-scope"><Oscilloscope targetFreq={target?.freq} carrierFreq={carrier} gameState={gameState} progress={progress} /></div>
        <div className="hunter-arena-caption"><span>ПОЛЕ РЕЗОНАНСА</span><span>{target?.name.toUpperCase()} · СИГНАЛ {String(target?.id || 0).padStart(3, "0")}</span></div>
      </div>
      <div className="hunter-feedback" role="status" data-state={gameState}><span className="hunter-status-dot" />{feedback[gameState]}{streak > 1 && <b className="hunter-streak">СЕРИЯ ×{streak}</b>}<span className="font-mono">{Math.round(progress * 100)}%</span></div>
      <VaultStrip captured={captured} />
      <TuneSlider value={carrier} onChange={onCarrier} targetFreq={target?.freq} {...{ tolerance, gameState }} disabled={gameState === "transformed"} />
      <div className="hunter-footnote"><span>НАСТРОЙ · УДЕРЖИВАЙ · ЗАХВАТЫВАЙ</span><span>ОЧКИ <b>{score.toLocaleString("ru-RU")}</b></span></div>
    </main>
  );
}