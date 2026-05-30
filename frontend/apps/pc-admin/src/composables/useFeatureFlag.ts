/**
 * Feature Flag Composable
 *
 * 提供响应式的 Feature Flag 访问接口，支持：
 * - 单个 Flag 查询 → computed boolean
 * - 全部 Flag 查询 → reactive refs
 * - 当前用户上下文 → reactive context
 * - URL 参数覆盖 → ?ff_KEY=true/false（开发环境）
 */
import { computed, ref, watch, type ComputedRef, type Ref } from 'vue'
import { useUserStore } from '@/stores/user'
import {
  getFeatureFlagService,
  type FeatureFlagContext,
} from '@/utils/featureFlags'

// ── URL Override ────────────────────────────────────────

/**
 * 从 URL 查询参数中读取 Feature Flag 覆盖值
 * 用法: ?ff_EXPORT_FEATURE=true 或 ?ff_NEW_DASHBOARD=false
 */
function getUrlOverrides(): Record<string, boolean> {
  const overrides: Record<string, boolean> = {}
  if (import.meta.env.SSR) return overrides

  try {
    const params = new URLSearchParams(window.location.search)
    for (const [key, value] of params.entries()) {
      if (key.startsWith('ff_')) {
        const flagKey = key.slice(3) // 去掉 'ff_' 前缀
        if (value === 'true') overrides[flagKey] = true
        else if (value === 'false') overrides[flagKey] = false
      }
    }
  } catch { /* ignore */ }

  return overrides
}

/** 当前会话的 URL 覆盖缓存 */
const _urlOverrides: Record<string, boolean> = import.meta.env.DEV
  ? getUrlOverrides()
  : {}

/**
 * 检查是否有 URL 参数覆盖（仅开发环境生效）
 */
export function getDevOverride(key: string): boolean | null {
  if (!import.meta.env.DEV) return null
  if (key in _urlOverrides) return _urlOverrides[key]
  return null
}

// ── Context ─────────────────────────────────────────────

/** 全局共享的 Feature Flag 上下文（响应式） */
const _context: Ref<FeatureFlagContext> = ref({})

/**
 * 自动同步用户状态到 Feature Flag 上下文
 */
let _contextInitialized = false

function ensureContext(): Ref<FeatureFlagContext> {
  if (!_contextInitialized) {
    _contextInitialized = true
    try {
      const userStore = useUserStore()
      const sync = () => {
        _context.value = {
          userId: userStore.userId || undefined,
          roles: userStore.roles?.length ? [...userStore.roles] : undefined,
          tenantId: userStore.tenantId || undefined,
        }
      }
      sync()
      // 监听用户状态变化
      watch(
        () => [userStore.userId, userStore.roles, userStore.tenantId],
        () => sync(),
        { deep: true }
      )
    } catch {
      // userStore 尚不可用（如 app 未初始化）
    }
  }
  return _context
}

// ── Composables ─────────────────────────────────────────

/**
 * 查询单个 Feature Flag 是否启用
 *
 * @param key - Feature Flag 键名
 * @returns 响应式的布尔值 computed
 *
 * @example
 * ```ts
 * const showExport = useFeatureFlag('EXPORT_FEATURE')
 * // <a-button v-if="showExport">导出</a-button>
 * ```
 */
export function useFeatureFlag(key: string): ComputedRef<boolean> {
  const service = getFeatureFlagService()
  const context = ensureContext()

  return computed(() => {
    // 开发环境优先检查 URL 覆盖
    const devOverride = getDevOverride(key)
    if (devOverride !== null) return devOverride
    return service.isEnabled(key, context.value)
  })
}

/**
 * 查询所有 Feature Flags 的启用状态
 *
 * @returns 以 flag key 为键的响应式对象，每个值为 computed boolean
 *
 * @example
 * ```ts
 * const flags = useFeatureFlags()
 * // flags.NEW_DASHBOARD.value → true/false
 * ```
 */
export function useFeatureFlags(): Record<string, ComputedRef<boolean>> {
  const service = getFeatureFlagService()
  const context = ensureContext()
  const flagMap = service.getAllFlags()

  const result: Record<string, ComputedRef<boolean>> = {}
  for (const key of Object.keys(flagMap)) {
    result[key] = computed(() => {
      const devOverride = getDevOverride(key)
      if (devOverride !== null) return devOverride
      return service.isEnabled(key, context.value)
    })
  }

  return result
}

/**
 * 获取当前用户的 Feature Flag 上下文（响应式）
 *
 * @returns reactive 上下文对象，包含 userId, roles, tenantId
 *
 * @example
 * ```ts
 * const ctx = useFeatureFlagContext()
 * // ctx.userId → 当前用户 ID
 * // ctx.roles  → 当前用户角色列表
 * ```
 */
export function useFeatureFlagContext(): Ref<FeatureFlagContext> {
  return ensureContext()
}
