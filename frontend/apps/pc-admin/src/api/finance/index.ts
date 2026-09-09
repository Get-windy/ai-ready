import request, { type ApiResponse, type PageResponse } from '@/utils/request'

/**
 * 应收账款（旧版，与receivable页面兼容，使用/erp/finance/receivable/接口）
 */
export const receivableApi = {
  getPage: (params: any) => request.get('/erp/finance/receivable/list', params),
  getById: (id: number) => request.get(`/erp/finance/receivable/${id}`),
  getAging: () => request.get('/erp/finance/receivable/aging'),
  create: (data: any) => request.post('/erp/finance/receivable', data),
  writeOff: (id: number, amount: number) => request.put(`/erp/finance/receivable/${id}/write-off`, { amount }),
  markBadDebt: (id: number) => request.put(`/erp/finance/receivable/${id}/bad-debt`)
}

/**
 * 应付账款（旧版，与payable页面兼容，使用/erp/finance/payable/接口）
 */
export const payableApi = {
  getPage: (params: any) => request.get('/erp/finance/payable/list', params),
  getById: (id: number) => request.get(`/erp/finance/payable/${id}`),
  getAging: () => request.get('/erp/finance/payable/aging'),
  create: (data: any) => request.post('/erp/finance/payable', data),
  writeOff: (id: number, amount: number) => request.put(`/erp/finance/payable/${id}/write-off`, { amount })
}

/**
 * 会计科目API
 */
export const accountSubjectApi = {
  getTree: (params?: any) => request.get('/erp/finance/subject/tree', params),
  getList: (params?: any) => request.get('/erp/finance/subject/list', params),
  getById: (id: number) => request.get(`/erp/finance/subject/${id}`),
  getByType: (type: number) => request.get(`/erp/finance/subject/type/${type}`),
  create: (data: any) => request.post('/erp/finance/subject', data),
  update: (id: number, data: any) => request.put(`/erp/finance/subject/${id}`, data),
  delete: (id: number) => request.delete(`/erp/finance/subject/${id}`),
  toggleEnabled: (id: number, enabled?: boolean) => request.put(`/erp/finance/subject/${id}/enable`, null, { params: { enabled } })
}

/**
 * 凭证API
 */
export const voucherApi = {
  getPage: (params: any) => request.get('/erp/finance/voucher/list', params),
  getById: (id: number) => request.get(`/erp/finance/voucher/${id}`),
  getByVoucherNo: (no: string) => request.get(`/erp/finance/voucher/no/${no}`),
  create: (data: any) => request.post('/erp/finance/voucher', data),
  audit: (id: number) => request.put(`/erp/finance/voucher/${id}/audit`),
  post: (id: number) => request.put(`/erp/finance/voucher/${id}/post`),
  reverse: (id: number, reason: string) => request.post(`/erp/finance/voucher/${id}/reverse`, { reason })
}

/**
 * 分类账API（总账 + 明细账）
 * 后端控制器: erp-finance LedgerController @RequestMapping("/api/erp/finance/ledger")
 */
export const ledgerApi = {
  /** 总账：按科目汇总期初/本期/期末借贷 */
  getGeneral: (params: any) => request.get('/erp/finance/ledger/general', params),
  /** 明细账：按科目逐笔凭证分录 */
  getDetail: (params: any) => request.get('/erp/finance/ledger/detail', params)
}

/**
 * 财务报表API v2
 */
export const reportApi = {
  getTrialBalance: (params: any) => request.get('/erp/finance/report/v2/trial-balance', params),
  getBalanceSheet: (params: any) => request.get('/erp/finance/report/v2/balance-sheet', params),
  getIncomeStatement: (params: any) => request.get('/erp/finance/report/v2/income-statement', params),
  getDashboard: () => request.get('/erp/finance/report/v2/dashboard')
}

/**
 * 财务对账API
 * 后端控制器: erp-finance ReconciliationController @RequestMapping("/api/erp/finance/reconciliation")
 */
export const reconciliationApi = {
  /**
   * 获取对账统计数据
   */
  getStats(): Promise<ApiResponse<any>> {
    return request.get('/erp/finance/reconciliation/stats')
  },

  /**
   * 创建对账记录
   */
  create(params: any): Promise<ApiResponse<number>> {
    return request.post('/erp/finance/reconciliation/create', params)
  },

  /**
   * 更新对账记录
   */
  update(params: any): Promise<ApiResponse<void>> {
    return request.put('/erp/finance/reconciliation/update', params)
  },

  /**
   * 根据ID获取对账记录详情
   */
  getById(id: number): Promise<ApiResponse<any>> {
    return request.get(`/erp/finance/reconciliation/${id}`)
  },

  /**
   * 删除对账记录
   */
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/erp/finance/reconciliation/${id}`)
  },

  /**
   * 分页查询对账记录
   */
  page(params: any): Promise<ApiResponse<any>> {
    return request.post('/erp/finance/reconciliation/list', params)
  },

  /**
   * 执行对账
   */
  reconcile(id: number): Promise<ApiResponse<void>> {
    return request.post(`/erp/finance/reconciliation/reconcile/${id}`)
  },

  /**
   * 处理差异
   */
  handleDifference(id: number, reason: string): Promise<ApiResponse<void>> {
    return request.post(`/erp/finance/reconciliation/handle-difference/${id}`, null, { params: { differenceReason: reason } })
  },

  /**
   * 批量删除对账记录
   */
  deleteBatch(ids: number[]): Promise<ApiResponse<void>> {
    return request.delete('/erp/finance/reconciliation/batch', { data: ids })
  },

  /**
   * 导出对账记录列表
   */
  exportList(params: any): Promise<ApiResponse<any[]>> {
    return request.get('/erp/finance/reconciliation/export', { params })
  },

  // ── 以下为旧版API，兼容现有组件 ──
  /** @deprecated 使用 create() 或 page() */
  bankReconciliation(params: any): Promise<ApiResponse<any>> {
    return request.post('/erp/finance/reconciliation/create', params)
  },
  /** @deprecated 使用 page() */
  customerReconciliation(params: any): Promise<ApiResponse<any>> {
    return request.post('/erp/finance/reconciliation/list', params)
  },
  /** @deprecated 使用 page() */
  supplierReconciliation(params: any): Promise<ApiResponse<any>> {
    return request.post('/erp/finance/reconciliation/list', params)
  }
}

/**
 * 预收款
 */
export interface PreReceipt {
  id: number
  preReceiptNo: string
  sourceType: string
  sourceId: number
  sourceNo: string
  sourceUnsettledAmount: number
  partnerCode: string
  customerId: number
  customerName: string
  amount: number
  usedAmount: number
  remainingAmount: number
  giftAmount: number
  totalAmount: number
  prevAmount: number
  depositType: number
  depositFlag: number
  receiptDate: string
  status: string
  summary: string
  handlerId: number
  handlerName: string
  deptId: number
  deptName: string
  creatorName: string
  bookkeeperId: number
  bookkeeperName: string
  bookkeepingTime: string
  auditorId: number
  auditorName: string
  auditorTime: string
  printCount: number
  paymentMethod: string
  bankAccount: string
  bankName: string
  transactionNo: string
  remark: string
  createTime: string
  items?: PreReceiptItem[]
}

/**
 * 预收款-收款账户明细（一单多账户）
 */
export interface PreReceiptItem {
  id?: number
  preReceiptId?: number
  lineNo?: number
  accountNo: string
  accountName: string
  amount: number
  remark: string
}

/**
 * 预付款
 */
export interface PrePayment {
  id: number
  prePaymentNo: string
  sourceType: string
  sourceId: number
  sourceNo: string
  supplierId: number
  supplierName: string
  amount: number
  usedAmount: number
  remainingAmount: number
  depositType: number
  depositFlag: number
  paymentDate: string
  status: string
  paymentMethod: string
  bankAccount: string
  bankName: string
  transactionNo: string
  remark: string
  createTime: string
}

/**
 * 往来对冲
 */
export interface Offset {
  id: number
  offsetNo: string
  partyType: string
  partyId: number
  partyName: string
  receivableAmount: number
  payableAmount: number
  offsetAmount: number
  balanceAmount: number
  offsetDate: string
  status: string
  remark: string
  createTime: string
}

/**
 * 对冲明细
 */
export interface OffsetItem {
  id: number
  offsetId: number
  lineNo: number
  direction: string
  refType: string
  refId: number
  refNo: string
  amount: number
}

/**
 * 资金流水
 */
export interface CapitalFlow {
  id: number
  flowNo: string
  flowType: string
  direction: string
  refId: number
  refNo: string
  refType: string
  amount: number
  balance: number
  partyType: string
  partyId: number
  partyName: string
  businessType: string
  occurDate: string
  paymentMethod: string
  bankAccount: string
  bankName: string
  transactionNo: string
  remark: string
  createTime: string
}

/**
 * 预收款API
 */
export const preReceiptApi = {
  getPage(params: any): Promise<ApiResponse<PageResponse<PreReceipt>>> {
    return request.get('/erp/pre-receipt/page', { params })
  },
  getById(id: number): Promise<ApiResponse<PreReceipt>> {
    return request.get(`/erp/pre-receipt/${id}`)
  },
  /** 生成下一预收款单号（YSKD-） */
  nextNo(prefix = 'YSKD'): Promise<ApiResponse<string>> {
    return request.get('/erp/pre-receipt/next-no', { params: { prefix } })
  },
  /** 保存草稿（含收款账户明细） */
  saveDraft(data: any): Promise<ApiResponse<PreReceipt>> {
    return request.post('/erp/pre-receipt/save', data)
  },
  /** 记账：预收余额增加 + 生成凭证 */
  confirm(id: number, operatorId?: number, operatorName?: string): Promise<ApiResponse<PreReceipt>> {
    return request.post('/erp/pre-receipt/confirm', null, { params: { id, operatorId, operatorName } })
  },
  /** 批量确认预收款项（到账入账） */
  batchConfirm(ids: number[]): Promise<ApiResponse<any>> {
    return request.post('/erp/pre-receipt/batch-confirm', ids)
  },
  /** 查询结算单位（客户）预收余额 */
  getAdvanceBalance(customerId: number): Promise<ApiResponse<number>> {
    return request.get('/erp/pre-receipt/advance-balance', { params: { customerId } })
  },
  create(data: any): Promise<ApiResponse<PreReceipt>> {
    return request.post('/erp/pre-receipt', data)
  },
  offsetToReceipt(id: number, receiptId: number, amount: number): Promise<ApiResponse<PreReceipt>> {
    return request.post(`/erp/pre-receipt/${id}/offset-to-receipt`, { receiptId, amount })
  },
  forfeit(id: number, reason: string): Promise<ApiResponse<PreReceipt>> {
    return request.post(`/erp/pre-receipt/${id}/forfeit`, { reason })
  },
  refund(id: number, reason: string): Promise<ApiResponse<PreReceipt>> {
    return request.post(`/erp/pre-receipt/${id}/refund`, { reason })
  },
  getStats(): Promise<ApiResponse<any>> {
    return request.get('/erp/pre-receipt/statistics')
  }
}

/**
 * 预付款API
 */
export const prePaymentApi = {
  getPage(params: any): Promise<ApiResponse<PageResponse<PrePayment>>> {
    return request.get('/erp/pre-payment/page', { params })
  },
  getById(id: number): Promise<ApiResponse<PrePayment>> {
    return request.get(`/erp/pre-payment/${id}`)
  },
  create(data: any): Promise<ApiResponse<PrePayment>> {
    return request.post('/erp/pre-payment', data)
  },
  offsetToPayment(id: number, paymentId: number, amount: number): Promise<ApiResponse<PrePayment>> {
    return request.post(`/erp/pre-payment/${id}/offset-to-payment`, { paymentId, amount })
  },
  recover(id: number, reason: string): Promise<ApiResponse<PrePayment>> {
    return request.post(`/erp/pre-payment/${id}/recover`, { reason })
  },
  refund(id: number, reason: string): Promise<ApiResponse<PrePayment>> {
    return request.post(`/erp/pre-payment/${id}/refund`, { reason })
  },
  getStats(): Promise<ApiResponse<any>> {
    return request.get('/erp/pre-payment/statistics')
  }
}

/**
 * 往来对冲API
 */
export const offsetApi = {
  getPage(params: any): Promise<ApiResponse<PageResponse<Offset>>> {
    return request.get('/erp/offset/page', { params })
  },
  getById(id: number): Promise<ApiResponse<Offset>> {
    return request.get(`/erp/offset/${id}`)
  },
  create(data: any): Promise<ApiResponse<Offset>> {
    return request.post('/erp/offset', data)
  },
  complete(id: number): Promise<ApiResponse<Offset>> {
    return request.post(`/erp/offset/${id}/complete`)
  },
  cancel(id: number, reason: string): Promise<ApiResponse<Offset>> {
    return request.post(`/erp/offset/${id}/cancel`, { reason })
  },
  getItems(offsetId: number): Promise<ApiResponse<OffsetItem[]>> {
    return request.get(`/erp/offset/${offsetId}/items`)
  }
}

/**
 * 资金流水API
 */
export const capitalFlowApi = {
  getPage(params: any): Promise<ApiResponse<PageResponse<CapitalFlow>>> {
    return request.get('/erp/capital-flow/page', { params })
  },
  getStats(): Promise<ApiResponse<any>> {
    return request.get('/erp/capital-flow/statistics')
  },
  exportList(params: any): Promise<Blob> {
    return request.get('/erp/capital-flow/export', { params, responseType: 'blob' })
  },
  /**
   * 在线支付对账单分页查询（单入口列表）
   * 后端: CapitalFlowController /api/erp/capital-flow/reconcile/page
   */
  getReconcilePage(params: any): Promise<ApiResponse<PageResponse<CapitalFlow>>> {
    return request.get('/erp/capital-flow/reconcile/page', { params })
  },
  /**
   * 切换对账标记：flag=1 已对账 0 未对账（缺省取反）
   */
  toggleReconcile(id: number, flag?: number, operator?: string): Promise<ApiResponse<void>> {
    return request.put('/erp/capital-flow/reconcile/' + id, null, { params: { flag, operator } })
  }
}

/**
 * 收款单API
 */
export const receiptApi = {
  getPage(params: any): Promise<ApiResponse<PageResponse<any>>> {
    return request.get('/erp/receipt/page', { params })
  },
  getById(id: number): Promise<ApiResponse<any>> {
    return request.get(`/erp/receipt/${id}`)
  },
  create(data: any): Promise<ApiResponse<any>> {
    return request.post('/erp/receipt', data)
  },
  update(id: number, data: any): Promise<ApiResponse<any>> {
    return request.put(`/erp/receipt/${id}`, data)
  },
  submit(id: number): Promise<ApiResponse<any>> {
    return request.post(`/erp/receipt/${id}/submit`)
  },
  approve(id: number, note?: string): Promise<ApiResponse<any>> {
    return request.post(`/erp/receipt/${id}/approve`, null, { params: { note } })
  },
  reject(id: number, reason: string): Promise<ApiResponse<any>> {
    return request.post(`/erp/receipt/${id}/reject`, null, { params: { reason } })
  },
  complete(id: number): Promise<ApiResponse<any>> {
    return request.post(`/erp/receipt/${id}/complete`)
  },
  cancel(id: number, reason: string): Promise<ApiResponse<any>> {
    return request.post(`/erp/receipt/${id}/cancel`, null, { params: { reason } })
  },
  /**
   * 核销
   */
  writeOff(id: number, amount: number): Promise<ApiResponse<any>> {
    return request.post(`/erp/receipt/${id}/write-off`, { amount })
  },
  /**
   * 完成核销（收款确认）
   */
  completeVerify(id: number): Promise<ApiResponse<any>> {
    return request.post(`/erp/receipt/${id}/complete-verify`)
  },
  /**
   * 批量确认待确认款项（到账入账）
   */
  batchConfirm(ids: number[]): Promise<ApiResponse<any>> {
    return request.post('/erp/receipt/batch-confirm', ids)
  },
  getStatistics(): Promise<ApiResponse<any>> {
    return request.get('/erp/receipt/statistics')
  },
  /**
   * 生成下一个收款单号（SKD- 前缀）
   */
  nextNo(): Promise<ApiResponse<string>> {
    return request.get('/erp/receipt/next-no')
  },
  /**
   * 按明细分页查询收款单（收款明细 tab）
   */
  getPageDetail(params: any): Promise<ApiResponse<PageResponse<any>>> {
    return request.get('/erp/receipt/page-detail', { params })
  },
  /**
   * 批量打印
   */
  batchPrint(data: any): Promise<ApiResponse<any>> {
    return request.post('/erp/receipt/batch-print', data)
  }
}

/**
 * 付款单API
 */
export const paymentApi = {
  getPage(params: any): Promise<ApiResponse<PageResponse<any>>> {
    return request.get('/erp/payment/page', { params })
  },
  getById(id: number): Promise<ApiResponse<any>> {
    return request.get(`/erp/payment/${id}`)
  },
  create(data: any): Promise<ApiResponse<any>> {
    return request.post('/erp/payment', data)
  },
  update(id: number, data: any): Promise<ApiResponse<any>> {
    return request.put(`/erp/payment/${id}`, data)
  },
  submit(id: number): Promise<ApiResponse<any>> {
    return request.post(`/erp/payment/${id}/submit`)
  },
  approve(id: number, note?: string): Promise<ApiResponse<any>> {
    return request.post(`/erp/payment/${id}/approve`, null, { params: { note } })
  },
  reject(id: number, reason: string): Promise<ApiResponse<any>> {
    return request.post(`/erp/payment/${id}/reject`, null, { params: { reason } })
  },
  complete(id: number): Promise<ApiResponse<any>> {
    return request.post(`/erp/payment/${id}/complete`)
  },
  cancel(id: number, reason: string): Promise<ApiResponse<any>> {
    return request.post(`/erp/payment/${id}/cancel`, null, { params: { reason } })
  },
  /**
   * 核销
   */
  writeOff(id: number, amount: number): Promise<ApiResponse<any>> {
    return request.post(`/erp/payment/${id}/write-off`, { amount })
  },
  /**
   * 完成核销（付款确认）
   */
  completeVerify(id: number): Promise<ApiResponse<any>> {
    return request.post(`/erp/payment/${id}/complete-verify`)
  },
  getStatistics(): Promise<ApiResponse<any>> {
    return request.get('/erp/payment/statistics')
  }
}

/**
 * 按单付款（应付款核销工作台）API
 * 数据源：采购单据统一查询（应付来源）+ 发票查询（销售/采购方向）
 */
export interface PaymentByDocQuery {
  current?: number
  size?: number
  /** 单据日期起（yyyy-MM-dd） */
  dateStart?: string
  /** 单据日期止（yyyy-MM-dd） */
  dateEnd?: string
  /** 单据编号 */
  documentNo?: string
  /** 单据类型（INBOUND/RETURN/EXCHANGE） */
  documentType?: string
  /** 往来单位 */
  supplierName?: string
  /** 结算单位 */
  settlementUnit?: string
  /** 经手人 */
  handlerName?: string
  /** 结算状态（已结算/未结算） */
  settlementStatus?: string
  /** 来源订单 */
  sourceOrder?: string
  /** 对账（是/否） */
  reconcileFlag?: string
  /** 发票号码 */
  invoiceNumber?: string
  /** 发票代码 */
  invoiceCode?: string
}

export const paymentByDocApi = {
  /**
   * 应付来源单据分页（待付款/全部单据）
   * @param targetTab pending=待付款 / all=全部单据
   */
  page(targetTab: 'pending' | 'all', params?: PaymentByDocQuery): Promise<PageResponse<any>> {
    const apiParams: Record<string, any> = {
      current: params?.current,
      size: params?.size,
      dateStart: params?.dateStart,
      dateEnd: params?.dateEnd,
      documentNo: params?.documentNo,
      documentType: params?.documentType,
      supplierName: params?.supplierName,
      handlerName: params?.handlerName,
      settlementStatus: params?.settlementStatus,
      sourceOrder: params?.sourceOrder,
    }
    // 待付款 → 只查未结算
    if (targetTab === 'pending' && !apiParams.settlementStatus) {
      apiParams.settlementStatus = '未结算'
    }
    Object.keys(apiParams).forEach(k => {
      if (apiParams[k] === '' || apiParams[k] === null || apiParams[k] === undefined) delete apiParams[k]
    })
    return request.get('/purchase/doc-query/page', apiParams)
  },
  /**
   * 发票方向分页（发票查询：销售/采购）
   */
  invoicePage(params: any): Promise<ApiResponse<PageResponse<any>>> {
    return request.get('/erp/invoice/query', params)
  },
  /**
   * 对账标记（按单付款）：标记采购入库单对应应付为已对账/取消，记录对账人/时间
   * @param documentType 单据类型（INBOUND/RETURN/EXCHANGE）
   * @param id 单据ID
   * @param flag 1-√ 0-否（缺省取反）
   * @param operator 对账人（当前登录用户名）
   */
  reconcile(documentType: string, id: number, flag: number, operator: string): Promise<ApiResponse<any>> {
    return request.put(`/purchase/doc-query/${documentType}/${id}/reconcile`, null, {
      params: { flag, operator },
    })
  },
  /**
   * 无结算付款单核销（按单付款）：对已记账未勾选核销单据的付款单事后补核销
   * @param docIds 勾选的采购入库单ID列表
   */
  noSettleWriteOff(docIds: number[]): Promise<ApiResponse<any>> {
    return request.post('/purchase/doc-query/no-settle-write-off', docIds)
  },
}

/**
 * 按单收款（应收款核销工作台）API
 * 数据源：销售单据统一查询（应收来源）+ 发票查询（销售/采购方向）
 */
export interface ReceiptByDocQuery {
  current?: number
  size?: number
  /** 单据日期起（yyyy-MM-dd） */
  dateStart?: string
  /** 单据日期止（yyyy-MM-dd） */
  dateEnd?: string
  /** 单据编号 */
  documentNo?: string
  /** 单据类型（OUTBOUND/RETURN/EXCHANGE） */
  documentType?: string
  /** 往来单位（客户名称） */
  customerName?: string
  /** 结算单位 */
  settlementUnit?: string
  /** 经手人 */
  handlerName?: string
  /** 结算状态（已结算/未结算） */
  settlementStatus?: string
  /** 来源订单 */
  sourceOrder?: string
  /** 对账（是/否） */
  reconcileFlag?: string
  /** 发票号码 */
  invoiceNumber?: string
  /** 发票代码 */
  invoiceCode?: string
}

export const receiptByDocApi = {
  /**
   * 应收来源单据分页（待收款/超期待收款/全部单据）
   * @param targetTab pending=待收款 / overdue=超期待收款 / all=全部单据
   */
  page(targetTab: 'pending' | 'overdue' | 'all', params?: ReceiptByDocQuery): Promise<PageResponse<any>> {
    const apiParams: Record<string, any> = {
      current: params?.current,
      size: params?.size,
      startDate: params?.dateStart,
      endDate: params?.dateEnd,
      documentNo: params?.documentNo,
      documentType: params?.documentType,
      customerName: params?.customerName,
      handlerName: params?.handlerName,
      settlementStatus: params?.settlementStatus,
      sourceOrder: params?.sourceOrder,
    }
    // 应收来源：仅销售出库/退货/换货单，排除销售订单
    apiParams.receivableOnly = true
    // 待收款 → 只查未结算的应收来源单据
    if (targetTab === 'pending' && !apiParams.settlementStatus) {
      apiParams.settlementStatus = '未结算'
    }
    // 全部单据默认过滤红冲（status=3）
    if (targetTab === 'all') {
      apiParams.showRed = false
    }
    Object.keys(apiParams).forEach(k => {
      if (apiParams[k] === '' || apiParams[k] === null || apiParams[k] === undefined) delete apiParams[k]
    })
    return request.get('/sales/doc-query/page', apiParams)
  },
  /**
   * 发票方向分页（发票查询：销售/采购）
   */
  invoicePage(params: any): Promise<ApiResponse<PageResponse<any>>> {
    return request.get('/erp/invoice/query', params)
  }
}

/**
 * 核销记录
 */
export interface WriteOff {
  id: number
  tenantId?: number
  writeOffNo: string
  writeOffType: string
  receiptId?: number
  receiptNo?: string
  paymentId?: number
  paymentNo?: string
  receivableId?: number
  payableId?: number
  customerId?: number
  customerName?: string
  supplierId?: number
  supplierName?: string
  totalAmount: number
  writeOffAmount: number
  remainingAmount: number
  writeOffDate: string
  status: string
  remark?: string
  createTime: string
}

/**
 * 核销记录API
 */
export const writeOffApi = {
  getPage(params: any): Promise<ApiResponse<PageResponse<WriteOff>>> {
    return request.get('/erp/write-off/page', { params })
  },
  getById(id: number): Promise<ApiResponse<WriteOff>> {
    return request.get(`/erp/write-off/${id}`)
  },
  getByReceiptId(receiptId: number): Promise<ApiResponse<WriteOff[]>> {
    return request.get(`/erp/write-off/receipt/${receiptId}`)
  },
  getByPaymentId(paymentId: number): Promise<ApiResponse<WriteOff[]>> {
    return request.get(`/erp/write-off/payment/${paymentId}`)
  }
}

/**
 * 定金条件API
 */
export interface DepositCondition {
  id: number
  preReceiptId?: number
  prePaymentId?: number
  depositType: string
  direction: string
  sourceType: string
  sourceId: number
  sourceNo: string
  conditionDesc: string
  breachClause: string
  agreedDate: string
  expiredDate: string
  amount: number
  forfeitAmount: number
  contactPerson: string
  contactPhone: string
  status: string
  remark: string
}

export const depositConditionApi = {
  getByPreReceiptId(preReceiptId: number): Promise<ApiResponse<DepositCondition>> {
    return request.get(`/erp/deposit-condition/pre-receipt/${preReceiptId}`)
  },
  getByPrePaymentId(prePaymentId: number): Promise<ApiResponse<DepositCondition>> {
    return request.get(`/erp/deposit-condition/pre-payment/${prePaymentId}`)
  },
  getBySource(sourceType: string, sourceId: number): Promise<ApiResponse<DepositCondition[]>> {
    return request.get('/erp/deposit-condition/source', { params: { sourceType, sourceId } })
  },
  create(data: any): Promise<ApiResponse<DepositCondition>> {
    return request.post('/erp/deposit-condition', data)
  },
  convert(id: number): Promise<ApiResponse<DepositCondition>> {
    return request.post(`/erp/deposit-condition/${id}/convert`)
  },
  refund(id: number, reason: string): Promise<ApiResponse<DepositCondition>> {
    return request.post(`/erp/deposit-condition/${id}/refund`, null, { params: { reason } })
  },
  forfeit(id: number, forfeitAmount: number, reason: string): Promise<ApiResponse<DepositCondition>> {
    return request.post(`/erp/deposit-condition/${id}/forfeit`, { forfeitAmount, reason })
  },
  deduct(id: number, deductAmount: number, reason: string): Promise<ApiResponse<DepositCondition>> {
    return request.post(`/erp/deposit-condition/${id}/deduct`, { deductAmount, reason })
  }
}

/**
 * 往来余额表（辅助核算余额）API
 * 后端: PartnerBalanceController /api/erp/finance/partner-balance
 */
export const partnerBalanceApi = {
  getPage: (params: any) => request.get('/erp/finance/partner-balance/page', params)
}

/**
 * 回款统计（账款交账）API
 * 后端: CollectionStatsController /api/erp/finance/collection-stats
 */
export const collectionStatsApi = {
  getStats: (params: any) => request.get('/erp/finance/collection-stats', params)
}

/**
 * 费用审批 API
 * 后端: ExpenseApprovalController /api/erp/expense/approval
 */
export const expenseApprovalApi = {
  getPending: (params?: any) => request.get('/erp/expense/approval/pending', params),
  process: (data: {
    applicationId: number
    action: 'APPROVE' | 'REJECT'
    comment?: string
    approverId?: string
    approverName?: string
  }) => request.post('/erp/expense/approval/process', data),
  getRecords: (params: any) => request.get('/erp/expense/approval/records', params)
}

/**
 * 费用统计 API
 * 后端: ExpenseController /api/erp/expense/statistics/*
 */
export const expenseStatsApi = {
  getSummary: (params: any) => request.get('/erp/expense/statistics/summary', params),
  getByDepartment: (params: any) => request.get('/erp/expense/statistics/by-department', params),
  getByType: (params: any) => request.get('/erp/expense/statistics/by-type', params)
}

/**
 * 会计期间
 * 后端: AccountingPeriodController /api/erp/finance/period
 */
export interface AccountingPeriod {
  id: number
  periodYear: number
  periodMonth: number
  /** 期间编码，格式 yyyy-MM，如 2026-07 */
  periodCode: string
  startDate: string
  endDate: string
  /** 状态：1-开启 0-关闭（已月结） */
  status: number
  closedBy?: string
  closedTime?: string
  remark?: string
}

/**
 * 月结检查项
 */
export interface MonthClosingCheckItem {
  checkCode: string
  checkName: string
  passed: boolean
  /** 检查明细（未过账凭证阻断时含凭证号列表） */
  detail?: string
}

/**
 * 月结执行结果
 */
export interface MonthClosingResult {
  periodCode: string
  /** 是否全部检查通过（通过则期间已关闭） */
  success: boolean
  message: string
  checks: MonthClosingCheckItem[]
  closedBy?: string
  closedTime?: string
}

/**
 * 月结操作日志
 */
export interface MonthClosingLog {
  id: number
  periodCode: string
  /** 操作类型：close-月结 reopen-反月结 */
  action: string
  operatorId?: string
  operatorName?: string
  /** 月结检查结果快照（JSON字符串） */
  checkResult?: string
  createTime: string
}

/**
 * 会计期间 API
 * 后端: AccountingPeriodController /api/erp/finance/period
 */
export const accountingPeriodApi = {
  /** 分页查询会计期间 */
  getPage: (params: { periodYear?: number; status?: number; page?: number; size?: number }) =>
    request.get('/erp/finance/period/page', params),
  /** 查询会计期间列表（不分页，按期间编码升序） */
  getList: (params?: { periodYear?: number }) =>
    request.get('/erp/finance/period/list', params),
  /** 新增会计期间（后端 @PostMapping("/")，需带尾部斜杠） */
  create: (data: { periodYear: number; periodMonth: number; remark?: string }) =>
    request.post('/erp/finance/period/', data),
  /** 启用/停用会计期间：1-开启 0-关闭 */
  updateStatus: (id: number, status: number) =>
    request.put(`/erp/finance/period/${id}/status`, null, { params: { status } })
}

/**
 * 总账月结 API
 * 后端: MonthClosingController /api/erp/finance/month-closing
 */
export const monthClosingApi = {
  /** 执行月结（先跑检查项，全部通过则关闭期间） */
  execute: (periodCode: string): Promise<MonthClosingResult> =>
    request.post('/erp/finance/month-closing/execute', null, { params: { periodCode } }),
  /** 反月结（重新开启期间） */
  reopen: (periodCode: string): Promise<MonthClosingResult> =>
    request.post('/erp/finance/month-closing/reopen', null, { params: { periodCode } }),
  /** 查询期间月结状态 */
  getStatus: (periodCode: string) =>
    request.get('/erp/finance/month-closing/status', { periodCode }),
  /** 分页查询月结日志 */
  getLogsPage: (params: { periodCode?: string; page?: number; size?: number }) =>
    request.get('/erp/finance/month-closing/logs/page', params)
}

export default {
  reconciliationApi,
  preReceiptApi,
  prePaymentApi,
  receiptApi,
  paymentApi,
  paymentByDocApi,
  receiptByDocApi,
  offsetApi,
  capitalFlowApi,
  writeOffApi,
  depositConditionApi,
  accountSubjectApi,
  voucherApi,
  ledgerApi,
  reportApi,
  receivableApi,
  payableApi,
  partnerBalanceApi,
  collectionStatsApi,
  expenseApprovalApi,
  expenseStatsApi,
  accountingPeriodApi,
  monthClosingApi
}