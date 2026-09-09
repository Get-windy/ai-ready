/**
 * 提存（提存现金转账）API 模块
 * 后端: CashTransferController (/api/erp/finance/cash-transfer)
 * 资金在企业账户间移动（银行提现、现金存行、账户互转），不涉及往来单位。
 */
import request from '@/utils/request'

// ── 提存主表（对齐 CashTransfer 实体） ────────────
export interface CashTransfer {
  id: number
  tenantId?: number
  docNo: string
  docDate: string
  fromAccountId: number
  fromAccountName: string
  fromAccountType?: number
  fromSubjectCode?: string
  fromAmount: number
  fee: number
  toAmount: number
  totalAmount: number
  handlerId?: number
  handlerName?: string
  deptId?: number
  deptName?: string
  status: number
  creatorName?: string
  bookkeeperId?: number
  bookkeeperName?: string
  bookkeepingTime?: string
  summary?: string
  attachment?: string
  remark?: string
  printCount?: number
  redFlag?: number
  createTime: string
  updateTime: string
}

// ── 提存转入账户明细（对齐 CashTransferItem 实体） ─────
export interface CashTransferItem {
  id?: number
  transferId?: number
  lineNo?: number
  toAccountId: number
  toAccountNo?: string
  toAccountName: string
  toAccountType?: number
  toSubjectCode?: string
  amount: number
  remark?: string
}

// ── 提存单（含明细，对齐 CashTransferVO） ────────────
export interface CashTransferVO extends CashTransfer {
  items?: CashTransferItem[]
}

// ── 按明细列表行（对齐 CashTransferItemVO） ──────────
export interface CashTransferItemVO extends CashTransferItem {
  transferId?: number
  docDate?: string
  docNo?: string
  status?: number
  fromAccountId?: number
  fromAccountName?: string
  toAmount?: number
  fee?: number
  handlerName?: string
  deptName?: string
  creatorName?: string
  bookkeeperName?: string
  bookkeepingTime?: string
  summary?: string
  remark?: string
  itemRemark?: string
  printCount?: number
}

// ── 列表查询条件（对齐 CashTransferQuery） ────────────
export interface CashTransferQuery {
  pageNum?: number
  pageSize?: number
  dateStart?: string
  dateEnd?: string
  docNo?: string
  handlerName?: string
  deptName?: string
  creatorName?: string
  bookkeeperName?: string
  status?: number
  fromAccountName?: string
  toAccountName?: string
  remark?: string
  itemRemark?: string
  showRed?: boolean
}

export const cashTransferApi = {
  /** 多条件分页查询(按单据)：GET /erp/finance/cash-transfer/page */
  page(params: CashTransferQuery) { return request.get('/erp/finance/cash-transfer/page', { params }) },
  /** 多条件分页查询(按单据，别名)：GET /erp/finance/cash-transfer/doc-query */
  docQuery(params: CashTransferQuery) { return request.get('/erp/finance/cash-transfer/doc-query', { params }) },
  /** 分页查询提存明细(按明细)：GET /erp/finance/cash-transfer/page-detail */
  pageDetail(params: CashTransferQuery) { return request.get('/erp/finance/cash-transfer/page-detail', { params }) },
  /** 生成下一提存单号：GET /erp/finance/cash-transfer/next-no */
  nextNo() { return request.get('/erp/finance/cash-transfer/next-no') },
  /** 单据详情（含明细 items）：GET /erp/finance/cash-transfer/{id} */
  getById(id: number) { return request.get(`/erp/finance/cash-transfer/${id}`) },
  /** 保存草稿（含明细）：POST /erp/finance/cash-transfer/create */
  create(data: Partial<CashTransferVO>) { return request.post('/erp/finance/cash-transfer/create', data) },
  /** 更新草稿（明细整体替换）：POST /erp/finance/cash-transfer/update */
  update(data: Partial<CashTransferVO>) { return request.post('/erp/finance/cash-transfer/update', data) },
  /** 记账（生成凭证 + 动账户 + 流水）：POST /erp/finance/cash-transfer/confirm */
  confirm(id: number, operatorId?: number, operatorName?: string) {
    return request.post('/erp/finance/cash-transfer/confirm', null, { params: { id, operatorId, operatorName } })
  },
  /** 取消单据（仅草稿）：POST /erp/finance/cash-transfer/cancel */
  cancel(id: number) { return request.post('/erp/finance/cash-transfer/cancel', null, { params: { id } }) },
  /** 删除单据（仅草稿/已取消）：DELETE /erp/finance/cash-transfer/{id} */
  remove(id: number) { return request.delete(`/erp/finance/cash-transfer/${id}`) },
}
