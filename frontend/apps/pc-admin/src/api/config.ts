import request, { type ApiResponse, type PageResponse } from '@/utils/request'

// 系统配置信息
export interface ConfigInfo {
  id: number
  configKey: string
  configValue: string
  description: string
  groupName: string
  createTime?: string
  updateTime?: string
}

// 配置查询参数
export interface ConfigQuery {
  configKey?: string
  groupName?: string
  pageNum?: number
  pageSize?: number
}

// 配置保存请求
export interface ConfigSaveRequest {
  configKey: string
  configValue: string
  description: string
  groupName: string
}

// 配置更新请求
export interface ConfigUpdateRequest extends ConfigSaveRequest {
  id: number
}

// 系统配置API
export const configApi = {
  // 分页查询配置
  getPage(params: ConfigQuery): Promise<ApiResponse<PageResponse<ConfigInfo>>> {
    return request.get('/config/page', params)
  },

  // 获取配置详情
  getById(id: number): Promise<ApiResponse<ConfigInfo>> {
    return request.get(`/config/${id}`)
  },

  // 按配置键获取配置
  getByKey(configKey: string): Promise<ApiResponse<ConfigInfo>> {
    return request.get(`/config/key/${configKey}`)
  },

  // 创建配置
  create(data: ConfigSaveRequest): Promise<ApiResponse<ConfigInfo>> {
    return request.post('/config', data)
  },

  // 更新配置
  update(data: ConfigUpdateRequest): Promise<ApiResponse<ConfigInfo>> {
    return request.put(`/config/${data.id}`, data)
  },

  // 删除配置
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/config/${id}`)
  },

  // 批量删除
  batchDelete(ids: number[]): Promise<ApiResponse<void>> {
    return request.delete('/config/batch', { data: ids })
  },

  // 获取所有配置分组
  getGroups(): Promise<ApiResponse<string[]>> {
    return request.get('/config/groups')
  },

  // 刷新配置缓存
  refreshCache(): Promise<ApiResponse<void>> {
    return request.post('/config/refresh-cache')
  }
}

export default configApi
