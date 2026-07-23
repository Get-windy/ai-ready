import request from '@/utils/request'
import type { PageResult, Result } from '@/api/common'

// 支付渠道枚举
export const PAYMENT_CHANNEL_MAP: Record<string, { name: string; color: string }> = {
  ALIPAY: { name: '支付宝', color: 'blue' },
  WECHAT: { name: '微信支付', color: 'green' },
  UNIONPAY: { name: '银联支付', color: 'orange' },
  BANK: { name: '银行转账', color: 'purple' },
  CASH: { name: '现金/线下', color: 'default' }
}

// 支付状态枚举
export const PAYMENT_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '待支付', color: 'warning' },
  1: { text: '支付中', color: 'processing' },
  2: { text: '已支付', color: 'success' },
  3: { text: '已取消', color: 'default' },
  4: { text: '支付失败', color: 'error' }
}

// 退款状态枚举
export const REFUND_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '待处理', color: 'warning' },
  1: { text: '处理中', color: 'processing' },
  2: { text: '已退款', color: 'success' },
  3: { text: '已拒绝', color: 'error' }
}

// ── 支付渠道参数配置 ──
// 后端无支付渠道参数专用 CRUD 端点（core-payment 仅提供渠道查询/支付请求/记录），
// 渠道参数（商户号、密钥、回调地址等）以 JSON 形式暂存系统参数：
// configGroup = payment，configKey = payment.channel.{channelCode 小写}

export interface PaymentChannelParam {
  appId?: string
  merchantNo?: string
  appSecret?: string
  notifyUrl?: string
  enabled?: boolean
}

const channelParamKey = (channelCode: string) => `payment.channel.${channelCode.toLowerCase()}`

export const paymentChannelConfigApi = {
  /** 读取渠道参数（经 /api/config/list?configGroup=payment，无记录时返回 null） */
  async load(channelCode: string): Promise<PaymentChannelParam | null> {
    const res: any = await request.get('/config/list', { configGroup: 'payment' })
    const records: any[] = res?.records || []
    const hit = records.find((r: any) => r.configKey === channelParamKey(channelCode))
    if (!hit?.configValue) return null
    try {
      return JSON.parse(hit.configValue) as PaymentChannelParam
    } catch {
      return null
    }
  },

  /**
   * 保存渠道参数。后端写接口 /api/config/save-value 返回 { success, message }（无 code 字段），
   * 响应拦截器会将其误判为失败并reject，因此保存后重新读取校验是否真实写入。
   */
  async save(channelCode: string, data: PaymentChannelParam): Promise<boolean> {
    try {
      await request.post('/config/save-value', {
        configKey: channelParamKey(channelCode),
        configValue: JSON.stringify(data)
      })
      return true
    } catch {
      const loaded = await paymentChannelConfigApi.load(channelCode)
      return loaded !== null
    }
  }
}

// 对账状态枚举
export const RECON_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '待对账', color: 'warning' },
  1: { text: '已对账', color: 'success' },
  2: { text: '有差异', color: 'error' }
}

// 支付请求类型
export interface PaymentRequest {
  id: number
  tenantId: number
  bizType: string
  bizId: number
  bizNo: string
  amount: number
  channel: string
  status: number
  channelOrderNo: string
  channelTradeNo: string
  expireTime: string
  paidTime: string
  remark: string
  payerId: number
  payerName: string
  createTime: string
}

// 支付记录类型
export interface PaymentRecord {
  id: number
  tenantId: number
  requestId: number
  channel: string
  channelOrderNo: string
  channelTradeNo: string
  amount: number
  status: number
  callbackTime: string
  callbackData: string
  errorCode: string
  errorMsg: string
  createTime: string
}

// 退款请求类型
export interface RefundRequest {
  id: number
  tenantId: number
  paymentId: number
  amount: number
  reason: string
  status: number
  channelRefundNo: string
  refundedTime: string
  applicantId: number
  applicantName: string
  approverId: number
  approveRemark: string
  createTime: string
}

// 退款记录类型
export interface RefundRecord {
  id: number
  tenantId: number
  requestId: number
  channel: string
  channelRefundNo: string
  amount: number
  status: number
  callbackTime: string
  callbackData: string
  errorCode: string
  errorMsg: string
  createTime: string
}

// 对账记录类型
export interface PaymentReconciliation {
  id: number
  tenantId: number
  reconcileDate: string
  channel: string
  totalCount: number
  totalAmount: number
  successCount: number
  successAmount: number
  diffCount: number
  diffAmount: number
  status: number
  reconciledTime: string
  remark: string
  createTime: string
}

// 渠道信息
export interface ChannelInfo {
  code: string
  name: string
  minAmount: number
  maxAmount: number
  available: boolean
}

// 支付请求 API
export const paymentApi = {
  // 创建支付请求
  create: (params: { bizType: string; bizId: number; bizNo: string; amount: number; channel: string }) =>
    request.post<Result<PaymentRequest>>('/api/payment/request', params),

  // 分页查询支付请求
  pageRequest: (params: { pageNum: number; pageSize: number; bizType?: string; channel?: string; status?: number }) =>
    request.get<Result<PageResult<PaymentRequest>>>('/api/payment/request/page', { params }),

  // 查询支付请求详情
  getRequest: (id: number) =>
    request.get<Result<PaymentRequest>>(`/api/payment/request/${id}`),

  // 取消支付请求
  cancel: (id: number) =>
    request.post<Result<void>>(`/api/payment/request/${id}/cancel`),

  // 确认线下支付
  confirmOffline: (id: number, channelOrderNo: string) =>
    request.post<Result<void>>(`/api/payment/request/${id}/confirm`, null, { params: { channelOrderNo } }),

  // 确认支付
  confirmPayment: (id: number, params: { method?: string; remark?: string; channelOrderNo?: string }) =>
    request.post<Result<void>>(`/api/payment/request/${id}/confirm`, params),

  // 分页查询支付记录
  pageRecord: (params: { pageNum: number; pageSize: number; channel?: string }) =>
    request.get<Result<PageResult<PaymentRecord>>>('/api/payment/record/page', { params }),

  // 获取可用支付渠道
  getChannels: (amount: number) =>
    request.get<Result<ChannelInfo[]>>('/api/payment/channels', { params: { amount } })
}

// 退款请求 API
export const refundApi = {
  // 创建退款请求
  create: (params: { paymentId: number; amount: number; reason: string }) =>
    request.post<Result<RefundRequest>>('/api/refund/request', params),

  // 分页查询退款请求
  pageRequest: (params: { pageNum: number; pageSize: number; status?: number }) =>
    request.get<Result<PageResult<RefundRequest>>>('/api/refund/request/page', { params }),

  // 查询退款请求详情
  getRequest: (id: number) =>
    request.get<Result<RefundRequest>>(`/api/refund/request/${id}`),

  // 审批退款
  approve: (id: number, approved: boolean, remark?: string) =>
    request.post<Result<void>>(`/api/refund/request/${id}/approve`, null, { params: { approved, remark } }),

  // 分页查询退款记录
  pageRecord: (params: { pageNum: number; pageSize: number; channel?: string }) =>
    request.get<Result<PageResult<RefundRecord>>>('/api/refund/record/page', { params })
}

// 对账 API
export const reconciliationApi = {
  // 执行日对账
  execute: (date: string, channel?: string) =>
    request.post<Result<PaymentReconciliation[]>>('/api/reconciliation/execute', null, { params: { date, channel } }),

  // 分页查询对账记录
  page: (params: { pageNum: number; pageSize: number; startDate?: string; endDate?: string; channel?: string; status?: number }) =>
    request.get<Result<PageResult<PaymentReconciliation>>>('/api/reconciliation/page', { params }),

  // 查询对账详情
  get: (id: number) =>
    request.get<Result<PaymentReconciliation>>(`/api/reconciliation/${id}`),

  // 处理差异
  handleDifference: (id: number, params: { method: string; remark: string }) =>
    request.post<Result<void>>(`/api/reconciliation/${id}/handle`, params),

  // 获取待对账日期列表
  getPendingDates: (channel?: string) =>
    request.get<Result<string[]>>('/api/reconciliation/pending-dates', { params: { channel } })
}