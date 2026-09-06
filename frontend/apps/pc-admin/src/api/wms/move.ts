/**
 * WMS 移库 API 模块
 * 后端: MoveController (/api/wms/move)
 */
import request from '@/utils/request'

// ── 移库任务（对齐 WmsMoveTask 实体） ─────────────────
export interface WmsMoveTask {
  id: number
  taskNo: string
  warehouseId: number
  warehouseName: string
  fromLocationId: number
  fromLocationCode: string
  toLocationId: number
  toLocationCode: string
  totalItems: number
  totalQuantity: number
  movedQuantity: number
  status: number
  moveType: number
  sourceType: number
  sourceNo: string
  assigneeId: number
  assigneeName: string
  remark: string
  createTime: string
  updateTime: string
}

// ── 移库明细（对齐 WmsMoveDetail 实体） ───────────────
export interface WmsMoveDetail {
  id: number
  taskId: number
  lineNo: number
  productId: number
  productCode: string
  productName: string
  productSpec: string
  productUnit: string
  quantity: number
  batchNo: string
  serialNo: string
  fromLocationId: number
  fromLocationCode: string
  toLocationId: number
  toLocationCode: string
  status: number
  remark: string
}

export const moveApi = {
  save(data: Partial<WmsMoveTask>) { return request.post('/wms/move/save', data) },
  update(data: Partial<WmsMoveTask>) { return request.post('/wms/move/update', data) },
  getById(id: number) { return request.get(`/wms/move/${id}`) },
  page(params: any) { return request.get('/wms/move/page', { params }) },
  /** 按明细分页（明细行 + 单头字段）：GET /wms/move/page-detail */
  pageDetail(params: any) { return request.get('/wms/move/page-detail', { params }) },
  /** 生成移库单号：GET /wms/move/next-no */
  nextNo() { return request.get('/wms/move/next-no') },
  /** 批量删除：DELETE /wms/move/batch，body {ids} */
  batchRemove(ids: number[]) { return request.delete('/wms/move/batch', { data: ids }) },
  remove(id: number) { return request.delete(`/wms/move/${id}`) },
  /** 开始移库：POST /wms/move/start?taskId&userId&userName */
  startMove(taskId: number, userId?: number, userName?: string) {
    return request.post('/wms/move/start', null, { params: { taskId, userId, userName } })
  },
  /** 执行移库：POST /wms/move/execute?taskId&userId&userName */
  executeMove(taskId: number, userId?: number, userName?: string) {
    return request.post('/wms/move/execute', null, { params: { taskId, userId, userName } })
  },
  /** 移库明细：GET /wms/move/details/{taskId} */
  getDetails(taskId: number) { return request.get(`/wms/move/details/${taskId}`) },
  /** 明细批量保存（仅待处理，先删后插）：POST /wms/move/detail/save，body {taskId, details} */
  saveDetails(taskId: number, details: Partial<WmsMoveDetail>[]) {
    return request.post('/wms/move/detail/save', { taskId, details })
  },
  /** 取消移库：POST /wms/move/cancel?taskId */
  cancelMove(taskId: number, reason?: string) {
    return request.post('/wms/move/cancel', null, { params: { taskId, reason } })
  },
}
