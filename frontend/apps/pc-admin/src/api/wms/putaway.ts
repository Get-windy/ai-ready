/**
 * WMS 上架 API 模块
 * 后端: PutawayController (/api/wms/putaway)
 */
import request from '@/utils/request'

// ── 上架任务（对齐 WmsPutawayTask 实体） ──────────────
export interface WmsPutawayTask {
  id: number
  taskNo: string
  sourceType: number
  sourceId: number
  sourceOrderNo: string
  warehouseId: number
  warehouseName: string
  totalItems: number
  totalQuantity: number
  putawayQuantity: number
  status: number
  assigneeId: number
  assigneeName: string
  remark: string
  createTime: string
  updateTime: string
}

// ── 上架明细（对齐 WmsPutawayDetail 实体） ────────────
export interface WmsPutawayDetail {
  id: number
  taskId: number
  lineNo: number
  productId: number
  productCode: string
  productName: string
  productSpec: string
  productUnit: string
  quantity: number
  fromLocationId: number
  fromLocationCode: string
  toLocationId: number
  toLocationCode: string
  batchNo: string
  productionDate: string
  validityDate: string
  status: number
  remark: string
}

export const putawayApi = {
  save(data: Partial<WmsPutawayTask>) { return request.post('/wms/putaway/save', data) },
  update(data: Partial<WmsPutawayTask>) { return request.post('/wms/putaway/update', data) },
  getById(id: number) { return request.get(`/wms/putaway/${id}`) },
  page(params: any) { return request.get('/wms/putaway/page', { params }) },
  pageDetail(params: any) { return request.get('/wms/putaway/page-detail', { params }) },
  remove(id: number) { return request.delete(`/wms/putaway/${id}`) },
  /** 开始上架：POST /wms/putaway/start?taskId&userId&userName */
  startPutaway(taskId: number, userId?: number, userName?: string) {
    return request.post('/wms/putaway/start', null, { params: { taskId, userId, userName } })
  },
  /** 确认上架：POST /wms/putaway/confirm?taskId&userId&userName */
  confirmPutaway(taskId: number, userId?: number, userName?: string) {
    return request.post('/wms/putaway/confirm', null, { params: { taskId, userId, userName } })
  },
  /** 上架明细：GET /wms/putaway/details/{taskId} */
  getDetails(taskId: number) { return request.get(`/wms/putaway/details/${taskId}`) },
  /** 明细批量保存（仅待处理，先删后插）：POST /wms/putaway/detail/save，body {taskId, details} */
  saveDetails(taskId: number, details: Partial<WmsPutawayDetail>[]) {
    return request.post('/wms/putaway/detail/save', { taskId, details })
  },
  /** 取消上架：POST /wms/putaway/cancel?taskId */
  cancelPutaway(taskId: number, reason?: string) {
    return request.post('/wms/putaway/cancel', null, { params: { taskId, reason } })
  },
}
