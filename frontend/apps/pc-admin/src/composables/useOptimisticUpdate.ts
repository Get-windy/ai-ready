/**
 * 乐观更新 Composable
 *
 * 模式：先更新 UI，再调用 API；API 失败时自动回滚。
 * 支持撤销操作（Snackbar/toast 方式，含操作按钮）。
 *
 * 泛型 T 必须包含 `id: number | string` 字段，用于定位列表中的条目。
 *
 * @example
 * ```ts
 * const { executeOptimistic, isUndoing } = useOptimisticUpdate({
 *   dataList: tableData,
 *   actionName: '删除',
 *   showUndo: true,
 * })
 *
 * await executeOptimistic(
 *   (list) => list.filter(item => item.id !== record.id),
 *   (originalList) => { tableData.value = originalList },
 *   () => userApi.delete(record.id),
 *   async () => { await userApi.create(savedRecord); fetchData() }
 * )
 * ```
 */
import { ref, h, type Ref } from 'vue'
import { message } from 'ant-design-vue'

export interface OptimisticUpdateOptions<T extends { id: number | string }> {
  /** 要乐观更新的响应式数据列表 */
  dataList: Ref<T[]>

  /** 操作名称，用于提示消息（如"删除"、"更新状态"） */
  actionName?: string

  /** 是否在成功后显示撤销消息，默认 false */
  showUndo?: boolean

  /** 撤销消息显示时长（毫秒），默认 5000 */
  undoTimeout?: number
}

/**
 * 乐观更新工具函数
 *
 * @returns executeOptimistic — 执行乐观更新的核心函数
 * @returns showUndoMessage — 暴露给外部手动触发撤销消息
 * @returns isUndoing — 是否正在执行撤销
 * @returns cleanup — 清理定时器与消息
 */
export function useOptimisticUpdate<T extends { id: number | string }>(
  options: OptimisticUpdateOptions<T>
) {
  const {
    dataList,
    actionName = '操作',
    showUndo = false,
    undoTimeout = 5000,
  } = options

  const isUndoing = ref(false)
  let undoTimer: ReturnType<typeof setTimeout> | null = null
  let undoMessageKey: string | null = null

  // ============================================================
  // 核心：执行乐观更新
  // ============================================================

  /**
   * 执行一次乐观更新。
   *
   * 1. 保存原始列表快照
   * 2. 立即应用 optimisticChange
   * 3. 调用 apiCall
   *    - 成功：保留更改，若 showUndo 且提供 undoRestore 则显示撤销消息
   *    - 失败：调用 rollbackChange 还原 UI，显示错误提示
   *
   * @param optimisticChange - 将列表变换为"已更新"状态（纯函数）
   * @param rollbackChange    - 将列表恢复为原始状态（通常直接赋值 originList）
   * @param apiCall           - 后台 API 调用（返回 Promise）
   * @param undoRestore       - （可选）撤销时调用的服务端恢复逻辑
   * @returns true 表示 API 成功，false 表示失败并已回滚
   */
  async function executeOptimistic(
    optimisticChange: (list: T[]) => T[],
    rollbackChange: (originalList: T[]) => void,
    apiCall: () => Promise<any>,
    undoRestore?: () => Promise<void>,
    actionNameOverride?: string
  ): Promise<boolean> {
    const label = actionNameOverride || actionName

    // 保存快照
    const originalList = [...dataList.value]

    // 立即更新 UI
    dataList.value = optimisticChange([...dataList.value])

    try {
      await apiCall()
      message.success(`${label}成功`)

      // 成功后展示撤销消息（仅当提供了撤销恢复逻辑时）
      if (showUndo && undoRestore) {
        showUndoMessage(label, async () => {
          isUndoing.value = true
          try {
            await undoRestore()
            message.success(`${label}已撤销`)
          } catch (error: any) {
            message.error(error?.message || `撤销${label}失败`)
          } finally {
            isUndoing.value = false
          }
        })
      }

      return true
    } catch (error: any) {
      // API 失败：回滚 UI
      rollbackChange(originalList)
      message.error(error?.message || `${label}失败，已恢复`)
      return false
    }
  }

  // ============================================================
  // 撤销消息
  // ============================================================

  /**
   * 展示带"撤销"操作按钮的成功消息。
   *
   * 消息会在 undoTimeout 毫秒后自动消失；
   * 用户点击"撤销"会立即调用 undoFn 并销毁消息。
   *
   * @param name   - 操作名称，用于消息文案
   * @param undoFn - 撤销时执行的函数（支持异步）
   */
  function showUndoMessage(name: string, undoFn: () => void | Promise<void>) {
    cleanup()

    const key = `undo-${Date.now()}`
    undoMessageKey = key

    message.success({
      content: () =>
        h('span', [
          `${name}成功 `,
          h(
            'a',
            {
              style: {
                cursor: 'pointer',
                marginLeft: '8px',
                fontWeight: 'bold',
                textDecoration: 'underline',
              },
              onClick: () => undoFn(),
            },
            '撤销'
          ),
        ]),
      key,
      duration: undoTimeout / 1000,
    } as any)
  }

  // ============================================================
  // 生命周期
  // ============================================================

  /** 清理定时器与消息 */
  function cleanup() {
    if (undoTimer) {
      clearTimeout(undoTimer)
      undoTimer = null
    }
    if (undoMessageKey) {
      message.destroy(undoMessageKey)
      undoMessageKey = null
    }
  }

  return {
    executeOptimistic,
    showUndoMessage,
    isUndoing,
    cleanup,
  }
}
