# Working rules for bujjet

bujjet is one file, `index.html` (inline CSS and JS), served at https://bujjet.com from this repo's root, and wrapped as an Android app (Capacitor, `native/`, built by `.github/workflows/android.yml`). The owner uses it daily with real money data, so mistakes cost him time. Follow these rules every time.

## Before building anything: say what it affects
1. Before changing code, search for **every caller** of each function, variable or data field you will change (`grep -n` the name). Shared helpers (`periodNow`, `periodAt`, `upcomingPaydays`, `balances`, `plan`, `budgetsFor`, `parseMessage`, `addToInbox`, `call`, `apply`, `render*`) are used by several screens.
2. **Tell the owner, without being asked, what the change will affect** — which screens, numbers and stored data — before or together with building it. Example: "This changes when pay periods start. It also affects the Paydays tab, the Today card's countdown and the spending breakdown." If something could change or hide existing data, say so first.
3. Never remove or rename a stored field (in `store.config`, `expenses`, `paydays`, `inbox`) without migrating old data.

## Before pushing: test
4. Run `node tests/run.mjs` and read the result. Do not push if anything fails.
5. For every bug fixed, add a check to `tests/checks.js` that would have caught it. For every new feature, add at least one check for its main behaviour and one for each screen it touches.
6. `tests/fixture.json` is synthetic. Never put the owner's real data, names or account numbers in the repo; it's public.
7. Bump `CACHE` in `sw.js` on every release.

## APK
8. Don't send an APK unless the owner asks. When he asks, confirm the latest Android workflow run succeeded (it runs the tests first) and that the APK contains the latest change.
9. Test builds are signed with `native/signing/test.jks` so updates install in place. Never change it.

## Product rules
- Record only what has actually been paid (auto-debits count on their charge day). Nothing is logged without the owner's review, except auto-charged subscriptions he set up.
- No financial advice; show numbers and the reasoning, plus the disclaimer where advice-like content appears.
- Philippines first: peso, 15th/30th paydays, local banks and e-wallets.
