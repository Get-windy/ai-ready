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
  createTime: string
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
    request.post<Result<ExternalChannelConfig>>('/api/trade/channel', config),

  update: (id: number, config: ExternalChannelConfig) =>
    request.put<Result<ExternalChannelConfig>>(`/api/trade/channel/${id}`, config),

  delete: (id: number) =>
    request.delete<Result<void>>(`/api/trade/channel/${id}`),

  get: (id: number) =>
    request.get<Result<ExternalChannelConfig>>(`/api/trade/channel/${id}`),

  getByCode: (channelCode: string) =>
    request.get<Result<ExternalChannelConfig>>(`/api/trade/channel/code/${channelCode}`),

  listEnabled: () =>
    request.get<Result<ExternalChannelConfig[]>>('/api/trade/channel/enabled'),

  toggleStatus: (id: number, enabled: boolean) =>
    request.post<Result<void>>(`/api/trade/channel/${id}/toggle`, null, { params: { enabled } }),

  initialize: (id: number) =>
    request.post<Result<boolean>>(`/api/trade/channel/${id}/initialize`),

  sync: (id: number) =>
    request.post<Result<{ syncedOrders: number; syncedProducts: number; message: string }>>(`/api/trade/channel/${id}/sync`)
}

// 外部订单 API
export const externalOrderApi = {
  page: (params: { pageNum: number; pageSize: number; channelCode?: string; status?: number }) =>
    request.get<Result<PageResult<ExternalOrderRaw>>>('/api/trade/external-order/page', { params }),

  countPending: (channelCode?: string) =>
    request.get<Result<number>>('/api/open/order/pending-count', { params: { channelCode } }),

  retry: (id: number) =>
    request.post<Result<boolean>>(`/api/trade/external-order/${id}/retry`)
}

// 库存同步 API
export const inventorySyncApi = {
  query: (skuCode: string, warehouseId?: number) =>
    request.get<Result<InventoryQueryResult>>('/api/open/inventory/query', { params: { skuCode, warehouseId } }),

  batchQuery: (skuCodes: string[], warehouseId?: number) =>
    request.post<Result<Record<string, InventoryQueryResult>>>('/api/open/inventory/batch-query', skuCodes, { params: { warehouseId } }),

  sync: (channelCode: string, skuCode: string, quantity: number) =>
    request.post<Result<ProductSyncResult>>(`/api/open/inventory/sync/${channelCode}`, null, { params: { skuCode, quantity } }),

  batchSync: (channelCode: string, skuQuantities: Record<string, number>) =>
    request.post<Result<Record<string, ProductSyncResult>>>(`/api/open/inventory/batch-sync/${channelCode}`, skuQuantities),

  page: (params: { pageNum: number; pageSize: number; channelCode?: string; skuCode?: string }) =>
    request.get<Result<PageResult<InventorySyncRecord>>>('/api/trade/inventory-sync/page', { params })
}

// API监控
export const apiMonitorApi = {
  health: () =>
    request.get<Result<Record<string, any>>>('/api/open/health')
}