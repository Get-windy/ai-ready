import request, { type ApiResponse, type PageResponse } from '@/utils/request'

/**
 * 系统配置信息（匹配后端 SystemConfig 实体，真实落库表 `sys_config`）
 *
 * 注意：本表的 `id` 来自 PG 序列 `sys_config_id_seq`（**不是雪花 ID**，实测值为 1..N 的小整数），
 * 因此这里用 `number` 是安全的；页面渲染也**不使用 id 做业务判断**（一律按 `configKey`）。
 */
export interface SystemConfig {
  id: number
  configKey: string
  configValue: string
  configType: string       // system/security/business/notification/integration
  configGroup: string      // basic/login/password/session/upload/email/sms（旧分组维度）
  configName: string
  description: string
  valueType: string        // string/number/boolean/enum/list
  defaultValue: string     // ⚠️ 后端不落库（sys_config 无 default_value 列）
  enabled: boolean
  systemConfig: boolean    // 是否内置（落库列名 builtin）
  sortOrder?: number
  // ── 系统参数页专有字段（Flyway V11.393.0 新增列）──
  navGroup?: string        // 左列纵向视图编码：industry/flow/bill/stock/finance/data_perm/notify/other
  parentKey?: string       // 父配置键（「可展开的父开关」的子项）
  helpText?: string        // `?` 帮助气泡文案
  tipText?: string         // 灰色「温馨提示」文案
  locked?: boolean         // 不可逆配置锁定（锁定后禁止修改；读取时按「商品是否引用」实时算出）
  lockedReason?: string    // 锁定原因（后端实时计算，不落库；用于悬浮说明「为什么不能改」）
  createTime: string
  updateTime: string
  tenantId: number
}

/** 系统参数页左列纵向视图（GET /config/nav-groups，8 个） */
export interface ConfigNavGroup {
  code: string
  name: string
  sortOrder: number
}

/** 枚举型 / 多段型配置项的候选值 */
export interface ConfigValueOption {
  value: string
  label: string
}

/**
 * `/config/value-options` 的返回：`配置键 → 扁平选项集 | 分段选项集`
 *
 * 两种形态（前端按「首元素是不是数组」区分，不需要按配置键硬编码）：
 *   · `ConfigValueOption[]`       单一枚举/单选下拉的候选值
 *   · `ConfigValueOption[][]`     多段控件（如「批次条码规则生成 = 第1段 + 第2段 + 第3段」）
 *                                 的候选值，**外层下标 = 段序号**（ql361 实测第 3 段的取值域
 *                                 与第 1、2 段不同，所以候选值必须按段给）
 */
export type ConfigValueOptionsPayload = Record<string, ConfigValueOption[] | ConfigValueOption[][]>

/** 配置查询参数 */
export interface ConfigQuery {
  configType?: string
  configGroup?: string
  /** 左列视图编码（系统参数页按视图取数） */
  navGroup?: string
  configKey?: string
  configName?: string
  enabled?: boolean
  systemConfig?: boolean
  pageNum?: number
  pageSize?: number
}

/** 列表查询参数（不分页的 /config/list） */
export interface ConfigListQuery {
  configType?: string
  configGroup?: string
  navGroup?: string
  configKey?: string
  configName?: string
  enabled?: boolean
  systemConfig?: boolean
}

// 系统配置API
export const configApi = {
  // 分页查询配置（真分页：total 独立 COUNT、records 走 LIMIT/OFFSET）
  getPage(params: ConfigQuery): Promise<ApiResponse<PageResponse<SystemConfig>>> {
    return request.get('/config/page', params)
  },

  // 获取配置列表（不分页；系统参数页按 navGroup 取整个视图）
  getList(params: ConfigListQuery): Promise<any> {
    return request.get('/config/list', params)
  },

  // 获取系统参数页的左列纵向视图（8 个）
  getNavGroups(): Promise<ConfigNavGroup[]> {
    return request.get('/config/nav-groups')
  },

  // 获取枚举/多段配置项的候选值字典（按配置键索引，含分段形态）
  getValueOptions(): Promise<ConfigValueOptionsPayload> {
    return request.get('/config/value-options')
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

  // 批量保存配置（系统参数页底部统一「保存」用：一次提交当前视图的全部键值）
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

  // 刷新配置缓存（返回 cleared = 实际清理的历史缓存键数量）
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
