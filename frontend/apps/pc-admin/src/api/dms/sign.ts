/**
 * DMS 签收 API 模块（配送 → 配送跟踪 → 签收管理，菜单 80900 `dms:sign`）
 *
 * 后端：`SignController`（`/api/dms/sign`）——台账分页 / 审核流转 / 批量审核 / 统计 / 真实 xlsx 导出。
 * 口径见《签收管理开发文档》§3.5。
 */
import request from '@/utils/request'

/** 签收台账行（后端 SignVO） */
export interface DmsSignRecord {
  id: number
  taskId: number
  /** 任务编号（联查 dms_task.task_no） */
  taskNo?: string
  taskStatus?: number
  taskStatusText?: string
  orderNo?: string
  sourceBillNo?: string
  riderId?: number
  riderName?: string
  riderPhone?: string
  vehicleName?: string
  customerId?: number
  customerName?: string
  customerPhone?: string
  /** 1-正常签收 2-部分签收 3-拒收 */
  signType: number
  signTypeText?: string
  /** 实际签收数量（部分签收） */
  actualQuantity?: number
  /** 应签收数量快照 */
  plannedQuantity?: number
  photoUrls?: string
  photoCount?: number
  signatureUrl?: string
  hasSignature?: boolean
  signLat?: number
  signLng?: number
  customerLat?: number
  customerLng?: number
  /** 定位偏差（米） */
  locationDeviation?: number
  /** 定位偏差告警：0-正常 1-超限 */
  locationWarning?: number
  /** 本次生效的偏差阈值（米） */
  deviationThresh?: number
  /** 签收备注（拒收原因等） */
  remark?: string
  signTime?: string
  /** 0-待审核 1-已通过 2-已驳回 */
  auditStatus?: number
  auditStatusText?: string
  auditBy?: number
  auditByName?: string
  auditTime?: string
  auditRemark?: string
  collectOnDelivery?: number
  deliveryFee?: number
  createTime?: string
}

/** 台账查询条件（《签收管理开发文档》§3.3） */
export interface SignQuery {
  pageNum?: number
  pageSize?: number
  keyword?: string
  taskNo?: string
  riderId?: number
  customerName?: string
  /** CSV 多选：1-正常 2-部分 3-拒收 */
  signTypes?: string
  /** CSV 多选：0-待审核 1-已通过 2-已驳回 */
  auditStatusList?: string
  signTimeStart?: string
  signTimeEnd?: string
  onlyWarning?: boolean
  hasSignature?: boolean
  hasPhoto?: boolean
  sortField?: string
  sortOrder?: string
}

/** 签收统计（后端 SignStatVO，比率均为百分数） */
export interface SignStat {
  total: number
  pending: number
  approved: number
  rejected: number
  normalCount: number
  partialCount: number
  rejectCount: number
  warningCount: number
  signatureCount: number
  photoCount: number
  approveRate: number
  warningRate: number
  rejectRate: number
}

export interface SignAuditParams {
  /** 1-通过 2-驳回 */
  auditStatus: number
  /** 驳回原因（驳回必填） */
  auditRemark?: string
}

/** 提交签收参数（配送员端；管理端一般不用） */
export interface SignSubmitParams {
  taskId: number
  /** 1-正常 2-部分 3-拒收 */
  signType: number
  photoUrls?: string
  signatureUrl?: string
  signLat?: number
  signLng?: number
  customerLat?: number
  customerLng?: number
  /** 偏差阈值(米)，缺省读配送参数，最终回落 100 */
  deviationThresh?: number
  /** 实际签收数量（部分签收必填） */
  actualQuantity?: number
  /** 签收备注（拒收必填） */
  remark?: string
}

export const signApi = {
  /** 台账分页（多条件） */
  page(params: SignQuery) { return request.get('/dms/sign/page', { params }) },

  /** 详情 */
  detail(id: number | string) { return request.get(`/dms/sign/detail/${id}`) },

  /** 按任务查最新一条签收（兼容旧接口） */
  getByTaskId(taskId: number | string) { return request.get(`/dms/sign/${taskId}`) },

  /** 统计（签收率 / 超阈值率 / 拒收率） */
  stat(params?: SignQuery) { return request.get('/dms/sign/stat', { params }) },

  /** 审核（通过 → 任务已完成；驳回 → 任务退回配送中） */
  audit(id: number | string, data: SignAuditParams) { return request.post(`/dms/sign/${id}/audit`, data) },

  /** 批量审核 */
  batchAudit(ids: (number | string)[], data: SignAuditParams) {
    return request.post('/dms/sign/batch-audit', { ids, ...data })
  },

  /** 删除（仅待审核可删） */
  remove(id: number | string) { return request.delete(`/dms/sign/${id}`) },

  /** 提交签收 */
  submit(params: SignSubmitParams) { return request.post('/dms/sign/submit', params) },

  /** 导出真实 xlsx */
  export(params: SignQuery): Promise<Blob> {
    return request.get('/dms/sign/export', { params, responseType: 'blob' })
  },
}
