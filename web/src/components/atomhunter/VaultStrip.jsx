import React from "react";
import { ELEMENTS } from "@/lib/rezonator/elements";

export default function VaultStrip({ captured }) {
  const recent = (captured || []).slice(-14);
  return (
    <div key={captured.length} className="hunter-vault-strip">
      <span className="shrink-0 text-[9px] tracking-[0.3em] text-white/40">АРХИВ АТОМОВ</span>
      <div className="flex min-w-0 flex-1 items-center gap-1 overflow-hidden">
        {recent.length === 0 && <span className="text-[10px] tracking-[0.2em] text-white/25">ПОКА ПУСТО</span>}
        {recent.map((key, i) => (
          <span
            key={`${key}-${i}`}
            className="h-2 w-2 shrink-0 rounded-full"
            style={{ background: ELEMENTS[key].color, boxShadow: `0 0 6px ${ELEMENTS[key].color}` }}
          />
        ))}
      </div>
      <span className="shrink-0 text-[11px] tabular-nums text-white/75">{captured.length}</span>
    </div>
  );
}