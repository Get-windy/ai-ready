import request from '@/utils/request'

/**
 * 支付方式
 * 后端: MdPaymentMethodController (/api/erp/md/payment-method)
 */
export interface PaymentMethodRecord {
  id: number
  tenantId: number
  methodCode: string
  methodName: string
  methodType: string
  accountId?: number
  feeRate: number
  isDefault: number
  sort: number
  status: number
  createTime: string
  updateTime: string
}

/** 支付方式查询条件（后端 PaymentMethodQuery） */
export interface PaymentMethodQuery {
  /** 筛选条件：编码/名称模糊 */
  keyword?: string
  /** 类型 CASH/BANK/WECHAT/ALIPAY/CHECK/OTHER（空=全部） */
  methodType?: string
  /** 状态 0-停用 1-启用（空=全部） */
  status?: number
  pageNum?: number
  pageSize?: number
}

export const paymentMethodApi = {
  page(params: PaymentMethodQuery) { return request.get('/erp/md/payment-method/page', { params }) },
  getById(id: number) { return request.get(`/erp/md/payment-method/${id}`) },
  list() { return request.get('/erp/md/payment-method/list') },
  create(data: Partial<PaymentMethodRecord>) { return request.post('/erp/md/payment-method', data) },
  update(id: number, data: Partial<PaymentMethodRecord>) { return request.put(`/erp/md/payment-method/${id}`, data) },
  remove(id: number) { return request.delete(`/erp/md/payment-method/${id}`) },
  updateStatus(id: number, status: number) { return request.put(`/erp/md/payment-method/${id}/status`, { status }) },
  /** 批量启用/停用 */
  batchStatus(ids: number[], status: number) {
    return request.put('/erp/md/payment-method/batch-status', { ids, status })
  },
  /** 导出（后端返回真实 xlsx） */
  export(params: PaymentMethodQuery): Promise<Blob> {
    return request.get('/erp/md/payment-method/export', { params, responseType: 'blob' })
  },
}

/**
 * 支付渠道
 * 后端: MdPaymentChannelController (/api/erp/md/payment-channel)
 */
export interface PaymentChannelRecord {
  id: number
  tenantId: number
  channelCode: string
  channelName: string
  methodId: number
  /** 回填自 md_payment_method */
  methodCode?: string
  methodName?: string
  methodType?: string
  methodTypeText?: string
  merchantNo: string
  configJson: string
  sort: number
  status: number
  statusText?: string
  remark?: string
  createTime: string
  updateTime: string
}

export const paymentChannelApi = {
  page(params: any) { return request.get('/erp/md/payment-channel/page', { params }) },
  getById(id: number) { return request.get(`/erp/md/payment-channel/${id}`) },
  list(methodId?: number) { return request.get('/erp/md/payment-channel/list', { params: { methodId } }) },
  create(data: Partial<PaymentChannelRecord>) { return request.post('/erp/md/payment-channel', data) },
  update(id: number, data: Partial<PaymentChannelRecord>) { return request.put(`/erp/md/payment-channel/${id}`, data) },
  remove(id: number) { return request.delete(`/erp/md/payment-channel/${id}`) },
  updateStatus(id: number, status: number) { return request.put(`/erp/md/payment-channel/${id}/status`, { status }) },
  /** 导出（后端返回真实 xlsx） */
  export(params: Record<string, any>): Promise<Blob> {
    return request.get('/erp/md/payment-channel/export', { params, responseType: 'blob' })
  },
}

// 支付方式类型枚举
export const METHOD_TYPE_MAP: Record<string, string> = {
  CASH: '现金',
  BANK: '银行转账',
  WECHAT: '微信',
  ALIPAY: '支付宝',
  CHECK: '支票',
  OTHER: '其他',
}

export const METHOD_TYPE_OPTIONS = Object.entries(METHOD_TYPE_MAP).map(([value, label]) => ({ label, value }))
