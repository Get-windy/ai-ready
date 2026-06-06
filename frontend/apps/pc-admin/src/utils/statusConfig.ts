/**
 * 状态配置工具
 * 集中管理各模块的状态颜色与文本映射，避免页面重复定义
 */
import { h } from 'vue'
import { Tag } from 'ant-design-vue'

// ── 通用状态 ────────────────────────────────────────
export type StatusMap = Record<string | number, { text: string; color: string }>

/** 通用启用/停用 */
export const ENABLE_STATUS: StatusMap = {
  0: { text: '启用', color: 'success' },
  1: { text: '停用', color: 'warning' }
}

/** 通用正常/停用 */
export const NORMAL_STATUS: StatusMap = {
  0: { text: '正常', color: 'success' },
  1: { text: '停用', color: 'error' }
}

/** 通用是/否 */
export const YES_NO_STATUS: StatusMap = {
  0: { text: '否', color: 'default' },
  1: { text: '是', color: 'success' }
}

/** 性别 */
export const GENDER_MAP: StatusMap = {
  0: { text: '未知', color: 'default' },
  1: { text: '男', color: 'blue' },
  2: { text: '女', color: 'pink' }
}

// ── 业务状态 ────────────────────────────────────────

/** 采购订单状态 */
export const PURCHASE_ORDER_STATUS: StatusMap = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审批', color: 'orange' },
  2: { text: '已审批', color: 'processing' },
  3: { text: '部分入库', color: 'blue' },
  4: { text: '已完成', color: 'success' },
  5: { text: '已取消', color: 'error' }
}

/** 入库单状态 */
export const INBOUND_STATUS: StatusMap = {
  0: { text: '待入库', color: 'orange' },
  1: { text: '部分入库', color: 'processing' },
  2: { text: '已完成', color: 'success' },
  3: { text: '已取消', color: 'error' }
}

/** 退货单/换货单状态 */
export const RETURN_EXCHANGE_STATUS: StatusMap = {
  0: { text: '待处理', color: 'orange' },
  1: { text: '处理中', color: 'processing' },
  2: { text: '已完成', color: 'success' },
  3: { text: '已拒绝', color: 'error' },
  4: { text: '已取消', color: 'default' }
}

/** 审批状态 */
export const APPROVAL_STATUS: StatusMap = {
  0: { text: '待审批', color: 'orange' },
  1: { text: '已通过', color: 'success' },
  2: { text: '已拒绝', color: 'error' },
  3: { text: '已撤回', color: 'default' }
}

/** 付款状态 */
export const PAYMENT_STATUS: StatusMap = {
  0: { text: '未付款', color: 'error' },
  1: { text: '部分付款', color: 'processing' },
  2: { text: '已付款', color: 'success' },
  3: { text: '已退款', color: 'warning' }
}

/** 库存盘点状态 */
export const STOCKTAKE_STATUS: StatusMap = {
  0: { text: '待盘点', color: 'orange' },
  1: { text: '盘点中', color: 'processing' },
  2: { text: '已完成', color: 'success' },
  3: { text: '已调整', color: 'warning' }
}

/** 凭证状态 */
export const VOUCHER_STATUS: StatusMap = {
  0: { text: '草稿', color: 'default' },
  1: { text: '已审核', color: 'success' },
  2: { text: '已过账', color: 'processing' },
  3: { text: '已作废', color: 'error' }
}

/** 合同状态 */
export const CONTRACT_STATUS: StatusMap = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审核', color: 'orange' },
  2: { text: '已生效', color: 'success' },
  3: { text: '已到期', color: 'warning' },
  4: { text: '已终止', color: 'error' }
}

/** 用户类型 */
export const USER_TYPE_MAP: StatusMap = {
  0: { text: '系统用户', color: 'gold' },
  1: { text: '企业用户', color: 'blue' },
  2: { text: '代理用户', color: 'green' }
}

/** 菜单类型 */
export const MENU_TYPE_MAP: StatusMap = {
  0: { text: '目录', color: 'blue' },
  1: { text: '菜单', color: 'processing' },
  2: { text: '按钮', color: 'green' }
}

/** 操作日志类型 */
export const LOG_TYPE_MAP: StatusMap = {
  0: { text: '登录', color: 'blue' },
  1: { text: '操作', color: 'processing' },
  2: { text: '异常', color: 'error' },
  3: { text: '其他', color: 'default' }
}

/** 价格审批状态（字符串键） */
export const PRICE_APPROVAL_STATUS: StatusMap = {
  pending: { text: '待审批', color: 'orange' },
  approved: { text: '已通过', color: 'success' },
  rejected: { text: '已拒绝', color: 'error' },
  cancelled: { text: '已取消', color: 'default' }
}

/** 出库单/发货状态 */
export const SHIPMENT_STATUS: StatusMap = {
  0: { text: '待审核', color: 'default' },
  1: { text: '已审核', color: 'processing' },
  2: { text: '已出库', color: 'success' }
}

/** 退货状态 */
export const RETURN_STATUS: StatusMap = {
  0: { text: '待审核', color: 'default' },
  1: { text: '已审核', color: 'processing' },
  2: { text: '已入库', color: 'warning' },
  3: { text: '已退款', color: 'success' }
}

/** 盘点状态 */
export const STOCKTAKE_STATUS_ORDER: StatusMap = {
  0: { text: '待盘点', color: 'default' },
  1: { text: '盘点中', color: 'processing' },
  2: { text: '已完成', color: 'success' }
}

/** 固定资产状态 */
export const FIXED_ASSET_STATUS: StatusMap = {
  0: { text: '在库', color: 'success' },
  1: { text: '使用中', color: 'processing' },
  2: { text: '维修中', color: 'warning' },
  3: { text: '已报废', color: 'error' },
  4: { text: '已出库', color: 'default' }
}

// ── 渲染函数 ────────────────────────────────────────

/**
 * 根据状态映射表生成 Tag 组件
 * 用法: <StatusTag :status="record.status" :map="PURCHASE_ORDER_STATUS" />
 */
export function renderStatusTag(status: string | number | undefined | null, map: StatusMap) {
  if (status === undefined || status === null) return h(Tag, { color: 'default' }, '-')
  const config = map[status]
  if (!config) return h(Tag, { color: 'default' }, String(status))
  return h(Tag, { color: config.color }, () => config.text)
}

// ── 通用映射函数 ────────────────────────────────────

export function getStatusText(status: string | number | undefined | null, map: StatusMap): string {
  if (status === undefined || status === null) return '-'
  return map[status]?.text || String(status)
}

export function getStatusColor(status: string | number | undefined | null, map: StatusMap): string {
  if (status === undefined || status === null) return 'default'
  return map[status]?.color || 'default'
}
