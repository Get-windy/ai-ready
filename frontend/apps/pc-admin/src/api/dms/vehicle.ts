/**
 * DMS 车辆管理 API 模块
 */
import request from '@/utils/request'

// ── 车辆 ──────────────────────────────────────────
export interface DmsVehicle {
  id: number
  plateNo: string
  vehicleType: number
  brand: string
  model: string
  color: string
  vin: string
  engineNo: string
  mileage: number
  status: number
  riderId: number
  riderName: string
  /** 车队长姓名 */
  vehicleManagerName?: string
  /** 核定载重(kg) */
  ratedLoad?: number
  /** 核定载客 */
  ratedPassenger?: number
  /** 货箱容积(m³) */
  cargoVolume?: number
  /** 注册日期 */
  registerDate?: string
  /** 保险到期日 */
  insuranceExpireDate?: string
  /** 年检到期日 */
  inspectionExpireDate?: string
  /** 保养间隔(km) */
  maintenanceIntervalKm?: number
  /** 所属性质: 1-公司自有 2-个人自带 3-租赁 */
  ownershipType?: number
  /** 所属部门 */
  department?: string
  remark: string
  createTime: string
  updateTime: string
}

export interface DmsMaintenanceRecord {
  id: number
  vehicleId: number
  plateNo: string
  maintenanceType: number
  maintenanceDate: string
  mileage: number
  cost: number
  remark: string
  createTime: string
  /** 维保厂商 */
  maintVendor?: string
  /** 维保内容 */
  maintContent?: string
}

export interface DmsBindingHistory {
  id: number
  vehicleId: number
  riderId: number
  riderName: string
  bindTime: string
  handoverTime?: string
  status: number
}

export const vehicleApi = {
  page(params: any) { return request.get('/dms/vehicle/page', { params }) },

  getById(id: number) { return request.get(`/dms/vehicle/${id}`) },

  create(data: Partial<DmsVehicle>) { return request.post('/dms/vehicle', data) },

  update(id: number, data: Partial<DmsVehicle>) { return request.put(`/dms/vehicle/${id}`, data) },

  remove(id: number) { return request.delete(`/dms/vehicle/${id}`) },

  updateStatus(id: number, status: number) { return request.put(`/dms/vehicle/${id}/status`, { status }) },

  /** 绑定骑手 */
  bindRider(id: number, riderId: number) { return request.post(`/dms/vehicle/${id}/bind`, { riderId }) },
  /** 绑定骑手（含姓名） */
  bindRiderWithName(data: { vehicleId: number; riderId: number; riderName: string }) { return request.post('/dms/vehicle/bind-rider', data) },

  /** 更新里程 */
  updateMileage(id: number, mileage: number) { return request.put(`/dms/vehicle/${id}/mileage`, { mileage }) },

  /** 到期保养提醒列表 */
  maintenanceDue(params: any) { return request.get('/dms/vehicle/maintenance/due', { params }) },

  /** 保养记录分页 */
  maintenancePage(params: any) { return request.get('/dms/vehicle/maintenance/page', { params }) },

  /** 创建保养记录 */
  createMaintenance(data: Partial<DmsMaintenanceRecord>) { return request.post('/dms/vehicle/maintenance', data) },

  /** 删除保养记录 */
  deleteMaintenance(id: number) { return request.delete(`/dms/vehicle/maintenance/${id}`) },

  /** 获取绑定历史 */
  bindingHistory(vehicleId: number) { return request.get(`/dms/vehicle/${vehicleId}/binding-history`) },
}
