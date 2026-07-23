/**
 * WMS 收货 API 模块
 * 后端: ReceiptController (/api/wms/receipt)
 */
import request from '@/utils/request'

// ── 收货任务（对齐 WmsReceiptTask 实体） ──────────────
export interface WmsReceiptTask {
  id: number
  taskNo: string
  sourceType: number
  sourceOrderId: number
  sourceOrderNo: string
  warehouseId: number
  warehouseName: string
  supplierId: number
  supplierName: string
  totalItems: number
  totalQuantity: number
  receivedQuantity: number
  status: number
  priority: number
  expectedTime: string
  completedTime: string
  assigneeId: number
  assigneeName: string
  remark: string
  createTime: string
  updateTime: string
}

// ── 收货明细（对齐 WmsReceiptDetail 实体） ────────────
export interface WmsReceiptDetail {
  id: number
  taskId: number
  lineNo: number
  productId: number
  productCode: string
  productName: string
  productSpec: string
  productUnit: string
  expectedQuantity: number
  receivedQuantity: number
  putawayQuantity: number
  locationId: number
  locationCode: string
  batchNo: string
  productionDate: string
  validityDate: string
  status: number
  remark: string
}

export const receiptApi = {
  save(data: Partial<WmsReceiptTask>) { return request.post('/wms/receipt/save', data) },
  update(data: Partial<WmsReceiptTask>) { return request.post('/wms/receipt/update', data) },
  getById(id: number) { return request.get(`/wms/receipt/${id}`) },
  page(params: any) { return request.get('/wms/receipt/page', { params }) },
  remove(id: number) { return request.delete(`/wms/receipt/${id}`) },
  /** 开始收货：POST /wms/receipt/start?taskId&userId&userName */
  startReceipt(taskId: number, userId?: number, userName?: string) {
    return request.post('/wms/receipt/start', null, { params: { taskId, userId, userName } })
  },
  /** 确认收货：POST /wms/receipt/confirm?taskId&userId&userName */
  confirmReceipt(taskId: number, userId?: number, userName?: string) {
    return request.post('/wms/receipt/confirm', null, { params: { taskId, userId, userName } })
  },
  /** 取消收货：POST /wms/receipt/cancel?taskId&reason */
  cancelReceipt(taskId: number, reason?: string) {
    return request.post('/wms/receipt/cancel', null, { params: { taskId, reason: reason || 'PC端取消' } })
  },
  /** 收货明细：GET /wms/receipt/details/{taskId} */
  getDetails(taskId: number) { return request.get(`/wms/receipt/details/${taskId}`) },
  /** 明细批量保存（仅待处理，先删后插）：POST /wms/receipt/detail/save，body {taskId, details} */
  saveDetails(taskId: number, details: Partial<WmsReceiptDetail>[]) {
    return request.post('/wms/receipt/detail/save', { taskId, details })
  },
}
