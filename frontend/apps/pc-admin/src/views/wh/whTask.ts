/**
 * wh 域作业单页面共享辅助
 * 状态常量与后端 cn.aiedge.wms.enums.WmsTaskStatus 对齐
 */

/** WMS 任务状态（WmsTaskStatus: 0待处理 1处理中 2已完成 3已取消 4异常） */
export const WMS_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '待处理', color: 'orange' },
  1: { text: '处理中', color: 'blue' },
  2: { text: '已完成', color: 'green' },
  3: { text: '已取消', color: 'red' },
  4: { text: '异常', color: 'red' },
}

/** 盘点任务状态（CheckServiceImpl 流程：0待盘点 1盘点中 2待审核 3已审核） */
export const CHECK_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '待盘点', color: 'orange' },
  1: { text: '盘点中', color: 'blue' },
  2: { text: '待审核', color: 'gold' },
  3: { text: '已审核', color: 'green' },
  4: { text: '异常', color: 'red' },
}

/** 明细行状态（后端明细 status：0待处理 1已完成） */
export const DETAIL_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '待处理', color: 'orange' },
  1: { text: '已完成', color: 'green' },
  2: { text: '异常', color: 'red' },
}

/** 数量千分位格式化 */
export function formatQty(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

/** 生成默认单号：前缀 + yyyyMMddHHmmss */
export function genTaskNo(prefix: string): string {
  const d = new Date()
  const p = (n: number) => String(n).padStart(2, '0')
  return `${prefix}${d.getFullYear()}${p(d.getMonth() + 1)}${p(d.getDate())}${p(d.getHours())}${p(d.getMinutes())}${p(d.getSeconds())}`
}

/** 截取日期时间到分钟（后端 LocalDateTime 序列化为 ISO 字符串） */
export function formatTime(val: string | null | undefined): string {
  if (!val) return '-'
  return String(val).replace('T', ' ').slice(0, 16)
}
