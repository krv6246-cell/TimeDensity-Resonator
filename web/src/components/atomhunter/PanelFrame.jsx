import React from "react";

export default function PanelFrame({ label, className = "", children }) {
  return (
    <div
      className={`hunter-panel relative p-[1px] ${className}`}
    >
      <div className="relative h-full w-full overflow-hidden rounded-[15px] border border-white/5 bg-[#08080f]/85 backdrop-blur-md">
        <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_center,rgba(34,211,238,0.07),transparent_70%)]" />
        {label && (
          <span className="pointer-events-none absolute left-1/2 top-2 -translate-x-1/2 text-[8px] tracking-[0.3em] text-white/35">
            {label}
          </span>
        )}
        {children}
      </div>
    </div>
  );
}