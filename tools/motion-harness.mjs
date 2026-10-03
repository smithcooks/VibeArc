// Local-only diagnostic fixture, not served by GitHub Pages (/docs).
// It exercises the production engine without changing the user's OS preference.
const response = await fetch('../docs/index.html');
let html = await response.text();
html = html.replace(/<script[^>]*src="script.js"[^>]*><\/script>/, '')
  .replace(/href="(styles.css|mark.svg|space-grotesk.ttf)"/g, 'href="../docs/$1"');
document.open(); document.write(html); document.close();
const originalMedia = window.matchMedia.bind(window);
window.matchMedia = query => query.includes('prefers-reduced-motion') ? {matches:false,addEventListener(){},removeEventListener(){}} : originalMedia(query);
const originalFrame = window.requestAnimationFrame.bind(window);
let frameTime = 0, gap = 16.67, fakeHidden = false;
window.requestAnimationFrame = callback => originalFrame(() => {frameTime += gap;callback(frameTime);});
Object.defineProperty(document, 'hidden', {get:() => fakeHidden || document.visibilityState === 'hidden'});
const controls = document.createElement('div'); controls.style.cssText = 'position:fixed;right:8px;top:8px;z-index:100;background:#07070a;color:white;padding:8px;border:1px solid white;font:12px Arial';
controls.innerHTML = '<strong>LOCAL SYNTHETIC MOTION TEST</strong><br><button id="test-slow">Simulate 40 FPS</button> <button id="test-hide">Simulate hidden tab</button>';
document.body.append(controls);
const fixtureStyles=document.createElement('style');fixtureStyles.textContent='html[data-motion=full] .depth{transform:translate3d(0,var(--dy,0px),0)!important}html[data-motion=full] .tilt{transform:rotateX(var(--tx,0deg)) rotateY(var(--ty,0deg))!important}html[data-motion=full][data-tier="0"] #dust,html[data-motion=full][data-tier="1"] #dust{display:block!important}';document.head.append(fixtureStyles);
document.querySelector('#test-slow').addEventListener('click', () => {gap=25;});
document.querySelector('#test-hide').addEventListener('click', event => {fakeHidden=!fakeHidden;event.target.textContent=fakeHidden?'Resume test tab':'Simulate hidden tab';document.dispatchEvent(new Event('visibilitychange'));});
await import('../docs/script.js');
