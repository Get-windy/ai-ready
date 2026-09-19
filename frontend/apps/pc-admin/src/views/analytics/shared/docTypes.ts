/**
 * 分析 → 综合单据（待审批单据 / 业务草稿 / 经营历程）共用的「单据类型登记表」
 *
 * 三页均是对 `DocQueryService` 13 个分支的 UNION 台账，行级 / 批量动作按 `docTypeCode`
 * 分发到各业务域既有端点 —— 不新建审批通道、不直改状态列，避免绕过各域的业务流转。
 * 端点能力逐表核实自后端控制器（2026-09-18），**某类单据没有的动作一律为 null 并由页面隐藏按钮**，
 * 不伪造入口（例：报损单无 submit/approve/reject，只有取消与删除）。
 */

/** 单据类型可用的行级 / 批量动作（null = 该业务域无此端点） */
export interface DocActionSupport {
  /** 提交记账：POST {base}/{id}/submit */
  submit: string | null
  /** 审核通过：POST {base}/{id}/approve */
  approve: string | null
  /** 驳回：POST {base}/{id}/reject */
  reject: string | null
  /** 删除草稿：DELETE {base}/{id} */
  remove: string | null
  /** 复制：POST {base}/{id}/copy */
  copy: string | null
  /** 红冲/作废：POST {base}/{id}/cancel */
  cancel: string | null
}

export interface DocTypeMeta {
  /** 与后端 DocQueryService 分支代码一一对应 */
  code: string
  /** 单据类型名（对标下拉逐字） */
  name: string
  /** 该类型所属业务域的控制器前缀（相对 /api） */
  base: string
  /** 单据表单页路由（点击「单据编号」跳转） */
  formPath: string
  /** 动作能力 */
  actions: DocActionSupport
}

const NO_ACTION: DocActionSupport = { submit: null, approve: null, reject: null, remove: null, copy: null, cancel: null }

/** 声明某类型支持哪些动作（动作名即端点追加段，故值取键名） */
function acts(...kinds: (keyof DocActionSupport)[]): DocActionSupport {
  const out: DocActionSupport = { ...NO_ACTION }
  kinds.forEach(k => { out[k] = k })
  return out
}

export const DOC_TYPES: DocTypeMeta[] = [
  { code: 'SALE_ORDER', name: '销售订单', base: '/erp/sale/order', formPath: '/sales/order/form',
    actions: acts('submit', 'approve', 'reject', 'remove', 'cancel') },
  { code: 'SALE_OUTBOUND', name: '销售出库单', base: '/erp/sale/outbound', formPath: '/sales/outbound/form',
    actions: acts('submit', 'approve', 'reject', 'copy', 'cancel') },
  { code: 'SALE_RETURN', name: '销售退货单', base: '/erp/sale/return', formPath: '/sales/return-apply/form',
    actions: acts('submit', 'approve', 'reject', 'remove', 'cancel') },
  // 销售预订单业务域无驳回/取消端点（已核实 SalePreOrderController）
  { code: 'SALE_PRE_ORDER', name: '销售预订单', base: '/erp/sale/pre-order', formPath: '/sales/pre-order/form',
    actions: acts('submit', 'approve', 'remove') },
  { code: 'PURCHASE_ORDER', name: '采购订单', base: '/erp/purchase/order', formPath: '/purchase/order/form',
    actions: acts('submit', 'approve', 'reject', 'remove', 'cancel') },
  { code: 'PURCHASE_INBOUND', name: '采购入库单', base: '/erp/purchase/inbound', formPath: '/purchase/inbound/form',
    actions: acts('submit', 'approve', 'reject', 'cancel') },
  { code: 'PURCHASE_RETURN', name: '采购退货单', base: '/erp/purchase/return', formPath: '/purchase/return/form',
    actions: acts('submit', 'approve', 'reject', 'cancel') },
  { code: 'RECEIPT', name: '收款单', base: '/erp/receipt', formPath: '/finance/receipt-doc/form',
    actions: acts('submit', 'approve', 'reject', 'cancel') },
  { code: 'PAYMENT', name: '付款单', base: '/erp/payment', formPath: '/finance/payment-doc/form',
    actions: acts('submit', 'approve', 'reject', 'cancel') },
  { code: 'STOCK_TRANSFER', name: '调拨单', base: '/erp/stock/transfer', formPath: '/erp/stock-transfer/form',
    actions: acts('submit', 'approve', 'reject', 'remove', 'cancel') },
  // 报损单走「登记 → 执行」直通流程，业务域本身无提交/审批端点（已核实 StockDamageController）
  { code: 'STOCK_DAMAGE', name: '报损单', base: '/erp/stock/damage', formPath: '/erp/stock-damage/form',
    actions: acts('remove', 'cancel') },
  { code: 'STOCK_OVERFLOW', name: '报溢单', base: '/erp/stock/overflow', formPath: '/erp/stock-overflow/form',
    actions: acts('submit', 'approve', 'reject', 'remove', 'cancel') },
  // 费用申请单：审批走 JPA 审批流（/erp/expense/approval/process 专用入参），其余走 /erp/expense/application
  { code: 'EXPENSE', name: '费用申请单', base: '/erp/expense/application', formPath: '/finance/expense-doc/form',
    actions: acts('submit', 'approve', 'reject', 'remove') }
]

/** 单据类型下拉选项（含「全部单据」由页面自行补） */
export const DOC_TYPE_OPTIONS = DOC_TYPES.map(t => ({ label: t.name, value: t.code }))

const DOC_TYPE_MAP = new Map(DOC_TYPES.map(t => [t.code, t]))

export function getDocType(code: string): DocTypeMeta | undefined {
  return DOC_TYPE_MAP.get(code)
}

/** 账期比较符下拉（对标为「比较符 + 天数」组合） */
export const ACCOUNT_PERIOD_OPS = [
  { label: '≥', value: 'ge' },
  { label: '≤', value: 'le' },
  { label: '=', value: 'eq' },
  { label: '>', value: 'gt' },
  { label: '<', value: 'lt' }
]

/** 时间快捷段（对标实测：昨日/今日/本周/近一周/本月/上月/近三月/本年） */
export const QUICK_DATES = [
  { key: 'yesterday', label: '昨日' },
  { key: 'today', label: '今日' },
  { key: 'week', label: '本周' },
  { key: 'last7', label: '近一周' },
  { key: 'month', label: '本月' },
  { key: 'lastMonth', label: '上月' },
  { key: 'last3Month', label: '近三月' },
  { key: 'year', label: '本年' }
]

/** 时间快捷段 → [startDate, endDate]（yyyy-MM-dd） */
export function quickDateRange(key: string): [string, string] {
  const pad = (n: number) => String(n).padStart(2, '0')
  const fmt = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
  const now = new Date()
  const y = now.getFullYear()
  const m = now.getMonth()
  switch (key) {
    case 'yesterday': {
      const d = new Date(y, m, now.getDate() - 1)
      return [fmt(d), fmt(d)]
    }
    case 'today':
      return [fmt(now), fmt(now)]
    case 'week': {
      // 本周：周一 ~ 今日
      const day = now.getDay() === 0 ? 7 : now.getDay()
      return [fmt(new Date(y, m, now.getDate() - day + 1)), fmt(now)]
    }
    case 'last7':
      return [fmt(new Date(y, m, now.getDate() - 6)), fmt(now)]
    case 'lastMonth':
      return [fmt(new Date(y, m - 1, 1)), fmt(new Date(y, m, 0))]
    case 'last3Month':
      return [fmt(new Date(y, m - 2, 1)), fmt(now)]
    case 'year':
      return [`${y}-01-01`, fmt(now)]
    case 'month':
    default:
      return [fmt(new Date(y, m, 1)), fmt(now)]
  }
}
