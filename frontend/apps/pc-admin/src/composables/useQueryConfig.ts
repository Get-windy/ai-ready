/**
 * Vue Query 默认配置
 * 提供统一的 staleTime、gcTime、重试策略
 *
 * 安装: pnpm add @tanstack/vue-query
 */
import type { DefaultOptions } from '@tanstack/vue-query'

export const defaultQueryOptions: DefaultOptions['queries'] = {
  staleTime: 5 * 60 * 1000,
  gcTime: 10 * 60 * 1000,
  retry: 1,
  retryDelay: (attemptIndex: number) => Math.min(1000 * 2 ** attemptIndex, 10000),
  refetchOnWindowFocus: false,
  refetchOnReconnect: true,
  refetchOnMount: true,
}

export const defaultMutationOptions: DefaultOptions['mutations'] = {
  retry: 0,
}

export function getVueQueryClientConfig(): { defaultOptions: DefaultOptions } {
  return {
    defaultOptions: {
      queries: defaultQueryOptions,
      mutations: defaultMutationOptions,
    }
  }
}

/**
 * 返回默认查询选项的 Composable
 * 可在组件内调用并局部覆盖特定选项
 */
export function useDefaultQueryOptions() {
  return { ...defaultQueryOptions }
}

/**
 * 返回默认变更选项的 Composable
 * 可在组件内调用并局部覆盖特定选项
 */
export function useDefaultMutationOptions() {
  return { ...defaultMutationOptions }
}
