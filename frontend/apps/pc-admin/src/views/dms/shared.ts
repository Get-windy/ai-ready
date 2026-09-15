/**
 * DMS 模块共享工具函数与字典
 *
 * 字典取值与后端枚举一一对应，页面禁止再写内联硬编码：
 * · 绑定状态 → `cn.aiedge.dms.verification.enums.BindingStatusEnum`
 * · 告警类型 → `cn.aiedge.dms.verification.enums.AlertTypeEnum`
 */

/** 人车绑定状态 */
export const DMS_BINDING_STATUS: Record<number, { text: string; color: string }> = {
  0: { text: '绑定中', color: 'blue' },
  1: { text: '已交车', color: 'green' },
  2: { text: '异常解绑', color: 'red' },
}

/** 人车核验告警类型 */
export const DMS_ALERT_TYPE: Record<number, { text: string; color: string }> = {
  1: { text: '人车分离', color: 'red' },
  2: { text: '异常滞留', color: 'orange' },
  3: { text: '速度异常', color: 'gold' },
  4: { text: '偏离路线', color: 'volcano' },
  5: { text: '绑定超时', color: 'purple' },
  6: { text: '非工作时段用车', color: 'magenta' },
}

/** 人车核验告警级别 */
export const DMS_ALERT_LEVEL: Record<number, { text: string; color: string }> = {
  1: { text: '提示', color: 'blue' },
  2: { text: '警告', color: 'orange' },
  3: { text: '严重', color: 'red' },
}

/**
 * 格式化日期时间 (YYYY-MM-DD HH:mm:ss)
 */
export function formatDateTime(dateStr?: string | null): string {
  if (!dateStr) return '-'
  try {
    const d = new Date(dateStr)
    if (isNaN(d.getTime())) return dateStr
    const y = d.getFullYear()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    const hh = String(d.getHours()).padStart(2, '0')
    const mm = String(d.getMinutes()).padStart(2, '0')
    const ss = String(d.getSeconds()).padStart(2, '0')
    return `${y}-${m}-${day} ${hh}:${mm}:${ss}`
  } catch {
    return dateStr
  }
}

/**
 * 格式化日期 (YYYY-MM-DD)
 */
export function formatDate(dateStr?: string | null): string {
  if (!dateStr) return '-'
  try {
    const d = new Date(dateStr)
    if (isNaN(d.getTime())) return dateStr
    return d.toLocaleDateString('zh-CN')
  } catch {
    return dateStr
  }
}

