/**
 * DMS 实名认证（KYC）与人车核验 API 模块
 *
 * 契约（2026-09-12 对齐）：
 *  · 分页参数统一 page / size（后端默认 page=1&size=20）
 *  · 写操作一律 JSON body；处理人/审核人由登录态决定，前端不传
 */
import request from '@/utils/request'

// ── 人车绑定 ──────────────────────────────────────────
export interface DmsRiderVehicleBinding {
  id: number
  riderId: number
  riderName: string
  riderPhone?: string
  vehicleId: number
  plateNo: string
  bindTime: string
  bindMileage?: number
  handoverTime?: string
  handoverMileage?: number
  handoverLocation?: string
  bindReason?: string
  /** 0-绑定中 1-已交车 2-异常解绑 */
  status: number
  remark?: string
  createTime?: string
}

// ── 位置核验 ──────────────────────────────────────────
export interface DmsPositionVerification {
  id: number
  bindingId: number
  riderId: number
  vehicleId: number
  riderLat: number
  riderLng: number
  vehicleLat: number
  vehicleLng: number
  distanceMeters: number
  thresholdMeters: number
  /** 0-正常 1-超阈值 */
  isAbnormal: number
  verifyTime: string
  verifyDesc: string
}

// ── 预警 ──────────────────────────────────────────────
export interface DmsVerificationAlert {
  id: number
  bindingId?: number
  riderId?: number
  riderName?: string
  vehicleId?: number
  plateNo?: string
  /** 1-人车分离 2-异常滞留 3-速度异常 4-偏离路线 5-绑定超时 6-非工作时段用车 7-证照到期 */
  alertType: number
  /** 1-提示 2-警告 3-严重 */
  alertLevel: number
  alertContent: string
  distanceMeters?: number
  stayDuration?: number
  /** 0-待处理 1-已确认 2-已忽略 3-已处理 */
  handleStatus: number
  handler?: string
  handleTime?: string
  handleRemark?: string
  createTime: string
}

// ── 巡检 ──────────────────────────────────────────────
export interface DmsVehicleInspection {
  id: number
  vehicleId: number
  plateNo: string
  riderId: number
  riderName: string
  /** 1-出车前 2-收车后 3-随机抽检 4-定期检查 */
  inspectionType: number
  /** 1-通过 2-不通过 */
  result: number
  inspectionTime: string
  mileage?: number
  fuelLevel?: number
  exteriorStatus?: number
  tireStatus?: number
  lightStatus?: number
  brakeStatus?: number
  cleanlinessStatus?: number
  fireExtinguisher?: number
  warningTriangle?: number
  inspectionLocation?: string
  reviewer?: string
  reviewTime?: string
  reviewRemark?: string
  remark?: string
}

// ── 实名认证台账 ──────────────────────────────────────
export interface DmsRiderCertificate {
  id?: number
  verificationId?: number
  riderId?: number
  /** 1-驾驶证 2-行驶证 3-健康证 4-从业资格证 5-其他 */
  certType: number
  certNo?: string
  issueDate?: string
  expireDate?: string
  certUrl?: string
  /** 0-待核验 1-有效 2-已过期 3-无效 */
  verifyStatus?: number
  remark?: string
}

export interface DmsRiderVerification {
  id: number
  riderId: number
  riderName: string
  riderPhone?: string
  riderType?: number
  riderTypeText?: string
  channelId?: number
  channelName?: string
  realName?: string
  /** 已脱敏（前 3 后 4） */
  idCardNo?: string
  idCardUrls?: string
  endorseOrg?: string
  endorseResult?: number
  endorseExpireDate?: string
  /** 0-待提交 1-待审核 2-已通过 3-已驳回 4-已过期 */
  verifyStatus: number
  verifyStatusText?: string
  eligible?: boolean
  ineligibleReason?: string
  auditBy?: number
  auditTime?: string
  auditRemark?: string
  effectiveTime?: string
  remark?: string
  certificates?: DmsRiderCertificate[]
  certCount?: number
  expiringCertCount?: number
  createTime?: string
}

export interface EligibilityResult {
  riderId: number
  eligible: boolean
  verifyStatus?: number
  verifyStatusText?: string
  reasons: string[]
}

export interface ScanResult {
  scannedBindings: number
  timeoutAlerts: number
  stayAlerts: number
  separationAlerts: number
  certExpiringAlerts: number
  skippedDuplicated: number
}

export interface BindingDetail {
  binding: DmsRiderVehicleBinding
  verifications: DmsPositionVerification[]
  inspections: DmsVehicleInspection[]
  alerts: DmsVerificationAlert[]
  verifyCount: number
  abnormalCount: number
}

export const verificationApi = {
  // ── 人车绑定 ──
  /** 分页查询绑定记录 */
  bindingPage(params: any) { return request.get('/dms/verification/binding/page', { params }) },
  /** 绑定详情（含核验历史/巡检/预警） */
  bindingDetail(id: number | string) { return request.get(`/dms/verification/binding/${id}`) },
  /** 交车（POST + JSON body；收车后检查随交车一并提交） */
  bindingHandover(id: number | string, data: {
    handoverMileage?: number
    handoverLocation?: string
    handoverLat?: number
    handoverLng?: number
    remark?: string
    /** 收车后检查（强制）：异常项将自动转《车辆维护》并置车辆「维修中」 */
    inspection?: Record<string, any>
  }) {
    return request.post(`/dms/verification/binding/${id}/handover`, data)
  },
  /** 创建人车绑定（出车登记） */
  bind(data: { riderId: number | string; vehicleId: number | string; inspectionId?: number; bindReason?: string }) {
    return request.post('/dms/verification/bind', data)
  },
  /** 配送员当前活跃绑定 */
  activeByRider(riderId: number | string) { return request.get(`/dms/verification/binding/active/rider/${riderId}`) },
  /** 车辆当前活跃绑定 */
  activeByVehicle(vehicleId: number | string) { return request.get(`/dms/verification/binding/active/vehicle/${vehicleId}`) },

  // ── 巡检 ──
  /** 分页查询巡检记录 */
  inspectionPage(params: any) { return request.get('/dms/verification/inspection/page', { params }) },
  /** 创建巡检记录（出车前/收车后/抽检/定检） */
  inspectionCreate(data: any) { return request.post('/dms/verification/inspection', data) },
  /** 巡检记录详情（单张完整检查项） */
  inspectionDetail(id: number | string) { return request.get(`/dms/verification/inspection/${id}`) },
  /** 审核巡检（result：1-通过 2-不通过） */
  reviewInspection(id: number | string, data: { result: number; remark?: string }) {
    return request.put(`/dms/verification/inspection/${id}/review`, data)
  },

  // ── 位置核验 ──
  /** 分页查询核验记录 */
  verifyPage(params: any) { return request.get('/dms/verification/verify/page', { params }) },
  /** 手动位置核验 */
  verifyPosition(bindingId: number | string, params: any) { return request.post(`/dms/verification/verify/${bindingId}`, null, { params }) },

  // ── 预警 ──
  /** 分页查询预警 */
  alertPage(params: any) { return request.get('/dms/verification/alert/page', { params }) },
  /** 处理预警 */
  handleAlert(id: number | string, data: { handleStatus: number; remark?: string }) {
    return request.put(`/dms/verification/alert/${id}/handle`, data)
  },
  /** 手动触发批量核验（与定时任务同一实现） */
  scan() { return request.post('/dms/verification/scan') },

  // ── 实名认证 / 资质（KYC）──
  /** 台账分页 */
  kycPage(params: any) { return request.get('/dms/verification/kyc/page', { params }) },
  /** 台账详情 */
  kycDetail(id: number | string) { return request.get(`/dms/verification/kyc/${id}`) },
  /** 证照核验分页（证照维度：到期清单 + 剩余天数） */
  certificatePage(params: any) { return request.get('/dms/verification/kyc/certificate/page', { params }) },
  /** 按配送员取台账（表单回填） */
  kycByRider(riderId: number | string) { return request.get(`/dms/verification/kyc/by-rider/${riderId}`) },
  /** 提交实名认证 */
  kycSubmit(data: any) { return request.post('/dms/verification/kyc/submit', data) },
  /** 审核实名认证 */
  kycAudit(id: number | string, data: { approved: boolean; auditRemark?: string; endorseExpireDate?: string; certificates?: DmsRiderCertificate[] }) {
    return request.post(`/dms/verification/kyc/${id}/audit`, data)
  },
  /** 接单资质校验 */
  kycEligibility(riderId: number | string) { return request.get(`/dms/verification/kyc/eligibility/${riderId}`) },
  /** 准入核验（人证 × 车证一屏：可否接单 / 可否出车 + 阻塞原因） */
  onboardingPage(params: any) { return request.get('/dms/verification/onboarding/page', { params }) },
  /** 证照到期扫描 */
  scanExpiry(warnDays?: number) { return request.post('/dms/verification/kyc/scan-expiry', null, { params: { warnDays } }) },

  // ── 下拉 ──
  riderOptions(keyword?: string) { return request.get('/dms/verification/options/riders', { params: { keyword } }) },
  channelOptions() { return request.get('/dms/verification/options/channels') },

  // ── 导出真实 xlsx ──
  exportExcel(tab: string, params: any) {
    return request.get('/dms/verification/export', { params: { ...params, tab }, responseType: 'blob' })
  },
  exportCertificates(verificationId: number | string) {
    return request.get('/dms/verification/kyc/certificates/export', { params: { verificationId }, responseType: 'blob' })
  },
}
