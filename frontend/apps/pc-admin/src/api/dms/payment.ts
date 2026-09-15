/**
 * DMS 末端收款 API 模块（代收货款 / 配送费收款）
 * 后端: PaymentController (/api/dms/payment)
 *
 * 金标准口径（2026-09-13 第二轮补齐）：
 *   · 收款类型 `paymentType`（1 代收货款 / 2 配送费）与支付方式 `payChannel` **分离**；
 *   · 收款码**配置化**（未配置不生成二维码，杜绝假二维码）；
 *   · 支付回调**幂等**（`tradeNo` 为幂等键）；
 *   · 资金上交/稽核：应上交 vs 已上交 + **交款时限超时预警**；
 *   · **未付管理**：挂账 → 催收 → 承诺付款 → 核销闭环；
 *   · **支付流水对账**：导入平台流水 → 逐笔匹配 → 三类差异；
 *   · **推送财务**幂等（生成收款单 / 核销应收，事件发件箱外发）。
 */
import request from '@/utils/request'

/** 收款记录（台账行） */
export interface DmsPayment {
  id?: number
  taskId?: number
  /** 收款类型 1-代收货款 2-配送费 */
  paymentType?: number
  /** 支付方式 1-微信 2-支付宝 3-现金 4-POS 5-银行转账 9-其他 */
  payChannel?: number
  payChannelName?: string
  amount?: number
  qrcodeUrl?: string
  externalOrderNo?: string
  tradeNo?: string
  payTime?: string
  callbackTime?: string
  /** 支付状态 0-待支付 1-已支付 2-已退款 3-未付（挂账） */
  status?: number
  unpaidRemark?: string
  riderId?: number
  /** 交款状态 0-未交 1-部分交 2-已交 */
  handoverStatus?: number
  handoverAmount?: number
  handoverTime?: string
  handoverByName?: string
  handoverRemark?: string
  /** 财务推送状态 0-未推送 1-已推送 */
  financePushStatus?: number
  financeTraceId?: string
  auditStatus?: number
  createTime?: string
  // 台账联查 + 派生列
  taskNo?: string
  customerName?: string
  customerPhone?: string
  riderName?: string
  taskCollectOnDelivery?: number
  taskDeliveryFee?: number
  /** 累计催收次数 */
  urgeCount?: number
  lastUrgeTime?: string
  /** 客户承诺付款日 */
  promiseDate?: string
  /** 累计核销金额 */
  writeOffAmount?: number
  /** 是否超交款时限 */
  overdue?: boolean
  overdueHours?: number
}

/** 支付平台流水 */
export interface PaymentFlow {
  id?: number
  channelCode?: string
  channelName?: string
  tradeNo?: string
  outTradeNo?: string
  amount?: number
  tradeTime?: string
  payer?: string
  batchNo?: string
  /** 0-未匹配 1-已匹配 2-差异 3-已忽略 */
  matchStatus?: number
  paymentId?: number
  matchType?: number
  matchTime?: string
  remark?: string
  createTime?: string
}

/** 挂账催收/核销流水 */
export interface PaymentCollectionRecord {
  id?: number
  paymentId?: number
  taskId?: number
  /** 1-催收 2-核销 3-承诺付款 */
  actionType?: number
  amount?: number
  promiseDate?: string
  content?: string
  operatorName?: string
  createTime?: string
}

/** 收款字典 */
export interface PaymentDict {
  payChannels?: Record<string, string>
  paymentTypes?: Record<string, string>
  qrcodeEnabled?: boolean
  cashLimitPerOrder?: number
  cashLimitDaily?: number
  handoverDeadlineHours?: number
}

export const paymentApi = {
  /** 收款字典（支付方式 / 收款类型 / 收款码是否开通 / 资金安全规则） */
  dict() { return request.get<PaymentDict>('/dms/payment/dict') },

  // ── 台账 / 统计 ──
  /** 收款台账分页 */
  page(params: Record<string, any>) { return request.get('/dms/payment/page', params) },
  /** 收款统计 */
  stat(params: Record<string, any>) { return request.get('/dms/payment/stat', params) },
  /** 导出数据（前端生成真实 xlsx；后端留审计） */
  exportList(params: Record<string, any>) { return request.get('/dms/payment/export', params) },

  // ── 收款 ──
  /** 生成收款二维码（未配置收款码服务时返回空 URL） */
  qrcode(taskId: number, amount: number) {
    return request.post('/dms/payment/qrcode', null, { params: { taskId, amount } })
  },
  /** 线下收款确认（现金/POS/银行转账；现金超限额会被拒绝） */
  confirm(params: { taskId: number; payChannel?: number; amount: number; externalOrderNo?: string; paymentType?: number }) {
    return request.post('/dms/payment/confirm', null, { params })
  },
  /** 批量线下收款确认（逐单反馈） */
  confirmBatch(rows: Array<Record<string, any>>) {
    return request.post('/dms/payment/confirm-batch', rows)
  },
  /** 支付回调（幂等） */
  callback(params: { taskId: number; tradeNo?: string; payChannel?: number; amount?: number; externalOrderNo?: string }) {
    return request.post('/dms/payment/callback', null, { params })
  },
  /** 标记未付（挂账） */
  markUnpaid(taskId: number, remark: string) {
    return request.post('/dms/payment/mark-unpaid', null, { params: { taskId, remark } })
  },

  // ── 资金上交 / 稽核 ──
  /** 交款登记（配送员上交企业） */
  handover(id: number, params: { amount: number; operatorId?: number; operatorName?: string; remark?: string }) {
    return request.post(`/dms/payment/${id}/handover`, null, { params })
  },
  /** 交款稽核汇总（按配送员：应上交 vs 已上交 + 超时预警） */
  handoverSummary(params: { riderId?: number; startDate?: string; endDate?: string }) {
    return request.get('/dms/payment/handover/summary', params)
  },

  // ── 未付管理（挂账 → 催收 → 核销） ──
  /** 未付（挂账）台账分页 */
  unpaidPage(params: Record<string, any>) { return request.get('/dms/payment/unpaid/page', params) },
  /** 未付（挂账）汇总 */
  unpaidStat(params: Record<string, any>) { return request.get('/dms/payment/unpaid/stat', params) },
  /** 催收登记 */
  urge(id: number, params: { content: string; promiseDate?: string; operatorName?: string }) {
    return request.post(`/dms/payment/${id}/urge`, null, { params })
  },
  /** 承诺付款日登记 */
  promise(id: number, params: { promiseDate: string; content?: string; operatorName?: string }) {
    return request.post(`/dms/payment/${id}/promise`, null, { params })
  },
  /** 核销（挂账收回） */
  writeOff(id: number, params: { amount: number; payChannel?: number; content?: string; operatorName?: string }) {
    return request.post(`/dms/payment/${id}/write-off`, null, { params })
  },
  /** 挂账动作流水（催收/承诺/核销） */
  collections(id: number) { return request.get(`/dms/payment/${id}/collections`) },

  // ── 财务打通 ──
  /** 推送财务（单个，幂等） */
  pushFinance(paymentId: number) {
    return request.post('/dms/payment/push-finance', null, { params: { paymentId } })
  },
  /** 推送财务（批量，逐单反馈） */
  pushFinanceBatch(paymentIds: number[]) {
    return request.post('/dms/payment/push-finance', null, { params: { paymentIds: paymentIds.join(',') } })
  },

  // ── 支付流水 / 对账 ──
  /** 支付平台流水分页 */
  flowPage(params: Record<string, any>) { return request.get('/dms/payment/flow/page', params) },
  /** 支付平台流水统计 */
  flowStat(params: Record<string, any>) { return request.get('/dms/payment/flow/stat', params) },
  /** 支付平台流水批量导入 */
  flowImport(rows: Array<Record<string, any>>, defaultChannel?: string) {
    return request.post('/dms/payment/flow/import', rows, { params: { defaultChannel } })
  },
  /** 与支付平台流水对账 */
  reconcile(params: { startDate?: string; endDate?: string; channelCode?: string }) {
    return request.post('/dms/payment/reconcile', null, { params })
  },
  /** 流水人工匹配 */
  flowMatch(id: number, paymentId: number) {
    return request.post(`/dms/payment/flow/${id}/match`, null, { params: { paymentId } })
  },
  /** 忽略流水差异 */
  flowIgnore(id: number, remark?: string) {
    return request.post(`/dms/payment/flow/${id}/ignore`, null, { params: { remark } })
  },

  /** 按任务查收款记录 */
  getByTaskId(taskId: number) { return request.get(`/dms/payment/${taskId}`) },
}
