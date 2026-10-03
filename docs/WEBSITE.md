# VibeArc website

## Contract

Publish a responsive, liquid-glass download landing page at
`https://smithcooks.github.io/VibeArc/`. Android downloads the existing signed
v1.0.0 APK; Windows is visibly disabled and marked Coming Soon. This is a
marketing site, not a browser player. The player illustration is labelled as
a visual preview, not an app screenshot. No analytics, login, tracking, or CDN
dependencies. App functionality and signing files stay untouched.

Visual thesis: quiet black, oversized editorial typography, mint light moving
through glass, and a floating player as the focal point. Content: hero/download,
listening features, interactive palette preview, final download/installation FAQ.
Motion: entrance, desktop scroll parallax, and palette transitions. Respect
reduced motion; no scroll animation loop on small/touch screens.

## Implementation and verification

Native HTML/CSS/JavaScript in `docs/`; reuse the app's Manrope/Space Grotesk
fonts and original icon. No React build or animation dependency is needed.
Use semantic HTML, named controls, visible keyboard focus, and CSS custom
properties (for example `color: var(--accent)`).

1. Build the page and a small Node check for downloads, assets, and interactions.
2. Inspect the live page at 320, 768, 1024, and 1440 pixels; test controls,
   reduced-motion policy, loading, and console errors.
3. Commit only website files, publish, select GitHub Pages `main` / `docs`,
   and verify the public URL and APK link before handoff.

Commands from the repository root:

```powershell
node tools/check-website.mjs
python -m http.server 4173 --bind 127.0.0.1 --directory docs
```

Always verify links and scope before publishing. Ask before adding accounts,
tracking, paid services, or dependencies. Never publish secrets or pretend
Windows, universal lyrics, or guaranteed lossless playback is available.

Publishing follows [GitHub's branch-source guide](https://docs.github.com/en/pages/getting-started-with-github-pages/configuring-a-publishing-source-for-your-github-pages-site).
Glass uses [CSS backdrop-filter](https://developer.mozilla.org/en-US/docs/Web/CSS/Reference/Properties/backdrop-filter)
with an opaque fallback; animation respects [prefers-reduced-motion](https://developer.mozilla.org/en-US/docs/Web/CSS/Reference/At-rules/@media/prefers-reduced-motion).

Rollback: revert the website commit(s) on `main`; Pages republishes the prior
files. APK assets and Android source are independent of this deployment.
Fonts retain their existing repository OFL notices; original page artwork is
included under the repository's GPL-3.0 license.
