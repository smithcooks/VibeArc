# Vibe Arc / Now-playing launch website

## Design contract

The page is the player: one visual track controls every section's atmosphere.
Obsidian #07070A, champagne #D9B77E, Manrope 400/600 and Space Grotesk 600/700.
Five original CSS covers: Champagne, Vermilion, Forest, Cobalt, Pearl.
Cover selection changes artwork, readable accents and tempo (96/108/84/128/92 BPM).
The illustration is a visual session, not a web audio player or actual screenshot.

Hero → pinned color demo → six varied feature tiles → editorial philosophy →
ten-screen rail → wordmark/download → footer. No stock images, external images,
analytics, provider requests, dependencies or build step. The two existing fonts
are self-hosted, so even font loading has no third-party network dependency.
Downloads still point at the signed Android v1.0.0 release; Windows is Coming soon.
The Android app, APK, signing and account integrations are not modified here.

## Single-clock motion contract

One requestAnimationFrame scheduling point advances a shared time and beat.
Color transitions use that clock (750 ms); interpolated accent colors are lifted
to at least 4.7:1 against the conservative brightest glass surface #404046.
CTA text uses obsidian. Grain is static. Beat intensity changes are below 8%.
Headings use a static blurred duplicate crossfade: no animated blur radius.
Reveals use the (0.22,1,0.36,1) curve; words/tiles stagger by 60 ms.
Pointer, cover, rail and scroll smoothing use 0.08–0.12 interpolation.
Parallax is limited to decorative layers: four in the hero, one in the demo,
at 0.04–0.25 depth, capped at 40 px mobile / 120 px desktop.

| Tier | Effects retained | Estimated mid-range Android cost (not measured) |
| --- | --- | --- |
| 0 | 5 drifting aurora layers; 60/24 particles; pointer orb; tilt; bob; beat; reveals; scroll demo | Medium–high, dominated by glass compositing |
| 1 | Half particles, no cursor orb; other effects retained | Medium |
| 2 | No canvas/orb; static aurora; reduced fixed blur; other UI motion retained | Low–medium |
| 3 | Static gradient; no dust, aurora, tilt, bob, breathing, rim rotation or parallax | Low |

First governor sample: 90 frames. Later samples: every 5 shared-clock seconds.
Average frame time above 20 ms steps down one tier. No oscillating auto-upgrade.
Hidden tabs cancel the loop; visibility resume resets the frame sample. An
IntersectionObserver pauses ambient work outside the hero/color-demo regions.
DeviceOrientation is attached only after the user taps Enable phone tilt;
unsupported/denied sensors fall back to scroll. No permission prompt on load.
Reduced motion honors the OS preference and the footer toggle (memory only).
It stops the clock, removes particles/tilt/parallax/bob/orb, and swaps colors
instantly. Native keyboard controls, dialog dismissal and download links remain.

## Verification

```powershell
node tools/check-website.mjs
node --check docs/script.js
python -m http.server 4173 --bind 127.0.0.1 --directory docs
```

Browser checks: 360×800, 768×1024, 1280×900, 1920×1080; no page overflow;
ten screen previews; track/palette metadata; keyboard cover navigation; dialog
open/close/focus return; palette retained outside demo; rail button/arrow navigation;
OS reduced-motion state; console clean after fixes. Covers/phones reserve space,
and optional font loading avoids late font swaps. No lorem ipsum or fake social proof.
Unit checks: all 525 palette interpolation samples have AA accent contrast;
beat bounds, deterministic wave seeds, governor policy, depth caps, local assets,
single clock/no timers/storage/network requests in the production motion engine.

Local diagnostic fixture (not deployed by Pages): serve the repo root on port
4180, then open `/tools/motion-harness.html`. It simulates a normal-motion OS,
60/40 FPS timestamps and hidden-tab events without modifying system settings.
Verified shared beat/aurora variables, 108 BPM track switch, manual reduction,
hidden-tab beat freeze, and governor progression to tier 3. Synthetic timings
are correctness tests, not a GPU benchmark or a measurement of device FPS.

Not verified: Lighthouse 90+/95+ scores, physical Android GPU/battery/thermal
cost, real orientation sensor behavior, touch swipe feel on a physical phone,
screen-reader speech, and cross-browser/Safari compositing. No such results
are claimed. Those need a Lighthouse runner and representative hardware.

## Deployment / rollback

Existing GitHub Pages source: `main` → `/docs`, with `.nojekyll`.
Public URL: https://smithcooks.github.io/VibeArc/
Also deployable on Cloudflare Pages: no build command, output directory `docs`.
Revert the website commit to roll back. APK assets are independent.
Artwork is original; fonts retain the repository's OFL notices and source is GPL-3.0.

API references: [requestAnimationFrame](https://developer.mozilla.org/en-US/docs/Web/API/Window/requestAnimationFrame),
[orientation permission](https://developer.mozilla.org/en-US/docs/Web/API/DeviceOrientationEvent/requestPermission_static).
