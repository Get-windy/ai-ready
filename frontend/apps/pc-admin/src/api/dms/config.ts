/**
 * DMS 系统配置 API 模块
 * 后端: ConfigController (/api/dms/config)
 */
import request from '@/utils/request'

// ── 系统配置 ──────────────────────────────────────────
export interface DmsConfig {
  id: number
  configKey: string
  configValue: string
  configDesc: string
  scope: string
  createTime: string
  updateTime: string
}

export const configApi = {
  /**
   * 获取租户全部配置（不分页）
   * 兼容旧调用方把查询参数对象当首参传入（非数字一律按默认租户 0 处理）
   */
  list(tenantId: number | Record<string, any> = 0): Promise<DmsConfig[]> {
    const tid = typeof tenantId === 'number' ? tenantId : 0
    return request.get('/dms/config/list', { params: { tenantId: tid } })
  },

  /** 根据 key 获取单个配置 */
  get(key: string, tenantId = 0): Promise<DmsConfig> {
    return request.get(`/dms/config/${key}`, { params: { tenantId } })
  },

  /** 更新配置（后端接收裸字符串请求体） */
  update(key: string, data: { configValue: string }, tenantId = 0): Promise<DmsConfig> {
    return request.put(`/dms/config/${key}`, JSON.stringify(data.configValue), { params: { tenantId } })
  },
}
