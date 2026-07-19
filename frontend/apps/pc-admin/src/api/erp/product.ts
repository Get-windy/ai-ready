/**
 * 产品管理 API 模块
 */
import request from '@/utils/request'
import type { PageQuery, PageResult } from '@/api/erp'

// ── 产品分类 ──
export interface ProductCategory {
  id: string
  categoryCode: string
  categoryName: string
  parentId: string
  categoryLevel: number
  sortOrder: number
  status: number
  children?: ProductCategory[]
  productCount?: number
}

export const productCategoryApi = {
  getTree(): Promise<ProductCategory[]> {
    return request.get('/erp/product-category/tree')
  },
  getChildren(parentId: string): Promise<ProductCategory[]> {
    return request.get(`/erp/product-category/children/${parentId}`)
  },
  getById(id: string): Promise<ProductCategory> {
    return request.get(`/erp/product-category/${id}`)
  },
  create(data: Partial<ProductCategory>): Promise<boolean> {
    return request.post('/erp/product-category', data)
  },
  update(id: string, data: Partial<ProductCategory>): Promise<boolean> {
    return request.put(`/erp/product-category/${id}`, data)
  },
  updateSort(id: string, data: Partial<ProductCategory>): Promise<boolean> {
    return request.put(`/erp/product-category/${id}/sort`, data)
  },
  delete(id: string): Promise<boolean> {
    return request.delete(`/erp/product-category/${id}`)
  }
}

// ── 产品等级 ──
export interface ProductGrade {
  id: number
  gradeCode: string
  gradeName: string
  gradeLevel: number
  sortOrder: number
  status: number
}

export const productGradeApi = {
  list(): Promise<ProductGrade[]> {
    return request.get('/erp/product-grade/list')
  },
  getById(id: number): Promise<ProductGrade> {
    return request.get(`/erp/product-grade/${id}`)
  },
  create(data: Partial<ProductGrade>): Promise<boolean> {
    return request.post('/erp/product-grade', data)
  },
  update(id: number, data: Partial<ProductGrade>): Promise<boolean> {
    return request.put(`/erp/product-grade/${id}`, data)
  },
  delete(id: number): Promise<boolean> {
    return request.delete(`/erp/product-grade/${id}`)
  }
}

// ── 产品主表 ──
export interface Product {
  id: number
  productCode: string
  productName: string
  spec: string
  unit: string
  categoryId: number
  categoryName: string
  productGradeId: number
  gradeName: string
  costPrice: number
  standardPrice: number
  wholesalePrice: number
  imageUrl: string
  barcode: string
  sku?: string
  productType: string
  hasGradePrice: number
  status: string
  remark: string
  createTime: string

  // 扩展字段(V3.0.0)
  weight?: number
  volume?: number
  origin?: string
  brand?: string
  taxRate?: number
  purchasePrice?: number
  retailPrice?: number
  shelfLifeDays?: number
  isBatchManaged?: number
  isSerialManaged?: number
  approvalStatus?: string
  approvalBy?: number
  approvalTime?: string

  // 商品表单扩展字段
  productCodeAlias?: string
  model?: string
  industryCategory?: string
  nearExpiryDays?: number
  isBatchExpiryManaged?: number
  isStandardProduct?: number
  useCoupon?: number
  defaultSalesUnitId?: number
  defaultPurchaseUnitId?: number
  defaultStockUnitId?: number
  mallDisplayTitle?: string
  mallDescription?: string
  mallTags?: string
  mallShelfStatus?: number
  mallSortOrder?: number
  mallMinOrderQty?: number
  mallPurchaseLimit?: number
  richTextDetail?: string
  videoUrl?: string

  /** 等级价格映射：key=等级编码(GRADE_1等)，value=价格 */
  gradePriceMap?: Record<string, number>
}

export const productApi = {
  page(params: PageQuery & {
    categoryId?: number
    brand?: string
    industryCategory?: string
    createTimeStart?: string
    createTimeEnd?: string
    useCoupon?: number
    isStandardProduct?: number
    productType?: string
    mallShelfStatus?: number
  }): Promise<PageResult<Product>> {
    return request.get('/erp/product/page', params)
  },
  getById(id: number): Promise<Product> {
    return request.get(`/erp/product/${id}`)
  },
  getByCode(code: string): Promise<Product> {
    return request.get(`/erp/product/by-code/${code}`)
  },
  create(data: Partial<Product>): Promise<boolean> {
    return request.post('/erp/product', data)
  },
  update(id: number, data: Partial<Product>): Promise<boolean> {
    return request.put(`/erp/product/${id}`, data)
  },
  updateStatus(id: number, status: string): Promise<boolean> {
    return request.put(`/erp/product/${id}/status`, null, { params: { status } })
  },
  delete(id: number): Promise<boolean> {
    return request.delete(`/erp/product/${id}`)
  },
  /** 批量更新商品价格 */
  batchUpdatePrices(items: { id: number; costPrice?: number; standardPrice?: number; wholesalePrice?: number }[]): Promise<boolean> {
    return request.put('/erp/product/batch-prices', { items })
  },
  /** 批量更新商品状态 */
  batchUpdateStatus(ids: number[], status: string): Promise<boolean> {
    return request.put('/erp/product/batch-status', { ids, status })
  },
  /** 批量删除商品 */
  batchDelete(ids: number[]): Promise<boolean> {
    return request.put('/erp/product/batch-delete', { ids })
  },
  /** 产品审批 */
  approval(id: number, action: string): Promise<boolean> {
    return request.put(`/erp/product/${id}/approval`, { action })
  },
  /** 获取行业类别选项 */
  getIndustryCategories(): Promise<string[]> {
    return request.get('/erp/product/industry-categories')
  },
  /** 获取品牌选项 */
  getBrands(): Promise<string[]> {
    return request.get('/erp/product/brands')
  }
}

// ── 产品属性定义 ──
export interface ProductAttributeDef {
  id: number
  attrName: string
  attrType: string
  sortOrder: number
  status: number
  remark: string
}

export interface ProductAttributeOption {
  id: number
  attrDefId: number
  optionValue: string
  optionLabel: string
  colorHex: string
  sortOrder: number
}

export interface ProductAttributeValue {
  id: number
  productId: number
  attrDefId: number
  attrValue: string
  sortOrder: number
  attrName: string
  attrType: string
}

export const productAttributeApi = {
  getDefs(): Promise<ProductAttributeDef[]> {
    return request.get('/erp/product/attributes/defs')
  },
  createDef(data: Partial<ProductAttributeDef>): Promise<boolean> {
    return request.post('/erp/product/attributes/defs', data)
  },
  updateDef(id: number, data: Partial<ProductAttributeDef>): Promise<boolean> {
    return request.put(`/erp/product/attributes/defs/${id}`, data)
  },
  deleteDef(id: number): Promise<boolean> {
    return request.delete(`/erp/product/attributes/defs/${id}`)
  },
  getOptions(defId: number): Promise<ProductAttributeOption[]> {
    return request.get(`/erp/product/attributes/defs/${defId}/options`)
  },
  createOption(data: Partial<ProductAttributeOption>): Promise<boolean> {
    return request.post('/erp/product/attributes/options', data)
  },
  updateOption(id: number, data: Partial<ProductAttributeOption>): Promise<boolean> {
    return request.put(`/erp/product/attributes/options/${id}`, data)
  },
  deleteOption(id: number): Promise<boolean> {
    return request.delete(`/erp/product/attributes/options/${id}`)
  },
  getValues(productId: number): Promise<ProductAttributeValue[]> {
    return request.get(`/erp/product/attributes/values/${productId}`)
  },
  saveValues(productId: number, values: Partial<ProductAttributeValue>[]): Promise<boolean> {
    return request.put(`/erp/product/attributes/values/${productId}`, values)
  }
}

// ── 产品多单位 ──
export interface ProductUnit {
  id: number
  productId: number
  unitName: string
  isBaseUnit: number
  conversionRate: number
  barcode: string
  sortOrder: number
  unitType?: string
  presetPurchasePrice?: number
  referenceCost?: number
  recentPurchasePrice?: number
  wholesalePrice?: number
  retailPrice?: number
  minSalePrice?: number
  minDiscount?: number
  /** 8个等级价格（与erp_product_grade对应，用户可自定义等级名称） */
  gradePrice1?: number
  gradePrice2?: number
  gradePrice3?: number
  gradePrice4?: number
  gradePrice5?: number
  gradePrice6?: number
  gradePrice7?: number
  gradePrice8?: number
}

export const productUnitApi = {
  getByProduct(productId: string): Promise<ProductUnit[]> {
    return request.get(`/erp/product/units/${productId}`)
  },
  create(data: Partial<ProductUnit>): Promise<boolean> {
    return request.post('/erp/product/units', data)
  },
  update(id: string, data: Partial<ProductUnit>): Promise<boolean> {
    return request.put(`/erp/product/units/${id}`, data)
  },
  delete(id: string): Promise<boolean> {
    return request.delete(`/erp/product/units/${id}`)
  }
}

// ── 产品条形码 ──
export interface ProductBarcode {
  id: number
  productId: number
  barcode: string
  barcodeType: string
  isDefault: number
  unitId: number
  unitName: string
}

export const productBarcodeApi = {
  getByProduct(productId: number): Promise<ProductBarcode[]> {
    return request.get(`/erp/product/barcodes/${productId}`)
  },
  create(data: Partial<ProductBarcode>): Promise<boolean> {
    return request.post('/erp/product/barcodes', data)
  },
  update(id: number, data: Partial<ProductBarcode>): Promise<boolean> {
    return request.put(`/erp/product/barcodes/${id}`, data)
  },
  delete(id: number): Promise<boolean> {
    return request.delete(`/erp/product/barcodes/${id}`)
  }
}

// ── 产品附件 ──
export interface ProductAttachment {
  id: number
  productId: number
  fileName: string
  fileUrl: string
  fileSize: number
  fileType: string
  category: string
  sortOrder: number
}

export const productAttachmentApi = {
  getByProduct(productId: number): Promise<ProductAttachment[]> {
    return request.get(`/erp/product/attachments/${productId}`)
  },
  create(data: Partial<ProductAttachment>): Promise<boolean> {
    return request.post('/erp/product/attachments', data)
  },
  delete(id: number): Promise<boolean> {
    return request.delete(`/erp/product/attachments/${id}`)
  }
}

// ── 产品关联 ──
export interface ProductRelated {
  id: number
  productId: number
  relatedProductId: number
  relationType: string
  sortOrder: number
  remark: string
  relatedProductCode: string
  relatedProductName: string
  relatedProductSpec: string
}

export const productRelatedApi = {
  getByProduct(productId: number): Promise<ProductRelated[]> {
    return request.get(`/erp/product/related/${productId}`)
  },
  create(data: Partial<ProductRelated>): Promise<boolean> {
    return request.post('/erp/product/related', data)
  },
  delete(id: number): Promise<boolean> {
    return request.delete(`/erp/product/related/${id}`)
  }
}

// ── SKU生成规则 ──
export interface ProductSkuRule {
  id: number
  ruleName: string
  ruleFormat: string
  separator: string
  seqLength: number
  seqStart: number
  isDefault: number
  status: number
}

export const productSkuRuleApi = {
  list(): Promise<ProductSkuRule[]> {
    return request.get('/erp/product/sku-rules')
  },
  create(data: Partial<ProductSkuRule>): Promise<boolean> {
    return request.post('/erp/product/sku-rules', data)
  },
  update(id: number, data: Partial<ProductSkuRule>): Promise<boolean> {
    return request.put(`/erp/product/sku-rules/${id}`, data)
  },
  delete(id: number): Promise<boolean> {
    return request.delete(`/erp/product/sku-rules/${id}`)
  },
  setDefault(id: number): Promise<boolean> {
    return request.put(`/erp/product/sku-rules/${id}/set-default`)
  },
  generateSku(ruleId: number, productId: number): Promise<string> {
    return request.post('/erp/product/sku-rules/generate', null, { params: { ruleId, productId } })
  }
}

// ── 库存管理模式 ──
export type InventoryMode = 'BATCH' | 'SERIAL' | 'SKU'

export interface ModeOption {
  value: InventoryMode
  label: string
  description: string
}

export const inventoryModeApi = {
  /** 获取当前租户的库存管理模式 */
  get(): Promise<InventoryMode> {
    return request.get('/erp/inventory-mode')
  },
  /** 设置库存管理模式 */
  set(mode: InventoryMode): Promise<boolean> {
    return request.put('/erp/inventory-mode', { mode })
  },
  /** 获取所有支持的模式选项 */
  options(): Promise<ModeOption[]> {
    return request.get('/erp/inventory-mode/options')
  }
}

// ── 产品等级价格 ──
export interface ProductGradePrice {
  id: number
  productId: number
  productGradeId: number
  price: number
  minOrderQty: number
  isActive: number
  gradeName: string
  gradeCode: string
}

export const productGradePriceApi = {
  getByProduct(productId: number): Promise<ProductGradePrice[]> {
    return request.get(`/erp/product-grade-price/by-product/${productId}`)
  },
  batchSave(productId: number, data: Partial<ProductGradePrice>[]): Promise<boolean> {
    return request.post(`/erp/product-grade-price/batch-save?productId=${productId}`, data)
  },
  create(data: Partial<ProductGradePrice>): Promise<boolean> {
    return request.post('/erp/product-grade-price', data)
  },
  update(id: number, data: Partial<ProductGradePrice>): Promise<boolean> {
    return request.put(`/erp/product-grade-price/${id}`, data)
  },
  delete(id: number): Promise<boolean> {
    return request.delete(`/erp/product-grade-price/${id}`)
  }
}

// ── 产品推荐 ──
export interface ProductRecommend {
  id?: number
  productId?: number
  recommendProductId: number
  recommendProductName?: string
  recommendProductCode?: string
  recommendProductSpec?: string
  recommendProductUnit?: string
  recommendProductOrigin?: string
  recommendProductBrand?: string
  sortOrder?: number
}

export const productRecommendApi = {
  list(productId: number): Promise<ProductRecommend[]> {
    return request.get(`/erp/product/recommends/${productId}`)
  },
  create(data: Partial<ProductRecommend>): Promise<boolean> {
    return request.post('/erp/product/recommends', data)
  }
}

// ── 商城标签 ──
export interface MallTag {
  id?: string
  tagName: string
  sortOrder?: number
}

export const mallTagApi = {
  list(): Promise<MallTag[]> {
    return request.get('/erp/mall-tag/list')
  },
  create(data: Partial<MallTag>): Promise<boolean> {
    return request.post('/erp/mall-tag', data)
  },
  update(id: string, data: Partial<MallTag>): Promise<boolean> {
    return request.put(`/erp/mall-tag/${id}`, data)
  },
  delete(id: string): Promise<boolean> {
    return request.delete(`/erp/mall-tag/${id}`)
  }
}

// ── 商品品牌 ──
export interface ProductBrand {
  id?: string
  brandName: string
  mnemonicCode?: string
  remark?: string
  sortOrder?: number
  status?: number
}

export const productBrandApi = {
  page(params: { keyword?: string; pageNum?: number; pageSize?: number }): Promise<PageResult<ProductBrand>> {
    return request.get('/erp/product-brand/page', params)
  },
  list(): Promise<ProductBrand[]> {
    return request.get('/erp/product-brand/list')
  },
  create(data: Partial<ProductBrand>): Promise<boolean> {
    return request.post('/erp/product-brand', data)
  },
  update(id: string, data: Partial<ProductBrand>): Promise<boolean> {
    return request.put(`/erp/product-brand/${id}`, data)
  },
  delete(id: string): Promise<boolean> {
    return request.delete(`/erp/product-brand/${id}`)
  }
}

// ── 商品单位字典 ──
export interface ProductUnitDict {
  id?: string
  unitName: string
  mnemonicCode?: string
  unitType?: string
  conversionRate?: number
  remark?: string
  sortOrder?: number
  status?: number
}

export const productUnitDictApi = {
  page(params: { keyword?: string; pageNum?: number; pageSize?: number }): Promise<PageResult<ProductUnitDict>> {
    return request.get('/erp/product-unit-dict/page', params)
  },
  list(): Promise<ProductUnitDict[]> {
    return request.get('/erp/product-unit-dict/list')
  },
  create(data: Partial<ProductUnitDict>): Promise<boolean> {
    return request.post('/erp/product-unit-dict', data)
  },
  update(id: string, data: Partial<ProductUnitDict>): Promise<boolean> {
    return request.put(`/erp/product-unit-dict/${id}`, data)
  },
  delete(id: string): Promise<boolean> {
    return request.delete(`/erp/product-unit-dict/${id}`)
  }
}

// ── 产品表单（含附属数据） ──
export interface ProductFormData {
  product: Product
  units: ProductUnit[]
  recommends: ProductRecommend[]
  mallTags: MallTag[]
}

export const productFormApi = {
  getById(id: number): Promise<ProductFormData> {
    return request.get(`/erp/product/${id}/form`)
  },
  batchUpdate(productId: number, data: {
    product: Partial<Product>
    units: any[]
    recommends: any[]
    mallTags?: string[]
  }): Promise<boolean> {
    return request.put(`/erp/product/${productId}`, data)
  },
  batchCreate(data: {
    product: Partial<Product>
    units: any[]
    recommends: any[]
    mallTags?: string[]
  }): Promise<boolean> {
    return request.post('/erp/product', data)
  }
}
