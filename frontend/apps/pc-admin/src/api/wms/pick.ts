/**
 * WMS 拣货/波次 API 模块
 * 后端: PickController (/api/wms/pick)
 */
import request from '@/utils/request'

// ── 波次（对齐 WmsPickWave 实体） ─────────────────────
export interface WmsPickWave {
  id: number
  waveNo: string
  warehouseId: number
  warehouseName: string
  orderCount: number
  itemCount: number
  totalQuantity: number
  pickedQuantity: number
  status: number
  priority: number
  waveType: number
  assigneeId: number
  assigneeName: string
  remark: string
  createTime: string
  updateTime: string
}

// ── 拣货任务（对齐 WmsPickTask 实体） ─────────────────
export interface WmsPickTask {
  id: number
  taskNo: string
  waveId: number
  sourceType: number
  sourceOrderId: number
  sourceOrderNo: string
  warehouseId: number
  warehouseName: string
  customerId: number
  customerName: string
  totalItems: number
  totalQuantity: number
  pickedQuantity: number
  status: number
  priority: number
  assigneeId: number
  assigneeName: string
  expectShipTime: string
  remark: string
  createTime: string
  updateTime: string
}

// ── 拣货明细（对齐 WmsPickDetail 实体） ───────────────
export interface WmsPickDetail {
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
  expectedQuantity: number
  pickedQuantity: number
  shortageQuantity: number
  batchNo: string
  serialNo: string
  status: number
  remark: string
}

export const pickApi = {
  // 波次
  waveSave(data: Partial<WmsPickWave>) { return request.post('/wms/pick/wave/save', data) },
  waveUpdate(data: Partial<WmsPickWave>) { return request.post('/wms/pick/wave/update', data) },
  waveGetById(id: number) { return request.get(`/wms/pick/wave/${id}`) },
  wavePage(params: any) { return request.get('/wms/pick/wave/page', { params }) },
  waveRemove(id: number) { return request.delete(`/wms/pick/wave/${id}`) },
  /** 从销售订单创建波次：POST /wms/pick/wave/create，body 为销售订单ID数组 */
  waveCreate(saleOrderIds: number[]) { return request.post('/wms/pick/wave/create', saleOrderIds) },

  // 拣货任务
  taskSave(data: Partial<WmsPickTask>) { return request.post('/wms/pick/task/save', data) },
  taskUpdate(data: Partial<WmsPickTask>) { return request.post('/wms/pick/task/update', data) },
  taskGetById(id: number) { return request.get(`/wms/pick/task/${id}`) },
  taskPage(params: any) { return request.get('/wms/pick/task/page', { params }) },
  taskRemove(id: number) { return request.delete(`/wms/pick/task/${id}`) },
  /** 开始拣货：POST /wms/pick/task/start?taskId&userId&userName */
  taskStart(taskId: number, userId?: number, userName?: string) {
    return request.post('/wms/pick/task/start', null, { params: { taskId, userId, userName } })
  },
  /** 完成拣货：POST /wms/pick/task/complete?taskId */
  taskComplete(taskId: number) {
    return request.post('/wms/pick/task/complete', null, { params: { taskId } })
  },
  taskListByWave(waveId: number) { return request.get(`/wms/pick/task/list-by-wave/${waveId}`) },

  // 拣货明细
  /** 确认拣货明细：POST /wms/pick/detail/confirm?detailId&pickedQuantity */
  detailConfirm(detailId: number, pickedQuantity: number) {
    return request.post('/wms/pick/detail/confirm', null, { params: { detailId, pickedQuantity } })
  },
  /** 标记缺货：POST /wms/pick/detail/shortage?detailId&shortageQuantity */
  detailShortage(detailId: number, shortageQuantity: number) {
    return request.post('/wms/pick/detail/shortage', null, { params: { detailId, shortageQuantity } })
  },
  detailList(taskId: number) { return request.get(`/wms/pick/detail/list/${taskId}`) },
  /** 明细批量保存（仅待处理，先删后插）：POST /wms/pick/detail/save，body {taskId, details} */
  saveDetails(taskId: number, details: Partial<WmsPickDetail>[]) {
    return request.post('/wms/pick/detail/save', { taskId, details })
  },
  /** 取消拣货任务：POST /wms/pick/task/cancel?taskId */
  taskCancel(taskId: number, reason?: string) {
    return request.post('/wms/pick/task/cancel', null, { params: { taskId, reason } })
  },

  // ── 金标准查询 / 编号 ──
  /** 生成下一拣货单号：GET /wms/pick/next-no */
  nextNo() { return request.get('/wms/pick/next-no') },
  /** 多条件分页查询拣货单(按单据)：GET /wms/pick/doc-query */
  docQuery(params: any) { return request.get('/wms/pick/doc-query', { params }) },
  /** 分页查询拣货明细(按明细)：GET /wms/pick/page-detail */
  pageDetail(params: any) { return request.get('/wms/pick/page-detail', { params }) },
}
