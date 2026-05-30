/**
 * Feature Flags 渐进式发布系统
 * 支持按用户/角色/租户/百分比灰度发布功能
 */
import { reactive, computed } from 'vue'
import { useUserStore } from '@/stores/user'

// ── 类型定义 ────────────────────────────────────────────

export interface FeatureFlag {
  key: string
  name: string
  description: string
  enabled: boolean
  rolloutPercentage?: number   // 0-100, 灰度百分比
  allowedUsers?: number[]       // 白名单用户 ID
  allowedRoles?: string[]       // 白名单角色
  allowedTenants?: number[]     // 白名单租户
}

export interface FeatureFlagContext {
  userId?: number
  roles?: string[]
  tenantId?: number
}

export type FeatureFlagConfig = Record<string, FeatureFlag>

// ── 默认 Feature Flags ──────────────────────────────────

export const defaultFeatureFlags: FeatureFlagConfig = {
  NEW_DASHBOARD: {
    key: 'NEW_DASHBOARD',
    name: '新版仪表盘',
    description: '使用新版仪表盘设计',
    enabled: false,
    rolloutPercentage: 0,
  },
  ADVANCED_SEARCH: {
    key: 'ADVANCED_SEARCH',
    name: '高级搜索',
    description: '高级搜索筛选功能',
    enabled: true,
  },
  BATCH_OPERATIONS: {
    key: 'BATCH_OPERATIONS',
    name: '批量操作',
    description: '批量审批/打印/导出功能',
    enabled: true,
  },
  EXPORT_FEATURE: {
    key: 'EXPORT_FEATURE',
    name: '数据导出',
    description: '导出 Excel/PDF 功能',
    enabled: true,
  },
  KANBAN_VIEW: {
    key: 'KANBAN_VIEW',
    name: '看板视图',
    description: '采购/销售看板视图（灰度中）',
    enabled: false,
  },
  CALENDAR_VIEW: {
    key: 'CALENDAR_VIEW',
    name: '日历视图',
    description: '日历视图展示（内部测试）',
    enabled: false,
  },
}

// ── Feature Flag 服务 ────────────────────────────────────

class FeatureFlagService {
  private flags: FeatureFlagConfig
  private overrides: Record<string, boolean> = {}

  constructor(config: FeatureFlagConfig = {}) {
    this.flags = { ...defaultFeatureFlags, ...config }
    this.loadOverrides()
  }

  private loadOverrides(): void {
    try {
      const stored = localStorage.getItem('ff_overrides')
      if (stored) this.overrides = JSON.parse(stored)
    } catch { /* ignore */ }
  }

  private saveOverrides(): void {
    localStorage.setItem('ff_overrides', JSON.stringify(this.overrides))
  }

  private hashUserId(userId: number | string): number {
    const str = String(userId)
    let hash = 0
    for (let i = 0; i < str.length; i++) {
      const char = str.charCodeAt(i)
      hash = ((hash << 5) - hash) + char
      hash = hash & hash
    }
    return Math.abs(hash % 100)
  }

  /** 通过 URL 参数覆盖 Flag (?ff_KEY=true) */
  private getUrlOverride(key: string): boolean | null {
    try {
      const params = new URLSearchParams(window.location.search)
      const val = params.get(`ff_${key}`)
      if (val === 'true') return true
      if (val === 'false') return false
    } catch { /* SSR or test */ }
    return null
  }

  /**
   * 检查指定 Feature Flag 是否启用
   *
   * 检查优先级：
   * 1. URL 参数覆盖（开发环境）
   * 2. 本地存储覆盖
   * 3. enabled 标志
   * 4. rolloutPercentage 灰度（hash userId 得到一致的 0-100 值）
   * 5. allowedUsers 白名单
   * 6. allowedRoles 白名单
   * 7. allowedTenants 白名单
   */
  isEnabled(key: string, context?: FeatureFlagContext): boolean {
    const flag = this.flags[key]
    if (!flag) return false

    // URL 参数覆盖（开发环境）
    if (import.meta.env.DEV) {
      const urlOverride = this.getUrlOverride(key)
      if (urlOverride !== null) return urlOverride
    }

    // 本地存储覆盖
    if (key in this.overrides) return this.overrides[key]

    // 1. Check enabled flag first
    if (!flag.enabled) return false

    // 无上下文场景，仅依赖 enabled 属性
    if (!context) return flag.enabled

    // 2. Check rolloutPercentage — hash userId to get consistent 0-100 value
    if (flag.rolloutPercentage !== undefined && context.userId) {
      const hash = this.hashUserId(context.userId)
      if (hash < flag.rolloutPercentage) return true
    }

    // 3. Check allowedUsers list
    if (flag.allowedUsers && context.userId) {
      if (flag.allowedUsers.includes(context.userId)) return true
    }

    // 4. Check allowedRoles
    if (flag.allowedRoles && context.roles) {
      if (context.roles.some(r => flag.allowedRoles!.includes(r))) return true
    }

    // 5. Check allowedTenants
    if (flag.allowedTenants && context.tenantId) {
      if (flag.allowedTenants.includes(context.tenantId)) return true
    }

    // 若有白名单配置但用户不在其中
    if (flag.allowedUsers || flag.allowedRoles || flag.allowedTenants) {
      return false
    }

    return flag.enabled
  }

  getFlag(key: string): FeatureFlag | undefined {
    return this.flags[key]
  }

  getAllFlags(): FeatureFlagConfig {
    return { ...this.flags }
  }

  /**
   * 获取当前上下文中所有已启用的 Feature Flags
   */
  getAllEnabled(context?: FeatureFlagContext): FeatureFlag[] {
    const result: FeatureFlag[] = []
    for (const key of Object.keys(this.flags)) {
      if (this.isEnabled(key, context)) {
        result.push({ ...this.flags[key] })
      }
    }
    return result
  }

  setOverride(key: string, value: boolean): void {
    this.overrides[key] = value
    this.saveOverrides()
  }

  removeOverride(key: string): void {
    delete this.overrides[key]
    this.saveOverrides()
  }

  clearOverrides(): void {
    this.overrides = {}
    this.saveOverrides()
  }

  updateFlags(flags: FeatureFlagConfig): void {
    this.flags = { ...this.flags, ...flags }
  }

  getContext(): FeatureFlagContext {
    try {
      const userStore = useUserStore()
      return {
        userId: userStore.userId,
        roles: userStore.roles,
        tenantId: userStore.tenantId,
      }
    } catch {
      return {}
    }
  }
}

// ── 单例 ─────────────────────────────────────────────────

let instance: FeatureFlagService | null = null

export function getFeatureFlagService(): FeatureFlagService {
  if (!instance) {
    instance = new FeatureFlagService()
  }
  return instance
}

export function initFeatureFlags(config?: FeatureFlagConfig): FeatureFlagService {
  instance = new FeatureFlagService(config)
  return instance
}

/**
 * 定义 Feature Flags 配置（initFeatureFlags 的语义化别名）
 */
export function defineFeatureFlags(config: FeatureFlagConfig): FeatureFlagService {
  return initFeatureFlags(config)
}

// ── Composable ───────────────────────────────────────────

export function useFeatureFlag(key: string) {
  const service = getFeatureFlagService()
  const context = service.getContext()
  return computed(() => service.isEnabled(key, context))
}

export function useFeatureFlags() {
  const service = getFeatureFlagService()
  const context = service.getContext()
  const flags = service.getAllFlags()

  const result: Record<string, ReturnType<typeof computed>> = {}
  for (const key of Object.keys(flags)) {
    result[key] = computed(() => service.isEnabled(key, context))
  }

  return result
}

export { getFeatureFlagService as useFeatureFlagService }
