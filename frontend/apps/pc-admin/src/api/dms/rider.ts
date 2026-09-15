/**
 * 配送员管理 API（配送 → 人车管理 → 配送员管理）
 *
 * 接口口径见《配送员管理开发文档》§3.3：
 * 状态更新 PUT + body、审核收 remark、删除走 DELETE、位置上报 body 携带 riderId、统一 /options 选择器。
 */
import request, { type ApiResponse, type PageResponse } from '@/utils/request'

// ── 配送员类型（与后端 RiderTypeEnum 严格对齐，禁止再出现 0全职/1兼职 的错位口径） ──
export const RIDER_TYPE_MAP: Record<number, { text: string; color: string }> = {
  1: { text: '企业员工', color: 'blue' },
  2: { text: '众包兼职', color: 'purple' },
  3: { text: '外部平台配送员', color: 'cyan' },
  4: { text: '社会车辆司机', color: 'orange' },
}

export const RIDER_TYPE_OPTIONS = Object.entries(RIDER_TYPE_MAP).map(([value, item]) => ({
  label: item.text,
  value: Number(value),
}))

// ── 状态（与实体语义对齐：1=空闲，非「在线」） ──
export const RIDER_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '离线', color: 'default' },
  1: { text: '空闲', color: 'green' },
  2: { text: '忙碌', color: 'orange' },
  3: { text: '休息', color: 'blue' },
}

export const RIDER_STATUS_OPTIONS = Object.entries(RIDER_STATUS_MAP).map(([value, item]) => ({
  label: item.text,
  value: Number(value),
}))

// ── 审核状态 ──
export const RIDER_VERIFY_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '待审核', color: 'orange' },
  1: { text: '已通过', color: 'green' },
  2: { text: '已拒绝', color: 'red' },
}

// ── 结算方式 ──
export const RIDER_SETTLE_MAP: Record<number, string> = {
  1: '按单结算',
  2: '月结',
  3: '时段结算',
}

export interface DmsRider {
  id: number
  riderNo: string
  realName: string
  phone: string
  riderType: number
  riderTypeText?: string
  status: number
  statusText?: string
  verifyStatus: number
  verifyStatusText?: string
  verifyRemark?: string
  idCard?: string
  userId?: number
  userName?: string
  deptId?: number
  deptName?: string
  entryDate?: string
  channelId?: number
  channelName?: string
  platformRiderId?: string
  qualificationExpireDate?: string
  qualificationExpired?: boolean
  qualificationRemainDays?: number
  driverLicense?: string
  healthCertNo?: string
  settleMethod?: number
  settleMethodText?: string
  ratingScore?: number
  totalOrders?: number
  todayOrders?: number
  punctualRate?: number
  vehicleType?: string
  vehicleNo?: string
  vehiclePlate?: string
  onlineStatus?: number
  onlineStatusText?: string
  lastReportTime?: string
  currentLat?: number
  currentLng?: number
  serviceRadius?: number
  maxConcurrent?: number
  workHoursStart?: string
  workHoursEnd?: string
  depositAmount?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface DmsRiderQuery {
  pageNum?: number
  pageSize?: number
  keyword?: string
  realName?: string
  phone?: string
  riderTypes?: number[]
  channelId?: number
  deptId?: number
  status?: number
  verifyStatus?: number
  onlineStatus?: number
  qualifyExpireAlert?: number
  qualifyExpireDays?: number
  createTimeStart?: string
  createTimeEnd?: string
  sortField?: string
  sortOrder?: string
}

export interface RiderOption {
  id: number
  riderNo: string
  realName: string
  phone: string
  riderType: number
  riderTypeText: string
  status: number
}

export const riderApi = {
  /** 多条件分页 */
  page(params: DmsRiderQuery): Promise<ApiResponse<PageResponse<DmsRider>>> {
    return request.get('/dms/rider/page', params)
  },

  /** 不分页列表（导出/兼容旧调用） */
  list(params?: DmsRiderQuery): Promise<ApiResponse<DmsRider[]>> {
    return request.get('/dms/rider/list', params)
  },

  /** 详情 */
  getById(id: number): Promise<ApiResponse<DmsRider>> {
    return request.get(`/dms/rider/${id}`)
  },

  /** 新增 */
  create(data: Partial<DmsRider>): Promise<ApiResponse<DmsRider>> {
    return request.post('/dms/rider', data)
  },

  /** 修改 */
  update(id: number, data: Partial<DmsRider>): Promise<ApiResponse<DmsRider>> {
    return request.put(`/dms/rider/${id}`, data)
  },

  /** 状态更新（PUT + body，后端做状态机与资质门控校验） */
  updateStatus(id: number, status: number): Promise<ApiResponse<void>> {
    return request.put(`/dms/rider/${id}/status`, { status })
  },

  /** 批量启停 */
  batchStatus(ids: number[], status: number): Promise<ApiResponse<number>> {
    return request.post('/dms/rider/batch-status', { ids, status })
  },

  /** 审核（通过/拒绝 + 备注） */
  approve(id: number, data: { verifyStatus: number; remark?: string }): Promise<ApiResponse<void>> {
    return request.post(`/dms/rider/${id}/approve`, data)
  },

  /** 批量审核 */
  batchApprove(ids: number[], data: { verifyStatus: number; remark?: string }): Promise<ApiResponse<number>> {
    return request.post('/dms/rider/batch-approve', { ids, ...data })
  },

  /** 删除（真实删除，替换原「假删除」） */
  remove(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/dms/rider/${id}`)
  },

  /** 批量删除 */
  batchDelete(ids: number[]): Promise<ApiResponse<number>> {
    return request.post('/dms/rider/batch-delete', { ids })
  },

  /** 选择器数据源（调度指派 / 车辆绑定 / 线路共用） */
  options(params?: { keyword?: string; assignable?: boolean }): Promise<ApiResponse<RiderOption[]>> {
    return request.get('/dms/rider/options', params)
  },

  /** 生成下一个配送员编号 */
  nextCode(): Promise<ApiResponse<string>> {
    return request.get('/dms/rider/next-code')
  },

  /** 位置上报（配送员端调用） */
  reportLocation(data: { riderId: number; lat: number; lng: number }): Promise<ApiResponse<void>> {
    return request.post('/dms/rider/location', data)
  },

  /** 导出（后端返回真实 xlsx） */
  export(params: DmsRiderQuery): Promise<Blob> {
    return request.get('/dms/rider/export', { params, responseType: 'blob' })
  },
}

export default riderApi
