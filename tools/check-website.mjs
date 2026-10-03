import assert from 'node:assert/strict';
import { readFile, access } from 'node:fs/promises';
import { runInNewContext } from 'node:vm';

const root = new URL('../docs/', import.meta.url);
const html = await readFile(new URL('index.html', root), 'utf8');
assert.equal((html.match(/<h1\b/g) || []).length, 1);
assert.match(html, /https:\/\/github.com\/smithcooks\/VibeArc\/releases\/download\/v1\.0\.0\/VibeArc-v1\.0\.0\.apk/);
assert.match(html, /<button[^>]*disabled[^>]*>[\s\S]*?Coming soon/i);
for (const [, file] of html.matchAll(/(?:src|href)="([^"#:]+)"/g)) {
  if (!file.startsWith('https://')) await access(new URL(file, root));
}

const css = await readFile(new URL('styles.css', root), 'utf8');
assert.match(css, /prefers-reduced-motion:\s*reduce/);
const script = await readFile(new URL('script.js', root), 'utf8');
for (const reduced of [true, false]) {
  const buttons = ['mint', 'ember', 'aurora'].map(theme => ({
    dataset: { theme }, attributes: {},
    setAttribute(key, value) { this.attributes[key] = value; },
    addEventListener(_, handler) { this.click = handler; },
  }));
  let offset = null;
  const documentElement = { dataset: {}, style: { setProperty(_, value) { offset = value; } } };
  const status = { textContent: '' };
  let scrollHandler;
  const motion = { matches: reduced, addEventListener() {} };
  const desktop = { matches: true, addEventListener() {} };
  runInNewContext(script, {
    document: { documentElement, hidden: false,
      querySelectorAll: () => buttons,
      querySelector: () => status },
    matchMedia: query => query.includes('reduced-motion') ? motion : desktop,
    addEventListener: (event, handler) => { if (event === 'scroll') scrollHandler = handler; },
    requestAnimationFrame: callback => callback(), scrollY: 500,
  });
  buttons[1].click();
  assert.equal(documentElement.dataset.theme, 'ember');
  assert.equal(buttons[1].attributes['aria-pressed'], 'true');
  assert.equal(buttons[0].attributes['aria-pressed'], 'false');
  assert.match(status.textContent, /Ember/);
  assert.equal(Boolean(scrollHandler), !reduced);
  if (scrollHandler) {
    scrollHandler();
    assert.equal(offset, '500px');
    motion.matches = true;
    scrollHandler();
    assert.equal(offset, '0px', 'Reduced motion must stop parallax after a preference change');
    motion.matches = false;
    desktop.matches = false;
    scrollHandler();
    assert.equal(offset, '0px', 'Touch/small layouts must stop parallax');
  }
}
console.log('Website checks passed: downloads, assets, palette, reduced motion.');
