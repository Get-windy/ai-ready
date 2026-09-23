import { message } from 'ant-design-vue'

/** 导入接口统一的返回结构（后端各模块一致） */
export interface ImportResult {
  /** 成功行数 */
  count?: number
  /** 失败行数 */
  failed?: number
  /** 失败原因，形如「第3行: 商品[X]在商品档案中不存在」 */
  errors?: string[]
}

/**
 * 统一展示 Excel 导入结果。
 *
 * 为什么需要它：导入接口原先只返回成功条数，失败行被后端逐行 catch 后**静默丢弃** ——
 * 用户看到「导入成功 N 条」却不知道另有一半没进来（2026-09-22 审计 P1）。
 * 现在后端回传 `failed` 与逐行原因，这里负责把它显示出来。
 */
export function showImportResult(data?: ImportResult): void {
  const ok = data?.count ?? 0
  const failed = data?.failed ?? 0
  if (failed > 0) {
    const detail = (data?.errors || []).slice(0, 3).join('；')
    message.warning(`导入成功 ${ok} 条，失败 ${failed} 条：${detail}${failed > 3 ? ' …' : ''}`, 8)
  } else {
    message.success(`导入成功 ${ok} 条`)
  }
}
