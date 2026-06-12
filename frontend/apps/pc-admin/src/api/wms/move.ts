/**
 * WMS 移库 API 模块
 */
import request from '@/utils/request'

// ── 移库单 ──────────────────────────────────────────
export interface WmsMoveTask {
  id: number
  taskNo: string
  warehouseId: number
  warehouseName: string
  sourceLocationId: number
  sourceLocationCode: string
  targetLocationId: number
  targetLocationCode: string
  totalQty: number
  movedQty: number
  status: number
  moveReason: string
  operatorName: string
  remark: string
  createTime: string
  updateTime: string
}

// ── 移库明细 ──────────────────────────────────────────
export interface WmsMoveDetail {
  id: number
  taskId: number
  productId: number
  productCode: string
  productName: string
  productSpec: string
  productUnit: string
  moveQty: number
  sourceLocationId: number
  sourceLocationCode: string
  targetLocationId: number
  targetLocationCode: string
  batchNo: string
  status: number
  remark: string
}

export const moveApi = {
  save(data: Partial<WmsMoveTask>) { return request.post('/wms/move/save', data) },
  update(data: Partial<WmsMoveTask>) { return request.post('/wms/move/update', data) },
  getById(id: number) { return request.get(`/wms/move/${id}`) },
  page(params: any) { return request.get('/wms/move/page', { params }) },
  remove(id: number) { return request.delete(`/wms/move/${id}`) },
  startMove(id: number) { return request.post(`/wms/move/${id}/start`) },
  executeMove(id: number) { return request.post(`/wms/move/${id}/execute`) },
  getDetails(taskId: number) { return request.get(`/wms/move/${taskId}/details`) },
}
