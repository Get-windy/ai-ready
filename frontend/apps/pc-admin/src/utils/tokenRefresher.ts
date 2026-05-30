/**
 * Token 无感刷新机制
 *
 * 核心设计：
 * - Promise 队列：并发 401 请求不会同时触发多次 refreshToken 调用
 * - isRefreshing 标志位：阻止并发的刷新请求
 * - failedQueue 数组：暂存刷新期间到达的请求，刷新完成后统一重放
 * - Token 变更事件订阅：供其他模块监听 token 变化
 */
import { useUserStore } from '@/stores/user'

// ── 队列状态 ────────────────────────────────────────────

let isRefreshing = false
let failedQueue: Array<{
  resolve: (token: string) => void
  reject: (error: any) => void
}> = []

/**
 * 处理队列中的所有待处理请求
 * @param error 如果有错误则 reject 所有请求
 * @param token 如果成功则用新 token resolve 所有请求
 */
function processQueue(error: any, token: string | null): void {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) {
      reject(error)
    } else if (token) {
      resolve(token)
    }
  })
  failedQueue = []
}

// ── Token 存取 ──────────────────────────────────────────

export function getToken(): string | null {
  return localStorage.getItem('token')
}

export function setToken(token: string): void {
  localStorage.setItem('token', token)
  // 同步更新 Pinia store
  try {
    const userStore = useUserStore()
    if (userStore) {
      userStore.token = token
    }
  } catch {
    // Pinia store 可能尚未初始化，仅使用 localStorage
  }
  // 通知订阅者 token 已变更
  emitTokenChange(token)
}

export function removeToken(): void {
  localStorage.removeItem('token')
  try {
    const userStore = useUserStore()
    if (userStore) {
      userStore.token = ''
    }
  } catch {
    // Store may not be available
  }
  emitTokenChange(null)
}

// ── Token 刷新 ──────────────────────────────────────────

/**
 * 调用后端 /api/auth/refresh-token 刷新 Token
 * 注意：此函数使用原生 fetch 以避免循环依赖（axios instance 的拦截器依赖本模块）
 */
export async function refreshToken(): Promise<string> {
  const currentToken = getToken()
  if (!currentToken) {
    throw new Error('No token available for refresh')
  }

  const response = await fetch('/api/auth/refresh-token', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${currentToken}`
    }
  })

  if (!response.ok) {
    throw new Error(`Token refresh failed: ${response.status}`)
  }

  const data = await response.json()
  if (data.code === 200 && data.data?.token) {
    const newToken = data.data.token
    setToken(newToken)
    return newToken
  }

  throw new Error('Invalid refresh token response')
}

/**
 * 刷新 Token 并重试失败的请求
 *
 * 这是外部调用的主入口：
 * 1. 如果当前正在刷新，将请求加入队列等待
 * 2. 如果未在刷新，开始刷新流程，并将并发请求加入队列
 * 3. 刷新成功后重放所有队列请求
 * 4. 刷新失败后清除登录状态并跳转
 *
 * @param makeRequest 使用新 token 重试请求的回调
 */
export async function refreshTokenAndRetry(
  makeRequest: (token: string) => Promise<any>
): Promise<any> {
  // 如果正在刷新，将请求加入队列等待
  if (isRefreshing) {
    return new Promise<string>((resolve, reject) => {
      failedQueue.push({ resolve, reject })
    }).then((token) => makeRequest(token))
  }

  isRefreshing = true

  try {
    const newToken = await refreshToken()
    // 重放所有队列中的请求
    processQueue(null, newToken)
    return makeRequest(newToken)
  } catch (error) {
    processQueue(error, null)

    // 刷新失败，清除登录状态
    removeToken()
    try {
      const userStore = useUserStore()
      userStore.logout()
    } catch {
      // Store may not be available
    }

    // 跳转到登录页（避免多次重定向）
    if (window.location.pathname !== '/login') {
      window.location.href = `/login?redirect=${encodeURIComponent(window.location.pathname)}`
    }

    throw error
  } finally {
    isRefreshing = false
  }
}

// ── Token 过期检查 ──────────────────────────────────────

/**
 * 检查 Token 是否已过期（仅检查 exp 字段，不做签名验证）
 * 对于非 JWT 格式的 token（如 mock），假定未过期
 */
export function isTokenExpired(token: string): boolean {
  try {
    // JWT token 格式: header.payload.signature
    const payload = JSON.parse(atob(token.split('.')[1]))
    const exp = payload.exp * 1000 // 转换为毫秒
    return Date.now() >= exp - 60000 // 提前1分钟判定过期
  } catch {
    // 非 JWT 格式的 Token（如 mock token），假定未过期
    return false
  }
}

// ── Token 变更事件订阅 ──────────────────────────────────

type TokenListener = (token: string | null) => void
const tokenListeners: TokenListener[] = []

/**
 * 订阅 Token 变更事件，返回取消订阅的函数
 */
export function onTokenChange(listener: TokenListener): () => void {
  tokenListeners.push(listener)
  return () => {
    const idx = tokenListeners.indexOf(listener)
    if (idx > -1) tokenListeners.splice(idx, 1)
  }
}

function emitTokenChange(token: string | null): void {
  tokenListeners.forEach((fn) => fn(token))
}
