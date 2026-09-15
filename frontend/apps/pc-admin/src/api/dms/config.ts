/**
 * DMS 系统配置 API 模块
 * 后端: ConfigController (/api/dms/config)
 */
import request from '@/utils/request'

// ── 系统配置 ──────────────────────────────────────────
export interface DmsConfig {
  id: number
  configKey: string
  /** 敏感键（Key/密钥/令牌/密码）后端只返回**掩码**，明文不出服务端 */
  configValue: string
  configDesc: string
  scope: string
  /** 是否敏感配置（后端标记） */
  secret?: boolean
  /** 是否已配置（敏感键「留空=不修改」判断与「清除」按钮显隐用） */
  configured?: boolean
  createTime: string
  updateTime: string
}

// ── 参数中心（元数据驱动） ──────────────────────────────

/** 参数类型（决定控件） */
export type ConfigValueType = 'NUMBER' | 'TEXT' | 'BOOLEAN' | 'ENUM' | 'JSON' | 'TIME_RANGE'

export interface ConfigEnumOption { label: string; value: string }

/** 参数元数据（后端 DmsConfigMeta） */
export interface DmsConfigMeta {
  group: string
  groupText: string
  configKey: string
  name: string
  valueType: ConfigValueType
  defaultValue?: string
  min?: number
  max?: number
  unit?: string
  options?: ConfigEnumOption[]
  editable?: boolean
  /** HOT-保存即热生效 / RESTART-需重启 */
  effect?: string
  secret?: boolean
  desc?: string
}

/** 参数中心列表行 = 当前值 + 元数据 */
export interface ConfigItem extends DmsConfigMeta {
  configValue?: string
  configured?: boolean
  tenantOverride?: boolean
  scope?: string
  updateTime?: string
}

export interface ConfigQuery {
  pageNum?: number
  pageSize?: number
  keyword?: string
  group?: string
  valueType?: string
  editable?: boolean
  effect?: string
  configuredOnly?: boolean
  /** 作用域：TENANT=仅租户覆盖 / GLOBAL=仅继承全局 */
  scope?: string
  /** 仅看租户覆盖(true) / 仅看继承全局(false) */
  tenantOverride?: boolean
  updateTimeStart?: string
  updateTimeEnd?: string
}

export interface ConfigMetaPayload {
  groups: { key: string; text: string; count: number }[]
  items: DmsConfigMeta[]
}

// ── 参数集导入 / 导出（《配送参数开发文档》§3.4 / §3.5） ──

/** 导出物中的单项（`configValue` 对敏感键为空，不回传明文） */
export interface ConfigExportItem {
  configKey: string
  name?: string
  group?: string
  valueType?: string
  unit?: string
  configValue?: string
  secret?: boolean
  configured?: boolean
  defaultValue?: string
}

/** 参数集导出物（结构与导入请求体一致，可直接回灌） */
export interface ConfigExportPayload {
  schema: string
  exportedAt: string
  tenantId: number
  count: number
  items: ConfigExportItem[]
}

export interface ConfigImportPreviewRow {
  configKey: string
  name?: string
  group?: string
  valueType?: string
  oldValue?: string
  newValue?: string
  /** CREATE=新增租户覆盖 / UPDATE=更新租户覆盖 / SKIP=跳过 / SAME=值相同 */
  action: 'CREATE' | 'UPDATE' | 'SKIP' | 'SAME'
  reason?: string
  secret?: boolean
}

export interface ConfigImportResult {
  mode: 'PREVIEW' | 'APPLY'
  total: number
  changed: number
  unchanged: number
  skipped: number
  failed: { configKey: string; reason: string }[]
  preview: ConfigImportPreviewRow[]
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

  /**
   * 更新配置（后端接收裸字符串请求体）
   * 敏感键留空 = 不修改（后端语义），需清空请用 clear()
   */
  update(key: string, data: { configValue: string }, tenantId = 0): Promise<DmsConfig> {
    return request.put(`/dms/config/${key}`, JSON.stringify(data.configValue), { params: { tenantId } })
  },

  /** 清空配置值（等同撤销配置；敏感键也支持） */
  clear(key: string, tenantId = 0): Promise<DmsConfig> {
    return request.delete(`/dms/config/${key}`, { params: { tenantId } })
  },

  // ── 参数中心（金标准：元数据驱动 / 分页 / 批量保存 / 恢复默认 / 变更历史） ──
  /** 参数中心分页（行内含元数据） */
  page(params: ConfigQuery): Promise<{ records: ConfigItem[]; total: number }> {
    return request.get('/dms/config/page', { params })
  },

  /** 参数元数据 + 分组树计数 */
  meta(): Promise<ConfigMetaPayload> {
    return request.get('/dms/config/meta')
  },

  /** 批量保存（事务；后端逐项类型校验） */
  batch(items: { configKey: string; configValue: string }[]) {
    return request.put('/dms/config/batch', items)
  },

  /** 恢复默认值 */
  reset(key: string, tenantId = 0) {
    return request.post(`/dms/config/${key}/reset`, null, { params: { tenantId } })
  },

  /** 变更历史（审计） */
  history(key: string, tenantId = 0): Promise<any[]> {
    return request.get(`/dms/config/${key}/history`, { params: { tenantId } })
  },

  /** 回滚到指定历史版本 */
  rollback(key: string, historyId: number, tenantId = 0) {
    return request.post(`/dms/config/${key}/rollback`, null, { params: { tenantId, historyId } })
  },

  /** 导出参数集（JSON 参数清单；按筛选条件过滤；敏感键不导出明文） */
  exportConfigs(params: ConfigQuery = {}): Promise<ConfigExportPayload> {
    return request.get('/dms/config/export', { params })
  },

  /** 导入参数集：mode=PREVIEW 预览差异（不落库）/ APPLY 落库；存在失败项整体拒绝 */
  importConfigs(payload: {
    mode: 'PREVIEW' | 'APPLY'
    overwrite?: boolean
    items: ConfigExportItem[]
  }): Promise<ConfigImportResult> {
    return request.post('/dms/config/import', payload)
  },
}
