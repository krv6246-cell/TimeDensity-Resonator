import React from "react";
import { Play, Square, RotateCcw } from "lucide-react";

export default function MelodyControls({ playing, pending, arranged, setArranged, onPlay, onStop, onReplay, empty, audioError }) {
  return (
    <section className="hunter-music">
      <div className="hunter-section-label"><span>{arranged ? "ИНДУСТРИАЛЬНЫЙ МАРШ / 112 BPM" : "ЧАСТОТЫ АТОМОВ"}</span><span>{pending ? "ЗАПУСК ЗВУКА" : playing ? "ВОСПРОИЗВЕДЕНИЕ" : "ГОТОВО К ПРОСЛУШИВАНИЮ"}</span></div>
      <div className="hunter-music-actions">
        <button className="hunter-primary" onClick={onPlay} disabled={empty || playing || pending}><Play size={17} /> СЛУШАТЬ МЕЛОДИЮ</button>
        <button className="hunter-secondary" onClick={onStop} disabled={!playing && !pending}><Square size={16} /> СТОП</button>
        <button className="hunter-secondary" onClick={onReplay} disabled={empty || pending}><RotateCcw size={16} /> ЗАНОВО</button>
      </div>
      <label className="hunter-arranged"><input type="checkbox" checked={arranged} disabled={empty || playing || pending} onChange={e => setArranged(e.target.checked)} /> ИНДУСТРИАЛЬНЫЙ МАРШ <span>Искажённые риффы · бас · механические ударные</span></label>
      <p className="hunter-music-note">{audioError || (empty ? "Поймай атом, чтобы создать первую ноту." : arranged ? "Один атом — один такт. Частоты управляют риффом, а галактика следует за битом." : "Чистые частоты в порядке захвата. Включи индустриальный марш для тяжёлого звучания.")}</p>
    </section>
  );
}