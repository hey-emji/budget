const CACHE = 'bujjet-v37';
const SHELL = ['./', 'index.html', 'manifest.json', 'icon-192.png', 'icon-512.png', 'privacy.html', 'terms.html'];
self.addEventListener('install', e => { e.waitUntil(caches.open(CACHE).then(c => c.addAll(SHELL))); self.skipWaiting(); });
self.addEventListener('activate', e => { e.waitUntil(caches.keys().then(ks => Promise.all(ks.filter(k => k !== CACHE).map(k => caches.delete(k))))); self.clients.claim(); });
self.addEventListener('fetch', e => {
  const u = new URL(e.request.url);
  if (e.request.method === 'POST' && u.origin === location.origin && u.pathname.endsWith('/share-target')) {
    e.respondWith((async () => {
      try {
        const fd = await e.request.formData(), c = await caches.open('bujjet-share'), base = self.registration.scope;
        const t = [fd.get('title'), fd.get('text'), fd.get('url')].filter(Boolean).join('\n');
        if (t) await c.put(new Request(base + '__share/text-' + Date.now()), new Response(t));
        let i = 0; for (const f of fd.getAll('media')) { if (f && f.size) await c.put(new Request(base + '__share/img-' + Date.now() + '-' + (i++)), new Response(f, { headers: { 'content-type': f.type || 'image/*' } })); }
      } catch (err) {}
      return Response.redirect(new URL('./?shared=1', self.registration.scope).href, 303);
    })());
    return;
  }
  if (e.request.method !== 'GET' || u.origin !== location.origin) return;
  e.respondWith(fetch(e.request).then(r => { const c = r.clone(); caches.open(CACHE).then(x => x.put(e.request, c)); return r; }).catch(() => caches.match(e.request)));
});
