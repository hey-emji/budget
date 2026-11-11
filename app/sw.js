// Retired: bujjet now lives one folder up. Clear this folder's cache and step aside.
self.addEventListener('install', () => self.skipWaiting());
self.addEventListener('activate', e => { e.waitUntil(caches.keys().then(ks => Promise.all(ks.filter(k => k.startsWith('bujjet-app')).map(k => caches.delete(k)))).then(() => self.registration.unregister())); });
