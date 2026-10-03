# Vibe Arc / Real-app launch page

The October 3 brief supersedes the dark illustrated player website: white paper,
black typography, cobalt controls with black outlines, soft pink highlights,
and authentic Vibe Arc screenshots. The reference informs visual principles,
not branding, mascot, copy, or distribution links.

## Content and scope

Hero → Search → Library/playlists → Appearance → Settings → screenshot rail →
open source → download → footer. Android v1.0.0 has a direct official APK link.
Windows and Linux are clearly disabled, labeled Coming soon. No invented
package distributions, testimonials, plugin support, or playback guarantees.
The Android application and its release artifacts are unchanged. The optional
Buy Me a Chai support button uses the user-provided URL and external image. CSP
permits only that additional image host; the text fallback survives image failure.

Eight user-provided JPEG captures are copied unchanged into `screenshots/`.
They show v0.9 beta, explicitly labeled on the website, rather than pretending
to be the newer APK. Screenshot album artwork belongs to its respective owners.
The capture with an account first name was supplied for this website; no account
credentials are included. Fonts and icon are self-hosted existing brand assets.

## Interaction and accessibility

Image links progressively enhance to a native modal dialog: close button,
Escape, backdrop dismissal, native focus containment, return to the opener.
Without JavaScript, screenshots still open through normal image links and all
downloads/navigation work. The native horizontal gallery supports touch,
keyboard scrolling, and scroll snap. No autoplay, sensor prompts, network API
calls, analytics, storage, runtime dependencies, or build step.

Motion: small hero entrance, once-only section reveals, tactile button and
image hover lift. No continuous rendering, particles, blur, or parallax loops.
The operating-system preference and an in-memory footer toggle disable motion.
All site text uses AA contrast pairs; screenshot pixels remain unaltered.
Images reserve their native 720:1600 ratio and lazy-load below the hero.

## Verification

```powershell
node tools/check-website.mjs
node --check docs/script.js
python -m http.server 4173 --bind 127.0.0.1 --directory docs
```

The current website contract checks assets, alt text/dimensions, honest version
labels, supported platforms, modal/motion hooks, release links, and AA palette.
The older motion engine and diagnostic fixture remain historical source; they
are not imported by this page. Their old illustrated-layout assertions do not
define this replacement brief.

Observed in the Codex browser: no page overflow at 320, 360, 768, 1280, or
1920px; all eight captures load at 720×1600; Chai image loads at 350×85;
gallery arrow-key scrolling, modal close/Escape and focus return work;
the OS reduced-motion preference is honored; console errors/warnings are empty.
Node checks also exercise modal data, focus return, manual/system reduction and
the no-observer fallback. Physical Android, Safari, screen-reader speech, and
Lighthouse/Core Web Vitals scores were not verified. No such results are claimed.

## Deployment

GitHub Pages uses `main` → `/docs`, with `.nojekyll`.
Live URL: https://smithcooks.github.io/VibeArc/
Also compatible with static hosting: no build command; publish directory `docs`.
Rollback by reverting the website commits; APK assets remain independent.
Versioned stylesheet/script URLs prevent returning browsers from mixing the
new HTML with the previous design's cached assets. Bump both asset revisions
when either changes; the website contract guards this deployment boundary.
