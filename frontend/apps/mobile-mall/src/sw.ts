/**
 * PWA Service Worker — 离线支持与缓存策略
 *
 * 策略总览:
 *   App Shell   → Precache on install (HTML 入口 + 离线页面)
 *   静态资源     → Cache-First (JS / CSS / 字体 — 带哈希，可永久缓存)
 *   API 请求     → Network-First (优先获取最新数据，失败回退缓存)
 *   图片         → Stale-While-Revalidate (即时展示缓存，后台更新)
 *   导航请求     → Network-First，完全离线时回退到离线页面
 *
 * 版本管理: 修改下方的 CACHE_VERSION 即可全局刷新所有缓存。
 */

// ════════════════════════════════════════════════════════════════
// 缓存版本
// ════════════════════════════════════════════════════════════════

/** 修改此值即可全局刷新所有缓存 (Semver 风格) */
const CACHE_VERSION = '1.0.0'

/** 各缓存桶名 — 带版本号以实现缓存版本化 */
const APP_SHELL      = `app-shell-v${CACHE_VERSION}`
const STATIC_ASSETS   = `static-v${CACHE_VERSION}`
const API_RESPONSES   = `api-v${CACHE_VERSION}`
const IMAGES          = `images-v${CACHE_VERSION}`

const VALID_CACHES = new Set([APP_SHELL, STATIC_ASSETS, API_RESPONSES, IMAGES])

// ════════════════════════════════════════════════════════════════
// 预缓存清单 (App Shell)
// ════════════════════════════════════════════════════════════════

/**
 * Install 阶段预缓存的资源。
 * - "/"            → SPA 入口 HTML (后续 JS/CSS 由运行时 Cache-First 覆盖)
 * - "/offline.html" → 离线回退页
 */
const PRECACHE_URLS: string[] = [
  '/',
  '/offline.html',
]

// ════════════════════════════════════════════════════════════════
// Install — 预缓存 App Shell
// ════════════════════════════════════════════════════════════════

self.addEventListener('install', (event) => {
  console.log('[SW] 安装中…')

  ;(event as any).waitUntil(
    caches
      .open(APP_SHELL)
      .then((cache) => {
        console.log(`[SW] 预缓存 App Shell: ${PRECACHE_URLS.join(', ')}`)
        return cache.addAll(PRECACHE_URLS)
      })
      .then(() => {
        console.log('[SW] 预缓存完成 → skipWaiting')
        return (self as any).skipWaiting()
      })
      .catch((err: Error) => {
        console.error('[SW] 预缓存失败:', err.message)
      }),
  )
})

// ════════════════════════════════════════════════════════════════
// Activate — 清理旧版本缓存
// ════════════════════════════════════════════════════════════════

self.addEventListener('activate', (event) => {
  console.log('[SW] 激活中…')

  ;(event as any).waitUntil(
    caches
      .keys()
      .then((cacheNames) =>
        Promise.all(
          cacheNames.map((name) => {
            if (!VALID_CACHES.has(name)) {
              console.log(`[SW] 清理旧缓存: ${name}`)
              return caches.delete(name)
            }
          }),
        ),
      )
      .then(() => {
        console.log('[SW] 激活完成 → clients.claim')
        return (self as any).clients.claim()
      }),
  )
})

// ════════════════════════════════════════════════════════════════
// Fetch — 请求拦截 & 策略分发
// ════════════════════════════════════════════════════════════════

self.addEventListener('fetch', (event) => {
  const request: Request = (event as any).request

  // 只处理 GET 请求
  if (request.method !== 'GET') return

  const url = new URL(request.url)

  // 忽略非 HTTP(S) 请求 (chrome-extension:// 等)
  if (!url.protocol.startsWith('http')) return

  // ── API 请求: Network-First ──
  if (url.pathname.startsWith('/api/')) {
    ;(event as any).respondWith(networkFirst(request, API_RESPONSES))
    return
  }

  // ── 图片: Stale-While-Revalidate ──
  if (
    request.destination === 'image' ||
    /\.(png|jpe?g|gif|svg|webp|ico)(\?.*)?$/.test(url.pathname)
  ) {
    ;(event as any).respondWith(staleWhileRevalidate(request, IMAGES))
    return
  }

  // ── 静态资源 (JS/CSS/字体): Cache-First ──
  if (
    request.destination === 'script' ||
    request.destination === 'style' ||
    request.destination === 'font' ||
    /\.(js|mjs|css|woff2?|ttf|otf)(\?.*)?$/.test(url.pathname)
  ) {
    ;(event as any).respondWith(cacheFirst(request, STATIC_ASSETS))
    return
  }

  // ── 导航请求: Network-First → 离线回退 ──
  if (request.mode === 'navigate') {
    ;(event as any).respondWith(
      networkFirst(request, APP_SHELL).catch(() => caches.match('/offline.html')),
    )
    return
  }

  // ── 默认: Network-First ──
  ;(event as any).respondWith(networkFirst(request, APP_SHELL))
})

// ════════════════════════════════════════════════════════════════
// 消息处理 — 接收客户端控制指令
// ════════════════════════════════════════════════════════════════

self.addEventListener('message', (event) => {
  const data = (event as any).data

  if (data?.type === 'SKIP_WAITING') {
    console.log('[SW] 收到客户端 SKIP_WAITING 指令 → 立即激活新版本')
    ;(self as any).skipWaiting()
  }
})

// ════════════════════════════════════════════════════════════════
// 缓存策略实现
// ════════════════════════════════════════════════════════════════

/**
 * Cache-First (缓存优先)
 *
 * 适用场景: 带哈希的静态资源 (JS / CSS / Fonts)，内容不变，可长期缓存。
 *
 * 流程:
 *   1. 命中缓存 → 直接返回
 *   2. 未命中   → 请求网络，成功后写入缓存
 *   3. 网络失败  → 抛出异常
 */
async function cacheFirst(request: Request, cacheName: string): Promise<Response> {
  const cached = await caches.match(request)
  if (cached) return cached

  const response = await fetch(request)
  if (!response.ok || response.type !== 'basic') {
    return response
  }

  const cache = await caches.open(cacheName)
  cache.put(request, response.clone()).catch(() => {
    /* 缓存写入失败不影响响应 */
  })

  return response
}

/**
 * Network-First (网络优先)
 *
 * 适用场景: API 请求、HTML 导航，需要尽可能新的内容。
 *
 * 流程:
 *   1. 网络成功 → 写入缓存，返回响应
 *   2. 网络失败 → 回退缓存
 *   3. 缓存也无 → 抛出异常 (由调用方 catch 处理)
 */
async function networkFirst(request: Request, cacheName: string): Promise<Response> {
  let response: Response
  try {
    response = await fetch(request)
  } catch {
    // 网络不可用 → 尝试缓存
    const cached = await caches.match(request)
    if (cached) return cached
    throw new Error(`Network unavailable and no cache for: ${request.url}`)
  }

  // 网络响应 ok → 异步写入缓存
  if (response.ok && response.type === 'basic') {
    const cache = await caches.open(cacheName)
    cache.put(request, response.clone()).catch(() => {
      /* 静默失败 */
    })
  }

  return response
}

/**
 * Stale-While-Revalidate (过期时重新验证)
 *
 * 适用场景: 图片等可容忍短暂过期的资源。
 *
 * 流程:
 *   1. 缓存命中 → 立即返回缓存，同时在后台 fetch 更新
 *   2. 缓存未命中 → 等待网络返回
 *   3. 网络失败   → 静默，下次请求再试
 */
async function staleWhileRevalidate(request: Request, cacheName: string): Promise<Response> {
  const cache = await caches.open(cacheName)
  const cached = await cache.match(request)

  // 后台更新 (不阻塞响应)
  const updatePromise = fetch(request)
    .then((response) => {
      if (response.ok && response.type === 'basic') {
        cache.put(request, response.clone()).catch(() => {})
      }
      return response
    })
    .catch(() => {
      /* 静默失败 — 下次再试 */
    })

  // 立即返回缓存 (如果有)，否则等待网络
  if (cached) {
    updatePromise // fire-and-forget
    return cached
  }

  return (await updatePromise) ?? new Response('Offline', { status: 503 })
}
