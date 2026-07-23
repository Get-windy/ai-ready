/**
 * POS 收银页共享常量与工具
 */
import type { RetailPayment } from '@/api/retail'

/** 金额千分位 2 位小数 */
export function fmtMoney(v?: number | null): string {
  return (Number(v) || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 两位小数（规避浮点误差） */
export function round2(v: number): number {
  return Math.round((v + Number.EPSILON) * 100) / 100
}

/** 支付方式（与后端 RetailOrderPayment.paymentMethod 对齐） */
export const PAY_METHODS = [
  { value: 'CASH', label: '现金' },
  { value: 'WECHAT', label: '微信' },
  { value: 'ALIPAY', label: '支付宝' },
  { value: 'CARD', label: '银行卡' },
  { value: 'TRANSFER', label: '其他' },
] as const

export function payMethodLabel(method?: string): string {
  return PAY_METHODS.find((m) => m.value === method)?.label || method || '其他'
}

/** 结算弹窗确认结果 */
export interface SettleResult {
  /** 净额支付行（找零已从现金行扣减，合计=应收） */
  payments: RetailPayment[]
  /** 顾客实付总额（含找零） */
  received: number
  /** 找零 */
  change: number
}
