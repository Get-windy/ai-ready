/**
 * WMS 库存 API 模块
 * 后端: InventoryController (/api/wms/inventory)
 */
import request from '@/utils/request'

// ── 库存（对齐 WmsInventory 实体） ────────────────────
export interface WmsInventory {
  id: number
  productId: number
  productCode: string
  productName: string
  productSpec: string
  productUnit: string
  warehouseId: number
  warehouseName: string
  locationId: number
  locationCode: string
  batchNo: string
  serialNo: string
  quantity: number
  availableQuantity: number
  frozenQuantity: number
  unitCost: number
  supplierId: number
  supplierName: string
  productionDate: string
  validityDate: string
  remark: string
  createTime: string
  updateTime: string
}

// ── 库存日志（对齐 WmsInventoryLog 实体） ─────────────
export interface WmsInventoryLog {
  id: number
  traceId: string
  productId: number
  productCode: string
  productName: string
  warehouseId: number
  warehouseName: string
  locationId: number
  locationCode: string
  batchNo: string
  serialNo: string
  changeType: number
  direction: number
  quantity: number
  beforeQuantity: number
  afterQuantity: number
  sourceType: string
  sourceId: number
  sourceNo: string
  operatorId: number
  operatorName: string
  remark: string
  createTime: string
}

export const inventoryApi = {
  query(params: any) { return request.get('/wms/inventory/query', { params }) },
  page(params: any) { return request.get('/wms/inventory/page', { params }) },
  /** 商品库存流水：后端无单独 by-product 端点，用 /log/page + productId 过滤，返回 records 数组 */
  async logByProduct(productId: number) {
    const res: any = await request.get('/wms/inventory/log/page', { params: { productId, current: 1, size: 100 } })
    return res?.records || []
  },
  logPage(params: any) { return request.get('/wms/inventory/log/page', { params }) },
  increase(data: any) { return request.post('/wms/inventory/increase', data) },
  decrease(data: any) { return request.post('/wms/inventory/decrease', data) },
  freeze(data: any) { return request.post('/wms/inventory/freeze', data) },
  unfreeze(data: any) { return request.post('/wms/inventory/unfreeze', data) },
  move(data: any) { return request.post('/wms/inventory/move', data) },
}
