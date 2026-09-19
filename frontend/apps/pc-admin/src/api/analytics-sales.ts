import request from '@/utils/request'

/**
 * 分析模块「销售分析组」专用 API 封装（销售业绩 / 销售分析 / 销售履约分析 / 销售欠款分析）
 *
 * 后端端点统一挂在 `/api/erp/sale/analysis/**`（SaleAnalysisReportController）。
 * ⚠️ `utils/request.ts` 的 axios 实例已设 `baseURL: '/api'`，此处**一律写相对路径**，
 *    写成 `/api/...` 会变成双前缀导致 404。
 *
 * 响应经拦截器解包（`code===200` 时直接返回 `data`），因此拿到的就是
 * `{ records, total, page, size, pages, summary }`。
 *
 * 说明：本文件仅供这 4 个页面使用；分析模块其它页面请沿用 `src/api/analytics.ts`（勿在其内追加本组端点）。
 */

/** 分页 + 合计行响应 */
export interface SaleAnalysisPageResult<T = Record<string, any>> {
  records: T[]
  total: number
  page: number
  size: number
  pages: number
  /** 合计行：对当前过滤范围求和；比率列按合计口径重算，无法计算的比率返回 null */
  summary?: Record<string, any>
}

/** 四个页面共用的查询参数（逐页按对标查询区取用其中一部分） */
export interface SaleAnalysisQuery {
  /** 视图 Tab：sales-performance=time|staff；sales-analysis=time|product|brand|customer|region|warehouse|staff|source；
   *  sales-fulfillment=doc|customer；sales-debt=staff|customer|region */
  tab?: string
  /** 起始日期 yyyy-MM-dd（含） */
  startDate?: string
  /** 结束日期 yyyy-MM-dd（含） */
  endDate?: string
  customerId?: string
  customerName?: string
  salesmanId?: string
  salesmanName?: string
  deptId?: string
  deptName?: string
  warehouseId?: string
  warehouseName?: string
  region?: string
  /** 商品关键字（名称/货号/条码） */
  keyword?: string
  brand?: string
  /** 来源 / 产生方式 */
  source?: string
  saleType?: number
  /** 时间粒度：day（默认）/ week / month，仅「按时间」维度生效 */
  granularity?: 'day' | 'week' | 'month'
  /** 不显示停用职员 */
  hideDisabledStaff?: boolean
  /** 仅显示销售数量大于 0 的行 */
  onlyPositiveQty?: boolean
  /** 单据状态多选（销售履约分析），逗号分隔状态码 */
  docStatus?: string
  /** 客户分类ID多选（销售履约分析左侧客户分类树），逗号分隔，含所选节点及其全部下级 */
  customerCategoryIds?: string
  page?: number
  size?: number
}

function get<T = Record<string, any>>(path: string, params: SaleAnalysisQuery) {
  return request.get<SaleAnalysisPageResult<T>>(path, { params }) as unknown as Promise<SaleAnalysisPageResult<T>>
}

export const saleAnalysisReportApi = {
  /** 销售业绩（按时间 / 按职员） */
  performance(params: SaleAnalysisQuery) {
    return get('/erp/sale/analysis/sales-performance/page', params)
  },
  /** 销售分析（按时间/商品/品牌/客户/区域/仓库/职员/来源） */
  analysis(params: SaleAnalysisQuery) {
    return get('/erp/sale/analysis/sales-analysis/page', params)
  },
  /** 销售履约分析（按单据 / 按客户） */
  fulfillment(params: SaleAnalysisQuery) {
    return get('/erp/sale/analysis/sales-fulfillment/page', params)
  },
  /** 销售欠款分析（按职员 / 按客户 / 按区域） */
  debt(params: SaleAnalysisQuery) {
    return get('/erp/sale/analysis/sales-debt/page', params)
  }
}
