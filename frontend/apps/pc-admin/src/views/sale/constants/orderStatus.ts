/**
 * 销售订单状态常量 & 工具函数
 * 集中管理状态映射，消除模板中的魔法数字
 */

export const ORDER_STATUS = {
  DRAFT: 0,
  PENDING_APPROVAL: 1,
  APPROVED: 2,
  PARTIALLY_SHIPPED: 3,
  COMPLETED: 4,
  CANCELLED: 5,
} as const

export type OrderStatusValue = (typeof ORDER_STATUS)[keyof typeof ORDER_STATUS]

export const ORDER_STATUS_TEXT: Record<number, string> = {
  [ORDER_STATUS.DRAFT]: '草稿',
  [ORDER_STATUS.PENDING_APPROVAL]: '待审批',
  [ORDER_STATUS.APPROVED]: '已审批',
  [ORDER_STATUS.PARTIALLY_SHIPPED]: '部分出库',
  [ORDER_STATUS.COMPLETED]: '已完成',
  [ORDER_STATUS.CANCELLED]: '已取消',
}

export const ORDER_STATUS_COLOR: Record<number, string> = {
  [ORDER_STATUS.DRAFT]: 'default',
  [ORDER_STATUS.PENDING_APPROVAL]: 'orange',
  [ORDER_STATUS.APPROVED]: 'green',
  [ORDER_STATUS.PARTIALLY_SHIPPED]: 'blue',
  [ORDER_STATUS.COMPLETED]: 'success',
  [ORDER_STATUS.CANCELLED]: 'red',
}

export function getOrderStatusText(status: number): string {
  return ORDER_STATUS_TEXT[status] || '未知'
}

export function getOrderStatusColor(status: number): string {
  return ORDER_STATUS_COLOR[status] || 'default'
}

export function canEdit(status: number): boolean {
  return status === ORDER_STATUS.DRAFT
}

export function canSubmit(status: number): boolean {
  return status === ORDER_STATUS.DRAFT
}

export function canApprove(status: number): boolean {
  return status === ORDER_STATUS.PENDING_APPROVAL
}

export function canDelete(status: number): boolean {
  return status === ORDER_STATUS.DRAFT || status === ORDER_STATUS.CANCELLED
}
