export const tracks = [
  { title: 'Golden hour', mood: 'Champagne', bpm: 96, palette: [217, 183, 126], art: 'gold' },
  { title: 'Heat bloom', mood: 'Vermilion', bpm: 108, palette: [232, 116, 94], art: 'red' },
  { title: 'Into the green', mood: 'Forest', bpm: 84, palette: [130, 173, 145], art: 'green' },
  { title: 'Night current', mood: 'Cobalt', bpm: 128, palette: [106, 144, 223], art: 'blue' },
  { title: 'Soft orbit', mood: 'Pearl', bpm: 92, palette: [212, 206, 223], art: 'pearl' },
];
export const clamp = (v, lo = 0, hi = 1) => Math.max(lo, Math.min(hi, v));
export const mix = (a, b, t) => a.map((v, i) => v + (b[i] - v) * clamp(t));
export const beatPhase = beats => ((beats % 1) + 1) % 1;
export const depthOffset = (delta, depth, mobile) => clamp(delta * depth, mobile ? -40 : -120, mobile ? 40 : 120);
export const qualityTier = (tier, averageMs) => averageMs > 20 ? Math.min(3, tier + 1) : tier;
export const seeded = n => { const x = Math.sin(n * 127.1 + 311.7) * 43758.5453; return x - Math.floor(x); };
const luminance = rgb => rgb.map(c => c / 255).map(c => c <= .04045 ? c / 12.92 : ((c + .055) / 1.055) ** 2.4).reduce((s, c, i) => s + c * [.2126, .7152, .0722][i], 0);
export const contrast = (a, b) => { const x = luminance(a), y = luminance(b); return (Math.max(x, y) + .05) / (Math.min(x, y) + .05); };
// A conservative upper bound for every glass/text surface, not just the black body.
export function readableAccent(rgb) {
  let color = [...rgb];
  while (contrast(color, [64, 64, 70]) < 4.7) color = mix(color, [255, 255, 255], .06);
  return color.map(Math.round);
}
