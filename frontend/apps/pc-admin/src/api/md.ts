import request, { type ApiResponse, type PageResponse } from '@/utils/request'

/**
 * 主数据（md 域）API 封装
 * 端点均已与后端 Controller 逐一核对：
 * - 岗位：core-api PositionController        /api/position
 * - 财务账户（支付账户）：erp-finance FinanceAccountController /api/erp/finance/account
 * 仓库（/api/wms/warehouse）复用 @/api/wms/warehouse
 * 部门（/api/department）复用 @/api/department
 * 操作员（/api/user）复用 @/api/user
 * 支付流水/渠道（/api/payment）复用 @/api/payment
 */

// ── 岗位（staff-role） ─────────────────────────────────

/** 岗位信息（与后端 PositionVO 字段一致） */
export interface MdPositionInfo {
  id: number
  positionCode: string
  positionName: string
  categoryId?: number
  categoryName?: string
  deptId?: number
  deptName?: string
  level?: number
  sort?: number
  status: number
  description?: string
  remark?: string
  userCount?: number
  createTime?: string
  updateTime?: string
}

/** 岗位查询参数（与后端 PositionQueryRequest 一致） */
export interface MdPositionQuery {
  positionCode?: string
  positionName?: string
  categoryId?: number
  deptId?: number
  status?: number
  pageNum?: number
  pageSize?: number
}

/** 岗位 API（/api/position） */
export const mdPositionApi = {
  /** 分页查询岗位 */
  getPage(params: MdPositionQuery): Promise<ApiResponse<PageResponse<MdPositionInfo>>> {
    return request.get('/position/page', params)
  },

  /** 获取所有岗位（不分页） */
  getList(params?: Partial<MdPositionQuery>): Promise<ApiResponse<MdPositionInfo[]>> {
    return request.get('/position/list', params)
  },

  /** 创建岗位 */
  create(data: Partial<MdPositionInfo>): Promise<ApiResponse<number>> {
    return request.post('/position', data)
  },

  /** 更新岗位 */
  update(id: number, data: Partial<MdPositionInfo>): Promise<ApiResponse<void>> {
    return request.put(`/position/${id}`, data)
  },

  /** 删除岗位 */
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/position/${id}`)
  },

  /** 启用/禁用岗位 */
  updateStatus(id: number, status: number): Promise<ApiResponse<void>> {
    return request.put(`/position/${id}/status`, null, { params: { status } })
  }
}

// ── 财务账户 / 支付账户（payment-account） ─────────────

/**
 * 财务账户（与后端 FinanceAccount 实体一致）
 * ⚠️ 全平台账户下拉（提存现 / 费用单 / 收付款 / reconciliation / options.ts）复用 `getList`，
 *    本接口与 `getList` 签名保持兼容，勿删改字段。
 */
export interface FinanceAccountInfo {
  id: number
  accountName: string
  /** 账户类型 1-银行账户 2-现金账户 3-内部账户 4-外部账户 */
  accountType: number
  bankName?: string
  bankAccount?: string
  balance?: number
  /** 状态 0-停用 1-启用 */
  status: number
  currency?: string
  /** 账户等级 1-基本账户 2-一般账户 3-专用账户 */
  accountLevel?: number
  createTime?: string
  updateTime?: string
}

/** 支付账户（= 资金账户，与《银行账户》同源 finance_account 单一口径；后端回包为 BankAccountDTO） */
export type PaymentAccountInfo = BankAccountInfo

/** 支付账户查询参数（与后端 BankAccountQueryDTO 一致） */
export interface PaymentAccountQuery {
  /** 账户名称/科目编号/开户银行/银行账号/助记码/银行简称 模糊匹配 */
  keyword?: string
  accountType?: number
  accountLevel?: number
  currency?: string
  status?: number
  /** 显示停用 1=同时显示停用数据（不传且无 status 时仅启用，与《银行账户》口径一致） */
  showDisabled?: number
  /** 显示层次结构 1=按父子层级树形返回 */
  showTree?: number
  parentId?: number
  pageNum?: number
  pageSize?: number
}

/** 支付账户统计（后端 /statistics 回包） */
export interface PaymentAccountStatistics {
  total: number
  enabled: number
  disabled: number
  totalBalance: number
}

/**
 * 支付账户 API（/api/erp/finance/account，erp-finance FinanceAccountController）
 *
 * 单一口径：账户增删改/启停/编号建议后端复用《银行账户》同一 Service（finance_account），
 * 本页仅做「支付账户」视图的接口编排，不重复实现业务规则。
 */
export const financeAccountApi = {
  /** 查询财务账户列表（不分页；全平台账户下拉专用） */
  getList(params?: { status?: number; accountType?: number }): Promise<ApiResponse<FinanceAccountInfo[]>> {
    return request.get('/erp/finance/account/list', params)
  },

  /** 分页查询支付账户（多条件） */
  page(params: PaymentAccountQuery): Promise<ApiResponse<PageResponse<PaymentAccountInfo>>> {
    return request.get('/erp/finance/account/page', params)
  },

  /** 支付账户详情 */
  detail(id: number): Promise<ApiResponse<PaymentAccountInfo>> {
    return request.get(`/erp/finance/account/${id}`)
  },

  /** 生成下一个账户编号（不传 parentId 为顶级编号） */
  nextCode(parentId?: number): Promise<ApiResponse<string>> {
    return request.get('/erp/finance/account/next-code', parentId ? { parentId } : undefined)
  },

  /** 新增支付账户 */
  create(data: Partial<PaymentAccountInfo>): Promise<ApiResponse<PaymentAccountInfo>> {
    return request.post('/erp/finance/account', data)
  },

  /** 修改支付账户 */
  update(id: number, data: Partial<PaymentAccountInfo>): Promise<ApiResponse<PaymentAccountInfo>> {
    return request.put(`/erp/finance/account/${id}`, data)
  },

  /** 删除支付账户（预置账户 / 有下级 / 余额非 0 后端拒绝） */
  remove(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/erp/finance/account/${id}`)
  },

  /** 启用/停用账户（请求体为状态数字：0-停用 1-启用） */
  updateStatus(id: number, status: number): Promise<ApiResponse<void>> {
    return request.put(`/erp/finance/account/status/${id}`, status)
  },

  /** 余额调整（增量口径：正数增加、负数减少） */
  updateBalance(accountId: number, amount: number): Promise<ApiResponse<void>> {
    return request.put(`/erp/finance/account/balance/${accountId}`, amount)
  },

  /** 查询账户统计（与列表同口径过滤；统计卡片走后端，前端不再本地汇总） */
  getStatistics(params?: PaymentAccountQuery): Promise<ApiResponse<PaymentAccountStatistics>> {
    return request.get('/erp/finance/account/statistics', params)
  },

  /** 导出（后端返回真实 xlsx） */
  export(params: PaymentAccountQuery): Promise<Blob> {
    return request.get('/erp/finance/account/export', { params, responseType: 'blob' })
  }
}

// ── 银行账户（md/bank-account，资料 → 财务账户 → 银行账户） ──

/**
 * 银行账户（= 资金账户，与《支付账户》同源 finance_account 单一口径）
 * 对标 ql361 列：科目编号 / 科目名称 / 账户类型 / 是否用于商城线下转账收款
 */
export interface BankAccountInfo {
  id: number
  /** 科目编号（银行编号） */
  subjectCode?: string
  /** 科目名称（银行全称） */
  accountName: string
  /** 账户类型 1-银行账户 2-现金账户 3-内部账户 4-外部账户 */
  accountType?: number
  bankName?: string
  bankAccount?: string
  /** 户主名 */
  accountHolder?: string
  /** 助记码 */
  easyCode?: string
  /** 银行简称 */
  briefName?: string
  /** 收款码地址 */
  qrcodeUrl?: string
  /** 是否用于商城线下转账收款 0-否 1-是 */
  mallTransferEnabled?: number
  /** 上级账户ID */
  parentId?: number | null
  /** 上级账户名称（详情回显） */
  parentName?: string
  balance?: number
  currency?: string
  accountLevel?: number
  /** 状态 0-停用 1-启用 */
  status?: number
  /** 是否系统预置 0-否 1-是 */
  isSystem?: number
  sortNo?: number
  remark?: string
  /** 层级（根=1，树形缩进用） */
  level?: number
  /** 是否有下级 */
  hasChildren?: boolean
  createTime?: string
  updateTime?: string
}

/** 银行账户查询参数（与后端 BankAccountQueryDTO 一致） */
export interface BankAccountQuery {
  keyword?: string
  accountType?: number
  mallTransferEnabled?: number
  status?: number
  /** 显示停用 1=同时显示停用数据 */
  showDisabled?: number
  /** 显示层次结构 1=按父子层级树形返回 */
  showTree?: number
  parentId?: number
  pageNum?: number
  pageSize?: number
}

/** 银行账户 API（/api/erp/finance/bank-account，erp-finance BankAccountController） */
export const bankAccountApi = {
  /** 分页查询（支持显示停用/显示层次结构） */
  page(params: BankAccountQuery): Promise<ApiResponse<PageResponse<BankAccountInfo>>> {
    return request.get('/erp/finance/bank-account/page', params)
  },

  /** 查询详情 */
  detail(id: number): Promise<ApiResponse<BankAccountInfo>> {
    return request.get(`/erp/finance/bank-account/${id}`)
  },

  /** 上级账户下拉（树形顺序，仅启用） */
  options(): Promise<ApiResponse<BankAccountInfo[]>> {
    return request.get('/erp/finance/bank-account/options')
  },

  /** 生成下一个银行编号 */
  nextCode(parentId?: number): Promise<ApiResponse<string>> {
    return request.get('/erp/finance/bank-account/next-code', parentId ? { parentId } : undefined)
  },

  /** 新增银行账户 */
  create(data: Partial<BankAccountInfo>): Promise<ApiResponse<BankAccountInfo>> {
    return request.post('/erp/finance/bank-account', data)
  },

  /** 修改银行账户 */
  update(id: number, data: Partial<BankAccountInfo>): Promise<ApiResponse<BankAccountInfo>> {
    return request.put(`/erp/finance/bank-account/${id}`, data)
  },

  /** 删除银行账户 */
  remove(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/erp/finance/bank-account/${id}`)
  },

  /** 启用/停用（请求体为状态数字：0-停用 1-启用） */
  updateStatus(id: number, status: number): Promise<ApiResponse<BankAccountInfo>> {
    return request.put(`/erp/finance/bank-account/${id}/status`, status)
  },

  /** 导出（后端返回真实 xlsx） */
  export(params: BankAccountQuery): Promise<Blob> {
    return request.get('/erp/finance/bank-account/export', { params, responseType: 'blob' })
  }
}

// ── 线路（md/route，资料 → 配送管理 → 线路） ──

/**
 * 线路主数据（= 配送线路档案，后端 erp_route）
 * 对标 ql361 列：线路编号 / 线路名称 / 线路类型(自配·物流) / 物流公司 / 配送区域 / 备注
 * ⚠️ 与《配送路线单》执行单据（@/api/dms/route 的 deliveryRouteApi）严格区分
 */
export interface MdRouteArea {
  id?: number
  routeId?: number
  /** 配送区域类型 PROVINCE-省 CITY-市 DISTRICT-区县 */
  areaType?: string
  /** 配送区域编码（行政区划编码） */
  areaCode?: string
  areaName?: string
  sortNo?: number
}

export interface MdRouteInfo {
  id: number
  routeCode: string
  routeName: string
  /** 线路类型-自配 0-否 1-是 */
  routeSelf?: number
  /** 线路类型-物流 0-否 1-是 */
  routeLogistics?: number
  expressName?: string
  /** ENABLED-已启用 DISABLED-已停用 */
  status?: string
  remark?: string
  areas?: MdRouteArea[]
  /** 派生：线路类型文本 */
  routeTypeText?: string
  /** 派生：配送区域文本 */
  areaText?: string
  statusText?: string
  createTime?: string
  updateTime?: string
}

/** 线路查询参数（对标固定查询项） */
export interface MdRouteQuery {
  keyword?: string
  status?: string
  routeType?: string
  showDisabled?: number
  pageNum?: number
  pageSize?: number
}

/** 线路 API（/api/erp/md/route，erp-delivery-route RouteMasterController） */
export const mdRouteApi = {
  /** 分页查询 */
  page(params: MdRouteQuery): Promise<ApiResponse<PageResponse<MdRouteInfo>>> {
    return request.get('/erp/md/route/page', params)
  },

  /** 详情（含配送区域子表） */
  detail(id: number): Promise<ApiResponse<MdRouteInfo>> {
    return request.get(`/erp/md/route/${id}`)
  },

  /** 启用线路下拉（单据「配送线路」引用） */
  options(): Promise<ApiResponse<MdRouteInfo[]>> {
    return request.get('/erp/md/route/options')
  },

  /** 生成下一个线路编号 */
  nextCode(): Promise<ApiResponse<string>> {
    return request.get('/erp/md/route/next-code')
  },

  /** 新增 */
  create(data: Partial<MdRouteInfo>): Promise<ApiResponse<MdRouteInfo>> {
    return request.post('/erp/md/route', data)
  },

  /** 修改 */
  update(id: number, data: Partial<MdRouteInfo>): Promise<ApiResponse<MdRouteInfo>> {
    return request.put(`/erp/md/route/${id}`, data)
  },

  /** 删除 */
  remove(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/erp/md/route/${id}`)
  },

  /** 启用/停用（ENABLED / DISABLED） */
  updateStatus(id: number, status: string): Promise<ApiResponse<MdRouteInfo>> {
    return request.put(`/erp/md/route/${id}/status`, { status })
  },

  /** 批量启用/停用 */
  batchStatus(ids: number[], status: string): Promise<ApiResponse<number>> {
    return request.post('/erp/md/route/batch-status', { ids, status })
  },

  /** 导出（后端返回真实 xlsx） */
  export(params: MdRouteQuery): Promise<Blob> {
    return request.get('/erp/md/route/export', { params, responseType: 'blob' })
  }
}

// ── 其他收入（md/other-income，资料 → 财务账户 → 其他收入） ──

/** 其他收入 = 收入类会计科目视图（subject_type=5 损益类 且 direction=2 贷方），非独立字典表 */
export interface MdOtherIncomeInfo {
  id: number
  subjectCode: string
  subjectName: string
  mnemonicCode?: string
  fullName?: string
  auxiliaryTypeId?: number | null
  auxiliaryTypeName?: string
  direction?: number
  isEnabled?: boolean
}

export interface MdOtherIncomeQuery {
  pageNum?: number
  pageSize?: number
  keyword?: string
  /** true=含已停用收入科目 */
  includeDisabled?: boolean
}

/** 其他收入 API（/api/erp/md/other-income，erp-finance OtherIncomeSubjectController） */
export const mdOtherIncomeApi = {
  /** 分页查询收入科目 */
  page(params: MdOtherIncomeQuery): Promise<ApiResponse<PageResponse<MdOtherIncomeInfo>>> {
    return request.get('/erp/md/other-income/page', params)
  },

  /** 平铺列表（下拉/导出共用口径） */
  list(params?: MdOtherIncomeQuery): Promise<ApiResponse<MdOtherIncomeInfo[]>> {
    return request.get('/erp/md/other-income/list', params)
  },

  /** 核算项下拉（复用辅助核算类型主数据；方法名与 accountSubjectApi 对齐，供共享编辑器组件调用） */
  getAuxTypes(): Promise<ApiResponse<any[]>> {
    return request.get('/erp/md/other-income/aux-types')
  },

  /** 详情 */
  detail(id: number): Promise<ApiResponse<MdOtherIncomeInfo>> {
    return request.get(`/erp/md/other-income/${id}`)
  },

  /** 新增收入 */
  create(data: Partial<MdOtherIncomeInfo>): Promise<ApiResponse<MdOtherIncomeInfo>> {
    return request.post('/erp/md/other-income', data)
  },

  /** 修改收入 */
  update(id: number, data: Partial<MdOtherIncomeInfo>): Promise<ApiResponse<MdOtherIncomeInfo>> {
    return request.put(`/erp/md/other-income/${id}`, data)
  },

  /** 删除收入 */
  remove(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/erp/md/other-income/${id}`)
  },

  /** 启用/停用 */
  toggleEnabled(id: number, enabled: boolean): Promise<ApiResponse<MdOtherIncomeInfo>> {
    return request.put(`/erp/md/other-income/${id}/enable`, null, { params: { enabled } })
  },

  /** 导出（后端返回真实 xlsx） */
  export(params: MdOtherIncomeQuery): Promise<Blob> {
    return request.get('/erp/md/other-income/export', { params, responseType: 'blob' })
  }
}

// ── 费用类型（md/expense-type，资料 → 财务账户 → 费用类型） ──

/**
 * 费用类型 = 费用类会计科目视图（subject_type=5 损益类 且 direction=1 借方），非独立字典表。
 * 对标依据（ql361 实测）：费用类型页与会计科目页调用同一接口
 * `cc.erp.bll.bas.account.getlist`，仅 bastype 不同（费用类型=fee/root=00004）；
 * 行内「修改」打开的编辑器标题为「会计科目」（科目编号/科目名称/助记码/科目全名/核算项/借-贷）。
 * ⚠️ 与《费用单》（finance:expense-doc，erp_expense_doc）严格区分：前者是科目主数据视图，后者是费用单据。
 */
export interface MdExpenseTypeInfo {
  id: number
  subjectCode: string
  subjectName: string
  parentId?: number | null
  /** 上级科目编码（列表展示用） */
  parentCode?: string
  /** 上级科目名称（列表展示用） */
  parentName?: string
  level?: number
  /** 科目分类：固定 5-损益类 */
  subjectType?: number
  /** 借贷方向：固定 1-借方（费用方向） */
  direction?: number
  isLeaf?: boolean
  isEnabled?: boolean
  mnemonicCode?: string
  fullName?: string
  auxiliaryTypeId?: number | null
  auxiliaryTypeName?: string
  remark?: string
  children?: MdExpenseTypeInfo[]
}

/** 费用类型查询参数（对标固定查询项：筛选条件 + 显示停用 + 显示层次结构） */
export interface MdExpenseTypeQuery {
  keyword?: string
  /** true=含已停用费用科目 */
  includeDisabled?: boolean
  /** true=树形（默认），false=平铺 */
  hierarchical?: boolean
}

/** 费用类型 API（/api/erp/md/expense-type，erp-finance ExpenseTypeSubjectController） */
export const mdExpenseTypeApi = {
  /** 费用类科目树（hierarchical=false 时返回平铺列表） */
  tree(params?: MdExpenseTypeQuery): Promise<ApiResponse<MdExpenseTypeInfo[]>> {
    return request.get('/erp/md/expense-type/tree', params)
  },

  /** 平铺列表（下拉/导出共用口径） */
  list(params?: MdExpenseTypeQuery): Promise<ApiResponse<MdExpenseTypeInfo[]>> {
    return request.get('/erp/md/expense-type/list', params)
  },

  /** 核算项下拉（复用辅助核算类型主数据；方法名与 accountSubjectApi 对齐，供共享编辑器组件调用） */
  getAuxTypes(): Promise<ApiResponse<any[]>> {
    return request.get('/erp/md/expense-type/aux-types')
  },

  /** 详情 */
  detail(id: number): Promise<ApiResponse<MdExpenseTypeInfo>> {
    return request.get(`/erp/md/expense-type/${id}`)
  },

  /** 新增费用 */
  create(data: Partial<MdExpenseTypeInfo>): Promise<ApiResponse<MdExpenseTypeInfo>> {
    return request.post('/erp/md/expense-type', data)
  },

  /** 修改费用 */
  update(id: number, data: Partial<MdExpenseTypeInfo>): Promise<ApiResponse<MdExpenseTypeInfo>> {
    return request.put(`/erp/md/expense-type/${id}`, data)
  },

  /** 删除费用 */
  remove(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/erp/md/expense-type/${id}`)
  },

  /** 启用/停用 */
  toggleEnabled(id: number, enabled: boolean): Promise<ApiResponse<MdExpenseTypeInfo>> {
    return request.put(`/erp/md/expense-type/${id}/enable`, null, { params: { enabled } })
  },

  /** 导出（后端返回真实 xlsx） */
  export(params: MdExpenseTypeQuery): Promise<Blob> {
    return request.get('/erp/md/expense-type/export', { params, responseType: 'blob' })
  }
}

export default { mdPositionApi, financeAccountApi, bankAccountApi, mdRouteApi, mdOtherIncomeApi, mdExpenseTypeApi }
