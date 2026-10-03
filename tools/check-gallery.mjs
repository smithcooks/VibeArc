import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { runInNewContext } from 'node:vm';

const source = readFileSync(new URL('../docs/script.js', import.meta.url), 'utf8');
function element() {
  const classes = new Set();
  return {
    events: {}, attributes: {}, dataset: {}, textContent: '',
    classList: { add: name => classes.add(name), remove: name => classes.delete(name), toggle: (name,on) => on ? classes.add(name) : classes.delete(name), contains: name => classes.has(name) },
    addEventListener(name, callback) { this.events[name] = callback; },
    setAttribute(name, value) { this.attributes[name] = value; },
    getAttribute(name) { return this.attributes[name]; },
    querySelector() { return null; },
    focus() { this.focused = true; }
  };
}
function mount(code = source, systemReduced = false) {
  const dialog = element(), image = element(), title = element(), close = element(), toggle = element(), link = element(), body = element(), root = element(), reveal = element();
  link.dataset.title = 'Search';
  link.attributes.href = 'screenshots/search.jpg';
  dialog.showModal = () => { dialog.open = true; };
  dialog.close = () => { dialog.open = false; dialog.events.close(); };
  const system = { matches: systemReduced, addEventListener() {} };
  const selectors = { '#screenshot-dialog':dialog, '#dialog-image':image, '#dialog-title':title, '#close-dialog':close, '#motion-toggle':toggle };
  runInNewContext(code, {
    document: { body, documentElement:root, querySelector:name => selectors[name], querySelectorAll:name => name === '[data-screenshot]' ? [link] : [reveal] },
    window:{}, matchMedia:() => system
  });
  return { dialog,image,title,close,toggle,link,body,root,reveal };
}
const app = mount();
let prevented = false;
app.link.events.click({ preventDefault() { prevented = true; } });
assert.ok(prevented && app.dialog.open);
assert.equal(app.image.src, 'screenshots/search.jpg');
assert.equal(app.image.alt, 'Search screenshot');
assert.equal(app.title.textContent, 'Search');
assert.ok(app.body.classList.contains('dialog-open'));
app.close.events.click();
assert.equal(app.dialog.open, false);
assert.ok(app.link.focused);
assert.equal(app.body.classList.contains('dialog-open'), false);
app.toggle.events.click();
assert.ok(app.root.classList.contains('reduce-motion'));
app.toggle.events.click();
assert.equal(app.root.classList.contains('reduce-motion'), false);
assert.ok(app.reveal.classList.contains('visible'), 'No observer must not hide content');
const reduced = mount(source, true);
assert.ok(reduced.root.classList.contains('reduce-motion'));
assert.equal(reduced.toggle.disabled, true);
// Mutation guard: removing the OS override must be caught by the behavior test.
const mutation = mount(source.replace('manualReduction || systemMotion.matches', 'manualReduction && systemMotion.matches'), true);
assert.equal(mutation.root.classList.contains('reduce-motion'), false);
console.log('Gallery behavior passed: modal data, dismissal, focus return, motion toggle, OS preference and no-observer fallback.');
