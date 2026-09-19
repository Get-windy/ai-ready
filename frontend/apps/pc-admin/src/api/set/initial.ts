import request, { type ApiResponse, type PageResponse } from '@/utils/request'

/**
 * 设置模块（set 域）API 封装
 *
 * ⚠️ 路径一律写**相对路径**（axios baseURL 已是 `/api`），严禁再写 `/api/...`（会双前缀 404）。
 * 后端 Controller 前缀：`/api/set/initial-stock`（InitialStockController）。
 * 端点清单（2026-09-18 与后端逐一核对，前端调用与后端实现严格一致）：
 * - GET    /set/initial-stock/page     分页查询
 * - POST   /set/initial-stock/save     新增（body 为**数组**，沿用后端批量口径）
 * - PUT    /set/initial-stock/update   编辑（id 在 body）
 * - DELETE /set/initial-stock/{id}     删除
 * - GET    /set/initial-stock/export   导出全量（与分页同口径）
 */

/** 期初库存行（与后端 InitialStockVO 字段一致） */
export interface InitialStockInfo {
  /** 期初行主键（雪花ID，一律按字符串处理，禁止 Number(id)） */
  id: string
  productId?: string
  productName?: string
  /** 货号 */
  productCode?: string
  barcode?: string
  /** 规格（来自商品档案 erp_product.spec） */
  spec?: string
  /** 型号（来自商品档案 erp_product.model） */
  model?: string
  /** 产地（来自商品档案 erp_product.origin） */
  origin?: string
  /** 小单位 */
  unit?: string
  warehouseId?: string
  warehouseName?: string
  quantity?: number | null
  unitPrice?: number | null
  /** 期初金额 = 期初数量 × 期初成本单价（后端计算，不落库） */
  amount?: number | null
  productionDate?: string
  /** 有效期至（与 DB 列 validity_date 同名口径） */
  validityDate?: string
  remark?: string
  createTime?: string
}

/** 期初库存查询条件 */
export interface InitialStockQuery {
  pageNum?: number
  pageSize?: number
  /** 仓库（对标 ql361 为必选筛选项；本系统保留为可选，不阻断未选仓库时的查询） */
  warehouseId?: string
  /** 商品分类（左分类树选中项，后端按递归子分类过滤） */
  categoryId?: string
  /** 关键字：商品名称 / 货号 / 条码 */
  keyword?: string
  productCode?: string
  productName?: string
  /** 期初数量筛选项：ALL=全部 / HAS=有期初 / NONE=无期初 */
  initialQtyFilter?: string
}

/** 期初库存新增/编辑入参（与后端 InitialStockDTO 字段一致） */
export interface InitialStockForm {
  id?: string
  productId?: string
  productName?: string
  productCode?: string
  warehouseId?: string
  warehouseName?: string
  /** 小单位快照（写入 erp_stock.unit） */
  unit?: string | null
  quantity?: number
  unitPrice?: number
  /** 生产日期（传 null 表示清空；后端用 UpdateWrapper 显式 set，可写入 null） */
  productionDate?: string | null
  /** 有效期至（传 null 表示清空） */
  validityDate?: string | null
  remark?: string | null
}

export const initialStockApi = {
  /** 分页查询期初库存 */
  page(params: InitialStockQuery): Promise<ApiResponse<PageResponse<InitialStockInfo>>> {
    return request.get('/set/initial-stock/page', params)
  },

  /** 新增期初（后端收 List，故单条也包成数组） */
  save(rows: InitialStockForm[]): Promise<ApiResponse<void>> {
    return request.post('/set/initial-stock/save', rows)
  },

  /** 编辑期初（id 在 body） */
  update(data: InitialStockForm): Promise<ApiResponse<void>> {
    return request.put('/set/initial-stock/update', data)
  },

  /** 删除期初（只允许删 is_initial=1 的期初行） */
  remove(id: string): Promise<ApiResponse<void>> {
    return request.delete(`/set/initial-stock/${id}`)
  },

  /** 导出全量期初（与分页同口径，前端据此生成 xlsx） */
  export(params: InitialStockQuery): Promise<ApiResponse<InitialStockInfo[]>> {
    return request.get('/set/initial-stock/export', params)
  },
}

// ═══════════════════════════════════════════════════════════════════════════
// 财务期初（设置 → 数据录入 → 财务期初，菜单 70551 / set:initial-finance）
//
// 后端 Controller 前缀：`/api/erp/finance/initial`（InitialFinanceController）
// 端点清单（2026-09-18 与后端逐一核对，前端调用与后端实现严格一致）：
//   按科目（银行现金 / 固定资产 / 资产负债）：
//   - GET    /erp/finance/initial/subject/page    分页（必传 initialType）
//   - POST   /erp/finance/initial/subject/save    批量保存（body 为数组，id 有值=更新，无值=新增）
//   - PUT    /erp/finance/initial/subject/update  单条更新（id 在 body；科目不可改）
//   - DELETE /erp/finance/initial/subject/{id}    删除（逻辑删除）
//   按往来单位（应付 / 应收）：
//   - GET    /erp/finance/initial/partner/page    分页
//   - POST   /erp/finance/initial/partner/save    批量保存
//   - PUT    /erp/finance/initial/partner/update  单条更新（往来单位不可改）
//   - DELETE /erp/finance/initial/partner/{id}    删除
//   公共：
//   - GET    /erp/finance/initial/trial-balance   试算平衡（借贷合计 + 存货对平检查）
//   - GET    /erp/finance/initial/current-year    当前会计年（取代前端写死的自然年）
//   - GET    /erp/finance/initial/period-status   指定年度关账状态（关账保护）
//   - GET    /erp/finance/initial/export          导出（与分页同口径，不分页）
//
// ⚠️ 历史 P0（本文件创建时登记的缺陷）：旧页面直接 `request.get('/set/initial-finance/page')`
//    等 5 处调用 —— 后端零控制器、库零表，全部 404（开发文档 §6.2）。
// ═══════════════════════════════════════════════════════════════════════════

/** 期初类型（两张表共用一个字段，值域互不重叠） */
export type InitialFinanceType =
  /** 按科目：银行现金期初 */
  | 'BANK_CASH'
  /** 按科目：固定资产期初 */
  | 'FIXED_ASSET'
  /** 按科目：资产负债期初（唯一带借贷方向的类型） */
  | 'BALANCE_SHEET'
  /** 按往来单位：应付期初 */
  | 'PAYABLE'
  /** 按往来单位：应收期初 */
  | 'RECEIVABLE'

/** 借贷方向 */
export type InitialFinanceDirection = 'DEBIT' | 'CREDIT'

/**
 * 财务期初行（按科目）—— 与后端 InitialFinanceSubjectVO 一致。
 * 后端 VO 带 `@JsonInclude(NON_NULL)`：银行现金/固定资产两类**不会**返回 direction 字段。
 */
export interface InitialFinanceSubjectInfo {
  /** 主键（雪花ID，一律按字符串处理，禁止 Number(id)） */
  id: string
  initialType: InitialFinanceType
  periodYear?: number
  subjectId?: string
  /** 科目编号（对标列「科目编号」） */
  subjectCode?: string
  /** 科目名称（对标列「科目名称」） */
  subjectName?: string
  /** 借贷方向（仅资产负债期初返回） */
  direction?: InitialFinanceDirection
  /** 期初金额 */
  openingAmount?: number | null
  createTime?: string
}

/** 财务期初行（按往来单位）—— 与后端 InitialFinancePartnerVO 一致 */
export interface InitialFinancePartnerInfo {
  id: string
  initialType: InitialFinanceType
  periodYear?: number
  partnerId?: string
  /** 供应商编号 / 客户编号 */
  partnerCode?: string
  /** 供应商名称 / 客户名称 */
  partnerName?: string
  /** 默认经手人（仅应收期初返回） */
  defaultHandler?: string
  /** 应付金额（仅应付期初返回） */
  payableAmount?: number | null
  /** 预付金额（仅应付期初返回） */
  prepayAmount?: number | null
  /** 应收金额（仅应收期初返回） */
  receivableAmount?: number | null
  /** 预收金额（仅应收期初返回） */
  advanceAmount?: number | null
  createTime?: string
}

/** 财务期初查询条件 */
export interface InitialFinanceQuery {
  pageNum?: number
  pageSize?: number
  /** 期初类型（必传，用于区分 Tab，避免各 Tab 串数据） */
  initialType: InitialFinanceType
  /** 期初年度（不传 = 不按年度过滤） */
  periodYear?: number
  /** 科目编号（模糊） */
  subjectCode?: string
  /** 科目名称（模糊） */
  subjectName?: string
  /** 供应商编号 / 客户编号（模糊） */
  partnerCode?: string
  /** 供应商名称 / 客户名称（模糊） */
  partnerName?: string
}

/** 期初试算平衡结果 */
export interface TrialBalanceResult {
  periodYear?: number
  debitTotal: number
  creditTotal: number
  difference: number
  balanced: boolean
  rowCount: number
  /** 存货对平检查（财务期初存货类科目余额 vs 库存期初金额）；只报数不阻断保存 */
  inventoryCheck?: InventoryBalanceCheck
}

/** 存货对平检查结果（口径见后端 InventoryBalanceCheckVO） */
export interface InventoryBalanceCheck {
  /** 存货类科目前缀（沿用资产负债表口径：140） */
  subjectCodePrefix: string
  /** 财务侧：存货类科目期初余额合计 */
  financeAmount: number
  /** 库存侧：库存期初金额合计 Σ(数量×单价) */
  stockAmount: number
  /** 差额 = 财务侧 - 库存侧 */
  difference: number
  /** 是否对平（|差额| < 0.01） */
  matched: boolean
  /** 财务侧参与合计的行数 */
  financeRowCount: number
  /** 库存侧参与合计的行数（0 = 还没录库存期初） */
  stockRowCount: number
  /** 结论说明 */
  note: string
}

/** 当前会计年取值来源 */
export type CurrentYearSource =
  /** 命中「开启中且今天落在期间内」的会计期间 */
  | 'OPEN_PERIOD'
  /** 回退到该租户最大会计年度 */
  | 'MAX_PERIOD_YEAR'
  /** 回退到服务器系统年（该租户没有任何会计期间） */
  | 'SYSTEM_YEAR'

/** 当前会计年（GET /erp/finance/initial/current-year） */
export interface CurrentYearResult {
  /** 当前会计年（回退链的最终取值） */
  periodYear: number
  /** 服务器系统年 */
  systemYear: number
  source: CurrentYearSource
  /** 回退提示（非空时页面要提示） */
  hint?: string | null
}

/** 年度关账状态（GET /erp/finance/initial/period-status） */
export interface PeriodStatusResult {
  periodYear: number
  /** 开启中的期间数（status = 1） */
  openCount: number
  /** 已关闭的期间数（status = 0） */
  closedCount: number
  /** 是否允许新增/修改/删除该年度期初 */
  editable: boolean
  /** 禁止原因（editable = false 时有值） */
  reason?: string | null
}

export const initialFinanceApi = {
  // ── 按科目 ──

  /** 分页查询「按科目」期初（银行现金 / 固定资产 / 资产负债） */
  subjectPage(params: InitialFinanceQuery): Promise<ApiResponse<PageResponse<InitialFinanceSubjectInfo>>> {
    return request.get('/erp/finance/initial/subject/page', params)
  },

  /** 批量保存「按科目」期初（单条也包成数组） */
  subjectSave(rows: Partial<InitialFinanceSubjectInfo>[]): Promise<ApiResponse<number>> {
    return request.post('/erp/finance/initial/subject/save', rows)
  },

  /** 更新单条「按科目」期初（科目/期初类型不可改） */
  subjectUpdate(data: Partial<InitialFinanceSubjectInfo>): Promise<ApiResponse<number>> {
    return request.put('/erp/finance/initial/subject/update', data)
  },

  /** 删除「按科目」期初（逻辑删除） */
  subjectRemove(id: string): Promise<ApiResponse<number>> {
    return request.delete(`/erp/finance/initial/subject/${id}`)
  },

  // ── 按往来单位 ──

  /** 分页查询「按往来单位」期初（应付 / 应收） */
  partnerPage(params: InitialFinanceQuery): Promise<ApiResponse<PageResponse<InitialFinancePartnerInfo>>> {
    return request.get('/erp/finance/initial/partner/page', params)
  },

  /** 批量保存「按往来单位」期初 */
  partnerSave(rows: Partial<InitialFinancePartnerInfo>[]): Promise<ApiResponse<number>> {
    return request.post('/erp/finance/initial/partner/save', rows)
  },

  /** 更新单条「按往来单位」期初（往来单位/期初类型不可改） */
  partnerUpdate(data: Partial<InitialFinancePartnerInfo>): Promise<ApiResponse<number>> {
    return request.put('/erp/finance/initial/partner/update', data)
  },

  /** 删除「按往来单位」期初（逻辑删除） */
  partnerRemove(id: string): Promise<ApiResponse<number>> {
    return request.delete(`/erp/finance/initial/partner/${id}`)
  },

  // ── 公共 ──

  /** 期初试算平衡（借贷合计校验 + 存货对平检查） */
  trialBalance(periodYear?: number): Promise<ApiResponse<TrialBalanceResult>> {
    return request.get('/erp/finance/initial/trial-balance', { periodYear })
  },

  /** 当前会计年（取代前端写死的自然年；返回值带 source/hint 供页面提示） */
  currentYear(): Promise<ApiResponse<CurrentYearResult>> {
    return request.get('/erp/finance/initial/current-year')
  },

  /** 指定年度的关账状态（不传 = 当前会计年）；前端据此置灰录入/保存按钮 */
  periodStatus(periodYear?: number): Promise<ApiResponse<PeriodStatusResult>> {
    return request.get('/erp/finance/initial/period-status', { periodYear })
  },

  /** 导出当前 Tab 的全部期初（与分页同口径） */
  export(params: InitialFinanceQuery): Promise<ApiResponse<InitialFinanceSubjectInfo[] | InitialFinancePartnerInfo[]>> {
    return request.get('/erp/finance/initial/export', params)
  },
}

export default { initialStockApi, initialFinanceApi }
