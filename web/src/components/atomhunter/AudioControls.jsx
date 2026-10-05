import React from "react";
import { Volume2, VolumeX } from "lucide-react";

export default function AudioControls({ muted, volume, onToggleMute, onVolumeChange }) {
  return (
    <div className="hunter-audio">
      <button
        type="button"
        onClick={onToggleMute}
        aria-label={muted ? "Включить звук" : "Выключить звук"}
        className="hunter-icon-button"
      >
        {muted ? <VolumeX className="h-4 w-4" /> : <Volume2 className="h-4 w-4" />}
      </button>
      <input
        type="range"
        min="0"
        max="1"
        step="0.01"
        value={volume}
        onChange={(e) => onVolumeChange(Number(e.target.value))}
        aria-label="Громкость"
        className="h-1 w-14 cursor-pointer appearance-none rounded-full bg-white/15 accent-cyan-400"
      />
    </div>
  );
}