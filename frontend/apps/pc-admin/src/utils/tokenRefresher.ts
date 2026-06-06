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

    // 刷新失败，清除登录状态（不调用logout API，避免无限循环）
    removeToken()
    try {
      const userStore = useUserStore()
      // 直接清除状态，不调用API
      userStore.token = ''
      userStore.userId = 0
      userStore.tenantId = 1
      userStore.userInfo = null
      userStore.permissions = []
      userStore.roles = []
      userStore.menus = []
      localStorage.removeItem('token')
      localStorage.removeItem('tenantId')
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

// ── Token 格式判断 ──────────────────────────────────────

/**
 * 判断 Token 是否为 JWT 格式（header.payload.signature）
 */
export function isJWT(token: string): boolean {
  return token.split('.').length === 3
}

// ── Token 过期检查 ──────────────────────────────────────

const T = '[DEBUG:token]'

/**
 * 检查 Token 是否已过期（仅检查 exp 字段，不做签名验证）
 * 对于非 JWT 格式的 token（如 UUID），本地无法判断，需后端验证
 */
export function isTokenExpired(token: string): boolean {
  try {
    // JWT token 格式: header.payload.signature
    const payload = JSON.parse(atob(token.split('.')[1]))
    const exp = payload.exp * 1000 // 转换为毫秒
    const expired = Date.now() >= exp - 60000 // 提前1分钟判定过期
    console.log(`${T} isTokenExpired: exp=${new Date(exp).toISOString()}, expired=${expired}`)
    return expired
  } catch {
    // 非 JWT 格式（如 UUID token）→ 本地无法判断，返回 false
    // 由 verifyToken() 进行后端验证
    console.log(`${T} isTokenExpired: 非JWT格式, 需要后端验证`)
    return false
  }
}

// ── Token 后端验证（生产级前端无法本地验证 UUID token） ──

let lastVerifiedToken: string | null = null
let lastVerifiedTime = 0
const VERIFY_TTL = 5 * 60 * 1000 // 5 分钟缓存

/**
 * 调用后端验证 Token 是否有效
 * 用 /api/auth/user-info 作为验证端点（已有、轻量）
 * 适用场景：非 JWT 格式 token（UUID），前端无法本地判断过期
 */
export async function verifyToken(): Promise<boolean> {
  const token = getToken()
  if (!token) return false

  // 缓存命中：同一 token 且在 TTL 内
  if (token === lastVerifiedToken && Date.now() - lastVerifiedTime < VERIFY_TTL) {
    return true
  }

  try {
    const response = await fetch('/api/auth/check', {
      method: 'GET',
      headers: { 'Authorization': `Bearer ${token}` },
    })

    // 404 等非预期状态 → 端点不可用，无法验证，保守起见视为有效
    if (response.status === 404) {
      console.warn(`${T} verifyToken: 验证端点不存在(404), 跳过验证`)
      return true
    }

    if (!response.ok) {
      console.warn(`${T} verifyToken: 后端拒绝, status=${response.status}`)
      lastVerifiedToken = null
      lastVerifiedTime = 0
      return false
    }

    // 解析响应：/api/auth/check 返回 {code:200, data:{valid:true/false, userId, tokenTimeout}}
    const body = await response.json()
    const valid = body?.data?.valid === true
    if (valid) {
      lastVerifiedToken = token
      lastVerifiedTime = Date.now()
    } else {
      console.warn(`${T} verifyToken: token 无效 (valid=false)`)
      lastVerifiedToken = null
      lastVerifiedTime = 0
    }
    return valid
  } catch (err) {
    console.warn(`${T} verifyToken: 请求失败`, err)
    lastVerifiedToken = null
    lastVerifiedTime = 0
    return false
  }
}

/**
 * 清除 token 验证缓存（在登出时调用）
 */
export function clearTokenVerifyCache(): void {
  lastVerifiedToken = null
  lastVerifiedTime = 0
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
