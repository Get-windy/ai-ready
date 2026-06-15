/**
 * DMS 核验管理 API 模块
 */
import request from '@/utils/request'

// ── 核验绑定 ──────────────────────────────────────────
export interface DmsVerificationBinding {
  id: number
  riderId: number
  riderName: string
  vehicleId: number
  vehiclePlate: string
  plateNo?: string
  bindTime: string
  handoverTime?: string
  status: number
  createTime: string
  updateTime: string
}

// ── 核验记录 ──────────────────────────────────────────
export interface DmsVerificationRecord {
  id: number
  riderId: number
  riderName: string
  vehicleId: number
  vehiclePlate: string
  verificationType: number
  result: number
  verificationTime: string
  location: string
  remark: string
  createTime: string
}

// ── 核验告警 ──────────────────────────────────────────
export interface DmsVerificationAlert {
  id: number
  riderId: number
  riderName: string
  alertType: number
  alertLevel: number
  alertMsg: string
  alertContent?: string
  status: number
  handleStatus?: number
  handler?: string
  handleTime: string
  createTime: string
}

// ── 车辆巡检 ──────────────────────────────────────────
export interface DmsVehicleInspection {
  id: number
  vehicleId: number
  plateNo: string
  riderId: number
  riderName: string
  inspectionType: number
  result: number
  inspectionTime: string
  reviewer: string
  reviewTime: string
  remark: string
}

export const verificationApi = {
  /** 创建巡检任务 */
  inspectionCreate(data: { riderId: number; vehicleId: number; remark?: string }) { return request.post('/dms/verification/inspection', data) },

  /** 绑定核验（人车绑定） */
  bind(data: { riderId: number; vehicleId: number }) { return request.post('/dms/verification/bind', data) },

  /** 交接核验 */
  handover(data: { fromRiderId: number; toRiderId: number; vehicleId: number }) { return request.post('/dms/verification/handover', data) },

  /** 绑定记录分页 */
  bindingPage(params: any) { return request.get('/dms/verification/binding/page', { params }) },

  /** 绑定详情 */
  bindingDetail(id: number) { return request.get(`/dms/verification/binding/${id}`) },

  /** 交车操作 */
  bindingHandover(id: number, data: { handoverMileage?: number; handoverLocation?: string }) { return request.put(`/dms/verification/binding/${id}/handover`, data) },

  /** 骑手当前活跃绑定 */
  activeByRider(riderId: number) { return request.get(`/dms/verification/active/rider/${riderId}`) },

  /** 车辆当前活跃绑定 */
  activeByVehicle(vehicleId: number) { return request.get(`/dms/verification/active/vehicle/${vehicleId}`) },

  /** 提交核验结果 */
  verify(data: { bindingId: number; result: number; location?: string; remark?: string }) { return request.post('/dms/verification/verify', data) },

  /** 核验记录分页 */
  verifyPage(params: any) { return request.get('/dms/verification/verify/page', { params }) },

  /** 告警记录分页 */
  alertPage(params: any) { return request.get('/dms/verification/alert/page', { params }) },

  /** 处理告警 */
  handleAlert(id: number, data: { handleStatus: number; remark?: string }) { return request.put(`/dms/verification/alert/${id}/handle`, data) },

  /** 审核巡检结果 */
  reviewInspection(id: number, data: { reviewResult: number; result?: number; reviewer?: string; reviewRemark?: string; remark?: string }) { return request.put(`/dms/verification/inspection/${id}/review`, data) },

  /** 巡检记录分页 */
  inspectionPage(params: any) { return request.get('/dms/verification/inspection/page', { params }) },
}
