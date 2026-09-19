import request from '@/utils/request'

/**
 * 分析模块「财务批次」5 页专用 API 封装
 *
 * 覆盖（本文件仅供这 5 页使用，勿在其它的 analytics-*.ts 内追加本组端点）：
 * - 回款统计（80442）    /erp/finance/analytics/collection-stats/page|detail
 * - 业绩提成中心（80443）/erp/marketing/commission/analytics/*
 * - 查费用（80454）      /erp/expense/statistics/matrix|detail|partner
 * - 发票统计（80455）    /erp/finance/analytics/invoice-stats/page
 * - 往来余额表（80459）  /erp/finance/analytics/partner-balance/page|detail|reconcile|reconcile-history
 *
 * ⚠️ `utils/request.ts` 的 axios 实例已设 `baseURL: '/api'`，此处**一律写相对路径**，
 *    写成 `/api/...` 会变成双前缀导致 404。
 *
 * 响应经拦截器解包（`code===200` 时直接返回 `data`），因此拿到的就是
 * `{ records, total, page, size, pages, summary }`。
 */

/** 分页 + 合计行响应 */
export interface AnalyticsPageResult<T = Record<string, any>> {
  records: T[]
  total: number
  page: number
  size: number
  pages: number
  /** 合计行：按当前过滤范围求和；无法计算的比率/无源列返回 null */
  summary?: Record<string, any>
}

/** 五页共用查询参数（逐页按对标查询区取用其中一部分） */
export interface FinanceAnalyticsQuery {
  /** 视图 Tab */
  tab?: string
  startDate?: string
  endDate?: string
  /** 发票统计：起始月份 YYYY-MM */
  monthStart?: string
  /** 发票统计：结束月份 YYYY-MM */
  monthEnd?: string
  /** 发票统计：查询方式（按月查询） */
  queryMode?: string
  /** 年度（提成矩阵） */
  year?: number
  /** 日期类型 */
  dateType?: string
  /** 部门名称模糊 */
  deptName?: string
  /** 职员名称模糊 */
  staffName?: string
  /** 往来单位名称模糊 */
  partnerName?: string
  /** 单据编号 / 科目关键字模糊 */
  keyword?: string
  /** 仅显示既是客户又是供应商（往来余额表清账场景） */
  onlyBoth?: boolean
  /** 显示停用 */
  showDisabled?: boolean
  /** 显示本期金额为 0 的数据 */
  showZero?: boolean
  /** 不显示为零数据项（提成构成 / 方案汇总提成） */
  hideZero?: boolean
  /** 进销均无发生的不显示（发票统计） */
  hideEmpty?: boolean
  /** 包含红冲（查费用） */
  includeReversed?: boolean
  /** 配送员名称模糊 */
  riderName?: string
  /** 配送员角色 */
  roleKey?: string
  /** 提成方案名称模糊 */
  planName?: string
  /** 提成类型 */
  typeKey?: string
  /** 仓库名称模糊（业绩明细） */
  warehouseName?: string
  /** 商品名称/货号模糊（业绩明细） */
  productName?: string
  page?: number
  size?: number
}

function get<T = Record<string, any>>(path: string, params: FinanceAnalyticsQuery) {
  return request.get<AnalyticsPageResult<T>>(path, { params }) as unknown as Promise<AnalyticsPageResult<T>>
}

// ════════════════════════════════════════════════════════════════════
//  1. 回款统计（按职员 / 按部门，三口径）
// ════════════════════════════════════════════════════════════════════

export interface CollectionStatsRow {
  code: string | null
  name: string
  groupKey: string
  docCount: number
  receiptAmount: number
  preReceiptAmount: number
  preOrderDepositAmount: number
  totalAmount: number
}

export const collectionStatsApi = {
  page(params: FinanceAnalyticsQuery) {
    return get<CollectionStatsRow>('/erp/finance/analytics/collection-stats/page', params)
  },
  /** 行级「明细」：该职员/部门在三张单据表下的回款流水 */
  detail(params: FinanceAnalyticsQuery) {
    return request.get('/erp/finance/analytics/collection-stats/detail', { params }) as unknown as Promise<{
      receipts: any[]
      preReceipts: any[]
      preOrders: any[]
    }>
  }
}

// ════════════════════════════════════════════════════════════════════
//  2. 业绩提成中心（六视图 + 批量结算）
// ════════════════════════════════════════════════════════════════════

export const commissionAnalyticsApi = {
  /** 配送员 / 每月提成：人员 × 12 月矩阵 */
  riderMatrix(params: FinanceAnalyticsQuery) {
    return get('/erp/marketing/commission/analytics/rider-matrix/page', params)
  },
  /** 提成构成（7 列） */
  composition(params: FinanceAnalyticsQuery) {
    return get('/erp/marketing/commission/analytics/composition/page', params)
  },
  /** 方案汇总提成（5 列） */
  planSummary(params: FinanceAnalyticsQuery) {
    return get('/erp/marketing/commission/analytics/plan-summary/page', params)
  },
  /** 业绩概览（21 列） */
  performanceOverview(params: FinanceAnalyticsQuery) {
    return get('/erp/marketing/commission/analytics/performance-overview/page', params)
  },
  /** 业绩明细（21 列） */
  performanceDetail(params: FinanceAnalyticsQuery) {
    return get('/erp/marketing/commission/analytics/performance-detail/page', params)
  },
  /** 提成方案列表（工具栏「提成方案」只读入口） */
  plans() {
    return request.get('/erp/marketing/commission/analytics/plans') as unknown as Promise<any[]>
  },
  /** 批量结算（把指定年月未结提成置为已结算） */
  settle(data: { year: number; month: number; riderNames?: string[] }) {
    return request.post('/erp/marketing/commission/analytics/settle', data) as unknown as Promise<{
      year: number
      month: number
      settledCount: number
    }>
  }
}

// ════════════════════════════════════════════════════════════════════
//  3. 查费用（按部门 / 按职员 / 按明细 / 按往来单位）
// ════════════════════════════════════════════════════════════════════

export const expenseAnalyticsApi = {
  /** 按部门 / 按职员矩阵（tab=dept|staff）：额外返回 matrixColumns（动态矩阵列，不进列配置） */
  matrix(params: FinanceAnalyticsQuery) {
    return request.get('/erp/expense/statistics/matrix', { params }) as unknown as Promise<
      AnalyticsPageResult & { matrixColumns?: string[] }
    >
  },
  /** 按明细（15 列） */
  detail(params: FinanceAnalyticsQuery) {
    return get('/erp/expense/statistics/detail', params)
  },
  /** 按往来单位（4 列 + 占比） */
  partner(params: FinanceAnalyticsQuery) {
    return get('/erp/expense/statistics/partner', params)
  }
}

// ════════════════════════════════════════════════════════════════════
//  4. 发票统计（单视图 17 列 · 多级表头）
// ════════════════════════════════════════════════════════════════════

export const invoiceStatsApi = {
  page(params: FinanceAnalyticsQuery) {
    return get('/erp/finance/analytics/invoice-stats/page', params)
  }
}

// ════════════════════════════════════════════════════════════════════
//  5. 往来余额表（四象限 18 列 + 清账）
// ════════════════════════════════════════════════════════════════════

export const partnerLedgerApi = {
  page(params: FinanceAnalyticsQuery) {
    return get('/erp/finance/analytics/partner-balance/page', params)
  },
  /** 行级「对账」：该结算单位四象限来源流水 */
  detail(params: FinanceAnalyticsQuery) {
    return request.get('/erp/finance/analytics/partner-balance/detail', { params }) as unknown as Promise<{
      receivables: any[]
      preReceipts: any[]
      payables: any[]
      prePayments: any[]
    }>
  },
  /** 行级「清账」：应收与应付对冲（经会计凭证） */
  reconcile(partnerName: string, amount: number, remark?: string) {
    return request.post('/erp/finance/analytics/partner-balance/reconcile', null, {
      params: { partnerName, amount, remark }
    }) as unknown as Promise<{ docNo: string; partnerName: string; amount: number; docDate: string }>
  },
  /** 工具栏「清账历史」 */
  reconcileHistory(page = 1, size = 20) {
    return request.get('/erp/finance/analytics/partner-balance/reconcile-history', {
      params: { page, size }
    }) as unknown as Promise<AnalyticsPageResult>
  }
}

export type { AnalyticsPageResult as FinanceAnalyticsPageResult }
