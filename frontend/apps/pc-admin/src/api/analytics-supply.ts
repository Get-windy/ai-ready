import request from '@/utils/request'

/**
 * 分析模块「采购/库存/预订货/推广」四页专用 API 封装
 *
 * 覆盖：
 * - 采购分析（80421）        /erp/purchase/analytics/page
 * - 进销存分析（80433）      /erp/stock/analytics/page
 * - 预订货查询（80419）      /erp/sale/pre-order/analysis/page
 * - 推广分析（80418）        /erp/sale/analysis/promotion-funnel/page
 *
 * ⚠️ `utils/request.ts` 的 axios 实例已设 `baseURL: '/api'`，此处**一律写相对路径**，
 *    写成 `/api/...` 会变成双前缀导致 404。
 *
 * 响应经拦截器解包（`code===200` 时直接返回 `data`），因此拿到的就是
 * `{ records, total, page, size, pages, summary }`。
 *
 * 说明：本文件仅供这 4 个页面使用；分析模块其它页面请沿用 `src/api/analytics.ts`
 * 或 `src/api/analytics-sales.ts`（勿在其中追加本组端点）。
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

/** 四页共用查询参数（逐页按对标查询区取用其中一部分） */
export interface SupplyAnalyticsQuery {
  /** 视图 Tab：purchase=time|product|supplier；inventory=product|transferWarehouse|transferProduct；
   *  preOrder=product|customer；推广分析为单视图无需传 */
  tab?: string
  startDate?: string
  endDate?: string
  /** 采购分析：供应商名称模糊 */
  supplierName?: string
  /** 商品关键字（名称/货号/条码） */
  keyword?: string
  /** 采购分析：经手人；预订货查询：经手人；推广分析：职员名称 */
  handlerName?: string
  /** 采购分析：仓库名称模糊 */
  warehouseName?: string
  /** 进阶存分析调拨视图：出库仓库名称模糊 */
  outWarehouseName?: string
  /** 进阶存分析调拨视图：入库仓库名称模糊 */
  inWarehouseName?: string
  /** 品牌模糊 */
  brand?: string
  /** 采购分析：部门名称模糊 */
  deptName?: string
  /** 采购分析「按时间」粒度：day（默认）/ week / month */
  granularity?: 'day' | 'week' | 'month'
  /** 预订货查询：客户名称模糊 */
  customerName?: string
  /** 推广分析：分享类型（PRODUCT/COUPON/PROMOTION/GROUP_BUY/FLASH_SALE） */
  shareType?: string
  /** 进销存分析：显示红冲 */
  showReversed?: boolean
  page?: number
  size?: number
}

function get<T = Record<string, any>>(path: string, params: SupplyAnalyticsQuery) {
  return request.get<AnalyticsPageResult<T>>(path, { params }) as unknown as Promise<AnalyticsPageResult<T>>
}

/** 采购分析（按时间 / 按商品 / 按供应商） */
export const purchaseAnalysisApi = {
  page(params: SupplyAnalyticsQuery) {
    return get('/erp/purchase/analytics/page', params)
  }
}

/** 进销存分析（按商品 / 仓库调拨分析 / 商品调拨分析） */
export const inventoryAnalysisApi = {
  page(params: SupplyAnalyticsQuery) {
    return get('/erp/stock/analytics/page', params)
  }
}

/** 预订货查询（商品预订货分析 / 客户预订货分析） */
export const preOrderAnalysisApi = {
  page(params: SupplyAnalyticsQuery) {
    return get('/erp/sale/pre-order/analysis/page', params)
  }
}

/** 推广分析（职员分享推广漏斗，单视图） */
export const promotionFunnelApi = {
  page(params: SupplyAnalyticsQuery) {
    return get('/erp/sale/analysis/promotion-funnel/page', params)
  }
}
