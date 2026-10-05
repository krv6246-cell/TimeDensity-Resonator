import React, { useEffect, useRef } from "react";
import { CAPTURE_MS } from "@/lib/rezonator/constants";

const TAU = Math.PI * 2;

export default function AtomField({ target, gameState, progress, captured, captureToken, onCaptureDone }) {
  const canvasRef = useRef(null);
  const doneRef = useRef(onCaptureDone);
  doneRef.current = onCaptureDone;

  const props = useRef(/** @type {any} */ ({}));
  props.current = { target, gameState, progress, captured, captureToken };

  const anim = useRef({ token: captureToken, capture: null, flash: 0 });

  useEffect(() => {
    const cvs = canvasRef.current;
    if (!cvs) return undefined;
    const ctx = cvs.getContext("2d");
    let raf = 0;
    let W = 0;
    let H = 0;
    let stars = [];
    let last = 0;

    const sprites = ["255,255,255", "34,211,238", "167,139,250"].map((rgb) => {
      const s = document.createElement("canvas");
      const S = 64;
      s.width = S;
      s.height = S;
      const g = s.getContext("2d");
      const rg = g.createRadialGradient(S / 2, S / 2, 0, S / 2, S / 2, S / 2);
      rg.addColorStop(0, `rgba(${rgb},0.95)`);
      rg.addColorStop(0.22, `rgba(${rgb},0.42)`);
      rg.addColorStop(0.55, `rgba(${rgb},0.11)`);
      rg.addColorStop(1, "rgba(0,0,0,0)");
      g.fillStyle = rg;
      g.fillRect(0, 0, S, S);
      return s;
    });

    const reduced = window.matchMedia("(prefers-reduced-motion: reduce)");
    let motes = Array.from({ length: window.innerWidth < 600 ? 32 : 56 }, () => ({
      x: Math.random(),
      y: Math.random(),
      r: 0.7 + Math.random() * 2.1,
      a: 0.16 + Math.random() * 0.42,
      tw: Math.random() * TAU,
      tws: 0.5 + Math.random() * 1.7,
      vy: 0.005 + Math.random() * 0.014,
      vx: (Math.random() - 0.5) * 0.008,
      c: Math.random() < 0.62 ? 0 : Math.random() < 0.6 ? 1 : 2,
    }));

    const resize = () => {
      const r = cvs.getBoundingClientRect();
      if (r.width < 4 || r.height < 4) return;
      const dpr = Math.min(window.devicePixelRatio || 1, 2);
      W = r.width;
      H = r.height;
      cvs.width = Math.round(W * dpr);
      cvs.height = Math.round(H * dpr);
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      if (!stars.length) {
        stars = Array.from({ length: 120 }, () => ({
          x: Math.random(),
          y: Math.random(),
          r: Math.random() * 1.3 + 0.3,
          tw: Math.random() * TAU,
          sp: 0.4 + Math.random() * 0.9,
        }));
      }
    };
    resize();
    const ro = new ResizeObserver(resize);
    ro.observe(cvs);

    const arcPath = (a0, a1, r) => {
      const x0 = Math.cos(a0) * r;
      const y0 = Math.sin(a0) * r;
      const x1 = Math.cos(a1) * r;
      const y1 = Math.sin(a1) * r;
      const large = Math.abs(a1 - a0) > Math.PI ? 1 : 0;
      return `M ${x0} ${y0} A ${r} ${r} 0 ${large} 1 ${x1} ${y1}`;
    };

    const draw = (now) => {
      const t = reduced.matches ? 0 : now / 1000;
      const dt = last && !reduced.matches ? Math.min(64, now - last) / 1000 : 0;
      last = now;
      const p = props.current;
      const cx = W / 2 + Math.min(28, W * 0.05);
      const cy = H * 0.56;
      const coreR = Math.max(26, Math.min(W, H) * 0.14);
      const atomR = Math.max(17, Math.min(30, Math.min(W, H) * 0.043));

      if (p.captureToken !== anim.current.token) {
        anim.current.token = p.captureToken;
        anim.current.capture = { start: now, from: null };
      }

      // backdrop
      const bg = ctx.createLinearGradient(0, 0, 0, H);
      bg.addColorStop(0, "#05050c");
      bg.addColorStop(0.5, "#080613");
      bg.addColorStop(1, "#040409");
      ctx.fillStyle = bg;
      ctx.fillRect(0, 0, W, H);

      const blobs = [
        { x: 0.22, y: 0.3, c: "124,58,237", s: 0.6 },
        { x: 0.82, y: 0.6, c: "14,116,144", s: 0.62 },
        { x: 0.5, y: 0.86, c: "76,29,149", s: 0.5 },
      ];
      blobs.forEach((b, i) => {
        const bx = (b.x + Math.sin(t * 0.05 + i) * 0.04) * W;
        const by = (b.y + Math.cos(t * 0.04 + i * 2) * 0.05) * H;
        const rad = Math.max(W, H) * b.s;
        const g = ctx.createRadialGradient(bx, by, 0, bx, by, rad);
        g.addColorStop(0, `rgba(${b.c},0.11)`);
        g.addColorStop(0.55, `rgba(${b.c},0.035)`);
        g.addColorStop(1, "rgba(0,0,0,0)");
        ctx.fillStyle = g;
        ctx.beginPath();
        ctx.arc(bx, by, rad, 0, TAU);
        ctx.fill();
      });

      stars.forEach((s) => {
        const a = 0.2 + 0.5 * (0.5 + 0.5 * Math.sin(t * s.sp + s.tw));
        ctx.fillStyle = `rgba(220,230,255,${a})`;
        ctx.beginPath();
        ctx.arc(s.x * W, s.y * H, s.r, 0, TAU);
        ctx.fill();
      });

      // drifting glow motes
      ctx.globalCompositeOperation = "lighter";
      for (let i = 0; i < motes.length; i++) {
        const m = motes[i];
        m.y -= m.vy * dt;
        m.x += m.vx * dt + Math.sin(t * 0.25 + m.tw) * 0.0007;
        if (m.y < -0.06) {
          m.y = 1.06;
          m.x = Math.random();
        }
        if (m.x < -0.06) m.x = 1.06;
        else if (m.x > 1.06) m.x = -0.06;

        const twinkle = 0.35 + 0.65 * (0.5 + 0.5 * Math.sin(t * m.tws + m.tw));
        const rad = m.r * (1 + 0.3 * Math.sin(t * m.tws * 0.7 + m.tw));
        const size = rad * 13;
        ctx.globalAlpha = m.a * twinkle;
        ctx.drawImage(sprites[m.c], m.x * W - size / 2, m.y * H - size / 2, size, size);
      }
      ctx.globalAlpha = 1;
      ctx.globalCompositeOperation = "source-over";

      // black hole
      const capture = anim.current.capture;
      const ck = capture ? Math.min(1, (now - capture.start) / CAPTURE_MS) : 0;
      const flashK = anim.current.flash ? Math.max(0, 1 - (now - anim.current.flash) / 700) : 0;
      const ringBoost = Math.max(ck, flashK);

      const halo = ctx.createRadialGradient(cx, cy, coreR * 0.5, cx, cy, coreR * 3.4);
      halo.addColorStop(0, `rgba(140,100,255,${0.22 + ringBoost * 0.25})`);
      halo.addColorStop(0.45, "rgba(70,50,160,0.1)");
      halo.addColorStop(1, "rgba(0,0,0,0)");
      ctx.fillStyle = halo;
      ctx.beginPath();
      ctx.arc(cx, cy, coreR * 3.4, 0, TAU);
      ctx.fill();

      ctx.save();
      ctx.translate(cx, cy);
      for (let i = 0; i < 3; i++) {
        const rr = coreR * (1.22 + i * 0.17);
        const spin = t * (0.5 - i * 0.12) + i * 1.6;
        ctx.save();
        ctx.rotate(spin);
        ctx.beginPath();
        ctx.arc(0, 0, rr, 0, Math.PI * (1.05 + i * 0.16));
        ctx.strokeStyle =
          i === 0
            ? `rgba(167,139,250,${0.7 + ringBoost * 0.3})`
            : i === 1
            ? `rgba(34,211,238,${0.45 + ringBoost * 0.25})`
            : `rgba(251,191,36,${0.26 + ringBoost * 0.4})`;
        ctx.lineWidth = 1.6 + i;
        ctx.shadowColor = i === 2 ? "rgba(251,191,36,0.7)" : "rgba(167,139,250,0.7)";
        ctx.shadowBlur = 12 + ringBoost * 18;
        ctx.stroke();
        ctx.restore();
      }
      ctx.restore();

      const core = ctx.createRadialGradient(cx, cy, 0, cx, cy, coreR);
      core.addColorStop(0, "#000000");
      core.addColorStop(0.7, "#04030a");
      core.addColorStop(1, "rgba(26,14,52,0.95)");
      ctx.fillStyle = core;
      ctx.beginPath();
      ctx.arc(cx, cy, coreR, 0, TAU);
      ctx.fill();

      ctx.beginPath();
      ctx.arc(cx, cy, coreR, 0, TAU);
      ctx.strokeStyle = `rgba(${flashK > 0 ? "255,255,255" : "178,158,255"},${0.32 + ringBoost * 0.55})`;
      ctx.lineWidth = 1.4;
      ctx.stroke();

      // vault contents
      const stored = p.captured || [];
      stored.slice(-18).forEach((key, i) => {
        const meta = ELEMENT_COLORS[key];
        if (!meta) return;
        const ang = t * 0.7 + i * 2.4;
        const rad = coreR * (0.45 + (i % 3) * 0.2);
        ctx.beginPath();
        ctx.arc(cx + Math.cos(ang) * rad, cy + Math.sin(ang) * rad, 1.9, 0, TAU);
        ctx.fillStyle = `rgba(${meta},0.95)`;
        ctx.shadowColor = `rgba(${meta},0.9)`;
        ctx.shadowBlur = 8;
        ctx.fill();
        ctx.shadowBlur = 0;
      });

      // atom
      const el = p.target;
      const locked = p.gameState !== "idle";
      const orbit = locked ? -Math.PI / 2 : -Math.PI / 2 + Math.sin(t * 0.33) * 0.85;
      const orbitR = coreR * 1.95;
      const drift = { x: cx + Math.cos(orbit) * orbitR, y: cy + Math.sin(orbit) * orbitR };
      ctx.beginPath(); ctx.ellipse(cx, cy, orbitR, orbitR, 0, 0, TAU);
      ctx.strokeStyle = locked ? "rgba(34,211,238,0.28)" : "rgba(167,139,250,0.12)";
      ctx.lineWidth = 1; ctx.stroke();

      if (capture && !capture.from) capture.from = { ...drift };

      const drawAtom = (x, y, scale, alpha) => {
        const r = atomR * scale;
        ctx.save(); ctx.translate(x, y); ctx.rotate(t * 0.6);
        ctx.beginPath(); ctx.ellipse(0, 0, r * 1.7, r * 0.65, 0.5, 0, TAU);
        ctx.strokeStyle = `rgba(${el.rgb},0.4)`; ctx.lineWidth = 1; ctx.stroke(); ctx.restore();
        const glow = ctx.createRadialGradient(x, y, r * 0.2, x, y, r * 3);
        glow.addColorStop(0, `rgba(${el.rgb},0.5)`);
        glow.addColorStop(0.5, `rgba(${el.rgb},0.14)`);
        glow.addColorStop(1, "rgba(0,0,0,0)");
        ctx.fillStyle = glow;
        ctx.beginPath();
        ctx.arc(x, y, r * 3, 0, TAU);
        ctx.fill();

        const body = ctx.createRadialGradient(x - r * 0.35, y - r * 0.4, r * 0.1, x, y, r);
        body.addColorStop(0, "rgba(255,255,255,0.95)");
        body.addColorStop(0.38, `rgba(${el.rgb},0.92)`);
        body.addColorStop(1, `rgba(${el.rgb},0.35)`);
        ctx.beginPath();
        ctx.arc(x, y, r, 0, TAU);
        ctx.fillStyle = body;
        ctx.fill();
        ctx.strokeStyle = "rgba(255,255,255,0.55)";
        ctx.lineWidth = 1;
        ctx.stroke();

        ctx.fillStyle = "rgba(6,6,14,0.9)";
        ctx.font = `600 ${Math.max(11, r * 0.85)}px system-ui, sans-serif`;
        ctx.textAlign = "center";
        ctx.textBaseline = "middle";
        ctx.fillText(el.symbol, x, y + 1);

        const pr = p.progress || 0;
        if (pr > 0.002) {
          const gold = p.gameState === "stable" || p.gameState === "transformed";
          ctx.save();
          ctx.translate(x, y);
          ctx.beginPath();
          ctx.arc(0, 0, r * 1.5, -Math.PI / 2, -Math.PI / 2 + TAU * pr);
          ctx.strokeStyle = gold ? "rgba(251,191,36,0.95)" : "rgba(255,255,255,0.55)";
          ctx.lineWidth = 2.4;
          ctx.lineCap = "round";
          ctx.shadowColor = "rgba(251,191,36,0.85)";
          ctx.shadowBlur = gold ? 14 : 0;
          ctx.stroke();
          ctx.restore();
        }

        ctx.font = `500 ${Math.max(9, r * 0.42)}px ui-monospace, monospace`;
        ctx.fillStyle = "rgba(255,255,255,0.62)";
        ctx.fillText(`${el.freq} Hz`, x, y + r * 1.95);
      };

      if (el) {
        ctx.save();
        if (capture) {
          const angle = Math.atan2(capture.from.y - cy, capture.from.x - cx) + (reduced.matches ? 0 : ck * TAU * 1.3);
          const radius = Math.hypot(capture.from.x - cx, capture.from.y - cy) * (1 - ck) ** 1.4;
          const x = cx + Math.cos(angle) * radius;
          const y = cy + Math.sin(angle) * radius;
          ctx.globalAlpha = 1 - ck * 0.9;
          drawAtom(x, y, 1 - ck * 0.82, 1);
        } else {
          drawAtom(drift.x, drift.y, 1, 1);
        }
        ctx.restore();

        if (capture && ck >= 1) {
          anim.current.capture = null;
          anim.current.flash = now;
          if (doneRef.current) doneRef.current();
        }
      }

      if (flashK > 0) {
        ctx.beginPath();
        ctx.arc(cx, cy, coreR * (1 + (1 - flashK) * 1.6), 0, TAU);
        ctx.strokeStyle = `rgba(255,245,220,${flashK * 0.5})`;
        ctx.lineWidth = 2;
        ctx.stroke();
      }

      raf = requestAnimationFrame(draw);
    };

    raf = requestAnimationFrame(draw);
    return () => {
      cancelAnimationFrame(raf);
      ro.disconnect();
    };
  }, []);

  return <canvas ref={canvasRef} className="absolute inset-0 block w-full h-full" />;
}

const ELEMENT_COLORS = {
  H: "34,211,238",
  He: "167,139,250",
  C: "251,191,36",
  N: "96,165,250",
  O: "249,115,22",
  Fe: "244,63,94",
};