/**
 * WMS 上架 API 模块
 */
import request from '@/utils/request'

// ── 上架单 ──────────────────────────────────────────
export interface WmsPutawayTask {
  id: number
  taskNo: string
  sourceType: number
  sourceNo: string
  sourceId: number
  warehouseId: number
  warehouseName: string
  locationId: number
  locationCode: string
  totalQty: number
  putawayQty: number
  status: number
  operatorName: string
  remark: string
  createTime: string
  updateTime: string
}

// ── 上架明细 ──────────────────────────────────────────
export interface WmsPutawayDetail {
  id: number
  taskId: number
  productId: number
  productCode: string
  productName: string
  productSpec: string
  productUnit: string
  expectedQty: number
  putawayQty: number
  locationId: number
  locationCode: string
  batchNo: string
  productionDate: string
  expiryDate: string
  status: number
  remark: string
}

export const putawayApi = {
  save(data: Partial<WmsPutawayTask>) { return request.post('/wms/putaway/save', data) },
  update(data: Partial<WmsPutawayTask>) { return request.post('/wms/putaway/update', data) },
  getById(id: number) { return request.get(`/wms/putaway/${id}`) },
  page(params: any) { return request.get('/wms/putaway/page', { params }) },
  remove(id: number) { return request.delete(`/wms/putaway/${id}`) },
  startPutaway(id: number) { return request.post(`/wms/putaway/${id}/start`) },
  confirmPutaway(id: number) { return request.post(`/wms/putaway/${id}/confirm`) },
  getDetails(taskId: number) { return request.get(`/wms/putaway/${taskId}/details`) },
}
