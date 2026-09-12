/**
 * 商品价格管理 API（资料 → 商品管理 → 商品价格管理）
 *
 * 对标 ql361 四子标签：商品价格批量修改 / 客户级别折扣设置 / 级别指定价设置 / 客户指定价设置。
 * 价格口径与《商品》《批量改价》同源（erp_product / erp_product_unit），不做本地副本。
 */
import request from '@/utils/request'
import type { PageResult } from '@/api/erp'

/** 子标签 1：商品价格行（商品 × 单位） */
export interface ProductPriceRow {
  unitId: string
  productId: string
  imageUrl?: string
  shelfStatus?: number
  shelfStatusText?: string
  productCode?: string
  productName?: string
  unitName?: string
  brand?: string
  conversionRelation?: string
  barcode?: string
  recentPurchasePrice?: number
  presetPurchasePrice?: number
  referenceCost?: number
  costAvgPrice?: number
  stockQty?: number
  lastPurchaseDate?: string
  wholesalePrice?: number
  minDiscount?: number
  minSalePrice?: number
  retailPrice?: number
  gradePrice1?: number
  gradePrice2?: number
  gradePrice3?: number
  gradePrice4?: number
  gradePrice5?: number
  gradePrice6?: number
  gradePrice7?: number
  gradePrice8?: number
  spec?: string
  model?: string
  origin?: string
  updateTime?: string
}

export interface ProductPriceQuery {
  categoryId?: string
  keyword?: string
  brand?: string
  unitType?: string
  productId?: string
  shelfStatus?: number
  purchaseDateOp?: string
  purchaseDate?: string
  stockQtyOp?: string
  stockQty?: string
  /** 显示层次结构：勾选后按商品分类聚合排序 */
  showHierarchy?: boolean
  pageNum?: number
  pageSize?: number
}

/** 价格修改项（批量修改弹窗一行） */
export interface PriceModifyItem {
  field: string
  mode: 'FIXED' | 'RULE'
  value?: number | null
  basePriceField?: string
  calcOperator?: string
  calcValue?: number | null
}

/** 子标签 2：客户级别折扣 */
export interface CustomerGradeDiscount {
  id?: string
  gradeName: string
  basePriceType: string
  calcOperator: string
  calcValue: number
  previewText?: string
  lastModifierName?: string
  updateTime?: string
}

/** 子标签 3/4：指定价行 */
export interface GradePriceRow {
  id: string
  gradeName?: string
  customerId?: string
  customerName?: string
  customerCode?: string
  productId?: string
  productCode?: string
  productName?: string
  categoryId?: string
  categoryName?: string
  targetName?: string
  unitId?: string
  unitName?: string
  priceRule?: string
  basePriceType?: string
  calcOperator?: string
  calcValue?: number
  barcode?: string
  spec?: string
  model?: string
  brand?: string
  presetPurchasePrice?: number
  retailPrice?: number
  wholesalePrice?: number
  gradePrice1?: number
  gradePrice2?: number
  gradePrice3?: number
  gradePrice4?: number
  gradePrice5?: number
  gradePrice6?: number
  gradePrice7?: number
  gradePrice8?: number
  lastModifierName?: string
  lastModifyTime?: string
}

export const productPriceApi = {
  /** 子标签 1：商品价格分页 */
  page(params: ProductPriceQuery): Promise<PageResult<ProductPriceRow>> {
    return request.get('/erp/md/product-price/page', { params })
  },
  /** 子标签 1：导出真实 Excel */
  exportExcel(params: ProductPriceQuery): Promise<Blob> {
    return request.get('/erp/md/product-price/export', { params, responseType: 'blob' })
  },
  /** 子标签 1：批量修改价格 */
  batchModify(payload: { unitIds: string[]; items: PriceModifyItem[] }): Promise<number> {
    return request.put('/erp/md/product-price/batch-modify', payload)
  },
  /** 品牌下拉 */
  getBrands(): Promise<string[]> {
    return request.get('/erp/md/product-price/brands')
  },
  /** 客户级别下拉 */
  getCustomerGrades(): Promise<string[]> {
    return request.get('/erp/md/product-price/customer-grades')
  },
  /** 价格等级名称列表（按槽位顺序，与列表列标题/导出表头同源） */
  getGrades(): Promise<string[]> {
    return request.get('/erp/md/product-price/grades')
  },
  /** 统一取价：客户/商品/单位 → 最终售价（含命中规则），供销售/采购/零售复用 */
  resolvePrice(params: { productId: string; customerId?: string; unitId?: string }): Promise<Record<string, any>> {
    return request.get('/erp/md/product-price/resolve', { params })
  },

  // ── 子标签 2：客户级别折扣设置 ──
  gradeDiscountPage(params: { keyword?: string; pageNum?: number; pageSize?: number }): Promise<PageResult<CustomerGradeDiscount>> {
    return request.get('/erp/md/product-price/grade-discount/page', { params })
  },
  gradeDiscountExport(keyword?: string): Promise<Blob> {
    return request.get('/erp/md/product-price/grade-discount/export', { params: { keyword }, responseType: 'blob' })
  },
  saveGradeDiscount(data: CustomerGradeDiscount): Promise<boolean> {
    return request.post('/erp/md/product-price/grade-discount/save', data)
  },
  deleteGradeDiscount(id: string): Promise<boolean> {
    return request.delete(`/erp/md/product-price/grade-discount/${id}`)
  },

  // ── 子标签 3：级别指定价设置 ──
  levelPricePage(params: { gradeName?: string; keyword?: string; brand?: string; pageNum?: number; pageSize?: number }): Promise<PageResult<GradePriceRow>> {
    return request.get('/erp/md/product-price/level-price/page', { params })
  },
  levelPriceExport(params: { gradeName?: string; keyword?: string; brand?: string }): Promise<Blob> {
    return request.get('/erp/md/product-price/level-price/export', { params, responseType: 'blob' })
  },
  saveLevelPrice(data: Record<string, any>): Promise<boolean> {
    return request.post('/erp/md/product-price/level-price/save', data)
  },
  deleteLevelPrices(ids: string[]): Promise<boolean> {
    return request.post('/erp/md/product-price/level-price/batch-delete', { ids })
  },
  importLevelPrices(rows: Record<string, any>[]): Promise<{ success: number; failed: number; messages: string[] }> {
    return request.post('/erp/md/product-price/level-price/import', { rows })
  },

  // ── 子标签 4：客户指定价设置 ──
  customerPricePage(params: { customerId?: string; productId?: string; keyword?: string; brand?: string; pageNum?: number; pageSize?: number }): Promise<PageResult<GradePriceRow>> {
    return request.get('/erp/md/product-price/customer-price/page', { params })
  },
  customerPriceExport(params: { customerId?: string; productId?: string; keyword?: string; brand?: string }): Promise<Blob> {
    return request.get('/erp/md/product-price/customer-price/export', { params, responseType: 'blob' })
  },
  saveCustomerPrice(data: Record<string, any>): Promise<boolean> {
    return request.post('/erp/md/product-price/customer-price/save', data)
  },
  deleteCustomerPrices(ids: string[]): Promise<boolean> {
    return request.post('/erp/md/product-price/customer-price/batch-delete', { ids })
  },
  importCustomerPrices(rows: Record<string, any>[]): Promise<{ success: number; failed: number; messages: string[] }> {
    return request.post('/erp/md/product-price/customer-price/import', { rows })
  },
}
