/**
 * WMS 盘点 API 模块
 * 后端: CheckController (/api/wms/check)
 */
import request from '@/utils/request'

// ── 盘点任务（对齐 WmsCheckTask 实体） ────────────────
export interface WmsCheckTask {
  id: number
  taskNo: string
  warehouseId: number
  warehouseName: string
  checkType: number
  scopeType: number
  totalItems: number
  checkedItems: number
  diffItems: number
  status: number
  lockLocation: number
  assigneeId: number
  assigneeName: string
  checkerId: number
  checkerName: string
  approvedBy: number
  approvedTime: string
  remark: string
  createTime: string
  updateTime: string
  /** @deprecated 后端实体字段为 taskNo；为兼容旧页面 wms/check 的模板引用暂时保留，新代码勿用 */
  checkNo?: string
}

// ── 盘点结果（对齐 WmsCheckResult 实体） ──────────────
export interface WmsCheckResult {
  id: number
  taskId: number
  lineNo: number
  productId: number
  productCode: string
  productName: string
  productSpec: string
  productUnit: string
  locationId: number
  locationCode: string
  batchNo: string
  bookQuantity: number
  actualQuantity: number
  diffQuantity: number
  diffType: number
  unitCost: number
  diffAmount: number
  checkStatus: number
  remark: string
}

export const checkApi = {
  save(data: Partial<WmsCheckTask>) { return request.post('/wms/check/save', data) },
  update(data: Partial<WmsCheckTask>) { return request.post('/wms/check/update', data) },
  getById(id: number) { return request.get(`/wms/check/${id}`) },
  page(params: any) { return request.get('/wms/check/page', { params }) },
  remove(id: number) { return request.delete(`/wms/check/${id}`) },
  /** 开始盘点：POST /wms/check/start?taskId&userId&userName */
  startCheck(taskId: number, userId?: number, userName?: string) {
    return request.post('/wms/check/start', null, { params: { taskId, userId, userName } })
  },
  /** 提交盘点结果：POST /wms/check/submit?taskId&userId&userName */
  submitResult(taskId: number, userId?: number, userName?: string) {
    return request.post('/wms/check/submit', null, { params: { taskId, userId, userName } })
  },
  /** 审核盘点：POST /wms/check/approve?taskId&userId */
  approveCheck(taskId: number, userId?: number) {
    return request.post('/wms/check/approve', null, { params: { taskId, userId } })
  },
  /** 盘点结果：GET /wms/check/results/{taskId} */
  getResults(taskId: number) { return request.get(`/wms/check/results/${taskId}`) },
  /** 明细批量保存（仅待盘点，先删后插）：POST /wms/check/detail/save，body {taskId, details} */
  saveDetails(taskId: number, details: Partial<WmsCheckResult>[]) {
    return request.post('/wms/check/detail/save', { taskId, details })
  },
  /** 取消盘点：POST /wms/check/cancel?taskId */
  cancelCheck(taskId: number, reason?: string) {
    return request.post('/wms/check/cancel', null, { params: { taskId, reason } })
  },
}
