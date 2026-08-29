/**
 * 统一商品默认字段定义
 *
 * 解决 H3: 7个文件有不同的 productDefaults 字段定义问题
 * 所有表单页面应引用此文件的常量，保持字段一致
 */

// ─── 基础字段（所有模块共用） ───
export const PRODUCT_BASE_DEFAULTS = {
  itemCode: '',
  barcode: '',
  specification: '',
  unit: '',
} as const

// ─── 扩展字段（含品牌/产地/型号） ───
export const PRODUCT_EXTEND_DEFAULTS = {
  ...PRODUCT_BASE_DEFAULTS,
  model: '',
  origin: '',
  brand: '',
} as const

// ─── 包装字段（含大小包装） ───
export const PRODUCT_PACK_DEFAULTS = {
  ...PRODUCT_BASE_DEFAULTS,
  bigPack: 0,
  midPack: 0,
  smallPack: 0,
  pieceQuantity: 0,
} as const

// ─── 转换字段（含单位转换） ───
export const PRODUCT_CONVERSION_DEFAULTS = {
  ...PRODUCT_BASE_DEFAULTS,
  smallUnit: '',
  smallUnitQuantity: 0,
  conversionRelation: '',
  conversionResult: 0,
} as const

// ─── 批次字段（含批次/保质期） ───
export const PRODUCT_BATCH_DEFAULTS = {
  ...PRODUCT_BASE_DEFAULTS,
  batchCode: '',
  productionDate: '',
  shelfLife: '',
  expiryDate: '',
} as const

// ─── 库存字段（含可用库存） ───
export const PRODUCT_STOCK_DEFAULTS = {
  ...PRODUCT_BATCH_DEFAULTS,
  locationCode: '',
  availableStock: 0,
  quantity: 0,
  conversionRelation: '',
  pieceQuantity: 0,
} as const

// ─── 销售模块专用（退货/换货/报价） ───
export const PRODUCT_SALES_DEFAULTS = {
  ...PRODUCT_BASE_DEFAULTS,
  batchCode: '',
  conversionRelation: '',
  pieceQuantity: 0,
} as const

// ─── 采购模块专用 ───
export const PRODUCT_PURCHASE_DEFAULTS = {
  ...PRODUCT_EXTEND_DEFAULTS,
  smallUnit: '',
  batchNo: '',
  productionDate: '',
  expiryDate: '',
  quantity: 0,
  unitPrice: 0,
  taxRate: 13,
} as const

// ─── 零售模块专用 ───
export const PRODUCT_RETAIL_DEFAULTS = {
  ...PRODUCT_BASE_DEFAULTS,
  batchCode: '',
  productionDate: '',
  shelfLife: '',
  expiryDate: '',
  bigPack: 0,
  midPack: 0,
  smallPack: 0,
} as const

// ─── WMS 入库/出库/移库专用 ───
export const PRODUCT_WMS_DEFAULTS = {
  ...PRODUCT_STOCK_DEFAULTS,
} as const

// ─── WMS 盘点专用 ───
export const PRODUCT_CHECK_DEFAULTS = {
  ...PRODUCT_BASE_DEFAULTS,
  locationCode: '',
  batchCode: '',
  productionDate: '',
  shelfLife: '',
  expiryDate: '',
  bookQuantity: 0,
  actualQuantity: 0,
  diffQuantity: 0,
  diffAmount: 0,
} as const

// ─── 价格跟踪专用 ───
export const PRODUCT_PRICE_DEFAULTS = {
  ...PRODUCT_EXTEND_DEFAULTS,
  wholesalePrice: 0,
  retailPrice: 0,
  latestPurchaseDate: '',
} as const

// ─── 向后兼容别名 ───
export const productDefaults = PRODUCT_SALES_DEFAULTS
