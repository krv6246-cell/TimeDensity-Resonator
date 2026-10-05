// A short, filtered synth riff and mechanical drum kit. No samples or external tracks.
export default function industrialHit(ctx, output, bucket, frequency, when, step) {
  const track = (source, nodes, gain, duration) => {
    const entry = { nodes: [source], gain, cleanup: () => nodes.forEach(n => n.disconnect()) };
    bucket.add(entry); source.onended = () => { bucket.delete(entry); entry.cleanup(); };
    source.start(when); source.stop(when + duration);
  };
  const tone = (freq, duration, level, type, cutoff, drive = false, drop = false) => {
    const osc = ctx.createOscillator(), amp = ctx.createGain(), filter = ctx.createBiquadFilter();
    osc.type = type; osc.frequency.setValueAtTime(freq, when);
    if (drop) osc.frequency.exponentialRampToValueAtTime(38, when + duration * 0.8);
    amp.gain.setValueAtTime(0.0001, when); amp.gain.exponentialRampToValueAtTime(level, when + 0.008);
    amp.gain.exponentialRampToValueAtTime(0.0001, when + duration);
    filter.type = 'lowpass'; filter.frequency.value = cutoff; filter.Q.value = 0.65;
    const nodes = [osc, amp, filter];
    if (drive) {
      const shaper = ctx.createWaveShaper();
      shaper.curve = distortion; shaper.oversample = '2x';
      osc.connect(shaper); shaper.connect(filter); nodes.push(shaper);
    } else osc.connect(filter);
    filter.connect(amp); amp.connect(output); track(osc, nodes, amp, duration + 0.02);
  };
  const noise = (duration, level, cutoff) => {
    const source = ctx.createBufferSource(), filter = ctx.createBiquadFilter(), amp = ctx.createGain();
    const buffer = ctx.createBuffer(1, Math.ceil(ctx.sampleRate * duration), ctx.sampleRate), data = buffer.getChannelData(0);
    for (let i = 0; i < data.length; i++) data[i] = Math.random() * 2 - 1;
    source.buffer = buffer; filter.type = 'highpass'; filter.frequency.value = cutoff;
    amp.gain.setValueAtTime(level, when); amp.gain.exponentialRampToValueAtTime(0.0001, when + duration);
    source.connect(filter); filter.connect(amp); amp.connect(output); track(source, [source, filter, amp], amp, duration + 0.01);
  };
  // Eight eighth-notes per captured atom: kick / snare march, palm-muted root and fifth.
  if (step % 2 === 0 || step === 7) {
    tone(frequency / 2, 0.19, 0.075, 'sawtooth', 1300, true);
    tone(frequency * 0.75, 0.16, 0.035, 'sawtooth', 1100, true);
    tone(frequency / 4, 0.24, 0.07, 'sine', 400);
  }
  if (step === 0 || step === 4 || step === 5) tone(125, 0.19, 0.22, 'sine', 850, false, true);
  if (step === 2 || step === 6) { noise(0.15, 0.14, 1300); tone(185, 0.09, 0.05, 'triangle', 1800); }
  noise(step % 2 ? 0.07 : 0.04, step % 2 ? 0.024 : 0.035, 5800);
}
const distortion = Float32Array.from({ length: 512 }, (_, i) => Math.tanh((i / 255.5 - 1) * 7));