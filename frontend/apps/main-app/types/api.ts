/**
 * API响应类型定义
 */

// 基础响应接口
export interface IApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
  success: boolean
}

// 分页响应接口
export interface IPaginatedResponse<T = any> {
  items: T[]
  total: number
  page: number
  pageSize: number
  totalPages: number
  hasNext: boolean
  hasPrev: boolean
}

// 分页请求参数
export interface IPaginationParams {
  page?: number
  pageSize?: number
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
}

// 搜索请求参数
export interface ISearchParams extends IPaginationParams {
  keyword?: string
  filters?: Record<string, any>
}

// 文件上传响应
export interface IFileUploadResponse {
  id: string
  name: string
  url: string
  size: number
  type: string
  uploadTime: number
}

// 错误响应
export interface IErrorResponse {
  code: number
  message: string
  details?: Record<string, any>
  stack?: string
}

// 批量操作响应
export interface IBatchResponse<T = any> {
  success: T[]
  failed: Array<{
    id: string | number
    error: string
  }>
  total: number
  successCount: number
  failedCount: number
}

// 数据验证错误
export interface IValidationError {
  field: string
  message: string
  code?: string
}

// API状态响应
export interface IApiStatus {
  status: 'healthy' | 'degraded' | 'unhealthy'
  version: string
  uptime: number
  timestamp: number
  dependencies?: Array<{
    name: string
    status: 'healthy' | 'degraded' | 'unhealthy'
    latency?: number
  }>
}

// 业务模块状态
export interface IModuleStatus {
  name: string
  enabled: boolean
  version: string
  health: 'healthy' | 'degraded' | 'unhealthy'
  lastCheck: number
}

// 监控指标
export interface IMetricData {
  name: string
  value: number
  unit: string
  timestamp: number
  labels?: Record<string, string>
}

// 统计报表数据
export interface IReportData {
  period: string
  metrics: Array<{
    name: string
    value: number
    change?: number
    unit?: string
  }>
  trends?: Array<{
    date: string
    values: Record<string, number>
  }>
}

// 通知消息
export interface INotification {
  id: string
  type: 'info' | 'success' | 'warning' | 'error'
  title: string
  message: string
  timestamp: number
  read: boolean
  data?: Record<string, any>
}

// 用户会话信息
export interface IUserSession {
  userId: string
  username: string
  email?: string
  roles: string[]
  permissions: string[]
  expiresAt: number
  issuedAt: number
  ip?: string
  userAgent?: string
}

// 导出类型工具
export type ApiResponse<T = any> = IApiResponse<T>
export type PaginatedResponse<T = any> = IPaginatedResponse<T>
export type ErrorResponse = IErrorResponse
export type BatchResponse<T = any> = IBatchResponse<T>
export type ValidationError = IValidationError
export type ApiStatus = IApiStatus
export type ModuleStatus = IModuleStatus
export type MetricData = IMetricData
export type ReportData = IReportData
export type Notification = INotification
export type UserSession = IUserSession

// 类型守卫
export function isApiResponse<T>(data: any): data is IApiResponse<T> {
  return (
    data &&
    typeof data === 'object' &&
    'code' in data &&
    'message' in data &&
    'data' in data &&
    'success' in data
  )
}

export function isPaginatedResponse<T>(data: any): data is IPaginatedResponse<T> {
  return (
    data &&
    typeof data === 'object' &&
    'items' in data &&
    'total' in data &&
    'page' in data &&
    'pageSize' in data
  )
}

export function isErrorResponse(data: any): data is IErrorResponse {
  return (
    data &&
    typeof data === 'object' &&
    'code' in data &&
    'message' in data
  )
}

// 响应状态码常量
export const ResponseCodes = {
  SUCCESS: 0,
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  INTERNAL_ERROR: 500,
  SERVICE_UNAVAILABLE: 503,
  
  // 业务错误码
  VALIDATION_ERROR: 1001,
  BUSINESS_ERROR: 1002,
  DATA_NOT_FOUND: 1003,
  DUPLICATE_DATA: 1004,
  INVALID_PARAMETER: 1005,
  RATE_LIMIT_EXCEEDED: 1006
} as const

// 错误消息映射
export const ErrorMessages: Record<number, string> = {
  [ResponseCodes.BAD_REQUEST]: '请求参数错误',
  [ResponseCodes.UNAUTHORIZED]: '未授权，请重新登录',
  [ResponseCodes.FORBIDDEN]: '权限不足',
  [ResponseCodes.NOT_FOUND]: '资源不存在',
  [ResponseCodes.INTERNAL_ERROR]: '服务器内部错误',
  [ResponseCodes.SERVICE_UNAVAILABLE]: '服务暂时不可用',
  [ResponseCodes.VALIDATION_ERROR]: '数据验证失败',
  [ResponseCodes.BUSINESS_ERROR]: '业务逻辑错误',
  [ResponseCodes.DATA_NOT_FOUND]: '数据不存在',
  [ResponseCodes.DUPLICATE_DATA]: '数据重复',
  [ResponseCodes.INVALID_PARAMETER]: '参数无效',
  [ResponseCodes.RATE_LIMIT_EXCEEDED]: '请求频率过高'
}

// 创建成功响应
export function createSuccessResponse<T>(data: T, message: string = '请求成功'): IApiResponse<T> {
  return {
    code: ResponseCodes.SUCCESS,
    message,
    data,
    timestamp: Date.now(),
    success: true
  }
}

// 创建错误响应
export function createErrorResponse(
  code: number,
  message?: string,
  details?: Record<string, any>
): IApiResponse {
  return {
    code,
    message: message || ErrorMessages[code] || '未知错误',
    data: details || null,
    timestamp: Date.now(),
    success: false
  }
}