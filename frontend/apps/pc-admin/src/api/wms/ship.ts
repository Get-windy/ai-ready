/**
 * WMS 发货 API 模块
 * 后端: ShipController (/api/wms/ship)
 */
import request from '@/utils/request'

// ── 发货任务（对齐 WmsShipTask 实体） ─────────────────
export interface WmsShipTask {
  id: number
  taskNo: string
  pickTaskId: number
  sourceOrderId: number
  sourceOrderNo: string
  warehouseId: number
  warehouseName: string
  customerId: number
  customerName: string
  totalItems: number
  totalQuantity: number
  scannedQuantity: number
  status: number
  assigneeId: number
  assigneeName: string
  carrierName: string
  trackingNo: string
  shipTime: string
  remark: string
  createTime: string
  updateTime: string
}

// ── 发货明细（对齐 WmsShipDetail 实体） ───────────────
export interface WmsShipDetail {
  id: number
  shipId: number
  lineNo: number
  productId: number
  productCode: string
  productName: string
  productSpec: string
  productUnit: string
  expectedQuantity: number
  scannedQuantity: number
  confirmedQuantity: number
  batchNo: string
  serialNo: string
  locationId: number
  locationCode: string
  status: number
  remark: string
}

export const shipApi = {
  save(data: Partial<WmsShipTask>) { return request.post('/wms/ship/task/save', data) },
  update(data: Partial<WmsShipTask>) { return request.post('/wms/ship/task/update', data) },
  getById(id: number) { return request.get(`/wms/ship/task/${id}`) },
  page(params: any) { return request.get('/wms/ship/task/page', { params }) },
  /** 按单据多条件分页：GET /wms/ship/task/query（pageNum/pageSize + 关键字/仓库/客户/承运商/状态/日期范围） */
  queryPage(params: any) { return request.get('/wms/ship/task/query', { params }) },
  /** 按明细分页：GET /wms/ship/task/page-detail（pageNum/pageSize + 商品名/编码/单号/状态/日期范围） */
  pageDetail(params: any) { return request.get('/wms/ship/task/page-detail', { params }) },
  remove(id: number) { return request.delete(`/wms/ship/task/${id}`) },
  /** 开始发货：POST /wms/ship/start?taskId&userId&userName */
  startShip(taskId: number, userId?: number, userName?: string) {
    return request.post('/wms/ship/start', null, { params: { taskId, userId, userName } })
  },
  /** 扫描发货商品：POST /wms/ship/scan?detailId&scannedQuantity */
  scanItem(detailId: number, scannedQuantity: number) {
    return request.post('/wms/ship/scan', null, { params: { detailId, scannedQuantity } })
  },
  /** 确认发货：POST /wms/ship/confirm?taskId */
  confirmShip(taskId: number) {
    return request.post('/wms/ship/confirm', null, { params: { taskId } })
  },
  /** 发货明细：GET /wms/ship/details/{shipId} */
  getDetails(shipId: number) { return request.get(`/wms/ship/details/${shipId}`) },
  /** 明细批量保存（仅待处理，先删后插）：POST /wms/ship/detail/save，body {taskId(=shipId), details} */
  saveDetails(shipId: number, details: Partial<WmsShipDetail>[]) {
    return request.post('/wms/ship/detail/save', { taskId: shipId, details })
  },
  /** 取消发货：POST /wms/ship/cancel?taskId */
  cancelShip(taskId: number, reason?: string) {
    return request.post('/wms/ship/cancel', null, { params: { taskId, reason } })
  },
}
