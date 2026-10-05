import React from "react";
import { ArrowUpRight, AudioLines, Orbit, Sparkles, Trophy } from "lucide-react";
import { ELEMENT_LIST } from "@/lib/rezonator/elements";

export default function StartScreen({ onStart, bestScore = 0 }) {
  return (
    <main className="hunter-entry">
      <header className="hunter-entry-top">
        <a className="hunter-wordmark" href="#top" aria-label="Atom Hunter — главная"><span className="hunter-logo-mark"><Orbit size={19} /></span> ATOM <b>HUNTER</b></a>
        <span className="hunter-live-label"><i /> СИМУЛЯЦИЯ СИГНАЛА · № 08</span>
      </header>
      <div id="top" className="hunter-entry-content">
        <section className="hunter-hero-copy">
          <p className="hunter-eyebrow"><Sparkles size={13} /> КОСМИЧЕСКАЯ ОХОТА НАЧИНАЕТСЯ</p>
          <h1 className="hunter-title">Поймай<br /><span>резонанс.</span></h1>
          <p className="hunter-entry-tagline">Настраивай частоту. Удерживай сигнал.<br />Собери свою вселенную из атомов.</p>
          <button onClick={onStart} aria-label="Начать охоту" className="hunter-primary hunter-start">НАЧАТЬ ОХОТУ <ArrowUpRight size={19} /></button>
          <div className="hunter-hero-meta"><span><b>02:00</b> МИНУТЫ</span><span><b>06</b> ЭЛЕМЕНТОВ</span><span><b>∞</b> КОМБИНАЦИЙ</span></div>
        </section>
        <div className="hunter-hero-art" aria-hidden="true">
          <div className="hunter-orbit hunter-orbit-one" /><div className="hunter-orbit hunter-orbit-two" />
          <div className="hunter-planet hunter-planet-main"><span>Au</span></div>
          <div className="hunter-planet hunter-planet-small planet-cyan">H</div>
          <div className="hunter-planet hunter-planet-small planet-lilac">Fe</div>
          <div className="hunter-art-cross cross-one">✳</div><div className="hunter-art-cross cross-two">✳</div>
          <div className="hunter-art-caption"><AudioLines size={15} /><span>ЧАСТОТА — ЭТО ТВОЙ ИНСТРУМЕНТ</span></div>
        </div>
      </div>
      <footer className="hunter-entry-bottom">
        <div className="hunter-element-legend">{ELEMENT_LIST.map(el => <span key={el.key} style={/** @type {import("react").CSSProperties} */ ({ "--element-color": el.color })}>{el.symbol}<small>{el.name}</small></span>)}</div>
        <div className="hunter-best-record"><Trophy size={15} /><span>ЛИЧНЫЙ РЕКОРД</span><b>{bestScore.toLocaleString("ru-RU")}</b></div>
      </footer>
    </main>
  );
}