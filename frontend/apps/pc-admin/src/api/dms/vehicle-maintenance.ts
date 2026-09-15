/**
 * DMS 车辆维保 API（配送 → 人车管理 → 车辆维护）
 *
 * 对接口径与《车辆维护开发文档》§3.3 一致：
 *   GET    /dms/vehicle/maintenance/page      多条件分页（车牌联查/类型多选/日期·费用区间/厂商/含附件）
 *   GET    /dms/vehicle/maintenance/{id}      详情
 *   GET    /dms/vehicle/maintenance/next-no   下一个维保单号（WBD 号段）
 *   GET    /dms/vehicle/maintenance/stat      费用统计（按类型/月份/车辆）
 *   GET    /dms/vehicle/maintenance/expiring  到期提醒（日期 + 里程双阈值）
 *   POST   /dms/vehicle/maintenance           新增
 *   PUT    /dms/vehicle/maintenance/{id}      修改
 *   DELETE /dms/vehicle/maintenance/{id}      删除
 *   GET    /dms/vehicle/maintenance/export    真实 xlsx 导出
 */
import request from '@/utils/request'

/** 维保记录（列表/详情统一 VO） */
export interface MaintenanceVO {
  id: number
  maintNo: string
  vehicleId: number
  plateNo?: string
  vehicleBrand?: string
  vehicleModel?: string
  vehicleType?: number
  /** 1-保养 2-维修 3-年检 4-保险 5-事故 6-其他 */
  maintType: number
  maintTypeText?: string
  maintDate?: string
  maintContent?: string
  maintCost?: number
  maintVendor?: string
  maintContact?: string
  maintPhone?: string
  /** 维保前里程（服务端按同车上一笔维保推导） */
  beforeMaintMileage?: number
  afterMaintMileage?: number
  nextMaintDate?: string
  nextMaintMileage?: number
  /** 厂商往来单位ID（空=未建档厂商，仅名称快照；雪花ID，按字符串处理） */
  vendorId?: number | string
  /** 剩余天数（负数=已逾期） */
  remainDays?: number
  /** 剩余公里（负数=已超） */
  remainKm?: number
  /** OVERDUE / DUE_SOON / NORMAL / NONE */
  dueStatus?: string
  attachmentUrls?: string
  remark?: string
  handlerName?: string
  createTime?: string
  updateTime?: string
}

/** 维保记录创建/修改请求 */
export interface MaintenanceDTO {
  vehicleId?: number
  maintType?: number
  maintDate?: string
  maintContent?: string
  maintCost?: number
  /** 厂商名称（手工填写；填了 vendorId 时由服务端按档案回填快照） */
  maintVendor?: string
  /** 厂商往来单位ID（biz_party.id，供应商/其他往来单位；雪花ID，前端按字符串透传） */
  vendorId?: number | string
  maintContact?: string
  maintPhone?: string
  afterMaintMileage?: number
  nextMaintDate?: string
  nextMaintMileage?: number
  attachmentUrls?: string
  remark?: string
}

/** 维保厂商选择器选项（来源：资料 → 往来单位） */
export interface VendorOption {
  /** 往来单位ID（雪花ID，按字符串透传避免精度丢失） */
  id: number | string
  code?: string
  name: string
  /** 2-供应商 4-其他往来单位 */
  partyType?: number
  partyTypeText?: string
}

/** 维保查询条件 */
export interface MaintenanceQuery {
  pageNum?: number
  pageSize?: number
  vehicleId?: number
  plateNo?: string
  maintNo?: string
  maintTypes?: number[]
  dateFrom?: string
  dateTo?: string
  costMin?: number
  costMax?: number
  /** 厂商名称模糊（含未建档厂商） */
  vendor?: string
  /** 厂商往来单位ID（精确过滤已建档厂商） */
  vendorId?: number | string
  hasAttachment?: number
}

/** 费用统计结果 */
export interface MaintenanceStat {
  recordCount: number
  totalCost: number
  vehicleCount: number
  byType: Array<{ type: number; typeText: string; count: number; cost: number }>
  byMonth: Array<{ month: string; count: number; cost: number }>
  byVehicle: Array<{ plateNo: string; count: number; cost: number }>
  /** 按厂商（服务商成本分析）：已建档按 vendorId 归并、未建档按名称归并 */
  byVendor: Array<{ vendorKey: string; vendorId?: number | null; vendorName: string; linked: boolean; count: number; cost: number }>
}

export const vehicleMaintenanceApi = {
  page(params: MaintenanceQuery) {
    return request.get('/dms/vehicle/maintenance/page', { params })
  },

  detail(id: number) {
    return request.get(`/dms/vehicle/maintenance/${id}`)
  },

  nextNo() {
    return request.get('/dms/vehicle/maintenance/next-no')
  },

  /** 厂商选择器数据源：资料 → 往来单位（供应商 / 其他往来单位） */
  vendorOptions(params: { keyword?: string; limit?: number }) {
    return request.get('/dms/vehicle/maintenance/vendor-options', { params })
  },

  stat(params: MaintenanceQuery) {
    return request.get('/dms/vehicle/maintenance/stat', { params })
  },

  expiring(params: { days?: number; warnKm?: number }) {
    return request.get('/dms/vehicle/maintenance/expiring', { params })
  },

  create(data: MaintenanceDTO) {
    return request.post('/dms/vehicle/maintenance', data)
  },

  update(id: number, data: MaintenanceDTO) {
    return request.put(`/dms/vehicle/maintenance/${id}`, data)
  },

  remove(id: number) {
    return request.delete(`/dms/vehicle/maintenance/${id}`)
  },

  export(params: MaintenanceQuery): Promise<Blob> {
    return request.get('/dms/vehicle/maintenance/export', { params, responseType: 'blob' })
  },

  /** 车辆选择器数据源（车辆档案 /options；此处封装避免与车辆管理 API 模块耦合） */
  vehicleOptions() {
    return request.get('/dms/vehicle/options')
  },
}
