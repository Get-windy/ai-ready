/**
 * DMS 渠道管理（运力来源）API 模块
 *
 * 后端：`cn.aiedge.dms.channel.controller.ChannelController`（/api/dms/channel/*）
 * 说明：`configJson` 由后端脱敏返回（凭据值为 ******），更新时回传掩码不会覆盖原值。
 */
import request from '@/utils/request'

/** 配送渠道 */
export interface DmsChannel {
  /** 后端 Long → 字符串（防 JS 精度丢失） */
  id: number | string
  channelCode: string
  channelName: string
  /** 1-自有员工 2-众包兼职 3-外部平台 4-社会车辆 */
  channelType: number
  adapterBean?: string | null
  /** 对接配置 JSON（脱敏） */
  configJson?: string | null
  /** 0-禁用 1-启用 */
  status: number
  priority: number
  /** 0-未对接 1-已对接 2-对接异常 */
  linkStatus: number
  lastTestTime?: string | null
  lastTestResult?: string | null
  coverageArea?: string | null
  /** 1-按单 2-按距 3-按重 */
  billingType: number
  billingConfig?: string | null
  remark?: string | null
  createTime?: string
  updateTime?: string
  /** 该渠道配送员总数（实时统计） */
  riderTotal: number
  /** 该渠道在线配送员数（空闲/忙碌） */
  riderOnline: number
}

/** 渠道查询条件 */
export interface DmsChannelQuery {
  pageNum?: number
  pageSize?: number
  channelCode?: string
  channelName?: string
  channelType?: number | null
  linkStatus?: number | null
  status?: number | null
  startDate?: string
  endDate?: string
}

/** 分页响应 */
export interface DmsChannelPage {
  records: DmsChannel[]
  total: number
  size: number
  current: number
}

/** 渠道下拉项 */
export interface DmsChannelOption {
  id: number | string
  channelCode: string
  channelName: string
  channelType: number
}

/** 连通性测试结果 */
export interface DmsChannelTestResult {
  success: boolean
  linkStatus: number
  message: string
  testTime: string
}

/** 运力同步结果 */
export interface DmsChannelSyncResult {
  inserted: number
  updated: number
  message: string
}

/** 渠道派单（向外部平台下单）结果 */
export interface DmsChannelPushResult {
  success: boolean
  /** true=幂等命中（同任务同渠道已下过单，直接复用原单号） */
  reused: boolean
  taskId: number | string
  taskNo: string
  channelId: number | string
  channelCode: string
  channelName: string
  /** 外部平台单号（失败时为 null） */
  channelOrderNo?: string | null
  attempts: number
  orderStatus: number
  message: string
}

/** 外部单台账行（dms_channel_order） */
export interface DmsChannelOrder {
  id: number | string
  channelId: number | string
  channelCode?: string
  taskId?: number | string
  taskNo?: string
  idemKey: string
  channelOrderNo?: string
  /** 0-待提交 1-已提交 2-已接单 3-配送中 4-已完成 5-已取消 6-提交失败 */
  orderStatus: number
  attempts?: number
  lastError?: string
  submitTime?: string
  callbackTime?: string
  remark?: string
  createTime?: string
}

/** 回调日志行（dms_channel_callback_log） */
export interface DmsChannelCallbackLog {
  id: number | string
  channelId?: number | string
  channelCode?: string
  channelOrderNo?: string
  taskId?: number | string
  taskNo?: string
  eventType?: string
  externalStatus?: string
  nonce?: string
  /** 0-验签失败 1-通过 */
  signOk: number
  /** 0-否 1-重放 */
  replayed: number
  /** OK / REPLAY / REJECT */
  processResult?: string
  processMessage?: string
  payload?: string
  receiveTime?: string
}

export const channelApi = {
  /** 多条件分页 */
  page(params: DmsChannelQuery) {
    return request.get<any, DmsChannelPage>('/dms/channel/page', { params })
  },

  /** 启用渠道下拉 */
  options() {
    return request.get<any, DmsChannelOption[]>('/dms/channel/options')
  },

  /** 详情（凭据脱敏） */
  getById(id: number | string) {
    return request.get<any, DmsChannel>(`/dms/channel/${id}`)
  },

  create(data: Partial<DmsChannel>) {
    return request.post('/dms/channel', data)
  },

  update(id: number | string, data: Partial<DmsChannel>) {
    return request.put(`/dms/channel/${id}`, data)
  },

  /** 启停（后端为 query 参数，不能走请求体） */
  updateStatus(id: number | string, status: number) {
    return request.put(`/dms/channel/${id}/status`, null, { params: { status } })
  },

  /** 批量启停 */
  batchStatus(ids: (number | string)[], status: number) {
    return request.post('/dms/channel/batch-status', { ids }, { params: { status } })
  },

  /** 真删除（被配送员引用时后端拒绝） */
  remove(id: number | string) {
    return request.delete(`/dms/channel/${id}`)
  },

  /** 批量删除 */
  batchRemove(ids: (number | string)[]) {
    return request.post('/dms/channel/batch-delete', { ids })
  },

  /** 连通性测试 */
  test(id: number | string) {
    return request.post<any, DmsChannelTestResult>(`/dms/channel/${id}/test`)
  },

  /** 同步外部平台运力 */
  syncRiders(id: number | string) {
    return request.post<any, DmsChannelSyncResult>(`/dms/channel/${id}/sync-riders`)
  },

  /** 渠道派单（向外部平台下单；幂等 + 重试，失败如实返回原因） */
  pushOrder(id: number | string, taskId: number | string) {
    return request.post<any, DmsChannelPushResult>(`/dms/channel/${id}/push-order`, { taskId })
  },

  /** 外部单台账分页 */
  orders(id: number | string, params?: { orderStatus?: number; pageNum?: number; pageSize?: number }) {
    return request.get<any, { records: DmsChannelOrder[]; total: number; current: number; size: number }>(
      `/dms/channel/${id}/orders`, { params })
  },

  /** 回调日志分页（验签/防重放/处理结果留痕） */
  callbackLogs(id: number | string, params?: { pageNum?: number; pageSize?: number }) {
    return request.get<any, { records: DmsChannelCallbackLog[]; total: number; current: number; size: number }>(
      `/dms/channel/${id}/callback-logs`, { params })
  },
}
