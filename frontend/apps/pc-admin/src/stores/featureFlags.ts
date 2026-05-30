/**
 * Feature Flags Pinia Store
 *
 * 管理 Feature Flag 的远程配置与本地覆盖。
 * - 从 API (GET /api/feature-flags) 拉取服务端配置
 * - 支持本地覆盖并持久化到 localStorage
 * - 提供响应式 getter 用于组件消费
 */
import { defineStore } from 'pinia'
import request, { type ApiResponse } from '@/utils/request'
import type { FeatureFlag, FeatureFlagConfig } from '@/utils/featureFlags'

// ── 类型定义 ────────────────────────────────────────────

/** 服务端返回的 Feature Flag 列表 */
interface FeatureFlagListResponse {
  flags: FeatureFlag[]
}

/** Store State */
interface FeatureFlagState {
  /** 从 API 获取的 flags */
  flags: FeatureFlagConfig
  /** 本地覆盖（key → boolean） */
  overrideFlags: Record<string, boolean>
  /** 是否正在加载 */
  loading: boolean
  /** API 是否已成功调用 */
  fetched: boolean
  /** 错误信息 */
  error: string | null
}

// ── Store 定义 ──────────────────────────────────────────

export const useFeatureFlagStore = defineStore('featureFlags', {
  state: (): FeatureFlagState => ({
    flags: {},
    overrideFlags: {},
    loading: false,
    fetched: false,
    error: null,
  }),

  getters: {
    /**
     * 获取当前上下文中所有已启用的 Feature Flags
     * 注意：此 getter 仅基于 store 中的 flags + overrides 判断 enabled 属性，
     * 不包含 per-user rollout/whitelist 逻辑（那些由 FeatureFlagService.isEnabled 处理）。
     */
    enabledFlags(state): FeatureFlag[] {
      const result: FeatureFlag[] = []
      for (const key of Object.keys(state.flags)) {
        const flag = state.flags[key]
        // 本地覆盖优先
        const overrideVal = state.overrideFlags[key]
        const enabled = overrideVal !== undefined ? overrideVal : flag.enabled
        if (enabled) {
          result.push({ ...flag })
        }
      }
      return result
    },

    /**
     * 判断指定 key 的 Feature Flag 是否启用（仅基于 store 数据）
     */
    isFlagEnabled(state): (key: string) => boolean {
      return (key: string): boolean => {
        const flag = state.flags[key]
        if (!flag) return false
        // 本地覆盖优先
        if (key in state.overrideFlags) return state.overrideFlags[key]
        return flag.enabled
      }
    },
  },

  actions: {
    /**
     * 从服务端拉取 Feature Flag 配置
     * GET /api/feature-flags
     */
    async fetchFlags(): Promise<void> {
      if (this.loading) return

      this.loading = true
      this.error = null

      try {
        const res = await request.get<FeatureFlagListResponse>('/feature-flags')

        if (res.data && res.data.flags) {
          const config: FeatureFlagConfig = {}
          for (const flag of res.data.flags) {
            if (flag.key) {
              config[flag.key] = flag
            }
          }
          this.flags = config
          this.fetched = true
        } else {
          // 服务端返回空数据，保留现有 flags
          this.fetched = true
        }
      } catch (err: any) {
        this.error = err?.message || '获取 Feature Flags 失败'
        console.error('[FeatureFlagStore] fetchFlags 失败:', err)
      } finally {
        this.loading = false
      }
    },

    /**
     * 设置本地覆盖值
     * @param key   - Feature Flag 键名
     * @param value - 强制启用/禁用
     */
    setOverride(key: string, value: boolean): void {
      this.overrideFlags = { ...this.overrideFlags, [key]: value }
    },

    /**
     * 移除单个覆盖
     */
    removeOverride(key: string): void {
      if (key in this.overrideFlags) {
        const next = { ...this.overrideFlags }
        delete next[key]
        this.overrideFlags = next
      }
    },

    /**
     * 清除所有本地覆盖
     */
    clearOverrides(): void {
      this.overrideFlags = {}
    },

    /**
     * 批量更新 flags（由外部 source 同步）
     */
    updateFlags(flags: FeatureFlagConfig): void {
      this.flags = { ...this.flags, ...flags }
    },
  },

  // ── 持久化配置 ────────────────────────────────────────

  persist: {
    key: 'feature-flags-store',
    storage: localStorage,
    // 仅持久化覆盖项，服务端 flags 每次启动时重新拉取
    paths: ['overrideFlags'],
  },
})
