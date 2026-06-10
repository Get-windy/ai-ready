/**
 * 产品管理 API 模块
 */
import request from '@/utils/request'
import type { PageQuery, PageResult } from '@/api/erp'

// ── 产品分类 ──
export interface ProductCategory {
  id: number
  categoryCode: string
  categoryName: string
  parentId: number
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
  getChildren(parentId: number): Promise<ProductCategory[]> {
    return request.get(`/erp/product-category/children/${parentId}`)
  },
  getById(id: number): Promise<ProductCategory> {
    return request.get(`/erp/product-category/${id}`)
  },
  create(data: Partial<ProductCategory>): Promise<boolean> {
    return request.post('/erp/product-category', data)
  },
  update(id: number, data: Partial<ProductCategory>): Promise<boolean> {
    return request.put(`/erp/product-category/${id}`, data)
  },
  updateSort(id: number, data: Partial<ProductCategory>): Promise<boolean> {
    return request.put(`/erp/product-category/${id}/sort`, data)
  },
  delete(id: number): Promise<boolean> {
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
}

export const productApi = {
  page(params: PageQuery & { categoryId?: number }): Promise<PageResult<Product>> {
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
  /** 批量更新产品价格 */
  batchUpdatePrices(items: { id: number; costPrice?: number; standardPrice?: number; wholesalePrice?: number }[]): Promise<boolean> {
    return request.put('/erp/product/batch-prices', { items })
  },
  /** 产品审批 */
  approval(id: number, action: string): Promise<boolean> {
    return request.put(`/erp/product/${id}/approval`, { action })
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
