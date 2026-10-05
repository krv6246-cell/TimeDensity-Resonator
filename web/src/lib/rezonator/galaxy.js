import { ELEMENTS } from '@/lib/rezonator/elements';
const TAU = Math.PI * 2;
const seeded = n => { const v = Math.sin(n * 127.1 + 311.7) * 43758.5453; return v - Math.floor(v); };

export function galaxyCloud(captured) {
  const dust = [], perAtom = Math.min(220, Math.floor(2200 / Math.max(1, captured.length)));
  captured.forEach((key, index) => {
    const radius = 0.28 + 0.65 * Math.sqrt((index + 1) / (captured.length + 1));
    const angle = index * 2.39996;
    for (let j = 0; j < perAtom; j++) {
      const seed = index * 509 + j, r = Math.max(0.045, Math.min(1, radius + (seeded(seed) - 0.5) * 0.65));
      dust.push({ index, rgb: ELEMENTS[key].rgb, r, angle: angle + r * 4.7 + (seeded(seed + 77) - 0.5) * 0.6, size: 0.35 + seeded(seed + 38) * 1.1, alpha: 0.18 + seeded(seed + 11) * 0.6 });
    }
  });
  return dust;
}

export default function drawGalaxy(ctx, W, H, elapsed, captured, dust, playback, reduced) {
  if (W < 4 || H < 4) return;
  const cx = W / 2, cy = H * 0.47, radius = Math.min(W * 0.43, H * 0.55), time = reduced ? 0 : elapsed / 1000;
  const collapse = elapsed < 2200 ? 1 : Math.max(0.025, 1 - (elapsed - 2200) / 1200);
  const expansion = Math.min(1, Math.max(0, (elapsed - 3400) / 2400));
  const pulse = reduced ? 0 : playback.pulse;
  ctx.fillStyle = '#070910'; ctx.fillRect(0, 0, W, H);
  for (let i = 0; i < 100; i++) { ctx.fillStyle = `rgba(210,222,255,${0.12 + seeded(i + 150) * 0.35})`; ctx.fillRect(seeded(i) * W, seeded(i + 900) * H, 1, 1); }
  if (!captured.length) return;
  const halo = ctx.createRadialGradient(cx, cy, 0, cx, cy, radius * 1.3);
  halo.addColorStop(0, `rgba(167,139,250,${0.18 + pulse * 0.12})`); halo.addColorStop(0.4, 'rgba(96,100,220,0.07)'); halo.addColorStop(1, 'transparent'); ctx.fillStyle = halo; ctx.fillRect(0, 0, W, H);
  if (elapsed >= 3400) {
    ctx.save(); ctx.globalCompositeOperation = 'lighter';
    for (const star of dust) {
      const angle = star.angle + time * 0.045, r = star.r * radius * expansion * (1 + pulse * 0.025);
      const x = cx + Math.cos(angle) * r, y = cy + Math.sin(angle) * r * 0.55;
      ctx.fillStyle = `rgba(${star.rgb},${Math.min(1, star.alpha + (playback.index === star.index ? pulse * 0.45 : 0))})`;
      ctx.beginPath(); ctx.arc(x, y, star.size * (1 + pulse * 0.25), 0, TAU); ctx.fill();
    }
    ctx.restore();
  }
  captured.forEach((key, i) => {
    const el = ELEMENTS[key], r = elapsed < 3400 ? radius * 0.7 * collapse : radius * (0.28 + 0.65 * Math.sqrt((i + 1) / (captured.length + 1))) * expansion;
    const angle = i * 2.39996 + (elapsed < 3400 ? time * 0.35 : r / radius * 4.7 + time * 0.045);
    const x = cx + Math.cos(angle) * r, y = cy + Math.sin(angle) * r * 0.55, active = playback.index === i;
    const glow = ctx.createRadialGradient(x, y, 0, x, y, active ? 30 : 16); glow.addColorStop(0, `rgba(${el.rgb},${active ? 0.8 : 0.45})`); glow.addColorStop(1, 'transparent'); ctx.fillStyle = glow; ctx.fillRect(x - 30, y - 30, 60, 60);
    ctx.fillStyle = el.color; ctx.beginPath(); ctx.arc(x, y, active ? 4 + pulse * 2 : 2.8, 0, TAU); ctx.fill();
    if (captured.length <= 18 || active) { ctx.font = '10px system-ui'; ctx.textAlign = 'center'; ctx.fillStyle = '#eaeef9'; ctx.fillText(key, x, y - 12); }
  });
  const core = ctx.createRadialGradient(cx, cy, 0, cx, cy, 24 + pulse * 8); core.addColorStop(0, '#fff5dd'); core.addColorStop(0.12, 'rgba(255,222,174,.9)'); core.addColorStop(1, 'transparent'); ctx.fillStyle = core; ctx.fillRect(cx - 34, cy - 34, 68, 68);
}