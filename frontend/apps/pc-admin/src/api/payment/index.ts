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

// 对账状态枚举（与后端 payment_reconciliation.status 注释一致；3「处理中」用于页面统计口径）
export const RECON_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '待对账', color: 'warning' },
  1: { text: '已对账', color: 'success' },
  2: { text: '有差异', color: 'error' },
  3: { text: '处理中', color: 'processing' }
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
    request.post<Result<PaymentRequest>>('/payment/request', params),

  // 分页查询支付请求（业务类型 / 业务单号 / 渠道 / 状态 / 创建时间区间 / 付款人）
  pageRequest: (params: {
    pageNum: number
    pageSize: number
    bizType?: string
    bizNo?: string
    channel?: string
    status?: number
    startTime?: string
    endTime?: string
    /** 付款人（模糊）；后端 PaymentController#pagePaymentRequest 支持 */
    payerName?: string
  }) => request.get<Result<PageResult<PaymentRequest>>>('/payment/request/page', { params }),

  // 支付请求统计（后端聚合：待支付/成功/失败 + 累计金额）
  statRequest: (params?: { channel?: string }) =>
    request.get<Result<Record<string, any>>>('/payment/request/stat', { params }),

  // 查询支付请求详情
  getRequest: (id: number) =>
    request.get<Result<PaymentRequest>>(`/payment/request/${id}`),

  // 取消支付请求
  cancel: (id: number) =>
    request.post<Result<void>>(`/payment/request/${id}/cancel`),

  // 确认线下支付
  confirmOffline: (id: number, channelOrderNo: string) =>
    request.post<Result<void>>(`/payment/request/${id}/confirm`, null, { params: { channelOrderNo } }),

  // 确认支付
  confirmPayment: (id: number, params: { method?: string; remark?: string; channelOrderNo?: string }) =>
    request.post<Result<void>>(`/payment/request/${id}/confirm`, params),

  // 分页查询支付记录（渠道 / 状态 / 渠道订单号 / 支付时间区间）
  pageRecord: (params: {
    pageNum: number
    pageSize: number
    channel?: string
    status?: number
    channelOrderNo?: string
    startTime?: string
    endTime?: string
  }) => request.get<Result<PageResult<PaymentRecord>>>('/payment/record/page', { params }),

  // 支付记录统计（后端聚合：成功/失败笔数 + 成功金额合计）
  statRecord: (params?: { channel?: string }) =>
    request.get<Result<Record<string, any>>>('/payment/record/stat', { params }),

  // 获取可用支付渠道
  getChannels: (amount: number) =>
    request.get<Result<ChannelInfo[]>>('/payment/channels', { params: { amount } })
}

// 退款请求 API
export const refundApi = {
  // 创建退款请求
  create: (params: { paymentId: number; amount: number; reason: string }) =>
    request.post<Result<RefundRequest>>('/refund/request', params),

  // 分页查询退款请求（状态 / 单号 / 退款日期区间 / 渠道）
  pageRequest: (params: {
    pageNum: number
    pageSize: number
    status?: number
    refundNo?: string
    startTime?: string
    endTime?: string
    channel?: string
  }) => request.get<Result<PageResult<RefundRequest>>>('/refund/request/page', { params }),

  // 退款请求统计（后端聚合：待审批/已批准/已拒绝 + 退款金额合计）
  statRequest: (params?: { channel?: string }) =>
    request.get<Result<Record<string, any>>>('/refund/request/stat', { params }),

  // 查询退款请求详情
  getRequest: (id: number) =>
    request.get<Result<RefundRequest>>(`/refund/request/${id}`),

  // 审批退款
  approve: (id: number, approved: boolean, remark?: string) =>
    request.post<Result<void>>(`/refund/request/${id}/approve`, null, { params: { approved, remark } }),

  // 分页查询退款记录
  pageRecord: (params: { pageNum: number; pageSize: number; channel?: string }) =>
    request.get<Result<PageResult<RefundRecord>>>('/refund/record/page', { params })
}

// 对账 API
export const reconciliationApi = {
  // 执行日对账
  execute: (date: string, channel?: string) =>
    request.post<Result<PaymentReconciliation[]>>('/reconciliation/execute', null, { params: { date, channel } }),

  // 分页查询对账记录
  page: (params: { pageNum: number; pageSize: number; startDate?: string; endDate?: string; channel?: string; status?: number }) =>
    request.get<Result<PageResult<PaymentReconciliation>>>('/reconciliation/page', { params }),

  // 查询对账详情
  get: (id: number) =>
    request.get<Result<PaymentReconciliation>>(`/reconciliation/${id}`),

  // 处理差异（method：MANUAL手工调账 / IGNORE忽略差异 / REPROCESS重新对账）
  handleDifference: (id: number, params: { method: string; remark: string }) =>
    request.post<Result<void>>(`/reconciliation/${id}/handle`, null, { params }),

  // 对账统计（后端聚合：已对账/有差异/处理中 + 差异金额合计）
  stat: (params?: { startDate?: string; endDate?: string; channel?: string }) =>
    request.get<Result<Record<string, any>>>('/reconciliation/stat', { params }),

  // 获取待对账日期列表
  getPendingDates: (channel?: string) =>
    request.get<Result<string[]>>('/reconciliation/pending-dates', { params: { channel } })
}