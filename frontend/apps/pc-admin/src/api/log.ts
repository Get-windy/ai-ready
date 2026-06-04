import request, { type ApiResponse, type PageResponse } from '@/utils/request'

// 操作日志信息
export interface OperationLog {
  id: number
  module: string
  operationType: string
  description: string
  requestUrl: string
  requestMethod: string
  operatorName: string
  ipAddress: string
  operationTime: string
  costTime: number
  status: number // 0: 成功, 1: 失败
  requestParams?: string
  responseResult?: string
}

// 日志查询参数
export interface LogQuery {
  module?: string
  operationType?: string
  operatorName?: string
  startDate?: string
  endDate?: string
  pageNum?: number
  pageSize?: number
}

// 操作日志API
export const logApi = {
  // 分页查询
  getPage(params: LogQuery): Promise<ApiResponse<PageResponse<OperationLog>>> {
    return request.get('/log/page', params)
  },

  // 获取详情
  getById(id: number): Promise<ApiResponse<OperationLog>> {
    return request.get(`/log/${id}`)
  },

  // 获取模块列表
  getModules(): Promise<ApiResponse<string[]>> {
    return request.get('/log/modules')
  },

  // 获取操作类型列表
  getOperationTypes(): Promise<ApiResponse<string[]>> {
    return request.get('/log/operation-types')
  },

  // 清空日志
  clearLogs(): Promise<ApiResponse<void>> {
    return request.delete('/log/clear')
  },

  // 导出日志
  exportLogs(params: LogQuery): Promise<ApiResponse<Blob>> {
    return request.get('/log/export', params, { responseType: 'blob' })
  }
}

export default logApi
