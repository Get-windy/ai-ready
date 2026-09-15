/**
 * DMS 车辆管理 API 模块（配送 → 人车管理 → 车辆管理，菜单 80770 `dms:vehicle`）
 *
 * 对标《车辆管理开发文档》§3.4 金标准接口清单；出参字段统一为 `currentRider*`（原 riderId/riderName 与实体不符，页面列恒空）。
 * 维保台账接口见 `/dms/vehicle/maintenance/**`（车辆维护页 80780）。
 */
import request from '@/utils/request'

// ── 车辆档案 ──────────────────────────────────────────
export interface DmsVehicle {
  id: number
  /** 车辆编码（VH 号段，新增时留空由后端生成） */
  vehicleCode?: string
  plateNo: string
  vehicleType: number
  vehicleTypeText?: string
  brand?: string
  model?: string
  color?: string
  vin?: string
  engineNo?: string
  /** 归属类型：1-公司自有 2-个人自带 3-租赁 */
  ownershipType?: number
  ownershipTypeText?: string
  /** 车主姓名（个人自带/租赁） */
  ownerName?: string
  ownerPhone?: string
  /** 核定载重(kg) */
  ratedLoad?: number
  /** 核定载客(人) */
  ratedPassenger?: number
  /** 货厢容积(m³) */
  cargoVolume?: number
  registerDate?: string
  maintenanceIntervalKm?: number
  operatingPermitNo?: string
  operatingPermitExpireDate?: string
  insuranceExpireDate?: string
  inspectionExpireDate?: string
  department?: string
  remark?: string
  /** 状态：0-空闲 1-使用中 2-维修中 3-已报废 4-已出勤 */
  status?: number
  statusText?: string
  /** 车队长（车辆负责人） */
  vehicleManagerId?: number
  vehicleManagerName?: string
  vehicleManagerPhone?: string
  /** 当前配送员（一对一绑定） */
  currentRiderId?: number
  currentRiderName?: string
  currentMileage?: number
  lastMaintenanceKm?: number
  lastMaintenanceDate?: string
  /** 证件最近到期日与剩余天数（保险/年检/营运证三者最小值） */
  certNearestExpireDate?: string
  certDaysLeft?: number
  certWarnText?: string
  createTime?: string
  updateTime?: string
}

export interface DmsVehicleQuery {
  pageNum?: number
  pageSize?: number
  keyword?: string
  vehicleType?: number
  ownershipType?: number
  status?: number
  expiringDays?: number
  certType?: string
  currentRiderName?: string
  department?: string
  registerDateStart?: string
  registerDateEnd?: string
  sortField?: string
  sortOrder?: string
}

/** 证件到期清单行 */
export interface DmsVehicleCertExpiry {
  vehicleId: number
  vehicleCode?: string
  plateNo: string
  certType: 'INSURANCE' | 'INSPECTION' | 'PERMIT'
  certTypeText: string
  certDate: string
  daysLeft: number
  warnLevel: 'EXPIRED' | 'WARNING' | 'NORMAL'
  status?: number
  statusText?: string
  currentRiderName?: string
  ownershipTypeText?: string
  department?: string
}

export interface DmsMaintenanceRecord {
  id: number
  vehicleId: number
  plateNo?: string
  maintType: number
  maintNo?: string
  maintDate: string
  mileage?: number
  cost?: number
  remark?: string
  createTime?: string
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
  riderPhone?: string
  bindTime: string
  handoverTime?: string
  bindMileage?: number
  handoverMileage?: number
  bindReason?: string
  /** 0-绑定中 1-已交车 2-异常解绑 */
  status: number
}

export const vehicleApi = {
  // ── 列表 ──
  page(params: DmsVehicleQuery) { return request.get('/dms/vehicle/page', { params }) },

  /** 选择器数据源（调度指派 / 人车绑定 / 线路共用，排除已报废） */
  options() { return request.get('/dms/vehicle/options') },

  /** 生成下一个车辆编码（VH 号段） */
  nextCode() { return request.get('/dms/vehicle/next-code') },

  /** 证件到期清单（保险 / 年检 / 营运证） */
  expiring(params?: { days?: number; certType?: string }) { return request.get('/dms/vehicle/expiring', { params }) },

  /** 导出真实 xlsx */
  export(params: DmsVehicleQuery): Promise<Blob> {
    return request.get('/dms/vehicle/export', { params, responseType: 'blob' })
  },

  getById(id: number) { return request.get(`/dms/vehicle/${id}`) },

  create(data: Partial<DmsVehicle>) { return request.post('/dms/vehicle', data) },

  update(id: number, data: Partial<DmsVehicle>) { return request.put(`/dms/vehicle/${id}`, data) },

  remove(id: number) { return request.delete(`/dms/vehicle/${id}`) },

  /** 批量删除（原实现把 "1,2" 拼进 DELETE /{id}，类型转换必失败） */
  batchDelete(ids: (number | string)[]) { return request.post('/dms/vehicle/batch-delete', { ids }) },

  // ── 状态 / 人车绑定 ──
  /** 状态变更（状态机校验由后端执行） */
  updateStatus(id: number, status: number) { return request.put(`/dms/vehicle/${id}/status`, { status }) },

  /** 批量状态变更（批量启停 / 批量报废） */
  batchStatus(ids: (number | string)[], status: number) {
    return request.post('/dms/vehicle/batch-status', { ids, status })
  },

  /** 绑定配送员（人车一对一；body 携带 riderId，路径统一 /{id}/bind-rider） */
  bindRider(id: number, data: { riderId: number; mileage?: number; remark?: string }) {
    return request.post(`/dms/vehicle/${id}/bind-rider`, data)
  },

  /** 解绑配送员（写交车里程） */
  unbindRider(id: number, mileage?: number) {
    return request.post(`/dms/vehicle/${id}/unbind-rider`, { mileage })
  },

  /** 绑定/解绑流水 */
  bindingHistory(vehicleId: number) { return request.get(`/dms/vehicle/${vehicleId}/binding-history`) },

  /** 更新当前里程 */
  updateMileage(id: number, mileage: number) { return request.put(`/dms/vehicle/${id}/mileage`, { mileage }) },

  /** 待保养/证件到期提醒列表 */
  maintenanceDue(params: { pageNum?: number; pageSize?: number; warnKm?: number }) {
    return request.get('/dms/vehicle/maintenance-due', { params })
  },

  // ── 维保台账（车辆维护页 80780，后端 VehicleMaintenanceController） ──
  maintenancePage(params: any) { return request.get('/dms/vehicle/maintenance/page', { params }) },

  createMaintenance(data: Partial<DmsMaintenanceRecord>) { return request.post('/dms/vehicle/maintenance', data) },

  deleteMaintenance(id: number) { return request.delete(`/dms/vehicle/maintenance/${id}`) },
}

// ── 车辆补能（加油/充电/加气/换电）─────────────────────────
export interface DmsVehicleEnergyLog {
  id: number
  /** 四轮车ID（与 riderId 二选一） */
  vehicleId?: number
  /** 骑手ID（两轮车换电/充电） */
  riderId?: number
  /** 1-汽油 2-柴油 3-充电 4-换电 5-加气 */
  energyType: number
  /** 1-现金 2-油卡 3-电卡 4-月租套餐 5-平台代扣 */
  payMode?: number
  quantity?: number
  unitPrice?: number
  amountYuan?: number
  odometer?: number
  /** 区间里程（服务端自动） */
  mileageSinceLast?: number
  /** 每公里成本（服务端自动） */
  unitCost?: number
  station?: string
  occurredAt: string
  cardNo?: string
  voucherUrl?: string
  /** 0-正常 1-异常 */
  abnormalFlag?: number
  abnormalReason?: string
  remark?: string
  // VO 附加
  subjectType?: 'VEHICLE' | 'RIDER'
  subjectName?: string
  plateNo?: string
  riderName?: string
  energyTypeText?: string
  payModeText?: string
  /** 百公里油耗（派生，不落库） */
  fuelPer100Km?: number
}

/**
 * 补能流水 API
 *
 * 落点是「配送 → 人车管理 → 用车管理」的「车辆补能」Tab —— 补能是一车/一人多条的从属流水，
 * 不单独占菜单项；区间里程与每公里成本由服务端自动核算。
 */
export const energyLogApi = {
  page(params: any) { return request.get('/dms/vehicle/energy/page', { params }) },
  detail(id: number | string) { return request.get(`/dms/vehicle/energy/${id}`) },
  create(data: Partial<DmsVehicleEnergyLog>) { return request.post('/dms/vehicle/energy', data) },
  update(id: number | string, data: Partial<DmsVehicleEnergyLog>) { return request.put(`/dms/vehicle/energy/${id}`, data) },
  remove(id: number | string) { return request.delete(`/dms/vehicle/energy/${id}`) },
  batchRemove(ids: (number | string)[]) { return request.post('/dms/vehicle/energy/batch-delete', ids) },
  export(params: any): Promise<Blob> {
    return request.get('/dms/vehicle/energy/export', { params, responseType: 'blob' })
  },
  /** 能耗报表（汇总 + 油电对比 + 分组） */
  stats(params: any) { return request.get('/dms/vehicle/energy/stats', { params }) },
}

// ── 补能卡 / 套餐（一卡一车一人）─────────────────────────
export interface DmsVehicleEnergyCard {
  id: number
  cardNo: string
  cardName?: string
  /** 1-油卡 2-电卡 3-换电套餐 4-充电套餐 5-加气卡 */
  cardType: number
  vehicleId?: number
  riderId?: number
  issuer?: string
  monthlyFee?: number
  balance?: number
  quota?: number
  usedQuota?: number
  startDate?: string
  expireDate?: string
  /** 0-停用 1-启用 */
  status?: number
  remark?: string
  // VO 附加
  cardTypeText?: string
  subjectType?: 'VEHICLE' | 'RIDER'
  subjectName?: string
  statusText?: string
  expired?: boolean
  daysToExpire?: number
  quotaUsagePercent?: number
}

export const energyCardApi = {
  page(params: any) { return request.get('/dms/vehicle/energy/card/page', { params }) },
  detail(id: number | string) { return request.get(`/dms/vehicle/energy/card/${id}`) },
  options() { return request.get('/dms/vehicle/energy/card/options') },
  create(data: Partial<DmsVehicleEnergyCard>) { return request.post('/dms/vehicle/energy/card', data) },
  update(id: number | string, data: Partial<DmsVehicleEnergyCard>) { return request.put(`/dms/vehicle/energy/card/${id}`, data) },
  remove(id: number | string) { return request.delete(`/dms/vehicle/energy/card/${id}`) },
  updateStatus(id: number | string, status: number) {
    return request.put(`/dms/vehicle/energy/card/${id}/status`, null, { params: { status } })
  },
}
