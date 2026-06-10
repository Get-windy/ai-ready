/**
 * 轻量级 Mock Server — 基于 fetch 拦截器
 *
 * 通过 monkey-patch window.fetch 拦截 API 请求并返回模拟数据。
 * 无需完整 MSW (Mock Service Worker)，减小打包体积。
 * 通过 VITE_USE_MOCKS 或 VITE_FEATURE_MOCK_API 环境变量控制开关。
 */
import { handlers, type HandlerContext, type MockHandlerDefinition } from './handlers'
import { resetUserStore } from './handlers'
import axios from 'axios'
import apiService from '@/utils/request'

// ── 内部状态 ────────────────────────────────────────────

let originalFetch: typeof window.fetch | null = null
let isActive = false
// 保存 Axios 拦截器 ID，以便在 stopMockServer 中移除
let axiosInterceptorId: number | null = null

// ── URL 匹配 ────────────────────────────────────────────

/**
 * 将路径模式与请求 URL 进行匹配，提取路径参数。
 *   /api/user/:id 匹配 /api/user/123 → { id: '123' }
 *   返回 null 表示不匹配。
 */
function matchUrl(url: string, pattern: string): Record<string, string> | null {
  try {
    const urlObj = new URL(url, window.location.origin)
    const path = urlObj.pathname

    const patternSegs = pattern.split('/')
    const pathSegs = path.split('/')

    if (patternSegs.length !== pathSegs.length) return null

    const params: Record<string, string> = {}
    for (let i = 0; i < patternSegs.length; i++) {
      if (patternSegs[i].startsWith(':')) {
        // 动态路径参数
        params[patternSegs[i].slice(1)] = decodeURIComponent(pathSegs[i])
      } else if (patternSegs[i] !== pathSegs[i]) {
        return null
      }
    }
    return params
  } catch {
    return null
  }
}

// ── 查询参数解析 ────────────────────────────────────────

function parseQueryParams(url: string): Record<string, string> {
  const result: Record<string, string> = {}
  try {
    const urlObj = new URL(url, window.location.origin)
    urlObj.searchParams.forEach((value, key) => {
      result[key] = value
    })
  } catch {
    // 忽略解析错误
  }
  return result
}

// ── 请求体解析 ──────────────────────────────────────────

async function parseBody(init?: RequestInit): Promise<any> {
  if (!init?.body) return undefined
  if (typeof init.body === 'string') {
    try {
      return JSON.parse(init.body)
    } catch {
      return init.body
    }
  }
  // FormData / Blob / ReadableStream — 暂不处理
  return undefined
}

// ── 响应构造 ────────────────────────────────────────────

/** ApiResponse 封套结构 */
interface ApiResponseEnvelope<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

function buildResponse(status: number, envelope: ApiResponseEnvelope): Response {
  return new Response(JSON.stringify(envelope), {
    status,
    headers: {
      'Content-Type': 'application/json; charset=utf-8',
      'X-Mock-Response': 'true'
    }
  })
}

// ── 请求处理 ────────────────────────────────────────────

async function handleMockRequest(
  url: string,
  init?: RequestInit
): Promise<Response | null> {
  const method = (init?.method || 'GET').toUpperCase()

  for (const handlerDef of handlers) {
    const params = matchUrl(url, handlerDef.path)
    if (!params) continue
    if (handlerDef.method !== method) continue

    // 构建处理上下文
    const query = parseQueryParams(url)
    const body = await parseBody(init)
    const ctx: HandlerContext = { params, body, query }

    try {
      const data = await handlerDef.handler(ctx)
      return buildResponse(200, {
        code: 200,
        message: 'success',
        data,
        timestamp: Date.now()
      })
    } catch (error: any) {
      // 模拟的业务错误（如 mockError 抛出的）
      const code = error?.mockCode || 500
      const message = error?.mockMessage || error?.message || '服务器错误'
      return buildResponse(code >= 100 && code < 600 ? code : 500, {
        code,
        message,
        data: null,
        timestamp: Date.now()
      })
    }
  }

  // 不匹配任何 mock handler，返回 null 表示放行到真实请求
  return null
}

// ── fetch 拦截器 ────────────────────────────────────────

function createMockFetch(original: typeof window.fetch): typeof window.fetch {
  return async function mockFetch(
    input: RequestInfo | URL,
    init?: RequestInit
  ): Promise<Response> {
    // 提取 URL 字符串
    const urlStr = typeof input === 'string'
      ? input
      : input instanceof URL
        ? input.href
        : input.url

    // 尝试匹配 mock 处理程序
    const mockResponse = await handleMockRequest(urlStr, init)
    if (mockResponse) {
      return mockResponse
    }

    // 放行到原始 fetch
    return original(input, init)
  }
}

// ── 公共 API ────────────────────────────────────────────

/**
 * 初始化 Mock Server。
 * 在应用启动时调用（通常在 main.ts 中，路由初始化之前）。
 *
 * 同时拦截 window.fetch（直接 fetch 调用）和 Axios 请求（项目实际使用的 HTTP 客户端）。
 *
 * 通过以下环境变量控制是否启用（优先级从高到低）：
 *   VITE_USE_MOCKS    — 如果为 'true' 则启用
 *   VITE_FEATURE_MOCK_API — 备选开关
 */
export function initMockServer(): void {
  // 检查环境变量
  const useMocks =
    import.meta.env.VITE_USE_MOCKS === 'true' ||
    import.meta.env.VITE_FEATURE_MOCK_API === 'true'

  if (!useMocks) {
    if (import.meta.env.DEV) {
      console.warn('[MockServer] 未启用 (VITE_USE_MOCKS / VITE_FEATURE_MOCK_API 不为 true)')
    }
    return
  }

  if (isActive) {
    console.warn('[MockServer] 已经激活，跳过重复初始化')
    return
  }

  // 保存原始 fetch
  originalFetch = window.fetch

  // 安装拦截器
  window.fetch = createMockFetch(originalFetch)

  // 重置用户数据到初始状态
  resetUserStore()

  isActive = true
  console.warn(
    `[MockServer] 已激活 — ${handlers.length} 个处理程序已注册 (${handlers.map((h) => `${h.method} ${h.path}`).join(', ')})`
  )

  // ── 设置 Axios 拦截器 ──────────────────────────────────
  // 项目实际使用 Axios（基于 XMLHttpRequest），需要单独拦截。
  // 通过 request interceptor 注入自定义 adapter 来短路匹配的请求。
  setupAxiosMock()
}

/**
 * 为 Axios 添加 Mock 拦截器。
 */
function setupAxiosMock() {
  try {
    // 使用 Axios 实例（@/utils/request 默认导出）
    const service = apiService as import('axios').AxiosInstance

    // 保存原始 adapter
    const originalAdapter = service.defaults.adapter

    // 添加请求拦截器：当请求匹配 mock handler 时，注入自定义 adapter
    axiosInterceptorId = service.interceptors.request.use(async (config) => {
      const url = config.url || ''
      const method = (config.method || 'get').toUpperCase()
      const fullUrl = (config.baseURL || '') + url

      // 尝试匹配 mock handler
      for (const handlerDef of handlers) {
        const params = matchUrl(fullUrl, handlerDef.path)
        if (!params) continue
        if (handlerDef.method !== method) continue

        // 匹配成功，注入自定义 adapter 返回 mock 数据
        const query = config.params || {}
        const body = config.data
        let parsedBody = body
        if (typeof body === 'string') {
          try { parsedBody = JSON.parse(body) } catch { /* keep original */ }
        }
        const ctx: HandlerContext = { params, body: parsedBody, query }

        config.adapter = async (adapterConfig) => {
          try {
            const data = await handlerDef.handler(ctx)
            return {
              data: {
                code: 200,
                message: 'success',
                data,
                timestamp: Date.now()
              },
              status: 200,
              statusText: 'OK',
              headers: { 'Content-Type': 'application/json', 'X-Mock-Response': 'true' },
              config: adapterConfig,
              request: {}
            }
          } catch (error: any) {
            const mockCode = error?.mockCode || 500
            const mockMsg = error?.mockMessage || error?.message || '服务器错误'
            throw {
              response: {
                data: {
                  code: mockCode,
                  message: mockMsg,
                  data: null,
                  timestamp: Date.now()
                },
                status: mockCode >= 100 && mockCode < 600 ? mockCode : 500,
                statusText: 'Mock Error',
                headers: { 'Content-Type': 'application/json', 'X-Mock-Response': 'true' },
                config: adapterConfig,
                request: {}
              }
            }
          }
        }
        break
      }

      return config
    })

    console.warn(`[MockServer] Axios 拦截器已安装 (id=${axiosInterceptorId})`)
  } catch (e) {
    console.warn('[MockServer] 无法安装 Axios 拦截器，仅支持 fetch:', e)
  }
}

/**
 * 停止 Mock Server 并恢复原始 fetch，移除 Axios 拦截器。
 * 用于测试或开发环境中的热重载清理。
 */
export function stopMockServer(): void {
  if (!isActive || !originalFetch) return
  window.fetch = originalFetch
  originalFetch = null
  isActive = false

  // 移除 Axios 拦截器
  if (axiosInterceptorId !== null) {
    try {
      const service = apiService as import('axios').AxiosInstance
      service.interceptors.request.eject(axiosInterceptorId)
    } catch { /* ignore */ }
    axiosInterceptorId = null
  }

  console.warn('[MockServer] 已停止，已恢复原始 fetch 和 Axios 拦截器')
}

/**
 * 查询 Mock Server 当前是否处于激活状态。
 */
export function isMockActive(): boolean {
  return isActive
}
