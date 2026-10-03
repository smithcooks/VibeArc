import assert from 'node:assert/strict';
import { readFile, access } from 'node:fs/promises';
import { tracks, mix, readableAccent, contrast, qualityTier, beatPhase, depthOffset, seeded } from '../docs/motion-core.mjs';

assert.equal(tracks.length, 5);
assert.equal(tracks[0].bpm, 96);
assert.ok(tracks.every(t => t.bpm >= 84 && t.bpm <= 128));
for (const a of tracks) for (const b of tracks) for (let i = 0; i <= 20; i++) {
  const accent = readableAccent(mix(a.palette, b.palette, i / 20));
  assert.ok(contrast(accent, [37, 37, 42]) >= 4.5, 'Accent text needs AA through interpolation');
  assert.ok(contrast(accent, [64, 64, 70]) >= 4.5, 'Brightest permitted glass surface also needs AA');
  assert.ok(contrast(accent, [7, 7, 10]) >= 4.5);
  assert.ok(contrast(accent, [7, 7, 10]) >= contrast(accent, [255, 255, 255]), 'CTA uses dark text');
}
assert.equal(qualityTier(0, 16.7), 0);
assert.equal(qualityTier(0, 21), 1);
assert.equal(qualityTier(2, 25), 3);
assert.equal(qualityTier(3, 100), 3);
assert.equal(beatPhase(0), 0);
assert.ok(beatPhase(8.3) >= 0 && beatPhase(8.3) < 1);
assert.equal(depthOffset(2000, .25, true), 40);
assert.equal(depthOffset(-2000, .25, false), -120);
assert.equal(seeded(4), seeded(4));

const root = new URL('../docs/', import.meta.url);
const html = await readFile(new URL('index.html', root), 'utf8');
const css = await readFile(new URL('styles.css', root), 'utf8');
const js = await readFile(new URL('script.js', root), 'utf8');
assert.equal((html.match(/<h1\b/g) || []).length, 1);
assert.match(html, /VibeArc-v1\.0\.0\.apk/);
assert.match(html, /disabled[^>]*>[\s\S]*?Coming soon/);
assert.match(html, /id="motion-toggle"/);
assert.equal((js.match(/requestAnimationFrame\(/g) || []).length, 1, 'Only one scheduling point');
assert.doesNotMatch(js, /setInterval|localStorage|sessionStorage|fetch\(/);
assert.match(js, /visibilitychange/);
assert.match(js, /IntersectionObserver/);
assert.match(js, /requestPermission/);
assert.match(js, /document\.body\.querySelectorAll/, 'Track metadata selectors must exclude the HTML root that also carries diagnostics');
assert.match(css, /prefers-reduced-motion/);
assert.match(css, /@supports/);
assert.match(css, /\.album-choice \.cover-art\{display:block/, 'Covers must occupy their reserved square');
assert.doesNotMatch(css, /@keyframes/, 'Continuous animation belongs to the shared clock');
for (const [, file] of html.matchAll(/(?:src|href)="([^"#:]+)"/g)) {
  if (!file.startsWith('https://')) await access(new URL(file, root));
}
console.log('Website checks passed: 5 tracks, interpolated AA contrast, governor, beat, motion bounds, assets, single clock.');
