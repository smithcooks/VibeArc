import { tracks, clamp, mix, readableAccent, qualityTier, beatPhase, depthOffset, seeded } from './motion-core.mjs';

const root = document.documentElement;
const $ = selector => document.querySelector(selector);
const $$ = selector => [...document.body.querySelectorAll(selector)];
const systemMotion = matchMedia('(prefers-reduced-motion: reduce)');
const finePointer = matchMedia('(pointer: fine)');
const stack = $('#album-stack'), demo = $('#color-demo'), rail = $('#screen-rail');
const dialog = $('#full-player'), mini = $('.mini-player');
const canvas = $('#dust'), ctx = canvas.getContext('2d');
let manualReduced = false, reduced = systemMotion.matches, tier = 0;
let active = 0, color = [...tracks[0].palette], colorTween = null;
let time = 0, beats = 0, lastStamp = null, raf = 0;
let scrollTarget = scrollY, scrollFollow = scrollY, resizePending = true, scrollDirty = true;
let demoTop = 0, demoRange = 1, width = innerWidth, height = innerHeight;
let manualPaletteAt = null, railTarget = null, morph = null;
let pointer = { x: -1000, y: -1000, smoothX: -1000, smoothY: -1000, interactive: false, scale: 1 };
let tiltTarget = null, magnetTarget = null, sensor = null, sensorAttached = false;
let samples = 0, sampleSum = 0, initialSample = true, sampleStart = 0;
const ambientVisible = new Set();
const aurora = $$('.aurora i'), bars = $$('.waveform i'), bobbers = $$('.bob');
const depthLayers = $$('.depth').map(node => ({ node, anchor: 0, depth: Number(node.dataset.depth) }));
const reveals = new Map();
const coverPositions = tracks.map(() => ({ x: 0, y: 0, z: 0, rotation: 0, scale: 1, opacity: 1 }));
const coverTargets = tracks.map(() => ({}));
const particles = Array.from({ length: 60 }, (_, i) => ({ x: seeded(i + 3), y: seeded(i + 73), size: .8 + seeded(i + 183) * 1.5, speed: 3 + seeded(i + 39) * 8 }));
const sprite = document.createElement('canvas'); sprite.width = sprite.height = 24;
const spriteContext = sprite.getContext('2d');
if (spriteContext) { const glow = spriteContext.createRadialGradient(12, 12, 0, 12, 12, 12); glow.addColorStop(0, '#ffffffc0'); glow.addColorStop(.25, '#ffffff35'); glow.addColorStop(1, '#ffffff00'); spriteContext.fillStyle = glow; spriteContext.fillRect(0, 0, 24, 24); }

// All artwork and preview text are our own fixed content. No provider calls or audio.
stack.innerHTML = tracks.map((track, i) => `<button class="album-choice" data-track="${i}" aria-label="Play visual session ${track.title}, ${track.mood}, ${track.bpm} BPM" aria-pressed="${i === 0}"><span class="cover-art cover-${track.art}"><span class="art-shape"></span><span class="cover-stamp">V / A</span><span class="cover-label">${track.title}<small>VISUAL SESSIONS / 0${i + 1}</small></span></span></button>`).join('');
$('#palette-picker').innerHTML = tracks.map((track, i) => `<button class="palette-button" data-track="${i}" aria-label="${track.mood} atmosphere" aria-pressed="${i === 0}" style="--swatch:rgb(${track.palette.join(' ')})"><i aria-hidden="true"></i></button>`).join('');
const covers = $$('.album-choice');
const screenNames = ['Home', 'Now playing', 'Lyrics', 'Search', 'Library', 'Queue', 'Playlists', 'Equalizer', 'Appearance', 'Listening stats'];
const thumb = art => `<div class="screen-thumb cover-${art}"><div class="art-shape"></div></div>`;
const row = (title, subtitle, art = 'gold') => `<div class="screen-line">${thumb(art)}<div><strong>${title}</strong><small>${subtitle}</small></div></div>`;
const previewScreens = [
  '<h3>Good evening.</h3><p class="screen-sub">A little space for your sound.</p><div class="screen-banner"><strong>In your<br>element.</strong><span>Start a listening session ↗</span></div><h4>Jump back in</h4>' + row('Golden hour', 'Vibe Arc Sessions') + row('Heat bloom', 'Vibe Arc Sessions', 'red'),
  '<p class="screen-sub">NOW PLAYING</p><div class="screen-now-art live-cover" data-cover><div class="art-shape"></div></div><h3 data-title>Golden hour</h3><p class="screen-sub">Vibe Arc Sessions</p><div class="phone-wave waveform"><i></i><i></i><i></i><i></i><i></i><i></i><i></i><i></i></div><div class="phone-controls"><span>Ⅰ◀</span><span class="play-disc">Ⅱ</span><span>▶Ⅰ</span></div>',
  '<h3>Lyrics</h3><p class="screen-sub">Original visual session</p><div class="screen-lyrics"><span>The world can wait.</span><strong>Stay here a<br>little longer.</strong><span>Let the evening in.</span></div>',
  '<h3>Find your sound.</h3><div class="screen-line">Search tracks, artists, albums</div><h4>Explore a mood</h4><span class="screen-chip active">After hours</span><span class="screen-chip">Jazz</span><span class="screen-chip">Electronic</span><span class="screen-chip">Indie</span><h4>Your recent searches</h4>' + row('Night current', 'Visual sessions', 'blue'),
  '<h3>Your library</h3><span class="screen-chip active">Tracks</span><span class="screen-chip">Albums</span>' + tracks.slice(0,4).map(t=>row(t.title, 'Vibe Arc Sessions', t.art)).join(''),
  '<h3>Up next</h3><p class="screen-sub">Your order. Your listening.</p>' + tracks.map(t=>row(t.title, 'Vibe Arc Sessions', t.art)).join(''),
  '<h3>Playlists</h3><p class="screen-sub">Collected moments.</p>' + row('Evening rituals', '12 tracks') + row('For the long way home', '24 tracks', 'blue') + row('A little green', '8 tracks', 'green') + row('Warm light', '16 tracks', 'red'),
  '<h3>Shape the sound.</h3><p class="screen-sub">Equalizer · Visual preview</p><div class="screen-bars">' + Array.from({length:15},(_,i)=>`<i style="--band:${20+seeded(i)*60}%"></i>`).join('') + '</div><span class="screen-chip active">Custom</span><span class="screen-chip">Balanced</span>',
  '<h3>Make it yours.</h3>' + row('Liquid glass', 'Your choice, always') + row('AMOLED', 'A quieter kind of black', 'pearl') + row('Dynamic now playing', 'Colors from your artwork', 'green') + '<h4>Accent</h4><div class="screen-color-row"><i></i><i></i><i></i><i></i></div>',
  '<h3>Your listening.</h3><p class="screen-sub">Illustrative local session</p><div class="screen-stat"><strong>1</strong>visual session</div><h4>In rotation</h4>' + row('Golden hour', 'Vibe Arc Sessions') + row('Into the green', 'Vibe Arc Sessions', 'green'),
];
rail.innerHTML = previewScreens.map((content, i) => `<figure class="screen-card"><div class="screen-shell glass"><div class="screen-inner" aria-hidden="true">${content}<div class="screen-nav"><span>Feed</span><span>Library</span><span>Settings</span></div></div></div><figcaption><strong>${screenNames[i]}</strong><span>${String(i+1).padStart(2,'0')} / 10</span></figcaption></figure>`).join('');
// Include the generated rail waveform in the same clock, not a second animation.
bars.push(...$$('#screen-rail .waveform i'));
$$('.split').forEach(heading => {
  const walker = document.createTreeWalker(heading, NodeFilter.SHOW_TEXT);
  const nodes = []; while (walker.nextNode()) nodes.push(walker.currentNode);
  nodes.forEach(node => {
    const fragment = document.createDocumentFragment();
    node.textContent.split(/(\s+)/).filter(Boolean).forEach(word => {
      if (/^\s+$/.test(word)) fragment.append(document.createTextNode(word));
      else { const span = document.createElement('span'); span.className = 'word'; span.textContent = word; span.dataset.ghost = word; fragment.append(span); }
    }); node.replaceWith(fragment);
  });
});

function palette(rgb) {
  color = [...rgb];
  const safe = readableAccent(rgb);
  root.style.setProperty('--accent', safe.join(' '));
  root.style.setProperty('--on-accent', '#07070a');
}
function setTrack(index, announce = false, tween = true) {
  const next = ((index % tracks.length) + tracks.length) % tracks.length;
  if (next === active && !announce) return;
  active = next; const track = tracks[active];
  root.dataset.art = track.art; root.dataset.track = String(active); root.dataset.bpm = String(track.bpm);
  $$('[data-title]').forEach(node => node.textContent = track.title);
  $$('[data-bpm]').forEach(node => node.textContent = String(track.bpm));
  $$('[data-mood]').forEach(node => node.textContent = track.mood);
  $$('[data-track]').forEach(node => node.setAttribute('aria-pressed', String(Number(node.dataset.track) === active)));
  tracks.forEach((_, i) => {
    let slot = (i - active + 5) % 5; if (slot > 2) slot -= 5;
    coverTargets[i] = { x: slot * (width < 768 ? 32 : 56), y: Math.abs(slot) * 16, z: -Math.abs(slot)*40, rotation: slot * 11, scale: 1 - Math.abs(slot)*.075, opacity: 1 - Math.abs(slot)*.12 };
    covers[i].style.setProperty('--order', String(5 - Math.abs(slot)));
    if (reduced) coverPositions[i] = {...coverTargets[i]};
  });
  if (tween && !reduced) colorTween = { from: [...color], to: track.palette, start: time };
  else if (tween) palette(track.palette);
  if (announce) { $('#track-announcement').textContent = `${track.title}. ${track.mood} atmosphere. ${track.bpm} BPM. Visual preview, no audio.`; manualPaletteAt = scrollTarget; }
  schedule();
}
function chooseTrack(index) { setTrack(index, true); if (reduced) renderStatic(); }
$$('[data-track]').forEach(button => button.addEventListener('click', () => { if (!suppressClick) chooseTrack(Number(button.dataset.track)); suppressClick = false; }));
$$('[data-next]').forEach(button => button.addEventListener('click', () => chooseTrack(active + 1)));
$$('[data-previous]').forEach(button => button.addEventListener('click', () => chooseTrack(active - 1)));
let swipeStart = null, suppressClick = false;
stack.addEventListener('pointerdown', e => { suppressClick=false;if (e.pointerType !== 'mouse') swipeStart = {x:e.clientX,y:e.clientY}; });
stack.addEventListener('pointerup', e => { if (!swipeStart) return; const dx = e.clientX - swipeStart.x, dy = e.clientY - swipeStart.y; swipeStart = null; if (Math.abs(dx) > 35 && Math.abs(dx) > Math.abs(dy)) { suppressClick = true; chooseTrack(active + (dx < 0 ? 1 : -1)); } });
stack.addEventListener('pointercancel', () => { swipeStart = null; });
stack.addEventListener('keydown', e=>{if(e.key==='ArrowLeft'||e.key==='ArrowRight'){e.preventDefault();chooseTrack(active+(e.key==='ArrowRight'?1:-1));}});

const revealObserver = new IntersectionObserver(entries => entries.forEach(entry => {
  if (!entry.isIntersecting) return;
  const stagger=entry.target.classList.contains('tile')?[...entry.target.parentElement.children].indexOf(entry.target)*.06:0;
  reveals.set(entry.target, { start: time+stagger, words: [...entry.target.querySelectorAll('.word')] });
  revealObserver.unobserve(entry.target); schedule();
}), { threshold: .12 });
$$('.reveal').forEach(node => { if (!reduced) { node.style.setProperty('--reveal','0'); node.querySelectorAll('.word').forEach(word => word.style.setProperty('--word-progress','0')); } revealObserver.observe(node); });
const ambientObserver = new IntersectionObserver(entries => { entries.forEach(entry => entry.isIntersecting ? ambientVisible.add(entry.target) : ambientVisible.delete(entry.target)); schedule(); }, { threshold: 0 });
ambientObserver.observe($('#hero')); ambientObserver.observe(demo);

function readMetrics() {
  width = innerWidth; height = innerHeight;
  const rect = demo.getBoundingClientRect(); demoTop = rect.top + scrollTarget; demoRange = Math.max(1, demo.offsetHeight - height);
  depthLayers.forEach(layer => { const section = layer.node.closest('section'); layer.anchor = section.getBoundingClientRect().top + scrollTarget; });
  if (ctx) { const dpr = Math.min(devicePixelRatio || 1, 1.5); canvas.width = Math.round(width*dpr); canvas.height = Math.round(height*dpr); ctx.setTransform(dpr,0,0,dpr,0,0); }
  const old = active; active = -1; setTrack(old, false, false);
  resizePending = false;
}
function scrub() {
  if (manualPaletteAt !== null && Math.abs(scrollTarget-manualPaletteAt) < 16) return;
  manualPaletteAt = null;
  const progress = (scrollTarget-demoTop)/demoRange;
  if (progress < 0 || progress > 1) return;
  const position = clamp(progress)*4, left = Math.min(3,Math.floor(position)), blend = position-left;
  setTrack(Math.round(position), false, false);
  colorTween = null; palette(mix(tracks[left].palette,tracks[left+1].palette,blend));
  root.style.setProperty('--demo-progress', String(clamp(progress,.05,1)));
}
function ease(value) {
  const x = clamp(value); let lo = 0, hi = 1;
  for (let i=0;i<10;i++) { const t=(lo+hi)/2; const bx=3*(1-t)**2*t*.22+3*(1-t)*t*t+t**3; if(bx<x)lo=t;else hi=t; }
  return 1-(1-(lo+hi)/2)**3;
}
function paintCovers(moving = true) {
  covers.forEach((cover,i) => {
    const current=coverPositions[i], target=coverTargets[i];
    Object.keys(target).forEach(key => current[key] = moving ? current[key]+(target[key]-current[key])*.1 : target[key]);
    for (const [key,prop,unit] of [['x','ax','px'],['y','ay','px'],['z','az','px'],['rotation','ar','deg'],['scale','as',''],['opacity','ao','']]) cover.style.setProperty(`--${prop}`,`${current[key].toFixed(3)}${unit}`);
  });
}
function renderStatic() {
  root.style.setProperty('--beat','0'); root.style.setProperty('--pulse','0'); root.style.setProperty('--breath','1'); root.style.setProperty('--rim','0deg');
  aurora.forEach(node => node.style.transform='none'); depthLayers.forEach(layer=>layer.node.style.setProperty('--dy','0px')); bobbers.forEach(node=>node.style.setProperty('--bob','0px'));
  $$('.tilt').forEach(node=>{node.style.setProperty('--tx','0deg');node.style.setProperty('--ty','0deg');});
  $$('.magnetic').forEach(node=>{node.style.setProperty('--mx','0px');node.style.setProperty('--my','0px');});
  $$('.reveal').forEach(node=>{node.style.setProperty('--reveal','1');node.querySelectorAll('.word').forEach(word=>word.style.setProperty('--word-progress','1'));});
  bars.forEach((bar,i)=>bar.style.transform=`scaleY(${.25+seeded(i)*.6})`);
  colorTween=null; palette(tracks[active].palette); paintCovers(false); if(ctx)ctx.clearRect(0,0,width,height);
  if (dialog.open) { dialog.style.transform='none'; dialog.style.opacity='1'; } morph=null;
}
function updateMotion() {
  reduced = manualReduced || systemMotion.matches;
  root.dataset.motion = reduced ? 'reduced' : 'full';
  $('#motion-toggle').setAttribute('aria-pressed', String(reduced));
  $('#motion-toggle').setAttribute('aria-label', systemMotion.matches ? 'Reduce motion, enabled by your device preference' : 'Reduce motion');
  if (reduced) { if(raf)cancelAnimationFrame(raf);raf=0;lastStamp=null;renderStatic(); } else schedule();
}
$('#motion-toggle').addEventListener('click',()=>{manualReduced=!manualReduced;updateMotion();});
systemMotion.addEventListener('change',updateMotion);
addEventListener('scroll',()=>{scrollTarget=scrollY;scrollDirty=true;if(reduced){scrub();}else schedule();},{passive:true});
addEventListener('resize',()=>{resizePending=true;if(reduced){readMetrics();renderStatic();}else schedule();},{passive:true});
addEventListener('pointermove', e=>{pointer.x=e.clientX;pointer.y=e.clientY;tiltTarget=e.target.closest('.tilt');magnetTarget=e.target.closest('.magnetic');pointer.interactive=Boolean(e.target.closest('a,button,summary,[tabindex]'));},{passive:true});
addEventListener('pointerout', e=>{if(!e.relatedTarget){pointer.x=-1000;pointer.y=-1000;tiltTarget=null;magnetTarget=null;}},{passive:true});
document.addEventListener('visibilitychange',()=>{if(document.hidden){if(raf)cancelAnimationFrame(raf);raf=0;lastStamp=null;samples=0;sampleSum=0;}else{lastStamp=null;sampleStart=time;schedule();}});

const tiltButton=$('#tilt-toggle');
if (!finePointer.matches && 'DeviceOrientationEvent' in window) tiltButton.hidden=false;
tiltButton.addEventListener('click',async()=>{
  if(reduced){$('#sensor-status').textContent='Device motion is off while Reduce motion is enabled.';return;}
  if(sensorAttached){sensor=null;sensorAttached=false;removeEventListener('deviceorientation',onOrientation);tiltButton.textContent='Enable phone tilt';return;}
  try {
    if(typeof DeviceOrientationEvent.requestPermission==='function' && await DeviceOrientationEvent.requestPermission()!=='granted'){ $('#sensor-status').textContent='Tilt not enabled. Scroll still changes the atmosphere.';return; }
    addEventListener('deviceorientation',onOrientation,{passive:true});sensorAttached=true;tiltButton.textContent='Disable phone tilt';$('#sensor-status').textContent='Phone tilt enabled where your browser supplies sensor data.';
  } catch { $('#sensor-status').textContent='Tilt is unavailable. Scroll still changes the atmosphere.'; }
});
function onOrientation(event) { if(event.beta===null||event.gamma===null)return;sensor={x:clamp((event.beta-45)/15,-1,1)*-4,y:clamp(event.gamma/15,-1,1)*4}; }

function moveRail(direction){const step=rail.querySelector('.screen-card').offsetWidth+24;railTarget=clamp(rail.scrollLeft+direction*step,0,rail.scrollWidth-rail.clientWidth);if(reduced){rail.scrollLeft=railTarget;railTarget=null;}else{rail.dataset.dragging='true';schedule();}}
$('#rail-next').addEventListener('click',()=>moveRail(1));$('#rail-previous').addEventListener('click',()=>moveRail(-1));
rail.addEventListener('keydown',e=>{if(e.key==='ArrowRight'||e.key==='ArrowLeft'){e.preventDefault();moveRail(e.key==='ArrowRight'?1:-1);}});
rail.addEventListener('pointerdown',()=>{railTarget=null;delete rail.dataset.dragging;},{passive:true});
$('#expand-player').addEventListener('click',()=>{if(dialog.open)return;const from=mini.getBoundingClientRect();dialog.showModal();$('#close-player').focus();if(!reduced)morph={from,start:time,to:null};schedule();});
$('#close-player').addEventListener('click',()=>dialog.close());
dialog.addEventListener('close',()=>{morph=null;dialog.style.transform='none';dialog.style.opacity='1';$('#expand-player').focus();});

function drawDust(delta,pulse){
  if(!ctx||reduced||tier>=2||!ambientVisible.size)return;
  ctx.clearRect(0,0,width,height);const count=(width<768?24:60)/(tier===1?2:1);
  for(let i=0;i<count;i++){
    const p=particles[i];p.y-=p.speed*delta/height;if(p.y<-.03)p.y=1.03;
    let x=p.x*width+Math.sin(time/12+i)*8,y=p.y*height;
    const dx=pointer.smoothX-x,dy=pointer.smoothY-y,d=Math.hypot(dx,dy);
    if(finePointer.matches&&d<160){x+=dx*(1-d/160)*.05;y+=dy*(1-d/160)*.05;}
    ctx.globalAlpha=(.35+seeded(i+93)*.35)*(1+pulse*.05);const size=p.size*8;ctx.drawImage(sprite,x-size/2,y-size/2,size,size);
  }
}
function schedule(){if(!raf&&!document.hidden&&!reduced)raf=requestAnimationFrame(frame);}
function frame(stamp){
  raf=0;if(document.hidden||reduced)return;
  const rawDelta=lastStamp===null?0:stamp-lastStamp;lastStamp=stamp;
  const delta=Math.min(rawDelta,64)/1000;time+=rawDelta/1000;beats+=rawDelta/1000*tracks[active].bpm/60;
  // Read phase: cache geometry on resize, then collect the few active pointer rectangles.
  if(resizePending)readMetrics();
  const tiltRect=tiltTarget?.getBoundingClientRect(),magnetRect=magnetTarget?.getBoundingClientRect();
  if(morph&&!morph.to)morph.to=dialog.getBoundingClientRect();
  if(scrollDirty){scrub();scrollDirty=false;}
  // Write phase. Every rhythmic/decorative effect consumes the same time and beat.
  const phase=beatPhase(beats),pulse=(1-Math.cos(phase*Math.PI*2))/2;
  root.style.setProperty('--beat',phase.toFixed(4));root.style.setProperty('--pulse',tier<3?pulse.toFixed(4):'0');root.style.setProperty('--breath',tier<3?(1+pulse*.015).toFixed(5):'1');root.style.setProperty('--rim',`${tier<3?time*4:0}deg`);
  root.style.setProperty('--ambient-pulse',tier<2&&ambientVisible.size?pulse.toFixed(4):'0');
  if(colorTween){const p=clamp((time-colorTween.start)/.75);palette(mix(colorTween.from,colorTween.to,ease(p)));if(p===1)colorTween=null;}
  scrollFollow+=(scrollTarget-scrollFollow)*.1;
  depthLayers.forEach(layer=>{const bob=layer.node.classList.contains('bob')&&tier<3?Math.sin(time/(6+seeded(layer.depth)*4)*Math.PI*2+seeded(layer.depth)*6)*6:0;layer.node.style.setProperty('--dy',`${tier<3?depthOffset(scrollFollow-layer.anchor,layer.depth,width<768)+bob:0}px`);});
  bobbers.filter(node=>!node.classList.contains('depth')).forEach((node,i)=>node.style.transform=tier<3?`translate3d(0,${Math.sin(time/(6+seeded(i)*4)*Math.PI*2+i)*6}px,0)`:'none');
  if(tier<2&&ambientVisible.size)aurora.forEach((node,i)=>node.style.transform=`translate3d(${Math.sin(time/(20+i*4)*Math.PI*2+i)*80}px,${Math.cos(time/(24+i*3)*Math.PI*2+i)*60}px,0)`);
  bars.forEach((bar,i)=>{const energy=seeded(Math.floor(beats)+i*37);bar.style.transform=`scaleY(${tier<3?.2+(.4*energy+.35)*pulse:.4})`;});
  paintCovers();pointer.smoothX+=(pointer.x-pointer.smoothX)*.1;pointer.smoothY+=(pointer.y-pointer.smoothY)*.1;
  pointer.scale+=((pointer.interactive?1.8:1)-pointer.scale)*.1;
  if(tier===0&&finePointer.matches)$('.cursor-orb').style.transform=`translate3d(${pointer.smoothX-12}px,${pointer.smoothY-12}px,0) scale(${pointer.scale})`;
  $$('.tilt').forEach(node=>{let x=0,y=0;if(tier<3&&sensor){x=sensor.x;y=sensor.y;}else if(tier<3&&finePointer.matches&&node===tiltTarget&&tiltRect){x=clamp((pointer.smoothY-tiltRect.top)/tiltRect.height-.5,-.5,.5)*-12;y=clamp((pointer.smoothX-tiltRect.left)/tiltRect.width-.5,-.5,.5)*12;}node.style.setProperty('--tx',`${x}deg`);node.style.setProperty('--ty',`${y}deg`);if(node===tiltTarget&&tiltRect){node.style.setProperty('--sx',`${clamp((pointer.smoothX-tiltRect.left)/tiltRect.width)*100}%`);node.style.setProperty('--sy',`${clamp((pointer.smoothY-tiltRect.top)/tiltRect.height)*100}%`);}});
  $$('.magnetic').forEach(node=>{const engaged=node===magnetTarget&&magnetRect&&tier<3&&finePointer.matches;node.style.setProperty('--mx',`${engaged?clamp((pointer.smoothX-magnetRect.left-magnetRect.width/2)*.08,-8,8):0}px`);node.style.setProperty('--my',`${engaged?clamp((pointer.smoothY-magnetRect.top-magnetRect.height/2)*.08,-8,8):0}px`);});
  reveals.forEach((state,node)=>{const elapsed=time-state.start;node.style.setProperty('--reveal',String(ease(elapsed/.7)));state.words.forEach((word,i)=>word.style.setProperty('--word-progress',String(ease((elapsed-i*.06)/.7))));if(elapsed>.7+state.words.length*.06)reveals.delete(node);});
  if(railTarget!==null){rail.scrollLeft+=(railTarget-rail.scrollLeft)*.12;if(Math.abs(railTarget-rail.scrollLeft)<2){rail.scrollLeft=railTarget;railTarget=null;delete rail.dataset.dragging;}}
  if(morph){const p=ease((time-morph.start)/.65),to=morph.to,from=morph.from;dialog.style.transform=`translate3d(${(from.x+from.width/2-to.x-to.width/2)*(1-p)}px,${(from.y+from.height/2-to.y-to.height/2)*(1-p)}px,0) scale(${from.width/to.width+(1-from.width/to.width)*p},${from.height/to.height+(1-from.height/to.height)*p})`;dialog.style.opacity=String(.5+.5*p);if(p===1){morph=null;dialog.style.transform='none';}}
  drawDust(delta,pulse);
  if(rawDelta>0){samples++;sampleSum+=rawDelta;if((initialSample&&samples>=90)||(!initialSample&&time-sampleStart>=5)){const average=sampleSum/samples;const next=qualityTier(tier,average);root.dataset.fps=String(Math.round(1000/average));if(next!==tier){tier=next;root.dataset.tier=String(tier);if(tier>=2){aurora.forEach(node=>node.style.transform='none');if(ctx)ctx.clearRect(0,0,width,height);}}samples=0;sampleSum=0;sampleStart=time;initialSample=false;}}
  schedule();
}

active=-1;setTrack(0,false,false);palette(tracks[0].palette);readMetrics();paintCovers(false);updateMotion();
