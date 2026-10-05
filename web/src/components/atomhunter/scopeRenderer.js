const TAU = Math.PI * 2;

export default function scopeRenderer(ctx, W, H, time, { targetFreq = 320, carrierFreq, progress = 0, gameState }, colors) {
  if (W < 4 || H < 4) return;
  const locked = gameState !== 'idle', captured = gameState === 'transformed';
  const accent = captured ? colors.gold : colors.cyan, mid = W / 2;
  const radius = Math.min(W * 0.33, H * 0.16), cy = 28 + radius;
  ctx.clearRect(0, 0, W, H); ctx.save();
  ctx.fillStyle = colors.surface; ctx.fillRect(0, 0, W, H);
  ctx.strokeStyle = colors.ink; ctx.globalAlpha = 0.07; ctx.lineWidth = 1;
  ctx.beginPath();
  for (let x = 8; x < W; x += 12) { ctx.moveTo(x, 22); ctx.lineTo(x, H - 56); }
  for (let y = 24; y < H - 56; y += 16) { ctx.moveTo(6, y); ctx.lineTo(W - 6, y); }
  ctx.stroke(); ctx.globalAlpha = 1;
  // The lens is a Lissajous comparison: equal frequencies become a clean circle.
  ctx.strokeStyle = colors.muted; ctx.globalAlpha = 0.3; ctx.beginPath(); ctx.arc(mid, cy, radius + 4, 0, TAU); ctx.stroke(); ctx.globalAlpha = 1;
  ctx.strokeStyle = accent; ctx.lineWidth = 2; ctx.shadowColor = accent; ctx.shadowBlur = locked ? 13 : 5;
  ctx.beginPath();
  const ratio = carrierFreq / targetFreq;
  for (let i = 0; i <= 160; i++) {
    const a = i / 160 * TAU, x = mid + Math.cos(a) * radius * 0.78;
    const y = cy + Math.sin(a * ratio + Math.sin(time * 1.3) * Math.min(0.6, Math.abs(1 - ratio))) * radius * 0.78;
    if (i) ctx.lineTo(x, y); else ctx.moveTo(x, y);
  }
  ctx.stroke(); ctx.shadowBlur = 0;
  ctx.strokeStyle = captured ? colors.gold : colors.cyan; ctx.lineWidth = 3;
  ctx.beginPath(); ctx.arc(mid, cy, radius + 4, -Math.PI / 2, -Math.PI / 2 + TAU * progress); ctx.stroke();
  const top = cy + radius + 18, bottom = H - 65, height = bottom - top;
  if (height > 20) {
    const trace = (freq, color, width, alpha) => {
      ctx.beginPath(); ctx.strokeStyle = color; ctx.lineWidth = width; ctx.globalAlpha = alpha;
      ctx.shadowColor = color; ctx.shadowBlur = locked ? 12 : 6;
      for (let y = top; y <= bottom; y += 1.5) {
        const a = (y - top) / height, envelope = Math.sin(a * Math.PI) ** 0.4;
        const x = mid + Math.sin(a * TAU * freq / 110 - time * 2.2) * W * 0.34 * envelope;
        if (y === top) ctx.moveTo(x, y); else ctx.lineTo(x, y);
      }
      ctx.stroke();
    };
    trace(targetFreq, colors.violet, 3, 0.7); trace(carrierFreq, accent, 1.5, 1);
    ctx.shadowBlur = 0; ctx.globalAlpha = 0.18;
    const scanY = top + (time * 0.17 % 1) * height;
    ctx.fillStyle = accent; ctx.fillRect(5, scanY, W - 10, 2);
  }
  ctx.restore();
}