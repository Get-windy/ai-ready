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
  remark?: string
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
  mallSortType?: string
  mallPoints?: number
  keywords?: string

  /** 等级价格映射：key=等级编码(GRADE_1等)，value=价格 */
  gradePriceMap?: Record<string, number>
  /** 可用库存(Σ 库存可用数量，列表展示) */
  availableStock?: number
  /** 换算关系描述，如 1箱=12袋 */
  conversionRelation?: string
  /** 商品标签数组(由 mallTags 拆分) */
  mallTagList?: string[]
}

// ── 商品授权（屏蔽客户） ──
export interface ProductShield {
  id?: string
  productId?: string
  partnerId?: string
  partnerName?: string
  shieldLevel?: string
  region?: string
  remark?: string
  productName?: string
  productCodeAlias?: string
  imageUrl?: string
  barcode?: string
  spec?: string
  model?: string
  origin?: string
  brand?: string
}

export const productShieldApi = {
  page(params: PageQuery & { keyword?: string; shieldLevel?: string; region?: string; partnerId?: string }): Promise<PageResult<ProductShield>> {
    return request.get('/erp/product-shield/page', params)
  },
  save(data: Partial<ProductShield>): Promise<boolean> {
    return request.post('/erp/product-shield', data)
  },
  batchShield(body: {
    productIds: (string | number)[]
    partnerIds?: (string | number)[]
    partnerNames?: string
    shieldLevel?: string
    region?: string
  }): Promise<number> {
    return request.post('/erp/product-shield/batch-shield', body)
  },
  batchCancel(ids: (string | number)[]): Promise<number> {
    return request.post('/erp/product-shield/batch-cancel', { ids })
  }
}

// ── 套餐（套装） ──
export interface ProductKit {
  id: string
  kitCode?: string
  kitName?: string
  productId?: string
  productCode?: string
  productName?: string
  productSpec?: string
  productUnit?: string
  kitType?: number
  status?: number
  kitPrice?: number
  kitCost?: number
  active?: boolean
  allowSplit?: boolean
  allowPartial?: boolean
  minQuantity?: number
  maxQuantity?: number
  remark?: string
  items?: ProductKitItem[]
}

export interface ProductKitItem {
  id?: string
  kitId?: string
  lineNo?: number
  componentProductId?: string
  componentProductCode?: string
  componentProductName?: string
  componentProductSpec?: string
  componentProductUnit?: string
  quantity?: number
  unitCost?: number
  lineCost?: number
}

export const productKitApi = {
  page(params: PageQuery & { keyword?: string; kitType?: number; status?: number }): Promise<PageResult<ProductKit>> {
    return request.get('/erp/product-kit/page', params)
  },
  getById(id: string): Promise<ProductKit> {
    return request.get(`/erp/product-kit/${id}`)
  },
  getItems(id: string): Promise<ProductKitItem[]> {
    return request.get(`/erp/product-kit/${id}/items`)
  }
}

// ── 云商品库（云导入） ──
export interface CloudProduct {
  id: string
  cloudCode?: string
  cloudName?: string
  spec?: string
  model?: string
  origin?: string
  brand?: string
  unit?: string
  barcode?: string
  categoryName?: string
  industryCategory?: string
  presetPurchasePrice?: number
  retailPrice?: number
  wholesalePrice?: number
  shelfLifeDays?: number
}

export const cloudProductApi = {
  page(params: PageQuery & { keyword?: string; industryCategory?: string }): Promise<PageResult<CloudProduct>> {
    return request.get('/erp/product/cloud-catalog/page', params)
  },
  cloudImport(body: { cloudIds: (string | number)[]; categoryId?: string | number }): Promise<{ imported: number; skipped: number }> {
    return request.post('/erp/product/cloud-import', body)
  }
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
  },
  /** 批量搬移分类 */
  batchMove(ids: (string | number)[], categoryId: string | number): Promise<number> {
    return request.put('/erp/product/batch-move', { ids, categoryId })
  },
  /** 批量修改字段（品牌/行业类别/分类/是否标品/使用优惠券） */
  batchUpdateFields(ids: (string | number)[], fields: Record<string, any>): Promise<number> {
    return request.put('/erp/product/batch-update-fields', { ids, ...fields })
  },
  /** 批量上架/下架（商城） */
  batchShelf(ids: (string | number)[], mallShelfStatus: number): Promise<number> {
    return request.put('/erp/product/batch-shelf', { ids, mallShelfStatus })
  },
  /** 设置商城默认排序方式 */
  setMallSortType(sortType: string): Promise<number> {
    return request.put('/erp/product/set-mall-sort', { sortType })
  },
  /** Excel 导入商品 */
  importFile(file: File): Promise<{ count: number; skipped: number; errors: string[] }> {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/erp/product/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
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
  /** 8个等级价格（标准槽位 GRADE_1..8 对应 grade_price_1..8；界面名称为用户自定义昵称） */
  gradePrice1?: number
  gradePrice2?: number
  gradePrice3?: number
  gradePrice4?: number
  gradePrice5?: number
  gradePrice6?: number
  gradePrice7?: number
  gradePrice8?: number
  /** 重量（kg，单位级） */
  weight?: number
  /** 体积（m³，单位级） */
  volume?: number
}

export const productUnitApi = {
  getByProduct(productId: number): Promise<ProductUnit[]> {
    return request.get(`/erp/product/units/${productId}`)
  },
  create(data: Partial<ProductUnit>): Promise<boolean> {
    return request.post('/erp/product/units', data)
  },
  update(id: number, data: Partial<ProductUnit>): Promise<boolean> {
    return request.put(`/erp/product/units/${id}`, data)
  },
  delete(id: number): Promise<boolean> {
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

/** 商品条码页一行 = 商品 × 单位 */
export interface ProductBarcodeRow {
  /** 单位行ID（雪花ID，字符串避免精度丢失） */
  unitId: string
  productId: string
  imageUrl?: string
  productName: string
  /** 货号 */
  productCode: string
  unitName: string
  conversionRate?: number
  isBaseUnit?: number
  baseUnitName?: string
  /** 换算关系展示文案，如「1箱=12瓶」 */
  conversionRelation?: string
  /** 1=已上架 0=未上架 */
  shelfStatus?: number
  shelfStatusText?: string
  barcode?: string
  barcodeType?: string
  spec?: string
  model?: string
  origin?: string
  createTime?: string
  status?: string
  lastPurchaseDate?: string
}

export interface ProductBarcodeQuery extends PageQuery {
  categoryId?: string | number
  /** 商品名称/货号/条码 */
  keyword?: string
  /** ALL/空=全部，SET=已设置，UNSET=未设置，其它=条码类型 */
  barcodeFilter?: string
  shelfStatus?: number | ''
  status?: string
  /** 新增时间比较符：< = > != <= >=（默认 >=） */
  createTimeOp?: string
  /** 新增时间 */
  createTimeStart?: string
  /** 采购日期比较符：< = > != <= >=（默认 >=） */
  purchaseDateOp?: string
  /** 采购日期 */
  purchaseDateStart?: string
  /** 排序字段（productName / productCode / barcode，白名单） */
  sortField?: string
  /** 排序方向 asc / desc */
  sortOrder?: string
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
  },
  /** 商品条码分页（一行 = 商品 × 单位） */
  page(params: ProductBarcodeQuery): Promise<PageResult<ProductBarcodeRow>> {
    return request.get('/erp/product/barcodes/page', params)
  },
  /** 导出真实 Excel（与查询同一过滤口径） */
  export(params: ProductBarcodeQuery): Promise<Blob> {
    return request.get('/erp/product/barcodes/export', { params, responseType: 'blob' })
  },
  /** 行级修改单位条码（含全局唯一性校验） */
  updateUnitBarcode(unitId: string, data: { barcode?: string; barcodeType?: string }): Promise<boolean> {
    return request.put(`/erp/product/barcodes/unit/${unitId}`, data)
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
  /** 标准槽位编码 TAG_1..TAG_20（商品侧 mall_tags 存的就是它） */
  tagCode?: string
  /** 标签显示名（用户自定义昵称，默认「标签N」） */
  tagName: string
  sortOrder?: number
  /** 状态: 1启用 0停用 */
  status?: number
  /** 对应商品数量 */
  productCount?: number
  /** 对应商品名称聚合串 */
  productNames?: string
}

export const mallTagApi = {
  list(): Promise<MallTag[]> {
    return request.get('/erp/mall-tag/list')
  },
  page(params: { keyword?: string; pageNum?: number; pageSize?: number }): Promise<PageResult<MallTag>> {
    return request.get('/erp/mall-tag/page', params)
  },
  create(data: Partial<MallTag>): Promise<boolean> {
    return request.post('/erp/mall-tag', data)
  },
  update(id: string, data: Partial<MallTag>): Promise<boolean> {
    return request.put(`/erp/mall-tag/${id}`, data)
  },
  /** 启用/停用（对标 Tab3 行内「停用」） */
  updateStatus(id: string, status: number): Promise<boolean> {
    return request.put(`/erp/mall-tag/${id}/status?status=${status}`)
  },
  delete(id: string): Promise<boolean> {
    return request.delete(`/erp/mall-tag/${id}`)
  },
  /** 导出标签（后端真实 xlsx 流） */
  exportFile(params: { keyword?: string } = {}): Promise<Blob> {
    return request.get('/erp/mall-tag/export', { responseType: 'blob', params })
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
  },
  /** 导出品牌（后端真实 xlsx 流） */
  exportFile(params: { keyword?: string } = {}): Promise<Blob> {
    return request.get('/erp/product-brand/export', { responseType: 'blob', params })
  }
}

// ── 商品单位字典 ──
export interface ProductUnitDict {
  id?: string
  unitName: string
  mnemonicCode?: string
  unitType?: string
  conversionRate?: number
  /** 备注（列表列「计量单位备注」） */
  remark?: string
  /** 是否默认单位: 1是 0否 */
  isDefault?: number
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
  },
  /** 导出单位（后端真实 xlsx 流） */
  exportFile(params: { keyword?: string } = {}): Promise<Blob> {
    return request.get('/erp/product-unit-dict/export', { responseType: 'blob', params })
  }
}

// ── 商品单位组（对标 Tab2「单位组管理」：小/中/大单位 + 换算关系） ──
export type ProductUnitGroupItemType = 'SMALL' | 'MEDIUM' | 'LARGE'

export interface ProductUnitGroupItem {
  unitId?: string
  /** 单位类型：SMALL小单位 / MEDIUM中单位 / LARGE大单位 */
  unitType: ProductUnitGroupItemType
  unitName: string
  /** 换算关系（相对小单位的倍数，小单位为 1） */
  conversionRate?: number
  sortOrder?: number
}

export interface ProductUnitGroup {
  id?: string
  status?: number
  /** 单位名称聚合串（对标列「单位」，如 袋,提,箱） */
  unitNames?: string
  /** 换算关系聚合串（对标列「单位关系」，如 1:12:48） */
  unitRates?: string
  items?: ProductUnitGroupItem[]
}

export const productUnitGroupApi = {
  page(params: { keyword?: string; status?: number; pageNum?: number; pageSize?: number }): Promise<PageResult<ProductUnitGroup>> {
    return request.get('/erp/product-unit-group/page', params)
  },
  detail(id: string): Promise<ProductUnitGroup> {
    return request.get(`/erp/product-unit-group/${id}`)
  },
  create(data: Partial<ProductUnitGroup>): Promise<string> {
    return request.post('/erp/product-unit-group', data)
  },
  update(id: string, data: Partial<ProductUnitGroup>): Promise<void> {
    return request.put(`/erp/product-unit-group/${id}`, data)
  },
  delete(id: string): Promise<void> {
    return request.delete(`/erp/product-unit-group/${id}`)
  },
  /** 启用/停用（对标行内「停用 / 启用」） */
  updateStatus(id: string, status: number): Promise<void> {
    return request.put(`/erp/product-unit-group/${id}/status?status=${status}`)
  },
  /** 导出单位组（后端真实 xlsx 流） */
  exportFile(params: { keyword?: string; status?: number } = {}): Promise<Blob> {
    return request.get('/erp/product-unit-group/export', { responseType: 'blob', params })
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
  getById(id: number | string): Promise<ProductFormData> {
    return request.get(`/erp/product/${id}/form`)
  },
  batchUpdate(productId: number | string, data: {
    product: Partial<Product>
    units: any[]
    recommends: any[]
    mallTags?: string[]
  }): Promise<boolean> {
    // 必须走 batch-update：PUT /erp/product/{id} 只更新主表，会丢弃 units/recommends（含单位条码）
    return request.put(`/erp/product/batch-update/${productId}`, data)
  },
  batchCreate(data: {
    product: Partial<Product>
    units: any[]
    recommends: any[]
    mallTags?: string[]
  }): Promise<boolean> {
    // 必须走 batch-create：POST /erp/product 只创建主表，会丢弃 units/recommends
    return request.post('/erp/product/batch-create', data)
  }
}

// ── 商品货位设置（商品 × 仓库 → 推荐货位，对标 ql361 GoodsGPositionList） ──
/** 一行 = 一个商品在所选仓库下的推荐货位状态（未设置时 locationId/locationCode 为空） */
export interface ProductLocationRow {
  id?: string
  productId: string
  productName: string
  /** 货号（后端取商品货号别名，缺失回退商品编码） */
  productCode: string
  imageUrl?: string
  /** 上架状态 1已上架 0未上架（列表显示 √ / ×） */
  shelfStatus?: number
  unit?: string
  barcode?: string
  spec?: string
  model?: string
  origin?: string
  brand?: string
  warehouseId?: string
  warehouseName?: string
  locationId?: string
  locationCode?: string
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
  remark?: string
  modifyTime?: string
}

export interface ProductLocationQueryParams {
  /** 仓库（必填） */
  warehouseId?: string
  categoryId?: string
  keyword?: string
  brand?: string
  locationCode?: string
  /** -1全部 1无条码 2有条码 */
  hasBarcodeStatus?: number
  /** -1全部 1已上架 0未上架 */
  shelfStatus?: number
  /** -1全部 2已启用 1已停用 */
  showStop?: number
  onlyUnsettedGoods?: boolean
  onlyStockGoods?: boolean
  pageNum?: number
  pageSize?: number
}

export const productLocationApi = {
  /** 商品货位设置分页（商品 × 仓库 → 推荐货位） */
  page(params: ProductLocationQueryParams): Promise<PageResult<ProductLocationRow>> {
    return request.get('/erp/product-location/page', params)
  },
  /** 导出真实 Excel（与查询同一过滤口径） */
  export(params: ProductLocationQueryParams): Promise<Blob> {
    return request.get('/erp/product-location/export', { params, responseType: 'blob' })
  },
  /** 设置推荐货位（支持批量，须同一仓库） */
  set(data: { warehouseId: string; productIds: string[]; locationId: string; remark?: string }): Promise<number> {
    return request.post('/erp/product-location/set', data)
  },
  /** 批量移除推荐货位（须同一仓库） */
  batchRemove(data: { warehouseId: string; productIds: string[] }): Promise<number> {
    return request.post('/erp/product-location/batch-remove', data)
  }
}
