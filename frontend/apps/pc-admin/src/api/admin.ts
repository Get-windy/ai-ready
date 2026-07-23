import axios from 'axios'
import request from '@/utils/request'
import { getToken } from '@/utils/tokenRefresher'

/**
 * admin 平台域 API 封装
 *
 * 覆盖「系统管理/admin」菜单下数据管理、开发工具、平台参数、模块列表等页面
 * 使用的 core-api 自身端点（/data-source/**、/module/**、/config/**、/import-templates/**）。
 *
 * 后端响应两种形态（cn.aiedge.datasource/module/config/export 包控制器）：
 *  - 列表端点 → Map{records, total, ...}：标准 request 拦截器按 Page 格式透传，直接用 request.get
 *  - 操作端点 → Map{success, message, data?}：无 code===200，标准拦截器会误判为失败并弹错，
 *    故参照 ./erp/batch、./analytics expenseAnalyticsApi 的既定做法，原生 axios 自行解包
 *  - 少数端点返回裸 DTO（如 /import-templates/{id}/preview），同样需原生 axios 透传
 *
 * 注意：后端控制器读取 X-Tenant-Id 请求头，而全局拦截器约定的是 tenantId，两者都带上。
 */

// ── 通用 ────────────────────────────────────────────────

const API_PREFIX = '/api'

/** 与全局 request 拦截器一致的认证/租户请求头（后端读 X-Tenant-Id） */
function adminHeaders(): Record<string, string> {
  const tenantId = localStorage.getItem('tenantId') || '1'
  return {
    ...(getToken() ? { Authorization: `Bearer ${getToken()}` } : {}),
    'X-Tenant-Id': tenantId,
    tenantId
  }
}

/** {success, message, data?} 操作响应 */
interface SuccessBody<T = any> {
  success?: boolean
  message?: string
  data?: T
}

/** 操作类请求：解包 {success, message}，失败时抛出后端 message */
async function mutate<T = any>(method: 'post' | 'put' | 'delete', url: string, data?: any): Promise<T> {
  try {
    const response = await axios.request<SuccessBody<T>>({ method, url: API_PREFIX + url, data, headers: adminHeaders() })
    const body = response.data
    if (body && body.success === true) return body.data as T
    throw new Error(body?.message || '操作失败')
  } catch (e: any) {
    if (e?.response) {
      throw new Error(e.response.data?.message || `请求失败 (${e.response.status})`)
    }
    throw e instanceof Error ? e : new Error(String(e))
  }
}

/** 裸 DTO 透传 GET（既非 records+total，也非 code===200 包装的端点） */
async function getRaw<T = any>(url: string, params?: Record<string, any>): Promise<T> {
  try {
    const response = await axios.get<T>(API_PREFIX + url, { params, headers: adminHeaders() })
    return response.data
  } catch (e: any) {
    if (e?.response) {
      throw new Error(e.response.data?.message || `请求失败 (${e.response.status})`)
    }
    throw e instanceof Error ? e : new Error(String(e))
  }
}

/** 后端内存分页响应（records/total/current/size/pages） */
export interface AdminPage<T = any> {
  records: T[]
  total: number
  current?: number
  size?: number
  pages?: number
}

// ═══ 数据源连接（/data-source） ═══

/** 数据源连接（匹配后端 DataSource 模型） */
export interface DataSourceItem {
  id: number
  name: string
  dbType: string
  host: string
  port: number
  databaseName: string
  username: string
  password?: string
  /** connected/disconnected/error */
  status: string
  description?: string
  tenantId?: number
  createTime?: string
  updateTime?: string
  createBy?: string
  updateBy?: string
}

export const dataSourceApi = {
  /** 数据源分页（keyword/page/pageSize） */
  page(params?: { keyword?: string; page?: number; pageSize?: number }): Promise<AdminPage<DataSourceItem>> {
    return request.get('/data-source/list', params)
  },
  /** 新建数据源 */
  create(data: Partial<DataSourceItem>): Promise<DataSourceItem> {
    return mutate('post', '/data-source/', data)
  },
  /** 更新数据源（后端为 null-safe 部分更新，密码留空不会覆盖） */
  update(id: number, data: Partial<DataSourceItem>): Promise<DataSourceItem> {
    return mutate('put', `/data-source/${id}`, data)
  },
  /** 删除数据源 */
  remove(id: number): Promise<any> {
    return mutate('delete', `/data-source/${id}`)
  },
  /** 测试连接（后端 success=false 表示连接失败，会抛出后端提示） */
  test(id: number): Promise<any> {
    return mutate('post', '/data-source/test', { id })
  }
}

// ═══ 慢查询（/data-source/slow-query） ═══

/** 慢查询记录（匹配后端 SlowQuery 模型） */
export interface SlowQueryItem {
  id: number
  dataSourceId: number
  queryText: string
  queryTimeMs: number
  lockTimeMs?: number
  rowsExamined?: number
  rowsSent?: number
  queryTime?: string
  databaseName?: string
  userName?: string
  hostInfo?: string
  createTime?: string
}

export const slowQueryApi = {
  /** 慢查询分页（dataSourceId/page/pageSize） */
  page(params?: { dataSourceId?: number; page?: number; pageSize?: number }): Promise<AdminPage<SlowQueryItem>> {
    return request.get('/data-source/slow-query/list', params)
  },
  /** 慢查询全量导出（{success, records, total}，拦截器按 records+total 透传） */
  exportAll(dataSourceId?: number): Promise<AdminPage<SlowQueryItem>> {
    return request.get('/data-source/slow-query/export', dataSourceId ? { dataSourceId } : {})
  }
}

// ═══ 数据备份（/data-source/backup） ═══

/** 备份记录（匹配后端 BackupRecord 模型） */
export interface BackupRecordItem {
  id: number
  dataSourceId: number
  backupName: string
  /** full/incremental */
  backupType: string
  filePath?: string
  fileSize?: number
  /** running/success/failed */
  status: string
  startTime?: string
  endTime?: string
  errorMessage?: string
  createBy?: string
  createTime?: string
}

export const backupApi = {
  /** 备份记录分页（dataSourceId/page/pageSize） */
  page(params?: { dataSourceId?: number; page?: number; pageSize?: number }): Promise<AdminPage<BackupRecordItem>> {
    return request.get('/data-source/backup/list', params)
  },
  /** 创建备份（backupName/backupType 可空，后端有默认值） */
  create(data: { dataSourceId: number; backupName?: string; backupType?: string }): Promise<BackupRecordItem> {
    return mutate('post', '/data-source/backup/create', data)
  },
  /** 从备份恢复（危险操作） */
  restore(id: number): Promise<any> {
    return mutate('post', `/data-source/backup/${id}/restore`)
  },
  /** 删除备份记录 */
  remove(id: number): Promise<any> {
    return mutate('delete', `/data-source/backup/${id}`)
  }
}

// ═══ 数据同步任务（/data-source/sync） ═══

/** 同步任务（匹配后端 SyncTask 模型） */
export interface SyncTaskItem {
  id: number
  sourceId: number
  targetId: number
  taskName: string
  /** full/incremental */
  syncType: string
  cronExpression?: string
  /** running/paused/stopped */
  status: string
  lastSyncTime?: string
  nextSyncTime?: string
  description?: string
  createTime?: string
  updateTime?: string
}

export const syncTaskApi = {
  /** 同步任务分页（page/pageSize） */
  page(params?: { page?: number; pageSize?: number }): Promise<AdminPage<SyncTaskItem>> {
    return request.get('/data-source/sync/list', params)
  },
  /** 新建同步任务 */
  create(data: Partial<SyncTaskItem>): Promise<SyncTaskItem> {
    return mutate('post', '/data-source/sync/', data)
  },
  /** 更新同步任务（后端按 body 整体更新） */
  update(id: number, data: Partial<SyncTaskItem>): Promise<SyncTaskItem> {
    return mutate('put', `/data-source/sync/${id}`, data)
  },
  /** 删除同步任务 */
  remove(id: number): Promise<any> {
    return mutate('delete', `/data-source/sync/${id}`)
  },
  /** 立即执行同步任务 */
  execute(id: number): Promise<any> {
    return mutate('post', `/data-source/sync/${id}/execute`)
  }
}

// ═══ 数据清理规则（/data-source/cleanup） ═══

/** 数据清理规则（匹配后端 CleanupRule 模型） */
export interface CleanupRuleItem {
  id: number
  ruleName: string
  targetTable: string
  conditionColumn?: string
  retentionDays?: number
  cronExpression?: string
  /** running/paused/stopped */
  status: string
  description?: string
  createTime?: string
  updateTime?: string
}

export const cleanupRuleApi = {
  /** 清理规则分页（page/pageSize） */
  page(params?: { page?: number; pageSize?: number }): Promise<AdminPage<CleanupRuleItem>> {
    return request.get('/data-source/cleanup/list', params)
  },
  /** 新建清理规则 */
  create(data: Partial<CleanupRuleItem>): Promise<CleanupRuleItem> {
    return mutate('post', '/data-source/cleanup/', data)
  },
  /** 更新清理规则（启停用 status: running/paused 整体提交） */
  update(id: number, data: Partial<CleanupRuleItem>): Promise<CleanupRuleItem> {
    return mutate('put', `/data-source/cleanup/${id}`, data)
  },
  /** 删除清理规则 */
  remove(id: number): Promise<any> {
    return mutate('delete', `/data-source/cleanup/${id}`)
  },
  /** 立即执行清理 */
  execute(id: number): Promise<any> {
    return mutate('post', `/data-source/cleanup/${id}/execute`)
  }
}

// ═══ 系统模块（/module） ═══

/** 系统模块（匹配后端 SysModule 模型） */
export interface ModuleItem {
  id: number
  moduleName: string
  moduleCode: string
  version?: string
  description?: string
  /** 0=禁用 1=启用 */
  status: number
  icon?: string
  sortOrder?: number
  createTime?: string
  updateTime?: string
}

/** 模块版本（匹配后端 SysModuleVersion 模型） */
export interface ModuleVersionItem {
  id: number
  moduleId: number
  moduleName?: string
  version: string
  changelog?: string
  releaseStatus?: string
  publisher?: string
  releaseTime?: string
  createTime?: string
}

export const moduleApi = {
  /** 模块列表（不分页，{records, total}） */
  list(): Promise<AdminPage<ModuleItem>> {
    return request.get('/module/list')
  },
  /** 更新模块基础信息 */
  update(id: number, data: Partial<ModuleItem>): Promise<ModuleItem> {
    return mutate('put', `/module/${id}`, data)
  },
  /** 切换模块启用/停用状态 */
  toggleStatus(id: number): Promise<any> {
    return mutate('put', `/module/${id}/status`)
  },
  /** 模块版本列表（可按 moduleId 过滤，{records, total}） */
  versions(moduleId?: number): Promise<AdminPage<ModuleVersionItem>> {
    return request.get('/module/versions', moduleId ? { moduleId } : {})
  }
}

// ═══ 平台参数（/config） ═══

/** 平台参数（匹配后端 SystemConfig 模型） */
export interface PlatformConfigItem {
  id: number
  configKey: string
  configValue: string
  /** system/business/security/integration/notification */
  configType?: string
  configGroup?: string
  configName?: string
  description?: string
  valueType?: string
  defaultValue?: string
  enabled?: boolean
  systemConfig?: boolean
  createTime?: string
  updateTime?: string
}

/** 参数变更日志（匹配后端 ConfigChangeLog 模型） */
export interface ConfigChangeLogItem {
  logId: number
  configId?: number
  configKey?: string
  oldValue?: string
  newValue?: string
  changeType?: string
  changeReason?: string
  operatorName?: string
  operateTime?: string
  clientIp?: string
}

export const platformConfigApi = {
  /** 参数列表（configType/configGroup 过滤，{records, total}） */
  list(params?: { configType?: string; configGroup?: string }): Promise<AdminPage<PlatformConfigItem>> {
    return request.get('/config/list', params)
  },
  /** 保存参数值（{configKey, configValue}） */
  saveValue(configKey: string, configValue: string): Promise<any> {
    return mutate('post', '/config/save-value', { configKey, configValue })
  },
  /** 参数变更日志（裸数组，拦截器透传） */
  changeLogs(configKey: string): Promise<ConfigChangeLogItem[]> {
    return request.get(`/config/logs/${configKey}`)
  },
  /** 刷新配置缓存（configKey 为空则全量刷新） */
  refreshCache(configKey?: string): Promise<any> {
    return mutate('post', '/config/refresh-cache' + (configKey ? `?configKey=${encodeURIComponent(configKey)}` : ''))
  },
  /** 配置类型（裸数组，拦截器透传） */
  types(): Promise<{ code: string; name: string }[]> {
    return request.get('/config/types')
  },
  /** 配置分组（裸数组，拦截器透传） */
  groups(): Promise<{ code: string; name: string }[]> {
    return request.get('/config/groups')
  }
}

// ═══ 导入模板（/import-templates） ═══

/** 导入模板（匹配后端 ImportTemplateDefinition，fields 为嵌套字段定义） */
export interface ImportTemplateItem {
  templateId: string
  dataType: string
  templateName: string
  description?: string
  version?: string
  fields?: { fieldName: string; fieldTitle?: string; fieldType?: string; required?: boolean }[]
  maxImportRows?: number
  strictValidation?: boolean
  createTime?: string
  updateTime?: string
}

/** 模板预览字段（匹配后端 TemplatePreview.FieldPreview） */
export interface TemplatePreviewField {
  fieldName: string
  fieldTitle?: string
  fieldType?: string
  required?: boolean
  maxLength?: number
  dropdownOptions?: Record<string, string>
  sampleValue?: string
  description?: string
}

/** 模板预览（匹配后端 TemplatePreview） */
export interface TemplatePreview {
  templateId: string
  templateName: string
  description?: string
  maxImportRows?: number
  fields: TemplatePreviewField[]
}

export const importTemplateApi = {
  /** 模板列表（裸数组，拦截器透传） */
  list(): Promise<ImportTemplateItem[]> {
    return request.get('/import-templates')
  },
  /** 模板预览（裸 DTO，需原生 axios 透传） */
  preview(templateId: string): Promise<TemplatePreview> {
    return getRaw(`/import-templates/${templateId}/preview`)
  },
  /** 删除模板 */
  remove(templateId: string): Promise<any> {
    return mutate('delete', `/import-templates/${templateId}`)
  },
  /** 下载模板 xlsx（blob） */
  async download(templateId: string): Promise<Blob> {
    const response = await axios.get(`${API_PREFIX}/import-templates/${templateId}/download`, {
      responseType: 'blob',
      headers: adminHeaders()
    })
    return response.data as Blob
  }
}
