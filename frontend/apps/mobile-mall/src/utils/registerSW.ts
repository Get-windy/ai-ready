/**
 * PWA Service Worker 注册工具
 *
 * 功能:
 *   - 生产环境自动注册 Service Worker
 *   - 开发环境跳过 (利用 Vite HMR)
 *   - 监听 SW 更新，显示"新版本可用"通知栏
 *   - 用户点击"刷新"后发送 SKIP_WAITING 并重载页面
 *   - 完整控制台生命周期日志
 */

/**
 * 注册 Service Worker (生产环境调用)
 *
 * @example
 *   // src/main.ts
 *   import { registerSW } from './utils/registerSW'
 *   registerSW()
 */
export function registerSW(): void {
  // 浏览器不支持 Service Worker — 静默退出
  if (!('serviceWorker' in navigator)) {
    console.log('[SW] 浏览器不支持 Service Worker')
    return
  }

  // 开发环境跳过 — 避免缓存干扰 HMR
  if (import.meta.env.DEV) {
    console.log('[SW] 开发模式，跳过 Service Worker 注册')
    return
  }

  // ── 全局刷新标记 ─────────────────────────────────────────
  // 防止 controllerchange → reload 死循环
  let refreshing = false

  navigator.serviceWorker.addEventListener('controllerchange', () => {
    if (refreshing) return
    refreshing = true
    console.log('[SW] 控制器已切换，自动刷新页面')
    window.location.reload()
  })

  // ── 注册 SW ─────────────────────────────────────────────
  window.addEventListener('load', () => {
    navigator.serviceWorker
      .register('/sw.js')
      .then((registration) => {
        console.log('[SW] 注册成功')
        console.log(`[SW]   作用域: ${registration.scope}`)
        console.log(`[SW]   ServiceWorker 状态: ${stateLabel(registration.active)}`)
        logWorkerState(registration.active)

        // 已有 waiting worker — 说明之前下载了新版本但未激活
        if (registration.waiting) {
          console.log('[SW] 检测到待激活的新版本')
          showUpdateNotification(registration)
        }

        // ── 监听更新 ──
        registration.addEventListener('updatefound', () => {
          console.log('[SW] 发现新版本，正在下载…')

          const newWorker = registration.installing
          if (!newWorker) {
            console.log('[SW] (无 installing worker)')
            return
          }

          // 追踪新 worker 状态
          newWorker.addEventListener('statechange', () => {
            console.log(`[SW] 新 Worker 状态: ${stateLabel(newWorker)}`)

            if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
              // 有旧 SW 在运行 → 新 SW 等待用户操作
              console.log('[SW] 新版本已就绪，等待用户点击"刷新"')
              showUpdateNotification(registration)
            }
          })
        })
      })
      .catch((error) => {
        console.error('[SW] 注册失败:', error)
      })
  })

  // ── 监听 SW 主动推送的消息 ───────────────────────────────
  navigator.serviceWorker.addEventListener('message', (event) => {
    if (event.data?.type === 'UPDATE_AVAILABLE') {
      console.log('[SW] 收到 UPDATE_AVAILABLE 消息')
    }
  })
}

// ════════════════════════════════════════════════════════════════
// 辅助函数
// ════════════════════════════════════════════════════════════════

/**
 * 显示"新版本可用"通知栏
 *
 * 在页面顶部显示一个蓝色通知条，包含"刷新"按钮。
 * 样式为内联注入，不依赖任何 UI 框架。
 */
function showUpdateNotification(registration: ServiceWorkerRegistration): void {
  // 防止重复创建
  if (document.getElementById('sw-update-toast')) return

  const toast = document.createElement('div')
  toast.id = 'sw-update-toast'

  // 内联样式 — mobile-first
  Object.assign(toast.style, {
    position: 'fixed',
    top: '0',
    left: '0',
    right: '0',
    zIndex: '99999',
    background: '#1988fa',
    color: '#fff',
    padding: '12px 16px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    fontFamily:
      '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", sans-serif',
    fontSize: '14px',
    boxShadow: '0 2px 12px rgba(0, 0, 0, 0.18)',
    transition: 'transform 0.3s ease',
    transform: 'translateY(-100%)',
    // 适配 iPhone 刘海屏
    paddingTop: 'env(safe-area-inset-top, 12px)',
  })

  toast.innerHTML = `
    <span style="flex:1; min-width:0; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">
      发现新版本，推荐更新以获得最佳体验
    </span>
    <button id="sw-update-btn"></button>
  `

  document.body.appendChild(toast)

  // 按钮单独设置样式以支持 :active 伪类
  const btn = document.getElementById('sw-update-btn')!
  Object.assign(btn.style, {
    background: 'rgba(255, 255, 255, 0.2)',
    color: '#fff',
    border: '1px solid rgba(255, 255, 255, 0.35)',
    padding: '6px 18px',
    borderRadius: '4px',
    fontSize: '14px',
    cursor: 'pointer',
    whiteSpace: 'nowrap',
    marginLeft: '12px',
    flexShrink: '0',
    transition: 'background 0.15s',
  })
  btn.textContent = '刷新'

  // ── hover / active ──
  btn.addEventListener('pointerenter', () => {
    btn.style.background = 'rgba(255, 255, 255, 0.3)'
  })
  btn.addEventListener('pointerleave', () => {
    btn.style.background = 'rgba(255, 255, 255, 0.2)'
  })
  btn.addEventListener('pointerdown', () => {
    btn.style.background = 'rgba(255, 255, 255, 0.15)'
  })

  // ── 入场动画 ──
  requestAnimationFrame(() => {
    requestAnimationFrame(() => {
      toast.style.transform = 'translateY(0)'
    })
  })

  // ── 点击"刷新" → 发送 SKIP_WAITING → 刷新页面 ──
  btn.addEventListener('click', () => {
    applyUpdate(registration)
  })
}

/**
 * 应用更新
 *
 * 1. 向 waiting Service Worker 发送 SKIP_WAITING 消息
 * 2. 监听其激活，激活后刷新页面
 * 3. 3 秒兜底强制刷新
 */
function applyUpdate(registration: ServiceWorkerRegistration): void {
  const waitingWorker = registration.waiting
  if (!waitingWorker) {
    console.log('[SW] 无 waiting worker，直接刷新')
    window.location.reload()
    return
  }

  console.log('[SW] 用户点击"刷新" → 发送 SKIP_WAITING 指令')

  // 发送消息
  waitingWorker.postMessage({ type: 'SKIP_WAITING' })

  // 监听激活
  waitingWorker.addEventListener('statechange', function onStateChange() {
    if (waitingWorker.state === 'activated') {
      console.log('[SW] 新版本已激活 → 刷新页面')
      window.location.reload()
    }
  })

  // 兜底: 3 秒后无论如何刷新
  setTimeout(() => {
    console.log('[SW] 兜底超时 → 强制刷新')
    window.location.reload()
  }, 3000)
}

/**
 * 输出 Worker 当前状态
 */
function logWorkerState(worker: ServiceWorker | null | undefined): void {
  if (!worker) return
  console.log(`[SW] Worker 状态: ${stateLabel(worker)}`)
}

/**
 * 将 SW 状态码转为可读中文标签
 */
function stateLabel(worker: ServiceWorker | null | undefined): string {
  if (!worker) return 'none'
  const map: Record<string, string> = {
    installing:  '下载中',
    installed:   '已下载，等待激活',
    activating:  '激活中',
    activated:   '已激活',
    redundant:   '已废弃',
  }
  return map[worker.state] || worker.state
}
