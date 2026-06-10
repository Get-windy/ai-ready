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
export interface PartnerContact {
  id: number
  partnerId: number
  contactName: string
  contactPhone: string
  contactEmail: string
  position: string
  department: string
  isDefault: number
  remark: string
}

export const partnerContactApi = {
  getByPartner(partnerId: number): Promise<PartnerContact[]> {
    return request.get(`/erp/partner/contacts/${partnerId}`)
  },
  create(data: Partial<PartnerContact>): Promise<boolean> {
    return request.post('/erp/partner/contacts', data)
  },
  update(id: number, data: Partial<PartnerContact>): Promise<boolean> {
    return request.put(`/erp/partner/contacts/${id}`, data)
  },
  delete(id: number): Promise<boolean> {
    return request.delete(`/erp/partner/contacts/${id}`)
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
    return request.get(`/erp/partner/addresses/${partnerId}`)
  },
  create(data: Partial<PartnerAddress>): Promise<boolean> {
    return request.post('/erp/partner/addresses', data)
  },
  update(id: number, data: Partial<PartnerAddress>): Promise<boolean> {
    return request.put(`/erp/partner/addresses/${id}`, data)
  },
  setDefault(id: number, partnerId: number): Promise<boolean> {
    return request.put(`/erp/partner/addresses/${id}/set-default`, null, { params: { partnerId } })
  },
  delete(id: number): Promise<boolean> {
    return request.delete(`/erp/partner/addresses/${id}`)
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
    return request.get(`/erp/partner/bank-accounts/${partnerId}`)
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
  sortOrder: number
  status: number
}

export const partnerTagApi = {
  list(): Promise<PartnerTag[]> {
    return request.get('/erp/partner/tags')
  },
  create(data: Partial<PartnerTag>): Promise<boolean> {
    return request.post('/erp/partner/tags', data)
  },
  update(id: number, data: Partial<PartnerTag>): Promise<boolean> {
    return request.put(`/erp/partner/tags/${id}`, data)
  },
  delete(id: number): Promise<boolean> {
    return request.delete(`/erp/partner/tags/${id}`)
  },
  getTagIds(partnerId: number): Promise<number[]> {
    return request.get(`/erp/partner/tags/partner/${partnerId}`)
  },
  attachTags(partnerId: number, tagIds: number[]): Promise<boolean> {
    return request.post('/erp/partner/tags/attach', { partnerId, tagIds })
  }
}
