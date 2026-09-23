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

/** 明细行状态（后端明细 status：0待处理 1已完成） */
export const DETAIL_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '待处理', color: 'orange' },
  1: { text: '已完成', color: 'green' },
  2: { text: '异常', color: 'red' },
}

/** 移库类型（WmsMoveTask.moveType 与后端/数据库注释对齐：1库内移库 2补货移库 3整理移库） */
export const MOVE_TYPE_MAP: Record<number, { text: string; color: string }> = {
  1: { text: '库内移库', color: 'default' },
  2: { text: '补货移库', color: 'blue' },
  3: { text: '整理移库', color: 'purple' },
}

/**
 * 拣货任务状态（cn.aiedge.wms 拣货状态机：0待拣货 1拣货中 2已完成 3缺货 4已取消）
 * 区别于通用 WMS_STATUS_MAP（3=已取消/4=异常），拣货单拥有独立状态语义
 */
export const PICK_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '待拣货', color: 'orange' },
  1: { text: '拣货中', color: 'blue' },
  2: { text: '已完成', color: 'green' },
  3: { text: '缺货', color: 'red' },
  4: { text: '已取消', color: 'default' },
}

/** 拣货来源类型（对齐 WmsPickTask.sourceType：1销售出库 2退货出库 3调拨出库 4盘亏出库） */
export const PICK_SOURCE_TYPE_OPTIONS = [
  { label: '销售出库', value: 1 },
  { label: '退货出库', value: 2 },
  { label: '调拨出库', value: 3 },
  { label: '盘亏出库', value: 4 },
]

/** 拣货来源类型文案 */
export function pickSourceTypeText(t: number): string {
  return PICK_SOURCE_TYPE_OPTIONS.find(o => o.value === t)?.label || '其他'
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
