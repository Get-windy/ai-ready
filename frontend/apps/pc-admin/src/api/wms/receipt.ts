/**
 * WMS 收货 API 模块
 */
import request from '@/utils/request'

// ── 收货单 ──────────────────────────────────────────
export interface WmsReceiptTask {
  id: number
  taskNo: string
  sourceType: number
  sourceNo: string
  sourceId: number
  warehouseId: number
  warehouseName: string
  supplierId: number
  supplierName: string
  expectedQty: number
  receivedQty: number
  status: number
  plannedTime: string
  receivedTime: string
  operatorName: string
  remark: string
  createTime: string
  updateTime: string
}

// ── 收货明细 ──────────────────────────────────────────
export interface WmsReceiptDetail {
  id: number
  taskId: number
  productId: number
  productCode: string
  productName: string
  productSpec: string
  productUnit: string
  expectedQty: number
  receivedQty: number
  locationId: number
  locationCode: string
  batchNo: string
  productionDate: string
  expiryDate: string
  status: number
  remark: string
}

export const receiptApi = {
  save(data: Partial<WmsReceiptTask>) { return request.post('/wms/receipt/save', data) },
  update(data: Partial<WmsReceiptTask>) { return request.post('/wms/receipt/update', data) },
  getById(id: number) { return request.get(`/wms/receipt/${id}`) },
  page(params: any) { return request.get('/wms/receipt/page', { params }) },
  remove(id: number) { return request.delete(`/wms/receipt/${id}`) },
  startReceipt(id: number) { return request.post(`/wms/receipt/${id}/start`) },
  confirmReceipt(id: number) { return request.post(`/wms/receipt/${id}/confirm`) },
  cancelReceipt(id: number) { return request.post(`/wms/receipt/${id}/cancel`) },
  getDetails(taskId: number) { return request.get(`/wms/receipt/${taskId}/details`) },
}
