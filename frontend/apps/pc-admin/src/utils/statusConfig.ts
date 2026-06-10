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

/** 销售订单状态 */
export const SALES_ORDER_STATUS: StatusMap = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审批', color: 'orange' },
  2: { text: '已审批', color: 'processing' },
  3: { text: '部分发货', color: 'blue' },
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

/** 费用审批状态（字符串键） */
export const EXPENSE_APPROVAL_STATUS: StatusMap = {
  DRAFT: { text: '草稿', color: 'default' },
  SUBMITTED: { text: '待审批', color: 'blue' },
  APPROVING: { text: '审批中', color: 'orange' },
  APPROVED: { text: '已通过', color: 'success' },
  REJECTED: { text: '已拒绝', color: 'error' },
  RETURNED: { text: '已退回', color: 'warning' }
}

/** 费用付款状态（字符串键） */
export const EXPENSE_PAYMENT_STATUS: StatusMap = {
  PENDING: { text: '待付款', color: 'orange' },
  PROCESSING: { text: '处理中', color: 'blue' },
  COMPLETED: { text: '已付款', color: 'success' },
  FAILED: { text: '失败', color: 'error' },
  CANCELLED: { text: '已取消', color: 'default' }
}

/** 预算使用率状态（基于使用率阈值判定） */
export const BUDGET_STATUS: StatusMap = {
  NORMAL: { text: '正常', color: 'success' },
  WARNING: { text: '警戒', color: 'warning' },
  OVERRUN: { text: '超支', color: 'error' }
}

/** 产品启用/停用状态 */
export const PRODUCT_STATUS: StatusMap = {
  ENABLED: { text: '启用', color: 'success' },
  DISABLED: { text: '停用', color: 'error' }
}

/** 费用报销状态 */
export const EXPENSE_REIMBURSEMENT_STATUS: StatusMap = {
  DRAFT: { text: '草稿', color: 'default' },
  SUBMITTED: { text: '已提交', color: 'blue' },
  APPROVING: { text: '审批中', color: 'orange' },
  APPROVED: { text: '已通过', color: 'green' },
  REJECTED: { text: '已拒绝', color: 'red' },
  CANCELLED: { text: '已撤回', color: 'default' },
  WITHDRAWN: { text: '已撤回', color: 'default' }
}

/** 费用申请状态 */
export const EXPENSE_APPLICATION_STATUS: StatusMap = {
  DRAFT: { text: '草稿', color: 'default' },
  SUBMITTED: { text: '待审批', color: 'blue' },
  APPROVING: { text: '审批中', color: 'orange' },
  APPROVED: { text: '已通过', color: 'success' },
  REJECTED: { text: '已拒绝', color: 'error' },
  CANCELLED: { text: '已取消', color: 'default' },
  WITHDRAWN: { text: '已撤回', color: 'default' }
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

/** 往来单位状态 */
export const PARTNER_STATUS: StatusMap = {
  ENABLED: { text: '启用', color: 'success' },
  DISABLED: { text: '停用', color: 'error' }
}

/** 序列号状态 */
export const SERIAL_STATUS: StatusMap = {
  AVAILABLE: { text: '可用', color: 'blue' },
  IN_USE: { text: '使用中', color: 'green' },
  INSERVICE: { text: '售后中', color: 'orange' },
  MAINTAINED: { text: '维修中', color: 'red' },
  SCRAP: { text: '报废', color: 'gray' }
}

/** 序列号阶段 */
export const SERIAL_STAGE: StatusMap = {
  WAREHOUSE: { text: '在库', color: 'blue' },
  IN_TRANSIT: { text: '在途', color: 'gold' },
  EOF_CUSTOMER: { text: '终端客户', color: 'green' },
  IN_SERVICE: { text: '使用中', color: 'orange' },
  SCRAPPED: { text: '已报废', color: 'gray' }
}

/** 批次状态 */
export const BATCH_STATUS: StatusMap = {
  ACTIVE: { text: '启用', color: 'success' },
  EXPIRED: { text: '过期', color: 'error' },
  QUARANTINED: { text: '隔离', color: 'orange' },
  CANCELLED: { text: '取消', color: 'default' }
}

/** 质量状态 */
export const QUALITY_STATUS: StatusMap = {
  NORMAL: { text: '合格', color: 'success' },
  QUARANTINED: { text: '隔离', color: 'orange' },
  DEFECTIVE: { text: '不合格', color: 'error' }
}

/** 退货状态 */
export const RETURN_STATUS: StatusMap = {
  0: { text: '待审核', color: 'default' },
  1: { text: '已审核', color: 'processing' },
  2: { text: '已入库', color: 'warning' },
  3: { text: '已退款', color: 'success' }
}

/** 盘点状态（与后端 StockCheckStatus 枚举一致） */
export const STOCKTAKE_STATUS_ORDER: StatusMap = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审批', color: 'orange' },
  2: { text: '已审批', color: 'processing' },
  3: { text: '已拒绝', color: 'error' },
  4: { text: '盘点中', color: 'blue' },
  5: { text: '进行中', color: 'processing' },
  6: { text: '已完成', color: 'success' },
  7: { text: '已调整', color: 'warning' },
  8: { text: '已取消', color: 'default' }
}

/** 成本调价状态 */
export const COST_ADJUST_STATUS: StatusMap = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审批', color: 'orange' },
  2: { text: '已审批', color: 'processing' },
  3: { text: '已执行', color: 'success' },
  4: { text: '已拒绝', color: 'error' },
  5: { text: '已取消', color: 'default' }
}

/** 报溢单状态 */
export const OVERFLOW_STATUS: StatusMap = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审批', color: 'orange' },
  2: { text: '已审核', color: 'processing' },
  3: { text: '已入库', color: 'success' },
  4: { text: '已拒绝', color: 'error' },
  5: { text: '已取消', color: 'default' }
}

/** 报损单状态 */
export const DAMAGE_STATUS: StatusMap = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审批', color: 'orange' },
  2: { text: '已审核', color: 'processing' },
  3: { text: '已出库', color: 'success' },
  4: { text: '已拒绝', color: 'error' },
  5: { text: '已取消', color: 'default' }
}

/** 调拨单状态 */
export const TRANSFER_STATUS: StatusMap = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审批', color: 'orange' },
  2: { text: '已审批', color: 'processing' },
  3: { text: '已执行', color: 'success' },
  4: { text: '已拒绝', color: 'error' },
  5: { text: '已取消', color: 'default' }
}

/** BOM状态 */
export const BOM_STATUS: StatusMap = {
  0: { text: '停用', color: 'error' },
  1: { text: '启用', color: 'success' }
}

/** 组装单/拆分单状态 */
export const ASSEMBLE_STATUS: StatusMap = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审批', color: 'orange' },
  2: { text: '已审核', color: 'processing' },
  3: { text: '已完成', color: 'success' },
  4: { text: '已拒绝', color: 'error' },
  5: { text: '已取消', color: 'default' }
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
