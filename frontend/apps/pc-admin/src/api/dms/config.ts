/**
 * DMS 系统配置 API 模块
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
  /** 获取配置列表（不分页） */
  list() { return request.get('/dms/config') },

  /** 根据 key 获取单个配置 */
  get(key: string) { return request.get(`/dms/config/${key}`) },

  /** 更新配置 */
  update(key: string, data: { configValue: string }) { return request.put(`/dms/config/${key}`, data) },
}
