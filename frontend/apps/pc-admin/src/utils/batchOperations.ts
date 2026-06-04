/**
 * 批量操作通用工具
 * 统一处理循环调用 API、成功/失败计数、消息提示
 */
import { message } from 'ant-design-vue'

export interface BatchResult {
  successCount: number
  failCount: number
}

/**
 * 执行批量操作（逐项调用 API）
 * @param ids 要操作的数据 ID 列表
 * @param apiFn 单个操作的 API 函数 (id) => Promise<any>
 * @param actionName 操作名称（用于提示语）
 * @returns 批量结果
 */
export async function executeBatch(
  ids: number[],
  apiFn: (id: number) => Promise<any>,
  actionName: string
): Promise<BatchResult> {
  let successCount = 0
  let failCount = 0

  for (const id of ids) {
    try {
      await apiFn(id)
      successCount++
    } catch {
      failCount++
    }
  }

  if (failCount === 0) {
    message.success(`${actionName}完成，成功 ${successCount} 个`)
  } else {
    message.warning(`${actionName}完成: 成功 ${successCount} 个, 失败 ${failCount} 个`)
  }

  return { successCount, failCount }
}

/**
 * 获取选中项数量并校验
 * @param selectedKeys 选中的 key 数组
 * @param actionName 操作名称
 * @returns 是否有效（有选中项）
 */
export function validateSelection(
  selectedKeys: number[] | string[],
  actionName: string
): boolean {
  if (selectedKeys.length === 0) {
    message.warning(`请选择要${actionName}的记录`)
    return false
  }
  return true
}
