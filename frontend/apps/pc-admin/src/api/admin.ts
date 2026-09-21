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
  const headers: Record<string, string> = {
    ...(getToken() ? { Authorization: `Bearer ${getToken()}` } : {})
  }
  // 仅在拿到真实租户时才发送：不再回落 '1'（回落会让未初始化会话冒充平台租户，
  // 且与后端 TenantHeaderInterceptor 的会话租户校验冲突而 403）
  const tenantId = localStorage.getItem('tenantId')
  if (tenantId) {
    headers['X-Tenant-Id'] = tenantId
    headers.tenantId = tenantId
  }
  return headers
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

/** 操作类请求：返回完整 {success, message, data?} 包装体（需要读 message / 计数类字段时用） */
async function mutateRaw<T = any>(method: 'post' | 'put' | 'delete', url: string, data?: any): Promise<SuccessBody<T>> {
  try {
    const response = await axios.request<SuccessBody<T>>({ method, url: API_PREFIX + url, data, headers: adminHeaders() })
    const body = response.data
    if (body && body.success === true) return body
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
  /**
   * 当前**生效**的数据库连接——本系统自身连的那个库，不是下方登记的外部数据源。
   * 后端从运行时 DataSource 读 JDBC 元数据；密码不返回，用户名已脱敏。
   */
  current(): Promise<Record<string, any>> {
    return request.get('/data-source/current')
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
  /** pending/running/success/failed —— 备份本身（pg_dump）的状态 */
  status: string
  startTime?: string
  endTime?: string
  errorMessage?: string
  /** 最近一次恢复状态: none/dispatched/success/failed（后端 V11.409.0 新增列） */
  restoreStatus?: string
  restoreTime?: string
  restoreMessage?: string
  createBy?: string
  createTime?: string
}

export const backupApi = {
  /** 备份记录分页（dataSourceId/page/pageSize） */
  page(params?: { dataSourceId?: number; page?: number; pageSize?: number }): Promise<AdminPage<BackupRecordItem>> {
    return request.get('/data-source/backup/list', params)
  },
  /** 创建备份（backupName/backupType 可空，后端有默认值；后端异步执行 pg_dump） */
  create(data: { dataSourceId?: number; backupName?: string; backupType?: string }): Promise<BackupRecordItem> {
    return mutate('post', '/data-source/backup/create', data)
  },
  /**
   * 从备份恢复（危险操作）
   * @param confirm 后端强制要求显式二次确认开关，缺省/ false 会被直接拒绝（不是前端能绕过的）
   */
  restore(id: number, confirm = true): Promise<any> {
    return mutate('post', `/data-source/backup/${id}/restore`, { confirm })
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
  /** running/paused/stopped —— 启停开关，不是执行状态 */
  status: string
  /** 最后一次「成功投递给同步引擎」的时间 */
  lastSyncTime?: string
  nextSyncTime?: string
  /** 最近一次执行的发起时间（后端 V11.409.0 新增列） */
  lastRunTime?: string
  /** 最近一次执行结论: running/dispatched/failed（后端 V11.409.0 新增列） */
  lastRunStatus?: string
  lastRunMessage?: string
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
  /** running/paused/stopped —— 启停开关，不是执行状态 */
  status: string
  /** 最近一次执行时间（后端 V11.409.0 新增列） */
  lastRunTime?: string
  /** 最近一次执行结论: success/failed/rejected（后端 V11.409.0 新增列） */
  lastRunStatus?: string
  /** 最近一次实际删除行数（后端 V11.409.0 新增列） */
  lastDeletedCount?: number
  lastRunResult?: string
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
  /**
   * 立即执行清理
   * @param confirm 后端强制要求显式二次确认开关，缺省/ false 会被直接拒绝
   * @param dryRun  true 时只预统计「将删除 N 行」，不删任何数据
   */
  execute(id: number, confirm = true, dryRun = false): Promise<any> {
    return mutate('post', `/data-source/cleanup/${id}/execute`, { confirm, dryRun })
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
  /** 模块列表（分页 + 名称/编码模糊 + 状态筛选，{records, total}） */
  list(params?: { keyword?: string; status?: number; pageNum?: number; pageSize?: number }): Promise<AdminPage<ModuleItem>> {
    return request.get('/module/list', { params })
  },
  /** 新增模块 */
  create(data: Partial<ModuleItem>): Promise<ModuleItem> {
    return mutate('post', '/module', data)
  },
  /** 更新模块基础信息 */
  update(id: number, data: Partial<ModuleItem>): Promise<ModuleItem> {
    return mutate('put', `/module/${id}`, data)
  },
  /** 删除模块（后端为逻辑删除） */
  remove(id: number): Promise<any> {
    return mutate('delete', `/module/${id}`)
  },
  /** 切换模块启用/停用状态 */
  toggleStatus(id: number): Promise<any> {
    return mutate('put', `/module/${id}/status`)
  },
  /** 模块版本列表（分页 + 模块/发布状态/版本号筛选，{records, total}） */
  versions(params?: { moduleId?: number; releaseStatus?: string; version?: string; pageNum?: number; pageSize?: number }): Promise<AdminPage<ModuleVersionItem>> {
    return request.get('/module/versions', { params })
  },
  /** 模块发布记录（与 versions 同源：本系统无独立发布单实体） */
  releases(params?: { moduleId?: number; releaseStatus?: string; version?: string; pageNum?: number; pageSize?: number }): Promise<AdminPage<ModuleVersionItem>> {
    return request.get('/module/releases', { params })
  },
  /** 发布新版本（{id} 语义是 moduleId） */
  publish(moduleId: number, data: { version: string; changelog?: string }): Promise<ModuleVersionItem> {
    return mutate('post', `/module/${moduleId}/publish`, data)
  },
  /** 回滚：把模块当前版本号回写为某个已存在的历史版本 */
  rollback(moduleId: number, version: string): Promise<any> {
    return mutate('post', `/module/${moduleId}/rollback`, { version })
  },
  /** 模块使用统计（days=统计窗口天数，默认 30；返回 {records, total, summary, windowDays, windowStart}） */
  usage(days = 30): Promise<any> {
    return request.get('/module/usage', { params: { days } })
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
  /**
   * 参数分页（后端真分页 LIMIT/OFFSET + 独立 COUNT）
   * configKey / configName 在后端是 LIKE 模糊匹配，故可作为「参数键」查询条件直接下发。
   */
  page(params?: { configKey?: string; configGroup?: string; pageNum?: number; pageSize?: number }): Promise<AdminPage<PlatformConfigItem>> {
    return request.get('/config/page', params)
  },
  /** 新增参数（全量保存；落库后返回回读行） */
  create(data: Partial<PlatformConfigItem>): Promise<PlatformConfigItem> {
    return mutate('post', '/config/save', data)
  },
  /** 保存参数值（{configKey, configValue}） */
  saveValue(configKey: string, configValue: string): Promise<any> {
    return mutate('post', '/config/save-value', { configKey, configValue })
  },
  /** 按参数键删除（后端逻辑删除；内置参数会被后端拒绝并返回 success:false） */
  remove(configKey: string): Promise<any> {
    return mutate('delete', `/config/${encodeURIComponent(configKey)}`)
  },
  /** 参数变更日志（裸数组，拦截器透传） */
  changeLogs(configKey: string): Promise<ConfigChangeLogItem[]> {
    return request.get(`/config/logs/${configKey}`)
  },
  /** 刷新配置缓存（configKey 为空则全量刷新）；返回体含 cleared=实际清理的历史缓存键数量 */
  refreshCache(configKey?: string): Promise<{ success?: boolean; message?: string; cleared?: number }> {
    return mutateRaw('post', '/config/refresh-cache' + (configKey ? `?configKey=${encodeURIComponent(configKey)}` : ''))
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

// ═══ 平台设置：邮件 / 短信 / 存储 / 安全策略配置（cn.aiedge.platform） ═══
//
// 契约（2026-09-19 实测，以源码为准 —— 开发文档 §6 记的「全部 404」是装配修复前的旧结论）：
//   GET  /mail/config            → {code:200, data:<MailConfig>, message}  → 拦截器拆包后即实体本体
//   POST /mail/config            → {success:true, config:<MailConfig>}     → 无 code，拦截器整体透传
//   POST /mail/test              → {success:boolean, message}              → 必须读 success 字段
//   短信同款（/sms/**）；存储是 /storage-config/**（注意不是 /storage/**，后者是文件读写端点）
//   GET  /system/security/policy → 实测 500（见页面 a-alert 的取证说明）
//   POST /system/security/policy/save → {code:200, data:<SecurityPolicy>, message}
//
// 测试类端点（/test）后端当前是桩：ServiceImpl 只 log 后 return true，
// 因此页面必须把「后端恒真」这一事实显式标注，不能把 success=true 当成真实连通性证据。

/** 邮件配置（匹配后端 cn.aiedge.platform.model.MailConfig；id/tenantId 为雪花 BIGINT，序列化为字符串） */
export interface PlatformMailConfig {
  id?: string
  /** SMTP 服务器地址 */
  host?: string
  port?: number
  /** 加密方式：none/ssl/tls（DB 与后端 Schema 注释存在大小写分歧） */
  encryption?: string
  username?: string
  password?: string
  fromAddress?: string
  enabled?: boolean
  tenantId?: string
  createTime?: string
  updateTime?: string
}

/** 短信配置（匹配后端 SmsConfig） */
export interface PlatformSmsConfig {
  id?: string
  /** 服务商：aliyun/tencent/huawei（后端 Schema 注释只声明 3 项，前端历史值域多一个 qiniu） */
  provider?: string
  accessKey?: string
  accessSecret?: string
  signName?: string
  enabled?: boolean
  tenantId?: string
  createTime?: string
  updateTime?: string
}

/** 存储配置（匹配后端 StorageConfig 实体；注意与 cn.aiedge.storage.config.StorageConfig 同名不同物） */
export interface PlatformStorageConfig {
  id?: string
  /** 存储类型：后端 Schema 注释为 local/oss/cos/s3，前端历史值域为 local/aliyun/tencent/qiniu/minio */
  storageType?: string
  localPath?: string
  localUrlPrefix?: string
  endpoint?: string
  bucket?: string
  accessKey?: string
  accessSecret?: string
  enabled?: boolean
  tenantId?: string
  createTime?: string
  updateTime?: string
}

/** 安全策略（匹配后端 SecurityPolicy；字段名与前端 reactive 分组不同名，见页面映射表） */
export interface PlatformSecurityPolicy {
  id?: string
  lockThreshold?: number
  lockDuration?: number
  captchaEnabled?: boolean
  twoFactorEnabled?: boolean
  passwordMinLength?: number
  requireUpper?: boolean
  requireLower?: boolean
  requireDigit?: boolean
  requireSpecial?: boolean
  passwordExpireDays?: number
  /** 后端 Schema 注释为「秒」，DB 默认 60，前端历史 label 为「分钟」—— 单位口径未统一 */
  sessionTimeout?: number
  singleDevice?: boolean
  /** 后端为逗号分隔（前端历史 placeholder 是「每行一个」） */
  ipWhitelist?: string
  /** 限流阈值（次/分）。2026-09-19 三方类型已统一为整数（实体/DB/前端），此前 boolean 与 DB integer 冲突致读写双 500 */
  rateLimit?: number
  auditRetentionDays?: number
  logSensitiveOps?: boolean
  logLogin?: boolean
  enabled?: boolean
  tenantId?: string
  createTime?: string
  updateTime?: string
}

/** 测试类端点的统一返回（页面据此如实提示） */
export interface ConfigTestResult {
  success: boolean
  message: string
}

/** {success, config} 保存响应解包：后端返回 success=false 时抛错（拦截器不会）。 */
async function saveConfigPayload<T>(url: string, data: any, label: string): Promise<{ success: boolean; config?: T }> {
  const res = (await request.post(url, data)) as any
  const ok = res?.success !== false
  if (!ok) throw new Error(res?.message || `${label}保存失败`)
  return { success: ok, config: res?.config as T }
}

/** POST /test 读 success + message（拦截器对无 code 的响应整体透传） */
async function testConfigPayload(url: string, data: any, label: string): Promise<ConfigTestResult> {
  const res = (await request.post(url, data)) as any
  return {
    success: res?.success === true,
    message: res?.message || `${label}测试未返回结果说明`
  }
}

export const platformMailConfigApi = {
  /** 读取邮件配置（后端无配置行时 `Map.of` 传 null 会 NPE 500，由页面按错误态处理） */
  load(): Promise<PlatformMailConfig> {
    return request.get('/mail/config')
  },
  save(data: Partial<PlatformMailConfig>) {
    return saveConfigPayload<PlatformMailConfig>('/mail/config', data, '邮件配置')
  },
  test(data: Partial<PlatformMailConfig>): Promise<ConfigTestResult> {
    return testConfigPayload('/mail/test', data, '邮件')
  }
}

export const platformSmsConfigApi = {
  load(): Promise<PlatformSmsConfig> {
    return request.get('/sms/config')
  },
  save(data: Partial<PlatformSmsConfig>) {
    return saveConfigPayload<PlatformSmsConfig>('/sms/config', data, '短信配置')
  },
  test(data: Partial<PlatformSmsConfig>): Promise<ConfigTestResult> {
    return testConfigPayload('/sms/test', data, '短信')
  }
}

export const platformStorageConfigApi = {
  /** 读取存储配置：前缀是 /storage-config（历史实现误写成 /storage → 全页 404） */
  load(): Promise<PlatformStorageConfig> {
    return request.get('/storage-config/config')
  },
  save(data: Partial<PlatformStorageConfig>) {
    return saveConfigPayload<PlatformStorageConfig>('/storage-config/config', data, '存储配置')
  },
  test(data: Partial<PlatformStorageConfig>): Promise<ConfigTestResult> {
    return testConfigPayload('/storage-config/test', data, '存储')
  }
}

export const platformSecurityPolicyApi = {
  /** 读取安全策略：路径与后端 @GetMapping 逐字一致，但实测 500（rate_limit 类型冲突） */
  load(): Promise<PlatformSecurityPolicy> {
    return request.get('/system/security/policy')
  },
  /** 保存安全策略（后端返回 {code:200, data:<SecurityPolicy>}，拦截器拆包后即实体） */
  save(data: Partial<PlatformSecurityPolicy>): Promise<PlatformSecurityPolicy> {
    return request.post('/system/security/policy/save', data)
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
  /**
   * 模板列表（裸数组，拦截器透传）
   *
   * 数据源（2026-09-19 落库改造）：后端 `ImportTemplateController#getAllTemplates` 经
   * `ImportTemplateStore` 读**库表 `dev_template`**（只取 `template_kind='import' AND enabled=true`
   * 的行，迁移 V11.415.0），模板定义已持久化（重启不复原、多实例一致）。
   * 当前返回 4 条（原内存注册表的 4 个模板已迁为种子数据），且
   * **不接受任何查询参数** → 模板管理页的筛选与分页均在前端完成（见《模板管理开发文档》§3.3 / §9.4）。
   * 另：`GET /import-templates/{id}` 等 9 个端点本页未用（文档 §12-P1⑥）。
   */
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

// ═══ API 文档（OpenAPI 3 spec） ═══

/**
 * 后端服务源：非 `/api` 前缀的后端资源（OpenAPI spec、Knife4j 文档页）必须用绝对地址直连。
 *
 * 依据（实测 2026-09-18）：
 *  - vite dev server 只代理 `/api`、`/api/erp/mall`、`/ws`（vite.config.ts:109-137），
 *    `/v3/api-docs` 不在代理内 → 相对路径会落到 SPA 自身（返回 index.html）；
 *  - 后端 CORS 已放行 `http://localhost:5656`（预检 OPTIONS 200 + Access-Control-Allow-Origin），
 *    故浏览器可跨域直连；
 *  - `/v3/api-docs` 与 `/doc.html` 均在 Sa-Token 放行清单内（SaTokenConfig.java:60-64/100-105）；
 *    `/swagger-ui/**` **不在**清单内 → 直连返回 401（页面按钮据此置灰）。
 *
 * 可用 `VITE_BACKEND_ORIGIN` 覆盖（不设则按「同主机 + 后端默认端口 5655」推导）。
 */
export function backendOrigin(): string {
  const override = import.meta.env.VITE_BACKEND_ORIGIN
  if (typeof override === 'string' && override) return override.replace(/\/+$/, '')
  const { protocol, hostname } = window.location
  return `${protocol}//${hostname}:5655`
}

/** OpenAPI 操作对象（只声明本页展示用到的字段，其余原样透传给详情弹窗） */
export interface OpenApiOperation {
  tags?: string[]
  summary?: string
  description?: string
  operationId?: string
  deprecated?: boolean
  parameters?: Array<{ name?: string; in?: string; required?: boolean; description?: string; schema?: any }>
  requestBody?: any
  responses?: Record<string, any>
  [key: string]: any
}

/** OpenAPI 3 文档根对象（本页只消费 info / tags / paths） */
export interface OpenApiDocument {
  openapi?: string
  info?: { title?: string; description?: string; version?: string }
  tags?: Array<{ name?: string; description?: string }>
  paths?: Record<string, Record<string, OpenApiOperation>>
}

export const apiDocApi = {
  /**
   * 拉取 OpenAPI 3 spec：`GET {后端源}/v3/api-docs`
   *
   * 实测返回 200 + 4.1MB JSON（2997 个路径 / 3431 个操作 / 369 个 tag），无需登录即可取。
   * 失败时**抛错**（由页面渲染错误态并留空），绝不返回任何兜底数据。
   */
  async spec(): Promise<OpenApiDocument> {
    try {
      const response = await axios.get<OpenApiDocument>(`${backendOrigin()}/v3/api-docs`, { headers: adminHeaders() })
      return response.data
    } catch (e: any) {
      if (e?.response) throw new Error(e.response.data?.message || `请求失败 (${e.response.status})`)
      // 网络层错误（后端未启动 / CORS 被拒）：axios 原文为英文 Network Error，此处补中文说明
      throw e instanceof Error ? new Error(`${e.message}（后端 ${backendOrigin()} 不可达或跨域被拒）`) : new Error(String(e))
    }
  }
}
