/**
 * 应收应付调整 API 模块
 * 后端: ArApAdjustController (/api/erp/finance/ar-ap-adjust)
 * 不动资金账户的往来余额调整：应收增加/应收减少/应付增加/应付减少四向。
 */
import request from '@/utils/request'

// ── 调整方向枚举（与后端一致） ────────────
export const ADJUST_DIRECTION = {
  AR_INCREASE: 1,
  AR_DECREASE: 2,
  AP_INCREASE: 3,
  AP_DECREASE: 4,
} as const

export const ADJUST_DIRECTION_OPTIONS: { value: number; label: string }[] = [
  { value: 1, label: '应收增加' },
  { value: 2, label: '应收减少' },
  { value: 3, label: '应付增加' },
  { value: 4, label: '应付减少' },
]

// ── 主表（对齐 ArApAdjust 实体） ────────────
export interface ArApAdjust {
  id: number
  tenantId?: number
  docNo: string
  docDate: string
  /** 调整方向 1应收增加 2应收减少 3应付增加 4应付减少 */
  direction: number
  directionName?: string
  /** 结算单位类型 customer-客户 supplier-供应商 */
  partnerType?: string
  partnerId?: number
  partnerCode?: string
  partnerName?: string
  handlerId?: number
  handlerName?: string
  deptId?: number
  deptName?: string
  totalAmount: number
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

// ── 科目明细（对齐 ArApAdjustItem 实体） ─────
export interface ArApAdjustItem {
  id?: number
  adjustId?: number
  lineNo?: number
  subjectCode: string
  subjectName: string
  amount: number
  remark?: string
}

// ── 单据详情（含明细 items，对齐 ArApAdjustVO） ────
export interface ArApAdjustVO extends ArApAdjust {
  items?: ArApAdjustItem[]
}

// ── 列表查询条件（对齐 ArApAdjustQuery） ────────────
export interface ArApAdjustQuery {
  pageNum?: number
  pageSize?: number
  dateStart?: string
  dateEnd?: string
  docNo?: string
  partnerName?: string
  handlerName?: string
  deptName?: string
  creatorName?: string
  bookkeeperName?: string
  status?: number
  direction?: number
  remark?: string
  showRed?: boolean
}

export const arApAdjustApi = {
  /** 多条件分页查询(单表)：GET /erp/finance/ar-ap-adjust/page */
  page(params: ArApAdjustQuery) { return request.get('/erp/finance/ar-ap-adjust/page', { params }) },
  /** 多条件分页查询(单表，别名)：GET /erp/finance/ar-ap-adjust/doc-query */
  docQuery(params: ArApAdjustQuery) { return request.get('/erp/finance/ar-ap-adjust/doc-query', { params }) },
  /** 生成下一调整单号：GET /erp/finance/ar-ap-adjust/next-no */
  nextNo() { return request.get('/erp/finance/ar-ap-adjust/next-no') },
  /** 单据详情（含明细 items）：GET /erp/finance/ar-ap-adjust/{id} */
  getById(id: number) { return request.get(`/erp/finance/ar-ap-adjust/${id}`) },
  /** 保存草稿（含明细）：POST /erp/finance/ar-ap-adjust/create */
  create(data: Partial<ArApAdjustVO>) { return request.post('/erp/finance/ar-ap-adjust/create', data) },
  /** 更新草稿（明细整体替换）：POST /erp/finance/ar-ap-adjust/update */
  update(data: Partial<ArApAdjustVO>) { return request.post('/erp/finance/ar-ap-adjust/update', data) },
  /** 记账（生成凭证 + 调整应收/应付余额）：POST /erp/finance/ar-ap-adjust/confirm */
  confirm(id: number, operatorId?: number, operatorName?: string) {
    return request.post('/erp/finance/ar-ap-adjust/confirm', null, { params: { id, operatorId, operatorName } })
  },
  /** 取消单据（仅草稿）：POST /erp/finance/ar-ap-adjust/cancel */
  cancel(id: number) { return request.post('/erp/finance/ar-ap-adjust/cancel', null, { params: { id } }) },
  /** 删除单据（仅草稿/已取消）：DELETE /erp/finance/ar-ap-adjust/{id} */
  remove(id: number) { return request.delete(`/erp/finance/ar-ap-adjust/${id}`) },
}
