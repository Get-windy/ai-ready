/**
 * Composables 统一导出入口
 */
export { useSubmitLock } from './useSubmitLock'
export type { SubmitLockOptions } from './useSubmitLock'

export { useOptimisticUpdate } from './useOptimisticUpdate'
export type { OptimisticUpdateOptions } from './useOptimisticUpdate'

export { useFocusTrap, useAnnouncer, useA11yKeyboard } from './useA11y'
export type { A11yKeyboardHandlers } from './useA11y'

export { useResponsive } from './useResponsiveState'

export { useTable } from './useTable'

export { useModulePage } from './useModulePage'

// Vue Query 配置
export {
  defaultQueryOptions,
  defaultMutationOptions,
  getVueQueryClientConfig,
  useDefaultQueryOptions,
  useDefaultMutationOptions,
} from './useQueryConfig'

// Feature Flag
export {
  useFeatureFlag,
  useFeatureFlags,
  useFeatureFlagContext,
  getDevOverride,
} from './useFeatureFlag'

// WebSocket
export { useWebSocket } from './useWebSocket'
export type { WsStatus, UseWebSocketOptions, UseWebSocketReturn } from './useWebSocket'

// 实时通知
export { useNotification } from './useNotification'
export type { NotificationItem } from './useNotification'

// 仪表盘实时指标
export { useDashboardMetrics } from './useDashboardMetrics'
export type { DashboardMetricsState } from './useDashboardMetrics'

// 用户管理 Query Hooks
export {
  userKeys,
  useUsersQuery,
  useUserQuery,
  useCreateUser,
  useUpdateUser,
  useDeleteUser,
  useToggleUserStatus,
} from './useUsersQuery'
