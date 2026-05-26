// 供应商类型定义

export type SupplierStatus = 'active' | 'pending' | 'inactive' | 'rejected'
export type SupplierType = 'manufacturer' | 'distributor' | 'wholesaler' | 'retailer' | 'service'
export type BadgeType = 'recommended' | 'new' | 'vip' | 'certified' | 'default'

// 联系人信息接口
export interface ContactInfo {
  name?: string
  phone: string
  email: string
  position?: string
  wechat?: string
  phone2?: string
}

// 地址信息接口
export interface AddressInfo {
  province: string
  city: string
  district: string
  street: string
  postalCode?: string
  fullAddress?: string
  lat?: number
  lng?: number
}

// 资质认证接口
export interface Qualification {
  name: string
  code?: string
  issuingAuthority?: string
  issueDate?: Date
  expiryDate?: Date
  verified: boolean
  attachmentUrl?: string
}

// 业绩指标接口
export interface PerformanceMetrics {
  cooperationYears: number
  orderCount: number
  totalTransactionAmount: number
  onTimeDeliveryRate: number
  qualityAcceptanceRate: number
  averageResponseTime: number // 小时
  rating: number // 1-5分
  lastOrderDate?: Date
}

// 产品类别接口
export interface ProductCategory {
  id: string
  name: string
  count: number
  mainProducts?: string[]
}

// 合作历史接口
export interface CooperationHistory {
  year: number
  orderCount: number
  totalAmount: number
  majorProjects?: string[]
}

// 评价接口
export interface Review {
  id: string
  reviewer: string
  rating: number
  content: string
  date: Date
  verifiedPurchase: boolean
}

// 主要供应商接口
export interface Supplier {
  // 基本信息
  id: string
  code?: string
  name: string
  shortName?: string
  type: SupplierType
  industry?: string
  description?: string
  establishedYear?: number
  employeeCount?: number
  
  // 联系信息
  contact?: ContactInfo
  address?: AddressInfo | string
  
  // 状态信息
  status: SupplierStatus
  registrationDate: Date
  lastActiveDate?: Date
  
  // 资质信息
  qualifications: string[] | Qualification[]
  isCertified?: boolean
  certificationLevel?: string
  
  // 业务信息
  productCategories?: ProductCategory[]
  mainProducts?: string[]
  serviceArea?: string[]
  
  // 绩效指标
  metrics?: PerformanceMetrics
  cooperationHistory?: CooperationHistory[]
  
  // 评价信息
  reviews?: Review[]
  averageRating?: number
  reviewCount?: number
  
  // 视觉信息
  logo?: string
  bannerImage?: string
  brandColor?: string
  
  // 标签和角标
  tags?: string[]
  badge?: string
  badgeType?: BadgeType
  
  // 财务信息
  paymentTerm?: string
  creditLimit?: number
  creditRating?: 'A+' | 'A' | 'B' | 'C' | 'D'
  
  // 系统信息
  createdAt: Date
  updatedAt: Date
  createdBy?: string
  updatedBy?: string
}

// 供应商筛选条件接口
export interface SupplierFilter {
  search?: string
  status?: SupplierStatus | SupplierStatus[]
  type?: SupplierType | SupplierType[]
  industry?: string | string[]
  location?: string
  hasCertification?: boolean
  minRating?: number
  minCooperationYears?: number
  productCategory?: string
  tags?: string[]
  createdDateRange?: [Date, Date] // 开始日期和结束日期
  page?: number
  pageSize?: number
  sortBy?: 'name' | 'rating' | 'cooperationYears' | 'orderCount' | 'createdAt'
  sortOrder?: 'asc' | 'desc'
}

// 供应商统计信息接口
export interface SupplierStatistics {
  totalCount: number
  activeCount: number
  pendingCount: number
  inactiveCount: number
  certifiedCount: number
  byType: Record<SupplierType, number>
  byIndustry: Record<string, number>
  byLocation: Record<string, number>
  averageRating: number
  totalOrderCount: number
  totalTransactionAmount: number
}

// 供应商导出格式接口
export interface SupplierExportFormat {
  id: string
  name: string
  type: string
  status: string
  contactPhone: string
  contactEmail: string
  address: string
  qualifications: string
  cooperationYears: number
  orderCount: number
  rating: number
  createdAt: string
}

// 供应商导入格式接口
export interface SupplierImportFormat {
  name: string
  type: SupplierType
  contactPhone: string
  contactEmail: string
  address: string
  industry?: string
  qualifications?: string
}

// 供应商事件类型
export type SupplierEvent = 
  | 'status_change'
  | 'qualification_updated'
  | 'review_added'
  | 'order_placed'
  | 'rating_updated'

// 供应商事件接口
export interface SupplierEventLog {
  id: string
  supplierId: string
  eventType: SupplierEvent
  eventData: Record<string, any>
  timestamp: Date
  triggeredBy: string
  notes?: string
}

// 供应商批量操作结果接口
export interface SupplierBatchResult {
  total: number
  success: number
  failed: number
  results: Array<{
    supplierId: string
    success: boolean
    error?: string
  }>
}

// 供应商API响应接口
export interface SupplierApiResponse<T> {
  success: boolean
  data?: T
  message?: string
  error?: string
  pagination?: {
    page: number
    pageSize: number
    total: number
    totalPages: number
  }
}

// 供应商列表响应接口
export interface SupplierListResponse {
  suppliers: Supplier[]
  statistics: SupplierStatistics
  pagination: {
    page: number
    pageSize: number
    total: number
    totalPages: number
  }
}

// 供应商表单数据接口
export interface SupplierFormData {
  // 基本信息
  name: string
  shortName?: string
  type: SupplierType
  industry?: string
  description?: string
  
  // 联系信息
  contactName?: string
  contactPhone: string
  contactEmail: string
  contactPosition?: string
  contactWechat?: string
  
  // 地址信息
  province: string
  city: string
  district: string
  street: string
  postalCode?: string
  
  // 资质信息
  qualifications: string[]
  isCertified?: boolean
  certificationLevel?: string
  
  // 产品信息
  mainProducts?: string[]
  serviceArea?: string[]
  
  // 财务信息
  paymentTerm?: string
  creditLimit?: number
  
  // 附件
  logoFile?: File
  qualificationFiles?: File[]
}

// 供应商验证规则接口
export interface SupplierValidationRules {
  name: {
    required: boolean
    minLength: number
    maxLength: number
    pattern?: RegExp
  }
  contactPhone: {
    required: boolean
    pattern: RegExp
  }
  contactEmail: {
    required: boolean
    pattern: RegExp
  }
  type: {
    required: boolean
    allowedValues: SupplierType[]
  }
}

// 供应商搜索建议接口
export interface SupplierSearchSuggestion {
  id: string
  name: string
  type: SupplierType
  matchType: 'name' | 'code' | 'product' | 'tag'
  matchText: string
  score: number
}

// 供应商地图位置接口
export interface SupplierMapLocation {
  id: string
  name: string
  address: string
  lat: number
  lng: number
  type: SupplierType
  status: SupplierStatus
  markerColor: string
}