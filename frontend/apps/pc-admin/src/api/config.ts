import request, { type ApiResponse, type PageResponse } from '@/utils/request'

// 系统配置信息（匹配后端 SystemConfig 模型）
export interface SystemConfig {
  id: number
  configKey: string
  configValue: string
  configType: string       // system/business/security/integration
  configGroup: string      // basic/login/password/session/upload/email/sms
  configName: string
  description: string
  valueType: string        // string/number/boolean/json/list
  defaultValue: string
  enabled: boolean
  systemConfig: boolean
  createTime: string
  updateTime: string
  tenantId: number
}

// 配置查询参数
export interface ConfigQuery {
  configType?: string
  configGroup?: string
  pageNum?: number
  pageSize?: number
}

// 系统配置API
export const configApi = {
  // 分页查询配置
  getPage(params: ConfigQuery): Promise<ApiResponse<PageResponse<SystemConfig>>> {
    return request.get('/config/page', params)
  },

  // 获取配置列表
  getList(params: { configType?: string; configGroup?: string }): Promise<any> {
    return request.get('/config/list', params)
  },

  // 获取配置Map
  getMap(configGroup?: string): Promise<Record<string, string>> {
    return request.get('/config/map', { configGroup })
  },

  // 按配置键获取值
  getValue(configKey: string): Promise<any> {
    return request.get(`/config/value/${configKey}`)
  },

  // 保存配置
  save(data: Partial<SystemConfig>): Promise<any> {
    return request.post('/config/save', data)
  },

  // 保存配置值
  saveValue(configKey: string, configValue: string): Promise<any> {
    return request.post('/config/save-value', { configKey, configValue })
  },

  // 批量保存配置
  batchSave(configs: Record<string, string>): Promise<any> {
    return request.post('/config/batch-save', configs)
  },

  // 删除配置
  delete(configKey: string): Promise<any> {
    return request.delete(`/config/${configKey}`)
  },

  // 批量删除
  batchDelete(ids: number[]): Promise<any> {
    return request.delete('/config/batch', { data: ids } as any)
  },

  // 获取配置变更日志
  getChangeLogs(configKey: string): Promise<ApiResponse<any[]>> {
    return request.get(`/config/logs/${configKey}`)
  },

  // 刷新配置缓存
  refreshCache(configKey?: string): Promise<any> {
    if (configKey) {
      return request.post('/config/refresh-cache', null, { params: { configKey } })
    }
    return request.post('/config/refresh-cache')
  },

  // 导出配置
  exportConfigs(params?: { configType?: string; configGroup?: string }): Promise<any> {
    return request.get('/config/export', params)
  },

  // 获取配置类型列表
  getConfigTypes(): Promise<any> {
    return request.get('/config/types')
  },

  // 获取配置分组列表
  getConfigGroups(): Promise<any> {
    return request.get('/config/groups')
  }
}

export default configApi
