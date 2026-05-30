/**
 * PWA Service Worker
 * 缓存策略: App Shell Precache + Cache-First (静态) + Network-First (API) + SWR (图片)
 * 版本: 修改 CACHE_VERSION 即可全局刷新所有缓存
 */
'use strict';

// ── 缓存版本 ────────────────────────────────────────────────
var CACHE_VERSION = '1.0.0';
var APP_SHELL      = 'app-shell-v' + CACHE_VERSION;
var STATIC_ASSETS   = 'static-v' + CACHE_VERSION;
var API_RESPONSES   = 'api-v' + CACHE_VERSION;
var IMAGES          = 'images-v' + CACHE_VERSION;

var VALID_CACHES = [APP_SHELL, STATIC_ASSETS, API_RESPONSES, IMAGES];

// ── 预缓存资源 (App Shell) ─────────────────────────────────────
var PRECACHE_URLS = ['/', '/offline.html'];

// ── Install: 预缓存 ─────────────────────────────────────────
self.addEventListener('install', function(event) {
  console.log('[SW] 安装中…');
  event.waitUntil(
    caches.open(APP_SHELL).then(function(cache) {
      console.log('[SW] 预缓存 App Shell:', PRECACHE_URLS.join(', '));
      return cache.addAll(PRECACHE_URLS);
    }).then(function() {
      console.log('[SW] 预缓存完成 → skipWaiting');
      return self.skipWaiting();
    }).catch(function(err) {
      console.error('[SW] 预缓存失败:', err);
    })
  );
});

// ── Activate: 清理旧缓存 ───────────────────────────────────
self.addEventListener('activate', function(event) {
  console.log('[SW] 激活中…');
  event.waitUntil(
    caches.keys().then(function(cacheNames) {
      return Promise.all(cacheNames.map(function(name) {
        if (VALID_CACHES.indexOf(name) === -1) {
          console.log('[SW] 清理旧缓存:', name);
          return caches.delete(name);
        }
      }));
    }).then(function() {
      console.log('[SW] 激活完成 → clients.claim');
      return self.clients.claim();
    })
  );
});

// ── Fetch: 策略分发 ────────────────────────────────────────
self.addEventListener('fetch', function(event) {
  var request = event.request;
  if (request.method !== 'GET') return;

  var url = new URL(request.url);
  if (!url.protocol.startsWith('http')) return;

  // API 请求: Network-First
  if (url.pathname.startsWith('/api/')) {
    event.respondWith(networkFirst(request, API_RESPONSES));
    return;
  }

  // 图片: Stale-While-Revalidate
  if (
    request.destination === 'image' ||
    /\.(png|jpe?g|gif|svg|webp|ico)(\?.*)?$/.test(url.pathname)
  ) {
    event.respondWith(staleWhileRevalidate(request, IMAGES));
    return;
  }

  // 静态资源 (JS/CSS/字体): Cache-First
  if (
    request.destination === 'script' ||
    request.destination === 'style' ||
    request.destination === 'font' ||
    /\.(js|mjs|css|woff2?|ttf|otf)(\?.*)?$/.test(url.pathname)
  ) {
    event.respondWith(cacheFirst(request, STATIC_ASSETS));
    return;
  }

  // 导航请求: Network-First → 离线回退
  if (request.mode === 'navigate') {
    event.respondWith(
      networkFirst(request, APP_SHELL).catch(function() {
        return caches.match('/offline.html');
      })
    );
    return;
  }

  // 默认: Network-First
  event.respondWith(networkFirst(request, APP_SHELL));
});

// ── 消息处理: SKIP_WAITING ─────────────────────────────────
self.addEventListener('message', function(event) {
  if (event.data && event.data.type === 'SKIP_WAITING') {
    console.log('[SW] 收到客户端 SKIP_WAITING 指令 → 立即激活');
    self.skipWaiting();
  }
});

// ══════════════════════════════════════════════════════════════
// 缓存策略
// ══════════════════════════════════════════════════════════════

/**
 * Cache-First: 缓存优先
 * 适用于带哈希的静态资源 (JS/CSS/Fonts)
 */
function cacheFirst(request, cacheName) {
  return caches.match(request).then(function(cached) {
    if (cached) return cached;

    return fetch(request).then(function(response) {
      if (!response.ok || response.type !== 'basic') return response;

      return caches.open(cacheName).then(function(cache) {
        cache.put(request, response.clone());
        return response;
      });
    });
  });
}

/**
 * Network-First: 网络优先
 * 适用于 API 请求和导航
 */
function networkFirst(request, cacheName) {
  return fetch(request).then(function(response) {
    if (response.ok && response.type === 'basic') {
      return caches.open(cacheName).then(function(cache) {
        cache.put(request, response.clone());
        return response;
      });
    }
    return response;
  }).catch(function() {
    return caches.match(request);
  });
}

/**
 * Stale-While-Revalidate: 过期重验证
 * 适用于图片 — 即时返回缓存，后台更新
 */
function staleWhileRevalidate(request, cacheName) {
  return caches.open(cacheName).then(function(cache) {
    return cache.match(request).then(function(cached) {
      var networkFetch = fetch(request).then(function(response) {
        if (response.ok && response.type === 'basic') {
          cache.put(request, response.clone());
        }
        return response;
      }).catch(function() {
        /* 静默失败，下次再试 */
      });

      return cached || networkFetch;
    });
  });
}
