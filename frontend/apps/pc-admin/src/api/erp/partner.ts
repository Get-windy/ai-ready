import request from '@/utils/request'
import type { PageQuery, PageResult } from '@/api/erp'

// ── 往来单位分类 ──
export interface PartnerCategory {
  id: number
  categoryCode: string
  categoryName: string
  categoryType: string
  parentId: number
  categoryLevel: number
  sortOrder: number
  status: number
  children?: PartnerCategory[]
}

export const partnerCategoryApi = {
  getTree(categoryType?: string): Promise<PartnerCategory[]> {
    return request.get('/erp/partner/categories/tree', { params: { categoryType } })
  },
  create(data: Partial<PartnerCategory>): Promise<boolean> {
    return request.post('/erp/partner/categories', data)
  },
  update(id: number, data: Partial<PartnerCategory>): Promise<boolean> {
    return request.put(`/erp/partner/categories/${id}`, data)
  },
  delete(id: number): Promise<boolean> {
    return request.delete(`/erp/partner/categories/${id}`)
  }
}

// ── 客户级别（全站唯一数据源 = biz_customer_grade，2026-09-26 收敛） ──
// 原 gradeType 维度已移除：它是历史表 erp_partner_grade 的列（按 grade_type 区分客户/供应商等级），
// 收敛后的 biz_customer_grade 没有该列，客户级别只有一套。
export interface PartnerGrade {
  id: number
  gradeCode: string
  gradeName: string
  gradeLevel: number
  /** 折扣率（百分数，100 = 不打折） */
  discountRate?: number
  pointRate?: number
  creditLimit?: number
  creditDays?: number
  description?: string
  sortOrder: number
  status: number
}

export const partnerGradeApi = {
  /** 下拉用：仅返回启用级别 */
  list(): Promise<PartnerGrade[]> {
    return request.get('/erp/partner/grades')
  },
  /** 列表页用：分页 + 关键字 + 状态（不传 status 则含停用） */
  page(params?: { keyword?: string; status?: number; pageNum?: number; pageSize?: number }): Promise<{ records: PartnerGrade[]; total: number }> {
    return request.get('/erp/partner/grades/page', { params })
  },
  create(data: Partial<PartnerGrade>): Promise<boolean> {
    return request.post('/erp/partner/grades', data)
  },
  update(id: number, data: Partial<PartnerGrade>): Promise<boolean> {
    return request.put(`/erp/partner/grades/${id}`, data)
  },
  delete(id: number): Promise<boolean> {
    return request.delete(`/erp/partner/grades/${id}`)
  }
}

// ── 往来单位主表 ──
export interface Partner {
  id: number
  partnerCode: string
  partnerName: string
  partnerShortName?: string
  /** 助记码（基础资料快速检索） */
  mnemonicCode?: string
  partnerType: string
  partnerCategoryId?: number
  partnerGradeId?: number
  categoryId?: number
  categoryName?: string
  gradeName?: string
  unifiedSocialCode?: string
  taxId?: string
  taxNumber?: string
  legalPerson?: string
  registeredCapital?: number
  companyPhone?: string
  companyEmail?: string
  phone?: string
  email?: string
  /** 联系人姓名（后端由 biz_party_contact 主记录带出） */
  contactPerson?: string
  /** 联系电话（后端由 biz_party_contact 主记录带出） */
  contactPhone?: string
  contactEmail?: string
  province?: string
  city?: string
  district?: string
  detailAddress?: string
  /** 对方地址（物流公司地址/联系地址，后端由主网点 detail_address 带出） */
  address?: string
  creditLimit?: number
  creditDays?: number
  paymentTerms?: string
  settleType?: string
  taxRate?: number
  openingBalance?: number
  currentBalance?: number
  remark?: string
  status: string
  statusDesc?: string
  website?: string
  fax?: string
  bankName?: string
  bankAccount?: string
  /** 纳税人信息：公司全称 */
  companyFullName?: string
  /** 纳税人信息：开户行地址 */
  bankAddress?: string
  shortName?: string
  createTime?: string
  updateTime?: string
  // ── 基础资料金标准补充（供应商等） ──
  /** 多重身份，逗号分隔：CUSTOMER/SUPPLIER/LOGISTICS/OTHER */
  roles?: string
  /** 列表「新增时间」列 */
  addTime?: string
  /** 附件数量 */
  attachmentCount?: number
  /** 纳税人信息：地址（与「联系地址」区分） */
  taxAddress?: string
  /** 期初应付金额 */
  openingPayable?: number
  /** 期初预付金额 */
  openingPrepaid?: number
  /** 经营系列 */
  operatingSeries?: string
  /** 经营面积 */
  operatingArea?: number
  /** 付款期限方式：DYNAMIC 动态付款期限 / FIXED 固定账期 */
  paymentTermType?: string
  /** 动态付款期限(天) */
  paymentDays?: number
  /** 固定账期日(号) */
  fixedPaymentDay?: number
  /** 结算期(号) */
  settlementDay?: number
  /** 启用价格跟踪 0/1 */
  priceTrackEnabled?: number

  // ── 客户金标准字段（全部客户 26 列 / 会员管理 10 列） ──
  /** 所属仓库 */
  warehouseName?: string
  /** 所属区域 */
  region?: string
  /** 推广人 */
  promoterId?: number | string
  promoterName?: string
  /** 买家账号（商城账号） */
  buyerAccount?: string
  /** 客户一票通 */
  customerOnePass?: string
  /** 客户来源 */
  customerSource?: string
  /** 营业执照有效期 */
  businessLicenseExpiry?: string
  /** 最近交易时间 */
  lastTradeTime?: string
  /** 动态收款期限（天） */
  creditDays?: number
  /** 固定账期（号） */
  fixedCreditDay?: number
  /** 结算期（号） */
  statementDay?: number
  /** 期初应收金额 */
  openingReceivable?: number
  /** 期初预收金额 */
  openingPreReceived?: number

  // ── 会员信息 ──
  memberName?: string
  memberCardNo?: string
  memberLevel?: string
  memberCardStatus?: string
  memberCardStatusDesc?: string
  memberValidStart?: string
  memberValidEnd?: string
  birthday?: string
  points?: number
  memberInitialPoints?: number
  memberTotalConsume?: number
  memberIssueTime?: string
}

/** 「全部联系人」子标签行（联系人 × 归属客户） */
export interface PartyContactRow {
  id: number
  partyId: number
  /** 姓名 */
  contactName: string
  /** 性别 */
  gender?: string
  /** 职务 */
  position?: string
  /** 手机 */
  mobile?: string
  phone?: string
  /** 联系地址 */
  detailAddress?: string
  /** 配送方式 */
  deliveryMethod?: string
  deliveryRoute?: string
  /** 物流公司 */
  logisticsCompany?: string
  /** 网点 */
  outletName?: string
  isPrimary?: number
  status?: number
  /** 对应客户 */
  partnerName?: string
  /** 客户编号 */
  partnerCode?: string
  /** 客户所属区域 */
  partyRegion?: string
  /** 客户经手人 */
  handlerName?: string
  createTime?: string
}

/** 客户区域（区域管理子标签） */
export interface CustomerRegion {
  id: number
  regionCode: string
  regionName: string
  parentId?: number
  regionLevel?: number
  sortOrder?: number
  status?: number
  remark?: string
  children?: CustomerRegion[]
}

export const customerRegionApi = {
  page(params: Record<string, any>): Promise<PageResult<CustomerRegion>> {
    return request.get('/erp/customer/region/page', params)
  },
  list(params?: Record<string, any>): Promise<CustomerRegion[]> {
    return request.get('/erp/customer/region/list', { params })
  },
  getById(id: number | string): Promise<CustomerRegion> {
    return request.get(`/erp/customer/region/${id}`)
  },
  create(data: Partial<CustomerRegion>): Promise<CustomerRegion> {
    return request.post('/erp/customer/region', data)
  },
  update(id: number | string, data: Partial<CustomerRegion>): Promise<boolean> {
    return request.put(`/erp/customer/region/${id}`, data)
  },
  remove(id: number | string): Promise<string | null> {
    return request.delete(`/erp/customer/region/${id}`)
  }
}

export const partnerApi = {
  page(params: PageQuery & { partnerType?: string; categoryId?: number; keyword?: string; showHierarchy?: boolean }): Promise<PageResult<Partner>> {
    return request.get('/erp/md/customer/page', params)
  },
  /** 会员管理子标签分页（客户页 → 会员管理） */
  memberPage(params: Record<string, any>): Promise<PageResult<Partner>> {
    return request.get('/erp/md/customer/member/page', params)
  },
  /** 全部联系人子标签分页（客户页 → 全部联系人） */
  contactPage(params: Record<string, any>): Promise<PageResult<PartyContactRow>> {
    return request.get('/erp/md/customer/contact/page', params)
  },
  getById(id: number): Promise<Partner> {
    return request.get(`/erp/md/customer/${id}`)
  },
  search(keyword: string, partnerType?: string): Promise<Partner[]> {
    return request.get('/erp/md/customer/search', { params: { keyword, partnerType } })
  },
  list(partnerType?: string, status?: string, pageSize?: number): Promise<Partner[]> {
    return request.get('/erp/md/customer/list', { params: { partnerType, status, pageSize } })
  },
  /** 新增往来单位，返回含 id 的对象（供继续挂接网点/联系人等子表） */
  create(data: Record<string, any>): Promise<Partner> {
    return request.post('/erp/md/customer', data)
  },
  update(id: number, data: Record<string, any>): Promise<boolean> {
    return request.put(`/erp/md/customer/${id}`, data)
  },
  updateStatus(id: number, status: string): Promise<boolean> {
    return request.put(`/erp/md/customer/${id}/status`, null, { params: { status } })
  },
  delete(id: number): Promise<boolean> {
    return request.delete(`/erp/md/customer/${id}`)
  },
  getNextSeq(prefix: string): Promise<{ seq: number }> {
    return request.get('/erp/md/customer/next-seq', { params: { prefix } })
  },
  /** 保存主联系人（表单「联系人」分区：联系人 / 联系电话 / 联系地址） */
  savePrimaryContact(id: number, data: { contactPerson?: string; contactPhone?: string; address?: string }): Promise<boolean> {
    return request.put(`/erp/md/customer/${id}/primary-contact`, data)
  },
  /** 导出 Excel（真实 xlsx 流，与分页查询同口径） */
  export(params: Record<string, any>): Promise<Blob> {
    return request.get('/erp/md/customer/export', { params, responseType: 'blob' })
  },
  /** 批量启用/停用 */
  batchStatus(ids: Array<number | string>, status: string): Promise<boolean> {
    return request.put('/erp/md/customer/batch-status', { ids, status })
  },
  /** 批量设置价格跟踪 */
  batchPriceTrack(ids: Array<number | string>, priceTrackEnabled: boolean): Promise<boolean> {
    return request.put('/erp/md/customer/batch-price-track', { ids, priceTrackEnabled })
  },
  /** 批量删除 */
  batchDelete(ids: Array<number | string>): Promise<boolean> {
    return request.delete('/erp/md/customer/batch', { data: { ids } })
  },
  /** 批量搬移（改所属分类） */
  batchMove(ids: Array<number | string>, categoryId: number | string): Promise<boolean> {
    return request.put('/erp/md/customer/batch-move', { ids, categoryId })
  },
  /** 客商合并：把当前往来单位（源）并入目标往来单位 */
  mergePartner(id: number | string, targetId: number | string): Promise<{
    merged: boolean
    reason?: string
    targetId?: string
    targetName?: string
    movedContacts?: number
    movedAttachments?: number
    /** 迁移的业务引用总数（订单/出入库/收付款/应收应付/发票等） */
    movedReferences?: number
    roles?: string
  }> {
    return request.put(`/erp/md/customer/${id}/merge-partner`, { targetId })
  },
}

// ── 联系人 ──
export interface PartyContact {
  id: number
  partyId: number
  /** 名称：客户/供应商场景=联系人姓名；网点场景=网点名称 */
  contactName: string
  /** 网点场景下的联系人姓名 */
  linkman?: string
  /** 联系地址（网点地址） */
  detailAddress?: string
  position: string
  department: string
  phone: string
  mobile: string
  email: string
  wechat: string
  qq: string
  isPrimary: number
  contactRole: number
  status: number
  remark: string
  region?: string
  detailAddress?: string
  deliveryMethod?: string
  deliveryRoute?: string
  openMallAccount?: number
}

export const partnerContactApi = {
  getByPartner(partyId: number): Promise<PartyContact[]> {
    return request.get(`/erp/partner/contacts/by-party/${partyId}`)
  },
  create(data: Partial<PartyContact>): Promise<boolean> {
    return request.post('/erp/partner/contacts', data)
  },
  update(id: number, data: Partial<PartyContact>): Promise<boolean> {
    return request.put(`/erp/partner/contacts/${id}`, data)
  },
  delete(id: number): Promise<boolean> {
    return request.delete(`/erp/partner/contacts/${id}`)
  },
  setPrimary(id: number, partyId: number): Promise<boolean> {
    return request.put(`/erp/partner/contacts/${id}/primary`, null, { params: { partyId } })
  }
}

// ── 角色 ──
export interface PartyRole {
  id: number
  roleName: string
  roleCode: string
  description: string
  status: number
  sortOrder: number
}

export const partnerRoleApi = {
  list(): Promise<PartyRole[]> {
    return request.get('/erp/partner/roles/list')
  },
  getRolesByParty(partyId: number): Promise<PartyRole[]> {
    return request.get(`/erp/partner/roles/by-party/${partyId}`)
  },
  create(data: Partial<PartyRole>): Promise<boolean> {
    return request.post('/erp/partner/roles', data)
  },
  update(id: number, data: Partial<PartyRole>): Promise<boolean> {
    return request.put(`/erp/partner/roles/${id}`, data)
  },
  delete(id: number): Promise<boolean> {
    return request.delete(`/erp/partner/roles/${id}`)
  },
  addRole(partyId: number, roleType: string, isDefault?: boolean): Promise<boolean> {
    return request.post(`/erp/partner/roles/${partyId}/add`, { roleType, isDefault })
  },
  removeRole(partyId: number, roleId: number): Promise<boolean> {
    return request.delete(`/erp/partner/roles/${partyId}/remove/${roleId}`)
  }
}

// ── 地址 ──
export interface PartnerAddress {
  id: number
  partnerId: number
  addressType: string
  province: string
  city: string
  district: string
  detailAddress: string
  zipCode: string
  contactName: string
  contactPhone: string
  isDefault: number
}

export const partnerAddressApi = {
  getByPartner(partnerId: number): Promise<PartnerAddress[]> {
    return request.get(`/erp/partner/addresses/by-partner/${partnerId}`)
  },
  create(data: Partial<PartnerAddress>): Promise<boolean> {
    return request.post('/erp/partner/addresses', data)
  },
  update(id: number, data: Partial<PartnerAddress>): Promise<boolean> {
    return request.put(`/erp/partner/addresses/${id}`, data)
  },
  delete(id: number): Promise<boolean> {
    return request.delete(`/erp/partner/addresses/${id}`)
  },
  setDefault(id: number, partnerId: number): Promise<boolean> {
    return request.put(`/erp/partner/addresses/${id}/default`, null, { params: { partnerId } })
  }
}

// ── 银行账户 ──
export interface PartnerBankAccount {
  id: number
  partnerId: number
  accountName: string
  bankName: string
  bankBranch: string
  accountNo: string
  currency: string
  isDefault: number
}

export const partnerBankAccountApi = {
  getByPartner(partnerId: number): Promise<PartnerBankAccount[]> {
    return request.get(`/erp/partner/bank-accounts/by-partner/${partnerId}`)
  },
  create(data: Partial<PartnerBankAccount>): Promise<boolean> {
    return request.post('/erp/partner/bank-accounts', data)
  },
  update(id: number, data: Partial<PartnerBankAccount>): Promise<boolean> {
    return request.put(`/erp/partner/bank-accounts/${id}`, data)
  },
  delete(id: number): Promise<boolean> {
    return request.delete(`/erp/partner/bank-accounts/${id}`)
  }
}

// ── 标签 ──
export interface PartnerTag {
  id: number
  tagName: string
  tagColor: string
  tagType: string
  description: string
  status: number
  sortOrder: number
}

export const partnerTagApi = {
  list(): Promise<PartnerTag[]> {
    return request.get('/erp/partner/tags/list')
  },
  getTagIds(partnerId: number): Promise<number[]> {
    return request.get(`/erp/partner/tags/${partnerId}/ids`)
  },
  attachTags(partnerId: number, tagIds: number[]): Promise<boolean> {
    return request.post(`/erp/partner/tags/${partnerId}/attach`, tagIds, {
      headers: { 'Content-Type': 'application/json' }
    })
  }
}

// ── 附件 ──
export interface PartnerAttachment {
  id: number
  partnerId: number
  fileName: string
  fileUrl: string
  fileSize: number
  fileType: string
  category: string
  uploadTime: string
  remark: string
}

export const partnerAttachmentApi = {
  getByPartner(partnerId: number): Promise<PartnerAttachment[]> {
    return request.get(`/erp/partner/attachments/by-partner/${partnerId}`)
  },
  create(data: Partial<PartnerAttachment>): Promise<boolean> {
    return request.post('/erp/partner/attachments', data)
  },
  delete(id: number): Promise<boolean> {
    return request.delete(`/erp/partner/attachments/${id}`)
  }
}
