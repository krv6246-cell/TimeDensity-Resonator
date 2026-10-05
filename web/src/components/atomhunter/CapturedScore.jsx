import React from "react";
import { ELEMENTS, ELEMENT_LIST } from "@/lib/rezonator/elements";

export default function CapturedScore({ captured, counts, activeNote }) {
  return (
    <div className="hunter-score-details">
      <section className="hunter-result-panel">
        <div className="hunter-section-label"><span>ПОСЛЕДОВАТЕЛЬНОСТЬ ЗАХВАТА</span><span>{captured.length} НОТ · ПО ПОРЯДКУ</span></div>
        {captured.length ? <div className="hunter-notes">{captured.map((key, i) => <span key={i} data-note-index={i} data-active={activeNote === i} className="hunter-note" style={/** @type {import("react").CSSProperties} */ ({ "--note-color": ELEMENTS[key].color })}><small>{String(i + 1).padStart(2, "0")}</small><strong>{key}</strong><span>{ELEMENTS[key].freq} Hz</span></span>)}</div> : <p className="hunter-empty">Вселенная пока тиха. В следующей охоте найди сигнал и удержи частоту.</p>}
      </section>
      <section className="hunter-result-panel">
        <div className="hunter-section-label"><span>АРХИВ АТОМОВ</span><span>КОЛЛЕКЦИЯ СИГНАЛОВ</span></div>
        <div className="hunter-inventory">{ELEMENT_LIST.map(el => <div key={el.key}><strong style={{ color: el.color }}>{el.symbol}</strong><span>{el.name}</span><b>{counts[el.key] || 0}</b></div>)}</div>
      </section>
    </div>
  );
}