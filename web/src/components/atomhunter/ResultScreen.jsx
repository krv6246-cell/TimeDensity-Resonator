import React, { useEffect, useRef, useState } from "react";
import { ArrowUpRight, Home } from "lucide-react";
import FinaleCanvas from "@/components/atomhunter/FinaleCanvas";
import CapturedScore from "@/components/atomhunter/CapturedScore";
import MelodyControls from "@/components/atomhunter/MelodyControls";
import AudioControls from "@/components/atomhunter/AudioControls";
import { ELEMENTS } from "@/lib/rezonator/elements";
import { playSequence, stopAll, stopSequence } from "@/lib/rezonator/audio";

export default function ResultScreen({ captured, counts, score = 0, bestScore = 0, newRecord = false, onNewHunt, onHome, muted, volume, onToggleMute, onVolumeChange }) {
  const [stage, setStage] = useState(0);
  const [playing, setPlaying] = useState(false);
  const [activeNote, setActiveNote] = useState(-1);
  const [arranged, setArranged] = useState(true);
  const [audioError, setAudioError] = useState("");
  const [pending, setPending] = useState(false);
  const request = useRef(0);
  useEffect(() => {
    stopAll();
    const timers = [setTimeout(() => setStage(1), 250), setTimeout(() => setStage(2), 900), setTimeout(() => setStage(3), 1750)];
    return () => { request.current++; timers.forEach(clearTimeout); stopSequence(); };
  }, []);
  const stop = () => { request.current++; stopSequence(); setPending(false); setPlaying(false); setActiveNote(-1); };
  const play = async () => {
    const id = ++request.current;
    setPending(true); setPlaying(false); setActiveNote(-1); setAudioError("");
    const ok = await playSequence(captured.map(key => ELEMENTS[key].freq), () => { setPlaying(false); setActiveNote(-1); }, { arranged, onNote: setActiveNote });
    if (id !== request.current) return;
    setPending(false); setPlaying(ok); setAudioError(ok ? "" : "Не удалось запустить звук. Разреши воспроизведение в браузере и попробуй ещё раз.");
  };
  const unique = new Set(captured).size;
  return (
    <main className="hunter-result">
      <header className="hunter-result-heading"><span className="hunter-eyebrow">АРХИВ КОСМИЧЕСКОЙ ОХОТЫ</span><h1>ВСЕЛЕННАЯ СОБРАНА</h1><p>{["Твои сигналы складываются в созвездие.", "Материя схлопывается в сингулярность.", "Рождается новая вселенная.", captured.length ? "Каждый пойманный атом стал звездой. Включи свою мелодию." : "Тихая вселенная. В следующий раз поймай первый сигнал."][stage]}</p></header>
      <div className="hunter-genesis" data-stage={stage}><FinaleCanvas captured={captured} /><div className="hunter-genesis-label">{["01 / СОЗВЕЗДИЕ", "02 / КОЛЛАПС", "03 / ГЕНЕЗИС", "ТВОЯ ЖИВАЯ ГАЛАКТИКА"][stage]}<span>{captured.length} ПОЙМАНО</span></div></div>
      {stage === 3 && <div className="hunter-result-content">
        <MelodyControls {...{ playing, pending, arranged, setArranged, audioError }} empty={!captured.length} onPlay={play} onStop={stop} onReplay={() => { stop(); play(); }} />
        <div className="hunter-result-stats">{[["ПОЙМАНО", captured.length], ["ВИДОВ", unique], ["ОЧКИ", score]].map(([label, n]) => <div key={label}><strong>{Number(n).toLocaleString("ru-RU")}</strong><small>{label}</small></div>)}</div>
        <p className="hunter-best-line">{newRecord ? "✦ НОВЫЙ ЛИЧНЫЙ РЕКОРД" : `ЛУЧШИЙ РЕЗУЛЬТАТ · ${bestScore.toLocaleString("ru-RU")}`}</p>
        <CapturedScore {...{ captured, counts, activeNote }} />
        <div className="hunter-result-navigation"><button className="hunter-primary" onClick={onNewHunt}>ЕЩЁ ОХОТА <ArrowUpRight size={18} /></button><button className="hunter-secondary" onClick={onHome}><Home size={17} /> В НАЧАЛО</button><AudioControls {...{ muted, volume, onToggleMute, onVolumeChange }} /></div>
      </div>}
    </main>
  );
}