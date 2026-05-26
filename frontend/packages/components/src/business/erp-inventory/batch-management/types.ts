/**
 * 批次管理相关类型定义
 */

// 批次状态
export type BatchStatus = 'normal' | 'warning' | 'expired' | 'locked' | 'out_of_stock'

// 批次操作类型
export type BatchAction = 'view' | 'edit' | 'delete' | 'lock' | 'unlock' | 'move' | 'split' | 'merge'

// 批次项接口
export interface BatchItem {
  id: number
  batchNo: string
  productName: string
  productCode: string
  quantity: number
  unit: string
  productionDate: string // ISO日期字符串
  expiryDate: string // ISO日期字符串
  status: BatchStatus
  warehouse: string
  location: string
  supplier: string
  remark?: string
  createdAt?: string
  updatedAt?: string
  createdBy?: string
  updatedBy?: string
}

// 批次详情接口
export interface BatchDetail extends BatchItem {
  specifications?: string // 规格
  qualityGrade?: string // 质量等级
  storageConditions?: string // 存储条件
  inspectionRecords?: InspectionRecord[] // 检验记录
  movementLogs?: MovementLog[] // 移动记录
  attachments?: Attachment[] // 附件
}

// 检验记录接口
export interface InspectionRecord {
  id: number
  inspectionNo: string
  inspectionDate: string
  inspector: string
  result: 'qualified' | 'unqualified' | 'pending'
  remarks?: string
  attachmentUrls?: string[]
}

// 移动记录接口
export interface MovementLog {
  id: number
  movementType: 'in' | 'out' | 'transfer' | 'adjustment'
  fromLocation?: string
  toLocation?: string
  quantity: number
  operator: string
  operationTime: string
  remarks?: string
}

// 附件接口
export interface Attachment {
  id: number
  name: string
  url: string
  type: string
  size: number
  uploadedAt: string
  uploadedBy: string
}

// 搜索参数接口
export interface SearchParams {
  batchNo?: string
  productName?: string
  productCode?: string
  status?: BatchStatus | ''
  warehouse?: string
  supplier?: string
  dateRange?: [string, string] | [] // 生产日期范围
  expiryDateRange?: [string, string] | [] // 有效期范围
}

// 分页参数接口
export interface PaginationParams {
  currentPage: number
  pageSize: number
  total?: number
}

// 批次操作表单
export interface BatchOperationForm {
  operationType: BatchAction
  targetBatches: number[] // 批次ID列表
  targetLocation?: string
  newQuantity?: number
  remarks?: string
  confirmPassword?: string // 敏感操作需要密码确认
}

// 批次编辑表单
export interface BatchEditForm {
  id?: number
  batchNo: string
  productName: string
  productCode: string
  quantity: number
  unit: string
  productionDate: string
  expiryDate: string
  warehouse: string
  location: string
  supplier: string
  status: BatchStatus
  remark?: string
}

// 批次过滤选项
export interface FilterOptions {
  statusOptions: Array<{ label: string; value: BatchStatus }>
  warehouseOptions: Array<{ label: string; value: string }>
  supplierOptions: Array<{ label: string; value: string }>
}

// 批次统计信息
export interface BatchStatistics {
  totalCount: number
  normalCount: number
  warningCount: number
  expiredCount: number
  lockedCount: number
  outOfStockCount: number
  totalQuantity: number
  expiringCount: number // 即将过期数量（30天内）
}

// API响应格式
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: string
}

export interface ListResponse<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

export interface BatchListResponse extends ListResponse<BatchItem> {
  statistics?: BatchStatistics
}