// Source-derived HTML sizing preview, not native Android rendering.
const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');

(async () => {
  const res = path.resolve('app/src/main/res');
  const names = ['player', 'album', 'vinyl', 'console', 'lyrics', 'mini', 'aurora', 'compact'];
  const layouts = Object.fromEntries(names.map(name => [name, fs.readFileSync(path.join(res, `layout/widget_${name}.xml`), 'utf8')]));
  const drawables = Object.fromEntries(fs.readdirSync(path.join(res, 'drawable')).filter(name => name.startsWith('widget_') && name.endsWith('.xml'))
    .map(name => [name.slice(0, -4), fs.readFileSync(path.join(res, 'drawable', name), 'utf8')]));
  const browser = await chromium.launch({ executablePath: 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe', headless: true });
  try {
    const page = await browser.newPage({ viewport: { width: 1440, height: 1200 }, deviceScaleFactor: 1 });
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    await page.route('**/*', route => route.abort());
    await page.setContent('<html><head><style>*{box-sizing:border-box}body{margin:0;background:#101317;color:white;font-family:Arial,sans-serif}h1{font-size:22px;margin:24px 32px 8px}p{margin:0 32px 24px;color:#b9c0c8;font-size:13px}main{display:grid;grid-template-columns:repeat(4,326px);gap:16px;padding:0 32px 32px}.sample{background:radial-gradient(ellipse at 20% 70%,#344d58,transparent 60%),radial-gradient(ellipse at 90% 0%,#453b4c,transparent 70%),#161c24;border-radius:24px;padding:0}.label{font-size:13px;font-weight:bold;padding:16px}.slot{width:326px;height:370px}.node{min-width:0;flex-shrink:0}svg{width:100%;height:100%}</style></head><body><h1>VibeArc widget layout check</h1><p>Source-derived HTML previews · fixture artwork and metadata · not Android launcher screenshots</p><main></main></body></html>');
    const result = await page.evaluate(({ layouts, drawables }) => {
      const ns = 'http://schemas.android.com/apk/res/android';
      const parser = new DOMParser();
      const colour = value => value?.length === 9 ? `rgba(${parseInt(value.slice(3,5),16)},${parseInt(value.slice(5,7),16)},${parseInt(value.slice(7,9),16)},${parseInt(value.slice(1,3),16)/255})` : value || 'transparent';
      const px = value => parseFloat(value) || 0;
      function render(xmlNode, fontScale = 1) {
        const attr = key => xmlNode.getAttributeNS(ns, key) || '';
        const node = document.createElement('div'); node.className = 'node';
        node.dataset.id = attr('id'); node.dataset.tag = xmlNode.tagName;
        for (const dimension of ['width', 'height']) {
          const value = attr(`layout_${dimension}`);
          node.style[dimension] = value === 'match_parent' ? '100%' : value === 'wrap_content' ? 'auto' : `${px(value)}px`;
        }
        if (attr('layout_weight')) { node.style.flex = `${attr('layout_weight')} 1 0`; node.style.width = 'auto'; }
        node.style.minHeight = `${px(attr('minHeight'))}px`;
        node.style.padding = `${px(attr('padding'))}px`;
        for (const [key, css] of [['Start','Left'],['End','Right'],['Top','Top'],['Bottom','Bottom']]) node.style[`margin${css}`] = `${px(attr(`layout_margin${key}`))}px`;
        if (attr('layout_gravity').includes('center')) node.style.alignSelf = 'center';
        if (xmlNode.tagName === 'LinearLayout') {
          node.style.display = 'flex'; node.style.flexDirection = attr('orientation') === 'horizontal' ? 'row' : 'column';
          if (attr('gravity').includes('center')) { node.style.alignItems = 'center'; node.style.justifyContent = 'center'; }
        } else if (xmlNode.tagName === 'FrameLayout') { node.style.display = 'flex'; node.style.flexDirection = 'column'; node.style.alignItems = 'center'; node.style.justifyContent = 'center'; }
        const background = attr('background').replace('@drawable/', '');
        if (drawables[background]) {
          const resource = parser.parseFromString(drawables[background], 'text/xml');
          const a = (el, key) => el?.getAttributeNS(ns,key);
          const gradient = resource.querySelector('gradient'), solid = resource.querySelector('solid'), stroke = resource.querySelector('stroke');
          if (gradient) node.style.background = `linear-gradient(135deg,${colour(a(gradient,'startColor'))},${colour(a(gradient,'endColor'))})`;
          else if (solid) node.style.backgroundColor = colour(a(solid,'color'));
          if (stroke) node.style.border = `${px(a(stroke,'width'))}px solid ${colour(a(stroke,'color'))}`;
          node.style.borderRadius = background.includes('round') || background === 'widget_disc' ? '50%' : `${px(a(resource.querySelector('corners'),'radius'))}px`;
          if (background.startsWith('widget_glass')) node.style.boxShadow = 'inset 0 1px rgba(255,255,255,.15)';
          if (background === 'widget_disc') node.style.background = 'repeating-radial-gradient(circle,#252629 0px,#252629 9px,#505155 10px,#252629 11px)';
        }
        if (xmlNode.tagName === 'TextView') {
          const fixture = { '@+id/widget_title': 'Midnight Drive', '@+id/widget_artist': 'Vibe Arc', '@+id/widget_lyric': 'A little light to carry through the night' };
          node.textContent = fixture[attr('id')] || '';
          node.style.fontSize = `${px(attr('textSize')) * fontScale}px`; node.style.lineHeight = '1.18';
          node.style.fontWeight = attr('textStyle') === 'bold' ? '700' : '400'; node.style.color = colour(attr('textColor'));
          node.style.display = '-webkit-box'; node.style.webkitBoxOrient = 'vertical'; node.style.webkitLineClamp = attr('maxLines') || '1'; node.style.overflow = 'hidden';
        } else if (xmlNode.tagName === 'ImageView') {
          node.style.background = 'radial-gradient(ellipse at 70% 20%,#8cb8b1,transparent 50%),linear-gradient(145deg,#253947,#152028 55%,#466362)';
          node.style.overflow = 'hidden';
        } else if (xmlNode.tagName === 'ImageButton') {
          const vector = parser.parseFromString(drawables[attr('src').replace('@drawable/','')], 'text/xml');
          const svg = document.createElementNS('http://www.w3.org/2000/svg','svg'); svg.setAttribute('viewBox','0 0 24 24');
          for (const source of vector.querySelectorAll('path')) { const shape = document.createElementNS(svg.namespaceURI,'path'); shape.setAttribute('d',source.getAttributeNS(ns,'pathData')); shape.setAttribute('fill',colour(attr('tint'))); svg.append(shape); } node.append(svg);
        } else if (xmlNode.tagName === 'ProgressBar') {
          node.style.background = '#ffffff30'; node.style.borderRadius = '99px';
          const fill = document.createElement('div'); fill.style.cssText = 'width:56%;height:100%;background:white;border-radius:99px'; node.append(fill);
        }
        for (const child of xmlNode.children) node.append(render(child, fontScale));
        return node;
      }
      const minimums = { player:[220,160], album:[220,320], vinyl:[196,288], console:[252,192], lyrics:[252,192], mini:[252,120], aurora:[252,160], compact:[196,112] };
      let checks = 0; const failures = [];
      for (const [name, xml] of Object.entries(layouts)) {
        const documentXml = parser.parseFromString(xml,'text/xml');
        const sample = document.createElement('section'); sample.className='sample';
        const label = document.createElement('div'); label.className='label'; label.textContent=name; sample.append(label);
        const slot = document.createElement('div'); slot.className='slot'; slot.append(render(documentXml.documentElement)); sample.append(slot); document.querySelector('main').append(sample);
        for (const font of [1,1.3]) for (const width of [minimums[name][0],326]) for (const height of [minimums[name][1],520]) {
          const test = document.createElement('div'); test.style.cssText=`position:absolute;left:-3000px;width:${width}px;height:${height}px`; test.append(render(documentXml.documentElement,font)); document.body.append(test);
          const surface = test.querySelector('[data-id="@android:id/background"]').getBoundingClientRect();
          const host = test.getBoundingClientRect();
          if(surface.height > host.height + 1) failures.push(`${name}: panel exceeds host at ${width}x${height} font ${font}`);
          for(const control of test.querySelectorAll('[data-tag="ImageButton"]')) { const box=control.getBoundingClientRect(); if(box.left < surface.left-1 || box.right > surface.right+1 || box.top < surface.top-1 || box.bottom > surface.bottom+1) failures.push(`${name}: clipped control at ${width}x${height} font ${font}`); }
          checks++; test.remove();
        }
      }
      return { checks, failures };
    }, { layouts, drawables });
    const output = path.resolve('build/widget-layout-preview.png');
    await page.screenshot({ path: output, fullPage: true });
    console.log(JSON.stringify({ ...result, errors, output }));
    if (result.failures.length || errors.length) process.exitCode = 1;
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
