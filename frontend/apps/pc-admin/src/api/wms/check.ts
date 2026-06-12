/**
 * WMS 盘点 API 模块
 */
import request from '@/utils/request'

// ── 盘点单 ──────────────────────────────────────────
export interface WmsCheckTask {
  id: number
  checkNo: string
  warehouseId: number
  warehouseName: string
  locationId: number
  locationCode: string
  checkType: number
  totalQty: number
  checkedQty: number
  diffQty: number
  status: number
  operatorName: string
  checkerName: string
  remark: string
  createTime: string
  updateTime: string
}

// ── 盘点结果 ──────────────────────────────────────────
export interface WmsCheckResult {
  id: number
  taskId: number
  productId: number
  productCode: string
  productName: string
  productSpec: string
  productUnit: string
  locationId: number
  locationCode: string
  batchNo: string
  bookQty: number
  actualQty: number
  diffQty: number
  status: number
  remark: string
  checkTime: string
}

export const checkApi = {
  save(data: Partial<WmsCheckTask>) { return request.post('/wms/check/save', data) },
  update(data: Partial<WmsCheckTask>) { return request.post('/wms/check/update', data) },
  getById(id: number) { return request.get(`/wms/check/${id}`) },
  page(params: any) { return request.get('/wms/check/page', { params }) },
  remove(id: number) { return request.delete(`/wms/check/${id}`) },
  startCheck(id: number) { return request.post(`/wms/check/${id}/start`) },
  submitResult(data: Partial<WmsCheckResult>) { return request.post('/wms/check/result/submit', data) },
  approveCheck(id: number) { return request.post(`/wms/check/${id}/approve`) },
  getResults(taskId: number) { return request.get(`/wms/check/${taskId}/results`) },
}
