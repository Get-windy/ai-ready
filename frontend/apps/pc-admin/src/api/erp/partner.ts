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

// ── 往来单位等级 ──
export interface PartnerGrade {
  id: number
  gradeCode: string
  gradeName: string
  gradeType: string
  gradeLevel: number
  sortOrder: number
  status: number
}

export const partnerGradeApi = {
  list(gradeType?: string): Promise<PartnerGrade[]> {
    return request.get('/erp/partner/grades', { params: { gradeType } })
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
  partnerShortName: string
  partnerType: string
  partnerCategoryId: number
  partnerGradeId: number
  categoryName: string
  gradeName: string
  unifiedSocialCode: string
  taxId: string
  legalPerson: string
  registeredCapital: number
  companyPhone: string
  companyEmail: string
  contactPerson: string
  contactPhone: string
  contactEmail: string
  province: string
  city: string
  district: string
  detailAddress: string
  creditLimit: number
  creditDays: number
  paymentTerms: string
  settleType: string
  taxRate: number
  openingBalance: number
  currentBalance: number
  remark: string
  status: string
  createTime: string
}

export const partnerApi = {
  page(params: PageQuery & { partnerType?: string; categoryId?: number }): Promise<PageResult<Partner>> {
    return request.get('/erp/partner/page', params)
  },
  getById(id: number): Promise<Partner> {
    return request.get(`/erp/partner/${id}`)
  },
  search(keyword: string, partnerType?: string): Promise<Partner[]> {
    return request.get('/erp/partner/search', { params: { keyword, partnerType } })
  },
  create(data: Partial<Partner>): Promise<boolean> {
    return request.post('/erp/partner', data)
  },
  update(id: number, data: Partial<Partner>): Promise<boolean> {
    return request.put(`/erp/partner/${id}`, data)
  },
  updateStatus(id: number, status: string): Promise<boolean> {
    return request.put(`/erp/partner/${id}/status`, null, { params: { status } })
  },
  delete(id: number): Promise<boolean> {
    return request.delete(`/erp/partner/${id}`)
  }
}

// ── 联系人 ──
export interface PartyContact {
  id: number
  partyId: number
  contactName: string
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
