/**
 * WMS 发货 API 模块
 */
import request from '@/utils/request'

// ── 发货单 ──────────────────────────────────────────
export interface WmsShipTask {
  id: number
  taskNo: string
  sourceType: number
  sourceNo: string
  sourceId: number
  warehouseId: number
  warehouseName: string
  customerId: number
  customerName: string
  totalQty: number
  shippedQty: number
  status: number
  carrierName: string
  trackingNo: string
  plannedTime: string
  shippedTime: string
  operatorName: string
  remark: string
  createTime: string
  updateTime: string
}

// ── 发货明细 ──────────────────────────────────────────
export interface WmsShipDetail {
  id: number
  taskId: number
  productId: number
  productCode: string
  productName: string
  productSpec: string
  productUnit: string
  expectedQty: number
  shippedQty: number
  locationId: number
  locationCode: string
  batchNo: string
  serialNo: string
  status: number
  remark: string
}

export const shipApi = {
  save(data: Partial<WmsShipTask>) { return request.post('/wms/ship/task/save', data) },
  update(data: Partial<WmsShipTask>) { return request.post('/wms/ship/task/update', data) },
  getById(id: number) { return request.get(`/wms/ship/task/${id}`) },
  page(params: any) { return request.get('/wms/ship/task/page', { params }) },
  remove(id: number) { return request.delete(`/wms/ship/task/${id}`) },
  startShip(id: number) { return request.post(`/wms/ship/task/${id}/start`) },
  scanItem(id: number, detailId: number, serialNo: string) { return request.post(`/wms/ship/task/${id}/scan`, null, { params: { detailId, serialNo } }) },
  confirmShip(id: number) { return request.post(`/wms/ship/task/${id}/confirm`) },
  getDetails(taskId: number) { return request.get(`/wms/ship/task/${taskId}/details`) },
}
