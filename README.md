# bujjet

A local-first, payday-based budget app for people paid on the 15th and 30th.

**Live:** https://hey-emji.github.io/budget/

## Layout
| Path | What it is |
|---|---|
| `index.html` | **The app.** One file: markup, styles and script. All changes go here. |
| `sw.js` | Service worker (offline cache). Bump `CACHE` on every release so phones pick up the update. |
| `manifest.json`, `icon-*.png` | Install-to-home-screen metadata and icons |
| `privacy.html`, `terms.html` | Privacy notice and terms of use |
| `app/` | Old preview address. Redirects to the main URL and removes its old cache. |
| `legacy/` | Emji's original Google-Sheet-connected tracker, kept for reference only. Not maintained. |

## Data
Everything is stored on the device in IndexedDB (database `bujjet-app`, key `state`), with a localStorage fallback (`bujjet.state`). Nothing is sent to a server. Users move data between devices with **Budget → Backup & data → Export / Import** (`.bujjet` file, optional AES-256-GCM password).

## Releasing a change
1. Edit `index.html`.
2. Bump `CACHE` in `sw.js` (e.g. `bujjet-v1` → `bujjet-v2`).
3. Commit and push to `main`. GitHub Pages deploys in a minute or two.
