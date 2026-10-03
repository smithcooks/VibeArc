import assert from 'node:assert/strict';
import { readFileSync, existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import './check-gallery.mjs';

const root = fileURLToPath(new URL('../docs/', import.meta.url));
const html = readFileSync(root + 'index.html', 'utf8');
const css = readFileSync(root + 'styles.css', 'utf8');
const js = readFileSync(root + 'script.js', 'utf8');
// The October brief replaces the illustrated dark player with real captures.
assert.match(html, /Captured from Vibe Arc v0\.9 beta/);
for (const screen of ['player', 'home', 'search', 'library', 'discovery', 'accounts', 'appearance', 'about']) {
  assert.ok(existsSync(root + `screenshots/${screen}.jpg`));
  assert.match(html, new RegExp(`screenshots/${screen}\\.jpg`));
}
for (const image of html.matchAll(/<img\b[^>]+>/g)) {
  assert.match(image[0], /alt="[^"]*"/);
  assert.match(image[0], /width="\d+"/);
  assert.match(image[0], /height="\d+"/);
}
assert.match(html, /releases\/download\/v1\.0\.0\/VibeArc-v1\.0\.0\.apk/);
assert.match(html, /disabled[^>]*>Windows/);
assert.match(html, /disabled[^>]*>Linux/);
assert.doesNotMatch(html, /Nuclear|Nuki|Discord|macOS|Flathub|Snapcraft|winget|Homebrew|lorem ipsum/i);
assert.match(html, /<dialog[^>]+aria-labelledby=/);
assert.match(css, /prefers-reduced-motion:\s*reduce/);
assert.match(css, /:focus-visible/);
assert.match(js, /showModal\(/);
assert.match(js, /addEventListener\('close'/);
assert.doesNotMatch(js, /innerHTML|setInterval|requestAnimationFrame|fetch\(/);
assert.match(html, /styles\.css\?v=real-screens-20261003/);
assert.match(html, /script\.js\?v=real-screens-20261003/);
assert.match(html, /href="https:\/\/buymeachai\.ezee\.li\/Smith_cooks" target="_blank" rel="noopener noreferrer"/);
for (const [, attribute, value] of html.matchAll(/\b(src|href)="([^"]+)"/g)) {
  if (!value.startsWith('https:') && !value.startsWith('#')) {
    assert.ok(existsSync(root + value.split('?')[0]), `Missing local ${attribute}: ${value}`);
  }
}
function luminance(hex) {
  const channels = hex.match(/\w\w/g).map(n => parseInt(n, 16) / 255)
    .map(n => n <= .04045 ? n / 12.92 : ((n + .055) / 1.055) ** 2.4);
  return channels[0] * .2126 + channels[1] * .7152 + channels[2] * .0722;
}
for (const [ink, surface] of [['141414','ffffff'], ['525252','ffffff'], ['ffffff','3548d5'], ['141414','f8dce5'], ['95364f','ffffff'], ['141414','e9edff']]) {
  const values = [luminance(ink), luminance(surface)].sort((a,b) => b-a);
  assert.ok((values[0]+.05)/(values[1]+.05) >= 4.5, `Contrast failed ${ink}/${surface}`);
}
console.log('Website contract passed: real captures, release links, platforms, local assets, dialog and AA palette.');
