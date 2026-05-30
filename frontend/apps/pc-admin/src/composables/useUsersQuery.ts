/**
 * 用户管理 Vue Query Hooks
 *
 * 提供用户数据的查询、变更及乐观更新能力。
 * 安装依赖: pnpm add @tanstack/vue-query
 */
import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'
import { userApi } from '@/api/user'
import type { UserQuery, UserInfo } from '@/api/user'
import type { PageResponse } from '@/utils/request'
import { message } from 'ant-design-vue'

// ── Query Key 工厂 ──────────────────────────────────────

export const userKeys = {
  /** 用户列表相关 key */
  all: ['users'] as const,
  lists: () => [...userKeys.all, 'list'] as const,
  list: (params: UserQuery) => [...userKeys.lists(), params] as const,
  /** 用户详情相关 key */
  details: () => [...userKeys.all, 'detail'] as const,
  detail: (id: number) => [...userKeys.details(), id] as const,
}

// ── 分页查询 ────────────────────────────────────────────

/**
 * 用户分页列表查询
 * @param params 查询参数（响应式 ref / computed）
 */
export function useUsersQuery(params: () => UserQuery) {
  return useQuery({
    queryKey: () => userKeys.list(params()),
    queryFn: async () => {
      const res = await userApi.getPage(params())
      return res.data
    },
    placeholderData: (prev) => prev,
  })
}

// ── 单条查询 ────────────────────────────────────────────

/**
 * 单个用户详情查询
 * @param id 用户 ID（响应式 ref / computed）
 */
export function useUserQuery(id: () => number | undefined) {
  return useQuery({
    queryKey: () => userKeys.detail(id()!),
    queryFn: async () => {
      const res = await userApi.getById(id()!)
      return res.data
    },
    enabled: () => id() !== undefined && id()! > 0,
  })
}

// ── 创建用户 ────────────────────────────────────────────

/**
 * 创建用户变更
 * 成功后自动刷新用户列表
 */
export function useCreateUser() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: Parameters<typeof userApi.create>[0]) => userApi.create(data),
    onSuccess: () => {
      message.success('创建用户成功')
      queryClient.invalidateQueries({ queryKey: userKeys.lists() })
    },
    onError: (error: Error) => {
      message.error(error?.message || '创建用户失败')
    },
  })
}

// ── 更新用户 ────────────────────────────────────────────

/**
 * 更新用户变更
 * 成功后刷新用户列表及对应详情
 */
export function useUpdateUser() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Parameters<typeof userApi.update>[1] }) =>
      userApi.update(id, data),
    onSuccess: (_result, variables) => {
      message.success('更新用户成功')
      queryClient.invalidateQueries({ queryKey: userKeys.lists() })
      queryClient.invalidateQueries({ queryKey: userKeys.detail(variables.id) })
    },
    onError: (error: Error) => {
      message.error(error?.message || '更新用户失败')
    },
  })
}

// ── 删除用户 ────────────────────────────────────────────

/**
 * 删除用户变更
 * 成功后刷新用户列表并移除对应缓存
 */
export function useDeleteUser() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: number) => userApi.delete(id),
    onSuccess: (_result, id) => {
      message.success('删除用户成功')
      queryClient.invalidateQueries({ queryKey: userKeys.lists() })
      queryClient.removeQueries({ queryKey: userKeys.detail(id) })
    },
    onError: (error: Error) => {
      message.error(error?.message || '删除用户失败')
    },
  })
}

// ── 切换用户状态（乐观更新） ────────────────────────────

/**
 * 用户启用 / 停用切换
 * 使用乐观更新：先更新 UI，API 失败时自动回滚
 */
export function useToggleUserStatus() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, status }: { id: number; status: number }) =>
      userApi.updateStatus(id, status),

    onMutate: async ({ id, status }) => {
      // 取消进行中的相关查询，避免覆盖乐观更新
      await queryClient.cancelQueries({ queryKey: userKeys.lists() })
      await queryClient.cancelQueries({ queryKey: userKeys.detail(id) })

      // 保存回滚快照
      const previousLists = queryClient.getQueriesData<PageResponse<UserInfo>>({
        queryKey: userKeys.lists(),
      })
      const previousDetail = queryClient.getQueryData<UserInfo>(userKeys.detail(id))

      // 乐观更新：列表中的用户状态
      for (const [queryKey] of previousLists) {
        queryClient.setQueryData<PageResponse<UserInfo>>(queryKey, (old) => {
          if (!old) return old
          return {
            ...old,
            records: old.records.map((user) =>
              (user as any).userId === id || (user as any).id === id
                ? { ...user, status }
                : user
            ),
          }
        })
      }

      // 乐观更新：单条用户详情
      if (previousDetail) {
        queryClient.setQueryData<UserInfo>(userKeys.detail(id), {
          ...previousDetail,
          status,
        } as any)
      }

      return { previousLists, previousDetail }
    },

    onError: (error: Error, { id }, context) => {
      message.error(error?.message || '状态切换失败，已恢复')

      // 回滚列表数据
      if (context?.previousLists) {
        for (const [queryKey, data] of context.previousLists) {
          queryClient.setQueryData(queryKey, data)
        }
      }

      // 回滚详情数据
      if (context?.previousDetail) {
        queryClient.setQueryData(userKeys.detail(id), context.previousDetail)
      }
    },

    onSettled: (_result, _error, { id }) => {
      // 无论成功失败，最终以服务端数据为准
      queryClient.invalidateQueries({ queryKey: userKeys.lists() })
      queryClient.invalidateQueries({ queryKey: userKeys.detail(id) })
    },
  })
}
