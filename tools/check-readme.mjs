import assert from 'node:assert/strict';
import { readFileSync, existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('../', import.meta.url));
const readme = readFileSync(root + 'README.md', 'utf8');
for (const screen of ['home', 'player', 'search', 'library', 'discovery', 'appearance', 'accounts', 'about']) {
  assert.match(readme, new RegExp(`docs/screenshots/${screen}\\.jpg`));
}
for (const [, src] of readme.matchAll(/<img\b[^>]*src="([^"]+)"/g)) {
  if (!src.startsWith('https:')) assert.ok(existsSync(root + src), `Missing image: ${src}`);
}
assert.match(readme, /https:\/\/buymeachai\.ezee\.li\/Smith_cooks/);
assert.match(readme, /https:\/\/buymeachai\.ezee\.li\/assets\/images\/buymeachai-button\.png/);
assert.match(readme, /https:\/\/github\.com\/sponsors\/smithcooks/);
assert.match(readme, /GitHub Sponsors is not active yet/);
assert.match(readme, /Client-YouTube%20Music/);
assert.match(readme, /Platform-Android/);
assert.match(readme, /VibeArc<\/b> is built with ❤️ by .*Smith/);
assert.match(readme, /Screenshots captured from VibeArc v0\.9 beta/);
assert.match(readme, /not implemented/);
assert.match(readme, /deployed HTTPS signer/);
assert.doesNotMatch(readme, /Clash-Projects\/LastWave.*releases|ajisth69|duxtami|zero battery drain|millisecond-accurate/i);
console.log('README passed: eight real captures, VibeArc badges, support links, honest limitations and Smith credit.');
