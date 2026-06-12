/**
 * WMS 库存 API 模块
 */
import request from '@/utils/request'

// ── 库存 ──────────────────────────────────────────────
export interface WmsInventory {
  id: number
  warehouseId: number
  warehouseName: string
  locationId: number
  locationCode: string
  productId: number
  productCode: string
  productName: string
  productSpec: string
  productUnit: string
  batchNo: string
  productionDate: string
  expiryDate: string
  quantity: number
  availableQty: number
  frozenQty: number
  lockedQty: number
  maxQty: number
  minQty: number
  lastInboundTime: string
  lastOutboundTime: string
  createTime: string
  updateTime: string
}

// ── 库存日志 ──────────────────────────────────────────
export interface WmsInventoryLog {
  id: number
  inventoryId: number
  productId: number
  productCode: string
  productName: string
  warehouseId: number
  warehouseName: string
  locationId: number
  locationCode: string
  batchNo: string
  changeType: number
  changeQty: number
  beforeQty: number
  afterQty: number
  sourceType: string
  sourceNo: string
  operatorName: string
  remark: string
  createTime: string
}

export const inventoryApi = {
  query(params: any) { return request.get('/wms/inventory/query', { params }) },
  page(params: any) { return request.get('/wms/inventory/page', { params }) },
  logByProduct(productId: number) { return request.get(`/wms/inventory/log/product/${productId}`) },
  logPage(params: any) { return request.get('/wms/inventory/log/page', { params }) },
  increase(data: any) { return request.post('/wms/inventory/increase', data) },
  decrease(data: any) { return request.post('/wms/inventory/decrease', data) },
  freeze(id: number, qty: number) { return request.post(`/wms/inventory/${id}/freeze`, null, { params: { qty } }) },
  unfreeze(id: number, qty: number) { return request.post(`/wms/inventory/${id}/unfreeze`, null, { params: { qty } }) },
  move(data: any) { return request.post('/wms/inventory/move', data) },
}
