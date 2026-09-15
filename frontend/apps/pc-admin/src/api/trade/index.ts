import request from '@/utils/request'
import type { PageResult, Result } from '@/api/common'

// 渠道类型枚举
export const CHANNEL_TYPE_MAP: Record<string, { name: string; color: string }> = {
  ECOMMERCE: { name: '电商平台', color: 'blue' },
  SOCIAL: { name: '社交电商', color: 'green' },
  SELF: { name: '自有渠道', color: 'purple' },
  ERP: { name: 'ERP对接', color: 'orange' }
}

// 渠道编码枚举
export const CHANNEL_CODE_MAP: Record<string, { name: string; type: string }> = {
  TAOBAO: { name: '淘宝/天猫', type: 'ECOMMERCE' },
  JD: { name: '京东', type: 'ECOMMERCE' },
  PDD: { name: '拼多多', type: 'ECOMMERCE' },
  DOUYIN: { name: '抖音', type: 'SOCIAL' },
  WECHAT_MINI: { name: '微信小程序', type: 'SOCIAL' },
  SELF_MALL: { name: '自有商城', type: 'SELF' },
  POS: { name: 'POS终端', type: 'SELF' },
  ERP_API: { name: 'ERP对接', type: 'ERP' }
}

// 渠道配置类型
export interface ExternalChannelConfig {
  id: number
  tenantId: number
  channelCode: string
  channelName: string
  channelType: string
  apiEndpoint: string
  appId: string
  appSecret: string
  accessToken: string
  refreshToken: string
  tokenExpireTime: string
  syncEnabled: number
  syncInterval: number
  lastSyncTime: string
  configJson: string
  status: number
  createTime: string
}

// 外部订单原始数据类型
export interface ExternalOrderRaw {
  id: number
  tenantId: number
  channelCode: string
  externalOrderId: string
  rawData: string
  receiveTime: string
  processStatus: number
  internalOrderId: number
  errorMsg: string
  retryCount: number
  createTime: string
}

// 库存同步记录类型
export interface InventorySyncRecord {
  id: number
  tenantId: number
  channelCode: string
  productId: number
  skuCode: string
  internalQty: number
  externalQty: number
  syncQty: number
  syncType: string
  syncTime: string
  syncStatus: number
  errorMsg: string
  /** 重试次数（失败重试累计） */
  retryCount?: number
  /** 最近一次重试时间 */
  lastRetryTime?: string
  /** 失败原因分类：NETWORK/AUTH/PARAM/RATE_LIMIT/BIZ_REJECT/UNKNOWN */
  errorCategory?: string
  createTime: string
}

/** 库存同步记录查询条件 */
export interface InventorySyncQuery {
  pageNum?: number
  pageSize?: number
  channelCode?: string
  skuCode?: string
  syncType?: string
  syncStatus?: number
  errorCategory?: string
  startTime?: string
  endTime?: string
}

/** 接口调用日志（后端 ApiAccessLog，表 api_access_log） */
export interface ApiCallLog {
  id: number | string
  channelCode?: string
  apiPath?: string
  apiName?: string
  requestMethod?: string
  requestParams?: string
  requestId?: string
  /** IN 外部调用我方 / OUT 我方调用渠道 / SANDBOX 联调自检 */
  direction?: string
  /** SUCCESS / FAIL */
  status?: string
  responseCode?: number
  responseTime?: number
  errorCode?: string
  errorMsg?: string
  ipAddress?: string
  userAgent?: string
  accessTime?: string
}

/** 接口调用日志查询条件 */
export interface ApiCallQuery {
  pageNum?: number
  pageSize?: number
  channelCode?: string
  apiPath?: string
  direction?: string
  status?: string
  keyword?: string
  startTime?: string
  endTime?: string
}

/** 依赖健康项（后端 DependencyHealthVO） */
export interface DependencyHealth {
  key: string
  name: string
  /** DATABASE / CACHE / MQ / MAP / CHANNEL */
  category: string
  /** UP / DOWN / NOT_CONFIGURED */
  status: string
  latencyMs?: number
  detail?: string
  lastErrorTime?: string
  lastError?: string
}

/** 异常告警项（后端 ApiMonitorAlertVO） */
export interface ApiMonitorAlert {
  alertType: string
  level: string
  title: string
  detail: string
  currentValue?: string
  threshold?: string
  unit?: string
  silenced: boolean
  suggestion?: string
}

/** 开放接口参数定义（快速联调参数表单） */
export interface ApiEndpointParam {
  name: string
  label: string
  /** path / query / body */
  in: string
  type: string
  required: boolean
  sample?: string
  description?: string
}

/** 开放接口目录项 */
export interface ApiEndpoint {
  group: string
  groupLabel: string
  key: string
  name: string
  method: string
  path: string
  description?: string
  params: ApiEndpointParam[]
}

/** 联调结果（后端 SandboxResultVO） */
export interface SandboxResult {
  requestId: string
  apiKey: string
  apiName?: string
  method?: string
  url?: string
  httpStatus: number
  success: boolean
  bizCode?: number
  costMs: number
  responseBody?: string
  errorMsg?: string
  invokeTime: string
}

// 库存查询结果
export interface InventoryQueryResult {
  skuCode: string
  externalSkuId: string
  quantity: number
  availableQuantity: number
  lockedQuantity: number
  queryTimestamp: number
  success: boolean
  errorMsg: string
}

// 商品同步结果
export interface ProductSyncResult {
  skuCode: string
  externalSkuId: string
  status: number
  statusMsg: string
  syncTimestamp: number
  response: string
  errorCode: string
  errorMsg: string
}

// 渠道配置 API
export const channelConfigApi = {
  create: (config: ExternalChannelConfig) =>
    request.post<Result<ExternalChannelConfig>>('/trade/channel', config),

  update: (id: number, config: ExternalChannelConfig) =>
    request.put<Result<ExternalChannelConfig>>(`/trade/channel/${id}`, config),

  delete: (id: number) =>
    request.delete<Result<void>>(`/trade/channel/${id}`),

  get: (id: number) =>
    request.get<Result<ExternalChannelConfig>>(`/trade/channel/${id}`),

  getByCode: (channelCode: string) =>
    request.get<Result<ExternalChannelConfig>>(`/trade/channel/code/${channelCode}`),

  listEnabled: () =>
    request.get<Result<ExternalChannelConfig[]>>('/trade/channel/enabled'),

  /** 管理端分页查询（渠道编码/名称关键字 + 渠道类型 + 同步开关 + 启用状态） */
  page: (params: {
    pageNum?: number
    pageSize?: number
    keyword?: string
    channelType?: string
    syncEnabled?: number
    status?: number
  }) => request.get<Result<PageResult<ExternalChannelConfig>>>('/trade/channel/page', { params }),

  /**
   * 渠道台账统计（后端真实聚合 `GET /api/trade/channel/stat`，无查询参数）
   * 返回 `{ total, enabledCount, syncEnabledCount, abnormalCount }`：
   * 渠道总数 / 启用数（status=1）/ 同步开启数（syncEnabled=1）/ 异常数（已禁用或令牌已过期）
   */
  stat: () =>
    request.get<Result<Record<string, any>>>('/trade/channel/stat'),

  /** 查看渠道密钥（后端脱敏返回，不返回明文） */
  secret: (id: number) =>
    request.get<Result<Record<string, any>>>(`/trade/channel/${id}/secret`),

  toggleStatus: (id: number, enabled: boolean) =>
    request.post<Result<void>>(`/trade/channel/${id}/toggle`, null, { params: { enabled } }),

  initialize: (id: number) =>
    request.post<Result<boolean>>(`/trade/channel/${id}/initialize`),

  sync: (id: number) =>
    request.post<Result<{ syncedOrders: number; syncedProducts: number; message: string }>>(`/trade/channel/${id}/sync`)
}

// 外部订单 API
export const externalOrderApi = {
  /** 管理端分页查询（`ExternalOrderController#page` 全参数，非 any 透传） */
  page: (params: {
    pageNum: number
    pageSize: number
    channelCode?: string
    status?: number
    /** 外部订单号（后端模糊匹配） */
    externalOrderId?: string
    /** 接收时间起（含） */
    startTime?: string
    /** 接收时间止（不含） */
    endTime?: string
  }) => request.get<Result<PageResult<ExternalOrderRaw>>>('/trade/external-order/page', { params }),

  /**
   * 待处理订单数量（管理端端点 `GET /api/trade/external-order/pending-count`）
   * —— 原调开放接口 `/api/open/order/pending-count`（供外部平台回调使用），管理端页面已切回本域端点
   */
  countPending: (channelCode?: string) =>
    request.get<Result<number>>('/trade/external-order/pending-count', { params: { channelCode } }),

  /**
   * 外部订单处理状态统计（后端真实聚合 `GET /api/trade/external-order/stat`）
   * 返回 `{ total, pendingCount, processedCount, failedCount }`：
   * 台账总数 / 待处理（0）/ 已处理（1 已转换 + 2 已入库）/ 失败（3）
   */
  stat: () =>
    request.get<Result<Record<string, any>>>('/trade/external-order/stat'),

  retry: (id: number) =>
    request.post<Result<boolean>>(`/trade/external-order/${id}/retry`)
}

// 库存同步 API
export const inventorySyncApi = {
  query: (skuCode: string, warehouseId?: number) =>
    request.get<Result<InventoryQueryResult>>('/open/inventory/query', { params: { skuCode, warehouseId } }),

  batchQuery: (skuCodes: string[], warehouseId?: number) =>
    request.post<Result<Record<string, InventoryQueryResult>>>('/open/inventory/batch-query', skuCodes, { params: { warehouseId } }),

  sync: (channelCode: string, skuCode: string, quantity: number) =>
    request.post<Result<ProductSyncResult>>(`/open/inventory/sync/${channelCode}`, null, { params: { skuCode, quantity } }),

  batchSync: (channelCode: string, skuQuantities: Record<string, number>) =>
    request.post<Result<Record<string, ProductSyncResult>>>(`/open/inventory/batch-sync/${channelCode}`, skuQuantities),

  page: (params: InventorySyncQuery) =>
    request.get<Result<PageResult<InventorySyncRecord>>>('/trade/inventory-sync/page', { params }),

  /**
   * 库存同步状态统计（后端真实聚合 `GET /api/trade/inventory-sync/stat`，无查询参数）
   * 返回 `{ total, pendingCount, successCount, failedCount }`：
   * 同步记录数 / 待同步（syncStatus 0）/ 成功（1）/ 失败（2）
   */
  stat: () =>
    request.get<Result<Record<string, any>>>('/trade/inventory-sync/stat'),

  /**
   * 行级重试（**本域管理端端点** `POST /api/trade/inventory-sync/{id}/retry`）
   * 与 `/api/trade/api-monitor/sync/{id}/retry` 等价（后端共用 ApiMonitorService#retrySync），
   * 返回 `{ id, success, retryCount, syncStatus, errorCategory, errorCategoryLabel, errorMsg, message }`
   */
  retry: (id: number | string) =>
    request.post<Result<Record<string, any>>>(`/trade/inventory-sync/${id}/retry`),

  /** 同步记录导出（真实 xlsx） */
  export: (params: InventorySyncQuery): Promise<Blob> =>
    request.get('/trade/inventory-sync/export', { params, responseType: 'blob' })
}

// API监控
export const apiMonitorApi = {
  /** 开放 API 健康检查（页面「API状态」卡） */
  health: () =>
    request.get<Result<Record<string, any>>>('/open/health'),

  /** 统计卡片（后端真实聚合：今日调用量 / 成功率 / 平均·P95 耗时 / 失败数 / 库存同步失败数） */
  stat: () =>
    request.get<Result<Record<string, any>>>('/trade/api-monitor/stat'),

  /** 依赖健康逐项（DB / Redis / MQ / 地图 / 第三方渠道） */
  deps: () =>
    request.get<Result<DependencyHealth[]>>('/trade/api-monitor/deps'),

  /** 接口调用日志分页 */
  callsPage: (params: ApiCallQuery) =>
    request.get<Result<PageResult<ApiCallLog>>>('/trade/api-monitor/calls/page', { params }),

  /** 接口调用日志导出（真实 xlsx） */
  callsExport: (params: ApiCallQuery): Promise<Blob> =>
    request.get('/trade/api-monitor/calls/export', { params, responseType: 'blob' }),

  /** 调用日志分维度统计（groupBy = channel / api / direction） */
  callsStat: (params: { groupBy: string; startTime?: string; endTime?: string }) =>
    request.get<Result<Array<Record<string, any>>>>('/trade/api-monitor/calls/stat', { params }),

  /** 调用量按小时趋势 */
  callsTrend: (params: { startTime?: string; endTime?: string }) =>
    request.get<Result<Array<Record<string, any>>>>('/trade/api-monitor/calls/trend', { params }),

  /** 开放接口目录（联调分组树） */
  endpoints: () =>
    request.get<Result<ApiEndpoint[]>>('/trade/api-monitor/calls/endpoints'),

  /** 快速联调：回环调用真实开放接口 */
  sandboxInvoke: (data: { apiKey: string; params: Record<string, string> }) =>
    request.post<Result<SandboxResult>>('/trade/api-monitor/sandbox/invoke', data),

  /** 异常告警（阈值判定 + 静默期） */
  alerts: () =>
    request.get<Result<ApiMonitorAlert[]>>('/trade/api-monitor/alerts'),

  /** 当前生效阈值（含来源：配置中心 / 代码默认） */
  thresholds: () =>
    request.get<Result<Record<string, any>>>('/trade/api-monitor/thresholds'),

  /** 库存同步记录统计（状态分布 + 失败原因分类） */
  syncStat: () =>
    request.get<Result<Record<string, any>>>('/trade/api-monitor/sync/stat'),

  /** 同步失败重试 */
  retrySync: (id: number | string) =>
    request.post<Result<Record<string, any>>>(`/trade/api-monitor/sync/${id}/retry`),

  /** 按保留策略清理调用日志 */
  cleanExpired: () =>
    request.post<Result<Record<string, any>>>('/trade/api-monitor/clean-expired')
}